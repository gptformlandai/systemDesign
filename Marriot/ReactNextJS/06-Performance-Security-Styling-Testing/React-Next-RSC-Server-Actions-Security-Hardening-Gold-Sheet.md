# React Server Components And Server Actions Security Hardening - Gold Sheet

> Track Module - Group 6: Performance, Security, Styling, And Testing
> Level: senior -> architect | RSC data leaks, Server Actions hardening, DAL, DTOs, tainting, environment boundaries, and incident response

---

## 1. Intuition

Server Components make it easy to fetch data close to the server. Server Actions make it easy to mutate data from UI.

That power changes the security question:

```text
Can this server-side value accidentally cross into the client?
Can this action be called outside the UI?
Does every mutation re-check auth, authz, and input?
```

The browser is not the trust boundary anymore. The React server/client boundary is.

---

## 2. Definition

- Definition: RSC and Server Action security is the practice of keeping privileged data and logic server-only while validating and authorizing every mutation entry point.
- Category: Full-stack React security.
- Core idea: Treat Client Components, serialized props, Server Action arguments, and route params as untrusted boundaries.

---

## 3. Threat Model

Assume:
- users can inspect client bundles;
- users can call Server Actions directly through POST requests;
- route params and form values are untrusted;
- anything passed to a Client Component can become visible;
- `NEXT_PUBLIC_` values are public;
- cached server output can leak data if scoped incorrectly.

Do not assume:
- "The button is hidden, so the action is safe."
- "The user came through Proxy, so they are authorized."
- "It is a Server Component, so nothing can leak."

---

## 4. Server-Only Modules

Use `server-only` to prevent accidental client imports:

```ts
// data/users.ts
import 'server-only';
import { db } from '@/lib/db';
import { requireSession } from '@/lib/auth';

export async function getCurrentUserDTO() {
  const session = await requireSession();

  const user = await db.user.findUniqueOrThrow({
    where: { id: session.user.id },
    select: {
      id: true,
      name: true,
      email: true,
      role: true,
    },
  });

  return {
    id: user.id,
    name: user.name,
    email: user.email,
    role: user.role,
  };
}
```

Pattern:

```text
Server Component -> server-only DAL -> DTO -> Client Component
```

Never:

```text
Server Component -> full database row -> Client Component
```

---

## 5. DTO Discipline

DTO means Data Transfer Object: only the fields the client needs.

Bad:

```tsx
<ProfileCard user={userFromDatabase} />
```

Good:

```tsx
<ProfileCard
  user={{
    id: user.id,
    displayName: user.name,
    avatarUrl: user.avatarUrl,
  }}
/>
```

Rule:
Every server-to-client prop should be shaped deliberately.

---

## 6. Environment Variables

Server-only:

```env
DATABASE_URL=postgres://internal
STRIPE_SECRET_KEY=sk_live_secret
AUTH_SECRET=secret
```

Client-visible:

```env
NEXT_PUBLIC_ANALYTICS_ID=public-id
```

Hard rule:
If it starts with `NEXT_PUBLIC_`, it is not a secret.

Typed server env:

```ts
import 'server-only';
import { z } from 'zod';

export const env = z
  .object({
    DATABASE_URL: z.string().url(),
    AUTH_SECRET: z.string().min(32),
  })
  .parse(process.env);
```

---

## 7. Server Action Hardening

A Server Action is an externally reachable mutation endpoint. Treat it like an API route.

Minimum checklist:

```text
[ ] Validate input.
[ ] Authenticate user.
[ ] Authorize resource ownership or role.
[ ] Execute mutation in a server-only module or transaction.
[ ] Return only safe output.
[ ] Revalidate/update exact cache tags.
[ ] Log security-relevant action.
```

Example:

```ts
// app/settings/actions.ts
'use server';

import { revalidatePath } from 'next/cache';
import { z } from 'zod';
import { requireSession } from '@/lib/auth';
import { updateProfile } from '@/data/users';

const schema = z.object({
  displayName: z.string().min(2).max(80),
});

export async function updateProfileAction(rawInput: unknown) {
  const session = await requireSession();
  const input = schema.parse(rawInput);

  await updateProfile({
    userId: session.user.id,
    displayName: input.displayName,
  });

  revalidatePath('/settings');

  return { ok: true };
}
```

Server-only mutation:

```ts
// data/users.ts
import 'server-only';
import { db } from '@/lib/db';

export async function updateProfile(input: { userId: string; displayName: string }) {
  return db.user.update({
    where: { id: input.userId },
    data: { name: input.displayName },
  });
}
```

---

## 8. Authorization Is Not Authentication

Authentication:

```text
Who are you?
```

Authorization:

```text
Are you allowed to touch this resource?
```

Bad:

```ts
const session = await requireSession();
await db.invoice.delete({ where: { id: invoiceId } });
```

Good:

```ts
const session = await requireSession();

const invoice = await db.invoice.findUniqueOrThrow({
  where: { id: invoiceId },
  select: { ownerId: true },
});

if (invoice.ownerId !== session.user.id) {
  throw new Error('Forbidden');
}

await db.invoice.delete({ where: { id: invoiceId } });
```

---

## 9. Tainting

React taint APIs can help prevent accidentally passing sensitive objects or values to the client.

Enable in Next.js:

```ts
// next.config.ts
import type { NextConfig } from 'next';

const nextConfig: NextConfig = {
  experimental: {
    taint: true,
  },
};

export default nextConfig;
```

Use tainting as a seatbelt, not as the only control.

Primary control:

```text
DAL filters data -> DTOs expose only safe fields -> client receives DTO
```

Secondary control:

```text
Taint sensitive objects/values -> fail if accidentally passed over boundary
```

---

## 10. Cache Security

Never public-cache:
- user account data;
- cart;
- checkout;
- payment status;
- admin screens;
- tenant-specific secrets;
- feature flags that reveal entitlements incorrectly.

Safe public-cache candidates:
- marketing pages;
- product catalog without personalized pricing;
- public CMS content;
- docs;
- static navigation.

When in doubt:

```text
Can two random users receive the exact same response safely?
```

If no, do not public-cache it.

---

## 11. Client Component Boundary

This is safe:

```tsx
// Server Component
import { getCurrentUserDTO } from '@/data/users';
import { ProfileClient } from './profile-client';

export default async function Page() {
  const user = await getCurrentUserDTO();
  return <ProfileClient user={user} />;
}
```

This is dangerous:

```tsx
// Server Component
const user = await db.user.findUnique({ where: { id } });
return <ProfileClient user={user} />;
```

Why:
The full object can contain fields the UI does not display but the browser can inspect.

---

## 12. Security Testing

Unit tests:
- schema validation rejects bad input;
- authorization rejects wrong owner;
- DTO does not include secret fields.

Integration tests:
- unauthenticated POST to action fails;
- unauthorized resource update fails;
- route handler returns correct status.

E2E tests:
- hidden UI is not the only access control;
- user A cannot open user B's resource;
- logout invalidates protected flows.

Example DTO test:

```ts
expect(Object.keys(userDto)).toEqual(['id', 'name', 'email', 'role']);
expect(userDto).not.toHaveProperty('passwordHash');
expect(userDto).not.toHaveProperty('mfaSecret');
```

---

## 13. Incident Response

If an RSC or Server Action security issue is announced:

```text
1. Identify affected React/Next.js versions.
2. Patch framework dependencies.
3. Search for exposed Server Actions.
4. Review server-to-client DTOs.
5. Rotate possibly exposed secrets.
6. Check logs for suspicious POST/action calls.
7. Add regression tests.
```

Security review search terms:

```bash
rg "\"use server\"|NEXT_PUBLIC_|server-only|experimental_taint|db\\." app data lib
```

---

## 14. Common Mistakes

| Mistake | Why It Is Wrong | Better Approach |
|---|---|---|
| Hiding button instead of authorizing action | Attackers can call action directly | Auth + authz inside action/DAL |
| Passing DB rows to Client Components | Hidden fields can leak | DTO shaping |
| Putting secrets in `NEXT_PUBLIC_` | They are bundled to browser | Server-only env |
| Relying only on Proxy | Proxy can be bypassed through direct endpoints | Re-check in server code |
| Public-caching personalized UI | Cross-user data leak | Private/no-store/split dynamic islands |
| Skipping dependency security updates | Framework bugs happen | Patch and audit fast |

---

## 15. Practical Question

> You are reviewing a Next.js Server Action that deletes an invoice. What security checks do you expect?

---

## 16. Strong Answer

```text
I treat a Server Action like an API endpoint. It must validate the invoice id,
authenticate the user, authorize that the user owns the invoice or has the right
role, perform the deletion in a server-only data layer, log the mutation, and
revalidate only the affected cache/path. I would not trust the UI hiding the
delete button, and I would not rely only on Proxy. The action must be secure if
called directly.
```

---

## 17. Revision Notes

- One-line summary: RSC and Server Actions are powerful because they are server-side, but boundaries still need explicit security.
- Three keywords: DTO, server-only, authz.
- One interview trap: Authentication is not authorization.
- One memory trick: Every action is an endpoint; every prop is a possible leak.
