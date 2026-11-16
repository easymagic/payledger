# PayLedger deploy notes

This is a single-node portfolio deploy path. It is not a claim of production scale.

## Required environment variables

| Variable | Purpose | Example |
|----------|---------|---------|
| `SPRING_PROFILES_ACTIVE` | Activate `prod` and optionally `redis` / `kafka` | `prod,redis` |
| `DB_HOST` / `DB_PORT` / `DB_NAME` / `DB_USER` / `DB_PASSWORD` | PostgreSQL | managed Postgres host |
| `REDIS_HOST` / `REDIS_PORT` | Redis (when `redis` profile is on) | `redis` / `6379` |
| `JWT_SECRET` | HMAC secret (≥ 32 bytes) | long random string |
| `DEMO_USERS_ENABLED` | Seed demo users | `false` in real deploys |
| `KAFKA_BOOTSTRAP_SERVERS` | Only with `kafka` profile | `broker:9092` |
| `SERVER_PORT` | HTTP port | `8080` |

Do not commit secrets. Pass them from the host, a secrets manager, or Compose env files that stay local.

## Health and metrics

- Liveness/readiness style check: `GET /actuator/health`
- Prometheus scrape: `GET /actuator/prometheus`
- OpenAPI (disable in hardened prod if you prefer): `/swagger-ui.html`

## Suggested path

1. Build the image: `docker build -t payledger:local .`
2. Run managed Postgres + Redis (Compose in this repo is fine for a lab VM).
3. Start the app with `SPRING_PROFILES_ACTIVE=prod,redis` and the env vars above.
4. Confirm Flyway applied (`flyway_schema_history`) and `/actuator/health` returns `UP`.
5. Create real users (or temporarily enable demo users once), obtain a JWT from `POST /api/v1/auth/login`, then exercise transfers.

## Rollback

1. Stop the new app containers/process.
2. Redeploy the previous image tag.
3. Flyway migrations in this project are additive (V1–V4). Rolling back application code to Milestone 1 without rolling back schema is usually safe; rolling schema backwards needs a deliberate down migration (not shipped here).

## Kafka (optional)

Leave Kafka off unless you are studying the outbox fan-out. The HTTP dispatcher remains the delivery worker. Activate with `SPRING_PROFILES_ACTIVE=...,kafka`.
