---
name: Commit Message
description: Generate a conventional commit message from staged changes
---

Generate a conventional commit message for my staged changes.

Changes summary:
${selection}

Format: `type(scope): description`

Types:
- `feat`: new feature for the user
- `fix`: bug fix
- `refactor`: code change that is neither a feature nor a bug fix
- `test`: adding or correcting tests
- `docs`: documentation only
- `chore`: maintenance (deps, build, config)
- `perf`: performance improvement
- `ci`: CI/CD changes
- `style`: formatting only (no logic change)
- `revert`: reverting a previous commit

Rules:
- Subject line: under 72 characters
- Imperative mood: "add validation" not "added validation" or "adds validation"
- Scope: the module/component/feature area (optional but preferred)
- Body (only if needed): explain WHY, not WHAT — the diff already shows what
- Footer: `BREAKING CHANGE: description` if there's a breaking change

Output: just the commit message — no explanation. If the change covers multiple concerns, suggest splitting into separate commits with a note.

Examples of good commit messages:
```
feat(auth): add JWT refresh token rotation
fix(orders): prevent double-processing on concurrent submissions
test(payment): add edge cases for partial refund validation
refactor(user): extract email validation into EmailValidator class
chore(deps): upgrade stripe-python to 7.0.0
docs(readme): add Docker setup instructions
```
