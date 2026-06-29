# Personal GitHub Workflow — Gold Sheet

> **Track**: Copilot Mastery Track — Group 3: Advanced Engineering
> **File**: Gap Fill (Track File #20b)
> **Audience**: Developers managing personal GitHub repos with Copilot integration
> **Read after**: AGENTS-MD-Strategy-Gold-Sheet.md

---

## 1. Personal GitHub Setup

### GitHub CLI — Your Daily Driver

```bash
# Install
brew install gh    # macOS
# or: https://cli.github.com

# Authenticate
gh auth login      # opens browser → select GitHub.com → select HTTPS or SSH

# Verify
gh auth status

# Set default editor for PR descriptions and issues
gh config set editor "code --wait"

# Essential daily commands:
gh pr create --fill      # create PR from current branch (--fill auto-populates from commits)
gh pr view --web         # open current PR in browser
gh issue list            # list open issues
gh issue create          # create new issue
gh pr checks             # check CI status of current PR
gh repo clone owner/repo # clone a repo
```

### Personal Account Configuration Checklist

```
[ ] SSH key added to GitHub (preferred over HTTPS for long sessions):
    ssh-keygen -t ed25519 -C "your@email.com"
    gh ssh-key add ~/.ssh/id_ed25519.pub --title "MacBook Personal"

[ ] Git identity configured:
    git config --global user.name "Your Name"
    git config --global user.email "your@email.com"
    git config --global core.editor "code --wait"
    git config --global init.defaultBranch main

[ ] GitHub notifications optimized:
    - Unwatch repos you don't actively contribute to
    - Watch only: Releases for dependencies you use
    - Watch actively: Repos you contribute to

[ ] GitHub profile repository (yourusername/yourusername):
    - README.md with your tech stack and current focus
    - Pinned repos showing your best work
    - Copilot prompt library contributions (if public)
```

---

## 2. Copilot-Assisted Personal Repo Management

### Setting Up a New Personal Repo with Copilot

```bash
# Create and clone
gh repo create my-project --private --clone
cd my-project

# Initialize Copilot configuration
mkdir -p .github/instructions .github/prompts .github/agents

# Start Copilot-assisted project setup
# In VS Code Chat:
"Bootstrap this project structure:
Stack: [your stack]
Purpose: [what this does]
Create: .github/copilot-instructions.md, AGENTS.md, .gitignore, .env.example, README.md"
```

### Branch Strategy for Personal Projects

```bash
# Simple feature branch workflow
git checkout -b feat/add-login-flow

# Work with Copilot...

# Before PR:
# 1. Run: /security-review on changed files
# 2. Run: /write-pr-description
# 3. Push and create PR

git push -u origin HEAD
gh pr create --fill     # populates from branch name and commits
```

### Personal Commit Discipline with Copilot

```bash
# Stage changes
git add .

# Use Copilot to generate commit message:
# Option 1: Click ✨ in VS Code Source Control
# Option 2: Run /commit-message prompt

# Conventional commits for personal projects — still worth it:
# - Makes git log readable in 6 months
# - GitHub auto-generates better release notes
# - Copilot learns your patterns better

# Good personal project commits:
git commit -m "feat: add JWT refresh token endpoint"
git commit -m "fix: prevent order double-submit on network retry"
git commit -m "docs: add Docker Compose setup to README"
```

---

## 3. PR Workflow — Personal and Open Source

### Self-PR Workflow (Personal Projects)

Even for solo projects, creating PRs is worth it:
```
Benefits:
  - Forces you to review your own diff before merging
  - CI runs automatically on the PR
  - Creates a record of why a change was made
  - Copilot Code Review runs on the diff

Workflow:
  1. Feature branch → implement
  2. Push → create PR (gh pr create --fill)
  3. Run /security-review on the diff in VS Code
  4. Run /write-pr-description
  5. Review the diff yourself in GitHub.com
  6. Merge (squash or regular — pick one style and stick to it)
```

### Copilot-Assisted PR Review Workflow

```bash
# When reviewing someone else's PR:

# 1. Check out the PR locally:
gh pr checkout 42

# 2. In VS Code — run your review prompts:
# /security-review on changed files
# /architecture-review on major changes
# /generate-tests to check what test coverage looks like

# 3. Generate a comprehensive review comment:
"Review #file:[changed file] as a senior developer.
Find: correctness issues, security concerns, missing tests, naming problems.
Format: numbered list, each with: file, line, issue, suggested fix.
Severity: mark CRITICAL issues first."

# 4. Post inline comments using GitHub.com
# 5. Use Copilot Code Review on GitHub.com as first pass
```

---

## 4. GitHub Issues — Copilot-Assisted Issue Management

### Generating Issue Descriptions from Code

```
When you find a bug, use Copilot to draft the issue:

"Generate a GitHub issue for this bug:

Bug: [describe what you observed]
Steps to reproduce: [what triggered it]
Expected: [what should happen]
Actual: [what happened]
Code context: #selection [select the relevant code]

Format for the issue:
## Bug Description
## Steps to Reproduce
## Expected Behavior
## Actual Behavior
## Environment
## Possible Fix [optional — only if you have an idea]"
```

### Generating Implementation Issues from Requirements

```
"Generate a GitHub issue for implementing this feature:

Feature: user notification preferences (email, in-app, SMS toggles)
Purpose: users should control which notifications they receive

Format:
## Summary
## User Story
## Acceptance Criteria
## Technical Notes
## Out of Scope"
```

---

## 5. Personal Project Conventions

### Branch Naming (stick to one style)

```
feat/short-description      — new feature
fix/issue-number-description — bug fix (link issue number if applicable)
refactor/what-changed       — refactoring
docs/what-documented        — documentation
chore/what-updated          — dependency updates, config changes
```

### PR Naming

```
[type]: brief description
feat: add user notification preferences
fix: prevent double order submission on retry
refactor: extract EmailValidator from UserService
```

### Tag Strategy for Personal Projects

```bash
# Tag before major AI-assisted changes (easy rollback point)
git tag pre-ai-refactor-$(date +%Y%m%d)

# Semantic versioning for releases
git tag v1.2.0
git push --tags

# Generate release notes with Copilot:
"Generate release notes for v1.2.0.
Commits since v1.1.0:
$(git log v1.1.0..HEAD --oneline)"
```

---

## 6. GitHub Actions for Personal Projects

### Minimal but Effective CI

Every personal project should have this minimum:

```yaml
# .github/workflows/ci.yml — generated by /create-github-action
name: CI
on:
  push:
    branches: [main]
  pull_request:
concurrency:
  group: ${{ github.workflow }}-${{ github.ref }}
  cancel-in-progress: true
jobs:
  test:
    runs-on: ubuntu-latest
    timeout-minutes: 15
    steps:
      - uses: actions/checkout@v4
      - name: Set up environment
        # [stack-specific setup]
      - name: Run tests
        run: # [your test command]
```

Use `/create-github-action` prompt for your specific stack.

---

## 7. Revision Checklist

- [ ] GitHub CLI installed and authenticated
- [ ] SSH key configured for GitHub authentication
- [ ] Git identity set (name and email)
- [ ] New repos get: `.github/copilot-instructions.md`, `AGENTS.md`, `.env.example`
- [ ] Uses feature branches even for personal projects
- [ ] Creates PRs before merging (even solo — for CI and diff review)
- [ ] Uses `/commit-message` or ✨ for commit messages
- [ ] Tags before major AI-assisted refactoring (`git tag pre-ai-refactor-date`)
- [ ] Minimal CI workflow in every project (lint + test)
