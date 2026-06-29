---
name: Daily Planner
description: Structure your coding session with a clear plan before writing code
---

I'm starting my development session. Help me plan it.

Today's task or goal:
${input:Paste your ticket description, GitHub issue, or personal goal}

Using #codebase (if available) to understand the project:

Give me:

1. **Implementation steps** (3-7 steps, ordered by dependency)
   For each step:
   - What specifically to do
   - Which files are affected (exact paths if you can find them)
   - Copilot mode to use: inline / Chat Ask / Edits / Agent Mode

2. **Blockers to resolve first** (3 max)
   Questions or unknowns that, if wrong, will waste the entire session

3. **Risk flag** (1-2 sentences)
   The most likely thing to go wrong and how to avoid it

4. **Success criteria**
   How I'll know this session was successful (testable statement)

5. **Time estimate** (rough)
   S = 1-2 hours / M = half day / L = full day

Rules:
- Do NOT start implementing — planning only
- Be specific about file paths, not "the service file"
- If you're uncertain about a file location: ask instead of guessing
- Under 300 words total
