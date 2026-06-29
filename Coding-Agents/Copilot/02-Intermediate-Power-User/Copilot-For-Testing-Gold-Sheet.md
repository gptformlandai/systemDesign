# Copilot For Testing — Gold Sheet

> **Track**: Copilot Mastery Track — Group 2: Intermediate Power User
> **File**: 5 of 7 (Track File #11)
> **Audience**: Developers using Copilot to accelerate test writing
> **Read after**: Agent-Mode-Safe-Usage-Gold-Sheet.md

---

## 1. Practical Impact Meter

| Topic | Impact | Why Developers Skip Tests |
|---|---|---|
| Unit test generation — what makes a good prompt | ★★★★★ | Bad prompts generate tests that test implementation not behavior |
| Test gap analysis — finding what's missing | ★★★★★ | Devs don't know what they're not testing until production breaks |
| Test-first with Copilot — the correct order | ★★★★☆ | Most devs generate tests AFTER implementation — tests then describe bugs not requirements |
| Mocking strategy — what to mock and why | ★★★★☆ | Bad mocking produces tests that always pass regardless of code correctness |
| Edge case generation — the hidden wins | ★★★★☆ | Happy-path-only tests miss 80% of production bugs |
| Per-language patterns: Python, Java, TypeScript | ★★★★☆ | Each language has framework idioms Copilot should follow |
| Flaky test diagnosis | ★★★★☆ | Copilot can help identify WHY a test is flaky — often ordering or time dependencies |

---

## 2. Test Generation — Core Prompts

### The Gold Standard Test Generation Prompt

```
In Chat or as a prompt file, use this structure:

"Generate [framework] unit tests for:
#selection (or #file:path)

Cover:
1. Happy path — successful execution with valid inputs
2. Error cases — at least 2 distinct error conditions
3. Edge cases — empty, None, boundary values, max/min lengths
4. [Domain-specific]: [any scenarios specific to this function's purpose]

Rules:
- Test names: test_<function>_<scenario>_<expected_outcome>
- One logical assertion per test (multiple assert statements for related properties OK)
- Mock all external dependencies: [list: HTTP calls, email, DB, time/date]
- Do NOT test implementation details — test observable behavior
- If the function has no error handling, note that in a comment — do not fabricate it

Output: complete test file with imports, ready to run"
```

### Python / pytest Example

```python
# Implementation to test:
async def create_user(session: AsyncSession, email: str, name: str) -> User:
    if not email or "@" not in email:
        raise ValueError("Invalid email format")
    existing = await session.execute(select(User).where(User.email == email))
    if existing.scalar_one_or_none():
        raise DuplicateEmailError(f"Email {email} already exists")
    user = User(email=email, name=name, active=True)
    session.add(user)
    await session.commit()
    await session.refresh(user)
    return user
```

```
Prompt:
"Generate pytest-asyncio unit tests for #selection.
Cover:
1. Happy path: valid email and name → returns User with id and active=True
2. Invalid email (no @) → raises ValueError
3. Empty email → raises ValueError
4. Duplicate email → raises DuplicateEmailError
5. Very long email (255+ chars) → behavior (does it validate length?)

Use:
- pytest-asyncio with @pytest.mark.asyncio
- AsyncMock for the database session
- unittest.mock.patch for external services if any
- Fixtures: create_session() → returns AsyncMock(spec=AsyncSession)

Test names follow: test_create_user_<scenario>_<expected>"
```

---

## 3. Test-First Workflow with Copilot

### The Correct Order

```
WRONG (implementation-first, then tests):
  1. Write implementation
  2. Ask Copilot to generate tests
  3. Tests describe the implementation, including its bugs
  4. All tests pass, bugs are "expected behavior"

CORRECT (test-first with Copilot):
  1. Write the function SIGNATURE only (name, parameters, return type, docstring)
  2. Ask Copilot to generate tests based on the docstring + requirements
  3. Review and add missing test cases
  4. Let Copilot generate the implementation to pass the tests
  5. Run tests → fix failures → commit

Why test-first is better:
  Tests describe REQUIREMENTS, not implementation details.
  A bug in implementation shows up as a test failure.
  A bug in implementation-first tests is invisible — tests pass, bug ships.
```

### Test-First Example

```python
# Step 1: Write signature only
async def process_refund(
    session: AsyncSession,
    order_id: int,
    amount: float,
    reason: str,
) -> RefundResult:
    """
    Process a refund for an order.
    
    Args:
        session: Database session
        order_id: ID of the order to refund
        amount: Amount to refund (must be > 0 and <= order total)
        reason: Required reason for the refund
    
    Returns:
        RefundResult with status='approved' or status='rejected'
    
    Raises:
        OrderNotFoundError: If order_id doesn't exist
        InvalidRefundAmount: If amount <= 0 or > order.total
        ValueError: If reason is empty
    """
    raise NotImplementedError  # Copilot will implement this after tests
```

```
Step 2 — Generate tests from the signature:
"Generate pytest-asyncio tests for #selection based on the docstring.
Cover all documented raises and return cases.
Mock: session (AsyncMock), any calls to payment processors."

Step 3 — Review and add edge cases you think of
Step 4 — Ask Copilot to implement the function to pass the tests
Step 5 — Run tests → iterate until all pass
```

---

## 4. Test Gap Analysis — Finding What's Missing

### The Gap Analysis Prompt

```
"Analyze the test coverage in #file:tests/unit/test_user_service.py
against the implementation in #file:src/services/user_service.py.

Report:
1. Functions/methods in the service that have NO tests
2. Functions/methods that have happy-path only (no error or edge case tests)
3. Error paths in the implementation that are not tested
4. Boundary conditions not tested (max values, empty collections, None inputs)
5. Any mocking gaps (external calls that bypass mocks in tests)

Format your response as a prioritized TODO list — most critical gaps first."
```

### Test Gap Analysis — Codebase Level

```
"Using #codebase, identify:
1. All Python files in src/ that have NO corresponding test file
2. Files where test file exists but coverage appears < 50% based on function count

Present as a table: File | Has Tests | Estimated Coverage | Priority to Add Tests"
```

---

## 5. Mocking Strategy

### What to Mock (and What Not to)

```
ALWAYS mock:
  - External HTTP calls (third-party APIs, webhooks, payment processors)
  - Email sending
  - SMS sending
  - File system operations in critical paths
  - Time/date (datetime.now(), time.time()) when testing time-dependent logic
  - Random number generation when testing probabilistic logic
  - Database (for unit tests — use real DB only for integration tests)

NEVER mock:
  - The code you're testing (the subject under test)
  - Core language/library behavior (don't mock list.append())
  - Simple data transformations that have no external dependencies

Rule of thumb:
  If a test can fail due to something OUTSIDE the code being tested → mock it.
  If a test requires a running server or DB to pass → it's an integration test, not a unit test.
```

### Python Mocking Patterns

```python
# Pattern 1 — Mock an async method on a class
from unittest.mock import AsyncMock, patch, MagicMock

@pytest.mark.asyncio
async def test_send_welcome_email_called_on_registration():
    mock_email_service = AsyncMock()
    service = UserService(db_session=AsyncMock(), email_service=mock_email_service)
    
    await service.register_user("alice@example.com", "Alice")
    
    mock_email_service.send_welcome.assert_called_once_with("alice@example.com")

# Pattern 2 — Mock time.now() for deterministic date tests
from unittest.mock import patch
from datetime import datetime

@pytest.mark.asyncio
async def test_token_expires_in_one_hour():
    fixed_now = datetime(2024, 1, 15, 12, 0, 0)
    with patch("src.utils.jwt.datetime") as mock_dt:
        mock_dt.utcnow.return_value = fixed_now
        token = create_jwt_token(user_id=1)
    
    payload = decode_token(token)
    expected_expiry = datetime(2024, 1, 15, 13, 0, 0)
    assert payload["exp"] == expected_expiry.timestamp()

# Pattern 3 — Mock HTTP with respx (for httpx)
import respx
import httpx

@pytest.mark.asyncio
async def test_fetch_user_from_external_api():
    with respx.mock:
        respx.get("https://api.example.com/users/1").mock(
            return_value=httpx.Response(200, json={"id": 1, "name": "Alice"})
        )
        result = await fetch_external_user(user_id=1)
    
    assert result.name == "Alice"
```

---

## 6. Edge Case Generation

### The Edge Case Prompt

```
"For the function #selection, generate a list of edge cases I should test.

Include:
- Boundary conditions (what happens at min/max valid values?)
- Empty/null inputs (empty string, None, empty list, empty dict)
- Type boundary (what if an int is passed as string?)
- Concurrency (if called simultaneously, could there be a race condition?)
- Resource exhaustion (what if this is called 1000 times rapidly?)
- Character encoding (unicode, special characters, SQL injection attempts)
- Numeric edge cases (zero, negative, very large, float precision)

Format: bulleted list with a brief description of each scenario and why it matters"
```

---

## 7. Per-Language Test Patterns

### Java / Spring Boot

```
Prompt:
"Generate JUnit 5 unit tests for #selection.

Use:
- @ExtendWith(MockitoExtension.class)
- @Mock for UserRepository and EmailService
- @InjectMocks for UserService
- Mockito.when().thenReturn() for happy path
- Mockito.when().thenThrow() for error cases
- AssertJ assertions (assertThat())
- assertThrows() for exception tests

Cover: [list scenarios]"
```

### TypeScript / Jest

```
Prompt:
"Generate Jest unit tests for #selection (TypeScript).

Use:
- jest.fn() for dependencies
- jest.spyOn() for partial mocking
- async/await with expect.resolves and expect.rejects
- Type-safe mocks with jest.MockedFunction

Cover: [list scenarios]
Do not use require() — use import syntax throughout."
```

### React / Testing Library

```
Prompt:
"Generate React Testing Library tests for the #selection component.

Use:
- render() from @testing-library/react
- screen.getByRole(), getByText(), getByLabelText() (prefer accessible queries)
- userEvent.click(), userEvent.type() for interactions
- waitFor() for async state updates

Cover:
- Initial render (does it render without crashing?)
- User interactions (button clicks, form submission)
- Loading states
- Error states
- Successful data display

Do NOT use getByTestId unless absolutely necessary."
```

---

## 8. Flaky Test Diagnosis

### Flaky Test Diagnosis Prompt

```
"This test is intermittently failing. Help me identify why:

Test: #selection

Symptoms: [describe when it fails — always, randomly, only in CI, only sequentially]

The test failure message when it fails: [paste failure message]

Analyze:
1. Does this test depend on execution order? (shared state, module-level setup)
2. Does this test depend on timing? (sleep, timeouts, async race conditions)
3. Does this test depend on external resources? (real HTTP, real DB, file system)
4. Does this test use random data that could cause it to fail?
5. Does this test have side effects that affect other tests?

For each identified cause: explain the fix."
```

---

## 9. Revision Checklist

- [ ] Can write a complete test generation prompt covering all required scenarios
- [ ] Knows the test-first workflow with Copilot and why it's better
- [ ] Can perform a test gap analysis for a file and for a codebase
- [ ] Knows what to mock (external calls) and what NOT to mock (subject under test)
- [ ] Has the Python mocking patterns memorized: AsyncMock, patch, respx
- [ ] Can generate an edge case list for any function
- [ ] Knows the Jest, JUnit 5, and React Testing Library prompt patterns
- [ ] Can diagnose flaky tests with a structured Copilot prompt
