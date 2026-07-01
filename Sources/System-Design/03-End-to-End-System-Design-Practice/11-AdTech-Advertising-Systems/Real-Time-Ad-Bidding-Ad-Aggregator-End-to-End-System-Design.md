# Real-Time Ad Bidding System / Ad Aggregator - End-to-End System Design

> Goal: design a real-time ad bidding and ad aggregation platform from ad request formation to bidder fanout, auction, creative selection, impression/click tracking, billing, fraud detection, pacing, reporting, HLD, LLD, and machine-coding level auction logic.

---

## How To Use This File

- Use this when the interview asks for real-time bidding, ad exchange, ad aggregator, ad server, DSP/SSP, auction system, sponsored ads, ad marketplace, campaign delivery, or low-latency fanout design.
- Start with the ad request lifecycle, then zoom into bidder fanout, auction logic, targeting, pacing, frequency caps, event tracking, billing, reporting, and fraud.
- Keep one idea sharp: ad serving optimizes for low-latency availability, but budget, billing, and advertiser reporting need correctness and reconciliation.
- In interviews, separate the online auction path from offline/nearline analytics and billing pipelines.

---

## Starter Learning Path

Read this file in four passes:

1. First pass: understand the entities, request flow, and latency budget.
2. Second pass: draw the HLD from memory before reading the full architecture.
3. Third pass: study auction logic, data model, event pipeline, failure modes, and trade-offs.
4. Fourth pass: implement the LLD/machine-coding auction engine without looking.

What a starter should master first:

- The one-line purpose of the system.
- The difference between publisher, SSP, ad exchange, DSP, advertiser, campaign, creative, impression, click, and conversion.
- The online serving path vs offline reporting/billing path.
- Why latency budgets are extremely tight.
- Why the system must degrade gracefully when bidders or profile stores are slow.
- Why event tracking must be durable and deduplicated.

Gold-level self-check:

- You can draw the full request flow in 5 minutes.
- You can explain how bids are collected and timed out.
- You can implement a simple auction engine.
- You can justify first-price vs second-price auction choices.
- You can explain pacing, frequency capping, fraud, privacy, observability, and billing reconciliation.
- You can clearly say which parts are eventually consistent and which parts need stronger correctness.

---

# Master Checklist For This Problem

| Layer | Interview signal | Ad bidding focus |
|---|---|---|
| Problem understanding | Can explain ad marketplace roles | publisher, SSP, exchange, DSP, advertiser, campaign, creative |
| HLD | Can separate online and offline paths | ad request, enrichment, fanout, auction, tracking, billing |
| LLD | Can model auction entities | `BidRequest`, `Bid`, `Campaign`, `Creative`, `AuctionResult` |
| Machine coding | Can implement auction | filters, floors, ranking, tie-break, timeout handling |
| Traffic spikes | Can protect low-latency serving | bidder timeouts, cache fallback, load shedding |
| Scale | Can reason event volume | impressions, clicks, conversions, counters, reporting |
| Correctness | Can separate serving from billing | at-least-once events, dedupe, reconciliation |
| Privacy/security | Can handle consent and fraud | PII controls, consent, bot detection, brand safety |

---

# 1. Problem Understanding

## 1.1 Core Terms

| Term | Meaning |
|---|---|
| Publisher | App/site that has ad inventory, such as a news app or video app |
| Advertiser | Business paying to show ads |
| Campaign | Advertiser budget, targeting, schedule, objective, bid strategy |
| Creative | Actual ad asset: image, video, native card, text, landing URL |
| Ad slot | Placement where an ad can be shown |
| Impression | Ad was shown or considered shown based on measurement rule |
| Click | User clicked the ad |
| Conversion | User performed desired action after click/view |
| SSP | Supply-side platform representing publisher inventory |
| DSP | Demand-side platform bidding on behalf of advertisers |
| Ad exchange | Marketplace that runs auctions between supply and demand |
| Ad aggregator | System that calls multiple ad networks/DSPs and selects best valid ad |
| RTB | Real-time bidding, auction per impression opportunity |
| Floor price | Minimum acceptable price for an ad slot |
| eCPM | Effective cost per thousand impressions |
| Pacing | Spreading budget over time |
| Frequency cap | Limit how often a user sees a campaign/creative |

One-line purpose:

```text
A real-time ad bidding system receives an ad opportunity, gathers eligible bids from advertisers
or external demand partners, runs a low-latency auction, returns a safe creative, and records events
for billing, reporting, optimization, and fraud detection.
```

---

## 1.2 Functional Requirements

Core serving requirements:

- Receive ad requests from web/mobile/server-side publishers.
- Validate publisher, app/site, placement, device, geo, and request context.
- Respect privacy consent and regional regulations.
- Enrich request with safe user/context features when allowed.
- Match eligible campaigns and creatives.
- Fan out bid requests to internal bidders and/or external DSP/ad networks.
- Enforce timeouts per bidder and total auction latency.
- Normalize bid responses from partners.
- Filter invalid, unsafe, expired, blocked, or policy-violating creatives.
- Run auction and choose winner.
- Return ad markup/creative metadata/tracking URLs.
- Track impressions, clicks, conversions, wins, losses, and errors.
- Support advertiser campaign management.
- Support budget, pacing, frequency caps, targeting, reporting, and billing.

Optional requirements to clarify:

- Are we building an ad exchange, DSP, SSP, ad server, or aggregator?
- Are ads display, native, video, audio, search, or sponsored products?
- Is bidding first-price, second-price, fixed price, or hybrid?
- Are external bidders in scope?
- Is user-level personalization allowed?
- What privacy framework/consent signals matter?
- Do we need real-time reporting or delayed reporting?
- Do we bill on impression, click, conversion, or action?

Out of scope unless interviewer asks:

- Full ML model training pipeline.
- Full identity graph implementation.
- Full fraud ML platform.
- Full legal compliance implementation.
- Full creative rendering SDK.

---

## 1.3 Non-Functional Requirements

Latency:

- Ad response must be extremely fast.
- Total server-side ad decision may have about 50-150 ms depending product and network.
- External bidders may get only 20-80 ms to respond.
- Slow bidders should not block the auction.

Availability:

- Ad serving should degrade gracefully.
- If some bidders fail, run auction with remaining bids.
- If personalization fails, use contextual or house ads.
- If no paid ad is available, return no-ad or fallback ad.

Correctness:

- Do not overspend advertiser budgets beyond acceptable tolerance.
- Do not bill duplicate impressions/clicks.
- Do not serve blocked/unsafe creatives.
- Respect frequency caps and privacy constraints as much as the serving model requires.
- Reporting should be reconcilable.

Scalability:

- Ad requests can be massive, often higher than core product transactions.
- Event volume is larger than served ad volume because of requests, bids, wins, impressions, clicks, conversions, and logs.
- Hot publishers, placements, or campaigns can spike suddenly.

Security/privacy:

- Avoid leaking PII to bidders.
- Respect consent and regional rules.
- Protect advertiser/publisher APIs.
- Detect bots, click fraud, impression fraud, and invalid traffic.
- Validate creative URLs and scripts.

---

## 1.4 Scale Assumptions

Example interview-scale assumptions:

| Metric | Assumption |
|---|---:|
| Ad requests/day | 50B |
| Average ad requests/sec | about 580K/sec |
| Peak ad requests/sec | 3M to 5M/sec globally |
| External bidders | 20 to 100 |
| Bidder timeout | 20 to 80 ms |
| Auction p95 server latency | 50 to 150 ms |
| Impression events/day | 20B |
| Click events/day | 100M to 1B |
| Campaigns | millions |
| Creatives | tens/hundreds of millions |
| Reporting delay | near-real-time to hourly |

Back-of-the-envelope:

- `50B requests/day` is about `580K/sec` average.
- At 5x to 10x peak, the system must handle millions of requests/sec.
- If each request fans out to 20 bidders, naive fanout can create tens of millions of outbound calls/sec.
- Online path must use in-memory indexes, caches, precomputed eligibility, and strict timeouts.
- Event ingestion needs durable streaming, partitioning, dedupe, and batch/stream processing.

---

## 1.5 Clarifying Questions To Ask

- Are we serving publisher display ads, search ads, sponsored products, or video ads?
- Is this an ad exchange, ad aggregator, or advertiser-side DSP?
- What is the total latency budget?
- How many bidders can we call per request?
- What auction model should we use?
- What privacy/consent constraints exist?
- Is budget enforcement hard strict or near-real-time with reconciliation?
- Do we need multi-region active-active serving?
- What happens if no bidder responds?
- Are fraud detection and brand safety in scope?

Strong interview framing:

> I will design this as a low-latency online auction path plus durable offline/nearline event pipelines. The online path enriches the request, picks eligible demand, fans out with strict timeouts, runs an auction, returns a creative, and logs events. Budget, billing, reporting, model training, and fraud analytics are handled by streaming and batch pipelines with dedupe and reconciliation.

---

# 2. High-Level Design

## 2.1 Architecture

```text
Publisher Web/Mobile/App Server
  |
  v
Ad SDK / Ad Tag
  |
  v
Edge / CDN / WAF / Bot Defense
  |
  v
Ad Gateway
  |
  +--> Request Validation
  +--> Consent / Privacy Filter
  +--> Context Enrichment
  +--> User/Profile Feature Cache
  +--> Frequency Cap Store
  +--> Campaign Eligibility Service
  +--> Bidder Selection Service
  |
  v
Bid Fanout Service
  |
  +--> Internal Bidder
  +--> External DSP / Ad Network 1
  +--> External DSP / Ad Network 2
  +--> External DSP / Ad Network N
  |
  v
Bid Normalizer + Policy Filter
  |
  v
Auction Engine
  |
  v
Creative Decision / Ad Response Builder
  |
  v
Publisher Client

Event Pipeline:
  Ad requests / bid responses / wins / impressions / clicks / conversions
    -> Event Collector
    -> Kafka/Pulsar/Kinesis
    -> Stream Processing
    -> Budget/Pacing Counters
    -> Reporting Store
    -> Billing/Reconciliation
    -> Fraud Detection
    -> Feature Store / Model Training
```

Online serving path:

```text
ad request
  -> validate publisher and placement
  -> privacy/consent check
  -> enrich context
  -> find eligible campaigns/bidders
  -> fan out with deadlines
  -> collect bids until timeout
  -> filter invalid bids/creatives
  -> run auction
  -> return ad and tracking URLs
  -> emit events
```

Offline/nearline path:

```text
events
  -> durable stream
  -> dedupe and validation
  -> reporting aggregates
  -> budget spend updates
  -> fraud scoring
  -> conversion attribution
  -> model training/features
  -> billing and reconciliation
```

---

## 2.2 Component Responsibilities

| Component | Responsibility |
|---|---|
| Ad SDK / Tag | collects context, calls ad server, renders creative, fires events |
| Ad Gateway | auth, request validation, rate limiting, trace ID, routing |
| Privacy Service | consent enforcement, PII filtering, regional rules |
| Enrichment Service | geo, device, placement, contextual category, user segments |
| Campaign Eligibility Service | finds campaigns that can bid |
| Bidder Selection Service | chooses internal/external bidders to call |
| Bid Fanout Service | parallel outbound bid requests with strict deadlines |
| Bid Normalizer | converts partner responses to internal bid format |
| Policy Filter | creative safety, category blocks, brand safety |
| Auction Engine | selects winner and clearing price |
| Budget/Pacing Service | controls spend rate and budget availability |
| Frequency Cap Service | limits user exposure by campaign/creative |
| Event Collector | durable ingestion of impressions/clicks/conversions |
| Reporting Pipeline | aggregates metrics for advertisers/publishers |
| Billing/Reconciliation | computes billable events and money owed |
| Fraud Detection | identifies invalid traffic and suspicious activity |

---

## 2.3 Ad Request Lifecycle

```text
1. User opens publisher page/app.
2. Ad SDK detects placement that needs an ad.
3. SDK forms request with placementId, publisherId, device/context, consent, and trace ID.
4. Edge/WAF blocks obvious abuse.
5. Ad Gateway validates publisher, placement, request shape, and rate limits.
6. Privacy service removes or blocks restricted user data.
7. Enrichment adds geo, device type, content category, and allowed user segments.
8. Eligibility service finds campaigns/bidders that match placement and targeting.
9. Frequency cap and pacing filter remove ineligible campaigns.
10. Bid fanout sends requests to selected bidders with per-bidder deadlines.
11. Bids arrive or time out.
12. Bid normalizer validates price, currency, creative ID, TTL, and policy metadata.
13. Auction engine chooses winner.
14. Response builder returns creative markup and tracking URLs.
15. SDK renders creative and fires impression/click/conversion events.
16. Event pipeline records data for budget, billing, reporting, fraud, and models.
```

Failure path:

```text
If profile lookup fails -> use contextual targeting.
If some bidders time out -> run auction with available bids.
If all bidders fail -> return house ad/no-ad/fallback.
If event collector is slow -> buffer or drop non-critical debug logs, never block serving.
```

---

# 3. APIs

## 3.1 Ad Request API

```http
POST /v1/ad-requests
Content-Type: application/json
```

Request:

```json
{
  "requestId": "req_123",
  "publisherId": "pub_9",
  "siteId": "site_4",
  "placementId": "slot_home_top",
  "adFormat": "NATIVE",
  "device": {
    "type": "MOBILE",
    "os": "iOS",
    "ip": "redacted-or-hashed",
    "userAgent": "..."
  },
  "context": {
    "url": "https://news.example.com/sports/123",
    "category": "sports",
    "language": "en"
  },
  "user": {
    "userId": "hashed_user_id",
    "segments": ["sports_fan", "travel_intent"]
  },
  "consent": {
    "personalizedAdsAllowed": true,
    "region": "US"
  },
  "floorPriceMicros": 2500000,
  "currency": "USD"
}
```

Response:

```json
{
  "requestId": "req_123",
  "auctionId": "auc_456",
  "ad": {
    "creativeId": "cr_88",
    "campaignId": "cmp_22",
    "advertiserId": "adv_7",
    "format": "NATIVE",
    "title": "Weekend hotel deals",
    "imageUrl": "https://cdn.example.com/cr_88.webp",
    "clickUrl": "https://ads.example.com/click?auctionId=auc_456",
    "impressionUrl": "https://ads.example.com/imp?auctionId=auc_456"
  },
  "ttlMs": 300000
}
```

No fill:

```json
{
  "requestId": "req_123",
  "auctionId": "auc_456",
  "ad": null,
  "reason": "NO_ELIGIBLE_BID"
}
```

---

## 3.2 Bid Request API To External Bidder

```http
POST /bid
Content-Type: application/json
```

Request:

```json
{
  "auctionId": "auc_456",
  "placementId": "slot_home_top",
  "format": "NATIVE",
  "floorPriceMicros": 2500000,
  "currency": "USD",
  "device": {
    "type": "MOBILE",
    "geo": "US-CA"
  },
  "context": {
    "category": "sports",
    "language": "en"
  },
  "user": {
    "segments": ["sports_fan"]
  },
  "timeoutMs": 50
}
```

Response:

```json
{
  "auctionId": "auc_456",
  "bidderId": "dsp_12",
  "bidPriceMicros": 4300000,
  "currency": "USD",
  "creativeId": "cr_88",
  "campaignId": "cmp_22",
  "ttlMs": 300000
}
```

Important:

```text
The ad aggregator must treat missing/late bid responses as no-bid, not as a request failure.
```

---

## 3.3 Event APIs

Impression:

```http
POST /v1/events/impression
```

Click:

```http
POST /v1/events/click
```

Conversion:

```http
POST /v1/events/conversion
```

Event fields:

```json
{
  "eventId": "evt_999",
  "auctionId": "auc_456",
  "requestId": "req_123",
  "publisherId": "pub_9",
  "campaignId": "cmp_22",
  "creativeId": "cr_88",
  "userIdHash": "u_hash",
  "eventType": "IMPRESSION",
  "occurredAt": "2026-07-02T10:00:00Z",
  "metadata": {
    "viewability": 0.75,
    "client": "ios"
  }
}
```

Event API properties:

- accepts duplicates
- returns fast
- writes to durable stream
- deduplicates downstream by `eventId` or `(auctionId, eventType, measurementWindow)`
- never blocks ad serving path

---

## 3.4 Campaign Management APIs

Create campaign:

```http
POST /v1/advertisers/{advertiserId}/campaigns
```

Update budget:

```http
PATCH /v1/campaigns/{campaignId}/budget
```

Upload creative:

```http
POST /v1/campaigns/{campaignId}/creatives
```

Campaign changes:

- write to campaign source-of-truth DB
- validate targeting and budget
- send creative for policy review/scanning
- publish `CampaignUpdated` event
- update serving indexes/caches asynchronously

Wrong option:

```text
Have ad serving query the campaign management database on every request.
```

What fails:

```text
Campaign DB becomes a massive request-path bottleneck.
```

Better:

```text
Compile campaigns into low-latency serving indexes and refresh them through events/CDC.
```

---

# 4. Data Model

## 4.1 Core Entities

```text
Publisher
  publisherId
  name
  status
  allowedFormats

Placement
  placementId
  publisherId
  siteId
  format
  floorPrice
  blockedCategories

Advertiser
  advertiserId
  accountStatus
  billingStatus

Campaign
  campaignId
  advertiserId
  status
  objective
  startTime
  endTime
  totalBudget
  dailyBudget
  bidStrategy
  targetingRules
  pacingMode

Creative
  creativeId
  campaignId
  format
  assetUrl
  landingUrl
  status
  policyLabels

Bid
  auctionId
  bidderId
  campaignId
  creativeId
  priceMicros
  currency
  receivedAt

AuctionResult
  auctionId
  requestId
  winningBidderId
  campaignId
  creativeId
  clearingPriceMicros
  reason

AdEvent
  eventId
  auctionId
  eventType
  occurredAt
  campaignId
  publisherId
  billableStatus
```

---

## 4.2 Storage Choices

| Data | Store | Why |
|---|---|---|
| campaign source of truth | relational DB | transactions, advertiser admin, budget config |
| creative metadata | relational/document DB | flexible creative attributes |
| creative assets | object storage + CDN | large static assets |
| serving campaign index | in-memory/cache/KV/search index | low-latency eligibility |
| user segments/profile | KV/feature store | fast lookup |
| frequency caps | Redis/KV/counter store | low-latency counters |
| pacing/budget counters | Redis/KV + stream reconciliation | fast near-real-time control |
| ad events | Kafka/Pulsar/Kinesis | huge durable event stream |
| reporting aggregates | ClickHouse/Druid/Pinot/BigQuery/Snowflake | analytical queries |
| billing ledger | relational/warehouse with reconciliation | correctness/audit |
| fraud features | stream store/feature store | online and offline scoring |

Source of truth:

```text
Campaign config and billing records are source-of-truth data.
Serving indexes, profile caches, and reporting aggregates are derived.
```

---

## 4.3 Sharding And Partitioning

Serving path:

| Data | Partition Key |
|---|---|
| ad request stream | publisherId or requestId hash |
| campaign serving index | placement/category/geo/ad format |
| frequency caps | userIdHash + campaignId |
| budget counters | campaignId |
| bidder metrics | bidderId |

Event path:

| Event | Partition Key |
|---|---|
| impression | auctionId or campaignId |
| click | auctionId or userIdHash |
| conversion | userIdHash or attribution key |
| billing aggregate | campaignId + day |
| publisher reporting | publisherId + day |

Hot partition risks:

- large publisher
- home page top placement
- viral event page
- massive advertiser campaign
- hot campaign budget counter

Mitigations:

- split hot publisher/placement partitions
- shard budget counters with periodic aggregation
- local edge counters with reconciliation
- bidder selection limits
- adaptive throttling
- sampled debug logs

---

# 5. Auction Design

## 5.1 Auction Inputs

Auction uses:

- eligible bids
- floor price
- currency conversion
- quality score
- creative policy status
- publisher category blocks
- advertiser budget/pacing
- frequency cap
- bid TTL
- predicted click/conversion score if available

Common scoring:

```text
rankScore = bidPrice * qualityScore
```

or:

```text
rankScore = bidPrice * predictedCTR * advertiserValueMultiplier
```

Do not overcomplicate in first interview pass. Start with highest valid bid above floor, then add quality and pacing.

---

## 5.2 First-Price vs Second-Price

First-price auction:

```text
Winner pays their bid price.
```

Pros:

- simpler
- common in modern programmatic ads
- transparent clearing price

Cons:

- bidders need bid shading
- can increase advertiser cost if bidding poorly

Second-price auction:

```text
Winner pays second-highest bid plus increment, subject to floor.
```

Pros:

- historically encouraged truthful bidding

Cons:

- more complexity
- less common in some modern exchange contexts
- hidden fees/auction mechanics can create trust issues

Chosen for this design:

```text
Use first-price auction for simplicity and modern aggregator behavior. Keep auction logs auditable.
```

Wrong option:

```text
Change auction type per request without transparent reporting.
```

What fails:

```text
Advertisers cannot reason about bidding strategy and trust erodes.
```

Better:

```text
Use a clear auction policy by placement/contract and log it in auction result.
```

---

## 5.3 Bidder Fanout And Timeouts

Naive fanout:

```text
Call every bidder for every request.
```

What fails:

- too many outbound requests
- high tail latency
- partner overload
- network cost explosion
- low-quality bidders waste time

Better fanout:

```text
1. Preselect bidders likely to bid for this request.
2. Apply bidder QPS quotas and health scores.
3. Use parallel calls with per-bidder deadlines.
4. Stop waiting at auction deadline.
5. Run auction with bids received in time.
```

Bidder selection signals:

- placement format support
- geo/device/category support
- historical bid rate
- historical win rate
- response latency
- advertiser demand
- budget availability
- bidder health

Wrong option:

```text
Wait for all bidders before choosing winner.
```

What fails:

```text
One slow bidder destroys p99 latency and publisher revenue.
```

Better:

```text
Use strict deadlines and treat late bidders as no-bid.
```

---

# 6. Budget, Pacing, And Frequency Caps

## 6.1 Budget Enforcement

Budget types:

- campaign total budget
- daily budget
- hourly pacing target
- advertiser account credit limit

Hard problem:

```text
Ad serving is massively distributed. Strict global budget checks on every auction can add latency
and reduce availability, but loose checks can overspend.
```

Options:

| Option | Fit | Failure Mode |
|---|---|---|
| strict DB budget check per request | small system | too slow at scale |
| Redis atomic counter | fast regional control | region/counter failure and reconciliation |
| budget token allocation | high-scale distributed serving | token allocation complexity |
| approximate spend with reconciliation | high availability | bounded overspend possible |

Chosen:

```text
Use near-real-time budget counters and pacing tokens for serving, with billing reconciliation from
deduplicated impression/click/conversion events.
```

For finance-like advertiser billing:

```text
The billable ledger/reporting path must dedupe and reconcile. The serving path can use bounded
approximation to avoid latency collapse.
```

---

## 6.2 Pacing

Pacing prevents:

- spending full daily budget in first minute
- starving later traffic
- overloading hot campaigns

Pacing modes:

| Mode | Meaning |
|---|---|
| ASAP | spend as fast as eligible |
| even | spread across time window |
| accelerated | spend faster but controlled |
| custom | advertiser-specific schedule |

Simple formula:

```text
expectedSpendByNow = dailyBudget * elapsedDayFraction
pacingRatio = actualSpend / expectedSpendByNow
```

If pacing ratio is too high:

```text
reduce bid eligibility or lower effective bid
```

If pacing ratio is low:

```text
increase eligibility or bid aggressiveness within policy
```

---

## 6.3 Frequency Caps

Examples:

```text
max 3 impressions per user per campaign per day
max 1 impression per creative per user per hour
```

Storage:

```text
key = userIdHash:campaignId:day
value = impressionCount
TTL = cap window
```

Wrong option:

```text
Enforce frequency cap only in offline reporting.
```

What fails:

```text
Users can be spammed with repeated ads before offline correction.
```

Better:

```text
Use online low-latency counters with TTL and reconcile event logs for reporting.
```

---

# 7. Event Tracking, Billing, And Reporting

## 7.1 Event Pipeline

```text
SDK/browser/server
  -> event collector
  -> Kafka/Pulsar/Kinesis
  -> validation/dedupe
  -> stream aggregates
  -> reporting OLAP store
  -> billing pipeline
  -> fraud detection
  -> feature/model pipeline
```

Event types:

- ad request
- bid request
- bid response
- auction win
- auction loss
- impression
- viewable impression
- click
- conversion
- fraud signal
- billing adjustment

Guarantee:

```text
Assume at-least-once event delivery. Deduplicate downstream.
```

Wrong option:

```text
Bill directly from client impression pixels without dedupe or fraud filtering.
```

What fails:

```text
Refreshes, retries, bots, and duplicate events inflate billing.
```

Better:

```text
Use durable event ingestion, dedupe keys, measurement rules, invalid traffic filtering, and reconciliation.
```

---

## 7.2 Reporting

Advertiser report dimensions:

- campaign
- creative
- country/region
- device
- publisher/category
- hour/day
- impressions
- clicks
- conversions
- spend
- CTR/CVR
- win rate
- average CPM/CPC/CPA

Publisher report dimensions:

- placement
- fill rate
- impressions
- revenue
- eCPM
- latency
- no-fill reason
- bidder demand

Storage:

| Store | Fit |
|---|---|
| ClickHouse/Druid/Pinot | fast rollups and dimensions |
| BigQuery/Snowflake/Redshift | warehouse analytics |
| S3/data lake | raw immutable event archive |
| Redis/KV | near-real-time counters |

---

## 7.3 Billing Correctness

Billing source:

```text
deduplicated, validated, fraud-filtered, policy-compliant billable events
```

Billing may use:

- billable impression
- click
- conversion
- viewable impression
- fixed sponsorship fee

Billing correctness tools:

- event IDs
- auction IDs
- dedupe windows
- fraud invalidation
- publisher/advertiser reconciliation
- immutable raw logs
- adjustment records

Wrong option:

```text
Use in-memory auction winner count as final bill.
```

What fails:

```text
Server crashes, retries, and render failures make auction wins different from billable impressions.
```

Better:

```text
Bill from validated impression/click/conversion events, not merely auction decisions.
```

---

# 8. Privacy, Security, And Fraud

## 8.1 Privacy Controls

Privacy service handles:

- consent flags
- regional policy
- user opt-out
- sensitive category restrictions
- PII redaction
- bidder-specific data sharing rules

If personalized ads are not allowed:

```text
Do not send user segments or identifiers to bidders. Use contextual targeting and non-personalized ads.
```

Wrong option:

```text
Always send raw user ID, IP, and segments to all bidders to improve bid price.
```

What fails:

```text
Privacy violations, regulatory risk, data leakage, and loss of user trust.
```

Better:

```text
Minimize data, honor consent, hash/pseudonymize identifiers where allowed, and apply bidder data-sharing policy.
```

---

## 8.2 Creative Safety

Creative checks:

- malware scan
- landing URL scan
- image/video policy review
- blocked categories
- brand safety labels
- publisher category restrictions
- expiration and approval status

Wrong option:

```text
Allow bidder to return arbitrary JavaScript creative at auction time with no pre-approval.
```

What fails:

```text
Malware, bad UX, policy violations, and publisher trust issues.
```

Better:

```text
Require pre-approved creative IDs or sandboxed/validated markup.
```

---

## 8.3 Fraud And Invalid Traffic

Fraud signals:

- impossible click rates
- data center/proxy traffic
- repeated impressions without viewability
- click farms
- publisher spoofing
- bot user agents
- abnormal conversion patterns
- high request volume from suspicious devices

Mitigations:

- WAF/bot defense
- signed SDK/ad requests
- publisher authentication
- viewability measurement
- fraud scoring
- event dedupe
- delayed billing finalization
- invalid traffic clawbacks

Wrong option:

```text
Optimize only for click-through rate.
```

What fails:

```text
The system may reward clickbait, fraud, poor advertiser ROI, and bad user experience.
```

Better:

```text
Optimize with quality, conversion, fraud, user experience, and advertiser value signals.
```

---

# 9. Reliability And Failure Modes

| Failure | User/Publisher Observes | Mitigation |
|---|---|---|
| bidder timeout | fewer bids, maybe lower revenue | strict deadlines, bidder health scoring |
| profile store down | less personalized ads | contextual fallback |
| campaign cache stale | old campaign served briefly | versioned serving index, kill switch |
| frequency cap store down | cap may be loose | conservative fallback or no personalized ad |
| budget counter lag | bounded overspend | pacing tokens, reconciliation |
| event collector down | lost billing/reporting risk | durable buffering, multi-region collectors |
| creative CDN down | blank ad | asset replication and fallback |
| fraud spike | invalid revenue/cost | bot controls and delayed billing |
| reporting lag | stale dashboard | freshness labels and backfill |
| auction service overloaded | high no-fill/latency | load shedding and fallback ads |

Critical principle:

```text
Do not let non-critical analytics/reporting pipelines block the online ad decision path.
```

---

# 10. Observability

## 10.1 Key Metrics

Serving metrics:

- ad requests/sec
- auction latency p50/p95/p99
- bidder response latency
- bidder timeout rate
- fill rate
- no-fill reason
- win rate by bidder
- revenue/eCPM
- cache hit ratio
- profile lookup latency
- frequency cap latency

Event metrics:

- impression ingestion rate
- click ingestion rate
- event validation failure
- duplicate event rate
- Kafka lag
- DLQ count
- reporting freshness lag
- billing reconciliation mismatch

Fraud/privacy metrics:

- invalid traffic rate
- blocked request rate
- consent-denied request rate
- unsafe creative block count
- publisher spoof detection

---

## 10.2 Trace View

Trace waterfall:

```text
POST /ad-requests                         82ms
  gateway.validate                         3ms
  privacy.filter                           2ms
  enrichment.lookup_geo                    4ms
  profile.lookup                           8ms
  campaign_eligibility                     6ms
  frequency_cap.check                      3ms
  bidder_fanout                           45ms
    dsp_1                                  31ms
    dsp_2 timeout                          50ms
    internal_bidder                        12ms
  bid_normalize                            2ms
  auction.run                              1ms
  response.build                           2ms
  event.publish                            4ms
```

Logs should include:

- `trace_id`
- `request_id`
- `auction_id`
- `publisher_id`
- `placement_id`
- `bidder_id`
- `campaign_id`
- `creative_id`
- `timeout_ms`
- `auction_result`
- `no_fill_reason`
- `privacy_mode`

Wrong option:

```text
Log full user identifiers and raw URLs with sensitive query params for every ad request.
```

What fails:

```text
Privacy and cost issues. Logs become unsafe and too expensive.
```

Better:

```text
Use hashed/pseudonymized IDs, redacted URLs, sampled debug logs, and bounded-cardinality tags.
```

---

# 11. LLD - Object Model

## 11.1 Core Classes

```java
enum AdFormat {
    BANNER,
    NATIVE,
    VIDEO
}

enum BidStatus {
    VALID,
    BELOW_FLOOR,
    EXPIRED,
    CREATIVE_BLOCKED,
    BUDGET_EXHAUSTED,
    FREQUENCY_CAPPED
}

final class BidRequest {
    String requestId;
    String auctionId;
    String publisherId;
    String placementId;
    AdFormat adFormat;
    long floorPriceMicros;
    String currency;
    Map<String, String> context;
    Set<String> userSegments;
}

final class Bid {
    String bidderId;
    String campaignId;
    String creativeId;
    long priceMicros;
    String currency;
    double qualityScore;
    long expiresAtEpochMs;
}

final class Creative {
    String creativeId;
    String campaignId;
    AdFormat format;
    boolean approved;
    Set<String> policyLabels;
}

final class AuctionResult {
    String auctionId;
    Bid winningBid;
    long clearingPriceMicros;
    String reason;
}
```

---

## 11.2 Interfaces

```java
interface BidderClient {
    List<Bid> requestBids(BidRequest request, long timeoutMs);
}

interface CreativePolicyService {
    boolean isAllowed(BidRequest request, Bid bid);
}

interface BudgetService {
    boolean canSpend(String campaignId, long priceMicros);
}

interface FrequencyCapService {
    boolean isAllowed(String userIdHash, String campaignId);
}

interface AuctionEngine {
    AuctionResult runAuction(BidRequest request, List<Bid> bids);
}
```

Design note:

```text
Keep auction logic deterministic and testable. Keep network fanout, cache access, and event logging
outside the pure auction function where possible.
```

---

# 12. Machine-Coding Layer - Auction Engine

## 12.1 Simple First-Price Auction

```java
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

final class FirstPriceAuctionEngine implements AuctionEngine {
    private final CreativePolicyService creativePolicyService;
    private final BudgetService budgetService;

    FirstPriceAuctionEngine(
            CreativePolicyService creativePolicyService,
            BudgetService budgetService
    ) {
        this.creativePolicyService = creativePolicyService;
        this.budgetService = budgetService;
    }

    @Override
    public AuctionResult runAuction(BidRequest request, List<Bid> bids) {
        long now = System.currentTimeMillis();

        Optional<Bid> winner = bids.stream()
                .filter(bid -> bid.priceMicros >= request.floorPriceMicros)
                .filter(bid -> request.currency.equals(bid.currency))
                .filter(bid -> bid.expiresAtEpochMs > now)
                .filter(bid -> creativePolicyService.isAllowed(request, bid))
                .filter(bid -> budgetService.canSpend(bid.campaignId, bid.priceMicros))
                .max(Comparator
                        .comparingDouble((Bid bid) -> bid.priceMicros * bid.qualityScore)
                        .thenComparing(bid -> bid.priceMicros));

        AuctionResult result = new AuctionResult();
        result.auctionId = request.auctionId;

        if (winner.isEmpty()) {
            result.reason = "NO_ELIGIBLE_BID";
            result.clearingPriceMicros = 0;
            return result;
        }

        result.winningBid = winner.get();
        result.clearingPriceMicros = winner.get().priceMicros;
        result.reason = "WINNER_SELECTED";
        return result;
    }
}
```

What this demonstrates:

- floor price enforcement
- currency validation
- bid expiry
- creative policy
- budget check
- quality-adjusted ranking
- deterministic winner selection

Production additions:

- bidder timeout handling
- second-price option if needed
- frequency cap check
- pacing score
- deal priority
- publisher blocks
- advertiser exclusions
- audit logging
- explainable no-fill reasons

---

## 12.2 Fanout With Deadlines - Pseudocode

```text
deadline = now + totalAuctionTimeoutMs
selectedBidders = bidderSelector.choose(request)

for bidder in selectedBidders in parallel:
    bidderTimeout = min(bidder.defaultTimeout, deadline - now)
    send bid request

collect responses until deadline
ignore late responses
normalize valid responses
run auction
emit auction diagnostics
return response
```

Wrong option:

```text
Let each bidder use its own timeout without a global auction deadline.
```

What fails:

```text
Total latency becomes unbounded and p99 becomes terrible.
```

Better:

```text
Use a global deadline and per-bidder deadline derived from it.
```

---

# 13. Traffic Spikes And Capacity

Spike examples:

- major sports event
- app home page traffic surge
- breaking news publisher traffic
- advertiser campaign launch
- bot attack
- bidder outage causing retries

Protection:

- CDN for creative assets
- edge rate limiting
- request sampling for debug logs
- bidder fanout caps
- health-based bidder suppression
- auction deadline
- local campaign index cache
- profile fallback
- no-ad/house-ad fallback
- event collector buffering
- partitioned Kafka topics
- autoscaling by request rate and latency

Wrong option:

```text
During spike, retry timed-out bidder calls aggressively.
```

What fails:

```text
Creates a retry storm and worsens partner outage.
```

Better:

```text
Use circuit breakers, bidder health scores, and temporary suppression.
```

---

# 14. CAP And Consistency Choices

Availability-first:

- online ad serving
- bidder fanout
- contextual fallback
- profile enrichment
- serving campaign index
- reporting freshness

Stronger correctness:

- campaign budget source of truth
- advertiser billing
- publisher payout
- fraud-adjusted billable events
- creative approval status
- privacy consent enforcement

Ad serving CAP answer:

```text
The online auction path should favor availability and low latency. It can use cached campaign indexes,
approximate pacing counters, and fallback ads. But billing and budget reconciliation need durable
event logs, dedupe, auditability, and stronger correctness.
```

Wrong option:

```text
Use one strict relational transaction for every ad request, budget update, impression, and report.
```

What fails:

```text
The online path cannot meet latency or throughput requirements.
```

Better:

```text
Use low-latency serving state for auction decisions and durable event pipelines for billing/reporting reconciliation.
```

---

# 15. Final Architecture Choice

Chosen design:

```text
Use a low-latency ad gateway and auction service backed by in-memory/cached serving indexes. Enrich
requests only with privacy-allowed context, preselect bidders, fan out in parallel with strict
deadlines, normalize bids, filter creatives, run a first-price auction, and return an ad or no-fill
quickly. Use durable streaming for impressions, clicks, conversions, spend, reporting, fraud, and
billing. Use near-real-time budget and frequency counters for serving, then reconcile billable events
from deduplicated logs.
```

Why this is right:

- online path meets tight latency
- slow bidders cannot block auctions
- campaign DB is not in hot path
- privacy and creative safety are explicit
- event stream supports replay and reconciliation
- budget/frequency controls are fast but auditable
- reporting and billing are decoupled from serving

Rejected designs:

| Wrong Design | Why Rejected |
|---|---|
| query campaign DB per request | too slow and not scalable |
| wait for all bidders | p99 latency disaster |
| call every bidder always | outbound explosion and low efficiency |
| bill from auction wins only | ad may not render; duplicates/fraud ignored |
| send PII to all bidders | privacy and compliance risk |
| no creative pre-approval | malware/policy risk |
| strict global budget DB check per request | latency and availability collapse |

---

# 16. Strong Interview Answer

```text
I would design the ad bidding system as a low-latency online auction path plus durable event and
billing pipelines. The publisher SDK sends an ad request with placement, context, and consent. The
gateway validates it, applies privacy rules, enriches safe context, checks frequency and campaign
eligibility from low-latency caches, then selects a small set of likely bidders. The fanout service
calls bidders in parallel with a global auction deadline, treats late responses as no-bid, normalizes
valid responses, filters unsafe creatives, and runs a first-price auction.

I would keep campaign management and reporting databases out of the hot path. Campaign updates
compile into serving indexes. Events such as auctions, impressions, clicks, and conversions go to
Kafka/Pulsar/Kinesis for dedupe, reporting, fraud detection, billing, and model training. Budget and
pacing use fast near-real-time counters or token allocation for serving, with final billing from
deduplicated validated events. The main trade-off is low-latency availability in the auction path
versus stronger correctness in billing and reconciliation.
```

---

# 17. Final Interview Playbook

## 30-Second Version

```text
An ad bidding system receives an ad opportunity, enriches it with privacy-safe context, finds
eligible demand, fans out to selected bidders with strict timeouts, runs an auction, returns a
creative, and logs events for billing/reporting. The hot path is optimized for low latency and
availability. Billing, reporting, fraud, and models are handled by durable event pipelines with
dedupe and reconciliation.
```

## Common Follow-Ups

| Follow-up | Strong Direction |
|---|---|
| How do you handle bidder timeout? | deadline, no-bid, health score, circuit breaker |
| How do you prevent overspend? | pacing counters/tokens plus reconciliation |
| How do you enforce frequency cap? | low-latency per user/campaign counters with TTL |
| How do you bill correctly? | deduped valid events, not raw wins |
| How do you scale event volume? | partitioned stream, OLAP aggregates, data lake |
| How do you protect privacy? | consent gate, data minimization, bidder policy |
| How do you handle no fill? | fallback ad, house ad, or null response |
| How do you avoid slow campaign DB? | compiled serving indexes and caches |
| How do you detect fraud? | bot signals, event anomalies, delayed billing finalization |

## Fast Recall

- One-line summary: RTB is a deadline-driven auction plus a durable event/billing machine.
- Three keywords: fanout, auction, event pipeline.
- One bottleneck: bidder tail latency.
- One correctness trap: billing from undeduplicated client events.
- One privacy trap: leaking raw user identity to bidders.
- One design trap: querying campaign DB on every ad request.

