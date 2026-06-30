# System Prompt Engineering — Gold Sheet

> **Track**: Codex Mastery Track — Group 3: Advanced Engineering
> **File**: 2 of 7 (Track File #15)
> **Audience**: Developers who want to control Codex's persona and output style globally
> **Read after**: Full-Auto-Mode-Gold-Sheet.md

---

## Practical Impact Meter

| Topic | Impact | Why Developers Miss This |
|---|---|---|
| AGENTS.md vs --system-prompt — when to use each | ★★★★★ | Using AGENTS.md for everything misses the --system-prompt flag for session-specific roles |
| Role-based system prompts (security, reviewer, writer) | ★★★★☆ | Same task with different role = different output quality and focus |
| System prompt vs task prompt separation | ★★★★☆ | Mixing role setup and task in one prompt produces weaker output for both |
| Output format control via system prompt | ★★★☆☆ | System-level format instructions apply to every response without repeating in each prompt |
| Constraint architecture — what goes where | ★★★☆☆ | Knowing which constraints belong in AGENTS.md vs system-prompt vs task prompt |

---

## ⭐ Beginner Tier — Start Here

### B1: Your first role-based session

```bash
# Default: no system prompt, Codex uses its own judgment
codex "review src/auth/service.py for security issues"
# Output: generic review, may miss OWASP specifics, no severity grading

# With security reviewer role:
codex --system-prompt \
  "You are a security engineer. For every finding: SEVERITY, ATTACK VECTOR, FIX, OWASP." \
  "review src/auth/service.py for security issues"
# Output: severity-graded table, OWASP categories, specific fixes
```

Run both versions on the same file. Compare the output depth and structure.
This is the difference a role makes.

### B2: Identify which level each instruction belongs to

```
For each of these rules, decide: AGENTS.md? --system-prompt? Task prompt?

1. "Use ValueError for business validation errors" → [answer: AGENTS.md — always]
2. "Output findings as: | SEVERITY | ISSUE | FIX |" → [answer: --system-prompt or task]
3. "Review src/payments/service.py" → [answer: task prompt — one-time]
4. "Do not run database migrations" → [answer: AGENTS.md — always]
5. "You are a technical writer" → [answer: --system-prompt — this session]
```

If you can answer all 5 correctly: you understand the constraint architecture.

---

## 1. AGENTS.md vs --system-prompt vs Task Prompt

```
Three levels of instruction — each has a purpose:

AGENTS.md (persistent project memory):
  - Project context, architecture, coding standards, forbidden actions
  - Read at start of every session automatically
  - Applies to: all tasks in this project, always
  - Change frequency: weekly or when project conventions change

--system-prompt (session-level role):
  - Sets Codex's persona and focus for this specific session
  - Override for specialized tasks: security reviewer, technical writer, architect
  - Applies to: all tasks in this session
  - Change frequency: per session when role differs from default

Task prompt (what to do):
  - The specific task, scope, constraints, and verification for this one task
  - Applies to: this task only
  - Change frequency: per task
```

### When to use --system-prompt

```bash
# Use AGENTS.md alone when: normal development session
codex "add input validation to create_user()"

# Use --system-prompt when: specialized role that improves output quality
codex --system-prompt "You are an expert application security engineer..." \
  "review src/auth/ for vulnerabilities"

# The difference in output:
# Without role: standard review with general security knowledge
# With security role: OWASP-focused, severity-graded, attack vector explained
```

---

## 2. Built-In Role Templates

### Security Reviewer

```bash
codex --system-prompt \
  "You are a senior application security engineer specializing in OWASP Top 10.
   
   For every review:
   - Check for SQL injection, broken auth, sensitive data exposure, security misconfigs,
     XSS, IDOR, insecure deserialization, missing rate limiting, vulnerable dependencies
   - Every finding must include: SEVERITY (CRITICAL/HIGH/MEDIUM/LOW), ATTACK VECTOR,
     SPECIFIC FIX with code example, OWASP category
   - Never approve code with CRITICAL or HIGH findings without explicit notation
   - Be conservative: flag anything that could be exploited, even if unlikely
   
   Output format: severity table, then APPROVED / CHANGES REQUIRED." \
  "review src/auth/ for security vulnerabilities"
```

### Code Reviewer

```bash
codex --system-prompt \
  "You are a senior engineer conducting a thorough code review.
   
   For every review:
   - Check: correctness, performance, security, maintainability, test coverage
   - Cite specific line numbers for every finding
   - Suggest concrete fixes, not abstract improvements
   - Rate overall: APPROVED / APPROVE WITH COMMENTS / CHANGES REQUIRED
   - If CHANGES REQUIRED: list the minimum changes needed for approval
   
   Style:
   - Be direct and specific (not vague)
   - Focus on meaningful issues (not style nitpicks that formatters handle)
   - Acknowledge what is done well (not only what is wrong)
   
   Output: table of findings, then overall verdict." \
  "review the changes in src/orders/"
```

### Technical Writer

```bash
codex --system-prompt \
  "You are a technical writer specializing in developer documentation.
   
   For every documentation task:
   - Explain WHY, not just WHAT (the reader can read the code for WHAT)
   - Use concrete examples — one minimal code example per function
   - Write for developers who are new to this codebase, not for the author
   - Never invent behavior — only document what is verifiably in the code
   - Flag anything you're uncertain about as: [verify with team]
   
   Format: Google-style docstrings for Python, JSDoc for TypeScript.
   Tone: clear, professional, concise. No marketing language." \
  "write documentation for src/payments/service.py"
```

### Architecture Advisor

```bash
codex --model gpt-4.1 --system-prompt \
  "You are a principal engineer specializing in distributed systems architecture.
   
   For every architecture task:
   - Consider: scalability, fault tolerance, maintainability, operational complexity
   - Present trade-offs explicitly — do not recommend without acknowledging costs
   - Prefer boring, proven technology over cutting-edge when reliability matters
   - When recommending a pattern: cite where it has worked at scale
   - Output: recommendation + trade-offs + risks + migration path
   
   Constraints for this project:
   - No new infrastructure without explicit human approval
   - Must work with existing PostgreSQL + FastAPI stack
   - Team has 5 engineers, avoid operationally complex solutions" \
  "design the caching strategy for the user service"
```

---

## 3. Constraint Architecture — What Goes Where

```
System prompt:
  ✅ Role and expertise level
  ✅ Output format rules that apply to all tasks this session
  ✅ Review criteria (what to check, how to rate)
  ✅ Tone and communication style

AGENTS.md:
  ✅ Project-specific conventions (naming, error handling, test patterns)
  ✅ Architecture rules (layering, dependencies, patterns)
  ✅ Forbidden actions (migrations, git push, PII logging)
  ✅ Tech stack versions
  ✅ Verification commands

Task prompt:
  ✅ Specific task (what to do this time)
  ✅ File scope (which files to touch)
  ✅ Task-specific constraints (not in AGENTS.md, just for this task)
  ✅ Verification command override (if different from AGENTS.md default)
```

---

## 4. Combining System Prompt with AGENTS.md

```bash
# When both are set, both apply:
# AGENTS.md: project conventions (always)
# --system-prompt: session role (this session only)
# Task prompt: specific task

# Example: security review of a file using project conventions
codex --system-prompt \
  "You are a security engineer. Check every finding against AGENTS.md conventions." \
  "Security review of src/payments/service.py.
   Focus on: input validation for payment amounts, SQL parameterization,
   auth check on the admin payment override endpoint."

# AGENTS.md ensures: Codex knows the coding standards, forbidden SQL patterns, etc.
# System prompt ensures: Codex focuses on security analysis, not implementation
# Task prompt: specific file and focus areas
```

---

## 5. Testing Your System Prompts

```bash
# Test 1: Does the role change the output format?
# Run same task with and without the security role → compare security detail level

# Test 2: Does the system prompt conflict with AGENTS.md?
# If AGENTS.md says "use ValueError" and system prompt says "use HTTPException for all errors"
# → They conflict → clarify which takes precedence

# Test 3: Does the output stay within the role?
# Security reviewer should NOT start implementing fixes without being asked
# If it does: add constraint to system prompt: "review only — do not implement"

# Test 4: Does the format spec apply consistently?
# Every response in the session should use the same output format
# If it drifts: /compact and restate the format expectation
```

---

## Interview Traps

```
TRAP: "--system-prompt replaces AGENTS.md — I only need one or the other"
TRUTH: They serve different purposes. AGENTS.md = permanent project memory (standards,
       forbidden actions, architecture). --system-prompt = session-level role (security
       reviewer, tech writer). Both can be active simultaneously. Neither replaces the other.

TRAP: "The security reviewer role prevents Codex from generating insecure code"
TRUTH: The security reviewer role improves the quality of security REVIEW output.
       It makes Codex a better reviewer when asked to review. It doesn't prevent Codex
       from generating insecure code in implementation prompts that don't use the role.

TRAP: "A longer, more detailed system prompt is always more effective"
TRUTH: System prompts have diminishing returns past ~200 words. The most effective ones
       are focused: one role, one output format, key constraints. A 1000-word system
       prompt dilutes the signal that most shapes output behavior.
```

---

## Revision Checklist

- [ ] Can explain the difference between AGENTS.md, --system-prompt, and task prompt
- [ ] Can write a security reviewer, code reviewer, and technical writer system prompt
- [ ] Know which constraints go in system prompt vs AGENTS.md vs task prompt
- [ ] Can combine system prompt + AGENTS.md for specialized sessions
- [ ] Can test whether a system prompt is actually changing output quality
