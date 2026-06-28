# MarTech Tricky Scenario Questions — Gold Sheet

> Format: broken config / real symptom → diagnose before reading the answer

---

## How to Use

Cover the answer section. Read the scenario. Think through the root cause. Then reveal.

---

## Scenario 1 — The Analytics Report That Shows 0 Bookings

**Situation:**
The conversion report in Adobe Analytics shows 0 booking confirmations for the last 7 days. The dev team confirms the booking page is working fine — users ARE completing bookings successfully in the payment system.

**What do you check first?**

<details><summary>Answer</summary>

This is a data collection failure, not a business failure. Work through the chain:

**Step 1: Check if Tags is loading on the booking confirmation page**
Open the AEP Debugger browser extension on the confirmation page.
- Does the Summary tab show the Adobe Analytics extension loaded? If not → Tags embed code is missing on the confirmation page (common when confirmation page is on a different subdomain or SPA route).

**Step 2: Check if the booking:confirmed rule is firing**
- Debugger → Events tab → Does `booking:confirmed` appear when you complete a booking?
- If no event in the data layer → engineering didn't push the event on the confirmation step.

**Step 3: Check the Analytics beacon**
- Debugger → Analytics tab → Is there a beacon with `events=event4`?
- If beacon is missing → the Tags rule condition may be filtering it out (e.g., condition checks `%environment% = production` but the QA environment tag is wrong).

**Step 4: Check the rule condition**
- In Tags, review the "Booking Complete" rule conditions — a common mistake is `URL contains /confirmation` but the actual URL is `/booking/success` → condition never passes.

**Root cause:** Usually either (1) no data layer push on confirmation page, or (2) Tags rule condition that filters out the beacon.

</details>

---

## Scenario 2 — The A/B Test Where Both Variants Are Identical

**Situation:**
A product manager says the A/B test launched in Target but after 3 days of running, the conversion rates are exactly the same for both variants — down to three decimal places. This is statistically impossible unless something is wrong.

**What happened?**

<details><summary>Answer</summary>

When both variants show identical metrics, the most likely cause is that **Target is not actually delivering the experience change** — users see the same thing regardless of which variant they're assigned to.

**Diagnoses in order:**

1. **The DOM modification selector is wrong:**
   Target uses a CSS selector to change content (e.g., `#hero-banner h1`). If the selector doesn't match any element on the actual page (because a developer changed the HTML structure), Target silently fails — no error, no change. Both variants look identical.

   **Fix:** Use the Target Visual Experience Composer (VEC) to verify the selector still matches the current page DOM.

2. **at.js is loading after the DOM renders (no prehiding):**
   Target changes the DOM but users briefly see the original content and the change flashes in. For `s.t()` analytics hits, the variant assignment IS happening — but users may have scrolled past the changed element. Not an identical-rates issue, but related.

3. **Activity is in QA mode only:**
   Target QA Preview Links only show the variant to the person with the QA link. If the activity was accidentally left in QA mode or not fully activated, real users all see the Control.

4. **Analytics report suite mismatch:**
   The Analytics goal metric (event4) is being reported from a different report suite than where the activity data flows. The test runs, but the success metrics don't connect.

**Most common:** Wrong CSS selector → experience never applied → identical data.

</details>

---

## Scenario 3 — The Data Layer Shows Wrong Country Code

**Situation:**
Your data layer shows `browser_akamai_loc_country: 'NL'` (Netherlands) for a user in Hyderabad, India. The analytics team is seeing wrong geo data in their segmentation reports. The user has not used a VPN (they claim).

**Diagnose and explain.**

<details><summary>Answer</summary>

Several possibilities, in order of likelihood:

**1. Corporate VPN/Proxy (most common):**
Even if the user says "no VPN," many enterprise networks route all traffic through a corporate proxy. The egress IP of the proxy is in the Netherlands → Akamai sees NL.

**2. ISP with international peering:**
Some ISPs route traffic through international network hubs. The IP that Akamai sees at the edge is the peering point's IP, not the user's ISP IP.

**3. IPv6 address not in Akamai's geo database:**
If the user's ISP assigned an IPv6 address, Akamai's geo database may not have a location for it yet → it may fall back to a default or misidentify it.

**4. Stale Akamai geo-IP database:**
Akamai updates its geo-IP database regularly, but IP ranges reassigned between ISPs can take weeks to update.

**What NOT to do:**
Don't use this Akamai geo-IP for legal/regulatory geo-blocking. Use explicit user declaration or account-level country settings for anything requiring accuracy.

**Fix for analytics:**
Add a fallback: if user is logged in, use their account country (from loyalty profile in AEP) rather than Akamai geo-IP for Analytics dimensions.

</details>

---

## Scenario 4 — Tags Rule Fires Twice on Every Page

**Situation:**
The analytics team notices each hotel detail page shows 2 page view hits in Adobe Analytics (every single page, not just some). Revenue is double-counted. The rule was working fine last week.

**What happened?**

<details><summary>Answer</summary>

"Fires twice" in Tags almost always comes from one of these causes:

**1. Two rules firing on the same event:**
Check the Tags rules list — is there a second "page view" rule that was accidentally created or duplicated? Both rules listen to `page:loaded` and both fire a `Send Beacon` action.

**2. The same Tags library loaded twice:**
A developer added the Tags embed code to both the `<head>` (standard) and at the end of `<body>` (thinking it would help load time). Two instances of Tags load → each fires once.

**3. SPA route change firing a data layer push twice:**
If the site is a Single-Page Application (React, Angular) and the router fires `page:loaded` on both the initial load AND the route transition for the same page, the rule fires twice.

**Diagnosis:**
Open Debugger → Events tab → count how many `page:loaded` events appear for a single page load. If two events → data layer push fires twice. If one event → check for two rules or two Tags instances.

**Fix for SPA:**
Use `page:viewed` event only on route changes, not on both initial load and route change. Add a condition: "fire only if previous page URL is different from current URL."

</details>

---

## Scenario 5 — AEP Segments Not Reaching Adobe Target in Time

**Situation:**
Marketing set up a segment in AEP: "Users who searched but didn't book in the last 2 hours." They want to show a targeted offer on the home page when the user returns. But users are seeing the default home page instead of the offer.

**What's the technical problem?**

<details><summary>Answer</summary>

**Root cause: Wrong segment evaluation type.**

A segment rule with "in the last 2 hours" lookback requires **streaming segmentation** (updates within 5 minutes as events arrive) or **edge segmentation** (resolved in <100ms on page load).

**Batch segmentation** — the default — only refreshes once every 24 hours. A user who searched 90 minutes ago won't be in the segment until the next day's batch run → they see the default home page.

**Second issue: Target integration type**

Even with streaming segmentation, if the activation to Target uses an audience that requires AEP to push the membership to Target (not edge evaluation), there's additional latency.

For **real-time Target personalization** (resolving the segment on each page load):
- The segment must use **edge segmentation**
- The segment rules must be edge-compatible (simple attribute + recent event rules; no complex multi-dataset joins)
- Target must be configured to evaluate segments on the AEP Edge Network

**Fix:**
1. Check segment rule complexity — simplify if needed to qualify for edge evaluation
2. Change segment evaluation type to "Streaming" or "Edge" in AEP segment builder
3. Verify Target activity uses AEP audience with edge-compatible destination
4. Use Adobe Experience Platform Debugger to confirm the segment membership resolves in the Target response payload

</details>

---

## Scenario 6 — Data Layer Push Working Locally but Not in Production

**Situation:**
A developer verified the data layer push in development and staging using the browser console. But in production, the Tags rule that listens to `hotel:viewed` never fires, and no Analytics beacon appears.

**What's different in production?**

<details><summary>Answer</summary>

**Most likely: Content Security Policy (CSP) blocking Tags or the beacon.**

Production sites often have a strict CSP that development doesn't have:

```
Content-Security-Policy:
  script-src 'self' https://assets.adobedtm.com;
  connect-src 'self' https://dpm.demdex.net;
```

If `assets.adobedtm.com` is not in the `script-src` allowlist → Tags fails to load. No Tags → no rules fire → no Analytics beacon.

If the Analytics beacon domain (`marriott.d1.sc.omtrdc.net`) is not in `connect-src` → beacons are blocked silently.

**Check:** Open browser DevTools → Console tab → look for CSP violation errors in production.

**Other possibilities:**
1. **Tag rule condition checks `%environment% = production`** but the Tags property's environment classification isn't set → condition fails
2. **Akamai is caching the page aggressively** and the cached version doesn't have the updated Tags embed code
3. **Tags embed code is missing** from the specific production CDN-cached page template

**Fix:** Add Akamai and Adobe domains to CSP; verify Tags embed code is in the deployed template; test with Debugger in production with cache bypass.

</details>

---

## Revision Notes

- One-line summary: Six real-world MarTech debugging scenarios covering data loss, A/B test failures, geo-IP issues, double-counting, segment latency, and CSP blocking.
- Debugging pattern: always start from the browser (Debugger extension) → trace back through Tags → data layer → origin.
- One interview trap: "Analytics shows zero bookings" is almost never a business problem — it's a collection pipeline break.
- Memory trick: When MarTech data is wrong, the answer is always in the chain: data layer push → Tags rule → beacon → processing → report.
