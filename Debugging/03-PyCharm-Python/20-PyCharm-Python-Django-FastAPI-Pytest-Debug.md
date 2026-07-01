# 20. PyCharm: Django, FastAPI, and Pytest Debug

## Goal

Debug Django views, middleware, and signals, FastAPI routes with Pydantic, and pytest with parametrize — all in PyCharm with practical breakpoint placement strategies.

---

## Django Debug Configuration

```text
Run -> Edit Configurations -> + -> Django server

Name:          Django Dev
Working dir:   /path/to/project
  Additional options: --noreload --nothreading
  Environment:  DJANGO_SETTINGS_MODULE=myapp.settings.dev;PYTHONPATH=/path/to/project/src
  Python interpreter: Project Default
```

Why `--noreload --nothreading`:
- `--noreload`: Django's default watcher spawns a subprocess. That child process is not controlled by the debugger. Disable it.
- `--nothreading`: single-threaded mode ensures the debugger doesn't lose control to concurrent request threads.

---

## Django View Breakpoints

```python
from django.http import JsonResponse
from .models import Order

def order_detail(request, order_id):
    # breakpoint here: inspect request.user, request.method, order_id
    if not request.user.is_authenticated:
        return JsonResponse({'error': 'Unauthorized'}, status=401)
    
    # breakpoint here: auth passed, about to query DB
    order = Order.objects.get(pk=order_id)
    # breakpoint here: inspect order fields from DB
    
    if order.user != request.user:
        return JsonResponse({'error': 'Forbidden'}, status=403)
    
    return JsonResponse({
        'id': str(order.id),
        'status': order.status,
        'total': float(order.total),
    })
```

In PyCharm Variables panel when paused:
```text
request.user        -> AnonymousUser or auth.User object
request.META        -> expand to see all HTTP headers
order               -> expand to see all model fields
order._state        -> django internal state (adding, db)
```

---

## Django ORM Query Debug

```python
def order_list(request):
    # breakpoint before query: inspect filter kwargs
    orders = Order.objects.filter(
        user=request.user,
        status__in=['PENDING', 'PROCESSING']
    ).select_related('user').order_by('-created_at')
    
    # breakpoint after assignment but BEFORE iteration:
    # orders is a QuerySet (lazy) — not yet executed
    
    orders_list = list(orders)  # breakpoint after: DB query has executed here
    
    return JsonResponse({'orders': [o.to_dict() for o in orders_list]})
```

In Evaluate Expression:
```python
# Force QuerySet evaluation to see SQL.
str(orders.query)   # prints the raw SQL string
orders.explain()    # prints EXPLAIN output (Django 2.1+)
```

---

## Django Middleware Debug

```python
class RequestTimingMiddleware:
    def __init__(self, get_response):
        self.get_response = get_response

    def __call__(self, request):
        # breakpoint here: runs BEFORE the view
        # inspect request.path, request.method, request.META
        import time
        start = time.time()
        
        response = self.get_response(request)
        # breakpoint here: runs AFTER the view
        # inspect response.status_code, response.content
        
        duration = time.time() - start
        response['X-Processing-Time'] = str(duration)
        return response
```

---

## Django Signals Debug

```python
from django.db.models.signals import post_save
from django.dispatch import receiver
from .models import Order

@receiver(post_save, sender=Order)
def order_saved_handler(sender, instance, created, **kwargs):
    # breakpoint: fires after every Order.save()
    # inspect: instance (the saved Order), created (bool), kwargs
    if created:
        send_confirmation_email.delay(instance.id)  # Celery task
```

---

## FastAPI Debug Configuration

```text
Run -> Edit Configurations -> + -> Python

Name:       FastAPI Dev
Module:     uvicorn
Parameters: main:app --host 0.0.0.0 --port 8000
            (NO --reload)
Working dir: /path/to/project
Environment: PYTHONPATH=/path/to/project/src
```

---

## FastAPI Route Debug

```python
from fastapi import FastAPI, Depends, HTTPException
from pydantic import BaseModel

app = FastAPI()

class OrderRequest(BaseModel):
    user_id: str
    items: list[dict]
    total: float

@app.post("/orders")
async def create_order(
    order_req: OrderRequest,
    db: Session = Depends(get_db)
):
    # breakpoint: inspect order_req fields (Pydantic model)
    # order_req.user_id, order_req.items, order_req.total are all validated
    
    if order_req.total <= 0:
        raise HTTPException(status_code=422, detail="Total must be positive")
    
    # breakpoint: about to write to DB
    order = await order_service.create(db, order_req)
    # breakpoint: inspect created order
    return order
```

In Evaluate Expression:
```python
order_req.dict()           # Pydantic model as plain dict
order_req.json()           # Pydantic model as JSON string
order_req.schema()         # Pydantic field schema
```

---

## Pytest Debug Configuration

```text
Run -> Edit Configurations -> + -> Python tests -> pytest

Name:        All Tests
Working dir: /path/to/project
Additional arguments: -v -s
Python interpreter: Project Default
Environment: PYTHONPATH=/path/to/project/src
```

The `-s` flag is equivalent to `--capture=no` — it lets print() and breakpoint() work during test execution.

---

## Debugging A Parametrized Test

```python
import pytest

@pytest.mark.parametrize("order_id,expected_status", [
    ("ORD-001", "PENDING"),
    ("ORD-002", "PROCESSING"),
    ("ORD-003", "FAILED"),
])
def test_order_status(order_id, expected_status, order_service):
    # breakpoint here: inspect order_id and expected_status for each iteration
    order = order_service.get(order_id)
    # Conditional breakpoint: order_id == "ORD-003" to debug only failing case
    assert order.status == expected_status
```

To debug only one parametrize case:
- Use a conditional breakpoint: `order_id == "ORD-003"`
- Or run with pytest `-k "ORD-003"` in Additional arguments

---

## Debugging Pytest Fixtures

```python
@pytest.fixture
def order_service(db_session):
    # breakpoint here: inspect db_session
    service = OrderService(db=db_session)
    yield service  # breakpoint on yield: service is ready, test about to run
    # breakpoint after yield: test has finished, cleanup runs here
    db_session.rollback()
```

---

## Interview Sound Bite

Django debug requires `--noreload --nothreading` in the run config or the debugger attaches to the wrong process. Set breakpoints at view entry (inspect request), after DB queries (inspect ORM result), and in middleware `__call__` before and after `get_response`. For pytest, add `-s` to arguments so `print()` and `breakpoint()` work. For parametrized tests, use a conditional breakpoint (`order_id == "failing_case"`) to debug only the failing iteration without stepping through all cases. FastAPI Pydantic models expose `.dict()` and `.json()` in Evaluate Expression for clean inspection.
