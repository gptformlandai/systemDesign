# Prompt Files & Slash Commands — Gold Sheet

> **Track**: Copilot Mastery Track — Group 2: Intermediate Power User
> **File**: 2 of 7 (Track File #8)
> **Audience**: Developers building a reusable prompt library
> **Read after**: Custom-Instructions-Deep-Dive-Gold-Sheet.md

---

## 1. Practical Impact Meter

| Topic | Impact | Why Developers Miss This |
|---|---|---|
| Prompt file anatomy — YAML frontmatter + body | ★★★★★ | Devs don't know prompt files exist as first-class VS Code citizens |
| Creating a slash command from a prompt file | ★★★★★ | Without this, devs retype the same 10-line prompt every day |
| Argument hints — making prompts dynamic | ★★★★☆ | Static prompt files are limited; arguments make them reusable |
| Organizing a prompt library | ★★★★☆ | Unorganized prompts become unusable quickly |
| Prompt quality principles | ★★★★★ | A bad prompt file is worse than typing ad-hoc — it enforces bad patterns |
| Team-shared prompt libraries | ★★★★☆ | The same prompt library shared across a team multiplies productivity |

---

## 2. What Prompt Files Are

### Must Know

```
Prompt files are Markdown files with YAML frontmatter that become reusable
slash commands in Copilot Chat.

Location: .github/prompts/<name>.prompt.md

Once saved in .github/prompts/, they appear as:
  /<name>   in the Copilot Chat slash command picker (type / in Chat)

Why they matter:
  Prompts you type repeatedly = knowledge that should be encoded in a prompt file.
  Prompt files: version-controlled, team-sharable, consistent, iteratable.

What prompt files can do:
  - Define a reusable workflow (e.g., /generate-tests always asks for coverage of
    happy path, error cases, and edge cases — without repeating this every time)
  - Provide a consistent structure for common tasks
  - Reduce cognitive load — you don't need to remember the optimal prompt for each task
  - Capture institutional knowledge ("how we do code review here")

What prompt files cannot do:
  - Run code
  - Access external systems directly (unless paired with MCP tools)
  - Force Copilot to always be correct
```

---

## 3. Prompt File Anatomy

### Minimal Prompt File

```markdown
---
name: Explain Code
description: Explain what selected code does, its purpose, and any notable patterns
---

Explain what the following code does:

${input:What code should I explain? (leave blank if you've made a selection)}

Provide:
1. A one-line summary of what this code does
2. A step-by-step explanation of the logic
3. Any notable design patterns or techniques used
4. Potential edge cases or gotchas
5. How this code fits into the broader system (if context is available)
```

### Full Prompt File — All Fields

```markdown
---
name: Generate Tests
description: Generate comprehensive pytest unit tests for selected Python code
mode: ask
tools: []
---

Generate pytest unit tests for the following Python code:
${selection}

Requirements:
- Framework: pytest with pytest-asyncio for async functions
- Cover: happy path, at least 2 error scenarios, at least 1 edge case
- Use descriptive test names: test_<function>_<scenario>_<expected>
- Mock all external dependencies (HTTP clients, email services, database if not using fixtures)
- Use type hints in test functions
- One logical assertion per test (multiple assert statements allowed for related checks)

If the code uses SQLAlchemy AsyncSession, use a db_session fixture.
If the code uses httpx, mock it with respx.

Output format:
- Complete test file ready to run
- Import statements at top
- Test class wrapping related tests
- Brief comment above each test explaining what it validates
```

### YAML Frontmatter Fields

```yaml
---
name: Human-readable name displayed in the slash command picker
description: One-line description shown in the picker below the name
mode: ask | edit | agent   # which Copilot mode to use
tools: []   # list of MCP or built-in tools the agent can use
---
```

### Template Variables

```
${input:<placeholder>}      — prompts user to type input when command runs
${selection}                — uses currently selected text in editor
${file}                     — uses currently active file
${workspaceFolder}          — absolute path to workspace root
```

---

## 4. Prompt File Library — Production Examples

### `/explain-code`

```markdown
---
name: Explain Code
description: Explain selected code with purpose, logic, patterns, and gotchas
---

Explain the following code to a developer who is new to this codebase:

${selection}

Structure your explanation:
1. **One-line summary**: What does this do?
2. **Why it exists**: What problem does it solve?
3. **How it works**: Walk through the logic step by step
4. **Design patterns**: Name any patterns used (decorator, strategy, factory, etc.)
5. **Gotchas**: Any surprising behavior, edge cases, or common mistakes
6. **Dependencies**: What does this rely on that is not visible here?

Keep explanations concrete. Use "This returns..." and "This calls..." not "This is used to...".
```

### `/debug-error`

```markdown
---
name: Debug Error
description: Diagnose an error with root cause analysis and fix options
---

Debug the following error:

Error:
${input:Paste the full error message and stack trace}

Context:
${input:Describe what you were doing when the error occurred (optional)}

Relevant code:
${selection}

Provide:
1. **Root cause**: What is actually wrong?
2. **Why it happens**: The underlying reason (not just "the variable is None")
3. **Fix options**: At least 2 ways to fix it, with trade-offs
4. **Prevention**: How to prevent this class of error in future code
5. **If still stuck**: What to check next if none of the fixes work

Do not suggest "check your syntax" unless there is a clear syntax error.
Provide specific, actionable fixes.
```

### `/generate-tests`

```markdown
---
name: Generate Tests
description: Generate comprehensive pytest tests for selected code
---

Generate pytest unit tests for:

${selection}

Requirements:
- pytest with pytest-asyncio for async functions
- Cover: happy path, error cases, edge cases (empty, None, boundary values)
- Test names: test_<function>_<scenario>_<expected_outcome>
- One logical assertion per test
- Mock external dependencies: HTTP, email, payment, file I/O
- Do NOT test implementation details — test behavior

Output:
- Complete test file with imports
- Pytest fixture if database session needed (use AsyncSession from SQLAlchemy)
- Brief docstring or comment explaining what each test validates

If the selected code has no clear error handling: note that in a comment above the tests.
```

### `/refactor-code`

```markdown
---
name: Refactor Code
description: Suggest a clean refactoring with explanation of each improvement
---

Refactor the following code:

${selection}

Goals:
${input:What is the primary refactoring goal? (e.g., extract method, reduce duplication, apply SOLID, improve readability)}

Rules:
- Preserve all existing public API signatures unless explicitly asked to change them
- Do not change test behavior — existing tests must still pass
- Do not add new dependencies unless they are clearly better than what exists
- Keep the same language/framework — do not suggest migration

Output:
1. The refactored code (complete, runnable)
2. A list of what changed and WHY each change was made
3. What you intentionally did NOT change and why
4. Any follow-up refactoring opportunities (but don't implement them now)
```

### `/architecture-review`

```markdown
---
name: Architecture Review
description: Review code or design for architectural quality and trade-offs
---

Review the following for architectural quality:

${selection}

Evaluate:
1. **Separation of concerns**: Are responsibilities cleanly separated?
2. **Coupling**: What is tightly coupled that should be loosely coupled?
3. **SOLID principles**: Which are followed? Which are violated?
4. **Testability**: How easy is this to unit test? What makes it hard?
5. **Scalability**: What breaks under load or with growing data?
6. **Maintainability**: What would be painful to change 6 months from now?
7. **Missing abstractions**: What concepts in the domain are not represented?

For each issue found:
- Describe the problem
- Explain the consequence (what goes wrong when this is a problem)
- Suggest a concrete improvement

Be direct. Don't compliment the code — identify what needs improvement.
```

### `/generate-learning-notes`

```markdown
---
name: Generate Learning Notes
description: Create structured learning notes on a topic for future review
---

Create structured learning notes on:

Topic: ${input:What topic should I create notes for?}

Format (follow this exactly):
## [Topic Name]

### What It Is
[2-3 sentence definition]

### Why It Matters
[Why a developer needs to understand this]

### How It Works
[Internals or mechanics — step by step]

### Key Rules
[Bullet list of the most important things to remember]

### Code Example
[Minimal but complete working example]

### Common Mistakes
[3-5 mistakes developers make with this topic]

### Strong Answer
[How to explain this topic clearly in 3-5 sentences — interview or team discussion ready]

### Revision Questions
[5 questions to test understanding of this topic]

Keep each section concise. Prefer code examples over prose where possible.
```

### `/create-github-action`

```markdown
---
name: Create GitHub Action
description: Generate a GitHub Actions workflow file
---

Create a GitHub Actions workflow for:

${input:Describe what this workflow should do (e.g., CI for Python with pytest, deploy to AWS Lambda, publish npm package)}

Requirements:
- Trigger: ${input:When should it run? (e.g., push to main, pull_request, manual dispatch)}
- Environment: ${input:What OS and runtime? (e.g., ubuntu-latest, Python 3.12)}

Rules:
- Pin all action versions (e.g., actions/checkout@v4, not @main or @latest)
- Cache dependencies for faster runs
- Use environment secrets for credentials (never hardcode)
- Add concurrency group to cancel stale runs on PR
- Show clear step names — no "Run step 1" naming

Output:
- Complete workflow YAML, ready to save in .github/workflows/
- Brief comment explaining any non-obvious configuration
- List of required GitHub Secrets to configure
```

---

## 5. Organizing Your Prompt Library

### Directory Structure

```
.github/
  prompts/
    # Development workflow
    explain-code.prompt.md
    debug-error.prompt.md
    refactor-code.prompt.md

    # Testing
    generate-tests.prompt.md
    test-gap-analysis.prompt.md
    fix-failing-tests.prompt.md

    # Documentation
    create-readme.prompt.md
    generate-api-docs.prompt.md
    generate-learning-notes.prompt.md
    generate-adr.prompt.md

    # Review
    architecture-review.prompt.md
    security-review.prompt.md
    performance-review.prompt.md

    # CI/CD and Ops
    create-github-action.prompt.md
    fix-github-action.prompt.md

    # PR workflow
    write-pr-description.prompt.md
    review-pull-request.prompt.md
    generate-release-notes.prompt.md

    # Planning
    create-feature-plan.prompt.md
    codebase-onboarding.prompt.md
    system-design-helper.prompt.md
```

### Naming Convention

```
Pattern: <verb>-<noun>.prompt.md

Verbs: generate, create, explain, debug, refactor, review, fix, write, analyze

Examples:
  generate-tests.prompt.md
  explain-code.prompt.md
  review-security.prompt.md
  debug-error.prompt.md
  create-github-action.prompt.md

NOT:
  tests.prompt.md           (no verb — unclear)
  copilot-tests.prompt.md   (redundant prefix)
  my-prompt.prompt.md       (non-descriptive)
```

---

## 6. Prompt Quality Principles

### Principle 1 — Be the Expert Who Is Asking

```
Bad prompt that doesn't specify constraints:
  "Generate tests for this code"
  → Copilot writes whatever tests it thinks are appropriate

Good prompt that encodes expertise:
  "Generate pytest tests covering happy path, duplicate key error,
  missing required field, and None input. Mock the database session.
  Test names: test_<function>_<scenario>_<expected>. One assertion per test."
  → Copilot follows your testing conventions, not its defaults
```

### Principle 2 — Specify Output Format Explicitly

```
Without format guidance, Copilot may:
  - Give a wall of prose when you wanted code
  - Give code when you wanted a summary
  - Use markdown when you wanted plain text

Always include:
  Output format:
  - [numbered list / bullet points / code block / prose / table]
  - [maximum length if important]
  - [specific sections if it's a structured document]
```

### Principle 3 — Include Constraint Negatives

```
Equally important to "do X" is "do NOT do Y":

  Do NOT add new dependencies
  Do NOT change the public API
  Do NOT suggest a different framework
  Do NOT add TODO comments — either implement or skip
  Do NOT generate placeholder functions — if you can't implement it, say so
```

### Principle 4 — Version and Iterate

```
Treat prompt files like code:
  - Start with a working v1
  - Use it 5-10 times; note where it produces suboptimal output
  - Improve the constraint or format specification
  - Commit with a descriptive message: "improve generate-tests prompt to require mocking"

A prompt file used 100 times with a small improvement per 10 uses
compounds into dramatically better output quality over time.
```

---

## 7. Revision Checklist

- [ ] Can describe what a prompt file is and where it lives
- [ ] Can create a prompt file with correct YAML frontmatter
- [ ] Knows the template variables: `${selection}`, `${input:...}`, `${file}`
- [ ] Has created at least 3 prompt files: explain-code, generate-tests, debug-error
- [ ] Knows how to verify prompt files appear as slash commands (type / in Chat)
- [ ] Applies prompt quality principles: format, negatives, constraints
- [ ] Has an organized `.github/prompts/` directory structure
- [ ] Treats prompt files as version-controlled, iterable assets
