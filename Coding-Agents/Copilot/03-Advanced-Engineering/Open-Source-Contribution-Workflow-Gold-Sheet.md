# Open-Source Contribution Workflow — Gold Sheet

> **Track**: Copilot Mastery Track — Group 3: Advanced Engineering
> **File**: Gap Fill (Track File #20d)
> **Audience**: Developers contributing to open-source projects with Copilot assistance
> **Read after**: Personal-Project-Bootstrap-Gold-Sheet.md

---

## 1. The OSS Contribution Mental Model

```
Key difference from personal projects:
  Personal: you know the codebase.
  OSS: you're a stranger in someone else's house.

Rules change:
  1. Understand before changing — always read CONTRIBUTING.md first
  2. Match existing patterns EXACTLY — no introducing "improvements" they didn't ask for
  3. Tests required — most projects won't merge without them
  4. Small PRs — large PRs take months to review; small PRs get merged fast
  5. One PR = one concern — don't bundle unrelated changes
  6. Respect the license — check what Copilot-generated code's license implications are

Copilot's role:
  - Understand the codebase fast (codebase-navigator agent)
  - Match existing coding patterns (pattern-reference prompt)
  - Generate tests matching the project's test conventions
  - Write clear commit messages and PR descriptions
  - NOT: rewrite the project, introduce new dependencies, or "improve" unrelated code
```

---

## 2. Phase 1 — Finding and Understanding the Project

### Step 1 — Understand Before Touching

```bash
# Fork and clone
gh repo fork owner/repo --clone
cd repo

# Read these files FIRST before opening VS Code:
cat CONTRIBUTING.md
cat CODE_OF_CONDUCT.md
cat .github/PULL_REQUEST_TEMPLATE.md   # if exists
ls .github/                            # see what's there

# Open in VS Code
code .
```

### Step 2 — Copilot Codebase Analysis

```
In Chat with #codebase:

"I'm about to contribute to this open-source project. 
Give me:
1. Architecture pattern used (layered/hexagonal/event-driven/etc.)
2. Coding conventions visible in the code (naming, patterns, style)
3. Test framework and testing conventions
4. How to run tests locally (from any test file you can find)
5. The most important file I should read before making any change

Do NOT suggest improvements — I want to understand, not change."
```

---

## 3. Phase 2 — Choosing the Right Issue

### What Makes a Good First Issue

```
Look for issues labeled:
  good first issue      — explicitly marked for newcomers
  help wanted           — maintainers want outside help
  bug                   — concrete problem with clear expected behavior
  documentation         — low risk, high impact

Avoid as a first contribution:
  enhancement           — often requires design discussion first
  refactoring           — high conflict potential with existing work
  performance           — requires benchmarks and profiling context
  breaking change       — requires deep understanding of all consumers
```

### Claim the Issue

```
Comment on the issue BEFORE writing code:
  "Hi, I'd like to work on this. My approach would be [brief description].
  Does this align with what you had in mind? Any constraints I should know?"

Wait for maintainer confirmation before writing code.
This saves you from building something they won't merge.
```

---

## 4. Phase 3 — Understanding the Specific Area to Change

### Targeted Codebase Analysis

```
"I'm implementing [issue description] in this project.

Using #codebase, help me understand:
1. Which files are most relevant to [the feature area]?
2. What is the existing pattern for [the thing I need to do]?
3. Are there existing tests I should look at for conventions?
4. What would break if I change [specific area]?

Cite specific file paths for every answer."
```

### Pattern Matching (Critical for OSS)

```
"Looking at #file:[existing implementation of similar feature]:
What is the exact pattern used for:
- Error handling in this module?
- How is the database/storage accessed?
- What naming convention is used for [thing]?
- How are tests structured?

I need to match this EXACTLY in my new code."
```

---

## 5. Phase 4 — Implementing with Pattern Matching

### The OSS Implementation Prompt

```
"Implement [the issue/feature] for this project.

Existing pattern to match: #file:[most relevant existing file]
This is the template — my new code must look IDENTICAL in style.

Requirements (from the issue):
  [paste the issue requirements]

Constraints (non-negotiable for OSS):
  - Match the existing code style exactly (indentation, naming, patterns)
  - Do NOT introduce new external dependencies
  - Do NOT change any files outside the scope of this issue
  - Do NOT add error handling patterns that don't exist elsewhere in the codebase
  - Tests must use the same framework and fixtures as existing tests

Plan first: which files to create/modify, one sentence each.
Wait for my approval."
```

---

## 6. Phase 5 — Tests That Match the Project

### OSS Test Generation

```
"Generate tests for my new [feature/fix] matching the project's test conventions.

Existing test to use as template: #file:[closest existing test file]

My implementation: #file:[your new file]

Requirements:
  - Same testing framework as the existing test file
  - Same fixture patterns (don't create new fixtures if existing ones work)
  - Same assertion style
  - Cover: happy path, the error case from the issue, edge cases
  - Do NOT: use fixtures that don't exist in this project
  - Do NOT: add test dependencies not already in the project"
```

---

## 7. Phase 6 — PR Preparation

### Pre-PR Checklist for OSS Contributions

```
[ ] Issue referenced in branch name: git checkout -b fix/123-descriptive-name
[ ] All existing tests pass: [project's test command]
[ ] New tests added for your change
[ ] Code style matches project (run their linter: [linter command from CONTRIBUTING])
[ ] No unrelated changes snuck in (git diff — review carefully)
[ ] PR is small and focused (one issue = one PR)
```

### OSS PR Description Prompt

```
"/write-pr-description
Context: this is a contribution to an open-source project
Issue: #[issue number] — [brief issue description]

Format exactly as required by the project (check .github/PULL_REQUEST_TEMPLATE.md):
[or use our standard template if no template exists]

Additional OSS-specific sections:
## Related Issue
Fixes #[number]

## How I Tested This
[Exact commands another developer can run to verify]

## Checklist
- [ ] Tests pass
- [ ] New tests added
- [ ] Documentation updated (if applicable)
- [ ] Follows project contribution guidelines"
```

---

## 8. Phase 7 — Responding to Review Feedback

### Using Copilot for Review Response

```
"The maintainer left this review comment on my OSS PR:
[paste the comment]

On this code: #selection

Help me:
1. Understand what they're asking for (if not clear)
2. Draft the change they're requesting
3. Write a polite response to their comment explaining what I changed

Constraint: don't change anything beyond what the reviewer requested."
```

---

## 9. OSS-Specific Safe Usage Rules

```
License considerations:
  GitHub Copilot is trained on public code, including open-source.
  Some generated code may resemble GPL-licensed code.
  For projects with restrictive licenses: verify generated code doesn't replicate
  recognizable GPL-licensed patterns. When in doubt: ask the maintainer.

What to tell maintainers (if asked):
  "I used GitHub Copilot as an AI assistant for code generation.
  I reviewed all generated code for correctness, security, and style."
  Most maintainers are fine with this. Some projects prohibit AI assistance —
  check CONTRIBUTING.md for any AI policy before submitting.

Contribution attribution:
  Your commit is yours. Copilot is a tool, like an IDE.
  You are responsible for reviewing and accepting everything that goes in the PR.
```

---

## 10. Quick Command Reference for OSS

```bash
# Fork and set up upstream
gh repo fork owner/repo --clone
git remote add upstream https://github.com/owner/repo.git

# Keep fork in sync
git fetch upstream
git checkout main
git merge upstream/main
git push origin main

# Work on issue
git checkout -b fix/123-issue-title
# ... implement ...
git add . && git commit -m "fix: descriptive message"
git push origin fix/123-issue-title
gh pr create --web

# Update PR after review feedback
git add .
git commit -m "address review: [specific change]"
git push origin fix/123-issue-title
# PR updates automatically
```

---

## 11. Revision Checklist

- [ ] Reads CONTRIBUTING.md before writing any code
- [ ] Comments on issue and gets maintainer buy-in before implementing
- [ ] Uses #codebase to understand patterns before writing new code
- [ ] Matches existing test framework and fixture patterns exactly
- [ ] PR is small and focused (one issue = one PR)
- [ ] All existing tests pass before submitting
- [ ] Knows OSS license considerations for Copilot-generated code
- [ ] Can respond to review feedback using Copilot without over-changing
