# Debugging Scenarios — Gold Sheet

> **Track**: Codex Mastery Track — Group 5: Scenario Practice
> **File**: 3 of 4 (Track File #28)
> **Audience**: Developers who want to debug faster with Codex
> **Read after**: Feature-Building-Scenarios-Gold-Sheet.md

---

## ⭐ Beginner Tier

### B1: Decode a NameError (3 minutes)

```bash
# You got: NameError: name 'validate_amount' is not defined
codex "Python error in src/payments/service.py:
       NameError: name 'validate_amount' is not defined
       
       Explain:
       1. Why does this error happen? (root cause)
       2. Where in the code should I look?
       3. What are the 2 most likely causes?
       
       Do not fix yet — just explain."
```

Then fix it yourself (or ask Codex to fix with the explanation as context).

### B2: TypeError mismatch

```bash
# You got: TypeError: unsupported operand type(s) for +: 'int' and 'str'
codex "Error in src/orders/service.py line 47:
       TypeError: unsupported operand type(s) for +: 'int' and 'str'
       Code at line 47: total = base_price + tax_rate
       
       Explain: why does this happen and what are the likely causes?
       What's the correct fix (without losing type safety)?
       Do not modify files — explain only."
```

---

## Scenario 1 — Debugging an Unfamiliar Error (10 minutes)

**Situation**: you hit an error you've never seen before.

```bash
# Capture everything: error + stack + relevant code
codex "Error I can't diagnose:
       
       Error message: [exact error]
       Stack trace:
       [paste stack trace]
       
       Code at failing line:
       [paste relevant function — anonymize any sensitive values]
       
       What I know:
       - This started happening after: [describe recent change if any]
       - It happens: [always / sometimes / under specific conditions]
       
       Diagnose:
       1. Root cause (WHY this happens)
       2. Minimum fix
       3. What test would have caught this?
       
       Do not modify any files yet."
```

---

## Scenario 2 — Async Hanging Request (15 minutes)

**Situation**: an API request hangs and never returns.

```bash
codex "My API endpoint is hanging (no response, eventually times out).
       
       Endpoint: GET /orders/{id} in src/api/orders.py
       It calls: get_order_with_items() in src/services/order_service.py
       
       I can reproduce with: curl -v http://localhost:8000/orders/1
       
       Common causes of async hanging I should check:
       
       Diagnose step by step:
       1. Is there an await missing? (calling async function without await)
       2. Is there a database query not using async? (sync query in async context)
       3. Is there an external HTTP call without timeout?
       4. Is there a lock that's never released?
       
       Check each possibility in the code and tell me which is most likely.
       Do not make changes."
```

---

## Scenario 3 — CI Passes, Local Fails (or Vice Versa) (15 minutes)

**Situation**: test passes in CI but fails locally (or opposite).

```bash
codex "Test inconsistency:
       Test: tests/test_users.py::test_get_user_by_email
       
       Status: FAILS locally, PASSES in CI (or opposite — specify which)
       
       Local environment: macOS 14, Python 3.11
       CI environment: Ubuntu 22.04, Python 3.11
       
       Failure output (locally):
       [paste local pytest output]
       
       Common environment differences that cause this:
       1. Timezone (local = America/NY, CI = UTC)
       2. Database state (local DB has data, CI has clean DB)
       3. File system case sensitivity
       4. Environment variables not set locally
       5. Test ordering dependency (other test sets up state that this test depends on)
       
       Which of these is most likely given the failure output?
       Propose a fix and a local reproduction method."
```

---

## Scenario 4 — N+1 Query Performance Bug (15 minutes)

**Situation**: endpoint is slow; you suspect N+1.

```bash
codex "Performance issue: GET /users/{id}/orders is slow for users with many orders.
       
       Implementation in src/users/service.py::get_user_orders():
       [paste the function]
       
       Diagnose:
       1. Is this an N+1 query? Show the evidence.
       2. How many queries does this generate for a user with 50 orders?
       3. What is the fix? (JOIN vs eager loading vs batch fetch)
       4. Reference pattern to follow: [if you have one in the codebase]
       
       Verification after fix: should show 1 query instead of N+1.
       Provide the fixed version."
```

---

## Scenario 5 — Production Incident Debug (Anonymized) (20 minutes)

**Situation**: production alert, users can't check out.

```bash
codex "Production incident — anonymized data only.
       
       Alert: 500 errors on POST /checkout endpoint, started 30 min ago
       Error rate: ~40% of requests failing
       
       Recent changes (last 2 hours):
       - Deployed new rate limiting middleware
       - No other changes
       
       Log sample (anonymized):
       [paste 5-10 log lines with real values replaced by: USER_ID, ORDER_ID, AMOUNT, etc.]
       
       Diagnose:
       1. Is the new middleware the likely cause? How?
       2. What would the root cause look like in the code?
       3. What's the fastest mitigation? (revert? hotfix? config change?)
       4. What's the permanent fix?
       
       Do not implement. Diagnosis only."
```

---

## Self-Assessment

| Scenario | Time | Root cause found? | Fix correct? | What I'd change |
|----------|------|------------------|-------------|----------------|
| B1: NameError decode | | | | |
| B2: TypeError | | | | |
| 1: Unfamiliar error | | | | |
| 2: Async hang | | | | |
| 3: CI vs local | | | | |
| 4: N+1 query | | | | |
| 5: Production incident | | | | |

---

## Revision Checklist

- [ ] Can identify root cause, not just symptom, from an error message
- [ ] Can diagnose an async hanging request step by step
- [ ] Can identify N+1 queries and propose the fix
- [ ] Can analyze a CI vs local test inconsistency
- [ ] Never share real PII in debugging prompts — always anonymize
