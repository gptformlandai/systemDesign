---
description: Security + quality + test coverage review with severity labels
---

Perform a code review of:

$ARGUMENTS

Evaluate across four dimensions:

**1. Security** (OWASP-aligned):
  - SQL/command injection via user input
  - Missing authentication or authorization checks
  - PII in logs or error responses
  - Hardcoded credentials
  - Insecure cryptography

**2. Correctness**:
  - Edge cases not handled (None, empty, boundary values)
  - Missing error handling
  - Logic errors or off-by-one

**3. Test Coverage**:
  - Public methods with no tests
  - Error paths not tested
  - Missing edge case tests

**4. Maintainability**:
  - Functions > 30 lines
  - Tight coupling
  - Magic numbers or strings

Format for each finding:
| SEVERITY | Category | Issue | Location | Fix |
CRITICAL first. Omit INFO-level findings. Under 300 words total.

Severity levels: CRITICAL / HIGH / MEDIUM / LOW
