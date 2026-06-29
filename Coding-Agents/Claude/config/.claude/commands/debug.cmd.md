---
description: Root cause analysis with ranked fix options and prevention pattern
---

Debug the following:

$ARGUMENTS

Provide:
1. **Root cause** (1 sentence — WHY, not just the symptom)
2. **Why it happens** (the underlying mechanism — not just "the value is None")
3. **Fix options** (at least 2, ranked by: correctness → maintainability → effort)
   - Option A: [description + code]
   - Option B: [description + code]
4. **Recommended fix**: which option and why (1 sentence)
5. **Prevention**: how to avoid this class of error in new code

Rules:
- Root cause must explain WHY, not just describe the symptom
- Show fix as unified diff (not full file rewrite)
- Do not suggest "restart the server" without a mechanism
- Do not suggest "check your syntax" unless there is a clear syntax error
- If you need more context: ask for it before guessing
