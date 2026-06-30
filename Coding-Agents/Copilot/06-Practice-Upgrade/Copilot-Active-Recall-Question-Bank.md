# Copilot Active Recall Question Bank

> **Track**: Copilot Mastery Track — Group 6: Practice Upgrade
> **File**: 1 of 5 (Track File #32)
> **Usage**: Cover the answer, recall from memory, then reveal to verify. Re-run weak questions until you score 100%.

---

## Foundations (Sheets 1-6)

**Q1:** What are the 6 Copilot surfaces? Name each and its best use case.

**A1:** Inline suggestions (code completion while typing), Chat Ask mode (questions, explanations), Chat Edit mode (targeted multi-file edits with diff review), Agent Mode (autonomous multi-file tasks), Code Review (automated PR feedback on GitHub.com), Copilot CLI / `gh copilot` (terminal commands — explain and suggest).

---

**Q2:** What context does Copilot see during inline suggestions (autocomplete)?

**A2:** The current file's content around the cursor, other open tabs (nearby files), and `copilot-instructions.md` if present. It does NOT see your entire repo — only what's open or visible near the cursor.

---

**Q3:** What context does Copilot NOT automatically see during Chat?

**A3:** Any file you have not explicitly referenced. Copilot Chat does not read your filesystem automatically — you must attach files using `#file:`, `#selection`, `#codebase`, `#sym`, or `#editor`. Without these, Copilot only sees the current conversation text.

---

**Q4:** What is the "first draft" mental model? Why is it important?

**A4:** Copilot output is always a first draft — generated code may compile and still contain logical bugs, security vulnerabilities, hallucinated API methods, or outdated patterns. The mental model matters because it prevents accepting output blindly. Every Copilot output requires human review before being treated as correct.

---

**Q5:** Name the 5 context variables available in Chat. What does each attach?

**A5:** `#file:path` — attaches a specific file; `#selection` — attaches currently selected text; `#codebase` — triggers semantic search of repo index; `#sym:SymbolName` — attaches a specific class/function; `#editor` — attaches the full content of the active file. Bonus: `#terminalLastCommand`, `#terminalSelection`, `#problems`.

---

**Q6:** When should you start a NEW Chat conversation vs continue the same one?

**A6:** Start a new conversation when: switching to a completely different topic, the current conversation has become long and unfocused (context window pressure), or the previous response was wrong and continuing risks building on bad context. Continue the same conversation when the next question is a follow-up on the same code or task.

---

**Q7:** What does `#codebase` do differently from `#file`?

**A7:** `#file` attaches the literal content of a specific file. `#codebase` triggers a semantic search of your repository index — Copilot searches for relevant chunks across the codebase and injects the most relevant pieces. Use `#file` when you know exactly what file you need; use `#codebase` for "find anything related to X" questions.

---

**Q8:** Name the 6 built-in Chat slash commands.

**A8:** `/explain`, `/fix`, `/tests`, `/doc`, `/simplify`, `/new`. Each operates on selected code or the active file. These are VS Code built-ins — distinct from your custom prompt file slash commands.

---

**Q9:** What are the 3 pieces of information you should never paste into Copilot?

**A9:** API keys, tokens, passwords, or private keys (any credentials); real customer data including emails, names, SSNs, or payment data (PII); database connection strings with real passwords. Use synthetic/anonymized equivalents for all debugging examples.

---

**Q10:** What is the recovery command after a bad Agent Mode session?

**A10:** `git checkout .` — discards all unstaged changes and reverts the working tree to the last commit. This is why you must always `git add . && git commit -m "checkpoint: ..."` BEFORE starting Agent Mode. Without the checkpoint commit, recovery may lose legitimate work.

---

## Intermediate Power User (Sheets 7-13)

**Q11:** Where must `copilot-instructions.md` be located?

**A11:** `.github/copilot-instructions.md` at the repository root. This is the only location VS Code Copilot automatically reads. Files at other paths are not auto-loaded (though they can be attached manually with `#file:`).

---

**Q12:** What does the `applyTo` field in path-specific instruction frontmatter do?

**A12:** It restricts the instruction file to only apply when Copilot is working with files matching the glob pattern. Example: `applyTo: "**/*.test.ts"` means the instruction only activates for TypeScript test files — not for source files. Placed in `.github/instructions/` folder.

---

**Q13:** What is the ideal length of a `copilot-instructions.md` file and why?

**A13:** Under 500 words (ideally 150-300). Shorter instructions are followed more reliably. Long files dilute attention — the model's probability of following instruction #47 in a 3000-word file is much lower than following instruction #3 in a 200-word file. Every instruction should be testable.

---

**Q14:** What is the "Principle 2" of instruction design (must change default behavior)?

**A14:** Instructions must tell Copilot to do something different from what it would do by default. "Write clean code" is not an instruction — it describes what Copilot already tries to do. "Always use `async/await` instead of `.then()` chains" changes default behavior and is testable.

---

**Q15:** Where do prompt files live and what extension do they use?

**A15:** `.github/prompts/*.prompt.md` — in the prompts subdirectory of `.github/`. Extension must be `.prompt.md` (not `.md`). Copilot surfaces them as `/filename` slash commands in Chat. Files in the wrong location or with the wrong extension will not appear.

---

**Q16:** What are the template variables available in prompt files?

**A16:** `${selection}` — the currently selected text; `${file}` — path of the active file; `${input:variableName}` — prompts the user to enter a value when the prompt runs. These inject dynamic context into reusable prompt templates.

---

**Q17:** What is the difference between Edits mode and Agent Mode?

**A17:** Edits mode: you define the working set of files explicitly; Copilot makes targeted edits and shows a diff for each file; you review and accept/reject each change manually. Agent Mode: Copilot autonomously decides which files to read and edit, runs commands, creates files, and works until the task is complete — you review at the end.

---

**Q18:** What is the "working set" in Edits mode?

**A18:** The explicit set of files you add to the Edits session. Copilot can only read and modify files in the working set. If a file you didn't add needs to change, Copilot will tell you but cannot edit it. Keeping the working set small improves focus and reduces unwanted changes.

---

**Q19:** Name 5 "red flags" to look for when reviewing a Copilot Edits diff.

**A19:** (1) Deleted error handling or try/catch blocks. (2) New imports from libraries not in your dependencies. (3) Hardcoded values where your code had config/env variables. (4) Changed public API signatures (renamed parameters, added required arguments). (5) Removed or weakened security checks (auth bypass, removed input validation).

---

**Q20:** What must you always do before running Agent Mode? Why?

**A20:** `git add . && git commit -m "checkpoint: before agent mode — [task]"`. Because Agent Mode can create, modify, and delete files across the project. If the result is wrong, `git checkout .` recovers everything to the checkpoint. Without a checkpoint, recovery is manual and error-prone.

---

**Q21:** Write the 4 parts of the Agent Mode task template.

**A21:** (1) **Context** — what codebase/files/constraints exist; (2) **Goal** — the specific outcome required; (3) **Constraints** — what must NOT change (public APIs, tests, config format); (4) **Plan first** — "Plan only, no code yet. List: files to create/modify, approach for each, assumptions, clarifying questions."

---

**Q22:** When should you NOT use Agent Mode? Name 5 scenarios.

**A22:** (1) Simple one-file changes (use Edits or inline Chat). (2) Explaining or understanding code (use Ask mode). (3) When you need to review changes file by file carefully. (4) Debugging a single failing test. (5) Any task where the scope is clear and contained to 1-2 files — Agent Mode overhead isn't worth it.

---

**Q23:** What is the correct order for test-first workflow with Copilot?

**A23:** (1) Write the test (or ask Copilot to generate failing tests for the spec). (2) Verify the test fails (confirms it tests real behavior). (3) Ask Copilot to implement the code that makes the test pass. (4) Verify all tests pass. (5) Ask Copilot for refactoring suggestions (tests protect against regression).

---

**Q24:** Name 3 things you should always mock in unit tests.

**A24:** (1) External API/HTTP calls (network → flaky, slow, non-deterministic). (2) Database sessions or connection pools (tests should not need a real DB). (3) Time (`datetime.now()`, `Date.now()`) — tests should be deterministic regardless of when they run.

---

**Q25:** What are the 2 pieces of the "PR quality gate" — the CI check and the Copilot prompt?

**A25:** CI check: test suite must pass (all existing tests green, new tests for new code). Copilot prompt: run `/security-review` on changed files and `/generate-tests` on new code before opening the PR. Both must pass before the PR is opened.

---

## Advanced Engineering (Sheets 14-20)

**Q26:** What are the required sections in a custom `.agent.md` file?

**A26:** `description` (what the agent does — appears in the picker), `tools` (list of tools the agent can use), and the agent's behavioral instructions (what it should and should not do, its scope, output format expectations). Optionally: `model` to pin a specific model.

---

**Q27:** What is the difference between `AGENTS.md` and `copilot-instructions.md`?

**A27:** `copilot-instructions.md` is VS Code Copilot specific — auto-loaded by Copilot in VS Code only. `AGENTS.md` is designed to be portable across multiple AI coding tools (Claude Code, Codex, Devin, etc.) — it describes the project, conventions, and agent constraints in a tool-agnostic format. For multi-AI-tool projects, use AGENTS.md. For VS Code-only: copilot-instructions.md.

---

**Q28:** How does folder-level `AGENTS.md` relate to root-level?

**A28:** They are additive. The root `AGENTS.md` sets global project context. Folder-level `AGENTS.md` (e.g., `backend/AGENTS.md`) adds domain-specific rules for that subdirectory. When Copilot works in `backend/`, it reads both files and merges the instructions. Folder-level overrides root where they conflict.

---

**Q29:** What makes an AGENTS.md file "tool-agnostic"?

**A29:** It describes project conventions in plain language without VS Code-specific syntax (no `#file:` references, no prompt file syntax, no `.agent.md` references). It focuses on: what the project does, stack/frameworks, coding conventions, testing requirements, and what agents should not do — facts any AI tool can understand.

---

**Q30:** What is the "minimum viable context rule"?

**A30:** Give Copilot the minimum context needed to answer the question correctly — no more. Opening every file "just in case" bloats the context window with irrelevant content and degrades answer quality. Identify the 1-3 files actually relevant to the task, attach only those, and ask a specific question.

---

**Q31:** Name 5 signs that your context window has been exceeded.

**A31:** (1) Copilot references code from earlier in the conversation that is no longer visible. (2) Answers become increasingly generic or contradictory. (3) Copilot re-introduces code you explicitly asked it to remove. (4) Response quality drops compared to earlier in the same conversation. (5) Copilot says "Based on our previous discussion..." but the detail it references is wrong.

---

**Q32:** What is a "project context file" and when should you update it?

**A32:** A `CONTEXT.md` or `.copilot/context.md` file that summarizes: what the project does, current state of implementation, recent decisions, next steps, and key conventions. Update it: at the start of each major feature, after important architectural decisions, and at the end of a session when the next session will continue the same work.

---

**Q33:** Name the 4 token waste patterns (anti-patterns).

**A33:** (1) Attaching entire large files when only one function is relevant (use `#selection`). (2) Using `#codebase` for a question that needs one specific file. (3) Continuing a long conversation that has drifted off-topic (start a new chat). (4) Asking Copilot to "rewrite the whole thing" when you only need targeted changes (ask for a diff/patch instead).

---

**Q34:** What model should you use for: simple questions, standard code gen, complex reasoning, DSA?

**A34:** Simple questions (explain this error, what does X mean) → fastest/cheapest model (GPT-4o mini, Claude Haiku). Standard code gen → balanced model (GPT-4o, Claude Sonnet). Complex reasoning / architecture / trade-off analysis → most powerful model (o3, Claude Opus). DSA / algorithm correctness → o-series reasoning model.

---

**Q35:** What is MCP? What is the difference between MCP client and MCP server?

**A35:** Model Context Protocol — a standard for AI tools to connect to external data sources and tools. MCP client: the AI tool (VS Code Copilot) that sends requests. MCP server: a process that exposes specific tools/resources to the client (e.g., a Postgres MCP server exposes DB schema and query tools). VS Code runs the MCP client; you configure MCP servers in `.vscode/mcp.json`.

---

**Q36:** Where must `.vscode/mcp.json` be located?

**A36:** In the `.vscode/` folder at the project root — specifically `.vscode/mcp.json`. This is the VS Code Copilot MCP configuration file. It must be gitignored because it may contain real server URLs and environment variable references that are resolved at runtime.

---

**Q37:** Why must `.vscode/mcp.json` be gitignored? What should be committed instead?

**A37:** The real `mcp.json` may contain server URLs, environment variable names, or local paths that differ per machine. Commit a `mcp.example.json` with placeholders (no real values) that teammates can copy and customize. The real file goes in `.gitignore`.

---

**Q38:** What are the 5 security rules for MCP configuration?

**A38:** (1) Never put real credentials in mcp.json — use environment variable references. (2) Always gitignore mcp.json — commit only the example template. (3) Use least-privilege tool scope — only expose the tools the agent needs. (4) Review what each MCP server can access before enabling it. (5) Treat MCP tool calls like any other external dependency — they can fail, be slow, or be compromised.

---

**Q39:** Name 3 MCP servers and their use cases.

**A39:** (1) `@modelcontextprotocol/server-filesystem` — exposes local files as tools (useful for reading files outside the workspace). (2) `@modelcontextprotocol/server-postgres` — exposes PostgreSQL schema and query execution. (3) Custom HTTP MCP server — wraps internal APIs (Jira, Confluence, internal docs) as MCP tools for Copilot to call.

---

**Q40:** What is an ADR and why does it matter?

**A40:** Architecture Decision Record — a short document capturing: what decision was made, what alternatives were considered, and why this option was chosen. ADRs matter because they prevent re-litigating past decisions and help new team members understand "why is the code this way." Copilot can generate ADR templates from a conversation about a design decision.

---

## Pro / Production Level (Sheets 21-27)

**Q41:** What are the 5 components of a Personal Copilot Operating System?

**A41:** (1) Personal `copilot-instructions.md` with curated conventions. (2) A prompt library of reusable slash commands (10+). (3) Custom agents for recurring specialist tasks. (4) Daily workflow rituals (morning planning, end-of-day notes). (5) A project context file updated per session.

---

**Q42:** Describe the 3 daily rituals: morning, during coding, end-of-day.

**A42:** Morning: use `/daily-planner` to review today's task, identify relevant files, set the first concrete subtask. During coding: use Git checkpoints before every Agent Mode run; use `#selection` + specific prompts; run security review before committing. End-of-day: use `/generate-learning-notes` to capture what you built, what prompts worked, one Copilot limitation hit.

---

**Q43:** What 12 SDLC phases map to Copilot workflows?

**A43:** Requirements analysis → Feature planning → Architecture review → Codebase exploration → Implementation → Test generation → Code review → PR description → Documentation → CI/CD pipeline → Release notes → Incident/debugging. Copilot has specific prompt patterns for each phase.

---

**Q44:** Name the failure mode: "Copilot uses session.query() instead of select() statement".

**A44:** Stale Knowledge / Training Cutoff — Copilot's training data contains the old SQLAlchemy 1.x API (`session.query()`). The correct modern API is `select()` with `session.execute()`. Fix: include a context file or instruction noting "use SQLAlchemy 2.0 style: `select()` with `session.scalars()`". Explicitly correct Copilot: "That's the old API. The correct method is `select()`."

---

**Q45:** What is the 6-step diagnostic for "MCP tool not appearing"?

**A45:** (1) Confirm `.vscode/mcp.json` exists and is valid JSON. (2) Check the MCP server process is running (check terminal). (3) Reload VS Code window (`Cmd+Shift+P` → Reload Window). (4) Check VS Code Copilot output panel for MCP error messages. (5) Verify environment variables referenced in mcp.json are actually set. (6) Test the MCP server independently (call it directly outside Copilot).

---

**Q46:** What causes "Copilot ignores copilot-instructions.md"? Name 4 causes.

**A46:** (1) File is not at `.github/copilot-instructions.md` (wrong path). (2) File is too long — instructions buried at line 300+ in a 400-line file are not reliably followed. (3) Instruction conflicts with a stronger model default behavior. (4) The instruction is too vague to be testable ("write good code") — Copilot can't determine what "following" the instruction means.

---

**Q47:** How do you detect a hallucinated API method before running the code?

**A47:** (1) Check the official docs or installed package for the exact method signature. (2) Search the codebase — if the method doesn't exist in any import or definition, it's suspect. (3) Run the code in isolation (not in production). (4) Tell Copilot: "Verify that `[method]` exists in `[library]` version `[version]`." (5) Use your IDE's auto-complete — if the method doesn't appear in IntelliSense, it doesn't exist.

---

**Q48:** Name all 12 non-negotiable responsible AI rules.

**A48:** (1) Never paste credentials. (2) Never paste PII or real customer data. (3) Always review every diff before accepting. (4) Run tests after every Copilot change. (5) Git checkpoint before Agent Mode. (6) Verify new imports are real and not malicious. (7) Validate generated shell commands before running. (8) Classify data: GREEN/YELLOW/RED before sharing. (9) Never commit code you can't explain. (10) Don't use Copilot output as authoritative on security, legal, or compliance topics. (11) Flag AI-generated code in code reviews. (12) Pre-commit secret scanning in every project.

---

**Q49:** What is the GREEN / YELLOW / RED data classification scheme?

**A49:** GREEN — safe to share with Copilot: synthetic/dummy data, public documentation, code with no credentials or PII. YELLOW — share with caution: internal business logic, architecture diagrams, aggregated analytics (no individual identifiers). RED — never share: credentials/API keys, PII (names, emails, SSNs), real production database contents, regulated data (HIPAA, PCI-DSS).

---

**Q50:** What are the 4 categories in the AI output evaluation standard?

**A50:** (1) Correctness — does it do what was asked? Does it handle edge cases? (2) Security — does it introduce any OWASP Top 10 issues, hardcoded credentials, or input validation gaps? (3) Completeness — are there missing error handlers, untested paths, or ignored requirements? (4) Style/Maintainability — does it match the existing code patterns, naming, and conventions — or introduce foreign style?


---

## Foundations (Sheets 1-6)

**Q1:** What are the 6 Copilot surfaces? Name each and its best use case.

**Q2:** What context does Copilot see during inline suggestions (autocomplete)?

**Q3:** What context does Copilot NOT automatically see during Chat?

**Q4:** What is the "first draft" mental model? Why is it important?

**Q5:** Name the 5 context variables available in Chat. What does each attach?

**Q6:** When should you start a NEW Chat conversation vs continue the same one?

**Q7:** What does `#codebase` do differently from `#file`?

**Q8:** Name the 6 built-in Chat slash commands (/, /explain, /fix, /tests, /doc, /new, /simplify).

**Q9:** What are the 3 pieces of information you should never paste into Copilot?

**Q10:** What is the recovery command after a bad Agent Mode session?

---

## Intermediate Power User (Sheets 7-13)

**Q11:** Where must `copilot-instructions.md` be located?

**Q12:** What does the `applyTo` field in path-specific instruction frontmatter do?

**Q13:** What is the ideal length of a `copilot-instructions.md` file and why?

**Q14:** What is the "Principle 2" of instruction design (must change default behavior)?

**Q15:** Where do prompt files live and what extension do they use?

**Q16:** What are the template variables available in prompt files?

**Q17:** What is the difference between Edits mode and Agent Mode?

**Q18:** What is the "working set" in Edits mode?

**Q19:** Name 5 "red flags" to look for when reviewing a Copilot Edits diff.

**Q20:** What must you always do before running Agent Mode? Why?

**Q21:** Write the 4 parts of the Agent Mode task template.

**Q22:** When should you NOT use Agent Mode? Name 5 scenarios.

**Q23:** What is the correct order for test-first workflow with Copilot?

**Q24:** Name 3 things you should always mock in unit tests.

**Q25:** What are the 2 pieces of the "PR quality gate" — the CI check and the Copilot prompt?

---

## Advanced Engineering (Sheets 14-20)

**Q26:** What are the required sections in a custom `.agent.md` file?

**Q27:** What is the difference between `AGENTS.md` and `copilot-instructions.md`?

**Q28:** How does folder-level `AGENTS.md` relate to root-level?

**Q29:** What makes an AGENTS.md file "tool-agnostic"?

**Q30:** What is the "minimum viable context rule"?

**Q31:** Name 5 signs that your context window has been exceeded.

**Q32:** What is a "project context file" and when should you update it?

**Q33:** Name the 4 token waste patterns (anti-patterns).

**Q34:** What model should you use for: simple questions, standard code gen, complex reasoning, DSA?

**Q35:** What is MCP? What is the difference between MCP client and MCP server?

**Q36:** Where must `.vscode/mcp.json` be located?

**Q37:** Why must `.vscode/mcp.json` be gitignored? What should be committed instead?

**Q38:** What are the 5 security rules for MCP configuration?

**Q39:** Name 3 MCP servers and their use cases.

**Q40:** What is an ADR and why does it matter?

---

## Pro / Production Level (Sheets 21-27)

**Q41:** What are the 5 components of a Personal Copilot Operating System?

**Q42:** Describe the 3 daily rituals: morning, during coding, end-of-day.

**Q43:** What 12 SDLC phases map to Copilot workflows?

**Q44:** Name the failure mode: "Copilot uses session.query() instead of select() statement".

**Q45:** What is the 6-step diagnostic for "MCP tool not appearing"?

**Q46:** What causes "Copilot ignores copilot-instructions.md"? Name 4 causes.

**Q47:** How do you detect a hallucinated API method before running the code?

**Q48:** Name all 12 non-negotiable responsible AI rules.

**Q49:** What is the GREEN / YELLOW / RED data classification scheme?

**Q50:** What are the 4 categories in the AI output evaluation standard?
