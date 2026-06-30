# Personal Claude Operating System — Gold Sheet

> **Track**: Claude Mastery Track — Group 4: Pro / Production Level
> **File**: 1 of 5 (Track File #21)
<<<<<<< HEAD
> **Read after**: Debugging-Claude-Handbook-Gold-Sheet.md
=======
> **Read after**: Multi-Agent-Orchestration-Gold-Sheet.md
>>>>>>> refs/remotes/origin/main

---

## 1. What a Personal Claude OS Is

```
<<<<<<< HEAD
A "Claude OS" is the complete system that makes Claude useful by default —
not just when you write a perfect prompt.

Components:
  1. CLAUDE.md stack    → persistent project memory (no repeated context)
  2. Command library    → reusable workflows (.claude/commands/*.cmd.md)
  3. Agent library      → specialist personas (.claude/agents/*.agent.md)
  4. Skills system      → automatic workflows (.claude/skills/*/SKILL.md)
  5. Hooks              → safety validation (.claude/hooks/*.sh)
  6. Daily rituals      → consistent habits that compound over time
  7. Session notes      → searchable personal knowledge base
  8. Metrics tracking   → measuring whether Claude is actually helping

Without a Claude OS:
  Each session: start from scratch, repeat context, forget constraints.
  Monthly result: mediocre output, high frustration.

With a Claude OS:
  Each session: Claude knows your project from the start.
  Monthly result: autonomous feature delivery, consistent quality.
=======
A "Claude OS" is the complete set of configurations, prompts, agents, skills,
hooks, and habits that make Claude assistance consistent, efficient, and safe.

Components:
  1. Configuration layer  → CLAUDE.md, subfolder CLAUDE.md files
  2. Command library      → .claude/commands/ slash commands for daily tasks
  3. Agent library        → .claude/agents/ specialist agents for deep tasks
  4. Skills library       → .claude/skills/ auto-invoked capability modules
  5. Hook layer           → .claude/hooks/ validation before/after tool use
  6. Daily rituals        → morning planning, pre-commit review, end-of-day notes
  7. Recovery patterns    → what to do when Claude produces wrong output

A developer with a Claude OS:
  - Starts every session with Claude already knowing the project context
  - Has a slash command for every repeated workflow
  - Has agents that specialize in debugging, testing, reviewing
  - Reviews all output before committing
  - Captures learning in session notes that grow their system over time

Without a Claude OS:
  - Types the same context every session
  - Types the same prompt for the same task repeatedly
  - Accepts output without structured review
  - Loses context between sessions
>>>>>>> refs/remotes/origin/main
```

---

<<<<<<< HEAD
## 2. The CLAUDE.md Stack

### Three Levels, Three Purposes

```
~/.claude/CLAUDE.md (personal defaults — all projects):
  My development style
  My preferred libraries (per language)
  My output format preferences
  My universal "Do NOT" rules

[project root]/CLAUDE.md (project memory):
  Tech stack (specific versions)
  Architecture rules
  Project-specific conventions
  Current state (updated weekly)

[subdirectory]/CLAUDE.md (domain overrides):
  Test-specific rules (tests/ directory)
  Infrastructure rules (infra/ directory)
  API-layer rules (src/api/ directory)

Verification that all levels load correctly:
  claude "What three levels of CLAUDE.md rules do you have loaded?
  List them by source: personal defaults, project, and any subdirectory."
```

---

## 3. Morning Planning Ritual

```
Every morning before touching code — takes 5 minutes, saves 30:

Session opener (paste this to start every coding session):
"Good morning session start.

Resume context:
  Project: [project name]
  Last session: [what you were working on]
  State: [what's done, what's in progress]
  Today's task: [specific task from your task tracker]
  
Plan this task:
1. Implementation steps (ordered by dependency)
2. Files in scope (exact paths)
3. Which agent handles which step
4. Success criteria (testable statement)

Do not implement. Planning only."

Why this works:
  Starts the session in planning mode (not just coding mode)
  Forces you to state success criteria before touching code
  Gives Claude the context it needs for the whole session in 100 words
```

---

## 4. The Pre-Commit Protocol

```
Before every commit — runs in 5 minutes:

1. Review the diff:
   claude "Review the staged diff (git diff --staged).
   Check: any secrets, any test modifications, any unexpected scope creep.
   Report: what changed and whether it matches my stated goal."

2. Security check:
   claude "/review @file:[all changed files]
   Focus: CRITICAL and HIGH issues only. Under 150 words."

3. Commit message:
   claude "Generate a conventional commit message for these changes:
   git diff --staged --stat
   Format: type(scope): description (under 72 chars)"

4. Run tests:
   pytest tests/ -x -q   (or your test command)
   
5. Commit:
   git add -p   (interactive staging — read each hunk)
   git commit -m "[generated message]"
```

---

## 5. End-of-Day Capture

```
Takes 10 minutes. Compounds into a permanent knowledge base.

"Session capture for today:

What I built: [summary]
Decisions made: [key architectural or design choices]
What worked well: [prompt or pattern that was effective]
What didn't work: [failed approach and why]
Best prompt of today: [paste the prompt that worked best]

Format:
## [Date] — [Feature/Task Name]
### Built: [list]
### Decisions: [list]
### Worked: [1-2 sentences]
### Didn't Work: [1-2 sentences + why]
### Best Prompt: [paste]
### Tomorrow: [next step]
Under 200 words."

Save to: notes/[YYYY-MM-DD]-session.md
This file is searchable, version-controlled, and becomes your personal Claude playbook.
=======
## 2. The Daily Claude Ritual

### Morning Planning (10 minutes)

```
Command: /plan  (or run this prompt)

"I'm starting my development session. Today I'm working on:
[paste your ticket or describe your task]

Using @file:CLAUDE.md and @codebase:
1. Break this task into implementation steps (3-7 ordered steps)
2. Which files will I need to touch?
3. What risks or unknowns should I resolve before coding?
4. Which Claude mode is best for each step:
   CLI agent loop / subagent / slash command / manual?

Plan only — do not implement anything."
```

### During Coding — Session Discipline

```
Rule 1: One task per Claude session — start fresh for each new topic
Rule 2: git commit before any multi-file agent session
Rule 3: State constraints upfront — don't add them halfway through
Rule 4: After each Claude-assisted change: run the relevant tests
Rule 5: At 25% wrong output: stop, re-read context, improve the prompt
       At 3 retries with same approach: switch mode or solve manually
Rule 6: Never commit code you can't explain line by line
```

### Pre-Commit Review (10 minutes before every commit)

```
Step 1: Run /review on all changed files
Step 2: Run security review for any auth/SQL/input handling changes
Step 3: Run /test (or check that tests pass)
Step 4: Check: are there any secrets or PII in the diff? (git diff)
Step 5: Commit with a descriptive message

If any step finds issues: fix before committing.
```

### End-of-Day Notes (10 minutes)

```
"Generate structured learning notes for today's session.

Topic: [the main technical problem I worked on]

Also capture:
  - One Claude prompt that worked really well today
  - One Claude prompt that failed — what I'll change next time
  - One new slash command I should create based on today's repeated prompts
  - One CLAUDE.md rule I should add based on a convention violation

Format: markdown with headers, code examples where relevant."

Save to: notes/[YYYY-MM-DD]-session.md
>>>>>>> refs/remotes/origin/main
```

---

<<<<<<< HEAD
## 6. Weekly System Maintenance

```
Every Friday — 15 minutes:

1. CLAUDE.md audit:
   "Review my CLAUDE.md. Are all rules still accurate for this project?
   Flag any rules that seem stale or contradictory."

2. Command library review:
   ls .claude/commands/
   For each command: did I use it this week?
   Unused for 2 weeks: consider removing or improving.

3. Session notes review:
   "Read this week's session notes: @file:notes/[this week's files]
   What patterns do I keep encountering?
   What should I add to CLAUDE.md or create a new command for?"

4. Scoring self-assessment:
   Rate 1-5 for the week:
   - CLAUDE.md quality (does it reflect actual project state?)
   - Prompt quality (am I using CRISP + constraints consistently?)
   - Verification loops (am I letting Claude run tests, not doing it manually?)
   - Agent usage (did I use the right agent for each task type?)
=======
## 3. Personal Claude OS Repository Structure

```
my-claude-os/                    (your private configuration repo)
  README.md                      → how to use this OS
  DAILY_WORKFLOW.md              → the ritual above

  CLAUDE.md                      → root template (customize per project)
  subfolder-CLAUDE.md            → subfolder override template

  .claude/
    commands/                    → slash commands for all repeated workflows
      explain.cmd.md             → /explain: structured code explanation
      debug.cmd.md               → /debug: error diagnosis
      refactor.cmd.md            → /refactor: safe refactoring with tests
      test.cmd.md                → /test: test generation with gap analysis
      review.cmd.md              → /review: pre-commit review
      plan.cmd.md                → /plan: task planning
      optimize.cmd.md            → /optimize: performance review
      build.cmd.md               → /build: full build and verify loop
      security.cmd.md            → /security: OWASP-focused review

    agents/                      → specialist subagent definitions
      planner.agent.md           → @planner: implementation planning
      builder.agent.md           → @builder: focused implementation
      debugger.agent.md          → @debugger: root cause analysis
      tester.agent.md            → @tester: test generation and verification
      reviewer.agent.md          → @reviewer: code review and security
      architect.agent.md         → @architect: architecture advice
      optimizer.agent.md         → @optimizer: performance analysis

    skills/
      testing/SKILL.md           → auto-invoked for test tasks
      refactoring/SKILL.md       → auto-invoked for refactor tasks
      documentation/SKILL.md     → auto-invoked for doc tasks
      performance/SKILL.md       → auto-invoked for performance tasks

    hooks/
      pre_tool_use.sh            → blocks dangerous commands before execution
      post_tool_use.sh           → validates outputs after tool runs
      on_error.sh                → structured error capture

  notes/                         → daily session notes
    2024-01-15-session.md
    [...]

  templates/
    feature-plan.md              → handoff document template
    adr-template.md              → ADR template
>>>>>>> refs/remotes/origin/main
```

---

<<<<<<< HEAD
## 7. Personal OS Metrics

### Track These Weekly

```
These metrics reveal if your Claude OS is actually working:

1. First-pass acceptance rate (sessions where Claude's first output was accepted):
   High (>60%): CLAUDE.md + commands are well-tuned
   Low (<30%): CLAUDE.md needs better Do NOT rules or commands need work

2. Average session length (messages to complete a task):
   Decreasing: good. You're getting better at scoping.
   Increasing: check if CLAUDE.md is still accurate and sessions need chunking.

3. Rework rate (sessions where you had to restart because Claude went wrong):
   High (>20%): need better stopping conditions in agent sessions
   Low (<5%): verification loops and scoping are working

4. Self-scored output quality (1-5):
   Track weekly. Should trend upward as OS matures.

5. Commands used vs created (library growth):
   Adding 1-2 commands per week for the first month: healthy
   Never adding commands: not capturing repeated patterns
=======
## 4. Maintenance Rituals

### Weekly (15 minutes)

```
- Review session notes from the week: what patterns emerged?
- Add 1-2 new slash commands for repeated prompts
- Update CLAUDE.md if a new project convention was established
- Delete or improve commands that haven't been used in 30 days
- Check: did Claude violate any constraints this week? Add them to CLAUDE.md or hooks.
```

### Monthly (30 minutes)

```
- CLAUDE.md audit: are all rules still accurate and relevant?
- Agent audit: do agents still have the right scope and instructions?
- Skills audit: are skills firing correctly? Any new skills needed?
- Hook audit: are hooks catching the right violations?
- Rotate MCP tokens (set 90-day expiry)
- Run scoring rubric self-assessment
```

### Quarterly (1 hour)

```
- Full slash command library audit: categorize, prune, improve
- Review Claude release notes: any new features to adopt?
- Share useful prompts, agents, or CLAUDE.md patterns with team
- Update the OS setup guide if Claude's capabilities have changed
>>>>>>> refs/remotes/origin/main
```

---

<<<<<<< HEAD
## 8. Personal OS Maturity Model

```
Level 1 — Beginner: Uses Claude for ad-hoc questions and code snippets.
  No CLAUDE.md. No commands. No agents. No verification loops.
  
Level 2 — User: Has CLAUDE.md and a few commands.
  Uses Claude Code for simple feature generation.
  
Level 3 — Intermediate: Full command library + some agents.
  Uses verification loops. Checkpoints before agent sessions.
  Context-manages across sessions.

Level 4 — Advanced: Full 4-agent pipeline operational.
  Skills auto-invoke. Hooks validate tool calls.
  Session notes maintained. Weekly system review.

Level 5 — Expert: Autonomous feature delivery.
  Claude runs verification loops without prompting.
  New features ship with tests passing from first agent session.
  Claude OS generates learnings that improve CLAUDE.md monthly.
  
Target: Level 4 after the 4-week roadmap. Level 5 after 3 months of daily use.
=======
## 5. Anti-Patterns of a Broken System

```
Sign 1 — You type the same prompt more than twice a week:
  Fix: Create a slash command. If you typed it 3 times, it belongs in .claude/commands/

Sign 2 — Claude violates a convention and you notice after committing:
  Fix: Add that constraint to CLAUDE.md immediately. Or add a hook.

Sign 3 — You haven't committed before a multi-file agent session:
  Fix: Make "git commit before agent session" muscle memory.

Sign 4 — Your slash command library has 20 commands but you only use 4:
  Fix: Prune. 5 great commands > 20 unused ones.

Sign 5 — You have no session notes and can't remember what you built last week:
  Fix: Start the end-of-day ritual. Even 5 minutes compounds.

Sign 6 — Claude keeps asking for context you've given before:
  Fix: The context belongs in CLAUDE.md, not repeated in prompts.
>>>>>>> refs/remotes/origin/main
```

---

<<<<<<< HEAD
## 9. Revision Checklist

- [ ] Has CLAUDE.md at personal (~/.claude/), project root, and at least one subdirectory
- [ ] Starts every session with the morning planning ritual (5 minutes)
- [ ] Runs the pre-commit protocol before every commit (5 minutes)
- [ ] Writes end-of-day session notes (10 minutes, 3+ times per week)
- [ ] Does weekly system maintenance (15 minutes every Friday)
- [ ] Tracks 5 OS metrics (acceptance rate, session length, rework rate, quality, commands)
- [ ] Can self-assess Claude OS maturity level (1-5)
=======
## 6. Revision Checklist

- [ ] Has a personal Claude OS repository with all 5 components configured
- [ ] Follows the morning planning ritual daily
- [ ] Runs pre-commit review before every commit
- [ ] Writes end-of-day session notes 3+ times per week
- [ ] Runs the weekly maintenance ritual
- [ ] Can identify all 6 signs of a broken Claude OS
- [ ] Has a slash command library of 8+ validated, frequently-used commands
>>>>>>> refs/remotes/origin/main
