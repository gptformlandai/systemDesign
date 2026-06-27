# Sonar, JaCoCo, Test Reports, Quality Gates Gold Sheet

> Topic: code quality analysis, coverage reports, test reports, quality gates, and CI interpretation.

---

## 1. Intuition

Tests answer: does expected behavior work? Coverage answers: how much code did tests execute? Sonar answers: is the code maintainable, secure, duplicated, risky, or violating agreed standards?

Beginner version:

> JaCoCo measures test coverage. Sonar evaluates code quality and can import coverage.

---

## 2. Definition

- Definition: Backend quality reporting is the CI process that generates test, coverage, static analysis, security, and maintainability signals before an artifact is promoted.
- Category: Quality engineering and release governance.
- Core idea: Do not deploy code that fails your quality contract.

---

## 3. Quality Pipeline

```txt
compile
   |
   v
unit tests
   |
   v
test report
   |
   v
coverage instrumentation
   |
   v
JaCoCo XML/HTML report
   |
   v
Sonar scanner imports code + reports
   |
   v
quality gate
   |
   v
package/promote artifact
```

Important order:

```txt
generate coverage before Sonar analysis
```

If coverage report does not exist before scanning, Sonar cannot import it.

---

## 4. JaCoCo

JaCoCo is a Java code coverage library.

It can produce:

- HTML report for humans.
- XML report for Sonar and CI tools.
- `.exec` execution data.

Maven mental model:

```txt
jacoco:prepare-agent
   |
   v
test JVM runs with coverage agent
   |
   v
jacoco.exec created
   |
   v
jacoco:report
   |
   v
target/site/jacoco/jacoco.xml
```

Gradle mental model:

```txt
test
   |
   v
jacocoTestReport
   |
   v
build/reports/jacoco/test/jacocoTestReport.xml
```

---

## 5. Maven Example

```bash
mvn clean verify
```

Typical POM plugin shape:

```xml
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
```

Why `verify` is useful:

- unit tests have run.
- integration-test reports may be available.
- coverage report can be generated.
- quality checks can evaluate final test results.

---

## 6. Gradle Example

```kotlin
plugins {
    jacoco
}

tasks.test {
    useJUnitPlatform()
    finalizedBy(tasks.jacocoTestReport)
}

tasks.jacocoTestReport {
    dependsOn(tasks.test)
    reports {
        xml.required.set(true)
        html.required.set(true)
    }
}
```

Command:

```bash
./gradlew test jacocoTestReport
```

---

## 7. Sonar

SonarQube/SonarCloud analyzes:

- bugs.
- vulnerabilities.
- code smells.
- duplications.
- coverage.
- maintainability.
- security hotspots.
- quality gate status.

Maven command:

```bash
mvn clean verify org.sonarsource.scanner.maven:sonar-maven-plugin:sonar \
  -Dsonar.token=$SONAR_TOKEN
```

Gradle command:

```bash
./gradlew clean test jacocoTestReport sonar \
  -Dsonar.token=$SONAR_TOKEN
```

Node/Python projects can use Sonar scanner too, but report generation differs:

```txt
Node -> jest/vitest + coverage/lcov.info
Python -> pytest + coverage.py XML
Sonar -> imports coverage report path
```

---

## 8. Quality Gate

A quality gate is a release policy.

Examples:

- coverage on new code >= 80%.
- no blocker vulnerabilities.
- no new critical bugs.
- duplication under threshold.
- maintainability rating acceptable.

Important maturity:

> Prefer quality gates on new/changed code over punishing legacy code forever.

Why:

- Legacy systems may start with low coverage.
- New code policy prevents things from getting worse.
- Teams can improve legacy areas incrementally.

---

## 9. Reading Reports

### Test Report

Ask:

- Which tests failed?
- Was it unit, integration, or contract test?
- Is it deterministic?
- Is the failure environment-specific?
- Did test discovery run correctly?

### JaCoCo Report

Ask:

- Is XML report generated?
- Is coverage low because tests are missing or report path is wrong?
- Are generated files excluded?
- Are integration tests included?
- Is branch coverage acceptable?

### Sonar Report

Ask:

- Did scanner run after reports were generated?
- Did analysis scope include right files?
- Are exclusions hiding important code?
- Did quality gate fail because of real issue or config?
- Are secrets/tokens absent from logs?

---

## 10. Real-World CI

```yaml
steps:
  - run: mvn -B clean verify
  - run: mvn -B org.sonarsource.scanner.maven:sonar-maven-plugin:sonar
  - run: mvn -B deploy
```

Better release policy:

```txt
deploy artifact only if:
  tests pass
  coverage report generated
  Sonar quality gate passes
  vulnerability scan acceptable
```

---

## 11. Common Mistakes

### Mistake: Running Sonar before coverage report generation

- Why wrong: Sonar analysis cannot import coverage that does not exist.
- Better approach: run tests and coverage first.

### Mistake: Chasing 100% coverage

- Why wrong: coverage can be high with poor assertions.
- Better approach: use coverage as risk signal, not as proof of correctness.

### Mistake: Excluding too much code

- Why wrong: reports become meaningless.
- Better approach: exclude generated code, not hard business logic.

### Mistake: Treating Sonar as only a blocker

- Why wrong: teams learn to bypass it.
- Better approach: use findings for code-health feedback and actionable gates.

---

## 12. Quality Profile vs Quality Gate

**Quality Profile** = which rules are active and at which severity.
**Quality Gate** = thresholds on metrics that must pass for promotion.

```
Quality Profile: "Company Java Profile"
  - rule: S1135 (TODO comments) → MAJOR
  - rule: S2095 (resource leaks) → CRITICAL
  - rule: S3776 (cognitive complexity) → MAJOR
  [300+ rules configured]

Quality Gate: "Company Standard"
  - Coverage on new code ≥ 80%
  - Bugs on new code = 0 (blocking severity only)
  - Critical vulnerabilities = 0
  - Technical debt ratio ≤ 5%
```

Multiple projects can share Quality Profiles and Gates — change the profile once, all projects use the new rules on next scan.

---

## 13. PR Decoration / Branch Analysis

Sonar shows coverage diff only for changed lines in a pull request:

```bash
# GitHub Actions PR analysis
mvn -B clean verify sonar:sonar \
  -Dsonar.pullrequest.key=${{ github.event.pull_request.number }} \
  -Dsonar.pullrequest.branch=${{ github.head_ref }} \
  -Dsonar.pullrequest.base=${{ github.base_ref }} \
  -Dsonar.token=$SONAR_TOKEN
```

**PR decoration shows:**
- New issues introduced by this PR (not existing issues)
- Coverage delta on changed files
- Status check posted back to GitHub (passes/fails based on Quality Gate)
- Inline comments on lines with issues

**Branch analysis:** Each branch has its own analysis history. The main branch tracks long-term metrics; feature branches show only the delta from the base branch.

---

## 14. SonarLint — IDE Integration

SonarLint is the IDE plugin that catches issues before CI:

```
VS Code: install SonarLint extension
IntelliJ: install SonarLint plugin

Connected mode: link to SonarQube/SonarCloud server
  → syncs your project's Quality Profile rules to IDE
  → same rules in IDE as in CI — no surprises
```

Benefits:
- Issues flagged in editor as you type (before commit)
- "Quick fix" suggestions for common rules
- No CI round-trip required for rule feedback

---

## 15. OWASP Dependency Check Integration

```xml
<!-- pom.xml — OWASP Dependency Check plugin -->
<plugin>
  <groupId>org.owasp</groupId>
  <artifactId>dependency-check-maven</artifactId>
  <version>11.1.0</version>
  <configuration>
    <failBuildOnCVSS>7</failBuildOnCVSS>  <!-- fail on HIGH/CRITICAL (CVSS ≥ 7) -->
    <formats>XML,HTML</formats>
    <outputDirectory>${project.build.directory}/owasp-reports</outputDirectory>
  </configuration>
  <executions>
    <execution>
      <goals><goal>check</goal></goals>
    </execution>
  </executions>
</plugin>
```

```bash
# Run separately (slow — downloads NVD database)
mvn dependency-check:check

# Import report into Sonar (via sonar-dependency-check-plugin)
mvn sonar:sonar -Dsonar.dependencyCheck.xmlReportPath=target/owasp-reports/dependency-check-report.xml
```

OWASP check complements Sonar: Sonar focuses on code quality/static analysis, OWASP checks dependency CVEs.

---

## 16. Interview Insight

Strong answer:

> I treat quality reporting as part of the build contract. Tests create behavior evidence. JaCoCo creates Java coverage evidence. Sonar imports source and reports, then applies quality gates. The important implementation detail is ordering: coverage reports must exist before Sonar runs, and gates should focus strongly on new code to improve quality without freezing legacy delivery.

Follow-up trap:

> Sonar shows 0% coverage, but tests ran. What do you check?

Good answer:

> I check whether the coverage report was generated, whether XML output is enabled, whether Sonar's report path matches the file, whether the scanner ran after tests, and whether modules/source paths match the analysis scope.

---

## 17. Revision Notes

- One-line summary: JaCoCo measures Java coverage; Sonar evaluates broader code quality and imports reports.
- Three keywords: report, gate, order.
- One interview trap: coverage must be generated before analysis.
- Memory trick: Tests create evidence; Sonar judges the evidence.

Strong answer:

> I treat quality reporting as part of the build contract. Tests create behavior evidence. JaCoCo creates Java coverage evidence. Sonar imports source and reports, then applies quality gates. The important implementation detail is ordering: coverage reports must exist before Sonar runs, and gates should focus strongly on new code to improve quality without freezing legacy delivery.

Follow-up trap:

> Sonar shows 0% coverage, but tests ran. What do you check?

Good answer:

> I check whether the coverage report was generated, whether XML output is enabled, whether Sonar's report path matches the file, whether the scanner ran after tests, and whether modules/source paths match the analysis scope.

---

## 13. Revision Notes

- One-line summary: JaCoCo measures Java coverage; Sonar evaluates broader code quality and imports reports.
- Three keywords: report, gate, order.
- One interview trap: coverage must be generated before analysis.
- Memory trick: Tests create evidence; Sonar judges the evidence.
