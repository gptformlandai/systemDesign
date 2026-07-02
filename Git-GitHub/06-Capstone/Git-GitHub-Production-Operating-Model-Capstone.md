# Git GitHub Production Operating Model Capstone

> Goal: prove beginner-to-pro mastery by designing the Git/GitHub operating model for a real engineering organization: developer setup, daily workflow, PRs, governance, releases, automation, security, monorepos, and incident recovery.

---

## 1. Capstone Scenario

You join a company with 120 engineers and 80 repositories.

Problems:

- main breaks often
- releases are hard to trace
- direct pushes happen
- PRs are huge and inconsistent
- secrets were committed twice
- some repos have no owner
- GitHub Actions workflows are edited without security review
- monorepo teams complain about slow clone/status
- automation uses broad personal access tokens
- release tags have been moved silently
- new hires struggle with SSH, SSO, and multiple GitHub accounts

Your task:

> Design a Git/GitHub operating model that is safe, fast, auditable, and teachable.

---

## 2. Requirements

### Developer Experience

- simple setup guide
- SSH/HTTPS auth guidance
- work vs personal account separation
- commit identity and signing policy
- clear branch workflow
- good PR templates
- local recovery playbooks

### Collaboration

- protected main
- required checks
- CODEOWNERS
- review policy
- merge method policy
- stacked PR guidance
- conflict/update guidance

### Security And Governance

- least privilege
- team-based access
- token policy
- GitHub App automation
- secret scanning and push protection
- signed commits/tags where needed
- workflow file protection
- audit/bypass policy

### Release Engineering

- semantic versioning
- annotated/signed tags
- GitHub Releases
- changelogs
- hotfix/backport process
- rollback vs revert policy
- artifact traceability

### Operations

- repo lifecycle administration
- template repositories
- repo health checks
- monorepo performance strategy
- history rewrite runbook
- GitHub API/webhook automation
- Actions handoff to the dedicated GitHub Actions track

---

## 3. Target Architecture

```text
developer machine
  -> setup/auth/signing baseline
  -> feature branch
  -> focused commits
  -> PR template
  -> CODEOWNERS review
  -> required checks / merge queue
  -> protected main
  -> release tag / GitHub Release
  -> deployment pipeline
  -> audit and observability
```

Automation layer:

```text
GitHub App
  -> webhooks
  -> repo health checks
  -> PR labeling / policy checks
  -> CODEOWNERS/ruleset verification
  -> external check runs
```

---

## 4. Developer Setup Standard

Baseline:

- Git and GitHub CLI installed
- `user.name` and `user.email` configured
- `init.defaultBranch=main`
- SSH or HTTPS credential helper configured
- `gh auth status` verified
- SSO authorized if enterprise org requires it
- signing configured for high-risk repos
- work/personal account separation documented

Setup verification:

```bash
git --version
gh --version
git config --list --show-origin
git remote -v
gh auth status
ssh -T git@github.com
```

Rule:

> Developer setup problems are identity/auth/access problems until proven otherwise.

---

## 5. Branch And PR Model

Default:

```text
main is protected
feature branches are short-lived
all production changes go through PR
required checks must pass
CODEOWNERS required for owned paths
no direct push to main
no force push to protected branches
```

Feature flow:

```bash
git fetch origin
git switch -c feature/order-timeout origin/main
git add .
git commit -m "Fix order timeout handling"
git push -u origin feature/order-timeout
gh pr create
```

PR expectations:

- small enough to review
- tests listed
- risk and rollback noted
- generated files separated
- security-sensitive changes called out
- stacked PRs used for large dependent work

---

## 6. Main Branch Stability

Controls:

- branch protection or rulesets
- required CI checks
- CODEOWNERS
- conversation resolution
- stale approval dismissal when needed
- merge queue for high-throughput repos
- restricted bypass list
- audit bypasses

Merge method policy:

| Repo Type | Default |
|---|---|
| application service | squash or rebase merge based on team preference |
| release-heavy library | preserve useful commit history |
| infrastructure | stricter review, signed commits/tags if required |
| high-volume monorepo | merge queue plus path-aware checks |

---

## 7. Recovery And Incident Playbooks

Required playbooks:

- wrong branch edits
- staged too much
- bad local commit
- bad pushed commit
- lost commit recovery with reflog
- non-fast-forward push rejected
- merge/rebase conflict
- accidental force push
- secret committed
- large file committed
- wrong release tag

Rules:

- inspect first
- identify local vs pushed
- identify private vs shared
- preserve recovery path
- prefer `revert` for shared branch rollback
- rotate secrets before history cleanup

---

## 8. Repository Lifecycle Governance

Every repo must have:

- owner team
- description
- visibility classification
- README
- CODEOWNERS
- PR template
- branch/ruleset policy
- security settings
- service catalog metadata
- release status
- archived/deprecated status if inactive

Quarterly repo health check:

```text
owner exists
main protected
checks valid
CODEOWNERS valid
admins reviewed
secrets scanning enabled
workflow paths protected
old branches reviewed
Apps/tokens/deploy keys audited
```

---

## 9. Automation Model

Use GitHub Apps for durable automation:

- PR labeling
- reviewer assignment
- repo health checks
- external policy checks
- service catalog sync
- release notifications

Webhook rules:

- validate signatures
- store delivery IDs
- process idempotently
- use queues for long work
- handle retries/rate limits
- log safely

Avoid:

- broad human PATs
- bots with admin access everywhere
- scripts with no dry-run mode
- mutating repo settings without audit

---

## 10. Security Model

Security controls:

- SSO/2FA
- least privilege
- team-based access
- branch protection/rulesets
- CODEOWNERS for sensitive paths
- secret scanning and push protection
- signed release tags
- token rotation process
- GitHub App over broad PAT
- workflow file review
- dependency/code scanning where applicable

Sensitive paths:

```text
/.github/workflows/
/terraform/
/k8s/
/security/
/payments/
/auth/
```

---

## 11. Release Model

Release flow:

```text
main -> release candidate -> annotated/signed tag -> GitHub Release -> artifact -> deployment -> hotfix/backport if needed
```

Rules:

- do not silently move consumed release tags
- use annotated tags for important releases
- use signed tags for high-trust releases
- generate changelog from tag ranges
- use `cherry-pick -x` for audited backports
- protect release branches
- document rollback vs revert

---

## 12. Monorepo Strategy

If the company has a large monorepo:

- use CODEOWNERS by path
- use path-aware CI
- evaluate sparse checkout and partial clone
- use LFS or artifact storage for large binaries
- enable maintenance strategy
- consider merge queue
- track repo health metrics
- document full checkout vs service checkout

Monorepo interview answer:

> I optimize both Git performance and organizational ownership. Sparse checkout without CODEOWNERS and affected CI only solves part of the problem.

---

## 13. GitHub Actions Handoff

This Git/GitHub track governs workflow files and required checks.

The dedicated `GithubActions` track owns deep CI/CD design:

- workflow syntax
- runners
- OIDC
- deployment environments
- reusable workflows
- supply-chain security
- observability and cost

Boundary:

```text
Git-GitHub track -> who can change workflows and how they gate merges
GithubActions track -> how workflows are designed, secured, deployed, and operated
```

---

## 14. Capstone Interview Prompt

> Design a Git/GitHub operating model for a 120-engineer company with many repos, one large monorepo, frequent production releases, strict security requirements, and a history of main-branch breakages and secret leaks.

---

## 15. Strong Answer Structure

1. Start with goals: safety, speed, auditability, developer experience.
2. Standardize setup/auth/signing.
3. Define branch and PR workflow.
4. Enforce main protection, CODEOWNERS, checks, and merge queue where needed.
5. Define repo lifecycle ownership and templates.
6. Define recovery playbooks for common Git incidents.
7. Define release model with tags, changelogs, hotfixes, backports.
8. Define security model: least privilege, tokens, Apps, secret scanning, workflow protection.
9. Define automation using GitHub Apps, APIs, webhooks, checks.
10. Define monorepo performance and ownership strategy.
11. Explain trade-offs and rollout plan.

---

## 16. Rollout Plan

Phase 1: baseline

- publish setup guide
- define PR template
- enable protected main on critical repos
- assign repo owners
- rotate broad tokens

Phase 2: governance

- CODEOWNERS
- rulesets
- required checks
- workflow path protection
- secret scanning/push protection

Phase 3: automation

- GitHub App for repo health
- PR labeling/policy checks
- service catalog sync
- dashboards

Phase 4: maturity

- release governance
- merge queue for busy repos
- monorepo performance tooling
- capstone drills and incident game days

---

## 17. Scoring Rubric

| Area | 1 | 3 | 5 |
|---|---|---|---|
| Git fundamentals | command list only | explains working tree/index/HEAD | explains commands by state change and recovery |
| Collaboration | vague PR flow | protected main and reviews | clear PR, CODEOWNERS, merge queue, stacked changes |
| Recovery | risky reset/force | local vs pushed distinction | full incident playbooks with reflog/revert/lease |
| Security | mentions tokens | least privilege and secret scanning | Apps, SSO, signing, audit, workflow protection |
| Release | tags vaguely | semver and release branches | signed tags, backports, traceability, tag policy |
| Automation | scripts with PAT | gh/api basics | Apps, webhooks, checks, rate limits, idempotency |
| Monorepo | says sparse checkout | path ownership/CI | sparse/partial/LFS/maintenance/governance |
| Communication | scattered | structured | trade-off driven and rollout-aware |

---

## 18. Final Checklist

You are capstone-ready when you can answer:

- What does each Git command change?
- When do you use reset vs revert?
- How do you recover a lost commit?
- How do you safely force-push a private branch?
- How do you protect main?
- How do CODEOWNERS and rulesets work together?
- How do you handle a committed secret?
- How do you clean a large file from history?
- How do you choose a merge method?
- How do you design stacked PRs?
- How do GitHub Apps differ from PATs?
- How do webhooks fail?
- How do you govern workflow files?
- How do you release with traceable tags?
- How do you keep a monorepo usable?

---

## 19. Revision Notes

- One-line summary: A production Git/GitHub operating model combines command safety, collaboration policy, security governance, automation, and release discipline.
- Three keywords: safety, ownership, audit.
- One interview trap: solving everything with branch protection while ignoring setup, recovery, tokens, releases, and repo lifecycle.
- One memory trick: "Setup, branch, review, protect, release, recover."

---

## 20. Official Source Notes

- Git command reference: <https://git-scm.com/docs>
- GitHub Docs: <https://docs.github.com/en>
- GitHub pull request docs: <https://docs.github.com/en/pull-requests>
- GitHub rulesets docs: <https://docs.github.com/en/repositories/configuring-branches-and-merges-in-your-repository/managing-rulesets/about-rulesets>
- GitHub CODEOWNERS docs: <https://docs.github.com/en/repositories/managing-your-repositorys-settings-and-features/customizing-your-repository/about-code-owners>
- GitHub webhooks docs: <https://docs.github.com/en/webhooks>
- GitHub Apps docs: <https://docs.github.com/en/apps/creating-github-apps>
- GitHub CLI manual: <https://cli.github.com/manual/>
