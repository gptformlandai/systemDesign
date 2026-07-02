# GitHub API, Webhooks, Apps, and Automation Gold Sheet

> Goal: understand GitHub as an automation platform: REST, GraphQL, webhooks, GitHub Apps, checks, statuses, rate limits, permissions, and safe automation design.

---

## 1. Intuition

GitHub UI is the dashboard.

GitHub APIs and webhooks are the control plane.

```text
REST/GraphQL API -> ask or change GitHub
webhook          -> GitHub tells your system something happened
GitHub App      -> durable automation identity with scoped permissions
Checks/statuses -> automation reports quality gates back to PRs
```

Senior mental model:

> Use APIs to query or mutate state, webhooks to react to events, and GitHub Apps to authenticate automation with least privilege.

---

## 2. Definition

- Definition: GitHub automation uses APIs, webhooks, Apps, tokens, and checks/statuses to integrate repositories with external systems and internal developer platforms.
- Category: platform engineering / DevEx / CI/CD integration.
- Core idea: automate GitHub with scoped identities, auditable events, idempotent handlers, and explicit failure handling.

---

## 3. Why It Exists

Manual GitHub work does not scale when:

- hundreds of repos need consistent settings
- PR checks come from external systems
- deploy systems need approval/status feedback
- incident systems need to react to releases
- bots need to label, assign, or triage issues
- internal portals need repository metadata
- security tools need to open alerts/PRs
- monorepos need affected-build orchestration

Automation prevents toil, but unsafe automation can bypass guardrails.

---

## 4. API Choices

| Interface | Best For | Notes |
|---|---|---|
| GitHub REST API | concrete resource operations | straightforward endpoints |
| GitHub GraphQL API | fetching related data efficiently | flexible queries, schema-driven |
| GitHub CLI `gh api` | quick scripts and admin queries | token scopes matter |
| Webhooks | event-driven integration | must verify signatures and retry safely |
| GitHub App | durable automation identity | preferred over broad human PATs |
| Actions `GITHUB_TOKEN` | workflow-local automation | permissions should be minimal |

REST example:

```bash
gh api repos/OWNER/REPO
```

GraphQL example:

```bash
gh api graphql -f query='
{
  repository(owner: "OWNER", name: "REPO") {
    pullRequests(first: 5, states: OPEN) {
      nodes {
        number
        title
      }
    }
  }
}'
```

---

## 5. GitHub Apps vs PATs

| Identity | Use Case | Risk |
|---|---|---|
| classic PAT | legacy scripts | broad, user-tied, hard to govern |
| fine-grained PAT | user-scoped automation | still tied to a human |
| GitHub App | durable product/platform automation | setup complexity |
| Actions `GITHUB_TOKEN` | workflow job automation | repository/workflow-scoped |
| deploy key | repo clone/deploy | one repo, usually Git-only |

Preferred senior answer:

> For long-lived automation, I prefer GitHub Apps because permissions, installations, ownership, rotation, and audit are cleaner than a human PAT.

GitHub App concepts:

- app registration
- private key
- installation
- installation access token
- repository permissions
- organization permissions
- webhook events

---

## 6. Webhooks

Webhook flow:

```text
GitHub event
-> HTTPS POST to webhook endpoint
-> validate signature
-> persist delivery/event ID
-> enqueue work
-> return quickly
-> process idempotently
-> retry or dead-letter failures
```

Common events:

- `pull_request`
- `push`
- `check_suite`
- `check_run`
- `workflow_run`
- `release`
- `issues`
- `issue_comment`
- `repository`
- `membership`
- `organization`

Webhook rules:

- validate signature
- use HTTPS
- make handlers idempotent
- do not do long work in request thread
- handle duplicate deliveries
- log delivery IDs
- store minimal payload if sensitive
- define retry and dead-letter behavior

---

## 7. Validating Webhook Signatures

Concept:

```text
payload + shared secret -> HMAC signature
compare expected signature with GitHub header
```

Python sketch:

```python
import hmac
import hashlib


def is_valid_signature(secret: bytes, payload: bytes, header: str) -> bool:
    expected = "sha256=" + hmac.new(secret, payload, hashlib.sha256).hexdigest()
    return hmac.compare_digest(expected, header)
```

Do not:

- skip signature verification
- log full payloads containing secrets
- trust event sender without checking
- let webhook endpoint mutate production without authorization checks

---

## 8. Checks API vs Commit Statuses

| Mechanism | Use |
|---|---|
| Commit statuses | simple pass/fail contexts on commits |
| Checks API | richer annotations, summaries, actions, and line-level feedback |
| Required checks/rulesets | governance that blocks merge until expected checks pass |

Use cases:

- external CI reports build result
- security scanner annotates code
- policy engine marks PR as blocked
- deployment system reports environment readiness
- monorepo build orchestrator reports affected-service checks

Important:

> A check is useful only if branch protection/rulesets require the right check names and prevent bypass.

---

## 9. Automation Design Patterns

### PR Labeler

```text
pull_request webhook
-> inspect changed files
-> apply labels
-> request CODEOWNERS or team review
```

### External CI Reporter

```text
push webhook
-> build in external CI
-> create check run
-> update check conclusion
-> required check gates merge
```

### Release Notifier

```text
release published
-> notify Slack/incident/release portal
-> record artifact version
-> link deployment run
```

### Repository Governance Bot

```text
scheduled job
-> query org repos
-> detect missing branch protection/CODEOWNERS/templates
-> open issue or PR
```

### Internal Developer Portal Sync

```text
repo/topic/team metadata
-> GraphQL query
-> service catalog update
-> ownership/dashboard sync
```

---

## 10. Rate Limits And Pagination

Pro automation must handle:

- pagination
- rate limits
- secondary rate limits
- retries with backoff
- conditional requests where appropriate
- partial failures
- API version changes
- permission errors

Script shape:

```text
query page
-> process page
-> persist cursor/checkpoint
-> sleep/backoff on limit
-> resume after failure
```

Avoid:

- one giant org-wide API loop with no checkpoint
- retry storms
- token with too much access
- assuming all repositories have the same feature set

---

## 11. Security Model

Automation checklist:

1. What identity runs this?
2. Which repositories can it access?
3. Which permissions does it need?
4. Can it write to protected branches?
5. Can it trigger workflows?
6. Can it read secrets or deployment environments?
7. Are webhook signatures verified?
8. Are payloads logged safely?
9. Are tokens rotated?
10. Are app installations audited?

Threats:

- leaked app private key
- over-scoped PAT
- unverified webhook spoofing
- command injection from PR title/body/branch name
- bot bypassing branch rules
- workflow file mutation with privileged token

---

## 12. Failure Modes

### Duplicate Webhook Delivery

Impact:

- duplicate labels/comments/deployments

Mitigation:

- use delivery ID and event ID idempotency
- check existing state before writing

### Missing Webhook Delivery

Impact:

- bot does not react

Mitigation:

- periodic reconciliation job
- event replay if platform supports
- dashboard for delivery failures

### API Permission Error

Impact:

- automation fails on some repos

Mitigation:

- inspect installation permissions
- handle 403/404 carefully
- request only needed scopes

### Rate Limit Hit

Impact:

- automation stalls or fails

Mitigation:

- pagination, backoff, caching, GraphQL batching, checkpointing

### Unsafe Mutation

Impact:

- bot changes repo settings or PRs incorrectly

Mitigation:

- dry-run mode
- allowlist repos
- change review
- audit logging
- staged rollout

---

## 13. Practical Question

> Your company wants a bot that labels PRs, requests reviewers, blocks risky changes, and reports external policy checks. How would you design it?

---

## 14. Strong Answer

I would build it as a GitHub App, not a broad personal token. The App would subscribe to `pull_request` and possibly `push` events, validate webhook signatures, persist delivery IDs for idempotency, and enqueue work instead of processing everything in the webhook request.

For each PR, it would inspect changed files through REST or GraphQL, apply labels, request reviews based on ownership rules, and publish check runs for policy results. Required checks and branch rulesets would enforce merge blocking. I would keep permissions minimal, roll out repo-by-repo, add dry-run mode, handle pagination/rate limits, and run a reconciliation job in case webhooks are missed.

---

## 15. Revision Notes

- One-line summary: GitHub automation needs scoped identity, verified events, idempotent handlers, and governed checks.
- Three keywords: App, webhook, check.
- One interview trap: using a broad human PAT for long-lived org automation.
- One memory trick: "API asks, webhook tells, App acts."

---

## 16. Official Source Notes

- GitHub REST API docs: <https://docs.github.com/en/rest>
- GitHub GraphQL API docs: <https://docs.github.com/en/graphql>
- GitHub webhooks docs: <https://docs.github.com/en/webhooks>
- GitHub webhook signature validation docs: <https://docs.github.com/en/webhooks/using-webhooks/validating-webhook-deliveries>
- GitHub Apps docs: <https://docs.github.com/en/apps/creating-github-apps>
- GitHub CLI manual: <https://cli.github.com/manual/>
