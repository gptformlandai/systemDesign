---
name: Cloud Agent Task
description: Convert a vague task into a cloud-agent-ready issue with scope, validation, and review gates
---

Turn this request into a Copilot cloud-agent-ready issue:

${input:task}

Output:

```md
## Goal

## User-visible behavior
- 

## Scope
Allowed:
- 

Not allowed:
- 

## Existing patterns to follow
- 

## Validation
- 

## Review notes
- 
```

Rules:
- Make acceptance criteria testable.
- Keep the task small enough for one reviewable PR.
- Include at least one explicit non-goal.
- Include a test or validation command field; write `UNKNOWN` if the command is not known.
- Do not invent production credentials, customer data, or hidden requirements.
