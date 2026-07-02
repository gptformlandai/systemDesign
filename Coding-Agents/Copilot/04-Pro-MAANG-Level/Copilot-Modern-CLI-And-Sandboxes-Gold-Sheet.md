# Copilot Modern CLI And Sandboxes Gold Sheet

> **Track**: Copilot Mastery Track - Group 4: Pro / MAANG Level
> **File**: 27b
> **Audience**: Developers moving from command suggestions to terminal-native agentic workflows

---

## Practical Impact Meter

5/5 - The CLI becomes your terminal agent: it can reason, edit, run tools, manage GitHub work, and operate in safer execution boundaries.

---

## 1. Intuition

Old mental model:

> "Copilot CLI suggests shell commands."

Modern mental model:

> "Copilot CLI is an agentic command-line teammate that can work with files, tools, GitHub state, sessions, custom agents, MCP, hooks, and sandboxes."

The legacy `gh copilot explain/suggest` flow is still useful for command help. The newer Copilot CLI platform is broader: it can run interactive sessions, automate tasks, roll back changes, use custom agents, and work with local or cloud execution controls.

---

## 2. Definition

- **Definition:** GitHub Copilot CLI is an agentic command-line interface for asking questions, changing code, running commands, interacting with GitHub, and automating agent sessions.
- **Category:** Terminal-native AI coding agent.
- **Core idea:** Keep high-control engineering workflows inside the shell while still using Copilot's reasoning and tool execution.

---

## 3. Surface Comparison

| Capability | `gh copilot` Command Help | Modern Copilot CLI |
|---|---|---|
| Explain shell commands | Yes | Yes |
| Suggest shell/git/gh commands | Yes | Yes |
| Edit files | No / limited | Yes |
| Run multi-step tasks | No | Yes |
| Use custom agents | No | Yes |
| Use MCP servers | No | Yes |
| Use hooks | No | Yes |
| Programmatic execution | Limited | Yes |
| Session rollback | No | Yes |
| Sandbox support | No | Yes, where enabled |

Use `gh copilot --help` for legacy command-help workflows and the current `copilot --help` / official CLI docs for the agentic CLI commands available in your installed version.

---

## 4. When CLI Beats IDE

Use CLI when:

- you are already in terminal flow
- task involves Git, GitHub, shell, CI, or local scripts
- you need repeatable automation
- you are SSH'd into a development box
- you want to run agentic work from a script or scheduled job
- you need a clear transcript of commands and decisions

Use IDE when:

- you need visual code navigation
- you want interactive diff review across many files
- UI feedback is faster than terminal output
- you are teaching or learning code with selections and symbols

---

## 5. Safe CLI Operating Loop

```md
1. Check repo state
   - git status
   - confirm no unrelated dirty files

2. Create a checkpoint
   - commit or stash before agentic changes

3. Start a bounded task
   - describe goal, scope, constraints, tests

4. Let CLI inspect and plan
   - ask for plan before file edits

5. Approve tools deliberately
   - read command intent before allowing

6. Review file changes
   - git diff
   - run tests yourself if needed

7. Commit only explainable code
   - never commit generated code you cannot explain
```

---

## 6. Programmatic CLI Use

Programmatic CLI mode is powerful but risky because there may be no human prompt before a tool call.

Use it for:

- nightly issue triage summaries
- changelog drafts
- test failure diagnosis
- PR description generation
- dependency update notes
- local report generation

Avoid it for:

- automatic merges
- production deploys
- credential rotation
- database changes
- broad file rewrites

Safe automation pattern:

```md
Task:
Generate a report only. Do not modify files.

Allowed:
- Read repository files.
- Read GitHub issue/PR metadata if available.

Not allowed:
- Do not write files.
- Do not run package install.
- Do not push branches.
- Do not call external services except GitHub.

Output:
- Markdown report to stdout.
- Include assumptions and missing data.
```

---

## 7. Sandboxes

Sandboxes are isolated environments where Copilot can interact with code, tools, filesystem, and network resources with reduced blast radius.

| Sandbox | Mental Model | Best For |
|---|---|---|
| Local sandbox | Guardrails around local execution | Running CLI safely near local files |
| Cloud sandbox | Ephemeral isolated Linux environment | Background or remote agent tasks |

Sandbox rules:

- isolation reduces risk; it does not remove review responsibility
- treat network access as explicit policy, not default trust
- never expose broad local filesystem roots
- do not pass production secrets unless the workflow is designed and approved for them
- make test commands deterministic so sandbox results are meaningful

---

## 8. CLI Permission Strategy

| Tool Action | Default Stance | Reason |
|---|---|---|
| Read source files | Allow in trusted repo | Needed for context |
| Edit scoped files | Ask/approve | Prevent surprise rewrites |
| Run tests | Allow after review | Validates changes |
| Install packages | Ask/deny by default | Supply chain and drift risk |
| Modify lockfiles | Ask | Can have wide effects |
| Delete files | Ask/deny by default | Destructive |
| Push branch | Ask | Remote side effect |
| Create PR | Ask | Visible team action |
| Call external service | Deny unless approved | Data exfiltration risk |

---

## 9. Hooks In CLI

Hooks turn local CLI work into a controlled workflow.

High-value CLI hooks:

- **preToolUse:** block destructive shell commands or writes to forbidden paths
- **postToolUse:** run formatting or report test results after edits
- **userPromptSubmitted:** inject reminders like "do not use production data"
- **sessionEnd:** summarize changed files and tests run
- **permissionRequest:** decide allow/deny for non-interactive CLI automation

Hook design rule:
- hooks should be small, deterministic, and auditable
- if a hook blocks something, the denial message should tell the agent how to proceed safely

---

## 10. CLI Task Templates

### Shell-first debugging

```md
Investigate this failing command.

Command:
[command]

Failure:
[relevant output]

Rules:
- Explain likely root causes first.
- Do not modify files until I approve the plan.
- If suggesting commands, explain risk level for each.
```

### Safe code edit from terminal

```md
Implement [small change].

Scope:
- Allowed files: [paths]
- Do not modify: [paths]

Plan first:
- Files to inspect
- Files to edit
- Test command

After edits:
- Show changed files
- Summarize behavior
- Report tests run
```

### PR follow-up from terminal

```md
Review the current branch diff and prepare a PR summary.

Do not edit files.
Include:
- Problem
- Solution
- Tests
- Risks
- Rollback plan
```

---

## 11. Failure Modes

| Failure Mode | Symptom | Fix |
|---|---|---|
| Tool overreach | CLI edits beyond scope | Add path allowlist and preToolUse hook |
| Hidden dirty state | Agent mixes your old changes with new ones | Check `git status` and checkpoint first |
| Automation loop | Programmatic mode repeats failing action | Add retry limit and stop condition |
| Risky command | Agent suggests destructive shell | Require explanation and hook blocklist |
| Sandbox false confidence | Works in sandbox, fails locally | Document environment differences |
| Session drift | Long terminal session loses original goal | Restart with a compact summary |

---

## 12. Interview Answer

> "I use Copilot CLI for terminal-native agent workflows: GitHub tasks, shell-heavy debugging, automation, and small code changes where the terminal gives me better control than the IDE. I keep a checkpoint before edits, require plan-first prompts, use hooks for policy checks, prefer sandboxed execution for risky work, and treat programmatic mode as read-only unless the workflow has explicit approvals and rollback."

---

## 13. Revision Notes

- One-line summary: Modern Copilot CLI is an agent platform, not just a command suggester.
- Three keywords: terminal agent, sandbox, rollback.
- One interview trap: using CLI automation to modify code without review gates.
- Memory trick: "CLI for control, IDE for visual review, cloud agent for delegated PR work."

---

## Official Source Anchors

- https://docs.github.com/en/copilot/concepts/agents/copilot-cli/about-copilot-cli
- https://docs.github.com/en/copilot/concepts/about-cloud-and-local-sandboxes
- https://docs.github.com/en/copilot/reference/hooks-reference
