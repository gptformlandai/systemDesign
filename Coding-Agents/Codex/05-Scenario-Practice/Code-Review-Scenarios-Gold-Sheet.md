# Code Review Scenarios — Gold Sheet

> **Track**: Codex Mastery Track — Group 5: Scenario Practice
> **File**: 4 of 4 (Track File #29)
> **Audience**: Developers who want to use Codex for systematic, thorough code reviews
> **Read after**: Debugging-Scenarios-Gold-Sheet.md

---

## ⭐ Beginner Tier

### B1: Your first security check (5 minutes)

```bash
# Pick any function that touches a database
codex "Security check for get_user_by_email() in src/users/repository.py:
       
       Check ONLY: is the SQL query parameterized?
       Show me the relevant line.
       If not parameterized: show what the fix looks like.
       Do not make changes."
```

This is the single most impactful security check for database-touching code.

### B2: Find missing tests (5 minutes)

```bash
codex "Test coverage check for create_order() in src/orders/service.py.
       
       From the function code:
       1. List every if/else branch
       2. List every exception the function can raise
       3. Check tests/test_order_service.py — which branches and exceptions have no test?
       
       Do not generate tests yet — just list what's missing."
```

---

## Scenario 1 — Security-Focused Review (15 minutes)

```bash
codex --system-prompt \
  "You are a security engineer. For every finding: SEVERITY, ATTACK VECTOR, FIX, OWASP category." \
  "Security review of src/auth/service.py.
   
   Check:
   1. SQL injection: is every query parameterized?
   2. Password handling: is bcrypt (not MD5/SHA1) used? Are passwords ever logged?
   3. JWT: is the secret from environment variable? Is expiry validated?
   4. Brute force: is there rate limiting on login attempts?
   5. Error messages: do they reveal internal details? (stack traces to API callers?)
   
   Format: | SEVERITY | ISSUE | LINE | FIX | OWASP |
   Final: APPROVED / CHANGES REQUIRED"
```

---

## Scenario 2 — Test Coverage Review (10 minutes)

```bash
codex "Test coverage review for src/payments/service.py.
       
       For each public function:
       1. Happy path tested? (Y/N)
       2. Error cases tested? (list which ones are and aren't)
       3. Edge cases tested? (empty input, zero, None, max value)
       
       Focus: find the highest-risk untested paths.
       
       Output: table per function, then top 3 gaps to address first.
       Do not generate tests yet."
```

---

## Scenario 3 — Architecture Review (15 minutes)

```bash
codex --model gpt-4.1 \
  "Architecture review of the new feature in src/orders/.
   
   Check:
   1. Layer violations: does API code make direct DB calls? Does service code use HTTP concepts?
   2. Coupling: are any modules importing from modules they shouldn't?
   3. Error propagation: is the error handling chain correct (repo → service → API)?
   4. Backwards compatibility: does this feature break any existing callers?
   5. Future extensibility: will this be easy to change in 6 months?
   
   Format: numbered findings with severity (architectural concern vs minor issue)
   Final: ARCHITECTURE APPROVED / CONCERNS TO ADDRESS / REWORK NEEDED"
```

---

## Scenario 4 — Full Pre-PR Review (20 minutes)

```bash
CHANGED=$(git diff main..HEAD --name-only)

codex --model gpt-4.1 \
  "Full code review for PR. Changed files: $CHANGED
   
   Review in this order:
   1. Security (CRITICAL/HIGH first): SQL injection, auth bypass, PII exposure
   2. Correctness: logic errors, wrong assumptions, missing null checks
   3. Tests: error paths not covered, tautological tests, mocks of own code
   4. Architecture: layer violations, wrong dependencies
   5. Conventions: AGENTS.md compliance, naming, error handling patterns
   6. Backwards compatibility: any existing callers broken?
   
   For each finding: SEVERITY | ISSUE | FILE:LINE | FIX
   Final: APPROVED / APPROVE WITH COMMENTS / CHANGES REQUIRED"
```

---

## Self-Assessment

| Scenario | Findings count | CRITICAL/HIGH? | Fixed before PR? |
|----------|---------------|---------------|-----------------|
| B1: SQL check | | | |
| B2: Missing tests | | | |
| 1: Security review | | | |
| 2: Test coverage | | | |
| 3: Architecture | | | |
| 4: Full pre-PR | | | |

**Target**: zero CRITICAL or HIGH findings per PR.

---

## Revision Checklist

- [ ] Security review run on every auth/SQL file before merge
- [ ] Test coverage review identifies uncovered error paths
- [ ] Architecture review catches layer violations
- [ ] Full pre-PR review used before every significant PR
- [ ] No CRITICAL or HIGH security findings in committed code
