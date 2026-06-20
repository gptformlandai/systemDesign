# Java Streams Interview Prep

Target: Marriott Tech Accelerator / Intervue Java backend round.

This sheet focuses on medium-level Java Stream questions:
- Most asked concepts
- Standard operator chains
- Tricky interview traps
- Practical backend examples
- Code snippets you can speak through in a live round

Java version note:
- The JD says Java 8+.
- Prefer Java 8-safe syntax in interviews: `collect(Collectors.toList())`.
- If you use `stream().toList()`, mention that it is available in newer Java versions and returns an unmodifiable list.

---

## 1. Stream Mental Model

### What Is A Stream?

A Stream is a pipeline for processing data from a source like a `List`, `Set`, array, or file.

It does not store data.

It does not modify the original collection unless you explicitly mutate objects inside the stream, which is usually a bad practice.

### Interview Definition

```text
A Stream is a sequence of elements supporting functional-style operations like filter, map,
sort, reduce, and collect. It is lazy for intermediate operations and executes only when a
terminal operation is called.
```

### Pipeline Shape

```java
source
    .stream()
    .intermediateOperation()
    .intermediateOperation()
    .terminalOperation();
```

Example:

```java
List<String> names = Arrays.asList("Aravind", "Rahul", "Anil", "Kiran");

List<String> result = names.stream()
    .filter(name -> name.startsWith("A"))
    .map(String::toUpperCase)
    .collect(Collectors.toList());
```

Output:

```text
[ARAVIND, ANIL]
```

### Stream Operation Types

| Type | Examples | What It Does |
|---|---|---|
| Source | `list.stream()`, `Arrays.stream(arr)` | Creates stream |
| Intermediate | `filter`, `map`, `sorted`, `distinct`, `limit` | Builds pipeline, lazy |
| Terminal | `collect`, `forEach`, `count`, `reduce`, `findFirst` | Executes pipeline |

### Most Important Interview Line

```text
Intermediate operations are lazy. Nothing runs until a terminal operation is invoked.
```

---

## 2. Setup Model Used In Examples

Use this mental model for most examples.

```java
import java.util.*;
import java.util.function.*;
import java.util.stream.*;

class Employee {
    private final int id;
    private final String name;
    private final String department;
    private final int age;
    private final int salary;
    private final List<String> skills;

    Employee(int id, String name, String department, int age, int salary, List<String> skills) {
        this.id = id;
        this.name = name;
        this.department = department;
        this.age = age;
        this.salary = salary;
        this.skills = skills;
    }

    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getDepartment() {
        return department;
    }

    public int getAge() {
        return age;
    }

    public int getSalary() {
        return salary;
    }

    public List<String> getSkills() {
        return skills;
    }

    @Override
    public String toString() {
        return name + "(" + department + ", " + salary + ")";
    }
}
```

Sample data:

```java
List<Employee> employees = Arrays.asList(
    new Employee(1, "Aravind", "Engineering", 29, 120000, Arrays.asList("Java", "Spring", "Kafka")),
    new Employee(2, "Rahul", "Engineering", 32, 150000, Arrays.asList("Java", "AWS")),
    new Employee(3, "Priya", "HR", 27, 80000, Arrays.asList("Excel", "Hiring")),
    new Employee(4, "Sneha", "Engineering", 26, 110000, Arrays.asList("React", "JavaScript")),
    new Employee(5, "Vikram", "Finance", 35, 130000, Arrays.asList("SQL", "Excel")),
    new Employee(6, "Anil", "Engineering", 31, 150000, Arrays.asList("Java", "Spring"))
);
```

---

## 3. Group A: Filtering Chains

### Operator Focus

```java
filter()
```

### Standard Question 1: Find Employees With Salary Greater Than 120000

```java
List<Employee> highPaid = employees.stream()
    .filter(emp -> emp.getSalary() > 120000)
    .collect(Collectors.toList());
```

Expected result:

```text
[Rahul(Engineering, 150000), Vikram(Finance, 130000), Anil(Engineering, 150000)]
```

### Explanation

`filter` keeps only elements matching the predicate.

Predicate:

```java
emp -> emp.getSalary() > 120000
```

### Tricky Question: Multiple Conditions

Find Engineering employees with salary greater than 120000.

```java
List<Employee> result = employees.stream()
    .filter(emp -> "Engineering".equals(emp.getDepartment()))
    .filter(emp -> emp.getSalary() > 120000)
    .collect(Collectors.toList());
```

You can also write:

```java
List<Employee> result = employees.stream()
    .filter(emp -> "Engineering".equals(emp.getDepartment()) && emp.getSalary() > 120000)
    .collect(Collectors.toList());
```

### Which Is Better?

Both are correct.

For interview readability:
- Multiple `filter` calls are easier to read when conditions are independent.
- One combined predicate is fine for simple logic.

### Null-Safe String Check

Prefer:

```java
"Engineering".equals(emp.getDepartment())
```

Instead of:

```java
emp.getDepartment().equals("Engineering")
```

Because `emp.getDepartment()` may be null.

### Common Mistake

```java
employees.stream()
    .filter(emp -> emp.getSalary() > 120000);
```

This does nothing because there is no terminal operation.

Correct:

```java
List<Employee> result = employees.stream()
    .filter(emp -> emp.getSalary() > 120000)
    .collect(Collectors.toList());
```

---

## 4. Group B: Mapping Chains

### Operator Focus

```java
map()
```

### Standard Question 1: Get Employee Names

```java
List<String> names = employees.stream()
    .map(Employee::getName)
    .collect(Collectors.toList());
```

Expected result:

```text
[Aravind, Rahul, Priya, Sneha, Vikram, Anil]
```

### Explanation

`map` transforms each element.

Here:

```text
Employee -> String
```

### Standard Question 2: Convert Names To Uppercase

```java
List<String> names = employees.stream()
    .map(Employee::getName)
    .map(String::toUpperCase)
    .collect(Collectors.toList());
```

Expected result:

```text
[ARAVIND, RAHUL, PRIYA, SNEHA, VIKRAM, ANIL]
```

### Tricky Question: `map` vs `forEach`

Wrong style:

```java
List<String> names = new ArrayList<>();

employees.stream()
    .forEach(emp -> names.add(emp.getName()));
```

Better:

```java
List<String> names = employees.stream()
    .map(Employee::getName)
    .collect(Collectors.toList());
```

### Interview Answer

```text
map is for transformation. forEach is for side effects. In stream pipelines, we should
prefer map plus collect instead of mutating an external list inside forEach.
```

### Common Mistake

Using `map` when the function returns a collection creates nested structures.

```java
List<List<String>> skillLists = employees.stream()
    .map(Employee::getSkills)
    .collect(Collectors.toList());
```

Output shape:

```text
[[Java, Spring, Kafka], [Java, AWS], [Excel, Hiring], ...]
```

If you need all skills as one flat list, use `flatMap`.

---

## 5. Group C: FlatMap Chains

### Operator Focus

```java
flatMap()
```

### Standard Question: Get All Skills From All Employees

```java
List<String> allSkills = employees.stream()
    .flatMap(emp -> emp.getSkills().stream())
    .collect(Collectors.toList());
```

Expected result:

```text
[Java, Spring, Kafka, Java, AWS, Excel, Hiring, React, JavaScript, SQL, Excel, Java, Spring]
```

### Explanation

`flatMap` is used when each input element produces multiple output elements.

Here:

```text
Employee -> List<String> -> Stream<String>
```

Then `flatMap` flattens:

```text
Stream<List<String>> -> Stream<String>
```

### Standard Question: Unique Skills

```java
List<String> uniqueSkills = employees.stream()
    .flatMap(emp -> emp.getSkills().stream())
    .distinct()
    .collect(Collectors.toList());
```

Expected result:

```text
[Java, Spring, Kafka, AWS, Excel, Hiring, React, JavaScript, SQL]
```

### Tricky Question: Count Employees Who Know Java

```java
long count = employees.stream()
    .filter(emp -> emp.getSkills().contains("Java"))
    .count();
```

Do not use this:

```java
long count = employees.stream()
    .flatMap(emp -> emp.getSkills().stream())
    .filter(skill -> skill.equals("Java"))
    .count();
```

Why?
- The second chain counts Java skill occurrences.
- If one employee has duplicate `"Java"` skills, it counts more than one.
- It does not count employees.

### Common Mistake

Confusing these shapes:

```java
map:     Stream<Employee> -> Stream<List<String>>
flatMap: Stream<Employee> -> Stream<String>
```

### Same Mindset Examples

Flatten a list of lists:

```java
List<List<Integer>> numbers = Arrays.asList(
    Arrays.asList(1, 2),
    Arrays.asList(3, 4),
    Arrays.asList(5)
);

List<Integer> flat = numbers.stream()
    .flatMap(List::stream)
    .collect(Collectors.toList());
```

Output:

```text
[1, 2, 3, 4, 5]
```

---

## 6. Group D: Sorting Chains

### Operator Focus

```java
sorted()
Comparator.comparing()
reversed()
thenComparing()
```

### Standard Question 1: Sort Employees By Salary Ascending

```java
List<Employee> sorted = employees.stream()
    .sorted(Comparator.comparingInt(Employee::getSalary))
    .collect(Collectors.toList());
```

### Standard Question 2: Sort Employees By Salary Descending

```java
List<Employee> sorted = employees.stream()
    .sorted(Comparator.comparingInt(Employee::getSalary).reversed())
    .collect(Collectors.toList());
```

### Standard Question 3: Sort By Department, Then Salary Descending

```java
List<Employee> sorted = employees.stream()
    .sorted(
        Comparator.comparing(Employee::getDepartment)
            .thenComparing(Comparator.comparingInt(Employee::getSalary).reversed())
    )
    .collect(Collectors.toList());
```

### Explanation

Sort order:
1. Department ascending.
2. Within same department, salary descending.

### Tricky Question: Sort By Salary Descending, Then Name Ascending

```java
List<Employee> sorted = employees.stream()
    .sorted(
        Comparator.comparingInt(Employee::getSalary)
            .reversed()
            .thenComparing(Employee::getName)
    )
    .collect(Collectors.toList());
```

### Important Trap With `reversed`

This:

```java
Comparator.comparing(Employee::getDepartment)
    .thenComparing(Employee::getName)
    .reversed()
```

Reverses the full comparator, not just name.

If you need only one field reversed, reverse only that comparator:

```java
Comparator.comparing(Employee::getDepartment)
    .thenComparing(Comparator.comparing(Employee::getName).reversed())
```

### Null-Safe Sorting

If department may be null:

```java
List<Employee> sorted = employees.stream()
    .sorted(
        Comparator.comparing(
            Employee::getDepartment,
            Comparator.nullsLast(String::compareTo)
        )
    )
    .collect(Collectors.toList());
```

### Common Mistake

```java
employees.stream()
    .sorted()
    .collect(Collectors.toList());
```

This works only if elements implement `Comparable`.

For custom objects like `Employee`, provide a comparator.

---

## 7. Group E: Distinct, Limit, Skip

### Operator Focus

```java
distinct()
limit()
skip()
```

### Standard Question 1: Get Unique Departments

```java
List<String> departments = employees.stream()
    .map(Employee::getDepartment)
    .distinct()
    .collect(Collectors.toList());
```

Expected result:

```text
[Engineering, HR, Finance]
```

### Standard Question 2: Top 3 Highest Paid Employees

```java
List<Employee> top3 = employees.stream()
    .sorted(Comparator.comparingInt(Employee::getSalary).reversed())
    .limit(3)
    .collect(Collectors.toList());
```

### Standard Question 3: Second Highest Salary

```java
Optional<Integer> secondHighestSalary = employees.stream()
    .map(Employee::getSalary)
    .distinct()
    .sorted(Comparator.reverseOrder())
    .skip(1)
    .findFirst();
```

Expected result:

```text
Optional[130000]
```

Why `distinct` matters:
- Salary `150000` appears twice.
- Without `distinct`, skipping one value would still return `150000`.

### Tricky Question: Third Highest Salary

```java
Optional<Integer> thirdHighestSalary = employees.stream()
    .map(Employee::getSalary)
    .distinct()
    .sorted(Comparator.reverseOrder())
    .skip(2)
    .findFirst();
```

### Common Mistake

Wrong:

```java
employees.stream()
    .sorted(Comparator.comparingInt(Employee::getSalary).reversed())
    .skip(1)
    .findFirst();
```

This returns the second employee after sorting, not the second distinct salary.

### Distinct On Custom Objects

`distinct()` depends on `equals()` and `hashCode()`.

If `Employee` does not override `equals` and `hashCode`, `distinct()` compares object references.

For distinct by employee ID:

```java
List<Employee> distinctById = employees.stream()
    .collect(Collectors.toMap(
        Employee::getId,
        Function.identity(),
        (existing, duplicate) -> existing,
        LinkedHashMap::new
    ))
    .values()
    .stream()
    .collect(Collectors.toList());
```

### Interview Answer

```text
distinct uses equals and hashCode. For custom distinct by a field, I use toMap with a
merge function or a helper predicate backed by a Set.
```

---

## 8. Group F: Matching And Finding

### Operator Focus

```java
anyMatch()
allMatch()
noneMatch()
findFirst()
findAny()
```

### Standard Question 1: Does Any Employee Know Kafka?

```java
boolean hasKafka = employees.stream()
    .anyMatch(emp -> emp.getSkills().contains("Kafka"));
```

### Standard Question 2: Do All Employees Have Salary Greater Than 50000?

```java
boolean allAbove50k = employees.stream()
    .allMatch(emp -> emp.getSalary() > 50000);
```

### Standard Question 3: No Employee Is Under 18

```java
boolean noMinor = employees.stream()
    .noneMatch(emp -> emp.getAge() < 18);
```

### Standard Question 4: Find First Engineering Employee

```java
Optional<Employee> firstEngineering = employees.stream()
    .filter(emp -> "Engineering".equals(emp.getDepartment()))
    .findFirst();
```

### `findFirst` vs `findAny`

| Method | Meaning |
|---|---|
| `findFirst` | Returns first element in encounter order |
| `findAny` | Returns any element, useful in parallel streams |

### Tricky Question: Short-Circuiting

These operations can stop early:

```java
anyMatch()
allMatch()
noneMatch()
findFirst()
findAny()
limit()
```

Example:

```java
boolean result = employees.stream()
    .peek(emp -> System.out.println("Checking " + emp.getName()))
    .anyMatch(emp -> emp.getSalary() > 140000);
```

Once an employee with salary greater than `140000` is found, processing stops.

### Common Mistake

Wrong:

```java
Employee emp = employees.stream()
    .filter(e -> e.getId() == 10)
    .findFirst()
    .get();
```

If no employee exists, `.get()` throws `NoSuchElementException`.

Better:

```java
Optional<Employee> emp = employees.stream()
    .filter(e -> e.getId() == 10)
    .findFirst();
```

Or:

```java
Employee emp = employees.stream()
    .filter(e -> e.getId() == 10)
    .findFirst()
    .orElseThrow(() -> new IllegalArgumentException("Employee not found"));
```

---

## 9. Group G: Reduce Chains

### Operator Focus

```java
reduce()
```

### Standard Question 1: Sum Salaries Using Reduce

```java
int totalSalary = employees.stream()
    .map(Employee::getSalary)
    .reduce(0, Integer::sum);
```

### Better For Numbers

```java
int totalSalary = employees.stream()
    .mapToInt(Employee::getSalary)
    .sum();
```

### Interview Answer

```text
reduce combines stream elements into a single result. For numeric sum, primitive streams
like mapToInt plus sum are simpler and avoid boxing.
```

### Standard Question 2: Find Max Salary Using Reduce

```java
Optional<Integer> maxSalary = employees.stream()
    .map(Employee::getSalary)
    .reduce(Integer::max);
```

### Standard Question 3: Concatenate Names

```java
String names = employees.stream()
    .map(Employee::getName)
    .reduce("", (a, b) -> a.isEmpty() ? b : a + "," + b);
```

Better:

```java
String names = employees.stream()
    .map(Employee::getName)
    .collect(Collectors.joining(","));
```

### Tricky Question: Identity Value

```java
int sum = numbers.stream()
    .reduce(0, Integer::sum);
```

Here `0` is identity because:

```text
0 + x = x
```

For multiplication:

```java
int product = numbers.stream()
    .reduce(1, (a, b) -> a * b);
```

Identity is `1` because:

```text
1 * x = x
```

### Common Mistake

Wrong identity:

```java
int product = numbers.stream()
    .reduce(0, (a, b) -> a * b);
```

This always returns `0`.

### Reduce vs Collect

| Use `reduce` | Use `collect` |
|---|---|
| Combining into one immutable value | Building mutable result container |
| Sum, max, product | List, Map, Set, StringBuilder |

---

## 10. Group H: Collectors Basic Chains

### Operator Focus

```java
collect()
Collectors.toList()
Collectors.toSet()
Collectors.joining()
Collectors.counting()
```

### Standard Question 1: Collect Names Into List

```java
List<String> names = employees.stream()
    .map(Employee::getName)
    .collect(Collectors.toList());
```

### Standard Question 2: Collect Departments Into Set

```java
Set<String> departments = employees.stream()
    .map(Employee::getDepartment)
    .collect(Collectors.toSet());
```

### Standard Question 3: Join Names With Comma

```java
String names = employees.stream()
    .map(Employee::getName)
    .collect(Collectors.joining(", "));
```

Output:

```text
Aravind, Rahul, Priya, Sneha, Vikram, Anil
```

### Standard Question 4: Join Names With Prefix And Suffix

```java
String names = employees.stream()
    .map(Employee::getName)
    .collect(Collectors.joining(", ", "[", "]"));
```

Output:

```text
[Aravind, Rahul, Priya, Sneha, Vikram, Anil]
```

### Tricky Question: `toList()` Differences

Java 8:

```java
List<String> names = employees.stream()
    .map(Employee::getName)
    .collect(Collectors.toList());
```

Newer Java:

```java
List<String> names = employees.stream()
    .map(Employee::getName)
    .toList();
```

Key difference:
- `Collectors.toList()` does not guarantee mutability, but usually returns mutable `ArrayList`.
- `Stream.toList()` returns an unmodifiable list.

Interview-safe answer:

```text
For Java 8 compatibility, I use collect(Collectors.toList()). If I need a guaranteed
mutable list, I collect with Collectors.toCollection(ArrayList::new).
```

Guaranteed mutable:

```java
List<String> names = employees.stream()
    .map(Employee::getName)
    .collect(Collectors.toCollection(ArrayList::new));
```

---

## 11. Group I: GroupingBy Chains

### Operator Focus

```java
Collectors.groupingBy()
```

### Standard Question 1: Group Employees By Department

```java
Map<String, List<Employee>> byDepartment = employees.stream()
    .collect(Collectors.groupingBy(Employee::getDepartment));
```

Output shape:

```text
Engineering -> [Aravind, Rahul, Sneha, Anil]
HR -> [Priya]
Finance -> [Vikram]
```

### Standard Question 2: Count Employees By Department

```java
Map<String, Long> countByDepartment = employees.stream()
    .collect(Collectors.groupingBy(
        Employee::getDepartment,
        Collectors.counting()
    ));
```

Output:

```text
Engineering -> 4
HR -> 1
Finance -> 1
```

### Standard Question 3: Names By Department

```java
Map<String, List<String>> namesByDepartment = employees.stream()
    .collect(Collectors.groupingBy(
        Employee::getDepartment,
        Collectors.mapping(Employee::getName, Collectors.toList())
    ));
```

Output shape:

```text
Engineering -> [Aravind, Rahul, Sneha, Anil]
HR -> [Priya]
Finance -> [Vikram]
```

### Standard Question 4: Average Salary By Department

```java
Map<String, Double> avgSalaryByDepartment = employees.stream()
    .collect(Collectors.groupingBy(
        Employee::getDepartment,
        Collectors.averagingInt(Employee::getSalary)
    ));
```

### Standard Question 5: Max Salary Employee By Department

```java
Map<String, Optional<Employee>> maxSalaryByDepartment = employees.stream()
    .collect(Collectors.groupingBy(
        Employee::getDepartment,
        Collectors.maxBy(Comparator.comparingInt(Employee::getSalary))
    ));
```

Output type has `Optional<Employee>` because a group could theoretically be empty.

### Cleaner Max Employee By Department

```java
Map<String, Employee> maxSalaryByDepartment = employees.stream()
    .collect(Collectors.groupingBy(
        Employee::getDepartment,
        Collectors.collectingAndThen(
            Collectors.maxBy(Comparator.comparingInt(Employee::getSalary)),
            Optional::get
        )
    ));
```

Use this only if every group is guaranteed non-empty, which is true for groups created from stream elements.

### Tricky Question: Group By Department, Then Age

```java
Map<String, Map<Integer, List<Employee>>> byDepartmentAndAge = employees.stream()
    .collect(Collectors.groupingBy(
        Employee::getDepartment,
        Collectors.groupingBy(Employee::getAge)
    ));
```

### Tricky Question: Preserve Group Insertion Order

```java
Map<String, List<Employee>> byDepartment = employees.stream()
    .collect(Collectors.groupingBy(
        Employee::getDepartment,
        LinkedHashMap::new,
        Collectors.toList()
    ));
```

### Common Mistake

Wrong expectation:

```java
Collectors.groupingBy(Employee::getDepartment)
```

does not guarantee sorted order.

If you need sorted keys:

```java
Map<String, List<Employee>> byDepartment = employees.stream()
    .collect(Collectors.groupingBy(
        Employee::getDepartment,
        TreeMap::new,
        Collectors.toList()
    ));
```

---

## 12. Group J: PartitioningBy Chains

### Operator Focus

```java
Collectors.partitioningBy()
```

### Standard Question: Split Employees Into High Paid And Others

```java
Map<Boolean, List<Employee>> partitioned = employees.stream()
    .collect(Collectors.partitioningBy(emp -> emp.getSalary() > 120000));
```

Output shape:

```text
true -> employees with salary > 120000
false -> employees with salary <= 120000
```

### GroupingBy vs PartitioningBy

| Feature | groupingBy | partitioningBy |
|---|---|---|
| Key type | Any key | Boolean only |
| Number of groups | Many | Always true/false |
| Use case | Group by department, age, status | Split by condition |

### Tricky Question: Count High Paid And Low Paid

```java
Map<Boolean, Long> count = employees.stream()
    .collect(Collectors.partitioningBy(
        emp -> emp.getSalary() > 120000,
        Collectors.counting()
    ));
```

Output:

```text
true -> 3
false -> 3
```

### Common Mistake

Using `groupingBy` for a boolean condition:

```java
Map<Boolean, List<Employee>> result = employees.stream()
    .collect(Collectors.groupingBy(emp -> emp.getSalary() > 120000));
```

This works, but `partitioningBy` communicates intent better.

---

## 13. Group K: ToMap Chains

### Operator Focus

```java
Collectors.toMap()
```

### Standard Question 1: Convert List Of Employees To Map By ID

```java
Map<Integer, Employee> byId = employees.stream()
    .collect(Collectors.toMap(
        Employee::getId,
        Function.identity()
    ));
```

### Standard Question 2: Employee ID To Name

```java
Map<Integer, String> idToName = employees.stream()
    .collect(Collectors.toMap(
        Employee::getId,
        Employee::getName
    ));
```

### Tricky Question: Duplicate Keys

This throws `IllegalStateException` if duplicate key exists:

```java
Map<Integer, Employee> byId = employees.stream()
    .collect(Collectors.toMap(Employee::getId, Function.identity()));
```

Safe version:

```java
Map<Integer, Employee> byId = employees.stream()
    .collect(Collectors.toMap(
        Employee::getId,
        Function.identity(),
        (existing, duplicate) -> existing
    ));
```

### Standard Question 3: Department To Highest Paid Employee

```java
Map<String, Employee> highestPaidByDepartment = employees.stream()
    .collect(Collectors.toMap(
        Employee::getDepartment,
        Function.identity(),
        (e1, e2) -> e1.getSalary() >= e2.getSalary() ? e1 : e2
    ));
```

### Preserve Order With ToMap

```java
Map<Integer, String> idToName = employees.stream()
    .collect(Collectors.toMap(
        Employee::getId,
        Employee::getName,
        (a, b) -> a,
        LinkedHashMap::new
    ));
```

### Common Mistake

Wrong:

```java
Collectors.toMap(Employee::getDepartment, Employee::getName)
```

If multiple employees are in the same department, this fails due to duplicate department key.

Correct if you need names by department:

```java
Map<String, List<String>> namesByDepartment = employees.stream()
    .collect(Collectors.groupingBy(
        Employee::getDepartment,
        Collectors.mapping(Employee::getName, Collectors.toList())
    ));
```

---

## 14. Group L: Numeric Streams

### Operator Focus

```java
mapToInt()
mapToLong()
mapToDouble()
sum()
average()
max()
min()
summaryStatistics()
```

### Standard Question 1: Total Salary

```java
int totalSalary = employees.stream()
    .mapToInt(Employee::getSalary)
    .sum();
```

### Standard Question 2: Average Salary

```java
OptionalDouble averageSalary = employees.stream()
    .mapToInt(Employee::getSalary)
    .average();
```

Handle it:

```java
double avg = employees.stream()
    .mapToInt(Employee::getSalary)
    .average()
    .orElse(0.0);
```

### Standard Question 3: Max Salary

```java
OptionalInt maxSalary = employees.stream()
    .mapToInt(Employee::getSalary)
    .max();
```

### Standard Question 4: Salary Statistics

```java
IntSummaryStatistics stats = employees.stream()
    .mapToInt(Employee::getSalary)
    .summaryStatistics();

System.out.println(stats.getCount());
System.out.println(stats.getSum());
System.out.println(stats.getMin());
System.out.println(stats.getMax());
System.out.println(stats.getAverage());
```

### Why Primitive Streams Matter

This causes boxing/unboxing:

```java
Integer total = employees.stream()
    .map(Employee::getSalary)
    .reduce(0, Integer::sum);
```

Better:

```java
int total = employees.stream()
    .mapToInt(Employee::getSalary)
    .sum();
```

### Interview Answer

```text
For numeric calculations, I prefer primitive streams like IntStream because they avoid
boxing and provide direct operations like sum, average, min, max, and summaryStatistics.
```

---

## 15. Group M: Optional With Streams

### Operator Focus

```java
Optional
orElse()
orElseGet()
orElseThrow()
ifPresent()
```

### Standard Question: Find Employee By ID

```java
Optional<Employee> employee = employees.stream()
    .filter(emp -> emp.getId() == 3)
    .findFirst();
```

### Handle Optional Safely

```java
Employee employee = employees.stream()
    .filter(emp -> emp.getId() == 3)
    .findFirst()
    .orElseThrow(() -> new IllegalArgumentException("Employee not found"));
```

### `orElse` vs `orElseGet`

```java
Employee employee = optionalEmployee.orElse(createDefaultEmployee());
```

`createDefaultEmployee()` runs even if optional has a value.

Better when default creation is expensive:

```java
Employee employee = optionalEmployee.orElseGet(() -> createDefaultEmployee());
```

`orElseGet` runs supplier only if optional is empty.

### Common Mistake

Avoid:

```java
optional.get()
```

Unless you already checked:

```java
if (optional.isPresent()) {
    Employee employee = optional.get();
}
```

Better:

```java
optional.ifPresent(emp -> System.out.println(emp.getName()));
```

---

## 16. Group N: Peek, Laziness, And Debugging

### Operator Focus

```java
peek()
```

### Standard Question: What Will This Print?

```java
employees.stream()
    .filter(emp -> emp.getSalary() > 120000)
    .peek(emp -> System.out.println(emp.getName()));
```

Answer:

```text
Nothing.
```

Why?
- No terminal operation.
- Stream pipeline is lazy.

Correct:

```java
List<Employee> result = employees.stream()
    .filter(emp -> emp.getSalary() > 120000)
    .peek(emp -> System.out.println(emp.getName()))
    .collect(Collectors.toList());
```

### Interview Answer

```text
peek is mainly for debugging intermediate stream values. It should not be used for business
side effects because it depends on stream execution and can behave unexpectedly with short-circuiting or parallel streams.
```

### Tricky Short-Circuit Example

```java
boolean found = employees.stream()
    .peek(emp -> System.out.println("Checking " + emp.getName()))
    .anyMatch(emp -> emp.getSalary() > 140000);
```

This may not print all employees because `anyMatch` stops once a match is found.

---

## 17. Group O: Parallel Streams

### Operator Focus

```java
parallelStream()
```

### Standard Question: What Is Parallel Stream?

```java
long count = employees.parallelStream()
    .filter(emp -> emp.getSalary() > 100000)
    .count();
```

### Interview Answer

```text
parallelStream splits work across multiple threads using the common ForkJoinPool.
It can help for CPU-heavy, independent operations on large datasets, but it can hurt
performance for small data, blocking IO, shared mutable state, or order-sensitive logic.
```

### Good Use Case

```text
Large in-memory list + CPU-heavy independent transformation.
```

### Bad Use Cases

```text
Database calls inside stream
HTTP calls inside stream
Mutating shared list
Order-dependent processing
Small collections
```

### Common Mistake: Shared Mutable State

Wrong:

```java
List<String> names = new ArrayList<>();

employees.parallelStream()
    .forEach(emp -> names.add(emp.getName()));
```

This is unsafe because multiple threads mutate the same `ArrayList`.

Correct:

```java
List<String> names = employees.parallelStream()
    .map(Employee::getName)
    .collect(Collectors.toList());
```

### `forEach` vs `forEachOrdered`

```java
employees.parallelStream()
    .forEach(emp -> System.out.println(emp.getName()));
```

Order is not guaranteed.

```java
employees.parallelStream()
    .forEachOrdered(emp -> System.out.println(emp.getName()));
```

Preserves encounter order, but may reduce parallel benefit.

---

## 18. Group P: Null Handling

### Standard Question: Remove Null Values

```java
List<String> names = Arrays.asList("A", null, "B", null, "C");

List<String> result = names.stream()
    .filter(Objects::nonNull)
    .collect(Collectors.toList());
```

Output:

```text
[A, B, C]
```

### Standard Question: Null-Safe Mapping

If employee name can be null:

```java
List<String> names = employees.stream()
    .map(Employee::getName)
    .filter(Objects::nonNull)
    .collect(Collectors.toList());
```

### Tricky Question: Stream From Nullable Collection

```java
List<Employee> employees = null;
```

Java 8-safe:

```java
List<Employee> safeList = employees == null ? Collections.emptyList() : employees;

List<String> names = safeList.stream()
    .map(Employee::getName)
    .collect(Collectors.toList());
```

Java 9+:

```java
List<String> names = Stream.ofNullable(employees)
    .flatMap(Collection::stream)
    .map(Employee::getName)
    .collect(Collectors.toList());
```

### Common Mistake

```java
employees.stream()
```

If `employees` is null, this throws `NullPointerException`.

Streams help process collections; they do not make the source object null-safe by default.

---

## 19. Group Q: Most Asked Operator Chains

### Chain 1: Filter + Map + Collect

Question:

```text
Get names of Engineering employees.
```

```java
List<String> names = employees.stream()
    .filter(emp -> "Engineering".equals(emp.getDepartment()))
    .map(Employee::getName)
    .collect(Collectors.toList());
```

### Chain 2: Filter + Sorted + Limit

Question:

```text
Get top 3 highest paid Engineering employees.
```

```java
List<Employee> top3Engineering = employees.stream()
    .filter(emp -> "Engineering".equals(emp.getDepartment()))
    .sorted(Comparator.comparingInt(Employee::getSalary).reversed())
    .limit(3)
    .collect(Collectors.toList());
```

### Chain 3: Map + Distinct + Sorted

Question:

```text
Get sorted unique departments.
```

```java
List<String> departments = employees.stream()
    .map(Employee::getDepartment)
    .distinct()
    .sorted()
    .collect(Collectors.toList());
```

### Chain 4: FlatMap + Distinct + Sorted

Question:

```text
Get sorted unique skills.
```

```java
List<String> skills = employees.stream()
    .flatMap(emp -> emp.getSkills().stream())
    .distinct()
    .sorted()
    .collect(Collectors.toList());
```

### Chain 5: GroupingBy + Counting

Question:

```text
Count employees in each department.
```

```java
Map<String, Long> countByDepartment = employees.stream()
    .collect(Collectors.groupingBy(
        Employee::getDepartment,
        Collectors.counting()
    ));
```

### Chain 6: GroupingBy + Mapping

Question:

```text
Get employee names by department.
```

```java
Map<String, List<String>> namesByDepartment = employees.stream()
    .collect(Collectors.groupingBy(
        Employee::getDepartment,
        Collectors.mapping(Employee::getName, Collectors.toList())
    ));
```

### Chain 7: GroupingBy + Averaging

Question:

```text
Average salary by department.
```

```java
Map<String, Double> avgSalaryByDepartment = employees.stream()
    .collect(Collectors.groupingBy(
        Employee::getDepartment,
        Collectors.averagingInt(Employee::getSalary)
    ));
```

### Chain 8: ToMap With Merge

Question:

```text
Get highest paid employee per department.
```

```java
Map<String, Employee> highestPaidByDepartment = employees.stream()
    .collect(Collectors.toMap(
        Employee::getDepartment,
        Function.identity(),
        (e1, e2) -> e1.getSalary() >= e2.getSalary() ? e1 : e2
    ));
```

### Chain 9: Second Highest Distinct Salary

Question:

```text
Find the second highest distinct salary.
```

```java
Optional<Integer> secondHighest = employees.stream()
    .map(Employee::getSalary)
    .distinct()
    .sorted(Comparator.reverseOrder())
    .skip(1)
    .findFirst();
```

### Chain 10: Employee With Second Highest Distinct Salary

Question:

```text
Find employees having the second highest distinct salary.
```

```java
Optional<Integer> secondHighestSalary = employees.stream()
    .map(Employee::getSalary)
    .distinct()
    .sorted(Comparator.reverseOrder())
    .skip(1)
    .findFirst();

List<Employee> result = secondHighestSalary
    .map(salary -> employees.stream()
        .filter(emp -> emp.getSalary() == salary)
        .collect(Collectors.toList()))
    .orElse(Collections.emptyList());
```

### Chain 11: Max Salary Employee

```java
Optional<Employee> maxSalaryEmployee = employees.stream()
    .max(Comparator.comparingInt(Employee::getSalary));
```

### Chain 12: Min Salary Employee

```java
Optional<Employee> minSalaryEmployee = employees.stream()
    .min(Comparator.comparingInt(Employee::getSalary));
```

### Chain 13: Count Frequency Of Words

```java
List<String> words = Arrays.asList("java", "spring", "java", "kafka", "spring", "java");

Map<String, Long> frequency = words.stream()
    .collect(Collectors.groupingBy(
        Function.identity(),
        Collectors.counting()
    ));
```

Output:

```text
java -> 3
spring -> 2
kafka -> 1
```

### Chain 14: Find Duplicate Words

```java
List<String> duplicates = words.stream()
    .collect(Collectors.groupingBy(
        Function.identity(),
        Collectors.counting()
    ))
    .entrySet()
    .stream()
    .filter(entry -> entry.getValue() > 1)
    .map(Map.Entry::getKey)
    .collect(Collectors.toList());
```

Output:

```text
[java, spring]
```

### Chain 15: Character Frequency In A String

```java
String input = "programming";

Map<Character, Long> frequency = input.chars()
    .mapToObj(ch -> (char) ch)
    .collect(Collectors.groupingBy(
        Function.identity(),
        LinkedHashMap::new,
        Collectors.counting()
    ));
```

Output shape:

```text
p -> 1
r -> 2
o -> 1
g -> 2
...
```

### Chain 16: First Non-Repeating Character

```java
String input = "programming";

Map<Character, Long> frequency = input.chars()
    .mapToObj(ch -> (char) ch)
    .collect(Collectors.groupingBy(
        Function.identity(),
        LinkedHashMap::new,
        Collectors.counting()
    ));

Optional<Character> firstNonRepeating = frequency.entrySet()
    .stream()
    .filter(entry -> entry.getValue() == 1)
    .map(Map.Entry::getKey)
    .findFirst();
```

Expected:

```text
p
```

### Chain 17: Sort Map By Value Descending

```java
Map<String, Long> frequency = new HashMap<>();

Map<String, Long> sorted = frequency.entrySet()
    .stream()
    .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
    .collect(Collectors.toMap(
        Map.Entry::getKey,
        Map.Entry::getValue,
        (a, b) -> a,
        LinkedHashMap::new
    ));
```

### Chain 18: Top 2 Frequent Words

```java
List<String> top2 = words.stream()
    .collect(Collectors.groupingBy(
        Function.identity(),
        Collectors.counting()
    ))
    .entrySet()
    .stream()
    .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
    .limit(2)
    .map(Map.Entry::getKey)
    .collect(Collectors.toList());
```

### Chain 19: Sum Salary By Department

```java
Map<String, Integer> salaryByDepartment = employees.stream()
    .collect(Collectors.groupingBy(
        Employee::getDepartment,
        Collectors.summingInt(Employee::getSalary)
    ));
```

### Chain 20: Department With Highest Total Salary

```java
Optional<Map.Entry<String, Integer>> highestSalaryDepartment = employees.stream()
    .collect(Collectors.groupingBy(
        Employee::getDepartment,
        Collectors.summingInt(Employee::getSalary)
    ))
    .entrySet()
    .stream()
    .max(Map.Entry.comparingByValue());
```

---

## 20. Group R: Tricky Interview Questions

### Question 1: Can A Stream Be Reused?

No.

Wrong:

```java
Stream<String> stream = names.stream();

long count = stream.count();
List<String> list = stream.collect(Collectors.toList());
```

This throws:

```text
IllegalStateException: stream has already been operated upon or closed
```

Correct:

```java
long count = names.stream().count();
List<String> list = names.stream().collect(Collectors.toList());
```

Interview answer:

```text
A stream is single-use. Once a terminal operation runs, the stream is consumed.
Create a new stream from the source if needed.
```

### Question 2: Are Streams Lazy?

Yes, intermediate operations are lazy.

```java
List<String> names = Arrays.asList("A", "B", "C");

Stream<String> stream = names.stream()
    .filter(name -> {
        System.out.println("Filtering " + name);
        return true;
    });
```

Output:

```text
No output yet
```

Add terminal operation:

```java
stream.collect(Collectors.toList());
```

Now it prints.

### Question 3: `map` vs `flatMap`

```text
map transforms one element into one result.
flatMap transforms one element into a stream of results and flattens all streams into one.
```

Example:

```java
List<List<Integer>> data = Arrays.asList(
    Arrays.asList(1, 2),
    Arrays.asList(3, 4)
);

List<List<Integer>> mapped = data.stream()
    .map(list -> list)
    .collect(Collectors.toList());

List<Integer> flattened = data.stream()
    .flatMap(List::stream)
    .collect(Collectors.toList());
```

### Question 4: `findFirst` vs `findAny`

```text
findFirst respects encounter order.
findAny may return any element and can be faster in parallel streams.
```

### Question 5: `limit` vs `skip`

```java
List<Integer> result = Arrays.asList(1, 2, 3, 4, 5)
    .stream()
    .skip(2)
    .limit(2)
    .collect(Collectors.toList());
```

Output:

```text
[3, 4]
```

### Question 6: Why Avoid Side Effects?

Bad:

```java
List<String> result = new ArrayList<>();

employees.stream()
    .filter(emp -> emp.getSalary() > 100000)
    .forEach(emp -> result.add(emp.getName()));
```

Better:

```java
List<String> result = employees.stream()
    .filter(emp -> emp.getSalary() > 100000)
    .map(Employee::getName)
    .collect(Collectors.toList());
```

Interview answer:

```text
Side effects reduce readability and can break badly with parallel streams.
Streams are intended for declarative transformations.
```

### Question 7: What Is Encounter Order?

Encounter order is the order in which stream sees elements from the source.

Examples:
- `List` has encounter order.
- `LinkedHashSet` has insertion order.
- `HashSet` does not guarantee predictable order.

### Question 8: Does `filter` Change The Original List?

No.

```java
List<Employee> result = employees.stream()
    .filter(emp -> emp.getSalary() > 120000)
    .collect(Collectors.toList());
```

Original list remains unchanged.

### Question 9: Difference Between Intermediate And Terminal Operations

Intermediate:

```text
filter, map, flatMap, sorted, distinct, limit, skip, peek
```

Terminal:

```text
collect, count, forEach, reduce, findFirst, anyMatch, allMatch, noneMatch, min, max
```

### Question 10: Stateful vs Stateless Operations

Stateless:

```text
filter, map
```

They process each element independently.

Stateful:

```text
sorted, distinct, limit, skip
```

They may need information about other elements.

Interview answer:

```text
Stateful operations can be more expensive because they may need to buffer or track elements.
For example, sorted needs to see all elements before producing sorted output.
```

---

## 21. Group S: Backend-Flavored Stream Examples

### Example 1: Convert Entity List To DTO List

```java
class EmployeeDto {
    private final int id;
    private final String name;
    private final String department;

    EmployeeDto(int id, String name, String department) {
        this.id = id;
        this.name = name;
        this.department = department;
    }
}
```

```java
List<EmployeeDto> dtos = employees.stream()
    .map(emp -> new EmployeeDto(emp.getId(), emp.getName(), emp.getDepartment()))
    .collect(Collectors.toList());
```

### Example 2: Filter Active API Responses

```java
List<EmployeeDto> dtos = employees.stream()
    .filter(emp -> "Engineering".equals(emp.getDepartment()))
    .map(emp -> new EmployeeDto(emp.getId(), emp.getName(), emp.getDepartment()))
    .collect(Collectors.toList());
```

### Example 3: Build ID Lookup Map For Service Logic

```java
Map<Integer, Employee> employeeById = employees.stream()
    .collect(Collectors.toMap(
        Employee::getId,
        Function.identity()
    ));
```

### Example 4: Validate Request IDs Exist

```java
List<Integer> requestedIds = Arrays.asList(1, 2, 10);

Set<Integer> existingIds = employees.stream()
    .map(Employee::getId)
    .collect(Collectors.toSet());

List<Integer> missingIds = requestedIds.stream()
    .filter(id -> !existingIds.contains(id))
    .collect(Collectors.toList());
```

### Example 5: Create Department Summary Response

```java
class DepartmentSummary {
    private final long employeeCount;
    private final double averageSalary;

    DepartmentSummary(long employeeCount, double averageSalary) {
        this.employeeCount = employeeCount;
        this.averageSalary = averageSalary;
    }
}
```

```java
Map<String, DepartmentSummary> summary = employees.stream()
    .collect(Collectors.groupingBy(
        Employee::getDepartment,
        Collectors.collectingAndThen(
            Collectors.toList(),
            list -> new DepartmentSummary(
                list.size(),
                list.stream().mapToInt(Employee::getSalary).average().orElse(0.0)
            )
        )
    ));
```

This is useful but a little heavy.

For interviews, you can mention:

```text
For complex summaries, sometimes a normal loop or a dedicated accumulator is more readable.
```

---

## 22. Rapid Interview Meter

| Topic | Importance | Confidence Target |
|---|---:|---|
| filter + map + collect | Very high | Must code instantly |
| flatMap | Very high | Must explain clearly |
| groupingBy | Very high | Must know counting, mapping, averaging |
| toMap duplicate key | Very high | Must mention merge function |
| sorting with Comparator | Very high | Must know reversed and thenComparing |
| Optional from findFirst/max/min | High | Avoid `.get()` blindly |
| second highest salary | High | Must use distinct + sorted + skip |
| primitive streams | High | Use `mapToInt` |
| laziness | High | Terminal operation triggers execution |
| parallel streams | Medium | Know caveats |
| peek | Medium | Debug only, not business side effects |

---

## 23. One-Line Answers To Memorize

### What is a Stream?

```text
A Stream is a lazy pipeline for processing elements from a source using functional-style operations.
```

### Stream vs Collection?

```text
A collection stores data. A stream processes data from a source and does not store elements.
```

### Intermediate vs Terminal?

```text
Intermediate operations build the pipeline lazily; terminal operations execute it.
```

### map vs flatMap?

```text
map transforms one element to one result; flatMap transforms one element to many results and flattens them.
```

### groupingBy vs partitioningBy?

```text
groupingBy groups by any key; partitioningBy splits into true and false groups based on a predicate.
```

### reduce vs collect?

```text
reduce combines elements into a single value; collect accumulates elements into a mutable container like List or Map.
```

### Why use mapToInt?

```text
mapToInt avoids boxing and gives direct numeric operations like sum, average, min, and max.
```

### Can stream be reused?

```text
No. A stream is consumed after a terminal operation. Create a new stream from the source.
```

### Is Stream thread-safe?

```text
The stream abstraction is not the main issue; the operations must be stateless and non-interfering, especially in parallel streams.
```

---

## 24. Monday Practice Questions

Practice these without looking:

1. Get names of employees whose salary is greater than `100000`.
2. Get sorted unique departments.
3. Get all unique skills from all employees.
4. Count employees by department.
5. Find average salary by department.
6. Find highest paid employee by department.
7. Find second highest distinct salary.
8. Find employees with second highest distinct salary.
9. Convert employee list to map by ID.
10. Handle duplicate key in `toMap`.
11. Count word frequency using streams.
12. Find duplicate words using streams.
13. Find first non-repeating character in a string.
14. Sort a map by value descending.
15. Explain why a stream cannot be reused.
16. Explain why `peek` without terminal operation prints nothing.
17. Explain `map` vs `flatMap` with an example.
18. Explain when not to use parallel streams.

---

## 25. One Master Stream Flow For Interview

This is the single flow to remember before an interview.

It covers the most asked intermediate operators:

- `stream`
- `filter`
- `map`
- `flatMap`
- `distinct`
- `sorted`
- `skip`
- `limit`
- `peek`

And it shows how terminal operations end the pipeline:

- `collect`
- `toList`
- `groupingBy`
- `toMap`
- `joining`
- `count`
- `findFirst`
- `anyMatch`
- `reduce`
- `forEach`

---

### Master Example: From Employees To Interview-Ready Results

Imagine interviewer gives this data:

```java
List<Employee> employees = Arrays.asList(
    new Employee(1, "Aravind", "Engineering", 29, 120000, Arrays.asList("Java", "Spring", "Kafka")),
    new Employee(2, "Rahul", "Engineering", 32, 150000, Arrays.asList("Java", "AWS")),
    new Employee(3, "Priya", "HR", 27, 80000, Arrays.asList("Excel", "Hiring")),
    new Employee(4, "Sneha", "Engineering", 26, 110000, Arrays.asList("React", "JavaScript")),
    new Employee(5, "Vikram", "Finance", 35, 130000, Arrays.asList("SQL", "Excel")),
    new Employee(6, "Anil", "Engineering", 31, 150000, Arrays.asList("Java", "Spring"))
);
```

---

### Flow 1: Most Important Intermediate Operators In One Chain

Question:

> Find top 3 unique skills of Engineering employees older than 25, sorted alphabetically after skipping the first skill.

```java
List<String> result = employees.stream()                         // source
    .filter(emp -> emp.getDepartment().equals("Engineering"))     // keep only engineering
    .filter(emp -> emp.getAge() > 25)                             // another condition
    .flatMap(emp -> emp.getSkills().stream())                     // List<Employee> -> Stream<String>
    .map(String::toUpperCase)                                     // transform
    .distinct()                                                   // remove duplicates
    .sorted()                                                     // natural sorting
    .peek(skill -> System.out.println("Skill: " + skill))         // debug only
    .skip(1)                                                      // skip first
    .limit(3)                                                     // take first 3
    .collect(Collectors.toList());                                // terminal operation
```

Output idea:

```text
[JAVASCRIPT, KAFKA, REACT]
```

Key explanation:

```text
filter narrows elements, flatMap flattens nested lists, map transforms values,
distinct removes duplicates, sorted orders, skip/limit paginate, and collect ends the stream.
```

Important interview warning:

```text
peek is mainly for debugging. Do not use it for business side effects.
```

---

### Flow 2: Same Data, `groupingBy` Terminal Collector

Question:

> Count employees by department.

```java
Map<String, Long> countByDepartment = employees.stream()
    .collect(Collectors.groupingBy(
        Employee::getDepartment,
        Collectors.counting()
    ));
```

Output idea:

```text
Engineering -> 4
HR -> 1
Finance -> 1
```

Why it is important:

```text
groupingBy is one of the most asked stream collectors. It is used when SQL-like GROUP BY
logic appears in Java code.
```

---

### Flow 3: `groupingBy` With Downstream `mapping`

Question:

> Get employee names by department.

```java
Map<String, List<String>> namesByDepartment = employees.stream()
    .collect(Collectors.groupingBy(
        Employee::getDepartment,
        Collectors.mapping(Employee::getName, Collectors.toList())
    ));
```

Output idea:

```text
Engineering -> [Aravind, Rahul, Sneha, Anil]
HR -> [Priya]
Finance -> [Vikram]
```

Interview line:

```text
groupingBy creates groups, and downstream collectors decide what to store inside each group.
```

---

### Flow 4: `toMap` Terminal Collector

Question:

> Convert employee list into map by employee ID.

```java
Map<Integer, Employee> employeeById = employees.stream()
    .collect(Collectors.toMap(
        Employee::getId,
        Function.identity()
    ));
```

Important duplicate-key version:

```java
Map<Integer, Employee> employeeById = employees.stream()
    .collect(Collectors.toMap(
        Employee::getId,
        Function.identity(),
        (oldValue, newValue) -> oldValue
    ));
```

Interview trap:

```text
Collectors.toMap throws IllegalStateException if duplicate keys appear and no merge function is provided.
```

---

### Flow 5: `joining` Terminal Collector

Question:

> Join all sorted Engineering employee names as comma-separated text.

```java
String names = employees.stream()
    .filter(emp -> emp.getDepartment().equals("Engineering"))
    .map(Employee::getName)
    .sorted()
    .collect(Collectors.joining(", "));
```

Output idea:

```text
Anil, Aravind, Rahul, Sneha
```

---

### Flow 6: `findFirst`, `anyMatch`, `count`

Most asked short terminals:

```java
Optional<Employee> firstHighPaid = employees.stream()
    .filter(emp -> emp.getSalary() > 140000)
    .findFirst();
```

```java
boolean hasKafkaSkill = employees.stream()
    .flatMap(emp -> emp.getSkills().stream())
    .anyMatch(skill -> skill.equalsIgnoreCase("Kafka"));
```

```java
long engineeringCount = employees.stream()
    .filter(emp -> emp.getDepartment().equals("Engineering"))
    .count();
```

Interview line:

```text
findFirst returns Optional, anyMatch returns boolean, and count returns long.
All three are terminal operations.
```

---

### Flow 7: `reduce` Terminal Operation

Question:

> Find total salary using reduce.

```java
int totalSalary = employees.stream()
    .map(Employee::getSalary)
    .reduce(0, Integer::sum);
```

Better production version:

```java
int totalSalary = employees.stream()
    .mapToInt(Employee::getSalary)
    .sum();
```

Interview line:

```text
reduce combines stream elements into one result. For primitive numeric sums,
mapToInt().sum() is usually cleaner.
```

---

### Flow 8: `forEach` Terminal Operation

```java
employees.stream()
    .filter(emp -> emp.getSalary() > 120000)
    .map(Employee::getName)
    .forEach(System.out::println);
```

Interview warning:

```text
forEach is terminal and usually used for side effects. Do not use it when map/collect
would express transformation better.
```

---

### One-Minute Interview Answer

If interviewer asks:

> Explain stream flow.

Say:

```text
A stream pipeline starts from a source like List. Intermediate operations such as filter,
map, flatMap, distinct, sorted, skip, and limit are lazy and return another stream.
Nothing executes until a terminal operation like collect, count, findFirst, anyMatch,
reduce, or forEach is called. In interviews, I explain the pipeline as source,
intermediate transformations, and one terminal result.
```

---

### Must-Remember Operator Map

| Need | Operator |
|---|---|
| Keep matching data | `filter` |
| Convert one value to another | `map` |
| Flatten nested lists | `flatMap` |
| Remove duplicates | `distinct` |
| Sort | `sorted` |
| Debug pipeline | `peek` |
| Pagination | `skip` + `limit` |
| Convert to list/set/map | `collect` |
| Group records | `groupingBy` |
| Split true/false | `partitioningBy` |
| Join strings | `joining` |
| Find one | `findFirst` / `findAny` |
| Boolean check | `anyMatch` / `allMatch` / `noneMatch` |
| Count records | `count` |
| Combine to one value | `reduce` |
| Side effect terminal | `forEach` |

---

## 26. Final Cheat Sheet

| Need | Stream Chain |
|---|---|
| Filter data | `stream().filter(...).collect(...)` |
| Transform data | `stream().map(...).collect(...)` |
| Flatten nested lists | `stream().flatMap(...).collect(...)` |
| Sort ascending | `sorted(Comparator.comparing(...))` |
| Sort descending | `sorted(Comparator.comparing(...).reversed())` |
| Unique values | `map(...).distinct()` |
| Top N | `sorted(...).limit(n)` |
| Nth highest | `distinct().sorted(reverse).skip(n - 1).findFirst()` |
| Count by key | `groupingBy(key, counting())` |
| Values by key | `groupingBy(key, mapping(value, toList()))` |
| Average by key | `groupingBy(key, averagingInt(...))` |
| Map by ID | `toMap(id, Function.identity())` |
| Duplicate key map | `toMap(key, value, mergeFunction)` |
| Sum numbers | `mapToInt(...).sum()` |
| Max object | `max(Comparator.comparing(...))` |
| Join strings | `collect(joining(","))` |
| Boolean split | `partitioningBy(predicate)` |

---

## 27. What To Say If Asked To Write Streams In Production Code

```text
I use streams when the transformation is clear and readable, especially for filtering,
mapping, grouping, and collecting. I avoid overly complex stream chains when a normal loop
is easier to debug or when the code involves side effects, checked exceptions, blocking IO,
or complicated branching.
```

This answer shows maturity.

Interviewers like streams, but they like judgment more.
