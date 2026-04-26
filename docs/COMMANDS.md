# Commands Reference

Copy-paste commands for common tasks. Works on macOS, Linux, and Windows (PowerShell).

## Backend

### Build and Test
```bash
# Unit tests (no Docker needed)
./gradlew test

# Integration tests (Docker required)
./gradlew integrationTest

# All tests
./gradlew clean test integrationTest

# Build fat JAR
./gradlew bootJar
```

### Run Locally
```bash
# Start PostgreSQL
docker compose up -d postgres

# Run backend
./gradlew bootRun
```

### Docker
```bash
# Database only
docker compose up -d postgres

# Database + app
docker compose up --build app

# Full observability stack
docker compose --profile observability up --build

# All-in-one (compose variant)
docker compose -f docker-compose.full.yml up --build

# Tear down
docker compose down -v
```

## Dashboard

```bash
cd dashboard

# Install dependencies
npm install

# Development server (http://localhost:5173)
npm run dev

# Production build
npm run build
```

## Demo

```bash
# Seed demo data (PowerShell)
.\scripts\demo-seed.ps1

# Seed demo data (Unix)
chmod +x scripts/demo-seed.sh
./scripts/demo-seed.sh
```

## API (curl)

```bash
BASE="http://localhost:8080"

# Submit a job
curl -X POST $BASE/api/v1/jobs -H "Content-Type: application/json" \
  -d '{"queue":"default","type":"example.log","payload":{"msg":"hello"}}'

# List jobs
curl "$BASE/api/v1/jobs?status=PENDING&size=5"

# Get job detail
curl $BASE/api/v1/jobs/{jobId}

# Run worker once
curl -X POST $BASE/api/v1/workers/worker-1/run-once

# Dashboard summary
curl $BASE/api/v1/admin/dashboard/summary

# Queue stats
curl $BASE/api/v1/admin/queues/stats

# Worker list
curl $BASE/api/v1/admin/workers

# Job event timeline
curl $BASE/api/v1/admin/jobs/{jobId}/events

# Requeue dead-letter job
curl -X POST $BASE/api/v1/admin/jobs/{jobId}/requeue \
  -H "Content-Type: application/json" -d '{"resetAttempts":true}'

# Cancel job
curl -X POST $BASE/api/v1/admin/jobs/{jobId}/cancel

# Retry now
curl -X POST $BASE/api/v1/admin/jobs/{jobId}/retry-now \
  -H "Content-Type: application/json" -d '{"resetAttempts":true}'

# Health
curl $BASE/actuator/health

# Prometheus metrics
curl $BASE/actuator/prometheus | grep queueforge
```

## Useful URLs

| Service | URL |
|---------|-----|
| Dashboard | http://localhost:5173 |
| Backend | http://localhost:8080 |
| Swagger UI | http://localhost:8080/swagger-ui.html |
| Health | http://localhost:8080/actuator/health |
| Prometheus | http://localhost:9090 |
| Grafana | http://localhost:3000 (admin / admin) |
