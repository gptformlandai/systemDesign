# Copilot Mental Model — Gold Sheet

> **Track**: Copilot Mastery Track — Group 1: Foundations
> **File**: 1 of 6 (Track File #1)
> **Audience**: Developers starting their GitHub Copilot journey
> **Read after**: Nothing — start here

---

## 1. Practical Impact Meter

| Topic | Impact | Why Developers Miss This |
|---|---|---|
| Copilot is a context machine — not a mind reader | ★★★★★ | Devs expect Copilot to "just know" their codebase without explicit context |
| Difference between inline, Chat, Edits, and Agent Mode | ★★★★★ | Using the wrong mode for a task produces worse results and wastes tokens |
| What context Copilot actually sees | ★★★★★ | Devs wonder why Copilot ignores their architecture; it never saw the architecture files |
| Copilot ≠ ChatGPT ≠ Copilot for M365 | ★★★★☆ | These are three different products with different capabilities and use cases |
| Why prompt clarity determines output quality | ★★★★★ | Vague prompts produce generic output; specific prompts produce targeted output |
| The "first draft" mental model | ★★★★★ | Treating Copilot output as final is the root cause of most AI-assisted bugs |
| How Copilot fits into the developer lifecycle | ★★★★☆ | Knowing WHERE in the SDLC to invoke Copilot vs where to think independently |

---

## 2. What GitHub Copilot Is

### Must Know

```
GitHub Copilot is an AI pair programmer built into your editor.
It uses large language models (LLMs) to:
  1. Suggest code inline as you type (autocomplete)
  2. Answer questions about your code in a chat interface
  3. Make multi-file edits based on natural language instructions
  4. Act autonomously as an agent to complete multi-step coding tasks
  5. Review your code for quality, security, and correctness issues

What it is NOT:
  - It is not a search engine for documentation
  - It is not a code execution environment
  - It is not a replacement for understanding code
  - It is not always right — it hallucinates APIs, methods, and logic
  - It does not have real-time internet access in most modes (model-dependent)
  - It does not have persistent memory across conversations (by default)
```

### The Five Products — Do Not Confuse Them

| Product | What It Is | Where You Use It |
|---|---|---|
| GitHub Copilot | AI pair programmer — code generation, chat, agents | VS Code, JetBrains, CLI, GitHub.com |
| Microsoft 365 Copilot | AI productivity assistant — documents, email, meetings | Word, Excel, Teams, Outlook, M365 web |
| GitHub Copilot Chat | The conversational AI layer of GitHub Copilot | VS Code Chat panel, GitHub.com |
| Copilot Studio | Low-code platform to build enterprise AI agents | Web-based, admin-controlled |
| Copilot Chat Agent Builder | Tool to create custom Copilot Chat agents | M365 ecosystem (if licensed) |

**Interview-ready answer:** GitHub Copilot is the VS Code / IDE-level AI coding tool.
Microsoft 365 Copilot is the enterprise productivity tool for Office apps.
They share branding but are separate products with separate licensing.

---

## 3. The Context Machine Model

### Must Know

This is the most important concept in the entire track.

```
Copilot output quality = f(context quality, prompt quality)

Context = everything Copilot can see when it generates a response:

  Open files in your editor          ← most important for inline suggestions
  Selected text in editor            ← most important for Chat
  Files you explicitly reference     ← #file:path context variable
  Your selection via #selection      ← Chat context variable
  Codebase index                     ← #codebase context variable
  Repository instructions            ← .github/copilot-instructions.md
  Path-specific instructions         ← .github/instructions/*.instructions.md
  Your conversation history          ← within one chat session only
  MCP tool outputs                   ← if MCP tools are configured

Context Copilot does NOT automatically see:
  Files you have closed
  Other repositories
  Your architecture decisions not written down
  Your team conventions not in instruction files
  Your mental model of the codebase
  Previous chat conversations (no persistent memory by default)
  Secrets (it sees what you paste — never paste secrets)
```

### Practical Consequence

```
Scenario: You want Copilot to refactor a UserService class.

Wrong approach (no context):
  "Refactor the UserService to be more maintainable."
  → Copilot generates generic boilerplate, ignores your actual code

Right approach (targeted context):
  Open UserService.py → select the class
  In Chat: "Refactor #selection to extract the email validation logic
  into a separate EmailValidator class following the single responsibility principle.
  Keep the public API identical. Project uses Python 3.12, pytest for tests."
  → Copilot sees the actual code, has a clear goal, knows the constraints
```

---

## 4. The Six Copilot Surfaces — When To Use Each

### Surface 1: Inline Suggestions (Autocomplete)

```
What it is:
  Ghost text that appears as you type — complete line or multi-line suggestion.
  Press Tab to accept, Escape to reject, Alt+] / Alt+[ to cycle alternatives.

Best for:
  - Boilerplate code (getters, setters, constructors, standard methods)
  - Completing a pattern you've started (writing 1 test, Copilot offers the next 3)
  - Implementing known algorithms in a known file context
  - Filling out configuration (JSON, YAML, SQL schemas)

Not great for:
  - Complex multi-step logic requiring planning
  - Cross-file understanding
  - Questions about existing code

Context it sees:
  Open file content above and below cursor, imports, nearby function signatures
```

### Surface 2: Chat — Ask Mode

```
What it is:
  Conversational interface. You ask; Copilot answers.
  Does NOT modify your files without you acting on the response.

Best for:
  - Understanding code ("Explain what this function does")
  - Asking about approach ("What's the best way to handle pagination in FastAPI?")
  - Debugging ("Why might this return None?")
  - Learning concepts ("What is the GIL in Python?")
  - Getting code snippets to copy manually

Context it sees:
  What you reference with: #file, #selection, #codebase, #sym, #terminalLastCommand
  Your message history in the current conversation

Not great for:
  - Making changes (use Edits or Agent Mode)
  - Large multi-file operations
```

### Surface 3: Chat — Edits Mode (Copilot Edits)

```
What it is:
  Copilot makes changes directly to your files.
  You review a diff and accept/reject changes.
  Works on a "working set" of files you explicitly add.

Best for:
  - Targeted multi-file changes with diff review
  - Refactoring across 2-5 files
  - Adding consistent patterns across files
  - Updating tests after changing implementation

Context it sees:
  Files in your working set + any #file references

Not great for:
  - Tasks requiring planning before execution
  - Tasks that need to run commands (use Agent Mode)
  - Exploratory codebase analysis
```

### Surface 4: Agent Mode

```
What it is:
  Copilot autonomously plans and executes a multi-step task.
  Can read files, write files, run terminal commands, run tests, iterate.
  Requires more oversight — it can make sweeping changes.

Best for:
  - Building a new feature end to end
  - Scaffolding a new project
  - Complex refactoring with test updates
  - Automated workflows where steps depend on previous step output

Context it sees:
  Codebase index, files it reads, tool outputs, your instruction files

Critical rules:
  - Always give Agent Mode a planning requirement: "Plan the changes before making any"
  - Always use source control — commit before Agent Mode runs
  - Review all changes in the diff before accepting
  - Do not use Agent Mode for production-critical changes without human review gate
```

### Surface 5: Copilot Code Review

```
What it is:
  Copilot reviews your code change (PR diff or selection) and generates review comments.
  Available in GitHub.com PR interface and VS Code.

Best for:
  - Pre-PR self-review
  - Catching missed edge cases
  - Security pattern review
  - Test coverage gap identification

Context it sees:
  The diff/selection you submit for review

Critical rule:
  Copilot code review is NOT a substitute for human review.
  Use it as a first pass to catch obvious issues before human reviewers see the PR.
```

### Surface 6: Terminal / Commit / PR Assistance

```
Terminal:
  Copilot can suggest shell commands in the VS Code terminal.
  Always read the suggested command before running it.
  Copilot does not know your current directory or environment unless it can see it.

Commit messages:
  Copilot can generate a commit message from your staged diff.
  Useful for descriptive commits; always review for accuracy.

PR summaries:
  GitHub.com Copilot can generate a PR description from the diff.
  Provides a first draft; review to ensure it accurately describes the change.
```

---

## 5. The Developer Lifecycle — Where Copilot Adds Value

```
Phase                  | Copilot Surface      | High-ROI Use Cases
-----------------------|----------------------|-----------------------------------------------
Requirements           | Chat Ask             | Clarify ambiguous requirements, generate questions
Architecture Planning  | Chat Ask             | Compare patterns, generate ADR draft
Codebase Exploration   | Chat + #codebase     | "How does user authentication work in this repo?"
API Design             | Chat Ask + Inline    | Generate OpenAPI spec, design request/response shapes
Implementation         | Inline + Edits       | Boilerplate, standard patterns, algorithm completion
Unit Testing           | Edits + Prompt file  | Generate tests for selected code, test gap analysis
Integration Testing    | Agent Mode           | Scaffold integration test suite, configure Testcontainers
Debugging              | Chat Ask             | Explain error, suggest fix, analyze stack trace
Refactoring            | Edits + Agent Mode   | Extract methods, apply patterns, update tests
Documentation          | Prompt file          | README generation, docstring completion, ADR
PR Creation            | PR summary prompt    | Generate PR description from diff
PR Review              | Code Review surface  | Security, correctness, test coverage gaps
CI/CD                  | Prompt file + Edits  | GitHub Actions workflow generation and debugging
Release Notes          | Prompt file          | Generate release notes from commits/diff
Incident Debugging     | Chat Ask             | Analyze log snippet, suggest investigation steps
Learning               | Chat Ask             | Explain concepts, compare approaches, generate notes
```

---

## 6. The First Draft Mental Model

### Must Know

```
Every Copilot output is a FIRST DRAFT.

First drafts:
  ✓ Save time on boilerplate
  ✓ Provide starting points
  ✓ Surface options you hadn't considered
  ✗ Are not verified correct
  ✗ May use deprecated APIs
  ✗ May contain logical bugs
  ✗ May contain security vulnerabilities
  ✗ May hallucinate method names that don't exist
  ✗ May miss edge cases

Your job as the developer:
  1. Evaluate the first draft critically
  2. Verify correctness by reasoning through the logic
  3. Run existing tests
  4. Add tests for the new code
  5. Review for security issues
  6. Check that dependencies exist and are at safe versions
  7. Accept, modify, or reject — never blindly accept

The most dangerous Copilot output is the one that LOOKS correct.
It compiles, passes linting, and produces wrong results in edge cases.
```

---

## 7. Copilot Does Not Know These Things (Unless You Tell It)

```
1. Your project's architecture decisions
   Fix: Add an architecture note to copilot-instructions.md

2. Your team's coding conventions
   Fix: Add conventions to copilot-instructions.md or path-specific instructions

3. Your preferred libraries for each task
   Fix: Explicitly name libraries in prompts or instructions

4. Your security requirements
   Fix: Add a security.instructions.md with security rules

5. Which tests exist and what they cover
   Fix: Reference the test directory in your prompt

6. The current state of a bug you haven't explained
   Fix: Paste the error message, stack trace, and relevant code

7. What you tried before that didn't work
   Fix: Include "I already tried X, it didn't work because Y" in your prompt

8. Files you haven't opened or referenced
   Fix: Use #file:/path/to/file or add files to working set in Edits mode

9. Your previous conversations with Copilot
   Fix: Keep related tasks in one conversation session, or summarize previous context

10. Production data, logs, or metrics (and you should NEVER paste real production data)
    Fix: Use sanitized/synthetic examples that mirror the structure without real data
```

---

## 8. Strong Copilot Mental Model — Demonstration

### Bad mental model (leads to frustration):

```
"I asked Copilot to refactor my service and it gave me something completely different
from what my codebase looks like. Copilot doesn't work."

Why this fails:
  - "Refactor my service" — which service? Copilot hasn't seen it.
  - No file context attached.
  - No constraints given (patterns to follow, methods to keep, tests to pass).
  - No indication of what "better" means.
```

### Good mental model (leads to results):

```
"Let me attach UserService.py with #file, select the processPayment method,
and tell Copilot: 'Refactor #selection to extract validation logic into a
separate PaymentValidator class. Keep processPayment's public signature
identical. We use Python 3.12, Pydantic v2 for validation, and pytest.
Do not change any existing tests.'"

Why this works:
  - Copilot sees the exact code to change.
  - The goal is specific and scoped.
  - Constraints prevent unwanted side effects.
  - Technology stack is named so library choices are correct.
```

---

## 9. Revision Checklist

- [ ] Can explain what Copilot is and what it is NOT
- [ ] Can distinguish GitHub Copilot from M365 Copilot from Copilot Studio
- [ ] Can name all 6 Copilot surfaces and the best use case for each
- [ ] Knows what context Copilot sees and what it does NOT automatically see
- [ ] Can explain the "first draft" mental model
- [ ] Knows 5 things Copilot doesn't know unless told explicitly
- [ ] Can write a context-rich prompt vs a vague prompt for the same task
- [ ] Can identify which SDLC phase maps to which Copilot surface
