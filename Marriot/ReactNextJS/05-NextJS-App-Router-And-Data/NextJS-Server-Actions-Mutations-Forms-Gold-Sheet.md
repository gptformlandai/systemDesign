# Next.js Server Actions — Mutations, Forms, and Progressive Enhancement Gold Sheet

> Track Module - Group 5: Next.js App Router And Data
> Level: intermediate → senior | Server Actions, form mutations, revalidation, progressive enhancement

---

## 1. Intuition

Server Actions are async functions that run on the server but can be called from the client. They replace API routes for most mutation use cases.

```text
Before Server Actions:
  client form submit → POST /api/endpoint (separate file)
  → server validates → returns JSON → client updates state

With Server Actions:
  client form submit → directly calls server function
  → server validates → revalidates cache → client sees updated UI
  No API route file needed. No fetch() in the component.
```

The mental model: Server Actions blur the client-server boundary in a controlled, type-safe way.

---

## 2. Defining Server Actions

```tsx
// app/actions/posts.ts — dedicated actions file
'use server';

import { revalidatePath, revalidateTag } from 'next/cache';
import { redirect } from 'next/navigation';
import { z } from 'zod';

const CreatePostSchema = z.object({
  title: z.string().min(3, 'Title must be at least 3 characters'),
  content: z.string().min(10, 'Content must be at least 10 characters'),
  published: z.boolean().default(false),
});

export async function createPost(formData: FormData) {
  // 1. Parse and validate
  const raw = {
    title: String(formData.get('title')),
    content: String(formData.get('content')),
    published: formData.get('published') === 'true',
  };

  const result = CreatePostSchema.safeParse(raw);
  if (!result.success) {
    return { error: result.error.flatten().fieldErrors };
  }

  // 2. Authenticate — check session inside the action
  const session = await getSession();
  if (!session) redirect('/login');

  // 3. Persist to database
  await db.post.create({
    data: { ...result.data, authorId: session.userId },
  });

  // 4. Invalidate cache so the posts list shows the new post
  revalidatePath('/posts');
  revalidateTag('posts');

  // 5. Redirect (alternative to revalidate)
  redirect('/posts');
}
```

**Rules for Server Actions:**
- Must be in a file with `'use server'` at the top, OR be an async function with `'use server'` as the first line
- They execute on the server — never in the browser
- They have access to server-only resources (DB, filesystem, environment variables)
- They can be passed as props to Client Components

---

## 3. Using Server Actions with HTML Forms

The simplest integration — no JavaScript required (progressive enhancement):

```tsx
// app/posts/new/page.tsx
import { createPost } from '@/actions/posts';

export default function NewPostPage() {
  return (
    <form action={createPost}>
      <div>
        <label htmlFor="title">Title</label>
        <input id="title" name="title" type="text" required />
      </div>
      <div>
        <label htmlFor="content">Content</label>
        <textarea id="content" name="content" required />
      </div>
      <button type="submit">Create Post</button>
    </form>
  );
}
```

This form works WITHOUT JavaScript. The `action={createPost}` tells Next.js to call `createPost` on the server when the form submits. This is progressive enhancement — if JS fails to load, the form still works.

---

## 4. useActionState — Form State + Pending + Errors

`useActionState` (React 19 / Next.js 14+) gives you form state, pending status, and the ability to return validation errors back to the UI:

```tsx
'use client';

import { useActionState } from 'react';
import { createPost } from '@/actions/posts';

type FormState = {
  error?: { title?: string[]; content?: string[] };
  message?: string;
};

const initialState: FormState = {};

export function CreatePostForm() {
  const [state, formAction, isPending] = useActionState(createPost, initialState);

  return (
    <form action={formAction}>
      <div>
        <input name="title" placeholder="Post title" />
        {state.error?.title && (
          <p style={{color: 'red'}}>{state.error.title[0]}</p>
        )}
      </div>

      <div>
        <textarea name="content" placeholder="Post content" />
        {state.error?.content && (
          <p style={{color: 'red'}}>{state.error.content[0]}</p>
        )}
      </div>

      <button type="submit" disabled={isPending}>
        {isPending ? 'Creating...' : 'Create Post'}
      </button>

      {state.message && <p>{state.message}</p>}
    </form>
  );
}
```

```tsx
// Updated server action to return state instead of redirecting on error
'use server';

export async function createPost(prevState: FormState, formData: FormData): Promise<FormState> {
  const result = CreatePostSchema.safeParse({
    title: String(formData.get('title')),
    content: String(formData.get('content')),
  });

  if (!result.success) {
    return { error: result.error.flatten().fieldErrors };
  }

  try {
    await db.post.create({ data: result.data });
    revalidatePath('/posts');
    return { message: 'Post created successfully!' };
  } catch {
    return { message: 'Database error. Please try again.' };
  }
}
```

---

## 5. useFormStatus — Pending State Anywhere in the Form

`useFormStatus` reads the pending state of the nearest form ancestor. Use it in child components within the form:

```tsx
'use client';

import { useFormStatus } from 'react-dom';

// Reusable submit button — works with any form action
function SubmitButton({ label = 'Submit', pendingLabel = 'Saving...' }: { label?: string; pendingLabel?: string }) {
  const { pending } = useFormStatus();

  return (
    <button type="submit" disabled={pending} aria-disabled={pending}>
      {pending ? pendingLabel : label}
    </button>
  );
}

// Use inside any form with a server action
<form action={createPost}>
  <input name="title" />
  <SubmitButton label="Create Post" pendingLabel="Creating..." />
</form>
```

---

## 6. useOptimistic — Instant UI Updates

Show the new state immediately while the server action completes in the background:

```tsx
'use client';

import { useOptimistic, startTransition } from 'react';
import { togglePostLike } from '@/actions/posts';

function LikeButton({ postId, initialLikeCount, initialLiked }: {
  postId: string;
  initialLikeCount: number;
  initialLiked: boolean;
}) {
  const [optimisticState, updateOptimistic] = useOptimistic(
    { likeCount: initialLikeCount, liked: initialLiked },
    (current, newLiked: boolean) => ({
      likeCount: newLiked ? current.likeCount + 1 : current.likeCount - 1,
      liked: newLiked,
    }),
  );

  async function handleLike() {
    const newLiked = !optimisticState.liked;

    startTransition(() => {
      updateOptimistic(newLiked);  // immediate UI update
    });

    await togglePostLike(postId, newLiked);  // server action — if it fails, state reverts
  }

  return (
    <button onClick={handleLike}>
      {optimisticState.liked ? '❤️' : '🤍'} {optimisticState.likeCount}
    </button>
  );
}
```

---

## 7. Inline Server Actions vs Action Files

```tsx
// Inline — define directly in Server Component (simple cases)
async function DeleteButton({ id }: { id: string }) {
  async function deletePost() {
    'use server';  // inline server action directive
    await db.post.delete({ where: { id } });
    revalidatePath('/posts');
  }

  return (
    <form action={deletePost}>
      <button type="submit">Delete</button>
    </form>
  );
}

// Dedicated actions file — for complex actions, sharing, testing
// app/actions/posts.ts
'use server';
export async function deletePost(id: string) { ... }

// When to use inline vs file:
// Inline: one-off actions, simple delete/update, no reuse
// File: shared across multiple components, complex logic, needs testing
```

---

## 8. File Uploads via Server Actions

```tsx
'use server';

export async function uploadAvatar(formData: FormData) {
  const file = formData.get('avatar') as File;

  if (!file || file.size === 0) {
    return { error: 'No file selected' };
  }

  if (file.size > 5 * 1024 * 1024) {  // 5MB limit
    return { error: 'File too large (max 5MB)' };
  }

  if (!['image/jpeg', 'image/png', 'image/webp'].includes(file.type)) {
    return { error: 'Only JPEG, PNG, and WebP files are accepted' };
  }

  const bytes = await file.arrayBuffer();
  const buffer = Buffer.from(bytes);

  // Upload to S3/R2/Cloudinary
  const url = await uploadToStorage(buffer, file.name, file.type);

  await db.user.update({ where: { id: session.userId }, data: { avatarUrl: url } });
  revalidatePath('/profile');

  return { success: true, url };
}

// Form component
<form action={uploadAvatar} encType="multipart/form-data">
  <input type="file" name="avatar" accept="image/*" />
  <SubmitButton label="Upload" />
</form>
```

---

## 9. Revalidation Strategies

After a mutation, you need to tell Next.js what to refresh:

```tsx
'use server';

// Option 1: Revalidate a URL path
revalidatePath('/posts');           // revalidate /posts
revalidatePath('/posts/[id]', 'page');  // revalidate specific page type

// Option 2: Revalidate by cache tag
revalidateTag('posts');             // revalidates all fetch() calls tagged with 'posts'
revalidateTag(`post-${id}`);        // revalidates one specific post

// Option 3: Redirect — navigate away after mutation
redirect('/posts');                 // throws an internal Next.js redirect
redirect('/posts', RedirectType.replace);  // replace history instead of push

// In Server Components — tag your fetches for targeted revalidation
const posts = await fetch('/api/posts', { next: { tags: ['posts'] } });
const post = await fetch(`/api/posts/${id}`, { next: { tags: [`post-${id}`, 'posts'] } });
```

---

## 10. Security Considerations

Server Actions are public endpoints — treat them like API routes:

```tsx
'use server';

export async function updatePost(id: string, data: UpdateData) {
  // 1. Always authenticate inside the action — do not trust client-passed userId
  const session = await getSession();
  if (!session) throw new Error('Unauthorized');

  // 2. Always authorize — does this user own this resource?
  const post = await db.post.findUnique({ where: { id } });
  if (!post || post.authorId !== session.userId) throw new Error('Forbidden');

  // 3. Always validate input — never trust FormData values directly
  const result = UpdatePostSchema.safeParse(data);
  if (!result.success) return { error: result.error.flatten().fieldErrors };

  // 4. CSRF protection — Next.js handles this automatically for Server Actions
  // (Origin validation + SameSite cookies)
  
  await db.post.update({ where: { id }, data: result.data });
  revalidatePath(`/posts/${id}`);
}
```

---

## 11. Common Mistakes

| Mistake | Why Wrong | Fix |
|---|---|---|
| Trusting `userId` from FormData | User can forge it | Get userId from server-side session |
| No input validation in action | SQL injection, bad data | Validate with Zod before DB call |
| Using action for reads | Actions run on POST, not GET | Use Server Component async fetch for reads |
| Missing `revalidatePath` after mutation | Stale cache shown to user | Always revalidate affected paths/tags |
| Throwing errors from action to client | Security — reveals server internals | Return error objects, not throw |
| Calling Server Action with GET | Server Actions only accept POST | Use route handlers for GET endpoints |

---

## 12. Strong Interview Answer

**Q: When would you use Server Actions instead of API routes in Next.js?**

```text
Server Actions are the right choice for mutations — creating, updating, or deleting
data — where the action originates from a form or a user interaction. They integrate
directly with React's useActionState and useFormStatus, providing pending state and
validation errors without writing a client-side fetch. They also enable progressive
enhancement — forms with server actions work even without JavaScript.

For API routes, I still use route handlers when I need a public API consumed by
external clients, mobile apps, or third-party services; when the response must be
JSON and the caller is not React; or when I need fine-grained control over HTTP
method, headers, and response format.

Security-wise, server actions are treated the same as API routes — authentication
and authorization must happen inside the action, and all input must be validated
before touching the database.
```

---

## 13. Revision Notes

- `'use server'` at file top marks all exports as server actions; inside an async function marks just that function
- `useActionState(action, initialState)` → `[state, formAction, isPending]`
- `useFormStatus()` inside a form child component reads `{ pending }` from the nearest form ancestor
- `useOptimistic` + `startTransition` → instant UI update, auto-reverts if server action fails
- Always authenticate and authorize INSIDE the action — never trust client-sent user IDs
- Progressive enhancement: server action forms work without JavaScript
- Revalidation: `revalidatePath(path)` or `revalidateTag(tag)` after mutation — pick the most targeted option
