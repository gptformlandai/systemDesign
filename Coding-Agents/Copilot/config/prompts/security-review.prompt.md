---
name: Security Review
description: OWASP-aligned security review with severity-ranked findings and concrete fixes
---

Perform a security review of:

${selection}

Check for each OWASP category:

1. **Injection** (A03): SQL, command, template, path traversal via user input?
2. **Broken Access Control** (A01): Missing auth checks, IDOR, privilege escalation?
3. **Cryptographic Failures** (A02): Weak algorithms, hardcoded keys, insecure randomness?
4. **Insecure Design** (A04): Business logic flaws, missing rate limiting on sensitive operations?
5. **Security Misconfiguration** (A05): Debug mode, verbose errors, missing headers?
6. **Vulnerable Components** (A06): Outdated or known-vulnerable dependencies visible in code?
7. **Auth Failures** (A07): Broken login, session management, token handling?
8. **Data Integrity** (A08): Deserialization issues, unsigned data trusted as input?
9. **Logging Failures** (A09): PII in logs, insufficient audit trail, secrets logged?
10. **SSRF** (A10): User-controlled URLs used in server-side requests?

For each finding:
```
SEVERITY: CRITICAL | HIGH | MEDIUM | LOW
CATEGORY: [OWASP category and number]
LOCATION: [specific line or function]
ISSUE: [what the vulnerability is]
ATTACK VECTOR: [how an attacker exploits this]
FIX: [specific code change — not generic advice]
```

Rules:
- If no issues in a category: state "No issues found" (do not skip)
- FIX must be a concrete code change, not "add validation"
- Do not flag style issues or non-security performance concerns
- If you find a CRITICAL: flag it at the top before the full list
