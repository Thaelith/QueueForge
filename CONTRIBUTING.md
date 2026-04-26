# Contributing to QueueForge

QueueForge is a portfolio/demo infrastructure project demonstrating distributed job queue architecture, PostgreSQL-backed worker leasing, retry/dead-letter behavior, and observability. Contributions and feedback are welcome.

## Development Workflow

1. Fork the repository.
2. Create a focused branch for your change.
3. Make a small, reviewable change.
4. Run backend and frontend quality checks.
5. Open a pull request with a clear description.

## Local Setup

**Backend:**

```bash
git clone https://github.com/Thaelith/QueueForge.git
cd QueueForge
docker compose up -d postgres
.\gradlew bootRun
```

- Backend: `http://localhost:8080`
- Swagger UI: `http://localhost:8080/swagger-ui.html`
- Health: `http://localhost:8080/actuator/health`

**Dashboard:**

```bash
cd dashboard
npm install
npm run dev
```

- Dashboard: `http://localhost:5173`

## Quality Checks

**Backend:**

```bash
./gradlew test                         # unit tests (no Docker required)
./gradlew integrationTest              # integration tests (Docker required for Testcontainers/PostgreSQL)
./gradlew bootJar
```

**Frontend:**

```bash
cd dashboard
npm run build
```

**All-in-one:**

```bash
./gradlew clean test integrationTest bootJar
cd dashboard && npm run build
```

## Code Guidelines

Keep clean architecture boundaries clear:

- `domain` — core business rules and domain models
- `application` — use cases and orchestration
- `infrastructure` — database, persistence, repository implementations
- `api` — REST controllers and request/response DTOs
- `config` — Spring configuration and properties binding

Specific guidelines:

- Keep SQL inside repository/infrastructure classes. No ORM — use `JdbcTemplate` with custom `RowMapper` implementations.
- Use constructor injection. Avoid field injection.
- Use `@Transactional` on write operations, `@Transactional(readOnly = true)` on reads.
- Keep worker leasing logic transaction-safe.
- Do not weaken worker ownership checks around `locked_by`.
- Do not bypass retry/dead-letter behavior.
- Keep job status transitions explicit and testable.
- Use DTOs for API requests/responses. Keep API errors consistent through the global exception handler.
- Keep frontend API calls centralized in `dashboard/src/lib/api.ts`.
- Do not hardcode localhost inside React components; use `VITE_API_BASE_URL`.
- Keep dashboard styling consistent with the existing dark infrastructure-tool theme.
- Avoid unnecessary dependencies.

## Worker and Queue Safety

Worker leasing is the most sensitive part of QueueForge.

- `SELECT ... FOR UPDATE SKIP LOCKED` is central to safe concurrent leasing. Any change to leasing, retries, dead-lettering, or expired lease recovery must include integration tests.
- Handlers execute with at-least-once semantics and should be idempotent where possible.
- Job ownership checks (`locked_by`) prevent stale workers from completing another worker's lease. Do not weaken these checks.

## Commit Conventions

- `feat:` for new features
- `fix:` for bug fixes
- `docs:` for documentation
- `refactor:` for refactoring
- `test:` for tests
- `chore:` for maintenance

## Do Not Commit

- `.env` files
- `build/`, `.gradle/`, `bin/`
- `dashboard/node_modules/`, `dashboard/dist/`
- Local database files
- IDE-specific files unless intentionally tracked
- Screenshots with private information
- Secrets or API keys
- Generated logs

## Documentation

Update relevant docs when changing:

- Worker leasing behavior
- Retry/dead-letter behavior
- API response shapes
- Dashboard routes
- Docker Compose commands
- Observability metrics
- Demo scripts
- README screenshots or demo flow

## Pull Request Checklist

- [ ] I ran backend unit tests (`./gradlew test`).
- [ ] I ran integration tests if backend/database behavior changed (`./gradlew integrationTest`).
- [ ] I ran dashboard build if frontend changed (`cd dashboard && npm run build`).
- [ ] I updated docs if behavior changed.
- [ ] I did not commit secrets or local machine files.

## Questions

Open a GitHub issue for questions, suggestions, or design discussions.
