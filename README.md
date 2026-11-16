# PayLedger

PayLedger is a public portfolio project: a payments and wallet API built with **Java 21**, **Spring Boot 3** and **Spring Data JPA**.

It is aimed at recruiters and hiring managers who want evidence of solid JPA design in a payments-flavoured domain — auditing, optimistic locking, embeddable money, Specifications, and Testcontainers — rather than a claim of production traffic.

Milestone 1 (this repository) is intentionally small and runnable. Later milestones are left as TODOs for the owner to implement as a learning path; see [docs/BUILD_PLAN.md](docs/BUILD_PLAN.md).

## Features (Milestone 1)

- Merchant → Customer → Wallet domain with ledger transactions (`TRANSFER`, `CREDIT`, `DEBIT`, `REFUND` types modelled)
- `Money` as a JPA `@Embeddable` (`BigDecimal` amount + ISO-4217 currency)
- Shared auditable base entity (`@CreatedDate` / `@LastModifiedDate`)
- Wallet `@Version` optimistic locking with transfer retry / clear conflict errors
- Idempotent transfers via a unique `idempotency_key`
- Spring Data repositories: derived queries, JPQL `@Query`, Specification search with pagination, interface projection
- REST API with Bean Validation and RFC 7807 Problem Details
- Flyway migrations aligned with the entities
- Actuator health/info/metrics endpoints
- OpenAPI UI via springdoc
- Testcontainers PostgreSQL integration tests, including a concurrency / double-spend case
- Docker Compose (Postgres + app) and GitHub Actions `mvn verify`

## Architecture

```
Client
  │
  ▼
REST controllers  ──►  services (@Transactional)  ──►  Spring Data repositories
                              │
                              ▼
                     JPA entities + Flyway / PostgreSQL
```

Package layout under `com.payledger`:

| Package        | Role                                      |
|----------------|-------------------------------------------|
| `domain`       | Entities, enums, `Money` embeddable       |
| `repository`   | Spring Data JPA + Specifications          |
| `service`      | Transfer / wallet / query application logic |
| `web`          | Controllers, DTOs, global exception handler |
| `config`       | JPA auditing, OpenAPI                     |

## Domain (ER)

```mermaid
erDiagram
    MERCHANTS ||--o{ CUSTOMERS : has
    CUSTOMERS ||--|| WALLETS : owns
    WALLETS ||--o{ TRANSACTIONS : posts
    WALLETS ||--o{ TRANSACTIONS : "counterparty"

    MERCHANTS {
        uuid id PK
        string name
        string email UK
        string status
        timestamptz created_at
        timestamptz updated_at
    }
    CUSTOMERS {
        uuid id PK
        uuid merchant_id FK
        string external_ref
        string name
        string email
        timestamptz created_at
        timestamptz updated_at
    }
    WALLETS {
        uuid id PK
        uuid customer_id FK UK
        numeric balance_amount
        string balance_currency
        bigint version
        timestamptz created_at
        timestamptz updated_at
    }
    TRANSACTIONS {
        uuid id PK
        uuid wallet_id FK
        uuid counterparty_wallet_id FK
        string type
        string status
        numeric amount
        string currency
        string idempotency_key UK
        string description
        uuid correlation_id
        timestamptz created_at
        timestamptz updated_at
    }
```

## Why these JPA choices

**Auditing (`@CreatedDate` / `@LastModifiedDate`)**  
Payments support work always asks “when did this change?” Centralising timestamps on a `@MappedSuperclass` keeps every table consistent without hand-rolled setters, and pairs cleanly with Flyway `TIMESTAMPTZ` columns.

**`@Version` on `Wallet`**  
Balances must not be overwritten by concurrent writers. Optimistic locking turns a double-spend race into an `OptimisticLockException`, which the transfer service retries a few times or surfaces as a Problem Details conflict. The integration suite includes a concurrency test that proves only one of two competing debits can win.

**`Money` as `@Embeddable`**  
Amount and currency travel together. That avoids “naked” `BigDecimal` columns and makes currency mismatch a domain error instead of a silent bug. `@AttributeOverride` maps the same embeddable onto wallet balance columns and transaction amount columns.

**N+1 avoidance with entity graphs / fetch joins**  
Associations default to `LAZY`. Wallet reads that need customer and merchant use `@EntityGraph`. Transaction search applies a fetch join inside the Specification when the query result type is the entity (not the count query). `spring.jpa.open-in-view` is `false` so lazy loads cannot hide in the view layer.

**Specifications**  
Filtered transaction search is composable and type-safe. Controllers stay thin; new filters are additional predicates rather than a combinatorial explosion of repository method names.

## API (quick reference)

| Method | Path | Purpose |
|--------|------|---------|
| `POST` | `/api/v1/wallets` | Provision merchant/customer/wallet (demo bootstrap) |
| `GET`  | `/api/v1/wallets/{id}` | Fetch wallet |
| `GET`  | `/api/v1/merchants/{id}/balance?currency=NGN` | JPQL sum of wallet balances |
| `POST` | `/api/v1/transfers` | Idempotent transfer |
| `GET`  | `/api/v1/transactions` | Specification search + pagination |
| `GET`  | `/api/v1/transactions/wallets/{id}/summaries` | Interface projection list |

Interactive docs: [http://localhost:8080/swagger-ui.html](http://localhost:8080/swagger-ui.html)  
OpenAPI JSON: [http://localhost:8080/api-docs](http://localhost:8080/api-docs)  
Health: [http://localhost:8080/actuator/health](http://localhost:8080/actuator/health)

### Example transfer

```bash
curl -s -X POST http://localhost:8080/api/v1/transfers \
  -H 'Content-Type: application/json' \
  -d '{
    "sourceWalletId": "<uuid>",
    "destinationWalletId": "<uuid>",
    "amount": 25.0000,
    "currency": "NGN",
    "idempotencyKey": "demo-transfer-001",
    "description": "Demo transfer"
  }'
```

Replaying the same `idempotencyKey` with the same payload returns the original transaction (`replayed: true`) instead of moving money twice.

## How to run

### Prerequisites

- Java 21+
- Maven 3.9+
- Docker (for Compose and Testcontainers)

### Tests

```bash
mvn verify
```

Uses Testcontainers to start PostgreSQL. Docker must be available to the test process.

### Local app (Compose)

```bash
docker compose up --build
```

Postgres listens on `5432`; the app on `8080`.

### Local app (Maven against Compose DB only)

```bash
docker compose up -d db
mvn spring-boot:run
```

Default DB settings are in `src/main/resources/application.yml` (`payledger` / `payledger` @ `localhost:5432/payledger`).

## Roadmap

| Milestone | Focus |
|-----------|--------|
| **1 (done)** | Core ledger domain, transfers, JPA patterns, tests, Docker/CI |
| **2** | Refunds and a reconciliation report |
| **3** | Outbound webhooks with retry (optional Kafka) |
| **4** | Spring Security + JWT |
| **5** | Redis caching, Micrometer metrics, deploy notes |

Detailed steps and LinkedIn post ideas: [docs/BUILD_PLAN.md](docs/BUILD_PLAN.md).

## Licence

MIT — see [LICENSE](LICENSE).
