# Copilot Production Capstone - Cloud Agent And Enterprise Workflow

> **Track**: Copilot Mastery Track - Group 6: Practice Upgrade
> **File**: 38
> **Audience**: Learners finishing the track who want a realistic pro-level demonstration

---

## Capstone Goal

Demonstrate that you can use Copilot like a production engineer:

- frame a task
- select the right surface
- configure repo context
- use agentic workflows safely
- review output
- measure quality
- explain governance decisions

This capstone is intentionally practical. The final artifact is not "a prompt." It is a reviewable engineering workflow.

---

## Scenario

You own a small service with:

- one backend API
- one frontend or CLI client
- tests
- GitHub Actions CI
- documentation
- a few open issues

Your task:

> Add request validation to one API path, update tests, update docs, and prepare a PR using Copilot surfaces safely.

If you do not have a real repo, create a tiny practice repo and simulate the same flow.

---

## Required Artifacts

```md
[ ] `.github/copilot-instructions.md`
[ ] at least one `.github/instructions/*.instructions.md`
[ ] at least one `.github/agents/*.agent.md`
[ ] at least one `.github/prompts/*.prompt.md`
[ ] MCP example config with no secrets
[ ] hook example for secret/path protection
[ ] issue template used to assign the task
[ ] implementation branch or simulated diff
[ ] tests added/updated
[ ] PR description
[ ] review checklist results
[ ] capstone reflection
```

---

## Phase 1 - Prepare The Repo

Create or verify:

```md
.github/copilot-instructions.md
.github/instructions/testing.instructions.md
.github/instructions/security.instructions.md
.github/agents/test-engineer.agent.md
.github/prompts/security-review.prompt.md
.github/hooks/pre-tool-use-security-scan.example.json
config/mcp.example.json
```

Minimum instruction content:

```md
# Copilot Instructions

- Follow existing project style before introducing new patterns.
- Keep public APIs backward compatible unless the issue explicitly requests a breaking change.
- Add or update tests for behavior changes.
- Do not add dependencies unless explicitly approved.
- Never use real secrets or customer data in examples, tests, logs, or prompts.
- Before final output, list changed files, tests run, and assumptions.
```

---

## Phase 2 - Write The Issue

Use this issue:

```md
## Goal
Add request validation to [endpoint/function].

## Behavior
- Reject missing [field] with HTTP 400 or equivalent error.
- Reject invalid [field] format.
- Preserve current success behavior for valid requests.

## Scope
Allowed:
- [source path]
- [test path]
- [docs path]

Not allowed:
- Do not change auth behavior.
- Do not add dependencies.
- Do not change response schema except documented validation errors.

## Validation
- Add tests for missing, invalid, and valid input.
- Run [test command].

## Output
- Summary of changed files.
- Tests run.
- Assumptions.
```

---

## Phase 3 - Choose The Surface

Run this decision:

| Situation | Use |
|---|---|
| Real repo, background PR wanted | cloud agent |
| Local practice repo, hands-on | IDE Agent Mode or Edits |
| Terminal-heavy repo | Copilot CLI |
| Need GitHub issue/PR context | GitHub.com or GitHub MCP |

Write one sentence explaining your choice.

Example:

```md
I used IDE Agent Mode because this is a local practice repo and I want immediate diff review.
```

---

## Phase 4 - Plan First

Prompt:

```md
Plan only. Do not edit files yet.

Task:
[paste issue]

Inspect the repository and produce:
1. Files to inspect
2. Files likely to change
3. Test cases to add
4. Risks
5. Any clarification needed
```

Score the plan:

```md
[ ] Scope is limited
[ ] Existing patterns identified
[ ] Tests are behavior-focused
[ ] No unnecessary dependency
[ ] Risks are named
```

---

## Phase 5 - Implement

Implementation prompt:

```md
Proceed with the plan.

Rules:
- Make the smallest correct change.
- Follow existing validation/error patterns.
- Add tests for missing, invalid, and valid input.
- Do not add dependencies.
- Stop and explain if scope needs to expand.

After editing:
- List changed files.
- Explain behavior change.
- List tests run and results.
- List assumptions.
```

---

## Phase 6 - Review

Use this review rubric:

```md
Correctness:
[ ] Valid input still works
[ ] Missing input fails correctly
[ ] Invalid input fails correctly
[ ] Error format matches project pattern

Security:
[ ] No PII or secrets in logs/tests
[ ] No auth behavior weakened
[ ] Input validation cannot be bypassed

Maintainability:
[ ] Existing style followed
[ ] No unnecessary abstraction
[ ] No dependency added

Testing:
[ ] New tests prove behavior
[ ] Existing tests pass
[ ] Tests are not brittle implementation checks

Operations:
[ ] No noisy logs
[ ] No risky config changes
[ ] Rollback is simple
```

---

## Phase 7 - PR Description

Prompt:

```md
Write a PR description from the current diff.

Format:
## Problem
## Solution
## Tests
## Risks
## Rollback

Rules:
- Be factual.
- Do not overclaim.
- Mention any tests that were not run.
```

---

## Phase 8 - Governance Reflection

Answer:

```md
1. Which Copilot surface did I use and why?
2. What context did I provide?
3. What did I explicitly forbid?
4. What tests proved the behavior?
5. What did I manually verify?
6. What could have gone wrong with a vague prompt?
7. Which enterprise guardrails would I add before scaling this workflow?
8. What would I improve in repo instructions after this run?
```

---

## Scoring

| Area | Points |
|---|---:|
| Task framing and acceptance criteria | 10 |
| Correct surface selection | 10 |
| Instructions/prompts/agent setup | 10 |
| Implementation correctness | 15 |
| Tests and validation | 15 |
| Security and responsible use | 10 |
| Review discipline | 10 |
| PR communication | 10 |
| Governance reflection | 10 |
| **Total** | **100** |

Readiness:

| Score | Level |
|---:|---|
| 0-59 | Needs foundations review |
| 60-74 | Functional |
| 75-89 | Production-ready with supervision |
| 90-100 | Pro-level Copilot operator |

---

## Final Capstone Summary Template

```md
# Copilot Production Capstone Summary

Repo:
Task:
Surface used:

Context configured:
- Instructions:
- Prompt files:
- Agent:
- Hooks/MCP:

Changed files:
- 

Tests run:
- 

Review findings:
- 

Governance notes:
- 

What I would improve next:
- 
```

---

## Completion Standard

You complete the capstone only when:

- the change is reviewable
- tests are meaningful
- the PR description is accurate
- the review checklist is complete
- you can explain every generated line you keep
- you can explain which guardrails prevent unsafe agent behavior

