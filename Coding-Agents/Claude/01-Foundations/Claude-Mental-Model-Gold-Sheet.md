# Claude Mental Model — Gold Sheet

> **Track**: Claude Mastery Track — Group 1: Foundations
> **File**: 1 of 6 (Track File #1)
> **Audience**: Developers starting their Claude journey
> **Read after**: Nothing — start here

---

## 1. Practical Impact Meter

| Topic | Impact | Why Developers Miss This |
|---|---|---|
| Claude vs ChatGPT vs Copilot — the real differences | ★★★★★ | Devs use Claude like ChatGPT and miss its biggest strengths |
| Claude Chat vs Claude Code — completely different tools | ★★★★★ | Claude Chat is conversational; Claude Code is autonomous — different mental model |
| Why Claude excels at long context + reasoning | ★★★★★ | Devs don't exploit the 200k token window — Claude can hold your entire codebase |
| Context quality determines output quality | ★★★★★ | Vague prompts + no context = junior output from a senior-capable model |
| CLAUDE.md as persistent project memory | ★★★★★ | Without it, Claude forgets all project conventions every session |
| Why Claude prefers explicit instruction over inference | ★★★★☆ | Claude doesn't guess your intent — it does exactly what you say |

---

## 2. Claude vs ChatGPT vs Copilot — The Real Differences

### What Each Tool Is For

```
ChatGPT (OpenAI):
  - Conversational AI — broad knowledge, general purpose
  - Tool: chat interface, some integrations
  - Strength: wide knowledge, user-friendly
  - Claude use case: when you want a broad AI assistant for general tasks

GitHub Copilot:
  - Coding assistant embedded in your IDE
  - Tool: VS Code extension, JetBrains plugin
  - Strength: inline autocomplete, context-aware code suggestions
  - Claude use case: when you want real-time inline suggestions while typing

Claude (Anthropic):
  - Long-context, reasoning-first AI model
  - Tool: Claude.ai chat, Claude Code CLI, API
  - Strength: 200k token context window, nuanced reasoning, structured output,
              following complex multi-part instructions precisely
  - Claude use case: complex multi-file codebase work, architectural decisions,
                     verification-driven autonomous workflows, nuanced analysis
```

### What Claude Does Better Than the Others

```
1. Long context (200k tokens):
   Claude can read your entire codebase — not just a few files.
   GPT-4 has 128k tokens. Most Copilot context is ~8-32k tokens per prompt.
   Claude can hold: full project codebase + tests + docs + architecture notes.

2. Precise instruction following:
   Claude treats instructions as contracts.
   If you say "do not add abstractions" — Claude will not add abstractions.
   ChatGPT may add them "helpfully". Copilot may suggest them inline.

3. Reasoning transparency:
   Claude explains WHY it made each decision.
   This makes review easier and helps you catch wrong assumptions.

4. Structured output discipline:
   Ask Claude for "a table with 3 columns" and you get exactly that.
   Ask Claude for "unified diff only" and it won't add prose padding.

5. Autonomous agentic work:
   Claude Code can plan, execute, verify, and iterate without constant prompting.
   The verification loop (run tests → read output → fix → repeat) is native.
```

### When to Use Each Tool

```
Use Claude when:
  ✓ The task requires reading many files simultaneously
  ✓ You need precise multi-step instructions followed exactly
  ✓ You want autonomous code generation with verification
  ✓ You're doing architecture analysis across a large codebase
  ✓ You need nuanced reasoning about trade-offs
  ✓ You want consistent, structured output format every time

Use Copilot when:
  ✓ You're actively typing code and want inline completion
  ✓ Quick one-line suggestions while in your editor flow
  ✓ Integrated directly into VS Code or JetBrains

Use ChatGPT when:
  ✓ Quick general knowledge questions
  ✓ No IDE or CLI setup available
  ✓ Team members who don't code need to use AI
```

---

## 3. Claude Chat vs Claude Code — Two Different Tools

### Must Know

```
Claude.ai Chat:
  - Browser-based conversational interface
  - Does NOT have access to your filesystem or terminal
  - You paste/upload code — Claude reads and responds
  - No autonomous file editing
  - Best for: learning, planning, short-form Q&A, one-shot code generation

Claude Code (CLI/IDE):
  - Command-line tool installed on your machine
  - HAS access to your filesystem (reads files, edits files)
  - HAS terminal access (runs commands, runs tests)
  - Autonomous: can plan → implement → verify → iterate
  - Best for: multi-file editing, autonomous feature building, agentic workflows

The mental model shift:
  Chat = you bring code to Claude
  Code = Claude comes to your code

Everything in this track after Section 1 assumes Claude Code unless specified.
```

### Claude Code Autonomy Levels

```
Level 1 — Assisted (you drive):
  You tell Claude exactly what to do.
  Claude makes the change. You review.
  
Level 2 — Collaborative (50/50):
  You describe the goal.
  Claude proposes a plan. You approve.
  Claude implements. You review.

Level 3 — Supervised autonomous (Claude drives, you watch):
  You set the goal and constraints.
  Claude plans → executes → runs tests → fixes failures → iterates.
  You review the final state.

Level 4 — Fully autonomous (Claude drives, you check at end):
  You set the goal, guardrails, and stopping conditions.
  Claude runs the entire workflow without interruption.
  You review commits and test results.

Start at Level 2. Move to Level 3 as trust in your configuration builds.
Level 4 only for well-tested workflows with strong hooks and verification.
```

---

## 4. How Claude Processes Context

### The 200k Token Window

```
200,000 tokens ≈ 150,000 words ≈ 300-500 pages of text

What you can fit in one Claude context window:
  - A full 10,000-line Python codebase (with tests)
  - An entire React application with components
  - All your architecture docs + ADRs + README
  - A full conversation history of a project sprint

This is NOT infinite. It's a limit, not a guarantee.
Best practice: provide relevant context, not maximum context.
Quality of context matters more than quantity.
```

### What Claude Sees in Claude Code

```
When you run Claude Code in a project directory, Claude automatically sees:
  ✓ CLAUDE.md at the project root
  ✓ CLAUDE.md files in subdirectories (for override context)
  ✓ Files you explicitly reference (@file:path or paste)
  ✓ Files Claude reads when executing its task
  ✓ Git history it queries (commits, diffs)
  ✓ Command output it runs (tests, builds, lint)
  ✓ Conversation history in the current session

What Claude does NOT automatically see:
  ✗ Your entire project (it reads what it needs or what you reference)
  ✗ Previous Claude sessions (no persistent memory beyond CLAUDE.md)
  ✗ Your environment variables (do NOT paste .env content)
  ✗ Other open windows or browser tabs
  ✗ Your mental model — only what you write down
```

### Context Quality vs Quantity

```
Bad context pattern (quantity over quality):
  "Here's my entire 5,000-line codebase. What's wrong with the payment function?"
  → Claude reads 5,000 lines but focuses on payment function anyway.
  → Wasted context, slower response, potential truncation.

Good context pattern (precise targeting):
  "Fix the bug in the payment processing function.
  @file:src/services/payment_service.py — focus on process_refund().
  Error: [3-line stack trace]"
  → Claude reads exactly what it needs.
  → Faster, more focused, better answer.

Rule: Context window is like RAM — use what you need, not everything you have.
```

---

## 5. Why Claude Follows Instructions Literally

### Must Know

```
Claude treats your instructions as contracts.

Example:
  You say: "Refactor this function. Keep the public API identical."
  Claude will NOT add parameters. It will NOT rename the function.
  
  You say: "Refactor this function to be cleaner."
  Claude may add parameters, rename things, add comments — it fills in the gaps.

The gap-filling problem:
  When instructions are vague, Claude infers intent.
  Claude's inference is often wrong in ways that seem right (compiles, looks clean).
  
  Bad: "Make this function better"
  Claude adds: abstract base class, factory pattern, 3 helper methods
  You wanted: extract a 5-line helper

  Good: "Extract the email validation logic into a separate validate_email() function.
  Keep process_user() signature identical. Do not add new classes."
  Result: exactly what you asked for.

Corollary: What you don't say is as important as what you do say.
"Do NOT" instructions are first-class. Use them.
```

### The Explicit > Implicit Rule

```
Every assumption Claude can make without instruction is a potential wrong decision.

Write instructions so explicit that:
  A stranger who knows nothing about your project would make the correct change.

Include in every significant instruction:
  1. What to do (specific, not vague)
  2. What pattern to follow (reference an existing file)
  3. What to preserve (keep public API, keep tests passing)
  4. What not to do (no new dependencies, no new abstractions)
  5. What the output should look like (diff, code block, table)
```

---

## 6. CLAUDE.md — Your Persistent Memory

### Why It Exists

```
Problem: Claude has no memory between sessions.
Every new Claude Code session, Claude doesn't know:
  - What you've already implemented
  - Your architectural decisions
  - Your team's coding conventions
  - Which libraries you use vs avoid
  - What patterns exist in your codebase

Solution: CLAUDE.md
  Claude reads CLAUDE.md at the start of every session.
  It is your "always-loaded" project brief.
  
  Without CLAUDE.md:
    You repeat "we use pytest, not unittest" in every session.
    Claude suggests sync DB drivers in your async codebase.
    Claude adds global state you explicitly said to avoid.
  
  With CLAUDE.md:
    Claude knows your stack, patterns, and forbidden antipatterns.
    Conventions are applied consistently every session.
    You never repeat project context in prompts.
```

---

## 7. Revision Checklist

- [ ] Can explain Claude vs ChatGPT vs Copilot in one sentence each
- [ ] Understands Claude Chat (conversational) vs Claude Code (autonomous filesystem access)
- [ ] Knows what the 200k token window means practically
- [ ] Understands why context quality matters more than quantity
- [ ] Knows why Claude follows instructions literally (no gap-filling inference)
- [ ] Understands why CLAUDE.md is essential (persistent project memory)
- [ ] Can choose the right tool (Claude vs Copilot vs ChatGPT) for a given task
