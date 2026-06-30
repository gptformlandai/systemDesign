# Personal Codex Operating System — Gold Sheet

> **Track**: Codex Mastery Track — Group 4: Pro / Production Level
> **File**: 1 of 5 (Track File #21)
> **Audience**: Developers who use Codex daily and want a systematic, high-leverage workflow
> **Read after**: All Advanced Engineering files

---

## Practical Impact Meter

| Topic | Impact | Why Developers Miss This |
|---|---|---|
| Daily ritual — morning planning with Codex | ★★★★★ | Ad-hoc Codex use wastes 30-60% of potential leverage; ritual makes it systematic |
| AGENTS.md as a living document (weekly updates) | ★★★★★ | Static AGENTS.md drifts from reality; weekly update keeps it accurate and growing |
| Prompt script library (reusable scripts) | ★★★★★ | Retyping prompts daily = wasted time; saved scripts = compound leverage |
| End-of-day learning capture | ★★★★☆ | Sessions produce insights; without capture, they're lost the next day |
| Monthly maintenance ritual | ★★★☆☆ | AGENTS.md and scripts need pruning — unused rules and scripts create noise |

---

## ⭐ Beginner Tier — Start Here

### B1: Your first daily ritual (10 minutes)

Try this tomorrow morning:

```bash
# Morning: before writing any code
codex "Today I need to work on: [describe your main task for today].
       Using the project structure, help me:
       1. Break this into 3-5 implementation steps
       2. Identify which files I'll need to touch
       3. Identify one risk or unknown I should resolve first
       Do not implement. Plan only."
```

Save the output. Work from the plan. At end of day, note what was accurate and what wasn't.
This 10-minute investment prevents false starts and scope drift.

---

## 1. What a Codex OS Is

```
A Codex OS is the set of persistent configurations that make every Codex session
immediately productive, without needing to re-explain your project, standards,
or workflow preferences.

It consists of 5 components:

1. AGENTS.md (project memory)
   - Project context, architecture, coding standards, forbidden actions
   - Lives in the project root (and key subdirectories)
   - Updated weekly based on observed mistakes and new patterns

2. Prompt Script Library (reusable workflows)
   - Shell scripts in scripts/codex/ (or your preferred location)
   - Each script: one common task with all constraints baked in
   - Grow this library from your actual daily tasks

3. Global config.yaml (sensible defaults)
   - ~/.codex/config.yaml: model, approval_policy, notify
   - Set it once, forget it

4. Daily ritual (habitual usage pattern)
   - Morning: plan
   - During coding: implement with verification loops
   - Pre-commit: review
   - End of day: capture

5. Weekly/monthly maintenance
   - AGENTS.md updates
   - Prompt library pruning
   - Config optimization
```

---

## 2. Daily Ritual

### Morning (10 minutes)

```bash
# Step 1: Orient Codex to today's work
codex "Today's focus: [ticket/task description]
       Current branch: $(git branch --show-current)
       Today's plan:
       1. Break this into implementation steps
       2. Identify files in scope
       3. Identify risks or blockers
       Do not implement. Plan only."

# Step 2: Git checkpoint before first Codex task
git add -A
git commit -m "day start: $(date +%Y-%m-%d)"
```

### During Coding Sessions

```bash
# Rule: one task per Codex session for implementation
# Rule: always include verification command
# Rule: review diff before committing

# Pattern for each task:
git commit -m "checkpoint: before [task]"
codex --approval-policy auto-edit "[task with verification command]"
git diff HEAD~1        # review
pytest -x              # verify
git commit -m "[task description]"
```

### Pre-Commit Gate

```bash
# Run before every git commit (add this to git hooks or Makefile)
STAGED=$(git diff --staged --name-only)
codex --approval-policy suggest \
  "Quick pre-commit review of: $STAGED
   Check: security issues, missing tests, convention violations.
   Report in 5 bullets maximum. Be brief."
```

### End-of-Day Capture (5 minutes)

```bash
codex "Generate session notes for today.
       What I worked on: [brief description]
       
       Output:
       ## Best Prompt Today
       [The prompt that produced the most useful result]
       
       ## Prompt That Failed
       [What didn't work and what I'll change next time]
       
       ## AGENTS.md Update Needed?
       [Any mistake Codex made that a new AGENTS.md rule would prevent]
       
       ## New Script Candidate
       [Any prompt I typed 2+ times today — should be a saved script]"
```

---

## 3. Prompt Script Library Structure

```
scripts/codex/
  review.sh           — security + test coverage review
  test-gen.sh         — test generation with gap analysis
  debug.sh            — error diagnosis from stack trace
  precommit.sh        — pre-commit review
  docs.sh             — docstring generation
  security.sh         — OWASP-focused security review
  scaffold.sh         — new endpoint from pattern

# Usage:
./scripts/codex/review.sh src/payments/service.py
./scripts/codex/test-gen.sh src/orders/service.py
./scripts/codex/security.sh src/auth/login.py
```

### Template: Add a script

```bash
#!/bin/bash
# scripts/codex/review.sh
# Usage: ./scripts/codex/review.sh <file>

FILE=${1:?Usage: $0 <filepath>}
[ -f "$FILE" ] || { echo "File not found: $FILE"; exit 1; }

codex --approval-policy suggest \
  "Review $FILE:
   1. Security: SQL injection, missing auth, hardcoded credentials, PII in logs
   2. Test gaps: error paths not covered by tests
   3. Convention violations vs AGENTS.md
   Format: | SEVERITY | ISSUE | LINE | FIX |
   Final: APPROVED / APPROVE WITH COMMENTS / CHANGES REQUIRED
   Do not make changes."
```

---

## 4. Personal OS Repository Structure

```
~/codex-os/               ← your personal Codex OS (separate git repo)
  
  configs/
    global-config.yaml    ← symlinked to ~/.codex/config.yaml
    
  agents-templates/
    python-fastapi.md     ← AGENTS.md template for Python/FastAPI projects
    typescript-express.md ← AGENTS.md template for TypeScript/Express projects
    go-gin.md             ← AGENTS.md template for Go/Gin projects
    
  scripts/
    review.sh             ← reusable across all projects
    test-gen.sh
    security.sh
    precommit.sh
    scaffold.sh
    
  notes/
    YYYY-MM-DD-session.md ← daily capture files
    patterns.md           ← prompt patterns that work
    anti-patterns.md      ← prompts that produced bad output
```

---

## 5. Maintenance Rituals

### Weekly (15 minutes)

```
1. AGENTS.md review:
   - What mistakes did Codex make this week?
   - What rule would have prevented each?
   - Add the rules. Be specific.
   
2. Prompt script review:
   - Any prompt typed 3+ times this week? → Save as a script.
   - Any script that wasn't used at all? → May not be needed.
   
3. Anti-patterns log:
   - What output did you reject this week?
   - What constraint would have prevented the wrong output?
   
Weekly time investment: 15 minutes
Compound return: every hour of Codex work next week is more precise.
```

### Monthly (30 minutes)

```
1. AGENTS.md audit:
   - Is every rule still relevant and accurate?
   - Are stack versions correct? (Did we upgrade anything?)
   - Are forbidden actions comprehensive?
   - Total length: should be under 200 lines. Prune if over.

2. Prompt script library audit:
   - Used in the past month: keep.
   - Not used: archive or delete.
   - Missing: add from notes.

3. Model usage review:
   - Am I using gpt-4.1 where o4-mini would work?
   - Am I using o4-mini where gpt-4.1 would get better results?

4. Security review:
   - Review any auth/SQL files that had AI-generated changes this month.
   - Any security findings from production that AI-generated code contributed to?
```

---

## 6. Anti-Patterns of a Broken Personal OS

```
BROKEN PATTERN 1: No AGENTS.md or stale AGENTS.md
  Symptom: Codex produces code that violates your conventions every session
  Fix: update AGENTS.md immediately after every mistake

BROKEN PATTERN 2: Ad-hoc prompts, no script library
  Symptom: Retyping the same prompt daily with slight variations
  Fix: save the first time you type a prompt twice

BROKEN PATTERN 3: No git checkpoint discipline
  Symptom: "I can't undo the full-auto session" is a regular occurrence
  Fix: commit before every full-auto, no exceptions

BROKEN PATTERN 4: No end-of-day capture
  Symptom: repeating the same mistakes weekly, not remembering best prompts
  Fix: 5-minute session notes at end of each day

BROKEN PATTERN 5: Review skipped when tests pass
  Symptom: AI-generated scope creep and wrong abstractions committed regularly
  Fix: git diff HEAD~1 is non-negotiable, regardless of test results
```

---

## Interview Traps

```
TRAP: "My Codex OS is AGENTS.md — that's the main thing"
TRUTH: AGENTS.md is one component. The OS is: AGENTS.md + script library + config.yaml
       + daily ritual + maintenance habit. Without the maintenance ritual, AGENTS.md
       becomes stale and the OS degrades — Codex applies outdated conventions.

TRAP: "I'll build the OS once and it will be permanent"
TRUTH: A Codex OS degrades without maintenance. New tech stack additions aren't in
       AGENTS.md. Outdated conventions remain enforced. The 10-minute weekly review
       keeps the OS current. Skip it for 2 months and the OS becomes a liability.

TRAP: "More scripts = better OS"
TRUTH: A script library with 50 scripts has a discovery problem — you can't remember
       which to use. 10 scripts with memorable names you use daily outperform 50 scripts
       you never invoke. Prune monthly. Keep only what you actually run.
```

---

## Revision Checklist

- [ ] Daily morning planning ritual running consistently for 1 week
- [ ] End-of-day capture producing at least 3 notes per week
- [ ] Have at least 5 prompt scripts in the script library
- [ ] AGENTS.md updated at least once this week based on observed mistake
- [ ] Weekly maintenance ritual completed at least once
- [ ] Can describe all 5 components of a Codex OS
