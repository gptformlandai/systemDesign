# Git and GitHub Production Interview Scenario Playbook

> Goal: practice real repo situations until you know the correct command sequence and the interview explanation.

---

## 0. How To Use This Playbook

For every scenario, train yourself to answer in this structure:

```text
1. First I inspect state.
2. Then I choose the safest command based on local vs pushed/shared.
3. Then I verify.
4. Then I push/open PR only if needed.
5. I avoid rewriting shared history unless coordinated.
```

Default first commands:

```bash
git status -sb
git log --oneline --decorate -5
git remote -v
```

---

## 1. Scenario: I Edited Files On The Wrong Branch

Problem:

```text
You edited files on main, but the changes should be on feature/order-validation.
No commit yet.
```

Commands:

```bash
git status -sb
git switch -c feature/order-validation
git status -sb
```

Why it works:

```text
Uncommitted changes move with you when switching branches if there is no conflict.
Creating the feature branch from current point preserves the edits.
```

If Git refuses because files would be overwritten:

```bash
git stash push -u -m "move wrong-branch work"
git switch -c feature/order-validation
git stash pop
```

Strong interview line:

```text
Since the work is uncommitted, I do not need reset or revert. I just create/switch to the correct branch, using stash only if checkout conflicts.
```

---

## 2. Scenario: I Committed On The Wrong Branch

Problem:

```text
You committed on main, but the commit belongs on a feature branch.
Commit is local only.
```

Commands:

```bash
git status -sb
git log --oneline --decorate -3
git switch -c feature/order-validation
git switch main
git reset --hard HEAD~1
git switch feature/order-validation
```

Why it works:

```text
The feature branch is created at the accidental commit.
Then main is moved back locally.
```

If the commit was pushed to shared main:

```bash
git switch main
git pull --ff-only
git revert <bad-commit>
git push
git switch -c feature/order-validation
git cherry-pick <bad-commit>
git push -u origin HEAD
```

Strong interview line:

```text
Local mistake can be fixed with reset. Shared mistake should be fixed with revert to preserve history.
```

---

## 3. Scenario: I Staged Too Much

Problem:

```text
You ran git add . and staged unrelated files.
```

Commands:

```bash
git status -sb
git diff --staged
git restore --staged <file>
```

Then stage carefully:

```bash
git add -p
git diff --staged
git commit -m "Add order validation"
```

Strong interview line:

```text
I unstage without discarding working tree changes, then use patch staging to create a focused commit.
```

---

## 4. Scenario: I Want To Undo Last Local Commit But Keep Changes

Problem:

```text
Last commit is local only and should be redone.
```

Keep changes staged:

```bash
git reset --soft HEAD~1
```

Keep changes unstaged:

```bash
git reset --mixed HEAD~1
```

Verify:

```bash
git status -sb
git diff
git diff --staged
```

Strong interview line:

```text
Because it is local only, reset is acceptable. Soft keeps changes staged; mixed keeps changes in the working tree.
```

---

## 5. Scenario: I Want To Undo A Pushed Commit

Problem:

```text
Bad commit is already on a remote/shared branch.
```

Commands:

```bash
git switch main
git pull --ff-only
git log --oneline --decorate -10
git revert <bad-commit>
git push
```

Strong interview line:

```text
I use revert because other developers may already have the commit. Revert preserves history and avoids breaking their local branches.
```

---

## 6. Scenario: I Need To Recover A Lost Commit

Problem:

```text
You reset/rebased and a commit disappeared from log.
```

Commands:

```bash
git reflog
git switch -c rescue-lost-commit <sha>
git log --oneline --decorate -5
```

Why branch instead of reset immediately:

```text
Creating a rescue branch is safer. It preserves the found commit without moving the current branch again.
```

Strong interview line:

```text
Git reflog records local HEAD movements, so even if a commit falls out of branch history, I can often recover it by creating a new branch at that SHA.
```

---

## 7. Scenario: Merge Conflict During Pull/Rebase

Problem:

```text
You rebased feature branch on main and got conflicts.
```

Commands:

```bash
git status
git diff
```

Open files and resolve conflict markers:

```text
<<<<<<< HEAD
current side
=======
incoming side
>>>>>>> branch
```

After resolving:

```bash
git add <resolved-file>
git rebase --continue
```

Abort if needed:

```bash
git rebase --abort
```

For merge:

```bash
git merge --continue
```

or:

```bash
git merge --abort
```

Strong interview line:

```text
I inspect the conflict, preserve both intended changes where needed, run tests, then continue the rebase or merge. I do not blindly choose one side.
```

---

## 8. Scenario: Non-Fast-Forward Push Rejected

Problem:

```text
git push is rejected because remote has commits you do not have.
```

Commands:

```bash
git fetch origin
git log --oneline --decorate HEAD..origin/<branch>
```

If your private feature branch:

```bash
git rebase origin/<branch>
git push --force-with-lease
```

If shared branch:

```bash
git merge origin/<branch>
git push
```

Strong interview line:

```text
I first fetch and inspect remote changes. I do not force push blindly. For my own rebased feature branch I use force-with-lease, not plain force.
```

---

## 9. Scenario: My PR Has Conflicts With Main

Problem:

```text
GitHub says branch cannot merge due to conflicts.
```

Rebase approach for private feature branch:

```bash
git fetch origin
git switch feature/order-validation
git rebase origin/main
git status
git add <resolved-file>
git rebase --continue
git push --force-with-lease
```

Merge approach if team prefers merge commits:

```bash
git fetch origin
git switch feature/order-validation
git merge origin/main
git add <resolved-file>
git merge --continue
git push
```

Strong interview line:

```text
I follow team policy. Rebase gives cleaner feature history for private branches; merge avoids rewriting history.
```

---

## 10. Scenario: I Need To Update My Fork

Problem:

```text
You forked a repo. Your fork's main is behind upstream.
```

Set upstream:

```bash
git remote -v
git remote add upstream <upstream-url>
```

Sync:

```bash
git fetch upstream
git switch main
git merge --ff-only upstream/main
git push origin main
```

Strong interview line:

```text
Origin is my fork. Upstream is the original repo. I fetch upstream and fast-forward my local main, then push my fork.
```

---

## 11. Scenario: I Need To Cherry-Pick A Hotfix

Problem:

```text
A fix merged to main must also go to release/1.4.
```

Commands:

```bash
git fetch origin
git switch release/1.4
git pull --ff-only
git cherry-pick <fix-commit>
git push origin release/1.4
```

If conflict:

```bash
git status
git add <resolved-file>
git cherry-pick --continue
```

Abort:

```bash
git cherry-pick --abort
```

Strong interview line:

```text
Cherry-pick copies one specific commit to another branch. It is useful for hotfix backports but can create duplicate commits, so I use it intentionally.
```

---

## 12. Scenario: I Need Emergency Hotfix While My Working Tree Is Dirty

Problem:

```text
Production is broken, but your current feature branch has many uncommitted changes.
```

Best approach:

```bash
git fetch origin
git worktree add -b hotfix/prod-timeout ../repo-hotfix origin/main
cd ../repo-hotfix
```

Fix and open PR:

```bash
git add -p
git commit -m "Fix production timeout"
git push -u origin hotfix/prod-timeout
gh pr create --fill --base main
```

Strong interview line:

```text
I use worktree so the hotfix is isolated from unfinished work. This avoids risky stash/reset operations during an incident.
```

---

## 13. Scenario: I Need To Find Who Introduced A Bug

Problem:

```text
Bug exists now, older release was good.
```

Commands:

```bash
git tag
git bisect start
git bisect bad
git bisect good <known-good-tag-or-sha>
git bisect run <test-command>
git bisect reset
```

Inspect found commit:

```bash
git show <bad-commit>
git log -S "suspiciousFunction" -p
git blame -L 100,150 <file>
```

Strong interview line:

```text
I use bisect to find the first bad commit with a repeatable test, then inspect the code and context. Blame is context, not accusation.
```

---

## 14. Scenario: I Need To Split One Big Commit

Problem:

```text
One commit contains unrelated changes.
It is local only.
```

Commands:

```bash
git reset --mixed HEAD~1
git add -p
git commit -m "Add order validation"
git add -p
git commit -m "Refactor payment test helpers"
```

Strong interview line:

```text
Since the commit is local, I reset it into working tree changes and rebuild focused commits using patch staging.
```

---

## 15. Scenario: I Need To Squash Local Commits Before PR

Problem:

```text
Your feature branch has noisy WIP commits.
```

Interactive rebase:

```bash
git fetch origin
git rebase -i origin/main
```

In editor:

```text
pick abc123 Add order validation
squash def456 Fix typo
squash 999aaa Address test
```

Push:

```bash
git push --force-with-lease
```

Alternative:

```bash
git reset --soft origin/main
git commit -m "Add order validation"
git push --force-with-lease
```

Strong interview line:

```text
I only rewrite my private branch. For shared branches, I avoid rewriting and use merge/revert patterns instead.
```

---

## 16. Scenario: I Accidentally Force Pushed

Problem:

```text
Remote branch lost commits after force push.
```

Immediate actions:

```text
1. Stop pushing.
2. Tell the team.
3. Find the old commit SHA.
```

Find recovery SHA:

```bash
git reflog
git reflog show origin/<branch>
```

Ask teammate who still has old branch:

```bash
git log --oneline --decorate -10
```

Restore by pushing recovered branch:

```bash
git switch -c restore-branch <old-sha>
git push -u origin restore-branch
```

Then coordinate replacement or PR.

Strong interview line:

```text
I would not keep force pushing. I would preserve the recovered commit on a new branch, coordinate with the team, and restore through review if possible.
```

---

## 17. Scenario: I Need To Delete Branches Safely

Delete local merged branch:

```bash
git branch -d feature/old-work
```

Force delete local branch:

```bash
git branch -D feature/old-work
```

Delete remote branch:

```bash
git push origin --delete feature/old-work
```

Clean remote tracking branches:

```bash
git fetch --prune
```

Strong interview line:

```text
I use -d for safe local deletion because it refuses unmerged branches. I only use -D when I am sure the branch is no longer needed.
```

---

## 18. Scenario: I Need To Create A Release Tag

Commands:

```bash
git switch main
git pull --ff-only
git log --oneline --decorate -5
git tag -a v1.4.0 -m "Release v1.4.0"
git push origin v1.4.0
gh release create v1.4.0 --generate-notes
```

Strong interview line:

```text
I tag the exact commit being released, push the tag, and create a GitHub release. Annotated tags are better for releases because they contain metadata and a message.
```

---

## 19. Scenario: CI Failed On My PR

Commands:

```bash
gh pr checks
gh pr view --web
gh run list
gh run view
```

Then inspect locally:

```bash
git status -sb
git diff origin/main...HEAD
```

Fix:

```bash
git add -p
git commit -m "Fix failing order validation test"
git push
gh pr checks --watch
```

Strong interview line:

```text
I first identify which check failed, reproduce locally if possible, push a focused fix commit, and wait for CI to pass before requesting review again.
```

---

## 20. Scenario: I Need To Review Someone Else's PR Locally

Commands:

```bash
gh pr checkout 123
git status -sb
git diff origin/main...HEAD
git log --oneline origin/main..HEAD
```

Run tests:

```bash
mvn test
```

Review:

```bash
gh pr review 123 --comment --body "Reviewed locally and left comments."
```

Approve:

```bash
gh pr review 123 --approve
```

Strong interview line:

```text
For non-trivial PRs I review locally, inspect the branch diff against main, run targeted tests, then review through GitHub.
```

---

## 21. Scenario: I Need To Compare PR Before And After Rebase

Commands:

```bash
git fetch origin
git range-diff origin/main...old-feature origin/main...feature
```

Alternative if old branch does not exist:

```bash
git reflog
```

Find previous head SHA and compare:

```bash
git range-diff origin/main...<old-sha> origin/main...HEAD
```

Strong interview line:

```text
Range-diff compares two commit series, which is better than a raw diff when verifying a rebased PR.
```

---

## 22. Scenario: I Need To Remove A Secret From Repo

If local only:

```bash
git reset --mixed HEAD~1
git restore --staged <secret-file>
```

Remove secret, then recommit:

```bash
git add -p
git commit -m "Add config without secret"
```

If pushed:

```text
1. Rotate/revoke the secret immediately.
2. Inform the team/security process.
3. Remove the secret in a new commit.
4. Coordinate history rewrite only if required by policy.
```

Command for latest removal:

```bash
git rm --cached <secret-file>
git commit -m "Remove secret file from repository"
git push
```

Strong interview line:

```text
Removing the file from the latest commit is not enough because the secret remains in history. Rotation is the real security fix.
```

---

## 23. Scenario: I Need To Sync My Local Main After PR Merge

Commands:

```bash
git switch main
git pull --ff-only
git fetch --prune
git branch -d feature/order-validation
```

Strong interview line:

```text
I keep local main clean and fast-forward only. Then I prune deleted remote branches and delete merged local feature branches.
```

---

## 24. Scenario: I Need To Stop Tracking A Generated File

Problem:

```text
target/output.log is already tracked but should be ignored.
```

Commands:

First add these lines to `.gitignore`:

```gitignore
target/
*.log
```

Then remove the already tracked file from the index:

```bash
git rm --cached target/output.log
git add .gitignore
git commit -m "Stop tracking generated output"
```

Strong interview line:

```text
.gitignore only affects untracked files. For already tracked files, I remove them from the index with git rm --cached.
```

---

## 25. Scenario: I Need A Clean Branch From Main

Commands:

```bash
git fetch origin
git switch main
git pull --ff-only
git switch -c feature/new-work
```

Alternative one-liner:

```bash
git fetch origin
git switch -c feature/new-work origin/main
```

Strong interview line:

```text
I start from updated main so my PR contains only relevant changes and avoids avoidable conflicts.
```

---

## 26. Scenario: I Need To Inspect What Will Be Pushed

Commands:

```bash
git status -sb
git log --oneline origin/<branch>..HEAD
git diff origin/<branch>...HEAD
```

If upstream branch is configured:

```bash
git log --oneline @{u}..HEAD
git diff @{u}...HEAD
```

Strong interview line:

```text
Before pushing, I compare my local branch against its upstream to verify commits and content.
```

---

## 27. Scenario: I Need To Know If My Branch Is Behind

Commands:

```bash
git fetch origin
git status -sb
git branch -vv
```

Detailed:

```bash
git log --oneline HEAD..@{u}
git log --oneline @{u}..HEAD
```

Strong interview line:

```text
I fetch first because local remote-tracking branches may be stale. Then I compare HEAD with upstream.
```

---

## 28. Scenario: I Need To Abort A Bad Operation

Abort merge:

```bash
git merge --abort
```

Abort rebase:

```bash
git rebase --abort
```

Abort cherry-pick:

```bash
git cherry-pick --abort
```

Abort bisect:

```bash
git bisect reset
```

Strong interview line:

```text
I use the operation-specific abort command so Git returns the repo to the pre-operation state when possible.
```

---

## 29. Practical Interview Drill

Question:

> Your branch is rejected on push, your PR has conflicts, and production needs a hotfix. Walk me through what you do.

Strong answer:

```text
First, I separate the problems.

For the rejected push, I fetch and inspect what changed remotely. I avoid force pushing blindly.

For PR conflicts, I update my branch using team policy: rebase for a private branch or merge from main if that is the repo convention. I resolve conflicts, run tests, and push.

For the production hotfix, I do not mix it with the dirty feature branch. I create a worktree from origin/main, make a focused hotfix branch, push it, open a PR, and merge through the emergency process.
```

Command sketch:

```bash
git fetch origin
git log --oneline HEAD..origin/<branch>
git rebase origin/main
git push --force-with-lease

git worktree add -b hotfix/prod ../repo-hotfix origin/main
cd ../repo-hotfix
git add -p
git commit -m "Fix production issue"
git push -u origin hotfix/prod
gh pr create --fill --base main
```

---

## 30. Revision Notes

- One-line summary: always inspect first, choose the safest command for local vs shared state, then verify.
- Three keywords: inspect, preserve, recover.
- One interview trap: using force push or reset on a shared branch without checking who depends on it.
- One memory trick: "Local mistakes can be rewritten; shared mistakes should be reversed."
