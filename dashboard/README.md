# QueueForge Admin Dashboard

React + TypeScript + Tailwind CSS admin dashboard for the QueueForge distributed job queue.

## Tech Stack

- React 19 + TypeScript
- Vite 6
- Tailwind CSS 4 (with Stitch design tokens)
- React Router 7

## Local Setup

```bash
npm install
npm run dev
```

Dashboard at `http://localhost:5173`.

## Environment

Create `.env` from `.env.example`:
```
VITE_API_BASE_URL=http://localhost:8080
```

Vite's dev server proxies `/api` and `/actuator` requests to the backend automatically.

## Pages

| Route | Page | Backend API |
|-------|------|-------------|
| `/` | Overview | dashboard/summary, queues/stats, workers |
| `/jobs` | Jobs List | GET /api/v1/jobs |
| `/jobs/:id` | Job Detail | GET job, GET events, POST requeue/cancel/retry |
| `/queues` | Queues | queues/stats |
| `/workers` | Workers | workers, workers/config |
| `/dead-letter` | Dead Letter | GET jobs?status=DEAD_LETTERED |
| `/settings` | Settings | /actuator/health |

## Backend Dependency

Requires the QueueForge Spring Boot backend running on port 8080:
```bash
docker compose up -d postgres
./gradlew bootRun
```

## Build

```bash
npm run build
```

Output: `dist/` — static files deployable to any web server.

## Known Limitations

- No live polling — data refreshes on page load only
- No authentication (matches backend demo mode)
- Dark theme only (matches Stitch design)
- Table pagination is server-side
- Job payload viewer is raw JSON (no syntax highlighting)
