# Personal Claude Operating System — Gold Sheet

> **Track**: Claude Mastery Track — Group 4: Pro / Production Level
> **File**: 1 of 5 (Track File #21)
> **Read after**: Multi-Agent-Orchestration-Gold-Sheet.md

---

## 1. What a Personal Claude OS Is

```
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
```

---

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
```

---

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
```

---

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
```

---

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
```

---

## 6. Revision Checklist

- [ ] Has a personal Claude OS repository with all 5 components configured
- [ ] Follows the morning planning ritual daily
- [ ] Runs pre-commit review before every commit
- [ ] Writes end-of-day session notes 3+ times per week
- [ ] Runs the weekly maintenance ritual
- [ ] Can identify all 6 signs of a broken Claude OS
- [ ] Has a slash command library of 8+ validated, frequently-used commands
