# MarTech (Marketing Technology) Mastery Track

> Goal: understand the full Adobe MarTech stack — from how a browser event fires to how it becomes a personalized experience, an analytics report, or a Real-Time CDP audience segment.

---

## Core Mental Model

```txt
User Browser
    │
    ├── Adobe Data Layer (adobeDataLayer[])
    │       Push events: page view, product view, add-to-cart, booking-complete
    │
    ├── Akamai CDN
    │       Edge delivery + geo-IP detection + bot protection + EdgeWorkers
    │
    ├── Adobe Tags (Launch)
    │       Tag manager: rules fire on data layer events → send beacons to Adobe products
    │
    ├── Adobe Analytics
    │       Clickstream storage: eVars, props, events, segments, calculated metrics
    │
    ├── Adobe Target
    │       A/B testing + personalization: serve different content based on audience
    │
    └── Adobe Experience Platform (AEP)
            Unified profile: Real-Time CDP, XDM schemas, identity graph, segments → activation
```

Short version:

```txt
Data Layer → Tags → Analytics / Target → AEP
```

---

## Learning Path

### Phase 1: Foundations

1. [MarTech Stack Overview — Adobe Ecosystem](./01-Foundations/MarTech-Stack-Overview-Adobe-Ecosystem-Gold-Sheet.md)
2. [Adobe Data Layer — Events, Schema, Tag Integration](./01-Foundations/Adobe-Data-Layer-Events-Tag-Integration-Gold-Sheet.md)

### Phase 2: Analytics and Edge

3. [Adobe Analytics — Data Collection, Reports, Segments](./02-Analytics/Adobe-Analytics-Data-Collection-Reports-Segments-Gold-Sheet.md)
4. [Akamai — CDN, Geo-IP, EdgeWorkers, Security](./02-Analytics/Akamai-CDN-Geo-IP-EdgeWorkers-Security-Gold-Sheet.md)

### Phase 3: Activation and Testing

5. [Adobe Tags (Launch) — Rules, Data Elements, Publishing](./03-Activation/Adobe-Tags-Launch-Rules-DataElements-Publishing-Gold-Sheet.md)
6. [Adobe Target — A/B Testing, Personalization, Recommendations](./03-Activation/Adobe-Target-AB-Testing-Personalization-Gold-Sheet.md)

### Phase 4: Platform

7. [Adobe Experience Platform (AEP) — CDP, XDM, Identity, Segments](./04-Platform/Adobe-Experience-Platform-AEP-CDP-XDM-Identity-Gold-Sheet.md)

### Phase 5: Practice Upgrade

8. [Active Recall Question Bank](./05-Practice-Upgrade/MarTech-Active-Recall-Question-Bank.md)
9. [Tricky Scenario Questions](./05-Practice-Upgrade/MarTech-Tricky-Scenario-Questions.md)

---

## Ecosystem Map

| Product | Category | What It Does | Your Role Touches It When |
|---|---|---|---|
| Adobe Data Layer | Client-side standard | Standardizes events pushed by the page | Building page instrumentation |
| Adobe Tags (Launch) | Tag Manager | Loads scripts, fires rules based on data layer events | Configuring tracking rules |
| Adobe Analytics | Analytics | Stores clickstream, dimensions, metrics | Analyzing user behavior |
| Akamai | CDN + Security | Delivers content, detects geo/bot, runs edge logic | Performance, security, geo data |
| Adobe Target | Personalization | Serves A/B test variants, recommendations | Experimentation, personalization |
| Adobe Experience Platform | Unified Platform | Real-time customer profile, CDP, identity resolution | Audience building, activation |
| Customer Journey Analytics (CJA) | Advanced analytics | Cross-channel stitched journey analysis | Attribution, multi-touch |
| Adobe Audience Manager (AAM) | DMP | Audience segments from 1st/2nd/3rd party data (legacy) | Programmatic advertising |

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
