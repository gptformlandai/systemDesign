# Copilot For Documentation — Gold Sheet

> **Track**: Copilot Mastery Track — Group 2: Intermediate Power User
> **File**: 8 of 8 (Track File #13a — inserted after PR Review)
> **Audience**: Developers using Copilot to accelerate and improve documentation
> **Read after**: Copilot-For-PR-Review-Gold-Sheet.md

---

## 1. Practical Impact Meter

| Topic | Impact | Why Developers Skip It |
|---|---|---|
| README generation — structured prompt | ★★★★★ | Devs write poor READMEs because starting from blank is painful |
| Docstring generation — language-agnostic | ★★★★★ | Undocumented functions are the #1 cause of slow onboarding |
| API documentation from code | ★★★★★ | API docs drift from implementation — generate from source to keep in sync |
| ADR (Architecture Decision Record) generation | ★★★★☆ | Decisions not recorded get re-litigated. Copilot makes writing ADRs fast |
| Code comment review — remove noise, keep signal | ★★★★☆ | Most codebases have too many trivial comments and too few important ones |
| Release notes from commits or diff | ★★★★☆ | Release notes manually written are always incomplete or late |
| Onboarding documentation from codebase | ★★★★☆ | New developer onboarding docs are almost always stale |
| Changelog generation | ★★★☆☆ | Changelogs are important but tedious — Copilot makes them instant |

---

## 2. README Generation

### The Complete README Prompt

```
"Generate a README.md for this project.

Project info:
#file:.copilot-context.md (or paste brief description)

Structure (use these exact headers in this order):
# [Project Name]
[1-sentence project description]

## What This Does
[Paragraph: problem solved, who uses it, key capabilities]

## Prerequisites
[Bullet list: required tools, versions, accounts]

## Installation
[Numbered steps — runnable commands only, no vague "set up your environment"]

## Configuration
[How to set environment variables, what each one does]

## Running Locally
[Commands to start the service, where to access it]

## Running Tests
[Exact command to run tests, what each test suite covers]

## Project Structure
[Directory tree with one-line description per directory]

## API Reference
[If applicable: key endpoints with method, path, auth requirement]

## Contributing
[How to contribute: branch naming, PR process, code standards]

## License
[License type and year]

Rules:
- Every code block must be runnable (no placeholder commands)
- No section should say 'TODO' — either fill it or omit it
- Target audience: a developer who has never seen this project before
- Maximum length: 500 words (links to detailed docs for the rest)"
```

---

## 3. Docstring Generation — Language Patterns

### Python — Google Style

```
Select a function → Chat:
"Add a Google-style docstring to #selection.
Include: Args (with types), Returns (with type), Raises (if any), Example (one line).
Do not describe HOW it works — describe WHAT it does and WHAT it returns."

Example output:
def create_order(user_id: int, items: list[dict], shipping_address: str) -> Order:
    """Create a new order for a user with the specified items.

    Args:
        user_id: ID of the user placing the order.
        items: List of dicts with 'product_id' (int) and 'quantity' (int).
        shipping_address: Full shipping address as a single string.

    Returns:
        The created Order object with id, status='pending', and created_at set.

    Raises:
        UserNotFoundError: If user_id does not exist.
        InsufficientInventoryError: If any item quantity exceeds available stock.
        ValueError: If items list is empty.

    Example:
        order = await create_order(user_id=42, items=[{"product_id": 1, "quantity": 2}],
                                   shipping_address="123 Main St, NY 10001")
    """
```

### TypeScript / JavaScript — JSDoc

```
Select a function → Chat:
"Add JSDoc documentation to #selection.
Include: @param with types, @returns with type, @throws if applicable,
@example with one usage. Use TypeScript types, not just 'any'."

Example output:
/**
 * Fetches a user by their ID from the API.
 *
 * @param userId - The unique identifier of the user to fetch.
 * @param options - Optional request configuration.
 * @param options.includeDeleted - Whether to include soft-deleted users. Default: false.
 * @returns A promise resolving to the user object, or null if not found.
 * @throws {NetworkError} If the API is unreachable.
 * @throws {AuthError} If the request lacks valid authorization.
 *
 * @example
 * const user = await fetchUser(42, { includeDeleted: false });
 * console.log(user?.name); // "Alice"
 */
```

### Java — Javadoc

```
Select a method → Chat:
"Add Javadoc to #selection.
Include: @param for all parameters, @return, @throws for checked exceptions.
One sentence per tag. Describe behavior, not implementation."
```

---

## 4. API Documentation Generation

### From Code to OpenAPI-Compatible Docs

```
"Generate API documentation for #file:src/api/orders.py (or your router file).

For each endpoint, produce:
| Method | Path | Auth Required | Request Body | Response (200) | Error Responses |
|--------|------|---------------|--------------|----------------|-----------------|

Then for each endpoint also produce a detailed block:
**POST /orders**
Description: [what it does]
Auth: Bearer token (required)
Request body:
  {
    "user_id": int,
    "items": [{"product_id": int, "quantity": int}]
  }
Response 201:
  {
    "id": int,
    "status": "pending",
    "created_at": "ISO 8601 string"
  }
Errors:
  400: Validation error (empty items, invalid quantity)
  401: Missing or invalid token
  404: User not found
  409: Insufficient inventory"
```

### Keeping API Docs in Sync

```
Rule: Generate API docs FROM code, not alongside code.
If you maintain separate API docs (Confluence, Notion, wiki):
  Run this prompt after every API change in a PR:
  "Compare the actual API in #file:[router file] against the docs in #file:[doc file].
  List any endpoints that exist in code but not in docs,
  any endpoints in docs but not in code, and any parameter mismatches."
  
  Then update the docs based on this gap analysis.
```

---

## 5. ADR Generation

### Architecture Decision Record Template

```
"Generate an ADR for this decision:

Decision: [State the decision in one sentence]
Context: [Why was this decision needed? What problem was being solved?]

Format exactly:
# ADR-[NNN]: [Short title]
Date: [today's date]
Status: Accepted

## Context
[2-3 sentences: the problem and what made a decision necessary]

## Options Considered
### Option 1: [Name]
- Pro: ...
- Pro: ...
- Con: ...
- Con: ...

### Option 2: [Name]
[same format]

### Option 3: [Name]
[same format]

## Decision
[What was chosen and the primary reason — one paragraph]

## Consequences
**Positive:**
- [What improves]

**Negative:**
- [What gets harder or more constrained]

**Neutral:**
- [What changes without being clearly better or worse]

## Compliance
[How will code reviewers enforce this decision?]"
```

---

## 6. Code Comment Review

### Remove Noise, Keep Signal

```
"Review the comments in #selection.

Categorize each comment as:
  KEEP: Explains non-obvious intent, documents a gotcha, or provides essential context
  REMOVE: Describes what the code obviously does (e.g., '# increment counter')
  IMPROVE: The comment exists for good reason but is inaccurate or unclear

Output:
  - List of REMOVE items (line numbers)
  - List of IMPROVE items with suggested rewrites
  - List of KEEP items (no action needed)
  - Identify any complex logic that has NO comment and NEEDS one"
```

### Anti-Patterns to Catch

```
Comments that add noise (remove these):
  # increment the counter
  counter += 1
  
  # return the result
  return result
  
  # create a new user
  user = User(name=name)

Comments that add signal (keep these):
  # Rate limit: max 5 per minute to prevent abuse (see TICKET-1234)
  
  # expire_on_commit=False required: async SQLAlchemy cannot lazy-load
  # after session close — attributes would raise MissingGreenlet
  
  # Sorting is stable in Python 3.7+ — primary key order preserved within groups
```

---

## 7. Release Notes Generation

```
"Generate release notes for version [X.Y.Z].

Changed files (or paste git log):
#file:CHANGELOG.md (recent commits section)
OR: git log --oneline v[previous]..HEAD output:
[paste output]

Format:
## [X.Y.Z] — [date]

### New Features
[Bullet list of user-visible new capabilities]

### Improvements
[Bullet list of enhancements to existing functionality]

### Bug Fixes
[Bullet list of bugs fixed — describe the symptom, not the code change]

### Breaking Changes
[If any: what changed and how to migrate]

### Internal / Developer Changes
[Optional: dependency updates, refactoring, tests]

Rules:
- Write for end users and API consumers, not for developers
- 'Fixed NullPointerException' → 'Fixed crash when creating order with no items'
- No commit hashes in the output
- Group by user impact, not by file changed"
```

---

## 8. Onboarding Documentation

```
"Generate an onboarding guide for a new developer joining this project.

Use #codebase to understand the project structure.

Sections:
## System Overview
[What the system does and who uses it]

## Architecture Quick Map
[Key directories and what they contain — 1 sentence each]

## How to Run Locally
[Step-by-step, copy-paste ready]

## First Task Guide
[Walk through finding a simple task, implementing it, and submitting a PR]

## Key Concepts to Learn First
[3-5 domain or technical concepts that must be understood before contributing]

## Common Pitfalls for New Developers
[Top 3-5 mistakes new devs make in this codebase]

## Where to Get Help
[Who to ask, what channels, what documentation exists]

Rules:
- Every command must be copy-paste runnable
- Assume: developer knows how to code but has NEVER seen this project
- Maximum: 600 words (link to detailed docs for anything longer)"
```

---

## 9. Revision Checklist

- [ ] Can generate a complete README with the structured prompt
- [ ] Can generate Google-style Python, JSDoc (TypeScript), and Javadoc docstrings
- [ ] Can generate API documentation in tabular + detailed format
- [ ] Can generate an ADR with all required sections
- [ ] Can run a code comment review (remove noise, keep signal, improve unclear)
- [ ] Can generate release notes from git log or CHANGELOG
- [ ] Can generate a developer onboarding guide from #codebase
- [ ] Knows the rule: generate docs FROM code, not alongside code (for API docs)
