# JavaScript Tricky Output Questions

> Goal: master JavaScript output questions by rules, not memorization: coercion, equality, hoisting, TDZ, closures, `this`, prototypes, classes, async order, event loop, arrays, objects, modules, and production-grade traps.

---

## 1. How To Use This Sheet

This sheet is for scenario-practice mode.

For each question:

1. Predict the output before reading the answer.
2. Name the rule involved.
3. Execute the code mentally line by line.
4. Explain the answer in 20-40 seconds.
5. Say the production trap.

Do not memorize outputs. Interviewers can change one character and break memorization.

Your target is to say:

> The output is X because JavaScript first does Y, then Z. The trap is A. In production I would avoid relying on this behavior by doing B.

---

## 2. The Output Question Method

Use this sequence for every tricky output question:

1. Identify declarations: `var`, `let`, `const`, function declarations, class declarations.
2. Mark hoisting and TDZ.
3. Identify execution context and scope chain.
4. Identify `this` binding at each call site.
5. Identify type coercion and equality rules.
6. Identify object reference vs primitive value behavior.
7. Identify sync code, microtasks, and macrotasks.
8. Identify mutation vs reassignment.
9. Identify prototype lookup.
10. Only then state output.

This is the difference between guessing and reasoning.

---

## 3. Fast Rule Table

| Topic | Rule |
|---|---|
| `var` | Hoisted and initialized to `undefined`. Function scoped. |
| `let`/`const` | Hoisted but uninitialized until declaration; TDZ before declaration. |
| Function declaration | Hoisted with function body. |
| Function expression | Variable follows `var`/`let`/`const` rules. |
| `==` | Allows coercion. Avoid in production except intentional `x == null`. |
| `===` | No type coercion except same type comparison rules. |
| `+` | String concatenation if either side becomes string; otherwise numeric addition. |
| `NaN` | Not equal to itself. Use `Number.isNaN`. |
| `this` | Determined by call site for normal functions; lexical for arrow functions. |
| Arrow function | Captures `this`, `arguments`, `super`, `new.target` from surrounding scope. |
| Object keys | Plain object keys are strings or symbols. |
| Map keys | Map keys preserve identity and type. |
| Promise callbacks | Microtasks run after current sync stack, before timers. |
| `setTimeout` | Macrotask, runs after microtasks. |
| `async` function | Returns a Promise. `await` pauses and resumes in microtask. |
| `sort` | Mutates array and sorts as strings unless comparator supplied. |
| `const` object | Binding is constant; object contents may mutate. |

---

## 4. Mental Output Template

Use this spoken template:

```text
The output is <answer>. The reason is <rule>. First <step one>, then <step two>. The trap is <trap>. In production, I would write it more explicitly as <safer pattern>.
```

Example:

```text
The output is undefined, then 10. The reason is var hoisting. The declaration is hoisted and initialized to undefined, but the assignment happens later. In production I would use let/const and avoid reading before declaration.
```

---

## 5. Coercion And Equality Questions

These questions test conversion rules.

---

## 6. Q1: String Plus Number

```js
console.log("5" + 1);
console.log("5" - 1);
```

Output:

```text
51
4
```

Why:

- `+` with a string performs concatenation.
- `-` performs numeric conversion.

Production trap:

- User input from forms is string by default. Convert explicitly with `Number(value)` when doing math.

---

## 7. Q2: Multiple Plus Operators

```js
console.log(1 + 2 + "3");
console.log("1" + 2 + 3);
```

Output:

```text
33
123
```

Why:

- Operators evaluate left to right.
- `1 + 2` is numeric `3`, then `3 + "3"` is string `"33"`.
- `"1" + 2` is `"12"`, then `"12" + 3` is `"123"`.

Production trap:

- Do not rely on mixed `+` behavior. Normalize values first.

---

## 8. Q3: Boolean Coercion With Plus

```js
console.log(true + true);
console.log(true + false);
console.log(false + false);
```

Output:

```text
2
1
0
```

Why:

- Numeric operators convert booleans to numbers.
- `true` becomes `1`, `false` becomes `0`.

Production trap:

- This is clever but unclear. Use explicit counts or conditionals.

---

## 9. Q4: Null And Undefined Math

```js
console.log(null + 1);
console.log(undefined + 1);
```

Output:

```text
1
NaN
```

Why:

- `Number(null)` is `0`.
- `Number(undefined)` is `NaN`.

Production trap:

- Missing values should be validated before arithmetic.

---

## 10. Q5: Empty Array Coercion

```js
console.log([] + []);
console.log([] + {});
```

Output:

```text

[object Object]
```

Why:

- `[]` converts to empty string.
- `{}` converts to `"[object Object]"` in this expression context.

Production trap:

- Object-to-primitive coercion is not business logic. Avoid it.

---

## 11. Q6: Object Plus Array

```js
console.log({} + []);
```

Output:

```text
[object Object]
```

Why:

- In this expression, `{}` is treated as an object literal.
- Object converts to `"[object Object]"`, array converts to `""`.

Caution:

- In some console or statement contexts, leading `{}` can be parsed as a block. Interviewers may discuss this parsing nuance.

---

## 12. Q7: Equality With Null

```js
console.log(null == undefined);
console.log(null === undefined);
```

Output:

```text
true
false
```

Why:

- `==` has a special rule: `null` and `undefined` are loosely equal only to each other.
- `===` requires same type.

Production note:

- `value == null` is sometimes intentionally used to check both `null` and `undefined`; otherwise prefer `===`.

---

## 13. Q8: Empty String Equality

```js
console.log("" == false);
console.log("" === false);
```

Output:

```text
true
false
```

Why:

- `==` converts both sides toward numbers: `""` becomes `0`, `false` becomes `0`.
- `===` does not coerce.

Production trap:

- Empty form values can be confused with false. Validate types explicitly.

---

## 14. Q9: Array Equality

```js
console.log([] == false);
console.log([] === false);
```

Output:

```text
true
false
```

Why:

- `[]` converts to `""`, then to `0`.
- `false` converts to `0`.
- Strict equality does not coerce.

Production trap:

- Arrays should be checked with `Array.isArray` and `.length`, not equality to boolean.

---

## 15. Q10: Array String Equality

```js
console.log([1, 2] == "1,2");
console.log([1, 2] === "1,2");
```

Output:

```text
true
false
```

Why:

- Array converts to string using `.toString()`, which joins elements with commas.
- Strict equality rejects different types.

Production trap:

- Do not compare arrays to strings. Serialize explicitly if needed.

---

## 16. Q11: NaN Equality

```js
console.log(NaN === NaN);
console.log(Object.is(NaN, NaN));
console.log(Number.isNaN(NaN));
```

Output:

```text
false
true
true
```

Why:

- `NaN` is not equal to itself.
- `Object.is` treats `NaN` as same.
- `Number.isNaN` checks actual `NaN` without broad coercion.

Production trap:

- Use `Number.isNaN(value)` for numeric validation.

---

## 17. Q12: Negative Zero

```js
console.log(0 === -0);
console.log(Object.is(0, -0));
```

Output:

```text
true
false
```

Why:

- Strict equality treats `0` and `-0` as equal.
- `Object.is` distinguishes them.

Production note:

- Rare, but can matter in math, charts, and low-level numeric logic.

---

## 18. Q13: Number Parsing

```js
console.log(parseInt("08"));
console.log(parseInt("08px"));
console.log(Number("08px"));
```

Output:

```text
8
8
NaN
```

Why:

- `parseInt` parses until invalid character.
- `Number` requires the whole string to be numeric.

Production trap:

- For strict numeric input, prefer `Number` plus validation. For parsing CSS-like values, `parseInt` may be intentional.

---

## 19. Q14: parseInt With Map

```js
console.log(["10", "10", "10"].map(parseInt));
```

Output:

```text
[10, NaN, 2]
```

Why:

- `map` passes `(value, index, array)`.
- `parseInt` expects `(string, radix)`.
- Calls become `parseInt("10", 0)`, `parseInt("10", 1)`, `parseInt("10", 2)`.

Safer:

```js
console.log(["10", "10", "10"].map(value => parseInt(value, 10)));
```

Production trap:

- Passing functions directly can be wrong when signatures do not match.

---

## 20. Q15: Truthy And Falsy

```js
console.log(Boolean("false"));
console.log(Boolean("0"));
console.log(Boolean([]));
console.log(Boolean({}));
```

Output:

```text
true
true
true
true
```

Why:

- Non-empty strings are truthy.
- Objects and arrays are truthy, even when empty.

Production trap:

- Query params like `?enabled=false` are strings. Parse booleans explicitly.

---

## 21. Hoisting And TDZ Questions

These questions test creation phase and declaration behavior.

---

## 22. Q16: var Hoisting

```js
console.log(count);
var count = 10;
console.log(count);
```

Output:

```text
undefined
10
```

Why:

- `var count` is hoisted and initialized to `undefined`.
- Assignment happens during execution.

Production trap:

- Prefer `let`/`const` and declare before use.

---

## 23. Q17: let TDZ

```js
console.log(count);
let count = 10;
```

Output:

```text
ReferenceError
```

Why:

- `let` is hoisted but uninitialized.
- Access before declaration is temporal dead zone.

Production note:

- TDZ prevents silent `undefined` bugs.

---

## 24. Q18: Function Declaration Hoisting

```js
sayHi();

function sayHi() {
  console.log("hi");
}
```

Output:

```text
hi
```

Why:

- Function declarations are hoisted with their body.

---

## 25. Q19: Function Expression With var

```js
sayHi();

var sayHi = function () {
  console.log("hi");
};
```

Output:

```text
TypeError
```

Why:

- `var sayHi` is hoisted as `undefined`.
- Calling `undefined()` throws TypeError.

---

## 26. Q20: Function Expression With const

```js
sayHi();

const sayHi = function () {
  console.log("hi");
};
```

Output:

```text
ReferenceError
```

Why:

- `const sayHi` is in TDZ before initialization.

---

## 27. Q21: var Function Scope

```js
function test() {
  if (true) {
    var value = 10;
  }

  console.log(value);
}

test();
```

Output:

```text
10
```

Why:

- `var` is function-scoped, not block-scoped.

---

## 28. Q22: let Block Scope

```js
function test() {
  if (true) {
    let value = 10;
  }

  console.log(value);
}

test();
```

Output:

```text
ReferenceError
```

Why:

- `let` is block-scoped to the `if` block.

---

## 29. Q23: Shadowing And TDZ

```js
let value = 10;

function test() {
  console.log(value);
  let value = 20;
}

test();
```

Output:

```text
ReferenceError
```

Why:

- The inner `let value` shadows the outer value for the entire function block.
- Access before inner initialization is TDZ.

Production trap:

- Shadowing can make code look like it reads outer state when it actually targets inner TDZ binding.

---

## 30. Q24: Class Declaration TDZ

```js
const user = new User();

class User {}
```

Output:

```text
ReferenceError
```

Why:

- Class declarations are hoisted but uninitialized until evaluated.
- They have TDZ behavior.

---

## 31. Scope And Closure Questions

These questions test lexical environment and captured variables.

---

## 32. Q25: Basic Closure

```js
function outer() {
  let count = 0;

  return function inner() {
    count += 1;
    console.log(count);
  };
}

const fn = outer();
fn();
fn();
```

Output:

```text
1
2
```

Why:

- `inner` closes over `count` from `outer`.
- The environment remains alive after `outer` returns.

Production use:

- Encapsulation, callbacks, memoization, once functions.

---

## 33. Q26: Closure With var Loop

```js
for (var i = 0; i < 3; i += 1) {
  setTimeout(() => console.log(i), 0);
}
```

Output:

```text
3
3
3
```

Why:

- `var` has one function/global-scoped `i` binding.
- Timers run after loop completes.
- Final `i` is `3`.

---

## 34. Q27: Closure With let Loop

```js
for (let i = 0; i < 3; i += 1) {
  setTimeout(() => console.log(i), 0);
}
```

Output:

```text
0
1
2
```

Why:

- `let` creates a new binding per iteration.
- Each callback captures its own `i`.

---

## 35. Q28: IIFE Fix

```js
for (var i = 0; i < 3; i += 1) {
  (function (value) {
    setTimeout(() => console.log(value), 0);
  })(i);
}
```

Output:

```text
0
1
2
```

Why:

- IIFE creates a new parameter binding `value` each iteration.

Modern note:

- Prefer `let` in modern JavaScript.

---

## 36. Q29: Closure Captures Binding, Not Snapshot

```js
let value = 1;

function print() {
  console.log(value);
}

value = 2;
print();
```

Output:

```text
2
```

Why:

- Closures capture variable bindings, not a frozen value snapshot.

---

## 37. Q30: Function Factory Snapshot

```js
function makePrinter(value) {
  return function print() {
    console.log(value);
  };
}

let current = 1;
const print = makePrinter(current);
current = 2;
print();
```

Output:

```text
1
```

Why:

- `current` value `1` is passed as argument.
- Parameter `value` is a new binding inside `makePrinter`.

---

## 38. Q31: Closure Memory Trap

```js
function createHandler(report) {
  return function handleClick() {
    console.log(report.id);
  };
}
```

Output:

```text
No output until handleClick is called
```

Why this is tricky:

- The handler retains `report` as long as the handler exists.
- If `report` is huge, this can retain more memory than needed.

Safer:

```js
function createHandler(report) {
  const reportId = report.id;

  return function handleClick() {
    console.log(reportId);
  };
}
```

Production trap:

- Closures can cause leaks when long-lived callbacks retain large objects.

---

## 39. this Binding Questions

These questions test call site rules.

---

## 40. Q32: Method Call

```js
const user = {
  name: "Asha",
  print() {
    console.log(this.name);
  }
};

user.print();
```

Output:

```text
Asha
```

Why:

- Called as `user.print()`, so `this` is `user`.

---

## 41. Q33: Lost Method Reference

```js
const user = {
  name: "Asha",
  print() {
    console.log(this.name);
  }
};

const fn = user.print;
fn();
```

Output:

```text
undefined
```

Why:

- The call site is `fn()`, not `user.print()`.
- In non-strict mode, `this` may be global object; in strict mode, `this` is `undefined` and may throw if reading property directly.

Production fix:

```js
const fn = user.print.bind(user);
```

---

## 42. Q34: Arrow Function As Method

```js
const user = {
  name: "Asha",
  print: () => {
    console.log(this.name);
  }
};

user.print();
```

Output:

```text
undefined
```

Why:

- Arrow functions do not bind `this` from call site.
- They capture `this` from surrounding lexical scope.

Production trap:

- Do not use arrow functions for object methods when you need dynamic receiver `this`.

---

## 43. Q35: Nested Function Loses this

```js
const user = {
  name: "Asha",
  print() {
    function inner() {
      console.log(this.name);
    }

    inner();
  }
};

user.print();
```

Output:

```text
undefined
```

Why:

- `print` has `this === user`.
- `inner()` is a plain function call and gets its own `this`.

Fix:

```js
const user = {
  name: "Asha",
  print() {
    const inner = () => {
      console.log(this.name);
    };

    inner();
  }
};
```

---

## 44. Q36: Arrow Captures this

```js
const user = {
  name: "Asha",
  print() {
    const inner = () => {
      console.log(this.name);
    };

    inner();
  }
};

user.print();
```

Output:

```text
Asha
```

Why:

- `inner` is arrow function and captures `this` from `print`.

---

## 45. Q37: bind vs call

```js
function printName() {
  console.log(this.name);
}

const user = { name: "Asha" };

printName.call(user);
const bound = printName.bind(user);
bound();
```

Output:

```text
Asha
Asha
```

Why:

- `call` invokes immediately with explicit `this`.
- `bind` returns a new function permanently bound to `user`.

---

## 46. Q38: Bound Function Cannot Be Rebound

```js
function printName() {
  console.log(this.name);
}

const first = { name: "First" };
const second = { name: "Second" };

const bound = printName.bind(first);
bound.call(second);
```

Output:

```text
First
```

Why:

- Once bound, `this` cannot be changed by `call` or `apply`.

---

## 47. Q39: Constructor this

```js
function User(name) {
  this.name = name;
}

const user = new User("Asha");
console.log(user.name);
```

Output:

```text
Asha
```

Why:

- `new` creates a new object and binds `this` to it.

---

## 48. Q40: Arrow Cannot Be Constructor

```js
const User = (name) => {
  this.name = name;
};

const user = new User("Asha");
```

Output:

```text
TypeError
```

Why:

- Arrow functions do not have `[[Construct]]` and cannot be used with `new`.

---

## 49. Objects And References Questions

These questions test mutation, identity, and key conversion.

---

## 50. Q41: Object Reference Assignment

```js
const first = { count: 1 };
const second = first;

second.count = 2;
console.log(first.count);
```

Output:

```text
2
```

Why:

- `first` and `second` reference the same object.

---

## 51. Q42: Reassignment Does Not Mutate Original

```js
let first = { count: 1 };
let second = first;

second = { count: 2 };
console.log(first.count);
```

Output:

```text
1
```

Why:

- Reassigning `second` points it to a new object.
- Original object is unchanged.

---

## 52. Q43: const Object Mutation

```js
const user = { name: "Asha" };
user.name = "Mira";
console.log(user.name);
```

Output:

```text
Mira
```

Why:

- `const` protects binding reassignment, not object contents.

---

## 53. Q44: Object Key Coercion

```js
const key1 = { id: 1 };
const key2 = { id: 2 };

const obj = {};
obj[key1] = "first";
obj[key2] = "second";

console.log(obj[key1]);
```

Output:

```text
second
```

Why:

- Plain object keys are strings or symbols.
- Both object keys convert to `"[object Object]"`.

Use Map:

```js
const map = new Map();
map.set(key1, "first");
map.set(key2, "second");
console.log(map.get(key1));
```

---

## 54. Q45: Object Property Order

```js
const obj = {
  b: 1,
  2: 2,
  a: 3,
  1: 4
};

console.log(Object.keys(obj));
```

Output:

```text
["1", "2", "b", "a"]
```

Why:

- Integer-like keys are ordered ascending first.
- String keys follow insertion order.

Production trap:

- Do not rely on object key order for domain ordering. Use arrays when order matters.

---

## 55. Q46: delete Property

```js
const user = { name: "Asha", role: "admin" };
delete user.role;
console.log(user.role);
```

Output:

```text
undefined
```

Why:

- `delete` removes an own property.

Production note:

- Prefer creating new objects for immutable state updates in frontend apps.

---

## 56. Q47: Optional Chaining Short Circuit

```js
const user = null;
console.log(user?.profile?.name);
```

Output:

```text
undefined
```

Why:

- Optional chaining stops when left side is `null` or `undefined`.

Production trap:

- Optional chaining prevents crashes but does not replace data contract validation.

---

## 57. Q48: Nullish Coalescing

```js
console.log(0 || 10);
console.log(0 ?? 10);
console.log("" || "default");
console.log("" ?? "default");
```

Output:

```text
10
0
default

```

Why:

- `||` falls back for any falsy value.
- `??` falls back only for `null` or `undefined`.
- Empty string remains empty with `??`, so the printed line is blank.

Production use:

- Use `??` for defaults where `0`, `false`, or `""` are valid values.

---

## 58. Arrays Questions

These questions test mutation, callbacks, holes, and sorting.

---

## 59. Q49: sort Without Comparator

```js
console.log([10, 2, 1].sort());
```

Output:

```text
[1, 10, 2]
```

Why:

- Default sort converts elements to strings and sorts lexicographically.

Fix:

```js
console.log([10, 2, 1].sort((a, b) => a - b));
```

---

## 60. Q50: sort Mutates

```js
const numbers = [3, 1, 2];
const sorted = numbers.sort((a, b) => a - b);

console.log(numbers);
console.log(sorted === numbers);
```

Output:

```text
[1, 2, 3]
true
```

Why:

- `sort` mutates the array and returns the same array reference.

Frontend trap:

- Do not mutate props/state arrays. Copy first: `[...numbers].sort(...)`.

---

## 61. Q51: map vs forEach

```js
const result = [1, 2, 3].forEach(value => value * 2);
console.log(result);
```

Output:

```text
undefined
```

Why:

- `forEach` returns `undefined`.
- Use `map` when you need a transformed array.

---

## 62. Q52: map With Missing Return

```js
const result = [1, 2, 3].map(value => {
  value * 2;
});

console.log(result);
```

Output:

```text
[undefined, undefined, undefined]
```

Why:

- Block-body arrow functions need explicit `return`.

Fix:

```js
const result = [1, 2, 3].map(value => value * 2);
```

---

## 63. Q53: reduce Without Initial Value

```js
console.log([].reduce((sum, value) => sum + value));
```

Output:

```text
TypeError
```

Why:

- Empty array with no initial accumulator has no first value.

Production fix:

```js
console.log([].reduce((sum, value) => sum + value, 0));
```

---

## 64. Q54: Sparse Array map

```js
const arr = new Array(3);
const result = arr.map(() => 1);
console.log(result);
console.log(result.length);
```

Output:

```text
[empty x 3]
3
```

Why:

- Sparse array has holes.
- `map` skips holes.

Fix:

```js
console.log(Array.from({ length: 3 }, () => 1));
```

---

## 65. Q55: Array fill Object Reference

```js
const rows = Array(3).fill({ count: 0 });
rows[0].count = 5;
console.log(rows);
```

Output:

```text
[{ count: 5 }, { count: 5 }, { count: 5 }]
```

Why:

- `fill` uses the same object reference for every slot.

Fix:

```js
const rows = Array.from({ length: 3 }, () => ({ count: 0 }));
```

---

## 66. Q56: includes vs indexOf With NaN

```js
const values = [NaN];
console.log(values.includes(NaN));
console.log(values.indexOf(NaN));
```

Output:

```text
true
-1
```

Why:

- `includes` uses SameValueZero comparison and can find `NaN`.
- `indexOf` uses strict equality-like comparison and cannot find `NaN`.

---

## 67. Q57: Destructuring Defaults

```js
const [a = 10, b = 20] = [undefined, null];
console.log(a, b);
```

Output:

```text
10 null
```

Why:

- Destructuring defaults apply only when value is `undefined`, not `null`.

---

## 68. Prototype Questions

These questions test lookup, inheritance, and own properties.

---

## 69. Q58: Prototype Lookup

```js
const parent = { role: "admin" };
const child = Object.create(parent);

console.log(child.role);
console.log(child.hasOwnProperty("role"));
```

Output:

```text
admin
false
```

Why:

- Property lookup checks object first, then prototype chain.
- `role` exists on prototype, not as own property.

---

## 70. Q59: Shadowing Prototype Property

```js
const parent = { role: "admin" };
const child = Object.create(parent);
child.role = "user";

console.log(child.role);
console.log(parent.role);
```

Output:

```text
user
admin
```

Why:

- Assignment creates own property on child.
- Parent remains unchanged.

---

## 71. Q60: in vs hasOwn

```js
const parent = { role: "admin" };
const child = Object.create(parent);
child.name = "Asha";

console.log("role" in child);
console.log(Object.hasOwn(child, "role"));
console.log(Object.hasOwn(child, "name"));
```

Output:

```text
true
false
true
```

Why:

- `in` checks prototype chain.
- `Object.hasOwn` checks only own properties.

---

## 72. Q61: Function Prototype

```js
function User() {}

const user = new User();
console.log(user.__proto__ === User.prototype);
```

Output:

```text
true
```

Why:

- `new User()` creates object whose prototype is `User.prototype`.

Production note:

- Prefer `Object.getPrototypeOf(user)` over `__proto__`.

---

## 73. Q62: Prototype Method Shared

```js
function Counter() {
  this.count = 0;
}

Counter.prototype.increment = function () {
  this.count += 1;
};

const first = new Counter();
const second = new Counter();

first.increment();
console.log(first.count, second.count);
```

Output:

```text
1 0
```

Why:

- Method is shared on prototype.
- `count` is own property per instance.

---

## 74. Class Questions

These questions test class syntax, methods, fields, and inheritance.

---

## 75. Q63: Class Method this Loss

```js
class User {
  constructor(name) {
    this.name = name;
  }

  print() {
    console.log(this.name);
  }
}

const user = new User("Asha");
const print = user.print;
print();
```

Output:

```text
TypeError
```

Why:

- Class bodies run in strict mode.
- Lost method reference called as plain function has `this === undefined`.

Fix:

```js
const print = user.print.bind(user);
```

---

## 76. Q64: Class Field Arrow Method

```js
class User {
  name = "Asha";

  print = () => {
    console.log(this.name);
  };
}

const user = new User();
const print = user.print;
print();
```

Output:

```text
Asha
```

Why:

- Class field arrow captures instance `this` during construction.

Trade-off:

- Creates a function per instance instead of sharing method on prototype.

---

## 77. Q65: super Before this

```js
class Parent {
  constructor() {
    this.name = "parent";
  }
}

class Child extends Parent {
  constructor() {
    this.age = 10;
    super();
  }
}

new Child();
```

Output:

```text
ReferenceError
```

Why:

- In derived classes, `super()` must be called before accessing `this`.

---

## 78. Q66: Static Method

```js
class User {
  static create() {
    return new User();
  }
}

const user = new User();
console.log(typeof User.create);
console.log(typeof user.create);
```

Output:

```text
function
undefined
```

Why:

- Static methods belong to the class constructor, not instances.

---

## 79. Async And Event Loop Questions

These questions test sync stack, microtasks, and macrotasks.

---

## 80. Q67: Promise Before Timeout

```js
console.log("A");

setTimeout(() => console.log("B"), 0);

Promise.resolve().then(() => console.log("C"));

console.log("D");
```

Output:

```text
A
D
C
B
```

Why:

- Sync code runs first.
- Promise callback is microtask.
- Timer callback is macrotask.
- Microtasks run before next macrotask.

---

## 81. Q68: Promise Chain

```js
Promise.resolve()
  .then(() => {
    console.log("A");
  })
  .then(() => {
    console.log("B");
  });

console.log("C");
```

Output:

```text
C
A
B
```

Why:

- `then` callbacks run after sync code.
- Second `then` waits for first `then` to complete.

---

## 82. Q69: async Function Start Is Sync

```js
async function run() {
  console.log("A");
  await null;
  console.log("B");
}

run();
console.log("C");
```

Output:

```text
A
C
B
```

Why:

- Async function starts synchronously until first `await`.
- Continuation after `await` runs as microtask.

---

## 83. Q70: await Promise Resolution

```js
async function run() {
  console.log("A");
  await Promise.resolve();
  console.log("B");
}

console.log("C");
run();
console.log("D");
```

Output:

```text
C
A
D
B
```

Why:

- `run()` executes until `await`.
- Sync stack finishes.
- Await continuation runs later.

---

## 84. Q71: Promise Constructor Is Sync

```js
console.log("A");

new Promise(resolve => {
  console.log("B");
  resolve();
}).then(() => console.log("C"));

console.log("D");
```

Output:

```text
A
B
D
C
```

Why:

- Promise executor runs synchronously.
- `.then` callback runs as microtask.

---

## 85. Q72: Microtask Inside Timer

```js
setTimeout(() => {
  console.log("A");
  Promise.resolve().then(() => console.log("B"));
}, 0);

setTimeout(() => {
  console.log("C");
}, 0);
```

Typical output:

```text
A
B
C
```

Why:

- First timer callback runs.
- Microtasks created inside that callback run before the next timer callback.

---

## 86. Q73: return In then

```js
Promise.resolve(1)
  .then(value => value + 1)
  .then(value => console.log(value));
```

Output:

```text
2
```

Why:

- Return value from `then` becomes resolved value for next `then`.

---

## 87. Q74: Missing return In then

```js
Promise.resolve(1)
  .then(value => {
    value + 1;
  })
  .then(value => console.log(value));
```

Output:

```text
undefined
```

Why:

- Block-body arrow has no explicit return.
- Next `then` receives `undefined`.

---

## 88. Q75: catch Recovery

```js
Promise.reject("bad")
  .catch(error => {
    console.log(error);
    return "recovered";
  })
  .then(value => console.log(value));
```

Output:

```text
bad
recovered
```

Why:

- `catch` handles rejection and returns a fulfilled value.

---

## 89. Q76: finally Value

```js
Promise.resolve("ok")
  .finally(() => "ignored")
  .then(value => console.log(value));
```

Output:

```text
ok
```

Why:

- `finally` return value is ignored unless it throws or returns a rejected promise.

---

## 90. Q77: Promise.all Fail Fast

```js
Promise.all([
  Promise.resolve("A"),
  Promise.reject("B"),
  Promise.resolve("C")
])
  .then(console.log)
  .catch(console.log);
```

Output:

```text
B
```

Why:

- `Promise.all` rejects when any promise rejects.

Production note:

- Use `Promise.allSettled` when partial success is acceptable.

---

## 91. Q78: Promise.allSettled

```js
Promise.allSettled([
  Promise.resolve("A"),
  Promise.reject("B")
]).then(console.log);
```

Output:

```text
[
  { status: "fulfilled", value: "A" },
  { status: "rejected", reason: "B" }
]
```

Why:

- `allSettled` waits for every promise and records each outcome.

---

## 92. Q79: forEach With async

```js
async function run() {
  [1, 2, 3].forEach(async value => {
    await Promise.resolve();
    console.log(value);
  });

  console.log("done");
}

run();
```

Output:

```text
done
1
2
3
```

Why:

- `forEach` does not await async callbacks.
- `done` logs before microtasks complete.

Fix:

```js
for (const value of [1, 2, 3]) {
  await Promise.resolve();
  console.log(value);
}
```

---

## 93. Q80: Promise.all Starts Work Immediately

```js
function task(id) {
  console.log("start", id);
  return Promise.resolve().then(() => console.log("end", id));
}

Promise.all([task(1), task(2)]).then(() => console.log("done"));
```

Output:

```text
start 1
start 2
end 1
end 2
done
```

Why:

- `task(1)` and `task(2)` are called synchronously before `Promise.all` receives promises.

Production trap:

- `Promise.all(items.map(task))` starts all tasks immediately. Use concurrency limits for large batches.

---

## 94. Modules And Strict Mode Questions

These questions test modern JavaScript runtime differences.

---

## 95. Q81: Module this

```js
console.log(this);
```

In an ES module, output:

```text
undefined
```

Why:

- Top-level `this` in ES modules is `undefined`.
- Modules are strict by default.

In non-module browser scripts, top-level `this` is often `window`.

---

## 96. Q82: Import Binding Is Live

```js
// counter.js
export let count = 0;
export function increment() {
  count += 1;
}

// app.js
import { count, increment } from "./counter.js";

console.log(count);
increment();
console.log(count);
```

Output:

```text
0
1
```

Why:

- ES module imports are live bindings, not copied values.

---

## 97. Q83: Cannot Reassign Import

```js
import { count } from "./counter.js";

count = 10;
```

Output:

```text
TypeError
```

Why:

- Imported bindings are read-only from the importing module.

---

## 98. Mixed Topic Questions

These combine multiple rules.

---

## 99. Q84: Object Method And Timeout

```js
const user = {
  name: "Asha",
  print() {
    setTimeout(function () {
      console.log(this.name);
    }, 0);
  }
};

user.print();
```

Output:

```text
undefined
```

Why:

- `print` is called with `this === user`.
- Timer callback is a normal function called by timer machinery, not as `user.method`.

Fix:

```js
setTimeout(() => {
  console.log(this.name);
}, 0);
```

---

## 100. Q85: Object Method And Arrow Timeout

```js
const user = {
  name: "Asha",
  print() {
    setTimeout(() => {
      console.log(this.name);
    }, 0);
  }
};

user.print();
```

Output:

```text
Asha
```

Why:

- Arrow callback captures `this` from `print`.

---

## 101. Q86: Chained Assignment

```js
let a = { value: 1 };
let b = a;

a.value = a = { value: 2 };

console.log(a.value);
console.log(b.value);
```

Output:

```text
2
{ value: 2 }
```

Why:

- Property reference `a.value` is resolved against the old object before assignment expression completes.
- `a` is reassigned to new object.
- Old object property `value` receives the new object.

This is intentionally confusing.

Production rule:

- Never write chained assignments like this.

---

## 102. Q87: Increment Operators

```js
let count = 1;
console.log(count++);
console.log(++count);
console.log(count);
```

Output:

```text
1
3
3
```

Why:

- Post-increment returns old value, then increments.
- Pre-increment increments first, then returns new value.

---

## 103. Q88: Default Parameter Scope

```js
let value = 10;

function test(value = value) {
  console.log(value);
}

test();
```

Output:

```text
ReferenceError
```

Why:

- Parameter `value` shadows outer `value`.
- Default initializer tries to read parameter before initialized.

---

## 104. Q89: Default Parameter Uses Previous Parameter

```js
function test(a, b = a) {
  console.log(a, b);
}

test(5);
```

Output:

```text
5 5
```

Why:

- Earlier parameters are available to later default initializers.

---

## 105. Q90: Rest Parameter

```js
function test(first, ...rest) {
  console.log(first);
  console.log(rest);
}

test(1, 2, 3);
```

Output:

```text
1
[2, 3]
```

Why:

- Rest parameter collects remaining arguments into a real array.

---

## 106. Q91: arguments With Arrow

```js
function outer() {
  const arrow = () => console.log(arguments[0]);
  arrow("inner");
}

outer("outer");
```

Output:

```text
outer
```

Why:

- Arrow functions do not have their own `arguments`.
- They capture `arguments` from the nearest non-arrow function.

---

## 107. Q92: Destructuring Renaming

```js
const user = { name: "Asha" };
const { name: displayName } = user;

console.log(displayName);
console.log(name);
```

Output:

```text
Asha
ReferenceError
```

Why:

- `name: displayName` creates variable `displayName`, not `name`.

---

## 108. Q93: Spread Shallow Copy

```js
const first = { nested: { count: 1 } };
const second = { ...first };

second.nested.count = 2;
console.log(first.nested.count);
```

Output:

```text
2
```

Why:

- Object spread is shallow.
- `nested` object reference is shared.

Production trap:

- Deep state updates require copying every changed level or using a safe helper.

---

## 109. Q94: JSON Drops Values

```js
const value = {
  a: undefined,
  b: function () {},
  c: Symbol("c"),
  d: null
};

console.log(JSON.stringify(value));
```

Output:

```text
{"d":null}
```

Why:

- `undefined`, functions, and symbols are omitted from objects during JSON serialization.
- `null` is preserved.

---

## 110. Q95: JSON Array Drops Differently

```js
console.log(JSON.stringify([undefined, function () {}, Symbol("x"), null]));
```

Output:

```text
[null,null,null,null]
```

Why:

- In arrays, unsupported JSON values become `null`.

---

## 111. Q96: BigInt JSON

```js
console.log(JSON.stringify({ id: 10n }));
```

Output:

```text
TypeError
```

Why:

- JSON does not support BigInt by default.

Fix:

```js
console.log(JSON.stringify({ id: "10" }));
```

Production trap:

- Serialize BigInt IDs as strings at API boundaries.

---

## 112. Q97: Date To JSON

```js
const date = new Date("2026-06-20T00:00:00.000Z");
console.log(JSON.stringify({ date }));
```

Output:

```text
{"date":"2026-06-20T00:00:00.000Z"}
```

Why:

- `Date` implements `toJSON`, returning ISO string.

Production trap:

- Distinguish instant timestamps from local calendar dates.

---

## 113. Q98: Set Uniqueness

```js
const set = new Set([1, 1, "1", {}, {}]);
console.log(set.size);
```

Output:

```text
4
```

Why:

- Duplicate `1` collapses.
- Number `1` and string `"1"` are different.
- Each object literal is a different reference.

---

## 114. Q99: Map Key Identity

```js
const first = { id: 1 };
const second = { id: 1 };

const map = new Map();
map.set(first, "first");
map.set(second, "second");

console.log(map.size);
console.log(map.get(first));
```

Output:

```text
2
first
```

Why:

- Map uses object identity for object keys.

---

## 115. Q100: WeakMap Key Rule

```js
const weakMap = new WeakMap();
weakMap.set("id", 123);
```

Output:

```text
TypeError
```

Why:

- WeakMap keys must be objects or non-registered symbols, not primitive strings.

Production use:

- WeakMap is useful for object-associated metadata without preventing garbage collection.

---

## 116. DOM And Browser Output Traps

These questions connect JavaScript rules to browser behavior.

---

## 117. Q101: Event Listener this

```js
button.addEventListener("click", function () {
  console.log(this === button);
});
```

Output after click:

```text
true
```

Why:

- For normal function event listeners, browser sets `this` to current target for many DOM event APIs.

---

## 118. Q102: Arrow Event Listener this

```js
button.addEventListener("click", () => {
  console.log(this === button);
});
```

Output after click:

```text
false
```

Why:

- Arrow function captures lexical `this`; it does not get DOM listener `this`.

Better:

```js
button.addEventListener("click", event => {
  console.log(event.currentTarget === button);
});
```

---

## 119. Q103: Dataset Values Are Strings

```html
<button data-count="5">Click</button>
```

```js
console.log(button.dataset.count + 1);
console.log(Number(button.dataset.count) + 1);
```

Output:

```text
51
6
```

Why:

- Dataset values are strings.

Production trap:

- Convert DOM attributes explicitly.

---

## 120. Q104: localStorage Values Are Strings

```js
localStorage.setItem("enabled", false);
console.log(localStorage.getItem("enabled"));
console.log(Boolean(localStorage.getItem("enabled")));
```

Output:

```text
false
true
```

Why:

- localStorage stores strings.
- String `"false"` is truthy.

Fix:

```js
const enabled = localStorage.getItem("enabled") === "true";
```

---

## 121. Node.js Output Traps

These questions connect JavaScript rules to Node runtime behavior.

---

## 122. Q105: process.nextTick vs Promise

```js
Promise.resolve().then(() => console.log("promise"));
process.nextTick(() => console.log("nextTick"));
console.log("sync");
```

Typical Node output:

```text
sync
nextTick
promise
```

Why:

- Node processes `nextTick` queue before promise microtasks.

Caution:

- Overusing `process.nextTick` can starve the event loop.

---

## 123. Q106: setImmediate vs setTimeout

```js
setTimeout(() => console.log("timeout"), 0);
setImmediate(() => console.log("immediate"));
```

Output:

```text
Order can vary in top-level code
```

Why:

- At top level, timing depends on event loop scheduling.
- Inside I/O callbacks, `setImmediate` usually runs before `setTimeout(0)`.

Interview note:

- Say the nuance. Do not overclaim a fixed order for top-level Node code.

---

## 124. Q107: Buffer Reference Slice

```js
const buffer = Buffer.from([1, 2, 3]);
const slice = buffer.slice(0, 1);
slice[0] = 9;
console.log(buffer[0]);
```

Output:

```text
9
```

Why:

- `Buffer.prototype.slice` returns a view over the same memory, not a deep copy.

Production trap:

- Be careful when retaining slices of large Buffers.

---

## 125. Production Trick Questions

These are the ones interviewers use to see whether you can connect output to maintainability.

---

## 126. Q108: Mutation In Function

```js
function update(user) {
  user.name = "Mira";
}

const user = { name: "Asha" };
update(user);
console.log(user.name);
```

Output:

```text
Mira
```

Why:

- Object reference is passed by value; the function receives a copy of the reference and mutates the object.

---

## 127. Q109: Reassignment In Function

```js
function update(user) {
  user = { name: "Mira" };
}

const user = { name: "Asha" };
update(user);
console.log(user.name);
```

Output:

```text
Asha
```

Why:

- Reassigning the local parameter does not change the caller's binding.

---

## 128. Q110: Object.freeze Is Shallow

```js
const user = Object.freeze({
  name: "Asha",
  profile: { city: "NYC" }
});

user.profile.city = "LA";
console.log(user.profile.city);
```

Output:

```text
LA
```

Why:

- `Object.freeze` is shallow.
- Nested object remains mutable unless frozen too.

---

## 129. Q111: Assignment In Condition

```js
let isAdmin = false;

if (isAdmin = true) {
  console.log("allowed");
}
```

Output:

```text
allowed
```

Why:

- `=` assigns and returns assigned value.
- Condition becomes `true`.

Production guard:

- Linters should catch assignment in conditions.

---

## 130. Q112: Floating Point

```js
console.log(0.1 + 0.2 === 0.3);
console.log(0.1 + 0.2);
```

Output:

```text
false
0.30000000000000004
```

Why:

- JavaScript numbers use binary floating point.

Production rule:

- Use integer cents or decimal library for money.

---

## 131. Q113: Date Parsing

```js
console.log(new Date("2026-06-20").toISOString());
```

Output:

```text
2026-06-20T00:00:00.000Z
```

Why:

- Date-only ISO string is parsed as UTC date in modern JavaScript.

Production trap:

- A booking date may be a local calendar date, not a UTC instant. Model it intentionally.

---

## 132. Q114: typeof null

```js
console.log(typeof null);
console.log(null instanceof Object);
```

Output:

```text
object
false
```

Why:

- `typeof null` is a historic JavaScript bug.
- `null` is not an object instance.

Production rule:

- Check `value === null` explicitly.

---

## 133. Q115: typeof Function

```js
console.log(typeof function () {});
console.log(typeof class User {});
```

Output:

```text
function
function
```

Why:

- Classes are special functions under the hood, though they cannot be called without `new`.

---

## 134. Q116: Class Called Without new

```js
class User {}
User();
```

Output:

```text
TypeError
```

Why:

- Class constructors must be called with `new`.

---

## 135. Q117: Function Constructor Return Object

```js
function User() {
  this.name = "Asha";
  return { name: "Mira" };
}

const user = new User();
console.log(user.name);
```

Output:

```text
Mira
```

Why:

- Constructor returning an object replaces the newly created instance.

---

## 136. Q118: Function Constructor Return Primitive

```js
function User() {
  this.name = "Asha";
  return "Mira";
}

const user = new User();
console.log(user.name);
```

Output:

```text
Asha
```

Why:

- Constructor returning a primitive is ignored.

---

## 137. Q119: Instance Method From Prototype

```js
function User(name) {
  this.name = name;
}

User.prototype.name = "Prototype";

const user = new User("Instance");
console.log(user.name);
delete user.name;
console.log(user.name);
```

Output:

```text
Instance
Prototype
```

Why:

- Own property wins over prototype property.
- After deleting own property, lookup finds prototype property.

---

## 138. Q120: Object.create null

```js
const obj = Object.create(null);
console.log(obj.toString);
console.log("toString" in obj);
```

Output:

```text
undefined
false
```

Why:

- Object has no prototype.
- It does not inherit `Object.prototype` methods.

Use case:

- Dictionary-like objects without prototype key collisions, though `Map` is often clearer.

---

## 139. Output Drill Answer Key By Topic

| Topic | Must Say |
|---|---|
| Coercion | Which operation coerces and to what type. |
| Equality | Whether `==`, `===`, `Object.is`, or SameValueZero is used. |
| Hoisting | Creation phase vs execution phase. |
| TDZ | Binding exists but cannot be accessed before initialization. |
| Closure | Captures binding/environment, not always a snapshot. |
| `this` | Determined by call site unless arrow/bind/new applies. |
| Prototype | Own property first, then prototype chain. |
| Array | Know mutation methods and callback signatures. |
| Async | Sync stack, microtasks, macrotasks. |
| Module | Strict mode, live bindings, top-level `this`. |
| Browser | DOM/storage values are often strings; lifecycle matters. |
| Node | Event loop details and Buffer memory can differ from browser assumptions. |

---

## 140. Common Interview Follow-Up Questions

- How would you avoid this bug in production?
- Would a linter catch this?
- What changes in strict mode?
- What changes in ES modules?
- What changes if this function becomes an arrow function?
- What changes if `var` becomes `let`?
- What changes if this is in Node vs browser?
- Is this mutation or reassignment?
- Is this object value copied or referenced?
- Which queue does this callback go into?

---

## 141. Strong Answer Examples

### Hoisting Answer

```text
The output is undefined, then 10. var declarations are hoisted and initialized to undefined during the creation phase, but the assignment runs later during execution. I would avoid this by using let or const and declaring before use.
```

### Async Answer

```text
The output is A, D, C, B. Synchronous logs run first. Promise callbacks are microtasks, so they run before timer macrotasks. setTimeout runs after the microtask queue drains.
```

### this Answer

```text
The method loses this because it is assigned to a variable and called as a plain function. this is determined by call site, not where the function was defined. bind or an arrow wrapper would make the receiver explicit.
```

### Coercion Answer

```text
The result is true because loose equality coerces both sides. The empty string becomes 0 and false becomes 0. In production I would avoid this by using strict equality and explicit parsing.
```

---

## 142. Production Rules To Avoid Tricky Bugs

- Prefer `const`, then `let`; avoid `var`.
- Prefer `===`; use `== null` only intentionally.
- Parse user input explicitly.
- Do not rely on object-to-primitive coercion.
- Avoid mutation of shared objects and arrays.
- Copy arrays before `sort` in frontend state.
- Always provide initial value to `reduce` for possibly empty arrays.
- Avoid passing functions directly when callback signature differs.
- Bind class methods or use stable arrow handlers intentionally.
- Abort stale browser requests.
- Do not use `forEach` when you need `await` sequencing.
- Use concurrency limits for large async batches.
- Use `Number.isNaN`, not `value === NaN`.
- Serialize BigInt as string at API boundaries.
- Validate date/time behavior around timezone and DST.
- Use `Map` when keys are objects.
- Use `Object.hasOwn` when own property matters.
- Upload source maps so production stack traces are readable.

---

## 143. Rapid Revision

- `+` can concatenate; `-` coerces to number.
- `null` becomes `0` in numeric coercion; `undefined` becomes `NaN`.
- `NaN !== NaN`; use `Number.isNaN`.
- `Object.is(0, -0)` is false.
- `var` hoists to `undefined`; `let`/`const` have TDZ.
- Function declarations hoist; function expressions follow variable rules.
- Closures capture bindings.
- `var` loop callbacks share one binding; `let` loop creates per-iteration binding.
- `this` depends on call site for normal functions.
- Arrow functions capture lexical `this`.
- `bind` creates a permanently bound function.
- Object spread is shallow.
- Plain object keys coerce to strings; Map keys preserve identity.
- `sort` mutates and sorts strings by default.
- Sparse arrays skip holes in many array methods.
- Promise callbacks are microtasks.
- Promise executor runs synchronously.
- Async functions run synchronously until first `await`.
- `forEach` does not await async callbacks.
- ES module imports are live bindings.
- localStorage and dataset values are strings.
- Buffer slice can share memory.

---

## 144. Final Mental Model

Tricky output questions are not random.

They come from a small set of rules:

1. Type conversion.
2. Declaration creation.
3. Scope and closure.
4. Call-site `this`.
5. Object identity.
6. Prototype lookup.
7. Mutation vs reassignment.
8. Task scheduling.
9. Runtime differences.

If you identify the rule first, the output becomes predictable.

That is the interview skill: not remembering the answer, but proving it.
