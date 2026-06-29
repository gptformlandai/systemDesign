# Copilot Mock Workflow Scripts

> **Track**: Copilot Mastery Track — Group 6: Practice Upgrade
> **File**: 3 of 5 (Track File #34)
> **Usage**: Timed exercises to build Copilot workflow muscle memory

---

## How to Use This File

Each script is a timed drill. Run it with a timer.
Use your actual codebase or the exercises from the Foundations quick wins.
Score yourself at the end.

---

## Mock #1 — Morning Planning Sprint (10 minutes)

**Setup**: Pick one real task you're working on today.

**Timer**: Start now.

**Drill**:
```
Step 1 (2 min): Open Copilot Chat. Paste your task description.
  Prompt: "Break this task into implementation steps.
  Using #codebase, identify which files are affected.
  Identify 3 risks or unknowns.
  Plan only — no code."

Step 2 (3 min): Review the plan. Correct any wrong assumptions.

Step 3 (3 min): Ask which Copilot mode is best for each step.
  "For each step, which Copilot mode is most appropriate:
  inline suggestions / Chat Ask / Edits / Agent Mode / prompt file?"

Step 4 (2 min): Set up your working context.
  Open only the files relevant to step 1.
  Close everything else.
```

**Score**:
- Plan generated: ✓ / ✗
- At least one incorrect assumption caught and corrected: ✓ / ✗
- Files opened match exactly what's needed: ✓ / ✗
- Correct mode identified for each step: ✓ / ✗

Score: ___/4

---

## Mock #2 — Test Generation Drill (15 minutes)

**Setup**: Use any function from your current project that has no or minimal tests.

**Timer**: Start now.

**Drill**:
```
Step 1 (2 min): Select the function in editor.

Step 2 (5 min): Generate tests using the prompt from your library.
  /generate-tests (or type the prompt manually)
  Verify: tests cover happy path, 2+ error cases, 2+ edge cases.

Step 3 (3 min): Review generated tests.
  Ask: "Are there any test cases missing that I can think of?"
  Add at least 1 test that Copilot missed.

Step 4 (3 min): Run the tests.
  If failing: diagnose with Copilot before fixing manually.
  Fix any test setup issues.

Step 5 (2 min): Run test gap analysis on the result.
  "What is still not covered?"
```

**Score**:
- Tests generated in under 5 minutes: ✓ / ✗
- At least 6 distinct test cases: ✓ / ✗
- You added at least 1 test Copilot missed: ✓ / ✗
- All tests pass without manual debugging: ✓ / ✗
- Gap analysis run: ✓ / ✗

Score: ___/5

---

## Mock #3 — Debug an Error (10 minutes)

**Setup**: Find a real error in your project (recent CI failure, test failure, or runtime error).
OR use the simulated error from Debugging-With-Copilot-Scenarios.

**Timer**: Start now.

**Drill**:
```
Step 1 (1 min): Collect: error message + stack trace + relevant code.
  Verify: no sensitive data in what you're about to share.

Step 2 (3 min): Use #terminalLastCommand or paste the error.
  /debug-error [or your debug prompt]
  Read the full response before asking follow-ups.

Step 3 (3 min): Evaluate the diagnosis.
  Is the root cause explanation convincing?
  Do you understand WHY it happens (not just WHAT happened)?
  If not, ask: "Explain WHY [specific part] causes this"

Step 4 (3 min): Apply the recommended fix.
  Run tests to verify the fix works.
```

**Score**:
- No sensitive data shared: ✓ / ✗
- Understood root cause (not just symptom): ✓ / ✗
- Fix applied and tests pass: ✓ / ✗
- Completed in under 10 minutes: ✓ / ✗

Score: ___/4

---

## Mock #4 — Pre-PR Review Sprint (15 minutes)

**Setup**: A PR you're about to open (or a file you changed recently).

**Timer**: Start now.

**Drill**:
```
Step 1 (5 min): Security review.
  /security-review on each changed file.
  Note: any CRITICAL or HIGH issues found?

Step 2 (3 min): Test gap analysis.
  "What error paths in my changes are not tested?"

Step 3 (3 min): Generate PR description.
  /write-pr-description

Step 4 (2 min): Run tests.
  All should pass. Note any failures.

Step 5 (2 min): Review the PR description.
  Edit for accuracy.
  Would a reviewer understand what changed and how to test it?
```

**Score**:
- Security review completed: ✓ / ✗
- At least 1 test gap identified: ✓ / ✗
- PR description generated and reviewed: ✓ / ✗
- All tests pass: ✓ / ✗
- Completed in under 15 minutes: ✓ / ✗

Score: ___/5

---

## Mock #5 — Agent Mode Controlled Session (20 minutes)

**Setup**: A scaffolding task. Example: "Add a new API endpoint for [X] following existing patterns."

**Timer**: Start now.

**Drill**:
```
Pre-session (before timer):
  git add . && git commit -m "checkpoint: before agent mode drill"

Step 1 (3 min): Write the Agent Mode prompt.
  Use the task template: Context, Goal, Requirements, Constraints, Plan First.
  
Step 2 (5 min): Review the plan.
  Correct at least 1 incorrect assumption if present.
  Confirm the plan is correct before approving.

Step 3 (7 min): Execute and monitor.
  Watch for: unexpected file modifications, scope drift.
  If you see scope drift: stop immediately and note it.

Step 4 (5 min): Review all changes.
  Open Source Control diff.
  Read every changed file.
  Run tests.
```

**Score**:
- Pre-session commit done: ✓ / ✗
- Used full task template (Context/Goal/Requirements/Constraints/Plan First): ✓ / ✗
- Reviewed and corrected the plan before approving: ✓ / ✗
- No unreviewed changes accepted: ✓ / ✗
- Tests pass after accepting: ✓ / ✗

Score: ___/5

---

## Mock #6 — Token Efficiency Drill (10 minutes)

**Purpose**: Practice writing compact, high-signal prompts.

**Drill**:
```
Step 1 (3 min): Write a verbose version of a prompt for this task:
  "I need to add input validation to my create user endpoint"
  Write it as you normally would (no constraints).

Step 2 (3 min): Rewrite it as a compact version.
  Target: under 50 words.
  Must include: #file or #selection reference, goal, one constraint, output format.

Step 3 (2 min): Send BOTH prompts in separate chat sessions.
  Compare: which produced more useful output?

Step 4 (2 min): Apply the token-efficient version going forward.
  Save it as a prompt template if it works well.
```

**Score**:
- Compact version is under 50 words: ✓ / ✗
- Compact version references a specific file or selection: ✓ / ✗
- Compact version includes an output format: ✓ / ✗
- Compact version produced equal or better output than verbose: ✓ / ✗

Score: ___/4

---

## Weekly Score Tracker

| Week | Planning | Testing | Debugging | Pre-PR | Agent Mode | Token Efficiency | Total |
|------|----------|---------|-----------|--------|------------|-----------------|-------|
| 1 | /4 | /5 | /4 | /5 | /5 | /4 | /27 |
| 2 | /4 | /5 | /4 | /5 | /5 | /4 | /27 |
| 3 | /4 | /5 | /4 | /5 | /5 | /4 | /27 |
| 4 | /4 | /5 | /4 | /5 | /5 | /4 | /27 |

**Target**: 22+/27 by Week 4 = Pro-level workflow maturity
