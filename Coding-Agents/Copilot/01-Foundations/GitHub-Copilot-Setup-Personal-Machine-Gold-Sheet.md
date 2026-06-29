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

## 10. JetBrains IDE Setup (IntelliJ, PyCharm, WebStorm, GoLand)

### Installation

```
GitHub Copilot is available as a first-party JetBrains plugin.

Install via JetBrains Marketplace:
  1. Open Settings (Cmd+, on macOS / Ctrl+Alt+S on Windows/Linux)
  2. Go to: Plugins → Marketplace
  3. Search: "GitHub Copilot"
  4. Install the plugin published by "GitHub" (official)
  5. Restart the IDE when prompted

OR install from JetBrains website:
  https://plugins.jetbrains.com/plugin/17718-github-copilot
```

### Sign In to GitHub in JetBrains

```
1. After installation: a notification appears "GitHub Copilot requires authentication"
   OR: Tools → GitHub Copilot → Login to GitHub

2. A device code appears in the IDE — copy it

3. Browser opens: https://github.com/login/device
   Paste the device code → Authorize

4. Return to IDE — status bar shows "GitHub Copilot" icon (usually bottom-right)

Verify sign-in:
  Tools → GitHub Copilot → Copilot Status
  Should show: "GitHub Copilot is ready"
```

### JetBrains Copilot Features and Shortcuts

```
Inline Suggestions:
  Accept:         Tab
  Reject:         Escape
  Next suggestion: Alt+]    (macOS: Option+])
  Previous:       Alt+[    (macOS: Option+[)
  Accept word:    Ctrl+Right (macOS: Option+Right)

Copilot Chat (JetBrains):
  Open Chat panel: Tools → GitHub Copilot → Open GitHub Copilot Chat
  OR: the chat icon in the right sidebar

Inline Chat (in editor):
  Alt+Enter on selected code → "Ask GitHub Copilot"
  OR: right-click selected code → GitHub Copilot → [action]

Generate Tests:
  Right-click on a method → GitHub Copilot → Generate Tests
```

### JetBrains vs VS Code — Key Differences

```
Feature                    | VS Code              | JetBrains
---------------------------|----------------------|------------------
Inline suggestions         | Full support         | Full support
Chat panel                 | Full support         | Full support
Agent Mode                 | Full support         | Partial (check current version)
Edits mode                 | Full support         | Limited
Prompt files (.prompt.md)  | Full support         | Not supported directly
Custom agents (.agent.md)  | Full support         | Not supported directly
copilot-instructions.md    | Full support         | Partial support
MCP tools                  | Full support         | Limited (check version)
Code Review                | Full support         | Via GitHub.com only

Recommendation:
  Use VS Code as your primary Copilot environment.
  JetBrains is excellent for Java/Kotlin/Go development but has fewer Copilot-specific
  configuration options. Prompt files and custom agents require VS Code.
```

### JetBrains Troubleshooting

```
Issue: Copilot icon missing from status bar
  → Check: Plugin is enabled (Settings → Plugins → Installed → GitHub Copilot → enabled)
  → Check: IDE has internet access (test in browser)
  → Try: Tools → GitHub Copilot → Logout → Login again

Issue: No inline suggestions appear
  → Check: Editor → General → Code Completion → Show suggestions automatically (enabled)
  → Check: Copilot is not disabled for this file type
  → File: .idea/copilot.xml — delete and restart if corrupted

Issue: Authentication loop (keeps asking to log in)
  → Delete cached credentials: Keychain Access (macOS) → remove "GitHub Copilot" entry
  → Re-authenticate via Tools → GitHub Copilot → Login
```

---

## 11. GitHub Authentication — Complete Flow

### OAuth Device Flow (what actually happens)

```
When you sign in to Copilot in VS Code or JetBrains:

1. Your IDE requests a device code from GitHub
2. GitHub returns: a device code + verification URL
3. IDE shows the code; your browser opens the URL
4. You log into GitHub.com and paste the device code
5. GitHub generates an OAuth access token for the IDE
6. IDE stores this token in your system keychain:
   macOS: Keychain Access → "GitHub Copilot" or "vscode-github-auth"
   Windows: Credential Manager → Windows Credentials
   Linux: GNOME Keyring or KWallet (or plaintext fallback in ~/.config)
7. Every API call uses this stored token — no re-auth needed until token expires

Token expiry:
  OAuth tokens for GitHub Copilot typically don't have a fixed expiry.
  They expire when: you revoke them, you sign out, or GitHub security detects unusual use.
  If token is revoked: you see the ⚠ icon in the status bar.
```

### HTTPS vs SSH — Which to Use for What

```
GitHub OAuth token (used by Copilot extension): always HTTPS internally.
  You cannot use SSH for Copilot API calls — this is handled by the extension.

Git operations (clone, push, pull): your choice.

SSH is better for git operations because:
  - No password or token re-entry after initial setup
  - Session-based — doesn't expire like HTTPS personal access tokens
  - No risk of accidentally pasting a token into a git URL

HTTPS with personal access token is acceptable for:
  - CI/CD environments
  - Machines where SSH key management is inconvenient

Setup SSH for git (recommended):
  ssh-keygen -t ed25519 -C "your@email.com"
  # Press Enter for default location (~/.ssh/id_ed25519)
  # Set a passphrase (recommended for security)
  
  # Start ssh-agent so you don't retype passphrase every time:
  eval "$(ssh-agent -s)"
  ssh-add ~/.ssh/id_ed25519
  
  # Copy public key:
  cat ~/.ssh/id_ed25519.pub   # copy this output
  
  # Add to GitHub: Settings → SSH and GPG keys → New SSH key
  
  # Test:
  ssh -T git@github.com
  # Expected: "Hi username! You've successfully authenticated..."
```

### Managing Multiple GitHub Accounts

```
Scenario: personal GitHub account + work GitHub account on the same machine.

Option 1 — VS Code Profiles (simplest):
  Each VS Code profile can have a separate GitHub sign-in.
  Create a "Personal" profile and a "Work" profile.
  Copilot uses the account active in the current profile.

Option 2 — SSH config for git (for separate git identities):
  # ~/.ssh/config
  Host github-personal
    HostName github.com
    User git
    IdentityFile ~/.ssh/id_ed25519_personal

  Host github-work
    HostName github.com
    User git
    IdentityFile ~/.ssh/id_ed25519_work

  # Clone personal repos:
  git clone git@github-personal:yourusername/repo.git

  # Clone work repos:
  git clone git@github-work:workorg/repo.git

Option 3 — Separate machine user accounts (most isolated, overkill for most):
  Create a separate macOS/Linux user account for work.
  Each user account has its own SSH keys, git config, and VS Code settings.

Sign-in verification (confirm which account Copilot is using):
  VS Code: Command Palette → "GitHub: Show Signed In Accounts"
  Should match the account that has the active Copilot subscription.
```

### Revoke and Rotate Access

```
Revoke a Copilot token (e.g., if you suspect compromise):
  GitHub.com → Settings → Applications → Authorized OAuth Apps
  → Find "GitHub Copilot for VS Code" → Revoke
  → Sign back in fresh from VS Code

Review what has access to your GitHub account:
  GitHub.com → Settings → Applications → Authorized OAuth Apps
  Remove anything you don't recognize.

Personal Access Tokens (for GitHub CLI):
  GitHub.com → Settings → Developer settings → Personal access tokens
  Use fine-grained tokens, not classic tokens.
  Set expiration dates (90 days maximum recommended).
```

---

## 12. Extension Activation Deep Troubleshooting

### Diagnostic Flow — Start Here

```
Step 1: Check the status bar icon
  VS Code bottom-left (or bottom-right depending on layout):
  ✓ Copilot icon visible, no warning     → extension active, check suggestions settings
  ⚠ Copilot icon with warning            → hover for error message, see below
  No Copilot icon at all                 → extension not loaded, see below

Step 2: Check extension is installed and enabled
  Cmd+Shift+X → search "copilot" → verify:
    GitHub.copilot      → status: enabled (not disabled)
    GitHub.copilot-chat → status: enabled (not disabled)
  If disabled: click Enable

Step 3: Check Output logs
  View → Output → select "GitHub Copilot" from dropdown (top-right of output panel)
  Look for: error messages, authentication failures, API errors

Step 4: Reload window
  Cmd+Shift+P → "Developer: Reload Window"
  Often fixes transient activation issues.
```

### "Extension Not Found" or Extension Won't Load

```
Symptom: Extension installed but doesn't appear to do anything.

Cause 1: Extension requires a newer VS Code version.
  Fix: Help → Check for Updates → update VS Code

Cause 2: Extension is installed but quarantined by another security tool.
  Fix (macOS): System Preferences → Security → Privacy → check if VS Code is blocked
  Fix: Uninstall and reinstall the extension

Cause 3: Corrupted extension files.
  Fix: Cmd+Shift+P → "Extensions: Open Extensions Folder"
  Delete the GitHub.copilot folder → reinstall from Marketplace

Cause 4: Extension conflict with another extension.
  Fix: Disable all extensions → re-enable one by one until the conflict is found
  Common conflicts: other AI code completion extensions
```

### "GitHub Copilot is not licensed for use"

```
This error means your GitHub account does not have an active Copilot subscription.

Check your plan:
  github.com → Settings → Copilot → check subscription status
  
  If you have no subscription:
    → Start a free trial or subscribe at github.com/features/copilot

  If you have Copilot Business via an organization:
    → Confirm your org admin has granted you a seat
    → github.com → Your organizations → Settings → Copilot → Policies

  If you have Copilot Free:
    → Check monthly usage limits — free tier has usage caps
    → github.com → Settings → Copilot → Usage

Sign out and back in after fixing:
  Command Palette → "GitHub: Sign Out" → "GitHub: Sign In"
```

### Extension Activated but Suggestions Are Intermittent

```
Symptom: Suggestions appear sometimes but not consistently.

Cause 1: Typing too fast — Copilot waits for a brief pause.
  Fix: Pause for 300-500ms after typing. Copilot triggers on pause, not keypress.

Cause 2: File type disabled in settings.
  Fix: Check "github.copilot.enable" in settings.json for this file extension.

Cause 3: Large file — Copilot limits suggestions for very large files.
  Fix: No fix — split large files into smaller modules.

Cause 4: Network latency — slow connection delays suggestion generation.
  Diagnose: View → Output → GitHub Copilot → look for timeout messages.
  Fix: Check internet connection quality.

Cause 5: Monthly free tier limit reached.
  Fix: Check usage at github.com/settings/copilot or upgrade plan.
```

---

## 13. Account and Plan Deep Troubleshooting

### Understanding What Your Plan Includes

```
Copilot Free (available on any GitHub account):
  - Limited number of completions per month
  - Limited Chat messages per month
  - No Agent Mode
  - No Code Review
  - Fewer models available
  
  Check usage: github.com → Settings → Copilot → Usage

Copilot Pro (individual paid subscription):
  - Unlimited completions
  - Unlimited Chat
  - Agent Mode
  - Code Review
  - All available models
  - No org policy restrictions

Copilot Business (via organization):
  - All Pro features
  - Admin can restrict features via org policy
  - Admin can see usage data
  - Org admin must grant you a seat

Copilot Enterprise (large organizations):
  - All Business features
  - Custom model fine-tuning
  - Knowledge base / Bing integration
  - Enterprise audit logs

If a feature you expect is missing:
  1. Confirm your plan at github.com/settings/copilot
  2. If on Business: ask org admin if the feature is policy-restricted
  3. Some features roll out gradually — check the GitHub blog for availability
```

### Plan-Related Error Messages and Fixes

```
"Copilot is disabled by your organization"
  → Your org admin has disabled Copilot or specific features
  → You cannot override this — contact your org admin
  → For personal use: use a personal account with a personal subscription

"You have exceeded the limit"
  → Free tier monthly limits reached
  → Wait for the next month's reset OR upgrade to Pro
  → github.com → Settings → Copilot → Usage → see reset date

"Copilot is not available in your region"
  → Check current geographic availability: docs.github.com/copilot
  → Note: this is rare; Copilot is available in most regions

"Your account requires MFA"
  → Enable two-factor authentication: github.com → Settings → Password and authentication
  → Required for some organization policies

"Subscription has been cancelled"
  → Reactivate at github.com/settings/copilot
  → Or update payment method if card expired
```

### Diagnosing Auth Token Issues

```
Symptom: Was working, suddenly shows authentication error.

Check 1: Was the OAuth token revoked?
  github.com → Settings → Applications → Authorized OAuth Apps
  Look for "GitHub Copilot for VS Code" — if missing, it was revoked.
  Fix: Re-sign-in from VS Code.

Check 2: Did your organization's policy change?
  If on Business plan: check with org admin.

Check 3: Did GitHub detect suspicious activity and revoke the token?
  Check: github.com → Settings → Security → Recent activity
  If suspicious login detected: GitHub may have auto-revoked tokens.
  Fix: Verify your account is secure, then re-auth.

Check 4: System keychain issue (token stored but not readable).
  macOS fix:
    Keychain Access → search "github" → delete stale GitHub entries
    Re-sign-in in VS Code.
  Windows fix:
    Credential Manager → Windows Credentials → remove GitHub entries
    Re-sign-in in VS Code.
```

---

## 14. Verification Protocol — Comprehensive Test

### Level 1 — Basic Functionality (2 minutes)

```
Test 1: Inline suggestion
  1. Create file: test_copilot_verify.py (or .ts, .js)
  2. Type: # Calculate the sum of all even numbers in a list
  3. Press Enter → start typing: def
  4. Expected: ghost text suggests a function implementation
  5. Press Tab to accept

Test 2: Chat basic response
  1. Cmd+Shift+I to open Chat
  2. Type: "What is 2 + 2?"
  3. Expected: "4" or equivalent response (not an error or spinner)

Test 3: Status bar confirmation
  Verify: Copilot icon visible in status bar without ⚠ warning
```

### Level 2 — Context and Features (5 minutes)

```
Test 4: Context variable
  1. Open any code file
  2. Select a function
  3. In Chat: "Explain #selection"
  4. Expected: explanation specific to the selected code (not generic)

Test 5: File reference
  1. In Chat: "What does #file:[path to any file in your project] do?"
  2. Expected: specific description of that file's purpose

Test 6: Slash command (if you have prompt files)
  1. Type / in Chat
  2. Expected: your .github/prompts/*.prompt.md files appear as options
  3. Select one and verify it runs

Test 7: Custom agent (if you have agent files)
  1. Type @codebase-navigator in Chat
  2. Expected: the agent persona is invoked (response mentions navigation focus)
```

### Level 3 — Instructions Loaded (2 minutes)

```
Test 8: Instructions verification
  In Chat: "What instructions do you have for this workspace?"
  
  Expected: Copilot summarizes the rules from your copilot-instructions.md
  
  If it gives a generic answer ("I don't have specific instructions"):
    → Verify file is at .github/copilot-instructions.md (not .copilot-instructions.md)
    → Verify setting: "github.copilot.chat.codeGeneration.useInstructionFiles": true
    → Reload VS Code window
```

### Level 4 — Agent Mode (3 minutes)

```
Test 9: Agent Mode basic
  1. In Chat: switch to Agent mode (dropdown at top of Chat)
  2. Type: "List the files in the current workspace directory"
  3. Expected: Agent Mode reads the filesystem and lists files
  4. If it asks for permission to use a tool: click Allow

Test 10: Terminal integration
  1. In Chat Agent mode: "Run: echo 'Copilot Agent Mode is working'"
  2. Expected: opens terminal, runs the command, shows output
```

---

## 15. Verifying Your Setup Is Complete

Run through this checklist before proceeding to the next sheet:

```
Basic Setup:
[ ] VS Code installed (latest stable version)
[ ] GitHub Copilot extension installed (publisher: GitHub)
[ ] GitHub Copilot Chat extension installed (publisher: GitHub)
[ ] Signed in to correct GitHub account (verified via "Show Signed In Accounts")
[ ] Copilot status bar icon visible without warning
[ ] Plan confirmed at github.com/settings/copilot

Functionality:
[ ] Level 1 verification: inline suggestion appears in a test file
[ ] Level 2 verification: Chat responds to "Explain #selection"
[ ] Level 3 verification: "What instructions do you have?" returns project-specific rules
[ ] Level 4 verification (optional): Agent Mode lists workspace files

Configuration:
[ ] Recommended settings.json applied (from config/vscode-settings.json)
[ ] At least 5 keyboard shortcuts memorized
[ ] Practice repository created with .github/ folder
[ ] SSH key configured for git operations

Optional:
[ ] JetBrains plugin installed if you use a JetBrains IDE
[ ] Multiple GitHub accounts set up via VS Code Profiles if needed
```

---

## 16. Revision Checklist

- [ ] Can install Copilot in VS Code and JetBrains from scratch
- [ ] Understands the OAuth device flow (what happens when you sign in)
- [ ] Can distinguish HTTPS vs SSH for git and knows which to use when
- [ ] Can manage multiple GitHub accounts via VS Code Profiles
- [ ] Can revoke and re-issue a Copilot OAuth token
- [ ] Can run all 4 levels of verification tests
- [ ] Can troubleshoot: no suggestions, Chat errors, wrong account, plan issues, extension not loading
- [ ] Has recommended settings.json applied
- [ ] Has SSH key configured for GitHub
- [ ] Has a personal practice repository set up with .github/ folder
