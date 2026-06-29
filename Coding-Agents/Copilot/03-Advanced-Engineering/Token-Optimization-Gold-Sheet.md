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

## 7. Revision Checklist

- [ ] Matches model complexity to task complexity (not always using the strongest model)
- [ ] Asks for diffs not full rewrites for targeted changes
- [ ] References existing patterns with a single example file instead of explaining conventions
- [ ] Lists assumptions before implementation for complex tasks
- [ ] Chunks large tasks into focused sessions
- [ ] Keeps prompts under 100 words for most standard tasks
- [ ] Uses copilot-instructions.md to avoid repeating project context
- [ ] Knows the 5 token waste patterns and can avoid them
