# Git History Rewrite and Repository Cleanup Gold Sheet

> Goal: know when history rewrite is safe, when it is dangerous, and how to clean secrets, large files, bad commits, and repository bloat without making the incident worse.

---

## 1. Intuition

History rewrite is repo surgery.

Small local surgery:

```text
amend last commit
interactive rebase local branch
fixup/squash before PR
```

Major production surgery:

```text
remove secret from all history
purge large binary from old commits
split monorepo history
rewrite author metadata across repository
```

Senior rule:

> Rewriting private history is a workflow tool. Rewriting shared history is an incident/change-management event.

---

## 2. Definition

- Definition: History rewrite changes commit graph history by creating new commits, refs, or object reachability instead of merely adding a new corrective commit.
- Category: advanced Git operations / repository administration / incident response.
- Core idea: new history means new commit hashes; shared clones, forks, tags, PRs, and CI references may need coordination.

---

## 3. Why It Exists

History rewrite is useful when:

- a secret was committed
- a large binary polluted history
- a feature branch needs cleanup before review
- a repo needs splitting or migration
- author metadata must be corrected before publishing
- old generated files need removal from all history
- a repo needs size reduction

But it is risky because:

- commit hashes change
- open PRs may become confusing
- forks still contain old objects
- tags may point to old commits
- CI/build provenance may break
- teammates can accidentally reintroduce old history

---

## 4. Rewrite Decision Matrix

| Situation | Preferred Approach |
|---|---|
| bad local unpushed commit | `commit --amend`, `reset`, or interactive rebase |
| bad pushed commit on shared branch | `git revert` |
| secret committed but not pushed | remove/amend/reset locally, rotate if real |
| secret pushed to remote | rotate secret first, then coordinate cleanup |
| large file pushed to feature branch | coordinate branch rewrite or new clean branch |
| large file merged into main long ago | repo-admin cleanup with migration plan |
| wrong release tag | prefer new patch tag if already consumed |
| public open-source history | avoid disruptive rewrites unless absolutely necessary |

Golden rule:

```text
If others may have based work on it, prefer additive repair unless the risk demands rewrite.
```

---

## 5. Rewrite Tools

| Tool | Use |
|---|---|
| `git commit --amend` | rewrite latest local commit |
| `git rebase -i` | rewrite local commit series |
| `git reset` | move current branch ref locally |
| `git filter-repo` | modern high-speed history rewrite/filtering tool |
| BFG Repo-Cleaner | common tool for secret/large-file cleanup |
| `git filter-branch` | older built-in rewrite tool; generally avoid for new cleanup work |
| `git replace` | temporary alternate object mapping |
| `git gc` / maintenance | clean unreachable objects after rewrite |

Interview note:

> For serious repository cleanup, I would use `git filter-repo` or a vetted cleanup tool, not manually edit `.git` internals.

---

## 6. Local Branch Cleanup

Amend last commit message:

```bash
git commit --amend
```

Amend last commit content:

```bash
git add path/to/file
git commit --amend --no-edit
```

Clean up local commits:

```bash
git fetch origin
git rebase -i origin/main
```

Push rewritten private feature branch:

```bash
git push --force-with-lease
```

Why `--force-with-lease`:

- it refuses to overwrite remote work you have not seen
- it is safer than plain `--force`
- it still rewrites remote history, so use intentionally

---

## 7. Secret Cleanup Runbook

If a secret is committed:

1. Assume compromise.
2. Revoke/rotate the secret immediately.
3. Identify exposure window.
4. Identify whether pushed to remote.
5. Identify forks, clones, CI logs, package artifacts, and caches.
6. Remove secret from current code.
7. Add prevention: `.gitignore`, secret scanning, push protection.
8. Decide whether history rewrite is required.
9. If rewriting, coordinate freeze and migration.
10. Verify no secret remains in reachable history.
11. Audit use of the old secret.

Important:

> History cleanup does not make an exposed secret safe. Rotation does.

Quick local check:

```bash
git log --all -- path/to/secret.file
git grep -n "SECRET_PATTERN" $(git rev-list --all)
```

Be careful: large grep across all history can be slow in big repos.

---

## 8. Large File Cleanup Runbook

Find large objects:

```bash
git count-objects -vH
git rev-list --objects --all
```

Typical cleanup process:

1. Identify large paths or object IDs.
2. Decide whether they should move to Git LFS or external storage.
3. Freeze pushes or coordinate with active contributors.
4. Rewrite history in a mirror/clean clone.
5. Recreate protected refs/tags as required.
6. Force-update remote only with approval.
7. Ask collaborators to reclone or perform exact cleanup instructions.
8. Run Git maintenance.
9. Add LFS tracking or prevention rules.

Use Git LFS for future large binary assets:

```bash
git lfs install
git lfs track "*.zip"
git add .gitattributes
git commit -m "Track large assets with Git LFS"
```

---

## 9. Using `git filter-repo`

Common pattern in a fresh clone or mirror:

```bash
git clone --mirror git@github.com:ORG/REPO.git repo-cleanup.git
cd repo-cleanup.git
git filter-repo --path path/to/secret.file --invert-paths
```

Remove multiple paths:

```bash
git filter-repo \
  --path secret.env \
  --path old-dumps/ \
  --invert-paths
```

Replace text from history:

```bash
git filter-repo --replace-text replacements.txt
```

Caution:

- run in a clean clone
- back up before rewriting
- do not improvise on the only copy
- coordinate protected branch/ruleset bypass
- understand tag impact
- expect all commit IDs after affected commits to change

---

## 10. After Rewrite

Server-side:

- update protected branches through approved bypass
- update tags if required
- delete stale refs
- invalidate caches if applicable
- notify teams
- verify secret scanning

Developer-side:

Best option:

```bash
git clone git@github.com:ORG/REPO.git
```

Risky option:

```bash
git fetch --all --prune
git reset --hard origin/main
git gc --prune=now
```

Do not let old branches be pushed back:

```bash
git push origin old-main:main
```

That can reintroduce old history.

---

## 11. Tags, Releases, And Provenance

History rewrite can affect:

- release tags
- GitHub Releases
- source archives
- CI artifact provenance
- SBOMs and attestations
- deployment records
- changelog comparisons

For released software, prefer:

- rotate secrets
- revoke artifacts if needed
- publish a new clean release
- explain what changed
- avoid silently moving public release tags

Strong release answer:

> If consumers already depend on a tag, I prefer a new patch release and clear advisory over silently moving the tag.

---

## 12. Recovery If Rewrite Goes Wrong

Before rewriting:

```bash
git branch backup/main origin/main
git tag backup-before-cleanup-2026-07-02 origin/main
git bundle create repo-before-cleanup.bundle --all
```

If local rebase went wrong:

```bash
git reflog
git reset --hard <old-good-sha>
```

If remote was force-updated incorrectly:

- stop pushes
- locate previous remote SHA from audit logs, reflog, teammates, or backup bundle
- restore via controlled force push
- preserve incident record

---

## 13. Common Mistakes

| Mistake | Better Approach |
|---|---|
| deleting secret file but not rotating secret | rotate first |
| rewriting shared history without announcing | freeze, backup, coordinate |
| using plain `--force` | use `--force-with-lease` for feature branches |
| moving public release tags silently | publish replacement tag/release when possible |
| cleaning only main and forgetting tags | rewrite all relevant refs |
| asking developers to "just pull" after major rewrite | provide reclone/reset instructions |
| using cleanup tool on only working copy | use fresh clone/mirror and backup |

---

## 14. Practical Question

> A developer pushed a 500 MB zip file and a real API key to `main`. What do you do?

---

## 15. Strong Answer

I split the response into security and repository cleanup.

First, I rotate/revoke the API key because history cleanup does not make it safe. I identify exposure in clones, forks, CI logs, artifacts, and audit logs. Then I remove the secret from current code and add prevention with `.gitignore`, secret scanning, and push protection.

For the 500 MB file, I check whether it was only in a feature branch or already merged into main. Since it hit `main`, I coordinate a repository cleanup window. I back up the repo, use a clean mirror clone and a vetted history rewrite tool such as `git filter-repo`, rewrite affected refs/tags intentionally, force-update through an approved bypass, and tell developers to reclone or follow exact reset/gc instructions. For future assets, I add Git LFS or external artifact storage.

---

## 16. Revision Notes

- One-line summary: Private history rewrite is normal; shared history rewrite is a coordinated production change.
- Three keywords: rotate, backup, coordinate.
- One interview trap: thinking removing a secret from Git history removes the breach.
- One memory trick: "Security first, Git cleanup second."

---

## 17. Official Source Notes

- Git filter-repo project: <https://github.com/newren/git-filter-repo>
- Git filter-branch docs: <https://git-scm.com/docs/git-filter-branch>
- Git bundle docs: <https://git-scm.com/docs/git-bundle>
- Git garbage collection docs: <https://git-scm.com/docs/git-gc>
- Git maintenance docs: <https://git-scm.com/docs/git-maintenance>
- GitHub secret scanning docs: <https://docs.github.com/en/code-security/secret-scanning>
- GitHub LFS docs: <https://docs.github.com/en/repositories/working-with-files/managing-large-files/about-git-large-file-storage>
