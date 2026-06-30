# Codex Mental Model — Gold Sheet

> **Track**: Codex Mastery Track — Group 1: Foundations
> **File**: 1 of 6 (Track File #1)
> **Audience**: Developers starting their Codex journey
> **Read after**: Nothing — start here

---

## Practical Impact Meter

| Topic | Impact | Why Developers Miss This |
|---|---|---|
| Codex CLI vs GitHub Copilot — completely different products | ★★★★★ | Devs assume Codex = the model that powers Copilot. The CLI is a different tool. |
| Agent mindset vs autocomplete mindset | ★★★★★ | Codex is an autonomous agent that executes tasks; Copilot suggests inline completions |
| Approval policy as a risk dial | ★★★★★ | Using full-auto by default without understanding the risk spectrum causes accidents |
| AGENTS.md as persistent project context | ★★★★★ | Without it, Codex applies generic defaults — not your team's conventions |
| Verification commands as the "done" signal | ★★★★☆ | Without them, Codex stops when it thinks it's done — not when it actually is |
| Codex vs Claude Code — same concept, different ecosystem | ★★★☆☆ | Both are CLI agents; choosing between them is an ecosystem decision, not a capability gap |

---

## ⭐ Beginner Tier — Start Here

### B1: Your first Codex interaction

Before touching any config, do this:
```bash
# In any project directory with some code
codex "Explain what this project does based on the files here. Do not make any changes."
```
What to observe:
- Codex reads your files automatically
- It gives a structured summary
- Nothing was changed — you just used Codex as a smart code reader

This is `suggest` mode (the default). Zero risk.

### B2: The difference between Codex and Copilot

```
GitHub Copilot:
  - Lives inside your editor (VS Code, JetBrains)
  - Suggests code as you type (autocomplete)
  - Context: usually the open file and a few nearby files
  - You stay in control at every keystroke
  - Strength: frictionless inline suggestions

OpenAI Codex CLI:
  - Lives in your terminal
  - Executes tasks: reads files, writes files, runs commands
  - Context: your entire repo + AGENTS.md instructions
  - Operates autonomously (with your approval level setting)
  - Strength: multi-step task execution, test iteration, codebase-wide changes
```

The right question is not "which is better" — it's "which tool for which task."

---

## 1. The Three Claude/Codex/Copilot Comparison

```
GitHub Copilot:
  What it is: IDE autocomplete assistant
  How you interact: inline, as you type
  Autonomy: zero — you accept/reject each suggestion
  Best for: fast inline code generation while editing

OpenAI Codex CLI:
  What it is: Terminal-based autonomous coding agent
  How you interact: task descriptions in natural language
  Autonomy: configurable — suggest → auto-edit → full-auto
  Best for: multi-file tasks, test loops, scaffold, refactoring

Anthropic Claude Code CLI:
  What it is: Terminal-based autonomous coding agent (same category as Codex)
  How you interact: task descriptions, slash commands, subagents
  Autonomy: configurable — interactive → autonomous
  Best for: same category of tasks; ecosystem choice (OpenAI vs Anthropic)

ChatGPT / Claude.ai Chat:
  What it is: Conversational AI web interface
  How you interact: conversation — question, answer, follow-up
  Autonomy: zero — it suggests, you implement manually
  Best for: learning, architecture brainstorming, explaining concepts
```

---

## 2. The Agent Mindset — What Changes

```
Old mindset (autocomplete era):
  - AI suggests → you type it in → you move on
  - You are always the execution layer
  - AI is a typing accelerator

New mindset (agent era):
  - You define WHAT and WHY (the task + constraints + verification)
  - Codex figures out HOW (which files, what changes, what commands)
  - Codex executes and verifies
  - You review the output before accepting

What this means in practice:
  - Your most valuable skill: writing precise task descriptions and constraints
  - Not: writing code line by line
  - Your review discipline replaces your implementation time
```

---

## 3. How Codex Processes a Task

```
When you run: codex "add pagination to GET /users"

Step 1 — Context gathering:
  Codex reads:
    - AGENTS.md (project instructions)
    - Files in the working directory (scanned for relevance)
    - Any files you explicitly reference
    - Prior conversation in the current session

Step 2 — Planning:
  Codex proposes a plan: which files to modify, what changes to make, what command to run

Step 3 — Approval (based on policy):
  suggest: shows you every change before making it
  auto-edit: makes file changes directly, asks before running commands
  full-auto: executes everything automatically

Step 4 — Verification:
  If you provided a verification command: Codex runs it and iterates until it passes
  If not: Codex stops when its implementation is complete (may still have bugs)

Step 5 — Report:
  Summary of what was done, what passed, what (if anything) remains
```

---

## 4. The Four Codex Operating Modes

```
1. Interactive REPL (codex with no arguments):
   - Conversation-style session
   - You type tasks, Codex executes, you continue
   - Best for: exploratory sessions, back-and-forth refinement
   - Like having a developer next to you

2. Non-interactive single task (codex "task"):
   - One task, Codex completes it, returns control
   - Best for: scripted workflows, CI integration
   - Like giving a contractor a ticket

3. System-prompted task (codex --system-prompt "..."):
   - Override the default system context for this run
   - Best for: specialized roles (security reviewer, documentation writer)

4. Full-auto bounded task (codex --approval-policy full-auto "bounded task"):
   - Codex operates completely autonomously
   - Best for: well-defined scaffold or refactor tasks with clean checkpoints
   - Requires: git checkpoint before, full diff review after
```

---

## 5. What Codex Excels At

```
★★★★★ Most effective:
  - Multi-file implementation from a clear spec
  - Test generation with iteration until all tests pass
  - Codebase-wide refactoring (rename, restructure, apply patterns)
  - Debugging with a reproduction command ("run this test until it passes")
  - Scaffold tasks (new endpoint, new service, new module from pattern)

★★★☆☆ Effective with good prompting:
  - Architecture proposals (verify against your actual constraints)
  - Code review (security + logic, not stylistic judgment)
  - Documentation (good output; needs manual verification)

★☆☆☆☆ Use with caution:
  - Business logic decisions (Codex doesn't know your domain)
  - Database migrations (high risk of data loss — always human-in-loop)
  - Infrastructure changes (always human-in-loop)
  - Security-sensitive crypto/auth (generate + dedicated human security review)
```

---

## 6. What Codex Is Not

```
❌ Codex is not infallible — it produces plausible-wrong output for complex problems
❌ Codex does not know your production system — it knows what's in its context window
❌ Codex is not a replacement for design review — it implements, it doesn't decide
❌ Codex is not always the fastest approach — small changes are faster manually
❌ Codex does not catch its own security vulnerabilities — you must review auth/SQL code
```

---

## Interview Traps

```
TRAP: "Codex is just GitHub Copilot with a different UI"
TRUTH: Completely different architectures. Copilot is IDE autocomplete. Codex CLI is an
       autonomous agent that executes tasks, runs commands, and iterates on tests.

TRAP: "Full-auto mode is safe because Codex is smart"
TRUTH: Full-auto is powerful but requires scope control. Without a bounded task description
       and git checkpoint, one session can modify dozens of files in unexpected ways.

TRAP: "I don't need AGENTS.md for small projects"
TRUTH: AGENTS.md isn't about project size. It's about getting Codex to follow YOUR
       conventions — naming, error handling, test patterns. Without it, you get generic output.
```

---

## Revision Checklist

- [ ] Can explain the difference between Codex CLI, GitHub Copilot, and Claude Code
- [ ] Can describe the 4 Codex operating modes and when to use each
- [ ] Can describe what AGENTS.md does and why it's required
- [ ] Can explain the 3 approval policies and which to use for which risk level
- [ ] Can articulate what Codex excels at vs what needs human judgment
