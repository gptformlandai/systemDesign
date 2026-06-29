# Personal Project Bootstrap — Gold Sheet

> **Track**: Copilot Mastery Track — Group 3: Advanced Engineering
> **File**: Gap Fill (Track File #20c)
> **Audience**: Developers spinning up new personal, freelance, or startup projects
> **Read after**: Personal-GitHub-Workflow-Gold-Sheet.md

---

## 1. The 30-Minute Project Bootstrap

Use this checklist + Copilot to go from zero to a working, CI-enabled, Copilot-configured project in 30 minutes.

---

### Phase 1 — Repository Setup (5 min)

```bash
# Create repo and clone
gh repo create my-project-name --private --clone --description "What this does"
cd my-project-name

# Initialize git
git init    # (if not using gh repo create)
git checkout -b main

# Open in VS Code
code .
```

---

### Phase 2 — Copilot Bootstrap Prompt (15 min)

In VS Code Chat, use the `/bootstrap-project` prompt, OR paste this directly:

```
Bootstrap a new [STACK] project named [PROJECT_NAME].

Stack: [e.g., Python 3.12 + FastAPI + PostgreSQL + SQLAlchemy async + pytest + Poetry]
Purpose: [2-sentence description of what this project does]

Create these files (plan first, wait for my approval before creating):

1. .gitignore — appropriate for this stack
2. .env.example — list ALL required env vars with placeholder values
3. pyproject.toml / package.json — with dev dependencies pinned
4. src/ directory with:
   - Entry point (main.py or index.ts)
   - One example module following the intended pattern
5. tests/ directory with:
   - conftest.py with basic fixtures
   - One example test file
6. .github/
   - copilot-instructions.md (project conventions)
   - workflows/ci.yml (lint + test)
   - AGENTS.md (behavioral rules)
7. .vscode/
   - settings.json
   - extensions.json
8. README.md with: What It Does, Prerequisites, Install, Run, Test

Rules:
- No placeholder code (every file must be usable as-is)
- No real credentials anywhere (use .env.example with placeholders)
- All CLI commands in README must be copy-paste runnable
- Pin all dependency versions
```

---

### Phase 3 — Review the Plan (5 min)

```
After Copilot presents the plan:
[ ] Directory structure matches your mental model
[ ] Files listed are appropriate for this stack
[ ] No files you didn't ask for
[ ] No files missing that you'll need soon

Approve or correct:
"The plan looks good. One correction: [correction]. Now build it."
```

---

### Phase 4 — Verify and Commit (5 min)

```bash
# Run tests to verify generated code works
pytest tests/        # Python
npm test             # Node/TypeScript
./mvnw test          # Java

# If tests pass:
git add .
git commit -m "chore: initial project scaffold"
git push -u origin main

# Create first PR (even on a solo project — good habit):
gh pr create --title "Initial scaffold" --body "Copilot-assisted project initialization"
```

---

## 2. Stack-Specific Bootstrap Templates

### Python FastAPI + PostgreSQL

```
Stack: Python 3.12, FastAPI 0.115, SQLAlchemy 2.x async, asyncpg, Pydantic v2, pytest, Poetry

Required files:
  pyproject.toml with: fastapi, sqlalchemy[asyncio], asyncpg, pydantic, pytest, pytest-asyncio, ruff, mypy
  src/
    main.py           — FastAPI app factory
    api/              — routers
    services/         — business logic
    repositories/     — database access
    models/           — SQLAlchemy models
    schemas/          — Pydantic schemas
    config.py         — settings via pydantic-settings
  tests/
    conftest.py       — AsyncSession fixture with rollback
    unit/
    integration/
  alembic/            — DB migration setup
  .env.example        — DATABASE_URL, SECRET_KEY, ENVIRONMENT
```

### Node.js Express + TypeScript

```
Stack: Node 20, TypeScript 5, Express 4, Prisma ORM, Jest, Zod validation

Required files:
  package.json with: express, typescript, prisma, zod, jest, ts-jest, eslint
  tsconfig.json — strict mode
  src/
    index.ts          — app entry + server start
    routes/           — Express routers
    services/         — business logic
    middleware/        — auth, error handler, request logger
    types/            — shared TypeScript types
  prisma/
    schema.prisma     — DB schema
  tests/
    __mocks__/        — manual mocks for Prisma
    unit/
  .env.example        — DATABASE_URL, JWT_SECRET, PORT
```

### React TypeScript SPA

```
Stack: React 19, TypeScript 5, Vite, TanStack Query, Zustand, Vitest, React Testing Library

Required files:
  package.json with: react, typescript, vite, @tanstack/react-query, zustand, vitest
  vite.config.ts
  src/
    main.tsx          — app entry
    App.tsx           — root component + routing
    components/       — reusable UI components
    pages/            — route-level components
    hooks/            — custom hooks
    stores/           — Zustand stores
    api/              — API client functions
    types/            — TypeScript type definitions
  public/
  .env.example        — VITE_API_URL
```

---

## 3. Post-Bootstrap Setup Checklist

```
After Copilot builds the project:

[ ] .env created from .env.example with real local values (gitignored)
[ ] Database: local instance running, schema created (run migrations if needed)
[ ] VS Code extensions installed (opened? VS Code prompts from extensions.json)
[ ] Tests pass: [test command] — all tests green
[ ] Lint passes: [lint command] — no errors
[ ] App starts: [run command] — accessible at localhost:[port]
[ ] First commit pushed to GitHub
[ ] CI runs on GitHub and passes
[ ] copilot-instructions.md opened and customized for project specifics
[ ] AGENTS.md reviewed and updated if needed
```

---

## 4. Personal Project copilot-instructions.md Template

```markdown
# [Project Name]

## What This Is
[2-3 sentences: what it does, who uses it, current scale]

## Tech Stack
- [Your stack — be specific with versions]

## Current Development State
[Briefly: what's done, what's in progress, what's next]

## Architecture
[Key layers and their rules]

## Do NOT
- [Stack-specific antipatterns to avoid]
- Do not add dependencies without adding them to [package file]
- Do not use print()/console.log() for logging in committed code

## Key Files to Know
- [file]: [what it does]
- [file]: [what it does]
```

---

## 5. Revision Checklist

- [ ] Can run the 30-minute bootstrap process from memory
- [ ] Knows which stack-specific files to request for their primary language
- [ ] Reviews and approves the bootstrap plan before Agent Mode builds anything
- [ ] Post-bootstrap checklist completed before starting feature work
- [ ] Has a personalized `copilot-instructions.md` template ready to paste
