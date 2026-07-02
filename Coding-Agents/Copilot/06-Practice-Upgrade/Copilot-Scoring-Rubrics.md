# Copilot Scoring Rubrics

> **Track**: Copilot Mastery Track — Group 6: Practice Upgrade
> **File**: 4 of 5 (Track File #35)
> **Usage**: Self-assessment tool — score your Copilot usage honestly

---

## Rubric 1 — Prompt Quality (1-5)

| Score | Description |
|---|---|
| 1 | Vague prompts with no context. "Fix this", "Make it better", "Help me" |
| 2 | Basic context (pastes code) but no constraints or output format specified |
| 3 | Clear goal with context attached via #file or #selection. Has one constraint |
| 4 | Context + goal + output format + constraints. Uses CGOFC pattern consistently |
| 5 | Token-efficient, precise prompts. References existing patterns. Lists assumptions first for complex tasks |

**Your current score**: ___/5

---

## Rubric 2 — Context Management (1-5)

| Score | Description |
|---|---|
| 1 | No context attached. Types prompts with no file references |
| 2 | Sometimes uses #file or #selection. Often types descriptions instead of attaching |
| 3 | Consistently uses context variables. Knows #file vs #codebase vs #selection |
| 4 | Uses minimum viable context (only relevant files). Starts new conversations for new topics |
| 5 | Has project context file. Applies summarize-before-ask for large codebases. Never hits context window issues |

**Your current score**: ___/5

---

## Rubric 3 — Output Review Discipline (1-5)

| Score | Description |
|---|---|
| 1 | Accepts Copilot output without reading. Accept All is the default |
| 2 | Reads output quickly but doesn't check for security issues or edge cases |
| 3 | Reads all diffs, checks new imports, runs tests after accepting |
| 4 | Runs security review for auth/SQL/input code. Runs tests. Validates new dependencies |
| 5 | Full AI output evaluation standard applied before every commit. Never commits code they can't explain |

**Your current score**: ___/5

---

## Rubric 4 — Safety and Responsible Use (1-5)

| Score | Description |
|---|---|
| 1 | Has pasted real credentials or PII into Copilot |
| 2 | Knows the rules but hasn't established them as habits |
| 3 | Never pastes secrets. Uses synthetic data. Commits before Agent Mode |
| 4 | GREEN/YELLOW/RED data classification applied. Never uses real production data for debugging |
| 5 | All 12 non-negotiable rules followed by habit. Actively helps team members adopt safe practices |

**Your current score**: ___/5

---

## Rubric 5 — Configuration and System Maturity (1-5)

| Score | Description |
|---|---|
| 1 | No custom instructions, no prompt files, no agents configured |
| 2 | Has a basic copilot-instructions.md but it's vague and rarely followed |
| 3 | Working instructions, 5+ prompt files as slash commands, 3+ custom agents |
| 4 | Full AGENTS.md strategy (root + folder levels), prompt library of 10+, MCP configured |
| 5 | Personal Copilot OS operational with daily rituals, maintained prompt library, portable workspace |

**Your current score**: ___/5

---

## Rubric 6 — Workflow Integration (1-5)

| Score | Description |
|---|---|
| 1 | Uses Copilot occasionally for code generation only |
| 2 | Uses Chat for debugging and generation. Rarely uses Edits or Agent Mode |
| 3 | Uses appropriate mode for each task type. Runs pre-PR review with Copilot |
| 4 | Copilot integrated at all 12 SDLC phases. Test-first workflow. Learning notes habit |
| 5 | Full daily ritual established. Prompt library grows weekly. Shares useful prompts with team |

**Your current score**: ___/5

---

## Rubric 7 — Agentic Platform Usage (1-5)

| Score | Description |
|---|---|
| 1 | Treats all Copilot surfaces as the same. Uses Agent Mode/cloud agent for vague tasks |
| 2 | Knows cloud agent and CLI exist but has no task framing or review process |
| 3 | Can choose IDE Agent Mode vs cloud agent vs CLI for common tasks |
| 4 | Writes cloud-agent-ready issues with scope, validation, and non-goals. Uses CLI with checkpoints |
| 5 | Runs agentic workflows with hooks, branch protection, CI, review gates, and rollback discipline |

**Your current score**: ___/5

---

## Rubric 8 — MCP, Indexing, And Tool Governance (1-5)

| Score | Description |
|---|---|
| 1 | Connects tools casually with broad tokens or unclear permissions |
| 2 | Understands MCP basics but exposes too many tools or writes real config with secrets |
| 3 | Uses example MCP configs, env vars, and read-only permissions for practice |
| 4 | Designs least-privilege toolsets and knows when to use `#file`, `#selection`, `#codebase`, indexing, or MCP |
| 5 | Can explain registry/allowlist strategy, content exclusion limits, and safe MCP rollout for a team |

**Your current score**: ___/5

---

## Rubric 9 — Memory, Hooks, And Skills Maturity (1-5)

| Score | Description |
|---|---|
| 1 | Relies on repeated prompts only; no reusable guardrails |
| 2 | Has instructions but no distinction between memory, hooks, and skills |
| 3 | Understands Memory as stable facts, hooks as guardrails, skills as playbooks |
| 4 | Uses hooks for secrets/protected paths and keeps skills procedural and reusable |
| 5 | Maintains a layered customization system: instructions for policy, Memory for facts, hooks for enforcement, skills for repeatable workflows |

**Your current score**: ___/5

---

## Rubric 10 — Enterprise Readiness (1-5)

| Score | Description |
|---|---|
| 1 | No awareness of policies, budgets, audit logs, or data governance |
| 2 | Knows enterprise settings exist but cannot explain rollout risks |
| 3 | Can identify access, content exclusion, model, MCP, and agentic policy needs |
| 4 | Can design a pilot rollout with metrics, cost controls, training, and review gates |
| 5 | Can lead governance review: policy conflicts, model availability, MCP allowlists, cloud agent readiness, auditability, AI credits, and quality metrics |

**Your current score**: ___/5

---

## Total Score and Readiness Gates

| Total | Level | Action |
|---|---|---|
| 10-20 | Beginner | Focus on: Foundations + Safe Prompting + Quick Wins |
| 21-30 | Intermediate | Focus on: Custom Instructions + Prompt Files + Agent Mode |
| 31-38 | Advanced | Focus on: Custom Agents + Context Engineering + Token Optimization |
| 39-44 | Pro | Focus on: Daily rituals + Prompt library maintenance + Team sharing |
| 45-50 | Production / Enterprise Ready | Maintain + evolve. Share with team. Write a team playbook and governance notes |

**Your total**: ___/50

---

## Improvement Tracking

Re-run this scoring rubric every 30 days.

| Date | Prompts | Context | Review | Safety | Config | Workflow | Agentic | MCP | Hooks/Memory | Enterprise | Total |
|---|---|---|---|---|---|---|---|---|---|---|---|
| | | | | | | | | | | | /50 |
| | | | | | | | | | | | /50 |
| | | | | | | | | | | | /50 |
