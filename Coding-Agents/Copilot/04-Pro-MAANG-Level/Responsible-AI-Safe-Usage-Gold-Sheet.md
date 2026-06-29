# Responsible AI & Safe Usage — Gold Sheet

> **Track**: Copilot Mastery Track — Group 4: Pro / Production Level
> **File**: 7 of 7 (Track File #27)
> **Audience**: All Copilot users — the non-negotiable professional standard
> **Read alongside**: All other sheets in this track

---

## 1. The 12 Non-Negotiable Rules

```
Rule 1: Never paste real secrets into Copilot.
  API keys, tokens, passwords, SSH keys, certificates, OAuth secrets, PATs.
  Violation: Your token may be logged, transmitted, or cached by the AI service.
  Fix: Use placeholders. "Replace API_KEY with your actual key" in instructions.

Rule 2: Never paste real customer PII into Copilot.
  Names, emails, SSNs, credit cards, health data, addresses, phone numbers.
  Violation: Sending real PII to an external AI service is a potential privacy/compliance breach.
  Fix: Anonymize. Use synthetic data that mirrors the structure.

Rule 3: Always review generated code before accepting.
  Copilot output is a first draft. It compiles ≠ it is correct.
  Violation: Silent bugs, security vulnerabilities, hallucinated methods.
  Fix: Read every changed line. Run tests. Run lint.

Rule 4: Commit before Agent Mode runs.
  Agent Mode can modify many files in one session.
  Violation: No recovery point means bad output = manual undo of many files.
  Fix: git add . && git commit -m "checkpoint: before Agent Mode - [task]"

Rule 5: Run tests after every Copilot-assisted change.
  Violation: Copilot can break existing behavior without changing the function signature.
  Fix: Baseline → change → test. If tests break: the change is wrong.

Rule 6: Validate every suggested dependency before installing.
  Violation: Typosquatted or malicious packages. Outdated dependencies with CVEs.
  Fix: Verify existence on PyPI/npm. Run pip audit / npm audit. Pin versions.

Rule 7: Read every suggested command before running.
  Violation: rm -rf with wrong path, git reset --hard losing work, curl | bash running untrusted code.
  Fix: "Explain this command: [command]" before running. Especially for rm, git, and curl.

Rule 8: Never accept hardcoded credentials in generated code.
  Violation: Credentials committed to repo = security incident.
  Fix: Reject immediately. Ask for environment variable pattern instead.

Rule 9: Apply small, reviewable changes — not massive refactors at once.
  Violation: 50-file change = impossible to review = hidden bugs shipped.
  Fix: One task → review → commit → next task.

Rule 10: Treat Copilot as a first-draft generator, not an authority.
  Violation: Shipping code you can't explain = technical debt and security risk.
  Fix: Only commit code you understand and can defend.

Rule 11: Use source control so bad AI changes can be reverted.
  Violation: No git history = no undo for bad Agent Mode sessions.
  Fix: Commit frequently. Tag before major AI-assisted changes.

Rule 12: Do not bypass security controls.
  Never use Copilot to: bypass authentication, generate SQL to access unauthorized data,
  create tools to attack systems, generate malware patterns, or circumvent access controls.
  This is a professional and legal responsibility.
```

---

## 2. Privacy-First Development with Copilot

```
Before sharing any context with Copilot, ask:
  "Would I be comfortable if this text appeared in a privacy audit?"

If NO: Anonymize before sharing.

Data classification:
  GREEN (safe to share): Synthetic data, public code, open source patterns,
         anonymized examples, your own personal test data
  
  YELLOW (share with caution): Internal tool names, architecture patterns,
         non-sensitive configuration keys, anonymized stack traces
  
  RED (never share): Customer PII, production credentials, internal security data,
       regulated data (HIPAA, GDPR scope), proprietary business algorithms,
       competitive intelligence, financial account data

When debugging with production data:
  1. Reproduce the issue with synthetic data first
  2. Use only the error message and stack trace (rarely contains PII)
  3. If the stack trace DOES contain PII (e.g., logged a user email by mistake):
     Redact the PII before pasting — replace with "user@example.com"
```

---

## 3. Copilot for Security-Sensitive Code

```
Security-sensitive code areas (apply extra scrutiny):
  - Authentication and authorization logic
  - Input validation and sanitization
  - Cryptography and hashing
  - SQL and database access
  - File I/O and path handling
  - Environment variable and secrets handling
  - HTTP client calls and response handling
  - Logging (ensure no PII leaks)
  - Error messages (ensure no internal details leak)

For security-sensitive code: ALWAYS run the security review prompt.
Never accept generated auth, crypto, or SQL code without review.

After accepting security-sensitive generated code:
  - Have a human security-aware developer review it as well
  - Run SAST tools (Bandit for Python, SonarQube, Semgrep)
  - Test negative cases: can an attacker bypass this?
```

---

## 4. AI Output Evaluation Standard

```
Before committing any Copilot-generated code, verify:

CORRECTNESS:
[ ] I can explain what every line does
[ ] The logic handles all documented requirements
[ ] Edge cases (empty, None, boundary values) are handled
[ ] Error paths are implemented, not just happy path

SECURITY:
[ ] No hardcoded credentials
[ ] No SQL string concatenation (use parameterized queries)
[ ] No shell=True with user input
[ ] No logging of sensitive data
[ ] Error responses don't expose internals

TESTING:
[ ] Tests exist for the new code
[ ] Tests cover at least one error case
[ ] Existing tests still pass

QUALITY:
[ ] New dependencies are verified and safe
[ ] No deprecated API usage
[ ] Code matches project conventions
[ ] No unnecessary abstractions or over-engineering
```

---

## 5. Pro-Level Track Completion Checklist

```
All Sheets Read:
01-Foundations:
  [ ] Copilot Mental Model
  [ ] GitHub Copilot Setup
  [ ] Copilot Inline Suggestions
  [ ] Copilot Chat Fundamentals
  [ ] Safe Prompting Principles
  [ ] Beginners Quick Wins (all 10 exercises done)

02-Intermediate Power User:
  [ ] Custom Instructions Deep Dive
  [ ] Prompt Files & Slash Commands
  [ ] Copilot Edits Mode
  [ ] Agent Mode Safe Usage
  [ ] Copilot For Testing
  [ ] Copilot For CI & GitHub Actions
  [ ] Copilot For PR Review

03-Advanced Engineering:
  [ ] Custom Agents Deep Dive
  [ ] AGENTS.md Strategy
  [ ] Context Engineering
  [ ] Token Optimization
  [ ] MCP Integration
  [ ] Copilot For Architecture
  [ ] Prompt Library Management

04-Pro MAANG Level:
  [ ] Personal Copilot Operating System
  [ ] SDLC Automation
  [ ] Copilot Debugging Handbook
  [ ] Advanced Context Engineering
  [ ] Agent Governance & Output Evaluation
  [ ] Team-Ready Instructions
  [ ] Responsible AI & Safe Usage (this sheet)

05-Scenario Practice:
  [ ] Daily Workflow Scenarios
  [ ] Feature Building Scenarios
  [ ] Debugging Scenarios
  [ ] Code Review Scenarios

06-Practice Upgrade:
  [ ] Active Recall Question Bank
  [ ] Prompt Library Templates
  [ ] Mock Workflow Scripts
  [ ] Scoring Rubrics
  [ ] 4-Week Mastery Roadmap

Pro-Level Skills:
[ ] Personal Copilot OS repository active and maintained
[ ] 10+ validated prompt files in library
[ ] 5+ custom agents configured
[ ] Root + folder AGENTS.md strategy in place
[ ] MCP configured safely (mcp.example.json committed, mcp.json gitignored)
[ ] Never violated the 12 non-negotiable rules
[ ] Daily ritual established (planning, pre-PR, end-of-day notes)
[ ] Can diagnose any failure mode from the debugging handbook
[ ] Can evaluate Copilot output against the full quality standard
```

---

## 6. Revision Checklist

- [ ] Can recite all 12 non-negotiable rules from memory
- [ ] Knows the GREEN/YELLOW/RED data classification scheme
- [ ] Applies the AI output evaluation standard before every commit
- [ ] Knows which code areas require extra security scrutiny
- [ ] Has completed the full Pro-Level Track Completion Checklist
