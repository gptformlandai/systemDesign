# Prompt Engineering Fundamentals — Gold Sheet

> **Track**: Claude Mastery Track — Group 1: Foundations
> **File**: 4 of 6 (Track File #4)
> **Audience**: Developers learning to write prompts that produce expert-level output
> **Read after**: Claude-Chat-Fundamentals-Gold-Sheet.md

---

## 1. Practical Impact Meter

| Topic | Impact | Why Developers Skip This |
|---|---|---|
| The CRISP prompt structure | ★★★★★ | Without structure, prompts are vague — Claude fills gaps incorrectly |
| Bad vs good prompt gallery | ★★★★★ | Seeing concrete examples is 10x more valuable than reading principles |
| Output format control | ★★★★★ | Uncontrolled output is verbose — you get 500 words when you needed 50 |
| Role and persona prompting | ★★★★☆ | Telling Claude "you are a senior security engineer" changes depth of response |
| Multi-shot prompting | ★★★★☆ | One example in the prompt is worth more than 200 words of instruction |
| Constraint-first prompting | ★★★★★ | What Claude must NOT do is as important as what it must do |

---

## 2. The CRISP Prompt Structure

```
C — Context:     What is the situation? What file/code/project is this about?
R — Role:        Who should Claude be? (optional but powerful)
I — Instruction: What exactly should Claude do?
S — Scope:       What is in bounds? What is out of bounds?
P — Produce:     What should the output look like?

Every high-quality prompt has all 5. Most bad prompts have only I.
```

### CRISP Applied

```
BAD (I only):
  "Fix the bug"

CRISP (all 5):
  Context:     "The process_payment function at line 45 of payment_service.py raises
                AttributeError: 'NoneType' has no attribute 'stripe_charge_id' when
                the order has no associated Stripe charge."
  Role:        "You are a Python backend engineer."
  Instruction: "Find the root cause and fix the AttributeError."
  Scope:       "Only modify payment_service.py. Do not change the Order model or any test."
  Produce:     "Show a unified diff. Under 50 words of explanation."

Output quality: night and day.
```

---

## 3. Bad vs Good Prompt Gallery

### Category: Code Generation

```
BAD: "Write a function to handle users"
  → Claude writes: a 200-line UserManager class with unnecessary abstraction.

GOOD: "Write a Python async function get_user_by_email(session: AsyncSession, email: str) -> User | None.
  Use SQLAlchemy 2.x: select(User).where(User.email == email).
  Return None if not found. No try/except — let exceptions propagate.
  Type hints required. Under 15 lines."
  → Claude writes exactly that function, no extras.
```

### Category: Debugging

```
BAD: "My code doesn't work, help"
  → Claude asks 5 clarifying questions.

GOOD: "Error: TypeError: object NoneType can't be used in await expression
  Line 87, payment_service.py, process_refund()
  Code: @file:src/services/payment_service.py [select process_refund function]
  Root cause only. One sentence. Fix as unified diff."
  → Claude identifies the root cause immediately and shows the fix.
```

### Category: Code Review

```
BAD: "Review this code"
  → Claude writes 800 words covering style, formatting, and vague "best practices".

GOOD: "Security review of @file:src/api/auth.py.
  Check only: SQL injection, hardcoded credentials, missing auth checks, PII in logs.
  Format: [SEVERITY] — [line] — [issue] — [fix]. One line each.
  CRITICAL first. Skip LOW. Under 200 words."
  → Structured, severity-ranked, actionable findings.
```

### Category: Refactoring

```
BAD: "Refactor this to be cleaner and follow SOLID principles"
  → Claude rewrites half the codebase, adds 3 new base classes.

GOOD: "Refactor @file:src/services/order_service.py:
  Goal: extract the price calculation into a PriceCalculator class
  Keep: OrderService public API identical, all existing tests pass
  Do NOT: create any other new classes, change any test files, add new dependencies
  Output: unified diff only"
  → Focused extraction, no scope creep.
```

### Category: Learning

```
BAD: "Explain async/await"
  → 2000-word essay starting with the history of concurrency.

GOOD: "Explain Python async/await for a developer who knows threading.
  Format exactly:
  ## What it is (2 sentences)
  ## How it differs from threading (3 bullet points)
  ## When to use it (2 sentences)
  ## Minimal code example (15 lines max)
  ## One common mistake (bad pattern → good pattern)
  Under 400 words."
  → Structured, targeted, efficient.
```

---

## 4. Output Format Control

### Why Format Control Matters

```
Without format control:
  Claude answers in whatever format it prefers that day.
  Code review: 3 paragraphs of prose
  Explanation: wall of text
  Architecture review: meandering essay

With format control:
  Code review: table with 4 columns
  Explanation: 3 bullet points
  Architecture review: prioritized list with severity labels

Format instructions are not style preferences — they are functional requirements.
A table with 4 columns is 3x faster to scan than 4 paragraphs of prose.
```

### Format Control Instructions

```
For code output:
  "Output: unified diff only"
  "Output: one code block, no explanation"
  "Output: complete function with imports"

For analysis:
  "Format: | Issue | Severity | Fix | (table, no prose)"
  "Format: numbered list. One sentence per item."
  "Format: prioritized table, HIGH issues first"

For reviews:
  "Format: [SEVERITY] — [location] — [issue] — [fix]"
  "Format: bulleted list. Under 10 items."

For explanations:
  "Under 200 words"
  "3 bullet points max"
  "One paragraph"
  "ELI5 (explain like I'm 5)"

For comparisons:
  "Format: comparison table with columns: [aspect] | [option A] | [option B]"
```

---

## 5. Role and Persona Prompting

```
Telling Claude what role to play calibrates the depth and style of its response.

"Review this code" → general review at medium depth

"You are a senior security engineer reviewing an authentication flow.
Review this code." → security-first, deep, adversarial thinking

"You are a Python performance engineer with expertise in async Python.
Analyze why this endpoint is slow." → profiling mindset, async-specific

"You are a senior staff engineer conducting a design review.
What are the top 3 architectural concerns with this approach?" → systematic, opinionated

Role prompting is most valuable for:
  - Reviews that require specialist mindset
  - Learning explanations (adjust for audience)
  - Code generation that needs domain expertise
  - Trade-off analysis

Don't overuse it: for simple code generation, role prompting adds overhead.
```

---

## 6. Multi-Shot Prompting

### One Example Is Worth 200 Words of Instruction

```
BAD: "Write test functions. Use descriptive names. Cover edge cases. Use pytest.
  Mock external dependencies. One assertion per test. Group related tests in classes."
  [200 words of instructions]

GOOD: "Write tests following this pattern:
  [paste one example test from your codebase]
  Now write tests for: [target function]"
  [30 words + 15-line example]

Why multi-shot works:
  Examples show format, style, naming, and patterns simultaneously.
  Instructions describe them — examples demonstrate them.
  Claude learns more from demonstration than from rules.
```

### Multi-Shot Template

```
"Generate [X] following the exact style of this example:

Example input: [sample]
Example output:
[your example]

Now generate [X] for: [your actual request]"
```

---

## 7. Constraint-First Prompting

### The Power of "Do NOT"

```
Constraint-first prompting explicitly prevents Claude's most common wrong turns.

Most powerful constraints:
  "Do NOT add new dependencies"    → prevents library sprawl
  "Do NOT change the public API"   → preserves backward compatibility
  "Do NOT add new abstractions"    → prevents over-engineering
  "Do NOT create new files"        → keeps scope bounded
  "Do NOT modify tests"            → protects test integrity
  "Do NOT add print() statements"  → prevents debug code in commits
  "Do NOT generate TODOs"          → forces complete implementation

Pattern:
  [Goal] + [constraints] = targeted output without unwanted additions

Example:
  "Add input validation to create_user().
  Do NOT change the function signature.
  Do NOT add a new validation class.
  Do NOT modify any test file.
  Do NOT add new imports beyond what's needed for validation.
  Output: unified diff only"
```

---

## 8. Prompt Templates for Daily Use

### Template: Focused Code Change

```
[What]: [one sentence description of the change]
[Where]: @file:[path]
[Pattern]: Follow the pattern in @file:[example file]
[Preserve]: [what must not change]
[Avoid]: [common wrong moves for this task]
[Output]: unified diff only
```

### Template: Root Cause Debug

```
Error: [paste exact error — 3-5 lines of stack trace]
Code: @file:[failing file] — [function name]
Expected: [what it should do]
Actual: [what it does]
Already tried: [what you've ruled out]
Answer: root cause in one sentence + fix as diff
```

### Template: Architecture Question

```
You are a [role: senior backend engineer / architect / etc].
Question: [specific question about the design]
Context: @file:[key files — 2-3 max]
Constraint: [any decisions already made that can't change]
Answer: comparison table OR numbered list. Under 300 words.
```

### Template: Learning Note Generation

```
Create revision notes on: [topic]
Target reader: a developer who knows [background] but not [this topic]

Format exactly:
## What It Is (2 sentences)
## Why It Matters (practical consequence of not knowing)
## How It Works (numbered steps)
## Key Rules (5 bullet points)
## Code Example (runnable, under 20 lines)
## Common Mistakes (bad → good pattern)
## Revision Questions (5 applied questions)
Under 500 words.
```

---

## 9. Revision Checklist

- [ ] Can write a prompt with all 5 CRISP elements
- [ ] Knows the bad vs good pattern for: code gen, debugging, review, refactor, learning
- [ ] Always specifies output format in every prompt
- [ ] Uses role prompting for specialist reviews and analysis
- [ ] Uses multi-shot examples instead of long instruction lists
- [ ] Uses "Do NOT" constraints to prevent common wrong turns
- [ ] Has saved all 4 prompt templates for daily reuse
