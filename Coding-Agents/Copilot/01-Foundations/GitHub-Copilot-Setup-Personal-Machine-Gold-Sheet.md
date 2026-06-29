# GitHub Copilot Setup — Personal Machine — Gold Sheet

> **Track**: Copilot Mastery Track — Group 1: Foundations
> **File**: 2 of 6 (Track File #2)
> **Audience**: Developers installing Copilot for the first time or verifying their setup
> **Read after**: Copilot-Mental-Model-Gold-Sheet.md

---

## 1. Practical Impact Meter

| Topic | Impact | Why Developers Skip This |
|---|---|---|
| VS Code + Copilot extension — correct install | ★★★★★ | Installing wrong extension or wrong account causes silent failures |
| Account sign-in and plan verification | ★★★★★ | Free vs Pro vs Business plans have different feature sets |
| Verifying Copilot is active and working | ★★★★★ | Devs assume it works; it may be disabled by org policy or plan |
| Recommended VS Code settings | ★★★★☆ | Default settings leave several Copilot features turned off |
| Troubleshooting — auth, network, proxy | ★★★★☆ | Corporate VPN and proxy silently block Copilot API calls |
| Personal GitHub workflow setup | ★★★★☆ | Using wrong account in VS Code leads to wrong plan limits |
| Keyboard shortcuts for daily speed | ★★★★★ | Devs who don't know the shortcuts use Copilot 5x slower |

---

## 2. Prerequisites

```
Required:
  - A GitHub account (free tier is sufficient for Copilot Free plan)
  - VS Code (latest stable release recommended — 1.90+ for Agent Mode)
  - Internet connection (Copilot API is cloud-based)

Recommended:
  - Git installed and configured
  - A personal test repository for practicing Copilot features
  - A dedicated VS Code profile for Copilot experiments

Plans:
  Copilot Free    — limited completions and chat messages per month, no Agent Mode
  Copilot Pro     — unlimited completions, chat, Agent Mode, Code Review, all models
  Copilot Business— team management, policy controls, audit logs
  Copilot Enterprise — org-level customization, fine-tuning, security features

Note: Feature availability changes. Verify current plan features at:
https://github.com/features/copilot/plans
```

---

## 3. Step-by-Step Installation

### Step 1 — Install VS Code

```bash
# Download from: https://code.visualstudio.com/
# Or via Homebrew on macOS:
brew install --cask visual-studio-code

# Verify installed:
code --version
```

### Step 2 — Install GitHub Copilot Extension

```
In VS Code:
  1. Open Extensions panel: Cmd+Shift+X (macOS) / Ctrl+Shift+X (Windows/Linux)
  2. Search: "GitHub Copilot"
  3. Install the extension published by "GitHub" (verified publisher)
     Extension ID: GitHub.copilot
  4. Also install: "GitHub Copilot Chat"
     Extension ID: GitHub.copilot-chat
  5. Reload VS Code when prompted

Verify correct publisher:
  The official extensions are from publisher "GitHub" with a blue verified badge.
  Do NOT install third-party Copilot extensions — they may not be official.
```

### Step 3 — Sign In to GitHub

```
1. After installing, VS Code shows a sign-in prompt in the bottom status bar.
2. Click "Sign in to GitHub" or open Command Palette: Cmd+Shift+P → "GitHub Copilot: Sign In"
3. Browser opens → authorize VS Code to access your GitHub account
4. Return to VS Code — status bar shows "GitHub Copilot" icon (not ⚠ warning)

If the status bar icon shows a warning:
  → Hover over it to read the error
  → Common causes: no active Copilot subscription, VPN blocking auth

Verify active account:
  Command Palette → "GitHub: Show Signed In Accounts"
  Should show your GitHub username
```

### Step 4 — Verify Copilot Is Active

```
Test inline suggestions:
  1. Create a new Python file: test_copilot.py
  2. Type: def calculate_fibonacci(n):
  3. Press Enter — you should see grey ghost text suggesting the function body
  4. Press Tab to accept

Test Chat:
  1. Open Chat panel: Cmd+Shift+I or click the Copilot icon in the sidebar
  2. Type: "What is 2 + 2?"
  3. Copilot should respond in the Chat panel

If no ghost text appears:
  → Check status bar for Copilot icon
  → Open Command Palette → "GitHub Copilot: Enable"
  → Check that you are signed in with a plan that includes completions
```

---

## 4. Recommended VS Code Settings

Add these to your VS Code `settings.json` (Cmd+Shift+P → "Open User Settings JSON"):

```json
{
  // Copilot — General
  "github.copilot.enable": {
    "*": true,
    "markdown": true,
    "yaml": true,
    "plaintext": false
  },

  // Chat — prefer longer, more detailed responses
  "github.copilot.chat.localeOverride": "en",

  // Editor — show inline suggestions immediately
  "editor.inlineSuggest.enabled": true,
  "editor.inlineSuggest.showToolbar": "onHover",

  // Copilot — allow suggestions in comments (useful for prompt-driven coding)
  "github.copilot.inlineSuggest.enable": true,

  // Agent Mode — allow file system and terminal tool use
  "chat.agent.enabled": true,
  "github.copilot.chat.agent.runTasks": true,

  // Copilot Chat — use workspace index for #codebase
  "github.copilot.chat.codeGeneration.useInstructionFiles": true,

  // Source control — show Copilot commit message suggestion
  "github.copilot.git.generateCommitMessageOnAcceptAll": true,

  // Recommended editor settings for AI-assisted work
  "editor.formatOnSave": true,
  "editor.wordWrap": "on",
  "files.autoSave": "afterDelay",
  "files.autoSaveDelay": 1000
}
```

---

## 5. Essential Keyboard Shortcuts

### Inline Suggestions

| Action | macOS | Windows/Linux |
|---|---|---|
| Accept suggestion | `Tab` | `Tab` |
| Reject suggestion | `Escape` | `Escape` |
| Next suggestion | `Alt+]` | `Alt+]` |
| Previous suggestion | `Alt+[` | `Alt+[` |
| Accept word by word | `Cmd+→` | `Ctrl+→` |
| Open Copilot hover | `Cmd+.` | `Ctrl+.` |

### Chat

| Action | macOS | Windows/Linux |
|---|---|---|
| Open Chat panel | `Cmd+Shift+I` | `Ctrl+Shift+I` |
| Open inline Chat | `Cmd+I` | `Ctrl+I` |
| New Chat conversation | `Cmd+L` | `Ctrl+L` (in Chat panel) |
| Send message | `Enter` | `Enter` |
| Newline in message | `Shift+Enter` | `Shift+Enter` |

### Other

| Action | macOS | Windows/Linux |
|---|---|---|
| Open Command Palette | `Cmd+Shift+P` | `Ctrl+Shift+P` |
| Generate commit message | Click ✨ in Source Control | Click ✨ in Source Control |

---

## 6. Recommended VS Code Extensions Alongside Copilot

```
Required:
  GitHub.copilot                     — Copilot inline suggestions
  GitHub.copilot-chat                — Copilot Chat, Edits, Agent Mode

Strongly Recommended:
  GitHub.vscode-pull-request-github  — PR creation and review in VS Code
  eamodio.gitlens                    — Git history and blame (provides context to Copilot)
  esbenp.prettier-vscode             — Auto-format (keeps Copilot output clean)
  ms-python.python                   — Python language support
  ms-python.vscode-pylance           — Python type checking
  dbaeumer.vscode-eslint             — JS/TS linting

For Testing:
  hbenl.vscode-test-explorer         — Test explorer UI
  ms-python.debugpy                  — Python debugging

For Markdown (notes and docs):
  yzhang.markdown-all-in-one         — Markdown preview and shortcuts
  DavidAnson.vscode-markdownlint     — Markdown linting
```

---

## 7. VS Code Profiles — Keep Copilot Work Organized

```
VS Code Profiles let you have different extension sets and settings per project type.
This prevents extension conflicts and keeps Copilot context focused.

Create a Copilot Learning Profile:
  1. File → Preferences → Profiles → Create Profile
  2. Name: "Copilot Labs"
  3. Copy from: Default
  4. Add only Copilot + language extensions

Benefits:
  - Copilot suggestions are not polluted by unrelated extensions
  - Settings are not shared with other project profiles
  - Easy to switch between personal/work contexts
```

---

## 8. Personal GitHub Workflow Setup

```
For a personal machine, follow this setup:

1. One GitHub account per machine is simplest.
   If you need work + personal: use VS Code Profiles with different GitHub accounts.

2. Configure Git identity:
   git config --global user.name "Your Name"
   git config --global user.email "your@email.com"

3. Use SSH for GitHub authentication (more reliable than HTTPS for long sessions):
   ssh-keygen -t ed25519 -C "your@email.com"
   # Add public key to GitHub: Settings → SSH and GPG keys

4. Create a personal Copilot practice repository:
   gh repo create copilot-practice --private --clone
   cd copilot-practice
   mkdir -p .github/instructions .github/prompts .github/agents

5. This practice repo is where you experiment with:
   - copilot-instructions.md
   - Prompt files
   - Custom agents
   - Before/after examples
```

---

## 9. Troubleshooting Common Issues

### Issue: No inline suggestions appear

```
Diagnosis:
  1. Check status bar — is Copilot icon visible without warning?
  2. Command Palette → "GitHub Copilot: Enable"
  3. Check settings: "github.copilot.enable" for the current file language
  4. Check current file — plaintext and some non-code files have suggestions disabled
  5. Check subscription: free plan has monthly limits

Fix:
  → Sign out and sign back in: Command Palette → "GitHub: Sign Out"
  → Reload VS Code window: Cmd+Shift+P → "Developer: Reload Window"
```

### Issue: Chat panel shows error or empty responses

```
Diagnosis:
  1. Check internet connection
  2. Check VPN — some corporate VPNs block Copilot API endpoints (api.githubcopilot.com)
  3. Open VS Code Output panel → select "GitHub Copilot Chat" from dropdown → look for error messages

Fix:
  → Allowlist: api.githubcopilot.com and copilot-proxy.githubusercontent.com in VPN/firewall
  → Disable and re-enable the Copilot Chat extension
  → Sign out and sign in again
```

### Issue: Copilot suggests code from wrong language or framework

```
Cause:
  Copilot is seeing unrelated files open in your editor.
  Its context includes nearby open files, which may be from a different stack.

Fix:
  → Close all unrelated tabs before a Copilot session
  → Use VS Code Profiles to keep language environments separate
  → Add explicit language/library constraints to your prompt
```

### Issue: VPN or corporate proxy blocking Copilot

```
Corporate machines may route all traffic through a proxy.

Fix:
  → In VS Code settings, add proxy configuration:
    "http.proxy": "http://proxy.company.com:8080"
    "http.proxyStrictSSL": false  (only if internal CA is not trusted)
  → Ask your IT team to allowlist GitHub Copilot API endpoints
  → Alternatively, use Copilot on a personal machine outside the corporate network
```

### Issue: Wrong GitHub account — using work account instead of personal

```
Fix:
  → Command Palette → "GitHub: Sign Out"
  → Command Palette → "GitHub: Sign In" → sign in with correct account
  → Use VS Code Profiles with different accounts per profile
```

---

## 10. Verifying Your Setup Is Complete

Run through this checklist before proceeding to the next sheet:

```
[ ] VS Code installed (latest stable version)
[ ] GitHub Copilot extension installed (publisher: GitHub)
[ ] GitHub Copilot Chat extension installed (publisher: GitHub)
[ ] Signed in to correct GitHub account
[ ] Copilot status bar icon visible without warning
[ ] Inline suggestion appears in a test Python/JavaScript file
[ ] Chat panel opens and responds to a test message
[ ] Recommended settings.json updated
[ ] At least 3 keyboard shortcuts memorized: Tab, Escape, Cmd+Shift+I
[ ] Practice repository created
[ ] .github/ folder created in practice repo
```

---

## 11. Revision Checklist

- [ ] Can install Copilot extensions in VS Code from scratch
- [ ] Can verify Copilot is active via status bar
- [ ] Knows at least 5 keyboard shortcuts
- [ ] Has recommended settings.json applied
- [ ] Can distinguish Copilot Free vs Pro feature sets
- [ ] Can diagnose and fix: no suggestions, Chat errors, wrong account
- [ ] Has a personal practice repository set up
- [ ] Knows how to use VS Code Profiles for multi-context work
