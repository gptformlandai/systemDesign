# Codex For Documentation — Gold Sheet

> **Track**: Codex Mastery Track — Group 2: Intermediate Power User
> **File**: 6 of 7 (Track File #12)
> **Audience**: Developers who want production-quality documentation with minimal manual effort
> **Read after**: Codex-For-Testing-Gold-Sheet.md

---

## Practical Impact Meter

| Topic | Impact | Why Developers Miss This |
|---|---|---|
| Docstrings from codebase (not invented) | ★★★★★ | Codex invents behavior that doesn't exist — always constrain to what is verifiably in the code |
| README generation from actual code | ★★★★★ | README written in isolation drifts from reality; generated from codebase stays accurate |
| API documentation with examples | ★★★★☆ | "Add examples" is the most impactful single instruction for API docs |
| ADR (Architecture Decision Record) generation | ★★★★☆ | Capturing WHY a decision was made is almost never documented; Codex can help |
| "Do not invent" constraint | ★★★★☆ | The most important documentation constraint — prevents confident hallucination |

---

## ⭐ Beginner Tier — Start Here

### B1: Document one function (2 minutes)

```bash
codex --approval-policy auto-edit \
  "Write a Google-style docstring for process_payment() in src/payments/service.py.
   Include: description, Args (with types and what invalid values mean), Returns, Raises.
   Do not change the function logic.
   Constraint: only document what you can verify from the code — do not invent behavior."
```

Review: is the docstring accurate? Does it describe what the code actually does?
If Codex invented behavior that isn't there — add "do not invent" constraints.

### B2: Explain before you document

```bash
# Step 1: Understand first
codex "Explain what process_payment() does, its failure modes, and its side effects. Make no changes."

# Step 2: Verify the explanation is correct (your judgment)

# Step 3: Then document
codex --approval-policy auto-edit \
  "Write a docstring for process_payment() based on this understanding: [paste verified explanation]"
```

---

## 1. Docstring Generation — Python / TypeScript / Java

### Python (Google style)

```bash
codex --approval-policy auto-edit \
  "Write Google-style docstrings for all public functions in src/orders/service.py.
   For each function include:
   - One-line description
   - Args: name, type, description, validation rules if any
   - Returns: type and description
   - Raises: each exception type and when it's raised
   - Example: one minimal usage example
   Constraint: do not document behavior you cannot verify from the code.
   Do not change function logic.
   Run: pytest tests/test_order_service.py -x (ensure no regressions)"
```

### TypeScript (JSDoc)

```bash
codex --approval-policy auto-edit \
  "Write JSDoc comments for all exported functions in src/api/users.ts.
   For each function:
   - @description: what it does
   - @param: each parameter with type annotation
   - @returns: return type and description
   - @throws: each error condition
   - @example: one usage example
   Constraint: only document what is verifiable in the code.
   Do not change any function logic."
```

### Java (Javadoc)

```bash
codex --approval-policy auto-edit \
  "Write Javadoc comments for all public methods in OrderService.java.
   Include: @param for each parameter, @return for return value, @throws for checked exceptions.
   Do not document private methods.
   Constraint: only document verifiable behavior.
   Do not change method logic."
```

---

## 2. README Generation from Codebase

```bash
# Step 1: Generate with strict accuracy constraint
codex "Generate a README.md for this project.
       Required sections:
       - Project title and one-paragraph description
       - Architecture overview: what the main modules are and how they interact
       - Tech stack: language version, framework, database, key libraries
       - Setup: step-by-step from git clone to running locally
       - Running tests: exact command
       - Environment variables: list every required env var and what it controls
       - API endpoints: list all endpoints with method, path, description
       - Contributing: branch naming, PR process, code style
       
       Critical constraint: only include information you can verify from the codebase.
       If something is unclear: write [TODO: verify] — do not guess.
       Output the README content. Do not create the file yet."

# Step 2: Review the output
# Check every section: is it accurate? Is anything invented?

# Step 3: Create the file only after review
codex --approval-policy auto-edit "Create README.md with this exact content: [paste reviewed content]"
```

---

## 3. Architecture Decision Records (ADRs)

ADRs document WHY a decision was made. They are almost never written. Codex can help.

```bash
codex "Generate an Architecture Decision Record (ADR) for the decision to use JWT-based
       authentication in this project.
       
       ADR format:
       # ADR-001: JWT-Based Authentication
       
       ## Status
       Accepted
       
       ## Context
       [What problem were we solving? What alternatives were considered?]
       
       ## Decision
       [What did we decide?]
       
       ## Consequences
       [What are the trade-offs? What becomes easier? What becomes harder?]
       
       ## Alternatives Rejected
       [What else was considered and why was it rejected?]
       
       Derive the content from the codebase — look at the implementation, tests, and
       any existing comments for evidence of the decision.
       Mark anything uncertain as [inferred from code — verify with team]."
```

---

## 4. API Documentation with Examples

```bash
codex "Generate API documentation for all endpoints in src/api/.
       For each endpoint:
       - Method and path
       - Description: what it does
       - Authentication: required (what header/token format)
       - Request body: schema with types and constraints
       - Response 200: schema with example
       - Response errors: each status code with description
       - cURL example: complete working example
       
       Format: Markdown with code blocks for examples.
       Critical: base all request/response schemas on the actual Pydantic models,
       not invented fields.
       Do not document fields that don't exist in the model."
```

---

## 5. Onboarding Documentation from Codebase

```bash
codex "Generate developer onboarding documentation for someone new to this codebase.
       
       Structure:
       1. System overview: what this system does, who uses it, scale
       2. Architecture: layers, their responsibilities, naming conventions
       3. Request lifecycle: trace a typical API request end-to-end (with code references)
       4. Data models: key entities and their relationships
       5. How to make a change: step-by-step guide for adding a new endpoint
       6. How to run tests: including how to set up test database, env vars
       7. Gotchas: 5 things that would trip up a new developer
       
       Source everything from the codebase. Use file:line references where possible.
       Mark any section where you had to infer rather than observe."
```

---

## 6. Release Notes from Git Log

```bash
# Step 1: Get the git log for the release
git log v1.2.0..HEAD --oneline > /tmp/commits.txt

# Step 2: Generate release notes
codex "Generate release notes for the upcoming v1.3.0 release.
       Commit log: $(cat /tmp/commits.txt)
       
       Categorize changes into:
       ## New Features
       ## Bug Fixes
       ## Performance Improvements
       ## Breaking Changes
       ## Security Fixes
       
       Format for each item: user-facing description (not implementation detail).
       'Improve performance of user queries' not 'Add index on users.created_at'.
       Skip commits marked as: chore, docs, ci, refactor (internal only)."
```

---

## 7. The "Do Not Invent" Constraint

The most important documentation constraint.

```bash
# WITHOUT this constraint:
codex "Document the create_user() function"
# Codex: "This function creates a user and sends a welcome email"
# Reality: this function never sends emails. Codex invented it.

# WITH this constraint:
codex "Document the create_user() function.
       CRITICAL: only describe behavior you can verify from the code.
       If there is something you're uncertain about, write: [unverified — check with team]
       Do NOT invent behavior, side effects, or constraints that aren't clearly in the code."
```

Signs Codex is inventing:
- Documentation mentions features not in the code
- Error cases that never exist in the implementation
- Side effects (emails, events, logs) that aren't there
- Performance characteristics stated as fact ("fast", "efficient")

---

## Production Pitfall

```
PITFALL: Documenting generated code without verifying accuracy
  Generated documentation may describe behavior that doesn't exist.
  Rule: read every docstring you merge and verify it against the actual function.
  Time: 30 seconds per function. Worth it every time.
  
PITFALL: Using Codex to document unfamiliar code without reading the code first
  If you don't understand the code, you can't catch wrong documentation.
  Rule: explain before documenting. Run the explanation prompt first,
        verify the explanation is correct, then document.
```

---

## Interview Traps

```
TRAP: "Generated documentation looks right, so it probably is right"
TRUTH: Documentation hallucinations look completely plausible — wrong parameter types,
       examples using nonexistent methods, behavior descriptions that are aspirational.
       Always compare generated docs against the actual implementation before merging.

TRAP: "The 'do not invent' constraint is unnecessary — Codex reads the code"
TRUTH: Without this constraint, Codex fills uncertainty gaps with confident-sounding
       fabrications. Every documentation prompt must include it. It's the most important
       single constraint in documentation work.

TRAP: "A README generated from the codebase is always accurate"
TRUTH: Codex will include features it infers should exist but haven't been implemented.
       Review every instruction in a generated README: does the code actually do this?
       Flag inaccuracies before users run the documented commands.
```

---

## Revision Checklist

- [ ] Can generate Google/JSDoc/Javadoc for a complete file
- [ ] Always use "do not invent" constraint on documentation prompts
- [ ] Can generate a README with accuracy constraints
- [ ] Can create an ADR from an existing codebase decision
- [ ] Can generate API docs with request/response examples
- [ ] Always verify generated documentation against actual code before merging
