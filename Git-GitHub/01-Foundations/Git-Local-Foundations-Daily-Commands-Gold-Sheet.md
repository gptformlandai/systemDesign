# Git Local Foundations and Daily Commands Gold Sheet

> Goal: master the local Git commands you use every day inside a repository.

---

## 0. How To Read This

Beginner focus:

- `git status`
- `git add`
- `git commit`
- `git diff`
- `git log`

Intermediate focus:

- staging intentionally
- partial staging
- commit messages
- restore files
- inspect commits

Pro focus:

- clean small commits
- review before commit
- avoid accidental files
- explain working tree vs staging area vs commit history

---

# Topic 1: Local Git Foundations and Daily Commands

---

## 1. Intuition

Git has three local zones:

```text
working tree -> staging area -> commit history
```

- working tree: files you are editing
- staging area: changes selected for the next commit
- commit history: saved snapshots

Beginner explanation:

You edit files, stage the exact changes you want, then commit them as a saved checkpoint.

---

## 2. Definition

- Definition: Local Git commands manage files, staged changes, and commits on your machine before sharing with a remote.
- Category: Version control fundamentals
- Core idea: control exactly what becomes part of history.

---

## 3. Why It Exists

Without local Git discipline:

- accidental files get committed
- commits become huge and messy
- hard-to-review changes reach PRs
- debugging history becomes painful
- recovery becomes risky

Local Git commands let you work safely before pushing.

---

## 4. Reality

Daily repo workflow:

```bash
git status
git switch -c feature/order-validation
git diff
git add .
git commit -m "Validate order total"
git push -u origin feature/order-validation
```

But the pro version is:

```bash
git status
git diff
git diff --staged
git add -p
git commit
```

You review before you save history.

---

## 5. How It Works

### Part A: Configure Git

Check config:

```bash
git config --list
```

Set identity:

```bash
git config --global user.name "Your Name"
git config --global user.email "you@example.com"
```

Set default branch name:

```bash
git config --global init.defaultBranch main
```

Set default editor:

```bash
git config --global core.editor "code --wait"
```

### Part B: Create Or Clone A Repo

New repo:

```bash
git init
```

Clone existing repo:

```bash
git clone git@github.com:org/repo.git
```

Clone into a folder:

```bash
git clone git@github.com:org/repo.git my-folder
```

### Part C: Check Status

Most important command:

```bash
git status
```

Short status:

```bash
git status -sb
```

Meaning:

```text
modified = changed in working tree
staged = selected for next commit
untracked = Git does not know this file yet
```

### Part D: See Changes

Unstaged changes:

```bash
git diff
```

Staged changes:

```bash
git diff --staged
```

Compare file:

```bash
git diff -- src/App.java
```

Word-level diff:

```bash
git diff --word-diff
```

### Part E: Stage Changes

Stage one file:

```bash
git add src/OrderService.java
```

Stage all tracked and untracked changes:

```bash
git add .
```

Stage interactively:

```bash
git add -p
```

Pro rule:

> Use `git add -p` when one file contains changes for multiple logical commits.

### Part F: Commit Changes

Commit staged changes:

```bash
git commit -m "Validate order total"
```

Open editor for detailed message:

```bash
git commit
```

Commit all tracked modified files:

```bash
git commit -am "Fix order validation"
```

Important:

`git commit -am` does not add new untracked files.

### Part G: Good Commit Message

Good:

```text
Validate order total before payment
```

Weak:

```text
fix
changes
done
```

Pro shape:

```text
Short imperative summary

Why this change is needed.
Any important behavior or migration note.
```

### Part H: Inspect History

Compact log:

```bash
git log --oneline
```

Graph log:

```bash
git log --oneline --graph --decorate --all
```

Last 5 commits:

```bash
git log --oneline -5
```

Show one commit:

```bash
git show <commit-sha>
```

Show file at commit:

```bash
git show <commit-sha>:path/to/file
```

### Part I: Restore Files

Discard unstaged changes in one file:

```bash
git restore path/to/file
```

Unstage file but keep changes:

```bash
git restore --staged path/to/file
```

Restore file from another commit:

```bash
git restore --source <commit-sha> -- path/to/file
```

### Part J: Remove Or Move Files

Remove tracked file:

```bash
git rm old-file.txt
```

Stop tracking but keep local file:

```bash
git rm --cached secret.env
```

Move/rename:

```bash
git mv old-name.java new-name.java
```

### Part K: Ignore Files

Create `.gitignore`:

```gitignore
target/
node_modules/
.env
*.log
```

Check why file is ignored:

```bash
git check-ignore -v path/to/file
```

---

## 6. What Problem It Solves

- Primary problem solved: safe local change tracking
- Secondary benefits: clean commits, easier PR review, simpler recovery
- Systems impact: keeps repository history understandable

---

## 7. When To Use Each Command

| Need | Command |
|---|---|
| See repo state | `git status -sb` |
| See unstaged changes | `git diff` |
| See staged changes | `git diff --staged` |
| Stage file | `git add <file>` |
| Stage pieces | `git add -p` |
| Save checkpoint | `git commit` |
| Inspect history | `git log --oneline --graph --decorate --all` |
| Discard file edits | `git restore <file>` |
| Unstage file | `git restore --staged <file>` |

---

## 8. When Not To Use It Carelessly

Be careful with:

```bash
git add .
git restore .
git rm -r .
```

Why:

- can stage unrelated files
- can discard important local work
- can remove too much

Pro habit:

```bash
git status
git diff
git add -p
git diff --staged
git commit
```

---

## 9. Pros and Cons

| Command Habit | Pros | Cons |
|---|---|---|
| `git add .` | fast | can stage unrelated files |
| `git add -p` | precise commits | slower |
| small commits | easy review/revert | needs discipline |
| detailed commit messages | useful history | takes more time |
| `git status -sb` often | avoids surprises | none really |

---

## 10. Trade-offs and Common Mistakes

### Trade-offs

- Fast committing:
  Quick progress, but messy history.
- Careful staging:
  Cleaner commits, but slower.
- Tiny commits:
  Easier review, but too many trivial commits can be noisy.

### Common Mistakes

- Mistake: committing secrets.
  Better approach: `.gitignore`, secret scanning, `git rm --cached`.

- Mistake: committing generated files accidentally.
  Better approach: check `git status` and `.gitignore`.

- Mistake: using `git commit -am` for new files.
  Why wrong: it skips untracked files.
  Better approach: `git add <new-file>` first.

- Mistake: not reviewing staged changes.
  Better approach: `git diff --staged`.

---

## 11. Key Commands

```bash
git status -sb
git diff
git diff --staged
git add <file>
git add -p
git commit -m "Message"
git log --oneline --graph --decorate --all
git show <sha>
git restore <file>
git restore --staged <file>
```

---

## 12. Failure Modes

### Accidentally Staged Too Much

Fix:

```bash
git restore --staged .
git add -p
```

### Committed Wrong File Locally

If commit is not pushed:

```bash
git reset --soft HEAD~1
git restore --staged wrong-file
git commit
```

### Untracked File Not Committed

Check:

```bash
git status
git add new-file
git commit --amend
```

### Secret Added To Git

If staged but not committed:

```bash
git restore --staged .env
echo ".env" >> .gitignore
```

If already committed and pushed, rotate the secret. Removing it from the latest commit is not enough.

---

## 13. Scenario

- Product / system: backend feature branch
- Why this concept fits: you need clean local commits before opening a PR
- What would go wrong without it: unrelated files, generated output, and weak commits clutter the review

---

## 14. Code Sample

Clean local commit flow:

```bash
git status -sb
git diff
git add -p
git diff --staged
git commit -m "Validate order total before payment"
git log --oneline -3
```

---

## 15. Mini Program / Simulation

Mental model:

```text
edit file
  -> working tree changed
git add file
  -> staged for commit
git commit
  -> saved in history
git push
  -> shared with remote
```

---

## 16. Practical Question

> You edited five files but only two belong to the current bug fix. How do you commit cleanly?

---

## 17. Strong Answer

I would inspect the changes with `git status` and `git diff`, then stage only the relevant hunks using `git add -p` or specific files. Before committing, I would run `git diff --staged` to verify the commit contains only the bug fix. Then I would commit with a clear message.

---

## 18. Revision Notes

- One-line summary: local Git is working tree, staging area, and commit history.
- Three keywords: status, add, commit
- One interview trap: `git commit -am` does not add new files.
- One memory trick: check, stage, verify, commit.

---

## 19. Official Source Notes

- Git reference: <https://git-scm.com/docs>
- GitHub using Git: <https://docs.github.com/en/get-started/using-git>

