# Advanced Context Engineering — Gold Sheet

> **Track**: Copilot Mastery Track — Group 4: Pro / Production Level
> **File**: 4 of 7 (Track File #24)
> **Audience**: Developers who want surgical precision over what Copilot sees and when
> **Read after**: Copilot-Debugging-Handbook-Gold-Sheet.md

---

## 1. Practical Impact Meter

| Topic | Impact | Why Developers Miss This |
|---|---|---|
| Context hierarchy — what wins when multiple sources conflict | ★★★★★ | Devs add instructions but don't know which ones Copilot prioritizes |
| The project context document pattern | ★★★★★ | Restarting context from scratch every session is the biggest time sink |
| Chunking strategy for large implementation tasks | ★★★★★ | Large tasks in one session degrade because context fills and gets truncated |
| "Plan → Confirm → Execute" — preventing drift | ★★★★★ | Without confirmation gates, multi-step tasks diverge from intent early |
| Context for different environments: personal/freelance/enterprise | ★★★★☆ | One-size-fits-all instructions don't work across context types |
| Workspace-level vs user-level vs repository-level configuration | ★★★★☆ | Devs only know repo-level; user-level and workspace-level unlock more control |
| Giving Copilot architectural memory across sessions | ★★★★☆ | Copilot forgets — the pattern for giving it back fast |

---

## 2. Context Hierarchy — What Wins

### Priority Order (highest to lowest)

```
When multiple instruction sources exist, Copilot applies them in this order:

1. Your current message content (highest — always read)
2. Explicit context variables: #file, #selection, #sym (explicit > implicit)
3. Conversation history in current session
4. Path-specific instruction files (.github/instructions/*.instructions.md)
   matching the file being discussed — more specific path wins
5. Root-level copilot-instructions.md
6. AGENTS.md (if in Agent Mode, nearest file wins)
7. Workspace index (#codebase, implicit from open files)
8. Open tabs in editor (implicit)

Practical implication:
  If your message says "use requests" but copilot-instructions.md says "use httpx",
  YOUR MESSAGE WINS. The instructions are the default; your message overrides them.

  If python.instructions.md says "use pytest" and copilot-instructions.md says
  "use unittest", python.instructions.md wins for .py files (more specific).
```

---

## 3. The Project Context Document

### Why Every Serious Project Needs One

```
Problem: Copilot has zero memory between sessions.
Every new chat session, Copilot doesn't know:
  - What you've already implemented
  - What architectural decisions were made
  - What's in-progress vs done
  - What constraints exist
  - What the current sprint goal is

Solution: A living project context document that you paste at session start.

Location options:
  .copilot-context.md  (gitignored — personal context, not team-shared)
  docs/project-context.md (committed — team-shared, less session-specific)
```

### The Project Context Document Template

```markdown
# Project Context — [Project Name]
Updated: [date]

## What This Is
[2-3 sentences: what the project does, scale, tech stack summary]

## Current State
[What is done, what is in progress, what's next]
- DONE: User auth (JWT), User CRUD, Order creation
- IN PROGRESS: Payment integration (Stripe) — 60% complete
- NEXT: Refund flow, notification system

## Architecture That's Locked In
[Decisions already made that Copilot should NOT reverse-engineer]
- Layered: Router → Service → Repository → DB (strict, no cross-layer)
- Async throughout: asyncpg + SQLAlchemy async. No sync DB calls.
- Auth: JWT stateless. No sessions. No cookies.
- External services: called only from service layer, never from routers.

## Current File I'm Working On
[Update this per session]
- File: src/services/payment_service.py
- Goal: Implement Stripe charge + refund
- Tests needed: charge success, card declined, refund partial/full

## Do Not
[What Copilot should never suggest in this session]
- Do not suggest adding Redis (we're not adding caching yet)
- Do not change the User model schema
- Do not suggest switching to synchronous SQLAlchemy
```

### Using It

```
Start every relevant session with:
"Context for this session: #file:.copilot-context.md
Now: [your actual question or task]"

Total cost: ~150-200 words of context
Total benefit: Copilot instantly knows the full project state
```

---

## 4. Context Strategies for Different Environments

### Personal Projects

```
Personal project context needs:
  - What you're building and why (motivation context helps with architecture advice)
  - Your skill level in this tech (helps calibrate explanation depth)
  - Time constraints (helps calibrate solution complexity)

Example addition to personal project context:
  "This is a personal learning project — I'm learning Rust.
  I prefer explanations of patterns over just code.
  I have 2 hours per weekend — suggest minimal viable approaches."
```

### Freelance Projects

```
Freelance project context needs:
  - Client's tech stack preferences and constraints
  - Delivery timeline pressure
  - What cannot be changed (existing client infrastructure)
  - Handoff requirement (code must be understandable by the client's team)

Example addition:
  "Client constraint: must use PHP 8.1. No modern framework preference.
  The handoff team has junior-level PHP experience — keep patterns simple.
  No external services unless free-tier available.
  Delivery: 3 weeks."
```

### Enterprise Projects

```
Enterprise context needs:
  - Security requirements (OWASP, compliance, audit logging)
  - Architecture governance (patterns approved by architecture board)
  - Library whitelist (only approved libraries)
  - Code review process requirements

Example addition:
  "Enterprise constraint: all libraries must be in our internal Artifactory.
  Security: all inputs validated via our internal validation library (not Pydantic).
  Architecture: approved patterns only — layered, event-driven via Kafka.
  No cloud SDK direct usage — use our internal infrastructure abstractions."
```

### Open-Source Projects

```
Open-source context needs:
  - Contribution guidelines
  - Existing community conventions
  - Backward compatibility requirements
  - License considerations

Example addition:
  "Open source: Apache 2.0 license. All contributions must be Apache 2.0 compatible.
  No GPL dependencies.
  Backward compatibility: must support Python 3.9+ (not just 3.12).
  CHANGELOG.md must be updated for every change.
  Test coverage must remain above 90%."
```

---

## 5. The Plan-Confirm-Execute Pattern

### The Anti-Drift Protocol

```
For any task > 30 minutes of work:

Step 1 — PLAN (Copilot generates, you read):
  "Plan only (no code):
  1. Files to create or modify
  2. Approach for each component
  3. Assumptions you're making
  4. Questions that need answers before coding
  Wait for my approval."

Step 2 — CONFIRM (you review the plan):
  Check: Is the approach correct? Are the files right?
  Correct wrong assumptions before Copilot writes a single line.
  "The plan looks good EXCEPT: [correction]. Update your plan accordingly."
  
Step 3 — EXECUTE (one file at a time):
  "Implement file 1: [filename]. Wait for me to review before moving to file 2."
  Review → run tests → "Approved. Implement file 2."
  
Why this works:
  - Error in planning = correction in one sentence
  - Error in implementation = caught before it propagates to 5 files
  - Total quality: much higher than open-ended Agent Mode
```

---

## 6. Chunking Large Tasks

### The Chunk Strategy

```
Rule: Any task > 3 files → chunk it.

Wrong:
  "Build the complete payment processing system"
  → Copilot makes 15 assumptions, creates 20 files, half are wrong

Right (chunked by dependency):

Chunk 1 — Schema and models:
  "Design only the Pydantic schemas and database models for payments.
  No implementation. Show me the data structures."
  → Review → approve → commit

Chunk 2 — Repository layer:
  "Implement PaymentRepository with: create, find_by_id, update_status.
  Follow the pattern in #file:src/repositories/user_repository.py"
  → Review → run tests → commit

Chunk 3 — Service layer:
  "Implement PaymentService using PaymentRepository.
  Handle: charge, refund, status lookup.
  Mock Stripe API calls."
  → Review → run tests → commit

Chunk 4 — API layer:
  "Add payment endpoints to #file:src/api/ following the users router pattern."
  → Review → run tests → commit

Each chunk: 1 conversation session, 1-3 files, clear success criteria.
```

---

## 7. Giving Copilot Architectural Memory

### The "Resume" Pattern

```
When resuming work on a complex task across sessions:

"Resume context: I was implementing [feature].
Here's what was done:
  ✓ PaymentRepository created: #file:src/repositories/payment_repository.py
  ✓ PaymentService skeleton: #file:src/services/payment_service.py
  ✓ Tests for repository: #file:tests/unit/test_payment_repository.py
  
What's next:
  - Implement the Stripe charge in PaymentService.process_charge()
  - Add the /payments POST endpoint to the router
  
Constraint reminders:
  - Never call Stripe from the router — service layer only
  - Use our existing StripeClient wrapper in #file:src/clients/stripe_client.py
  
Now implement PaymentService.process_charge(). Tests first."

Why this works:
  - Copilot gets compressed architectural memory in ~100 words
  - References to existing files = no rework of already-decided patterns
  - The "what's next" section = clear bounded scope
```

---

## 8. Stack-Agnostic Context Patterns

### These patterns work regardless of language or framework

```
For any implementation request, always include:
  1. The file or symbol to look at (#file or #sym)
  2. What pattern to FOLLOW (reference an existing similar file)
  3. What must NOT change
  4. What success looks like (passing tests, specific output)

Example — Node.js:
  "Add input validation to #sym:createOrderHandler.
  Follow the validation pattern in #sym:createUserHandler.
  Do not change the database layer.
  Tests must pass after the change."

Example — Java/Spring:
  "Add a new @PostMapping to #file:src/main/java/com/example/UserController.java
  following the same pattern as the existing @GetMapping.
  Service call only — no repository access from the controller.
  Add a test in #file:src/test/java/com/example/UserControllerTest.java"

Example — React/TypeScript:
  "Add a loading state to #sym:UserDashboard component.
  Follow the pattern in #sym:OrderDashboard (same repo).
  Do not add new state management library.
  Test with React Testing Library."
```

---

## 9. Revision Checklist

- [ ] Understands context hierarchy (message > explicit vars > path instructions > root instructions)
- [ ] Has a project context document for at least one project
- [ ] Uses the context document at the start of every relevant session
- [ ] Applies Plan-Confirm-Execute for all tasks > 3 files
- [ ] Chunks large tasks into 1-3 file sessions
- [ ] Uses the "Resume" pattern when resuming work across sessions
- [ ] Can adapt context strategy for personal/freelance/enterprise/open-source
- [ ] Knows stack-agnostic context patterns (reference existing files, not descriptions)
