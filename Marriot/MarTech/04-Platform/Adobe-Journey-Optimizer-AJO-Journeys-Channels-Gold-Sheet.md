# Adobe Journey Optimizer (AJO) — Journeys, Channels, Offers — Gold Sheet

> Topic: Adobe Journey Optimizer — the omnichannel platform that orchestrates the right message, on the right channel, at the right moment — triggered by real-time customer behavior

---

## 1. Intuition

A guest books a hotel in New York. Three things should happen automatically: a confirmation email fires immediately, a pre-stay packing checklist email fires 48 hours before check-in, and an in-app push fires when they arrive at the property. Doing this manually would require three separate systems and three separate engineering efforts. Adobe Journey Optimizer is the canvas where you draw this flow as a visual journey — once — and it executes it automatically for every guest who matches the trigger.

Beginner version:

> Journey Optimizer is like an automated workflow builder for marketing messages. "When a guest books → send confirmation email → wait 48h → send pre-stay email → on check-in day → send welcome push." You draw it once; it runs for every guest.

---

## 2. Definition

- **Adobe Journey Optimizer (AJO):** An Adobe Experience Platform application that orchestrates real-time, personalized customer journeys across email, SMS, push, in-app, and web channels — triggered by live AEP events and profile data.
- **Journey:** A visual workflow canvas. Entry condition → sequence of steps (waits, conditions, actions) → exit.
- **Channel action:** A node in the journey that sends a message on a specific channel (email, SMS, push, in-app, direct mail).
- **Decision Management (Offer Decisioning):** AJO's AI-powered offer selection engine — determines which offer is best for a specific profile at a specific moment.

---

## 3. AJO Architecture

```
AEP Real-Time Customer Profile (the source of truth)
    │
    ├── Events streaming in (booking confirmed, check-in, search, browse)
    │
    ▼
Journey Optimizer
    │
    ├── Journey Engine
    │     → Triggered by: AEP events, audience entry/exit, schedule
    │     → Evaluates: conditions, wait timers, branching
    │     → Executes: channel actions
    │
    ├── Message Authoring
    │     → Email (drag-drop HTML editor)
    │     → SMS / MMS
    │     → Push notification (iOS + Android)
    │     → In-app message (mobile SDK or web)
    │     → Direct mail (API to print vendors)
    │
    ├── Decisioning (Offer Management)
    │     → Rules-based offers
    │     → AI-ranked offers (auto-optimization)
    │
    └── Reporting
          → Journey analytics: entries, exits, bounce, conversions
          → Channel reporting: open rate, click rate, conversion
```

---

## 4. Journey Types

### Event-Triggered Journey (real-time)

Starts the moment a specific event is received from AEP. Sub-second latency.

```
Journey: "Booking Confirmation Journey"
  Entry: AEP event "commerce.purchases" received
         (booking confirmed on marriott.com)
  
  [START]
    │
    ├── Action: Send Email "Your Reservation is Confirmed"
    │     Template: booking-confirmation-v3
    │     Personalization: {{profile.person.name}}, {{booking.hotelName}},
    │                      {{booking.checkIn}}, {{booking.checkOut}}
    │
    ├── Wait: 1 day before check-in date
    │     (absolute wait: evaluate when checkIn - today = 1 day)
    │
    ├── Condition: Has the guest downloaded the Marriott app?
    │     └── YES → Send Push: "Your room is almost ready! Check-in tips"
    │     └── NO  → Send SMS: "Tomorrow's your stay! Contactless check-in: [link]"
    │
    ├── Wait: Until check-in day, 2pm
    │
    ├── Action: Send Push: "Welcome to {{booking.hotelName}}! Your room is ready"
    │
    ├── Wait: 24h after check-out
    │
    └── Action: Send Email "How was your stay? Rate us" (satisfaction survey)
  
  [EXIT conditions]
    - Booking cancelled event received → exit immediately
    - Profile unsubscribed from email → suppress email steps
    - Max 30 days in journey
```

### Audience-Triggered Journey (scheduled batch)

Starts when a profile enters or exits an AEP audience segment.

```
Journey: "Loyalty Re-engagement Campaign"
  Entry: Profile enters AEP segment "Gold Members - Lapsed 90 days"
         (segment refreshes daily via batch evaluation)
  
  [START]
    │
    ├── Action: Send Email "We miss you, {{profile.person.firstName}}!"
    │     Offer: 20% off next booking
    │
    ├── Wait: 5 days
    │
    ├── Condition: Did they book in last 5 days?
    │     └── YES → Exit (goal achieved)
    │     └── NO  → Action: Send Push "Your 20% offer expires in 3 days"
    │
    ├── Wait: 3 days
    │
    └── Action: Send SMS "Last chance — 20% discount on your next stay [link]"
  
  [EXIT]
    - Profile exits the "lapsed" segment (they booked)
    - 14 days elapsed
```

### Read-Segment Journey (one-time batch)

Run once for a static snapshot of an audience — like a targeted promotional blast.

```
Journey: "NYC Summer Promotion Blast"
  Entry: Run once → read all profiles in segment "East Coast Loyalists"
  
  Action: Send Email "NYC Summer deals — up to 40% off"
  
  (No waits, no branches — pure one-time email blast to a segment)
```

---

## 5. Message Personalization in AJO

AJO uses a templating language called **Expression Language (EL)** (similar to Handlebars/Liquid):

```
Email subject line:
  "{{profile.person.firstName}}, your {{booking.hotelBrand}} stay is confirmed"
  → "Aravind, your Marriott stay is confirmed"

Email body:
  Hotel: {{booking.hotelName}}
  Check-in: {{booking.checkIn | dateFormat("MMMM d, yyyy")}}
  Check-out: {{booking.checkOut | dateFormat("MMMM d, yyyy")}}
  Room type: {{booking.roomType}}
  
  {% if profile.loyalty.tier == "titanium" %}
    As a Titanium Elite member, you'll receive complimentary breakfast.
  {% endif %}
  
  {% if profile.preferences.roomPreference == "high-floor" %}
    We've noted your preference for a high floor room.
  {% endif %}
```

Personalization data sources:
- Real-Time Customer Profile (all attributes and events)
- Context data from the triggering event (booking details)
- Lookup datasets (hotel information, loyalty program rules)

---

## 6. Decision Management — AI Offer Selection

Decision Management selects the best offer for each profile using rules or AI ranking:

```
Offer Library:
  Offer A: "Earn 2x points on weekend stays"
    Eligibility: loyalty tier IN [gold, platinum, titanium]
    Cap: 1 per profile per month

  Offer B: "Free breakfast voucher"
    Eligibility: loyalty tier IN [platinum, titanium]
    Cap: 3 per year per profile

  Offer C: "20% off 3-night stays"
    Eligibility: no bookings in last 90 days (lapsed)
    Cap: 1 per profile per quarter

  Fallback offer: "Discover our latest deals"
    Eligibility: all profiles (no rules)

Decision:
  Profile: Gold member, last booking was 45 days ago
  Eligible offers: A (gold ✓), C (45 days < 90, not lapsed ✗)
  
  → Offer A served: "Earn 2x points on weekend stays"
```

AI Ranking (Auto-Optimization):
- AJO tracks which offers each profile type accepts
- Learns over time: "Titanium members respond to room upgrades more than discounts"
- Auto-ranks offers per profile without manual rules

---

## 7. Channels AJO Manages

| Channel | Requires | Typical use case |
|---|---|---|
| **Email** | SMTP configuration (Adobe or custom MTA) | Confirmation, campaigns, newsletters |
| **SMS / MMS** | Twilio or Sinch integration | OTPs, day-of reminders, urgent updates |
| **Push (iOS/Android)** | Mobile SDK (Adobe Journey Optimizer SDK) + APNS/FCM credentials | Real-time alerts, promotions, check-in |
| **In-app** | Mobile SDK (web or native) | Contextual messages while user is in app |
| **Web (on-site)** | Web SDK + AJO Web channel | Personalized banners while browsing |
| **Direct mail** | API to print vendor | High-value loyalty member printed offers |

---

## 8. Frequency Capping and Suppression

Without guardrails, a guest could receive 10 messages in one day from different journeys:

```
Business Rules in AJO:
  Global frequency cap: Max 3 marketing emails per week per profile
  Channel cap: Max 1 SMS per day per profile
  
  Per-journey cap: This journey can only run once per profile per 90 days

Suppression lists:
  Unsubscribed from email → all email steps skipped (GDPR / CAN-SPAM compliance)
  SMS opt-out (replied STOP) → all SMS steps skipped
  Push disabled on device → push steps skipped automatically
```

---

## 9. AJO vs Adobe Campaign — Key Distinction

| | Adobe Campaign (Classic) | Adobe Journey Optimizer |
|---|---|---|
| **Data source** | Campaign's own database | AEP Real-Time Customer Profile |
| **Journey model** | Batch-first; real-time bolt-on | Real-time-first; built on streaming AEP events |
| **Personalization** | Campaign database attributes | Full AEP profile + event context |
| **Trigger latency** | Minutes (batch) | Sub-second (streaming event) |
| **Offer engine** | Basic; rule-based | AI-powered Decision Management |
| **Status** | Legacy / being migrated | Adobe's current platform (2022+) |

At Marriott scale, AJO is the forward-looking platform. Adobe Campaign Classic is still in production for many brands migrating from it.

---

## 10. Common Mistakes

| Mistake | Consequence | Fix |
|---|---|---|
| Journey has no exit conditions | Profiles stuck in journey indefinitely; competing journeys can't re-enter | Always set max duration (e.g., 30 days) + event-based exits (booking cancelled) |
| No frequency capping | Guest receives 10 emails in one day from overlapping journeys | Configure global business rules: max N emails per week |
| Hardcoded content instead of personalization tokens | Every guest sees "Dear Customer" instead of their name | Always use `{{profile.person.firstName}}` tokens |
| Audience-triggered journey re-triggers on same profile | If segment evaluation re-adds a profile, they restart the journey | Set "Re-entrance condition: profile can only enter once per 90 days" |
| SMS without opt-in check | Sending SMS to users who never opted in → CAN-SPAM / TCPA violation | Check SMS opt-in flag from profile before sending; AJO has built-in consent check |

---

## 11. Interview Insight

Strong answer:

> Adobe Journey Optimizer is Adobe's real-time omnichannel orchestration platform built on AEP. Journeys are triggered by AEP streaming events — a booking confirmation event fires immediately and the profile enters the "Booking Confirmation Journey." The journey canvas lets you define waits (48h before check-in), conditions (has the app?), and channel actions (email/SMS/push). Decision Management selects the best offer for each profile using eligibility rules and optional AI ranking. Business rules enforce frequency caps so a guest doesn't receive too many messages. The key distinction from Adobe Campaign Classic is that AJO is event-driven and real-time — it reacts to live AEP events, not batch database extracts.

Follow-up trap:

> What happens if a guest is already in a booking journey and they make a second booking?

Good answer:

> By default, AJO can be configured to either allow or block re-entry. If re-entry is allowed, the profile enters the journey again for the second booking — running two instances simultaneously. The second booking event triggers the journey from step one again. This is usually the correct behavior for booking confirmations (each booking needs its own confirmation flow). If you want to prevent re-entry (e.g., for a one-time win-back offer), you set a re-entrance condition: "not if they've entered this journey in the last 90 days."

---

## 12. Revision Notes

- One-line summary: AJO orchestrates personalized multi-channel journeys (email, SMS, push, in-app) triggered by real-time AEP events; Decision Management selects the best offer per profile using AI ranking.
- Three keywords: journey canvas, event-triggered, Decision Management.
- One interview trap: no frequency capping + multiple overlapping journeys = guest receives 10 messages in a day — configure global business rules.
- Memory trick: AJO = the conductor who knows what each guest needs and when, across every channel — one canvas, infinite personalized performances.
