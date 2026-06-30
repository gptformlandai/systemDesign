# Daily Workflow Scenarios — Gold Sheet

> **Track**: Codex Mastery Track — Group 5: Scenario Practice
> **File**: 1 of 4 (Track File #26)
> **Audience**: Developers who want to build daily Codex workflow habits
> **Read after**: All concept files — practice sessions, not theory

---

## ⭐ Beginner Tier

### B1: Explain before touching (5 minutes)

Pick any file you'll work on today. Before opening it in your editor:

```bash
codex "Explain [filename]:
       1. What does it do?
       2. What are its inputs and outputs?
       3. What are 3 things that could go wrong?
       Make no changes."
```

What you're training: the habit of orienting before acting.
A developer who explains first makes fewer wrong changes.

### B2: Plan without implementing

You have a ticket: "Add rate limiting to the login endpoint."

```bash
codex "I need to add rate limiting to POST /auth/login in src/api/auth.py.
       Plan this (do not implement):
       1. What files need to change?
       2. What approach should I use (middleware? in-handler? Redis?)
       3. What are the risks?
       4. What tests do I need?
       Plan only."
```

Review the plan. Would you have planned it the same way? What did Codex miss?

---

## Scenario 1 — Morning Planning Sprint (10 minutes)

**Situation**: Monday morning, 3 tickets in the backlog, no idea where to start.

```bash
codex --model gpt-4.1 \
  "I have these 3 tasks today:
   1. Add pagination to GET /orders
   2. Fix the broken login test (tests/test_auth.py::test_login_expired fails)
   3. Add docstrings to src/notifications/service.py
   
   Help me plan:
   1. Which should I tackle first and why? (dependencies? risk? complexity?)
   2. For each task: what files are involved?
   3. For each task: what's the verification command?
   4. What's the total estimated time?
   
   Do not implement. Plan only."
```

Expected output: ordered list with rationale, file list, verification per task.
Adjustment if wrong: if Codex orders incorrectly, identify WHY (it may have missed a dependency you know about).

---

## Scenario 2 — Debugging a CI Failure (10 minutes)

**Situation**: pushed a commit, CI fails, local tests pass.

```bash
# Step 1: Capture the exact CI output
# Copy the failure from GitHub Actions / Jenkins / etc.

codex "CI is failing but local tests pass.
       CI environment: GitHub Actions, Ubuntu 22.04, Python 3.11
       Local environment: macOS 14, Python 3.11
       
       Failure output:
       [paste CI failure here]
       
       Common differences to check:
       - Missing environment variables in CI
       - Timezone (UTC in CI vs local)  
       - File path case sensitivity (Linux vs macOS)
       - Test ordering dependencies
       
       Diagnose the root cause. Propose the minimum fix.
       Do not modify test files."
```

---

## Scenario 3 — Pre-Commit Review (5 minutes)

**Situation**: about to commit 3 files, want a quick safety check.

```bash
STAGED=$(git diff --staged --name-only)
codex "Pre-commit review. Staged files: $STAGED
       
       Check:
       1. Any SQL injection risk in the changed code?
       2. Any new endpoint missing auth check?
       3. Any error path that could leak internal error details to API callers?
       4. Any changed function missing a test for the new behavior?
       
       Report: table with severity + issue + file:line + fix
       Final verdict: APPROVED / CHANGES REQUIRED
       Do not make changes."
```

If CHANGES REQUIRED: fix before committing.
If APPROVED: commit with confidence.

---

## Scenario 4 — Implementing a Ticket with Verification

**Situation**: ticket says "Add input validation to the create subscription endpoint."

```bash
# Step 1: Plan
codex "Ticket: 'Add input validation to POST /subscriptions'
       Plan:
       1. What inputs need validation? (look at the request model)
       2. What validation rules for each field?
       3. What error should be returned for invalid input?
       4. What tests do I need?
       Plan only — do not implement."

# Review the plan. Adjust if needed.

# Step 2: Implement
codex --approval-policy auto-edit \
  "Implement input validation for POST /subscriptions in src/api/subscriptions.py.
   Validation rules: [paste from plan or your ticket]
   Error response: HTTPException(422, detail=[field: error])
   Do not modify tests.
   Verification: pytest tests/test_subscription_api.py -x"
```

---

## Scenario 5 — End-of-Day Learning Capture (5 minutes)

**Situation**: end of a development day. What did you learn? What should you reuse?

```bash
codex "Generate end-of-day notes for my development session today.
       
       I worked on: [brief description of what you did]
       
       Generate:
       1. Best prompt from today: [if you remember one that worked well, describe it]
       2. What failed: [any prompt that produced wrong output]
       3. AGENTS.md update needed: [any mistake Codex made that a rule would prevent]
       4. New script candidate: [any prompt you typed 2+ times — describe it]
       5. Tomorrow: [what's the first task tomorrow and what context will Codex need]
       
       Keep it under 200 words total."
```

---

## Self-Assessment

After running each scenario, rate your performance:

| Scenario | Ran it? | Result? | What I'd change next time |
|----------|---------|---------|--------------------------|
| B1: Explain before touching | | | |
| B2: Plan without implementing | | | |
| 1: Morning planning | | | |
| 2: CI debugging | | | |
| 3: Pre-commit review | | | |
| 4: Implement with verification | | | |
| 5: End-of-day capture | | | |

---

## Revision Checklist

- [ ] Morning planning ritual run at least 3 days this week
- [ ] Pre-commit review run before every commit this week
- [ ] End-of-day capture run at least 2 days this week
- [ ] At least one CI failure debugged using the structured prompt
- [ ] At least one implementation done with explicit verification command
