# Copilot Edits Mode — Gold Sheet

> **Track**: Copilot Mastery Track — Group 2: Intermediate Power User
> **File**: 3 of 7 (Track File #9)
> **Audience**: Developers ready to do multi-file editing with Copilot
> **Read after**: Prompt-Files-Slash-Commands-Gold-Sheet.md

---

## 1. Practical Impact Meter

| Topic | Impact | Why Developers Miss This |
|---|---|---|
| Edits vs Chat Ask — when to switch | ★★★★★ | Devs use Chat Ask for edits and manually copy output — much slower |
| Working set — adding the right files | ★★★★★ | Adding wrong/too many files produces irrelevant edits |
| Reviewing the diff — file by file | ★★★★★ | Accepting all without reading is how bugs get committed |
| Accept / Reject per hunk, not all at once | ★★★★☆ | Granular acceptance keeps the good parts and discards the bad |
| Edits for refactoring across 2-5 files | ★★★★☆ | This is where Edits shines — coordinated changes across small file sets |
| When Edits creates too many changes | ★★★★☆ | Scope creep in Edits mode; how to constrain to only what you asked |

---

## 2. Copilot Edits vs Chat Ask

### Must Know

```
Chat Ask:
  - Copilot generates text/code in the Chat panel
  - YOU manually copy and apply the changes to files
  - Changes are not directly in your editor
  - Good for: planning, exploration, learning, short snippets

Copilot Edits:
  - Copilot directly modifies files in your editor
  - You review a diff and accept/reject
  - Changes happen IN your files, not in the Chat panel
  - Good for: actual implementation, refactoring, multi-file updates

Rule: If you find yourself copying code from Chat into a file, switch to Edits mode.
```

### How to Open Edits Mode

```
1. Open Chat panel: Cmd+Shift+I
2. Click the "Edit" dropdown at the top of the Chat panel
   (or look for the "Edits" tab depending on VS Code version)
3. Add files to the working set (the files Copilot will edit)
4. Type your instruction
5. Review diffs, accept/reject
```

---

## 3. The Working Set — Critical Concept

### Must Know

```
The working set = the list of files Copilot is allowed to read AND modify.

Key rules:
  1. Only add files that are DIRECTLY relevant to the change
  2. Adding too many files causes Copilot to make unnecessary changes to unrelated files
  3. Adding too few files causes Copilot to make incomplete changes
  4. Copilot can READ files not in the working set if you reference them with #file

Good working sets:
  For "add validation to the create_user endpoint":
    + src/api/users.py     (the router — this will be edited)
    + src/schemas/user.py  (Pydantic schemas — may need new fields)
    (NOT: tests/, all of src/, entire project)

  For "update UserService to use the new EmailValidator class":
    + src/services/user_service.py
    + src/validators/email_validator.py  (if it already exists)
    (NOT: every file that imports UserService)
```

### Adding Files to Working Set

```
Method 1: Drag and drop files from the Explorer into the Edits panel
Method 2: Click "Add Files" in the Edits working set area
Method 3: Type #file:path/to/file in your Edits prompt — Copilot adds it
Method 4: Use "Add Open Files" to add all currently open editor tabs
```

---

## 4. Writing Effective Edits Instructions

### The Edits Prompt Pattern

```
[What to change] in [which files/classes/functions]
keeping [what to preserve]
do not [what to avoid]
```

### Examples

```
Good Edits instruction:
  "Add input validation to the create_user and update_user endpoints in the
  users router. Use Pydantic validators for email format and name length
  (2-100 chars). Raise HTTP 422 for validation errors with a descriptive message.
  Do not change the response schema or the database layer."

Working set: src/api/users.py, src/schemas/user.py

Why it works:
  - Scoped to specific functions in specific files
  - Clear behavior expected (Pydantic, HTTP 422)
  - Clear preservation rule (don't touch response schema or DB)
```

```
Good Edits instruction for refactoring:
  "Extract the email sending logic from UserService into a new EmailService class.
  UserService should call EmailService, not implement the logic directly.
  Keep the existing method signatures on UserService identical.
  Add a basic EmailService class with send_welcome and send_password_reset methods."

Working set: src/services/user_service.py (plus new file to be created)
```

---

## 5. Reviewing Diffs — The Non-Negotiable Step

### Review Workflow

```
After Copilot finishes making edits:

1. Copilot shows a diff view for each modified file.

2. For each file:
   - Read the removed lines (red) first — what was deleted?
   - Read the added lines (green) — what was added?
   - Ask: "Does this match what I asked for?"
   - Ask: "Did Copilot change anything I didn't ask it to change?"
   - Ask: "Are there any new imports I don't recognize?"
   - Ask: "Was any error handling removed?"

3. Accept options:
   - Accept all changes in the file (use sparingly — read first)
   - Accept individual hunks (recommended — keep what's right, reject what's wrong)
   - Reject all changes in the file (if the file's changes are wrong)

4. After reviewing all files:
   - Run the test suite
   - If tests fail: use git diff to see what changed and pinpoint the regression
```

### Red Flags in Diffs

```
Watch for these patterns that indicate Copilot overstepped:

1. Deleted error handling:
   - try/except blocks removed
   - Validation logic deleted
   → Reject the hunk; ask Copilot to add it back

2. Added new imports you didn't expect:
   - New library import (especially if you didn't ask for a new dependency)
   → Check the library exists and is safe

3. Changed method signatures:
   - Different parameter names or types
   - Added/removed parameters
   → Only accept if you explicitly asked for signature changes

4. Files outside your working set were modified:
   - This shouldn't happen in Edits mode but check
   → Review all modified files, not just the ones in your working set

5. Comments or docstrings removed:
   - Copilot sometimes removes documentation it considers redundant
   → Reject these — documentation is not Copilot's decision to remove
```

---

## 6. Edits Mode Best Practices

```
Best practice 1 — Commit before Edits:
  git add . && git commit -m "checkpoint: before edits session"
  If the edits produce wrong output: git checkout . to restore

Best practice 2 — One task per Edits session:
  "Add validation" and "refactor service layer" in one Edits session produces
  too many overlapping changes. Do one task, review and commit, then do the next.

Best practice 3 — Use constraints to limit scope:
  "Do not modify any existing tests" prevents Copilot from updating tests
  to match new (potentially wrong) behavior.

Best practice 4 — Run tests after accepting:
  pytest tests/unit/test_users.py -v
  If any test fails that was passing before: the edits broke something.

Best practice 5 — Iterative editing:
  If the first Edits result is 80% correct, don't reject all.
  Accept the 80%, note what's wrong, run another Edits pass for the remaining 20%.
```

---

## 7. Edits Mode vs Agent Mode — Choosing Correctly

```
Use Edits when:
  - You know exactly which files need to change (2-5 files)
  - The task is bounded and well-defined
  - You want to review a diff before any changes land
  - You don't need terminal commands (no running tests, no installs)
  - The task doesn't require planning — it's implementation only

Use Agent Mode when:
  - The task spans many files and you're not sure which ones
  - You need to run commands (install packages, run tests, execute scripts)
  - The task requires iterative steps (generate → test → fix → test again)
  - You want Copilot to figure out WHAT needs to change, not just make the change

Edits is safer and more controlled.
Agent Mode is more powerful but requires more oversight.
Default to Edits; escalate to Agent Mode when Edits can't complete the task.
```

---

## 8. Revision Checklist

- [ ] Can explain the difference between Chat Ask, Edits, and Agent Mode
- [ ] Knows what a "working set" is and how to populate it correctly
- [ ] Knows the signs of an overly large or overly small working set
- [ ] Can write an effective, scoped Edits mode instruction
- [ ] Reviews every diff file-by-file before accepting
- [ ] Knows the 5 red flags in a diff (deleted error handling, unexpected imports, etc.)
- [ ] Has the commit-before-edits habit established
- [ ] Knows when to use Edits vs Agent Mode
