# Multi-Agent Patterns — Gold Sheet

> **Track**: Codex Mastery Track — Group 3: Advanced Engineering
> **File**: 4 of 7 (Track File #17)
> **Audience**: Developers building complex features that exceed single-session scope
> **Read after**: Agent-Loops-Gold-Sheet.md

---

## Practical Impact Meter

| Topic | Impact | Why Developers Miss This |
|---|---|---|
| Context isolation — why separate sessions matter | ★★★★★ | Long sessions accumulate contradictions; clean context per phase = cleaner output |
| Planner → Builder → Tester pipeline | ★★★★★ | The most reusable multi-agent pattern; applies to any feature build |
| Handoff document — structured output between sessions | ★★★★★ | Sessions can't share context; the handoff doc IS the communication channel |
| Failure recovery — what to do when a session produces wrong output | ★★★★☆ | Without recovery patterns, one bad session derails the entire pipeline |
| When NOT to use multi-agent (overhead for small tasks) | ★★★☆☆ | Multi-agent adds overhead; small tasks are faster in a single auto-edit session |

---

## ⭐ Beginner Tier — Start Here

### B1: Understand why separate sessions help

```bash
# Problem with a single session for a large task:
codex "Design and implement and test the complete payment service"
# What happens:
# - Session accumulates 30+ messages of context
# - Context drift: Codex forgets constraints from message 5 by message 25
# - Mixing planning + implementation + testing = worse output for each

# Solution: separate sessions, each with clean context
# Session 1: PLAN ONLY
codex "Design the payment service. Output: structured plan document. Do not implement."
# Save the output: docs/plans/payment-service-plan.md

# Session 2: IMPLEMENT ONLY (new terminal = new session)
codex "Implement the payment service using this plan: docs/plans/payment-service-plan.md"

# Session 3: TEST ONLY (new terminal = new session)
codex "Generate tests for the payment service using: docs/plans/payment-service-plan.md"
```

Notice: each session has clean context, focused on one job.

---

## 1. The Core Concept: Context Isolation

```
Why multi-agent works:
  Each new Codex session starts with a clean context window.
  AGENTS.md + your prompt = the only context.
  No accumulated noise from prior decisions.

Single-session risk:
  After 20+ messages:
  - Codex may "remember" a constraint from message 3 incorrectly
  - Codex may apply patterns from a previous sub-task to the current one
  - Output quality degrades as context fills with irrelevant history

Multi-agent benefit:
  Each agent gets exactly the context it needs — no more, no less.
  Planner gets: the requirement. That's it.
  Builder gets: the plan. That's it.
  Tester gets: the new code and test conventions. That's it.
```

---

## 2. The Planner → Builder → Tester Pipeline

### Phase 1: Planner Session

```bash
# NEW terminal (or interactive session)
codex --model gpt-4.1 --approval-policy suggest \
  "Plan the implementation of: [feature description]
   
   Context:
   - Existing codebase uses: [framework, pattern summary]
   - Files most relevant to this feature: [list them]
   - Constraints: [architecture rules, forbidden libraries, etc.]
   
   Output a structured plan document with:
   1. Files to create (name, purpose, key functions)
   2. Files to modify (which functions to change and why)
   3. Implementation sequence (order matters for dependency reasons)
   4. Test cases to verify correctness
   5. Risks and unknowns
   
   Do NOT implement. Plan only.
   Save output: I will copy this to docs/plans/[feature]-plan.md"
```

→ Copy the output to `docs/plans/[feature]-plan.md`
→ Read the plan. Correct any wrong assumptions before proceeding.

### Phase 2: Builder Session

```bash
# NEW terminal (clean context)
codex --approval-policy auto-edit \
  "Implement this feature following the plan in docs/plans/[feature]-plan.md.
   
   Context files: [list only the files mentioned in the plan]
   
   Process:
   1. Read the plan completely before starting
   2. Implement one component at a time in the order specified
   3. After each component: run [verification command]
   4. Fix any failures before moving to the next component
   5. Follow AGENTS.md conventions
   
   Constraints:
   - Only modify files listed in the plan
   - Do not modify test files
   - Do not deviate from the plan without noting it
   
   When complete: report what was built, what (if anything) deviates from the plan."
```

### Phase 3: Tester Session

```bash
# NEW terminal (clean context)
codex --approval-policy auto-edit \
  "Generate comprehensive tests for the feature implemented in: [list new files]
   
   Context:
   - Implementation plan: docs/plans/[feature]-plan.md (for the spec)
   - Test conventions: [test framework, file location, mock patterns]
   - Key test cases from plan: [paste test cases section from plan]
   
   Generate tests covering:
   - All happy paths from the plan
   - All error cases from the plan
   - Edge cases: empty inputs, boundary values, invalid types
   
   Do NOT modify implementation files.
   Mock: external dependencies only.
   Verification: [test command] (all must pass)"
```

---

## 3. The Handoff Document — Communication Between Sessions

Since sessions can't share context, the handoff document is the only communication channel.

```markdown
# Handoff: [Feature Name] — [Phase]

## Summary
[What was accomplished in this phase — 2-3 sentences]

## Files Created/Modified
| File | What Changed |
|------|-------------|
| src/api/orders.py | Added GET /orders/{id} endpoint |
| tests/test_order_api.py | Added 8 test cases |

## Test Results
[pytest output — last run: N passed, M failed]

## Implementation Notes
[Decisions made during implementation that deviate from or extend the plan]
- Used X instead of Y because [reason]

## What Remains
[For the next phase — what needs to happen]

## For Next Phase
Context files needed: [exact list]
Verification command: [exact command]
Key constraint: [most important constraint for next phase]
```

---

## 4. The Reviewer Session (Phase 4)

```bash
# NEW terminal — after builder and tester are done
codex --model gpt-4.1 --approval-policy suggest \
  "Review the completed implementation of [feature].
   
   Files to review: [list all new and modified files]
   Plan (to verify implementation matches intent): docs/plans/[feature]-plan.md
   
   Review checklist:
   1. Security: OWASP vulnerabilities, auth checks, input validation
   2. Correctness: does the implementation match the plan?
   3. Test quality: are tests meaningful or tautological?
   4. Conventions: AGENTS.md compliance
   5. Backwards compatibility: does this break existing callers?
   
   Output: | SEVERITY | ISSUE | FILE:LINE | FIX |
   Final: APPROVED / APPROVE WITH COMMENTS / CHANGES REQUIRED"
```

---

## 5. Failure Recovery

### Session produces wrong output

```bash
# Scenario: Builder built something different from the plan
# Recovery:
git checkout -- src/    # revert builder changes
# Diagnose: what assumption in the plan was wrong?
# Fix the plan document
# Restart builder session with corrected plan
```

### Context drift in builder session

```bash
# Signs: builder is making changes inconsistent with the plan
# Recovery:
# 1. Stop the session
# 2. Run: git diff HEAD — see what was done
# 3. Commit the good parts: git add [good files] && git commit
# 4. Start a new builder session starting from the next uncommitted component
```

### Tester generates tests that fail

```bash
# Scenario: Generated tests fail because they test the wrong behavior
# Before fixing: determine which is wrong — the test or the implementation?
#   If test is wrong (tests invented behavior): fix the test
#   If implementation is wrong (tests test the spec correctly): fix implementation

# In a new session:
codex --approval-policy auto-edit \
  "The following tests are failing:
   [paste pytest output]
   
   Implementation file: [file]
   Reason tests should pass: [explain expected behavior from plan]
   
   Determine: is the test wrong or is the implementation wrong?
   Fix the side that is wrong. Do not modify the other.
   Verification: [test command]"
```

---

## 6. When NOT to Use Multi-Agent

```
Use multi-agent when:
  ✅ Feature spans 5+ files
  ✅ Task needs 30+ minutes of Codex execution
  ✅ Planning is complex (architecture decisions, many unknowns)
  ✅ You want separate review of plan before building

Skip multi-agent when:
  ❌ Adding a small feature to one or two files (use auto-edit)
  ❌ Debugging a specific bug (single focused session)
  ❌ Generating tests for one function (single auto-edit)
  ❌ Adding a docstring (single command)
  ❌ The overhead of 3 sessions costs more than the feature itself

Rule of thumb:
  If you could describe the task in a single bounded auto-edit prompt: use auto-edit.
  If the task needs a plan before implementation: use multi-agent.
```

---

## Interview Traps

```
TRAP: "Multi-agent means one Codex session automatically spawns other sessions"
TRUTH: Codex CLI doesn't spawn subagents. Multi-agent means YOU run multiple Codex
       sessions sequentially and pass handoff documents between them. The 'pipeline'
       is your orchestration. This is a workflow pattern, not an automated system.

TRAP: "The Builder session automatically knows what the Planner decided"
TRUTH: Each session starts with only what you provide. The handoff document IS the
       context. A vague handoff = a Builder session that makes its own assumptions.
       Precision in the handoff document directly determines precision of implementation.

TRAP: "Multi-agent adds overhead without benefit for most tasks"
TRUTH: For tasks where wrong implementation direction is expensive to undo, the Planner
       phase pays back in 5 minutes by catching wrong direction before 30 minutes of
       building. Rule: single-session for bounded tasks; multi-agent when you need a plan
       review before committing to implementation.
```

---

## Revision Checklist

- [ ] Can explain why context isolation between sessions matters
- [ ] Can run a complete Planner → Builder → Tester pipeline on a real feature
- [ ] Can write a handoff document that a new Codex session can work from
- [ ] Know the 3 failure recovery patterns
- [ ] Know when to use multi-agent vs single-session auto-edit
