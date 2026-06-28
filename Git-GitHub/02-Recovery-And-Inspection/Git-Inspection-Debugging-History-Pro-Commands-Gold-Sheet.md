# Git Inspection, Debugging, and History Pro Commands Gold Sheet

> Goal: become the person who can read Git history like a production incident timeline.

---

## 0. How To Read This Doc

This document is command-first.

Use it when you need to answer:

```text
What changed?
Who changed it?
When did it change?
Which commit introduced the bug?
How is my branch different from main?
What changed between PR revision 1 and revision 2?
Which tag/release contains this commit?
```

Daily Git makes changes. Pro Git investigates changes.

---

## 1. Intuition

Git history is an event log.

Each commit says:

```text
snapshot + parent pointer + author + message + timestamp
```

Inspection commands are different lenses:

```text
log        -> timeline
show       -> one commit
diff       -> file/content difference
blame      -> line ownership
bisect     -> binary search for bug commit
grep       -> search tracked content
tag        -> named release points
range-diff -> compare two versions of a commit series
```

---

## 2. Definition

- Definition: Git inspection is the use of read-only and diagnostic commands to understand repository history, branch differences, ownership, and bug origins.
- Category: Debugging, release investigation, code review, incident response.
- Core idea: inspect before changing.

---

## 3. Why It Exists

Large repositories have thousands of commits and many contributors.

Without inspection commands:

- code review becomes guesswork
- incident debugging becomes slow
- release notes become manual
- ownership is unclear
- regressions are hard to isolate
- rebases and PR updates are hard to verify

Git's history tools let you narrow the search space quickly.

---

## 4. Reality

In real teams, these commands are used for:

- reviewing PRs locally
- debugging production incidents
- finding regression commits
- auditing sensitive code changes
- generating release notes
- comparing release branches
- checking whether a hotfix reached production
- understanding unfamiliar code

Senior engineers often start with:

```bash
git status -sb
git log --oneline --decorate --graph -20
git diff origin/main...HEAD
```

before making changes.

---

## 5. How It Works

Git inspection reads objects and references:

```text
commit object
  tree snapshot
  parent commit(s)
  author metadata
  commit message

branch
  name pointing to a commit

tag
  stable name pointing to a commit or tag object

HEAD
  current checked-out commit or branch reference
```

Inspection commands usually do not mutate repository state.

Exception:

```text
git bisect changes checkout state while searching.
```

---

## 6. What Problem It Solves

- Primary problem solved: finding the truth in repository history.
- Secondary benefits: faster reviews, faster debugging, safer releases.
- Systems impact: reduces incident MTTR and review risk.

---

## 7. When To Rely On It

Use this doc when:

- a PR is too large and you need a clean diff
- CI broke and you need the likely commit
- a file changed unexpectedly
- a line looks suspicious
- a branch has diverged
- a release contains unknown changes
- a rebase changed commit order/content
- you need to compare branch versions

---

## 8. When Not To Use It

Do not overuse history inspection as a substitute for:

- tests
- observability
- code owners
- commit discipline
- good PR descriptions

Example:

```text
git blame tells who last touched a line.
It does not prove who caused a bug.
```

Use blame as context, not as accusation.

---

## 9. Pros and Cons

| Pros | Cons |
|---|---|
| Fast root-cause narrowing | Can be misread without context |
| Mostly read-only and safe | Large histories can be noisy |
| Helps code review and incident response | Squash merges can reduce line-by-line story |
| `bisect` finds regression commits efficiently | Requires a reliable test command |
| `range-diff` validates rebased PR revisions | Advanced syntax needs practice |

---

## 10. Trade-offs and Common Mistakes

### Trade-offs

- `git log` is broad but may hide file-level detail.
- `git show` is precise but only for one commit.
- `git diff main..branch` and `git diff main...branch` answer different questions.
- `git blame` finds last modifier, not necessarily root cause.
- `git bisect` is powerful but only as good as the test used.

### Common Mistakes

Mistake:

```bash
git diff main..feature
```

when reviewing PR changes.

Better approach:

```bash
git diff main...feature
```

Why:

```text
Three-dot diff compares the feature branch against the merge base with main.
That usually matches what a PR shows.
```

Mistake:

```bash
git blame file
```

and assuming the person caused the bug.

Better approach:

```bash
git blame -L 50,90 file
git show <commit>
git log --follow -- file
```

---

## 11. Key Commands

### Compact Timeline

```bash
git log --oneline --decorate -10
```

Use for:

```text
What are the latest commits?
Which branch/tag points here?
```

### Graph View

```bash
git log --oneline --decorate --graph --all -30
```

Use for:

```text
How did branches merge or diverge?
```

### Commit With Changed Files

```bash
git log --stat -5
```

### Commit With Patch

```bash
git log -p -3
```

### Search Commit Messages

```bash
git log --grep="payment"
```

### Search Commits That Touched A File

```bash
git log --oneline -- src/main/java/com/app/PaymentService.java
```

### Follow File Across Renames

```bash
git log --follow -- src/main/java/com/app/PaymentService.java
```

### Show One Commit

```bash
git show <commit>
```

Only stats:

```bash
git show --stat <commit>
```

Only file names:

```bash
git show --name-only <commit>
```

### Show Current Branch Commit

```bash
git show HEAD
```

Previous commit:

```bash
git show HEAD~1
```

### Compare Working Tree To Last Commit

```bash
git diff
```

### Compare Staged Changes

```bash
git diff --staged
```

### Compare Branch To Main For PR Review

```bash
git fetch origin
git diff origin/main...HEAD
```

### Compare Two Branch Tips

```bash
git diff origin/main..HEAD
```

Meaning:

```text
Two-dot compares the two endpoint commits directly.
Three-dot compares your branch against merge base with main.
```

### List Commits In Your Branch Not In Main

```bash
git log --oneline origin/main..HEAD
```

### Find Merge Base

```bash
git merge-base origin/main HEAD
```

Then inspect:

```bash
git show $(git merge-base origin/main HEAD)
```

If your shell does not support command substitution in a safe context, run the first command, copy the SHA, then:

```bash
git show <merge-base-sha>
```

### Blame A File

```bash
git blame <file>
```

Specific lines:

```bash
git blame -L 120,170 <file>
```

Ignore whitespace-only changes:

```bash
git blame -w <file>
```

### Search Tracked Files

```bash
git grep "OrderStatus"
```

With line numbers:

```bash
git grep -n "OrderStatus"
```

Search only Java files:

```bash
git grep -n "OrderStatus" -- "*.java"
```

### Find Commits That Added Or Removed Text

```bash
git log -S "calculateDiscount" --oneline
```

Show patches:

```bash
git log -S "calculateDiscount" -p
```

Regex search in patches:

```bash
git log -G "discount.*amount" -p
```

### Bisect A Regression

Start:

```bash
git bisect start
```

Mark current bad commit:

```bash
git bisect bad
```

Mark known good commit:

```bash
git bisect good <known-good-sha>
```

Run test manually at each step:

```bash
mvn test
git bisect good
```

or:

```bash
mvn test
git bisect bad
```

End:

```bash
git bisect reset
```

Automated:

```bash
git bisect start
git bisect bad
git bisect good <known-good-sha>
git bisect run mvn test
git bisect reset
```

### Tags And Release Inspection

List tags:

```bash
git tag
```

Create annotated tag:

```bash
git tag -a v1.4.0 -m "Release v1.4.0"
```

Push tag:

```bash
git push origin v1.4.0
```

Show tag:

```bash
git show v1.4.0
```

Find nearest tag:

```bash
git describe --tags
```

### Release Notes Between Tags

```bash
git log --oneline v1.3.0..v1.4.0
```

With authors:

```bash
git shortlog -sn v1.3.0..v1.4.0
```

### Check Whether Commit Is In Branch

```bash
git branch --contains <commit>
```

Remote branches:

```bash
git branch -r --contains <commit>
```

### Compare Rebased Commit Series

```bash
git range-diff origin/main...old-branch origin/main...new-branch
```

Use when:

```text
You rebased or edited a PR and want to verify that the logical changes are still the same.
```

### Resolve Revision Names

```bash
git rev-parse HEAD
```

Current branch:

```bash
git branch --show-current
```

Root directory of repo:

```bash
git rev-parse --show-toplevel
```

---

## 12. Failure Modes

### Failure Mode 1: Wrong Diff Range

Symptom:

```text
Diff shows unrelated main branch changes.
```

Fix:

```bash
git fetch origin
git diff origin/main...HEAD
```

### Failure Mode 2: Bisect Lands On A Build-Broken Commit

Symptom:

```text
Test cannot run for unrelated build issue.
```

Use:

```bash
git bisect skip
```

### Failure Mode 3: Blame Points To Formatting Commit

Symptom:

```text
git blame shows a code formatting commit.
```

Try:

```bash
git blame -w <file>
git log --follow -p -- <file>
```

### Failure Mode 4: Tag Missing Locally

Symptom:

```text
git describe --tags cannot find expected release tag.
```

Fix:

```bash
git fetch --tags
git tag
```

---

## 13. Scenario

### Scenario: Production Bug Started Sometime Last Week

You know:

```text
main is currently bad
commit 9f8e7d6 from last week was good
the test command is ./gradlew test --tests PaymentRegressionTest
```

Commands:

```bash
git switch main
git pull --ff-only
git bisect start
git bisect bad
git bisect good 9f8e7d6
git bisect run ./gradlew test --tests PaymentRegressionTest
git bisect reset
```

Then inspect the found commit:

```bash
git show <bad-commit>
git blame -L 80,130 src/main/java/com/app/PaymentService.java
```

---

## 14. Code Sample

### Java Mental Model: Bisect

```java
public class BisectMentalModel {
    public static void main(String[] args) {
        int firstCommit = 1;
        int lastCommit = 16;
        int bugIntroducedAt = 11;

        while (firstCommit < lastCommit) {
            int mid = (firstCommit + lastCommit) / 2;
            boolean testFails = mid >= bugIntroducedAt;

            System.out.println("Testing commit " + mid + ": " + (testFails ? "bad" : "good"));

            if (testFails) {
                lastCommit = mid;
            } else {
                firstCommit = mid + 1;
            }
        }

        System.out.println("First bad commit: " + firstCommit);
    }
}
```

---

## 15. Mini Program / Simulation

### Python Simulation: Two-Dot vs Three-Dot

```python
main = ["A", "B", "C", "D"]
feature_base = "B"
feature = ["A", "B", "X", "Y"]

two_dot = "Compare D directly with Y"
three_dot = "Compare merge-base B with Y"

print("main:", main)
print("feature:", feature)
print("two-dot:", two_dot)
print("three-dot:", three_dot)
```

Interview memory:

```text
Two-dot: endpoint to endpoint.
Three-dot: merge base to feature tip.
PR review usually wants three-dot.
```

---

## 16. Practical Question

> A bug appears in production and nobody knows which commit caused it. How do you investigate using Git?

---

## 17. Strong Answer

First I would find a known good commit, ideally the previous production tag:

```bash
git tag
git log --oneline --decorate -20
```

Then I would use `git bisect` with a reliable regression test:

```bash
git bisect start
git bisect bad
git bisect good <known-good-sha>
git bisect run <test-command>
git bisect reset
```

After Git identifies the first bad commit, I would inspect it:

```bash
git show <bad-commit>
git log --oneline -- <affected-file>
git blame -L <start>,<end> <affected-file>
```

Then I would revert or patch depending on urgency. If production is broken, I would prefer a safe revert through PR or emergency process rather than rewriting shared history.

---

## 18. Revision Notes

- One-line summary: use `log`, `show`, `diff`, `blame`, and `bisect` to turn Git history into evidence.
- Three keywords: timeline, diff, regression.
- One interview trap: blaming the person from `git blame` without inspecting the actual commit context.
- One memory trick: "Inspect before you edit."

---

## 19. Pro Command Recipes

### Review My PR Locally

```bash
git fetch origin
git switch feature/my-work
git diff origin/main...HEAD
git log --oneline origin/main..HEAD
```

### Find Who Changed A Method

```bash
git log -S "methodName" --oneline --all
git log -S "methodName" -p -- <file>
```

### Find Which Release Has A Commit

```bash
git tag --contains <commit>
git branch -r --contains <commit>
```

### Compare Release Branches

```bash
git fetch --all --tags
git log --oneline release/1.4..release/1.5
git diff release/1.4..release/1.5
```

### Verify Rebase Did Not Lose Changes

```bash
git range-diff origin/main...old-feature origin/main...feature
```

---

## 20. Official Source Notes

- Git inspection commands such as `log`, `show`, `diff`, `blame`, `bisect`, `grep`, `tag`, `describe`, `range-diff`, `rev-parse`, and `merge-base` are documented in the official Git reference: <https://git-scm.com/docs>

