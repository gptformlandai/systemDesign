# Autonomous Workflows — Gold Sheet

> **Track**: Claude Mastery Track — Group 4: Pro / Production Level
> **File**: 5 of 5 (Track File #25)
> **Read after**: Verification-Driven-Workflows-Gold-Sheet.md

---

## Core Principle

```
Autonomous ≠ unsupervised.
The goal is: Claude completes well-defined tasks independently while humans
retain control of scope, verification, and the commit decision.

Autonomy is safe when:
  1. The scope is clearly defined (what to build, what NOT to touch)
  2. Stopping conditions are explicit (when to ask vs when to proceed)
  3. Verification loops are built in (Claude checks its own work)
  4. Checkpoints exist (commits before agent runs; recovery if needed)
  5. Output is reviewed before it merges (autonomous run ≠ autonomous merge)
```

---

## 1. Safe Autonomy Checklist

### Before Every Autonomous Session

```
[ ] git commit: "checkpoint before autonomous session — [task name]"
[ ] CLAUDE.md is in place and accurate for this project
[ ] Scope is written down: exactly what files Claude will touch
[ ] Forbidden actions are explicit: "Do NOT run migrations / modify tests / touch [file]"
[ ] Stopping conditions are defined: when Claude should stop and ask
[ ] Verification command is defined: what Claude runs to confirm success
[ ] Time estimate set: if Claude is taking longer than [X], stop and report

If any of these are not in place: add them to the prompt before starting.
```

---

## 2. The Autonomous Feature Build

### Full Template

```
"Build [feature name] autonomously.

Context:
  Project: @file:CLAUDE.md
  Plan: @file:docs/plans/[feature]-plan.md
  Existing patterns: @file:src/services/[example].py

Goal: Implement all components in the plan to the point where all tests pass.

Implementation protocol:
  1. Start with the plan. Implement components in the listed order.
  2. After each file: run ruff check [file]
  3. After each component group: run pytest tests/unit/ -x
  4. Fix all failures before moving to the next component
  5. Do NOT modify test files to fix failures — fix the implementation

Forbidden actions:
  - Do NOT run database migrations
  - Do NOT modify: tests/, CLAUDE.md, .claude/, any config file
  - Do NOT install new packages without listing them first for approval

Stopping conditions:
  - Stop if a task requires modifying the public API (report first)
  - Stop if the same test fails 3 times with different approaches
  - Stop if you need to install a package not already in requirements.txt
  - Stop after implementing each major component group for a status checkpoint

Final verification:
  Run: pytest tests/ -v
  Run: ruff check src/
  Report: X tests passed, Y failed, lint status, what's complete, what's not"
```

---

## 3. Autonomous Refactoring

```
"Refactor @file:src/ [scope] to achieve: [goal].

Safety rules:
  - Record current test count: pytest tests/ -v (baseline)
  - Make ONE structural change per iteration
  - Run: pytest tests/ -x after EACH change
  - If any test breaks: undo the change and try a different approach
  - Never modify test files
  - If a refactoring goal cannot be achieved without changing behavior: STOP and report

Allowed changes: extract functions, rename variables, extract classes
Forbidden changes: modify logic, change function signatures, alter error behavior

Autonomous until: all planned refactoring is done, baseline test count preserved
Stop immediately if: any baseline test now fails and cannot be restored"
```

---

## 4. Autonomous Bug Fix

```
"Fix the bug described below. Autonomous mode.

Bug report:
  Symptom: [describe observable behavior]
  Reproduction: [steps or test case]
  Expected: [what should happen]
  Relevant code: @file:src/services/[service].py

Process:
  1. Read the relevant code and identify the root cause
  2. State your hypothesis before making any change
  3. Make the fix
  4. Run: pytest tests/unit/test_[service].py::test_[relevant_test] -x
  5. If bug reproduced: wrong fix — try the next hypothesis
  6. If bug gone: run full test suite: pytest tests/ -v
  7. If any regression: fix it before stopping

Stopping conditions:
  - Stop after 4 failed hypotheses — report all 4 and ask for input
  - Stop if the fix requires a database migration

Forbidden: modifying tests to make them pass the buggy behavior"
```

---

## 5. Autonomous CI Fix

```
"Fix the failing CI workflow.

Failing workflow: @file:.github/workflows/[workflow].yml
Error log: [paste the failing step's output]

Process:
  1. Diagnose: root cause of the failure
  2. Make the fix
  3. Verify: does the YAML syntax still parse? (python3 -c "import yaml; yaml.safe_load(open('.github/workflows/[workflow].yml'))")
  4. Report: what was wrong, what was changed, and whether the fix is verified

Constraints:
  - Do NOT remove existing steps — only fix them
  - Do NOT change the trigger conditions
  - All action versions must remain pinned (no @latest, no @main)
  - No hardcoded secrets — all credentials must use ${{ secrets.NAME }}"
```

---

## 6. Recovery Patterns

### When Autonomous Sessions Go Wrong

```
Pattern 1 — Wrong files modified:
  Detection: git diff shows files you didn't expect
  Fix: git checkout [unexpected files]
  Prevention: explicit "Do NOT modify [list]" in every autonomous prompt

Pattern 2 — Tests modified to pass:
  Detection: git diff tests/ shows changes
  Fix: git checkout tests/
  Prevention: "Test files are read-only" in CLAUDE.md + hook

Pattern 3 — Scope creep (too many changes):
  Detection: diff is far larger than expected
  Fix: git checkout . (if nothing useful was done)
       OR review file by file, accepting useful changes and rejecting scope creep
  Prevention: "Only modify the files listed in the plan"

Pattern 4 — Infinite loop (Claude keeps retrying the same failure):
  Detection: session is running without progress for 10+ minutes
  Fix: Stop the session manually (Ctrl+C in CLI)
  Recovery: start new session with explicit "try a different approach for [failure]"
  Prevention: "Stop after 3 failed attempts on the same error"

Pattern 5 — Dangerous command ran (migration, rm, etc.):
  Detection: pre_tool_use.sh should have blocked it; if it didn't: git log
  Fix: alembic downgrade -1 (if migration); git reflog (if git command)
  Prevention: Add the command pattern to pre_tool_use.sh immediately
```

---

## 7. What Autonomous Workflows Enable

```
With a mature Claude OS + autonomous workflows, you can:

1. "Build the notification preferences feature" →
   @planner plans → @builder builds with verification loops → @tester tests →
   @reviewer reviews → you commit after reviewing the final diff

2. "Fix all lint errors in src/" →
   Claude iterates through files, fixes each, re-runs lint, done

3. "Generate tests for all untested functions in src/services/" →
   Claude audits, generates tests, runs them, reports final coverage

4. "Update all deprecated API usage found in @codebase" →
   Claude finds usages, updates each, runs tests per module, reports summary

In all cases: you own the commit decision.
Claude does the repetitive mechanical work. You do the judgment calls.
```

---

## 8. Revision Checklist

- [ ] Knows the 5 components of safe autonomy (scope / stopping conditions / verification / checkpoints / review)
- [ ] Runs the pre-session safety checklist before every autonomous run
- [ ] Every autonomous prompt has explicit forbidden actions and stopping conditions
- [ ] Knows all 5 recovery patterns and their first fixes
- [ ] Never merges autonomous output without reviewing the final diff
- [ ] Has pre_tool_use.sh blocking dangerous commands from autonomous sessions
