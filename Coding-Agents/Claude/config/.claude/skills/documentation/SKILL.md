# Documentation Skill

## When to Invoke
Apply this skill automatically when the task involves:
- Writing README files
- Writing or improving docstrings (Google, NumPy, JSDoc, Javadoc)
- Generating API documentation
- Writing Architecture Decision Records (ADRs)
- Creating onboarding guides for new team members
- Writing release notes or changelogs
- Reviewing existing documentation for accuracy
- Keywords: "document", "README", "docstring", "ADR", "onboarding", "API docs"

## Core Principle: Document What It DOES, Not What It Should Do

The single most important rule: read the implementation, then document actual behavior.
Never document intent. Never document what the code was supposed to do. Only document what it does.

"This validates email format" → WRONG if the implementation only checks for @ symbol
"This checks if the string contains exactly one @ symbol" → CORRECT (what it actually does)

## Workflow

### Step 1 — Read Before Writing
1. Read the implementation file(s) completely
2. Identify the target audience:
   - Developer onboarding → explain concepts, show examples, link to related code
   - API consumer → focus on endpoints, request/response, auth, errors
   - Internal team → explain design decisions, architecture, why not just what
3. Verify any existing documentation against the implementation (find discrepancies)

### Step 2 — Draft with Accuracy Checks
For every statement written, ask: "Is this EXACTLY what the code does?"
For every command written, verify: "Can I run this right now and it will work?"

### Step 3 — Verify
- If documentation contains code examples: run them and confirm they work
- If documentation contains CLI commands: run them and confirm output matches
- If documentation makes claims about behavior: trace through the code to verify

## Document Type Templates

### README
```
# [Project Name]
[One sentence: what problem this solves]

## What This Does
[2-3 sentences: the problem, the solution, who it's for]

## Prerequisites
- [Tool] [version]
- [Tool] [version]

## Installation
```bash
[exact command — tested]
[exact command — tested]
```

## Running Locally
```bash
[exact command] # runs on: http://localhost:[port]
```

## Running Tests
```bash
[exact command]
```

## Configuration
| Variable | Required | Default | Description |
|----------|----------|---------|-------------|
| DATABASE_URL | Yes | — | PostgreSQL connection string |

## Project Structure
```
src/
  api/        — FastAPI routers (request parsing only)
  services/   — Business logic
  repositories/ — Database access
```
```

### Docstring (Google style)
```python
def function_name(param: Type) -> ReturnType:
    """Does [exactly one thing described in present tense].

    [One optional sentence of additional context if non-obvious.]

    Args:
        param: [Description. Include expected format/constraints if relevant.]

    Returns:
        [Describe what is returned and when. Include None case if applicable.]

    Raises:
        ValidationError: When [specific condition].
        UserNotFoundError: When [specific condition].

    Example:
        result = function_name(valid_input)
        # result == expected_output
    """
```

### ADR
```markdown
# ADR-[NNN]: [Short Title]
Date: [YYYY-MM-DD]
Status: Accepted

## Context
[Problem being solved. What constraints led to this decision point.]

## Options Considered
### Option 1: [Name]
- Pro: [specific advantage]
- Con: [specific disadvantage]

### Option 2: [Name]
[same format]

## Decision
[What was chosen and the primary reason.]

## Consequences
- Positive: [specific improvements]
- Negative: [specific costs or limitations]
- Neutral: [changes that are neither better nor worse]

## Compliance
[How to enforce this in code reviews.]
```

## Quality Standards
- Every CLI command is copy-paste runnable (tested before documenting)
- No aspirational language: "This will do X" → only "This does X"
- No vague descriptions: "handles errors gracefully" → "raises UserNotFoundError when user_id does not exist"
- Length is appropriate: README under 400 words, docstring under 150 words
- Target audience language: adjust technical depth for the reader type

## What I NEVER Do
- Document code I haven't read
- Write "TODO: add documentation here"
- Use passive voice when active voice is clearer: "is used to validate" → "validates"
- Repeat what the code obviously says: `user.save()` doesn't need a comment saying "saves user"
- Document private implementation details that callers shouldn't know about
