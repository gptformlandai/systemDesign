# Python Time, Money, UUID, and Locale Correctness - Gold Sheet

> **Track File #12c - Group 2: Intermediate Backend**
> For: backend Python | Level: production data correctness

---

## 1. Why This Topic Matters

Many production outages are not caused by algorithms. They are caused by "small" values being handled casually:

- money stored as `float`
- naive datetimes crossing time zones
- DST making "add one day" wrong
- unstable IDs causing duplicate records
- locale formatting leaking into APIs

This sheet teaches the boring correctness that senior engineers are expected to protect.

---

## 2. Money

### Rule

Never use `float` for money.

Bad:

```python
price = 0.1 + 0.2
print(price)  # 0.30000000000000004
```

Use one of:

| Representation | Best For |
|---|---|
| integer minor units | payments, ledgers, APIs |
| `Decimal` | financial calculations, tax/interest |
| database `NUMERIC/DECIMAL` | persisted money values |

### Integer Minor Units

```python
from dataclasses import dataclass


@dataclass(frozen=True)
class Money:
    amount_cents: int
    currency: str

    def add(self, other: "Money") -> "Money":
        if self.currency != other.currency:
            raise ValueError("currency mismatch")
        return Money(self.amount_cents + other.amount_cents, self.currency)
```

This is excellent for:

- USD cents
- ledger entries
- idempotent payment APIs

But be careful:

- not all currencies have 2 decimal places
- crypto/token assets may need more precision

### Decimal

```python
from decimal import Decimal, ROUND_HALF_UP


def calculate_total(price: Decimal, tax_rate: Decimal) -> Decimal:
    total = price * (Decimal("1") + tax_rate)
    return total.quantize(Decimal("0.01"), rounding=ROUND_HALF_UP)


print(calculate_total(Decimal("19.99"), Decimal("0.0825")))
```

Rules:

- Construct `Decimal` from strings, not floats.
- Decide rounding mode explicitly.
- Store currency with amount.

Bad:

```python
Decimal(0.1)
```

Good:

```python
Decimal("0.10")
```

---

## 3. Time

### Core Rule

Use timezone-aware datetimes for real-world instants.

Bad:

```python
from datetime import datetime

created_at = datetime.now()
```

Good:

```python
from datetime import UTC, datetime

created_at = datetime.now(UTC)
```

### Naive vs Aware

| Type | Meaning |
|---|---|
| naive datetime | no timezone attached; ambiguous |
| aware datetime | timezone attached; represents an instant or local civil time clearly |

Interview answer:

> For backend services, I store instants in UTC using timezone-aware datetimes. I convert to user time zones only at the edge: display, reports, notifications, or user-specific scheduling.

---

## 4. UTC Storage, Local Display

```python
from datetime import UTC, datetime
from zoneinfo import ZoneInfo


stored = datetime.now(UTC)
user_zone = ZoneInfo("America/New_York")
display = stored.astimezone(user_zone)

print(stored.isoformat())
print(display.isoformat())
```

Pattern:

```text
API receives local time + timezone
    -> validate
    -> convert to UTC instant
    -> store UTC
    -> convert back for display
```

---

## 5. DST Trap

Adding "one day" to local time can be surprising around daylight saving transitions.

```python
from datetime import datetime, timedelta
from zoneinfo import ZoneInfo


ny = ZoneInfo("America/New_York")
meeting = datetime(2026, 3, 8, 1, 30, tzinfo=ny)
next_day = meeting + timedelta(days=1)
```

Questions to ask:

- Is this an elapsed duration of 24 hours?
- Or the same wall-clock local time tomorrow?

They are not always the same.

Production design:

- Use UTC instants for elapsed duration.
- Use local timezone + wall-clock rules for calendar scheduling.
- Store the user's timezone ID, not just offset.

---

## 6. `date`, `datetime`, and `time`

| Type | Use |
|---|---|
| `date` | birthdays, business date, partition date |
| aware `datetime` | exact instant in time |
| `time` | clock time without date, rarely enough alone |
| timestamp | interop format, but beware units and timezone |

Birthday example:

```python
from datetime import date

birth_date = date(1995, 7, 14)
```

Do not convert birthdays to UTC datetimes unless the domain truly needs an instant.

---

## 7. Parsing and Formatting

Use ISO-8601 at service boundaries:

```python
from datetime import datetime

value = datetime.fromisoformat("2026-07-02T10:15:30+00:00")
print(value.isoformat())
```

For strict API validation, prefer Pydantic models and explicit formats.

Avoid:

- ambiguous strings like `02/07/2026`
- locale-dependent date parsing
- silently accepting missing timezone for event timestamps

---

## 8. UUIDs and IDs

### UUID4

```python
from uuid import uuid4

order_id = uuid4()
```

Good for:

- public non-sequential IDs
- distributed ID generation
- avoiding database round trip for ID allocation

Trade-offs:

- random UUIDs are less index-friendly than monotonic IDs
- logs are harder to visually scan

### UUID7 / Time-Ordered IDs

When available in your runtime or library, time-ordered UUIDs improve index locality and event ordering. If not, common alternatives include ULID, KSUID, Snowflake-style IDs, or database-generated IDs.

Senior rule:

```text
Choose ID shape based on generation location, ordering requirement, privacy, index behavior,
and cross-service uniqueness.
```

---

## 9. Idempotency Keys

For payment/order APIs, request IDs matter as much as entity IDs.

```python
from dataclasses import dataclass
from uuid import UUID


@dataclass(frozen=True)
class IdempotencyKey:
    value: UUID
```

Usage:

```text
Client sends Idempotency-Key
Service stores key + request hash + response
Retry with same key returns same response
Different request body with same key is rejected
```

This prevents duplicate payments and duplicate order creation.

---

## 10. Locale

Locale affects human presentation, not canonical storage.

Bad API design:

```json
{
  "amount": "$1,234.50",
  "date": "07/02/26"
}
```

Better API design:

```json
{
  "amount_cents": 123450,
  "currency": "USD",
  "created_at": "2026-07-02T10:15:30Z"
}
```

Presentation layer can format based on user locale.

Backend APIs should send canonical values.

---

## 11. Pydantic Boundary Example

```python
from datetime import datetime
from decimal import Decimal
from uuid import UUID

from pydantic import BaseModel, Field, field_validator


class CreatePaymentRequest(BaseModel):
    idempotency_key: UUID
    amount: Decimal = Field(gt=Decimal("0"))
    currency: str = Field(min_length=3, max_length=3)
    requested_at: datetime

    @field_validator("currency")
    @classmethod
    def uppercase_currency(cls, value: str) -> str:
        return value.upper()

    @field_validator("requested_at")
    @classmethod
    def require_timezone(cls, value: datetime) -> datetime:
        if value.tzinfo is None or value.utcoffset() is None:
            raise ValueError("requested_at must include timezone")
        return value
```

---

## 12. Database Mapping

| Domain Value | Database Type |
|---|---|
| amount in cents | `BIGINT` |
| amount decimal | `NUMERIC(precision, scale)` |
| currency | `CHAR(3)` or constrained `VARCHAR(3)` |
| timestamp instant | `TIMESTAMP WITH TIME ZONE` where supported |
| UUID | native `UUID` where supported |
| timezone ID | string like `America/New_York` |

Do not rely only on app validation. Add database constraints for critical invariants:

- amount non-negative
- currency length/code
- unique idempotency key
- created timestamp not null

---

## 13. Common Mistakes

| Mistake | Failure Mode | Better Approach |
|---|---|---|
| Money as float | Rounding errors | integer minor units or `Decimal` |
| Naive datetime in DB | Ambiguous events | aware UTC datetime |
| Storing timezone offset only | DST rules lost | store IANA zone ID |
| Locale strings in APIs | Parsing breaks by country | canonical numeric/time fields |
| Random UUID as clustered key everywhere | Index fragmentation | consider UUID7/ULID/DB sequence |
| Missing idempotency key | Duplicate operations on retry | store idempotency key and response |

---

## 14. Practical Question

> You are designing a payment API in Python. How do you represent amount, currency, created time, and request idempotency?

Strong answer:

> I would not use float for money. I would represent amount as integer minor units or Decimal depending on the domain, always with currency. I would store created timestamps as timezone-aware UTC instants and convert to local time only for display. For duplicate protection, I would require an idempotency key, store the request hash and response against it, and reject mismatched retries. In the database I would add constraints for non-negative amount, currency code, not-null timestamps, and unique idempotency keys.

---

## 15. Revision Notes

- One-line summary: time, money, and IDs are domain correctness problems, not formatting details.
- Three keywords: Decimal, UTC, idempotency.
- One interview trap: using `float` for money or naive `datetime.now()`.
- One memory trick: store canonical values; format only at the edge.
