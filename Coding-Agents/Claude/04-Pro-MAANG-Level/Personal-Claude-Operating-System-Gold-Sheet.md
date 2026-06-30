# Personal Claude Operating System — Gold Sheet

> **Track**: Claude Mastery Track — Group 4: Pro / Production Level
> **File**: 1 of 5 (Track File #21)
> **Read after**: Debugging-Claude-Handbook-Gold-Sheet.md

---

## 1. What a Personal Claude OS Is

```
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
```

---

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
```

---

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
```

---

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
```

---

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
```

---

## 9. Revision Checklist

- [ ] Has CLAUDE.md at personal (~/.claude/), project root, and at least one subdirectory
- [ ] Starts every session with the morning planning ritual (5 minutes)
- [ ] Runs the pre-commit protocol before every commit (5 minutes)
- [ ] Writes end-of-day session notes (10 minutes, 3+ times per week)
- [ ] Does weekly system maintenance (15 minutes every Friday)
- [ ] Tracks 5 OS metrics (acceptance rate, session length, rework rate, quality, commands)
- [ ] Can self-assess Claude OS maturity level (1-5)
