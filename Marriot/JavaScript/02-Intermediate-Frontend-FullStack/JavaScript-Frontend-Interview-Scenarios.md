# JavaScript Frontend Interview Scenarios

Target: frontend, full-stack, React, JavaScript, TypeScript, and MAANG interviews where the interviewer tests practical UI bugs, async races, forms, state correctness, rendering performance, browser APIs, debugging, and production readiness.

This sheet covers:
- How to answer frontend scenario questions
- UI race conditions
- Stale async responses
- Debounce and throttle scenarios
- Forms, validation, and submission bugs
- State modeling mistakes
- Rendering and performance traps
- Event handling bugs
- DOM and browser API pitfalls
- Fetch, CORS, auth, and storage incidents
- Accessibility and focus bugs
- Memory leaks and cleanup
- Debugging workflows with DevTools
- Production hardening patterns
- MAANG-style spoken answers

How to use this:
- Read each scenario as an interview prompt.
- Practice answering before reading the strong answer.
- For every bug, identify symptom, cause, proof, fix, and prevention.
- Do not memorize code only. Memorize the reasoning pattern.

---

## 1. Scenario Answer Framework

Use this structure for frontend scenario answers:

```text
1. Clarify the symptom.
2. Identify likely causes.
3. Explain how to reproduce and measure.
4. Inspect browser evidence: console, network, performance, memory, DOM, accessibility.
5. Fix the root cause.
6. Add guardrails: tests, monitoring, types, validation, cleanup, UX states.
7. Explain trade-offs.
```

Short interview version:

```text
Symptom -> cause hypothesis -> debugging evidence -> fix -> prevention.
```

Strong line:

```text
I avoid guessing. For frontend bugs, I use DevTools evidence: network timing, console errors,
performance traces, event listeners, DOM state, storage, and reproduction steps.
```

---

## 2. Frontend Scenario Priority Meter

| Scenario | Priority | What It Tests |
|---|---:|---|
| Stale search results | Very high | Async race conditions |
| Double submit | Very high | Forms and idempotency |
| Slow table/list | Very high | Rendering and DOM size |
| Input lag | Very high | Main-thread blocking |
| Fetch 500 treated as success | Very high | Fetch semantics |
| CORS works in Postman but not browser | Very high | Browser security model |
| Token storage debate | Very high | XSS/CSRF trade-offs |
| Event delegation bug | High | DOM event model |
| Modal closes unexpectedly | High | Bubbling/propagation |
| Memory leak after navigation | Very high | Cleanup and lifecycle |
| Infinite re-render | High | State/effect dependencies |
| Stale closure | Very high | Closures and state updates |
| Debounce vs throttle | High | Noisy event control |
| Layout thrashing | High | Browser rendering performance |
| Accessibility broken by JS | High | Production UI quality |
| Offline/slow network behavior | Medium-high | Resilient UX |
| Optimistic UI rollback | Medium-high | State consistency |
| Cache shows stale data | Medium-high | Client cache strategy |
| Large file import freezes page | High | CPU work and workers |
| Third-party script breaks page | Medium | Isolation and defensive loading |

---

## 3. Debugging Toolkit

Use the browser like an observability platform.

| Tool | Use |
|---|---|
| Console | Errors, warnings, manual inspection |
| Network panel | Requests, status codes, headers, payloads, timing, CORS |
| Performance panel | Long tasks, layout, paint, scripting cost |
| Memory panel | Heap snapshots, detached DOM nodes, leaks |
| Elements panel | DOM structure, styles, event listeners, accessibility tree |
| Application panel | Cookies, localStorage, sessionStorage, IndexedDB, cache, service workers |
| Lighthouse | Performance, accessibility, SEO, best practices |
| React DevTools | Component tree, props, state, renders, profiler |
| Source maps | Debug original source in production-like builds |

Interview line:

```text
I start by reproducing the bug and then use the browser panel that matches the symptom: Network
for request issues, Performance for lag, Memory for leaks, Elements for DOM/CSS, and Application
for storage or service worker issues.
```

---

## 4. Scenario: Stale Search Results

Prompt:

```text
A search box calls the API on each keystroke. Users type quickly and sometimes old results replace
newer results.
```

Symptom:

```text
User types "java", sees correct results briefly, then older "ja" results appear.
```

Root cause:

```text
Multiple requests are in flight. Older slower response finishes after newer faster response and
overwrites current UI state.
```

Bad code:

```javascript
async function onSearch(query) {
    const response = await fetch(`/api/search?q=${query}`);
    const results = await response.json();
    renderResults(results);
}
```

Fix with request ID:

```javascript
let latestRequestId = 0;

async function onSearch(query) {
    const requestId = ++latestRequestId;
    const params = new URLSearchParams({ q: query });
    const response = await fetch(`/api/search?${params}`);

    if (!response.ok) {
        throw new Error(`HTTP ${response.status}`);
    }

    const results = await response.json();

    if (requestId !== latestRequestId) {
        return;
    }

    renderResults(results);
}
```

Fix with AbortController:

```javascript
let currentController;

async function onSearch(query) {
    currentController?.abort();
    currentController = new AbortController();

    try {
        const params = new URLSearchParams({ q: query });
        const response = await fetch(`/api/search?${params}`, {
            signal: currentController.signal
        });

        if (!response.ok) {
            throw new Error(`HTTP ${response.status}`);
        }

        renderResults(await response.json());
    } catch (error) {
        if (error.name !== "AbortError") {
            showError(error);
        }
    }
}
```

Strong answer:

```text
This is an async race condition. I would debounce input to reduce request volume, then prevent
stale responses from updating state using AbortController or request IDs. I would also handle
loading, error, and empty states explicitly.
```

---

## 5. Scenario: Search API Rate Limit

Prompt:

```text
Search works but the API team reports too many requests and rate-limit errors during typing.
```

Likely causes:

- API called on every keystroke.
- No debounce.
- No minimum query length.
- No cache or deduplication.
- Retrying rate-limited requests blindly.

Debounce fix:

```javascript
function debounce(fn, delayMs) {
    let timeoutId;

    return function debounced(...args) {
        clearTimeout(timeoutId);
        timeoutId = setTimeout(() => fn.apply(this, args), delayMs);
    };
}

const handleInput = debounce(event => {
    search(event.target.value);
}, 300);
```

Additional protections:

```javascript
async function search(query) {
    const normalized = query.trim();

    if (normalized.length < 2) {
        clearResults();
        return;
    }

    return fetchSearch(normalized);
}
```

Production hardening:

- Debounce client input.
- Minimum query length.
- Cancel stale requests.
- Cache repeated queries briefly.
- Backend rate limits.
- Respect 429 and `Retry-After`.
- Avoid retrying rate limits aggressively.

Strong answer:

```text
I would reduce request creation at the source with debounce and minimum query length, cancel stale
requests, and handle 429 responses respectfully. If the same query repeats, I may add short-lived
client caching or request deduplication.
```

---

## 6. Scenario: Double Form Submit

Prompt:

```text
Users double-click Submit and two orders are created.
```

Root causes:

- Submit button not disabled while submitting.
- Backend lacks idempotency.
- User can submit again on slow network.
- Client retries non-idempotent write.

Client fix:

```javascript
let isSubmitting = false;

form.addEventListener("submit", async event => {
    event.preventDefault();

    if (isSubmitting) {
        return;
    }

    isSubmitting = true;
    submitButton.disabled = true;

    try {
        await submitOrder(new FormData(form));
        showSuccess();
    } catch (error) {
        showError(error);
    } finally {
        isSubmitting = false;
        submitButton.disabled = false;
    }
});
```

Backend-safe answer:

```text
Client-side disabling improves UX but is not enough. The backend should protect writes with
idempotency keys or duplicate detection.
```

Idempotency idea:

```javascript
await fetch("/api/orders", {
    method: "POST",
    headers: {
        "Idempotency-Key": crypto.randomUUID()
    },
    body: JSON.stringify(payload)
});
```

Strong answer:

```text
I would prevent duplicate submission in the UI and require server-side idempotency for the actual
business guarantee. Client code can fail, be bypassed, or retry unexpectedly.
```

---

## 7. Scenario: Form Reloads Page Unexpectedly

Prompt:

```text
A JavaScript form handler runs, but the page reloads and loses state.
```

Cause:

```text
The submit event's default browser action was not prevented.
```

Bad:

```javascript
form.addEventListener("submit", () => {
    save();
});
```

Fix:

```javascript
form.addEventListener("submit", event => {
    event.preventDefault();
    save();
});
```

But do not blindly prevent default:

```text
If native form submission is desired as fallback, progressive enhancement may intentionally allow
normal submission when JavaScript is unavailable.
```

Strong answer:

```text
For JavaScript-controlled form submission, I call preventDefault on submit, not click, because
submit covers button clicks, Enter key, and other form submission paths.
```

---

## 8. Scenario: Enter Key Does Not Submit

Prompt:

```text
Clicking the button submits the form, but pressing Enter in the input does nothing.
```

Likely cause:

```text
The code listens only to button click instead of form submit, or the button type is wrong.
```

Bad:

```javascript
button.addEventListener("click", saveForm);
```

Better:

```javascript
form.addEventListener("submit", event => {
    event.preventDefault();
    saveForm();
});
```

HTML:

```html
<button type="submit">Save</button>
```

Strong answer:

```text
I handle the form submit event instead of only button click so mouse, keyboard Enter, and assistive
technology paths all work consistently.
```

---

## 9. Scenario: Validation Shows Wrong Error

Prompt:

```text
A form sometimes shows an error for a previous value even after the user fixed the input.
```

Likely causes:

- Async validation race.
- Stale closure over old value.
- Error state not cleared when input changes.
- Multiple validators resolving out of order.

Request ID fix:

```javascript
let validationVersion = 0;

async function validateEmail(email) {
    const version = ++validationVersion;

    clearEmailError();

    if (!email.includes("@")) {
        showEmailError("Invalid email");
        return;
    }

    const available = await checkEmailAvailable(email);

    if (version !== validationVersion) {
        return;
    }

    if (!available) {
        showEmailError("Email already exists");
    }
}
```

Strong answer:

```text
This is usually stale async validation. I would clear local errors on change, track validation
version, and only apply the result if it matches the latest input value.
```

---

## 10. Scenario: Stale Closure In Counter

Prompt:

```text
A counter increments incorrectly when clicking quickly or inside a timer.
```

Generic JavaScript stale closure:

```javascript
let count = 0;

function scheduleIncrement() {
    const captured = count;

    setTimeout(() => {
        count = captured + 1;
    }, 1000);
}
```

If called multiple times, each timer may capture the same old count.

Better:

```javascript
function scheduleIncrement() {
    setTimeout(() => {
        count = count + 1;
    }, 1000);
}
```

React-style fix:

```javascript
setCount(previous => previous + 1);
```

Strong answer:

```text
The bug happens because async callbacks close over values from when they were created. For state
updates based on previous state, I use the current value at execution time or a functional update.
```

---

## 11. Scenario: Infinite Re-render Or Effect Loop

Prompt:

```text
A component keeps re-rendering until the browser slows down.
```

Common causes:

- Updating state during render.
- Effect updates a dependency every time it runs.
- Creating unstable object/function dependencies.
- Missing dependency understanding.

Bad React-like example:

```javascript
function Component() {
    setCount(count + 1);
    return null;
}
```

Effect loop idea:

```javascript
useEffect(() => {
    setFilters({ status: "active" });
}, [filters]);
```

Because `setFilters` creates a new object, `filters` changes again.

Fix patterns:

- Do not set state unconditionally during render.
- Move initialization into initial state.
- Use stable dependencies.
- Use functional updates.
- Split effects by purpose.
- Avoid storing derived state unnecessarily.

Strong answer:

```text
I would inspect which state update triggers the render and whether an effect dependency changes
on every render. Then I would remove unnecessary state, stabilize dependencies, or move the update
to the correct event/effect boundary.
```

---

## 12. Scenario: Derived State Gets Out Of Sync

Prompt:

```text
UI shows total price that does not match selected items.
```

Cause:

```text
Total is stored separately and not updated in every path where items change.
```

Bad:

```javascript
let selectedItems = [];
let total = 0;

function addItem(item) {
    selectedItems.push(item);
    total += item.price;
}

function removeItem(id) {
    selectedItems = selectedItems.filter(item => item.id !== id);
    // forgot to update total
}
```

Better derive:

```javascript
function getTotal(items) {
    return items.reduce((sum, item) => sum + item.price, 0);
}
```

Strong answer:

```text
If a value can be derived from source state, storing it separately can create inconsistency. I
would derive total from selected items or centralize updates through a reducer.
```

---

## 13. Scenario: Mutating State Does Not Update UI

Prompt:

```text
A list item changes internally, but the UI does not re-render.
```

Cause:

```text
The code mutates existing object/array instead of creating a new reference expected by the UI framework.
```

Bad:

```javascript
items[0].selected = true;
setItems(items);
```

Better:

```javascript
setItems(items.map(item =>
    item.id === id
        ? { ...item, selected: true }
        : item
));
```

Deep update:

```javascript
setUser(user => ({
    ...user,
    profile: {
        ...user.profile,
        city: "NYC"
    }
}));
```

Strong answer:

```text
Many UI systems rely on reference changes to detect updates. I avoid mutating state directly and
create new objects or arrays for changed paths.
```

---

## 14. Scenario: Modal Closes When Clicking Inside

Prompt:

```text
Clicking inside a modal closes it, but only the overlay click should close it.
```

Cause:

```text
Click event bubbles from modal content to overlay listener.
```

Pattern 1: compare target/currentTarget:

```javascript
overlay.addEventListener("click", event => {
    if (event.target !== event.currentTarget) {
        return;
    }

    closeModal();
});
```

Pattern 2: stop propagation inside content:

```javascript
modalContent.addEventListener("click", event => {
    event.stopPropagation();
});
```

Preferred reasoning:

```text
Using target/currentTarget on overlay is often cleaner because it avoids interfering with other
bubbling behavior inside the modal.
```

Strong answer:

```text
I would use event.target and event.currentTarget to close only when the overlay itself was clicked.
I avoid broad stopPropagation unless necessary because it can break delegated handlers.
```

---

## 15. Scenario: Event Listener Not Removed

Prompt:

```text
A resize handler keeps running after leaving the page.
```

Cause:

```text
removeEventListener used a different function reference or cleanup was missing.
```

Bad:

```javascript
window.addEventListener("resize", () => updateLayout());
window.removeEventListener("resize", () => updateLayout());
```

Fix:

```javascript
function handleResize() {
    updateLayout();
}

window.addEventListener("resize", handleResize);
window.removeEventListener("resize", handleResize);
```

AbortController cleanup:

```javascript
const controller = new AbortController();

window.addEventListener("resize", handleResize, {
    signal: controller.signal
});

controller.abort();
```

Strong answer:

```text
Listeners must be cleaned up with the same function reference, or by using AbortSignal-based
listener cleanup. Missing cleanup can cause leaks and stale updates.
```

---

## 16. Scenario: Slow Large List

Prompt:

```text
A page renders 50,000 rows and becomes slow to load, scroll, and update.
```

Root causes:

- Too many DOM nodes.
- Too many event listeners.
- Expensive layout and paint.
- Re-rendering whole list for small changes.
- Heavy formatting inside render path.

Fixes:

- Virtualization/windowing.
- Pagination or infinite loading.
- Event delegation.
- Memoize expensive formatting carefully.
- Batch DOM updates.
- Avoid layout thrashing.
- Use stable keys in frameworks.

Plain DOM delegation:

```javascript
container.addEventListener("click", event => {
    const button = event.target.closest("button[data-row-id]");

    if (!button) {
        return;
    }

    handleRowAction(button.dataset.rowId);
});
```

Strong answer:

```text
I would reduce DOM size first through virtualization or pagination. Rendering fewer nodes usually
beats micro-optimizing handlers. Then I would use event delegation and profile layout, paint, and
scripting costs.
```

---

## 17. Scenario: Scroll Handler Causes Jank

Prompt:

```text
Scrolling feels choppy after adding analytics and sticky header logic.
```

Likely causes:

- Heavy work in scroll handler.
- Layout reads/writes mixed.
- Handler runs too often.
- Non-passive listener blocking scroll.

Bad:

```javascript
window.addEventListener("scroll", () => {
    expensiveAnalytics();
    header.style.top = `${window.scrollY}px`;
    console.log(header.offsetHeight);
});
```

Fixes:

```javascript
window.addEventListener("scroll", throttle(handleScroll, 100), {
    passive: true
});
```

Use CSS when possible:

```css
.header {
    position: sticky;
    top: 0;
}
```

Strong answer:

```text
I would profile scroll performance, make listeners passive when I do not call preventDefault,
throttle non-visual work, avoid layout thrashing, and prefer CSS sticky or IntersectionObserver
instead of doing everything in scroll events.
```

---

## 18. Scenario: Layout Thrashing

Prompt:

```text
A drag-and-drop page becomes slow as more cards are added.
```

Bad pattern:

```javascript
for (const card of cards) {
    card.style.width = "300px";
    const width = card.offsetWidth;
    card.style.left = `${width / 2}px`;
}
```

Problem:

```text
The code writes style, reads layout, writes style repeatedly. Reads may force synchronous layout.
```

Better:

```javascript
const widths = cards.map(card => card.offsetWidth);

cards.forEach((card, index) => {
    card.style.left = `${widths[index] / 2}px`;
});
```

Even better:

```text
Use CSS transforms, grid/flex layout, requestAnimationFrame batching, or a layout system that
avoids manual repeated measurement.
```

Strong answer:

```text
I would batch DOM reads separately from writes, then profile layout cost. Layout thrashing is a
classic sign of repeated read-write-read cycles.
```

---

## 19. Scenario: Large CSV Import Freezes Page

Prompt:

```text
A large CSV import uses async/await but the tab still freezes.
```

Cause:

```text
Parsing is CPU-heavy synchronous work. async/await does not move CPU work off the main thread.
```

Bad:

```javascript
async function importCsv(file) {
    const text = await file.text();
    const rows = parseHugeCsvSynchronously(text);
    renderRows(rows);
}
```

Fix options:

- Parse in Web Worker.
- Stream/chunk parsing.
- Show progress.
- Allow cancellation.
- Limit file size.
- Virtualize result rendering.

Worker idea:

```javascript
const worker = new Worker("csv-worker.js");

worker.postMessage({ file });

worker.addEventListener("message", event => {
    renderProgress(event.data.progress);
});
```

Strong answer:

```text
The issue is main-thread CPU blocking. I would move parsing to a Web Worker or process in chunks,
then virtualize rendering so parsing and displaying large data do not freeze input and paint.
```

---

## 20. Scenario: Fetch 404 Treated As Success

Prompt:

```text
A component shows success UI even when the API returns 404 or 500.
```

Cause:

```text
fetch resolves for HTTP error responses. It rejects mainly for network failures or aborts.
```

Bad:

```javascript
const response = await fetch("/api/bookings/123");
const data = await response.json();
showSuccess(data);
```

Fix:

```javascript
async function fetchJson(url, options) {
    const response = await fetch(url, options);

    if (!response.ok) {
        throw new Error(`HTTP ${response.status}`);
    }

    return response.json();
}
```

Strong answer:

```text
I would check response.ok before parsing success data. fetch does not reject on HTTP 4xx or 5xx,
so treating any Response as success is a common bug.
```

---

## 21. Scenario: CORS Works In Postman But Not Browser

Prompt:

```text
API works in Postman, but browser frontend gets a CORS error.
```

Cause:

```text
Postman is not bound by browser CORS enforcement. Browser JavaScript is.
```

Debug checklist:

```text
Check Origin request header.
Check OPTIONS preflight response.
Check Access-Control-Allow-Origin.
Check Access-Control-Allow-Methods.
Check Access-Control-Allow-Headers.
Check credentials mode.
Check cookies SameSite/Secure.
Check gateway/proxy headers.
```

Strong answer:

```text
CORS must be fixed on the server or gateway. Frontend JavaScript cannot bypass browser CORS. I
would inspect the preflight and actual response headers in DevTools Network panel.
```

---

## 22. Scenario: Cookie Auth Not Sent

Prompt:

```text
Login succeeds, but later API requests act unauthenticated in browser.
```

Likely causes:

- Missing `credentials: "include"` for cross-origin fetch.
- Server missing `Access-Control-Allow-Credentials: true`.
- Server uses wildcard origin with credentials.
- Cookie SameSite/Secure incompatible.
- HTTP instead of HTTPS for Secure cookie.
- Domain/path mismatch.

Fetch:

```javascript
await fetch("https://api.example.com/me", {
    credentials: "include"
});
```

Server CORS idea:

```text
Access-Control-Allow-Origin: https://app.example.com
Access-Control-Allow-Credentials: true
```

Cookie idea:

```text
Set-Cookie: session=abc; HttpOnly; Secure; SameSite=None; Path=/
```

Strong answer:

```text
For cross-origin cookie auth, the frontend credentials mode, server CORS headers, and cookie
attributes all need to align. I would inspect Set-Cookie and request Cookie headers in Network.
```

---

## 23. Scenario: Token Storage Security Review

Prompt:

```text
A frontend stores JWT access tokens in localStorage. Security review flags it.
```

Issue:

```text
localStorage is readable by JavaScript, so XSS can steal tokens.
```

Alternatives:

- HttpOnly Secure SameSite cookies.
- In-memory access token with refresh flow.
- Short-lived access tokens.
- Refresh token rotation.
- Backend-for-frontend session model.

Trade-off:

```text
HttpOnly cookies reduce JS token theft but require CSRF protection. localStorage avoids automatic
cookie sending but is vulnerable to XSS token exfiltration.
```

Strong answer:

```text
There is no universal perfect storage. I would discuss the threat model. If XSS token theft is
the concern, HttpOnly Secure cookies or memory-based access tokens reduce exposure, but cookie-based
flows need CSRF defenses and careful SameSite settings.
```

---

## 24. Scenario: XSS In Comment Rendering

Prompt:

```text
After adding rich comments, a security test injects HTML and JavaScript into the page.
```

Bad:

```javascript
commentElement.innerHTML = comment.body;
```

Safe text:

```javascript
commentElement.textContent = comment.body;
```

If limited HTML is required:

```text
Use a trusted sanitizer and allowlist tags/attributes. Do not write your own sanitizer casually.
```

Additional defenses:

- Content Security Policy.
- Avoid inline scripts.
- Escape output by context.
- Use framework defaults correctly.
- Review dangerous APIs.

Strong answer:

```text
The root cause is treating untrusted input as HTML. I would render as text by default or sanitize
with a trusted allowlist sanitizer, then add CSP and tests around dangerous rendering paths.
```

---

## 25. Scenario: Memory Leak After Navigation

Prompt:

```text
After navigating between pages repeatedly, memory grows and old handlers still run.
```

Likely causes:

- Event listeners not removed.
- Timers not cleared.
- Observers not disconnected.
- WebSocket not closed.
- Fetch results update unmounted UI.
- Detached DOM nodes still referenced.

Cleanup checklist:

```javascript
const controller = new AbortController();
const intervalId = setInterval(refresh, 1000);
const observer = new IntersectionObserver(handleEntries);
const socket = new WebSocket(url);

function cleanup() {
    controller.abort();
    clearInterval(intervalId);
    observer.disconnect();
    socket.close();
}
```

Debug evidence:

- Memory heap snapshots.
- Detached DOM nodes.
- Listener count growing.
- Network connections not closing.
- Logs from old screen after navigation.

Strong answer:

```text
I would look for resources created on mount but not cleaned on unmount: listeners, timers,
observers, sockets, and in-flight requests. Then I would verify with heap snapshots and repeated
navigation tests.
```

---

## 26. Scenario: Image Lazy Loading Still Slow

Prompt:

```text
The page uses lazy loading, but initial load is still slow and layout jumps.
```

Likely causes:

- Hero image lazy-loaded accidentally.
- Missing width/height causing layout shift.
- Images too large or wrong format.
- Too many images near viewport.
- No responsive `srcset`.
- JavaScript lazy loader blocks main thread.

Fixes:

```html
<img
    src="/room-800.webp"
    srcset="/room-400.webp 400w, /room-800.webp 800w"
    sizes="(max-width: 600px) 400px, 800px"
    width="800"
    height="500"
    alt="Hotel room"
    loading="lazy"
>
```

Hero image:

```html
<img src="/hero.webp" fetchpriority="high" alt="Hotel lobby">
```

Strong answer:

```text
Lazy loading is not enough. I would reserve image dimensions to avoid layout shift, use responsive
sizes, compress images, avoid lazy-loading the LCP hero, and verify with performance metrics.
```

---

## 27. Scenario: CLS From Dynamic Content

Prompt:

```text
Content shifts after ads/images/API widgets load.
```

Cause:

```text
The page does not reserve space before late content appears.
```

Fixes:

- Set image/video width and height.
- Reserve slots for ads/widgets.
- Use skeletons with stable dimensions.
- Avoid inserting banners above existing content after load.
- Prefer transform animations over layout-changing animations.

CSS example:

```css
.card-skeleton {
    min-height: 180px;
}

.image-frame {
    aspect-ratio: 16 / 9;
}
```

Strong answer:

```text
I would identify what element shifts using Performance or Layout Shift tooling, then reserve
stable space for late-loading content so the layout does not move unexpectedly.
```

---

## 28. Scenario: Button Click Has Wrong Item ID

Prompt:

```text
Every button in a list opens the last item instead of the clicked item.
```

Classic closure with `var`:

```javascript
for (var index = 0; index < items.length; index++) {
    button.addEventListener("click", () => {
        openItem(items[index]);
    });
}
```

After loop, `index` is final value.

Fix with `let`:

```javascript
for (let index = 0; index < items.length; index++) {
    button.addEventListener("click", () => {
        openItem(items[index]);
    });
}
```

Better with data attributes and delegation:

```javascript
list.addEventListener("click", event => {
    const button = event.target.closest("button[data-id]");

    if (button) {
        openItem(button.dataset.id);
    }
});
```

Strong answer:

```text
This is usually a closure bug from var loop scope or incorrect binding. I would use let for
block scope or use event delegation with data IDs.
```

---

## 29. Scenario: Dropdown Closes Before Item Click

Prompt:

```text
A dropdown closes on blur before the menu item click handler runs.
```

Cause:

```text
Focus/blur/mousedown/click ordering can close UI before click is processed.
```

Fix options:

- Handle selection on `mousedown` or `pointerdown` carefully.
- Delay close until after click.
- Manage focus within component.
- Use ARIA combobox/listbox patterns.

Example:

```javascript
menu.addEventListener("pointerdown", event => {
    const item = event.target.closest("[data-option]");

    if (item) {
        selectOption(item.dataset.option);
    }
});
```

Accessibility caution:

```text
Do not solve only mouse behavior. Keyboard and screen reader interactions also need correct focus
and roles.
```

Strong answer:

```text
I would inspect event order and focus behavior. Dropdowns are interaction components, so the fix
must support pointer, keyboard, focus management, and accessibility semantics.
```

---

## 30. Scenario: Infinite Scroll Loads Too Much

Prompt:

```text
Infinite scroll keeps firing and loads too many pages.
```

Likely causes:

- No loading guard.
- Observer keeps observing while request in flight.
- Sentinel remains visible after append.
- No hasMore check.
- Request failure immediately retriggers.

Fix:

```javascript
let isLoading = false;
let hasMore = true;
let page = 1;

const observer = new IntersectionObserver(async entries => {
    const entry = entries[0];

    if (!entry.isIntersecting || isLoading || !hasMore) {
        return;
    }

    isLoading = true;

    try {
        const result = await loadPage(page);
        renderItems(result.items);
        page++;
        hasMore = result.hasMore;
    } finally {
        isLoading = false;
    }
});
```

Strong answer:

```text
I would add loading and hasMore guards, handle errors, and ensure the observer does not trigger
unbounded parallel loads while the sentinel is visible.
```

---

## 31. Scenario: Optimistic UI Shows Wrong State

Prompt:

```text
A user clicks Like. UI updates immediately, but if the API fails, the UI remains liked incorrectly.
```

Optimistic update:

```javascript
async function likePost(postId) {
    const previous = getPost(postId);
    updatePost(postId, { liked: true });

    try {
        await apiLikePost(postId);
    } catch (error) {
        updatePost(postId, previous);
        showError("Could not like post");
    }
}
```

Production concerns:

- Rollback on failure.
- Disable duplicate actions or merge them.
- Idempotent backend operation.
- Reconcile with server response.
- Handle out-of-order responses.

Strong answer:

```text
Optimistic UI improves perceived speed but needs rollback or reconciliation. I would store the
previous state, handle failure, and make the backend operation idempotent where possible.
```

---

## 32. Scenario: Cache Shows Stale Data

Prompt:

```text
User updates profile, but the page still shows old profile data.
```

Likely causes:

- Client cache not invalidated.
- Browser HTTP cache.
- Service worker cache.
- State duplicated in multiple stores.
- Mutation response not merged.

Fix options:

- Invalidate relevant query/cache key.
- Update cache with mutation response.
- Add cache-control headers.
- Version service worker caches.
- Avoid duplicating source of truth.

Strong answer:

```text
I would identify which cache is stale: app state, data-fetching cache, HTTP cache, or service
worker cache. Then I would invalidate or update the right cache after mutation.
```

---

## 33. Scenario: Service Worker Serves Old App

Prompt:

```text
After deployment, some users still see old JavaScript and broken UI.
```

Likely cause:

```text
Service worker cache strategy or update lifecycle is serving stale assets.
```

Debug:

- Application panel -> Service Workers.
- Cache Storage contents.
- Asset filenames and hashes.
- Service worker update lifecycle.
- Network requests served from service worker.

Fixes:

- Cache versioning.
- Hash static assets.
- Clear old caches on activate.
- Avoid caching HTML with stale strategy unless intentional.
- Provide update prompt or skip-waiting strategy carefully.

Strong answer:

```text
Service workers are powerful but can trap users on stale assets. I would inspect Cache Storage
and SW lifecycle, version caches, clear old caches, and ensure HTML and hashed assets use correct
caching strategies.
```

---

## 34. Scenario: Accessibility Broken By Custom Button

Prompt:

```text
A custom div button works with mouse but not keyboard or screen reader.
```

Bad:

```html
<div onclick="save()">Save</div>
```

Better:

```html
<button type="button">Save</button>
```

If custom role is unavoidable:

```html
<div role="button" tabindex="0">Save</div>
```

Then you must handle keyboard activation:

```javascript
element.addEventListener("keydown", event => {
    if (event.key === "Enter" || event.key === " ") {
        event.preventDefault();
        save();
    }
});
```

Strong answer:

```text
I would use semantic HTML first. A real button already supports keyboard, focus, role, and default
accessibility behavior. Custom interactive elements require extra work and are easy to get wrong.
```

---

## 35. Scenario: Focus Lost After Modal Opens

Prompt:

```text
A modal opens but keyboard users remain focused behind it.
```

Expected behavior:

- Move focus into modal.
- Trap focus while modal is open.
- Restore focus to opener when closed.
- Close with Escape when appropriate.
- Hide/inert background content.

Native dialog idea:

```javascript
const opener = document.activeElement;
dialog.showModal();
dialog.querySelector("button")?.focus();

function closeDialog() {
    dialog.close();

    if (opener instanceof HTMLElement) {
        opener.focus();
    }
}
```

Strong answer:

```text
Modal behavior is not only visual. I would manage focus, keyboard escape, background interaction,
and focus restoration, preferably using native dialog or a well-tested accessible component.
```

---

## 36. Scenario: Hydration Mismatch

Prompt:

```text
A server-rendered page logs hydration mismatch warnings.
```

Likely causes:

- Server and client render different content.
- Using `Date.now()` or random values during render.
- Reading `window` during server render.
- Locale/timezone differences.
- User-specific data differs before hydration.

Bad:

```javascript
function Component() {
    return `<div>${Date.now()}</div>`;
}
```

Fix patterns:

- Render deterministic server output.
- Move client-only data to effect after hydration.
- Pass same data from server to client.
- Avoid random IDs unless framework provides stable ID helpers.

Strong answer:

```text
Hydration mismatches happen when server HTML does not match the initial client render. I would
remove nondeterministic render output and move client-only reads into client lifecycle code.
```

---

## 37. Scenario: Third-Party Script Slows Page

Prompt:

```text
After adding analytics/chat widget, LCP and input responsiveness get worse.
```

Likely causes:

- Blocking script loading.
- Heavy main-thread execution.
- Layout shifts from injected widgets.
- Long tasks from third-party code.
- Network contention.

Mitigation:

```html
<script src="/analytics.js" defer></script>
```

Strategies:

- Load asynchronously/deferred.
- Delay non-critical third-party scripts.
- Use consent-based loading.
- Measure long tasks and transfer size.
- Set performance budgets.
- Sandbox iframes where possible.
- Remove unused vendors.

Strong answer:

```text
Third-party scripts are production dependencies. I would measure their network and main-thread
cost, defer non-critical loading, isolate where possible, and set performance budgets.
```

---

## 38. Scenario: Responsive UI Breaks On Mobile

Prompt:

```text
A form works on desktop but buttons overflow and input is hidden on mobile.
```

Debug:

- Test real viewport sizes.
- Inspect layout boxes.
- Check fixed widths.
- Check long text and localization.
- Check virtual keyboard behavior.
- Check touch target sizes.

CSS fixes:

```css
.form-row {
    display: flex;
    flex-wrap: wrap;
    gap: 8px;
}

.form-row > * {
    min-width: 0;
}

button {
    min-height: 44px;
}
```

Strong answer:

```text
I would inspect the actual layout constraints on mobile, remove fixed-width assumptions, allow
wrapping, preserve readable touch targets, and test with long content and keyboard open states.
```

---

## 39. Scenario: Timezone Bug In Date Input

Prompt:

```text
A selected date shows as previous day for some users.
```

Cause:

```text
Date-only values are mixed with UTC/local timezone conversions.
```

Bad:

```javascript
const date = new Date("2026-06-20");
```

Depending timezone, display can shift.

Safer date-only handling:

```javascript
const selectedDate = "2026-06-20";
```

Use date-only string for date-only domain concepts.

Strong answer:

```text
For date-only values like birthdays or booking dates, I avoid converting to Date unless time zone
semantics are explicit. I keep date-only strings or use a date library/model that represents
calendar dates correctly.
```

---

## 40. Scenario: Floating Point Price Bug

Prompt:

```text
Cart total shows 0.30000000000000004.
```

Cause:

```text
JavaScript numbers are floating-point. Decimal money math can produce precision artifacts.
```

Bad:

```javascript
const total = 0.1 + 0.2;
```

Better:

```javascript
const totalCents = 10 + 20;
const display = `$${(totalCents / 100).toFixed(2)}`;
```

Strong answer:

```text
For money, I use integer minor units like cents or a decimal library, not floating-point dollars
for business-critical calculations.
```

---

## 41. Scenario: Client-Side Permission Check Bypass

Prompt:

```text
A button is hidden for non-admins, but a user still calls the admin API manually.
```

Root issue:

```text
Client-side checks are UX hints, not security boundaries.
```

Frontend:

```javascript
if (user.role === "admin") {
    showAdminButton();
}
```

Backend must enforce:

```text
Every protected API must check authentication and authorization server-side.
```

Strong answer:

```text
The frontend can hide or disable admin UI for usability, but the backend must enforce permissions.
Anything in the browser can be modified by the user.
```

---

## 42. Scenario: File Upload Fails For Large Files

Prompt:

```text
Small uploads work, large uploads fail or freeze the UI.
```

Likely causes:

- Reading entire file into memory.
- Server size limits.
- Timeout too short.
- No progress UI.
- No chunking/resumable strategy.
- Main-thread parsing.

Better approach:

```javascript
const formData = new FormData();
formData.append("file", file);

await fetch("/api/upload", {
    method: "POST",
    body: formData
});
```

For very large files:

- Chunk upload.
- Resume support.
- Progress reporting.
- Server-side size validation.
- Virus scanning workflow.
- Direct-to-object-storage upload.

Strong answer:

```text
I would avoid loading the full file into JS memory unnecessarily, check server and proxy limits,
provide progress and cancellation, and use chunked or direct uploads for very large files.
```

---

## 43. Scenario: Autocomplete Keyboard Navigation Broken

Prompt:

```text
Autocomplete works with mouse, but keyboard users cannot select suggestions.
```

Expected behavior:

- ArrowDown/ArrowUp move active option.
- Enter selects.
- Escape closes.
- Input keeps focus or manages focus correctly.
- Screen reader gets active descendant updates.

Simplified key handling:

```javascript
input.addEventListener("keydown", event => {
    if (event.key === "ArrowDown") {
        event.preventDefault();
        moveActive(1);
    }

    if (event.key === "ArrowUp") {
        event.preventDefault();
        moveActive(-1);
    }

    if (event.key === "Enter") {
        event.preventDefault();
        selectActive();
    }

    if (event.key === "Escape") {
        closeMenu();
    }
});
```

Strong answer:

```text
Autocomplete is not just filtering. It needs keyboard behavior, focus semantics, ARIA roles or a
well-tested component pattern, async race handling, and accessible announcements.
```

---

## 44. Scenario: Production Error Only In Minified Build

Prompt:

```text
Bug appears only in production build, not local development.
```

Possible causes:

- Environment variable differences.
- Minification/tree-shaking side effects.
- Dead-code elimination.
- Source maps missing.
- Race condition timing changes.
- API endpoint/config mismatch.
- Strict CSP in production.

Debug approach:

- Reproduce with production build locally.
- Enable source maps safely.
- Compare environment config.
- Check console/network errors.
- Bisect recent deployment.
- Review build warnings.

Strong answer:

```text
I would reproduce using the production build, inspect source-mapped stack traces, compare runtime
configuration, and check whether bundling/minification changed module side effects or timing.
```

---

## 45. Scenario: Feature Flag Causes Broken State

Prompt:

```text
A feature works when flag is on from page load, but breaks when flag changes during session.
```

Likely causes:

- Initialization only runs once.
- State shape differs between flag modes.
- Cleanup not performed when flag turns off.
- API contract differs but caller assumes one shape.

Fix:

- Treat flag as dynamic input if it can change live.
- Add migration/cleanup between modes.
- Use compatible state shape.
- Test both transitions: off -> on and on -> off.

Strong answer:

```text
Feature flags are runtime inputs. If a flag can change during a session, the UI must handle both
transition directions, cleanup old resources, and avoid incompatible state assumptions.
```

---

## 46. Scenario: Error Boundary Does Not Catch Error

Prompt:

```text
A React error boundary catches render errors but not async request failures.
```

Reason:

```text
Error boundaries catch render/lifecycle errors, not arbitrary async promise rejections or event
handler errors unless those are moved into state/render flow.
```

Async handling:

```javascript
async function load() {
    try {
        const data = await fetchData();
        setData(data);
    } catch (error) {
        setError(error);
    }
}
```

Strong answer:

```text
I would not rely on error boundaries for every async failure. Async operations need explicit
catch handling, error state, logging, and user-facing recovery.
```

---

## 47. Scenario: Unhandled Promise Rejection

Prompt:

```text
Console shows unhandled promise rejection after clicking Save.
```

Bad:

```javascript
button.addEventListener("click", () => {
    saveBooking();
});
```

If `saveBooking` rejects, no one handles it.

Fix:

```javascript
button.addEventListener("click", () => {
    void saveBooking().catch(error => {
        showError(error);
        reportError(error);
    });
});
```

Or inside async handler:

```javascript
button.addEventListener("click", async () => {
    try {
        await saveBooking();
    } catch (error) {
        showError(error);
    }
});
```

Strong answer:

```text
Every async action needs an ownership and error-handling path. Fire-and-forget promises should
still attach catch and observability.
```

---

## 48. Scenario: Browser Back Button Breaks Filters

Prompt:

```text
Filters update the list, but browser back does not restore previous filter state.
```

Cause:

```text
Filter state exists only in memory, not in URL/history.
```

Fix with URLSearchParams:

```javascript
function updateFilters(filters) {
    const params = new URLSearchParams(filters);
    history.pushState(filters, "", `?${params}`);
    renderWithFilters(filters);
}

window.addEventListener("popstate", () => {
    const params = new URLSearchParams(location.search);
    renderWithFilters(Object.fromEntries(params));
});
```

Strong answer:

```text
If filter state should be shareable and back-button aware, I would store it in the URL and handle
popstate, or let the router manage URL-synchronized state.
```

---

## 49. Scenario: Network Retry Duplicates Payment

Prompt:

```text
Payment request times out, frontend retries, and user is charged twice.
```

Root cause:

```text
Retrying a non-idempotent operation without idempotency protection.
```

Fix:

```javascript
const idempotencyKey = crypto.randomUUID();

await fetch("/api/payments", {
    method: "POST",
    headers: {
        "Content-Type": "application/json",
        "Idempotency-Key": idempotencyKey
    },
    body: JSON.stringify(payment)
});
```

Strong answer:

```text
For non-idempotent writes like payments, timeout does not prove failure. I would use server-side
idempotency keys and avoid blind retries that can duplicate side effects.
```

---

## 50. Scenario: API Storm From Promise.all

Prompt:

```text
A dashboard loads 500 widgets and sends hundreds of API calls at once.
```

Bad:

```javascript
await Promise.all(widgets.map(widget => loadWidget(widget.id)));
```

Fix with concurrency limit:

```javascript
async function mapWithConcurrency(items, limit, mapper) {
    const results = new Array(items.length);
    let nextIndex = 0;

    async function worker() {
        while (nextIndex < items.length) {
            const currentIndex = nextIndex++;
            results[currentIndex] = await mapper(items[currentIndex], currentIndex);
        }
    }

    const workers = Array.from(
        { length: Math.min(limit, items.length) },
        () => worker()
    );

    await Promise.all(workers);
    return results;
}
```

Strong answer:

```text
Promise.all is fine for small bounded groups, but large fan-out needs concurrency limits,
batching, caching, request aggregation, or backend endpoints designed for the dashboard.
```

---

## 51. Scenario: User Sees Blank Screen

Prompt:

```text
Some users report a blank page after deployment.
```

Debug path:

1. Check console errors.
2. Check network failures for JS/CSS chunks.
3. Check source map stack traces.
4. Check browser version compatibility.
5. Check CSP violations.
6. Check service worker stale cache.
7. Check feature flags/config.
8. Check monitoring session replay/logs if available.

Common causes:

- JS chunk 404 after deploy.
- Unsupported syntax in older browser.
- Runtime config missing.
- Uncaught render error.
- Service worker serving mixed old/new assets.
- CSP blocks script.

Strong answer:

```text
A blank screen usually means critical render JavaScript failed. I would inspect console and
network first, then source-mapped stack traces, browser compatibility, CSP, and service worker
cache behavior.
```

---

## 52. Scenario: Missing Loading And Error States

Prompt:

```text
Users click a button and nothing appears to happen on slow networks.
```

Problem:

```text
The UI does not represent async state.
```

State model:

```javascript
const state = {
    status: "idle",
    data: null,
    error: null
};
```

Better state machine idea:

```text
idle -> loading -> success
idle -> loading -> error
success -> refreshing -> success/error
```

Plain JS example:

```javascript
async function loadData() {
    setStatus("loading");

    try {
        const data = await fetchJson("/api/data");
        renderData(data);
        setStatus("success");
    } catch (error) {
        renderError(error);
        setStatus("error");
    }
}
```

Strong answer:

```text
Async UI should show loading, success, empty, and error states. Without visible state, slow
networks look broken even when code is working.
```

---

## 53. Scenario: Empty State Treated As Error

Prompt:

```text
When search returns zero results, UI shows a failure message.
```

Cause:

```text
Code treats empty arrays as failure or does not model empty state separately.
```

Bad:

```javascript
if (!results.length) {
    showError("Failed to load results");
}
```

Better:

```javascript
if (results.length === 0) {
    showEmpty("No results found");
} else {
    renderResults(results);
}
```

Strong answer:

```text
Empty is a successful state, not an error. I model empty, error, loading, and success separately
so the UI gives accurate feedback.
```

---

## 54. Scenario: Partial Failure Dashboard

Prompt:

```text
A dashboard has five widgets. If one widget API fails, the whole dashboard fails.
```

Bad:

```javascript
const [profile, stats, alerts] = await Promise.all([
    loadProfile(),
    loadStats(),
    loadAlerts()
]);
```

If alerts fails, all rejects.

Better:

```javascript
const results = await Promise.allSettled([
    loadProfile(),
    loadStats(),
    loadAlerts()
]);
```

Render per widget:

```javascript
for (const result of results) {
    if (result.status === "fulfilled") {
        renderWidget(result.value);
    } else {
        renderWidgetError(result.reason);
    }
}
```

Strong answer:

```text
If widgets are independent, I would isolate failures with Promise.allSettled or per-widget error
boundaries so one optional failure does not take down the whole dashboard.
```

---

## 55. Scenario: Data Shape Changes Break UI

Prompt:

```text
Backend changes `guest_name` to `guestName`, and frontend silently shows blank names.
```

Causes:

- Weak API contract.
- No runtime validation.
- No TypeScript generated types or contract tests.
- UI tolerates missing fields silently.

Fixes:

- API schema validation.
- Contract tests.
- Generated types from OpenAPI/GraphQL.
- Runtime boundary mapper.
- Observability for invalid payloads.

Boundary mapper:

```javascript
function toBooking(value) {
    if (typeof value.guestName !== "string") {
        throw new Error("invalid booking guestName");
    }

    return {
        id: String(value.id),
        guestName: value.guestName
    };
}
```

Strong answer:

```text
Frontend and backend need an explicit contract. I would validate or map API responses at the
boundary and add contract tests or generated types so shape changes fail early.
```

---

## 56. Scenario: Duplicate Keys Cause Wrong Rows

Prompt:

```text
After sorting or filtering, row UI state appears on the wrong item.
```

Likely cause in frameworks:

```text
Using array index as key for dynamic lists.
```

Bad:

```javascript
items.map((item, index) => <Row key={index} item={item} />)
```

Better:

```javascript
items.map(item => <Row key={item.id} item={item} />)
```

Strong answer:

```text
Keys should represent stable item identity. Index keys break when lists reorder, insert, or delete,
causing component state to attach to the wrong item.
```

---

## 57. Scenario: Internationalization Breaks Layout

Prompt:

```text
UI looks fine in English but breaks in German or Hindi.
```

Causes:

- Fixed-width containers.
- Text cannot wrap.
- Hardcoded string concatenation.
- Directionality issues.
- Date/number formatting assumptions.

Fixes:

- Allow wrapping.
- Avoid fixed heights for text-heavy areas.
- Use ICU/message formatting.
- Test long translations.
- Use Intl APIs.

Example:

```javascript
const formatter = new Intl.NumberFormat("de-DE", {
    style: "currency",
    currency: "EUR"
});

formatter.format(1234.56);
```

Strong answer:

```text
I would design layouts for text expansion, use proper i18n formatting, avoid concatenating
translated fragments, and test with long strings and different writing systems.
```

---

## 58. Scenario: Browser Compatibility Failure

Prompt:

```text
App works in Chrome latest but fails in an older enterprise browser.
```

Possible causes:

- Unsupported syntax not transpiled.
- Missing polyfill for runtime API.
- New Web API not available.
- CSS feature unsupported.
- Mobile WebView differences.

Example:

```javascript
items.toSorted(compareItems);
```

Fallback:

```javascript
const sorted = [...items].sort(compareItems);
```

Debug:

- Check console syntax/API errors.
- Check target browsers list.
- Check Babel/TypeScript output.
- Check polyfills.
- Check caniuse/MDN.

Strong answer:

```text
I separate syntax support from API support. Syntax may need transpilation, while APIs may need
polyfills or fallbacks. I would check the supported browser matrix and build targets.
```

---

## 59. Mini Program: Latest-Only Debounced Search

```javascript
function createSearchController({ input, results, endpoint }) {
    let controller;
    let latestRequestId = 0;

    input.addEventListener("input", debounce(() => {
        void search(input.value).catch(error => {
            if (error.name !== "AbortError") {
                results.textContent = "Search failed";
            }
        });
    }, 300));

    async function search(query) {
        const normalized = query.trim();
        const requestId = ++latestRequestId;

        controller?.abort();

        if (normalized.length < 2) {
            results.replaceChildren();
            return;
        }

        controller = new AbortController();

        const params = new URLSearchParams({ q: normalized });
        const response = await fetch(`${endpoint}?${params}`, {
            signal: controller.signal
        });

        if (!response.ok) {
            throw new Error(`HTTP ${response.status}`);
        }

        const items = await response.json();

        if (requestId !== latestRequestId) {
            return;
        }

        renderItems(items);
    }

    function renderItems(items) {
        const fragment = document.createDocumentFragment();

        for (const item of items) {
            const element = document.createElement("li");
            element.textContent = item.label;
            fragment.append(element);
        }

        results.replaceChildren(fragment);
    }
}

function debounce(fn, delayMs) {
    let timeoutId;

    return function debounced(...args) {
        clearTimeout(timeoutId);
        timeoutId = setTimeout(() => fn.apply(this, args), delayMs);
    };
}
```

Why this is interview-strong:

- Debounces noisy input.
- Cancels previous request.
- Guards stale responses with request ID.
- Checks HTTP status.
- Uses URLSearchParams.
- Uses textContent safely.
- Handles async errors.

---

## 60. Mini Program: Safe Submit Button

```javascript
function createSafeSubmitHandler({ form, button, submit }) {
    let isSubmitting = false;

    form.addEventListener("submit", async event => {
        event.preventDefault();

        if (isSubmitting) {
            return;
        }

        isSubmitting = true;
        button.disabled = true;
        button.setAttribute("aria-busy", "true");

        const idempotencyKey = crypto.randomUUID();

        try {
            const formData = new FormData(form);
            await submit(formData, { idempotencyKey });
            form.reset();
            showMessage("Saved");
        } catch (error) {
            showMessage("Save failed");
            reportError(error);
        } finally {
            isSubmitting = false;
            button.disabled = false;
            button.removeAttribute("aria-busy");
        }
    });
}
```

Why strong:

- Handles submit event, not only click.
- Prevents duplicate client submit.
- Includes idempotency key for backend protection.
- Shows user feedback.
- Restores UI in finally.
- Reports errors.

---

## 61. Mini Program: Virtual List Shape

Virtualization idea:

```javascript
function getVisibleRange({ scrollTop, rowHeight, viewportHeight, totalCount, overscan = 5 }) {
    const start = Math.max(0, Math.floor(scrollTop / rowHeight) - overscan);
    const visibleCount = Math.ceil(viewportHeight / rowHeight);
    const end = Math.min(totalCount, start + visibleCount + overscan * 2);

    return { start, end };
}
```

Render idea:

```javascript
function renderVirtualRows({ container, items, rowHeight, scrollTop, viewportHeight }) {
    const { start, end } = getVisibleRange({
        scrollTop,
        rowHeight,
        viewportHeight,
        totalCount: items.length
    });

    const fragment = document.createDocumentFragment();

    for (let index = start; index < end; index++) {
        const row = document.createElement("div");
        row.style.position = "absolute";
        row.style.top = `${index * rowHeight}px`;
        row.style.height = `${rowHeight}px`;
        row.textContent = items[index].label;
        fragment.append(row);
    }

    container.replaceChildren(fragment);
}
```

Interview note:

```text
Real virtualization needs accessibility, dynamic height handling, keyboard navigation, and scroll
container details. The key concept is rendering only visible rows plus overscan.
```

---

## 62. Common Mistakes

| Mistake | Why It Hurts | Better Approach |
|---|---|---|
| Updating UI from old async response | Stale data | Abort or request ID guard |
| API call on every keystroke | Rate limits and lag | Debounce and minimum query length |
| Button click only for form submit | Keyboard path broken | Use form submit event |
| Client-only duplicate prevention | Can be bypassed | Add server idempotency |
| Storing derived state | Gets out of sync | Derive from source state |
| Mutating state in place | UI may not update | Immutable updates |
| Huge DOM list | Slow render/scroll | Virtualize or paginate |
| Heavy scroll handler | Jank | Passive, throttle, CSS, observers |
| Promise.all for massive fan-out | API storm | Concurrency limit/batching |
| Blind retry of writes | Duplicate side effects | Idempotency keys |
| localStorage token by default | XSS theft risk | Threat-model storage choice |
| Treating empty as error | Bad UX | Separate empty and error states |
| No cleanup on unmount | Memory leaks | Remove listeners, abort, disconnect |
| Custom div button | Accessibility broken | Use semantic button |
| CORS fix attempted in frontend | Cannot bypass browser | Fix server/gateway headers |
| async/await for CPU work | Still blocks main thread | Worker/chunk/optimize |

---

## 63. Strong Interview Answers

### How do you debug stale search results?

```text
I treat it as an async race. I reproduce with throttled network, inspect overlapping requests in
Network, then fix by debouncing input and ensuring only the latest request can update UI using
AbortController or request IDs.
```

### How do you prevent double submits?

```text
I disable or guard the submit path while a request is in flight, but I also require backend
idempotency for real protection. Client-side prevention improves UX; server-side idempotency
protects the business operation.
```

### How do you optimize a slow table?

```text
I first reduce how much DOM is rendered using virtualization or pagination. Then I remove
per-row handlers with event delegation, profile layout and scripting cost, and batch DOM updates.
```

### How do you handle frontend async errors?

```text
Every async action should have a clear owner and error path. I handle loading, success, empty,
and error states, catch promise rejections, report unexpected failures, and avoid silent
fire-and-forget work.
```

### How do you debug CORS?

```text
I use the browser Network panel, not Postman, because CORS is browser-enforced. I inspect the
OPTIONS preflight, Origin, allowed methods/headers, credentials mode, and server CORS response headers.
```

### How do you approach frontend performance?

```text
I measure first. Then I separate network cost, JavaScript execution, rendering/layout cost, image
weight, and main-thread long tasks. Fixes include code splitting, virtualization, batching,
workers, image optimization, and better caching.
```

---

## 64. FAANG Scenario 1: Search Box System

> Design and debug a search box for a large e-commerce site. It should feel fast, avoid stale results, and not overload the API.

Strong answer:

```text
I would debounce input, require a minimum query length, cancel stale requests, and guard results
with request IDs so older responses cannot overwrite newer intent. I would use URLSearchParams
for query building and check response.ok for HTTP errors.

For UX, I would show loading, empty, and error states, keep keyboard navigation accessible, and
avoid clearing useful results too aggressively during refresh. For production, I would add short
client caching or request deduplication for repeated queries, respect 429 responses, and make
backend search support pagination and relevance limits.
```

---

## 65. FAANG Scenario 2: Slow Dashboard

> A dashboard has many widgets, slow loading, partial failures, and browser jank.

Strong answer:

```text
I would split the problem into network, rendering, and failure isolation. For network, I would
avoid unbounded Promise.all fan-out, batch or limit concurrency, and cache shared data. For partial
failures, independent widgets should not take down the whole dashboard, so I would use per-widget
error states or Promise.allSettled.

For jank, I would profile the main thread, reduce DOM size, virtualize large lists, defer
non-critical widgets, and avoid layout thrashing. I would add observability for widget latency,
error rate, render time, and long tasks.
```

---

## 66. FAANG Scenario 3: Secure Auth Frontend

> A frontend app needs secure login and API calls. Discuss token storage and browser risks.

Strong answer:

```text
I would start with the threat model. localStorage is easy but vulnerable to XSS token theft.
HttpOnly Secure SameSite cookies reduce JavaScript access to tokens but require CSRF defenses.
Memory-only access tokens reduce persistence but need refresh and reload handling.

I would combine secure storage choices with XSS prevention, CSP, short-lived tokens, refresh
rotation, CSRF tokens or SameSite protections, strict CORS, and server-side authorization on every
protected API. The frontend can improve UX, but it is not a security boundary.
```

---

## 67. FAANG Scenario 4: Production Blank Screen

> After deployment, 5 percent of users see a blank screen. How do you investigate?

Strong answer:

```text
I would first check production monitoring for JavaScript errors, affected browser versions, routes,
and release versions. Then I would inspect console and network failures using session replay or
reproduction. Common causes include missing JS chunks, service worker stale cache, unsupported
syntax, CSP blocking scripts, runtime config missing, or an uncaught render error.

I would verify source maps, compare build assets, inspect service worker/cache behavior, and roll
back or patch quickly if the issue is release-related. Then I would add a safer deployment strategy,
chunk error handling, and better client-side error reporting.
```

---

## 68. Rapid Revision

- Frontend scenario answers should include symptom, cause, proof, fix, and prevention.
- Use DevTools evidence instead of guessing.
- Stale search results are async race conditions.
- Debounce reduces noisy requests.
- AbortController cancels supported requests.
- Request IDs prevent old responses from updating UI.
- Form submit is better than button click for submissions.
- preventDefault stops native submit navigation.
- Double submit needs client guard and server idempotency.
- Stale closures happen when async callbacks capture old values.
- Derived state can get out of sync.
- Mutating state can prevent UI updates in reference-based frameworks.
- Event delegation handles dynamic lists with fewer listeners.
- target is event origin; currentTarget is listener owner.
- Large lists need virtualization or pagination.
- Scroll handlers should be passive/throttled when appropriate.
- Layout thrashing comes from repeated DOM write/read cycles.
- async/await does not move CPU work off the main thread.
- fetch does not reject on HTTP 4xx/5xx.
- CORS works in Postman is irrelevant to browser enforcement.
- Cookie auth cross-origin needs credentials and server CORS alignment.
- localStorage tokens are vulnerable to XSS theft.
- XSS comes from treating untrusted input as executable markup.
- Memory leaks often come from missing cleanup of listeners, timers, observers, sockets, or requests.
- CLS usually means late content lacks reserved space.
- Optimistic UI needs rollback or reconciliation.
- Stale cache can be app cache, HTTP cache, or service worker cache.
- Semantic HTML prevents many accessibility bugs.
- Hydration mismatch means server and client initial render differ.
- Third-party scripts can hurt network, main thread, and layout.
- Date-only values need explicit timezone strategy.
- Money should use integer minor units or decimal-safe handling.
- Client-side permissions are not security boundaries.
- Promise.all fan-out can create API storms.
- Empty state is not error state.
- Partial dashboards should isolate widget failures.
- Stable keys must represent item identity.
- Browser compatibility requires syntax and API support checks.

---

## 69. Official Source Notes

Use these sources when refreshing frontend scenario knowledge:

- MDN Event loop: `https://developer.mozilla.org/en-US/docs/Web/JavaScript/Event_loop`
- MDN Fetch API: `https://developer.mozilla.org/en-US/docs/Web/API/Fetch_API`
- MDN AbortController: `https://developer.mozilla.org/en-US/docs/Web/API/AbortController`
- MDN CORS: `https://developer.mozilla.org/en-US/docs/Web/HTTP/Guides/CORS`
- MDN Forms: `https://developer.mozilla.org/en-US/docs/Learn_web_development/Extensions/Forms`
- MDN DOM events: `https://developer.mozilla.org/en-US/docs/Learn_web_development/Core/Scripting/Events`
- MDN Web Storage API: `https://developer.mozilla.org/en-US/docs/Web/API/Web_Storage_API`
- MDN Service Worker API: `https://developer.mozilla.org/en-US/docs/Web/API/Service_Worker_API`
- web.dev rendering performance: `https://web.dev/articles/rendering-performance`
- web.dev optimize long tasks: `https://web.dev/articles/optimize-long-tasks`
- web.dev Cumulative Layout Shift: `https://web.dev/articles/cls`
- web.dev Interaction to Next Paint: `https://web.dev/articles/inp`
- OWASP XSS: `https://owasp.org/www-community/attacks/xss/`
- OWASP CSRF: `https://owasp.org/www-community/attacks/csrf`
- WAI ARIA Authoring Practices: `https://www.w3.org/WAI/ARIA/apg/`
- React docs on state: `https://react.dev/learn/state-a-components-memory`
- React docs on effects: `https://react.dev/learn/synchronizing-with-effects`

Interview safety line:

```text
For frontend interviews, I connect JavaScript behavior to user-visible symptoms: stale data,
slow input, broken forms, failed network calls, insecure storage, layout shifts, accessibility
breaks, and production debugging evidence.
```
