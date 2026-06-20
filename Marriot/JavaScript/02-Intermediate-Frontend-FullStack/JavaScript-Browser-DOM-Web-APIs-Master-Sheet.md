# JavaScript Browser DOM And Web APIs Master Sheet

Target: JavaScript interviews where DOM manipulation, events, browser APIs, rendering, fetch, CORS, storage, security, performance, and frontend production behavior are tested.

This sheet covers:
- Browser runtime mental model
- DOM, BOM, CSSOM, render tree
- Selecting, creating, updating, and removing elements
- Attributes, properties, dataset, classes, styles
- Events, bubbling, capturing, delegation, default behavior
- Forms and input handling
- Timers, animation, rendering frames
- Fetch API, HTTP basics, CORS, credentials, errors
- Browser storage: cookies, localStorage, sessionStorage, IndexedDB awareness
- History, URL, location, navigation
- IntersectionObserver, ResizeObserver, MutationObserver
- Web Workers and Service Workers awareness
- Security: XSS, CSRF, storage risks, trusted boundaries
- Performance: layout thrashing, long tasks, reflow/repaint, virtualization awareness
- Accessibility and progressive enhancement basics
- Common output/trap questions
- Mini programs and FAANG-level scenarios

How to use this:
- Learn the browser as a runtime, not just a place where JavaScript runs.
- Connect DOM operations to rendering cost.
- Connect events to propagation and default browser behavior.
- Connect fetch/storage to security and production constraints.
- In interviews, explain what the browser does step by step.

---

## 1. Mental Model

Browser JavaScript runs inside a host environment that provides Web APIs.

```text
JavaScript language -> syntax, objects, promises, functions
Browser runtime     -> DOM, events, fetch, storage, timers, rendering, navigation
```

The browser has several major models:

```text
DOM    -> HTML document as objects
CSSOM  -> CSS rules as objects
Render tree -> what needs to be painted
Event system -> how user/browser events reach handlers
Network stack -> fetch, caching, CORS, credentials
Storage -> cookies, localStorage, sessionStorage, IndexedDB
Security model -> origins, permissions, sandboxing
```

Strong interview line:

```text
Frontend JavaScript is not only language behavior. It is JavaScript interacting with browser
runtime APIs like DOM, events, fetch, storage, rendering, security boundaries, and the event loop.
```

---

## 2. Interview Priority Meter

| Topic | Priority | Why It Matters |
|---|---:|---|
| DOM tree | Very high | Foundation of page manipulation |
| Query selectors | Very high | Daily DOM access |
| DOM updates | Very high | UI behavior and performance |
| Event bubbling/capturing | Very high | Common interview topic |
| Event delegation | Very high | Scalable event handling |
| `preventDefault` / `stopPropagation` | Very high | Browser behavior control |
| Forms | High | Real frontend workflows |
| Fetch API | Very high | Network basics |
| CORS | Very high | Production frontend/backend integration |
| Cookies vs localStorage | Very high | Auth/security discussions |
| Rendering pipeline | Very high | Performance maturity |
| Reflow/repaint | High | DOM performance |
| Layout thrashing | High | Senior frontend signal |
| Debounce/throttle | High | Event performance |
| IntersectionObserver | Medium-high | Lazy loading and visibility |
| MutationObserver | Medium | DOM change detection |
| Web Workers | Medium-high | CPU-heavy work escape hatch |
| Service Workers | Medium | Offline/cache/PWA awareness |
| XSS | Very high | Frontend security baseline |
| CSRF | High | Cookie-based auth security |
| Accessibility | High | Production UI quality |
| Browser compatibility | High | Real delivery concern |

---

## 3. Browser Runtime Components

Browser runtime includes:

```text
JavaScript engine
DOM implementation
CSS parser and CSSOM
Layout engine
Rendering/painting/compositing engine
Networking stack
Storage systems
Event loop
Security model
Web APIs
```

The browser parses HTML into DOM:

```html
<body>
    <button id="save">Save</button>
</body>
```

DOM representation idea:

```text
Document
  html
    body
      button#save
```

Important:

```text
The DOM is not the HTML string. It is an in-memory object tree created from the HTML.
```

Strong answer:

```text
The browser parses HTML into the DOM, CSS into the CSSOM, combines them into a render tree, then
performs layout and paint. JavaScript can read and mutate the DOM, which may trigger rendering work.
```

---

## 4. DOM vs BOM

DOM:

```text
Document Object Model: object representation of the HTML/XML document.
```

Examples:

```javascript
document.querySelector("button");
document.createElement("div");
element.textContent = "Hello";
```

BOM:

```text
Browser Object Model: browser/window-related APIs outside the document.
```

Examples:

```javascript
window.location.href;
window.history.pushState({}, "", "/bookings");
window.navigator.userAgent;
window.setTimeout(() => {}, 1000);
```

Interview line:

```text
The DOM represents the page document. The BOM represents browser-level objects like window,
location, history, navigator, and timers.
```

---

## 5. Selecting Elements

Common selectors:

```javascript
const saveButton = document.getElementById("save");
const firstCard = document.querySelector(".card");
const allCards = document.querySelectorAll(".card");
```

`querySelector` returns first match or `null`.

```javascript
const button = document.querySelector("button");

if (button) {
    button.textContent = "Save";
}
```

`querySelectorAll` returns a static NodeList.

```javascript
const buttons = document.querySelectorAll("button");

buttons.forEach(button => {
    button.disabled = true;
});
```

Static vs live awareness:

```text
querySelectorAll returns a static NodeList.
getElementsByClassName and getElementsByTagName return live HTMLCollections.
```

Example live collection trap:

```javascript
const items = document.getElementsByClassName("item");
```

If DOM changes, `items` updates automatically.

Strong answer:

```text
I use querySelector/querySelectorAll for flexible CSS-style selection. I remember querySelector
can return null and querySelectorAll returns a static NodeList.
```

---

## 6. Creating And Appending Elements

Create element:

```javascript
const item = document.createElement("li");
item.textContent = "Booking created";
```

Append:

```javascript
const list = document.querySelector("#events");
list?.append(item);
```

Append multiple:

```javascript
list?.append(item1, item2, "plain text");
```

Use DocumentFragment for batch insert:

```javascript
const fragment = document.createDocumentFragment();

for (const booking of bookings) {
    const item = document.createElement("li");
    item.textContent = booking.id;
    fragment.append(item);
}

list.append(fragment);
```

Why:

```text
Batching DOM insertions can reduce repeated DOM work and make intent clearer.
```

---

## 7. Updating Text Safely

Use `textContent` for text.

```javascript
element.textContent = userInput;
```

Avoid unsafe `innerHTML` for untrusted content.

```javascript
element.innerHTML = userInput; // XSS risk if userInput contains HTML/script vectors
```

Example:

```javascript
const comment = "<img src=x onerror=alert(1)>";

safeElement.textContent = comment;
unsafeElement.innerHTML = comment;
```

Strong answer:

```text
For untrusted user content, I use textContent because it treats input as text. innerHTML parses
HTML and can introduce XSS if content is not sanitized.
```

---

## 8. Attributes vs Properties

Attribute:

```html
<input value="initial">
```

Property:

```javascript
const input = document.querySelector("input");
console.log(input.value);
```

Attributes are HTML-level values. Properties are DOM object values.

```javascript
input.setAttribute("value", "from attribute");
input.value = "from property";
```

Common rule:

```text
Use DOM properties for current interactive state. Use attributes for markup/configuration-like values.
```

Boolean attribute example:

```javascript
button.disabled = true;
button.setAttribute("disabled", "");
button.removeAttribute("disabled");
```

Interview line:

```text
Attributes come from markup, while properties live on DOM objects and often represent current
state. For form values, I usually read and write properties like input.value.
```

---

## 9. Dataset

HTML:

```html
<button data-booking-id="B1" data-action="cancel">Cancel</button>
```

JavaScript:

```javascript
const button = document.querySelector("button");

console.log(button?.dataset.bookingId); // B1
console.log(button?.dataset.action);    // cancel
```

Use cases:

- Store small element metadata.
- Event delegation action routing.
- IDs for client-side handlers.

Caution:

```text
data-* attributes are visible and editable by users through DevTools. Do not store secrets or
trusted authorization data in the DOM.
```

---

## 10. Classes And Styles

Class manipulation:

```javascript
element.classList.add("active");
element.classList.remove("hidden");
element.classList.toggle("selected");
element.classList.contains("active");
```

Inline style:

```javascript
element.style.backgroundColor = "red";
```

Prefer classes for most styling:

```javascript
element.classList.add("is-loading");
```

Why:

```text
Classes keep styling in CSS and behavior in JavaScript. Inline styles are useful for dynamic
values but can become hard to maintain.
```

Strong answer:

```text
I usually toggle classes from JavaScript and let CSS handle presentation. I use inline styles
only for truly dynamic values like measured positions or custom properties.
```

---

## 11. Removing And Replacing Elements

Remove:

```javascript
element.remove();
```

Replace:

```javascript
oldElement.replaceWith(newElement);
```

Clear children:

```javascript
container.replaceChildren();
```

Replace all children:

```javascript
container.replaceChildren(header, list, footer);
```

Caution:

```text
Removing DOM nodes can leave event listeners, timers, or external references alive if your code
still holds references. Clean up owned resources.
```

---

## 12. Event Basics

Add listener:

```javascript
button.addEventListener("click", event => {
    console.log("clicked", event.target);
});
```

Remove listener:

```javascript
function handleClick(event) {
    console.log(event.type);
}

button.addEventListener("click", handleClick);
button.removeEventListener("click", handleClick);
```

Important:

```text
To remove a listener, you need the same function reference.
```

Bad:

```javascript
button.addEventListener("click", () => console.log("clicked"));
button.removeEventListener("click", () => console.log("clicked")); // does not remove original
```

Strong answer:

```text
addEventListener registers event callbacks. If I need cleanup, I keep the handler reference so
removeEventListener can remove the same function.
```

---

## 13. Event Object

Event object contains details about the event.

```javascript
button.addEventListener("click", event => {
    console.log(event.type);
    console.log(event.target);
    console.log(event.currentTarget);
});
```

Important difference:

```text
event.target        -> deepest element that triggered the event
event.currentTarget -> element whose listener is currently running
```

Example:

```html
<button id="save"><span>Save</span></button>
```

If user clicks span:

```text
target = span
currentTarget = button, inside button listener
```

Interview line:

```text
target is where the event originated. currentTarget is the element handling the listener right now.
```

---

## 14. Capturing And Bubbling

Event flow:

```text
capturing phase: window -> document -> html -> body -> target parent
target phase: target element
bubbling phase: target parent -> body -> html -> document -> window
```

Bubbling listener default:

```javascript
parent.addEventListener("click", () => {
    console.log("parent bubble");
});
```

Capture listener:

```javascript
parent.addEventListener("click", () => {
    console.log("parent capture");
}, { capture: true });
```

Example:

```javascript
document.body.addEventListener("click", () => console.log("body bubble"));
document.body.addEventListener("click", () => console.log("body capture"), { capture: true });
```

Strong answer:

```text
Most DOM events travel through capturing, target, and bubbling phases. By default listeners run
in bubbling phase, but addEventListener can register capture listeners.
```

---

## 15. Event Delegation

Event delegation attaches one listener to a parent instead of many children.

HTML:

```html
<ul id="bookings">
    <li><button data-action="cancel" data-id="B1">Cancel</button></li>
    <li><button data-action="cancel" data-id="B2">Cancel</button></li>
</ul>
```

JavaScript:

```javascript
const list = document.querySelector("#bookings");

list?.addEventListener("click", event => {
    const target = event.target;

    if (!(target instanceof HTMLElement)) {
        return;
    }

    const button = target.closest("button[data-action]");

    if (!button) {
        return;
    }

    const action = button.dataset.action;
    const id = button.dataset.id;

    if (action === "cancel" && id) {
        cancelBooking(id);
    }
});
```

Why useful:

- Handles dynamic children.
- Fewer listeners.
- Centralized behavior.
- Better for large lists.

Caution:

```text
Use closest carefully and ensure the matched element belongs to the intended container if nested
structures can cross boundaries.
```

Strong answer:

```text
Event delegation uses bubbling to handle events from child elements at a parent. It is useful
for dynamic lists and avoids attaching many individual listeners.
```

---

## 16. preventDefault

`preventDefault` stops the browser's default action.

Form example:

```javascript
form.addEventListener("submit", event => {
    event.preventDefault();
    saveForm();
});
```

Link example:

```javascript
link.addEventListener("click", event => {
    event.preventDefault();
    openModal();
});
```

Default actions include:

- Form submission.
- Link navigation.
- Checkbox toggling.
- Context menu opening.
- Some keyboard scrolling behavior.

Caution:

```text
Do not prevent default browser behavior casually. It can hurt accessibility and expected user behavior.
```

---

## 17. stopPropagation

`stopPropagation` prevents event from continuing through propagation.

```javascript
button.addEventListener("click", event => {
    event.stopPropagation();
});
```

Use cases:

- Click inside modal should not trigger overlay close.
- Nested interactive elements need separation.

Caution:

```text
Overusing stopPropagation makes event flow harder to reason about and can break delegated listeners.
```

`stopImmediatePropagation`:

```javascript
event.stopImmediatePropagation();
```

This prevents remaining listeners on the same target from running too.

Strong answer:

```text
preventDefault stops default browser action. stopPropagation stops event propagation. They solve
different problems.
```

---

## 18. Passive Listeners

Passive listeners promise not to call `preventDefault`.

```javascript
window.addEventListener("scroll", handleScroll, { passive: true });
```

Why:

```text
For scroll/touch events, passive listeners let the browser continue scrolling without waiting
to see if JavaScript cancels it.
```

Use for:

- Scroll tracking.
- Touch movement analytics.
- Performance-sensitive input listeners when you do not need cancellation.

Caution:

```text
If you need preventDefault, do not mark listener passive.
```

---

## 19. Once And Abortable Listeners

Run listener once:

```javascript
button.addEventListener("click", handleClick, { once: true });
```

Abort listener:

```javascript
const controller = new AbortController();

button.addEventListener("click", handleClick, {
    signal: controller.signal
});

controller.abort();
```

Why useful:

```text
AbortSignal can clean up multiple listeners together.
```

Example:

```javascript
function mountDialog(dialog) {
    const controller = new AbortController();

    dialog.addEventListener("click", handleDialogClick, { signal: controller.signal });
    window.addEventListener("keydown", handleKeyDown, { signal: controller.signal });

    return () => controller.abort();
}
```

---

## 20. Forms

Form submit:

```javascript
const form = document.querySelector("form");

form?.addEventListener("submit", event => {
    event.preventDefault();

    const formData = new FormData(form);
    const email = String(formData.get("email") ?? "");

    submitEmail(email);
});
```

Input event:

```javascript
input.addEventListener("input", event => {
    const target = event.target;

    if (target instanceof HTMLInputElement) {
        console.log(target.value);
    }
});
```

Change vs input:

```text
input  -> fires as value changes
change -> fires when committed, depending control type
```

Validation:

```javascript
if (!form.checkValidity()) {
    form.reportValidity();
    return;
}
```

Strong answer:

```text
For forms, I handle submit, prevent default when using JavaScript submission, read FormData, and
use built-in validation where appropriate while still validating on the server.
```

---

## 21. Keyboard Events

Example:

```javascript
window.addEventListener("keydown", event => {
    if (event.key === "Escape") {
        closeDialog();
    }
});
```

Common properties:

```text
event.key   -> logical key, like "Escape" or "a"
event.code  -> physical key position, like "KeyA"
event.altKey, ctrlKey, metaKey, shiftKey -> modifier states
```

Caution:

```text
Keyboard shortcuts must not break typing in inputs or accessibility expectations.
```

Guard:

```javascript
function isTypingTarget(target) {
    return target instanceof HTMLInputElement
        || target instanceof HTMLTextAreaElement
        || target instanceof HTMLSelectElement
        || target?.isContentEditable;
}
```

---

## 22. Fetch API

Basic GET:

```javascript
const response = await fetch("/api/bookings");
const bookings = await response.json();
```

Important:

```text
fetch rejects on network failure, not on HTTP 4xx/5xx by default.
```

Correct status handling:

```javascript
async function fetchJson(url, options) {
    const response = await fetch(url, options);

    if (!response.ok) {
        throw new Error(`HTTP ${response.status}`);
    }

    return response.json();
}
```

POST JSON:

```javascript
const response = await fetch("/api/bookings", {
    method: "POST",
    headers: {
        "Content-Type": "application/json"
    },
    body: JSON.stringify({ roomId: "R101" })
});
```

Strong answer:

```text
fetch returns a promise for a Response. It only rejects for network-level failures by default,
so I check response.ok for HTTP errors before parsing the body.
```

---

## 23. Fetch Cancellation And Timeout

Use AbortController:

```javascript
const controller = new AbortController();

const request = fetch("/api/bookings", {
    signal: controller.signal
});

controller.abort();
```

Timeout helper:

```javascript
async function fetchWithTimeout(url, options = {}) {
    const { timeoutMs = 5000, ...fetchOptions } = options;
    const controller = new AbortController();
    const timeoutId = setTimeout(() => controller.abort(), timeoutMs);

    try {
        const response = await fetch(url, {
            ...fetchOptions,
            signal: controller.signal
        });

        return response;
    } finally {
        clearTimeout(timeoutId);
    }
}
```

Caution:

```text
Promise.race timeout patterns do not automatically cancel the underlying fetch. AbortController does.
```

---

## 24. CORS

CORS means Cross-Origin Resource Sharing.

Origin:

```text
scheme + host + port
```

Examples:

```text
https://app.example.com
https://api.example.com
http://app.example.com
https://app.example.com:8443
```

These are different origins if scheme, host, or port differs.

CORS is enforced by browsers.

```text
The server must opt in by sending appropriate CORS response headers.
```

Common header:

```text
Access-Control-Allow-Origin: https://app.example.com
```

With credentials:

```text
Access-Control-Allow-Credentials: true
Access-Control-Allow-Origin cannot be * when credentials are included
```

Strong answer:

```text
CORS is a browser security mechanism. The browser blocks frontend JavaScript from reading
cross-origin responses unless the server explicitly allows that origin through CORS headers.
```

---

## 25. Preflight Requests

A preflight is an OPTIONS request sent before certain cross-origin requests.

Preflight happens for non-simple requests, such as:

- Custom headers.
- Methods like PUT, PATCH, DELETE.
- Content types outside simple allowed values.
- Credentialed scenarios depending setup.

Example:

```text
OPTIONS /api/bookings
Origin: https://app.example.com
Access-Control-Request-Method: POST
Access-Control-Request-Headers: content-type, authorization
```

Server responds:

```text
Access-Control-Allow-Origin: https://app.example.com
Access-Control-Allow-Methods: POST
Access-Control-Allow-Headers: content-type, authorization
```

Interview line:

```text
A preflight lets the browser ask the server whether the actual cross-origin request is allowed
before sending it.
```

---

## 26. Credentials And Cookies With Fetch

Fetch credential modes:

```javascript
fetch("/api/me", { credentials: "same-origin" });
fetch("https://api.example.com/me", { credentials: "include" });
fetch("/api/public", { credentials: "omit" });
```

Meaning:

| Mode | Behavior |
|---|---|
| `same-origin` | Send credentials to same origin only |
| `include` | Send credentials cross-origin too if allowed |
| `omit` | Do not send credentials |

Cookie-based auth cross-origin needs:

```text
Frontend: credentials: "include"
Server: Access-Control-Allow-Credentials: true
Server: specific Access-Control-Allow-Origin
Cookie: SameSite=None; Secure for third-party context
```

Strong answer:

```text
For cookie-based cross-origin requests, both fetch credentials and server CORS headers must be
configured correctly. Wildcard CORS origin cannot be used with credentials.
```

---

## 27. Browser Storage Overview

| Storage | Lifetime | Sent To Server | Size | Use Case |
|---|---|---:|---:|---|
| Cookie | Configurable | Yes, matching requests | Small | Sessions, server-readable state |
| localStorage | Until cleared | No | Medium | Non-sensitive preferences |
| sessionStorage | Tab session | No | Medium | Per-tab temporary state |
| IndexedDB | Persistent | No | Large | Structured offline/client data |
| Cache API | Persistent-ish | No direct | Large | Request/response caching, service workers |

Strong answer:

```text
Storage choice depends on lifetime, sensitivity, server visibility, size, and access pattern.
I avoid putting sensitive tokens in localStorage when XSS risk matters.
```

---

## 28. Cookies

Cookie example:

```text
Set-Cookie: session=abc; HttpOnly; Secure; SameSite=Lax; Path=/
```

Important attributes:

| Attribute | Meaning |
|---|---|
| HttpOnly | JavaScript cannot read cookie |
| Secure | Sent only over HTTPS |
| SameSite | Controls cross-site sending |
| Expires/Max-Age | Lifetime |
| Path/Domain | Scope |

JavaScript can read non-HttpOnly cookies:

```javascript
console.log(document.cookie);
```

Security line:

```text
Session cookies should often be HttpOnly and Secure so JavaScript cannot steal them during XSS.
```

CSRF connection:

```text
Cookies are sent automatically with matching requests, so cookie-based auth needs CSRF protection.
```

---

## 29. localStorage And sessionStorage

localStorage:

```javascript
localStorage.setItem("theme", "dark");
const theme = localStorage.getItem("theme");
localStorage.removeItem("theme");
```

sessionStorage:

```javascript
sessionStorage.setItem("draft", "hello");
```

Important:

```text
localStorage and sessionStorage store strings.
```

JSON example:

```javascript
const settings = { theme: "dark" };
localStorage.setItem("settings", JSON.stringify(settings));

const raw = localStorage.getItem("settings");
const parsed = raw ? JSON.parse(raw) : null;
```

Cautions:

- Synchronous API can block main thread for large data.
- Accessible to JavaScript, so XSS can read it.
- Not suitable for highly sensitive secrets.
- Can be unavailable or limited in privacy modes.

Strong answer:

```text
localStorage is simple persistent string storage, but it is synchronous and readable by JavaScript.
I use it for non-sensitive preferences, not high-value secrets.
```

---

## 30. IndexedDB Awareness

IndexedDB is browser database storage for larger structured data.

Use cases:

- Offline-first apps.
- Large client-side caches.
- Structured records.
- Background sync workflows.

Why not always use localStorage:

```text
localStorage is synchronous, string-only, and limited. IndexedDB is asynchronous and designed
for larger structured storage.
```

Interview line:

```text
For small preferences, localStorage may be fine. For large structured offline data, IndexedDB is
the more appropriate browser storage API.
```

---

## 31. URL And URLSearchParams

Parse URL:

```javascript
const url = new URL("https://example.com/bookings?page=2&status=CONFIRMED");

console.log(url.searchParams.get("page"));
```

Build query safely:

```javascript
const params = new URLSearchParams({
    page: "2",
    status: "CONFIRMED"
});

const requestUrl = `/api/bookings?${params.toString()}`;
```

Current page:

```javascript
const current = new URL(window.location.href);
```

Strong answer:

```text
I use URL and URLSearchParams instead of manual string concatenation because they handle encoding
and make query manipulation safer and clearer.
```

---

## 32. History API

Push new URL without full page reload:

```javascript
history.pushState({ page: "bookings" }, "", "/bookings");
```

Replace current history entry:

```javascript
history.replaceState({ page: "bookings" }, "", "/bookings?page=2");
```

Listen to back/forward:

```javascript
window.addEventListener("popstate", event => {
    console.log(event.state);
});
```

Use cases:

- Single-page app navigation.
- Filter state in URL.
- Modal routes.

Caution:

```text
Changing history does not render UI by itself. Your app/router must update state and view.
```

---

## 33. Location And Navigation

Read current URL:

```javascript
console.log(window.location.href);
console.log(window.location.pathname);
```

Navigate:

```javascript
window.location.assign("/login");
```

Replace without back entry:

```javascript
window.location.replace("/login");
```

Reload:

```javascript
window.location.reload();
```

Caution:

```text
For SPA internal navigation, prefer router/history APIs instead of full reload when appropriate.
```

---

## 34. Timers In Browser

Timeout:

```javascript
const timeoutId = setTimeout(() => {
    console.log("later");
}, 1000);

clearTimeout(timeoutId);
```

Interval:

```javascript
const intervalId = setInterval(() => {
    console.log("tick");
}, 1000);

clearInterval(intervalId);
```

Cautions:

- Timers are delayed by busy main thread.
- Background tabs may throttle timers.
- Intervals can drift.
- Always clean up timers owned by removed UI.

Strong answer:

```text
Timer delay is a minimum, not an exact guarantee. Browser scheduling, main-thread work, and tab
throttling can delay timer callbacks.
```

---

## 35. requestAnimationFrame

Use for visual updates:

```javascript
function animate() {
    element.style.transform = `translateX(${position}px)`;
    position += 1;

    requestAnimationFrame(animate);
}

requestAnimationFrame(animate);
```

Why:

```text
requestAnimationFrame runs before the next paint, making it suitable for animations.
```

Compared to timer:

```text
setTimeout is general scheduling. requestAnimationFrame is paint-aligned scheduling.
```

Caution:

```text
Do not do heavy work inside animation callbacks. Keep frames short to maintain smooth rendering.
```

---

## 36. requestIdleCallback Awareness

`requestIdleCallback` schedules low-priority work when the browser is idle.

```javascript
requestIdleCallback(deadline => {
    while (deadline.timeRemaining() > 0 && tasks.length > 0) {
        runTask(tasks.shift());
    }
});
```

Use cases:

- Non-urgent analytics preparation.
- Cache warming.
- Low-priority cleanup.

Caution:

```text
Support and timing vary. Do not rely on requestIdleCallback for urgent user-visible work.
```

---

## 37. Rendering Pipeline

Simplified rendering pipeline:

```text
JavaScript
Style calculation
Layout
Paint
Composite
```

Definitions:

| Step | Meaning |
|---|---|
| Style | Calculate CSS rules for elements |
| Layout | Calculate size and position |
| Paint | Fill pixels for visual parts |
| Composite | Combine layers on screen |

DOM/CSS changes may trigger different costs.

```text
Change transform/opacity -> often composite-only
Change color -> paint
Change width/height/top/font -> layout + paint + composite
```

Strong answer:

```text
DOM changes can trigger style, layout, paint, and compositing work. Performance-sensitive UI
tries to minimize layout and paint, especially during scroll and animation.
```

---

## 38. Reflow, Repaint, Composite

Reflow is layout recalculation.

Examples that may trigger layout:

- Changing width/height.
- Changing font size.
- Adding/removing elements.
- Reading layout after writing styles.

Repaint updates pixels.

Examples:

- Color changes.
- Background changes.
- Box shadow changes.

Composite combines layers.

Examples:

- `transform` changes.
- `opacity` changes.

Interview line:

```text
Layout is usually more expensive than paint, and transform/opacity animations are often smoother
because they can be handled by compositing.
```

---

## 39. Layout Thrashing

Layout thrashing happens when code repeatedly writes then reads layout.

Bad:

```javascript
for (const item of items) {
    item.style.width = "200px";
    console.log(item.offsetWidth);
}
```

The read may force the browser to calculate layout repeatedly.

Better:

```javascript
for (const item of items) {
    item.style.width = "200px";
}

for (const item of items) {
    console.log(item.offsetWidth);
}
```

Even better:

```text
Batch reads, then batch writes.
```

Strong answer:

```text
Layout thrashing occurs when JavaScript alternates DOM writes and layout reads, forcing repeated
synchronous layout calculations. I batch reads and writes to avoid it.
```

---

## 40. Long Tasks

A long task blocks the main thread long enough to hurt responsiveness.

Example:

```javascript
button.addEventListener("click", () => {
    for (let index = 0; index < 1_000_000_000; index++) {
        // heavy sync work
    }
});
```

Impact:

- Clicks delayed.
- Input delayed.
- Rendering delayed.
- Timers delayed.
- Page feels frozen.

Mitigation:

- Optimize algorithm.
- Split work into chunks.
- Use Web Workers.
- Reduce DOM work.
- Virtualize large lists.

Interview line:

```text
Because browser JavaScript runs on the main thread, long synchronous tasks block user input and
rendering. Async/await does not fix CPU-heavy synchronous work.
```

---

## 41. IntersectionObserver

Detect when element enters or leaves viewport.

```javascript
const observer = new IntersectionObserver(entries => {
    for (const entry of entries) {
        if (entry.isIntersecting) {
            console.log("visible", entry.target);
        }
    }
});

observer.observe(document.querySelector("#sentinel"));
```

Use cases:

- Lazy loading images.
- Infinite scrolling.
- Visibility analytics.
- Trigger animations on visibility.

Why better than scroll polling:

```text
Browser can optimize observation instead of running heavy scroll handlers constantly.
```

---

## 42. ResizeObserver

Observe element size changes.

```javascript
const observer = new ResizeObserver(entries => {
    for (const entry of entries) {
        console.log(entry.contentRect.width);
    }
});

observer.observe(panel);
```

Use cases:

- Responsive components.
- Charts.
- Virtualized layouts.
- Container-aware UI.

Caution:

```text
Avoid feedback loops where resize handling changes size repeatedly.
```

---

## 43. MutationObserver

Observe DOM changes.

```javascript
const observer = new MutationObserver(mutations => {
    for (const mutation of mutations) {
        console.log(mutation.type);
    }
});

observer.observe(document.body, {
    childList: true,
    subtree: true
});
```

Use cases:

- Integrating with third-party DOM changes.
- Detecting dynamically inserted nodes.
- Dev tooling.

Caution:

```text
Do not use MutationObserver as a substitute for owning your application state. It is a tool for
observing DOM changes, not a primary state-management model.
```

---

## 44. Web Workers

Web Workers run JavaScript off the main thread.

Main thread:

```javascript
const worker = new Worker("worker.js");

worker.postMessage({ numbers: [1, 2, 3] });

worker.addEventListener("message", event => {
    console.log(event.data);
});
```

Worker:

```javascript
self.addEventListener("message", event => {
    const sum = event.data.numbers.reduce((total, value) => total + value, 0);
    self.postMessage({ sum });
});
```

Use cases:

- CPU-heavy calculations.
- Parsing large data.
- Image processing.
- Keeping UI responsive.

Limitations:

```text
Workers do not directly access the DOM. Communication uses message passing.
```

Strong answer:

```text
Web Workers are useful when CPU-heavy JavaScript would block the main thread. They keep UI
responsive by moving work off the main thread, but they communicate through messages and cannot
manipulate the DOM directly.
```

---

## 45. Service Workers Awareness

Service Worker is a background script between page and network.

Use cases:

- Offline support.
- Request caching.
- Push notifications.
- Background sync.
- PWA behavior.

Simplified flow:

```text
Page fetch -> Service Worker intercepts -> cache/network strategy -> response
```

Registration:

```javascript
if ("serviceWorker" in navigator) {
    navigator.serviceWorker.register("/service-worker.js");
}
```

Caution:

```text
Service workers can cause confusing cache bugs if update and invalidation strategies are weak.
```

Interview line:

```text
Service workers are powerful for offline and caching, but they require careful cache versioning,
update handling, and security awareness because they sit between the app and network.
```

---

## 46. WebSocket Awareness

WebSocket provides persistent bidirectional communication.

```javascript
const socket = new WebSocket("wss://example.com/events");

socket.addEventListener("open", () => {
    socket.send(JSON.stringify({ type: "subscribe", topic: "bookings" }));
});

socket.addEventListener("message", event => {
    const message = JSON.parse(event.data);
    console.log(message);
});
```

Use cases:

- Real-time dashboards.
- Chat.
- Notifications.
- Live collaboration.

Production concerns:

- Reconnection.
- Heartbeats.
- Backpressure.
- Authentication.
- Message validation.
- Ordering and idempotency.

---

## 47. Security: XSS

XSS means Cross-Site Scripting.

Risky:

```javascript
commentsContainer.innerHTML = userComment;
```

Safer:

```javascript
commentsContainer.textContent = userComment;
```

XSS can steal:

- localStorage tokens.
- non-HttpOnly cookies indirectly through actions.
- page data.
- user actions.

Defenses:

- Escape output.
- Avoid unsafe HTML injection.
- Sanitize allowed HTML with trusted sanitizer.
- Use Content Security Policy.
- Use HttpOnly cookies for sessions where appropriate.
- Validate and encode at boundaries.

Strong answer:

```text
XSS happens when untrusted input executes as script in the page. I avoid unsafe innerHTML, encode
or sanitize output, use CSP, and avoid storing high-value secrets where injected JavaScript can read them.
```

---

## 48. Security: CSRF

CSRF means Cross-Site Request Forgery.

Problem:

```text
If auth uses cookies, browser may automatically send cookies with requests. A malicious site may
try to trigger state-changing requests to another site.
```

Defenses:

- SameSite cookies.
- CSRF tokens.
- Check Origin/Referer for state-changing requests.
- Use proper CORS configuration.
- Avoid state-changing GET requests.

Important distinction:

```text
CORS is not a complete CSRF defense. CORS controls whether browser JavaScript can read responses.
CSRF is about causing authenticated requests.
```

Strong answer:

```text
CSRF matters most with cookie-based authentication because cookies may be sent automatically.
SameSite cookies and CSRF tokens are common protections.
```

---

## 49. Security: Storage And Tokens

Token storage trade-offs:

| Storage | Pros | Cons |
|---|---|---|
| localStorage | Easy JS access | Stolen by XSS |
| sessionStorage | Tab-scoped | Still stolen by XSS |
| Memory | Harder to steal persistently | Lost on refresh |
| HttpOnly cookie | JS cannot read | Needs CSRF protection |

Interview-safe line:

```text
There is no perfect token storage. The choice depends on threat model. localStorage is vulnerable
to XSS token theft, while HttpOnly cookies reduce JS theft but require CSRF protections.
```

Production answer:

```text
I reduce risk with short-lived tokens, refresh rotation, HttpOnly/Secure/SameSite cookies where
appropriate, CSP, XSS prevention, CSRF defenses, and server-side session controls.
```

---

## 50. Accessibility Basics

Good frontend JavaScript should preserve accessibility.

Examples:

- Use semantic HTML first.
- Buttons for actions, links for navigation.
- Labels for inputs.
- Keyboard navigation.
- Focus management in dialogs.
- ARIA only when semantic HTML is not enough.
- Do not trap focus accidentally.

Button:

```html
<button type="button">Cancel booking</button>
```

Input label:

```html
<label for="email">Email</label>
<input id="email" name="email" type="email">
```

Dialog focus idea:

```javascript
function openDialog(dialog) {
    dialog.showModal();
    dialog.querySelector("button")?.focus();
}
```

Strong answer:

```text
Accessibility is not an afterthought. JavaScript interactions should preserve semantic HTML,
keyboard support, focus management, and screen-reader-friendly state.
```

---

## 51. Progressive Enhancement

Progressive enhancement means start with a usable baseline, then enhance with JavaScript.

Example:

```html
<form action="/bookings" method="post">
    <button type="submit">Book</button>
</form>
```

JavaScript can enhance:

```javascript
form.addEventListener("submit", async event => {
    event.preventDefault();
    await submitWithFetch(new FormData(form));
});
```

Why useful:

- Better resilience.
- Better accessibility.
- Better SEO for some pages.
- Graceful failure when JS breaks.

Interview line:

```text
Progressive enhancement means the core experience works with standard browser behavior, and
JavaScript improves it rather than being the only way the page functions.
```

---

## 52. Browser Compatibility

Questions before using a Web API:

1. Is it supported in target browsers?
2. Is it available in mobile webviews?
3. Is a polyfill possible?
4. Does it require HTTPS or permissions?
5. Does it behave differently in private browsing?
6. What is the fallback?

Example:

```javascript
if ("IntersectionObserver" in window) {
    // use observer
} else {
    // fallback lazy loading strategy
}
```

Strong answer:

```text
For browser APIs, I check target browser support and provide fallback behavior when the feature
is not universally available.
```

---

## 53. Mini Program: Event Delegated Todo List

HTML shape:

```html
<form id="todo-form">
    <input name="title" required>
    <button type="submit">Add</button>
</form>

<ul id="todo-list"></ul>
```

JavaScript:

```javascript
const form = document.querySelector("#todo-form");
const list = document.querySelector("#todo-list");

const todos = [];

form.addEventListener("submit", event => {
    event.preventDefault();

    const formData = new FormData(form);
    const title = String(formData.get("title") ?? "").trim();

    if (!title) {
        return;
    }

    const todo = {
        id: crypto.randomUUID(),
        title,
        completed: false
    };

    todos.push(todo);
    renderTodos();
    form.reset();
});

list.addEventListener("click", event => {
    const target = event.target;

    if (!(target instanceof HTMLElement)) {
        return;
    }

    const button = target.closest("button[data-action]");

    if (!button) {
        return;
    }

    const id = button.dataset.id;
    const action = button.dataset.action;
    const todo = todos.find(item => item.id === id);

    if (!todo) {
        return;
    }

    if (action === "toggle") {
        todo.completed = !todo.completed;
    }

    if (action === "delete") {
        const index = todos.findIndex(item => item.id === id);
        todos.splice(index, 1);
    }

    renderTodos();
});

function renderTodos() {
    const fragment = document.createDocumentFragment();

    for (const todo of todos) {
        const item = document.createElement("li");
        item.classList.toggle("completed", todo.completed);

        const title = document.createElement("span");
        title.textContent = todo.title;

        const toggle = document.createElement("button");
        toggle.type = "button";
        toggle.dataset.action = "toggle";
        toggle.dataset.id = todo.id;
        toggle.textContent = todo.completed ? "Undo" : "Done";

        const remove = document.createElement("button");
        remove.type = "button";
        remove.dataset.action = "delete";
        remove.dataset.id = todo.id;
        remove.textContent = "Delete";

        item.append(title, toggle, remove);
        fragment.append(item);
    }

    list.replaceChildren(fragment);
}
```

Why strong:

- Uses FormData.
- Uses textContent safely.
- Uses event delegation.
- Uses dataset for action metadata.
- Uses DocumentFragment for rendering.
- Uses replaceChildren for clean update.

---

## 54. Mini Program: Safe Fetch Search

Problem:

```text
Search input should debounce requests, cancel stale requests, and avoid old results replacing new ones.
```

Code:

```javascript
const input = document.querySelector("#search");
const results = document.querySelector("#results");

let currentController;

input.addEventListener("input", debounce(() => {
    search(input.value);
}, 300));

async function search(query) {
    currentController?.abort();

    if (!query.trim()) {
        results.replaceChildren();
        return;
    }

    const controller = new AbortController();
    currentController = controller;

    try {
        const params = new URLSearchParams({ q: query });
        const response = await fetch(`/api/search?${params}`, {
            signal: controller.signal
        });

        if (!response.ok) {
            throw new Error(`HTTP ${response.status}`);
        }

        const items = await response.json();

        if (currentController !== controller) {
            return;
        }

        renderResults(items);
    } catch (error) {
        if (error.name !== "AbortError") {
            results.textContent = "Search failed";
        }
    }
}

function renderResults(items) {
    const fragment = document.createDocumentFragment();

    for (const item of items) {
        const element = document.createElement("li");
        element.textContent = item.label;
        fragment.append(element);
    }

    results.replaceChildren(fragment);
}

function debounce(fn, delayMs) {
    let timeoutId;

    return function debounced(...args) {
        clearTimeout(timeoutId);
        timeoutId = setTimeout(() => fn.apply(this, args), delayMs);
    };
}
```

Production value:

- Debounces noisy input.
- Cancels stale requests.
- Handles HTTP errors.
- Avoids unsafe HTML rendering.
- Uses URLSearchParams.

---

## 55. Mini Program: Lazy Image Loader

HTML:

```html
<img data-src="/images/room-1.jpg" alt="Room 1" class="lazy">
```

JavaScript:

```javascript
const images = document.querySelectorAll("img[data-src]");

const observer = new IntersectionObserver(entries => {
    for (const entry of entries) {
        if (!entry.isIntersecting) {
            continue;
        }

        const image = entry.target;

        if (!(image instanceof HTMLImageElement)) {
            continue;
        }

        image.src = image.dataset.src ?? "";
        image.removeAttribute("data-src");
        observer.unobserve(image);
    }
}, {
    rootMargin: "200px"
});

images.forEach(image => observer.observe(image));
```

Why strong:

```text
IntersectionObserver avoids heavy scroll polling and loads images shortly before they enter view.
```

Modern HTML note:

```html
<img src="/images/room-1.jpg" loading="lazy" alt="Room 1">
```

Native lazy loading may be enough for simple cases.

---

## 56. Common Browser Traps

### Trap 1: `innerHTML` With User Input

```javascript
element.innerHTML = comment;
```

Risk:

```text
XSS if comment is untrusted.
```

Better:

```javascript
element.textContent = comment;
```

### Trap 2: target vs currentTarget

```javascript
parent.addEventListener("click", event => {
    console.log(event.target);
    console.log(event.currentTarget);
});
```

Rule:

```text
target is origin. currentTarget is listener owner.
```

### Trap 3: Fetch 404 Does Not Reject

```javascript
const response = await fetch("/missing");
```

Rule:

```text
Check response.ok.
```

### Trap 4: localStorage Stores Objects

```javascript
localStorage.setItem("user", { id: "U1" });
```

Actually stores:

```text
[object Object]
```

Use JSON.

### Trap 5: CSS Display None And Measurements

```javascript
element.style.display = "none";
console.log(element.offsetWidth); // often 0
```

Hidden elements may not have normal layout measurements.

### Trap 6: `stopPropagation` Fixes Everything

It can break parent/delegated handlers. Use narrowly.

---

## 57. Common Mistakes

| Mistake | Why It Hurts | Better Approach |
|---|---|---|
| Using `innerHTML` for untrusted text | XSS risk | Use textContent or sanitize |
| Forgetting querySelector can return null | Runtime crash | Null check or enforce existence |
| Attaching listener to every list item | Memory/perf cost | Use event delegation |
| Confusing target/currentTarget | Wrong element logic | Use currentTarget for listener owner |
| Overusing stopPropagation | Breaks event flow | Use only when necessary |
| Not calling preventDefault on JS form submit | Page reloads unexpectedly | preventDefault when handling in JS |
| Assuming fetch rejects on 500 | HTTP errors still resolve | Check response.ok |
| Using wildcard CORS with credentials | Browser blocks | Use specific origin and credentials headers |
| Storing tokens in localStorage casually | XSS theft risk | Use threat-model-based storage |
| Blocking main thread | Frozen UI | Chunk, worker, optimize |
| Alternating layout reads/writes | Layout thrashing | Batch reads and writes |
| Not cleaning timers/listeners | Memory leaks and stale behavior | Cleanup on unmount/remove |
| Ignoring accessibility | Broken keyboard/screen reader UX | Use semantic HTML and focus management |
| Assuming browser support | Breaks users | Check compatibility and fallback |

---

## 58. Strong Interview Answers

### What is the DOM?

```text
The DOM is the browser's object representation of the HTML document. JavaScript can query and
mutate DOM nodes, and those mutations may cause style, layout, paint, or compositing work.
```

### Event Bubbling And Capturing

```text
Most DOM events travel from ancestors down to the target during capture, then from the target
back up during bubbling. By default addEventListener uses bubbling, but capture can be enabled
with an option.
```

### Event Delegation

```text
Event delegation attaches one listener to a parent and uses bubbling to handle child events. It
is useful for dynamic lists and reduces the number of individual listeners.
```

### preventDefault vs stopPropagation

```text
preventDefault stops the browser's default action, like form submit or link navigation.
stopPropagation stops the event from continuing through the capture/bubble path.
```

### Fetch And HTTP Errors

```text
fetch rejects for network-level failures, but HTTP 4xx and 5xx responses still resolve to a
Response. I check response.ok before parsing and treating the request as successful.
```

### CORS

```text
CORS is a browser-enforced security mechanism. A frontend can only read cross-origin responses
when the server explicitly allows the requesting origin with CORS headers.
```

### Browser Performance

```text
Frontend performance depends on avoiding unnecessary main-thread work, layout thrashing, large
DOM updates, and heavy rendering. I batch DOM reads/writes, use delegation, virtualize large lists,
and move CPU-heavy work to workers when needed.
```

---

## 59. FAANG-Level Question 1

> A large table has 20,000 rows. Each row has three buttons with click handlers. Scrolling is slow and memory usage is high. What would you change?

Strong answer:

```text
I would first avoid rendering all 20,000 rows if the user can only see a small portion. Virtualization
or pagination would reduce DOM size and rendering cost. Second, I would replace per-button event
listeners with event delegation on the table container, using data attributes to identify row and action.

I would also check whether row rendering uses unsafe or expensive innerHTML, whether layout reads
and writes are mixed, and whether scroll handlers are doing heavy synchronous work. For data updates,
I would batch DOM changes and avoid forcing layout repeatedly.
```

This answer shows:

- DOM size awareness.
- Event delegation.
- Rendering performance.
- Layout-thrashing maturity.

---

## 60. FAANG-Level Question 2

> A login app stores JWT access tokens in localStorage. Security review flags it. What is the issue and what alternatives do you discuss?

Strong answer:

```text
The main issue is XSS risk. If an attacker runs JavaScript in the page, they can read localStorage
and steal the token. Alternatives depend on the threat model. HttpOnly Secure SameSite cookies
reduce JavaScript token theft but require CSRF protections. In-memory access tokens reduce
persistence but need refresh strategy. Short-lived tokens, refresh rotation, CSP, XSS prevention,
and server-side session controls can also reduce risk.

I would not claim one storage choice is universally perfect. I would discuss XSS, CSRF, token
lifetime, refresh flow, backend architecture, and user experience trade-offs.
```

---

## 61. FAANG-Level Question 3

> A frontend request works in Postman but fails in the browser with a CORS error. How do you debug it?

Strong answer:

```text
Postman is not enforcing browser CORS, so working in Postman does not prove browser access is
allowed. I would check the browser network panel for the actual request and any OPTIONS preflight.
Then I would inspect Origin, Access-Control-Allow-Origin, Access-Control-Allow-Methods,
Access-Control-Allow-Headers, and credentials settings.

If cookies are involved, I would verify fetch credentials, Access-Control-Allow-Credentials,
SameSite and Secure cookie attributes, and ensure the server returns a specific allowed origin
instead of wildcard. CORS must be fixed on the server or gateway, not by frontend JavaScript.
```

---

## 62. FAANG-Level Question 4

> Users report the page freezes when importing a large CSV file. The code uses async/await. Why can it still freeze?

Strong answer:

```text
async/await does not move CPU-heavy work off the main thread. If CSV parsing is a large synchronous
loop, it blocks input, rendering, timers, and promise continuations. I would profile the main
thread, measure long tasks, and move parsing to a Web Worker or process the file in chunks with
yields between chunks.

I would also consider streaming parsing, progress UI, cancellation, file size limits, and memory
usage. The key is that waiting asynchronously is different from doing CPU work asynchronously.
```

---

## 63. Rapid Revision

- Browser JavaScript runs inside a host environment with Web APIs.
- DOM is the object representation of HTML.
- CSSOM is the object representation of CSS.
- DOM plus CSSOM contribute to rendering.
- querySelector returns first match or null.
- querySelectorAll returns a static NodeList.
- Some older DOM collection APIs return live collections.
- Use textContent for untrusted text.
- innerHTML parses HTML and can create XSS risk.
- Attributes are markup-level values; properties are DOM object state.
- dataset reads data-* attributes.
- classList is preferred for toggling CSS classes.
- removeEventListener needs the same function reference.
- event.target is origin; currentTarget is listener owner.
- Events commonly have capture, target, and bubble phases.
- Event delegation uses bubbling to handle child events at a parent.
- preventDefault stops default browser action.
- stopPropagation stops propagation.
- Passive listeners improve scroll/touch performance when preventDefault is not needed.
- Forms can be handled with submit events and FormData.
- fetch does not reject on HTTP 4xx/5xx by default.
- Always check response.ok.
- AbortController cancels supported async operations like fetch.
- CORS is browser-enforced and server-controlled.
- Preflight is an OPTIONS permission check.
- Credentials require both frontend and server CORS configuration.
- Cookies are sent to server; localStorage is not.
- HttpOnly cookies cannot be read by JavaScript.
- localStorage is synchronous and readable by JavaScript.
- IndexedDB is better for large structured offline data.
- URLSearchParams avoids unsafe query string concatenation.
- History API changes URL without full reload.
- Timer delay is minimum, not exact.
- requestAnimationFrame is for paint-aligned visual updates.
- Rendering pipeline includes style, layout, paint, composite.
- Layout thrashing comes from repeated write/read cycles.
- Long tasks block input and rendering.
- IntersectionObserver is good for visibility/lazy loading.
- ResizeObserver tracks element size changes.
- MutationObserver observes DOM mutations.
- Web Workers move CPU-heavy JS off the main thread.
- Service Workers support offline/cache/PWA behavior.
- WebSocket enables bidirectional real-time communication.
- XSS is untrusted script execution.
- CSRF matters with automatic cookie authentication.
- Accessibility requires semantic HTML, keyboard support, and focus management.
- Browser compatibility and fallbacks matter in production.

---

## 64. Official Source Notes

Use these sources when refreshing browser DOM and Web API details:

- MDN DOM introduction: `https://developer.mozilla.org/en-US/docs/Web/API/Document_Object_Model/Introduction`
- MDN Document: `https://developer.mozilla.org/en-US/docs/Web/API/Document`
- MDN Element: `https://developer.mozilla.org/en-US/docs/Web/API/Element`
- MDN addEventListener: `https://developer.mozilla.org/en-US/docs/Web/API/EventTarget/addEventListener`
- MDN Event bubbling: `https://developer.mozilla.org/en-US/docs/Learn_web_development/Core/Scripting/Event_bubbling`
- MDN Fetch API: `https://developer.mozilla.org/en-US/docs/Web/API/Fetch_API`
- MDN CORS: `https://developer.mozilla.org/en-US/docs/Web/HTTP/Guides/CORS`
- MDN AbortController: `https://developer.mozilla.org/en-US/docs/Web/API/AbortController`
- MDN Web Storage API: `https://developer.mozilla.org/en-US/docs/Web/API/Web_Storage_API`
- MDN IndexedDB: `https://developer.mozilla.org/en-US/docs/Web/API/IndexedDB_API`
- MDN History API: `https://developer.mozilla.org/en-US/docs/Web/API/History_API`
- MDN IntersectionObserver: `https://developer.mozilla.org/en-US/docs/Web/API/Intersection_Observer_API`
- MDN Web Workers: `https://developer.mozilla.org/en-US/docs/Web/API/Web_Workers_API`
- MDN Service Worker API: `https://developer.mozilla.org/en-US/docs/Web/API/Service_Worker_API`
- web.dev rendering performance: `https://web.dev/articles/rendering-performance`
- web.dev long tasks: `https://web.dev/articles/long-tasks-devtools`
- OWASP XSS: `https://owasp.org/www-community/attacks/xss/`
- OWASP CSRF: `https://owasp.org/www-community/attacks/csrf`

Interview safety line:

```text
When I discuss browser JavaScript, I connect code to browser behavior: DOM mutation, event
propagation, network security, storage risks, rendering cost, accessibility, compatibility, and
main-thread responsiveness.
```
