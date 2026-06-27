# Backend Build Tools Coverage Checklist

Use this checklist to confirm the backend build track covers the requested surface.

---

## Mandatory Coverage

| Area | Status | File |
|---|---|---|
| Build -> Package -> Runtime -> Serve -> Scale | Covered | [Pipeline](./01-Universal-Pipeline/Backend-Build-Package-Runtime-Serve-Scale-Gold-Sheet.md) |
| Universal build pipeline | Covered | [Pipeline](./01-Universal-Pipeline/Backend-Build-Package-Runtime-Serve-Scale-Gold-Sheet.md) |
| Build systems and artifacts | Covered | [Pipeline](./01-Universal-Pipeline/Backend-Build-Package-Runtime-Serve-Scale-Gold-Sheet.md) |
| Build DAG and dependency resolution | Covered | [Dependency Management](./01-Universal-Pipeline/Dependency-Management-Reproducibility-Supply-Chain-Gold-Sheet.md) |
| Direct/transitive dependencies | Covered | [Dependency Management](./01-Universal-Pipeline/Dependency-Management-Reproducibility-Supply-Chain-Gold-Sheet.md) |
| Version conflicts and lockfiles | Covered | [Dependency Management](./01-Universal-Pipeline/Dependency-Management-Reproducibility-Supply-Chain-Gold-Sheet.md) |
| Maven lifecycle and POM | Covered | [Maven](./02-Java-JVM-Builds/Maven-Lifecycle-Dependencies-Snapshots-Gold-Sheet.md) |
| Maven snapshots | Covered | [Maven](./02-Java-JVM-Builds/Maven-Lifecycle-Dependencies-Snapshots-Gold-Sheet.md) |
| Gradle task DAG, caching, parallelism | Covered | [Gradle](./02-Java-JVM-Builds/Gradle-Task-DAG-Caching-Optimization-Gold-Sheet.md) |
| JAR/WAR and repositories | Covered | [Java Artifacts](./02-Java-JVM-Builds/Java-Artifacts-Repositories-Snapshots-Releases-Gold-Sheet.md) |
| Nexus/Artifactory concepts | Covered | [Java Artifacts](./02-Java-JVM-Builds/Java-Artifacts-Repositories-Snapshots-Releases-Gold-Sheet.md) |
| Sonar reports | Covered | [Quality Reports](./03-Quality-Reports/Sonar-JaCoCo-Test-Reports-Quality-Gates-Gold-Sheet.md) |
| JaCoCo reports | Covered | [Quality Reports](./03-Quality-Reports/Sonar-JaCoCo-Test-Reports-Quality-Gates-Gold-Sheet.md) |
| Node.js npm/Yarn/pnpm | Covered | [Node Package Managers](./04-NodeJS-Builds/NodeJS-Package-Managers-npm-yarn-pnpm-Gold-Sheet.md) |
| Node runtime/build/start logs | Covered | [Node Runtime](./04-NodeJS-Builds/NodeJS-Build-Runtime-Serve-Scale-Logs-Gold-Sheet.md) |
| Python pip/venv/Poetry/uv | Covered | [Python Packaging](./05-Python-Builds-Runtime/Python-Packaging-pip-venv-Poetry-uv-Wheels-Gold-Sheet.md) |
| Wheel vs sdist | Covered | [Python Packaging](./05-Python-Builds-Runtime/Python-Packaging-pip-venv-Poetry-uv-Wheels-Gold-Sheet.md) |
| FastAPI + ASGI runtime | Covered | [FastAPI ASGI](./05-Python-Builds-Runtime/Python-FastAPI-ASGI-Runtime-Serve-Scale-Logs-Gold-Sheet.md) |
| CI/CD integration | Covered | [CI/CD](./06-Production-CICD-Logs/Containers-CICD-Caching-Artifact-Promotion-Gold-Sheet.md) |
| Reading backend startup logs | Covered | [Startup Logs](./06-Production-CICD-Logs/Reading-Backend-Startup-Logs-Troubleshooting-Gold-Sheet.md) |

---

## Fast Debug Map

```txt
Build fails before tests?
  -> dependency resolution, compiler/plugin/toolchain issue

Tests pass but Sonar fails?
  -> quality gate, coverage import, rule violation, duplicated code, security hotspot

Coverage missing in Sonar?
  -> JaCoCo/coverage.py/nyc report was not generated before scanner step

Maven SNAPSHOT not updating?
  -> repository metadata, update policy, local cache, wrong repository, missing deploy

Gradle task skipped unexpectedly?
  -> up-to-date check, build cache, task inputs/outputs, configuration cache

Node works locally but fails in CI?
  -> lockfile mismatch, Node version mismatch, native dependency, missing env var

Python works locally but fails in container?
  -> missing system libraries, Python version mismatch, wheel build failure, venv path

App starts then exits?
  -> read logs top-to-bottom: config, dependency init, port bind, DB/cache connection, health
```

---

## Learner Outcome

By the end of this track, you should be able to:

- Explain backend build systems without memorizing commands.
- Debug dependency and artifact issues confidently.
- Explain Maven vs Gradle with architecture maturity.
- Read Sonar and JaCoCo reports like an engineer, not like a checkbox.
- Design Node and Python build pipelines for production.
- Read startup logs for Java, Node, Python, Docker, and Kubernetes.
- Speak about CI/CD, artifacts, runtime, serving, and scale at MAANG level.
