---
name: Debug Error
description: Diagnose an error with root cause analysis and ranked fix options
---

Debug the following error:

**Error message:**
${input:Paste the full error message and stack trace}

**What triggered it:**
${input:What were you doing when this occurred? (optional)}

**Relevant code:**
${selection}

Provide:

1. **Root cause** (one sentence): What is actually broken?
2. **Why it happens**: The underlying reason — not just "the value is None" but WHY it is None
3. **Fix options** (at least 2):
   - Option A: [description + code snippet]
   - Option B: [description + code snippet]
   - For each: trade-offs (simplicity vs correctness vs performance)
4. **Recommended fix**: Which option and why
5. **Prevention**: How to avoid this class of error in new code

Do NOT:
- Suggest "check your syntax" unless there is a clear syntax error
- Suggest "restart the server" as a meaningful fix
- Guess without evidence — if you need more context, ask for it

If the error is in a Copilot-generated section of code, say so explicitly.
