---
name: Refactor Code
description: Clean refactoring with explanation of every change and what was preserved
---

Refactor the following code:

${selection}

Primary goal: ${input:Refactoring goal — e.g., extract method, reduce duplication, apply SOLID, improve readability, remove dead code}

Constraints:
- Preserve ALL existing public API signatures (unless asked to change)
- Existing tests must still pass after the refactoring
- Do NOT add new external dependencies
- Do NOT migrate to a different framework or library
- Do NOT add abstractions not needed for this specific change
- Do NOT add comments to every line — only comment non-obvious logic

Output:
1. **Refactored code** (complete and runnable — not a partial snippet)
2. **What changed**: bullet list — each change and the reason
3. **What I kept unchanged**: what you intentionally preserved and why
4. **Follow-up opportunities**: what else could be improved — do NOT implement these now

Scoring criteria for this refactoring:
- Does each function/class have exactly one reason to change?
- Is the code easier to test than before?
- Is it easier to read without the author present?
- Would a future developer spend less time understanding this?
