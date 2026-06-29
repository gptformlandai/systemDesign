# Agent Loops — Gold Sheet

> **Track**: Claude Mastery Track — Group 3: Advanced Engineering
> **File**: 5 of 7 (Track File #19)
> **Read after**: MCP-Integration-Gold-Sheet.md

---

## 1. What Agent Loops Are

### Must Know

```
An agent loop is a structured cycle where Claude:
  1. Plans what to do
  2. Executes a step
  3. Verifies the result
  4. Decides what to do next based on the result
  5. Repeats until done or stopped

This is NOT: Claude writing code once and stopping.
This IS: Claude working autonomously until a condition is met.

The verification loop is the most important pattern:
  Claude writes code → runs tests → tests fail → Claude reads output →
  Claude fixes the failures → runs tests again → repeat until all pass

Without verification loops:
  Claude generates code that compiles but fails at runtime.
  You discover problems only when you run it manually.

With verification loops:
  Claude generates code → runs tests → fixes failures → all tests pass.
  You receive working code, not just code that looks right.
```

---

## 2. The Core Loop Patterns

### Pattern 1 — The Test-Verify Loop

```
"Implement [feature] following the pattern in @file:[example].

After implementing each function:
  1. Run: pytest tests/unit/test_[module].py -x
  2. Read the output
  3. If tests fail: fix the failures before moving to the next function
  4. If tests pass: continue to next function

Do not mark any function as complete until its tests pass.
Report: function name + test result after each function."
```

### Pattern 2 — The Build-Verify Loop

```
"Build [feature] and verify it works end to end.

Loop:
  1. Implement the next component
  2. Run: [build command]
  3. If build fails: fix the error and rebuild
  4. If build passes: run tests
  5. If tests fail: fix and re-test
  6. Continue to next component only when build + tests pass

Stop condition: all components built, all tests pass, linting clean."
```

### Pattern 3 — The Refactor-Safe Loop

```
"Refactor @file:[target] to [goal].

Process:
  1. Run: pytest (record current pass count)
  2. Make one refactoring change
  3. Run: pytest again
  4. If same tests pass as before: continue
  5. If any test now fails: undo the change and try a different approach
  6. Commit each successful change

Stop when: refactoring goal achieved, all original tests still pass."
```

### Pattern 4 — The Debug Loop

```
"Debug: [describe the bug]

Relevant code: @file:[file]
Error: [paste error]

Loop:
  1. Identify most likely root cause
  2. Make the fix
  3. Run: [test command that reproduces the bug]
  4. If bug reproduces: it's the wrong fix — try the next hypothesis
  5. If bug is gone: run full test suite
  6. If all tests pass: done. If not: fix regressions.

Max iterations: 5 before stopping and asking for input."
```

---

## 3. Stopping Conditions

### Why You Need Explicit Stopping Conditions

```
Without stopping conditions:
  Claude can loop indefinitely on a problem.
  This wastes time and context window.
  Costs money if using API billing.

Good stopping conditions:
  - "Stop if you have not made progress in 3 iterations"
  - "Stop if you encounter a test failure you cannot fix in 2 attempts"
  - "Stop after implementing [N] functions, not before"
  - "Stop immediately if you need to change the public API to make tests pass"
  - "Stop if you identify a database migration is needed"

Template:
"Loop until: [success condition]
Stop if: [failure conditions — list 3-5]
When stopping: report current state + what blocked progress"
```

---

## 4. Loop Guardrails

```
Guardrail 1 — Max iterations:
  "Run a maximum of 5 fix attempts per test failure.
  If not fixed in 5: stop and report the failure for human input."

Guardrail 2 — Scope boundary:
  "If fixing a test requires changing the public API:
  Stop. Report that the interface design needs review."

Guardrail 3 — Human checkpoints:
  "After implementing 3 functions: pause and report progress.
  I will confirm whether to continue."

Guardrail 4 — Verification before loop exit:
  "Before declaring success: run the full test suite.
  Report the exact count: X tests passed, Y failed, Z skipped."

Guardrail 5 — No test modification:
  "If a test fails: fix the implementation. NEVER modify the test.
  If the test is wrong: stop and flag it for human review."
```

---

## 5. Autonomous Loop — Full Example

```
"Build and verify the user notification preferences feature.

Files to create (in this order):
  1. src/schemas/notification.py (Pydantic schemas)
  2. src/repositories/notification_repo.py (DB access)
  3. src/services/notification_service.py (business logic)
  4. src/api/notifications.py (FastAPI router)
  5. tests/unit/test_notification_service.py (tests)

Loop protocol:
  After creating each file:
    a. Run: ruff check [file] — fix any lint errors
    b. Run: pytest tests/unit/ -x — fix any test failures
    c. Move to next file only when lint + tests pass

Stop conditions:
  - Stop if you need to modify existing files outside this list
  - Stop if a test requires changing the public API design
  - Stop if you cannot fix a test failure in 3 attempts

Final verification:
  Run: pytest tests/ -v
  Run: ruff check src/
  Report: test count, lint status, any remaining issues.

Begin with a plan. Wait for my approval."
```

---

## 6. Revision Checklist

- [ ] Understands the verify-iterate loop (not just generate-once)
- [ ] Uses test-verify loop pattern for all code generation
- [ ] Every loop has explicit stopping conditions
- [ ] Guardrails prevent infinite loops and test modification
- [ ] Human checkpoints inserted for sessions > 30 minutes
- [ ] Autonomous loop template memorized for complex features
