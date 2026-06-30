# Codex 4-Week Mastery Roadmap

> **Track**: Codex Mastery Track — Group 6: Practice & Upgrade
> **File**: 5 of 6 (Track File #34)
> **Audience**: Developers who want a structured day-by-day plan from beginner to pro
> **Time commitment**: 30-60 minutes per day (outside regular work)

---

## Week 1 — Foundations (Days 1–7)

**Goal**: Codex is installed, configured, and producing real value on safe tasks.

### Day 1 — Install + First Session

```bash
# Install
npm install -g @openai/codex

# Set API key (macOS/Linux)
export OPENAI_API_KEY=sk-...
echo 'export OPENAI_API_KEY=sk-...' >> ~/.zshrc  # or ~/.bashrc

# Verify
codex --version

# First session — explain a real file from your project
codex "Explain src/[any-file].py:
       1. What does it do?
       2. Inputs and outputs
       3. 3 things that could go wrong?
       Make no changes."
```

**Checkpoint**: Codex responded without errors. API key works.

---

### Day 2 — Config + AGENTS.md

```bash
# Create config.yaml
mkdir -p ~/.codex
cat > ~/.codex/config.yaml << 'EOF'
model: o4-mini
approval_policy: auto-edit
EOF

# Create AGENTS.md for a real project
codex "Generate an AGENTS.md for this project.
       Include: project context, tech stack, architecture, forbidden actions, verification command.
       Base it on what you can read in the project."
```

**Checkpoint**: AGENTS.md created. Review it — is it accurate?

---

### Day 3 — First Real Task (suggest mode)

```bash
# Pick a real ticket or small improvement
# Use suggest mode (safe — no changes applied)
codex --approval-policy suggest \
  "Plan: [describe your task]
   Which files? What approach? What tests needed?
   Plan only — do not implement."
```

**Checkpoint**: Plan output makes sense. You would have planned it the same way (or better).

---

### Day 4 — First Implementation Task

```bash
# Use auto-edit with verification
codex --approval-policy auto-edit \
  "Add [simple feature] to [file].
   Verification: pytest [test file] -x
   Do not modify test files."
```

**Checkpoint**: Tests pass. `git diff` shows clean change.

---

### Day 5 — Pre-Commit Review Habit

```bash
# Add this to your git workflow today
git add [file]
git diff --staged | codex --approval-policy suggest \
  "Security review. Check: SQL injection, auth, PII. Table: SEVERITY|ISSUE|FIX. Verdict: APPROVED/CHANGES REQUIRED."
```

**Checkpoint**: Review ran before at least one commit today.

---

### Day 6 — Test Generation

```bash
# Generate tests for a function you wrote this week
codex --approval-policy auto-edit \
  "Generate tests for [function] in [file].
   Cover: happy path, error paths, edge cases (None, empty, max).
   Only mock external deps.
   Verification: pytest [test file] -v"
```

**Checkpoint**: Tests generated, pass, and actually test the function.

---

### Day 7 — Week 1 Review

```bash
# Self-assessment
codex "Review my AGENTS.md and suggest improvements.
       Has anything I did this week revealed missing rules?"
```

Run the Scoring Rubrics: Prompt Quality + Verification Discipline.
Target: 3+/5 on each.

---

## Week 2 — Intermediate Power User (Days 8–14)

**Goal**: Codex integrated into daily workflow. Using for testing, documentation, and reviews consistently.

### Day 8 — Context Engineering

```
Read: Context-Engineering-Gold-Sheet.md
Practice: Start 3 Codex sessions today with explicit file scope in startup
Compare: does primed session outperform unprimed?
```

### Day 9 — Test-First TDD Loop

```bash
# Write tests before implementation for one task today
codex --approval-policy auto-edit \
  "Write tests for [feature that doesn't exist yet].
   Tests should fail now (no implementation).
   Verification: pytest [test file] --collect-only (should collect N tests)"

# Then implement
codex --approval-policy auto-edit \
  "Make all tests in [test file] pass.
   Do not modify test files.
   Verification: pytest [test file] -v"
```

### Day 10 — Documentation Sprint

```bash
# Docstring 3 files in under 30 minutes
for file in src/services/*.py; do
    codex --model gpt-4.1-mini --approval-policy auto-edit \
      "Add Google-style docstrings to all public functions in $file. No invented content."
done
```

### Day 11 — Build Script Library (3 scripts)

From the Prompt Script Library, install at minimum:
- `codex-review` (pre-commit security review)
- `codex-fix-tests` (fix failing tests)
- `codex-plan` (morning planning)

### Day 12 — Full PR Review

```bash
git diff main..HEAD | codex --model gpt-4.1 --approval-policy suggest \
  "Full PR review. Security + correctness + tests + AGENTS.md compliance.
   Table: SEVERITY|ISSUE|FILE:LINE|FIX. Verdict: APPROVED/CHANGES REQUIRED."
```

### Day 13 — Approval Policy Deep Dive

```
Read: Approval-Policy-Modes-Gold-Sheet.md
Practice: Run 3 tasks in suggest mode, 3 in auto-edit, note differences
Decision: which tasks in your workflow belong in which mode?
```

### Day 14 — Week 2 Review

Run scoring rubrics: all 7. Target: 3+/5 average.
Update AGENTS.md with new rules you've learned this week.

---

## Week 3 — Advanced Engineering (Days 15–21)

**Goal**: System prompts, agent loops, context optimization, and first full-auto session.

### Day 15 — System Prompt Roles

```bash
# Try 3 roles today on real code
# Role 1: Security reviewer
codex --system-prompt "You are a security engineer. SEVERITY|ATTACK VECTOR|FIX|OWASP for every finding." \
  "Review src/auth/ for vulnerabilities"

# Role 2: Technical writer
codex --system-prompt "You are a technical writer. No invented content. [verify] for uncertain facts." \
  "Document src/payments/service.py"

# Compare output quality vs without roles
```

### Day 16 — Agent Loop Engineering

```
Read: Agent-Loops-Gold-Sheet.md
Practice: Write a looping implementation prompt with:
  - 4 stopping conditions
  - Verification after each iteration
  - "Fix implementation, not tests" constraint
```

### Day 17 — First Full-Auto Session

```bash
# Prerequisites: feature branch + clean git state + bounded task
git checkout -b feature/codex-auto-test
git add -A && git commit -m "checkpoint: before first full-auto"
codex --approval-policy full-auto \
  "[bounded, well-defined task with explicit scope, forbidden list, done-when]"
# Post session: git diff HEAD~1 and read every line
```

### Day 18 — Token & Context Optimization

```
Read: Token-Context-Optimization-Gold-Sheet.md
Practice: Use /compact between 3 related tasks in one session
Measure: how does output quality differ between cluttered vs compact context?
```

### Day 19 — Shell Integration

```
Read: Tool-Use-and-Shell-Integration-Gold-Sheet.md
Practice: Add Codex targets to a real Makefile
Install: all 6 script library scripts
```

### Day 20 — Multi-Agent Pipeline

```
Read: Multi-Agent-Patterns-Gold-Sheet.md
Practice: Run a 4-phase pipeline (Planner → Builder → Tester → Reviewer) on a small feature
Write: handoff document between phases
```

### Day 21 — Week 3 Review

Run scoring rubrics. Target: 4+/5 on all 7.
Write a retrospective: what does autonomous Codex usage enable that you couldn't do before?

---

## Week 4 — Pro / MAANG Level (Days 22–28)

**Goal**: Personal Operating System complete. Autonomous feature delivery. MAANG-level usage.

### Day 22 — Personal OS Setup

```
Read: Personal-Codex-Operating-System-Gold-Sheet.md
Build: complete OS — AGENTS.md + script library + config.yaml + daily ritual
```

### Day 23 — Full SDLC Coverage

```
Read: SDLC-Automation-Gold-Sheet.md
Practice: Use Codex for all 10 phases on one complete ticket (requirements → post-merge)
```

### Day 24 — Verification-Driven Workflow

```
Read: Verification-Driven-Workflows-Gold-Sheet.md
Practice: Test-first → implement → refactor loop with Codex for one feature
No implementation step without a verification command
```

### Day 25 — Autonomous Feature Build

```
Attempt: autonomous feature build using the full template
  - 7-condition checklist complete
  - All phases: bounded task, scope, forbidden, done-when
  - Post-session full review
```

### Day 26 — MAANG Interview Prep

```
Read: Codex-MAANG-Interview-Prep-Gold-Sheet.md
Practice: 60-second pitch without notes
Practice: Answer 5 L5-level questions cold
```

### Day 27 — Full Question Bank Run

```
Read: Codex-Active-Recall-Question-Bank.md
Run: all 30 questions without looking at answers
Score: target 25+/30
Review: any question below 4/5 → go back to source gold sheet
```

### Day 28 — Final Assessment

Run all 7 scoring rubrics on real work from this week.

**Target**: 31+/35 total score.

**Done**: Codex is a professional force multiplier in your workflow.

---

## After Week 4 — Maintenance

```
Weekly: Morning planning ritual every day + update AGENTS.md as needed
Monthly: Prune script library, retire prompts that don't work, add new ones
Quarterly: Re-run full question bank + scoring rubrics — are you drifting?
```
