# ADR-0002: Separate Dashboard from Backend

## Context

QueueForge needs an admin dashboard for monitoring queues, inspecting jobs, and performing admin actions. We must decide whether to embed it in the Spring Boot app or build a separate frontend application.

Options considered:
1. Server-rendered dashboard (Thymeleaf, HTMX)
2. React SPA served from Spring Boot static resources
3. Separate React SPA with its own dev server and build pipeline

## Decision

Build a separate React + TypeScript + Vite application in `/dashboard/` that communicates with the backend via REST API.

**Why this approach:**
- Clean separation of concerns — frontend and backend evolve independently
- Standard frontend tooling (Vite, npm, Tailwind) without Maven/Gradle integration
- Easy to replace or rewrite the frontend later without touching backend
- Vite dev server proxies `/api` requests to backend during development
- Production build produces static assets that can be served by any web server

## Consequences

**Positive:**
- Full modern frontend DX (HMR, TypeScript, Tailwind)
- Backend stays pure REST API
- Frontend team can work without Java toolchain
- Static build output deployable to CDN

**Negative:**
- Two separate build pipelines (Gradle + npm)
- Additional dependency (Node.js) for frontend developers
- CORS issues possible if not using Vite proxy
- Frontend needs separate documentation

## Status

Accepted. Phase 6 implementation.
