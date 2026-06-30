# Code Review Scenarios — Gold Sheet

> **Track**: Claude Mastery Track — Group 5: Scenario Practice
> **File**: 4 of 4 (Track File #29)
> **Audience**: Developers practicing structured code review with Claude

---

## ⭐ Beginner Tier — First Code Review with Claude (B1–B2)

---

### Scenario B1 — Your First Security Check (10 min)

**Setup**: Paste this into a file and select it:

```python
def login(username, password, db):
    query = f"SELECT * FROM users WHERE username = '{username}' AND password = '{password}'"
    user = db.execute(query).fetchone()
    if user:
        return {"token": generate_token(user.id), "message": f"Welcome {username}!"}
    return {"error": f"Login failed for user: {username}"}
```

```
"Perform a security review of this function:
[paste or select the code]

For each issue:
  - SEVERITY: CRITICAL / HIGH / MEDIUM / LOW
  - What the vulnerability is
  - How an attacker could exploit it
  - The specific fix

Do not suggest style improvements — only security issues."
```

**Expected findings**:
- CRITICAL: SQL injection — `f"...{username}...{password}"` → use parameterized queries
- HIGH: User enumeration — different error for valid/invalid user → same message for both
- HIGH: PII in logs — `f"Welcome {username}"` and `f"Login failed for user: {username}"` → don't log username

**What you're building**: The habit of security review before every PR.

---

### Scenario B2 — Find Missing Tests (10 min)

**Setup**: Paste this:

```python
def calculate_shipping(weight_kg, destination, express=False):
    base_rate = 5.00
    if destination == "international":
        base_rate = 15.00
    cost = base_rate + (weight_kg * 0.50)
    if express:
        cost *= 1.5
    if weight_kg <= 0:
        raise ValueError("Weight must be positive")
    return round(cost, 2)
```

And imagine the only test is:
```python
def test_calculate_shipping_domestic():
    assert calculate_shipping(2.0, "domestic") == 6.0
```

```
"Review test coverage for calculate_shipping.

Implementation: [paste the function]
Current tests: [paste test]

What test cases are missing?
For each gap: what bug could exist that the current tests would NOT catch?
Priority: HIGH / MEDIUM / LOW"
```

---

## Intermediate Tier — Scenarios 1–6

---

## Scenario 1 — Security-Focused Review (15 min)

**Code to review** (paste into editor):

```python
from fastapi import APIRouter, Depends, HTTPException
from sqlalchemy.ext.asyncio import AsyncSession
from sqlalchemy import text

router = APIRouter()

@router.post("/login")
async def login(request: LoginRequest, db: AsyncSession = Depends(get_db)):
    result = await db.execute(
        text(f"SELECT * FROM users WHERE email = '{request.email}'")
    )
    user = result.fetchone()
    if not user or not verify_password(request.password, user.password_hash):
        raise HTTPException(status_code=401, detail=f"Login failed for {request.email}")
    token = create_jwt(user.id)
    print(f"User {request.email} logged in from IP {request.client.host}")
    return {"token": token}
```

```
/security

"Security review of @selection.

For each issue:
  SEVERITY: CRITICAL / HIGH / MEDIUM / LOW
  ISSUE: describe the vulnerability
  ATTACK VECTOR: how it could be exploited
  FIX: the specific code change
  OWASP: category if applicable"
```

**Expected findings**: SQL injection, user enumeration, PII logging (email + IP), no rate limiting.

---

## Scenario 2 — Test Coverage Review (10 min)

```
"Review test coverage:

Implementation: @file:src/services/discount_service.py
Tests: @file:tests/unit/test_discount_service.py

Report:
1. Functions with NO tests
2. Error conditions in the code with no test
3. Edge cases missing (negative values, zero, boundary)
4. Any tests that are tautological (always pass regardless of logic)

Output: prioritized list, most critical first.
For each HIGH gap: generate the missing test."
```

---

## Scenario 3 — Architecture and Maintainability Review (15 min)

```
"Review @selection for architecture and maintainability.

Evaluate:
1. Single Responsibility: does this class/function do one thing?
2. Coupling: what would break if dependency X changes?
3. Testability: what makes this hard to test in isolation?
4. Naming: any unclear or misleading names?
5. Error handling: silent failures? Swallowed exceptions?
6. Future fragility: what will break when requirements change?

For each issue:
  - Problem description
  - Consequence (what breaks when this is a problem)
  - Concrete improvement
  - Effort: SMALL (< 1h) / MEDIUM (half day) / LARGE (> 1 day)"
```

---

## Scenario 4 — Generate Constructive Review Comments (10 min)

**Setup**: You found two issues in a teammate's PR.

```
"Generate constructive PR review comments for these issues:

Issue 1: handle_payment() has no error handling for network timeout.
Location: @selection

Issue 2: The test mocks PaymentGateway but doesn't verify it was called
with the correct charge amount — just that it returned a value.

For each: generate a PR comment that:
- Acknowledges what's correct (if applicable)
- Explains the issue without being critical
- Shows the specific fix as a code suggestion
- Explains WHY the fix is better
- Under 150 words"
```

---

## Scenario 5 — PR Description Generation (5 min)

```
"Generate a GitHub PR description.

Changed files: @file:[list your changed files]
What changed (paste your notes):
[your raw notes or git log --oneline]

Format:
## Summary
## Changes Made
## How to Test
## Breaking Changes (if any)
## Checklist: [ ] tests pass [ ] security reviewed [ ] docs updated

Under 300 words. Factual. No marketing language."
```

---

## Scenario 6 — Full Review Sprint (30 min)

**Goal**: Review a file you wrote as if it were a teammate's PR.

**Step 1 (5 min)**: Run `/security` on the file. Note inline findings.
**Step 2 (5 min)**: Run `/review` (full pre-commit review). Note additions.
**Step 3 (5 min)**: Run test gap analysis. Note untested paths.
**Step 4 (5 min)**: Generate the PR description. Does it describe what the code does?
**Step 5 (10 min)**: Fix any CRITICAL or HIGH issues found.

**Score using CRESTS**:
```
C — Correctness issues found: ___
R — Risk/security issues found: ___
E — Error handling gaps found: ___
S — Style/naming issues: ___
T — Test gaps found: ___
S — Scope (did anything surprise you?): ___

Total issues caught: ___
Would you approve this PR as written? Y / N / Approve with comments
```

**Success**: You caught at least 2 issues you didn't notice when you wrote the code.

---

## Revision Checklist

- [ ] Can run a security-focused review covering SQL injection, auth bypass, user enumeration, PII logging
- [ ] Can run a test coverage review and identify gaps by priority
- [ ] Can generate constructive, actionable review comments
- [ ] Can generate a PR description from a set of changed files
- [ ] Has run the Full Review Sprint on at least one of their own files
- [ ] Uses CRESTS framework to score review completeness
