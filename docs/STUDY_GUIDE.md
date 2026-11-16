# PayLedger 4-week study guide

Use this guide to walk the finished codebase yourself. Each week lists concepts, files to read in order, exercises, self-test questions, and a LinkedIn post draft angle. Prefer reading tests before writing new code.

Demo logins (local / Compose only):

- Admin: `admin` / `admin-change-me`
- Merchant: `merchant` / `merchant-change-me`

---

## Week 1 — Milestone 1 foundations (JPA ledger core)

### Concepts
- `@MappedSuperclass` auditing, `@Embeddable` money, `@Version` optimistic locking
- Derived queries, JPQL `@Query`, Specifications, interface projections, entity graphs
- Idempotent transfers and Problem Details errors

### Read in order
1. `src/main/java/com/payledger/domain/Money.java`
2. `src/main/java/com/payledger/domain/AuditableEntity.java`
3. `src/main/java/com/payledger/domain/Wallet.java`
4. `src/main/java/com/payledger/domain/LedgerTransaction.java`
5. `src/main/resources/db/migration/V1__init_schema.sql`
6. `src/main/java/com/payledger/repository/TransactionRepository.java`
7. `src/main/java/com/payledger/repository/TransactionSpecs.java`
8. `src/main/java/com/payledger/service/TransferExecutor.java`
9. `src/main/java/com/payledger/service/TransferService.java`
10. `src/test/java/com/payledger/service/TransferServiceIntegrationTest.java`

### Exercises
1. Add a derived query `findByStatusAndCreatedAtAfter` and a failing-then-passing test.
2. Change the concurrency test amount and predict which assertion breaks.
3. Draw the transfer sequence (read wallets → debit/credit → save → outbox) from memory.

### Self-test
- Why is `spring.jpa.open-in-view=false` important here?
- What happens if two transfers race without `@Version`?
- Why is the idempotency key unique instead of only checked in memory?

### LinkedIn post
“Why `@Version` belongs on a wallet balance” — show the double-spend Testcontainers test name and a 5-line snippet from `Wallet`.

---

## Week 2 — Milestone 2 (refunds & reconciliation)

### Concepts
- Reversing a transfer without inventing a second ledger
- Partial refunds and remaining-refundable checks
- Reports as aggregates over ledger rows

### Read in order
1. `src/main/resources/db/migration/V2__refunds.sql`
2. `src/main/java/com/payledger/service/RefundExecutor.java`
3. `src/main/java/com/payledger/service/RefundService.java`
4. `src/main/java/com/payledger/exception/OverRefundException.java`
5. `src/main/java/com/payledger/service/ReconciliationService.java`
6. Reconciliation native query in `TransactionRepository`
7. `src/main/java/com/payledger/web/ReportController.java`
8. `src/test/java/com/payledger/service/RefundAndReconciliationIntegrationTest.java`

### Exercises
1. Implement a second partial refund until the remaining amount is zero; assert over-refund still fails.
2. Add a report field for net movement (`transfersIn + refunds - transfersOut - debits`) in the DTO only.
3. Explain in one paragraph why refunds keep the original `correlation_id`.

### Self-test
- Which wallet is debited on refund, and why?
- How does the report derive opening balance from closing balance and period net?
- Where is “no double full-refund” enforced?

### LinkedIn post
“Reconciliation is just the ledger in a trench coat” — paste the report endpoint path and one SQL aggregate from the native query.

---

## Week 3 — Milestone 3 (webhooks, outbox, optional Kafka)

### Concepts
- Transactional outbox in the same DB transaction as the ledger write
- Signed HTTP delivery, backoff, dead-letter, manual retry
- Kafka as optional fan-out behind a Spring profile (off by default)

### Read in order
1. `src/main/resources/db/migration/V3__webhooks_outbox.sql`
2. `src/main/java/com/payledger/domain/WebhookEndpoint.java`
3. `src/main/java/com/payledger/domain/WebhookDelivery.java`
4. `src/main/java/com/payledger/webhook/WebhookOutboxService.java`
5. `src/main/java/com/payledger/webhook/WebhookDispatchWorker.java`
6. `src/main/java/com/payledger/webhook/WebhookDispatcher.java`
7. `src/main/java/com/payledger/webhook/NoOpWebhookEventPublisher.java`
8. `src/main/java/com/payledger/webhook/KafkaWebhookEventPublisher.java`
9. `src/main/resources/application-kafka.yml`
10. `src/test/java/com/payledger/webhook/WebhookOutboxIntegrationTest.java`
11. `src/test/java/com/payledger/webhook/KafkaWebhookPublisherIntegrationTest.java`

### Exercises
1. Register a webhook with MockWebServer that fails three times, then succeeds after admin retry.
2. Toggle `SPRING_PROFILES_ACTIVE=kafka` locally and confirm both outbox row and Kafka message exist.
3. Write down what would break if webhooks were sent *before* the ledger commit.

### Self-test
- Why is the publisher a no-op by default?
- Who is the source of truth for delivery attempts — Kafka or `webhook_deliveries`?
- How is `X-PayLedger-Signature` produced?

### LinkedIn post
“Transactional outbox before Kafka” — contrast `WebhookOutboxService` with `KafkaWebhookEventPublisher` and say which one local demos need.

---

## Week 4 — Milestones 4–5 (JWT security, Redis, metrics, deploy)

### Concepts
- Stateless JWT auth, `MERCHANT` vs `ADMIN`, merchant-scoped access checks
- Cache-aside for merchant balance aggregates + eviction on ledger writes
- Micrometer timers/counters and Prometheus scrape
- Prod profile and env-based config

### Read in order
1. `src/main/resources/db/migration/V4__users.sql`
2. `src/main/java/com/payledger/security/SecurityConfig.java`
3. `src/main/java/com/payledger/security/JwtService.java`
4. `src/main/java/com/payledger/security/JwtAuthenticationFilter.java`
5. `src/main/java/com/payledger/security/MerchantAccessService.java`
6. `src/main/java/com/payledger/web/AuthController.java`
7. `src/test/java/com/payledger/security/SecurityIntegrationTest.java`
8. `src/main/java/com/payledger/service/CacheEvictionService.java`
9. `src/main/java/com/payledger/config/RedisCacheConfig.java`
10. `src/main/java/com/payledger/metrics/PayLedgerMetrics.java`
11. `src/main/resources/application-prod.yml`
12. `docs/DEPLOY.md`
13. `src/test/java/com/payledger/service/MerchantBalanceCacheIntegrationTest.java`
14. `src/test/java/com/payledger/service/RedisCacheIntegrationTest.java`

### Exercises
1. Call a transfer without a token (expect 401) and with the merchant token against another merchant’s wallet (expect 403).
2. Hit `/actuator/prometheus` after a transfer and find `payledger_transfer_duration` / optimistic-lock counters.
3. Run Compose with Redis profile and confirm a merchant balance cache hit after a warm read.

### Self-test
- Where is cross-merchant access blocked — filter, method security, or service?
- Why cache `BigDecimal` balances instead of `Wallet` entities in Redis?
- Which env vars must change before any non-demo deploy?

### LinkedIn post
“JWT boundary + metrics recruiters can ask about” — show the 403 test name and one Micrometer counter name from `PayLedgerMetrics`.

---

## Suggested weekly rhythm

| Day | Activity |
|-----|----------|
| Mon | Read the week’s files; run `mvn test -Dtest=...` for that milestone |
| Tue–Wed | Do the exercises; keep commits small on a private branch |
| Thu | Answer self-test questions in writing |
| Fri | Draft and publish the LinkedIn post; note one thing you would change |

Keep UK English in your notes. Do not invent traffic numbers or download badges when you write publicly about this project.
