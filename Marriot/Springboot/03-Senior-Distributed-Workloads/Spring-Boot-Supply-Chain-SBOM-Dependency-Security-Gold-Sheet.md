# Spring Boot Supply Chain, SBOM, And Dependency Security Gold Sheet

> Track: Spring Boot Interview Track - Senior Distributed Workloads  
> Goal: make a Spring Boot service explainable and defensible from source code to running artifact.

---

## 1. Intuition

Your service is not just your code. It is your code plus Spring Boot, transitive JARs,
base images, plugins, generated artifacts, container layers, CI secrets, and deployment
metadata. Supply-chain security answers: "What exactly are we running, where did it come
from, and can we prove it is safe enough to ship?"

---

## 2. Definition

- Definition: Supply-chain security protects the integrity, provenance, dependency health,
  and vulnerability posture of the software from source commit to production runtime.
- Category: CI/CD, application security, release engineering, production governance.
- Core idea: build artifacts should be reproducible, inspectable, scanned, signed, and
  traceable back to source.

---

## 3. Why It Exists

Modern Spring Boot apps depend on hundreds of transitive libraries. One vulnerable logging,
serialization, web, crypto, or container dependency can become the weakest point.

Supply-chain controls exist because:

- vulnerabilities appear after release
- transitive dependencies are easy to miss
- base images age quickly
- malicious packages can enter public registries
- CI credentials can leak through build logs or image layers
- production teams need an inventory during incident response

---

## 4. Reality

Real Spring Boot teams use:

- Maven/Gradle dependency locking or controlled version catalogs
- Spring Boot BOM and Spring Cloud BOM alignment
- SBOM generation in CycloneDX or SPDX format
- Actuator SBOM endpoint when enabled and appropriate
- image scanning with tools such as Trivy, Grype, Snyk, or enterprise scanners
- Dependabot/Renovate for dependency PRs
- signed images and provenance attestations
- internal artifact repositories such as Nexus or Artifactory
- policy gates in CI before deploying to production

---

## 5. How It Works

1. Developer commits source code and dependency metadata.
2. CI checks out a specific commit.
3. The wrapper runs the build with pinned Maven or Gradle behavior.
4. Dependencies resolve from approved repositories.
5. Unit, slice, integration, contract, and security tests run.
6. CI creates the executable JAR and/or container image.
7. CI generates an SBOM listing packages and versions.
8. Scanners compare dependencies and image layers against vulnerability databases.
9. Policy gates fail on disallowed severity, license, or provenance issues.
10. Artifact is signed, labeled, and promoted.
11. Runtime metadata links pod/image/version back to commit and SBOM.

Failure path:

- critical CVE discovered in a transitive JAR
- CI pulls an untrusted plugin
- image includes build tools and old OS packages
- app exposes SBOM or Actuator endpoints publicly
- urgent patch cannot identify affected services

Recovery path:

- query SBOM inventory
- patch direct or transitive dependency
- update base image or buildpack
- rebuild from pinned source
- rerun tests/scans
- redeploy with canary and rollback plan

---

## 6. What Problem It Solves

- Primary problem solved: unknown and ungoverned software composition.
- Secondary benefits: faster CVE response, safer releases, audit readiness, rollback clarity.
- Systems impact: reduces security risk across every running service, not just one codebase.

---

## 7. When To Rely On It

Use strong supply-chain controls when:

- services handle money, PII, healthcare, identity, or regulated data
- teams deploy containers to Kubernetes
- code ships frequently
- many microservices share dependencies
- interviews ask about production readiness, security, or release governance

Interviewer triggers:

- "How do you handle Log4Shell-style incidents?"
- "How do you know what dependencies are in production?"
- "How do you scan a Spring Boot image?"
- "How do you prevent dependency drift?"
- "What is an SBOM?"

---

## 8. When Not To Use It

Do not turn supply-chain checks into noisy theater:

- failing every build on low-risk dev-only CVEs slows teams without reducing real risk
- scanning without ownership creates ignored dashboards
- exposing detailed SBOMs publicly can reveal attack surface
- pinning everything manually can fight Spring Boot BOM alignment

Use risk-based policies:

- fail critical/high exploitable runtime CVEs
- warn for dev/test-only issues
- require remediation SLA for medium vulnerabilities
- keep emergency override auditable and time-bound

---

## 9. Pros And Cons

| Pros | Cons |
|---|---|
| Fast vulnerability inventory | Scanner false positives require triage |
| Better audit and incident response | CI can become slower |
| Reproducible builds | Dependency pinning needs discipline |
| Safer base images | Image patching may require frequent rebuilds |
| Stronger release trust | Signing/provenance adds platform complexity |

---

## 10. Trade-offs And Common Mistakes

### Trade-offs

- Gain: security, traceability, confidence.
- Give up: some build simplicity and speed.
- Latency: no request latency impact unless runtime agents are heavy.
- Throughput: CI throughput may drop if scans are serial.
- Cost: scanners, artifact storage, and rebuild automation cost money.
- Complexity: policies must distinguish runtime, test, build, and transitive risk.

### Common Mistakes

| Mistake | Why It Is Wrong | Better Approach |
|---|---|---|
| Scanning only source dependencies | Misses base image CVEs | Scan JAR and image |
| Ignoring transitive dependencies | Most risk is transitive | Use dependency tree and SBOM |
| Exposing all Actuator endpoints | Leaks internals | Restrict management network and auth |
| Building images with full JDK and Maven | Larger attack surface | Use buildpacks or multi-stage images |
| Manually overriding Spring versions randomly | Breaks compatibility | Use Boot BOM, upgrade intentionally |
| No owner for dependency PRs | Patch backlog grows | Assign service ownership and SLAs |

---

## 11. Key Numbers

Approximate interview numbers:

- A medium Spring Boot service can easily include 100 to 300+ transitive JARs.
- Container base images may include hundreds of OS packages.
- Critical/high exploitable runtime CVEs should usually block promotion.
- Emergency patch target: hours to days, depending on exploitability and exposure.
- Routine dependency update cadence: weekly or biweekly for active services.
- SBOM formats commonly discussed: CycloneDX and SPDX.

---

## 12. Failure Modes

| Failure | User Observes | Root Cause | Mitigation |
|---|---|---|---|
| New critical CVE | Security incident | Vulnerable transitive library | SBOM query and patch |
| Image scan fails | Release blocked | Old OS package | Rebase/rebuild image |
| Runtime class conflict | Startup failure | Forced incompatible version | Use Boot BOM alignment |
| Secrets in image layer | Credential exposure | Build args or copied config | BuildKit secrets and secret scanning |
| Dependency confusion | Malicious package | Untrusted repository order | Internal repository proxy and allowlist |
| Slow CI | Developer pain | Serial heavyweight scans | Cache, parallelize, risk-tier checks |

---

## 13. Scenario

- Product/system: hotel booking payment service.
- Why this concept fits: payment services must prove exactly which artifacts and libraries
  were deployed and must patch vulnerabilities quickly.
- What would go wrong without it: a vulnerable transitive dependency could remain unknown
  across many replicas until exploited.

---

## 14. Maven SBOM And Dependency Checks

Example CI-friendly Maven commands:

```bash
./mvnw -B clean verify
./mvnw -B org.cyclonedx:cyclonedx-maven-plugin:makeAggregateBom
./mvnw -B dependency:tree
```

Example image scan:

```bash
trivy image --severity HIGH,CRITICAL --exit-code 1 registry.example.com/booking:abc123
```

Example Actuator posture:

```yaml
management:
  server:
    port: 9001
  endpoints:
    web:
      exposure:
        include: health,info,metrics,prometheus,sbom
  endpoint:
    health:
      probes:
        enabled: true
```

Production note: expose management endpoints only on trusted networks with authentication
and least privilege. Do not put detailed operational endpoints on the public internet.

---

## 15. Mini Program / Simulation

```python
POLICY = {
    "runtime": {"CRITICAL": "block", "HIGH": "block", "MEDIUM": "warn"},
    "test": {"CRITICAL": "warn", "HIGH": "warn", "MEDIUM": "ignore"},
}


def decision(scope, severity, exploitable):
    action = POLICY.get(scope, {}).get(severity, "ignore")
    if action == "block" and not exploitable:
        return "manual-review"
    return action


def main():
    findings = [
        ("runtime", "CRITICAL", True),
        ("runtime", "HIGH", False),
        ("test", "HIGH", True),
    ]
    for finding in findings:
        print(finding, "=>", decision(*finding))


if __name__ == "__main__":
    main()
```

---

## 16. Practical Question

> A critical vulnerability is announced in a popular transitive dependency. How would you
> find affected Spring Boot services and safely patch production?

---

## 17. Strong Answer

I would first query SBOM or dependency inventory to identify affected services, versions,
and whether the vulnerable dependency is in runtime scope. Then I would check exposure:
public endpoint, reachable code path, exploitability, compensating controls. For affected
services, I would update through Spring Boot's BOM or an approved override, run tests and
contract checks, rebuild the image from the exact commit, scan the JAR and container, sign
or attest the artifact if the platform supports it, and canary deploy. I would monitor
error rate, latency, startup, and security logs, then roll out broadly. Finally, I would
add a regression check or dependency policy so the vulnerable version cannot come back.

---

## 18. Revision Notes

- One-line summary: SBOM plus scanning plus signed, reproducible builds tells you what
  you are running and whether it is safe enough to promote.
- Three keywords: SBOM, provenance, policy gate.
- One interview trap: scanning source dependencies only is incomplete because the image
  base layer can be vulnerable too.
- One memory trick: source -> build -> artifact -> image -> deploy -> inventory.

