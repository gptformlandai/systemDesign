# Backend Build Tools — Mock Interview Scripts — Gold Sheet

> Three timed interview rounds. Use with a partner or self-drill. Time each answer.

---

## How to Use

1. Set a timer for the recommended duration per question.
2. State your answer aloud — do not type it.
3. After each answer, check the scoring rubric in the Scoring Rubrics file.
4. Mark each question: ✅ Nailed it / 🟡 Partial / ❌ Blank

---

## Round 1 — Foundations (30-minute round)

> Target: Mid-level engineer. Questions test breadth across all topics.

---

**Q1 (2 min) — Maven Basics**

You've joined a Spring Boot project. The POM has 40+ direct dependencies. How do you check for version conflicts and ensure reproducible builds?

*Expected coverage:*
- `mvn dependency:tree` to see transitive tree
- `mvn enforcer:enforce` with banned duplicate dependency versions rule
- BOM imports for framework families (Spring Boot, Spring Cloud)
- SNAPSHOT vs RELEASE version strategy

---

**Q2 (2 min) — Spring Boot Config**

What is the priority order of configuration sources in Spring Boot? Give me three examples where a higher-priority source would override a lower one.

*Expected coverage:*
- CLI args → System properties → Env vars → profile-specific yml → application.yml
- Example 1: `SERVER_PORT=8081` env var overrides `server.port: 8080` in application.yml
- Example 2: `--spring.profiles.active=prod` CLI arg overrides profile in application.yml
- Example 3: K8s ConfigMap mounted as env var overrides application.yml default

---

**Q3 (2 min) — Docker**

Walk me through a production-ready Dockerfile for a Spring Boot service. What choices do you make and why?

*Expected coverage:*
- Multi-stage: separate build (JDK) from runtime (JRE)
- `COPY pom.xml` + `dependency:go-offline` before `COPY src` — layer cache
- Non-root user
- `-XX:MaxRAMPercentage=75` in ENTRYPOINT
- No secrets in ENV or ARG

---

**Q4 (2 min) — JVM Memory**

A Spring Boot pod in Kubernetes is OOM-killed every few hours. Heap metrics look healthy — under 512MB. The container limit is also 512MB. What's happening and how do you fix it?

*Expected coverage:*
- JVM total memory ≠ heap: add Metaspace, thread stacks, direct memory, overhead
- Total typically 1.5-2× heap size
- Fix: `-XX:MaxRAMPercentage=75` + larger container limit OR cap Metaspace
- `-XX:+ExitOnOutOfMemoryError` to fail fast instead of hang

---

**Q5 (2 min) — Database Migrations**

Your team wants to add a NOT NULL column to a 200-million-row orders table during a zero-downtime rolling deployment. How do you do it?

*Expected coverage:*
- Expand: add nullable column first
- Backfill in batches (no full-table lock)
- Deploy new app that writes to both columns
- Contract: add NOT NULL constraint after all rows filled
- Final contract step: drop old column in later release

---

**Q6 (2 min) — Testing**

What is the difference between `@WebMvcTest` and `@SpringBootTest` in Spring Boot? When do you use each?

*Expected coverage:*
- `@WebMvcTest`: loads only web layer (controllers, filters, converters). Fast. No DB. Good for controller logic tests.
- `@SpringBootTest`: loads full application context. Slower. Good for integration tests that need the full stack.
- `@DataJpaTest`: loads only JPA layer. Good for repository tests.
- Test pyramid: 70% unit, 20% slice, 10% full integration

---

**Q7 (2 min) — CI/CD Scenario**

SonarQube is showing 0% coverage after a perfectly passing test run in CI. Name at least two causes and how to diagnose each.

*Expected coverage:*
- Cause 1: JaCoCo `prepare-agent` not configured → no coverage data collected during tests
- Cause 2: Sonar runs before `jacoco:report` generates XML → Sonar reads empty/no file
- Diagnosis: check `target/site/jacoco/jacoco.xml` exists after `mvn verify`
- Fix: correct phase ordering in POM + run `mvn verify` before `sonar:sonar`

---

**Q8 (2 min) — Node.js**

What is the difference between `dependencies` and `devDependencies` in `package.json`, and what happens if you put a runtime library in `devDependencies`?

*Expected coverage:*
- `dependencies`: needed at runtime
- `devDependencies`: only for development/testing/building
- `npm ci` with `NODE_ENV=production` omits devDeps
- Putting runtime lib in devDeps → `Cannot find module` error in production container

---

**Debrief Questions (5 min)**

- Which questions did you feel least confident on?
- Were your answers structured (definition → example → trade-off)?

---

## Round 2 — Depth Drill (45-minute round)

> Target: Senior engineer. Questions test depth and production reasoning.

---

**Q1 (5 min) — Architecture: Build Pipeline**

Design the ideal CI/CD pipeline for a Java Spring Boot monorepo with 8 microservices. Walk through each stage, what runs in parallel, what must be sequential, and how you keep build times under 10 minutes.

*Expected coverage:*
- Parallel: compile + unit test per service (Gradle multi-project parallel task execution)
- Parallel: code quality checks, SAST scan
- Sequential: integration tests (need DB/containers), Docker build, image scan, push to registry
- Caching: Gradle build cache (local or remote), Docker layer cache via registry
- Artifact promotion: push once, promote by tagging (not rebuilding)
- Total: target <10 min via parallelism + caching

---

**Q2 (5 min) — Deep Dive: Gradle Build Cache**

Explain Gradle's build cache. What makes a task cacheable? What breaks the cache? How do you set up a remote cache in CI?

*Expected coverage:*
- Cache key: task class + action implementations + classpath + declared inputs
- Cacheable if: task has `@CacheableTask` annotation and proper `@InputFiles`/`@OutputFiles` declarations
- Breaks cache: undeclared inputs (file system access without `@Input`), non-deterministic inputs (timestamps), volatile env vars used without declaration
- Remote cache: Gradle Enterprise / Develocity, or self-hosted via HTTP cache backend
- `org.gradle.caching=true` in `gradle.properties`

---

**Q3 (5 min) — Security: Supply Chain**

Your security team flags a "dependency confusion" risk in the npm builds. Explain what it is and how you would remediate it in the package manager configuration.

*Expected coverage:*
- Attack: attacker publishes public package with same name as internal private package at higher version
- Package manager resolves public registry first (by default), downloads attacker's package
- Fix: scoped packages (`@company/lib`), `.npmrc` with registry for scope:
  ```
  @company:registry=https://private.registry.company.com/
  //private.registry.company.com/:_authToken=${REGISTRY_TOKEN}
  ```
- Additional: Sigstore attestations, npm provenance, Dependabot alerts

---

**Q4 (5 min) — Testing: TestContainers Strategy**

You're setting up a new Spring Boot service with PostgreSQL. Describe your full testing strategy from unit tests through integration tests. How do you use Testcontainers and why?

*Expected coverage:*
- Unit: pure JUnit 5 + Mockito, no Spring context, fast
- Repository slice: `@DataJpaTest` + Testcontainers PostgreSQLContainer. `@DynamicPropertySource` to wire JDBC URL. `@BeforeEach` to clean data.
- API integration: `@SpringBootTest` + `TestRestTemplate` or `MockMvc`. TestContainers at session scope.
- Testcontainers value: real PostgreSQL behavior vs H2 (no dialect differences, real constraints, real indexes)
- `@DynamicPropertySource` + `@BeforeAll` / session-scoped fixture for container startup

---

**Q5 (5 min) — Observability: Actuator + Prometheus**

How do you wire a Spring Boot service to expose Prometheus metrics and configure Kubernetes health probes? What additional custom metric would you add for a payments service?

*Expected coverage:*
- `spring-boot-starter-actuator` + `micrometer-registry-prometheus`
- `management.endpoints.web.exposure.include: health,prometheus`
- Management port separate from app port: `management.server.port: 8081`
- K8s probes: liveness → `/actuator/health/liveness`, readiness → `/actuator/health/readiness`
- Custom metric: `Counter` for payment attempts/successes/failures, `Timer` for payment processing latency
- Custom: `@Timed` annotation or `registry.timer("payment.processing.duration").record()`

---

**Debrief (5 min)**

- Were your answers concrete (with config/code examples) or abstract?
- Did you mention trade-offs and production constraints?

---

## Round 3 — Incident Simulation (45-minute round)

> Target: Staff engineer. Open-ended production incidents requiring structured diagnosis.

---

**Incident 1 (15 min): The Friday Deploy That Broke the DB**

*Scenario brief:*
On Friday at 17:30, a deployment of `orders-service v3.2.0` was pushed to production. Within 2 minutes, error rates across three services spike to 30%. Services affected: `orders-service`, `payments-service`, `fulfillment-service`. Databases are healthy. All services query the same PostgreSQL orders schema. Your on-call rotation is paged.

Walk me through:
1. Immediate triage steps
2. How you identify that this is a schema migration issue
3. Root cause hypothesis
4. Rollback strategy
5. Prevention for future deployments

*Expected coverage:*
1. Check error messages: "column not found" or "relation not found" in logs. Check `flyway_schema_history` for recent V migration. Compare migration with previous schema state.
2. Identify: V migration with `ALTER TABLE ... DROP COLUMN` or `RENAME COLUMN` — breaking change for services still using old schema.
3. Root cause: expand-contract pattern not followed. Old column dropped in same deployment as code change.
4. Rollback: either redeploy previous version (if Flyway migration can be rolled back with `U` migration) or apply hotfix migration to restore column. Faster: add back column as nullable if no data was deleted.
5. Prevention: expand-contract policy enforced in PR review. Additive-only migrations in rolling deployments. Separate migration deployments from code deployments for destructive changes.

---

**Incident 2 (15 min): The Slow Build That Broke Sprint Velocity**

*Scenario brief:*
Team reports CI builds that previously took 8 minutes now take 34 minutes after onboarding 3 new services to the Maven monorepo. No one changed the pipeline. Developers are starting to bypass CI.

Walk me through:
1. What data you gather first
2. How you identify the bottleneck
3. Three different optimizations you would implement
4. How you measure success

*Expected coverage:*
1. Gather: Maven build timeline (`-Dorg.slf4j.simpleLogger.log.org.apache.maven.cli.transfer.Slf4jMavenTransferListener=warn`), GitHub Actions job timing breakdown, network latency to artifact registry.
2. Identify: `mvn clean verify -pl <module>` timing vs total timing. Check if parallel build enabled. Check if dependency download is slow (artifactory network). Check if `@SpringBootTest` test count × context startup time = the bottleneck.
3. Optimizations:
   - Enable parallel builds: `mvn -T 4 clean verify`
   - Maven build cache / Gradle migration with build cache
   - Fix context cache invalidation from `@MockBean` sprawl → shared `@TestConfiguration`
   - Separate fast unit tests from slow integration tests, run them in parallel CI jobs
   - Cache `~/.m2` between CI runs
4. Measure: P50/P90 build time over 1 week before and after. Rerun benchmark on same commit.

---

**Debrief (5 min)**

- Did you ask clarifying questions before diving into solutions?
- Did you propose both immediate fixes AND long-term prevention?
- Did you quantify impact and success metrics?

---

## Revision Notes

- One-line summary: Three timed rounds — foundations (30 min), depth (45 min), incidents (45 min).
- Format: always lead with "here's the root cause" before the solution.
- One interview trap: Give structured answers — definition, example, trade-off — not just a list.
