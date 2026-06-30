# Verification-Driven Workflows — Gold Sheet

> **Track**: Claude Mastery Track — Group 4: Pro / Production Level
> **File**: 4 of 5 (Track File #24)
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
```

---

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
```

---

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
```

---

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
```

---

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
```

---

## 6. Revision Checklist

- [ ] Understands verification-driven = Claude runs tests, reads output, fixes, repeats
- [ ] Enforces "fix implementation, never tests" as a non-negotiable rule
- [ ] Uses all 6 verification gates: lint, type check, unit, integration, security, performance
- [ ] Implements verification-first (tests before implementation) for critical code
- [ ] Test output flows directly back to Claude (Claude runs tests, not you)
- [ ] Uses atomic test scope (function → module → integration → full suite)
- [ ] Has stopping conditions defined for every verification loop
