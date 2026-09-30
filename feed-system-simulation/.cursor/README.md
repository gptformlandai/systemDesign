# Cursor configuration in this repo

This folder contains **sample** Cursor project configuration. Copy, rename, or delete pieces as needed.

## What lives where

| Config | Location | Purpose |
|--------|----------|---------|
| **Agent instructions** | [`../AGENTS.md`](../AGENTS.md) | High-level project context for the agent (architecture, commands, conventions) |
| **Rules** | [`.cursor/rules/`](rules/) | Persistent guidance — always-on or file-pattern scoped (`.mdc` files) |
| **Skills** | [`.cursor/skills/`](skills/) | Reusable workflows the agent loads when relevant (`SKILL.md` per skill) |
| **Subagents** | [`.cursor/agents/`](agents/) | Specialized agents with custom system prompts (invoked via Task tool) |
| **Slash commands** | [`.cursor/commands/`](commands/) | Prompt templates triggered with `/command-name` in chat |
| **Hooks** | [`hooks.json`](hooks.json) + [`hooks/`](hooks/) | Scripts that run on agent events (shell gate, post-edit, session start) |
| **MCP servers** | [`mcp.json`](mcp.json) | Project-scoped Model Context Protocol servers |
| **CLI overrides** | [`cli.json`](cli.json) | Cursor CLI permission/sandbox overrides for this repo |
| **Ignore list** | [`../.cursorignore`](../.cursorignore) | Files the agent should not index or read |

## User-level vs project-level

| Scope | Path | Shared via git? |
|-------|------|-----------------|
| **Project** | `.cursor/` in repo root | Yes — team shares the same config |
| **User** | `~/.cursor/` | No — personal machine only |

Examples of user-level equivalents: `~/.cursor/rules/`, `~/.cursor/skills/`, `~/.cursor/agents/`, `~/.cursor/hooks.json`.

## Quick start

1. **Rules** — Edit `.cursor/rules/*.mdc`. Set `alwaysApply: true` for global rules, or `globs` for file-specific ones.
2. **Skills** — Add a folder under `.cursor/skills/my-skill/SKILL.md`. Mention the skill in chat or let the agent discover it from the description.
3. **Subagents** — Add `.cursor/agents/my-agent.md`, then ask: “Use the my-agent subagent to …”
4. **Hooks** — Edit `hooks.json`, make scripts executable (`chmod +x .cursor/hooks/*.sh`), test in **Settings → Hooks** or the Hooks output channel.
5. **MCP** — Uncomment/edit `mcp.json`, then enable the server in **Cursor Settings → MCP**.
6. **Commands** — Type `/review-changes` (or your command name) in Agent chat.

## Cursor Automations

**Automations** (scheduled agents, PR triggers, Slack workflows) are configured in the Cursor UI (**Automations** tab), not as repo files. Use them for recurring tasks like “on every PR, run a review agent.”

## Further reading

- [Cursor Docs — Rules](https://docs.cursor.com/context/rules)
- [Cursor Docs — Agent Skills](https://docs.cursor.com/context/skills)
- [Cursor Docs — Hooks](https://docs.cursor.com/agent/hooks)
