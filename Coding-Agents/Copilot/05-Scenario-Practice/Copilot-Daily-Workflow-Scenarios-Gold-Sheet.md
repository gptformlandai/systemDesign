# Copilot Daily Workflow Scenarios — Gold Sheet

> **Track**: Copilot Mastery Track — Group 5: Scenario Practice
> **File**: 1 of 4 (Track File #28)
> **Audience**: Developers practicing real-world Copilot workflows under time pressure

---

## ⭐ Beginner Tier — First Week with Copilot (Scenarios B1–B3)

> Complete these before the Intermediate scenarios. No frameworks required — just a file and VS Code.

---

### Scenario B1 — Explain a File You've Never Seen (5 minutes)

**Setup**: Open any unfamiliar file in your project (or paste the code below into a new file).

**Simulated file** (create `calculator.py`):
```python
def calculate_discount(price, customer_type, quantity):
    base = price * quantity
    if customer_type == "VIP":
        return base * 0.75
    elif customer_type == "MEMBER":
        return base * 0.90 if quantity >= 10 else base * 0.95
    return base
```

**Task**: Use Copilot Chat to understand it before touching it.

**Prompt**:
```
"Explain what this function does:
#selection

1. What is its purpose in plain English?
2. What are the inputs and what are valid values?
3. What does each branch of the if/elif do?
4. What edge cases does it NOT handle?
5. If I call calculate_discount(100, 'VIP', 5), what is the return value and why?"
```

**Success criteria**: You can explain the function to someone else without re-reading the code.

**What you're practicing**: Using Chat to read code before modifying it — the single most important Copilot habit.

---

### Scenario B2 — Your First Inline Suggestion Drill (10 minutes)

**Setup**: Create a new file `string_utils.py`.

**Task**: Use inline completions (not Chat) to build a small utility module.

**Step 1** — Type this comment, then press Tab to accept the suggestion:
```python
# Returns True if the string is a valid email address
def is_valid_email(
```

**Step 2** — On a new line, type:
```python
# Truncates a string to max_length characters and adds "..." if truncated
def truncate(
```

**Step 3** — Type:
```python
# Counts the number of words in a sentence (split by spaces)
def word_count(
```

**Step 4** — Review each generated function:
- Does it do what the comment says?
- Does it handle an empty string?
- If the function is wrong: reject it, rewrite the comment more specifically, try again.

**Score**:
- [ ] 3 functions generated via inline completions
- [ ] You reviewed each one — not just accepted all
- [ ] You can explain what each function does
- [ ] At least one iteration where you rejected the first suggestion and refined it

---

### Scenario B3 — Document Existing Code (10 minutes)

**Setup**: Take any undocumented function you wrote (or use one from Scenario B2).

**Task**: Use Copilot Chat to add docstrings and improve the function.

**Prompt 1 — Generate docstring**:
```
"Generate a Python docstring for this function using Google style format:
#selection

Include: what it does, Args with types and descriptions,
Returns with type and description, Raises if it can raise errors,
and one Example."
```

**Prompt 2 — Find edge cases**:
```
"What inputs could break this function or produce unexpected results?
#selection

List: invalid input types, boundary values, empty inputs, None values.
For each: what happens currently and what should happen."
```

**Prompt 3 — Improve error handling**:
```
"Add input validation to this function so it raises ValueError with a clear message
for each invalid input you identified:
#selection

Do not change the return behavior for valid inputs."
```

**Success criteria**: The function now has a docstring, handles invalid input with clear errors, and you understand every line.

---

## Intermediate Tier — Scenarios 1–5

> Pre-requisite: complete 01-Foundations and 02-Intermediate-Power-User tracks.

---

## Scenario 1 — Morning Planning Sprint (10 minutes)

**Setup**: You have a ticket: "Add rate limiting to the login endpoint — max 5 attempts per IP per 15 minutes. Return HTTP 429 after that."

**Task**: Use Copilot to plan the implementation in under 10 minutes without writing any code.

**Prompt**:
```
"I need to implement rate limiting on the login endpoint.
Ticket: max 5 login attempts per IP per 15 minutes, return HTTP 429 afterward.

Using #codebase:
1. Which existing files will be affected?
2. What does our current login flow look like?
3. What's the simplest implementation approach given our stack (FastAPI + Redis)?
4. What are the implementation steps in order?
5. What tests do we need?

Plan only — no code."
```

**Success criteria**: You have a clear implementation plan with specific files and test cases identified in under 10 minutes.

---

## Scenario 2 — Quick Debugging Session (5 minutes)

**Setup**: CI just failed with this output in the terminal.

**Simulated error**:
```
FAILED tests/unit/test_user_service.py::test_create_user_duplicate_email_raises_conflict
E   AttributeError: 'AsyncMock' object has no attribute 'scalar_one_or_none'
```

**Task**: Diagnose and fix using only Copilot.

**Prompt**:
```
"Fix the test failure shown in #terminalLastCommand.
Relevant test: #file:tests/unit/test_user_service.py
Relevant service: #file:src/services/user_service.py
Root cause and fix — show the corrected test only."
```

**Success criteria**: Test fixed, understanding of why `scalar_one_or_none` isn't on `AsyncMock` by default.

---

## Scenario 3 — Pre-PR Checklist (15 minutes)

**Setup**: You finished implementing the rate limiting feature. About to open a PR.

**Task**: Run the full pre-PR workflow using Copilot.

**Step 1 — Security review**:
```
"Security review for #file:src/middleware/rate_limiter.py:
Check: input handling, Redis key injection, IP spoofing risk, error disclosure"
```

**Step 2 — Test gap analysis**:
```
"Test gap analysis: #file:src/middleware/rate_limiter.py vs #file:tests/unit/test_rate_limiter.py
What error paths and edge cases are not tested?"
```

**Step 3 — PR description**:
```
"/write-pr-description
Changed: src/middleware/rate_limiter.py, src/api/auth.py, tests/unit/test_rate_limiter.py
What: added Redis-based rate limiting to login endpoint — 5 attempts per IP per 15 min"
```

**Success criteria**: Security review done, gaps identified and addressed, PR description generated.

---

## Scenario 4 — End-of-Day Learning Capture (10 minutes)

**Setup**: You implemented rate limiting today. It worked but you hit an issue with Redis connection pooling in async context.

**Prompt**:
```
"Generate structured learning notes on:
Topic: Redis connection pooling with aioredis in FastAPI async context

Also:
- The prompt that helped me most today was: [paste the prompt]
- One Copilot limitation I hit: [describe what didn't work well]
- What I'd do differently next time: [your reflection]

Format: markdown with headers, code example, 5 revision questions."
```

**Success criteria**: Session notes saved to notes/[date]-session.md, including at least one prompt improvement.

---

## Scenario 5 — Codebase Onboarding (20 minutes)

**Setup**: You've just been added to a new repository you've never seen before.

**Prompt sequence**:

**Step 1 — Architecture overview**:
```
"Using #codebase, give me:
1. What this system does (2 sentences)
2. Architecture pattern (layered/hexagonal/etc.)
3. Request flow from API to database (one sentence each step)
4. Key domain entities
5. Testing strategy
Under 250 words."
```

**Step 2 — Find the hottest file**:
```
"Which file in #codebase is most likely to cause problems if changed without care?
Why? What does it affect?"
```

**Step 3 — Identify quick wins**:
```
"Looking at #codebase, what are 3 small improvements (under 30 min each)
that would have meaningful impact on code quality?"
```

**Success criteria**: In 20 minutes you can describe the system, its risks, and propose improvements.
