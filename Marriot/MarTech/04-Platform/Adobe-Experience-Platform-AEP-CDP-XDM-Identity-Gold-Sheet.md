# Adobe Experience Platform (AEP) — CDP, XDM, Identity, Segments — Gold Sheet

> Topic: AEP — the unified customer data platform that stitches every touchpoint into one real-time profile and activates audiences everywhere

---

## 1. Intuition

Imagine a hotel guest who visited the website three times anonymously, then booked using their loyalty ID, then called the contact center, then stayed at the property. Five separate systems know a piece of this story. AEP is the platform that stitches all five pieces together into one unified customer profile — in real time — and uses that complete picture to activate personalized experiences across every channel.

Beginner version:

> AEP is the single source of truth about every customer across every channel — web, mobile, call center, in-property, email — all stitched together and ready to activate.

---

## 2. Definition

- **Definition:** Adobe Experience Platform (AEP) is an enterprise cloud platform that ingests data from any source, builds unified real-time customer profiles using identity resolution, computes audience segments, and activates those segments to downstream destinations.
- **Category:** Customer Data Platform (CDP) / Unified Data Platform.
- **Core idea:** Ingest → unify → segment → activate.

---

## 3. AEP Architecture

```
DATA SOURCES (Ingest)
   ├── Web (Tags / Web SDK)          → streaming ingestion
   ├── Mobile (iOS/Android SDK)      → streaming ingestion
   ├── CRM (Salesforce, Dynamics)    → batch ingestion
   ├── Call Center (Genesys, NICE)   → batch/streaming
   ├── Email (Marketo, Campaign)     → batch ingestion
   ├── Point of Sale / Property      → batch ingestion
   └── Third-party (data providers)  → batch ingestion
                │
                ▼
        XDM (Experience Data Model)
        Standardize all data to a common schema
                │
                ▼
        DATASETS
        Storage layer (data lake on Azure/AWS)
                │
                ▼
        IDENTITY GRAPH
        Stitch ECID + email + loyalty ID + phone → one person
                │
                ▼
        REAL-TIME CUSTOMER PROFILE
        Merged profile per person: all events + attributes
                │
                ▼
        SEGMENTS / AUDIENCES
        Compute who qualifies for which audience
                │
                ▼
DESTINATIONS (Activate)
   ├── Adobe Target                  → personalized web/app experiences
   ├── Adobe Campaign / Journey Opt  → triggered email/push campaigns
   ├── Google Ads / DV360           → paid media targeting
   ├── Meta (Facebook/Instagram)    → paid social targeting
   ├── Snowflake / Azure Synapse    → analytics/BI
   └── Custom HTTP destination      → any system via webhook
```

---

## 4. XDM — Experience Data Model

XDM is Adobe's standardized JSON schema for all customer experience data. Every dataset in AEP must conform to an XDM schema.

```json
// XDM ExperienceEvent schema — one hotel view event
{
  "_id": "uuid-abc-123",
  "timestamp": "2026-06-28T14:23:00Z",
  "eventType": "web.webpagedetails.pageViews",

  "web": {
    "webPageDetails": {
      "name": "hotel-detail",
      "URL": "https://www.marriott.com/hotels/nycmq-marriott-marquis"
    }
  },

  "commerce": {
    "productViews": { "value": 1 }
  },

  "productListItems": [{
    "SKU": "NYCMQ-DELUXE-KING",
    "name": "Marriott Marquis Times Square - Deluxe King",
    "priceTotal": 450.00,
    "quantity": 1
  }],

  "identityMap": {
    "ECID": [{"id": "12345678901234567890", "primary": true}],
    "loyaltyId": [{"id": "MR-GOLD-7890123"}]
  },

  "_marriott": {
    "geo": {
      "country": "IN",
      "state": "TG"
    },
    "loyalty": {
      "tier": "gold",
      "points": 45000
    }
  }
}
```

**Field groups:** XDM is modular — you mix field groups:
- `Web Details` (page name, URL, referrer)
- `Commerce Details` (cart, orders, products)
- `Person` (name, email, phone)
- `Loyalty` (tier, points — custom field group)
- `Geo` (country, state, city)

---

## 5. Identity Resolution — The Identity Graph

The hardest problem in MarTech: the same person visits from three devices and has five IDs.

```
Visit 1 (anonymous, desktop):  ECID = aaaa-1111
Visit 2 (logged in, desktop):  ECID = aaaa-1111 + loyaltyId = MR-GOLD-789
Visit 3 (mobile app):          device ID + loyaltyId = MR-GOLD-789
Email click:                   email hash (SHA256) + loyaltyId = MR-GOLD-789
Call center:                   phone = +91-98765-43210 + loyaltyId = MR-GOLD-789

AEP Identity Graph links:
  aaaa-1111 ←→ MR-GOLD-789 ←→ device-id-bbb ←→ email-hash-ccc ←→ phone-ddd

Unified profile: one person, all five touchpoints
```

**Identity namespaces:**

| Namespace | Symbol | Type | Example |
|---|---|---|---|
| Experience Cloud ID | ECID | Cookie | 12345678901234567890 |
| Email | Email | PII | hashed: abc123def456 |
| Phone | Phone | PII | hashed |
| Loyalty ID | loyaltyId | Cross-device | MR-GOLD-789 |
| CRM ID | CRMID | Cross-device | SF-CONTACT-456 |
| Google GCLID | GCLID | Ad click | CjwKCAj... |

**Important:** PII (email, phone) must be hashed before sending to AEP for privacy compliance.

---

## 6. Real-Time Customer Profile

The unified profile has two layers:

```
PROFILE ATTRIBUTES (slowly changing)
  name: "Aravind M."
  loyaltyTier: "gold"
  totalLifetimeBookings: 12
  preferredProperty: "NYCMQ"
  memberSince: "2019-03-15"

EXPERIENCE EVENTS (time-series, all events)
  [2026-06-28] viewed Marriott Marquis detail page
  [2026-06-25] searched "New York" 3 nights
  [2026-06-20] opened email: "Summer Deals"
  [2026-05-15] completed booking at W Midtown
  [2026-04-02] called contact center re: points redemption
  ... (all events retained per data retention policy)
```

Profile lookup latency: **<100ms** for real-time personalization (Target, web SDK).

---

## 7. Audience Segments — Segment Builder

Segments are rules that define who belongs to an audience:

```
Segment: "High-Value Lapsed Bookers"
Rules:
  AND all of:
    - loyaltyTier IN ["platinum", "titanium"]           (profile attribute)
    - totalLifetimeBookings >= 5                        (profile attribute)
    - eventType = "commerce.purchases" occurred         (experience event)
      in last 365 days
    - eventType = "commerce.purchases" NOT occurred     (experience event)
      in last 90 days

Audience size: estimated 42,000 profiles
Evaluation type: Batch (updated every 24h)
```

**Evaluation types:**

| Type | Latency | Use case |
|---|---|---|
| Batch segmentation | 24h refresh | Email campaigns, large audiences |
| Streaming segmentation | Real-time (<5 min) | Trigger email on cart abandon immediately |
| Edge segmentation | <100ms | Used by Target for real-time personalization on page load |

---

## 8. Destinations — Activating Audiences

```
Segment: "High-Value Lapsed Bookers"
  │
  ├── Adobe Target           → Show re-engagement offer on website
  │     (edge segmentation → real-time on page load)
  │
  ├── Adobe Journey Optimizer → Send personalized email + push sequence
  │     (streaming segmentation → email triggers within minutes)
  │
  ├── Google Ads (DV360)     → Suppress from paid ads (they're loyalty members)
  │     (batch → daily sync)
  │
  └── Meta Ads               → Lookalike audience (find users similar to lapsed bookers)
        (batch → daily sync)
```

Activation removes the need for each channel to maintain its own segment logic — define once in AEP, activate everywhere.

---

## 9. Data Governance — Labels and Policies

AEP has built-in data governance to prevent misuse:

```
Data Labels (applied per field):
  C1 = Cannot be exported to third parties
  C2 = Cannot be used for targeting
  C5 = Cannot be used for onsite personalization
  I1 = Directly identifiable information (name, email)
  I2 = Indirectly identifiable (device ID)
  S1 = Sensitive — exact geo location
  S2 = Sensitive — financial data

Policy enforcement example:
  Field: user.email (labeled I1 + C1)
  Destination: Google Ads
  Policy: I1 + C1 fields CANNOT be exported to third-party destinations
  Result: AEP blocks this export with policy violation error
```

This prevents GDPR/CCPA violations from human error.

---

## 10. AEP Services Landscape

```
AEP Core Services:
  ├── Data Ingestion (batch + streaming)
  ├── Data Lake (query service — SQL on raw data)
  ├── Identity Service (identity graph)
  ├── Real-Time Customer Profile
  ├── Segmentation Service
  └── Destinations

AEP Applications (built on AEP):
  ├── Real-Time CDP          → B2C customer data platform
  ├── Real-Time CDP B2B      → Account-based marketing
  ├── Customer Journey Analytics (CJA) → stitched cross-channel analytics
  ├── Adobe Journey Optimizer (AJO) → omnichannel journey orchestration
  └── Mix Modeler           → marketing mix modeling / attribution
```

---

## 11. Common Mistakes

| Mistake | Consequence | Fix |
|---|---|---|
| Sending raw PII to AEP without hashing | GDPR violation; AEP stores unencrypted email/phone | Hash before ingestion; use SHA256 consistently |
| No data governance labels | Teams freely export sensitive fields to ad platforms | Apply governance labels during schema creation |
| Using batch segmentation for real-time use cases | Audience 24h stale — Target serves yesterday's segment | Use streaming or edge segmentation for real-time activation |
| Schema proliferation (different schema per team) | Profiles from two teams can't be merged — different identity fields | Enforce a single canonical XDM schema per entity |
| Identity graph too aggressive (wrong stitching) | Household merging — all family members get one profile | Tune identity priority and namespace strength carefully |

---

## 12. Interview Insight

Strong answer:

> AEP is the unified data platform that ingests event streams and profile data from web, mobile, CRM, and call center — all standardized to XDM schemas. The identity graph stitches together anonymous cookies, loyalty IDs, hashed emails, and device IDs into a single unified profile. The segmentation service computes audiences using batch (daily), streaming (near real-time), or edge (sub-100ms) evaluation. Activated audiences flow to destinations — Target for web personalization, Journey Optimizer for email/push, Google/Meta for paid media. The data governance layer enforces policies to prevent PII from reaching destinations that aren't allowed to use it.

Follow-up trap:

> What is the difference between AEP and Adobe Analytics?

Good answer:

> Adobe Analytics is a clickstream analytics system — it collects web events, stores them in report suites (its own proprietary schema), and provides Analysis Workspace for reporting. AEP is a general-purpose customer data platform — it ingests from any source, stores in a common XDM data lake, builds unified cross-channel profiles with identity resolution, and activates to any destination. They serve different purposes and often run together: Analytics is the reporting tool, AEP is the data activation platform. Customer Journey Analytics (CJA) bridges them — it's an Analytics-style workspace that queries AEP data.

---

## 13. Revision Notes

- One-line summary: AEP ingests all customer data into XDM schemas, resolves identities across touchpoints into unified profiles, computes audience segments, and activates them to Target, email, and paid media.
- Three keywords: XDM, identity graph, activation.
- One interview trap: AEP ≠ Adobe Analytics — they coexist; AEP is a CDP, Analytics is a clickstream reporting tool.
- Memory trick: AEP is the spine. All channels plug into it; all audiences come out of it.
