# Copilot Chat — Fundamentals — Gold Sheet

> **Track**: Copilot Mastery Track — Group 1: Foundations
> **File**: 4 of 6 (Track File #4)
> **Audience**: Developers learning to use Copilot Chat effectively
> **Read after**: Copilot-Inline-Suggestions-Gold-Sheet.md

---

## 1. Practical Impact Meter

| Topic | Impact | Why Developers Miss This |
|---|---|---|
| Context variables: `#file`, `#selection`, `#codebase` | ★★★★★ | Most devs type vague prompts without attaching context — results are generic |
| Chat panel vs inline Chat — when to use which | ★★★★★ | Using inline chat for complex questions limits output quality |
| Conversation scoping — starting fresh vs continuing | ★★★★☆ | Carrying stale context from old conversations pollutes answers |
| Built-in slash commands: `/explain`, `/fix`, `/tests` | ★★★★★ | Devs recreate these manually not knowing they exist as built-ins |
| Model selection — when to use which model | ★★★★☆ | Default model is not always the best choice for the task |
| Referencing symbols with `#sym` | ★★★★☆ | Saves manually pasting function/class definitions |
| Chat history — what persists and what doesn't | ★★★★☆ | Devs assume Copilot remembers — it doesn't across sessions |

---

## 2. Chat Panel vs Inline Chat

### Chat Panel — The Main Interface

```
Open: Cmd+Shift+I (macOS) / Ctrl+Shift+I (Windows)

Use for:
  - Longer conversations requiring back-and-forth
  - Questions about your codebase architecture
  - Explaining complex code
  - Planning features or refactoring strategies
  - Generating code you will manually paste/adapt
  - Learning concepts ("Explain async/await in Python")

The Chat panel is a persistent conversation window.
All messages in one session share context (within the context window limit).
Starting a new chat (Cmd+L) clears the conversation — use this to avoid stale context.
```

### Inline Chat — In-Editor Commands

```
Open: Cmd+I (macOS) / Ctrl+I (Windows)
Appears in the editor at cursor position.

Use for:
  - Quick, targeted edits to selected code
  - Generating boilerplate at a specific location
  - Asking about one line or one function
  - Quick fix suggestions

Limitations:
  - Shorter output — not suited for long explanations
  - Less back-and-forth — one-shot is most common use
  - Changes are applied directly to the file; review carefully

Best workflow:
  Select the code → Cmd+I → type instruction → press Enter
  Copilot modifies the selected code in place.
  Review the change, then: Accept (Enter) or Discard (Escape)
```

---

## 3. Context Variables — The Power Features

### Must Know

Context variables are how you tell Copilot exactly what to look at.
Without them, Copilot guesses based on what is open in your editor.

### `#file` — Reference a Specific File

```
Usage: #file:/path/to/file.py
Or: type # in Chat → Copilot shows a file picker

Example prompts:
  "Review #file:src/services/user_service.py for potential null pointer errors"
  "Explain how #file:src/models/user.py is used in the codebase"
  "Generate unit tests for #file:src/api/auth.py"

When to use:
  - When the relevant code is NOT currently selected or open
  - When you want Copilot to analyze a specific file without opening it
  - When you need to reference multiple files in one prompt

Multiple files:
  "Compare the validation logic in #file:user_service.py and #file:order_service.py"
```

### `#selection` — Reference Selected Code

```
Usage: First select text in editor, then type #selection in Chat

Example prompts:
  "Explain what #selection does"
  "Refactor #selection to use list comprehension"
  "Write a unit test for #selection"
  "What edge cases does #selection miss?"

Best for:
  - Asking about a specific function or block you have highlighted
  - Quick targeted operations on selected code
  - Getting an explanation of code you don't understand

Rule: Select before opening Chat. #selection captures whatever is highlighted at prompt time.
```

### `#codebase` — Search the Entire Repository Index

```
Usage: type #codebase in your prompt

Example prompts:
  "How does #codebase handle database connections?"
  "Does #codebase have any tests for the payment service?"
  "What authentication strategy does #codebase use?"
  "Find all places in #codebase where we directly access the database from a controller"

What it does:
  Copilot indexes your repository and performs a semantic search.
  It finds the most relevant files/functions and includes them in context.
  Works best in repos that are not too large (< 100k lines is comfortable).

Limitation:
  #codebase does not read every file. It searches the index for most relevant results.
  Very large repos may not return the exact file you need.
  For precise context, use #file instead.
```

### `#sym` — Reference a Symbol (Function, Class, Method)

```
Usage: type #sym in Chat → start typing a symbol name → picker appears

Example prompts:
  "Explain #sym:UserService.create_user"
  "What callers does #sym:validate_email have in the codebase?"
  "Generate a test for #sym:PaymentProcessor.charge"

Best for:
  - Asking about a specific function without manually navigating to it
  - Cross-referencing usages of a class or method
```

### `#terminalLastCommand` — Reference Last Terminal Command Output

```
Usage: type #terminalLastCommand in Chat

Example prompts:
  "Explain the error in #terminalLastCommand"
  "Fix the test failure shown in #terminalLastCommand"
  "What does this warning in #terminalLastCommand mean?"

Best for:
  - Quick debugging of terminal errors without copy-pasting
  - Analyzing test output, build failures, or stack traces
```

### `#editor` — Reference the Active Editor Content

```
Usage: type #editor in Chat

Attaches the full content of the currently active file to the context.
Use when you want Copilot to see the complete file, not just a selection.
```

---

## 4. Built-In Slash Commands

These are built into Copilot Chat — you do not need to create prompt files for them.

```
/explain    — Explain selected code or a file
             Example: select a function → /explain

/fix        — Suggest a fix for selected code or an error
             Example: select broken code → /fix
             Or: /fix the error shown in #terminalLastCommand

/tests      — Generate unit tests for selected code
             Example: select a service class → /tests

/doc        — Generate a docstring/documentation for selected code
             Example: select a function → /doc

/simplify   — Simplify selected code
             Example: select complex nested conditions → /simplify

/new        — Scaffold a new file or project
             Example: /new create a FastAPI router for user management

/newNotebook — Create a new Jupyter notebook
              Example: /newNotebook for data analysis of CSV files

Note: Available slash commands depend on your Copilot version.
Check current available commands: type / in Chat → see the list.
```

---

## 5. Model Selection

Copilot supports multiple models. Choosing correctly saves tokens and improves quality.

```
Model Selection: Click the model name at the top of the Chat panel

Available models (verify current list — this changes over time):
  GPT-4.1         — Strong reasoning, code, architecture questions (default for complex)
  Claude Sonnet   — Strong for nuanced analysis, code review, explanation
  Claude Haiku    — Fast, cheap — good for simple questions and quick tasks
  Gemini          — Alternative perspective, sometimes better for specific languages
  o1 / o3 series  — Deep reasoning — use for complex algorithmic problems

Practical model selection guide:
  Simple question ("What does this do?")        → Use any fast model
  Code generation (standard CRUD/boilerplate)   → GPT-4.1 or Claude Sonnet
  Architecture review / complex reasoning       → o1 or Claude Sonnet with extended thinking
  High-volume quick tasks (batch questions)     → Claude Haiku or GPT-4.1 mini
  Security review / nuanced analysis            → Claude Sonnet
  Algorithm / DSA problem solving               → o1 or o3
```

---

## 6. Effective Chat Prompt Patterns

### The CGOFC Pattern (Context → Goal → Output → Format → Constraints)

```
Context:     What code / file / situation is this about?
Goal:        What do you want Copilot to do?
Output:      What should the result look like?
Format:      How should the output be structured?
Constraints: What must it NOT do? What must it preserve?

Example applying CGOFC:
  Context:    "The #file:src/services/user_service.py currently handles user auth"
  Goal:       "Extract the token validation logic"
  Output:     "into a separate TokenValidator class in a new file"
  Format:     "Show me the new class and the updated UserService"
  Constraints:"Keep all existing method signatures. Do not change the database layer."

Combined: "The UserService in #file:src/services/user_service.py handles auth.
Extract the token validation logic into a separate TokenValidator class in a
new file. Show me the new class and the updated UserService. Keep all existing
method signatures. Do not change the database layer."
```

### Good vs Bad Prompts

```
BAD: "Fix my code"
Why: No context, no code, no description of what's wrong.

BETTER: "Fix the TypeError in #selection — it occurs when user_id is None"

BAD: "Write a test"
Why: No context, no code to test, no testing framework specified.

BETTER: "Write pytest unit tests for #sym:UserService.create_user.
Cover: happy path, duplicate email error, missing required field.
Use fixtures for the database session. Mock the email service."

BAD: "Explain this"
Why: "this" is undefined without selection.

BETTER: "Explain what #selection does and why it uses a context manager here."

BAD: "Make it faster"
Why: No profiling data, no code, no performance target.

BETTER: "The function #sym:process_orders takes 5 seconds for 10k records.
It currently does N+1 database queries. Suggest how to fix the N+1 issue
without changing the function signature."
```

---

## 7. Managing Chat Conversations

### When to Start a New Conversation

```
Start a NEW conversation when:
  - The topic changes completely (switching from auth to billing)
  - Previous answers were poor and you want a fresh start
  - You finished a task and are starting a new one
  - The conversation is very long (context window may be exceeded)

How to start new: Cmd+L in the Chat panel (macOS)

Continue the SAME conversation when:
  - Iterating on the same piece of code
  - Following up with more details after initial context was given
  - Multi-step problem where Copilot needs to remember prior steps

Why this matters:
  Long conversations accumulate context. Once the context window is exceeded,
  Copilot forgets earlier messages — effectively having amnesia mid-conversation.
  Starting fresh avoids this degradation.
```

### What Copilot Remembers — and What It Doesn't

```
Within ONE chat session:
  ✓ All messages in the current conversation (until context window exceeded)
  ✓ Files you referenced with #file
  ✓ Code you pasted
  ✓ Code it generated earlier in the session

Across chat sessions:
  ✗ Previous conversations (no persistent memory by default)
  ✗ Files you referenced in old sessions
  ✗ Your previous requests and outcomes

Persistent across all sessions:
  ✓ copilot-instructions.md (loaded every time)
  ✓ Path-specific instruction files
  ✓ Your prompt files (appear as slash commands)
  ✓ Your custom agent files

Rule: If context must persist — put it in copilot-instructions.md, not in chat history.
```

---

## 8. Chat for Code Explanation — Patterns

```
Explaining code you don't understand:

1. Select the confusing code
2. Ask: "Explain what #selection does step by step.
         Point out any non-obvious behavior or potential pitfalls."

Explaining why a pattern was used:

"Looking at #file:src/database.py — why does it use a context manager
for database sessions? What problem does this solve?"

Explaining an error:

"I'm getting this error: [paste error]
The error comes from #file:src/services/user_service.py line 45.
Explain why this error occurs and what the fix should be."

Explaining architectural decisions:

"Looking at #codebase — why is there a separate repository layer
between the service and the database? What pattern is this?"
```

---

## 9. Chat for Learning — Using Copilot as a Teacher

```
Concept explanation:
"Explain Python async/await for someone who understands Java threads.
Use analogies and a concrete code example. Keep it under 300 words."

Comparison:
"Compare Python generators vs list comprehensions.
When should I use each? Show a memory usage example."

Best practices:
"What are the top 5 mistakes developers make when using asyncio in Python?
For each mistake, show the bad pattern and the correct pattern."

Deep dive:
"Explain how Python's GIL works internally.
Use a threading example to show where the GIL causes a bottleneck
and where it doesn't matter."

Note: Copilot is a strong teacher but verifies what it tells you.
For cutting-edge topics (very recent library releases), prefer official docs.
Copilot's training data has a knowledge cutoff.
```

---

## 10. Revision Checklist

- [ ] Knows when to use Chat Panel vs Inline Chat
- [ ] Can use `#file`, `#selection`, `#codebase`, `#sym` fluently
- [ ] Knows all built-in slash commands: `/explain`, `/fix`, `/tests`, `/doc`
- [ ] Understands what Copilot remembers within a session vs across sessions
- [ ] Can apply the CGOFC pattern to any prompt
- [ ] Knows when to start a new conversation vs continue the same one
- [ ] Can select an appropriate model for a given task type
- [ ] Can write a prompt for code explanation, debugging, and learning
