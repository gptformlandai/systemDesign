# Docker Mini Projects Portfolio

> Track File #29 of 40 - Group 06: Practice Upgrade
> For: portfolio and interview proof | Level: intermediate to pro | Mode: projects

## 1. Project 1: Containerize A Web API

Deliverables:

- Dockerfile
- `.dockerignore`
- non-root runtime user
- health check
- README run commands
- image size note

Interview proof:

```text
I can explain image layers, build context, port mapping, env vars, health checks, and logging.
```

---

## 2. Project 2: Multi-Service Compose Stack

Deliverables:

- `compose.yaml`
- app service
- database/cache service
- named volume
- user-defined network
- health checks
- `.env.example`

Interview proof:

```text
I can explain Compose DNS, service dependencies, volumes, environment config, and local developer workflow.
```

---

## 3. Project 3: Secure Image Baseline

Deliverables:

- minimal base image decision
- multi-stage build
- non-root user
- no secrets in image
- vulnerability scan notes
- image digest recorded

Interview proof:

```text
I can explain least privilege, supply-chain controls, image scanning, and immutable deployment identity.
```

---

## 4. Project 4: CI/CD Image Promotion

Deliverables:

- build pipeline sketch
- test stage
- scan stage
- push to registry
- digest promotion plan
- rollback plan

Interview proof:

```text
I can explain build once, scan once, promote the same artifact, deploy by digest, and rollback safely.
```

---

## 5. Project 5: Docker Incident Runbook

Deliverables:

- container won't start runbook
- networking failure runbook
- volume permission runbook
- high CPU/memory runbook
- registry pull failure runbook

Interview proof:

```text
I can debug Docker using evidence instead of guessing.
```

---

## 6. Project 6: Production Docker Capstone

Deliverables:

- cache-aware Dockerfile with non-root runtime
- advanced Compose file with profiles and health checks
- Buildx build command with cache strategy
- SBOM/provenance or documented equivalent
- vulnerability scan and policy decision
- registry tag plus digest promotion record
- hardening proof with read-only filesystem, tmpfs, and minimized capabilities
- daemon/network/storage/runbook notes

Interview proof:

```text
I can explain Docker as an end-to-end production workflow: build, trust, ship, run, observe, debug, recover, and roll back by digest.
```

Use [40-Docker-Production-Capstone-Secure-Build-Compose-Registry-Runbook.md](40-Docker-Production-Capstone-Secure-Build-Compose-Registry-Runbook.md) as the project rubric.
