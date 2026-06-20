# Python Backend APIs — FastAPI, Flask & Patterns — Gold Sheet

> **Track**: Python Interview Track — Group 2: Intermediate Backend  
> **File**: 5 of 5 (Track File #12)  
> **Audience**: Java developers learning Python for MAANG-level interviews  
> **Read after**: Python-File-IO-Serialization-JSON-Pickle-Gold-Sheet.md

---

## 1. Interview Priority Meter

| Topic | Frequency | Why It Gets Java Devs |
|---|---|---|
| WSGI vs ASGI — what each means | ★★★★★ | Java has Servlet API; Python has two parallel standards; interviews always ask this |
| FastAPI request lifecycle — from HTTP to response | ★★★★★ | Spring MVC DispatcherServlet equivalent; most interviewers want the full picture |
| Pydantic in FastAPI — request parsing, response model | ★★★★★ | Spring `@RequestBody` + Bean Validation combined; FastAPI does it via type hints |
| `Depends()` — dependency injection system | ★★★★★ | Spring `@Autowired` equivalent; async-capable; per-request or shared |
| Path parameters, query params, request body | ★★★★★ | Spring `@PathVariable`, `@RequestParam`, `@RequestBody` — Python uses just type hints |
| Background tasks vs async endpoints | ★★★★☆ | Spring `@Async` equivalent; `BackgroundTasks` vs `asyncio.create_task` |
| Middleware — logging, auth, CORS | ★★★★☆ | Spring `HandlerInterceptor` / filter chain; FastAPI uses Starlette middleware |
| Exception handlers — `@app.exception_handler` | ★★★★☆ | Spring `@ControllerAdvice` / `@ExceptionHandler` — same concept |
| FastAPI vs Flask — when to choose | ★★★★☆ | Both appear in job descriptions; need to articulate async, type safety differences |
| Response models — `response_model=` | ★★★☆☆ | Spring `@JsonView` equivalent; filters fields in the response |
| Lifespan events — startup/shutdown | ★★★☆☆ | Spring `@PostConstruct` / `@PreDestroy` / ApplicationListener |
| Testing FastAPI — `TestClient` | ★★★★☆ | Spring `MockMvc` equivalent; synchronous test client over async app |

---

## 2. WSGI vs ASGI — The Foundation

### Must Know

```
WSGI (Web Server Gateway Interface) — PEP 3333, 2010
  Synchronous only. Each request blocks a thread until the response is returned.
  Servers: Gunicorn, uWSGI
  Frameworks: Flask (2.x sync), Django (sync by default)
  Java equivalent: Servlet API (javax.servlet / jakarta.servlet)
  
  WSGI app signature:
  def application(environ: dict, start_response: callable) -> Iterable[bytes]:
      start_response("200 OK", [("Content-Type", "text/plain")])
      return [b"Hello, World!"]

ASGI (Asynchronous Server Gateway Interface) — 2018
  Supports async/await, WebSockets, HTTP/2, long-polling.
  Servers: Uvicorn, Hypercorn, Daphne
  Frameworks: FastAPI, Starlette, Django (async views, 3.1+)
  Java equivalent: Jakarta Servlet 4+ async support / Netty / Project Reactor
  
  ASGI app signature:
  async def application(scope: dict, receive: callable, send: callable) -> None:
      ...
```

### When to Use Each

```
Use ASGI / FastAPI when:
  - Many concurrent I/O-bound requests (DB queries, HTTP calls, file I/O)
  - WebSocket support needed
  - High-throughput REST APIs with async database drivers (asyncpg, motor)
  - Modern Python stack (3.8+)

Use WSGI / Flask when:
  - Simple APIs with synchronous code and blocking libraries
  - Team is familiar with Flask; migration cost not justified
  - Using legacy libraries that don't support async
  - Small services or prototypes

Both serve JSON REST APIs equally well for typical CRUD operations.
Performance difference only matters under high concurrency with I/O-bound operations.
```

---

## 3. FastAPI — Request Lifecycle

### Full Request Flow

```
1. HTTP request arrives at Uvicorn (ASGI server)
2. Uvicorn hands scope/receive/send to FastAPI (Starlette) ASGI app
3. Starlette middleware chain processes the request:
   a. TrustedHostMiddleware
   b. CORSMiddleware
   c. Custom middleware (auth, logging, tracing)
4. Router matches URL path + HTTP method to endpoint function
5. FastAPI dependency injection resolves all Depends() recursively
6. FastAPI reads and validates:
   a. Path parameters (from URL)
   b. Query parameters (from URL query string)
   c. Request headers
   d. Cookies
   e. Request body (parsed via Pydantic model)
7. Endpoint function is called (sync or async)
8. Return value is serialized:
   a. If response_model= is set: Pydantic validates and filters the output
   b. JSONResponse serializes the result
9. Middleware chain processes the response (outbound)
10. Uvicorn sends HTTP response bytes to client
```

### Minimal Application

```python
from fastapi import FastAPI
from pydantic import BaseModel

app = FastAPI(
    title="My Service",
    version="1.0.0",
    description="Production Python service",
)

class HealthResponse(BaseModel):
    status: str
    version: str

@app.get("/health", response_model=HealthResponse)
async def health_check() -> HealthResponse:
    return HealthResponse(status="ok", version="1.0.0")

# Run: uvicorn myapp.main:app --host 0.0.0.0 --port 8080 --reload
# Swagger UI: http://localhost:8080/docs
# ReDoc:      http://localhost:8080/redoc
# OpenAPI JSON: http://localhost:8080/openapi.json
```

---

## 4. Routing — Path Parameters, Query Params, Request Body

### Path Parameters

```python
from fastapi import FastAPI, Path
from uuid import UUID

app = FastAPI()

# Basic path parameter
@app.get("/users/{user_id}")
async def get_user(user_id: int) -> dict:
    # FastAPI validates that user_id is an int; 404 if path doesn't match
    return {"user_id": user_id}

# Multiple path parameters
@app.get("/orgs/{org_id}/repos/{repo_id}")
async def get_repo(org_id: str, repo_id: str) -> dict:
    return {"org": org_id, "repo": repo_id}

# UUID path parameter — FastAPI parses and validates automatically
@app.get("/orders/{order_id}")
async def get_order(order_id: UUID) -> dict:
    return {"order_id": str(order_id)}

# Path with validation constraints
@app.get("/items/{item_id}")
async def get_item(
    item_id: int = Path(gt=0, le=1000, description="Item ID between 1 and 1000")
) -> dict:
    return {"item_id": item_id}

# Fixed path must be declared BEFORE dynamic path to avoid shadowing
@app.get("/users/me")        # MUST come before /users/{user_id}
async def get_current_user() -> dict:
    return {"user": "current"}

@app.get("/users/{user_id}")
async def get_user_by_id(user_id: int) -> dict:
    return {"user_id": user_id}
```

### Query Parameters

```python
from fastapi import FastAPI, Query
from typing import Annotated

app = FastAPI()

# Simple query params — any param not in path is a query param
# GET /search?q=python&limit=10&offset=0
@app.get("/search")
async def search(
    q: str,                          # Required query param (?q=...)
    limit: int = 10,                 # Optional with default
    offset: int = 0,                 # Optional with default
    active: bool = True,             # FastAPI parses "true"/"false"/"1"/"0"/"yes"/"no"
) -> dict:
    return {"q": q, "limit": limit, "offset": offset, "active": active}

# Optional query param
from typing import Optional

@app.get("/users")
async def list_users(
    department: Optional[str] = None,   # Omitting the param = None
    min_age: int | None = None,         # Python 3.10+ style
) -> dict:
    return {"department": department, "min_age": min_age}

# Query param with validation
@app.get("/products")
async def list_products(
    page: Annotated[int, Query(ge=1, description="Page number")] = 1,
    page_size: Annotated[int, Query(ge=1, le=100)] = 20,
    tag: Annotated[list[str], Query()] = [],  # Multi-value: ?tag=a&tag=b
) -> dict:
    return {"page": page, "page_size": page_size, "tags": tag}
```

### Request Body

```python
from fastapi import FastAPI
from pydantic import BaseModel, Field
from datetime import datetime
from uuid import UUID

app = FastAPI()

class CreateUserRequest(BaseModel):
    name: str = Field(min_length=1, max_length=100)
    email: str = Field(pattern=r"^[^@]+@[^@]+\.[^@]+$")
    age: int = Field(ge=18, le=120)
    role: str = "user"

class UserResponse(BaseModel):
    id: UUID
    name: str
    email: str
    created_at: datetime

@app.post("/users", response_model=UserResponse, status_code=201)
async def create_user(body: CreateUserRequest) -> UserResponse:
    # body is already validated by Pydantic — if invalid, FastAPI returns 422
    # Spring equivalent: @PostMapping + @RequestBody + @Valid
    user = UserResponse(
        id=UUID("12345678-1234-5678-1234-567812345678"),
        name=body.name,
        email=body.email,
        created_at=datetime.now(),
    )
    return user
```

### Mixing Path, Query, and Body

```python
from fastapi import FastAPI, Path, Query
from pydantic import BaseModel

app = FastAPI()

class UpdateItemRequest(BaseModel):
    name: str
    price: float

@app.put("/stores/{store_id}/items/{item_id}")
async def update_item(
    store_id: int = Path(gt=0),             # Path param
    item_id: int = Path(gt=0),             # Path param
    notify: bool = Query(default=False),    # Query param
    body: UpdateItemRequest = ...,          # Request body (... = required)
) -> dict:
    return {
        "store_id": store_id,
        "item_id": item_id,
        "notify": notify,
        "updated": body.model_dump(),
    }
```

### Headers and Cookies

```python
from fastapi import FastAPI, Header, Cookie
from typing import Annotated

app = FastAPI()

@app.get("/secure")
async def secure_endpoint(
    # FastAPI automatically converts header names: X-Token → x_token
    x_token: Annotated[str, Header()] = ...,
    user_agent: Annotated[str | None, Header()] = None,
    session_id: Annotated[str | None, Cookie()] = None,
) -> dict:
    return {"token": x_token, "ua": user_agent, "session": session_id}
```

---

## 5. Response Models and Status Codes

### `response_model` — Filter Output Fields

```python
from fastapi import FastAPI
from pydantic import BaseModel

app = FastAPI()

class UserInDB(BaseModel):
    id: int
    name: str
    email: str
    hashed_password: str    # NEVER return this!
    role: str

class UserPublicResponse(BaseModel):
    id: int
    name: str
    email: str
    role: str

@app.get("/users/{user_id}", response_model=UserPublicResponse)
async def get_user(user_id: int) -> UserInDB:
    # Return a UserInDB (which has hashed_password)
    # FastAPI uses response_model to FILTER — only UserPublicResponse fields are sent
    # hashed_password is stripped from the response!
    return UserInDB(
        id=user_id, name="Alice", email="a@b.com",
        hashed_password="$2b$12$...", role="user"
    )

# response_model_exclude and response_model_include for fine control
@app.get("/users/{user_id}/summary", response_model=UserPublicResponse,
         response_model_exclude={"role"})
async def get_user_summary(user_id: int) -> UserInDB:
    ...
```

### HTTP Status Codes

```python
from fastapi import FastAPI, status
from fastapi.responses import JSONResponse, Response

app = FastAPI()

# status_code parameter
@app.post("/items", status_code=status.HTTP_201_CREATED)
async def create_item(): ...

@app.delete("/items/{item_id}", status_code=status.HTTP_204_NO_CONTENT)
async def delete_item(item_id: int) -> None:
    return None   # 204 — no body

# Dynamic status codes using Response
@app.get("/items/{item_id}")
async def get_item(item_id: int, response: Response) -> dict:
    if item_id > 1000:
        response.status_code = status.HTTP_404_NOT_FOUND
        return {"error": "not found"}
    return {"item_id": item_id}

# Return different response types
from fastapi.responses import PlainTextResponse, HTMLResponse, StreamingResponse
import io

@app.get("/export/csv")
async def export_csv() -> StreamingResponse:
    data = "name,age\nAlice,30\n"
    return StreamingResponse(
        io.StringIO(data),
        media_type="text/csv",
        headers={"Content-Disposition": "attachment; filename=export.csv"}
    )
```

---

## 6. Dependency Injection — `Depends()`

### Must Know

FastAPI's dependency injection system is powerful and composable. Dependencies are declared as function parameters using `Depends()`. They can be async, can have their own dependencies (nested), and support lifecycle management (with `yield`).

```python
from fastapi import FastAPI, Depends, HTTPException, status
from typing import Annotated

app = FastAPI()

# Simple dependency — shared logic extracted into a function
def get_pagination(page: int = 1, page_size: int = 20) -> dict:
    """Reusable pagination params — declared once, used everywhere."""
    return {"offset": (page - 1) * page_size, "limit": page_size}

# Inject via Depends
@app.get("/users")
async def list_users(pagination: Annotated[dict, Depends(get_pagination)]) -> dict:
    return {"pagination": pagination, "users": []}

@app.get("/products")
async def list_products(pagination: Annotated[dict, Depends(get_pagination)]) -> dict:
    return {"pagination": pagination, "products": []}
```

### Authentication Dependency

```python
from fastapi import FastAPI, Depends, HTTPException, status, Header
from typing import Annotated

app = FastAPI()

# Fake token verification — use proper JWT in production
def verify_token(authorization: Annotated[str, Header()] = ...) -> str:
    if not authorization.startswith("Bearer "):
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED,
            detail="Invalid authorization header",
            headers={"WWW-Authenticate": "Bearer"},
        )
    token = authorization.split(" ", 1)[1]
    if token != "valid-token":   # Real code: jwt.decode(token, SECRET_KEY, ...)
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED,
            detail="Invalid token",
        )
    return token

@app.get("/protected")
async def protected_route(token: Annotated[str, Depends(verify_token)]) -> dict:
    return {"message": "You are authenticated", "token": token}
```

### Database Session Dependency (with `yield`)

```python
from fastapi import FastAPI, Depends
from typing import Annotated, Generator
from contextlib import contextmanager

app = FastAPI()

# Dependency with yield — setup + teardown per request
# Equivalent to Spring's @Transactional setup/teardown, or @Scope("request") beans

class FakeDB:
    def __init__(self):
        self.open = True
        print("DB connection opened")

    def query(self, sql: str) -> list:
        return [{"id": 1, "name": "Alice"}]

    def close(self):
        self.open = False
        print("DB connection closed")

def get_db() -> Generator[FakeDB, None, None]:
    db = FakeDB()
    try:
        yield db        # Provides the dependency value
    finally:
        db.close()      # Always runs after request (even on exception)

@app.get("/users")
async def list_users(db: Annotated[FakeDB, Depends(get_db)]) -> list:
    return db.query("SELECT * FROM users")

# With SQLAlchemy async:
# from sqlalchemy.ext.asyncio import AsyncSession, async_sessionmaker
# async def get_db_session(session_factory: ...) -> AsyncGenerator[AsyncSession, None]:
#     async with session_factory() as session:
#         yield session
```

### Dependency on Dependency (Chaining)

```python
from fastapi import FastAPI, Depends, HTTPException
from typing import Annotated

app = FastAPI()

class UserContext:
    def __init__(self, user_id: int, roles: list[str]):
        self.user_id = user_id
        self.roles = roles

def get_current_user(token: Annotated[str, Depends(verify_token)]) -> UserContext:
    # verify_token runs first, then this function uses its result
    # Simulates looking up user from token
    return UserContext(user_id=1, roles=["read", "write"])

def require_admin(user: Annotated[UserContext, Depends(get_current_user)]) -> UserContext:
    if "admin" not in user.roles:
        raise HTTPException(status_code=403, detail="Admin role required")
    return user

@app.delete("/users/{user_id}")
async def delete_user(
    user_id: int,
    admin: Annotated[UserContext, Depends(require_admin)],  # Chains: verify_token → get_current_user → require_admin
) -> dict:
    return {"deleted": user_id, "by": admin.user_id}
```

---

## 7. Routers — Organizing Large Applications

```python
# users/router.py
from fastapi import APIRouter, Depends
from typing import Annotated

# APIRouter — like Spring's @RestController with a base path
router = APIRouter(
    prefix="/users",
    tags=["users"],            # Groups in Swagger UI
    responses={404: {"description": "User not found"}},
)

@router.get("/")
async def list_users() -> list:
    return []

@router.get("/{user_id}")
async def get_user(user_id: int) -> dict:
    return {"user_id": user_id}

@router.post("/", status_code=201)
async def create_user() -> dict:
    return {}

# main.py
from fastapi import FastAPI
from users.router import router as users_router
from orders.router import router as orders_router

app = FastAPI()
app.include_router(users_router)
app.include_router(orders_router)
app.include_router(
    users_router,
    prefix="/api/v2",   # Override prefix for versioning
    tags=["users-v2"],
)
```

---

## 8. Middleware

```python
from fastapi import FastAPI, Request, Response
from fastapi.middleware.cors import CORSMiddleware
from fastapi.middleware.trustedhost import TrustedHostMiddleware
import time
import uuid
import logging

app = FastAPI()

# CORS middleware — required for browser-based clients
app.add_middleware(
    CORSMiddleware,
    allow_origins=["https://myapp.com", "https://staging.myapp.com"],
    allow_credentials=True,
    allow_methods=["GET", "POST", "PUT", "DELETE", "PATCH"],
    allow_headers=["Authorization", "Content-Type", "X-Request-ID"],
)

# Trusted host middleware — prevent Host header attacks
app.add_middleware(
    TrustedHostMiddleware,
    allowed_hosts=["myapp.com", "*.myapp.com", "localhost"],
)

# Custom middleware — request/response logging with timing
@app.middleware("http")
async def logging_middleware(request: Request, call_next) -> Response:
    request_id = str(uuid.uuid4())
    start_time = time.perf_counter()

    # Attach request ID to request state (accessible in endpoints)
    request.state.request_id = request_id

    logging.info(
        "Request started",
        extra={"request_id": request_id, "method": request.method, "path": request.url.path}
    )

    response: Response = await call_next(request)   # Call the next handler

    duration_ms = (time.perf_counter() - start_time) * 1000
    response.headers["X-Request-ID"] = request_id
    response.headers["X-Response-Time"] = f"{duration_ms:.2f}ms"

    logging.info(
        "Request completed",
        extra={
            "request_id": request_id,
            "status_code": response.status_code,
            "duration_ms": round(duration_ms, 2),
        }
    )
    return response
```

---

## 9. Exception Handling

```python
from fastapi import FastAPI, HTTPException, Request, status
from fastapi.responses import JSONResponse
from fastapi.exception_handlers import http_exception_handler
from pydantic import ValidationError

app = FastAPI()

# HTTPException — the standard way to return error responses
@app.get("/users/{user_id}")
async def get_user(user_id: int) -> dict:
    if user_id <= 0:
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail="user_id must be positive",
        )
    if user_id > 9999:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail=f"User {user_id} not found",
            headers={"X-Error-Code": "USER_NOT_FOUND"},
        )
    return {"user_id": user_id}

# Custom exception class
class AppError(Exception):
    def __init__(self, code: str, message: str, status_code: int = 400):
        self.code = code
        self.message = message
        self.status_code = status_code

# Custom exception handler — like Spring @ControllerAdvice + @ExceptionHandler
@app.exception_handler(AppError)
async def app_error_handler(request: Request, exc: AppError) -> JSONResponse:
    return JSONResponse(
        status_code=exc.status_code,
        content={"error": exc.code, "message": exc.message},
    )

@app.exception_handler(404)
async def not_found_handler(request: Request, exc: HTTPException) -> JSONResponse:
    return JSONResponse(
        status_code=404,
        content={"error": "NOT_FOUND", "path": str(request.url.path)},
    )

# Validation errors (422) are handled automatically by FastAPI
# Override to customize the response format:
from fastapi.exceptions import RequestValidationError

@app.exception_handler(RequestValidationError)
async def validation_error_handler(request: Request, exc: RequestValidationError) -> JSONResponse:
    return JSONResponse(
        status_code=status.HTTP_422_UNPROCESSABLE_ENTITY,
        content={
            "error": "VALIDATION_ERROR",
            "details": exc.errors(),   # List of field errors from Pydantic
        },
    )
```

---

## 10. Async Endpoints and Background Tasks

### Async vs Sync Endpoints

```python
from fastapi import FastAPI
import asyncio
import httpx

app = FastAPI()

# ASYNC endpoint — use when calling async I/O (DB, HTTP, Redis, etc.)
@app.get("/async-data")
async def fetch_data() -> dict:
    async with httpx.AsyncClient() as client:
        response = await client.get("https://api.example.com/data")
    return response.json()

# SYNC endpoint — FastAPI runs it in a threadpool automatically
# Use when calling blocking libraries (boto3, psycopg2, requests, etc.)
@app.get("/sync-data")
def get_data_sync() -> dict:
    import requests   # Blocking HTTP client
    response = requests.get("https://api.example.com/data")
    return response.json()

# TRAP: calling blocking code in an async endpoint blocks the event loop!
@app.get("/bad-async")
async def bad_async_endpoint() -> dict:
    import time
    time.sleep(5)   # BLOCKS the event loop! All other requests wait 5 seconds
    return {}

# FIX: run blocking code in a threadpool
@app.get("/good-async")
async def good_async_endpoint() -> dict:
    import asyncio
    import time

    loop = asyncio.get_event_loop()
    result = await loop.run_in_executor(None, lambda: time.sleep(5) or {"done": True})
    return result

# Or use anyio.to_thread.run_sync (preferred in modern FastAPI)
import anyio

@app.get("/blocking-in-thread")
async def with_thread() -> dict:
    result = await anyio.to_thread.run_sync(some_blocking_function)
    return {"result": result}
```

### Background Tasks

```python
from fastapi import FastAPI, BackgroundTasks
import logging

app = FastAPI()

def send_welcome_email(email: str, name: str) -> None:
    """Runs in background AFTER response is sent to client."""
    logging.info(f"Sending welcome email to {email}")
    # ... email sending logic ...

def log_signup(user_id: int) -> None:
    logging.info(f"User {user_id} signed up")

@app.post("/users", status_code=201)
async def create_user(
    background_tasks: BackgroundTasks,
    # ... body ...
) -> dict:
    user = {"id": 1, "email": "alice@example.com", "name": "Alice"}

    # Register background tasks — they run AFTER response is sent
    background_tasks.add_task(send_welcome_email, user["email"], user["name"])
    background_tasks.add_task(log_signup, user["id"])

    # Response is returned immediately — client doesn't wait for background tasks
    return user

# IMPORTANT: BackgroundTasks run in the SAME process, not a separate worker
# For heavy background work (ML inference, image processing), use Celery or ARQ
```

---

## 11. Lifespan Events — Startup and Shutdown

```python
from fastapi import FastAPI
from contextlib import asynccontextmanager
import httpx

# Modern approach (FastAPI 0.93+) — lifespan context manager
@asynccontextmanager
async def lifespan(app: FastAPI):
    # STARTUP: runs before the app starts accepting requests
    print("Starting up...")
    app.state.http_client = httpx.AsyncClient()    # Shared HTTP client
    app.state.db_pool = await create_db_pool()     # DB connection pool
    yield   # Application runs here
    # SHUTDOWN: runs after all requests are handled, before process exits
    print("Shutting down...")
    await app.state.http_client.aclose()
    await app.state.db_pool.close()

app = FastAPI(lifespan=lifespan)

# Access shared resources in endpoints
@app.get("/status")
async def status(request: Request) -> dict:
    # request.app.state.http_client is available here
    return {"db": "connected"}

# Java equivalent:
# @PostConstruct   → startup code
# @PreDestroy      → shutdown code
# ApplicationListener<ContextRefreshedEvent>
# ApplicationListener<ContextClosedEvent>
```

---

## 12. Flask — Comparison and When to Choose

### Flask Quick Reference

```python
# pip install flask
from flask import Flask, request, jsonify, abort
from functools import wraps

app = Flask(__name__)

# Basic route
@app.route("/health", methods=["GET"])
def health_check():
    return jsonify({"status": "ok"})

# Path parameter
@app.route("/users/<int:user_id>", methods=["GET"])
def get_user(user_id: int):
    return jsonify({"user_id": user_id})

# Request body — no automatic Pydantic validation
@app.route("/users", methods=["POST"])
def create_user():
    data = request.get_json()   # Returns dict or None
    if not data:
        abort(400, "JSON body required")
    name = data.get("name")
    if not name:
        abort(400, "name required")
    return jsonify({"id": 1, "name": name}), 201

# Query parameters
@app.route("/search")
def search():
    q = request.args.get("q", "")
    limit = int(request.args.get("limit", 10))
    return jsonify({"q": q, "limit": limit})

# Error handler
@app.errorhandler(404)
def not_found(e):
    return jsonify({"error": "Not found"}), 404

# Run
if __name__ == "__main__":
    app.run(host="0.0.0.0", port=8080, debug=True)
# Production: gunicorn -w 4 myapp:app
```

### Flask Blueprints — Like FastAPI Routers

```python
from flask import Blueprint, jsonify

users_bp = Blueprint("users", __name__, url_prefix="/users")

@users_bp.route("/", methods=["GET"])
def list_users():
    return jsonify([])

@users_bp.route("/<int:user_id>", methods=["GET"])
def get_user(user_id):
    return jsonify({"user_id": user_id})

# Register blueprint in app factory
def create_app() -> Flask:
    app = Flask(__name__)
    app.register_blueprint(users_bp)
    return app
```

---

## 13. FastAPI vs Flask — Decision Matrix

| Feature | FastAPI | Flask |
|---|---|---|
| Performance | High (async, ASGI) | Moderate (sync, WSGI) |
| Type safety | Built-in (Pydantic + type hints) | Manual validation required |
| Request validation | Automatic (422 on bad input) | Manual (`abort(400, ...)`) |
| API docs | Auto-generated (Swagger + ReDoc) | Extension needed (flask-swagger) |
| Dependency injection | Built-in (`Depends()`) | Manual (Flask-Injector or manual) |
| WebSocket support | Built-in (ASGI) | Requires extension (flask-socketio) |
| Async support | Native (`async def`) | Flask 2.x supports `async def` but WSGI limits it |
| Learning curve | Moderate (type hints + Pydantic) | Low (simple and explicit) |
| Ecosystem maturity | Newer (2018) — rapidly growing | Mature (2010) — huge ecosystem |
| Use case | High-concurrency APIs, ML serving | Simple REST APIs, quick prototypes |
| Testing | `TestClient` (sync over async) | `app.test_client()` |
| Background jobs | `BackgroundTasks` (same process) | `celery` (separate worker) |

---

## 14. Testing FastAPI

```python
# pip install httpx pytest pytest-asyncio
from fastapi.testclient import TestClient
from fastapi import FastAPI
from pydantic import BaseModel

app = FastAPI()

class Item(BaseModel):
    name: str
    price: float

@app.post("/items", status_code=201)
async def create_item(item: Item) -> Item:
    return item

@app.get("/items/{item_id}")
async def get_item(item_id: int) -> dict:
    if item_id != 1:
        from fastapi import HTTPException
        raise HTTPException(status_code=404, detail="Not found")
    return {"item_id": item_id, "name": "Widget"}

# TestClient wraps the async app synchronously — no pytest-asyncio needed for HTTP tests
client = TestClient(app)

def test_create_item():
    response = client.post("/items", json={"name": "Gadget", "price": 9.99})
    assert response.status_code == 201
    assert response.json() == {"name": "Gadget", "price": 9.99}

def test_get_item_found():
    response = client.get("/items/1")
    assert response.status_code == 200
    assert response.json()["name"] == "Widget"

def test_get_item_not_found():
    response = client.get("/items/999")
    assert response.status_code == 404
    assert response.json()["detail"] == "Not found"

def test_create_item_validation_error():
    response = client.post("/items", json={"name": "Widget"})   # Missing price
    assert response.status_code == 422   # Pydantic validation error

# Override dependencies in tests
from fastapi import Depends
from typing import Annotated

def get_current_user() -> dict:
    return {"user_id": 1}

def get_test_user() -> dict:
    return {"user_id": 999, "is_test": True}

def test_with_dependency_override():
    app.dependency_overrides[get_current_user] = get_test_user
    response = client.get("/profile")
    assert response.status_code == 200
    app.dependency_overrides.clear()   # Always clean up!
```

---

## 15. Java Developer Bridge — Complete Comparison

| Concept | Java (Spring Boot) | Python (FastAPI) |
|---|---|---|
| Web framework | Spring MVC / Spring WebFlux | FastAPI (ASGI) / Flask (WSGI) |
| HTTP server | Tomcat / Netty / Undertow | Uvicorn / Hypercorn / Gunicorn |
| Request mapping | `@GetMapping("/users/{id}")` | `@app.get("/users/{id}")` |
| Path variable | `@PathVariable int id` | `id: int` in function params |
| Query param | `@RequestParam(defaultValue="10") int limit` | `limit: int = 10` |
| Request body | `@RequestBody @Valid CreateUserDto body` | `body: CreateUserRequest` (Pydantic auto-validates) |
| Validation | Bean Validation (`@NotNull`, `@Min`, `@Valid`) | Pydantic `Field(ge=0)` + auto-422 |
| Validation error response | 400 + `ConstraintViolationException` | 422 + Pydantic `ValidationError` details |
| Response DTO filtering | `@JsonView` / `@JsonIgnore` | `response_model=PublicSchema` |
| HTTP status code | `@ResponseStatus(HttpStatus.CREATED)` | `status_code=201` or `status.HTTP_201_CREATED` |
| Dependency injection | `@Autowired` / constructor injection | `Depends(get_dependency)` |
| Request-scoped bean | `@Scope("request")` | `Depends()` — new instance per request |
| Shared singleton | `@Bean @Scope("singleton")` | Module-level variable / `lifespan` state |
| Exception handling | `@ControllerAdvice + @ExceptionHandler` | `@app.exception_handler(ExcClass)` |
| Middleware / Filter | `HandlerInterceptor` / `OncePerRequestFilter` | `@app.middleware("http")` |
| CORS | `@CrossOrigin` / `CorsConfigurationSource` | `CORSMiddleware` |
| Async processing | `@Async` + `CompletableFuture` | `async def` endpoint |
| Blocking in async | `ExecutorService` | `anyio.to_thread.run_sync()` |
| Background task | `@Async` + fire-and-forget | `BackgroundTasks.add_task()` |
| App startup/shutdown | `@PostConstruct` / `@PreDestroy` | `lifespan` async context manager |
| API documentation | Springdoc OpenAPI / Swagger | Auto-generated at `/docs` and `/redoc` |
| Integration testing | `MockMvc` / `WebTestClient` | `TestClient` (httpx-based) |
| Grouping controllers | `@RestController` with `@RequestMapping` prefix | `APIRouter(prefix="/prefix", tags=["tag"])` |
| Header param | `@RequestHeader("X-Token") String token` | `x_token: Annotated[str, Header()]` |
| Cookie | `@CookieValue String sessionId` | `session_id: Annotated[str, Cookie()]` |

---

## 16. Hot Interview Q&A

**Q: What is the difference between WSGI and ASGI?**  
A: WSGI (PEP 3333) is a synchronous interface — each request is handled by a callable that returns a response; the server calls it once per request and blocks until it returns. ASGI is asynchronous — the server communicates via `scope`, `receive`, and `send` coroutines, allowing a single thread to handle thousands of concurrent connections using `async/await`. WSGI servers (Gunicorn) are limited by thread count; ASGI servers (Uvicorn) are limited by the event loop's I/O concurrency. FastAPI requires ASGI; Flask is WSGI by default.

**Q: How does FastAPI validate request data, and what happens when validation fails?**  
A: FastAPI reads function parameter type annotations and builds Pydantic models from them automatically. When a request arrives, FastAPI runs Pydantic validation on path params, query params, headers, cookies, and request body. If any validation fails, FastAPI automatically returns HTTP 422 Unprocessable Entity with a detailed JSON body listing every invalid field. No code is needed — this is entirely automatic from type hints. In Spring Boot, the equivalent requires `@Valid` + Bean Validation annotations + `@ExceptionHandler(MethodArgumentNotValidException.class)`.

**Q: Explain FastAPI's `Depends()` system. How is it different from Spring's `@Autowired`?**  
A: `Depends()` injects values into endpoint functions per-request, not per-container-startup. When a request arrives, FastAPI calls each dependency function, uses its return value, and injects it. Dependencies can be async, can have their own dependencies (chained), and can use `yield` for setup/teardown (like a `try/finally` block per request). Spring's `@Autowired` resolves beans at startup and injects singletons. FastAPI's `Depends()` is more like Spring's `@Scope("request")` beans — created fresh per request and destroyed after.

**Q: What is the difference between declaring an endpoint as `async def` vs `def` in FastAPI?**  
A: `async def` endpoints run directly in the event loop — they should use `await` for I/O operations and must never call blocking code (no `time.sleep()`, no synchronous DB drivers). `def` (sync) endpoints are automatically run by FastAPI in a separate thread pool (`run_in_executor`) — this allows blocking code to run without blocking the event loop. If you accidentally put blocking code in an `async def` endpoint, you block the event loop and no other requests can be processed concurrently.

**Q: How does `response_model` work in FastAPI?**  
A: `response_model` tells FastAPI to validate and filter the return value through a specified Pydantic model before sending it to the client. Even if your endpoint returns an object with extra fields (like `hashed_password`), only fields defined in the `response_model` schema will appear in the JSON response. This prevents accidentally leaking internal data. Pydantic also validates that all required fields are present in the return value. It is equivalent to Jackson's `@JsonView` or `@JsonIgnore` in Spring.

**Q: How do you test a FastAPI application?**  
A: Use `TestClient` from `fastapi.testclient` (wraps `httpx.Client`). It runs the ASGI app synchronously inside the test, so no `async`/`await` is needed in test functions. You can override dependencies with `app.dependency_overrides[original_dep] = mock_dep` to inject test doubles (fake DB sessions, fake users). Always call `app.dependency_overrides.clear()` after each test. This is analogous to Spring's `MockMvc` with `@MockBean`.

**Q: When would you choose Flask over FastAPI?**  
A: Choose Flask when: (1) The team is already familiar with Flask and migration cost is not justified; (2) The service uses blocking libraries that don't have async versions; (3) The codebase is predominantly synchronous and concurrency is not a concern; (4) Simplicity is preferred — Flask has a smaller, simpler API. Choose FastAPI when: (1) The service makes many concurrent I/O-bound calls (DB, HTTP, cache); (2) Auto-generated documentation is important; (3) Strong type safety and validation is desired; (4) WebSockets are needed; (5) Starting a new service from scratch.

**Q: What happens if you call `time.sleep()` inside an `async def` endpoint?**  
A: It blocks the entire event loop. Uvicorn runs on a single-threaded event loop. If `time.sleep(5)` is called inside an `async def` endpoint, no other coroutines (other requests, background tasks, connection handling) can run for those 5 seconds. All concurrent requests queue up. The fix is to either: (1) use `await asyncio.sleep(5)` (non-blocking sleep), (2) run the blocking call with `await anyio.to_thread.run_sync(lambda: time.sleep(5))`, or (3) declare the endpoint as `def` (sync) so FastAPI puts it in a thread pool automatically.

---

## 17. Final Revision Checklist

### WSGI / ASGI Foundations

- [ ] I know WSGI = synchronous, ASGI = asynchronous; ASGI enables `async def` endpoints
- [ ] I know Uvicorn/Hypercorn are ASGI servers; Gunicorn/uWSGI are WSGI servers
- [ ] I know Flask uses WSGI; FastAPI uses ASGI (Starlette)

### FastAPI Request Handling

- [ ] I know the full request lifecycle: Uvicorn → middleware → router → DI resolution → validation → endpoint → response_model → response
- [ ] I know path params (`{id}`), query params (non-path params), body (Pydantic model), headers (`Header()`), cookies (`Cookie()`)
- [ ] I know `Path(gt=0)`, `Query(ge=1, le=100)`, `Field()` for constraints
- [ ] I know 422 is returned automatically when Pydantic validation fails

### Dependency Injection

- [ ] I know `Depends(func)` injects per-request; chained dependencies work recursively
- [ ] I know `yield` inside a dependency enables setup + teardown (DB session pattern)
- [ ] I know `app.dependency_overrides` for test doubles

### Response and Error Handling

- [ ] I know `response_model=` filters output fields; prevents leaking internal data
- [ ] I can raise `HTTPException(status_code=..., detail=...)` for error responses
- [ ] I can write `@app.exception_handler(MyExc)` for global error handling
- [ ] I know `@app.middleware("http")` for logging, timing, auth middleware

### Async Patterns

- [ ] I know `async def` must never call blocking code — use `await anyio.to_thread.run_sync()`
- [ ] I know `def` (sync) endpoints run in a thread pool automatically — safe for blocking code
- [ ] I know `BackgroundTasks` runs tasks after response is sent, in the same process

### Testing

- [ ] I can use `TestClient` for synchronous tests of async FastAPI apps
- [ ] I can use `app.dependency_overrides` to inject test fakes
- [ ] I know to call `app.dependency_overrides.clear()` after each test

### Java Developer Reminders

- [ ] `@GetMapping` → `@app.get`; `@PathVariable` → path param in type hint; `@RequestBody` → Pydantic model param
- [ ] `@Valid` + Bean Validation → Pydantic model fields + `Field()` constraints — auto-422
- [ ] `@Autowired` → `Depends()`; `@Scope("request")` → per-request dependency with `yield`
- [ ] `@ControllerAdvice + @ExceptionHandler` → `@app.exception_handler(ExcType)`
- [ ] `MockMvc` → `TestClient`; `@MockBean` → `app.dependency_overrides`

---

*File 5 of 5 — Group 2: Intermediate Backend — GROUP 2 COMPLETE*  
*Next Group: 03-Senior-MAANG — Python-Concurrency-Threading-Multiprocessing-MAANG-Master-Sheet.md*
