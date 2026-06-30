# Codex MAANG Interview Prep — Gold Sheet

> **Track**: Codex Mastery Track — Group 6: Practice & Upgrade
> **File**: 6 of 6 (Track File #35)
> **Audience**: Developers preparing for L3–L7 engineering interviews where AI tooling proficiency is evaluated
> **Read after**: All other track files

---

## The 60-Second Codex Pitch (Memorize This)

```
"I use OpenAI Codex CLI as an autonomous coding agent — not just autocomplete.
 It reads my codebase context from AGENTS.md, can run tests and commands,
 and operates in three modes depending on the risk level.
 
 In practice: I use it for test-first TDD loops, security reviews before PRs,
 and full autonomous feature builds on well-specified tasks.
 
 The key discipline: verification-driven workflows. Codex's output is only done
 when the tests I specified actually pass — not when Codex 'thinks' it's done.
 
 The result: I spend my time on architecture, product decisions, and review —
 the high-judgment work — instead of boilerplate implementation."
```

Practice until you can deliver this in under 60 seconds without notes.

---

## L3 — New Grad Level Questions

**Q: "What AI tools do you use in development?"**
> "I use Codex CLI for autonomous coding tasks — implementing features, generating tests, running security reviews. The key principle is verification-driven: every task has a test command that proves it worked. I use GitHub Copilot for in-editor autocomplete."

**Q: "How do you make sure Codex-generated code is correct?"**
> "Verification command in every prompt — Codex must pass the tests before the task is considered done. Then I run git diff and read every change. Tests passing doesn't replace code review."

**Q: "What would you never put in a Codex prompt?"**
> "Secrets or API keys — never. Real PII — use synthetic data. Security bypass instructions."

---

## L4 — Mid-Level Questions

**Q: "Describe your AI-assisted development workflow."**
> "Morning planning with Codex — ordered task list with file scope and verification per task. Implementation with auto-edit mode — Codex applies file edits, I approve commands. Pre-commit security review. End-of-day: update AGENTS.md with any new rules Codex violated that would be preventable. The workflow compounds: AGENTS.md gets better over time, Codex makes fewer mistakes per session."

**Q: "How do you use Codex for code reviews?"**
> "Pre-commit: security review of staged diff with a structured prompt — checks SQL injection, auth bypass, PII exposure, input validation. Pre-PR: full review with gpt-4.1 covering security, correctness, tests, architecture. Format: severity table with file:line and fix. I don't merge until APPROVED."

**Q: "How do you prevent Codex from modifying test files?"**
> "Explicit constraint in every prompt: 'Do not modify test files.' And in AGENTS.md as a forbidden action. If Codex modifies tests to make them pass — that's a red flag that means the implementation is wrong, not the test."

---

## L5 — Senior Level Questions

**Q: "How do you use Codex for large feature development?"**
> "Multi-phase pipeline: Planner reads the codebase and produces an implementation plan + handoff document. Builder gets the handoff doc as fresh context — implements with a bounded, verified task. Tester runs gap analysis on coverage. Reviewer does security + architecture review. Each phase is a separate Codex session for context isolation. The handoff document is the key — it carries the design intent from planner to builder."

**Q: "How do you handle autonomous Codex sessions gone wrong?"**
> "Git checkpoint before every full-auto session — that's non-negotiable. If something goes wrong mid-session: Ctrl+C. If it completed but output is wrong: git reset --hard HEAD~1 to the checkpoint. Partial rollback for specific files: git checkout -- [file]. The checkpoint discipline means 'wrong' is always recoverable."

**Q: "How do you prevent context drift in long Codex sessions?"**
> "Two mechanisms: /compact between tasks to free context headroom, and AGENTS.md as the persistent anchor. Context drift symptoms: Codex starts using wrong error types, wrong naming, or ignoring constraints from AGENTS.md. Detection: compare output against AGENTS.md. Fix: /compact and restate the constraint explicitly, or fresh session."

---

## L6 — Staff Level Questions

**Q: "How would you build a team Codex practice at scale?"**
> "Three layers: (1) Shared AGENTS.md in every repo — the team's conventions enforced at the Codex level. New team members contribute to it in their first week. (2) Shared prompt script library — the team's accumulated best prompts for common workflows, version-controlled. (3) Workflow integration in CI — Codex security gate on PRs using --quiet mode. The compound effect: Codex gets better as the team's AGENTS.md matures."

**Q: "What are the limits of autonomous Codex workflows and how do you manage them?"**
> "Three categories of limits: (1) Product decisions — Codex doesn't know your users or business goals. Specification is human work. (2) Architecture decisions — Codex optimizes locally but doesn't understand long-term system evolution. Architecture review is human work. (3) Novel debugging — when the codebase has a bug in an unexpected layer (infrastructure, environment, subtle concurrency) Codex loops. Stopping conditions in prompts prevent infinite iteration. The management principle: Codex does implementation well; humans do judgment well."

---

## L7 — Principal/Distinguished Level Questions

**Q: "How do you think about the long-term trajectory of AI coding agents in engineering orgs?"**
> "The leverage point shifts from implementation to specification and evaluation. The engineer who writes the clearest implementation spec — and can evaluate whether the output meets it — creates more leverage than the engineer who types the fastest. AGENTS.md quality becomes a team asset. The skill that compounds: prompt engineering, verification discipline, and architectural judgment. The risk: teams that use AI for implementation without improving specification rigor will produce faster-generated incorrect code."

**Q: "How do you measure the impact of AI tooling on engineering productivity?"**
> "Lead time metrics: time from ticket to merged PR. Not lines of code — that's noise. Defect escape rate: are AI-assisted PRs producing more bugs in production? Review time: is code review faster or slower (it should be faster with AI-generated PR descriptions and pre-review). The anti-metric: AI tool adoption rate — adoption without quality impact is just cargo culting."

---

## Common Interview Traps

```
Trap: "Isn't AI just autocomplete?"
→ Never agree. Codex CLI is an autonomous agent with plan-execute-verify loops, file editing,
  command execution, and session memory via AGENTS.md. Fundamentally different from autocomplete.

Trap: "How do you know the AI-generated code is secure?"
→ Don't say "it checks for security." Say: "I run explicit security review prompts with OWASP
  categories. The review is as rigorous as human review — it checks SQL injection, auth bypass,
  PII exposure. The constraint is that I am responsible for the output — Codex doesn't replace judgment."

Trap: "What if the AI makes a mistake?"
→ "That's why verification discipline exists. Tests must pass. Git diff must be reviewed. Tests
  passing doesn't mean I skip review. Codex-generated code gets the same review rigor as
  human-written code — the difference is the AI can also help with the review itself."

Trap: "Have you caught Codex making a significant mistake?"
→ Have a real example ready. Key elements: what Codex did wrong, how you detected it (tests?
  diff review? security check?), how you recovered (git reset? partial revert?), what AGENTS.md
  rule you added to prevent it next time.
```

---

## Pre-Interview Self-Check

```
[ ] 60-second pitch: delivered in under 60 seconds without notes
[ ] Can explain the difference between Codex CLI and GitHub Copilot
[ ] Can describe full-auto workflow with all safety mechanisms
[ ] Can describe multi-agent pipeline (4 phases)
[ ] Have a real "mistake Codex made and how I caught it" story
[ ] Can articulate what autonomous workflows enable (the cognitive shift)
[ ] No interview answer says "the AI handles it" without describing the human judgment layer
```
