# JavaScript Scope, Hoisting, TDZ, and Prototypal Inheritance

## 1. The Full Picture — Event Loop First

Before understanding `var` vs `let`, you must understand **why setTimeout runs after the loop at all**.

```
JavaScript is single-threaded.
It runs one thing at a time.
setTimeout does NOT pause execution — it schedules a callback.
```

### JavaScript's event loop in plain English

```
1. Run all synchronous code (the for loop is synchronous)
2. Only after the call stack is EMPTY does the event loop
   pick up queued callbacks (setTimeout callbacks)
```

So this is the sequence regardless of `var` or `let`:

```text
Tick 1: loop iteration 0  → registers callback-0  in the task queue
Tick 2: loop iteration 1  → registers callback-1  in the task queue
Tick 3: loop iteration 2  → registers callback-2  in the task queue
Tick 4: loop condition 3  → 3 < 3 is false, loop ends
Tick 5: call stack is empty → event loop fires callback-0
Tick 6: event loop fires callback-1
Tick 7: event loop fires callback-2
```

This **same timing happens with both var and let**.
The difference is what `i` the callbacks are POINTING TO.

---

## 2. Why `var` prints `3 3 3`

### What `var` actually does in memory

`var` is **function-scoped** (or global-scoped). It does not care about `{}` blocks.

This loop:

```js
for (var i = 0; i < 3; i++) {
  setTimeout(() => console.log(i), 0);
}
```

Is IDENTICAL to writing:

```js
var i;          // hoisted to the top — ONE variable in memory

i = 0;          // iteration 0
setTimeout(() => console.log(i), 0);  // callback captures reference to i

i = 1;          // iteration 1
setTimeout(() => console.log(i), 0);  // same reference to same i

i = 2;          // iteration 2
setTimeout(() => console.log(i), 0);  // same reference to same i

i = 3;          // loop condition fails, loop stops
                // i is now 3 and stays 3
// ---- call stack empty ----
// callback 0 runs → looks up i → i is 3 → prints 3
// callback 1 runs → looks up i → i is 3 → prints 3
// callback 2 runs → looks up i → i is 3 → prints 3
```

All three callbacks hold a **reference to the same variable `i`**.
They do not hold a snapshot of the value at the time they were created.
By the time they run, `i` has been overwritten to `3`.

```text
Memory diagram:

[ i → 3 ] ← all 3 callbacks point here (same box in memory)

callback-0 says: "go find i"   → finds 3
callback-1 says: "go find i"   → finds 3
callback-2 says: "go find i"   → finds 3
```

---

## 3. Why `let` prints `0 1 2`

This is the KEY part. The timing is IDENTICAL. The callbacks still run after the loop.
But `let` has a special behavior in `for` loops.

```js
for (let i = 0; i < 3; i++) {
  setTimeout(() => console.log(i), 0);
}
```

### What `let` actually does in a for loop

JavaScript **creates a brand new `i` for every single iteration**.
It is as if a fresh variable is born each loop iteration, with the current value copied in.

The engine treats the `let` version as if it secretly does this:

```js
// Iteration 0:
{
  let i_0 = 0;          // NEW variable, separate box in memory
  setTimeout(() => console.log(i_0), 0);  // callback captures i_0
}

// Iteration 1:
{
  let i_1 = 1;          // ANOTHER new variable, separate box
  setTimeout(() => console.log(i_1), 0);  // callback captures i_1
}

// Iteration 2:
{
  let i_2 = 2;          // ANOTHER new variable, separate box
  setTimeout(() => console.log(i_2), 0);  // callback captures i_2
}

// ---- call stack empty ----
// callback 0 runs → looks up i_0 → still 0 → prints 0
// callback 1 runs → looks up i_1 → still 1 → prints 1
// callback 2 runs → looks up i_2 → still 2 → prints 2
```

Each callback has its own private `i`. Nobody else writes to it. So it stays the value it was born with.

```text
Memory diagram:

[ i_0 → 0 ] ← callback-0 points here
[ i_1 → 1 ] ← callback-1 points here
[ i_2 → 2 ] ← callback-2 points here

Three separate boxes. Each callback knows exactly which box is its own.
```

### The one-sentence difference

```
var: one shared variable, all callbacks share the same reference, last write wins.
let: one new variable per iteration, each callback holds its own private reference.
```

---

## 4. Proving it with a non-async example

You might think: "maybe it's only about async?" No. Even synchronous code shows the same behavior.

```js
// var - shared reference
var funcs = [];
for (var i = 0; i < 3; i++) {
  funcs.push(() => i);    // capture i
}
console.log(funcs[0]()); // 3
console.log(funcs[1]()); // 3
console.log(funcs[2]()); // 3
// Loop is done, i=3. All functions see the same i.

// let - per-iteration binding
var funcs2 = [];
for (let i = 0; i < 3; i++) {
  funcs2.push(() => i);   // capture per-iteration i
}
console.log(funcs2[0]()); // 0
console.log(funcs2[1]()); // 1
console.log(funcs2[2]()); // 2
// Each function has its own i.
```

Nothing async here. Same result.

---

## 5. The IIFE Fix — Why it also works with `var`

```js
for (var i = 0; i < 3; i++) {
  ((copy) => {
    setTimeout(() => console.log(copy), 0);
  })(i);   // immediately invoke and pass current i as argument
}
```

When you call `(i)` immediately, the current value of `i` (0, then 1, then 2) is passed **by value** into `copy`.
`copy` is a new local variable inside that function. Changing `i` later does not affect `copy`.

This is the manual version of what `let` does automatically for you.

---

## 6. Lexical Scope (Core Idea)

Lexical scope means variable access is determined by **where code is written**, not where it is called.

```js
const globalVar = "I am global";

function outer() {
  const outerVar = "I am outer";

  function inner() {
    const innerVar = "I am inner";
    console.log(globalVar); // accessible
    console.log(outerVar);  // accessible
    console.log(innerVar);  // accessible
  }

  inner();
}

outer();
```

`inner` can access its own scope + outer scopes. This chain is called the **scope chain**.

---

## 4. `var` vs `let` vs `const`

## `var`

- Function-scoped
- Can be re-declared
- Can be updated
- Hoisted and initialized with `undefined`

```js
var a = 10;
var a = 20; // allowed
a = 30;     // allowed
```

## `let`

- Block-scoped
- Cannot be re-declared in same scope
- Can be updated
- Hoisted, but in TDZ until declaration line

```js
let b = 10;
// let b = 20; // error (same scope)
b = 30;        // allowed
```

## `const`

- Block-scoped
- Cannot be re-declared
- Cannot be re-assigned
- Hoisted, but in TDZ until declaration line

```js
const c = 10;
// c = 20; // error
```

Important: `const` means binding cannot change, not that object content is immutable.

```js
const user = { name: "Aravind" };
user.name = "Aru"; // allowed
// user = {};       // error
```

---

## 5. Hoisting

Hoisting means declarations are processed before execution.

### `var` hoisting

```js
console.log(x); // undefined
var x = 5;
console.log(x); // 5
```

Engine view:

```js
var x;
console.log(x); // undefined
x = 5;
```

### Function declaration hoisting

```js
sayHi(); // works

function sayHi() {
  console.log("Hi");
}
```

### `let`/`const` hoisting

They are hoisted too, but not initialized immediately. Access before declaration causes error due to TDZ.

```js
// console.log(y); // ReferenceError
let y = 10;
```

---

## 6. Temporal Dead Zone (TDZ)

TDZ is the time between entering scope and the declaration line of `let`/`const`.

```js
{
  // TDZ starts
  // console.log(score); // ReferenceError
  let score = 99;
  console.log(score); // 99
  // TDZ ends after declaration
}
```

Why TDZ exists:

- Prevents accidental usage before initialization
- Makes code safer and less bug-prone

---

## 7. Prototypal Inheritance (Clear Model)

In JavaScript, objects inherit from other objects through the prototype chain.

If property/method is missing on object, JS looks up the chain.

### Example 1: `Object.create`

```js
const animal = {
  eats: true,
  speak() {
    console.log("Some sound");
  }
};

const dog = Object.create(animal);
dog.bark = function () {
  console.log("Woof");
};

console.log(dog.eats); // true (from prototype)
dog.speak();           // Some sound (from prototype)
dog.bark();            // Woof (own method)
```

Lookup flow for `dog.eats`:

1. Check `dog` object
2. Not found -> check `dog.__proto__` (which is `animal`)
3. Found in `animal`

### Example 2: Constructor + prototype

```js
function Person(name) {
  this.name = name;
}

Person.prototype.greet = function () {
  console.log(`Hello, I am ${this.name}`);
};

const p1 = new Person("Aravind");
const p2 = new Person("Nexturn");

p1.greet(); // Hello, I am Aravind
p2.greet(); // Hello, I am Nexturn
```

`greet` is shared through prototype, not copied per object.

### Example 3: ES6 class syntax (still prototypes under the hood)

```js
class Vehicle {
  move() {
    console.log("Vehicle moves");
  }
}

class Car extends Vehicle {
  honk() {
    console.log("Car honks");
  }
}

const car = new Car();
car.move(); // inherited
car.honk(); // own method
```

`class` is syntactic sugar over prototype-based inheritance.

---

## 8. One Combined Demo Program

Run this file as a quick interview-ready refresher:

```js
console.log("--- var in loop with setTimeout ---");
for (var i = 0; i < 3; i++) {
  setTimeout(() => console.log("var i:", i), 0);
}

console.log("--- let in loop with setTimeout ---");
for (let j = 0; j < 3; j++) {
  setTimeout(() => console.log("let j:", j), 0);
}

console.log("--- hoisting ---");
console.log(a); // undefined
var a = 42;

try {
  console.log(b); // ReferenceError (TDZ)
} catch (err) {
  console.log("Accessing let before declaration:", err.message);
}
let b = 100;

console.log("--- lexical scope ---");
const globalName = "Global";
function outerFn() {
  const outerName = "Outer";
  function innerFn() {
    console.log(globalName, outerName);
  }
  innerFn();
}
outerFn();

console.log("--- prototypal inheritance ---");
const parent = { role: "parent", work() { console.log("working"); } };
const child = Object.create(parent);
child.name = "child";
console.log(child.role); // from prototype
child.work();
```

Expected key output pattern:

```text
var i: 3
var i: 3
var i: 3
let j: 0
let j: 1
let j: 2
```

---

## 9. Interview-Ready Short Answers

### Why `3 3 3`?

Because `var` creates one function-scoped `i`. `setTimeout` callbacks run after loop ends, so all read final value `3`.

### Difference between `var`, `let`, `const`?

- `var`: function-scoped, re-declare allowed, hoisted with `undefined`
- `let`: block-scoped, re-declare not allowed, TDZ before declaration
- `const`: block-scoped, no reassignment, TDZ before declaration

### What is hoisting?

JavaScript moves declarations to the top of scope during compilation phase. `var` gets `undefined`; `let`/`const` stay uninitialized until declaration line.

### What is TDZ?

The period from scope start to declaration line where `let`/`const` exist but cannot be accessed.

### What is prototypal inheritance?

Objects inherit properties/methods from other objects via prototype chain lookup.

---

## 10. Quick Memory Trick

- `var` -> function box
- `let`/`const` -> block box
- Hoisting -> declaration remembered early
- TDZ -> "declared but not usable yet"
- Prototype -> fallback object chain
