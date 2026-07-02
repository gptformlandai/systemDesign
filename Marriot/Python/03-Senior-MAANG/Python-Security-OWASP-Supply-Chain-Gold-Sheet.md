# Python Security, OWASP, and Supply Chain - Gold Sheet

> **Track File #18d - Group 3: Senior MAANG**
> For: backend Python | Level: production security and interview judgment

---

## 1. Why This Sheet Exists

Python makes it easy to build fast. It also makes it easy to accidentally:

- deserialize code
- shell out unsafely
- trust user paths
- leak secrets in logs
- install compromised dependencies
- treat validation as authorization

This sheet turns scattered security instincts into one production checklist.

---

## 2. Security Mental Model

```text
External input is hostile.
Internal state can be stale.
Dependencies are part of your attack surface.
Logs are a data exfiltration path.
Runtime configuration is security-sensitive.
```

Senior Python security is not memorizing OWASP names. It is knowing where Python code crosses trust boundaries.

---

## 3. Trust Boundaries In A Python Backend

Common boundaries:

- HTTP request body
- query/path parameters
- headers and cookies
- uploaded files
- message queue payloads
- database rows written by other services
- cache values
- environment variables
- config files
- third-party API responses
- package installation from PyPI or internal indexes

Rule:

```text
Validate at boundaries.
Authorize at use.
Escape or parameterize at sinks.
Observe without leaking secrets.
```

---

## 4. Injection Risks

### SQL Injection

Bad:

```python
query = f"SELECT * FROM users WHERE email = '{email}'"
cursor.execute(query)
```

Good:

```python
cursor.execute("SELECT * FROM users WHERE email = %s", (email,))
```

SQLAlchemy:

```python
from sqlalchemy import select

stmt = select(User).where(User.email == email)
```

Rule:

```text
Never build SQL by concatenating user input.
Use parameterized queries or ORM expression APIs.
```

### Command Injection

Bad:

```python
import os

os.system(f"convert {filename} output.png")
```

Better:

```python
import subprocess

subprocess.run(
    ["convert", filename, "output.png"],
    check=True,
    timeout=10,
)
```

Rules:

- avoid `shell=True`
- pass argument lists
- validate allowlisted commands/paths
- set timeout
- run with least privilege

---

## 5. Path Traversal

Bad:

```python
from pathlib import Path

base = Path("/srv/uploads")
target = base / user_supplied_filename
return target.read_bytes()
```

`../../etc/passwd` can escape the base directory.

Safer:

```python
from pathlib import Path


def safe_join(base: Path, user_name: str) -> Path:
    base = base.resolve()
    target = (base / user_name).resolve()
    if base not in target.parents and target != base:
        raise ValueError("invalid path")
    return target
```

Also:

- generate server-side filenames
- store original filename as metadata only
- validate extension and content type
- enforce size limits

---

## 6. Unsafe Deserialization

### Pickle

Never load untrusted pickle.

```python
import pickle

pickle.loads(user_bytes)  # dangerous
```

Use:

- JSON
- Pydantic
- Protocol Buffers
- Avro/Parquet for data pipelines

### YAML

Bad:

```python
import yaml

data = yaml.load(raw, Loader=yaml.Loader)
```

Better:

```python
import yaml

data = yaml.safe_load(raw)
```

Rule:

```text
Deserialization is code-adjacent. Treat formats and loaders as security decisions.
```

---

## 7. SSRF

Server-Side Request Forgery happens when a user controls a URL your server fetches.

Bad:

```python
import httpx

response = httpx.get(user_url)
```

Mitigations:

- allowlist domains
- block private IP ranges
- resolve DNS and validate final IP
- set connect/read timeouts
- disable redirects or revalidate after redirects
- use egress controls outside the app

Sketch:

```python
from urllib.parse import urlparse


ALLOWED_HOSTS = {"api.stripe.com", "example-cdn.com"}


def validate_outbound_url(url: str) -> str:
    parsed = urlparse(url)
    if parsed.scheme != "https":
        raise ValueError("https required")
    if parsed.hostname not in ALLOWED_HOSTS:
        raise ValueError("host not allowed")
    return url
```

---

## 8. Template Injection and XSS

Jinja danger:

```python
template = Template(user_supplied_template)
template.render(context)
```

Rules:

- do not render user-supplied templates
- keep autoescape enabled for HTML
- separate text templates from HTML templates
- never mark untrusted content as safe

API services still need XSS awareness because admin dashboards, emails, and generated HTML often share backend code.

---

## 9. Authentication and Authorization

Authentication asks:

```text
Who are you?
```

Authorization asks:

```text
Are you allowed to do this specific action on this specific resource?
```

FastAPI sketch:

```python
from fastapi import Depends, HTTPException


async def require_order_access(
    order_id: str,
    user: User = Depends(current_user),
    service: OrderService = Depends(get_order_service),
) -> User:
    allowed = await service.can_access_order(user.id, order_id)
    if not allowed:
        raise HTTPException(status_code=403, detail="forbidden")
    return user
```

Common mistakes:

- validating JWT but not checking resource ownership
- trusting `user_id` from request body
- putting admin checks only in frontend
- using role checks when object-level checks are required

---

## 10. JWT and Token Mistakes

Checklist:

- verify signature
- verify issuer
- verify audience
- verify expiration
- use expected algorithm; do not accept arbitrary `alg`
- rotate keys
- avoid logging tokens
- keep token TTL short enough for risk
- use refresh token rotation where appropriate

Bad smell:

```text
Decode JWT without verification to get user_id, then trust it.
```

---

## 11. CORS and CSRF

CORS is browser access control, not backend authentication.

Bad:

```text
allow_origins=["*"]
allow_credentials=True
```

Use explicit origins for credentialed browser apps.

CSRF matters when browsers automatically attach credentials such as cookies. Token-only APIs called with `Authorization` headers have different risk, but browser flows still need careful design.

---

## 12. Secrets

Rules:

- never commit secrets
- never log secrets
- never put secrets in error messages
- use environment variables, secret managers, or workload identity
- rotate secrets
- scope credentials narrowly

Pydantic settings sketch:

```python
from pydantic_settings import BaseSettings, SettingsConfigDict


class Settings(BaseSettings):
    database_url: str
    jwt_public_key: str
    stripe_api_key: str

    model_config = SettingsConfigDict(env_file=".env", extra="ignore")
```

`.env` is local convenience, not a production secret store.

---

## 13. Logging Without Leaking

Bad:

```python
logger.info("login request", extra={"password": password})
```

Better:

```python
logger.info("login request", extra={"user_id": user_id, "request_id": request_id})
```

Redact:

- passwords
- tokens
- API keys
- cookies
- session IDs
- authorization headers
- full credit card numbers
- PII unless explicitly approved

---

## 14. Dependency and Supply Chain Security

Python dependencies are production code.

Minimum checklist:

- lock dependencies for applications
- review new transitive dependencies
- use private indexes carefully
- protect against dependency confusion
- run vulnerability scanning
- run static security checks
- pin Docker base image intentionally
- generate SBOM when required

Common tools:

| Tool | Use |
|---|---|
| `pip-audit` | dependency vulnerability audit |
| Bandit | Python security static analysis |
| Semgrep | customizable static analysis |
| Ruff | lint rules; not a full security scanner |
| Dependabot/Renovate | dependency update automation |
| SBOM tooling | inventory for compliance |

Example CI:

```bash
uv sync --frozen
uv run ruff check .
uv run mypy src
uv run pytest
uv run pip-audit
uv run bandit -r src
```

---

## 15. File Upload Security

Checklist:

- enforce max size
- do not trust client filename
- store outside web root
- generate server-side name
- validate content type and magic bytes when important
- scan if policy requires
- never execute uploaded files
- use pre-signed object storage URLs for large files

---

## 16. Security Headers

For browser-facing apps:

- `Content-Security-Policy`
- `X-Content-Type-Options`
- `Strict-Transport-Security`
- `Referrer-Policy`
- `Frame-Options` or CSP frame ancestors

For pure APIs:

- TLS everywhere
- strict CORS
- no sensitive error detail
- rate limits
- request size limits

---

## 17. Rate Limiting and Abuse

Security is also availability.

Protect:

- login attempts
- password reset
- expensive search
- file upload
- webhook endpoints
- public APIs

Patterns:

- per-IP limit
- per-user limit
- per-API-key quota
- token bucket
- sliding window
- circuit breakers for downstream abuse

---

## 18. Production Security Checklist

- [ ] All external input validated.
- [ ] SQL uses parameters or ORM expressions.
- [ ] No `pickle.loads` on untrusted data.
- [ ] No unsafe YAML loader.
- [ ] No `shell=True` with user input.
- [ ] File paths resolved under allowlisted base.
- [ ] SSRF protections for outbound user-controlled URLs.
- [ ] JWT validation verifies issuer, audience, exp, and algorithm.
- [ ] Authorization checks resource ownership.
- [ ] Secrets are not logged or committed.
- [ ] Dependencies are locked and scanned.
- [ ] Logs redact tokens, cookies, passwords, and PII.
- [ ] CORS configured with explicit origins.
- [ ] Request size and rate limits exist.

---

## 19. Practical Question

> You inherit a FastAPI service that accepts file uploads, reads YAML configs, calls a callback URL from the request, and stores data in Postgres. What security review do you perform?

Strong answer:

> I would inspect every trust boundary. For uploads, I would enforce size, generate server-side filenames, validate type, and store outside executable paths. For YAML, I would require safe loading only. For callback URLs, I would add SSRF controls: HTTPS only, host allowlist, private IP blocking, redirect checks, and timeouts. For Postgres, I would verify SQLAlchemy expressions or parameterized queries, not string SQL. I would also check auth and object-level authorization, redacted logs, secret handling, dependency scanning, and rate limits.

---

## 20. Revision Notes

- One-line summary: Python security is boundary validation plus safe sinks plus dependency discipline.
- Three keywords: validate, parameterize, lock.
- One interview trap: thinking Pydantic validation equals authorization.
- One memory trick: every input goes to a sink; secure both ends.
