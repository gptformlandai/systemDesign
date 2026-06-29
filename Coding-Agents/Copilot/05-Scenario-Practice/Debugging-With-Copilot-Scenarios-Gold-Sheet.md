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
