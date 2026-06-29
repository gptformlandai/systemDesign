---
description: Session planning before any implementation — planning only, no code
---

Plan the following task. Do NOT write any code.

Task: $ARGUMENTS

Provide:

1. **Implementation steps** (3-7, ordered by dependency)
   For each step:
   - Specific action (not "implement service" but "create process_refund() method in PaymentService")
   - Files to create or modify (exact paths)
   - Subagent to use (if any: @builder, @tester, @reviewer, or none)

2. **Blockers** (max 3 — questions to resolve BEFORE coding):
   - What information or decision is missing?

3. **Risk flag** (1 sentence):
   - Most likely thing to go wrong

4. **Success criteria** (testable statement):
   - "Done means: [specific, verifiable condition]"

5. **Estimate**: S (1-2h) / M (half day) / L (full day)

Rules:
- Be specific about file paths — never say "the service file"
- If uncertain about a file: ask rather than guess
- Under 300 words
- After presenting the plan: wait for approval before doing anything
