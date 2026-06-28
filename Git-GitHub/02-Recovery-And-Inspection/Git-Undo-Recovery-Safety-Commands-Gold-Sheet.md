# Git Undo, Recovery, and Safety Commands Gold Sheet

> Goal: know exactly what to type when something goes wrong, without damaging shared history or losing work.

---

## 0. How To Read This Doc

This document is for the moments where Git feels scary:

```text
I changed the wrong file.
I staged too much.
I committed too early.
I committed on the wrong branch.
I pushed a bad commit.
I lost a commit.
I need my stash back.
I need to clean generated files.
I need to undo safely in a team repo.
```

The most important rule:

```text
Before undoing, identify where the change lives:

1. Working tree only?
2. Staging area?
3. Local commit?
4. Pushed commit?
5. Shared protected branch?
```

Your command depends on that answer.

---

## 1. Intuition

Git undo commands are like different levels of "time travel."

```text
restore  = fix files
reset    = move branch pointer
revert   = make a new commit that cancels old work
reflog   = find where your branch used to be
stash    = temporarily park dirty work
clean    = remove untracked files
```

Beginner mental model:

```text
If it is not committed, use restore.
If it is committed only locally, reset can help.
If it is pushed/shared, use revert.
If you lost it, check reflog.
```

---

## 2. Definition

- Definition: Git recovery is the set of commands used to move files, staging area, commits, and branch pointers back to a safe state.
- Category: Source control safety, history management, collaboration hygiene.
- Core idea: choose the least destructive command that solves the exact problem.

---

## 3. Why It Exists

Real developers make small mistakes constantly:

- stage extra files
- commit debug logs
- commit on the wrong branch
- break tests
- merge incorrectly
- rebase incorrectly
- push something that should not have gone out
- lose local work after reset/rebase

Without recovery commands, teams would either panic, rewrite shared history carelessly, or waste hours manually recreating work.

Git gives multiple undo tools because each mistake lives at a different layer.

---

## 4. Reality

Production teams rely on these commands every day:

- `git restore` for local file cleanup
- `git restore --staged` for staging mistakes
- `git reset` for local commit cleanup
- `git revert` for production-safe rollback
- `git reflog` for disaster recovery
- `git stash` for context switching
- `git clean -n` and `git clean -fd` for generated files
- `git push --force-with-lease` for safe private branch history updates

In interviews, a strong candidate does not just know the command. They first ask:

```text
Was it pushed?
Is anyone else using the branch?
Do we need to preserve audit history?
Do we want to keep the file changes?
```

---

## 5. How It Works

Git tracks state in layers:

```text
working tree
  your local files

staging area / index
  what will go into the next commit

local commit history
  commits pointed to by your current branch

remote history
  commits visible to other people
```

Recovery commands operate on different layers:

| Command | Changes Files? | Changes Staging? | Changes Commit History? | Safe On Shared Branch? |
|---|---:|---:|---:|---:|
| `git restore <file>` | Yes | No | No | Yes |
| `git restore --staged <file>` | No | Yes | No | Yes |
| `git reset --soft HEAD~1` | No | Yes | Yes, local pointer | Only if not pushed |
| `git reset --mixed HEAD~1` | No | Yes | Yes, local pointer | Only if not pushed |
| `git reset --hard HEAD~1` | Yes | Yes | Yes, local pointer | Dangerous |
| `git revert <commit>` | Yes via new commit | Yes via new commit | Adds new commit | Yes |
| `git reflog` | No | No | Reads movement history | Yes |
| `git clean -fd` | Deletes untracked files | No | No | Dangerous locally |

---

## 6. What Problem It Solves

- Primary problem solved: safely recovering from local and shared repository mistakes.
- Secondary benefits: cleaner history, safer collaboration, faster incident rollback.
- Systems impact: prevents broken releases, lost work, and dangerous history rewrites.

---

## 7. When To Rely On It

Use these commands when you hear:

- "I staged the wrong file."
- "I want to discard local changes."
- "I committed too early."
- "I committed on the wrong branch."
- "I pushed a bad commit."
- "I lost my work after rebase."
- "I need to rollback production."
- "My branch is messed up."
- "I want to clean generated files."

---

## 8. When Not To Use It

Do not use destructive commands casually.

Avoid:

```bash
git reset --hard
git clean -fd
git push --force
```

unless you know exactly what will be lost.

Prefer:

```bash
git status
git diff
git log --oneline --decorate -5
git reflog -10
git clean -n
git push --force-with-lease
```

before destructive operations.

---

## 9. Pros and Cons

| Pros | Cons |
|---|---|
| Fast recovery from mistakes | Wrong command can lose local work |
| Supports both local and shared rollback | `reset` and `rebase` can rewrite history |
| `reflog` can rescue lost commits | Reflog is local, not a shared backup |
| `revert` preserves audit history | Revert commits can make history noisier |
| `stash` helps context switching | Stashes are easy to forget |

---

## 10. Trade-offs and Common Mistakes

### Trade-offs

- `reset` is clean for local cleanup but unsafe for shared history.
- `revert` is safe for shared history but creates an extra commit.
- `stash` is fast for temporary parking but can hide important work.
- `clean` removes noise but can delete untracked work.
- `force-with-lease` enables clean branch history but still needs care.

### Common Mistakes

Mistake:

```bash
git reset --hard origin/main
```

Why it is wrong:

This discards all local tracked file changes and moves the branch to `origin/main`.

Better approach:

```bash
git status
git diff
git stash push -u -m "save before syncing"
git fetch origin
git switch main
git pull --ff-only
```

Mistake:

```bash
git push --force
```

Why it is wrong:

It can overwrite remote work added by someone else.

Better approach:

```bash
git push --force-with-lease
```

Mistake:

```bash
git revert HEAD~3..HEAD
```

without checking the commit range.

Better approach:

```bash
git log --oneline --decorate -10
git show <commit>
git revert <bad-commit>
```

---

## 11. Key Commands

### Check Current State First

```bash
git status -sb
git diff
git diff --staged
git log --oneline --decorate -5
```

Use this before almost every recovery command.

### Unstage A File

```bash
git restore --staged <file>
```

Example:

```bash
git restore --staged application.yml
```

What it does:

```text
staging area -> removes file from next commit
working tree -> keeps your edits
commit history -> unchanged
```

### Discard Local File Changes

```bash
git restore <file>
```

Example:

```bash
git restore src/main/java/com/app/OrderService.java
```

What it does:

```text
working tree -> resets file to HEAD version
staging area -> unchanged
commit history -> unchanged
```

Warning:

```text
Local uncommitted edits in that file are lost.
```

### Restore A File From Another Commit

```bash
git restore --source=<commit> -- <file>
```

Example:

```bash
git restore --source=origin/main -- pom.xml
```

Use when:

```text
I want this file to look exactly like it does on main.
```

### Undo Last Local Commit But Keep Changes Staged

```bash
git reset --soft HEAD~1
```

Use when:

```text
I committed too early, but I want to immediately recommit.
```

State after command:

```text
commit removed from branch pointer
changes remain staged
files remain edited
```

### Undo Last Local Commit And Keep Changes Unstaged

```bash
git reset --mixed HEAD~1
```

or:

```bash
git reset HEAD~1
```

Use when:

```text
I committed too early and want to reselect files.
```

State after command:

```text
commit removed from branch pointer
changes remain in working tree
staging area cleared
```

### Undo Last Local Commit And Discard Changes

```bash
git reset --hard HEAD~1
```

Use only when:

```text
The commit is local and I truly do not need those changes.
```

Safer pre-check:

```bash
git log --oneline --decorate -3
git show --stat HEAD
```

### Revert A Pushed Commit

```bash
git revert <commit>
```

Example:

```bash
git revert a1b2c3d
git push
```

What it does:

```text
creates a new commit that reverses the old commit
does not rewrite public history
safe for shared branches
```

### Revert A Merge Commit

```bash
git revert -m 1 <merge-commit>
```

Meaning:

```text
-m 1 says keep parent 1 as the mainline and undo the merged-in changes.
```

Use carefully:

```bash
git show --summary <merge-commit>
git revert -m 1 <merge-commit>
```

### Amend Last Commit

Change only the message:

```bash
git commit --amend -m "Better commit message"
```

Add missed file to last commit:

```bash
git add <file>
git commit --amend --no-edit
```

Rule:

```text
Amend is usually fine before push.
After push, amend requires force-with-lease and should be done only for your private branch.
```

### Recover Lost Commits With Reflog

```bash
git reflog
```

Example:

```bash
git reflog --date=local
```

Recover:

```bash
git switch -c rescue-lost-work <sha>
```

or:

```bash
git reset --hard <sha>
```

Safer recovery is usually:

```bash
git switch -c rescue-lost-work <sha>
```

because it creates a new branch instead of moving your current branch immediately.

### Stash Current Work

```bash
git stash push -m "work in progress before pulling main"
```

Include untracked files:

```bash
git stash push -u -m "save untracked test files"
```

List:

```bash
git stash list
```

Inspect:

```bash
git stash show -p stash@{0}
```

Apply but keep stash:

```bash
git stash apply stash@{0}
```

Apply and remove stash:

```bash
git stash pop
```

Create branch from stash:

```bash
git stash branch feature/from-stash stash@{0}
```

Drop stash:

```bash
git stash drop stash@{0}
```

### Clean Untracked Files

Dry run first:

```bash
git clean -n
```

Remove untracked files:

```bash
git clean -f
```

Remove untracked directories too:

```bash
git clean -fd
```

Remove ignored generated files too:

```bash
git clean -fdX
```

Warning:

```text
Files removed by git clean are not in Git history.
If you need them, commit, stash with -u, or copy them before cleaning.
```

### Force Push Safely

```bash
git push --force-with-lease
```

Use when:

```text
You rebased your own feature branch and need to update the remote branch.
```

Avoid:

```bash
git push --force
```

because it does not protect you from overwriting remote commits you have not seen.

---

## 12. Safety Matrix

### Green Commands

Generally safe:

```bash
git status -sb
git diff
git diff --staged
git log --oneline
git show <commit>
git reflog
git stash list
git clean -n
```

### Yellow Commands

Safe if you understand the layer:

```bash
git restore <file>
git restore --staged <file>
git reset --soft HEAD~1
git reset --mixed HEAD~1
git commit --amend
git stash pop
git revert <commit>
git push --force-with-lease
```

### Red Commands

Double-check before typing:

```bash
git reset --hard
git clean -fd
git clean -fdx
git push --force
git branch -D <branch>
```

---

## 13. Failure Modes

### Failure Mode 1: Reset Removed My Commit

Observed:

```text
Commit disappeared from git log.
```

Recover:

```bash
git reflog
git switch -c rescue <sha>
```

### Failure Mode 2: Stash Pop Caused Conflicts

Observed:

```text
Files contain conflict markers.
```

Recover:

```bash
git status
git diff
```

Resolve conflicts, then:

```bash
git add <file>
git commit -m "Apply stashed changes"
```

If you used `stash pop`, Git usually keeps the stash when conflicts occur. Check:

```bash
git stash list
```

### Failure Mode 3: Pushed Bad Commit To Main

Observed:

```text
CI fails or production breaks after a pushed commit.
```

Recover:

```bash
git switch main
git pull --ff-only
git revert <bad-commit>
git push
```

Then create a fix branch if needed:

```bash
git switch -c fix/root-cause
```

### Failure Mode 4: Deleted Local Branch

Observed:

```text
Feature branch is gone locally.
```

Recover if remote exists:

```bash
git fetch origin
git switch -c feature/name origin/feature/name
```

Recover if only local existed:

```bash
git reflog
git switch -c recovered-feature <sha>
```

### Failure Mode 5: Committed Secret

If only local:

```bash
git reset --mixed HEAD~1
git restore --staged <secret-file>
```

Then remove secret from file, rotate the secret anyway if it was real.

If pushed:

```text
1. Rotate/revoke the secret immediately.
2. Inform the team/security process.
3. Remove the secret in a new commit.
4. Consider history rewrite only with coordinated approval.
```

Basic removal commit:

```bash
git rm --cached <secret-file>
git commit -m "Remove secret file from repository"
git push
```

Note:

```text
Removing a secret from the latest commit does not erase it from history.
Rotation is mandatory.
```

---

## 14. Scenario

### Scenario: You Committed On The Wrong Branch

You are on `main` and accidentally created a commit that belongs on `feature/payment-validation`.

Check:

```bash
git status -sb
git log --oneline --decorate -3
```

Create correct branch at current commit:

```bash
git switch -c feature/payment-validation
```

Move `main` back by one commit:

```bash
git switch main
git reset --hard HEAD~1
```

Now your commit lives on the feature branch.

If the bad commit was already pushed to shared `main`, do not reset shared main. Use:

```bash
git revert <bad-commit>
git push
```

Then cherry-pick the work into a feature branch:

```bash
git switch -c feature/payment-validation
git cherry-pick <bad-commit>
```

---

## 15. Code Sample

### Java Mental Model: Reset vs Revert

```java
import java.util.ArrayList;
import java.util.List;

public class GitUndoMentalModel {
    public static void main(String[] args) {
        List<String> history = new ArrayList<>();
        history.add("A: initial commit");
        history.add("B: add order API");
        history.add("C: bad payment change");

        System.out.println("Before reset: " + history);

        // reset moves the branch pointer backward.
        history.remove(history.size() - 1);
        System.out.println("After reset:  " + history);

        // revert keeps history and adds a new inverse commit.
        history.add("C: bad payment change");
        history.add("D: revert C");
        System.out.println("After revert: " + history);
    }
}
```

Interview explanation:

```text
Reset changes where the branch points.
Revert creates a new commit that undoes an older commit.
That is why revert is safer for shared history.
```

---

## 16. Mini Program / Simulation

### Python Simulation: Choose The Undo Command

```python
def choose_command(location, pushed, shared):
    if location == "working_tree":
        return "git restore <file>"
    if location == "staging_area":
        return "git restore --staged <file>"
    if location == "local_commit" and not pushed:
        return "git reset --soft HEAD~1 or git reset --mixed HEAD~1"
    if pushed or shared:
        return "git revert <commit>"
    return "git reflog, inspect, then recover with a branch"


cases = [
    ("working_tree", False, False),
    ("staging_area", False, False),
    ("local_commit", False, False),
    ("local_commit", True, True),
]

for case in cases:
    print(case, "=>", choose_command(*case))
```

---

## 17. Practical Question

> You pushed a commit to `main` that breaks production. What commands do you use and why?

---

## 18. Strong Answer

I would avoid rewriting shared `main`. First I would identify the bad commit:

```bash
git switch main
git pull --ff-only
git log --oneline --decorate -10
```

Then I would create a revert commit:

```bash
git revert <bad-commit>
git push
```

This preserves history and is safe for everyone who already pulled `main`. After service is restored, I would create a separate fix branch, add tests for the failure, open a PR, and merge normally after review.

If the bad commit exposed a secret, I would rotate the secret immediately. A revert does not erase secret history.

---

## 19. Revision Notes

- One-line summary: use `restore` for files, `reset` for local commits, `revert` for shared commits, `reflog` for rescue.
- Three keywords: layer, history, safety.
- One interview trap: using `reset --hard` on a shared branch.
- One memory trick: "Revert records the apology; reset rewrites the story."

---

## 20. Command Decision Tree

```text
Did you only edit a file?
  -> git restore <file>

Did you stage a file by mistake?
  -> git restore --staged <file>

Did you commit locally and want to redo the commit?
  -> git reset --soft HEAD~1

Did you commit locally and want to reselect files?
  -> git reset --mixed HEAD~1

Did you push the bad commit?
  -> git revert <commit>

Did you lose a commit?
  -> git reflog
  -> git switch -c rescue <sha>

Do you need to save dirty work temporarily?
  -> git stash push -u -m "message"

Do you need to remove generated files?
  -> git clean -n
  -> git clean -fd
```

---

## 21. Official Source Notes

- Git reset, restore, revert, stash, clean, and reflog are part of the official Git command reference: <https://git-scm.com/docs>
- GitHub recommends safe collaboration through branches, pull requests, and protected history: <https://docs.github.com/en/get-started/using-git>

