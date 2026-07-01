# Chrome DevTools: Beginner to Pro Mastery

> **Scope:** Every DevTools panel from first principles → Network tab deep dive → Application/Storage tab → Core Web Vitals → Performance profiling → Memory leak detection → JS debugging → real-world debugging scenarios → interview Q&A → cheatsheet.

---

## Table of Contents

1. [Mental Model: What Is DevTools?](#1-mental-model)
2. [Opening DevTools + Panel Overview](#2-opening-devtools)
3. [Elements Tab: DOM + CSS Live Editing](#3-elements-tab)
4. [Console Tab: JavaScript REPL + Debugging](#4-console-tab)
5. [Sources Tab: JavaScript Debugging](#5-sources-tab)
6. [Network Tab: Request/Response Deep Dive](#6-network-tab)
7. [Performance Tab: Profiling + Core Web Vitals](#7-performance-tab)
8. [Memory Tab: Heap Snapshots + Leak Detection](#8-memory-tab)
9. [Application Tab: Storage, Cookies, Service Workers](#9-application-tab)
10. [Security Tab: Certificates + Mixed Content](#10-security-tab)
11. [Lighthouse: Audits + Scoring](#11-lighthouse)
12. [Core Web Vitals Deep Dive](#12-core-web-vitals)
13. [Responsive Design Mode](#13-responsive-design)
14. [Advanced: Coverage, Overrides, Rendering, Sensors](#14-advanced)
15. [Real-World Debugging Scenarios](#15-debugging-scenarios)
16. [Interview Q&A: Beginner to Pro](#16-interview-qa)
17. [Cheatsheet: All Shortcuts + Commands](#17-cheatsheet)

---

## 1. Mental Model

### What Is DevTools?

Chrome DevTools is a set of web developer tools built directly into the Chrome browser. Think of it as an X-ray machine for any web page — it lets you see exactly what the browser downloaded, what code is running, what's stored, and how fast everything happens.

```text
DevTools gives you three superpowers:

  1. INSPECT:  See the real DOM, CSS, network requests, stored data
               Nothing is hidden — even minified code can be inspected

  2. MODIFY:   Change the DOM, CSS, JavaScript live (changes are not permanent)
               Test fixes without editing source files

  3. MEASURE:  Measure performance, find memory leaks, measure Core Web Vitals
               Profile why your page is slow or crashing
```

### The Browser Rendering Pipeline (Why This Matters for Debugging)

```text
URL entered → DNS → TCP → TLS → HTTP request
    ↓
Server returns HTML
    ↓
Browser parses HTML → builds DOM tree
    ↓
Browser parses CSS  → builds CSSOM tree
    ↓
DOM + CSSOM merge   → Render Tree
    ↓
Layout (Reflow)     → calculate sizes/positions for each node
    ↓
Paint               → draw pixels for each node (fill, text, borders)
    ↓
Composite           → GPU combines layers → final frame to screen
    ↓
JavaScript execution can trigger Layout (expensive!) or Composite (cheap)

DevTools exposes every step of this pipeline.
```

---

## 2. Opening DevTools + Panel Overview

### Open DevTools

```text
macOS:
  Cmd + Option + I      → open DevTools (last used panel)
  Cmd + Option + J      → open DevTools → Console tab
  Cmd + Option + C      → open DevTools → Elements tab (with inspect cursor)
  F12                   → toggle DevTools

Windows/Linux:
  F12                   → open DevTools
  Ctrl + Shift + I      → open DevTools
  Ctrl + Shift + J      → open DevTools → Console
  Ctrl + Shift + C      → Elements tab with inspect cursor

Right-click any element on page → "Inspect" → Elements tab

Undock DevTools (useful for wide layouts):
  Click the three-dot menu (⋮) in DevTools → Dock side
  Options: Dock to bottom / left / right / Open as separate window
```

### Panel Overview

```text
┌─────────────────────────────────────────────────────────────────┐
│ Elements │ Console │ Sources │ Network │ Performance │ Memory   │
│ Application │ Security │ Lighthouse │ Recorder │ ...           │
└─────────────────────────────────────────────────────────────────┘

Elements:      Live DOM tree + CSS editor + Box model + Accessibility
Console:       JS REPL + log output + errors + warnings
Sources:       JS file viewer + debugger + breakpoints + snippets
Network:       All HTTP requests + responses + timing + HAR export
Performance:   CPU profiling + Core Web Vitals + flame chart
Memory:        Heap snapshots + allocation tracking + memory leaks
Application:   Cookies + localStorage + sessionStorage + IndexedDB +
               Service Workers + Cache Storage + Web App Manifest
Security:      TLS cert info + mixed content + origin security
Lighthouse:    Automated audits (performance/SEO/accessibility/PWA)
Recorder:      Record user flows + replay + performance measurement

Hidden panels (via ⋮ → More tools):
  Coverage:    Which CSS/JS bytes are actually used vs unused
  Rendering:   Frame rate overlay + paint flashing + layer borders
  Sensors:     Fake geolocation + device orientation
  Network conditions: Manual UA string + network throttling
  Request Blocking: Block specific URLs
  WebAuthn:    Debug passkeys/FIDO2
```

### DevTools Settings

```text
Settings (F1 or ⚙ icon):
  Theme:           Light / Dark (System preference)
  Experiments:     Enable unreleased features
  Blackboxing:     Hide framework internals from stack traces
  Ignore list:     (newer name for blackboxing) — hide node_modules etc.
  Persistence:     Workspace: map DevTools edits to local files
  DevTools language: English, or your locale
```

---

## 3. Elements Tab: DOM + CSS Live Editing

### DOM Tree

```text
Left pane shows the LIVE DOM tree (not the source HTML — the parsed, modified DOM).
This is crucial: if JavaScript modified the DOM, you see the current state.

Navigation:
  Click any element to select it and see its CSS on the right
  Arrow keys to navigate up/down in DOM tree
  Click triangle to expand/collapse nodes
  Double-click to edit text content, tag name, or attribute values
  Delete key to delete selected element (temporary, page reload restores it)
  Drag elements to reorder them in the DOM
  
Keyboard shortcuts in DOM tree:
  H          → hide selected element (adds display:none)
  Cmd/Ctrl+Z → undo DOM change (limited history)
  Cmd/Ctrl+F → search DOM (supports text, CSS selector, XPath)
```

### Inspect Specific Elements

```text
Three ways to select an element:
  1. Inspect cursor: Cmd+Opt+C → hover over element on page → click
  2. Right-click on page element → Inspect
  3. In DevTools: Cmd/Ctrl+F → type selector → Enter

After selecting:
  The element is highlighted in blue on the page
  The Styles pane on the right shows all applied CSS
  Breadcrumb trail at the bottom shows: html > body > div.container > p
  
$0 shortcut: the currently selected element is available as $0 in Console
  e.g., $0.textContent, $0.style.display = 'none'
```

### Styles Pane

```text
Shows ALL CSS rules applied to the selected element, in cascade order:
  - Inline styles (highest specificity)
  - ID selectors
  - Class selectors
  - Element selectors
  - Inherited styles
  - Browser default styles (at the bottom)

Features:
  - Strikethrough text = overridden rule
  - Click a property value to edit it live
  - Click the color swatch to open color picker
  - Toggle a property on/off by clicking the checkbox next to it
  - Add new property by clicking at end of a rule
  - Click :hov button to force element states (:hover, :focus, :active, :visited)
  - Filter box at top to search for specific CSS properties
  
Computed pane:
  Shows the FINAL computed value for every property
  Useful for: "what font-size is actually applied after all the cascading?"
  Click any computed property to see which CSS rule set it
  Shows the box model visually (margin, border, padding, content dimensions)
```

### Box Model

```text
In Computed pane, the box model diagram shows:
  ┌──────────────────────────────────┐
  │           margin                 │
  │   ┌──────────────────────────┐   │
  │   │         border           │   │
  │   │   ┌──────────────────┐   │   │
  │   │   │     padding      │   │   │
  │   │   │   ┌──────────┐   │   │   │
  │   │   │   │ content  │   │   │   │
  │   │   │   │ 300×150  │   │   │   │
  │   │   │   └──────────┘   │   │   │
  │   │   └──────────────────┘   │   │
  │   └──────────────────────────┘   │
  └──────────────────────────────────┘

Click any number to edit it live
Hover over a box model region → that region highlights on the page
Useful for: diagnosing unexpected spacing, margin collapse issues
```

### Event Listeners Pane

```text
Shows all event listeners attached to the selected element and its ancestors.

For each listener:
  - Event type (click, input, keydown, etc.)
  - Handler function location (file + line number)
  - Whether it bubbles / is passive / once-only

Click the handler location to jump to the source code in Sources tab.

Tip: Check "Ancestors" checkbox to see listeners on parent elements too.
     Useful for: "why isn't my click event firing?" → check if parent is intercepting.
```

### Accessibility Tree

```text
Elements tab → Accessibility pane (bottom):
  Shows the accessibility tree — what screen readers see
  
  Each node has:
    Role:    button, link, heading, article, etc.
    Name:    the accessible name (label text, aria-label, alt text)
    State:   focusable, focused, checked, disabled, etc.
  
Useful for: testing if your button has an accessible name,
            verifying ARIA roles are correct,
            checking color contrast (Styles pane shows a contrast ratio badge).
```

---

## 4. Console Tab: JavaScript REPL + Debugging

### Console Methods

```javascript
// Logging
console.log('basic message', { user: 'alice', id: 42 });   // standard log
console.info('info message');                              // same as log (blue icon)
console.warn('this is a warning');                         // yellow warning
console.error('this is an error');                         // red error + stack trace
console.debug('debug detail');                             // hidden by default (toggle in filter)

// Grouping (collapsible sections)
console.group('API Calls');
  console.log('GET /users');
  console.log('POST /login');
console.groupEnd();

console.groupCollapsed('Collapsed group');  // starts collapsed
  console.log('details...');
console.groupEnd();

// Tables (renders objects/arrays as a table)
console.table([
  { name: 'Alice', role: 'admin' },
  { name: 'Bob',   role: 'viewer' },
]);
console.table({ total: 100, paid: 75, outstanding: 25 });

// Timing
console.time('fetch-users');
await fetch('/api/users');
console.timeEnd('fetch-users');        // prints: fetch-users: 234ms

// Counting
console.count('loginAttempt');         // loginAttempt: 1
console.count('loginAttempt');         // loginAttempt: 2
console.countReset('loginAttempt');

// Assertions
console.assert(user.isAdmin, 'User is not admin!', user);
// Only logs if condition is false

// Stack trace
console.trace('how did I get here?');  // prints current call stack

// Clearing
console.clear();
```

### Console Dollar Sign Shortcuts (DevTools-Specific)

```javascript
// CSS selector shortcuts (only in DevTools console, not in page scripts)
$('#app')               // same as document.querySelector('#app')
$$('.card')             // same as Array.from(document.querySelectorAll('.card'))
                        // returns an array, not NodeList

$x('//div[@class="card"]')  // XPath selector, returns array of elements

$0                      // the element currently selected in Elements tab
$1                      // previously selected element
$2, $3, $4              // up to 5 history entries

// Inspect
inspect($0)             // opens Elements tab and selects $0
inspect(window.myFunc)  // jumps to function definition in Sources tab

// Monitor function calls
monitor(window.myFunc)  // logs every call to myFunc with arguments
unmonitor(window.myFunc)

monitorEvents(document.body, 'click')    // log all click events on body
unmonitorEvents(document.body, 'click')

// Query from Elements tab selection
getEventListeners($0)   // returns object of all listeners on selected element

// Last evaluated expression
$_                      // evaluates to the last expression typed in console
```

### Reading Errors in the Console

```text
Error format:
  Uncaught TypeError: Cannot read properties of undefined (reading 'name')
      at getUser (app.js:42:15)
      at handleClick (app.js:78:22)
      at HTMLButtonElement.onclick (index.html:23:50)

Reading the stack trace:
  Line 1: error type + message
  Line 2: innermost function where the error occurred + file + line:column
  Line 3-N: call chain leading to the error (most recent call first)
  
  Click any "file:line" link to jump to Sources tab at that exact line.

Error types:
  TypeError:      used wrong type (null/undefined where object expected)
  ReferenceError: variable not declared
  SyntaxError:    invalid JavaScript syntax
  RangeError:     value out of valid range (e.g., new Array(-1))
  URIError:       malformed URI (decodeURIComponent('%'))
  NetworkError:   failed fetch (CORS, DNS, connection refused)
  
Tip: Click the error count badge (🔴) in the top right of DevTools
     to filter Console to show only errors.
```

### Live Expressions

```text
Console toolbar → Create live expression (eye icon):
  Type any JavaScript expression → it evaluates in real-time and updates automatically
  
Examples:
  document.querySelectorAll('.card').length   ← watch element count live
  window.scrollY                              ← watch scroll position
  performance.memory.usedJSHeapSize           ← watch memory live
  navigator.onLine                            ← watch network status
  document.hasFocus()                         ← is page focused?
  
Use case: debugging "the count goes to X and then crashes" — watch it happen live.
```

### Console Filtering

```text
Filter input (top of Console):
  Text filter:    type any string → only logs containing that string shown
  Regex:          /pattern/ → regex match
  Negative:       -text → exclude logs containing "text"
  URL filter:     url:filename.js → only logs from that file
  
Log level buttons:
  All Levels / Verbose / Info / Warnings / Errors
  
  Verbose = includes console.debug() output (hidden by default)
  
Source checkboxes:
  All / First Party / Custom (filter by which file produced the log)

Tip: Right-click a log entry → "Store as global variable"
     → creates temp1 in console window → can then inspect: temp1.someProperty
```

---

## 5. Sources Tab: JavaScript Debugging

### File Navigator

```text
Left panel: three panes
  Page:       All scripts loaded by the current page (organized by origin)
  Filesystem: If you've added a local folder as a workspace
  Overrides:  Local overrides for network-served files
  Snippets:   Reusable JS scripts you can run on any page
  
Navigate to a file:
  Cmd+P (Mac) / Ctrl+P (Win) → fuzzy search by filename
  Click any file → opens in editor pane
  
In editor: lines with code can have breakpoints set
```

### Breakpoint Types

```text
Line Breakpoint:
  Click the line number in the gutter
  Execution pauses at that line when it's reached
  Use for: "I want to stop exactly here and inspect state"

Conditional Breakpoint:
  Right-click line number → Add conditional breakpoint
  Enter a condition: e.g., user.id === 42 or items.length > 100
  Pauses ONLY when the condition is truthy
  Use for: "pause only when processing a specific user/item" — avoids pausing 1000 times

Logpoint:
  Right-click line number → Add logpoint
  Enter a message: e.g., "User ID is: {user.id}"
  Prints to Console WITHOUT pausing execution
  Use for: adding debug logs without changing source code at all
  
DOM Breakpoint:
  In Elements tab: right-click a DOM node → Break on → 
    Subtree modifications   (any descendant changes)
    Attribute modifications (class/style/aria attributes change)
    Node removal            (element is removed from DOM)
  Use for: "something is adding class 'hidden' to my button — but what?"

XHR/Fetch Breakpoint:
  Sources tab → right panel → XHR/Fetch Breakpoints → Add
  Enter a URL substring: e.g., /api/users
  Pauses on any fetch/XHR request matching that URL
  Use for: "find which code is calling this API endpoint"

Event Listener Breakpoint:
  Sources tab → right panel → Event Listener Breakpoints
  Check any event (click, keydown, focus, DOMContentLoaded, etc.)
  Pauses whenever that event fires
  Use for: "find what code handles this click event"

Exception Breakpoint:
  Sources tab → right panel → check "Pause on exceptions"
  Optional: "Pause on caught exceptions" too
  Use for: catching errors that are swallowed by try/catch
```

### The Debugger Controls (When Paused)

```text
When execution pauses at a breakpoint:

  Resume (F8 / ▶):       Continue running until next breakpoint
  Step Over (F10):        Execute current line, stay in same function
  Step Into (F11):        Execute current line, enter function calls
  Step Out (Shift+F11):   Finish current function, return to caller
  
Right panel when paused:
  Scope:        Shows all variables in current scope (local + closure + global)
                Expand any object to see its properties
                Double-click a value to edit it live!
                
  Call Stack:   Shows the chain of function calls that led here
                Click any frame to jump to that function's context
                Useful for: "how did I get into this function?"
                
  Watch:        Add expressions to evaluate continuously while stepping
                e.g., user.permissions.includes('admin')
                e.g., items.filter(i => i.active).length
                
  Breakpoints:  List of all active breakpoints; click to enable/disable
```

### Blackboxing / Ignore List

```text
Problem: stack traces include React internals, Webpack bundler code,
         lodash internals — you don't care about those, you want YOUR code.

Solution: Ignore List (Settings → Ignore List)
  Add patterns: node_modules, webpack, react-dom
  DevTools hides ignored files from stack traces and Steps Into skips them.

Quick way: right-click any file in Sources → Add to ignore list
           → That file's lines are grayed out and skipped when stepping
```

### Snippets

```text
Sources tab → Snippets → + New snippet

Write any JS code. Run with Cmd+Enter (Mac) / Ctrl+Enter (Win) 
or right-click → Run

Snippets run in the context of the current page.
They persist across sessions (saved in DevTools, not the page).

Useful snippets:
  - List all event listeners on the page
  - Find all images without alt text
  - Measure how long DOM operations take
  - Clear all localStorage
  - Disable all CSS animations (for debugging CLS)
```

### Local Overrides

```text
Sources tab → Overrides → Select folder

After selecting a local folder:
  DevTools saves network-served files to your local folder
  When you edit a file in DevTools → saved to local folder
  Chrome serves from your local override instead of the network
  PERSISTS across page reloads!

Use case: 
  "I want to test a CSS change on a production site without deploying"
  "I want to remove an ad script to test performance impact"
  
Network tab → right-click a response → Override content → same feature
```

---

## 6. Network Tab: Request/Response Deep Dive

The Network tab is the most important panel for debugging web performance and API issues.

### Toolbar Controls

```text
Record button (●):      Start/stop recording (default: on when tab opens)
Clear (⊘):              Clear all recorded requests
Filter bar:             Filter by URL, method, status, type (see below)
Preserve log:           Keep requests across page navigations (crucial for SPA debugging)
Disable cache:          Forces all requests to bypass browser cache (simulates first visit)
                        Only active while DevTools is open
Throttling dropdown:    Simulate slow networks (see throttling section)
Blocked URLs:           Mark specific URLs as blocked → see impact

Import/Export HAR:      Save all recorded requests as .har file for sharing
```

### Request List Columns

```text
Default columns:
  Name:         URL filename + status code badge
  Status:       HTTP status code (200, 404, 503, etc.)
  Type:         document, script, stylesheet, xhr, fetch, img, font, ws, etc.
  Initiator:    What triggered this request (file + line number)
  Size:         Transferred size / resource size (shows compression savings)
  Time:         Total request duration

Right-click column headers to add more:
  Method:       GET, POST, PUT, DELETE, etc.
  Domain:       Which domain served this resource
  Protocol:     http/1.1, h2 (HTTP/2), h3 (HTTP/3)
  Scheme:       https, http
  Priority:     Highest, High, Medium, Low (browser resource priority)
  Cache-Control: Response cache header value
  Content-Type: MIME type of response
  Response Headers: Any specific header value
  Waterfall:    Timing bars (enabled by default)
```

### Filtering Requests

```text
Filter bar syntax:
  Type text → matches URL substring
  method:GET, method:POST     → filter by HTTP method
  status-code:404             → only 404 responses
  larger-than:100k            → resources over 100KB
  -larger-than:10k            → resources under 10KB (negate with -)
  domain:example.com          → requests to that domain
  has-response-header:Cache-Control   → requests with that header
  is:blocked                  → blocked requests
  is:from-cache               → served from cache
  is:service-worker-intercepted  → handled by service worker
  
Type buttons (above the request list):
  All | Fetch/XHR | JS | CSS | Img | Media | Font | Doc | WS | Wasm | Manifest | Other
  
  Fetch/XHR: your API calls — most important for debugging
  Doc:       the HTML document
  JS:        all script files
  
Multi-select types: hold Cmd/Ctrl and click multiple type buttons
```

### Inspecting a Request

```text
Click any request row → details pane opens on the right (or bottom)

HEADERS tab:
  General:
    Request URL:       full URL (check for query params, typos)
    Request Method:    GET/POST/PUT/DELETE/PATCH
    Status Code:       200 OK / 404 Not Found / 500 Internal Server Error
                       Click the status code for MDN documentation
    Remote Address:    server IP + port (useful: is it hitting the right server?)
    Referrer Policy:   no-referrer / origin / strict-origin-when-cross-origin
    
  Response Headers:
    Content-Type:      application/json; charset=utf-8
    Content-Encoding:  gzip / br (brotli) — shows compression
    Cache-Control:     max-age=3600, no-cache, no-store, private, public
    ETag:              "abc123" (for conditional requests)
    Access-Control-Allow-Origin: * (CORS header)
    Strict-Transport-Security: max-age=31536000 (HSTS)
    
  Request Headers:
    Authorization:     Bearer token / API key (REDACTED in some cases)
    Content-Type:      application/json (for POST bodies)
    Accept:            what the client expects back
    Cookie:            sent cookies (click on them → Application tab)
    Origin:            for CORS preflight requests

PAYLOAD tab (POST/PUT requests):
  Query String Parameters:  parsed from URL ?key=value
  Request Payload:          JSON body / form data
                            Click "view parsed" / "view source" to toggle

PREVIEW tab:
  Formatted view of the response body
  JSON → collapsible tree (like a mini JSON viewer)
  Images → displayed inline
  HTML → rendered

RESPONSE tab:
  Raw response body text
  For JSON: useful to copy raw for pasting into external tools
  
TIMING tab (very important — see below):
  Breakdown of how long each phase took
  
COOKIES tab:
  Request cookies sent + response cookies received
  Click "Show filtered out cookies" to see blocked cookies
  
INITIATOR tab:
  Which script/line triggered this request
  Full call stack leading to the fetch/XHR
  Critical for: "what code is making this API call?"
```

### Timing Breakdown (Waterfall Details)

```text
Click a request → Timing tab:

Phases explained:

Queueing:
  Time spent waiting to be assigned a TCP connection
  Reasons: max 6 connections per origin (HTTP/1.1), low priority, main thread busy
  High queueing → too many requests, or wrong priority
  
Stalled:
  Request is ready but waiting for a connection slot or cache check
  Often grouped with Queueing in total wait time
  
DNS Lookup:
  Time to resolve domain name to IP address
  First visit: may take 20-120ms
  Subsequent visits: cached by OS (< 1ms)
  Long DNS → consider DNS prefetching: <link rel="dns-prefetch" href="//api.example.com">

Initial Connection (TCP):
  3-way TCP handshake time
  Depends on network latency (RTT) and server location
  
SSL:
  TLS handshake time (adds 1-2 RTTs for TLS 1.2, 0-1 RTT for TLS 1.3)
  HTTP/2 and HTTP/3 reduce this impact
  
Request Sent:
  Time for browser to send the request bytes to the server
  Usually < 1ms (outgoing data)
  
Waiting (TTFB — Time To First Byte):
  Time from sending request to receiving the FIRST byte of response
  This is server processing time + network transit time
  HIGH TTFB = server is slow (slow DB query, missing cache, compute-heavy)
  Target: < 200ms for API calls, < 600ms for HTML documents
  
Content Download:
  Time to download all response bytes
  Depends on: file size + network speed
  Slow content download = large file size or slow network

Total:
  Sum of all phases above
```

### Throttling: Simulate Slow Networks

```text
Network tab → Throttling dropdown (default: No throttling):

Built-in presets:
  Fast 3G:    1.5 Mbps down, 750 kbps up, 40ms latency
  Slow 3G:    780 kbps down, 330 kbps up, 100ms latency
  Offline:    no network (test PWA offline mode)
  
Custom throttling:
  Network conditions (More tools) → Add custom profile:
    Download:   e.g., 50000 Kbps
    Upload:     e.g., 20000 Kbps
    Latency:    e.g., 10ms

Combine with: Device emulation → CPU throttling (6x slowdown)
→ Simulates budget Android phone on 3G → how real users experience your site

Important: throttling is applied ONLY to network requests, not JS execution.
Use Performance tab's CPU throttling for JS slowdown simulation.
```

### Waterfall View

```text
The waterfall shows all requests as horizontal bars on a timeline.

Reading the waterfall:
  ┌─────────────────────────────────────────────────────────────────────┐
  │ HTML doc      ──────■                                               │ ← blocking
  │ style.css          ────────■                                        │ ← render-blocking
  │ main.js            ────────────────────■                            │ ← parser-blocking
  │ image1.jpg                             ──────────■                  │
  │ image2.jpg                             ──────────■                  │ ← parallel
  │ image3.jpg                             ──────────■                  │
  │ api/data                                         ──────────■        │ ← initiated by JS
  └─────────────────────────────────────────────────────────────────────┘
  
Key patterns to spot:
  Render-blocking CSS: loaded before JS can execute, blocks page render
  Parser-blocking JS:  <script> without async/defer, blocks HTML parsing
  Long chain (waterfall):  request B starts only after A finishes → dependency chain
  Wide bars:           slow downloads (large files or slow network)
  Lots of tiny requests: HTTP/1.1 queuing; switch to HTTP/2 for multiplexing
  
Blue vertical line:  DOMContentLoaded event
Red vertical line:   Load event (all resources loaded)
```

### Key Network Debugging Tricks

```text
1. Find what's causing a CORS error:
   → Filter by XHR/Fetch → click failing request → Headers tab
   → Check: is Access-Control-Allow-Origin present in response headers?
   → Check Preflight: look for OPTIONS request before the actual request

2. Find redirect chains:
   → Preserve log ON → navigate → look for 301/302 in status column
   → Click redirect request → Headers → Location header shows destination
   
3. Find what's setting a cookie:
   → Click any request → Cookies tab → see Set-Cookie response cookies
   
4. Compare cached vs non-cached:
   → Size column: "(from disk cache)" or "(from memory cache)" → 0 transfer time
   → Disable cache checkbox → forces fresh fetch every time

5. Find the real URL behind a redirect:
   → Click the 301/302 → Headers → Location

6. Copy request as cURL:
   → Right-click request → Copy → Copy as cURL
   → Paste in terminal → replays exact request with all headers/cookies
   → Invaluable for API debugging without browser

7. Replay a request:
   → Right-click request → Replay XHR (for XHR/Fetch only)
   
8. Check HTTP version:
   → Add Protocol column (right-click headers) → h2 = HTTP/2, h3 = HTTP/3
   → h2 = multiplexed (good), http/1.1 = 6 connection limit (may cause queueing)
   
9. Block a request to test fallback:
   → Right-click request → Block request URL / Block request domain
   → Reload page → see what breaks

10. Override response with local file:
    → Right-click request → Override content
    → Edit response in DevTools → change persists across reloads
```

---

## 7. Performance Tab: Profiling + Core Web Vitals

### Recording a Profile

```text
Two modes:

Mode 1: Record user interaction
  Click Record (●) → perform actions on page → click Stop (■)
  Use for: profiling a button click, form submit, animation, scroll

Mode 2: Reload and record page load
  Click "Start profiling and reload page" (↺●)
  Captures the full page load including network waterfall + JS execution
  Use for: diagnosing slow page loads, measuring LCP/CLS/FID

Settings (⚙ icon):
  CPU throttling:   4x or 6x slowdown → simulates mobile device
  Network:          Apply same throttles as Network tab
  Screenshots:      Capture screenshots at each frame (default: on)
```

### Anatomy of a Performance Recording

```text
┌──────────────────────────────────────────────────────────────────┐
│ OVERVIEW (summary bars)                                          │
│  CPU:        colored bar (yellow=scripting, purple=rendering)    │
│  Network:    blue/green bars for resource loading                │
│  Frames:     screenshots of what page looked like at each point  │
├──────────────────────────────────────────────────────────────────┤
│ TIMINGS (Core Web Vitals markers)                                │
│  DCL (DOMContentLoaded) │ L (Load) │ FCP │ LCP │ CLS │          │
├──────────────────────────────────────────────────────────────────┤
│ MAIN (flame chart — most important)                              │
│  Yellow = JavaScript execution                                   │
│  Purple = Rendering (Layout, Paint, Composite)                   │
│  Green  = Painting                                               │
│  Gray   = Tasks                                                  │
├──────────────────────────────────────────────────────────────────┤
│ THREAD POOL (Web Workers)                                        │
├──────────────────────────────────────────────────────────────────┤
│ RASTER / GPU                                                     │
├──────────────────────────────────────────────────────────────────┤
│ NETWORK (requests during profiling period)                       │
└──────────────────────────────────────────────────────────────────┘
```

### The Flame Chart (Main Thread)

```text
The flame chart shows the CALL STACK over time:
  X-axis:  time
  Y-axis:  call depth (bottom = caller, top = deepest call)
  Width:   how long a function took
  
Each colored rectangle = one function execution.
Stacked rectangles = function A called B called C...

Reading a flame chart:
  Wide bars at the top = functions that ran for a long time (expensive)
  Tall stacks = deep call chains
  Red triangles = "long task" warnings (> 50ms on main thread)
  
Long tasks (> 50ms):
  Block the main thread → user cannot interact (Input Delay)
  Causes janky scrolling, delayed button responses
  Causes FID/INP degradation
  
To diagnose a long task:
  Click on the red triangle → bottom panel shows Self Time, Total Time
  Look for the widest yellow blocks → that's where JS time is spent
  Drill down: hover over blocks to see function names and files
```

### Layout Thrashing (Forced Reflows)

```text
Layout (Reflow) = browser recalculates positions of all elements.
Very expensive, especially for large DOMs.

Layout thrashing = JavaScript alternately reads and writes layout properties,
                   forcing the browser to recalculate layout repeatedly.

Example of thrashing (BAD):
  for (let el of elements) {
    const height = el.offsetHeight;  // READ  → forces reflow
    el.style.height = height + 10 + 'px';  // WRITE → invalidates layout
  }
  // Each iteration triggers a full layout → O(n) layouts instead of 1

Fix: batch reads before writes:
  const heights = elements.map(el => el.offsetHeight);  // all READs
  elements.forEach((el, i) => el.style.height = heights[i] + 10 + 'px'); // all WRITEs

In flame chart: look for alternating purple (Layout) blocks
```

---

## 8. Memory Tab: Heap Snapshots + Leak Detection

### When To Use the Memory Tab

```text
Symptoms of a memory leak:
  - Page gets slower over time without reloading
  - Browser tab uses more and more RAM
  - Scrolling/interactions eventually become unresponsive
  - Chrome's task manager shows growing tab memory
  
Memory types:
  JS Heap:      Objects created by JavaScript (most common leak location)
  Documents:    DOM nodes
  Workers:      Web Worker memory
  GPU Memory:   Video/canvas/WebGL
```

### Tool 1: Heap Snapshot

```text
Memory tab → Heap snapshot → Take snapshot
Takes a point-in-time photo of the JavaScript heap.

After taking:
  Summary view: objects grouped by constructor name
  Comparison view: compare two snapshots (before/after action)
  Containment view: tree view from GC roots → what's keeping objects alive

Columns:
  Constructor:     JavaScript class/constructor name
  Distance:        edges from GC root (shorter = strongly retained)
  Shallow Size:    memory this object itself uses (not including children)
  Retained Size:   memory freed if this object were garbage collected
                   (this object + everything only reachable through it)
  
Finding leaks:
  Step 1: Load page → Snapshot 1
  Step 2: Do the action you suspect leaks (open/close modal 10x, navigate 10x)
  Step 3: Snapshot 2
  Step 4: Change view to "Comparison" → Sort by "# Delta" (new objects added)
  Step 5: Large numbers of new Detached DOM Trees or event listeners = leak
```

### Tool 2: Allocation Instrumentation on Timeline

```text
Memory tab → Allocation instrumentation on timeline → Start
Perform actions → Stop

Shows a timeline of object allocations:
  Blue bars = objects allocated (still live)
  Gray bars = objects garbage collected
  
Click any blue bar → see what objects were allocated at that time
→ find which code path is creating retained objects
```

### Common Memory Leak Patterns

```text
1. Detached DOM Nodes:
   Elements removed from the DOM but still referenced in JavaScript.
   let savedRef = document.getElementById('modal');
   document.body.removeChild(savedRef);
   // savedRef still holds the element → it cannot be GC'd
   
   Fix: set savedRef = null after removing the element.

2. Forgotten Event Listeners:
   component.addEventListener('click', handler);
   // component is removed but handler still references it
   
   Fix: removeEventListener in cleanup / component unmount:
   component.removeEventListener('click', handler);
   // Or: use AbortController:
   const controller = new AbortController();
   element.addEventListener('click', handler, { signal: controller.signal });
   controller.abort();  // removes all listeners using this signal

3. Closures Capturing Large Objects:
   function createHandler(data) {    // data is 10MB
     return function() {
       console.log(data.id);         // closure captures ALL of data, not just .id
     };
   }
   
   Fix: capture only what you need:
   function createHandler(data) {
     const id = data.id;   // extract only the small value
     return function() { console.log(id); };
   }

4. Timers Not Cleared:
   const id = setInterval(updateChart, 1000);
   // component is destroyed but interval keeps running
   // callback captures component reference → never GC'd
   
   Fix: clearInterval(id) in cleanup

5. Unbounded Caches:
   const cache = {};
   cache[userId] = heavyData;  // grows forever
   
   Fix: use WeakMap (keys can be GC'd), or LRU cache with size limit
```

---

## 9. Application Tab: Storage, Cookies, Service Workers

### Local Storage

```text
Application → Storage → Local Storage → [origin]

Shows all key-value pairs for the current origin.
Max size: ~5-10MB (varies by browser)
Persists: until explicitly cleared (survives tab close, browser close)
Scope: same origin only

Operations in DevTools:
  Double-click a value → edit it live
  Click a row → Delete (⊖) button → remove that entry
  Right-click → Clear → removes all entries for this origin
  
Common debugging use:
  "My feature flag isn't taking effect" → check localStorage.featureFlags
  "User appears logged out" → check localStorage.authToken
  "Dark mode keeps resetting" → check localStorage.theme
  
In Console:
  localStorage.getItem('key')
  localStorage.setItem('key', 'value')
  localStorage.removeItem('key')
  localStorage.clear()
  Object.entries(localStorage)  // all entries as array
```

### Session Storage

```text
Application → Storage → Session Storage → [origin]

Same API as localStorage, but:
  Persists: only for the browser TAB session
  Cleared: when the tab is closed (not just navigated away)
  Scope: same origin AND same tab (not shared with other tabs!)

Use case:
  Storing temporary state (wizard step, temporary form data, tab-specific filters)
  
Debugging: "my multi-step form data is gone after opening a new tab" → sessionStorage
```

### Cookies

```text
Application → Storage → Cookies → [domain]

Table columns:
  Name:           cookie name
  Value:          cookie value (may be a JWT, session ID, etc.)
  Domain:         which domain/subdomain receives the cookie
  Path:           URL path scope (usually /)
  Expires:        when the cookie expires (session = when tab closes)
  Size:           bytes
  HttpOnly:       ✓ = not accessible via document.cookie (JS cannot read it)
  Secure:         ✓ = only sent over HTTPS
  SameSite:       Strict / Lax / None — CSRF protection setting
  Priority:       Low / Medium / High (browser decides which cookies to drop if over limit)
  Partition Key:  for partitioned cookies (Storage Partitioning / CHIPS)
  
Cookie flags explained:
  HttpOnly: prevents JavaScript from reading the cookie → protects against XSS
  Secure:   only transmitted over HTTPS → prevents network eavesdropping
  SameSite=Strict: cookie never sent cross-site → prevents CSRF entirely
  SameSite=Lax:    sent cross-site only for top-level GET navigations (default in modern browsers)
  SameSite=None:   sent cross-site (requires Secure flag) → for third-party cookies
  
Debugging cookies:
  "My auth cookie isn't being sent to the API" →
    Check: is SameSite=Strict blocking cross-origin API calls?
    Check: is Secure set but you're testing on http://?
    Check: is Domain set correctly (.example.com vs example.com)?
    Check: Network tab → request → Cookies tab → "Show filtered out cookies" → shows WHY cookies were blocked
    
  "My cookie keeps disappearing" →
    Check Expires column: session cookies disappear on tab close
    Check that HttpOnly is set so JS cannot delete it
```

### IndexedDB

```text
Application → Storage → IndexedDB → [database name] → [object store]

A full key-value database in the browser.
Max size: can be GBs (depends on available disk space)
Persists: until explicitly cleared
Supports: indexes, transactions, ranges, complex queries

In DevTools:
  Navigate the database structure: database → object store → records
  Click any record to see its value
  Right-click → Clear object store → delete all records
  Right-click → Delete database
  
Common uses: offline-capable apps (PWAs), draft saving, caching API responses
```

### Service Workers

```text
Application → Service Workers

Shows all registered service workers for the current origin.

Status indicators:
  ● (green):    active and running
  ● (yellow):   waiting to activate (new version waiting for tabs to close)
  ● (red):      stopped / errored
  
Controls:
  Update:       force re-fetch and update the service worker script
  Unregister:   remove the service worker entirely
  Offline:      checkbox to simulate offline mode
  Update on reload: bypass service worker cache on each page load (dev mode)
  Bypass for network: route all requests directly to network, skipping SW cache
  
Push notifications testing:
  Enter a payload → Push button → tests your push notification handler
  
Sync testing:
  Enter a tag → Sync button → tests Background Sync

Debugging service workers:
  "My page still shows old code after deploying" →
    Service worker is caching old files
    Solution: Application → Service Workers → Unregister, then hard reload
    OR: temporarily enable "Update on reload" in DevTools
    
  "My service worker isn't activating" →
    Another tab with the old service worker is open
    Solution: close all other tabs for this origin, or click "skipWaiting"
```

### Cache Storage

```text
Application → Cache Storage → [cache name]

Shows all caches created by your Service Worker (via Cache API).
Each cache can have a list of cached request/response pairs.

Click any cached entry → shows the cached response headers + body

Use:
  "Why is my service worker returning stale data?" →
    Check what's in Cache Storage → compare URL and response date
    Right-click entry → Delete to clear specific entry
    
  "My PWA isn't working offline" →
    Check Cache Storage: are the shell HTML/CSS/JS files cached?
    Check Service Worker: is it intercepting fetch events and responding from cache?
```

### Web App Manifest

```text
Application → Manifest

Shows the parsed Web App Manifest (manifest.json).

Displays:
  Identity:     name, short_name, description
  Presentation: display (standalone/fullscreen/minimal-ui), orientation
  Icons:        all icons with sizes, if they're maskable
  Protocol handlers, Screenshots, etc.
  Installability: issues preventing the PWA from being installable
  
Common issues:
  "Add to Home Screen prompt isn't showing" →
    Check Manifest section → Installability issues:
      Missing 192px and 512px icons?
      HTTPS not being used?
      Service Worker not registered?
      display not set to standalone?
```

### Background Services

```text
Application → Background services:
  Background Fetch:   track large background download/upload operations
  Background Sync:    track sync events (retry failed requests when online)
  Notifications:      log all notification requests/responses
  Payment Handler:    Payment Request API debugging
  Periodic Background Sync: scheduled sync events
  Push Messaging:     incoming push messages + payloads
  
Each section lets you:
  Record 3 days of events (even when DevTools is closed)
  Replay/inspect each event
```

---

## 10. Security Tab

```text
Security tab shows overall security state of the current page.

Overview pane:
  This page is secure (green padlock) / Not Secure / Mixed Content
  Certificate validity
  Connection: protocol (TLS 1.3), key exchange (X25519), cipher (AES_256_GCM)
  
Origins pane:
  Lists every origin that served content
  Click any origin → see its certificate, connection, and content details
  
Certificate details:
  Click "View certificate" → shows full X.509 certificate
  Same info as clicking the padlock in the address bar

Mixed content:
  "Mixed content" = HTTPS page loading HTTP resources
  Passive mixed content: images/video/audio loaded over HTTP (browser loads but warns)
  Active mixed content:  scripts/styles/XHR over HTTP (browser blocks entirely!)
  
  In Security tab: lists all mixed content URLs
  In Console: Blocked loading mixed active content error
  Fix: change all resource URLs to HTTPS (or use protocol-relative: //example.com/...)
  
Common debugging:
  "My page shows 'Not Secure'" →
    Check Security tab → usually: mixed content (HTTP resource on HTTPS page)
    OR: page served over HTTP entirely
    
  "My auth cookie isn't being sent" →
    Check if cookie has Secure flag AND you're on HTTP
    Check Security tab → is the connection actually HTTPS?
```

---

## 11. Lighthouse: Automated Audits

### Running Lighthouse

```text
Lighthouse tab → Configure:
  Categories:  Performance, Accessibility, Best Practices, SEO, PWA
  Device:      Mobile (default) or Desktop
  Throttle:    Simulated (uses data to estimate) or Devtools (actual throttle)
  
Click "Analyze page load" → wait 30-60 seconds

Tip: Always run Lighthouse in Incognito mode (Chrome extensions affect scores).
Tip: Close other tabs (prevents CPU competition).
Tip: Run 3 times and take the median (scores vary 5-10 points run-to-run).
```

### Score Breakdown

```text
Each category scored 0-100:
  0-49:   Poor (red)
  50-89:  Needs Improvement (orange)
  90-100: Good (green)

Performance score is weighted average of 6 metrics:
  FCP  (First Contentful Paint):   15% weight
  LCP  (Largest Contentful Paint): 25% weight
  TBT  (Total Blocking Time):      30% weight
  CLS  (Cumulative Layout Shift):  15% weight
  SI   (Speed Index):               10% weight
  TTI  (Time to Interactive):       5% weight  (being phased out)
```

### Opportunities vs Diagnostics

```text
Opportunities:
  Specific actionable improvements with estimated savings
  Examples:
    "Eliminate render-blocking resources" → savings: 1.2s
    "Serve images in modern formats (WebP/AVIF)" → savings: 340KB
    "Properly size images" → savings: 190KB
    "Reduce unused JavaScript" → savings: 680KB
    "Enable text compression (gzip/brotli)" → savings: 220KB
  
  Each item is expandable: shows WHICH resources are the problem

Diagnostics:
  Issues that don't have a direct time/size savings but indicate problems
  Examples:
    "Avoid an excessive DOM size" → 5,842 elements
    "Serve static assets with efficient cache policy" → 12 resources
    "Avoid chaining critical requests"
    "Avoid large layout shifts"
    "Main-thread work breakdown" (table of scripting, rendering, painting time)
    
Passed Audits:
  Collapsed section showing what's already good → quick confidence check
```

---

## 12. Core Web Vitals Deep Dive

Core Web Vitals (CWV) are Google's metrics for page experience. They directly affect Google Search rankings since 2021.

### The Three Core Web Vitals

```text
LCP  — Largest Contentful Paint   → Loading performance
INP  — Interaction to Next Paint  → Interactivity (replaced FID in 2024)
CLS  — Cumulative Layout Shift    → Visual stability
```

---

### LCP (Largest Contentful Paint)

**What it measures:** Time from page navigation start until the largest image or text block visible in the viewport is rendered.

```text
Good:          ≤ 2.5s
Needs work:    2.5s – 4.0s
Poor:          > 4.0s

What counts as "largest element"?
  - <img> elements
  - <image> inside SVG
  - <video> poster image
  - Block-level element with background-image (CSS)
  - Block-level elements containing text (most common: hero <h1>, hero paragraph)
  
The "largest" is measured by visible area in the viewport at time of render.
LCP is reported once: the element that was largest at the moment it rendered.
```

**How to find your LCP element:**

```javascript
// In Console (or snippets):
new PerformanceObserver((list) => {
  for (const entry of list.getEntries()) {
    console.log('LCP element:', entry.element, 'time:', entry.startTime);
  }
}).observe({ type: 'largest-contentful-paint', buffered: true });
```

**LCP Causes and Fixes:**

```text
Slow server response (TTFB > 600ms):
  → Cache HTML at CDN (Edge Caching)
  → Optimize database queries, enable connection pooling
  → Use streaming SSR (Next.js streaming, React 18)
  
Render-blocking CSS/JS:
  → <link rel="stylesheet"> for critical CSS only (inline it)
  → Move non-critical CSS to lazy-load
  → Add async or defer to scripts: <script defer src="app.js">
  → Use <link rel="preload"> for critical resources:
    <link rel="preload" href="hero.jpg" as="image" fetchpriority="high">
  
Slow image load (most common LCP issue):
  → Serve images via CDN (low latency)
  → Use modern formats: WebP (30% smaller than JPEG) or AVIF (50% smaller)
  → Proper srcset for responsive images:
    <img src="hero-800.webp" srcset="hero-400.webp 400w, hero-800.webp 800w" sizes="100vw">
  → Add fetchpriority="high" to the LCP image (no lazy loading on hero!)
  → Never: loading="lazy" on the LCP image
  → Preconnect to image CDN origin:
    <link rel="preconnect" href="https://cdn.example.com">

Client-side rendering (SPA with empty <div>):
  → SSR (Next.js / Nuxt.js) → HTML arrives with content
  → SSG for static pages
  → Streaming SSR for dynamic pages
```

---

### CLS (Cumulative Layout Shift)

**What it measures:** The sum of all unexpected layout shifts on the page during its entire life. A layout shift happens when a visible element changes its starting position between frames.

```text
Good:          ≤ 0.1
Needs work:    0.1 – 0.25
Poor:          > 0.25

Score formula:
  Impact fraction (how much viewport area moved) × Distance fraction (how far it moved)
  e.g., element moves 25% of viewport area by 50% of viewport height = 0.125 CLS
  
Cumulative: all shifts during the entire session are summed (except within 500ms of user input)
```

**Common CLS Causes and Fixes:**

```text
1. Images without dimensions:
   BAD:  <img src="hero.jpg">
   → Browser doesn't know image size → allocates 0px → shifts when image loads
   
   FIX:  <img src="hero.jpg" width="800" height="600">
         OR CSS: img { aspect-ratio: 4/3; width: 100%; }
   → Browser pre-allocates the space before image loads

2. Ads, embeds, iframes without reserved space:
   FIX:  Reserve space: min-height: 250px on the ad container
         Use aspect-ratio CSS property

3. Web Fonts causing FOUT/FOIT:
   FOUT: Flash of Unstyled Text (fallback font renders, then shifts when web font loads)
   FIX:  font-display: optional → show fallback only if font loads instantly
         font-display: swap    → show fallback, swap (allows FOUT shift)
         Use size-adjust to make fallback font match web font dimensions:
         @font-face {
           font-family: 'MyFont';
           src: url('myfont.woff2');
           size-adjust: 98%;          ← make fallback match closely
         }
         → Or: preload fonts:
         <link rel="preload" href="myfont.woff2" as="font" crossorigin>

4. Dynamically injected content:
   → Modal/banner/cookie notice slides in and pushes content down
   FIX:  Overlay (position: fixed/absolute) elements don't cause CLS
         If content must push → load it before rendering starts (SSR)

5. Animations triggering layout:
   FIX:  Only animate transform and opacity (GPU-composited, no layout)
         Never animate: top, left, width, height, margin, padding
         Use: transform: translateX(10px) instead of left: 10px
```

**Finding CLS elements:**

```javascript
// Log all layout shifts and their culprit elements
new PerformanceObserver((list) => {
  for (const entry of list.getEntries()) {
    if (!entry.hadRecentInput) {
      console.log('Layout shift:', entry.value);
      console.log('Shifted elements:', entry.sources?.map(s => s.node));
    }
  }
}).observe({ type: 'layout-shift', buffered: true });
```

In Performance tab: CLS marker appears on the Timings row. Click it → bottom panel shows the shifted elements.

---

### INP (Interaction to Next Paint)

**What it measures:** The 98th percentile latency across all user interactions (clicks, taps, key presses) from input start to the next frame painted. Replaced FID in March 2024.

```text
Good:          ≤ 200ms
Needs work:    200ms – 500ms
Poor:          > 500ms

Components of INP:
  Input delay:       time from user action to browser starting to process it
                     Cause: main thread was busy (long task running)
  Processing time:   time to run the event handler
                     Cause: expensive JavaScript in the handler
  Presentation delay:time for browser to paint after handler finishes
                     Cause: too much layout/paint work triggered by handler
```

**Measuring INP:**

```javascript
// Using web-vitals library
import { onINP } from 'web-vitals';
onINP(({ value, entries, attribution }) => {
  console.log('INP:', value, 'ms');
  console.log('Worst interaction:', attribution.interactionTarget);
  console.log('Event type:', attribution.eventType);
});

// Manual observation
new PerformanceObserver((list) => {
  for (const entry of list.getEntries()) {
    if (entry.duration > 200) {
      console.log('Slow interaction:', entry.name, entry.duration, 'ms');
    }
  }
}).observe({ type: 'event', durationThreshold: 100, buffered: true });
```

**INP Fixes:**

```text
Long tasks blocking input:
  → Break up long tasks with setTimeout(fn, 0) or scheduler.yield()
  → Move heavy processing to Web Workers
  → Use isInputPending() to yield early if user interacted

Expensive event handlers:
  → Debounce rapid events (scroll, resize, keypress)
  → Defer non-critical work: requestIdleCallback() or requestAnimationFrame()
  → Avoid synchronous DOM reads inside animation frames
  → Use CSS animations/transitions instead of JS-driven frame loops

React-specific:
  → Use React.lazy() + Suspense to reduce bundle size
  → Use useTransition() / startTransition() to mark state updates as non-urgent
  → Avoid large re-renders in click handlers (React DevTools profiler helps)
```

---

### Supporting Metrics

```text
FCP (First Contentful Paint):
  Time until ANY content (text, image, canvas) is first painted.
  Good: ≤ 1.8s | Needs work: 1.8-3s | Poor: > 3s
  
  Fix: same as LCP (render-blocking resources, server speed)
  Difference from LCP: FCP = first bit of content; LCP = largest piece

TTFB (Time To First Byte):
  Time from navigation start to first byte of HTML response.
  Good: ≤ 800ms | Poor: > 1800ms
  
  Directly impacts FCP and LCP.
  Fix: CDN caching, faster server, streaming HTML response

TBT (Total Blocking Time):
  Sum of all "blocking time" in long tasks (> 50ms) between FCP and TTI.
  "Blocking time" = time over 50ms that each long task runs.
  
  Good: ≤ 200ms
  Proxy metric for INP (used in lab tools like Lighthouse because INP requires interaction)
  Fix: break up long tasks

FID (First Input Delay) — DEPRECATED March 2024:
  Replaced by INP. FID measured only the FIRST interaction; INP measures all.
```

---

## 13. Responsive Design Mode

### Enabling Device Emulation

```text
Click the device toggle icon (📱) in the DevTools toolbar, OR:
  Cmd+Shift+M (Mac) / Ctrl+Shift+M (Win)
  
The page is now viewed as if on a mobile device.
```

### Device Toolbar Controls

```text
Dimensions dropdown:
  Preset devices: iPhone 14 Pro, Pixel 7, iPad Pro, Galaxy S20, Surface Pro 7...
  Custom: type in your own Width × Height
  Responsive: drag the edge handles to resize freely
  
Rotate button: switch portrait ↔ landscape

DPR (Device Pixel Ratio): 
  Mobile screens have high DPR (2x, 3x)
  1x = standard desktop (1 CSS pixel = 1 physical pixel)
  3x = iPhone 14 Pro (1 CSS pixel = 3 physical pixels)
  → Test if your @2x/@3x images are being served correctly
  
Network throttle: apply per-device network speed

CPU throttle: apply per-device CPU slowdown (simulates lower-end phones)
```

### Media Queries

```text
In device emulation mode:
  Click "..." → Show media queries
  → Colored bars appear at the top showing breakpoint ranges
  → Click any bar to jump to that viewport size
  
Useful for:
  "At what width does my layout break?"
  "Is my 768px breakpoint actually triggering?"
```

### Remote Debugging (Real Device)

```text
Test on a REAL Android device (far better than emulation):
  1. Enable Developer Options on Android phone
  2. Enable USB Debugging
  3. Connect phone to laptop via USB
  4. Chrome desktop: chrome://inspect → Devices → your phone appears
  5. Find your tab in the list → Inspect → DevTools opens connected to phone's browser
  → All DevTools features work on the real device
  → Network tab, Performance, Console, etc. — real-device performance numbers
```

---

## 14. Advanced Features

### Coverage Tab

```text
More tools → Coverage
Click record (●) → interact with page → stop

Shows: which CSS and JavaScript bytes were actually EXECUTED vs LOADED.
Red bars:   unused code
Green bars: used code

Example result:
  bundle.js    1.2MB loaded    320KB used   (73% UNUSED)
  styles.css   450KB loaded    89KB used    (80% UNUSED)
  
This is why code splitting is important:
  Webpack/Vite can split bundles → load only what's needed per page
  Dynamic imports: import('./heavyModule').then(...)
  React.lazy(): lazy-load components until they're rendered
  
Export coverage: right-click → Export as CSV → analyze in spreadsheet
```

### Request Blocking

```text
Network tab → right-click any request → Block request URL / domain
OR:
  More tools → Request Blocking → Add pattern (supports wildcards)
  e.g.: *.google-analytics.com  blocks all GA requests

Use for:
  "If I block this analytics script, how much faster is the page?"
  "If I block this third-party font, what fallback font shows?"
  "Does the app still work without this API endpoint?" (resilience testing)
  
Patterns support * wildcard, e.g.: */api/recommendations*
```

### Rendering Tab

```text
More tools → Rendering

Frame Rate Meter (FPS):
  Shows live frames per second counter overlaid on page
  Jank visible as drops below 60 FPS
  
Paint Flashing:
  Green flash = area being repainted
  Helps identify: "why is my background being repainted on scroll?"
  
Layer Borders:
  Orange lines = composited layers
  Useful for verifying transform/will-change creates a layer

Layout Shift Regions:
  Blue highlight = elements that just shifted (shifted in last frame)
  Makes CLS issues visually obvious

Scrolling Performance Issues:
  Highlights non-passive scroll event listeners (can block scrolling thread)
  
Core Web Vitals Overlay:
  Shows LCP, CLS, INP badges overlaid on the live page
  
Emulate CSS media:
  Force prefers-color-scheme: dark/light (test dark mode without OS setting)
  Force prefers-reduced-motion: reduce (test reduced animation UX)
  Force print media: test print stylesheets
  Emulate focused page: test focus-visible styles
```

### Sensors Tab

```text
More tools → Sensors

Geolocation:
  Set custom latitude/longitude → test "get my location" features
  Preset locations: Tokyo, San Francisco, London, Mumbai, Sydney
  Simulate "Location unavailable" error
  
Device Orientation:
  Simulate gyroscope/accelerometer
  α (alpha), β (beta), γ (gamma) rotation axes
  Test: 3D map rotation, device-tilt-based UI

Touch:
  Force touch instead of pointer events
  (Also available in device emulation mode)
  
Idle detection:
  Override idle state: active/idle/locked
  Test Idle Detection API
```

### WebAuthn Tab

```text
More tools → WebAuthn

Create a virtual authenticator (software passkey device)
Test Web Authentication (WebAuthn) flows without physical hardware keys
  
Options:
  Authenticator transport: USB, NFC, BLE, Internal
  Protocol: CTAP2 / U2F
  Resident key / User verification settings
  
Useful for: testing passkey registration and authentication flows end-to-end
```

### Performance Insights Panel

```text
Performance Insights (alongside Performance tab):
  A more guided version of the Performance tab
  Automatically highlights: LCP element, CLS clusters, long tasks, render blocking
  Provides specific recommendations alongside the trace
  
Good for: developers less familiar with raw flame chart reading
```

---

## 15. Real-World Debugging Scenarios

### Scenario 1: "The page loads but feels slow"

```text
Step 1: Open DevTools → Performance tab
Step 2: Record → reload page (Cmd+Shift+R) → stop after load

Diagnose:
  → Check Timings row: where is LCP? Is it > 2.5s?
  → Check Main thread: are there Long Tasks (red triangles)?
  → Look at Network section: is there a dependency chain? (A loads, then B starts)
  
Step 3: Network tab with cache disabled
  → Sort by Time column → which resource takes longest?
  → Look at TTFB (Waiting bar in timing): > 200ms → slow server
  → Look at Content Download: > 500ms for JS → large bundles

Step 4: Lighthouse
  → Run audit → check Opportunities: render-blocking, unused JS, large images
  
Common findings:
  3MB uncompressed JavaScript bundle → enable gzip/brotli compression
  Hero image is 2MB JPEG → convert to WebP, add width/height attributes
  Render-blocking Google Fonts → add font-display: swap, preload fonts
  3rd party scripts (chat widget, analytics) → defer or async load them
```

### Scenario 2: "Users report layout jumping on page load"

```text
Step 1: Open Performance tab → enable Screenshots → reload
Step 2: Scrub through the timeline → look for blue CLS markers
Step 3: Click a CLS marker → bottom panel shows shifted elements

OR use Rendering tab → Layout Shift Regions (live visualization)

OR in Console:
  new PerformanceObserver((list) => {
    list.getEntries().forEach(entry => {
      console.log('CLS:', entry.value, entry.sources?.map(s => s.node));
    });
  }).observe({ type: 'layout-shift', buffered: true });

Common causes:
  Image without width/height → fix: add dimensions or aspect-ratio CSS
  Cookie consent banner pushed content down → fix: position: fixed overlay
  Web font FOUT → fix: font-display: optional or preload + size-adjust
```

### Scenario 3: "API call returns 401 randomly"

```text
Network tab → filter: XHR/Fetch → Preserve log ON

Reproduce the 401:
  Click the 401 request → Headers tab
    → Check Request Headers: Authorization header present? Correct value?
    → Check Request URL: correct endpoint? Correct query params?
  
  Compare a working 200 request with the failing 401:
    → Right-click working request → Copy → Copy as cURL
    → Right-click failing request → Copy → Copy as cURL
    → Diff them in terminal → find the difference

Common cause: auth token expired
  → Application tab → Cookies → check token cookie expiry
  → Application tab → Local Storage → check stored token timestamp
  
Console:
  → Check for "401 Unauthorized" logged with token info
  → Check if refresh token logic is firing (monitor('/api/auth/refresh'))
```

### Scenario 4: "Button click is laggy / feels unresponsive"

```text
Step 1: Performance tab → Record → click the button → Stop

Step 2: Find the click event handler in flame chart (yellow bars after the click)
  → Click the widest bar to see function name
  → Bottom panel shows: Function, File, Line, Self Time, Total Time

Step 3: Check for long tasks (red triangle)
  → If a long task runs BEFORE the click is processed → Input Delay
  → If a long task runs DURING the handler → Processing Time

Common fixes:
  Heavy synchronous loop in click handler → break into chunks with requestIdleCallback
  Synchronous fetch (rare in modern code) → always use async/await
  React: expensive re-render triggered by click → use React.memo, useMemo, useTransition
  Reading layout properties in handler → batching reads

Test INP specifically:
  More tools → Performance Insights → click button → see INP report
```

### Scenario 5: "Memory usage keeps growing"

```text
Step 1: Open Task Manager (DevTools hidden feature):
  Shift+Esc → Chrome's task manager → watch your tab's memory

Step 2: Memory tab → Record Allocation Timeline → reproduce leak action 10x → stop
  → Look for growing blue bars that never turn gray (never GC'd)

Step 3: Memory tab → Take two snapshots:
  Snapshot 1 → do leak action 10x → Snapshot 2
  Comparison view → sort by "# Delta" → what constructor grew?

Step 4: Click the constructor with biggest delta → bottom panel shows all instances
  → Click an instance → right panel shows Retainer tree
  → Read up: what root object is keeping this alive?

Common findings:
  (detached) HTMLDivElement × 234 → DOM nodes held by JS variable → set to null
  EventListener × 800 growing → forgot removeEventListener → add cleanup
  Closure × 1200 growing → large data captured in closure → extract small value only
```

### Scenario 6: "CORS error on API call"

```text
Console shows:
  Access to fetch at 'https://api.example.com/data' from origin 'https://mysite.com'
  has been blocked by CORS policy: No 'Access-Control-Allow-Origin' header is present.

Network tab → click the failing request → Headers tab:
  → Check: is Access-Control-Allow-Origin in Response Headers? If absent → server config issue
  → Check: is there an OPTIONS preflight request before the actual request?
    If preflight 200 but actual request 403 → CORS allowed for preflight but blocked at actual

What to check:
  Server must respond with:
    Access-Control-Allow-Origin: https://mysite.com (or *)
    Access-Control-Allow-Methods: GET, POST, PUT, DELETE
    Access-Control-Allow-Headers: Content-Type, Authorization
    Access-Control-Max-Age: 86400 (cache preflight for 24h)
  
  Common mistake: CORS configured for http:// but site is https://
  Common mistake: Trailing slash difference: mysite.com vs mysite.com/
  Common mistake: API gateway/load balancer stripping CORS headers before they reach client
  
  NOT a fix: using a proxy in development (webpack devServer.proxy) — masks the real issue
```

---

## 16. Interview Q&A: Beginner to Pro

### Beginner

**Q: What is the difference between the DOM and the HTML source?**

The HTML source is the static text the server returned. The DOM (Document Object Model) is the live, parsed, in-memory representation of that HTML. JavaScript can modify the DOM — adding elements, changing attributes, removing nodes — without changing the original source. When you view source (Cmd+U), you see the original HTML. When you use Elements tab, you see the current DOM state. If React, Vue, or Angular has rendered components, Elements shows the fully hydrated DOM.

---

**Q: How do you debug a 404 Not Found error on a page?**

Open Network tab → filter by status code 404 (type `status-code:404` in filter). Click the failing request → check the full URL in Request URL (look for typos, missing path segments, wrong environment base URL). Check Initiator tab → see which code triggered the request. Look at the URL — often a relative path is resolving from the wrong base URL (SPA routing issue), or an environment variable is wrong.

---

**Q: What is the difference between local storage and session storage?**

Both are key-value string stores available in Application tab. localStorage persists indefinitely (survives browser close) and is shared across all tabs of the same origin. sessionStorage exists only for the current tab session and is cleared when the tab closes; it's not shared with other tabs even on the same origin. Both have ~5-10MB limits and are accessible via JavaScript on the same origin.

---

### Intermediate

**Q: What is TTFB and how do you measure it in DevTools?**

TTFB (Time To First Byte) is the time from the browser sending the request to receiving the first byte of the server response — essentially server processing time + network round-trip time. In the Network tab: click any request → Timing tab → "Waiting (TTFB)" shows this value. Lighthouse also measures it and flags it if > 600ms. High TTFB means slow server (slow DB, no caching, heavy compute) — the fix is server-side: add CDN caching, optimize database queries, use streaming HTML responses.

---

**Q: How do you find and fix a render-blocking resource?**

Open Lighthouse → look for "Eliminate render-blocking resources" opportunity. Click to expand → shows which CSS/JS files are render-blocking. In Network tab: these appear early in the waterfall as scripts loaded synchronously in `<head>`. Fix: add `async` or `defer` to `<script>` tags (defer preserves order, async doesn't); for CSS, inline critical CSS in `<style>` and lazy-load non-critical CSS using `media="print"` then swap; use `<link rel="preload">` for the truly critical path.

---

**Q: How do you identify a memory leak using DevTools?**

Take a heap snapshot in the Memory tab before and after repeatedly performing the suspected leaking action (open/close modal 10× or navigate routes 10×). In the Comparison view, sort by "# Delta" — constructor types with large positive deltas added more objects than were garbage collected. Click any suspiciously growing constructor (often "Detached HTMLElement" or "EventListener") → expand instances → look at the Retainer column to trace which variable is preventing garbage collection. Common causes: DOM references kept after element removal, event listeners not cleaned up, closures capturing large objects.

---

**Q: What is CLS and how do you debug layout shifts?**

CLS (Cumulative Layout Shift) measures unexpected movement of visible content. Score > 0.1 is "needs improvement." Debug with: Performance tab → record page load → look for blue CLS markers on the Timings row; click a marker → bottom panel shows which elements shifted. Or use More Tools → Rendering → "Layout Shift Regions" for live highlights. Or use `PerformanceObserver` with `{ type: 'layout-shift' }` in the Console. Common causes: images without width/height attributes (fix: always specify dimensions), dynamic content injected above existing content (fix: reserve space), web font FOUT (fix: font-display: optional + preload).

---

### Senior / Pro

**Q: Walk me through diagnosing a page with poor INP.**

Start with Performance tab → record → click the slow button → stop. Find the event handler in the flame chart: look for a wide yellow bar (JavaScript) immediately after the interaction timestamp. Check if there's a long task BEFORE the click marker (input delay) — caused by another task running on the main thread. Check the event handler's Self Time vs Total Time to isolate which callee is expensive. In Performance Insights panel → click the INP entry → attribution shows: event type, target element, input delay, processing time, presentation delay. For React apps: check if the handler triggers a full re-render with no batching — use `useTransition` to mark state updates as interruptible. If processing can't be made faster, move heavy work to a Web Worker and communicate back via `postMessage`.

---

**Q: How would you use DevTools to audit a page's Core Web Vitals and produce an action plan?**

1. Open an Incognito window (no extensions), navigate to the page. Open DevTools → Performance → record page load with CPU 4x throttle + Fast 3G.
2. Check Timings row: identify LCP time and element (click the LCP marker). If > 2.5s: look at Network waterfall — is the LCP image discovered late (dependency chain)? Add `<link rel="preload" fetchpriority="high">`. Is TTFB high? → CDN caching.
3. Check CLS markers: click each to see shifted elements. Add `width` + `height` to images, reserve space for injected content.
4. Check long tasks (red triangles) before interactions end → TBT. Look at the widest yellow blocks → which scripts? → code split with dynamic imports.
5. Run Lighthouse for quantified "Opportunities" with estimated savings. Export report as JSON for CI/CD comparison.
6. For field data vs lab data divergence: check Chrome UX Report (CrUX) — PageSpeed Insights shows real-user 75th percentile CWV. Field data may differ due to caching, device diversity, and real interaction patterns.

---

**Q: A production SPA has a growing memory leak reported by users. How do you find it with DevTools remotely?**

For field data: add `window.performance.memory.usedJSHeapSize` to your analytics on each route transition — alert when it crosses a threshold. For investigation in DevTools: reproduce the leak scenario in a controlled session. Use Memory tab → Allocation instrumentation on timeline — record while performing the route cycle multiple times. Stop recording → blue bars that remain (not grayed out) are retained allocations. Click the largest retained allocation groups → trace the retainer chain to find the root. Also check: are there unbounded event listener maps, global arrays that grow with each navigation, or React component subscriptions not unsubscribed in `useEffect` cleanup? Use `getEventListeners($0)` in Console on suspected DOM nodes to count listeners. Add a Performance Observer in production for `memory-pressure` events to catch devices near limit.

---

## 17. Cheatsheet: All Shortcuts + Commands

### DevTools Keyboard Shortcuts

```text
OPENING DEVTOOLS
  Cmd+Opt+I / F12         Open DevTools (last panel)
  Cmd+Opt+J               Open DevTools → Console
  Cmd+Opt+C               Open DevTools → Elements + inspect cursor
  Cmd+Shift+M             Toggle Device Toolbar (responsive mode)
  Escape                  Toggle Console drawer (when in any other panel)

NAVIGATION
  Cmd+]                   Next panel
  Cmd+[                   Previous panel
  Cmd+1 to Cmd+9          Jump to specific panel by position
  Cmd+P                   Go to file (in Sources tab)
  Cmd+Shift+P             Run command (command palette)

ELEMENTS TAB
  H                       Hide element (toggle display:none)
  Delete                  Delete element
  Cmd+Z                   Undo DOM change
  Cmd+F                   Search DOM
  Ctrl+` (backtick)       Open Console drawer while in Elements

CONSOLE
  Enter                   Execute expression
  Shift+Enter             New line without executing
  Up/Down arrows          Navigate command history
  Ctrl+L / Cmd+K          Clear console
  Cmd+F                   Find in console output

SOURCES / DEBUGGER
  F8                      Resume (when paused)
  F10                     Step over
  F11                     Step into
  Shift+F11               Step out
  Cmd+B                   Toggle breakpoint on current line
  Cmd+\                   Pause / resume script execution
  Cmd+Opt+F               Search all files
  Ctrl+G                  Go to line number

NETWORK
  Cmd+R                   Reload page (normal reload)
  Cmd+Shift+R             Hard reload (bypass cache)
  Cmd+F                   Find in request list

PERFORMANCE
  Cmd+E                   Start/stop recording
```

### Console Quick Reference

```javascript
// Selection
$('selector')              // querySelector (first match)
$$('selector')             // querySelectorAll → array
$x('//xpath')              // XPath selector
$0                         // selected element
$_                         // last evaluated expression

// Monitoring
monitor(fn)                // log every call to fn
monitorEvents(el, 'click') // log events on element
getEventListeners($0)      // all listeners on element

// Utility
copy(value)                // copy value to clipboard
inspect(node)              // jump to element in Elements tab
table(array)               // render as table
keys(obj)                  // object keys (like Object.keys)
values(obj)                // object values

// Performance
console.time('label')
console.timeEnd('label')
performance.now()          // high-res timestamp

// Debugging
debug(fn)                  // set breakpoint at start of fn
undebug(fn)
```

### Core Web Vitals Quick Reference

```text
Metric     Good       Needs Work   Poor     Weight (LH)
─────────────────────────────────────────────────────────
LCP        ≤ 2.5s     2.5-4.0s    > 4.0s   25%
INP        ≤ 200ms    200-500ms   > 500ms  (field only)
CLS        ≤ 0.1      0.1-0.25    > 0.25   15%
FCP        ≤ 1.8s     1.8-3.0s    > 3.0s   15%
TTFB       ≤ 800ms    800-1800ms  > 1800ms (not CWV)
TBT        ≤ 200ms    200-600ms   > 600ms  30%
```

### Network Tab Filter Syntax

```text
text                       URL substring match
-text                      Exclude URL substring
method:POST                HTTP method
status-code:404            Status code
larger-than:100k           Size > 100KB
domain:example.com         Specific domain
has-response-header:ETag   Has response header
is:blocked                 Blocked requests
is:from-cache              Cached responses
is:service-worker-intercepted  Service worker handled
mime-type:image/png        Specific MIME type
```

### Application Tab Quick Actions

```javascript
// Storage operations in Console
localStorage.clear()
sessionStorage.clear()

// View all localStorage
Object.entries(localStorage).forEach(([k,v]) => console.log(k, v))

// Service Worker: force update via code
navigator.serviceWorker.getRegistration().then(r => r.update())

// IndexedDB: inspect via Console
indexedDB.databases().then(dbs => console.table(dbs))

// Cache API: list all cached URLs
caches.keys().then(names => 
  Promise.all(names.map(n => caches.open(n).then(c => c.keys())))
).then(all => all.flat().map(r => r.url)).then(console.log)
```

### Performance Observer Snippets

```javascript
// Observe LCP
new PerformanceObserver(list => {
  list.getEntries().forEach(e => console.log('LCP:', e.startTime, e.element));
}).observe({ type: 'largest-contentful-paint', buffered: true });

// Observe CLS
new PerformanceObserver(list => {
  list.getEntries().filter(e => !e.hadRecentInput).forEach(e => {
    console.log('CLS shift:', e.value, e.sources?.map(s => s.node));
  });
}).observe({ type: 'layout-shift', buffered: true });

// Observe slow interactions (INP precursor)
new PerformanceObserver(list => {
  list.getEntries().filter(e => e.duration > 100).forEach(e => {
    console.log('Slow event:', e.name, e.duration + 'ms', e.target);
  });
}).observe({ type: 'event', durationThreshold: 100, buffered: true });

// Observe long tasks
new PerformanceObserver(list => {
  list.getEntries().forEach(e => console.log('Long task:', e.duration + 'ms'));
}).observe({ type: 'longtask', buffered: true });

// Observe resource timing (all network requests with timings)
new PerformanceObserver(list => {
  list.getEntries().forEach(e => {
    console.log(e.name, {
      ttfb: e.responseStart - e.requestStart,
      download: e.responseEnd - e.responseStart,
      total: e.duration,
    });
  });
}).observe({ type: 'resource', buffered: true });
```

---

*Track covers: DevTools mental model + rendering pipeline, Elements tab (DOM/CSS live editing/box model/event listeners/accessibility tree), Console tab (all console methods, $ shortcuts, live expressions, error reading, filtering), Sources tab (all breakpoint types: line/conditional/logpoint/DOM/XHR/event/exception, debugger controls, call stack, scope, watch, snippets, local overrides, ignore list), Network tab (all columns, filtering, request inspection, timing waterfall, throttling, HAR, 10 debugging tricks), Performance tab (flame chart anatomy, long tasks, layout thrashing, Core Web Vitals overlay), Memory tab (heap snapshot, allocation timeline, detached DOM/event listener/closure/timer/cache leak patterns), Application tab (localStorage, sessionStorage, cookies + all flags, IndexedDB, Service Workers, Cache Storage, Manifest, Background Services), Security tab (TLS details, mixed content), Lighthouse (scoring, opportunities vs diagnostics), Core Web Vitals deep dive (LCP/INP/CLS — thresholds, causes, fixes, measurement code), Responsive Design Mode (device emulation, media queries, remote device debugging), Advanced (Coverage, Request Blocking, Rendering tab, Sensors, WebAuthn, Performance Insights), Real-world debugging scenarios (slow page, CLS, 401, laggy button, memory leak, CORS error), Interview Q&A (beginner → MAANG), full keyboard/Console/filter/PerformanceObserver cheatsheet.*
