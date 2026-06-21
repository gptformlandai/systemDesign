# Git GitHub Mock Interview Scripts

> Track: Git and GitHub Command Mastery - Practice Upgrade  
> Goal: rehearse Git/GitHub answers like real senior engineering interviews.

---

## 1. How To Run A Mock

Rules:

1. Timebox the round.
2. Answer with commands plus safety boundaries.
3. Explain what each command changes.
4. Always inspect before destructive operations.
5. Score immediately with the rubric.

Default answer format:

```text
inspect -> decide local/shared/protected -> command -> state change -> verify -> recovery/prevention
```

---

## 2. Mock 1: Git Foundations

Time: 30 minutes.

### Opening

```text
Explain Git's working tree, staging area, and commit history.
```

Expected points:

- working tree is editable files
- index/staging area is next commit snapshot
- commit history is immutable commit graph
- `git add` changes index
- `git commit` creates commit and moves branch ref
- `git diff` vs `git diff --staged`

### Follow-ups

1. How do you commit only part of a file?
2. How do you unstage a file?
3. How do you discard one file's changes?
4. What is HEAD?
5. What does `git status` tell you?

---

## 3. Mock 2: Branching Merge Rebase Conflicts

Time: 45 minutes.

### Opening

```text
Explain merge vs rebase and when you would use each.
```

Expected points:

- merge combines histories and may create merge commit
- rebase copies commits onto new base
- rebase changes commit hashes
- local/private rebase is usually fine
- shared branch rebase is risky
- conflict resolution steps

### Follow-ups

1. What is a fast-forward merge?
2. What is a merge base?
3. How do you abort a merge?
4. How do you continue a rebase after conflict?
5. Why does rebase make PR history cleaner?

---

## 4. Mock 3: Undo Recovery Safety

Time: 45 minutes.

### Opening

```text
You made a bad commit. Walk through how you decide between reset, revert, restore, and reflog.
```

Expected points:

- local vs pushed
- shared/protected branch
- preserve history
- `reset` for local pointer movement
- `revert` for shared/pushed history
- `restore` for file/index changes
- reflog for local recovery

### Follow-ups

1. What does `reset --soft` change?
2. What does `reset --mixed` change?
3. What does `reset --hard` change?
4. How do you recover a lost commit?
5. Why is reflog not a backup on GitHub?

---

## 5. Mock 4: Remote Collaboration PR Flow

Time: 45 minutes.

### Opening

```text
Describe a clean feature branch to PR workflow using Git and GitHub.
```

Expected points:

- fetch latest main
- create branch
- commit logical changes
- push with upstream
- open PR
- CI and review
- update branch with main
- merge according to policy
- delete branch after merge

### Follow-ups

1. Fetch vs pull?
2. What is an upstream branch?
3. What causes non-fast-forward rejection?
4. When use `--force-with-lease`?
5. How do you review a teammate PR locally?

---

## 6. Mock 5: Inspection Debugging History

Time: 45 minutes.

### Opening

```text
A bug appeared last week. How do you find the commit that introduced it?
```

Expected points:

- reproduce bug
- identify known good and bad commits
- use `git bisect`
- run deterministic test
- confirm culprit
- inspect diff and PR context

### Follow-ups

1. How do you inspect commits only on your branch?
2. Two-dot vs three-dot diff?
3. When use blame?
4. What does range-diff show?
5. How do you inspect a file from old commit?

---

## 7. Mock 6: Git Internals

Time: 45 minutes.

### Opening

```text
Explain what Git stores internally when you make a commit.
```

Expected points:

- blob/tree/commit objects
- commit points to tree and parent(s)
- branch ref moves
- HEAD points to branch or commit
- index provides next snapshot
- commit hash depends on content/metadata/parent

### Follow-ups

1. Why does rebase change hashes?
2. What is detached HEAD?
3. What does fetch update internally?
4. What are packfiles?
5. How can garbage collection affect unreachable commits?

---

## 8. Mock 7: GitHub CLI And PR Operations

Time: 35 minutes.

### Opening

```text
Use GitHub CLI to create, inspect, and merge a PR.
```

Expected commands:

```bash
gh pr create --fill
gh pr checks
gh pr view
gh pr checkout <number>
gh pr review --approve
gh pr merge --squash
```

Expected explanation:

- `git` changes repository history
- `gh` manages GitHub collaboration around that history
- auth and token scopes matter
- admin scripts need dry-run/review

---

## 9. Mock 8: GitHub Governance Security

Time: 60 minutes.

### Opening

```text
Design GitHub governance for a 200-engineer organization.
```

Expected points:

- org/team permissions
- branch protection or rulesets
- PR-required flow
- required checks
- CODEOWNERS
- review policy
- merge strategy
- merge queue for busy repos
- secret scanning and push protection
- least-privilege tokens/GitHub Apps
- audit trail

### Follow-ups

1. Why protect `.github/workflows`?
2. Why is CODEOWNERS not enough by itself?
3. How do you respond to a committed secret?
4. What should be audited?
5. How should bot permissions be handled?

---

## 10. Mock 9: Release Engineering

Time: 60 minutes.

### Opening

```text
Design a Git/GitHub release process for a backend service with hotfix and backport needs.
```

Expected points:

- trunk-based or release branch strategy
- annotated/signed tags
- semantic versioning
- protected release branches
- GitHub releases/changelog
- hotfix branches
- backport with cherry-pick
- release artifact traceability
- rollback vs revert vs roll forward

### Follow-ups

1. What if a release tag is wrong?
2. How do you know what shipped?
3. How do you patch an old release branch?
4. Why reconcile hotfixes back to main?
5. What metadata should a release record include?

---

## 11. Mock 10: MAANG Capstone

Time: 75 minutes.

### Prompt

```text
You join a company where Git history is messy, main breaks often, release tags move, direct pushes happen, secrets were committed, and PR checks are flaky. Design a Git/GitHub operating model.
```

Strong answer includes:

1. Immediate risk triage.
2. Branch strategy.
3. PR review policy.
4. Branch protection/rulesets.
5. CODEOWNERS for sensitive paths.
6. Required and reliable checks.
7. Merge strategy and merge queue if needed.
8. Secret incident process.
9. Token and permission cleanup.
10. Release tagging and artifact traceability.
11. Hotfix/backport workflow.
12. Audit and training.

### Staff-Level Follow-ups

1. How do you migrate without blocking all engineers?
2. What do you make required first?
3. How do you handle emergency bypass?
4. How do you measure success after 30 days?
5. What do you automate vs document?

---

## 12. Self Review Questions

After each mock, ask:

1. Did I inspect before changing state?
2. Did I identify local vs shared history?
3. Did I explain what command changes?
4. Did I avoid dangerous shortcuts?
5. Did I include verification commands?
6. Did I include recovery/prevention?
7. Did I cover governance when many engineers are involved?
8. Did I cover release traceability when production is involved?

---

## 13. Completion Gate

You are mock-ready when:

1. Foundations answer fits in 5 minutes.
2. Recovery answer avoids unsafe reset/force push claims.
3. Internals answer explains refs, HEAD, index, and objects.
4. Governance answer includes branch protection, CODEOWNERS, checks, permissions, and audit.
5. Release answer includes tags, changelog, hotfix/backport, and rollback strategy.
6. Capstone answer is structured and calm under pressure.
