# Claude MAANG Interview Prep — Gold Sheet

> **Track**: Claude Mastery Track — Group 6: Practice Upgrade
> **File**: 5 of 5 (Track File #34 — replaces 4-Week Roadmap as final file; roadmap is now #30)
> **Audience**: Developers preparing for L4–L7 interviews at top-tier companies
> **Purpose**: How to talk about AI-assisted development in interviews

---

## Why This Matters in 2025

```
At L5+ (and increasingly L4) at MAANG-tier companies, interviewers ask:
  - "How do you use AI coding tools in your workflow?"
  - "What are the risks of autonomous AI coding agents?"
  - "How do you ensure code quality when Claude writes the code?"
  - "How would you introduce Claude Code to a team of engineers?"

They want to see:
  1. Deliberate use — not "I just use it for autocomplete"
  2. Failure mode awareness — you know what can go wrong
  3. Output review discipline — you have standards before committing
  4. System thinking (L6+) — you think about this at team/org scale
```

---

## Level Guide

| Level | Focus | Signal |
|-------|-------|--------|
| L3/L4 | "How do you use it daily?" | Has a workflow, reviews output, knows basic failure modes |
| L5 | "How do you ensure quality?" | Systematic approach, test-first, prompt craftsmanship |
| L6 | "How do you scale this?" | Team CLAUDE.md, governance, onboarding plan |
| L7 | "Org-scale strategy?" | Policy framework, ROI metrics, risk management |

---

## Section 1 — L3/L4 Questions

**Q: Walk me through how you use Claude in your day-to-day development work.**

```
Strong answer:
"I follow a structured daily ritual. In the morning, I use /plan to break down my ticket
into implementation steps — it takes 5 minutes and prevents false starts.

During implementation, I use the CLI in agent mode for multi-file scaffold tasks,
but only after committing a checkpoint and writing an explicit scope. For targeted
changes I use focused prompts with @file references instead of @codebase.

Before every commit, I run /security on auth-touching code and /review on all changes.

My single non-negotiable: I never commit code I can't explain line by line,
regardless of whether Claude generated it."

Why it works: shows a system, not random usage. Shows review discipline.
```

---

**Q: What have you found Claude to be bad at? Where does it fail?**

```
Strong answer:
"Several specific failure modes I've observed:

1. Context drift: in long sessions, Claude contradicts earlier decisions.
   My fix: sessions are task-scoped; I summarize and restart when I notice drift.

2. Hallucinated APIs: Claude will confidently use methods that don't exist in the
   library version you're running. Fix: run tests immediately after generation.

3. Over-engineering: vague prompts produce factory patterns and registries for
   problems that need a 3-line function. Fix: explicit 'Do NOT add abstractions'
   constraints.

4. Security-sensitive code: technically working but insecure SQL or auth patterns.
   Fix: dedicated security review prompt on all auth/SQL code before PR.

5. Test quality: generated tests cover happy path only and are sometimes tautological.
   Fix: gap analysis prompt after every generated test suite."
```

---

**Q: Have you had Claude produce incorrect or insecure code? How did you handle it?**

```
Strong answer:
"Yes. Most instructive: Claude generated a SQLAlchemy query using string formatting
instead of parameterized queries. The code was functionally correct for clean input
— it would have shipped if I hadn't run my /security prompt on the database layer.

It found: text(f'SELECT * FROM users WHERE email = {email}') — SQL injection.
The fix was parameterized queries, which I had it apply immediately.

The lesson I took from it: testing doesn't catch security vulnerabilities.
A test that calls create_user('test@example.com') will pass even with SQL injection.
Security requires a dedicated review pass, not just a green test suite."
```

---

## Section 2 — L5 Engineering Depth Questions

**Q: How do you ensure code quality when Claude is generating significant portions of your code?**

```
Strong answer:
"I approach it in layers:

Layer 1 — Configuration: CLAUDE.md at project root encodes all team conventions —
naming, error handling, security rules, testing requirements. Claude generates code
that already follows our standards before I review it.

Layer 2 — Test-first: I never ask Claude to generate code and then tests.
I define the tests (what correct behavior looks like) before implementation.
This makes tests independent truth-sources instead of implementation mirrors.

Layer 3 — Verification loops: agent sessions run lint + tests after every component.
'Done' means: tests pass + lint clean + no regressions. Not 'it compiles.'

Layer 4 — Structured pre-commit review: security scan + test gap analysis on every PR.
If I can't describe what each changed line does, I don't commit.

Layer 5 — Never commit what I can't explain: this is the only rule that doesn't fail."
```

---

**Q: Describe how you design prompts to get consistently good output.**

```
Strong answer:
"A few principles:

Minimum viable context: I use @file for known targets, not @codebase for everything.
Diluting signal with irrelevant context degrades output quality.

Constraint-first for sensitive code: 'Use parameterized queries, never string
interpolation, raise HTTPException not raw Python exceptions' — before the goal.

Reference existing patterns: 'Follow the pattern in @file:src/services/user_service.py'
outperforms describing the pattern in words every time.

Separate concerns per session: plan in one session, implement in the next, test in the next.
A session that plans + implements + tests produces lower quality than three focused sessions.

State forbidden actions: 'Do NOT add new classes beyond what's in the plan' prevents
Claude from filling specification gaps with unnecessary abstractions."
```

---

**Q: Describe the difference between using Claude interactively vs autonomous agent mode. When do you use each?**

```
Strong answer:
"Interactive: I drive the process. I ask a question, review the answer, decide the next
step. Good for: exploration, understanding unfamiliar code, short focused changes.

Agent mode: I write a bounded task specification and Claude executes it — reading files,
making changes, running tests, iterating. Good for: scaffold tasks, multi-file refactors,
test generation at scale.

The key word in 'autonomous agent' is bounded, not autonomous. My agent prompts always
have: a defined scope (which files), explicit forbidden actions (what not to touch),
stopping conditions (when to ask vs proceed), and a verification command (what proves done).

I never merge from an agent session without reviewing the full diff. The agent runs
autonomously; I own the commit."
```

---

## Section 3 — L6 Team and System Design Questions

**Q: How would you introduce Claude Code to a team of 30 engineers?**

```
Strong answer:
"Three phases over 8 weeks:

Phase 1 — Foundation (weeks 1-2):
Create a team CLAUDE.md that encodes our existing coding standards. The first
experience everyone has should produce code that follows our conventions — not generic
output. Run a 2-hour workshop: the 6 Claude surfaces, the safety rules, and 3
hands-on exercises. Everyone leaves with the CLI working and one /debug interaction.

Phase 2 — Adoption (weeks 3-6):
Release 5 shared slash commands for our most common tasks (debug, test, review,
security, plan). Zero setup required — just copy to .claude/commands/. Identify
3-4 champions who explore agent mode and report back at the next team retro.

Phase 3 — Codify (weeks 7-8):
Run a retro: what worked, what produced wrong output, what got committed that shouldn't have.
Update the team CLAUDE.md. Establish: same review standard for all code regardless
of how it was generated. Add pre_tool_use.sh blocking dangerous commands."
```

---

**Q: How would you set up governance for Claude usage at an org level?**

```
Strong answer:
"Three dimensions: configuration, process, risk.

Configuration governance:
  - Shared team CLAUDE.md maintained as a versioned artifact
  - Approved MCP server list (nobody adds unapproved MCP tools without review)
  - Prohibited actions in CLAUDE.md: no real production data, no credentials in prompts

Process governance:
  - PR review standard is unchanged: all code reviewed by a human, period.
    'Generated by Claude' doesn't reduce the review standard.
  - Pre_tool_use.sh blocking dangerous commands for all agent sessions

Risk governance:
  - Security-sensitive code (auth, crypto, SQL) requires human security-aware review
  - New dependencies added by Claude require CVE check and license verification
  - Incident post-mortems tag whether AI-generated code was involved (track trends)

Metrics (quarterly review):
  - PR cycle time (delivery impact)
  - Bug escape rate in AI-assisted code vs baseline (quality impact)
  - Security findings in AI-generated code (risk impact)"
```

---

## Section 4 — L7 Strategy Questions

**Q: What's your view on how AI coding tools change what senior engineers need to know?**

```
Strong answer:
"AI tools raise the floor and change the differentiators — they don't lower the ceiling.

The floor rises: routine generation, test scaffolding, boilerplate, documentation —
these become near-instant for any engineer with strong AI workflow skills.

Senior engineer differentiators in this environment:
1. Problem definition: knowing WHAT to build and WHY. Claude can't determine whether
   the requirements are complete or whether the feature is worth building.
2. Architecture judgment: knowing which patterns scale and which create technical debt.
   Claude suggests patterns from training data — it doesn't know your system's constraints.
3. Output evaluation: knowing what good code looks like and evaluating AI output against
   that standard. This compounds deep technical knowledge.
4. Context engineering: knowing what context to give Claude for each task type.
   This is a learnable skill that improves with practice.

The risk: engineers who use AI tools without building deep expertise become dependent on
a system that produces plausible-wrong output for complex problems.
The durable edge: deep domain knowledge × strong AI workflow = compounding advantage."
```

---

## Section 5 — Anti-Patterns in Interviews

```
DO NOT SAY                                    WHY IT'S A RED FLAG
"I use it to write code faster"               No mention of review — signals you ship without checking
"It writes most of my code"                   Suggests you don't understand what you're shipping
"I don't really use it"                       At L5+ in 2025, not adapting is a signal
"It's usually right so I just accept it"      No review discipline — how bugs get shipped
"I haven't had problems with it"              You haven't been reviewing. Problems exist, you missed them.
"It's just chat"                              Misses CLI, agent mode, CLAUDE.md, subagents, hooks
"It hallucinates sometimes but it's fine"     No mitigation strategy — signals carelessness
```

---

## Section 6 — Your 60-Second Claude Pitch

Memorize a version of this:

```
"I treat Claude as a highly capable collaborator with specific failure modes I know
how to mitigate.

My workflow has three non-negotiables:
  1. Plan first: I always define the scope and constraints before Claude touches code
  2. Verification loops: every agent session runs tests + lint before reporting done
  3. Review everything: I read every diff, every new import, before committing

I've built a personal Claude OS — CLAUDE.md, slash commands, custom agents, and
hooks — that makes Claude output consistent across projects.

The one rule I live by: I never commit code I can't explain line by line.
Claude writes the first draft. I'm responsible for what ships."
```

---

## Revision Checklist

- [ ] Can deliver the 60-second Claude pitch without notes
- [ ] Can name 5 specific Claude failure modes with mitigations
- [ ] Can describe the pre-commit review workflow (what 3 things to run)
- [ ] Can outline an 8-week team onboarding plan
- [ ] Can describe org-level governance (3 dimensions)
- [ ] Can explain why deep technical knowledge still matters in the AI era
- [ ] Can answer all 8 MAANG questions without checking this sheet
