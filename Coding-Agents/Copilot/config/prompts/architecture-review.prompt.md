---
name: Architecture Review
description: Structured architecture review with SOLID analysis and prioritized improvements
---

Perform an architecture review of:

${selection}

Evaluate on 6 dimensions:

1. **Separation of Concerns**
   Does each class/function have one clear responsibility?

2. **Coupling**
   What is tightly coupled that should be loosely coupled?
   What changes would cascade unexpectedly?

3. **SOLID Principles**
   - Single Responsibility: does each class have one reason to change?
   - Open/Closed: can new behavior be added without modifying existing code?
   - Dependency Inversion: does code depend on abstractions or concretions?

4. **Testability**
   Can each component be unit tested in isolation?
   What makes it hard to test? (hidden dependencies, static calls, global state)

5. **Scalability**
   What breaks first under 10x load?
   Where is shared mutable state?

6. **Maintainability**
   What will be painful to change in 6 months?
   What is the blast radius if X changes?

Output format:
| Priority | Dimension | Issue | Consequence | Improvement |
|----------|-----------|-------|-------------|-------------|
| HIGH | ... | ... | ... | ... |
| MEDIUM | ... | ... | ... | ... |
| LOW | ... | ... | ... | ... |

Rules:
- Cite specific line/method for every issue
- Each improvement must be concrete and actionable
- Acknowledge trade-offs of the improvement
- Do not compliment the code — focus on improvements
- Under 400 words total
