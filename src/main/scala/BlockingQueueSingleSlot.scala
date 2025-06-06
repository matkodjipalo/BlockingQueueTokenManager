import zio._

object BlockingQueueSingleSlot extends ZIOAppDefault {

  private case class Token(bearer: String)

  // Simulate token validation: randomly valid or not
  private def isTokenValid(token: Token): UIO[Boolean] =
    Random.nextBoolean.flatMap { isValid =>
      Console
        .printLine(s"[Validation] Token '${token.bearer}' is ${if (isValid) "valid" else "invalid"}")
        .as(isValid)
        .orDie
    }

  // Simulate a potentially failing token fetch
  private def fetchNewToken: IO[Throwable, Token] =
    Random.nextBoolean.flatMap {
      case true  =>
        Random.nextUUID.map(uuid => Token(s"token-${uuid.toString.take(8)}")) <*
          Console.printLine(s"[TokenService] Acquired new token")
      case false =>
        Console.printLine(s"[TokenService] Failed to fetch new token") *>
          ZIO.fail(new RuntimeException("Token service unavailable"))
    }

  // OPTION 1: Retry 3 times, then succeed with fallback token
  private def fetchNewTokenWithFallback: UIO[Token] =
    fetchNewToken
      .retry(Schedule.recurs(10) && Schedule.spaced(3500.millis))
      .orElse {
        Console
          .printLine("[TokenService] Giving up after 10 attempts, using fallback")
          .orDie
          .as(Token("fallback-token"))
      }

  // Select one option here 👇
  private val selectedFetch: UIO[Token] = fetchNewTokenWithFallback
  // private val selectedFetch: UIO[Token] = fetchNewTokenFailFast
  // private val selectedFetch: UIO[Token] = fetchNewTokenRetryForever

  // Storelogix "request client" worker
  private def makeStorelogixRequest(
                                     requestName: String,
                                     queue: Queue[Token],
                                     semaphore: Semaphore
                                   ): UIO[Unit] =
    (for {
      _ <- semaphore.withPermit {
        for {
          token   <- queue.take
          isValid <- isTokenValid(token)
          _       <- if (!isValid)
            for {
              _      <- Console.printLine(s"[$requestName] Token is invalid. Will fetch a new one.")
              newTok <- selectedFetch
              _      <- queue.offer(newTok)
            } yield ()
          else
            (
              if (requestName == "askForStockUpdates")
                Console
                  .printLine(s"[$requestName] request failed with the token ${token.bearer}")
                  .delay(1.second) *>
                  ZIO.fail(new RuntimeException(s"[$requestName] request simulated failure"))
              else
                Console
                  .printLine(s"[$requestName] request successfully finished with the token ${token.bearer}")
                  .delay(1.second)
              ).onExit {
              case Exit.Success(_)     =>
                queue.offer(token) *>
                  Console.printLine(s"[$requestName] returned token to queue after success").orDie
              case Exit.Failure(cause) =>
                queue.offer(token) *>
                  Console
                    .printLine(s"[$requestName] returned token to queue after failure: ${cause.prettyPrint}")
                    .orDie
            }
        } yield ()
      }
      _ <- makeStorelogixRequest(requestName, queue, semaphore)
    } yield ()).orDie

  override def run: ZIO[Any, Nothing, Unit] =
    for {
      queue     <- Queue.bounded[Token](1)
      semaphore <- Semaphore.make(1)
      _         <- ZIO.foreachDiscard(
        List("askForShipmentNotifications", "askForStockUpdates", "dispatchProducts")
      )(name => makeStorelogixRequest(name, queue, semaphore).fork)

      _         <- queue.offer(Token("initial-token")) *>
        Console.printLine("[Queue] offered initial token").orDie

      _         <- ZIO.sleep(30.seconds) // Run for a while
    } yield ()
}
