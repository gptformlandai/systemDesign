# Copilot MCP, Registry, Toolsets, And Repository Indexing Gold Sheet

> **Track**: Copilot Mastery Track - Group 4: Pro / MAANG Level
> **File**: 27e
> **Audience**: Developers who want Copilot to use the right external context without turning every tool into a security risk

---

## Practical Impact Meter

5/5 - MCP and repository indexing decide what Copilot can know and do beyond the current prompt.

---

## 1. Intuition

Copilot has three main context expansion paths:

| Context Path | What It Adds | Risk |
|---|---|---|
| Repository indexing | Semantic search over code | Wrong/stale matches or excluded content gaps |
| MCP | External tools and data sources | Tool misuse, data exposure, broad permissions |
| Spaces / shared context | Curated docs and conversation context | Stale or misleading shared knowledge |

Pro users do not just "add more context." They curate context and constrain tools.

---

## 2. MCP Definition

MCP is an open standard for connecting AI systems to external data sources and tools.

In Copilot workflows, MCP can expose:

- GitHub issues, PRs, code search, and repo metadata
- databases or schemas
- internal docs
- ticketing systems
- observability tools
- design systems
- cloud inventory
- test management systems

MCP turns Copilot from "answering from text" into "working with connected systems."

---

## 3. MCP Architecture

```md
Copilot surface
  -> MCP client
    -> configured MCP server
      -> tools/resources/prompts
        -> external system
```

Key terms:

- **Server:** exposes tools/resources to Copilot.
- **Tool:** action Copilot may call.
- **Resource:** data Copilot may read.
- **Toolset:** curated subset of tools for a purpose.
- **Registry:** approved catalog of MCP servers.
- **Allowlist:** policy that controls which servers may be used.

---

## 4. GitHub MCP Server

The GitHub MCP Server is the natural first MCP server for Copilot because many workflows need GitHub context:

- summarize issues
- inspect PRs
- read discussions
- search code
- create or update issues
- work with pull requests
- connect cloud agent workflows

Recommended starting policy:

```md
Phase 1:
- Read-only GitHub MCP tools.
- Fine-grained token.
- Repos limited to pilot projects.

Phase 2:
- Allow issue/PR write tools only for maintainers.
- Keep branch pushes and merge actions behind human approval.

Phase 3:
- Add workflow-specific toolsets.
- Audit usage and errors.
```

---

## 5. Repository MCP Config

Good committed config:

```md
config/mcp.example.json
```

Bad committed config:

```md
.vscode/mcp.json with real tokens or personal local paths
```

Rules:

- commit examples, not real secrets
- use environment variables for credentials
- keep filesystem paths narrow
- prefer read-only tools first
- document setup and required permissions
- review every server before enabling it in agent workflows

---

## 6. Toolset Design

Do not expose every tool just because the server has it.

| Workflow | Useful Tools | Usually Exclude |
|---|---|---|
| PR review | read PR, read files, comment draft | merge, force push |
| Issue triage | read issues, label suggestions | close/delete |
| Incident analysis | read logs/metrics | mutate prod resources |
| DB schema help | read schema | write/query production rows |
| Release prep | read tags/releases | publish release without approval |

Principle:
- if the agent only needs read context, do not give write tools.

---

## 7. Repository Indexing

Repository indexing lets Copilot use semantic search over repository content.

Useful for:

- "where is auth enforced?"
- "find code related to payment retries"
- "which files implement order cancellation?"
- cloud agent discovering relevant code without exact names

Indexing is not a replacement for explicit context:

| Situation | Best Context |
|---|---|
| You know exact file | `#file` |
| You selected exact function | `#selection` |
| You know symbol name | `#sym` |
| You need discovery | `#codebase` / repository index |
| Agent needs broad repo research | cloud agent semantic search |

---

## 8. Content Exclusion

Content exclusion tells Copilot to ignore configured files or paths where supported.

Use for:

- secrets
- generated dumps
- customer data samples
- private algorithms
- production-only config
- files with legal/regulatory constraints

Important:
- do not rely on content exclusion as the only control
- support differs across surfaces/modes
- keep secrets out of repos entirely where possible
- combine with secret scanning and hooks

---

## 9. MCP Security Checklist

```md
[ ] What system does the server access?
[ ] What exact tools are enabled?
[ ] Does any tool write, delete, publish, or trigger external actions?
[ ] What credentials does it use?
[ ] Are credentials fine-grained and revocable?
[ ] Is the server approved by org/enterprise policy?
[ ] Are logs/audit records available?
[ ] What happens if the server returns wrong/stale data?
[ ] Can the agent exfiltrate sensitive data through this tool?
[ ] Is there a human approval step before side effects?
```

---

## 10. Failure Modes

| Failure Mode | Symptom | Fix |
|---|---|---|
| Too many tools | Agent chooses risky/irrelevant tool | Narrow toolsets |
| Overprivileged token | Read task has write permissions | Fine-grained read-only token |
| Stale index | Copilot misses recent changes | Refresh context or attach exact file |
| Exclusion mismatch | Sensitive file appears in unsupported mode | Add repo hygiene + hooks + training |
| Server name bypass | Allowlist depends on ID/name only | Prefer registry and disable MCP if risk is high |
| External data trust | Agent trusts ticket/log as truth | Ask for evidence and cross-check code |

---

## 11. Strong MCP Prompt

```md
Use MCP only for read context.

Goal:
Summarize open PRs related to authentication and identify risky changes.

Allowed:
- Read PR metadata.
- Read changed files.
- Read CI status.

Not allowed:
- Do not comment, merge, close, label, or push.

Output:
- PR number/title
- Risk level
- Files involved
- Suggested reviewer
- Reasoning
```

---

## 12. Interview Answer

> "I treat MCP as tool access, not just extra context. I start with read-only servers, fine-grained credentials, registry/allowlist controls, and workflow-specific toolsets. For code context I prefer repository indexing for discovery but exact file references for implementation. I also use content exclusion, hooks, and review gates because support differs by surface and MCP can create real side effects."

---

## 13. Revision Notes

- One-line summary: MCP gives Copilot tools; repository indexing gives it semantic repo context.
- Three keywords: least privilege, toolsets, semantic search.
- One interview trap: exposing write-capable MCP tools for read-only tasks.
- Memory trick: "Context is what Copilot knows; tools are what Copilot can do."

---

## Official Source Anchors

- https://docs.github.com/en/copilot/concepts/context/mcp
- https://docs.github.com/en/copilot/concepts/context/repository-indexing
- https://docs.github.com/en/copilot/concepts/context/content-exclusion
- https://docs.github.com/en/copilot/reference/mcp-allowlist-enforcement
