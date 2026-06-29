# Claude Mastery Track Index

This folder is the Claude mastery track — from first conversation to expert-level
agentic workflows, multi-agent orchestration, and a personal Claude Operating System.

Audience:
- You are a developer who has used Claude Chat casually but haven't gone deep.
- You may have tried Claude Code but aren't fluent in CLAUDE.md, slash commands, hooks, or subagents.
- You want to reach the level where Claude autonomously builds, tests, debugs, and ships code for you.

Goal:
- Master every Claude surface: Chat, Claude Code CLI, Desktop, MCP, subagents, skills, hooks.
- Build a complete Claude configuration system (CLAUDE.md, slash commands, agents, skills).
- Develop disciplined, token-efficient, verification-driven agentic workflows.
- Reach the level where Claude is your full-cycle AI development partner.

---

## How To Read This Track

Before anything else, internalize these five reframes:

### 1. Claude is a context machine — quality in, quality out

Claude's output quality is directly proportional to the quality of context it receives.
A precise CLAUDE.md + targeted slash command + correct file scope = expert-level output.
No setup + vague prompt = junior-quality output from a senior-capable model.

### 2. CLAUDE.md is your persistent memory

Claude has no cross-session memory by default. CLAUDE.md is your solution.
It is read at the start of every Claude Code session. Everything you want Claude to know
about your project permanently belongs there, not in repeated prompts.

### 3. Agentic ≠ autonomous blindly

Claude Code can autonomously create files, run commands, and iterate on code.
This power requires guardrails: verification steps, test requirements, explicit
stopping conditions, and commit checkpoints. The best Claude workflows are
autonomous but bounded, not autonomous and unchecked.

### 4. Subagents isolate context — context isolation prevents drift

Every time you start a new Claude Code session, you get a clean context window.
This is a feature. Multi-step complex tasks should be split across subagent sessions
to prevent context drift, hallucination compounding, and instruction conflicts.

### 5. Skills and hooks are multipliers — invest in them once

A well-written SKILL.md runs automatically when Claude detects the relevant task.
A well-written hook validates every tool use before it executes.
Write them once; benefit from them on every task forever.

---

## Practical Impact Meter — Legend

```
★★★★★ — Use this daily; not knowing it costs hours per week
★★★★☆ — Core professional skill; expert Claude users need this
★★★☆☆ — Advanced technique; adds significant leverage at senior level
★★☆☆☆ — Situational; useful in specific workflows
★☆☆☆☆ — Niche; know it exists but rarely needed
```

---

## 1. Foundations Path

Read these first. They build Claude intuition from zero.

| Order | File | What It Builds |
|---:|---|---|
| 1 | `01-Foundations/Claude-Mental-Model-Gold-Sheet.md` | Claude vs ChatGPT vs Copilot, how Claude processes context, why Claude excels at long context + reasoning + structured output |
| 2 | `01-Foundations/Claude-Setup-Personal-Machine-Gold-Sheet.md` | Claude.ai Chat setup, Claude Code CLI install, Claude Desktop, API key management, verification protocol |
| 3 | `01-Foundations/Claude-Chat-Fundamentals-Gold-Sheet.md` | Projects vs conversations, context sharing, file uploads, artifacts, web search, Claude.ai vs API |
| 4 | `01-Foundations/Prompt-Engineering-Fundamentals-Gold-Sheet.md` | Clarity, structured prompts, role prompting, multi-shot, output formatting, bad vs good prompt gallery |
| 5 | `01-Foundations/Safe-Usage-Principles-Gold-Sheet.md` | No secrets, synthetic data, review discipline, verification before execution, version control safety net |
| 6 | `01-Foundations/Claude-For-Beginners-Quick-Wins-Gold-Sheet.md` | First 10 high-ROI workflows: explain code, debug error, generate tests, write docs, plan feature |

Foundations target:
- You understand how Claude processes context and what it excels at vs other AI tools.
- You have Claude set up on your machine (Chat, Code CLI, Desktop).
- You can write structured prompts that produce targeted, high-quality output.
- You never paste secrets into Claude.
- You have completed at least 5 quick-win exercises.

---

## 2. Intermediate Power User Path

After foundations, read these.

| Order | File | What It Builds |
|---:|---|---|
| 7 | `02-Intermediate-Power-User/CLAUDE-MD-Design-Gold-Sheet.md` | Root CLAUDE.md anatomy, subfolder CLAUDE.md strategy, project rules, architecture decisions, coding standards |
| 8 | `02-Intermediate-Power-User/Slash-Commands-Gold-Sheet.md` | `.claude/commands/` anatomy, YAML frontmatter, 9 production commands (/explain /debug /refactor /test /review /plan /optimize), reusable patterns |
| 9 | `02-Intermediate-Power-User/Context-Engineering-Gold-Sheet.md` | Context window mechanics (200k tokens), what goes in vs out, session memory limits, summarization, chunking, context drift prevention |
| 10 | `02-Intermediate-Power-User/Claude-Code-CLI-Gold-Sheet.md` | CLI install, how Claude reads the repo, file editing mechanics, command execution, project initialization, safety flags |
| 11 | `02-Intermediate-Power-User/Claude-For-Testing-Gold-Sheet.md` | Test generation, test gap analysis, TDD with Claude, mocking strategy, verification loops |
| 12 | `02-Intermediate-Power-User/Claude-For-Documentation-Gold-Sheet.md` | README generation, docstring writing, API docs, ADRs, onboarding docs, release notes |
| 13 | `02-Intermediate-Power-User/Before-After-Prompt-Examples-Gold-Sheet.md` | 10 before/after pairs: debugging, refactoring, test gen, architecture review, PR review, feature planning |

Intermediate target:
- You have a working CLAUDE.md for at least one project.
- You have at least 5 slash commands in `.claude/commands/`.
- You can use Claude Code CLI for multi-file changes safely.
- You can manage context window usage consciously.
- You can generate tests and documentation with Claude at expert level.

---

## 3. Advanced Engineering Path

These are the leverage multipliers.

| Order | File | What It Builds |
|---:|---|---|
| 14 | `03-Advanced-Engineering/Subagents-Deep-Dive-Gold-Sheet.md` | What subagents are, context isolation, when to use them, subagent design patterns, 7 specialist subagents |
| 15 | `03-Advanced-Engineering/Skills-System-Gold-Sheet.md` | SKILL.md anatomy, automatic invocation, when skills fire, building reusable capability modules |
| 16 | `03-Advanced-Engineering/Hooks-Lifecycle-Gold-Sheet.md` | pre_tool_use, post_tool_use, on_error hooks, bash scripting, validation patterns, dangerous command blocking |
| 17 | `03-Advanced-Engineering/MCP-Integration-Gold-Sheet.md` | MCP protocol, server configuration, filesystem/GitHub/database/browser servers, security rules |
| 18 | `03-Advanced-Engineering/Token-Context-Optimization-Gold-Sheet.md` | Token mindset, model selection, high-signal vs low-signal prompts, scoping, conciseness forcing |
| 19 | `03-Advanced-Engineering/Agent-Loops-Gold-Sheet.md` | Loop engineering, plan→execute→verify cycles, stopping conditions, guardrails, autonomous iteration |
| 20 | `03-Advanced-Engineering/Multi-Agent-Orchestration-Gold-Sheet.md` | Planner→Builder→Tester→Reviewer pipeline, context handoff, coordination patterns, failure recovery |

Advanced target:
- You have working subagent definitions for debugging, testing, and documentation.
- You have SKILL.md files that fire automatically for test and refactor tasks.
- You have at least pre_tool_use and post_tool_use hooks validating tool calls.
- You understand MCP and can configure a local MCP server safely.
- You can design and run a multi-agent pipeline end to end.

---

## 4. Pro / Production Level Path

These are the professional operating system patterns.

| Order | File | What It Builds |
|---:|---|---|
| 21 | `04-Pro-MAANG-Level/Personal-Claude-Operating-System-Gold-Sheet.md` | Daily rituals, personal CLAUDE.md OS, session planning, end-of-day capture, maintenance schedule |
| 22 | `04-Pro-MAANG-Level/SDLC-Automation-Gold-Sheet.md` | Claude across all 12 SDLC phases: requirements → feature → test → review → release → incident |
| 23 | `04-Pro-MAANG-Level/Debugging-Claude-Handbook-Gold-Sheet.md` | 20 Claude failure modes with symptom, root cause, diagnosis, fix, prevention |
| 24 | `04-Pro-MAANG-Level/Verification-Driven-Workflows-Gold-Sheet.md` | Test-first, build checks, lint loops, validation gates, Claude iterates until green |
| 25 | `04-Pro-MAANG-Level/Autonomous-Workflows-Gold-Sheet.md` | Full autonomous pipelines, safe autonomy principles, guardrails, recovery patterns |

Pro target:
- You operate a personal Claude OS with daily rituals and a reusable configuration library.
- You can apply Claude at every stage of a real SDLC workflow.
- You can diagnose any Claude failure mode from the debugging handbook.
- You design and run verification loops where Claude iterates until tests pass.
- You can build and run safe autonomous workflows on real codebases.

---

## 5. Scenario Practice Path

Use these after the concept sheets. They train fast decisions under real workflow pressure.

| Order | File | What It Builds |
|---:|---|---|
| 26 | `05-Scenario-Practice/Daily-Workflow-Scenarios-Gold-Sheet.md` | Morning planning, coding session, pre-commit, end-of-day rituals |
| 27 | `05-Scenario-Practice/Feature-Building-Scenarios-Gold-Sheet.md` | Full feature delivery: plan → implement → test → review |
| 28 | `05-Scenario-Practice/Debugging-Scenarios-Gold-Sheet.md` | Error diagnosis, stack trace analysis, production incident support |
| 29 | `05-Scenario-Practice/Code-Review-Scenarios-Gold-Sheet.md` | Security, test coverage, maintainability, architecture review |

Scenario target:
- You can pick the right Claude workflow instantly for any development task.
- You can run a complete feature delivery cycle with Claude.
- You can debug effectively without sharing sensitive production data.
- You can conduct a thorough code review in under 10 minutes.

---

## 6. Practice Upgrade Path

Convert passive reading into active skill.

| Order | File | What It Builds |
|---:|---|---|
| 30 | `06-Practice-Upgrade/Claude-Active-Recall-Question-Bank.md` | 50 applied questions mapped to every sheet |
| 31 | `06-Practice-Upgrade/Claude-Slash-Command-Library.md` | Ready-to-use slash command templates for every major workflow |
| 32 | `06-Practice-Upgrade/Claude-Mock-Workflow-Scripts.md` | Timed exercises: planning, debugging, refactoring, review |
| 33 | `06-Practice-Upgrade/Claude-Scoring-Rubrics.md` | 1-5 scoring rubrics for prompt quality, CLAUDE.md quality, workflow maturity |
| 34 | `06-Practice-Upgrade/Claude-4-Week-Mastery-Roadmap.md` | Day-by-day plan from beginner to pro operating level |

---

## 7. Configuration Reference Library

Copy-paste-ready Claude configuration files.

```
config/
  CLAUDE.md                           ← root CLAUDE.md template (customize per project)
  subfolder-CLAUDE.md                 ← subfolder override template

  .claude/
    commands/                         ← slash commands (copy to your project's .claude/commands/)
      explain.cmd.md                  ← /explain
      debug.cmd.md                    ← /debug
      refactor.cmd.md                 ← /refactor
      test.cmd.md                     ← /test
      review.cmd.md                   ← /review
      plan.cmd.md                     ← /plan
      optimize.cmd.md                 ← /optimize
      build.cmd.md                    ← /build
      token-efficient.cmd.md          ← /token-efficient

    agents/                           ← subagent definitions
      planner.agent.md                ← @planner: session planning specialist
      builder.agent.md                ← @builder: implementation specialist
      debugger.agent.md               ← @debugger: root cause analysis
      tester.agent.md                 ← @tester: test generation specialist
      reviewer.agent.md               ← @reviewer: code review specialist
      architect.agent.md              ← @architect: architecture advisor
      optimizer.agent.md              ← @optimizer: performance specialist

    skills/
      testing/SKILL.md                ← auto-invoked for test generation tasks
      refactoring/SKILL.md            ← auto-invoked for refactoring tasks
      documentation/SKILL.md          ← auto-invoked for documentation tasks
      performance/SKILL.md            ← auto-invoked for performance tasks

    hooks/
      pre_tool_use.sh                 ← validates every tool call before execution
      post_tool_use.sh                ← validates tool output after execution
      on_error.sh                     ← error handling and recovery actions
```

---

## 8. What A Pro-Level Claude User Masters

### Mental Model and Setup
- Knows what Claude excels at vs ChatGPT vs Copilot
- Claude Code CLI installed and operational
- CLAUDE.md in every active project, loaded and verified

### Configuration
- Root CLAUDE.md with project context, architecture rules, and forbidden patterns
- Subfolder CLAUDE.md for domain-specific overrides
- Slash commands for every repeated workflow
- Subagents for each specialist task type
- Skills that fire automatically
- Hooks that validate before and after every tool use

### Workflows
- Plan-first, execute-second for every non-trivial task
- Verification loops (run tests → read output → iterate → pass)
- Checkpoint commits before every agent session
- Token-efficient prompts by habit
- Context scoping (relevant files only, not entire repo)

### Daily Operation
- Morning planning ritual using /plan
- Pre-commit review using /review
- End-of-day capture in session notes
- Weekly CLAUDE.md and slash command review
