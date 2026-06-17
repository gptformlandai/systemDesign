# Java Collectors Terminal Operators Gold Sheet

> Goal: remove confusion around `collect(...)` and `Collectors`.

---

# 1. First Understand This

`collect(...)` is a terminal operation.

That means it ends the stream and produces a final result.

```java
List<String> names = employees.stream()
    .map(Employee::getName)
    .collect(Collectors.toList());
```

Flow:

```text
employees.stream()       -> source
map(Employee::getName)   -> intermediate operation
collect(toList())        -> terminal operation
```

Important:

```text
Collectors.toList() is not terminal by itself.
collect(...) is terminal.
Collectors.toList() tells collect how to collect.
```

---

# 2. Sample Data

Use this for all examples:

```java
record Employee(
    int id,
    String name,
    String department,
    int salary,
    List<String> skills
) {
}
```

```java
List<Employee> employees = List.of(
    new Employee(1, "Aravind", "Engineering", 120000, List.of("Java", "Spring")),
    new Employee(2, "Rahul", "Engineering", 150000, List.of("Java", "AWS")),
    new Employee(3, "Priya", "HR", 80000, List.of("Excel")),
    new Employee(4, "Sneha", "Engineering", 110000, List.of("React", "Java")),
    new Employee(5, "Vikram", "Finance", 130000, List.of("SQL", "Excel"))
);
```

---

# 3. `Collectors.toList()`

Use when you want final result as a `List`.

Question:

> Get all employee names.

```java
List<String> names = employees.stream()
    .map(Employee::name)
    .collect(Collectors.toList());
```

Output:

```text
[Aravind, Rahul, Priya, Sneha, Vikram]
```

Memory line:

```text
toList means collect stream elements into a List.
```

---

# 4. `Collectors.toSet()`

Use when you want unique values.

Question:

> Get unique departments.

```java
Set<String> departments = employees.stream()
    .map(Employee::department)
    .collect(Collectors.toSet());
```

Output:

```text
[Engineering, HR, Finance]
```

Note:

```text
Set removes duplicates, but order is not guaranteed.
```

If order matters:

```java
Set<String> departments = employees.stream()
    .map(Employee::department)
    .collect(Collectors.toCollection(LinkedHashSet::new));
```

---

# 5. `Collectors.toMap()`

Use when you want a `Map`.

Question:

> Convert employees to map by ID.

```java
Map<Integer, Employee> employeeById = employees.stream()
    .collect(Collectors.toMap(
        Employee::id,
        Function.identity()
    ));
```

Output idea:

```text
1 -> Aravind
2 -> Rahul
3 -> Priya
```

Meaning:

```text
key mapper   = Employee::id
value mapper = Function.identity()
```

---

# 6. `toMap()` With Duplicate Key

If duplicate keys come, normal `toMap` throws exception.

Example:

```java
Map<String, Employee> employeeByDepartment = employees.stream()
    .collect(Collectors.toMap(
        Employee::department,
        Function.identity()
    ));
```

Problem:

```text
Engineering appears multiple times.
This throws IllegalStateException.
```

Fix with merge function:

```java
Map<String, Employee> oneEmployeeByDepartment = employees.stream()
    .collect(Collectors.toMap(
        Employee::department,
        Function.identity(),
        (oldValue, newValue) -> oldValue
    ));
```

Meaning:

```text
If duplicate department comes, keep old employee.
```

Alternative:

```java
(oldValue, newValue) -> newValue
```

means keep latest.

---

# 7. `Collectors.groupingBy()`

Use when you want SQL-like `GROUP BY`.

Question:

> Group employees by department.

```java
Map<String, List<Employee>> employeesByDepartment = employees.stream()
    .collect(Collectors.groupingBy(Employee::department));
```

Output idea:

```text
Engineering -> [Aravind, Rahul, Sneha]
HR -> [Priya]
Finance -> [Vikram]
```

Memory line:

```text
groupingBy creates Map<groupKey, List<itemsInThatGroup>> by default.
```

---

# 8. `groupingBy()` With `counting()`

Question:

> Count employees by department.

```java
Map<String, Long> countByDepartment = employees.stream()
    .collect(Collectors.groupingBy(
        Employee::department,
        Collectors.counting()
    ));
```

Output:

```text
Engineering -> 3
HR -> 1
Finance -> 1
```

Pattern:

```text
groupingBy(key, counting())
```

---

# 9. `groupingBy()` With `mapping()`

Question:

> Get employee names by department.

```java
Map<String, List<String>> namesByDepartment = employees.stream()
    .collect(Collectors.groupingBy(
        Employee::department,
        Collectors.mapping(Employee::name, Collectors.toList())
    ));
```

Output:

```text
Engineering -> [Aravind, Rahul, Sneha]
HR -> [Priya]
Finance -> [Vikram]
```

Meaning:

```text
First group by department.
Inside each group, store only employee names.
```

---

# 10. `groupingBy()` With `averagingInt()`

Question:

> Find average salary by department.

```java
Map<String, Double> averageSalaryByDepartment = employees.stream()
    .collect(Collectors.groupingBy(
        Employee::department,
        Collectors.averagingInt(Employee::salary)
    ));
```

Output idea:

```text
Engineering -> 126666.67
HR -> 80000.0
Finance -> 130000.0
```

Other averaging collectors:

```java
Collectors.averagingInt(...)
Collectors.averagingLong(...)
Collectors.averagingDouble(...)
```

---

# 11. `groupingBy()` With `summingInt()`

Question:

> Find total salary by department.

```java
Map<String, Integer> totalSalaryByDepartment = employees.stream()
    .collect(Collectors.groupingBy(
        Employee::department,
        Collectors.summingInt(Employee::salary)
    ));
```

Output idea:

```text
Engineering -> 380000
HR -> 80000
Finance -> 130000
```

Other summing collectors:

```java
Collectors.summingInt(...)
Collectors.summingLong(...)
Collectors.summingDouble(...)
```

---

# 12. `groupingBy()` With `maxBy()`

Question:

> Find highest paid employee by department.

```java
Map<String, Optional<Employee>> highestPaidByDepartment = employees.stream()
    .collect(Collectors.groupingBy(
        Employee::department,
        Collectors.maxBy(Comparator.comparingInt(Employee::salary))
    ));
```

Output type has `Optional<Employee>` because a group may theoretically be empty.

Cleaner conversion:

```java
Map<String, Employee> highestPaidByDepartment = employees.stream()
    .collect(Collectors.groupingBy(
        Employee::department,
        Collectors.collectingAndThen(
            Collectors.maxBy(Comparator.comparingInt(Employee::salary)),
            Optional::get
        )
    ));
```

Interview note:

```text
maxBy returns Optional because it is a reduction operation.
```

---

# 13. `Collectors.partitioningBy()`

Use when you want two groups:

```text
true group
false group
```

Question:

> Split employees into high salary and non-high salary.

```java
Map<Boolean, List<Employee>> partitioned = employees.stream()
    .collect(Collectors.partitioningBy(emp -> emp.salary() >= 120000));
```

Output idea:

```text
true -> [Aravind, Rahul, Vikram]
false -> [Priya, Sneha]
```

Difference:

```text
groupingBy can create many groups.
partitioningBy always creates true/false groups.
```

---

# 14. `Collectors.joining()`

Use for joining strings.

Question:

> Get comma-separated employee names.

```java
String names = employees.stream()
    .map(Employee::name)
    .collect(Collectors.joining(", "));
```

Output:

```text
Aravind, Rahul, Priya, Sneha, Vikram
```

With prefix and suffix:

```java
String names = employees.stream()
    .map(Employee::name)
    .collect(Collectors.joining(", ", "[", "]"));
```

Output:

```text
[Aravind, Rahul, Priya, Sneha, Vikram]
```

---

# 15. `Collectors.counting()`

Use when you want count as collector.

Example with `groupingBy`:

```java
Map<String, Long> countByDepartment = employees.stream()
    .collect(Collectors.groupingBy(
        Employee::department,
        Collectors.counting()
    ));
```

If you only need total count, use terminal `count()`:

```java
long count = employees.stream()
    .filter(emp -> emp.salary() > 100000)
    .count();
```

Difference:

```text
stream.count() is terminal operation.
Collectors.counting() is a collector used inside collect.
```

---

# 16. `Collectors.collectingAndThen()`

Use when you want to collect first, then transform final result.

Question:

> Collect names into immutable list.

```java
List<String> names = employees.stream()
    .map(Employee::name)
    .collect(Collectors.collectingAndThen(
        Collectors.toList(),
        Collections::unmodifiableList
    ));
```

Meaning:

```text
First collect to list.
Then convert list to unmodifiable list.
```

---

# 17. `Collectors.toCollection()`

Use when you want a specific collection type.

Question:

> Collect departments into insertion-order set.

```java
Set<String> departments = employees.stream()
    .map(Employee::department)
    .collect(Collectors.toCollection(LinkedHashSet::new));
```

Why not just `toSet()`?

```text
toSet does not guarantee order.
toCollection lets us choose LinkedHashSet.
```

---

# 18. `flatMap` + `collect`

Question:

> Get all unique skills.

```java
Set<String> skills = employees.stream()
    .flatMap(emp -> emp.skills().stream())
    .collect(Collectors.toSet());
```

Output:

```text
[Java, Spring, AWS, Excel, React, SQL]
```

If sorted:

```java
List<String> sortedSkills = employees.stream()
    .flatMap(emp -> emp.skills().stream())
    .distinct()
    .sorted()
    .collect(Collectors.toList());
```

---

# 19. Most Important Patterns

## Pattern 1: List

```java
list.stream()
    .map(...)
    .collect(Collectors.toList());
```

## Pattern 2: Set

```java
list.stream()
    .map(...)
    .collect(Collectors.toSet());
```

## Pattern 3: Map

```java
list.stream()
    .collect(Collectors.toMap(
        keyMapper,
        valueMapper
    ));
```

## Pattern 4: Group

```java
list.stream()
    .collect(Collectors.groupingBy(
        classifier
    ));
```

## Pattern 5: Group + Count

```java
list.stream()
    .collect(Collectors.groupingBy(
        classifier,
        Collectors.counting()
    ));
```

## Pattern 6: Group + Average

```java
list.stream()
    .collect(Collectors.groupingBy(
        classifier,
        Collectors.averagingInt(valueExtractor)
    ));
```

## Pattern 7: Group + Names

```java
list.stream()
    .collect(Collectors.groupingBy(
        classifier,
        Collectors.mapping(valueExtractor, Collectors.toList())
    ));
```

---

# 20. Quick Interview Answers

## Q1. Is `Collectors.toList()` terminal?

No.

`collect(...)` is terminal.

`Collectors.toList()` is a collector passed to `collect`.

---

## Q2. What is `groupingBy`?

`groupingBy` collects stream elements into a `Map` based on a classifier function.

```java
Map<String, List<Employee>> map = employees.stream()
    .collect(Collectors.groupingBy(Employee::department));
```

---

## Q3. `groupingBy` vs `partitioningBy`?

```text
groupingBy can create many groups based on key.
partitioningBy creates only two groups: true and false.
```

---

## Q4. Why does `toMap` fail?

Because duplicate keys appeared and no merge function was provided.

Fix:

```java
Collectors.toMap(key, value, (oldValue, newValue) -> oldValue)
```

---

## Q5. When to use `mapping`?

Use `mapping` inside `groupingBy` when you want to store transformed values inside each group.

Example:

```java
Map<String, List<String>> namesByDepartment = employees.stream()
    .collect(Collectors.groupingBy(
        Employee::department,
        Collectors.mapping(Employee::name, Collectors.toList())
    ));
```

---

# 21. Final Memory Trick

Remember:

```text
collect decides the final container.
Collectors decides the collection style.
```

Examples:

```text
collect(toList())              -> List
collect(toSet())               -> Set
collect(toMap())               -> Map
collect(groupingBy())          -> Map<K, List<V>>
collect(groupingBy(counting())) -> Map<K, Long>
collect(joining())             -> String
```

---

# 22. Gold Layer: What Interviewers Really Test

They are usually not testing whether you memorized collector names.

They are testing whether you understand this shape:

```text
Stream<T>
    -> intermediate transformations
    -> terminal operation
    -> final result type
```

For collectors:

```text
collect(collector)
```

means:

```text
collect = terminal operation
collector = strategy for building final result
```

Strong line:

```text
collect is the terminal operation. Collectors.toList, groupingBy, toMap, and joining are
collector strategies passed into collect.
```

---

# 23. Beginner, Intermediate, Senior Levels

## Beginner Level

Know these:

| Need | Collector |
|---|---|
| List | `Collectors.toList()` |
| Set | `Collectors.toSet()` |
| Map | `Collectors.toMap()` |
| Group | `Collectors.groupingBy()` |
| Join strings | `Collectors.joining()` |

## Intermediate Level

Know these:

| Need | Pattern |
|---|---|
| Count by key | `groupingBy(key, counting())` |
| Names by department | `groupingBy(dept, mapping(name, toList()))` |
| Average by key | `groupingBy(key, averagingInt(...))` |
| Duplicate key handling | `toMap(key, value, mergeFunction)` |
| Preserve set order | `toCollection(LinkedHashSet::new)` |

## Senior Level

Know these:

- `Collectors.toList()` may return a mutable list, but do not design APIs around that assumption.
- `Stream.toList()` returns an unmodifiable list in modern Java.
- `toMap` throws on duplicate key unless a merge function is provided.
- `groupingByConcurrent` can help parallel grouping, but only when the downstream collector and data pattern fit.
- Avoid side effects inside stream pipelines.
- Use loops when a stream becomes unreadable.

---

# 24. `Collectors.toList()` vs `Stream.toList()`

Java 8 style:

```java
List<String> names = employees.stream()
    .map(Employee::name)
    .collect(Collectors.toList());
```

Modern Java style:

```java
List<String> names = employees.stream()
    .map(Employee::name)
    .toList();
```

Important difference:

```text
Stream.toList() returns an unmodifiable list.
Collectors.toList() does not guarantee a specific list implementation.
```

Interview answer:

```text
In Java 8 interviews, I use collect(Collectors.toList()). In modern Java, stream().toList()
is concise, but I remember it returns an unmodifiable list.
```

---

# 25. `groupingBy` vs `groupingByConcurrent`

Normal grouping:

```java
Map<String, List<Employee>> byDepartment = employees.stream()
    .collect(Collectors.groupingBy(Employee::department));
```

Concurrent grouping:

```java
ConcurrentMap<String, List<Employee>> byDepartment = employees.parallelStream()
    .collect(Collectors.groupingByConcurrent(Employee::department));
```

Senior caution:

```text
groupingByConcurrent is not automatically faster. It is useful mainly with parallel streams
and suitable concurrent downstream accumulation. Always measure before using it for performance.
```

---

# 26. Collector Decision Table

| Interview Requirement | Best Collector |
|---|---|
| Get all names | `map(...).collect(toList())` |
| Unique departments | `map(...).collect(toSet())` |
| Preserve unique department order | `toCollection(LinkedHashSet::new)` |
| ID to employee | `toMap(Employee::id, Function.identity())` |
| Duplicate key, keep first | `toMap(k, v, (oldVal, newVal) -> oldVal)` |
| Duplicate key, keep latest | `toMap(k, v, (oldVal, newVal) -> newVal)` |
| Group employees by department | `groupingBy(Employee::department)` |
| Count by department | `groupingBy(Employee::department, counting())` |
| Names by department | `groupingBy(dept, mapping(name, toList()))` |
| Average salary by department | `groupingBy(dept, averagingInt(salary))` |
| High salary vs others | `partitioningBy(predicate)` |
| Join names | `joining(", ")` |

---

# 27. Final Strong Answer

If interviewer asks:

> Explain collectors in Java streams.

Say:

```text
collect is a terminal stream operation. A Collector tells collect how to accumulate stream
elements into a final result, such as a List, Set, Map, grouped Map, partitioned Map, count,
average, or joined String. I use toMap carefully because duplicate keys throw unless I give
a merge function. For complex grouping, I use downstream collectors like counting, mapping,
averagingInt, maxBy, or collectingAndThen.
```

Memory trick:

```text
collect ends the stream.
Collectors shape the result.
```
