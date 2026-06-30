# Claude For Testing — Gold Sheet

> **Track**: Claude Mastery Track — Group 2: Intermediate Power User
> **File**: 5 of 7 (Track File #11)
> **Read after**: Claude-Code-CLI-Gold-Sheet.md

---

## Practical Impact Meter

| Topic | Impact | Why Developers Struggle |
|-------|--------|------------------------|
| Test-first generation — write tests before implementation | ★★★★★ | Devs generate implementation first, then ask Claude for tests that just validate the (possibly wrong) implementation |
| Test gap analysis — find untested error paths | ★★★★★ | Most generated tests cover only happy path; gaps are invisible until production |
| Mocking strategy — what to mock and what not to | ★★★★☆ | Wrong mocks produce tests that pass even when business logic is broken |
| Verification loop — Claude iterates until all tests pass | ★★★★★ | Without loops, Claude generates tests that don't run or fixes the wrong thing |
| Tautological test detection | ★★★★☆ | Tests that always pass regardless of implementation give false confidence |

---

## 1. Test-First Development with Claude

### The Correct Order

```
WRONG order (developer default):
  1. Implement the feature
  2. Ask Claude to write tests for it
  → Tests describe what the code DOES, not what it SHOULD DO.
  → Claude writes tests that pass by design — they can never catch the bug.

RIGHT order (test-first):
  1. Describe the requirements precisely
  2. Ask Claude to write FAILING tests
  3. Ask Claude to implement until tests pass
  → Tests define correct behavior independently of the implementation
  → Claude's implementation is forced to satisfy the tests, not the other way round
```

### Test-First Prompt

```
"I need to implement [function/class] that [description].

Requirements:
  - [requirement 1]
  - [requirement 2]
  - [edge case: what happens when input is empty/None/negative]
  - [error case: what should be raised and when]

Step 1 — TESTS ONLY. No implementation yet.
Generate pytest tests for this behavior.
The implementation file does NOT exist yet — the tests will fail on import.
Cover:
  - 1 happy path test per normal code path
  - 1 test per error/exception case
  - boundary values (zero, empty, max)

Output: only the test file. No implementation."
```

### Then Implement Against the Tests

```
"Now implement [function/class] to make these tests pass:

Tests: @file:tests/unit/test_[module].py

Rules:
  - Do NOT modify the test file under any circumstances
  - Run: pytest tests/unit/test_[module].py -x after each function
  - Report: function name + pass/fail status after each step
  - The task is complete only when ALL tests pass"
```

---

## 2. Test Generation from Existing Code

### Function-Level Test Generation

```
"Generate comprehensive pytest tests for this function:
@file:src/services/[module].py (focus on: [function name])

Think through this first:
  1. All valid inputs and expected outputs
  2. All invalid inputs that should raise exceptions
  3. Boundary values (empty string, zero, None, max value)
  4. Side effects that should or should not happen

Then generate:
  - 1 happy path test
  - 1 test per exception type raised
  - 1 test per boundary value
  - Descriptive test names: test_[function]_[condition]_[expected_result]

Style: pytest, AAA pattern (Arrange, Act, Assert), fixtures over repetition.
File: tests/unit/test_[module].py"
```

### Codebase-Level Coverage Audit

```
"Audit @file:src/services/ for test coverage gaps.

Report:
  1. Functions with NO test file
  2. Functions with tests but missing error path coverage
  3. Functions with tests that test implementation instead of behavior

Then generate tests for the top 3 most-critical untested functions.
Use the same fixture/mock patterns as @file:tests/conftest.py"
```

---

## 3. Test Gap Analysis

### Find What's Missing

```
"Analyze test coverage gaps:

Implementation: @file:src/services/[service].py
Tests: @file:tests/unit/test_[service].py

Report (prioritized):
  HIGH  — Error paths in the code with no test (try/except, if/elif branches)
  HIGH  — Functions with NO corresponding test
  MEDIUM — Edge cases missing (empty input, None, boundary values)
  LOW   — Implementation-coupled tests that will break on refactoring

For each HIGH gap: generate the missing test."
```

### Detect Tautological Tests

```
Tests that always pass regardless of correctness:

Type 1 — Testing implementation detail, not behavior:
  BAD:  assert user._normalize_email(email) == email.lower().strip()
  GOOD: assert create_user("User@EXAMPLE.COM").email == "user@example.com"

Type 2 — Mocking the thing you're testing:
  BAD:  mock_service.process_order.return_value = expected_result
        assert result == expected_result   (tests the mock, not the code)
  
Type 3 — Never-failing assertions:
  BAD:  assert result is not None   (passes even if result is wrong)
  GOOD: assert result.status == "confirmed" and result.order_id == expected_id

Prompt to detect:
"Review these tests. Are any tautological — tests that pass regardless of whether
the implementation is correct? Flag each with an explanation of why it's misleading."
```

---

## 4. Mocking Strategy

### What to Mock

```
MOCK (things your function doesn't own):
  - External HTTP clients (requests, httpx, aiohttp)
  - Database sessions (SQLAlchemy AsyncSession)
  - File system access (if testing logic not I/O)
  - datetime.now() calls (for deterministic time)
  - External services (email, SMS, payment gateways)
  - Environment variables

DO NOT MOCK (your own business logic):
  - Domain models and value objects
  - Business rules in the function being tested
  - Pure functions with no external deps
  Mocking your own logic = the test tests nothing.
```

### Standard Mocking Prompt

```
"Generate tests for @file:src/services/order_service.py

External dependencies to mock:
  - db: AsyncSession — use AsyncMock; set scalar_one_or_none.return_value for queries
  - email_service: EmailService — MagicMock; verify it's called with correct args
  - payment_client: PaymentClient — MagicMock; simulate success and failure

Do NOT mock: OrderService itself, any domain logic inside the service.
Follow the fixture patterns in @file:tests/conftest.py"
```

### Async Mocking — Most Common Failure

```
Symptom: TypeError: object MagicMock can't be used in 'await' expression

Fix: Use AsyncMock, not MagicMock, for any async function being mocked.

Correct pattern:
  from unittest.mock import AsyncMock, MagicMock, patch

  @pytest.fixture
  def mock_db():
      db = AsyncMock()
      db.execute.return_value.scalar_one_or_none.return_value = mock_user
      return db

Prompt:
"The function uses async def get_user(user_id). Mock it with AsyncMock.
Show the full fixture that makes db.execute().scalar_one_or_none() work
in an async test context."
```

---

## 5. Verification Loop

### Claude Iterates Until Green

```
"Run the verification loop:

1. Run: pytest tests/unit/test_[module].py -x --tb=short
2. If ALL pass: done. Report: X tests passed.
3. If any fail:
   a. Read the failure output
   b. Fix the IMPLEMENTATION (never the test file)
   c. Run pytest again
   d. If same test fails 3 times: stop, explain root cause, ask for input
4. After all pass: run full suite: pytest -v
5. Report final: X passed, Y failed, Z skipped

Constraint: test files are read-only."
```

---

## 6. Beginner Tier — First Tests with Claude

> No framework knowledge required. Start here.

### Scenario B1 — Ask Claude to Explain a Test (5 min)

```
"Explain this pytest test to me like I'm new to testing:

[paste any existing test]

1. What is it testing (behavior, not implementation)?
2. What is Arrange / Act / Assert in this test?
3. What would have to change in the code to make this test fail?
4. Is this a unit test or integration test? How do you know?"
```

### Scenario B2 — Generate Your First Test (10 min)

```
"Generate a pytest test for this simple Python function:

def add_tax(price: float, rate: float) -> float:
    return price + (price * rate)

Tests needed:
  - Normal calculation: price=100, rate=0.1 should return 110.0
  - Zero rate: price=50, rate=0.0 should return 50.0
  - Zero price: should return 0.0 regardless of rate

Show the complete test file with imports."
```

---

## 7. Revision Checklist

- [ ] Knows the correct order: tests FIRST, implementation second — and why it matters
- [ ] Can write a test-first prompt that generates failing tests
- [ ] Can run a gap analysis and find untested error paths
- [ ] Knows the mock vs don't-mock rule (own logic vs external deps)
- [ ] Knows the AsyncMock vs MagicMock distinction
- [ ] Can run a Claude verification loop (test → fix impl → test again)
- [ ] Can identify tautological tests that give false confidence
