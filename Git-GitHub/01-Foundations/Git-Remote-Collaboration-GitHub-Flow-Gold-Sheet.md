# Git Remote Collaboration and GitHub Flow Gold Sheet

> Goal: master commands for working with remote repositories, pushing branches, syncing with teammates, forks, pull requests, and non-fast-forward errors.

---

## 0. How To Read This

Beginner focus:

- `git clone`
- `git remote -v`
- `git fetch`
- `git pull`
- `git push`

Intermediate focus:

- upstream tracking
- `origin/main`
- non-fast-forward errors
- fork workflow
- pull request flow

Pro focus:

- safe sync habits
- protected branch workflow
- fork vs upstream remotes
- PR review and branch update strategy

---

# Topic 1: Remote Collaboration and GitHub Flow

---

## 1. Intuition

Local Git is your notebook.

Remote GitHub is the shared notebook.

You:

```text
fetch to see remote changes
pull/rebase to integrate them
push to share your commits
open PR to review and merge
```

Beginner explanation:

Remote commands move commits between your local repository and GitHub.

---

## 2. Definition

- Definition: Remote collaboration commands synchronize local commit history with remote repositories and support PR-based teamwork.
- Category: Distributed version control collaboration
- Core idea: share work through branches and pull requests without breaking protected main.

---

## 3. Why It Exists

In teams:

- many developers change code
- main branch must stay stable
- code should be reviewed before merge
- CI should validate changes
- remote history must be shared safely

GitHub flow solves this:

```text
branch -> commit -> push -> pull request -> review/CI -> merge
```

---

## 4. Reality

Daily remote flow:

```bash
git switch main
git pull --ff-only
git switch -c feature/order-validation
# work and commit
git push -u origin feature/order-validation
```

Update feature branch:

```bash
git fetch origin
git rebase origin/main
git push --force-with-lease
```

or:

```bash
git merge origin/main
git push
```

---

## 5. How It Works

### Part A: Remote Basics

Show remotes:

```bash
git remote -v
```

Add remote:

```bash
git remote add origin git@github.com:org/repo.git
```

Rename remote:

```bash
git remote rename origin upstream
```

Remove remote:

```bash
git remote remove old-origin
```

### Part B: `origin` and `upstream`

Common meanings:

```text
origin = your fork or primary remote you push to
upstream = original project repository
```

In company repos:

```text
origin = company repo
```

In open-source fork flow:

```text
origin = your fork
upstream = original repo
```

### Part C: Fetch

Fetch downloads remote refs without changing your current branch:

```bash
git fetch origin
```

Fetch all remotes:

```bash
git fetch --all --prune
```

Prune deleted remote branches:

```bash
git fetch --prune
```

Pro habit:

```bash
git fetch origin
git log --oneline --graph --decorate --all -10
```

### Part D: Pull

Pull is fetch plus integrate.

Default pull may merge or rebase depending config.

Safer main update:

```bash
git switch main
git pull --ff-only
```

Pull with rebase:

```bash
git pull --rebase
```

Interview sentence:

> `fetch` is observe; `pull` is observe plus integrate.

### Part E: Push

Push current branch:

```bash
git push
```

Push new branch and set upstream:

```bash
git push -u origin feature/order-validation
```

Push tags:

```bash
git push origin v1.2.0
```

Delete remote branch:

```bash
git push origin --delete feature/order-validation
```

### Part F: Upstream Tracking

Check tracking:

```bash
git branch -vv
```

Set upstream:

```bash
git branch --set-upstream-to=origin/feature/order-validation
```

Why it matters:

- `git pull` knows where to pull from
- `git push` knows where to push
- status shows ahead/behind

### Part G: Non-Fast-Forward Error

Error meaning:

```text
remote has commits you do not have locally
```

Safe fix:

```bash
git fetch origin
git rebase origin/main
git push
```

If you rebased your own pushed feature branch:

```bash
git push --force-with-lease
```

Do not use:

```bash
git push --force
```

unless you absolutely know the impact.

### Part H: GitHub Flow

```text
1. Create branch from main
2. Commit changes
3. Push branch
4. Open pull request
5. Review and CI
6. Update branch if needed
7. Merge
8. Delete branch
```

Commands:

```bash
git switch main
git pull --ff-only
git switch -c feature/payment-validation
git add -p
git commit -m "Validate payment amount"
git push -u origin feature/payment-validation
```

### Part I: Fork Flow

Clone your fork:

```bash
git clone git@github.com:your-user/project.git
cd project
git remote add upstream git@github.com:original/project.git
```

Sync fork main:

```bash
git fetch upstream
git switch main
git merge --ff-only upstream/main
git push origin main
```

Create contribution branch:

```bash
git switch -c fix/readme-typo
git push -u origin fix/readme-typo
```

### Part J: Protected Branches

Usually you cannot push directly to `main`.

Good:

```text
push branch -> PR -> review -> CI -> merge
```

Bad:

```bash
git push origin main
```

when main is protected or team forbids direct pushes.

---

## 6. What Problem It Solves

- Primary problem solved: safe collaboration with shared remote repositories
- Secondary benefits: code review, CI, protected main, audit trail
- Systems impact: prevents direct unreviewed changes to important branches

---

## 7. When To Use Each Command

| Need | Command |
|---|---|
| See remotes | `git remote -v` |
| Download remote updates | `git fetch origin` |
| Update local main safely | `git pull --ff-only` |
| Push new branch | `git push -u origin <branch>` |
| Check tracking | `git branch -vv` |
| Delete remote branch | `git push origin --delete <branch>` |
| Sync fork | `git fetch upstream` |

---

## 8. When Not To Use It Carelessly

Avoid:

```bash
git pull
```

without knowing whether it merges or rebases.

Prefer explicit:

```bash
git pull --ff-only
git pull --rebase
```

Avoid:

```bash
git push --force
```

Prefer:

```bash
git push --force-with-lease
```

for your own branch after rebase.

---

## 9. Pros and Cons

| Approach | Pros | Cons |
|---|---|---|
| `fetch` first | safe visibility | extra command |
| `pull --ff-only` | avoids surprise merge commits | fails if local branch diverged |
| `pull --rebase` | linear local history | can conflict |
| PR workflow | review and CI | slower than direct push |
| fork flow | safe open-source contribution | more remotes to manage |

---

## 10. Trade-offs and Common Mistakes

### Trade-offs

- Direct push:
  Fast, but unsafe for shared branches.
- PR flow:
  Slower, but reviewable and CI-protected.
- Rebase feature branch:
  Clean history, but requires force-with-lease push.

### Common Mistakes

- Mistake: thinking `origin/main` is always current.
  Better approach: `git fetch origin`.

- Mistake: force pushing shared branch.
  Better approach: coordinate or avoid history rewrite.

- Mistake: pulling into dirty working tree.
  Better approach: commit, stash, or restore first.

- Mistake: confusing fork `origin` and original `upstream`.
  Better approach: check `git remote -v`.

---

## 11. Key Commands

```bash
git remote -v
git fetch origin
git fetch --all --prune
git pull --ff-only
git pull --rebase
git push -u origin <branch>
git branch -vv
git push origin --delete <branch>
```

---

## 12. Failure Modes

### Non-Fast-Forward Push Rejected

Fix:

```bash
git fetch origin
git rebase origin/<branch>
git push
```

If rebased own feature branch:

```bash
git push --force-with-lease
```

### Pulled And Got Unexpected Merge Commit

Cause:

- default `git pull` merged

Prevention:

```bash
git pull --ff-only
```

or configure:

```bash
git config --global pull.ff only
```

### Pushed To Wrong Remote

Check:

```bash
git remote -v
git branch -vv
```

Fix depends on whether branch/commit should be removed from that remote.

---

## 13. Scenario

- Product / system: team backend repo with protected main
- Why this concept fits: all changes go through branch and PR review
- What would go wrong without it: direct pushes can bypass CI and review

---

## 14. Code Sample

Feature branch to PR flow:

```bash
git switch main
git pull --ff-only
git switch -c feature/order-status-api
git add -p
git commit -m "Add order status API"
git push -u origin feature/order-status-api
```

Then open PR on GitHub or with `gh pr create`.

---

## 15. Mini Program / Simulation

Remote state mental model:

```text
git fetch
  updates origin/main locally

git pull
  fetches and integrates into current branch

git push
  sends your local commits to remote branch
```

---

## 16. Practical Question

> Your push is rejected with a non-fast-forward error. What do you do?

---

## 17. Strong Answer

I would not force push immediately. First I would run `git fetch origin` and inspect what changed. If I am on a feature branch, I would rebase or merge the remote branch into my local branch, resolve conflicts, run tests, and push again. If I intentionally rebased my own feature branch, I would use `git push --force-with-lease`, not plain `--force`.

---

## 18. Revision Notes

- One-line summary: remotes share commits; PRs review and protect shared branches.
- Three keywords: fetch, pull, push
- One interview trap: `origin/main` is stale until fetch.
- One memory trick: fetch sees, pull brings, push shares.

---

## 19. Official Source Notes

- Git fetch/pull/push docs: <https://git-scm.com/docs>
- GitHub using Git docs: <https://docs.github.com/en/get-started/using-git>
- GitHub pull request docs: <https://docs.github.com/en/pull-requests>

