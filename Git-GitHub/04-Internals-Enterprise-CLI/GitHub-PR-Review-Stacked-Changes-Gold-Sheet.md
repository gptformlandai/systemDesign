# GitHub Pull Request Review Excellence and Stacked Changes Gold Sheet

> Goal: review and manage pull requests like a senior engineer: focused commits, reviewable diffs, stacked PRs, merge method choices, update strategy, and reviewer trust.

---

## 1. Intuition

A PR is not just a code upload.

It is a communication package:

```text
problem -> approach -> diff -> tests -> risk -> rollout -> review conversation -> merge decision
```

Senior PR skill is making change easy to review and safe to merge.

---

## 2. Definition

- Definition: PR review excellence is the discipline of creating, reviewing, updating, and merging changes with clear context, small scope, reliable checks, and safe history strategy.
- Category: collaboration / engineering quality / GitHub workflow.
- Core idea: optimize for reviewer understanding and production safety, not only for author convenience.

---

## 3. Why It Exists

Poor PR process causes:

- rubber-stamp reviews
- giant unreviewable diffs
- hidden risky changes
- repeated main breakage
- stale approvals after force pushes
- unclear ownership
- merge conflicts near release
- noisy style-only churn

Good PR process creates:

- clear review boundaries
- faster feedback
- better audit trail
- safer merges
- better onboarding
- stronger production confidence

---

## 4. Anatomy Of A Strong PR

A strong PR includes:

- clear title
- concise summary
- linked issue/ticket
- what changed
- why it changed
- testing evidence
- screenshots/logs if relevant
- migration/deployment notes
- rollback plan
- risk areas
- reviewer guidance

PR body shape:

```md
## Summary

## Testing

## Risk

## Rollback
```

Review-friendly diff:

- one logical change
- focused commits
- no unrelated formatting
- generated files clearly separated
- large file changes explained
- tests near changed behavior

---

## 5. Review Modes

| Mode | Use |
|---|---|
| comment | non-blocking question/suggestion |
| approve | change is safe enough to merge |
| request changes | blocking correctness/security/maintainability issue |
| suggested change | small reviewer-proposed patch |
| draft PR | early feedback, not ready to merge |
| review required by CODEOWNERS | ownership-controlled approval |

Senior reviewer behavior:

- lead with correctness and risk
- distinguish blocking from preference
- explain why
- suggest concrete alternatives
- avoid drive-by style churn unless style matters
- re-review changed areas after updates

---

## 6. Merge Methods

| Method | What Happens | Good For | Trade-off |
|---|---|---|---|
| merge commit | preserves branch history | explicit integration history | main can be noisy |
| squash merge | one commit on main | clean feature-level history | loses individual commit structure |
| rebase merge | replay commits on main | linear history with commits preserved | commit hashes change |

Decision:

- use squash for small feature PRs if main should stay concise
- use merge commits when preserving branch context matters
- use rebase merge when team values linear commit series
- protect release branches from accidental broad merges

Interview line:

> Merge method is a team policy and audit decision, not a personal taste contest.

---

## 7. Updating A PR

Fetch first:

```bash
git fetch origin
```

Rebase private feature branch:

```bash
git rebase origin/main
git push --force-with-lease
```

Merge main into shared feature branch:

```bash
git merge origin/main
git push
```

Use `range-diff` after rewriting:

```bash
git range-diff origin/main...old-head origin/main...HEAD
```

Good update note:

```text
Rebased on main. Only conflict resolution changed in PaymentMapper; range-diff shows patch 2 unchanged.
```

---

## 8. Stacked Changes

Stacked PRs split a large feature into dependent reviewable slices.

Example:

```text
main
  -> pr1: database schema
    -> pr2: repository layer
      -> pr3: API endpoint
        -> pr4: UI integration
```

Use stacked PRs when:

- one feature is too large for one PR
- foundations must land before later changes
- reviewers differ by layer
- you want incremental merge safety

Avoid stacked PRs when:

- team tooling cannot handle it
- dependencies are confusing
- one small PR is enough
- release branch policy cannot tolerate partial landing

---

## 9. Stacked PR Commands

Create first branch:

```bash
git switch -c feature/schema origin/main
git commit -m "Add order audit table"
git push -u origin feature/schema
gh pr create --base main --head feature/schema
```

Create second branch on top:

```bash
git switch -c feature/repository
git commit -m "Add order audit repository"
git push -u origin feature/repository
gh pr create --base feature/schema --head feature/repository
```

After PR1 merges:

```bash
git fetch origin
git switch feature/repository
git rebase origin/main
git push --force-with-lease
gh pr edit --base main
```

Caution:

- clearly state dependency chain in PR body
- update bases after earlier PRs merge
- use `range-diff` to verify rewrites

---

## 10. Review Local PR

With GitHub CLI:

```bash
gh pr checkout 123
git status
git log --oneline --decorate --graph origin/main..HEAD
git diff --stat origin/main...HEAD
```

Run tests:

```bash
# project-specific test command
```

Return to your branch:

```bash
git switch -
```

Senior note:

> I review risky PRs locally when static diff review is not enough: migrations, generated code, complex refactors, or performance-sensitive changes.

---

## 11. Risk-Based Review Checklist

Ask:

1. What user/system behavior changes?
2. What data migration or compatibility risk exists?
3. What security boundary changes?
4. What rollback path exists?
5. Are tests meaningful?
6. Are logs/metrics affected?
7. Are config or secrets touched?
8. Are workflow/deployment files touched?
9. Are public APIs changed?
10. Are generated files expected?

For high-risk paths:

- require CODEOWNERS
- require security/platform review
- require migration plan
- require deployment/rollback plan
- consider feature flags

---

## 12. Common Mistakes

| Mistake | Better Approach |
|---|---|
| huge PR with mixed concerns | split into focused PRs or stack |
| force-push after approval without note | explain rewrite and request re-review |
| PR title says "fix stuff" | use behavior-focused title |
| unresolved review comments | resolve with explanation or follow-up |
| no tests listed | include evidence or rationale |
| stale branch blindly merged | update and verify required checks |
| merge method chosen randomly | follow repo policy |
| stacked PRs without dependency notes | document stack and base branches |

---

## 13. Practical Question

> You have a 3,000-line feature touching database, backend, and UI. Reviewers complain it is too large. What do you do?

---

## 14. Strong Answer

I would split it into reviewable slices. If the pieces depend on each other, I would use stacked PRs: schema/migration first, backend repository/service next, API layer next, then UI integration. Each PR would have its own tests, risk notes, and rollback consideration. I would clearly document the stack and base branches. As earlier PRs merge, I would rebase later PRs onto main, update their base, use `range-diff` to verify changes, and ask for re-review where needed.

---

## 15. Revision Notes

- One-line summary: A PR is a reviewable, testable, risk-aware communication package.
- Three keywords: scope, evidence, stack.
- One interview trap: treating force-push updates as invisible after approval.
- One memory trick: "Small diff, clear risk, clean merge."

---

## 16. Official Source Notes

- GitHub pull request docs: <https://docs.github.com/en/pull-requests>
- GitHub CODEOWNERS docs: <https://docs.github.com/en/repositories/managing-your-repositorys-settings-and-features/customizing-your-repository/about-code-owners>
- GitHub merge queue docs: <https://docs.github.com/en/repositories/configuring-branches-and-merges-in-your-repository/configuring-pull-request-merges/managing-a-merge-queue>
- Git range-diff docs: <https://git-scm.com/docs/git-range-diff>
- GitHub CLI manual: <https://cli.github.com/manual/>
