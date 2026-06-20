# JavaScript Security Best Practices Master Sheet

Target: JavaScript, browser, Node.js, full-stack, frontend, backend, platform, and MAANG interviews where you must explain real security risks, write safer code, and design production guardrails.

This sheet covers:
- JavaScript threat model
- Browser vs server trust boundaries
- XSS and output encoding
- DOM XSS and dangerous APIs
- Content Security Policy
- CSRF and cookie security
- JWT, sessions, and token storage
- Authentication and authorization mistakes
- CORS security model
- SQL/NoSQL/command/template injection
- Prototype pollution
- SSRF and server-side fetch risks
- Path traversal and file upload security
- npm supply chain and dependency hygiene
- Secrets management
- Logging and PII safety
- Rate limiting and abuse controls
- Secure headers
- Webhook verification
- Deserialization and unsafe parsing
- Security testing checklist
- Production incident scenarios
- Strong interview answers

How to use this:
- Learn the attack first, then the defense.
- Practice saying: attacker input, vulnerable sink, exploit impact, safe pattern, production guardrail.
- Connect frontend security to backend enforcement. The browser is user-controlled.
- In interviews, avoid one-size-fits-all answers. Security depends on threat model and context.

---

## 1. Security Mental Model

Security is about controlling trust.

```text
Untrusted input -> validation/canonicalization -> safe processing -> safe output -> monitoring
```

Core rule:

```text
Anything from users, browsers, network calls, files, queues, webhooks, environment, dependencies,
or databases can be malformed, malicious, stale, or unexpected.
```

Strong interview line:

```text
I do not treat TypeScript types, frontend checks, or happy-path validation as security boundaries.
Security controls must exist at runtime and on the server side where the protected resource lives.
```

---

## 2. JavaScript Security Priority Meter

| Topic | Priority | Why It Matters |
|---|---:|---|
| XSS | Very high | JavaScript executes in user browsers |
| CSRF | Very high | Cookie-authenticated actions can be abused |
| Token storage | Very high | XSS/CSRF trade-off topic |
| Authorization | Very high | Backend must enforce permissions |
| Prototype pollution | Very high | JavaScript-specific backend risk |
| npm supply chain | Very high | Node depends heavily on packages |
| Injection attacks | Very high | SQL/NoSQL/command/template risks |
| CORS | High | Often misunderstood browser boundary |
| CSP | High | XSS impact reduction |
| SSRF | High | Server-side URL fetch risk |
| Path traversal | High | File access risk |
| File upload | High | Malware, storage, memory, path, type risk |
| Webhook verification | High | External event authenticity |
| Secrets management | Very high | Prevent credential leakage |
| Logging PII/secrets | High | Common incident source |
| Rate limiting | High | Abuse and cost control |
| Secure headers | Medium-high | Defense-in-depth |
| Dependency scanning | High | Vulnerable transitive packages |
| Session security | High | Account takeover prevention |
| Security testing | High | Regression prevention |

---

## 3. Trust Boundaries

Common trust boundaries:

| Boundary | Why It Matters |
|---|---|
| Browser to API | User can modify requests |
| API to database | Injection and authorization checks required |
| API to external service | Timeouts, SSRF, data exposure |
| Webhook provider to API | Signature verification required |
| Queue to worker | Message validation required |
| File upload to storage | Type, size, scanning, ownership required |
| npm package to app | Dependency code runs with app privileges |
| Logs to observability vendor | PII/secrets can leak |

Strong line:

```text
At every trust boundary, I validate input, enforce authorization, limit resources, and log safely.
```

---

## 4. Frontend Is Not A Security Boundary

Bad assumption:

```text
The admin button is hidden, so non-admins cannot delete users.
```

Reality:

```text
Users can call APIs directly with curl, browser DevTools, scripts, or modified clients.
```

Backend must enforce:

```javascript
app.delete("/admin/users/:id", requireUser, requireRole("admin"), async (request, response) => {
    await deleteUser(request.params.id);
    response.status(204).end();
});
```

Strong answer:

```text
Frontend permission checks improve UX, but the backend must enforce authorization on every protected action.
```

---

## 5. XSS In One Line

Definition:

```text
Cross-site scripting happens when untrusted input is rendered as executable JavaScript or HTML in a user's browser.
```

Impact:

- Steal tokens readable by JavaScript.
- Perform actions as the user.
- Modify page content.
- Capture keystrokes.
- Exfiltrate sensitive data.
- Attack other users if stored XSS.

Strong line:

```text
XSS is dangerous because injected code runs inside the trusted origin of the application.
```

---

## 6. Reflected XSS

Prompt:

```text
Search page reflects query into HTML: /search?q=<script>alert(1)</script>
```

Bad server rendering:

```javascript
response.send(`<h1>Results for ${request.query.q}</h1>`);
```

Safe output encoding idea:

```javascript
function escapeHtml(value) {
    return String(value)
        .replaceAll("&", "&amp;")
        .replaceAll("<", "&lt;")
        .replaceAll(">", "&gt;")
        .replaceAll('"', "&quot;")
        .replaceAll("'", "&#39;");
}

response.send(`<h1>Results for ${escapeHtml(request.query.q)}</h1>`);
```

Strong answer:

```text
Reflected XSS occurs when request input is immediately rendered unsafely. I prevent it with
context-aware output encoding and safe templating, not by trying to blacklist script tags.
```

---

## 7. Stored XSS

Prompt:

```text
User posts a comment containing HTML/JS. Everyone who views the comment executes it.
```

Bad:

```javascript
commentElement.innerHTML = comment.body;
```

Safe default:

```javascript
commentElement.textContent = comment.body;
```

If rich HTML is required:

```text
Use a trusted sanitizer with allowlisted tags and attributes. Do not write casual custom sanitizers.
```

Strong answer:

```text
Stored XSS is worse than reflected XSS because the malicious payload persists and attacks every
viewer. I render user content as text by default and sanitize strictly only when rich text is a real requirement.
```

---

## 8. DOM XSS

DOM XSS happens fully in browser-side JavaScript.

Bad:

```javascript
const params = new URLSearchParams(location.search);
document.querySelector("#message").innerHTML = params.get("message");
```

Better:

```javascript
const params = new URLSearchParams(location.search);
document.querySelector("#message").textContent = params.get("message") ?? "";
```

Dangerous sinks:

- `innerHTML`
- `outerHTML`
- `insertAdjacentHTML`
- `document.write`
- `eval`
- `new Function`
- `setTimeout(string)`
- unsafe template rendering
- dangerous framework escape hatches

Strong answer:

```text
DOM XSS occurs when client-side code moves untrusted data into dangerous DOM sinks. I use safe text
APIs and avoid executable sinks.
```

---

## 9. Context-Aware Encoding

Different contexts need different escaping:

| Context | Example | Defense |
|---|---|---|
| HTML text | `<p>${value}</p>` | HTML escape |
| HTML attribute | `<img alt="${value}">` | Attribute escape |
| URL | `?q=${value}` | URL encode / URLSearchParams |
| JavaScript string | `<script>var x='${value}'</script>` | Avoid inline JS; JS string escape |
| CSS | `style="color:${value}"` | Avoid untrusted CSS; allowlist |

Safe URL building:

```javascript
const params = new URLSearchParams({ q: userInput });
const url = `/search?${params}`;
```

Strong answer:

```text
Escaping must match the output context. HTML escaping is not automatically safe for URLs,
JavaScript strings, CSS, or attributes.
```

---

## 10. Dangerous HTML Sanitization Assumption

Bad sanitizer:

```javascript
function sanitize(html) {
    return html.replaceAll("<script>", "").replaceAll("</script>", "");
}
```

Why bad:

- Attackers use event handlers: `onerror`.
- Mixed case and malformed tags bypass filters.
- SVG/MathML can execute in surprising ways.
- URL attributes can contain `javascript:`.
- Browser parsing is complex.

Better:

```text
Use a mature sanitizer with allowlist rules, keep it updated, and test dangerous payloads.
```

Strong answer:

```text
HTML sanitization is hard because browser parsing is complex. I prefer text rendering; if rich HTML
is required, I use a maintained allowlist sanitizer.
```

---

## 11. Content Security Policy

CSP reduces XSS impact by controlling what the browser can execute/load.

Example:

```text
Content-Security-Policy: default-src 'self'; script-src 'self' 'nonce-random'; object-src 'none'; base-uri 'self'; frame-ancestors 'none'
```

Benefits:

- Blocks many inline scripts.
- Restricts script sources.
- Reduces data exfiltration paths.
- Helps detect violations through reports.

Cautions:

- CSP is defense-in-depth, not a replacement for output encoding.
- Avoid broad `unsafe-inline`.
- Nonces must be unpredictable per response.
- Third-party scripts complicate policy.

Strong answer:

```text
CSP is a strong XSS mitigation layer, but the primary defense is still preventing injection with
safe rendering and context-aware encoding.
```

---

## 12. Trusted Types

Trusted Types can reduce DOM XSS by restricting dangerous sinks.

Concept:

```text
Instead of allowing arbitrary strings into innerHTML-like sinks, the browser requires a TrustedHTML object.
```

Use case:

```text
Large apps with many contributors and legacy DOM code can use Trusted Types to enforce safer patterns.
```

Strong answer:

```text
Trusted Types are a browser-level guardrail for DOM XSS, especially useful in large apps where unsafe sinks are hard to audit manually.
```

---

## 13. CSRF In One Line

Definition:

```text
Cross-site request forgery tricks a user's browser into sending an authenticated request to another site where the user is logged in.
```

Why cookies matter:

```text
Browsers attach cookies automatically based on domain/path/SameSite rules.
```

Example attack:

```html
<form action="https://bank.example.com/transfer" method="POST">
    <input name="to" value="attacker">
    <input name="amount" value="1000">
</form>
<script>document.forms[0].submit()</script>
```

Strong answer:

```text
CSRF abuses ambient authority: the browser automatically includes cookies even when the request was triggered from an attacker's site.
```

---

## 14. CSRF Defenses

Main defenses:

- SameSite cookies.
- CSRF tokens.
- Origin/Referer checks.
- Custom headers for XHR/fetch APIs.
- Re-authentication for sensitive actions.
- Avoid state changes via GET.

CSRF token shape:

```javascript
app.post("/transfer", requireUser, verifyCsrfToken, async (request, response) => {
    await transferMoney(request.user.id, request.body);
    response.status(204).end();
});
```

Origin check idea:

```javascript
function requireTrustedOrigin(request, response, next) {
    const origin = request.header("Origin");

    if (origin !== "https://app.example.com") {
        response.status(403).json({ error: "bad_origin" });
        return;
    }

    next();
}
```

Strong answer:

```text
For cookie-authenticated state-changing requests, I use SameSite plus CSRF tokens or origin checks.
GET must not mutate server state.
```

---

## 15. Cookie Security

Important attributes:

```text
HttpOnly -> JavaScript cannot read cookie
Secure -> sent only over HTTPS
SameSite -> controls cross-site sending
Path/Domain -> scope
Max-Age/Expires -> lifetime
```

Example:

```text
Set-Cookie: session=abc; HttpOnly; Secure; SameSite=Lax; Path=/; Max-Age=3600
```

Cross-site cookie auth may need:

```text
SameSite=None; Secure
```

Strong answer:

```text
HttpOnly reduces token theft through XSS, Secure requires HTTPS, and SameSite helps reduce CSRF risk.
```

---

## 16. Token Storage Trade-Off

Common options:

| Storage | Benefit | Risk |
|---|---|---|
| localStorage | Easy persistence | XSS can read tokens |
| sessionStorage | Per-tab persistence | XSS can still read tokens |
| memory | Less persistent theft | Lost on refresh; refresh flow needed |
| HttpOnly cookie | JS cannot read token | CSRF considerations |
| BFF/session | Server-side control | More infrastructure complexity |

Strong answer:

```text
There is no universal perfect token storage. I discuss threat model: XSS risk, CSRF risk, token lifetime,
refresh strategy, user experience, and backend architecture.
```

---

## 17. JWT Verification

Dangerous mistake:

```javascript
const payload = JSON.parse(Buffer.from(token.split(".")[1], "base64url").toString("utf8"));
request.user = payload;
```

Problem:

```text
Decoding reads claims. It does not verify authenticity.
```

JWT checks:

- Signature.
- Algorithm allowlist.
- Expiration.
- Issuer.
- Audience.
- Not-before.
- Key ID and key rotation.
- Clock tolerance.

Strong answer:

```text
A JWT must be verified, not only decoded. I verify signature and claims before trusting identity or roles.
```

---

## 18. JWT Common Mistakes

| Mistake | Why It Is Bad |
|---|---|
| Trusting decoded payload | Attacker can forge claims |
| Accepting `alg: none` | Signature bypass risk in vulnerable setups |
| Not checking audience | Token for another app accepted |
| Not checking issuer | Wrong identity provider accepted |
| Long-lived access tokens | Larger damage window |
| No key rotation handling | Outages after JWKS rotation |
| Storing sensitive data in JWT | JWT payload is usually readable |
| No revocation strategy | Hard to invalidate compromised tokens |

Strong answer:

```text
JWTs are bearer credentials. Keep them short-lived, verify claims, handle key rotation, and avoid storing secrets in payloads.
```

---

## 19. Sessions Vs JWTs

Session cookie approach:

```text
Browser stores opaque session ID -> server/session store maps ID to user/session state
```

JWT approach:

```text
Client sends signed token -> server verifies signature and claims without looking up session by default
```

Trade-offs:

| Topic | Session | JWT |
|---|---|---|
| Revocation | Easier | Harder without state/denylist |
| Server storage | Needed | Often not needed |
| Token size | Small cookie ID | Larger token |
| Claim freshness | Server-controlled | Can become stale |
| Microservices | Needs shared session or introspection | Easier local verification |

Strong answer:

```text
Sessions are often easier to revoke and control. JWTs are useful for distributed verification but need careful lifetime, claim, and revocation design.
```

---

## 20. Password Handling

Never store plaintext passwords.

Use password hashing designed for passwords:

```text
Argon2id, bcrypt, scrypt
```

Do not use:

```text
MD5, SHA1, plain SHA256 without password hashing parameters
```

Good practices:

- Unique salt per password.
- Work factor tuned to environment.
- Optional pepper in secret manager.
- Rate limit login attempts.
- MFA for sensitive accounts.
- Breached password checks where appropriate.

Strong answer:

```text
Passwords need slow adaptive hashing, not fast general-purpose hashing. I use Argon2id/bcrypt/scrypt and rate-limit authentication attempts.
```

---

## 21. Authorization Models

Authorization answers:

```text
Can this authenticated identity perform this action on this resource?
```

Common models:

- RBAC: role-based access control.
- ABAC: attribute-based access control.
- ReBAC: relationship-based access control.
- ACLs: per-resource access lists.
- Tenant isolation rules.

Bad:

```javascript
if (request.user) {
    await deleteBooking(request.params.id);
}
```

Better:

```javascript
const booking = await loadBooking(request.params.id);

if (booking.tenantId !== request.user.tenantId) {
    throw forbidden();
}

if (!request.user.roles.includes("booking_admin")) {
    throw forbidden();
}
```

Strong answer:

```text
Authentication is not enough. Authorization must check action, resource, tenant, and user permissions.
```

---

## 22. IDOR / Broken Object Level Authorization

IDOR example:

```text
GET /api/bookings/123 returns booking 123 to any logged-in user.
```

Bad:

```javascript
app.get("/bookings/:id", requireUser, async (request, response) => {
    response.json(await bookings.findById(request.params.id));
});
```

Better:

```javascript
app.get("/bookings/:id", requireUser, async (request, response) => {
    const booking = await bookings.findById(request.params.id);

    if (!booking || booking.tenantId !== request.user.tenantId) {
        response.status(404).json({ error: "not_found" });
        return;
    }

    response.json(booking);
});
```

Strong answer:

```text
Every object access must be authorized. Guessing another ID should not grant access to another user's data.
```

---

## 23. CORS Is Not Auth

CORS controls which browser origins can read responses.

It does not:

- Authenticate users.
- Authorize actions.
- Protect APIs from curl/server-to-server clients.
- Replace CSRF protection.
- Replace rate limiting.

Misconfiguration:

```text
Access-Control-Allow-Origin: *
Access-Control-Allow-Credentials: true
```

Invalid combination for browsers with credentials.

Strong answer:

```text
CORS is a browser security policy, not an API authentication mechanism. Server-side auth and authorization are still required.
```

---

## 24. Secure CORS With Credentials

Frontend:

```javascript
await fetch("https://api.example.com/me", {
    credentials: "include"
});
```

Server response:

```text
Access-Control-Allow-Origin: https://app.example.com
Access-Control-Allow-Credentials: true
Vary: Origin
```

Important:

- Use explicit origins, not wildcard, with credentials.
- Validate allowed origins carefully.
- Handle preflight OPTIONS.
- Do not reflect arbitrary Origin blindly.

Strong answer:

```text
Credentialed CORS requires explicit allowed origins and allow-credentials. Blind origin reflection is risky.
```

---

## 25. SQL Injection

Bad:

```javascript
const sql = `select * from users where email = '${request.query.email}'`;
await db.query(sql);
```

Attack:

```text
' OR '1'='1
```

Safe:

```javascript
await db.query(
    "select * from users where email = $1",
    [request.query.email]
);
```

Strong answer:

```text
Never concatenate untrusted input into SQL. Use parameterized queries or safe query builders.
```

---

## 26. NoSQL Injection

Bad Mongo-like pattern:

```javascript
const user = await users.findOne({
    email: request.body.email,
    password: request.body.password
});
```

If body allows objects:

```json
{
  "email": { "$ne": null },
  "password": { "$ne": null }
}
```

Safer:

```javascript
function requireString(value, field) {
    if (typeof value !== "string") {
        throw new Error(`${field} must be string`);
    }

    return value;
}

const email = requireString(request.body.email, "email");
const password = requireString(request.body.password, "password");
```

Strong answer:

```text
NoSQL injection often comes from accepting objects/operators where strings are expected. Runtime schema validation is essential.
```

---

## 27. Command Injection

Bad:

```javascript
import { exec } from "node:child_process";

exec(`convert ${request.body.fileName} output.png`);
```

Attack:

```text
file.jpg; rm -rf /tmp/uploads
```

Better:

```javascript
import { execFile } from "node:child_process";

execFile("convert", [safeInputPath, outputPath], {
    timeout: 5000
});
```

Strong answer:

```text
Avoid shell string construction with untrusted input. Use execFile/spawn with validated arguments and timeouts.
```

---

## 28. Template Injection

Risk:

```text
If user-controlled content is interpreted as a template, attackers may access variables or execute code depending on engine.
```

Bad idea:

```javascript
const template = compile(request.body.template);
return template(data);
```

Defenses:

- Do not allow arbitrary user templates unless sandboxed by design.
- Use restricted template languages.
- Escape output by default.
- Separate user content from executable templates.

Strong answer:

```text
User content should be data, not executable templates. If user-defined templates are a feature, sandboxing and strict capability limits are required.
```

---

## 29. Prototype Pollution

Definition:

```text
Prototype pollution occurs when attacker-controlled keys modify Object.prototype or object prototypes, causing unexpected inherited properties.
```

Dangerous input:

```json
{
  "__proto__": {
    "isAdmin": true
  }
}
```

Bad merge:

```javascript
function merge(target, source) {
    for (const key in source) {
        if (typeof source[key] === "object") {
            target[key] = target[key] ?? {};
            merge(target[key], source[key]);
        } else {
            target[key] = source[key];
        }
    }
}
```

Defenses:

- Schema validation with allowlisted keys.
- Reject `__proto__`, `constructor`, `prototype`.
- Avoid unsafe deep merge of untrusted input.
- Use patched libraries.
- Use `Object.create(null)` for dictionaries when appropriate.

Strong answer:

```text
Prototype pollution is a JavaScript-specific risk. I prevent it with allowlist validation and safe object merging.
```

---

## 30. Unsafe Object Access

Bad authorization check:

```javascript
if (user.isAdmin) {
    allowAdminAction();
}
```

If prototype pollution sets inherited `isAdmin`, this may pass.

Safer:

```javascript
if (Object.hasOwn(user, "isAdmin") && user.isAdmin === true) {
    allowAdminAction();
}
```

Even better:

```text
Do not authorize from mutable request body objects. Use trusted server-side user records/claims after verification.
```

Strong answer:

```text
For security-sensitive checks, data should come from trusted sources and expected own properties, not inherited polluted values.
```

---

## 31. SSRF

Definition:

```text
Server-side request forgery occurs when attackers make your server send requests to internal or unintended destinations.
```

Bad:

```javascript
app.post("/preview", async (request, response) => {
    const result = await fetch(request.body.url);
    response.send(await result.text());
});
```

Dangerous targets:

```text
http://localhost:3000/admin
http://127.0.0.1:6379
http://169.254.169.254/latest/meta-data
http://internal-service.local
```

Defenses:

- Prefer allowlist of domains.
- Block private/internal IP ranges.
- Resolve DNS carefully.
- Re-check after redirects.
- Set timeouts and response size limits.
- Do not forward internal credentials.
- Isolate fetcher network permissions.

Strong answer:

```text
If a server fetches user-provided URLs, I treat it as SSRF risk and restrict destinations aggressively.
```

---

## 32. Path Traversal

Bad:

```javascript
app.get("/files", (request, response) => {
    response.sendFile(`/app/uploads/${request.query.name}`);
});
```

Attack:

```text
GET /files?name=../../etc/passwd
```

Safe path resolution:

```javascript
import path from "node:path";

const root = path.resolve("/app/uploads");

function safePath(name) {
    const candidate = path.resolve(root, name);

    if (!candidate.startsWith(root + path.sep)) {
        throw new Error("invalid_path");
    }

    return candidate;
}
```

Strong answer:

```text
Normalize user-controlled paths and constrain them to an allowed root. Prefer server-generated file IDs over raw filenames.
```

---

## 33. File Upload Security

Upload risks:

- Oversized files causing memory/disk pressure.
- Malware upload.
- Content-type spoofing.
- Path traversal in filenames.
- Public access to private files.
- Executable files served from same origin.
- Image parser vulnerabilities.
- Zip bombs.

Controls:

- Size limits.
- Auth and ownership checks.
- Store outside web root.
- Server-generated names.
- Malware scanning.
- Content sniffing where appropriate.
- Async processing.
- Direct-to-object-storage for large files.
- Signed URLs with expiration.

Strong answer:

```text
File upload is a full workflow risk: validation, storage, scanning, access control, processing, and download permissions all matter.
```

---

## 34. Zip Slip

Risk:

```text
Archive entries can contain paths like ../../app/server.js and overwrite files during extraction.
```

Defense:

```text
For each archive entry, resolve the final path and verify it stays inside the intended extraction directory.
```

Strong answer:

```text
Never trust archive entry paths. Treat them like user-controlled file paths and constrain extraction targets.
```

---

## 35. Open Redirect

Bad:

```javascript
app.get("/login/callback", (request, response) => {
    response.redirect(request.query.next);
});
```

Attack:

```text
/login/callback?next=https://evil.example/phishing
```

Safe allowlist:

```javascript
function safeRedirect(next) {
    if (typeof next === "string" && next.startsWith("/")) {
        return next;
    }

    return "/";
}
```

Strong answer:

```text
Redirect destinations should be allowlisted or constrained to relative internal paths to prevent phishing and token leakage.
```

---

## 36. Webhook Verification

Webhook risks:

- Fake events.
- Replay attacks.
- Duplicate delivery.
- Payload tampering.
- Heavy processing causing provider retries.

Checklist:

- Verify signature with raw body.
- Check timestamp tolerance.
- Deduplicate event ID.
- Process idempotently.
- Acknowledge quickly.
- Queue heavy work.

Strong answer:

```text
Webhook endpoints must verify authenticity and idempotency because providers retry and attackers can send fake requests.
```

---

## 37. Raw Body For Webhooks

Issue:

```text
Many providers sign exact raw bytes. JSON parsing before verification can break signatures.
```

Pattern:

```javascript
app.post("/webhooks/payment", rawBodyMiddleware, async (request, response) => {
    const event = verifySignature(request.rawBody, request.headers);
    await handleEventOnce(event);
    response.status(200).json({ received: true });
});
```

Strong answer:

```text
For signed webhooks, verify against the raw body before trusting or processing the parsed payload.
```

---

## 38. npm Supply Chain Risk

Node apps often depend on many packages. Risks:

- Vulnerable direct dependency.
- Vulnerable transitive dependency.
- Maintainer account compromise.
- Typosquatting package.
- Malicious postinstall script.
- Dependency confusion.
- Abandoned package.
- ESM/CJS breaking change.

Controls:

- Commit lockfiles.
- Use `npm ci` in CI.
- Pin Node version.
- Audit dependencies.
- Review new packages.
- Minimize dependencies.
- Use private registry controls where needed.
- Avoid running install scripts where possible if policy allows.
- Automated dependency updates with tests.

Strong answer:

```text
In Node, dependencies are part of the attack surface. I treat package changes like production code changes.
```

---

## 39. npm Audit Nuance

Good response:

```text
Assess severity, exploitability, affected path, runtime exposure, patch availability, and test impact.
```

Avoid:

```text
Blindly upgrading everything without tests.
```

Command examples:

```bash
npm audit
npm ls package-name
npm outdated
npm ci
```

Strong answer:

```text
Security updates should be fast but controlled: understand exploitability, update safely, run tests, and deploy with monitoring.
```

---

## 40. Dependency Confusion

Risk:

```text
Build system installs a public package with the same name as an internal package.
```

Defenses:

- Scoped private packages.
- Registry configuration.
- Lockfiles.
- Private registry rules.
- CI install policy.

Strong answer:

```text
Dependency confusion is prevented by clear private package scoping, registry controls, and reproducible installs.
```

---

## 41. Secrets Management

Never hardcode secrets:

```javascript
const apiKey = "sk_live_abc123";
```

Use:

- Secret manager.
- Environment variables injected by platform.
- Rotation policy.
- Least privilege credentials.
- Separate dev/stage/prod secrets.
- Secret scanning in CI.

Do not log:

- Tokens.
- API keys.
- Passwords.
- Private keys.
- Session cookies.
- Authorization headers.

Strong answer:

```text
Secrets should be stored outside code, injected securely, rotated, least-privileged, and redacted from logs.
```

---

## 42. Logging Sensitive Data

Bad:

```javascript
logger.info({ headers: request.headers, body: request.body }, "request received");
```

Better:

```javascript
logger.info({
    requestId: request.id,
    method: request.method,
    route: request.route?.path,
    statusCode: response.statusCode,
    durationMs
}, "request completed");
```

Redact:

- Authorization.
- Cookie.
- Set-Cookie.
- Password fields.
- Tokens.
- Credit card data.
- Sensitive PII.

Strong answer:

```text
Logs should help debugging without becoming a data breach. I log safe metadata and use redaction for sensitive fields.
```

---

## 43. PII And Data Minimization

Principle:

```text
Collect, store, process, and log only what is needed.
```

Controls:

- Field-level classification.
- Redaction and masking.
- Retention limits.
- Access controls.
- Encryption where appropriate.
- Audit trails.
- Data deletion workflows.

Strong answer:

```text
Security is not only preventing attackers. It also includes minimizing sensitive data exposure when systems fail.
```

---

## 44. Rate Limiting And Abuse Controls

Protect against:

- Brute force login.
- OTP abuse.
- Password reset spam.
- Signup abuse.
- Scraping.
- Expensive endpoint abuse.
- Webhook flood.

Dimensions:

- IP.
- User ID.
- Tenant.
- API key.
- Route.
- Device/session.

Strong answer:

```text
Rate limits should match the abuse model. In distributed systems, limits need shared state or gateway support.
```

---

## 45. Brute Force Protection

Login controls:

- Rate limit by account and IP.
- Progressive delay or temporary lock.
- MFA for sensitive users.
- Generic error messages.
- Breached password detection.
- Alert on credential stuffing patterns.

Avoid leaking:

```text
This email exists but password is wrong.
```

Better:

```text
Invalid email or password.
```

Strong answer:

```text
Authentication endpoints need abuse controls and careful error messages to avoid account enumeration and brute force attacks.
```

---

## 46. Secure Headers

Important headers:

```text
Content-Security-Policy
Strict-Transport-Security
X-Content-Type-Options: nosniff
Referrer-Policy
Permissions-Policy
Cross-Origin-Opener-Policy
Cross-Origin-Resource-Policy
```

Node/Express often uses Helmet-like middleware.

Strong answer:

```text
Security headers are defense-in-depth. They do not replace secure coding, but they reduce browser attack surface.
```

---

## 47. Clickjacking

Risk:

```text
Attacker frames your app and tricks users into clicking sensitive UI.
```

Defenses:

```text
Content-Security-Policy: frame-ancestors 'none'
```

Or older:

```text
X-Frame-Options: DENY
```

Strong answer:

```text
Use frame-ancestors in CSP to control who can embed your app.
```

---

## 48. HTTPS And HSTS

HTTPS protects data in transit.

HSTS tells browsers to use HTTPS for future requests:

```text
Strict-Transport-Security: max-age=31536000; includeSubDomains
```

Caution:

```text
HSTS is powerful. Roll it out carefully, especially includeSubDomains and preload.
```

Strong answer:

```text
HTTPS is baseline. HSTS prevents downgrade mistakes after the browser has learned the policy.
```

---

## 49. Unsafe eval

Bad:

```javascript
const result = eval(request.body.expression);
```

Also dangerous:

```javascript
new Function(userCode)();
setTimeout("doSomething()", 1000);
```

Defenses:

- Do not execute user strings as code.
- Use parsers/interpreters with restricted grammar if needed.
- Sandbox carefully if user scripting is a product feature.

Strong answer:

```text
Executing strings as JavaScript is almost always a security bug unless the entire product is a sandboxed code execution platform.
```

---

## 50. JSON Parsing And Validation

Parsing JSON is not validation.

Bad:

```javascript
const body = JSON.parse(raw);
await createUser(body);
```

Better:

```javascript
function parseCreateUser(value) {
    if (!value || typeof value !== "object") {
        throw new Error("body must be object");
    }

    if (typeof value.email !== "string") {
        throw new Error("email required");
    }

    return {
        email: value.email.toLowerCase().trim()
    };
}
```

Strong answer:

```text
Validation must check type, shape, length, allowed values, and business rules at runtime.
```

---

## 51. Runtime Validation And TypeScript

TypeScript helps internal code but disappears at runtime.

Bad assumption:

```typescript
app.post("/users", (req: Request<unknown, unknown, CreateUserBody>, res) => {
    createUser(req.body); // req.body still came from network at runtime
});
```

Strong answer:

```text
TypeScript is compile-time safety. External data still needs runtime validation with schemas or explicit checks.
```

---

## 52. Regular Expression DoS

Risk:

```text
Catastrophic backtracking can block the JavaScript event loop.
```

Risky pattern:

```javascript
const pattern = /^(a+)+$/;
pattern.test("aaaaaaaaaaaaaaaaaaaa!");
```

Defenses:

- Avoid nested ambiguous quantifiers.
- Limit input length.
- Use safe-regex tools where useful.
- Use libraries for complex validation.
- Monitor event-loop delay.

Strong answer:

```text
Regex is code. A vulnerable regex can become a CPU DoS in Node because it blocks the event loop.
```

---

## 53. Denial Of Service Controls

Common DoS vectors:

- Huge request bodies.
- Expensive queries.
- Unbounded pagination.
- Large JSON serialization.
- Slow clients.
- Regex backtracking.
- Login brute force.
- File upload floods.
- Retry amplification.

Controls:

- Body limits.
- Timeouts.
- Rate limits.
- Pagination max.
- Backpressure.
- Circuit breakers.
- Queue limits.
- Resource quotas.

Strong answer:

```text
Availability is part of security. I bound request size, time, concurrency, and output size.
```

---

## 54. Safe Error Messages

Bad:

```json
{
  "error": "Database password invalid for user admin at postgres://..."
}
```

Better:

```json
{
  "error": "internal_error",
  "message": "Something went wrong",
  "requestId": "req_123"
}
```

Server logs can contain safe internal detail with redaction.

Strong answer:

```text
Clients get stable non-sensitive errors. Internal details go to protected logs with request IDs.
```

---

## 55. Security Testing Checklist

For JavaScript apps, test:

- XSS payloads in user-rendered fields.
- CSRF on state-changing cookie-auth routes.
- Authorization on every object ID.
- JWT verification and claim checks.
- CORS preflight and credential behavior.
- SQL/NoSQL injection inputs.
- Prototype pollution payloads.
- Body size limits.
- File upload size/type/path behavior.
- Webhook signature and replay.
- Rate limits.
- Secrets in logs.
- Dependency vulnerabilities.
- Security headers.

---

## 56. Security Code Review Checklist

Ask:

- Where does input enter?
- Is it validated at runtime?
- Where is output rendered?
- Is output encoded for the right context?
- Is authorization enforced server-side?
- Are resource limits present?
- Are secrets/PII logged?
- Are dependency calls timed out?
- Is user-controlled URL/path/command allowed?
- Are dependencies necessary and maintained?
- Are tokens stored safely for the threat model?
- Does the route need CSRF protection?

---

## 57. Mini Program: Safe HTML Rendering Helper

```javascript
function escapeHtml(value) {
    return String(value)
        .replaceAll("&", "&amp;")
        .replaceAll("<", "&lt;")
        .replaceAll(">", "&gt;")
        .replaceAll('"', "&quot;")
        .replaceAll("'", "&#39;");
}

function renderSearchPage(query) {
    return `<!doctype html>
<html lang="en">
<head><title>Search</title></head>
<body>
    <h1>Results for ${escapeHtml(query)}</h1>
</body>
</html>`;
}
```

Interview note:

```text
Real apps should prefer trusted template engines/frameworks that escape by default, but knowing
context-aware escaping is essential.
```

---

## 58. Mini Program: Basic CSRF Token Shape

```javascript
import { randomBytes, timingSafeEqual } from "node:crypto";

function createCsrfToken(session) {
    const token = randomBytes(32).toString("base64url");
    session.csrfToken = token;
    return token;
}

function verifyCsrfToken(request, response, next) {
    const expected = request.session.csrfToken;
    const actual = request.header("X-CSRF-Token");

    if (!expected || !actual || !safeEqual(expected, actual)) {
        response.status(403).json({ error: "csrf_failed" });
        return;
    }

    next();
}

function safeEqual(a, b) {
    const left = Buffer.from(a);
    const right = Buffer.from(b);

    return left.length === right.length && timingSafeEqual(left, right);
}
```

Production note:

```text
CSRF implementation depends on session architecture, cookie SameSite settings, and frontend/API design.
```

---

## 59. Mini Program: Safe Redirect

```javascript
function getSafeRedirect(next) {
    if (typeof next !== "string") {
        return "/";
    }

    if (!next.startsWith("/")) {
        return "/";
    }

    if (next.startsWith("//")) {
        return "/";
    }

    return next;
}
```

Why strong:

- Allows internal relative paths.
- Blocks full external URLs.
- Blocks protocol-relative URLs.

---

## 60. Mini Program: Safe URL Fetch Guard

```javascript
import net from "node:net";

function assertPublicHttpUrl(rawUrl) {
    const url = new URL(rawUrl);

    if (url.protocol !== "https:" && url.protocol !== "http:") {
        throw new Error("unsupported_protocol");
    }

    if (url.username || url.password) {
        throw new Error("credentials_not_allowed");
    }

    if (isBlockedHost(url.hostname)) {
        throw new Error("blocked_host");
    }

    return url;
}

function isBlockedHost(hostname) {
    const lower = hostname.toLowerCase();

    return lower === "localhost" ||
        lower.endsWith(".local") ||
        net.isIP(lower) !== 0 && (
            lower.startsWith("127.") ||
            lower.startsWith("10.") ||
            lower.startsWith("192.168.") ||
            lower.startsWith("169.254.")
        );
}
```

Production note:

```text
Real SSRF defense must also handle DNS resolution, IPv6, redirects, cloud metadata ranges, and network isolation.
```

---

## 61. Common Mistakes

| Mistake | Why It Hurts | Better Approach |
|---|---|---|
| Trusting frontend checks | Users control browsers | Enforce on backend |
| Using innerHTML for user content | DOM XSS | textContent or sanitizer |
| Blacklisting script tags | Easy bypass | Context-aware encoding/sanitization |
| No CSP | XSS impact higher | Add strict CSP defense-in-depth |
| localStorage token by default | XSS token theft | Threat-model storage |
| Cookie auth without CSRF plan | CSRF risk | SameSite + token/origin checks |
| Decoding JWT only | Forged claims | Verify signature and claims |
| SQL string concatenation | SQL injection | Parameterized queries |
| Accepting objects for string fields | NoSQL injection | Runtime validation |
| Shell exec with user input | Command injection | execFile/spawn safe args |
| Unsafe deep merge | Prototype pollution | Schema allowlist/safe merge |
| Server fetches arbitrary URL | SSRF | Allowlist/block internal networks |
| Raw user filename path | Path traversal | Resolve under safe root |
| Unbounded upload | DoS/storage risk | Size limits/streaming/scanning |
| Logging headers/body | Secrets/PII leak | Redact and log metadata |
| In-memory rate limit across pods | Bypass | Shared store/gateway |
| Blind npm updates | Breakage risk | Audit, test, controlled deploy |
| No lockfile in CI | Dependency drift | npm ci + lockfile |
| Detailed user errors | Info leak | Stable safe errors |
| High-cardinality security logs only | Hard alerting | Metrics + logs + traces |

---

## 62. Strong Interview Answers

### How do you prevent XSS?

```text
I render untrusted content as text by default, use context-aware output encoding, avoid dangerous
DOM sinks like innerHTML, use trusted sanitizers only when rich HTML is required, and add CSP as
defense-in-depth.
```

### localStorage or HttpOnly cookies for tokens?

```text
It depends on the threat model. localStorage is vulnerable to XSS token theft. HttpOnly Secure
SameSite cookies reduce JavaScript access but require CSRF controls. I also consider token lifetime,
refresh flow, BFF/session architecture, and XSS prevention.
```

### How do you prevent CSRF?

```text
For cookie-authenticated state-changing requests, I use SameSite cookies plus CSRF tokens or
Origin/Referer checks, avoid state changes via GET, and require re-authentication for highly
sensitive actions.
```

### How do you secure Node APIs?

```text
I validate runtime input, enforce authentication and authorization server-side, use parameterized
queries, set body limits/timeouts/rate limits, handle errors safely, avoid logging secrets, verify
webhooks, and monitor abuse signals.
```

### How do you handle npm supply-chain risk?

```text
I minimize dependencies, review new packages, commit lockfiles, use npm ci, scan vulnerabilities,
pin Node versions, update with tests, and treat dependency changes like production code changes.
```

### What is prototype pollution?

```text
Prototype pollution happens when attacker-controlled keys like __proto__ or constructor modify
object prototypes. I prevent it by validating input, allowlisting keys, and avoiding unsafe deep
merges of untrusted objects.
```

---

## 63. MAANG Scenario 1: Stored XSS In Comments

> Users can post comments. Security reports that one comment executes JavaScript for every viewer.

Strong answer:

```text
This is stored XSS because untrusted content is persisted and rendered as executable markup. I would
immediately disable unsafe rendering or sanitize existing content, then change rendering to textContent
or framework-safe text interpolation.

If rich text is required, I would use a maintained allowlist sanitizer, add CSP, test common XSS
payloads, and review every dangerous sink such as innerHTML and insertAdjacentHTML.
```

---

## 64. MAANG Scenario 2: Token Stolen From Browser

> A user's JWT is stolen after an XSS bug. The app stores tokens in localStorage.

Strong answer:

```text
localStorage is readable by JavaScript, so XSS can exfiltrate tokens. Immediate response is to fix
the XSS, revoke affected tokens if possible, rotate secrets if needed, and investigate exposure.

For durable design, I would revisit token storage. HttpOnly Secure SameSite cookies can reduce JS
token theft but need CSRF controls. Memory tokens reduce persistence but complicate refresh. The
right answer depends on threat model and architecture.
```

---

## 65. MAANG Scenario 3: Tenant Data Leak

> Customer A can fetch Customer B's record by changing an ID in the URL.

Strong answer:

```text
This is broken object-level authorization. Authentication only proves who the user is; the API also
must check whether that user can access that specific object.

I would patch the endpoint to scope queries by tenant/user, return 404 or 403 safely, add tests for
cross-tenant access, review similar endpoints, and audit logs to assess exposure.
```

---

## 66. MAANG Scenario 4: npm Package Compromise

> A widely used npm dependency in your service is reported compromised.

Strong answer:

```text
I would identify whether the compromised version is in our lockfile and deployed artifact, whether
it runs in install scripts or runtime, and what access it had. Then I would remove or upgrade the
package, rotate potentially exposed secrets, rebuild from clean dependencies, and redeploy.

Afterward, I would review dependency approval, lockfile policy, secret exposure, and monitoring for
unexpected network/file activity.
```

---

## 67. MAANG Scenario 5: SSRF In Link Preview

> Your API takes a URL and fetches preview metadata. Security proves it can call internal metadata endpoints.

Strong answer:

```text
This is SSRF. The server is making requests on behalf of the attacker from a privileged network
position. I would disable or restrict the feature immediately, block private/internal IP ranges and
cloud metadata addresses, handle redirects carefully, set timeouts and response size limits, and
prefer domain allowlists.

At infrastructure level, I would restrict network egress so even a bug cannot reach sensitive internal endpoints.
```

---

## 68. Rapid Revision

- Frontend checks are UX, not security boundaries.
- XSS means untrusted input becomes executable browser code.
- Use textContent for user text.
- Avoid innerHTML with untrusted data.
- Sanitization must be allowlist-based and maintained.
- Encoding must match context: HTML, attribute, URL, JS, CSS.
- CSP reduces XSS impact but does not replace safe rendering.
- CSRF abuses automatically sent cookies.
- Cookie-auth state changes need SameSite plus CSRF token or origin checks.
- localStorage tokens are readable by XSS.
- HttpOnly cookies reduce JS theft but need CSRF strategy.
- JWT decoding is not verification.
- Verify JWT signature, issuer, audience, expiry, and algorithm.
- Authentication is who you are; authorization is what you can do.
- Every object access needs authorization.
- CORS is not authentication.
- SQL injection is prevented with parameters.
- NoSQL injection is prevented with runtime type validation.
- Command injection comes from shell strings with user input.
- Prototype pollution comes from unsafe object merging.
- SSRF comes from server-side fetching of untrusted URLs.
- Path traversal comes from unsafe file path construction.
- File uploads need limits, scanning, ownership, and safe storage.
- Webhooks need raw-body signature verification and idempotency.
- npm dependencies are attack surface.
- Lockfiles and npm ci reduce dependency drift.
- Secrets do not belong in code, logs, or client bundles.
- Logs should avoid tokens, cookies, passwords, and sensitive PII.
- Rate limits protect login, OTP, reset, expensive routes, and APIs.
- Secure headers are defense-in-depth.
- eval/new Function with user input is dangerous.
- JSON parsing is not validation.
- TypeScript does not validate runtime input.
- Regex can cause event-loop DoS.
- Safe errors should not leak internals.
- Security tests should include abuse cases, not only happy paths.

---

## 69. Official Source Notes

Use these sources when refreshing JavaScript security knowledge:

- OWASP Top 10: `https://owasp.org/www-project-top-ten/`
- OWASP API Security Top 10: `https://owasp.org/API-Security/`
- OWASP XSS Prevention Cheat Sheet: `https://cheatsheetseries.owasp.org/cheatsheets/Cross_Site_Scripting_Prevention_Cheat_Sheet.html`
- OWASP CSRF Prevention Cheat Sheet: `https://cheatsheetseries.owasp.org/cheatsheets/Cross-Site_Request_Forgery_Prevention_Cheat_Sheet.html`
- OWASP Prototype Pollution Prevention: `https://cheatsheetseries.owasp.org/cheatsheets/Prototype_Pollution_Prevention_Cheat_Sheet.html`
- OWASP SSRF Prevention Cheat Sheet: `https://cheatsheetseries.owasp.org/cheatsheets/Server_Side_Request_Forgery_Prevention_Cheat_Sheet.html`
- OWASP File Upload Cheat Sheet: `https://cheatsheetseries.owasp.org/cheatsheets/File_Upload_Cheat_Sheet.html`
- MDN Content Security Policy: `https://developer.mozilla.org/en-US/docs/Web/HTTP/CSP`
- MDN CORS: `https://developer.mozilla.org/en-US/docs/Web/HTTP/Guides/CORS`
- MDN Set-Cookie: `https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers/Set-Cookie`
- Node.js Security Best Practices: `https://nodejs.org/en/learn/getting-started/security-best-practices`
- npm security docs: `https://docs.npmjs.com/auditing-package-dependencies-for-security-vulnerabilities`

Interview safety line:

```text
For JavaScript security interviews, I connect attack surface to runtime behavior: untrusted input,
browser execution, server-side enforcement, dependency risk, resource limits, safe output, and
production observability.
```
