# Akamai — CDN, Geo-IP, EdgeWorkers, Security — Gold Sheet

> Topic: Akamai as the edge layer between the internet and your origin — delivery, security, geo-detection, and edge compute

---

## 1. Intuition

Before a single byte of your website reaches a user's browser, it passes through Akamai. Akamai is a global network of servers sitting at the "edge" — close to users worldwide. It caches your content, detects who the user is (geo, device, bot vs human), protects your origin from attacks, and can even run JavaScript logic before the request ever reaches your servers.

Beginner version:

> Akamai is the world's largest CDN. It sits between users and your servers, making sites fast, secure, and geo-aware.

---

## 2. Definition

- **Definition:** Akamai is a Content Delivery Network (CDN) and edge platform that delivers web content from servers geographically close to users, provides DDoS/bot protection, enforces security policies, and runs edge compute logic via EdgeWorkers.
- **Category:** Edge networking / CDN / security.
- **Core idea:** Move content and compute to the edge — closer to users — to reduce latency, protect origin, and enable real-time decision-making before requests reach application servers.

---

## 3. How CDN Delivery Works

```
Without CDN:
  User in Hyderabad → DNS → Origin server in Virginia (200ms round trip)

With Akamai CDN:
  User in Hyderabad → DNS → Akamai POP in Mumbai (5ms round trip)
                          → Cache HIT: serve directly from edge
                          → Cache MISS: fetch from Virginia origin, cache it, serve
```

**POP (Point of Presence):** Akamai operates 4,100+ servers in 130+ countries. Each POP caches content and serves local users.

### Request flow in detail:

```
1. User types www.marriott.com
2. DNS resolves to Akamai edge IP (not origin IP) → "traffic routing"
3. Akamai edge server receives request
4. Edge checks: Is this cacheable? Is it in cache?
   → Cache HIT: Serve from edge. Origin never sees request.
   → Cache MISS: Forward to origin. Cache response. Serve.
5. Akamai injects response headers with cache status, geo info
6. Browser receives response
```

---

## 4. Caching at the Edge

```
# Controlled by Cache-Control headers from origin:
Cache-Control: public, max-age=86400     # cache for 24 hours at edge
Cache-Control: no-cache                 # validate with origin on every request
Cache-Control: no-store                 # never cache (login pages, cart)

# Akamai also respects:
Surrogate-Control: max-age=3600        # Akamai-specific, stripped before browser
Edge-Control: max-age=3600            # Akamai Edge header for fine-grained control
```

**Cache key:** By default, the full URL is the cache key. Akamai can customize:
```
Default key:  https://www.marriott.com/hotels/new-york?checkIn=2026-07-01&checkOut=2026-07-03
With normalization: cache both dates as one → reduce cache fragmentation
```

**Purge (cache invalidation):**
```bash
# Akamai Fast Purge API — invalidate by URL or cache tag
curl -X DELETE "https://api.ccu.akamai.com/ccu/v3/delete/url/production" \
  -H "Authorization: EG1-HMAC-SHA256 ..." \
  -d '{"objects": ["https://www.marriott.com/hotels/new-york"]}'
```

Cache tags let you purge entire groups of pages (e.g., all hotel pages for a specific property) in one API call.

---

## 5. Geo-IP Detection — Where the MarTech Data Comes From

Akamai detects the user's geographic location from their IP address and injects it as HTTP headers:

```
# Akamai Edgescape headers (injected by edge, available to origin and EdgeWorkers):
X-Akamai-Edgescape: georegion=263,country_code=IN,region_code=TG,city=HYDERABAD,
                    dma=0,pmsa=0,areacode=0,county=0,fips=0,lat=17.3850,long=78.4867,
                    timezone=IST,zip=500001,continent=AS,throughput=vhigh,bw=5000

# Individual headers (configured in Akamai property):
X-Akamai-Country: IN
X-Akamai-State: TG        # ← this is what you saw as "browser_akamai_loc_state: tg"
X-Akamai-City: HYDERABAD
```

The origin server (or an EdgeWorker) reads these headers and exposes them to the page. JavaScript then puts them in the data layer:

```javascript
// Server-side rendering (SSR) or backend reads Akamai headers
// and embeds them in the page HTML or API response:
window.adobeDataLayer.push({
  browser_akamai_loc_country: 'IN',    // from X-Akamai-Country
  browser_akamai_loc_state: 'tg',      // from X-Akamai-State
});
```

**Use cases for geo-IP:**
- Show prices in local currency
- Redirect to language-specific URL (IN visitors → `/en-in/`)
- Enforce geo-blocking (content unavailable in certain countries)
- Personalize promotions by region
- Analytics segmentation by geography

---

## 6. Bot Detection and Protection

Akamai Bot Manager identifies and blocks automated traffic:

```
Bot categories:
  ┌─────────────────────────────────────────────┐
  │ Friendly bots: Googlebot, Bingbot            │
  │   → Allow (they index your content)         │
  ├─────────────────────────────────────────────┤
  │ Scraper bots: price comparison, aggregators │
  │   → Rate limit or serve decoy content       │
  ├─────────────────────────────────────────────┤
  │ Malicious bots: credential stuffing,        │
  │ account takeover, inventory hoarding        │
  │   → Block, CAPTCHA, or honeypot            │
  └─────────────────────────────────────────────┘
```

Detection mechanisms:
- **JavaScript fingerprinting** — Akamai injects invisible JS that measures browser environment
- **Behavioral analysis** — Mouse movements, click patterns, timing between requests
- **IP reputation** — Blocklists of known bot IPs, Tor exit nodes, proxy services
- **Device intelligence** — Does the browser behave like a real browser? Canvas fingerprint?

```
# Response when bot is detected:
HTTP 403 Forbidden (hard block)
HTTP 429 Too Many Requests (rate limit)
302 Redirect to CAPTCHA challenge
Silent fail — serve empty/wrong response
```

---

## 7. DDoS Protection — Prolexic and Kona

```
Akamai Prolexic: Network-level DDoS (Layer 3/4)
  → Absorbs volumetric attacks (Tbps scale)
  → Origin never sees the flood

Akamai Kona Site Defender: Application-level (Layer 7)
  → WAF (Web Application Firewall)
  → Blocks: SQL injection, XSS, CSRF, OWASP Top 10
  → Rate limiting per IP, per user agent, per endpoint
  → Custom rules: "block all requests to /api/rates with > 100 req/min"
```

---

## 8. EdgeWorkers — JavaScript at the Edge

EdgeWorkers let you run JavaScript on Akamai's edge servers — before the request reaches your origin:

```javascript
// EdgeWorker: Redirect mobile users to app download page
import { HtmlRewriter } from 'html-rewriter';

export async function onClientRequest(request) {
  const userAgent = request.getHeader('User-Agent') || '';
  const isMobile = /Mobile|Android|iPhone/i.test(userAgent);
  const country = request.getVariable('PMUSER_COUNTRY');  // from geo-IP

  // A/B test at the edge — no origin involved
  if (isMobile && country === 'IN') {
    request.respondWith(302, {
      'Location': '/in/app-download'
    }, '');
    return;
  }
}

export async function onOriginRequest(request) {
  // Modify request before it goes to origin
  // Add internal headers, rewrite path, etc.
  request.setHeader('X-Edge-Decision', 'variant-b');
}

export async function onClientResponse(response) {
  // Modify response before it reaches the browser
  // Inject headers, add geo data to response
  const country = response.getVariable('PMUSER_COUNTRY');
  response.setHeader('X-Geo-Country', country);
}
```

**EdgeWorker use cases:**
- A/B testing at edge (no origin involved — zero latency)
- URL normalization (case, trailing slash)
- Auth token validation (don't let unauthenticated requests reach origin)
- Personalized redirects (geo, device, experiment)
- Header enrichment (add data for downstream services)

---

## 9. Image and Performance Optimization

Akamai Image Manager:

```
Origin stores: /images/hotel-photo.jpg (5MB, full-resolution)
Akamai serves: /images/hotel-photo.jpg?imwidth=800&imformat=webp
  → Automatically resizes to 800px
  → Converts to WebP for supporting browsers
  → Compresses with quality=85
  → Result: 120KB instead of 5MB
```

This happens at the edge — no origin compute involved, no image variants stored.

---

## 10. Akamai Property Manager — Configuration

All Akamai behavior is configured in **Property Manager** (the Akamai portal):

```
Property: www.marriott.com

Rules (evaluated in order):
  Rule 1: If path = /api/* → Forward to API origin, no-cache, no compression
  Rule 2: If path = /images/* → Cache 7 days, enable Image Manager
  Rule 3: If path = /hotels/* → Cache 1 hour, enable EdgeScape geo injection
  Rule 4: If user-agent = Googlebot → Allow, bypass Bot Manager
  Rule 5: Default → Cache 10 minutes, enable Kona WAF, enable SureRoute

Behaviors per rule:
  - Caching TTL
  - Compression (gzip, brotli)
  - Origin selection (which backend server)
  - Security policy (Bot Manager, WAF)
  - EdgeWorker attachment
  - Response header manipulation
```

Configuration changes are deployed via:
```bash
# Akamai CLI
akamai property activate --property www.marriott.com --network staging
akamai property activate --property www.marriott.com --network production
```

---

## 11. SureRoute — Optimized Origin Connectivity

Without SureRoute: User → Akamai edge → Public internet → Origin (variable path)
With SureRoute: User → Akamai edge → Akamai private backbone → Origin (optimized path)

Reduces latency to origin by 30-50% for non-cacheable requests (dynamic API calls).

---

## 12. Common Mistakes

| Mistake | Consequence | Fix |
|---|---|---|
| Setting `Cache-Control: no-cache` on all assets | CDN is bypassed; every request hits origin; performance degrades | Only no-cache for truly dynamic content (user-specific, authenticated) |
| Not using cache tags | Cache purge requires URL-by-URL invalidation (slow) | Tag responses; purge by tag in one call |
| Not configuring Bot Manager allowlist for Googlebot | Google can't crawl your site — SEO impact | Always allowlist verified search engine bots |
| Using query string in cache key for all parameters | Cache fragmentation: `?source=email` and `?source=google` are different cache entries | Strip or ignore marketing UTM parameters from the cache key |
| Treating geo-IP as 100% accurate | VPN users, corporate proxies show wrong country | Use geo as a signal, not a guarantee; allow user override |

---

## 13. Interview Insight

Strong answer:

> Akamai sits at the edge between the internet and the origin server. It handles four things: content delivery (caching at POPs close to users), geo-IP detection (the Edgescape headers that expose country/state/city to the application), bot/DDoS protection (Bot Manager + Kona WAF), and edge compute (EdgeWorkers for logic that should run before the request reaches the origin). The geo data you see in the data layer — `browser_akamai_loc_country`, `browser_akamai_loc_state` — comes from Akamai's Edgescape service, which maps the user's IP to a geographic location at the edge.

Follow-up trap:

> Why would a user in India see geo-IP data showing a different country?

Good answer:

> Geo-IP from Akamai is based on the egress IP of the request — the IP the request arrives from at the edge. If the user is behind a VPN, corporate proxy, or mobile carrier NAT, the egress IP may map to a different country. Akamai has one of the best geo-IP databases but it's still IP-based inference, not certainty. Critical use cases (like regulatory geo-blocking) need a secondary verification signal like user-declared location or confirmed identity.

---

## 14. Revision Notes

- One-line summary: Akamai is the CDN edge that delivers content fast, detects geo/bots, protects origin, and runs edge compute before requests reach your servers.
- Three keywords: edge, POP, geo-IP.
- One interview trap: Akamai geo-IP is IP-based inference — VPN/proxy users will show wrong country.
- Memory trick: Akamai is the bouncer, the GPS, and the cache clerk — all at the door before users enter your app.
