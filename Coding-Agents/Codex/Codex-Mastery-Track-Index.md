# OpenAI Codex Mastery Track Index

This folder is the Codex mastery track — from first `codex` command to expert-level
autonomous agentic workflows, multi-agent orchestration, and a personal Codex Operating System.

Audience:
- You are a developer who has heard of Codex but haven't gone deep beyond basic prompts.
- You may have used GitHub Copilot but not the Codex CLI agent (a different product entirely).
- You want to reach the level where Codex autonomously plans, implements, tests, and ships code.

Goal:
- Master every Codex surface: CLI interactive mode, full-auto mode, AGENTS.md, system prompts, tool use.
- Build a complete Codex configuration system (AGENTS.md, reusable scripts, approval policies).
- Develop disciplined, verification-driven, token-efficient agentic workflows.
- Reach the level where Codex is your full-cycle AI development partner.

---

## What Is OpenAI Codex CLI?

```
OpenAI Codex CLI (released April 2025) is an open-source terminal-based AI coding agent.

  Install:  npm install -g @openai/codex
  Run:      codex                         ← interactive mode
            codex "add pagination to the users API"  ← non-interactive
            codex --approval-policy full-auto "refactor auth module"

  What it can do:
  - Read and write files in your project
  - Run terminal commands (tests, linters, builds)
  - Iterate until tests pass
  - Follow project-level instructions from AGENTS.md

  Key models:
  - o4-mini (default, fast, cost-efficient)
  - gpt-4.1 (strongest reasoning, best for architecture)
  - gpt-4.1-mini (balance of speed and quality)

  NOT the same as:
  - GitHub Copilot (IDE autocomplete extension)
  - ChatGPT (conversational web interface)
  - Claude Code CLI (Anthropic's CLI agent — similar concept, different ecosystem)
```

---

## How To Read This Track

Before anything else, internalize these five reframes:

### 1. Codex is a planning + execution machine — scope it precisely

Codex performs best when given a well-defined task with explicit constraints.
A precise AGENTS.md + scoped task description + clear verification command = expert output.
A vague "fix everything" prompt = unpredictable changes across many files.

### 2. AGENTS.md is your persistent project memory

Codex re-reads AGENTS.md at the start of every session. Everything you want Codex to know
permanently — coding standards, architecture decisions, forbidden actions — belongs there.
Without it, Codex applies its training defaults, not your project conventions.

### 3. Approval policy is a risk dial — not a binary switch

`suggest` = safe, you see everything before it happens.
`auto-edit` = Codex edits files freely, asks before running commands.
`full-auto` = fully autonomous. Use only with a clean git checkpoint and bounded scope.
Always start in `suggest` mode when trying Codex on a new codebase.

### 4. Verification loops are the core discipline

The pattern: give Codex a task with a verification command → Codex executes → Codex reads
output → Codex iterates until the verification passes. Without a verification command,
Codex stops when it thinks it's done. With one, Codex knows when it's actually done.

### 5. Review is always yours — autonomy is bounded, not unlimited

Codex can run in full-auto mode, but you own every commit. Review the full diff before
committing. The agent's job is to generate; your job is to evaluate and decide.

---

## Practical Impact Meter — Legend

```
★★★★★ — Use this daily; not knowing it costs hours per week
★★★★☆ — Core professional skill; expert Codex users need this
★★★☆☆ — Advanced technique; adds significant leverage at senior level
★★☆☆☆ — Situational; useful in specific workflows
★☆☆☆☆ — Niche; know it exists but rarely needed
```

---

## 1. Foundations Path

Read these first. They build Codex intuition from zero.

| Order | File | What It Builds |
|---:|---|---|
| 1 | `01-Foundations/Codex-Mental-Model-Gold-Sheet.md` | Codex vs Copilot vs Claude Code, agent mindset, how Codex processes context, why it excels at autonomous loops |
| 2 | `01-Foundations/Codex-Setup-Personal-Machine-Gold-Sheet.md` | npm install, API key, config file, first run, model selection, verification protocol |
| 3 | `01-Foundations/Codex-CLI-Fundamentals-Gold-Sheet.md` | Interactive vs non-interactive mode, file context, how Codex reads the repo, `/help` `/compact` commands |
| 4 | `01-Foundations/Prompt-Engineering-for-Codex-Gold-Sheet.md` | Prompt clarity, constraints, output format control, bad vs good gallery, 10-minute drill |
| 5 | `01-Foundations/Safe-Usage-Principles-Gold-Sheet.md` | No secrets in prompts, suggest-first policy, commit checkpoints, review before merge |
| 6 | `01-Foundations/Codex-For-Beginners-Quick-Wins-Gold-Sheet.md` | First 10 high-ROI tasks: explain file, generate tests, fix failing test, write docs, add input validation |

Foundations target:
- You understand how Codex differs from Copilot and Claude Code.
- Codex CLI is installed, API key configured, first task completed.
- You can write structured prompts that produce targeted, high-quality output.
- You use `suggest` mode by default and know when to escalate to `auto-edit`.
- You never put secrets into Codex prompts.

---

## 2. Intermediate Power User Path

After foundations, read these.

| Order | File | What It Builds |
|---:|---|---|
| 7 | `02-Intermediate-Power-User/AGENTS-MD-Design-Gold-Sheet.md` | AGENTS.md anatomy, root vs subfolder files, project rules, coding standards, forbidden patterns |
| 8 | `02-Intermediate-Power-User/Context-Engineering-Gold-Sheet.md` | Context window mechanics, what to include vs exclude, session drift, scoping to relevant files |
| 9 | `02-Intermediate-Power-User/Codex-CLI-Deep-Dive-Gold-Sheet.md` | All CLI flags, config file (~/.codex/config.yaml), model selection, wrapping Codex in scripts |
| 10 | `02-Intermediate-Power-User/Approval-Policy-Modes-Gold-Sheet.md` | suggest / auto-edit / full-auto deep dive, when to use each, sandbox setup, recovery |
| 11 | `02-Intermediate-Power-User/Codex-For-Testing-Gold-Sheet.md` | Test generation, TDD loop, gap analysis, mocking strategy, verification via pytest/jest |
| 12 | `02-Intermediate-Power-User/Codex-For-Documentation-Gold-Sheet.md` | Docstrings, README, API docs, ADRs, onboarding docs from codebase |
| 13 | `02-Intermediate-Power-User/Before-After-Prompt-Examples-Gold-Sheet.md` | 10 before/after pairs: debug, implement, test, review, refactor, architecture, security |

Intermediate target:
- You have a working AGENTS.md for at least one real project.
- You use `auto-edit` mode confidently with a checkpoint commit in place.
- You can generate tests and documentation with Codex at expert level.
- You manage context window usage consciously — scoped files, not entire repo.
- You have 5+ reusable prompt scripts for your most common tasks.

---

## 3. Advanced Engineering Path

These are the leverage multipliers.

| Order | File | What It Builds |
|---:|---|---|
| 14 | `03-Advanced-Engineering/Full-Auto-Mode-Gold-Sheet.md` | Full-auto deep dive, pre-conditions, safety guardrails, monitoring during execution, recovery |
| 15 | `03-Advanced-Engineering/System-Prompt-Engineering-Gold-Sheet.md` | AGENTS.md vs --system-prompt, writing effective system prompts, persona, constraint architecture |
| 16 | `03-Advanced-Engineering/Agent-Loops-Gold-Sheet.md` | Loop engineering, plan→execute→verify cycles, stopping conditions, iteration limits |
| 17 | `03-Advanced-Engineering/Multi-Agent-Patterns-Gold-Sheet.md` | Planner→Builder→Tester pipeline using separate Codex sessions, context handoff, failure recovery |
| 18 | `03-Advanced-Engineering/Token-Context-Optimization-Gold-Sheet.md` | Token budgeting, /compact command, model selection by task complexity, high-signal prompts |
| 19 | `03-Advanced-Engineering/Tool-Use-and-Shell-Integration-Gold-Sheet.md` | Codex running shell commands, piping output back, scripted workflows, Makefile integration |
| 20 | `03-Advanced-Engineering/OpenAI-API-Integration-Gold-Sheet.md` | Using the Codex model via API directly, assistant threads, streaming, function calling |

Advanced target:
- You can run full-auto mode safely on any bounded task.
- You design and run multi-agent Codex pipelines with context isolation.
- You understand token budgets and /compact correctly.
- You integrate Codex into Makefiles and CI scripts.
- You can use the OpenAI API directly for custom Codex workflows.

---

## 4. Pro / Production Level Path

These are the professional operating system patterns.

| Order | File | What It Builds |
|---:|---|---|
| 21 | `04-Pro-MAANG-Level/Personal-Codex-Operating-System-Gold-Sheet.md` | Daily rituals, personal AGENTS.md OS, session planning, end-of-day capture, maintenance schedule |
| 22 | `04-Pro-MAANG-Level/SDLC-Automation-Gold-Sheet.md` | Codex across all 10 SDLC phases: requirements → architecture → test-first → implementation → pre-commit → release |
| 23 | `04-Pro-MAANG-Level/Debugging-Codex-Handbook-Gold-Sheet.md` | 9 Codex failure modes with symptom, root cause, fix, prevention: hallucination, drift, scope creep, test modification, over-mocking, infinite retry, layer violations, hallucinated structure, security blind spot |
| 24 | `04-Pro-MAANG-Level/Verification-Driven-Workflows-Gold-Sheet.md` | Test-first, lint loops, CI integration, Codex iterates until green |
| 25 | `04-Pro-MAANG-Level/Autonomous-Workflows-Gold-Sheet.md` | Full autonomous pipelines, safe autonomy principles, guardrails, checkpoint strategy |

Pro target:
- You operate a personal Codex OS with daily rituals and a reusable AGENTS.md library.
- You apply Codex at every stage of a real SDLC workflow.
- You can diagnose any Codex failure mode from the handbook.
- You design verification loops where Codex iterates until tests pass.
- You run safe autonomous workflows on real production codebases.

---

## 5. Scenario Practice Path

| Order | File | What It Builds |
|---:|---|---|
| 26 | `05-Scenario-Practice/Daily-Workflow-Scenarios-Gold-Sheet.md` | Morning planning, coding session, pre-commit, end-of-day rituals |
| 27 | `05-Scenario-Practice/Feature-Building-Scenarios-Gold-Sheet.md` | Full feature delivery: plan → implement → test → review |
| 28 | `05-Scenario-Practice/Debugging-Scenarios-Gold-Sheet.md` | Error diagnosis, stack trace analysis, CI vs local failures |
| 29 | `05-Scenario-Practice/Code-Review-Scenarios-Gold-Sheet.md` | Security, test coverage, architecture review, constructive PR comments |

---

## 6. Practice Upgrade Path

| Order | File | What It Builds |
|---:|---|---|
| 30 | `06-Practice-Upgrade/Codex-Active-Recall-Question-Bank.md` | 30 Q&A pairs with full answers mapped to every track section |
| 31 | `06-Practice-Upgrade/Codex-Prompt-Script-Library.md` | Ready-to-use reusable prompt scripts for every major workflow |
| 32 | `06-Practice-Upgrade/Codex-Mock-Workflow-Scripts.md` | 7 timed drills: planning, testing, debugging, pre-commit, full-auto, tokens, pipeline |
| 33 | `06-Practice-Upgrade/Codex-Scoring-Rubrics.md` | 7 rubrics /35: prompt quality, AGENTS.md quality, workflow maturity |
| 34 | `06-Practice-Upgrade/Codex-4-Week-Mastery-Roadmap.md` | Day-by-day plan from beginner to pro operating level |
| 35 | `06-Practice-Upgrade/Codex-MAANG-Interview-Prep-Gold-Sheet.md` | L3–L7 interview Q&A, 60-second pitch, anti-patterns table |

---

## 7. Configuration Reference Library

```
config/
  AGENTS.md                           ← root AGENTS.md template (customize per project)
  subfolder-AGENTS.md                 ← subfolder override template
  system-prompt-template.md           ← --system-prompt flag template
  ~/.codex/config.yaml                ← global config template (model, approval-policy)
```

---

## 8. What A Pro-Level Codex User Masters

### Mental Model and Setup
- Knows what Codex CLI is vs Copilot vs Claude Code
- Codex CLI installed, OPENAI_API_KEY set, config.yaml configured
- AGENTS.md in every active project, loaded and verified

### Configuration
- Root AGENTS.md with project context, architecture rules, forbidden patterns
- Subfolder AGENTS.md for domain-specific overrides
- Reusable prompt scripts for every repeated workflow
- Model selection matched to task complexity
- Approval policy matched to task risk level

### Workflows
- Plan-first for every non-trivial task
- Verification loops (run tests → read output → iterate → green)
- Checkpoint commits before every full-auto session
- Token-efficient prompts by habit
- Context scoping: give Codex exactly the files it needs, not the whole repo

### Daily Operation
- Morning planning: scope the day's tasks before writing any code
- Pre-commit: security + test gap review before every PR
- End-of-day: capture best prompts from the session as reusable scripts
- Weekly: review and update AGENTS.md based on what broke
