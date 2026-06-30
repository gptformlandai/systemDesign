# Multi-Agent Orchestration — Gold Sheet

> **Track**: Claude Mastery Track — Group 3: Advanced Engineering
> **File**: 7 of 7 (Track File #20)
> **Audience**: Developers designing and running production multi-agent pipelines
> **Read after**: Agent-Loops-Gold-Sheet.md

---

## 1. Practical Impact Meter

| Topic | Impact | Why Developers Miss This |
|---|---|---|
| Why isolation > one-session-does-everything | ★★★★★ | Devs run everything in one context — quality degrades after 30+ messages |
| The canonical 4-agent pipeline | ★★★★★ | Planner→Builder→Tester→Reviewer covers 90% of feature development |
| Handoff document design — what to pass and what NOT to pass | ★★★★★ | Passing too much context between agents defeats isolation |
| Failure recovery across agent boundaries | ★★★★★ | When Builder fails, Debugger needs only the failure, not the entire build history |
| Parallel agent execution patterns | ★★★★☆ | Tester and Reviewer can often run in parallel, not sequentially |
| Agent coordination without shared state | ★★★★☆ | Agents communicate only through structured handoff documents, never through shared context |

---

## 2. The Core Architecture Principle

### Why Isolation is the Foundation

```
One-session-everything problem (what most developers do):
  Session 1 (3 hours): Plan → Design → Implement → Test → Review → Fix → Re-review
  
  What happens:
    Hour 1: High quality. Claude follows CLAUDE.md, uses correct patterns.
    Hour 2: Context drift starts. Claude forgets early decisions.
    Hour 3: Context is saturated. Output is generic, constraints ignored.
    
  Root cause: mixing planning context + implementation context + testing context
  into one shared session. Each domain corrupts the others.

Multi-agent pipeline (the correct approach):
  Agent 1 (Planner): pure design context → outputs a plan document
  Agent 2 (Builder): plan + pattern files → outputs implementation + file list
  Agent 3 (Tester): fresh read of implementation → outputs tests + gap analysis
  Agent 4 (Reviewer): fresh read of impl + tests → outputs severity-ranked findings
  
  What happens:
    Each session: 20-40 messages, focused context, no drift
    Each agent: reads implementation fresh, without author bias
    Total: higher quality, auditable at each stage, easily reversible
```

---

## 3. The Canonical 4-Agent Pipeline

### Pipeline Overview

```
@planner ──── plan.md ──→ @builder ──── impl + file_list ──→ @tester ──── tests + gaps ──→ @reviewer
                                                                                               │
                              ←── fix requests ──────────────────────────────────────────────┘
```

### Stage 1: @planner

```
Input: business requirements, existing codebase patterns
Output: plan.md (saved to disk)

Invoke:
"Use the @planner agent.
Task: implement user notification preferences (email, in-app, SMS toggles)
Codebase pattern: @file:src/api/users.py (router), @file:src/services/user_service.py (service)
Architecture: layered — router → service → repository → DB
Output: plan.md with files to create, one-sentence purpose each, build order, assumptions"

plan.md example output:
## Feature: Notification Preferences
## Files to Create (in order):
  1. src/schemas/notification.py — Pydantic schemas for request/response
  2. src/repositories/notification_repo.py — DB access for preferences
  3. src/services/notification_service.py — business logic
  4. src/api/notifications.py — FastAPI router
  5. tests/unit/test_notification_service.py — unit tests
## Assumptions:
  - One preference record per user (created on first access)
  - At least one channel must remain enabled
## Build dependencies:
  schemas → repo → service → api → tests
```

### Stage 2: @builder

```
Input: plan.md + pattern files
Context: ONLY what's needed to implement — no planning discussion

Invoke:
"Use the @builder agent.
Plan: @file:plan.md
Patterns:
  Router: @file:src/api/users.py
  Service: @file:src/services/user_service.py
  Repository: @file:src/repositories/user_repo.py
Build order: schemas → repo → service → api
After each file: run ruff check [file] and fix lint errors.
Output: file list of what was created"

File list (builder output):
## Files Created:
  - src/schemas/notification.py (45 lines)
  - src/repositories/notification_repo.py (38 lines)
  - src/services/notification_service.py (62 lines)
  - src/api/notifications.py (41 lines)
## Test results: pending (hand off to @tester)
## Lint: clean on all files
## Blockers encountered: none
```

### Stage 3: @tester

```
Input: file list from builder (fresh read — no build context)
Context: ONLY the implementation files to test

CRITICAL: @tester reads implementation as a STRANGER.
No "I know the author meant X" — only what the code actually does.

Invoke:
"Use the @tester agent.
Implementation files:
  @file:src/services/notification_service.py
  @file:src/repositories/notification_repo.py
Framework: pytest + pytest-asyncio
Mock: AsyncMock(spec=AsyncSession) for DB, AsyncMock for any external services
Cover: happy path, error cases, at least 2 edge cases per public method
Output: complete test file + gap analysis + test run results"

Tester output:
## Tests Generated: tests/unit/test_notification_service.py
## Test count: 14 tests
## Run results: 13 passed, 1 failed
## Failure: test_get_preferences_user_not_found — expects 404, service returns None
## Gap analysis:
  - concurrent_update scenario not tested
  - disable_all_channels edge case only partially covered
## Potential bug: get_preferences() returns None instead of raising NotFoundError
   (flagging for reviewer — not fixing implementation)
```

### Stage 4: @reviewer

```
Input: implementation files + test files (fresh read — no implementation context)
Context: code only — not who wrote it or why

CRITICAL: @reviewer sees the code with no knowledge of the author's intent.
This is what makes the review valuable.

Invoke:
"Use the @reviewer agent.
Review:
  @file:src/services/notification_service.py
  @file:src/api/notifications.py
  @file:tests/unit/test_notification_service.py
Focus: security + correctness + test coverage
The @tester flagged: get_preferences() may return None instead of raising NotFoundError
Output: severity-ranked findings + APPROVE / APPROVE WITH CHANGES / REQUEST CHANGES"

Reviewer output:
| SEVERITY | File | Line | Issue | Fix |
|----------|------|------|-------|-----|
| HIGH | notification_service.py | 45 | get_preferences() returns None, caller crashes | Raise NotFoundError |
| MEDIUM | notifications.py | 23 | Missing auth check on GET endpoint | Add Depends(get_current_user) |
| LOW | test_notification_service.py | — | No concurrent update test | Add @pytest.mark.parametrize |
Decision: REQUEST CHANGES (1 HIGH finding must be fixed before merge)
```

---

## 4. Handoff Document Design

### What to Include in Handoffs

```
ALWAYS include:
  - Files created (exact paths)
  - Test results (pass/fail counts)
  - Active constraints (rules that must continue to be followed)
  - Blocking issues (things the next agent must know about)

NEVER include:
  - The full conversation from the previous session (defeats isolation)
  - Your reasoning for design decisions (let the next agent reason fresh)
  - "I thought about X but chose Y" explanations
  - Debugging attempts that were abandoned

The handoff document is a STATE SNAPSHOT, not a conversation log.
```

### Handoff Template

```markdown
## Handoff: [Stage N] → [Stage N+1]
Date: [timestamp]
Feature: [name]

## State
What exists now:
  - [file]: [what it does — one sentence]
  - [file]: [what it does — one sentence]

Test results (if applicable):
  - X passed, Y failed
  - Failed tests: [test name] — [why it fails]

## Active Constraints (must be carried forward)
  - [constraint 1]
  - [constraint 2]

## Flags for Next Agent
  - [issue or question the next agent must know about]
  
## Next Agent Task
  [Specific task for the receiving agent — one paragraph]
```

---

## 5. Failure Recovery Across Agent Boundaries

### When @builder Fails

```
Scenario: Builder creates 4/5 files, then hits an architectural problem
(e.g., the database schema doesn't support the required query).

Recovery:
  1. Commit what's done: git add . && git commit -m "WIP: 4/5 files from builder"
  2. Create a targeted handoff for @debugger or @architect:
     "Architectural blocker from @builder session:
     Built: [list of 4 files]
     Problem: [specific issue]
     Need: [architectural decision or schema change]"
  3. Resolve the blocker with @architect
  4. Resume @builder with the updated plan

DON'T: Restart @builder from scratch (loses the 4 good files)
DON'T: Continue in the same session (mixing concern contexts)
```

### When @tester Finds Bugs

```
Scenario: @tester discovers 3 bugs during testing.

Options:
  A. Small bugs (< 5 lines each): @tester can fix them without handoff
     "Fix the implementation (not the tests) for these 3 failures."
  
  B. Architectural bugs (wrong return type, missing error propagation):
     Hand off back to @builder:
     "Bug report from @tester:
     Implementation files: [list]
     Bug 1: [description + test that fails]
     Bug 2: [description + test that fails]
     Fix the implementation. Do NOT change the tests."
  
  C. Design bugs (feature works differently than spec):
     Hand off to @planner + human review:
     "Design mismatch found by @tester:
     Spec says: [X]
     Implementation does: [Y]
     Human decision needed before continuing."
```

---

## 6. Parallel Agent Execution

### What Can Run in Parallel

```
Sequential is required:
  @planner → @builder (builder needs the plan)
  @builder → @tester (tester needs the implementation)
  @tester → @reviewer (reviewer needs both code and tests)

CAN run in parallel:
  @tester and @reviewer on SEPARATE files from the same feature
  @builder (building feature A) and @tester (testing feature B)
  @optimizer (analyzing module X) and @reviewer (reviewing module Y)
  @documentation (writing docs for feature A) while @builder (building feature B)

Parallel execution with separate Claude Code sessions:
  Terminal 1: claude "Use @tester agent on payment_service.py"
  Terminal 2: claude "Use @reviewer agent on order_service.py"
  Both run simultaneously in separate isolated contexts
```

---

## 7. Multi-Agent Anti-Patterns

```
Anti-pattern 1 — Context leakage:
  "Tell @tester that the complex caching logic in notification_service.py
  exists for performance reasons."
  → This gives the tester author bias. Tester must evaluate the code without knowing WHY.
  Fix: just pass the file. Let the tester read the code independently.

Anti-pattern 2 — Circular handoffs:
  Builder → Tester → Builder → Tester → Builder (indefinite loop)
  Fix: limit to 2 repair cycles. If tests still fail after 2: escalate to human.

Anti-pattern 3 — Mega handoffs:
  Handing 3,000 lines of conversation history to the next agent.
  Fix: write a structured handoff document (< 300 words). Discard the conversation.

Anti-pattern 4 — Single agent for everything:
  "Just use one session — it's simpler."
  Fix: use separate sessions for any task > 2 hours or 3+ file types.
  The quality difference becomes obvious by hour 3.

Anti-pattern 5 — No commit between stages:
  Builder finishes → no commit → Tester makes changes → no commit → bugs everywhere.
  Fix: commit after every agent stage. Each stage's output is a checkpoint.
```

---

## 8. Revision Checklist

- [ ] Understands WHY isolation produces better output than one long session
- [ ] Can run the full 4-agent pipeline (@planner → @builder → @tester → @reviewer)
- [ ] Writes handoff documents with state, active constraints, and flags — not conversation dumps
- [ ] Commits after every agent stage (each stage is a git checkpoint)
- [ ] Knows the failure recovery pattern for: builder fails, tester finds bugs, design mismatch
- [ ] Runs agents in parallel when tasks are independent
- [ ] Avoids the 5 multi-agent anti-patterns
- [ ] Has all 7 agent files in config/.claude/agents/ (planner, builder, debugger, tester, reviewer, architect, optimizer)
