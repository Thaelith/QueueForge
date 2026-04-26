# Security Policy

## Reporting a Vulnerability

Please report vulnerabilities responsibly. Do not disclose exploit details publicly.

- Open a minimal GitHub issue without sensitive reproduction details if private reporting is not enabled on the repository.
- Contact the maintainer directly if contact information is available in the GitHub profile.
- Include a clear description of impact and affected area when possible.

## Security Notes

QueueForge is a portfolio/demo infrastructure project designed to demonstrate distributed job queue architecture. It is not production-ready without additional hardening.

- REST and admin endpoints are unauthenticated by design for local demo use.
- Local development database credentials may exist in `application.yml` or Docker Compose.
- Actuator/Prometheus endpoints are exposed for local observability.
- Demo handlers simulate email, webhook, and report behavior.
- Job payloads are accepted as JSON and should not be treated as trusted input in production.

## Current Security Limitations

- No authentication
- No authorization or role-based access control
- No API keys
- No rate limiting
- No TLS/HTTPS
- No CSRF protection for admin actions
- Admin endpoints are open
- Local database credentials are for development only
- Job payloads are not sandboxed
- No tenant isolation
- No production secret management
- No container image scanning configured

## Production Hardening Checklist

- Add authentication, such as API keys or OAuth2/OIDC.
- Add role-based authorization.
- Protect admin endpoints separately from public job submission endpoints.
- Move secrets to environment variables or a secrets manager.
- Enable TLS/HTTPS.
- Add request rate limiting.
- Validate and restrict job payload schemas.
- Add audit logging for admin actions.
- Restrict actuator endpoint exposure.
- Put PostgreSQL on a private network.
- Review dependencies with `./gradlew dependencies` and `npm audit`.
- Add container image scanning if publishing Docker images.
- Add CSRF protection or same-site controls if cookie-based auth is introduced.
- Add CORS restrictions for the dashboard/API.
- Consider signed/encrypted job payloads if sensitive data is processed.

## Scope

Reports should focus on:

- API/admin endpoint exposure risks
- Unsafe job payload handling
- SQL injection risks
- Authentication or authorization bypass if auth is added later
- Dependency vulnerabilities
- Dashboard XSS risks
- Docker/Compose misconfiguration
- Unsafe observability endpoint exposure
- Secrets accidentally committed to the repository

## Out of Scope

- Vulnerabilities requiring direct local machine access without privilege escalation
- Issues caused by intentionally running the demo with public ports exposed
- Missing production features already documented as limitations, unless they create an unexpected exploit path
