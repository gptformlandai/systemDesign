# Verification-Driven Workflows — Gold Sheet

> **Track**: Codex Mastery Track — Group 4: Pro / Production Level
> **File**: 4 of 5 (Track File #24)
> **Audience**: Developers who want Codex to iterate until tests pass, not just until "done"
> **Read after**: SDLC-Automation-Gold-Sheet.md

---

## Practical Impact Meter

| Topic | Impact | Why Developers Miss This |
|---|---|---|
| Verification command as the "done" signal | ★★★★★ | Without it, Codex stops when it thinks it's done. With it, Codex knows when it IS done |
| Test-first loop — tests before implementation | ★★★★★ | Tests after implementation mirror the implementation instead of specifying intent |
| Lint loop — lint must pass before moving on | ★★★★☆ | Accepting lint failures now creates debt; fix immediately with the loop |
| The complete build loop: implement → lint → test | ★★★★☆ | Running each separately loses the compound benefit of the three together |
| "Done" definition: tests + lint + no regression | ★★★★☆ | "It runs" is not done. "Compiles" is not done. Verification defines done. |

---

## ⭐ Beginner Tier — Start Here

### B1: Your first verification loop

```bash
# Notice the difference between these two

# WITHOUT verification: Codex stops when it thinks it's done
codex --approval-policy auto-edit "add input validation to create_user()"

# WITH verification: Codex iterates until tests pass
codex --approval-policy auto-edit \
  "Add input validation to create_user() in src/users/service.py.
   Validate: email format, age 0-150, name 1-100 chars.
   Raise ValueError for each invalid case.
   Verification: pytest tests/test_user_service.py -x
   Iterate until all tests pass. Do not modify test files."
```

Run the second version. Watch Codex implement, run the tests, read the failure output, fix, and retry.
That iteration loop is the core pattern of all verification-driven workflows.

---

## 1. The Core Principle

```
Traditional approach:
  Codex implements → returns "done" → you run tests → you debug failures

Verification-driven approach:
  Codex implements → Codex runs verification command → Codex reads output
  → if failures: Codex diagnoses and fixes → runs verification again
  → repeats until verification passes → reports done

The difference:
  In the traditional approach, YOU are the verification loop.
  In verification-driven, CODEX is the verification loop.
  You review the result at the end, not every iteration in the middle.
```

---

## 2. The Test-First Loop

```bash
# Phase 1: Write tests for behavior that doesn't exist yet
codex --approval-policy auto-edit \
  "Write tests for process_refund() in src/payments/service.py.
   The function does not exist yet — these tests define the specification.
   
   Test cases:
   - Full refund: returns Refund object with status='processed', amount=original_amount
   - Partial refund: amount <= original_amount, returns correct partial amount
   - Invalid amount: amount > original_amount, raises ValidationError
   - Already refunded: raises AlreadyRefundedError
   - Payment gateway failure: raises ServiceError
   
   Create: tests/test_payment_service.py::TestProcessRefund
   Run: pytest tests/test_payment_service.py -v
   Expected result: tests collected but FAIL (function doesn't exist yet)"

# Verify tests are collected and failing (not erroring at collection time)
pytest tests/test_payment_service.py --collect-only

# Phase 2: Implement to make tests pass (new session)
codex --approval-policy auto-edit \
  "Implement process_refund() in src/payments/service.py.
   Test specification is in tests/test_payment_service.py::TestProcessRefund.
   Implement ONLY — do not modify test files.
   Verification: pytest tests/test_payment_service.py::TestProcessRefund -v
   Iterate until all tests pass."
```

---

## 3. The Build-Lint-Test Loop

```bash
# The complete verification loop for any implementation task
codex --approval-policy auto-edit \
  "Implement [task] in [file].
   
   Process — complete in this order:
   1. Implement the change
   2. Run: ruff check [file] — fix all lint errors before continuing
   3. Run: ruff format [file] — fix all formatting issues
   4. Run: pytest [test file] -x — fix test failures before reporting done
   5. Report: lint status, test results, any remaining issues
   
   Constraints:
   - Do not modify test files
   - Fix lint errors before running tests (not after)
   
   Done = lint passes + tests pass + no regressions"
```

### Makefile target for the build loop

```makefile
# Makefile
codex-build:
	@test -n "$(TASK)" || (echo "Usage: make codex-build TASK='description' FILE=src/..."; exit 1)
	codex --approval-policy auto-edit \
	  "$(TASK). Process: implement → ruff check $(FILE) → ruff format $(FILE) → pytest -x. \
	   Fix failures at each step before proceeding. Done = lint+tests pass."
```

---

## 4. The Refactor-Safe Loop

```bash
# Refactoring with zero behavior change guarantee
codex --approval-policy auto-edit \
  "Refactor [describe the structural change] in [file].
   
   Safety protocol:
   Step 1: Run: pytest [test file] -x — record baseline (all must pass)
   Step 2: Make ONE structural change
   Step 3: Run: pytest [test file] -x — must still pass
   Step 4: If failure: undo that change and try a different approach
   Step 5: Repeat for each structural change
   
   Constraint: behavior must not change — same inputs → same outputs after refactor.
   Do not modify test files.
   Done = all tests from Step 1 still pass after all structural changes."
```

---

## 5. The CI Failure Loop

```bash
# Fixing a CI failure with verification
CI_OUTPUT="[paste exact CI failure output here]"

codex --approval-policy auto-edit \
  "CI pipeline is failing. Here is the output:
   $CI_OUTPUT
   
   Process:
   1. Diagnose: what is the root cause?
   2. Fix: apply the minimum change needed
   3. Verify locally: run the exact command that failed in CI
   4. Report: root cause, fix applied, verification result
   
   Constraints:
   - Do not modify test files
   - Do not change unrelated code
   - The fix must reproduce the CI environment (check: Python version, env vars)
   
   Done = the exact CI command passes locally."
```

---

## 6. Defining "Done" — The Three Conditions

```
A task is done when ALL THREE are true:

1. Tests pass
   pytest -x (or your framework's equivalent)
   Zero failures. Zero errors. Not "skipping" the tests.

2. Lint passes
   ruff check . (or eslint / golint)
   Zero errors. Warnings addressed or explicitly accepted.

3. No regressions
   pytest (entire test suite — not just the new tests)
   Nothing that was passing before now fails.

"It compiles" is NOT done.
"My new test passes" is NOT done.
"Codex said it's done" is NOT done.

These three conditions — together — define done.
```

---

## 7. Anti-Patterns in Verification

```
ANTI-PATTERN: Accept first output without verification
  "Codex looks right" → commit
  Result: untested code, potential hidden failures
  Fix: always include a verification command and run it

ANTI-PATTERN: Modify tests to make them pass
  Test expects 400 → Codex changes test to expect 200 → "test passes"
  Result: test now mirrors wrong behavior, bug is hidden
  Fix: "do not modify test files" in every implementation prompt and in AGENTS.md

ANTI-PATTERN: Run only the new tests, not the full suite
  New test passes → commit
  Result: the new code broke 3 existing tests that weren't run
  Fix: run full test suite before committing (pytest without -k filter)

ANTI-PATTERN: Skip lint because "tests pass"
  Tests pass, but code has style violations and security warnings
  Result: lint errors accumulate; some warnings are security findings
  Fix: lint + test in sequence, both required for "done"

ANTI-PATTERN: Trust "0 tests failed" when 0 tests ran
  Empty test file → pytest: "no tests ran" → "tests pass"
  Fix: verify test count > 0 after generation (pytest --collect-only | grep "test session stats")
```

---

## Interview Traps

```
TRAP: "A verification command is best practice but not strictly required"
TRUTH: Without a verification command, Codex's stopping condition is its own judgment.
       Codex can have high confidence in wrong output. Tests are the objective signal
       that overrides subjective confidence. Verification is the loop control.

TRAP: "Running tests is the same as reviewing the diff"
TRUTH: Tests tell you behavior passes. Diff review tells you: which files changed,
       whether changes are in scope, and whether the implementation is sensible.
       A test passing and a diff showing a security regression can both be true simultaneously.
       Both gates are required.

TRAP: "If pytest passes, lint is optional — style enforcement isn't a blocker"
TRUTH: Lint is not style enforcement. It catches unused imports, shadowed variables,
       and in many configs, actual security warnings (e.g., ruff's S-prefix rules).
       Lint + test is the complete verification gate. Either alone is incomplete.
```

---

## Revision Checklist

- [ ] Verification command in every implementation prompt
- [ ] Can run a test-first TDD loop where tests define the spec
- [ ] Build-lint-test loop runs in sequence (lint before test)
- [ ] Refactor-safe loop with baseline comparison at every step
- [ ] "Done" means: tests pass + lint passes + no regressions
- [ ] "Do not modify test files" in AGENTS.md permanently
