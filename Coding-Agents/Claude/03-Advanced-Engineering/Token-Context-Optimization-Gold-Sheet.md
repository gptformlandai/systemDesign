# Token & Context Optimization — Gold Sheet

> **Track**: Claude Mastery Track — Group 3: Advanced Engineering
> **File**: 4 of 7 (Track File #18)
> **Read after**: MCP-Integration-Gold-Sheet.md

---

## Practical Impact Meter

| Topic | Impact | Why Developers Miss This |
|-------|--------|--------------------------|
| Model selection (Haiku vs Sonnet vs Opus) | ★★★★★ | Using Opus for simple tasks wastes 5-10x cost and slows responses |
| Context scoping — relevant files only | ★★★★★ | Loading the whole codebase bloats context and degrades output quality |
| High-signal vs low-signal prompts | ★★★★★ | Verbose prompts don't produce better output — they produce worse |
| Session management — when to start fresh | ★★★★☆ | Long sessions cause context drift — Claude forgets earlier decisions |
| Chunking large tasks | ★★★★☆ | 200k context ≠ unlimited — large codebases hit the limit fast |
| Output format control for token efficiency | ★★★☆☆ | Markdown-heavy responses waste tokens when plain text is sufficient |

---

## 1. Model Selection

### Claude Model Tiers

```
Claude Haiku 3.5:
  Speed: fastest
  Cost: lowest (~20x cheaper than Opus)
  Best for: simple generation, code completion, short explanations, formatting tasks
  Examples: generate a docstring, format a JSON file, add type hints, fix a lint error

Claude Sonnet 4 / 4.5:
  Speed: fast
  Cost: moderate (~4x cheaper than Opus)
  Best for: most development tasks — the daily workhorse
  Examples: implement a function, debug an error, generate tests, explain code

Claude Opus 4:
  Speed: slower
  Cost: highest
  Best for: complex architecture reasoning, multi-step planning, ambiguous problem diagnosis
  Examples: design a system from scratch, diagnose a complex multi-layer bug,
            write an ADR for a major technical decision

Rule: default to Sonnet. Drop to Haiku for simple tasks. Escalate to Opus for
      architecture decisions or problems Sonnet can't solve after 2 tries.
```

### CLI Model Selection

```
claude --model claude-haiku-3-5  "Add a docstring to this function: [paste]"
claude --model claude-sonnet-4   "Implement the UserService.update method"
claude --model claude-opus-4     "Design the event sourcing architecture for this system"
```

---

## 2. Context Scoping

### The Context Window Is Finite

```
Claude's context window: ~200,000 tokens
  = ~150,000 words = ~500 typical source files

Why you shouldn't use all of it:
  1. Quality degrades as context grows: Claude's attention distributes across ALL tokens.
     Relevant signal is diluted by irrelevant context.
  2. Speed decreases: Claude reads the entire context on every request.
  3. Cost increases: you pay per input token.

The principle: Minimum viable context.
  Load the files Claude NEEDS for the task. Remove everything else.
```

### Scoping Rules

```
For a focused implementation task:
  INCLUDE: the file to edit + 1-2 directly related files (schema, related service)
  EXCLUDE: all test files, unrelated services, config files, README

For a debugging task:
  INCLUDE: the file with the bug + the error trace
  EXCLUDE: everything that isn't in the stack trace

For a codebase overview:
  INCLUDE: @codebase (let Claude index)
  LIMIT: ask for high-level architecture first; drill into specific files as needed

For a multi-file refactoring:
  INCLUDE: only the files being changed + direct dependencies
  EXCLUDE: files that import the changed code (add them only if needed)
```

### What @codebase Does

```
@codebase tells Claude to index your entire workspace.
It reads all non-ignored files and builds a semantic index.

When to use @codebase:
  - "Find all places in @codebase that use the deprecated get_user_v1() function"
  - "What follows the repository pattern in @codebase? Show examples."
  - Codebase exploration prompts (architecture overview, pattern detection)

When NOT to use @codebase:
  - When you know exactly which files are relevant (use @file instead)
  - For implementation tasks on known files (adds noise)
  - When the codebase is large and the task is narrow
```

---

## 3. High-Signal Prompts

### The Signal/Noise Principle

```
More words ≠ better output.
The ratio of useful information to total prompt length is what matters.

LOW SIGNAL (verbose, unclear):
  "I've been working on this user service and I noticed that when I try to
  create a user with an email that already exists in the database it doesn't
  give back a proper error message and I wanted to see if you could help me
  fix this issue so users get a clear error"

HIGH SIGNAL (concise, precise):
  "UserService.create_user doesn't raise an error when email already exists.
  @file:src/services/user_service.py
  Expected: raise EmailAlreadyExistsError(email)
  Actual: silently overwrites or returns None
  Fix: add duplicate check before insert"

Same task. The high-signal version will produce a better answer in fewer tokens.
```

### High-Signal Patterns

```
Pattern 1 — Lead with the action:
  BAD:  "I'm trying to figure out if there's a way to..."
  GOOD: "Implement X. Constraint: Y."

Pattern 2 — Use references, not descriptions:
  BAD:  "The user service file that handles creating users..."
  GOOD: "@file:src/services/user_service.py"

Pattern 3 — State expected vs actual for bugs:
  BAD:  "Something is wrong with the login"
  GOOD: "Expected: 200 OK. Actual: 403 Forbidden. Auth middleware: @file..."

Pattern 4 — Specify output format when you need scannable output:
  BAD:  (no format spec) → Claude writes 5 paragraphs
  GOOD: "Output: bullet list only" → Claude writes bullets

Pattern 5 — State what NOT to do:
  BAD:  (no constraint) → Claude adds 3 new classes and a factory
  GOOD: "Do NOT add new classes. Modify the existing UserService only."
```

---

## 4. Session Management

### Context Drift

```
Context drift: in a long session, Claude's "attention" to early context weakens.
  - Claude may contradict decisions made 30+ exchanges ago
  - Rules stated at the start may be "forgotten" late in a long session
  - Earlier code examples lose influence on later generation

Signs of context drift:
  - Claude uses a different naming convention than it used earlier
  - Claude contradicts a constraint you stated at session start
  - Claude says "I don't recall" something clearly discussed earlier

Fix: Start a new session. Carry the important context forward explicitly.
```

### Session Priming for New Sessions

```
When starting a fresh session after a complex task:

"Continuing from the last session. Here's the context:

Task: [1-sentence description]
Decisions made: [bullet list of key choices]
Current state: [what's been done]
Next step: [what we're doing now]
Constraints still active: [rules that apply to this session too]

[link or paste the relevant files]"
```

### When to Start a New Session

```
Start a new session when:
  - The current session has gone > 40 exchanges
  - Claude starts contradicting earlier decisions
  - The current task is clearly separate from what came before
  - You're shifting from planning to implementation
  - You're shifting from implementation to testing

Keep the session going when:
  - You're in the middle of an iterative loop (test → fix → test)
  - Claude is maintaining important state (half-built feature context)
  - Breaking the session would lose hard-won context
```

---

## 5. Chunking Large Tasks

### Why Chunking Matters

```
A feature with 10 files, 500 lines each = ~5,000 lines = ~7,500 tokens (code).
Plus prompts, explanations, diffs = easily 50,000+ tokens in one session.
A 200k session can still run out if Claude is generating and revising extensively.

More importantly: Claude's reasoning quality degrades on very large tasks.
Better approach: decompose into subtasks with checkpoints.
```

### The Chunking Pattern

```
Phase 1 — Plan (separate session):
  Define: components, interfaces, file list, implementation order
  Output: a written plan document you can paste into later sessions

Phase 2 — Implement component by component (one session per component):
  Session A: schema + model
  Session B: repository/data layer
  Session C: service/business logic
  Session D: API/router

Phase 3 — Integration + tests (one session):
  Run full test suite, fix integration failures, verify end-to-end

Phase 4 — Review (separate session):
  Security review, performance review, documentation

This produces better output than one monolithic "build the whole feature" prompt.
```

---

## 6. Output Format Control

```
Control Claude's output format to avoid token waste:

For code output only (no explanation):
  "Output: code only. No explanation."

For a table (not prose):
  "Output: markdown table with columns: [col1, col2, col3]. No prose."

For bullet points (not paragraphs):
  "Format: bullet list. Max 2 lines per bullet."

For a specific file format:
  "Output: complete file contents only. Start with the import block."

For short answers:
  "Answer in under 100 words."

Context: Claude defaults to extensive explanation. Explicit format control
produces the output you actually need without the surrounding narrative.
```

---

## 7. Revision Checklist

- [ ] Knows which model to use for each task type (Haiku / Sonnet / Opus)
- [ ] Applies minimum viable context — uses @file not @codebase for narrow tasks
- [ ] Writes high-signal prompts: action first, references not descriptions, constraints explicit
- [ ] Knows the signs of context drift and when to start a new session
- [ ] Uses session priming when restarting to preserve important context
- [ ] Chunks large tasks across multiple sessions by component
- [ ] Controls output format to avoid verbose explanations when code is all that's needed
