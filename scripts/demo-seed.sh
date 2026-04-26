#!/usr/bin/env bash
# QueueForge Demo Seed Script
# Prerequisites: backend running on localhost:8080
set -e

BASE="http://localhost:8080"
WORKER="worker-1"

echo "=== QueueForge Demo Seed ==="
echo ""

# Successful jobs
echo ">>> Submitting example.log job..."
curl -s -X POST "$BASE/api/v1/jobs" -H "Content-Type: application/json" \
  -d '{"queue":"default","type":"example.log","payload":{"msg":"Hello from seed script"}}' | python3 -m json.tool 2>/dev/null || true

echo ">>> Submitting example.email job..."
curl -s -X POST "$BASE/api/v1/jobs" -H "Content-Type: application/json" \
  -d '{"queue":"default","type":"example.email","payload":{"to":"user@example.com","subject":"Welcome"}}' | python3 -m json.tool 2>/dev/null || true

echo ">>> Submitting example.webhook job..."
curl -s -X POST "$BASE/api/v1/jobs" -H "Content-Type: application/json" \
  -d '{"queue":"default","type":"example.webhook","payload":{"url":"https://example.com/hook","method":"POST","body":{}}}' | python3 -m json.tool 2>/dev/null || true

echo ">>> Submitting example.report job..."
curl -s -X POST "$BASE/api/v1/jobs" -H "Content-Type: application/json" \
  -d '{"queue":"default","type":"example.report","payload":{"reportName":"daily-summary","format":"pdf"}}' | python3 -m json.tool 2>/dev/null || true

# Failing job (will become DEAD_LETTERED after 2 run-once calls)
echo ">>> Submitting example.fail job (maxAttempts=2)..."
curl -s -X POST "$BASE/api/v1/jobs" -H "Content-Type: application/json" \
  -d '{"queue":"default","type":"example.fail","payload":{"message":"Simulated failure"},"maxAttempts":2}' | python3 -m json.tool 2>/dev/null || true

echo ""
echo ">>> Processing jobs (run-once #1)..."
curl -s -X POST "$BASE/api/v1/workers/$WORKER/run-once" | python3 -m json.tool 2>/dev/null || true

echo ""
echo ">>> Processing jobs (run-once #2)..."
curl -s -X POST "$BASE/api/v1/workers/$WORKER/run-once" | python3 -m json.tool 2>/dev/null || true

echo ""
echo "=== Done ==="
echo ""
echo "Useful URLs:"
echo "  Dashboard    http://localhost:5173"
echo "  Swagger UI   http://localhost:8080/swagger-ui.html"
echo "  Health       http://localhost:8080/actuator/health"
echo "  Prometheus   http://localhost:9090  (if observability profile active)"
echo "  Grafana      http://localhost:3000  (admin/admin - if observability profile active)"
echo ""
