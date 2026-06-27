# Build Tools — Mastery Roadmap — Gold Sheet

> 1-week sprint and 2-week mastery plans with daily session structure

---

## Track Overview

| Track Stage | Description | Files to Cover |
|---|---|---|
| Foundations (Group 01-02) | Pipeline model, transpilation, module systems | 5 files |
| Core Tools (Group 03-04) | HMR, optimization, bundler deep dives | 9 files |
| Advanced Architecture (Group 05) | Performance, debugging, CI/CD, monorepos | 5 files |
| New Topics (New files) | TypeScript, CSS, Assets, Env, Library, MF, Monorepo | 7 files |
| Practice Infrastructure (Group 06) | Active recall, mock interviews, case studies | 6 files |

**Total: 32 content files + 6 practice files**

---

## Plan A: 1-Week Sprint (Interview in ~7 days)

**Goal:** Cover all foundational and core topics; get to junior-ready by Day 4, mid-level-ready by Day 7.

---

### Day 1 — Pipeline Foundations (2 hours)

**Morning (60 min): Read**
- [ ] `01-Pipeline-Foundations/Build-Pipeline-Overview-Gold-Sheet.md`
- [ ] `02-Transpilation-Modules-Bundling/Transpilation-Babel-SWC-esbuild-TypeScript-Gold-Sheet.md`
- [ ] `02-Transpilation-Modules-Bundling/TypeScript-Config-Deep-Dive-Gold-Sheet.md`

**Evening (60 min): Active Recall**
- [ ] Active Recall Q1, Q2, Q10, Q11, Q12 (Core Concepts + Transpilation group)
- [ ] Tricky Scenarios: TS-1, TS-2, TS-3

**Day 1 Self-check:** Can you explain loaders vs plugins and what `isolatedModules: true` does?

---

### Day 2 — Module Systems + Bundling (2 hours)

**Morning (60 min): Read**
- [ ] `02-Transpilation-Modules-Bundling/Module-Systems-ESM-CommonJS-Tree-Shaking-Gold-Sheet.md`
- [ ] `02-Transpilation-Modules-Bundling/Bundling-Core-Dependency-Graph-Chunking-Gold-Sheet.md`

**Evening (60 min): Active Recall**
- [ ] Active Recall Q6, Q7, Q8, Q9 (Module Systems group)
- [ ] Tricky Scenarios: BO-1, BO-2

**Day 2 Self-check:** Can you explain why CommonJS can't be tree-shaken and what the `exports` field does?

---

### Day 3 — Dev Server + HMR (2 hours)

**Morning (60 min): Read**
- [ ] `03-Optimization-DevServer-HMR/Dev-Server-File-Watching-HMR-Pipeline-Gold-Sheet.md`
- [ ] `03-Optimization-DevServer-HMR/HMR-Hot-Reload-Internals-State-Preservation-Gold-Sheet.md`

**Evening (60 min): Active Recall**
- [ ] Active Recall Q17, Q18, Q19 (Dev Server + HMR group)
- [ ] Environment Variables: Active Recall Q26, Q27
- [ ] Read: `03-Optimization-DevServer-HMR/Environment-Variables-Feature-Flags-Build-Gold-Sheet.md`

**Day 3 Self-check:** Can you explain Vite dev server vs Webpack, and why HMR falls back to full reload?

---

### Day 4 — Bundler Deep Dives (2.5 hours)

**Morning (75 min): Read**
- [ ] `04-Bundler-Deep-Dives/Webpack-Deep-Dive-Loaders-Plugins-DevServer-Gold-Sheet.md`
- [ ] `04-Bundler-Deep-Dives/Vite-Modern-Dev-Server-Rollup-Production-Gold-Sheet.md`

**Evening (75 min): Active Recall + Mock**
- [ ] Active Recall Q20, Q21 (Bundler Comparison group)
- [ ] Tricky Scenarios: WP-1 through WP-5 + VT-1, VT-2, VT-3
- [ ] **Do Mock Interview Round 1** (time yourself)

**Day 4 Self-check:** Did you score 5+/7 on Round 1? If not, identify gaps.

---

### Day 5 — Optimization + Performance (2 hours)

**Morning (60 min): Read**
- [ ] `03-Optimization-DevServer-HMR/Code-Optimization-Tree-Shaking-Minification-Splitting-Gold-Sheet.md`
- [ ] `05-Performance-Debugging-Architecture/Build-Performance-Pipeline-Cold-Start-Hot-Updates-Gold-Sheet.md`

**Evening (60 min): Active Recall**
- [ ] Active Recall Q13, Q14, Q15, Q16 (Optimization group)
- [ ] Active Recall Q29, Q30, Q31 (Performance + Debugging group)
- [ ] Case Study 4 (12-minute CI build)

**Day 5 Self-check:** Can you explain content hashing, code splitting strategies, and Turborepo cache misses?

---

### Day 6 — Advanced Architecture (2 hours)

**Morning (60 min): Read**
- [ ] `04-Bundler-Deep-Dives/Webpack-Module-Federation-Micro-Frontends-Gold-Sheet.md`
- [ ] `05-Performance-Debugging-Architecture/Monorepo-Build-Turborepo-Nx-Workspaces-Gold-Sheet.md`
- [ ] `05-Performance-Debugging-Architecture/Debugging-Builds-Source-Maps-Bundle-Issues-Gold-Sheet.md`

**Evening (60 min): Active Recall + Case Studies**
- [ ] Active Recall Q22, Q23, Q24, Q25 (Monorepo + Library group)
- [ ] Case Study 1 (5MB bundle), Case Study 3 (broken source maps)
- [ ] Tricky Scenario WP-1, WP-2

**Day 6 Self-check:** Can you configure Module Federation shell + remote and explain `singleton: true`?

---

### Day 7 — Full Review + Mock (2 hours)

**Morning (60 min): Fill gaps**
- [ ] Re-read any gold sheet where you scored <3 in your tracking
- [ ] Read: `05-Performance-Debugging-Architecture/Real-World-Build-Pipeline-Design-CICD-Gold-Sheet.md`

**Evening (60 min): Full Mock**
- [ ] **Do Mock Interview Round 2** (time yourself, record if possible)
- [ ] Score yourself using the Scoring Rubrics file
- [ ] Identify any topic below 3 and read its summary section

**Day 7 Goal:** Mid-level-ready (all core topics at score 3+, most at 4+).

---

## Plan B: 2-Week Mastery (Interview in 10-14 days)

**Goal:** Full track coverage, reach senior-ready by end of Week 1, staff-ready by end of Week 2.

---

### Week 1: Full Coverage (Same as 1-week sprint + extended reading)

Follow the Day 1-7 plan above, but extend each session to cover ALL gold sheets in each group:

**Day 1 additions:**
- [ ] `01-Pipeline-Foundations/Build-Pipeline-Overview-Gold-Sheet.md` (full deep read)

**Day 2 additions:**
- [ ] `02-Transpilation-Modules-Bundling/CSS-PostCSS-Processing-Build-Pipeline-Gold-Sheet.md`

**Day 3 additions:**
- [ ] `03-Optimization-DevServer-HMR/Asset-Handling-Images-Fonts-SVG-Gold-Sheet.md`

**Day 4 additions:**
- [ ] `04-Bundler-Deep-Dives/Parcel-Zero-Config-Auto-Optimization-Gold-Sheet.md`
- [ ] `04-Bundler-Deep-Dives/Modern-Build-Tools-esbuild-Rollup-Turbopack-Metro-Gold-Sheet.md`

**Day 6 additions:**
- [ ] `04-Bundler-Deep-Dives/Library-Publishing-Pipeline-tsup-Rollup-Gold-Sheet.md`
- [ ] Active Recall Q23, Q24, Q25 (Library group)

---

### Week 2: Depth + Mastery

---

**Day 8 — All Case Studies (2 hours)**
- [ ] Read all 5 case studies (production debugging)
- [ ] For each: write down root cause + fix without looking
- [ ] Case Study 2 (HMR stopped working): trace to Rules of Hooks

---

**Day 9 — All Tricky Scenarios (2 hours)**
- [ ] All WP scenarios (WP-1 through WP-5)
- [ ] All VT scenarios (VT-1, VT-2, VT-3)
- [ ] All TS scenarios (TS-1, TS-2, TS-3)
- [ ] All BO scenarios (BO-1, BO-2)

---

**Day 10 — Deep Read: Architecture (2 hours)**
- [ ] Module Federation full re-read (section 9: alternatives table)
- [ ] Monorepo re-read (section 8: Turborepo vs Nx comparison)
- [ ] `05-Performance-Debugging-Architecture/Real-World-Build-Pipeline-Design-CICD-Gold-Sheet.md`

---

**Day 11 — Mock Interview Round 2 (1 hour)**
- [ ] Full timed Round 2
- [ ] Score with rubric
- [ ] For any topic < 4: create a personal cheat note (3 bullets max)

---

**Day 12 — Weak Area Intensive (2 hours)**
- [ ] Re-read any gold sheets where you scored < 4
- [ ] Do all active recall questions for those topics again
- [ ] Write explanations in your own words (Feynman technique)

---

**Day 13 — System Design Practice (2 hours)**
- [ ] Practice Round 3 Q1 out loud: "Design a build pipeline for a 5-team e-commerce monorepo"
- [ ] Practice Round 3 Q2: "Library publishing pipeline"
- [ ] Time yourself — both should be answered in 10-12 minutes

---

**Day 14 — Final Mock + Readiness Check (1.5 hours)**
- [ ] **Full Mock Interview Round 3**
- [ ] Score with rubric
- [ ] Check readiness gates:
  - [ ] All topics at 4+ → Senior Ready ✓
  - [ ] Majority at 5 → Staff Ready ✓

---

## Daily Session Format

**Recommended session structure (60-90 minutes):**

| Phase | Time | Activity |
|---|---|---|
| Warm-up | 5 min | Review previous session's revision notes |
| Core Read | 25-35 min | New gold sheet(s) |
| Active Recall | 15-20 min | Question bank for today's topics |
| Application | 10-15 min | Tricky scenario or case study |
| Revision Notes | 5 min | Write 3 bullets: what you learned today |

---

## Weekly Milestones

| End of... | Milestone |
|---|---|
| Day 4 | Junior-ready (pass Round 1 mock) |
| Day 7 | Mid-level-ready (pass Round 2 mock at 5+/7) |
| Day 10 | All topics read at least once |
| Day 14 | Senior-ready (all topics at 4+) |

---

## Revision Notes

- One-line summary: 7-day sprint for mid-level readiness; 14-day mastery for senior/staff readiness.
- Daily rhythm: Read → Active Recall → Apply → Write 3 bullets.
- Gate check: Take Mock Round 1 on Day 4. If <5/7, extend plan by 2 days before moving to Round 2.
