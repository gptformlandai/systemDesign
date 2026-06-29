# Claude Daily Checklist

> Keep this open every day. Work through it top to bottom.
> This is your personal Claude Operating System ritual.

---

## ☀️ Morning Setup (10 minutes)

### Step 1 — Open Your Workspace (2 min)
```
[ ] Open your project in Claude Code or editor
[ ] Verify CLAUDE.md is present: cat CLAUDE.md | head -20
[ ] Check last session's state:
    git log --oneline -5   (what was done yesterday?)
    git status             (anything uncommitted?)
[ ] Close all unrelated files — Claude reads your open context
```

### Step 2 — Morning Planning with /plan (5 min)
```
Open a new Claude Code session or Chat.
Run /plan or paste this:

"Today I'm working on: [paste your ticket, goal, or task]

Help me:
1. Break into implementation steps (3-7, ordered by dependency)
2. Identify which files are most relevant (#codebase scan)
3. Flag any blocker or unknown I should resolve BEFORE coding
4. Suggest whether to use a subagent for any step
5. Estimate: S/M/L for total session

Do not implement anything — planning only."

[ ] Plan reviewed and one incorrect assumption corrected
[ ] Relevant files identified
[ ] Checkpoint commit created if using agentic mode:
    git add . && git commit -m "checkpoint: starting [task]"
```

### Step 3 — Verify CLAUDE.md Is Loaded (1 min)
```
In Claude Code: "What project rules do you have loaded?"
Expected: Claude summarizes your CLAUDE.md conventions.

If generic response:
  → Check: CLAUDE.md is at repo root (not in a subfolder)
  → Reload the session
[ ] CLAUDE.md confirmed loaded
```

---

## 💻 During Coding Session

### Every Prompt You Write
```
[ ] Is it anchored? (references a specific file, not a description)
[ ] Is it bounded? ("only this function", "only these files")
[ ] Is it constrained? (what must NOT change)
[ ] Does it specify output format? (diff / list / code / prose)
[ ] Is it under 100 words for standard tasks?
```

### Every File Edit Claude Makes
```
[ ] Read the diff before accepting: git diff
[ ] Run tests: [your test command]
[ ] Check: no unexpected imports, no deleted error handling
[ ] Check: no hardcoded credentials in any added line
```

### Every Agent Session
```
[ ] Checkpoint commit first:
    git add . && git commit -m "checkpoint: [task name]"
[ ] Session starts with: "Plan first — list files to change. Wait for approval."
[ ] Watch for scope creep (files touched you didn't list)
[ ] Stop session immediately if wrong direction detected
```

### Verification Loop (for builds and tests)
```
The verification loop:
  1. Claude generates code
  2. Run: [test command]
  3. If tests fail → paste output to Claude: "Fix the failing tests: [output]"
  4. Repeat until: all tests pass
  
[ ] Never accept code until tests pass
[ ] Never skip the verification step
```

---

## 🔐 Before Every Commit (15 minutes)

### Security Check
```
[ ] Run /review on changed files
[ ] Check: no hardcoded API keys, tokens, or passwords
[ ] Check: no real customer data in examples or tests
[ ] Check: SQL queries parameterized (no string concatenation)
[ ] Scan diff: git diff --staged | grep -i "key\|secret\|password\|token"
```

### Test Quality
```
[ ] /test run — all tests pass
[ ] Test gap analysis: any new code without tests?
[ ] Run linting: [your lint command]
[ ] Run type check: [your type check command] (if applicable)
```

### Code Review
```
[ ] /review run on staged changes
[ ] Is there anything you can't explain? Understand it before committing.
[ ] No over-engineering introduced by Claude (abstractions not needed)
```

### Commit
```
[ ] Run /plan or describe the commit in conventional format
[ ] git add -p (interactive staging — read each hunk)
[ ] git commit -m "type(scope): description"
[ ] Did Claude write something you can't explain? Stop and understand it first.
```

---

## 🌙 End of Day (10 minutes)

### Capture Learning
```
"Generate session notes for today:
Topic: [main thing I worked on]
What worked: [best prompt or workflow]
What didn't: [failure + what to change]
Tomorrow: [next step]

Format: 5 bullets max. Under 150 words."

[ ] Notes saved to: notes/[YYYY-MM-DD]-session.md
```

### Improve the Command Library
```
[ ] Did I type the same prompt > 2 times today?
    → Create a slash command for it in .claude/commands/
[ ] Did a prompt produce unusually good output?
    → Add it to the library
[ ] Did a prompt fail badly?
    → Note what was wrong; create a better version
```

### Clean Up
```
[ ] Commit today's work (if not already done)
[ ] Update CLAUDE.md if any new conventions were established today
[ ] Write the "resume" pattern for tomorrow:
    "Yesterday: implemented [X]. Done: [A, B]. Next: [C]. Constraint: [rule]."
```

---

## 📅 Weekly Rituals (Friday, 15 minutes)

```
[ ] Review session notes from the week — patterns?
[ ] Add 1-2 slash commands based on repeat prompts
[ ] Audit CLAUDE.md — still accurate? Stale rules?
[ ] Check subagent files — scope still correct?
[ ] Review hooks — any new validations needed?
[ ] Run self-assessment using Claude-Scoring-Rubrics.md
[ ] Share one slash command or SKILL.md with a teammate (if team context)
```

---

## 🚨 When Claude Goes Wrong — Quick Recovery

```
Bad code generated:
  → git checkout .   (restore from checkpoint commit)
  → Restart with tighter constraints

Claude ignoring CLAUDE.md:
  → Ask: "What project rules do you have?"
  → Check CLAUDE.md is at repo root
  → Reload session

Context drift (Claude forgets earlier decisions):
  → Start a new session (context is now clean)
  → Paste resume pattern at top of new session

Claude over-engineering:
  → "Stop. Simpler. The existing pattern in [file] is sufficient."
  → Add anti-pattern to CLAUDE.md: "Do NOT add new abstractions..."

Agent loop going wrong:
  → Stop immediately
  → git checkout .
  → Re-scope the task into smaller pieces

Hook blocking a legitimate command:
  → Read the hook output to understand the rule
  → Update pre_tool_use.sh with an exception
  → Re-run the task
```

---

## 📊 Daily Score (quick self-check)

Rate yourself 1-5 at end of day:

| Habit | Score |
|---|---|
| Used specific file references (not descriptions) | /5 |
| Read all diffs before accepting | /5 |
| No secrets pasted into Claude | /5 |
| Ran tests after every Claude-assisted change | /5 |
| Session notes written | /5 |
| Checkpoint commits used before agent sessions | /5 |

**Target**: 25+/30 consistently = Pro-level Claude habits
