# Adobe Target — A/B Testing, Personalization, Recommendations — Gold Sheet

> Topic: Adobe Target — the experimentation and personalization engine that serves different experiences to different users

---

## 1. Intuition

Adobe Target answers one question: "Should THIS user see version A or version B?" It makes that decision in milliseconds using profile data, audience rules, and machine learning — then delivers the right content before the page fully renders. Every hotel chain, airline, and retailer uses some form of A/B testing to optimize conversion. Target is Adobe's enterprise solution for this.

Beginner version:

> Target is the system that decides which version of a page, banner, or offer each user sees — and learns which version works better.

---

## 2. Definition

- **Definition:** Adobe Target is an AI-powered personalization and experimentation platform that delivers targeted content experiences to users based on audience rules, A/B test assignments, and machine learning–driven auto-personalization.
- **Category:** Experimentation / CRO (Conversion Rate Optimization) / Personalization.
- **Core idea:** Define an experience change (text, image, layout, price), define who sees it, measure impact, and learn.

---

## 3. Core Activity Types

```
┌──────────────────────────────────────────────────────────────────┐
│  A/B TEST                                                         │
│  Split traffic randomly between two or more experiences           │
│  Variant A: "Book Now" button (control)                           │
│  Variant B: "Check Availability" button (challenger)              │
│  Goal: Measure which drives more bookings (event4 in Analytics)  │
└──────────────────────────────────────────────────────────────────┘
┌──────────────────────────────────────────────────────────────────┐
│  MULTIVARIATE TEST (MVT)                                          │
│  Test multiple elements simultaneously                            │
│  Headline (3 options) × Hero Image (2 options) × CTA (2 options) │
│  = 12 combinations tested at once                                 │
│  Goal: Find the best combination                                  │
└──────────────────────────────────────────────────────────────────┘
│  EXPERIENCE TARGETING (XT)                                        │
│  Show specific experience to specific audience — no random split  │
│  Rule: Loyalty Tier = Titanium → show suite upgrade offer        │
│  Rule: Geo = IN → show INR pricing and local promotions          │
└──────────────────────────────────────────────────────────────────┘
┌──────────────────────────────────────────────────────────────────┐
│  AUTO-PERSONALIZE (AP)                                            │
│  ML model (Random Forest) assigns each visitor to best experience │
│  No manual audience rules — Target learns from behavior data      │
│  Gets better as it collects more data (exploitation vs exploration)│
└──────────────────────────────────────────────────────────────────┘
┌──────────────────────────────────────────────────────────────────┐
│  RECOMMENDATIONS                                                  │
│  "Hotels similar to what you viewed"                              │
│  "Guests who booked X also booked Y"                             │
│  Algorithm types: Collaborative filtering, Content-based, Popular │
└──────────────────────────────────────────────────────────────────┘
```

---

## 4. How Target Delivers Experiences — The Request Flow

```
1. Page loads → at.js (Target client library) loads in <head>
2. at.js makes a single request to Target Edge servers:
   POST edge.adobedc.net/ee/v1/interact?dataStreamId=...
   Body: {
     visitor ID (ECID),
     current page URL,
     profile attributes from data layer,
     mbox/location name being requested
   }
3. Target server responds in ~30-100ms:
   {
     "handle": [{
       "type": "personalization:decisions",
       "payload": [{
         "scope": "hero-banner",
         "items": [{
           "data": {
             "type": "dom-action",
             "operation": "replace",
             "selector": "#hero-banner h1",
             "content": "Your Titanium Welcome Awaits"
           }
         }]
       }]
     }]
   }
4. at.js applies the DOM change before page renders
5. User sees the personalized experience instantly
```

**Anti-flicker:** Without proper implementation, users briefly see the default content before Target applies changes (the "flicker"). Fix: hide the body until Target responds, then reveal:

```javascript
// In <head>, before at.js loads:
document.documentElement.style.visibility = 'hidden';

// at.js reveals body after Target responds:
// (handled automatically by at.js prehiding snippet)
```

---

## 5. Mboxes / Locations / Scopes

A **location** (called "mbox" in legacy, "scope" in Web SDK) is a named slot on the page where Target can inject content:

```
Global mbox:       target-global-mbox  ← fires on every page
Named locations:   hero-banner         ← homepage hero
                   hotel-detail-promo  ← hotel detail page
                   checkout-upsell     ← checkout step 2
                   search-header       ← search results page top
```

Target decides what to deliver for each location based on active activities.

---

## 6. Audiences — Who Sees What

Audiences are reusable segments used across activities:

```
Built-in audiences:
  Browser: Chrome users
  Device: Mobile visitors
  Geo: Users in India
  New vs Returning: First-time visitors

Profile-based audiences:
  User loyalty tier = "platinum"
  User has viewed > 3 hotels
  User has no prior bookings

Custom audience (from data layer profile attributes):
  profileAttribute.loyaltyTier = "titanium"   ← sent with Target request

AEP audience (shared from Real-Time CDP):
  Segment: "Users who searched but didn't book in 7 days"
  → AEP activates this segment to Target
  → Target creates audience from AEP segment
  → Activity uses AEP audience for targeting
```

---

## 7. Profile Attributes — Persisting User Data in Target

Profile attributes accumulate in Target's visitor profile (stored server-side, keyed by ECID):

```javascript
// Send profile attributes with each Target request (via at.js):
targetPageParams = function() {
  return {
    "profile.loyaltyTier": "titanium",
    "profile.preferredDestination": "New York",
    "profile.totalBookings": 12,
    "profile.averageNightlyRate": 420
  };
};

// These persist across visits (90 days default retention)
// Activity can then target: profile.totalBookings > 5
```

---

## 8. A/B Test Lifecycle

```
1. HYPOTHESIS
   "Changing 'Book Now' to 'Check Availability' will reduce booking hesitation 
    and increase conversion for first-time visitors"

2. DEFINE ACTIVITY IN TARGET UI
   Activity type: A/B Test
   Location: checkout-step-1
   Variants:
     Control (50%): Button = "Book Now"
     Challenger (50%): Button = "Check Availability"
   Audience: New Visitors (first-time visitors)
   Goal metric: event4 (Booking Confirmed) in Analytics
   Secondary metrics: event3 (Booking Started), Revenue

3. STATISTICAL CONFIDENCE SETTING
   Confidence: 95%
   Minimum detectable effect: 5% lift in conversions
   Required sample size: ~8,000 visitors per variant (calculated)

4. RUN
   Target randomly assigns each qualifying new visitor to Control or Challenger
   Data flows to Analytics for reporting

5. ANALYZE
   After 2 weeks (enough traffic for significance):
   Control: 3.2% conversion
   Challenger: 3.8% conversion
   Lift: +18.75%
   Confidence: 97% → statistically significant

6. DECIDE
   Winner: Challenger
   Action: Update application code with winning variant; stop test
```

---

## 9. Recommendations — Collaborative Filtering

```
"Guests who viewed the W Hotel Midtown also viewed:"
  → Marriott Marquis Times Square
  → The New York EDITION
  → W Times Square

How it works:
1. Target collects behavioral data: which hotels each visitor viewed
2. Builds item-to-item collaborative filtering matrix
3. For current hotel, recommends co-viewed hotels

Algorithm options:
  - People Who Viewed This, Viewed That  ← most common
  - People Who Viewed This, Bought That  ← cross-sell
  - People Who Bought This, Bought That  ← upsell
  - Top Sellers by Revenue               ← popularity-based
  - Content Similarity                   ← attribute-based (same city, same stars)

Feed-based Recommendations:
  Upload hotel catalog feed (CSV/JSON) with attributes:
  hotel_id, name, city, stars, price, amenities, image_url
  Target personalizes recommendations using this catalog + behavior
```

---

## 10. Reporting and Stats

```
Target Activity Report:
  Variant   | Visitors | Conversions | Rate  | Lift  | Confidence
  Control   |  10,482  |    335      | 3.20% |  -    |    -
  Challenger|  10,519  |    400      | 3.80% | +18.7%| 97.2% ✓

Lift = (Challenger rate - Control rate) / Control rate × 100
Confidence = statistical certainty that the lift is real, not random noise

Target uses the frequentist approach by default.
Adobe also offers Bayesian statistics mode: shows probability of being best.
```

---

## 11. Auto-Personalize (AP) and Auto-Target

```
Auto-Personalize:
  Offer-level ML: Each visitor gets a different combination of content elements
  Target tries thousands of combinations → Random Forest model learns what works
  Best for: Complex offer combinations, many content variants

Auto-Target:
  Experience-level ML: Each visitor gets the best full experience (A, B, or C)
  Uses: profile attributes, behavioral signals, past conversion patterns
  Best for: 3-5 distinct experience variants where manual rules are too complex
```

---

## 12. Common Mistakes

| Mistake | Consequence | Fix |
|---|---|---|
| Running A/B test for too short a time | Underpowered result — declare winner too early ("peeking problem") | Pre-calculate required sample size; run until target reached |
| Testing too many things at once (not MVT) | Cannot attribute lift to specific change | MVT for multiple elements; A/B for single change |
| Not excluding internal traffic | Employee behavior skews conversion data | IP exclusion rule in Target; or segment out internal IPs in reporting |
| Antiflicker not configured | Visible flicker — users see default then personalized version | Implement at.js prehiding snippet correctly |
| Not QA-ing activity before launch | Wrong variant delivered in production | Use Target QA Links to preview each variant before activating |

---

## 13. Interview Insight

Strong answer:

> Adobe Target is the experimentation and personalization layer. It receives a request from at.js on page load, looks up the visitor's profile and active activities, and returns the experience they should see. For A/B tests it randomly splits traffic and measures lift against Analytics goals. For Experience Targeting, it uses audience rules — like loyalty tier or geo — to serve specific content without randomization. Auto-Target and Auto-Personalize use machine learning to automatically find which experience converts best per visitor segment without manual audience definition.

Follow-up trap:

> Why would an A/B test show a winner early, then the winner underperforms after full rollout?

Good answer:

> This is the "novelty effect" and "peeking problem." Novelty: users engage with something simply because it's new, not because it's genuinely better — the effect fades. Peeking: declaring a winner before statistical significance is reached leads to false positives — random noise looks like a real lift. Solutions: run tests to full required sample size, use guardrail metrics (not just conversion rate), segment results by new vs returning visitors, and plan holdout groups for long-term measurement.

---

## 14. Revision Notes

- One-line summary: Adobe Target decides which experience each user sees — via A/B testing, audience rules, or ML-driven auto-personalization — and delivers it before the page renders.
- Three keywords: mbox, audience, lift.
- One interview trap: declaring A/B winners before reaching required sample size is the peeking problem — always pre-calculate and commit to the sample size.
- Memory trick: Target = the experiment runner. Data layer sends the user's profile; Target decides what they see.
