# Claude Mock Workflow Scripts

> **Track**: Claude Mastery Track — Group 6: Practice Upgrade
> **File**: 2 of 5 (Track File #31)
> **Usage**: Timed drills to build Claude workflow muscle memory. Use a timer.

---

## Mock #1 — Morning Planning Sprint (10 min)

**Setup**: Pick one real task you're working on today.

**Drill**:
```
Step 1 (2 min): Open Claude Code. Paste your task description.
  /plan
  "Break this task into implementation steps.
  Using @codebase, identify which files are affected.
  Identify 3 risks or unknowns.
  Plan only — no code."

Step 2 (3 min): Review the plan. Find and correct one incorrect assumption.

Step 3 (3 min): For each step, identify the Claude mode:
  "Which mode is best for each step:
  CLI agent / subagent / slash command / Chat Ask?"

Step 4 (2 min): Open only the files for Step 1.
  Close everything else.
```

**Score**:
- [ ] Plan generated with steps and affected files
- [ ] At least one incorrect assumption caught and corrected
- [ ] Files opened match exactly what Step 1 needs
- [ ] Mode identified for each step

Score: ___/4

---

## Mock #2 — Test Generation Drill (15 min)

**Setup**: Use any function in your project that lacks tests.

**Drill**:
```
Step 1 (2 min): Select the function in your editor.

Step 2 (5 min): Generate tests using /test or the manual prompt.
  "Generate tests covering: happy path, 3+ error cases, 2 boundary values.
  Mocks: [external deps only — not own logic]"

Step 3 (3 min): Review tests critically.
  - Are there any tautological tests?
  - Add at least 1 test Claude missed.

Step 4 (3 min): Run the tests.
  If failing: diagnose with Claude before fixing manually.

Step 5 (2 min): Run gap analysis.
  "What is still not covered?"
```

**Score**:
- [ ] Tests generated in under 5 minutes
- [ ] At least 6 distinct test cases
- [ ] You added at least 1 test Claude missed
- [ ] All tests pass
- [ ] Gap analysis run

Score: ___/5

---

## Mock #3 — Debug an Error (10 min)

**Setup**: Find a real error (recent CI failure, test failure, or runtime error).

**Drill**:
```
Step 1 (1 min): Collect: exact error + stack trace + relevant code.
  Verify: no sensitive data in what you're about to share.

Step 2 (3 min): Use /debug or the manual prompt.
  Read the full response before asking follow-ups.

Step 3 (3 min): Evaluate the diagnosis.
  Is the root cause convincing?
  Do you understand WHY, not just WHAT?
  If not: "Explain WHY [specific part] causes this"

Step 4 (3 min): Apply the fix and run tests.
```

**Score**:
- [ ] No sensitive data shared
- [ ] Understood root cause (not just symptom)
- [ ] Fix applied and tests pass
- [ ] Completed in under 10 minutes

Score: ___/4

---

## Mock #4 — Pre-Commit Review (15 min)

**Setup**: Any change you're about to commit.

**Drill**:
```
Step 1 (5 min): /security on each changed file.
  Note: any CRITICAL or HIGH findings?

Step 2 (3 min): Test gap analysis.
  "What error paths in my changes are untested?"

Step 3 (3 min): Generate commit message.
  "Generate a conventional commit message for:
  Files: [list]
  Change: [describe]"

Step 4 (2 min): Run tests.
  All should pass.

Step 5 (2 min): Review git diff.
  Any unexpected files? Any secrets in the diff?
```

**Score**:
- [ ] Security review completed
- [ ] At least 1 test gap identified
- [ ] Commit message generated and reviewed
- [ ] All tests pass
- [ ] git diff reviewed — no unexpected files or secrets

Score: ___/5

---

## Mock #5 — Agent Mode Controlled Session (20 min)

**Setup**: A scaffolding task — "Add a new API endpoint for [X] following existing patterns."

**Pre-session (before timer)**:
```
git commit -m "checkpoint: before agent mode drill"
```

**Drill**:
```
Step 1 (3 min): Write the agent session prompt.
  Include: Context (CLAUDE.md + key files), Goal, Requirements,
  Forbidden actions, Stopping conditions, Final verification command.

Step 2 (5 min): Review the plan Claude proposes.
  Correct at least 1 incorrect assumption if present.
  Confirm plan is correct before approving.

Step 3 (7 min): Execute and monitor.
  Watch for: unexpected file modifications, scope drift.
  If scope drift: stop and note it.

Step 4 (5 min): Review ALL changes.
  Open Source Control diff. Read every modified file.
  Run tests.
```

**Score**:
- [ ] Pre-session commit done
- [ ] Full prompt used: Context + Goal + Requirements + Forbidden + Stopping + Verification
- [ ] Plan reviewed and corrected before approving
- [ ] No unreviewed changes accepted
- [ ] Tests pass after accepting

Score: ___/5

---

## Mock #6 — Token Efficiency Drill (10 min)

**Drill**:
```
Step 1 (3 min): Write a verbose prompt for this task:
  "Add input validation to the create_user endpoint"
  Write it as you normally would — no constraints.

Step 2 (3 min): Rewrite as a compact version.
  Target: under 60 words.
  Must include: @file or @selection reference, goal, one constraint, output format.

Step 3 (2 min): Send BOTH prompts to Claude (separate sessions).
  Which produced more useful output?

Step 4 (2 min): Save the better version as a slash command if it worked well.
```

**Score**:
- [ ] Compact version is under 60 words
- [ ] Compact version references a specific file or selection
- [ ] Compact version specifies output format
- [ ] Compact version produced equal or better output

Score: ___/4

---

## Mock #7 — Multi-Agent Pipeline (45 min)

**Setup**: A feature you need to build that touches 3+ files.

**Drill**:
```
Phase 1 — Planner session (10 min):
  New Claude Code session.
  Context: @file:CLAUDE.md + relevant existing files.
  Goal: produce an implementation plan document.
  Save output as: docs/plans/[feature]-plan.md

Phase 2 — Builder session (20 min):
  New Claude Code session.
  Context: @file:docs/plans/[feature]-plan.md + only the listed files.
  Follow the plan exactly. Report after each file.

Phase 3 — Tester session (15 min):
  New Claude Code session.
  Context: new files from builder + test conventions.
  Generate and run tests. Fix failures without modifying implementation.
```

**Score**:
- [ ] Plan document created before building starts
- [ ] Builder used only files listed in the plan
- [ ] Tester used fresh context (not the builder's full history)
- [ ] All tests pass at end of tester session
- [ ] You reviewed the final diff before considering it done

Score: ___/5

---

## Weekly Score Tracker

| Week | Planning | Testing | Debugging | Pre-Commit | Agent | Tokens | Pipeline | Total |
|------|----------|---------|-----------|------------|-------|--------|----------|-------|
| 1 | /4 | /5 | /4 | /5 | /5 | /4 | /5 | /32 |
| 2 | /4 | /5 | /4 | /5 | /5 | /4 | /5 | /32 |
| 3 | /4 | /5 | /4 | /5 | /5 | /4 | /5 | /32 |
| 4 | /4 | /5 | /4 | /5 | /5 | /4 | /5 | /32 |

**Target**: 26+/32 by Week 4 = Pro-level workflow maturity
