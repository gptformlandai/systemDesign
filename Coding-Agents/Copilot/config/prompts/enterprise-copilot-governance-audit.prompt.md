---
name: Enterprise Copilot Governance Audit
description: Review a repo or workflow for enterprise Copilot readiness, MCP risk, agentic controls, and cost visibility
---

Perform an enterprise Copilot governance audit for:

${input:scope}

Use any attached repository files, policy notes, MCP configs, instruction files, or workflow docs.

Assess:

1. Access and policy readiness
2. Model and feature controls
3. Content exclusion and sensitive data handling
4. MCP server/toolset risk
5. Cloud agent readiness
6. CLI/sandbox usage
7. Hooks and guardrails
8. Metrics, budgets, and auditability
9. Developer onboarding and review discipline

Output:

```md
## Governance Readiness Score
[0-100 with rationale]

## Critical Risks
- 

## High Priority Actions
- 

## Medium Improvements
- 

## Quick Wins
- 

## Rollout Recommendation
Pilot / Limited rollout / Broad rollout / Blocked

## Evidence Used
- 
```

Rules:
- Be specific and evidence-based.
- Do not assume a policy exists if it is not provided.
- Separate missing evidence from confirmed risk.
- Do not recommend write-capable MCP tools unless a human approval gate exists.

