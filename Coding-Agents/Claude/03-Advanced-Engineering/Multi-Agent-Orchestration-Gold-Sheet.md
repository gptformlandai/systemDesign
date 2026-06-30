# Multi-Agent Orchestration — Gold Sheet

> **Track**: Claude Mastery Track — Group 3: Advanced Engineering
> **File**: 7 of 7 (Track File #20)
<<<<<<< HEAD
> **Audience**: Developers designing and running production multi-agent pipelines
=======
>>>>>>> refs/remotes/origin/main
> **Read after**: Agent-Loops-Gold-Sheet.md

---

<<<<<<< HEAD
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
=======
## Practical Impact Meter

| Topic | Impact | Why Developers Miss This |
|-------|--------|--------------------------|
| Why single-agent sessions fail for large tasks | ★★★★★ | Context drift and instruction conflicts compound — multi-agent is the fix |
| Planner → Builder → Tester pipeline | ★★★★★ | The most common orchestration pattern; directly applicable to feature delivery |
| Context handoff between agents | ★★★★★ | Without structured handoff, each new agent starts blind |
| When to split vs when to stay in one session | ★★★★☆ | Not every task needs orchestration; knowing the trigger saves overhead |
| Failure recovery in pipelines | ★★★★☆ | Pipelines fail; knowing how to detect and recover is production-critical |

---

## 1. Why Multi-Agent Orchestration

```
Single-agent problem:
  - One long session for a complex feature = context drift
  - Early instructions lose influence after 50+ exchanges
  - Planning context and implementation detail compete for attention
  - Claude second-guesses decisions it made 40 messages ago

Multi-agent solution:
  - Each agent has a focused, clean context window
  - Context isolation prevents drift and contradiction
  - Each agent specializes in one role: planning, building, testing, reviewing
  - Context handoff documents capture outputs cleanly between agents

When to use multi-agent:
  ✓ Feature requires > 5 files
  ✓ Task has distinct phases (plan / build / test / review)
  ✓ A session is drifting (agent contradicts earlier decisions)
  ✓ You want specialist quality for each phase

When NOT to use multi-agent:
  ✗ Simple bug fix in 1-2 files (single session is faster)
  ✗ Quick explanation or code snippet
  ✗ Task where phases are heavily interleaved
>>>>>>> refs/remotes/origin/main
```

---

<<<<<<< HEAD
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
=======
## 2. The Core Pipeline: Planner → Builder → Tester → Reviewer

### Overview

```
Agent 1: @planner
  Input: ticket/requirements + codebase reference
  Output: implementation plan document (files, interfaces, order)
  Context: architecture docs + existing patterns

Agent 2: @builder
  Input: planner's output + relevant source files
  Output: implemented code
  Context: plan document + only the files being built

Agent 3: @tester
  Input: builder's output + test conventions
  Output: tests + verification run results
  Context: new code + testing conventions (not the whole history)

Agent 4: @reviewer
  Input: builder's code + tester's test results
  Output: review findings (security, quality, coverage)
  Context: final code + review standards
```

### Phase 1 — Planner Agent

```
Session start: New Claude Code session

Context to provide:
  - @file:CLAUDE.md (project rules)
  - @file:src/ (or reference the key service files)
  - The ticket or requirements

Prompt:
"You are @planner. Create an implementation plan for this feature.

Ticket: [paste ticket content]

Produce:
  1. Files to CREATE: name, purpose, key interfaces (function signatures)
  2. Files to MODIFY: what changes in each
  3. New dependencies: any new libraries needed (justify each)
  4. Implementation order: which file to build first and why
  5. Data model changes: any schema migrations needed (flag — do NOT run)
  6. Test plan: what test file for each new file, what cases to cover

Output the plan as a structured document I can save and share with the builder.
Do NOT implement anything. Plan only."

Save the output as: docs/implementation-plans/[feature]-plan.md
```

### Phase 2 — Builder Agent

```
Session start: New Claude Code session

Context to provide:
  - @file:docs/implementation-plans/[feature]-plan.md (planner output)
  - Only the files listed in the plan (not @codebase)

Prompt:
"You are @builder. Implement the plan in @file:docs/implementation-plans/[feature]-plan.md

Rules:
  - Follow the implementation order in the plan
  - After each file: run lint (ruff check [file]) and fix any issues
  - Do NOT create files not listed in the plan
  - Do NOT run database migrations
  - Do NOT modify test files (builder writes implementation only)
  - If you encounter an ambiguity: describe it and ask before proceeding

Begin with file 1. Report status after each file."
```

### Phase 3 — Tester Agent

```
Session start: New Claude Code session

Context to provide:
  - @file:docs/implementation-plans/[feature]-plan.md (test plan section)
  - @file:src/ [the new files the builder created]
  - @file:tests/conftest.py (existing fixtures and patterns)

Prompt:
"You are @tester. Generate and run tests for the feature described in
@file:docs/implementation-plans/[feature]-plan.md

Process:
  1. Generate tests per the test plan in the plan document
  2. Run: pytest [test_file] -x after each test file
  3. If tests fail: fix the TEST only if the test setup is wrong;
     fix the IMPLEMENTATION if the implementation is wrong
  4. Run full suite: pytest tests/ -v when all new tests pass
  5. Report: test count, pass/fail, and any coverage gaps remaining

Constraint: Do NOT modify the builder's implementation to make tests pass
unless there is a genuine bug (explain before modifying)."
```

### Phase 4 — Reviewer Agent

```
Session start: New Claude Code session

Context to provide:
  - All new and modified files from the builder
  - Test results from the tester

Prompt:
"You are @reviewer. Review this feature implementation.

Files to review: [list @file references]
Test results: [paste pytest summary]

Review for:
  1. Security: OWASP top 10, no hardcoded creds, no SQL injection, proper auth
  2. Test coverage: error paths tested, edge cases, no tautological tests
  3. Error handling: all exceptions caught and handled appropriately
  4. API contracts: does this break existing callers?
  5. Technical debt: anything that should be noted for future cleanup

Format: severity table (CRITICAL / HIGH / MEDIUM / LOW) for each finding.
End with: APPROVED / APPROVED WITH COMMENTS / CHANGES REQUIRED"
>>>>>>> refs/remotes/origin/main
```

---

<<<<<<< HEAD
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
=======
## 3. Context Handoff Documents

### Why Handoff Documents Are Critical

```
Without a handoff document:
  - Each new agent starts with partial context
  - The builder doesn't know the architectural decisions the planner made
  - The tester doesn't know what the builder changed or why
  - Context is reconstructed by reading files — slow and lossy

With a handoff document:
  - Each agent receives the decisions and reasoning from the previous phase
  - No context is lost between sessions
  - The pipeline can resume after interruption
  - The document becomes a permanent record of the feature's development
```

### Handoff Document Template

```markdown
# Feature: [Name] — Implementation Handoff

## Status: [Planning Complete / Building Complete / Testing Complete]

## Plan Summary
[Copy the planner's output here]

## Builder Notes (filled by @builder)
Files created:
  - [file]: [what it does, key design choices]
Files modified:
  - [file]: [what changed and why]
Known limitations / deferred work:
  - [anything the builder flagged]

## Tester Notes (filled by @tester)
Tests created: [list]
Test results: X passed, Y failed
Coverage gaps remaining: [list]

## Reviewer Notes (filled by @reviewer)
Findings: [severity table]
Decision: APPROVED / APPROVED WITH COMMENTS / CHANGES REQUIRED
>>>>>>> refs/remotes/origin/main
```

---

<<<<<<< HEAD
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
=======
## 4. Failure Recovery

```
Common failures and recovery:

Failure 1 — Builder deviates from plan:
  Detection: builder creates files not in the plan, or modifies forbidden files
  Fix: git checkout [unexpected files]
  Recovery: "Only implement what's in the plan. What you created was not planned.
  Please remove [file] and implement [planned file] instead."

Failure 2 — Tester breaks the implementation to pass tests:
  Detection: tests pass but git diff shows changes to src/ files
  Fix: git checkout src/ — restore implementation
  Recovery: start new tester session with explicit "Do NOT modify src/ files"

Failure 3 — Context drift mid-build:
  Detection: builder uses inconsistent naming or violates a constraint stated earlier
  Fix: stop the session
  Recovery: new session, paste the plan document and state violated constraints explicitly

Failure 4 — Pipeline stalls (agent asks for input repeatedly):
  Detection: agent keeps asking for clarification instead of building
  Fix: answer the questions, then use the planner to add clarity to the plan document
  Recovery: restart the builder with the updated plan

Failure 5 — Reviewer finds CRITICAL issues:
  Detection: CRITICAL finding in reviewer's output
  Fix: do NOT merge. Return to builder with the reviewer's findings.
  Recovery: builder session with @file:review-findings.md as input
>>>>>>> refs/remotes/origin/main
```

---

<<<<<<< HEAD
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
=======
## 5. Lightweight Orchestration — 2-Agent Pipeline

For smaller features, use a simpler setup:

```
Agent 1: @planner → produces a plan + test spec
Agent 2: @builder-tester → implements AND runs tests in one session

Trigger this when:
  - Feature touches 3-5 files (not 10+)
  - You want faster turnaround than the full 4-agent pipeline
  - The planning phase is short (30 min or less)

Still use handoff: even for 2 agents, save the plan before starting the builder.
>>>>>>> refs/remotes/origin/main
```

---

<<<<<<< HEAD
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
=======
## 6. Revision Checklist

- [ ] Understands why context isolation is the core value of multi-agent
- [ ] Knows the 4-phase pipeline: Planner → Builder → Tester → Reviewer
- [ ] Can write a complete planner prompt with all required output sections
- [ ] Always creates a handoff document between phases
- [ ] Knows the 5 failure modes and the recovery for each
- [ ] Can identify when to use 2-agent vs 4-agent pipeline
- [ ] Has agent definition files for @planner, @builder, @tester, @reviewer
>>>>>>> refs/remotes/origin/main
