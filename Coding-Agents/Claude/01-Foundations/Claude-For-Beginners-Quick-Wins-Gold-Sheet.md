# Claude For Beginners — Quick Wins — Gold Sheet

> **Track**: Claude Mastery Track — Group 1: Foundations
> **File**: 6 of 6 (Track File #6)
> **Audience**: Developers completing the Foundations path — hands-on exercises
> **Read after**: Safe-Usage-Principles-Gold-Sheet.md

---

## How to Use This Sheet

Complete all 10 exercises before moving to the Intermediate path.
Each exercise takes 5-15 minutes. Use real code from your own projects.
Follow the Safe Usage rules — no real secrets in any prompt.

---

## Quick Win #1 — Explain Unfamiliar Code

**Setup**: Find a function in your codebase you've been meaning to understand.

**Exercise**:
```
In Claude Code or Chat:
"Explain this code to a developer who hasn't seen this codebase before:
@file:[path to file containing the function]

Focus on the [function name] function:
1. What does it do?
2. What problem does it solve?
3. What are the non-obvious parts?
4. What edge cases does it handle?

Under 200 words."
```

**Success criteria**: You can explain the function to a teammate without looking at Claude's response.

---

## Quick Win #2 — Debug with Root Cause

**Setup**: Find a recent error from your project logs or tests (use anonymized data).

**Exercise**:
```
"Diagnose this error:
Error: [paste 3-5 lines of error + stack trace]
Code: @file:[the file where error occurs]

Tell me:
1. Root cause (1 sentence — WHY, not just what)
2. Fix (code change as diff)
3. Prevention (how to avoid this class of error)

Under 150 words."
```

**Success criteria**: You understand WHY the error occurs, not just how to fix it.

---

## Quick Win #3 — Generate Tests

**Setup**: Find a function in your project with no tests.

**Exercise**:
```
"Generate tests for this function:
@file:[path to function]

Requirements:
- Framework: [your test framework]
- Cover: happy path, at least 2 error cases, edge cases (None, empty, boundary)
- Mock any external dependencies
- Test names: [your naming convention]
- Output: complete test file with imports, ready to run"
```

**Run the tests**: Confirm they pass or identify real bugs found.

**Success criteria**: At least 5 tests generated; you understand what each tests.

---

## Quick Win #4 — Generate a Docstring

**Setup**: Pick any undocumented public function.

**Exercise**:
```
Select the function → In Claude Code:
"Add a docstring to this function.
Style: [Google/NumPy/JSDoc/Javadoc — pick one]
Include: purpose, all parameters, return value, any exceptions raised, one usage example.
Do NOT change the function body."
```

**Success criteria**: Docstring accurately describes what the function does.

---

## Quick Win #5 — Security Review

**Setup**: Pick any file in your project that handles user input.

**Exercise**:
```
"Security review of @file:[path]:
Check: SQL injection, hardcoded credentials, missing input validation,
       PII in logs, error messages exposing internals.
Format: [SEVERITY] — [location] — [issue] — [fix]
CRITICAL first. Skip INFO. Under 150 words."
```

**Success criteria**: You found at least one issue (most real code has some) and understand how to fix it.

---

## Quick Win #6 — Refactor for Readability

**Setup**: Find a function that is too long or has nested conditionals.

**Exercise**:
```
"Refactor @file:[path] — specifically [function name]:
Goal: improve readability by reducing nesting or extracting helpers
Keep: the exact same public behavior and tests passing
Do NOT: change function signature, add new classes, modify tests
Output: unified diff only"
```

**Success criteria**: The refactored function is easier to read; existing tests still pass.

---

## Quick Win #7 — Generate a README

**Exercise**:
```
"Generate a README.md for this project.

Read: @file:[a few key files for context]

Required sections (in this order):
## What This Does (2 sentences)
## Prerequisites (bullet list with versions)
## Installation (numbered steps — every command must be copy-paste runnable)
## Running Locally (exact command + URL)
## Running Tests (exact command)
## Project Structure (directory tree, one-line description per dir)
## Contributing (3-bullet summary)

Rules:
- Every command must be copy-paste runnable
- Target reader: developer who has never seen this repo
- Under 400 words"
```

**Success criteria**: A developer could set up the project from the README alone.

---

## Quick Win #8 — Create a GitHub Actions Workflow

**Exercise**:
```
"Create a GitHub Actions CI workflow for this project.

Stack: [your stack — language + framework + test framework]
Trigger: push to main, pull_request
Requirements:
- Run lint, then type check, then tests (in that order)
- Cache dependencies for faster runs
- Fail fast on first error
- Pin all action versions (not @latest)
- Add concurrency group to cancel stale PR runs

Output: complete .github/workflows/ci.yml"
```

**Review before committing**: Verify all action versions are pinned, no hardcoded secrets.

---

## Quick Win #9 — Learn a New Concept

**Exercise**:
```
"Create revision notes on: [a technical concept you've been meaning to learn]

Format:
## What It Is (2 sentences)
## Why It Matters (consequence of not knowing)
## How It Works (numbered steps)
## Key Rules (5 bullets)
## Code Example (runnable, under 20 lines)
## Common Mistakes (bad → good pattern)
## Revision Questions (5 applied questions)

Under 500 words."
```

**Success criteria**: You can answer the 5 revision questions without looking at the notes.

---

## Quick Win #10 — Daily Planning

**Exercise** (do this tomorrow morning):
```
"Plan my coding session:
Today's task: [paste your ticket or goal]

Give me:
1. Implementation steps (3-7, ordered by dependency)
2. Relevant files in this project: [mention the project]
3. Blockers I should resolve before coding
4. Copilot mode for each step (plan/implement/test/review)
5. Success criteria: how I'll know I'm done

Do not implement. Planning only."
```

**Success criteria**: You have a clear, sequenced plan before writing a single line of code.

---

## Foundations Completion Checklist

```
Sheets read:
[ ] Claude Mental Model
[ ] Claude Setup (Claude.ai + Claude Code CLI working)
[ ] Claude Chat Fundamentals (Project created with instructions)
[ ] Prompt Engineering Fundamentals (CRISP pattern learned)
[ ] Safe Usage Principles (checkpoint commit habit + no secrets)
[ ] Quick Wins (this sheet)

Exercises completed:
[ ] QW1: Explained unfamiliar code with Claude
[ ] QW2: Debugged an error with root cause analysis
[ ] QW3: Generated a test suite for a real function
[ ] QW4: Added a docstring to a real function
[ ] QW5: Ran a security review on a real file
[ ] QW6: Refactored a real function for readability
[ ] QW7: Generated a README for a real project
[ ] QW8: Created a GitHub Actions CI workflow
[ ] QW9: Generated learning notes for a concept
[ ] QW10: Used /plan for morning session planning

Baseline habits established:
[ ] CLAUDE.md in at least one project
[ ] Checkpoint commits before agent sessions
[ ] Never pasted a secret into Claude
[ ] Read diffs before accepting all changes

Ready for: 02-Intermediate-Power-User/CLAUDE-MD-Design-Gold-Sheet.md
```
