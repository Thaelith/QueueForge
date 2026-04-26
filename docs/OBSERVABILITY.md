# Observability

## Metrics (Micrometer → Prometheus)

| Metric | Type | Description |
|--------|------|-------------|
| `queueforge_jobs_leased_total` | Counter | Jobs leased |
| `queueforge_jobs_completed_total` | Counter | Jobs completed |
| `queueforge_jobs_failed_total` | Counter | Jobs that failed and scheduled retry |
| `queueforge_jobs_dead_lettered_total` | Counter | Jobs permanently failed |
| `queueforge_jobs_recovered_total` | Counter | Expired leases recovered |
| `queueforge_worker_polls_total` | Counter | Poll cycles executed |
| `queueforge_worker_poll_errors_total` | Counter | Poll cycles that errored |
| `queueforge_job_execution_duration_seconds` | Timer | Handler execution duration |

All exposed at `/actuator/prometheus`.

## Prometheus Setup

```bash
docker compose --profile observability up -d prometheus
```

Prometheus scrapes the app on port 8080 at `/actuator/prometheus`.

Access: `http://localhost:9090`

## Grafana Setup

```bash
docker compose --profile observability up -d grafana
```

Pre-provisioned with:
- Prometheus datasource
- QueueForge dashboard (job rates, worker polls, execution duration, JVM memory)

Access: `http://localhost:3000` (admin/admin)

## Structured Logging

Worker logs use MDC (Mapped Diagnostic Context) to include contextual fields:

```
[workerId=local-worker-1][jobId=abc-123] status=COMPLETED durationMs=42
```

MDC fields: `jobId`, `workerId`, `queueName`, `jobType`, `attempt`

Log pattern configured in `application.yml`:
```yaml
logging:
  pattern:
    console: "%d{HH:mm:ss.SSS} %-5level [%thread] %logger{36} [%X{workerId}][%X{jobId}] %msg%n"
```

MDC is set before execution and cleared in `finally` block — no cross-job leakage.

## Health

Standard Spring Boot Actuator health endpoint at `/actuator/health`.

Includes database connectivity check.
