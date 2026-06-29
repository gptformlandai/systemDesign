# Safe Usage Principles — Gold Sheet

> **Track**: Claude Mastery Track — Group 1: Foundations
> **File**: 5 of 6 (Track File #5)
> **Audience**: All Claude users — non-negotiable baseline
> **Read after**: Prompt-Engineering-Fundamentals-Gold-Sheet.md

---

## 1. The Non-Negotiable Rules

### Rule 1 — Never Paste Real Secrets

```
NEVER paste into Claude (Chat, Code, API, any surface):
  - API keys (Anthropic, OpenAI, AWS, Stripe, Twilio)
  - Database connection strings with real passwords
  - JWT secrets or signing keys
  - OAuth client secrets
  - SSH private keys
  - .env file contents with real values
  - Personal access tokens
  - Any production credential

Why: Claude sends your prompt to Anthropic's servers.
     Even if data isn't stored, it passes through external systems.

Replace with placeholders:
  WRONG: "DB_URL=postgresql://admin:MyRealPass@prod.company.com/users"
  RIGHT: "DB_URL=postgresql://user:password@host:port/dbname"
```

### Rule 2 — No Real Customer Data

```
NEVER paste real:
  - Customer names, emails, phone numbers
  - User IDs that map to real people
  - Payment or financial transaction data
  - Health records
  - Any PII (personally identifiable information)

Use synthetic data:
  WRONG: {"user_id": 8273, "email": "john.smith@company.com", "balance": 47832}
  RIGHT: {"user_id": 12345, "email": "test@example.com", "balance": 1000}
```

### Rule 3 — Review Before Accepting

```
Every Claude-generated change is a first draft:
  ✓ Read the diff line by line before accepting
  ✓ Run tests after every change
  ✓ Check new imports — are they expected?
  ✓ Check error handling — was any removed?
  ✓ Verify you can explain every line Claude wrote

"It compiles" ≠ "It is correct"
"It looks right" ≠ "It is secure"
```

### Rule 4 — Checkpoint Commits Before Agent Sessions

```
Before any Claude Code agent session:
  git add . && git commit -m "checkpoint: before claude agent - [task]"

If the session goes wrong:
  git checkout .   ← restore all files to the checkpoint

Without the checkpoint: bad agent sessions have no recovery.
```

### Rule 5 — Verify Commands Before Running

```
When Claude suggests shell commands:
  Read the full command before pressing Enter.
  
Dangerous patterns to watch for:
  - rm -rf with any path
  - Commands piped to bash: curl ... | bash
  - Commands that modify git history (push --force, reset --hard)
  - Commands that touch system directories (/etc, /usr, /var)
  - Database commands without a WHERE clause

Claude can hallucinate commands that look correct but do the wrong thing.
If uncertain: "Explain what this command does before I run it."
```

### Rule 6 — Use Source Control

```
Every project touched by Claude must be in git.

Git is your safety net:
  - git diff → see exactly what Claude changed
  - git checkout . → undo all uncommitted changes
  - git log → audit what was done session by session
  - git tag → mark safe points before autonomous sessions

Working outside git with Claude = no recovery when things go wrong.
```

### Rule 7 — Treat Claude as First Draft Generator

```
Claude output is a first draft. You are the final authority.

Claude can:
  ✓ Generate plausible-looking code
  ✓ Suggest reasonable patterns
  ✓ Provide useful starting points

Claude can also:
  ✗ Hallucinate method names that don't exist
  ✗ Use deprecated APIs
  ✗ Generate code with subtle logical errors
  ✗ Miss edge cases
  ✗ Introduce security vulnerabilities

Your review is not optional. It is the critical last step.
```

---

## 2. Secret Leak Prevention

### Pre-Session Checklist

```
Before any Claude session involving code:
  [ ] CLAUDE.md does not contain real credentials
  [ ] No .env files with real values are open in context
  [ ] No real API keys are visible in any open file
  [ ] You will use placeholders for any sensitive values in examples
```

### Gitignore Template for Claude Projects

```bash
# Add to .gitignore:
.env
.env.local
.env.production
*.pem
*.key
*.p12
secrets.json
credentials.json
.claude/hooks/*.sh.log    # hook output logs (may contain command output)
```

### Secret Scanning Before Commit

```bash
# Install gitleaks:
brew install gitleaks

# Scan before every push:
gitleaks detect --source=. --verbose

# As a pre-commit hook:
cat > .git/hooks/pre-commit << 'EOF'
#!/bin/bash
gitleaks protect --staged --verbose
if [ $? -ne 0 ]; then
  echo "Secret detected! Remove before committing."
  exit 1
fi
EOF
chmod +x .git/hooks/pre-commit
```

---

## 3. Responsible AI Mindset

```
Claude is a productivity multiplier, not a decision maker.

Claude:
  ✓ Accelerates implementation of things you understand
  ✓ Provides starting points and options
  ✓ Saves time on boilerplate and patterns
  ✓ Helps you learn by generating examples

Claude is NOT:
  ✗ An authority on your production architecture
  ✗ Guaranteed to be correct
  ✗ A replacement for code review
  ✗ Able to make security-critical decisions alone
  ✗ Aware of your production constraints unless you tell it

Standard: Only commit code you understand and can defend in a code review.
```

---

## 4. Safe Usage Pre-Commit Checklist

```
[ ] No real secrets in any generated code
[ ] Tests pass
[ ] Diff reviewed line by line
[ ] No unnecessary new dependencies
[ ] No shell commands I didn't review
[ ] I can explain every Claude-generated line
[ ] Commit message follows conventional format
[ ] Source control has a checkpoint commit for the session
```

---

## 5. Revision Checklist

- [ ] Can list 7 types of secrets never to paste into Claude
- [ ] Knows how to use synthetic data instead of real data
- [ ] Has checkpoint commit habit before agent sessions
- [ ] Verifies all generated shell commands before running
- [ ] Has gitleaks or equivalent configured as a pre-commit hook
- [ ] Treats Claude output as first draft requiring review
- [ ] Has .gitignore covering .env and credential files
