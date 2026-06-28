# GitHub CLI Command Mastery Gold Sheet

> Goal: use `gh` confidently for repo work, pull requests, reviews, checks, releases, issues, and automation-friendly workflows.

---

## 0. How To Read This Doc

Git is for version control.

GitHub is for collaboration around that version control.

GitHub CLI, `gh`, lets you operate GitHub from your terminal:

```text
auth
repo
pull request
issue
release
workflow
checks
API
```

This doc focuses on practical commands a backend/devops engineer actually uses on a repo.

---

## 1. Intuition

`git` talks to repositories.

`gh` talks to GitHub.

```text
git commit        -> create local history
git push          -> send history to GitHub
gh pr create      -> ask team to review the pushed branch
gh pr checks      -> see CI result
gh pr merge       -> merge through GitHub rules
gh release create -> publish release metadata/assets
```

Mental model:

```text
local branch -> remote branch -> pull request -> checks/review -> merge -> release
```

---

## 2. Definition

- Definition: GitHub CLI is a command-line tool for managing GitHub repositories, pull requests, issues, releases, workflows, and API operations.
- Category: Developer workflow, collaboration tooling, DevOps productivity.
- Core idea: keep GitHub actions scriptable, repeatable, and terminal-friendly.

---

## 3. Why It Exists

Without `gh`, developers constantly switch between terminal and browser:

- create PR
- check CI
- review comments
- approve PR
- merge PR
- create release
- run workflow
- inspect issue

`gh` makes these workflows faster and scriptable.

For interviews, it also shows you understand the full repo workflow beyond local Git commands.

---

## 4. Reality

Teams use `gh` for:

- creating PRs from feature branches
- checking CI status locally
- checking out a teammate's PR
- reviewing and approving PRs
- merging with squash/rebase/merge strategy
- creating releases
- triggering GitHub Actions workflows
- querying GitHub API from scripts
- automating repository maintenance

Example daily flow:

```bash
git switch -c feature/order-timeout
git add -p
git commit -m "Add order timeout validation"
git push -u origin feature/order-timeout
gh pr create --fill --base main
gh pr checks --watch
```

---

## 5. How It Works

`gh` uses GitHub authentication and API calls.

Typical flow:

```text
1. Authenticate with GitHub.
2. Link local repository to GitHub remote.
3. Use gh commands to create or inspect GitHub objects.
4. Use git commands for local commits and branch movement.
5. Use gh commands for PR, issue, release, and workflow actions.
```

Important distinction:

```text
git push changes code.
gh pr create creates collaboration around code.
```

---

## 6. What Problem It Solves

- Primary problem solved: terminal-based GitHub collaboration.
- Secondary benefits: automation, faster reviews, consistent repo operations.
- Systems impact: smoother CI/CD and better developer productivity.

---

## 7. When To Rely On It

Use `gh` when:

- creating or updating PRs
- checking CI status
- checking out someone else's PR
- merging through GitHub protections
- creating releases
- running GitHub Actions manually
- scripting GitHub operations
- querying GitHub API

---

## 8. When Not To Use It

Do not use `gh` as a replacement for understanding Git.

Examples:

```text
gh pr merge cannot fix a bad commit history by itself.
gh pr checks cannot explain a failing test without logs.
gh repo clone still depends on Git underneath.
```

Also avoid scripting destructive admin operations without dry runs, permission checks, and review.

---

## 9. Pros and Cons

| Pros | Cons |
|---|---|
| Fast terminal workflow | Requires GitHub access/authentication |
| Great for PR and CI workflows | Some org policies may restrict actions |
| Scriptable | Easy to automate the wrong thing |
| Reduces browser switching | Git knowledge still required |
| Useful in CI/devops tasks | Token scopes matter |

---

## 10. Trade-offs and Common Mistakes

### Trade-offs

- `gh` is faster than browser UI for repetitive workflows.
- Browser UI can be clearer for complex review discussions.
- CLI scripts are repeatable but need careful permissions.
- `gh api` is powerful but can bypass guardrails if misused.

### Common Mistakes

Mistake:

```bash
gh pr merge
```

without checking CI/reviews.

Better approach:

```bash
gh pr checks
gh pr view --web
gh pr merge --squash --delete-branch
```

Mistake:

```bash
gh auth login
```

with the wrong GitHub account in a corporate repo.

Better approach:

```bash
gh auth status
gh repo view
```

Mistake:

```bash
gh pr create
```

from an unsynced branch.

Better approach:

```bash
git status -sb
git push -u origin HEAD
gh pr create --fill --base main
```

---

## 11. Key Commands

### Authenticate

```bash
gh auth login
```

Check auth:

```bash
gh auth status
```

Configure Git to use `gh` credentials:

```bash
gh auth setup-git
```

Logout:

```bash
gh auth logout
```

### Repository Commands

Clone:

```bash
gh repo clone owner/repo
```

View current repo:

```bash
gh repo view
```

Open repo in browser:

```bash
gh repo view --web
```

Create repo:

```bash
gh repo create my-service --private --source=. --remote=origin --push
```

Fork repo:

```bash
gh repo fork owner/repo --clone
```

Set default repo when needed:

```bash
gh repo set-default owner/repo
```

### Pull Request Commands

Create PR with inferred title/body:

```bash
gh pr create --fill --base main
```

Create draft PR:

```bash
gh pr create --draft --fill --base main
```

Create PR with explicit title/body:

```bash
gh pr create --base main --title "Add order timeout" --body "Adds validation for stale orders."
```

List PRs:

```bash
gh pr list
```

List your PRs:

```bash
gh pr list --author "@me"
```

View PR:

```bash
gh pr view
```

View specific PR:

```bash
gh pr view 123
```

View PR in browser:

```bash
gh pr view 123 --web
```

Checkout a PR:

```bash
gh pr checkout 123
```

See PR diff:

```bash
gh pr diff 123
```

See PR status:

```bash
gh pr status
```

See checks:

```bash
gh pr checks
```

Watch checks:

```bash
gh pr checks --watch
```

Update PR branch with base branch:

```bash
gh pr update-branch
```

Mark draft PR ready:

```bash
gh pr ready
```

Review PR:

```bash
gh pr review 123 --comment --body "Left a few suggestions."
```

Approve:

```bash
gh pr review 123 --approve
```

Request changes:

```bash
gh pr review 123 --request-changes --body "Please add test coverage for the timeout path."
```

Merge PR with squash:

```bash
gh pr merge 123 --squash --delete-branch
```

Merge PR with merge commit:

```bash
gh pr merge 123 --merge --delete-branch
```

Merge PR with rebase:

```bash
gh pr merge 123 --rebase --delete-branch
```

Auto-merge when checks pass:

```bash
gh pr merge 123 --squash --auto --delete-branch
```

Revert PR:

```bash
gh pr revert 123
```

### Issue Commands

Create issue:

```bash
gh issue create --title "Order timeout bug" --body "Orders are not timing out after 30 minutes."
```

List issues:

```bash
gh issue list
```

List assigned issues:

```bash
gh issue list --assignee "@me"
```

View issue:

```bash
gh issue view 45
```

Comment:

```bash
gh issue comment 45 --body "I can pick this up."
```

Close:

```bash
gh issue close 45 --comment "Fixed in #123."
```

Reopen:

```bash
gh issue reopen 45
```

### Release Commands

List releases:

```bash
gh release list
```

View release:

```bash
gh release view v1.4.0
```

Create release from tag:

```bash
gh release create v1.4.0 --title "v1.4.0" --notes "Adds order timeout validation."
```

Create release with generated notes:

```bash
gh release create v1.4.0 --generate-notes
```

Upload asset:

```bash
gh release upload v1.4.0 build/app.jar
```

Download release:

```bash
gh release download v1.4.0
```

Delete release:

```bash
gh release delete v1.4.0
```

### GitHub Actions Workflow Commands

List workflows:

```bash
gh workflow list
```

View workflow:

```bash
gh workflow view "CI"
```

Run workflow:

```bash
gh workflow run "CI"
```

Run workflow on branch:

```bash
gh workflow run "CI" --ref feature/order-timeout
```

List runs:

```bash
gh run list
```

View run:

```bash
gh run view
```

Watch run:

```bash
gh run watch
```

Download logs:

```bash
gh run download
```

### API Commands

Get repo metadata:

```bash
gh api repos/OWNER/REPO
```

List open PRs:

```bash
gh api repos/OWNER/REPO/pulls
```

Use GraphQL:

```bash
gh api graphql -f query='
{
  viewer {
    login
  }
}'
```

Use API carefully:

```text
gh api can perform privileged operations depending on token scopes.
Always understand the endpoint before scripting mutations.
```

---

## 12. Failure Modes

### Failure Mode 1: Authentication Fails

Symptom:

```text
gh command says authentication required or forbidden.
```

Check:

```bash
gh auth status
```

Fix:

```bash
gh auth login
```

If using multiple accounts, confirm:

```bash
gh repo view
git remote -v
```

### Failure Mode 2: PR Create Fails Because Branch Is Not Pushed

Symptom:

```text
No commits between base and head, or branch not found.
```

Fix:

```bash
git status -sb
git push -u origin HEAD
gh pr create --fill --base main
```

### Failure Mode 3: PR Cannot Merge

Common causes:

- failing checks
- required review missing
- branch behind base
- merge conflicts
- protected branch rules

Commands:

```bash
gh pr checks
gh pr view
gh pr update-branch
```

If conflicts exist:

```bash
git fetch origin
git switch <branch>
git rebase origin/main
```

or:

```bash
git merge origin/main
```

Then resolve, push, and recheck.

### Failure Mode 4: Wrong Repository Context

Symptom:

```text
gh command acts on unexpected repo.
```

Check:

```bash
gh repo view
git remote -v
```

Set default:

```bash
gh repo set-default owner/repo
```

---

## 13. Scenario

### Scenario: Open A PR, Watch CI, Merge Safely

Commands:

```bash
git status -sb
git switch -c feature/order-timeout
git add -p
git commit -m "Add order timeout validation"
git push -u origin HEAD
gh pr create --fill --base main
gh pr checks --watch
gh pr view --web
gh pr merge --squash --delete-branch
```

Why this works:

```text
git handles source history.
gh handles GitHub collaboration.
checks and reviews happen before merge.
squash merge keeps main history compact if that is team policy.
```

---

## 14. Code Sample

### Shell Script: PR Readiness Check

```bash
#!/usr/bin/env bash
set -euo pipefail

git status -sb
git fetch origin
git diff --check
gh pr status
gh pr checks
```

Use:

```text
Run before asking a reviewer to take another look.
```

---

## 15. Mini Program / Simulation

### Python Simulation: Local Git vs GitHub CLI

```python
workflow = [
    ("git", "create commits"),
    ("git", "push branch"),
    ("gh", "create pull request"),
    ("gh", "watch checks"),
    ("gh", "merge pull request"),
    ("git", "sync local main"),
]

for tool, action in workflow:
    print(f"{tool}: {action}")
```

---

## 16. Practical Question

> You finished a feature branch. What commands do you run to open a PR, validate it, and merge it through GitHub?

---

## 17. Strong Answer

I would first confirm my local branch is clean:

```bash
git status -sb
git diff --staged
```

Then push and create the PR:

```bash
git push -u origin HEAD
gh pr create --fill --base main
```

After that I would check CI and review status:

```bash
gh pr checks --watch
gh pr view
```

If the branch is behind or has conflicts, I would update it using team policy, usually rebase for my private feature branch or merge from main if the team prefers merge commits.

When checks and reviews pass:

```bash
gh pr merge --squash --delete-branch
```

The merge strategy depends on repository policy.

---

## 18. Revision Notes

- One-line summary: `git` changes repository history; `gh` manages GitHub collaboration around that history.
- Three keywords: PR, checks, review.
- One interview trap: trying to fix Git history problems using GitHub CLI without understanding local branch state.
- One memory trick: "Commit with git, collaborate with gh."

---

## 19. Command Recipes

### Clone And Start Work

```bash
gh repo clone owner/repo
cd repo
git switch -c feature/name
```

### Checkout A Teammate's PR

```bash
gh pr checkout 123
git status -sb
```

### Review A PR Locally

```bash
gh pr checkout 123
git diff origin/main...HEAD
gh pr review 123 --comment --body "Reviewed locally."
```

### Merge After Checks Pass

```bash
gh pr checks --watch
gh pr merge --squash --delete-branch
```

### Release From Tag

```bash
git tag -a v1.4.0 -m "Release v1.4.0"
git push origin v1.4.0
gh release create v1.4.0 --generate-notes
```

### Trigger A Manual Deployment Workflow

```bash
gh workflow list
gh workflow run "Deploy" --ref main
gh run watch
```

---

## 20. Official Source Notes

- GitHub CLI command groups such as `auth`, `repo`, `pr`, `issue`, `release`, `workflow`, `run`, and `api` are documented in the official CLI manual: <https://cli.github.com/manual/>
- Pull request collaboration concepts are documented in GitHub's pull request docs: <https://docs.github.com/en/pull-requests>

