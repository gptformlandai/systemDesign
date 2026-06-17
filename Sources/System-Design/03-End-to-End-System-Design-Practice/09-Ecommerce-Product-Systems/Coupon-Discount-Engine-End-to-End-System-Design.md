# Coupon / Discount Engine - End-to-End System Design

> Goal: practice one complete pricing-rule system from requirements to HLD, LLD, machine coding, rule evaluation, usage limits, concurrency, and scale.

---

## How To Use This File

- Use this when the interview asks for coupon engine, promotion engine, discount service, pricing rules, campaign system, or offers platform.
- Start with coupon validation, then cover eligibility, rule evaluation, stacking, usage limits, redemption atomicity, audit, and experimentation.
- Keep one idea sharp: discount preview and discount redemption are different. Preview can be approximate; redemption must be concurrency-safe.
- In interviews, separate rule configuration, rule evaluation, and usage counter mutation.

---

## Starter Learning Path

Read this file in four passes:

1. First pass: understand the product goal, core requirements, and the user-facing workflow.
2. Second pass: trace the main read/write path through the high-level design.
3. Third pass: study the data model, scaling choices, failures, and trade-offs.
4. Fourth pass: practice the LLD, machine-coding layer, and final interview playbook without looking.

What a starter should master first:

- The one-line purpose of the system.
- The core entities and APIs.
- The main request flow.
- The storage choice and why it fits.
- The biggest bottleneck.
- The failure that most affects users.
- The trade-off you would defend in an interview.

Gold-level self-check:

- You can draw the architecture from memory in 5 minutes.
- You can explain the happy path and one failure path clearly.
- You can justify consistency, latency, availability, and cost choices.
- You can name what you would simplify for an MVP and what you would add at scale.
- You can answer follow-ups about spikes, retries, idempotency, observability, and data growth.

---

# Master Checklist For This Problem

| Layer | Interview signal | Discount engine focus |
|---|---|---|
| Problem understanding | Can define promotion scope | coupon, automatic discount, eligibility, stacking, redemption |
| HLD | Can design rule platform | rule store, evaluator, usage counters, pricing integration |
| LLD | Can model flexible rules | `Coupon`, `DiscountRule`, `Condition`, `Action`, `Redemption` |
| Machine coding | Can implement evaluator | validate conditions, compute discount, enforce max limits |
| Traffic spikes | Can protect campaigns | flash sale coupons, counter contention, abuse |
| Scale | Can reason consistency | cached rules, atomic counters, audit events, regional campaigns |

---

# 1. Problem Understanding

## 1.1 Functional Requirements

Core requirements:

- Create coupon/promotion campaigns.
- Validate coupon code for a cart/user.
- Apply automatic discounts when eligible.
- Support percentage discount.
- Support fixed amount discount.
- Support free shipping.
- Support buy-one-get-one or item-specific discount.
- Enforce start/end time.
- Enforce minimum cart value.
- Enforce per-user usage limit.
- Enforce global usage limit.
- Enforce stackability rules.
- Return discount breakdown to cart/checkout.
- Record redemption at checkout.

Optional requirements to clarify:

- Are coupons typed by code, user, product, category, seller, or payment method?
- Are automatic promotions in scope?
- Should discounts stack?
- Is best-offer selection required?
- Is experiment/A-B testing in scope?
- Are refunds and coupon reversal in scope?
- Are seller-funded and platform-funded discounts separate?

Out of scope unless interviewer asks:

- Full tax calculation.
- Full payment system.
- Full fraud detection.
- Full marketing campaign UI.

## 1.2 Non-Functional Requirements

Correctness:

- Do not exceed global coupon usage limit.
- Do not exceed per-user limit.
- Do not apply expired/ineligible coupon.
- Do not allow discount greater than allowed cap.
- Final checkout redemption must be atomic.

Performance:

- Coupon validation should be low latency.
- Cart preview should not block on heavy rule scans.
- Hot campaigns should avoid database bottlenecks.

Availability:

- Coupon preview can degrade.
- Checkout redemption must fail closed for risky coupons.

Auditability:

- Track why a coupon was accepted or rejected.
- Store applied discount snapshot on order.
- Store redemption records for finance and abuse investigation.

Security/abuse:

- Prevent brute force coupon guessing.
- Rate limit coupon attempts.
- Detect repeated abuse across accounts/devices.

## 1.3 Constraints

- Marketing teams need flexible rules.
- Rules change frequently.
- Users can preview coupons many times before checkout.
- Usage counters are high-contention during campaigns.
- Cart contents and prices can change between preview and checkout.
- Business teams need explainable discount decisions.

## 1.4 Scale Assumptions

Example interview-scale assumptions:

| Metric | Assumption |
|---|---|
| Active campaigns | 100K+ |
| Coupon validation requests/day | 1B |
| Peak validation/sec | 100K+ |
| Checkout redemptions/day | 50M |
| Hot coupon peak/sec | 20K+ |
| Rule update frequency | thousands/day |
| P95 validation latency | under 100 ms |

Back-of-the-envelope:

- Most validation requests do not become orders.
- Rule reads are much more frequent than rule writes.
- Redemption writes are fewer but require stronger consistency.
- Hot global counters can become the bottleneck.

## 1.5 Clarifying Questions To Ask

- Are discounts applied at cart preview, checkout, or both?
- Do coupons reserve usage during checkout or redeem only after payment success?
- Can multiple coupons stack?
- What are the per-user and global limits?
- Do we need partial rollback on payment failure?
- Should coupon usage be restored after cancellation/refund?
- Are seller-funded promotions required?

Strong interview framing:

> I will separate preview from redemption. Preview evaluates eligibility and shows estimated discount. Redemption happens during checkout with atomic usage-counter updates and stores a discount snapshot on the order.

---

# 2. High-Level Design

## 2.1 Architecture

```text
Admin Portal
  -> Promotion Management API
  -> Rule Store
  -> Rule Cache / Distribution

Cart / Checkout
  -> Discount API
      |
      +--> Rule Resolver
      +--> Eligibility Evaluator
      +--> Stackability Engine
      +--> Usage Counter Service
      +--> Redemption Store
      +--> Audit/Event Publisher

Event Bus:
  PromotionCreated
  PromotionUpdated
  CouponPreviewed
  CouponRedeemed
  CouponRejected
  CouponReleased
```

Preview path:

```text
Cart Service -> Discount API -> cached rules -> evaluate cart/user -> discount breakdown
```

Checkout redemption path:

```text
Checkout Service -> Discount API
  -> load rules
  -> re-evaluate eligibility
  -> atomically increment usage counters
  -> create redemption record
  -> return final discount snapshot
```

## 2.2 APIs

Validate/preview:

```http
POST /v1/discounts/preview

{
  "userId": "user_1",
  "cartId": "cart_1",
  "couponCodes": ["SAVE10"],
  "items": [
    {"skuId": "sku_1", "categoryId": "books", "quantity": 2, "price": 50000}
  ],
  "subtotal": 100000,
  "paymentMethod": "CARD"
}
```

Redeem:

```http
POST /v1/discounts/redeem
Idempotency-Key: order_1_discount

{
  "orderId": "ord_1",
  "userId": "user_1",
  "cartSnapshotId": "snap_1",
  "couponCodes": ["SAVE10"]
}
```

Release/reverse:

```http
POST /v1/discounts/redemptions/{redemptionId}/release
```

Admin create campaign:

```http
POST /v1/promotions

{
  "code": "SAVE10",
  "type": "PERCENT",
  "value": 10,
  "maxDiscount": 20000,
  "minSubtotal": 100000,
  "globalLimit": 100000,
  "perUserLimit": 1,
  "startsAt": "2026-06-17T00:00:00Z",
  "endsAt": "2026-06-30T23:59:59Z"
}
```

## 2.3 Core Components

### Component Responsibility Map

| Component | Responsibility |
|---|---|
| Promotion Management | create/update/disable campaigns |
| Rule Store | durable promotion definitions |
| Rule Cache | fast rule lookup |
| Rule Resolver | finds candidate rules/coupons |
| Eligibility Evaluator | checks user/cart/item/payment conditions |
| Discount Calculator | computes amount and caps |
| Stackability Engine | handles conflicts and best offer |
| Usage Counter Service | per-user/global atomic limits |
| Redemption Store | records final usage |
| Audit Publisher | emits decisions and changes |

### Rule Model

Promotion:

```text
Promotion(
  promotionId,
  code,
  status,
  startsAt,
  endsAt,
  conditions,
  actions,
  priority,
  stackGroup,
  globalLimit,
  perUserLimit
)
```

Condition examples:

- user segment
- minimum subtotal
- product/category/seller eligibility
- payment method
- first order only
- region/pincode
- start/end time

Action examples:

- percentage off
- fixed amount off
- free shipping
- item-level discount
- buy X get Y

### Preview Versus Redemption

Preview:

- Called frequently.
- Can use cached rules.
- Can return "estimated" discount.
- Should not permanently consume usage.

Redemption:

- Called during checkout.
- Must revalidate against latest rule state.
- Must atomically check and increment counters.
- Must write redemption record.
- Must be idempotent per order.

### Stackability

Rules:

| Stack rule | Meaning |
|---|---|
| exclusive | cannot combine with any other discount |
| same group exclusive | only one discount from same group |
| stackable | can combine if caps allow |
| best offer | evaluate many, choose highest savings |

Common approach:

1. Evaluate all eligible promotions.
2. Group by stack group.
3. Pick highest priority or best savings per group.
4. Apply caps.
5. Ensure total discount does not exceed payable amount.

## 2.4 Data Layer

Tables/documents:

```text
Promotion(promotionId, code, status, startsAt, endsAt, priority)
PromotionCondition(conditionId, promotionId, type, config)
PromotionAction(actionId, promotionId, type, config)
UsageCounter(promotionId, usedCount, version)
UserUsageCounter(promotionId, userId, usedCount, version)
Redemption(redemptionId, promotionId, userId, orderId, amount, status)
PromotionAudit(auditId, promotionId, actor, change, timestamp)
```

Storage choices:

| Data | Storage |
|---|---|
| promotion definitions | relational/document DB |
| rule cache | Redis/local cache |
| usage counters | strongly consistent DB/KV |
| redemption records | relational DB |
| audit events | append-only log |
| analytics | data lake |

Indexes:

- `code -> promotion`
- `status + time -> active promotions`
- `promotionId + userId -> user usage`
- `orderId -> redemption`

## 2.5 Scalability

Read path:

- Cache active rule definitions.
- Pre-index coupons by code.
- Pre-index automatic promotions by category/user segment.
- Use local in-memory cache in discount service.

Write path:

- Redemption needs atomic counters.
- Use optimistic locking, conditional updates, or Redis Lua-like atomic scripts.
- Store idempotency record by `orderId` or `idempotencyKey`.

Hot coupon mitigation:

- Bucket counters by promotion and region if business allows.
- Pre-allocate usage quota to shards.
- Queue redemptions for extreme flash campaigns.
- Use admission control when remaining count is low.

## 2.6 Performance

Latency budget:

| Step | Target |
|---|---|
| rule lookup | 1 to 10 ms from cache |
| eligibility evaluation | 5 to 30 ms |
| counter check for preview | optional/approx |
| redemption counter update | 10 to 50 ms |
| total preview | under 100 ms typical |

Optimization rules:

- Compile rules into evaluable structures.
- Avoid scanning all campaigns.
- Filter by code, category, region, and active time first.
- Use short-circuit evaluation for failed conditions.
- Keep rule evaluation deterministic and explainable.

## 2.7 Async Systems

Events:

| Event | Consumers |
|---|---|
| PromotionCreated | cache warmer |
| PromotionUpdated | cache invalidator |
| CouponPreviewed | analytics/fraud |
| CouponRedeemed | finance, campaign analytics |
| CouponRejected | abuse detection |
| CouponReleased | counter repair/reconciliation |

Rules:

- Admin rule updates should publish invalidation events.
- Redemption events should be idempotent.
- Analytics should not block checkout.

## 2.8 Safety And Failure Handling

| Failure | Handling |
|---|---|
| stale rule cache | revalidate from source during redemption for sensitive campaigns |
| duplicate redeem request | idempotency key/order ID |
| global limit race | atomic conditional counter update |
| per-user limit race | atomic user counter update |
| payment fails after redeem | release or mark redemption cancelled |
| rule disabled during checkout | checkout revalidation decides |
| coupon brute force | rate limit and hide exact reason if needed |
| counter mismatch | reconciliation from redemption records |

## 2.9 Observability

Metrics:

- preview request rate
- redeem request rate
- coupon acceptance/rejection rate
- rejection reason counts
- cache hit rate
- counter conflict rate
- redemption latency
- hot coupon QPS
- usage remaining per campaign

Logs:

- `requestId`
- `userId`
- `cartId`
- `orderId`
- `couponCode`
- `promotionId`
- `decision`
- `rejectionReason`
- `discountAmount`

Alerts:

- redemption failure spike
- counter conflict spike
- hot coupon near exhaustion
- cache invalidation lag
- rule evaluation latency spike
- abuse/brute force spike

## 2.10 Tradeoffs

| Choice | Pros | Cons |
|---|---|---|
| flexible rule engine | business agility | complexity and debugging cost |
| hardcoded promotions | fast/simple | low flexibility |
| cached rules | low latency | stale rule risk |
| source-of-truth revalidation | correctness | higher checkout latency |
| exact global counters | strict limits | contention on hot coupons |
| preallocated quotas | scalable | can underutilize total quota |

---

# 3. Low-Level Design

## 3.1 Object Modelling

```text
Promotion
CouponCode
Condition
Action
DiscountResult
CartContext
UserContext
RuleEvaluator
StackabilityPolicy
UsageCounter
Redemption
```

## 3.2 OOP Fundamentals

Encapsulation:

- `Promotion` owns metadata and active-window checks.
- `Condition` owns eligibility logic.
- `Action` owns discount calculation.
- `UsageCounterService` owns atomic limits.

Polymorphism:

- `Condition` implementations: min subtotal, category, payment method, user segment.
- `Action` implementations: percent off, fixed off, free shipping.

Composition:

- `DiscountEngine` composes rule resolver, evaluator, stackability policy, counter service, and redemption store.

## 3.3 Design Patterns

| Pattern | Use |
|---|---|
| Strategy | condition/action implementations |
| Chain of Responsibility | evaluate conditions in order |
| Specification | eligibility rules |
| Composite | combine conditions with AND/OR |
| Repository | rule and redemption persistence |
| Factory | create rule objects from configs |

## 3.4 Sequence Diagram

Preview:

```text
CartService
  -> DiscountEngine: preview(cart, user, coupons)
  -> RuleResolver: find candidate promotions
  -> Evaluator: check eligibility
  -> Calculator: compute discounts
  -> StackabilityPolicy: choose final set
  -> CartService: discount breakdown
```

Redeem:

```text
CheckoutService
  -> DiscountEngine: redeem(order, cartSnapshot, coupons)
  -> Evaluator: recheck eligibility
  -> UsageCounterService: atomic increment if under limits
  -> RedemptionStore: create redemption
  -> CheckoutService: final discount snapshot
```

## 3.5 Edge Cases

- Coupon expired after preview.
- Cart subtotal drops below minimum.
- Same coupon applied twice.
- Two devices redeem same one-time coupon.
- Global limit has one redemption left and many concurrent requests arrive.
- Discount exceeds payable amount.
- Coupon applies only to some items.
- Payment fails after redemption.
- Refund should or should not restore coupon usage depending policy.
- Automatic promotion conflicts with manual coupon.

---

# 4. Machine Coding Layer

## 4.1 Code Structure

```text
models/
  promotion.py
  cart_context.py
  discount_result.py
rules/
  conditions.py
  actions.py
services/
  discount_engine.py
  usage_counter_service.py
repositories/
  promotion_repository.py
  redemption_repository.py
```

## 4.2 Core Logic Implementation

Small discount engine with atomic redemption counter:

```python
from dataclasses import dataclass
from threading import Lock
from time import time


@dataclass(frozen=True)
class Cart:
    user_id: str
    subtotal: int
    category_ids: set[str]


@dataclass(frozen=True)
class Promotion:
    promotion_id: str
    code: str
    percent_off: int
    max_discount: int
    min_subtotal: int
    global_limit: int
    per_user_limit: int
    starts_at: float
    ends_at: float


@dataclass(frozen=True)
class DiscountResult:
    promotion_id: str
    amount: int
    reason: str


class UsageCounterService:
    def __init__(self) -> None:
        self.global_used: dict[str, int] = {}
        self.user_used: dict[tuple[str, str], int] = {}
        self.lock = Lock()

    def redeem(self, promotion: Promotion, user_id: str) -> bool:
        with self.lock:
            global_count = self.global_used.get(promotion.promotion_id, 0)
            user_key = (promotion.promotion_id, user_id)
            user_count = self.user_used.get(user_key, 0)

            if global_count >= promotion.global_limit:
                return False
            if user_count >= promotion.per_user_limit:
                return False

            self.global_used[promotion.promotion_id] = global_count + 1
            self.user_used[user_key] = user_count + 1
            return True


class DiscountEngine:
    def __init__(self, promotions: dict[str, Promotion], counters: UsageCounterService) -> None:
        self.promotions = promotions
        self.counters = counters

    def preview(self, cart: Cart, code: str, now: float | None = None) -> DiscountResult:
        now = now or time()
        promotion = self.promotions.get(code)
        if promotion is None:
            return DiscountResult("", 0, "coupon not found")

        eligible, reason = self._eligible(cart, promotion, now)
        if not eligible:
            return DiscountResult(promotion.promotion_id, 0, reason)

        discount = cart.subtotal * promotion.percent_off // 100
        discount = min(discount, promotion.max_discount, cart.subtotal)
        return DiscountResult(promotion.promotion_id, discount, "eligible")

    def redeem(self, cart: Cart, code: str, now: float | None = None) -> DiscountResult:
        result = self.preview(cart, code, now)
        if result.amount <= 0:
            return result

        promotion = self.promotions[code]
        if not self.counters.redeem(promotion, cart.user_id):
            return DiscountResult(promotion.promotion_id, 0, "usage limit reached")

        return result

    def _eligible(self, cart: Cart, promotion: Promotion, now: float) -> tuple[bool, str]:
        if now < promotion.starts_at or now > promotion.ends_at:
            return False, "coupon inactive"
        if cart.subtotal < promotion.min_subtotal:
            return False, "minimum subtotal not met"
        return True, "eligible"
```

What this demonstrates:

- Preview evaluates eligibility and computes discount.
- Redemption reuses eligibility but also increments counters atomically.
- Global and per-user limits are protected by one critical section.
- Production systems would use DB conditional updates or Redis Lua-like atomic operations.

## 4.3 Testing Thinking

Test cases:

- Unknown coupon returns rejection.
- Expired coupon returns rejection.
- Minimum subtotal failure returns rejection.
- Percentage discount respects max cap.
- Discount never exceeds subtotal.
- Per-user limit blocks second redemption.
- Global limit blocks concurrent redemptions beyond cap.
- Payment failure can release redemption if policy allows.

---

# 5. Handling Abnormal Traffic Spikes

## 5.1 Spike Types

- Viral coupon code.
- Flash sale global coupon limit.
- Brute force coupon guessing.
- Admin updates many campaigns.
- Checkout burst on campaign start.
- Counter hot key on one coupon.

## 5.2 Immediate Response

- Cache rules aggressively.
- Rate limit invalid coupon attempts.
- Use atomic counter operations.
- Pre-warm hot campaign rules.
- Queue or throttle redemptions for extreme hot coupons.
- Use circuit breaker for rule store if cache is fresh enough.

## 5.3 Degradation Policy

| Situation | Degradation |
|---|---|
| preview service slow | show cart without coupon preview |
| rule cache stale | force source revalidation at checkout |
| counter service overloaded | fail closed for limited coupon |
| analytics down | still allow redemption, retry event async |
| admin update delayed | old rule remains until propagation |

## 5.4 Spike Interview Answer

> During a hot coupon spike, I would serve preview from cached rules but make checkout redemption perform an atomic counter update. For very hot global limits, I would consider quota preallocation or a redemption queue. I would rate limit brute force attempts and store redemption records so counters can be reconciled.

---

# 6. Scaling Beyond One Region

## 6.1 Global Campaigns

Challenges:

- One global limit across regions creates counter contention.
- Rules must propagate quickly.
- Different regions may have different legal/promo constraints.
- Currency and tax rules can affect discount calculations.

Options:

| Option | Use |
|---|---|
| single global counter | strict limit, higher latency |
| regional quotas | scalable, may leave unused quota |
| async approximate counters | fast, can overshoot |
| redemption queue | strict but adds wait time |

## 6.2 Recommended Approach

- Use regional campaigns when possible.
- For global campaigns, preallocate quota per region.
- Keep rule definitions versioned.
- Store the rule version used on each redemption.
- Reconcile counters from redemption records.

## 6.3 Interview Answer

> For global scale, I would cache rules regionally and version every promotion. Preview is local and fast. Redemption must be atomic against either a regional quota or a global counter, depending strictness. Each order stores the applied rule version and discount snapshot so finance and support can explain the decision later.

---

# 7. Final Interview Playbook

Start with:

> A discount engine has two paths: preview and redemption. Preview is read-heavy and cacheable; redemption is write-critical because limits and financial impact must be correct.

Then cover:

1. Coupon and automatic promotion requirements.
2. Rule model: conditions and actions.
3. Preview versus redemption.
4. Stackability and best offer.
5. Atomic global/per-user counters.
6. Discount snapshot on order.
7. Failure handling and abuse prevention.
8. Hot coupon scale.

Common traps:

- Treating preview as final redemption.
- Forgetting per-user/global usage limits.
- Applying stale coupon after checkout changes.
- Allowing discount greater than payable amount.
- Ignoring stacking conflicts.
- Forgetting audit/explainability.

---

# 8. Fast Recall Rules

- Preview is not redemption.
- Checkout must revalidate coupon eligibility.
- Usage counters need atomicity.
- Store discount snapshot on order.
- Rules should be versioned.
- Cache rule reads, protect redemption writes.
- Stackability must be explicit.
- Discount cannot exceed payable amount.
- Hot coupons create counter hot keys.
- Reconcile counters from redemption records.

