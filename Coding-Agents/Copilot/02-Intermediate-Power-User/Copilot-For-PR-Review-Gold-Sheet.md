# Copilot For PR Review — Gold Sheet

> **Track**: Copilot Mastery Track — Group 2: Intermediate Power User
> **File**: 7 of 7 (Track File #13)
> **Audience**: Developers using Copilot to improve PR quality
> **Read after**: Copilot-For-CI-GitHub-Actions-Gold-Sheet.md

---

## 1. Practical Impact Meter

| Topic | Impact | Why Developers Skip This |
|---|---|---|
| PR self-review before pushing — the pre-flight | ★★★★★ | Devs push code that's missing obvious things; Copilot catches them fast |
| Security-focused review prompt | ★★★★★ | Security issues are the hardest for developers to self-catch |
| Test coverage gap review | ★★★★★ | Tests are often the first thing a developer skips under time pressure |
| PR description generation | ★★★★☆ | Descriptive PRs make reviews 3x faster — Copilot writes them in 30 seconds |
| Copilot Code Review surface vs Chat prompts | ★★★★☆ | Two different tools for review; knowing which to use when |
| Maintainability and technical debt review | ★★★★☆ | Tech debt review is subjective; Copilot provides a structured second opinion |
| Review comment generation — how to be constructive | ★★★★☆ | Review comments need to be actionable, not just critical |

---

## 2. Pre-PR Self-Review Workflow

### The Pre-PR Checklist Prompt

```
Run this in Chat with your diff as context before pushing:

"Review the changes in my current diff for this PR:
#file:src/  [or list specific changed files]

Perform a complete pre-PR self-review:

1. Correctness: Are there logical errors, edge cases, or missing null checks?
2. Security: Any SQL injection risks, missing input validation, hardcoded credentials,
   logging of sensitive data, or insecure patterns?
3. Tests: Do the changes have corresponding tests? Are any error paths untested?
4. Documentation: Are complex parts explained? Are new public APIs documented?
5. Error handling: Are all errors caught and handled appropriately?
6. Backwards compatibility: Does this break any existing API contracts?
7. Performance: Any N+1 queries, unnecessary loops, or missing caching?
8. Code style: Does this match our conventions in .github/copilot-instructions.md?

Format: numbered list of issues. For each issue: describe the problem, 
show the specific line/section, and suggest the fix.
If no issues in a category: state 'No issues found'."
```

---

## 3. Security Review Prompt

```
"Perform a security-focused code review of #selection (or #file:path).

Check for:
1. Injection vulnerabilities: SQL, command injection, SSTI, LDAP injection
2. Authentication: Is auth enforced? Can it be bypassed?
3. Authorization: Does every endpoint check that the user has permission?
4. Input validation: Is all user input validated before use?
5. Output encoding: Is output encoded to prevent XSS?
6. Sensitive data: Is PII/payment data logged? Is it stored unencrypted?
7. Cryptography: Are secure algorithms used? (SHA256+, bcrypt/argon2, not MD5/SHA1)
8. Dependency risks: Are there known vulnerable dependencies?
9. Error handling: Do error responses reveal internal details?
10. Rate limiting: Is this endpoint protected against brute force?

For each finding:
- Severity: CRITICAL / HIGH / MEDIUM / LOW
- Description: what the vulnerability is
- Attack vector: how it could be exploited
- Fix: specific code change to remediate
- Reference: OWASP category if applicable"
```

---

## 4. PR Description Generation

### The PR Description Prompt

```
"Generate a GitHub PR description for my changes.

Changed files:
#file:src/services/user_service.py
#file:src/api/users.py
#file:tests/unit/test_user_service.py

What I changed (in plain English):
[Describe your changes in 2-3 sentences — Copilot will format this properly]

Format the PR description using this structure:

## Summary
[2-3 sentence summary of what changed and why]

## Changes Made
[Bullet list of specific changes]

## How to Test
[Step-by-step testing instructions for the reviewer]

## Screenshots / Examples (if applicable)
[placeholder or actual if relevant]

## Checklist
- [ ] Tests added/updated
- [ ] Documentation updated
- [ ] Security review done
- [ ] No breaking changes (or breaking changes documented)

Keep it concise and factual. Focus on WHAT changed and WHY, not HOW."
```

---

## 5. Copilot Code Review vs Chat Review

```
Copilot Code Review (GitHub.com surface):
  - Available in the PR view on GitHub.com
  - Reviews the actual PR diff automatically
  - Posts inline comments on specific lines
  - Best for: automated first-pass review on every PR
  - Limitation: less controllable than a prompt — Copilot decides what to flag

Chat Review (VS Code):
  - You control what context to provide
  - You control what aspects to focus on (security, tests, performance)
  - You can iterate: "Focus more on the authentication flow"
  - Best for: targeted, thorough review of specific concerns
  - Limitation: requires manual context setup

Use both:
  - Copilot Code Review on GitHub.com for automated first pass
  - Chat Review in VS Code for security, performance, and targeted deep dives
```

---

## 6. Review Comment Generation

```
After finding an issue, generate a constructive review comment:

"Generate a constructive code review comment for this issue I found:

Issue: [describe the problem]
Code location: #selection

Requirements for the comment:
- Start with what the code does correctly (if applicable)
- Explain the issue clearly without being critical of the developer
- Show the specific fix as a code suggestion
- Explain WHY the fix is better (not just that it's different)
- Keep it under 150 words"
```

---

## 7. Technical Debt and Maintainability Review

```
"Review #file:path for technical debt and maintainability issues.

Evaluate:
1. Complexity: Functions/classes that are too long or do too many things
2. Duplication: Code that is repeated and should be extracted
3. Naming: Variables, functions, or classes with unclear names
4. Magic values: Hardcoded numbers/strings that should be constants
5. Coupling: Components that are tightly coupled and hard to change independently
6. Test coverage: Behaviors that are not tested
7. Error handling: Silent failures, swallowed exceptions
8. Future fragility: Code that will break when requirements change

Priority: HIGH (fix now), MEDIUM (fix in next sprint), LOW (track in backlog)
Format: Prioritized table with file, line range, issue description, and suggested fix"
```

---

## 8. Intermediate Path Completion Checklist

After completing all 7 Intermediate sheets, verify:

```
Sheets completed:
[ ] Custom Instructions Deep Dive
[ ] Prompt Files & Slash Commands
[ ] Copilot Edits Mode
[ ] Agent Mode Safe Usage
[ ] Copilot For Testing
[ ] Copilot For CI & GitHub Actions
[ ] Copilot For PR Review (this sheet)

Skills demonstrated:
[ ] Have a working copilot-instructions.md in at least one project
[ ] Have at least 5 prompt files in .github/prompts/
[ ] Have done a multi-file Edits session and reviewed the diff carefully
[ ] Have run Agent Mode with a plan-first prompt and reviewed all changes
[ ] Have generated a test suite with gap analysis
[ ] Have generated a GitHub Actions workflow that actually runs
[ ] Have run a pre-PR review with Copilot before merging

Next step: 03-Advanced-Engineering/Custom-Agents-Deep-Dive-Gold-Sheet.md
```

---

## 9. Revision Checklist

- [ ] Can run a complete pre-PR self-review with a structured Copilot prompt
- [ ] Can run a security-focused review covering all 10 OWASP-aligned categories
- [ ] Can generate a PR description using the structured template
- [ ] Knows the difference between Copilot Code Review and Chat review
- [ ] Can generate constructive, actionable review comments
- [ ] Can run a technical debt review with priority levels
