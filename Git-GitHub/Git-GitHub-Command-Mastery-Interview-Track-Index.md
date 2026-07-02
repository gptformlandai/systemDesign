# Git and GitHub Command Mastery Interview Track Index

> Goal: become confident working inside real repositories using Git and GitHub commands: daily workflow, branching, PRs, conflicts, recovery, history inspection, debugging, and production scenarios.

---

## How To Use This Track

This track is command-first.

Every topic answers:

```text
What do I type?
Why do I type it?
What does it change?
What can go wrong?
How do I recover?
What would I say in an interview?
```

The core mental model:

```text
working tree
-> staging area
-> local commit history
-> remote repository
-> pull request
-> protected main branch
```

---

## Study Order

| Order | Document | Why It Exists |
|---|---|---|
| 1 | [Git GitHub Setup, Authentication, and Identity](00-Setup/Git-GitHub-Setup-Authentication-Identity-Gold-Sheet.md) | install/config, SSH vs HTTPS, credential helpers, tokens, SSO, signing, multiple accounts |
| 2 | [Local Git Foundations and Daily Commands](01-Foundations/Git-Local-Foundations-Daily-Commands-Gold-Sheet.md) | `config`, `init`, `clone`, `status`, `add`, `commit`, `diff`, `log`, `show`, `restore` |
| 3 | [Branching, Merging, Rebasing, and Conflicts](01-Foundations/Git-Branching-Merging-Rebasing-Conflicts-Gold-Sheet.md) | `branch`, `switch`, `merge`, `rebase`, conflict resolution, `cherry-pick`, `revert` |
| 4 | [Remote Collaboration and GitHub Flow](01-Foundations/Git-Remote-Collaboration-GitHub-Flow-Gold-Sheet.md) | `remote`, `fetch`, `pull`, `push`, upstreams, forks, PR workflow, non-fast-forward fixes |
| 5 | [Undo, Recovery, and Safety Commands](02-Recovery-And-Inspection/Git-Undo-Recovery-Safety-Commands-Gold-Sheet.md) | `restore`, `reset`, `revert`, `reflog`, `stash`, `clean`, `commit --amend`, `force-with-lease` |
| 6 | [Inspection, Debugging, and History Pro Commands](02-Recovery-And-Inspection/Git-Inspection-Debugging-History-Pro-Commands-Gold-Sheet.md) | `log`, `show`, `diff`, `blame`, `bisect`, `grep`, `tag`, `describe`, `range-diff` |
| 7 | [GitHub CLI Command Mastery](04-Internals-Enterprise-CLI/GitHub-CLI-Command-Mastery-Gold-Sheet.md) | `gh auth`, `gh repo`, `gh pr`, `gh issue`, `gh release`, checks, reviews, merge commands |
| 8 | [GitHub Pull Request Review Excellence and Stacked Changes](04-Internals-Enterprise-CLI/GitHub-PR-Review-Stacked-Changes-Gold-Sheet.md) | reviewable PRs, merge methods, re-review, stacked PRs, local PR checkout, risk-based review |
| 9 | [Advanced Repository Workflows](03-Advanced-Workflows/Git-Advanced-Repository-Workflows-Gold-Sheet.md) | `worktree`, `submodule`, `sparse-checkout`, hooks, `.gitignore`, attributes, LFS, aliases |
| 10 | [Git Monorepo Performance and Maintenance](03-Advanced-Workflows/Git-Monorepo-Performance-Maintenance-Gold-Sheet.md) | partial clone, sparse checkout, LFS policy, commit-graph, fsmonitor, scalar, maintenance, repo diagnostics |
| 11 | [Git Patch, Email, and Open Source Contribution](03-Advanced-Workflows/Git-Patch-Email-Open-Source-Contribution-Gold-Sheet.md) | `format-patch`, `git am`, `git apply`, `send-email`, DCO sign-off, patch series review |
| 12 | [Git Internals, Object Model, Refs, and Packfiles](04-Internals-Enterprise-CLI/Git-Internals-Object-Model-Refs-Packfiles-Gold-Sheet.md) | Senior internals: objects, refs, HEAD, index, DAG, merge/rebase mechanics, reflog, packfiles, GC |
| 13 | [Git History Rewrite and Repository Cleanup](03-Advanced-Workflows/Git-History-Rewrite-Repository-Cleanup-Gold-Sheet.md) | local rewrite vs shared rewrite, secret cleanup, large file purge, `git filter-repo`, bundles, coordination |
| 14 | [GitHub Repository Lifecycle and Administration](04-Internals-Enterprise-CLI/GitHub-Repository-Lifecycle-Administration-Gold-Sheet.md) | templates, repo creation, issue/PR templates, labels, ownership, transfers, archive/delete, health checks |
| 15 | [GitHub Enterprise Governance, Security, and Branch Protection](04-Internals-Enterprise-CLI/GitHub-Enterprise-Governance-Security-Branch-Protection-Gold-Sheet.md) | Enterprise layer: rulesets, branch protection, CODEOWNERS, checks, permissions, tokens, secrets, audit |
| 16 | [GitHub API, Webhooks, Apps, and Automation](04-Internals-Enterprise-CLI/GitHub-API-Webhooks-Apps-Automation-Gold-Sheet.md) | REST, GraphQL, webhook signatures, GitHub Apps, checks/statuses, rate limits, idempotent automation |
| 17 | [Git Release Engineering, Versioning, Tags, and Backports](03-Advanced-Workflows/Git-Release-Engineering-Versioning-Tags-Backports-Gold-Sheet.md) | Release layer: annotated tags, semver, release branches, hotfixes, backports, changelogs, rollback, traceability |
| 18 | [Production and Interview Scenario Playbook](05-Practice-Upgrade/Git-GitHub-Production-Interview-Scenario-Playbook.md) | wrong branch, bad merge, lost commit, conflict, force push, hotfix, release, protected branch scenarios |
| 19 | [Golden Command Cheat Sheet](Git-GitHub-Golden-Command-Cheat-Sheet.md) | fast command recipes for daily use and interview revision |
| 20 | [Git-GitHub Active Recall Question Bank](05-Practice-Upgrade/Git-GitHub-Active-Recall-Question-Bank.md) | Retrieval practice across daily commands, recovery, internals, governance, and release engineering |
| 21 | [Git-GitHub Scenario Drill Bank](05-Practice-Upgrade/Git-GitHub-Scenario-Drill-Bank.md) | Production and interview scenario drills for local, remote, recovery, governance, and release workflows |
| 22 | [Git-GitHub Hands-On Labs](05-Practice-Upgrade/Git-GitHub-Hands-On-Labs.md) | Throwaway-repo labs for staging, conflicts, reset, reflog, bisect, tags, backports, CODEOWNERS, and PR flow |
| 23 | [Git-GitHub Mock Interview Scripts](05-Practice-Upgrade/Git-GitHub-Mock-Interview-Scripts.md) | Timed mock rounds from foundations through MAANG Git/GitHub operating-model capstone |
| 24 | [Git-GitHub Interview Scoring Rubrics](05-Practice-Upgrade/Git-GitHub-Interview-Scoring-Rubrics.md) | Measurable scoring for command safety, internals, governance, release, and capstone readiness |
| 25 | [Git-GitHub 2 Week 4 Week Mastery Roadmaps](05-Practice-Upgrade/Git-GitHub-2-Week-4-Week-Mastery-Roadmaps.md) | Structured fast prep and deeper mastery plans |
| 26 | [Git GitHub Production Operating Model Capstone](06-Capstone/Git-GitHub-Production-Operating-Model-Capstone.md) | End-to-end proof of mastery: setup, workflow, PRs, governance, automation, releases, monorepos, and incident recovery |

---

## Practice Upgrade Layer

Use the `05-Practice-Upgrade` folder after the command and senior concept sheets. It turns command knowledge into production reflexes.

| Practice File | Use It For |
|---|---|
| [Git-GitHub Active Recall Question Bank](05-Practice-Upgrade/Git-GitHub-Active-Recall-Question-Bank.md) | Daily recall and weak-spot detection |
| [Git-GitHub Scenario Drill Bank](05-Practice-Upgrade/Git-GitHub-Scenario-Drill-Bank.md) | Production incident and interview scenario practice |
| [Git-GitHub Hands-On Labs](05-Practice-Upgrade/Git-GitHub-Hands-On-Labs.md) | Safe command practice in throwaway repositories |
| [Git-GitHub Mock Interview Scripts](05-Practice-Upgrade/Git-GitHub-Mock-Interview-Scripts.md) | Timed spoken interview rehearsals |
| [Git-GitHub Interview Scoring Rubrics](05-Practice-Upgrade/Git-GitHub-Interview-Scoring-Rubrics.md) | Objective readiness scoring after labs and mocks |
| [Git-GitHub 2 Week 4 Week Mastery Roadmaps](05-Practice-Upgrade/Git-GitHub-2-Week-4-Week-Mastery-Roadmaps.md) | Fast and deep study plans |
| [Git GitHub Production Operating Model Capstone](06-Capstone/Git-GitHub-Production-Operating-Model-Capstone.md) | Final integrated system-design exercise for Git/GitHub operations |

Recommended loop:

```text
read one sheet -> practice commands in throwaway repo -> answer recall -> solve scenario -> speak mock answer -> score with rubric
```

Final loop:

```text
finish concept sheets -> run labs -> solve incident scenarios -> complete capstone -> repeat mock under time pressure
```

---

## GitHub Actions Bridge

This track covers Git/GitHub source-control operations and governance. The separate [GitHub Actions track](../GithubActions/GitHub-Actions-Interview-Track-Index.md) covers deep CI/CD workflow design, runners, OIDC, deployments, reusable workflows, and supply-chain security.

Boundary:

```text
Git-GitHub track -> who can change workflow files and how checks gate merges
GithubActions track -> how workflows are built, secured, deployed, observed, and scaled
```

---

## Learning Levels

### Beginner

You should become comfortable with:

- Git installation and first-time config
- SSH vs HTTPS basics
- `gh auth status`
- `git status`
- `git add`
- `git commit`
- `git diff`
- `git log`
- `git switch`
- `git pull`
- `git push`
- `git restore`

### Intermediate

You should confidently handle:

- feature branches
- merge conflicts
- rebasing local work
- stashing changes
- undoing commits safely
- pushing branches
- creating pull requests
- syncing with main
- PR templates and review comments
- basic GitHub CLI usage
- local auth/debug checks

### Pro / Interview Level

You should be able to explain and use:

- `reflog` recovery
- `reset` vs `revert`
- `merge` vs `rebase`
- `fetch` vs `pull`
- `force --force-with-lease`
- `cherry-pick`
- `bisect`
- `worktree`
- fork/upstream flow
- protected branches and PR review flow
- stacked PRs and re-review after force-push
- Git object model: blob, tree, commit, tag
- refs, HEAD, index, remote-tracking branches, and merge bases
- branch protection, rulesets, CODEOWNERS, required checks, token safety, and audit
- release tags, semantic versioning, hotfixes, backports, changelogs, and artifact traceability
- SSH/HTTPS credential helpers, SSO, commit/tag signing, and multiple-account setup
- history rewrite and repository cleanup for secrets, large files, and bad shared history
- GitHub APIs, webhooks, GitHub Apps, checks/statuses, rate limits, and idempotent automation
- repository lifecycle administration: templates, repo health, archive/transfer/delete policy
- monorepo performance: sparse checkout, partial clone, LFS, commit-graph, fsmonitor, maintenance
- patch/email/open-source workflows with `format-patch`, `git am`, and DCO sign-off

---

## Master Map

```text
Setup / identity work:
  install / version check
  user.name / user.email
  config scopes
  SSH vs HTTPS
  credential helpers
  gh auth
  SSO
  multiple accounts
  commit / tag signing

Local work:
  status
  add
  commit
  diff
  restore
  log

Branch work:
  branch
  switch
  merge
  rebase
  conflict resolution

Remote work:
  remote
  fetch
  pull
  push
  upstream tracking
  fork/upstream origin

Recovery work:
  restore
  reset
  revert
  reflog
  stash
  clean

Investigation work:
  log
  show
  diff
  blame
  bisect
  grep
  range-diff

GitHub work:
  gh auth
  gh repo
  gh pr
  gh issue
  gh release
  gh workflow
  PR templates
  local PR checkout
  stacked PRs
  review comments
  suggested changes
  merge methods

Internals work:
  objects
  refs
  HEAD
  index
  merge base
  reflog
  packfiles
  garbage collection
  commit graph
  partial clone
  fsmonitor

Governance work:
  branch protection
  rulesets
  CODEOWNERS
  required checks
  permissions
  token scopes
  secret scanning
  audit
  repo templates
  repository lifecycle
  issue / PR templates
  archive / transfer / delete
  workflow file protection

Automation work:
  REST API
  GraphQL API
  gh api
  webhooks
  webhook signatures
  GitHub Apps
  checks / statuses
  rate limits
  idempotent handlers

Large repo work:
  monorepo ownership
  sparse checkout
  partial clone
  Git LFS
  maintenance
  large file policy
  history cleanup

Open source / patch work:
  format-patch
  git apply
  git am
  send-email
  DCO sign-off
  range-diff for patch revisions

Release work:
  annotated tags
  semantic versioning
  release branches
  hotfixes
  backports
  changelogs
  GitHub releases
  rollback vs revert
```

---

## MAANG Completion Definition

This track is complete only when you can do all of the following without notes:

1. Explain every Git command by what it changes in the working tree, index, HEAD, refs, or remote.
2. Distinguish local/private history from pushed/shared/protected history before undoing or rewriting anything.
3. Recover local mistakes with reflog, restore, reset, revert, stash, and rescue branches.
4. Resolve merge and rebase conflicts and explain merge base, fast-forward, and hash changes.
5. Collaborate through GitHub Flow, forks, upstreams, PR checks, reviews, and safe branch updates.
6. Debug history with `log`, `show`, `diff`, `blame`, `bisect`, `range-diff`, and exact two-dot/three-dot ranges.
7. Explain Git internals: object database, blob/tree/commit/tag, refs, HEAD, index, DAG, packfiles, reflog, and garbage collection.
8. Use GitHub CLI for PRs, checks, reviews, workflow inspection, issues, and releases.
9. Set up and debug GitHub authentication through SSH, HTTPS, credential helpers, `gh auth`, SSO, commit email, multiple accounts, and signing.
10. Design PR workflows with focused changes, templates, review policy, stacked PRs, merge strategy, local PR checkout, and re-review after rewrites.
11. Design enterprise GitHub governance using branch protection/rulesets, CODEOWNERS, required checks, merge queue, permissions, token safety, secret scanning, workflow protection, and audit.
12. Perform history rewrite and repository cleanup safely for secrets, large files, bad commits, stale refs, and repo-size incidents.
13. Operate repository lifecycle administration with templates, issue/PR templates, ownership, health checks, visibility, transfer, archive, and deletion policy.
14. Design GitHub automation using REST, GraphQL, webhooks, GitHub Apps, checks/statuses, idempotency, rate limits, and least privilege.
15. Keep large repositories usable with sparse checkout, partial clone, Git LFS, commit-graph, fsmonitor, maintenance, and monorepo ownership.
16. Contribute through patch/email/open-source workflows using `format-patch`, `git am`, `git apply`, `send-email`, DCO sign-off, and revision notes.
17. Run release engineering workflows with annotated/signed tags, semantic versioning, release branches, hotfixes, backports, changelogs, GitHub Releases, and artifact traceability.
18. Deliver a full Git/GitHub operating-model capstone and score at least 4/5 on the rubric.

---

## Interview Rule

Never say "I will reset it" without saying:

- local or pushed?
- shared branch or private branch?
- do we want to preserve history?
- should we use `revert` instead?
- do we need `--force-with-lease`?

Safe wording:

> If the commit is already pushed to a shared branch, I prefer `git revert` because it preserves history. If it is only local, I can use `git reset` depending on whether I want to keep changes staged, unstaged, or discard them.

---

## Official Source Notes

- Git command reference: <https://git-scm.com/docs>
- Git credentials docs: <https://git-scm.com/docs/gitcredentials>
- Git partial clone docs: <https://git-scm.com/docs/partial-clone>
- Git maintenance docs: <https://git-scm.com/docs/git-maintenance>
- Git commit-graph docs: <https://git-scm.com/docs/git-commit-graph>
- Git fsmonitor docs: <https://git-scm.com/docs/git-fsmonitor--daemon>
- Scalar docs: <https://git-scm.com/docs/scalar>
- Git format-patch docs: <https://git-scm.com/docs/git-format-patch>
- Git am docs: <https://git-scm.com/docs/git-am>
- Git apply docs: <https://git-scm.com/docs/git-apply>
- Git send-email docs: <https://git-scm.com/docs/git-send-email>
- Git bundle docs: <https://git-scm.com/docs/git-bundle>
- Git filter-repo project: <https://github.com/newren/git-filter-repo>
- GitHub using Git docs: <https://docs.github.com/en/get-started/using-git>
- GitHub pull request docs: <https://docs.github.com/en/pull-requests>
- GitHub SSH docs: <https://docs.github.com/en/authentication/connecting-to-github-with-ssh>
- GitHub personal access token docs: <https://docs.github.com/en/authentication/keeping-your-account-and-data-secure/managing-your-personal-access-tokens>
- GitHub commit signature verification docs: <https://docs.github.com/en/authentication/managing-commit-signature-verification>
- GitHub CODEOWNERS docs: <https://docs.github.com/en/repositories/managing-your-repositorys-settings-and-features/customizing-your-repository/about-code-owners>
- GitHub rulesets docs: <https://docs.github.com/en/repositories/configuring-branches-and-merges-in-your-repository/managing-rulesets/about-rulesets>
- GitHub merge queue docs: <https://docs.github.com/en/repositories/configuring-branches-and-merges-in-your-repository/configuring-pull-request-merges/managing-a-merge-queue>
- GitHub webhooks docs: <https://docs.github.com/en/webhooks>
- GitHub webhook signature validation docs: <https://docs.github.com/en/webhooks/using-webhooks/validating-webhook-deliveries>
- GitHub Apps docs: <https://docs.github.com/en/apps/creating-github-apps>
- GitHub REST API docs: <https://docs.github.com/en/rest>
- GitHub GraphQL API docs: <https://docs.github.com/en/graphql>
- GitHub template repository docs: <https://docs.github.com/en/repositories/creating-and-managing-repositories/creating-a-template-repository>
- GitHub issue and PR template docs: <https://docs.github.com/en/repositories/managing-your-repositorys-settings-and-features/customizing-your-repository/about-issue-and-pull-request-templates>
- GitHub LFS docs: <https://docs.github.com/en/repositories/working-with-files/managing-large-files/about-git-large-file-storage>
- GitHub Actions docs: <https://docs.github.com/en/actions>
- GitHub code security docs: <https://docs.github.com/en/code-security>
- GitHub CLI manual: <https://cli.github.com/manual/>
