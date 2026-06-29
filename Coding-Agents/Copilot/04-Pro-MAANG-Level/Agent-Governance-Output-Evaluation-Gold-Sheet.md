# Agent Governance & Output Evaluation — Gold Sheet

> **Track**: Copilot Mastery Track — Group 4: Pro / Production Level
> **File**: 5 of 7 (Track File #25)
> **Audience**: Senior developers responsible for AI output quality and team AI practices
> **Read after**: Advanced-Context-Engineering-Gold-Sheet.md

---

## 1. Practical Impact Meter

| Topic | Impact | Why Developers Miss This |
|---|---|---|
| Output evaluation standard — before every commit | ★★★★★ | Without a standard, "it looks right" is the review process |
| Hallucination detection patterns | ★★★★★ | The most dangerous Copilot output is the one that compiles and ships |
| Agent scope drift — detecting when an agent exceeds its brief | ★★★★★ | Agents that do too much produce changes you didn't ask for |
| Scoring generated code before accepting | ★★★★☆ | A quick mental rubric prevents 80% of AI-generated bugs |
| Team-level AI governance for small teams | ★★★★☆ | Even a 2-person team benefits from shared AI usage norms |
| When to escalate — human review gates | ★★★★☆ | AI cannot review AI for security-critical or production-critical code |
| Prompt quality audit — improving over time | ★★★★☆ | Devs use the same mediocre prompts for months without improving them |

---

## 2. The Output Evaluation Framework

### CREST — 5-Dimension Evaluation

Apply this before committing any Copilot-generated code:

```
C — Correctness:
  Does it do exactly what the requirement says?
  Does it handle edge cases (None, empty, boundary values)?
  Can I trace the logic and verify it myself?
  Would I write it the same way? If not, why not?

R — Risk:
  Does it touch auth, crypto, SQL, or file I/O? (auto-heightened review)
  Any new dependencies? (verify they exist and are safe)
  Any shell commands or subprocess calls? (read carefully)
  Any hardcoded values that should be configurable?

E — Error Handling:
  Are all error paths handled?
  Are errors caught at the right level?
  Are error messages safe (no internal details exposed)?
  Was any existing error handling removed?

S — Style Alignment:
  Does it match the codebase conventions?
  Does it match the instructions in copilot-instructions.md?
  Are naming conventions followed?
  Is it over-engineered for the use case?

T — Tests:
  Do tests exist for the new code?
  Do existing tests still pass?
  Are tests testing behavior or implementation details?
  Is there a test for the main error case?
```

### Quick Scoring (1 minute per diff)

```
Rate each dimension: 1 (fail) / 2 (partial) / 3 (pass)

Total 15/15 = ship it
Total 12-14 = fix the failing dimensions, then ship
Total < 12 = reject and regenerate with better prompt

This is a mental model, not a form. Do it in your head.
```

---

## 3. Hallucination Detection

### The Four Hallucination Types

```
Type 1 — Invented method names:
  Example: session.execute_async(), httpx.get_json(), user.save()
  
  Detection:
    - Method appears as AttributeError when you run it
    - IDE shows "member not found" (Pylance, IntelliJ)
    - You can't find it in the official docs
  
  Fix: "This method doesn't exist. The correct method in [lib] [version] is X."
  Prevention: Name specific library versions in instructions.

Type 2 — Hallucinated library features:
  Example: "FastAPI's built-in rate_limit decorator"
  "SQLAlchemy's async_scoped_session auto-cleanup"
  
  Detection:
    - Can't find it in the library's documentation
    - ImportError when you run it
  
  Fix: Tell Copilot what the correct equivalent is.
  Prevention: Specify library versions + paste a snippet from real docs.

Type 3 — Invented project conventions:
  Example: Copilot references a BaseService class that doesn't exist in your codebase.
  
  Detection:
    - The class/function doesn't appear in #codebase search
    - It has no file path you recognize
  
  Fix: "We don't have a BaseService. Implement without it."
  Prevention: Project context document listing what exists.

Type 4 — Wrong logic that looks right:
  Example: Off-by-one in pagination, wrong sort direction, wrong null check
  
  Detection: This one requires you to read the logic, not just run it.
  Ask yourself: "If input is [edge case], what does this return?"
  
  Fix: Test with a specific edge case; fix the failing case.
  Prevention: Always include edge cases in your test prompt.
```

---

## 4. Agent Scope Drift Detection

### Signs an Agent Is Exceeding Its Brief

```
Symptoms:
  - Files modified that you did not list in the working set
  - Database schema changed without being asked
  - Existing tests modified to pass new (possibly wrong) implementation
  - New abstractions (base classes, factories, registries) created without being asked
  - Dependencies added to package.json / requirements.txt without being asked
  - Comments or documentation removed (Copilot sometimes "cleans" these)

When you detect scope drift:
  1. Stop the session immediately (don't accept remaining changes)
  2. Run: git diff to see everything that changed
  3. Reject the out-of-scope changes (git checkout <file>)
  4. Restart with explicit constraint: "Only modify [file list]. Nothing else."

Prevention:
  - Explicit constraint in every Agent Mode prompt:
    "Only modify files I list. Do not create any new files without confirmation.
    Do not delete or modify any test files unless I explicitly ask."
  - Smaller working sets (2-3 files maximum in Edits mode)
  - Plan-Confirm-Execute pattern for Agent Mode
```

---

## 5. Team-Level AI Governance (Small Teams)

### For 2-10 Person Teams

```
These are lightweight — not enterprise policy documents.

Shared team prompt library:
  - Use .github/prompts/ in the main repo
  - PRs required to add/change prompts
  - Each prompt should have: name, description, and a comment explaining why it exists
  - Review prompt quality at monthly retros

Shared instructions strategy:
  - copilot-instructions.md is a team-reviewed document
  - Changes go through PR review
  - One person owns keeping it accurate
  - It should describe what the team actually does, not what they aspire to do

AI-generated code in PRs:
  - Reviewers ask: "Did Copilot write this?" — not to judge, but to calibrate review depth
  - AI-heavy PRs get slightly more review attention on edge cases and security
  - No rule against AI-generated code — just more thorough review of critical paths

Error postmortems:
  - When an AI-generated bug makes it to production: write 2 sentences in a shared doc
  - "What was the prompt? What was the hallucination? How to prevent?"
  - Build a shared pattern library of "Copilot got this wrong for us" over time
```

### Human Review Gates — When AI Review Is Not Enough

```
Always require human review (not just Copilot review) for:
  - Authentication and authorization logic
  - Database schema migrations
  - Payment processing code
  - Cryptographic operations
  - Any code that handles PII
  - Infrastructure as code (Terraform, Kubernetes)
  - Security controls (rate limiting, input sanitization)
  - Any code that runs with elevated privileges

For these areas:
  Use Copilot for: drafting, pattern generation, documentation
  Use human review for: verifying correctness and security
  Use security-specialist review or SAST tools for: final security gate
```

---

## 6. Prompt Quality Audit

### Monthly Prompt Audit Process

```
1. List all prompts in .github/prompts/ (ls .github/prompts/)
2. For each prompt, ask:
   a. When did I last use this? (check git blame or memory)
   b. Does the output it generates meet the CREST standard consistently?
   c. Is there a better version I've been typing ad-hoc?
   d. Is this prompt too generic to be useful?
   
3. Action for each:
   - KEEP: used recently, output is good
   - IMPROVE: add missing constraint or output format
   - DELETE: not used in 60 days or never produces useful output
   - REPLACE: create a new version, mark old as deprecated
   
4. Commit: "chore: prompt library audit - [date]"
```

### Prompt Anti-Patterns to Catch in the Audit

```
Anti-pattern 1 — The prayer prompt:
  "Generate good tests"
  Problem: No framework, no coverage requirements, no naming convention
  Fix: Add all three.

Anti-pattern 2 — The constraint-free prompt:
  "Refactor this code"
  Problem: No constraint on what must NOT change
  Fix: Add "Do not change public API" and "Existing tests must pass"

Anti-pattern 3 — The format-free prompt:
  "Review this code for issues"
  Problem: Output format varies every time — hard to scan consistently
  Fix: Add "Format: table with columns: Issue | Severity | Fix"

Anti-pattern 4 — The project-specific prompt committed as generic:
  "Use our BaseService class and the UserRepository pattern"
  Problem: Only works in one project, useless everywhere else
  Fix: Move project-specific context to copilot-instructions.md; keep prompts generic
```

---

## 7. Revision Checklist

- [ ] Can apply the CREST evaluation framework in under 2 minutes per diff
- [ ] Can identify all 4 hallucination types by their symptoms
- [ ] Detects agent scope drift (unexpected file modifications) immediately
- [ ] Has team-level AI governance norms (even for solo or 2-person team)
- [ ] Knows which code areas require human review gates (not just Copilot review)
- [ ] Runs monthly prompt audit (used/not used, quality, anti-patterns)
- [ ] Has a "Copilot got this wrong for us" log growing over time
