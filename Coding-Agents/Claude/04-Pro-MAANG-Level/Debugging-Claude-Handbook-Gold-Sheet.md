# Debugging Claude Handbook — Gold Sheet

> **Track**: Claude Mastery Track — Group 4: Pro / Production Level
> **File**: 3 of 5 (Track File #23)
> **Read after**: SDLC-Automation-Gold-Sheet.md

---

## 1. The 20 Claude Failure Modes

### Category 1 — Setup Issues

#### Failure 1: CLAUDE.md Not Loading

```
Symptom: Claude gives generic output, ignores project conventions.
Diagnose: claude "What project rules do you have?"
          If answer is generic: CLAUDE.md not found.
Root cause: Wrong file location or file not present.
Fix:
  ls -la CLAUDE.md   ← must be at project root
  Check: .claude/CLAUDE.md is NOT the right location (that's for commands/agents)
  Right location: ./CLAUDE.md at root of project
Prevention: Run the verification prompt after creating CLAUDE.md.
```

#### Failure 2: Slash Command Not Found

```
Symptom: /debug not found when typed in Claude Code.
Root cause: File not in .claude/commands/ or wrong extension.
Fix:
  ls .claude/commands/   ← verify files exist
  Check: files end in .cmd.md (not .md or .prompt.md)
  Check: frontmatter is valid YAML
Prevention: After creating a command, test it immediately with /command-name.
```

#### Failure 3: Hook Not Executing

```
Symptom: Dangerous commands run without being blocked.
Root cause: Hook file not executable or wrong location.
Fix:
  ls -la .claude/hooks/   ← verify files exist
  chmod +x .claude/hooks/pre_tool_use.sh
  Test manually: bash .claude/hooks/pre_tool_use.sh "bash" "rm -rf /"
Prevention: Test hooks manually after creating them.
```

---

### Category 2 — Output Quality Issues

#### Failure 4: Claude Over-Engineers

```
Symptom: Simple change produces 5 new classes, factory patterns, registry patterns.
Root cause: Vague prompt. Claude fills gaps with "standard" software patterns.
Diagnose: "Did I ask for this abstraction?" If no → prompt was too vague.
Fix:
  Stop. "Remove the [abstraction]. The simple solution is: [describe what you want]."
  Restart with: "Do NOT add new classes or abstractions beyond what's asked."
Prevention: Add "Do NOT add abstractions not needed for this specific change" to CLAUDE.md.
```

#### Failure 5: Claude Hallucinates APIs

```
Symptom: Claude uses methods like session.execute_async() that don't exist.
Root cause: Training data includes incorrect code or Claude interpolates method names.
Diagnose: AttributeError at runtime — the method doesn't exist.
Fix:
  "This method doesn't exist: [method]. The correct method is: [correct method]."
  Specify library version in CLAUDE.md: "SQLAlchemy 2.x — use select() statement."
Prevention: Add library versions to CLAUDE.md. Run tests immediately after generation.
```

#### Failure 6: Context Drift — Claude Forgets Earlier Decisions

```
Symptom: Claude contradicts a decision it made earlier in the session.
Root cause: Session is too long. Earlier context has low attention weight.
Diagnose: Session > 40 exchanges, or Claude says "I don't recall X" (it said X earlier).
Fix:
  "Summarize the decisions we've made in this session."
  Start a new session with the summary as context.
Prevention: Keep sessions to one task. Use resume pattern between sessions.
```

#### Failure 7: Circular Reasoning Loop

```
Symptom: Claude tries the same fix 3+ times with minor variations, no progress.
Root cause: Claude can't identify the real root cause with current context.
Fix:
  Stop the loop. "Stop trying that approach. It's not working."
  Provide more context: "The actual root cause is [X]. Fix it."
  Or: switch to @debugger subagent with fresh context.
Prevention: Set explicit stopping conditions: "Stop after 3 failed attempts."
```

---

### Category 3 — Safety Issues

#### Failure 8: Claude Modifies Tests to Make Them Pass

```
Symptom: Tests "pass" but Claude changed them to match wrong implementation.
Root cause: Insufficient constraint on test files.
Fix:
  git diff tests/   ← check if Claude modified tests
  git checkout tests/   ← restore original tests
  Restart: "Tests define correct behavior. NEVER modify test files."
Prevention: Add to CLAUDE.md: "Never modify test files to make tests pass."
           Add to hooks: log any edit to tests/ as a warning.
```

#### Failure 9: Secrets in Generated Code

```
Symptom: Claude generates code with real-looking credentials.
Root cause: Claude inferred values from context you provided.
Diagnose: git diff | grep -i "key\|secret\|password\|token"
Fix: Immediately remove and never commit. Rotate if real credentials were used as examples.
Prevention: Use placeholder values in all examples you show Claude.
```

#### Failure 10: Scope Creep — Claude Modifies Unexpected Files

```
Symptom: Claude modifies files you didn't ask it to touch.
Root cause: Task was scoped too broadly or Claude followed an import chain.
Fix:
  git checkout [unexpected file]   ← restore the file
  Restart with: "Only modify [explicit list]. Do NOT touch any other file."
Prevention: Always include "Do NOT touch: [list]" in agent session prompts.
```

---

### Category 4 — Performance Issues

#### Failure 11: Claude is Very Slow

```
Symptom: Claude takes 30+ seconds to respond.
Root cause: Too much context in the prompt, or strong model being used for simple task.
Fix:
  Use faster model: --model claude-haiku-3-5
  Reduce context: remove unneeded file references
  Start fresh session to clear conversation history
Prevention: Match model to task complexity. Use Haiku for simple tasks.
```

#### Failure 12: Context Window Exceeded Mid-Task

```
Symptom: Claude's response is truncated, or Claude says it can't continue.
Root cause: Session accumulated too much context.
Fix:
  Summarize: "Summarize what we've done so far in 200 words."
  Start new session with summary.
Prevention: Keep sessions short and focused. Chunk large tasks.
```

---

### Category 5 — Agent Mode Issues

#### Failure 13: Agent Loop Runs Indefinitely

```
Symptom: Claude keeps trying to fix the same error with no progress.
Root cause: Stopping conditions not defined. Claude doesn't know when to give up.
Fix: Stop the session. Provide explicit stopping conditions in restart.
Prevention: "Stop after 3 failed attempts on the same error. Report the failure."
```

#### Failure 14: Agent Runs Migrations Automatically

```
Symptom: Claude runs alembic upgrade or database migration without asking.
Root cause: CLAUDE.md doesn't explicitly forbid migrations.
Fix:
  Review what migration ran: git log --all --oneline
  Revert if in development: alembic downgrade -1
Prevention:
  CLAUDE.md: "Never run database migrations. Flag that migration is needed and stop."
  Hook: pre_tool_use.sh blocks "alembic upgrade" commands.
```

---

### Quick Reference: Failure → First Fix

```
| Failure | First Fix |
|---------|-----------|
| CLAUDE.md ignored | Verify ./CLAUDE.md exists at root, reload session |
| Command not found | Check .claude/commands/ + .cmd.md extension |
| Hook not blocking | chmod +x hook file, test manually |
| Over-engineering | Stop → "Remove [X]. Do NOT add abstractions." |
| Hallucinated API | Name correct method, add version to CLAUDE.md |
| Context drift | Summarize session, start fresh |
| Circular loop | Stop → "Try different approach" → subagent if needed |
| Tests modified | git checkout tests/ → "Never modify tests" |
| Secrets generated | Remove, rotate, use placeholders in future |
| Scope creep | git checkout [file] → explicit "Do NOT touch" list |
| Agent runs migrations | Pre-tool hook + CLAUDE.md rule |
| Context exceeded | Summarize → new session with summary |
```

---

## 2. Revision Checklist

- [ ] Knows first fix for all 14 failure modes in the quick reference
- [ ] Has CLAUDE.md rule against test modification
- [ ] Has pre_tool_use.sh hook blocking migrations and dangerous commands
- [ ] Always sets stopping conditions in agent loop prompts
- [ ] Uses git diff to audit changes before committing
- [ ] Can detect context drift and knows the summarize-then-restart fix
