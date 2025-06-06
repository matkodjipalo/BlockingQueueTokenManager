# BlockingQueueSingleSlot

A Scala application built with ZIO that demonstrates the use of a bounded queue and semaphore to manage token-based requests.

## Features

- Simulates token validation and fetching with retry and fallback mechanisms.
- Implements a bounded queue to manage tokens.
- Uses a semaphore to control access to shared resources.
- Demonstrates ZIO's concurrency and error-handling capabilities.

## Project Structure

- **`BlockingQueueSingleSlot`**: The main object containing the application logic.
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