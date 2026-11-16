# PayLedger build plan (milestones 2–5)

Milestone 1 ships a runnable ledger core. The steps below are for the owner to implement as a learning portfolio — not as unfinished work hidden in this PR. Each milestone should stay mergeable, tested, and documented in UK English.

## Milestone 2 — Refunds and reconciliation

**Goal:** Reverse a completed transfer safely and produce an operator-friendly reconciliation view.

Suggested steps:

1. Add a `RefundService` that creates a `REFUND` ledger entry, credits the original source wallet, and debits the destination (or a designated settlement wallet).
2. Link refunds to the original transfer via `correlation_id` (already on `transactions`). Enforce “refund ≤ original amount” and “no double full-refund” in the domain.
3. Introduce a dedicated idempotency key for refunds (reuse the unique constraint pattern from transfers).
4. Build a reconciliation report endpoint, e.g. `GET /api/v1/reports/reconciliation?merchantId=&from=&to=`, returning opening balance, credits, debits, refunds, closing balance per wallet (JPQL aggregates or a native query with a DTO projection).
5. Flyway `V2__refunds_and_reports.sql` only if new tables/indexes are required (prefer reusing `transactions`).
6. Testcontainers coverage: happy-path refund, over-refund rejection, report totals match seeded ledger rows.

**Done when:** a transfer can be refunded idempotently; report numbers match the ledger; tests pass under `mvn verify`.

## Milestone 3 — Webhooks with retry (optional Kafka)

**Goal:** Notify merchants of completed transfers/refunds with at-least-once delivery semantics.

Suggested steps:

1. Add `webhook_endpoints` and `webhook_deliveries` tables (URL, secret, event type, attempt count, next_attempt_at, status).
2. On transfer/refund completion, enqueue a delivery row in the same transaction as the ledger write (transactional outbox style).
3. Implement a scheduled dispatcher (`@Scheduled` or Spring Retry) that POSTs signed payloads, backs off on failure, and caps attempts.
4. Optional stretch: publish outbox events to Kafka and consume them with a worker module — keep the HTTP dispatcher as a fallback so local demos stay simple.
5. Expose admin endpoints to list dead-letter deliveries and trigger a manual retry.
6. Tests: stub HTTP with MockWebServer/WireMock; prove retries increment and succeed after transient failure.

**Done when:** a successful transfer produces a delivered webhook within the retry policy; failures are observable and redriveable.

## Milestone 4 — Spring Security + JWT

**Goal:** Protect mutating APIs; keep actuator/health appropriately exposed.

Suggested steps:

1. Add `spring-boot-starter-security` and a minimal JWT issuer (login endpoint for demo merchants, or API keys mapped to merchants).
2. Secure `POST /transfers`, refunds, and webhook admin routes; leave OpenAPI and `GET /actuator/health` reachable as you prefer for demos.
3. Map authentication principal → `merchantId` and enforce that wallet operations stay within that merchant (defence in depth beyond JWT validity).
4. Document token acquisition in the README; add a security integration test that expects `401` without a token and `403` cross-merchant.
5. Avoid over-building RBAC — one `MERCHANT` role is enough for the portfolio story.

**Done when:** unauthenticated writes fail; authenticated merchants cannot move another merchant’s wallets.

## Milestone 5 — Redis caching, Micrometer metrics, deploy

**Goal:** Show operational polish without pretending this is a multi-region production platform.

Suggested steps:

1. Cache merchant balance aggregates or wallet reads with Spring Cache + Redis; define a clear eviction point on transfer/refund.
2. Add Micrometer timers/counters for transfer latency, optimistic-lock retries, and webhook failures; scrape via Actuator Prometheus endpoint if you add the registry dependency.
3. Provide a sample deploy path: container image to a single VM or a PaaS of your choice, managed Postgres, env-based config only (no secrets in git).
4. Add a short `docs/DEPLOY.md` with required env vars, health check URL, and rollback notes.
5. Optional: Grafana dashboard JSON for the metrics you actually emit — no fake traffic claims.

**Done when:** a cache hit is demonstrable in logs/metrics after a warm read; deploy doc matches the Compose/env contract.

---

## LinkedIn post topics (tie each to a milestone)

Use these as draft angles. Prefer showing a small code snippet, a test name, or an ER fragment over buzzwords.

1. **Milestone 1 — “Why `@Version` belongs on a wallet balance”**  
   Walk through the double-spend Testcontainers test and how optimistic locking beats “check balance then update” without `SELECT FOR UPDATE` everywhere.

2. **Milestone 1 / 2 — “Money as an `@Embeddable`, not two loose columns”**  
   Explain currency mismatch prevention and `@AttributeOverride` for balance vs transaction amounts — a natural bridge into refunds.

3. **Milestone 2 — “Reconciliation is just the ledger in a trench coat”**  
   Show how a report endpoint is an aggregate over immutable (append-ish) transaction rows rather than a second source of truth.

4. **Milestone 3 — “Transactional outbox before Kafka”**  
   Argue that reliable webhooks start with an outbox row in the same DB transaction; Kafka is an optimisation, not the first move.

5. **Milestone 4 / 5 — “JWT boundary + metrics recruiters can ask about”**  
   Pair merchant-scoped authorisation with a Micrometer counter for optimistic-lock retries — operational and security storytelling in one thread.

---

## Working agreements for later milestones

- Keep UK English in docs and API descriptions.
- Do not invent usage metrics, download badges, or production claims.
- Prefer one vertical slice per PR: migration + domain + API + Testcontainers test + README note.
- Leave explicit `TODO` comments only where a future milestone is intentionally deferred — not as silent unfinished behaviour in Milestone 1 paths.
