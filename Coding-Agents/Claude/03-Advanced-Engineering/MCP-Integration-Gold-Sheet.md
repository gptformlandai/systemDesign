# MCP Integration — Gold Sheet

> **Track**: Claude Mastery Track — Group 3: Advanced Engineering
> **File**: 3 of 7 (Track File #17)
> **Read after**: Skills-System-Gold-Sheet.md

---

## 1. What MCP Is

### Must Know

```
MCP (Model Context Protocol) = a standard protocol for connecting AI models to external tools.

Without MCP: Claude Code can read and write files + run shell commands.
With MCP: Claude Code can call APIs, query databases, control browsers, search GitHub issues.

MCP architecture:
  Claude Code (MCP client) ↔ stdio/SSE protocol ↔ MCP Server (runs locally)
                                                           ↕
                                              External system (GitHub, DB, browser, API)

Each MCP server exposes tools with names like:
  github_list_issues, filesystem_read_file, browser_navigate, db_query
  
Claude calls these tools during a session. You see what Claude is calling.
You can approve/deny individual tool calls.
```

---

## 2. Configuration — `.claude/mcp.json`

```json
{
  "mcpServers": {
    "filesystem": {
      "command": "npx",
      "args": ["-y", "@modelcontextprotocol/server-filesystem", "/Users/yourname/projects"],
      "_note": "NEVER set path to / — scope to specific directories only"
    },
    "github": {
      "command": "npx",
      "args": ["-y", "@modelcontextprotocol/server-github"],
      "env": {
        "GITHUB_PERSONAL_ACCESS_TOKEN": "${env:GITHUB_TOKEN}"
      },
      "_security": "Never hardcode tokens. Use ${env:VAR} to read from shell environment."
    },
    "postgres": {
      "command": "npx",
      "args": ["-y", "@modelcontextprotocol/server-postgres", "${env:DATABASE_URL}"],
      "_security": "Only connect to development/local DB, never production."
    }
  }
}
```

### Security Rules (Non-Negotiable)

```
Rule 1: Never commit .claude/mcp.json with real tokens.
  Add to .gitignore: .claude/mcp.json
  Commit instead: .claude/mcp.example.json with placeholder values

Rule 2: Never hardcode credentials.
  BAD:  "GITHUB_TOKEN": "ghp_realtoken123"
  GOOD: "GITHUB_TOKEN": "${env:GITHUB_TOKEN}"
  Set the environment variable in your shell profile, not in the config file.

Rule 3: Scope filesystem MCP to minimum required directories.
  BAD:  "/Users/yourname" (your entire home directory)
  GOOD: "/Users/yourname/projects/my-specific-project"

Rule 4: Use read-only DB connections where possible.
  Postgres connection: use a read-only user for analysis tasks.
  Only use write-capable connection when Claude needs to write data.

Rule 5: Review every tool call Claude makes.
  Claude shows you what MCP tool it's about to call.
  Read the tool name and arguments before approving.
  "github_delete_repo" is very different from "github_list_issues".
```

---

## 3. Verified MCP Servers and Use Cases

### GitHub MCP

```
Install: npx -y @modelcontextprotocol/server-github
Token permissions needed (fine-grained): Contents=read, Issues=read, Pull requests=read

Use cases:
  "List all open issues in my-org/my-repo labeled 'bug' created this week"
  "Find the PR that introduced the payment refund endpoint — summarize its changes"
  "What issues reference the term 'database timeout'?"
  "List all open PRs with no reviewer assigned"
```

### Filesystem MCP

```
Install: npx -y @modelcontextprotocol/server-filesystem /path/to/allowed/directory
Security: NO path traversal beyond the specified directory

Use cases:
  - Reading files in a project that Claude Code can't directly access
  - Comparing files across two project directories
  - Finding patterns in log files
  - Searching documentation files outside the current project
```

### PostgreSQL MCP

```
Install: npx -y @modelcontextprotocol/server-postgres postgresql://user:pass@localhost/dbname
Security: ONLY local or development database. Never production.

Use cases:
  "Show me the 10 most recent orders with status='failed'"
  "What's the distribution of user signup dates by month?"
  "Count rows where payment_method is null"
  "Find all users who have never placed an order"
```

### Playwright/Browser MCP

```
Install: npx -y @executeautomation/playwright-mcp-server
Security: ONLY local or test environments. Never automate production user accounts.

Use cases:
  "Navigate to http://localhost:3000/login and verify the form renders correctly"
  "Click the 'Submit' button and capture the network request"
  "Take a screenshot of the dashboard after login and describe what you see"
  "Verify the error message text when submitting an empty form"
```

---

## 4. Using MCP in Claude Code Sessions

```bash
# Verify MCP servers are connected
claude "List what MCP tools you have available"
# Expected: Claude lists tool names from each configured server

# GitHub MCP workflow example
claude "Using the GitHub MCP tools:
1. List open issues labeled 'bug' in my-org/order-service
2. For each issue: check if there's a related PR by searching the PR title for the issue number
3. Report: issues with no PR (need work) vs issues with open PR (in progress)
Output: two tables — Issues Needing Work | Issues With Open PRs"

# Database analysis workflow
claude "Using the postgres MCP:
Run this analysis (read-only):
1. Count of orders by status for the past 30 days
2. Average order value by payment method
3. Users with > 3 failed payments in 30 days (anonymize — show user_id only, not email)
Format: 3 separate tables"

# Filesystem + code analysis
claude "Using the filesystem MCP to read @file:/path/to/logs/error.log:
Find all unique error types in the past 24 hours.
Group by: error type, count, first occurrence, last occurrence.
Format: table"
```

---

## 5. MCP Safety Checklist

```
Before starting any MCP session:
[ ] .claude/mcp.json is NOT committed (gitignored)
[ ] All tokens use ${env:VAR} not hardcoded values
[ ] Filesystem MCP scoped to minimum required directory
[ ] Database MCP connected to local/dev, not production
[ ] Browser MCP only on localhost or test environment

During MCP session:
[ ] Read every tool call name and arguments before approving
[ ] "delete", "create", "modify" tool calls require extra scrutiny
[ ] Database write operations: verify the WHERE clause before approving

After MCP session:
[ ] Review what Claude actually read/queried (Claude Code shows a log)
[ ] Rotate any token that was used in a session exposed to sensitive data
```

---

## 6. Revision Checklist

- [ ] Understands MCP client (Claude) → server → external system architecture
- [ ] Has .claude/mcp.example.json committed; .claude/mcp.json gitignored
- [ ] Never hardcodes tokens in mcp.json (uses ${env:VAR})
- [ ] Knows GitHub, Filesystem, Postgres, and Browser MCP server use cases
- [ ] Reads tool name + arguments before approving every MCP tool call
- [ ] Only connects Postgres MCP to local/dev database
- [ ] Scopes Filesystem MCP to minimum required directories
