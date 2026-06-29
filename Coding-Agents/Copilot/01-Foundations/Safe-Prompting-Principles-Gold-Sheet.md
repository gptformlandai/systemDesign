# Safe Prompting Principles — Gold Sheet

> **Track**: Copilot Mastery Track — Group 1: Foundations
> **File**: 5 of 6 (Track File #5)
> **Audience**: All Copilot users — non-negotiable baseline
> **Read after**: Copilot-Chat-Fundamentals-Gold-Sheet.md

---

## 1. Practical Impact Meter

| Topic | Impact | Why It Gets Ignored |
|---|---|---|
| Never paste real secrets into Copilot | ★★★★★ | Devs think "it's just for context" — secrets in prompts are processed and potentially logged |
| Treat all Copilot output as a first draft | ★★★★★ | The most dangerous bug is one that LOOKS right |
| Use synthetic/anonymized data for debugging | ★★★★★ | Real customer data in prompts violates privacy even if Copilot "deletes" it |
| Commit before Agent Mode runs | ★★★★★ | Agent Mode can modify many files at once — uncommitted work is unrecoverable |
| Review every diff before accepting | ★★★★★ | Accept-All is the fastest way to introduce hard-to-find bugs |
| Small, reviewable changes — not huge refactors | ★★★★☆ | Large AI-generated changes are hard to review and blame correctly |
| Validate generated dependencies before using | ★★★★☆ | Copilot can suggest outdated, deprecated, or malicious-looking package names |
| Run tests after every Copilot-assisted change | ★★★★★ | Generated code can break existing behavior without type errors |

---

## 2. The Non-Negotiable Rules

### Rule 1 — Never Paste Real Secrets

```
NEVER paste into Copilot (Chat, inline, or any prompt):
  - API keys (AWS, OpenAI, Stripe, Twilio, etc.)
  - Database connection strings with passwords
  - JWT secrets or signing keys
  - OAuth client secrets
  - SSH private keys
  - TLS/SSL certificates or private keys
  - Passwords of any kind
  - Personal access tokens (GitHub, GitLab, etc.)
  - Production database credentials
  - .env file contents with real values
  - Any credential that grants access to a system

Why this matters:
  GitHub Copilot transmits your prompt to Microsoft/GitHub cloud servers.
  Even if data is not stored permanently, it passes through external systems.
  If your company has data residency requirements, real secrets in prompts
  may violate compliance policies (GDPR, HIPAA, SOC 2, etc.).
  
What to do instead:
  Replace secrets with placeholders in your prompts.

WRONG:
  "My database URL is postgresql://admin:SuperSecret123@prod-db.company.com:5432/users
  Why is my connection failing?"

CORRECT:
  "My database URL follows the pattern postgresql://user:password@host:port/dbname
  I'm connecting to a PostgreSQL database. Why might the connection fail?"
```

### Rule 2 — Use Synthetic Data for Debugging

```
NEVER paste real:
  - Customer names, emails, or contact information
  - User IDs that can be correlated to real people
  - Financial transaction data
  - Health records or medical information
  - Any PII (personally identifiable information)

How to anonymize debugging data:

Real data (NEVER use):
  {
    "user_id": 82734,
    "name": "John Smith",
    "email": "john.smith@company.com",
    "ssn": "123-45-6789",
    "account_balance": 47832.50
  }

Safe synthetic replacement (ALWAYS use this):
  {
    "user_id": 12345,
    "name": "Test User",
    "email": "testuser@example.com",
    "ssn": "000-00-0000",
    "account_balance": 1000.00
  }

For error debugging: Copy the error message and stack trace only.
Stack traces show code paths and variable names — rarely contain real PII.
If they do (e.g., a log line that captured a real email), redact before pasting.
```

### Rule 3 — Review Every Diff Before Accepting

```
For inline suggestions:
  Read the ghost text before pressing Tab.
  If it's more than 3 lines, read every line.

For Chat output code:
  Read the full code block before copying it.
  Ask yourself: "Does this do exactly what I asked for?"
  Ask yourself: "What happens in edge cases?"
  Ask yourself: "Does this import anything I didn't expect?"

For Copilot Edits / Agent Mode:
  NEVER click "Accept All" without reading the diff.
  Review file by file.
  For each file: scroll through the changes.
  Look for: new imports, removed error handling, changed method signatures,
            new dependencies added, files deleted unexpectedly.

The three-second rule is not enough:
  Copilot output that compiles and looks correct can still:
  - Use deprecated APIs with subtle behavior changes
  - Remove exception handling silently
  - Miss null checks for edge cases
  - Generate SQL with injection vulnerabilities if inputs aren't parameterized
  - Add transitive dependencies with known vulnerabilities
```

### Rule 4 — Commit Before Agent Mode

```
Before any Agent Mode session:
  git add .
  git commit -m "checkpoint: before Agent Mode session"

Why:
  Agent Mode can modify 10+ files in one session.
  If the result is wrong, you need a clean recovery point.
  Without a commit, "Undo" history may not cover all the changes.

After Agent Mode completes:
  Review all changes in the Source Control diff panel.
  Run your full test suite.
  If tests fail or the output is wrong: git checkout . (restore from commit)
  If tests pass: review each file change manually, then commit with a descriptive message.
```

### Rule 5 — Run Tests After Every Change

```
Test discipline with Copilot:

1. Before any Copilot-assisted change: run the test suite. Note which tests pass.
2. Make the Copilot change.
3. Run the test suite again.
4. If any test that previously passed now fails: the Copilot change is wrong.
5. Ask Copilot to explain why the test fails, then fix it.

For code with no existing tests:
  Ask Copilot to generate tests BEFORE generating the implementation.
  Test-first ensures the tests describe actual expected behavior.
  Implementation-first means tests are generated to match the implementation — not the requirement.

Minimum test check after Copilot-assisted change:
  - Unit tests for the changed function/class
  - Integration tests if the change touches a boundary (API, DB, external service)
  - Regression: run the full suite at least once per feature
```

### Rule 6 — Small, Reviewable Changes

```
Prefer:
  "Add null check for user_id in the create_order function"
  → One function, one change, easy to review

Over:
  "Refactor the entire order processing module"
  → 20 files changed, impossible to review efficiently

When you need a large change:
  Break it into sequential steps.
  Each step: Copilot generates, you review, you test, you commit.
  Next step: repeat.

Why large changes are dangerous:
  - Harder to catch bugs (too much to review at once)
  - Git blame becomes useless (entire file is "AI" authored)
  - If a test fails, hard to isolate which change caused it
  - Harder to revert cleanly
```

### Rule 7 — Validate Generated Dependencies

```
When Copilot suggests adding a new library:

1. Verify the package exists on PyPI/npm/Maven Central:
   pip index versions <package-name>
   npm view <package-name> version

2. Check the package's maintenance status:
   - Last release date
   - Open issues / CVEs
   - Star count and community

3. Check for known vulnerabilities:
   pip audit  (after pip install)
   npm audit  (after npm install)

4. Check for typosquatting: if the package name is very similar to a well-known
   package but with a typo or extra character, it may be malicious.
   Example: "requests" is legitimate; "requestes" or "request-lib" may not be.

5. Pin versions in requirements:
   requests==2.31.0  (not just: requests)
   This prevents supply chain attacks from future version changes.
```

### Rule 8 — Verify Generated Commands Before Running

```
When Copilot suggests a shell command:
  Read it fully before pressing Enter.
  Ask: "What does this command do? What does each flag mean?"
  If uncertain: ask Copilot "Explain this command: [command]"

Dangerous patterns to watch for:
  Commands with rm -rf
  Commands piped to bash or sh (curl ... | bash is a common attack vector)
  Commands that write to system directories
  Commands that modify git history (git push --force, git reset --hard)
  Commands that create or modify AWS/cloud resources

Safe practice:
  For commands that could be destructive: run in a sandbox or test environment first.
  For git commands: check what they will do with --dry-run or git status first.
```

---

## 3. Responsible AI Mindset

```
Copilot is a productivity multiplier, not a decision maker.

Copilot:
  ✓ Accelerates implementation of things you already understand
  ✓ Provides options and alternatives to consider
  ✓ Saves time on boilerplate and standard patterns
  ✓ Helps learn by generating examples to study
  ✓ Is a first-draft generator

Copilot is NOT:
  ✗ An authority on correct architecture
  ✗ Aware of your production constraints
  ✗ Guaranteed to be correct
  ✗ A replacement for understanding code
  ✗ Able to make final decisions about your system
  ✗ An acceptable substitute for code review
  ✗ A source of truth for security-sensitive decisions

Your job: Be the intelligent, accountable developer who uses Copilot as one tool
among many — not as a crutch that bypasses critical thinking.
```

---

## 4. Safe Prompting Checklist

Use this before any Copilot session:

```
Before prompting:
[ ] No secrets in the code or text I'm about to share
[ ] No real customer PII in the debugging examples
[ ] I've committed recent work (especially before Agent Mode)
[ ] I know what I expect the output to look like
[ ] I have a test plan for validating the output

During prompting:
[ ] Prompts contain context, goal, constraints — not just vague requests
[ ] I'm using synthetic/anonymized examples for debugging
[ ] I reference files explicitly instead of pasting confidential code blocks

After receiving output:
[ ] I read the entire diff/code before accepting
[ ] I verified any new packages exist and are safe
[ ] I verified any generated commands are safe to run
[ ] I ran the relevant tests
[ ] I did not accept changes that I can't explain
[ ] I committed the final accepted change with a descriptive message
```

---

## 5. Common Safety Traps — With Solutions

### Trap 1 — Pasting `.env` File for Context

```
WRONG:
  "Here's my .env file for context: [pastes full .env with real credentials]
  Why is my database connection failing?"

CORRECT:
  "My .env file has these keys: DATABASE_URL, SECRET_KEY, API_KEY.
  The DATABASE_URL format is postgresql://user:password@host:port/dbname.
  I'm getting a connection refused error. What should I check?"
```

### Trap 2 — Real Customer Data in Debug Example

```
WRONG:
  "This function fails for this record: {'user_id': 82734, 'email': 'john.smith@acme.com', 'ssn': '...'}"

CORRECT:
  "This function fails when the input dict has a user_id and email but the email
  format is unusual. Example: {'user_id': 12345, 'email': 'user+tag@example.com'}"
```

### Trap 3 — Accepting Agent Mode Output Without Review

```
WRONG:
  Agent Mode completes → "Accept All Changes" → commit without reading

CORRECT:
  Agent Mode completes → open Source Control → review each file diff →
  run test suite → if tests pass, review each change manually →
  commit with descriptive message
```

### Trap 4 — Blindly Running a Suggested npm/pip Install

```
WRONG:
  Copilot says "Run: pip install fast-json-parser"
  Developer runs it without checking

CORRECT:
  Check if "fast-json-parser" exists on PyPI
  pip index versions fast-json-parser
  → If not found, or if it looks like a typosquat of a known package, don't install
```

---

## 6. Revision Checklist

- [ ] Can list the 8 types of secrets that must never go into Copilot
- [ ] Knows how to anonymize debugging data before sharing with Copilot
- [ ] Has the "commit before Agent Mode" habit established
- [ ] Reviews every diff before accepting (no blind Accept All)
- [ ] Knows how to validate a newly suggested package before installing
- [ ] Understands Copilot as a first-draft generator, not an authority
- [ ] Has the safe prompting checklist available for reference
- [ ] Can identify the 5 common safety traps
