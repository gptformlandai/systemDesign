---
name: Modernize Code
description: Upgrade code to current language idioms, APIs, and patterns
---

Modernize the following code to current best practices:

${selection}

Language/Framework: ${input:What language and version? (e.g., Python 3.12, TypeScript 5, Java 21, React 19)}

Modernization goals:
${input:What aspects to modernize? (e.g., replace callbacks with async/await, use dataclasses, remove deprecated APIs, adopt new syntax)}

Rules:
- Do NOT change the public API or method signatures
- Do NOT change the observable behavior — same inputs must produce same outputs
- Do NOT add dependencies that don't already exist in the project
- Do NOT over-engineer — apply only idioms the language community uses widely

For each change made, explain:
- What was changed
- Why it's more modern/idiomatic
- Any edge case behavior that changed (even if subtle)

Output:
1. Modernized code (complete function/class — not a diff)
2. Bullet list: what changed + why
3. Breaking change risk: NONE / LOW / MEDIUM — with explanation

Do NOT include:
- Changes that are purely style with no practical benefit
- Rewrites to a different framework
- New features not in the original
