# Java Annotation Processing And Code Generation Gold Sheet

Target: senior Java interviews and framework-internals rounds where annotations, reflection, generated code, Lombok, MapStruct, and native-image compatibility come up.

---

## 1. Intuition

Annotations are labels. Reflection reads labels at runtime. Annotation processors read labels at compile time and can generate code.

```text
@Annotation
    -> metadata on code

Reflection
    -> inspect metadata while the app is running

Annotation processor
    -> inspect metadata while compiling and generate source/resources
```

Beginner line:

```text
Runtime reflection discovers things after the app starts. Annotation processing generates
things before the app starts.
```

---

## 2. Definition

- Definition: Annotation processing is a compile-time Java mechanism that scans annotations and can generate source files, metadata, or validation errors.
- Category: Java compiler tooling and framework internals.
- Core idea: shift repetitive or discoverable work from runtime reflection to compile-time generation when useful.

---

## 3. Why It Exists

Java frameworks often need boilerplate:

- DTO mappers.
- Builders.
- Logging fields.
- Dependency metadata.
- Serialization adapters.
- Query metadata.
- Validation wiring.

Naive approaches:

- Hand-write repetitive boilerplate.
- Use broad runtime reflection everywhere.
- Fail late at runtime when metadata is wrong.

Annotation processing exists to generate code and catch mistakes earlier.

---

## 4. Reality

Common tools:

| Tool | What It Does | Main Trade-off |
|---|---|---|
| Lombok | Generates boilerplate like getters/builders | IDE/build integration and hidden code |
| MapStruct | Generates mapper implementations | Requires clear mapping definitions |
| Dagger | Generates dependency injection code | More explicit than reflection-heavy DI |
| AutoValue | Generates immutable value classes | Less common with records now |
| JPA Metamodel | Generates type-safe criteria metadata | Extra build step |
| Immutables | Generates immutable models/builders | Generated-source learning curve |

Runtime reflection is still common in Spring, Jackson, Hibernate, and testing frameworks. Modern AOT/native-image paths reward code that is explicit or backed by reachability metadata.

---

## 5. How It Works

### Annotation Basics

1. Define annotation with `@interface`.
2. Choose target: class, method, field, parameter, etc.
3. Choose retention: source, class, or runtime.
4. Compiler records metadata based on retention.
5. Runtime frameworks or compile-time processors consume it.

### Annotation Processor Flow

1. Compiler starts.
2. Processor receives annotated elements.
3. Processor validates usage.
4. Processor writes generated source files.
5. Compiler compiles generated files in later rounds.
6. Build artifact includes normal compiled classes.

### Failure Path

1. Processor is missing from annotation processor path.
2. Generated class is absent.
3. Code compiles in IDE but fails in CI, or the opposite.
4. Fix build configuration and IDE annotation processing.

---

## 6. What Problem It Solves

- Primary problem solved: repetitive code and late runtime discovery.
- Secondary benefits: faster startup, fewer reflection needs, earlier validation.
- Systems impact: improves build-time safety and can help AOT/native-image compatibility.

---

## 7. When To Rely On It

Use annotation processing when:

- The generated code is deterministic.
- Boilerplate is large and mechanical.
- Compile-time validation prevents runtime bugs.
- Runtime reflection is costly or native-image unfriendly.
- The team understands generated-source debugging.

Interviewer keywords:

- Lombok
- MapStruct
- annotation retention
- reflection vs compile time
- generated sources
- native image
- annotation processor

---

## 8. When Not To Use It

Avoid annotation/code generation when:

- It hides simple code that would be clearer by hand.
- Generated code becomes hard to debug.
- Team tooling is inconsistent.
- Library users need explicit APIs.
- Compile time and IDE experience degrade.

Better approach:

- Use records for simple DTOs.
- Hand-write critical domain logic.
- Use MapStruct for repetitive structural mapping.
- Use Lombok carefully and consistently if team standard allows it.

---

## 9. Pros And Cons

| Pros | Cons |
|---|---|
| Reduces boilerplate | Hides generated code |
| Catches errors at compile time | Build/IDE setup matters |
| Can improve startup | Adds processor dependencies |
| Helps avoid broad reflection | Harder for beginners to trace |
| Useful for mappers and metadata | Bad generation creates magic |

---

## 10. Trade-offs And Common Mistakes

### Trade-offs

- Runtime reflection is flexible but can be slower and harder for AOT.
- Compile-time generation is fast at runtime but adds build complexity.
- Lombok reduces typing but can obscure constructors, equals/hashCode, and builders.
- MapStruct is explicit and fast but requires mapping maintenance.

### Common Mistakes

| Mistake | Why It Is Wrong | Better Approach |
|---|---|---|
| Confusing retention policies | Runtime framework cannot see SOURCE retention | Use RUNTIME only when needed |
| Overusing Lombok on domain entities | Hidden mutability/equality surprises | Prefer records/explicit code where clarity matters |
| Ignoring generated source | Debugging becomes guesswork | Inspect generated files |
| Processor only configured in IDE | CI fails | Configure Maven/Gradle annotation processor path |
| Reflection-heavy native image | Runtime metadata missing | Add reachability metadata or use generated code |

---

## 11. Key Numbers

| Item | Meaning |
|---|---|
| `RetentionPolicy.SOURCE` | discarded by compiler after processing |
| `RetentionPolicy.CLASS` | stored in class file, not necessarily visible via reflection |
| `RetentionPolicy.RUNTIME` | visible via reflection |
| Annotation processing rounds | compiler may run multiple rounds as files are generated |
| Generated source location | `target/generated-sources` or `build/generated` commonly |
| Native image | closed-world analysis needs explicit dynamic metadata |

---

## 12. Failure Modes

| Failure | User Observes | Cause | Mitigation |
|---|---|---|---|
| Missing generated mapper | compile error | processor not configured | fix build annotation processor path |
| Annotation invisible at runtime | framework ignores it | wrong retention | use RUNTIME where necessary |
| Native image runtime failure | reflection class missing | metadata not reachable | register reflection/resources/proxies |
| Lombok IDE mismatch | red editor, green CLI or reverse | plugin/config mismatch | align IDE and build |
| Broken equals/hashCode | subtle collection bugs | generated equality wrong for entity | define equality deliberately |

---

## 13. Scenario

- Product / system: Booking API with DTO/entity mapping.
- Why this concept fits: MapStruct can generate boring mapping code and fail compilation if fields are unmapped.
- What would go wrong without it: hand-written mapping drifts silently and misses fields after contract changes.

---

## 14. Code Sample

Runtime annotation:

```java
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface Audited {
    String value();
}

@Audited("booking")
class BookingService {
}
```

Reflection read:

```java
public class AnnotationReadDemo {
    public static void main(String[] args) {
        Audited audited = BookingService.class.getAnnotation(Audited.class);
        System.out.println(audited.value());
    }
}
```

Trap:

```text
If retention is SOURCE or CLASS, this runtime reflection example will not work as expected.
```

---

## 15. Mini Program / Simulation

Annotation processor mental model:

```java
// Source code you write:
@Mapper
interface BookingMapper {
    BookingDto toDto(Booking booking);
}

// Source code generated during compilation:
final class BookingMapperImpl implements BookingMapper {
    public BookingDto toDto(Booking booking) {
        return new BookingDto(booking.id(), booking.roomId(), booking.status());
    }
}
```

Debrief:

1. Which class did the developer write?
2. Which class did the processor generate?
3. Why can generated code be faster than reflection at runtime?
4. What build config must CI know about?

---

## 16. Practical Question

> A service uses Lombok and MapStruct. It builds locally but fails in CI saying `BookingMapperImpl` is missing. How do you debug it?

---

## 17. Strong Answer

I would check whether annotation processing is enabled consistently in the build, not just in the IDE. For Maven I would inspect the compiler plugin and annotation processor path; for Gradle I would check `annotationProcessor` dependencies. Then I would verify generated-source directories are created in CI and that the mapper annotation is visible to the processor. I would also check Java version and incremental compilation issues. The root principle is that generated classes must come from the build tool so CI, IDE, and local terminal produce the same artifact.

---

## 18. Revision Notes

- One-line summary: annotations label code; reflection reads labels at runtime; processors generate code at compile time.
- Three keywords: retention, processor, generated source.
- One interview trap: Lombok/MapStruct must be configured in the build, not only the IDE.
- One memory trick: reflection discovers, processors manufacture.
