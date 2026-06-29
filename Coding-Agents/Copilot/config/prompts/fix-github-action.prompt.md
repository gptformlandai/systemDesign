---
name: Fix GitHub Action
description: Diagnose and fix a broken GitHub Actions workflow
---

Diagnose and fix this broken GitHub Actions workflow.

Workflow section that is failing:
${selection}

Error from the Actions log:
${input:Paste the exact error from the GitHub Actions log — not a description, the actual text}

What this workflow should do:
${input:Briefly describe what the workflow is supposed to accomplish}

Diagnose:
1. **Root cause**: what is actually broken? (be specific — not "the step failed")
2. **Why it fails**: the underlying reason (version mismatch, wrong path, missing secret, etc.)
3. **Fix**: the corrected YAML for the failing step(s) only — not the whole workflow
4. **Prevention**: how to avoid this type of failure in future workflows

Rules:
- Show only the fixed section (not the full workflow unless it needs a structural change)
- Pin any action versions in the fix (never use @latest)
- If the fix requires a new GitHub Secret: name it and explain what value to set
- If the fix requires environment changes: describe exactly what needs to be set where
