# Optional — M365 Copilot, Agent Builder & Copilot Studio — Gold Sheet

> **Track**: Copilot Mastery Track — Group 3: Advanced Engineering
> **File**: Gap Fill (Track File #20a)
> **Audience**: Developers who also have Microsoft 365 or enterprise Copilot access
> **Status**: OPTIONAL — requires separate M365 or enterprise licensing. Skip if not applicable.

---

## 1. The Three Optional Copilot Products

```
This sheet covers three products distinct from GitHub Copilot.
None of these require a GitHub Copilot subscription.
All require separate Microsoft licensing or specific enterprise access.

Check availability:
  M365 Copilot:         requires Microsoft 365 E3/E5 + Copilot add-on
  Copilot Chat (work):  available at copilot.microsoft.com with work account
  Copilot Agent Builder: available in M365 Copilot admin center
  Copilot Studio:       requires Power Platform license
```

---

## 2. Microsoft 365 Copilot — Personal Productivity Workflows

### What It Is (vs GitHub Copilot)

```
GitHub Copilot:         AI for coding — VS Code, terminal, GitHub
Microsoft 365 Copilot: AI for office work — Word, Excel, Outlook, Teams, SharePoint

They are COMPLETELY separate products with separate subscriptions.
GitHub Copilot helps you write code.
M365 Copilot helps you write documents, emails, meeting summaries, and data analysis.

Key point for developers:
  M365 Copilot can help with the NON-CODING parts of your work:
  - Writing spec documents
  - Summarizing long Confluence/wiki pages
  - Drafting emails about technical decisions
  - Summarizing meeting notes
  - Creating project status reports
```

### Daily Developer Workflows with M365 Copilot

#### Writing Technical Specifications

```
In Word or Copilot Pages:
  Prompt: "Write a technical specification for this feature:
  [paste your bullet points or rough notes]
  
  Include:
  - Background and problem statement
  - Proposed solution with key design decisions
  - Out of scope
  - Success criteria
  - Risks and open questions
  
  Target audience: software engineers and one product manager.
  Length: 2-3 pages."
```

#### Meeting Preparation

```
Before a technical design meeting:
  "I'm presenting this architecture decision to a group of senior engineers.
  Prepare 5 likely questions they will ask about this design:
  [paste your design summary]
  
  For each question, provide:
  - The question itself
  - The best answer based on the design
  - What to say if challenged on this decision"
```

#### Summarizing Long Documents

```
In M365 Copilot Chat (copilot.microsoft.com):
  "Summarize this document in 5 bullet points:
  [paste the content or use /file to reference a SharePoint file]
  
  Focus on: decisions made, open questions, and action items."

Referencing files (if available):
  Type / to see files you can reference
  Or: use "Add content" to attach a document
```

#### Status Report Generation

```
"Generate a weekly status report from these notes:
[paste your raw notes about what you worked on]

Format:
## Week of [date]
### Completed
### In Progress
### Blocked / At Risk
### Next Week

Keep it factual and under 200 words.
Target audience: engineering manager."
```

### M365 Copilot Limitations (Be Aware)

```
- Does NOT have access to your code repository
- Does NOT know the specifics of your GitHub projects
- Knowledge cutoff: depends on your company's data residency settings
- Cannot browse the internet in work mode (by default)
- Quality depends heavily on what files/data Copilot can access in your tenant

Best used for: documentation, communication, meetings, planning
NOT for: code generation, debugging, PR review (use GitHub Copilot instead)
```

---

## 3. Copilot Chat at copilot.microsoft.com

### Work Mode vs Web Mode

```
Work mode:
  - Searches your org's data (SharePoint, Teams, email)
  - Copilot can reference your company's internal documents
  - Useful for: "Summarize recent discussions about [project]"

Web mode:
  - Searches the public internet (like Bing)
  - No access to your company's internal data
  - Useful for: general research, public documentation, news

Switch: Toggle "Work" / "Web" in the interface (availability varies by license).
```

### Developer-Useful Prompt Patterns

```
Research and summarize:
  "Explain the trade-offs between event sourcing and CQRS for a high-write system.
  Cite specific scenarios where each approach is preferred."

Decision memos:
  "Write a one-page decision memo for [technical choice].
  Include: options considered, recommendation, trade-offs, and next steps."

Learning accelerator:
  "Create a structured learning plan for [topic] for a software engineer
  with [background]. Include: key concepts, resources, hands-on exercises.
  Timeframe: 2 weeks of 1 hour per day."
```

---

## 4. Copilot Chat Agent Builder — OPTIONAL

### What It Is

```
Availability: Requires Microsoft 365 Copilot license + admin enablement.
Location: Copilot.microsoft.com → "Create an agent" or via M365 admin center.

Copilot Chat Agent Builder creates CUSTOM AI AGENTS that appear in the
Microsoft 365 Copilot chat interface (not GitHub Copilot).

These are different from GitHub Copilot custom agents (.agent.md files):
  GitHub Copilot agents (.agent.md): appear in VS Code Copilot Chat
  M365 Copilot agents (Agent Builder): appear in copilot.microsoft.com chat

Use Agent Builder when:
  - You want a specialized assistant in the M365 environment
  - The assistant needs to search your company's SharePoint/Teams data
  - You want to share the agent with non-technical colleagues
  - The use case is productivity/communication, not coding
```

### Agent Builder Concepts

```
What you configure:
  1. Name and description
  2. Instructions (what the agent does, tone, limitations)
  3. Knowledge sources (which SharePoint sites, files it can search)
  4. Actions (if available: forms it can fill, systems it can call)
  5. Starter prompts (example prompts shown to users)

What you cannot configure without Power Platform:
  - Custom connectors to external APIs
  - Process automation
  - Complex multi-step workflows

Agent Builder template for a developer productivity agent:
  Name: "Dev Standup Helper"
  Instructions: "You help software engineers prepare their daily standup.
  Ask: What did you complete yesterday? What are you working on today?
  Any blockers? Summarize in the standard standup format."
  Knowledge: [your team's Sprint board SharePoint page if available]
  Starter prompts:
    - "Help me prepare my standup"
    - "Summarize what I should say about the current sprint"
```

---

## 5. Copilot Studio — Overview

### What It Is and When to Use It

```
Copilot Studio ≠ GitHub Copilot ≠ M365 Copilot.

Copilot Studio is a low-code platform to build ENTERPRISE AGENTS
that can be integrated into:
  - Microsoft Teams
  - Company websites
  - Internal IT helpdesks
  - Business process automation flows

When to use Copilot Studio (not GitHub or M365 Copilot):
  - Building an agent for NON-DEVELOPER end users
  - The agent needs to connect to enterprise systems (SAP, Dynamics, ServiceNow)
  - You need governance, admin controls, and usage analytics
  - You need to deploy an agent to an entire organization
  - The agent replaces a helpdesk or FAQ bot

When NOT to use Copilot Studio:
  - For coding assistance (use GitHub Copilot)
  - For personal productivity (use M365 Copilot)
  - For quick experiments (Agent Builder is simpler)
  - When you don't have Power Platform licensing

Copilot Studio requires:
  - Power Platform / Dataverse license
  - IT admin deployment for production use
  - Testing in a sandbox environment before org-wide rollout
```

### Copilot Studio vs GitHub Copilot Custom Agents

| Dimension | GitHub Copilot `.agent.md` | Copilot Studio Agent |
|---|---|---|
| Platform | VS Code | Teams, web, apps |
| Audience | Individual developer | Whole org / business users |
| Configuration | Markdown file in repo | Low-code drag-and-drop |
| Connections | Local tools + MCP | Enterprise connectors (SAP, D365, etc.) |
| Governance | None (personal) | Admin controls, analytics, audit |
| Setup time | 5 minutes | Days to weeks |
| Use case | Coding assistance | Business process automation |
| Cost | GitHub Copilot plan | Power Platform license (separate) |

---

## 6. Responsible Use for Optional Products

```
For ALL Microsoft Copilot products, the same responsible use principles apply:

Do NOT share:
  - Customer PII in any prompt (M365 Copilot can search your org data — don't paste customer data)
  - Production credentials or secrets
  - Confidential competitive information
  - Financial data that should be restricted

Organization-specific rules:
  Your company may have policies on:
  - Which AI tools are approved for use
  - What data categories can be used with AI
  - Data residency requirements for your industry
  Check with your IT/security team before using M365 Copilot for work that involves
  regulated data (HIPAA, GDPR, PCI-DSS, SOC 2 scope).

Review all outputs:
  M365 Copilot summarizes and generates — it can get facts wrong, miss nuance,
  or produce summaries that misrepresent the source material.
  Never share a Copilot-generated summary without reviewing it first.
```

---

## 7. Revision Checklist

- [ ] Understands the difference between GitHub Copilot, M365 Copilot, Copilot Studio, and Agent Builder
- [ ] Knows which product applies to coding vs productivity vs enterprise automation
- [ ] Has 3 developer-useful M365 Copilot prompt patterns (spec writing, meeting prep, status reports)
- [ ] Knows Agent Builder is for M365 chat agents (not VS Code agents)
- [ ] Knows Copilot Studio requires Power Platform licensing and is for org-wide enterprise bots
- [ ] Applies responsible use rules to all Copilot products (not just GitHub Copilot)
