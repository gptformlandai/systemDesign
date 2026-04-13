# Java String Deep Dive

This note explains Java `String` from first principles and then goes into the tricky parts people usually memorize without really understanding.

The goal is to answer these questions clearly:

1. Why is `String` immutable?
2. What exactly is the String Pool?
3. In which memory area do strings live?
4. How is `String s = "a";` different from `new String("a")`?
5. What happens when we reassign or concatenate strings?
6. What tricky cases matter in real code and interviews?

---

## 1. First Mental Model

In Java, a `String` variable does not hold the characters directly.
It holds a reference to a `String` object.

Conceptually:

```text
stack frame / local variable area
s  ----------------------->  heap String object
```

Important distinction:

- The reference is what your variable stores.
- The actual `String` object is an object on the heap.

Also, `String` is a `final` class, so it cannot be subclassed.

Inside the JDK, `String` is immutable. In modern Java, the internal storage is based on a `byte[]` plus a small coder flag. In older Java versions, it was commonly explained as a `char[]`.

That version detail is interesting, but the core rule stays the same:

> Once a `String` object is created, its textual content does not change.

---

## 2. Why `String` Is Immutable

Java made `String` immutable on purpose. This is not just a design preference. It enables several important behaviors.

### 2.1 Pooling becomes safe

If many references can point to the same `String` object, then that object must not change.

Example:

```java
String a = "optum";
String b = "optum";
```

If Java lets both variables point to the same pooled object, then changing `a` must not silently change `b`.

Immutability makes sharing safe.

### 2.2 Hash code caching becomes safe

`String` is used heavily as a key in `HashMap`, `HashSet`, caches, headers, config maps, and JSON-like structures.

If a string could change after being inserted into a hash-based collection, the object could end up in the wrong bucket.

Because `String` is immutable, Java can compute and cache its hash code safely.

### 2.3 Thread safety becomes much easier

Two threads can read the same `String` object without coordination because no thread can mutate it.

That does not make every program thread-safe, but it makes `String` itself safe to share.

### 2.4 Security-sensitive values stay stable

Paths, URLs, class names, SQL fragments, host names, and class loader inputs are often represented as strings.

If code validates one value and some other code later mutates it, security breaks down.

Immutability prevents that category of bug.

### 2.5 Important real-world nuance: immutable is not always ideal

`String` is good for identity-like text.

`String` is not ideal for secrets such as passwords because you cannot wipe its contents explicitly after use. That is why APIs often prefer `char[]` for sensitive data.

---

## 3. Three Concepts People Mix Up

When people say "constant pool" and "string pool," they often mix three different things.

| Concept | What it is | Where it exists |
|---|---|---|
| Class file constant pool | Entries stored in the compiled `.class` file, including string literals and symbolic references | In the `.class` file on disk |
| Runtime constant pool | JVM runtime representation of class constants and symbolic references | JVM runtime data area associated with class metadata |
| String Pool | Canonical table of `String` objects used for sharing equal string values | Heap in modern HotSpot JVMs |

This distinction matters.

### 3.1 Class file constant pool

When you compile this code:

```java
String s = "a";
```

the literal `"a"` is recorded in the class file's constant pool.

That does not mean a heap `String` object already exists at compile time.

### 3.2 Runtime constant pool

When the class is loaded, the JVM builds runtime structures from the class file metadata. The runtime constant pool helps resolve constants and symbolic references while executing bytecode.

### 3.3 String Pool

The String Pool is where canonical `String` objects live.

If the JVM decides the string value `"a"` should be represented by a pooled object, that pooled `String` object is a heap object.

### 3.4 Where are strings stored in memory?

The short version for modern Java is:

- Local reference variables are in the current stack frame conceptually.
- `String` objects are on the heap.
- The String Pool is also on the heap in Java 7 and later.
- Class metadata is stored separately from normal heap objects.

Version note:

- In older HotSpot JVMs such as Java 6, the String Pool was associated with PermGen.
- From Java 7 onward, the String Pool moved to the heap.
- In Java 8+, PermGen was removed and replaced by Metaspace for class metadata.

---

## 4. What Happens in `String s = "a";`

This line looks simple, but several things are happening.

```java
String s = "a";
```

### Step-by-step

1. The compiler stores the literal `"a"` in the class file constant pool.
2. At runtime, the JVM executes bytecode that refers to that literal.
3. The JVM resolves that literal to a canonical `String` object from the String Pool.
4. If that pooled object does not exist yet, it is created and recorded.
5. `s` now points to that pooled object.

Conceptually:

```text
class constant data  -->  "a"
                           |
                           v
                    String Pool entry for "a"
                           |
                           v
                        s points here
```

Now look at this:

```java
String s1 = "a";
String s2 = "a";
System.out.println(s1 == s2);      // true
System.out.println(s1.equals(s2)); // true
```

Why is `==` true here?

Because both references point to the same pooled object.

Important technical nuance:

The source code contains a literal. The actual heap `String` object is materialized and resolved by the JVM when needed. Do not think of a string literal as a raw piece of memory sitting in the heap just because you typed quotes in source code.

---

## 5. What Happens in `String s = new String("a");`

Now compare the literal form with explicit object creation.

```java
String s = new String("a");
```

This is not the same as the literal-only version.

### Step-by-step

1. The literal `"a"` is still resolved through the String Pool.
2. Then `new String(...)` creates a separate `String` object on the heap.
3. Your variable `s` points to the newly created object, not the pooled one.

So this code typically involves two distinct `String` objects:

- one pooled object for `"a"`
- one additional heap object created by `new`

Example:

```java
String pooled = "a";
String created = new String("a");

System.out.println(pooled == created);      // false
System.out.println(pooled.equals(created)); // true
```

### Tricky detail

The two `String` objects are distinct objects, but modern JDK implementations may share internal backing storage because the content is immutable. So object identity is different even if some internal bytes are reused.

### Practical rule

Avoid `new String("...")` unless you have a very specific reason.
In normal application code, it is almost always unnecessary.

---

## 6. Reassignment Does Not Mutate the Old String

Consider this code:

```java
String s = "a";
s = "b";
```

What changed?

Not the original string object.
Only the reference changed.

Before reassignment:

```text
s  ---->  pooled "a"
```

After reassignment:

```text
s  ---->  pooled "b"

pooled "a" still exists unchanged
```

That is a core idea:

> Reassigning a `String` variable changes what the variable points to. It does not modify the existing string object.

Real-world consequence:

- If other references still point to the old string, they keep seeing the old value.
- If no live references point to a non-pooled string object, it can become garbage-collection eligible.

For literals, reachability often lasts as long as the relevant class remains loaded.

---

## 7. Concatenation: Compile-Time vs Runtime

This is one of the most important interview and debugging topics.

Not every concatenation behaves the same way.

### 7.1 Compile-time constant concatenation

```java
String x = "a" + "b";
String y = "ab";

System.out.println(x == y); // true
```

Why?

Because `"a" + "b"` is a compile-time constant expression.
The compiler folds it into `"ab"`.

So effectively the compiler treats it like:

```java
String x = "ab";
String y = "ab";
```

Both point to the same pooled object.

### 7.2 Runtime concatenation with a non-final variable

```java
String a = "a";
String b = a + "b";
String c = "ab";

System.out.println(b == c);      // false
System.out.println(b.equals(c)); // true
```

Why is `==` false now?

Because `a` is a variable, not a compile-time constant expression.
So `a + "b"` is built at runtime.
The result is usually a new `String` object, not automatically a pooled one.

### 7.3 Runtime concatenation with a compile-time constant variable

```java
final String a = "a";
String b = a + "b";
String c = "ab";

System.out.println(b == c); // true
```

Because `a` is `final` and initialized with a constant expression, the compiler can still fold the result into `"ab"`.

That is a classic trap.

### 7.4 How concatenation is implemented

At the language level, runtime concatenation produces a new string result.

Historically, Java compiled many concatenations into `StringBuilder` operations.
In modern Java, the compiler often uses `invokedynamic` with `StringConcatFactory`.

The implementation strategy may differ by Java version, but the practical rule remains:

- compile-time constant concatenation can reuse the pool
- runtime concatenation usually creates a new result object

### 7.5 `concat()` is also runtime work

```java
String x = "a".concat("b");
String y = "ab";

System.out.println(x == y);      // normally false
System.out.println(x.equals(y)); // true
```

`concat()` is a method call. It does not turn into a compile-time literal.

Tricky detail:

```java
String x = "a";
String y = x.concat("");
System.out.println(x == y); // true
```

If the argument is empty, `concat()` can return the same instance.

---

## 8. Step-by-Step: `s = s + "b"`

Consider:

```java
String s = "a";
s = s + "b";
```

What actually happens?

1. `s` first points to pooled `"a"`.
2. `s + "b"` is evaluated.
3. Because this is runtime concatenation, a new result string like `"ab"` is produced.
4. `s` is reassigned to point to that new result.
5. The original pooled `"a"` remains unchanged.

Now compare it with a literal:

```java
String s = "a";
s = s + "b";
String t = "ab";

System.out.println(s == t);      // false
System.out.println(s.equals(t)); // true
```

That surprises many people.

The pooled literal `"ab"` and the runtime-built result `"ab"` have the same content, but they are not automatically the same object.

If you really want the pooled canonical reference, you must call `intern()`.

---

## 9. `intern()` and the String Pool in Detail

`intern()` is the API that explicitly asks for the canonical pooled version of a string.

```java
String s1 = new String("hello");
String s2 = s1.intern();
String s3 = "hello";

System.out.println(s2 == s3); // true
```

### What `intern()` does conceptually

1. Look in the String Pool for an equal string.
2. If found, return the pooled reference.
3. If not found, add a canonical entry and return that reference.

### Why this matters

It lets many equal strings share one canonical object.

This can reduce duplicate objects in workloads with repeated identifiers such as:

- country codes
- protocol tokens
- event types
- tenant IDs
- small repeated dictionary words

### Important nuance in modern Java

In HotSpot from Java 7 onward, `intern()` can store or reuse a heap object directly rather than creating a separate permanent copy in PermGen.

That changed the memory story compared with older Java versions.

### Example where `intern()` can surprise you

```java
String dynamic = new StringBuilder().append("or").append("der").toString();
String pooled = dynamic.intern();

System.out.println(dynamic.equals(pooled)); // true
```

Identity depends on whether that value already existed in the pool before `intern()` ran.

- If the value was already present, `pooled` points to the existing canonical object.
- If it was not present, modern JVMs may record the current object as the canonical one.

So `dynamic == pooled` can be either `true` or `false` depending on timing and prior pool state.

### Real-world caution

Do not blindly call `intern()` on huge volumes of unbounded user input.

Pooling is useful when the set of values is small and highly repetitive.
It is a bad idea when the set of values is massive and mostly unique.

---

## 10. How the String Pool Works Internally

The String Pool is best understood as a canonicalization table.

Conceptually:

```text
requested string content  --->  pool lookup by content
                               /                 \
                        found existing       not found
                               |                 |
                               v                 v
                    return canonical ref   create/store canonical ref
```

It is not just "some special place for literals."

It is a sharing mechanism based on string content.

### Key characteristics

- It is content-based, not variable-name-based.
- Equal pooled strings can share one canonical object.
- The pool is shared at JVM level, not per local variable.
- Literals commonly end up there automatically.
- Runtime-built strings only join the pool if the JVM resolves them as literals or if you call `intern()`.

### Common misunderstanding

People often say: "all strings go to the pool."

That is false.

This goes to the pool automatically:

```java
String a = "hello";
```

This does not automatically become pooled as a canonical reference:

```java
String a = new StringBuilder().append("hel").append("lo").toString();
```

It is an ordinary heap string unless you call `a.intern()`.

---

## 11. Real-World Examples

### 11.1 Map keys

```java
Map<String, Integer> counts = new HashMap<>();
counts.put("SUCCESS", 10);
counts.put("FAILURE", 2);
```

This works well because strings are immutable and hash codes stay stable.

### 11.2 Building text in a loop

```java
String result = "";
for (int i = 0; i < 1000; i++) {
    result = result + i;
}
```

This creates many intermediate strings and is inefficient.

Use `StringBuilder` instead:

```java
StringBuilder builder = new StringBuilder();
for (int i = 0; i < 1000; i++) {
    builder.append(i);
}
String result = builder.toString();
```

### 11.3 Secrets and passwords

```java
String password = "SuperSecret123";
```

This is often discouraged because the content is immutable and can stay in memory until garbage collection.

### 11.4 Repeated identifiers

In a high-volume parser or log processor, values like `INFO`, `WARN`, `ERROR`, `GET`, `POST`, or tenant codes may repeat heavily.

That is where canonicalization can help, but only if the value space is bounded.

---

## 12. Tricky Interview Cases

### 12.1 `==` vs `equals()`

```java
String a = "x";
String b = new String("x");

System.out.println(a == b);      // false
System.out.println(a.equals(b)); // true
```

- `==` checks reference identity.
- `equals()` checks textual content.

### 12.2 Literal folding

```java
String a = "ja" + "va";
String b = "java";

System.out.println(a == b); // true
```

### 12.3 Final constant trap

```java
final String a = "ja";
String b = a + "va";
String c = "java";

System.out.println(b == c); // true
```

### 12.4 Non-final variable trap

```java
String a = "ja";
String b = a + "va";
String c = "java";

System.out.println(b == c); // false
```

### 12.5 `new String()` almost always creates avoidable overhead

```java
String a = new String("java");
String b = "java";

System.out.println(a == b); // false
```

### 12.6 Unicode trap

```java
String emoji = "😀";
System.out.println(emoji.length()); // 2
```

Why 2?

Because `length()` returns the number of UTF-16 code units, not the number of human-visible characters.

That is separate from the String Pool topic, but it is one of the most common String misconceptions.

### 12.7 Historical substring trap

Older Java versions had a substring implementation that could share the original larger backing array, which sometimes caused memory retention surprises.

Modern Java copies the relevant range instead.

---

## 13. Quick Rules to Remember

1. `String` objects are immutable.
2. String literals usually resolve to pooled canonical objects.
3. `new String("x")` creates a distinct object even though the literal `"x"` is also pooled.
4. Reassignment changes the reference, not the existing string object.
5. Compile-time constant concatenation can reuse the pool.
6. Runtime concatenation usually creates a new result object.
7. `intern()` returns the canonical pooled representation.
8. Use `equals()` for content, not `==`.
9. Use `StringBuilder` for repeated concatenation in loops.
10. Do not use `String` for secrets when explicit wiping matters.

---

## 14. One Clean End-to-End Example

```java
String a = "x";
String b = "x";
String c = new String("x");
String d = a + "y";
String e = "xy";
final String f = "x";
String g = f + "y";

System.out.println(a == b);      // true  -> same pooled object
System.out.println(a == c);      // false -> c is a new object
System.out.println(a.equals(c)); // true  -> content matches
System.out.println(d == e);      // false -> runtime concatenation result
System.out.println(d.equals(e)); // true
System.out.println(g == e);      // true  -> compile-time folded constant
System.out.println(d.intern() == e); // true -> canonical pooled reference
```

If you understand why every line above behaves that way, your String fundamentals are strong.

---

## 15. Final Mental Picture

Think about Java `String` like this:

- A variable holds a reference.
- A `String` object lives on the heap.
- Equal literals can share one canonical object through the String Pool.
- Immutability is what makes that sharing safe.
- Reassignment changes references.
- Concatenation often creates new objects unless the compiler can fold constants.
- `intern()` gives you the canonical pooled reference when you truly need it.

That mental model is enough to reason correctly about most tricky String questions.

---

## === Technical Concepts Used ===

- Concept: `String` immutability and sharing safety
  - File: `Mastering-Java/Concepts/Java-String-Deep-Dive.md:43-88`
  - Reason: Explains why pooled references, hash caching, and cross-thread sharing are safe.
  - Docs: https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/lang/String.html

- Concept: Runtime constant pool vs String Pool
  - File: `Mastering-Java/Concepts/Java-String-Deep-Dive.md:92-139`
  - Reason: Separates class metadata resolution from canonical `String` object sharing.
  - Docs: https://docs.oracle.com/javase/specs/jvms/se21/html/jvms-2.html#jvms-2.5.5

- Concept: String literals and compile-time constants
  - File: `Mastering-Java/Concepts/Java-String-Deep-Dive.md:143-186`
  - Reason: Explains why literal reuse and constant folding can make `==` appear to work.
  - Docs: https://docs.oracle.com/javase/specs/jls/se21/html/jls-3.html#jls-3.10.5

- Concept: Constant-expression concatenation
  - File: `Mastering-Java/Concepts/Java-String-Deep-Dive.md:273-365`
  - Reason: Shows the difference between compile-time folding and runtime concatenation.
  - Docs: https://docs.oracle.com/javase/specs/jls/se21/html/jls-15.html#jls-15.29

- Concept: `String.intern()` canonicalization
  - File: `Mastering-Java/Concepts/Java-String-Deep-Dive.md:405-462`
  - Reason: Documents how dynamically created strings can be mapped to the pooled canonical reference.
  - Docs: https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/lang/String.html#intern()

## === Learning Radar ===

### Already Known

- `String` references point to heap objects.
- `equals()` compares content and `==` compares references.
- Reassignment changes the reference, not the old object.

### Newly Introduced

- The class file constant pool, runtime constant pool, and String Pool are different things.
- Since Java 7, the String Pool lives on the heap instead of PermGen.
- `final` constant variables allow compile-time folding during concatenation.
- `intern()` behavior changed meaningfully after older PermGen-based implementations.

### Needs Deeper Review

- The exact bytecode path for string concatenation in different Java versions.
- The interaction between `intern()`, class loading, and object reachability.
- Modern `String` internal representation with `byte[]` and coder optimization.

## === Daily Learning Entry (Markdown) ===

### 2026-04-09

- Task Summary: Wrote a detailed Java String deep-dive covering immutability, memory placement, String Pool behavior, literals vs `new String(...)`, reassignment, concatenation, and `intern()`.
- Concepts:
  - `String` immutability enables safe pooling, hash caching, and easy sharing.
  - String literals, runtime constant pools, and the String Pool are related but not identical.
  - Compile-time constant concatenation behaves differently from runtime concatenation.
  - `intern()` returns the canonical pooled representation.
- Radar:
  - Already Known: reference vs object, `==` vs `equals()`, reassignment semantics.
  - Newly Introduced: heap-based pool in Java 7+, constant folding with `final`, modern `intern()` nuance.
  - Needs Deeper Review: JVM concatenation internals, reachability details of pooled strings.