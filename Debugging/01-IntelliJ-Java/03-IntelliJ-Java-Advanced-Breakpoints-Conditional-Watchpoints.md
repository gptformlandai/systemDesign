# 03. IntelliJ Java: Advanced Breakpoints — Conditional, Watchpoints, Exception

## Goal

Use IntelliJ's advanced breakpoint features to stop execution precisely when and where you need it, without manually stepping through thousands of iterations.

---

## Conditional Breakpoints

A conditional breakpoint only pauses when a boolean expression evaluates to `true`.

### Setting Up

```text
1. Set a regular line breakpoint (click gutter or Cmd+F8).
2. Right-click the red circle -> "Edit Breakpoint" (or Cmd+Shift+F8 on that line).
3. Enter condition in the "Condition" field.
4. Press Enter or click Done.
```

A breakpoint with a condition shows a question mark inside the red circle.

### Condition Examples

```java
// Break when processing a specific order.
orderId.equals("ORD-99001")

// Break when a loop reaches a specific iteration.
i == 9999  // useful for finding bugs at iteration N of a 10000-item loop

// Break when a field has unexpected value.
order.getStatus() == null

// Break when processing high-value orders.
order.getTotal().compareTo(new BigDecimal("10000")) > 0

// Break when a specific user triggers the code path.
request.getUserId().equals("USR-ADMIN-001")

// Break when an error condition is about to happen.
items.size() == 0 && !allowEmpty

// Break for a specific thread (useful in multithreaded code).
Thread.currentThread().getName().equals("order-processor-1")
```

### Performance Note

Conditional breakpoints evaluate the expression for EVERY hit, not just matching ones. In a tight loop running 1 million times, a conditional breakpoint with a complex expression may significantly slow execution. For loop-heavy code, use hit count breakpoints or log breakpoints instead.

---

## Hit Count Breakpoints

Pause after the breakpoint has been hit N times.

```text
Edit Breakpoint -> "More" link -> Pass count: 1000
-> Pauses only on the 1000th hit
-> Useful for: finding a bug that occurs at a specific iteration
```

Combine with condition for maximum precision:

```text
Condition:  item.getQuantity() < 0
Pass count: 5  -> pause on the 5th occurrence of negative quantity
```

---

## Field Watchpoints

Pause execution when a specific field is read or written.

```text
1. Open the class containing the field.
2. Right-click the field declaration -> "Add Field Watchpoint"
   OR click in the gutter next to the field declaration.

Configure:
  Field access:       pause when field is READ
  Field modification: pause when field is WRITTEN

A diamond-shaped breakpoint appears in the gutter.
```

### Use Case: Finding Unexpected State Mutation

```java
public class OrderService {
    private BigDecimal totalRevenue = BigDecimal.ZERO;  // <- watchpoint on this field
    
    // Watch on totalRevenue will pause execution whenever it is modified.
    // You see which method is writing an unexpected value.
}
```

### Stack Trace At Write

When the watchpoint fires on a write, IntelliJ shows you:
- The new value being written
- The stack frame where the write happened
- The complete call stack leading to that write

---

## Exception Breakpoints

Pause execution at the exact line where an exception is thrown.

```text
Run -> View Breakpoints (Cmd+Shift+F8) -> + (plus icon) -> Java Exception Breakpoints

Exception class: java.lang.NullPointerException
  [x] Caught exceptions    (pauses even inside try/catch blocks)
  [x] Uncaught exceptions  (pauses when exception propagates out)
```

### Why This Is Powerful

Without exception breakpoints:

```text
You see: "NullPointerException at OrderService.java:45"
But the NPE was caused 3 stack frames up and 200ms earlier.
```

With exception breakpoint:

```text
Execution pauses at the exact line where null was dereferenced.
You see the variable that was null.
You see who called this method and with what arguments.
```

### Common Exception Breakpoints To Keep Active

```text
java.lang.NullPointerException          (catch bugs early)
java.lang.ClassCastException            (find bad type assumptions)
java.lang.ArrayIndexOutOfBoundsException
java.lang.StackOverflowError            (find infinite recursion immediately)
java.util.ConcurrentModificationException  (find thread safety bugs)
```

---

## Method Entry/Exit Breakpoints

Pause when any method matching a pattern is called.

```text
Run -> View Breakpoints -> + -> Java Method Breakpoints

Class pattern:   com.example.orders.*  (any class in this package)
Method name:     create*               (any method starting with "create")
  [x] Method entry
  [x] Method exit: logs the return value

OR

Click in the gutter next to the method SIGNATURE (not body).
Diamond icon appears.
```

### Method Breakpoints Are Slow

Method entry/exit breakpoints have significant performance overhead — they instrument every call to the matching methods. Use them sparingly for investigation, then disable them.

---

## Log Breakpoints (Non-Suspending)

Print a message when code passes through a line without pausing.

```text
Set a breakpoint -> Edit -> uncheck "Suspend"
Enable: "Evaluate and log" -> check
Expression: "Processing order " + order.getId() + " status=" + order.getStatus()
```

Output appears in the Debug Console tab, formatted as:
```
Processing order ORD-001 status=PENDING
Processing order ORD-002 status=PENDING
Processing order ORD-003 status=CANCELLED
```

This is like System.out.println debugging but:
- No code changes needed
- Toggle on/off without modifying source
- Can include complex expressions
- Timestamps available via `new java.util.Date()`

---

## Breakpoint Groups And Management

```text
Run -> View Breakpoints (Cmd+Shift+F8)

Left panel: list of all breakpoints grouped by type.
Right panel: settings for selected breakpoint.

Disable all:        click the checkmark next to "Breakpoints" heading
Re-enable all:      same
Export:             saves breakpoints to XML file (share with team or restore later)
Mute all:           debug toolbar mute icon (temporary, all breakpoints are skipped but not deleted)
```

---

## Interview Sound Bite

Conditional breakpoints eliminate the need to step through thousands of iterations manually — set the condition to stop only when `i == 9999` or `orderId.equals("ORD-FAIL")`. Field watchpoints reveal unexpected mutation: add a watchpoint on a field and the debugger shows you exactly which method modified it and via what call path. Exception breakpoints stop at the throw site, not the catch site, which is where the actual bug is. Log breakpoints add trace output without suspending or changing source code — essential for timing-sensitive multithreaded bugs where suspending changes the thread schedule.
