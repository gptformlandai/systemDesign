<<<<<<< HEAD
# SDLC Automation — Gold Sheet
=======
# SDLC Automation with Claude — Gold Sheet
>>>>>>> refs/remotes/origin/main

> **Track**: Claude Mastery Track — Group 4: Pro / Production Level
> **File**: 2 of 5 (Track File #22)
> **Read after**: Personal-Claude-Operating-System-Gold-Sheet.md

---

<<<<<<< HEAD
## The 12-Phase Claude SDLC Map

| Phase | Claude Surface | Command/Agent | What Claude Does |
|-------|---------------|---------------|-----------------|
| 1. Requirements | Chat + /plan | @planner | Clarify, decompose, identify unknowns |
| 2. Design | Claude Code + @architect | /plan | Architecture, interfaces, trade-offs |
| 3. API Contract | Claude Code | /plan | Define request/response schemas first |
| 4. Test Design | @tester | /test | Write failing tests before implementation |
| 5. Implementation | Claude Code + @builder | /build | Implement to pass the failing tests |
| 6. Unit Testing | Verification loop | /test | All tests pass, no test modification |
| 7. Code Review | @reviewer | /review | Security + correctness + coverage |
| 8. Documentation | Claude Code + skill | /docs | README, docstrings, API docs |
| 9. Integration Testing | Verification loop | /test | Full integration suite green |
| 10. PR Preparation | Claude Code | /review | Pre-PR security scan + PR description |
| 11. CI/CD | Claude Code | /build | GitHub Actions workflow generation |
| 12. Release | Claude Code | @planner | Release notes from commits |

---

## Phase 1 — Requirements

```
"Clarify these requirements. Ask me questions for each ambiguity.
Requirement: [paste]

Ask about:
1. Edge cases not covered
2. Error handling behavior
3. Performance requirements
4. Security requirements
5. Any assumptions in the spec

After I answer, decompose into implementation tasks ordered by dependency."
```

## Phase 2 — Design

```
"Use @architect agent.
Design the implementation for: [feature].
Constraints: @file:CLAUDE.md (architecture rules)
Existing pattern: @file:src/[similar existing feature]
Output: interface definitions (not implementations) + component diagram (text)"
```

## Phase 3 — API Contract First

```
"Define the API contract BEFORE implementation.
Feature: [feature]

Define:
1. Request schema (with validation rules and examples)
2. Response schema (success cases)
3. Error responses (status codes, error formats)
4. Auth requirement

Output: Pydantic/Zod/TypeScript schemas. Implementation comes AFTER this is approved."
```

## Phase 4 — Test Design (Test-First)

```
"Generate failing tests for: [feature]
Based on: the API contract above
Do NOT implement yet. Every test should FAIL (red state).
Tests must cover: spec requirements, error paths, edge cases."
```

## Phase 5 — Implementation

```
"Use @builder agent.
Implement [feature] to pass the failing tests.
Tests: @file:tests/[test file]
Pattern: @file:src/[similar implementation]
Verification loop: after each function, run relevant tests."
```

## Phases 6-9 (Verification Loop)

```
All verification phases use the same loop pattern:
  Claude runs [test command] → reads output → fixes failures → repeats
  Human reviews only the final result, not each iteration.

The loop is the key — Claude does the feedback loop, not you.
```

## Phase 10 — PR Preparation

```
"Prepare this feature for PR:
1. /review all changed files — fix any CRITICAL/HIGH findings
2. Generate PR description:
   ## Summary / ## Changes Made / ## How to Test / ## Breaking Changes
3. Verify: no secrets in diff, all tests pass, lint clean"
```

## Phase 11 — GitHub Actions

```
"/build a GitHub Actions CI workflow for this project.
Stack: [from CLAUDE.md]
Trigger: push to main, pull_request
Jobs: lint → typecheck → unit tests → integration tests (if DB available)
Requirements: pin all versions, concurrency group, cache dependencies"
```

## Phase 12 — Release Notes

```
"Generate release notes for v[X.Y.Z].
Commits since last tag:
$(git log v[previous]..HEAD --oneline)

Format:
## [version] — [date]
### Features (user-visible new capabilities)
### Fixes (bugs resolved)
### Breaking Changes (with migration steps)
### Internal (dependency updates, refactoring)"
=======
## Overview

This sheet maps every SDLC phase to the optimal Claude workflow.
For each phase: the mode, the agent, the prompt pattern, and what success looks like.

---

## Phase 1 — Requirements Clarification

```
Mode: Claude Chat (Projects) or CLI Chat Ask
Agent: @planner

Prompt:
"I have this requirement: [paste ticket or requirement]

Help me:
1. Identify ambiguities or missing details (what needs clarification before I code)
2. Break into implementation tasks ordered by dependency
3. Which existing code in @codebase will be affected?
4. Estimate complexity: S (< 2h) / M (half day) / L (> 1 day) per task
5. Technical risks: what could go wrong?

Plan only — no code."
```

---

## Phase 2 — Architecture and Design

```
Mode: CLI Chat Ask or Claude Chat Projects
Agent: @architect

Prompts:
"Design the component structure for [feature].
Using @codebase to understand our existing patterns:
  - Components: what they are and their responsibilities
  - Interfaces: how they communicate
  - Data flow: request path through the system
  - Follow the pattern: @file:src/services/[example].py

Output: component diagram (text), interface definitions, implementation order."

"Generate an ADR for this architectural decision:
  Decision: [describe]
  Context: [what forced this decision]
  Alternatives considered: [list]

Format: Context / Decision / Alternatives / Consequences"
```

---

## Phase 3 — API Design

```
Mode: CLI or Chat Ask
Agent: @architect

Prompt:
"Design the REST API for [feature].

Resources: [list your domain entities]
Operations: [list the user-facing actions]
Constraints:
  - Follow our existing API patterns in @file:src/api/[example].py
  - Authentication: [your auth mechanism]
  - Error format: [your error response format]

Output: resource definitions with method, path, request schema, response schema,
status codes (success + all error cases), and one curl example per endpoint.

Do not implement — design only."
```

---

## Phase 4 — Test-First Generation

```
Mode: CLI Slash Command
Agent: @tester

Prompt (write tests BEFORE implementation):
"I'm implementing [feature].

Generate failing tests BEFORE implementation.
Test file: tests/unit/test_[module].py
Cover: happy path per requirement, 3 error cases, 2 boundary values.
Mock: @file:tests/conftest.py patterns for external dependencies.

Do not write any implementation — tests only.
The implementation file does not exist yet."
```

---

## Phase 5 — Implementation

```
Mode: CLI Agent Mode (multi-file) or CLI Edits (targeted)
Agent: @builder

For new feature scaffold:
"Implement the plan in @file:docs/plans/[feature]-plan.md

Process:
  1. Create each file in the order listed
  2. After each file: run lint check
  3. After all files: run pytest -x on the new test suite
  4. Fix all lint and test failures before reporting done

Constraints:
  - Do NOT modify test files
  - Do NOT run database migrations
  - Do NOT modify CLAUDE.md or hook files"

For targeted changes:
"Implement [specific function] in @file:src/services/[service].py
to pass the tests in @file:tests/unit/test_[service].py.
Do NOT modify the test file. Do NOT change other functions."
```

---

## Phase 6 — Test Coverage and Quality

```
Mode: CLI or Chat
Agent: @tester

"Analyze test coverage:
Implementation: @file:src/services/[service].py
Tests: @file:tests/unit/test_[service].py

Report:
  - Functions with no tests (HIGH)
  - Error paths not tested (HIGH)
  - Edge cases missing (MEDIUM)
  - Tests that test implementation not behavior (flag and fix)

Generate tests for all HIGH gaps."
```

---

## Phase 7 — Code Review Self-Pass

```
Mode: CLI Slash Command
Command: /review  (or run manually)

Run in order:
  1. /security — OWASP-focused review on auth/SQL/input handling
  2. /review — full pre-commit review
  3. Test gap check — any untested paths in the new code?

Only proceed to PR after all CRITICAL and HIGH issues are resolved.
```

---

## Phase 8 — Documentation

```
Mode: CLI or Chat
Agent: @documentation (or @builder)

"Generate documentation for the [feature] implementation:

1. API endpoint docs: method, path, request/response schema, error codes, auth requirement
2. Service layer docstrings for all public methods (@file:src/services/[service].py)
3. README update: add a section describing this feature
4. ADR: if a significant design decision was made during implementation

Use only what's in the actual code — do not fabricate details."
```

---

## Phase 9 — PR Creation

```
Mode: CLI or Chat
Command: /build  (or manual prompt)

"Generate a GitHub PR description for these changes:

Changed files: [list @file references]
What I changed: [paste your notes or git log --oneline]

Format:
## Summary
## Changes Made
## How to Test
## Breaking Changes (if any)
## Checklist
  - [ ] Tests pass
  - [ ] Security review done
  - [ ] Documentation updated
  - [ ] No hardcoded values

Keep under 300 words. Factual, no marketing language."
>>>>>>> refs/remotes/origin/main
```

---

<<<<<<< HEAD
## Revision Checklist

- [ ] Has used Claude at every one of the 12 SDLC phases at least once
- [ ] Uses test-first (Phase 4 before Phase 5) for all critical features
- [ ] Verification loop runs autonomously (Claude runs tests, not you manually)
- [ ] @reviewer runs as separate agent (fresh context, not same session as builder)
- [ ] PR preparation includes /review before submitting
=======
## Phase 10 — CI/CD

```
Mode: CLI or Chat
Use Case: generating or debugging GitHub Actions

Generate CI workflow:
"Generate a GitHub Actions CI workflow for [language/framework].
Requirements: [your specific steps]
Pin all action versions. Add concurrency group. No hardcoded secrets."

Debug CI failure:
"This GitHub Actions step is failing.
Workflow section: [paste]
Error: [paste exact error]
Expected behavior: [describe]
Diagnose: root cause and fix."
```

---

## Phase 11 — Incident Debugging

```
Mode: CLI or Chat (NEVER paste real production data)

Prompt:
"An incident is occurring. Symptoms: [describe observable behavior]
Relevant error (SANITIZED — no real user data): [paste anonymized error]
Relevant code: @file:src/services/[suspected service]

Diagnose:
1. Most likely root causes (ranked by probability)
2. For each: what to check in logs/code to confirm
3. Immediate mitigation: what to deploy NOW to stop the bleeding
4. Proper fix: what to change for long-term correctness
5. Regression test: what test would have caught this"
```

---

## Phase 12 — Post-Incident / Learning

```
Mode: Claude Chat Projects (persistent)

"Generate a post-incident learning document:

Incident: [brief description, no PII]
What broke: [technical cause]
What we changed: [immediate fix]

Produce:
  1. Timeline (brief)
  2. Root cause analysis (5 Whys)
  3. What test/check would have caught this
  4. Action items with owners
  5. What to add to CLAUDE.md to prevent this class of issue"
```

---

## SDLC Quick Reference

| Phase | Mode | Agent | Time |
|-------|------|-------|------|
| Requirements | Chat / CLI | @planner | 10 min |
| Architecture | Chat / CLI | @architect | 15-30 min |
| API Design | CLI | @architect | 15 min |
| Test-First | CLI / /test | @tester | 20 min |
| Implementation | CLI Agent | @builder | varies |
| Test Coverage | CLI | @tester | 10 min |
| Self Review | CLI /review | @reviewer | 15 min |
| Documentation | CLI | @builder | 10 min |
| PR Description | CLI | @builder | 5 min |
| CI/CD | CLI | @builder | 10 min |
| Incident Debug | CLI | @debugger | varies |
| Learning Capture | Chat | /generate-notes | 10 min |

---

## Revision Checklist

- [ ] Can apply the correct Claude mode and agent for each of the 12 SDLC phases
- [ ] Uses test-first workflow (tests before implementation)
- [ ] Runs security review on all new auth/SQL/input handling code
- [ ] Generates PR descriptions using the structured template
- [ ] Captures learning notes at end of every feature cycle
- [ ] Never pastes real production data during incident debugging
>>>>>>> refs/remotes/origin/main
