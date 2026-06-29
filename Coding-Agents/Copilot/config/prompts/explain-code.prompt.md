---
name: Explain Code
description: Explain selected code with purpose, logic, patterns, and gotchas
---

Explain the following code to a developer who is familiar with the tech stack but may not know this specific module:

${selection}

Structure your explanation:

1. **One-line summary**: What does this do in plain English?
2. **Why it exists**: What problem does it solve in this codebase?
3. **How it works**: Step-by-step walkthrough of the logic
4. **Design patterns**: Name any patterns used (e.g., context manager, strategy, factory, observer)
5. **Gotchas**: Surprising behavior, edge cases, or common mistakes with this code
6. **Dependencies**: What does this rely on that is not visible in the selection?

Rules:
- Use "This does X" not "This is used to X"
- Reference specific variable/method names from the code
- If there is ambiguous logic: say so, don't guess
- Keep total response under 400 words unless the code is very complex
