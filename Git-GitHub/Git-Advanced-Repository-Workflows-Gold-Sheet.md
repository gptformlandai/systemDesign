# Git Advanced Repository Workflows Gold Sheet

> Goal: handle mature repository workflows: hotfixes, monorepos, submodules, hooks, large files, attributes, aliases, and multi-branch work without chaos.

---

## 0. How To Read This Doc

This document is for real-world repository complexity:

```text
I need to work on two branches at once.
This repo is huge.
This repo has submodules.
Generated files keep polluting my status.
Line endings keep changing.
Large binary files are bloating Git.
I want safer local automation.
I want command aliases that make Git easier.
```

These are not day-one Git commands, but they are production-level skills.

---

## 1. Intuition

Advanced Git workflows are repo ergonomics.

The core commands:

```text
worktree        -> multiple checkouts of one repo
submodule       -> repo inside repo
sparse-checkout -> checkout only part of a repo
hooks           -> run scripts around Git actions
.gitignore      -> ignore untracked noise
.gitattributes  -> normalize file handling
LFS             -> store large files outside normal Git objects
aliases         -> shorten repeated commands
rerere          -> remember conflict resolutions
```

---

## 2. Definition

- Definition: Advanced Git workflows are commands and repository configuration techniques that help teams scale Git usage across large, complex, or highly collaborative repositories.
- Category: Repository operations, developer productivity, monorepo workflows, release engineering.
- Core idea: make complex repo work predictable and repeatable.

---

## 3. Why It Exists

Simple Git commands are enough for small projects.

Large production repos introduce extra problems:

- multiple active release branches
- urgent hotfix while feature work is uncommitted
- huge monorepo checkout size
- generated files
- OS-specific line endings
- large binaries
- shared dependencies across repositories
- repeated conflict resolution
- local quality checks before commit

Advanced workflows solve these operational problems.

---

## 4. Reality

You will see these in:

- platform teams
- backend monorepos
- mobile repos with huge assets
- infra repos
- microservice repos with shared libraries
- game/media repos with large files
- organizations with strict pre-commit policies
- release teams managing hotfix branches

Senior-level signal:

```text
You know when to use these tools, and you also know when they make life worse.
```

---

## 5. How It Works

Advanced Git features usually add one of these:

```text
extra checkout      -> worktree
extra nested repo   -> submodule
partial checkout    -> sparse-checkout
local automation    -> hooks
file rules          -> .gitignore and .gitattributes
large file pointers -> Git LFS
command shortcuts   -> aliases
conflict memory     -> rerere
```

They are powerful because they change developer workflow, not just a single commit.

---

## 6. What Problem It Solves

- Primary problem solved: scaling Git workflow for complex repositories.
- Secondary benefits: less context switching, fewer local mistakes, faster large-repo operations.
- Systems impact: better release operations, cleaner commits, less build noise.

---

## 7. When To Rely On It

Use these workflows when:

- you need to patch production while keeping feature work untouched
- repo checkout is too large
- generated files keep appearing
- sub-repositories are part of the build
- binary assets are too large for normal Git
- conflict resolutions repeat often
- your team wants local checks before commit/push

---

## 8. When Not To Use It

Avoid advanced workflows when they add more process than value.

Examples:

- Do not use submodules when a package manager dependency is enough.
- Do not use sparse checkout for a small repo.
- Do not use hooks as the only enforcement mechanism; CI must still validate.
- Do not use LFS for normal source files.
- Do not create too many aliases that teammates cannot understand.

---

## 9. Pros and Cons

| Feature | Pros | Cons |
|---|---|---|
| `worktree` | Work on multiple branches simultaneously | Extra directories to manage |
| `submodule` | Pin dependency repo to exact commit | Easy to forget update/init commands |
| `sparse-checkout` | Faster monorepo workflow | Some tooling expects full repo |
| hooks | Catch issues early | Local hooks can be bypassed |
| `.gitattributes` | Consistent file handling | Misconfiguration causes churn |
| LFS | Better large binary storage | Requires LFS support and quotas |
| aliases | Faster commands | Can hide real Git behavior |
| rerere | Reuses conflict resolutions | Can reuse wrong resolution if careless |

---

## 10. Trade-offs and Common Mistakes

### Trade-offs

- Worktrees reduce stash pressure but create multiple working directories.
- Submodules give exact dependency versions but add workflow complexity.
- Sparse checkout saves time but can confuse scripts that assume full files.
- Hooks improve local feedback but are not security boundaries.
- LFS reduces Git object bloat but requires separate infrastructure.

### Common Mistakes

Mistake:

```bash
git submodule update
```

without initialization.

Better approach:

```bash
git submodule update --init --recursive
```

Mistake:

```text
Trusting pre-commit hooks as the only quality gate.
```

Better approach:

```text
Use hooks for fast feedback and CI for enforcement.
```

Mistake:

```text
Using worktree but deleting directories manually without pruning.
```

Better approach:

```bash
git worktree list
git worktree remove ../repo-hotfix
git worktree prune
```

---

## 11. Worktree Commands

### List Worktrees

```bash
git worktree list
```

### Create Worktree For Existing Branch

```bash
git worktree add ../myrepo-hotfix hotfix/payment-timeout
```

### Create Worktree And New Branch

```bash
git worktree add -b hotfix/payment-timeout ../myrepo-hotfix origin/main
```

### Remove Worktree

```bash
git worktree remove ../myrepo-hotfix
```

### Prune Stale Worktree Metadata

```bash
git worktree prune
```

### Real Hotfix Flow

You are in the middle of feature work:

```bash
git status -sb
```

Create separate hotfix checkout:

```bash
git fetch origin
git worktree add -b hotfix/payment-timeout ../service-hotfix origin/main
cd ../service-hotfix
```

Fix, commit, push:

```bash
git add -p
git commit -m "Fix payment timeout regression"
git push -u origin hotfix/payment-timeout
gh pr create --fill --base main
```

Why this is strong:

```text
No stash needed.
Feature branch stays untouched.
Hotfix is isolated.
```

---

## 12. Submodule Commands

### Clone With Submodules

```bash
git clone --recurse-submodules <repo-url>
```

### Initialize Existing Submodules

```bash
git submodule update --init --recursive
```

### Add Submodule

```bash
git submodule add <repo-url> libs/shared-contracts
git commit -m "Add shared contracts submodule"
```

### Update Submodule To Recorded Commit

```bash
git submodule update --recursive
```

### Pull Latest Inside Submodule

```bash
cd libs/shared-contracts
git fetch origin
git switch main
git pull --ff-only
cd ../..
git add libs/shared-contracts
git commit -m "Update shared contracts submodule"
```

Important:

```text
The parent repo records the submodule commit SHA.
Updating a submodule means committing the new pointer in the parent repo.
```

### Remove Submodule

Modern flow:

```bash
git submodule deinit -f libs/shared-contracts
git rm -f libs/shared-contracts
git commit -m "Remove shared contracts submodule"
```

---

## 13. Sparse Checkout Commands

### Enable Sparse Checkout

```bash
git sparse-checkout init --cone
```

### Checkout Only Specific Paths

```bash
git sparse-checkout set services/order-service libs/common
```

### Add More Paths

```bash
git sparse-checkout add services/payment-service
```

### Disable Sparse Checkout

```bash
git sparse-checkout disable
```

### Monorepo Flow

```bash
git clone <repo-url> platform
cd platform
git sparse-checkout init --cone
git sparse-checkout set services/order-service
```

Use when:

```text
The repo is huge and you only need a subdirectory.
```

---

## 14. Hooks

Hooks are scripts that run at Git lifecycle points.

Common local hooks:

```text
pre-commit     -> before commit is created
commit-msg     -> validate commit message
pre-push       -> before pushing
post-checkout  -> after checkout
```

Hook location:

```text
.git/hooks/
```

Example pre-commit hook:

```bash
#!/usr/bin/env bash
set -euo pipefail

mvn -q test
git diff --check
```

Make executable:

```bash
chmod +x .git/hooks/pre-commit
```

Important:

```text
Local hooks are not automatically shared through normal Git clone.
Teams usually use tools like pre-commit frameworks, build scripts, or CI for enforcement.
```

Interview answer:

```text
I use hooks for fast local feedback, but CI remains the source of truth because local hooks can be skipped.
```

---

## 15. .gitignore

`.gitignore` prevents untracked files from appearing in `git status`.

Examples:

```gitignore
target/
build/
.gradle/
.idea/
*.log
.env
```

Check why a file is ignored:

```bash
git check-ignore -v <file>
```

Important:

```text
.gitignore does not remove files already tracked by Git.
```

Stop tracking a file but keep local copy:

```bash
git rm --cached <file>
git commit -m "Stop tracking generated file"
```

---

## 16. .gitattributes

`.gitattributes` controls how Git treats files.

Common use cases:

- line endings
- binary file detection
- diff strategy
- merge strategy
- LFS tracking

Example:

```gitattributes
* text=auto
*.sh text eol=lf
*.bat text eol=crlf
*.png binary
*.jar binary
```

Why it matters:

```text
Teams using Windows, macOS, and Linux can avoid noisy line-ending diffs.
```

Renormalize after attributes change:

```bash
git add --renormalize .
git commit -m "Normalize line endings"
```

---

## 17. Git LFS

Git LFS stores large files as lightweight pointers in Git and real content outside normal Git objects.

Install/enable:

```bash
git lfs install
```

Track file type:

```bash
git lfs track "*.psd"
git lfs track "*.zip"
```

Commit tracking rules:

```bash
git add .gitattributes
git commit -m "Track design assets with Git LFS"
```

Add large file:

```bash
git add assets/mockup.psd
git commit -m "Add checkout mockup asset"
git push
```

Check tracked LFS files:

```bash
git lfs ls-files
```

When not to use:

```text
Do not use LFS for normal source code, configs, or small text files.
```

---

## 18. Git Aliases

Aliases reduce typing for commands you truly understand.

Useful aliases:

```bash
git config --global alias.st "status -sb"
git config --global alias.co "switch"
git config --global alias.br "branch"
git config --global alias.cm "commit -m"
git config --global alias.lg "log --oneline --decorate --graph --all"
```

Use:

```bash
git st
git lg
```

Do not make aliases that hide destructive commands.

Bad idea:

```bash
git config --global alias.nuke "reset --hard"
```

---

## 19. Rerere

`rerere` means reuse recorded resolution.

Enable:

```bash
git config --global rerere.enabled true
```

Use case:

```text
You repeatedly rebase a long-running branch and keep resolving the same conflict.
Git can remember your previous conflict resolution.
```

Check recorded resolutions:

```bash
git rerere status
```

Forget a bad recorded resolution:

```bash
git rerere forget <file>
```

Warning:

```text
Still inspect conflict resolutions. Reusing a resolution blindly can preserve an old mistake.
```

---

## 20. Failure Modes

### Failure Mode 1: Worktree Branch Already Checked Out

Symptom:

```text
Git says branch is already checked out in another worktree.
```

Fix:

```bash
git worktree list
```

Switch to another branch or create a new branch:

```bash
git worktree add -b hotfix/new ../repo-hotfix origin/main
```

### Failure Mode 2: Submodule Directory Is Empty

Fix:

```bash
git submodule update --init --recursive
```

### Failure Mode 3: Sparse Checkout Missing Files Needed By Build

Fix:

```bash
git sparse-checkout add <needed-path>
```

or disable:

```bash
git sparse-checkout disable
```

### Failure Mode 4: Line Endings Create Huge Diff

Check:

```bash
git diff --check
git status -sb
```

Fix with `.gitattributes`, then:

```bash
git add --renormalize .
```

### Failure Mode 5: Large File Accidentally Committed Without LFS

If local only:

```bash
git reset --mixed HEAD~1
git lfs track "*.zip"
git add .gitattributes
git add <large-file>
git commit -m "Track large asset with LFS"
```

If already pushed:

```text
Coordinate with the team before rewriting history.
Large file cleanup in shared history is disruptive.
```

---

## 21. Scenario

### Scenario: Emergency Hotfix During Unfinished Feature Work

You are halfway through feature work:

```bash
git status -sb
```

Instead of stashing, create a clean hotfix worktree:

```bash
git fetch origin
git worktree add -b hotfix/login-timeout ../app-hotfix origin/main
cd ../app-hotfix
```

Make fix:

```bash
git add -p
git commit -m "Fix login timeout regression"
git push -u origin hotfix/login-timeout
gh pr create --fill --base main
```

After merge:

```bash
cd ../app
git fetch origin
git worktree remove ../app-hotfix
git worktree prune
```

---

## 22. Code Sample

### Shell Script: Safe Repo Cleanup

```bash
#!/usr/bin/env bash
set -euo pipefail

git status -sb
git clean -n

echo "If the dry run only shows generated files, run:"
echo "git clean -fd"
```

This script intentionally does not run `git clean -fd` automatically.

---

## 23. Mini Program / Simulation

### Python Simulation: Worktree Benefit

```python
current_repo = {
    "branch": "feature/checkout-redesign",
    "dirty_files": ["CheckoutController.java", "CheckoutServiceTest.java"],
}

hotfix_worktree = {
    "branch": "hotfix/payment-timeout",
    "dirty_files": [],
}

print("Feature repo:", current_repo)
print("Hotfix repo:", hotfix_worktree)
print("Result: hotfix can happen without stashing feature work")
```

---

## 24. Practical Question

> You are in the middle of a large uncommitted feature, and production needs a hotfix from `main`. What do you do?

---

## 25. Strong Answer

I would avoid disturbing my dirty feature branch. I would create a separate worktree from `origin/main`:

```bash
git fetch origin
git worktree add -b hotfix/prod-issue ../repo-hotfix origin/main
cd ../repo-hotfix
```

Then I would make the fix, test it, push, and open a PR:

```bash
git add -p
git commit -m "Fix production issue"
git push -u origin hotfix/prod-issue
gh pr create --fill --base main
```

This keeps the hotfix isolated from unfinished work and avoids risky stash/reset commands. After merge, I would remove the worktree:

```bash
git worktree remove ../repo-hotfix
git worktree prune
```

---

## 26. Revision Notes

- One-line summary: advanced workflows make complex repo operations safer and faster.
- Three keywords: worktree, sparse, hooks.
- One interview trap: relying on local hooks as the only enforcement mechanism.
- One memory trick: "Worktree for parallel work, sparse for huge repos, attributes for file rules."

---

## 27. Official Source Notes

- Git advanced commands such as `worktree`, `submodule`, `sparse-checkout`, `clean`, `rerere`, and configuration commands are documented in the official Git command reference: <https://git-scm.com/docs>
- GitHub CLI commands used in PR and workflow examples are documented in the official CLI manual: <https://cli.github.com/manual/>

