# Security

QueueForge is a **portfolio/demo project** demonstrating distributed job queue architecture. It is not intended for production deployment without additional security hardening.

## Reporting Issues

If you discover a security vulnerability, please open a GitHub issue or contact the repository owner directly. Do not disclose vulnerabilities publicly until they are addressed.

## Current Security Limitations

- No authentication on REST API endpoints
- No rate limiting
- Database credentials in `application.yml` (for local development)
- No TLS/HTTPS
- No input sanitization beyond Jakarta Bean Validation
- Admin endpoints accessible without API keys

## Production Hardening (Not Implemented)

If deploying beyond demo use:

1. **Authentication**: Add API key or OAuth2/OpenID Connect
2. **Authorization**: Separate admin API key from submit API key
3. **TLS**: Configure HTTPS with valid certificates
4. **Secrets**: Move database credentials to environment variables or vault
5. **Rate Limiting**: Add request rate limiting per IP/API key
6. **Input Validation**: Sanitize job payloads against injection
7. **Audit Logging**: Log admin actions for compliance
8. **Network**: Isolate database on private network, expose only API ports

## Dependencies

Keep dependencies updated. Use `./gradlew dependencies` and `npm audit` to check for known vulnerabilities.
