# MarTech (Marketing Technology) Mastery Track

> Goal: understand the full Adobe MarTech stack — from how a browser event fires to how it becomes a personalized experience, an analytics report, or a Real-Time CDP audience segment.

---

## Core Mental Model

```txt
User Browser
    │
    ├── Consent Management Platform (OneTrust)
    │       Cookie banner → user consent → gates everything below
    │
    ├── Adobe Data Layer (adobeDataLayer[])
    │       Push events: page view, product view, add-to-cart, booking-complete
    │
    ├── Akamai CDN
    │       Edge delivery + geo-IP detection + bot protection + EdgeWorkers
    │
    ├── Adobe Web SDK (alloy.js)  ← Modern standard (replaces AppMeasurement + at.js)
    │       One library → one request → Adobe Edge Network
    │       DataStream routes to: Analytics + Target + AEP simultaneously
    │
    ├── Adobe Tags (Launch)
    │       Tag manager: rules fire on data layer events → calls Web SDK
    │
    ├── Adobe Analytics
    │       Clickstream storage: eVars, props, events, segments, calculated metrics
    │       Marketing Channels, UTM attribution, Analysis Workspace
    │
    ├── Adobe Target
    │       A/B testing + personalization: serve different content based on audience
    │
    ├── Adobe Experience Platform (AEP)
    │       Unified profile: Real-Time CDP, XDM schemas, identity graph, segments → activation
    │
    ├── Adobe Journey Optimizer (AJO)
    │       Omnichannel journeys: email, SMS, push, in-app — triggered by AEP events
    │
    └── Customer Journey Analytics (CJA)
            Cross-channel analysis: web + app + call center + email stitched per person
```

Short version:

```txt
Consent → Data Layer → Tags → Web SDK → Edge Network → Analytics / Target / AEP → AJO / CJA
```

---

## Learning Path

### Phase 1: Foundations

1. [MarTech Stack Overview — Adobe Ecosystem](./01-Foundations/MarTech-Stack-Overview-Adobe-Ecosystem-Gold-Sheet.md)
2. [Adobe Data Layer — Events, Schema, Tag Integration](./01-Foundations/Adobe-Data-Layer-Events-Tag-Integration-Gold-Sheet.md)
3. [Adobe Web SDK (alloy.js) and DataStreams](./01-Foundations/Adobe-Web-SDK-DataStream-Edge-Gold-Sheet.md)
4. [Privacy, Consent, and Cookie Management](./01-Foundations/Privacy-Consent-Cookie-Management-Gold-Sheet.md)

### Phase 2: Analytics and Edge

5. [Adobe Analytics — Data Collection, Reports, Segments](./02-Analytics/Adobe-Analytics-Data-Collection-Reports-Segments-Gold-Sheet.md)
6. [Akamai — CDN, Geo-IP, EdgeWorkers, Security](./02-Analytics/Akamai-CDN-Geo-IP-EdgeWorkers-Security-Gold-Sheet.md)
7. [Marketing Attribution — UTMs, Channels, Campaign Tracking](./02-Analytics/Marketing-Attribution-UTM-Channels-Campaign-Gold-Sheet.md)

### Phase 3: Activation and Testing

8. [Adobe Tags (Launch) — Rules, Data Elements, Publishing](./03-Activation/Adobe-Tags-Launch-Rules-DataElements-Publishing-Gold-Sheet.md)
9. [Adobe Target — A/B Testing, Personalization, Recommendations](./03-Activation/Adobe-Target-AB-Testing-Personalization-Gold-Sheet.md)

### Phase 4: Platform

10. [Adobe Experience Platform (AEP) — CDP, XDM, Identity, Segments](./04-Platform/Adobe-Experience-Platform-AEP-CDP-XDM-Identity-Gold-Sheet.md)
11. [Adobe Journey Optimizer (AJO) — Journeys, Channels, Offers](./04-Platform/Adobe-Journey-Optimizer-AJO-Journeys-Channels-Gold-Sheet.md)
12. [Customer Journey Analytics (CJA) — Cross-Channel Analysis](./04-Platform/Customer-Journey-Analytics-CJA-Cross-Channel-Gold-Sheet.md)

### Phase 5: Practice Upgrade

13. [Active Recall Question Bank](./05-Practice-Upgrade/MarTech-Active-Recall-Question-Bank.md)
14. [Tricky Scenario Questions](./05-Practice-Upgrade/MarTech-Tricky-Scenario-Questions.md)

---

## Ecosystem Map

| Product | Category | What It Does | Your Role Touches It When |
|---|---|---|---|
| Adobe Data Layer (ACDL) | Client-side standard | Standardizes events pushed by the page | Building page instrumentation |
| Adobe Web SDK (alloy.js) | Data collection library | Replaces AppMeasurement+at.js; sends one request to Edge Network | Implementing modern Adobe data collection |
| Adobe Tags (Launch) | Tag Manager | Loads scripts, fires rules based on data layer events | Configuring tracking rules |
| Consent Management (OneTrust) | Privacy/compliance | Cookie banner, user consent storage, gates MarTech tools | Privacy compliance, GDPR/CCPA |
| Adobe Analytics | Analytics | Stores web clickstream; dimensions, metrics, segments | Analyzing web user behavior |
| Akamai | CDN + Security | Delivers content, detects geo/bot, runs edge logic | Performance, security, geo data |
| Marketing Attribution | Analytics concept | UTMs, channel classification, multi-touch credit | Campaign tracking, ROI measurement |
| Adobe Target | Personalization | Serves A/B test variants, recommendations | Experimentation, personalization |
| Adobe Experience Platform (AEP) | Unified Platform | Real-time customer profile, CDP, identity resolution | Audience building, activation |
| Adobe Journey Optimizer (AJO) | Journey Orchestration | Omnichannel journeys — email, SMS, push, in-app | Triggered campaigns, lifecycle marketing |
| Customer Journey Analytics (CJA) | Advanced analytics | Cross-channel stitched journey analysis on AEP data | Attribution, multi-channel analysis |
| Adobe Audience Manager (AAM) | DMP (legacy) | Audience segments from 1st/2nd/3rd party data | Programmatic advertising (being replaced by AEP) |

---

## Interview Communication Formula

When asked about MarTech stack:

1. Start with the data layer — where data originates on the client.
2. Explain how Tags picks it up without code deploys.
3. Explain where the data lands (Analytics, Target, AEP).
4. Explain how it enables business outcomes (personalization, attribution, audience activation).
5. Always mention the Akamai edge layer as where geo/performance decisions happen before the app code runs.

Strong sentence:

> MarTech is the infrastructure that turns browser events into business intelligence. Data flows from a standardized client-side data layer through a tag manager into analytics, testing, and platform products — without every feature requiring a full engineering deploy.

---

## Official References

- Adobe Data Layer: https://github.com/adobe/adobe-client-data-layer
- Adobe Tags (Launch): https://experienceleague.adobe.com/docs/experience-platform/tags/home.html
- Adobe Analytics: https://experienceleague.adobe.com/docs/analytics/
- Adobe Target: https://experienceleague.adobe.com/docs/target/
- Adobe Experience Platform: https://experienceleague.adobe.com/docs/experience-platform/
- Akamai EdgeWorkers: https://techdocs.akamai.com/edgeworkers/docs/welcome-to-edgeworkers
