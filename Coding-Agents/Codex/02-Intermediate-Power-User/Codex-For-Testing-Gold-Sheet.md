# Codex For Testing — Gold Sheet

> **Track**: Codex Mastery Track — Group 2: Intermediate Power User
> **File**: 5 of 7 (Track File #11)
> **Audience**: Developers who want to use Codex to build a real test suite, not just generate boilerplate
> **Read after**: Approval-Policy-Modes-Gold-Sheet.md

---

## Practical Impact Meter

| Topic | Impact | Why Developers Miss This |
|---|---|---|
| Test-first loop (define tests before implementation) | ★★★★★ | Devs generate tests after; tests mirror the implementation instead of verifying intent |
| Gap analysis after generation | ★★★★★ | Generated tests cover happy path; gap analysis finds the 70% that's missing |
| Mocking strategy — mock external, not own logic | ★★★★★ | Over-mocking produces tests that pass but don't test anything real |
| Iteration loop: run → read output → fix | ★★★★☆ | Accepting tests without running them is how hidden failures get committed |
| Test naming as specification | ★★★☆☆ | test_create_order_zero_amount_raises_value_error tells you the spec at a glance |

---

## ⭐ Beginner Tier — Start Here

### B1: Generate your first real tests

Pick any function in your project that has no tests.

```bash
codex --approval-policy auto-edit \
  "Generate tests for process_payment() in src/payments/service.py.
   Cover: successful payment, zero amount (should reject), negative amount (should reject),
   invalid currency (should reject), database connection failure.
   Use pytest. Mock the database client only.
   Naming: test_process_payment_[scenario]_[expected]
   Run: pytest tests/test_payment_service.py -v"
```

After generation — check each test:
```
□ Does it assert a specific return value or exception? (not just "didn't crash")
□ Is the mock appropriate? (external dependency, not own code)
□ Is the test name readable? (tells you what it tests)
```

### B2: Run the gap analysis

After generating tests, immediately follow up:

```bash
codex "I just generated tests for process_payment(). 
       What is still not tested?
       List: uncovered error paths, missing boundary values, untested interactions.
       Do not generate code yet — just list the gaps."
```

Add the gaps to the test file manually or run the generation again for the missing cases.

---

## 1. Test-First Development With Codex

The most powerful Codex testing pattern: define the tests BEFORE the implementation.

```bash
# Step 1: Define the specification as tests
codex --approval-policy auto-edit \
  "Write the test file for a new create_order() function in src/orders/service.py.
   The function does not exist yet — we're writing tests first (TDD).
   
   Specification:
   - Accepts: user_id (int), items (list of {product_id, quantity}), currency (str)
   - Returns: Order object with id, total_amount, status='pending'
   - Validates: items not empty, quantity > 0 for each item, currency in USD/EUR/GBP
   - Raises: ValidationError for invalid input, ServiceError for database failures
   - Side effect: decrements inventory for each item
   
   Create: tests/test_order_service.py with 8+ tests
   Mock: database operations, inventory service
   Run: pytest tests/test_order_service.py -v
   Expected: all tests collected but FAIL (implementation doesn't exist yet)"

# Step 2: Implement to make the tests pass
codex --approval-policy auto-edit \
  "Implement create_order() in src/orders/service.py to make all tests pass.
   Tests are in tests/test_order_service.py — implement only, do not modify tests.
   Verification: pytest tests/test_order_service.py -v (all must pass)"
```

Why this matters: the tests become the specification, not the implementation's mirror.

---

## 2. The Mocking Strategy

### Mock only external dependencies

```python
# CORRECT: mock external systems
@patch('src.payments.service.stripe_client.charge')     # ✅ external HTTP
@patch('src.payments.service.db.session')               # ✅ database
@patch('src.payments.service.email_client.send')        # ✅ external email service
def test_process_payment_success(mock_email, mock_db, mock_stripe):
    ...

# WRONG: mock own service logic
@patch('src.payments.service.validate_amount')          # ❌ own logic
@patch('src.payments.service.calculate_fees')           # ❌ own logic
def test_process_payment_success(mock_calc, mock_validate):
    # This test doesn't test validate_amount or calculate_fees
    # If those functions have bugs, this test won't catch them
    ...
```

### Codex mocking prompt

```bash
codex --approval-policy auto-edit \
  "Generate tests for send_order_confirmation() in src/notifications/service.py.
   Mocking rules:
   - Mock: external dependencies (email_client, sms_client, database)
   - Do NOT mock: own helper functions (format_message, validate_email, etc.)
   - Use pytest-mock (fixture 'mocker') or unittest.mock
   - Every mock assertion should verify it was called with correct arguments"
```

---

## 3. The Iteration Loop: Run → Read → Fix

```bash
# Pattern: give Codex the test result and let it iterate

# Step 1: Generate tests
codex --approval-policy auto-edit \
  "Generate tests for authenticate_user() in src/auth/service.py.
   Run: pytest tests/test_auth_service.py -v
   Fix any failures — do not modify tests."

# If tests still fail after first attempt:
pytest tests/test_auth_service.py -v 2>&1 | head -50

codex "The following tests are failing:
       [paste the pytest output]
       
       Fix the implementation in src/auth/service.py.
       Do NOT modify the tests.
       Verification: pytest tests/test_auth_service.py -v (all must pass)"
```

---

## 4. Test Gap Analysis — The Complete Pattern

```bash
# Step 1: Generate initial tests
codex --approval-policy auto-edit \
  "Generate tests for validate_order() in src/orders/validator.py"

# Step 2: Immediate gap analysis
codex "Analyze the tests I just generated for validate_order().
       Gaps report (be specific):
       1. Untested error conditions (what exceptions can the function raise that aren't tested?)
       2. Untested boundary values (what edge values aren't covered?)
       3. Missing interaction tests (what side effects aren't verified?)
       4. Missing negative tests (what should fail but isn't tested?)
       5. Tautological tests (tests that can't fail regardless of implementation)
       
       List gaps — do not generate code yet."

# Step 3: Generate the missing tests
codex --approval-policy auto-edit \
  "Add the missing tests identified:
   [paste the gap list]
   Add them to tests/test_order_validator.py.
   Run: pytest tests/test_order_validator.py -v"
```

---

## 5. Identifying Tautological Tests

A tautological test always passes regardless of whether the implementation is correct.

```python
# TAUTOLOGICAL — this test can never fail
def test_create_user_returns_something():
    result = create_user("test@example.com", "password123")
    assert result is not None  # any non-None return makes this pass

# MEANINGFUL — this test fails if the return is wrong
def test_create_user_returns_user_with_correct_email():
    result = create_user("test@example.com", "password123")
    assert result.email == "test@example.com"
    assert result.id is not None
    assert result.created_at is not None

# TAUTOLOGICAL — tests that the function doesn't raise (almost always useless)
def test_calculate_fee_no_exception():
    try:
        calculate_fee(100.0, "USD")
    except Exception:
        pytest.fail("Unexpected exception")

# MEANINGFUL — tests the actual return value
def test_calculate_fee_usd_standard():
    fee = calculate_fee(100.0, "USD")
    assert fee == 2.5  # 2.5% fee on USD transactions
```

### Codex prompt to detect tautological tests

```bash
codex "Review the tests in tests/test_payment_service.py.
       Identify any tautological tests — tests that would pass even if the implementation
       were completely wrong.
       For each tautological test: explain why it's tautological and provide a better assertion."
```

---

## 6. AsyncMock vs MagicMock

```python
# When the function being mocked is async: use AsyncMock
from unittest.mock import AsyncMock, patch

# WRONG: using MagicMock for async function
@patch('src.payments.client.charge', MagicMock(return_value={"status": "success"}))
async def test_process_payment():
    result = await process_payment(100, "USD")  # will fail — MagicMock is not awaitable

# CORRECT: AsyncMock for async function
@patch('src.payments.client.charge', AsyncMock(return_value={"status": "success"}))
async def test_process_payment():
    result = await process_payment(100, "USD")  # works correctly
    assert result["status"] == "success"
```

### Codex prompt for async tests

```bash
codex --approval-policy auto-edit \
  "Generate tests for async functions in src/notifications/service.py.
   Note: use AsyncMock (not MagicMock) for any async dependency mocks.
   Use pytest-asyncio for async test functions.
   Run: pytest tests/test_notification_service.py -v"
```

---

## 7. Running Tests as Part of the Verification Loop

```bash
# The complete test generation workflow
FUNCTION="create_subscription"
FILE="src/billing/service.py"
TEST_FILE="tests/test_billing_service.py"

# 1. Generate
codex --approval-policy auto-edit \
  "Generate tests for $FUNCTION() in $FILE.
   Cover: happy path, all validation errors, service unavailability.
   Mock: external billing API, database.
   Run: pytest $TEST_FILE -v"

# 2. Gap analysis
codex "What's missing in the tests I just generated for $FUNCTION?
       Focus on: error paths, boundary values, security edge cases (zero, negative, overflow)."

# 3. Add missing tests  
codex --approval-policy auto-edit \
  "Add the missing test cases to $TEST_FILE.
   Run: pytest $TEST_FILE -v (all must pass)"

# 4. Verify coverage
codex "Estimate test coverage for $FUNCTION based on the tests in $TEST_FILE.
       Which branches are not covered?"
```

---

## Interview Traps

```
TRAP: "Codex-generated tests have comprehensive coverage — no gap analysis needed"
TRUTH: Generated tests cover the happy path well and miss most error paths and edge cases.
       Gap analysis is mandatory, not optional. The default output looks comprehensive
       but is typically missing 50-70% of meaningful test coverage.

TRAP: "If tests pass after generation, they're testing the right things"
TRUTH: Ask Codex immediately after generation: "Which of these tests would still pass if
       the function returned None?" Tests that pass with a broken implementation are
       tautological — they test the mock, not the behavior.

TRAP: "Mocking my own service layer is required to properly isolate the API layer"
TRUTH: Mocking your own code produces tests that pass even when your code is broken.
       The service layer runs fast in tests — there's no cost to running the real thing.
       Only mock: external HTTP calls, database drivers, third-party cloud SDKs.
```

---

## Revision Checklist

- [ ] Can run a test-first TDD loop with Codex (define tests before implementation)
- [ ] Can run a gap analysis after every test generation
- [ ] Mocking strategy: only mock external dependencies
- [ ] Can detect and replace tautological tests
- [ ] Know when to use AsyncMock vs MagicMock
- [ ] Run the iteration loop: generate → run → read failures → fix → run again
