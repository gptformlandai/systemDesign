# Copilot Debugging Handbook — Gold Sheet

> **Track**: Copilot Mastery Track — Group 4: Pro / Production Level
> **File**: 3 of 7 (Track File #23)
> **Audience**: Developers diagnosing Copilot failures and unexpected behavior
> **Read after**: SDLC-Automation-With-Copilot-Gold-Sheet.md

---

## 1. Practical Impact Meter

| Topic | Impact | Why Developers Don't Know This |
|---|---|---|
| 25 Copilot failure modes — catalogued | ★★★★★ | Devs give up on Copilot when it fails; most failures have simple fixes |
| Auth and extension failures — the most common | ★★★★★ | 80% of "Copilot doesn't work" is auth or extension state |
| Why Copilot ignores instructions — root causes | ★★★★★ | Instructions that are too vague, too long, or in wrong file are the usual causes |
| Bad Agent Mode — recovery patterns | ★★★★★ | Devs don't know git checkout . is the recovery command |
| Hallucinated APIs — how to detect | ★★★★★ | Generated code that uses non-existent methods is the silent killer |
| Premium usage and quota issues | ★★★★☆ | Devs hit quota limits but don't understand what used them |

---

## 2. The 25 Copilot Failure Modes

### Category 1 — Setup and Authentication

#### Failure 1: No inline suggestions appear

```
Symptom: Typing in a file produces no ghost text.
Likely causes:
  - Copilot is disabled for this file type (settings.json)
  - Copilot extension is paused (status bar shows pause icon)
  - Not signed in to GitHub
  - Monthly free tier limit reached

Diagnose:
  1. Check status bar icon — does it show ⚠ or pause?
  2. Command Palette → "GitHub Copilot: Enable"
  3. Command Palette → "GitHub: Show Signed In Accounts"
  4. Check settings: "github.copilot.enable" for this file language

Fix:
  Enable in settings, re-enable via Command Palette, or sign back in.

Prevention: Don't disable Copilot globally; disable per language type only.
```

#### Failure 2: Chat shows "Error: Request Failed" or no response

```
Symptom: Chat panel shows error or sits spinning.
Likely causes:
  - Network issue (VPN blocking api.githubcopilot.com)
  - Authentication token expired
  - GitHub API rate limiting

Diagnose:
  Output panel → GitHub Copilot Chat → look for HTTP error codes

Fix:
  - Disable VPN → test → re-enable
  - Sign out → sign back in
  - Reload VS Code window

Prevention: Allowlist api.githubcopilot.com in corporate firewall/VPN.
```

#### Failure 3: Copilot uses wrong GitHub account

```
Symptom: Copilot uses free tier limits but you have a Pro subscription.
Likely causes:
  - Signed into work/org account that doesn't have Copilot Pro
  - Multiple accounts; wrong one was used for authentication

Fix:
  Command Palette → "GitHub: Sign Out" → sign back in with correct account.
  Use VS Code Profiles to separate work vs personal account contexts.
```

---

### Category 2 — Instruction and Configuration Issues

#### Failure 4: Copilot ignores copilot-instructions.md

```
Symptom: Instructions in the file are not reflected in Copilot's behavior.
Likely causes:
  - File is not at .github/copilot-instructions.md (wrong location)
  - File contains contradictory instructions (earlier overrides later)
  - Instructions are too vague ("write clean code") to change behavior
  - VS Code setting "github.copilot.chat.codeGeneration.useInstructionFiles" is false

Diagnose:
  In Chat: "What instructions do you have for this workspace?"
  Copilot should summarize its loaded instructions.
  If it doesn't mention your file: the file is not loading.

Fix:
  - Verify file path: .github/copilot-instructions.md (not .copilot-instructions.md)
  - Enable in settings: "github.copilot.chat.codeGeneration.useInstructionFiles": true
  - Reload VS Code window
  - Simplify instructions — remove redundant or vague rules

Prevention: Test instructions with a specific verification prompt after every change.
```

#### Failure 5: Prompt files not appearing as slash commands

```
Symptom: Type / in Chat — prompt files don't appear.
Likely causes:
  - Files not in .github/prompts/ directory
  - Files don't have .prompt.md extension
  - Invalid YAML frontmatter (syntax error)

Diagnose:
  Open the prompt file → check frontmatter syntax carefully
  Look for: missing quotes, incorrect indentation, invalid field names

Fix:
  Correct the file path, extension, or frontmatter.
  Reload VS Code window.
```

#### Failure 6: Custom agent not appearing in agent picker

```
Symptom: Type @ in Chat — custom agent doesn't appear.
Likely causes:
  - File not in .github/agents/ directory
  - File doesn't have .agent.md extension
  - Invalid frontmatter (missing name or description)

Fix:
  Verify location, extension, and frontmatter fields (name, description are required).
```

---

### Category 3 — Response Quality Issues

#### Failure 7: Chat response is generic / ignores the actual code

```
Symptom: Copilot gives a textbook answer instead of an answer specific to your code.
Likely cause: No context attached to the prompt.

Fix:
  Add #file, #selection, or #codebase reference to the prompt.
  "Explain #selection" vs "Explain this code" — the first attaches the code.

Prevention: Always attach context explicitly. Copilot doesn't automatically see open files in Chat.
```

#### Failure 8: Copilot hallucinates API methods that don't exist

```
Symptom: Generated code calls methods that don't exist in the library.
Common examples:
  - session.execute_async() (doesn't exist — it's await session.execute())
  - httpx.get_async() (doesn't exist — use async with httpx.AsyncClient() as client)
  - datetime.now_utc() (doesn't exist — use datetime.utcnow() or datetime.now(timezone.utc))

Why it happens:
  - Training data may include beta APIs, docs examples, or incorrect Stack Overflow answers
  - Model makes up methods that "should" exist based on pattern matching

Detect:
  Before running: look up every method call you didn't recognize
  IDE: type hints or Pylance will flag non-existent methods
  Runtime: AttributeError on the method name

Fix:
  Check official library docs for the correct method.
  Tell Copilot: "session.execute_async doesn't exist in SQLAlchemy 2.x.
  The correct pattern is: result = await session.execute(stmt)"
  Ask Copilot to fix it with the correct pattern.

Prevention:
  Add your library version to copilot-instructions.md.
  "Use SQLAlchemy 2.x async patterns. Verify method names against official SQLAlchemy 2.x docs."
```

#### Failure 9: Copilot generates insecure code

```
Symptom: Generated code has SQL injection, hardcoded credentials, or no input validation.

Common insecure patterns Copilot generates:
  - query = f"SELECT * FROM users WHERE email = '{email}'"  (SQL injection)
  - API_KEY = "sk-real-key-here"  (hardcoded credentials)
  - subprocess.run(user_input, shell=True)  (command injection)

Why it happens:
  Training data includes insecure code examples from the internet.

Fix:
  Never accept generated SQL strings without checking for parameterized queries.
  Add a security.instructions.md with specific rules.
  Always run the security review prompt before accepting code.

Prevention:
  Add to security.instructions.md:
  "Never generate SQL queries with string interpolation. Always use parameterized queries."
  "Never hardcode credentials. Always use environment variables."
```

#### Failure 10: Copilot gives outdated library usage

```
Symptom: Generated code uses deprecated APIs.
Examples:
  - SQLAlchemy 1.x session.query() patterns (deprecated in 2.x)
  - Python typing.List instead of list (deprecated in 3.9+)
  - Old requests session patterns

Fix:
  Specify version in the prompt: "Using SQLAlchemy 2.x with async patterns"
  Specify version in copilot-instructions.md.
  Tell Copilot explicitly: "session.query() is deprecated. Use select() statement."
```

---

### Category 4 — Agent Mode Issues

#### Failure 11: Agent Mode makes wrong changes

```
Symptom: Agent Mode modifies files you didn't ask it to touch, or makes wrong logic changes.
Likely cause: Underspecified task — not enough constraints in the prompt.

Fix:
  Run: git checkout .  (restore from pre-session commit)
  Rewrite the prompt with explicit file-level constraints:
  "Do not modify any file outside src/services/user_service.py"
  "Do not change any existing test files"

Prevention: Pre-session commit is non-negotiable. Always include explicit constraint list.
```

#### Failure 12: Agent Mode loops on the same error

```
Symptom: Agent Mode tries the same fix 3+ times, test still fails, loops.
Fix:
  Stop Agent Mode.
  Diagnose the test failure manually.
  Fix it yourself OR tell Copilot exactly what the correct fix is.
  Do not let it loop — it will not self-correct after 3 failed attempts.
```

#### Failure 13: Agent Mode creates too many files / over-engineers

```
Symptom: Asked for one feature, Agent Mode created 15 files including an entire framework.
Likely cause: Vague goal ("build a notification system") without scope constraint.

Fix: git checkout . → restart with scoped prompt:
  "Create ONLY: one service class, one Pydantic schema, and one test file.
  No additional abstractions, no base classes, no additional layers."
```

---

### Category 5 — Context and Quality Issues

#### Failure 14: Too verbose answers

```
Symptom: Asked a simple question; got a 1000-word essay.
Fix in the prompt:
  "Answer in under 100 words."
  "Give the code only — no explanation."
  "One paragraph maximum."

Fix for recurring verbosity: add to copilot-instructions.md:
  "Prefer concise answers. For code requests: code first, explanation second (optional).
  Unless the user asks for explanation, show code only."
```

#### Failure 15: Copilot can't infer repo structure

```
Symptom: "I don't see a users module in this codebase" — but it exists.
Likely cause: The file isn't open and wasn't indexed by #codebase.

Fix:
  Open the relevant file, or use #file:src/services/user_service.py explicitly.
  For large repos: rebuild the codebase index (Command Palette → "GitHub Copilot: Rebuild Workspace Index")
```

---

### Category 6 — MCP Issues

#### Failure 16: MCP tool not appearing in Agent Mode

```
Symptom: MCP tools not listed when Agent Mode starts.
Diagnose:
  1. Check .vscode/mcp.json syntax (valid JSON?)
  2. Check environment variable is set: echo $GITHUB_TOKEN
  3. Check MCP server package is installable: npx -y @modelcontextprotocol/server-github --help
  4. Reload VS Code window

Fix: Correct the mcp.json, set the env variable, reload.
```

#### Failure 17: MCP tool fails during Agent Mode

```
Symptom: MCP tool is invoked but returns an error.
Diagnose: Check VS Code Output → GitHub Copilot for the specific MCP error.
Common causes:
  - Token doesn't have required permissions (GitHub MCP: needs repo read)
  - Rate limit on the MCP server's backend API
  - Network connectivity to the MCP server

Fix: Check token permissions, wait for rate limit reset, verify network.
```

---

### Summary Quick Reference

```
| Failure | First Fix |
|---|---|
| No inline suggestions | Command Palette → Enable, check settings |
| Chat error | Check VPN, sign out/in, reload window |
| Instructions ignored | Check file path, enable setting, simplify rules |
| Prompt files missing | Check .github/prompts/ and .prompt.md extension |
| Hallucinated API | Look up official docs, add version to instructions |
| Insecure code | Run security review prompt, add security instructions |
| Agent Mode wrong changes | git checkout . → re-prompt with constraints |
| Agent Mode loops | Stop → fix manually → restart with better constraints |
| Too verbose | Add word limit to prompt |
| MCP not appearing | Check mcp.json, env var, reload window |
```

---

## 3. Revision Checklist

- [ ] Can diagnose and fix: no inline suggestions, Chat error, instructions ignored
- [ ] Knows how to detect hallucinated API methods (IDE type check + manual verification)
- [ ] Knows `git checkout .` as the Agent Mode recovery command
- [ ] Has the security review habit after any generated code touching auth/SQL
- [ ] Knows the MCP 6-step diagnostic checklist
- [ ] Knows the quick reference table for all 17 failure modes covered
