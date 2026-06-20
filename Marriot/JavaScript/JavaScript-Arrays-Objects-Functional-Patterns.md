# JavaScript Arrays, Objects, And Functional Patterns

Target: JavaScript coding interviews, frontend data transformation rounds, Node.js backend payload shaping, and production code reviews where arrays and objects are everywhere.

This sheet covers:
- Array and object mental models
- Mutating vs non-mutating methods
- `map`, `filter`, `reduce`, `forEach`, `find`, `some`, `every`, `flatMap`
- Sorting, grouping, indexing, partitioning, deduplication
- Object transforms with `Object.keys`, `Object.values`, `Object.entries`, `Object.fromEntries`
- Spread/rest and destructuring in data transformations
- Shallow copy vs nested update patterns
- `Map`, `Set`, `WeakMap`, and `WeakSet`
- Immutability patterns for frontend state and backend payloads
- Performance trade-offs and readability judgment
- Common interview traps and production-safe answers

How to use this:
- Learn the method selection table first.
- Practice the sample problems until you can solve them without looking.
- For each pattern, say the strong interview answer out loud.
- In real code, prefer clarity over clever chained transformations.

---

## 1. Mental Model

Arrays and objects are the daily data structures of JavaScript.

Simple model:

```text
Array  -> ordered list of values
Object -> named properties / record-like shape
Map    -> key-value store with any key type
Set    -> unique values
```

Functional array methods help express data transformations:

```text
source data
    -> filter what you need
    -> map into the shape you want
    -> reduce/group/index when you need aggregation
```

Important production rule:

```text
Know which operations mutate and which return new values.
```

Strong interview line:

```text
I use arrays for ordered data, objects for structured records, Map for flexible key-value
lookup, and Set for uniqueness. For transformations, I choose the simplest method that matches
the intent: map to transform, filter to keep, find to locate one, some/every for predicates,
and reduce when I truly need aggregation.
```

---

## 2. Interview Priority Meter

| Topic | Priority | Why It Matters |
|---|---:|---|
| Mutating vs non-mutating methods | Very high | State bugs and interview traps |
| `map` | Very high | Transform list values |
| `filter` | Very high | Select matching values |
| `reduce` | Very high | Aggregation and grouping |
| `forEach` vs `map` | Very high | Side effects vs transformation |
| `find` / `some` / `every` | High | Efficient predicate problems |
| `sort` behavior | Very high | Numeric sort trap and mutation |
| `flatMap` | High | Flatten nested data cleanly |
| Spread and shallow copy | Very high | Immutability and shared-reference bugs |
| Object entries/fromEntries | High | Object transformation patterns |
| Grouping and indexing | Very high | Common coding interview task |
| Deduplication | High | Set and Map usage |
| Map vs Object | High | Lookup design judgment |
| Set vs Array includes | Medium-high | Membership performance |
| Optional chaining/nullish checks | High | Safe data handling |
| Performance trade-offs | High | Senior-level maturity |
| Readability judgment | Very high | Avoid clever unreadable chains |

---

## 3. Method Selection Guide

Use this table in interviews.

| Need | Best Tool |
|---|---|
| Transform every item | `map` |
| Keep only matching items | `filter` |
| Run side effect for each item | `forEach` or `for...of` |
| Find first matching item | `find` |
| Find index of first matching item | `findIndex` |
| Check if at least one item matches | `some` |
| Check if all items match | `every` |
| Check primitive membership | `includes` |
| Combine into one value | `reduce` |
| Group by key | `reduce` or `Map` |
| Convert array to object/map | `reduce`, `Object.fromEntries`, or `Map` |
| Remove duplicates | `Set` |
| Sort values | `toSorted` or copy then `sort` |
| Flatten one level | `flat` |
| Map and flatten | `flatMap` |

Strong answer:

```text
I do not use reduce for everything. I use map when the output has one value per input, filter
when I am selecting values, find when I need one item, and reduce when I am accumulating or
building a different structure like a grouped object or index.
```

---

## 4. Sample Data

Use this model for the examples.

```javascript
const employees = [
    {
        id: 1,
        name: "Aravind",
        department: "Engineering",
        salary: 120000,
        active: true,
        skills: ["JavaScript", "Node", "React"]
    },
    {
        id: 2,
        name: "Rahul",
        department: "Engineering",
        salary: 150000,
        active: true,
        skills: ["JavaScript", "AWS"]
    },
    {
        id: 3,
        name: "Priya",
        department: "HR",
        salary: 80000,
        active: false,
        skills: ["Hiring", "Excel"]
    },
    {
        id: 4,
        name: "Sneha",
        department: "Engineering",
        salary: 110000,
        active: true,
        skills: ["React", "CSS"]
    },
    {
        id: 5,
        name: "Vikram",
        department: "Finance",
        salary: 130000,
        active: false,
        skills: ["SQL", "Excel"]
    }
];
```

---

## 5. Arrays Are Objects With Order

Arrays are objects with numeric indexes and a `length` property.

```javascript
const values = [10, 20, 30];

console.log(values[0]); // 10
console.log(values.length); // 3
console.log(typeof values); // object
console.log(Array.isArray(values)); // true
```

Sparse array trap:

```javascript
const values = [];
values[3] = "x";

console.log(values.length); // 4
console.log(values); // [empty x 3, "x"]
```

Production line:

```text
Arrays are best for dense ordered lists. If keys are not natural numeric positions, use an
object or Map instead of sparse arrays.
```

---

## 6. Mutating vs Non-Mutating Array Methods

This is one of the most important JavaScript habits.

| Method | Mutates Original? | Notes |
|---|---:|---|
| `push` | Yes | Adds to end |
| `pop` | Yes | Removes from end |
| `shift` | Yes | Removes from start |
| `unshift` | Yes | Adds to start |
| `splice` | Yes | Insert/remove/replace in place |
| `sort` | Yes | Sorts in place |
| `reverse` | Yes | Reverses in place |
| `fill` | Yes | Fills in place |
| `copyWithin` | Yes | Copies within same array |
| `map` | No | Returns new transformed array |
| `filter` | No | Returns new selected array |
| `slice` | No | Returns shallow copy portion |
| `concat` | No | Returns combined array |
| `flat` | No | Returns flattened array |
| `flatMap` | No | Maps and flattens one level |
| `toSorted` | No | Modern non-mutating sort |
| `toReversed` | No | Modern non-mutating reverse |
| `toSpliced` | No | Modern non-mutating splice |

Mutation trap:

```javascript
const original = [3, 1, 2];
const sorted = original.sort();

console.log(original); // [1, 2, 3]
console.log(sorted);   // [1, 2, 3]
```

Safe older pattern:

```javascript
const sorted = [...original].sort((a, b) => a - b);
```

Modern pattern when supported:

```javascript
const sorted = original.toSorted((a, b) => a - b);
```

Strong answer:

```text
Before using an array method, I check whether it mutates. In UI state or shared backend data,
accidental mutation can create hard-to-debug bugs, so I often copy before sort, reverse, or splice.
```

---

## 7. `map`: Transform Each Item

Use `map` when each input item becomes one output item.

Question:

> Get employee names.

```javascript
const names = employees.map(employee => employee.name);

console.log(names);
```

Output:

```text
["Aravind", "Rahul", "Priya", "Sneha", "Vikram"]
```

Question:

> Convert employees into API response DTOs.

```javascript
const response = employees.map(employee => ({
    id: employee.id,
    label: `${employee.name} (${employee.department})`,
    active: employee.active
}));
```

Trap: using `map` for side effects

```javascript
const names = [];

employees.map(employee => {
    names.push(employee.name);
});
```

Better:

```javascript
const names = employees.map(employee => employee.name);
```

Interview line:

```text
map is for transformation. It should return a new value for each item. If I am only doing side
effects, map is the wrong signal.
```

---

## 8. `filter`: Keep Matching Items

Use `filter` when you want a subset.

Question:

> Get active engineering employees.

```javascript
const activeEngineering = employees.filter(employee =>
    employee.active && employee.department === "Engineering"
);
```

Question:

> Remove inactive employees.

```javascript
const activeEmployees = employees.filter(employee => employee.active);
```

Filter keeps original item references.

```javascript
const activeEmployees = employees.filter(employee => employee.active);
activeEmployees[0].name = "Changed";

console.log(employees[0].name); // Changed
```

Why:

```text
filter returns a new array, but the objects inside are the same references.
```

If you need copied objects:

```javascript
const activeCopies = employees
    .filter(employee => employee.active)
    .map(employee => ({ ...employee }));
```

Strong answer:

```text
filter creates a new array, but it does not deep-copy the items. If the items are objects,
mutating a filtered item still mutates the original object.
```

---

## 9. `map` + `filter` Chain

Question:

> Get names of active engineering employees.

Readable chain:

```javascript
const names = employees
    .filter(employee => employee.active)
    .filter(employee => employee.department === "Engineering")
    .map(employee => employee.name);
```

One combined filter:

```javascript
const names = employees
    .filter(employee => employee.active && employee.department === "Engineering")
    .map(employee => employee.name);
```

Both are correct.

Interview judgment:

```text
Separate filters can read better when conditions are conceptually independent. A combined
predicate is fine when the logic is short.
```

Performance nuance:

```text
Each map/filter call creates an intermediate array. For normal interview and UI-sized data,
clarity wins. For very large data or hot paths, use a single loop or transducer-style approach
only when profiling shows it matters.
```

---

## 10. `forEach`: Side Effects Only

Use `forEach` when you want to do something for each item and do not need a returned array.

```javascript
employees.forEach(employee => {
    console.log(employee.name);
});
```

Trap:

```javascript
const result = employees.forEach(employee => employee.name);
console.log(result); // undefined
```

`forEach` does not return transformed values.

Async trap:

```javascript
employees.forEach(async employee => {
    await sendEmail(employee);
});

console.log("done"); // runs before emails finish
```

Better sequential:

```javascript
for (const employee of employees) {
    await sendEmail(employee);
}
```

Better parallel:

```javascript
await Promise.all(employees.map(employee => sendEmail(employee)));
```

Strong answer:

```text
forEach is for side effects and returns undefined. I avoid async forEach when I need to await
completion because forEach does not await the callback promises.
```

---

## 11. `find`, `findIndex`, `some`, `every`, `includes`

Use these for predicate-style questions.

Find first match:

```javascript
const employee = employees.find(employee => employee.id === 2);
```

Find index:

```javascript
const index = employees.findIndex(employee => employee.id === 2);
```

Any match:

```javascript
const hasInactive = employees.some(employee => !employee.active);
```

All match:

```javascript
const allHaveSalary = employees.every(employee => typeof employee.salary === "number");
```

Primitive membership:

```javascript
const allowedRoles = ["admin", "manager", "user"];
console.log(allowedRoles.includes("admin")); // true
```

Performance note:

```text
find, some, and every can short-circuit. map and filter process the whole array.
```

Interview line:

```text
If I only need to know whether a condition exists, I use some. If I need all items to satisfy a
condition, I use every. If I need the item itself, I use find.
```

---

## 12. `reduce`: Accumulate Into One Result

Use `reduce` when you need to build one result from many items.

Sum salaries:

```javascript
const totalSalary = employees.reduce((total, employee) => {
    return total + employee.salary;
}, 0);
```

Count active employees:

```javascript
const activeCount = employees.reduce((count, employee) => {
    return employee.active ? count + 1 : count;
}, 0);
```

Reduce anatomy:

```text
array.reduce((accumulator, currentItem) => nextAccumulator, initialValue)
```

Always provide an initial value for interview clarity.

Trap without initial value:

```javascript
[].reduce((a, b) => a + b); // TypeError
```

Strong answer:

```text
reduce is for accumulation. I use it when I am building a single value or structure from a
list, such as a sum, grouped object, index, or frequency map. I avoid reduce when map/filter
would express the intent more clearly.
```

---

## 13. Group By Pattern

Question:

> Group employees by department.

Object version:

```javascript
const employeesByDepartment = employees.reduce((groups, employee) => {
    const key = employee.department;

    if (!groups[key]) {
        groups[key] = [];
    }

    groups[key].push(employee);
    return groups;
}, {});
```

Output shape:

```text
{
    Engineering: [ ... ],
    HR: [ ... ],
    Finance: [ ... ]
}
```

Immutable-style version:

```javascript
const employeesByDepartment = employees.reduce((groups, employee) => {
    const key = employee.department;
    const existing = groups[key] ?? [];

    return {
        ...groups,
        [key]: [...existing, employee]
    };
}, {});
```

Production nuance:

```text
The immutable-style version is expressive but creates many intermediate objects and arrays.
For large data, the mutating accumulator inside reduce is often acceptable because the mutation
is local to the accumulator, not mutating the source data.
```

Modern note:

```javascript
// Object.groupBy may be available in modern runtimes.
// Always check runtime support before relying on it.
```

Interview line:

```text
For grouping, I usually use reduce with an accumulator object or Map. I am careful to separate
local accumulator mutation from mutating input data.
```

---

## 14. Count By Pattern

Question:

> Count employees by department.

```javascript
const countByDepartment = employees.reduce((counts, employee) => {
    const key = employee.department;
    counts[key] = (counts[key] ?? 0) + 1;
    return counts;
}, {});
```

Frequency count for words:

```javascript
const words = ["js", "node", "js", "react", "node", "js"];

const frequency = words.reduce((counts, word) => {
    counts[word] = (counts[word] ?? 0) + 1;
    return counts;
}, {});

console.log(frequency); // { js: 3, node: 2, react: 1 }
```

Using `Map`:

```javascript
const frequency = new Map();

for (const word of words) {
    frequency.set(word, (frequency.get(word) ?? 0) + 1);
}
```

When `Map` is better:

```text
Use Map when keys are not naturally strings, when insertion order matters strongly, or when you
want cleaner key-value operations.
```

---

## 15. Index By ID Pattern

Question:

> Convert employees into lookup object by ID.

```javascript
const employeeById = employees.reduce((index, employee) => {
    index[employee.id] = employee;
    return index;
}, {});

console.log(employeeById[2].name); // Rahul
```

Using `Object.fromEntries`:

```javascript
const employeeById = Object.fromEntries(
    employees.map(employee => [employee.id, employee])
);
```

Using `Map`:

```javascript
const employeeById = new Map(
    employees.map(employee => [employee.id, employee])
);

console.log(employeeById.get(2).name); // Rahul
```

Duplicate key caution:

```text
If duplicate IDs exist, later entries overwrite earlier entries in Object.fromEntries and Map.
In production, validate uniqueness when duplicates are invalid.
```

Strong answer:

```text
Indexing by ID turns repeated O(n) searches into O(1)-style lookups for objects or Map. I use
it when I need repeated access by key.
```

---

## 16. Partition Pattern

Question:

> Split employees into active and inactive.

```javascript
const partitioned = employees.reduce((result, employee) => {
    const key = employee.active ? "active" : "inactive";
    result[key].push(employee);
    return result;
}, { active: [], inactive: [] });
```

Output shape:

```text
{
    active: [ ... ],
    inactive: [ ... ]
}
```

Reusable utility:

```javascript
function partition(items, predicate) {
    return items.reduce((result, item) => {
        result[predicate(item) ? 0 : 1].push(item);
        return result;
    }, [[], []]);
}

const [active, inactive] = partition(employees, employee => employee.active);
```

Interview line:

```text
Partitioning is grouping into exactly two buckets based on a boolean predicate.
```

---

## 17. Deduplication With Set

Primitive dedupe:

```javascript
const departments = employees.map(employee => employee.department);
const uniqueDepartments = [...new Set(departments)];

console.log(uniqueDepartments);
```

One-liner:

```javascript
const uniqueDepartments = [...new Set(employees.map(employee => employee.department))];
```

Object dedupe by ID:

```javascript
const uniqueById = [...new Map(
    employees.map(employee => [employee.id, employee])
).values()];
```

Keep first duplicate instead of last:

```javascript
const seen = new Set();
const uniqueById = employees.filter(employee => {
    if (seen.has(employee.id)) {
        return false;
    }

    seen.add(employee.id);
    return true;
});
```

Strong answer:

```text
Set is great for primitive uniqueness. For object uniqueness by a field like id, I use Map or a
Set of seen keys because object identity is not the same as business identity.
```

---

## 18. `flat` And `flatMap`

Flatten nested arrays:

```javascript
const nested = [[1, 2], [3, 4], [5]];
console.log(nested.flat()); // [1, 2, 3, 4, 5]
```

Get all skills:

```javascript
const skills = employees.flatMap(employee => employee.skills);
```

Unique sorted skills:

```javascript
const uniqueSkills = [...new Set(
    employees.flatMap(employee => employee.skills)
)].sort();
```

`map` + `flat` equivalent:

```javascript
const skills = employees
    .map(employee => employee.skills)
    .flat();
```

`flatMap` is cleaner when each item maps to an array and you want one flattened result.

Interview line:

```text
flatMap maps each item to an array and flattens one level. It is useful for nested lists like
employees to skills, orders to line items, or users to roles.
```

---

## 19. Sorting Patterns

Default sort converts values to strings.

```javascript
const numbers = [10, 2, 1];
numbers.sort();

console.log(numbers); // [1, 10, 2]
```

Numeric sort:

```javascript
const numbers = [10, 2, 1];
const sorted = [...numbers].sort((a, b) => a - b);

console.log(sorted); // [1, 2, 10]
```

Sort employees by salary descending:

```javascript
const bySalaryDesc = [...employees].sort((a, b) => b.salary - a.salary);
```

Sort by name:

```javascript
const byName = [...employees].sort((a, b) => a.name.localeCompare(b.name));
```

Modern non-mutating sort:

```javascript
const bySalaryDesc = employees.toSorted((a, b) => b.salary - a.salary);
```

Production caution:

```text
sort mutates the array. In UI state or shared data, copy first or use toSorted when supported.
```

---

## 20. Top N Pattern

Question:

> Get top 3 highest-paid employees.

```javascript
const topThree = [...employees]
    .sort((a, b) => b.salary - a.salary)
    .slice(0, 3);
```

For huge data:

```text
Sorting all items is O(n log n). For very large data and small N, a heap can be more efficient.
For normal interview/frontend data, sorting is often fine and clearer.
```

Interview line:

```text
For typical coding rounds I sort descending and slice. If the interviewer asks about scale, I
mention that a heap can reduce work when N is small relative to the data size.
```

---

## 21. Object Basics

Objects represent structured records.

```javascript
const booking = {
    id: "B1",
    roomId: "R101",
    status: "CONFIRMED"
};
```

Read properties:

```javascript
console.log(booking.id);
console.log(booking["roomId"]);
```

Computed property:

```javascript
const field = "status";
console.log(booking[field]);
```

Dynamic object creation:

```javascript
const key = "department";
const value = "Engineering";

const filter = {
    [key]: value
};
```

Production line:

```text
Objects are excellent for known record-like shapes. If keys are highly dynamic or not strings,
Map may be a better fit.
```

---

## 22. Object Keys, Values, Entries

```javascript
const user = {
    id: 1,
    name: "Ava",
    active: true
};

console.log(Object.keys(user));
console.log(Object.values(user));
console.log(Object.entries(user));
```

Output:

```text
["id", "name", "active"]
[1, "Ava", true]
[["id", 1], ["name", "Ava"], ["active", true]]
```

Transform values:

```javascript
const upperCasedStrings = Object.fromEntries(
    Object.entries(user).map(([key, value]) => [
        key,
        typeof value === "string" ? value.toUpperCase() : value
    ])
);
```

Filter object properties:

```javascript
const publicUser = Object.fromEntries(
    Object.entries(user).filter(([key]) => key !== "password")
);
```

Interview line:

```text
Object.entries turns an object into key-value pairs that array methods can transform. Then
Object.fromEntries turns the pairs back into an object.
```

---

## 23. Object Spread And Rest

Copy and override:

```javascript
const booking = { id: "B1", status: "CREATED" };
const confirmed = { ...booking, status: "CONFIRMED" };

console.log(booking.status);   // CREATED
console.log(confirmed.status); // CONFIRMED
```

Remove property with rest:

```javascript
const user = { id: 1, name: "Ava", password: "secret" };
const { password, ...safeUser } = user;

console.log(safeUser); // { id: 1, name: "Ava" }
```

Order matters:

```javascript
const result1 = { status: "CONFIRMED", ...booking };
const result2 = { ...booking, status: "CONFIRMED" };
```

In `result1`, `booking.status` overwrites the earlier status.

In `result2`, the final status overwrites `booking.status`.

Strong answer:

```text
Object spread is a shallow copy. Later properties override earlier properties. It is useful for
immutable updates, but nested objects still share references.
```

---

## 24. Shallow Copy And Nested Update

Shallow copy trap:

```javascript
const user = {
    id: 1,
    profile: {
        city: "NYC"
    }
};

const copy = { ...user };
copy.profile.city = "LA";

console.log(user.profile.city); // LA
```

Correct nested update:

```javascript
const updated = {
    ...user,
    profile: {
        ...user.profile,
        city: "LA"
    }
};
```

Array of objects update:

```javascript
const updatedEmployees = employees.map(employee =>
    employee.id === 2
        ? { ...employee, salary: employee.salary + 10000 }
        : employee
);
```

Remove by ID:

```javascript
const remainingEmployees = employees.filter(employee => employee.id !== 3);
```

Add item immutably:

```javascript
const nextEmployees = [
    ...employees,
    { id: 6, name: "Mia", department: "Engineering", salary: 125000, active: true, skills: [] }
];
```

Production line:

```text
For immutable updates, copy every level that changes. Unchanged nested references can be shared,
but changed paths must get new objects or arrays.
```

---

## 25. `Map` vs Object

| Feature | Object | Map |
|---|---|---|
| Key types | String/symbol keys | Any value as key |
| Size | Manual with `Object.keys(obj).length` | `map.size` |
| Iteration | `Object.entries(obj)` | Directly iterable |
| Prototype keys | Has prototype unless null-prototype | No accidental prototype keys |
| JSON | Direct JSON shape | Needs conversion |
| Record-like data | Strong fit | Often overkill |
| Dynamic lookup table | Good | Strong fit |

Map example:

```javascript
const employeeById = new Map();

for (const employee of employees) {
    employeeById.set(employee.id, employee);
}

console.log(employeeById.get(2).name); // Rahul
```

Object example:

```javascript
const employeeById = Object.fromEntries(
    employees.map(employee => [employee.id, employee])
);
```

Strong answer:

```text
I use plain objects for record-like JSON data and Map for dynamic key-value lookup, especially
when keys are not strings or I need convenient size and iteration.
```

---

## 26. `Set` Patterns

Set stores unique values.

```javascript
const departments = new Set();

departments.add("Engineering");
departments.add("Engineering");
departments.add("HR");

console.log(departments.size); // 2
```

Membership:

```javascript
const allowed = new Set(["admin", "manager", "user"]);

console.log(allowed.has("admin")); // true
```

Why Set can be better than array includes:

```text
For repeated membership checks, Set.has is usually more appropriate than scanning an array each
time with includes.
```

Deduplicate:

```javascript
const unique = [...new Set([1, 2, 2, 3])];
```

Object identity trap:

```javascript
const a = { id: 1 };
const b = { id: 1 };

console.log(new Set([a, b]).size); // 2
```

Why:

```text
Objects are unique by reference, not by matching fields.
```

---

## 27. WeakMap And WeakSet Awareness

WeakMap keys must be objects and are held weakly.

```javascript
const metadata = new WeakMap();
const user = { id: 1 };

metadata.set(user, { lastSeen: Date.now() });
console.log(metadata.get(user));
```

Why useful:

```text
WeakMap can associate metadata with objects without preventing those objects from being garbage
collected when nothing else references them.
```

Limitations:

- WeakMap is not iterable.
- WeakMap keys must be objects.
- WeakSet stores objects only.
- You cannot inspect size.

Use cases:

- Private metadata.
- Caching per object without strong retention.
- DOM node metadata.

Interview line:

```text
WeakMap is useful when I want object-keyed metadata without keeping the key object alive just
because it is in the map.
```

---

## 28. Null-Safe Data Access

Real payloads are messy.

Unsafe:

```javascript
const city = user.profile.address.city;
```

Safe read:

```javascript
const city = user.profile?.address?.city;
```

Default value:

```javascript
const city = user.profile?.address?.city ?? "UNKNOWN";
```

Why `??` instead of `||`:

```javascript
const limit = 0;

console.log(limit || 20); // 20
console.log(limit ?? 20); // 0
```

Safe array fallback:

```javascript
const skills = employee.skills ?? [];
const skillNames = skills.map(skill => skill.toUpperCase());
```

Strong answer:

```text
Optional chaining prevents crashes when intermediate values are null or undefined. Nullish
coalescing gives defaults only for null or undefined, so valid falsy values like 0 or empty
string are preserved.
```

---

## 29. Functional Composition And Pipelines

Small named functions make chains readable.

```javascript
const isActive = employee => employee.active;
const isEngineering = employee => employee.department === "Engineering";
const toName = employee => employee.name;

const names = employees
    .filter(isActive)
    .filter(isEngineering)
    .map(toName);
```

Avoid overly clever chains:

```javascript
const result = employees.reduce((a, e) => ((a[e.department] ??= []).push(e.name), a), {});
```

Better:

```javascript
const namesByDepartment = employees.reduce((groups, employee) => {
    const names = groups[employee.department] ?? [];
    groups[employee.department] = [...names, employee.name];
    return groups;
}, {});
```

Interview line:

```text
Functional style is useful when it makes data flow clear. If a chain becomes hard to read or
hard to debug, I break it into named functions or use a simple loop.
```

---

## 30. `for...of` Is Still Useful

Array methods are expressive, but loops are not bad.

Use `for...of` when:

- You need `await` sequentially.
- You need early `break` or `continue` with complex logic.
- You are doing multiple accumulations.
- Performance matters in a hot path.
- A chain would be unreadable.

Example:

```javascript
const result = [];

for (const employee of employees) {
    if (!employee.active) {
        continue;
    }

    if (employee.salary < 100000) {
        break;
    }

    result.push(employee.name);
}
```

Strong answer:

```text
I like array methods for clear transformations, but I do not force them. A for...of loop can be
more readable for complex control flow, sequential async work, or performance-sensitive code.
```

---

## 31. Performance And Big-O Judgment

Common costs:

| Operation | Typical Cost |
|---|---:|
| Access array by index | O(1) |
| Push/pop at end | O(1) amortized |
| Shift/unshift at start | O(n) |
| Linear search with find/includes | O(n) |
| Sort | O(n log n) typical |
| Build lookup map | O(n) |
| Repeated lookup in object/map | O(1)-style average |
| Nested loops | O(n * m) or O(n^2) |

Optimization example:

Bad repeated search:

```javascript
const ordersWithUsers = orders.map(order => ({
    ...order,
    user: users.find(user => user.id === order.userId)
}));
```

Cost:

```text
O(orders * users)
```

Better index:

```javascript
const userById = new Map(users.map(user => [user.id, user]));

const ordersWithUsers = orders.map(order => ({
    ...order,
    user: userById.get(order.userId) ?? null
}));
```

Cost:

```text
O(users + orders)
```

Senior line:

```text
When I see repeated find inside map, I check whether building an index first would reduce
nested O(n^2)-style behavior to linear work.
```

---

## 32. Production Data-Shaping Rules

| Situation | Good Habit |
|---|---|
| External API payload | Validate shape before transforming |
| UI state update | Do immutable updates for changed paths |
| Sorting shared data | Copy before sort or use `toSorted` |
| Large repeated lookups | Build Map/object index |
| Membership checked often | Use Set |
| Grouping large data | Use local accumulator mutation, not repeated spreading |
| Money values | Keep integer minor units where possible |
| Unknown nested fields | Use optional chaining and explicit defaults |
| Untrusted object keys | Watch for `__proto__` and prototype pollution |
| Complex chains | Break into named functions or loops |

Strong line:

```text
Production data transformation is not only about getting the right output. I also consider
mutation, input validation, algorithmic cost, readability, and whether the transformed shape is
safe for the next layer.
```

---

## 33. Mini Program: Build Department Report

Question:

> Given employees, build a report by department with employee count, active count, total salary, average salary, and sorted unique skills.

```javascript
function buildDepartmentReport(employees) {
    const grouped = employees.reduce((departments, employee) => {
        const department = employee.department;

        if (!departments.has(department)) {
            departments.set(department, {
                department,
                employeeCount: 0,
                activeCount: 0,
                totalSalary: 0,
                skills: new Set()
            });
        }

        const report = departments.get(department);
        report.employeeCount++;
        report.activeCount += employee.active ? 1 : 0;
        report.totalSalary += employee.salary;

        for (const skill of employee.skills ?? []) {
            report.skills.add(skill);
        }

        return departments;
    }, new Map());

    return [...grouped.values()]
        .map(report => ({
            department: report.department,
            employeeCount: report.employeeCount,
            activeCount: report.activeCount,
            totalSalary: report.totalSalary,
            averageSalary: Math.round(report.totalSalary / report.employeeCount),
            skills: [...report.skills].sort()
        }))
        .sort((a, b) => a.department.localeCompare(b.department));
}

console.log(buildDepartmentReport(employees));
```

Why this is strong:

- Uses `Map` for grouping.
- Uses `Set` for unique skills.
- Uses local accumulator mutation without mutating input employees.
- Converts final internal structures into API-friendly plain objects.
- Sorts output for deterministic results.

Interview explanation:

```text
I use Map as the grouping accumulator because departments are dynamic keys. Each group stores
running totals and a Set for unique skills. After accumulation, I convert the Map values into
plain response objects and sort them for deterministic output.
```

---

## 34. Mini Program: Join Orders With Users

Question:

> Join orders with users without doing repeated linear search.

```javascript
const users = [
    { id: "U1", name: "Ava" },
    { id: "U2", name: "Mia" }
];

const orders = [
    { id: "O1", userId: "U1", totalCents: 2500 },
    { id: "O2", userId: "U2", totalCents: 4000 },
    { id: "O3", userId: "U9", totalCents: 1200 }
];

function attachUsers(orders, users) {
    const userById = new Map(users.map(user => [user.id, user]));

    return orders.map(order => ({
        ...order,
        user: userById.get(order.userId) ?? null
    }));
}

console.log(attachUsers(orders, users));
```

Why this is strong:

```text
It avoids orders.map(... users.find(...)), which becomes expensive as data grows.
```

Senior extension:

```text
If a missing user is invalid, I would collect missing IDs and fail clearly instead of silently
returning null.
```

---

## 35. Common Output Traps

### Trap 1: `sort` Mutates

```javascript
const a = [3, 1, 2];
const b = a.sort();

console.log(a);
console.log(b);
```

Output:

```text
[1, 2, 3]
[1, 2, 3]
```

Rule:

```text
sort mutates and returns the same array reference.
```

### Trap 2: Default Numeric Sort

```javascript
console.log([10, 2, 1].sort());
```

Output:

```text
[1, 10, 2]
```

Rule:

```text
Default sort compares strings.
```

### Trap 3: `map` Without Return

```javascript
const result = [1, 2, 3].map(value => {
    value * 2;
});

console.log(result);
```

Output:

```text
[undefined, undefined, undefined]
```

Rule:

```text
Block-body arrow functions need explicit return.
```

### Trap 4: Filter Does Not Clone Objects

```javascript
const active = employees.filter(employee => employee.active);
active[0].name = "Changed";

console.log(employees[0].name);
```

Output:

```text
Changed
```

Rule:

```text
The array is new, but object references are shared.
```

### Trap 5: Set With Objects

```javascript
const result = new Set([{ id: 1 }, { id: 1 }]);
console.log(result.size);
```

Output:

```text
2
```

Rule:

```text
Objects are unique by reference.
```

### Trap 6: `forEach` Return

```javascript
const result = [1, 2, 3].forEach(value => value * 2);
console.log(result);
```

Output:

```text
undefined
```

Rule:

```text
forEach returns undefined.
```

---

## 36. Common Mistakes

| Mistake | Why It Hurts | Better Approach |
|---|---|---|
| Using `map` for side effects | Misleading intent | Use `forEach` or `for...of` |
| Using `forEach` for async work | Does not await callbacks | Use `for...of` or `Promise.all` |
| Using `reduce` for simple map/filter | Harder to read | Use the direct method |
| Forgetting `sort` mutates | Shared state changes | Copy first or use `toSorted` |
| Default sorting numbers | Lexicographic order | Use comparator `(a, b) => a - b` |
| Shallow copying nested state | Nested mutation leaks | Copy every changed level |
| Dedupe objects with Set directly | Uses reference identity | Dedupe by key with Map/Set |
| Repeated `find` inside `map` | O(n^2)-style behavior | Build lookup index first |
| Using object for all maps | Key coercion/prototype issues | Use Map for dynamic lookup |
| Over-chaining | Hard to debug | Use named helpers or loops |
| Mutating function inputs | Surprising side effects | Return new values or document mutation |
| Ignoring missing/null arrays | Runtime crashes | Default with `?? []` |

---

## 37. Strong Interview Answers

### `map` vs `forEach`

```text
map is for transformation and returns a new array. forEach is for side effects and returns
undefined. If I want a transformed result, I use map. If I want to log, send, mutate intentionally,
or call an effect for each item, I use forEach or for...of.
```

### `filter` Behavior

```text
filter returns a new array containing items that match the predicate. It does not deep-clone the
items, so object references inside the filtered array are the same objects as in the original.
```

### When To Use `reduce`

```text
I use reduce when I need to accumulate a list into one result, like a sum, count, grouped object,
lookup map, or frequency table. I avoid reduce when map, filter, find, or some expresses the
intent more directly.
```

### Object vs Map

```text
I use objects for JSON-like records with known string keys. I use Map for dynamic lookup tables,
non-string keys, cleaner size and iteration, or when I want to avoid prototype-key surprises.
```

### Immutability

```text
Immutable updates mean I do not mutate the original object or array. With nested data, I copy
each level that changes. This prevents shared-reference bugs in UI state, caches, and reusable
payloads.
```

---

## 38. FAANG-Level Question

> A dashboard receives 50,000 orders and 10,000 users. The page freezes while attaching user data to each order, sorting results mutates cached data, and some UI updates do not re-render. Explain the likely JavaScript issues and fixes.

Strong answer:

```text
I would look for three common array/object issues. First, if the code does orders.map(order =>
users.find(...)), that is repeated linear search and can become O(orders * users). I would build
a Map of users by ID once, then map orders with O(1)-style lookups.

Second, if sorting mutates cached data, the code probably calls sort directly on a shared array.
I would copy before sorting with [...orders].sort(...) or use toSorted when the runtime supports it.

Third, if UI updates do not re-render, the code may be mutating nested state in place. I would
return new arrays and new objects along the changed path, for example mapping the changed item
and spreading nested objects that changed.

I would also profile before over-optimizing. For very large data, I would consider pagination,
virtualization, web workers, server-side sorting/filtering, and keeping transformations linear.
```

That answer shows:

- Data-structure knowledge.
- Big-O awareness.
- Mutation awareness.
- Frontend production judgment.
- Performance debugging maturity.

---

## 39. Rapid Revision

- `map` transforms one item into one output item.
- `filter` keeps matching items.
- `forEach` is for side effects and returns undefined.
- `find` returns first matching item.
- `some` checks whether any item matches.
- `every` checks whether all items match.
- `reduce` accumulates into one result.
- Use an initial value with `reduce`.
- `sort` mutates.
- Default `sort` compares strings.
- Copy before sort with `[...array].sort(...)`.
- Use `toSorted` when supported for non-mutating sort.
- `filter` returns a new array but shared item references.
- Spread creates shallow copies.
- Copy every changed level in nested state.
- Use `Set` for primitive uniqueness.
- Use `Map` or seen keys for object dedupe by ID.
- Use `Object.entries` and `Object.fromEntries` for object transforms.
- Use `Map` for dynamic key-value lookup.
- Use `Set.has` for repeated membership checks.
- WeakMap holds object keys weakly and is not iterable.
- Optional chaining protects nested reads.
- Nullish coalescing preserves `0`, `false`, and empty string.
- Repeated `find` inside `map` can be expensive.
- Build an index for repeated lookups.
- Use loops when chains become unreadable or need control flow.
- Do not use async `forEach` when you need to await completion.
- Validate external payloads before transforming.

---

## 40. Official Source Notes

Use these sources when refreshing arrays, objects, and collection details:

- MDN Array: `https://developer.mozilla.org/en-US/docs/Web/JavaScript/Reference/Global_Objects/Array`
- MDN Array methods: `https://developer.mozilla.org/en-US/docs/Web/JavaScript/Reference/Global_Objects/Array#instance_methods`
- MDN Object: `https://developer.mozilla.org/en-US/docs/Web/JavaScript/Reference/Global_Objects/Object`
- MDN Map: `https://developer.mozilla.org/en-US/docs/Web/JavaScript/Reference/Global_Objects/Map`
- MDN Set: `https://developer.mozilla.org/en-US/docs/Web/JavaScript/Reference/Global_Objects/Set`
- MDN WeakMap: `https://developer.mozilla.org/en-US/docs/Web/JavaScript/Reference/Global_Objects/WeakMap`
- MDN WeakSet: `https://developer.mozilla.org/en-US/docs/Web/JavaScript/Reference/Global_Objects/WeakSet`
- MDN Spread syntax: `https://developer.mozilla.org/en-US/docs/Web/JavaScript/Reference/Operators/Spread_syntax`
- ECMAScript specification: `https://tc39.es/ecma262/`

Interview safety line:

```text
For arrays and objects, I choose methods by intent, avoid accidental mutation, copy changed
state carefully, build indexes for repeated lookups, and keep transformations readable enough
for production maintenance.
```
