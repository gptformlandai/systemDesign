# Git GitHub Scenario Drill Bank

> Track: Git and GitHub Command Mastery - Practice Upgrade  
> Goal: practice Git/GitHub production and interview scenarios until the safe response is automatic.

---

## 1. Answer Format

For every scenario, answer in this shape:

```text
inspect first -> identify shared/local risk -> choose command -> explain state change -> verify -> recovery/prevention
```

Safety questions:

1. Is this local only or already pushed?
2. Is the branch shared/protected?
3. Do we need to preserve audit history?
4. Are there uncommitted changes?
5. What command can verify before changing anything?

---

## 2. Local Workflow Scenarios

### Scenario 1: Staged Too Much

Prompt:

```text
You staged five files but only two belong in the commit.
```

Expected answer:

- inspect with `git status` and `git diff --staged`
- unstage selected files with `git restore --staged <file>`
- commit only intended files
- verify staged diff before commit

---

### Scenario 2: Edited Wrong Branch

Prompt:

```text
You made uncommitted changes on main but they belong on a feature branch.
```

Expected answer:

- if clean enough, create/switch branch with changes carried
- or stash, switch, apply stash
- avoid committing on main

---

### Scenario 3: Last Commit Message Is Wrong

Prompt:

```text
Your last local commit message is wrong and not pushed.
```

Expected answer:

```bash
git commit --amend
```

Boundary:

```text
Safe if local/private. If already pushed/shared, coordinate before rewriting.
```

---

### Scenario 4: Need Partial Commit

Prompt:

```text
A file has two unrelated changes. Commit only one.
```

Expected answer:

```bash
git add -p <file>
git diff --staged
git commit
```

---

## 3. Branching And Conflict Scenarios

### Scenario 5: Merge Conflict During Pull

Prompt:

```text
`git pull` caused conflicts.
```

Expected answer:

- inspect status
- identify merge vs rebase state
- resolve files
- run tests
- continue merge/rebase or abort if needed

---

### Scenario 6: Rebase Conflict

Prompt:

```text
You are rebasing feature on main and hit a conflict.
```

Expected answer:

- resolve current commit conflict
- `git add <files>`
- `git rebase --continue`
- abort with `git rebase --abort` if wrong path

---

### Scenario 7: PR Has Conflicts With Main

Prompt:

```text
Your PR cannot merge because main changed.
```

Expected answer:

- fetch latest main
- merge or rebase based on team policy
- resolve conflicts locally
- push updated branch
- run checks

---

### Scenario 8: Need To Split Big Commit

Prompt:

```text
One local commit contains refactor, bug fix, and formatting.
```

Expected answer:

- reset mixed to parent if local/private
- stage logical pieces
- create separate commits
- verify each diff

---

## 4. Remote Collaboration Scenarios

### Scenario 9: Non-Fast-Forward Push Rejected

Prompt:

```text
Push rejected because remote has work you do not have.
```

Expected answer:

- fetch
- inspect remote changes
- integrate with merge/rebase
- rerun tests
- push
- do not force push blindly

---

### Scenario 10: Need To Update Fork

Prompt:

```text
Your fork is behind upstream main.
```

Expected answer:

- ensure upstream remote exists
- fetch upstream
- update local main
- push fork main
- update feature branch if needed

---

### Scenario 11: Need To Review PR Locally

Prompt:

```text
You need to run a teammate's PR locally.
```

Expected answer:

```bash
gh pr checkout <number>
git status
# run tests
```

Alternative:

```bash
git fetch origin pull/<id>/head:pr-<id>
```

---

### Scenario 12: Force Push Needed After Rebase

Prompt:

```text
You rebased your private PR branch and need to update remote.
```

Expected answer:

```bash
git push --force-with-lease
```

Boundary:

```text
Use only for owned/private branch or after coordination. Never blindly force push shared branches.
```

---

## 5. Recovery Scenarios

### Scenario 13: Lost Local Commit

Prompt:

```text
You reset and lost a local commit.
```

Expected answer:

```bash
git reflog
git branch rescue/lost <sha>
```

Explain:

- reset moved branch pointer
- reflog records local pointer movement
- create branch before cleanup

---

### Scenario 14: Accidentally Hard Reset

Prompt:

```text
You ran `git reset --hard` and lost uncommitted changes.
```

Expected answer:

- committed changes may be recoverable through reflog
- uncommitted changes are much harder and may be gone
- check IDE/local history if available
- prevention: stash/commit before destructive commands

---

### Scenario 15: Accidentally Force Pushed

Prompt:

```text
A force push removed teammates' commits from a shared branch.
```

Expected answer:

- stop more pushes
- identify previous remote state from teammate clone/reflog/CI
- restore branch pointer carefully
- communicate impact
- protect branch and require `--force-with-lease` or disallow force pushes

---

### Scenario 16: Secret Committed

Prompt:

```text
An API key was committed and pushed.
```

Expected answer:

- rotate/revoke secret immediately
- remove from current code
- audit usage
- coordinate history cleanup if needed
- enable secret scanning/push protection

---

## 6. Inspection Debugging Scenarios

### Scenario 17: Find Bug-Introducing Commit

Prompt:

```text
A bug appeared sometime last week.
```

Expected answer:

- identify known good and bad commits
- use `git bisect`
- run deterministic test
- confirm culprit after bisect

---

### Scenario 18: Compare PR Before/After Rebase

Prompt:

```text
A PR was rebased and you need to know whether content changed.
```

Expected answer:

```bash
git range-diff origin/main...old-head origin/main...new-head
```

---

### Scenario 19: Inspect What Will Be Pushed

Prompt:

```text
Before pushing, verify exactly what commits and diff will go up.
```

Expected answer:

```bash
git status
git log --oneline @{u}..HEAD
git diff @{u}...HEAD
```

---

## 7. GitHub Governance Scenarios

### Scenario 20: Main Was Directly Pushed

Prompt:

```text
Someone pushed directly to main and bypassed PR review.
```

Expected answer:

- identify actor and commits
- inspect diff and CI status
- revert/fix if needed
- audit branch protection/ruleset bypass
- tighten permissions

---

### Scenario 21: CODEOWNERS Did Not Trigger

Prompt:

```text
A sensitive workflow file changed without platform review.
```

Expected answer:

- check CODEOWNERS syntax/path order
- check team write access
- check branch rule requires CODEOWNER review
- protect `.github/workflows`

---

### Scenario 22: Required Checks Are Flaky

Prompt:

```text
Required CI checks are flaky and blocking merges.
```

Expected answer:

- do not simply remove protection
- quarantine/fix flaky check
- split blocking vs informational checks if needed
- track reliability

---

### Scenario 23: Over-Permitted Automation Token

Prompt:

```text
A bot PAT with admin repo scope is used in CI.
```

Expected answer:

- rotate token
- replace with GitHub App or scoped token
- reduce workflow permissions
- audit use

---

## 8. Release Engineering Scenarios

### Scenario 24: Wrong Tag Pushed

Prompt:

```text
Release tag v2.0.0 points to the wrong commit.
```

Expected answer:

- check whether consumed by builds/deployments
- avoid silently moving public release tag
- prefer corrected patch tag when consumed
- coordinate if retagging is unavoidable

---

### Scenario 25: Backport Critical Fix

Prompt:

```text
Main has the fix, but production runs release/1.8.
```

Expected answer:

```bash
git switch -c backport/1.8-fix origin/release/1.8
git cherry-pick -x <fix-sha>
```

Then PR to release branch, test, tag patch release, ensure main remains fixed.

---

### Scenario 26: Need To Know What Shipped

Prompt:

```text
Incident asks: what code is running in production?
```

Expected answer:

- deployment record -> artifact -> build run -> commit SHA -> tag -> PRs
- `git show <tag>`
- `gh release view <tag>`
- compare tag ranges

---

## 9. Capstone Scenarios

### Scenario 27: Design Git/GitHub Workflow For 100 Engineers

Must include:

- branch strategy
- PR review policy
- branch protection/rulesets
- required checks
- CODEOWNERS
- merge strategy
- release tagging
- hotfix/backport process
- permissions and audit

---

### Scenario 28: Monorepo Workflow

Must include:

- CODEOWNERS by path
- required checks by affected area
- sparse checkout or partial workflows when useful
- merge queue if main stability is hard
- release ownership
- large file policy

---

### Scenario 29: Production Git Incident

Prompt:

```text
Bad release, wrong tag, direct push to main, and secret exposure all happen in one week. Build a prevention plan.
```

Must include:

- incident triage
- rollback/revert/rotate
- branch/tag protection
- CODEOWNERS
- release traceability
- secret scanning/push protection
- audit and training

---

## 10. Completion Gate

You are ready when you can solve:

1. 5 local/branching scenarios.
2. 5 recovery scenarios.
3. 4 remote/PR scenarios.
4. 4 inspection/debugging scenarios.
5. 4 governance/security scenarios.
6. 4 release/hotfix/backport scenarios.
7. 1 full enterprise workflow capstone.
