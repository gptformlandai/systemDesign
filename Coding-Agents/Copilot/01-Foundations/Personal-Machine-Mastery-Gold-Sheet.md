# Personal Machine Mastery — Workspace, Backup & Git Safety — Gold Sheet

> **Track**: Copilot Mastery Track — Group 1: Foundations
> **File**: Gap Fill (Track File #2a)
> **Audience**: Developers building a durable, portable personal Copilot setup
> **Read after**: GitHub-Copilot-Setup-Personal-Machine-Gold-Sheet.md

---

## 1. Practical Impact Meter

| Topic | Impact | Why Developers Skip This |
|---|---|---|
| Personal workspace instructions — what to configure | ★★★★★ | Without instructions, Copilot ignores all your conventions forever |
| Sharing setup across multiple machines | ★★★★★ | Most devs re-configure from scratch every time — hours wasted |
| Personal AI productivity repo structure | ★★★★★ | Prompt/agent files without version control = rebuilt every few months |
| Backup strategy for prompts and agents | ★★★★☆ | Losing a prompt library of 20 files takes days to recreate |
| Preventing secret leaks from local projects | ★★★★★ | AI tools see file context — one wrong open file can expose credentials |
| Git safety with Copilot-generated changes | ★★★★★ | AI-generated commits that you can't explain are a career and security risk |

---

## 2. Configuring Personal Workspace Instructions

### The Three Levels of Instructions

```
Level 1 — Global (applies everywhere, any project):
  Location: not a file — achieved through VS Code user settings
  What goes here: your personal preferences for Copilot Chat behavior
  Example: preferred language (English), verbosity preference
  
Level 2 — Repository-level (applies to one project):
  Location: .github/copilot-instructions.md in the project root
  What goes here: project-specific stack, conventions, architecture rules
  Example: "This project uses Python 3.12 + FastAPI + asyncpg"
  
Level 3 — Path-specific (applies to files matching a glob):
  Location: .github/instructions/*.instructions.md
  What goes here: language-specific or domain-specific rules
  Example: testing.instructions.md → applies to tests/**
```

### What Every Personal Project Needs

Create `.github/copilot-instructions.md` with this minimum:

```markdown
# [Project Name]

## What This Is
[2-3 sentences: what it does, who uses it (even if just you)]

## Stack
[Language + framework + database + test framework — be specific with versions]

## Architecture Rules
[2-3 key rules: e.g., "service layer only handles business logic, no DB access"]

## Coding Conventions
[What Copilot might get wrong without explicit instruction:
  - preferred libraries (use httpx, not requests)
  - naming conventions
  - async vs sync preference
  - test framework and fixture names]

## Do NOT
[Antipatterns specific to your setup:
  - "Do not use os.system()" 
  - "Do not add print() for debugging"
  - "Do not suggest synchronous DB drivers in async routes"]
```

### Personal User Settings for Chat Behavior

```json
// In VS Code user settings.json (affects all projects globally):
{
  // Always load instruction files — the most important setting:
  "github.copilot.chat.codeGeneration.useInstructionFiles": true,

  // Set your preferred language for Chat responses:
  "github.copilot.chat.localeOverride": "en",

  // Disable suggestions for file types where AI assistance is risky:
  "github.copilot.enable": {
    "*": true,
    "env": false,         // NEVER enable for .env files
    "dotenv": false,      // NEVER enable for dotenv files
    "plaintext": false    // disable for unstructured text
  }
}
```

### Validating Instructions Are Loaded

```
After creating or updating copilot-instructions.md:

Test: open Chat → ask: "What instructions do you have for this workspace?"

Expected response: Copilot summarizes your project conventions.

If response is generic ("I don't have specific instructions"):
  → Check: file is at .github/copilot-instructions.md (exact path)
  → Check: useInstructionFiles setting is true
  → Check: file is valid Markdown (no broken formatting)
  → Reload VS Code: Cmd+Shift+P → "Developer: Reload Window"
```

---

## 3. Sharing Setup Across Multiple Machines

### The Multi-Machine Problem

```
Common scenario:
  - MacBook (personal dev machine)
  - Linux desktop (home server / secondary machine)
  - Work laptop (separate environment)

Without a sync strategy:
  - Extensions must be installed manually on each machine
  - Settings diverge across machines
  - Prompt files and agents are not available everywhere
  - Different instruction files produce inconsistent Copilot behavior

With a sync strategy:
  - New machine setup takes 15 minutes, not 2 hours
  - Prompt library is always the same everywhere
  - Copilot behaves consistently across all machines
```

### Strategy 1 — VS Code Settings Sync (built-in, easiest)

```
VS Code has built-in settings sync that syncs via your GitHub account:

Enable:
  1. Click the account icon (bottom-left of VS Code)
  2. "Turn on Settings Sync..."
  3. Sign in with GitHub
  4. Select what to sync: Settings, Keyboard Shortcuts, Extensions, Snippets, Profiles

What syncs:
  ✓ VS Code settings.json
  ✓ Extensions list (installed on all machines)
  ✓ Keyboard shortcuts
  ✓ VS Code Profiles
  ✓ Snippets
  ✗ Workspace-level .github/ files (those live in the repo)

On a new machine:
  1. Install VS Code
  2. Sign in with GitHub
  3. Turn on Settings Sync
  4. All settings + extensions install automatically

Limitation:
  Only syncs VS Code configuration — not your project-level prompt files or agents.
  Those need to be in a git repository (Strategy 2).
```

### Strategy 2 — Personal AI Productivity Repository (recommended)

```
Create a private GitHub repo that IS your Copilot OS:

Repository structure:
  copilot-personal-os/              ← your personal Copilot setup repo
    README.md                       ← how to use this on a new machine
    SETUP.md                        ← new machine setup checklist
    
    config/
      vscode-settings.json          ← your VS Code settings template
      vscode-extensions.json        ← extensions list (for reference)
      gitconfig                     ← your .gitconfig template
      
    .github/                        ← your personal Copilot config
      copilot-instructions.md       ← personal defaults (not project-specific)
      instructions/
        python.instructions.md
        testing.instructions.md
        security.instructions.md
        github-actions.instructions.md
      prompts/
        [all your prompt files]
      agents/
        [all your agent files]
    
    github-actions/
      [your reusable workflow YAML templates]
    
    templates/
      [project-specific copilot-instructions.md templates by stack]
      python-fastapi-instructions.md
      node-express-instructions.md
      react-instructions.md
    
    notes/
      [your daily session notes]

Setup on a new machine:
  git clone git@github.com:yourusername/copilot-personal-os.git
  # Then copy relevant files to new projects as needed
```

### Strategy 3 — Dotfiles Repository (for the full machine setup)

```
A dotfiles repo manages your entire machine configuration:

What to include for Copilot-related setup:
  .gitconfig         — git identity, editor, default branch
  .zshrc / .bashrc   — aliases, GITHUB_TOKEN env var (without the actual value)
  install.sh         — script to install tools and configure a new machine

Example install.sh for Copilot-relevant tools:
  #!/bin/bash
  # Install VS Code
  brew install --cask visual-studio-code
  
  # Install GitHub CLI
  brew install gh
  
  # Install git
  brew install git
  
  # Configure git defaults
  git config --global init.defaultBranch main
  git config --global core.editor "code --wait"
  
  # Install VS Code extensions
  code --install-extension GitHub.copilot
  code --install-extension GitHub.copilot-chat
  code --install-extension GitHub.vscode-pull-request-github
  code --install-extension eamodio.gitlens
  code --install-extension esbenp.prettier-vscode
  code --install-extension charliermarsh.ruff    # Python
  code --install-extension ms-python.python
  code --install-extension ms-python.vscode-pylance
  
  echo "Done. Now: sign in to GitHub in VS Code and enable Settings Sync"

Keep the install script in your dotfiles repo and run it on every new machine.
```

### New Machine Checklist (15-minute setup)

```
[ ] Install VS Code: brew install --cask visual-studio-code (macOS)
    OR download from https://code.visualstudio.com

[ ] Install GitHub CLI: brew install gh

[ ] Clone your personal OS repo:
    gh auth login
    git clone git@github.com:yourusername/copilot-personal-os.git

[ ] Install extensions from your extensions list:
    cat copilot-personal-os/config/vscode-extensions.json
    # Install each one: code --install-extension <id>

[ ] Sign in to GitHub in VS Code:
    Cmd+Shift+P → "GitHub: Sign In"

[ ] Enable Settings Sync (if using Strategy 1):
    Account icon → Turn on Settings Sync

[ ] Copy settings.json to user settings:
    cp copilot-personal-os/config/vscode-settings.json ~/.config/Code/User/settings.json
    # macOS: ~/Library/Application\ Support/Code/User/settings.json

[ ] Configure SSH key:
    ssh-keygen -t ed25519 -C "your@email.com"
    gh ssh-key add ~/.ssh/id_ed25519.pub --title "$(hostname)"

[ ] Verify Copilot is active:
    Open VS Code → create test file → check for inline suggestions
    Open Chat → type "What is 2+2?" → verify response

[ ] Test instructions:
    Open a project → Chat: "What instructions do you have?"
    Verify: Copilot summarizes project conventions
```

---

## 4. Structuring Your Personal AI Productivity Repository

### Recommended Repository Structure

```
copilot-personal-os/
│
├── README.md                   ← Quick reference for new machine setup
├── SETUP.md                    ← Detailed new machine setup guide
├── DAILY-CHECKLIST.md          ← Your daily Copilot ritual (symlink or copy)
├── COMMAND-CHEATSHEET.md       ← Quick reference (symlink or copy)
│
├── config/                     ← Machine configuration files
│   ├── vscode-settings.json    ← Template settings (copy to new machine)
│   ├── vscode-extensions.json  ← Extensions list for install script
│   ├── gitconfig               ← .gitconfig template
│   └── mcp.example.json        ← MCP config template (no real secrets)
│
├── .github/                    ← Your personal Copilot configuration
│   ├── copilot-instructions.md ← Personal defaults (not project-specific)
│   └── instructions/
│   └── prompts/                ← Your complete prompt library
│   └── agents/                 ← Your complete agent library
│
├── templates/                  ← Project starter templates
│   ├── python-fastapi/
│   │   └── copilot-instructions.md
│   ├── node-express/
│   │   └── copilot-instructions.md
│   └── react-typescript/
│       └── copilot-instructions.md
│
├── github-actions/             ← Reusable workflow templates
│   ├── python-ci.yml
│   ├── node-ci.yml
│   └── release.yml
│
├── notes/                      ← Daily session notes (auto-generated)
│   ├── 2024-01-15-session.md
│   └── ...
│
└── install.sh                  ← New machine setup script
```

### The Personal `copilot-instructions.md` (your personal defaults)

```markdown
# Personal Copilot Defaults

This file provides my personal development preferences.
It is NOT project-specific — project-specific rules live in the project's
.github/copilot-instructions.md.

## My Development Style
- Type hints / TypeScript types always required on public APIs
- Tests always required for new functionality
- No print()/console.log() for debugging in committed code
- Conventional commits for all commit messages
- Max function length: 30 lines
- No magic numbers — use named constants

## My Preferred Libraries (default when not overridden by project)
- Python HTTP: httpx (not requests)
- Python validation: Pydantic v2
- Python testing: pytest + pytest-asyncio
- JS HTTP: fetch API (no axios unless project uses it)
- JS testing: Vitest (not Jest unless project uses it)

## Response Style Preferences
- Show code first, explanation second (unless I ask for explanation first)
- Concise — under 200 words for short answers
- Use real variable names from my code, not 'foo' and 'bar'
- When suggesting multiple options: recommend one and explain why

## Do NOT
- Do not add TODO comments — either implement or skip
- Do not generate code I can't explain — if complex, explain it
- Do not add over-engineering (interfaces for one implementation, etc.)
```

---

## 5. Backing Up Prompt Files and Custom Agents

### Why Backups Matter

```
Your prompt library represents months of refinement.
A prompt that took 5 iterations to get right is intellectual property.

What you can lose:
  - All prompts if the .github/prompts/ folder is on a machine that dies
  - Agent files if not in version control
  - Instruction files if not committed to a repo
  - Daily notes if kept only locally

Backup strategy:
  The simplest backup IS version control.
  Everything in your personal OS repo is backed up automatically
  by being pushed to GitHub.
```

### Version Control Your Entire Prompt Library

```bash
# Initial setup:
cd copilot-personal-os
git init
git remote add origin git@github.com:yourusername/copilot-personal-os.git

# Add everything:
git add .
git commit -m "chore: initial prompt library and agent library"
git push -u origin main

# After adding or improving a prompt:
git add .github/prompts/new-prompt.prompt.md
git commit -m "feat(prompts): add /modernize-code prompt"
git push

# After improving an existing prompt:
git add .github/prompts/generate-tests.prompt.md
git commit -m "improve(prompts): require mock targets in generate-tests"
git push
```

### Syncing Prompts to Active Projects

```bash
# Option 1: copy files to project
cp ~/copilot-personal-os/.github/prompts/*.prompt.md .github/prompts/
cp ~/copilot-personal-os/.github/agents/*.agent.md .github/agents/

# Option 2: symlink (changes to personal OS are reflected immediately)
# macOS/Linux only:
ln -s ~/copilot-personal-os/.github/prompts .github/prompts
ln -s ~/copilot-personal-os/.github/agents .github/agents

# Option 3: Git submodule (advanced — tracks a specific version)
git submodule add git@github.com:yourusername/copilot-personal-os.git .copilot-os
# Then copy files as needed from .copilot-os/

# Recommendation: use Option 1 (copy) for project repos that will be shared.
# Use Option 2 (symlink) for personal-only projects.
```

### Exporting Your Prompt Library

```bash
# Create a snapshot of your full library (useful before a machine wipe):
tar -czf copilot-library-backup-$(date +%Y%m%d).tar.gz \
    .github/prompts/ \
    .github/agents/ \
    .github/instructions/ \
    .github/copilot-instructions.md

# Verify the archive:
tar -tzf copilot-library-backup-*.tar.gz | head -20

# Store the archive: iCloud, Dropbox, Google Drive, or a private S3 bucket
```

---

## 6. Avoiding Secret Leaks from Local Projects

### How Secrets Reach Copilot (and how to prevent it)

```
Copilot sees context from:
  1. Files you open in VS Code (implicit context for inline)
  2. Files you reference with #file in Chat (explicit context)
  3. Agent Mode file reads (when it scans your workspace)

How secrets leak:
  → .env file is open in an editor tab → Copilot's inline context sees it
  → You paste a function that has a hardcoded API key
  → Agent Mode reads a config file that contains real tokens
  → You paste a connection string "for debugging"

Prevention — at the file level:
```

### `.gitignore` Discipline

```bash
# Every project must have these in .gitignore:
cat >> .gitignore << 'EOF'
# Secrets
.env
.env.local
.env.development
.env.production
.env.staging
*.pem
*.key
*.p12
*.pfx
id_rsa
id_ed25519
*.secrets
secrets.json
credentials.json
service-account.json

# MCP (if contains real tokens)
.vscode/mcp.json

# AWS credentials
.aws/credentials
.aws/config

# Cloud provider configs
.gcloud/
.azure/
EOF

# Verify nothing secret is tracked:
git status
# Anything sensitive that appears here should be removed and added to .gitignore
```

### Copilot-Specific Secret Prevention

```
Rule 1: NEVER open .env in VS Code if it has real values.
  Alternative: keep .env in a secure location outside the project,
  or use a secrets manager and never write values to disk.

Rule 2: Disable Copilot for sensitive file types in settings.json:
  "github.copilot.enable": {
    "env": false,
    "dotenv": false
  }

Rule 3: Check what Agent Mode can access before running it:
  Review .github/AGENTS.md — does it explicitly list what CAN be read?
  The AGENTS.md in your repo should list which directories are accessible.

Rule 4: Use placeholder values in debugging examples:
  WRONG: "My database URL is postgresql://admin:RealPass123@prod.mycompany.com/users"
  RIGHT: "My database URL format is postgresql://user:password@host:port/dbname"

Rule 5: Rotate any credentials that were accidentally pasted:
  If you realize you shared a real credential with Copilot:
    1. Assume it may have been logged or transmitted
    2. Immediately rotate/revoke the credential
    3. Audit logs for unauthorized access
    4. Report to your security team if work credentials
```

### Pre-Commit Secret Scanning

```bash
# Install gitleaks (detects secrets before commit):
brew install gitleaks   # macOS

# Scan your repo before pushing:
gitleaks detect --source=. --verbose

# Install as a pre-commit hook:
cat > .git/hooks/pre-commit << 'EOF'
#!/bin/bash
gitleaks protect --staged --verbose
if [ $? -ne 0 ]; then
    echo "Secret detected! Commit blocked. Remove the secret and try again."
    exit 1
fi
EOF
chmod +x .git/hooks/pre-commit

# GitHub repository scanning:
# Enable Secret Scanning in repo Settings → Security → Code security
# GitHub will alert you if a secret pattern is detected in any push
```

### `.copilot-ignore` — Files Copilot Should Not Read

```
GitHub Copilot respects .gitignore for most operations.
For additional exclusions (files you commit but don't want Copilot to see):

.copilotignore file (in repo root — experimental, check current docs for support):
  # Files to exclude from Copilot context:
  *.pem
  *.key
  secrets/
  internal-docs/confidential/

Note: The most reliable protection is:
  1. Not having secrets in files at all (use env vars or secrets manager)
  2. Keeping sensitive files in .gitignore (so they're not tracked)
  3. Never pasting real credentials into Copilot Chat
```

---

## 7. Using Git Safely with Copilot-Generated Changes

### The Core Safety Model

```
Every Copilot-generated change you commit is YOUR commit.
You are responsible for:
  - Understanding what the code does
  - Verifying it is correct
  - Ensuring it doesn't contain secrets
  - Ensuring it doesn't break existing behavior

"Copilot wrote it" is not an acceptable answer to:
  - A security audit
  - A code review question
  - A production incident postmortem

Git discipline is your primary safety net.
```

### Checkpoint Commits — Your Safety Net

```bash
# Before any Agent Mode session:
git add .
git commit -m "checkpoint: before agent mode - [task description]"
# This gives you a clean restore point

# Before major Copilot Edits sessions:
git stash   # if you don't want to commit
# OR
git add . && git commit -m "checkpoint: before edits"

# If Agent Mode goes wrong:
git checkout .   # discards all uncommitted changes, restores to last commit
# OR
git reset --hard HEAD   # same effect (also discards staged changes)

# Check what changed before committing:
git diff HEAD   # shows all changes since last commit
git diff --staged   # shows staged changes about to be committed
```

### Reviewing Copilot Changes Before Committing

```bash
# See exactly what changed in each file:
git diff src/services/user_service.py

# See a summary of all changes:
git diff --stat

# Stage selectively — only the parts you've verified:
git add -p src/services/user_service.py   # interactive staging (y/n per hunk)
# This forces you to read each change before staging it

# View staged changes before commit:
git diff --staged

# Only commit after you can answer:
#   1. What does this change do?
#   2. Why is it correct?
#   3. What could break?
#   4. Were the tests run and did they pass?
```

### Commit Message Discipline for AI-Assisted Changes

```bash
# DO NOT use vague AI-assisted messages:
git commit -m "fix: copilot fix"      # ❌ — what was fixed?
git commit -m "wip: ai generated"     # ❌ — can't revert this intelligently later
git commit -m "changes"               # ❌ — useless in git log

# DO use specific, conventional messages (Copilot can help generate these):
git commit -m "fix(auth): prevent null pointer when user_id is missing in JWT"
git commit -m "feat(orders): add email confirmation after successful order creation"
git commit -m "refactor(payment): extract Stripe charge logic into PaymentGateway class"
git commit -m "test(user): add edge cases for email validation boundary conditions"

# Use the /commit-message prompt or VS Code ✨ button to generate good messages
```

### Branch Safety for Large AI-Assisted Work

```bash
# Always use a feature branch for significant Copilot-assisted work:
git checkout -b feat/add-notification-preferences
# Work with Copilot...
# Review all changes...
# Commit each logical piece separately...

# When ready to merge: review the full diff vs main:
git diff main...feat/add-notification-preferences

# If the diff is large and hard to review:
# → Break the feature into smaller PRs
# → Each PR = one logical unit of Copilot-assisted work
# → Smaller PRs are easier to review and revert if needed

# Tag before major AI-assisted refactoring:
git tag pre-ai-refactor-$(date +%Y%m%d)
git push origin pre-ai-refactor-$(date +%Y%m%d)
# Now you have a permanent restore point
```

### Running Tests After Every Copilot Change

```bash
# Build the habit: every Copilot-assisted change → immediately run tests

# Python:
pytest tests/unit/ -v --tb=short
# Check: no previously-passing test now fails

# Node/TypeScript:
npm test
# OR
npx vitest run

# Java:
./mvnw test -q

# After running:
# ✓ All tests pass → safe to commit
# ✗ Any test fails → the change is wrong, do not commit
#   → Ask Copilot: "This test is now failing after my change: [test name]
#     The change was: #file:[file]. Why does it fail and how do I fix it?"
```

### The Five-Point Commit Gate

```
Before git commit after Copilot-assisted work:

[ ] 1. I read the diff (git diff --staged) — not just "looks right" but line-by-line
[ ] 2. I can explain in one sentence what each changed function does
[ ] 3. Tests pass (run the test command — don't assume)
[ ] 4. No secrets, API keys, or PII appear in the diff (git diff --staged | grep -i "key\|secret\|password\|token")
[ ] 5. The commit message describes WHAT changed and WHY (not just "fix" or "update")

All 5 checked → commit. Any unchecked → fix first.
```

### Recovering from Bad AI-Generated Commits

```bash
# Scenario 1: bad commit not yet pushed
# Undo the last commit but keep the changes:
git reset --soft HEAD~1
# Now the changes are unstaged — review them, fix them, recommit

# Scenario 2: bad commit already pushed to your personal branch (not main)
# Revert the bad commit:
git revert HEAD   # creates a new "revert" commit — safe for shared history
# OR for personal branches:
git push --force-with-lease   # after reset —ONLY on personal feature branches

# Scenario 3: multiple bad commits, need to go back to a known good state
git log --oneline -10   # find the commit you want to restore to
git checkout <commit-hash>   # detached HEAD — examine the state
git checkout -b restored-state   # create a branch from this good state
git push -u origin restored-state   # push for review before merging

# Scenario 4: you accepted Copilot changes and broke main (never do this)
# This is why you should ALWAYS work on feature branches
git revert <bad-commit-hash>   # revert on main — never force-push main
git push
```

---

## 8. Revision Checklist

- [ ] Has `.github/copilot-instructions.md` in every active project
- [ ] Understands the three levels of instructions (global/repo/path-specific)
- [ ] Has a multi-machine sync strategy (Settings Sync and/or personal OS repo)
- [ ] Personal AI productivity repository created on GitHub (private)
- [ ] Prompt library and agent library are version-controlled and pushed
- [ ] `.gitignore` includes: `.env`, `*.key`, `*.pem`, `.vscode/mcp.json`
- [ ] Gitleaks or equivalent pre-commit hook installed
- [ ] Has the checkpoint commit habit before Agent Mode (`git add . && git commit -m "checkpoint: ..."`)
- [ ] Uses interactive staging (`git add -p`) for Copilot-generated changes
- [ ] Can run the five-point commit gate from memory
- [ ] Can recover from a bad AI-generated commit with `git reset --soft` or `git revert`
- [ ] Knows the difference: `git checkout .` vs `git reset --hard` vs `git revert`
