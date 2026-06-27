# React Native Interview Scoring Rubrics — Gold Sheet

> Track File #28 of 37 · Group 7: Practice Upgrade
> Level: all levels | Mode: honest self-assessment with 1-5 rubrics

---

## 1. How to Use This Sheet

After each mock interview round or concept explanation, score yourself on the relevant rubric. Be honest — inflated scores hide weak spots that will surface in real interviews.

**Scale:**
- 1 — Blank or completely wrong
- 2 — Some correct pieces but major gaps, no structure
- 3 — Correct but surface-level, no internals, no trade-offs
- 4 — Solid answer with internals and one trade-off mentioned
- 5 — Complete answer: mental model, internals, code, traps, production judgment, trade-offs

**Readiness gates**: Do not move to the next group until 4+ on all rubrics in the current group.

---

## 2. Rubric 1: React Native Rendering Model

**Question**: Explain how React Native renders UI.

| Score | What That Looks Like |
|---|---|
| 1 | "It uses React to make apps" — no mention of native views |
| 2 | "It renders native components" — no explanation of how |
| 3 | "Components → native views via a bridge/renderer. It is not a WebView." — correct but no depth |
| 4 | Explains JS thread, UI thread, React renderer → shadow tree → Yoga layout → native views. Mentions Hermes. |
| 5 | Adds New Architecture: Fabric renderer, JSI, TurboModules, why they replace the old async bridge. Explains synchronous nature. Mentions render pipeline: render phase, commit phase, view mounting. |

**Your score**: ___

---

## 3. Rubric 2: useState and Re-renders

**Question**: How does React decide whether to re-render after setState?

| Score | What That Looks Like |
|---|---|
| 1 | "It always re-renders" |
| 2 | "It re-renders when state changes" |
| 3 | "Uses shallow comparison with Object.is — same reference = no re-render" |
| 4 | Explains functional updates for correct prev-based updates. Demonstrates mutable object trap. Explains React 18 batching. |
| 5 | Adds: how batching works in async vs sync contexts. How `useTransition` and concurrent features affect scheduling. When `React.memo` prevents child re-renders. |

**Your score**: ___

---

## 4. Rubric 3: useEffect

**Question**: Explain useEffect, its cleanup, and dependency array.

| Score | What That Looks Like |
|---|---|
| 1 | "It runs code after render" — no mention of deps or cleanup |
| 2 | "Empty array = run once, deps array = run when deps change" |
| 3 | Explains cleanup: runs before next effect and on unmount. Mentions stale closure danger. |
| 4 | Demonstrates stale closure with setInterval. Explains cancelled flag pattern. Explains when to use AbortController. |
| 5 | Adds: how React runs cleanup before re-running effect (with captured old values). Difference between `useEffect` and `useLayoutEffect` timing. Why ESLint exhaustive-deps rule exists. |

**Your score**: ___

---

## 5. Rubric 4: Error Boundaries

**Question**: What is an Error Boundary and what does it catch?

| Score | What That Looks Like |
|---|---|
| 1 | "It catches errors" — no specifics |
| 2 | "It's a component that catches render errors and shows fallback UI" |
| 3 | Correctly states it only catches render-time errors. Knows async is NOT caught. |
| 4 | Shows the class component implementation (`getDerivedStateFromError` + `componentDidCatch`). Explains granular boundaries vs global boundary. Mentions recovery action (Try Again button). |
| 5 | Adds: integration with crash reporters (Sentry). Global unhandled promise rejection handler setup. Distinction between ErrorBoundary for render errors and try/catch for async errors. When to split vs consolidate boundaries. |

**Your score**: ___

---

## 6. Rubric 5: Navigation

**Question**: How do you architect navigation for a large React Native app?

| Score | What That Looks Like |
|---|---|
| 1 | "Use React Navigation" — no architecture |
| 2 | "Stack navigator with screens" |
| 3 | Mentions AuthStack vs MainStack based on auth state. Tabs + Stack composition. |
| 4 | Explains TypeScript param lists, `navigate()` vs `push()` difference, deep link configuration, navigation ref for use outside components. |
| 5 | Adds: modal stack for presentations, `Linking.getInitialURL()` for cold start deep links, navigation event listener for foreground deep links, Android BackHandler, screen focus effects for data refresh. |

**Your score**: ___

---

## 7. Rubric 6: Performance

**Question**: A list screen is janky on older devices. How do you investigate and fix it?

| Score | What That Looks Like |
|---|---|
| 1 | "Optimize the code" — vague |
| 2 | "Use FlatList instead of ScrollView" |
| 3 | Mentions React.memo on rows, useCallback on handlers, keyExtractor with stable IDs |
| 4 | Names specific profiling tools (React Native DevTools Profiler, Perf Monitor). Distinguishes JS FPS vs UI FPS. Explains getItemLayout. |
| 5 | Adds: windowSize tuning, maxToRenderPerBatch, removeClippedSubviews, image caching with react-native-fast-image, Reanimated for UI-thread animations, bundle size impact on startup, Hermes profiling. Knows what to measure before fixing. |

**Your score**: ___

---

## 8. Rubric 7: Security

**Question**: What are the top security concerns for a production React Native app?

| Score | What That Looks Like |
|---|---|
| 1 | "Use HTTPS" — only one point |
| 2 | HTTPS + don't expose secrets in code |
| 3 | Token storage in SecureStore/Keychain, HTTPS enforcement, deep link validation |
| 4 | Adds: certificate pinning awareness, obfuscation limits, dependencies audit with `npm audit`, ProGuard/R8, biometric auth patterns, PII handling |
| 5 | Adds: OWASP Mobile Top 10 awareness, code obfuscation vs security (not sufficient alone), jail-break detection nuances, secure clipboard, prevent screenshots for sensitive screens, token refresh security (no refresh storms) |

**Your score**: ___

---

## 9. Rubric 8: Machine Coding / LLD Round

**Criteria for a shopping cart or chat screen design:**

| Score | What That Looks Like |
|---|---|
| 1 | Cannot produce working code in time |
| 2 | Produces code with logic errors, no TypeScript, no error state |
| 3 | Working code, handles happy path, has TypeScript types |
| 4 | Handles: loading state, error state, empty state, offline state. Separates business logic into hooks. |
| 5 | All above + optimistic updates, cleanup in all useEffects, `React.memo` on list rows, `useCallback` on handlers, cancellation pattern, edge case explanation, and verbal walk-through of trade-offs. |

**Your score**: ___

---

## 10. Rubric 9: System Design (Senior)

**Criteria for offline support or notification system design:**

| Score | What That Looks Like |
|---|---|
| 1 | Generic description without specifics |
| 2 | Names some tools (AsyncStorage, push notifications) without architecture |
| 3 | Describes the flow but misses edge cases (cold start, token refresh, queue drain) |
| 4 | Complete architecture: explains each layer, handles all app states, discusses data persistence, covers retry/rollback |
| 5 | All above + discusses scale (what changes at 10M users), monitoring (Sentry, analytics), rollback strategy, A/B testing integration, platform differences (iOS vs Android), and makes explicit trade-off decisions |

**Your score**: ___

---

## 11. Readiness Assessment

Score yourself across all rubrics and assess overall readiness:

| Level | Criteria |
|---|---|
| Group 1 Starter Ready | Rubrics 1, 2, 3 all score 3+ |
| Group 2-3 Intermediate Ready | Rubrics 1-5 all score 3+; Rubrics 1-3 score 4+ |
| Group 4 Senior Ready | Rubrics 1-7 all score 4+; Rubric 6, 7 score 4+ |
| MAANG Ready | All rubrics score 4+; Rubrics 8, 9 score 4+; 2 full mock rounds completed with 4+ average |

---

## 12. Weak Area Action Plan

When a rubric scores below 3:

```text
Score 1-2 on Rubric X:
  1. Re-read the concept sheet for that topic
  2. Run the relevant mini lab
  3. Write a 200-word explanation as if teaching a junior developer
  4. Re-take the rubric after 24 hours
  5. Repeat until 3+

Score 3 on a rubric:
  1. Study the "what a 4 looks like" row above
  2. Practice the specific gap (internals, trade-offs, production judgment)
  3. Add one production example from your own experience
  4. Re-test in next mock round
```

---

## 13. Revision Notes

- Rubrics 1-3 are foundational — score 4+ before attempting senior rounds
- A "5" requires production judgment, not just correct mechanics
- "Score 3" = interview-safe for junior/mid roles; "Score 4-5" = competitive for senior/MAANG
- Use the mock interview scripts to generate the scenarios you score on these rubrics
- Honest self-scoring matters more than getting a high number — gaps found now save you from surprises in real interviews
- Track your scores over time — improvement across 2 weeks is the signal that the study system is working
