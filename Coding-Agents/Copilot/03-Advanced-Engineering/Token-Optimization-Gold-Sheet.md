# Token Optimization — Gold Sheet

> **Track**: Copilot Mastery Track — Group 3: Advanced Engineering
> **File**: 4 of 7 (Track File #17)
> **Audience**: Developers who want to use Copilot efficiently without waste
> **Read after**: Context-Engineering-Gold-Sheet.md

---

## 1. Practical Impact Meter

| Topic | Impact | Why Developers Skip This |
|---|---|---|
| Token-efficient prompt patterns | ★★★★★ | Verbose prompts produce longer responses that are harder to review |
| Model selection for task complexity | ★★★★★ | Using o1 for a simple docstring wastes premium quota |
| Ask for diffs not full rewrites | ★★★★★ | Full rewrites are hard to review; diffs are fast to evaluate |
| Compact context — summarize before asking | ★★★★☆ | Pasting 500 lines when you need 20 is the most common waste |
| Plan-first, code-second — saves regen cycles | ★★★★☆ | Wrong direction = full regen; plan first = one correct pass |
| Ask Copilot to list assumptions first | ★★★★☆ | Assumptions surface misunderstandings before they become bad code |
| Reuse over regen — point to existing patterns | ★★★★☆ | "Follow this pattern" + one example = better than full regeneration |

---

## 2. The Token Mindset

### Must Know

```
Tokens ≈ characters / 3.5 (rough approximation)

Why tokens matter:
  1. Context window limits: too many tokens → context gets truncated → worse output
  2. Response cost: more tokens in = more tokens allowed out (generally)
  3. Speed: more tokens to process = slower response
  4. Quota: some plans have monthly limits on premium model usage

What costs tokens:
  ✗ Open files you didn't use in the response
  ✗ Long conversation history accumulating
  ✗ Verbose prompts that could be half as long
  ✗ Asking for a full file rewrite when only 5 lines need to change
  ✗ Providing the full stack trace when only the relevant lines matter
  ✗ Using a large model for a simple factual question

What saves tokens:
  ✓ Precise file selection (only what's needed)
  ✓ Focused prompts (state the goal, constraints, and output format concisely)
  ✓ Ask for diffs not rewrites
  ✓ Use smaller/faster models for simple tasks
  ✓ Custom instructions so you never repeat project context
  ✓ Start new conversations instead of accumulating context
```

---

## 3. Model Selection Strategy

```
Simple questions / facts / quick explanations:
  → Use fastest available model (Claude Haiku, GPT-4.1 mini, etc.)
  → Examples: "What is the syntax for Python match/case?"

Standard code generation (CRUD, boilerplate, standard patterns):
  → Use balanced model (GPT-4.1, Claude Sonnet)
  → Examples: "Generate a FastAPI router for user management"

Complex reasoning / architecture / security review:
  → Use strongest model (o1, o3, Claude Sonnet with extended thinking)
  → Examples: "What is the best database schema for a multi-tenant SaaS?"

DSA / algorithm problems:
  → Use reasoning models (o1, o3)
  → Examples: "Find the optimal algorithm for finding the k-th largest element"

Code review / nuanced analysis:
  → Claude Sonnet (strong at nuance and explaining problems)
  → Examples: "Review this function for security vulnerabilities"

Rule: Match model capability to task complexity.
      Using o1 for "explain a for loop" is like using a forklift to move a pencil.
```

---

## 4. Token-Efficient Prompt Patterns

### Pattern 1 — Concise Goal Statement

```
VERBOSE (wastes tokens):
"I have a Python function that processes user data and I need you to help me
make it better. Currently it takes a list and processes each item and I think
there might be a way to make it faster and also I wanted to ask about whether
we should add type hints or not."

COMPACT (same information):
"Optimize #selection for performance.
Add type hints if missing.
Show diff only — not the full file."
```

### Pattern 2 — Ask for Diff Not Full Rewrite

```
VERBOSE output request:
"Please rewrite this function with the improvements"
→ Copilot returns: full 80-line function, imports, class definition...

COMPACT output request:
"Show only the changed lines as a unified diff.
Do not show unchanged code."
→ Copilot returns: 12 lines showing what changed and why
```

### Pattern 3 — Reference Existing Pattern

```
VERBOSE (explain everything):
"Write a repository class for the Order model. It should follow the pattern
we use across the project with async session injection and type hints and
should have methods for find_by_id, find_all, create, update, and delete..."

COMPACT (reference the pattern):
"Write a Repository class for Order following the EXACT same pattern as
#file:src/repositories/user_repository.py.
Methods needed: find_by_id, find_all, create, update, delete."
→ Copilot infers all conventions from the example. You wrote 3 lines not 8.
```

### Pattern 4 — List Assumptions First

```
Before generating code for a complex task:

"Before implementing, list your assumptions about:
1. What the input format is
2. What the output format is
3. Any edge cases you're assuming are handled elsewhere
4. Any architecture decisions you're making

Don't generate code yet. Just list the assumptions."

→ Review assumptions → correct any wrong ones → then ask for implementation
→ One correct pass instead of a wrong pass + regen
```

### Pattern 5 — Chunk Large Tasks

```
Instead of: "Build the complete user management system with auth, profiles,
             notifications, and admin panel"
→ Too broad, Copilot makes wrong assumptions, produces a mess

Do this:
  Session 1: "Design only the user auth endpoints and Pydantic schemas. No implementation."
  → Review and approve the design
  
  Session 2: "Implement the auth endpoints from the design. Tests required."
  → Review and run tests
  
  Session 3: "Add user profile management. Follow the auth pattern exactly."
  → Review and run tests
  
Each session: focused, reviewable, correctable. Total quality: much higher.
```

---

## 5. Token-Efficient Prompts For Common Tasks

### Debugging

```
VERBOSE:
"I have this error: [pastes 200 lines of log output including irrelevant info]
My code does many things. Here's the full file: [pastes 500 lines]
Can you help me understand why there might be a problem?"

COMPACT:
"Error: [paste ONLY the relevant error + 3-5 lines of stack trace]
Relevant code: #selection [select only the failing function]
Root cause and fix."
```

### Refactoring

```
VERBOSE:
"Refactor this code to be better and follow good practices and make it
more maintainable and readable and also add better error handling..."

COMPACT:
"Refactor #selection:
Goal: extract payment validation into a separate method
Constraint: keep public signature identical, tests must still pass
Output: unified diff only"
```

### Architecture Review

```
VERBOSE:
"I want you to look at my whole codebase and tell me everything that is wrong
with it and what I should change and whether the architecture is good or not..."

COMPACT:
"Review #file:src/services/user_service.py for:
1. SOLID violations
2. Testability issues
3. Top 3 changes with highest impact

Format: prioritized table, 2-3 sentences per item. Under 300 words total."
```

### Codebase Onboarding

```
VERBOSE:
"I just joined this project. Can you read through all the files and tell me
everything about how it works?"

COMPACT:
"Using #codebase, give me a 5-bullet architecture summary:
1. What this system does
2. Request flow from API to DB (one sentence)
3. Key models/entities
4. Testing strategy
5. Biggest code quality concern

Under 250 words."
```

---

## 6. What NOT to Do — Token Waste Patterns

```
Waste pattern 1 — The daily recap:
  "Good morning! I'm working on the FastAPI project. It uses PostgreSQL..."
  Fix: Put this in copilot-instructions.md — it loads automatically.

Waste pattern 2 — Full file paste for one-line question:
  [pastes 300-line file] "What does line 147 do?"
  Fix: Select only the relevant function → #selection

Waste pattern 3 — Asking for a full response when you need one thing:
  "Explain the entire async SQLAlchemy lifecycle"
  Fix: "Explain the expire_on_commit=False setting. One paragraph."

Waste pattern 4 — Multiple questions in one message for unrelated topics:
  "Why does this function fail AND what is the best caching strategy AND
  also can you explain what SOLID means AND can you review this PR..."
  Fix: One question per conversation. Start new chats for new topics.

Waste pattern 5 — Not reading the response before asking follow-up:
  [Copilot answers completely] → "Can you explain more?"
  Fix: Read the full response first. Only ask follow-up for genuinely unclear parts.
```

---

## 7. High-Signal vs Low-Signal Prompts — The Full Taxonomy

### What Makes a Prompt High-Signal

```
A high-signal prompt has five properties:

1. ANCHORED      → references specific code with #file, #selection, or #sym
                   (not a description of what the code does)
2. BOUNDED       → defines the scope of what to change or analyze
                   ("only this function", "only these 2 files")
3. CONSTRAINED   → tells Copilot what must NOT change
                   ("keep public API identical", "tests must still pass")
4. FORMATTED     → specifies the expected output format
                   ("unified diff only", "table with 3 columns", "under 200 words")
5. PURPOSEFUL    → states one goal, not a vague direction
                   ("extract email validation into EmailValidator class")
                   vs ("make this better")
```

### High-Signal Pattern Catalogue

```
# Pattern: Anchor + Bound + Constrain + Format
"Refactor #selection to extract the retry logic into a separate RetryPolicy class.
Keep UserService.call_external_api() signature unchanged.
Show only the diff — not the full files."

# Pattern: Reference instead of describe
"Follow the exact pattern in #file:src/repositories/user_repo.py
to create a new repository for Order. Methods: find_by_id, create, update."
→ 2 lines vs 10 lines of explaining your conventions

# Pattern: Assumption-first (for complex tasks)
"List your assumptions about the input/output format and any architecture decisions
you will make. Do NOT generate code yet."
→ Surfaces wrong assumptions before they become wrong code

# Pattern: Output-format-first (for structured output)
"Format: | Issue | Severity | File | Fix | (no prose, no preamble)
Review #file:src/api/auth.py for security issues."
→ Table output, no padding text

# Pattern: Scope + goal + anti-goal (for edits)
"In #file:src/services/payment_service.py:
Goal: make process_refund() testable without a real Stripe client
Do NOT: change the public signature, add new dependencies, create base classes"

# Pattern: Compact follow-up
After a good Copilot response:
"Apply fix 2 to #selection. Diff only."
→ 6 words, not a full re-explanation of the context
```

### Low-Signal Anti-Pattern Catalogue

```
Anti-pattern 1 — The description instead of reference:
  LOW:  "I have a FastAPI service that uses SQLAlchemy 2.x with async sessions
         and Pydantic v2 for validation. The service has a layered architecture..."
  HIGH: #file:src/services/user_service.py

Anti-pattern 2 — The everything-at-once:
  LOW:  "Can you help me with testing, and also maybe look at the error handling,
         and should we be using async here, and what about the schema?"
  HIGH: One question per conversation. Pick the most important one.

Anti-pattern 3 — The open scope:
  LOW:  "Improve this file"
  HIGH: "Improve #selection: extract the nested if-else into a separate validator.
         Keep public signature. Diff only."

Anti-pattern 4 — The courtesy preamble:
  LOW:  "Hi! I'm working on a project and I'm running into an issue. 
         The context is that I'm using..."
  HIGH: "Error: [3 lines]. Code: #selection. Root cause."

Anti-pattern 5 — The format-free request:
  LOW:  "Review this code"
  HIGH: "Review #selection. Format: bulleted list, severity label, one-line fix per item."

Anti-pattern 6 — The memory assumption:
  LOW:  "Remember that function we discussed earlier? Add tests for it."
        (Copilot doesn't remember — new conversation = blank slate)
  HIGH: Select the function → "#selection — generate tests using pytest.
         Cover: happy path, ValueError, None input."

Anti-pattern 7 — The over-elaborated constraint:
  LOW:  "When you generate the code please make sure you don't change anything
         that is already working and try to keep things simple and follow
         the existing conventions and don't add new imports unless necessary..."
  HIGH: "Keep: public API, existing tests pass. No new dependencies."
```

---

## 8. Scoping Files Before Agent Mode

### Why Scoping Matters

```
Agent Mode has read access to your entire workspace.
Without explicit scoping, it will:
  - Read files it doesn't need (wastes context on noise)
  - Modify files you didn't intend (scope creep)
  - Make architectural decisions based on incomplete understanding
  - Produce inconsistent output because it encountered conflicting patterns

With explicit scoping:
  - Agent Mode reads only the relevant files
  - Output matches your actual patterns
  - Changes are bounded and reviewable
  - Context window is used for the code that matters
```

### The File Scoping Protocol

```
Before any Agent Mode session, answer these three questions:

1. READS: Which files does Copilot NEED to read to do this task correctly?
   (These are pattern files, interface files, schema files)

2. WRITES: Which files will be created or modified?
   (These go in the explicit working set for Edits mode)

3. FORBIDDEN: Which files must NOT be touched?
   (List these explicitly in the prompt constraints)

Template:
"Context files to read (do not modify):
  #file:src/repositories/user_repo.py    ← pattern to follow
  #file:src/schemas/user.py              ← schema pattern to match

Files to create or modify:
  src/repositories/order_repo.py         ← new file
  src/schemas/order.py                   ← new file

Do NOT touch:
  src/api/                               ← not part of this task
  tests/                                 ← no test changes this session
  alembic/                               ← no migrations"
```

### Scoping by Task Type

```
Task: "Add a new repository class"
  READ:  one existing repository (pattern reference)
  WRITE: new repository file + optionally its test file
  SKIP:  api layers, schemas, migrations, unrelated modules

Task: "Fix a bug in one function"
  READ:  the file containing the bug + any direct callers
  WRITE: only the file containing the bug
  SKIP:  entire rest of codebase

Task: "Refactor a service class"
  READ:  the service file + its tests + one example of the target pattern
  WRITE: the service file + its tests
  SKIP:  other services, API layer, infrastructure

Task: "Add a new feature end to end"
  READ:  one existing similar feature (all its layers) + shared schemas/models
  WRITE: new files for each layer (router, service, repository, schema, test)
  SKIP:  existing features, infrastructure code, CI config

Scope violation symptoms:
  - Agent Mode edits a test to make it pass instead of fixing the implementation
  - Agent Mode adds imports from modules you didn't plan to use
  - Agent Mode proposes a structural change you didn't ask for
  - Context window fills before Copilot finishes the task

When you see scope violation: Stop → add explicit FORBIDDEN list → restart
```

---

## 9. Plan-First Patterns — Deep Coverage

### Why Planning Saves Net Tokens

```
Common misconception: "Asking for a plan is an extra round-trip, so it costs more."

Reality:
  Wrong implementation = full regen (2x or 3x the tokens)
  Wrong implementation that you accepted = debugging + regen (5x the tokens)
  Correct plan → correct implementation = 1x tokens

Plan-first is cheaper overall because:
  - Copilot's failure mode is going in the wrong direction, not being slow
  - A wrong implementation that compiles and runs is harder to detect than a wrong plan
  - Correcting a plan: 2-3 sentences
  - Correcting an implementation: regen + diff + test cycle
```

### The Plan-First Prompt Templates

```
Template 1 — Feature implementation plan:
"Plan for implementing [feature].
List:
1. Files to create (with one-sentence purpose)
2. Files to modify (with what changes)
3. Assumptions you are making
4. Dependencies between the files (build order)
Do NOT write any code. Wait for my approval."

Template 2 — Refactoring plan:
"Plan to refactor #selection.
Goal: [one sentence]
List:
1. What changes and why
2. What stays the same (must be preserved)
3. Test impact: which tests will need updating?
4. Risk: what could break that isn't obviously affected?
No code yet."

Template 3 — Debug investigation plan:
"Plan for debugging this error:
Error: [paste error]
Code: #selection
List:
1. Most likely root cause (ranked)
2. What evidence confirms or rules out each cause
3. What you'd check first
Do not suggest a fix yet — just the investigation plan."

Template 4 — Architectural decision plan:
"Plan for adding [capability] to this system.
Context: #codebase summary (or #file:key-files)
List:
1. Three architectural approaches with trade-offs
2. Which approach you recommend and why
3. What you'd need to know to change the recommendation
This is for discussion, not implementation."
```

### Prompt That Forces Planning Before Edits

```
For Edits mode:
"Before making any changes, describe in one sentence per file:
  - What you will change in each file
  - Why that change is necessary for the goal
Post the plan as a bulleted list. Then wait — I will reply 'proceed' when ready."

For Agent Mode:
"IMPORTANT: Do not create or modify any files until I say 'approved'.
Plan first:
  1. Files to create/modify (exact paths)
  2. One-sentence description of each change
  3. Build order (what must exist before what)
  4. Any assumption I need to confirm before you proceed."
```

---

## 10. Avoiding Full-Repo Rereads

### The Full-Repo-Reread Problem

```
Symptoms:
  - You start every Chat session by re-explaining the project
  - You re-paste the same 20-line architectural overview every time
  - You type the same constraints every session ("we use pytest, not unittest")
  - Agent Mode reads 40 files when only 3 are relevant

Why it happens:
  1. copilot-instructions.md is missing or not loaded
  2. No project context file maintained
  3. Prompts don't use #file references (rely on re-explanation instead)
  4. New Chat conversation opened without summarizing prior state

Each full-repo-reread costs:
  - Time waiting for #codebase to complete indexing
  - Context window filled with tangentially-relevant code
  - Attention diluted across irrelevant files
  - Inconsistent results because Copilot may select different files each time
```

### Prevention Systems

```
Prevention 1 — copilot-instructions.md (persistent context):
  What to put there: tech stack, architecture rules, forbidden patterns
  These load every conversation automatically.
  Cost: zero extra tokens per prompt after initial setup.

Prevention 2 — Project context file (session starter):
  .copilot-context.md — updated per sprint
  At session start: "Context: #file:.copilot-context.md. Now: [actual question]"
  Cost: ~150 tokens once per session instead of ~1000 tokens per prompt re-explaining.

Prevention 3 — Targeted file references:
  Instead of: "#codebase, how does auth work?"
  Use: "#file:src/services/auth_service.py — explain the token validation flow"
  Difference: #codebase indexes many files; #file reads exactly what you specified.

Prevention 4 — Resume pattern for multi-session tasks:
  "Resume: implementing [feature].
  Done: [list 3 things]. Files so far: #file:f1, #file:f2.
  Next: [next step]. Constraint: [key rule].
  Now: [specific action]."
  Cost: 50-80 tokens of targeted context vs 500+ tokens of re-explanation.

Prevention 5 — Prompt files carry permanent instruction context:
  A good /generate-tests prompt file embeds:
    - The testing framework
    - Naming conventions
    - Mock targets
    - Output format
  Every time you run /generate-tests, those instructions apply automatically.
  Cost: zero extra tokens per use after the prompt file is created.
```

### When to Use `#codebase` vs `#file`

```
Use #codebase when:
  - You don't know which file contains the relevant code
  - You're asking "does the project have X?" (existence check)
  - Architecture overview at the start of onboarding
  - Finding where a concept is implemented across multiple files

Use #file when:
  - You know exactly which file to analyze
  - You're making changes to a specific file
  - You want Copilot to follow a specific file's pattern
  - You're generating tests for a known implementation

Use #sym when:
  - You want to find usages of a specific function or class
  - Cross-file reference for a known symbol name
  - "What calls this function?" type questions

Cost comparison for "explain how auth works":
  #codebase version: reads ~15 files, uses 4000-8000 tokens of context
  #file:auth_service.py version: reads 1 file, uses 400-800 tokens of context
  → Use #file whenever you know which file is relevant
```

---

## 11. Forcing Concise Responses

### The Verbosity Problem

```
Copilot's default is to be thorough and explain its reasoning.
For learning: this is good.
For daily workflow: this is expensive — 400 words when you needed 40.

Symptoms of over-verbose output:
  - Code is correct on line 1, but surrounded by 15 lines of explanation you didn't ask for
  - Architecture review has 6 paragraphs when you wanted a table
  - Debug suggestion has 3 paragraphs of preamble before the actual fix
  - "Here is the corrected version of your code..." followed by the full file

Solutions: explicit length and format instructions.
```

### Conciseness Forcing Instructions

```
For code output:
  "Show the changed lines only. No surrounding context. No explanation."
  "Diff format: + for additions, - for removals. No prose."
  "One code block. No explanation unless I ask."

For analysis output:
  "Under 100 words."
  "Bullet list only. No prose paragraphs."
  "Table with columns: [col1] | [col2] | [col3]. No preamble."
  "One sentence per finding. No elaboration."

For review output:
  "Format: [SEVERITY] file:line — issue — fix. One line per finding."
  "List only CRITICAL and HIGH issues. Skip LOW and INFO."
  "Maximum 5 findings. Prioritize by impact."

For explanations:
  "ELI5 version. Under 50 words."
  "Explain like I know the language but not this library. One paragraph."
  "Skip the background. Just the mechanism."

For follow-up answers:
  "One sentence."
  "Yes or no, then one sentence of reason."
  "Code only."
```

### Conciseness Instructions in copilot-instructions.md

```markdown
## Response Style
- Code requests: show code first, explanation only if I ask
- Analysis requests: use tables or bullet lists, not prose paragraphs
- Under 200 words for most answers unless the question requires depth
- Do not restate the question before answering
- Do not say "Great question!" or "Certainly!" — start with the answer
- If the answer is short: keep it short. Don't pad.
```

---

## 12. Prompt Files as Instruction Carriers

### How Prompt Files Eliminate Repeated Context

```
Without a prompt file (ad-hoc prompt):
  Every time you generate tests, you type:
    "Generate tests using pytest-asyncio.
     Use AsyncMock for the db session.
     Cover: happy path, error cases, edge cases.
     Name: test_<function>_<scenario>_<expected>.
     Mock external HTTP with respx.
     Output: complete test file with imports."
  
  Cost: you re-type 50 words every time, and you sometimes forget constraints.

With /generate-tests prompt file:
  All those instructions are in the file.
  You type: "/generate-tests" and select the code.
  
  Cost: 2 words. Same quality. Same constraints. Every time.

Prompt files ARE compressed repeated context.
Every prompt file you create is debt paid once, benefit collected forever.
```

### What to Encode in Prompt Files

```
Always encode in prompt files (never retype):
  - Framework and library names ("use pytest-asyncio, not unittest")
  - Naming conventions ("test_<function>_<scenario>_<expected>")
  - Mock targets ("mock external HTTP with respx, db session with AsyncMock")
  - Output format ("complete test file with imports, not a snippet")
  - Constraint boilerplate ("keep public API, tests must still pass")

Never encode in prompt files (must be dynamic):
  - Specific file paths (use ${selection} or ${file} instead)
  - Specific function or class names
  - Version numbers that change frequently
  - Task-specific constraints

The template variable pattern:
  ${selection}                → the code you selected before running the command
  ${input:Your question here} → asks you for dynamic input at run time
  #file:...                   → referenced in the prompt body, not the frontmatter
```

### Prompt File Efficiency Metrics

```
Metric: tokens saved per use

/generate-tests prompt (~60 words of instructions):
  Used 200 times per year → saves 60 × 200 = 12,000 words of re-typing
  And removes 200 chances of forgetting a constraint

/security-review prompt (~80 words of structured checklist):
  Used 100 times per year → saves 8,000 words of re-typing
  And ensures you always check injection, auth, PII — not just what you remember

/write-pr-description prompt (~40 words of format):
  Used 500 times per year → saves 20,000 words of re-typing
  And enforces consistent PR format across all your projects

The prompt file ROI calculation:
  Time to write a prompt file: 15-30 minutes
  Time saved per use: 1-2 minutes
  Break-even: 15 uses
  Average prompt: used 50-200 times per year
  Net ROI: 10x-30x
```

---

## 13. Splitting Large Refactors

### Why "Refactor Everything" Fails

```
When you ask Copilot to refactor a large module in one pass:

1. Context window fills — Copilot reads all the code but may truncate before
   generating the output, producing an incomplete refactoring.
   
2. Pattern inconsistency — Copilot may apply different patterns to different
   parts of the same file, producing inconsistent results.
   
3. Untestable output — a 300-line refactoring diff is nearly impossible to review
   and nearly impossible to attribute a test failure to.

4. Rollback is all-or-nothing — if part of the refactoring is wrong,
   you can't keep the good parts without manually untangling the diff.

Rule: Any refactoring > 2 functions or > 50 lines of change → split it.
```

### The Refactoring Split Protocol

```
Step 1 — Inventory the refactoring:
"List all the changes needed to refactor #file:src/services/order_service.py
to use the repository pattern.
Format: bulleted list, one change per bullet, ordered by dependency.
Do NOT make any changes. List only."

Step 2 — Split into atomic steps:
Take the list and group into sessions:
  Session A: Move database queries from OrderService to OrderRepository
  Session B: Update OrderService to call OrderRepository (not DB directly)
  Session C: Update tests to mock OrderRepository instead of the DB session

Step 3 — Execute one session at a time:
  Execute Session A:
    "Implement change: extract all database queries from #file:order_service.py
    into a new file #file:order_repository.py.
    Do NOT change OrderService yet — it will still call the DB directly.
    After: both old tests and new repository tests must pass."
  → Review → run tests → commit
  
  Execute Session B:
    "Update #file:order_service.py to call OrderRepository instead of DB directly.
    Assume OrderRepository exists with these methods: [list from Step A].
    Do NOT change anything else."
  → Review → run tests → commit
  
  Execute Session C:
    "Update #file:tests/test_order_service.py to mock OrderRepository
    instead of AsyncSession.
    Do NOT change the test scenarios — only the mocking target."
  → Review → run tests → commit

Each commit: independent, testable, reviewable, revertable.
```

### Refactor Size Heuristics

```
Small (handle in one Edits session):
  - Single function extraction
  - Rename a class or variable consistently
  - Add type hints to one file
  - Replace one error handling pattern

Medium (2-3 sessions):
  - Extract a class from one module
  - Add a validation layer to an existing service
  - Convert sync code to async in one module

Large (plan first, then 4+ sessions):
  - Introduce a new architectural layer (repository pattern)
  - Migrate from one library to another (requests → httpx)
  - Apply a consistent pattern across many files (add logging everywhere)
  - Restructure a module's directory layout

Extra-large (multiple PRs, not a Copilot session):
  - Change the database ORM
  - Migrate authentication strategy
  - Restructure the entire project layout
  → Don't use Copilot for the orchestration of extra-large refactors.
    Use Copilot for the individual atomic changes within each PR.
```

---

## 14. Model Selection — Complete Decision Guide

### The Model Hierarchy (current as of 2026)

```
Reasoning-first models (o1, o3, Claude with extended thinking):
  Strength: multi-step reasoning, complex analysis, finding subtle bugs
  Weakness: slow, expensive — overkill for simple generation
  Use for: algorithm design, architecture trade-offs, security analysis,
            complex debugging where the cause is not obvious

Balanced models (GPT-4.1, Claude Sonnet, Gemini Pro):
  Strength: code generation, explanation, review — all-purpose
  Weakness: not as deep for pure reasoning tasks
  Use for: most daily coding tasks, test generation, refactoring,
            documentation, standard code review

Fast/cheap models (Claude Haiku, GPT-4.1 mini):
  Strength: extremely fast, good for simple structured tasks
  Weakness: misses nuance in complex analysis
  Use for: simple questions, commit messages, boilerplate generation,
            "what is X?" factual questions, quick explanations

Rule: Start with the fast model. If the output is wrong or shallow after one retry,
      switch to the balanced model. Only use reasoning models for genuinely hard problems.
```

### Task-to-Model Decision Matrix

| Task | Start With | Escalate To | Never Use |
|---|---|---|---|
| "What does this function do?" | Fast | — | Reasoning |
| Generate CRUD boilerplate | Fast/Balanced | — | Reasoning |
| Write a commit message | Fast | — | Reasoning |
| Add a docstring | Fast | — | Reasoning |
| Generate 10 unit tests | Balanced | — | — |
| Fix a simple bug (obvious cause) | Fast | Balanced | Reasoning |
| Debug a subtle async race condition | Balanced | Reasoning | Fast |
| Architecture trade-off analysis | Balanced | Reasoning | Fast |
| Security vulnerability review | Balanced | Reasoning | Fast |
| Algorithm design (DSA) | Reasoning | — | Fast |
| Complex refactoring plan | Balanced | Reasoning | Fast |
| Quick code review before PR | Balanced | — | — |
| Deep security audit | Reasoning | — | Fast |
| Generate a README | Fast | — | — |
| Explain Python GIL in depth | Balanced | — | Fast |

### When to Escalate — The 2-Strike Rule

```
Try the fast/balanced model first.
If the output is wrong or misses the key insight after 1 retry → escalate.

Signs you need a stronger model:
  - The suggested fix doesn't address the actual root cause
  - The analysis misses an obvious constraint you stated
  - The code compiles but has a subtle logical error
  - The security review missed an obvious injection vector
  - The architecture recommendation ignores a constraint you mentioned

Signs you're using too strong a model:
  - You're using reasoning model for commit messages
  - Every question uses o1 by default
  - You're using the strongest model for "explain a for loop"
  - Simple tasks take 30 seconds when they should take 3 seconds

Monthly quota strategy:
  Reserve reasoning model usage for: complex debugging, architecture decisions,
  security reviews, and multi-step algorithm problems.
  Use fast models for: everything else (commit messages, simple questions, boilerplate).
  Rough allocation: 80% fast/balanced, 20% reasoning.
```

---

## 15. Recovering from Context Waste

### Detecting Context Waste

```
Signs Copilot has burned context on irrelevant files:

1. Response references files you didn't ask about
   "Looking at UserController.java and OrderController.java and..."
   When you only needed UserController.

2. Response misses context you explicitly provided
   "I don't see any existing test patterns here..."
   When you attached the test file.

3. Response gives generic advice not specific to your codebase
   "You could use a repository pattern here..."
   When your codebase already uses the repository pattern.

4. Response takes 30+ seconds (context window is very large — model is reading a lot)

5. Response is incomplete — truncated mid-code block
   (Context window was used up before output could finish)
```

### Recovery Techniques

```
Recovery 1 — Start a fresh conversation:
  Cmd+L → new chat.
  Restate context precisely: only what's needed for THIS task.
  
  "New session. Task: [one sentence].
  Relevant files: #file:path1, #file:path2.
  Pattern to follow: #file:path3.
  Constraint: [key rule].
  Now: [specific ask]."

Recovery 2 — Name the irrelevant files explicitly:
  "Focus only on #file:src/services/payment_service.py.
  Do not read or reference any other file.
  My question: [question]."

Recovery 3 — Compress context with a summary:
  "Here is a 3-sentence summary of the project context:
  [summary]
  I am asking about: #selection.
  Question: [question]."
  
  Using a summary instead of #codebase uses ~100 tokens vs 4000-8000 tokens.

Recovery 4 — Pre-summarize large files before questions:
  "Summarize #file:large_complex_module.py in 5 bullets.
  Focus on: exported functions, dependencies, error handling approach."
  → Save the summary.
  → Use the summary as context for subsequent questions (not the full file).

Recovery 5 — Restart Agent Mode with an explicit file list:
  "Start over. Read ONLY these files:
  - #file:relevant1.py (pattern to follow)
  - #file:relevant2.py (interface to implement)
  Do NOT read any other file. Plan the changes first."

Recovery 6 — Switch to Edits mode when Agent Mode goes wide:
  If Agent Mode is reading 20 files when you need 3:
  → Stop Agent Mode
  → Switch to Edits mode (you control the working set explicitly)
  → Add ONLY the 3 relevant files to the working set
```

---

## 16. Complete Task Examples — Token-Optimized

### Task 1 — Debugging

```
BEFORE (context waste):
  Opens 5 files in tabs. Pastes the full error log (300 lines).
  "My service is broken, can you help debug it?"

AFTER (token-optimized):
  Opens only the failing file.
  Selects only the function that throws the error.
  New Chat conversation.
  
  "Error (line 87 only):
    AttributeError: 'NoneType' object has no attribute 'stripe_charge_id'
    File: payment_service.py, in process_refund, line 87
    order = await self.order_repo.get_order_for_refund(order_id)
    charge_id = order.stripe_charge_id   ← here

  Code: #selection [selected: process_refund function only]
  
  Root cause and fix. Under 100 words. Diff only."

What changed:
  - One file open (not 5)
  - 3 lines of error (not 300)
  - Specific question (not open-ended)
  - Length constraint
  - Output format specified
```

### Task 2 — Refactoring

```
BEFORE (context waste):
  "Refactor src/services/ to use the repository pattern"
  → Copilot reads 15 service files, makes inconsistent changes across all of them.

AFTER (token-optimized):

  Step 1 — Plan:
  "Plan to refactor #file:src/services/order_service.py to use repository pattern.
  Existing pattern example: #file:src/repositories/user_repo.py
  List only: files to create/modify + one-sentence change per file.
  No code yet."

  Step 2 — Execute first change:
  "Create #file:src/repositories/order_repo.py
  Extract: all db session.execute() calls from #file:src/services/order_service.py
  Pattern: follow #file:src/repositories/user_repo.py exactly
  Do NOT change order_service.py yet."

  Step 3 — Execute second change:
  "Update #file:src/services/order_service.py to use OrderRepository.
  Assume OrderRepository exists. Methods: [list from step 2].
  Keep all method signatures. Diff only."

  Step 4 — Tests:
  "Update #file:tests/unit/test_order_service.py
  Change: mock OrderRepository instead of AsyncSession
  Keep: all existing test scenarios unchanged"
```

### Task 3 — Codebase Onboarding

```
BEFORE (context waste):
  "#codebase — explain everything"
  → Reads 50 files, produces 2000-word response, most irrelevant.

AFTER (token-optimized):

  Step 1 — Architecture in 5 bullets:
  "Using #codebase, give me 5 bullets:
  1. What this does (1 sentence)
  2. Request flow: API → ... → DB (one step per arrow)
  3. Key domain models (max 5)
  4. Test strategy (1 sentence)
  5. Most important file I must read first
  Under 150 words."

  Step 2 — Targeted deep dive on what matters:
  [After reading that key file]
  "Explain #file:[key file] — specifically:
  How is X handled? What happens when Y fails?
  Under 200 words."

  Step 3 — Find the hotspot:
  "Which area of #codebase has the most complexity or most technical debt?
  Cite one specific file and one specific function. Under 50 words."
```

### Task 4 — Test Generation

```
BEFORE (context waste):
  "Write tests for this"
  → Copilot writes 2 happy-path-only tests with wrong framework.

AFTER (token-optimized):
  Run /generate-tests (prompt file carries all framework context) OR:
  
  "Tests for #selection:
  Framework: pytest + pytest-asyncio (@pytest.mark.asyncio)
  Mock: AsyncMock(spec=AsyncSession) for db, AsyncMock for email_service
  Cover: happy path, duplicate email (DuplicateEmailError), None email (TypeError), empty name (ValueError)
  Name: test_create_user_<scenario>_<expected>
  Output: complete file with imports"

  Token count: ~60 words vs 10 words with /generate-tests (if prompt file exists)
```

### Task 5 — Architecture Review

```
BEFORE (context waste):
  "Is my architecture good?"
  → Generic SOLID principles, no specifics.

AFTER (token-optimized):
  "Architecture review of #file:src/services/order_service.py.
  
  Evaluate ONLY:
  1. Single responsibility (does any method do > 1 thing?)
  2. Testability (what makes unit testing hard?)
  3. Coupling (what else breaks if this file changes?)
  
  Format: | Issue | Location | Consequence | Fix |
  Max 5 rows. Under 200 words total."
```

### Task 6 — PR Review

```
BEFORE (context waste):
  "#codebase review my PR"
  → Copilot reads unrelated files, gives generic advice.

AFTER (token-optimized):
  Step 1 — Security first (focused):
  "Security review of #file:[changed file 1] and #file:[changed file 2].
  Check: SQL injection, hardcoded creds, PII in logs, missing auth.
  Format: [SEVERITY] — [issue] — [fix]. One line each.
  CRITICAL issues first."
  
  Step 2 — Test coverage:
  "Test gap analysis:
  Implementation: #file:src/services/payment_service.py
  Tests: #file:tests/unit/test_payment_service.py
  List: untested functions, untested error paths. Prioritized."
  
  Step 3 — PR description:
  "PR description for changes in #file:[file1] and #file:[file2].
  What I changed: [2 sentences].
  Use template:
  ## Summary / ## Changes Made / ## How to Test / ## Breaking Changes
  Under 200 words."
```

### Task 7 — Personal Project Creation

```
BEFORE (context waste):
  "Create a new FastAPI project for me"
  → Copilot generates hello-world quality, no CI, no Copilot config, no tests.

AFTER (token-optimized):
  Run /bootstrap-project OR:
  
  "Bootstrap a Python FastAPI service.
  Name: user-management-api
  Stack: Python 3.12, FastAPI 0.115, SQLAlchemy 2.x async, asyncpg, Pydantic v2, pytest, Poetry
  
  Plan first (files + one-sentence purpose each). Wait for my approval.
  
  After approval, create in this order:
  1. pyproject.toml + .gitignore + .env.example
  2. src/ skeleton (main.py, api/, services/, repositories/, models/, schemas/, config.py)
  3. tests/conftest.py with AsyncSession fixture
  4. .github/copilot-instructions.md
  5. .github/workflows/ci.yml (ruff + mypy + pytest)
  6. README.md (setup + run + test — all commands copy-paste runnable)
  
  Quality bar: every file must be production-starting-point quality.
  No hello world. No placeholder functions."
```

### Task 8 — Learning Notes Generation

```
BEFORE (context waste):
  "Explain asyncio to me"
  → 2000-word essay with academic history of event loops.

AFTER (token-optimized):
  Run /generate-learning-notes OR:
  
  "Create revision notes on: Python asyncio for a developer who knows threading.
  
  Format exactly:
  ## What It Is (2 sentences)
  ## Why It Matters (why care vs threading?)
  ## How It Works (numbered steps: event loop → coroutine → await → resume)
  ## Key Rules (5 bullets — must-remember facts)
  ## Code Example (30 lines max — shows async def, await, gather, common mistake)
  ## Common Mistakes (3 — show bad pattern → correct pattern)
  ## Strong Explanation (how to explain this to a teammate in 3 sentences)
  ## Revision Questions (5 applied questions — not 'what is X?' but 'what happens when Y?')
  
  Under 600 words. Code must run. No academic history."
```

---

## 17. Revision Checklist

### Prompt Quality

- [ ] Every prompt is ANCHORED (uses #file, #selection, #sym — not a description)
- [ ] Every prompt is BOUNDED ("only this function", "only these 2 files")
- [ ] Every prompt is CONSTRAINED ("keep API identical", "no new dependencies")
- [ ] Every prompt specifies OUTPUT FORMAT ("diff only", "table", "under 200 words")
- [ ] Prompts are under 100 words for standard tasks
- [ ] Zero courtesy preamble ("Hi!", "I'm working on...", "Can you help me...")
- [ ] Never repeats project context that belongs in copilot-instructions.md

### Context Management

- [ ] Opens only relevant files before Chat sessions
- [ ] Uses #file instead of #codebase when the file is known
- [ ] Has a project context file (.copilot-context.md) for complex projects
- [ ] Uses the Resume pattern for multi-session tasks (not re-explaining from scratch)
- [ ] Starts a new conversation (Cmd+L) when topics change
- [ ] Detects context waste (generic responses, truncated output) and recovers fast

### Agent Mode

- [ ] Files scoped before every Agent Mode session (READ / WRITE / FORBIDDEN list)
- [ ] Plan required before any file changes ("Plan first, wait for approval")
- [ ] Checkpoint commit before every Agent Mode session
- [ ] Stops Agent Mode immediately on scope violation

### Model Selection

- [ ] Uses fast models for: questions, boilerplate, commit messages, simple fixes
- [ ] Uses balanced models for: generation, review, explanation, test writing
- [ ] Uses reasoning models for: complex debugging, architecture, algorithms, security audits
- [ ] Applies 2-strike rule before escalating (try fast → retry → escalate if still wrong)

### Refactoring

- [ ] No single-pass refactoring of > 50 lines
- [ ] Every refactoring is planned (list of changes) before implementation
- [ ] Refactoring split into atomic sessions (each independently testable and committable)
- [ ] Tests run after every session before moving to the next

### Prompt Files

- [ ] Has prompt files for every workflow typed more than 3 times per week
- [ ] Prompt files encode: framework, conventions, output format, constraints
- [ ] Prompt files use ${selection} not hardcoded file paths
- [ ] Prompt library is version-controlled and backed up

### Recovery

- [ ] Knows the 6 context-waste recovery techniques
- [ ] Knows when to switch from Agent Mode to Edits mode (scope creep)
- [ ] Can detect context waste from response quality signals
- [ ] Starts fresh conversation within 30 seconds of detecting a wasted context session
