# Daily Workflow Scenarios — Gold Sheet

> **Track**: Claude Mastery Track — Group 5: Scenario Practice
> **File**: 1 of 4 (Track File #26)
> **Audience**: Developers practicing Claude workflows under real time pressure

---

## ⭐ Beginner Tier — First Workflows (B1–B3)

> No framework needed. Complete these before the Intermediate scenarios.

---

### Scenario B1 — Explain Code Before Touching It (5 min)

**Setup**: Create `discount.py`:

```python
def calculate_bundle_price(items, customer_tier, promo_code=None):
    subtotal = sum(item['price'] * item['qty'] for item in items)
    if customer_tier == "GOLD":
        subtotal *= 0.80
    elif customer_tier == "SILVER":
        subtotal *= 0.90
    if promo_code == "SAVE10":
        subtotal -= 10
    elif promo_code == "SAVE20PCT":
        subtotal *= 0.80
    return max(0, round(subtotal, 2))
```

**Task**: Use Claude Chat to understand it before modifying it.

```
"Explain this function:
[paste the code]

1. What does it do in plain English (1 sentence)?
2. List every input and what happens for each valid value
3. What are the execution paths? (draw them as: condition → result)
4. What edge cases does it NOT handle?
5. If I call calculate_bundle_price([{'price':50,'qty':2}], 'GOLD', 'SAVE10'),
   what is the return value and how did you calculate it?"
```

**What you're building**: The habit of understanding before modifying. Every senior engineer asks "what does this do?" before they touch it.

---

### Scenario B2 — Document Then Improve (10 min)

**Task**: Use Claude to document the function from B1, then find its gaps.

**Step 1 — Docstring**:
```
"Add a Google-style Python docstring to this function:
[paste calculate_bundle_price]

Include: purpose, Args with types, Returns, one Example."
```

**Step 2 — Find edge cases**:
```
"What inputs to this function produce surprising or incorrect results?
Consider: empty items list, negative prices, both promo code and GOLD tier together,
very large subtotals, None as customer_tier.

For each: what happens now, what should happen."
```

**Step 3 — Fix one gap**:
```
"Add validation to raise ValueError for negative item prices.
Keep all other behavior identical."
```

---

### Scenario B3 — Plan a Change Without Implementing (10 min)

**Setup**: You need to add a new customer tier "PLATINUM" with 30% discount.

**Task**: Use Claude to plan before touching any code.

```
"I need to add a 'PLATINUM' tier (30% discount) to calculate_bundle_price.
[paste the function]

Before I change anything:
1. Exactly which lines will change?
2. Are there other places in the code that reference 'GOLD'/'SILVER' that also need updating?
3. What tests should I write BEFORE making the change?
4. What could go wrong with this change?

Plan only — no code changes."
```

**What you're building**: Plan-before-code discipline. Changes with a plan produce fewer regressions than changes without one.

---

## Intermediate Tier — Scenarios 1–5

> Pre-requisite: 01-Foundations and 02-Intermediate-Power-User tracks complete.

---

## Scenario 1 — Morning Planning Sprint (10 min)

**Setup**: Ticket: "Add rate limiting to the login endpoint — max 5 attempts per IP per 15 minutes. Return HTTP 429."

```
/plan

"Ticket: Add login rate limiting — max 5 per IP per 15 min, HTTP 429 response.
Using @codebase:
1. Which files will be affected?
2. What does the current login flow look like?
3. Simplest implementation approach (FastAPI + Redis / Express + Redis / etc.)?
4. Implementation steps in order
5. Tests needed

Plan only — no code."
```

**Success**: Clear plan with specific files and test cases in under 10 minutes.

---

## Scenario 2 — Quick Debugging Session (5 min)

**Setup**: CI just failed with:
```
FAILED tests/unit/test_user_service.py::test_create_user_duplicate_email_raises_conflict
AttributeError: 'AsyncMock' object has no attribute 'scalar_one_or_none'
```

```
/debug

"Fix the test failure in the terminal output.
Relevant test: @file:tests/unit/test_user_service.py
Relevant service: @file:src/services/user_service.py

Root cause and fix — show only the corrected test setup."
```

**Success**: Test fixed, understanding of why `scalar_one_or_none` isn't on `AsyncMock` by default.

---

## Scenario 3 — Pre-Commit Review (15 min)

**Setup**: You finished the rate limiting feature. About to commit.

**Step 1 — Security**:
```
/security

"Security review for @file:src/middleware/rate_limiter.py
Check: Redis key injection, IP spoofing (X-Forwarded-For), error disclosure, bypass via IPv6"
```

**Step 2 — Test gaps**:
```
"Test gap analysis: @file:src/middleware/rate_limiter.py vs @file:tests/unit/test_rate_limiter.py
What error paths and edge cases are not tested?"
```

**Step 3 — Commit message**:
```
"Generate a descriptive git commit message for:
Changed: src/middleware/rate_limiter.py, tests/unit/test_rate_limiter.py
What: Redis rate limiting on login endpoint — 5 attempts / IP / 15 min
Format: conventional commits style (feat: ...)"
```

---

## Scenario 4 — End-of-Day Learning Capture (10 min)

**Setup**: You implemented rate limiting. Hit an issue with Redis async connection in tests.

```
"Generate session notes for today.

Topic: Redis connection pooling in async pytest context

Capture:
  - The prompt that helped me most today: [paste it]
  - One Claude failure today and what I'll do differently: [describe]
  - One new slash command I should create based on today's repeated prompts

Format: markdown with headers, code examples, 3 revision questions."
```

---

## Scenario 5 — Codebase Onboarding (20 min)

**Setup**: Just added to a new repository you've never seen.

```
Step 1 — Architecture overview (ask Claude):
"Using @codebase:
1. What does this system do? (2 sentences)
2. Architecture pattern?
3. Request flow: API → service → DB (one sentence per step)
4. Key domain entities
5. Testing strategy
Under 200 words."

Step 2 — Find the danger zone:
"Which file in @codebase is most dangerous to change without understanding first? Why?"

Step 3 — First task:
"Suggest 3 small improvements (< 1 hour each) that would have real impact on code quality."
```

---

## Revision Checklist

- [ ] Can run morning planning with /plan in under 10 minutes
- [ ] Can diagnose a CI failure with /debug in under 5 minutes
- [ ] Always runs pre-commit review before committing
- [ ] Writes end-of-day session notes regularly
- [ ] Can onboard to an unfamiliar codebase in 20 minutes with Claude
