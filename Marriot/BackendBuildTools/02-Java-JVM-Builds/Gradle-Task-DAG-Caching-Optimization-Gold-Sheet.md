# Gradle Task DAG, Caching, Optimization Gold Sheet

> Topic: Gradle DSL, task graph, incremental builds, build cache, parallelism, and performance.

---

## 1. Intuition

Gradle is task-graph driven. Instead of asking "which lifecycle phase should I run?", you ask "which task do I want?", and Gradle computes the dependent task graph needed to produce that output.

Beginner version:

> Gradle builds a map of tasks and runs only what is needed.

---

## 2. Definition

- Definition: Gradle is a flexible build automation tool based on plugins, tasks, dependency graphs, incremental execution, and caching.
- Category: JVM and multi-language build system.
- Core idea: Task DAG plus inputs/outputs plus caching.

---

## 3. Gradle Build File

Kotlin DSL:

```kotlin
plugins {
    java
    jacoco
    id("org.sonarqube") version "6.2.0.5505"
}

group = "com.example"
version = "1.0.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.springframework.boot:spring-boot-starter-web")
    testImplementation("org.junit.jupiter:junit-jupiter:5.11.0")
}

tasks.test {
    useJUnitPlatform()
}
```

Groovy DSL:

```groovy
plugins {
    id 'java'
    id 'jacoco'
}

group = 'com.example'
version = '1.0.0-SNAPSHOT'
```

---

## 4. Task DAG

```txt
compileJava
   |
   v
processResources
   |
   v
classes
   |
   v
test
   |
   v
jacocoTestReport
   |
   v
check
   |
   v
build
```

Command:

```bash
./gradlew build
```

Gradle asks:

- Which task did the user request?
- Which tasks does it depend on?
- Which inputs changed?
- Which outputs already exist?
- Can any outputs come from cache?
- Can independent tasks run in parallel?

---

## 5. Maven vs Gradle

| Area | Maven | Gradle |
|---|---|---|
| Mental model | lifecycle phases | task graph |
| Config style | XML POM | Kotlin/Groovy DSL |
| Convention | strong defaults | convention plus flexibility |
| Performance | predictable, simpler | incremental/cached/parallel strengths |
| Customization | plugins and profiles | rich build logic |
| Risk | verbose but standardized | flexible but can become complex |

Interview sentence:

> Maven is standardized and lifecycle-oriented; Gradle is flexible and task-graph-oriented.

---

## 6. Incremental Builds

Gradle can skip tasks when inputs and outputs are unchanged.

```txt
source file unchanged
dependency unchanged
compiler options unchanged
output exists
       |
       v
task is UP-TO-DATE
```

Example log:

```txt
> Task :compileJava UP-TO-DATE
> Task :test
> Task :jar
```

How to read:

- `UP-TO-DATE`: Gradle reused local output.
- `FROM-CACHE`: Gradle restored output from build cache.
- `NO-SOURCE`: task has no source files.
- `SKIPPED`: task condition prevented execution.

---

## 7. Build Cache

Build cache stores task outputs keyed by inputs.

```txt
Task inputs
  source files
  compiler version
  classpath
  task config
      |
      v
cache key
      |
      v
stored output
```

Local cache:

```txt
same machine reuse
```

Remote cache:

```txt
CI builds once
developers/other CI jobs reuse outputs
```

Good candidates:

- compile tasks.
- test tasks when deterministic.
- code generation.
- packaging tasks.

Bad candidates:

- tasks with hidden inputs.
- tasks that depend on current time.
- tasks that access network unpredictably.

---

## 8. Parallel Execution

Gradle can run independent tasks in parallel, especially in multi-project builds.

```txt
:service-a:compileJava     :service-b:compileJava
          \                 /
           v               v
              aggregateCheck
```

Common command:

```bash
./gradlew build --parallel
```

Use carefully:

- tasks must not share unsafe mutable outputs.
- database/integration tests need isolated ports and data.
- CI agents need enough CPU/memory.

---

## 9. Performance Knobs

Useful knobs:

- Gradle Wrapper for pinned Gradle version.
- Build cache.
- Configuration cache.
- Parallel execution.
- Dependency locking.
- Version catalogs.
- Avoid eager configuration.
- Avoid broad `allprojects` / `subprojects` build logic.
- Split slow integration tests.
- Profile with build scans or `--profile`.

Example commands:

```bash
./gradlew build --scan
./gradlew build --profile
./gradlew test --info
./gradlew dependencies
./gradlew dependencyInsight --dependency guava
```

---

## 10. SNAPSHOT With Gradle

Gradle can publish Maven-compatible artifacts.

```kotlin
plugins {
    `maven-publish`
}

version = "1.0.0-SNAPSHOT"

publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            from(components["java"])
        }
    }
    repositories {
        maven {
            url = uri("https://repo.example.com/snapshots")
        }
    }
}
```

Command:

```bash
./gradlew publish
```

Same release rule:

```txt
SNAPSHOT for development, immutable release for production
```

---

## 11. Common Mistakes

### Mistake: Writing custom tasks without declaring inputs/outputs

- Why wrong: Gradle cannot safely skip or cache the task.
- Better approach: declare task inputs and outputs.

### Mistake: Disabling tests to speed up builds

- Why wrong: hides correctness problems.
- Better approach: split tests, parallelize safely, cache deterministic tasks.

### Mistake: Overusing dynamic dependency versions

- Why wrong: reproducibility suffers.
- Better approach: use dependency locking and version catalogs.

### Mistake: Complex build logic in every module

- Why wrong: configuration time grows and behavior diverges.
- Better approach: use convention plugins.

---

## 12. Interview Insight

Strong answer:

> Gradle builds a task DAG from requested tasks and their dependencies. It can skip work through incremental build checks and reuse outputs through build cache when task inputs match. This makes it powerful for large multi-project builds, but only if tasks are deterministic and build logic is structured well.

Follow-up trap:

> Why did Gradle skip my task?

Good answer:

> I check the task outcome. `UP-TO-DATE` means local inputs/outputs matched. `FROM-CACHE` means outputs were restored. `NO-SOURCE` means no inputs. If skipping is wrong, the task probably has undeclared inputs or outputs.

---

## 13. Revision Notes

- One-line summary: Gradle is a task DAG engine optimized by inputs, outputs, and cache.
- Three keywords: task, cache, incremental.
- One interview trap: Gradle speed depends on correct task modeling.
- Memory trick: Maven is a timetable; Gradle is a map.
