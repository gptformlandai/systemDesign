# Docker CI/CD, Registries, Scanning, and Promotion - Gold Sheet

> Track File #15 of 40 - Group 03: Senior Production
> For: platform/DevOps interviews | Level: senior | Mode: container delivery pipeline

## 1. Core Idea

Docker CI/CD should produce one trustworthy image artifact and promote that artifact through environments.

```text
build -> test -> scan -> sign/provenance -> push -> deploy by digest -> monitor -> rollback
```

---

## 2. Pipeline Stages

| Stage | Purpose |
|---|---|
| build | create image from source and Dockerfile |
| test | unit/integration/container smoke tests |
| scan | vulnerabilities, secrets, licenses, policy |
| publish | push to registry with tag and digest |
| promote | same digest moves dev -> staging -> prod |
| deploy | orchestrator pulls approved image |
| observe | monitor logs, health, metrics |
| rollback | redeploy previous known-good digest |

---

## 3. Tag Strategy

Use multiple tags carefully:

```text
app:git-sha
app:1.2.3
app:release-2026-07-01
```

Record digest for exact deployment:

```text
app@sha256:...
```

---

## 4. Production Gates

- build reproducibility
- test pass
- critical CVE policy
- no secrets in image
- SBOM produced
- image signed or provenance recorded
- deploy by digest or immutable tag
- rollback digest available

---

## 5. Failure Modes

- mutable tag points to wrong image
- prod rebuild differs from tested image
- registry retention deletes rollback image
- scanning not enforced
- secrets leaked in image layer
- architecture mismatch in multi-platform deploy

---

## 6. Interview Summary

```text
In Docker CI/CD, I build once, test and scan the image, push it to a registry, record the digest, promote the same artifact across environments, deploy by digest or immutable tag, and keep rollback images available.
```

---

## 7. Revision Notes

- One-line summary: Production container delivery should promote artifacts, not rebuild guesses.
- Three keywords: scan, digest, promote.
- One trap: deploying `latest` and losing exact rollback identity.