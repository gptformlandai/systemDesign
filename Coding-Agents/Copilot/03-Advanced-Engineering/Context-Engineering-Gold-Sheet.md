# Context Engineering — Gold Sheet

> **Track**: Copilot Mastery Track — Group 3: Advanced Engineering
> **File**: 3 of 7 (Track File #16)
> **Audience**: Developers who want precise control over what Copilot sees
> **Read after**: AGENTS-MD-Strategy-Gold-Sheet.md

---

## 1. Practical Impact Meter

| Topic | Impact | Why Developers Miss This |
|---|---|---|
| What exactly Copilot sees — the full picture | ★★★★★ | Devs assume Copilot sees everything; it sees only what you give it |
| Context window mechanics — size and limits | ★★★★★ | Long conversations or large files silently degrade response quality |
| File selection strategy — minimum viable context | ★★★★★ | Opening 20 files when 3 are relevant wastes context and produces noise |
| Context variables — deep dive beyond basics | ★★★★☆ | `#sym`, `#editor`, `#problems` are underused but very powerful |
| Project context file — the persistent summary | ★★★★☆ | A well-maintained project summary doc gives Copilot project-level memory |
| Summarize before asking — the compress pattern | ★★★★☆ | Long codebases need summarization before implementation questions |
| Context for debugging vs context for generation | ★★★★☆ | These need different context strategies |

---

## 2. What Copilot Sees — The Complete Picture

### Context Sources by Mode

```
Inline Suggestions:
  ✓ File content above cursor (primary — most tokens allocated here)
  ✓ File content below cursor (supporting context)
  ✓ Other open tabs (supporting, lower weight)
  ✓ File imports (high weight — sets language/library context)
  ✗ Chat history
  ✗ copilot-instructions.md
  ✗ Closed files

Chat Ask / Edits / Agent Mode:
  ✓ Your current message (highest priority)
  ✓ Conversation history in current session
  ✓ Files referenced with #file, #selection, #codebase, #sym, etc.
  ✓ copilot-instructions.md (root-level)
  ✓ Matching path-specific instructions
  ✓ Workspace index (for #codebase)
  ✓ MCP tool outputs (if configured)
  ✗ Previous chat sessions (no persistent memory)
  ✗ Files you haven't opened or referenced
  ✗ Your mental model (only what you write down)
```

---

## 3. Context Window — What Happens When It's Full

```
Every model has a context window — the maximum amount of text it can process.
When context exceeds the limit:
  - Oldest messages in the conversation are dropped (forgotten)
  - Large files may be truncated
  - Instructions from copilot-instructions.md may be compressed or omitted
  - Output quality degrades — Copilot starts being generic

Signs you've exceeded the context window:
  - Copilot ignores rules it was following earlier in the conversation
  - Copilot asks you to restate things it already knows
  - Suggestions stop referencing your specific code and become generic
  - In Agent Mode: Copilot forgets the architectural decisions from earlier steps

Solutions:
  1. Start a new chat conversation (Cmd+L) — fresh context
  2. Summarize prior context before starting fresh ("Here's what we've done so far...")
  3. Use smaller, scoped conversations instead of one long session
  4. Use copilot-instructions.md for recurring context that must always be present
  5. Use a project context file that you paste at the start of each relevant session
```

---

## 4. File Selection Strategy

### The Minimum Viable Context Rule

```
Only include files that are DIRECTLY relevant to the current task.

Task: "Add email validation to the create_user endpoint"
Minimum viable context:
  #file:src/api/users.py          ← the endpoint being changed
  #file:src/schemas/user.py       ← Pydantic schema that may need a validator
  
NOT needed (adds noise):
  All of src/ (too broad)
  tests/ (not changing tests right now)
  src/services/ (not modifying the service layer for this change)
  .github/workflows/ (completely irrelevant)

Rule: Every file you add to context should directly influence the answer.
If you remove the file and the answer would be the same → it's not needed.
```

### Context for Different Task Types

```
Understanding existing code:
  ✓ The specific file/function you want to understand
  ✓ Its direct dependencies (one level up or down)
  ✗ Unrelated files in the same directory

Generating new code:
  ✓ The file where new code will go
  ✓ One example file that shows the pattern to follow
  ✓ The interface/schema the new code must conform to
  ✗ All files in the module

Debugging an error:
  ✓ The stack trace or error message (paste it)
  ✓ The file and function where the error originates
  ✓ The calling code that triggers the error
  ✗ Unrelated services that are not in the call chain

Architecture analysis:
  ✓ #codebase (uses the index for broad search)
  ✓ Key architectural files (entry points, config, base classes)
  ✗ Individual implementation files (noise at this level)
```

---

## 5. Context Variables — Advanced Usage

### `#problems` — Reference Editor Errors

```
Attaches the current list of errors/warnings from the Problems panel.

Usage: type #problems in Chat
Example: "Fix the TypeScript errors in #problems that relate to the user module"

Best for:
  - Batch fixing type errors after a refactoring
  - Understanding why a set of imports is failing
  - Getting a fix plan for all lint warnings before a PR
```

### `#terminalSelection` — Reference Selected Terminal Output

```
Usage: select text in the terminal → #terminalSelection in Chat
Example: Select a specific failed test output → "Explain why this test is failing: #terminalSelection"

Different from #terminalLastCommand (full last command output).
Use #terminalSelection when only part of the terminal output is relevant.
```

### `#sym` — Symbol Deep Dive

```
Usage: #sym:<SymbolName> in Chat

More powerful than just referencing a file because:
  - Goes directly to the symbol definition
  - Includes callers and usages when you ask for them
  - Works across files — finds the symbol wherever it is defined

Example prompts:
  "Explain how #sym:AsyncSession is used throughout this codebase"
  "What are all the places where #sym:validate_email is called?"
  "Generate a test for #sym:UserService.create_user"
```

---

## 6. The Project Context File

### What It Is and Why It Matters

```
Copilot has no persistent memory across sessions.
A project context file is a document you maintain that gives Copilot
instant project-level context at the start of any session.

Location: .copilot-context.md (in repo root, NOT committed if it contains sensitive decisions)
Or: docs/copilot-project-context.md (committed — sharable with team)

Structure:
  ## What This Project Does
  [2-3 sentences]
  
  ## Architecture Summary
  [Key patterns, layers, naming conventions]
  
  ## Critical Current State
  [What phase of development, current sprint focus, known constraints]
  
  ## Do NOT Do
  [Architectural decisions already made that Copilot should not try to reverse]
  
  ## Context for this Session
  [Updated per session — what specific problem you're working on]
```

### Maintaining the Context File

```
Update this file:
  - At the start of each week (or sprint)
  - When a major architectural decision is made
  - When a new library is adopted
  - When a domain concept is finalized

Use it:
  At the start of any Chat session where project knowledge matters:
  "Context: #file:.copilot-context.md — [then your actual question]"

This gives Copilot ~200 words of high-density project knowledge without
bloating the context window with 20 open files.
```

---

## 7. The Summarize-Before-Ask Pattern

```
For large or complex codebases, ask Copilot to summarize before implementing.

Pattern:
  Step 1: "Analyze #codebase and summarize:
           - The overall architecture (layers, patterns)
           - How the authentication flow works
           - The database access patterns used
           Do not make any changes — analysis only."
  
  Step 2: Review the summary for accuracy.
  
  Step 3: Use the summary as context for the implementation question:
           "Based on the architecture you just described, how should I implement
           [feature] to fit the existing patterns?"

Why this works:
  The summary is compact (300-500 words) vs the raw codebase (potentially 100k tokens).
  Copilot answers from the summary, which is more accurate than trying to reason
  across a noisy, partially-loaded codebase index.
```

---

## 8. Anti-Patterns — Context Mistakes to Avoid

```
Anti-pattern 1 — "Open everything" approach:
  Adding 30 files to a working set for a one-function change.
  Fix: Open only the 2-3 files directly affected.

Anti-pattern 2 — Long single conversation:
  One Chat session for 3 different feature tasks across 2 hours.
  Fix: Start a new conversation (Cmd+L) per task or per topic change.

Anti-pattern 3 — Repeating the same context setup every session:
  "I'm working on a FastAPI app with PostgreSQL and asyncpg and..."
  Fix: Put this in copilot-instructions.md — it loads automatically.

Anti-pattern 4 — Describing context instead of attaching it:
  "The UserService class has a create_user method that..."
  Fix: Use #file:src/services/user_service.py — let Copilot read it directly.

Anti-pattern 5 — Using #codebase for precise questions:
  "Does #codebase have a create_user function that returns None on failure?"
  Fix: Use #sym:create_user or #file:user_service.py — precise, not a codebase scan.
```

---

## 9. Revision Checklist

- [ ] Can describe all context sources for inline, Chat, and Agent Mode
- [ ] Knows the signs of context window exhaustion
- [ ] Applies the minimum viable context rule for every prompt
- [ ] Knows which context variables to use for which task types
- [ ] Has created a project context file for their main project
- [ ] Uses the summarize-before-ask pattern for large codebases
- [ ] Can identify and correct the 5 context anti-patterns
