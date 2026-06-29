# Copilot 4-Week Mastery Roadmap

> **Track**: Copilot Mastery Track — Group 6: Practice Upgrade
> **File**: 5 of 5 (Track File #36)
> **Audience**: Developers committing to building a professional Copilot workflow

---

## How to Use This Roadmap

Allocate 45-60 minutes per day, 5 days per week.
Each week builds on the previous. Do not skip weeks.
Complete the success criteria before moving to the next week.

---

## Week 1 — Foundations (Beginner → Functional User)

**Goal:** Copilot works on your machine, you understand all 6 surfaces, and you've completed 10 quick wins.

### Day 1
- Read: Copilot Mental Model Gold Sheet
- Exercise: Explain 3 unfamiliar functions in your codebase using Chat + #selection
- Setup: Install Copilot, verify it's working

### Day 2
- Read: GitHub Copilot Setup Gold Sheet
- Exercise: Configure settings.json with all recommended settings
- Setup: Create a personal practice repository

### Day 3
- Read: Copilot Inline Suggestions Gold Sheet
- Exercise: Write 5 functions using comment-driven inline suggestions
- Practice: Tab / Escape / Alt+] cycle — until it feels natural

### Day 4
- Read: Copilot Chat Fundamentals Gold Sheet
- Exercise: Use #file, #selection, #codebase, #sym in 4 different prompts
- Practice: Learn the CGOFC prompt pattern

### Day 5
- Read: Safe Prompting Principles Gold Sheet
- Read: Beginners Quick Wins Gold Sheet
- Exercise: Complete all 10 Quick Win exercises

**Week 1 Success Criteria:**
- [ ] Copilot installed, verified working
- [ ] All 6 surfaces known and can describe use cases
- [ ] All 10 quick wins completed
- [ ] Never pasted a secret into Copilot

---

## Week 2 — Intermediate Power User

**Goal:** Custom instructions configured, 5 prompt files created, Agent Mode used safely once.

### Day 6
- Read: Custom Instructions Deep Dive Gold Sheet
- Exercise: Write `copilot-instructions.md` for your practice repo
- Exercise: Write `python.instructions.md` and `testing.instructions.md`
- Validate: Ask Copilot to confirm it loaded your instructions

### Day 7
- Read: Prompt Files & Slash Commands Gold Sheet
- Exercise: Create 3 prompt files: explain-code, debug-error, generate-tests
- Validate: Type / in Chat — prompt files appear in picker

### Day 8
- Read: Copilot Edits Mode Gold Sheet
- Exercise: Do a 2-file refactoring session using Edits mode
- Review: Read every diff before accepting

### Day 9
- Read: Agent Mode Safe Usage Gold Sheet
- Exercise: Run one Agent Mode session with plan-first requirement
- Prerequisite: Commit before running Agent Mode
- Review: All changes in diff before accepting

### Day 10
- Read: Copilot For Testing Gold Sheet
- Exercise: Generate a complete test suite for one real service class
- Exercise: Run test gap analysis on an existing module

**Week 2 Success Criteria:**
- [ ] `copilot-instructions.md` working in practice repo
- [ ] 5 prompt files available as slash commands
- [ ] One successful Edits mode session with full diff review
- [ ] One Agent Mode session with plan-first + pre-session commit
- [ ] Test suite generated with gap analysis

---

## Week 3 — Advanced Engineering

**Goal:** Custom agents built, AGENTS.md strategy in place, MCP configured, context mastery.

### Day 11
- Read: Custom Agents Deep Dive Gold Sheet
- Exercise: Create 3 custom agents: codebase-navigator, debugging-tutor, test-engineer
- Validate: @ picker shows your agents in Chat

### Day 12
- Read: AGENTS.md Strategy Gold Sheet
- Exercise: Write root AGENTS.md and src/AGENTS.md for practice repo
- Validate: Root and folder levels have no contradictions

### Day 13
- Read: Context Engineering Gold Sheet
- Exercise: Create a project context file for your main project
- Practice: Apply minimum viable context rule to 5 prompts

### Day 14
- Read: Token Optimization Gold Sheet
- Exercise: Rewrite 3 verbose prompts as compact equivalents
- Practice: Select the right model for 5 different task types

### Day 15
- Read: MCP Integration Gold Sheet
- Read: Copilot For Architecture Gold Sheet
- Exercise: Configure `mcp.example.json` (safe, no real secrets)
- Exercise: Run an architecture review prompt on a real module

**Week 3 Success Criteria:**
- [ ] 3+ custom agents created and working
- [ ] AGENTS.md at root and in one subdirectory
- [ ] Project context file maintained
- [ ] Can write compact, token-efficient prompts by habit
- [ ] `mcp.example.json` committed; `mcp.json` gitignored

---

## Week 4 — Pro / Production Level

**Goal:** Personal Copilot OS operational, daily rituals established, debugging handbook internalized.

### Day 16
- Read: Personal Copilot Operating System Gold Sheet
- Exercise: Set up your personal Copilot OS repository (prompts, agents, instructions)
- Establish: Morning planning ritual and end-of-day notes habit

### Day 17
- Read: SDLC Automation With Copilot Gold Sheet
- Exercise: Deliver one complete mini-feature using the 12-phase SDLC workflow
- (Requirements → design → test-first → implement → review → PR description)

### Day 18
- Read: Copilot Debugging Handbook Gold Sheet
- Exercise: Intentionally trigger 3 failure modes and practice diagnosing + fixing
- Internalize: The quick reference table of failure modes

### Day 19
- Read: Responsible AI & Safe Usage Gold Sheet
- Review: All 12 non-negotiable rules
- Self-assessment: Run the Copilot Scoring Rubrics against your current practice

### Day 20
- Read: All Scenario Practice sheets
- Exercise: Complete 4 timed scenario exercises (one per sheet)
- Complete: The full Pro-Level Track Completion Checklist

**Week 4 Success Criteria:**
- [ ] Personal Copilot OS repository active
- [ ] Morning planning + end-of-day notes happening daily
- [ ] One complete mini-feature delivered with 12-phase SDLC workflow
- [ ] Can diagnose 10+ failure modes from the debugging handbook
- [ ] Pro-Level Track Completion Checklist completed
- [ ] Copilot Scoring Rubrics self-assessment completed

---

## After Week 4 — Ongoing Practice

```
Daily (5-10 min):
  - Morning planning ritual
  - End-of-day learning notes (at least 3x per week)

Weekly (15-20 min):
  - Add 1-2 new prompts to library based on repeat tasks
  - Update project context file if architecture changed
  - Review notes from the week for patterns

Monthly (30 min):
  - Scoring rubric self-assessment
  - Prompt library prune and improve
  - Review Copilot changelog for new features
  - Rotate MCP tokens

Signs you've reached Pro level:
  ✓ Copilot is faster for you than manual coding for most boilerplate
  ✓ Your prompt library is used daily without thinking about it
  ✓ You reach for the right agent automatically for each task
  ✓ You catch Copilot errors before running code (visual review habit)
  ✓ You have daily notes going back 30+ days
  ✓ Someone on your team has adopted a prompt or agent you shared
```
