# SDLC Automation — Gold Sheet

> **Track**: Codex Mastery Track — Group 4: Pro / Production Level
> **File**: 2 of 5 (Track File #22)
> **Audience**: Developers who want Codex integrated at every phase of the development lifecycle
> **Read after**: Personal-Codex-Operating-System-Gold-Sheet.md

---

## Practical Impact Meter

| Topic | Impact | Why Developers Miss This |
|---|---|---|
| Requirements → scope definition before coding | ★★★★★ | Skipping planning = scope explosion during implementation |
| Test-first before any implementation | ★★★★★ | Tests after implementation mirror the code; tests before define the spec |
| Pre-commit review as mandatory gate | ★★★★★ | Most AI-generated security bugs ship because no review was done pre-commit |
| Incident debug loop with Codex | ★★★★☆ | Codex can analyze stack traces and logs without seeing real PII |
| Learning capture from each PR | ★★★☆☆ | Insights from Codex sessions that aren't captured are lost forever |

---

## ⭐ Beginner Tier — Start Here

### B1: Run Phase 1 on your current ticket

```bash
# Before writing a single line of code for your current ticket:
codex --model gpt-4.1 --approval-policy suggest \
  "Help me clarify requirements for: [paste your ticket description]
   1. What exactly is 'done'? (acceptance criteria)
   2. What edge cases should we handle vs explicitly NOT handle?
   3. Who calls this? What format do they expect?
   4. What happens on failure?
   Do not implement. Clarify only."
```

If this reveals unclear requirements: you've saved 30-60 minutes of wrong implementation.
This is the most high-leverage phase in the SDLC.

### B2: Always test-first (Phase 3 before Phase 4)

```bash
# WRONG order (tests after): implement first, write tests to match
codex "implement [feature]; then write tests"
# Result: tests mirror the implementation, even if the implementation is wrong

# RIGHT order (Phase 3 before Phase 4)
codex "write tests for [feature] that doesn't exist yet.
       Tests should FAIL now. They define the spec."
# Then:
codex "make these tests pass (do not modify test files)"
```

---

## Phase 1 — Requirements Clarity

```bash
# BEFORE writing any code: clarify what you're building
codex --model gpt-4.1 --approval-policy suggest \
  "I need to implement: [feature description]
   
   Help me clarify the requirements:
   1. What are the acceptance criteria? (What exactly constitutes 'done'?)
   2. What edge cases should we handle vs explicitly not handle?
   3. What are the performance requirements? (100 req/s? 1000 users?)
   4. What are the security requirements? (Who can call this? What data is involved?)
   5. What are the failure modes? (What happens when the database is down?)
   6. What depends on this? (Who calls it? What format do they expect?)
   
   Output: a requirements document I can paste into the implementation ticket.
   Do NOT implement — clarify only."
```

---

## Phase 2 — Architecture Planning

```bash
codex --model gpt-4.1 --approval-policy suggest \
  "Design the implementation for: [feature from Phase 1]
   
   Current system context:
   [paste relevant sections of AGENTS.md]
   
   Constraints:
   - Layer architecture: [describe your layers]
   - Existing patterns to follow: [reference files]
   - What must NOT change: [existing interfaces, DB schema if frozen]
   
   Output:
   1. Files to create (with their interfaces)
   2. Files to modify (what changes in each)
   3. Data model changes (if any)
   4. Dependency changes (new libraries needed?)
   5. Migration path (how to go from current state to target state)
   
   Do NOT implement. Architecture only."
```

---

## Phase 3 — Test-First Specification

```bash
# Write tests before implementation — tests ARE the specification
codex --approval-policy auto-edit \
  "Write the test specification for: [feature]
   Based on requirements: [paste from Phase 1]
   
   Create test file: tests/test_[feature].py
   
   Test cases:
   - [list each requirement as a test case]
   - Include: happy paths, error cases, boundary values, security edge cases
   
   These tests will FAIL until implementation is complete.
   Run: pytest tests/test_[feature].py --collect-only
   All tests must be collected (even if failing). Fix syntax errors only."
```

---

## Phase 4 — Implementation

```bash
# Build against the test specification
codex --approval-policy auto-edit \
  "Implement [feature] to make the tests in tests/test_[feature].py pass.
   
   Architecture plan: [reference Phase 2 output or paste it]
   Files to create: [list from Phase 2]
   Files to modify: [list from Phase 2]
   
   Process:
   1. Implement one component at a time (in dependency order)
   2. After each component: run pytest tests/test_[feature].py -x
   3. Fix failures before the next component
   4. Final: run full suite pytest -x
   
   Constraints:
   - Do not modify test files
   - Follow AGENTS.md conventions
   - Follow architecture plan exactly
   
   Done = all Phase 3 tests pass + full suite passes"
```

---

## Phase 5 — Test Coverage Audit

```bash
codex "Audit test coverage for the implemented [feature].
       
       Implementation files: [list]
       Test files: [list]
       
       Report:
       1. Branches in implementation not covered by tests
       2. Error paths that are tested vs not tested
       3. Security edge cases not tested (auth, input validation, injection)
       4. Integration paths not tested
       5. Suggested 3 most valuable additional tests to add
       
       Do not generate tests yet — audit only."

# Then add the missing tests
codex --approval-policy auto-edit \
  "Add the missing tests identified: [paste audit output]
   Only tests that are genuinely missing — not duplicates of existing coverage.
   Run: pytest tests/test_[feature].py -v"
```

---

## Phase 6 — Pre-Commit Review

```bash
# Run before every git commit
CHANGED=$(git diff --staged --name-only)

codex --approval-policy suggest \
  "Pre-commit review for these staged files: $CHANGED
   
   Review checklist:
   1. Security: SQL injection, missing auth, hardcoded credentials, PII in logs
   2. Correctness: logic errors, wrong assumptions, missing null checks
   3. Test coverage: error paths in changed code that aren't tested
   4. Backwards compatibility: does this break any existing callers?
   5. AGENTS.md compliance: layer rules, naming, error handling conventions
   
   Format: | SEVERITY | ISSUE | FILE:LINE | FIX |
   Final verdict: APPROVED / APPROVE WITH COMMENTS / CHANGES REQUIRED
   
   Do not make changes — review only."
```

---

## Phase 7 — PR Description Generation

```bash
CHANGED=$(git diff main..HEAD --name-only)
COMMITS=$(git log main..HEAD --oneline)

codex "Generate a pull request description for:
       Commits: $COMMITS
       Files changed: $CHANGED
       
       Format:
       ## Summary
       [What this PR does — for a reviewer who knows the codebase]
       
       ## Changes
       [Bullet list of key changes — what was added/modified/removed]
       
       ## Testing
       [What tests cover this change — specific test file/function names]
       
       ## Security Notes
       [Any security-relevant changes: new endpoints, auth changes, data handling]
       
       ## How to Review
       [What the reviewer should focus on — key files and what to check]
       
       Keep it factual — only include what's verifiable from the commits and files."
```

---

## Phase 8 — CI Failure Resolution

```bash
# When CI fails
CI_LOG=$(cat ci-failure.log | tail -100)

codex "CI is failing. Here is the failure output:
       $CI_LOG
       
       Context:
       - CI environment: GitHub Actions, Ubuntu 22.04, Python 3.11
       - Local environment: macOS 14 (same Python version)
       - Tests pass locally but fail in CI
       
       Diagnose:
       1. Root cause (not just the error message — WHY it fails in CI but not locally)
       2. The minimum fix needed
       3. How to verify the fix before pushing again
       
       Common CI vs local differences to check:
       - Missing environment variables in CI
       - Timezone differences (UTC in CI vs local)
       - File system case sensitivity (Linux vs macOS)
       - Missing test data setup in CI fixtures
       
       Fix constraint: do not modify test files."
```

---

## Phase 9 — Documentation Update

```bash
# After implementation is complete and merged
codex --approval-policy auto-edit \
  "Update documentation for the [feature] implementation.
   
   Files that need updating:
   1. README.md: add [feature] to the API endpoints section
   2. src/[module]/[file].py: update docstrings for modified functions
   3. docs/architecture.md: update if this changed how the system works
   
   Critical: only update with information that is verifiably in the new code.
   Mark anything uncertain as: [TODO: verify]
   
   Verification: no test failures (run: pytest -x)"
```

---

## Phase 10 — Post-Incident Learning Capture

```bash
# After resolving a production incident
codex "Help me write an incident learning capture.
       
       What happened (using synthetic/anonymized data):
       [describe the incident without real user data, IPs, or internal system details]
       
       Root cause (as we understand it):
       [what was the technical cause]
       
       Generate:
       1. Incident summary (for the team wiki)
       2. Root cause analysis (5-whys format)
       3. Was AI-generated code involved? If so, what failed in the review process?
       4. Three concrete prevention actions
       5. Tests that would have caught this before production
       
       Output in Markdown format."
```

---

## Quick Reference: Codex Mode by Phase

| Phase | Approval Policy | Model | Verification |
|-------|----------------|-------|-------------|
| Requirements | suggest | gpt-4.1 | Human review |
| Architecture | suggest | gpt-4.1 | Human review |
| Test-first | auto-edit | o4-mini | pytest --collect-only |
| Implementation | auto-edit | o4-mini | pytest -x |
| Coverage audit | suggest | o4-mini | Human review |
| Pre-commit review | suggest | o4-mini | Human decision |
| PR description | suggest | o4-mini | Human review |
| CI fix | auto-edit | o4-mini | pytest -x |
| Documentation | auto-edit | gpt-4.1-mini | pytest -x |
| Learning capture | suggest | gpt-4.1 | Human review |

---

## Interview Traps

```
TRAP: "Skip requirements clarity and go straight to architecture + implementation"
TRUTH: Ambiguous requirements produce correct implementations of the wrong feature.
       5 minutes on Phase 1 (requirements clarity) prevents 30-60 minutes of rework
       when the implementation doesn't match what the ticket actually needed.

TRAP: "Test-first (Phase 3) is optional if I already have unit tests"
TRUTH: Test-first specification means writing tests that define 'done' BEFORE any
       implementation code exists. This is different from having existing tests.
       Tests-before-code surface missing requirements during specification, not debugging.

TRAP: "Post-incident learning (Phase 10) is only for major production outages"
TRUTH: The highest-value learning often comes from small bugs that reach production.
       Even a 30-minute issue reveals: a detection gap, a test gap, and a review gap.
       Capturing these three items prevents the same class of bug from recurring.
```

---

## Revision Checklist

- [ ] Plan before implementing: at minimum Phase 2 (architecture) before Phase 4 (implementation)
- [ ] Tests written before implementation (Phase 3 before Phase 4)
- [ ] Pre-commit review run before every PR (Phase 6)
- [ ] Can run a complete Phases 1-7 cycle on a real feature
- [ ] CI failure resolution uses the structured diagnosis prompt
- [ ] Learning capture used after any significant bug or incident
