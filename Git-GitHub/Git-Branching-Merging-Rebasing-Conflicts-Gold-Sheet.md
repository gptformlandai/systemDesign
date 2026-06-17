# Git Branching, Merging, Rebasing, and Conflicts Gold Sheet

> Goal: become confident creating branches, syncing with main, merging, rebasing, resolving conflicts, cherry-picking, and reverting.

---

## 0. How To Read This

Beginner focus:

- `git branch`
- `git switch`
- `git merge`
- conflict markers

Intermediate focus:

- `git rebase`
- `git cherry-pick`
- `git revert`
- merge vs rebase

Pro focus:

- conflict resolution strategy
- rebase safety
- clean PR history
- shared branch rules

---

# Topic 1: Branching, Merging, Rebasing, and Conflicts

---

## 1. Intuition

A branch is a movable label pointing to a commit.

```text
main:     A---B---C
feature:         \---D---E
```

Your feature branch lets you work without changing `main` until review/merge.

Beginner explanation:

Use branches to isolate work. Merge or rebase to bring histories together. Resolve conflicts when two branches changed the same area differently.

---

## 2. Definition

- Definition: Branching and integration commands manage parallel lines of development and combine changes safely.
- Category: Git collaboration and history management
- Core idea: isolate work, synchronize with main, integrate intentionally.

---

## 3. Why It Exists

Without branches:

- everyone edits main directly
- unfinished work blocks others
- releases are risky
- review is messy

Without merge/rebase skill:

- conflicts become scary
- PRs drift behind main
- history becomes hard to understand

---

## 4. Reality

Typical feature branch flow:

```bash
git switch main
git pull --ff-only
git switch -c feature/order-validation
# edit files
git add -p
git commit -m "Validate order total"
git push -u origin feature/order-validation
```

Before PR merge:

```bash
git fetch origin
git rebase origin/main
```

or:

```bash
git merge origin/main
```

Team policy decides.

---

## 5. How It Works

### Part A: Branch Commands

List branches:

```bash
git branch
```

List all local and remote branches:

```bash
git branch -a
```

Create branch:

```bash
git branch feature/order-validation
```

Switch branch:

```bash
git switch feature/order-validation
```

Create and switch:

```bash
git switch -c feature/order-validation
```

Delete local branch:

```bash
git branch -d feature/order-validation
```

Force delete local branch:

```bash
git branch -D feature/order-validation
```

### Part B: Merge

Merge another branch into current branch:

```bash
git switch feature/order-validation
git merge origin/main
```

Meaning:

```text
take changes from origin/main and combine them into my current branch
```

Merge keeps both histories and may create a merge commit.

### Part C: Rebase

Rebase current branch onto latest main:

```bash
git fetch origin
git rebase origin/main
```

Meaning:

```text
replay my commits on top of origin/main
```

Before:

```text
main:    A---B---C
feature:     \---D---E
```

After:

```text
main:    A---B---C
feature:         \---D'---E'
```

Rebase rewrites commit IDs.

### Part D: Merge vs Rebase

| Use | Merge | Rebase |
|---|---|---|
| Preserve exact branch history | yes | no |
| Clean linear feature history | less clean | yes |
| Shared branch safety | safer | risky |
| Local private branch cleanup | okay | great |

Rule:

> Rebase your own local/private branch. Avoid rebasing shared branches unless the team agrees.

### Part E: Conflict Markers

Conflict example:

```text
<<<<<<< HEAD
current branch version
=======
incoming branch version
>>>>>>> origin/main
```

Resolve by editing file to desired final content.

Then:

```bash
git add conflicted-file
```

Continue merge:

```bash
git merge --continue
```

Continue rebase:

```bash
git rebase --continue
```

Abort merge:

```bash
git merge --abort
```

Abort rebase:

```bash
git rebase --abort
```

### Part F: Conflict Resolution Flow

```bash
git status
# open conflicted files
# edit final content
git add <resolved-file>
git status
git rebase --continue
```

or:

```bash
git merge --continue
```

### Part G: Cherry-Pick

Apply one commit from elsewhere:

```bash
git cherry-pick <commit-sha>
```

Use for:

- hotfix from one branch to another
- picking one safe commit
- backporting

Do not use for:

- blindly copying a whole feature branch
- avoiding proper merge strategy

### Part H: Revert

Create a new commit that undoes another commit:

```bash
git revert <commit-sha>
```

Use when:

- commit is pushed/shared
- history must be preserved
- production rollback needs audit

### Part I: Squash Merge Concept

On GitHub, squash merge combines PR commits into one commit on main.

Good for:

- clean main history
- noisy feature commits

Trade-off:

- individual feature branch commits are not preserved on main

---

## 6. What Problem It Solves

- Primary problem solved: safe parallel development and integration
- Secondary benefits: PR isolation, release control, clean history
- Systems impact: teams can work independently without breaking main

---

## 7. When To Use Each Command

| Need | Command |
|---|---|
| New feature branch | `git switch -c feature/name` |
| Sync feature with main linearly | `git rebase origin/main` |
| Sync feature preserving merge history | `git merge origin/main` |
| Undo shared commit safely | `git revert <sha>` |
| Copy one commit | `git cherry-pick <sha>` |
| Abort bad merge | `git merge --abort` |
| Abort bad rebase | `git rebase --abort` |

---

## 8. When Not To Use It

Avoid:

```bash
git rebase main
```

if your local `main` is stale. Prefer:

```bash
git fetch origin
git rebase origin/main
```

Avoid rebasing:

- shared branches
- release branches used by others
- branches already under active review if team forbids rewrite

Avoid cherry-pick:

- when the whole branch should be merged
- when dependencies between commits are unclear

---

## 9. Pros and Cons

| Approach | Pros | Cons |
|---|---|---|
| Merge | preserves history | can create merge commits |
| Rebase | clean linear history | rewrites commit IDs |
| Squash | clean main | loses individual commit structure |
| Cherry-pick | precise commit transfer | can duplicate commits |
| Revert | safe for shared history | creates additional commit |

---

## 10. Trade-offs and Common Mistakes

### Trade-offs

- Linear history:
  Easier to read, but rebase requires discipline.
- Merge commits:
  Preserve context, but can clutter history.
- Squash merge:
  Clean main, but branch-level commit details vanish.

### Common Mistakes

- Mistake: rebasing shared branch.
  Better approach: merge or coordinate with team.

- Mistake: resolving conflict by keeping one side blindly.
  Better approach: understand both changes and run tests.

- Mistake: cherry-picking without dependencies.
  Better approach: inspect commit context with `git show`.

- Mistake: using reset to undo pushed shared commit.
  Better approach: `git revert`.

---

## 11. Key Commands

```bash
git switch -c feature/name
git fetch origin
git merge origin/main
git rebase origin/main
git rebase --continue
git rebase --abort
git merge --abort
git cherry-pick <sha>
git revert <sha>
```

---

## 12. Failure Modes

### Rebase Conflict

Fix:

```bash
git status
# resolve files
git add <file>
git rebase --continue
```

Abort:

```bash
git rebase --abort
```

### Merge Conflict

Fix:

```bash
git status
# resolve files
git add <file>
git merge --continue
```

Abort:

```bash
git merge --abort
```

### Rebased And Push Rejected

Reason:

- rebase changed commit IDs

If it is your feature branch:

```bash
git push --force-with-lease
```

Never use plain force casually.

### Need To Undo Bad Shared Commit

Use:

```bash
git revert <sha>
git push
```

---

## 13. Scenario

- Product / system: feature branch behind main with conflicts
- Why this concept fits: you need to update branch before PR merge
- What would go wrong without it: PR cannot merge or may break latest main

---

## 14. Code Sample

Clean rebase flow:

```bash
git fetch origin
git switch feature/order-validation
git rebase origin/main
# resolve conflicts if any
git status
git add <resolved-file>
git rebase --continue
git push --force-with-lease
```

Merge alternative:

```bash
git fetch origin
git switch feature/order-validation
git merge origin/main
git push
```

---

## 15. Mini Program / Simulation

Mental model:

```text
merge = combine histories
rebase = replay my commits on new base
revert = create new commit that undoes old commit
reset = move branch pointer
```

---

## 16. Practical Question

> Your feature branch is behind `main` and has conflicts. What do you do?

---

## 17. Strong Answer

I would fetch the latest remote state with `git fetch origin`. If team policy prefers linear history and the branch is mine, I would run `git rebase origin/main`, resolve conflicts one file at a time, stage resolved files, continue the rebase, run tests, then push with `--force-with-lease`. If the team prefers merge commits or the branch is shared, I would merge `origin/main` instead.

---

## 18. Revision Notes

- One-line summary: branches isolate work; merge combines histories; rebase replays commits.
- Three keywords: branch, merge, rebase
- One interview trap: rebasing shared branches without coordination.
- One memory trick: merge preserves, rebase rewrites, revert records undo.

---

## 19. Official Source Notes

- Git branch/merge/rebase docs: <https://git-scm.com/docs>
- GitHub rebase docs: <https://docs.github.com/en/get-started/using-git/about-git-rebase>
- GitHub conflict after rebase docs: <https://docs.github.com/en/get-started/using-git/resolving-merge-conflicts-after-a-git-rebase>

