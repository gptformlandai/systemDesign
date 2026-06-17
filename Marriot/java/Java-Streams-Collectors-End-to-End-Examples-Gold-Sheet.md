# Java Streams And Collectors End-To-End Examples Gold Sheet

Target: complete stream + collector practice for interviews.

Use this after:

- `Java-Streams-Interview-Prep.md`
- `Java-Collectors-Terminal-Operators-Gold-Sheet.md`

Goal:

```text
Move from knowing operators individually to solving full interview problems end to end.
```

---

## 1. Stream Problem Solving Template

For every stream question, think in this order:

```text
1. What is the source?
2. Do I need to filter?
3. Do I need to transform with map?
4. Do I need to flatten with flatMap?
5. Do I need sorting/distinct/limit/skip?
6. What final result type is required?
7. Which terminal operation or collector gives that result?
```

Memory line:

```text
Source -> filter -> map/flatMap -> sort/distinct -> collect/reduce/find/count.
```

---

## 2. Common Sample Model

Use this model for most examples:

```java
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

record Employee(
    int id,
    String name,
    String department,
    int age,
    int salary,
    List<String> skills
) {
}
```

Sample data:

```java
List<Employee> employees = List.of(
    new Employee(1, "Aravind", "Engineering", 29, 120000, List.of("Java", "Spring", "Kafka")),
    new Employee(2, "Rahul", "Engineering", 32, 150000, List.of("Java", "AWS")),
    new Employee(3, "Priya", "HR", 27, 80000, List.of("Excel", "Hiring")),
    new Employee(4, "Sneha", "Engineering", 26, 110000, List.of("React", "JavaScript")),
    new Employee(5, "Vikram", "Finance", 35, 130000, List.of("SQL", "Excel")),
    new Employee(6, "Anil", "Engineering", 31, 150000, List.of("Java", "Spring"))
);
```

---

## 3. Filter + Map + Collect

Question:

> Get names of Engineering employees earning more than 120000.

Code:

```java
List<String> names = employees.stream()
    .filter(employee -> "Engineering".equals(employee.department()))
    .filter(employee -> employee.salary() > 120000)
    .map(Employee::name)
    .collect(Collectors.toList());
```

Output idea:

```text
[Rahul, Anil]
```

Explanation:

```text
filter narrows employees, map extracts names, collect builds the final List.
```

Trap:

```text
Use "Engineering".equals(value) for null-safe string comparison.
```

---

## 4. Unique Sorted Departments

Question:

> Get unique departments sorted alphabetically.

Code:

```java
List<String> departments = employees.stream()
    .map(Employee::department)
    .distinct()
    .sorted()
    .collect(Collectors.toList());
```

Output idea:

```text
[Engineering, Finance, HR]
```

Interview line:

```text
distinct removes duplicates based on equals/hashCode, and sorted orders the result.
```

---

## 5. FlatMap: Unique Skills

Question:

> Get all unique skills from all employees.

Code:

```java
List<String> skills = employees.stream()
    .flatMap(employee -> employee.skills().stream())
    .distinct()
    .sorted()
    .collect(Collectors.toList());
```

Output idea:

```text
[AWS, Excel, Hiring, Java, JavaScript, Kafka, React, SQL, Spring]
```

Explanation:

```text
Each employee has List<String>. map would create Stream<List<String>>. flatMap flattens
all skills into one Stream<String>.
```

Trap:

```text
Use flatMap when one input element can produce many output elements.
```

---

## 6. Count Employees By Department

Question:

> Count employees in each department.

Code:

```java
Map<String, Long> countByDepartment = employees.stream()
    .collect(Collectors.groupingBy(
        Employee::department,
        Collectors.counting()
    ));
```

Output idea:

```text
Engineering -> 4
HR -> 1
Finance -> 1
```

Interview line:

```text
groupingBy creates groups, and counting is the downstream collector that decides the value.
```

---

## 7. Employee Names By Department

Question:

> Group employee names by department.

Code:

```java
Map<String, List<String>> namesByDepartment = employees.stream()
    .collect(Collectors.groupingBy(
        Employee::department,
        Collectors.mapping(Employee::name, Collectors.toList())
    ));
```

Output idea:

```text
Engineering -> [Aravind, Rahul, Sneha, Anil]
HR -> [Priya]
Finance -> [Vikram]
```

Explanation:

```text
mapping transforms Employee to name inside each group.
```

---

## 8. Average Salary By Department

Question:

> Find average salary by department.

Code:

```java
Map<String, Double> averageSalaryByDepartment = employees.stream()
    .collect(Collectors.groupingBy(
        Employee::department,
        Collectors.averagingInt(Employee::salary)
    ));
```

Interview line:

```text
averagingInt is a downstream collector for numeric aggregation.
```

---

## 9. Total Salary By Department

Question:

> Find total salary by department.

Code:

```java
Map<String, Integer> totalSalaryByDepartment = employees.stream()
    .collect(Collectors.groupingBy(
        Employee::department,
        Collectors.summingInt(Employee::salary)
    ));
```

Trap:

```text
Use summingInt for int values. Use summingLong or summingDouble for other numeric types.
```

---

## 10. Highest Paid Employee

Question:

> Find the highest paid employee.

Code:

```java
Optional<Employee> highestPaid = employees.stream()
    .max(Comparator.comparingInt(Employee::salary));
```

Safe usage:

```java
Employee employee = highestPaid.orElseThrow();
```

Interview line:

```text
max returns Optional because the stream may be empty.
```

---

## 11. Highest Paid Employee By Department

Question:

> Find highest paid employee in each department.

Code:

```java
Map<String, Optional<Employee>> highestPaidByDepartment = employees.stream()
    .collect(Collectors.groupingBy(
        Employee::department,
        Collectors.maxBy(Comparator.comparingInt(Employee::salary))
    ));
```

Cleaner result without Optional:

```java
Map<String, Employee> highestPaidByDepartment = employees.stream()
    .collect(Collectors.groupingBy(
        Employee::department,
        Collectors.collectingAndThen(
            Collectors.maxBy(Comparator.comparingInt(Employee::salary)),
            Optional::orElseThrow
        )
    ));
```

Trap:

```text
maxBy returns Optional because each group could theoretically be empty, even though normal
grouping creates non-empty groups.
```

---

## 12. Second Highest Distinct Salary

Question:

> Find the second highest distinct salary.

Code:

```java
Optional<Integer> secondHighestSalary = employees.stream()
    .map(Employee::salary)
    .distinct()
    .sorted(Comparator.reverseOrder())
    .skip(1)
    .findFirst();
```

Output idea:

```text
130000
```

Trap:

```text
Use distinct before skip, otherwise duplicate top salaries can give the wrong answer.
```

---

## 13. Employees With Second Highest Salary

Question:

> Find all employees who have the second highest distinct salary.

Code:

```java
Optional<Integer> secondHighestSalary = employees.stream()
    .map(Employee::salary)
    .distinct()
    .sorted(Comparator.reverseOrder())
    .skip(1)
    .findFirst();

List<Employee> result = secondHighestSalary
    .map(salary -> employees.stream()
        .filter(employee -> employee.salary() == salary)
        .collect(Collectors.toList()))
    .orElseGet(List::of);
```

Interview line:

```text
First find the second highest distinct salary, then filter employees matching that salary.
```

---

## 14. Convert List To Map By ID

Question:

> Convert employees to `Map<id, employee>`.

Code:

```java
Map<Integer, Employee> employeeById = employees.stream()
    .collect(Collectors.toMap(
        Employee::id,
        Function.identity()
    ));
```

Trap:

```text
If duplicate IDs appear, toMap throws IllegalStateException unless a merge function is provided.
```

---

## 15. Duplicate Key In toMap

Question:

> Convert employees to map by department, keeping highest paid employee per department.

Code:

```java
Map<String, Employee> highestPaidByDepartment = employees.stream()
    .collect(Collectors.toMap(
        Employee::department,
        Function.identity(),
        (oldEmployee, newEmployee) ->
            oldEmployee.salary() >= newEmployee.salary() ? oldEmployee : newEmployee
    ));
```

Explanation:

```text
The merge function resolves duplicate department keys.
```

---

## 16. Preserve Map Order With toMap

Question:

> Convert employees to map by ID while preserving encounter order.

Code:

```java
Map<Integer, Employee> employeeById = employees.stream()
    .collect(Collectors.toMap(
        Employee::id,
        Function.identity(),
        (oldValue, newValue) -> oldValue,
        LinkedHashMap::new
    ));
```

Interview line:

```text
The fourth argument supplies the Map implementation.
```

---

## 17. Partition Employees

Question:

> Split employees into high paid and others.

Code:

```java
Map<Boolean, List<Employee>> partitioned = employees.stream()
    .collect(Collectors.partitioningBy(employee -> employee.salary() > 120000));
```

Meaning:

```text
true  -> high paid
false -> others
```

groupingBy vs partitioningBy:

```text
groupingBy can create many groups. partitioningBy always creates true and false groups.
```

---

## 18. Join Names

Question:

> Join all employee names alphabetically using comma.

Code:

```java
String names = employees.stream()
    .map(Employee::name)
    .sorted()
    .collect(Collectors.joining(", "));
```

Output idea:

```text
Anil, Aravind, Priya, Rahul, Sneha, Vikram
```

---

## 19. Count Word Frequency

Question:

> Count frequency of words.

Code:

```java
List<String> words = List.of("java", "spring", "java", "kafka", "spring");

Map<String, Long> frequency = words.stream()
    .collect(Collectors.groupingBy(
        Function.identity(),
        Collectors.counting()
    ));
```

Output:

```text
java -> 2
spring -> 2
kafka -> 1
```

Alternative with merge:

```java
Map<String, Integer> frequency = words.stream()
    .collect(Collectors.toMap(
        Function.identity(),
        word -> 1,
        Integer::sum
    ));
```

---

## 20. Find Duplicate Words

Question:

> Find duplicate words.

Code:

```java
List<String> words = List.of("java", "spring", "java", "kafka", "spring");

Set<String> duplicates = words.stream()
    .collect(Collectors.groupingBy(
        Function.identity(),
        Collectors.counting()
    ))
    .entrySet()
    .stream()
    .filter(entry -> entry.getValue() > 1)
    .map(Map.Entry::getKey)
    .collect(Collectors.toSet());
```

Output:

```text
[java, spring]
```

Interview line:

```text
First build a frequency map, then filter entries whose count is greater than one.
```

---

## 21. First Non-Repeating Character

Question:

> Find first non-repeating character in a string.

Code:

```java
String input = "swiss";

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

Output:

```text
w
```

Trap:

```text
Use LinkedHashMap to preserve encounter order.
```

---

## 22. Sort Map By Value Descending

Question:

> Sort word-frequency map by count descending.

Code:

```java
Map<String, Long> sorted = frequency.entrySet()
    .stream()
    .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
    .collect(Collectors.toMap(
        Map.Entry::getKey,
        Map.Entry::getValue,
        (oldValue, newValue) -> oldValue,
        LinkedHashMap::new
    ));
```

Why LinkedHashMap:

```text
It preserves the sorted encounter order in the final map.
```

---

## 23. Top N Salaries

Question:

> Get top 3 distinct salaries.

Code:

```java
List<Integer> topSalaries = employees.stream()
    .map(Employee::salary)
    .distinct()
    .sorted(Comparator.reverseOrder())
    .limit(3)
    .collect(Collectors.toList());
```

Trap:

```text
Use distinct if the question asks distinct salaries.
```

---

## 24. Any / All / None Match

Question:

> Check if any employee knows Kafka, all employees are adults, and none have negative salary.

Code:

```java
boolean anyKafka = employees.stream()
    .flatMap(employee -> employee.skills().stream())
    .anyMatch(skill -> skill.equalsIgnoreCase("Kafka"));

boolean allAdults = employees.stream()
    .allMatch(employee -> employee.age() >= 18);

boolean noNegativeSalary = employees.stream()
    .noneMatch(employee -> employee.salary() < 0);
```

Interview line:

```text
Match operations are terminal and short-circuit when the result is known.
```

---

## 25. Reduce vs Sum

Question:

> Find total salary using reduce and better numeric approach.

Reduce:

```java
int total = employees.stream()
    .map(Employee::salary)
    .reduce(0, Integer::sum);
```

Better:

```java
int total = employees.stream()
    .mapToInt(Employee::salary)
    .sum();
```

Interview line:

```text
For numeric aggregations, primitive streams are usually cleaner and avoid boxing.
```

---

## 26. Null-Safe Stream

Question:

> How do you stream a possibly null list?

Code:

```java
List<Employee> safeEmployees = employees == null ? List.of() : employees;

List<String> names = safeEmployees.stream()
    .map(Employee::name)
    .collect(Collectors.toList());
```

Modern Optional style:

```java
List<String> names = Optional.ofNullable(employees)
    .orElseGet(List::of)
    .stream()
    .map(Employee::name)
    .collect(Collectors.toList());
```

Production line:

```text
Prefer APIs that return empty collections instead of null collections.
```

---

## 27. Null-Safe Field Filtering

Question:

> Filter employees whose department is Engineering without NPE.

Code:

```java
List<Employee> result = employees.stream()
    .filter(employee -> "Engineering".equals(employee.department()))
    .collect(Collectors.toList());
```

Why:

```text
Calling equals on the constant avoids NullPointerException if department is null.
```

---

## 28. `collectingAndThen`

Question:

> Return an unmodifiable list of employee names.

Code:

```java
List<String> names = employees.stream()
    .map(Employee::name)
    .collect(Collectors.collectingAndThen(
        Collectors.toList(),
        Collections::unmodifiableList
    ));
```

Modern Java:

```java
List<String> names = employees.stream()
    .map(Employee::name)
    .toList();
```

Caution:

```text
Stream.toList() returns an unmodifiable list in modern Java.
```

---

## 29. Teeing Collector

Question:

> Get min and max salary in one pass.

Code:

```java
record SalaryRange(int min, int max) {
}

SalaryRange range = employees.stream()
    .collect(Collectors.teeing(
        Collectors.minBy(Comparator.comparingInt(Employee::salary)),
        Collectors.maxBy(Comparator.comparingInt(Employee::salary)),
        (min, max) -> new SalaryRange(
            min.orElseThrow().salary(),
            max.orElseThrow().salary()
        )
    ));
```

Interview priority:

```text
teeing is useful to know, but groupingBy, toMap, mapping, counting, and flatMap are asked more often.
```

---

## 30. Parallel Stream With Collectors

Question:

> Should we use parallel stream for grouping?

Safe answer:

```text
Only if the data is large, CPU-bound, independent, and measured to benefit. For parallel
grouping, groupingByConcurrent can help, but it is not automatically faster. Avoid parallel
streams for blocking DB/API calls and shared mutable state.
```

Example:

```java
Map<String, Long> countByDepartment = employees.parallelStream()
    .collect(Collectors.groupingByConcurrent(
        Employee::department,
        Collectors.counting()
    ));
```

Trap:

```text
Parallel stream uses common ForkJoinPool by default.
```

---

## 31. End-To-End Mini Challenge

Question:

> For Engineering employees older than 25, get top 3 unique skills alphabetically after converting to uppercase.

Code:

```java
List<String> result = employees.stream()
    .filter(employee -> "Engineering".equals(employee.department()))
    .filter(employee -> employee.age() > 25)
    .flatMap(employee -> employee.skills().stream())
    .map(String::toUpperCase)
    .distinct()
    .sorted()
    .limit(3)
    .collect(Collectors.toList());
```

Explain in interview:

```text
I filter employees first, flatten their skills, transform to uppercase, remove duplicates,
sort, take top 3, and collect into a list.
```

---

## 32. Collector Cheat Sheet

| Need | Pattern |
|---|---|
| List | `collect(toList())` |
| Set | `collect(toSet())` |
| Specific collection | `collect(toCollection(LinkedHashSet::new))` |
| Map by key | `collect(toMap(key, value))` |
| Map duplicate key | `collect(toMap(key, value, merge))` |
| Preserve map order | `toMap(key, value, merge, LinkedHashMap::new)` |
| Group by key | `groupingBy(key)` |
| Count by key | `groupingBy(key, counting())` |
| Transform inside group | `groupingBy(key, mapping(value, toList()))` |
| Average by key | `groupingBy(key, averagingInt(...))` |
| Max by key | `groupingBy(key, maxBy(comparator))` |
| True/false split | `partitioningBy(predicate)` |
| Join strings | `joining(", ")` |
| Post-process result | `collectingAndThen(...)` |

---

## 33. Common Mistakes

| Mistake | Why Wrong | Better Approach |
|---|---|---|
| Forgetting terminal operation | Stream never runs | Add collect/count/find/forEach |
| Using map for nested lists | Creates Stream<List<T>> | Use flatMap |
| `toMap` without duplicate handling | Throws on duplicate key | Add merge function |
| Mutating external list | Side effects, unsafe in parallel | Use collect |
| Assuming parallel is faster | Can add overhead | Measure first |
| Using Optional.get blindly | Throws if empty | Use orElse/orElseThrow |
| Iterating map after sort into HashMap | Order lost | Use LinkedHashMap |
| Using reduce for mutable containers | Awkward/wrong in parallel | Use collect |

---

## 34. Final Interview Answer

If interviewer asks:

> How comfortable are you with streams and collectors?

Say:

```text
I am comfortable with stream pipelines from source to terminal operation. I use filter for
selection, map for one-to-one transformation, flatMap for one-to-many flattening, sorted and
distinct for ordering and uniqueness, and collectors for final results. For collectors, I
regularly use toList, toSet, toMap with merge functions, groupingBy with downstream collectors
like counting, mapping, averagingInt and maxBy, partitioningBy, and joining. I avoid side
effects, use Optional safely, and avoid parallel streams unless the workload is large,
CPU-bound, independent, and measured.
```

---

## 35. Final Memory Trick

```text
filter asks: should this stay?
map asks: what should this become?
flatMap asks: how do I flatten many values?
collect asks: what final shape do I want?
```
