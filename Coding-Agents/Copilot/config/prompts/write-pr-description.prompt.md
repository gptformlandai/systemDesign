---
name: Write PR Description
description: Generate a structured, reviewer-friendly PR description from your changes
---

Generate a GitHub PR description for my changes.

What I changed:
${input:Describe what you changed in 2-3 sentences — Copilot will format it properly}

Changed files (for context):
${selection}

Format exactly:

## Summary
[2-3 sentences: what changed and WHY — business or technical reason]

## Changes Made
- [Specific change 1 — one bullet per meaningful change]
- [Specific change 2]
- [...]

## How to Test
1. [Step 1: copy-paste ready command or action]
2. [Step 2]
3. [Expected result: what reviewers should see]

## Screenshots / Examples
[Skip if not a UI change. Include before/after if visual]

## Breaking Changes
[None — OR: describe what breaks and provide migration steps]

## Checklist
- [ ] Tests added or updated
- [ ] Security implications considered
- [ ] Documentation updated if needed
- [ ] No hardcoded values or credentials
- [ ] CI passing

Rules:
- Under 300 words
- Factual — no marketing language or "improved" without specifics
- "Fixed crash when X" not "Fixed NullPointerException"
- Every test step must be runnable by someone who didn't write the code
