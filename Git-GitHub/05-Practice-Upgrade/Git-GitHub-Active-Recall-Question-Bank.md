# Git GitHub Active Recall Question Bank

> Track: Git and GitHub Command Mastery - Practice Upgrade  
> Mode: answer from memory before checking notes.

Goal: make Git/GitHub commands, safety rules, internals, and production workflows fast under interview pressure.

---

## 1. How To Use

Daily loop:

1. Pick 20 questions.
2. Answer aloud without notes.
3. Say the command and the safety boundary.
4. Mark Green, Yellow, or Red.
5. Repeat Red questions after 24 hours and 7 days.

Strong answer shape:

```text
situation -> inspect first -> command -> what it changes -> safety boundary -> recovery plan
```

---

## 2. Local Foundations

1. What are the working tree, staging area, and commit history?
2. What does `git status` tell you?
3. What does `git add` change?
4. What does `git commit` create?
5. What is the difference between `git diff` and `git diff --staged`?
6. What does `git restore file` do?
7. What does `git restore --staged file` do?
8. Why should you inspect before committing?
9. How do you commit only part of a file?
10. What is a good commit message?
11. How do you see the last commit?
12. How do you show changed files in a commit?
13. How do you check repo config?
14. What is the difference between local and global config?
15. How do you stop tracking a generated file already committed?

---

## 3. Branching Merging Rebasing

1. What is a branch internally?
2. What does `git switch -c feature/x` do?
3. What is a fast-forward merge?
4. What is a merge commit?
5. What is a merge base?
6. What does rebase do internally?
7. Why do commit hashes change after rebase?
8. When is rebase safe?
9. When is rebase risky?
10. How do you abort a merge?
11. How do you abort a rebase?
12. How do you resolve a conflict safely?
13. What do conflict markers mean?
14. When do you use `cherry-pick`?
15. What is the risk of cherry-picking across release branches?

---

## 4. Remote Collaboration GitHub Flow

1. What is `origin`?
2. What is an upstream branch?
3. What is the difference between `fetch` and `pull`?
4. Why is `git fetch` safer than `git pull`?
5. What does `git push -u origin HEAD` do?
6. What is a remote-tracking branch?
7. How do you inspect ahead/behind status?
8. What is a non-fast-forward push rejection?
9. How do you update a feature branch from main?
10. How do you sync a fork with upstream?
11. What is GitHub Flow?
12. Why protect main?
13. How do required checks affect PR merge?
14. How do you review a PR locally?
15. What should you inspect before pushing?

---

## 5. Undo Recovery Safety

1. `reset` vs `revert`?
2. `restore` vs `reset`?
3. What does `reset --soft` change?
4. What does `reset --mixed` change?
5. What does `reset --hard` change?
6. Why is `reset --hard` dangerous?
7. When should you use `revert` instead of `reset`?
8. What is reflog?
9. Why is reflog local?
10. How do you recover a lost local commit?
11. How do you undo the last local commit but keep changes?
12. How do you unstage a file?
13. How do you discard local changes to one file?
14. How do you use stash safely?
15. Why is `--force-with-lease` safer than `--force`?

---

## 6. Inspection Debugging History

1. How do you inspect commits on your branch not in main?
2. What does `git log main..feature` mean?
3. What does `git diff main...feature` mean?
4. What is the difference between two-dot and three-dot?
5. How do you find who last changed a line?
6. When do you use `git blame` carefully?
7. What does `git bisect` do?
8. How do you start a bisect?
9. How do you compare a PR before and after rebase?
10. What does `git range-diff` show?
11. How do you find commits touching a file?
12. How do you grep repository history?
13. How do you show file content from another commit?
14. How do you inspect a tag?
15. What should you check before deleting a branch?

---

## 7. GitHub CLI

1. What is the difference between `git` and `gh`?
2. How do you authenticate `gh`?
3. How do you create a PR from terminal?
4. How do you view PR checks?
5. How do you checkout a PR locally?
6. How do you review/comment/approve with `gh`?
7. How do you merge a PR with `gh`?
8. How do you list workflow runs?
9. How do you view failed CI logs?
10. How do you create a GitHub release?
11. When should you avoid scripting `gh` admin commands?
12. How do token scopes affect `gh`?
13. How do you create issues with labels?
14. How do you inspect repo metadata?
15. How does `gh` help in incident workflows?

---

## 8. Advanced Workflows

1. What is `git worktree`?
2. When is worktree better than stash?
3. What is a submodule?
4. Why are submodules tricky?
5. What is sparse checkout?
6. When is sparse checkout useful?
7. What is Git LFS?
8. When should you not use LFS?
9. What are hooks?
10. What is `.gitignore`?
11. What is `.gitattributes`?
12. What is `rerere`?
13. How do you remove a stale worktree?
14. How do you initialize submodules after clone?
15. How do you handle a large file committed without LFS?

---

## 9. Git Internals

1. What are blob, tree, commit, and tag objects?
2. Why is a commit not just a diff?
3. What does content-addressed storage mean?
4. Why does changing a commit message change the commit hash?
5. What is the commit DAG?
6. What is a ref?
7. What is HEAD?
8. What is detached HEAD?
9. What is the index internally?
10. What happens internally during merge?
11. What happens internally during rebase?
12. What is a refspec?
13. What does fetch update?
14. What are packfiles?
15. What can garbage collection eventually prune?

---

## 10. GitHub Governance Security

1. What does branch protection enforce?
2. What are GitHub rulesets?
3. What is CODEOWNERS?
4. Why must CODEOWNERS be paired with required owner review?
5. What required checks should protect main?
6. What is merge queue used for?
7. What merge strategy should a team choose?
8. How should repo permissions be assigned?
9. Why avoid broad personal access tokens?
10. When use GitHub Apps?
11. What does commit signing prove and not prove?
12. How should secrets committed to Git be handled?
13. Why protect `.github/workflows`?
14. What should be audited in GitHub?
15. How do you make a repo enterprise-ready?

---

## 11. Release Engineering

1. Annotated tag vs lightweight tag?
2. Why should release tags be immutable?
3. What is semantic versioning?
4. What is a release branch?
5. What is trunk-based release flow?
6. What is a hotfix?
7. What is a backport?
8. Why use `cherry-pick -x` for backports?
9. How do you generate release notes from tags?
10. How do you trace production artifact to Git commit?
11. Rollback vs revert vs roll forward?
12. Why protect release branches?
13. How do you handle a wrong pushed release tag?
14. What should a GitHub Release contain?
15. How do you ensure a hotfix reaches main?

---

## 12. Final Readiness Gate

You are ready when you can answer without notes:

1. Explain working tree, index, HEAD, branch ref, and commit object.
2. Choose safely between restore, reset, revert, stash, and reflog.
3. Resolve conflicts during merge/rebase and explain what happened.
4. Recover lost commits and explain reflog boundaries.
5. Update, review, and push a feature branch without damaging shared history.
6. Use GitHub CLI for PRs, checks, releases, and CI investigation.
7. Explain internals: object model, refs, DAG, merge base, packfiles.
8. Design GitHub governance for a large engineering org.
9. Run release/hotfix/backport workflows safely.
10. Communicate production Git incidents clearly.
