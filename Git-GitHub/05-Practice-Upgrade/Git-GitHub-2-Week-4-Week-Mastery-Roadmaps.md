# Git GitHub 2 Week 4 Week Mastery Roadmaps

> Track: Git and GitHub Command Mastery - Practice Upgrade  
> Goal: turn the Git-GitHub notes into a complete interview and production-readiness plan.

Use the 2-week roadmap for fast interview acceleration. Use the 4-week roadmap for deeper mastery.

---

## 1. Daily Study Loop

Use this loop every day:

1. Read one focused sheet.
2. Practice 10-20 commands in a throwaway repo.
3. Answer 15 active recall questions.
4. Solve 1 scenario or lab.
5. Speak a 3-minute answer aloud.
6. Score yourself with the rubric.

Daily safety mantra:

```text
Inspect first. Identify local vs shared. Choose the command. Verify. Preserve recovery path.
```

---

## 2. 2-Week Interview Acceleration Plan

Target: backend engineer who needs strong Git/GitHub interview and production confidence quickly.

### Day 1: Local Git Foundations

Read:

- `Git-Local-Foundations-Daily-Commands-Gold-Sheet.md`

Practice:

- Lab 1: three-state model
- Lab 2: partial staging
- 25 local foundation recall questions

Outcome:

```text
Explain working tree, index, HEAD, add, commit, diff, restore, and status.
```

---

### Day 2: Branches Merges Conflicts

Read:

- `Git-Branching-Merging-Rebasing-Conflicts-Gold-Sheet.md`

Practice:

- Lab 3: branch and fast-forward
- Lab 4: merge conflict
- Mock 2 opening answer

Outcome:

```text
Explain branch, merge, fast-forward, merge base, and conflict workflow.
```

---

### Day 3: Rebase And Cherry-Pick

Read:

- branching/rebasing sections again
- production playbook conflict/cherry-pick scenarios

Practice:

- Lab 5: rebase conflict
- Scenario 7: PR conflicts
- Scenario 25: backport critical fix

Outcome:

```text
Explain when rebase is safe, why hashes change, and when cherry-pick is useful.
```

---

### Day 4: Remote Collaboration GitHub Flow

Read:

- `Git-Remote-Collaboration-GitHub-Flow-Gold-Sheet.md`

Practice:

- Lab 9: remote branch simulation
- Scenario 9: non-fast-forward push
- Mock 4 remote PR flow

Outcome:

```text
Explain fetch, pull, push, upstream tracking, fork flow, PR flow, and branch updates.
```

---

### Day 5: Undo Recovery Safety

Read:

- `Git-Undo-Recovery-Safety-Commands-Gold-Sheet.md`

Practice:

- Lab 6: reset modes
- Lab 7: reflog recovery
- Mock 3 undo recovery

Outcome:

```text
Choose restore, reset, revert, stash, reflog, and force-with-lease safely.
```

---

### Day 6: Inspection Debugging History

Read:

- `Git-Inspection-Debugging-History-Pro-Commands-Gold-Sheet.md`

Practice:

- Lab 11: bisect
- Lab 12: range-diff
- Scenario 17: find bug-introducing commit

Outcome:

```text
Use log, show, diff, blame, bisect, range-diff, and tag/branch comparisons.
```

---

### Day 7: Week 1 Review

Practice:

- 60 active recall questions
- 5 scenarios from the scenario bank
- Mock 1 and Mock 3

Pass bar:

- foundations: 5
- undo/recovery: 4+
- branch/remote: 4+

---

### Day 8: GitHub CLI

Read:

- `GitHub-CLI-Command-Mastery-Gold-Sheet.md`

Practice:

- Lab 19: full PR flow with GitHub CLI
- PR checkout/review/checks commands
- release command review

Outcome:

```text
Use gh for PRs, checks, reviews, workflow inspection, and releases.
```

---

### Day 9: Advanced Repository Workflows

Read:

- `Git-Advanced-Repository-Workflows-Gold-Sheet.md`

Practice:

- Lab 8: stash vs worktree
- sparse checkout, submodule, LFS recall
- hotfix with worktree scenario

Outcome:

```text
Explain worktree, submodule, sparse checkout, hooks, LFS, attributes, aliases, and rerere.
```

---

### Day 10: Git Internals

Read:

- `Git-Internals-Object-Model-Refs-Packfiles-Gold-Sheet.md`

Practice:

- Lab 13: object inspection
- Mock 6 Git internals
- 25 internals recall questions

Outcome:

```text
Explain blob/tree/commit/tag objects, refs, HEAD, index, DAG, packfiles, reflog, and GC.
```

---

### Day 11: GitHub Governance Security

Read:

- `GitHub-Enterprise-Governance-Security-Branch-Protection-Gold-Sheet.md`

Practice:

- Lab 16: CODEOWNERS simulation
- Lab 17: branch protection design
- Lab 18: secret incident runbook

Outcome:

```text
Design GitHub branch protection, rulesets, CODEOWNERS, checks, permissions, token, secret, and audit controls.
```

---

### Day 12: Release Engineering

Read:

- `Git-Release-Engineering-Versioning-Tags-Backports-Gold-Sheet.md`

Practice:

- Lab 14: tags and release notes
- Lab 15: backport with cherry-pick
- Mock 9 release engineering

Outcome:

```text
Run tag, release branch, hotfix, backport, changelog, rollback, and traceability discussions.
```

---

### Day 13: Production Scenario Playbook

Read:

- `Git-GitHub-Production-Interview-Scenario-Playbook.md`
- `Git-GitHub-Golden-Command-Cheat-Sheet.md`

Practice:

- 8 random scenario drills
- 1 governance/security mock
- 1 release mock

Outcome:

```text
Handle real Git incidents without panic or destructive shortcuts.
```

---

### Day 14: Final Mock Day

Practice:

- Mock 10 MAANG capstone
- 80 active recall questions
- 5 hands-on labs from memory
- score every area with rubric

Pass bar:

- foundations: 5
- recovery: 5
- remote/PR: 4+
- internals: 4+
- governance: 4+
- release: 4+
- capstone: 4+

---

## 3. 4-Week Mastery Plan

Target: durable Git/GitHub production confidence and senior interview fluency.

---

## Week 1: Daily Git And Branch Safety

Focus:

- working tree/index/HEAD
- local commands
- branching
- merging
- rebasing
- conflicts
- remote basics

Files:

- `Git-Local-Foundations-Daily-Commands-Gold-Sheet.md`
- `Git-Branching-Merging-Rebasing-Conflicts-Gold-Sheet.md`
- `Git-Remote-Collaboration-GitHub-Flow-Gold-Sheet.md`

Practice:

- Labs 1-5 and 9
- Mocks 1, 2, 4
- 100 active recall questions

Week gate:

```text
You can work on feature branches, resolve conflicts, and update PRs without damaging shared history.
```

---

## Week 2: Recovery And History Debugging

Focus:

- restore/reset/revert
- reflog
- stash
- force-with-lease
- log/show/diff/blame
- bisect
- range-diff

Files:

- `Git-Undo-Recovery-Safety-Commands-Gold-Sheet.md`
- `Git-Inspection-Debugging-History-Pro-Commands-Gold-Sheet.md`
- `Git-GitHub-Production-Interview-Scenario-Playbook.md`

Practice:

- Labs 6, 7, 10, 11, 12
- Mocks 3, 5
- 10 production scenarios

Week gate:

```text
You can recover local mistakes, avoid unsafe shared-history rewrites, and debug history precisely.
```

---

## Week 3: Advanced Workflows And Internals

Focus:

- GitHub CLI
- worktree
- submodule
- sparse checkout
- LFS
- hooks
- rerere
- Git object model
- refs, HEAD, index, packfiles, GC

Files:

- `GitHub-CLI-Command-Mastery-Gold-Sheet.md`
- `Git-Advanced-Repository-Workflows-Gold-Sheet.md`
- `Git-Internals-Object-Model-Refs-Packfiles-Gold-Sheet.md`

Practice:

- Labs 8, 13, 19
- Mock 6, 7
- internals active recall

Week gate:

```text
You can explain not only what command to run, but what Git changes internally.
```

---

## Week 4: Enterprise Governance Release Capstone

Focus:

- branch protection/rulesets
- CODEOWNERS
- required checks
- permissions/tokens
- secret scanning
- audit
- release branches
- annotated tags
- hotfix/backport
- changelog
- artifact traceability

Files:

- `GitHub-Enterprise-Governance-Security-Branch-Protection-Gold-Sheet.md`
- `Git-Release-Engineering-Versioning-Tags-Backports-Gold-Sheet.md`
- all `05-Practice-Upgrade` files

Practice:

- Labs 14-18 and 20
- Mocks 8, 9, 10
- 2 full capstone answers

Week gate:

```text
You can design and defend a safe Git/GitHub operating model for a large engineering organization.
```

---

## 4. Topic Weighting For Senior Interviews

| Area | Weight |
|---|---:|
| Daily workflow and state model | 15% |
| Branching, merging, rebasing | 15% |
| Undo/recovery safety | 20% |
| Remote/PR collaboration | 15% |
| Inspection/debugging | 10% |
| Git internals | 10% |
| GitHub governance/security | 10% |
| Release engineering | 5% |

For platform or staff-level roles, governance and release engineering become much heavier.

---

## 5. Final Readiness Checklist

You are ready when you can:

1. Explain every command by state change.
2. Distinguish local/private history from pushed/shared history.
3. Recover lost commits with reflog.
4. Resolve merge and rebase conflicts safely.
5. Update PR branches without unsafe force pushes.
6. Debug history with bisect, blame, range-diff, log, and diff.
7. Explain Git internals: objects, refs, HEAD, index, DAG, packfiles.
8. Use GitHub CLI for PRs, checks, reviews, workflow inspection, and releases.
9. Design GitHub branch protection, CODEOWNERS, checks, permissions, and audit controls.
10. Run release/hotfix/backport/tag/changelog workflows safely.

---

## 6. Final Message To Remember

```text
Strong Git skill is not memorizing commands. It is knowing what state each command changes,
whether that history is shared, how to verify before acting, and how to recover when something goes wrong.
```
