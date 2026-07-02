# React + Next.js Production Capstone App

> Track File #52 - Group 10: Capstone
> Level: beginner -> PRO validation | One end-to-end project that proves the whole track

---

## 1. Goal

Build a production-style Next.js app that forces you to use the full track:

```text
App Router + Server Components + Client Components + forms + Server Actions
+ auth + caching + Proxy + design system + testing + observability + deployment
```

The capstone is not a toy todo app. It is a small but realistic product surface.

---

## 2. Product: Team Operations Dashboard

You are building an internal dashboard for a team to manage:
- projects;
- tasks;
- comments;
- members;
- activity feed;
- admin settings.

Why this product works:
- has public-ish and private routes;
- needs auth and authorization;
- has forms and mutations;
- has list/detail pages;
- benefits from caching;
- has real-time or near-real-time updates;
- needs accessibility and production observability.

---

## 3. Required Features

### Foundation

```text
[ ] Next.js App Router with TypeScript.
[ ] `src/` or clear project structure.
[ ] Strict TypeScript.
[ ] ESLint/Biome.
[ ] Environment validation.
[ ] Feature-based folder structure.
```

### Auth

```text
[ ] Sign in / sign out.
[ ] Protected dashboard routes.
[ ] Role model: member, manager, admin.
[ ] Proxy redirects unauthenticated users.
[ ] Server Actions and DAL re-check auth/authz.
```

### Data

```text
[ ] Server-only Data Access Layer.
[ ] DTOs for all server-to-client props.
[ ] Project list page.
[ ] Project detail page.
[ ] Task create/update/delete.
[ ] Comment create.
[ ] Activity feed.
```

### Forms

```text
[ ] Accessible fields and errors.
[ ] Server-side validation with Zod.
[ ] `useActionState` for form result.
[ ] `useFormStatus` for pending state.
[ ] Optimistic update for comments or task status.
[ ] File upload stretch goal.
```

### Caching

```text
[ ] Cache public/team-safe data with `use cache`.
[ ] Use `cacheLife`.
[ ] Use tags: `projects`, `project:id`, `tasks:projectId`.
[ ] Update/revalidate exact tags after mutations.
[ ] Do not public-cache user-specific settings.
```

### UI

```text
[ ] Design tokens.
[ ] Reusable Button/Input/Dialog/Table primitives.
[ ] Loading, empty, error, success states.
[ ] Keyboard-accessible modal.
[ ] Responsive layout.
[ ] Basic dark mode stretch goal.
```

### Testing

```text
[ ] Unit tests for schemas and reducers.
[ ] Integration tests for server-only business logic.
[ ] React Testing Library tests for forms.
[ ] Playwright smoke tests for login, create project, update task.
[ ] Accessibility test for critical pages.
```

### Observability

```text
[ ] Error boundary routes.
[ ] `instrumentation.ts` for server tracing.
[ ] Web Vitals reporter.
[ ] Structured logs for mutations.
[ ] Release/version tag in client and server telemetry.
```

---

## 4. Suggested File Tree

```text
team-ops/
  app/
    layout.tsx
    page.tsx
    proxy.ts
    manifest.ts
    instrumentation.ts
    (auth)/
      login/page.tsx
    dashboard/
      layout.tsx
      page.tsx
      projects/
        page.tsx
        [projectId]/
          page.tsx
          loading.tsx
          error.tsx
      settings/
        page.tsx
    api/
      rum/route.ts
  components/
    ui/
      button.tsx
      input.tsx
      dialog.tsx
      table.tsx
      toast.tsx
  features/
    projects/
      actions.ts
      components/
      queries.ts
      schema.ts
      types.ts
    tasks/
      actions.ts
      components/
      queries.ts
      schema.ts
      types.ts
  data/
    projects.ts
    tasks.ts
    users.ts
  lib/
    auth.ts
    env.ts
    logger.ts
    telemetry.ts
    validation.ts
  tests/
    unit/
    integration/
    e2e/
```

---

## 5. Milestone Plan

### Milestone 1: Setup

Acceptance:
- app starts locally;
- TypeScript strict passes;
- lint passes;
- env validation exists.

Commands:

```bash
pnpm create next-app@latest team-ops
cd team-ops
pnpm dev
```

### Milestone 2: Routes And Layout

Acceptance:
- dashboard layout;
- projects list route;
- project detail route;
- loading/error/not-found states.

### Milestone 3: Data Layer

Acceptance:
- data access modules are server-only;
- Client Components receive DTOs only;
- tests prove secret fields do not leak.

### Milestone 4: Forms And Server Actions

Acceptance:
- create task form validates on server;
- pending and error states are visible;
- unauthorized mutation fails;
- cache tags update after mutation.

### Milestone 5: UI System

Acceptance:
- shared primitives;
- keyboard dialog;
- accessible form errors;
- responsive dashboard.

### Milestone 6: Testing

Acceptance:
- schema unit tests;
- Server Action tests or integration-level mutation tests;
- Playwright smoke test for critical path.

### Milestone 7: Observability

Acceptance:
- error boundary captures route failures;
- Web Vitals POST to `/api/rum`;
- server logs include request/action ids;
- mutation traces have useful names.

### Milestone 8: Production Review

Acceptance:
- build passes;
- bundle impact checked;
- secrets not exposed;
- caching decisions documented;
- rollback/deployment notes written.

---

## 6. Core Code Patterns

Server-only data:

```ts
// data/projects.ts
import 'server-only';
import { cacheLife, cacheTag } from 'next/cache';
import { db } from '@/lib/db';
import { requireSession } from '@/lib/auth';

export async function getProjectDTO(projectId: string) {
  'use cache';

  cacheLife('minutes');
  cacheTag(`project:${projectId}`);

  const session = await requireSession();

  const project = await db.project.findFirstOrThrow({
    where: {
      id: projectId,
      members: { some: { userId: session.user.id } },
    },
    select: {
      id: true,
      name: true,
      status: true,
      updatedAt: true,
    },
  });

  return project;
}
```

Server Action:

```ts
// features/tasks/actions.ts
'use server';

import { updateTag } from 'next/cache';
import { z } from 'zod';
import { requireSession } from '@/lib/auth';
import { createTask } from '@/data/tasks';

const schema = z.object({
  projectId: z.string().uuid(),
  title: z.string().min(2).max(120),
});

export async function createTaskAction(_: unknown, formData: FormData) {
  const session = await requireSession();

  const input = schema.parse({
    projectId: formData.get('projectId'),
    title: formData.get('title'),
  });

  await createTask({
    actorId: session.user.id,
    projectId: input.projectId,
    title: input.title,
  });

  updateTag(`project:${input.projectId}`);
  updateTag(`tasks:${input.projectId}`);

  return { ok: true };
}
```

Accessible form:

```tsx
'use client';

import { useActionState } from 'react';
import { useFormStatus } from 'react-dom';
import { createTaskAction } from '../actions';

function SubmitButton() {
  const { pending } = useFormStatus();

  return (
    <button type="submit" disabled={pending} aria-busy={pending || undefined}>
      {pending ? 'Creating...' : 'Create task'}
    </button>
  );
}

export function CreateTaskForm({ projectId }: { projectId: string }) {
  const [state, action] = useActionState(createTaskAction, null);

  return (
    <form action={action}>
      <input type="hidden" name="projectId" value={projectId} />
      <label htmlFor="title">Title</label>
      <input id="title" name="title" aria-invalid={Boolean(state?.error)} />
      {state?.error ? <p role="alert">{state.error}</p> : null}
      <SubmitButton />
    </form>
  );
}
```

Proxy:

```ts
// proxy.ts
import { NextResponse } from 'next/server';
import type { NextRequest } from 'next/server';

export function proxy(request: NextRequest) {
  const isDashboard = request.nextUrl.pathname.startsWith('/dashboard');
  const hasSession = Boolean(request.cookies.get('session')?.value);

  if (isDashboard && !hasSession) {
    const loginUrl = new URL('/login', request.url);
    loginUrl.searchParams.set('callbackUrl', request.nextUrl.pathname);
    return NextResponse.redirect(loginUrl);
  }

  return NextResponse.next();
}

export const config = {
  matcher: ['/dashboard/:path*'],
};
```

---

## 7. Review Rubric

| Area | Junior | Mid | Senior | PRO |
|---|---|---|---|---|
| Routing | Pages render | Nested routes | Loading/error states | Route architecture explained |
| Data | Fetches work | Server data used | DAL + DTOs | Cache/security boundaries clear |
| Forms | Submit works | Validation | Pending/error states | Optimistic + accessible + secure |
| Security | Login exists | Protected pages | Action auth/authz | Threat model documented |
| Performance | Basic build | Image/code split | Bundle checked | Web Vitals and cache measured |
| Testing | Few unit tests | Component tests | E2E smoke | Regression suite by risk |
| Observability | Console logs | Error monitoring | RUM/tracing | SLO/release health |

---

## 8. Final Interview Prompt

> Walk me through the architecture of your capstone app as if I am a staff engineer reviewing it before launch.

Strong answer structure:

```text
1. Product scope and user flows.
2. Route architecture.
3. Server/client component boundaries.
4. Data access and DTO model.
5. Auth/authz model.
6. Caching and revalidation.
7. Forms and mutation handling.
8. Error/loading/empty states.
9. Performance and bundle strategy.
10. Testing and observability.
11. Known trade-offs and future improvements.
```

---

## 9. Completion Standard

You are done only when:

```text
[ ] Another engineer can run the app.
[ ] A reviewer can understand the architecture.
[ ] A user can complete the main flow.
[ ] A failure path is visible and recoverable.
[ ] Tests protect the critical path.
[ ] Observability can explain production issues.
```

---

## 10. Revision Notes

- One-line summary: The capstone proves you can combine React, Next.js, security, performance, testing, and production thinking.
- Three keywords: route, boundary, evidence.
- One interview trap: A working demo is not the same as a production-ready app.
- One memory trick: Build it, break it, measure it, explain it.

