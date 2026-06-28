# MarTech Active Recall Question Bank — Gold Sheet

> Self-test: cover the answer, say it aloud, then reveal. Track what trips you.

---

## Key

🟢 Foundation — must know
🟡 Intermediate — expected at analyst/engineer level
🔴 Senior/MAANG — deep system design and debugging

---

## Group 1: Adobe Data Layer

### 🟢 Q1
What is `window.adobeDataLayer` and who writes to it vs who reads from it?

<details><summary>Answer</summary>

`window.adobeDataLayer` is a JavaScript array on the browser's window object that acts as a shared event bus between engineering and marketing tools.

- **Engineering writes:** Pushes events (page:loaded, hotel:viewed, booking:confirmed) with structured context (page name, user tier, hotel details).
- **Adobe Tags reads:** Listens to push events using the ACDL extension and fires rules in response.

Key benefit: once engineering ships the data layer events, marketing teams can add/change tracking via Tags UI — no engineering deploys needed.

</details>

---

### 🟢 Q2
What happens to events pushed to `adobeDataLayer` before the ACDL library loads?

<details><summary>Answer</summary>

They are NOT lost. The pattern is:
```javascript
window.adobeDataLayer = window.adobeDataLayer || [];
// ↑ initialized as a plain array before library loads
```

Events pushed before the library loads are stored in this plain array. When the ACDL library loads, it processes all queued entries — **replaying** them as if they fired in real time.

This "replay" behavior is intentional — it prevents data loss when Tags loads asynchronously after the page starts pushing events.

</details>

---

### 🟡 Q3
What is the difference between `adobeDataLayer.getState()` and subscribing to a `push` event?

<details><summary>Answer</summary>

- `getState()` returns the **current accumulated state** — the merged object of all pushes so far. Good for reading "what is the current value of `user.loyaltyTier`".
- Event subscription (`addEventListener("hotel:viewed", ...)`) reacts to **future events** — it fires only when new matching events are pushed.

Use `getState()` for on-demand reading; use event listeners (in Tags or JS) for reactive behavior triggered by specific user actions.

</details>

---

## Group 2: Adobe Analytics

### 🟢 Q4
What is the difference between an eVar and a prop in Adobe Analytics?

<details><summary>Answer</summary>

- **eVar (conversion variable):** Persists across multiple hits within a visit (or longer, depending on expiration setting). Can credit downstream conversions — e.g., a search term can credit a booking that happens 3 clicks later.
- **Prop (traffic variable):** Single hit only — no persistence. Cannot credit downstream events. Used for path analysis (next/previous page reports).

Key rule: if you need to attribute a dimension to a conversion metric, use an eVar. If you only need to report on what the user did on that specific hit, a prop is sufficient.

</details>

---

### 🟡 Q5
What does segment "scope" mean in Adobe Analytics?

<details><summary>Answer</summary>

Segment scope determines which container the filter applies to:

- **Hit scope:** Filters individual page views or interactions. "Hits where page name = hotel-detail."
- **Visit scope:** Filters entire sessions. "Visits that contained a hotel view AND a booking." A visit where the user viewed a hotel on page 3 is included even though page 3 alone doesn't match.
- **Visitor scope:** Filters across all sessions for that person. "Visitors who have ever made a booking."

Wrong scope = wrong answer. A booking funnel analysis requires visit scope (not hit scope) to correctly capture multi-step journeys.

</details>

---

### 🟡 Q6
What is a calculated metric in Adobe Analytics and why would you use one?

<details><summary>Answer</summary>

A calculated metric is a formula derived from existing raw metrics — it doesn't require new data collection.

Examples:
- Booking Rate = `Booking Confirmed (event4)` / `Sessions`
- Revenue per Visit = `Revenue (event7)` / `Sessions`
- Cart Abandon Rate = `1 - (Bookings / Checkout Started)`

Use them when you need a ratio or derived KPI in any Workspace report, applied retroactively to all historical data — because they're computed at query time, not stored at collection time.

</details>

---

### 🔴 Q7
Explain how attribution models work in Adobe Analytics and when you would use each.

<details><summary>Answer</summary>

Attribution determines which marketing touchpoint gets credit for a conversion when a user was exposed to multiple channels:

| Model | Logic | Use when |
|---|---|---|
| Last Touch | 100% to last touchpoint | Default; good for direct-response campaigns |
| First Touch | 100% to first touchpoint | Brand awareness campaigns |
| Linear | Equal credit to all | All channels equally valuable |
| Time Decay | More credit to recent touchpoints | Promotions where recency matters |
| Algorithmic | ML-driven data-driven attribution | Enough data volume for ML; most accurate |

Attribution is configurable per eVar (set at collection) or per calculated metric in Analysis Workspace (retroactive). Multi-touch attribution across channels requires a dedicated attribution model applied to conversion events.

</details>

---

## Group 3: Akamai CDN

### 🟢 Q8
What is a CDN POP and how does it reduce latency?

<details><summary>Answer</summary>

A **POP (Point of Presence)** is an Akamai server cluster physically located close to users (Mumbai, Singapore, Frankfurt, etc.).

Without CDN: user in Hyderabad → origin in Virginia = 200ms round trip.
With CDN: user in Hyderabad → Akamai POP in Mumbai = 5ms.

On a cache HIT, the response is served entirely from the POP — the origin server never receives the request. On a cache MISS, Akamai fetches from origin, caches the response, and serves it — future requests from that region hit the cache.

</details>

---

### 🟡 Q9
How does Akamai inject geo-IP data into the page, and why is it not 100% reliable?

<details><summary>Answer</summary>

Akamai uses its **Edgescape** service to map the user's egress IP to a geographic location (country, state, city, timezone, latitude/longitude). It injects this as HTTP response headers:
```
X-Akamai-Edgescape: country_code=IN,region_code=TG,city=HYDERABAD,...
```

The origin server or EdgeWorker reads these headers and passes them to the page (e.g., into the `adobeDataLayer`).

**Why not 100% reliable:**
- VPN users → egress IP shows VPN server's country, not the user's real country
- Corporate proxies → all employees appear to be in the proxy's location
- Mobile carrier NAT → multiple users share one IP
- IPv6 geo-IP databases are still maturing

Use geo as a signal for personalization, not for legal/regulatory enforcement.

</details>

---

### 🔴 Q10
What are EdgeWorkers and when would you choose them over origin logic?

<details><summary>Answer</summary>

EdgeWorkers are JavaScript functions that run on Akamai's edge servers — before requests reach your origin. They can: inspect/modify request headers, respond without touching origin, redirect users, run A/B logic, and inject values.

**Choose EdgeWorkers when:**
- You need <10ms response (no origin round-trip)
- The decision is geo/device/cookie based (all available at edge)
- You want to A/B test without an application deploy
- You need to redirect or gate traffic at global scale

**Do NOT use EdgeWorkers for:**
- Business logic requiring database access (edge has no DB)
- Personalization requiring user profile from CRM (too slow to fetch at edge)
- Complex state management

Rule: EdgeWorkers are stateless, fast, and geography-based. Any decision needing persistent user data goes to origin or AEP.

</details>

---

## Group 4: Adobe Tags (Launch)

### 🟢 Q11
What are the three parts of an Adobe Tags rule?

<details><summary>Answer</summary>

1. **Events:** What triggers the rule. Examples: "Data Pushed - hotel:viewed", "DOM Ready", "Window Loaded", "Click on element".
2. **Conditions:** Optional filters that must be true for actions to run. Examples: "Data element %environment% equals production", "URL path contains /hotels/".
3. **Actions:** What happens when event fires and conditions pass. Examples: "Adobe Analytics - Set Variables", "Adobe Analytics - Send Beacon", "Adobe Target - Load Target", "Custom Code".

</details>

---

### 🟡 Q12
What is the risk of not using staging environment in Adobe Tags before publishing to production?

<details><summary>Answer</summary>

Without staging QA:
- A misconfigured rule fires wrong eVar values in production Analytics — all reporting is corrupted for the period it was live
- A broken rule causes a JavaScript error that blocks all subsequent Tags rules
- A missing condition causes a beacon to fire on every page instead of only hotel pages — Analytics hit volume and billing spike
- Target activity fires on wrong pages, showing personalized content to wrong users

Fix: always use the Staging environment + Adobe Experience Platform Debugger browser extension to verify every rule before promoting to Production. The Tags publish workflow enforces dev → staging → production, so use it.

</details>

---

## Group 5: Adobe Target

### 🟢 Q13
What is the difference between an A/B Test and an Experience Targeting (XT) activity in Adobe Target?

<details><summary>Answer</summary>

- **A/B Test:** Random traffic split between two or more variants. Used to measure which experience statistically converts better. Audience: all visitors (or a broad segment). Goal: find the winner via statistical significance.
- **Experience Targeting (XT):** Rule-based delivery — no randomization. Define who gets what by audience rule. Example: loyalty tier = "titanium" → show suite upgrade offer. Used when you already know what specific segment should see.

A/B test = discovery. Experience Targeting = delivery of known content to known segments.

</details>

---

### 🔴 Q14
What is the "peeking problem" in A/B testing and how do you avoid it?

<details><summary>Answer</summary>

**Peeking problem:** Looking at A/B test results before the planned sample size is reached and stopping the test early when results look significant. Because of random variation, a test may briefly look significant early — if you stop there, you'll often declare a false winner (Type I error rate increases dramatically).

**Why it happens:** With frequentist statistics, p-values fluctuate as data accumulates. Early significance is common and misleading.

**How to avoid:**
1. **Pre-calculate sample size** using effect size, baseline conversion rate, and desired confidence (95%) before the test starts
2. **Commit to running until sample size is reached** — no early stopping
3. **Use Bayesian statistics mode** (available in Target) — "probability of being best" is valid to check at any time
4. **Correct for multiple comparisons** if running many variants (Bonferroni correction)

</details>

---

## Group 6: Adobe Experience Platform

### 🟢 Q15
What is XDM and why does AEP require it?

<details><summary>Answer</summary>

**XDM (Experience Data Model)** is Adobe's standardized JSON schema for customer experience data. It defines canonical fields for common concepts: `web.webPageDetails.name`, `commerce.purchases.value`, `identityMap`, etc.

AEP requires XDM because:
- All data from all sources (web, mobile, CRM, call center) uses the same field names → profile merging is possible
- Segments can reference the same field regardless of which source ingested it
- Destinations know exactly which field contains the booking value

Without XDM: five sources, five schemas → impossible to merge into one profile. With XDM: five sources, one schema → unified profile.

</details>

---

### 🟡 Q16
What is the AEP Identity Graph and how does it stitch anonymous and known identities?

<details><summary>Answer</summary>

The Identity Graph is a linked graph where each node is an identity (ECID, loyalty ID, hashed email, device ID) and edges connect identities that belong to the same person.

Stitching process:
1. Anonymous visit: ECID = aaaa-1111 → creates profile
2. User logs in: loyalty ID = MR-GOLD-789 appears alongside ECID aaaa-1111 → graph links them
3. Mobile app visit: device ID + loyalty ID → graph links device to the same profile
4. Result: aaaa-1111 ↔ MR-GOLD-789 ↔ device-id are one unified profile

**Strong vs weak identities:** Deterministic (loyalty ID + ECID seen together in same event) = high confidence. Probabilistic (same device fingerprint) = lower confidence. AEP uses configurable identity priority.

</details>

---

### 🔴 Q17
Explain the three segment evaluation types in AEP and when you would use each.

<details><summary>Answer</summary>

| Type | Evaluation latency | Update frequency | Best use case |
|---|---|---|---|
| **Batch** | 24h | Once per day | Email campaigns, audiences where recency within a day is fine |
| **Streaming** | <5 min | Continuous as events arrive | Trigger a cart abandon email within 30 min of abandonment |
| **Edge** | <100ms | On each page request | Adobe Target real-time personalization — segment must be resolved before page renders |

**Key constraint:** Not all segment rules qualify for streaming or edge. Complex multi-event, multi-day lookback rules may only support batch. AEP's UI shows which evaluation types are eligible for a given rule set.

</details>

---

## Rapid-Fire Review

| # | Question |
|---|---|
| RF-1 | What does `adobeDataLayer.push()` trigger in Adobe Tags? |
| RF-2 | What header does Akamai inject to indicate user's country? |
| RF-3 | What is the difference between eVar and prop? |
| RF-4 | Name three activity types in Adobe Target. |
| RF-5 | What schema standard does AEP use for all data? |
| RF-6 | What is the peeking problem in A/B tests? |
| RF-7 | Where does Tags get values to set in Analytics (the mechanism)? |
| RF-8 | What AEP service stitches ECID + loyalty ID + email? |
| RF-9 | What does it mean to "activate" a segment in AEP? |
| RF-10 | Why is Akamai geo-IP not 100% accurate? |

<details><summary>Rapid-Fire Answers</summary>

1. It fires any Tags rule with an Event of type "Data Pushed" matching that event name
2. `X-Akamai-Edgescape` (or configured individual headers like `X-Akamai-Country`)
3. eVar persists across hits (can credit conversions); prop is single-hit only (no persistence)
4. A/B Test, Experience Targeting (XT), Auto-Personalize (AP), Multivariate Test (MVT), Recommendations
5. XDM (Experience Data Model)
6. Declaring a winner before the required sample size is reached — false positives from early random variation
7. Data Elements — Tags reads data layer paths or other sources, stores as named variables, references them in rules using `%Element Name%`
8. Identity Service / Identity Graph
9. Sending the audience to a destination (Target, email, paid ads) so those systems can act on the segment membership
10. VPN, corporate proxy, carrier NAT — egress IP doesn't reflect the user's actual location

</details>

---

## Revision Notes

- One-line summary: Seven topic areas tested — data layer, Analytics (eVar/prop/segment), Akamai (CDN/geo), Tags (rules/data elements), Target (A/B/XT/peeking), AEP (XDM/identity/segments).
- Foundation must-know: eVar vs prop, data layer replay, A/B vs XT, XDM purpose.
- Senior differentiators: peeking problem, edge vs streaming segmentation, EdgeWorkers vs origin logic, attribution models.
