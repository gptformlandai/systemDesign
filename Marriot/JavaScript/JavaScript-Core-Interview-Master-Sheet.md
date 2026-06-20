# JavaScript Core Interview Master Sheet

Target: JavaScript beginner-to-senior interviews, frontend/full-stack rounds, Node.js backend rounds, and MAANG-style fundamentals checks.

This sheet covers:
- JavaScript language mental model
- Values, types, primitives, and references
- `typeof`, `null`, `undefined`, `NaN`, `BigInt`, `Symbol`
- Truthy/falsy values
- Equality and coercion
- `var`, `let`, `const`, hoisting, and temporal dead zone
- Functions, arrow functions, parameters, and return behavior
- Objects, arrays, references, copying, and mutation
- Errors and defensive coding
- Common traps, strong answers, and production judgment

How to use this:
- First understand the mental model.
- Then learn the rule behind each weird output.
- Say the strong answer out loud in 30-60 seconds.
- Type the important snippets once so the rules become muscle memory.

---

## 1. Interview Priority Meter

| Area | Priority | What They Usually Test |
|---|---:|---|
| Primitive vs reference values | Very high | Memory model and mutation clarity |
| `var`, `let`, `const` | Very high | Scope, hoisting, TDZ |
| `null` vs `undefined` | Very high | API design and output traps |
| `typeof` behavior | Very high | Classic tricky output questions |
| Truthy/falsy values | Very high | Condition bugs |
| `==` vs `===` | Very high | Coercion and correctness |
| Type coercion | Very high | Weird JavaScript outputs |
| `NaN` and `Number.isNaN` | High | Numeric edge cases |
| Functions | Very high | Declaration vs expression, return, parameters |
| Arrow functions | Very high | Syntax and `this` awareness |
| Objects and arrays | Very high | Reference mutation and copying |
| Shallow copy vs deep copy | Very high | Production state bugs |
| Destructuring/spread/rest | High | Modern syntax and clean code |
| Error handling | High | Production maturity |
| Modules | Medium-high | Modern JavaScript organization |
| Strict mode | Medium | Safer runtime behavior |

---

## 2. JavaScript Big Picture

JavaScript is a high-level, dynamically typed, prototype-based language.

It is used in multiple runtimes:

| Runtime | Where It Runs | Examples |
|---|---|---|
| Browser | User device | DOM, fetch, events, rendering |
| Node.js | Server/CLI | APIs, file system, streams, workers |
| Edge runtimes | CDN/serverless edge | Request handlers, caching, lightweight APIs |

Important distinction:

```text
JavaScript language rules are defined by ECMAScript.
Browser APIs are provided by browsers.
Node.js APIs are provided by Node.js.
```

Example:

```javascript
console.log(typeof Promise); // language/runtime available in modern JS runtimes
console.log(typeof document); // browser only, usually undefined in Node.js
console.log(typeof process); // Node.js only, usually undefined in browsers
```

Strong interview answer:

```text
JavaScript is the language. The browser and Node.js are runtimes that provide extra APIs.
That is why features like closures and promises are language-level, while DOM and process
are runtime-specific.
```

---

## 3. Values And Types

JavaScript values are grouped into primitives and objects.

Primitives:

| Type | Example |
|---|---|
| `string` | `"hello"` |
| `number` | `42`, `3.14`, `NaN`, `Infinity` |
| `bigint` | `10n` |
| `boolean` | `true`, `false` |
| `undefined` | `undefined` |
| `symbol` | `Symbol("id")` |
| `null` | `null` |

Objects:

| Type | Example |
|---|---|
| Object | `{ name: "Ava" }` |
| Array | `[1, 2, 3]` |
| Function | `function run() {}` |
| Date | `new Date()` |
| Map | `new Map()` |
| Set | `new Set()` |
| Promise | `Promise.resolve(1)` |

Interview line:

```text
In JavaScript, primitives are immutable values, while objects are reference values. Arrays
and functions are objects too.
```

---

## 4. Primitive vs Reference Values

Primitive assignment copies the value.

```javascript
let a = 10;
let b = a;
b = 20;

console.log(a); // 10
console.log(b); // 20
```

Object assignment copies the reference.

```javascript
const user1 = { name: "Aravind" };
const user2 = user1;

user2.name = "Rahul";

console.log(user1.name); // Rahul
console.log(user2.name); // Rahul
```

Why:

```text
user1 and user2 point to the same object.
The variable was copied, not the object itself.
```

Strong answer:

```text
Primitives behave like direct values. Objects behave through references. Assigning an object
to another variable copies the reference, so mutations through one variable are visible
through the other.
```

Production caution:

```text
Many frontend state bugs come from mutating shared object or array references instead of
creating a new copy.
```

---

## 5. `typeof` And Type Checks

Common outputs:

```javascript
console.log(typeof "hello");      // string
console.log(typeof 42);           // number
console.log(typeof 10n);          // bigint
console.log(typeof true);         // boolean
console.log(typeof undefined);    // undefined
console.log(typeof Symbol("id")); // symbol
console.log(typeof {});           // object
console.log(typeof []);           // object
console.log(typeof function () {}); // function
console.log(typeof null);         // object
```

Classic trap:

```javascript
console.log(typeof null); // object
```

Why:

```text
typeof null returning object is a historical JavaScript bug kept for backward compatibility.
```

Better checks:

```javascript
Array.isArray([]);              // true
value === null;                 // null check
typeof value === "function";    // function check
value instanceof Date;          // Date object check, with caveats across realms
```

Interview-safe answer:

```text
typeof is useful for primitive checks, but it has traps. It returns object for null and arrays,
so I use value === null for null and Array.isArray for arrays.
```

---

## 6. `undefined` vs `null`

`undefined` usually means a value is missing or not assigned by JavaScript.

```javascript
let name;
console.log(name); // undefined

const user = {};
console.log(user.email); // undefined
```

`null` usually means intentional empty value.

```javascript
const selectedUser = null;
```

Difference:

| Value | Meaning |
|---|---|
| `undefined` | Not assigned, missing, not provided |
| `null` | Intentionally empty or no object |

Trap:

```javascript
console.log(null == undefined);  // true
console.log(null === undefined); // false
```

Strong answer:

```text
undefined usually means JavaScript or the caller did not provide a value. null is usually an
intentional empty value. I use strict equality to distinguish them when the difference matters.
```

Production rule:

```text
For API contracts, be consistent. Decide whether a missing field is omitted, null, or an empty
collection. Inconsistent absence semantics cause frontend and backend bugs.
```

---

## 7. Truthy And Falsy Values

Falsy values in JavaScript:

```text
false
0
-0
0n
""
null
undefined
NaN
```

Everything else is truthy, including:

```javascript
Boolean([]);       // true
Boolean({});       // true
Boolean("false");  // true
Boolean("0");      // true
```

Trap:

```javascript
if ([]) {
    console.log("runs");
}

if ({}) {
    console.log("also runs");
}
```

Both run because arrays and objects are truthy.

Common production bug:

```javascript
function applyDiscount(discount) {
    if (!discount) {
        return "no discount";
    }
    return `discount: ${discount}`;
}

console.log(applyDiscount(0)); // no discount
```

Better:

```javascript
function applyDiscount(discount) {
    if (discount == null) {
        return "no discount";
    }
    return `discount: ${discount}`;
}
```

Interview line:

```text
Falsy checks are convenient, but they can accidentally treat valid values like 0 or empty
string as missing. In production code, I check the exact absence condition when it matters.
```

---

## 8. `var`, `let`, And `const`

| Keyword | Scope | Reassign? | Redeclare? | Hoisted? |
|---|---|---:|---:|---:|
| `var` | Function | Yes | Yes in same scope | Yes, initialized to `undefined` |
| `let` | Block | Yes | No in same scope | Yes, but TDZ applies |
| `const` | Block | No | No in same scope | Yes, but TDZ applies |

`var` function scope:

```javascript
function run() {
    if (true) {
        var count = 1;
    }
    console.log(count); // 1
}
```

`let` block scope:

```javascript
function run() {
    if (true) {
        let count = 1;
    }
    console.log(count); // ReferenceError
}
```

`const` prevents reassignment, not object mutation:

```javascript
const user = { name: "Ava" };
user.name = "Mia";

console.log(user.name); // Mia

user = {}; // TypeError
```

Strong answer:

```text
I prefer const by default, let when reassignment is needed, and avoid var in modern code.
const protects the variable binding from reassignment, but it does not make objects immutable.
```

---

## 9. Hoisting And Temporal Dead Zone

Hoisting means declarations are processed before code execution within their scope.

`var` example:

```javascript
console.log(count); // undefined
var count = 10;
```

Mental model:

```javascript
var count;
console.log(count);
count = 10;
```

`let` and `const` are also hoisted, but not usable before initialization.

```javascript
console.log(name); // ReferenceError
let name = "Ava";
```

This unusable region is the temporal dead zone.

Function declarations are hoisted:

```javascript
sayHi(); // works

function sayHi() {
    console.log("hi");
}
```

Function expressions follow variable rules:

```javascript
run(); // TypeError if var, ReferenceError if let/const

var run = function () {
    console.log("run");
};
```

Strong answer:

```text
var declarations are hoisted and initialized to undefined. let and const are hoisted too,
but they stay in the temporal dead zone until the declaration is evaluated. Function
declarations are callable before their declaration, but function expressions are not.
```

---

## 10. Equality: `==`, `===`, And `Object.is`

Strict equality avoids most coercion.

```javascript
console.log(1 === "1"); // false
console.log(1 == "1");  // true
```

General rule:

```text
Use === by default.
Use == only when you intentionally want its coercion behavior and can explain it.
```

Common outputs:

```javascript
console.log(null == undefined);  // true
console.log(null === undefined); // false

console.log(false == 0);  // true
console.log(false === 0); // false

console.log("" == 0);  // true
console.log("" === 0); // false
```

`Object.is` differs in edge cases:

```javascript
console.log(NaN === NaN);        // false
console.log(Object.is(NaN, NaN)); // true

console.log(0 === -0);        // true
console.log(Object.is(0, -0)); // false
```

Strong answer:

```text
I use strict equality by default because it avoids implicit coercion. Object.is is useful for
edge cases like NaN and -0. Loose equality has specific rules, but I avoid relying on it in
production code except for deliberate nullish checks like value == null.
```

Production pattern:

```javascript
if (value == null) {
    // true for null or undefined only
}
```

This is one accepted intentional use of loose equality.

---

## 11. Type Coercion Mental Model

Coercion means JavaScript converts a value from one type to another.

Common conversions:

```javascript
Number("42");      // 42
String(42);        // "42"
Boolean(0);        // false
Boolean("hello");  // true
```

`+` is tricky because it can mean numeric addition or string concatenation.

```javascript
console.log(1 + 2);       // 3
console.log("1" + 2);     // "12"
console.log(1 + "2");     // "12"
console.log("1" + 2 + 3); // "123"
console.log(1 + 2 + "3"); // "33"
```

Other arithmetic operators usually coerce to number:

```javascript
console.log("5" - 2); // 3
console.log("5" * 2); // 10
console.log("5" / 2); // 2.5
```

Array/object traps:

```javascript
console.log([] + []);       // ""
console.log([] + {});       // "[object Object]"
console.log({}.toString()); // "[object Object]"
```

Strong answer:

```text
Coercion is JavaScript converting values implicitly or explicitly. The plus operator is the
classic trap because it performs string concatenation if either side becomes a string. For
production code, I prefer explicit conversion so the intent is visible.
```

Better production style:

```javascript
const count = Number(inputCount);
const label = String(id);
const enabled = Boolean(flag);
```

---

## 12. Numbers, `NaN`, And Precision

JavaScript has one main number type: `number`.

It uses double-precision floating point.

```javascript
console.log(0.1 + 0.2); // 0.30000000000000004
```

Why:

```text
Many decimal fractions cannot be represented exactly in binary floating point.
```

`NaN` means Not-a-Number, but its type is number.

```javascript
console.log(typeof NaN); // number
console.log(NaN === NaN); // false
```

Use:

```javascript
Number.isNaN(value);
```

Instead of global `isNaN` for strict checks:

```javascript
Number.isNaN("hello"); // false
isNaN("hello");        // true, because it coerces first
```

Infinity:

```javascript
console.log(1 / 0);  // Infinity
console.log(-1 / 0); // -Infinity
```

Production caution:

```text
Do not use floating point numbers directly for money calculations unless you control rounding
carefully. Prefer integer minor units like cents or a decimal library.
```

---

## 13. BigInt

BigInt is for integers larger than safe JavaScript numbers.

```javascript
const large = 9007199254740993n;
console.log(typeof large); // bigint
```

Safe integer limit:

```javascript
Number.MAX_SAFE_INTEGER; // 9007199254740991
```

Trap:

```javascript
console.log(1n + 1); // TypeError
```

You cannot mix `bigint` and `number` directly.

Use explicit conversion only when safe:

```javascript
Number(1n);
BigInt(1);
```

Interview line:

```text
BigInt is useful for very large integers, but it does not mix automatically with number.
For APIs and databases, I am careful because JSON does not serialize BigInt by default.
```

---

## 14. Symbol

Symbol creates unique property keys.

```javascript
const id1 = Symbol("id");
const id2 = Symbol("id");

console.log(id1 === id2); // false
```

Use case:

```javascript
const internalId = Symbol("internalId");

const user = {
    name: "Ava",
    [internalId]: 123
};
```

Symbols are not normally included in common enumeration methods:

```javascript
console.log(Object.keys(user)); // ["name"]
```

Interview line:

```text
Symbol creates unique keys. It is useful when a property should not collide with normal string
keys, but it is not a security boundary.
```

---

## 15. Strings And Template Literals

Strings are immutable primitives.

```javascript
let name = "Ava";
name[0] = "E";
console.log(name); // Ava
```

Template literals:

```javascript
const user = "Ava";
const message = `Hello, ${user}`;
```

Multi-line:

```javascript
const sql = `
SELECT id, name
FROM users
WHERE active = true
`;
```

Production caution:

```text
Template literals make string building easy, but do not use interpolation to build unsafe SQL,
HTML, shell commands, or URLs without proper escaping/parameterization.
```

---

## 16. Functions

Function declaration:

```javascript
function add(a, b) {
    return a + b;
}
```

Function expression:

```javascript
const add = function (a, b) {
    return a + b;
};
```

Arrow function:

```javascript
const add = (a, b) => a + b;
```

Functions are first-class values:

```javascript
function execute(operation, value) {
    return operation(value);
}

const doubled = execute(x => x * 2, 10);
console.log(doubled); // 20
```

Strong answer:

```text
Functions in JavaScript are first-class values. They can be stored in variables, passed as
arguments, returned from other functions, and used to create closures.
```

---

## 17. Arrow Functions

Arrow functions are concise and capture lexical `this`.

```javascript
const square = x => x * x;
```

Object return trap:

```javascript
const makeUserWrong = () => { name: "Ava" };
console.log(makeUserWrong()); // undefined
```

Why:

```text
The braces are parsed as a function body, not an object literal.
```

Correct:

```javascript
const makeUser = () => ({ name: "Ava" });
```

Arrow functions do not have their own `this`, `arguments`, or `prototype`.

Interview line:

```text
Arrow functions are great for callbacks and concise functions, but I avoid them when I need
a dynamic this, a constructor, or the arguments object.
```

Deep `this` rules are covered in the dedicated `this`, prototypes, and classes sheet.

---

## 18. Parameters, Defaults, Rest, And Spread

Default parameters:

```javascript
function greet(name = "guest") {
    return `Hello, ${name}`;
}
```

Rest parameters gather arguments:

```javascript
function sum(...numbers) {
    return numbers.reduce((total, value) => total + value, 0);
}
```

Spread expands values:

```javascript
const nums = [1, 2, 3];
console.log(Math.max(...nums)); // 3
```

Function argument trap:

```javascript
function update(user) {
    user.name = "Mia";
}

const user = { name: "Ava" };
update(user);
console.log(user.name); // Mia
```

Mental model:

```text
JavaScript passes arguments by value. For objects, the value being passed is the reference.
That means the function cannot replace the caller's variable, but it can mutate the object.
```

---

## 19. Return Behavior And Automatic Semicolon Insertion

Trap:

```javascript
function getUser() {
    return
    {
        name: "Ava"
    };
}

console.log(getUser()); // undefined
```

Why:

```text
Automatic semicolon insertion inserts a semicolon after return.
```

Correct:

```javascript
function getUser() {
    return {
        name: "Ava"
    };
}
```

Interview line:

```text
Do not put a line break immediately after return when returning an expression or object.
```

---

## 20. Objects

Objects store key-value pairs.

```javascript
const user = {
    id: 1,
    name: "Ava",
    active: true
};
```

Property access:

```javascript
console.log(user.name);
console.log(user["name"]);
```

Computed property:

```javascript
const field = "email";
const user = {
    [field]: "ava@example.com"
};
```

Optional property access:

```javascript
const city = user.address?.city;
```

Deleting property:

```javascript
delete user.active;
```

Production caution:

```text
Objects are flexible, but uncontrolled shape changes can make code harder to reason about and
can hurt runtime optimization in hot paths.
```

---

## 21. Arrays

Arrays are ordered objects with numeric indexes and a `length` property.

```javascript
const numbers = [1, 2, 3];
console.log(numbers[0]); // 1
console.log(numbers.length); // 3
```

Common methods:

| Method | Mutates? | Use |
|---|---:|---|
| `push` | Yes | Add to end |
| `pop` | Yes | Remove from end |
| `shift` | Yes | Remove from start |
| `unshift` | Yes | Add to start |
| `slice` | No | Copy portion |
| `splice` | Yes | Insert/remove in place |
| `map` | No | Transform |
| `filter` | No | Keep matching elements |
| `reduce` | No | Accumulate |
| `sort` | Yes | Sort in place |

Sort trap:

```javascript
const values = [10, 2, 1];
values.sort();
console.log(values); // [1, 10, 2]
```

Why:

```text
Default sort converts elements to strings.
```

Correct numeric sort:

```javascript
values.sort((a, b) => a - b);
```

Production line:

```text
I remember which array methods mutate. Mutating arrays accidentally is a common UI state and
cache bug.
```

---

## 22. Object And Array Copying

Shallow object copy:

```javascript
const user = { name: "Ava", address: { city: "NYC" } };
const copy = { ...user };

copy.name = "Mia";
copy.address.city = "LA";

console.log(user.name); // Ava
console.log(user.address.city); // LA
```

Why:

```text
The top-level object was copied, but nested objects still share references.
```

Shallow array copy:

```javascript
const list = [1, 2, 3];
const copy = [...list];
```

Deep copy options:

```javascript
const deepCopy = structuredClone(value);
```

Caution:

```text
JSON.parse(JSON.stringify(value)) loses functions, undefined, Date objects, Map, Set, BigInt,
and special values. It is not a general deep clone solution.
```

Strong answer:

```text
Spread syntax creates a shallow copy. It is enough for flat objects but not nested structures.
For deep copying, I prefer structuredClone when supported, or a domain-specific copy when the
object has special types or business meaning.
```

---

## 23. Destructuring, Optional Chaining, And Nullish Coalescing

Object destructuring:

```javascript
const user = { id: 1, name: "Ava" };
const { id, name } = user;
```

Array destructuring:

```javascript
const [first, second] = [10, 20];
```

Default value:

```javascript
const { role = "guest" } = user;
```

Optional chaining:

```javascript
const city = user.profile?.address?.city;
```

Nullish coalescing:

```javascript
const limit = input.limit ?? 20;
```

Difference between `||` and `??`:

```javascript
console.log(0 || 10);  // 10
console.log(0 ?? 10);  // 0
console.log("" || "x"); // x
console.log("" ?? "x"); // ""
```

Strong answer:

```text
I use optional chaining to safely read nested values and nullish coalescing when only null or
undefined should fall back. I avoid || defaults when 0, false, or empty string are valid values.
```

---

## 24. Error Handling

Throwing errors:

```javascript
function parseAmount(value) {
    const amount = Number(value);
    if (Number.isNaN(amount)) {
        throw new Error("amount must be numeric");
    }
    return amount;
}
```

Catching errors:

```javascript
try {
    parseAmount("abc");
} catch (error) {
    console.error(error.message);
}
```

Custom error:

```javascript
class ValidationError extends Error {
    constructor(message, details = {}) {
        super(message);
        this.name = "ValidationError";
        this.details = details;
    }
}
```

Async error reminder:

```javascript
async function loadUser() {
    try {
        const response = await fetch("/api/user");
        return await response.json();
    } catch (error) {
        throw new Error("failed to load user", { cause: error });
    }
}
```

Production line:

```text
I separate programmer errors, validation errors, and external dependency failures. In
production, I log enough context to debug but avoid leaking secrets or sensitive user data.
```

---

## 25. Strict Mode

Strict mode makes JavaScript reject or change some unsafe behaviors.

```javascript
"use strict";

x = 10; // ReferenceError instead of creating accidental global
```

Modern ES modules are strict by default.

Benefits:

- Prevents accidental globals.
- Makes some silent failures throw.
- Changes `this` behavior in plain function calls.
- Disallows some confusing syntax.

Interview line:

```text
Strict mode makes JavaScript safer by turning some silent mistakes into errors. Modern modules
run in strict mode automatically.
```

---

## 26. Modules At Core Level

Named export:

```javascript
export function add(a, b) {
    return a + b;
}
```

Named import:

```javascript
import { add } from "./math.js";
```

Default export:

```javascript
export default function createUser() {
    return { active: true };
}
```

Default import:

```javascript
import createUser from "./createUser.js";
```

CommonJS in Node.js:

```javascript
const fs = require("node:fs");
module.exports = { readConfig };
```

Interview line:

```text
Modern JavaScript uses ES modules with import/export. Node.js also has CommonJS, and real
projects may need to understand the interop rules between the two module systems.
```

Deep module/runtime behavior is covered in the Node.js sheet.

---

## 27. Common Output Traps

### Trap 1: `typeof null`

```javascript
console.log(typeof null); // object
```

Rule:

```text
Historical bug. Check null with value === null.
```

### Trap 2: Array Truthiness

```javascript
console.log(Boolean([])); // true
```

Rule:

```text
Objects and arrays are truthy even when empty.
```

### Trap 3: String Plus Number

```javascript
console.log("5" + 1); // "51"
console.log("5" - 1); // 4
```

Rule:

```text
+ may concatenate. Other arithmetic operators usually coerce to number.
```

### Trap 4: `NaN`

```javascript
console.log(NaN === NaN); // false
console.log(Number.isNaN(NaN)); // true
```

Rule:

```text
Use Number.isNaN for precise NaN checks.
```

### Trap 5: `const` Object Mutation

```javascript
const user = { name: "Ava" };
user.name = "Mia";
console.log(user.name); // Mia
```

Rule:

```text
const prevents rebinding, not object mutation.
```

### Trap 6: Return Newline

```javascript
function value() {
    return
    10;
}

console.log(value()); // undefined
```

Rule:

```text
Do not put a line break immediately after return.
```

---

## 28. Production Coding Rules

| Risk | Safer Habit |
|---|---|
| Accidental coercion | Use explicit conversion and `===` |
| Missing value confusion | Distinguish `null`, `undefined`, empty string, and 0 |
| Shared mutation | Copy objects/arrays before changing state |
| Nested shallow copy bug | Use structured clone or domain-specific copy when needed |
| Money precision | Use integer minor units or decimal library |
| Unsafe string interpolation | Use parameterized queries and proper escaping |
| Hidden globals | Use modules and strict mode |
| Weak errors | Throw meaningful error types with safe context |
| Huge object retention | Avoid unnecessary references from closures/caches/listeners |
| Runtime data trust | Validate external input even when using TypeScript |

Strong line:

```text
Production JavaScript is mostly about making dynamic behavior explicit: explicit types at
boundaries, explicit absence checks, explicit copying, explicit error handling, and explicit
runtime validation.
```

---

## 29. Mini Program: Normalize Booking Payload

This example combines core JavaScript rules: nullish checks, type conversion, validation,
copying, and safe output shape.

```javascript
class ValidationError extends Error {
    constructor(message, details = {}) {
        super(message);
        this.name = "ValidationError";
        this.details = details;
    }
}

function normalizeBookingPayload(payload) {
    if (payload == null || typeof payload !== "object" || Array.isArray(payload)) {
        throw new ValidationError("payload must be an object");
    }

    const roomId = String(payload.roomId ?? "").trim();
    const guestId = String(payload.guestId ?? "").trim();
    const nights = Number(payload.nights ?? 1);
    const couponCode = payload.couponCode == null
        ? null
        : String(payload.couponCode).trim().toUpperCase();

    if (roomId === "") {
        throw new ValidationError("roomId is required");
    }

    if (guestId === "") {
        throw new ValidationError("guestId is required");
    }

    if (!Number.isInteger(nights) || nights <= 0) {
        throw new ValidationError("nights must be a positive integer", { nights });
    }

    return {
        roomId,
        guestId,
        nights,
        couponCode,
        metadata: {
            source: String(payload.source ?? "web")
        }
    };
}

const normalized = normalizeBookingPayload({
    roomId: " R101 ",
    guestId: " U1 ",
    nights: "2",
    couponCode: " summer10 "
});

console.log(normalized);
```

Why this is interview-useful:

- `payload == null` intentionally catches both `null` and `undefined`.
- `Array.isArray` prevents arrays being accepted as generic objects.
- `??` preserves valid falsy values better than `||`.
- `Number.isInteger` avoids weak numeric validation.
- The output object is a clean normalized shape.

---

## 30. Strong Interview Answers

### What is JavaScript?

```text
JavaScript is a dynamically typed, prototype-based language standardized by ECMAScript. It can
run in browsers, Node.js, edge runtimes, and other environments. The language gives features
like functions, closures, objects, promises, and modules, while each runtime provides extra
APIs like DOM in browsers or file system APIs in Node.js.
```

### Primitive vs Reference

```text
Primitives are immutable values like string, number, boolean, null, undefined, symbol, and
bigint. Objects, arrays, and functions are reference values. Assigning an object copies the
reference, not the object, so mutation through one reference is visible through another.
```

### `var`, `let`, `const`

```text
var is function-scoped and hoisted with undefined. let and const are block-scoped and have a
temporal dead zone before initialization. I use const by default, let for reassignment, and
avoid var in modern JavaScript.
```

### `==` vs `===`

```text
Strict equality compares without most implicit coercion, while loose equality can convert
values first. I use === by default because it avoids surprising conversions. One deliberate
exception is value == null when I intentionally want to match both null and undefined.
```

### Why JavaScript Has Weird Outputs

```text
Most weird JavaScript outputs come from a small set of rules: type coercion, truthy/falsy
checks, reference mutation, hoisting, automatic semicolon insertion, and runtime-specific APIs.
If I know the rule, I do not need to memorize the output.
```

---

## 31. Common Mistakes

| Mistake | Why It Hurts | Better Approach |
|---|---|---|
| Using `==` everywhere | Hidden coercion bugs | Use `===` by default |
| Using `||` for all defaults | Breaks valid `0`, `false`, `""` | Use `??` for nullish fallback |
| Thinking `const` means immutable | Object can still mutate | Use copies or freezing when needed |
| Accepting arrays as objects | Arrays pass `typeof value === "object"` | Check `Array.isArray` |
| Checking `NaN` with `===` | `NaN !== NaN` | Use `Number.isNaN` |
| Mutating input objects | Side effects surprise callers | Return new objects |
| Forgetting `sort` mutates | Original array changes | Copy before sorting if needed |
| Using default sort for numbers | Sorts as strings | Use `(a, b) => a - b` |
| Returning object after newline | ASI returns undefined | Put object on same line as return |
| Trusting TypeScript at runtime | Types vanish after compile | Validate external input |

---

## 32. FAANG-Level Question

> A production checkout page sometimes treats a valid discount of `0` as missing, mutates cached user data, and shows strange totals like `10020`. How would you debug and fix this in JavaScript?

Strong answer:

```text
I would look for three JavaScript core issues. First, a discount of 0 being treated as missing
usually means the code uses a broad falsy check like if (!discount) or discount || fallback.
I would replace that with a precise nullish check such as discount == null or discount ?? 0.

Second, cached user data being mutated suggests shared object references. I would check where
objects are assigned or shallow-copied, and avoid mutating shared state directly. For flat
updates I would use object spread; for nested structures I would use a proper nested copy or
structuredClone when appropriate.

Third, totals like 10020 often mean string concatenation happened instead of numeric addition.
I would validate and convert external input with Number, reject NaN using Number.isNaN, and
keep money in integer minor units where possible.
```

That answer shows:

- JavaScript type knowledge.
- Coercion knowledge.
- Reference mutation knowledge.
- Production debugging judgment.
- Safer coding habits.

---

## 33. Rapid Revision

- JavaScript language is ECMAScript; browser and Node.js provide runtime APIs.
- Primitives are immutable values.
- Objects, arrays, and functions are reference values.
- `typeof null` is `object` because of a historical bug.
- Use `Array.isArray` for arrays.
- `undefined` usually means missing or not assigned.
- `null` usually means intentionally empty.
- Falsy values: `false`, `0`, `-0`, `0n`, `""`, `null`, `undefined`, `NaN`.
- Empty arrays and objects are truthy.
- Prefer `const`, then `let`, avoid `var`.
- `const` prevents reassignment, not object mutation.
- `var` is function-scoped.
- `let` and `const` are block-scoped.
- TDZ means `let`/`const` cannot be used before initialization.
- Use `===` by default.
- `value == null` intentionally checks null or undefined.
- `Object.is` handles `NaN` and `-0` edge cases differently.
- `+` may concatenate strings.
- `Number.isNaN` is safer than global `isNaN`.
- Floating point math can be imprecise.
- BigInt cannot mix directly with number.
- Arrow functions capture lexical `this`.
- Functions are first-class values.
- Spread creates shallow copies.
- `structuredClone` is a better general deep clone than JSON tricks when supported.
- `sort` mutates and sorts as strings by default.
- Use `??` when `0`, `false`, or `""` are valid values.
- Do not put a newline immediately after `return`.
- Validate external input at runtime.

---

## 34. Official Source Notes

Use these sources when refreshing JavaScript core details:

- ECMAScript specification: `https://tc39.es/ecma262/`
- MDN JavaScript Guide: `https://developer.mozilla.org/en-US/docs/Web/JavaScript/Guide`
- MDN JavaScript Reference: `https://developer.mozilla.org/en-US/docs/Web/JavaScript/Reference`
- MDN Equality comparisons: `https://developer.mozilla.org/en-US/docs/Web/JavaScript/Guide/Equality_comparisons_and_sameness`
- MDN Grammar and types: `https://developer.mozilla.org/en-US/docs/Web/JavaScript/Guide/Grammar_and_types`
- MDN Functions: `https://developer.mozilla.org/en-US/docs/Web/JavaScript/Guide/Functions`
- MDN Strict mode: `https://developer.mozilla.org/en-US/docs/Web/JavaScript/Reference/Strict_mode`

Interview safety line:

```text
For JavaScript fundamentals, I separate language rules from runtime APIs. I use strict equality,
explicit conversion, precise absence checks, immutable update patterns, and runtime validation
at system boundaries.
```
