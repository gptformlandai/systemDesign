# Python Pattern Matching `match`/`case` - Gold Sheet

> **Track File #12b - Group 2: Intermediate Backend**
> For: Java developer | Level: modern Python control flow and data-shape matching

---

## 1. Intuition

Python `match` is not just a prettier `switch`.

It is closer to this:

```text
"Look at the shape of this value.
If it looks like this shape, bind useful parts to names and run that branch."
```

It can match:

- literal values
- tuples/lists
- dictionaries
- classes/dataclasses
- OR patterns
- guards with `if`

---

## 2. Definition

- Definition: Structural pattern matching is Python syntax for branching based on the structure and contents of an object.
- Category: Language-level control flow.
- Core idea: Match value shape, optionally bind names, then execute the first matching case.

---

## 3. Java Developer Bridge

```text
Similar to Java:
  `match` can replace some enum/string/int switch-style branching.

Different in Python:
  Python pattern matching can destructure objects, sequences, and mappings.
  A bare name in a pattern captures a value. It does not compare against a variable.

Does not exist in Java in the same way:
  Tuple/list/dict destructuring directly inside the switch pattern.

Pythonic replacement:
  Use match/case for data-shape dispatch, parsers, command handling, AST-like values,
  and API event payloads. Use if/elif for simple boolean conditions.

Interview trap for Java developers:
  Thinking `case status:` compares to an existing variable named status.
  In Python it captures the value into a new name unless it is a dotted constant
  like `HttpStatus.OK` or a literal like `"OK"`.
```

---

## 4. Basic Syntax

```python
def status_message(code: int) -> str:
    match code:
        case 200:
            return "ok"
        case 400 | 422:
            return "bad request"
        case 401 | 403:
            return "auth failed"
        case _:
            return "unknown"
```

Rules:

- `match` evaluates the subject once.
- Cases are tested top to bottom.
- First successful case wins.
- `_` is the wildcard.

---

## 5. Literal Patterns

```python
match event_type:
    case "ORDER_CREATED":
        handle_created()
    case "ORDER_CANCELLED":
        handle_cancelled()
    case _:
        handle_unknown()
```

Good for:

- event types
- command names
- small protocol states

Use `Enum` when the set of values is part of your domain model.

---

## 6. Capture Pattern Trap

This is wrong if you meant to compare to an existing variable:

```python
expected = "PAID"

match status:
    case expected:
        print("matches everything")
```

`case expected` captures any value into the name `expected`.

Better:

```python
match status:
    case "PAID":
        print("paid")
```

For constants, use a dotted name:

```python
from enum import Enum


class PaymentStatus(Enum):
    PAID = "PAID"
    FAILED = "FAILED"


match status:
    case PaymentStatus.PAID:
        print("paid")
```

---

## 7. Sequence Patterns

```python
def parse_command(parts: list[str]) -> str:
    match parts:
        case ["create", entity, name]:
            return f"create {entity}: {name}"
        case ["delete", entity, identifier]:
            return f"delete {entity}: {identifier}"
        case ["list", entity]:
            return f"list {entity}"
        case _:
            return "invalid command"
```

This matches by length and position.

Star capture:

```python
match path.split("/"):
    case ["", "users", user_id, *rest]:
        print(user_id, rest)
```

---

## 8. Mapping Patterns

Useful for JSON-like payloads:

```python
def route_event(payload: dict) -> str:
    match payload:
        case {"type": "user.created", "user_id": user_id}:
            return f"create user {user_id}"
        case {"type": "invoice.paid", "invoice_id": invoice_id, "amount": amount}:
            return f"invoice {invoice_id} paid {amount}"
        case {"type": event_type}:
            return f"unsupported event {event_type}"
        case _:
            return "invalid payload"
```

Important:

- A mapping pattern requires listed keys to exist.
- Extra keys are allowed unless you explicitly capture/validate them.
- Pattern matching is not a replacement for API validation.

Use Pydantic at boundaries when payloads are untrusted.

---

## 9. Class and Dataclass Patterns

```python
from dataclasses import dataclass


@dataclass(frozen=True)
class PaymentSucceeded:
    payment_id: str
    amount_cents: int


@dataclass(frozen=True)
class PaymentFailed:
    payment_id: str
    reason: str


def describe(event: PaymentSucceeded | PaymentFailed) -> str:
    match event:
        case PaymentSucceeded(payment_id=payment_id, amount_cents=amount):
            return f"{payment_id} succeeded for {amount}"
        case PaymentFailed(payment_id=payment_id, reason=reason):
            return f"{payment_id} failed: {reason}"
```

Dataclasses generate `__match_args__` by default, allowing positional matching.

Prefer keyword matching for readability:

```python
case PaymentSucceeded(payment_id=payment_id, amount_cents=amount):
```

Avoid cryptic positional patterns:

```python
case PaymentSucceeded(pid, amount):
```

---

## 10. Guards

Use guards when shape is not enough:

```python
def classify_payment(event: dict) -> str:
    match event:
        case {"type": "payment", "amount": amount} if amount > 10_000:
            return "large payment"
        case {"type": "payment", "amount": amount} if amount > 0:
            return "normal payment"
        case {"type": "payment"}:
            return "invalid amount"
        case _:
            return "not payment"
```

Guard rules:

- Pattern must match first.
- Guard expression then runs.
- If guard is false, Python tries the next case.

---

## 11. OR Patterns

```python
match method:
    case "POST" | "PUT" | "PATCH":
        return "writes"
    case "GET" | "HEAD":
        return "reads"
    case _:
        return "other"
```

All alternatives in an OR pattern must bind the same names.

Valid:

```python
case {"id": item_id} | {"item_id": item_id}:
    return item_id
```

Invalid:

```python
case {"id": item_id} | {"name": name}:
    ...
```

---

## 12. Where It Fits In Backend Code

Strong uses:

- command parsing
- webhook event dispatch
- state machine transitions
- AST/token processing
- internal domain events
- DTO-to-command mapping after validation

Weak uses:

- large business rule trees
- authorization decisions
- deeply nested untrusted JSON without validation
- logic that needs polymorphism

If the branch behavior belongs to the object, prefer polymorphism.

---

## 13. Match vs if/elif

| Use `match` When | Use `if/elif` When |
|---|---|
| Branching by shape | Branching by arbitrary boolean logic |
| Destructuring is useful | Conditions are range checks or predicates |
| Cases are stable and readable | Conditions depend on external services |
| Event/command/state dispatch | Two or three simple branches |

Example where `if` is clearer:

```python
if user.is_admin and feature.enabled:
    ...
elif user.is_suspended:
    ...
```

---

## 14. Mini Program

```python
from dataclasses import dataclass


@dataclass(frozen=True)
class CreateOrder:
    customer_id: str
    sku: str
    quantity: int


@dataclass(frozen=True)
class CancelOrder:
    order_id: str


Command = CreateOrder | CancelOrder


def parse(payload: dict) -> Command:
    match payload:
        case {
            "type": "create_order",
            "customer_id": str(customer_id),
            "sku": str(sku),
            "quantity": int(quantity),
        } if quantity > 0:
            return CreateOrder(customer_id, sku, quantity)
        case {"type": "cancel_order", "order_id": str(order_id)}:
            return CancelOrder(order_id)
        case _:
            raise ValueError("invalid command")


def handle(command: Command) -> str:
    match command:
        case CreateOrder(customer_id=customer_id, sku=sku, quantity=quantity):
            return f"create {quantity} of {sku} for {customer_id}"
        case CancelOrder(order_id=order_id):
            return f"cancel {order_id}"
```

Note:

- This is useful for internal command dispatch.
- For public API payloads, Pydantic validation is still the safer first layer.

---

## 15. Common Mistakes

| Mistake | Why It Is Wrong | Better Approach |
|---|---|---|
| Using `case variable:` to compare | It captures, so it matches almost anything | Use literals, enums, or dotted constants |
| Treating match as validation | Patterns are not full schema validation | Use Pydantic for external input |
| Huge nested match blocks | Hard to test and extend | Extract functions or use polymorphism |
| Forgetting case order | First match wins | Put specific cases before general ones |
| Positional dataclass matching everywhere | Fragile if field order changes | Prefer keyword class patterns |

---

## 16. Practical Question

> You receive webhook events from a payment provider. How would you use Python pattern matching, and where would you avoid it?

Strong answer:

> I would first validate the raw webhook with Pydantic because external JSON is untrusted. After validation, I can use `match` on a typed event or normalized dict to dispatch by event shape, such as payment succeeded, failed, or refunded. I would keep cases small and delegate to service methods. I would avoid large business rule trees inside `match`; if behavior grows per event type, I would move toward command handlers or polymorphism.

---

## 17. Revision Notes

- One-line summary: `match` branches on structure, not only value.
- Three keywords: shape, capture, guard.
- One interview trap: bare names capture; they do not compare.
- One memory trick: "literal compares, bare name captures, underscore ignores."
