# Codex Daily Workflow Checklist

> Use this every day. It takes 2 minutes to check and prevents the most common mistakes.

---

## Morning (Before First Codex Session)

```
[ ] AGENTS.md exists and is up to date for today's work area
[ ] API key is set: echo $OPENAI_API_KEY | head -c 10
[ ] Git status is clean or has a checkpoint commit
[ ] Today's task is scoped: I know which files Codex will need
[ ] Verification command identified: [pytest / jest / npm test / make lint]
```

---

## Before Every Full-Auto Session

```
[ ] Checkpoint commit done: git add -A ; git commit -m "checkpoint"
[ ] Task is bounded: I can describe it in 2 sentences max
[ ] Files in scope are listed (not "the whole codebase")
[ ] Forbidden actions are in AGENTS.md or the prompt
[ ] Verification command is in the prompt
[ ] I'm using: codex --approval-policy full-auto "[task]"
```

---

## During Coding Sessions

```
[ ] Using auto-edit for targeted implementation (not full-auto)
[ ] Scoping Codex to the relevant files, not entire repo
[ ] Reading Codex's plan before approving execution
[ ] Running verification command after each task, not just at end
[ ] Using /compact if session has been running for 30+ min
```

---

## Pre-Commit (Before Every git commit)

```
[ ] Security: run codex "review for OWASP issues" on auth/SQL code
[ ] Test gap: run codex "what error paths are untested in these changes?"
[ ] Diff review: git diff HEAD — I can explain every changed line
[ ] No secrets in diff: grep -r "password\|secret\|api_key" -- [changed files]
[ ] Tests pass: [pytest -x / npm test / make test]
[ ] Lint passes: [ruff / eslint / golint]
[ ] No unexpected files: git status — only expected files modified
```

---

## End of Day

```
[ ] Best prompts from today saved as reusable scripts in scripts/prompts/
[ ] Any AGENTS.md improvement identified and applied
[ ] Failures noted: what prompt produced bad output and why
[ ] Tomorrow's task scoped: next task description drafted
```

---

## Weekly (Friday or Monday)

```
[ ] AGENTS.md reviewed: is it still accurate? Add anything that was repeated this week
[ ] Prompt script library reviewed: any new additions?
[ ] Model choices reviewed: am I using gpt-4.1 only when needed?
[ ] Checkpoint commit discipline: did any full-auto sessions run without a checkpoint?
[ ] Security: any AI-generated code shipped without a dedicated security review?
```

---

## Red Flags — Stop and Investigate

```
⚠ Codex modified files outside the stated scope
⚠ Codex added a new dependency without listing it first
⚠ Tests pass but you don't understand why the fix works
⚠ Codex produced a long response but skipped the verification step
⚠ You accepted changes without reading them because "Codex is usually right"
⚠ Your prompt had the actual database URL, API key, or real user data
⚠ You ran full-auto without a checkpoint commit
```

---

## Metrics — Track These Monthly

| Metric | How to Measure | Target |
|--------|---------------|--------|
| Checkpoint compliance | % of full-auto sessions that had prior checkpoint | 100% |
| Pre-commit review | % of PRs where security review was run | 100% |
| Prompt reuse rate | % of prompts that were copied from script library | > 50% |
| Bug escape rate in AI-assisted code | Post-deploy bugs from AI-generated changes | ↓ vs baseline |
| AGENTS.md update frequency | Updates per week | ≥ 1 |
