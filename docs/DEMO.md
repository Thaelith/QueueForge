# Local Demo Guide

Step-by-step to run and explore QueueForge end-to-end.

## Prerequisites

- Java 21+
- Docker & Docker Compose
- Node.js 18+ (for dashboard)
- curl

## 1. Start PostgreSQL

```bash
docker compose up -d postgres
```

Wait for healthcheck: `pg_isready -U queueforge -d queueforge`

## 2. Start the Backend

```bash
./gradlew bootRun
```

Backend starts on `http://localhost:8080`.

Verify:
```bash
curl http://localhost:8080/actuator/health
# Expected: {"status":"UP"}
```

## 3. Start the Dashboard

```bash
cd dashboard
npm install
npm run dev
```

Dashboard starts on `http://localhost:5173`.

## 4. Seed Demo Jobs

**PowerShell:**
```powershell
.\scripts\demo-seed.ps1
```

**macOS/Linux:**
```bash
chmod +x scripts/demo-seed.sh
./scripts/demo-seed.sh
```

This submits 4 successful jobs + 1 failing job, then runs the worker twice to process them.

## 5. Open the Dashboard

http://localhost:5173

- **Overview** — see total jobs, pending, running, completed, dead-lettered
- **Jobs** — filterable job list, click any row for detail
- **Job Detail** — view metadata, raw JSON payload, **event timeline**, and perform actions (requeue, cancel, retry-now)
- **Dead Letter** — view all dead-lettered jobs, click to requeue directly
- **Queues** — per-queue stats table
- **Workers** — worker visibility and configuration

## 6. Inspect the Job Detail Timeline

1. Go to Jobs page
2. Click on the failed/dead-lettered job
3. See the event timeline: CREATED → LEASED → FAILED → RETRY_SCHEDULED → LEASED → FAILED → DEAD_LETTERED
4. Click **Requeue** to send it back to PENDING
5. Events update in real-time on page refresh

## 7. Request the Dead Letter Queue

View all permanently failed jobs at `/dead-letter`. Click **Requeue** to send one back for retry.

## 8. View Metrics

```bash
# Prometheus endpoint
curl http://localhost:8080/actuator/prometheus | grep queueforge

# Swagger UI
open http://localhost:8080/swagger-ui.html
```

## 9. Optional: Start Observability Stack

```bash
docker compose --profile observability up -d
```

- Prometheus: `http://localhost:9090`
- Grafana: `http://localhost:3000` (admin / admin)

The QueueForge Grafana dashboard auto-loads with job rates, worker polls, execution duration, and JVM memory.

## 10. Run All in Docker

```bash
docker compose -f docker-compose.full.yml up --build
```

This starts PostgreSQL, the app, Prometheus, and Grafana in one command.

## Cleanup

```bash
docker compose down -v
```
