# MCP Integration with Copilot — Gold Sheet

> **Track**: Copilot Mastery Track — Group 3: Advanced Engineering
> **File**: 5 of 7 (Track File #18)
> **Audience**: Developers extending Copilot capabilities with MCP tools
> **Read after**: Token-Optimization-Gold-Sheet.md

---

## 1. Practical Impact Meter

| Topic | Impact | Why Developers Miss This |
|---|---|---|
| What MCP is — the protocol, not the tool | ★★★★★ | Devs confuse MCP with specific tools; it's a universal protocol |
| MCP servers vs MCP clients | ★★★★★ | Devs don't know Copilot is the client — MCP servers run separately |
| `.vscode/mcp.json` configuration | ★★★★★ | Without this, no MCP tools appear in Copilot Agent Mode |
| Security risks — what MCP can access | ★★★★★ | MCP tools can read files, run commands — least privilege is critical |
| Debugging MCP tools not appearing | ★★★★☆ | Most common MCP setup issue has a simple fix |
| Use cases — GitHub, filesystem, browser automation | ★★★★☆ | Devs don't know what MCP enables; missing major productivity wins |

---

## 2. What MCP Is

### Must Know

```
MCP = Model Context Protocol

A standard protocol that lets AI models (like Copilot) use external tools.

Without MCP:
  Copilot can only read and write files in your workspace.
  It cannot: browse the web, query GitHub API, run database queries,
  interact with browsers, call REST APIs, or read from external data sources.

With MCP:
  Copilot can do all of those things — through MCP server tools.
  Each MCP server provides a set of tools that Copilot can call.
  Copilot stays the "brain"; MCP servers are the "hands".

Architecture:
  Copilot (MCP client) ↔ MCP Protocol ↔ MCP Server (runs locally or remotely)
                                                ↕
                                        External system (GitHub, browser, DB, filesystem)
```

### MCP vs Built-in Copilot Capabilities

```
Without MCP (built-in):                  | With MCP:
-------------------------------------|---------------------------------------
Read/write workspace files           | Query GitHub Issues, PRs, repos (GitHub MCP)
Run terminal commands (Agent Mode)   | Control a real browser (Playwright MCP)
Analyze code                         | Query a real database (DB MCP)
Search workspace index (#codebase)   | Read web pages (browser/fetch MCP)
Use context variables (#file, etc.)  | Call custom REST APIs (HTTP MCP)
Generate GitHub Actions YAML         | Actually trigger CI/CD (if MCP tool for it)
```

---

## 3. MCP Configuration — `.vscode/mcp.json`

### Template Configuration

```json
{
  "servers": {
    "github": {
      "type": "stdio",
      "command": "npx",
      "args": ["-y", "@modelcontextprotocol/server-github"],
      "env": {
        "GITHUB_PERSONAL_ACCESS_TOKEN": "${env:GITHUB_TOKEN}"
      }
    },
    "filesystem": {
      "type": "stdio",
      "command": "npx",
      "args": [
        "-y",
        "@modelcontextprotocol/server-filesystem",
        "/Users/yourname/projects"
      ]
    },
    "playwright": {
      "type": "stdio",
      "command": "npx",
      "args": ["-y", "@executeautomation/playwright-mcp-server"]
    }
  }
}
```

### Security Rules for MCP Configuration

```
1. Never hardcode tokens in mcp.json:
   BAD:  "GITHUB_TOKEN": "ghp_actualtoken123..."
   GOOD: "GITHUB_TOKEN": "${env:GITHUB_TOKEN}"
   The env variable must be set in your shell environment, not in the file.

2. Do NOT commit mcp.json with secrets:
   Add to .gitignore: .vscode/mcp.json
   Instead, commit: .vscode/mcp.example.json (with placeholders)

3. Scope filesystem MCP to minimum needed directories:
   BAD:  path argument: "/" (entire filesystem)
   GOOD: path argument: "/Users/yourname/projects/my-project-only"

4. Only enable MCP servers you actively use:
   Each server is a potential attack surface — unused servers should be removed.
   Review your MCP configuration quarterly.

5. Treat MCP servers as running processes with your credentials:
   A malicious MCP server could exfiltrate your GitHub token.
   Only use MCP servers from trusted sources with published, auditable code.
```

---

## 4. Common MCP Servers and Use Cases

### GitHub MCP Server

```
Source: @modelcontextprotocol/server-github
Tools provided: search repos, read issues, read PRs, list commits, read file content

Use cases with Copilot:
  "Use the GitHub MCP tool to list open issues in my-org/my-repo
  that are labeled 'bug' and created in the last 7 days"
  
  "Find the PR that introduced the payment processing feature
  using the GitHub MCP tool and summarize the changes"

Requirements:
  GITHUB_PERSONAL_ACCESS_TOKEN with: repo (read), issues (read)
  Use a fine-grained token — not a classic token with full access
```

### Filesystem MCP Server

```
Source: @modelcontextprotocol/server-filesystem
Tools provided: read files, list directories, search files

Use cases with Copilot:
  Reading files outside the current workspace
  Comparing files across multiple project directories
  Searching for patterns across many repos simultaneously

Security note:
  Only grant access to directories you explicitly want Copilot to read.
  Do NOT grant access to home directory root — scope to project directories only.
```

### Playwright MCP Server

```
Source: @executeautomation/playwright-mcp-server (or similar)
Tools provided: navigate browser, click elements, fill forms, take screenshots

Use cases with Copilot:
  "Navigate to localhost:3000/login and verify the login form renders correctly"
  "Take a screenshot of the dashboard page and describe what you see"
  "Test the user registration flow end to end in the browser"

Warning:
  Playwright MCP controls a REAL browser.
  Do NOT use with production URLs — only local or test environments.
  Never automate login to real accounts — only test accounts.
```

---

## 5. Debugging MCP Tools Not Appearing

```
Symptom: MCP tools don't appear in Copilot Agent Mode

Check 1: Is mcp.json in the right location?
  → Must be at .vscode/mcp.json in the workspace root
  → Not: .vscode/mcp.config.json or any other name

Check 2: Is the JSON valid?
  → Open the file in VS Code — any syntax error will be highlighted
  → Use: cat .vscode/mcp.json | python3 -m json.tool to validate

Check 3: Is the MCP server package installed?
  → npx -y @modelcontextprotocol/server-github --help
  → If error: the package may not exist or may be incompatible

Check 4: Is the environment variable set?
  → echo $GITHUB_TOKEN (for GitHub MCP)
  → If empty: export GITHUB_TOKEN=your_token in your shell before opening VS Code

Check 5: Reload VS Code window:
  → Command Palette → "Developer: Reload Window"
  → MCP servers are initialized at startup

Check 6: Check VS Code Output panel:
  → View → Output → select "GitHub Copilot" from dropdown
  → Look for MCP-related error messages
```

---

## 6. Least Privilege MCP Principles

```
Principle 1 — Use fine-grained tokens:
  GitHub: Create a fine-grained token with ONLY the repos and permissions needed.
  Don't use: a classic token with "repo" scope (full access to all repos).
  Use: fine-grained token scoped to specific repos + read-only permissions.

Principle 2 — Scope filesystem access tightly:
  Grant access to: /Users/name/projects/specific-project/
  Not to: /Users/name/ or /

Principle 3 — Run MCP servers locally:
  Local servers are safer than remote servers — data doesn't leave your machine.
  If using a remote MCP server: treat it like any external API — vet it carefully.

Principle 4 — Rotate tokens regularly:
  MCP tokens should be treated like API keys — rotate every 90 days.
  GitHub: Settings → Developer settings → Fine-grained tokens → set expiration.

Principle 5 — Review MCP server permissions in Agent Mode:
  When Copilot proposes to use an MCP tool, it shows what the tool will do.
  Read the tool action before confirming it.
  "Read file X" is different from "write file X" — verify before proceeding.
```

---

## 7. Revision Checklist

- [ ] Can explain what MCP is and how it differs from built-in Copilot capabilities
- [ ] Can create a `.vscode/mcp.json` with at least 2 MCP servers configured
- [ ] Knows never to commit secrets in mcp.json (uses `.gitignore` and env vars)
- [ ] Knows the GitHub, filesystem, and Playwright MCP server use cases
- [ ] Can diagnose "MCP tools not appearing" with the 6-step checklist
- [ ] Applies least privilege principles: fine-grained tokens, scoped filesystem
- [ ] Has `.vscode/mcp.example.json` committed and `.vscode/mcp.json` gitignored
