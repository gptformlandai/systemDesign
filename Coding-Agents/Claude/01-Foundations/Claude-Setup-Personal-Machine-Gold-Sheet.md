# Claude Setup — Personal Machine — Gold Sheet

> **Track**: Claude Mastery Track — Group 1: Foundations
> **File**: 2 of 6 (Track File #2)
> **Audience**: Developers installing Claude tools for the first time
> **Read after**: Claude-Mental-Model-Gold-Sheet.md

---

## 1. Practical Impact Meter

| Topic | Impact | Why Developers Skip This |
|---|---|---|
| Claude Code CLI install and verify | ★★★★★ | Most devs only use Claude.ai — missing the autonomous coding capability entirely |
| Claude.ai Projects — persistent context | ★★★★★ | Default conversations have no memory; Projects carry context across sessions |
| API key management — secure storage | ★★★★★ | API keys hardcoded in config files = security incident |
| CLAUDE.md at home directory (~/.claude/) | ★★★★☆ | Personal defaults that apply to ALL projects across all machines |
| Verification protocol — confirm setup works | ★★★★★ | Devs assume it works without testing; silent failures waste hours |
| Model selection per task | ★★★★☆ | Default model is not always the best choice |

---

## 2. Claude Surfaces — The Three Tools

```
Surface 1: Claude.ai (web chat)
  URL: claude.ai
  What it is: browser-based chat interface
  No filesystem access. You paste code or upload files.
  Best for: learning, Q&A, one-shot tasks
  Free plan: available with usage limits
  Pro plan: higher limits, priority access, all models

Surface 2: Claude Code CLI
  Install: npm install -g @anthropic-ai/claude-code
  What it is: terminal-based agentic tool with full filesystem access
  Best for: multi-file editing, autonomous workflows, CI integration
  Requires: Claude API key or Claude.ai subscription

Surface 3: Claude Desktop (if available)
  Download: anthropic.com/claude
  What it is: native macOS/Windows app with some filesystem access
  Best for: daily chat tasks without browser
  Note: check current feature availability — Desktop evolves rapidly
```

---

## 3. Claude.ai Setup

### Step 1 — Create an Account

```
1. Go to: claude.ai
2. Sign up with email or Google
3. Verify email
4. Choose plan:
   Free: limited messages per day, Claude 3 Haiku
   Pro: unlimited, Claude 3.5 Sonnet + Opus, Projects, file uploads

Free plan is sufficient to start. Upgrade when you hit limits.
```

### Step 2 — Create a Project (Persistent Context)

```
Projects give Claude persistent context across conversations.

Create a project:
1. Sidebar → "Projects" → "New Project"
2. Name it after your current focus (e.g., "Python FastAPI Work")
3. Open Project Settings
4. Add "Project Instructions" — this is equivalent to CLAUDE.md for Claude Chat:
   
   Example project instructions:
   "I am a Python developer working on FastAPI services.
   - Always use pytest for tests
   - Use asyncpg for PostgreSQL, never psycopg2
   - Use httpx for HTTP, not requests
   - Follow the repository pattern: router → service → repository
   - Never add print() for debugging — use logging module
   - Never hardcode credentials — use environment variables"

5. All conversations within this project share this context automatically.
```

### Step 3 — Test Claude.ai

```
In a new conversation in Claude.ai:

Test 1: Basic response
  "What is the difference between Python generators and list comprehensions?
  Answer in exactly 3 bullet points."
  Expected: 3 bullets, no more.

Test 2: Code generation
  "Write a Python function that validates an email address.
  Include: regex pattern, type hints, return True/False, handle None input."
  Expected: complete function with all specified features.

Test 3: Context from project instructions
  "What testing framework should I use in this project?"
  Expected: "pytest" (from your project instructions)
  If generic answer: project instructions may not have saved correctly.
```

---

## 4. Claude Code CLI Setup

### Install

```bash
# Prerequisites: Node.js 18+ 
node --version   # must be 18 or higher

# Install Claude Code globally
npm install -g @anthropic-ai/claude-code

# Verify installation
claude --version
```

### Authenticate

```bash
# Option 1: Use Claude.ai subscription (no API key needed)
claude
# First run: Claude will prompt for authentication
# Opens browser → sign in with Claude.ai account → authorize
# Claude Code links to your Claude.ai subscription plan

# Option 2: Use Anthropic API key
export ANTHROPIC_API_KEY="your-key-here"   # add to ~/.zshrc or ~/.bashrc
claude

# Verify authentication
claude "What model are you?"
# Expected: a response (not an auth error)
```

### Secure API Key Storage

```bash
# NEVER do this (hardcodes key in shell history):
claude --api-key sk-ant-your-key

# CORRECT: add to shell profile (never commit this file):
echo 'export ANTHROPIC_API_KEY="sk-ant-..."' >> ~/.zshrc
source ~/.zshrc

# Even better: use a secrets manager
# macOS Keychain:
security add-generic-password -a "$USER" -s "anthropic-api-key" -w "sk-ant-..."
# Then in .zshrc:
export ANTHROPIC_API_KEY=$(security find-generic-password -a "$USER" -s "anthropic-api-key" -w)

# For development machines: .env file (gitignored)
echo "ANTHROPIC_API_KEY=sk-ant-..." >> .env
# .env must be in .gitignore — ALWAYS
```

### First Run Test

```bash
# Navigate to any project directory
cd ~/projects/my-project

# Basic test
claude "What files are in this directory?"
# Expected: Claude lists directory contents

# Code understanding test
claude "Explain the purpose of this project in 3 sentences"
# Expected: Claude reads project files and explains

# Edit test (safe — no permanent changes)
claude --no-auto-accept "Add a docstring to the first function in the largest Python file"
# Expected: Claude proposes a change; you see a diff; no auto-apply
```

### Global Configuration

```bash
# Claude Code config file location
cat ~/.claude/config.json   # or claude config

# Set default model
claude config set model claude-sonnet-4-5

# Set default editor for diffs
claude config set editor "code --wait"

# Verify config
claude config list
```

---

## 5. Personal CLAUDE.md at Home Directory

### What It Is and Why It Matters

```
Location: ~/.claude/CLAUDE.md

This file applies to EVERY Claude Code session across ALL projects.
Use it for your personal development preferences — not project-specific rules.

Project-specific rules: ./CLAUDE.md (project root)
Personal defaults: ~/.claude/CLAUDE.md

Example personal CLAUDE.md:
```

```markdown
# Personal Claude Defaults

## My Development Style
- Type hints required on all public functions
- Tests required for all new code I write
- No print()/console.log() in committed code
- Conventional commits: feat/fix/refactor/test/docs/chore

## My Preferred Libraries
- Python HTTP: httpx (not requests)
- Python testing: pytest + pytest-asyncio
- Python validation: Pydantic v2
- JS testing: Vitest (not Jest)
- JS formatting: Prettier + ESLint

## How I Like Responses
- Code first, explanation second
- Under 200 words for short answers
- Diffs for code changes, not full rewrites
- Do NOT restate my question before answering
- Do NOT add TODO comments — implement or skip

## Do NOT (applies everywhere)
- Do not add over-engineering I didn't ask for
- Do not add new dependencies without listing them
- Do not generate placeholder functions — implement or say you can't
```

---

## 6. Model Selection

```
Claude offers multiple models. Choose based on task complexity.

claude-haiku-3-5        — Fast, cheap. Simple questions, boilerplate, quick explanations.
claude-sonnet-4-5       — Balanced. Daily coding, test generation, reviews, refactoring.
claude-opus-4-5         — Most capable. Complex architecture, hard debugging, nuanced analysis.

Switch model in CLI:
  claude --model claude-opus-4-5 "Design the database schema for a multi-tenant SaaS"

Set default in config:
  claude config set model claude-sonnet-4-5

Decision guide:
  Simple Q&A, boilerplate:          → Haiku
  Standard coding tasks:             → Sonnet (default)
  Complex reasoning, hard bugs:      → Opus
  Architecture trade-offs:           → Opus
  Long context analysis (full repo): → Sonnet or Opus (Haiku loses quality on long context)
```

---

## 7. Verification Protocol — 4-Level Test

### Level 1 — CLI Basics (2 min)

```bash
cd /tmp && mkdir test-claude && cd test-claude
echo "def hello(): return 'world'" > test.py
claude "Explain what test.py does. Under 50 words."
# Expected: brief explanation of the hello function
```

### Level 2 — File Reading and Editing (3 min)

```bash
claude "Add a docstring to the hello function in test.py. Diff only."
# Expected: Claude shows a diff adding a docstring — does NOT auto-apply
# (unless you use --auto-accept flag)

claude --print "What does this project contain?"
# Expected: Claude reads the directory and describes it
```

### Level 3 — CLAUDE.md Loading (2 min)

```bash
cat > CLAUDE.md << 'EOF'
# Test Project
## Do NOT
- Never use print() for debugging
EOF

claude "Should I use print() or logging in this project?"
# Expected: Claude says "logging" based on CLAUDE.md
# If says "print()": CLAUDE.md may not be in the right location
```

### Level 4 — Agentic Task (3 min)

```bash
claude "Create a simple test file for the hello function using pytest.
Run the test after creating it. Report the result."
# Expected: Claude creates test_test.py, runs pytest, reports pass/fail
```

---

## 8. Setup Checklist

```
Claude.ai:
[ ] Account created at claude.ai
[ ] Plan confirmed (Free or Pro)
[ ] At least one Project created with custom instructions
[ ] Basic response test passed (3-bullet response)

Claude Code CLI:
[ ] Node.js 18+ installed
[ ] claude --version shows installed version
[ ] Authentication complete (Claude.ai or API key)
[ ] API key stored securely (not hardcoded)
[ ] First run test passed in a project directory

Personal Configuration:
[ ] ~/.claude/CLAUDE.md created with personal preferences
[ ] Default model set in config
[ ] Test project created with a CLAUDE.md
[ ] 4-level verification protocol completed successfully
```

---

## 9. Revision Checklist

- [ ] Can distinguish Claude.ai, Claude Code CLI, and Claude Desktop
- [ ] Has Claude.ai set up with at least one Project with instructions
- [ ] Has Claude Code CLI installed and authenticated
- [ ] API key stored securely (not in shell history or committed files)
- [ ] ~/.claude/CLAUDE.md created with personal preferences
- [ ] Knows how to switch models per task
- [ ] Completed 4-level verification protocol
