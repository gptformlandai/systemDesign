# Python LLD & Machine Coding Patterns — Gold Sheet

> **Track File #25 of 31 · Group 5: Special Interview Rounds**
> For: Java developer | Level: MAANG machine coding round | Mode: design + implement under pressure

---

## 1. Interview Priority Meter

| Topic | MAANG Frequency | Java Dev Trap Level |
|---|---|---|
| `Protocol` for structural typing | ★★★★★ | HIGH — Python interface is different from Java |
| `@dataclass` for value objects | ★★★★★ | MEDIUM — similar to Java record |
| Repository pattern | ★★★★★ | MEDIUM — same pattern, Pythonic syntax |
| Strategy pattern | ★★★★★ | MEDIUM — same pattern, cleaner in Python |
| Factory pattern | ★★★★☆ | MEDIUM |
| Observer / event bus | ★★★★☆ | MEDIUM |
| LRU Cache design | ★★★★★ | MEDIUM |
| Rate limiter design | ★★★★★ | MEDIUM |
| Parking lot / hotel booking | ★★★★☆ | LOW — same OOP concepts |
| Clean service layering | ★★★★★ | MEDIUM |

---

## 2. Python LLD Toolkit

### 2-A — Prefer `@dataclass` Over Plain Classes for Models

```python
from dataclasses import dataclass, field
from datetime import datetime
from enum import Enum, auto
from typing import Optional
import uuid

class VehicleType(Enum):
    MOTORCYCLE = auto()
    CAR = auto()
    TRUCK = auto()

@dataclass
class Vehicle:
    license_plate: str
    vehicle_type: VehicleType
    id: str = field(default_factory=lambda: str(uuid.uuid4()))

    def __hash__(self):
        return hash(self.license_plate)

    def __eq__(self, other):
        if not isinstance(other, Vehicle):
            return NotImplemented
        return self.license_plate == other.license_plate

@dataclass
class ParkingTicket:
    vehicle: Vehicle
    spot_id: str
    entry_time: datetime = field(default_factory=datetime.utcnow)
    exit_time: Optional[datetime] = None

    @property
    def is_active(self) -> bool:
        return self.exit_time is None

    @property
    def duration_minutes(self) -> float:
        end = self.exit_time or datetime.utcnow()
        return (end - self.entry_time).total_seconds() / 60
```

**Why `@dataclass` in LLD:**
- Auto-generates `__init__`, `__repr__`, `__eq__`
- `field(default_factory=...)` for mutable defaults
- `frozen=True` for immutable value objects
- Less boilerplate than Java POJOs

---

### 2-B — `Protocol` as the Pythonic Interface

```python
from typing import Protocol, runtime_checkable

@runtime_checkable
class ParkingSpotRepository(Protocol):
    """Structural interface — any class with these methods qualifies."""

    def find_available(self, vehicle_type: VehicleType) -> Optional["ParkingSpot"]:
        ...

    def save(self, spot: "ParkingSpot") -> None:
        ...

    def find_by_id(self, spot_id: str) -> Optional["ParkingSpot"]:
        ...
```

**Protocol vs ABC:**

| Feature | `Protocol` | `ABC` |
|---|---|---|
| Subclass required? | NO — structural (duck typing) | YES — explicit inheritance |
| `isinstance` check | Only with `@runtime_checkable` | Always works |
| Use case | Library boundaries, DI | Enforcing implementation contract |
| Java analogy | Java `interface` (implicit impl) | Java `abstract class` |

```python
# No explicit inheritance needed — structural subtyping
class InMemorySpotRepository:
    def __init__(self):
        self._spots: dict[str, ParkingSpot] = {}

    def find_available(self, vehicle_type: VehicleType) -> Optional[ParkingSpot]:
        return next(
            (s for s in self._spots.values()
             if s.is_available and s.spot_type == vehicle_type),
            None
        )

    def save(self, spot: ParkingSpot) -> None:
        self._spots[spot.id] = spot

    def find_by_id(self, spot_id: str) -> Optional[ParkingSpot]:
        return self._spots.get(spot_id)

# isinstance works because of @runtime_checkable
repo = InMemorySpotRepository()
print(isinstance(repo, ParkingSpotRepository))   # True — no inheritance needed
```

---

## 3. Full Machine Coding Problem — Parking Lot

### Problem Statement
Design a parking lot with multiple floors and spot types. Support: park a vehicle, unpark a vehicle, get available spots, calculate fee.

```python
from dataclasses import dataclass, field
from datetime import datetime
from enum import Enum, auto
from typing import Optional, Protocol
import uuid

# ── Enums ──────────────────────────────────────────────────────────────────
class VehicleType(Enum):
    MOTORCYCLE = auto()
    CAR = auto()
    TRUCK = auto()

class SpotType(Enum):
    SMALL = auto()    # motorcycles
    MEDIUM = auto()   # cars
    LARGE = auto()    # trucks

# ── Value Objects and Entities ─────────────────────────────────────────────
@dataclass
class Vehicle:
    license_plate: str
    vehicle_type: VehicleType

SPOT_MAP = {
    VehicleType.MOTORCYCLE: SpotType.SMALL,
    VehicleType.CAR: SpotType.MEDIUM,
    VehicleType.TRUCK: SpotType.LARGE,
}

@dataclass
class ParkingSpot:
    floor: int
    number: int
    spot_type: SpotType
    id: str = field(default_factory=lambda: str(uuid.uuid4()))
    parked_vehicle: Optional[Vehicle] = None

    @property
    def is_available(self) -> bool:
        return self.parked_vehicle is None

    def park(self, vehicle: Vehicle) -> None:
        if not self.is_available:
            raise ValueError(f"Spot {self.id} is occupied")
        self.parked_vehicle = vehicle

    def unpark(self) -> Vehicle:
        if self.is_available:
            raise ValueError(f"Spot {self.id} is already empty")
        vehicle = self.parked_vehicle
        self.parked_vehicle = None
        return vehicle

@dataclass
class ParkingTicket:
    vehicle: Vehicle
    spot: ParkingSpot
    entry_time: datetime = field(default_factory=datetime.utcnow)
    exit_time: Optional[datetime] = None

    @property
    def fee(self) -> float:
        end = self.exit_time or datetime.utcnow()
        minutes = (end - self.entry_time).total_seconds() / 60
        rate = {SpotType.SMALL: 1.0, SpotType.MEDIUM: 2.0, SpotType.LARGE: 3.0}
        return round((minutes / 60) * rate[self.spot.spot_type], 2)

# ── Strategy: Fee Calculator ───────────────────────────────────────────────
class FeeStrategy(Protocol):
    def calculate(self, ticket: ParkingTicket) -> float: ...

class HourlyFeeStrategy:
    RATES = {SpotType.SMALL: 1.0, SpotType.MEDIUM: 2.0, SpotType.LARGE: 3.0}

    def calculate(self, ticket: ParkingTicket) -> float:
        end = ticket.exit_time or datetime.utcnow()
        hours = (end - ticket.entry_time).total_seconds() / 3600
        return round(hours * self.RATES[ticket.spot.spot_type], 2)

class FlatFeeStrategy:
    RATE = 5.0

    def calculate(self, ticket: ParkingTicket) -> float:
        return self.RATE

# ── Service Layer ──────────────────────────────────────────────────────────
class ParkingLotService:
    def __init__(self, spots: list[ParkingSpot], fee_strategy: FeeStrategy):
        self._spots = spots
        self._tickets: dict[str, ParkingTicket] = {}
        self._fee_strategy = fee_strategy

    def park(self, vehicle: Vehicle) -> ParkingTicket:
        required = SPOT_MAP[vehicle.vehicle_type]
        spot = next(
            (s for s in self._spots if s.is_available and s.spot_type == required),
            None
        )
        if spot is None:
            raise ValueError(f"No available {required.name} spots")
        spot.park(vehicle)
        ticket = ParkingTicket(vehicle=vehicle, spot=spot)
        self._tickets[vehicle.license_plate] = ticket
        return ticket

    def unpark(self, license_plate: str) -> float:
        ticket = self._tickets.get(license_plate)
        if not ticket:
            raise KeyError(f"No active ticket for {license_plate}")
        ticket.exit_time = datetime.utcnow()
        ticket.spot.unpark()
        fee = self._fee_strategy.calculate(ticket)
        del self._tickets[license_plate]
        return fee

    def available_count(self, spot_type: SpotType) -> int:
        return sum(1 for s in self._spots if s.is_available and s.spot_type == spot_type)

# ── Usage ──────────────────────────────────────────────────────────────────
spots = [
    ParkingSpot(floor=1, number=i, spot_type=SpotType.MEDIUM) for i in range(1, 51)
] + [
    ParkingSpot(floor=1, number=i, spot_type=SpotType.SMALL) for i in range(51, 71)
]

service = ParkingLotService(spots=spots, fee_strategy=HourlyFeeStrategy())

car = Vehicle("ABC-123", VehicleType.CAR)
ticket = service.park(car)
print(f"Parked in spot {ticket.spot.number} on floor {ticket.spot.floor}")

fee = service.unpark("ABC-123")
print(f"Fee: ${fee}")
```

---

## 4. Design Pattern Implementations

### 4-A — Strategy Pattern

**When:** Multiple algorithms for the same operation; swap at runtime.

```python
from typing import Protocol

class SortStrategy(Protocol):
    def sort(self, data: list) -> list: ...

class QuickSort:
    def sort(self, data: list) -> list:
        if len(data) <= 1: return data
        pivot = data[len(data) // 2]
        left = [x for x in data if x < pivot]
        mid = [x for x in data if x == pivot]
        right = [x for x in data if x > pivot]
        return self.sort(left) + mid + self.sort(right)

class MergeSort:
    def sort(self, data: list) -> list:
        if len(data) <= 1: return data
        mid = len(data) // 2
        left = self.sort(data[:mid])
        right = self.sort(data[mid:])
        return self._merge(left, right)

    def _merge(self, left, right):
        result = []
        i = j = 0
        while i < len(left) and j < len(right):
            if left[i] <= right[j]: result.append(left[i]); i += 1
            else: result.append(right[j]); j += 1
        return result + left[i:] + right[j:]

class Sorter:
    def __init__(self, strategy: SortStrategy):
        self._strategy = strategy

    def set_strategy(self, strategy: SortStrategy):
        self._strategy = strategy

    def sort(self, data: list) -> list:
        return self._strategy.sort(data)

sorter = Sorter(QuickSort())
print(sorter.sort([3, 1, 4, 1, 5, 9, 2, 6]))
sorter.set_strategy(MergeSort())
print(sorter.sort([3, 1, 4, 1, 5]))
```

**Python advantage:** In Python, strategies can also be plain functions passed as `key=`, `sort_fn=` parameters — no class needed for simple cases.

---

### 4-B — Observer / Event Bus

**When:** Decoupled notification — publishers don't know subscribers.

```python
from collections import defaultdict
from typing import Callable, Any

class EventBus:
    """Simple synchronous event bus."""

    def __init__(self):
        self._subscribers: dict[str, list[Callable]] = defaultdict(list)

    def subscribe(self, event_type: str, handler: Callable[[Any], None]) -> None:
        self._subscribers[event_type].append(handler)

    def unsubscribe(self, event_type: str, handler: Callable) -> None:
        self._subscribers[event_type].remove(handler)

    def publish(self, event_type: str, payload: Any = None) -> None:
        for handler in self._subscribers[event_type]:
            handler(payload)

# Usage
bus = EventBus()

def on_user_created(user: dict):
    print(f"Sending welcome email to {user['email']}")

def on_user_created_audit(user: dict):
    print(f"Audit log: user {user['id']} created")

bus.subscribe("user.created", on_user_created)
bus.subscribe("user.created", on_user_created_audit)

bus.publish("user.created", {"id": 1, "email": "alice@example.com"})
# Sending welcome email to alice@example.com
# Audit log: user 1 created
```

---

### 4-C — Factory Pattern

**When:** Object creation logic should be centralized and extensible.

```python
from typing import Protocol
from dataclasses import dataclass

class Notifier(Protocol):
    def send(self, to: str, message: str) -> None: ...

class EmailNotifier:
    def send(self, to: str, message: str) -> None:
        print(f"Email → {to}: {message}")

class SMSNotifier:
    def send(self, to: str, message: str) -> None:
        print(f"SMS → {to}: {message}")

class SlackNotifier:
    def send(self, to: str, message: str) -> None:
        print(f"Slack → {to}: {message}")

class NotifierFactory:
    _registry: dict[str, type] = {
        "email": EmailNotifier,
        "sms": SMSNotifier,
        "slack": SlackNotifier,
    }

    @classmethod
    def register(cls, name: str, notifier_class: type) -> None:
        cls._registry[name] = notifier_class

    @classmethod
    def create(cls, channel: str) -> Notifier:
        if channel not in cls._registry:
            raise ValueError(f"Unknown notifier channel: {channel}")
        return cls._registry[channel]()

# Adding a new notifier without modifying the factory
class PushNotifier:
    def send(self, to: str, message: str) -> None:
        print(f"Push → {to}: {message}")

NotifierFactory.register("push", PushNotifier)

notifier = NotifierFactory.create("push")
notifier.send("user_123", "Your order shipped!")
```

---

### 4-D — Repository Pattern

**When:** Decouple domain logic from data access; swap storage backends.

```python
from typing import Protocol, Optional
from dataclasses import dataclass, field
import uuid

@dataclass
class Order:
    user_id: str
    items: list[str]
    total: float
    id: str = field(default_factory=lambda: str(uuid.uuid4()))
    status: str = "pending"

class OrderRepository(Protocol):
    def save(self, order: Order) -> None: ...
    def find_by_id(self, order_id: str) -> Optional[Order]: ...
    def find_by_user(self, user_id: str) -> list[Order]: ...
    def delete(self, order_id: str) -> None: ...

class InMemoryOrderRepository:
    def __init__(self):
        self._store: dict[str, Order] = {}

    def save(self, order: Order) -> None:
        self._store[order.id] = order

    def find_by_id(self, order_id: str) -> Optional[Order]:
        return self._store.get(order_id)

    def find_by_user(self, user_id: str) -> list[Order]:
        return [o for o in self._store.values() if o.user_id == user_id]

    def delete(self, order_id: str) -> None:
        self._store.pop(order_id, None)

class OrderService:
    def __init__(self, repo: OrderRepository):  # depends on Protocol, not impl
        self._repo = repo

    def place_order(self, user_id: str, items: list[str], total: float) -> Order:
        order = Order(user_id=user_id, items=items, total=total)
        self._repo.save(order)
        return order

    def cancel_order(self, order_id: str) -> None:
        order = self._repo.find_by_id(order_id)
        if not order:
            raise KeyError(f"Order {order_id} not found")
        if order.status != "pending":
            raise ValueError(f"Cannot cancel order in status: {order.status}")
        order.status = "cancelled"
        self._repo.save(order)

# Swapping repository: no changes to OrderService
repo = InMemoryOrderRepository()
service = OrderService(repo)
o = service.place_order("user-1", ["item-A", "item-B"], 49.99)
service.cancel_order(o.id)
print(repo.find_by_id(o.id).status)   # "cancelled"
```

---

## 5. Full Machine Coding Problem — Rate Limiter

### Problem Statement
Design a rate limiter supporting multiple algorithms: fixed window, sliding window, token bucket. Support per-user limits.

```python
from typing import Protocol
from collections import deque
import time

# ── Strategy interface ─────────────────────────────────────────────────────
class RateLimitStrategy(Protocol):
    def is_allowed(self, key: str) -> bool: ...

# ── Fixed Window ──────────────────────────────────────────────────────────
class FixedWindowRateLimiter:
    """Allows max_requests per window_seconds, window resets at fixed intervals."""

    def __init__(self, max_requests: int, window_seconds: float):
        self.max_requests = max_requests
        self.window_seconds = window_seconds
        self._windows: dict[str, tuple[float, int]] = {}  # key → (window_start, count)

    def is_allowed(self, key: str) -> bool:
        now = time.monotonic()
        window_start, count = self._windows.get(key, (now, 0))

        if now - window_start >= self.window_seconds:
            # New window
            self._windows[key] = (now, 1)
            return True

        if count >= self.max_requests:
            return False

        self._windows[key] = (window_start, count + 1)
        return True

# ── Sliding Window Log ────────────────────────────────────────────────────
class SlidingWindowRateLimiter:
    """Accurate sliding window using a timestamp deque per key."""

    def __init__(self, max_requests: int, window_seconds: float):
        self.max_requests = max_requests
        self.window_seconds = window_seconds
        self._logs: dict[str, deque] = {}

    def is_allowed(self, key: str) -> bool:
        now = time.monotonic()
        if key not in self._logs:
            self._logs[key] = deque()
        log = self._logs[key]

        # Evict timestamps outside the window
        cutoff = now - self.window_seconds
        while log and log[0] <= cutoff:
            log.popleft()

        if len(log) >= self.max_requests:
            return False

        log.append(now)
        return True

# ── Token Bucket ──────────────────────────────────────────────────────────
class TokenBucketRateLimiter:
    """Allows bursts up to bucket_size; refills at rate tokens/second."""

    def __init__(self, rate: float, bucket_size: float):
        self.rate = rate
        self.bucket_size = bucket_size
        self._buckets: dict[str, tuple[float, float]] = {}  # key → (tokens, last_refill)

    def is_allowed(self, key: str) -> bool:
        now = time.monotonic()
        tokens, last_refill = self._buckets.get(key, (self.bucket_size, now))

        # Refill tokens based on elapsed time
        elapsed = now - last_refill
        tokens = min(self.bucket_size, tokens + elapsed * self.rate)

        if tokens < 1.0:
            self._buckets[key] = (tokens, now)
            return False

        self._buckets[key] = (tokens - 1.0, now)
        return True

# ── Rate Limiter Service ──────────────────────────────────────────────────
class RateLimiterService:
    """Applies rate limiting using a configured strategy."""

    def __init__(self, strategy: RateLimitStrategy):
        self._strategy = strategy

    def check(self, user_id: str) -> bool:
        return self._strategy.is_allowed(user_id)

# Usage
limiter = RateLimiterService(SlidingWindowRateLimiter(max_requests=5, window_seconds=10))

for i in range(7):
    allowed = limiter.check("user-alice")
    print(f"Request {i+1}: {'ALLOWED' if allowed else 'BLOCKED'}")
# Requests 1-5: ALLOWED
# Requests 6-7: BLOCKED
```

---

## 6. Full Machine Coding Problem — LRU Cache

### Problem Statement
Design an LRU (Least Recently Used) cache with O(1) get and put.

```python
from collections import OrderedDict
from typing import TypeVar, Generic, Optional

K = TypeVar("K")
V = TypeVar("V")

class LRUCache(Generic[K, V]):
    """
    O(1) get and put using OrderedDict.
    OrderedDict maintains insertion order; move_to_end + popitem(last=False) = LRU.
    """

    def __init__(self, capacity: int):
        if capacity <= 0:
            raise ValueError("Capacity must be positive")
        self._capacity = capacity
        self._cache: OrderedDict[K, V] = OrderedDict()

    def get(self, key: K) -> Optional[V]:
        if key not in self._cache:
            return None
        self._cache.move_to_end(key)   # mark as most recently used
        return self._cache[key]

    def put(self, key: K, value: V) -> None:
        if key in self._cache:
            self._cache.move_to_end(key)
        self._cache[key] = value
        if len(self._cache) > self._capacity:
            self._cache.popitem(last=False)   # evict LRU (first item)

    def __len__(self) -> int:
        return len(self._cache)

    def __repr__(self) -> str:
        return f"LRUCache({dict(self._cache)})"

# Usage
cache: LRUCache[str, int] = LRUCache(capacity=3)
cache.put("a", 1)
cache.put("b", 2)
cache.put("c", 3)

cache.get("a")          # "a" becomes MRU

cache.put("d", 4)       # evicts "b" (LRU)
print(cache.get("b"))   # None — evicted
print(cache)            # LRUCache({'c': 3, 'a': 1, 'd': 4})
```

**Without `OrderedDict` — doubly linked list + dict (shows data structure knowledge):**

```python
class DLLNode:
    def __init__(self, key=None, value=None):
        self.key = key
        self.value = value
        self.prev: DLLNode = None
        self.next: DLLNode = None

class LRUCacheManual(Generic[K, V]):
    def __init__(self, capacity: int):
        self._capacity = capacity
        self._cache: dict[K, DLLNode] = {}
        self._head = DLLNode()   # dummy head (oldest)
        self._tail = DLLNode()   # dummy tail (newest)
        self._head.next = self._tail
        self._tail.prev = self._head

    def _remove(self, node: DLLNode) -> None:
        node.prev.next = node.next
        node.next.prev = node.prev

    def _add_to_tail(self, node: DLLNode) -> None:
        node.prev = self._tail.prev
        node.next = self._tail
        self._tail.prev.next = node
        self._tail.prev = node

    def get(self, key: K) -> Optional[V]:
        if key not in self._cache:
            return None
        node = self._cache[key]
        self._remove(node)
        self._add_to_tail(node)
        return node.value

    def put(self, key: K, value: V) -> None:
        if key in self._cache:
            self._remove(self._cache[key])
        node = DLLNode(key, value)
        self._add_to_tail(node)
        self._cache[key] = node
        if len(self._cache) > self._capacity:
            lru = self._head.next
            self._remove(lru)
            del self._cache[lru.key]
```

---

## 7. Clean Service Architecture Pattern

### Structure

```
domain/
    models.py      # @dataclass entities and value objects
    protocols.py   # Protocol interfaces (repositories, services)
service/
    order_service.py   # pure business logic, depends only on protocols
repository/
    in_memory.py       # InMemoryOrderRepository
    postgres.py        # PostgresOrderRepository (swap without touching service)
api/
    routes.py          # FastAPI routes, thin — delegates to service
```

### 7-A — Domain Layer

```python
# domain/models.py
from dataclasses import dataclass, field
from enum import Enum, auto
from typing import Optional
import uuid

class OrderStatus(Enum):
    PENDING = auto()
    CONFIRMED = auto()
    SHIPPED = auto()
    CANCELLED = auto()

@dataclass
class OrderItem:
    product_id: str
    quantity: int
    unit_price: float

    @property
    def subtotal(self) -> float:
        return self.quantity * self.unit_price

@dataclass
class Order:
    user_id: str
    items: list[OrderItem]
    id: str = field(default_factory=lambda: str(uuid.uuid4()))
    status: OrderStatus = OrderStatus.PENDING

    @property
    def total(self) -> float:
        return sum(item.subtotal for item in self.items)

    def confirm(self) -> None:
        if self.status != OrderStatus.PENDING:
            raise ValueError(f"Cannot confirm order in state {self.status}")
        self.status = OrderStatus.CONFIRMED

    def cancel(self) -> None:
        if self.status in (OrderStatus.SHIPPED, OrderStatus.CANCELLED):
            raise ValueError(f"Cannot cancel order in state {self.status}")
        self.status = OrderStatus.CANCELLED
```

### 7-B — Protocol Layer (Interfaces)

```python
# domain/protocols.py
from typing import Protocol, Optional
from .models import Order

class OrderRepo(Protocol):
    def save(self, order: Order) -> None: ...
    def find_by_id(self, order_id: str) -> Optional[Order]: ...
    def find_by_user(self, user_id: str) -> list[Order]: ...

class PaymentGateway(Protocol):
    def charge(self, user_id: str, amount: float) -> str: ...  # returns transaction_id
```

### 7-C — Service Layer

```python
# service/order_service.py
from domain.models import Order, OrderItem, OrderStatus
from domain.protocols import OrderRepo, PaymentGateway

class OrderService:
    def __init__(self, repo: OrderRepo, payment: PaymentGateway):
        self._repo = repo
        self._payment = payment

    def place_order(self, user_id: str, items: list[dict]) -> Order:
        order_items = [
            OrderItem(
                product_id=i["product_id"],
                quantity=i["quantity"],
                unit_price=i["unit_price"]
            )
            for i in items
        ]
        order = Order(user_id=user_id, items=order_items)
        transaction_id = self._payment.charge(user_id, order.total)
        order.confirm()
        self._repo.save(order)
        return order

    def cancel_order(self, order_id: str) -> None:
        order = self._repo.find_by_id(order_id)
        if not order:
            raise KeyError(f"Order {order_id} not found")
        order.cancel()
        self._repo.save(order)
```

### 7-D — FastAPI Thin Route Layer

```python
# api/routes.py
from fastapi import FastAPI, HTTPException, Depends
from pydantic import BaseModel

app = FastAPI()

class PlaceOrderRequest(BaseModel):
    user_id: str
    items: list[dict]

def get_order_service() -> OrderService:
    # Dependency injection — swap repo for tests
    return OrderService(
        repo=InMemoryOrderRepository(),
        payment=StripePaymentGateway()
    )

@app.post("/orders")
async def place_order(
    request: PlaceOrderRequest,
    service: OrderService = Depends(get_order_service)
):
    try:
        order = service.place_order(request.user_id, request.items)
        return {"order_id": order.id, "total": order.total}
    except ValueError as e:
        raise HTTPException(status_code=400, detail=str(e))

@app.delete("/orders/{order_id}")
async def cancel_order(
    order_id: str,
    service: OrderService = Depends(get_order_service)
):
    try:
        service.cancel_order(order_id)
        return {"status": "cancelled"}
    except KeyError as e:
        raise HTTPException(status_code=404, detail=str(e))
    except ValueError as e:
        raise HTTPException(status_code=422, detail=str(e))
```

---

## 8. Testing Machine Coding Solutions

```python
# test_order_service.py
import pytest
from unittest.mock import MagicMock
from service.order_service import OrderService
from domain.models import Order, OrderStatus

@pytest.fixture
def mock_repo():
    return MagicMock()

@pytest.fixture
def mock_payment():
    p = MagicMock()
    p.charge.return_value = "txn-123"
    return p

@pytest.fixture
def service(mock_repo, mock_payment):
    return OrderService(repo=mock_repo, payment=mock_payment)

def test_place_order_charges_and_confirms(service, mock_repo, mock_payment):
    items = [{"product_id": "A", "quantity": 2, "unit_price": 10.0}]
    order = service.place_order("user-1", items)

    mock_payment.charge.assert_called_once_with("user-1", 20.0)
    mock_repo.save.assert_called()
    assert order.status == OrderStatus.CONFIRMED
    assert order.total == 20.0

def test_cancel_nonexistent_order_raises(service, mock_repo):
    mock_repo.find_by_id.return_value = None
    with pytest.raises(KeyError):
        service.cancel_order("nonexistent-id")
```

---

## 9. Machine Coding Checklist

Before writing code in an interview:

```
1. UNDERSTAND (2 min)
   - Clarify functional requirements
   - Ask: concurrency? persistence? scale constraints?

2. DESIGN (5 min)
   - Identify entities (models/dataclasses)
   - Identify behaviors (services/methods)
   - Identify interfaces (Protocol)
   - Identify storage (Repository)

3. CODE (20-25 min)
   - Start with models (dataclasses, enums)
   - Define Protocol interfaces
   - Implement service layer (business logic)
   - Implement in-memory repository
   - Wire together + demo

4. VALIDATE (5 min)
   - Walk through a usage example
   - Discuss extensibility (strategy swap, repo swap)
   - Mention test strategy
```

---

## 10. Java Developer Bridge — LLD Pattern Mapping

| Java Pattern | Python Equivalent | Key Difference |
|---|---|---|
| `interface Repo` | `class Repo(Protocol)` | Python: structural; Java: explicit `implements` |
| `abstract class Shape` | `class Shape(ABC)` | Both require explicit inheritance |
| POJO with getters/setters | `@dataclass` | Python: no boilerplate; auto-generates init/repr/eq |
| Java `record` (immutable) | `@dataclass(frozen=True)` | Very similar; Python more flexible |
| `enum VehicleType` | `class VehicleType(Enum)` | Near-identical |
| Strategy pattern | Same — but can use function as strategy | Python: callable Protocol covers functions too |
| Factory + registry | Same — `_registry: dict[str, type]` | Python: dict-of-types is idiomatic |
| Observer | Same — `list[Callable]` per event | Python: functions as handlers, no interface needed |
| Repository pattern | Same structure | Python: `Protocol` instead of `interface` |
| DI (Spring `@Autowired`) | Constructor injection via `Depends` | FastAPI's `Depends` = Spring `@Autowired` |
| Singleton | Metaclass or module-level instance | Python: module is a natural singleton |
| `instanceof` check | `isinstance(obj, MyClass)` | Same; `Protocol` needs `@runtime_checkable` |
| `Optional<T>` | `Optional[T]` from `typing` | Python 3.10+: `T | None` syntax |
| Generic class `Cache<K,V>` | `class Cache(Generic[K, V])` | Similar; Python `TypeVar` is more flexible |

---

## 11. Hot Interview Q&A

**Q1: How does `Protocol` differ from `ABC` in Python?**
> `ABC` requires explicit inheritance (`class MyImpl(MyABC)`). `Protocol` uses structural subtyping — any class that has the required methods satisfies the Protocol without inheriting from it. Use `Protocol` when you want duck typing with type hints (library boundaries, DI). Use `ABC` when you want to enforce a contract through the class hierarchy (template method pattern, shared base logic).

**Q2: Why use `@dataclass` instead of a plain class for domain models?**
> `@dataclass` auto-generates `__init__`, `__repr__`, and `__eq__` from field annotations. This eliminates boilerplate while keeping code readable. `field(default_factory=list)` prevents mutable default bugs. `frozen=True` creates immutable value objects (hashable, usable as dict keys). It makes the intent explicit and keeps models concise.

**Q3: What is the Repository pattern and why is it useful in Python?**
> The Repository pattern abstracts data access behind a Protocol interface. The service layer depends only on the Protocol, not the concrete implementation. This means you can run tests against an `InMemoryRepository` (fast, no DB) and deploy with `PostgresRepository`. Swapping storage backends requires zero changes to business logic.

**Q4: How would you make a rate limiter thread-safe for a multi-threaded FastAPI server?**
> Replace the plain dict with a `threading.Lock` per key, or use `asyncio.Lock` for async contexts. For the sliding window, use `asyncio.Lock` to protect the deque per key. For production, use Redis with atomic Lua scripts (EVALSHA) to make rate limiting distributed and atomic across multiple server instances.

**Q5: What is the difference between LRU implemented with `OrderedDict` vs doubly linked list + dict?**
> Both are O(1) for get and put. `OrderedDict` is simpler to implement and leverages CPython's built-in C implementation. The doubly linked list + dict approach shows deeper data structure knowledge and is what you'd use if you needed to implement from scratch (common in FAANG DSA rounds). For a real system, `OrderedDict` is preferred.

**Q6: How do you structure a machine coding solution for maximum score in an interview?**
> Start with domain models (`@dataclass` entities, `Enum` types). Define `Protocol` interfaces before implementing them. Write the service layer with pure logic, depending only on protocols. Implement in-memory repositories. Wire together in a short demo. Mention that swapping repository or strategy is a one-line change. Mention testability via mock repositories.

---

## 12. Final Revision Checklist

- [ ] Can implement `@dataclass` entity with `field(default_factory=...)` and computed `@property`
- [ ] Can define a `Protocol` interface and explain structural vs nominal subtyping
- [ ] Can implement Repository pattern with Protocol + InMemory implementation
- [ ] Can implement Strategy pattern with Protocol — including function-as-strategy
- [ ] Can implement Observer/EventBus with `dict[str, list[Callable]]`
- [ ] Can implement LRU Cache with `OrderedDict.move_to_end` + `popitem(last=False)`
- [ ] Can implement LRU Cache with doubly linked list + dict (interview DSA variant)
- [ ] Can implement FixedWindow, SlidingWindow, and TokenBucket rate limiters
- [ ] Can describe the 3-layer architecture: domain → service → API
- [ ] Can implement `dependency_overrides` for testable FastAPI routes
- [ ] Can write unit tests for service layer using `MagicMock` repository
- [ ] Can explain machine coding structure: models → protocols → service → wire → demo
- [ ] Can explain why `@dataclass(frozen=True)` is hashable and when to use it
- [ ] Can swap strategy or repository without changing service layer
