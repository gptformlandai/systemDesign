# React Forms Complete - Gold Sheet

> Track File #6 of 24 - Group 4: Forms, Client State, And Server State
> Covers: controlled/uncontrolled forms, validation, React Hook Form, Formik, large-form performance

---

## 1. Intuition

Forms are mini state machines.

```text
idle -> editing -> validating -> submitting -> success
                                  -> field_error
                                  -> server_error
```

A good form handles data entry, validation, accessibility, keyboard behavior, submission, server failures, and recovery.

---

## 2. Controlled Forms

React owns the value.

```tsx
function LoginForm() {
  const [email, setEmail] = useState('');

  return (
    <input
      value={email}
      onChange={event => setEmail(event.target.value)}
    />
  );
}
```

Pros:
- easy conditional UI
- easy validation while typing
- predictable React state

Cons:
- re-render on every keystroke
- can be costly in large forms

---

## 3. Uncontrolled Forms

DOM owns the value.

```tsx
function EmailForm() {
  const ref = useRef<HTMLInputElement>(null);

  function submit() {
    const email = ref.current?.value;
  }

  return <input ref={ref} name="email" />;
}
```

Useful for:
- file inputs
- simple submit-only forms
- libraries that minimize render cost

---

## 4. Validation Patterns

Types:
- required field validation
- schema validation
- async validation
- server validation
- cross-field validation

Schema-like example:

```ts
type SignupForm = {
  email: string;
  password: string;
};

function validateSignup(form: SignupForm) {
  return {
    email: form.email.includes('@') ? undefined : 'Enter a valid email.',
    password: form.password.length >= 8 ? undefined : 'Use 8+ characters.',
  };
}
```

Production rule:
Client validation improves UX. Server validation enforces truth.

---

## 5. React Hook Form

React Hook Form is popular because it uses uncontrolled inputs by default and reduces re-render pressure.

```tsx
import {useForm} from 'react-hook-form';

type LoginValues = {
  email: string;
  password: string;
};

function LoginForm() {
  const {register, handleSubmit, formState} = useForm<LoginValues>();

  function onSubmit(values: LoginValues) {
    return login(values);
  }

  return (
    <form onSubmit={handleSubmit(onSubmit)}>
      <input {...register('email', {required: true})} />
      <input type="password" {...register('password', {required: true})} />
      <button disabled={formState.isSubmitting}>Login</button>
    </form>
  );
}
```

Good for:
- large forms
- performance-sensitive forms
- field-level validation
- integrating schema resolvers

---

## 6. Formik

Formik uses a more controlled-state mental model.

Good for:
- teams already invested in Formik
- smaller/medium forms
- explicit form state

Trade-off:
It can re-render more unless optimized carefully.

Interview answer:
React Hook Form is often preferred for large modern forms because it minimizes re-rendering, while Formik remains common in existing codebases.

---

## 7. Large Form Performance

Problems:
- every keystroke re-renders whole form
- expensive validation on change
- huge select lists
- dynamic field arrays
- async validation races

Fixes:
- field-level subscriptions
- uncontrolled inputs
- debounced validation
- memoized field components
- split form sections
- avoid global form state for every keystroke
- validate on blur/submit where appropriate

---

## 8. Real-World Use Cases

- Checkout form: schema validation plus server payment validation.
- Admin entity editor: dynamic fields and server errors.
- Search form: controlled input with debounce.
- File upload form: uncontrolled file input.
- Multi-step onboarding: reducer/state machine.

---

## 9. Common Mistakes

| Mistake | Why It Is Wrong | Better Approach |
|---|---|---|
| Only client validation | Can be bypassed | Validate on server too |
| Validating expensive schema on every key | Jank | Debounce or validate on blur |
| One state object for huge form in parent | Re-renders everything | Use form library/field subscriptions |
| Losing server errors on edit | Bad UX | Track field and server error separately |
| Inaccessible errors | Screen readers miss issues | Link errors with labels/aria |

---

## 10. Strong Interview Answer

Question:
How do you handle forms in React?

Strong answer:

```text
For small forms, controlled inputs are simple and predictable. For large or
performance-sensitive forms, I prefer React Hook Form because uncontrolled inputs
and field subscriptions reduce re-renders. I separate client validation from
server validation, model submitting/error states, make errors accessible, and
avoid running expensive validation on every keystroke unless the UX requires it.
```

---

## 11. Revision Notes

- One-line summary: Forms are state machines with validation, submission, errors, and accessibility.
- Three keywords: controlled, validation, subscription.
- One interview trap: Client validation is not security.
- One memory trick: Small form can be controlled; big form needs field-level thinking.

