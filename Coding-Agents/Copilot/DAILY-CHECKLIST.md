# Copilot Daily Checklist

> Keep this open every day. Work through it top to bottom.
> Time estimates are targets — adjust to your session length.

---

## ☀️ Morning Setup (10 minutes)

### Step 1 — Open Your Workspace (2 min)
```
[ ] Open VS Code in your project folder
[ ] Verify Copilot is active (status bar icon, no ⚠)
[ ] Close all unrelated tabs from yesterday
[ ] Open only the files relevant to today's task
```

### Step 2 — Morning Planning Prompt (5 min)
```
Open a new Chat conversation (Cmd+L to clear previous context).

Paste this prompt:
"Today I'm working on: [paste your ticket, goal, or task]

Help me:
1. Break this into 3-7 implementation steps in dependency order
2. Identify which files in #codebase are most relevant
3. Flag any blocker or unknown I should answer BEFORE writing code
4. Suggest which Copilot mode for each step (inline / Chat / Edits / Agent Mode)

Do not implement anything — planning only."

[ ] Plan generated and reviewed
[ ] At least one wrong assumption corrected
[ ] Files to work on identified
```

### Step 3 — Context Setup (3 min)
```
[ ] Open ONLY the files relevant to step 1
[ ] If continuing from yesterday: paste the "Resume" pattern
    ("I was implementing X. Done: [list]. Next: [next step]. Constraint: [key rule].")
[ ] Pre-session git commit if using Agent Mode today:
    git add . && git commit -m "checkpoint: starting [task name]"
```

---

## 💻 During Coding Session

### Every Time You Use Inline Suggestions
```
[ ] Did you write a comment describing the intent before letting Copilot suggest?
[ ] Did you read the ghost text before pressing Tab?
[ ] If suggestion looks wrong: press Alt+] for alternatives before accepting
```

### Every Time You Use Chat
```
[ ] Is a #file or #selection attached? (never send context-free prompts)
[ ] Did you specify the output format? (diff / list / code / prose)
[ ] Is the prompt under 100 words? (if not, trim it first)
```

### Every Time You Use Edits Mode
```
[ ] Working set has ONLY the relevant 2-3 files (not everything)
[ ] Instruction includes what must NOT change
[ ] You will read every diff before accepting
```

### Every Time You Use Agent Mode
```
[ ] Pre-session commit done? (git add . && git commit -m "checkpoint: ...")
[ ] Prompt uses the task template: Context / Goal / Requirements / Constraints / Plan First
[ ] You will stop the session if it modifies files you didn't list
```

### After Every Copilot-Assisted Change
```
[ ] Read the diff (never accept without reading)
[ ] Run: [your test command] (pytest / npm test / mvn test)
[ ] Check: any new imports I don't recognize?
[ ] Check: was any error handling removed silently?
```

---

## 🔐 Before Every PR (15 minutes)

### Security Check
```
[ ] Run /security-review on all changed files
[ ] Check: no hardcoded credentials in generated code
[ ] Check: SQL queries are parameterized (no string concatenation)
[ ] Check: no PII logged
```

### Test Quality
```
[ ] Run /generate-tests if new functions have no tests
[ ] Run test gap analysis on changed files
[ ] Run: [full test suite] — all tests pass
[ ] Check: coverage didn't decrease
```

### Review Yourself
```
[ ] Run the pre-PR self-review prompt from your prompt library
[ ] Is there anything you can't explain? If yes — understand it before merging
```

### PR Description
```
[ ] Run /write-pr-description
[ ] Edit for accuracy (Copilot's description is a first draft)
[ ] Would a reviewer understand what changed and how to test it?
```

---

## 🌙 End of Day (10 minutes)

### Capture Learning
```
Open a new Chat. Run this prompt:

"Generate structured learning notes for the main concept I worked with today.
Topic: [concept / problem you solved]

Also capture:
- One prompt that worked really well today: [paste it]
- One prompt that didn't work well: [paste it + what to change next time]
- One Copilot behavior I noticed today: [anything surprising or useful]"

[ ] Notes saved to: notes/[YYYY-MM-DD]-session.md
```

### Improve the Prompt Library
```
[ ] Did you type the same prompt more than twice today?
    → Create a prompt file for it now (5 minutes)
[ ] Did a prompt produce unexpectedly good output?
    → Add it to your library
[ ] Did a prompt fail badly?
    → Note what to change; update the prompt file
```

### Clean Up
```
[ ] Commit today's work: git add . && git commit -m "feat/fix/refactor: [description]"
[ ] Close unneeded editor tabs
[ ] Update .copilot-context.md if anything changed in the project state
[ ] Mark which step of the plan is done for tomorrow's "Resume" pattern
```

---

## 📅 Weekly Rituals (Friday, 15 minutes)

```
[ ] Review notes from the past week — any patterns to capture?
[ ] Add 1-2 new prompts to library based on repeating tasks
[ ] Run: ls .github/prompts/ — any prompts unused this week? Consider pruning
[ ] Check copilot-instructions.md — still accurate? Any new conventions?
[ ] Run self-assessment using Copilot-Scoring-Rubrics.md — track your score
[ ] Share one useful prompt with a teammate (if working in team)
```

---

## 🚨 When Copilot Goes Wrong — Quick Recovery

```
Bad inline suggestion keeps appearing:
  → Press Escape → keep typing your own code → Copilot regenerates after pause

Copilot Chat giving generic answers:
  → Close conversation (Cmd+L) → attach #file or #selection → re-ask

Agent Mode making wrong changes:
  → Stop immediately → git checkout . → re-prompt with tighter constraints

Copilot ignoring instructions:
  → Check: is copilot-instructions.md at .github/copilot-instructions.md?
  → Ask: "What instructions do you have for this workspace?" to verify

Prompt file not appearing as slash command:
  → Check: file is in .github/prompts/ with .prompt.md extension
  → Check: frontmatter has 'name' and 'description' fields
  → Reload VS Code window
```

---

## 📊 Daily Score (quick self-check)

Rate yourself 1-5 on each at end of day:

| Habit | Score |
|---|---|
| Used context variables (#file, #selection) | /5 |
| Read all diffs before accepting | /5 |
| No secrets pasted into Copilot | /5 |
| Ran tests after every Copilot change | /5 |
| End-of-day notes written | /5 |

**Target**: 20+/25 consistently = Pro-level habits established
