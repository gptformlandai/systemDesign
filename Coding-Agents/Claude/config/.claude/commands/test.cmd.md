---
description: Comprehensive test generation following the testing skill workflow
---

Generate tests for:

$ARGUMENTS

Follow the testing skill in .claude/skills/testing/SKILL.md.

Requirements:
- Cover: happy path, at least 2 error scenarios, at least 2 edge cases
  (empty, None, boundary values, concurrent access)
- Test names: test_<function>_<scenario>_<expected_outcome>
- One logical assertion per test
- Mock all external dependencies (HTTP, email, database for unit tests)
- Do NOT test implementation details — test observable behavior

After generating tests:
1. Run them
2. Fix any that fail (fix test setup, not the implementation)
3. Report: X passed, Y failed, Z skipped

Output: complete test file with all imports, ready to run
