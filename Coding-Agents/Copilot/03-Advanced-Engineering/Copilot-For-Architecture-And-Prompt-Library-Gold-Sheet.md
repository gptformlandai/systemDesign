# Copilot For Architecture — Gold Sheet

> **Track**: Copilot Mastery Track — Group 3: Advanced Engineering
> **File**: 6 of 7 (Track File #19)
> **Audience**: Developers using Copilot for design and architecture decisions
> **Read after**: MCP-Integration-Copilot-Gold-Sheet.md

---

## 1. Practical Impact Meter

| Topic | Impact | Why Developers Miss This |
|---|---|---|
| Architecture review prompt — structured evaluation | ★★★★★ | Devs ask "is this good?" — vague question gets vague answer |
| ADR generation — Architecture Decision Records | ★★★★★ | Capturing decisions prevents future teams from re-litigating the same debates |
| System design brainstorming with Copilot | ★★★★☆ | Devs think Copilot is only for code — design brainstorming is underused |
| Component design — interface before implementation | ★★★★☆ | Designing interfaces first catches integration problems before they are built |
| Copilot limitations for architecture — what to distrust | ★★★★★ | Copilot has architectural opinions that may not fit your specific context |

---

## 2. Architecture Review Prompt

```
"Perform an architecture review of #file:src/ (or #selection for a specific component).

Evaluate on these dimensions:

1. Separation of Concerns
   - Are responsibilities cleanly separated between layers?
   - Does any single class/module do too many things?

2. Coupling and Cohesion
   - What is tightly coupled that should be loosely coupled?
   - What is split across files that should be together?

3. SOLID Principles
   - Single Responsibility: does each class have one reason to change?
   - Open/Closed: can new behavior be added without modifying existing code?
   - Liskov Substitution: would subclasses break if substituted?
   - Interface Segregation: are interfaces narrow enough?
   - Dependency Inversion: does code depend on abstractions or concretions?

4. Testability
   - Can each component be unit tested in isolation?
   - What makes testing hard? (hardcoded dependencies, static calls, global state)

5. Scalability and Bottlenecks
   - What breaks first under 10x load?
   - Where is shared mutable state that becomes a contention point?

6. Maintainability
   - What would be painful to change 6 months from now?
   - What is the blast radius if X changes?

Output: prioritized table (HIGH/MEDIUM/LOW) with:
  | Issue | Location | Consequence | Suggested Improvement |"
```

---

## 3. ADR Generation

### What is an ADR?

```
Architecture Decision Record = a short document capturing:
  - What architectural decision was made
  - Why it was made (context and options considered)
  - What the consequences are

Why they matter:
  New team members understand WHY the architecture is what it is.
  Future refactoring knows which constraints are intentional.
  Avoids re-litigating decided debates.
```

### ADR Generation Prompt

```
"Generate an Architecture Decision Record (ADR) for this decision:

Decision: [State the decision that was made]
Example: "We chose SQLAlchemy async with asyncpg over psycopg2 for database access"

Context:
[Describe the situation that led to the decision]

Format:
# ADR-[number]: [Short title]
Date: [today]
Status: Accepted

## Context
[1-2 paragraphs: the problem, constraints, and why a decision was needed]

## Decision
[1 paragraph: what was decided, stated clearly]

## Options Considered
[3 alternatives considered with 2-3 bullet pros/cons each]

## Consequences
**Positive:** [what improves]
**Negative:** [what gets harder or more complex]
**Neutral:** [what changes but is neither better nor worse]

## Compliance
[How to verify the decision is being followed]"
```

---

## 4. System Design Brainstorming

```
The right way to use Copilot for system design:

Step 1 — Set the problem clearly:
"Design a notification system that:
- Sends emails, SMS, and push notifications
- Handles 1 million users
- Supports scheduling (send at specific time)
- Supports user notification preferences
- Must not lose notifications even if downstream services are down

Constraints:
- Uses our existing PostgreSQL database
- Must integrate with our current FastAPI backend
- Budget for external services: limited (prefer OSS)

Do NOT design yet. Ask me clarifying questions first."

Step 2 — Answer clarifying questions (Copilot is good at finding the right questions)

Step 3 — Design:
"Now design the high-level architecture. Include:
1. Components and their responsibilities
2. Data flow diagram (text-based mermaid or ASCII)
3. Queue/messaging strategy
4. Database schema for notifications
5. Trade-offs of this approach vs alternatives"

Step 4 — Challenge the design:
"What are the top 3 weaknesses in this design?
What would fail first at 10x scale?
What is the risk if the SMS provider goes down?"
```

---

## 5. Copilot's Architecture Limitations

```
Trust Copilot for:
  ✓ Naming patterns (repo names, class names, method names)
  ✓ Textbook architectural patterns (layered, hexagonal, CQRS, event sourcing)
  ✓ SOLID principles analysis of small code units
  ✓ ADR formatting and structure
  ✓ Generating options to consider (not the final answer)

Do NOT trust Copilot for:
  ✗ Production scale estimates without your data ("how many servers do I need?")
  ✗ Cost estimates for cloud infrastructure (use vendor calculators)
  ✗ Regulatory and compliance decisions (HIPAA, GDPR, SOC 2)
  ✗ Security architecture without a human security review
  ✗ Long-term maintainability predictions (it has no knowledge of your team)
  ✗ The "right" answer for your specific organization (it has no org context)

Copilot gives you GENERIC best practices applied to YOUR context.
Your context-specific knowledge must validate every architectural suggestion.
```

---

## 6. Prompt Library Management — Gold Sheet

> **Track**: Copilot Mastery Track — Group 3: Advanced Engineering
> **File**: 7 of 7 (Track File #20)
> **Audience**: Developers building and maintaining a growing prompt library

---

## 6.1 Organizing a Production Prompt Library

```
Naming: <verb>-<noun>.prompt.md (max 3 words)
Location: .github/prompts/ (team-shared) or personal prompt notes folder

Versioning your prompts (commit messages):
  "improve generate-tests: add edge case requirement"
  "fix debug-error: specify stack trace format required"
  "add security-review: new OWASP-aligned review prompt"

Prompt lifecycle:
  1. Draft — write first version, use it 5 times
  2. Validated — works consistently, used by more than 1 person
  3. Deprecated — replaced by a better version, file kept for reference
  4. Archived — moved to archived/ subdirectory after 90 days inactive
```

## 6.2 Prompt Review Checklist

```
Before publishing a prompt file to the team:
[ ] Name clearly describes what the prompt does (verb-noun)
[ ] Description in frontmatter is one sentence and accurate
[ ] Prompt produces consistent quality output across 5 different inputs
[ ] Prompt includes: context collection, goal, output format, constraints
[ ] Prompt includes at least one "Do NOT" constraint
[ ] Output format is explicitly specified (table / list / diff / prose)
[ ] Tested with: good input, bad input, edge case input
[ ] Does not request sensitive information from the user
[ ] Does not assume project-specific context (that belongs in copilot-instructions.md)
```

## 6.3 Advanced Path Completion Checklist

```
Sheets completed:
[ ] Custom Agents Deep Dive
[ ] AGENTS.md Strategy
[ ] Context Engineering
[ ] Token Optimization
[ ] MCP Integration
[ ] Copilot For Architecture
[ ] Prompt Library Management

Skills demonstrated:
[ ] Have 5+ custom agents in .github/agents/
[ ] Have root AGENTS.md and at least 2 folder-level AGENTS.md files
[ ] Have a project context file in at least one project
[ ] Can explain what MCP is and have mcp.example.json configured safely
[ ] Have run an architecture review using the structured prompt
[ ] Have written at least 2 ADRs with Copilot assistance
[ ] Have a prompt library of 10+ files organized by category

Next step: 04-Pro-MAANG-Level/Personal-Copilot-Operating-System-Gold-Sheet.md
```
