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

## 12. Version Catalogs — libs.versions.toml

Version catalogs are the Gradle-native way to manage dependency versions in one place:

```toml
# gradle/libs.versions.toml
[versions]
spring-boot = "3.4.0"
testcontainers = "1.20.4"
jackson = "2.18.2"
junit = "5.11.4"

[libraries]
spring-boot-starter-web = { module = "org.springframework.boot:spring-boot-starter-web", version.ref = "spring-boot" }
spring-boot-starter-test = { module = "org.springframework.boot:spring-boot-starter-test", version.ref = "spring-boot" }
testcontainers-postgresql = { module = "org.testcontainers:postgresql", version.ref = "testcontainers" }
junit-api = { module = "org.junit.jupiter:junit-jupiter-api", version.ref = "junit" }

[bundles]
testing = ["spring-boot-starter-test", "testcontainers-postgresql", "junit-api"]

[plugins]
spring-boot = { id = "org.springframework.boot", version.ref = "spring-boot" }
```

```kotlin
// build.gradle.kts — use catalog references
dependencies {
    implementation(libs.spring.boot.starter.web)
    testImplementation(libs.bundles.testing)   // entire testing bundle in one line
}

plugins {
    alias(libs.plugins.spring.boot)
}
```

**Benefits:** Single source of truth for all versions. Refactoring-safe (rename in one file). IDE auto-completion. Supported by Renovate/Dependabot for automated updates.

---

## 13. Configuration Cache

### What It Is

The configuration cache serializes the **configuration phase** (build scripts + task graph) to disk. Subsequent builds skip the entire configuration phase if nothing changed — only the execution phase runs.

```
Without configuration cache:
  ./gradlew build
  -> Parse settings.gradle.kts           ← runs every time
  -> Evaluate all build.gradle.kts files ← runs every time
  -> Build task dependency graph          ← runs every time
  -> Execute tasks (compile, test, jar)  ← runs every time

With configuration cache hit:
  ./gradlew build
  -> Load cached task graph from disk    ← milliseconds
  -> Execute tasks (compile, test, jar)  ← runs every time
  
  Saved: 10–30s on large multi-project builds
```

### Enabling It

```properties
# gradle.properties
org.gradle.configuration-cache=true
org.gradle.configuration-cache.problems=warn   # warn during migration; switch to fail after
```

```bash
# Or pass on the command line
./gradlew build --configuration-cache
./gradlew build --configuration-cache --configuration-cache-problems=warn

# First run: "Configuration cache entry stored."
# Subsequent run: "Configuration cache entry reused — 0 ms configuration time."
```

### What Gets Cached vs What Invalidates the Cache

```txt
Cached:
  - build.gradle.kts / build.gradle content (parsed AST + evaluation result)
  - settings.gradle.kts / settings.gradle content
  - Task dependency graph
  - Task input declarations (but not the actual input files — those use build cache)
  - Extension configurations (plugins' DSL blocks)

Cache is INVALIDATED when:
  - Any build.gradle.kts file changes
  - settings.gradle.kts changes
  - gradle.properties changes
  - A buildSrc file changes
  - An included build changes
  - A task's @Input value changes (not @InputFile — that's build cache)
  - External properties accessed at configuration time change (e.g., project.version)
```

### Configuration Cache vs Build Cache — Key Distinction

```txt
Configuration Cache:
  Caches: the configuration PHASE (parsing scripts, building the task graph)
  Unit: the entire build's task graph
  Hit means: Gradle skips evaluating all build.gradle.kts files

Build Cache (org.gradle.caching=true):
  Caches: the OUTPUTS of individual tasks (compiled classes, test results, JARs)
  Unit: individual task output (keyed by inputs hash)
  Hit means: a specific task's output is restored without re-running

They are COMPLEMENTARY and can both be enabled simultaneously.
Most gains come from using BOTH together.
```

### Plugin Compatibility — The Most Common Problem

Not all Gradle plugins support the configuration cache. Incompatible plugins trigger problems:

```bash
# See all compatibility problems without failing the build
./gradlew build --configuration-cache --configuration-cache-problems=warn

# Example warning output:
# > Task ':processResources' using 'Project' at execution time.
# > Task ':generateSources' registering outputs after task graph has been calculated.
```

Common incompatible plugin patterns:
```kotlin
// ❌ Incompatible: accessing Project at execution time
tasks.register("myTask") {
    doLast {
        project.file("src/main/resources")   // Project reference — not serializable
    }
}

// ✅ Compatible: capture project values at configuration time
val resourceDir = project.file("src/main/resources")
tasks.register("myTask") {
    val dir = resourceDir   // captured value — serializable
    doLast {
        println(dir.absolutePath)
    }
}
```

### Fixing Configuration Cache Violations

```kotlin
// Violation: reading System properties at execution time
// ❌ Wrong
tasks.register("printEnv") {
    doLast {
        println(System.getenv("MY_VAR"))   // reads env at execution — not cached
    }
}

// ✅ Fixed: declare as task input, read at configuration time
abstract class PrintEnvTask : DefaultTask() {
    @get:Input
    val myVar = project.providers.environmentVariable("MY_VAR")

    @TaskAction
    fun run() {
        println(myVar.get())   // cached input — serializable
    }
}

// Violation: using Closures that capture Project
// ❌ Wrong
tasks.register("bad") {
    doLast {
        val content = project.configurations.getByName("runtimeClasspath").asPath
        println(content)
    }
}

// ✅ Fixed: use FileCollection as task input
abstract class GoodTask : DefaultTask() {
    @get:InputFiles
    abstract val classpath: ConfigurableFileCollection

    @TaskAction
    fun run() = println(classpath.asPath)
}

tasks.register<GoodTask>("good") {
    classpath.from(configurations.named("runtimeClasspath"))
}
```

### Checking Plugin Compatibility

```bash
# Check a plugin's compatibility
# 1. Check the plugin's GitHub issues/release notes for "configuration cache" support
# 2. Run with --configuration-cache-problems=warn and review the output
# 3. File with problematic plugin — upgrade or use workaround:

# Temporary workaround — disable for specific tasks
tasks.named("problemTask") {
    notCompatibleWithConfigurationCache("Using legacy API")
}
```

**Well-supported plugins (as of 2025):** Spring Boot Gradle Plugin, Shadow Plugin, Kotlin Gradle Plugin, Android Gradle Plugin (AGP 8.0+), Jacoco, Checkstyle.
**Historically problematic:** Some code generation plugins, custom in-house plugins, older Gradle plugins.

### Interview Insight

Strong answer:

> The Gradle configuration cache serializes the task graph after the configuration phase and reuses it on subsequent builds that haven't changed build scripts. This is separate from the build cache, which caches task outputs. Together they eliminate both configuration overhead and redundant task execution. The main adoption challenge is plugin compatibility — some plugins access `Project` at execution time rather than configuration time, which violates the serialization contract. The migration strategy is to enable with `problems=warn`, fix violations one by one, then switch to `problems=fail`.

Follow-up trap:

> What's the difference between UP-TO-DATE, FROM-CACHE, and configuration cache reuse?

Good answer:

> `UP-TO-DATE` means the task's inputs and outputs are unchanged since last run — Gradle skips the task locally. `FROM-CACHE` means the task's output was restored from the build cache (local or remote) — Gradle pulled outputs built elsewhere. Configuration cache reuse means the task GRAPH itself wasn't re-evaluated — Gradle skipped the configuration phase entirely. These three optimizations operate at different levels and can all be active in the same build.

---

## 14. Convention Plugins

Convention plugins move shared build logic from `allprojects {}` / `subprojects {}` into reusable plugins:

```kotlin
// buildSrc/src/main/kotlin/java-library-conventions.gradle.kts
plugins {
    `java-library`
    id("org.springframework.boot")
    checkstyle
    jacoco
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

tasks.test {
    useJUnitPlatform()
    maxParallelForks = (Runtime.getRuntime().availableProcessors() / 2).coerceAtLeast(1)
}

tasks.jacocoTestReport {
    dependsOn(tasks.test)
    reports {
        xml.required.set(true)
    }
}
```

Apply to any module:
```kotlin
// modules/payments-service/build.gradle.kts
plugins {
    id("java-library-conventions")  // one line applies all conventions
}

dependencies {
    implementation(libs.spring.boot.starter.data.jpa)
}
```

**Benefits:** DRY build logic. All modules consistently configured. Changing JaCoCo config in one convention plugin updates all 20 modules.

---

## 15. Interview Insight

Strong answer:

> Gradle builds a task DAG from requested tasks and their dependencies. It can skip work through incremental build checks and reuse outputs through build cache when task inputs match. This makes it powerful for large multi-project builds, but only if tasks are deterministic and build logic is structured well.

Follow-up trap:

> Why did Gradle skip my task?

Good answer:

> I check the task outcome. `UP-TO-DATE` means local inputs/outputs matched. `FROM-CACHE` means outputs were restored. `NO-SOURCE` means no inputs. If skipping is wrong, the task probably has undeclared inputs or outputs.

---

## 16. Revision Notes

- One-line summary: Gradle is a task DAG engine optimized by inputs, outputs, and cache.
- Three keywords: task, cache, incremental.
- One interview trap: Gradle speed depends on correct task modeling.
- Memory trick: Maven is a timetable; Gradle is a map.

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
