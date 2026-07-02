# Copilot Feature Matrix, Surfaces, And Integrations Gold Sheet

> **Track**: Copilot Mastery Track - Group 4: Pro / MAANG Level
> **File**: 27f
> **Audience**: Developers who need to know which Copilot capability belongs on which surface

---

## Practical Impact Meter

4/5 - Knowing the surface matrix prevents bad workflow choices, stale assumptions, and "why does this work in VS Code but not here?" confusion.

---

## 1. Intuition

Copilot is no longer one feature.

It is a platform with multiple surfaces:

- IDEs
- GitHub.com
- GitHub Mobile
- terminal / CLI
- cloud agent
- code review
- Copilot app
- Spark
- Agentic Workflows
- SDK and third-party agents

The pro skill is not memorizing every checkbox. It is knowing how to verify support and pick the right surface.

---

## 2. Surface Map

| Surface | Best For | Watch Out |
|---|---|---|
| VS Code | Fastest end-to-end Copilot experience, Agent Mode, prompt files, instructions, MCP | Feature set changes quickly; extension version matters |
| Visual Studio | .NET-heavy workflows, enterprise IDE usage | Not always same feature depth as VS Code |
| JetBrains | Java/Kotlin/Python enterprise teams | Some features may be preview or lag VS Code |
| Eclipse | Legacy Java teams | Smaller feature surface |
| Xcode | Apple platform development | Feature support differs from VS Code |
| NeoVim | Keyboard-first coding | Usually narrower Copilot feature set |
| GitHub.com | PRs, issues, cloud agent, code review, repo chat | Less local filesystem control |
| GitHub Mobile | Lightweight review and agent task kickoff | Not ideal for deep code review |
| Copilot CLI | Terminal-native tasks and automation | Tool approvals and sandboxing matter |
| Cloud agent | Background issue/PR implementation | Requires strong task framing and review |
| Copilot app | App-like agent workspace and sessions | Treat as agentic surface, not plain chat |
| Spark | Fast app prototyping/deployment workflows | Governance and production-readiness review still apply |

---

## 3. Feature Categories

| Category | Examples |
|---|---|
| Completion | inline suggestions, next edit suggestions |
| Chat | ask, explain, debug, codebase Q&A |
| Editing | inline chat, edits, multi-file changes |
| Agentic | Agent Mode, cloud agent, CLI, agent sessions |
| Review | PR summaries, Copilot code review, AI code review prompts |
| Customization | instructions, prompt files, custom agents, Memory |
| Context | repository indexing, Spaces, MCP, images where supported |
| Governance | policies, content exclusion, audit logs, budgets, model controls |
| Extensibility | MCP, SDK, plugins, third-party coding agents, agent apps |

---

## 4. Capability Verification Protocol

When a feature does not appear:

```md
1. Check plan/license
2. Check organization/enterprise policy
3. Check IDE support matrix
4. Check extension version
5. Check feature preview/beta status
6. Check repository trust/workspace state
7. Check network/VPN/firewall
8. Check logs
9. Check official docs for current support
```

Do not assume a teammate has the same feature just because you do. Policy, plan, IDE, extension version, and seat source can differ.

---

## 5. Model Selection Awareness

Model availability changes by plan, policy, region, surface, and time.

Strong default:

| Task | Model Strategy |
|---|---|
| Simple explanation | fast/default model |
| Normal code generation | balanced default |
| Architecture trade-off | stronger reasoning model |
| Security-sensitive review | strongest available + human specialist |
| Long autonomous agent task | model chosen for reliability/cost balance |
| Enterprise rollout | admin-managed model availability and defaults |

Do not write permanent team docs that say "always use model X" without a review date. Write "use the strongest approved reasoning model available for this class of task."

---

## 6. Integrations To Know

| Integration | Why It Matters |
|---|---|
| GitHub MCP Server | Connects Copilot to GitHub issues, PRs, code, and workflows |
| Jira/Linear/Azure Boards | Turns product tickets into agent tasks when approved |
| Slack/Teams | Lets teams start or steer agent work from collaboration tools |
| GitHub Actions | Powers cloud agent execution and validation |
| Copilot SDK | Lets platform teams build custom agentic experiences |
| Third-party coding agents | Expands agent ecosystem but increases governance need |
| Agent apps | Connect agent workflows to specialized surfaces |

---

## 7. Surface Choice Playbook

| Task | First Choice | Why |
|---|---|---|
| Explain selected code | IDE Chat | Selection context is precise |
| Fix one small bug | Inline Chat or Edits | Human review is immediate |
| Refactor 3 known files | Edits | Explicit working set |
| Implement issue in background | Cloud agent | Runs as branch/PR workflow |
| Diagnose failing shell command | CLI | Terminal context |
| Summarize PR risk | GitHub.com or CLI | PR metadata is native |
| Build repeatable workflow | CLI automation or SDK | Scriptable |
| Share team context | Spaces/instructions | Reusable |
| Query external system | MCP | Tool/data connection |
| Prototype small app | Spark or IDE agent | Fast iteration |

---

## 8. Common Surface Mistakes

| Mistake | Why It Hurts | Better Choice |
|---|---|---|
| Agent Mode for a one-line fix | Slower, more risk | Inline Chat |
| Chat Ask for multi-file change | Manual copy/paste drift | Edits or Agent Mode |
| Cloud agent for vague request | Low-quality PR | Write a real issue first |
| MCP for known local file | Extra risk and latency | Attach `#file` |
| Codebase search for exact function | Noisy context | `#sym` or `#selection` |
| Assuming VS Code docs apply to all IDEs | Feature mismatch | Check feature matrix |

---

## 9. Feature Matrix Review Cadence

For personal use:
- review official feature matrix monthly
- update prompt library when new surfaces appear
- retire stale model/plan assumptions

For teams:
- review quarterly with platform/security
- update onboarding docs
- re-test custom agents and hooks
- confirm MCP registry/allowlist
- document feature changes in release notes

---

## 10. Interview Answer

> "I think of Copilot as a multi-surface platform. I use IDE Chat and Edits for local precision, Agent Mode for local multi-file work, cloud agent for issue-sized background PRs, CLI for terminal and automation, MCP for approved external context, and GitHub.com for PR/review workflows. Before rolling features to a team, I verify plan, policy, IDE support, extension version, and governance controls instead of assuming every surface behaves the same."

---

## 11. Revision Notes

- One-line summary: Choose the Copilot surface based on task shape and support matrix.
- Three keywords: surface, policy, verification.
- One interview trap: saying "Copilot supports X" without naming the surface.
- Memory trick: "IDE for local code, GitHub for PRs/issues, CLI for terminal, cloud for delegated tasks."

---

## Official Source Anchors

- https://docs.github.com/en/copilot/reference/copilot-feature-matrix
- https://docs.github.com/en/copilot/reference/custom-instructions-support
- https://docs.github.com/en/copilot
