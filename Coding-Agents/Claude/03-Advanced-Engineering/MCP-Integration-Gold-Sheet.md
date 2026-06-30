# MCP Integration with Claude — Gold Sheet

> **Track**: Claude Mastery Track — Group 3: Advanced Engineering
> **File**: 3 of 7 (Track File #17)
> **Read after**: Skills-System-Gold-Sheet.md

---

## Practical Impact Meter

| Topic | Impact | Why Developers Miss This |
|-------|--------|--------------------------|
| What MCP is — protocol vs tool | ★★★★★ | Devs think MCP = specific servers; it's a universal protocol |
| `.claude/mcp.json` config | ★★★★★ | Without this, no MCP tools appear in Claude Code Agent Mode |
| Security rules — credentials in env vars | ★★★★★ | Hardcoded tokens in mcp.json = secrets committed to repo |
| GitHub MCP — issues, PRs, code | ★★★★☆ | Enables Claude to query your repo without copy-paste |
| Filesystem MCP — scoped access | ★★★★☆ | Lets Claude read across multiple project directories |
| Debugging MCP tools not appearing | ★★★★☆ | Most common setup issue has a 5-step fix |

---

## 1. What MCP Is

```
MCP = Model Context Protocol

A standard protocol that lets Claude (and any AI) use external tools.

Without MCP:
  Claude Code can only read and write files in your workspace.
  It cannot: query GitHub issues, control a browser, read external DBs,
  call REST APIs, or access files outside your project.

With MCP:
  Claude becomes the "brain" — MCP servers are the "hands."
  Each MCP server provides typed tools Claude can invoke by name.

Architecture:
  Claude (MCP client) ↔ MCP Protocol ↔ MCP Server
                                              ↕
                                  External system (GitHub / browser / DB / filesystem)

Key insight: MCP is a standard. Any server that speaks MCP can be used by Claude.
Claude does NOT need to be updated to use a new MCP server.
```

---

## 2. Claude MCP Configuration — `.claude/mcp.json`

### File Location

```
.claude/mcp.json   ← project-level (only for this project)
~/.claude/mcp.json ← global (available in all Claude Code sessions)
```

### Template Configuration

```json
{
  "mcpServers": {
    "github": {
      "command": "npx",
      "args": ["-y", "@modelcontextprotocol/server-github"],
      "env": {
        "GITHUB_PERSONAL_ACCESS_TOKEN": "${GITHUB_TOKEN}"
      }
    },
    "filesystem": {
      "command": "npx",
      "args": [
        "-y",
        "@modelcontextprotocol/server-filesystem",
        "/Users/yourname/projects"
      ]
    },
    "playwright": {
      "command": "npx",
      "args": ["-y", "@playwright/mcp"]
    }
  }
}
```

### Security Rules

```
Rule 1: NEVER hardcode tokens in mcp.json
  BAD:  "GITHUB_PERSONAL_ACCESS_TOKEN": "ghp_abc123realtoken"
  GOOD: "GITHUB_PERSONAL_ACCESS_TOKEN": "${GITHUB_TOKEN}"
  The variable must be set in your shell environment before running Claude.

Rule 2: Add mcp.json to .gitignore if it references env vars
  Even with env var substitution, the file structure reveals which services you use.
  Option A: gitignore mcp.json, commit mcp.example.json with placeholders.
  Option B: commit mcp.json (with env var refs only, no hardcoded values).

Rule 3: Scope filesystem access to minimum directories
  BAD:  args: ["-y", "@.../server-filesystem", "/"]   (entire filesystem)
  GOOD: args: ["-y", "@.../server-filesystem", "/Users/name/projects/my-project"]

Rule 4: Use fine-grained GitHub tokens — not personal tokens with full access
  Create: GitHub Settings → Developer settings → Fine-grained PAT
  Scope: only the repos Claude needs, read-only unless write is required.

Rule 5: Only enable servers you actively use
  Each server is a running process with access to your credentials.
  Disable (remove from config) any server not in current use.
```

---

## 3. Key MCP Servers

### GitHub MCP

```
Package: @modelcontextprotocol/server-github
Tools: search_repositories, get_file_contents, list_issues, get_issue,
       list_pull_requests, get_pull_request, create_issue, search_code

Use cases with Claude:
  "Using the GitHub MCP tool, list all open issues labeled 'bug'
  in my-org/my-repo created in the last 14 days"

  "Find the PR that introduced the authentication refactor.
  Summarize the changes and any review comments."

  "Search for all usages of the deprecated get_user_v1() function
  across all files in my-org/my-repo"

Required env var: GITHUB_PERSONAL_ACCESS_TOKEN
Token scope needed: repo:read, issues:read (add write only if creating issues)
```

### Filesystem MCP

```
Package: @modelcontextprotocol/server-filesystem
Tools: read_file, write_file, list_directory, search_files, create_directory

Use cases with Claude:
  Reading files outside the current workspace (documentation in another repo)
  Comparing configuration files across multiple project directories
  Searching for patterns across many repos simultaneously

Security: scope to /Users/name/projects/ not /Users/name/ or /
```

### Playwright / Browser MCP

```
Package: @playwright/mcp (or @executeautomation/playwright-mcp-server)
Tools: navigate, click, fill, screenshot, get_text

Use cases with Claude:
  "Navigate to localhost:3000/dashboard and take a screenshot"
  "Test the login flow: navigate to /login, fill email and password, click submit"
  "Verify the API documentation page renders all expected endpoints"

WARNING: This controls a REAL browser.
  - Only use with localhost or test environments
  - Never automate login to real production accounts
  - Never navigate to URLs containing real credentials
```

### Database MCP

```
Available: @modelcontextprotocol/server-sqlite (and others)
Tools: execute_query, list_tables, describe_table

Use cases with Claude:
  "Query the development database to show all users created in the last 7 days"
  "Describe the schema of the orders table"

CRITICAL SECURITY RULES:
  - Never connect to production databases via MCP
  - Use read-only credentials for the development DB
  - Never allow INSERT/UPDATE/DELETE through MCP (use read-only user)
```

---

## 4. Using MCP Tools in Claude Code

### How Claude Invokes MCP Tools

```
When Claude has MCP configured, you can reference the tools in prompts:

"Using the GitHub MCP server, find all open PRs in my-org/my-repo
that have been open for more than 7 days with no review activity."

"Use the filesystem MCP tool to read the architecture docs in
/Users/me/projects/design-docs/architecture/ and summarize the key decisions."

Claude will invoke the MCP tool, receive the response, and incorporate it
into its answer — no copy-paste required.
```

### Verifying MCP Tools Are Available

```
Run in Claude Code: "What MCP tools do you have access to? List them."

If Claude says it has no tools or doesn't know about mcp:
  Check 1: Is .claude/mcp.json at the right location?
  Check 2: Is the JSON syntax valid? (cat .claude/mcp.json | python3 -m json.tool)
  Check 3: Is the package installed? (npx -y @modelcontextprotocol/server-github --help)
  Check 4: Is the environment variable set? (echo $GITHUB_TOKEN)
  Check 5: Restart the Claude Code session (tools load at startup)
```

---

## 5. Debugging MCP Issues

```
Problem: Tool not appearing
  1. Check mcp.json location: .claude/mcp.json (not config/mcp.json)
  2. Validate JSON: cat .claude/mcp.json | python3 -m json.tool
  3. Check env vars: echo $GITHUB_TOKEN
  4. Check package: npx -y @modelcontextprotocol/server-github 2>&1 | head -5
  5. Restart Claude Code session

Problem: "Permission denied" when tool runs
  → Your token doesn't have the required scope. Check the token's permissions.

Problem: Filesystem tool can't read a file
  → The path isn't within the allowed directory in the args list.

Problem: GitHub rate limit errors
  → Fine-grained PAT may have a lower rate limit than classic PAT.
  → Use classic PAT (read-only scopes) for heavy read workloads.
```

---

## 6. Least Privilege Checklist

```
Before committing mcp.json or sharing your setup:

[ ] No hardcoded tokens anywhere in the file
[ ] All credentials use ${ENV_VAR} substitution
[ ] mcp.json or mcp.example.json committed with placeholders only
[ ] Real mcp.json with tokens is in .gitignore (if token values could leak)
[ ] GitHub token is fine-grained: specific repos + read-only scopes
[ ] Filesystem path is scoped to project directory, not home directory
[ ] Playwright MCP not pointing at production URLs
[ ] DB MCP using a read-only credential, not admin/root
[ ] Unused servers removed from config
```

---

## 7. Revision Checklist

- [ ] Can explain what MCP is and why it matters for Claude Code
- [ ] Can create `.claude/mcp.json` with GitHub and filesystem servers
- [ ] Knows the security rule: never hardcode tokens, use env vars
- [ ] Can diagnose "MCP tools not appearing" with the 5-step checklist
- [ ] Knows the use cases for GitHub, filesystem, Playwright, and DB MCP servers
- [ ] Has least-privilege checklist memorized for all MCP configurations
