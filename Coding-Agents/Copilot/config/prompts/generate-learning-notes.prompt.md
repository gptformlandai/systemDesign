---
name: Generate Learning Notes
description: Create structured, revision-ready learning notes on any technical topic
---

Create structured learning notes on:

Topic: ${input:What topic should I learn about?}

Format (follow exactly — do not skip sections):

## [Topic Name]

### What It Is
[2-3 precise sentences. Define the concept, not the use case.]

### Why It Matters
[Why does a developer need this? What breaks without it?]

### How It Works
[Internals / mechanics — numbered steps where possible]

### Key Rules
[5-7 bullet points — the most important things to remember]

### Code Example
```[language]
[Minimal but complete working example — no placeholder comments]
```

### Common Mistakes
[3-5 concrete mistakes developers make — show bad pattern then correct pattern]

### Strong Explanation
[How to explain this clearly in 3-5 sentences — for an interview or team discussion]

### Revision Questions
[5 applied questions — not "what is X?" but "what happens when Y?"]

Rules:
- Code examples must be runnable — no `// implement here` placeholders
- Prefer concrete over abstract: "Returns a list of User objects" not "Returns data"
- If language-agnostic concept: use the most natural language for the example
- If the topic has version-specific behavior: note the version
- Under 600 words total (link to docs for depth; notes are for revision)
