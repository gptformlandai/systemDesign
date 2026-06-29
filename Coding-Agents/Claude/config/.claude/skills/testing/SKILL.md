# Testing Skill

## When to Invoke
Apply this skill automatically when the task involves:
- Writing unit tests for any function or class
- Performing test gap analysis
- Generating integration tests
- Reviewing test quality or completeness
- Fixing failing tests (fix implementation, not tests)

## Workflow

1. Read the implementation file to understand actual behavior (not assumed intent)
2. Identify all public methods (ignore private/internal implementation details)
3. For each public method, generate:
   - Happy path test (normal inputs, expected behavior)
   - At least 2 error condition tests (exceptions, invalid inputs)
   - At least 2 edge case tests (None, empty, boundary values, zero, negative)
4. Identify all external dependencies — mock every one of them
5. Run generated tests and fix any setup issues
6. Report: test count, pass rate, gap analysis

## Quality Standards
- Test names: test_<function>_<scenario>_<expected_outcome>
- One logical assertion per test (multiple related asserts OK)
- Tests must test BEHAVIOR, not implementation details
  WRONG: verify that validate_email() is called inside create_user()
  RIGHT: verify that create_user() with invalid email raises ValidationError
- All mocks verified: not just that they returned, but that they were called correctly
- Complete test file with all imports — ready to run as-is

## Constraints
- NEVER modify the implementation to make tests pass
- NEVER test private methods or internal state
- NEVER share state between tests (each test is independent)
- If a test reveals a real bug: note it but don't fix the implementation

## Output Format
1. Complete test file (all imports at top, tests grouped by function)
2. Brief comment above each test describing what it validates
3. Gap analysis: list of paths not covered
4. Test results: X passed, Y failed, Z skipped (after running)
