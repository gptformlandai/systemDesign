# React Native 2-Week & 4-Week Mastery Roadmaps — Gold Sheet

> Track File #29 of 37 · Group 7: Practice Upgrade
> Level: beginner to pro | Mode: follow this schedule, not your mood

---

## 1. Who These Roadmaps Are For

**2-Week Roadmap**: You have an interview in 2 weeks. You need to get from basic React/web knowledge to interview-ready for a mid-level React Native role. Aggressive pace, prioritized content.

**4-Week Roadmap**: You want to genuinely master React Native from beginner to senior level, not just pass one interview. Thorough coverage with time for labs, reflection, and senior topics.

---

## 2. Two-Week Sprint Roadmap

### Pre-requisite Check

Before starting, verify you can answer these. If not, spend Day 0 on them:
- Can you explain what a React component, props, and state are?
- Do you understand `useEffect` at a basic level?
- Are you comfortable reading TypeScript?

If yes to all: start Day 1.

---

### Week 1: Foundation + Architecture (Days 1-7)

**Day 1 (2.5 hours) — Core Mental Model**

Morning (90 min):
- Read: `01-Starter-Path/React-Native-Web-Developer-Bridge-Gold-Sheet.md`
- Focus: sections 2 (rendering model), 3 (component mapping), 4 (styling), 6 (text inheritance)
- Task: Open Expo Snack and build a screen with View, Text, Pressable — using only StyleSheet (no inline styles)

Evening (60 min):
- Read: `01-Starter-Path/React-Native-Core-Foundations-Master-Sheet.md`
- Focus: Expo vs bare, Hermes, Metro, app startup flow
- Recall: Close the sheet and write 3 sentences explaining React Native to a web developer

---

**Day 2 (2.5 hours) — Components and Styling**

Morning (90 min):
- Read: `01-Starter-Path/React-Native-Components-Props-State-Hooks-Gold-Sheet.md`
- Read: `01-Starter-Path/React-Native-Styling-Flexbox-Responsive-UI-Gold-Sheet.md`
- Focus: Flexbox column default, SafeAreaView, platform-aware styles

Evening (60 min):
- Lab F1 and F2 from: `07-Practice-Upgrade/React-Native-Runnable-Mini-Labs.md`
- Goal: both labs complete without looking at solution

---

**Day 3 (2.5 hours) — Hooks Deep Dive**

Morning (90 min):
- Read: `01-Starter-Path/React-Native-Hooks-Deep-Dive-Gold-Sheet.md`
- Focus: useEffect cleanup, stale closure, useCallback rules, custom hooks design

Evening (60 min):
- Lab F3 from Runnable Mini Labs
- Active recall: Q1-Q12 from `07-Practice-Upgrade/React-Native-Active-Recall-Question-Bank.md`
- Score and flag any ❌ or ⚠️

---

**Day 4 (2.5 hours) — Error Handling + Navigation**

Morning (90 min):
- Read: `01-Starter-Path/React-Native-Error-Handling-Error-Boundaries-Gold-Sheet.md`
- Read: `02-App-Architecture/React-Native-Navigation-Routing-Deep-Linking-Gold-Sheet.md`
- Focus: Error Boundary class component, async error patterns, navigation param typing

Evening (60 min):
- Lab I1 from Runnable Mini Labs (debounced search)
- Recall: Q16-Q23 from Active Recall Question Bank

---

**Day 5 (2.5 hours) — State Management + TypeScript**

Morning (90 min):
- Read: `02-App-Architecture/React-Native-State-Management-Data-Fetching-Forms-Gold-Sheet.md`
- Read: `02-App-Architecture/React-Native-TypeScript-Deep-Dive-Gold-Sheet.md`
- Focus: Zustand vs Redux vs Context, discriminated unions, navigation typing end-to-end

Evening (60 min):
- Lab I2 from Runnable Mini Labs (useReducer form)
- Active recall: Q7-Q15 from Active Recall Question Bank

---

**Day 6 (2 hours) — Data Fetching + TanStack Query**

Morning (90 min):
- Read: `02-App-Architecture/React-Native-TanStack-Query-Data-Fetching-Internals-Gold-Sheet.md`
- Focus: useQuery, useMutation with optimistic update, useInfiniteQuery + FlatList

Evening (30 min):
- Mock Round 1: `07-Practice-Upgrade/React-Native-Mock-Interview-Scripts.md` — Rounds 1 and 2
- Score using `07-Practice-Upgrade/React-Native-Interview-Scoring-Rubrics.md`

---

**Day 7 (2 hours) — Week 1 Review Day**

- Re-read all ❌ and ⚠️ recall answers from Days 1-6
- Re-do any lab that scored below 4/7 on self-grading checklist
- Write: 5 things you now understand that you did not on Day 1
- Rest: no new content today — consolidation is part of learning

---

### Week 2: Native, Performance, Interview Readiness (Days 8-14)

**Day 8 (2.5 hours) — Native APIs + Platform Patterns**

Morning (90 min):
- Read: `03-Native-Device-And-Internals/React-Native-Native-APIs-Permissions-Storage-Gold-Sheet.md`
- Read: `03-Native-Device-And-Internals/React-Native-Platform-Specific-Patterns-Gold-Sheet.md`
- Focus: SecureStore vs AsyncStorage, permission flow, Platform.select, safe area, keyboard

Evening (60 min):
- Active recall: Q24-Q26 from Active Recall Question Bank
- Flashcard drill: the "Quick Recall — One-line Answers" section at the end of the recall bank

---

**Day 9 (2 hours) — Animations + Lists**

Morning (90 min):
- Read: `03-Native-Device-And-Internals/React-Native-Animations-Gestures-Lists-Gold-Sheet.md`
- Focus: FlatList virtualization, getItemLayout, React.memo on rows, Reanimated UI thread

Evening (30 min):
- Tricky Questions: `05-Scenario-Practice/React-Native-Tricky-Behavior-Questions-Gold-Sheet.md`
- Do Q1-Q8 (all state/render behavior questions)

---

**Day 10 (2.5 hours) — Performance + Testing**

Morning (90 min):
- Read: `04-Senior-MAANG/React-Native-Performance-Memory-Debugging-MAANG-Master-Sheet.md`
- Read: `04-Senior-MAANG/React-Native-Testing-Quality-Gates-Gold-Sheet.md`
- Focus: profiling tools, 60 FPS frame budget, JS vs UI thread, E2E testing setup

Evening (60 min):
- Tricky Questions: Q9-Q15
- Case Studies: `05-Scenario-Practice/React-Native-Production-Debugging-Case-Studies-Gold-Sheet.md` — Cases 1 and 2

---

**Day 11 (2 hours) — Security + Production Architecture**

Morning (90 min):
- Read: `04-Senior-MAANG/React-Native-Security-Offline-Release-Observability-MAANG-Master-Sheet.md`
- Read: `04-Senior-MAANG/React-Native-Production-App-Architecture-MAANG-Master-Sheet.md`
- Focus: token security, OTA limits, staged rollout, feature flags

Evening (30 min):
- Case Studies 3, 4, 5

---

**Day 12 (2.5 hours) — Scenario Practice + Custom Hooks**

Morning (60 min):
- Read: `03-Native-Device-And-Internals/React-Native-Custom-Hooks-Pattern-Library-Gold-Sheet.md`
- Study: Hooks 1, 4, 7, 8, 12, 15 — the most commonly asked

Evening (90 min):
- Scenario Bank: `05-Scenario-Practice/React-Native-MAANG-Interview-Scenario-Bank.md` — all questions
- Quick Revision: `05-Scenario-Practice/React-Native-Quick-Revision-Answer-Templates.md`

---

**Day 13 (2.5 hours) — Full Mock Round**

Full 2-hour mock simulation:
- Round 2 (Hooks and State) — 30 minutes
- Round 3 (Architecture) — 35 minutes
- Round 4 (Performance) — 30 minutes
- Score all rubrics: 1-9

If any rubric scores below 3: spend 25 minutes on that topic before moving to Day 14.

---

**Day 14 (1.5 hours) — Final Readiness Review**

- Read: Quick Revision Answer Templates (the 30-second answer patterns)
- Run through the one-liners at the bottom of the Active Recall Bank
- Write out the interview answer pattern from the track index (8 steps)
- Rest. You are ready.

---

## 3. Four-Week Deep Mastery Roadmap

### Week 1: Complete Foundations (same as 2-week Week 1 but at 60% pace)

Follow 2-week Week 1 with extra time for:
- Building a complete small app: login screen + product list + detail screen (use JSONPlaceholder)
- Lab F1, F2, F3 + Lab I1, I2 — all five labs completed
- Active Recall Bank: all questions in sections 2, 3, 4 scored ✅

---

### Week 2: Architecture and Native Depth

**Mon**: Navigation — full deep dive. Build a complete navigator: Auth stack + Tabs + Stack. Type all params.

**Tue**: TanStack Query — build a complete data layer. useInfiniteQuery + FlatList, optimistic cart mutation.

**Wed**: TypeScript — discriminated unions, generic components, Zod validation. Refactor previous labs to be strict TypeScript.

**Thu**: Native APIs — permissions, SecureStore, AsyncStorage, AppState. Build a custom hook for each.

**Fri**: Platform patterns — build a component with `.ios.tsx` and `.android.tsx` split. Add SafeAreaView properly.

**Weekend**: Build a mini social feed app: login, feed (infinite scroll), like (optimistic), profile screen. Use Expo Router.

---

### Week 3: Senior Topics

**Mon**: New Architecture — Fabric, TurboModules, JSI. Read internals file in full.

**Tue**: Animations — Reanimated worklets, gesture handler, FlatList optimization lab.

**Wed**: Push Notifications — set up expo-notifications in a test project. Handle all 3 app states.

**Thu**: Performance + Debugging — profile the mini app from Week 2. Find and fix one performance issue.

**Fri**: Testing — add unit tests for two custom hooks and one component with Testing Library.

**Weekend**: Add offline support to the mini app from Week 2 (AsyncStorage + mutation queue).

---

### Week 4: Interview Mastery

**Mon**: Case Studies + LLD design — read all 5 case studies. Design offline cart and chat from scratch.

**Tue**: Tricky Questions — all 15 questions. Re-study any you missed.

**Wed**: Mock Round 1 (Foundation + Hooks). Score rubrics 1-5.

**Thu**: Mock Round 2 (Architecture + Performance). Score rubrics 6-9.

**Fri**: Weak area day — study every rubric below 4.

**Weekend**: 
- Full 2-hour mock simulation
- Final readiness rubric check: all must be 4+
- Review the Quick Revision Answer Templates one more time

---

## 4. Daily Study Principles

```text
1. Active recall beats passive reading
   Do not just read — close the sheet and reconstruct the answer.

2. Labs beat reading about labs
   Build it. Even if imperfect. Check the solution after.

3. Spaced repetition
   Re-test ❌ marks the next day. ⚠️ marks in 3 days.

4. 2 hours of focused practice > 6 hours of distracted reading
   No phone. No notifications. Timed sessions.

5. Speak your answers aloud
   Interviews are spoken. Practice spoken answers.

6. Rest is not wasted time
   Sleep consolidates memory. Do not pull all-nighters before the interview.
```

---

## 5. Tracking Your Progress

Print or copy this checklist and mark as you go:

```text
Group 1 (Starter): 6 files
[ ] Core Foundations
[ ] Web Developer Bridge
[ ] Components Props State Hooks
[ ] Hooks Deep Dive
[ ] Styling Flexbox Responsive UI
[ ] Error Handling Error Boundaries

Group 2 (Architecture): 5 files
[ ] Navigation Routing Deep Linking
[ ] State Management Data Fetching Forms
[ ] Project Architecture TypeScript
[ ] TypeScript Deep Dive
[ ] TanStack Query Data Fetching Internals

Group 3 (Native): 6 files
[ ] Native APIs Permissions Storage
[ ] New Architecture Fabric TurboModules JSI
[ ] Animations Gestures Lists
[ ] Push Notifications Background Tasks
[ ] Platform Specific Patterns
[ ] Custom Hooks Pattern Library

Group 4 (Senior): 5 files
[ ] Performance Memory Debugging MAANG
[ ] Testing Quality Gates
[ ] Security Offline Release Observability MAANG
[ ] Production App Architecture MAANG
[ ] GraphQL Apollo URQL

Group 5 (Scenario Practice): 6 files
[ ] MAANG Interview Scenario Bank
[ ] Machine Coding Mini Labs
[ ] Quick Revision Answer Templates
[ ] Tricky Behavior Questions
[ ] LLD Machine Coding Design
[ ] Production Debugging Case Studies

Group 6 (Gold Level Completeness): 4 files
[ ] Networking API Clients Realtime
[ ] Debugging DevTools Native Release
[ ] Build Release Upgrades CICD
[ ] Accessibility I18n Design Systems

Group 7 (Practice Upgrade): 5 files
[ ] Active Recall Question Bank
[ ] Runnable Mini Labs
[ ] Mock Interview Scripts
[ ] Interview Scoring Rubrics
[ ] Mastery Roadmaps (this file)
```

---

## 6. Revision Notes

- 2-week roadmap: 2-2.5 hours daily, covers Groups 1-5 and essential Group 4 topics
- 4-week roadmap: 2 hours daily with deeper labs, covers all 7 groups plus a mini project
- Week 1 of both roadmaps is the same content — depth of execution differs
- Day 7 and Week 4 are deliberately consolidation-heavy — do not skip them
- Labs are not optional — they convert reading into retained skill
- Mock rounds in the final week are the most important practice
- Scoring rubrics gate your progression — a 3 means study more before moving to senior content
