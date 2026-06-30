# AGENTS.md — Project Template

> Copy this to any project root. Fill in the bracketed sections. Keep under 200 lines.

---

## Project Context

**Project**: [Project name]  
**Type**: [REST API / Frontend / CLI tool / Library / Full-stack app]  
**Tech stack**: [Language + version], [Framework + version], [Database]  
**Team size**: [N engineers]  
**Test framework**: [pytest / jest / JUnit / etc.]  
**Deployment**: [AWS ECS / Kubernetes / Vercel / etc.]

---

## Architecture

**Layer structure**:
```
API layer (src/api/) → Service layer (src/services/) → Repository layer (src/db/)
No layer may import from a higher layer.
API layer uses HTTP concepts (HTTPException, status codes).
Service layer contains business logic only — no HTTP or DB concepts.
Repository layer handles all database queries — no business logic.
```

**Patterns**:
- Pattern for new endpoints: follow `src/api/users.py` exactly
- Pattern for new services: follow `src/services/user_service.py` exactly
- Pattern for new repositories: follow `src/db/user_repository.py` exactly

**Key constraints**:
- Auth check must happen in the API layer before calling services
- All database queries must use parameterized queries (no string concatenation)
- External HTTP calls must have timeouts specified

---

## Coding Standards

**Error handling**:
- Business validation errors: raise `ValueError` in service layer
- Not found: raise `HTTPException(404)` in API layer
- Auth failures: raise `HTTPException(401)` or `HTTPException(403)` in API layer
- Never return raw exception details to API callers

**Naming**:
- Functions: `snake_case`
- Classes: `PascalCase`
- Constants: `UPPER_SNAKE_CASE`
- Test functions: `test_[function_name]_[scenario]`

**Type hints**: required on all public function signatures  
**Docstrings**: Google-style on all public functions  
**Response format**: always return `{"status": "success", "data": {...}}` or raise HTTPException

**Testing**:
- Test files: `tests/test_[module_name].py`
- Only mock external dependencies (HTTP, DB driver, cloud SDKs)
- Do NOT mock own services or repositories
- Every new branch in production code requires a corresponding test

---

## Forbidden Actions

1. **Run database migrations** — never run `alembic upgrade`, `flask db upgrade`, etc.
2. **Push to git** — never run `git push`
3. **Modify .env files** — never read, write, or create .env files
4. **Log PII** — never log user emails, names, addresses, payment info
5. **Change test assertions to make tests pass** — fix implementation, not tests
6. **Install new packages** — do not run pip install or npm install without listing the package first
7. **Modify production configs** — never touch config/production.yaml or any prod config

---

## Verification Command

```bash
pytest -x && ruff check src/
```

This command must pass before any task is considered done.
Run it at the end of every implementation task.

---

## Notes

- AGENTS.md version: [date created/updated]
- Last updated: [date]
- Owner: [team or person]
