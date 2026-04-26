# API Examples

All endpoints relative to `http://localhost:8080`.

## Submit a Job

```bash
curl -X POST /api/v1/jobs \
  -H "Content-Type: application/json" \
  -d '{"queue":"default","type":"example.email","payload":{"to":"u@e.com","subject":"Hi"},"maxAttempts":3}'
```

## Get Job

```bash
curl /api/v1/jobs/{jobId}
```

## List Jobs

```bash
curl "/api/v1/jobs?status=PENDING&queue=default&page=0&size=20"
```

## Worker Run-Once

```bash
curl -X POST /api/v1/workers/worker-1/run-once
```

## Worker Config

```bash
curl /api/v1/workers/config
```

## Dashboard Summary

```bash
curl /api/v1/admin/dashboard/summary
```

Response:
```json
{
  "totalJobs": 42,
  "pendingJobs": 15,
  "runningJobs": 2,
  "completedJobs": 20,
  "retryScheduledJobs": 3,
  "deadLetteredJobs": 2,
  "cancelledJobs": 0,
  "activeWorkers": 1,
  "completedLastHour": 8,
  "failedLastHour": 2,
  "jobsByQueue": {"default": 35, "emails": 7}
}
```

## Queue Stats

```bash
curl /api/v1/admin/queues/stats
```

## Workers

```bash
curl /api/v1/admin/workers
```

## Job Event Timeline

```bash
curl /api/v1/admin/jobs/{jobId}/events
```

## Admin Actions

```bash
# Requeue dead-lettered job
curl -X POST /api/v1/admin/jobs/{jobId}/requeue \
  -H "Content-Type: application/json" \
  -d '{"resetAttempts":true,"reason":"Fixed handler"}'

# Cancel pending job
curl -X POST /api/v1/admin/jobs/{jobId}/cancel

# Retry scheduled job immediately
curl -X POST /api/v1/admin/jobs/{jobId}/retry-now \
  -H "Content-Type: application/json" \
  -d '{"resetAttempts":false}'
```

## Demo Flow

```bash
# 1. Submit a job
JOB=$(curl -s -X POST /api/v1/jobs -H "Content-Type: application/json" \
  -d '{"queue":"default","type":"example.log","payload":{"msg":"hello"}}' | jq -r '.id')

# 2. Process one poll cycle
curl -s -X POST /api/v1/workers/worker-1/run-once | jq

# 3. View job events
curl -s /api/v1/admin/jobs/$JOB/events | jq

# 4. Submit failing job
FAIL_JOB=$(curl -s -X POST /api/v1/jobs -H "Content-Type: application/json" \
  -d '{"queue":"default","type":"example.fail","payload":{"message":"test"},"maxAttempts":2}' | jq -r '.id')

# 5. Process until dead-lettered (call run-once twice)
curl -s -X POST /api/v1/workers/worker-1/run-once | jq
curl -s -X POST /api/v1/workers/worker-1/run-once | jq

# 6. Verify dead-letter status
curl -s /api/v1/jobs/$FAIL_JOB | jq '.status'
# Output: "DEAD_LETTERED"

# 7. Requeue dead-lettered job
curl -s -X POST /api/v1/admin/jobs/$FAIL_JOB/requeue \
  -H "Content-Type: application/json" -d '{"resetAttempts":true}' | jq

# 8. Dashboard summary
curl -s /api/v1/admin/dashboard/summary | jq

# 9. Check metrics
curl -s /actuator/prometheus | grep queueforge
```
