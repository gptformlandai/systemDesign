# Copilot Prompt Library Templates

> **Track**: Copilot Mastery Track — Group 6: Practice Upgrade
> **File**: 2 of 5 (Track File #33)
> **Usage**: Copy these into `.github/prompts/` in any project. Customize where marked [CUSTOMIZE].

---

## How to Use This File

Each section is a complete, copy-paste-ready prompt file.
Save each as `.github/prompts/<name>.prompt.md`.
After saving, type `/` in Copilot Chat to see them as slash commands.

---

## Template 1: `/refactor-code`

```markdown
---
name: Refactor Code
description: Clean refactoring with explanation of every change made
---

Refactor the following code:

${selection}

Primary goal: ${input:What is the refactoring goal? (e.g., extract method, reduce duplication, improve readability, apply SOLID)}

Rules:
- Preserve ALL existing public API signatures unless explicitly asked to change them
- Existing tests must still pass after the refactoring
- Do not add new dependencies unless they are clearly better than what exists
- Do not migrate to a different framework or library
- Do not add abstractions that are not needed for this specific change

Output:
1. The refactored code (complete and runnable)
2. What changed and WHY each change was made
3. What you intentionally did NOT change and why
4. Any follow-up opportunities (do not implement — just note them)
```

---

## Template 2: `/security-review`

```markdown
---
name: Security Review
description: OWASP-aligned security review with severity-ranked findings
---

Perform a security review of:

${selection}

Check for:
1. Injection: SQL, command, template, path traversal
2. Authentication: auth bypass, missing auth checks
3. Authorization: missing permission checks, IDOR
4. Input validation: unvalidated user input used in operations
5. Output encoding: XSS, unescaped output
6. Cryptography: weak algorithms, hardcoded keys, predictable values
7. Sensitive data: PII/secrets in logs, responses, or error messages
8. Dependency risk: known vulnerable libraries (flag if visible)
9. Rate limiting: brute force potential on sensitive endpoints
10. Error disclosure: stack traces or internal paths in error responses

For each finding:
SEVERITY: CRITICAL / HIGH / MEDIUM / LOW
CATEGORY: [OWASP Top 10 category]
ISSUE: [Description]
ATTACK VECTOR: [How it could be exploited]
FIX: [Specific code change]

If no issues in a category: state "No issues found"
```

---

## Template 3: `/architecture-review`

```markdown
---
name: Architecture Review
description: Structured architecture review with prioritized improvements
---

Review the following for architectural quality:

${selection}

Evaluate:
1. Separation of Concerns — are responsibilities cleanly separated?
2. Coupling — what is tightly coupled that should be loosely coupled?
3. SOLID — which principles are violated? Which are followed?
4. Testability — what makes this hard to unit test?
5. Scalability — what breaks first under 10x load?
6. Maintainability — what will be painful to change in 6 months?
7. Missing abstractions — what domain concept has no clear representation?

For each issue:
| Priority | Issue | Consequence | Improvement |
|----------|-------|-------------|-------------|
| HIGH | ... | ... | ... |
| MEDIUM | ... | ... | ... |
| LOW | ... | ... | ... |

Be direct. Do not compliment the code.
Focus on issues that have actual consequences if not addressed.
```

---

## Template 4: `/write-pr-description`

```markdown
---
name: Write PR Description
description: Generate a structured PR description from changed files and notes
---

Generate a GitHub PR description for my changes.

What I changed:
${input:Describe what you changed in 2-3 sentences (Copilot will format this)}

Changed files for context:
${selection}

Format exactly:
## Summary
[2-3 sentences: what changed and why]

## Changes Made
- [Bullet: specific change 1]
- [Bullet: specific change 2]
[...]

## How to Test
1. [Step 1: how to verify this works]
2. [Step 2]
[...]

## Breaking Changes
[None — or describe breaking change and migration path]

## Checklist
- [ ] Tests added or updated
- [ ] Security implications considered
- [ ] Documentation updated if needed
- [ ] No hardcoded values or credentials

Rules:
- Under 250 words total
- Factual only — no marketing language
- "Fixed crash when..." not "Fixed NullPointerException in..."
- Every test step must be runnable by someone who didn't write the code
```

---

## Template 5: `/generate-learning-notes`

```markdown
---
name: Generate Learning Notes
description: Create structured, revision-ready learning notes on any topic
---

Create structured learning notes on:

Topic: ${input:What topic should I create notes for?}

Format (follow exactly):
## [Topic Name]

### What It Is
[2-3 sentence definition — precise, not vague]

### Why It Matters
[Why a developer needs to understand this — practical consequences of NOT knowing]

### How It Works
[Internals or mechanics — step by step. Prefer numbered list over prose]

### Key Rules
[5-7 bullet points: the most important things to remember]

### Code Example
[Minimal but complete working example — no placeholder code]

### Common Mistakes
[3-5 mistakes developers make with this topic — concrete examples]

### Strong Explanation
[How to explain this topic clearly in 3-5 sentences to a colleague or in an interview]

### Revision Questions
[5 questions to test understanding — not trivial "what is X" but applied "what happens when Y"]

Rules:
- Code examples must work — no pseudocode unless the concept is language-agnostic
- Prefer concrete over abstract: "This returns a list of 5 integers" not "This returns data"
- If the topic is language-specific, default to [CUSTOMIZE: your primary language]
```

---

## Template 6: `/codebase-onboarding`

```markdown
---
name: Codebase Onboarding
description: Fast architecture and structure overview for a new codebase
---

Using #codebase, give me an onboarding overview:

1. **What it does** (2 sentences max)

2. **Architecture pattern** (1 sentence: layered/hexagonal/event-driven/etc.)

3. **Request flow** (step by step from entry point to data layer):
   Step 1: [entry point file]
   Step 2: ...

4. **Key files to know** (5 most important files + one-line description each)

5. **Testing strategy** (what's tested, what's not, which framework)

6. **Most fragile area** (where are the most bugs likely? Why?)

7. **First contribution guide** (3 steps to make a safe first change)

Rules:
- Cite specific file paths for every claim
- Under 300 words total
- If the codebase is too large to analyze fully: say so and analyze the most relevant subset
```

---

## Template 7: `/commit-message`

```markdown
---
name: Commit Message
description: Generate a conventional commit message from staged changes
---

Generate a conventional commit message for my staged changes.

Changes: ${selection}

Format: type(scope): description

Types: feat, fix, refactor, test, docs, chore, perf, ci

Rules:
- Description: under 72 characters
- Imperative mood: "add validation" not "added validation"
- Scope: the module or feature area affected (optional but preferred)
- Body (if needed): explain WHY not WHAT
- Breaking change: add "BREAKING CHANGE:" in body if applicable

Examples:
  feat(auth): add JWT refresh token rotation
  fix(orders): prevent double-processing of concurrent order submissions
  test(payment): add edge cases for partial refund validation
  chore(deps): upgrade stripe-python to 7.0.0
```

---

## Template 8: `/generate-adr`

```markdown
---
name: Generate ADR
description: Create an Architecture Decision Record for a technical decision
---

Generate an ADR for:

Decision: ${input:What architectural decision was made? (one sentence)}

Context: ${input:What problem or situation led to this decision?}

Format:
# ADR-[NNN]: [Short descriptive title]
Date: [today]
Status: Accepted

## Context
[2-3 sentences: the problem and why a decision was needed]

## Options Considered

### Option 1: [Name]
**Pros:** [2-3 bullet points]
**Cons:** [2-3 bullet points]

### Option 2: [Name]
[same format]

### Option 3: [Name]
[same format]

## Decision
[What was chosen and the primary reason — one paragraph]

## Consequences
**Positive:** [what improves]
**Negative:** [what gets harder]
**Neutral:** [what changes without clear positive or negative impact]

## Compliance
[How code reviewers enforce this decision going forward]
```

---

## Quick Reference: All Available Prompts

| Slash Command | Use Case |
|---|---|
| `/explain-code` | Understand unfamiliar code |
| `/debug-error` | Diagnose errors with root cause |
| `/generate-tests` | Create unit tests |
| `/refactor-code` | Clean up code |
| `/security-review` | OWASP-aligned security check |
| `/architecture-review` | Structural quality review |
| `/write-pr-description` | Generate PR description |
| `/generate-learning-notes` | Study a new topic |
| `/codebase-onboarding` | Understand a new codebase |
| `/commit-message` | Generate a commit message |
| `/generate-adr` | Document an architecture decision |
| `/create-github-action` | Generate a CI workflow |
