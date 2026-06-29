# SDLC Automation with Copilot — Gold Sheet

> **Track**: Copilot Mastery Track — Group 4: Pro / Production Level
> **File**: 2 of 7 (Track File #22)

---

## Overview

This sheet maps every phase of the software development lifecycle to the optimal Copilot strategy.

---

## Phase 1 — Requirements and Planning

```
Mode: Chat Ask
Agent: @productivity-assistant or @system-design-mentor

Prompt:
"I have this requirement: [paste ticket/requirement]

Help me:
1. Identify ambiguities or missing details
2. Break into implementation tasks (ordered by dependency)
3. Identify which existing code in #codebase will be affected
4. Estimate complexity (S/M/L) for each task
5. List technical risks"
```

---

## Phase 2 — Architecture and Design

```
Mode: Chat Ask
Agent: @architecture-advisor

Prompts:
"Design the component structure for [feature].
Show: components, their responsibilities, interfaces, data flow.
Follow the pattern in #codebase (layered: router → service → repository)."

"Generate an ADR for this architectural decision: [decision]
Context, options, decision, consequences."
```

---

## Phase 3 — API Design

```
Mode: Chat Ask
Agent: @api-designer

Prompt:
"Design the REST API for [feature].
Resources: [list]
Operations: [list]
Constraints: follow our existing patterns in #file:src/api/users.py
Output: OpenAPI-style resource definitions with example request/response"
```

---

## Phase 4 — Test-First Generation

```
Mode: Chat Ask or Edits
Agent: @test-engineer

Prompt:
"I'm implementing [feature].
Generate failing tests BEFORE implementation.
Test file: tests/unit/test_[feature]_service.py
Cover: happy path, 3 error cases, 2 edge cases
Mock: database session, external HTTP"
```

---

## Phase 5 — Implementation

```
Mode: Edits (targeted) or Agent Mode (scaffold)
Strategy: Plan first, implement second

For targeted changes:
"Implement #sym:UserService.create_user to pass the failing tests.
Tests: #file:tests/unit/test_user_service.py
Do not change the test file. Implement until all tests pass."

For new feature scaffold:
Use Agent Mode with: Context, Goal, Requirements, Constraints, Plan First
```

---

## Phase 6 — Test Coverage and Quality

```
Mode: Chat Ask
Agent: @test-engineer

"Analyze #file:src/services/[feature]_service.py vs 
#file:tests/unit/test_[feature]_service.py.
Report: untested functions, untested error paths, missing edge cases.
Priority: HIGH / MEDIUM / LOW."
```

---

## Phase 7 — Code Review Self-Pass

```
Mode: Chat Ask
Prompt: Run the pre-PR self-review prompt (from Copilot-For-PR-Review-Gold-Sheet.md)

Specifically run:
1. Security review prompt
2. Test gap analysis
3. Architecture review (if significant new code)
```

---

## Phase 8 — Documentation

```
Mode: Chat Ask or prompt file
Agent: @documentation-writer

"Generate the following for the [feature] implementation:
1. API endpoint documentation (request, response, errors, auth requirement)
2. Service layer docstrings for all public methods
3. README section describing this feature
4. One ADR for any significant design decision made"
```

---

## Phase 9 — PR Creation

```
Mode: Prompt file
Prompt: /write-pr-description

Include in PR:
- Summary of changes
- Why the change was made
- How to test
- Checklist: tests, security review, docs updated
```

---

## Phase 10 — CI/CD

```
Mode: Prompt file or Edits
Prompt: /create-github-action (if new workflow needed)

After PR merges:
- Monitor CI run
- If it fails: /debug-error with #terminalLastCommand
```

---

## Phase 11 — Incident Debugging

```
Mode: Chat Ask
Constraints: NEVER paste real production data

Prompt:
"An incident is occurring. Symptoms: [describe observable behavior]
Relevant error from logs (sanitized): [paste anonymized error]
Relevant code: #file:src/services/[suspected service]

Diagnose: most likely root causes ranked.
Investigation steps: what to check next."
```

---

## Phase 12 — Learning and Notes

```
End of feature cycle:
"Generate structured learning notes for:
1. The main technical challenge I solved in this feature
2. The Copilot workflow that worked best
3. Any Copilot failure I encountered and how I fixed it

Format: markdown with headers, code examples, revision questions."
```

---

## Revision Checklist

- [ ] Can apply the correct Copilot mode for each of the 12 SDLC phases
- [ ] Uses test-first workflow (tests before implementation)
- [ ] Runs the security review on all new auth/SQL/input-handling code
- [ ] Generates PR descriptions using the structured prompt
- [ ] Captures learning notes at the end of every feature cycle
- [ ] Never pastes real production data during incident debugging
