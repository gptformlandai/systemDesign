---
applyTo: "tests/**,**/*.test.*,**/*.spec.*,**/__tests__/**"
---
# Testing Conventions

## Strategy
Unit tests: mock all external dependencies (DB, HTTP, email, SMS, file system, time).
Integration tests: use real infrastructure via Docker (Testcontainers or docker-compose).
Never call real external APIs in unit tests.
Never require a running server for unit tests.

## Naming
Pattern: test_<function_name>_<scenario>_<expected_outcome>
Examples:
  test_create_user_valid_input_returns_user_with_id
  test_create_user_duplicate_email_raises_conflict_error
  test_create_user_missing_name_raises_validation_error

## Structure
One logical assertion per test (multiple assert statements for related properties is OK).
Arrange / Act / Assert pattern — one blank line between each phase.
Test class names: Test<ClassOrFeatureName>

## Mocking
Mock at the system boundary (HTTP clients, DB sessions, email services, clocks).
Do NOT mock the code being tested (subject under test).
Do NOT mock standard library functions without a clear reason.

## Coverage
Every public method needs: happy path, at least one error case, at least one edge case.
Edge cases to always consider: empty input, None, zero, max value, concurrent calls.

## Do NOT
- Do not use sleep() in tests — use mocked clocks or event-based waits
- Do not share mutable state between tests — each test must be independent
- Do not write tests that only pass in a specific order
- Do not hard-code dates, times, or IDs — use fixtures or factories
- Do not assert on implementation details (private method calls, internal state)
