# Git GitHub Interview Scoring Rubrics

> Track: Git and GitHub Command Mastery - Practice Upgrade  
> Goal: make Git/GitHub readiness measurable.

Use this after every scenario, lab, or mock.

---

## 1. Score Scale

| Score | Meaning |
|---|---|
| 1 | unsafe, fragmented, command memorization only |
| 2 | basic commands known but weak safety boundaries |
| 3 | solid daily workflow with some senior gaps |
| 4 | strong senior answer with state model, verification, and recovery |
| 5 | MAANG-level answer with internals, governance, release, and production judgment |

Target:

- junior/mid: mostly 3s
- senior: mostly 4s
- MAANG/platform: consistent 4s with multiple 5s

---

## 2. Universal Rubric

| Dimension | 1 | 3 | 5 |
|---|---|---|---|
| State model | vague commands | explains working tree/index/HEAD | explains refs, objects, index, HEAD, and command side effects |
| Safety | dangerous shortcuts | distinguishes local vs pushed | handles shared/protected history, verification, and recovery calmly |
| Collaboration | basic push/pull | clean branch and PR flow | handles forks, upstreams, checks, reviews, force-with-lease, and PR updates |
| Debugging | guesses | uses log/diff/blame | uses bisect, range-diff, merge-base, reflog, and exact ranges |
| Governance | mentions PRs | branch protection and reviews | rulesets, CODEOWNERS, permissions, tokens, audit, secrets, required checks |
| Release | tags vaguely | release branches/tags | semver, annotated/signed tags, hotfix/backport, changelog, artifact traceability |

---

## 3. Foundations Rubric

5-point answer includes:

- working tree
- index/staging area
- HEAD
- branch ref
- commit history
- `git add`, `commit`, `diff`, `status`, `restore`
- verification before commit

Deductions:

| Issue | Deduct |
|---|---|
| cannot explain index | -2 |
| confuses staged vs unstaged diff | -1 |
| says commit stores only diff | -1 |
| no verification before commit | -1 |

---

## 4. Branching Merge Rebase Rubric

5-point answer includes:

- branch as movable ref
- fast-forward vs merge commit
- merge base
- rebase copies commits to new base
- hash changes after rebase
- conflict resolution and abort/continue commands
- local/private vs shared branch boundary

Deductions:

| Issue | Deduct |
|---|---|
| says rebase moves same commits | -2 |
| recommends rebasing shared branch without caution | -2 |
| cannot explain conflict workflow | -1 |
| no merge-base concept | -1 |

---

## 5. Undo Recovery Rubric

5-point answer includes:

- `restore` for files/index
- `reset` for local branch pointer and state
- `revert` for shared history
- reflog for local recovery
- stash caveats
- `--force-with-lease` boundary
- inspect before destructive command

Deductions:

| Issue | Deduct |
|---|---|
| uses `reset --hard` casually | -3 |
| resets pushed shared branch | -3 |
| no reflog recovery path | -1 |
| cannot explain soft/mixed/hard | -2 |
| no local vs pushed distinction | -2 |

---

## 6. Remote PR Flow Rubric

5-point answer includes:

- fetch vs pull
- upstream tracking
- feature branch flow
- PR creation and checks
- updating branch from main
- non-fast-forward handling
- safe force-with-lease for private rewritten branch
- local PR review

Deductions:

| Issue | Deduct |
|---|---|
| force pushes without checking ownership | -3 |
| no fetch-before-integrate habit | -1 |
| cannot explain non-fast-forward | -1 |
| ignores required checks/review | -1 |

---

## 7. Inspection Debugging Rubric

5-point answer includes:

- two-dot vs three-dot
- log/show/diff/blame
- bisect with good/bad commits
- range-diff after rebase
- merge-base
- tag/branch comparison
- exact verification commands

Deductions:

| Issue | Deduct |
|---|---|
| cannot explain `main..feature` | -1 |
| cannot explain `main...feature` | -1 |
| no bisect workflow | -1 |
| uses blame as accusation instead of investigation | -1 |
| no range-diff for rewritten PR | -1 |

---

## 8. Git Internals Rubric

5-point answer includes:

- blob/tree/commit/tag objects
- commit DAG
- refs and HEAD
- index as next snapshot
- merge/rebase internals
- fetch/push ref updates
- reflog and reachability
- packfiles/gc boundaries

Deductions:

| Issue | Deduct |
|---|---|
| says branch contains commits instead of points to commit | -1 |
| no object model | -2 |
| cannot explain detached HEAD | -1 |
| no reachability/reflog boundary | -1 |
| no rebase hash explanation | -1 |

---

## 9. GitHub CLI Rubric

5-point answer includes:

- separates `git` and `gh` responsibilities
- auth/token awareness
- PR create/view/checks/checkout/review/merge
- workflow run inspection
- release commands
- caution for scripted admin operations

Deductions:

| Issue | Deduct |
|---|---|
| treats `gh` as Git replacement | -1 |
| no token scope awareness | -1 |
| no PR checks inspection | -1 |
| scripts admin actions without dry-run/review | -2 |

---

## 10. Governance Security Rubric

5-point answer includes:

- branch protection/rulesets
- required PR and checks
- CODEOWNERS with required owner review
- permissions by team/least privilege
- secret scanning and rotation process
- token/GitHub App model
- protected workflow files
- audit trail and bypass policy

Deductions:

| Issue | Deduct |
|---|---|
| CODEOWNERS without enforcement | -1 |
| no required checks | -1 |
| direct push to main allowed by default | -2 |
| broad PATs for automation | -2 |
| secret cleanup without rotation | -3 |
| no audit/bypass story | -1 |

---

## 11. Release Engineering Rubric

5-point answer includes:

- annotated/signed tags for releases
- semver and compatibility
- trunk vs release branch trade-off
- hotfix workflow
- backport with cherry-pick
- GitHub releases/changelog
- rollback vs revert vs roll forward
- artifact traceability
- protected release branches/tags

Deductions:

| Issue | Deduct |
|---|---|
| moving release tags casually | -3 |
| no hotfix-to-main reconciliation | -2 |
| no artifact traceability | -1 |
| confuses rollback and revert | -1 |
| broad merge from main into old release branch | -2 |

---

## 12. Capstone Rubric

A 5-point MAANG capstone answer includes:

1. Clear initial risk triage.
2. Branching and PR model.
3. Required checks and review policy.
4. CODEOWNERS and sensitive path ownership.
5. Permissions and token governance.
6. Secret incident response.
7. Merge strategy and queue if needed.
8. Release tags, changelog, artifact traceability.
9. Hotfix/backport workflow.
10. Audit and phased rollout plan.

Red flags:

- starts with destructive commands
- ignores existing engineers and migration cost
- treats governance as only documentation
- no release/rollback story
- no measurement of success

---

## 13. Readiness Matrix

| Area | Target Score |
|---|---|
| Local foundations | 5 |
| Branching/merge/rebase | 4-5 |
| Undo/recovery | 5 |
| Remote/PR workflow | 4-5 |
| Inspection/debugging | 4 |
| Git internals | 4 |
| GitHub CLI | 4 |
| Governance/security | 4-5 |
| Release engineering | 4-5 |
| Capstone | 4-5 |

Final readiness rule:

```text
You are not senior-ready in Git until you can say what command you will run, what state it changes, why it is safe for this branch, and how you will recover if wrong.
```
