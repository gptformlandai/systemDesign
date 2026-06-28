# Adobe Analytics — Data Collection, Reports, Segments — Gold Sheet

> Topic: Adobe Analytics — the clickstream database that stores every user interaction and turns it into business intelligence

---

## 1. Intuition

Adobe Analytics is like a database that records every click, page view, search, and conversion — with dimensions you define (eVars, props) and metrics you track (events). After collection, analysts query it through Analysis Workspace to understand conversion funnels, user paths, cohort behavior, and attribution.

Beginner version:

> Adobe Analytics is a supercharged event log for websites. It records what users do, slices it any way you want, and shows you where they drop off.

---

## 2. Definition

- **Definition:** Adobe Analytics is a digital analytics platform that collects, processes, stores, and visualizes clickstream data using a flexible schema of dimensions, metrics, and segments.
- **Category:** Digital analytics / clickstream analytics.
- **Core idea:** Every user action is a hit (image request/XHR beacon) containing dimensions and metrics — stored in Adobe's processing pipeline.

---

## 3. The Analytics Data Model

### Three fundamental building blocks:

```
┌─────────────────────────────────────────────────────────┐
│  DIMENSIONS — describe what, who, where                  │
│  eVars (conversion variables): persist across hits       │
│  Props (traffic variables): single-hit only              │
│  Standard dimensions: page name, referrer, browser      │
└─────────────────────────────────────────────────────────┘
┌─────────────────────────────────────────────────────────┐
│  METRICS — count what happened                           │
│  Events (custom): purchase, booking, form submit         │
│  Standard metrics: page views, visits, unique visitors   │
│  Calculated metrics: booking rate = bookings / sessions  │
└─────────────────────────────────────────────────────────┘
┌─────────────────────────────────────────────────────────┐
│  SEGMENTS — filter everything                            │
│  Hit level: this specific page view                      │
│  Visit level: this session                               │
│  Visitor level: this person across all sessions          │
└─────────────────────────────────────────────────────────┘
```

---

## 4. eVars vs Props — The Key Distinction

### eVars (Conversion Variables)

```
Purpose: Track dimensions that contribute to conversions
Persistence: Can persist beyond a single hit (hit, visit, visitor, 30 days, etc.)
Count: eVar1 through eVar250 available
```

Examples:
```
eVar1 = Campaign ID (persists for visit → credit bookings to the campaign)
eVar5 = Hotel Name (persists for 30 days)
eVar10 = Search Destination (expires at visit end)
eVar20 = Loyalty Tier (persists at visitor level)
```

### Props (Traffic Variables)

```
Purpose: Track dimensions for path analysis (pages, links clicked)
Persistence: Single hit only — no persistence
Count: prop1 through prop75 available
Can do: Path analysis (next page, previous page reports)
```

Examples:
```
prop1 = Page Name (single hit — for path analysis)
prop5 = Internal Search Term (single hit)
prop10 = Link Name (click tracking)
```

### The practical difference:

```
Scenario: User searches "New York" → views 3 hotels → books one

eVar tracking (with visit persistence):
  → "New York" search gets credit for the booking
  → You can report: "Search term → booking conversion rate"

Prop tracking:
  → "New York" tracked on the search hit only
  → Cannot credit it to the booking that happened 3 clicks later
```

---

## 5. Events (Custom Metrics)

```
event1 = Hotel View
event2 = Rate Selected
event3 = Booking Started
event4 = Booking Confirmed
event5 = Booking Cancelled
event6 = Loyalty Points Redeemed
event7 = Revenue (numeric event — captures dollar value)
```

Events can be:
- **Counter events** — increment by 1 (event1=1)
- **Numeric events** — capture a value (event7=450.00 for revenue amount)
- **Currency events** — same as numeric but formatted as currency

Implementation via Tags:
```javascript
// In Adobe Tags rule action "Adobe Analytics - Set Variables":
s.events = "event3,event4";
s.eVar1 = "campaign-email-june2026";
s.eVar5 = "Marriott Marquis Times Square";
s.products = ";hotel-product-id;1;450.00"; // special format for commerce
```

---

## 6. The Hit Processing Pipeline

```
Browser fires image request (beacon):
GET /b/ss/{report-suite-id}/1/JS-2.22?
  pageName=hotel-detail&
  v5=Marriott+Marquis+Times+Square&   ← eVar5
  events=event1&                       ← hotel view event
  c1=hotel-detail-page                 ← prop1
  g=https://www.marriott.com/...       ← current page URL

                │
                ▼
Adobe Edge Collection Servers
  → Apply processing rules (transform values)
  → Apply VISTA rules (server-side customizations)
  → Bot filtering
                │
                ▼
Report Suite Database
  → Data stored by report suite ID
  → Retroactive segment application possible
  → Data retention: typically 25 months (configurable)
```

---

## 7. Report Suites

A **Report Suite** is an isolated data store — like a database per environment or region:

```
Global report suite:      marriott.global  ← all markets, all languages
US report suite:          marriott.us
EMEA report suite:        marriott.emea
Mobile app suite:         marriott.ios

Virtual report suite:     marriott.us.luxury  ← filtered view of the global suite
                                                 (no separate data collection)
```

**Multi-suite tagging:** One page hit can be sent to multiple report suites simultaneously:
```javascript
s.account = "marriott.global,marriott.us";  // sends to both
```

---

## 8. Analysis Workspace — The Reporting UI

Analysis Workspace is the drag-and-drop analytics interface. Key capabilities:

### Freeform Table

```
Dimension: Hotel Name (eVar5)
Metric: Bookings (event4), Revenue (event7), Booking Rate (calculated)
Result:
Hotel Name                   | Bookings | Revenue   | Rate
Marriott Marquis Times Sq    | 1,240    | $558,000  | 3.2%
W Hotel Midtown              |   890    | $312,000  | 2.8%
```

### Fallout Report (Funnel)

```
Step 1: Search Results Viewed → 50,000 users
Step 2: Hotel Detail Viewed   → 28,000 users (44% drop)
Step 3: Rate Selected         → 12,000 users (57% drop)
Step 4: Booking Confirmed     →  3,800 users (68% drop)
```

### Flow Report (Path Analysis)

```
What do users do AFTER viewing hotel details?
→ 45%: View another hotel
→ 22%: Go to checkout
→ 18%: Go back to search
→ 15%: Exit site
```

### Cohort Analysis

```
Retention: Of users who booked in January, what % returned in Feb, Mar, Apr?
Jan cohort: 100% → 28% → 19% → 12%
```

---

## 9. Segments

Segments filter any report to a subset of users/visits/hits:

```
Segment: "High-Value Loyalty Members"
Definition: visitor.loyaltyTier = "titanium" OR visitor.loyaltyTier = "platinum"
Scope: Visitor

Segment: "Mobile Bookers"
Definition: hit.deviceType = "mobile" AND hit.event4 = 1 (booking confirmed)
Scope: Visit

Segment: "Abandoned Checkout"
Definition: visit contains event3 (booking started) AND NOT event4 (booking confirmed)
Scope: Visit
```

Apply a segment to any Workspace report to see the filtered view.

---

## 10. Calculated Metrics

```
Booking Rate = Bookings (event4) / Sessions
Revenue per Visit = Revenue (event7) / Sessions
Cart Abandon Rate = 1 - (Bookings / Checkout Started)
```

Calculated metrics appear as new columns in any report without new data collection — computed from existing raw metrics.

---

## 11. Attribution Models

When a user is exposed to multiple marketing touchpoints before converting, which one gets credit?

```
User journey: Email → Organic Search → Paid Ad → Direct → Booking

Last Touch:     Direct gets 100%
First Touch:    Email gets 100%
Linear:         25% each
Time Decay:     Direct gets most, Email gets least
Algorithmic:    ML-based data-driven attribution
```

Attribution is configurable per eVar and per calculated metric in Workspace.

---

## 12. Data Feeds — Raw Data Export

For data science and custom BI:

```
Adobe Analytics Data Feed:
  → Daily or hourly export of all raw hits
  → Delivered to: S3, SFTP, Azure Blob
  → Format: TSV files, one row per hit
  → Columns: all eVars, props, events, timestamps, visitor IDs

Use cases:
  → Power BI / Tableau dashboards from raw data
  → Join with CRM data in Snowflake/Databricks
  → Build custom ML models on booking behavior
```

---

## 13. Common Mistakes

| Mistake | Consequence | Fix |
|---|---|---|
| Using the same eVar slot for two different purposes | Data corruption — reports mix two dimensions | Maintain a variable registry doc; document every eVar/prop |
| eVar expiration too long | Old campaign still gets credit for bookings months later | Set expiration to match the decision window (visit, 30 days) |
| Not setting `pageName` variable | All pages report as URL — hard to filter/segment | Always set `s.pageName` or equivalent data element |
| Sending hits on page exit without sampling | Hit volume spikes, cost increases | Sample exit events or send only meaningful exit signals |
| Segments scoped wrong | Hit-scoped segment on conversion = wrong results | Use visit-scope for funnel analysis; visitor-scope for long-term behavior |

---

## 14. Interview Insight

Strong answer:

> Adobe Analytics stores clickstream data in report suites using a schema of eVars (persistent dimensions), props (single-hit dimensions), and events (metrics). Every user action fires a beacon — a small image request or XHR — with all the variable values set. Analysis Workspace lets analysts drag dimensions and metrics to build funnels, path flows, cohort tables, and attribution reports. The data layer contract ensures engineering populates consistent values across all page types.

Follow-up trap:

> What is the difference between eVar persistence and segment scope?

Good answer:

> eVar persistence controls how long a dimension value sticks to future hits from the same visitor — for example, a campaign ID persists for the visit so it can credit the booking even if the user leaves and comes back. Segment scope controls which container the filter applies to — hit scope filters individual actions, visit scope filters entire sessions, visitor scope filters the entire history of a person. They're independent concepts — persistence affects data storage, scope affects analysis filtering.

---

## 15. Revision Notes

- One-line summary: Adobe Analytics is the clickstream database — eVars are persistent dimensions, props are single-hit dimensions, events are metrics, segments filter everything.
- Three keywords: eVar, event, segment.
- One interview trap: prop vs eVar — props can't credit downstream conversions; eVars can.
- Memory trick: eVar = evidence that persists; prop = a single moment in time.
