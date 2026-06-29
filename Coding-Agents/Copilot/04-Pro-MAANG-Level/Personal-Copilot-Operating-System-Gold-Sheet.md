# Personal Copilot Operating System — Gold Sheet

> **Track**: Copilot Mastery Track — Group 4: Pro / Production Level
> **File**: 1 of 7 (Track File #21)
> **Audience**: Developers building their personal AI-assisted development system
> **Read after**: Copilot-For-Architecture-And-Prompt-Library-Gold-Sheet.md

---

## 1. What a Personal Copilot Operating System Is

```
A "Copilot OS" is the complete set of configurations, prompts, agents, rituals,
and habits that make AI assistance consistent, efficient, and safe in your daily work.

Components:
  1. Configuration layer  → copilot-instructions.md, AGENTS.md, mcp.json
  2. Prompt library       → reusable slash commands for daily workflows
  3. Agent library        → specialist agents for deep tasks
  4. Daily rituals        → morning planning, pre-PR review, end-of-day notes
  5. Quality gates        → checklists before accepting AI output
  6. Recovery patterns    → what to do when Copilot produces bad output

A developer with a Copilot OS:
  - Starts every session with clear context (not from scratch)
  - Has a prompt ready for every common task (not typing ad-hoc)
  - Knows which agent to invoke for which problem
  - Reviews all output before accepting
  - Captures learning in notes that grow their system over time

A developer without a Copilot OS:
  - Types the same prompt multiple times per week
  - Gets inconsistent output because context varies
  - Accepts output without checking
  - Starts from zero every time the topic changes
```

---

## 2. The Daily Copilot Ritual

### Morning Planning (10 minutes)

```
Prompt: "I'm starting my development session. Today I'm working on:
[paste your ticket/task description or personal goal]

Help me:
1. Break this task into clear implementation steps (3-7 steps max)
2. Identify which files in #codebase are most relevant
3. Identify potential blockers or questions I should answer before coding
4. Suggest which Copilot mode is best for each step (inline/Chat/Edits/Agent)

Do not start implementing. Planning only."
```

### During Coding — The Session Discipline

```
Rule 1: One task per Chat session (start new conversation per task)
Rule 2: Commit before Agent Mode runs
Rule 3: Write the comment/intent first, then let inline suggest
Rule 4: After each Copilot-assisted change: run the relevant tests
Rule 5: At 25% wrong output: stop, re-read, correct the prompt
       At 3 retries: switch mode or solve manually
```

### Pre-PR Checklist (15 minutes before every PR)

```
Step 1: Run security review prompt on changed files
Step 2: Run test gap analysis on new code
Step 3: Run full test suite: pytest tests/ -v
Step 4: Generate PR description with the write-pr-description prompt
Step 5: Run pre-PR self-review prompt
Step 6: Check CI passes (GitHub Actions)
```

### End-of-Day Learning Notes (10 minutes)

```
Prompt: "/generate-learning-notes
Topic: [the main concept or problem I encountered today]

Also capture:
- One prompt that worked really well today
- One prompt that didn't work — what I'll change next time
- One thing I learned about Copilot or the codebase"

Save the output to: notes/[YYYY-MM-DD]-session-notes.md
This becomes a searchable personal knowledge base.
```

---

## 3. Personal Workspace Repository Structure

```
my-copilot-os/                    (your personal private repo)
  README.md                       → how to use this system
  DAILY_WORKFLOW.md               → the ritual above
  
  .github/
    copilot-instructions.md       → personal-level rules (for all your projects)
    instructions/
      python.instructions.md
      testing.instructions.md
      security.instructions.md
      github-actions.instructions.md
    prompts/                      → your full prompt library
      explain-code.prompt.md
      debug-error.prompt.md
      generate-tests.prompt.md
      refactor-code.prompt.md
      architecture-review.prompt.md
      security-review.prompt.md
      generate-learning-notes.prompt.md
      write-pr-description.prompt.md
      [... all your prompts]
    agents/                       → your specialist agent library
      codebase-navigator.agent.md
      debugging-tutor.agent.md
      test-engineer.agent.md
      security-reviewer.agent.md
      [... all your agents]
  
  .vscode/
    settings.json                 → your standard VS Code settings
    extensions.json               → your recommended extensions
    mcp.example.json              → MCP config template (no secrets)
  
  notes/                          → daily session notes
    2024-01-15-session-notes.md
    2024-01-16-session-notes.md
    [...]
  
  checklists/
    pre-pr-checklist.md
    agent-mode-safety-checklist.md
    security-review-checklist.md
  
  templates/
    copilot-instructions-template.md   → for new project setup
    agents-md-template.md              → for new project AGENTS.md
```

---

## 4. Maintenance Rituals

### Weekly (10 minutes)

```
- Review notes from the past week: what patterns emerged?
- Add 1-2 new prompts to the library based on repeating tasks
- Update copilot-instructions.md if a new project convention was established
- Delete prompts that haven't been used in 30+ days
```

### Monthly (30 minutes)

```
- Review all instruction files — are they still accurate?
- Review all agents — do they still have the right scope?
- Update the project context file for major projects
- Rotate MCP tokens (set 90-day expiry)
- Run scoring rubric self-assessment (from Practice Upgrade)
```

### Quarterly (1 hour)

```
- Full prompt library audit: categorize, prune, improve
- Review Copilot product changelog for new features
- Update this setup guide if new Copilot capabilities are available
- Share useful prompts or agents with team members
```

---

## 5. Anti-Patterns of a Broken System

```
Sign 1 — You retype the same prompt every day:
  Fix: Create a prompt file. If you typed it 3 times, it belongs in .github/prompts/

Sign 2 — Copilot surprises you with a convention violation:
  Fix: Add that convention to copilot-instructions.md immediately.

Sign 3 — You haven't committed before Agent Mode and it went wrong:
  Fix: Make "commit before Agent Mode" a physical habit — muscle memory.

Sign 4 — Your prompt library has 40 prompts but you only use 5:
  Fix: Prune. Quality over quantity. 10 great prompts > 40 mediocre ones.

Sign 5 — You have no daily notes and can't remember what you did last week:
  Fix: Start the end-of-day note ritual. Even 5 minutes compounds massively.
```

---

## 6. Revision Checklist

- [ ] Has a personal Copilot OS repository with prompts, agents, instructions
- [ ] Follows the morning planning ritual before every coding session
- [ ] Runs the pre-PR checklist before every PR
- [ ] Writes end-of-day learning notes at least 3x per week
- [ ] Follows the weekly/monthly/quarterly maintenance schedule
- [ ] Can identify the 5 signs of a broken Copilot system
- [ ] Has a prompt library of 10+ validated, frequently-used prompts
