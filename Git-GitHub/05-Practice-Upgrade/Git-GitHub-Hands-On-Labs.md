# Git GitHub Hands On Labs

> Track: Git and GitHub Command Mastery - Practice Upgrade  
> Goal: build safe command muscle memory through small repeatable labs.

Use a throwaway local repository for command practice.

---

## 1. Lab Rules

For every lab, write:

1. Goal.
2. Starting state.
3. Commands used.
4. What changed in working tree, index, refs, or remote.
5. Verification command.
6. Recovery command if something goes wrong.
7. 60-second interview explanation.

Safety rule:

```text
Never practice destructive commands in a real work repo unless you know exactly what they change.
```

---

## 2. Lab 1: Build The Three-State Model

Practice:

- create a file
- inspect `git status`
- stage it
- inspect staged diff
- commit it
- modify it again
- compare working tree vs staged diff

Commands:

```bash
git status
git add README.md
git diff
git diff --staged
git commit -m "Add README"
```

Outcome:

```text
Explain working tree, index, and HEAD with one example.
```

---

## 3. Lab 2: Partial Staging

Practice:

- create two logical changes in one file
- stage only one hunk
- commit it
- leave the second change unstaged

Commands:

```bash
git add -p <file>
git diff --staged
git commit -m "Commit one logical change"
```

Outcome:

```text
Explain why partial commits improve review quality.
```

---

## 4. Lab 3: Branch And Fast-Forward

Practice:

- create feature branch
- commit once
- switch main
- merge feature
- observe fast-forward behavior

Commands:

```bash
git switch -c feature/hello
# edit and commit
git switch main
git merge feature/hello
```

Outcome:

```text
Explain why no merge commit was needed.
```

---

## 5. Lab 4: Merge Conflict

Practice:

- create two branches
- edit same line differently
- merge and resolve conflict
- inspect conflict markers

Commands:

```bash
git merge feature/conflict
git status
git add <file>
git merge --continue
```

Outcome:

```text
Explain merge base and conflict resolution.
```

---

## 6. Lab 5: Rebase Conflict

Practice:

- create divergent branch
- rebase feature onto main
- resolve conflict
- continue rebase

Commands:

```bash
git rebase main
git status
git add <file>
git rebase --continue
```

Outcome:

```text
Explain why rebased commits get new hashes.
```

---

## 7. Lab 6: Reset Modes

Practice in a throwaway repo:

- create a commit
- try `reset --soft HEAD~1`
- recommit
- try `reset --mixed HEAD~1`
- recommit
- try `reset --hard HEAD~1`

Record changes to:

- branch pointer
- index
- working tree

Outcome:

```text
Explain soft, mixed, and hard reset safely.
```

---

## 8. Lab 7: Reflog Recovery

Practice:

- create two commits
- hard reset back one commit
- recover old commit from reflog

Commands:

```bash
git reflog
git branch rescue/lost <sha>
git log --oneline --decorate --all
```

Outcome:

```text
Explain why reflog can recover local commits but is not a shared backup.
```

---

## 9. Lab 8: Stash vs Worktree

Practice:

- create dirty feature work
- use stash to switch context
- repeat using worktree for hotfix

Commands:

```bash
git stash push -u -m "WIP feature"
git stash list
git stash pop

git worktree add -b hotfix/demo ../repo-hotfix main
git worktree list
git worktree remove ../repo-hotfix
git worktree prune
```

Outcome:

```text
Explain when worktree is safer than stash.
```

---

## 10. Lab 9: Remote Branch Simulation

Practice with a bare repo or second clone:

- create remote
- push branch with upstream
- make remote change from second clone
- see non-fast-forward rejection
- fetch and integrate

Commands:

```bash
git push -u origin HEAD
git fetch origin
git log --oneline HEAD..origin/<branch>
git pull --rebase
```

Outcome:

```text
Explain non-fast-forward push rejection.
```

---

## 11. Lab 10: Force With Lease

Practice only in throwaway remote:

- push branch
- rebase local branch
- push with `--force-with-lease`
- simulate remote branch moving before your push
- observe protection

Outcome:

```text
Explain why `--force-with-lease` checks remote expectation before overwriting.
```

---

## 12. Lab 11: Bisect A Bug

Practice:

- create a tiny script
- make several commits
- introduce a bug in one commit
- use `git bisect` with manual or automated test

Commands:

```bash
git bisect start
git bisect bad
git bisect good <old-good-sha>
# test each checked out commit
git bisect reset
```

Outcome:

```text
Explain how binary search applies to commit history.
```

---

## 13. Lab 12: Range-Diff After Rebase

Practice:

- create PR-style branch with 2 commits
- save old head SHA
- rebase/edit commits
- compare old and new branch versions

Command:

```bash
git range-diff main...<old-sha> main...HEAD
```

Outcome:

```text
Explain why range-diff is useful for PR review after history rewrite.
```

---

## 14. Lab 13: Git Object Inspection

Practice:

```bash
git cat-file -t HEAD
git cat-file -p HEAD
git ls-tree HEAD
git rev-parse HEAD
git show --stat HEAD
```

Outcome:

```text
Explain commit object, tree object, blob object, and refs.
```

---

## 15. Lab 14: Tags And Release Notes

Practice:

- create annotated tag
- compare two tags
- generate shortlog
- create draft GitHub release if connected to GitHub

Commands:

```bash
git tag -a v0.1.0 -m "Release v0.1.0"
git log --oneline v0.0.0..v0.1.0
git diff --stat v0.0.0..v0.1.0
git shortlog -sne v0.0.0..v0.1.0
```

Outcome:

```text
Explain how a release maps to a tag and commit SHA.
```

---

## 16. Lab 15: Backport With Cherry-Pick

Practice:

- create `main` and `release/1.0`
- fix bug on main
- cherry-pick fix onto release branch

Commands:

```bash
git switch -c backport/fix release/1.0
git cherry-pick -x <fix-sha>
```

Outcome:

```text
Explain why backport should be minimal and audited.
```

---

## 17. Lab 16: CODEOWNERS Simulation

Practice:

- create `.github/CODEOWNERS`
- map paths to pretend teams
- explain which owners should review changes

Example:

```text
/services/payments/ @payments-team
/.github/workflows/ @platform-security
/terraform/ @infra-team
```

Outcome:

```text
Explain why CODEOWNERS needs branch protection to enforce owner review.
```

---

## 18. Lab 17: Branch Protection Design

Design a protected-main policy for a team:

- required PR
- 2 approvals
- CODEOWNERS review
- required tests
- stale approval dismissal
- no force push
- merge queue if high traffic

Outcome:

```text
Explain policy choices and trade-offs.
```

---

## 19. Lab 18: Secret Incident Runbook

Write a runbook for a committed secret:

1. Revoke/rotate.
2. Remove current usage.
3. Audit exposure.
4. Decide history cleanup plan.
5. Enable prevention.
6. Communicate incident.

Outcome:

```text
Explain why history rewrite alone is not enough.
```

---

## 20. Lab 19: Full PR Flow With GitHub CLI

Practice:

```bash
git switch -c feature/demo
git push -u origin HEAD
gh pr create --fill
gh pr checks
gh pr view --web
gh pr merge --squash
```

Outcome:

```text
Explain which actions are Git operations and which are GitHub operations.
```

---

## 21. Lab 20: Enterprise Workflow Capstone

Design a workflow for:

```text
100 engineers, protected main, monorepo, required CI, CODEOWNERS, release branches, hotfixes, security scanning, and audit.
```

Deliverable:

- branch model
- PR policy
- merge strategy
- release tagging
- hotfix/backport workflow
- ownership model
- security controls
- incident recovery plan

---

## 22. Completion Gate

You completed the labs when you can:

1. Explain each command by state change.
2. Recover from local history mistakes.
3. Safely update PR branches.
4. Debug history with bisect, blame, range-diff, and reflog.
5. Inspect Git objects and refs.
6. Design release and governance workflows.
