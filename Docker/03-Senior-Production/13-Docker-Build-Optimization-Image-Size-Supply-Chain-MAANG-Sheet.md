# Docker Build Optimization, Image Size, and Supply Chain - MAANG Sheet

> Track File #13 of 40 - Group 03: Senior Production
> For: senior build/platform interviews | Level: senior | Mode: build performance and supply chain

## 1. Core Idea

Production images should be fast to build, small to ship, secure to run, and traceable to source.

```text
source -> reproducible build -> small scanned image -> signed/promoted artifact -> deployment
```

---

## 2. Optimization Levers

| Lever | Why It Matters |
|---|---|
| `.dockerignore` | smaller context, fewer leaks |
| cache-aware COPY order | faster rebuilds |
| multi-stage builds | remove build tools from runtime |
| minimal base images | smaller attack surface |
| dependency lock files | reproducibility |
| SBOM | dependency visibility |
| vulnerability scanning | risk detection |
| provenance/signing | supply-chain trust |

---

## 3. Commands

```bash
docker build -t app:local .
docker build --no-cache -t app:nocache .
docker history app:local
docker image inspect app:local
docker scout cves app:local 2>/dev/null || true
```

---

## 4. Strong Dockerfile Pattern

```text
base dependencies -> copy lock files -> install deps -> copy source -> build -> copy artifact into runtime image
```

Avoid:

- package manager caches left in runtime
- compilers/build tools in final image
- secrets in build args or copied files
- unpinned dependencies

---

## 5. Production Failure Modes

- huge images slow deploys and increase CVE count
- build cache misses cause slow CI
- base image vulnerable and not updated
- image provenance unknown
- staging and production images built separately and differ

---

## 6. Interview Summary

```text
I optimize Docker builds with .dockerignore, cache-aware layer ordering, multi-stage builds, small trusted bases, dependency locks, scanning, SBOMs, and artifact promotion. Production should deploy the same scanned image digest that passed CI, not rebuild separately per environment.
```

---

## 7. Revision Notes

- One-line summary: Build quality affects speed, cost, security, and deploy confidence.
- Three keywords: cache, SBOM, digest.
- One trap: rebuilding separately for each environment instead of promoting the same artifact.