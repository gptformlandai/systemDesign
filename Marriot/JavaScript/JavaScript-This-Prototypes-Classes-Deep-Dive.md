# JavaScript This, Prototypes, And Classes Deep Dive

Target: JavaScript interviews where `this`, prototype chain, constructor functions, classes, inheritance, and object behavior are tested through tricky output questions and production scenarios.

This sheet covers:
- `this` mental model
- `this` binding rules and precedence
- Default, implicit, explicit, `new`, and arrow-function binding
- `call`, `apply`, and `bind`
- Lost `this` bugs in callbacks
- Constructor functions and the `new` operator
- Prototype chain and property lookup
- Own vs inherited properties
- `Object.create`, `Object.getPrototypeOf`, and `Object.setPrototypeOf`
- Function `prototype` vs object `[[Prototype]]`
- Classes as syntax over prototypes
- `extends`, `super`, private fields, static fields/methods
- Property descriptors, getters, setters
- `instanceof` and prototype-based checks
- Production design judgment: composition, mutation, monkey patching, and prototype pollution awareness

How to use this:
- First learn the `this` binding rules.
- Then learn the prototype lookup rules.
- Then understand classes as cleaner syntax over prototype-based behavior.
- For output questions, identify how the function is called, not where it is written.

---

## 1. Mental Model

JavaScript has two ideas that confuse many learners:

```text
this       -> determined mostly by how a function is called
prototype  -> determines where object property/method lookup continues
```

Simple split:

```text
this answers: who is the receiver of this function call?
prototype answers: where should JavaScript look if a property is not found directly?
```

Important:

```text
this is not the same as lexical scope.
A function can close over variables lexically, but normal function this is decided at call time.
```

Example:

```javascript
const user = {
    name: "Ava",
    sayHi() {
        console.log(this.name);
    }
};

user.sayHi(); // Ava
```

Why:

```text
sayHi is called as a method of user, so this is user.
```

Strong interview line:

```text
In JavaScript, this usually depends on the call site, not where the function is defined. The
prototype chain is the lookup path JavaScript follows when a property is missing on the object
itself. Classes are mostly cleaner syntax over prototypes.
```

---

## 2. Interview Priority Meter

| Topic | Priority | Why It Matters |
|---|---:|---|
| `this` call-site rule | Very high | Most `this` questions depend on call style |
| Implicit binding | Very high | Object method calls |
| Lost `this` | Very high | Callback and event-handler bugs |
| Arrow function `this` | Very high | Lexical `this` trap |
| `call`, `apply`, `bind` | Very high | Explicit binding and interview utilities |
| `new` binding | Very high | Constructor behavior |
| Binding precedence | Very high | Tricky output questions |
| Prototype chain | Very high | Object model foundation |
| Function `prototype` | Very high | Constructor/class internals |
| Own vs inherited property | High | Debugging and security awareness |
| `Object.create` | High | Direct prototype creation |
| Classes | Very high | Modern JavaScript code |
| `extends` and `super` | High | Inheritance mechanics |
| Private fields | Medium-high | Modern encapsulation |
| Static methods | Medium-high | Class-level behavior |
| Property descriptors | Medium-high | Senior object mechanics |
| `instanceof` | Medium-high | Prototype-chain checks |
| Monkey patching risk | Medium | Production maturity |
| Prototype pollution awareness | High | Security maturity |

---

## 3. `this` In One Sentence

`this` is a value provided to a function when it is called.

For normal functions, `this` is usually decided by the call site.

```javascript
function showName() {
    console.log(this.name);
}

const user = { name: "Ava", showName };

user.showName(); // Ava
```

Same function, different call:

```javascript
const fn = user.showName;
fn(); // undefined in strict mode, or global lookup in sloppy browser script
```

Why:

```text
user.showName() has user as receiver.
fn() is a plain function call and loses the receiver.
```

Interview answer:

```text
For normal functions, I determine this by looking at how the function is called. If it is
called as obj.method(), this is obj. If the method is extracted and called as a plain function,
the original object is no longer the receiver.
```

---

## 4. The Five `this` Binding Rules

Use this order for most questions:

| Rule | Call Shape | `this` Value |
|---|---|---|
| Default binding | `fn()` | `undefined` in strict mode, global object in sloppy mode |
| Implicit binding | `obj.fn()` | `obj` |
| Explicit binding | `fn.call(obj)`, `fn.apply(obj)`, bound function | Provided object |
| `new` binding | `new Fn()` | Newly created object |
| Arrow lexical binding | `() => this` | `this` from surrounding lexical scope |

Precedence, simplified:

```text
new binding
    > explicit bind/call/apply for normal calls
    > implicit object call
    > default binding

Arrow function this ignores call/apply/bind for changing this.
```

Strong line:

```text
For `this` questions, I first ask: is it an arrow function? If yes, it uses lexical this. If
not, I inspect the call site and apply new, explicit, implicit, or default binding.
```

---

## 5. Default Binding

Default binding happens when a normal function is called directly.

```javascript
function show() {
    console.log(this);
}

show();
```

Strict mode:

```javascript
"use strict";

function show() {
    console.log(this);
}

show(); // undefined
```

Sloppy browser script:

```text
this may be the global object, usually window.
```

Production guidance:

```text
Modern modules and strict mode avoid many accidental global-this bugs.
```

Trap:

```javascript
const user = {
    name: "Ava",
    show() {
        function inner() {
            console.log(this.name);
        }
        inner();
    }
};

user.show(); // TypeError or undefined behavior depending on strict/sloppy mode
```

Why:

```text
inner() is a plain function call. It does not automatically inherit this from show.
```

Fix:

```javascript
const user = {
    name: "Ava",
    show() {
        const inner = () => {
            console.log(this.name);
        };
        inner();
    }
};

user.show(); // Ava
```

---

## 6. Implicit Binding

Implicit binding happens when a function is called as an object method.

```javascript
const user = {
    name: "Ava",
    greet() {
        console.log(this.name);
    }
};

user.greet(); // Ava
```

Nested object trap:

```javascript
const user = {
    name: "Ava",
    profile: {
        name: "Admin",
        greet() {
            console.log(this.name);
        }
    }
};

user.profile.greet(); // Admin
```

Rule:

```text
this is the object immediately to the left of the call dot.
```

For `user.profile.greet()`, the receiver is `user.profile`, not `user`.

Interview line:

```text
In an implicit method call, this is the receiver object at the call site, usually the object
immediately before the final dot.
```

---

## 7. Lost `this`

A method can lose its receiver when assigned or passed as a callback.

```javascript
const user = {
    name: "Ava",
    greet() {
        console.log(this.name);
    }
};

const greet = user.greet;
greet(); // undefined in strict mode
```

Why:

```text
The function value was copied. The original receiver was not copied with it.
```

Callback example:

```javascript
const user = {
    name: "Ava",
    greet() {
        console.log(this.name);
    }
};

setTimeout(user.greet, 0); // loses this
```

Fix with wrapper:

```javascript
setTimeout(() => user.greet(), 0);
```

Fix with bind:

```javascript
setTimeout(user.greet.bind(user), 0);
```

Strong answer:

```text
Methods do not permanently carry their object as this. The receiver is determined at call time.
Passing obj.method as a callback passes only the function, so this is lost unless we bind it or
call it through a wrapper.
```

---

## 8. Explicit Binding: `call`, `apply`, `bind`

`call` invokes immediately with arguments listed one by one.

```javascript
function greet(prefix) {
    return `${prefix}, ${this.name}`;
}

const user = { name: "Ava" };

console.log(greet.call(user, "Hello")); // Hello, Ava
```

`apply` invokes immediately with arguments as an array.

```javascript
console.log(greet.apply(user, ["Hi"])); // Hi, Ava
```

`bind` returns a new function with fixed `this`.

```javascript
const boundGreet = greet.bind(user, "Welcome");
console.log(boundGreet()); // Welcome, Ava
```

Difference:

| Method | Invokes Now? | Args Shape | Returns |
|---|---:|---|---|
| `call` | Yes | comma-separated | result |
| `apply` | Yes | array-like | result |
| `bind` | No | comma-separated preset | new function |

Interview line:

```text
call and apply immediately invoke a function with an explicit this. bind creates a new function
with this permanently bound for normal calls.
```

---

## 9. `bind` Details And Traps

`bind` creates a new function.

```javascript
function show() {
    return this.name;
}

const user = { name: "Ava" };
const bound = show.bind(user);

console.log(bound()); // Ava
console.log(bound === show); // false
```

Binding is hard to override with call/apply:

```javascript
const user1 = { name: "Ava" };
const user2 = { name: "Mia" };

function show() {
    console.log(this.name);
}

const bound = show.bind(user1);
bound.call(user2); // Ava
```

Why:

```text
A bound function keeps its bound this for normal invocation.
```

Common event-listener trap:

```javascript
element.addEventListener("click", user.greet.bind(user));
element.removeEventListener("click", user.greet.bind(user)); // does not remove original
```

Why:

```text
Each bind call returns a new function reference.
```

Better:

```javascript
const boundGreet = user.greet.bind(user);
element.addEventListener("click", boundGreet);
element.removeEventListener("click", boundGreet);
```

Production line:

```text
When registering callbacks, keep a reference to the exact function you need to remove later.
```

---

## 10. Arrow Function `this`

Arrow functions do not have their own `this`.

They capture `this` from the surrounding lexical scope.

```javascript
const user = {
    name: "Ava",
    greet() {
        const inner = () => {
            console.log(this.name);
        };
        inner();
    }
};

user.greet(); // Ava
```

Why:

```text
inner is an arrow function, so it uses this from greet. greet was called with user as this.
```

Trap: arrow as object method

```javascript
const user = {
    name: "Ava",
    greet: () => {
        console.log(this.name);
    }
};

user.greet(); // usually undefined
```

Why:

```text
The arrow function does not bind this to user. It uses this from the outer scope where the
object literal was created.
```

Interview line:

```text
Arrow functions are useful when I want lexical this, especially inside callbacks. I avoid them
as object methods when the method needs this to be the object.
```

---

## 11. `new` Binding

Calling a function with `new` creates a new object and binds `this` to it.

```javascript
function User(name) {
    this.name = name;
}

const user = new User("Ava");
console.log(user.name); // Ava
```

What `new` roughly does:

```text
1. Creates a new empty object.
2. Links the new object's [[Prototype]] to User.prototype.
3. Calls User with this set to the new object.
4. Returns the new object unless constructor returns an object explicitly.
```

Manual mental model:

```javascript
function createWithNew(Constructor, ...args) {
    const instance = Object.create(Constructor.prototype);
    const result = Constructor.apply(instance, args);
    return result !== null && (typeof result === "object" || typeof result === "function")
        ? result
        : instance;
}
```

Constructor return trap:

```javascript
function User(name) {
    this.name = name;
    return { name: "Override" };
}

console.log(new User("Ava").name); // Override
```

If constructor returns primitive, it is ignored:

```javascript
function User(name) {
    this.name = name;
    return 10;
}

console.log(new User("Ava").name); // Ava
```

---

## 12. Binding Precedence

Precedence for normal functions:

```text
new binding
    > explicit bind/call/apply
    > implicit binding
    > default binding
```

Example: implicit vs explicit

```javascript
function show() {
    console.log(this.name);
}

const user1 = { name: "Ava", show };
const user2 = { name: "Mia" };

user1.show.call(user2); // Mia
```

Explicit wins over implicit.

Bound function vs call:

```javascript
const bound = show.bind(user1);
bound.call(user2); // Ava
```

Bound this wins for normal calls.

Arrow exception:

```javascript
const arrow = () => console.log(this.name);
arrow.call({ name: "Ava" }); // call cannot change arrow this
```

Strong answer:

```text
For normal functions, new has the highest priority, then explicit binding, then implicit
binding, then default binding. Arrow functions are different because their this is lexical and
cannot be changed with call, apply, or bind.
```

---

## 13. `this` In Classes

Class methods are normal prototype methods.

```javascript
class User {
    constructor(name) {
        this.name = name;
    }

    greet() {
        console.log(this.name);
    }
}

const user = new User("Ava");
user.greet(); // Ava
```

Lost method still happens:

```javascript
const greet = user.greet;
greet(); // TypeError in strict mode
```

Why:

```text
Class methods are not automatically bound.
```

Common fix:

```javascript
class User {
    constructor(name) {
        this.name = name;
        this.greet = this.greet.bind(this);
    }

    greet() {
        console.log(this.name);
    }
}
```

Class field arrow method:

```javascript
class User {
    name = "Ava";

    greet = () => {
        console.log(this.name);
    };
}
```

Trade-off:

```text
Prototype methods are shared. Arrow field methods are created per instance.
```

Interview line:

```text
Class methods still follow normal this rules. They are not auto-bound. If a class method is
passed as a callback, bind it, wrap it, or use an instance field arrow method when appropriate.
```

---

## 14. `this` In Event Handlers

Browser event listener behavior:

```javascript
button.addEventListener("click", function () {
    console.log(this === button); // true
});
```

For a normal function listener, browsers commonly set `this` to the event target/current target.

Arrow listener:

```javascript
button.addEventListener("click", () => {
    console.log(this); // lexical this, not the button
});
```

Better explicit style:

```javascript
button.addEventListener("click", event => {
    console.log(event.currentTarget);
});
```

Production line:

```text
In browser event handlers, I prefer event.currentTarget for clarity instead of relying on this.
```

---

## 15. Prototype Mental Model

Every ordinary object has an internal prototype link, often called `[[Prototype]]`.

If a property is not found directly on the object, JavaScript looks up the prototype chain.

```javascript
const user = { name: "Ava" };

console.log(user.toString); // found through prototype chain
```

Simplified lookup:

```text
user
    -> user.[[Prototype]]
        -> Object.prototype
            -> null
```

Strong answer:

```text
The prototype chain is JavaScript's delegation mechanism. If an object does not have a property
itself, the engine looks at its prototype, then that prototype's prototype, until it finds the
property or reaches null.
```

---

## 16. Own vs Inherited Properties

Own property:

```javascript
const user = { name: "Ava" };
console.log(Object.hasOwn(user, "name")); // true
```

Inherited property:

```javascript
console.log(Object.hasOwn(user, "toString")); // false
console.log("toString" in user); // true
```

Difference:

| Check | Includes Inherited? |
|---|---:|
| `Object.hasOwn(obj, key)` | No |
| `obj.hasOwnProperty(key)` | No, but can be unsafe if shadowed/missing |
| `key in obj` | Yes |

Safer modern check:

```javascript
Object.hasOwn(user, "name");
```

Production caution:

```text
When validating plain data objects, inherited properties can surprise you. Prefer Object.hasOwn
for own-property checks.
```

---

## 17. Prototype Chain Lookup

Example:

```javascript
const animal = {
    eats: true,
    walk() {
        return "walking";
    }
};

const dog = Object.create(animal);
dog.barks = true;

console.log(dog.barks); // true, own
console.log(dog.eats);  // true, inherited
console.log(dog.walk()); // walking, inherited method
```

Lookup for `dog.walk`:

```text
dog has walk? no
animal has walk? yes
return animal.walk
```

Important:

```javascript
dog.eats = false;
console.log(dog.eats);    // false, own property shadows inherited property
console.log(animal.eats); // true
```

Interview line:

```text
Writing a property usually creates or updates an own property on the object. It does not
normally mutate the prototype property unless a setter is involved.
```

---

## 18. Function `prototype` vs Object `[[Prototype]]`

This is a common confusion.

| Thing | Meaning |
|---|---|
| `obj.[[Prototype]]` | Internal link used for lookup |
| `Object.getPrototypeOf(obj)` | Standard way to read internal prototype |
| `__proto__` | Legacy accessor for internal prototype |
| `FunctionName.prototype` | Object assigned as prototype for instances created with `new FunctionName()` |

Example:

```javascript
function User(name) {
    this.name = name;
}

User.prototype.greet = function () {
    return `Hi ${this.name}`;
};

const user = new User("Ava");

console.log(user.greet()); // Hi Ava
console.log(Object.getPrototypeOf(user) === User.prototype); // true
```

Memory line:

```text
A constructor function's prototype becomes the [[Prototype]] of objects created with new.
```

Avoid saying:

```text
Every object has a prototype property.
```

Better:

```text
Every ordinary object has an internal prototype link. Functions additionally have a prototype
property used when they are called with new.
```

---

## 19. Constructor Functions

Before class syntax, constructor functions were common.

```javascript
function Booking(id, roomId) {
    this.id = id;
    this.roomId = roomId;
    this.status = "CREATED";
}

Booking.prototype.confirm = function () {
    this.status = "CONFIRMED";
};

const booking = new Booking("B1", "R101");
booking.confirm();

console.log(booking.status); // CONFIRMED
```

Why methods go on prototype:

```text
All instances can share one method function instead of creating a new function per instance.
```

Trap without `new`:

```javascript
function User(name) {
    this.name = name;
}

User("Ava"); // TypeError in strict mode, or accidental global write in sloppy mode
```

Modern protection:

```javascript
function User(name) {
    if (!new.target) {
        throw new Error("User must be called with new");
    }
    this.name = name;
}
```

---

## 20. `Object.create`

`Object.create(proto)` creates an object with a chosen prototype.

```javascript
const serviceMethods = {
    confirm() {
        this.status = "CONFIRMED";
    }
};

const booking = Object.create(serviceMethods);
booking.id = "B1";
booking.status = "CREATED";

booking.confirm();
console.log(booking.status); // CONFIRMED
```

Null-prototype object:

```javascript
const dictionary = Object.create(null);
dictionary.roomId = "R101";

console.log(Object.getPrototypeOf(dictionary)); // null
```

Use case:

```text
A null-prototype object can be useful as a dictionary because it does not inherit Object.prototype
keys like toString.
```

Production caution:

```text
Null-prototype objects do not have methods like hasOwnProperty, so use Object.hasOwn.
```

---

## 21. `Object.getPrototypeOf` And `Object.setPrototypeOf`

Read prototype:

```javascript
const user = {};
console.log(Object.getPrototypeOf(user) === Object.prototype); // true
```

Set prototype:

```javascript
const animal = { eats: true };
const dog = { barks: true };

Object.setPrototypeOf(dog, animal);
console.log(dog.eats); // true
```

Production caution:

```text
Changing an object's prototype after creation can hurt performance and make behavior harder to
reason about. Prefer Object.create or class/constructor setup at creation time.
```

Interview line:

```text
Use Object.getPrototypeOf for reading prototypes. Avoid changing prototypes dynamically in hot
production code unless there is a very specific reason.
```

---

## 22. Property Descriptors

Properties have descriptors.

```javascript
const user = {};

Object.defineProperty(user, "id", {
    value: "U1",
    writable: false,
    enumerable: true,
    configurable: false
});

user.id = "U2";
console.log(user.id); // U1
```

Descriptor fields:

| Field | Meaning |
|---|---|
| `value` | Stored value |
| `writable` | Can value be changed? |
| `enumerable` | Appears in enumeration like `Object.keys`? |
| `configurable` | Can descriptor be changed or property deleted? |
| `get` | Getter function |
| `set` | Setter function |

Read descriptor:

```javascript
console.log(Object.getOwnPropertyDescriptor(user, "id"));
```

Strong answer:

```text
Property descriptors control whether a property is writable, enumerable, configurable, or
implemented through getters and setters. They are part of JavaScript's object model under the
normal property access syntax.
```

---

## 23. Getters And Setters

Getters and setters look like properties but run functions.

```javascript
const booking = {
    firstName: "Ava",
    lastName: "Mia",

    get fullName() {
        return `${this.firstName} ${this.lastName}`;
    },

    set fullName(value) {
        const [firstName, lastName] = value.split(" ");
        this.firstName = firstName;
        this.lastName = lastName;
    }
};

console.log(booking.fullName); // Ava Mia
booking.fullName = "Noah Lee";
console.log(booking.firstName); // Noah
```

Production caution:

```text
Getters should usually be cheap and side-effect-free. Expensive or surprising getters make
code harder to debug.
```

---

## 24. Classes Are Prototype Syntax

Class syntax is cleaner syntax over prototype-based behavior.

```javascript
class Booking {
    constructor(id, roomId) {
        this.id = id;
        this.roomId = roomId;
        this.status = "CREATED";
    }

    confirm() {
        this.status = "CONFIRMED";
    }
}

const booking = new Booking("B1", "R101");
booking.confirm();
```

Check where method lives:

```javascript
console.log(Object.hasOwn(booking, "confirm")); // false
console.log(Object.hasOwn(Booking.prototype, "confirm")); // true
```

Important:

```text
Class methods are placed on the prototype, not copied as own methods per instance.
```

Strong answer:

```text
JavaScript classes are mostly syntactic sugar over constructor functions and prototypes. The
methods are stored on the class prototype and instances delegate to that prototype.
```

---

## 25. Class Fields

Public class fields are instance fields.

```javascript
class Booking {
    status = "CREATED";

    constructor(id) {
        this.id = id;
    }
}

const booking = new Booking("B1");
console.log(booking.status); // CREATED
```

Arrow function class field:

```javascript
class Counter {
    count = 0;

    increment = () => {
        this.count++;
        return this.count;
    };
}
```

Trade-off:

```text
increment is created per instance, so it keeps lexical this but is not shared on the prototype.
```

Production line:

```text
Use prototype methods by default for shared behavior. Use arrow fields when callback binding is
more valuable than sharing one method function.
```

---

## 26. Private Fields

Private fields use `#` and are enforced by the language.

```javascript
class BankAccount {
    #balance;

    constructor(initialBalance) {
        this.#balance = initialBalance;
    }

    deposit(amount) {
        if (amount <= 0) {
            throw new Error("amount must be positive");
        }
        this.#balance += amount;
    }

    getBalance() {
        return this.#balance;
    }
}

const account = new BankAccount(100);
account.deposit(50);
console.log(account.getBalance()); // 150
```

This fails:

```javascript
// account.#balance; // SyntaxError outside the class body
```

Interview line:

```text
Private fields are not just naming convention. They are language-level private state accessible
only inside the class body.
```

---

## 27. Static Fields And Methods

Static members belong to the class constructor, not instances.

```javascript
class Booking {
    static allowedStatuses = ["CREATED", "CONFIRMED", "CANCELLED"];

    static isValidStatus(status) {
        return Booking.allowedStatuses.includes(status);
    }

    constructor(id) {
        this.id = id;
    }
}

console.log(Booking.isValidStatus("CREATED")); // true

const booking = new Booking("B1");
console.log(typeof booking.isValidStatus); // undefined
```

Use static for:

- Factory helpers.
- Validation helpers.
- Constants.
- Class-level metadata.

Interview line:

```text
Static methods are called on the class itself, not on instances. They are useful for behavior
that belongs to the type rather than one object.
```

---

## 28. Inheritance With `extends` And `super`

Example:

```javascript
class User {
    constructor(name) {
        this.name = name;
    }

    describe() {
        return `User: ${this.name}`;
    }
}

class Admin extends User {
    constructor(name, permissions) {
        super(name);
        this.permissions = permissions;
    }

    describe() {
        return `${super.describe()} with ${this.permissions.length} permissions`;
    }
}

const admin = new Admin("Ava", ["READ", "WRITE"]);
console.log(admin.describe());
```

Rules:

```text
A derived class constructor must call super() before using this.
super.method() calls the parent prototype method.
```

Prototype chain:

```text
admin -> Admin.prototype -> User.prototype -> Object.prototype -> null
```

Strong answer:

```text
extends sets up prototype delegation between child and parent prototypes. super() initializes
this through the parent constructor, and super.method() delegates to the parent prototype method.
```

---

## 29. `instanceof`

`instanceof` checks whether a constructor's prototype appears in an object's prototype chain.

```javascript
class User {}

const user = new User();

console.log(user instanceof User); // true
console.log(user instanceof Object); // true
```

Prototype-chain mental model:

```text
Does User.prototype appear somewhere in user.[[Prototype]] chain?
```

Trap across realms:

```text
Objects from different iframes/windows can have different constructors, so instanceof can be
surprising across realms.
```

Array check:

```javascript
Array.isArray(value);
```

is usually safer than:

```javascript
value instanceof Array;
```

Interview line:

```text
instanceof is prototype-chain based, not a deep type system. For arrays, Array.isArray is the
safer cross-realm check.
```

---

## 30. Built-In Prototypes

Built-ins also use prototypes.

```javascript
const names = ["Ava", "Mia"];

console.log(Object.getPrototypeOf(names) === Array.prototype); // true
console.log(typeof names.map); // function
```

Lookup:

```text
names -> Array.prototype -> Object.prototype -> null
```

String primitive wrapper behavior:

```javascript
console.log("hello".toUpperCase()); // HELLO
```

Mental model:

```text
JavaScript temporarily boxes primitives when accessing methods, so string methods can be called
on string primitives.
```

Production caution:

```text
Do not modify built-in prototypes in application code. It can break libraries, cause conflicts,
and create hard-to-debug behavior.
```

---

## 31. Monkey Patching And Prototype Pollution Awareness

Monkey patching means changing existing objects or prototypes at runtime.

Bad idea in most app code:

```javascript
Array.prototype.first = function () {
    return this[0];
};
```

Why risky:

- Can conflict with future language features.
- Can conflict with libraries.
- Can affect every array globally.
- Can change enumeration behavior if done badly.
- Makes debugging harder.

Prototype pollution means attacker-controlled input modifies prototypes.

Danger shape:

```javascript
const input = JSON.parse('{"__proto__":{"isAdmin":true}}');
Object.assign({}, input);
```

Modern runtimes and libraries may guard against many cases, but the risk remains in unsafe merge logic.

Production line:

```text
I avoid mutating built-in prototypes and treat deep merge of untrusted input carefully. For
security-sensitive object maps, I prefer allowlists, schema validation, and sometimes
null-prototype objects.
```

---

## 32. Composition vs Inheritance

Inheritance can be useful, but composition is often simpler.

Inheritance:

```javascript
class EmailNotifier extends Notifier {
}
```

Composition:

```javascript
class BookingService {
    constructor(notifier, paymentClient) {
        this.notifier = notifier;
        this.paymentClient = paymentClient;
    }
}
```

When inheritance fits:

- Clear is-a relationship.
- Shared base behavior is stable.
- Polymorphism is useful.
- Hierarchy is shallow.

When composition is better:

- Behavior changes independently.
- Multiple capabilities need to be combined.
- Testing needs easy dependency replacement.
- Inheritance hierarchy would become deep or fragile.

Senior line:

```text
JavaScript supports prototype and class inheritance, but I prefer composition for most
application services because dependencies and behavior are easier to test, replace, and reason about.
```

---

## 33. Common Output Traps

### Trap 1: Method Extraction

```javascript
const user = {
    name: "Ava",
    show() {
        console.log(this.name);
    }
};

const show = user.show;
show();
```

Output:

```text
undefined or TypeError depending on strictness and access
```

Rule:

```text
The receiver was lost. this is not permanently attached to the method.
```

### Trap 2: Nested Method Receiver

```javascript
const user = {
    name: "Ava",
    profile: {
        name: "Admin",
        show() {
            console.log(this.name);
        }
    }
};

user.profile.show();
```

Output:

```text
Admin
```

Rule:

```text
this is the object before the final dot: user.profile.
```

### Trap 3: Arrow Method

```javascript
const user = {
    name: "Ava",
    show: () => console.log(this.name)
};

user.show();
```

Output:

```text
usually undefined
```

Rule:

```text
Arrow function this is lexical, not the object receiver.
```

### Trap 4: Constructor Without `new`

```javascript
function User(name) {
    this.name = name;
}

User("Ava");
```

Output:

```text
TypeError in strict mode or accidental global mutation in sloppy mode
```

Rule:

```text
Without new, there is no new instance this.
```

### Trap 5: Prototype Method Shared

```javascript
function Counter() {
    this.count = 0;
}

Counter.prototype.increment = function () {
    this.count++;
};

const a = new Counter();
const b = new Counter();

a.increment();
console.log(a.count, b.count);
```

Output:

```text
1 0
```

Rule:

```text
The method is shared, but each instance has its own count property.
```

### Trap 6: Inherited Property Shadowing

```javascript
const parent = { role: "user" };
const child = Object.create(parent);

child.role = "admin";

console.log(child.role);
console.log(parent.role);
```

Output:

```text
admin
user
```

Rule:

```text
Writing creates/shadows an own property on child.
```

---

## 34. Mini Program: Booking Classes And Prototype Checks

This example combines classes, methods, private fields, static validation, inheritance, and prototype checks.

```javascript
class Booking {
    static statuses = new Set(["CREATED", "CONFIRMED", "CANCELLED"]);

    #status = "CREATED";

    constructor(id, roomId) {
        if (!id || !roomId) {
            throw new Error("id and roomId are required");
        }
        this.id = id;
        this.roomId = roomId;
    }

    static isValidStatus(status) {
        return Booking.statuses.has(status);
    }

    get status() {
        return this.#status;
    }

    confirm() {
        this.#setStatus("CONFIRMED");
    }

    cancel() {
        this.#setStatus("CANCELLED");
    }

    #setStatus(nextStatus) {
        if (!Booking.isValidStatus(nextStatus)) {
            throw new Error("invalid status");
        }
        this.#status = nextStatus;
    }
}

class PaidBooking extends Booking {
    constructor(id, roomId, amountCents) {
        super(id, roomId);
        this.amountCents = amountCents;
    }

    describe() {
        return `${this.id} for ${this.roomId}: ${this.status}, amount=${this.amountCents}`;
    }
}

const booking = new PaidBooking("B1", "R101", 25000);
booking.confirm();

console.log(booking.describe());
console.log(booking instanceof PaidBooking); // true
console.log(booking instanceof Booking); // true
console.log(Object.hasOwn(booking, "confirm")); // false
console.log(Object.hasOwn(Booking.prototype, "confirm")); // true
```

Interview explanation:

```text
confirm is shared on Booking.prototype, not copied onto each instance. The private status field
is instance state. PaidBooking extends Booking, so the instance prototype chain includes both
PaidBooking.prototype and Booking.prototype.
```

---

## 35. Production Scenario: Lost `this` In A Node.js Service

Problem:

```javascript
class BookingWorker {
    constructor(repository) {
        this.repository = repository;
    }

    async process(job) {
        await this.repository.save(job.bookingId);
    }
}

const worker = new BookingWorker(repository);
queue.on("job", worker.process); // bug
```

Why it fails:

```text
worker.process is passed as a function reference. When the queue calls it later, the receiver
is not worker, so this.repository is undefined.
```

Fix 1: bind once

```javascript
queue.on("job", worker.process.bind(worker));
```

Better if removal is needed:

```javascript
const handler = worker.process.bind(worker);
queue.on("job", handler);
queue.off("job", handler);
```

Fix 2: wrapper

```javascript
queue.on("job", job => worker.process(job));
```

Fix 3: class field arrow method

```javascript
class BookingWorker {
    constructor(repository) {
        this.repository = repository;
    }

    process = async (job) => {
        await this.repository.save(job.bookingId);
    };
}
```

Trade-off:

```text
The arrow method keeps lexical this but creates a function per instance. That is usually fine
for a few service instances, but prototype methods are more memory-efficient for many instances.
```

Strong answer:

```text
This is a lost receiver bug. Class methods are not auto-bound, so passing worker.process loses
this. I would bind the method, wrap the call, or use an arrow field depending on lifecycle and
performance needs.
```

---

## 36. Common Mistakes

| Mistake | Why It Is Wrong | Better Approach |
|---|---|---|
| Saying `this` is where function is defined | Wrong for normal functions | Look at call site |
| Using arrow functions as object methods needing `this` | Arrow has lexical this | Use method syntax |
| Passing methods as callbacks without binding | Receiver is lost | Bind, wrap, or use arrow field |
| Thinking classes auto-bind methods | They do not | Bind explicitly when needed |
| Confusing `prototype` and `[[Prototype]]` | Different concepts | Function prototype vs object prototype link |
| Saying classes are completely different from prototypes | Misleading | Classes use prototype-based method lookup |
| Modifying built-in prototypes | Global side effects | Use utility functions |
| Using `instanceof Array` for robust array checks | Cross-realm issue | Use `Array.isArray` |
| Using `obj.hasOwnProperty` blindly | Can be shadowed or absent | Use `Object.hasOwn` |
| Changing prototypes dynamically | Performance/readability risk | Set prototype at creation |
| Deep inheritance hierarchy | Fragile design | Prefer composition where possible |
| Ignoring prototype pollution | Security risk | Validate input and avoid unsafe deep merge |

---

## 37. Strong Interview Answers

### What is `this` in JavaScript?

```text
this is a value supplied when a function is called. For normal functions, it is usually decided
by the call site: obj.method gives this as obj, call/apply/bind set it explicitly, new creates
a new this, and plain function calls use default binding. Arrow functions are different because
they capture this lexically.
```

### Why does a method lose `this`?

```text
A method is just a function value stored on an object. When I pass obj.method as a callback, I
pass the function without its receiver. Later it may be called as a plain function, so this is
lost. I fix it with bind, a wrapper, or an arrow field when appropriate.
```

### What is the prototype chain?

```text
The prototype chain is JavaScript's property lookup path. If an object does not have a property
itself, JavaScript checks the object's prototype, then that prototype's prototype, until it
finds the property or reaches null.
```

### What happens with `new`?

```text
new creates a new object, links it to the constructor's prototype, calls the constructor with
this set to that object, and returns that object unless the constructor explicitly returns a
different object.
```

### Are classes different from prototypes?

```text
JavaScript classes are mostly cleaner syntax over constructor functions and prototypes. Methods
are stored on the class prototype, instances delegate through the prototype chain, and extends
sets up inheritance between prototypes.
```

---

## 38. FAANG-Level Question

> A JavaScript service passes a class method to a queue consumer. In production, jobs fail with `Cannot read properties of undefined`, and a later patch using arrow methods increases memory usage when thousands of instances are created. Explain the issue and your fix.

Strong answer:

```text
The first issue is a lost this binding. Class methods are prototype methods and are not
automatically bound. Passing worker.process to the queue passes only the function value. When
the queue invokes it, the receiver is no longer the worker instance, so this.repository is
undefined.

The direct fixes are to bind once and keep the bound reference, or wrap the call with an arrow
like job => worker.process(job). A class field arrow method also works because it captures
lexical this, but it creates one function per instance rather than sharing a prototype method.
If there are thousands of instances, that memory trade-off matters.

My production fix would be to keep process as a prototype method and bind once during wiring,
or use a wrapper at the queue boundary. I would also add a test that invokes the registered
handler the same way the queue does, because calling worker.process directly in a test would
miss the bug.
```

That answer shows:

- `this` call-site knowledge.
- Class/prototype method knowledge.
- Callback production debugging.
- Memory trade-off awareness.
- Test design maturity.

---

## 39. Rapid Revision

- `this` is usually determined by call site.
- Arrow functions capture lexical `this`.
- `obj.method()` gives `this` as `obj`.
- `fn()` gives default binding.
- Strict-mode default `this` is `undefined`.
- `call` and `apply` invoke immediately with explicit `this`.
- `bind` returns a new function with bound `this`.
- Passing `obj.method` loses the receiver.
- Class methods are not auto-bound.
- `new` creates an object, links prototype, calls constructor, returns object.
- Constructor function methods should usually live on prototype.
- Every ordinary object has an internal prototype link.
- Functions have a `prototype` property used by `new`.
- `Object.getPrototypeOf(obj)` reads the prototype.
- Prefer not to use `__proto__` in production code.
- Prototype lookup walks object -> prototype -> prototype -> null.
- Own properties shadow inherited properties.
- Use `Object.hasOwn` for own-property checks.
- `Object.create(proto)` creates an object with a chosen prototype.
- Classes are syntax over prototypes.
- Class methods are on the prototype.
- Class fields are per instance.
- Private fields with `#` are language-level private state.
- Static methods belong to the class, not instances.
- `extends` sets up prototype inheritance.
- `super()` must run before `this` in derived constructors.
- `instanceof` checks prototype chain.
- Use `Array.isArray` for arrays.
- Avoid modifying built-in prototypes.
- Prefer composition over deep inheritance for application services.
- Treat prototype pollution as a real security risk.

---

## 40. Official Source Notes

Use these sources when refreshing `this`, prototypes, and class behavior:

- ECMAScript specification: `https://tc39.es/ecma262/`
- MDN `this`: `https://developer.mozilla.org/en-US/docs/Web/JavaScript/Reference/Operators/this`
- MDN Inheritance and prototype chain: `https://developer.mozilla.org/en-US/docs/Web/JavaScript/Inheritance_and_the_prototype_chain`
- MDN Classes: `https://developer.mozilla.org/en-US/docs/Web/JavaScript/Reference/Classes`
- MDN `new`: `https://developer.mozilla.org/en-US/docs/Web/JavaScript/Reference/Operators/new`
- MDN `Function.prototype.call`: `https://developer.mozilla.org/en-US/docs/Web/JavaScript/Reference/Global_Objects/Function/call`
- MDN `Function.prototype.apply`: `https://developer.mozilla.org/en-US/docs/Web/JavaScript/Reference/Global_Objects/Function/apply`
- MDN `Function.prototype.bind`: `https://developer.mozilla.org/en-US/docs/Web/JavaScript/Reference/Global_Objects/Function/bind`
- MDN `Object.create`: `https://developer.mozilla.org/en-US/docs/Web/JavaScript/Reference/Global_Objects/Object/create`
- MDN Property descriptors: `https://developer.mozilla.org/en-US/docs/Web/JavaScript/Reference/Global_Objects/Object/defineProperty`

Interview safety line:

```text
For this questions, I inspect the call site. For prototype questions, I inspect the lookup
chain. For class questions, I remember that class syntax still uses prototype-based method
sharing and normal JavaScript this rules.
```
