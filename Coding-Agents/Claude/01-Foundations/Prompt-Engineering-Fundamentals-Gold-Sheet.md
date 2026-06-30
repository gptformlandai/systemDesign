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

## 9. XML Tags — Structured Reasoning for Complex Tasks

### Why XML Tags Work With Claude

```
Claude was trained with XML-tagged content. Using XML tags in prompts:
  - Clearly separates different parts of your instruction
  - Tells Claude EXACTLY what type of content is in each section
  - Forces Claude to reason through sections in the right order
  - Dramatically improves output for complex multi-part tasks

When to use XML tags:
  → Tasks with multiple distinct input types (code + error + context)
  → Tasks requiring structured step-by-step reasoning
  → Tasks where you want Claude to separate analysis from answer
  → Multi-document analysis
  → Long prompts where visual structure helps Claude parse intent
```

### Core XML Pattern for Code Tasks

```xml
<task>
Security review of the authentication service.
</task>

<code>
@file:src/api/auth.py
</code>

<requirements>
- Check: SQL injection, hardcoded credentials, missing auth checks, PII in logs
- Severity labels: CRITICAL, HIGH, MEDIUM, LOW
- Format: one finding per line: [SEVERITY] line:N — issue — fix
- CRITICAL first, skip INFO-level
</requirements>

<constraints>
- Under 250 words
- Code fixes shown inline, not in separate section
</constraints>
```

### The Reasoning Sandwich Pattern

```xml
<!-- For hard problems: think first, then answer -->
<problem>
Why is the payment webhook endpoint occasionally returning 500 errors
under load?
</problem>

<context>
@file:src/api/webhooks.py
Error rate: 2% at 100 req/s, 15% at 500 req/s
Logs show: database connection pool exhausted
</context>

<think>
Before answering, reason through:
1. What happens to connection pool under load?
2. What is the pool size vs concurrent requests?
3. What does "connection pool exhausted" look like in the code?
</think>

<answer>
Root cause and fix.
</answer>
```

### Document Analysis Pattern

```xml
<document_1>
@file:src/services/order_service.py
</document_1>

<document_2>
@file:src/repositories/order_repository.py
</document_2>

<task>
Compare the error handling approach across both files.
Which is more robust? Show concrete differences.
Format: comparison table.
</task>
```

---

## 10. Extended Thinking — Claude's Internal Reasoning

### What Extended Thinking Is

```
Extended thinking (available via API with "thinking" parameter and in some interfaces)
allows Claude to work through complex problems before producing an answer.

Claude uses a private "scratchpad" for reasoning, then produces a visible answer.

When to use it:
  → Complex multi-step debugging (race conditions, timing issues)
  → Architecture decisions with many trade-offs
  → Security analysis requiring adversarial thinking
  → Algorithm design problems
  → Any problem where you've gotten wrong answers from standard prompting

How to invoke in Claude Code:
  claude --extended-thinking "Debug this race condition: @file:src/worker.py"
  
Or in a prompt:
  "Think through this carefully before answering.
  Reason step by step. Show your reasoning, then give your conclusion."
```

### Simulating Extended Thinking Without API Access

```
Even without the extended thinking API:
1. Ask Claude to reason explicitly:

   "Before giving your final answer, work through:
   1. What are all the possible causes?
   2. What evidence supports or rules out each cause?
   3. What is the most likely cause?
   
   Then give your final diagnosis."

2. Use the scratchpad pattern:
   "First, reason through this problem for yourself — list your assumptions,
   possible approaches, and why each works or doesn't.
   Then show me only your recommended solution."

3. Ask for confidence levels:
   "Give me the answer, but also tell me:
   - How confident are you (0-100%)?
   - What would make you change your answer?
   - What are you uncertain about?"
```

---

## 11. Chain-of-Thought — Making Reasoning Explicit

### When Chain-of-Thought Helps

```
Chain-of-thought (COT) prompting forces Claude to reason step by step.
This catches errors that appear in "intuitive" answers.

Use COT for:
  - Logic-heavy debugging (not obvious root cause)
  - Security analysis (easy to miss attack vectors)
  - Architecture decisions (many trade-offs)
  - Complex business logic implementation

Don't use COT for:
  - Simple code generation (adds overhead)
  - Factual questions
  - Formatting/restructuring tasks
```

### COT Templates

```
Template 1 — Step-by-step analysis:
"Before giving your answer, list:
1. All possible root causes for this error
2. Evidence that supports or rules out each cause
3. Your conclusion

Then give: root cause (1 sentence) + fix (diff)"

Template 2 — Decision analysis:
"Before recommending, analyze:
Option A: [pros, cons, risks]
Option B: [pros, cons, risks]
Option C: [pros, cons, risks]
Decision criteria: [list the most important factors]

Then give: your recommendation and the 2 most important reasons."

Template 3 — Security analysis:
"Analyze this code for security issues by working through each attack vector:
1. Can user input reach this code without sanitization? [yes/no + evidence]
2. Are there SQL operations? [yes/no — show which ones]
3. Is authentication checked before business logic? [yes/no + location]
4. Is any sensitive data logged? [yes/no + location]

Then show findings table."
```

### Anti-Pattern: COT Without Constraint

```
BAD: "Think step by step about how to improve this code"
→ Claude reasons for 600 words, concludes with generic advice

GOOD: "Reason through exactly 3 options for solving this caching problem.
For each option: 2 pros, 2 cons, effort estimate (S/M/L).
Then recommend one option in 1 sentence."
→ Structured reasoning, bounded output, actionable conclusion
```

---

## 12. Prefilling — Control Where Claude Starts

### What Prefilling Is

```
Prefilling = providing the START of Claude's response in your prompt.
Claude then completes the response starting from where you left off.

Why it's powerful:
  - Prevents Claude from changing the format mid-response
  - Forces Claude to begin with code, not preamble
  - Ensures output is parseable by downstream processes
  - Eliminates "Certainly! I'd be happy to help..." preamble

How to use in Claude Code:
  End your prompt with the beginning of Claude's intended response.
  Claude will continue from there.
```

### Prefilling Examples

```python
# Example 1: Force code-only output
"Generate the process_refund() function for payment_service.py:
  - Takes order_id: int, amount: Decimal, reason: str
  - Returns RefundResult dataclass
  - Raises InsufficientFundsError if amount > order.total
  - Uses asyncpg session injection

```python
async def process_refund(session: AsyncSession, order_id: int, amount: Decimal, reason: str) -> RefundResult:"
# Claude continues from this exact line — no preamble

# Example 2: Force structured analysis
"Security analysis of @file:auth.py:

| Severity | Line | Vulnerability | Fix |
|----------|------|---------------|-----|"
# Claude fills in the table rows directly

# Example 3: Force ADR format
"Write an ADR for using JWT stateless tokens:

# ADR-042: JWT Stateless Authentication
Date: 2024-01-15
Status: Accepted

## Context"
# Claude fills in the section content
```

---

## 13. Output Parsing — Reliable Structured Data

### The Parsing Problem

```
If you need to programmatically process Claude's output:
  - Unstructured prose is impossible to parse reliably
  - Markdown has inconsistent formatting
  - Tables are fragile to parse

Solutions: JSON output, XML output, or strict delimiters.
```

### JSON Output Pattern

```python
# Prompt:
"Analyze the security issues in @file:auth.py.
Return ONLY a JSON array — no explanation, no markdown, no prose:
[
  {
    \"severity\": \"CRITICAL|HIGH|MEDIUM|LOW\",
    \"line\": <line_number>,
    \"issue\": \"<description>\",
    \"fix\": \"<code change>\"
  }
]"

# Parsing in Python:
import json
import subprocess
result = subprocess.run(['claude', '--print', prompt], capture_output=True, text=True)
findings = json.loads(result.stdout)
for f in findings:
    if f['severity'] in ('CRITICAL', 'HIGH'):
        print(f"[{f['severity']}] Line {f['line']}: {f['issue']}")
```

### Delimiter-Based Parsing

```python
# Prompt:
"Generate 3 test function names for user_service.py's create_user method.
Return exactly 3 names, one per line, between <names> tags:
<names>
test_name_1
test_name_2
test_name_3
</names>"

# Parsing:
import re
output = run_claude(prompt)
match = re.search(r'<names>(.*?)</names>', output, re.DOTALL)
names = [n.strip() for n in match.group(1).strip().split('\n')]
```

### Validation Pattern

```python
# Always validate parsed output:
def parse_severity(data: dict) -> str:
    allowed = {'CRITICAL', 'HIGH', 'MEDIUM', 'LOW'}
    sev = data.get('severity', '').upper()
    if sev not in allowed:
        raise ValueError(f"Invalid severity: {sev}")
    return sev

# Never trust Claude output in security-sensitive contexts without validation
```

---

## 14. Self-Critique Pattern — Constitutional Prompting

### What Self-Critique Is

```
Ask Claude to generate code, then critique it, then improve it.
This surfaces problems Claude wouldn't mention in a single pass.

When to use:
  - Security-sensitive code
  - Complex business logic
  - Public APIs

The pattern: Generate → Critique → Revise
```

### Self-Critique Template

```
"Generate a payment processing function that charges a credit card via Stripe.

After generating, critique your implementation against these principles:
1. Does this handle all error cases (network timeout, invalid card, insufficient funds)?
2. Is any sensitive data logged?
3. Are there any race conditions if called concurrently?
4. Does this fail safely if Stripe is unavailable?

Then revise the function to address any issues you found.

Output:
1. Initial implementation
2. Critique (what's wrong with it)
3. Revised implementation (addressing each critique point)"
```

---

## 15. Revision Checklist

### Foundation Techniques (Sections 2-8)
- [ ] Can write a prompt with all 5 CRISP elements
- [ ] Uses bad vs good pattern knowledge for: code gen, debugging, review, refactor, learning
- [ ] Always specifies output format in every prompt
- [ ] Uses role prompting for specialist reviews
- [ ] Uses multi-shot examples instead of 200-word instruction lists
- [ ] Uses "Do NOT" constraints to prevent common wrong turns
- [ ] Has all 4 prompt templates saved for daily reuse

### Advanced Techniques (Sections 9-14)
- [ ] Uses XML tags for multi-part tasks and complex instructions
- [ ] Knows when to use extended thinking (complex debugging, architecture, security)
- [ ] Uses COT templates for logic-heavy problems
- [ ] Applies prefilling to force specific output format (no preamble)
- [ ] Can parse JSON and delimiter-based Claude output programmatically
- [ ] Uses self-critique pattern for security-sensitive code generation
- [ ] Knows which advanced technique to apply for which task type
