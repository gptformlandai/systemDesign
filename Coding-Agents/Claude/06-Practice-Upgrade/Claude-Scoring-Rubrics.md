# Claude Scoring Rubrics

> **Track**: Claude Mastery Track — Group 6: Practice Upgrade
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

---

## Total and Readiness Gates

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
