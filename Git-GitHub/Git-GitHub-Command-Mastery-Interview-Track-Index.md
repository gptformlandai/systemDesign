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
| 1 | [Local Git Foundations and Daily Commands](01-Foundations/Git-Local-Foundations-Daily-Commands-Gold-Sheet.md) | `config`, `init`, `clone`, `status`, `add`, `commit`, `diff`, `log`, `show`, `restore` |
| 2 | [Branching, Merging, Rebasing, and Conflicts](01-Foundations/Git-Branching-Merging-Rebasing-Conflicts-Gold-Sheet.md) | `branch`, `switch`, `merge`, `rebase`, conflict resolution, `cherry-pick`, `revert` |
| 3 | [Remote Collaboration and GitHub Flow](01-Foundations/Git-Remote-Collaboration-GitHub-Flow-Gold-Sheet.md) | `remote`, `fetch`, `pull`, `push`, upstreams, forks, PR workflow, non-fast-forward fixes |
| 4 | [Undo, Recovery, and Safety Commands](02-Recovery-And-Inspection/Git-Undo-Recovery-Safety-Commands-Gold-Sheet.md) | `restore`, `reset`, `revert`, `reflog`, `stash`, `clean`, `commit --amend`, `force-with-lease` |
| 5 | [Inspection, Debugging, and History Pro Commands](02-Recovery-And-Inspection/Git-Inspection-Debugging-History-Pro-Commands-Gold-Sheet.md) | `log`, `show`, `diff`, `blame`, `bisect`, `grep`, `tag`, `describe`, `range-diff` |
| 6 | [GitHub CLI Command Mastery](04-Internals-Enterprise-CLI/GitHub-CLI-Command-Mastery-Gold-Sheet.md) | `gh auth`, `gh repo`, `gh pr`, `gh issue`, `gh release`, checks, reviews, merge commands |
| 7 | [Advanced Repository Workflows](03-Advanced-Workflows/Git-Advanced-Repository-Workflows-Gold-Sheet.md) | `worktree`, `submodule`, `sparse-checkout`, hooks, `.gitignore`, attributes, LFS, aliases |
| 8 | [Git Internals, Object Model, Refs, and Packfiles](04-Internals-Enterprise-CLI/Git-Internals-Object-Model-Refs-Packfiles-Gold-Sheet.md) | Senior internals: objects, refs, HEAD, index, DAG, merge/rebase mechanics, reflog, packfiles, GC |
| 9 | [GitHub Enterprise Governance, Security, and Branch Protection](04-Internals-Enterprise-CLI/GitHub-Enterprise-Governance-Security-Branch-Protection-Gold-Sheet.md) | Enterprise layer: rulesets, branch protection, CODEOWNERS, checks, permissions, tokens, secrets, audit |
| 10 | [Git Release Engineering, Versioning, Tags, and Backports](03-Advanced-Workflows/Git-Release-Engineering-Versioning-Tags-Backports-Gold-Sheet.md) | Release layer: annotated tags, semver, release branches, hotfixes, backports, changelogs, rollback, traceability |
| 11 | [Production and Interview Scenario Playbook](05-Practice-Upgrade/Git-GitHub-Production-Interview-Scenario-Playbook.md) | wrong branch, bad merge, lost commit, conflict, force push, hotfix, release, protected branch scenarios |
| 12 | [Golden Command Cheat Sheet](Git-GitHub-Golden-Command-Cheat-Sheet.md) | fast command recipes for daily use and interview revision |
| 13 | [Git-GitHub Active Recall Question Bank](05-Practice-Upgrade/Git-GitHub-Active-Recall-Question-Bank.md) | Retrieval practice across daily commands, recovery, internals, governance, and release engineering |
| 14 | [Git-GitHub Scenario Drill Bank](05-Practice-Upgrade/Git-GitHub-Scenario-Drill-Bank.md) | Production and interview scenario drills for local, remote, recovery, governance, and release workflows |
| 15 | [Git-GitHub Hands-On Labs](05-Practice-Upgrade/Git-GitHub-Hands-On-Labs.md) | Throwaway-repo labs for staging, conflicts, reset, reflog, bisect, tags, backports, CODEOWNERS, and PR flow |
| 16 | [Git-GitHub Mock Interview Scripts](05-Practice-Upgrade/Git-GitHub-Mock-Interview-Scripts.md) | Timed mock rounds from foundations through MAANG Git/GitHub operating-model capstone |
| 17 | [Git-GitHub Interview Scoring Rubrics](05-Practice-Upgrade/Git-GitHub-Interview-Scoring-Rubrics.md) | Measurable scoring for command safety, internals, governance, release, and capstone readiness |
| 18 | [Git-GitHub 2 Week 4 Week Mastery Roadmaps](05-Practice-Upgrade/Git-GitHub-2-Week-4-Week-Mastery-Roadmaps.md) | Structured fast prep and deeper mastery plans |

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

Recommended loop:

```text
read one sheet -> practice commands in throwaway repo -> answer recall -> solve scenario -> speak mock answer -> score with rubric
```

---

## Learning Levels

### Beginner

You should become comfortable with:

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
- Git object model: blob, tree, commit, tag
- refs, HEAD, index, remote-tracking branches, and merge bases
- branch protection, rulesets, CODEOWNERS, required checks, token safety, and audit
- release tags, semantic versioning, hotfixes, backports, changelogs, and artifact traceability

---

## Master Map

```text
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

Internals work:
  objects
  refs
  HEAD
  index
  merge base
  reflog
  packfiles
  garbage collection

Governance work:
  branch protection
  rulesets
  CODEOWNERS
  required checks
  permissions
  token scopes
  secret scanning
  audit

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
9. Design enterprise GitHub governance using branch protection/rulesets, CODEOWNERS, required checks, permissions, token safety, secret scanning, and audit.
10. Run release engineering workflows with annotated tags, semantic versioning, release branches, hotfixes, backports, changelogs, GitHub Releases, and artifact traceability.
11. Deliver a full Git/GitHub operating-model capstone and score at least 4/5 on the rubric.

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
- GitHub using Git docs: <https://docs.github.com/en/get-started/using-git>
- GitHub pull request docs: <https://docs.github.com/en/pull-requests>
- GitHub CLI manual: <https://cli.github.com/manual/>

