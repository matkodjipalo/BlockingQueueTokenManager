import zio._
import zio.Duration._

object BlockingQueueTokenPool extends ZIOAppDefault {

  private case class Token(bearer: String)

  private val poolSize = 10

  private def isTokenValid(token: Token): UIO[Boolean] =
    Random.nextBoolean.flatMap { isValid =>
      Console
        .printLine(
          s"[Validation] Token '${token.bearer}' is ${if (isValid) "valid" else "invalid"}"
        )
        .as(isValid)
        .orDie
    }

  private def fetchNewToken: IO[Throwable, Token] =
    Random.nextBoolean.flatMap {
      case true  =>
        Random.nextUUID.map(uuid => Token(s"token-${uuid.toString.take(8)}")) <*
          Console.printLine(s"[TokenService] Acquired new token")
      case false =>
        Console.printLine(s"[TokenService] Failed to fetch new token") *>
          ZIO.fail(new RuntimeException("Token service unavailable"))
    }

  private def fetchNewTokenWithFallback: UIO[Token] =
    fetchNewToken
      .retry(Schedule.recurs(3) && Schedule.spaced(3500.millis))
      .orElse {
        Console
          .printLine("[TokenService] Giving up after 10 attempts, using fallback")
          .orDie
          .as(Token("fallback-token"))
      }

  private def makeStorelogixRequest(
      requestName: String,
      queue: Queue[Token]
  ): UIO[Unit] =
    (for {
      token   <- queue.take
      isValid <- isTokenValid(token)
      _       <-
        if (!isValid)
          for {
            _      <- Console.printLine(s"[$requestName] Token is invalid. Fetching new one.")
            newTok <- fetchNewTokenWithFallback
            _      <- queue.offer(newTok)
          } yield ()
        else
          (
            if (requestName == "askForStockUpdates")
              Console
                .printLine(s"[$requestName] request failed with token ${token.bearer}")
                .delay(1.second) *>
                ZIO.fail(new RuntimeException(s"[$requestName] simulated failure"))
            else
              Console
                .printLine(s"[$requestName] request succeeded with token ${token.bearer}")
                .delay(1.second)
          ).onExit {
            case Exit.Success(_)     =>
              queue.offer(token) *>
                Console
                  .printLine(s"[$requestName] returned token to queue after success")
                  .orDie
            case Exit.Failure(cause) =>
              queue.offer(token) *>
                Console
                  .printLine(
                    s"[$requestName] returned token to queue after failure: ${cause.prettyPrint}"
                  )
                  .orDie
          }
      _       <- makeStorelogixRequest(requestName, queue)
    } yield ()).orDie

  override def run: ZIO[Any, Nothing, Unit] =
    for {
      queue <- Queue.bounded[Token](poolSize)

      // Pre-populate the pool with tokens
      _ <- ZIO.foreachDiscard(1 to poolSize) { i =>
             val token = Token(s"initial-token-$i")
             queue.offer(token) *>
               Console.printLine(s"[Queue] offered $token").orDie
           }

      _ <- ZIO.foreachDiscard(
             List("askForShipmentNotifications", "askForStockUpdates", "dispatchProducts")
           )(name => makeStorelogixRequest(name, queue).fork)

      _ <- ZIO.sleep(30.seconds)
    } yield ()
}
