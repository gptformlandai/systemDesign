# Agent Mode — Safe Usage — Gold Sheet

> **Track**: Copilot Mastery Track — Group 2: Intermediate Power User
> **File**: 4 of 7 (Track File #10)
> **Audience**: Developers ready to use Agent Mode safely
> **Read after**: Copilot-Edits-Mode-Gold-Sheet.md

---

## 1. Practical Impact Meter

| Topic | Impact | Why Developers Miss This |
|---|---|---|
| What Agent Mode can and cannot do | ★★★★★ | Devs give Agent Mode impossible tasks and blame "Copilot doesn't work" |
| Require a plan before coding — always | ★★★★★ | Without a plan step, Agent Mode jumps to implementation and may go in the wrong direction |
| Commit before Agent Mode runs | ★★★★★ | This is the recovery net — skip it and bad sessions have no undo |
| Reviewing all changes before Accept | ★★★★★ | Agent Mode can touch 10+ files; reviewing all is non-negotiable |
| Stopping and redirecting a bad session | ★★★★☆ | Devs let a wrong session run to completion; stop it early and restart with better prompt |
| Agent Mode task template | ★★★★☆ | A consistent template for every Agent Mode task produces consistent quality |
| When NOT to use Agent Mode | ★★★★☆ | Over-using Agent Mode on simple tasks wastes tokens and time |

---

## 2. What Agent Mode Is

### Must Know

```
Agent Mode is Copilot acting as an autonomous agent to complete a multi-step task.

What it can do:
  ✓ Read and analyze any files in your workspace
  ✓ Create new files
  ✓ Modify existing files across your project
  ✓ Run terminal commands (with your permission)
  ✓ Run tests and iterate based on results
  ✓ Install packages (in some configurations)
  ✓ Make decisions about HOW to accomplish a goal
  ✓ Ask clarifying questions when it needs more information

What it cannot do:
  ✗ Access external systems without MCP tools configured
  ✗ Make network requests to APIs (unless via MCP)
  ✗ Remember previous sessions (no persistent memory)
  ✗ Guarantee correctness — it can make wrong architectural decisions
  ✗ Replace human judgment on complex trade-offs
  ✗ Run indefinitely without getting stuck

Critical difference from Edits mode:
  Edits: You specify what changes to make → Copilot makes them → you review
  Agent: You specify the GOAL → Copilot plans AND executes → you review
  
  Agent mode has more autonomy and more risk.
```

---

## 3. The Plan-First Requirement

### Always Require a Plan

```
Never let Agent Mode jump directly to implementation.

Wrong prompt (no plan requirement):
  "Build a user authentication system with JWT tokens in FastAPI"
  → Agent Mode starts creating files immediately, possibly in the wrong structure

Right prompt (plan required):
  "Plan: Before making any changes, describe:
  1. Which files you will create or modify
  2. The overall architecture approach
  3. Any assumptions you are making
  4. Any decisions where you need my input

  Then wait for my approval before making any changes.

  Task: Build a user authentication system with JWT tokens in FastAPI.
  Requirements:
  - Register with email + password (bcrypt hashing)
  - Login returns access token (JWT, 1 hour expiry) and refresh token (7 days)
  - Protected routes use Authorization: Bearer header
  - Use our existing User model in src/models/user.py
  - Tests required: registration, login, invalid credentials, expired token"
  
  → Agent Mode presents the plan → you approve, modify, or cancel
  → Then Agent Mode implements with your guidance
```

### Reviewing a Plan Before Approving

```
When Agent Mode presents a plan, check:
  □ Does it create files in the right directories (matching our structure)?
  □ Does it use our existing models/schemas/patterns?
  □ Does it mention testing?
  □ Are there any files it plans to modify that shouldn't be touched?
  □ Does the approach match our architectural style?
  □ Are there any assumptions that are wrong?

If the plan has issues:
  "Your plan looks mostly right. Two corrections:
  1. Put the JWT utilities in src/utils/jwt.py not src/auth/
  2. Do not create a new User model — use the existing one in src/models/user.py
  Now proceed with the corrected plan."

Only approve if the plan is correct. It is much cheaper to correct a plan
than to undo a wrong 15-file implementation.
```

---

## 4. The Agent Mode Task Template

Use this template for every Agent Mode session:

```markdown
## Pre-session (do this before typing the prompt):
- [ ] git add . && git commit -m "checkpoint: before agent mode - [task name]"
- [ ] Close irrelevant files to reduce context noise

## Prompt Template:

### Context
[Describe the current state of the relevant code — what exists, what it does]

### Goal
[What the final result should look like — not HOW to get there, WHAT the end state is]

### Requirements
[Specific, verifiable requirements — each one should be testable]
- Requirement 1
- Requirement 2
- Requirement N

### Constraints
[What must NOT change, what patterns to follow]
- Do not modify: [list specific files/APIs to preserve]
- Use existing: [list patterns/models/utilities to reuse]
- Tech stack: [specific libraries/versions to use]
- Do NOT add: [libraries or patterns to avoid]

### Tests Required
[Whether tests are required and what they should cover]
- Unit tests for: [specific functions]
- Mock: [specific external services]

### Plan First
Before making any changes, describe:
1. The files you will create or modify
2. The approach for each major component
3. Any assumptions you are making

Wait for my confirmation before making changes.
```

---

## 5. Controlling Agent Mode During Execution

### When to Stop Agent Mode

```
Stop (press the Stop button) when:
  - The plan review showed Agent Mode misunderstood the task
  - Agent Mode starts creating files in wrong directories
  - Agent Mode modifies a file you explicitly said not to touch
  - Agent Mode asks a question whose answer changes the entire approach
  - The terminal shows an unexpected error Agent Mode can't recover from
  - Agent Mode loops more than 3 times on the same error

How to redirect after stopping:
  "Stop. The approach is wrong because [reason].
  New approach: [describe correct approach].
  Plan this new approach and wait for my approval."
```

### Asking Agent Mode to Stop and Explain

```
At any point, you can type in the Chat:
  "Pause. Before continuing, explain what you've done so far
  and what you plan to do next."
  
  → Agent Mode summarizes its current state
  → You can confirm, redirect, or stop

This is especially useful for long tasks where you lose track of what Agent Mode has done.
```

---

## 6. Recovering from a Bad Session

```
If Agent Mode produced output that is wrong or incomplete:

Step 1: Stop Agent Mode (click Stop button)

Step 2: Assess the damage:
  git diff   → see all changes Agent Mode made

Step 3: If the changes are salvageable:
  - Accept the good parts manually via diff review
  - Create a follow-up prompt to fix the bad parts
  - git add . && git commit to checkpoint the partial progress

Step 4: If the changes are a mess:
  git checkout .   → restore all files to the pre-session commit
  (This is why the pre-session commit is non-negotiable)

Step 5: Re-prompt with better constraints:
  - Be more specific about what NOT to do
  - Reduce the scope (break one big task into two smaller ones)
  - Provide an example of the pattern to follow
```

---

## 7. Agent Mode — High-Value Use Cases

### Use Case 1 — Scaffold a New Feature End to End

```
Prompt template:
  "Scaffold a new [feature name] feature with:
  - Router: src/api/[feature].py with CRUD endpoints
  - Service: src/services/[feature]_service.py with business logic
  - Repository: src/repositories/[feature]_repo.py with DB queries
  - Schema: src/schemas/[feature].py with Pydantic models
  - Tests: tests/unit/test_[feature]_service.py
  
  Follow the same pattern as src/api/users.py — use that as the template.
  Do not implement any business logic yet — use NotImplementedError stubs.
  Plan first, then implement."
```

### Use Case 2 — Generate and Run Tests

```
Prompt template:
  "Generate unit tests for src/services/user_service.py.
  After generating, run: pytest tests/unit/test_user_service.py -v
  Fix any failing tests.
  Target: all tests passing, at least 80% coverage of the service module."
```

### Use Case 3 — Codebase Exploration

```
Prompt template:
  "Analyze this codebase and answer:
  1. What is the overall architecture pattern (layered, hexagonal, etc.)?
  2. How does a request flow from the API router to the database?
  3. What is the testing strategy and what is NOT tested?
  4. What are the top 3 architectural improvements that would have the most impact?
  
  Do not make any changes. Only analyze and report."
```

### Use Case 4 — Refactor with Tests

```
Prompt template:
  "Refactor src/services/order_service.py to extract the pricing logic
  into a separate PricingEngine class in src/services/pricing_engine.py.
  
  Requirements:
  - All existing OrderService tests must still pass after refactoring
  - PricingEngine must have its own tests
  - OrderService.calculate_total() must delegate to PricingEngine
  - No behavior change — same inputs produce same outputs
  
  Run existing tests before starting to establish baseline.
  Run tests after each file change to catch regressions.
  Plan first."
```

---

## 8. When NOT to Use Agent Mode

```
Do NOT use Agent Mode for:
  - Simple one-file edits (use Edits or Chat)
  - Quick questions ("Explain what this function does")
  - Learning and exploration (Chat Ask is better)
  - Production-critical changes without a human architecture review first
  - Any change where you can't describe a clear success criteria
  - Anything involving real production data (testing or debugging)
  - Changes to infrastructure code (CI, deployment) without a second review

The cost of Agent Mode:
  - Tokens: Agent Mode uses significantly more tokens than Chat Ask
  - Time: Planning + execution + review takes longer than a targeted Chat Ask
  - Risk: More files changed = more to review = more opportunity for hidden bugs

Rule: If you can accomplish the task with a targeted Chat Ask or Edits session,
use that. Escalate to Agent Mode only when the task genuinely requires autonomy.
```

---

## 9. Agent Mode Safety Checklist

```
Before every Agent Mode session:
[ ] Committed current work to git (recovery net)
[ ] Closed irrelevant files to reduce context noise
[ ] Prompt includes: Context, Goal, Requirements, Constraints, Plan First instruction
[ ] I know which files should and should NOT be touched
[ ] I have a clear success criteria (how will I know it worked?)

During session:
[ ] Reviewed and approved the plan before allowing implementation
[ ] Monitoring for unexpected file modifications
[ ] Ready to stop the session if it goes wrong

After session:
[ ] Reviewed every changed file in the diff
[ ] Ran the test suite — no regressions
[ ] Checked all new imports are safe and appropriate
[ ] Committed the final accepted changes with a descriptive message
```

---

## 10. Revision Checklist

- [ ] Can explain the difference between Edits mode and Agent Mode
- [ ] Always requires a plan step in every Agent Mode prompt
- [ ] Has the pre-session commit habit established
- [ ] Uses the Agent Mode task template for every session
- [ ] Knows how to stop, redirect, and recover from a bad session
- [ ] Knows `git checkout .` as the recovery command for Agent Mode disasters
- [ ] Can identify 4 high-value Agent Mode use cases
- [ ] Knows when NOT to use Agent Mode (simple tasks, one-file edits, production critical)
- [ ] Completes the safety checklist for every session
