# Git Patch, Email, and Open Source Contribution Gold Sheet

> Goal: understand the non-GitHub collaboration world: patch files, email workflows, `format-patch`, `git am`, `git apply`, DCO sign-off, maintainer review, and open-source contribution etiquette.

---

## 1. Intuition

GitHub PRs are one collaboration interface.

Patch workflows are another.

Instead of:

```text
branch -> push -> pull request
```

Some projects use:

```text
commit series -> patch emails -> maintainer applies with git am
```

This is common in older, lower-level, or mailing-list-driven projects.

---

## 2. Definition

- Definition: Patch/email workflows exchange Git changes as patch files or emails instead of hosted pull requests.
- Category: open-source workflow / distributed collaboration.
- Core idea: a commit series can be serialized, reviewed, revised, and applied without shared repository permissions.

---

## 3. Why It Exists

Patch workflows exist because:

- Git was designed for distributed collaboration
- maintainers may not use GitHub/GitLab PRs
- contributors may not have push access
- mailing lists provide durable public review
- maintainers can apply patches while preserving author metadata
- patch series are easy to review as ordered changes

You need this knowledge when contributing to:

- Linux-style projects
- mailing-list-based projects
- security patches sent privately
- vendor patches
- backports between repos
- internal teams without shared hosting

---

## 4. Key Commands

| Command | Use |
|---|---|
| `git diff` | create raw diff for uncommitted changes |
| `git apply` | apply a patch to working tree |
| `git format-patch` | export commits as email-ready patch files |
| `git am` | apply mailbox patches as commits |
| `git send-email` | send patch series by email |
| `git request-pull` | ask maintainer to pull from a public branch |
| `git range-diff` | compare two versions of a patch series |

---

## 5. Raw Patch vs Commit Patch

Raw diff:

```bash
git diff > fix.patch
git apply fix.patch
```

Use for:

- local experiments
- simple patch exchange
- unstaged changes

Commit patch:

```bash
git format-patch origin/main..HEAD
git am 0001-fix-timeout.patch
```

Use for:

- preserving author, commit message, and metadata
- sending ordered series
- maintainer workflow

Rule:

> `git apply` applies file changes. `git am` applies commits from mailbox-style patches.

---

## 6. Creating Patch Series

Create patches for commits not on main:

```bash
git fetch origin
git format-patch origin/main..HEAD
```

Create numbered cover letter:

```bash
git format-patch --cover-letter origin/main..HEAD
```

Create v2 series:

```bash
git format-patch -v2 --cover-letter origin/main..HEAD
```

Check what will be sent:

```bash
git log --oneline origin/main..HEAD
git diff --stat origin/main..HEAD
```

---

## 7. Applying Patches

Apply raw patch:

```bash
git apply fix.patch
```

Check first:

```bash
git apply --check fix.patch
```

Apply mailbox patch as commit:

```bash
git am 0001-fix-timeout.patch
```

Abort failed apply:

```bash
git am --abort
```

Continue after conflict:

```bash
git add resolved-file
git am --continue
```

---

## 8. DCO And Signed-Off-By

Some projects require Developer Certificate of Origin sign-off.

Add sign-off:

```bash
git commit -s -m "Fix timeout handling"
```

Amend sign-off:

```bash
git commit --amend -s --no-edit
```

What `Signed-off-by` means:

- contributor certifies they have the right to submit the work under the project terms

What it does not mean:

- cryptographic signature
- code correctness
- maintainer approval

Do not confuse:

| Concept | Meaning |
|---|---|
| `Signed-off-by` | DCO/legal attestation line |
| signed commit | cryptographic commit signature |
| reviewed-by | reviewer approval in project convention |
| co-authored-by | authorship credit |

---

## 9. Open Source PR Etiquette

Before contributing:

1. Read `CONTRIBUTING.md`.
2. Check issue tracker and existing PRs.
3. Run tests locally.
4. Keep changes focused.
5. Follow style and commit message rules.
6. Add docs/tests where expected.
7. Avoid huge unrelated cleanup.
8. Explain motivation clearly.

Good commit series:

- one logical change per commit
- build stays green at each commit when possible
- commit messages explain why
- tests included
- no generated/noisy files unless required

Bad contribution:

- huge mixed refactor
- formatting entire repo
- no tests
- ignores maintainer template
- argues instead of revising

---

## 10. Revising A Patch Series

After review:

```bash
git rebase -i origin/main
git format-patch -v2 --cover-letter origin/main..HEAD
```

Compare v1 and v2:

```bash
git range-diff origin/main...old-series origin/main...HEAD
```

Include change notes:

```text
Changes in v2:
- Split parser fix from validation change.
- Added regression test.
- Renamed function based on review feedback.
```

Maintainer-friendly behavior:

- answer review comments directly
- explain changes between versions
- avoid force-updating without context
- keep patch subject stable if possible

---

## 11. Security Patch Flow

For sensitive vulnerabilities:

- follow project security policy
- do not open public issue if policy says private disclosure
- send minimal reproduction privately
- coordinate embargo if applicable
- avoid leaking exploit details in public branch names
- wait for maintainer guidance before public patch

GitHub-specific:

- use Security Advisories if project uses them
- avoid public PR for undisclosed vulnerability unless maintainers request it

---

## 12. Common Mistakes

| Mistake | Better Approach |
|---|---|
| sending raw diff when project expects patch series | use `format-patch` |
| applying mailbox patch with `git apply` | use `git am` |
| forgetting DCO sign-off | `git commit -s` |
| mixing refactor and bugfix | split commits |
| not reading contribution guide | read `CONTRIBUTING.md` first |
| submitting security bug publicly | follow security policy |
| v2 patch without change notes | include revision summary |

---

## 13. Practical Question

> An open-source project does not use GitHub PRs. It asks for a v2 patch series with DCO sign-off. How do you submit it?

---

## 14. Strong Answer

I would start by reading the contribution guide and checking the required base branch. I would create a clean commit series locally, sign off commits with `git commit -s`, run tests, and generate patches with:

```bash
git format-patch -v2 --cover-letter origin/main..HEAD
```

I would include a cover letter explaining the change and a "Changes in v2" section. If mailing is required, I would use the project-documented `git send-email` setup. If I receive review feedback, I would revise commits with interactive rebase, regenerate the patch series, and use `range-diff` to verify what changed between versions.

---

## 15. Revision Notes

- One-line summary: Patch workflows send commit series, not branches.
- Three keywords: format-patch, am, sign-off.
- One interview trap: confusing DCO sign-off with cryptographic signing.
- One memory trick: "diff applies files; am applies mail commits."

---

## 16. Official Source Notes

- Git format-patch docs: <https://git-scm.com/docs/git-format-patch>
- Git am docs: <https://git-scm.com/docs/git-am>
- Git apply docs: <https://git-scm.com/docs/git-apply>
- Git send-email docs: <https://git-scm.com/docs/git-send-email>
- Git request-pull docs: <https://git-scm.com/docs/git-request-pull>
- Git range-diff docs: <https://git-scm.com/docs/git-range-diff>
