# SDLC Automation — Gold Sheet

> **Track**: Claude Mastery Track — Group 4: Pro / Production Level
> **File**: 2 of 5 (Track File #22)
> **Read after**: Personal-Claude-Operating-System-Gold-Sheet.md

---

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
```

---

## Revision Checklist

- [ ] Has used Claude at every one of the 12 SDLC phases at least once
- [ ] Uses test-first (Phase 4 before Phase 5) for all critical features
- [ ] Verification loop runs autonomously (Claude runs tests, not you manually)
- [ ] @reviewer runs as separate agent (fresh context, not same session as builder)
- [ ] PR preparation includes /review before submitting
