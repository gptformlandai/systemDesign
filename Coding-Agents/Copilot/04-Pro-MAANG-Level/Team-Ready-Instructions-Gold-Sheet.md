# Team-Ready Instructions — Gold Sheet

> **Track**: Copilot Mastery Track — Group 4: Pro / Production Level
> **File**: 6 of 7 (Track File #26)
> **Audience**: Developers setting up Copilot instructions for teams and shared repos
> **Read after**: Agent-Governance-Output-Evaluation-Gold-Sheet.md

---

## 1. Practical Impact Meter

| Topic | Impact | Why Teams Skip This |
|---|---|---|
| Monorepo instruction strategy | ★★★★★ | Global instructions conflict with domain-specific rules; path-specific solves it |
| Multi-language repo strategy | ★★★★★ | One instruction file that covers Python AND TypeScript AND YAML is too long and inconsistent |
| Team onboarding — new developer setup checklist | ★★★★★ | New devs spend hours figuring out why Copilot behaves differently on their machine |
| Instruction ownership and review process | ★★★★☆ | Instructions that nobody reviews become stale and incorrect within months |
| Generic vs project-specific prompt design | ★★★★☆ | Prompts with hardcoded class names only work in one project |
| Portable workspace — clone and go | ★★★★☆ | Team workspaces should work identically on any developer's machine |
| Conflict resolution between instruction sources | ★★★★☆ | Instructions that contradict produce random behavior |

---

## 2. Monorepo Instruction Strategy

### The Problem

```
A monorepo with:
  backend/     Python FastAPI service
  frontend/    React/TypeScript SPA
  mobile/      React Native app
  infra/       Terraform + Kubernetes
  shared/      Shared TypeScript contracts

One copilot-instructions.md that tries to cover all of these:
  - Becomes too long (Copilot follows less of it)
  - Contradicts itself (Python rules clash with TypeScript rules)
  - Confuses developers (which rules apply here?)
```

### The Solution — Layered Instruction Architecture

```
.github/
  copilot-instructions.md              ← Cross-cutting team rules ONLY
  instructions/
    python.instructions.md             ← applyTo: "backend/**/*.py"
    typescript.instructions.md         ← applyTo: "frontend/**/*.ts", "frontend/**/*.tsx"
    react.instructions.md              ← applyTo: "frontend/**/*.tsx", "mobile/**/*.tsx"
    testing.instructions.md            ← applyTo: "**/*.test.*", "**/tests/**"
    github-actions.instructions.md     ← applyTo: ".github/workflows/**"
    security.instructions.md           ← applyTo: "**" (applies everywhere)
    terraform.instructions.md          ← applyTo: "infra/**/*.tf"

AGENTS.md               ← root: team-wide agent behavior
backend/AGENTS.md       ← backend-specific: async patterns, DB rules
frontend/AGENTS.md      ← frontend-specific: a11y, state management
infra/AGENTS.md         ← infra-specific: never apply migrations, plan first
```

### Root `copilot-instructions.md` for a Monorepo

```markdown
# [Team/Company Name] Monorepo

## What This Repo Contains
- backend/: Python FastAPI service (API + business logic + DB)
- frontend/: React TypeScript SPA (user interface)  
- mobile/: React Native app (iOS + Android)
- infra/: Terraform infrastructure + Kubernetes manifests
- shared/: Shared TypeScript type contracts (used by frontend, mobile, backend)

## Cross-Cutting Rules (apply everywhere)
- All secrets via environment variables only. Never hardcode credentials.
- All inter-service contracts must match the types in shared/.
- Test coverage must not decrease below current baseline.
- CHANGELOG.md must be updated for every user-facing change.

## Directory Quick Reference
For code in backend/: follow backend/AGENTS.md and python.instructions.md
For code in frontend/: follow frontend/AGENTS.md and react.instructions.md  
For code in infra/: follow infra/AGENTS.md and terraform.instructions.md

## Do NOT (everywhere)
- Do not add console.log() or print() for debugging in committed code
- Do not hardcode URLs — use environment-based configuration
- Do not merge without running relevant tests
```

---

## 3. Multi-Language Instruction Files

### `typescript.instructions.md`

```markdown
---
applyTo: "**/*.ts,**/*.tsx"
---
# TypeScript Conventions

## Language Version
TypeScript 5.x strict mode. Enable: strictNullChecks, noImplicitAny.
Use: `const` by default, `let` only when reassignment needed.
Never use: `any` without a // TODO: remove any comment.
Use: `unknown` instead of `any` for truly unknown types.

## Async
Use `async/await` over `.then().catch()` for readability.
Always handle promise rejections (try/catch or .catch()).
Never use `new Promise()` wrapper around already-async functions.

## Imports
Use named exports. Avoid default exports (they complicate refactoring).
Order: React imports first, third-party second, local absolute third, relative last.
Use path aliases defined in tsconfig.json (@components, @utils, etc.)

## Error Handling
Use Result<T, E> pattern or typed Error subclasses for expected failures.
Avoid throw for control flow — only throw for truly unexpected errors.
```

### `react.instructions.md`

```markdown
---
applyTo: "**/*.tsx,**/components/**"
---
# React Conventions

## Component Structure
Functional components only (no class components).
Props: always typed with an interface named <ComponentName>Props.
State: prefer local state; use context for cross-component state; Zustand for global.

## Accessibility (required, not optional)
All interactive elements must have accessible labels (aria-label or visible text).
Use semantic HTML (button, nav, main, article) before adding role= attributes.
Keyboard navigation must work for all interactive elements.
Test with: Tab navigation and screen reader (VoiceOver or NVDA).

## Performance
Wrap expensive computations in useMemo. Wrap stable callbacks in useCallback.
Do not use useMemo/useCallback for cheap operations (premature optimization).
Large lists: use virtualization (react-window or tanstack-virtual).

## Testing
Use React Testing Library. Prefer accessible queries (getByRole, getByLabelText).
Never use getByTestId unless there is no accessible alternative.
Test user behavior, not implementation details.
```

---

## 4. Team Onboarding — New Developer Setup

### What Every New Team Member Needs

```
The first time a developer opens this repo in VS Code:

Step 1 — Extensions install automatically:
  .vscode/extensions.json lists required extensions.
  VS Code prompts to install recommended extensions on first open.
  → GitHub.copilot, GitHub.copilot-chat, and language-specific extensions.

Step 2 — Copilot sign-in:
  Developer signs in with their GitHub account (must have Copilot access).
  If on company GitHub org: org admin must enable Copilot for the account.

Step 3 — Verify instructions are loading:
  In Chat: "What instructions do you have for this workspace?"
  Expected: Copilot summarizes the team conventions.
  If it gives generic answers: the .github/ folder may not be at repo root.

Step 4 — Run the onboarding prompt:
  /codebase-onboarding (if you've created this prompt file)
  Or: "@codebase-navigator Give me an architecture overview of this codebase"

Step 5 — Create local mcp.json:
  Copy config/mcp.example.json → .vscode/mcp.json
  Set environment variables for any tokens needed.
  Verify MCP tools appear in Agent Mode.
```

### Team Onboarding Checklist (add to team wiki)

```
New developer Copilot onboarding:
[ ] GitHub Copilot extension installed (publisher: GitHub)
[ ] GitHub Copilot Chat extension installed (publisher: GitHub)
[ ] Signed in with GitHub account that has Copilot access
[ ] Verified Copilot is active (status bar shows icon without warning)
[ ] Tested that instructions load ("What instructions do you have?" in Chat)
[ ] Created local mcp.json from mcp.example.json
[ ] Set required environment variables (listed in mcp.example.json comments)
[ ] Read: CONTRIBUTING.md (AI usage section if it exists)
[ ] Completed Quick Wins exercises from 01-Foundations/
[ ] Knows team prompt library location and how to use slash commands
[ ] Knows team agents library and how to invoke @agents
```

---

## 5. Generic vs Project-Specific Prompt Design

### The Portability Problem

```
BAD (project-specific — only works in one codebase):
---
name: Generate User Tests
description: Generate tests for user service
---
"Generate pytest tests for the UserService class in our project.
Use the UserFactory from tests/factories.py and the db_session fixture."

Why it's bad:
  - "UserService" is hardcoded
  - "UserFactory" may not exist in another project
  - Useless when you bring this to a new project

GOOD (generic — works anywhere):
---
name: Generate Tests
description: Generate unit tests for selected code
---
"Generate [framework] unit tests for: ${selection}
Use: existing test fixtures if present (check the test directory first)
Cover: happy path, 2 error cases, 2 edge cases
Mock: all external dependencies"

Why it's good:
  - Works for any class in any project
  - ${selection} makes it dynamic
  - Framework can be inferred from the file context
```

### Keep Project-Specific Context in Instructions, Not Prompts

```
Good separation:

copilot-instructions.md:
  "Testing: use pytest with pytest-asyncio. Database fixture: db_session. Factory: UserFactory."

generate-tests.prompt.md:
  "Generate pytest tests for ${selection}.
  Cover: happy path, error cases, edge cases.
  Mock: all external dependencies."

Result:
  - Prompt is portable across any project
  - Instructions inject the project-specific context automatically
  - Change instructions once when you move to a new project
  - Never need to edit the prompts
```

---

## 6. Instruction Ownership and Review

### The Instruction File Is a Team Asset

```
Treat copilot-instructions.md like a team coding standards document:
  - Changes go through PR review
  - At least one reviewer is required
  - Stale or incorrect instructions should be removed, not just commented out

Symptoms of stale instructions:
  - Copilot suggests a library that instructions say not to use, but the rule was removed
  - Instructions reference a file structure that was reorganized
  - Instructions say "use X" but the codebase migrated to Y months ago

Quarterly review process:
  1. Read every rule in copilot-instructions.md
  2. For each rule: is this still true? Is it still relevant? Is it being followed?
  3. Remove rules that no longer apply
  4. Add rules for conventions established since the last review
  5. Check path-specific instruction files too
  Commit: "chore: quarterly Copilot instructions review - [month year]"
```

---

## 7. Portable Workspace Verification

```
Before declaring a workspace "team-ready", verify on a clean machine:

Clone the repo → open in VS Code → verify ALL of these:

[ ] .vscode/extensions.json prompts to install all required extensions
[ ] After installing extensions: Copilot loads without error
[ ] In Chat: "What instructions do you have?" → Copilot summarizes team conventions
[ ] Type / in Chat → all prompt files appear as slash commands
[ ] Type @ in Chat → all custom agents appear in the picker
[ ] mcp.example.json exists and has placeholder tokens (not real ones)
[ ] mcp.json is listed in .gitignore (not committed with real tokens)
[ ] .vscode/settings.json has the recommended VS Code settings
[ ] README has a "Copilot setup" section for new contributors

If all green: workspace is portable.
Any red item: fix before the next developer onboards.
```

---

## 8. Revision Checklist

- [ ] Has a layered instruction architecture (root + path-specific) for any multi-language or monorepo
- [ ] Root instructions cover only cross-cutting team rules (< 400 words)
- [ ] Path-specific instructions use correct `applyTo` glob patterns
- [ ] Prompt files are generic (no hardcoded class or file names)
- [ ] Project-specific context is in instructions, not prompts
- [ ] Has a team onboarding checklist in the repo
- [ ] Instructions are under version control and reviewed quarterly
- [ ] Workspace passes the portable workspace verification checklist
