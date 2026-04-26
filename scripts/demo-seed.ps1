# QueueForge Demo Seed Script (PowerShell)
# Prerequisites: backend running on localhost:8080

$Base = "http://localhost:8080"
$Worker = "worker-1"

Write-Host "=== QueueForge Demo Seed ==="
Write-Host ""

# Successful jobs
Write-Host ">>> Submitting example.log job..."
Invoke-RestMethod -Uri "$Base/api/v1/jobs" -Method Post -ContentType "application/json" -Body '{"queue":"default","type":"example.log","payload":{"msg":"Hello from seed script"}}' | ConvertTo-Json

Write-Host ">>> Submitting example.email job..."
Invoke-RestMethod -Uri "$Base/api/v1/jobs" -Method Post -ContentType "application/json" -Body '{"queue":"default","type":"example.email","payload":{"to":"user@example.com","subject":"Welcome"}}' | ConvertTo-Json

Write-Host ">>> Submitting example.webhook job..."
Invoke-RestMethod -Uri "$Base/api/v1/jobs" -Method Post -ContentType "application/json" -Body '{"queue":"default","type":"example.webhook","payload":{"url":"https://example.com/hook","method":"POST","body":{}}}' | ConvertTo-Json

Write-Host ">>> Submitting example.report job..."
Invoke-RestMethod -Uri "$Base/api/v1/jobs" -Method Post -ContentType "application/json" -Body '{"queue":"default","type":"example.report","payload":{"reportName":"daily-summary","format":"pdf"}}' | ConvertTo-Json

# Failing job (will become DEAD_LETTERED after 2 run-once calls)
Write-Host ">>> Submitting example.fail job (maxAttempts=2)..."
Invoke-RestMethod -Uri "$Base/api/v1/jobs" -Method Post -ContentType "application/json" -Body '{"queue":"default","type":"example.fail","payload":{"message":"Simulated failure"},"maxAttempts":2}' | ConvertTo-Json

Write-Host ""
Write-Host ">>> Processing jobs (run-once #1)..."
Invoke-RestMethod -Uri "$Base/api/v1/workers/$Worker/run-once" -Method Post | ConvertTo-Json

Write-Host ""
Write-Host ">>> Processing jobs (run-once #2)..."
Invoke-RestMethod -Uri "$Base/api/v1/workers/$Worker/run-once" -Method Post | ConvertTo-Json

Write-Host ""
Write-Host "=== Done ==="
Write-Host ""
Write-Host "Useful URLs:"
Write-Host "  Dashboard    http://localhost:5173"
Write-Host "  Swagger UI   http://localhost:8080/swagger-ui.html"
Write-Host "  Health       http://localhost:8080/actuator/health"
Write-Host "  Prometheus   http://localhost:9090  (if observability profile active)"
Write-Host "  Grafana      http://localhost:3000  (admin/admin - if observability profile active)"
Write-Host ""
