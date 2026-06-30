# Refactoring Skill

## When to Invoke
Apply this skill automatically when the task involves:
- Improving code quality without changing observable behavior
- Extracting functions, classes, or modules
- Reducing code duplication
- Applying design patterns to existing code
- Improving readability or reducing complexity
- Splitting a class or function that does too many things
- Keywords: "refactor", "clean up", "extract", "simplify", "restructure"

## Pre-Condition: Establish Baseline
Before any refactoring change:
1. Run existing tests. Record: X tests pass, Y fail.
   If there are NO tests: generate minimal tests first using the Testing skill.
   Never refactor untested code — you have no way to verify behavior is preserved.
2. Record what the failing tests are (they should still fail after refactoring — that's expected)

## Workflow

### Step 1 — Understand Before Changing
- Read the target code fully
- List what each function/class does (not how)
- Identify the one specific refactoring goal from the request
- Ask yourself: what is the MINIMAL change to achieve this goal?

### Step 2 — Plan the Refactoring Sequence
Refactoring must be done in small, independently-testable steps.
One change = one pass through the test suite.

Example sequence for "extract email validation":
  Pass 1: Copy email validation logic to new validate_email() function (no behavior change)
  Pass 2: Replace inline logic with call to validate_email() (still same behavior)
  Pass 3: Run tests to confirm nothing broke
  
Not: one big rewrite that does everything at once.

### Step 3 — Execute One Step at a Time
For each refactoring step:
  a. Make the single change
  b. Run: [test command]
  c. If tests pass with same count: proceed to next step
  d. If any previously-passing test now fails: UNDO this step, try a different approach
  e. Never proceed to the next step with a regression

### Step 4 — Report
- What changed (one bullet per logical change)
- Why (the reason for each change)
- Before/after line count if meaningful
- Test results (must show same tests passing as baseline)

## Hard Constraints (enforced on every refactoring)
- Public API (method signatures, return types, exception types) must remain identical
  UNLESS the request explicitly says to change the API
- Existing tests must continue to pass — NEVER modify tests to match refactored code
- Maximum 1 new class per refactoring session (prevent over-abstraction)
- No new external dependencies
- No TODOs — either implement or leave the existing code unchanged

## Output Format
Unified diff (not full file rewrite)
Refactoring log: [step] → [change] → [test result]
Final: X tests pass (same as baseline), Y tests fail (unchanged from baseline)

## When to Stop
- The goal is achieved
- OR: the next refactoring step would require changing the public API (stop and flag)
- OR: a step cannot be made without tests failing (stop and flag — may indicate design issue)
