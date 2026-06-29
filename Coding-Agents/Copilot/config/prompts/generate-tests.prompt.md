---
name: Generate Tests
description: Generate comprehensive unit tests for selected Python code
---

Generate unit tests for the following code:

${selection}

Requirements:
- Framework: pytest with pytest-asyncio for async functions
- Cover:
  1. Happy path — successful execution with valid inputs
  2. At least 2 distinct error conditions (different error types)
  3. At least 2 edge cases (empty, None, boundary values, max/min)
- Test naming: test_<function_name>_<scenario>_<expected_outcome>
- One logical assertion per test (multiple assert for related properties is OK)
- Mock all external dependencies: HTTP clients, email, SMS, file I/O, external APIs
- For async code: @pytest.mark.asyncio decorator required
- For SQLAlchemy: use AsyncMock(spec=AsyncSession) for db_session
- For httpx: use respx.mock for mocking HTTP calls

Do NOT:
- Test implementation details (private method calls, internal state)
- Create tests that require a real running database or network
- Mock the code being tested

Output format:
- Complete test file ready to run (with all imports)
- Group related tests in a class named Test<ClassName> or Test<FunctionName>
- Brief comment above each test explaining what it validates
