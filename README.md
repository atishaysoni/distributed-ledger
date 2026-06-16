# distributed-ledger

Distributed financial transaction ledger built with Java and Spring Boot. Uses Apache Kafka for async event streaming and PostgreSQL with pessimistic locking to prevent race conditions, double-spending, and deadlocks under concurrent load.

## Stack

- Java 21
- Spring Boot 3
- Apache Kafka
- PostgreSQL 16
- Docker / Docker Compose

## Running locally

```bash
docker compose up --build
```

```bash
curl http://localhost:8080/health
```