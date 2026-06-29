# Slash Commands — Gold Sheet

> **Track**: Claude Mastery Track — Group 2: Intermediate Power User
> **File**: 2 of 7 (Track File #8)
> **Audience**: Developers building a reusable Claude command library
> **Read after**: CLAUDE-MD-Design-Gold-Sheet.md

---

## 1. Practical Impact Meter

| Topic | Impact | Why Developers Miss This |
|---|---|---|
| Slash command anatomy — YAML + body | ★★★★★ | Devs don't know slash commands exist; re-type the same 50-word prompt daily |
| Dynamic input with $ARGUMENTS | ★★★★★ | Static commands are limited; arguments make them reusable for any task |
| Creating the 9 core commands | ★★★★★ | A library of 9 commands covers 80% of daily development workflows |
| Validation rules in commands | ★★★★☆ | Commands without validation produce inconsistent quality output |
| Prompt quality in commands | ★★★★★ | Bad prompt in a command = bad output every time; invest in quality once |

---

## 2. What Slash Commands Are

### Must Know

```
Slash commands are saved prompt templates for Claude Code.
Location: .claude/commands/<name>.cmd.md

After creating a command file, type /<name> in Claude Code to run it.
Claude reads the command file and executes it as a structured prompt.

Without slash commands:
  You type the same 50-word prompt every time you debug or generate tests.
  You forget constraints. You get inconsistent output.

With slash commands:
  Type /debug, Claude runs your expert-crafted debug prompt.
  Same quality every time. Zero repeated typing. Captured institutional knowledge.
```

### File Format

```markdown
---
description: One-line description shown in the command picker
---

[Your prompt body here]

$ARGUMENTS goes here to inject the user's arguments when running /command [args]
```

---

## 3. The 9 Core Commands

### Command 1: `/explain`

```markdown
---
description: Deep code explanation with patterns, gotchas, and context
---

Explain the following code to a developer who is new to this codebase:

$ARGUMENTS

Structure your explanation:
1. **One-line summary**: What does this code do?
2. **Why it exists**: What problem does it solve?
3. **How it works**: Step-by-step walkthrough of the logic
4. **Design patterns**: Name any patterns in use
5. **Gotchas**: Surprising behavior or edge cases
6. **Dependencies**: What does this rely on that isn't visible here?

Rules:
- Use "This does X" not "This is used to X"
- Reference specific variable/method names from the code
- Under 300 words unless the code is complex
```

### Command 2: `/debug`

```markdown
---
description: Root cause analysis with ranked fix options and prevention
---

Debug the following:

$ARGUMENTS

Provide:
1. **Root cause** (1 sentence — WHY, not just what)
2. **Why it happens** (the underlying mechanism)
3. **Fix options** (at least 2, ranked by: correctness → maintainability → effort)
4. **Recommended fix** (which option and why)
5. **Prevention** (how to avoid this class of error)

Rules:
- Do not suggest "check your syntax" unless there is a clear syntax error
- Do not suggest "restart the server" without a mechanism
- Root cause must explain WHY, not just describe the symptom
- Show fix as unified diff
```

### Command 3: `/refactor`

```markdown
---
description: Focused refactoring with constraints and diff output
---

Refactor the following:

$ARGUMENTS

Rules:
- Preserve ALL existing public API signatures
- Existing tests must still pass
- Do NOT add new external dependencies
- Do NOT add abstractions not needed for this specific change
- Do NOT add comments to every line

Output:
1. Refactored code (complete and runnable)
2. What changed (bullet list — each change + reason)
3. What was intentionally preserved
4. Follow-up opportunities (do NOT implement — just note)
```

### Command 4: `/test`

```markdown
---
description: Comprehensive test generation with coverage requirements
---

Generate tests for the following:

$ARGUMENTS

Requirements:
- Cover: happy path, at least 2 error scenarios, at least 2 edge cases
- Names: test_<function>_<scenario>_<expected_outcome>
- One logical assertion per test
- Mock all external dependencies (HTTP, email, DB for unit tests)
- Do NOT test implementation details — test behavior

Output: complete test file with imports, ready to run
```

### Command 5: `/review`

```markdown
---
description: Security + quality + test coverage review with severity labels
---

Perform a comprehensive code review of:

$ARGUMENTS

Evaluate:

**Security** (check each):
- Injection vulnerabilities (SQL, command, template)
- Missing auth/authz checks
- PII in logs or error responses
- Hardcoded credentials
- Insecure dependencies

**Quality**:
- Single responsibility violations
- Tight coupling
- Missing error handling
- Untested edge cases

**Test Coverage**:
- Functions with no tests
- Error paths not tested
- Edge cases missing

Format: | SEVERITY | Category | Issue | Location | Fix |
CRITICAL issues first. Skip INFO-level findings. Under 300 words.
```

### Command 6: `/plan`

```markdown
---
description: Session planning before any implementation
---

Plan the following task. Do NOT implement anything.

Task: $ARGUMENTS

Provide:
1. **Implementation steps** (3-7, ordered by dependency)
   - For each: specific action, files affected, Copilot mode (implement/test/review)
2. **Blockers** (questions to answer BEFORE coding — max 3)
3. **Risk flag** (most likely thing to go wrong)
4. **Success criteria** (testable statement of done)
5. **Estimate**: S (1-2h) / M (half day) / L (full day)

Rules:
- Be specific about file paths
- If a step requires a subagent: note which one
- Under 300 words
```

### Command 7: `/optimize`

```markdown
---
description: Performance + token efficiency analysis with prioritized improvements
---

Optimize the following:

$ARGUMENTS

Evaluate:

1. **Algorithmic complexity**: current Big-O, better option if exists
2. **N+1 queries**: ORM lazy loading producing extra queries?
3. **Memory**: unnecessary allocations, large intermediate collections?
4. **I/O patterns**: sequential awaits that could be parallel?
5. **Caching opportunities**: pure function results that repeat?

For each finding:
| Issue | Location | Impact | Fix | Effort |
|-------|----------|--------|-----|--------|

Show code fix for the top 2 findings only.
Do NOT suggest premature micro-optimizations.
```

### Command 8: `/build`

```markdown
---
description: Scaffold a new project or feature using plan-first approach
---

Build the following:

$ARGUMENTS

Process:
STEP 1 — PLAN (always first):
  List exact files to create/modify with one-sentence purpose each.
  State assumptions and dependencies.
  Wait for my approval before building.

STEP 2 — BUILD (after explicit approval):
  Create files in dependency order.
  After creating each file: run available tests/lint.

STEP 3 — VERIFY:
  Run all tests. Report results.
  Produce summary: what was created, what to do next.

Rules:
- Production-quality code only (no hello world examples)
- Every generated file must have at least one test
- No hardcoded values — all configurable via environment
- README commands must be copy-paste runnable
```

### Command 9: `/token-efficient`

```markdown
---
description: Rewrite a verbose prompt to be compact and high-signal
---

Rewrite the following prompt to be more token-efficient:

$ARGUMENTS

Improve by:
1. Remove courtesy preamble ("Hi! I'm working on...")
2. Replace description with file reference (@file:path)
3. Add explicit output format ("diff only", "table", "under 200 words")
4. Add one key constraint ("keep public API", "no new dependencies")
5. Remove redundant information Claude can infer

Output:
1. Rewritten prompt (target: under 50 words)
2. What was changed and why (bullet list)
3. Estimated token savings (rough percentage)
```

---

## 4. Command File Organization

```
.claude/commands/
  # Core development
  explain.cmd.md
  debug.cmd.md
  refactor.cmd.md
  test.cmd.md

  # Review and quality
  review.cmd.md
  security-review.cmd.md    ← specialized review
  performance-review.cmd.md ← specialized review

  # Planning and building
  plan.cmd.md
  build.cmd.md

  # Optimization
  optimize.cmd.md
  token-efficient.cmd.md

  # Documentation
  docs.cmd.md               ← generate README, docstrings, ADRs
  commit-message.cmd.md     ← generate conventional commit message
  pr-description.cmd.md     ← generate PR description

  # Learning
  learn.cmd.md              ← generate revision notes on any topic
```

---

## 5. Using Commands with Arguments

```bash
# Basic usage — Claude reads $ARGUMENTS from what you type after /command
/debug the process_refund function is raising AttributeError on line 87

# Reference files as arguments
/review @file:src/services/payment_service.py

# Multiple context items
/test @file:src/services/user_service.py — focus on create_user()

# Explicit task description
/plan implement user notification preferences with email, in-app, and SMS toggles

# Token efficiency rewrite
/token-efficient [paste your verbose prompt here]
```

---

## 6. Command Quality Principles

```
Principle 1 — Every command must specify output format:
  BAD: "Review this code"
  GOOD: "Format: | SEVERITY | Issue | Fix | (table). CRITICAL first."

Principle 2 — Commands must have constraints:
  BAD: "Refactor this code"
  GOOD: "Keep public API identical. No new dependencies. Diff only."

Principle 3 — Commands must be generic (no hardcoded file names):
  BAD: "Generate tests for UserService using our db_session fixture"
  GOOD: "Generate tests for $ARGUMENTS. Mock all external dependencies."
  (Project-specific context: put in CLAUDE.md, not commands)

Principle 4 — One goal per command:
  BAD: /super-command that does review + refactor + generate tests
  GOOD: Three separate commands — each does one thing well
```

---

## 7. Revision Checklist

- [ ] Has `.claude/commands/` directory in at least one project
- [ ] Has created the 9 core commands: explain, debug, refactor, test, review, plan, optimize, build, token-efficient
- [ ] Knows how $ARGUMENTS works in command files
- [ ] Commands specify output format (table, diff, word limit)
- [ ] Commands have "Do NOT" constraints
- [ ] Project-specific context is in CLAUDE.md, not commands (commands are generic)
- [ ] Can run any command by typing /command-name in Claude Code
