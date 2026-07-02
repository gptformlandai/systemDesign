# Copilot Cloud Agent / Coding Agent Gold Sheet

> **Track**: Copilot Mastery Track - Group 4: Pro / MAANG Level
> **File**: 27a
> **Audience**: Developers who want Copilot to handle real GitHub tasks safely, not just chat inside an IDE

---

## Practical Impact Meter

5/5 - This is the difference between "Copilot helps me type" and "Copilot can carry a scoped engineering task through a PR while I review like an owner."

---

## 1. Intuition

Think of Copilot cloud agent as a junior developer running inside a temporary GitHub Actions-powered workspace.

You do not ask it for snippets. You assign it a bounded task, give it repository rules, let it create a branch and proposed changes, then review the result through a PR/session workflow.

It is not the same thing as IDE Agent Mode:

| Surface | Runs Where | Best For |
|---|---|---|
| IDE Agent Mode | Your local editor/workspace | Hands-on tasks where you want immediate local control |
| Copilot cloud agent | GitHub-managed cloud environment | Issue-sized work, PR follow-up, background implementation, repo research |
| Copilot CLI | Terminal/local or automated contexts | Shell-first agentic work, scripts, PR/issue workflows, programmatic sessions |

---

## 2. Definition

- **Definition:** Copilot cloud agent is an autonomous GitHub-hosted coding agent that can inspect a repository, plan a change, edit files, run commands, and produce branch/PR output for human review.
- **Category:** Agentic software development workflow.
- **Core idea:** Move from prompt-response coding to task delegation with guarded execution and review gates.

---

## 3. Why It Exists

Traditional Copilot chat is synchronous: you ask, it answers, you apply.

Cloud agent exists because many engineering tasks are not one answer:
- understand the issue
- search the repo
- find existing patterns
- modify several files
- update tests
- run checks
- prepare a reviewable change
- respond to PR feedback

Without a coding agent, you become the glue between all those steps. With a coding agent, your job shifts toward task framing, constraint setting, review, and final ownership.

---

## 4. What It Can Do

Cloud agent is strong at:

- small to medium issue implementation
- bug reproduction and proposed fixes
- test generation for touched behavior
- documentation sync after code changes
- dependency upgrade research and PR preparation
- PR follow-up tasks after review comments
- codebase exploration before implementation
- repetitive migration tasks with clear rules

Cloud agent is weak or risky for:

- ambiguous product design
- large architecture rewrites without human checkpoints
- tasks requiring live production credentials
- destructive database or infrastructure operations
- security-sensitive changes without specialist review
- work where acceptance criteria are not testable

---

## 5. Execution Lifecycle

1. **Task intake**
   - You assign an issue, prompt, or task with acceptance criteria.
   - The better the issue, the better the agent.

2. **Environment setup**
   - Copilot gets a temporary cloud workspace.
   - Repo files, configured instructions, available tools, hooks, and allowed MCP servers shape what it can do.

3. **Context discovery**
   - Agent searches the repo, reads relevant files, checks existing patterns, and builds an implementation plan.

4. **Implementation**
   - Agent edits files, may run commands/tests, and iterates.
   - It should prefer existing patterns over new abstractions.

5. **Output**
   - It creates or updates a branch/session/PR depending on how the task was started.
   - Human review remains the merge gate.

6. **Feedback loop**
   - You review diffs, comments, tests, and risks.
   - Agent can address follow-up comments, but you own the final acceptance.

---

## 6. Strong Cloud Agent Issue Template

Use this when assigning real work:

```md
## Goal
Implement [specific behavior].

## User-visible behavior
- Given [state], when [action], then [result].
- Error case: [expected behavior].

## Scope
Allowed:
- Modify [paths/modules].
- Add tests under [test path].

Not allowed:
- Do not change public API [X].
- Do not add new dependencies.
- Do not touch deployment config.

## Existing patterns to follow
- Use the same style as [file/path].
- Use existing helper [function/class].

## Validation
- Run [test command].
- Add/modify tests for [cases].

## Review notes
- Explain the files changed.
- Call out assumptions.
- Call out anything not completed.
```

---

## 7. Agent Readiness Checklist

Before assigning tasks:

```md
[ ] Repo has `.github/copilot-instructions.md`
[ ] Important folders have `.github/instructions/*.instructions.md` or `AGENTS.md`
[ ] CI is reliable enough that failures mean something
[ ] Branch protection requires tests and human review
[ ] Sensitive files are covered by content exclusion or instructions
[ ] MCP servers are least privilege and reviewed
[ ] Hooks exist for secret scanning and policy checks
[ ] Repo has a clear test command documented
[ ] The task has acceptance criteria, not just "fix this"
```

---

## 8. Safety Controls

| Risk | Control |
|---|---|
| Agent changes too much | Tight issue scope, path constraints, required human PR review |
| Agent uses wrong pattern | Repo instructions, examples, custom agents, Memory review |
| Secret leak | Content exclusion, secret scanning hook, no production secrets in environment |
| Tool misuse | MCP allowlists, firewall rules, least-privilege tokens |
| Cost spike | AI credit budget, model choice, task size limits |
| Low-quality PRs | CI gates, review rubric, required tests, small batches |
| Silent hallucination | Require evidence: files inspected, tests run, assumptions listed |

---

## 9. Best Task Sizes

| Task Size | Example | Fit |
|---|---|---|
| 15-30 min human task | Add validation + tests | Excellent |
| 1-2 hour human task | Implement one endpoint and tests | Good |
| Half-day human task | Migrate several related files | Good only with checkpoints |
| Multi-day project | Rewrite auth system | Poor unless decomposed into issues |

Rule:
- If the task cannot be reviewed in one focused PR, split it before assigning it.

---

## 10. Review Protocol For Agent PRs

Read agent PRs in this order:

1. **Intent**
   - Does the PR solve the original issue?
   - Did it invent extra behavior?

2. **Diff shape**
   - Are changed files expected?
   - Any config, auth, dependency, or generated file surprise?

3. **Correctness**
   - Happy path, edge cases, error paths.
   - Public API compatibility.

4. **Security**
   - Auth, authorization, injection, PII in logs, secret handling.

5. **Tests**
   - New tests prove the behavior.
   - Existing tests still pass.

6. **Maintainability**
   - Follows local patterns.
   - No needless abstraction.

7. **Cost/operations**
   - No noisy retries, expensive loops, runaway jobs, or broad API calls.

---

## 11. Cloud Agent Prompt Patterns

### Plan-first cloud task

```md
Before editing, inspect the relevant files and produce a brief plan.

Task:
[issue-sized goal]

Constraints:
- Only modify [paths].
- Do not add dependencies.
- Keep public APIs compatible.

Validation:
- Add tests for [cases].
- Run [command].

Output:
- Summary of files changed.
- Tests run and results.
- Assumptions.
```

### PR review follow-up

```md
Address only the review comments on this PR.

Rules:
- Do not refactor unrelated code.
- Preserve the existing public API.
- Add or update tests only where needed for the comments.
- Summarize each comment and the exact fix.
```

### Bug fix with reproduction

```md
Investigate and fix this bug.

Observed:
[symptom]

Expected:
[expected behavior]

Evidence:
[stack trace, failing test, issue link, or log excerpt]

Required:
1. Identify likely root cause.
2. Add a failing test if practical.
3. Implement the smallest fix.
4. Run the relevant tests.
5. Explain why the fix is safe.
```

---

## 12. Failure Modes

| Failure Mode | Symptom | Prevention |
|---|---|---|
| Scope drift | PR touches many unrelated files | Path constraints, issue split, review reject |
| Pattern mismatch | New style ignores local conventions | Strong instructions and example files |
| Test theater | Tests assert implementation, not behavior | Write acceptance criteria first |
| False success | Agent says tests pass but evidence is weak | Require command output summary |
| Risky dependency | Adds package for tiny helper | "No new dependencies" default |
| Over-trust | Human skims because PR "looks clean" | Review every generated diff like any other PR |

---

## 13. Metrics For Teams

Do not measure only "PRs created by Copilot." That rewards volume, not engineering value.

Better metrics:

- PRs created, merged, and reverted
- median time from assigned task to review-ready PR
- review comments per agent PR
- defect escape rate from agent-assisted PRs
- test coverage delta on touched code
- AI credits and Actions minutes per merged PR
- percent of tasks requiring human rescue
- top repeated failure categories

Strong maturity signal:
- Agent PRs get smaller over time, not larger.
- Human review finds fewer repeated issues.
- Instructions and hooks evolve based on real failures.

---

## 14. When To Use A Custom Agent

Use a custom agent when the task needs a repeated role:

| Role | Example |
|---|---|
| Release fixer | "Only touch release notes, changelog, tags, version files" |
| Test engineer | "Generate behavior-first tests, avoid brittle mocks" |
| Security reviewer | "Find OWASP risks; do not auto-fix critical issues" |
| Migration assistant | "Apply this exact framework migration pattern" |
| Docs maintainer | "Update docs only when code behavior changed" |

Do not create a custom agent for every prompt. Create one when the constraints are reusable and easy to test.

---

## 15. Interview Answer

> "I use Copilot cloud agent only for scoped, reviewable tasks. I write issues with acceptance criteria, path constraints, validation commands, and explicit non-goals. The agent can search the repo, implement on a branch, run checks, and open a PR, but branch protection, CI, and human review remain the control plane. For enterprise use I add content exclusion, least-privilege MCP, hooks for secret/policy checks, and budget monitoring so speed does not bypass governance."

---

## 16. Revision Notes

- One-line summary: Cloud agent is task delegation, not autocomplete.
- Three keywords: issue scope, guarded execution, human review.
- One interview trap: saying "the agent merges code" instead of "the agent proposes code."
- Memory trick: treat it like a junior developer in a temporary GitHub Actions workspace.

---

## Official Source Anchors

- https://docs.github.com/en/copilot/concepts/agents/cloud-agent/about-cloud-agent
- https://docs.github.com/en/copilot/concepts/agents/cloud-agent/about-custom-agents
- https://docs.github.com/en/copilot/concepts/agents/cloud-agent/risks-and-mitigations
