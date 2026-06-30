# Token & Context Optimization — Gold Sheet

> **Track**: Codex Mastery Track — Group 3: Advanced Engineering
> **File**: 4 of 7 (Track File #17)
> **Audience**: Developers who want to get more done per session without running out of context
> **Read after**: Agent-Loops-Gold-Sheet.md

---

## Practical Impact Meter

| Topic | Impact | Why Developers Miss This |
|---|---|---|
| /compact — when and how to use | ★★★★★ | Most users never use /compact and hit the context wall unnecessarily |
| Session startup priming — load only what matters | ★★★★☆ | Wasting context on irrelevant files = fewer tokens for actual work |
| Model selection by task complexity | ★★★★☆ | Using gpt-4.1 for doc tasks wastes cost + latency vs o4-mini |
| High-signal vs low-signal prompts | ★★★★☆ | Vague prompts use tokens to "understand" instead of "do" |
| Task batching — get more done per session | ★★★☆☆ | Separate sessions for related tasks = duplicate context loading |

---

## ⭐ Beginner Tier — Start Here

### B1: See /compact in action

```bash
# Start a session and complete one task
codex "explain how GET /users pagination works in src/api/users.py"

# Now run /compact before starting the next task
/compact

# Observe: the session summary is shorter than the full conversation
# The key context is preserved; the verbose back-and-forth is compressed
# Now start the next task with the freed context space
"now add the same pagination pattern to GET /orders"
```

What to notice: output quality for the second task with /compact vs without (try both).

### B2: Model selection — pick the right one

```bash
# Three tasks, three models:

# Task: add docstrings to src/utils/helpers.py
# Model: gpt-4.1-mini (documentation only — cheapest that works)
codex --model gpt-4.1-mini "add Google-style docstrings to src/utils/helpers.py"

# Task: implement GET /orders/{id} following the users pattern  
# Model: o4-mini (default — handles standard implementation well)
codex "add GET /orders/{id} endpoint following GET /users/{id} pattern"

# Task: design the caching strategy for the entire user service
# Model: gpt-4.1 (complex reasoning, architecture)
codex --model gpt-4.1 "design the caching strategy for user service"
```

---

## 1. Context Window Mental Model

```
Codex's context window works like a whiteboard:
  - Limited space
  - Everything written on it costs tokens
  - Once full: new content replaces old content (not ideal — use /compact instead)
  - Quality of work degrades when context is cluttered with low-signal content

What fills context fastest:
  - Large files with irrelevant sections
  - Long conversation history with finished tasks
  - Verbose responses from Codex that explain every step
  - Error output that was already resolved

What you want in context:
  - The specific files for this task (not all project files)
  - The task goal and constraints
  - Verification command
  - Current test failure output (if debugging)
```

---

## 2. Session Startup Priming

```bash
# Bad startup (loads everything, wastes context):
codex  # then just type the task
# Result: Codex may load 20+ files based on auto-discovery heuristics

# Good startup (prime with exactly what's needed):
codex "Context for this session:
       Working on: GET /orders pagination
       Key files: src/api/orders.py, src/services/order_service.py
       Reference pattern: src/api/users.py (the pagination there is the target pattern)
       Tests: tests/test_order_api.py
       Ignore: src/auth/, src/notifications/, src/db/migrations/
       
       First task: [describe first task]"

# This loads 4 files instead of potentially 20
# Saves context for actual work
```

---

## 3. /compact — When and How

```bash
# /compact tells Codex to summarize the conversation so far into a compact form
# Use this when:
#   - You've completed one task and starting another
#   - The conversation is long and you notice Codex forgetting earlier context
#   - You want to continue without starting a fresh session

# The mechanics: /compact summarizes conversation history, keeping the essence
# Trade-off: some detail is lost (exact earlier outputs) — but headroom is gained

# When to use /compact:
# 1. After completing a major task before starting the next
codex
# ... complete task 1 ...
/compact
# ... start task 2 with headroom ...

# 2. After a debugging session where long error output was in context
/compact
# "Now that [error] is resolved, next task: [new task]"

# 3. When Codex starts giving worse outputs (sign of context pollution)
/compact
# Then restate the current task with fresh context

# When NOT to use /compact:
# - In the middle of a multi-step task (you'll lose the intermediate context)
# - When the earlier context is directly relevant to the next step
```

---

## 4. Model Selection by Task Complexity

```yaml
# config.yaml — default for most work
model: o4-mini
approval_policy: auto-edit
```

```bash
# Override per session:

# gpt-4.1 for: architecture design, complex debugging, multi-file refactors
codex --model gpt-4.1 "design the caching strategy for the user service"

# o4-mini for: 90% of tasks — implementation, tests, docs, quick reviews
codex "add pagination to GET /orders"

# gpt-4.1-mini for: pure documentation tasks (fastest + cheapest for docs)
codex --model gpt-4.1-mini "write docstrings for src/payments/service.py"

# Decision table:
# Architecture, design, complex reasoning → gpt-4.1
# Debugging unfamiliar errors → gpt-4.1
# Security review (important) → gpt-4.1
# Implementation (clear spec) → o4-mini
# Test generation → o4-mini
# Refactoring → o4-mini
# Documentation only → gpt-4.1-mini
```

---

## 5. High-Signal vs Low-Signal Prompts

```bash
# Low-signal: Codex uses tokens to interpret ambiguity
codex "make the user endpoint better"
# Codex internally: "better means what? performance? readability? testing? 
#                    which endpoint? what's the current problem?"
# Result: either asks clarifying questions (uses tokens) or guesses wrong (wastes turn)

# High-signal: every word constrains the task
codex "Refactor GET /users in src/api/users.py to extract the auth check into 
       a get_current_user_or_403() helper. No behavior changes. 
       Verification: pytest tests/test_user_api.py -x"
# Codex: reads 2 files, makes 1 structural change, runs 1 command
# Result: first-pass success rate much higher

# Signal quality checklist:
[ ] File specified (not "the user code")
[ ] Action specified (not "improve" or "fix")
[ ] Success criterion specified (test command)
[ ] Scope boundaries specified (what NOT to touch)
```

---

## 6. Task Batching — Related Work in One Session

```bash
# Anti-pattern: 4 separate sessions for related tasks
# → loads context 4 times, no information flows between tasks

# Better: batch related tasks with /compact between each
codex "Context: working on the notifications module today.
       Files: src/notifications/service.py, tests/test_notifications.py
       
       First task: add input validation to send_notification()"
# ... complete task 1 ...
/compact
# "Second task: write docstrings for all functions in src/notifications/service.py"
# ... complete task 2 ...
/compact
# "Third task: generate test cases for the 3 error paths in send_notification()"
```

---

## Interview Traps

```
TRAP: "/compact loses important context — avoid using it"
TRUTH: /compact summarizes conversation history, not your codebase. The key information
       (AGENTS.md rules, current task, last error output) should be restated after /compact.
       The loss is a summary of earlier exchanges. The gain is significant context headroom.

TRAP: "o4-mini is a cost compromise — always use gpt-4.1 for quality work"
TRUTH: o4-mini handles the vast majority of coding tasks at equivalent quality to gpt-4.1.
       gpt-4.1 is better for architecture and complex reasoning. For implementation tasks
       with a clear spec, o4-mini is faster and produces equivalent output at lower cost.

TRAP: "More context always helps — give Codex the full codebase for better results"
TRUTH: Past ~5-10 highly relevant files, additional context is noise that degrades quality.
       Codex cannot reason about 50 files simultaneously. Precise scoping (3-5 key files)
       consistently outperforms "load everything and hope."
```

---

## Revision Checklist

- [ ] Use /compact between unrelated tasks in the same session
- [ ] Session startup explicitly scopes the relevant files
- [ ] Model selection matches task type (gpt-4.1 for architecture, o4-mini for implementation)
- [ ] Prompts include file, action, constraint, and verification — no ambiguous "improve X"
- [ ] Related tasks batched in one session instead of separate sessions
