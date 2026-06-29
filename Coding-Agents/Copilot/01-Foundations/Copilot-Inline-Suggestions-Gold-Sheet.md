# Copilot Inline Suggestions — Gold Sheet

> **Track**: Copilot Mastery Track — Group 1: Foundations
> **File**: 3 of 6 (Track File #3)
> **Audience**: Developers learning to master inline autocomplete
> **Read after**: GitHub-Copilot-Setup-Personal-Machine-Gold-Sheet.md

---

## 1. Practical Impact Meter

| Topic | Impact | Why Developers Miss This |
|---|---|---|
| Comment-driven suggestions — the key technique | ★★★★★ | Devs wait for Copilot to guess; comments direct it precisely |
| Tab / Escape / Alt+] cycle — the full workflow | ★★★★★ | Most devs only use Tab — missing alternative suggestions |
| How context above the cursor shapes suggestions | ★★★★★ | Importing libraries and writing type hints radically improve suggestions |
| Accept word-by-word, not entire suggestion | ★★★★☆ | Accepting in pieces lets you keep useful parts and reject bad parts |
| When inline is wrong — switching to Chat | ★★★★☆ | Chasing bad inline suggestions wastes time; Chat is better for complex logic |
| Disabling suggestions for specific file types | ★★★☆☆ | Suggestions in config files or plaintext can be distracting |
| Copilot completions for tests, config, YAML | ★★★★☆ | Inline is extremely fast for repetitive test cases and config patterns |

---

## 2. How Inline Suggestions Work

### Must Know

```
When you pause typing, Copilot sends the content around your cursor to the model
and receives a completion suggestion displayed as grey ghost text.

Context it uses for inline suggestions:
  1. File content ABOVE the cursor (highest weight)
  2. File content BELOW the cursor (next section)
  3. Open related files in other tabs
  4. Imports and type declarations at the top of the file
  5. Comments immediately before the cursor (extremely high weight)
  6. Function/class names and signatures above

Context it does NOT use for inline:
  - Files you haven't opened
  - copilot-instructions.md (instructions apply to Chat, not inline)
  - Your recent conversations
```

### The Ghost Text Lifecycle

```
1. You stop typing for ~300ms
2. Copilot sends context to model (asynchronously — you can keep typing)
3. Grey ghost text appears — the suggested completion
4. Your choices:
   Tab       → Accept the entire suggestion
   Escape    → Reject it
   Alt+]     → See the next alternative suggestion
   Alt+[     → See the previous alternative suggestion
   Cmd+→     → Accept ONE word at a time (partial accept)
   Keep typing → Copilot re-generates with the additional characters
```

---

## 3. Comment-Driven Suggestions — The Core Technique

This is the highest-ROI skill for inline suggestions.

### Strategy: Write the Intent as a Comment First

```python
# Validate that an email address is syntactically correct using regex
# Return True if valid, False if invalid
# Handle edge cases: empty string, None, no @ symbol, no domain

def validate_email(email: str) -> bool:
    # Copilot now generates: import re, regex pattern, None check, return logic
```

```python
# Sort a list of User objects by last_name ascending, then first_name ascending
# Return a new list; do not mutate the original

def sort_users(users: list[User]) -> list[User]:
    # Copilot generates the sorted() call with key=lambda correctly
```

```python
# Read a CSV file and return a list of dicts
# Skip the header row
# Handle FileNotFoundError and return empty list
# Use context manager for file handling

def read_csv_to_dicts(path: str) -> list[dict]:
    # Copilot generates complete implementation with error handling
```

### Strategy: Name Functions Descriptively

```python
# Vague name → Copilot guesses:
def process(data):
    ...  # could be anything

# Descriptive name → Copilot knows the intent:
def normalize_phone_number_to_e164_format(phone: str) -> str:
    ...  # Copilot generates E.164 normalization logic

def calculate_monthly_compound_interest(
    principal: float,
    annual_rate: float,
    months: int
) -> float:
    ...  # Copilot generates the compound interest formula correctly
```

### Strategy: Set Context with Imports and Type Hints

```python
# Without imports:
def send_email(to: str, subject: str, body: str):
    # Copilot doesn't know what library to use — suggests anything

# With imports:
import smtplib
from email.mime.text import MIMEText
from email.mime.multipart import MIMEMultipart

def send_email(to: str, subject: str, body: str) -> None:
    # Copilot correctly uses smtplib with MIME types
```

---

## 4. Pattern Completion — Copilot's Best Feature

Copilot excels at completing patterns. Write the first instance, then let Copilot complete the rest.

### Test Pattern Completion

```python
import pytest
from services.user_service import UserService

class TestUserService:

    def test_create_user_success(self, db_session):
        service = UserService(db_session)
        user = service.create_user("alice@example.com", "Alice")
        assert user.id is not None
        assert user.email == "alice@example.com"

    # Now press Enter and wait — Copilot will generate:
    def test_create_user_duplicate_email(self, db_session):
        # ...raises ValueError or DuplicateEmailError...

    def test_create_user_invalid_email(self, db_session):
        # ...raises ValidationError...

    def test_create_user_empty_name(self, db_session):
        # ...raises ValueError...
```

### Data / Config Pattern Completion

```python
ROLE_PERMISSIONS = {
    "admin": ["read", "write", "delete", "manage_users"],
    "editor": ["read", "write"],
    # Press Enter — Copilot suggests:
    # "viewer": ["read"],
    # "guest": [],
}
```

### Repetitive Method Pattern

```python
class UserRepository:

    def find_by_id(self, user_id: int) -> User | None:
        return self.session.get(User, user_id)

    # Press Enter and tab — Copilot generates:
    def find_by_email(self, email: str) -> User | None:
        return self.session.query(User).filter(User.email == email).first()

    def find_all_active(self) -> list[User]:
        return self.session.query(User).filter(User.active == True).all()
```

---

## 5. Partial Acceptance — Cmd+Right Arrow

```
When Copilot suggests a long line, accept one word at a time:

Suggestion appears:
  return sorted(users, key=lambda u: (u.last_name, u.first_name))

You want the structure but not the lambda:
  Press Cmd+→ three times to accept: return sorted(users, key=
  Then type your own: key=self.sort_key)

Why this matters:
  Partial acceptance is faster than accepting then deleting the unwanted parts.
  Use it when: the structure is right but details are wrong.
```

---

## 6. When Inline Suggestions Fall Short

```
Use inline for:
  ✓ Boilerplate (constructors, standard methods, repetitive patterns)
  ✓ Simple algorithm implementations (sort, filter, map)
  ✓ Test cases after you write the first one
  ✓ Config files (JSON, YAML schemas, environment variables)
  ✓ SQL queries for known schemas
  ✓ Standard library usage (file I/O, datetime, collections)

Switch to Chat when:
  ✗ The logic requires multi-step reasoning
  ✗ You need to understand the code before accepting it
  ✗ The suggestion is wrong and cycling alternatives didn't help
  ✗ The implementation spans multiple files
  ✗ You need to ask "why" or "is this the best approach"
  ✗ Security-sensitive code (auth, crypto, input validation) — always verify in Chat

Rule of thumb:
  If you press Alt+] more than 3 times looking for a good suggestion,
  switch to Chat with explicit context instead.
```

---

## 7. Controlling Suggestions — Enabling and Disabling

```json
// settings.json — enable/disable per language:
{
  "github.copilot.enable": {
    "*": true,           // enabled for all files by default
    "markdown": true,    // useful for drafting docs
    "yaml": true,        // useful for GitHub Actions, Kubernetes manifests
    "json": true,        // useful for schemas and config
    "plaintext": false,  // disable for plain text — usually irrelevant
    "env": false         // NEVER enable for .env files — security risk!
  }
}
```

### Inline Commands in the Editor

```
If Copilot is typing something you don't want and it keeps suggesting:
  Press Escape to reject the current suggestion
  Keep typing your own code — Copilot will regenerate after you pause

To temporarily disable Copilot:
  Click Copilot icon in status bar → "Disable Copilot"
  Or: Command Palette → "GitHub Copilot: Disable"

To re-enable:
  Command Palette → "GitHub Copilot: Enable"
```

---

## 8. Inline Chat — Quick Targeted Edits

```
Cmd+I in the editor opens inline Chat at cursor position.

Best patterns:

1. Selected code → quick fix:
   Select a function → Cmd+I → "Fix the bug where this returns None for empty input"

2. Generate at cursor:
   Place cursor at empty line → Cmd+I → "Add a method to get all active users sorted by creation date"

3. Add docstring:
   Place cursor inside a function → Cmd+I → "Add a Google-style docstring"

4. Convert pattern:
   Select a for-loop → Cmd+I → "Convert to list comprehension"

5. Add error handling:
   Select a function → Cmd+I → "Add proper error handling with custom exceptions"

Review before accepting:
  Inline Chat changes are shown as a diff.
  Press Enter to accept, Escape to discard.
  Do not accept without reading the diff — especially for anything security-relevant.
```

---

## 9. High-ROI Inline Workflows

### Workflow 1 — Generate Boilerplate with Comment + Tab

```
# Write comment describing the full class → tab through the implementation
# Fastest for: dataclasses, service skeletons, repository patterns, request handlers
```

### Workflow 2 — Write One Test → Tab for More

```
# Write the first test case → press Enter at the end of the class
# Copilot generates the next test case based on the first
# Fastest for: unit test suites, parametrized test cases
```

### Workflow 3 — SQL with Schema Comments

```python
# Users table: id (int PK), email (varchar unique), name (varchar), active (bool), created_at (timestamp)
# Orders table: id (int PK), user_id (int FK → users.id), amount (decimal), status (varchar), created_at (timestamp)

# Query: get total order amount per active user for the last 30 days

query = """
# Copilot generates the correct JOIN + GROUP BY + WHERE with date filter
"""
```

### Workflow 4 — YAML Config with Pattern Completion

```yaml
# GitHub Actions workflow — multi-job CI
name: CI

on:
  push:
    branches: [main, develop]
  pull_request:

jobs:
  lint:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - name: Run linting
        run: ruff check .
  # Press Enter — Copilot generates test and build jobs following the same pattern
```

---

## 10. Revision Checklist

- [ ] Can use Tab, Escape, Alt+], Alt+[ fluently
- [ ] Uses Cmd+→ for partial word-by-word acceptance
- [ ] Writes intent comments before function bodies
- [ ] Names functions descriptively to guide suggestions
- [ ] Sets imports at file top to steer library choices
- [ ] Knows when to switch from inline to Chat
- [ ] Has disabled Copilot for `.env` files in settings
- [ ] Has used the pattern-completion technique for tests at least once
- [ ] Can use Inline Chat (Cmd+I) for quick targeted edits
