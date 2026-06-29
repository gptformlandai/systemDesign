# Skills System — Gold Sheet

> **Track**: Claude Mastery Track — Group 3: Advanced Engineering
> **File**: 2 of 7 (Track File #15)
> **Read after**: Subagents-Deep-Dive-Gold-Sheet.md

---

## 1. What Skills Are

### Must Know

```
A Skill is a reusable capability Claude invokes automatically when it detects
the relevant task type.

Skills live in: .claude/skills/<name>/SKILL.md

A Skill file contains:
  - When to invoke this skill (trigger conditions)
  - What this skill does (the workflow)
  - What inputs it expects
  - What output it produces
  - Quality standards to enforce

How Skills differ from Slash Commands:
  Slash commands: you explicitly type /command
  Skills: Claude reads them and decides when to apply them automatically
  
  When you say "Write tests for this service"
  → Claude reads the testing SKILL.md
  → Applies the documented workflow automatically
  → Produces consistent, high-quality test output every time
```

---

## 2. The 4 Core Skills

### Skill 1: Testing

**File**: `.claude/skills/testing/SKILL.md`

```markdown
# Testing Skill

## When to Invoke
Apply this skill when the task involves:
- Writing unit tests for any function or class
- Performing test gap analysis
- Generating integration tests
- Reviewing test quality

## Workflow

1. Read the implementation file to understand behavior
2. Identify public methods (not private implementation details)
3. For each public method, generate:
   - Happy path test
   - At least 2 error condition tests
   - At least 2 edge case tests (None, empty, boundary)
4. Check for external dependencies — mock all of them
5. Run generated tests — fix any that fail
6. Report: tests created, coverage estimate, any gaps found

## Quality Standards
- Test names: test_<function>_<scenario>_<expected_outcome>
- One logical assertion per test
- Tests must test BEHAVIOR, not implementation
- All mocks must be properly set up and verified
- Complete test file with all imports

## Output Format
1. Complete test file (ready to run)
2. Gap analysis: list of untested paths
3. Test results: pass/fail count after running
```

### Skill 2: Refactoring

**File**: `.claude/skills/refactoring/SKILL.md`

```markdown
# Refactoring Skill

## When to Invoke
Apply this skill when the task involves:
- Improving code quality without changing behavior
- Extracting functions or classes
- Reducing code duplication
- Applying design patterns
- Improving readability

## Workflow

1. Read the target code to understand current behavior
2. Run existing tests to establish baseline (record which pass)
3. Identify the refactoring goal (what exactly to improve)
4. Create a refactoring plan (what changes, in what order)
5. Make changes incrementally (one logical change at a time)
6. After each change: run tests to verify behavior unchanged
7. Report: what changed, what was preserved, test results

## Constraints (always enforce)
- Public API signatures must remain identical unless explicitly asked to change
- Existing tests must still pass after every change
- Do NOT add abstractions not needed for this specific refactoring
- Do NOT change test files to match refactored code (tests define correct behavior)
- Maximum one new class per refactoring session (avoid over-engineering)

## Output Format
1. Unified diff (not full file rewrite)
2. Change explanation (what changed + why for each change)
3. Test results before and after
```

### Skill 3: Documentation

**File**: `.claude/skills/documentation/SKILL.md`

```markdown
# Documentation Skill

## When to Invoke
Apply when the task involves:
- Writing or improving docstrings
- Creating README files
- Generating API documentation
- Writing Architecture Decision Records (ADRs)
- Creating onboarding guides

## Workflow

1. Read the code to understand WHAT it does (not what the author intended)
2. Identify the target audience (developer, user, API consumer)
3. Document based on observed behavior, not assumptions
4. Verify all commands are copy-paste runnable
5. Review: every statement is factual (not aspirational)

## Quality Standards
- Every command in documentation must be runnable (test it)
- Use "This does X" not "This is intended to X"
- Audience-appropriate depth and language
- Under 500 words for READMEs (link to details, don't embed them)
- Docstrings: purpose + all params + return + raises + one example

## Format by Document Type

README:
  ## What This Does (2 sentences)
  ## Prerequisites (bullet list + versions)
  ## Installation (numbered steps)
  ## Running (exact commands)
  ## Testing (exact command)

Docstring (Google style):
  """One-sentence summary.
  Args: name (type): description
  Returns: description
  Raises: ExceptionType: when
  Example: one line
  """
```

### Skill 4: Performance

**File**: `.claude/skills/performance/SKILL.md`

```markdown
# Performance Analysis Skill

## When to Invoke
Apply when the task involves:
- Analyzing slow code
- Finding bottlenecks
- Reviewing database query patterns
- Memory optimization

## Workflow

1. Read the code and identify the performance hot path
2. Analyze: algorithmic complexity, I/O patterns, DB queries
3. Check for N+1 query patterns (ORM lazy loading)
4. Check for unnecessary allocations in hot paths
5. Check for sequential awaits that could be parallel
6. Prioritize findings by impact (not effort)
7. Show fix code for the top 2 findings only

## Analysis Dimensions
- Big-O complexity: current vs possible improvement
- DB query count: does N items = N queries?
- Memory: large intermediate collections that aren't needed?
- Concurrency: sequential waits that could be parallel?

## Output Format
| Issue | Location | Impact | Fix | Effort |
|-------|----------|--------|-----|--------|

Then: code fix for the #1 finding
Do NOT suggest micro-optimizations without evidence of actual impact.
```

---

## 3. Skill Invocation in Practice

```
Skills are invoked through CLAUDE.md or session context:

In CLAUDE.md:
  ## Skills
  - Testing: follow .claude/skills/testing/SKILL.md for all test generation
  - Refactoring: follow .claude/skills/refactoring/SKILL.md for all refactoring tasks

In a prompt:
  "Write tests for @file:src/services/user_service.py
  following the testing skill in @file:.claude/skills/testing/SKILL.md"

Automatic invocation (with CLAUDE.md reference):
  "Generate tests" → Claude reads testing SKILL.md → applies workflow
  "Refactor this" → Claude reads refactoring SKILL.md → applies workflow
```

---

## 4. Building New Skills

```
Template for any new SKILL.md:

---
# [Skill Name]

## When to Invoke
[Trigger conditions — what task types activate this skill]

## Workflow
[Numbered steps — the process Claude follows]

## Quality Standards
[What defines success for this skill]

## Constraints
[What must NOT happen]

## Output Format
[Exact structure of the output]
---

Principles for good skills:
  1. Triggers are specific (not "when coding")
  2. Workflow is numbered and concrete
  3. Quality standards are measurable
  4. Output format eliminates ambiguity
  5. Under 300 words total (quality > quantity)
```

---

## 5. Revision Checklist

- [ ] Has all 4 core SKILL.md files in `.claude/skills/`
- [ ] CLAUDE.md references the skills to enable automatic invocation
- [ ] Testing skill produces complete test files with gap analysis
- [ ] Refactoring skill enforces: existing tests pass, no signature changes
- [ ] Documentation skill enforces: copy-paste runnable commands
- [ ] Performance skill produces prioritized table + fix for top finding
- [ ] Can write a new SKILL.md following the template
