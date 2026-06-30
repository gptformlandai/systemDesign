# Codex Active Recall Question Bank

> **Track**: Codex Mastery Track — Group 6: Practice & Upgrade
> **File**: 1 of 6 (Track File #30)
> **Audience**: All levels — use for self-testing at the end of each section
> **Format**: Test yourself — cover the answer, write yours, then compare

---

## Section 1 — Foundations (Q1–Q8)

**Q1**: What is the difference between Codex CLI and GitHub Copilot?

> Codex CLI is a terminal-based agent that reads and writes files, runs commands, and operates with plan-execute-verify loops. GitHub Copilot is an IDE-integrated autocomplete/chat assistant that works within the editor and does not run commands autonomously. Codex CLI is released April 2025 as a separate open-source tool.

---

**Q2**: What are the three approval policy modes and when would you use each?

> `suggest`: proposes changes without applying — use for code review, planning, security analysis. `auto-edit`: applies file edits, asks before running commands — default for most development work. `full-auto`: fully autonomous, runs commands and edits without asking — use only on a feature branch with a git checkpoint.

---

**Q3**: What is AGENTS.md and what should it contain?

> AGENTS.md is a project-level instruction file that Codex reads automatically at the start of every session. It should contain: project context and tech stack, architecture conventions and layering rules, coding standards (naming, error handling, test patterns), forbidden actions (destructive operations Codex must never do), and the verification command.

---

**Q4**: What are the 4 ingredients of a high-quality Codex prompt?

> Scope (what files), Goal (what to do), Constraints (what not to do), Verification (command that proves it worked). A prompt missing any of these is incomplete.

---

**Q5**: What is the verification command pattern and why is it critical?

> The verification command (e.g., `pytest tests/test_foo.py -x`) is specified in the prompt as the test Codex must pass before considering the task done. Without it, "done" means "Codex thinks it's done." With it, "done" means the tests pass.

---

**Q6**: List 3 types of content that must never appear in a Codex prompt.

> (1) Secrets or API keys. (2) Real personally identifiable information (PII) — use synthetic/anonymized data instead. (3) Security bypass instructions (prompts that ask Codex to circumvent auth or security checks).

---

**Q7**: What are the default model and approval policy in config.yaml?

> Sensible defaults: `model: o4-mini`, `approval_policy: auto-edit`. These can be overridden per session with `--model` and `--approval-policy` flags.

---

**Q8**: What does the git checkpoint rule require?

> Before any full-auto session: `git add -A && git commit -m "checkpoint: [description]"`. This creates a safe recovery point. After the session: `git diff HEAD~1 --stat` to review every change.

---

## Section 2 — Intermediate (Q9–Q16)

**Q9**: What is context drift and how do you detect it?

> Context drift is when Codex starts producing output inconsistent with project conventions mid-session — using wrong error types, wrong naming conventions, or ignoring AGENTS.md rules. Detection: compare output against AGENTS.md. Fix: `/compact` + restate the constraint, or start a fresh session with explicit context.

---

**Q10**: Describe the test-first TDD loop with Codex.

> (1) Write tests that define expected behavior — do not implement yet. (2) Run the tests: they should fail (implementation doesn't exist). (3) Tell Codex: "Make these tests pass. Do not modify test files." (4) Run verification. (5) If gaps exist: add more tests, repeat. The test file is the spec — Codex implements to make it green.

---

**Q11**: What is the "do not invent" constraint and when must it appear?

> "Do not invent behavior — only document what is verifiably in the code." It must appear in every documentation prompt. Without it, Codex will fabricate plausible-sounding examples, parameter descriptions, or behavior that doesn't match the implementation.

---

**Q12**: What are the 6 steps in the full-auto pre-flight checklist?

> (1) On feature branch. (2) Git checkpoint committed. (3) Task is bounded (2-sentence description). (4) File scope explicit in prompt. (5) Forbidden actions stated. (6) Verification command included.

---

**Q13**: What is the difference between high-signal and low-signal context?

> High-signal: exact failing test output, specific file, current error message. Low-signal: entire src/ directory, "I've been working on the payments module," verbose previous responses. Load only the files relevant to the task.

---

**Q14**: When should you use `--system-prompt` instead of AGENTS.md?

> Use `--system-prompt` for session-level role personas that are not always needed — security reviewer, technical writer, code reviewer, architect. AGENTS.md is permanent project memory (applies always). `--system-prompt` sets a temporary role for one session.

---

**Q15**: What is over-mocking and why is it a red flag?

> Over-mocking is mocking functions from the same codebase (your own service or repository) instead of only mocking external dependencies. It produces tests that pass even when the code is broken because the real implementation is never called. Only mock: external HTTP calls, database drivers (not your own repository), cloud SDK calls.

---

**Q16**: What are the 4 stopping conditions every agent loop must have?

> (1) Iteration limit (e.g., 5 attempts on the same failure). (2) Scope violation trigger (stop if fix requires test file modification). (3) Infrastructure trigger (stop if fix requires migration or infra change). (4) Diminishing returns (stop after 3 different approaches all fail).

---

## Section 3 — Advanced & Pro (Q17–Q30)

**Q17**: When should you use gpt-4.1 vs o4-mini vs gpt-4.1-mini?

> `gpt-4.1`: architecture design, complex debugging, security reviews, multi-file reasoning. `o4-mini`: 90% of tasks — implementation, test generation, refactoring, quick reviews. `gpt-4.1-mini`: documentation-only tasks (fastest + cheapest for pure docstrings/README).

---

**Q18**: Describe the multi-agent pipeline pattern (4 phases).

> (1) Planner: reads codebase, produces implementation plan + handoff doc. (2) Builder: takes handoff doc, implements, passes tests. (3) Tester: gap analysis — what is NOT covered by current tests? (4) Reviewer: security + architecture review of all changes. Each phase uses context isolation — a fresh session with the handoff doc as input.

---

**Q19**: What is the compound benefit of autonomous workflows?

> Autonomous workflows shift your time from "how do I implement this?" to "is this the right implementation?" The leverage is cognitive: you spend time on architecture, product decisions, and security review — the decisions only you can make — instead of implementation mechanics.

---

**Q20**: What does a complete AGENTS.md contain? List the 6 sections.

> (1) Project context (what it is, tech stack, team size). (2) Architecture (layers, patterns, dependencies between modules). (3) Coding standards (naming, error types, test patterns, response format). (4) Forbidden actions (no migrations, no git push, no PII logging). (5) Verification command (the command to run to prove work is done). (6) Optional: subfolder overrides (different conventions for a specific subdirectory).

---

**Q21**: What is /compact and what is lost when you use it?

> `/compact` summarizes the conversation history into a compact form to free up context window headroom. What is lost: exact earlier outputs, specific intermediate outputs from earlier steps. What is kept: the essence of completed work and current context. Use between tasks, not mid-task.

---

**Q22**: Describe the 5 recovery patterns when full-auto goes wrong.

> (A) Interrupt immediately with Ctrl+C. (B) Full rollback: `git reset --hard HEAD~1`. (C) Partial rollback: `git checkout -- [specific file]`. (D) Undo a bad commit: `git revert HEAD`. (E) Undo multiple commits: `git reset HEAD~N --soft` to inspect staged changes.

---

**Q23**: What is tautological test detection? Give an example.

> A tautological test always passes because it tests the mock, not the implementation. Example: `order_service.get_order = Mock(return_value={"id": 1}); result = order_service.get_order(1); assert result == {"id": 1}` — this passes even if the real get_order() is completely broken. Detection: ask Codex "which tests would still pass if get_order() always returned None?"

---

**Q24**: What is the constraint architecture for what goes in a task prompt vs AGENTS.md vs system prompt?

> Task prompt: specific task, file scope, task-specific constraints, verification override. AGENTS.md: permanent project conventions, architecture rules, forbidden actions, tech stack. System prompt: session role/persona, output format for this session, review criteria. Don't mix these — each level has a purpose.

---

**Q25**: What are 5 signs that an agent loop is stuck?

> (1) Same test fails for 5+ iterations. (2) Codex makes increasingly broad changes. (3) Codex modifies test files. (4) Number of failing tests increases. (5) Codex starts changing files outside the stated scope.

---

**Q26**: What is the difference between the OpenAI API and the Codex CLI? When do you use each?

> CLI: interactive sessions, file editing with approval, multi-file context discovery, AGENTS.md-aware. Use for 95% of coding work. API (direct): programmatic integration, batch processing, building tools on top of Codex, when you need raw JSON response. Use when embedding Codex reasoning in your own tools.

---

**Q27**: Describe the Personal Codex Operating System (5 components).

> (1) AGENTS.md: living project spec. (2) Script library: reusable shell scripts for daily tasks. (3) config.yaml: model and approval policy defaults. (4) Daily ritual: morning planning → coding → pre-commit → end-of-day. (5) Maintenance ritual: weekly AGENTS.md review, monthly script/prompt pruning.

---

**Q28**: What are the 10 phases of SDLC that Codex can assist with?

> (1) Requirements clarity. (2) Architecture planning. (3) Test-first specification. (4) Implementation. (5) Coverage audit. (6) Pre-commit review. (7) PR description. (8) CI failure resolution. (9) Documentation update. (10) Post-incident learning.

---

**Q29**: What is the most important documentation rule and what happens without it?

> "Do not invent — only document what is verifiably in the code." Without it: Codex fabricates plausible examples, parameters that don't exist, and behavior descriptions that don't match the implementation. The documentation will be wrong and will mislead future developers.

---

**Q30**: What must the post-session review include after every full-auto run?

> (1) `git diff HEAD~1 --stat` — count of files changed. (2) `git diff HEAD~1` — read every line changed. (3) Run the full test suite. (4) Verify no files outside stated scope were modified. (5) Security check of any code that touches auth, payments, or user data. Tests passing does NOT replace this review.

---

## Scoring

Score your recall run:
- 26-30 correct → Pro level. Ready for production autonomy.
- 20-25 correct → Advanced. Review the sections you missed.
- 14-19 correct → Intermediate. Re-read the relevant gold sheets.
- Below 14 → Foundations need reinforcement. Return to Section 1-2.
