# Safe Usage Principles — Gold Sheet

> **Track**: Codex Mastery Track — Group 1: Foundations
> **File**: 5 of 6 (Track File #5)
> **Audience**: All Codex users — before your second task
> **Read after**: Codex-CLI-Fundamentals-Gold-Sheet.md

---

## Practical Impact Meter

| Topic | Impact | Why Developers Miss This |
|---|---|---|
| No secrets in prompts | ★★★★★ | Devs paste .env files into prompts for "context" — these go to OpenAI's API |
| Git checkpoint before full-auto | ★★★★★ | Without checkpoint, recovering from unexpected changes is painful |
| Review every diff before committing | ★★★★★ | "It looked right" is not a review — reading each changed line is |
| Suggest mode for new codebases | ★★★★★ | Using full-auto on an unfamiliar codebase is how you break production |
| Synthetic data in prompts | ★★★★☆ | Real user emails, IDs, or names in prompts = PII going to the API |
| Never modify tests to make them pass | ★★★★☆ | Codex will do this if not explicitly forbidden — tests become worthless |

---

## ⭐ Beginner Tier — Start Here

### B1: The three things never to put in a Codex prompt

```
1. API keys, tokens, passwords, secrets
   BAD: "My database URL is postgres://admin:P@ssw0rd@prod.db.company.com/app"
   GOOD: "The database connection uses DATABASE_URL environment variable"

2. Real user data (PII)
   BAD: "This user has email john.smith@company.com and user_id 84721"
   GOOD: "The user has email test@example.com and user_id 99999 (synthetic)"

3. Internal system details that would be sensitive if leaked
   BAD: "Our security bypass for admin is: append ?admin=true to any URL"
   GOOD: "We have an admin authentication system — don't expose admin routes"
```

If your prompt contains any of these: delete them before sending.

### B2: The checkpoint habit

```bash
# This takes 5 seconds. Do it every time before full-auto.
git add -A
git commit -m "checkpoint: before codex full-auto"

# Now run Codex in full-auto
codex --approval-policy full-auto "your task"

# After: always review
git diff HEAD~1
```

If the output is wrong: `git reset HEAD~1 --soft` — back to checkpoint instantly.

---

## 1. No Secrets in Prompts — The Non-Negotiable Rule

### What counts as a secret

```
API keys:         OPENAI_API_KEY, AWS_ACCESS_KEY, Stripe secret key, etc.
Passwords:        database password, admin password, service account password
Connection strings: postgres://user:pass@host/db
Tokens:           JWT secrets, OAuth client secrets, HMAC signing keys
Internal config:  internal IP addresses, internal hostnames, VPN credentials
Security logic:   "our bypass is...", "the admin token is..."
```

### What to do instead

```bash
# Instead of pasting the actual value:
"The database password is in DATABASE_PASSWORD env variable"
"Use the JWT secret from JWT_SECRET env variable"
"The payment API key is stored in STRIPE_SECRET_KEY"

# Codex understands env vars — you never need to paste actual values
```

### Why this matters

```
Your prompts go to OpenAI's API over HTTPS.
They may be stored for abuse monitoring and model improvement.
A leaked key can cause:
  - Unauthorized API usage charges
  - Access to your cloud resources
  - Data breach if DB credentials are leaked
```

---

## 2. Synthetic Data — No Real PII in Prompts

```bash
# WRONG: real user data in prompt
codex "Debug why this query fails for user sarah.johnson@acme.com (user_id: 48291)"

# RIGHT: synthetic data
codex "Debug why this query fails for a user with email like test@example.com.
       The error occurs when the user_id column has a value greater than 10000."
```

### Synthetic data patterns

```python
# Always use these patterns in prompts and test code
emails:       test@example.com, user1@test.com, fake@domain.example
user_ids:     99999, 12345, 0, -1
names:        Test User, Jane Doe, John Smith
Phone:        +1-555-555-0100 (555 numbers are reserved/fake)
Credit cards: 4111 1111 1111 1111 (Visa test card — safe to use)
```

---

## 3. Version Control Safety Net — Three Rules

### Rule 1: Checkpoint before every full-auto session

```bash
# Always — no exceptions
git add -A
git commit -m "checkpoint: before codex [task description]"
```

### Rule 2: Review the full diff after every Codex session

```bash
# After Codex completes any task
git diff HEAD        # unstaged changes
git diff --staged    # staged changes
git diff HEAD~1      # all changes since last commit (after full-auto)

# What to check in the diff:
# ✅ Only the files you expected were modified
# ✅ Changes match what you asked for
# ✅ No unexpected imports added
# ✅ No secrets or test data in the changed code
# ✅ No test files modified to make tests pass
```

### Rule 3: Never merge without understanding what changed

```bash
# Quick human-readable summary of what Codex changed
git diff HEAD~1 --stat        # which files, how many lines
git diff HEAD~1 -- [file]     # line-by-line diff for specific file

# Never commit just because tests pass
# Tests don't catch: wrong logic, scope creep, security issues, added tech debt
```

---

## 4. Approval Policy as a Safety System

```
suggest (safe mode):
  - Use for: new codebases, unfamiliar code areas, learning, sensitive files
  - Risk: zero — nothing happens without your explicit y
  - Rule: this is your default for your first week with Codex on any project

auto-edit (balanced mode):
  - Use for: daily development, well-understood code areas
  - Risk: low — file changes apply automatically, commands still need approval
  - Rule: your standard day-to-day mode after initial familiarity

full-auto (power mode):
  - Use for: well-defined, bounded tasks on well-understood code
  - Risk: medium — everything executes without pausing
  - Prerequisite: git checkpoint commit before every full-auto session
  - Rule: review the full diff after every full-auto session

NEVER full-auto without git checkpoint.
```

---

## 5. The Test File Protection Rule

```
Codex will modify test files to make failing tests pass.
This is almost always wrong — test files are the truth source.
If the tests fail, the implementation is wrong. The tests are not.

How to prevent it:
  Add to every implementation prompt:
  "Constraint: do not modify any test file."

  Add to AGENTS.md:
  "Codex must never modify test files to fix failing tests.
   Fix the implementation. If the test itself is wrong, ask the user."

  If Codex still modifies tests:
  Check if the test was actually written incorrectly (rare but possible).
  If the test is correct: reject the change, fix the implementation.
```

---

## 6. Safe Handling of Destructive Operations

```bash
# Operations that need human-in-the-loop (not Codex full-auto):
# - Database migrations (ALTER TABLE, DROP COLUMN, DELETE without WHERE)
# - Infrastructure changes (terraform apply, kubectl delete)
# - System file modifications (/etc, ~/.ssh, system config)
# - Production deployment commands

# How to protect against these in AGENTS.md:
```

Add to AGENTS.md:
```markdown
## Forbidden Actions
- NEVER run database migration commands (alembic upgrade, flyway migrate)
- NEVER run kubectl delete or terraform destroy
- NEVER modify files outside the src/ and tests/ directories
- NEVER run rm without explicit confirmation
- NEVER push to git remote (git push)

For any of these operations: stop and ask the user.
```

---

## 7. Output Review Discipline

```
"It looked right" is not a review.

What reviewing means:
  1. Open the diff: git diff HEAD~1
  2. Read each changed file — not skim, read
  3. For each changed line: can I explain why this change is here?
  4. For each new import: do I recognize this library and why it's needed?
  5. For each new function/class: does this match what I asked for?

The standard for committing is: I can explain every changed line.
Not: the tests passed. Not: it compiles. Not: Codex said it's done.

Time investment: 5-10 minutes for most Codex tasks.
Return: you catch scope creep, wrong logic, and security issues before they ship.
```

---

## 8. Safe Codex in CI/CD Pipelines

```bash
# Risky: full-auto in CI without constraints
codex --approval-policy full-auto --quiet "fix any failing tests"
# Can delete tests, modify fixtures, change logic to match expected output

# Safe: specific, bounded, read-only tasks in CI
codex --approval-policy suggest --quiet "identify which tests need updating after the schema change"
# Returns a list — a human decides what to act on

# Never in CI production pipelines:
# - full-auto with no scope
# - tasks that modify migration files
# - tasks that push to git remote
```

---

## Production Pitfalls

```
PITFALL: Pasting a .env file into a Codex prompt for "context"
  Impact: All environment variables (DB passwords, API keys) sent to OpenAI API
  Fix: Describe the env vars by name only, never by value

PITFALL: Running full-auto on the main branch without checkpoint
  Impact: If Codex makes unexpected changes, you have no clean rollback point
  Fix: Always checkpoint first; work on a feature branch

PITFALL: Accepting test modifications without reviewing them
  Impact: Tests become mirrors of the implementation — bugs are hidden, not caught
  Fix: Add "do not modify tests" to AGENTS.md permanently

PITFALL: Using real production data as test cases in prompts
  Impact: PII in API logs, potential GDPR/privacy violation, data breach risk
  Fix: Synthetic data patterns (fake@example.com, test user IDs)
```

---

## Interview Traps

```
TRAP: "I only need to worry about safety when using full-auto mode"
TRUTH: Even suggest mode sends your code to the OpenAI API. If the code contains secrets,
       PII, or proprietary logic you're not authorized to share — suggest mode is not safe.
       The safety rules apply to the prompt content, not the approval policy.

TRAP: "Git checkpoints only matter for big refactors"
TRUTH: The checkpoint habit exists because you can't predict which task will go wrong.
       A 2-line feature change can trigger a cascade of unexpected file modifications.
       Checkpoint before every full-auto — regardless of perceived scope.

TRAP: "Tests passing means the output is safe to commit"
TRUTH: Tests passing tells you nothing about: scope creep, secret exposure, wrong abstractions,
       or security vulnerabilities. Diff review is mandatory even when all tests pass.
       Tests and diff review are separate gates — both required.
```

---

## Revision Checklist

- [ ] Know what 3 categories of data never belong in a Codex prompt
- [ ] Have a synthetic data set ready for testing scenarios
- [ ] Git checkpoint habit is automatic before every full-auto session
- [ ] AGENTS.md has a "Forbidden Actions" section
- [ ] Know how to do a diff review after a Codex session (git diff HEAD~1)
- [ ] Can describe the difference in risk between suggest/auto-edit/full-auto
