# Verification-Driven Workflows — Gold Sheet

> **Track**: Claude Mastery Track — Group 4: Pro / Production Level
> **File**: 4 of 5 (Track File #24)
<<<<<<< HEAD
> **Read after**: SDLC-Automation-Gold-Sheet.md

---

## 1. The Verification-Driven Philosophy

```
Verification-driven = Claude doesn't "finish" until measurable criteria are met.

Traditional workflow:
  Claude generates code → you run tests → tests fail → you manually fix → repeat
  Your time: wasted on feedback loop between Claude and tests

Verification-driven workflow:
  Claude generates code → Claude runs tests → Claude reads output →
  Claude fixes failures → Claude runs tests again → repeat until all pass
  Your time: spent only on review, not on the feedback loop

The verification loop is the single highest-leverage Claude pattern.
Master this, and Claude becomes a force-multiplier instead of a code-generator.
=======
> **Read after**: Debugging-Claude-Handbook-Gold-Sheet.md

---

## Core Principle

```
Claude is a first-draft generator. Verification loops are the quality gate.

Without verification: Claude generates code that looks right but fails at runtime.
With verification: Claude generates → verifies → fixes → verifies again → done.

The difference: you receive working code, not code that needs manual testing.
>>>>>>> refs/remotes/origin/main
```

---

<<<<<<< HEAD
## 2. The Core Verification Loop

### Pattern: Generate → Run → Read → Fix → Repeat

```bash
# Invoke:
"Implement process_refund() in @file:src/services/payment_service.py.

After implementing:
1. Run: pytest tests/unit/test_payment_service.py -x --tb=short
2. If any tests fail: read the failure output
3. Fix the implementation (never the tests)
4. Run tests again
5. Repeat until all tests pass

Show me:
- The implementation (as diff)
- Test run results (final pass count)
- Any tests you couldn't fix (with the failure message)"
```

### What "Fix the Implementation, Never the Tests" Means

```
Tests define correctness. Implementation must satisfy tests.
If a test says: "process_refund() raises InsufficientFundsError when amount > order.total"
And the test fails because process_refund() returns None instead:
  WRONG: Change the test to expect None → Claude passes tests by making them vacuous
  CORRECT: Change process_refund() to raise InsufficientFundsError → test passes meaningfully

This is the #1 rule of verification-driven development.
Any time Claude wants to modify a test to make it pass: flag it and stop.
=======
## 1. The Verification Loop Architecture

```
Every verification loop has 4 components:

1. ACTION: what Claude generates or changes
2. CHECK: what command to run to verify
3. SUCCESS CONDITION: what output proves the action was correct
4. FAILURE RESPONSE: what Claude does if the check fails

Without all 4, the loop is incomplete.
```

### Template

```
"[Action description]

After each step:
  Run: [check command]
  Success: [what passing looks like — exact or pattern]
  On failure: [fix implementation / try different approach / stop and report]
  Max retries: [N] before stopping

Final verification:
  Run: [comprehensive check command]
  Report: [what to include in the final status report]

Constraint: [what Claude must never change — test files, public APIs, etc.]"
>>>>>>> refs/remotes/origin/main
```

---

<<<<<<< HEAD
## 3. Verification Gates — What to Check and When

### Gate 1: Lint Gate (before any commit)

```bash
"After making changes to any .py file:
Run: ruff check [file] && ruff format --check [file]
Fix all lint errors before moving to the next step.
If ruff format produces changes: apply them.
Report: lint result (clean / N errors fixed)"
```

### Gate 2: Type Check Gate (for typed codebases)

```bash
"After implementing [feature]:
Run: mypy src/[module]/ --strict
Fix all type errors. Do NOT add # type: ignore comments.
If a type error reveals a real type mismatch in the design: flag it.
Report: type check result (clean / N errors)"
```

### Gate 3: Unit Test Gate (after every function)

```bash
"After implementing each function:
Run: pytest tests/unit/test_[module].py::test_[function]* -v
Fix failures in the implementation.
Move to next function only when this function's tests pass.
Final: X passed, Y failed"
```

### Gate 4: Integration Test Gate (after feature complete)

```bash
"After all unit tests pass:
Run: pytest tests/integration/ -v --tb=short
These tests require: [DATABASE_URL, REDIS_URL] set in environment.
Report: integration test results.
Note: integration test failures may indicate infrastructure issues, not code bugs."
```

### Gate 5: Security Gate (before merge)

```bash
"Before flagging this feature as ready:
Run: /review @file:[all changed files]
Check specifically: injection vectors, auth, PII handling.
Report: any CRITICAL or HIGH findings that block merge."
```

### Gate 6: Performance Gate (for performance-sensitive code)

```bash
"After implementing the query in @file:src/repositories/order_repo.py:
Run: python -m cProfile -s cumulative benchmark.py | head -20
Compare to baseline: [baseline time]
If > 2x baseline: run /optimize on the implementation before proceeding."
=======
## 2. Test-First Verification Loop

```
"Implement [feature] to pass the tests in @file:tests/unit/test_[module].py

Process:
  1. Read the tests to understand required behavior
  2. Implement the function
  3. Run: pytest tests/unit/test_[module].py::test_[function] -x
  4. If passes: move to next function
  5. If fails: read the exact failure, fix implementation, re-run
  6. After all functions: run: pytest tests/unit/test_[module].py -v

Constraints:
  - NEVER modify test files to make tests pass
  - If a test appears wrong: flag it but do not change it
  - Implementation is done when ALL tests pass with no modifications to test files"
>>>>>>> refs/remotes/origin/main
```

---

<<<<<<< HEAD
## 4. Verification-First Development

### The Correct Sequence

```
Step 1: Write tests FIRST (or have @tester generate them)
  "Generate failing tests for process_refund() based on this spec:
  @file:docs/refund-spec.md
  Do not implement yet. Tests should all FAIL (red state)."

Step 2: Run tests, confirm they fail
  pytest tests/unit/test_payment_service.py -v
  Expected: all new tests fail (they test code that doesn't exist yet)

Step 3: Implement to pass the tests
  "Implement process_refund() to pass these tests: @file:tests/unit/test_payment_service.py
  After implementing: run the tests. Fix failures. All tests must pass before done."

Step 4: Lint and type check
  After all tests pass: run lint and type check gates.

Why this order matters:
  Tests written AFTER implementation validate the implementation, not the spec.
  Tests written BEFORE implementation validate the spec — which is what you need.
=======
## 3. Build-Lint-Test Pipeline Loop

```
"Implement @file:src/services/[service].py

For each function implemented:
  Step 1: Run: ruff check src/services/[service].py
  Step 2: If lint errors: fix them before continuing
  Step 3: Run: pytest tests/unit/test_[service].py -x --tb=short
  Step 4: If test fails: fix implementation, re-run from Step 1
  Step 5: Only move to next function when Step 1 and Step 3 both pass

Final verification:
  Run: ruff check src/ && pytest tests/ -v
  Report: lint status + test count (X passed, Y failed, Z skipped)

Never move to the next function while the current one fails either check."
>>>>>>> refs/remotes/origin/main
```

---

<<<<<<< HEAD
## 5. Verification Loop Design Principles

### Principle 1: Test Output Must Flow Back to Claude

```
WRONG: Claude generates code → you run tests → you paste results back manually
→ Breaks the loop, wastes your time

CORRECT: Claude runs tests in the same session
"Implement X. Then run: pytest [test path] -x. Fix failures. All tests must pass."
→ Claude reads test output directly, closes the loop autonomously
```

### Principle 2: Atomic Verification Scope

```
WRONG: Run all 400 tests after every change
→ Slow feedback, hard to isolate which change broke what

CORRECT: Run the smallest test set that covers the current change
Function scope: pytest tests/unit/test_service.py::TestClass::test_function -v
Module scope: pytest tests/unit/test_service.py -x
Integration: pytest tests/integration/ (after unit all pass)
Full suite: pytest (only at the end, before PR)
```

### Principle 3: Stopping Conditions

```
Stop and ask for human input when:
  - Same test has failed 3+ times with different fixes (underlying design problem)
  - Fixing test A breaks test B (architectural conflict)
  - Test requires changing the public API (spec problem, not implementation problem)
  - Test failure reveals infrastructure issue (DB not running, env var missing)

Report format when stopping:
  "Stopped verification loop. Reason: [specific reason].
  Test that keeps failing: [test name]
  Failure: [last failure message]
  Hypothesis: [what's actually wrong]
  Needs: [human decision / infrastructure setup / spec clarification]"
=======
## 4. Refactor-Safe Loop

```
"Refactor @file:src/services/[service].py to [goal].

Safety protocol:
  Before starting:
    Record: pytest tests/ -v (baseline pass count)

  For each refactoring step:
    1. Make ONE structural change (extract a function, rename, extract a class)
    2. Run: pytest tests/ -x
    3. If same tests pass: commit this step and continue
    4. If any test now fails: UNDO the change (git checkout [file])
       Try a different approach that preserves the same behavior
    5. Never change logic — only structure

Stopping conditions:
  - Goal achieved with all original tests passing: done
  - A structural change cannot be made without changing behavior: stop and report
  - 3 failed attempts on the same refactoring step: stop and ask"
>>>>>>> refs/remotes/origin/main
```

---

<<<<<<< HEAD
## 6. Revision Checklist

- [ ] Understands verification-driven = Claude runs tests, reads output, fixes, repeats
- [ ] Enforces "fix implementation, never tests" as a non-negotiable rule
- [ ] Uses all 6 verification gates: lint, type check, unit, integration, security, performance
- [ ] Implements verification-first (tests before implementation) for critical code
- [ ] Test output flows directly back to Claude (Claude runs tests, not you)
- [ ] Uses atomic test scope (function → module → integration → full suite)
- [ ] Has stopping conditions defined for every verification loop
=======
## 5. Migration-Safe Loop

```
"Run the database migration for [feature].

Pre-migration:
  1. Backup status: confirm we're in development/staging (NEVER production via Claude)
  2. Check current state: alembic current
  3. Show migration script: @file:alembic/versions/[migration].py

Migration:
  4. Run: alembic upgrade [revision]
  5. Verify: alembic current (should show new revision)
  6. Test: pytest tests/integration/ -x (integration tests against new schema)

On failure:
  7. Run: alembic downgrade -1
  8. Report: what failed and what the schema state is now

Constraint: Do NOT run migrations against the production database connection string.
Always verify DATABASE_URL points to development/staging before running."
```

---

## 6. CI Verification Loop

```
"Verify this CI pipeline runs correctly.

Step 1: Push the changes to a PR branch
Step 2: Observe the GitHub Actions run
  [If using GitHub MCP: "Check the CI run status using the GitHub MCP tool"]
Step 3: If all checks pass: done. Report the check names and statuses.
Step 4: If any check fails:
  a. Show me the failing step's output
  b. Diagnose: root cause of the failure
  c. Propose the fix
  d. After fix: verify the next CI run passes

Common CI failures and their first fix:
  'Module not found': dependency not in requirements.txt or package.json
  'Permission denied': action version pinned to SHA that changed
  'Environment variable': secret not set in repo settings"
```

---

## 7. Verification Standards

### What "Done" Means in a Verification Loop

```
A task is done when ALL of these pass, not just tests:

1. Tests pass: pytest [scope] exits with 0
2. Lint passes: ruff check [scope] finds no errors
3. Type check (if applicable): mypy [scope] finds no errors
4. No new warnings added (check diff)
5. All ORIGINAL tests still pass (nothing regressed)

Never accept "it works" without running the checks.
Never accept "tests pass" without checking lint.
```

### The Anti-Pattern: Accepting First Output

```
What developers do: Claude generates code → looks plausible → accept all → commit
Problem: code that compiles ≠ code that's correct

What pros do: Claude generates code → run tests → tests fail → Claude fixes →
              run tests again → tests pass → run lint → lint clean → commit

The extra 5 minutes of verification prevents the 2-hour debug session later.
```

---

## 8. Revision Checklist

- [ ] Every agent session has an explicit verification step (not "generate and done")
- [ ] Uses test-first: tests exist before implementation starts
- [ ] Knows the 4 components of a verification loop
- [ ] Can write a build-lint-test pipeline loop prompt
- [ ] Uses the refactor-safe loop pattern (never change behavior)
- [ ] Defines "done" as: tests + lint + no regression (not just "it compiles")
- [ ] Never commits code that hasn't passed at minimum: tests + lint
>>>>>>> refs/remotes/origin/main
