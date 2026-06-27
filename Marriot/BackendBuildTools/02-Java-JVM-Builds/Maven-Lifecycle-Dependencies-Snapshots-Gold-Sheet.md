# Maven Lifecycle, Dependencies, Snapshots Gold Sheet

> Topic: `pom.xml`, Maven lifecycle, dependency scopes, plugins, snapshots, and artifact publishing.

---

## 1. Intuition

Maven is a convention-driven build system. It says: if your project follows standard layout and declares a `pom.xml`, Maven already knows how to validate, compile, test, package, install, and deploy it.

Beginner version:

> Maven is like a railway timetable for Java builds. You pick a destination phase, and Maven runs every earlier stop in order.

---

## 2. Definition

- Definition: Maven is a build automation and dependency management tool centered on a Project Object Model file called `pom.xml`.
- Category: Java/JVM build system.
- Core idea: Standard lifecycle phases plus plugins plus repository-based artifacts.

---

## 3. POM Mental Model

```xml
<project>
  <modelVersion>4.0.0</modelVersion>

  <groupId>com.example</groupId>
  <artifactId>payments-service</artifactId>
  <version>1.0.0-SNAPSHOT</version>
  <packaging>jar</packaging>

  <dependencies>
    <dependency>
      <groupId>org.springframework.boot</groupId>
      <artifactId>spring-boot-starter-web</artifactId>
      <version>3.4.0</version>
    </dependency>
  </dependencies>
</project>
```

Coordinates:

```txt
groupId:artifactId:version
```

Example:

```txt
com.example:payments-service:1.0.0-SNAPSHOT
```

---

## 4. Maven Lifecycle

Common lifecycle:

```txt
validate
   |
   v
compile
   |
   v
test
   |
   v
package
   |
   v
verify
   |
   v
install
   |
   v
deploy
```

Meaning:

| Phase | What Happens |
|---|---|
| `validate` | check project structure/config |
| `compile` | compile source code |
| `test` | run unit tests |
| `package` | create JAR/WAR |
| `verify` | run checks such as integration tests/quality checks |
| `install` | place artifact in local Maven repo |
| `deploy` | publish artifact to remote repository |

Important:

```bash
mvn verify
```

runs every earlier phase up to `verify`.

```bash
mvn clean deploy
```

cleans old output and publishes the final artifact to a remote repository.

---

## 5. Plugin Architecture

Maven phases do not do work directly. Plugin goals do the work.

```txt
phase: compile
  -> compiler:compile

phase: test
  -> surefire:test

phase: package
  -> jar:jar

phase: deploy
  -> deploy:deploy
```

Custom plugin example:

```xml
<build>
  <plugins>
    <plugin>
      <groupId>org.jacoco</groupId>
      <artifactId>jacoco-maven-plugin</artifactId>
      <version>0.8.13</version>
      <executions>
        <execution>
          <goals>
            <goal>prepare-agent</goal>
          </goals>
        </execution>
        <execution>
          <id>report</id>
          <phase>verify</phase>
          <goals>
            <goal>report</goal>
          </goals>
        </execution>
      </executions>
    </plugin>
  </plugins>
</build>
```

---

## 6. Dependency Scopes

| Scope | Meaning | Example |
|---|---|---|
| `compile` | needed to compile and run | core library |
| `provided` | needed to compile, provided by runtime | servlet API in external container |
| `runtime` | not needed to compile, needed at runtime | JDBC driver |
| `test` | only needed for tests | JUnit, Mockito |
| `system` | local system path dependency | avoid in modern builds |
| `import` | import dependency management from BOM | Spring Boot BOM |

Common mistake:

```txt
putting a runtime-only driver in test scope
```

Result:

```txt
tests pass, app fails at runtime
```

---

## 7. SNAPSHOT Versions

A version ending in `-SNAPSHOT` means "moving development version".

```txt
1.0.0-SNAPSHOT
```

Local development:

```bash
mvn install
```

publishes to your local Maven repository:

```txt
~/.m2/repository/com/example/payments-service/1.0.0-SNAPSHOT/
```

Remote deployment:

```bash
mvn deploy
```

publishes snapshot metadata and timestamped snapshot artifacts in the remote repository.

Conceptual remote snapshot:

```txt
payments-service-1.0.0-20260628.101530-4.jar
maven-metadata.xml
```

Why timestamped snapshots exist:

- Many builds can publish the same logical `1.0.0-SNAPSHOT`.
- The repository needs unique physical files.
- Metadata tells consumers which snapshot is latest.

---

## 8. Release Versions

Release versions are immutable in healthy artifact repositories.

```txt
1.0.0
1.0.1
1.1.0
```

Rule:

```txt
SNAPSHOT = moving development artifact
release = stable promoted artifact
```

Staff-level policy:

- Never deploy SNAPSHOT to production.
- Never overwrite release artifacts.
- Promote release artifacts through environments.
- Attach commit SHA and build metadata.

---

## 9. Real-World Maven CI

```bash
mvn -B clean verify
mvn -B org.sonarsource.scanner.maven:sonar-maven-plugin:sonar
mvn -B deploy
```

Better multi-module Sonar pattern:

```bash
mvn -B clean install
mvn -B org.sonarsource.scanner.maven:sonar-maven-plugin:sonar
```

Why:

- Multi-module analysis may need installed module outputs.
- Coverage reports must exist before Sonar imports them.

---

## 10. Common Mistakes

### Mistake: Calling `integration-test` directly

- Why wrong: some cleanup/report goals run later in `verify`.
- Better approach: call `mvn verify`.

### Mistake: Using `mvn install` as a release step

- Why wrong: `install` only writes to local repository.
- Better approach: use `deploy` to publish to a remote repository.

### Mistake: Production depends on SNAPSHOT

- Why wrong: SNAPSHOT can change without version change.
- Better approach: depend on immutable release versions.

### Mistake: Plugin versions are not pinned

- Why wrong: future plugin changes can break builds.
- Better approach: use plugin management and explicit versions.

---

## 11. Interview Insight

Strong answer:

> Maven is lifecycle-based. I call a phase such as `verify`, and Maven runs all prior phases in order. The actual work is done by plugin goals bound to phases. Dependencies are resolved from repositories using coordinates and scopes. SNAPSHOT versions are mutable development artifacts, while release versions should be immutable and promoted through environments.

Follow-up trap:

> What is the difference between `package`, `install`, and `deploy`?

Good answer:

> `package` creates the artifact under `target`. `install` copies it to the local Maven repository for local reuse. `deploy` publishes it to a remote repository like Nexus or Artifactory for other builds and environments.

---

## 12. Revision Notes

- One-line summary: Maven is a standard lifecycle that creates and publishes repository artifacts.
- Three keywords: POM, phase, snapshot.
- One interview trap: `install` is local; `deploy` is remote.
- Memory trick: Maven runs a train route; plugins do the work at each station.
