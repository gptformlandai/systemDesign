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

## Total Score and Readiness Gates

| Total | Level | Action |
|---|---|---|
| 6-12 | Beginner | Focus on: Foundations + Safe Prompting + Quick Wins |
| 13-18 | Intermediate | Focus on: Custom Instructions + Prompt Files + Agent Mode |
| 19-24 | Advanced | Focus on: Custom Agents + Context Engineering + Token Optimization |
| 25-28 | Pro | Focus on: Daily rituals + Prompt library maintenance + Team sharing |
| 29-30 | Operating System | Maintain + evolve. Share with team. Write a team playbook |

**Your total**: ___/30

---

## Improvement Tracking

Re-run this scoring rubric every 30 days.

| Date | Prompts | Context | Review | Safety | Config | Workflow | Total |
|---|---|---|---|---|---|---|---|
| | | | | | | | /30 |
| | | | | | | | /30 |
| | | | | | | | /30 |
