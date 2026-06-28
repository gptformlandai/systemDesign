# MarTech Debugging and Testing Tools — Gold Sheet

> Topic: The complete toolkit for finding and fixing MarTech problems — from browser DevTools to Adobe Assurance to proxy tools

---

## 1. Intuition

A Tags rule isn't firing. Analytics shows 0 bookings. Target personalization isn't appearing. How do you debug it? Unlike backend bugs where you read server logs, MarTech bugs live in the browser, in network requests, and in data layer pushes — invisible unless you know where to look. This guide covers every tool, from free browser built-ins to Adobe-specific debuggers, and how to apply them systematically.

Beginner version:

> MarTech debugging = follow the event from data layer push → Tags rule → network beacon → Adobe server. Each step has a tool that lets you see what's happening. Learn the tools, and no bug stays hidden.

---

## 2. The Debugging Chain

```
Data Layer Push (engineering)
    │
    ├── TOOL: Browser Console / DevTools (Sources tab)
    │         Verify: adobeDataLayer push happened with correct data
    │
    ▼
Tags Rule Fires
    │
    ├── TOOL: Adobe Experience Platform Debugger (browser extension)
    │         Verify: Rule triggered, correct data elements resolved
    │
    ▼
Network Request (beacon / Web SDK sendEvent)
    │
    ├── TOOL: Browser DevTools — Network tab
    │         Verify: Request made, correct payload, HTTP 200 response
    │
    ▼
Adobe Processing (Analytics / Target / AEP)
    │
    ├── TOOL: Adobe Debugger → Analytics/Target tabs
    │         Verify: Correct eVars/events, Target decisions returned
    │
    ▼
Data Appears in Report Suite / AEP
    │
    ├── TOOL: Adobe Analytics Real-Time Report
    │         Verify: Hit arrived and processed (30 min latency for standard)
    │
    └── TOOL: AEP Query Service (SQL)
              Verify: Event landed in AEP dataset
```

---

## 3. Tool 1 — Browser DevTools (Built-In, Always Available)

Every browser has DevTools. Press `F12` or `Cmd+Option+I` on Mac.

### Console Tab — Inspect the Data Layer

```javascript
// Paste in browser console on any Marriott page:

// 1. See the current data layer state
window.adobeDataLayer.getState()

// 2. See all events that have been pushed (raw array)
window.adobeDataLayer

// 3. Watch future pushes in real time
window.adobeDataLayer.addEventListener("adobeDataLayer:change", (event) => {
  console.log("New push:", JSON.stringify(event.detail, null, 2));
});

// 4. Manually push a test event to trigger a Tags rule (QA)
window.adobeDataLayer.push({ event: "hotel:viewed", hotel: { code: "TEST", name: "Test Hotel" } });
```

### Network Tab — Inspect Beacons

```
Open Network tab → filter by:

For Web SDK (alloy.js):
  Filter: "interact" or "collect"
  Look for: POST edge.adobedc.net/ee/v2/interact?datastreamId=...
  Click the request → Payload tab → see full XDM body

For Legacy Analytics (AppMeasurement):
  Filter: "b/ss" 
  Look for: GET /b/ss/{report-suite-id}/1/JS-...?pageName=...&events=...
  Click → Headers → see all Analytics variables as query params

For Target (legacy at.js):
  Filter: "mbox" or "interact"
  Look for: POST mboxedge##.tt.omtrdc.net/rest/v1/delivery

For Meta Pixel (if client-side):
  Filter: "facebook"
  Look for: POST https://www.facebook.com/tr/?...

Check:
  ✓ HTTP Status = 200 (not 400/500)
  ✓ Payload contains expected fields
  ✓ Response body (for Web SDK — contains Target decisions, ECID confirmation)
```

### Application Tab — Inspect Cookies

```
Application tab → Cookies → https://www.marriott.com

Key cookies to check:
  s_ecid    → ECID is set (Web SDK is working; identity persisting)
  AMCV_xxx  → Legacy ECID cookie (AppMeasurement)
  mbox      → Target session cookie (at.js present)
  OptanonConsent → OneTrust consent cookie (what categories user accepted)
  _gcl_aw   → Google Click ID (gclid captured in first-party cookie)
```

---

## 4. Tool 2 — Adobe Experience Platform Debugger (Primary Tool)

**Install:** Chrome Web Store → "Adobe Experience Platform Debugger" (free, by Adobe)

This is the #1 tool for MarTech debugging on the web.

```
After installing and opening Debugger on marriott.com:

SUMMARY TAB:
  Shows all Adobe products detected on the page:
  ├── Adobe Tags: Property name, version, environment (dev/staging/prod)
  ├── Adobe Analytics: AppMeasurement version, report suite IDs
  ├── Adobe Target: at.js version, client code
  └── Web SDK: alloy.js version, DataStream IDs detected

ANALYTICS TAB:
  Every Analytics hit shown in real time as it fires:
  ┌────────────────────────────────────────────────────────┐
  │ Hit #1 — page view                                      │
  │  pageName: hotel-detail                                 │
  │  eVar5: Marriott Marquis Times Square                  │
  │  events: event1                                         │
  │  rsid: marriott.global                                  │
  │  vid: 1234567890 (visitor ID / ECID)                   │
  └────────────────────────────────────────────────────────┘
  
  Use to verify:
  ✓ Correct pageName set
  ✓ Correct eVars populated
  ✓ Correct events firing
  ✓ Correct report suite receiving data

TARGET TAB:
  Every Target request/response shown:
  ┌────────────────────────────────────────────────────────┐
  │ Request to: mboxedge35.tt.omtrdc.net                   │
  │   Mbox: hero-banner, hotel-search-results              │
  │   Profile: { loyaltyTier: "titanium" }                 │
  │ Response:                                               │
  │   hero-banner: "Experience B — Suite Upgrade Offer"    │
  │   Offer content: <div class="upgrade-offer">...        │
  └────────────────────────────────────────────────────────┘
  
  Use to verify:
  ✓ Target loaded and fired
  ✓ Correct activity returned
  ✓ DOM modification applied

NETWORK / EDGE TAB (Web SDK):
  All Web SDK sendEvent calls:
  ┌────────────────────────────────────────────────────────┐
  │ sendEvent #1 — commerce.purchases                       │
  │   DataStream: abc123-def456                             │
  │   XDM: { eventType: "commerce.purchases", ... }        │
  │   Response: { ECID confirmed, Target decisions: [] }   │
  └────────────────────────────────────────────────────────┘

EVENTS TAB:
  All data layer events in chronological order:
  00:00.123 → page:loaded { page.name: "hotel-detail" }
  00:00.456 → hotel:viewed { hotel.code: "NYCMQ" }
  00:02.891 → booking:confirmed { bookingId: "B-98765" }
```

**QA Lock feature:** Lock the Debugger to a specific Tags environment. Forces the page to load your `staging` Tags library instead of `production` — lets you test staged changes without publishing to production.

---

## 5. Tool 3 — Adobe Experience Cloud Debugger (Tags Validation)

Within Adobe Tags UI itself — after creating or modifying a rule:

```
Tags → Your Property → Rules → [Your Rule] → Preview
  → Click "Save to Library and Build" (for staging)
  → Switch browser to Staging environment using Debugger lock
  → Trigger the event on the real page
  → See if the rule fires in Debugger → Events tab
  → Verify action executed correctly
```

**Rule-level debugging:**
```
In Debugger → Tags tab → Expand rule:
  Shows each action's execution result:
  ✓ "Adobe Analytics - Set Variables" — executed successfully
  ✓ "Adobe Analytics - Send Beacon" — executed successfully
  ✗ "Custom Code" — JavaScript error: ReferenceError: hotelCode is not defined
```

---

## 6. Tool 4 — ObservePoint (Automated Tag Auditing)

ObservePoint is a paid SaaS tool that crawls your entire site and checks every page for:

```
For each URL crawled:
  ✓ Is Tags loaded? (embed code present?)
  ✓ Are required Tags rules firing? (page view rule on every page?)
  ✓ Are required Analytics variables set? (pageName not empty, eVar5 not empty)
  ✓ Are there duplicate beacons? (page view firing twice?)
  ✓ Are there broken rules? (JavaScript errors in Tags)
  ✓ Are third-party pixels loading? (Meta, Google, etc.)

Output: report of all violations across all URLs
  "Page /hotels/NYCMQ: pageName missing on 3% of page loads"
  "Page /booking/confirm: booking event fired 0 times in 100 test runs"
```

Use cases:
- Pre-launch QA: crawl a staging environment before release
- Ongoing monitoring: weekly crawl of production to catch regressions
- Compliance audit: verify no unapproved tags are loading

---

## 7. Tool 5 — Charles Proxy (For Mobile and HTTPS Inspection)

Charles is a desktop app that acts as a proxy between your device/browser and the internet — letting you see ALL network traffic, including HTTPS (with certificate installed).

```
Setup:
  1. Install Charles → enable SSL Proxying for *.adobedc.net, *.omtrdc.net, *.facebook.com
  2. On mobile: set device Wi-Fi proxy to your Mac's IP + Charles port (8888)
  3. Install Charles SSL certificate on the device (trust it in iOS Settings)
  4. Open Marriott app → all network traffic appears in Charles

What you can do that DevTools can't:
  ✓ Inspect ALL app network requests (not just web)
  ✓ See Web SDK / Analytics / Target requests from native iOS/Android app
  ✓ Modify requests on the fly (to test edge cases)
  ✓ Throttle network speed (test slow 3G behavior)
  ✓ Repeat a specific request (right-click → Repeat)
  ✓ Export requests as HAR file for sharing with Adobe support

Common use in mobile MarTech debugging:
  App fires booking event → find POST to edge.adobedc.net in Charles
  → Inspect XDM payload → verify all fields correct
  → Check response: is ECID returned? Are Target decisions in response?
```

---

## 8. Tool 6 — Adobe Experience Platform Assurance (Mobile Only)

Assurance (formerly Project Griffon) is the mobile equivalent of the AEP Debugger browser extension — without needing a proxy or certificate.

```
Setup:
  1. In Assurance UI (experience.adobe.com → Assurance): create a session
  2. QR code appears
  3. In your debug build of the app: scan QR code
  4. SDK connects to Assurance via WebSocket
  5. All SDK events stream into the Assurance UI in real time

Events you see:
  ├── XDM events sent to Edge (with full payload)
  ├── Edge responses (Target decisions, ECID)
  ├── Analytics hits (processed variables)
  ├── Lifecycle events (app open, session length)
  ├── AJO in-app message trigger evaluations
  │     "Rule: screen == 'hotel-detail' → TRUE → showing message"
  └── Push notification delivery status

Validation tools in Assurance:
  Analytics Validation: compare variables in hit vs expected schema
  Edge Validation: verify XDM conforms to DataStream schema
  Target Trace: see which activities are active + decisions returned
```

---

## 9. Tool 7 — AEP Query Service (Verify Data Landed)

Once you've confirmed the beacon fired in the browser, use Query Service to verify it landed in AEP datasets:

```sql
-- Check if a specific booking event landed
SELECT 
  timestamp,
  identityMap['ECID'][0].id AS ecid,
  identityMap['loyaltyId'][0].id AS loyalty_id,
  commerce.order.purchaseID AS booking_id,
  commerce.order.priceTotal AS revenue,
  _marriott.hotel.code AS hotel_code
FROM marriott_web_events
WHERE eventType = 'commerce.purchases'
  AND DATE(timestamp) = DATE('2026-06-28')
ORDER BY timestamp DESC
LIMIT 10;

-- Check for duplicate bookings
SELECT 
  commerce.order.purchaseID,
  COUNT(*) as event_count
FROM marriott_web_events
WHERE eventType = 'commerce.purchases'
  AND DATE(timestamp) = DATE('2026-06-28')
GROUP BY commerce.order.purchaseID
HAVING COUNT(*) > 1;  -- Any result = duplicate booking events
```

---

## 10. Debugging Workflow — Step-by-Step

```
SYMPTOM: "Analytics shows 0 bookings today"

Step 1: Check data layer on confirmation page
  Browser Console → window.adobeDataLayer.getState()
  ✓ booking:confirmed event present? → proceed
  ✗ Missing → engineering didn't push the event → file bug with dev team

Step 2: Check Tags is loaded
  AEP Debugger → Summary tab → Tags listed?
  ✗ Tags missing → embed code not on page → check page template

Step 3: Check rule fired
  AEP Debugger → Events tab → "booking:confirmed" event → rule "Booking Complete" listed?
  ✗ Rule not in list → check rule event condition (correct event name?)

Step 4: Check Analytics beacon
  AEP Debugger → Analytics tab → beacon with events=event4?
  ✗ No beacon → check rule action (Send Beacon action present and enabled?)

Step 5: Check beacon payload
  Network tab → filter "b/ss" → click request → check query params
  events=event4 present? eVar5=hotel name? purchaseID set?
  ✗ Wrong values → data elements pulling wrong data layer path

Step 6: Check report suite
  Debugger → Analytics → which rsid?
  ✓ Correct rsid → wait 30 minutes → check Workspace
  ✗ Wrong rsid → DataStream or s.account misconfigured

Step 7: If still not appearing after 30 min → Check Analytics real-time report
  Analytics → Workspace → Metrics → Real-Time Events report
  ✗ Not appearing → data may be in processing queue; wait; check Analytics Status Dashboard
```

---

## 11. Common Debugging Traps

| Symptom | Common Cause | How to Spot |
|---|---|---|
| Debugger shows no Tags | Embed code missing or wrong environment | Debugger Summary shows nothing under Tags |
| Rule fires but wrong eVar values | Data element path is wrong (e.g., `hotel.name` vs `hotel.hotelName`) | Debugger Analytics tab → see actual eVar value |
| Duplicate page views | Two rules listening to same event / embed code loaded twice | Debugger Analytics tab → two hits per page |
| Target decisions returned but no change | CSS selector doesn't match current DOM | Debugger Target tab → check "Applied" status |
| Analytics hits arrive but wrong report suite | DataStream pointing to dev RS in production | Debugger → Analytics → check rsid in hit |
| No data in AEP after 15 min | DataStream has AEP service disabled | Check DataStream config in Tags; verify AEP service enabled |

---

## 12. Interview Insight

Strong answer:

> When debugging MarTech, I follow the chain: data layer → Tags rule → network request → Adobe processing. The AEP Debugger browser extension is my first tool — it shows every data layer event, Analytics beacon variable, and Target decision in real time without any setup. For network-level inspection I use the browser Network tab filtered to "interact" for Web SDK or "b/ss" for legacy Analytics. For mobile I use Adobe Assurance — connected via QR code, it streams every SDK event in real time without a proxy setup. If data looks correct in the browser but isn't appearing in Analytics reports, I use AEP Query Service to SQL-query the raw AEP dataset directly and confirm the event landed at all.

Follow-up trap:

> Analytics shows bookings fine in the Debugger but the Workspace report shows zero. Where do you look?

Good answer:

> First check the report suite ID in the Debugger hit — is it the global RS that the Workspace report is querying? Second, check the date range and segment in Workspace — if someone applied a segment that filters out these visits, bookings disappear. Third, check if a bot filter or VISTA rule is excluding these hits (check with Adobe Support or look at the Data Feed raw export). Finally, check the Analytics Status Dashboard for any processing delays. The Debugger confirms the hit was sent — if it's not in Workspace, the answer is either wrong report suite, a Workspace filter, or a server-side processing rule excluding the data.

---

## 13. Revision Notes

- One-line summary: AEP Debugger (browser extension) is the primary web debugging tool; Charles Proxy + Assurance cover mobile; follow the chain data layer → Tags → beacon → processing → report.
- Three keywords: AEP Debugger, Assurance (Griffon), Network tab.
- One interview trap: data visible in Debugger but absent from Workspace usually means wrong report suite, a Workspace segment, or server-side processing exclusion — not a missing beacon.
- Memory trick: Debugger = X-ray glasses for the browser. Assurance = X-ray for the app. Network tab = the raw pipe. Query Service = the warehouse receipt confirming delivery.
