# Codex Scoring Rubrics

> **Track**: Codex Mastery Track — Group 6: Practice & Upgrade
> **File**: 4 of 6 (Track File #33)
> **Audience**: All levels — use to grade your own work and identify gaps
> **Format**: 7 rubrics, each /5, total /35

---

## Rubric 1 — Prompt Quality (/5)

Rate any task prompt you write:

| Score | Criteria |
|-------|----------|
| 5 | File scope + specific action + constraints + verification command + stopping conditions if looping |
| 4 | File scope + action + constraints + verification, but missing stopping conditions |
| 3 | Action and verification present, but file scope or constraints missing |
| 2 | Only action stated — no file scope, constraints, or verification |
| 1 | Vague goal ("improve the code") — requires Codex to make all decisions |
| 0 | Contains secrets, real PII, or security bypass request |

**What 5/5 looks like**:
```
"Add input validation to create_subscription() in src/subscriptions/service.py.
 Validation: amount > 0 (ValueError if not), plan_id must be in [BASIC, PRO, ENTERPRISE].
 Do not modify: src/db/, tests/.
 Verification: pytest tests/test_subscription_service.py -x
 Stop if: fix requires test file modification or new library."
```

---

## Rubric 2 — AGENTS.md Quality (/5)

Rate any AGENTS.md file:

| Score | Criteria |
|-------|----------|
| 5 | All 6 sections, forbidden list has 5+ specific rules, verification command present, no vague entries |
| 4 | All 6 sections, forbidden list has 3-4 rules, verification present |
| 3 | 4-5 sections present, some vague entries ("write clean code"), no verification |
| 2 | Only project context and coding standards — no forbidden list, no architecture |
| 1 | Exists but is empty or has only a title |
| 0 | No AGENTS.md at all (for any project using full-auto) |

**What a vague entry looks like** (scores lower):
- "Follow best practices"
- "Write clean code"
- "Be careful with the database"

**What a specific entry looks like** (scores higher):
- "Use ValueError for business validation errors, HTTPException for API errors"
- "Forbidden: run database migrations"
- "Verification: pytest -x && ruff check src/"

---

## Rubric 3 — Test Quality (Generated Tests) (/5)

Rate any test file Codex generates:

| Score | Criteria |
|-------|----------|
| 5 | Happy path + all error paths + edge cases; only external deps mocked; no own-code mocking; asserts behavior not return values |
| 4 | Happy path + most error paths; minor gaps (one edge case missing) |
| 3 | Happy path covered, some error paths; over-mocked (own service/repo mocked) |
| 2 | Only happy path; mocks own code; tests pass with broken implementation |
| 1 | Tests exist but are tautological (test the mock, not the code) |
| 0 | Tests were modified to make them pass instead of fixing implementation |

---

## Rubric 4 — Workflow Safety (/5)

Rate any full-auto session you ran:

| Score | Criteria |
|-------|----------|
| 5 | Git checkpoint before; on feature branch; bounded task; explicit scope; forbidden list; verification; post-session diff review |
| 4 | Checkpoint + branch + bounded task + verification; post-review done; missing forbidden list |
| 3 | Checkpoint + branch; task not well-bounded; no forbidden list; verification present |
| 2 | Branch present but no checkpoint; or checkpoint but on main |
| 1 | No checkpoint, no branch — ran full-auto directly on main |
| 0 | Full-auto without checkpoint on a shared branch |

---

## Rubric 5 — Security Review Thoroughness (/5)

Rate any security review you ran using Codex:

| Score | Criteria |
|-------|----------|
| 5 | Checks all: SQL injection, auth bypass, PII exposure, input validation, error message leakage; each finding has severity + OWASP + fix |
| 4 | 4 of 5 categories checked; findings have severity + fix |
| 3 | 3 categories checked; findings have fixes but no severity |
| 2 | 1-2 categories; findings identified but no fix provided |
| 1 | Generic "looks safe" or "no obvious issues" without specific checks |
| 0 | Security review skipped entirely |

---

## Rubric 6 — Context Engineering (/5)

Rate how you set up a Codex session:

| Score | Criteria |
|-------|----------|
| 5 | Session primed with exact files for the task; irrelevant dirs excluded; model matches task type; /compact used between tasks |
| 4 | Correct files loaded; model correct; no /compact between tasks (minor inefficiency) |
| 3 | Some irrelevant files loaded; model not optimized; no /compact |
| 2 | No priming — Codex auto-discovers (potentially wastes context on irrelevant files) |
| 1 | All of src/ loaded for a task touching 2 files |
| 0 | No consideration of context at all |

---

## Rubric 7 — Verification Discipline (/5)

Rate how you verify Codex output:

| Score | Criteria |
|-------|----------|
| 5 | Test command in every prompt; tests run and pass; git diff reviewed; full suite checked for regressions; security check on auth/payment code |
| 4 | Tests run and pass; full suite checked; diff reviewed; no explicit security check |
| 3 | Tests run and pass; diff not reviewed; full suite not checked |
| 2 | Tests run but not reviewed (just checking "passed"); no diff review |
| 1 | Verified by looking at the code — no test run |
| 0 | "Codex said it worked" — no verification at all |

---

## Total Score: /35

| Score | Level |
|-------|-------|
| 31-35 | Pro. Codex workflows are automatic. |
| 25-30 | Advanced. Occasional gaps in safety or verification. |
| 18-24 | Intermediate. Good instincts, needs reinforcement. |
| 10-17 | Foundations. Review core concepts and re-run drills. |
| Below 10 | Start with 01-Foundations and work through in order. |

---

## How to Use These Rubrics

```
Weekly habit:
  1. Pick 2-3 rubrics to focus on this week
  2. Apply them to real work (not exercises)
  3. Score yourself honestly
  4. Focus improvement on any rubric below 3/5

Improvement plan for each score below 3:
  Prompt Quality < 3       → Re-read: Prompt-Engineering-for-Codex-Gold-Sheet.md
  AGENTS.md < 3            → Re-read: AGENTS-MD-Design-Gold-Sheet.md
  Test Quality < 3         → Re-read: Codex-For-Testing-Gold-Sheet.md
  Workflow Safety < 3      → Re-read: Safe-Usage-Principles-Gold-Sheet.md
  Security Review < 3      → Re-read: Code-Review-Scenarios-Gold-Sheet.md
  Context Engineering < 3  → Re-read: Context-Engineering-Gold-Sheet.md
  Verification < 3         → Re-read: Verification-Driven-Workflows-Gold-Sheet.md
```
