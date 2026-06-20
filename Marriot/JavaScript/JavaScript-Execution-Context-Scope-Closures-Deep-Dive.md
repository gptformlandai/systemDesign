# JavaScript Execution Context, Scope, And Closures Deep Dive

Target: JavaScript interviews where the interviewer wants to know whether you understand how code actually runs, not only what syntax does.

This sheet covers:
- Execution context mental model
- Creation phase and execution phase
- Global, function, module, and eval execution contexts
- Call stack
- Lexical environment and variable environment
- Scope chain and identifier lookup
- Hoisting
- Temporal dead zone
- Function declarations vs function expressions
- Block scope and shadowing
- Closures
- Closure use cases
- Closure traps with loops and async callbacks
- Memory retention and production leaks
- Interview output questions and strong answers

How to use this:
- First learn the execution flow.
- Then use that flow to explain hoisting, scope, and closure questions.
- For output questions, identify the scope first, then the declaration type, then the execution order.
- Practice saying the strong answers out loud in 30-60 seconds.

---

## 1. Mental Model

JavaScript does not execute code as a flat text file.

It runs code inside execution contexts.

Simple model:

```text
Source code
    -> create execution context
    -> allocate declarations and scope links
    -> execute line by line
    -> push/pop function calls on call stack
```

Every time a function runs, JavaScript creates a new function execution context.

Core idea:

```text
Execution context answers: what code is running right now?
Scope answers: where does JavaScript look for variables?
Closure answers: why can a function remember variables after the outer function returned?
```

Strong interview line:

```text
JavaScript execution is based on execution contexts and lexical scope. When code runs, the
engine creates a context with bindings for variables and functions, links it to outer scopes,
and uses the call stack to track active function calls. Closures work because inner functions
keep access to the lexical environment where they were created.
```

---

## 2. Interview Priority Meter

| Topic | Priority | Why It Matters |
|---|---:|---|
| Execution context | Very high | Foundation for hoisting and closures |
| Call stack | Very high | Explains function execution order |
| Scope chain | Very high | Explains variable lookup |
| Hoisting | Very high | Common output questions |
| Temporal dead zone | Very high | `let`/`const` traps |
| Function declaration vs expression | Very high | Hoisting behavior |
| Closures | Very high | Core JavaScript concept |
| Closure loop trap | Very high | Classic interview question |
| Closures with async callbacks | Very high | Real frontend/backend bug source |
| Memory retention | High | Senior production maturity |
| Block scope | High | Modern JS correctness |
| Shadowing | High | Debugging and readability |
| Module scope | Medium-high | Modern code organization |
| IIFE | Medium | Legacy and closure pattern awareness |
| `eval` / `with` caution | Low-medium | Senior safety awareness |

---

## 3. JavaScript Execution Flow

When JavaScript starts running a script, the engine creates a global execution context.

Then function calls create function execution contexts.

Example:

```javascript
const appName = "booking";

function greet(user) {
    const message = `Hello ${user}`;
    return message;
}

const result = greet("Ava");
console.log(result);
```

High-level flow:

```text
1. Create global execution context.
2. Register global declarations.
3. Execute global code line by line.
4. When greet("Ava") is called, create function execution context.
5. Bind parameter user = "Ava".
6. Create local binding message.
7. Return message.
8. Pop greet context from call stack.
9. Continue global code.
```

Memory line:

```text
Global code creates the first context. Every function call creates another context.
```

---

## 4. Execution Context Components

A simplified execution context contains:

| Component | Meaning |
|---|---|
| Lexical environment | Bindings for `let`, `const`, functions, classes, and outer scope link |
| Variable environment | Bindings for `var` in simplified interview explanations |
| This binding | Value of `this` in that context |
| Outer reference | Link to parent lexical environment |

Simplified picture:

```text
Function Execution Context
    Lexical Environment
        let / const / function / class bindings
        outer -> parent lexical environment

    Variable Environment
        var bindings

    this binding
        depends on call style
```

Important:

```text
The exact ECMAScript spec has precise internal structures. In interviews, explain the
practical model clearly without pretending the runtime literally stores it as a simple object.
```

Strong answer:

```text
An execution context is the runtime environment for executing code. It tracks local bindings,
outer scope, and this. The scope chain comes from linked lexical environments.
```

---

## 5. Creation Phase And Execution Phase

JavaScript context setup has two useful interview phases:

```text
Creation phase
    -> allocate declarations
    -> set up scope links
    -> initialize var to undefined
    -> keep let/const in TDZ until declaration runs
    -> create function declarations

Execution phase
    -> run code line by line
    -> assign values
    -> call functions
```

Example:

```javascript
console.log(count);
var count = 10;
```

Mental model:

```text
Creation phase:
count exists and is initialized to undefined.

Execution phase:
console.log(count) prints undefined.
count = 10 runs after that.
```

Now with `let`:

```javascript
console.log(count);
let count = 10;
```

Mental model:

```text
Creation phase:
count binding exists but is uninitialized.

Execution phase:
access before declaration throws ReferenceError because count is in TDZ.
```

Strong line:

```text
Hoisting is easiest to explain with creation and execution phases. Declarations are prepared
before execution, but different declaration types are initialized differently.
```

---

## 6. Global Execution Context

Global code runs inside the global execution context.

Browser example:

```javascript
var legacyName = "app";
let modernName = "booking";

console.log(window.legacyName); // app in browser script
console.log(window.modernName); // undefined
```

Important distinction in browser scripts:

```text
Top-level var can become a property on the global object.
Top-level let and const do not become global object properties.
```

Node.js nuance:

```javascript
var name = "app";
console.log(global.name); // usually undefined in CommonJS module scope
```

Why:

```text
Node.js CommonJS files are wrapped in a module function, so top-level variables are module-scoped,
not true global object properties.
```

Interview-safe line:

```text
Global behavior depends on script vs module and browser vs Node.js. In browser scripts,
var can attach to window, but let and const do not. In modules, top-level bindings are module-scoped.
```

---

## 7. Function Execution Context

Every function call creates a new function execution context.

Example:

```javascript
function add(a, b) {
    const total = a + b;
    return total;
}

console.log(add(2, 3));
```

When `add(2, 3)` runs:

```text
Function context for add
    a -> 2
    b -> 3
    total -> 5 after execution reaches declaration
    outer -> global lexical environment
```

Each call gets its own context:

```javascript
function counter(start) {
    let value = start;
    value++;
    return value;
}

console.log(counter(1));  // 2
console.log(counter(10)); // 11
```

`value` is different for each call.

Strong answer:

```text
A function execution context is created for each function call. Parameters and local variables
belong to that call, so separate calls do not share local variables unless a closure keeps a
lexical environment alive.
```

---

## 8. Call Stack

The call stack tracks active execution contexts.

Example:

```javascript
function first() {
    second();
}

function second() {
    third();
}

function third() {
    console.log("done");
}

first();
```

Stack flow:

```text
push global
push first
push second
push third
console.log("done")
pop third
pop second
pop first
continue global
```

Stack overflow:

```javascript
function recurse() {
    recurse();
}

recurse(); // RangeError: Maximum call stack size exceeded
```

Why:

```text
Each recursive call adds a new function execution context. Without a base case, the stack grows
until the runtime limit is reached.
```

Interview line:

```text
The call stack is synchronous. It tells us which function is currently running and what called
it. Async callbacks run later only after the current stack is clear.
```

Async scheduling is covered deeply in the event loop sheet.

---

## 9. Lexical Scope

Lexical scope means scope is determined by where code is written, not where a function is called.

Example:

```javascript
const name = "global";

function outer() {
    const name = "outer";

    function inner() {
        console.log(name);
    }

    return inner;
}

const fn = outer();
fn(); // outer
```

Why:

```text
inner was created inside outer, so it remembers outer's lexical environment.
It does not look at where fn() is called.
```

Strong answer:

```text
JavaScript uses lexical scope. A function's accessible variables are determined by the location
where the function is defined in source code, not by the location where it is invoked.
```

---

## 10. Scope Chain

When JavaScript needs a variable, it looks through the scope chain.

Example:

```javascript
const globalName = "global";

function outer() {
    const outerName = "outer";

    function inner() {
        const innerName = "inner";
        console.log(innerName);
        console.log(outerName);
        console.log(globalName);
    }

    inner();
}

outer();
```

Lookup flow inside `inner`:

```text
innerName -> inner lexical environment
outerName -> outer lexical environment
globalName -> global lexical environment
```

If not found:

```javascript
console.log(missingValue); // ReferenceError
```

Interview line:

```text
Identifier lookup starts in the current lexical environment and walks outward through parent
scopes until it finds the binding or reaches the global scope.
```

---

## 11. Scope Types

JavaScript commonly uses these scopes:

| Scope Type | Created By | Example |
|---|---|---|
| Global scope | Script/global code | Top-level script declarations |
| Module scope | ES module file | Top-level module declarations |
| Function scope | Function call | `function run() {}` |
| Block scope | `{}` with `let`/`const`/class | `if`, `for`, standalone block |
| Catch scope | `catch (error)` | Error binding scope |

Block scope example:

```javascript
if (true) {
    let message = "inside";
    const count = 1;
}

console.log(message); // ReferenceError
```

Function scope example:

```javascript
function run() {
    var value = 10;
}

console.log(value); // ReferenceError
```

Interview line:

```text
var is function-scoped, while let and const are block-scoped. Modern JavaScript relies heavily
on block scope to avoid accidental variable leakage.
```

---

## 12. Hoisting Rules Summary

Hoisting behavior by declaration type:

| Declaration | Hoisted? | Initialization Before Line Runs | Access Before Declaration |
|---|---:|---|---|
| `var` | Yes | `undefined` | `undefined` |
| `let` | Yes | Uninitialized | ReferenceError |
| `const` | Yes | Uninitialized | ReferenceError |
| Function declaration | Yes | Function object | Works |
| Function expression with `var` | `var` hoisted | `undefined` | TypeError if called |
| Function expression with `let`/`const` | Binding hoisted | Uninitialized | ReferenceError |
| Class declaration | Yes | Uninitialized | ReferenceError |

Example:

```javascript
sayHi(); // works

function sayHi() {
    console.log("hi");
}
```

Function expression trap:

```javascript
sayHi(); // TypeError: sayHi is not a function

var sayHi = function () {
    console.log("hi");
};
```

`let` expression trap:

```javascript
sayHi(); // ReferenceError

const sayHi = function () {
    console.log("hi");
};
```

Strong answer:

```text
Hoisting does not mean code physically moves. It means declarations are processed during
context creation. var is initialized to undefined, function declarations are initialized to
the function object, and let/const/class bindings exist but cannot be used before initialization.
```

---

## 13. Temporal Dead Zone

Temporal dead zone is the time between entering a scope and initializing a `let`, `const`, or class binding.

Example:

```javascript
{
    console.log(count); // ReferenceError
    let count = 10;
}
```

Important:

```text
The variable exists in the scope, but it is not initialized yet.
```

Shadowing TDZ trap:

```javascript
let value = 10;

{
    console.log(value); // ReferenceError
    let value = 20;
}
```

Why:

```text
The inner let value shadows the outer value for the entire block, including the TDZ region.
```

Strong answer:

```text
TDZ prevents access to let, const, and class bindings before their declaration is evaluated.
This catches bugs that var would silently turn into undefined.
```

---

## 14. Function Declaration vs Function Expression

Function declaration:

```javascript
function calculate() {
    return 10;
}
```

Function expression:

```javascript
const calculate = function () {
    return 10;
};
```

Arrow function expression:

```javascript
const calculate = () => 10;
```

Hoisting difference:

```javascript
console.log(declared()); // 10

function declared() {
    return 10;
}

console.log(expressed()); // ReferenceError

const expressed = function () {
    return 20;
};
```

Interview line:

```text
Function declarations are hoisted with the function body. Function expressions follow the
hoisting behavior of the variable they are assigned to.
```

Production guidance:

```text
Use declarations when you want named reusable functions and expressions when passing behavior
or keeping functions close to a local scope. Consistency matters more than dogma.
```

---

## 15. Block Scope

`let`, `const`, and class declarations are block-scoped.

```javascript
for (let i = 0; i < 3; i++) {
    const value = i * 2;
    console.log(value);
}

console.log(i); // ReferenceError
```

`var` is not block-scoped:

```javascript
for (var i = 0; i < 3; i++) {
}

console.log(i); // 3
```

Why it matters:

```text
Block scope prevents loop variables and temporary variables from leaking into outer scopes.
```

Strong answer:

```text
Modern JavaScript uses block scope for let and const. This makes loops, conditionals, and
temporary variables safer because bindings do not escape the block where they are needed.
```

---

## 16. Shadowing

Shadowing happens when an inner scope declares a variable with the same name as an outer scope.

```javascript
const role = "user";

function printRole() {
    const role = "admin";
    console.log(role);
}

printRole(); // admin
console.log(role); // user
```

Shadowing is legal, but too much shadowing hurts readability.

TDZ shadowing trap:

```javascript
const role = "user";

function printRole() {
    console.log(role); // ReferenceError
    const role = "admin";
}

printRole();
```

Why:

```text
The inner const role shadows the outer role from the start of the function scope, but it is in
TDZ until its declaration runs.
```

Production line:

```text
I avoid shadowing important names like user, request, response, error, or config in nested
scopes because it makes debugging harder.
```

---

## 17. Closures: Definition

A closure is a function plus access to its lexical environment.

Simple example:

```javascript
function outer() {
    const message = "hello";

    function inner() {
        console.log(message);
    }

    return inner;
}

const fn = outer();
fn(); // hello
```

Why it works:

```text
outer finished executing, but inner still has access to the lexical environment where it was
created. The needed binding message remains reachable.
```

Strong answer:

```text
A closure is created when a function remembers variables from the lexical scope where it was
defined, even if that outer function has already returned.
```

---

## 18. Closure Step By Step

Code:

```javascript
function createCounter() {
    let count = 0;

    return function increment() {
        count++;
        return count;
    };
}

const counter = createCounter();
console.log(counter()); // 1
console.log(counter()); // 2
```

Step-by-step:

```text
1. Global context is created.
2. createCounter is called.
3. Function context for createCounter is created.
4. count binding is created with value 0.
5. increment function is created inside createCounter.
6. increment closes over count.
7. createCounter returns increment.
8. createCounter context is popped from the call stack.
9. count is still reachable through the closure.
10. counter() updates the same count binding each time.
```

Important nuance:

```text
A closure captures bindings, not a frozen snapshot of values.
```

Example:

```javascript
function outer() {
    let value = 1;

    const getValue = () => value;
    value = 2;

    return getValue;
}

console.log(outer()()); // 2
```

---

## 19. Closure Use Cases

Closures are used everywhere in JavaScript.

| Use Case | Example |
|---|---|
| Encapsulation | Private state in functions |
| Function factories | Create configured functions |
| Callbacks | Remember values when callback runs later |
| Event handlers | Remember component/user/action state |
| Async code | Remember request or timer state |
| Memoization | Cache values in a hidden map |
| Partial application | Pre-fill arguments |
| Module pattern | Hide implementation details |

Example: function factory

```javascript
function createMultiplier(factor) {
    return function multiply(value) {
        return value * factor;
    };
}

const double = createMultiplier(2);
const triple = createMultiplier(3);

console.log(double(10)); // 20
console.log(triple(10)); // 30
```

Interview line:

```text
Closures let us create functions that carry state or configuration without exposing that state
globally.
```

---

## 20. Private State With Closures

Before classes with private fields were common, closures were a classic way to hide state.

```javascript
function createBankAccount(initialBalance) {
    let balance = initialBalance;

    return {
        deposit(amount) {
            if (amount <= 0) {
                throw new Error("amount must be positive");
            }
            balance += amount;
            return balance;
        },
        withdraw(amount) {
            if (amount > balance) {
                throw new Error("insufficient funds");
            }
            balance -= amount;
            return balance;
        },
        getBalance() {
            return balance;
        }
    };
}

const account = createBankAccount(100);
account.deposit(50);
console.log(account.getBalance()); // 150
console.log(account.balance); // undefined
```

Why:

```text
balance is not a public property. It is a local binding kept alive by closures.
```

Production caution:

```text
Closures can hide state well, but hidden mutable state can still make tests and debugging hard
if overused.
```

---

## 21. Closure Loop Trap With `var`

Classic question:

```javascript
for (var i = 0; i < 3; i++) {
    setTimeout(function () {
        console.log(i);
    }, 0);
}
```

Output:

```text
3
3
3
```

Why:

```text
var is function-scoped, so all callbacks close over the same i binding. By the time callbacks
run, the loop has finished and i is 3.
```

Fix with `let`:

```javascript
for (let i = 0; i < 3; i++) {
    setTimeout(function () {
        console.log(i);
    }, 0);
}
```

Output:

```text
0
1
2
```

Why:

```text
let creates a new block-scoped binding for each loop iteration.
```

Legacy fix with IIFE:

```javascript
for (var i = 0; i < 3; i++) {
    (function (current) {
        setTimeout(function () {
            console.log(current);
        }, 0);
    })(i);
}
```

Strong answer:

```text
The var version closes over one shared function-scoped binding. The let version gives each
iteration its own binding, so each callback remembers the correct value.
```

---

## 22. Closures With Async Callbacks

Closures are common in async code.

```javascript
function loadUser(userId) {
    return fetch(`/api/users/${userId}`)
        .then(response => response.json())
        .then(user => {
            console.log("loaded", userId, user.name);
            return user;
        });
}
```

The callback remembers `userId`.

Race condition example:

```javascript
let currentRequestId = 0;

async function search(query) {
    const requestId = ++currentRequestId;
    const response = await fetch(`/api/search?q=${encodeURIComponent(query)}`);
    const results = await response.json();

    if (requestId !== currentRequestId) {
        return;
    }

    render(results);
}
```

Why closure matters:

```text
Each search call has its own requestId binding. The async continuation closes over it and can
compare it with the latest global request id.
```

Production line:

```text
Closures make async state convenient, but I still need explicit cancellation, stale-response
checks, or AbortController when multiple requests can overlap.
```

---

## 23. Closures And Event Handlers

Event handlers often close over state.

```javascript
function createButtonHandler(buttonId) {
    return function handleClick(event) {
        console.log("clicked", buttonId, event.type);
    };
}

const handler = createButtonHandler("save-button");
```

Frontend memory leak trap:

```javascript
function attachHandler(element, largeData) {
    element.addEventListener("click", function () {
        console.log(largeData.id);
    });
}
```

If the listener is not removed and `largeData` is large, it may stay reachable longer than expected.

Better lifecycle discipline:

```javascript
function attachHandler(element, dataId) {
    function handleClick() {
        console.log(dataId);
    }

    element.addEventListener("click", handleClick);

    return function cleanup() {
        element.removeEventListener("click", handleClick);
    };
}
```

Interview line:

```text
Event handlers are closures. They remember variables from the scope where they were created,
which is useful for state but can retain memory if handlers are not cleaned up.
```

---

## 24. Closures And Memory Retention

A closure can keep variables alive after a function returns.

This is useful:

```javascript
function createCache() {
    const cache = new Map();

    return {
        get(key) {
            return cache.get(key);
        },
        set(key, value) {
            cache.set(key, value);
        }
    };
}
```

But it can also retain too much memory:

```javascript
function createProcessor(largeDataset) {
    return function process(id) {
        return largeDataset.find(item => item.id === id);
    };
}
```

If `process` lives for the life of the app, `largeDataset` also stays reachable.

Senior explanation:

```text
Garbage collection removes objects that are no longer reachable. A closure can keep an outer
binding reachable. If that binding points to a large object, event listener, DOM node, cache,
or request data, memory can grow unexpectedly.
```

Production fixes:

- Store only the fields needed by the closure.
- Remove event listeners during cleanup.
- Clear timers and intervals.
- Bound caches.
- Null out references when lifecycle ends.
- Avoid capturing full request/response objects in long-lived callbacks.
- Use heap snapshots to confirm retained paths.

---

## 25. Closure Memory Leak Patterns

| Pattern | Why It Leaks | Better Approach |
|---|---|---|
| Long-lived event listener captures large object | Listener keeps closure alive | Remove listener, capture small ID only |
| `setInterval` captures state forever | Timer keeps callback reachable | Clear interval on cleanup |
| Unbounded memoization cache | Closure hides ever-growing Map | Add size/TTL eviction |
| Global callback array | References callbacks forever | Unregister callbacks |
| Capturing request object in server singleton | Request data retained across requests | Extract needed fields only |
| Detached DOM node in closure | DOM cannot be collected | Remove references and listeners |
| Debug logging closure stores payloads | Logs/callbacks retain sensitive data | Log safe IDs, not full objects |

Example timer leak:

```javascript
function startPolling(user) {
    const intervalId = setInterval(() => {
        console.log(user.id);
    }, 1000);

    return function stop() {
        clearInterval(intervalId);
    };
}
```

Good because it returns cleanup.

Interview line:

```text
A closure is not a leak by itself. It becomes a leak when a long-lived reference keeps a closure
alive after the captured data should have been released.
```

---

## 26. Module Scope And Closures

ES modules have their own module scope.

```javascript
// counter.js
let count = 0;

export function increment() {
    count++;
    return count;
}
```

Consumers cannot directly access `count` unless exported.

```javascript
import { increment } from "./counter.js";

console.log(increment()); // 1
console.log(increment()); // 2
```

Mental model:

```text
Module-level bindings can act like private state for exported functions.
```

Production caution:

```text
Module-level mutable state is shared by all imports in the same runtime instance. In Node.js
servers, this can accidentally share state across requests.
```

Strong answer:

```text
Modules create a scope boundary. Exported functions can close over module-level variables,
which is useful for encapsulation but dangerous for per-request state if used carelessly.
```

---

## 27. IIFE Pattern

IIFE means Immediately Invoked Function Expression.

```javascript
(function () {
    const privateValue = 10;
    console.log(privateValue);
})();
```

Why it was used historically:

- Create a private scope before block scope and modules were common.
- Avoid polluting global scope.
- Capture loop variables in old JavaScript.

Legacy loop fix:

```javascript
for (var i = 0; i < 3; i++) {
    (function (current) {
        setTimeout(() => console.log(current), 0);
    })(i);
}
```

Modern replacement:

```javascript
for (let i = 0; i < 3; i++) {
    setTimeout(() => console.log(i), 0);
}
```

Interview line:

```text
IIFE is an older closure pattern for creating private scope. Modern JavaScript usually uses
let/const block scope and ES modules instead.
```

---

## 28. `eval` And `with` Caution

`eval` executes a string as code.

```javascript
eval("console.log('hello')");
```

Problems:

- Security risk.
- Harder optimization.
- Harder static analysis.
- Can interfere with scope reasoning.

`with` changes scope lookup and is disallowed in strict mode.

```javascript
// Avoid this
with (user) {
    console.log(name);
}
```

Interview line:

```text
I avoid eval and with in production. They make scope harder to reason about, reduce optimizer
confidence, and can introduce serious security risks.
```

---

## 29. Output Question Strategy

For scope/closure output questions, use this process:

1. Identify declaration type: `var`, `let`, `const`, function declaration, function expression.
2. Identify scope: global, function, block, module.
3. Identify creation phase behavior: initialized or TDZ?
4. Identify execution order.
5. Identify closure: shared binding or per-call/per-iteration binding?
6. Identify async timing if callbacks are involved.

Example:

```javascript
var x = 1;

function run() {
    console.log(x);
    var x = 2;
}

run();
```

Reasoning:

```text
Inside run, var x is function-scoped and hoisted as undefined.
The local x shadows global x.
console.log reads local x before assignment.
Output: undefined.
```

Output:

```text
undefined
```

---

## 30. Common Output Traps

### Trap 1: `var` Hoisting

```javascript
console.log(a);
var a = 10;
```

Output:

```text
undefined
```

Rule:

```text
var is hoisted and initialized to undefined.
```

### Trap 2: `let` TDZ

```javascript
console.log(a);
let a = 10;
```

Output:

```text
ReferenceError
```

Rule:

```text
let is hoisted but uninitialized until declaration runs.
```

### Trap 3: Function Declaration

```javascript
console.log(add(2, 3));

function add(a, b) {
    return a + b;
}
```

Output:

```text
5
```

Rule:

```text
Function declarations are hoisted with their function body.
```

### Trap 4: Function Expression With `var`

```javascript
console.log(add(2, 3));

var add = function (a, b) {
    return a + b;
};
```

Output:

```text
TypeError
```

Rule:

```text
add is undefined when called, so undefined is not a function.
```

### Trap 5: Closure Captures Binding

```javascript
function outer() {
    let value = 1;
    const fn = () => value;
    value = 2;
    return fn;
}

console.log(outer()());
```

Output:

```text
2
```

Rule:

```text
Closure captures the binding, not a frozen value snapshot.
```

### Trap 6: Loop With `var`

```javascript
for (var i = 0; i < 3; i++) {
    setTimeout(() => console.log(i), 0);
}
```

Output:

```text
3
3
3
```

Rule:

```text
All callbacks share one var binding.
```

### Trap 7: Loop With `let`

```javascript
for (let i = 0; i < 3; i++) {
    setTimeout(() => console.log(i), 0);
}
```

Output:

```text
0
1
2
```

Rule:

```text
Each iteration gets a new let binding.
```

---

## 31. Mini Program: `once`, `counter`, And `memoize`

These utilities show closures as production-style tools.

### `once`

```javascript
function once(fn) {
    let called = false;
    let result;

    return function (...args) {
        if (!called) {
            called = true;
            result = fn.apply(this, args);
        }
        return result;
    };
}

const initialize = once(() => {
    console.log("initializing");
    return { ready: true };
});

console.log(initialize());
console.log(initialize());
```

Why closure matters:

```text
called and result are private state kept by the returned function.
```

### `counter`

```javascript
function createCounter(start = 0) {
    let count = start;

    return {
        increment() {
            count++;
            return count;
        },
        decrement() {
            count--;
            return count;
        },
        value() {
            return count;
        }
    };
}
```

### `memoize`

```javascript
function memoize(fn) {
    const cache = new Map();

    return function (...args) {
        const key = JSON.stringify(args);

        if (cache.has(key)) {
            return cache.get(key);
        }

        const result = fn.apply(this, args);
        cache.set(key, result);
        return result;
    };
}
```

Production caution:

```text
Memoization caches can grow forever. Real production memoization usually needs a size limit,
TTL, or explicit invalidation.
```

---

## 32. Production Debugging: Closure Leak Scenario

Scenario:

```text
A single-page app becomes slower after navigating between pages many times. Heap usage grows.
Refreshing the page fixes it.
```

Possible closure-related causes:

- Event listeners are added but not removed.
- Intervals continue after component/page unmount.
- Closures capture large response objects.
- Detached DOM nodes stay reachable through callbacks.
- Global arrays store callbacks forever.
- Unbounded caches hidden inside closures.

Debug flow:

```text
1. Reproduce memory growth.
2. Take heap snapshot before and after repeated navigation.
3. Compare retained objects.
4. Inspect retaining paths.
5. Look for event listeners, timers, caches, or closures keeping objects alive.
6. Add cleanup and retest.
```

Strong answer:

```text
Closures can retain memory because they keep their lexical environment reachable. I would use
heap snapshots and retaining paths to find what keeps the data alive, then remove listeners,
clear timers, bound caches, or capture smaller values.
```

---

## 33. Common Mistakes

| Mistake | Why It Is Wrong | Better Approach |
|---|---|---|
| Saying hoisting moves code physically | Misleading mental model | Say declarations are processed during creation phase |
| Saying let/const are not hoisted | Incorrect | Say they are hoisted but in TDZ |
| Thinking closure captures value snapshot | Often wrong | Closure captures lexical binding |
| Using var in async loops | Shared binding trap | Use let or capture value explicitly |
| Keeping large objects in long-lived closures | Memory retention | Capture only needed fields |
| Forgetting cleanup for event listeners | Leaks DOM/data | Remove listeners on lifecycle end |
| Creating unbounded closure caches | Memory growth | Add size/TTL/invalidation |
| Using module state for request state | Cross-request bugs | Keep request data local or scoped |
| Overusing hidden closure state | Hard to test/debug | Prefer explicit state when clarity wins |
| Ignoring TDZ shadowing | Surprise ReferenceError | Avoid shadowing confusing names |

---

## 34. Strong Interview Answers

### What is an execution context?

```text
An execution context is the runtime environment where JavaScript code runs. It contains local
bindings, a link to the outer lexical environment, and a this binding. Global code creates the
first context, and each function call creates a new function execution context.
```

### What is the call stack?

```text
The call stack tracks active execution contexts. When a function is called, its context is
pushed onto the stack. When it returns, that context is popped. If calls keep nesting without
returning, the stack can overflow.
```

### What is hoisting?

```text
Hoisting means declarations are processed before execution within their scope. var is hoisted
and initialized to undefined. Function declarations are hoisted with their body. let, const,
and class bindings are hoisted but remain in the temporal dead zone until initialized.
```

### What is a closure?

```text
A closure is a function that remembers variables from the lexical scope where it was created,
even after the outer function has returned. Closures are used for callbacks, private state,
function factories, memoization, and async code.
```

### Can closures cause memory leaks?

```text
Closures do not leak by default, but they can retain memory if a long-lived callback keeps a
large object, DOM node, request object, timer state, or cache reachable after it should be
released. I debug that with heap snapshots and retaining paths.
```

---

## 35. FAANG-Level Question

> A frontend app has a search box. Users type quickly, old results sometimes overwrite new results, and memory grows after repeated navigation. Explain the JavaScript mechanics and how you would fix it.

Strong answer:

```text
There are two likely JavaScript mechanics involved. First, each async search call creates a
function execution context, and the async continuation closes over values like query or
requestId. If older requests finish later, their closures may still update the UI unless we
check whether the response is stale or cancel the request.

I would fix that with an AbortController or a monotonically increasing request id. Each request
captures its own id, and before rendering I compare it with the latest id. If it is stale, I
ignore it.

Second, memory growth after navigation often means a long-lived reference is keeping closures
alive. Event listeners, intervals, global callback lists, or unbounded caches can retain DOM
nodes or large response objects. I would take heap snapshots, inspect retaining paths, and add
cleanup: remove listeners, clear timers, abort fetches, and avoid capturing large objects when
only an id is needed.
```

That answer shows:

- Execution context understanding.
- Closure understanding.
- Async race-condition judgment.
- Production memory debugging maturity.
- Frontend lifecycle awareness.

---

## 36. Rapid Revision

- JavaScript runs code inside execution contexts.
- Global code creates the first execution context.
- Every function call creates a function execution context.
- The call stack tracks active execution contexts.
- Creation phase prepares declarations and scope links.
- Execution phase runs code line by line.
- Scope is lexical: where code is written matters.
- Scope chain lookup starts local and walks outward.
- `var` is function-scoped and initialized to `undefined`.
- `let` and `const` are block-scoped and have TDZ.
- Function declarations are callable before their declaration.
- Function expressions follow variable hoisting rules.
- Class declarations also have TDZ.
- TDZ shadowing can hide outer variables.
- Closures remember lexical bindings.
- Closures capture bindings, not frozen value snapshots.
- `var` loop callbacks share one binding.
- `let` loop callbacks get per-iteration bindings.
- Event handlers are closures.
- Async callbacks are closures.
- Closures can be used for private state.
- Closures can retain memory if long-lived.
- Remove listeners and clear timers.
- Bound caches hidden inside closures.
- Avoid storing per-request state in module-level variables.
- Debug closure leaks with heap snapshots and retaining paths.

---

## 37. Official Source Notes

Use these sources when refreshing execution context, scope, and closure details:

- ECMAScript specification: `https://tc39.es/ecma262/`
- MDN Closures: `https://developer.mozilla.org/en-US/docs/Web/JavaScript/Guide/Closures`
- MDN Grammar and types: `https://developer.mozilla.org/en-US/docs/Web/JavaScript/Guide/Grammar_and_types`
- MDN Functions: `https://developer.mozilla.org/en-US/docs/Web/JavaScript/Guide/Functions`
- MDN `let`: `https://developer.mozilla.org/en-US/docs/Web/JavaScript/Reference/Statements/let`
- MDN `const`: `https://developer.mozilla.org/en-US/docs/Web/JavaScript/Reference/Statements/const`
- MDN `var`: `https://developer.mozilla.org/en-US/docs/Web/JavaScript/Reference/Statements/var`
- MDN Memory management: `https://developer.mozilla.org/en-US/docs/Web/JavaScript/Memory_management`
- Chrome DevTools Memory: `https://developer.chrome.com/docs/devtools/memory/`

Interview safety line:

```text
I explain JavaScript scope through lexical environments and the scope chain, not by saying
variables magically move. I explain closures as retained lexical bindings, then mention both
their benefits and their memory-retention risks in production systems.
```
