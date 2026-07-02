# Release Manager Skill

## Purpose

Use this skill when preparing, reviewing, or repairing a software release.

The goal is not to "ship fast." The goal is to produce a release that is traceable, test-backed, reversible, and easy for humans to review.

---

## When To Use

Use for:

- release notes
- changelog preparation
- version bump review
- release branch readiness
- hotfix preparation
- rollback notes
- pre-release risk review

Do not use for:

- automatic production deployment
- publishing packages without human approval
- credential rotation
- changing release infrastructure unless explicitly requested

---

## Required Inputs

Ask for missing inputs before making changes:

- target version
- release branch
- commit range or previous tag
- test command
- deployment target
- rollback method
- known risks or incidents included in the release

---

## Procedure

1. Inspect release context
   - current branch
   - latest tag
   - pending changes
   - CI status if available

2. Summarize candidate changes
   - features
   - fixes
   - dependency changes
   - migrations
   - breaking changes

3. Check release safety
   - tests documented
   - rollback path documented
   - migration risk called out
   - config changes reviewed
   - secrets absent from notes and logs

4. Prepare artifacts
   - changelog entry
   - release notes
   - PR description
   - rollback section

5. Stop before side effects
   - do not tag, publish, deploy, or merge unless explicitly approved by a human.

---

## Output Format

```md
## Release Summary

Version:
Branch:
Commit range:

## Changes
- Features:
- Fixes:
- Dependencies:
- Migrations:

## Validation
- Tests run:
- CI status:
- Manual checks:

## Risks
- 

## Rollback
- 

## Human Approvals Needed
- 
```

---

## Safety Rules

- Never include secrets, tokens, customer data, or private incident details in release notes.
- Never claim tests passed unless evidence is available.
- Never publish, tag, merge, or deploy without explicit approval.
- If migrations are included, require a rollback or mitigation note.
- If auth, billing, or data deletion changed, mark the release high-risk.

