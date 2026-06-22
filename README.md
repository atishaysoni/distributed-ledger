# distributed-ledger

Distributed financial transaction ledger built with Java and Spring Boot. Uses Apache Kafka for async event streaming and PostgreSQL with pessimistic locking to prevent race conditions, double-spending, and deadlocks under concurrent load.

## How it works

A transfer request hits the REST API, which validates it, publishes an event to Kafka, and returns immediately with a 202 and an idempotency key — the actual transfer hasn't happened yet. A Kafka consumer (3 parallel threads, matching the topic's partition count) picks up the event and calls into the transaction service, which locks both accounts with `SELECT ... FOR UPDATE`, checks the balance, and commits the debit and credit in one transaction.

Locks are always acquired in sorted UUID order. If two transfers run concurrently in opposite directions between the same two accounts, both threads try to lock the same account first — so one waits instead of deadlocking.

Each transfer carries an idempotency key. If Kafka redelivers the same event — a real possibility under at-least-once delivery — the consumer checks for an existing transaction with that key and skips reprocessing instead of debiting twice. Transfers that fail for business reasons (insufficient funds, unknown account) go to a dead letter topic rather than blocking the partition with retries.

`GET /api/v1/transactions/{idempotencyKey}` can return 404 right after submission — that's expected. The API is async by design; a 404 just means the consumer hasn't caught up yet, not that anything failed.

## Stack

- Java 21
- Spring Boot 4.1
- Apache Kafka (KRaft mode, no Zookeeper)
- PostgreSQL 16
- Docker / Docker Compose

## Running locally

```bash
docker compose up --build
```

```bash
curl http://localhost:8080/health
```

## API

| Method | Path | Description |
|---|---|---|
| `POST` | `/api/v1/accounts` | Create an account |
| `GET` | `/api/v1/accounts/{id}` | Check balance |
| `POST` | `/api/v1/transactions` | Submit a transfer — async, returns 202 |
| `GET` | `/api/v1/transactions/{idempotencyKey}` | Check transfer status |

## Tests

```bash
./mvnw test
```

Covers the locking logic directly, including a 2,000-transfer concurrent stress test on two accounts alternating direction, plus the REST layer end to end. No mocks for the database or Kafka — tests run against real local instances.

## Load test

```bash
docker compose --profile bench run --rm bench
```

A standalone Java program — no Maven dependencies, just `java.net.http.HttpClient` — creates 100 accounts, fires concurrent transfers from 50 threads for 20 seconds, then polls every account's balance until the total stabilizes before checking that nothing was created or destroyed. That conservation check, not raw throughput, is the actual proof the locking and idempotency logic hold up under real contention.

## Benchmark

```
workers           50
accounts          100
submitted         16319
failed            0
submission tps    816
p50 latency       49ms
p99 latency       223ms

accounts changed  96 / 100
BALANCE CONSERVED — no money created or destroyed
```

Submission TPS reflects the API/Kafka ingestion layer, not end-to-end processing speed — that's the point of the async design. The API accepts requests fast; the consumer drains the queue behind it at its own sustainable rate.

## Structure

```
src/main/java/com/atishaysoni/ledger/
  controller/      REST endpoints, global exception handler
  service/         account + transaction logic, pessimistic locking
  repository/      Spring Data JPA repositories
  model/           Account, Transaction entities
  kafka/           producer, consumer, event DTO
  dto/             request/response objects
  config/          Kafka topic config

src/main/resources/db/migration/   Flyway SQL migrations
benchmark/LoadTest.java            standalone concurrent load test
```