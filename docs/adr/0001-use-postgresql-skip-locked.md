# ADR-0001: Use PostgreSQL SKIP LOCKED for Job Leasing

## Context

QueueForge needs multiple concurrent workers to claim jobs without conflicts or an external queue broker (Redis, Kafka). Each job must be processed by exactly one worker at a time, and workers must never block waiting for each other.

Options considered:
1. External message broker (Redis streams, RabbitMQ)
2. PostgreSQL with row-level locking (`SELECT ... FOR UPDATE`)
3. PostgreSQL with `SELECT ... FOR UPDATE SKIP LOCKED`

## Decision

Use PostgreSQL `SELECT ... FOR UPDATE SKIP LOCKED` in a CTE that atomically selects and updates a single row.

```sql
WITH selected AS (
    SELECT id FROM jobs WHERE status IN ('PENDING', 'RETRY_SCHEDULED') ... LIMIT 1
    FOR UPDATE SKIP LOCKED
)
UPDATE jobs SET status = 'RUNNING', locked_by = ?, ... FROM selected WHERE jobs.id = selected.id
RETURNING jobs.*;
```

**Why this approach:**
- Single database is already the source of truth — no sync gap between queue and DB
- `SKIP LOCKED` is PostgreSQL's built-in lock-free work queue pattern
- No external infrastructure (Redis, Kafka) needed
- Transactional: lease is atomic with the row lock
- `locked_until` provides crash-recovery timeout

## Consequences

**Positive:**
- Simple deployment — one database, no message broker
- Transactional consistency between job state and queue
- `RETURNING` clause eliminates separate SELECT
- Proven pattern (many production work queues use this)

**Negative:**
- Polling model creates database load (mitigated by configurable poll interval)
- PostgreSQL may not scale to Kafka-level throughput for very large systems
- Lease expiration recovery is at-least-once — handlers must be idempotent

## Status

Accepted. Phase 2 implementation.
