# AGENTS.md Design — Gold Sheet

> **Track**: Codex Mastery Track — Group 2: Intermediate Power User
> **File**: 1 of 7 (Track File #7)
> **Audience**: Developers who've done quick wins and want consistent project-wide Codex quality
> **Read after**: Codex-For-Beginners-Quick-Wins-Gold-Sheet.md

---

## Practical Impact Meter

| Topic | Impact | Why Developers Miss This |
|---|---|---|
| Root AGENTS.md as permanent project context | ★★★★★ | Without it, every session starts with generic defaults — not your team's patterns |
| Forbidden actions section | ★★★★★ | Omitting it means Codex may run migrations, push to git, or modify test files |
| Subfolder AGENTS.md for domain overrides | ★★★★☆ | One root file works; subfolder files make Codex precise per domain |
| Architecture rules section | ★★★★☆ | Codex will invent abstractions that violate your layering if you don't encode it |
| Verification command in AGENTS.md | ★★★★☆ | Global default verification prevents every prompt from having to repeat it |
| Keeping AGENTS.md concise | ★★★☆☆ | Long AGENTS.md with noise degrades output — quality > quantity |

---

## ⭐ Beginner Tier — Start Here

### B1: Create your first AGENTS.md in 5 minutes

```bash
# In your project root
cat > AGENTS.md << 'EOF'
# AGENTS.md

## Project
[Describe your project in 1-2 sentences]

## Tech Stack
- Language: [Python 3.11 / TypeScript 5 / Go 1.22]
- Framework: [FastAPI / Express / Gin]
- Database: [PostgreSQL / MySQL / MongoDB]
- Tests: [pytest / jest / go test]

## Coding Standards
- Error handling: [raise HTTPException / throw AppError / return error tuple]
- Naming: [snake_case / camelCase / PascalCase]

## Forbidden Actions
- NEVER modify test files
- NEVER run database migrations
- NEVER log passwords or API keys

## Verification
Run `[your test command]` after any implementation task.
EOF
```

Now run: `codex "What are the coding standards for this project? Do not make changes."`
Verify: Codex reads back your standards correctly.

### B2: Test that AGENTS.md is actually being used

```bash
# Add a distinctive rule to AGENTS.md:
# "Always add a comment '# AGENTS.md verified' on the first line of any new file"

# Then run:
codex --approval-policy auto-edit "Create a new file src/utils/validator.py with an is_valid_email() function"

# Check: does the new file start with # AGENTS.md verified ?
head -1 src/utils/validator.py

# If yes: AGENTS.md is being read ✅
# If no: check that AGENTS.md is in the working directory you ran codex from
```

---

## 1. AGENTS.md Anatomy — Every Section Explained

```markdown
# AGENTS.md

## Project
Single paragraph: what this codebase does, who uses it, its scale.
This orients Codex when it has no other context.

## Tech Stack
Explicit list: language + version, framework, key libraries, database.
"Python 3.11, FastAPI 0.111, SQLAlchemy 2.0, PostgreSQL 15, pytest 8"
Codex will generate code compatible with these versions — not its defaults.

## Coding Standards
Your team's rules. Be specific:
- "raise HTTPException(status_code=...) — never raise ValueError in API layer"
- "use parameterized queries via SQLAlchemy — never string interpolation in SQL"
- "use structlog for all logging — never print() statements"
Without this section, Codex follows its training defaults, not your conventions.

## Architecture
Layer rules and their enforcement:
- "API layer: src/api/ — only HTTP routing and response formatting"
- "Service layer: src/services/ — all business logic here"
- "Repository layer: src/db/ — all database access here"
- "No direct DB calls from API layer"
This prevents Codex from writing SQLAlchemy queries in FastAPI route handlers.

## Testing
What framework, what to run, what to mock:
- "Framework: pytest"
- "Run: pytest -x (stop on first failure)"
- "Mock: external HTTP calls, database connections — never own services"
- "Naming: test_[function]_[scenario]_[expected]"

## Forbidden Actions
What Codex must never do — this is your safety layer:
- NEVER modify test files to fix failing tests
- NEVER run database migrations (alembic upgrade, flyway migrate)
- NEVER run git push, git merge, or git rebase
- NEVER log PII (passwords, email, names, phone numbers)
- NEVER install new packages without listing them first and asking

## Verification
Default verification command for implementation tasks:
"Verification: run `pytest -x` after any implementation task."
This runs automatically — you don't have to type it in every prompt.
```

---

## 2. Root vs Subfolder AGENTS.md

```
project-root/
  AGENTS.md              ← global: language, framework, general conventions
  
  src/api/
    AGENTS.md            ← API-specific: REST conventions, auth rules, response formats
    
  src/db/
    AGENTS.md            ← DB-specific: migration safety, query patterns, connection handling
    
  tests/
    AGENTS.md            ← Test-specific: what to mock, naming conventions, fixtures
    
  infra/
    AGENTS.md            ← Infra-specific: extra forbidden actions (no terraform apply)
```

### How Codex uses multiple AGENTS.md files

```
When working in src/api/users.py:
  Codex reads: project-root/AGENTS.md + src/api/AGENTS.md
  Both apply. Subfolder rules extend and override root rules.

When working in src/db/user_repo.py:
  Codex reads: project-root/AGENTS.md + src/db/AGENTS.md
  DB-specific rules (migration safety) apply automatically without prompt-level repetition.
```

---

## 3. High-Signal AGENTS.md Sections

### The Architecture section (most underused)

```markdown
## Architecture

### Layer Boundaries (MUST FOLLOW)
- API layer (src/api/): HTTP routing, request validation, response formatting only
  NO business logic. NO direct database calls.
- Service layer (src/services/): All business logic here
  NO HTTP concepts (status codes, request objects). NO direct database calls.
- Repository layer (src/db/): All database access here
  NO business logic. ONLY query building and result mapping.

### Dependency Direction
API → Service → Repository
API must never import from Repository directly.
Service must never import from API.

### Error Propagation
Repository: raises DatabaseError for unexpected DB issues
Service: catches DatabaseError, raises ServiceError with user-friendly message
API: catches ServiceError, returns HTTPException with appropriate status code
```

### The Coding Standards section (most impactful)

```markdown
## Coding Standards

### Python
- Type hints: required on all public functions
- Error handling: raise HTTPException(status_code=X, detail="message") in API layer
- SQL: always use SQLAlchemy ORM or parameterized text() queries — NEVER f-strings in SQL
- Logging: structlog.get_logger(__name__) — never print()
- Async: use async def for all database operations and external HTTP calls

### Naming
- Files: snake_case.py
- Classes: PascalCase
- Functions: snake_case
- Constants: UPPER_SNAKE_CASE
- Private: _prefixed_with_underscore

### Code Style
- Max line length: 88 (black formatter)
- Imports: isort-sorted, no star imports
```

---

## 4. The Forbidden Actions Section — Your Safety Layer

```markdown
## Forbidden Actions

### Never Without Explicit Confirmation
- Run database migration commands (alembic upgrade, flyway migrate, rails db:migrate)
- Run destructive SQL (DROP TABLE, DELETE without WHERE, TRUNCATE)
- Run git push, git merge, git rebase, or git reset --hard
- Install new packages (pip install, npm install [new package])
- Modify files outside src/ and tests/
- Modify any .env file or configuration with secrets

### Never Under Any Circumstances
- Log PII: passwords, API keys, user emails, names, phone numbers, credit card numbers
- Use string interpolation in SQL queries (f"SELECT ... WHERE id = {user_id}")
- Use MD5 or SHA1 for password hashing (use bcrypt or argon2)
- Commit credentials in any form (hardcoded, base64-encoded, commented-out)
- Modify test files to make failing tests pass

### For Any Uncertain Action
Stop and ask the user. Do not proceed with an action if its scope or safety is unclear.
```

---

## 5. AGENTS.md Quality vs Quantity

```
More text in AGENTS.md is NOT better.

AGENTS.md quality rules:
  - Each rule should be specific and actionable
  - If a rule has never been violated by Codex: it may not be needed
  - Keep total AGENTS.md under 200 lines
  - Remove rules that Codex follows by default anyway (it already uses camelCase in JS)
  - Add rules based on observed mistakes: "Codex used string interpolation in SQL" → add the rule

Review AGENTS.md monthly:
  - Remove rules that were never relevant
  - Add rules based on what broke in the past month
  - Update stack versions when you upgrade
```

---

## 6. AGENTS.md Maintenance Ritual

```bash
# Weekly (5 minutes)
1. Did Codex make any mistake this week that AGENTS.md should prevent?
   → Add the rule. Be specific.

2. Did any rule in AGENTS.md get violated? 
   → Make the rule more explicit and concrete.

3. Did you add any new library, framework, or pattern this week?
   → Update the Tech Stack and Coding Standards sections.

# Monthly (10 minutes)
1. Audit: is every rule in AGENTS.md still relevant?
2. Review: are there rules Codex consistently violates? (Maybe clarify them)
3. Review: are there rules Codex consistently follows anyway? (Maybe remove them)
```

---

## Interview Traps

```
TRAP: "I put everything important in each prompt instead of AGENTS.md"
INSIGHT: Prompt-level rules are forgotten after each session. AGENTS.md is permanent.
         If you type the same constraint 5+ times across prompts, it belongs in AGENTS.md.

TRAP: "One AGENTS.md for the whole codebase is enough"
INSIGHT: It's a good start but misses domain-specific rules. A subfolder AGENTS.md in
         src/db/ for database safety rules applies precisely when Codex is modifying
         database code — not for every task.

TRAP: "AGENTS.md should be comprehensive — list every possible rule"
TRUTH: A concise AGENTS.md (under 200 lines) with high-signal rules outperforms a
       500-line document with generic advice Codex already knows.
```

---

## Revision Checklist

- [ ] AGENTS.md exists in at least one real project root
- [ ] AGENTS.md has all 6 sections: Project, Tech Stack, Coding Standards, Architecture, Testing, Forbidden Actions
- [ ] Forbidden Actions includes: no test file modification, no migrations, no secrets in logs
- [ ] Verified AGENTS.md is being read: Codex reads back the standards correctly
- [ ] Subfolder AGENTS.md added for at least one high-risk area (db/ or auth/)
- [ ] AGENTS.md is under 200 lines — concise and actionable
