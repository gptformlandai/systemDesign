---
applyTo: "**"
---
# Security Rules — Apply Everywhere

## Input Handling
All user-supplied inputs must be validated before use.
Never use user input directly in SQL queries — always use parameterized queries or ORM.
Never use user input in shell commands without strict sanitization.
Never use eval() or equivalent with user-supplied data.

## Secrets and Credentials
Never hardcode: API keys, passwords, tokens, connection strings with passwords, private keys.
Always use environment variables or a secrets manager.
Never log: passwords, tokens, API keys, SSNs, credit card numbers, or full email addresses.
Secrets must not appear in error messages, stack traces, or API responses.

## Authentication and Authorization
Every protected endpoint must verify: is the user authenticated AND authorized?
Authentication check ≠ authorization check — do both.
Never expose different error messages for valid vs invalid users (user enumeration).

## Error Responses
API error responses must never include: stack traces, internal paths, database errors, or system details.
Use generic messages for 5xx errors: "An internal error occurred."
Use specific messages for 4xx errors without revealing internals.

## Cryptography
Password hashing: use bcrypt or argon2. Never MD5 or SHA1 for passwords.
Token generation: use cryptographically secure random (secrets module in Python, crypto.randomBytes in Node).
Never roll your own cryptographic algorithm.

## Dependencies
When adding a new dependency: verify it is actively maintained and has no critical CVEs.
Pin dependency versions in the lockfile.
Run security audit after dependency changes: pip audit / npm audit / mvn dependency-check.

## Do NOT
- Do not use shell=True with subprocess and user-provided data
- Do not store plain-text passwords
- Do not use MD5 or SHA1 for any security purpose
- Do not disable TLS certificate verification in production
- Do not use insecure random (random.random()) for security tokens
