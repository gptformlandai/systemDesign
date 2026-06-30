# Agent Loops — Gold Sheet

> **Track**: Codex Mastery Track — Group 3: Advanced Engineering
> **File**: 3 of 7 (Track File #16)
> **Audience**: Developers designing multi-step Codex sessions that iterate to completion
> **Read after**: System-Prompt-Engineering-Gold-Sheet.md

---

## Practical Impact Meter

| Topic | Impact | Why Developers Miss This |
|---|---|---|
| The plan-execute-verify loop structure | ★★★★★ | Codex without a loop structure stops when it "feels done" — not when tests pass |
| Stopping conditions — preventing infinite loops | ★★★★★ | Without stopping conditions, Codex iterates in circles on unfixable problems |
| The verification step as the loop control | ★★★★★ | Verification output drives the next iteration — it's not optional |
| Iteration limits — when to hand back to human | ★★★★☆ | 10+ failed iterations on the same test = Codex is stuck; human diagnosis needed |
| Loop for different task types | ★★★☆☆ | Debugging loop ≠ implementation loop ≠ refactor loop; each has different structure |

---

## ⭐ Beginner Tier — Start Here

### B1: Write your first loop prompt

```bash
# Without a loop structure (bad):
codex "implement pagination for GET /orders"
# Codex stops when it thinks it's done. No verification. No iteration.

# With a loop structure (good):
codex --approval-policy auto-edit \
  "Implement pagination for GET /orders in src/api/orders.py.
   After implementing: run pytest tests/test_order_api.py -x
   If tests fail: read the output, fix, run again.
   Stop only when: pytest tests/test_order_api.py -x passes.
   Do not modify test files."
# Codex iterates until tests pass — not until it 'feels done'
```

The only addition: a verification command + iteration instruction. That's a loop.

### B2: Write a stopping condition

```bash
# Add this to every loop prompt:
# "Stopping conditions:
#  - After 5 failed attempts on the same test: stop and report BLOCKED: [reason]
#  - If the fix requires modifying a test file: stop and report"

# Without stopping conditions: Codex loops on a broken test indefinitely
# With stopping conditions: you get a clear signal when to diagnose manually
```

---

## 1. The Core Loop Structure

```
Every effective Codex agent loop has four phases:

1. PLAN
   Codex reads context, proposes approach, identifies files

2. EXECUTE
   Codex makes the change (file edit, command, or both)

3. VERIFY
   Codex runs the verification command and reads the output
   If verification passes: done
   If verification fails: go to DIAGNOSE

4. DIAGNOSE (on failure)
   Codex reads the failure output
   Determines: is this a new attempt at the same approach, or a different approach?
   If same approach repeatedly: stopping condition triggered
   If new approach available: go back to EXECUTE
```

---

## 2. Implementation Loop

```bash
# Prompt structure for a self-iterating implementation loop
codex --approval-policy auto-edit \
  "Implement [task] in [file].
   
   Loop until verification passes:
   STEP 1: Implement the required change
   STEP 2: Run: [verification command]
   STEP 3: If failures:
     - Read the failure output carefully
     - Identify root cause (not symptom)
     - Fix the root cause (not the test)
     - Go back to STEP 2
   STEP 4: When verification passes:
     - Run full test suite: pytest -x
     - If regressions found: fix regressions (do not modify tests)
     - When full suite passes: report done
   
   Stopping conditions:
   - Stop after 5 failed attempts on the same test with the same approach
   - Stop if the fix would require modifying a test file
   - Stop if the fix would require a new library not in AGENTS.md
   - Stop and report: 'BLOCKED: [reason]' so human can diagnose
   
   Do not modify test files under any circumstances."
```

---

## 3. Debugging Loop

```bash
# Loop specifically for diagnosing and fixing an error
codex --approval-policy auto-edit \
  "Debugging loop for: [error description]
   
   Error reproduction command: [exact command that reproduces the error]
   
   LOOP:
   STEP 1: Run the reproduction command
   STEP 2: Read the full error output — not just the last line
   STEP 3: Identify root cause (why does this happen, not just what happens)
   STEP 4: Apply minimum fix to root cause
   STEP 5: Run reproduction command again
   STEP 6: If error persists with same root cause: try different fix
   STEP 7: If error persists with different root cause: you uncovered a deeper bug — diagnose fresh
   STEP 8: When error is gone: run full test suite — check for regressions
   
   STOPPING CONDITIONS:
   - After 3 failed attempts with different approaches: stop and report what was tried
   - If fix requires schema change or migration: stop and report
   - If fix requires changing the test: stop and report (the test may be correct)
   
   Report at end: root cause, fix applied, tests status."
```

---

## 4. Refactoring Loop (Behavior-Preserving)

```bash
codex --approval-policy auto-edit \
  "Refactoring loop: [describe structural change]
   
   CONSTRAINT: behavior must not change. Same inputs → same outputs.
   
   LOOP:
   STEP 1: Run baseline: [test command] → record N tests passing
   STEP 2: Make ONE structural change (not multiple at once)
   STEP 3: Run: [test command]
   STEP 4: If failures: UNDO that specific change (not all changes)
             Try a different approach for that change
             Go to STEP 3
   STEP 5: If passing: record as safe, move to next structural change
   STEP 6: Repeat from STEP 2 for each planned structural change
   STEP 7: Final: run full test suite
   
   STOPPING CONDITIONS:
   - If a specific structural change fails 3 different approaches: skip it and report
   - If any public function signature would need to change: stop and report
   - Do not modify test files
   
   Done = same test count, all passing, structural changes complete."
```

---

## 5. Stopping Conditions — Critical Safety Mechanism

```
Without stopping conditions, Codex will:
  - Try the same wrong fix repeatedly
  - Make increasingly broad changes looking for something that works
  - Potentially modify test files to break out of the loop
  - Keep going even when the problem requires human diagnosis

Required stopping conditions for every loop:

1. Iteration limit
   "Stop after 5 failed attempts on the same failure"
   Prevents infinite loops on unfixable bugs

2. Scope violation trigger
   "If the fix requires modifying test files: stop"
   "If the fix requires a new library: stop and list which one"
   Prevents scope creep

3. Schema/infrastructure trigger
   "If the fix requires a database migration: stop"
   "If the fix requires infrastructure changes: stop"
   Protects against destructive operations

4. Human decision trigger
   "If you're uncertain whether the expected behavior is correct: stop and ask"
   Prevents Codex from making product decisions

5. Diminishing returns trigger
   "If the same error persists after 3 different approaches: stop"
   Signals that human diagnosis is needed
```

---

## 6. Reading Loop Output — What to Watch

```
Signs the loop is working correctly:
  ✅ Each iteration changes the approach (not repeating the same fix)
  ✅ The number of failing tests decreases over iterations
  ✅ Codex reads the full error output, not just the last line
  ✅ Root cause diagnosis changes when a fix reveals a deeper bug

Signs the loop is stuck:
  ⚠ Same test fails for 5+ iterations
  ⚠ Codex makes increasingly broad changes that drift from the original task
  ⚠ Codex modifies test files "to make them work"
  ⚠ Number of failing tests increases over iterations

Action when stuck:
  Stop the session (Ctrl+C or wait for stopping condition)
  Read the last error output
  Provide fresh context to a new session:
    "Previous attempts failed with [approach]. 
     Here is the current error: [paste]
     Try a completely different approach: [suggest direction]"
```

---

## 7. Iteration Limit Guidance

```
Task type              Reasonable limit    When to escalate
Simple bug fix:        3-5 iterations      → Human diagnosis
Test generation:       1-2 iterations      → Manual gap analysis
Implementation:        3-7 iterations      → Architecture review
Refactoring:           Per-change: 3       → Skip that change
CI failure:            3-5 iterations      → Human environment check
```

---

## Interview Traps

```
TRAP: "Codex will stop when tests pass — stopping conditions are unnecessary"
TRUTH: Codex stops when tests pass OR when it runs out of approaches. Without explicit
       stopping conditions, it may try 20 variations of the same wrong fix, modify test
       files to force passing, or drift to files outside the stated scope.

TRAP: "More iterations always produce a better result eventually"
TRUTH: After 5 failed iterations on the same test, the problem requires human diagnosis.
       Codex is pattern-matching on fixes it's seen before. If those patterns are wrong
       for this specific problem, more iterations produce more wrong fixes, not progress.

TRAP: "Loop prompts are only for full-auto mode"
TRUTH: Loops are most safely run in auto-edit mode where you can interrupt if direction
       goes wrong. Loop structure is about verification discipline, not approval policy.
       Use loops in auto-edit mode where you can watch and interrupt.
```

---

## Revision Checklist

- [ ] Can write a self-iterating implementation loop prompt
- [ ] All loops have stopping conditions (at least: iteration limit + scope violation)
- [ ] Know the signs that a loop is stuck vs working
- [ ] Can write loop prompts for implementation, debugging, and refactoring tasks
- [ ] Stopping conditions include: test file modification trigger, schema migration trigger
