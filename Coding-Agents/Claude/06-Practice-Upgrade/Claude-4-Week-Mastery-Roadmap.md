# Claude 4-Week Mastery Roadmap

> **Track**: Claude Mastery Track — Group 6: Practice Upgrade
> **File**: 5 of 5 (Track File #34)
> **Usage**: Day-by-day plan from beginner to professional Claude OS

---

## How to Use This Roadmap

Allocate 45-60 minutes per day, 5 days per week.
Each week builds on the previous — do not skip ahead.
Complete success criteria before moving to the next week.

---

## Week 1 — Foundations (Beginner → Functional User)

**Goal**: Claude set up, CLAUDE.md working, 10 quick wins completed.

### Day 1
- Read: Claude Mental Model Gold Sheet
- Setup: Create Claude.ai account; create first Project with instructions
- Exercise: Ask 3 different questions using CRISP prompt structure

### Day 2
- Read: Claude Setup Personal Machine Gold Sheet
- Setup: Install Claude Code CLI; authenticate; run first session
- Setup: Create ~/.claude/CLAUDE.md with personal preferences
- Exercise: Run 4-level verification protocol

### Day 3
- Read: Claude Chat Fundamentals Gold Sheet
- Exercise: Upload a code file and ask 3 targeted questions about it
- Exercise: Create a second Project for a different domain

### Day 4
- Read: Prompt Engineering Fundamentals Gold Sheet
- Exercise: Rewrite 5 bad prompts using CRISP + format control + constraints

### Day 5
- Read: Safe Usage Principles + Quick Wins Gold Sheets
- Exercise: Complete all 10 Quick Win exercises with real code

**Week 1 Success Criteria:**
- [ ] Claude.ai with Project instructions active
- [ ] Claude Code CLI installed and verified working
- [ ] ~/. claude/CLAUDE.md in place
- [ ] All 10 Quick Wins completed
- [ ] Never pasted a secret into Claude

---

## Week 2 — Intermediate Power User

**Goal**: CLAUDE.md in a real project, 9 slash commands working, agent sessions with checkpoints.

### Day 6
- Read: CLAUDE.md Design Gold Sheet
- Exercise: Create CLAUDE.md for your primary project
- Exercise: Run verification prompts to confirm it's working

### Day 7
- Read: Slash Commands Gold Sheet
- Exercise: Create all 9 core commands in .claude/commands/
- Exercise: Test each command by running it

### Day 8
- Read: Context Engineering Gold Sheet
- Exercise: Practice the summarize-and-restart pattern
- Exercise: Run a session; when it gets long, summarize and start fresh

### Day 9
- Read: Claude Code CLI Gold Sheet
- Exercise: Run a multi-file task using @file references
- Exercise: Create pre_tool_use.sh with basic dangerous command blocking

### Day 10
- Read: Before-After-Prompt-Examples Gold Sheet
- Exercise: Improve 5 prompts from your own past usage

**Week 2 Success Criteria:**
- [ ] CLAUDE.md in primary project with tech stack, arch rules, Do NOT rules
- [ ] All 9 slash commands created and tested
- [ ] pre_tool_use.sh blocking dangerous commands
- [ ] Can use context engineering (session priming, summarize-restart)

---

## Week 3 — Advanced Engineering

**Goal**: Subagents operational, skills firing, MCP configured, verification loops established.

### Day 11
- Read: Subagents Deep Dive Gold Sheet
- Exercise: Create all 7 subagent definition files
- Exercise: Run @planner + @builder pipeline on a small feature

### Day 12
- Read: Skills System Gold Sheet
- Exercise: Create all 4 SKILL.md files (testing, refactoring, docs, performance)
- Exercise: Test that skills fire automatically when relevant

### Day 13
- Read: Hooks Lifecycle Gold Sheet
- Exercise: Create all 3 hooks (pre_tool_use, post_tool_use, on_error)
- Exercise: Test hooks by running a command that should be blocked

### Day 14
- Read: Agent Loops Gold Sheet
- Exercise: Run a test-verify loop on a real function you're implementing
- Exercise: Run a refactor-safe loop on an existing function

### Day 15
- Read: MCP Integration + Token/Context Optimization Gold Sheets
- Exercise: Configure mcp.json with a local server
- Exercise: Rewrite 5 prompts to be more token-efficient

**Week 3 Success Criteria:**
- [ ] All 7 subagents created; planner→builder pipeline run end to end
- [ ] All 4 skills fire automatically
- [ ] All 3 hooks installed and tested
- [ ] Can run autonomous agent loops with verification
- [ ] MCP configured (at least filesystem server)

---

## Week 4 — Pro / Production Level

**Goal**: Personal Claude OS operational, autonomous workflows running, daily rituals established.

### Day 16
- Read: Personal Claude OS Gold Sheet
- Exercise: Set up the full personal OS structure (daily ritual, session notes)
- Establish: Morning planning ritual with /plan

### Day 17
- Read: SDLC Automation Gold Sheet
- Exercise: Run a complete mini-feature through all 12 SDLC phases with Claude

### Day 18
- Read: Debugging Claude Handbook Gold Sheet
- Exercise: Intentionally trigger 3 failure modes; practice recovery

### Day 19
- Read: Verification-Driven Workflows + Autonomous Workflows Gold Sheets
- Exercise: Run a fully autonomous feature build with verification loops

### Day 20
- Read: All Scenario Practice sheets
- Exercise: Complete all 4 timed scenario exercises
- Self-assessment: Run Claude Scoring Rubrics

**Week 4 Success Criteria:**
- [ ] Morning planning ritual established (using /plan daily)
- [ ] End-of-day session notes happening 3+ times per week
- [ ] Can diagnose 10+ failure modes from the handbook
- [ ] Run one fully autonomous feature build with all loops working
- [ ] Scoring rubrics self-assessment completed

---

## After Week 4 — Ongoing

```
Daily (5-10 min):
  - Morning /plan ritual
  - End-of-day session notes

Weekly (15-20 min):
  - Add 1-2 new slash commands based on repeated prompts
  - Update CLAUDE.md if new conventions established
  - Review session notes for patterns

Monthly (30 min):
  - CLAUDE.md audit (stale rules?)
  - Slash command prune and improve
  - Skills review (new skills needed?)
  - Hook review (new validations needed?)

Signs you've reached Pro level:
  ✓ Claude follows your conventions without prompting
  ✓ Verification loops catch all issues before you see them
  ✓ Subagent pipeline runs with minimal oversight
  ✓ Session notes go back 30+ days
  ✓ You've shared a skill or CLAUDE.md template with a teammate
```
