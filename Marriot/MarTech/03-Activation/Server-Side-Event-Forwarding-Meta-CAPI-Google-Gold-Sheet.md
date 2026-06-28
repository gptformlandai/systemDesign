# Server-Side Event Forwarding — Gold Sheet

> Topic: How Adobe Tags sends data server-to-server to Meta, Google, and other ad platforms — bypassing third-party cookie restrictions and browser ad blockers

---

## 1. Intuition

When a user books a hotel on marriott.com, you want to tell Google and Meta "this person converted — optimize your ads toward people like them." The old way: load Google's pixel and Meta's pixel in the browser — JavaScript that drops third-party cookies and sends data. The new problem: Safari blocks these pixels entirely, Firefox blocks them, ad blockers kill them, and iOS 14+ privacy prompts make users opt out. Server-side event forwarding moves this data flow OUT of the browser — your server tells Google and Meta about the conversion, never touching the browser at all.

Beginner version:

> Instead of the browser sending conversion data to Google and Meta (which browsers increasingly block), your server sends it directly. Server-to-server = not blockable by browsers, ad blockers, or iOS privacy settings.

---

## 2. Definition

- **Event Forwarding:** An Adobe Tags capability that runs rules on Adobe's Edge Network servers (not in the browser) to forward event data to third parties via server-side API calls.
- **Adobe Edge Network:** Adobe's globally distributed server infrastructure that receives Web SDK events and can forward them to any HTTPS endpoint.
- **Server-side pixel (Conversions API):** Meta and Google offer server-to-server APIs that accept conversion events directly — bypassing browser restrictions entirely.
- **Data enrichment:** The server has access to data the browser doesn't (IP address, loyalty profile from CRM) — server-side events can be enriched before forwarding.

---

## 3. Client-Side vs Server-Side Pixel — The Problem

### Client-Side (Old Way — Increasingly Broken)

```
User books hotel
    │
    ▼
Browser runs:
  Meta Pixel (fbq.js)        → POST graph.facebook.com/... (Purchase event)
  Google Ads tag (gtag.js)   → POST google-analytics.com/... (conversion)
  LinkedIn Insight Tag       → POST px.ads.linkedin.com/...

BLOCKED BY:
  ✗ Safari ITP → blocks Meta pixel tracking cookie (IFP)
  ✗ Firefox ETP → blocks known tracker domains
  ✗ iOS 14+ → App Tracking Transparency → user opt-out
  ✗ Ad blockers → block all pixel requests
  ✗ Browser extensions (Privacy Badger, uBlock) → kill pixels

Result:
  Meta sees 40-60% of actual conversions
  Google sees 30-50% of actual conversions
  Attribution is wrong → you optimize toward the wrong audiences
```

### Server-Side Event Forwarding (Modern Way)

```
User books hotel
    │
    ▼
Browser: alloy.js sends ONE event to Adobe Edge Network
         (single first-party request — not blockable as tracker)
    │
    ▼
Adobe Edge Network (server-side)
    ├── → Adobe Analytics report suite
    ├── → AEP (profile + events)
    ├── → Meta Conversions API (server-to-server POST)
    │       body: { event_name: "Purchase", event_time: ..., value: 450, currency: "USD",
    │               email_hash: sha256(email), phone_hash: sha256(phone) }
    ├── → Google Ads Enhanced Conversions API (server-to-server)
    └── → Any other HTTPS endpoint (Klaviyo, Salesforce, custom webhook)

RESULT:
  Not blockable — traffic goes browser → Adobe (first-party) → server → Meta/Google
  Near-complete signal recovery (85-95% of conversions visible)
  Enriched data: server can add hashed email, IP, loyalty tier
```

---

## 4. Setting Up Event Forwarding in Adobe Tags

Event Forwarding uses a separate Tags Property — an "Event Forwarding Property":

```
Standard Tags Property (client-side)
  Purpose: Load libraries in browser, listen to data layer, call Web SDK
  Runs on: User's browser

Event Forwarding Property (server-side)
  Purpose: Receive events from Edge Network, forward to third parties
  Runs on: Adobe Edge Network servers (not in browser)
```

**Configuration flow:**

```
Step 1: Create Event Forwarding Property in Tags UI
  Property type: Event Forwarding (not Web)

Step 2: Install extensions
  - Meta Conversions API extension
  - Google Ads Enhanced Conversions extension
  - Custom HTTP Connector (for any other endpoint)

Step 3: Create rules (same Events/Conditions/Actions pattern as client-side Tags)
  Rule: "Forward Booking to Meta"
    Event: Any (fires on every edge hit)
    Condition: event.xdm.eventType = "commerce.purchases"
    Action: Meta CAPI - Send Event
              event_name: Purchase
              value: %event.xdm.commerce.order.priceTotal%
              currency: %event.xdm.commerce.order.currencyCode%
              email: %event.xdm.identityMap.Email[0].id%  ← hashed by extension

Step 4: Add Event Forwarding Property to DataStream
  DataStream → Event Forwarding → Select your EF Property
```

---

## 5. Meta Conversions API (CAPI) — Full Setup

```
Meta Conversions API endpoint:
  POST https://graph.facebook.com/v18.0/{PIXEL_ID}/events
  
Body (what Adobe's EF extension sends):
{
  "data": [{
    "event_name": "Purchase",
    "event_time": 1751107385,               // Unix timestamp
    "event_source_url": "https://www.marriott.com/booking/confirmation",
    "action_source": "website",
    "user_data": {
      "em": "sha256-hash-of-email",          // Hashed email for matching
      "ph": "sha256-hash-of-phone",          // Hashed phone
      "client_ip_address": "203.0.113.42",   // User's IP (available server-side)
      "client_user_agent": "Mozilla/5.0...", // User agent
      "fbc": "fb.1.1234567890.AbCdEfGh",    // Facebook click ID (fbclid cookie)
      "fbp": "fb.1.1234567890.1234567890"   // Meta pixel cookie (if readable)
    },
    "custom_data": {
      "currency": "USD",
      "value": 450.00,
      "content_ids": ["NYCMQ-DELUXE-KING"],
      "content_type": "hotel",
      "order_id": "B-98765"
    }
  }],
  "access_token": "YOUR_META_ACCESS_TOKEN"
}
```

**Event deduplication** (critical — prevents double counting):

If both client-side Meta pixel AND server-side CAPI fire, Meta deduplicates using `event_id`:

```javascript
// Client-side pixel (fbq.js) — set event ID
fbq('track', 'Purchase', { value: 450, currency: 'USD' }, { eventID: 'B-98765' });

// Server-side CAPI — same event ID
{
  "event_id": "B-98765",   // Same as client → Meta deduplicates, counts ONCE
  "event_name": "Purchase"
}
```

---

## 6. Google Enhanced Conversions — Server-Side

Google's equivalent of Meta CAPI:

```
Google Enhanced Conversions API sends hashed user data alongside conversion events:

POST https://www.googleapis.com/upload/conversion/v3/...

Sends:
  - Hashed email (SHA256)
  - Hashed phone
  - Conversion value + currency
  - Order ID (deduplication key)
  - Google Click ID (gclid from URL parameter)
```

**The gclid flow:**
```
User clicks Google Ad:
  URL: https://www.marriott.com/hotels?gclid=CjwKCAjw...

Tags reads gclid from URL → stores in first-party cookie
  Data Element: "Google Click ID"
  Type: Cookie
  Cookie name: _gcl_aw

On booking confirmation:
  alloy.js sends event to Edge with gclid in payload
  EF Rule forwards to Google Enhanced Conversions with gclid
  → Google matches this conversion back to the ad click
```

---

## 7. Data Enrichment — What Server-Side Enables That Browser Cannot

```
CLIENT-SIDE PIXEL DATA (what browser can send):
  ✓ Page URL
  ✓ Event name
  ✓ Purchase value
  ✗ User's real IP (JavaScript sees a fake/proxy IP)
  ✗ Hashed email (available only if user is logged in and page passes it to pixel)
  ✗ Loyalty profile data

SERVER-SIDE EVENT FORWARDING DATA (Adobe Edge + backend enrichment):
  ✓ Page URL
  ✓ Event name
  ✓ Purchase value
  ✓ User's real IP address (Edge sees the actual source IP)
  ✓ Hashed email (from AEP profile lookup or from authenticated session)
  ✓ Hashed phone
  ✓ Loyalty tier, lifetime value (from CRM enrichment)
  ✓ Order ID for deduplication

Richer data → better audience matching → better ad optimization → lower CPM
```

---

## 8. Custom HTTP Connector — Forward to Any System

Not every destination has a native extension. The Custom HTTP Connector lets you forward events to any HTTPS endpoint:

```
Rule: "Forward Booking to Internal Data Warehouse"
  Condition: eventType = "commerce.purchases"
  Action: Custom HTTP Connector
    URL: https://ingest.marriott-internal.com/events
    Method: POST
    Headers:
      Authorization: Bearer {{secret.INGEST_TOKEN}}   ← stored as EF secret, not in code
      Content-Type: application/json
    Body:
      {
        "bookingId": "{{arc.event.data.xdm.commerce.order.purchaseID}}",
        "revenue": "{{arc.event.data.xdm.commerce.order.priceTotal}}",
        "loyaltyId": "{{arc.event.data.xdm.identityMap.loyaltyId.0.id}}",
        "hotelCode": "{{arc.event.data.xdm.productListItems.0.SKU}}"
      }
```

---

## 9. Secrets in Event Forwarding

API keys and access tokens must NEVER be hard-coded in Tags rules. Event Forwarding has a Secrets management system:

```
EF Secrets:
  META_ACCESS_TOKEN    → stored encrypted; referenced as {{secret.META_ACCESS_TOKEN}}
  GOOGLE_API_KEY       → stored encrypted; referenced as {{secret.GOOGLE_API_KEY}}
  INGEST_API_TOKEN     → stored encrypted; referenced as {{secret.INGEST_API_TOKEN}}

Secrets can be:
  - Token type: static value (manually rotated)
  - OAuth2 type: auto-rotated using OAuth2 client credentials flow
  - Google OAuth: auto-rotated using Google service account
```

---

## 10. Common Mistakes

| Mistake | Consequence | Fix |
|---|---|---|
| Running server-side CAPI without deduplication | Meta counts every conversion twice (once from pixel, once from CAPI) → inflated conversions → wrong optimization | Always set matching `event_id` in both client pixel and CAPI |
| Forwarding PII (raw email) without hashing | Ad platforms receive raw personal data → GDPR violation | Always SHA256-hash email and phone before forwarding; EF extensions do this automatically |
| Not gating EF rules on consent | Forwarding conversions to Meta/Google for users who declined advertising cookies | Add condition: consent category "targeting" = granted before forwarding |
| Hard-coding API tokens in rule bodies | Secrets exposed in Tags audit logs and version history | Always use EF Secrets; never put tokens in rule action fields |
| Using Event Forwarding without Web SDK | EF only works with Web SDK (alloy.js) as the source; legacy AppMeasurement doesn't send to Edge Network | Migrate to Web SDK before implementing Event Forwarding |

---

## 11. Interview Insight

Strong answer:

> Server-side event forwarding moves conversion tracking from the browser to Adobe's Edge Network. The browser makes one first-party request to Adobe Edge (alloy.js to datastreamId). Edge then fans that event out server-to-server: to Adobe Analytics, AEP, Meta Conversions API, and Google Enhanced Conversions simultaneously. Because the browser never contacts Meta or Google directly, ad blockers, ITP, and iOS privacy restrictions don't affect the conversion signal. Meta CAPI with hashed email typically recovers 30-40% of conversions that were invisible to client-side pixels. Deduplication is critical when running both client and server-side: set the same `event_id` (booking ID works well) in both, and the platforms count it once.

Follow-up trap:

> If server-side forwarding is better, why keep the client-side pixel at all?

Good answer:

> The client-side pixel still contributes unique data the server can't get: the Meta pixel cookie (`fbp`) and Facebook click ID (`fbc`) are only accessible in the browser via JavaScript. These are key identifiers for Meta's attribution. The best-of-both-worlds approach is: keep a minimal client-side pixel to capture `fbp`/`fbc` cookies and pass them in the page's data layer — then forward everything to server-side CAPI enriched with those values plus hashed email from the server side. This maximizes both signal completeness and browser-independent reliability.

---

## 12. Revision Notes

- One-line summary: Event Forwarding moves conversion data from browser pixels to server-side API calls via Adobe Edge Network — recovering the 40-60% of conversions that client-side pixels miss due to ad blockers, ITP, and iOS privacy restrictions.
- Three keywords: Edge Network, Conversions API, deduplication.
- One interview trap: running both client-side pixel AND server-side CAPI without matching `event_id` doubles conversion counts and corrupts ad optimization.
- Memory trick: Client pixel = note slipped under the door (easily blocked). Server CAPI = direct phone call (nothing in the way).
