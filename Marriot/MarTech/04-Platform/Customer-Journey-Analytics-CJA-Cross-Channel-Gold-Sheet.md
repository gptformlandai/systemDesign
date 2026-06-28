# Customer Journey Analytics (CJA) — Cross-Channel Analysis — Gold Sheet

> Topic: Customer Journey Analytics — the advanced analytics tool that stitches every channel into one view and answers questions Adobe Analytics cannot

---

## 1. Intuition

Adobe Analytics is excellent for web clickstream — but it only sees what happens on the website. A Marriott guest might search on the website, call the contact center, book via the app, stay at the property, and tweet about their experience. Each of those channels has data in a different system. Customer Journey Analytics (CJA) pulls all five channels into AEP, stitches them into one unified customer journey per person, and lets you query the complete picture — from first website visit to post-stay review — in one analysis workspace.

Beginner version:

> Adobe Analytics only sees the website. CJA sees everything — web, app, call center, loyalty, in-property, email — stitched together per person, so you can analyze the complete guest journey from first click to checkout.

---

## 2. Definition

- **Customer Journey Analytics (CJA):** An Adobe application built on AEP that enables cross-channel journey analysis using any data in AEP's data lake, with person-level stitching across channels.
- **Connection:** A CJA configuration that links AEP datasets to CJA for analysis (replaces the concept of a Report Suite).
- **Data View:** A virtual configuration layer in CJA that defines how dataset fields map to dimensions and metrics (like a virtual report suite, but more flexible).
- **Stitching:** The process of connecting anonymous and authenticated events into one unified person-level journey using identity resolution.

---

## 3. Adobe Analytics vs CJA — The Key Differences

| | Adobe Analytics | Customer Journey Analytics |
|---|---|---|
| **Data source** | Web clickstream only | Any AEP dataset (web, app, CRM, call center, in-property, email, loyalty) |
| **Schema** | eVars / props / events (proprietary) | Any XDM schema field or custom field |
| **Identity** | Visitor ID (cookie-based) | Person-level stitching across anonymous + authenticated |
| **Cross-channel** | Limited (cannot join call center to web data) | Native — every channel stitched into one person journey |
| **Data manipulation** | Limited field transformations | Full field derivations at Data View level (no re-collection) |
| **Lookback window** | Visit / visitor scope only | Any time window (query 3 years of events per person) |
| **SQL access** | Not available | AEP Query Service (SQL) on the same data |
| **Latency** | ~30 minutes for processing | ~15 minutes for streaming datasets |

---

## 4. CJA Data Architecture

```
DATA SOURCES (all in AEP)
  ├── Adobe Web SDK events (web interactions) → AEP Dataset: "Marriott Web Events"
  ├── Mobile SDK events (app interactions)    → AEP Dataset: "Marriott App Events"
  ├── CRM export (loyalty, bookings)          → AEP Dataset: "Marriott CRM Bookings"
  ├── Call center logs (Genesys export)       → AEP Dataset: "Marriott Call Center"
  ├── AJO email events (sent, opened, clicked)→ AEP Dataset: "AJO Email Events"
  └── Property system (check-in, checkout)   → AEP Dataset: "Property Events"
          │
          ▼
CJA CONNECTION: "Marriott Full Journey"
  Includes all 6 datasets above
  Identity field: loyaltyId (primary person ID)
  
          │
          ▼
CJA STITCHING
  Anonymous web visit (ECID: aaaa-1111)
      +
  Call center call (phone: +91-98765, loyaltyId: MR-GOLD-789)
      +
  App booking (loyaltyId: MR-GOLD-789)
  
  → All three stitched to one person: loyaltyId = MR-GOLD-789

          │
          ▼
CJA DATA VIEW: "Marriott Guest Experience"
  Dimensions:
    Channel (derived from dataset source)
    Hotel Name (from CRM dataset)
    Page Name (from web dataset)
    Call Reason (from call center dataset)
    Loyalty Tier (from CRM dataset)
  Metrics:
    Page Views (count of web events)
    Bookings (count of purchase events across web + app)
    Calls (count of call center events)
    Revenue (sum of booking amounts)

          │
          ▼
CJA ANALYSIS WORKSPACE (identical UI to Adobe Analytics Workspace)
```

---

## 5. Cross-Channel Analysis — What's Now Possible

### Example 1: Call Center → Web Correlation

**Question:** "Do guests who call the contact center before booking convert at a higher rate than those who book directly online?"

```
CJA Analysis:
  Segment A: Persons who had at least 1 call center event → then booked
  Segment B: Persons who booked with no call center event

  Metric: Booking Conversion Rate, Average Order Value

  Result:
  Guests who called first:  Conversion = 45%, AOV = $620
  Direct online only:        Conversion = 12%, AOV = $380

  Insight: Contact center interactions strongly correlate with higher-value bookings.
  Action: Invest in contact center capacity; train agents as upsellers.
```

### Example 2: Full Funnel — From Email Click to In-Property

```
Cross-Channel Fallout:
  Step 1: Email opened (AJO event)                    → 120,000 guests
  Step 2: Website visited within 48h                  →  45,000 guests (62% drop)
  Step 3: Hotel detail page viewed                    →  28,000 guests (38% drop)
  Step 4: Booking completed (web or app)              →   8,400 guests (70% drop)
  Step 5: Property check-in (property system event)  →   7,900 guests (6% drop — no-shows)
  Step 6: Post-stay email survey responded            →   3,100 guests (61% drop)

  Insight: 62% of email clickers don't visit the site → subject line or landing page problem
  Action: A/B test email subject lines; optimize landing page.
```

### Example 3: Journey Visualization — How Guests Actually Navigate

```
Journey Map (Flow Report in CJA):
  Starting from: Email Clicked
  
  Email Clicked → Hotel Search Results [32%] → Hotel Detail [28%] → Booking ✓
  Email Clicked → Home Page [25%] → Loyalty Dashboard [18%] → Hotel Search → ...
  Email Clicked → Promo Landing Page [43%] → Hotel Detail [38%] → Booking ✓
  
  Insight: Direct email → promo landing page → hotel detail → book
           has 40% higher conversion than email → home page
  Action: Always link email CTAs to the relevant promo page, not the home page.
```

---

## 6. Derived Fields — Transforming Data at Analysis Time

CJA lets you create derived fields in the Data View — no re-collection needed:

```
Derived Field: "Channel Group"
  IF dataset is "Marriott Web Events" AND source = "email" → "Email"
  IF dataset is "Marriott Web Events" AND source = "google/cpc" → "Paid Search"
  IF dataset is "Marriott App Events" → "Mobile App"
  IF dataset is "Marriott Call Center" → "Contact Center"
  IF dataset is "Property Events" → "In-Property"
  ELSE → "Other"
```

```
Derived Field: "Days Until Check-in at First Touch"
  = checkInDate - firstWebVisitDate
  → Lets you analyze: "How far in advance do different loyalty tiers book?"
```

---

## 7. Stitching — How Cross-Channel Person Identification Works

CJA has two stitching methods:

**Field-based stitching (legacy, simpler):**
```
Take a "persistent ID" (ECID from cookie) and a "transient ID" (loyalty ID from login)
When they appear together in an event → link them
Backfill: up to 60 days back, retroactively associate past ECID events to loyaltyId
```

**Graph-based stitching (modern — uses AEP Identity Graph):**
```
Use the AEP Identity Graph (which already links ECID ↔ loyaltyId ↔ email ↔ phone)
CJA queries the identity graph to resolve person identity
More accurate — uses all available identity links, not just co-occurrence in events
```

---

## 8. Analysis Workspace in CJA

CJA's Analysis Workspace is identical to Adobe Analytics Workspace but with cross-channel data. Key additions:

```
Panels:
  ├── Freeform Table (same as Analytics)
  ├── Fallout (multi-channel — steps can span channels)
  ├── Flow (person-level navigation paths across channels)
  ├── Attribution (all models, including algorithmic)
  ├── Cohort Table (same as Analytics)
  └── Experimentation Panel (A/B test analysis natively in CJA)
         → Reads Target or AJO experiment data
         → Shows lift, confidence, statistical significance per metric
```

---

## 9. CJA + AJO Integration — Closing the Loop

```
AJO sends a journey email to 100,000 profiles
  ↓
AJO email events flow into AEP (sent, opened, clicked, bounced, unsubscribed)
  ↓
CJA Connection includes AJO email dataset + web dataset + booking dataset
  ↓
CJA Analysis:
  Email opened [55%] → website visit [32%] → booking [8%]
  Email not opened [45%] → website visit [5%] → booking [1%]
  
  → Email drives 8x higher booking conversion for engaged recipients
  → Insight: invest in email subject line optimization (drives open rate)
```

---

## 10. Common Mistakes

| Mistake | Consequence | Fix |
|---|---|---|
| Using CJA for real-time/operational reporting | CJA has 15-minute latency — not for live dashboards | Use AEP Dashboards or real-time APIs for live monitoring |
| No stitching configured | All cross-channel data appears as separate anonymous visitors — no person-level view | Configure field-based or graph-based stitching with a persistent + transient ID |
| Putting raw Analytics data into CJA without XDM | Schema mismatch; eVar fields don't map naturally to XDM dimensions | Use the Analytics Data Connector to transform Report Suite data into CJA connection |
| Too many Data Views | Governance problem — 20 teams each build a data view differently | Standardize 2-3 Data Views: executive, analyst, engineering |
| Forgetting consent in CJA datasets | If a user opted out of analytics, their data should be excluded from CJA queries too | Apply AEP Data Governance labels to datasets; CJA respects governance policies |

---

## 11. Interview Insight

Strong answer:

> Customer Journey Analytics is built on top of AEP and solves the core limitation of Adobe Analytics: it only sees web. CJA ingests any AEP dataset — web, mobile, CRM, call center, email (AJO), and in-property systems — stitches them to a single person via the AEP Identity Graph, and makes the complete journey queryable in Analysis Workspace. You can do cross-channel fallout analysis — "what percentage of email openers visited the site, viewed a hotel, and then booked?" — which is impossible in Adobe Analytics because it only has web data. The Experimentation panel reads Target and AJO experiment results so you can measure lift on any metric, including booking revenue, not just click-through.

Follow-up trap:

> Can CJA replace Adobe Analytics entirely?

Good answer:

> Not yet for most organizations — and not the goal. Adobe Analytics has a 30-minute processing latency, extensive bot filtering, VISTA rules, and decades of tuned report suites. CJA has 15-minute latency and is better for exploration and cross-channel questions. In practice they coexist: Analytics for standard web reporting, conversion tracking, and real-time monitoring; CJA for journey analysis, attribution modeling, and any question that requires data from non-web channels. Adobe's roadmap does trend toward CJA as the primary analytics surface, with Analytics as the collection layer.

---

## 12. Revision Notes

- One-line summary: CJA ingests all AEP channel datasets into one stitched person-level workspace — enabling cross-channel fallout, journey flows, and attribution that Adobe Analytics cannot do because it's web-only.
- Three keywords: cross-channel stitching, Connection + Data View, person-level journey.
- One interview trap: CJA is not real-time — 15-minute latency; don't use it for operational dashboards.
- Memory trick: Analytics = web microscope (high detail, one channel). CJA = telescope (full picture, all channels, less granular per-hit detail).
