# Java Artifacts, Repositories, Snapshots, Releases Gold Sheet

> Topic: JAR/WAR artifacts, repositories, Nexus/Artifactory, snapshots, release promotion.

---

## 1. Intuition

Java backend delivery revolves around artifacts. A team should be able to point to one artifact version and say: this exact code passed CI, this exact binary is in staging, and this exact binary was promoted to production.

Beginner version:

> A Java artifact is the packaged thing you deploy or share.

---

## 2. Definition

- Definition: A Java artifact is a versioned build output, usually stored in a Maven-compatible repository and identified by coordinates.
- Category: Build/package/release management.
- Core idea: Build once, publish once, promote safely.

---

## 3. Artifact Types

| Artifact | Meaning | Use |
|---|---|---|
| JAR | Java archive | libraries and Spring Boot executable apps |
| WAR | web application archive | app deployed to external servlet container |
| EAR | enterprise archive | older enterprise Java packaging |
| sources JAR | source code archive | IDE/debugging |
| javadoc JAR | API docs | library publishing |
| Docker image | containerized runtime artifact | cloud/Kubernetes deployment |

Spring Boot executable JAR:

```txt
app.jar
  -> application classes
  -> dependencies
  -> embedded server
  -> java -jar app.jar
```

---

## 4. Repository Mental Model

```txt
Developer/CI
   |
   v
build artifact
   |
   v
publish
   |
   v
artifact repository
  Nexus / Artifactory / Maven repository
   |
   v
consumers and deployments
```

Repository types:

- Local repository: `~/.m2/repository`.
- Remote release repository.
- Remote snapshot repository.
- Proxy/cache repository for Maven Central.
- Internal hosted repository for company artifacts.

---

## 5. Coordinates And Repository Path

Coordinates:

```txt
com.example:payments-service:1.2.3
```

Repository path:

```txt
com/example/payments-service/1.2.3/payments-service-1.2.3.jar
```

This mapping lets tools resolve artifacts consistently.

---

## 6. Snapshot Generation

Logical version:

```txt
1.2.4-SNAPSHOT
```

Local artifact:

```txt
payments-service-1.2.4-SNAPSHOT.jar
```

Remote physical artifact:

```txt
payments-service-1.2.4-20260628.091530-7.jar
```

Metadata:

```txt
maven-metadata.xml
```

Why this exists:

- Multiple snapshot deployments can happen for one logical version.
- The remote repository stores timestamped unique builds.
- Metadata tells clients which snapshot build is latest.

Debug checklist when snapshot is stale:

```txt
1. Was the snapshot deployed?
2. Are you using the snapshot repository, not release repository?
3. Did local cache keep an old version?
4. Is update policy preventing refresh?
5. Does CI use a mirror/proxy with stale metadata?
6. Are you actually depending on the same group/artifact/version?
```

---

## 7. Release Artifact Rules

Release versions:

```txt
1.2.3
```

Good release policy:

- Immutable.
- Signed if required.
- Traceable to commit.
- Has test reports.
- Has coverage report.
- Has SBOM where required.
- Promoted through environments.

Bad policy:

```txt
delete and re-upload 1.2.3
```

Why wrong:

- Breaks reproducibility.
- Makes incident investigation unreliable.
- Consumers may cache different binaries under same version.

---

## 8. Artifact Promotion

Weak approach:

```txt
build in dev
build again in QA
build again in prod
```

Strong approach:

```txt
build once
   |
   v
publish artifact
   |
   v
deploy same artifact to dev
   |
   v
promote same artifact to QA
   |
   v
promote same artifact to prod
```

Reason:

> If prod has a bug, you know exactly which tested artifact is running.

---

## 9. Real-World Spring Boot Release

```txt
commit abc123
   |
   v
mvn clean verify
   |
   v
JaCoCo + Sonar
   |
   v
payments-service-1.8.0.jar
   |
   v
container image
payments-service:1.8.0-abc123
   |
   v
deploy to staging
   |
   v
promote to production
```

---

## 10. Common Mistakes

### Mistake: Production uses SNAPSHOT

- Why wrong: artifact can change under same logical version.
- Better approach: use immutable releases.

### Mistake: Artifact version not connected to source

- Why wrong: incidents cannot trace binary to commit.
- Better approach: include commit SHA/build metadata in CI metadata and image labels.

### Mistake: Publishing local builds to shared release repository

- Why wrong: unverified artifacts leak to teams.
- Better approach: publish releases only from CI.

### Mistake: Treating JAR and Docker image as unrelated

- Why wrong: image should identify which app artifact it contains.
- Better approach: label images with artifact version and commit.

---

## 11. Interview Insight

Strong answer:

> In Java systems, artifacts are versioned outputs such as JARs or WARs stored in a repository. SNAPSHOT versions are mutable development builds and remote repositories usually store timestamped physical snapshot files plus metadata. Production should use immutable release artifacts, built once and promoted through environments.

Follow-up trap:

> Why not rebuild the same version in each environment?

Good answer:

> Rebuilding can produce different binaries due to dependency, toolchain, or environment drift. Promotion keeps the binary constant and makes debugging, rollback, and compliance much safer.

---

## 12. Revision Notes

- One-line summary: Java release maturity is artifact immutability plus repository discipline.
- Three keywords: coordinate, snapshot, promote.
- One interview trap: same version must mean same binary for releases.
- Memory trick: Artifacts are the receipts of your build.
