# Context Engineering — Gold Sheet

> **Track**: Claude Mastery Track — Group 2: Intermediate Power User
> **File**: 3 of 7 (Track File #9)
> **Audience**: Developers mastering Claude's 200k context window
> **Read after**: Slash-Commands-Gold-Sheet.md

---

## 1. Practical Impact Meter

| Topic | Impact | Why Developers Miss This |
|---|---|---|
| 200k token window — what fits and what doesn't | ★★★★★ | Devs either under-use it (paste only 20 lines) or over-use it (dump entire codebase) |
| What belongs in context vs prompt vs CLAUDE.md | ★★★★★ | Wrong placement means repeated work or ignored instructions |
| Context drift — how long sessions degrade | ★★★★★ | After 50+ exchanges, Claude starts contradicting its earlier decisions |
| Session start patterns — priming Claude correctly | ★★★★★ | The first 200 words of a session shape everything that follows |
| Summarization patterns — compressing long sessions | ★★★★☆ | Prevents context bloat without losing key decisions |
| Chunking — splitting tasks across sessions | ★★★★★ | Complex work done in one session degrades; chunked work stays sharp |

---

## 2. The 200k Token Window — Practical Understanding

### What 200k Tokens Can Hold

```
200,000 tokens ≈ 150,000 words ≈ 300-500 pages

What this means for code:
  - A 10,000-line Python codebase: fits easily
  - A 50,000-line monorepo: selective file reading required
  - All tests + docs + architecture notes for a medium project: fits
  - Your entire conversation history: accumulates fast

Token cost approximations:
  Average Python function (20 lines):    ~100 tokens
  Average service file (200 lines):    ~1,000 tokens
  Average test file (100 lines):         ~500 tokens
  CLAUDE.md (200 words):                 ~300 tokens
  One back-and-forth exchange:           ~300-800 tokens

A 50-exchange conversation ≈ 20,000-40,000 tokens used just for history.
Long sessions mean less room for code context.
```

### What Goes Where

```
CLAUDE.md (persistent, all sessions):
  → Tech stack, architecture rules, Do NOT rules
  → Updated weekly, reviewed quarterly

Prompt (this request, this task):
  → Specific goal, constraints for THIS task
  → File references for THIS task
  → Output format for THIS output

Context window (this session):
  → Current conversation history
  → Files Claude has read this session
  → Command output Claude has seen

Rule: If you want it to apply every time → CLAUDE.md
      If you want it for this task only → prompt
      If it's what happened this session → context window
```

---

## 3. Context Drift — The Long Session Problem

### What Context Drift Is

```
Context drift: as a session gets long, Claude's behavior degrades.

Symptoms:
  - Claude contradicts decisions it made earlier
  - Claude forgets constraints it was given at the start
  - Claude produces more generic output as the session progresses
  - Claude repeats mistakes it already corrected

Why it happens:
  The context window has a fixed size.
  As new exchanges accumulate, older context gets pushed further back.
  Claude's attention to far-back context weakens.
  
  Analogy: like a developer in a meeting who stops being able to
  recall what was said in the first 10 minutes.

Signs you're in a drifted session:
  - Claude says "I don't recall mentioning X" (it did)
  - Claude ignores a constraint stated at the beginning
  - Claude's last 5 responses feel less targeted than the first 5
```

### Preventing Context Drift

```
Prevention 1 — Keep sessions short and focused:
  One session = one task.
  Don't combine "debug the auth issue" + "generate tests" + "plan the new feature"
  in one session. Split into three sessions.

Prevention 2 — Periodic context summarization:
  Every 20-30 exchanges, ask:
  "Summarize the decisions we've made so far in this session.
  What have we implemented? What are the open questions?
  Keep it under 200 words."
  → Save this summary.
  → Start a new session with the summary as the first message.

Prevention 3 — Critical constraints at the TOP:
  Start every session by restating the key constraints.
  "CRITICAL CONSTRAINTS FOR THIS SESSION:
  - Never modify test files
  - Only touch src/services/payment_service.py
  - Run pytest after every change
  
  Now: [your task]"

Prevention 4 — Resume pattern between sessions:
  "Resume context: I was implementing [feature].
  Decisions made: [bullet list of key decisions]
  Files created/modified: [list]
  Current state: [where we are]
  Next: [what to do now]"
```

---

## 4. Session Start Patterns

### The High-Quality Session Opener

```
A well-structured session opener sets the tone for everything.

Structure:
1. Resume context (if continuing)
2. Task for THIS session only
3. Files in scope
4. Files out of scope
5. Success criteria

Example:
"Session context:
  Project: Order Processing Service
  CLAUDE.md is loaded with our stack and conventions.

Task for this session (ONLY this task):
  Implement order refund endpoint and service method.
  
Files to read (pattern reference):
  @file:src/api/orders.py      ← router pattern to follow
  @file:src/services/order_service.py ← service pattern

Files to create:
  src/api/refunds.py   ← new router
  src/services/refund_service.py ← new service

Do NOT touch:
  Any existing files
  Any test files (generate tests in next session)
  Database migration (flag it, don't run it)

Success criteria:
  POST /refunds creates a refund record
  All existing tests still pass"
```

---

## 5. Summarization Patterns

### Pattern 1 — Mid-Session Summarize and Compress

```
Use when: session is getting long (30+ exchanges)

"Pause. Summarize this session:
1. What have we implemented? (bullet list, 1 line per item)
2. What decisions did we make? (key architectural/technical choices)
3. What still needs to be done? (ordered list)
4. What constraints are in effect? (active rules)
Under 200 words.
I'll use this summary to start a fresh session."
```

### Pattern 2 — Cross-Session Handoff

```
Save the summary → start new session with:

"New session. Context from previous session:
[paste the 200-word summary]

Current state: [one sentence]
This session task: [specific next step]
Constraints: [any critical rules not in CLAUDE.md]
Go."
```

---

## 6. Chunking — Multi-Session Strategy

```
Large tasks degrade in one session.
Chunking into multiple sessions maintains quality.

Feature development chunking:
  Session 1: Plan and design (output: list of files and their purposes)
  Session 2: Create data models/schemas
  Session 3: Implement repository layer (DB access)
  Session 4: Implement service layer (business logic)
  Session 5: Implement API layer (routes and request/response)
  Session 6: Generate tests
  Session 7: Fix test failures and refine

Each session:
  - Has ONE clear goal
  - Starts with a context summary
  - Ends with a commit
  - Produces a summary for the next session

Antipattern (single session for everything):
  "Build the complete order management system"
  → Claude makes 20 assumptions, drifts by session end, produces inconsistent code
```

---

## 7. Context Variables and References

```
In Claude Code, reference files precisely:

@file:path/to/file.py           ← specific file
@file:src/services/             ← directory (Claude reads selectively)
#codebase                       ← semantic search (like Copilot's #codebase)

Best practices:
  - Use @file: for specific known files (most precise)
  - Use directory reference for "look at this area" tasks
  - Avoid "read everything" unless codebase is small

Comparison:
  Good: "@file:src/repositories/user_repo.py — use as pattern"
  Bad:  "Read all the repository files to understand our patterns"
  
  Good targets 1 file (400 tokens)
  Bad triggers reading 15 files (6,000 tokens) of which 13 add noise
```

---

## 8. Revision Checklist

- [ ] Knows what 200k tokens means in practical file terms
- [ ] Knows the three locations: CLAUDE.md vs prompt vs context window
- [ ] Can detect context drift symptoms
- [ ] Uses the mid-session summarize-and-compress pattern for long sessions
- [ ] Uses the resume pattern for multi-session tasks
- [ ] Chunks complex features across multiple focused sessions
- [ ] Uses @file: references for precise context targeting
- [ ] Starts sessions with the high-quality session opener pattern
