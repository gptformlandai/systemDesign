---
description: Focused refactoring with constraints, diff output, and change explanation
---

Refactor the following:

$ARGUMENTS

Rules:
- Preserve ALL existing public API signatures unless explicitly asked to change them
- Existing tests must still pass — do NOT change tests to match refactored code
- Do NOT add new external dependencies
- Do NOT add abstractions not needed for this specific change
- Do NOT add comments to every line — only comment non-obvious logic

Run tests after refactoring. Report results.

Output:
1. Unified diff (not full file rewrite — show only what changed)
2. What changed and WHY (one bullet per change)
3. What was intentionally preserved
4. Test results (before and after)
5. Follow-up opportunities (list only — do NOT implement)
