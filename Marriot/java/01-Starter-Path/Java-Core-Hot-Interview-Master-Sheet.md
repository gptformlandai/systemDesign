# Java Core Hot Interview Master Sheet

Target: Marriott Tech Accelerator / Intervue Java backend round.

This sheet covers the Java core topics that interviewers repeatedly touch for medium-level backend roles:
- JVM, JDK, JRE, bytecode, class loading
- Memory areas: heap, stack, metaspace, string pool, static data
- `static`, `final`, constructors, object creation
- OOP, immutability, `equals` / `hashCode`
- Collections and HashMap internals
- Exceptions and generics
- Multithreading, Java Memory Model, ExecutorService
- Serialization and deserialization
- Garbage collection and memory leaks
- Rapid hot questions and traps

How to use this:
- First read the "must know" answer.
- Then read the trap.
- Then say the answer out loud in 30-60 seconds.
- For code snippets, type at least the important ones once.

---

## 1. Interview Priority Meter

| Area | Priority | What They Usually Test |
|---|---:|---|
| JVM/JDK/JRE | Very high | Can you explain how Java runs? |
| Heap vs Stack | Very high | Memory clarity, object/reference basics |
| Class loading | High | ClassLoader hierarchy and initialization |
| `static` keyword | Very high | Class-level behavior and tricky overriding |
| `final` / `finally` / `finalize` | Very high | Common confusion check |
| OOP principles | Very high | Design maturity and fundamentals |
| `equals` / `hashCode` | Very high | HashMap/HashSet correctness |
| HashMap internals | Very high | Java backend interview classic |
| String immutability | Very high | Pool, `==`, `equals`, `intern` |
| Collections | Very high | List/Set/Map, fail-fast, concurrent collections |
| Exceptions | High | Checked vs unchecked, try-with-resources |
| Generics | Medium-high | Type erasure, wildcards, PECS |
| Multithreading | Very high | Thread safety, locks, visibility |
| ExecutorService | Very high | Real backend concurrency |
| Java Memory Model | High | `volatile`, happens-before |
| Serialization | High | `serialVersionUID`, transient, readResolve |
| Garbage collection | High | Generations, roots, leaks, collectors |
| Reflection/annotations | Medium | Framework awareness |
| Cloning/copying | Medium | Shallow vs deep copy |

---

## 2. JVM, JDK, And JRE

### Must Know

| Term | Meaning |
|---|---|
| JDK | Java Development Kit. Contains tools to develop Java apps, including compiler and JRE |
| JRE | Java Runtime Environment. Contains JVM and libraries to run Java apps |
| JVM | Java Virtual Machine. Executes bytecode |

### Strong Interview Answer

```text
JDK is for development, JRE is for running Java applications, and JVM is the runtime engine
inside the JRE that executes Java bytecode. We write .java files, compile them into .class
bytecode using javac, and the JVM loads, verifies, interprets, and JIT-compiles that bytecode.
```

### Java Execution Flow

```text
Source code (.java)
    -> javac compiler
Bytecode (.class)
    -> ClassLoader
Bytecode verification
    -> JVM execution engine
Interpreter + JIT compiler
    -> Native machine code execution
```

### Important JVM Components

| Component | Role |
|---|---|
| ClassLoader subsystem | Loads classes into JVM |
| Runtime data areas | Heap, stack, metaspace, PC register, native method stack |
| Execution engine | Executes bytecode |
| Interpreter | Executes bytecode line by line |
| JIT compiler | Compiles hot code paths to native machine code |
| Garbage collector | Reclaims unused heap memory |
| JNI | Bridge to native code |

### JDK Tools To Know

| Tool | Use |
|---|---|
| `javac` | Compiles `.java` to `.class` |
| `java` | Runs Java application |
| `jar` | Packages classes/resources |
| `javadoc` | Generates documentation |
| `jshell` | Java REPL |
| `jstack` | Thread dump |
| `jmap` | Heap dump / memory info |
| `jcmd` | JVM diagnostics |
| `jstat` | GC/class/memory stats |

### Hot Questions

#### Q1. Why is Java platform independent?

```text
Java source code compiles to bytecode, not directly to platform-specific machine code.
The JVM for each OS understands the same bytecode and executes it on that platform.
```

#### Q2. Is JVM platform independent?

```text
No. Bytecode is platform independent, but JVM itself is platform specific.
Each OS needs its own JVM implementation.
```

#### Q3. What is JIT?

```text
JIT stands for Just-In-Time compiler. It compiles frequently executed bytecode into native
machine code at runtime, improving performance compared with pure interpretation.
```

#### Q4. Interpreter vs JIT?

| Interpreter | JIT |
|---|---|
| Executes bytecode line by line | Compiles hot bytecode to native code |
| Starts quickly | Optimizes after runtime profiling |
| Slower for repeated code | Faster for repeated hot paths |

### Interview Trap

Wrong:

```text
JDK, JRE, and JVM are the same.
```

Correct:

```text
JDK contains JRE plus development tools. JRE contains JVM plus runtime libraries.
JVM executes bytecode.
```

---

## 3. Memory Areas In Java

### JVM Runtime Memory Areas

| Area | Per JVM or Per Thread? | Stores |
|---|---|---|
| Heap | Shared per JVM | Objects, arrays |
| Java Stack | Per thread | Stack frames, local variables, method calls |
| Metaspace | Shared per JVM | Class metadata |
| PC Register | Per thread | Current bytecode instruction address |
| Native Method Stack | Per thread | Native method execution |

### Heap

Heap stores objects and arrays.

Example:

```java
Employee emp = new Employee();
```

What happens:
- `new Employee()` creates object on heap.
- `emp` is a reference variable.
- If `emp` is local, reference variable is stored in stack frame.
- The actual object lives on heap.

### Stack

Each thread has its own stack.

Stack contains:
- Method call frames
- Local primitive variables
- Local reference variables
- Return addresses

Example:

```java
public void process() {
    int count = 10;
    Employee emp = new Employee();
}
```

Memory:

```text
count -> stack
emp reference -> stack
Employee object -> heap
```

### Metaspace

Metaspace stores class metadata:
- Class structure
- Method metadata
- Field metadata
- Runtime constant pool metadata

Java 8 removed PermGen and introduced Metaspace.

### String Pool

String literals are stored in the string pool, which is part of heap memory in modern Java.

```java
String a = "java";
String b = "java";

System.out.println(a == b); // true
```

Both refer to same pooled string literal.

### Static Variables

Static variables are class-level variables.

Interview-safe answer:

```text
Static fields belong to the class, not an object. Their lifecycle is tied to class loading.
Class metadata is in Metaspace, while actual static field values/references are associated
with the Class object and live as long as the class is loaded.
```

For most interviews, you can say:

```text
Static data is class-level and lives until the class is unloaded, not per object and not in
a normal method stack frame.
```

### Heap vs Stack

| Heap | Stack |
|---|---|
| Stores objects and arrays | Stores method frames and local variables |
| Shared by threads | Each thread has its own stack |
| Managed by garbage collector | Cleared when method exits |
| Larger memory area | Smaller memory area |
| Can throw `OutOfMemoryError` | Can throw `StackOverflowError` |

### StackOverflowError

Usually caused by deep or infinite recursion.

```java
public class StackOverflowExample {
    public static void main(String[] args) {
        recurse();
    }

    static void recurse() {
        recurse();
    }
}
```

### OutOfMemoryError

Can happen when heap/metaspace/native memory cannot allocate more.

Example heap pressure:

```java
List<byte[]> list = new ArrayList<>();

while (true) {
    list.add(new byte[1024 * 1024]);
}
```

### Hot Questions

#### Q1. Where are objects stored?

```text
Objects and arrays are stored in heap.
```

#### Q2. Where are local variables stored?

```text
Local primitive variables and local references are stored in the thread stack frame.
The object referred to by a reference is stored in heap.
```

#### Q3. Is Java pass-by-value or pass-by-reference?

```text
Java is always pass-by-value. For objects, the value passed is a copy of the reference.
That means methods can mutate the same object, but cannot reassign the caller's reference.
```

Example:

```java
class User {
    String name;
}

public class PassByValueExample {
    public static void main(String[] args) {
        User user = new User();
        user.name = "A";

        changeName(user);
        System.out.println(user.name); // B

        reassign(user);
        System.out.println(user.name); // B
    }

    static void changeName(User user) {
        user.name = "B";
    }

    static void reassign(User user) {
        user = new User();
        user.name = "C";
    }
}
```

### JVM Flags To Know

| Flag | Meaning |
|---|---|
| `-Xms` | Initial heap size |
| `-Xmx` | Maximum heap size |
| `-Xss` | Thread stack size |
| `-XX:MaxMetaspaceSize` | Max metaspace size |
| `-XX:+UseG1GC` | Use G1 garbage collector |
| `-XX:+HeapDumpOnOutOfMemoryError` | Generate heap dump on OOM |

---

## 4. Class Loading And ClassLoaders

### Class Loading Flow

```text
Loading
    -> Linking
        -> Verification
        -> Preparation
        -> Resolution
    -> Initialization
```

### Loading

ClassLoader finds and loads `.class` bytecode.

### Linking

Verification:
- Ensures bytecode is valid and safe.

Preparation:
- Allocates memory for static variables and assigns default values.

Resolution:
- Converts symbolic references to direct references.

### Initialization

Static variables get actual assigned values and static blocks execute.

Example:

```java
class Demo {
    static int count = 10;

    static {
        System.out.println("Static block");
    }
}
```

### ClassLoader Hierarchy

| ClassLoader | Loads |
|---|---|
| Bootstrap ClassLoader | Core Java classes |
| Platform ClassLoader | Platform modules/classes |
| Application ClassLoader | Application classpath classes |
| Custom ClassLoader | App/framework-specific loading |

### Parent Delegation Model

ClassLoader first delegates class loading to parent.

If parent cannot load it, child tries.

Why?
- Avoid duplicate loading of core classes.
- Protect Java core classes.
- Maintain class identity consistency.

### Hot Question: ClassNotFoundException vs NoClassDefFoundError

| ClassNotFoundException | NoClassDefFoundError |
|---|---|
| Checked exception | Error |
| Happens during explicit class loading | Happens when class was available at compile time but missing/failing at runtime |
| Example: `Class.forName("X")` | Example: dependency missing at runtime |

### Example

```java
try {
    Class<?> clazz = Class.forName("com.example.DoesNotExist");
} catch (ClassNotFoundException ex) {
    ex.printStackTrace();
}
```

### Hot Questions

#### Q1. When does class initialization happen?

```text
Class initialization happens when the class is actively used, such as creating an object,
accessing a static field, invoking a static method, or using reflection.
```

#### Q2. Can two classes with same fully qualified name be different?

```text
Yes. In JVM, class identity is based on fully qualified class name plus the ClassLoader
that loaded it.
```

#### Q3. Why do frameworks use custom ClassLoaders?

```text
For plugin loading, isolation, hot deployment, application server isolation, and dynamic
module behavior.
```

---

## 5. `static` Keyword

### What Is `static`?

`static` means class-level, not object-level.

Can be used with:
- Variables
- Methods
- Blocks
- Nested classes

Cannot be used with:
- Top-level classes
- Local variables
- Constructors

### Static Variable

```java
class Counter {
    static int count = 0;

    Counter() {
        count++;
    }
}
```

All objects share the same `count`.

### Static Method

```java
class MathUtils {
    static int add(int a, int b) {
        return a + b;
    }
}
```

Usage:

```java
int result = MathUtils.add(10, 20);
```

### Static Block

Executes once when class is initialized.

```java
class Config {
    static Map<String, String> values = new HashMap<>();

    static {
        values.put("env", "dev");
    }
}
```

### Static Nested Class

```java
class Outer {
    static class Nested {
        void print() {
            System.out.println("Nested");
        }
    }
}
```

Usage:

```java
Outer.Nested nested = new Outer.Nested();
```

### Can Static Method Be Overridden?

No.

Static methods are hidden, not overridden.

```java
class Parent {
    static void show() {
        System.out.println("Parent");
    }
}

class Child extends Parent {
    static void show() {
        System.out.println("Child");
    }
}

public class StaticMethodHiding {
    public static void main(String[] args) {
        Parent obj = new Child();
        obj.show(); // Parent
    }
}
```

Why?

```text
Static method resolution is based on reference type at compile time, not runtime object type.
```

### Static Method Restrictions

Static method cannot directly access instance members.

Wrong:

```java
class Demo {
    int value = 10;

    static void print() {
        System.out.println(value);
    }
}
```

Correct:

```java
static void print(Demo demo) {
    System.out.println(demo.value);
}
```

### Hot Questions

| Question | Strong Answer |
|---|---|
| What is static? | Class-level member shared across objects |
| Can static method be overridden? | No, it is hidden |
| Can static method access instance variable? | Not directly, needs object reference |
| When does static block run? | Once, during class initialization |
| Can constructor be static? | No |
| Can top-level class be static? | No |

---

## 6. `final`, `finally`, And `finalize`

### `final`

`final` is a keyword.

It can be applied to:
- Variable
- Method
- Class

Final variable:

```java
final int count = 10;
```

Cannot reassign.

Final method:

```java
class Parent {
    final void show() {
        System.out.println("show");
    }
}
```

Cannot override.

Final class:

```java
final class Utility {
}
```

Cannot extend.

### Final Reference Trap

```java
final List<String> names = new ArrayList<>();

names.add("Java"); // allowed
names.add("Spring"); // allowed

// names = new ArrayList<>(); // not allowed
```

Strong answer:

```text
final prevents reassignment of the reference. It does not make the referenced object immutable.
```

### `finally`

`finally` is a block that usually executes after try/catch.

```java
try {
    process();
} catch (Exception ex) {
    handle(ex);
} finally {
    cleanup();
}
```

Used for cleanup.

Modern Java often prefers try-with-resources for closeable resources.

### When Finally May Not Execute

`finally` may not execute if:
- JVM exits using `System.exit`
- JVM crashes
- Process is killed
- Infinite loop/deadlock prevents completion

### `finalize`

`finalize()` was an old Object method called by GC before reclaiming object.

It is deprecated and should not be used.

Strong answer:

```text
finalize is unreliable and deprecated. For resource cleanup, use try-with-resources,
AutoCloseable, or explicit lifecycle management.
```

### Hot Question: final vs finally vs finalize

| Term | Meaning |
|---|---|
| `final` | Prevents reassignment/overriding/inheritance |
| `finally` | Cleanup block after try/catch |
| `finalize` | Deprecated cleanup hook before GC |

---

## 7. Object Creation In Java

### Ways To Create Object

| Way | Example |
|---|---|
| `new` keyword | `new Employee()` |
| Reflection | `clazz.getDeclaredConstructor().newInstance()` |
| Clone | `employee.clone()` |
| Deserialization | `ObjectInputStream.readObject()` |
| Factory method | `Employee.create()` |
| Builder pattern | `Employee.builder().build()` |
| Dependency injection | Spring creates bean |

### Reflection Example

```java
Class<?> clazz = Class.forName("com.example.Employee");
Object obj = clazz.getDeclaredConstructor().newInstance();
```

### Clone Example

```java
class Employee implements Cloneable {
    String name;

    @Override
    protected Employee clone() throws CloneNotSupportedException {
        return (Employee) super.clone();
    }
}
```

### Deserialization Example

```java
ObjectInputStream input = new ObjectInputStream(new FileInputStream("employee.ser"));
Employee emp = (Employee) input.readObject();
```

### Constructor Order

Order when creating child object:

```text
Parent static block
Child static block
Parent instance block
Parent constructor
Child instance block
Child constructor
```

Example:

```java
class Parent {
    static {
        System.out.println("Parent static");
    }

    {
        System.out.println("Parent instance");
    }

    Parent() {
        System.out.println("Parent constructor");
    }
}

class Child extends Parent {
    static {
        System.out.println("Child static");
    }

    {
        System.out.println("Child instance");
    }

    Child() {
        System.out.println("Child constructor");
    }
}
```

### Hot Questions

| Question | Strong Answer |
|---|---|
| Can constructor be inherited? | No |
| Can constructor be overridden? | No |
| Can constructor be private? | Yes, used in singleton/factory |
| Does deserialization call constructor? | Serializable class constructor is not called; first non-serializable superclass constructor is called |
| Does clone call constructor? | No |

---

## 8. OOP Concepts

### Four Pillars

| Pillar | Meaning |
|---|---|
| Encapsulation | Hide data and expose controlled methods |
| Inheritance | Reuse/extend behavior from parent |
| Polymorphism | Same interface, different implementations |
| Abstraction | Hide implementation details and expose essential behavior |

### Encapsulation

```java
class Account {
    private int balance;

    public void deposit(int amount) {
        if (amount <= 0) {
            throw new IllegalArgumentException("amount must be positive");
        }
        balance += amount;
    }

    public int getBalance() {
        return balance;
    }
}
```

### Overloading vs Overriding

| Overloading | Overriding |
|---|---|
| Same method name, different parameters | Subclass provides implementation of parent method |
| Compile-time polymorphism | Runtime polymorphism |
| Same class or subclass | In subclass |
| Return type alone cannot overload | Covariant return type allowed |

### Overriding Rules

- Method name and parameters must match.
- Return type must be same or covariant.
- Cannot reduce visibility.
- Cannot throw broader checked exception.
- `final`, `static`, and `private` methods are not overridden.

### Interface vs Abstract Class

| Interface | Abstract Class |
|---|---|
| Contract/capability | Base class with shared state/behavior |
| Multiple interfaces can be implemented | Only one class can be extended |
| Fields are public static final by default | Can have instance fields |
| Methods can be abstract/default/static/private | Can have abstract and concrete methods |

### Composition vs Inheritance

Strong answer:

```text
Prefer composition when we need flexible behavior reuse. Use inheritance only when there is
a true is-a relationship and parent-child substitution makes sense.
```

Example:

```java
class Engine {
    void start() {
        System.out.println("Engine started");
    }
}

class Car {
    private final Engine engine = new Engine();

    void start() {
        engine.start();
    }
}
```

### Hot Questions

| Question | Strong Answer |
|---|---|
| Runtime polymorphism example? | Method overriding |
| Compile-time polymorphism example? | Method overloading |
| Can private method be overridden? | No, it is not visible to subclass |
| Can final method be overridden? | No |
| Can static method be overridden? | No, hidden |
| Can abstract class have constructor? | Yes |
| Can interface have constructor? | No |

---

## 9. Immutability

### What Is Immutable Class?

An immutable class is a class whose object state cannot change after construction.

### How To Create Immutable Class

Rules:
1. Make class `final`.
2. Make fields `private final`.
3. Do not provide setters.
4. Initialize fields through constructor.
5. Make defensive copies of mutable inputs.
6. Return defensive copies of mutable fields.

### Example

```java
import java.util.*;

public final class EmployeeProfile {
    private final int id;
    private final String name;
    private final List<String> skills;

    public EmployeeProfile(int id, String name, List<String> skills) {
        this.id = id;
        this.name = name;
        this.skills = new ArrayList<>(skills);
    }

    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public List<String> getSkills() {
        return new ArrayList<>(skills);
    }
}
```

### Why Immutability?

- Thread-safe by default
- Easier to reason about
- Good for map keys
- No accidental state changes
- Safer sharing between layers

### Final vs Immutable

```text
final reference means the reference cannot be reassigned.
Immutable object means the object's state cannot change.
```

### Hot Question

Why is `String` immutable?

```text
String is immutable for security, thread safety, caching, class loading, and string pool
correctness. Since strings are widely shared, immutability prevents accidental changes.
```

---

## 10. String, StringBuilder, And StringBuffer

### String

Immutable.

```java
String s = "Java";
s.concat(" Backend");

System.out.println(s); // Java
```

Correct:

```java
s = s.concat(" Backend");
```

### `==` vs `equals`

```java
String a = "java";
String b = "java";
String c = new String("java");

System.out.println(a == b);      // true
System.out.println(a == c);      // false
System.out.println(a.equals(c)); // true
```

### `intern`

```java
String a = new String("java");
String b = a.intern();
String c = "java";

System.out.println(b == c); // true
```

### StringBuilder vs StringBuffer

| StringBuilder | StringBuffer |
|---|---|
| Mutable | Mutable |
| Not synchronized | Synchronized |
| Faster | Slower due to synchronization |
| Use in single-threaded context | Use when shared across threads, though often avoid shared mutable string builders |

### Hot Questions

| Question | Strong Answer |
|---|---|
| Why String immutable? | Security, thread safety, caching, pool correctness |
| `==` vs `equals`? | Reference comparison vs content comparison |
| StringBuilder vs StringBuffer? | Non-synchronized vs synchronized |
| Where is string pool? | Heap in modern Java |

---

## 11. `equals` And `hashCode`

### Contract

If two objects are equal by `equals`, they must have same `hashCode`.

If two objects have same `hashCode`, they are not necessarily equal.

### Correct Example

```java
import java.util.Objects;

class Employee {
    private final int id;
    private final String name;

    Employee(int id, String name) {
        this.id = id;
        this.name = name;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof Employee)) {
            return false;
        }
        Employee other = (Employee) obj;
        return id == other.id && Objects.equals(name, other.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, name);
    }
}
```

### Why It Matters

Hash-based collections depend on it:
- `HashMap`
- `HashSet`
- `LinkedHashMap`
- `ConcurrentHashMap`

### Common Mistake

Override `equals` but not `hashCode`.

Result:
- Object may not be found in HashSet/HashMap even though logically equal.

### Hot Questions

#### Q1. What happens if hashCode is always same?

```text
Correctness can still work if equals is correct, but performance degrades because many
objects land in the same bucket.
```

#### Q2. What happens if equals returns true but hashCodes differ?

```text
HashMap/HashSet behavior breaks. Equal objects may go to different buckets and lookup may fail.
```

---

## 12. Collections Framework

### Core Interfaces

| Interface | Meaning |
|---|---|
| List | Ordered, allows duplicates |
| Set | No duplicates |
| Queue | FIFO/processing order |
| Deque | Double-ended queue |
| Map | Key-value pairs |

### ArrayList vs LinkedList

| ArrayList | LinkedList |
|---|---|
| Backed by dynamic array | Backed by nodes |
| Fast random access `O(1)` | Slow random access `O(n)` |
| Insert/delete middle requires shifting | Insert/delete with node reference is cheaper |
| Better cache locality | More memory overhead |
| Usually preferred by default | Useful for queue/deque style cases |

### HashSet vs TreeSet vs LinkedHashSet

| Type | Order | Performance |
|---|---|---|
| HashSet | No guaranteed order | Average `O(1)` |
| LinkedHashSet | Insertion order | Average `O(1)` |
| TreeSet | Sorted order | `O(log n)` |

### HashMap vs LinkedHashMap vs TreeMap

| Type | Order | Performance |
|---|---|---|
| HashMap | No guaranteed order | Average `O(1)` |
| LinkedHashMap | Insertion/access order | Average `O(1)` |
| TreeMap | Sorted by key | `O(log n)` |

### HashMap Internals

HashMap stores key-value pairs in an internal array of buckets.

Mental model:

```text
HashMap
  table[0]  -> Node -> Node
  table[1]  -> null
  table[2]  -> Node
  table[3]  -> Node -> Node -> TreeNode
  ...
```

In Java, the internal table is an array:

```java
Node<K, V>[] table;
```

Each bucket can contain:

- no node
- one node
- linked list of nodes
- red-black tree of nodes, in Java 8+ under heavy collision conditions

---

### HashMap Node Structure

Each entry has:

- key
- value
- hash
- next pointer

Simplified internal node:

```java
static class Node<K, V> {
    final int hash;
    final K key;
    V value;
    Node<K, V> next;
}
```

Interview line:

```text
HashMap is an array of buckets. Each bucket stores nodes. Collisions are handled first
through linked list chaining, and in Java 8+ long chains may become red-black trees.
```

---

### How HashMap Calculates Bucket Index

HashMap does not directly use:

```java
key.hashCode()
```

It applies a spreading function to reduce poor hash distribution.

Simplified:

```java
int h = key.hashCode();
int hash = h ^ (h >>> 16);
```

Then bucket index is calculated using:

```java
index = (capacity - 1) & hash;
```

Why this works:

- HashMap capacity is usually a power of 2.
- Bitwise `&` is faster than modulo.
- `(capacity - 1) & hash` keeps index inside table range.

Example:

```text
capacity = 16
capacity - 1 = 15

index = hash & 15
```

Strong answer:

```text
HashMap spreads the key hash and calculates bucket index using `(n - 1) & hash`.
This works because table capacity is maintained as a power of two.
```

---

### How `put(key, value)` Works

Flow:

1. Calculate key hash.
2. Calculate bucket index.
3. If bucket is empty, insert new node.
4. If bucket is not empty, compare existing node hash and key.
5. If same key is found, replace old value.
6. If different key, move through linked list or tree.
7. If no matching key exists, add new node.
8. If bucket chain is too long, treeify if capacity is large enough.
9. If total size crosses threshold, resize table.

Simplified:

```java
map.put("A", 100);
```

Internally:

```text
hash("A") -> bucket index -> table[index]

If table[index] is empty:
    store new Node("A", 100)

If table[index] has nodes:
    compare hash and equals
    replace value if same key
    otherwise append/tree insert
```

Important:

HashMap uses both:

- `hashCode()` to find bucket
- `equals()` to find exact key inside bucket

---

### How `get(key)` Works

Flow:

1. Calculate key hash.
2. Calculate bucket index.
3. Go to that bucket.
4. Compare first node hash and key.
5. If matched, return value.
6. Else search linked list or red-black tree.
7. If no match, return `null`.

Example:

```java
Integer value = map.get("A");
```

Internally:

```text
hash("A") -> index
table[index] -> check nodes using hash + equals
```

Strong answer:

```text
HashMap lookup is fast because hash points directly to a bucket. Within the bucket,
HashMap uses equals to find the exact key.
```

---

### Collision Handling

Collision means two different keys land in the same bucket.

Example:

```text
key1 -> hash -> index 5
key2 -> hash -> index 5
```

HashMap handles collisions using chaining.

Before Java 8:

```text
bucket -> Node -> Node -> Node
```

Java 8+:

```text
bucket -> linked list
if list becomes too long and capacity is enough
bucket -> red-black tree
```

Why collision handling matters:

- Good hash distribution gives average `O(1)`.
- Too many collisions degrade performance.
- Treeification protects worst-case lookup.

---

### Java 8 HashMap Treeification

In Java 8, long bucket chains can become red-black trees.

Important thresholds:

- Treeify threshold: 8
- Untreeify threshold: 6
- Minimum capacity for treeify: 64

Meaning:

| Threshold | Meaning |
|---|---|
| `TREEIFY_THRESHOLD = 8` | Convert long list to tree |
| `UNTREEIFY_THRESHOLD = 6` | Convert tree back to list when smaller |
| `MIN_TREEIFY_CAPACITY = 64` | Do not treeify if table is too small |

Important nuance:

If bucket length reaches 8 but table capacity is less than 64, HashMap usually resizes instead of treeifying.

Interview answer:

```text
Java 8 improved HashMap collision handling by converting long collision chains into
red-black trees under certain conditions, improving worst-case lookup from O(n) to O(log n).
```

### HashMap Resize

Default:

- Initial capacity: 16
- Load factor: 0.75

When size exceeds:

```text
capacity * loadFactor
```

HashMap resizes.

For default:

```text
16 * 0.75 = 12
```

After 12 entries, resize occurs.

What resize does:

1. Create a new table, usually double the old capacity.
2. Move existing nodes to the new table.
3. Recalculate bucket placement based on new capacity.

Important Java 8 optimization:

When capacity doubles, each node either:

- stays at the same index
- moves to `oldIndex + oldCapacity`

This is based on one extra hash bit.

Example:

```text
old capacity = 16
new capacity = 32

old index = 5
after resize node may stay at 5 or move to 21
```

Why resizing is expensive:

- it moves many entries
- it can cause latency spikes
- avoid frequent resize by giving expected capacity when size is known

Example:

```java
Map<String, Integer> scores = new HashMap<>(128);
```

Strong answer:

```text
HashMap resizes when size exceeds capacity * load factor. Resize usually doubles capacity
and redistributes nodes, so it is relatively expensive.
```

---

### Null Key and Null Values

HashMap allows:

- one `null` key
- multiple `null` values

The `null` key is handled specially.

Conceptually:

```text
null key -> hash 0 -> bucket 0
```

Example:

```java
Map<String, Integer> map = new HashMap<>();
map.put(null, 10);
map.put(null, 20);

System.out.println(map.get(null)); // 20
```

Why only one null key?

Because keys are unique. Second `put(null, 20)` replaces old value.

---

### HashMap Complexity

| Operation | Average | Worst Case |
|---|---:|---:|
| `put` | `O(1)` | `O(log n)` with tree bin, `O(n)` without treeification |
| `get` | `O(1)` | `O(log n)` with tree bin, `O(n)` without treeification |
| `remove` | `O(1)` | `O(log n)` with tree bin, `O(n)` without treeification |

Interview wording:

```text
HashMap gives average O(1) put/get/remove. In Java 8+, heavy collision buckets can
treeify, improving worst-case bucket search to O(log n), but good hashCode design is still important.
```

---

### Why `equals` And `hashCode` Matter In HashMap

HashMap lookup depends on this contract:

```text
If a.equals(b) is true, then a.hashCode() must equal b.hashCode().
```

If this contract breaks:

- object may be stored in one bucket
- lookup may search another bucket
- `get` may return `null` even for logically equal key

Bad example:

```java
class Employee {
    private final int id;

    Employee(int id) {
        this.id = id;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof Employee other)) {
            return false;
        }
        return id == other.id;
    }

    // hashCode missing - bad for HashMap key
}
```

Correct:

```java
@Override
public int hashCode() {
    return Objects.hash(id);
}
```

---

### Mutable Key Trap

Never mutate fields used in `equals` or `hashCode` after putting an object into HashMap.

Bad:

```java
class Employee {
    int id;

    Employee(int id) {
        this.id = id;
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof Employee other && id == other.id;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}

Map<Employee, String> map = new HashMap<>();
Employee emp = new Employee(1);
map.put(emp, "A");

emp.id = 2;

System.out.println(map.get(emp)); // may return null
```

Why?

The object was stored based on old hash, but lookup uses new hash.

Strong answer:

```text
HashMap keys should be immutable, or at least fields used in equals/hashCode should not
change while the key is inside the map.
```

---

### HashMap Is Not Thread-Safe

HashMap is not safe for concurrent modification.

Problems:

- lost updates
- inconsistent reads
- data corruption risk
- fail-fast iterator may throw `ConcurrentModificationException`

Use:

```java
ConcurrentHashMap
```

for concurrent access.

Strong answer:

```text
HashMap is not thread-safe. For concurrent reads and writes, use ConcurrentHashMap or
external synchronization depending on the use case.
```

---

### HashMap Interview Flow Answer

If interviewer asks:

> How does HashMap work internally?

Say:

```text
HashMap stores entries in an internal bucket array. For put/get, it calculates the key's
hash, spreads it, and finds bucket index using `(capacity - 1) & hash`. If the bucket is
empty, it inserts directly. If there is a collision, it compares hash and equals to find
the same key or add a new node. Collisions are handled by linked lists, and in Java 8+
long chains can become red-black trees. HashMap resizes when size crosses capacity times
load factor, default 16 * 0.75 = 12. Average get/put is O(1), but bad hash distribution
can degrade performance.
```

---

### ConcurrentHashMap

Thread-safe map for concurrent access.

Strong answer:

```text
ConcurrentHashMap is designed for concurrent reads and updates. Unlike Hashtable or
Collections.synchronizedMap, it does not lock the whole map for most operations.
```

### Fail-Fast vs Fail-Safe

Fail-fast:

```java
List<String> list = new ArrayList<>(Arrays.asList("A", "B", "C"));

for (String value : list) {
    list.remove(value); // ConcurrentModificationException
}
```

Correct:

```java
Iterator<String> iterator = list.iterator();

while (iterator.hasNext()) {
    String value = iterator.next();
    if ("A".equals(value)) {
        iterator.remove();
    }
}
```

Fail-safe style:
- `CopyOnWriteArrayList`
- Iterates over snapshot, not original structure.

### Comparable vs Comparator

| Comparable | Comparator |
|---|---|
| Natural ordering | Custom ordering |
| `compareTo` | `compare` |
| Implemented by class | External strategy |
| One natural sort | Multiple sort options |

### Hot Questions

| Question | Strong Answer |
|---|---|
| ArrayList default capacity? | Lazy allocation; commonly grows to default capacity 10 on first add |
| HashMap default capacity? | 16 |
| HashMap load factor? | 0.75 |
| HashMap allows null? | One null key, multiple null values |
| How HashMap finds bucket? | It spreads hash and uses `(capacity - 1) & hash` |
| Why capacity is power of two? | Makes index calculation fast and distribution efficient |
| How HashMap handles collision? | Linked list first, red-black tree in Java 8+ after thresholds |
| Treeify threshold? | 8, if table capacity is at least 64 |
| Why mutable keys are dangerous? | Changing hash fields after insertion can make lookup fail |
| Is HashMap thread-safe? | No, use `ConcurrentHashMap` for concurrent access |
| ConcurrentHashMap allows null? | No null key or value |
| TreeMap allows null key? | Not with natural ordering in modern Java |
| HashSet internally uses? | HashMap |
| Fail-fast guaranteed? | Best-effort, not guaranteed for correctness |

---

## 13. Exceptions

### Exception Hierarchy

```text
Throwable
    -> Error
    -> Exception
        -> Checked exceptions
        -> RuntimeException
```

### Checked vs Unchecked

| Checked Exception | Unchecked Exception |
|---|---|
| Checked at compile time | Runtime exception |
| Must handle or declare | Not required to handle |
| Example: IOException | Example: NullPointerException |

### Error vs Exception

| Error | Exception |
|---|---|
| Serious JVM/system problem | Application-level issue |
| Usually not recoverable | Often recoverable |
| Example: OutOfMemoryError | Example: IOException |

### throw vs throws

`throw` is used to actually throw exception.

```java
throw new IllegalArgumentException("Invalid input");
```

`throws` declares possible exception.

```java
void readFile() throws IOException {
}
```

### try-with-resources

```java
try (BufferedReader reader = Files.newBufferedReader(path)) {
    return reader.readLine();
}
```

Resource must implement `AutoCloseable`.

### Custom Exception

```java
class BookingNotFoundException extends RuntimeException {
    BookingNotFoundException(String message) {
        super(message);
    }
}
```

### Hot Questions

| Question | Strong Answer |
|---|---|
| Can finally override return? | Yes, but avoid returning from finally |
| Can catch block catch subclass after parent? | No, unreachable |
| Checked exception in overriding? | Cannot throw broader checked exception |
| Runtime exception need throws? | No |
| try-with-resources benefit? | Automatic resource cleanup |

### Finally Return Trap

```java
static int test() {
    try {
        return 1;
    } finally {
        return 2;
    }
}
```

Output:

```text
2
```

Interview advice:

```text
Never return from finally in production code. It hides exceptions and return values.
```

---

## 14. Generics

### Why Generics?

Generics provide compile-time type safety.

Without generics:

```java
List list = new ArrayList();
list.add("Java");
Integer value = (Integer) list.get(0); // runtime issue
```

With generics:

```java
List<String> list = new ArrayList<>();
list.add("Java");
String value = list.get(0);
```

### Type Erasure

Java generics are implemented using type erasure.

At runtime:

```java
List<String>
List<Integer>
```

both are mostly just:

```java
List
```

### Wildcards

Upper bounded:

```java
List<? extends Number>
```

Can read as `Number`, but cannot safely add numbers except null.

Lower bounded:

```java
List<? super Integer>
```

Can add `Integer`, but reading gives `Object`.

### PECS Rule

```text
Producer Extends, Consumer Super.
```

If collection produces values for you to read:

```java
? extends T
```

If collection consumes values you add:

```java
? super T
```

### Generic Method

```java
public static <T> T first(List<T> list) {
    return list.get(0);
}
```

### Hot Questions

| Question | Strong Answer |
|---|---|
| What is type erasure? | Generic type info mostly removed at runtime |
| Can we create `new T()`? | No, type erased |
| Can we create generic array? | Not directly |
| `List<Object>` vs `List<?>`? | `List<?>` can refer to list of any type; `List<Object>` only object list |
| What is PECS? | Producer extends, consumer super |

---

## 15. Multithreading Basics

### Process vs Thread

| Process | Thread |
|---|---|
| Independent program execution | Lightweight execution unit inside process |
| Own memory space | Shares process memory |
| More expensive | Cheaper |
| Communication is heavier | Communication easier but needs synchronization |

### Ways To Create Thread

#### Extending Thread

```java
class MyThread extends Thread {
    @Override
    public void run() {
        System.out.println("Running");
    }
}
```

#### Implementing Runnable

```java
Runnable task = () -> System.out.println("Running");
new Thread(task).start();
```

#### Callable With Executor

```java
Callable<Integer> task = () -> 10;
ExecutorService executor = Executors.newSingleThreadExecutor();
Future<Integer> future = executor.submit(task);
System.out.println(future.get());
executor.shutdown();
```

### Runnable vs Callable

| Runnable | Callable |
|---|---|
| `run()` | `call()` |
| No return value | Returns value |
| Cannot throw checked exception directly | Can throw checked exception |

### Thread Lifecycle

```text
NEW
RUNNABLE
BLOCKED
WAITING
TIMED_WAITING
TERMINATED
```

### `start()` vs `run()`

```text
start creates a new thread and calls run internally.
Calling run directly is just a normal method call on the current thread.
```

### `sleep` vs `wait`

| sleep | wait |
|---|---|
| Static method of Thread | Method of Object |
| Does not release lock | Releases monitor lock |
| Used for pause | Used for inter-thread communication |
| No synchronized block required | Must be called inside synchronized context |

### `notify` vs `notifyAll`

| notify | notifyAll |
|---|---|
| Wakes one waiting thread | Wakes all waiting threads |
| Can be risky if wrong thread wakes | Safer when multiple conditions |

### `join`

Current thread waits for another thread to finish.

```java
Thread thread = new Thread(() -> System.out.println("Task"));
thread.start();
thread.join();
System.out.println("Task finished");
```

### Daemon Thread

Daemon threads do not prevent JVM shutdown.

```java
Thread thread = new Thread(() -> {});
thread.setDaemon(true);
thread.start();
```

Examples:
- GC thread
- Background cleanup

### Hot Questions

| Question | Strong Answer |
|---|---|
| start vs run? | start creates new thread; run is normal call |
| sleep releases lock? | No |
| wait releases lock? | Yes |
| wait must be inside synchronized? | Yes |
| Runnable vs Callable? | Callable returns value and throws checked exception |
| What is daemon thread? | Background thread that does not block JVM exit |

---

## 16. Synchronization And Java Memory Model

### Race Condition

Occurs when multiple threads access shared data and final result depends on timing.

```java
class Counter {
    int count = 0;

    void increment() {
        count++;
    }
}
```

`count++` is not atomic.

It means:
1. Read count.
2. Add one.
3. Write back.

### synchronized

```java
class Counter {
    private int count = 0;

    public synchronized void increment() {
        count++;
    }

    public synchronized int getCount() {
        return count;
    }
}
```

### Synchronized Block

```java
class Counter {
    private int count = 0;
    private final Object lock = new Object();

    public void increment() {
        synchronized (lock) {
            count++;
        }
    }
}
```

### volatile

`volatile` guarantees visibility, not atomicity.

```java
class StopSignal {
    private volatile boolean running = true;

    void stop() {
        running = false;
    }

    void run() {
        while (running) {
            // work
        }
    }
}
```

### Volatile Does Not Fix Count++

Wrong:

```java
volatile int count = 0;

void increment() {
    count++;
}
```

`count++` is still not atomic.

Use:

```java
AtomicInteger count = new AtomicInteger();

count.incrementAndGet();
```

### Atomic Classes

```java
AtomicInteger counter = new AtomicInteger(0);
counter.incrementAndGet();
```

Atomic classes use CAS-style operations internally.

### ReentrantLock

```java
Lock lock = new ReentrantLock();

lock.lock();
try {
    // critical section
} finally {
    lock.unlock();
}
```

### synchronized vs ReentrantLock

| synchronized | ReentrantLock |
|---|---|
| JVM-managed monitor | Explicit lock |
| Simpler | More flexible |
| Auto release when block exits | Must unlock in finally |
| No tryLock | Supports tryLock, fairness, interruptible lock |

### Happens-Before

Strong answer:

```text
Happens-before is a Java Memory Model rule that guarantees visibility and ordering between
operations. For example, unlock happens-before a later lock on the same monitor, and a write
to a volatile variable happens-before a later read of that same variable.
```

### ThreadLocal

Each thread gets its own copy of a variable.

```java
private static final ThreadLocal<String> requestId = new ThreadLocal<>();

requestId.set("REQ-1");
System.out.println(requestId.get());
requestId.remove();
```

Important:

```text
Always remove ThreadLocal values in thread pools to avoid memory leaks.
```

### Deadlock

Four conditions:
1. Mutual exclusion
2. Hold and wait
3. No preemption
4. Circular wait

Prevention:
- Lock ordering
- Timeout locks
- Avoid nested locks
- Use higher-level concurrency utilities

### Hot Questions

| Question | Strong Answer |
|---|---|
| volatile vs synchronized? | volatile visibility only; synchronized visibility + mutual exclusion |
| Is volatile atomic? | Only for simple read/write, not compound operations |
| What is race condition? | Incorrect behavior due to timing of shared mutable access |
| What is deadlock? | Threads waiting on each other's locks forever |
| Why unlock in finally? | Ensure lock release even if exception occurs |
| ThreadLocal risk? | Memory leak in thread pools if not removed |

---

## 17. ExecutorService And Thread Pools

### Why ExecutorService?

Creating raw threads manually is expensive and hard to manage.

ExecutorService gives:
- Thread reuse
- Task queueing
- Lifecycle management
- Futures
- Scheduling options

### Basic Example

```java
ExecutorService executor = Executors.newFixedThreadPool(3);

Future<Integer> future = executor.submit(() -> {
    return 10 + 20;
});

System.out.println(future.get()); // 30

executor.shutdown();
```

### execute vs submit

| execute | submit |
|---|---|
| Accepts Runnable | Accepts Runnable or Callable |
| Returns void | Returns Future |
| Exceptions go to uncaught handler | Exceptions captured in Future |

### shutdown vs shutdownNow

| shutdown | shutdownNow |
|---|---|
| Graceful shutdown | Attempts immediate shutdown |
| Stops accepting new tasks | Interrupts running tasks |
| Existing queued tasks continue | Returns queued tasks not started |

### Executor Types

| Executor | Use |
|---|---|
| `newFixedThreadPool(n)` | Fixed number of worker threads |
| `newCachedThreadPool()` | Creates threads as needed, reuses idle |
| `newSingleThreadExecutor()` | One worker, sequential execution |
| `newScheduledThreadPool(n)` | Delayed/periodic tasks |
| `newWorkStealingPool()` | ForkJoin-based work stealing |

### ThreadPoolExecutor Parameters

```java
ThreadPoolExecutor executor = new ThreadPoolExecutor(
    5,
    10,
    60,
    TimeUnit.SECONDS,
    new ArrayBlockingQueue<>(100),
    new ThreadPoolExecutor.CallerRunsPolicy()
);
```

Parameters:
- corePoolSize
- maximumPoolSize
- keepAliveTime
- timeUnit
- workQueue
- threadFactory
- rejectionHandler

### Rejection Policies

| Policy | Meaning |
|---|---|
| AbortPolicy | Throws RejectedExecutionException |
| CallerRunsPolicy | Caller thread runs task |
| DiscardPolicy | Silently drops task |
| DiscardOldestPolicy | Drops oldest queued task |

### Future Limitations

Future:
- Blocks with `get`
- No easy chaining
- No easy combining
- Error handling is clunky

CompletableFuture improves this.

### CompletableFuture Quick Example

```java
CompletableFuture<Integer> price = CompletableFuture.supplyAsync(() -> 1000);
CompletableFuture<Integer> tax = CompletableFuture.supplyAsync(() -> 180);

Integer total = price.thenCombine(tax, Integer::sum).join();
```

### Hot Questions

| Question | Strong Answer |
|---|---|
| Why thread pool? | Reuse threads, control concurrency, manage lifecycle |
| execute vs submit? | submit returns Future |
| Callable vs Runnable? | Callable returns result and throws checked exception |
| shutdown vs shutdownNow? | Graceful vs immediate attempt |
| Why avoid unbounded queues? | Can cause memory pressure |
| Fixed vs cached pool? | Fixed controls concurrency; cached can grow aggressively |
| What happens when queue full? | Rejection policy applies |

### Production Advice

```text
In backend services, avoid blindly using Executors factory methods for critical pools because
some create unbounded queues or unbounded threads. Prefer ThreadPoolExecutor with explicit
queue size, thread names, and rejection policy.
```

---

## 18. Producer-Consumer With BlockingQueue

### Why BlockingQueue?

It handles thread-safe producer-consumer coordination.

### Example

```java
import java.util.concurrent.*;

public class ProducerConsumerExample {
    public static void main(String[] args) {
        BlockingQueue<Integer> queue = new ArrayBlockingQueue<>(5);

        Runnable producer = () -> {
            try {
                for (int i = 1; i <= 10; i++) {
                    queue.put(i);
                    System.out.println("Produced " + i);
                }
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
            }
        };

        Runnable consumer = () -> {
            try {
                for (int i = 1; i <= 10; i++) {
                    Integer value = queue.take();
                    System.out.println("Consumed " + value);
                }
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
            }
        };

        new Thread(producer).start();
        new Thread(consumer).start();
    }
}
```

### Hot Interview Answer

```text
BlockingQueue is preferred over manual wait/notify for producer-consumer because it is
thread-safe, handles blocking, and reduces low-level concurrency bugs.
```

---

## 19. Serialization And Deserialization

### What Is Serialization?

Serialization converts an object into a byte stream.

Deserialization converts byte stream back into an object.

### Serializable Example

```java
import java.io.*;

class Employee implements Serializable {
    private static final long serialVersionUID = 1L;

    private int id;
    private String name;
    private transient String password;

    Employee(int id, String name, String password) {
        this.id = id;
        this.name = name;
        this.password = password;
    }
}
```

### `serialVersionUID`

Strong answer:

```text
serialVersionUID is a version identifier for serialized class compatibility. If class
structure changes and serialVersionUID mismatches, deserialization can fail with
InvalidClassException.
```

### transient

`transient` fields are not serialized.

Use for:
- Passwords
- Tokens
- Derived/calculated fields
- Non-serializable dependencies

### static Fields

Static fields are not part of object state, so they are not serialized as object data.

### Constructor During Deserialization

Strong answer:

```text
For Serializable objects, the constructor of the serializable class is not called during
deserialization. The first non-serializable superclass constructor is called.
```

### Custom Serialization

```java
private void writeObject(ObjectOutputStream out) throws IOException {
    out.defaultWriteObject();
}

private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
    in.defaultReadObject();
}
```

### Externalizable

```java
class User implements Externalizable {
    private String name;

    public User() {
    }

    @Override
    public void writeExternal(ObjectOutput out) throws IOException {
        out.writeObject(name);
    }

    @Override
    public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
        name = (String) in.readObject();
    }
}
```

### Serializable vs Externalizable

| Serializable | Externalizable |
|---|---|
| Marker interface | Has methods to implement |
| JVM handles default serialization | Developer controls serialization |
| No-arg constructor not required for serializable class | Public no-arg constructor required |
| Easier | More control |

### readResolve For Singleton

```java
class Singleton implements Serializable {
    private static final Singleton INSTANCE = new Singleton();

    private Singleton() {
    }

    static Singleton getInstance() {
        return INSTANCE;
    }

    private Object readResolve() {
        return INSTANCE;
    }
}
```

Without `readResolve`, deserialization can create a new singleton instance.

### Serialization Security

Strong answer:

```text
Java native deserialization can be dangerous with untrusted data because it may allow gadget
chain attacks. In modern backend systems, JSON or safer formats are often preferred, and
native deserialization should be restricted.
```

### Hot Questions

| Question | Strong Answer |
|---|---|
| What is Serializable? | Marker interface for object serialization |
| What is serialVersionUID? | Version compatibility identifier |
| transient field? | Skipped during serialization |
| Are static fields serialized? | No, they belong to class |
| Constructor called during deserialization? | Serializable class constructor is not called |
| How protect singleton? | Implement `readResolve` |
| Serializable vs Externalizable? | Default marker vs explicit control |

---

## 20. Garbage Collection

### What Is GC?

Garbage collection automatically reclaims heap memory from objects that are no longer reachable.

### When Is Object Eligible For GC?

When no live thread can reach it through GC roots.

Example:

```java
Employee emp = new Employee();
emp = null;
```

If no other reference exists, object is eligible for GC.

### GC Roots

Common GC roots:
- Local variables in active stack frames
- Static fields
- Active threads
- JNI references
- References from class metadata
- Monitor locks

### Generational Hypothesis

Most objects die young.

Heap is commonly divided into:
- Young generation
- Old generation

Young generation:
- Eden
- Survivor spaces

### Minor, Major, Full GC

| Type | Meaning |
|---|---|
| Minor GC | Cleans young generation |
| Major GC | Cleans old generation |
| Full GC | Cleans entire heap/metaspace related areas depending collector |

### GC Algorithms

| Algorithm | Idea |
|---|---|
| Mark | Find reachable objects |
| Sweep | Remove unreachable objects |
| Compact | Move objects to reduce fragmentation |
| Copy | Copy live objects to another space |

### Common Collectors

| Collector | Use |
|---|---|
| Serial GC | Simple, single-threaded |
| Parallel GC | Throughput-focused |
| G1 GC | Balanced, default in many modern JDKs |
| ZGC | Low-latency collector |
| Shenandoah | Low-pause collector |

### Strong G1 Answer

```text
G1 divides heap into regions and tries to meet pause-time goals by collecting regions with
the most garbage first. It is commonly used for server applications.
```

### Strong ZGC/Shenandoah Answer

```text
ZGC and Shenandoah are low-pause collectors designed for large heaps and latency-sensitive
applications. They do more work concurrently with application threads.
```

### Memory Leak In Java

Java can still have memory leaks if objects remain reachable but are no longer useful.

Examples:
- Static collection keeps growing
- ThreadLocal not removed in thread pool
- Listeners/callbacks not unregistered
- Cache without eviction
- Unclosed resources
- ClassLoader leaks in app servers

### ThreadLocal Leak Example

```java
private static final ThreadLocal<String> context = new ThreadLocal<>();

void process() {
    try {
        context.set("REQ-1");
        // work
    } finally {
        context.remove();
    }
}
```

### Reference Types

| Type | Meaning |
|---|---|
| Strong reference | Normal reference, prevents GC |
| Soft reference | Cleared under memory pressure |
| Weak reference | Cleared when only weakly reachable |
| Phantom reference | Used for post-mortem cleanup tracking |

### WeakHashMap

Keys are weakly referenced.

```java
Map<Object, String> map = new WeakHashMap<>();
```

When key has no strong references elsewhere, entry can be removed by GC.

### GC Tools

| Tool | Use |
|---|---|
| `jstat` | GC stats |
| `jmap` | Heap dump |
| `jcmd` | JVM diagnostics |
| `jvisualvm` | Visual monitoring |
| Java Flight Recorder | Production profiling |
| Mission Control | Analyze JFR recordings |

### Hot Questions

| Question | Strong Answer |
|---|---|
| Can we force GC? | No, `System.gc()` is only a request |
| Is finalize reliable? | No, deprecated/unreliable |
| Can Java have memory leaks? | Yes, reachable unused objects |
| What is GC root? | Starting point for reachability analysis |
| What is minor GC? | Young generation collection |
| Why generations? | Most objects die young |

---

## 21. Reflection And Annotations

### Reflection

Reflection lets code inspect and manipulate classes, methods, fields, and constructors at runtime.

Example:

```java
Class<?> clazz = Class.forName("com.example.Employee");
Object obj = clazz.getDeclaredConstructor().newInstance();
```

### Use Cases

- Frameworks like Spring
- Dependency injection
- Serialization libraries
- Testing tools
- ORM frameworks

### Downsides

- Slower than direct calls
- Breaks encapsulation if abused
- Harder to refactor
- Security restrictions
- Runtime errors instead of compile-time errors

### Annotation

Annotation is metadata.

Example:

```java
@Override
public String toString() {
    return "Employee";
}
```

Custom annotation:

```java
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
@interface Auditable {
}
```

### Retention Policies

| Policy | Meaning |
|---|---|
| SOURCE | Available only in source code |
| CLASS | Stored in class file, not necessarily runtime |
| RUNTIME | Available at runtime via reflection |

### Hot Questions

| Question | Strong Answer |
|---|---|
| Why Spring uses reflection? | To inspect annotations, create beans, inject dependencies |
| Reflection downside? | Performance, encapsulation, runtime errors |
| What is annotation? | Metadata for compiler/runtime/tools |
| Runtime annotation needs? | `@Retention(RetentionPolicy.RUNTIME)` |

---

## 22. Singleton Pattern

### Eager Singleton

```java
class EagerSingleton {
    private static final EagerSingleton INSTANCE = new EagerSingleton();

    private EagerSingleton() {
    }

    static EagerSingleton getInstance() {
        return INSTANCE;
    }
}
```

### Lazy Singleton With Double-Checked Locking

```java
class LazySingleton {
    private static volatile LazySingleton instance;

    private LazySingleton() {
    }

    static LazySingleton getInstance() {
        if (instance == null) {
            synchronized (LazySingleton.class) {
                if (instance == null) {
                    instance = new LazySingleton();
                }
            }
        }
        return instance;
    }
}
```

Why `volatile`?

```text
To prevent visibility and instruction reordering issues during object initialization.
```

### Enum Singleton

```java
enum AppConfig {
    INSTANCE;

    void print() {
        System.out.println("Config");
    }
}
```

Strong answer:

```text
Enum singleton is simple and protects against reflection and serialization issues better
than many manual singleton implementations.
```

### Singleton Breaking Methods

- Reflection
- Serialization/deserialization
- Cloning
- Multiple ClassLoaders

Fixes:
- Enum singleton
- `readResolve`
- Prevent clone
- Guard constructor

---

## 23. Copying And Cloning

### Shallow Copy

Copies object fields, but nested object references are shared.

### Deep Copy

Copies object and nested objects.

### Example

```java
class Address {
    String city;
}

class User {
    String name;
    Address address;
}
```

If shallow copied:
- User object is new.
- Address object is shared.

If deep copied:
- User object is new.
- Address object is also new.

### Clone Problems

`Cloneable` has design issues:
- Marker interface
- `clone` is protected in Object
- Shallow by default
- Constructor not called

Interview answer:

```text
I prefer copy constructors, factory methods, or serialization/mapping-based copies over
Cloneable for most production code.
```

### Copy Constructor

```java
class Address {
    String city;

    Address(String city) {
        this.city = city;
    }

    Address(Address other) {
        this.city = other.city;
    }
}
```

---

## 24. Must-Know Java 8+ Core Questions

Streams and modern Java are covered in separate sheets, but these are high-frequency quick answers.

### Lambda

```text
Lambda is a concise implementation of a functional interface.
```

### Functional Interface

```text
An interface with exactly one abstract method. It can have default and static methods.
```

### Optional

```text
Optional represents a value that may be present or absent. Best used as a return type,
not usually as a field or method parameter.
```

### `orElse` vs `orElseGet`

```text
orElse evaluates default eagerly. orElseGet calls supplier lazily only when Optional is empty.
```

### Stream Laziness

```text
Intermediate stream operations are lazy. They execute only when a terminal operation is called.
```

### map vs flatMap

```text
map is one-to-one transformation. flatMap is one-to-many transformation plus flattening.
```

---

## 25. Rapid Hot Questions By Topic

### JVM And Memory

| Question | Answer |
|---|---|
| JDK vs JRE vs JVM? | JDK develops, JRE runs, JVM executes bytecode |
| Is Java platform independent? | Bytecode is; JVM is platform-specific |
| What is bytecode? | Platform-independent `.class` instructions |
| What is JIT? | Runtime compiler for hot bytecode |
| Heap vs stack? | Objects vs method frames/local refs |
| Object stored where? | Heap |
| Reference stored where? | Depends; local reference on stack, field reference inside object on heap |
| Static variable lifecycle? | Class-level, tied to class loading/unloading |
| String pool where? | Heap in modern Java |
| StackOverflowError cause? | Deep/infinite recursion |
| OutOfMemoryError cause? | Insufficient heap/metaspace/native memory |

### OOP And Keywords

| Question | Answer |
|---|---|
| static method overridden? | No, hidden |
| final class? | Cannot extend |
| final method? | Cannot override |
| final variable? | Cannot reassign |
| final object immutable? | Not necessarily |
| private method overridden? | No |
| constructor inherited? | No |
| abstract class constructor? | Yes |
| interface constructor? | No |
| class vs interface default conflict? | Class method wins |

### Collections

| Question | Answer |
|---|---|
| HashMap default capacity? | 16 |
| HashMap load factor? | 0.75 |
| HashMap null support? | One null key, many null values |
| ConcurrentHashMap null support? | No null keys/values |
| HashSet internally? | HashMap |
| ArrayList vs LinkedList? | Dynamic array vs linked nodes |
| TreeMap complexity? | `O(log n)` |
| Fail-fast? | Best-effort concurrent modification detection |
| Comparable vs Comparator? | Natural vs external ordering |

### Multithreading

| Question | Answer |
|---|---|
| start vs run? | start creates new thread |
| sleep releases lock? | No |
| wait releases lock? | Yes |
| volatile? | Visibility, not atomicity |
| synchronized? | Mutual exclusion + visibility |
| AtomicInteger? | Atomic CAS-based operations |
| Deadlock? | Threads waiting forever on each other's locks |
| ExecutorService why? | Thread reuse and lifecycle management |
| submit vs execute? | submit returns Future |
| shutdown vs shutdownNow? | Graceful vs immediate attempt |

### Serialization

| Question | Answer |
|---|---|
| Serializable? | Marker interface |
| serialVersionUID? | Version compatibility |
| transient? | Field not serialized |
| static serialized? | No |
| Constructor called? | Serializable class constructor not called |
| Singleton issue? | Deserialization can create new instance |
| Fix singleton serialization? | `readResolve` or enum |

### GC

| Question | Answer |
|---|---|
| Eligible for GC? | No longer reachable from GC roots |
| Force GC? | Cannot force, only request |
| Java memory leak? | Yes, reachable unused objects |
| GC roots? | Stack locals, statics, active threads, JNI refs |
| Minor GC? | Young generation |
| Full GC? | Larger whole-heap style collection |
| finalize? | Deprecated and unreliable |

---

## 26. Mini Programs To Practice

### Program 1: Immutable Class

```java
import java.util.*;

final class BookingSummary {
    private final String bookingId;
    private final List<String> guestNames;

    BookingSummary(String bookingId, List<String> guestNames) {
        this.bookingId = bookingId;
        this.guestNames = new ArrayList<>(guestNames);
    }

    String getBookingId() {
        return bookingId;
    }

    List<String> getGuestNames() {
        return new ArrayList<>(guestNames);
    }
}
```

### Program 2: Equals And HashCode

```java
import java.util.*;

class Room {
    private final String roomNumber;

    Room(String roomNumber) {
        this.roomNumber = roomNumber;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof Room)) {
            return false;
        }
        Room other = (Room) obj;
        return Objects.equals(roomNumber, other.roomNumber);
    }

    @Override
    public int hashCode() {
        return Objects.hash(roomNumber);
    }
}
```

### Program 3: Thread-Safe Counter

```java
import java.util.concurrent.atomic.AtomicInteger;

class SafeCounter {
    private final AtomicInteger count = new AtomicInteger();

    int increment() {
        return count.incrementAndGet();
    }

    int get() {
        return count.get();
    }
}
```

### Program 4: ExecutorService

```java
import java.util.concurrent.*;

public class ExecutorExample {
    public static void main(String[] args) throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(2);

        Future<String> future = executor.submit(() -> {
            Thread.sleep(500);
            return "Done";
        });

        System.out.println(future.get());
        executor.shutdown();
    }
}
```

### Program 5: Custom ThreadPoolExecutor

```java
import java.util.concurrent.*;

public class CustomThreadPoolExample {
    public static void main(String[] args) {
        ThreadPoolExecutor executor = new ThreadPoolExecutor(
            2,
            4,
            30,
            TimeUnit.SECONDS,
            new ArrayBlockingQueue<>(10),
            new ThreadPoolExecutor.CallerRunsPolicy()
        );

        executor.submit(() -> System.out.println("Task executed"));
        executor.shutdown();
    }
}
```

### Program 6: Serialization

```java
import java.io.*;

class Booking implements Serializable {
    private static final long serialVersionUID = 1L;

    private String id;
    private transient String token;

    Booking(String id, String token) {
        this.id = id;
        this.token = token;
    }
}
```

---

## 27. Interview Traps To Memorize

| Trap | Correct Answer |
|---|---|
| Java is pass-by-reference | No, Java is pass-by-value |
| `final List` means immutable list | No, reference cannot change but list can mutate |
| Static methods override | No, they hide |
| `volatile int count++` is thread-safe | No, volatile does not make compound operations atomic |
| `System.gc()` forces GC | No, only requests |
| `Optional.get()` is safe | Only if value present; avoid direct get |
| HashMap is thread-safe | No |
| ConcurrentHashMap allows null | No |
| `equals` without `hashCode` is fine | Not for hash-based collections |
| StringBuilder is synchronized | No, StringBuffer is synchronized |
| Deserialization calls constructor normally | Not for Serializable class |
| `List.of` returns mutable list | No |
| Reflection is compile-time safe | No, many errors move to runtime |
| Parallel stream always improves performance | No |
| ThreadLocal automatically cleans itself | No, remove in thread pools |

---

## 28. One-Hour Java Core Revision Plan

### First 15 Minutes

Revise:
- JDK/JRE/JVM
- Java execution flow
- Heap vs stack
- Static memory
- Class loading

### Next 15 Minutes

Revise:
- `static`
- `final/finally/finalize`
- OOP
- Immutable class
- `equals/hashCode`

### Next 15 Minutes

Revise:
- HashMap internals
- Collections comparison
- Exceptions
- Generics PECS

### Last 15 Minutes

Revise:
- Multithreading basics
- `volatile` vs `synchronized`
- ExecutorService
- Serialization
- GC and memory leaks

---

## 29. Strong Closing Answer For Java Core Round

If interviewer asks:

```text
How strong are you in core Java?
```

Say:

```text
I am comfortable with core Java fundamentals used in backend systems: JVM execution flow,
memory areas, class loading, OOP, collections, HashMap internals, exceptions, generics,
and Java concurrency. In day-to-day work, I pay special attention to equals/hashCode
correctness, immutability, thread safety, executor sizing, and avoiding memory leaks through
unbounded collections, ThreadLocal misuse, or poor resource handling.
```

This sounds practical and senior enough for a 4+ year Java backend role.
