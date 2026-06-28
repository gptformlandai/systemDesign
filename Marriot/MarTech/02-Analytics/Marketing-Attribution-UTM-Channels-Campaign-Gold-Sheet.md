# Marketing Attribution — UTMs, Channels, Campaign Tracking — Gold Sheet

> Topic: How to track where visitors come from, credit the right marketing channel for bookings, and understand which campaigns actually drive revenue

---

## 1. Intuition

Marriott runs thousands of marketing campaigns — Google Ads, email newsletters, Instagram sponsored posts, affiliate partners, direct bookings. When someone books a hotel, which campaign gets the credit? A user might have clicked a Google Ad three days ago, then received an email, then come back directly and booked. Attribution is the science of distributing credit fairly. Without it, you'd cut the wrong budget and kill the campaigns that actually work.

Beginner version:

> Attribution answers: "Which marketing effort caused this booking?" UTM parameters are the tracking codes that tell Analytics where a visitor came from. Marketing Channels are the buckets (email, paid search, organic, direct) that get the credit.

---

## 2. Definition

- **UTM parameters:** URL query string tags appended to links in campaigns to identify source, medium, and campaign. Parsed by Analytics on landing.
- **Marketing Channel:** A classified category of traffic (Paid Search, Email, Organic Search, Direct, Social) defined by processing rules in Adobe Analytics.
- **Attribution model:** The rule for distributing conversion credit when a user touched multiple channels before converting.
- **Last-touch attribution:** The default — 100% credit to the last channel the user visited before converting.

---

## 3. UTM Parameters — The Tracking Tags

UTM (Urchin Tracking Module) parameters are appended to URLs shared in campaigns:

```
https://www.marriott.com/hotels/new-york?
  utm_source=google          ← WHERE the traffic came from
  utm_medium=cpc             ← HOW it was delivered (paid click)
  utm_campaign=nyc-summer26  ← WHICH campaign
  utm_content=banner-v2      ← WHICH ad variant (for A/B ad testing)
  utm_term=new+york+hotels   ← WHICH keyword (paid search only)
```

**Full UTM taxonomy:**

| Parameter | Question | Examples |
|---|---|---|
| `utm_source` | Which platform/publisher? | google, facebook, newsletter, affiliate-expedia |
| `utm_medium` | How was it delivered? | cpc (paid click), email, social, banner, affiliate |
| `utm_campaign` | Which campaign? | nyc-summer-2026, loyalty-reactivation, black-friday |
| `utm_content` | Which ad/link variant? | hero-banner-v2, cta-button-blue, text-link |
| `utm_term` | Which keyword? (paid search) | "new york marriott", "times square hotel" |

**Where UTMs come from in practice:**
- Google Ads → auto-tagging (`gclid` parameter) or manual UTMs
- Email platform (Marketo/Campaign) → appended by the email sending tool
- Social ads (Meta, LinkedIn) → manual UTMs or auto-tagging
- Affiliate partners → their unique UTM codes
- Internal teams → added manually when creating campaign links

---

## 4. How UTMs Flow Into Adobe Analytics

When a visitor lands on marriott.com with a UTM URL:

```
Step 1: Browser lands on URL with UTMs
  https://www.marriott.com/hotels?utm_source=google&utm_medium=cpc&utm_campaign=nyc-summer26

Step 2: Analytics reads the query parameters on landing
  s.campaign = "nyc-summer26"           ← utm_campaign (the tracking code)
  Standard dimensions auto-populated:
    Referring Domain = google.com
    Original Referrer = https://google.com/...

Step 3: Campaign variable (s.campaign / eVar0) persists
  Default expiration: Visit
  Custom: You can extend to 30 days so bookings 30 days later credit this campaign

Step 4: Marketing Channel processing rules classify the visit
  (See section 5)
```

**In Tags — reading UTM from URL:**

```
Data Element: "UTM Campaign"
  Type: URL parameter
  URL parameter name: utm_campaign
  Result: "nyc-summer26"

Data Element: "UTM Source"
  Type: URL parameter
  URL parameter name: utm_source
  Result: "google"

Rule: "Set Campaign Tracking Code"
  Event: page:loaded (on landing page only)
  Condition: URL contains "utm_campaign"
  Action: Set Analytics variable s.campaign = %UTM Campaign%
```

---

## 5. Marketing Channels — The Attribution Buckets

Marketing Channels are configured in Adobe Analytics under Admin → Report Suites → Marketing Channels. They define processing rules that classify every visit:

```
Marketing Channel Processing Rules (run in order — first match wins):

Rule 1: Internal Traffic
  IF: IP address matches office IPs
  THEN: Channel = "Internal" (exclude from reports)

Rule 2: Paid Search
  IF: Referring domain = google.com OR bing.com
  AND: Query string contains "utm_medium=cpc" OR gclid parameter exists
  THEN: Channel = "Paid Search"
        Channel detail = utm_campaign value

Rule 3: Organic Search
  IF: Referring domain = google.com OR bing.com OR yahoo.com
  AND: No paid search indicators (no gclid, utm_medium ≠ cpc)
  THEN: Channel = "Organic Search"

Rule 4: Email
  IF: utm_medium = "email"
  OR: Referring domain = marketingplatform.google.com
  OR: Query string contains "utm_source=newsletter"
  THEN: Channel = "Email"
        Channel detail = utm_campaign value

Rule 5: Paid Social
  IF: Referring domain = facebook.com OR instagram.com OR linkedin.com
  AND: utm_medium = "cpc" OR "paid-social"
  THEN: Channel = "Paid Social"

Rule 6: Organic Social
  IF: Referring domain = facebook.com OR instagram.com OR twitter.com
  AND: No paid indicators
  THEN: Channel = "Social"

Rule 7: Affiliate
  IF: utm_medium = "affiliate"
  OR: Referring domain = expedia.com OR hotels.com OR booking.com
  THEN: Channel = "Affiliate"

Rule 8: Direct
  IF: No referring domain AND no utm parameters
  THEN: Channel = "Direct"

Rule 9: Other
  IF: Nothing else matched
  THEN: Channel = "Other"
```

---

## 6. Attribution Models — Who Gets the Credit?

When a user touches multiple channels before booking:

```
Day 1: User clicks Google Ad → visits hotel page (Paid Search)
Day 3: User opens email newsletter → visits again (Email)
Day 7: User types marriott.com directly → books hotel (Direct)

Question: Which channel gets credit for the booking?
```

**Attribution models in Adobe Analytics:**

| Model | Credit rule | Day 1 (Paid Search) | Day 3 (Email) | Day 7 (Direct) |
|---|---|---|---|---|
| Last Touch (default) | 100% to last | 0% | 0% | 100% |
| First Touch | 100% to first | 100% | 0% | 0% |
| Linear | Equal split | 33% | 33% | 33% |
| Time Decay | More to recent | 20% | 30% | 50% |
| Participation | 100% to each touched | 100% | 100% | 100% |
| Algorithmic | ML-driven | Based on historical patterns |

**Where to apply attribution models:**

```
1. eVar expiration setting (collection-time, per-report-suite)
   eVar1 (Campaign) expiration = 30 days
   → Campaign variable "nyc-summer26" persists 30 days
   → Booking on day 7 credits "nyc-summer26" as last campaign
   (This is last-touch within the persistence window)

2. Analysis Workspace — Attribution IQ (retroactive)
   Drag any metric onto a calculated metric
   Apply any model: Last Touch, First Touch, Linear, etc.
   This is retroactive — no new data collection needed
   
3. Adobe Analytics Attribution panel
   Compare multiple models side-by-side on the same report
```

---

## 7. Campaign Classification with SAINT / Import

Campaign codes are often alphanumeric (e.g., `EM-NYC-JUN26-GOLD`). SAINT (SiteCatalyst Attribute Importing and Naming Tool) lets you upload a lookup table that adds descriptive dimensions:

```
Upload file:
  Tracking Code  | Campaign Name          | Channel  | Target Audience
  ───────────────────────────────────────────────────────────────────
  EM-NYC-JUN26-G | NYC Summer 2026 Email  | Email    | Gold Tier Members
  GG-NYC-CPC-001 | NYC Google CPC - Brand | Paid SEM | All
  FB-NYC-RET-002 | NYC Facebook Retarget  | Paid Social | Previous Visitors

Result in Analysis Workspace:
  Instead of seeing: "EM-NYC-JUN26-G" in reports
  You see: "NYC Summer 2026 Email" with all classification columns available
```

---

## 8. Multi-Touch Attribution — The Complete Picture

Single-touch models (first/last) are simple but misleading. The reality:

```
Complete user journey before booking:
  Touch 1 (Day -14): Organic search → hotel detail page
  Touch 2 (Day -12): Paid social retargeting → home page
  Touch 3 (Day -8):  Email newsletter → promotion page
  Touch 4 (Day -3):  Google ad (brand keyword) → search results
  Touch 5 (Day  0):  Direct (typed URL) → booking completed

Last-touch says: Direct got all credit (misleading — direct visits are often from memory of earlier ads)
First-touch says: Organic Search started the journey (credit for awareness)
Linear says: all 5 channels equally contributed
Time-decay says: recent touches (Google ad, direct) get more credit
Algorithmic: analyzes thousands of journeys to find which channels truly influence conversion
```

**Customer Journey Analytics (CJA) is the tool to analyze this properly** — see the CJA gold sheet for cross-channel journey analysis.

---

## 9. Tracking Booking Conversion in Analytics

A booking event in Analytics:

```javascript
// Tags Rule: "Booking Confirmed"
// Fires when data layer event "booking:confirmed" is pushed

// Adobe Analytics variables set in this rule:
s.pageName = "booking-confirmation";
s.eVar1    = cookieRead("s_campaign") || "direct"; // Campaign that gets credit
s.eVar5    = "Marriott Marquis Times Square";       // Hotel booked
s.eVar20   = "titanium";                            // Loyalty tier
s.events   = "event4,event7=" + revenue;           // event4=booking, event7=revenue
s.products = ";NYCMQ-DELUXE-KING;1;" + revenue;   // Products string
s.purchaseID = "B-98765";                          // Prevent duplicate booking counting
```

**`purchaseID` is critical:** Without it, if the user refreshes the confirmation page, Analytics records the booking twice. `purchaseID` deduplicates — the same purchase ID is only counted once per visit.

---

## 10. Common Mistakes

| Mistake | Consequence | Fix |
|---|---|---|
| Not appending UTMs to all campaign links | All campaign traffic appears as "Direct" — impossible to attribute | Mandate UTMs on all paid/email/social links; use UTM builder tool |
| UTM parameter names misspelled (UTM_Source vs utm_source) | Analytics reads zero values; wrong channel classification | Always lowercase; validate with browser DevTools before launch |
| Not setting `s.purchaseID` on confirmation page | Refresh = duplicate booking counted; revenue inflated | Always set purchaseID = booking confirmation number |
| Marketing channel rule order wrong | Paid Search classified as Organic because organic rule runs first | Always put paid indicators before organic in rule order |
| eVar campaign expiration too short (visit only) | 7-day email campaign gets 0 booking credit if user takes a week to decide | Set eVar1 (campaign) expiration to 30-90 days per business requirement |

---

## 11. Interview Insight

Strong answer:

> UTM parameters in campaign URLs tell Analytics where traffic came from: source (google, newsletter), medium (cpc, email), and campaign name. When a visitor lands, Tags reads these UTMs and sets `s.campaign`, which persists in an eVar for the configured expiration period. Marketing Channel processing rules classify every visit into a channel bucket — paid search, email, organic, direct — based on referring domain and UTM values. The processing rules run top-to-bottom; the first match wins. Attribution models determine which channel gets booking credit when a user touched multiple channels — last-touch is the default but misleads when a user researched for weeks across many channels.

Follow-up trap:

> A user landed from a Google Ad 3 weeks ago, then returned directly today and booked. Which channel gets credit for the booking in last-touch attribution?

Good answer:

> In last-touch attribution, "Direct" gets the credit — because the last visit before booking was direct (typed the URL). This is why last-touch understates the value of top-of-funnel channels like paid search and display advertising. The better answer for a hotel business is to use a 30-day eVar expiration for the campaign variable, or apply time-decay attribution in Analysis Workspace, so the Google Ad still gets partial credit as long as the booking happened within the eVar's window.

---

## 12. Revision Notes

- One-line summary: UTMs tag campaign URLs so Analytics knows the source/medium/campaign; Marketing Channel processing rules classify visits into buckets; attribution models decide which channel credits the booking when a user touched many channels.
- Three keywords: UTM parameters, Marketing Channels, last-touch attribution.
- One interview trap: without `s.purchaseID`, page refreshes on the confirmation page double-count bookings and inflate revenue.
- Memory trick: UTM = label on the door (who sent this visitor). Marketing Channel = which room they entered. Attribution = who gets the trophy for the sale.
