# Subagents — Deep Dive — Gold Sheet

> **Track**: Claude Mastery Track — Group 3: Advanced Engineering
> **File**: 1 of 7 (Track File #14)
> **Audience**: Developers designing specialist Claude agents for complex workflows
> **Read after**: Before-After-Prompt-Examples-Gold-Sheet.md (Intermediate complete)

---

## 1. Practical Impact Meter

| Topic | Impact | Why Developers Miss This |
|---|---|---|
| What subagents are and why they beat one-agent-does-everything | ★★★★★ | Devs use Claude as a generalist and miss the specialist output quality |
| Context isolation — the core subagent benefit | ★★★★★ | Without isolation, one agent's mistakes pollute another agent's context |
| 7 specialist subagent designs | ★★★★★ | Ready-to-use designs for every major development workflow |
| When NOT to use subagents | ★★★★☆ | Over-using subagents for simple tasks adds overhead with no benefit |
| Subagent handoff patterns | ★★★★☆ | How to pass context between agents without losing critical decisions |

---

## 2. What Subagents Are

### Must Know

```
A subagent is a Claude Code session with a specific role, scope, and context.
It is NOT a separate AI model. It is NOT a background process.

Subagent = a new Claude session with:
  1. A specific system prompt defining its role
  2. Only the context relevant to its task
  3. Clear inputs and outputs
  4. Explicit boundaries (what it can and cannot touch)

Why subagents over one general agent:
  GENERAL AGENT session for a complete feature:
    - Carries planning + design + implementation + testing context
    - 3 hours in: context drift begins
    - Code quality degrades as context fills
    - Architectural decisions made early get forgotten
    - Everything ends up in one unreviewed block

  SUBAGENT pipeline for the same feature:
    - @planner: clean context, focuses only on design decisions
    - @builder: clean context, implements the design (no planning noise)
    - @tester: clean context, generates tests (no implementation noise)
    - @reviewer: clean context, reviews code (no bias from having written it)
    - Each agent: sharp, focused, no drift
```

---

## 3. Context Isolation — The Core Benefit

```
The key insight: isolation prevents context pollution.

Example: The reviewer should NOT know the author's intent.
  If the reviewer was the same session that wrote the code:
    "I know why I wrote it this way, so I'll assume it's correct"
    → Bias toward approving wrong decisions

  If the reviewer is a fresh session:
    "I don't know why this was written this way"
    → Reads it from a fresh perspective, like a real code reviewer
    → More likely to catch actual bugs and design issues

Similarly:
  The tester should not know the implementation details
  (tests should test behavior, not implementation)
  
  The optimizer should not know the original author's preferences
  (optimization requires challenging assumptions)

Rule: If a task benefits from a fresh perspective → use a subagent.
```

---

## 4. The 7 Specialist Subagents

### Subagent 1: @planner

```markdown
Role: Session and feature planning specialist
Context at start: business requirements, high-level goals
Context NOT included: implementation details, existing code
Output: structured plan (files, dependencies, assumptions)
Handoff to: @builder with the approved plan

Invoke with:
"Use the @planner agent.
Task: design the implementation for [feature].
Requirements: [list]
Constraints: [list]
Output: implementation plan (files to create, one-sentence purpose each, dependencies)"
```

### Subagent 2: @builder

```markdown
Role: Implementation specialist
Context at start: approved plan from @planner, relevant pattern files
Context NOT included: planning discussions, business context
Output: implemented code files
Handoff to: @tester with the list of created files

Invoke with:
"Use the @builder agent.
Plan: [paste plan from @planner]
Pattern to follow: @file:[example file]
Build: [specific files] in this order: [ordered list]
Plan before building. Wait for approval."
```

### Subagent 3: @debugger

```markdown
Role: Root cause analysis specialist
Context at start: error + stack trace + failing code only
Context NOT included: implementation context, other files
Output: root cause + fix options + prevention

Invoke with:
"Use the @debugger agent.
Error: [exact error + stack trace]
Failing code: @file:[file]
Output: root cause (1 sentence) + fix options (2-3) + prevention"
```

### Subagent 4: @tester

```markdown
Role: Test generation and gap analysis specialist
Context at start: implementation files to test (fresh read)
Context NOT included: how the code was written, author's intent
Output: complete test files + test gap analysis

Invoke with:
"Use the @tester agent.
Implementation: @file:[file1], @file:[file2]
Framework: [framework]
Cover: happy path, error cases, edge cases
Mock: [list external dependencies]
Output: complete test files + gap analysis report"
```

### Subagent 5: @reviewer

```markdown
Role: Code review specialist (security + quality + coverage)
Context at start: code diff or files to review (fresh read)
Context NOT included: implementation history or author intent
Output: severity-ranked findings with fixes

Invoke with:
"Use the @reviewer agent.
Review: @file:[files]
Check: security, correctness, test coverage, error handling, maintainability
Format: | SEVERITY | Issue | Location | Fix |
CRITICAL first. Under 300 words."
```

### Subagent 6: @architect

```markdown
Role: Architecture planning and review specialist
Context at start: system requirements + existing patterns
Context NOT included: implementation details
Output: architectural recommendations with trade-offs

Invoke with:
"Use the @architect agent.
Design: [system or feature to design]
Context: @file:[existing architecture files]
Output: component design, trade-offs, recommendation, risks"
```

### Subagent 7: @optimizer

```markdown
Role: Performance and efficiency specialist
Context at start: code + performance measurements/symptoms
Context NOT included: business requirements
Output: prioritized optimization opportunities with code

Invoke with:
"Use the @optimizer agent.
Optimize: @file:[file]
Symptom: [observed performance issue]
Output: | Issue | Impact | Fix | Effort | — top 3 only. Show code for #1."
```

---

## 5. Subagent Handoff Patterns

### Pattern: Plan → Build → Test → Review Pipeline

```
Step 1: @planner session
  Input: requirements
  Output: implementation plan → save as plan.md

Step 2: @builder session
  Input: paste plan.md content + relevant pattern files
  Output: implemented files → list them

Step 3: @tester session
  Input: list of implemented files (fresh read — no implementation context)
  Output: test files + run them → save test results

Step 4: @reviewer session
  Input: implemented files + test files (fresh read)
  Output: review findings → apply fixes

Handoff template between sessions:
"New session. Previous @planner produced this plan:
[paste plan output]
This session: [specific task for this agent]
Constraints from previous session: [key decisions made]"
```

### What NOT to Include in Handoffs

```
Do NOT include in handoff context:
  ✗ The full conversation history of the previous session
  ✗ Failed attempts and dead ends (irrelevant to next agent)
  ✗ Author's intent for ambiguous decisions (let reviewer see fresh)

DO include:
  ✓ The decisions made (what was built and why — factual)
  ✓ The file list (what exists)
  ✓ The active constraints (what must not change)
  ✓ The success criteria (what the next agent must achieve)
```

---

## 6. When NOT to Use Subagents

```
Overkill for:
  - Simple one-file changes
  - Quick bug fixes with obvious cause
  - Short code generation tasks (< 50 lines)
  - Factual questions
  - Boilerplate generation from a clear template

Use for:
  - Tasks that span multiple files with distinct concerns
  - Tasks that benefit from a fresh perspective (review, testing)
  - Long-running autonomous work where context drift is a risk
  - Tasks where the quality of isolation matters (security review)
```

---

## 7. Revision Checklist

- [ ] Can explain why subagents produce better output than one long session
- [ ] Understands context isolation and why it matters for review/testing
- [ ] Has all 7 specialist subagent invocation patterns memorized
- [ ] Uses the Plan → Build → Test → Review pipeline for complex features
- [ ] Knows what to include and exclude in handoff context
- [ ] Knows when NOT to use subagents (simple tasks)
