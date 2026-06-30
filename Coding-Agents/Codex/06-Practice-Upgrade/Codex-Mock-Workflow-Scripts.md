# Codex Mock Workflow Scripts

> **Track**: Codex Mastery Track — Group 6: Practice & Upgrade
> **File**: 3 of 6 (Track File #32)
> **Audience**: Developers who want to rehearse Codex workflows under time pressure
> **Format**: Timed drills — set a timer, execute, score yourself

---

## How to Use This File

1. Pick a drill
2. Set a timer for the stated time limit
3. Execute using a real codebase or the sample scenario
4. Score yourself using the rubric at the bottom
5. Run each drill at least 3 times before moving to the next

---

## Drill 1 — Morning Planning Sprint (10 minutes)

**Scenario**: Monday 9:00 AM. Three tickets in Jira, unread.

```
Ticket 1: Add rate limiting to POST /auth/login (security requirement)
Ticket 2: Fix test: tests/test_notifications.py::test_send_email_retry is flaky
Ticket 3: Write docstrings for src/billing/service.py (tech debt)
```

**Task**: Use Codex to:
1. Order these tickets (dependencies, risk, urgency)
2. For each: identify file scope, verification command, estimated time
3. Output a structured day plan

**Time limit**: 10 minutes  
**Pass criteria**: Ordered list with rationale + file scope + verification per ticket

---

## Drill 2 — Fix a Failing Test (15 minutes)

**Scenario**: pytest fails on this test:

```
FAILED tests/test_orders.py::test_create_order_with_zero_price
AssertionError: Expected HTTPException(422), got 201
```

**Task**:
1. Identify the root cause (no implementation to look at — reason about it)
2. Write the Codex prompt that would fix it (include: scope, constraints, verification)
3. Add the stopping conditions for the loop

**Time limit**: 15 minutes  
**Pass criteria**: Prompt includes file scope + "do not modify test files" + verification command + stopping conditions

---

## Drill 3 — Security Review Sprint (15 minutes)

**Scenario**: About to commit 2 files — `src/auth/service.py` and `src/users/api.py`.

**Task**:
1. Write the codex-review command for these two files
2. The review must check: SQL injection, auth bypass, PII in logs, input validation
3. Format output as: SEVERITY | ISSUE | FILE:LINE | FIX
4. End with APPROVED / CHANGES REQUIRED

**Time limit**: 15 minutes  
**Pass criteria**: System prompt or task prompt has all 4 check categories + correct format spec

---

## Drill 4 — Full Feature Prompt (20 minutes)

**Scenario**: Ticket says: "Add GET /reports/weekly endpoint that returns order count and revenue for the past 7 days. Auth required (any authenticated user). Response: `{week_start, week_end, order_count, total_revenue}`"

**Task**: Write the complete Codex autonomous feature build prompt for this ticket:
- Phase 1: Planning prompt
- Phase 2: Implementation prompt (with scope, reference files, process, forbidden, done-when)
- Phase 3: Post-implementation review prompt

**Time limit**: 20 minutes  
**Pass criteria**: All 3 phases present. Implementation phase has: scope, process order, forbidden list, done-when with test commands.

---

## Drill 5 — Debug an Async Problem (15 minutes)

**Scenario**: This endpoint hangs (no response, eventually times out):

```python
@router.get("/users/{id}/profile")
async def get_profile(id: int, db: AsyncSession = Depends(get_db)):
    user = db.query(User).filter(User.id == id).first()
    return user
```

**Task**:
1. Identify the bug without running it
2. Write the Codex debugging prompt with: exact reproduction, root cause hypothesis, fix approach, verification
3. What stopping condition applies here?

**Time limit**: 15 minutes  
**Pass criteria**: Bug correctly identified (sync `db.query()` in async context). Prompt includes reproduction + fix + verification.

---

## Drill 6 — AGENTS.md for a New Project (20 minutes)

**Scenario**: You've just joined a project with this structure:
- FastAPI + PostgreSQL
- src/api/, src/services/, src/db/repositories/ (layered architecture)
- Tests in tests/ using pytest
- Deployment to AWS ECS via GitHub Actions

**Task**: Write a complete AGENTS.md from scratch (all 6 sections):
1. Project context
2. Architecture (layer rules)
3. Coding standards
4. Forbidden actions (at least 5)
5. Verification command
6. One subfolder override example

**Time limit**: 20 minutes  
**Pass criteria**: All 6 sections present. Forbidden list has 5+ items. No vague entries (each rule is specific and actionable).

---

## Drill 7 — Post-Incident Learning (10 minutes)

**Scenario**: Production incident 2 hours ago. POST /checkout returned 500 for 40% of requests for 25 minutes. Root cause: rate limiting middleware rejected requests from load balancer IPs. Fix: added load balancer IPs to allowlist. Incident resolved.

**Task**: Write the Codex post-incident learning prompt that produces:
1. Root cause analysis
2. Detection gap (why did it take 25 min to notice?)
3. 3 prevention rules to add to AGENTS.md
4. 2 tests that would have caught this in CI

**Time limit**: 10 minutes  
**Pass criteria**: Prompt requests all 4 outputs. AGENTS.md prevention rules are actionable (not just "be careful").

---

## Scoring Rubric

| Drill | Points | Score |
|-------|--------|-------|
| 1: Morning planning | /5 (ordered + file scope + verification all present) | |
| 2: Fix failing test | /5 (scope + no test modification + stopping conditions) | |
| 3: Security review | /5 (all 4 categories + format + verdict) | |
| 4: Full feature | /5 (all 3 phases + implementation has all required sections) | |
| 5: Async debug | /5 (bug identified + prompt structure correct + stopping condition) | |
| 6: AGENTS.md | /5 (all 6 sections + 5+ forbidden + no vague entries) | |
| 7: Post-incident | /5 (all 4 outputs requested + actionable prevention rules) | |

**Total: /35**

- 31-35: Pro. These workflows are automatic.
- 25-30: Advanced. Strong foundation, occasional gaps.
- 18-24: Intermediate. Good instincts, needs repetition.
- Below 18: Returning to concept files before more drills.

**Repetition target**: Run each drill until you score 5/5 three times in a row.
