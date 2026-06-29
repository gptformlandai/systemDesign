# Claude Chat — Fundamentals — Gold Sheet

> **Track**: Claude Mastery Track — Group 1: Foundations
> **File**: 3 of 6 (Track File #3)
> **Audience**: Developers learning Claude Chat for daily productivity
> **Read after**: Claude-Setup-Personal-Machine-Gold-Sheet.md

---

## 1. Practical Impact Meter

| Topic | Impact | Why Developers Miss This |
|---|---|---|
| Projects vs conversations — persistent context | ★★★★★ | Default conversations have no memory; Projects fix this |
| File upload and analysis | ★★★★★ | Claude can read your code files directly — no copy-pasting |
| Web search and real-time information | ★★★★☆ | Claude has a knowledge cutoff — web search gets current info |
| Artifacts — code that Claude builds | ★★★★☆ | Artifacts are live, editable — not just static text |
| Conversation structure — when to start fresh | ★★★★★ | Long conversations accumulate stale context; fresh = cleaner output |
| Claude.ai vs API — when to use each | ★★★★☆ | API gives programmatic access; chat is interactive |

---

## 2. Projects — Persistent Context Across Conversations

### Must Know

```
Default Claude conversations: no memory between sessions.
Each new conversation: blank slate.

Projects solve this:
  - A Project has "Project Instructions" (your persistent prompt)
  - Every conversation within the project automatically loads those instructions
  - Uploaded files in the project are available across all conversations
  - Project instructions = CLAUDE.md equivalent for Claude Chat

Create effective project instructions:
  - Keep under 300 words
  - Include: tech stack, coding conventions, preferred libraries
  - Include: "Do NOT" rules for your most common annoyances
  - Include: output format preferences

Example project for a Python developer:
  "I'm a Python backend developer using FastAPI and PostgreSQL.
  - Always suggest pytest for tests, never unittest
  - HTTP client: httpx, not requests
  - Use async/await patterns throughout
  - Prefer concrete code over abstract advice
  - Code first, explanation after
  - Under 200 words for answers unless I ask for depth"
```

### Project Organization Strategy

```
Create separate Projects for separate concerns:

Project: "FastAPI Backend"
  Instructions: FastAPI + PostgreSQL + async patterns
  Files: architecture docs, schema files, key service files

Project: "React Frontend"
  Instructions: React + TypeScript + Vitest
  Files: component patterns, design system docs

Project: "Learning — Python Advanced"
  Instructions: detailed explanations, show internals, use analogies
  Files: none (learning context doesn't need code files)

Project: "Job / Interview Prep"
  Instructions: explain concepts clearly, show tradeoffs, interview-focused
  Files: study notes, practice problems

Rule: one Project per domain. Don't mix concerns in one Project.
```

---

## 3. File Upload and Analysis

### What You Can Upload

```
Code files: .py, .ts, .js, .java, .go, .rs (any text file)
Documents: .pdf, .txt, .md
Images: screenshots, diagrams (Claude has vision)
Data: .csv, .json (for analysis)

Limit: varies by plan (typically 10-20 files per conversation)
Max file size: varies (typically 10-30MB per file)

What Claude does with uploaded files:
  - Reads the full content into context
  - Can reference specific sections when asked
  - Can compare across multiple uploaded files
  - Can generate code that builds on the uploaded files
```

### File Upload Best Practices

```
Upload relevant files, not everything:
  Good: upload UserService.py when asking about user logic
  Bad: upload 30 files and ask "what's wrong?"

Ask specific questions about uploaded files:
  "In the uploaded UserService.py, why does create_user()
  call validate_email() twice?"

Compare uploaded files:
  "Compare the error handling patterns in these two uploaded files.
  Which is more robust? Show as a table."

Use uploads for review:
  Upload a file → "Security review. Check: injection, auth, PII in logs.
  Format: [SEVERITY] — [line] — [issue] — [fix]"
```

---

## 4. Artifacts

### What Artifacts Are

```
When Claude generates code in Claude.ai, it sometimes creates an "Artifact" —
a live code panel on the right side of the interface.

Artifacts support:
  - React components (renders live in browser)
  - HTML/CSS (renders live)
  - Code in other languages (syntax highlighted, copyable)
  - SVG diagrams (renders)
  - Markdown documents

What makes Artifacts useful:
  - Code is EDITABLE inside the artifact panel
  - You can ask Claude to modify the artifact by referencing it
  - For React: you see the component rendered, not just the code
  - Artifacts persist in the conversation — you can iterate
```

### Using Artifacts Effectively

```
Request artifacts for:
  "Create a React component for a user profile card.
  Use Tailwind CSS. Show the rendered preview."
  → Claude creates an Artifact — you see the rendered card immediately

Iterate on artifacts:
  "Make the avatar larger and add a blue border"
  → Claude modifies the Artifact — preview updates

Export artifacts:
  Copy the code from the Artifact panel into your project

Note: Artifacts are a Claude Chat feature, not Claude Code.
For multi-file projects: use Claude Code CLI.
```

---

## 5. Web Search

```
Claude can search the web for current information (if enabled in settings).

When to use web search:
  - Recent library releases (Claude's training has a cutoff)
  - Current security vulnerabilities
  - Latest API documentation
  - Recent blog posts or announcements

Enable web search:
  Claude.ai → Settings → Enable web search (if available)

Trigger web search explicitly:
  "Search the web for the current stable version of FastAPI and its breaking changes
  from the last 6 months."

Don't use web search for:
  - Concepts in well-established libraries (Claude already knows)
  - Code generation (web search adds latency, rarely helps for coding)
  - Anything in Claude's training data (faster without search)
```

---

## 6. Conversation Management

### When to Start a New Conversation

```
Start NEW conversation when:
  - Topic changes completely (switching from auth to billing)
  - Previous answers were poor and you want fresh context
  - Conversation is very long (context may be saturated)
  - You finished a task — don't carry it into the next task

Continue SAME conversation when:
  - Iterating on the same piece of code
  - Follow-up questions directly related to current topic
  - Multi-step problem where context from earlier is still relevant

Signs of context saturation:
  - Claude forgets what you said earlier in the conversation
  - Responses become more generic
  - Claude contradicts itself from earlier in the conversation
  → Start a new conversation and provide a brief summary of prior context
```

### Conversation Naming for Future Reference

```
Name conversations descriptively for easy retrieval:
  Good: "FastAPI user auth JWT flow debug"
  Bad: "New conversation"

Access old conversations:
  Sidebar → search by name or scroll history

Note: In Projects, all conversations share the Project Instructions
      but each conversation is independent otherwise.
```

---

## 7. Claude.ai vs API — When to Use Each

```
Claude.ai Chat:
  Use when: interactive conversation, learning, quick Q&A, file analysis
  Access: browser or Desktop app
  Auth: your account
  Cost: subscription plan

Anthropic API (via code):
  Use when: building applications that use Claude, automating workflows,
            processing many requests programmatically
  Access: ANTHROPIC_API_KEY in your code
  Auth: API key
  Cost: per-token pricing

Claude Code CLI:
  Use when: agentic coding, multi-file projects, autonomous workflows
  Access: terminal
  Auth: Claude.ai subscription or API key
  Cost: depends on auth method

Decision matrix:
  Learning / Q&A      → Claude.ai Chat
  Coding assistant    → Claude Code CLI
  Building an app     → Anthropic API
  Quick file analysis → Claude.ai Chat with file upload
  Autonomous feature  → Claude Code CLI
```

---

## 8. Revision Checklist

- [ ] Has at least one Claude.ai Project with custom instructions
- [ ] Knows how to upload files and ask targeted questions about them
- [ ] Understands what Artifacts are and when to use them
- [ ] Knows when to start a new conversation vs continue the same one
- [ ] Knows when to use Claude Chat vs Claude Code CLI vs API
- [ ] Can detect context saturation symptoms
- [ ] Names conversations descriptively for future reference
