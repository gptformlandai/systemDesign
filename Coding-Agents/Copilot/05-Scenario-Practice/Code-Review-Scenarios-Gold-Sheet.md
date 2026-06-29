# Code Review Scenarios — Gold Sheet

> **Track**: Copilot Mastery Track — Group 5: Scenario Practice
> **File**: 4 of 4 (Track File #31)
> **Audience**: Developers practicing Copilot-assisted code review workflows

---

## 1. Copilot Code Review Surface — How to Use It

### VS Code Code Review (inline comments on your code)

```
How to trigger in VS Code:
  Method 1 (inline): Select code → right-click → "Copilot: Review and Comment"
  Method 2 (file): Open a file → Command Palette → "GitHub Copilot: Review"
  Method 3 (diff): In Source Control diff view → click the Copilot icon

What it produces:
  - Inline comments on specific lines in your editor
  - Each comment: describes the issue and suggests a fix
  - You can: accept the suggestion, dismiss, or ask for more detail

Best use:
  Before creating a PR → run on your changed files
  As a structured first-pass before human review
  Specifically for: security patterns, error handling, edge cases
```

### GitHub.com PR Code Review

```
Where: Open a PR on GitHub.com → "Copilot" icon in the Files Changed tab
What it produces: Inline review comments directly in the PR

Limitations vs Chat review:
  - Less controllable — Copilot decides what to comment on
  - Cannot specify "focus on security" without a custom instructions setup
  - Good for: quick automated first pass
  - Not a substitute for a targeted security or architecture review
```

---

## 2. Scenario 1 — Security-Focused Review (15 minutes)

**Setup**: Review this code before merging a PR. Language-agnostic concepts.

```
Simulated code to review (paste in editor and select):
def login(request: LoginRequest, db: AsyncSession = Depends(get_db)):
    user = await db.execute(
        text(f"SELECT * FROM users WHERE email = '{request.email}'")
    )
    if not user or not check_password(request.password, user.password_hash):
        raise HTTPException(status_code=401, detail=f"Login failed for {request.email}")
    token = create_token(user.id)
    print(f"User {request.email} logged in from {request.client_ip}")
    return {"token": token}
```

**Exercise — Security Review**:
```
"Perform a security review of #selection.

For each issue found:
- SEVERITY: CRITICAL / HIGH / MEDIUM / LOW
- ISSUE: What the vulnerability is
- ATTACK VECTOR: How it could be exploited
- FIX: Specific code change

Do not suggest generic improvements — only security issues."
```

**Expected findings**:
- CRITICAL: SQL injection via `text(f"...{request.email}")` — use parameterized
- HIGH: `detail=f"Login failed for {request.email}"` — reveals which email exists (user enumeration)
- HIGH: `print(f"User {request.email} logged in")` — logs PII (email)
- MEDIUM: No rate limiting visible on this endpoint

---

## 3. Scenario 2 — Test Coverage Review (10 minutes)

**Setup**: Review a PR that adds a new discount calculation function.

```
"Review the test coverage of #file:tests/unit/test_discount_service.py
against the implementation in #file:src/services/discount_service.py
(or select both files with #file references)

Report:
1. Functions in the service with NO tests
2. Error conditions in the code that are NOT tested
3. Specific edge cases missing (negative amounts, zero, overflow)
4. Are the existing tests testing behavior or just implementation?
5. Is there a test for concurrent discount application?

Output: prioritized list — most critical missing tests first."
```

**Success criteria**: Identify at least 3 genuine test gaps that could mask bugs.

---

## 4. Scenario 3 — Architecture and Maintainability Review (15 minutes)

**Setup**: Review a PR that adds a new UserActivityTracker class.

```
"Review #selection for architecture and maintainability issues.

Evaluate:
1. Single Responsibility: does this class do one thing?
2. Coupling: what does this depend on? What would break if X changes?
3. Testability: can each method be tested in isolation? What makes it hard?
4. Naming: are method/variable names clear and consistent with the codebase?
5. Error handling: what happens when X fails? Is it handled?

For each issue:
- Describe the problem
- Explain the consequence (what breaks when this becomes a problem)
- Suggest a concrete improvement
- Effort estimate: SMALL (< 1 hour), MEDIUM (half day), LARGE (> 1 day)

Be direct. Do not compliment the code."
```

---

## 5. Scenario 4 — Generate Review Comments for a Real PR

**Setup**: You're reviewing a teammate's PR. Copilot helps generate constructive comments.

```
"Generate constructive PR review comments for these issues I found.

Issue 1: The handle_payment function has no error handling for network timeouts.
Location: #selection (select the function)

Issue 2: The test mocks the PaymentGateway but doesn't verify it's called with
correct arguments — just that it returns a value.

For each issue, generate a PR review comment that:
- Acknowledges what is done correctly
- Explains the issue clearly without being critical of the developer
- Shows the specific fix as a code suggestion
- Explains WHY the fix is better
- Stays under 150 words per comment"
```

---

## 6. Scenario 5 — Copilot Code Review — PR Summary Generation

**Setup**: Your PR has 12 changed files. Generate a description.

```
"Generate a PR description for my changes.

Changed files:
#file:[list your changed files with #file references]

What I changed (paste your raw notes or git log --oneline):
[paste]

Format:
## Summary
[2-3 sentences: what changed and why]

## Changes Made
[Bullet list of specific changes — one per file or feature area]

## How to Test
[Step-by-step: how does a reviewer verify this works?]

## Breaking Changes
[If any; 'None' if not applicable]

## Checklist
- [ ] Tests added or updated
- [ ] Security review completed
- [ ] Documentation updated if needed
- [ ] No hardcoded values

Keep under 300 words. Factual. No marketing language."
```

---

## 7. Scenario 6 — Full Code Review Sprint (30-minute practice)

**Goal**: Review a complete simulated PR using all Copilot review tools.

**Setup**: Take any file you wrote recently and treat it as a PR to review.

**Step 1 — Automated scan (5 min)**:
```
VS Code → Select the file → right-click → "Copilot: Review and Comment"
Note: which inline comments appear
Accept any that are clearly correct
```

**Step 2 — Security check (5 min)**:
```
Run the security review prompt from Copilot-For-PR-Review-Gold-Sheet.md
on the same file. Note anything the automated scan missed.
```

**Step 3 — Test gap analysis (5 min)**:
```
Run the test gap analysis prompt. Note untested paths.
```

**Step 4 — PR description (5 min)**:
```
Run the PR description prompt. Does it accurately describe what the file does?
```

**Step 5 — Review (10 min)**:
```
Score what you found using the CREST framework:
C — Correctness issues found:
R — Risk issues found:
E — Error handling gaps found:
S — Style alignment issues found:
T — Test gaps found:

Total issues: ___
Would you approve this PR as-is? Y / N / Approve with comments
```

**Success criteria**: You caught at least 2 issues in the simulated review that you didn't catch when you wrote the code.
