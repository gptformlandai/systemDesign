# Verification-Driven Workflows — Gold Sheet

> **Track**: Claude Mastery Track — Group 4: Pro / Production Level
> **File**: 4 of 5 (Track File #24)
> **Read after**: Debugging-Claude-Handbook-Gold-Sheet.md

---

## Core Principle

```
Claude is a first-draft generator. Verification loops are the quality gate.

Without verification: Claude generates code that looks right but fails at runtime.
With verification: Claude generates → verifies → fixes → verifies again → done.

The difference: you receive working code, not code that needs manual testing.
```

---

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
```

---

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
```

---

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
```

---

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
```

---

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
