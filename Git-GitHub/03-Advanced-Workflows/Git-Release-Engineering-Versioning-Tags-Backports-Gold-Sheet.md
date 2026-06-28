# Git Release Engineering Versioning Tags Backports Gold Sheet

> Track: Git and GitHub Command Mastery - Senior / MAANG Release Layer

Goal: handle releases with Git and GitHub: versioning, annotated tags, release branches, backports, hotfixes, changelogs, rollback, GitHub releases, and audit-safe production workflows.

---

## 0. How To Read This

Use this after branching, remote collaboration, undo/recovery, inspection, and GitHub CLI.

Release mental model:

```text
main branch -> release candidate -> tag -> artifact -> deployment -> hotfix/backport -> audit trail
```

Interview rule:

```text
A release is not just a tag. It is a reproducible source state, artifact, changelog, approval, deployment, and rollback story.
```

---

# Topic 1: Git Release Engineering

## 1. Intuition

Git records what changed.

Release engineering decides what becomes official.

A mature release process answers:

- What code is in the release?
- Who approved it?
- Which tests passed?
- Which artifact was built?
- Which tag identifies it?
- How do we patch it?
- How do we roll it back?
- How do we audit it later?

---

## 2. Definition

- Definition: Release engineering with Git/GitHub is the disciplined use of branches, tags, pull requests, changelogs, artifacts, and hotfix/backport workflows to ship software safely and traceably.
- Category: source control operations and production delivery.
- Core idea: every production release should map to an immutable, reviewable source state.

---

## 3. Why It Exists

Without release discipline:

- nobody knows exactly what shipped
- tags can point to the wrong commit
- hotfixes get lost between branches
- release notes are incomplete
- rollback decisions are chaotic
- production and main diverge silently
- audit cannot prove approval/checks
- patch releases accidentally include unrelated changes

---

## 4. Release Vocabulary

| Term | Meaning |
|---|---|
| release candidate | build/commit proposed for release |
| tag | Git ref naming a release point |
| annotated tag | tag object with message, tagger, date, optional signature |
| lightweight tag | simple ref pointing to commit |
| release branch | branch used to stabilize and patch a release line |
| hotfix | urgent production fix |
| backport | applying a fix from one branch to an older release branch |
| changelog | human-readable list of notable changes |
| rollback | restore previous deployed version or revert change |
| roll forward | fix issue with a new release |

---

## 5. Tags For Releases

Prefer annotated tags for real releases:

```bash
git tag -a v1.4.0 -m "Release v1.4.0"
git push origin v1.4.0
```

Why:

- tagger identity
- tag date
- message
- can be signed
- clearer audit trail

Avoid moving release tags:

```text
A release tag should be treated as immutable once published.
```

If a tag is wrong, coordinate carefully and prefer a new patch tag when possible.

---

## 6. Semantic Versioning

Common format:

```text
MAJOR.MINOR.PATCH
```

Meaning:

| Part | Change Type |
|---|---|
| MAJOR | incompatible API or behavior changes |
| MINOR | backward-compatible features |
| PATCH | backward-compatible fixes |

Examples:

```text
1.4.2 -> patch fix
1.5.0 -> feature release
2.0.0 -> breaking release
```

Caution:

```text
Semantic versioning only helps if the team defines compatibility and follows it consistently.
```

---

## 7. Trunk-Based Release Flow

Typical flow:

```text
feature branches -> PRs -> main -> tag release from main -> deploy artifact
```

Works well when:

- CI is strong
- feature flags exist
- main is always releasable
- releases are frequent
- rollback/roll-forward path is clear

Commands:

```bash
git switch main
git pull --ff-only
git tag -a v1.5.0 -m "Release v1.5.0"
git push origin v1.5.0
```

---

## 8. Release Branch Flow

Typical flow:

```text
main -> release/1.5 -> stabilize -> tag v1.5.0 -> patch release/1.5 -> tag v1.5.1
```

Use when:

- release stabilization takes time
- multiple supported versions exist
- enterprise customers need patch lines
- mobile/desktop releases need approval windows
- production cannot take every main change

Caution:

```text
Release branches create branch management cost. Every hotfix must be reconciled with main.
```

---

## 9. Hotfix Flow

Safe hotfix from main:

```bash
git fetch origin
git switch -c hotfix/payment-timeout origin/main
# fix, test, commit
git push -u origin HEAD
gh pr create --base main --head hotfix/payment-timeout
```

If production is on a release branch:

```bash
git fetch origin
git switch -c hotfix/payment-timeout origin/release/1.5
# fix, test, commit
git push -u origin HEAD
gh pr create --base release/1.5 --head hotfix/payment-timeout
```

After merge:

```bash
git tag -a v1.5.1 -m "Release v1.5.1"
git push origin v1.5.1
```

Then reconcile to main:

```bash
git switch main
git pull --ff-only
git cherry-pick <hotfix-sha>
# or merge release branch if that is team policy
```

---

## 10. Backport Flow

A backport copies a fix to an older supported branch.

```bash
git fetch origin
git switch -c backport/1.4-payment-timeout origin/release/1.4
git cherry-pick -x <fix-sha>
git push -u origin HEAD
gh pr create --base release/1.4 --head backport/1.4-payment-timeout
```

Use `-x` for public/open-source or audit-heavy backports because it records original commit id in the message.

Strong answer:

```text
A backport should include only the needed fix and its required dependencies. I avoid accidentally bringing unrelated main-branch changes into an older release line.
```

---

## 11. Changelog Discipline

Good changelog entry answers:

- what changed?
- why does user/operator care?
- is there migration work?
- is it breaking?
- what issue/PR links explain it?

Categories:

```text
Added
Changed
Deprecated
Removed
Fixed
Security
```

Git commands to gather changes:

```bash
git log --oneline v1.4.0..v1.5.0
git shortlog -sne v1.4.0..v1.5.0
git diff --stat v1.4.0..v1.5.0
```

GitHub CLI:

```bash
gh release create v1.5.0 --generate-notes
gh release view v1.5.0
gh release upload v1.5.0 dist/app.tar.gz
```

---

## 12. Compare Ranges For Releases

Useful ranges:

```bash
# commits in new release since previous tag
git log --oneline v1.4.0..v1.5.0

# code diff between releases
git diff v1.4.0..v1.5.0

# PR branch diff from merge base
git diff origin/main...HEAD

# compare rewritten PR versions
git range-diff origin/main...old-head origin/main...new-head
```

Interview line:

```text
For release notes I compare tag to tag. For PR review I usually compare merge-base to branch using three-dot diff.
```

---

## 13. Release Artifact Traceability

A mature release links:

```text
tag -> commit SHA -> build workflow run -> artifact digest -> deployment record -> changelog
```

Minimum metadata:

- release version
- Git tag
- commit SHA
- build id
- artifact name and digest
- environment
- deploy time
- approver
- rollback target

Strong answer:

```text
If someone asks what is running in production, I should be able to trace from deployment to artifact to tag to commit to PRs.
```

---

## 14. Rollback vs Revert vs Roll Forward

| Action | Meaning | Use When |
|---|---|---|
| rollback deployment | deploy previous artifact/version | bad release, previous version safe |
| revert commit | create new commit that undoes change | bad code already merged/shared |
| roll forward | ship new fix | rollback impossible or fix is small/urgent |

Caution:

```text
Git revert changes source history; deployment rollback changes runtime version. They are related but not the same.
```

---

## 15. Release Branch Protection

Release branches should often be protected.

Controls:

- PR required
- release owner review
- required tests
- restricted push
- no force pushes
- tag protection/rulesets
- signed release tags for sensitive systems

Reason:

```text
A release branch maps to production or supported customers, so it deserves at least as much protection as main.
```

---

## 16. GitHub Releases

GitHub Releases are product-facing release records built around tags.

Typical flow:

```bash
git tag -a v1.5.0 -m "Release v1.5.0"
git push origin v1.5.0
gh release create v1.5.0 --title "v1.5.0" --notes-file CHANGELOG.md
```

Add artifacts:

```bash
gh release upload v1.5.0 dist/service.tar.gz
```

Use draft/prerelease flags when needed:

```bash
gh release create v2.0.0-rc.1 --prerelease --generate-notes
```

---

## 17. Common Release Mistakes

| Mistake | Better Approach |
|---|---|
| lightweight tags for important releases | annotated/signed tags |
| moving a published tag silently | new patch tag or coordinated correction |
| hotfix release branch but forget main | reconcile hotfix back to main |
| release notes from memory | generate from tag ranges and PRs |
| cherry-pick broad feature into patch branch | isolate minimal fix |
| unprotected release branches | protect with PR/check/review policy |
| rollback without source follow-up | also revert/fix source if needed |
| no artifact traceability | link tag, SHA, build, artifact, deployment |

---

## 18. Scenario 1: Production Hotfix On Old Release

Prompt:

```text
Production runs v1.4.2 from release/1.4. Main has moved far ahead. A critical payment bug needs fixing.
```

Strong answer:

```text
I would branch from origin/release/1.4, apply the smallest safe fix, run the release branch checks, open a PR into release/1.4, and tag v1.4.3 after merge. Then I would ensure the fix exists on main, either by cherry-picking to main or confirming main already contains an equivalent fix. I would avoid merging all of main into release/1.4 because that could ship unrelated changes.
```

Commands:

```bash
git fetch origin
git switch -c hotfix/payment origin/release/1.4
# fix and test
git commit -am "Fix payment timeout handling"
git push -u origin HEAD
gh pr create --base release/1.4 --head hotfix/payment
```

---

## 19. Scenario 2: Wrong Release Tag

Prompt:

```text
v2.0.0 was tagged on the wrong commit and already pushed.
```

Strong answer:

```text
I would first check whether any artifact or deployment has used the tag. If it has been consumed, I prefer creating v2.0.1 or a clearly corrected release rather than silently moving v2.0.0. If policy allows retagging, I would coordinate with the team, delete/recreate the tag locally and remotely, and communicate that anyone who fetched the old tag must update. For audited systems, moving release tags is usually avoided.
```

---

## 20. Scenario 3: Need To Know What Shipped

Commands:

```bash
git show v1.5.0
git rev-list -n 1 v1.5.0
git log --oneline v1.4.0..v1.5.0
gh release view v1.5.0
```

Strong answer:

```text
I trace the deployment to the artifact, artifact to build run, build run to commit SHA, commit SHA to tag, and tag range to PRs/changelog.
```

---

## 21. Final Readiness Checklist

You should be able to explain:

1. Annotated vs lightweight tags.
2. Why release tags should be immutable.
3. How semantic versioning maps to compatibility.
4. Trunk-based release vs release branch flow.
5. Hotfix from main vs hotfix from release branch.
6. Backport with `cherry-pick -x`.
7. How to generate release notes from tag ranges.
8. Difference between rollback, revert, and roll forward.
9. How to trace production artifact to Git commit.
10. Why release branches and tags need protection.

---

## 22. Official Source Notes

- Git tag docs: https://git-scm.com/docs/git-tag
- Git shortlog docs: https://git-scm.com/docs/git-shortlog
- Git cherry-pick docs: https://git-scm.com/docs/git-cherry-pick
- GitHub releases docs: https://docs.github.com/en/repositories/releasing-projects-on-github/about-releases
- Semantic Versioning: https://semver.org/
