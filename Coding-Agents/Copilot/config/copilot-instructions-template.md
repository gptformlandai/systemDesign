# copilot-instructions.md Template
# Copy this to: .github/copilot-instructions.md in your project
# Customize every section marked [CUSTOMIZE]

# [CUSTOMIZE: Project Name]

## What This Project Is
[CUSTOMIZE: 2-3 sentences describing what this project does and who uses it.]

## Tech Stack
- Language: [CUSTOMIZE: e.g., Python 3.12]
- Framework: [CUSTOMIZE: e.g., FastAPI 0.115+]
- Database: [CUSTOMIZE: e.g., PostgreSQL 16 + SQLAlchemy 2.x async]
- Validation: [CUSTOMIZE: e.g., Pydantic v2]
- Testing: [CUSTOMIZE: e.g., pytest + pytest-asyncio]
- HTTP client: [CUSTOMIZE: e.g., httpx (not requests)]
- Dependency management: [CUSTOMIZE: e.g., Poetry]
- Linting: [CUSTOMIZE: e.g., ruff]

## Architecture Rules
- [CUSTOMIZE: e.g., "Service layer handles business logic. Routers only parse requests."]
- [CUSTOMIZE: e.g., "Repository layer handles all DB access. Services never query DB directly."]
- [CUSTOMIZE: e.g., "All async code. Never use sync SQLAlchemy in async routes."]

## Coding Standards
- [CUSTOMIZE: e.g., "Type hints required on all function signatures."]
- [CUSTOMIZE: e.g., "Use f-strings. Never % or .format()"]
- [CUSTOMIZE: e.g., "Max function length: 30 lines."]

## Testing Rules
- [CUSTOMIZE: e.g., "Every public method needs at least one test."]
- [CUSTOMIZE: e.g., "Mock all external services in unit tests."]
- [CUSTOMIZE: e.g., "Test file location mirrors src/ structure under tests/"]

## Security Rules
- Never log passwords, tokens, or PII.
- Always use parameterized queries. Never string formatting in SQL.
- Validate all user inputs with Pydantic before processing.
- Never expose internal error messages in API responses.

## Do NOT
- [CUSTOMIZE: e.g., "Do not use print() for logging. Use Python logging module."]
- [CUSTOMIZE: e.g., "Do not use os.system(). Use subprocess.run() with shell=False."]
- [CUSTOMIZE: e.g., "Do not suggest deprecated library patterns."]
- Do not generate code with hardcoded credentials or API keys.
