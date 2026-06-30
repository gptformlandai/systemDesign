# Claude Active Recall Question Bank

> **Track**: Claude Mastery Track — Group 6: Practice Upgrade
> **File**: 1 of 5 (Track File #30)
<<<<<<< HEAD
> **Usage**: Answer from memory first, then verify against the gold sheets

---

## Foundations (Sheets 1-6)

**Q1**: What are the three Claude surfaces and the best use case for each?

**Q2**: What does Claude Code have that Claude Chat does NOT have?

**Q3**: What is the 200k token window equivalent to in practical terms (pages, files)?

**Q4**: What three things does CLAUDE.md replace that you would otherwise repeat every session?

**Q5**: What is the "first draft" rule and why does it change how you use Claude?

**Q6**: List 5 types of content that must NEVER be pasted into Claude.

**Q7**: What is the checkpoint commit and when is it required?

**Q8**: Name 5 CRISP elements and give an example of each.

**Q9**: What is the difference between a constraint ("Do NOT X") and an instruction ("Do Y")?

**Q10**: When should you use XML tags in a prompt?

---

## Intermediate (Sheets 7-13)

**Q11**: What are the three CLAUDE.md levels and what goes in each?

**Q12**: What is the difference between CLAUDE.md content and prompt content?

**Q13**: What file extension do slash commands use and where do they live?

**Q14**: What does $ARGUMENTS do in a slash command file?

**Q15**: What are the two signs that context drift has started in a session?

**Q16**: What is the mid-session summarize-and-compress pattern and when do you use it?

**Q17**: What is the resume pattern? Write the template from memory.

**Q18**: How does Claude read your repository in a Claude Code session?

**Q19**: What does the verification loop look like (Claude's perspective)?

**Q20**: Name the 6 things the pre_tool_use.sh hook should block.

---

## Advanced (Sheets 14-20)

**Q21**: What is context isolation and why does it make review and testing better?

**Q22**: Name the 7 specialist agents and their primary responsibility in one sentence each.

**Q23**: What does a SKILL.md file contain and how does it get invoked?

**Q24**: What is the difference between a slash command and a SKILL?

**Q25**: What are the three hook files and when does each run?

**Q26**: What exit code blocks a tool call in pre_tool_use.sh?

**Q27**: What is the three-tier context architecture (tier 1, 2, 3)?

**Q28**: When should you use Haiku vs Sonnet vs Opus?

**Q29**: What is the 2-strike escalation rule?

**Q30**: What is context poisoning and name two defense patterns?

**Q31**: What is the canonical 4-agent pipeline order?

**Q32**: What should a handoff document include? What should it NEVER include?

**Q33**: When can agents run in parallel?

**Q34**: What is an MCP server and what can it do that Claude Code cannot do natively?

---

## Pro Level (Sheets 21-25)

**Q35**: What are the 5 components of a Personal Claude OS?

**Q36**: Write the morning planning ritual opening prompt from memory.

**Q37**: What is the "fix the implementation, never the tests" rule and why does it matter?

**Q38**: What are the 6 verification gates in order?

**Q39**: Name 3 stopping conditions that should terminate a verification loop.

**Q40**: What are the 5 metrics for measuring if your Claude OS is working?

**Q41**: What is the SDLC phase sequence and which Claude mode handles each?

**Q42**: What makes @reviewer's fresh context valuable vs having the builder review its own work?

**Q43**: Name 5 Claude failure modes from the debugging handbook and their first fix.

**Q44**: What is the multi-agent anti-pattern "context leakage" and how do you prevent it?

**Q45**: What does Level 5 Claude OS maturity look like?

---

## Applied Questions (scenario-based)

**Q46**: You're debugging a race condition in async code. What model do you use and why?

**Q47**: A junior developer asked Claude to "make the code cleaner." 
Write the better version of that prompt.

**Q48**: You're starting a session continuing from yesterday. Write the full resume pattern.

**Q49**: @builder finishes 4/5 files and hits an architectural blocker. What do you do?

**Q50**: You paste a user-submitted code snippet to ask Claude to analyze it.
What security technique do you use and why?
=======
> **Usage**: Cover the answer, recall from memory, then reveal to verify. Re-run weak questions until you score 100%.

---

## Foundations (Sheets 1–6)

**Q1:** What makes Claude different from ChatGPT and GitHub Copilot for development tasks?

**A1:** Claude excels at long context reasoning (200k token window), structured output, and extended multi-step reasoning. It reads entire files and multi-file codebases accurately. ChatGPT is stronger at general conversation; Copilot specializes in inline code completion within an IDE. Claude Code CLI is Claude's agentic development mode — it can read, write, and run code autonomously with tool access, unlike ChatGPT which is primarily chat.

---

**Q2:** What is CLAUDE.md and why does it exist?

**A2:** CLAUDE.md is a plain text file placed at the root of your project (or in a subfolder). Claude Code reads it at the start of every session, giving Claude persistent project-level context: tech stack, architecture patterns, coding conventions, forbidden patterns, and project-specific rules. Without it, Claude starts every session with no knowledge of your project and produces generic output that violates your conventions.

---

**Q3:** What is CRISP and what does each letter stand for?

**A3:** CRISP is a structured prompt framework: **C**ontext (background facts Claude needs), **R**ole (perspective or expertise to adopt), **I**nstruction (the specific task), **S**cope (constraints on what to include or exclude), **P**rocess (how to approach it, e.g., step-by-step, numbered list). Using CRISP produces precise, reviewable output instead of open-ended responses.

---

**Q4:** What are the non-negotiable safe usage rules for Claude?

**A4:** Never paste real credentials (API keys, tokens, passwords) into Claude — they may be logged. Never paste real customer PII (names, emails, SSNs) — it's a privacy/compliance breach. Always review generated code before accepting — it compiles ≠ it is correct. Always anonymize data before debugging production incidents. Commit before agent sessions so you can recover from wrong output. Never commit code you can't explain line by line.

---

**Q5:** What is the difference between Claude Chat Projects and a regular Claude Chat conversation?

**A5:** A Project has persistent instructions (like a system prompt) set once and applied to all conversations within the project. It also has a shared file context across conversations. A regular conversation is ephemeral — starts from scratch each time. Use Projects for ongoing work on a specific codebase or domain; use regular conversations for one-off questions.

---

**Q6:** What are the 10 Claude Quick Win workflows for beginners?

**A6:** (1) Explain unfamiliar code with structured questions, (2) Debug an error with exact message + code + what changed, (3) Generate a docstring for a function, (4) Generate unit tests for an existing function, (5) Plan a task before implementing, (6) Generate a PR description, (7) Refactor a function with a specific goal and constraints, (8) Generate a README section, (9) Explain a framework/library concept, (10) Review code for one specific concern (security or test gaps).

---

## Intermediate Power User (Sheets 7–13)

**Q7:** What belongs in a CLAUDE.md file? List the 6 key sections.

**A7:** (1) Project overview — what the project does and its tech stack. (2) Architecture rules — the patterns in use and why. (3) Coding conventions — naming, file structure, error handling style. (4) Forbidden patterns — things Claude must never do (e.g., string interpolation in SQL, modifying tests). (5) Testing requirements — coverage expectations, mock patterns, test naming. (6) Development workflow — how to run tests, lint, build.

---

**Q8:** What is a slash command in Claude Code? How do you create one?

**A8:** A slash command is a reusable prompt template stored as a `.cmd.md` file in `.claude/commands/`. When you type `/command-name` in Claude Code, Claude reads the file and executes the prompt. Create one by creating `.claude/commands/debug.cmd.md` with a YAML frontmatter block and the prompt body. The file is immediately available as `/debug` in the current session.

---

**Q9:** What is context drift and how do you fix it?

**A9:** Context drift is when Claude starts ignoring or contradicting rules and decisions stated earlier in a long session. It happens because older context has less "attention weight" as the session grows. Detection: Claude uses the wrong naming convention or says "I don't recall" something clearly discussed earlier. Fix: summarize the session ("summarize the key decisions we've made"), then start a new session with the summary as the opening context.

---

**Q10:** What are the 4 parts of an effective debugging prompt?

**A10:** (1) The exact error message including the full type (not "it's broken"). (2) The relevant code section — only the file/function where the error occurs, not the whole codebase. (3) Context — what changed recently, the stack and framework, what you expected vs what happened. (4) What you want back — "diagnose the root cause + show the fix" rather than just "help me".

---

**Q11:** What is the working set in Claude Code edits mode? What makes it too large vs too small?

**A11:** The working set is the list of files Claude is allowed to read and modify in an edits session. Too large: Claude makes unnecessary changes to unrelated files, produces noisy diffs, and quality degrades. Too small: Claude makes incomplete changes because it can't see dependent code. Right-sized: only the files directly being changed plus 1-2 closely related files (schema, related service). 

---

**Q12:** What is the Before-After prompt pattern for debugging?

**A12:** BEFORE (wrong): "My API is broken". AFTER (correct): Error message + relevant code + context (what changed). The three-piece pattern always outperforms symptom-only prompts because Claude needs the exact error to diagnose root cause, the code to see where the bug is, and the context (what changed) to understand why it broke now.

---

**Q13:** What makes a test tautological? Give an example.

**A13:** A tautological test passes regardless of whether the implementation is correct. Example: mocking the function you're testing and asserting on the mock's return value — `mock_service.process_order.return_value = expected; assert result == expected` (tests the mock, not the code). Another form: mocking everything so thoroughly that the test never exercises real logic. Detection: "If I delete the function body, does the test still pass?"

---

## Advanced Engineering (Sheets 14–20)

**Q14:** What is a subagent in Claude Code? When should you use one?

**A14:** A subagent is a Claude Code session invoked with its own clean context window, focused instructions, and specific role. Defined in `.claude/agents/[name].agent.md`. Use subagents when: a task has distinct phases (plan vs build vs test), a session is drifting (contradicting earlier decisions), or you need specialist behavior (the @debugger agent for root cause analysis only). Do not use for simple tasks that fit in one focused session.

---

**Q15:** What is a SKILL.md? How does it differ from a slash command?

**A15:** A SKILL.md is invoked automatically when Claude detects a relevant task — it's reactive. A slash command is invoked manually with `/name` — it's proactive. SKILL.md lives in `.claude/skills/[category]/SKILL.md` and activates when the task matches the skill's trigger description. A testing SKILL.md fires when Claude is writing tests; a refactoring SKILL.md fires when Claude is restructuring code. Write skills for behaviors you want without having to remember to ask for them.

---

**Q16:** What does a pre_tool_use.sh hook do? Give 3 examples of what it should block.

**A16:** pre_tool_use.sh runs before every tool call Claude makes, receiving the tool name and arguments. It returns exit code 0 to allow and non-zero to block. It should block: (1) `alembic upgrade` — migrations should never run automatically, (2) `rm -rf` — destructive file operations, (3) `git push --force` — force pushes to shared branches. The hook is the safety layer between autonomous Claude sessions and irreversible operations.

---

**Q17:** What is the difference between MCP client and MCP server? What is Claude's role?

**A17:** In MCP, the client is the AI (Claude Code) — it sends tool call requests. The server is the external process providing tools (GitHub, filesystem, browser, DB). Claude is the MCP client. When Claude calls the GitHub MCP tool, it sends a request to the MCP server process running locally, which calls the GitHub API and returns the result to Claude. Claude never calls GitHub directly — the MCP server is the intermediary.

---

**Q18:** What is the verification loop pattern? What are its 4 components?

**A18:** A verification loop is: Claude generates → runs a check → reads result → fixes if failed → loops until passing. Four components: (1) ACTION — what Claude generates, (2) CHECK — the command to verify (pytest, ruff, mypy), (3) SUCCESS CONDITION — what passing output looks like, (4) FAILURE RESPONSE — what Claude does on failure (fix implementation, try different approach, or stop after N attempts). Without all 4, the loop is incomplete.

---

**Q19:** What is the Planner → Builder → Tester → Reviewer pipeline? Why not do it all in one session?

**A19:** A 4-phase multi-agent pipeline where each phase is a fresh Claude session with focused context. @planner creates the implementation plan. @builder implements per the plan. @tester generates and runs tests. @reviewer checks security, coverage, quality. One-session approach fails for large features because: context drift causes Claude to contradict earlier decisions, planning context and implementation detail compete for attention, and quality degrades as the session grows long.

---

**Q20:** What is context scoping? Why not always use @codebase?

**A20:** Context scoping means providing only the files directly relevant to the task — not the entire codebase. @codebase loads all indexed files. Quality degrades with more context because: Claude's attention distributes across all tokens, relevant signal is diluted by irrelevant content, and performance slows as more tokens are processed. Use @file for known-target tasks; use @codebase only for exploration queries ("find all usages of X across the codebase").

---

## Pro / Production Level (Sheets 21–25)

**Q21:** What are the 5 components of a personal Claude OS?

**A21:** (1) Configuration layer — CLAUDE.md files capturing project rules and conventions. (2) Command library — .claude/commands/ slash commands for every repeated workflow. (3) Agent library — .claude/agents/ specialist subagents. (4) Skills library — .claude/skills/ auto-invoked capability modules. (5) Hook layer — .claude/hooks/ validation scripts that run before/after every tool use. Plus: daily rituals and a recovery pattern library.

---

**Q22:** Name the 12 SDLC phases and the Claude mode best suited for each.

**A22:** (1) Requirements — Chat Ask / @planner, (2) Architecture — Chat Ask / @architect, (3) API Design — CLI / @architect, (4) Test-first generation — CLI / @tester, (5) Implementation — CLI agent / @builder, (6) Test coverage — CLI / @tester, (7) Self-review — CLI /review / @reviewer, (8) Documentation — CLI / @builder, (9) PR creation — CLI / /build, (10) CI/CD — CLI / @builder, (11) Incident debugging — CLI / @debugger (anonymized data only), (12) Learning capture — Chat / /generate-notes.

---

**Q23:** What are the 14 Claude failure modes? Name them from memory.

**A23:** (1) CLAUDE.md not loading, (2) Slash command not found, (3) Hook not executing, (4) Over-engineering (unnecessary abstractions), (5) Hallucinated APIs, (6) Context drift (forgetting earlier decisions), (7) Circular reasoning loop, (8) Modifying tests to pass, (9) Secrets in generated code, (10) Scope creep (unexpected file modifications), (11) Very slow response, (12) Context window exceeded mid-task, (13) Agent loop runs indefinitely, (14) Runs migrations automatically.

---

**Q24:** What is the safe autonomy checklist? List all 7 items.

**A24:** Before every autonomous session: (1) `git commit` checkpoint before the session, (2) CLAUDE.md is in place and accurate, (3) Scope is written — exactly which files Claude will touch, (4) Forbidden actions are explicit — "Do NOT run migrations / modify tests / touch [file]", (5) Stopping conditions defined — when Claude should stop and ask vs proceed, (6) Verification command defined — what proves success, (7) Time estimate — if it's taking longer than expected, stop and report.

---

**Q25:** What is verification-driven development with Claude? How does "done" differ from just "tests pass"?

**A25:** Verification-driven development means Claude generates code and immediately verifies with automated checks before declaring done. "Done" in VDD means: tests pass + lint passes + type check passes (if applicable) + no regressions in previously passing tests. Not just "it compiles" or "tests pass". The verification loop is the quality gate — without it, Claude produces plausible-looking code that fails at runtime or has security issues not caught by tests.

---

## Scenario Practice (Sheets 26–29)

**Q26:** What are the 3 beginner daily workflow scenarios about?

**A26:** B1 — Explain code before touching it (structured Q&A to understand a function), B2 — Document then improve (docstring, edge case discovery, add validation), B3 — Plan a change without implementing (identify which lines change, what tests to write first, what could go wrong). These build the habits: understand before modify, document as you go, plan before code.

---

**Q27:** What 4 inputs make a production incident debugging prompt effective?

**A27:** (1) Symptoms — observable behavior (not guesses about cause), (2) Sanitized error — the actual log line with no real user data, (3) Relevant code — the file/function Claude should analyze, (4) Structured output request — ranked root causes, immediate mitigation, proper fix, and the test that would have caught it. Providing all 4 produces actionable diagnosis; providing only symptoms produces generic guesses.

---

**Q28:** What is the CRESTS code review framework?

**A28:** **C**orrectness — logic errors, edge cases, null handling. **R**isk — security vulnerabilities, auth bypass, data exposure. **E**rror handling — unhandled exceptions, silent failures. **S**tyle — naming, conventions, readability. **T**ests — coverage gaps, tautological tests. **S**cope — unexpected changes, breaking changes. Used as a scoring rubric after a full code review sprint to quantify how thorough the review was.

---

**Q29:** Why write tests BEFORE implementation (test-first)? What goes wrong with the reverse?

**A29:** Test-first forces you to define correct behavior before writing code. When you write tests after implementation, Claude generates tests that describe what the code DOES — including any bugs. Tests become validators of existing behavior rather than specifications of required behavior. The implementation can be wrong and all tests will pass. Test-first makes tests independent truth-sources; implementation-first makes tests implementation mirrors.

---

**Q30:** What 3 things should you never skip in the pre-commit review?

**A30:** (1) Security review for any code touching auth, SQL queries, or user input — these are the areas where generated code most commonly has CRITICAL vulnerabilities. (2) Test gap analysis — find what error paths are untested in the new code. (3) `git diff` review — check for unexpected files changed, secrets in the diff, and test file modifications. Never commit without all 3.

---

## Revision Score

| Date | Score | Weak Areas to Re-study |
|------|-------|----------------------|
| | /30 | |
| | /30 | |
| | /30 | |
>>>>>>> refs/remotes/origin/main
