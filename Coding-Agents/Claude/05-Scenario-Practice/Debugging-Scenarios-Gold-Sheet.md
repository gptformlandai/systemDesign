# Debugging Scenarios — Gold Sheet

> **Track**: Claude Mastery Track — Group 5: Scenario Practice
> **File**: 3 of 4 (Track File #28)
> **Audience**: Developers practicing structured debugging workflows with Claude

---

## Rules for All Debugging Scenarios

```
1. Never share real credentials, production tokens, or customer PII
2. Anonymize error messages before pasting (replace real emails, IDs, etc.)
3. Always paste the EXACT error — not a description of it
4. Include the stack trace when available
5. Include only the relevant code section — not entire files
```

---

## ⭐ Beginner Tier — First Debugging Sessions (B1–B3)

---

### Scenario B1 — Read the Error Before Guessing (5 min)

**Setup**: Create `orders.py` with a bug:

```python
def process_order(order_id, items, discount=None):
    total = sum(item['price'] * item['quantity'] for item in items)
    if discount:
        total = total - discunt  # intentional typo
    return {"order_id": order_id, "total": total, "status": "confirmed"}
```

Run it. Get `NameError: name 'discunt' is not defined`.

**Exercise**:
```
"Explain this Python error:

NameError: name 'discunt' is not defined
File: orders.py, line 4

Code: [paste process_order]

1. What type of error is NameError (vs TypeError vs AttributeError)?
2. Why does Python not catch this at import time?
3. What is the fix?
4. What tool could have caught this before running? (mention a linter)"
```

**What you're building**: The habit of decoding error messages instead of guessing.

---

### Scenario B2 — TypeError: Identify the Mismatch (5 min)

**Setup**: Run this:

```python
def apply_tax(prices, tax_rate):
    return [price * (1 + tax_rate) for price in prices]

result = apply_tax(["10.00", "25.50", "5.99"], 0.08)
```

Get: `TypeError: can't multiply sequence by non-int of type 'float'`

**Exercise**:
```
"Explain this TypeError:

TypeError: can't multiply sequence by non-int of type 'float'
File: line 2: return [price * (1 + tax_rate) for price in prices]

Input: prices = ['10.00', '25.50', '5.99']

1. What is the type mismatch? (what is 'sequence', what is 'float' here?)
2. Why does multiplying a string by a float fail?
3. Two ways to fix: (a) fix the input, (b) fix the function
4. Which fix is better for production code? Why?"
```

---

### Scenario B3 — Test Passes but Behavior Is Wrong (10 min)

**Setup**: Create `validator.py` and `test_validator.py`:

```python
# validator.py
def is_valid_age(age):
    return age > 0  # bug: doesn't check upper bound

# test_validator.py
def test_valid_age():
    assert is_valid_age(25) == True  # passes
    assert is_valid_age(-1) == False  # passes

def test_invalid_age_too_old():
    assert is_valid_age(150) == True  # wrong — should be False but returns True
```

Run pytest. All pass. But is_valid_age(150) should be invalid.

**Exercise**:
```
"These tests pass but the behavior is wrong:

is_valid_age(150) returns True but it should be False (max valid age is 120).

1. Why does the test suite pass despite the bug?
2. What test is missing?
3. What is the fix to the implementation?
4. What does this teach about test completeness?"
```

---

## Intermediate Tier — Scenarios 1–5

---

## Scenario 1 — Diagnose an Unfamiliar Error (5 min)

**Error**:
```
pydantic.v1.error_wrappers.ValidationError: 1 validation error for UserCreate
email
  value is not a valid email address (type=value_error.email)
```

```
"Explain this Pydantic validation error:
[paste error]

1. What caused it exactly (which field, what check)?
2. What input triggered it?
3. How to add a test that catches this validation?
4. How to return a useful HTTP 422 error to the API caller instead of the raw Pydantic error?"
```

---

## Scenario 2 — Async Hanging Request (10 min)

**Symptom**: Endpoint works for 3 requests, then hangs indefinitely.

```
"Diagnose: an async FastAPI endpoint works for first N requests then hangs.

Symptoms:
  - Requests 1-3 respond in ~50ms
  - Request 4 hangs forever (no timeout, no error in logs)
  - Restarting the service fixes it temporarily
  - Stack: FastAPI + SQLAlchemy 2.x async

Code: @file:src/api/[route].py (or select the suspect code)

Diagnose:
1. Ranked root causes by likelihood
2. What to check in code/logs for each cause
3. The fix for the most likely cause"
```

---

## Scenario 3 — Test Passes Locally, Fails in CI (10 min)

**CI failure**:
```
FAILED tests/test_config.py::test_database_url_from_env
AssertionError: assert None == 'postgresql://...'
```

```
"This test passes locally but fails in CI:

Test: def test_database_url_from_env():
      url = get_database_url()
      assert url == os.environ.get('DATABASE_URL')

Failure: url is None in CI

Diagnose:
1. Why does it work locally but not in CI?
2. All the ways environment variables can be set locally but not in CI
3. How to make this test environment-independent
4. What's the correct way to test env-var-dependent configuration?"
```

---

## Scenario 4 — N+1 Query (10 min)

**Symptom**: Endpoint returning 100 orders takes 3-5 seconds. Expected: < 100ms.

```
"Diagnose performance:

Code: @selection (select the route handler + service)
Symptom: 100 orders in 3-5 seconds. Expected: < 100ms.

1. Is there an N+1 query pattern? Where?
2. What queries is this code likely running?
3. Fix: eager loading, batching, or raw query?
4. How to add a test that catches N+1 regression?
5. What tool shows actual queries being executed?"
```

---

## Scenario 5 — Production Incident (Anonymized) (15 min)

**Sanitized log**:
```
ERROR [request_id=abc123] payment_service.py:87
Unhandled exception: KeyError: 'stripe_charge_id'
Traceback:
  File "payment_service.py", line 87, in process_refund
    charge_id = order['stripe_charge_id']
  File "order_repository.py", line 45, in get_order_for_refund
    return self.session.get(Order, order_id)
```

```
"Production incident analysis (sanitized — no real data).

Error: KeyError: 'stripe_charge_id'
Log: [paste sanitized log above]

Diagnose:
1. Root cause (not just 'KeyError' — WHY is the key missing?)
2. What scenario triggers this (new order type? migration gap?)
3. Immediate mitigation to deploy NOW
4. Proper fix for long-term correctness
5. What test would have caught this?"
```

---

## Revision Checklist

- [ ] Reads error messages character by character before guessing
- [ ] Provides 3 inputs for every debug session: error + code + what changed
- [ ] Understands the key Python error types (NameError / TypeError / AttributeError / KeyError)
- [ ] Can diagnose async deadlocks and N+1 queries
- [ ] Always anonymizes data before pasting to Claude
- [ ] Can write the test that would have caught the bug, after fixing it
