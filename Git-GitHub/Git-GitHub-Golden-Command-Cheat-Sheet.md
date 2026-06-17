# Git and GitHub Golden Command Cheat Sheet

> Goal: one fast command sheet for daily repo work, production recovery, PR workflow, and interview revision.

---

## 1. First Commands In Any Repo

```bash
git status -sb
git branch --show-current
git log --oneline --decorate -5
git remote -v
```

Meaning:

```text
Where am I?
What branch am I on?
What changed recently?
Which remote am I connected to?
```

---

## 2. Configure Git

```bash
git config --global user.name "Your Name"
git config --global user.email "you@example.com"
git config --global init.defaultBranch main
git config --global pull.ff only
git config --global core.editor "code --wait"
```

List config:

```bash
git config --list --show-origin
```

---

## 3. Start Work

Clone:

```bash
git clone <repo-url>
cd <repo>
```

Clone with GitHub CLI:

```bash
gh repo clone owner/repo
cd repo
```

Create branch from updated main:

```bash
git fetch origin
git switch main
git pull --ff-only
git switch -c feature/my-work
```

Create branch directly from remote main:

```bash
git fetch origin
git switch -c feature/my-work origin/main
```

---

## 4. Daily Commit Flow

```bash
git status -sb
git diff
git add -p
git diff --staged
git commit -m "Add order validation"
```

Push first time:

```bash
git push -u origin HEAD
```

Push after upstream exists:

```bash
git push
```

---

## 5. Branch Commands

List branches:

```bash
git branch
git branch -a
```

Create branch:

```bash
git switch -c feature/name
```

Switch branch:

```bash
git switch main
```

Rename current branch:

```bash
git branch -m new-branch-name
```

Delete merged local branch:

```bash
git branch -d feature/name
```

Force delete local branch:

```bash
git branch -D feature/name
```

Delete remote branch:

```bash
git push origin --delete feature/name
```

---

## 6. Diff Commands

Unstaged changes:

```bash
git diff
```

Staged changes:

```bash
git diff --staged
```

PR diff against main:

```bash
git fetch origin
git diff origin/main...HEAD
```

Commits in branch not in main:

```bash
git log --oneline origin/main..HEAD
```

Check whitespace problems:

```bash
git diff --check
```

---

## 7. Log And Inspection

Compact log:

```bash
git log --oneline --decorate -10
```

Graph log:

```bash
git log --oneline --decorate --graph --all -30
```

Show commit:

```bash
git show <commit>
```

Show file history:

```bash
git log --follow -- <file>
```

Find text in history:

```bash
git log -S "functionName" -p
```

Blame lines:

```bash
git blame -L 50,100 <file>
```

Search tracked files:

```bash
git grep -n "OrderStatus"
```

---

## 8. Stage, Unstage, Restore

Stage all:

```bash
git add .
```

Stage interactively:

```bash
git add -p
```

Unstage file:

```bash
git restore --staged <file>
```

Discard local file changes:

```bash
git restore <file>
```

Restore file from main:

```bash
git restore --source=origin/main -- <file>
```

---

## 9. Undo Local Commits

Undo last commit, keep changes staged:

```bash
git reset --soft HEAD~1
```

Undo last commit, keep changes unstaged:

```bash
git reset --mixed HEAD~1
```

Undo last commit, discard changes:

```bash
git reset --hard HEAD~1
```

Amend message:

```bash
git commit --amend -m "Better message"
```

Add missed file to last commit:

```bash
git add <file>
git commit --amend --no-edit
```

---

## 10. Undo Pushed Commits

Revert commit:

```bash
git revert <commit>
git push
```

Revert merge commit:

```bash
git revert -m 1 <merge-commit>
git push
```

Rule:

```text
Use revert for shared history.
Use reset only for local/private history.
```

---

## 11. Recovery

Find lost commits:

```bash
git reflog
```

Recover into new branch:

```bash
git switch -c rescue <sha>
```

Recover current branch to SHA:

```bash
git reset --hard <sha>
```

Safer habit:

```text
Create a rescue branch first. Reset later if needed.
```

---

## 12. Stash

Stash tracked files:

```bash
git stash push -m "wip before pull"
```

Stash including untracked:

```bash
git stash push -u -m "wip with new files"
```

List:

```bash
git stash list
```

Inspect:

```bash
git stash show -p stash@{0}
```

Apply and keep:

```bash
git stash apply stash@{0}
```

Apply and remove:

```bash
git stash pop
```

Create branch from stash:

```bash
git stash branch feature/from-stash stash@{0}
```

---

## 13. Sync With Remote

Fetch:

```bash
git fetch origin
```

Fetch and prune:

```bash
git fetch --all --prune
```

Pull fast-forward only:

```bash
git pull --ff-only
```

Pull with rebase:

```bash
git pull --rebase
```

Check upstream:

```bash
git branch -vv
```

Set upstream:

```bash
git push -u origin HEAD
```

---

## 14. Update Feature Branch

Rebase on main:

```bash
git fetch origin
git switch feature/name
git rebase origin/main
git push --force-with-lease
```

Merge main into feature:

```bash
git fetch origin
git switch feature/name
git merge origin/main
git push
```

Rule:

```text
Rebase private branches.
Merge shared branches.
```

---

## 15. Conflict Commands

During merge conflict:

```bash
git status
git diff
git add <resolved-file>
git merge --continue
```

Abort merge:

```bash
git merge --abort
```

During rebase conflict:

```bash
git status
git diff
git add <resolved-file>
git rebase --continue
```

Abort rebase:

```bash
git rebase --abort
```

---

## 16. Cherry-Pick

Apply one commit to current branch:

```bash
git cherry-pick <commit>
```

Continue after conflict:

```bash
git add <resolved-file>
git cherry-pick --continue
```

Abort:

```bash
git cherry-pick --abort
```

---

## 17. Bisect

Manual:

```bash
git bisect start
git bisect bad
git bisect good <known-good-sha>
```

Then mark each tested commit:

```bash
git bisect good
git bisect bad
```

Automated:

```bash
git bisect start
git bisect bad
git bisect good <known-good-sha>
git bisect run <test-command>
git bisect reset
```

---

## 18. Tags And Releases

Create annotated tag:

```bash
git tag -a v1.4.0 -m "Release v1.4.0"
```

Push tag:

```bash
git push origin v1.4.0
```

List tags:

```bash
git tag
```

Find nearest tag:

```bash
git describe --tags
```

Release notes between tags:

```bash
git log --oneline v1.3.0..v1.4.0
```

Create GitHub release:

```bash
gh release create v1.4.0 --generate-notes
```

---

## 19. Clean Generated Files

Dry run:

```bash
git clean -n
```

Remove untracked files:

```bash
git clean -f
```

Remove untracked dirs:

```bash
git clean -fd
```

Remove ignored generated files:

```bash
git clean -fdX
```

Warning:

```text
git clean deletes files that are not tracked by Git.
Always dry run first.
```

---

## 20. Worktree

Create hotfix worktree:

```bash
git fetch origin
git worktree add -b hotfix/prod ../repo-hotfix origin/main
cd ../repo-hotfix
```

List:

```bash
git worktree list
```

Remove:

```bash
git worktree remove ../repo-hotfix
git worktree prune
```

---

## 21. Submodule

Clone with submodules:

```bash
git clone --recurse-submodules <repo-url>
```

Initialize:

```bash
git submodule update --init --recursive
```

Update:

```bash
git submodule update --recursive
```

Add:

```bash
git submodule add <repo-url> libs/shared
git commit -m "Add shared submodule"
```

---

## 22. Sparse Checkout

Enable:

```bash
git sparse-checkout init --cone
```

Set paths:

```bash
git sparse-checkout set services/order-service libs/common
```

Add path:

```bash
git sparse-checkout add services/payment-service
```

Disable:

```bash
git sparse-checkout disable
```

---

## 23. GitHub CLI Auth

Login:

```bash
gh auth login
```

Status:

```bash
gh auth status
```

Setup Git credentials:

```bash
gh auth setup-git
```

---

## 24. GitHub CLI PR Flow

Create PR:

```bash
gh pr create --fill --base main
```

Create draft:

```bash
gh pr create --draft --fill --base main
```

View:

```bash
gh pr view
```

Open browser:

```bash
gh pr view --web
```

Checks:

```bash
gh pr checks
gh pr checks --watch
```

Checkout PR:

```bash
gh pr checkout 123
```

Review:

```bash
gh pr review 123 --approve
gh pr review 123 --request-changes --body "Please add tests."
```

Merge:

```bash
gh pr merge --squash --delete-branch
```

Auto merge:

```bash
gh pr merge --squash --auto --delete-branch
```

---

## 25. GitHub CLI Issues

Create:

```bash
gh issue create --title "Bug title" --body "Bug details"
```

List:

```bash
gh issue list
```

Assigned to me:

```bash
gh issue list --assignee "@me"
```

View:

```bash
gh issue view 45
```

Comment:

```bash
gh issue comment 45 --body "I can work on this."
```

Close:

```bash
gh issue close 45 --comment "Fixed in #123."
```

---

## 26. GitHub CLI Workflows

List workflows:

```bash
gh workflow list
```

Run workflow:

```bash
gh workflow run "CI" --ref main
```

List runs:

```bash
gh run list
```

View run:

```bash
gh run view
```

Watch:

```bash
gh run watch
```

---

## 27. GitHub CLI Releases

List:

```bash
gh release list
```

Create:

```bash
gh release create v1.4.0 --generate-notes
```

View:

```bash
gh release view v1.4.0
```

Upload:

```bash
gh release upload v1.4.0 build/app.jar
```

Download:

```bash
gh release download v1.4.0
```

---

## 28. Safe vs Dangerous

Safe inspection:

```bash
git status -sb
git diff
git log --oneline
git show <commit>
git reflog
git clean -n
```

Use carefully:

```bash
git reset --soft HEAD~1
git reset --mixed HEAD~1
git restore <file>
git stash pop
git push --force-with-lease
```

Danger zone:

```bash
git reset --hard
git clean -fd
git push --force
git branch -D <branch>
```

---

## 29. Interview One-Liners

`fetch` vs `pull`:

```text
fetch updates remote-tracking branches; pull fetches and integrates into the current branch.
```

`merge` vs `rebase`:

```text
merge preserves branch history; rebase rewrites private branch commits onto a new base for a cleaner story.
```

`reset` vs `revert`:

```text
reset moves branch history; revert creates a new commit that undoes an old commit.
```

`force` vs `force-with-lease`:

```text
force overwrites remote; force-with-lease refuses if remote moved since your last fetch.
```

`origin` vs `upstream`:

```text
origin is usually your fork or default remote; upstream is usually the original source repo in fork workflows.
```

---

## 30. Full Clean PR Workflow

```bash
git fetch origin
git switch main
git pull --ff-only
git switch -c feature/order-validation

git status -sb
git diff
git add -p
git diff --staged
git commit -m "Add order validation"

git push -u origin HEAD
gh pr create --fill --base main
gh pr checks --watch
gh pr view --web
```

After merge:

```bash
git switch main
git pull --ff-only
git fetch --prune
git branch -d feature/order-validation
```

---

## 31. Final Memory Map

```text
Create work:
  switch -c, add -p, commit, push

Review work:
  diff, log, show, gh pr checks

Sync work:
  fetch, pull --ff-only, rebase, merge

Undo work:
  restore, reset, revert, reflog

Investigate work:
  blame, bisect, grep, range-diff

Collaborate:
  gh pr create, gh pr review, gh pr merge

Release:
  tag, gh release create
```

