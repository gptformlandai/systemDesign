# Git and GitHub Command Mastery Interview Track Index

> Goal: become confident working inside real repositories using Git and GitHub commands: daily workflow, branching, PRs, conflicts, recovery, history inspection, debugging, and production scenarios.

---

## How To Use This Track

This track is command-first.

Every topic answers:

```text
What do I type?
Why do I type it?
What does it change?
What can go wrong?
How do I recover?
What would I say in an interview?
```

The core mental model:

```text
working tree
-> staging area
-> local commit history
-> remote repository
-> pull request
-> protected main branch
```

---

## Study Order

| Order | Document | Why It Exists |
|---|---|---|
| 1 | [Local Git Foundations and Daily Commands](Git-Local-Foundations-Daily-Commands-Gold-Sheet.md) | `config`, `init`, `clone`, `status`, `add`, `commit`, `diff`, `log`, `show`, `restore` |
| 2 | [Branching, Merging, Rebasing, and Conflicts](Git-Branching-Merging-Rebasing-Conflicts-Gold-Sheet.md) | `branch`, `switch`, `merge`, `rebase`, conflict resolution, `cherry-pick`, `revert` |
| 3 | [Remote Collaboration and GitHub Flow](Git-Remote-Collaboration-GitHub-Flow-Gold-Sheet.md) | `remote`, `fetch`, `pull`, `push`, upstreams, forks, PR workflow, non-fast-forward fixes |
| 4 | [Undo, Recovery, and Safety Commands](Git-Undo-Recovery-Safety-Commands-Gold-Sheet.md) | `restore`, `reset`, `revert`, `reflog`, `stash`, `clean`, `commit --amend`, `force-with-lease` |
| 5 | [Inspection, Debugging, and History Pro Commands](Git-Inspection-Debugging-History-Pro-Commands-Gold-Sheet.md) | `log`, `show`, `diff`, `blame`, `bisect`, `grep`, `tag`, `describe`, `range-diff` |
| 6 | [GitHub CLI Command Mastery](GitHub-CLI-Command-Mastery-Gold-Sheet.md) | `gh auth`, `gh repo`, `gh pr`, `gh issue`, `gh release`, checks, reviews, merge commands |
| 7 | [Advanced Repository Workflows](Git-Advanced-Repository-Workflows-Gold-Sheet.md) | `worktree`, `submodule`, `sparse-checkout`, hooks, `.gitignore`, attributes, LFS, aliases |
| 8 | [Production and Interview Scenario Playbook](Git-GitHub-Production-Interview-Scenario-Playbook.md) | wrong branch, bad merge, lost commit, conflict, force push, hotfix, release, protected branch scenarios |
| 9 | [Golden Command Cheat Sheet](Git-GitHub-Golden-Command-Cheat-Sheet.md) | fast command recipes for daily use and interview revision |

---

## Learning Levels

### Beginner

You should become comfortable with:

- `git status`
- `git add`
- `git commit`
- `git diff`
- `git log`
- `git switch`
- `git pull`
- `git push`
- `git restore`

### Intermediate

You should confidently handle:

- feature branches
- merge conflicts
- rebasing local work
- stashing changes
- undoing commits safely
- pushing branches
- creating pull requests
- syncing with main

### Pro / Interview Level

You should be able to explain and use:

- `reflog` recovery
- `reset` vs `revert`
- `merge` vs `rebase`
- `fetch` vs `pull`
- `force --force-with-lease`
- `cherry-pick`
- `bisect`
- `worktree`
- fork/upstream flow
- protected branches and PR review flow

---

## Master Map

```text
Local work:
  status
  add
  commit
  diff
  restore
  log

Branch work:
  branch
  switch
  merge
  rebase
  conflict resolution

Remote work:
  remote
  fetch
  pull
  push
  upstream tracking
  fork/upstream origin

Recovery work:
  restore
  reset
  revert
  reflog
  stash
  clean

Investigation work:
  log
  show
  diff
  blame
  bisect
  grep
  range-diff

GitHub work:
  gh auth
  gh repo
  gh pr
  gh issue
  gh release
  gh workflow
```

---

## Interview Rule

Never say "I will reset it" without saying:

- local or pushed?
- shared branch or private branch?
- do we want to preserve history?
- should we use `revert` instead?
- do we need `--force-with-lease`?

Safe wording:

> If the commit is already pushed to a shared branch, I prefer `git revert` because it preserves history. If it is only local, I can use `git reset` depending on whether I want to keep changes staged, unstaged, or discard them.

---

## Official Source Notes

- Git command reference: <https://git-scm.com/docs>
- GitHub using Git docs: <https://docs.github.com/en/get-started/using-git>
- GitHub pull request docs: <https://docs.github.com/en/pull-requests>
- GitHub CLI manual: <https://cli.github.com/manual/>

