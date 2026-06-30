# Debugging With Copilot — Scenarios — Gold Sheet

> **Track**: Copilot Mastery Track — Group 5: Scenario Practice
> **File**: 3 of 4 (Track File #30)
> **Audience**: Developers practicing structured debugging workflows with Copilot

---

## Rules for All Scenarios

```
Before every debugging session:
1. Anonymize any real data before sharing — use synthetic values
2. Never share real credentials or environment values
3. Paste the EXACT error (not a description of it)
4. Include the stack trace if available
5. Include only the relevant code section — not the whole file
```

---

## ⭐ Beginner Tier — First Debugging Sessions (Scenarios B1–B3)

> No framework knowledge required. Practice the habit of using Copilot to understand errors before guessing at fixes.

---

### Scenario B1 — NameError: Explain Before You Fix (5 minutes)

**Setup**: Create `budget.py` and run it.

```python
# budget.py
def calculate_monthly_budget(income, expenses):
    savings = incme - expenses   # intentional typo
    return savings

result = calculate_monthly_budget(5000, 3200)
print(f"Monthly savings: {result}")
```

**Error you'll see**:
```
NameError: name 'incme' is not defined
```

**Exercise — Explain then fix**:
```
"I got this Python error when running budget.py:

NameError: name 'incme' is not defined

File: budget.py, line 3: savings = incme - expenses

1. Why does Python throw NameError specifically?
2. What does 'not defined' mean — is the variable missing, misspelled, or out of scope?
3. What is the fix?
4. How can I catch this kind of mistake before running the code?
   (hint: mention a linter or type checker)"
```

**Expected learning**: Understand what NameError means vs TypeError vs AttributeError. Know how a linter (ruff, pylint) would have caught this before runtime.

---

### Scenario B2 — TypeError: Read the Error, Don't Guess (5 minutes)

**Setup**: Create `converter.py` and run it.

```python
# converter.py
def celsius_to_fahrenheit(celsius):
    return (celsius * 9/5) + 32

temperatures = ["20", "25", "30"]  # strings, not numbers
for temp in temperatures:
    print(celsius_to_fahrenheit(temp))
```

**Error you'll see**:
```
TypeError: unsupported operand type(s) for *: 'str' and 'float'
```

**Exercise**:
```
"Explain this Python TypeError:

TypeError: unsupported operand type(s) for *: 'str' and 'float'
Line: return (celsius * 9/5) + 32

Code context:
[paste the function]

1. What is Python telling me in plain English?
2. Where is the type mismatch — what is 'str' and what is 'float' referring to?
3. Two ways to fix this:
   a. Fix the data before calling the function
   b. Fix the function to handle string input
4. Which fix is better and why?"
```

**Expected learning**: Read error messages character by character. "unsupported operand type(s) for *: 'str' and 'float'" tells you everything — don't guess, decode it.

---

### Scenario B3 — Test Failure: What Does FAILED Actually Mean? (10 minutes)

**Setup**: Create `math_utils.py` and `test_math_utils.py`.

```python
# math_utils.py
def safe_divide(a, b):
    return a / b  # no protection against b=0
```

```python
# test_math_utils.py
def test_safe_divide_raises_on_zero():
    try:
        result = safe_divide(10, 0)
        assert False, "Should have raised an error"
    except ZeroDivisionError:
        pass  # this is what we want

def test_safe_divide_normal():
    assert safe_divide(10, 2) == 5
```

Run: `python -m pytest test_math_utils.py -v`

**The test passes** (ZeroDivisionError is caught by the test) — but the implementation is wrong.

**Exercise**:
```
"Explain this situation:

Implementation: safe_divide(a, b) just does a / b
Test: catches ZeroDivisionError, which 'passes'
Problem: the function is called 'safe_divide' but it's not actually safe

1. Why does the test pass even though the implementation is unsafe?
2. What should safe_divide actually do when b=0?
3. Rewrite safe_divide to return a clear error instead of ZeroDivisionError
4. Update the test to match the new behavior
5. What is the difference between ZeroDivisionError and a ValueError with a message?"
```

**Expected learning**: Passing tests ≠ correct behavior. Tests only verify what they test. Learning to write tests that actually express requirements, not just avoid crashes.

---

## Intermediate Tier — Scenarios 1–5

> Pre-requisite: comfortable with your language's testing framework and async patterns.

---

## Scenario 1 — Interpret an Error You've Never Seen

**Setup**: You encounter this error for the first time in a new library.

```
Error:
  pydantic.v1.error_wrappers.ValidationError: 1 validation error for UserCreate
  email
    value is not a valid email address (type=value_error.email)
```

**Exercise**:
```
"Explain this Pydantic validation error:
[paste error above]

Tell me:
1. What caused this exactly
2. What input triggered it
3. How to add a test that catches this validation
4. How to return a useful HTTP error message to the API caller (not the raw Pydantic error)"
```

**Expected insight**: Pydantic's email validator, HTTP 422 response pattern, how to wrap Pydantic errors in FastAPI/Express/Spring.

---

## Scenario 2 — Async Deadlock or Hanging Request

**Setup**: An API endpoint works for the first 3 requests, then hangs indefinitely.

```
Symptoms:
- Requests 1-3 respond in ~50ms
- Request 4 hangs forever (never returns)
- No error in logs
- Restarting the service fixes it temporarily

Relevant code structure:
async def get_user(user_id: int, db: AsyncSession = Depends(get_db)):
    # some db query

[paste actual route code if you have it]
```

**Exercise**:
```
"Diagnose this pattern: an async API endpoint works for first N requests then hangs forever.
Context:
- Language/framework: [your stack]
- Database: [your DB]
- Concurrency model: [async/threaded]
- Symptoms: [describe]

Likely causes and how to diagnose each one:
1. [ranked by likelihood]
For each cause: what to check in logs/code and what the fix is."
```

**Expected insight**: Connection pool exhaustion, session not closed, semaphore not released, event loop blocked.

---

## Scenario 3 — Test Passes Locally, Fails in CI

**Setup**: Test passes on your machine (`pytest -v`), fails in GitHub Actions.

```
CI failure:
  FAILED tests/test_config.py::test_database_url_from_env
  E   AssertionError: assert None == 'postgresql://...'
  
  Test code:
  def test_database_url_from_env():
      url = get_database_url()
      assert url == os.environ.get("DATABASE_URL")
```

**Exercise**:
```
"This test passes locally but fails in CI:

Test: [paste test]
Failure: AssertionError — environment variable is None in CI

Why does this happen even though it works locally?
What are all the places environment variables could be:
1. Set locally but not in CI
2. Set in CI but in a different scope
3. Loaded at import time (before test setup)

Fix: how to make this test environment-independent."
```

**Expected insight**: Environment variable scope in CI, fixture ordering, conftest.py initialization order, dotenv loading.

---

## Scenario 4 — N+1 Query Diagnosis

**Setup**: An endpoint that returns a list of orders is taking 2-5 seconds for 100 orders.

**Exercise**:
```
"Diagnose a performance problem:

Code: #selection (select the endpoint and service code)
Symptom: returns 100 orders in 2-5 seconds. Expected: under 100ms.

Analyze:
1. Is there an N+1 query pattern? Where?
2. What queries is this code likely running?
3. How to fix it (eager loading, batching, or raw query)?
4. How to add a test that catches N+1 regression?
5. What tool can I use to see the actual queries being executed?"
```

**Expected insight**: ORM lazy loading, `selectinload`/`include`/`JOIN FETCH`, query logging, test with query counter.

---

## Scenario 5 — Production Incident Debugging (Anonymized)

**Important**: Use only anonymized data in this exercise.

**Setup**: Service is returning 500 errors. You have a sanitized log snippet.

```
Sanitized log:
  ERROR 2024-01-15 14:23:45 [request_id=abc123] payment_service.py:87
  Unhandled exception: KeyError: 'stripe_charge_id'
  Traceback:
    File "payment_service.py", line 87, in process_refund
      charge_id = order['stripe_charge_id']
    File "order_repository.py", line 45, in get_order_for_refund
      return self.session.get(Order, order_id)
```

**Exercise**:
```
"Production 500 error — analyze this sanitized log:
[paste log above]

Diagnose:
1. Root cause (not just 'KeyError' — WHY is the key missing?)
2. Which code path leads here
3. What scenario triggers this (new order type? Migration gap?)
4. Immediate mitigation (what to deploy NOW to stop the bleeding)
5. Proper fix (what to change for long-term correctness)
6. What test should have caught this

Note: this is a sanitized log with no real data."
```

**Expected insight**: Data migration gap, handling optional fields in ORM models, defensive attribute access, regression test.
