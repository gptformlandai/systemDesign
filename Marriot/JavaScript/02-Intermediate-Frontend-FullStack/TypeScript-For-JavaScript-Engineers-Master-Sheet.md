# TypeScript For JavaScript Engineers Master Sheet

Target: JavaScript engineers preparing for frontend, full-stack, Node.js, React, MAANG, and production interviews where TypeScript safety, type-system reasoning, generics, narrowing, utility types, and API modeling are tested.

This sheet covers:
- TypeScript mental model
- Type annotations and inference
- `any`, `unknown`, `never`, `void`
- Object types, interfaces, and type aliases
- Optional properties and strict null checks
- Union and intersection types
- Type narrowing and type guards
- Literal types and discriminated unions
- Functions, callbacks, overloads, and async types
- Arrays, tuples, readonly types
- Generics from beginner to interview level
- Constraints, defaults, `keyof`, indexed access, mapped types, conditional types
- Utility types
- `as const`, `satisfies`, and safe assertions
- Classes and access modifiers
- Module typing
- React and Node.js typing awareness
- API response modeling
- Runtime validation gap
- tsconfig production settings
- Common interview traps
- Mini programs and FAANG scenarios

How to use this:
- Learn TypeScript as a design tool, not just syntax.
- Always separate compile-time types from runtime JavaScript behavior.
- Practice explaining how a type prevents a real production bug.
- For interviews, prefer clear, maintainable types before clever type gymnastics.

---

## 1. Mental Model

TypeScript is JavaScript plus a static type system.

```text
JavaScript decides what happens at runtime.
TypeScript checks whether your code is probably safe before runtime.
```

TypeScript does not change JavaScript runtime behavior by itself.

```typescript
const value: number = 10;
```

At runtime, the type annotation is erased.

```javascript
const value = 10;
```

Core idea:

```text
TypeScript helps catch mistakes while writing, reviewing, refactoring, and building code.
It is not a runtime validator unless you add runtime validation.
```

Strong interview line:

```text
TypeScript gives compile-time guarantees on top of JavaScript. It improves maintainability,
refactoring safety, API contracts, and editor feedback, but runtime inputs still need validation.
```

---

## 2. Interview Priority Meter

| Topic | Priority | Why It Matters |
|---|---:|---|
| Type inference | Very high | Write clean TS without over-annotating |
| `any` vs `unknown` | Very high | Safety boundary questions |
| Union types | Very high | Models real branching states |
| Narrowing | Very high | Core TypeScript reasoning |
| Discriminated unions | Very high | Production state modeling |
| Interfaces vs type aliases | High | Common interview question |
| Generics | Very high | Reusable typed functions/components |
| `keyof` | High | Generic object utilities |
| Indexed access types | High | Extract property types |
| Mapped types | High | Transform object types |
| Conditional types | Medium-high | Advanced utility types |
| Utility types | Very high | Daily production TS |
| `Partial`, `Pick`, `Omit` | Very high | API DTO modeling |
| `Record` | High | Dictionary shapes |
| `as const` | High | Literal inference |
| `satisfies` | High | Config/object validation without widening too much |
| Type assertions | High | Useful but risky |
| Runtime validation gap | Very high | Senior production maturity |
| tsconfig strict mode | Very high | Real project quality |
| React typing | High | Frontend interviews |
| Node/API typing | High | Full-stack interviews |
| Declaration files | Medium | Library integration |
| Type-level cleverness | Medium | Know limits and avoid overuse |

---

## 3. Why TypeScript Exists

JavaScript is flexible, but flexibility can hide bugs.

JavaScript example:

```javascript
function calculateTotal(items) {
    return items.reduce((sum, item) => sum + item.amount, 0);
}

calculateTotal(null); // runtime crash
calculateTotal([{ amount: "100" }]); // wrong behavior risk
```

TypeScript version:

```typescript
type LineItem = {
    amount: number;
};

function calculateTotal(items: LineItem[]): number {
    return items.reduce((sum, item) => sum + item.amount, 0);
}
```

Benefits:

- Catches type mismatches before runtime.
- Documents function contracts.
- Improves autocomplete and navigation.
- Makes refactoring safer.
- Helps large teams coordinate API shapes.
- Prevents many null/undefined and wrong-property bugs.

What it does not solve automatically:

- Bad business logic.
- Runtime API payload shape mismatch.
- Incorrect assumptions about external systems.
- Race conditions.
- Security validation.
- Performance problems.

Strong answer:

```text
TypeScript exists because large JavaScript codebases need safer contracts. It catches many
mistakes before runtime and improves refactoring, but it complements testing and runtime
validation rather than replacing them.
```

---

## 4. TypeScript Is Structural

TypeScript uses structural typing.

```typescript
type User = {
    id: string;
    name: string;
};

type Customer = {
    id: string;
    name: string;
};

const user: User = { id: "U1", name: "Ava" };
const customer: Customer = user;
```

This works because shapes match.

Structural typing means:

```text
If it has the required shape, it is compatible.
```

Not nominal by default:

```text
Type names alone do not create separate runtime identities.
```

Production implication:

```text
Two IDs that are both strings are interchangeable unless you add stronger modeling.
```

Example problem:

```typescript
type UserId = string;
type BookingId = string;

function loadBooking(id: BookingId) {
    return id;
}

const userId: UserId = "U1";
loadBooking(userId); // allowed because both are string
```

Branded type pattern:

```typescript
type Brand<T, Name extends string> = T & { readonly __brand: Name };

type UserId = Brand<string, "UserId">;
type BookingId = Brand<string, "BookingId">;

function loadBooking(id: BookingId) {
    return id;
}
```

Interview line:

```text
TypeScript is structurally typed, so compatibility is based on shape, not class or type name.
For domain IDs, I may use branded types when accidental interchange is risky.
```

---

## 5. Type Annotations

Basic annotations:

```typescript
const name: string = "Ava";
const age: number = 30;
const active: boolean = true;
```

Function parameters and return:

```typescript
function add(a: number, b: number): number {
    return a + b;
}
```

Object annotation:

```typescript
const booking: {
    id: string;
    amount: number;
    confirmed: boolean;
} = {
    id: "B1",
    amount: 200,
    confirmed: true
};
```

Better with named type:

```typescript
type Booking = {
    id: string;
    amount: number;
    confirmed: boolean;
};

const booking: Booking = {
    id: "B1",
    amount: 200,
    confirmed: true
};
```

Strong answer:

```text
I annotate function boundaries and important domain models. Inside simple expressions, I often
let TypeScript infer types to keep code clean.
```

---

## 6. Type Inference

TypeScript can infer many types.

```typescript
const name = "Ava";
const amount = 200;
const confirmed = true;
```

Inferred:

```text
name: "Ava" or string depending context
amount: 200 or number depending context
confirmed: true or boolean depending context
```

Function return inference:

```typescript
function calculateTotal(items: { amount: number }[]) {
    return items.reduce((sum, item) => sum + item.amount, 0);
}
```

Return type inferred as `number`.

Good use of explicit return type:

```typescript
function parseAmount(input: string): number {
    return Number(input);
}
```

Why annotate public boundaries:

- Documents intent.
- Prevents accidental return shape drift.
- Improves API clarity.
- Helps reviewers.

Interview line:

```text
I rely on inference for local variables but annotate public function boundaries, exported APIs,
and domain models where intent matters.
```

---

## 7. Primitive Types

Common primitives:

```typescript
const text: string = "hello";
const count: number = 10;
const enabled: boolean = true;
const big: bigint = 100n;
const key: symbol = Symbol("key");
const missing: null = null;
const absent: undefined = undefined;
```

Important JavaScript connection:

```text
TypeScript number is JavaScript number: floating-point double, not int/float split.
```

Avoid boxed types:

```typescript
let a: string;
let b: String; // avoid
```

Use lowercase primitives:

```text
string, number, boolean, bigint, symbol
```

Strong answer:

```text
TypeScript primitive types mirror JavaScript primitives. I use lowercase primitive types and
avoid boxed object types like String or Number.
```

---

## 8. Arrays

Array syntax:

```typescript
const ids: string[] = ["B1", "B2"];
const amounts: Array<number> = [100, 200];
```

Array of objects:

```typescript
type Booking = {
    id: string;
    amount: number;
};

const bookings: Booking[] = [
    { id: "B1", amount: 100 },
    { id: "B2", amount: 200 }
];
```

Readonly array:

```typescript
function sum(amounts: readonly number[]): number {
    return amounts.reduce((total, amount) => total + amount, 0);
}
```

This prevents mutation inside the function:

```typescript
function bad(amounts: readonly number[]) {
    // amounts.push(10); // error
}
```

Interview line:

```text
I use readonly arrays for function inputs when the function should not mutate caller-owned data.
It communicates intent and prevents accidental side effects.
```

---

## 9. Tuples

Tuples represent fixed-position arrays.

```typescript
type Point = [number, number];

const point: Point = [10, 20];
```

Named tuple elements:

```typescript
type Range = [start: number, end: number];
```

Tuple from function:

```typescript
function useToggle(initial = false): [boolean, () => void] {
    let value = initial;

    return [
        value,
        () => {
            value = !value;
        }
    ];
}
```

Readonly tuple:

```typescript
type Coordinates = readonly [latitude: number, longitude: number];
```

Production caution:

```text
Tuples are good when positions are obvious. If many fields exist, an object type is clearer.
```

---

## 10. Object Types

Object type:

```typescript
type User = {
    id: string;
    name: string;
    email: string;
};
```

Optional property:

```typescript
type User = {
    id: string;
    name: string;
    email?: string;
};
```

Readonly property:

```typescript
type User = {
    readonly id: string;
    name: string;
};
```

Index signature:

```typescript
type CountsByStatus = {
    [status: string]: number;
};
```

Better when keys are known:

```typescript
type BookingStatus = "CREATED" | "CONFIRMED" | "CANCELLED";

type CountsByStatus = Record<BookingStatus, number>;
```

Strong answer:

```text
I model object shapes with required, optional, and readonly properties. If keys are known, I
prefer literal unions and Record over a loose string index signature.
```

---

## 11. Optional Properties

Optional property:

```typescript
type User = {
    id: string;
    nickname?: string;
};
```

Reading optional property:

```typescript
function display(user: User): string {
    return user.nickname ?? user.id;
}
```

Important:

```text
nickname?: string means the property may be absent. With strict settings, reading it gives
string | undefined.
```

Common mistake:

```typescript
function upper(user: User): string {
    return user.nickname.toUpperCase(); // error under strict null checks
}
```

Correct:

```typescript
function upper(user: User): string {
    return user.nickname?.toUpperCase() ?? "UNKNOWN";
}
```

Interview line:

```text
Optional properties force me to handle absence. With strict null checks, TypeScript prevents
calling methods on possibly undefined values.
```

---

## 12. Interfaces

Interface:

```typescript
interface User {
    id: string;
    name: string;
}
```

Extending interface:

```typescript
interface AdminUser extends User {
    permissions: string[];
}
```

Declaration merging:

```typescript
interface Window {
    appVersion?: string;
}
```

Good uses:

- Object shapes.
- Public library extension points.
- Class contracts.
- Declaration merging when needed.

Interview line:

```text
Interfaces are good for object contracts and extension. They can be extended and declaration-merged,
which is useful for some library and global type scenarios.
```

---

## 13. Type Aliases

Type alias:

```typescript
type User = {
    id: string;
    name: string;
};
```

Union alias:

```typescript
type Status = "CREATED" | "CONFIRMED" | "CANCELLED";
```

Function type:

```typescript
type Predicate<T> = (value: T) => boolean;
```

Tuple type:

```typescript
type Point = [number, number];
```

Good uses:

- Unions.
- Intersections.
- Tuples.
- Mapped/conditional types.
- Function types.
- Object shapes when no merging is needed.

Strong answer:

```text
I use interfaces for extendable object contracts and type aliases for unions, tuples, mapped
types, conditional types, and function aliases. In many object-shape cases either works, so I
follow the codebase convention.
```

---

## 14. Interface vs Type Alias

| Feature | Interface | Type Alias |
|---|---|---|
| Object shape | Yes | Yes |
| Extends | Yes | Via intersection |
| Declaration merging | Yes | No |
| Union type | No | Yes |
| Tuple type | No | Yes |
| Mapped type | No | Yes |
| Conditional type | No | Yes |

Examples:

```typescript
interface User {
    id: string;
}

interface Admin extends User {
    role: "admin";
}
```

```typescript
type User = {
    id: string;
};

type Admin = User & {
    role: "admin";
};
```

Interview line:

```text
For simple object models, interface and type are often both fine. The real difference is that
interfaces support declaration merging, while type aliases can represent unions, tuples, mapped
and conditional types.
```

---

## 15. `any`

`any` disables type checking for a value.

```typescript
let value: any = "hello";
value.toUpperCase();
value.non.existing.method(); // TypeScript allows this
```

Why dangerous:

```text
any spreads unsafety. Once a value is any, TypeScript stops protecting you around it.
```

Use cases where it may appear:

- Migrating legacy JavaScript.
- Untyped third-party libraries.
- Temporary escape hatch.
- Generated code boundaries.

Better default for unknown input:

```typescript
let value: unknown = JSON.parse(input);
```

Strong answer:

```text
any is an escape hatch that turns off type safety. I avoid it in domain code and prefer unknown
at boundaries because unknown forces narrowing before use.
```

---

## 16. `unknown`

`unknown` means a value exists but its type is not known yet.

```typescript
function handle(value: unknown) {
    // value.toUpperCase(); // error

    if (typeof value === "string") {
        return value.toUpperCase();
    }

    return "unsupported";
}
```

Use for:

- JSON parsing.
- External API responses before validation.
- Catch errors.
- Message events.
- Local storage values.

Example:

```typescript
function parseJson(input: string): unknown {
    return JSON.parse(input);
}
```

Then validate or narrow.

Strong answer:

```text
unknown is safer than any because it forces me to prove the type before using the value. I use
unknown at trust boundaries.
```

---

## 17. `never`

`never` represents a value that should never exist.

Function that never returns:

```typescript
function fail(message: string): never {
    throw new Error(message);
}
```

Exhaustiveness check:

```typescript
type Status = "CREATED" | "CONFIRMED" | "CANCELLED";

function label(status: Status): string {
    switch (status) {
        case "CREATED":
            return "Created";
        case "CONFIRMED":
            return "Confirmed";
        case "CANCELLED":
            return "Cancelled";
        default: {
            const exhaustive: never = status;
            return exhaustive;
        }
    }
}
```

If a new status is added, TypeScript flags the default branch.

Strong answer:

```text
never is useful for impossible states and exhaustive checks. It helps ensure every union case
is handled.
```

---

## 18. `void`

`void` means a function does not return a useful value.

```typescript
function log(message: string): void {
    console.log(message);
}
```

Callback example:

```typescript
type ClickHandler = (event: MouseEvent) => void;
```

Important:

```text
void is not the same as never. void functions return normally without meaningful value. never
functions do not complete normally.
```

Async void confusion:

```typescript
async function save(): Promise<void> {
    await persist();
}
```

Async function returning no value returns `Promise<void>`.

---

## 19. Union Types

Union means value can be one of several types.

```typescript
type Id = string | number;
```

Literal union:

```typescript
type BookingStatus = "CREATED" | "CONFIRMED" | "CANCELLED";
```

Use:

```typescript
function canCancel(status: BookingStatus): boolean {
    return status === "CREATED" || status === "CONFIRMED";
}
```

Union of objects:

```typescript
type ApiResult =
    | { success: true; data: string[] }
    | { success: false; error: string };
```

Strong answer:

```text
Unions model values that can take multiple valid shapes. They are especially powerful with
narrowing and discriminated unions.
```

---

## 20. Intersection Types

Intersection combines types.

```typescript
type User = {
    id: string;
    name: string;
};

type Audited = {
    createdAt: Date;
    updatedAt: Date;
};

type AuditedUser = User & Audited;
```

Use:

```typescript
const user: AuditedUser = {
    id: "U1",
    name: "Ava",
    createdAt: new Date(),
    updatedAt: new Date()
};
```

Caution:

```typescript
type A = { id: string };
type B = { id: number };
type C = A & B;
```

`C["id"]` becomes impossible-like because `string & number` cannot exist.

Interview line:

```text
Intersections combine requirements. They are useful for composing object capabilities, but
conflicting property types can produce impossible types.
```

---

## 21. Type Narrowing

Narrowing means TypeScript reduces a broad type to a specific type after checks.

```typescript
function format(value: string | number): string {
    if (typeof value === "string") {
        return value.toUpperCase();
    }

    return value.toFixed(2);
}
```

Common narrowing tools:

- `typeof`
- `instanceof`
- `in`
- equality checks
- truthiness checks
- discriminant properties
- custom type guards

Strong answer:

```text
Narrowing is how TypeScript lets us safely use union values. Runtime checks inform the compiler
which branch-specific type is valid.
```

---

## 22. Truthiness Narrowing Trap

Truthiness can accidentally reject valid values.

```typescript
function displayCount(count: number | undefined): string {
    if (count) {
        return String(count);
    }

    return "missing";
}
```

Problem:

```text
0 is valid but falsy, so this returns missing.
```

Better:

```typescript
function displayCount(count: number | undefined): string {
    if (count !== undefined) {
        return String(count);
    }

    return "missing";
}
```

Strong answer:

```text
I avoid broad truthiness checks when 0, false, or empty string are valid values. I use explicit
null or undefined checks instead.
```

---

## 23. Discriminated Unions

A discriminated union uses a shared literal property to distinguish cases.

```typescript
type LoadState<T> =
    | { status: "idle" }
    | { status: "loading" }
    | { status: "success"; data: T }
    | { status: "error"; error: string };
```

Usage:

```typescript
function renderUsers(state: LoadState<string[]>): string {
    switch (state.status) {
        case "idle":
            return "Nothing loaded";
        case "loading":
            return "Loading";
        case "success":
            return state.data.join(", ");
        case "error":
            return state.error;
    }
}
```

Why powerful:

```text
TypeScript knows data exists only in the success branch and error exists only in the error branch.
```

Strong answer:

```text
Discriminated unions are one of the best TypeScript tools for modeling UI and API states because
they prevent impossible combinations like loading with data and error at the same time.
```

---

## 24. Exhaustive Switch

Use `never` to ensure every union case is handled.

```typescript
type PaymentStatus = "PENDING" | "PAID" | "FAILED";

function getPaymentLabel(status: PaymentStatus): string {
    switch (status) {
        case "PENDING":
            return "Pending";
        case "PAID":
            return "Paid";
        case "FAILED":
            return "Failed";
        default:
            return assertNever(status);
    }
}

function assertNever(value: never): never {
    throw new Error(`Unexpected value: ${value}`);
}
```

If `REFUNDED` is added to `PaymentStatus`, the switch becomes incomplete.

Production value:

```text
This prevents missing UI states, status transitions, and API cases during refactors.
```

---

## 25. Custom Type Guards

A type guard returns a type predicate.

```typescript
type User = {
    id: string;
    name: string;
};

function isUser(value: unknown): value is User {
    return typeof value === "object"
        && value !== null
        && "id" in value
        && "name" in value;
}
```

Use:

```typescript
function handle(value: unknown) {
    if (isUser(value)) {
        console.log(value.name);
    }
}
```

Caution:

```text
A type guard is only as correct as its runtime logic. TypeScript trusts your predicate.
```

Better guard:

```typescript
function isUser(value: unknown): value is User {
    if (typeof value !== "object" || value === null) {
        return false;
    }

    const candidate = value as Record<string, unknown>;

    return typeof candidate.id === "string"
        && typeof candidate.name === "string";
}
```

Strong answer:

```text
Custom type guards bridge runtime checks and compile-time narrowing, but they must be implemented
carefully because TypeScript trusts the predicate.
```

---

## 26. Type Assertions

Type assertion tells TypeScript to treat a value as a type.

```typescript
const element = document.getElementById("root") as HTMLElement;
```

Risk:

```text
Assertion does not add runtime checking.
```

Unsafe:

```typescript
const value = JSON.parse(input) as User;
```

This compiles even if input is not a user.

Safer:

```typescript
const value: unknown = JSON.parse(input);

if (!isUser(value)) {
    throw new Error("invalid user");
}
```

Interview line:

```text
Type assertions are escape hatches. I use them when I know more than the compiler, but I avoid
using assertions to skip validation at external boundaries.
```

---

## 27. `as const`

`as const` preserves literal types and readonly structure.

```typescript
const statuses = ["CREATED", "CONFIRMED", "CANCELLED"] as const;

type BookingStatus = typeof statuses[number];
```

Result:

```text
BookingStatus = "CREATED" | "CONFIRMED" | "CANCELLED"
```

Object example:

```typescript
const routes = {
    home: "/",
    bookings: "/bookings"
} as const;
```

Properties become readonly literal values.

Strong answer:

```text
as const is useful when I want TypeScript to preserve exact literal values, often to derive a
union type from an array or config object.
```

---

## 28. `satisfies`

`satisfies` checks that a value matches a type without losing useful inference.

```typescript
type RouteConfig = Record<string, {
    path: string;
    requiresAuth: boolean;
}>;

const routes = {
    home: { path: "/", requiresAuth: false },
    dashboard: { path: "/dashboard", requiresAuth: true }
} satisfies RouteConfig;
```

Why useful:

```text
The object must satisfy RouteConfig, but TypeScript still remembers the exact keys home and dashboard.
```

Compared with direct annotation:

```typescript
const routes: RouteConfig = {
    home: { path: "/", requiresAuth: false }
};
```

This may widen keys to `string` in ways that lose specificity.

Strong answer:

```text
satisfies is great for config objects because it validates the required shape while preserving
specific inferred keys and values for later type use.
```

---

## 29. Function Types

Function type alias:

```typescript
type Mapper<T, U> = (value: T) => U;
```

Callback parameter:

```typescript
function mapValues<T, U>(items: T[], mapper: Mapper<T, U>): U[] {
    return items.map(mapper);
}
```

Function property:

```typescript
type ButtonProps = {
    label: string;
    onClick: () => void;
};
```

Optional callback:

```typescript
type Options = {
    onSuccess?: (id: string) => void;
};
```

Call safely:

```typescript
options.onSuccess?.("B1");
```

Strong answer:

```text
I type callbacks by describing parameters and return values. For event handlers or public APIs,
clear callback types prevent incorrect callers and incorrect implementers.
```

---

## 30. Function Overloads

Overloads describe multiple call signatures.

```typescript
function format(value: string): string;
function format(value: number): string;
function format(value: string | number): string {
    if (typeof value === "string") {
        return value.trim();
    }

    return value.toFixed(2);
}
```

Use overloads when return type depends on input shape.

Example:

```typescript
function getValue(key: "count"): number;
function getValue(key: "name"): string;
function getValue(key: "count" | "name"): number | string {
    return key === "count" ? 1 : "Ava";
}
```

Caution:

```text
Do not overuse overloads when generics or union return types are clearer.
```

---

## 31. Generics: Basic

Generics let types be parameters.

```typescript
function identity<T>(value: T): T {
    return value;
}

const a = identity("hello");
const b = identity(123);
```

Array helper:

```typescript
function first<T>(items: T[]): T | undefined {
    return items[0];
}
```

Why useful:

```text
The function works for many types while preserving the relationship between input and output.
```

Strong answer:

```text
Generics are for reusable code where the exact type is not known upfront but relationships
between types must be preserved.
```

---

## 32. Generic Constraints

Constraints restrict generic types.

```typescript
function getId<T extends { id: string }>(value: T): string {
    return value.id;
}
```

Now `T` must have an `id` property.

Example:

```typescript
const user = { id: "U1", name: "Ava" };
const id = getId(user);
```

Constraint with keys:

```typescript
function getProperty<T, K extends keyof T>(object: T, key: K): T[K] {
    return object[key];
}
```

Use:

```typescript
const user = { id: "U1", name: "Ava" };
const name = getProperty(user, "name");
```

Strong answer:

```text
Generic constraints let me keep a function reusable while requiring the minimum shape needed
for safe implementation.
```

---

## 33. Generic Defaults

Generic type parameters can have defaults.

```typescript
type ApiResponse<TData = unknown> = {
    data: TData;
    requestId: string;
};
```

Use default:

```typescript
const response: ApiResponse = {
    data: { anything: true },
    requestId: "R1"
};
```

Use specific type:

```typescript
type UserResponse = ApiResponse<{ id: string; name: string }>;
```

Strong answer:

```text
Generic defaults make reusable types easier to use while still allowing callers to provide a
specific type when needed.
```

---

## 34. `keyof`

`keyof` creates a union of property keys.

```typescript
type User = {
    id: string;
    name: string;
    email: string;
};

type UserKey = keyof User;
```

Result:

```text
"id" | "name" | "email"
```

Generic property getter:

```typescript
function get<T, K extends keyof T>(object: T, key: K): T[K] {
    return object[key];
}
```

This prevents invalid keys:

```typescript
const user = { id: "U1", name: "Ava" };

get(user, "id");
// get(user, "age"); // error
```

Interview line:

```text
keyof lets me express valid property names as a type, which is essential for safe generic object utilities.
```

---

## 35. Indexed Access Types

Indexed access extracts a property type.

```typescript
type User = {
    id: string;
    profile: {
        city: string;
    };
};

type UserId = User["id"];
type Profile = User["profile"];
type City = User["profile"]["city"];
```

Array element type:

```typescript
type Booking = {
    id: string;
    amount: number;
};

const bookings: Booking[] = [];

type BookingItem = typeof bookings[number];
```

Strong answer:

```text
Indexed access types let me reuse existing type information instead of duplicating property or
array element types manually.
```

---

## 36. Mapped Types

Mapped types transform each property in a type.

```typescript
type ReadonlyUser<T> = {
    readonly [K in keyof T]: T[K];
};
```

Built-in `Readonly` works like this idea.

Make all fields optional:

```typescript
type MyPartial<T> = {
    [K in keyof T]?: T[K];
};
```

Make all fields nullable:

```typescript
type Nullable<T> = {
    [K in keyof T]: T[K] | null;
};
```

Use:

```typescript
type User = {
    id: string;
    name: string;
};

type NullableUser = Nullable<User>;
```

Strong answer:

```text
Mapped types let me derive new object types from existing ones, reducing duplication and keeping
model changes consistent.
```

---

## 37. Conditional Types

Conditional types choose a type based on another type.

```typescript
type IsString<T> = T extends string ? true : false;
```

Extract array element:

```typescript
type ElementType<T> = T extends Array<infer U> ? U : T;

type A = ElementType<string[]>;
type B = ElementType<number>;
```

Result:

```text
A = string
B = number
```

Production use:

```text
Mostly inside reusable library/helper types, not everyday business logic.
```

Strong answer:

```text
Conditional types are powerful for type-level transformations, especially in libraries. In
application code, I use them carefully because excessive type cleverness can hurt readability.
```

---

## 38. Utility Types Overview

Common built-in utility types:

| Utility | Purpose |
|---|---|
| `Partial<T>` | Make all properties optional |
| `Required<T>` | Make all properties required |
| `Readonly<T>` | Make all properties readonly |
| `Pick<T, K>` | Select properties |
| `Omit<T, K>` | Remove properties |
| `Record<K, V>` | Object with keys K and values V |
| `Exclude<T, U>` | Remove union members |
| `Extract<T, U>` | Keep matching union members |
| `NonNullable<T>` | Remove null and undefined |
| `ReturnType<F>` | Function return type |
| `Parameters<F>` | Function parameter tuple |
| `Awaited<T>` | Unwrap promise-like result |

Strong answer:

```text
Utility types let me derive related models from a source type, which reduces duplication and
keeps API request, response, and update shapes consistent.
```

---

## 39. `Partial`, `Pick`, `Omit`

Base model:

```typescript
type User = {
    id: string;
    name: string;
    email: string;
    passwordHash: string;
    createdAt: Date;
};
```

Partial update:

```typescript
type UserUpdate = Partial<Pick<User, "name" | "email">>;
```

Public response:

```typescript
type PublicUser = Omit<User, "passwordHash">;
```

Create request:

```typescript
type CreateUserRequest = Pick<User, "name" | "email"> & {
    password: string;
};
```

Strong answer:

```text
I use Pick and Omit to derive API shapes from domain models, but I am careful not to expose
internal fields accidentally. For security-sensitive responses, explicit allowlists are often safer.
```

---

## 40. `Record`

`Record<K, V>` creates an object type with keys `K` and values `V`.

```typescript
type Status = "CREATED" | "CONFIRMED" | "CANCELLED";

type StatusCounts = Record<Status, number>;

const counts: StatusCounts = {
    CREATED: 1,
    CONFIRMED: 2,
    CANCELLED: 0
};
```

Record with route config:

```typescript
type RouteName = "home" | "bookings";

type RouteConfig = Record<RouteName, {
    path: string;
    requiresAuth: boolean;
}>;
```

Caution:

```text
Record with broad string keys means any string key is allowed. Use literal key unions when keys
are known.
```

---

## 41. `ReturnType`, `Parameters`, `Awaited`

`ReturnType`:

```typescript
function createUser() {
    return {
        id: "U1",
        name: "Ava"
    };
}

type User = ReturnType<typeof createUser>;
```

`Parameters`:

```typescript
function search(query: string, limit: number) {
    return [];
}

type SearchArgs = Parameters<typeof search>;
```

`Awaited`:

```typescript
async function loadUser() {
    return { id: "U1", name: "Ava" };
}

type LoadedUser = Awaited<ReturnType<typeof loadUser>>;
```

Strong answer:

```text
ReturnType, Parameters, and Awaited are useful when I want types to follow implementation
signatures without manually duplicating them.
```

---

## 42. Template Literal Types

Template literal types build string patterns.

```typescript
type EventName = "created" | "updated" | "deleted";
type BookingEvent = `booking:${EventName}`;
```

Result:

```text
"booking:created" | "booking:updated" | "booking:deleted"
```

Useful for:

- Event names.
- Route patterns.
- CSS/token names.
- Namespaced keys.

Example:

```typescript
type Entity = "user" | "booking";
type Action = "create" | "update";

type Permission = `${Entity}:${Action}`;
```

Caution:

```text
Do not overuse template literal types when simple constants or enums are clearer.
```

---

## 43. Enums vs Literal Unions

Enum:

```typescript
enum Status {
    Created = "CREATED",
    Confirmed = "CONFIRMED",
    Cancelled = "CANCELLED"
}
```

Literal union:

```typescript
type Status = "CREATED" | "CONFIRMED" | "CANCELLED";
```

Const object pattern:

```typescript
const Status = {
    Created: "CREATED",
    Confirmed: "CONFIRMED",
    Cancelled: "CANCELLED"
} as const;

type Status = typeof Status[keyof typeof Status];
```

Trade-off:

| Approach | Notes |
|---|---|
| enum | Emits runtime object unless const enum/settings change behavior |
| literal union | Type-only, simple for API strings |
| const object | Runtime values plus type union |

Interview line:

```text
For API string statuses, I often prefer literal unions or const objects because they align with
runtime strings and avoid enum emission surprises. I follow project convention when enums are established.
```

---

## 44. Classes In TypeScript

TypeScript adds type checking to JavaScript classes.

```typescript
class Booking {
    private status: "CREATED" | "CONFIRMED" = "CREATED";

    constructor(public readonly id: string) {
    }

    confirm(): void {
        this.status = "CONFIRMED";
    }

    getStatus(): string {
        return this.status;
    }
}
```

Access modifiers:

| Modifier | Meaning |
|---|---|
| `public` | Accessible everywhere |
| `private` | TypeScript compile-time private |
| `protected` | Accessible in class and subclasses |
| `readonly` | Cannot be reassigned after initialization |

JavaScript private fields:

```typescript
class User {
    #token: string;

    constructor(token: string) {
        this.#token = token;
    }
}
```

Strong answer:

```text
TypeScript class modifiers improve compile-time safety. JavaScript #private fields provide
runtime privacy, while TypeScript private is mainly checked by the compiler.
```

---

## 45. Async Types

Async function return type:

```typescript
async function loadUser(id: string): Promise<User> {
    const response = await fetch(`/api/users/${id}`);
    return response.json();
}
```

Promise result type:

```typescript
type UserResult = Awaited<ReturnType<typeof loadUser>>;
```

Callback returning promise:

```typescript
type AsyncHandler<T> = (value: T) => Promise<void>;
```

Sequential mapper:

```typescript
async function mapAsync<T, U>(items: T[], mapper: (item: T) => Promise<U>): Promise<U[]> {
    return Promise.all(items.map(mapper));
}
```

Production caution:

```text
Typing an async function does not guarantee runtime response shape from fetch. Validate external data.
```

---

## 46. Runtime Validation Gap

This compiles:

```typescript
type User = {
    id: string;
    name: string;
};

async function loadUser(): Promise<User> {
    const response = await fetch("/api/user");
    return response.json();
}
```

But TypeScript does not inspect the server response at runtime.

Safer shape:

```typescript
async function loadUser(): Promise<User> {
    const response = await fetch("/api/user");
    const value: unknown = await response.json();

    if (!isUser(value)) {
        throw new Error("invalid user response");
    }

    return value;
}
```

Production options:

- Manual type guards.
- Schema libraries like Zod, Valibot, io-ts, or project-approved validation.
- OpenAPI/GraphQL generated types plus runtime validation where needed.
- Contract tests.

Strong answer:

```text
TypeScript types are erased at runtime, so external data must be validated. A Promise<User>
annotation is a promise about what my function returns, not proof that the network response was valid.
```

---

## 47. API Modeling

Model API states explicitly.

```typescript
type ApiSuccess<T> = {
    ok: true;
    data: T;
    requestId: string;
};

type ApiFailure = {
    ok: false;
    error: {
        code: string;
        message: string;
    };
    requestId: string;
};

type ApiResult<T> = ApiSuccess<T> | ApiFailure;
```

Use:

```typescript
function handleResult<T>(result: ApiResult<T>): T {
    if (result.ok) {
        return result.data;
    }

    throw new Error(result.error.message);
}
```

Why strong:

```text
Callers cannot access data unless ok is true.
```

Interview line:

```text
For APIs, I prefer discriminated result types because they make success and failure handling
explicit and prevent impossible combinations.
```

---

## 48. React Typing Awareness

Component props:

```typescript
type BookingCardProps = {
    id: string;
    guestName: string;
    amount: number;
    onCancel?: (id: string) => void;
};

function BookingCard(props: BookingCardProps) {
    const { id, guestName, amount, onCancel } = props;

    return null;
}
```

Event handler:

```typescript
function handleChange(event: React.ChangeEvent<HTMLInputElement>) {
    console.log(event.target.value);
}
```

State union:

```typescript
type UsersState =
    | { status: "idle" }
    | { status: "loading" }
    | { status: "success"; users: User[] }
    | { status: "error"; error: string };
```

Strong React answer:

```text
In React, TypeScript is most useful for props, event handlers, state machines, API data, and
component contracts. Discriminated unions are better than many optional fields for async UI state.
```

---

## 49. Node.js Typing Awareness

Typed request body after validation:

```typescript
type CreateBookingRequest = {
    roomId: string;
    guestId: string;
    nights: number;
};

function createBooking(input: CreateBookingRequest) {
    return {
        id: "B1",
        ...input,
        status: "CREATED" as const
    };
}
```

Express-like caution:

```text
Request body is external input. Do not trust req.body just because you cast it.
```

Bad:

```typescript
const body = req.body as CreateBookingRequest;
```

Better:

```typescript
const body: unknown = req.body;

if (!isCreateBookingRequest(body)) {
    throw new Error("invalid request");
}

createBooking(body);
```

Node line:

```text
For backend TypeScript, the most important boundary is runtime validation of external inputs,
including HTTP bodies, environment variables, queue messages, and third-party API responses.
```

---

## 50. Environment Variables

Environment variables are strings or undefined.

```typescript
const port = process.env.PORT;
```

Type:

```text
string | undefined
```

Bad:

```typescript
const port = Number(process.env.PORT);
```

If missing, this becomes `NaN`.

Better:

```typescript
function requiredEnv(name: string): string {
    const value = process.env[name];

    if (!value) {
        throw new Error(`Missing environment variable: ${name}`);
    }

    return value;
}

const port = Number(requiredEnv("PORT"));

if (!Number.isInteger(port) || port <= 0) {
    throw new Error("PORT must be a positive integer");
}
```

Strong answer:

```text
TypeScript reminds me that environment variables may be missing, but I still need runtime parsing
and validation because env values are strings at runtime.
```

---

## 51. tsconfig Production Settings

Important settings:

```json
{
    "compilerOptions": {
        "strict": true,
        "noImplicitAny": true,
        "strictNullChecks": true,
        "noUncheckedIndexedAccess": true,
        "exactOptionalPropertyTypes": true,
        "noImplicitOverride": true,
        "noFallthroughCasesInSwitch": true,
        "forceConsistentCasingInFileNames": true
    }
}
```

What they help with:

| Setting | Value |
|---|---|
| `strict` | Enables strong type checking family |
| `noImplicitAny` | Prevents accidental any |
| `strictNullChecks` | Makes null/undefined explicit |
| `noUncheckedIndexedAccess` | Array/object indexing may return undefined |
| `exactOptionalPropertyTypes` | Distinguishes missing optional property from explicit undefined |
| `noImplicitOverride` | Safer class overrides |
| `noFallthroughCasesInSwitch` | Prevents switch fallthrough bugs |

Strong answer:

```text
For production TypeScript, I prefer strict mode. The most important settings are strictNullChecks
and noImplicitAny because they prevent many real-world bugs.
```

---

## 52. `noUncheckedIndexedAccess`

Without this setting:

```typescript
const values = [1, 2, 3];
const first: number = values[10];
```

This can be unsafe because runtime value is `undefined`.

With `noUncheckedIndexedAccess`:

```typescript
const first: number | undefined = values[10];
```

You must handle it:

```typescript
const first = values[0];

if (first === undefined) {
    throw new Error("missing first value");
}
```

Strong answer:

```text
noUncheckedIndexedAccess makes indexing safer because arrays and dynamic object keys can return
undefined at runtime.
```

---

## 53. Declaration Files

Declaration files describe types for JavaScript code.

```text
*.d.ts
```

Example:

```typescript
declare module "legacy-library" {
    export function parse(input: string): unknown;
}
```

Use cases:

- Typing legacy JavaScript modules.
- Global variables.
- Third-party packages without built-in types.
- Library publishing.

DefinitelyTyped:

```text
@types/package-name
```

Interview line:

```text
Declaration files provide type information without implementation. They are important when
TypeScript consumes JavaScript libraries or publishes typed packages.
```

---

## 54. Mini Program: Typed API Client

Goal:

```text
Build a small API helper that keeps unknown data at the boundary and returns validated typed data.
```

Code:

```typescript
type User = {
    id: string;
    name: string;
    email: string;
};

function isUser(value: unknown): value is User {
    if (typeof value !== "object" || value === null) {
        return false;
    }

    const candidate = value as Record<string, unknown>;

    return typeof candidate.id === "string"
        && typeof candidate.name === "string"
        && typeof candidate.email === "string";
}

async function fetchJson(url: string): Promise<unknown> {
    const response = await fetch(url);

    if (!response.ok) {
        throw new Error(`HTTP ${response.status}`);
    }

    return response.json();
}

async function loadUser(id: string): Promise<User> {
    const value = await fetchJson(`/api/users/${encodeURIComponent(id)}`);

    if (!isUser(value)) {
        throw new Error("invalid user response");
    }

    return value;
}
```

Why this is strong:

- `fetchJson` returns `unknown`, not fake confidence.
- Runtime guard validates external data.
- `loadUser` returns a real `User` only after validation.
- Callers get a clean typed API.

Interview line:

```text
I keep external data unknown until validation succeeds. After that, the rest of the app can use
strong TypeScript types safely.
```

---

## 55. Mini Program: Discriminated UI State

Bad state model:

```typescript
type BadState<T> = {
    loading: boolean;
    data?: T;
    error?: string;
};
```

This allows impossible states:

```text
loading true with data and error at the same time
```

Better:

```typescript
type LoadState<T> =
    | { status: "idle" }
    | { status: "loading" }
    | { status: "success"; data: T }
    | { status: "error"; error: string };

function renderState<T>(state: LoadState<T>): string {
    switch (state.status) {
        case "idle":
            return "Idle";
        case "loading":
            return "Loading";
        case "success":
            return "Loaded";
        case "error":
            return state.error;
        default:
            return assertNever(state);
    }
}

function assertNever(value: never): never {
    throw new Error(`Unexpected state: ${JSON.stringify(value)}`);
}
```

Production value:

```text
The type system prevents impossible UI states and forces render logic to handle every valid state.
```

---

## 56. Mini Program: Generic Repository Shape

Generic repository interface:

```typescript
type Entity = {
    id: string;
};

interface Repository<T extends Entity> {
    findById(id: string): Promise<T | undefined>;
    save(entity: T): Promise<T>;
    deleteById(id: string): Promise<void>;
}
```

Implementation sketch:

```typescript
class MemoryRepository<T extends Entity> implements Repository<T> {
    private readonly items = new Map<string, T>();

    async findById(id: string): Promise<T | undefined> {
        return this.items.get(id);
    }

    async save(entity: T): Promise<T> {
        this.items.set(entity.id, entity);
        return entity;
    }

    async deleteById(id: string): Promise<void> {
        this.items.delete(id);
    }
}
```

Use:

```typescript
type Booking = Entity & {
    roomId: string;
    status: "CREATED" | "CONFIRMED";
};

const repository = new MemoryRepository<Booking>();
```

Why this is strong:

- Generic but constrained.
- Preserves entity-specific fields.
- Avoids `any`.
- Models async persistence.

---

## 57. Common Mistakes

| Mistake | Why It Hurts | Better Approach |
|---|---|---|
| Using `any` for convenience | Turns off safety | Use `unknown` and narrow |
| Casting API responses directly | No runtime proof | Validate external data |
| Over-annotating locals | Noise and duplication | Let inference work |
| Under-annotating public APIs | Return shapes drift | Annotate exported boundaries |
| Using truthiness for optional numbers | Breaks `0` | Explicit null/undefined checks |
| Modeling state with many optionals | Allows impossible states | Use discriminated unions |
| Using `as` to silence errors | Hides real mismatch | Fix types or validate |
| Confusing TS private with runtime private | Compile-time only | Use `#private` for runtime privacy |
| Broad `Record<string, T>` everywhere | Allows any key | Use literal key unions when known |
| Retyping existing shapes manually | Drift over time | Use utility/indexed access types |
| Excessive conditional types | Hard to maintain | Prefer simple readable types |
| Assuming TypeScript replaces tests | Types are not behavior tests | Use tests plus types |
| Assuming TypeScript validates JSON | Types are erased | Runtime validation required |

---

## 58. Strong Interview Answers

### What is TypeScript?

```text
TypeScript is a statically typed layer on top of JavaScript. It checks code at compile time and
then emits JavaScript. Types improve safety and maintainability, but they are erased at runtime.
```

### `any` vs `unknown`

```text
any disables type checking and lets unsafe operations spread. unknown means the value is not yet
known, so TypeScript forces narrowing before use. I use unknown at external boundaries and avoid
any in domain code.
```

### Interface vs Type

```text
Both can model object shapes. Interfaces support extension and declaration merging. Type aliases
can represent unions, tuples, mapped types, conditional types, and function aliases. For simple
objects, I follow project convention.
```

### Generics

```text
Generics let reusable code preserve type relationships. For example, a generic first<T> function
can accept T[] and return T | undefined without losing the element type.
```

### Discriminated Unions

```text
Discriminated unions model mutually exclusive states with a shared literal field. They are great
for API and UI states because TypeScript narrows each branch and prevents impossible combinations.
```

### Runtime Validation

```text
TypeScript does not validate runtime data. For API responses, request bodies, local storage, and
environment variables, I keep the value unknown until a runtime guard or schema validates it.
```

---

## 59. FAANG-Level Question 1

> A team migrated from JavaScript to TypeScript but still has production crashes from bad API payloads. Why, and what would you change?

Strong answer:

```text
TypeScript only checks code at compile time. If the API sends a payload that does not match the
annotated type, TypeScript will not catch it at runtime. A function returning Promise<User> is
only as trustworthy as the code that validates or constructs that User.

I would move external data to unknown at the boundary, validate with schema validation or type
guards, and only return typed domain objects after validation. I would also add contract tests,
monitor invalid payload rates, and avoid unsafe casts like response.json() as User.
```

This answer shows:

- Type erasure awareness.
- Boundary design maturity.
- Runtime validation discipline.
- Testing and observability thinking.

---

## 60. FAANG-Level Question 2

> How would you model a React data-fetching component state in TypeScript?

Strong answer:

```text
I would use a discriminated union instead of separate loading, data, and error optionals. Separate
flags allow impossible combinations like loading true with data and error at the same time.

A union like idle, loading, success with data, and error with message makes valid states explicit.
Then the render function can switch on status, and TypeScript narrows each branch. I would also
use an exhaustive never check so adding a new state forces every renderer to handle it.
```

Example:

```typescript
type LoadState<T> =
    | { status: "idle" }
    | { status: "loading" }
    | { status: "success"; data: T }
    | { status: "error"; error: string };
```

---

## 61. FAANG-Level Question 3

> A codebase uses `any` heavily. How would you improve type safety without stopping feature work?

Strong answer:

```text
I would improve safety incrementally. First, enable strict checks where possible and prevent new
implicit any with linting or tsconfig settings. Second, focus on high-risk boundaries: API clients,
request bodies, shared domain models, payment/auth flows, and commonly reused utilities.

I would replace any with unknown at external boundaries and add narrowing or schema validation.
For legacy modules, I might add wrapper functions with safe typed interfaces instead of rewriting
everything at once. I would track progress and avoid massive risky migrations.
```

This answer shows:

- Migration strategy.
- Risk-based prioritization.
- Practical team awareness.
- Production safety mindset.

---

## 62. Rapid Revision

- TypeScript is JavaScript plus compile-time types.
- Types are erased at runtime.
- TypeScript does not validate external JSON automatically.
- TypeScript is structurally typed.
- Use `const` inference and avoid unnecessary local annotations.
- Annotate public/exported boundaries.
- Use lowercase primitive types: `string`, `number`, `boolean`.
- Use `readonly` arrays when functions should not mutate inputs.
- Use tuples for clear fixed-position data.
- Optional properties read as possibly undefined under strict checks.
- Interfaces are extendable and mergeable.
- Type aliases handle unions, tuples, mapped types, and conditional types.
- `any` disables safety.
- `unknown` forces narrowing.
- `never` models impossible values and exhaustive checks.
- `void` means no useful return value.
- Union types model multiple valid possibilities.
- Intersection types combine requirements.
- Narrowing uses runtime checks to refine types.
- Avoid truthiness checks when `0`, `false`, or empty string are valid.
- Discriminated unions model safe state machines.
- Exhaustive switches catch missing cases.
- Custom type guards connect runtime checks to type narrowing.
- Type assertions do not validate runtime values.
- `as const` preserves literal values.
- `satisfies` checks shape while preserving inference.
- Generics preserve type relationships in reusable code.
- Constraints require minimum shape for generics.
- `keyof` gives property key unions.
- Indexed access reuses property types.
- Mapped types transform object types.
- Conditional types power advanced utilities.
- Utility types reduce duplication.
- `Pick` and `Omit` derive API shapes.
- `Record` is best with known key unions when possible.
- `Awaited<ReturnType<typeof fn>>` extracts async result types.
- Literal unions are often better for API status strings than enums.
- TypeScript private is compile-time; `#private` is runtime private.
- Runtime validation is required for API, env, storage, and message boundaries.
- Strict tsconfig settings catch more real bugs.
- Use discriminated unions for React async state.
- Avoid clever types that make code harder to maintain.

---

## 63. Official Source Notes

Use these sources when refreshing TypeScript details:

- TypeScript Handbook: `https://www.typescriptlang.org/docs/handbook/intro.html`
- TypeScript Everyday Types: `https://www.typescriptlang.org/docs/handbook/2/everyday-types.html`
- TypeScript Narrowing: `https://www.typescriptlang.org/docs/handbook/2/narrowing.html`
- TypeScript More on Functions: `https://www.typescriptlang.org/docs/handbook/2/functions.html`
- TypeScript Generics: `https://www.typescriptlang.org/docs/handbook/2/generics.html`
- TypeScript Object Types: `https://www.typescriptlang.org/docs/handbook/2/objects.html`
- TypeScript Utility Types: `https://www.typescriptlang.org/docs/handbook/utility-types.html`
- TypeScript tsconfig reference: `https://www.typescriptlang.org/tsconfig/`
- TypeScript release notes: `https://www.typescriptlang.org/docs/handbook/release-notes/overview.html`
- React TypeScript Cheatsheet: `https://react-typescript-cheatsheet.netlify.app/`

Interview safety line:

```text
I use TypeScript to make contracts explicit and refactoring safe, but I do not confuse compile-time
confidence with runtime truth. For production systems, TypeScript works best with strict settings,
validation at trust boundaries, tests, and simple maintainable types.
```
