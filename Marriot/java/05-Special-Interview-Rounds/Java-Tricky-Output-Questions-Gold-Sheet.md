# Java Tricky Output Questions Gold Sheet

Target: Java "what is the output?" and "why?" interview questions.

This sheet is for fast trap revision.

How to use:

1. Read the code.
2. Predict the output before reading the answer.
3. Explain the reason aloud.
4. Memorize the trap line.

---

## 1. Output Question Strategy

When you see a Java output question, check in this order:

```text
1. Is it about reference vs object?
2. Is it about compile-time vs runtime behavior?
3. Is it about static initialization or constructor order?
4. Is it about overloading vs overriding?
5. Is it about boxing/unboxing or numeric promotion?
6. Is it about equals/hashCode?
7. Is it about finally/exception flow?
8. Is it about streams laziness?
9. Is it about generics type erasure?
10. Is it about concurrency visibility or atomicity?
```

Memory line:

```text
Output questions test rules, not luck.
```

---

## 2. String Pool: Literal vs New

Code:

```java
String a = "java";
String b = "java";
String c = new String("java");

System.out.println(a == b);
System.out.println(a == c);
System.out.println(a.equals(c));
```

Output:

```text
true
false
true
```

Why:

```text
a and b refer to the same pooled literal. c is a new heap object. equals compares content.
```

Trap:

```text
== compares references, not String content.
```

---

## 3. Compile-Time String Concatenation

Code:

```java
String a = "ja" + "va";
String b = "java";

System.out.println(a == b);
```

Output:

```text
true
```

Why:

```text
"ja" + "va" is a compile-time constant expression. The compiler folds it to "java".
```

Trap:

```text
Compile-time constants can make == appear to work for Strings.
```

---

## 4. Runtime String Concatenation

Code:

```java
String part = "ja";
String a = part + "va";
String b = "java";

System.out.println(a == b);
System.out.println(a.equals(b));
```

Output:

```text
false
true
```

Why:

```text
part is a runtime variable, so concatenation creates a runtime result object.
```

Trap:

```text
Runtime concatenation is different from compile-time folding.
```

---

## 5. Final Compile-Time Constant

Code:

```java
final String part = "ja";
String a = part + "va";
String b = "java";

System.out.println(a == b);
```

Output:

```text
true
```

Why:

```text
final String part initialized with a literal is a compile-time constant, so the expression
can be folded.
```

Trap:

```text
final can enable constant folding only when the value is a compile-time constant.
```

---

## 6. Integer Cache

Code:

```java
Integer a = 127;
Integer b = 127;
Integer c = 128;
Integer d = 128;

System.out.println(a == b);
System.out.println(c == d);
```

Output:

```text
true
false
```

Why:

```text
Integer values from -128 to 127 are commonly cached. 128 creates separate boxed objects.
```

Trap:

```text
Use equals for wrapper value comparison.
```

---

## 7. Autoboxing And NullPointerException

Code:

```java
Integer value = null;
int result = value;

System.out.println(result);
```

Output:

```text
NullPointerException
```

Why:

```text
Unboxing null Integer to int throws NullPointerException.
```

Trap:

```text
Autounboxing can hide null dereference.
```

---

## 8. Method Overloading With Null

Code:

```java
class Test {
    static void print(String value) {
        System.out.println("String");
    }

    static void print(Object value) {
        System.out.println("Object");
    }

    public static void main(String[] args) {
        print(null);
    }
}
```

Output:

```text
String
```

Why:

```text
Overloading is compile-time. Java chooses the most specific matching type. String is more
specific than Object.
```

Trap:

```text
Overloading resolution happens at compile time.
```

---

## 9. Ambiguous Overload With Null

Code:

```java
class Test {
    static void print(String value) {
        System.out.println("String");
    }

    static void print(Integer value) {
        System.out.println("Integer");
    }

    public static void main(String[] args) {
        print(null);
    }
}
```

Output:

```text
Compilation error
```

Why:

```text
String and Integer are unrelated reference types. Both match null, but neither is more specific.
```

Trap:

```text
null overloads become ambiguous when matching types are unrelated.
```

---

## 10. Overloading vs Overriding

Code:

```java
class Parent {
    void show(Object value) {
        System.out.println("Parent Object");
    }
}

class Child extends Parent {
    void show(String value) {
        System.out.println("Child String");
    }
}

public class Test {
    public static void main(String[] args) {
        Parent obj = new Child();
        obj.show("java");
    }
}
```

Output:

```text
Parent Object
```

Why:

```text
Child.show(String) overloads, not overrides Parent.show(Object). Method selection uses
reference type at compile time for overload resolution.
```

Trap:

```text
Overloading is compile-time; overriding is runtime.
```

---

## 11. Static Method Hiding

Code:

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

public class Test {
    public static void main(String[] args) {
        Parent obj = new Child();
        obj.show();
    }
}
```

Output:

```text
Parent
```

Why:

```text
Static methods are hidden, not overridden. The method is resolved using reference type.
```

Trap:

```text
Static methods do not participate in runtime polymorphism.
```

---

## 12. Constructor Order

Code:

```java
class Parent {
    Parent() {
        System.out.println("Parent constructor");
    }
}

class Child extends Parent {
    Child() {
        System.out.println("Child constructor");
    }
}

public class Test {
    public static void main(String[] args) {
        new Child();
    }
}
```

Output:

```text
Parent constructor
Child constructor
```

Why:

```text
Parent constructor runs before child constructor.
```

Trap:

```text
Object construction starts from parent state, then child state.
```

---

## 13. Static Block And Instance Block

Code:

```java
class Test {
    static {
        System.out.println("static");
    }

    {
        System.out.println("instance");
    }

    Test() {
        System.out.println("constructor");
    }

    public static void main(String[] args) {
        new Test();
        new Test();
    }
}
```

Output:

```text
static
instance
constructor
instance
constructor
```

Why:

```text
Static block runs once when class loads. Instance block runs before constructor for every object.
```

Trap:

```text
Static once, instance per object.
```

---

## 14. Finally Overrides Return

Code:

```java
static int test() {
    try {
        return 1;
    } finally {
        return 2;
    }
}

public static void main(String[] args) {
    System.out.println(test());
}
```

Output:

```text
2
```

Why:

```text
finally executes before method completes and its return overrides the try return.
```

Trap:

```text
Never return from finally in production code.
```

---

## 15. Finally With Mutation

Code:

```java
static int test() {
    int x = 1;
    try {
        return x;
    } finally {
        x = 2;
    }
}

public static void main(String[] args) {
    System.out.println(test());
}
```

Output:

```text
1
```

Why:

```text
The return value is already prepared before finally changes local variable x.
```

Trap:

```text
Mutating a local variable in finally does not necessarily change the prepared return value.
```

---

## 16. Catch Order

Code:

```java
try {
    throw new RuntimeException();
} catch (Exception e) {
    System.out.println("Exception");
} catch (RuntimeException e) {
    System.out.println("RuntimeException");
}
```

Output:

```text
Compilation error
```

Why:

```text
RuntimeException catch block is unreachable because Exception catches it first.
```

Trap:

```text
Catch specific exceptions before general exceptions.
```

---

## 17. HashMap Mutable Key

Code:

```java
import java.util.*;

class Key {
    String id;

    Key(String id) {
        this.id = id;
    }

    public boolean equals(Object obj) {
        return obj instanceof Key other && Objects.equals(id, other.id);
    }

    public int hashCode() {
        return Objects.hash(id);
    }
}

public class Test {
    public static void main(String[] args) {
        Key key = new Key("A");
        Map<Key, String> map = new HashMap<>();
        map.put(key, "value");

        key.id = "B";

        System.out.println(map.get(key));
    }
}
```

Output:

```text
null
```

Why:

```text
The key's hashCode changed after insertion, so lookup searches the wrong bucket.
```

Trap:

```text
HashMap keys should be immutable.
```

---

## 18. Stream Laziness

Code:

```java
List<String> names = List.of("A", "B", "C");

names.stream()
    .peek(System.out::println)
    .map(String::toLowerCase);
```

Output:

```text
No output
```

Why:

```text
There is no terminal operation. Intermediate operations are lazy.
```

Trap:

```text
No terminal operation, no stream execution.
```

---

## 19. Stream Short-Circuit

Code:

```java
List<Integer> nums = List.of(1, 2, 3, 4);

boolean result = nums.stream()
    .peek(System.out::println)
    .anyMatch(n -> n == 2);

System.out.println(result);
```

Output:

```text
1
2
true
```

Why:

```text
anyMatch short-circuits once it finds a match.
```

Trap:

```text
Streams can short-circuit depending on terminal operation.
```

---

## 20. `Arrays.asList` Fixed Size

Code:

```java
List<String> list = Arrays.asList("A", "B");
list.add("C");
```

Output:

```text
UnsupportedOperationException
```

Why:

```text
Arrays.asList returns a fixed-size list backed by the array.
```

Trap:

```text
Fixed-size does not mean fully immutable.
```

---

## 21. `List.of` Null

Code:

```java
List<String> list = List.of("A", null);
```

Output:

```text
NullPointerException
```

Why:

```text
List.of does not allow null elements.
```

Trap:

```text
Modern collection factories reject nulls and return unmodifiable collections.
```

---

## 22. Generics Type Erasure

Code:

```java
List<String> names = new ArrayList<>();
List<Integer> numbers = new ArrayList<>();

System.out.println(names.getClass() == numbers.getClass());
```

Output:

```text
true
```

Why:

```text
Generic type information is erased at runtime. Both are ArrayList at runtime.
```

Trap:

```text
Generics are mostly compile-time type safety.
```

---

## 23. Pass By Value

Code:

```java
static void change(StringBuilder sb) {
    sb.append("B");
    sb = new StringBuilder("C");
}

public static void main(String[] args) {
    StringBuilder sb = new StringBuilder("A");
    change(sb);
    System.out.println(sb);
}
```

Output:

```text
AB
```

Why:

```text
Java passes the reference value by value. The method can mutate the object, but reassigning
the parameter does not change the caller's reference.
```

Trap:

```text
Java is pass-by-value, always.
```

---

## 24. Autoboxing With `==`

Code:

```java
Integer a = 1000;
int b = 1000;

System.out.println(a == b);
```

Output:

```text
true
```

Why:

```text
a is unboxed to int, then primitive comparison happens.
```

Trap:

```text
Wrapper vs primitive comparison can trigger unboxing.
```

---

## 25. Thread Start vs Run

Code:

```java
Thread t = new Thread(() -> System.out.println(Thread.currentThread().getName()));
t.run();
```

Output:

```text
main
```

Why:

```text
run is a normal method call. start creates a new thread.
```

Trap:

```text
Use start to begin new thread execution.
```

---

## 26. Volatile Counter

Code:

```java
class Counter {
    volatile int count;

    void increment() {
        count++;
    }
}
```

Question:

```text
Is this thread-safe?
```

Answer:

```text
No. volatile gives visibility, but count++ is not atomic.
```

Trap:

```text
Visibility is not atomicity.
```

---

## 27. Rapid Trap Table

| Trap | Correct Thought |
|---|---|
| String `==` | Reference comparison |
| Wrapper `==` | Cache/unboxing may affect result |
| Overloading | Compile-time |
| Overriding | Runtime |
| Static methods | Hidden, not overridden |
| final reference | Cannot reassign, object may mutate |
| finally return | Can override try/catch return |
| HashMap key mutation | Breaks lookup |
| Stream without terminal | Does not run |
| `toMap` duplicate key | Throws unless merge function exists |
| `Arrays.asList` | Fixed-size list |
| `List.of` | Unmodifiable and no nulls |
| Generics | Type erasure |
| Java parameters | Pass-by-value |
| volatile | Visibility, not atomicity |

---

## 28. Final Interview Line

If interviewer gives tricky output code, say:

```text
I will first identify whether the code is testing compile-time resolution, runtime dispatch,
reference equality, initialization order, exception flow, stream laziness, or concurrency
visibility. Then I will reason from the Java rule instead of guessing the output.
```
