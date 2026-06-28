# Privacy, Consent, and Cookie Management — Gold Sheet

> Topic: How MarTech systems handle GDPR, CCPA, cookie consent, and data privacy — the legal plumbing every MarTech engineer must understand

---

## 1. Intuition

Every piece of MarTech — Analytics, Target, AEP — collects user data. In 2026, collecting data without consent is a legal violation. The EU's GDPR and California's CCPA require that users are informed about what data is collected and can opt out. A consent management platform (CMP) like OneTrust sits in front of all your MarTech and acts as a gatekeeper — nothing fires until the user gives permission. Getting this wrong means fines up to 4% of global revenue.

Beginner version:

> Every Adobe tool collects user data. Privacy laws say users must consent first. A Consent Management Platform (OneTrust, TrustArc) handles the cookie banner and tells Tags which tools are allowed to fire — based on what the user said yes or no to.

---

## 2. Definition

- **GDPR (General Data Protection Regulation):** EU law requiring explicit consent before collecting personal data; grants users right to access, correct, and delete their data.
- **CCPA (California Consumer Privacy Act):** US law giving California residents the right to opt out of the sale of their personal information.
- **CMP (Consent Management Platform):** A tool (OneTrust, TrustArc, Cookiebot) that manages the cookie consent banner, stores user consent choices, and communicates them to marketing tools.
- **IAB TCF 2.0 (Transparency and Consent Framework):** An industry standard for how consent is communicated between CMPs and ad tech vendors.

---

## 3. Cookie Categories — What Consent Covers

Every cookie on marriott.com must be classified into a category. Users consent per category:

```
Category 1: Strictly Necessary (no consent required)
  Examples: Session cookie, authentication token, CSRF token
  Reason: Page cannot function without them
  Opt-out: Not possible

Category 2: Performance / Analytics (requires consent)
  Examples: Adobe Analytics beacon, Google Analytics
  Reason: Collects data about how users use the site
  If user declines: No Analytics beacons fire; no hits collected

Category 3: Functional (requires consent)
  Examples: Language preference cookie, recently viewed hotels
  Reason: Enhances experience but not essential
  If user declines: No preference persistence; site works but forgets preferences

Category 4: Targeting / Advertising (requires consent)
  Examples: Adobe Target A/B cookies, Meta Pixel, Google Ads conversion tracking
  Reason: Tracks users for personalization and advertising
  If user declines: No Target activities; no retargeting pixels; no ad tracking

Category 5: Social Media (requires consent)
  Examples: Facebook Like button tracking
  Reason: Enables social sharing with cross-site tracking
```

---

## 4. The Consent Flow on marriott.com

```
User visits www.marriott.com for the first time
        │
        ▼
CMP (OneTrust) detects no consent cookie exists
        │
        ▼
Cookie banner appears:
  "We use cookies to improve your experience and for advertising.
   [Accept All] [Reject Non-Essential] [Manage Preferences]"
        │
        ├── User clicks "Accept All"
        │     → CMP sets: OptanonConsent cookie (all categories = 1)
        │     → CMP fires event: "consentGranted"
        │     → Tags Consent extension: allows all rules to fire
        │     → Analytics, Target, AEP all start collecting
        │
        ├── User clicks "Reject Non-Essential"
        │     → CMP sets: OptanonConsent cookie (only cat 1 = 1; cat 2,3,4 = 0)
        │     → Tags Consent extension: BLOCKS Analytics, Target, ads
        │     → Only session/auth cookies fire (Category 1)
        │
        └── User clicks "Manage Preferences"
              → Detailed preferences modal opens
              → User toggles each category
              → CMP stores granular consent per category
```

---

## 5. OneTrust Integration with Adobe Tags

OneTrust integrates with Adobe Tags via the **Tags Consent Extension** or custom code:

**How Tags gates rules based on consent:**

```
Tags Rule: "Send Analytics Page View"
  Event: Data Layer - page:loaded
  Condition: Consent category "analytics" = GRANTED    ← the gate
  Action: Adobe Experience Platform Web SDK - Send event

Tags Rule: "Load Target (A/B)"
  Event: Library Loaded (top of page)
  Condition: Consent category "targeting" = GRANTED
  Action: Load Target library / sendEvent with renderDecisions:true
```

**Web SDK Consent API:**

```javascript
// When user consents via OneTrust callback
OneTrust.OnConsentChanged(function(event) {
  const activeGroups = OnetrustActiveGroups.split(",");
  const analyticsConsented = activeGroups.includes("C0002");  // performance
  const targetingConsented = activeGroups.includes("C0004");  // targeting

  alloy("setConsent", {
    consent: [{
      standard: "Adobe",    // or "IAB TCF" for programmatic advertising
      version: "2.0",
      value: {
        general: analyticsConsented ? "in" : "out"
      }
    }]
  });
});

// On initial page load, before consent is collected:
alloy("configure", {
  datastreamId: "...",
  defaultConsent: "pending"   // Don't send any data until consent is known
});
```

**What "pending" does:**

```
defaultConsent: "pending"
  → alloy.js queues all sendEvent calls
  → No network request made to Edge
  → Once setConsent("in") is called → all queued events fire
  → Once setConsent("out") is called → queued events dropped
```

---

## 6. Cookies Adobe Uses — What Each Does

```
Cookie Name       Domain              What it stores               Lifespan
─────────────────────────────────────────────────────────────────────────────
s_ecid            .marriott.com       ECID (visitor ID, 1st party) 2 years
AMCV_xxx          .marriott.com       Legacy ECID + product flags  2 years
mbox              .marriott.com       Target session data           1 session
AT_QA_MODE        .marriott.com       Target QA preview flag       Session
OptanonConsent    .marriott.com       OneTrust consent choices     1 year
OptanonAlertBox   .marriott.com       Whether banner was shown     1 year
s_cc              .marriott.com       Cookie support check         Session

BLOCKED by modern browsers (third-party):
demdex            .demdex.net         Cross-domain ID sync (legacy) 180 days
```

**ITP (Intelligent Tracking Prevention):**
Safari's ITP limits first-party cookies set by JavaScript to 7 days, and blocks third-party cookies entirely. This means:
- `s_ecid` set by a server (CNAME) survives; set by JS = 7 days max
- Solution: implement CNAME for data collection (marriott.data.adobedc.net → Adobe servers), which makes the cookie look server-set → 2-year lifespan preserved

---

## 7. GDPR — Key Rights Engineers Must Support

```
Right to Access:
  User requests: "What data do you have about me?"
  Engineering: Query AEP profile by email/loyalty ID → export as JSON
  Tool: Adobe Privacy Service API

Right to Delete (Right to Erasure):
  User requests: "Delete all my data"
  Engineering:
    → Privacy Service API call → AEP deletes profile + all events
    → Analytics data: anonymize (cannot delete specific hits; visitor ID zeroed)
    → Backup systems: must be covered by data retention policy

Right to Opt-Out (CCPA):
  "Do Not Sell My Personal Information" link in footer
  → Sets consent to "out" for targeting category
  → Tags rules for Meta Pixel, Google Ads, programmatic ads stop firing

Data Retention:
  Analytics: typically 25 months (configurable per report suite)
  AEP: set by dataset retention policy (90 days for events, unlimited for profile)
  Logs: must be cleared after agreed retention period
```

**Adobe Privacy Service API (code example):**

```javascript
// Submit a GDPR delete request programmatically
fetch("https://platform.adobe.io/data/privacy/gdpr/", {
  method: "POST",
  headers: {
    "Authorization": "Bearer " + accessToken,
    "x-api-key": apiKey,
    "Content-Type": "application/json"
  },
  body: JSON.stringify({
    companyContexts: [{ namespace: "imsOrgID", value: "ABC123@AdobeOrg" }],
    users: [{
      key: "loyalty-id-98765",
      action: ["delete"],
      userIDs: [
        { namespace: "loyaltyId", type: "analytics", value: "MR-GOLD-789" },
        { namespace: "Email",     type: "standard",  value: "hashed-email-abc123" }
      ]
    }]
  })
});
```

---

## 8. Third-Party Cookie Deprecation — What Happens

Timeline:
- **Safari / Firefox:** Already block all third-party cookies (ITP / ETP)
- **Chrome:** Third-party cookie phase-out ongoing in 2024-2026

Impact on MarTech:

| What breaks | Why | Fix |
|---|---|---|
| demdex.net cross-domain ID sync | Third-party cookie blocked | Use first-party ECID (s_ecid); implement CNAME |
| Audience Manager 3rd party segments | Rely on demdex.net pixels | Migrate to AEP first-party audiences |
| Cross-domain visitor stitching (adobe.com → marriott.com) | Cannot read cookies across domains | Use AEP Identity Graph with authenticated IDs (loyalty ID, email) |
| Retargeting pixels (Meta, Google) | Third-party script cookies blocked | Switch to server-side pixel (Conversions API / server-side Tags) |

**Adobe's answer to third-party cookie death:**
1. **First-party ECID** via s_ecid cookie (all Web SDK implementations already use this)
2. **Adobe Experience Platform** — identity resolution via deterministic IDs (loyalty ID, hashed email) instead of third-party cookies
3. **Server-side event forwarding** — Adobe Tags → Edge Network → send data to Meta/Google server-to-server, bypassing browser cookie restrictions

---

## 9. Common Mistakes

| Mistake | Consequence | Fix |
|---|---|---|
| Firing Analytics before consent is granted | GDPR violation — collecting data without consent | Set `defaultConsent: "pending"` in Web SDK; gate rules on consent |
| Sending raw email or PII in Analytics eVars | Personally identifiable data stored in Analytics — GDPR violation | Hash all PII before sending; never put email/name/phone in eVars/props |
| No "Do Not Sell" link for CCPA compliance | Legal exposure for California users | Add footer link → fire OneTrust opt-out → disable targeting category |
| Deleting a user from AEP but not Analytics | Incomplete erasure — GDPR violation | Cover all data stores in the deletion workflow |
| Not implementing CNAME for data collection | s_ecid cookie = 7 days on Safari (ITP caps JS cookies) | Implement CNAME → server-set cookie → 2-year lifespan |

---

## 10. Interview Insight

Strong answer:

> Privacy compliance in MarTech works in layers. The CMP (OneTrust) collects user consent and communicates it to Adobe Tags via the Consent Extension. Tags gates every rule by consent category — Analytics only fires if the user accepted performance cookies; Target only fires if they accepted targeting cookies. Web SDK's `defaultConsent: "pending"` prevents any data from leaving the browser until consent is confirmed. For GDPR erasure requests, we use Adobe Privacy Service API to delete from AEP and anonymize from Analytics. Third-party cookie deprecation is handled by using first-party ECIDs via CNAME implementation and server-side event forwarding for ad platform pixels.

Follow-up trap:

> A user clicks "Reject All" and then books a hotel. Do you still track that booking?

Good answer:

> This depends on your legal basis. If the booking creates a contract with the user, you may be able to use "Legitimate Interest" or "Contractual Necessity" as a legal basis for collecting transactional data — not consent. The session cookie (Category 1: Strictly Necessary) can still track the session. The key distinction: you can log the transaction in your CRM and order management system for business necessity, but you cannot use that data for analytics, retargeting, or personalization — because that requires consent which was denied. The Analytics beacon for the booking would not fire.

---

## 11. Revision Notes

- One-line summary: CMPs like OneTrust collect user consent per cookie category; Tags gates every rule on consent; Web SDK holds data with `defaultConsent: "pending"` until consent is known; GDPR erasure flows through Adobe Privacy Service API.
- Three keywords: consent categories, CMP, Privacy Service API.
- One interview trap: "Reject All" doesn't mean zero tracking — strictly necessary cookies (session, auth) fire without consent.
- Memory trick: Consent gates MarTech like a bouncer. No wristband (consent) = no entry (no data collected). Privacy Service = the cleanup crew after someone leaves.
