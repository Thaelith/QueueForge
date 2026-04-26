# Screenshot Checklist

Capture these screenshots for the GitHub README. Use the dark theme admin dashboard.

## Recommended Screenshots

### 1. Overview Dashboard
- URL: `http://localhost:5173/`
- Show: metric cards (total jobs, pending, running, completed, dead-letter), queues table, workers list
- File: `docs/assets/queueforge-overview.png`

### 2. Jobs List
- URL: `http://localhost:5173/jobs`
- Show: filterable job table with status badges (PENDING, COMPLETED, DEAD_LETTERED visible)
- File: `docs/assets/queueforge-jobs.png`

### 3. Job Detail with Event Timeline
- URL: `http://localhost:5173/jobs/{id}` (use a dead-lettered job)
- Show: metadata grid, JSON payload viewer, **event timeline** (CREATED → LEASED → FAILED → DEAD_LETTERED chain)
- File: `docs/assets/job-detail-timeline.png`

### 4. Dead Letter Queue
- URL: `http://localhost:5173/dead-letter`
- Show: dead-lettered jobs table with requeue button
- File: `docs/assets/dead-letter-queue.png`

### 5. Swagger UI
- URL: `http://localhost:8080/swagger-ui.html`
- Show: list of API endpoints under "Jobs", "Admin", "Worker" tags
- File: `docs/assets/swagger-ui.png`

### 6. Grafana Dashboard (optional)
- URL: `http://localhost:3000` (after `docker compose --profile observability up -d`)
- Show: QueueForge dashboard with job rate charts
- File: `docs/assets/grafana-dashboard.png`

## How to Take Screenshots

1. Run the full demo (see `docs/DEMO.md`)
2. Open each page in Chrome with address bar hidden (F11 or Cmd+Shift+F)
3. Crop to content area (exclude browser chrome)
4. Save as PNG, 2x resolution for retina

## Placeholder Note

Current screenshots in README are placeholders. Replace `docs/assets/` images with actual screenshots after running the demo.
