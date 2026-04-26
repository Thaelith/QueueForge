# Contributing

QueueForge is a portfolio/demo project. Contributions and feedback are welcome.

## Running Tests

```bash
./gradlew test                         # unit tests
./gradlew integrationTest              # integration tests (requires Docker)
./gradlew clean test integrationTest   # all tests
```

## Code Style

- Java sources follow clean architecture conventions (separate domain, application, infrastructure, api layers)
- Use Lombok-free records for domain objects
- Use constructor injection (no `@Autowired` on fields)
- Use `@Transactional` on write operations, `@Transactional(readOnly = true)` on reads
- Keep SQL in repository layer only
- No ORM — plain JDBC with `JdbcTemplate` and custom `RowMapper`

## Frontend

- React + TypeScript + Vite
- Tailwind CSS with Stitch design tokens
- API client in `dashboard/src/lib/api.ts`
- Use `VITE_API_BASE_URL` env var for backend URL

## Commit Conventions

- `feat: <description>` for new features
- `fix: <description>` for bug fixes
- `docs: <description>` for documentation
- `refactor: <description>` for refactoring
- `test: <description>` for tests

## Pull Requests

1. Run `./gradlew test` and ensure all pass
2. Run `cd dashboard && npm run build` and ensure no errors
3. Keep changes focused and reviewable
