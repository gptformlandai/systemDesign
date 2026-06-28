# MarTech Stack Overview — Adobe Ecosystem — Gold Sheet

> Topic: The full Adobe MarTech stack — who does what, how they connect, and why it matters

---

## 1. Intuition

Think of MarTech as the engineering infrastructure that exists between your website and business decisions. When a user lands on a hotel booking page, loads a search, clicks a room, and completes a booking — every one of those moments is a signal. MarTech is the system that captures those signals, stores them, analyzes them, and uses them to make the next experience better.

Beginner version:

> MarTech is the plumbing that connects user behavior (clicks) to business intelligence (reports, personalization, ads).

---

## 2. Definition

- **Definition:** Marketing Technology (MarTech) is the set of tools and systems that collect, process, analyze, and act on data about user interactions across digital channels.
- **Category:** Customer experience engineering + data infrastructure.
- **Core idea:** Every user action is an event; MarTech captures events, builds profiles, and closes the loop with personalization.

---

## 3. The Complete Data Flow

```txt
1. User opens browser → visits www.marriott.com
        │
        ▼
2. Akamai CDN
   - Serves static assets from edge (fast delivery)
   - Injects geo-IP metadata (country, state, city) into HTTP response headers
   - Runs bot detection (blocks scrapers, credential stuffing)
   - Optionally runs EdgeWorkers (JavaScript at the edge for A/B or redirect logic)
        │
        ▼
3. Browser loads the page
   - React / Angular / server-rendered HTML renders
   - Adobe Tags (JavaScript snippet) loads asynchronously
   - Page code pushes events to adobeDataLayer[]
        │
        ▼
4. Adobe Tags (Launch) — the traffic cop
   - Listens for adobeDataLayer push events
   - Fires rules: "on page view → send data to Adobe Analytics"
   - Fires rules: "on booking complete → fire conversion pixel"
   - Sends beacons to Adobe Analytics, Target, and AEP
        │
        ▼
5. Adobe Analytics
   - Receives the beacon (image request or XHR)
   - Stores dimensions (eVars, props) and metrics (events)
   - Analysts build reports: conversion funnels, path analysis, cohort reports
        │
        ▼
6. Adobe Target
   - Receives profile data from same Tags beacon
   - Applies A/B test: user is in "Variant B — show promotion banner"
   - Returns personalized content to the browser (before page renders, via at.js)
        │
        ▼
7. Adobe Experience Platform (AEP)
   - Ingests events from Analytics, Tags, CRM, call center, email
   - Builds a unified Real-Time Customer Profile across all touchpoints
   - Computes audience segments: "users who searched but did not book in 7 days"
   - Activates segments to: email (Marketo), paid ads (Google/Meta), on-site Target
```

---

## 4. What Each Product Owns

### Adobe Tags (Launch)

```
Role: Tag manager — orchestrates all other tools
Layer: Client-side JavaScript, loaded on every page
Does: Fires marketing pixels, sends analytics beacons, triggers experiments
Does NOT do: Store data long-term
```

### Adobe Analytics

```
Role: The clickstream database of record
Layer: Server-side + reporting UI (Analysis Workspace)
Does: Stores every page view, click, video play, conversion event
Does NOT do: Real-time profile unification across channels
```

### Adobe Target

```
Role: Experimentation and personalization engine
Layer: Client-side (at.js or alloy.js) + server API
Does: A/B tests, multivariate tests, Auto-Personalize (ML-driven)
Does NOT do: Long-term analytics storage
```

### Adobe Experience Platform (AEP)

```
Role: Unified data platform and Real-Time CDP
Layer: Cloud platform (SaaS)
Does: Ingests all event streams, unifies identity, builds segments, activates audiences
Does NOT do: Replace Analytics (it's a different product, often runs alongside)
```

### Akamai

```
Role: CDN + edge security + edge compute
Layer: Between DNS and origin servers
Does: Content delivery, TLS termination, DDoS/bot protection, geo-IP, EdgeWorkers
Does NOT do: Marketing analytics or personalization directly
```

---

## 5. Why MarTech Exists as a Separate Layer

Without MarTech:
```
Engineer deploys → Business team wants new tracking → Engineer deploys again → ...
```

With MarTech (Adobe Tags):
```
Engineer deploys data layer with standard events.
Marketing team configures tracking rules in Tags UI — no code deploy.
New campaign tracking: done in 1 hour, not 2 weeks.
```

**Key insight:** Tags decouples marketing instrumentation from engineering releases.

---

## 6. The Three Pillars of MarTech Data

```
1. COLLECT          2. ANALYZE          3. ACT
─────────────       ───────────         ────────────────
Data Layer          Adobe Analytics     Adobe Target
Adobe Tags          CJA                 Email (Marketo)
Web SDK             Reports             Paid Ads (DV360)
Mobile SDK          Segments            Push Notifications
Server API          Dashboards          On-site Content
```

---

## 7. First-Party vs Third-Party Data

| Type | Definition | Example | Adobe Product |
|---|---|---|---|
| First-party | Data you collect directly from your users | Booking history, click behavior, email opens | AEP, Analytics |
| Second-party | Another company's first-party data shared with you | Hotel partner's guest data | AEP data sharing |
| Third-party | Aggregated data from data brokers | Demographics, interest segments | Audience Manager (legacy) |

**Post-cookie era:** Third-party data is dying (Chrome phased out third-party cookies). First-party data strategies via AEP are now the primary focus.

---

## 8. Identity Resolution — How MarTech Knows It's "You"

```
Anonymous visit           → cookie ID (ECID — Experience Cloud ID)
Log in                    → CRM ID / loyalty ID
Email click               → email address hash
Mobile app               → device IDFA / push token
Call center call          → phone number

AEP Identity Graph stitches all of these together into one profile
Result: "The user who visited anonymously yesterday is the same loyalty member who booked today"
```

ECID (Experience Cloud ID) is Adobe's first-party cookie that persists across Adobe products — it's the glue.

---

## 9. Common Architecture Patterns

### Pattern 1: Web + Analytics Only (smallest footprint)

```
Page → Tags → Analytics
```
Used by: small e-commerce, informational sites.

### Pattern 2: Web + Analytics + Target (A/B focus)

```
Page → Tags → Analytics
             → Target (personalization)
```
Used by: conversion-focused retail, booking sites.

### Pattern 3: Full Enterprise AEP Stack

```
Web → Tags → Analytics → AEP (ingestion)
Mobile → SDK → AEP
CRM → AEP (batch)
Call Center → AEP (streaming)
            → AEP Real-Time CDP → Target (segment-based personalization)
                                 → Email (Marketo)
                                 → Paid Ads (Google, Meta)
```
Used by: Marriott, airlines, financial services — anywhere omnichannel matters.

---

## 10. Common Mistakes

| Mistake | Consequence | Fix |
|---|---|---|
| Firing Tags rules on every keypress | Analytics hit volume explosion, cost spike | Debounce or fire on specific events (blur, submit) |
| No data layer schema governance | Different pages push different field names — Analytics reports break | Define and enforce a data layer spec document |
| Mixing eVar slot numbers across teams | Two teams both use eVar10 for different things — data corrupted | Maintain an eVar/prop variable registry |
| Using third-party cookies for personalization | Data disappears when browser blocks cookies | Migrate to first-party ECID + AEP identity graph |
| No data governance in AEP | PII fields used in ad targeting — GDPR violation | Use AEP Data Governance labels (C2, C5 labels) |

---

## 11. Interview Insight

Strong answer:

> The Adobe MarTech stack works in layers: Akamai handles delivery and geo-IP at the edge before the page loads. Adobe Tags loads on the page and listens to data layer events — it decouples marketing instrumentation from engineering deploys. Analytics stores the clickstream. Target uses the same data layer events to serve personalized content before the page renders. AEP ingests all of it, stitches identities across channels, and activates audiences back into Target, email, and paid ads.

Follow-up trap:

> What happens if Adobe Tags fails to load?

Good answer:

> If Tags fails (CDN outage, blocked by ad-blocker, slow network), all dependent marketing pixels don't fire — no Analytics data, no Target personalization, no conversion tracking. This is why critical business-logic (checkout, payments) must never depend on Tags loading. The solution: Tags loads asynchronously and pages are designed to function without it. Critical events should be fired server-side via the Edge Network API as a backup.

---

## 12. Revision Notes

- One-line summary: MarTech is the event pipeline from browser click to business insight and back to personalized experience.
- Three keywords: data layer, Tags, identity.
- One interview trap: never put business-critical logic inside a tag — it's marketing infrastructure, not application infrastructure.
- Memory trick: Tags is the traffic cop. Analytics stores the evidence. Target acts on it. AEP unifies everything.
