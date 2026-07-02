# Java JDK, CLI, IDE, Maven, And Gradle Gold Sheet

Target: beginner-to-pro Java setup fluency. This sheet makes the environment, command line, project layout, build tools, and version choices feel boring in the best possible way.

---

## 1. Intuition

Java development has four layers:

```text
JDK installation
    -> gives javac, java, jar, jshell, jcmd, jfr, jlink

Shell environment
    -> tells your terminal which JDK to use

Project structure
    -> tells humans and build tools where source, tests, and resources live

Build tool
    -> compiles, tests, packages, resolves dependencies, and runs CI the same way
```

Beginner line:

```text
The JDK is the toolkit, JAVA_HOME points to it, Maven or Gradle builds the project, and
the IDE is only a comfortable interface on top of the same compiler and runtime.
```

---

## 2. Definition

- Definition: Java setup is the repeatable configuration of JDK, shell, IDE, project structure, dependency management, and build commands.
- Category: Developer environment and build fundamentals.
- Core idea: A Java engineer must be able to build and run code from the terminal, not only from the IDE.

---

## 3. Why It Exists

Without setup fluency:

- Code works in IntelliJ but fails in CI.
- The wrong JDK version runs locally.
- Maven/Gradle uses a different Java version than the terminal.
- Dependencies appear in the IDE but not in packaged artifacts.
- Interview coding becomes slow because `javac`, `java`, and package layout feel mysterious.

Setup mastery solves the "it works on my machine" problem before it becomes a production problem.

---

## 4. Reality

Real Java teams care about:

- JDK versions: usually 17, 21, or 25 LTS; some services track current non-LTS for experimentation.
- Build wrappers: `mvnw` or `gradlew` so CI and developers use a pinned build-tool version.
- CI parity: local `./mvnw test` or `./gradlew test` should match pull-request checks.
- Containers: runtime image JDK/JRE version must match the tested version.
- IDE import: IntelliJ or Eclipse should use the project build tool as the source of truth.

As of Oracle's April 2026 support roadmap, Java 8, 11, 17, 21, and 25 are LTS releases. Java 26 is a non-LTS release line with support ending quickly compared with LTS releases. For production interviews, say exactly which version your project targets and separate LTS features from preview/incubator features.

Official references:
- Oracle Java SE Support Roadmap: `https://www.oracle.com/java/technologies/java-se-support-roadmap.html`
- OpenJDK JDK 25: `https://openjdk.org/projects/jdk/25/`
- OpenJDK JDK 26: `https://openjdk.org/projects/jdk/26/`

---

## 5. How It Works

### From Source To Running Program

```text
App.java
    -> javac App.java
App.class
    -> java App
JVM loads bytecode
    -> verifies, interprets, JIT-compiles hot paths, manages memory
```

### From Project To Artifact

```text
src/main/java
src/main/resources
src/test/java
pom.xml or build.gradle
    -> compile
    -> test
    -> package
target/app.jar or build/libs/app.jar
    -> java -jar app.jar
```

### Failure Path

1. `java --version` shows one JDK.
2. IDE uses another JDK.
3. CI uses a third JDK.
4. A feature compiles locally but fails in CI.
5. Fix by pinning toolchain and wrapper versions.

### Recovery Path

1. Check `java --version`.
2. Check `javac --version`.
3. Check `echo $JAVA_HOME`.
4. Check Maven/Gradle toolchain config.
5. Reimport IDE project from `pom.xml` or `build.gradle`.
6. Run the same wrapper command CI uses.

---

## 6. What Problem It Solves

- Primary problem solved: reliable local and CI builds.
- Secondary benefits: faster debugging, easier onboarding, fewer version mismatch bugs.
- Systems impact: the build becomes reproducible enough for deployment pipelines.

---

## 7. When To Rely On It

Use setup discipline when:

- Joining any Java project.
- Upgrading Java versions.
- Debugging "works in IDE, fails in terminal."
- Preparing for Java interviews.
- Creating a new service or library.
- Building container images.
- Troubleshooting dependency or classpath issues.

Interviewer keywords:

- "How do you run Java from terminal?"
- "What is JAVA_HOME?"
- "Why does CI fail but IntelliJ passes?"
- "How do Maven and Gradle differ?"
- "How do you choose Java 17 vs 21 vs 25?"

---

## 8. When Not To Overcomplicate It

Avoid heavy setup work when:

- You are solving a single-file interview exercise.
- The project is a tiny throwaway script.
- A team standard already exists and you should follow it.

Better approach:

- For one file: `javac FileName.java && java FileName`.
- For real projects: use the wrapper and project build.
- For interview labs: keep the command visible and simple.

---

## 9. Pros And Cons

| Pros | Cons |
|---|---|
| Reproducible local builds | Requires a little upfront discipline |
| CI parity | Toolchains can feel noisy to beginners |
| Clear dependency management | Maven and Gradle have learning curves |
| Easier onboarding | Version managers must be documented |
| Fewer hidden IDE assumptions | Misconfigured wrappers cause confusion |

---

## 10. Trade-offs And Common Mistakes

### Trade-offs

- Maven gives convention and consistency; Gradle gives flexibility and speed.
- JDK LTS gives stability; current feature releases give earlier access but shorter support windows.
- IDE convenience improves speed; terminal commands prove reproducibility.
- Fat JARs are easy to run; slim artifacts plus managed runtime can reduce image size.

### Common Mistakes

| Mistake | Why It Is Wrong | Better Approach |
|---|---|---|
| Installing only a JRE | Modern development needs `javac` and tools | Install a full JDK |
| Trusting only the IDE run button | Hides build and runtime differences | Run `./mvnw test` or `./gradlew test` |
| Setting `JAVA_HOME` to `/bin/java` | `JAVA_HOME` should point to the JDK root | Use the JDK directory |
| Mixing Java versions silently | CI/runtime mismatch | Pin toolchains and document versions |
| Committing generated IDE files randomly | Creates team noise | Commit only agreed project metadata |

---

## 11. Key Numbers

| Item | Typical Value |
|---|---|
| Java release cadence | Feature release roughly every 6 months |
| LTS cadence | Roughly every 2 years for Oracle Java SE after Java 8 |
| Common production LTS versions in 2026 | 17, 21, 25 |
| Single-file compile command | `javac App.java` |
| Single-file run command | `java App` |
| Maven test command | `./mvnw test` |
| Gradle test command | `./gradlew test` |
| Standard source path | `src/main/java` |
| Standard test path | `src/test/java` |

---

## 12. Failure Modes

| Failure | User Observes | Likely Cause | Fix |
|---|---|---|---|
| `javac: command not found` | Cannot compile | JDK not installed or PATH wrong | Install JDK and update PATH |
| `UnsupportedClassVersionError` | Runtime crash | Compiled with newer Java than runtime | Align compile and runtime versions |
| Tests pass in IDE only | CI fails | IDE classpath differs | Reimport from build file and use wrapper |
| Dependency missing in JAR | Runtime class error | Wrong Maven scope or packaging | Check `compile`, `runtime`, `provided`, `test` |
| `NoClassDefFoundError` | App starts then crashes | Runtime dependency missing or init failed | Inspect packaged artifact and dependency tree |
| Preview feature compile error | Build fails | Missing `--enable-preview` | Avoid preview in production unless policy allows |

---

## 13. Scenario

- Product / system: Booking service written in Java 21.
- Why this concept fits: every developer and CI runner must compile, test, and package using the same JDK and build tool.
- What would go wrong without it: one engineer uses Java 25 APIs locally, CI runs Java 21, and the pull request fails late.

---

## 14. Code Sample

File: `HelloJava.java`

```java
public class HelloJava {
    public static void main(String[] args) {
        System.out.println("java.version=" + System.getProperty("java.version"));
        System.out.println("java.home=" + System.getProperty("java.home"));
        System.out.println("user.dir=" + System.getProperty("user.dir"));
    }
}
```

Run:

```bash
javac HelloJava.java
java HelloJava
```

Strong explanation:

```text
javac compiles source into bytecode. java starts a JVM and runs the class that contains
public static void main(String[] args). The output proves which runtime is actually used.
```

---

## 15. Mini Program / Simulation

File: `JavaEnvironmentCheck.java`

```java
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public class JavaEnvironmentCheck {
    public static void main(String[] args) {
        List<String> keys = List.of(
            "java.version",
            "java.vendor",
            "java.home",
            "java.class.path",
            "user.dir",
            "os.name",
            "os.arch"
        );

        for (String key : keys) {
            System.out.printf("%-16s = %s%n", key, System.getProperty(key));
        }

        System.out.println("pom.xml exists      = " + Files.exists(Path.of("pom.xml")));
        System.out.println("build.gradle exists = " + Files.exists(Path.of("build.gradle")));
        System.out.println("mvnw exists         = " + Files.exists(Path.of("mvnw")));
        System.out.println("gradlew exists      = " + Files.exists(Path.of("gradlew")));
    }
}
```

Run:

```bash
javac JavaEnvironmentCheck.java
java JavaEnvironmentCheck
```

Debrief:

1. Which JDK is the terminal using?
2. Is this a Maven project, Gradle project, or single-file exercise?
3. Would CI use the same version?
4. What command should the project README tell new developers to run?

---

## 16. Practical Question

> You join a Java backend team. The service passes tests in IntelliJ but fails in CI with `UnsupportedClassVersionError`. How would you debug and fix it?

---

## 17. Strong Answer

I would first confirm the Java versions used at each layer: `java --version`, `javac --version`, Maven or Gradle toolchain, IDE project SDK, CI image, and container runtime. `UnsupportedClassVersionError` means bytecode was compiled for a newer Java version than the runtime understands. The fix is not to guess flags; it is to align source, target, compiler release, CI JDK, and runtime image. I would pin the build through Maven/Gradle toolchains, run tests through the wrapper, document the expected JDK in the README, and add a CI check that prints `java --version` early in the pipeline.

---

## 18. Revision Notes

- One-line summary: Java setup mastery means you can prove which JDK builds and runs your code from terminal to CI.
- Three keywords: `JAVA_HOME`, wrapper, toolchain.
- One interview trap: the IDE run button is not proof of a reproducible build.
- One memory trick: JDK is the toolbox, `JAVA_HOME` is the address, Maven/Gradle is the factory.
