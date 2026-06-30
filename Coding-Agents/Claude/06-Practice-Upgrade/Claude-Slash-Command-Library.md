# Claude Slash Command Library

> **Track**: Claude Mastery Track — Group 6: Practice Upgrade
> **File**: 4 of 5 (Track File #33)
> **Usage**: Copy these to `.claude/commands/` in your project. Customize the constraints to match your stack.

---

## How to Install

```
1. Create the directory: mkdir -p .claude/commands
2. Copy the file: cp [command].cmd.md .claude/commands/[command].cmd.md
3. In Claude Code: /[command-name] — it's immediately available
4. Customize: replace [YOUR_FRAMEWORK], [YOUR_TEST_CMD], etc. with your actual values
```

---

## /explain — Structured Code Explanation

**File**: `.claude/commands/explain.cmd.md`

```markdown
---
description: Structured explanation of selected code or a file
---

Explain the code at @selection (or @file if no selection):

1. **Purpose**: What does this code do? (1-2 sentences)
2. **Inputs**: Each parameter — type, valid values, what happens for invalid input
3. **Execution paths**: All branches and what each produces
4. **External dependencies**: What it calls that it doesn't own
5. **Edge cases not handled**: What inputs would produce unexpected results
6. **How to use it**: One realistic usage example

Format: numbered sections with code examples where relevant.
```

---

## /debug — Error Diagnosis

**File**: `.claude/commands/debug.cmd.md`

```markdown
---
description: Diagnose and fix an error from the terminal or logs
---

Diagnose the error shown in @terminalLastCommand or @selection:

Context I'll provide after invoking:
- Stack: [my framework, language, version]
- What changed recently: [describe]
- Expected behavior: [what should happen]

Diagnose:
1. **Root cause**: WHY this error happens (not just what the error says)
2. **Fix**: The specific code change — show the diff
3. **Why it broke now**: What change triggered it
4. **Regression test**: What test would have caught this

Constraint: Do NOT modify test files to make tests pass.
```

---

## /refactor — Safe Refactoring

**File**: `.claude/commands/refactor.cmd.md`

```markdown
---
description: Refactor selected code with explicit safety constraints
---

Refactor @selection:

Goal: [state what structural change you want — will ask if not clear]

Rules:
- Do NOT change behavior — only structure
- Do NOT change public method signatures
- Run: [YOUR_TEST_CMD] after each change
- If any test breaks: undo the change and try a different approach
- Allowed: extract functions, rename variables, reduce duplication, extract classes
- Forbidden: change logic, modify error handling, add new abstractions not needed

After refactoring: run [YOUR_TEST_CMD] and report: pass count before vs after.
```

---

## /test — Test Generation with Gap Analysis

**File**: `.claude/commands/test.cmd.md`

```markdown
---
description: Generate comprehensive tests with mandatory gap analysis
---

Generate tests for @selection or @file:

Step 1 — Analyze:
- All valid inputs and their expected outputs
- All error conditions and what exception/response they produce
- Boundary values (empty, zero, None, max)

Step 2 — Generate tests covering:
- 1 happy path test
- 1 test per error/exception case
- 1 test per boundary value

Step 3 — Gap analysis:
After generating tests, report what is STILL NOT covered and why.

Rules:
- Test framework: [YOUR_TEST_FRAMEWORK] (pytest / jest / junit / etc.)
- Mock external dependencies only (HTTP, DB, filesystem) — not own logic
- Test naming: test_[function]_[scenario]_[expected_result]
- AAA pattern: Arrange / Act / Assert clearly separated

Run: [YOUR_TEST_CMD] and fix any failing tests before reporting done.
```

---

## /review — Pre-Commit Review

**File**: `.claude/commands/review.cmd.md`

```markdown
---
description: Complete pre-commit review: correctness, security, tests, docs
---

Review @selection or the changed files for this commit:

1. **Correctness**: Logic errors, missing null checks, wrong assumptions
2. **Security**: SQL injection, missing auth, hardcoded credentials, PII logging, OWASP
3. **Tests**: Error paths not tested, edge cases missing, tautological tests
4. **Error handling**: Unhandled exceptions, silent failures, missing rollback
5. **Backwards compatibility**: Does this break existing callers or API contracts?
6. **Conventions**: Does this follow the rules in CLAUDE.md?

Format: severity table
| SEVERITY | Issue | File:Line | Fix |
|----------|-------|-----------|-----|

End with: APPROVED / APPROVE WITH COMMENTS / CHANGES REQUIRED

Note: APPROVED means no CRITICAL or HIGH issues found.
```

---

## /security — Security-Focused Review

**File**: `.claude/commands/security.cmd.md`

```markdown
---
description: OWASP-focused security review — use on auth, SQL, and input handling
---

Perform a security review of @selection or @file:

Check for:
1. **SQL Injection**: string interpolation in queries vs parameterized queries
2. **Authentication bypass**: can this endpoint be reached without valid auth?
3. **Authorization**: does every operation verify the user has permission?
4. **Input validation**: is all user input validated before use?
5. **Sensitive data exposure**: PII/credentials in logs, error messages, or responses
6. **Insecure cryptography**: MD5/SHA1 for passwords, hardcoded salts, weak randomness
7. **Command injection**: shell=True with user input, unsafe subprocess calls
8. **Path traversal**: user-controlled file paths
9. **Rate limiting**: brute-force risk on auth or sensitive endpoints
10. **Dependency security**: any new packages added — are they from trusted sources?

For each finding:
  SEVERITY: CRITICAL / HIGH / MEDIUM / LOW
  ISSUE: what the vulnerability is
  ATTACK VECTOR: how it could be exploited
  FIX: specific code change
  OWASP: category (e.g., A1:2021 – Broken Access Control)
```

---

## /plan — Task Planning

**File**: `.claude/commands/plan.cmd.md`

```markdown
---
description: Plan a development task before implementation
---

Plan this task:

[Task description will be provided after invoking]

Using @codebase (or the specific files I provide):

1. **Implementation steps**: ordered by dependency (not by file)
2. **Files to modify**: which exact files and what changes in each
3. **New files to create**: name, purpose, key interfaces
4. **Risks**: what could go wrong, what needs clarification first
5. **Test plan**: what tests to write and when (test-first or after)
6. **Estimated complexity**: S (< 2h) / M (half day) / L (1+ days)
7. **Recommended Claude mode**: for each step (CLI agent / subagent / slash command)

Output this as a structured document I can save.
Do NOT implement anything. Plan only.
```

---

## /build — Full Build and Verify

**File**: `.claude/commands/build.cmd.md`

```markdown
---
description: Build and verify: implement + lint + test in one loop
---

Build: [task — describe what to implement]

Process:
  1. Implement the next component
  2. Run: [YOUR_LINT_CMD] — fix any errors before continuing
  3. Run: [YOUR_TEST_CMD] -x — fix any failures before continuing
  4. Only move to next component when lint + tests both pass

Files in scope: [list — or derived from context]

Forbidden:
  - Do NOT modify test files
  - Do NOT run database migrations
  - Do NOT install new packages without listing them first

Final report:
  Run: [YOUR_TEST_CMD] -v
  Report: lint status + X tests passed, Y failed, Z skipped
  List: what was built and what (if anything) remains
```

---

## /optimize — Performance Review

**File**: `.claude/commands/optimize.cmd.md`

```markdown
---
description: Performance analysis — find N+1 queries, inefficient algorithms, memory issues
---

Analyze performance of @selection or @file:

1. **N+1 queries**: ORM lazy loading, loops with DB calls inside
2. **Missing indexes**: queries filtering on unindexed columns
3. **Algorithm complexity**: O(n²) or worse where O(n) is possible
4. **Memory inefficiency**: loading entire datasets when pagination would work
5. **Caching opportunities**: repeated identical computations or DB reads
6. **Unnecessary work**: computations inside loops that could be hoisted

For each finding:
  IMPACT: HIGH / MEDIUM / LOW (on response time or resource usage)
  DESCRIPTION: what the issue is and why it's slow
  FIX: specific code change
  ESTIMATED IMPROVEMENT: rough order of magnitude

Do NOT suggest micro-optimizations (premature optimization). Focus on structural issues.
```

---

## /generate-notes — End-of-Day Learning Capture

**File**: `.claude/commands/generate-notes.cmd.md`

```markdown
---
description: Generate structured session notes for end-of-day learning capture
---

Generate learning notes for today's development session.

Topic I'll provide after invoking: [the main technical challenge I worked on]

Structure:
## What I Learned
[The main technical concept, 3-5 key points]

## Code Example
[The most instructive code snippet from today]

## Prompt That Worked Best
[The prompt I used that produced the best result]

## Prompt That Failed
[What didn't work and what I'll change next time]

## New Slash Command Needed
[If I typed the same prompt 2+ times today, describe it as a slash command]

## 3 Revision Questions
[3 questions I should be able to answer about today's topic next week]

Save as: notes/[YYYY-MM-DD]-session.md
```

---

## Revision Checklist

- [ ] All 9 core commands installed in .claude/commands/
- [ ] Each command has been run and tested at least once
- [ ] [YOUR_TEST_CMD] and [YOUR_LINT_CMD] replaced with actual commands for your stack
- [ ] /security run on at least one file from the current project
- [ ] /plan used for at least one real task this week
- [ ] /generate-notes used at end of last development session
