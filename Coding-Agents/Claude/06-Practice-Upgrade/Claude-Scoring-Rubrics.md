# Claude Scoring Rubrics

> **Track**: Claude Mastery Track — Group 6: Practice Upgrade
> **File**: 3 of 5 (Track File #32)
> **Usage**: Self-assessment — score your Claude usage honestly every 30 days

---

## Rubric 1 — Prompt Quality (1–5)

| Score | Description |
|-------|-------------|
| 1 | Vague prompts: "Fix this", "Make it better", "Help me with my code" |
| 2 | Basic context (pastes code) but no constraints, output format, or goal stated |
| 3 | Clear goal + code reference + one constraint. Uses CRISP structure occasionally |
| 4 | CRISP structure consistently: context, role (when useful), instruction, scope, process. Specifies output format. |
| 5 | Token-efficient, precise prompts. References existing patterns via @file. States forbidden actions explicitly. Asks for plan before code on complex tasks. |

**Your score**: ___/5

---

## Rubric 2 — CLAUDE.md Quality (1–5)

| Score | Description |
|-------|-------------|
| 1 | No CLAUDE.md in any project |
| 2 | A CLAUDE.md exists but it's vague or generic (just the template, not customized) |
| 3 | Working CLAUDE.md with tech stack, architecture notes, and at least 3 Do NOT rules |
| 4 | Root CLAUDE.md + subfolder CLAUDE.md for key domains (tests/, api/). Rules come from real violations observed. |
| 5 | Living document: updated weekly based on violations, new conventions, or capability changes. Verified working by testing Claude's output against the rules. |

**Your score**: ___/5

---

## Rubric 3 — Output Review Discipline (1–5)

| Score | Description |
|-------|-------------|
| 1 | Accepts Claude output without reading. "Accept All" is the default. |
| 2 | Reads output quickly but doesn't check for security issues or verify tests run |
| 3 | Reads all diffs, checks new imports, runs tests after accepting |
| 4 | Runs /security for auth/SQL/input code. Runs tests. Checks git diff for unexpected changes. Validates new dependencies. |
| 5 | Never commits code they can't explain line by line. Full review before every commit: security + tests + lint + no unexpected files. |

**Your score**: ___/5

---

## Rubric 4 — Safety and Responsible Use (1–5)

| Score | Description |
|-------|-------------|
| 1 | Has pasted real credentials or PII into Claude |
| 2 | Knows the rules but hasn't established them as habits |
| 3 | Never pastes secrets. Always anonymizes data before debugging incidents. Commits before agent sessions. |
| 4 | GREEN/YELLOW/RED data classification applied automatically. Never uses real production data for debugging. Pre_tool_use.sh blocking dangerous commands. |
| 5 | All rules followed by habit. Actively helps team members adopt safe practices. Reviews MCP tokens quarterly. |

**Your score**: ___/5

---

## Rubric 5 — Configuration System Maturity (1–5)

| Score | Description |
|-------|-------------|
| 1 | No CLAUDE.md, no slash commands, no agents, no hooks |
| 2 | Basic CLAUDE.md but no slash commands or agents configured |
| 3 | CLAUDE.md + 5+ slash commands + at least 2 custom agents |
| 4 | Full configuration: CLAUDE.md + 8+ commands + 5+ agents + 3+ skills + pre_tool_use.sh hook |
| 5 | Personal Claude OS operational: all configuration components, MCP configured, daily rituals established, portable across projects |

**Your score**: ___/5

---

## Rubric 6 — Workflow Integration (1–5)

| Score | Description |
|-------|-------------|
| 1 | Uses Claude occasionally for code generation only |
| 2 | Uses Chat for debugging and explanations. Rarely uses agent mode. |
| 3 | Uses correct mode for task type. Runs pre-commit review. Has test-first habit. |
| 4 | Claude integrated at all 12 SDLC phases. Verification loops standard. Session notes regular. |
| 5 | Full daily ritual: morning planning + session discipline + pre-commit review + end-of-day notes. Command library grows weekly. Multi-agent pipeline for complex features. |

**Your score**: ___/5

---

## Rubric 7 — Agentic Depth (1–5)

| Score | Description |
|-------|-------------|
| 1 | Has never used Claude Code CLI |
| 2 | Uses CLI for single-file tasks but no agent sessions |
| 3 | Runs agent sessions with safety checklist. Uses verification loops. Has stopping conditions. |
| 4 | Uses multi-agent pipeline (planner → builder → tester → reviewer) for features. Has handoff documents. |
| 5 | Fully autonomous workflows with verification loops, hooks, and structured recovery patterns. Can run a full feature build autonomously and review only the final diff. |

**Your score**: ___/5

---

## Total and Readiness Gates

| Total | Level | Next Focus |
|-------|-------|-----------|
| 7–14 | Beginner | 01-Foundations + Quick Wins exercises |
| 15–21 | Intermediate | CLAUDE.md Design + Slash Commands + Context Engineering |
| 22–28 | Advanced | Custom Agents + Skills + Hooks + Agent Loops |
| 29–32 | Pro | Daily rituals + Multi-agent pipeline + Autonomous workflows |
| 33–35 | Operating System | Maintain + evolve. Share with team. Write a team CLAUDE.md template. |

**Your total**: ___/35

---

## Progress Tracker (re-score monthly)

| Date | Prompts | CLAUDE.md | Review | Safety | Config | Workflow | Agentic | Total |
|------|---------|-----------|--------|--------|--------|----------|---------|-------|
| | | | | | | | | /35 |
| | | | | | | | | /35 |
| | | | | | | | | /35 |
| | | | | | | | | /35 |
