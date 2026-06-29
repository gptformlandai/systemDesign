# GitHub Copilot Mastery Track Index

This folder is the GitHub Copilot mastery track — from first-time user to professional
AI-assisted developer operating at MAANG-level workflow discipline.

Audience:
- You are a software developer (any language background).
- You may have used Copilot autocomplete but do not yet feel fluent in Chat, Agent Mode,
  custom instructions, prompt files, or custom agents.
- You want to build a personal "Copilot Operating System" for daily development work.

Goal:
- Master every Copilot surface: inline suggestions, Chat, Edits, Agent Mode, Code Review,
  prompt files, custom instructions, custom agents, MCP, and GitHub Actions integration.
- Build reusable prompt libraries, instruction files, and agent definitions.
- Develop a disciplined, token-efficient, safe daily workflow.
- Reach the level where Copilot accelerates every stage of the SDLC.

Use this index as the reading and practice order.

---

## How To Read This Track

Before anything else, accept these five reframes:

### 1. Copilot is a context machine, not a mind reader

Copilot responds to exactly what it sees: the files you have open, the text you select,
the instructions in your `.github/copilot-instructions.md`, and the words in your prompt.
It does not know your architecture, your team conventions, or your intent unless you
tell it explicitly. The quality of output is the quality of your context + your prompt.

### 2. Modes are different tools for different jobs

Inline autocomplete, Chat Ask mode, Chat Edit mode, and Agent Mode are not interchangeable.
Using Agent Mode for a simple question wastes tokens and time. Using Chat Ask mode for
a multi-file refactor produces half-applied suggestions you must manually track.
Choosing the right mode is the first decision of every Copilot interaction.

### 3. Custom instructions are not magic — they need curation

Dumping 2000 words of conventions into `copilot-instructions.md` does not guarantee
Copilot follows them. Instructions need to be short, concrete, and testable.
The shorter the instruction, the more likely it is followed.

### 4. Token efficiency is a discipline, not an afterthought

Every file you open, every line of context you add, and every vague prompt you write
costs tokens. In large codebases, poor context management means Copilot reads irrelevant
files and produces irrelevant answers. Learning to give Copilot the minimum necessary
context to answer correctly is a core professional skill.

### 5. Review everything — always

Copilot can generate code that compiles, passes linting, and still contains logical bugs,
security vulnerabilities, outdated API usage, or hallucinated library methods. No generated
code is correct by definition. Every Copilot output is a first draft, not a final answer.

---

## Practical Impact Meter — Legend

Sheets use this rating to show how much each topic affects daily developer productivity:

```
★★★★★ — Use this daily; missing it costs hours per week
★★★★☆ — Core professional skill; most Copilot power users need this
★★★☆☆ — Advanced technique; adds leverage at senior level
★★☆☆☆ — Situational; useful in specific workflows
★☆☆☆☆ — Niche; know it exists but rarely needed
```

---

## 1. Foundations Path

Read these first. They build Copilot intuition from zero.

| Order | File | What It Builds |
|---:|---|---|
| 1 | `01-Foundations/Copilot-Mental-Model-Gold-Sheet.md` | What Copilot is, how it sees context, difference between all modes, when to use each surface |
| 2 | `01-Foundations/GitHub-Copilot-Setup-Personal-Machine-Gold-Sheet.md` | VS Code setup, extension install, account sign-in, plan verification, troubleshooting auth and network |
| 3 | `01-Foundations/Copilot-Inline-Suggestions-Gold-Sheet.md` | How autocomplete works, how to guide it with comments, tab/reject patterns, ghost text control |
| 4 | `01-Foundations/Copilot-Chat-Fundamentals-Gold-Sheet.md` | Chat panel vs inline chat, Ask mode, context variables `#file` `#selection` `#codebase`, slash commands |
| 5 | `01-Foundations/Safe-Prompting-Principles-Gold-Sheet.md` | No secrets rule, synthetic data, review discipline, small changes, source control safety net |
| 6 | `01-Foundations/Copilot-For-Beginners-Quick-Wins-Gold-Sheet.md` | First 10 high-ROI use cases: explain, fix error, generate test, write docstring, create README |

Foundations target:
- You understand how Copilot processes context and generates output.
- You have Copilot installed and verified working on your machine.
- You can guide inline suggestions with strategic comments.
- You can use Chat effectively with context variables.
- You never paste secrets into Copilot.
- You have completed at least 5 quick-win exercises.

---

## 2. Intermediate Power User Path

After foundations, read these.

| Order | File | What It Builds |
|---:|---|---|
| 7 | `02-Intermediate-Power-User/Custom-Instructions-Deep-Dive-Gold-Sheet.md` | `copilot-instructions.md` structure, path-specific instructions, instruction design principles, bad vs good examples |
| 8 | `02-Intermediate-Power-User/Prompt-Files-Slash-Commands-Gold-Sheet.md` | YAML frontmatter, prompt file anatomy, creating reusable slash commands, prompt library management |
| 9 | `02-Intermediate-Power-User/Copilot-Edits-Mode-Gold-Sheet.md` | Edits vs Chat Ask, multi-file edits, working set management, diff review, accepting/rejecting changes |
| 10 | `02-Intermediate-Power-User/Agent-Mode-Safe-Usage-Gold-Sheet.md` | Agent Mode mechanics, planning before coding, multi-file safety, recovery from bad changes, task templates |
| 11 | `02-Intermediate-Power-User/Copilot-For-Testing-Gold-Sheet.md` | Unit test generation, test gap analysis, edge case generation, mocking strategy, per-language patterns |
| 12 | `02-Intermediate-Power-User/Copilot-For-CI-GitHub-Actions-Gold-Sheet.md` | GitHub Actions instruction file, workflow generation prompt, debugging broken workflows, PR gate examples |
| 13 | `02-Intermediate-Power-User/Copilot-For-PR-Review-Gold-Sheet.md` | PR review specialist prompt, correctness/security/test coverage checklist, PR summary generation |
| 13a | `02-Intermediate-Power-User/Copilot-For-Documentation-Gold-Sheet.md` | README generation, docstrings (Python/JS/Java), API docs from code, ADR generation, release notes, onboarding docs |

Intermediate target:
- You have working `copilot-instructions.md` for at least one project.
- You have created at least 3 reusable prompt files.
- You can use Edits mode for multi-file changes and review diffs confidently.
- You can run Agent Mode safely with a planning step and recovery procedure.
- You can generate tests and analyze test gaps with Copilot.
- You can create and debug GitHub Actions workflows with Copilot.
- You can generate documentation (README, docstrings, API docs, ADRs) using structured prompts.

---

## 3. Advanced Engineering Path

These are the leverage multipliers.

| Order | File | What It Builds |
|---:|---|---|
| 14 | `03-Advanced-Engineering/Custom-Agents-Deep-Dive-Gold-Sheet.md` | `.agent.md` anatomy, agent purpose/boundaries/tools, 19 specialist agents, chaining strategy |
| 15 | `03-Advanced-Engineering/AGENTS-MD-Strategy-Gold-Sheet.md` | Root vs folder vs domain `AGENTS.md`, multi-AI-tool compatibility, portable workspace strategy |
| 16 | `03-Advanced-Engineering/Context-Engineering-Gold-Sheet.md` | What Copilot sees, context window mechanics, file selection strategy, context variables deep dive |
| 17 | `03-Advanced-Engineering/Token-Optimization-Gold-Sheet.md` | Token cost mindset, compact prompt patterns, model selection, bad vs optimized prompt examples |
| 18 | `03-Advanced-Engineering/MCP-Integration-Copilot-Gold-Sheet.md` | What MCP is, MCP server use cases, `.vscode/mcp.json` config, security risks, least privilege |
| 19 | `03-Advanced-Engineering/Copilot-For-Architecture-And-Prompt-Library-Gold-Sheet.md` | Architecture review prompt, ADR generation, system design helper, prompt library management |
| 19a | `03-Advanced-Engineering/Optional-M365-AgentBuilder-Studio-Gold-Sheet.md` | M365 Copilot productivity workflows, Copilot Chat Agent Builder concepts, Copilot Studio overview — **OPTIONAL** |

Advanced target:
- You have built at least 3 custom agents with clear scopes and boundaries.
- You have an `AGENTS.md` or `copilot-instructions.md` strategy across a project.
- You can diagnose context window issues and fix them with targeted file selection.
- You can write token-efficient prompts by habit.
- You understand MCP and can configure a local MCP server safely.
- You can run a Copilot-assisted architecture review end to end.
- You know which optional Copilot products exist and when to use them (M365, Agent Builder, Studio).

---

## 4. Pro / Production Level Path

These are the professional operating system patterns.

| Order | File | What It Builds |
|---:|---|---|
| 21 | `04-Pro-MAANG-Level/Personal-Copilot-Operating-System-Gold-Sheet.md` | Personal workspace system: daily prompts, context files, prompt libraries, workflow rituals |
| 22 | `04-Pro-MAANG-Level/SDLC-Automation-With-Copilot-Gold-Sheet.md` | Copilot across full SDLC: requirements → feature → test → review → release → incident |
| 23 | `04-Pro-MAANG-Level/Copilot-Debugging-Handbook-Gold-Sheet.md` | 25 Copilot failure modes with symptom, cause, diagnosis, fix, and prevention pattern |
| 24 | `04-Pro-MAANG-Level/Advanced-Context-Engineering-Gold-Sheet.md` | Project context files, plan-first pattern, chunk strategy, assumption listing, reuse over regen |
| 25 | `04-Pro-MAANG-Level/Agent-Governance-Output-Evaluation-Gold-Sheet.md` | Output scoring rubrics, hallucination detection, agent quality gates, team-level governance |
| 26 | `04-Pro-MAANG-Level/Team-Ready-Instructions-Gold-Sheet.md` | Monorepo strategy, multi-language instructions, team instruction onboarding, conflict resolution |
| 27 | `04-Pro-MAANG-Level/Responsible-AI-Safe-Usage-Gold-Sheet.md` | Privacy principles, no-secrets discipline, code review requirements, dependency safety, data handling |

Pro target:
- You operate a personal Copilot system with daily rituals and a reusable prompt library.
- You can apply Copilot at every stage of a real SDLC workflow.
- You can diagnose any Copilot failure mode from the debugging handbook.
- You can evaluate Copilot output quality before accepting it.
- You can set up team-ready instructions for a multi-developer project.
- You apply responsible AI principles as non-negotiable defaults.

---

## 5. Scenario Practice Path

Use these after the concept sheets. They train fast decisions under real workflow pressure.

| Order | File | What It Builds |
|---:|---|---|
| 28 | `05-Scenario-Practice/Copilot-Daily-Workflow-Scenarios-Gold-Sheet.md` | Morning planning, coding session, pre-PR, end-of-day scenarios |
| 29 | `05-Scenario-Practice/Feature-Building-Scenarios-Gold-Sheet.md` | Feature planning, codebase exploration, implementation, test + review cycle |
| 30 | `05-Scenario-Practice/Debugging-With-Copilot-Scenarios-Gold-Sheet.md` | Error diagnosis, stack trace analysis, production incident support |
| 31 | `05-Scenario-Practice/Code-Review-Scenarios-Gold-Sheet.md` | PR review prompts, security checks, test coverage gaps, maintainability assessment |

Scenario target:
- You can pick the right Copilot mode instantly for any development task.
- You can debug using Copilot without sharing sensitive production data.
- You can run a complete feature delivery cycle with Copilot support.
- You can conduct a thorough code review using Copilot in under 10 minutes.

---

## 6. Practice Upgrade Path

Use these alongside and after the concept sheets. They convert passive reading into active skill.

| Order | File | What It Builds |
|---:|---|---|
| 32 | `06-Practice-Upgrade/Copilot-Active-Recall-Question-Bank.md` | Topic-by-topic recall questions mapped to every Copilot sheet |
| 33 | `06-Practice-Upgrade/Copilot-Prompt-Library-Templates.md` | Ready-to-use prompt templates for every major development workflow |
| 34 | `06-Practice-Upgrade/Copilot-Mock-Workflow-Scripts.md` | Timed scenario exercises for coding sessions, debugging, review, and architecture |
| 35 | `06-Practice-Upgrade/Copilot-Scoring-Rubrics.md` | 1-5 scoring rubrics for prompt quality, context efficiency, output review, and workflow maturity |
| 36 | `06-Practice-Upgrade/Copilot-4-Week-Mastery-Roadmap.md` | Realistic 4-week plan from beginner to pro operating level |

Practice target:
- You can answer questions about Copilot modes, context, and instructions from memory.
- You have a personal prompt library of at least 10 reusable prompts.
- You can score your own Copilot usage honestly and improve weak areas.
- You complete the 4-week roadmap and can articulate your Copilot operating system.

---

## 7. Config Reference Library

Reusable, copy-paste-ready configuration files.

```
config/
  copilot-instructions-template.md       — root-level instruction template (customize for any project)
  mcp.example.json                       — MCP server config template (no real secrets — gitignore mcp.json)
  vscode-settings.json                   — recommended VS Code settings for Copilot

  instructions/                          — copy to .github/instructions/ in your project
    python.instructions.md               — Python 3.11+ path-specific rules
    testing.instructions.md              — test generation rules (all languages)
    security.instructions.md             — OWASP-aligned security rules (applies everywhere)
    github-actions.instructions.md       — CI/CD workflow rules

  prompts/                               — copy to .github/prompts/ in your project
    explain-code.prompt.md               — /explain-code
    debug-error.prompt.md                — /debug-error
    generate-tests.prompt.md             — /generate-tests
    refactor-code.prompt.md              — /refactor-code (from Practice Upgrade templates)
    security-review.prompt.md            — /security-review
    architecture-review.prompt.md        — /architecture-review
    write-pr-description.prompt.md       — /write-pr-description (from Practice Upgrade templates)
    generate-learning-notes.prompt.md    — /generate-learning-notes (from Practice Upgrade templates)
    create-github-action.prompt.md       — /create-github-action

  agents/                                — copy to .github/agents/ in your project
    codebase-navigator.agent.md          — @codebase-navigator: codebase exploration
    debugging-tutor.agent.md             — @debugging-tutor: root cause analysis
    test-engineer.agent.md               — @test-engineer: test generation + gap analysis
    security-reviewer.agent.md           — @security-reviewer: OWASP-aligned security review
    architecture-advisor.agent.md        — @architecture-advisor: SOLID + coupling + scalability
```

---

## 8. What A Pro-Level Copilot User Masters

### Modes and Surfaces

- Knows when to use inline, Chat Ask, Chat Edit, Agent Mode, and Code Review.
- Never uses Agent Mode when a simple Ask prompt suffices.
- Knows how to scope a Chat conversation to avoid context bleed.

### Instructions and Agents

- Has working `copilot-instructions.md` in every project they own.
- Has path-specific instruction files for language and domain contexts.
- Has at least 3 custom agents with clear scopes.
- Keeps instructions under 500 words total per file.

### Prompt Engineering

- Uses `#file`, `#selection`, `#codebase`, and `#sym` context variables fluently.
- Writes prompts with: context → goal → constraints → output format.
- Never writes vague prompts like "help me with this code".
- Has a prompt library and adds to it weekly.

### Token and Context Discipline

- Opens only relevant files before sending a prompt.
- Summarizes large codebases before asking implementation questions.
- Asks for diffs instead of full rewrites for targeted changes.
- Chooses faster/cheaper models for simple tasks.

### Safety and Review

- Reviews every generated diff before accepting.
- Runs tests after every Copilot-assisted change.
- Never pastes credentials, keys, or PII into Copilot.
- Uses synthetic or anonymized data for debugging examples.
- Treats Copilot as a first-draft generator, not an authority.

### Daily Workflow

- Has a morning planning ritual using Copilot.
- Creates end-of-day learning notes.
- Maintains a prompt library that grows with experience.
- Reviews and prunes custom instructions quarterly.
