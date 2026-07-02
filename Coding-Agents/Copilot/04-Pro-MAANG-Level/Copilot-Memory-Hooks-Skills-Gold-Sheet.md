# Copilot Memory, Hooks, And Skills Gold Sheet

> **Track**: Copilot Mastery Track - Group 4: Pro / MAANG Level
> **File**: 27c
> **Audience**: Developers who want Copilot to behave consistently across sessions and enforce local workflow rules

---

## Practical Impact Meter

5/5 - Memory makes Copilot less repetitive, hooks make it safer, and skills make repeatable expertise portable.

---

## 1. Intuition

Three layers:

| Layer | Mental Model | Main Value |
|---|---|---|
| Memory | "What Copilot has learned over time" | Reduces repeated context |
| Hooks | "Automation gates around agent actions" | Enforces safety and compliance |
| Skills | "Packaged know-how for a domain/workflow" | Reusable specialist procedures |

Instructions tell Copilot what to do. Memory helps it remember stable facts. Hooks control what it may do. Skills teach it how to do recurring work.

---

## 2. Copilot Memory

Copilot Memory can store repository-level facts and user-level preferences.

Repository-level examples:
- build command is `pnpm test:unit`
- service modules use repository pattern
- API errors must use `ProblemDetails`
- migrations live under `db/migrations`
- frontend state uses Zustand, not Redux

User-level preference examples:
- "show diffs before explanation"
- "prefer concise answers"
- "always include tests in implementation plans"

Memory is useful because many agent mistakes come from forgetting stable repo conventions.

---

## 3. Memory Vs Instructions

| Use | Put In Instructions | Let Memory Learn |
|---|---|---|
| Non-negotiable rule | Yes | Maybe |
| Security constraint | Yes | No, make it explicit |
| Build/test command | Yes if critical | Yes |
| Personal response style | Optional | Yes |
| Temporary feature context | No | No, use prompt/session |
| Architecture decision | Yes if important | Yes |

Rule:
- If violating it could break production, put it in instructions.
- If it is a helpful stable fact, Memory can reduce repeated prompting.

---

## 4. Memory Hygiene

Good memory facts are:

- stable
- specific
- short
- repo-scoped when repo-specific
- easy to verify

Bad memory facts:

- "This feature is almost done"
- "Always use this temporary branch"
- "User token is ..."
- "Ignore tests for now"
- "The API is probably deprecated"

Memory review checklist:

```md
[ ] Does this memory still match the repo?
[ ] Is it a rule or just a temporary observation?
[ ] Should it be promoted to instructions?
[ ] Is it too vague to help?
[ ] Could it leak sensitive information?
```

---

## 5. Hooks

Hooks run custom commands at key points in an agent workflow.

Common hook moments:

- before a tool is used
- after a tool is used
- when a session starts or ends
- when a user prompt is submitted
- before context compaction
- when a permission decision is requested in CLI workflows

Hooks can:

- block dangerous tool calls
- run secret scanning
- inject extra context
- log actions for audit
- validate file paths
- summarize test results
- enforce team policy

---

## 6. Hook Placement

| Location | Applies To | Use |
|---|---|---|
| `.github/hooks/*.json` | Repository/cloud agent workflows | Team-shared guardrails |
| user-level hook directory | Personal CLI workflows | Individual preferences and local protections |

Repository hooks should be conservative and team-reviewed. Personal hooks can be more opinionated.

---

## 7. Hook Design Patterns

### Secret scan before write or shell

Goal:
- prevent accidental credential leakage before agent writes files or runs risky commands

Behavior:
- inspect proposed tool input
- deny if token-like strings or forbidden paths appear
- return a clear reason and safe next step

### Test report after edit

Goal:
- remind the agent and user what validation is still required

Behavior:
- after edit tools, return a note like "Run tests before claiming done"
- optionally run a cheap command if deterministic

### Dependency install gate

Goal:
- prevent supply-chain drift

Behavior:
- deny package installation unless the prompt explicitly allows dependency changes

### Forbidden path gate

Goal:
- protect secrets, generated files, lockfiles, infra, or production config

Behavior:
- deny edits to `.env`, credentials, production manifests, or migration files unless explicitly allowed

---

## 8. Hook Output Rules

Good denial:

```md
Denied: this command modifies `.env`.
Safe next step: propose the required variable name and update `.env.example` only.
```

Bad denial:

```md
No.
```

Hooks are part of the agent conversation. A helpful denial lets the agent recover safely.

---

## 9. Agent Skills

Skills package repeatable expertise. Use them when you want a procedure, checklist, or domain method that can be reused across agents and sessions.

Good skill candidates:

- release management
- incident analysis
- migration playbook
- accessibility audit
- API backward compatibility review
- database migration safety review
- PR triage
- dependency upgrade process

Do not use skills for:

- one-off notes
- secrets
- vague style preferences
- things better expressed as repo instructions

---

## 10. Memory + Hooks + Skills Together

Example: release workflow

| Layer | Responsibility |
|---|---|
| Instructions | "Never publish releases from unreviewed branches." |
| Memory | "This repo uses Changesets and release branches named release/*." |
| Skill | "Release checklist: changelog, version bump, CI, tag, rollback notes." |
| Hook | "Deny publish command unless branch and CI conditions are satisfied." |

This is the pro pattern: knowledge, procedure, and enforcement are separated.

---

## 11. Failure Modes

| Failure Mode | Symptom | Fix |
|---|---|---|
| Stale memory | Copilot keeps using old command/path | Review and delete/update memory |
| Rule only in memory | Agent violates security constraint | Move rule into instructions and hook |
| Overblocking hook | Agent cannot complete normal task | Make matcher/path rule narrower |
| Silent hook | Agent does not understand denial | Return actionable message |
| Skill bloat | Skill becomes a full textbook | Keep skill procedural and compact |
| Conflicting layers | Instructions say one thing, memory implies another | Instructions win; clean memory |

---

## 12. Strong Hook/Skill Prompt

```md
Create a repository guardrail for Copilot agent work.

Goal:
- Prevent edits to secrets and production-only config.

Rules:
- Block `.env`, `.pem`, `.key`, `prod*.yaml`, and `secrets/**`.
- Allow `.env.example`.
- Return a helpful denial message with safe alternative.

Output:
- Hook JSON.
- Explanation of which event it uses.
- Two examples: allowed and denied.
```

---

## 13. Interview Answer

> "I separate Copilot customization into memory, hooks, and skills. Instructions contain non-negotiable rules. Memory captures stable repo facts and preferences. Skills package repeatable procedures like releases or migrations. Hooks enforce policy at runtime by blocking unsafe tool calls, checking secrets, and logging important actions. This gives us speed without depending on prompt discipline alone."

---

## 14. Revision Notes

- One-line summary: Memory remembers, hooks enforce, skills teach.
- Three keywords: stable facts, runtime guardrails, packaged procedures.
- One interview trap: putting security rules only in Memory.
- Memory trick: "Instructions are policy; Memory is recall; Hooks are brakes; Skills are playbooks."

---

## Official Source Anchors

- https://docs.github.com/en/copilot/concepts/agents/copilot-memory
- https://docs.github.com/en/copilot/concepts/agents/hooks
- https://docs.github.com/en/copilot/reference/hooks-reference
