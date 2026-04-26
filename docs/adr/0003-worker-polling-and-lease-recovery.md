# ADR-0003: Worker Polling and Lease Recovery Strategy

## Context

Workers need to discover available jobs and recover jobs from crashed workers. We must choose between push-based notification (`LISTEN/NOTIFY`), polling, or a hybrid approach.

Options considered:
1. Continuous polling with `@Scheduled` in Spring Boot
2. PostgreSQL `LISTEN/NOTIFY` push notifications
3. Hybrid: polling with adaptive intervals

## Decision

Use a configurable fixed-delay `@Scheduled` polling loop for the MVP.

**Why this approach:**
- Simpler to implement, test, and configure
- No connection management for LISTEN/NOTIFY
- Poll interval is configurable (default 1s, tunable per workload)
- `maxJobsPerPoll` prevents overloading a single worker cycle
- Recovery uses a separate schedule (default 30s) for expired leases

## Consequences

**Positive:**
- Predictable database load per worker instance
- Easy to tune: interval, batch size, lease duration
- No stale connection issues (LISTEN/NOTIFY can disconnect silently)
- Works identically in Docker, local dev, and CI

**Negative:**
- Fixed waste: polls when queue is empty burn CPU/DB
- Latency floor: job waits at least one poll interval before processing
- Under high load, multiple workers polling simultaneously create spike traffic

## Future Considerations

- `LISTEN/NOTIFY` as a Phase 8 enhancement for 100ms latency targets
- Adaptive polling: increase interval when queue is empty, decrease when busy
- Leader election for coordinated recovery (ZooKeeper or DB advisory lock)

## Status

Accepted. Phase 3 implementation.
