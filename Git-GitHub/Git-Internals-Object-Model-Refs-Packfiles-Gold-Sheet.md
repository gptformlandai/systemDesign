# Git Internals Object Model Refs Packfiles Gold Sheet

> Track: Git and GitHub Command Mastery - Senior / MAANG Internals Layer

Goal: explain Git beyond commands: objects, refs, HEAD, index, DAG, merge bases, packfiles, reflog, refspecs, garbage collection, and why recovery commands work.

---

## 0. How To Read This

Use this after the local foundations, branching, undo/recovery, and inspection sheets.

Senior mental model:

```text
working tree -> index -> commit object -> refs -> remote refs -> packed storage -> maintenance/recovery
```

Interview rule:

```text
Do not only say the command. Say which pointer, object, file state, or remote ref the command changes.
```

---

# Topic 1: Git Internals

## 1. Intuition

Git is a content-addressed database plus movable names.

- Objects are immutable snapshots and metadata.
- Refs are names that point to objects.
- Branches are refs that move.
- HEAD tells Git what you currently have checked out.
- The index is the next commit being assembled.

Simple picture:

```text
objects are the data
refs are labels
HEAD is your current label or commit
index is your next snapshot
working tree is files you can edit
```

---

## 2. Definition

- Definition: Git internals are the object database, references, index, working tree, and storage mechanisms that make Git history, branching, merging, and recovery work.
- Category: distributed version control internals.
- Core idea: Git stores immutable objects and moves refs to create history.

---

## 3. Why It Exists

Git internals matter because advanced workflows require knowing what changes:

- `commit` creates objects and moves the current branch.
- `reset` moves a ref and optionally changes index/working tree.
- `checkout`/`switch` changes HEAD and files.
- `rebase` copies commits onto a new base.
- `merge` creates a commit with multiple parents.
- `fetch` updates remote-tracking refs.
- `push` asks a remote to update refs.
- `reflog` records previous ref positions.

Without this model, commands feel magical and dangerous.

---

## 4. Git Object Model

Git primarily stores four object types:

| Object | Meaning |
|---|---|
| blob | file content |
| tree | directory listing: names, modes, object ids |
| commit | pointer to tree, parent commits, author/committer metadata, message |
| tag | annotated tag object pointing to another object |

A commit is not a diff. A commit points to a full tree snapshot.

```text
commit
  -> tree
      -> blob: src/App.java
      -> tree: docs/
          -> blob: README.md
  -> parent commit(s)
```

Interview line:

```text
Git stores snapshots efficiently, not patches as the primary model. Diffs are computed between snapshots.
```

---

## 5. Content Addressing

Git object ids are derived from object content.

Implications:

- same content produces same object id
- changing one byte creates a different object id
- commit id changes if tree, parent, author metadata, timestamp, or message changes
- rebasing changes commit ids because parent commits change

Strong answer:

```text
A commit hash identifies the commit contents and metadata, including its parent pointer. That is why rewriting history changes hashes even when the file diff looks similar.
```

---

## 6. Commit DAG

Git history is a directed acyclic graph.

```text
A---B---C main
     \
      D---E feature
```

- Each commit points backward to parent commits.
- A normal commit has one parent.
- A merge commit has two or more parents.
- A root commit has no parent.
- Branch names point to commits.

Terms:

| Term | Meaning |
|---|---|
| ancestor | commit reachable by following parent links |
| descendant | commit that has another commit in its parent chain |
| merge base | best common ancestor for merge/diff operations |
| reachable | object can be found from refs or reflogs |

---

## 7. Refs And Branches

A ref is a name that points to an object id.

Common refs:

```text
refs/heads/main              local branch
refs/heads/feature/login     local branch
refs/remotes/origin/main     remote-tracking branch
refs/tags/v1.2.0             tag
```

Branch rule:

```text
A branch is just a movable ref pointing to a commit.
```

When you commit on a branch:

```text
new commit created -> branch ref moves to new commit -> HEAD still points to branch
```

---

## 8. HEAD

HEAD usually points to the current branch.

```text
HEAD -> refs/heads/feature/login -> commit E
```

Detached HEAD means HEAD points directly to a commit:

```text
HEAD -> commit B
```

Detached HEAD is useful for inspection but risky for new work unless you create a branch.

Safe recovery:

```bash
git switch -c rescue/my-work
```

Strong answer:

```text
Detached HEAD is not data loss by itself. The risk is creating commits that no branch points to. Reflog can often rescue them.
```

---

## 9. Index / Staging Area

The index is Git's proposed next snapshot.

```text
working tree: files on disk
index: selected content for next commit
HEAD: last committed snapshot
```

Command effects:

| Command | Working Tree | Index | HEAD/Branch |
|---|---|---|---|
| `git add file` | unchanged | updates staged content | unchanged |
| `git commit` | unchanged | becomes commit tree | branch moves |
| `git restore file` | resets file from index or HEAD | usually unchanged | unchanged |
| `git restore --staged file` | unchanged | resets index from HEAD | unchanged |
| `git reset --mixed HEAD~1` | unchanged | resets index | branch moves |
| `git reset --hard HEAD~1` | resets files | resets index | branch moves |

Interview line:

```text
Most confusing Git commands are easier once I ask which of the three states changes: HEAD, index, or working tree.
```

---

## 10. Merge Internals

A merge finds the merge base and combines changes from both sides.

```text
A---B---C main
     \
      D---E feature
```

Merge feature into main:

1. Find merge base `B`.
2. Compare `B -> C` and `B -> E`.
3. Auto-merge non-conflicting changes.
4. Ask user to resolve conflicts.
5. Create merge commit with parents `C` and `E`.

Fast-forward merge:

```text
main can move directly to feature because main has no unique commits.
```

No merge commit is required.

---

## 11. Rebase Internals

Rebase copies commits onto a new base.

```text
before:
A---B---C main
     \
      D---E feature

after rebase feature onto main:
A---B---C main
         \
          D'---E' feature
```

Important:

- `D'` and `E'` are new commits.
- Commit ids change because parent ids change.
- Old commits may remain reachable through reflog for a while.
- Rebase is clean for local/private branches.
- Rebase can disrupt shared branches because it rewrites published history.

---

## 12. Cherry-Pick Internals

Cherry-pick copies the patch introduced by one commit and creates a new commit on current HEAD.

```text
source commit X -> apply its diff -> create new commit X'
```

Use cases:

- hotfix backport
- release branch fix
- recover one useful commit from another branch

Caution:

```text
Cherry-pick duplicates changes as a new commit. Future merges may need careful conflict handling if the same change exists in multiple branches.
```

---

## 13. Fetch, Pull, Push, And Refspecs

`git fetch` downloads objects and updates remote-tracking refs.

```text
origin/main on server -> refs/remotes/origin/main locally
```

It does not change your current local branch by itself.

`git pull` is roughly:

```text
git fetch + git merge
```

or if configured:

```text
git fetch + git rebase
```

A refspec maps source refs to destination refs.

Common push idea:

```text
local feature branch -> remote refs/heads/feature branch
```

Strong answer:

```text
Fetch is safe because it updates remote-tracking refs. Pull also integrates into the current branch, so I inspect before pulling when the situation is risky.
```

---

## 14. Two-Dot And Three-Dot Thinking

Common comparisons:

```bash
git log main..feature
```

Means commits reachable from `feature` but not from `main`.

```bash
git diff main...feature
```

Means diff from merge base of `main` and `feature` to `feature`.

PR review usually wants three-dot thinking because it shows what the branch introduced since it diverged.

---

## 15. Reflog Internals

Reflog records local movements of refs.

Examples:

- branch moved by commit
- branch moved by reset
- HEAD changed by checkout/switch
- rebase changed branch pointer

Useful commands:

```bash
git reflog
git reflog show main
git branch rescue <sha-from-reflog>
```

Boundaries:

- reflog is local
- reflog expires eventually
- reflog is not a shared backup
- remote-tracking refs may have reflogs depending on config/environment

---

## 16. Packfiles And Git Storage

Loose objects are individual files in `.git/objects`.

Packfiles store many objects together efficiently.

Why packfiles exist:

- reduce disk usage
- improve clone/fetch efficiency
- delta-compress related objects
- keep large histories manageable

Commands to know:

```bash
git count-objects -v
git gc
git maintenance run
git fsck
```

Caution:

```text
Do not run cleanup commands blindly during active recovery. First create a branch or tag to preserve commits you care about.
```

---

## 17. Garbage Collection And Reachability

Git can eventually prune unreachable objects.

An object is normally safe if reachable from:

- local branch
- remote-tracking branch
- tag
- stash ref
- reflog entry

Risky flow:

```text
commit -> reset branch away -> no ref points to commit -> reflog expires -> object can be pruned
```

Safe recovery habit:

```bash
git branch rescue/lost-work <sha>
```

---

## 18. Tags Internals

Lightweight tag:

```text
ref name directly points to commit
```

Annotated tag:

```text
ref points to tag object -> tag object points to commit
```

For releases, prefer annotated tags because they carry tagger, date, message, and can be signed.

```bash
git tag -a v1.2.0 -m "Release v1.2.0"
git push origin v1.2.0
```

---

## 19. Common Internal Misunderstandings

| Misunderstanding | Better Model |
|---|---|
| A branch contains commits | A branch points to a commit; reachability gives the commit set |
| A commit is a diff | A commit points to a tree snapshot and parent commits |
| Rebase moves commits | Rebase creates new commits with new parents |
| Fetch changes my local branch | Fetch updates remote-tracking refs |
| Reflog is in GitHub | Reflog is local history of ref movements |
| `reset --hard` deletes commits immediately | It moves refs and resets files; commits may be recoverable until pruned |
| A tag and branch are the same | Both are refs, but branches move; release tags should not move |

---

## 20. Failure Modes

### Failure Mode 1: Lost Commit After Reset

What happened:

```text
branch ref moved away from commit
```

Recovery:

```bash
git reflog
git branch rescue/lost-work <sha>
```

### Failure Mode 2: Detached HEAD Commit

What happened:

```text
commit exists but no branch points to it
```

Recovery:

```bash
git switch -c rescue/detached-work
```

### Failure Mode 3: Rebase Changed Hashes

What happened:

```text
new commits were created with new parent ids
```

Recovery/inspection:

```bash
git range-diff origin/main...feature@{1} origin/main...feature
```

### Failure Mode 4: Repository Corruption Suspected

Check:

```bash
git fsck
```

Then restore from remote, backup, or known good clone depending on damage.

---

## 21. Scenario

Prompt:

```text
You accidentally reset a local branch and lost three commits. Explain what happened and recover safely.
```

Strong answer:

```text
Reset moved the branch ref to an earlier commit and changed the index/working tree depending on the mode. The commits may still exist as unreachable objects, and the reflog likely has the previous branch position. I would avoid running cleanup, inspect `git reflog`, create a rescue branch at the old SHA, then compare the recovered branch before reintegrating the commits.
```

Commands:

```bash
git reflog
git branch rescue/lost-commits <old-sha>
git log --oneline --decorate rescue/lost-commits
git diff main...rescue/lost-commits
```

---

## 22. Senior Interview Checklist

You should be able to explain:

1. Why commit hashes change after rebase.
2. Why fetch is safer than pull.
3. Why a branch is a movable ref.
4. What HEAD points to.
5. What the index contains.
6. Why merge commits have multiple parents.
7. What a merge base is.
8. Why reflog can recover local lost commits.
9. Why packfiles exist.
10. What garbage collection can eventually remove.
11. Why annotated tags are better for releases.
12. What `reset --soft`, `--mixed`, and `--hard` change internally.

---

## 23. Official Source Notes

- Git internals book: https://git-scm.com/book/en/v2/Git-Internals-Plumbing-and-Porcelain
- Git objects: https://git-scm.com/book/en/v2/Git-Internals-Git-Objects
- Git references: https://git-scm.com/book/en/v2/Git-Internals-Git-References
- Git maintenance: https://git-scm.com/docs/git-maintenance
- Git garbage collection: https://git-scm.com/docs/git-gc
- Git fsck: https://git-scm.com/docs/git-fsck
