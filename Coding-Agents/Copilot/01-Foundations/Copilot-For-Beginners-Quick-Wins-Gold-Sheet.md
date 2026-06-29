# Copilot For Beginners — Quick Wins — Gold Sheet

> **Track**: Copilot Mastery Track — Group 1: Foundations
> **File**: 6 of 6 (Track File #6)
> **Audience**: Developers completing the Foundations path — hands-on exercises
> **Read after**: Safe-Prompting-Principles-Gold-Sheet.md

---

## 1. Overview

This sheet contains 10 hands-on quick wins to build Copilot muscle memory.
Each exercise takes 5–15 minutes. Do all 10 before moving to the Intermediate path.

These exercises use Python as the default language. Adapt to your language as needed.

---

## 2. Quick Win #1 — Explain Code You Don't Understand

**Scenario**: You've joined a new codebase and found an unfamiliar pattern.

**Setup**: Create a file `explain_exercise.py` with this code:

```python
from contextlib import contextmanager
from typing import Generator
import threading

_local = threading.local()

@contextmanager
def request_context(user_id: int, trace_id: str) -> Generator[None, None, None]:
    _local.user_id = user_id
    _local.trace_id = trace_id
    try:
        yield
    finally:
        del _local.user_id
        del _local.trace_id

def get_current_user_id() -> int | None:
    return getattr(_local, "user_id", None)
```

**Exercise**:
```
1. Select the entire code block
2. Open Chat: Cmd+Shift+I
3. Type: "Explain what #selection does. Why is threading.local used?
          What problem does this pattern solve? What happens if
          get_current_user_id is called outside the context manager?"
4. Read the explanation. Verify it matches your understanding.
```

**Success criteria**: You can explain thread-local storage in your own words after this exercise.

---

## 3. Quick Win #2 — Fix an Error with Stack Trace

**Setup**: Create `buggy_service.py`:

```python
def calculate_discount(price: float, discount_pct: float) -> float:
    if discount_pct > 100:
        raise ValueError("Discount cannot exceed 100%")
    return price - (price * discount_pct / 100)

def process_order(items: list[dict]) -> float:
    total = 0
    for item in items:
        discounted = calculate_discount(item["price"], item["discont"])  # typo: discont
        total += discounted
    return total

result = process_order([{"price": 100.0, "discount": 20.0}])
print(f"Total: {result}")
```

**Exercise**:
```
1. Run the file: python buggy_service.py
2. In VS Code terminal, the error appears
3. In Chat, type: "Fix the error in #terminalLastCommand.
                   Show me the corrected line only."
4. Apply the fix.
5. Run again to confirm it works.
```

**Success criteria**: You used `#terminalLastCommand` and fixed the bug without manually pasting the error.

---

## 4. Quick Win #3 — Generate Unit Tests

**Setup**: Create `user_validator.py`:

```python
import re

def validate_email(email: str) -> bool:
    if not email or not isinstance(email, str):
        return False
    pattern = r'^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,}$'
    return bool(re.match(pattern, email))

def validate_password(password: str) -> tuple[bool, list[str]]:
    errors = []
    if len(password) < 8:
        errors.append("Password must be at least 8 characters")
    if not any(c.isupper() for c in password):
        errors.append("Password must contain at least one uppercase letter")
    if not any(c.isdigit() for c in password):
        errors.append("Password must contain at least one digit")
    return (len(errors) == 0, errors)
```

**Exercise**:
```
1. Select the entire file content
2. In Chat: "Generate pytest unit tests for #selection.
             Cover: valid inputs, invalid inputs, edge cases (None, empty string),
             boundary conditions. Use descriptive test names.
             Do not use external fixtures."
3. Create a new file: test_user_validator.py
4. Paste the generated tests.
5. Run: pytest test_user_validator.py -v
```

**Success criteria**: At least 8 tests generated; all pass.

---

## 5. Quick Win #4 — Write a Docstring

**Exercise**:
```
1. Open any Python function you've written recently
2. Place cursor inside the function (before the body)
3. Type /doc in inline Chat (Cmd+I → /doc)
4. Copilot generates a Google-style or NumPy-style docstring
5. Accept if accurate; edit the parameters/returns section if needed
```

Alternatively, via Chat:
```
"Write a Google-style docstring for #selection.
Include: Args, Returns, Raises, and a short Example section."
```

**Success criteria**: The docstring accurately describes the function behavior including all parameters and return values.

---

## 6. Quick Win #5 — Create a README for a Small Project

**Exercise**:
```
1. Open your practice repository root
2. In Chat:
   "Generate a README.md for a Python project with this structure:
    - src/services/ contains UserService and OrderService
    - src/models/ contains User and Order dataclasses
    - tests/ contains pytest unit tests
    - The project uses FastAPI, SQLAlchemy async, PostgreSQL, and pytest
    - It requires Python 3.12+
    Include: Project overview, Prerequisites, Installation,
    Running tests, Project structure, Contributing, License (MIT)."
3. Create README.md from the output
4. Edit to fix any inaccuracies about your actual project
```

**Success criteria**: A complete README with all standard sections, accurate to your project structure.

---

## 7. Quick Win #6 — Refactor with Inline Chat

**Setup**: Create `legacy.py`:

```python
def get_active_users_by_department(users, department):
    result = []
    for u in users:
        if u['active'] == True:
            if u['department'] == department:
                result.append(u)
    return result
```

**Exercise**:
```
1. Select the function body (the for loop)
2. Cmd+I → "Refactor to use list comprehension. Keep the function signature identical."
3. Review the generated diff
4. Accept only if correct
```

**Success criteria**: The nested for/if becomes a single list comprehension. Same behavior, fewer lines.

---

## 8. Quick Win #7 — Generate a GitHub Actions Workflow

**Exercise**:
```
1. Create .github/workflows/ directory in your practice repo
2. Open Chat:
   "Generate a GitHub Actions CI workflow that:
    - Triggers on push to main and pull_request
    - Uses Python 3.12
    - Installs dependencies from requirements.txt
    - Runs: ruff check . (linting)
    - Runs: pytest tests/ (unit tests)
    - Shows test results as annotations
    - Caches pip dependencies for faster runs"
3. Save as .github/workflows/ci.yml
4. Review the generated YAML carefully before committing
```

**Success criteria**: A valid, well-structured GitHub Actions YAML with caching and test reporting.

---

## 9. Quick Win #8 — Explain an Error You've Never Seen

**Exercise**:
```
1. Paste this error into Chat:
   "Explain this Python error and what causes it:
    
    Traceback (most recent call last):
      File 'app.py', line 15, in get_user
        user = await session.get(User, user_id)
      File 'sqlalchemy/ext/asyncio/session.py', line 180, in get
        return await greenlet_spawn(...)
    sqlalchemy.exc.MissingGreenlet: greenlet_spawn has not been called;
    can't call await_only() here. Was IO attempted in an unexpected place?
    
    We use SQLAlchemy 2.0 with asyncpg. The session is created with
    async_sessionmaker(engine)."
    
2. Ask: "What is the most common cause of this in FastAPI?
         How do I fix it?"
```

**Success criteria**: You understand what MissingGreenlet means and can name at least 2 causes.

---

## 10. Quick Win #9 — Generate Data with Synthetic Examples

**Exercise**:
```
1. In Chat:
   "Generate a Python function that creates a list of 10 synthetic User objects
    for testing. Each user should have:
    - id (int, 1-10)
    - name (realistic but fictional full name)
    - email (derived from name, @example.com domain)
    - department (one of: Engineering, Marketing, Sales, Finance)
    - salary (int, 60000-150000)
    - active (bool, mostly True)
    Return as a list of dicts. No real personal data."
    
2. Add this to a file: test_data.py
3. Run it to verify the output is correctly structured
```

**Success criteria**: 10 synthetic records generated, all realistic but clearly fake.

---

## 11. Quick Win #10 — Write a Commit Message

**Exercise**:
```
1. Make a small change in your practice repo
   (e.g., add a new function, fix a bug from earlier exercises)
2. Stage the change: git add .
3. In VS Code Source Control panel, click the ✨ sparkle icon next to the commit message field
4. Copilot generates a commit message from your diff
5. Read it — is it accurate?
6. Edit if needed, then commit

Alternatively, in Chat:
   "Write a conventional commit message for this change:
    [describe what you changed in 1-2 sentences]
    Format: type(scope): description"
```

**Success criteria**: A descriptive commit message using conventional format (feat/fix/refactor/docs/test/chore).

---

## 12. Foundations Completion Checklist

After completing all 10 quick wins, verify:

```
Foundations completed:
[ ] Read: Copilot Mental Model Gold Sheet
[ ] Read: GitHub Copilot Setup Gold Sheet
[ ] Read: Copilot Inline Suggestions Gold Sheet
[ ] Read: Copilot Chat Fundamentals Gold Sheet
[ ] Read: Safe Prompting Principles Gold Sheet
[ ] Read: This sheet (Quick Wins)

Exercises completed:
[ ] QW1: Explained unfamiliar code with Chat + #selection
[ ] QW2: Fixed a bug using #terminalLastCommand
[ ] QW3: Generated unit tests for a real function
[ ] QW4: Added a docstring with /doc
[ ] QW5: Created a README for a project
[ ] QW6: Refactored a loop to list comprehension with inline Chat
[ ] QW7: Generated a GitHub Actions CI workflow
[ ] QW8: Explained an error message you'd never seen before
[ ] QW9: Generated synthetic test data (no real PII used)
[ ] QW10: Used Copilot to generate a commit message

Personal baselines established:
[ ] I know all 6 Copilot surfaces and when to use each
[ ] I have never pasted a secret into Copilot
[ ] I have a practice repository set up
[ ] I review every diff before accepting
[ ] I am ready to move to the Intermediate Power User path
```

**Next step**: [02-Intermediate-Power-User/Custom-Instructions-Deep-Dive-Gold-Sheet.md](../02-Intermediate-Power-User/Custom-Instructions-Deep-Dive-Gold-Sheet.md)
