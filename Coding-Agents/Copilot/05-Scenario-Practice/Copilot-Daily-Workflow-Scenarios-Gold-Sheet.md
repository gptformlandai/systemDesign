# Copilot Daily Workflow Scenarios — Gold Sheet

> **Track**: Copilot Mastery Track — Group 5: Scenario Practice
> **File**: 1 of 4 (Track File #28)
> **Audience**: Developers practicing real-world Copilot workflows under time pressure

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
