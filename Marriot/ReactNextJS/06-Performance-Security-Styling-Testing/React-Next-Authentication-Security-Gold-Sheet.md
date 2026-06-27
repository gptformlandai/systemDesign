# React + Next.js Authentication And Security - Gold Sheet

> Track File #13 of 24 - Group 6: Performance, Security, Styling, And Testing
> Covers: cookies vs tokens, session management, NextAuth basics, protecting routes, XSS, CSRF

---

## 1. Intuition

Authentication answers "who are you?" Authorization answers "what can you do?"

In web apps, auth is also a storage and request-boundary problem.

```text
browser -> cookie/token -> server/session -> route/data authorization -> UI
```

---

## 2. Cookies vs Tokens

| Storage | Pros | Cons |
|---|---|---|
| HttpOnly secure cookie | not readable by JS, good for server auth | CSRF must be considered |
| localStorage token | easy JS access | XSS can steal it |
| memory token | less persistent exposure | lost on refresh |
| server session | revocable, centralized | session store complexity |

Senior default:
For server-rendered Next apps, HttpOnly secure cookies and server-side session checks are usually a strong default.

---

## 3. Session Management

Session concerns:
- login
- logout
- session expiration
- refresh/rotation
- revocation
- role/permission changes
- multi-tab behavior
- protecting server data

Server check:

```ts
import {cookies} from 'next/headers';
import {redirect} from 'next/navigation';

export async function requireSession() {
  const cookieStore = await cookies();
  const sessionId = cookieStore.get('session')?.value;
  const session = sessionId ? await getSession(sessionId) : null;

  if (!session) {
    redirect('/login');
  }

  return session;
}
```

---

## 4. NextAuth.js Basics

NextAuth/Auth.js-style libraries commonly handle:
- providers
- callbacks
- session creation
- cookies
- CSRF protections for auth flows
- server/client session helpers

Use when:
- common auth providers
- standard session patterns
- team wants battle-tested defaults

Still your responsibility:
- app authorization
- route protection
- role checks
- data access checks
- secure callback configuration

---

## 5. Protecting Routes

Layers:
- middleware/proxy for broad redirects
- server component/session check for route data
- route handler/server action authorization
- UI gating for user experience

Important:
UI gating is not security. Server-side authorization is security.

```tsx
export default async function AdminPage() {
  const session = await requireSession();

  if (!session.roles.includes('admin')) {
    notFound();
  }

  return <AdminDashboard />;
}
```

---

## 6. XSS Basics

XSS means attacker-controlled script runs in your page.

Sources:
- unsafe HTML injection
- markdown rendering
- third-party scripts
- user-generated content
- dangerouslySetInnerHTML

Avoid:

```tsx
<div dangerouslySetInnerHTML={{__html: userBio}} />
```

Safer:
- escape by default through React text rendering
- sanitize trusted HTML with reviewed sanitizer
- Content Security Policy
- avoid storing tokens in JS-readable storage

---

## 7. CSRF Basics

CSRF abuses browser auto-sent cookies to make unwanted state-changing requests.

Mitigations:
- SameSite cookies
- CSRF tokens for unsafe methods
- verify Origin/Referer where appropriate
- avoid state-changing GET
- framework/library protections

Rule:
If cookies authenticate state-changing requests, think about CSRF.

---

## 8. Real-World Use Cases

- SaaS dashboard: server session check in layout.
- Admin action: Server Action checks role before mutation.
- Auth callback: use auth library and validate redirect URLs.
- Public markdown blog: sanitize markdown output.
- Payment settings: re-auth or step-up auth.

---

## 9. Common Mistakes

| Mistake | Why It Is Wrong | Better Approach |
|---|---|---|
| Storing long-lived JWT in localStorage | XSS theft risk | HttpOnly cookie/session |
| Only hiding admin button | Not authorization | Check on server |
| Unsafe redirects after login | Open redirect | allowlist callback URLs |
| `dangerouslySetInnerHTML` with user input | XSS | sanitize or avoid |
| State-changing GET route | CSRF/cache risk | POST with protection |

---

## 10. Strong Interview Answer

Question:
How do you secure authentication in a Next.js app?

Strong answer:

```text
I prefer HttpOnly secure cookies or a well-managed server session for Next.js
apps, because server components and route handlers can authorize requests without
exposing tokens to JavaScript. I protect routes on the server, not just in the UI.
Server Actions and API routes validate input and check permissions. For XSS, I
avoid unsafe HTML and use CSP/sanitization where needed. For cookie-based auth, I
consider CSRF with SameSite cookies, CSRF tokens, and origin checks for mutations.
```

---

## 11. Revision Notes

- One-line summary: Auth security lives on the server boundary, not in hidden UI.
- Three keywords: HttpOnly, authorization, XSS/CSRF.
- One interview trap: localStorage tokens are easy but risky under XSS.
- One memory trick: UI can guide, server must enforce.

