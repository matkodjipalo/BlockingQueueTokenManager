# BlockingQueueTokenManager

A Scala application built with ZIO that demonstrates the use of bounded queues and semaphores to manage token-based requests. The project includes two implementations: `BlockingQueueSingleSlot` and `BlockingQueueTokenPool`.

## Features

### BlockingQueueSingleSlot
- Manages a single token in a bounded queue.
- Simulates token validation and fetching with retry and fallback mechanisms.
- Uses a semaphore to control access to shared resources.
- Demonstrates ZIO's concurrency and error-handling capabilities.

### BlockingQueueTokenPool
- Manages a pool of tokens in a bounded queue.
- Pre-populates the queue with multiple tokens.
- Simulates token validation and fetching with retry and fallback mechanisms.
- Uses a semaphore to control access to shared resources.
- Demonstrates ZIO's concurrency and scalability.

## Project Structure

- **`BlockingQueueSingleSlot`**: Contains the logic for managing a single token.
- **`BlockingQueueTokenPool`**: Contains the logic for managing a pool of tokens.
- **Token Management**: Simulates token fetching, validation, and fallback mechanisms.
- **Request Workers**: Simulates multiple workers making requests using tokens.

## Requirements

- Scala 3.3.6
- SBT (Scala Build Tool)
- ZIO library

## How to Run

1. Clone the repository:
   ```bash
   git clone <repository-url>
   cd <repository-folder>