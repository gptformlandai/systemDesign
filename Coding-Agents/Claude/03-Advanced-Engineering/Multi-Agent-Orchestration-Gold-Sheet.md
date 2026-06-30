# Multi-Agent Orchestration — Gold Sheet

> **Track**: Claude Mastery Track — Group 3: Advanced Engineering
> **File**: 7 of 7 (Track File #20)
> **Read after**: Agent-Loops-Gold-Sheet.md

---

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
```

---

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
```

---

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
```

---

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
```

---

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
```

---

## 6. Revision Checklist

- [ ] Understands why context isolation is the core value of multi-agent
- [ ] Knows the 4-phase pipeline: Planner → Builder → Tester → Reviewer
- [ ] Can write a complete planner prompt with all required output sections
- [ ] Always creates a handoff document between phases
- [ ] Knows the 5 failure modes and the recovery for each
- [ ] Can identify when to use 2-agent vs 4-agent pipeline
- [ ] Has agent definition files for @planner, @builder, @tester, @reviewer
