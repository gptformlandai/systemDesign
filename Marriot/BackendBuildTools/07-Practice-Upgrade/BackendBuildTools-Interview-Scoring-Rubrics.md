# Backend Build Tools — Interview Scoring Rubrics — Gold Sheet

> Use after mock interviews. Score each topic 1-5. Identify where to drill next.

---

## Scoring Scale

| Score | Meaning |
|---|---|
| 1 | Blank — no meaningful answer |
| 2 | Vague — some keywords, no structure or accuracy |
| 3 | Competent — correct answer, missing depth or examples |
| 4 | Strong — correct + example + trade-off mentioned |
| 5 | Expert — correct + concrete example + trade-off + edge case or production context |

**Interview readiness gate:** Average ≥ 3.5 across all topics. No topic below 2.

---

## Topic 1: Maven Lifecycle & Build Configuration

| Criterion | 1 | 3 | 5 |
|---|---|---|---|
| Phase ordering | Cannot name phases | Names compile/test/package | Names all phases in order with when each is appropriate |
| Dependency scopes | Cannot distinguish | Knows compile/test | Knows compile/runtime/test/provided + when JDBC driver = runtime |
| `<dependencyManagement>` | Doesn't know | Knows it manages versions | Explains BOM import, `scope=import`, child module inheritance |
| Maven Wrapper | Not mentioned | Knows it exists | Explains reproducible builds, pins Maven version, CI usage |
| Snapshot policy | Unknown | Knows -SNAPSHOT suffix | Explains `-U` flag, `daily` update policy, why SNAPSHOT in prod is risky |

**Your score: ___/5**

---

## Topic 2: Gradle Build System

| Criterion | 1 | 3 | 5 |
|---|---|---|---|
| Task DAG | Unknown | Knows tasks have dependencies | Explains incremental builds, UP-TO-DATE, how inputs/outputs enable caching |
| Build cache | Unknown | Knows it exists | Explains local + remote cache, cache key, what breaks the cache |
| Configuration cache | Unknown | Heard of it | Explains what it serializes, limitations with some plugins, `problems=warn` migration mode |
| Version catalogs | Unknown | Knows `libs.versions.toml` | Explains bundles, aliases, single source of truth for all versions |
| Parallel builds | Unknown | Knows `--parallel` flag | Explains module-level parallelism, decoupled project constraint |

**Your score: ___/5**

---

## Topic 3: Spring Boot Configuration

| Criterion | 1 | 3 | 5 |
|---|---|---|---|
| Property source priority | Unknown | Knows env vars override yml | Lists all levels in order, gives 3 concrete override examples |
| Profile activation | Unknown | Knows `spring.profiles.active` | Knows env var name (`SPRING_PROFILES_ACTIVE`), file naming (`application-{profile}.yml`), profile-name-must-match trap |
| `@ConfigurationProperties` | Unknown | Knows it binds properties | Explains `@Validated` + JSR-303 annotations, relaxed binding (underscore → camelCase) |
| Actuator exposure | Unknown | Knows health endpoint | Explains liveness vs readiness distinction, management port isolation, not exposing `/env` |
| Externalized config (K8s) | Unknown | Knows env vars work | Explains ConfigMap → env vars → Spring relaxed binding, Secrets for sensitive values |

**Your score: ___/5**

---

## Topic 4: JVM Memory & Tuning

| Criterion | 1 | 3 | 5 |
|---|---|---|---|
| JVM memory regions | Unknown | Knows heap exists | Explains heap (Young/Old), Metaspace, thread stacks, direct memory — all count toward RSS |
| Container memory sizing | Unknown | Knows container limit must be > heap | Explains `-XX:MaxRAMPercentage=75` self-adjusting, total JVM = ~1.8× heap |
| GC selection | Unknown | Knows G1GC is default | Compares G1GC vs ZGC (latency trade-off), explains when to choose ZGC |
| OOM diagnosis | Unknown | Knows jmap/jstack exist | Explains heap dump analysis, checking Metaspace growth, `jstat -gcmetacapacity` |
| Production flags | Unknown | Knows -Xmx | Knows full set: MaxRAMPercentage + MaxMetaspace + ExitOnOutOfMemoryError + GC flags |

**Your score: ___/5**

---

## Topic 5: Java Testing Strategy

| Criterion | 1 | 3 | 5 |
|---|---|---|---|
| Test pyramid | Unknown | Knows unit/integration distinction | States 70/20/10 ratio, explains why each layer exists and cost |
| `@WebMvcTest` vs `@SpringBootTest` | Unknown | Knows one loads less | Explains what each loads, when to use each, performance implications |
| `@MockBean` vs `@Mock` | Unknown | Knows they're different | Explains context cache invalidation caused by `@MockBean`, `@Mock` is Mockito only |
| Testcontainers | Unknown | Knows it spins up Docker | Explains `@DynamicPropertySource`, session vs function scope, why H2 is insufficient |
| Test data management | Unknown | Knows about `@BeforeEach` cleanup | Explains `@Transactional` rollback, `@Sql`, TestEntityManager, data isolation strategy |

**Your score: ___/5**

---

## Topic 6: Database Migrations (Flyway / Liquibase)

| Criterion | 1 | 3 | 5 |
|---|---|---|---|
| Flyway naming | Unknown | Knows V prefix + double underscore | States `V{version}__{description}.sql`, explains V vs R vs U prefixes |
| Applied migration rule | Unknown | Knows not to modify | Explains checksum validation failure, `repair` command, why editing is dangerous |
| Zero-downtime migrations | Unknown | Knows not to drop columns immediately | Fully explains expand-and-contract: add nullable → backfill → code deploy → drop old |
| Flyway vs Liquibase | Unknown | Knows both exist | Compares SQL-native (Flyway) vs XML/YAML/JSON/SQL multi-format (Liquibase), offline preview (Liquibase update-sql) |
| CI integration | Unknown | Knows it can run in CI | Explains `mvn flyway:info` for pre-deploy check, Testcontainers + Flyway for integration tests |

**Your score: ___/5**

---

## Topic 7: Python Build & Testing

| Criterion | 1 | 3 | 5 |
|---|---|---|---|
| Virtual environment | Unknown | Knows venv exists | Explains isolation, compares pip/venv vs Poetry vs uv, lockfile purpose |
| pytest fixtures | Unknown | Knows `@pytest.fixture` | Explains scope (function/session), `yield` for teardown, parametrize |
| Async testing | Unknown | Knows asyncio tests are different | Explains `pytest.mark.asyncio`, `asyncio_mode="auto"`, `AsyncMock`, `ASGITransport` |
| Patch at usage site | Unknown | Knows patch exists | States rule: patch where name is used, not where defined. Gives correct vs incorrect example |
| Coverage configuration | Unknown | Knows `pytest --cov` | Explains `branch=true`, `fail_under`, excludes pattern, XML report for Sonar |

**Your score: ___/5**

---

## Topic 8: Node.js Builds

| Criterion | 1 | 3 | 5 |
|---|---|---|---|
| npm vs yarn vs pnpm | Unknown | Knows they're package managers | Explains key differences: pnpm hard links, Yarn Berry PnP, Corepack version pinning |
| `npm ci` vs `npm install` | Unknown | Knows ci is for CI | Explains lockfile-exact, deletes node_modules, fails if inconsistent — reproducible |
| devDependencies in production | Unknown | Knows they're excluded | Explains `NODE_ENV=production` + `--omit=dev`, common bug of runtime deps in devDeps |
| Docker layer cache for Node | Unknown | Knows deps first | Explains `COPY package*.json ./` → `npm ci` → `COPY src` → build pattern |
| Node.js in containers | Unknown | Knows CMD syntax | Explains `CMD ["node", ...]` exec form vs shell form, graceful shutdown with SIGTERM |

**Your score: ___/5**

---

## Topic 9: Docker & Container Engineering

| Criterion | 1 | 3 | 5 |
|---|---|---|---|
| Multi-stage builds | Unknown | Knows build vs runtime stages | Explains `COPY --from=build`, JDK vs JRE, final image size savings |
| Layer caching | Unknown | Knows layers cache | Explains COPY ordering strategy, what invalidates layers, registry cache for CI |
| BuildKit secrets | Unknown | Knows `--secret` flag | Explains why ARG leaks in `docker history`, `--mount=type=secret`, secret never in layer |
| Non-root user | Unknown | Knows `USER` directive | Explains why root = risk, `addgroup`/`adduser` pattern, Kubernetes security context |
| Vulnerability scanning | Unknown | Knows Trivy exists | Explains Trivy scan targets (OS + app deps), `--exit-code 1` for CI gate, SBOM generation |

**Your score: ___/5**

---

## Topic 10: Code Quality (Sonar + JaCoCo)

| Criterion | 1 | 3 | 5 |
|---|---|---|---|
| JaCoCo setup | Unknown | Knows it measures coverage | Explains `prepare-agent` goal binding, `report` in verify phase, XML output for Sonar |
| Coverage ordering in CI | Unknown | Knows Sonar needs coverage | States correct order: tests → report → sonar. Explains what 0% means in diagnosis |
| Quality Gate vs Quality Profile | Unknown | Knows what a gate is | Distinguishes: Profile = which rules; Gate = thresholds on metrics (coverage, bugs, etc.) |
| Sonar PR decoration | Unknown | Knows Sonar can show in PRs | Explains branch analysis, PR decoration shows diff-only coverage, `sonar.pullrequest.*` properties |
| SonarLint | Unknown | Knows it's an IDE plugin | Explains connected mode, catches issues before CI, rule sync with server profile |

**Your score: ___/5**

---

## Readiness Assessment

### Score Summary Table

| Topic | Score | Status |
|---|---|---|
| 1. Maven Lifecycle | ___ | |
| 2. Gradle | ___ | |
| 3. Spring Boot Config | ___ | |
| 4. JVM Memory | ___ | |
| 5. Java Testing | ___ | |
| 6. Database Migrations | ___ | |
| 7. Python Build & Testing | ___ | |
| 8. Node.js Builds | ___ | |
| 9. Docker & Containers | ___ | |
| 10. Code Quality | ___ | |
| **Average** | **___** | |

### Readiness Gates

| Gate | Threshold | Met? |
|---|---|---|
| Average score | ≥ 3.5 | |
| No topic below | 2 | |
| Can explain 3 production incidents | Without notes | |
| Can write Dockerfile from memory | Multi-stage Java | |
| Can write correct Flyway migration | NOT NULL + rolling deploy safe | |

### Decision

- **Average ≥ 4.0, no topic below 3:** Apply immediately
- **Average 3.0-3.9:** One more targeted drill week on lowest 3 topics
- **Average 2.0-2.9:** Two weeks of focused study before target interviews
- **Average < 2.0:** Return to gold sheets systematically

---

## Revision Notes

- One-line summary: 10-topic rubric, score 1-5, readiness gate = avg 3.5 with no topic below 2.
- Self-assessment cadence: weekly score tracking, compare before/after targeted drills.
- One interview trap: Having a 5 on one topic doesn't compensate for a 1 on another — interviewers probe all areas.
