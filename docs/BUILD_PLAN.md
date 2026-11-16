# PayLedger build plan

Milestones 1–5 are **implemented** in this repository so the owner can study a complete vertical slice. Use [STUDY_GUIDE.md](STUDY_GUIDE.md) for a 4-week reading path. Use this file as a map of what landed where.

## Milestone 1 — Ledger core (done)

- Domain: Merchant, Customer, Wallet, `LedgerTransaction`, `Money` embeddable, auditing, `@Version`
- Transfers with idempotency + optimistic-lock retry
- Specs / JPQL / projections / entity graphs
- Testcontainers, Docker, CI, README architecture notes

## Milestone 2 — Refunds and reconciliation (done)

- `RefundService` / `RefundExecutor`: reverse a COMPLETED `TRANSFER`, partial refunds, over-refund rejection
- `related_transaction_id` (Flyway `V2__refunds.sql`)
- `GET /api/v1/reports/reconciliation`
- Tests: `RefundAndReconciliationIntegrationTest`

## Milestone 3 — Webhooks with outbox (+ optional Kafka) (done)

- Tables: `webhook_endpoints`, `webhook_deliveries` (`V3__webhooks_outbox.sql`)
- Outbox enqueue in the same transaction as transfer/refund
- Scheduled HTTP dispatcher with HMAC signature, backoff, dead-letter, admin retry
- Kafka publisher behind profile `kafka` (off by default); HTTP dispatcher remains the delivery worker
- Tests: `WebhookOutboxIntegrationTest`, `KafkaWebhookPublisherIntegrationTest`

## Milestone 4 — Spring Security + JWT (done)

- Roles: `MERCHANT`, `ADMIN` (`V4__users.sql` + demo user bootstrap)
- `POST /api/v1/auth/login` issues JWTs
- Merchant-scoped wallet access; admin webhook routes
- Tests: `SecurityIntegrationTest` (401/403 cases)

## Milestone 5 — Redis, Micrometer, deploy readiness (done)

- Cache merchant balance aggregates; evict on ledger writes
- Redis profile + Compose Redis service
- Micrometer timers/counters + Prometheus endpoint
- `application-prod.yml`, [DEPLOY.md](DEPLOY.md)
- Tests: `MerchantBalanceCacheIntegrationTest`, `RedisCacheIntegrationTest`

---

## LinkedIn post topics (still useful)

1. Milestone 1 — “Why `@Version` belongs on a wallet balance”
2. Milestone 1 / 2 — “Money as an `@Embeddable`, not two loose columns”
3. Milestone 2 — “Reconciliation is just the ledger in a trench coat”
4. Milestone 3 — “Transactional outbox before Kafka”
5. Milestone 4 / 5 — “JWT boundary + metrics recruiters can ask about”

Full weekly prompts: [STUDY_GUIDE.md](STUDY_GUIDE.md).

---

## Working agreements

- UK English in docs and API descriptions
- No invented usage metrics, download badges, or production traffic claims
- Prefer vertical slices: migration + domain + API + Testcontainers test + docs note
