# Adobe Tags (Launch) — Rules, Data Elements, Publishing — Gold Sheet

> Topic: Adobe Tags — the tag manager that orchestrates all MarTech without code deploys

---

## 1. Intuition

Adobe Tags is a tag manager. Think of it as an if-this-then-that system for the browser. "IF the user views a hotel page, THEN send an Analytics beacon." The key power: marketing and analytics teams configure these rules in a UI — without engineering shipping new code. One engineering deployment adds the data layer; Tags handles everything else.

Beginner version:

> Adobe Tags is a rule engine in the browser. Engineers build the data layer; Tags reacts to it and sends data to all the right places — without code deploys every time the business needs new tracking.

---

## 2. Definition

- **Definition:** Adobe Tags (formerly Adobe Launch) is a client-side tag management system (TMS) that loads JavaScript libraries, listens to data layer events, and fires configured rules to send data to analytics, personalization, and advertising tools.
- **Category:** Tag Management System (TMS).
- **Core idea:** Separate marketing instrumentation from engineering code. Rules live in Tags, not in application code.

---

## 3. Core Concepts

```
┌─────────────────────────────────────────────────────┐
│  PROPERTIES                                          │
│  A container per website/app                         │
│  Example: "Marriott.com - Production"               │
└─────────────────────────────────────────────────────┘
         │
         ▼
┌─────────────────────────────────────────────────────┐
│  EXTENSIONS                                          │
│  Plug-ins that add capabilities                      │
│  Examples:                                           │
│    Adobe Analytics                                   │
│    Adobe Target                                      │
│    Adobe Experience Platform Web SDK                 │
│    Adobe Client Data Layer                           │
│    Google Analytics 4                                │
│    Meta Pixel                                        │
└─────────────────────────────────────────────────────┘
         │
         ▼
┌──────────────┐   ┌──────────────┐   ┌──────────────┐
│  DATA         │   │  RULES       │   │  PUBLISH     │
│  ELEMENTS     │   │  Events      │   │  Environments│
│  (variables)  │   │  Conditions  │   │  Build       │
│               │   │  Actions     │   │  Deploy      │
└──────────────┘   └──────────────┘   └──────────────┘
```

---

## 4. Data Elements — Reading from the Data Layer

Data elements are named variables that extract values from sources:

```
Data Element: "Page Name"
  Type: Adobe Client Data Layer - Computed State
  Path: page.name
  Result: "hotel-detail"

Data Element: "Hotel Name"
  Type: Adobe Client Data Layer - Computed State
  Path: hotel.name
  Result: "Marriott Marquis Times Square"

Data Element: "User Loyalty Tier"
  Type: Adobe Client Data Layer - Computed State
  Path: user.loyaltyTier
  Result: "gold"

Data Element: "Geo Country"
  Type: Adobe Client Data Layer - Computed State
  Path: geo.country
  Result: "IN"

Data Element: "Query String - Campaign"
  Type: Query String Parameter
  Parameter name: utm_campaign
  Result: "email-june-2026"

Data Element: "Cookie - Return Visitor"
  Type: Cookie
  Cookie name: return_visitor
  Result: "true"
```

Data elements are referenced in rules using `%data element name%` syntax.

---

## 5. Rules — The If-Then Engine

A rule has three parts: **Events**, **Conditions**, and **Actions**.

### Rule Example 1: Page View Tracking

```
Rule: "Adobe Analytics - Page View"

EVENT:
  Type: Data Pushed (Adobe Client Data Layer extension)
  Specific Event: page:loaded

CONDITIONS:
  (none — fire on all pages)

ACTIONS:
  1. Adobe Analytics - Set Variables
     pageName = %Page Name%          → maps to s.pageName
     eVar20 = %User Loyalty Tier%    → s.eVar20 = "gold"
     eVar5 = %Hotel Name%            → s.eVar5 = "Marriott Marquis Times Square"
     channel = %Page Category%       → s.channel = "accommodation"
  
  2. Adobe Analytics - Send Beacon (Page View)
     → fires s.t() — the Analytics page view hit
```

### Rule Example 2: Booking Confirmation

```
Rule: "Adobe Analytics + AEP - Booking Complete"

EVENT:
  Type: Data Pushed
  Specific Event: booking:confirmed

CONDITIONS:
  Data element %Environment% equals "production"
  (don't fire on dev/staging to keep data clean)

ACTIONS:
  1. Adobe Analytics - Set Variables
     events = "event4,event7"         → booking confirmed + revenue
     eVar1 = %Campaign ID%            → attribution
     products = ";%Hotel ID%;1;%Booking Revenue%"  → commerce variable

  2. Adobe Analytics - Send Beacon (Custom Link)
     → fires s.tl() — a custom event hit (not a page view)

  3. Adobe Experience Platform Web SDK - Send Event
     xdm.commerce.purchases.value = %Booking Revenue%
     xdm.commerce.order.purchaseID = %Booking ID%
     → sends to AEP Edge Network for Real-Time CDP
```

### Rule Example 3: Conditional Personalization Signal

```
Rule: "Target - Hotel View for Loyalty Members"

EVENT:
  Type: Data Pushed
  Specific Event: hotel:viewed

CONDITIONS:
  Data element %User Login Status% equals "authenticated"
  Data element %User Loyalty Tier% is one of ["platinum", "titanium"]

ACTIONS:
  1. Adobe Target - Load Target
     → passes profile attributes to Target for personalization decision
     profileAttribute.loyaltyTier = %User Loyalty Tier%
     profileAttribute.preferredDestination = %Search Destination%
```

---

## 6. Extensions Deep Dive

### Adobe Analytics Extension

```
Configuration (set once at extension level):
  Report Suite IDs: marriott.global, marriott.us
  Tracking Server: marriott.d1.sc.omtrdc.net
  Character Set: UTF-8
  Currency: USD

Rule-level: Set Variables, Send Beacon (s.t() or s.tl())
```

### Adobe Experience Platform Web SDK Extension (alloy.js)

```
Configuration:
  Datastream ID: [your AEP datastream UUID]
  IMS Org ID: [your Adobe Org ID]

Rule action: "Send Event"
  Type: web.webpagedetails.pageViews
  XDM data: mapped from data elements

This single Send Event fires:
  → Adobe Analytics (via server-side Analytics forwarding)
  → Adobe Target (via Target on Edge)
  → AEP Profile ingestion
  → All from one network request instead of three
```

---

## 7. Publishing Workflow

```
Development → Staging → Production

1. Developer/analyst creates rule in Tags UI
2. Save to library → build for Development environment
3. QA: verify with Adobe Experience Platform Debugger
4. Approve → build for Staging
5. Test on staging.marriott.com
6. Approve → build for Production
7. Published → CDN-hosted Tags file updated

Each environment has its own embed code snippet:
<script src="//assets.adobedtm.com/[property-id]/tags.js" async></script>
```

**Embed code** (added once by engineering to every page `<head>`):
```html
<!-- Adobe Tags embed code — installed once, never changes -->
<script src="//assets.adobedtm.com/abc123/launch-xyz.min.js" async></script>
```

After this is deployed, all rule changes happen in the Tags UI — zero code deploys.

---

## 8. Adobe Experience Platform Debugger

The browser extension for validating Tags behavior:

```
Adobe Experience Platform Debugger (Chrome extension):
  → Summary tab: which extensions loaded, which rules fired
  → Analytics tab: shows every Analytics hit with all variables set
  → Target tab: shows A/B test mbox requests and responses
  → Network tab: all beacon URLs
  → Data layer tab: current adobeDataLayer state
  → Events tab: timeline of events fired

Essential for QA of any Tags rule change.
```

---

## 9. Rule Ordering and Conflicts

Multiple rules can fire on the same event. Order matters:

```
Rules firing on "page:loaded":
  Rule 1 (order: 10) → Set global Analytics variables
  Rule 2 (order: 20) → Set page-type-specific variables
  Rule 3 (order: 30) → Send Analytics beacon

Rules execute in order: 10 → 20 → 30
If Rule 2 sets eVar5, it overwrites Rule 1's value for eVar5
The beacon (Rule 3) fires last — captures the final state
```

---

## 10. Tags vs Google Tag Manager (GTM)

| | Adobe Tags | Google Tag Manager |
|---|---|---|
| Native integration | Adobe Analytics, Target, AEP | Google Analytics 4, Ads, Search Console |
| Data layer spec | Adobe Client Data Layer (ACDL) | dataLayer [] (Google spec) |
| Pricing | Included with Adobe Analytics license | Free |
| Governance | Approval workflow built-in | Basic |
| Enterprise features | Multi-property, environments, user roles | Workspaces |
| Best for | Adobe-heavy enterprise stack | Google-heavy or mixed stack |

---

## 11. Common Mistakes

| Mistake | Consequence | Fix |
|---|---|---|
| Publishing directly to production without staging test | Bad tracking fires in prod, corrupts Analytics data | Always use staging environment + Debugger first |
| Too many rules firing on every page view | Performance impact — Tags JS too large, too much work per page load | Consolidate rules; use conditions to narrow scope |
| Hardcoding values in rules instead of data elements | Maintenance nightmare — change one value in 40 rules | Always use data elements; rules reference elements |
| No rule naming convention | Team of 5 creates 200 rules, no one knows what does what | Naming: `[Product] - [Action] - [Page/Scope]` |
| Analytics beacon before variables set | Variables fire in wrong order; eVars empty in hit | Use rule ordering (set vars in lower-number rules, send beacon in higher) |

---

## 12. Interview Insight

Strong answer:

> Adobe Tags is the tag management system that sits between the data layer and all the downstream marketing tools. Data elements extract values from the data layer. Rules wire events to actions — when `hotel:viewed` is pushed, fire Analytics + Target. The publishing workflow has dev → staging → prod environments, with approval gates. The key value is decoupling: once engineering ships the data layer, all future tracking changes can be made by analytics teams in the Tags UI without engineering involvement.

Follow-up trap:

> What is the risk of putting business logic inside an Adobe Tags rule?

Good answer:

> Tags rules are marketing instrumentation — they run asynchronously, can fail silently if an extension errors, and are not under traditional engineering code review or testing discipline. Putting business logic (like price calculation, session management, or auth) in Tags means it can break without triggering alerts, its behavior isn't version-controlled the same way, and it's invisible to your backend systems. Business logic belongs in application code. Tags should only instrument behavior, not drive it.

---

## 13. Revision Notes

- One-line summary: Adobe Tags is the rule engine that listens to data layer events and fires analytics/personalization beacons — with no engineering deploys needed after the embed code is installed.
- Three keywords: data element, rule, publish.
- One interview trap: never put business logic in Tags — it's instrumentation only.
- Memory trick: Tags is the switchboard. Data layer pushes events in; Tags routes them to the right destinations.
