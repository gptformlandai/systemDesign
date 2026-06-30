# GitHub Copilot CLI — Terminal Surface Gold Sheet

> **Track**: Copilot Mastery Track — Group 1: Foundations
> **File**: Track File #4a
> **Practical Impact**: ★★★★☆ — Saves 5-10 minutes daily on command-line tasks
> **Read after**: Copilot-Chat-Fundamentals-Gold-Sheet.md
> **Read before**: Safe-Prompting-Principles-Gold-Sheet.md

---

## 0. How To Read This Sheet

**Beginner focus:**
- What `gh copilot` is and how it differs from VS Code Copilot
- Installing the extension and running your first `explain` and `suggest`

**Intermediate focus:**
- `gh copilot explain` — decode any terminal command before running it
- `gh copilot suggest` — generate commands from plain English
- Shell alias setup for faster workflow

**Senior / Pro focus:**
- Integrating CLI Copilot into daily terminal workflows
- When CLI beats VS Code Chat (and when it doesn't)
- Token-efficient patterns for terminal-specific questions

---

## Topic 1: What Is `gh copilot`?

### The Surface Gap

GitHub Copilot has multiple surfaces:

| Surface | Where | Best for |
|---|---|---|
| Inline suggestions | VS Code / JetBrains editor | Code completion while typing |
| Chat (Ask/Edit/Agent) | VS Code panel | Multi-file tasks, architecture, debugging |
| Code Review | GitHub.com PR | Automated PR feedback |
| **Copilot CLI** | **Your terminal** | **Shell commands, CLI tools, bash scripts** |

The CLI surface is specifically designed for **terminal-native tasks** — explaining `curl`, `jq`, `kubectl`, `git`, `awk`, `sed`, `docker`, `openssl` commands and generating them from natural language without leaving the terminal.

### What It Is

```text
gh copilot is a GitHub CLI extension that brings Copilot into your terminal.
It does NOT require VS Code.

Two core commands:
  gh copilot explain   — "what does this command do?"
  gh copilot suggest   — "write me a command that does X"

Backed by the same model as VS Code Copilot Chat.
Responds in the terminal. No browser, no editor.
```

### When CLI Beats VS Code Chat

```text
Use gh copilot (CLI) when:
  ✓ You're already in the terminal and don't want to switch windows
  ✓ You need to explain a command you found on Stack Overflow before running it
  ✓ You're SSH'd into a remote server (no VS Code available)
  ✓ You're writing or debugging bash/zsh scripts
  ✓ You want to explain piped commands: cat log.txt | grep ERROR | awk '{print $3}'
  ✓ You need kubectl / docker / aws CLI command generation
  ✓ You're in a CI debugging session in a headless environment

Use VS Code Chat when:
  ✓ You need to reference files, code symbols, or codebase context
  ✓ You're making code changes (Edits or Agent Mode)
  ✓ The answer involves reading multiple source files
```

---

## Topic 2: Installation and Setup

### Step 1 — Install GitHub CLI

```bash
# macOS (Homebrew)
brew install gh

# Windows (winget)
winget install --id GitHub.cli

# Ubuntu/Debian
curl -fsSL https://cli.github.com/packages/githubcli-archive-keyring.gpg | sudo dd of=/usr/share/keyrings/githubcli-archive-keyring.gpg
echo "deb [arch=$(dpkg --print-architecture) signed-by=/usr/share/keyrings/githubcli-archive-keyring.gpg] https://cli.github.com/packages stable main" | sudo tee /etc/apt/sources.list.d/github-cli.list > /dev/null
sudo apt update && sudo apt install gh

# Verify
gh --version
```

### Step 2 — Authenticate

```bash
gh auth login
# → Select: GitHub.com
# → Select: HTTPS (or SSH)
# → Authenticate via browser

# Verify auth
gh auth status
```

### Step 3 — Install the Copilot Extension

```bash
gh extension install github/gh-copilot

# Verify
gh copilot --help
```

### Step 4 — (Optional) Create Shell Aliases

```bash
# Add to ~/.zshrc or ~/.bashrc for faster access:
alias '??'='gh copilot suggest -t shell'
alias 'git?'='gh copilot suggest -t git'
alias 'why'='gh copilot explain'

# Or use the built-in alias command (see Topic 5)
```

---

## Topic 3: `gh copilot explain`

### What It Does

```text
Takes a shell command as input.
Returns: what it does, what each flag means, potential risks.
Interactive: asks clarifying questions if needed.
```

### Basic Usage

```bash
# Explain a command you found online
gh copilot explain "curl -fsSL https://example.com | bash"

# Explain a git command
gh copilot explain "git log --oneline --graph --all --decorate"

# Explain a kubectl command
gh copilot explain "kubectl get pods --all-namespaces -o wide --sort-by=.metadata.creationTimestamp"

# Explain a pipe chain
gh copilot explain "ps aux | sort -rk 3,3 | head -n 5"

# Explain a complex find command
gh copilot explain "find . -name '*.log' -mtime +7 -exec rm {} \;"

# Explain an openssl command (great for security contexts)
gh copilot explain "openssl req -x509 -nodes -days 365 -newkey rsa:2048 -keyout key.pem -out cert.pem"
```

### Example Output

```
$ gh copilot explain "git log --oneline --graph --all --decorate"

Copilot: This command shows a compact visual representation of your git history:

  --oneline      Condenses each commit to one line (hash + message)
  --graph        Shows ASCII art branch/merge visualization
  --all          Includes all branches (not just current)
  --decorate     Shows branch names and tags next to commits

Combined effect: a full visual map of all branches and their commit history,
one line per commit. Useful for understanding branch topology before a rebase or merge.

Safety: This is a read-only command — it only displays information.
```

### Interactive Mode

```bash
# gh copilot explain is interactive — it asks if you want to explain something else:

$ gh copilot explain "awk '{print $2}' file.txt"

Copilot: This prints the second whitespace-separated field from each line of file.txt.
  awk       invokes the awk text processing tool
  '{print $2}'  the awk program: for each line, print the second field ($1=first, $2=second...)
  file.txt  input file

? What would you like to do?
> Ask another question
  Exit
```

### Production Patterns

```bash
# Before running any "curl | bash" pattern — ALWAYS explain it first:
gh copilot explain "curl -fsSL https://get.docker.com | sh"

# Explain AWS CLI commands before applying to production:
gh copilot explain "aws s3 sync . s3://my-bucket --delete --exclude '.git/*'"

# Understand CI pipeline commands from error logs:
gh copilot explain "docker buildx build --platform linux/amd64,linux/arm64 --push -t myrepo/app:latest ."
```

---

## Topic 4: `gh copilot suggest`

### What It Does

```text
Takes a plain-English description of what you want to do.
Returns: the exact shell command(s) to accomplish it.
Interactive: lets you revise, copy to clipboard, or run directly.
Supports target types: shell, git, gh (GitHub CLI)
```

### Basic Usage

```bash
# General shell commands
gh copilot suggest "list all running docker containers with their memory usage"

# Git operations
gh copilot suggest -t git "squash the last 3 commits into one"

# GitHub CLI operations  
gh copilot suggest -t gh "create a PR from current branch to main with a draft flag"

# Kubernetes operations
gh copilot suggest "get all pods in the prod namespace that are not Running"

# System operations
gh copilot suggest "find all files larger than 100MB in the current directory recursively"
```

### Target Types

```bash
# -t shell (default) — general shell commands
gh copilot suggest -t shell "compress the logs folder and exclude .git"

# -t git — git-specific commands
gh copilot suggest -t git "undo the last commit but keep the changes staged"

# -t gh — GitHub CLI commands
gh copilot suggest -t gh "list all open PRs in the current repo assigned to me"
```

### Example Session

```
$ gh copilot suggest "delete all docker images that have no tag (dangling images)"

Copilot: docker image prune -f

? Select an option
> Copy command to clipboard
  Explain command
  Revise command
  Exit

# After selecting "Explain command":
Copilot: docker image prune -f
  docker image prune  removes all dangling images (images with no tag and not referenced by any container)
  -f                  forces removal without confirmation prompt

Safety: This only removes truly dangling images. It does not remove tagged images or images in use by containers.
```

### High-Value Patterns

```bash
# Kubernetes admin tasks (saves huge time):
gh copilot suggest "get the logs from the most recently crashed pod in namespace prod"
gh copilot suggest "scale payment-service deployment to 5 replicas in prod namespace"
gh copilot suggest "port forward port 5432 from postgres pod in database namespace"

# Git history operations (easy to get wrong):
gh copilot suggest -t git "show me all commits that touched the payment_service.py file"
gh copilot suggest -t git "find the commit that introduced the word 'stripe_charge_id'"
gh copilot suggest -t git "rebase current branch onto main and keep our changes on conflicts"

# AWS operations:
gh copilot suggest "download the most recent CloudWatch log group for my Lambda function"
gh copilot suggest "list all EC2 instances with tag Environment=production and their state"

# File operations:
gh copilot suggest "replace all occurrences of 'api.v1' with 'api.v2' in all .py files"
gh copilot suggest "find all Python files modified in the last 2 days"
```

---

## Topic 5: Shell Aliases (`gh copilot alias`)

### Built-In Alias Generation

```bash
# Generate and install shell aliases automatically
gh copilot alias -- zsh    # for zsh (adds to ~/.zshrc)
gh copilot alias -- bash   # for bash (adds to ~/.bashrc)
gh copilot alias -- fish   # for fish

# This installs:
# ??          → gh copilot suggest (shell mode)
# git?        → gh copilot suggest -t git
# gh?         → gh copilot suggest -t gh
```

### After Running Alias Command

```bash
# Reload your shell
source ~/.zshrc   # or source ~/.bashrc

# Now you can use:
?? "compress this directory excluding node_modules"
git? "cherry-pick commits from feature branch to hotfix"
gh? "create release v1.2.3 with notes from CHANGELOG"
```

### Custom Aliases

```bash
# Add to ~/.zshrc for domain-specific shortcuts:
alias 'k8s?'='gh copilot suggest -t shell "kubectl"'
alias 'aws?'='gh copilot suggest -t shell "aws cli"'
alias 'explain'='gh copilot explain'

# Usage:
explain "grep -rn 'TODO' . --include='*.py' | wc -l"
```

---

## Topic 6: CLI vs VS Code Chat — Decision Guide

```text
Task                                           CLI    VS Code Chat
----                                           ---    -----------
Explain a command I found online               ✓      (switch to terminal)
Generate a kubectl/docker/aws command          ✓      both work
Explain bash script or one-liner               ✓      both work
Debug why a git operation went wrong           ✓      ✓
Generate code that goes into a file            ✗      ✓
Explain code in the editor                     ✗      ✓
Multi-file refactoring                         ✗      ✓ (Edits/Agent)
Reference a specific file (#file:)             ✗      ✓
SSH into remote server (no VS Code)            ✓      ✗
CI environment (headless)                      ✓      ✗
Quick command, don't want to leave terminal    ✓      ✗
```

---

## Topic 7: Safety Rules for CLI Copilot

```text
ALWAYS before running a suggested command:
  1. Read the full command — every flag matters
  2. Use "Explain command" option to understand it before running
  3. Never run destructive commands without verifying:
     - Does it delete, overwrite, or push somewhere?
     - Does it affect production resources?
     - Does it contain wildcards (rm -rf *, s3 sync --delete)?

NEVER let gh copilot suggest run a command automatically.
ALWAYS use "Copy to clipboard" or "Revise command" first.
For destructive operations: test on dev/staging first.

Red-flag patterns to always explain first:
  ✗ Any command with --delete, -rf, --force, DROP, TRUNCATE
  ✗ Any command that includes a URL (pipe-to-bash pattern)
  ✗ Any AWS/GCP/Azure command that modifies infrastructure
  ✗ kubectl delete, kubectl apply on production namespace
```

---

## Topic 8: Integration Into Daily Workflow

### Morning Terminal Startup

```bash
# Instead of googling "how to list all docker containers with CPU usage":
?? "show all running docker containers with their CPU and memory usage"

# Instead of remembering exact git log format:
git? "show all commits on current branch not in main as one-liners"

# Instead of looking up kubectl syntax:
?? "get all pods in namespace prod sorted by restart count"
```

### Incident Response (Terminal-Focused)

```bash
# When SSH'd into a server during an incident:
explain "netstat -tlnp | grep LISTEN"
explain "ss -tulwn | grep :8080"
?? "show top 10 processes by CPU consumption"
?? "find all files opened by process with pid 1234"

# Kubernetes incident (kubectl commands you can't remember under pressure):
?? "get events for a specific pod in namespace prod sorted by time"
?? "exec into a running pod in the prod namespace interactively"
?? "watch pod status in real time for the payment namespace"
```

### Script Writing Assistance

```bash
# When writing a bash script and you're unsure of syntax:
?? "loop over all .json files in a directory and print the filename and its 'name' field using jq"
?? "bash function that retries a command up to 3 times with a 5 second wait between attempts"
?? "check if a port is open on a remote host without nmap"
```

---

## Topic 9: Interview Traps

| Trap | Reality |
|---|---|
| "Copilot CLI is the same as VS Code Copilot in a terminal" | CLI Copilot (`gh copilot`) is specifically designed for shell commands. It has no access to your files or codebase context — unlike VS Code Chat |
| "`gh copilot suggest` runs the command for you" | It only suggests the command. You choose to copy, explain further, revise, or exit. Nothing runs automatically |
| "You need VS Code to use Copilot" | `gh copilot` works in any terminal — SSH sessions, headless CI, Windows Terminal, macOS Terminal, WSL |
| "CLI Copilot needs a separate subscription" | It uses your existing GitHub Copilot subscription (Individual, Business, or Enterprise) |
| "`gh copilot alias` modifies global git config" | It modifies your shell config file (`~/.zshrc`, `~/.bashrc`). Git config is untouched |

---

## Topic 10: Revision Notes

- `gh copilot` = GitHub CLI extension (`gh extension install github/gh-copilot`)
- Two commands: `explain` (decode a command) + `suggest` (generate a command from English)
- Target types for suggest: `-t shell` (default), `-t git`, `-t gh`
- `gh copilot alias` installs `??`, `git?`, `gh?` shortcuts in your shell
- CLI surface: no file/codebase context; terminal-native only
- Always "Explain command" before running any suggested destructive operation
- Works SSH, headless CI, any terminal — not VS Code dependent
- Same Copilot subscription as VS Code — no extra cost

## Official Source Notes

- GitHub Copilot in the CLI: <https://docs.github.com/en/copilot/using-github-copilot/using-github-copilot-in-the-command-line>
- GitHub CLI: <https://cli.github.com/>
- gh copilot extension: <https://github.com/github/gh-copilot>
- Installing gh extensions: <https://docs.github.com/en/github-cli/github-cli/using-github-cli-extensions>
