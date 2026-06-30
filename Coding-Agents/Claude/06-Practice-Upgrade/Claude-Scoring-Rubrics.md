# Claude Scoring Rubrics

> **Track**: Claude Mastery Track — Group 6: Practice Upgrade
<<<<<<< HEAD
> **File**: 4 of 5 (Track File #33)
> **Usage**: Honest self-assessment — score yourself weekly, track improvement

---

## Rubric 1 — Prompt Quality (1-5)

| Score | Description |
|-------|-------------|
| 1 | Vague prompts: "fix this", "make it better", "help me" |
| 2 | Has some context but no constraints or output format |
| 3 | CRISP structure. Uses #file/@file references. Has one constraint. |
| 4 | CRISP + output format + 2+ constraints + length limit. Uses XML tags for complex tasks. |
| 5 | All above + COT for reasoning tasks + prefilling for output control + self-critique for security code |

Your score: ___/5

---

## Rubric 2 — Context Management (1-5)

| Score | Description |
|-------|-------------|
| 1 | No context management. Same long session for every task. |
| 2 | Sometimes references files, but also re-explains project every session. |
| 3 | CLAUDE.md loaded and working. Session primer at start. Files referenced by @file. |
| 4 | Three-tier architecture (CLAUDE.md / session primer / per-message context). Summarizes long sessions. |
| 5 | Dynamic context loading. Context poisoning defense. Decision journal. Compression before new sessions. |

Your score: ___/5

---

## Rubric 3 — Verification Discipline (1-5)

| Score | Description |
|-------|-------------|
| 1 | No tests. Accepts Claude code without running it. |
| 2 | Runs tests manually but doesn't have Claude in the verification loop. |
| 3 | Claude runs tests after implementation. Fixes failures before accepting. |
| 4 | Verification-first (tests before implementation). All 6 gates used. Stopping conditions defined. |
| 5 | Autonomous loops. Claude iterates until all gates pass. Test modification never happens. |

Your score: ___/5

---

## Rubric 4 — Agent System Design (1-5)

| Score | Description |
|-------|-------------|
| 1 | No agent files. All Claude work in one long generalist session. |
| 2 | Has some commands but no agent files or skills. |
| 3 | Has 4+ agent files. Uses @planner and @builder for feature work. |
| 4 | Full 7-agent library. 4-agent pipeline operational. Handoff documents used. |
| 5 | Parallel agent execution. Failure recovery across agent boundaries. Agent metrics tracked. |

Your score: ___/5

---

## Rubric 5 — Safety and Responsibility (1-5)

| Score | Description |
|-------|-------------|
| 1 | Has pasted real credentials or customer data into Claude. |
| 2 | Knows the rules but hasn't made them habits. |
| 3 | Never pastes secrets. Checkpoint commits before agent sessions. Hooks installed. |
| 4 | All 6 security rules. XML wrapping for untrusted content. Secret scanning in CI. |
| 5 | Context poisoning defense. Production environment blocked by hooks. Security gate in every PR. |

Your score: ___/5

---

## Rubric 6 — System Maturity (1-5)

| Score | Description |
|-------|-------------|
| 1 | No CLAUDE.md. No commands. No structured daily workflow. |
| 2 | Basic CLAUDE.md. A few ad-hoc commands. Irregular usage. |
| 3 | CLAUDE.md updated monthly. 9+ commands. Morning ritual. |
| 4 | Three-level CLAUDE.md. All 9 commands. All 4 skills. Weekly system review. Session notes. |
| 5 | Personal OS with metrics. Monthly CLAUDE.md audit. Commands library growing. Level 4+ maturity. |

Your score: ___/5
=======
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
>>>>>>> refs/remotes/origin/main

---

## Total and Readiness Gates

<<<<<<< HEAD
| Total | Level | Next Action |
|-------|-------|-------------|
| 6-12 | Beginner | Focus: Foundations sheets + CLAUDE.md + first 3 commands |
| 13-18 | Early Intermediate | Focus: Full command library + verification loops |
| 19-24 | Intermediate | Focus: Agent system + hooks + skills |
| 25-28 | Advanced | Focus: 4-agent pipeline + MCP + context optimization |
| 29-30 | Pro OS | Maintain + evolve. Write your personal playbook. |

**Your total**: ___/30

---

## Improvement Tracker

| Date | Prompts | Context | Verification | Agents | Safety | System | Total |
|------|---------|---------|--------------|--------|--------|--------|-------|
| | /5 | /5 | /5 | /5 | /5 | /5 | /30 |
| | /5 | /5 | /5 | /5 | /5 | /5 | /30 |
| | /5 | /5 | /5 | /5 | /5 | /5 | /30 |
| | /5 | /5 | /5 | /5 | /5 | /5 | /30 |
=======
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
>>>>>>> refs/remotes/origin/main
