# Docker Mastery Track - Beginner To Pro Index

This folder is a complete Docker mastery track for developers, backend engineers, DevOps/SRE engineers, cloud engineers, and system design interviews.

It contains the original 30-file core track, appendix #31 for production internals, senior gap-fill files #32-#39, and capstone #40.

It teaches Docker as a production containerization platform, not just a list of commands.

```text
application -> Dockerfile -> image layers -> container runtime -> network/storage/security -> production deployment -> debugging answer
```

Use this track if:

- You want beginner-to-pro Docker confidence for local development, CI/CD, and production systems.
- You want to understand images, containers, Dockerfiles, volumes, networking, Compose, registries, and debugging deeply.
- You want MAANG-level interview answers connecting Docker to Linux namespaces, cgroups, security, supply chain, and orchestration.
- You want hands-on labs, runbooks, and portfolio projects instead of reading-only notes.

---

## 1. Learning Style: Beginner To Pro Loop

Every topic should be learned with this loop:

```text
concept -> command -> Docker object -> runtime behavior -> failure mode -> fix -> production scenario -> interview explanation
```

Docker mastery is not memorizing `docker run`. It is understanding what image, container, network, volume, registry, and runtime state prove.

---

## 2. Track Structure

| Group | Folder | Purpose |
|---:|---|---|
| 1 | `01-Foundations` | Docker mental model, daemon/CLI, images, containers, Dockerfile basics |
| 2 | `02-Intermediate-Practical` | daily commands, Dockerfile cache, volumes, networking, Compose, registries |
| 3 | `03-Senior-Production` | security, performance, build optimization, observability, CI/CD, orchestration bridge, pro internals, daemon, Buildx, networking, storage, platform boundaries |
| 4 | `04-Scenario-Practice` | containerize app, debug startup, network, volume, build, registry, production incidents |
| 5 | `05-Special-Interview-Rounds` | Q&A, command maps, anti-patterns, debugging traps |
| 6 | `06-Practice-Upgrade` | active recall, drills, mini projects, production readiness checklist, final capstone |
| Lab | `docker-mastery-lab` | Dockerfiles, Compose examples, scripts, labs, projects, cheatsheets, interview prep, runbooks |

---

## 3. Foundations Path

| Order | File | What It Builds |
|---:|---|---|
| 1 | [01-Foundations/01-Docker-Mental-Model-Images-Containers-Daemon-Hot-Sheet.md](01-Foundations/01-Docker-Mental-Model-Images-Containers-Daemon-Hot-Sheet.md) | Docker mental model, image vs container, daemon, client, registry |
| 2 | [01-Foundations/02-Docker-Install-CLI-Daemon-Registry-Basics-Gold-Sheet.md](01-Foundations/02-Docker-Install-CLI-Daemon-Registry-Basics-Gold-Sheet.md) | Docker CLI, daemon health, Docker Desktop/Engine, registry basics |
| 3 | [01-Foundations/03-Docker-Images-Layers-Containers-Lifecycle-Gold-Sheet.md](01-Foundations/03-Docker-Images-Layers-Containers-Lifecycle-Gold-Sheet.md) | images, layers, writable container layer, lifecycle states |
| 4 | [01-Foundations/04-Docker-Dockerfile-Build-Context-Basics-Gold-Sheet.md](01-Foundations/04-Docker-Dockerfile-Build-Context-Basics-Gold-Sheet.md) | Dockerfile instructions, build context, `.dockerignore`, first image |

Foundation target:

- You can explain image vs container vs registry vs daemon.
- You can run and inspect containers safely.
- You can build a basic image and understand build context.

---

## 4. Intermediate Practical Path

| Order | File | What It Builds |
|---:|---|---|
| 5 | [02-Intermediate-Practical/05-Docker-Commands-Run-Exec-Logs-Inspect-Gold-Sheet.md](02-Intermediate-Practical/05-Docker-Commands-Run-Exec-Logs-Inspect-Gold-Sheet.md) | `run`, `ps`, `exec`, `logs`, `inspect`, `cp`, cleanup commands |
| 6 | [02-Intermediate-Practical/06-Dockerfile-Layers-Cache-Multistage-Best-Practices-Gold-Sheet.md](02-Intermediate-Practical/06-Dockerfile-Layers-Cache-Multistage-Best-Practices-Gold-Sheet.md) | layer cache, multi-stage builds, small secure images |
| 7 | [02-Intermediate-Practical/07-Docker-Volumes-Bind-Mounts-Persistence-Gold-Sheet.md](02-Intermediate-Practical/07-Docker-Volumes-Bind-Mounts-Persistence-Gold-Sheet.md) | named volumes, bind mounts, persistence, data ownership |
| 8 | [02-Intermediate-Practical/08-Docker-Networking-Ports-DNS-Bridge-Gold-Sheet.md](02-Intermediate-Practical/08-Docker-Networking-Ports-DNS-Bridge-Gold-Sheet.md) | bridge networks, ports, container DNS, localhost traps |
| 9 | [02-Intermediate-Practical/09-Docker-Compose-Services-Env-Dependencies-Gold-Sheet.md](02-Intermediate-Practical/09-Docker-Compose-Services-Env-Dependencies-Gold-Sheet.md) | Compose services, env vars, health checks, dependencies |
| 10 | [02-Intermediate-Practical/10-Docker-Registry-Tagging-Push-Pull-Versioning-Gold-Sheet.md](02-Intermediate-Practical/10-Docker-Registry-Tagging-Push-Pull-Versioning-Gold-Sheet.md) | tags, digests, push/pull, registry auth, versioning |

Practical target:

- You can build, run, inspect, debug, network, persist, and publish Docker workloads.
- You can interpret Docker command output and decide the next check.

---

## 5. Senior Production Path

| Order | File | What It Builds |
|---:|---|---|
| 11 | [03-Senior-Production/11-Docker-Security-Rootless-User-Secrets-Capabilities-MAANG-Sheet.md](03-Senior-Production/11-Docker-Security-Rootless-User-Secrets-Capabilities-MAANG-Sheet.md) | non-root containers, capabilities, secrets, rootless mode, image trust |
| 12 | [03-Senior-Production/12-Docker-Performance-Resource-Limits-Cgroups-Healthchecks-MAANG-Sheet.md](03-Senior-Production/12-Docker-Performance-Resource-Limits-Cgroups-Healthchecks-MAANG-Sheet.md) | CPU/memory limits, cgroups, health checks, restart policies |
| 13 | [03-Senior-Production/13-Docker-Build-Optimization-Image-Size-Supply-Chain-MAANG-Sheet.md](03-Senior-Production/13-Docker-Build-Optimization-Image-Size-Supply-Chain-MAANG-Sheet.md) | build speed, image size, SBOM, scanning, provenance |
| 14 | [03-Senior-Production/14-Docker-Observability-Logs-Events-Debugging-Gold-Sheet.md](03-Senior-Production/14-Docker-Observability-Logs-Events-Debugging-Gold-Sheet.md) | logs, events, inspect, stats, exec debugging, crash loops |
| 15 | [03-Senior-Production/15-Docker-CI-CD-Registries-Scanning-Promotion-Gold-Sheet.md](03-Senior-Production/15-Docker-CI-CD-Registries-Scanning-Promotion-Gold-Sheet.md) | build pipelines, registry promotion, immutable tags, scanning gates |
| 16 | [03-Senior-Production/16-Docker-Orchestration-Bridge-Kubernetes-Compose-Swarm-MAANG-Sheet.md](03-Senior-Production/16-Docker-Orchestration-Bridge-Kubernetes-Compose-Swarm-MAANG-Sheet.md) | Docker vs Compose vs Kubernetes, deployment boundaries |
| Appendix 31 | [03-Senior-Production/31-Docker-Pro-Gap-Fill-PID1-BuildKit-MultiPlatform-Disk-Pressure-MAANG-Sheet.md](03-Senior-Production/31-Docker-Pro-Gap-Fill-PID1-BuildKit-MultiPlatform-Disk-Pressure-MAANG-Sheet.md) | full MAANG deep dive on PID 1, signals, BuildKit secrets/cache, multi-platform images, OCI internals, disk pressure |
| 32 | [03-Senior-Production/32-Docker-Compose-Advanced-Profiles-Overrides-Secrets-Watch-MAANG-Sheet.md](03-Senior-Production/32-Docker-Compose-Advanced-Profiles-Overrides-Secrets-Watch-MAANG-Sheet.md) | advanced Compose profiles, overrides, env precedence, secrets, watch, readiness |
| 33 | [03-Senior-Production/33-Docker-Daemon-Host-Operations-Contexts-Logging-TLS-MAANG-Sheet.md](03-Senior-Production/33-Docker-Daemon-Host-Operations-Contexts-Logging-TLS-MAANG-Sheet.md) | daemon operations, contexts, socket/TLS, logging drivers, live restore, host debugging |
| 34 | [03-Senior-Production/34-Docker-Advanced-Security-Hardening-CIS-Namespaces-LSM-MAANG-Sheet.md](03-Senior-Production/34-Docker-Advanced-Security-Hardening-CIS-Namespaces-LSM-MAANG-Sheet.md) | seccomp, AppArmor, SELinux, user namespaces, rootless, CIS-style hardening |
| 35 | [03-Senior-Production/35-Docker-Supply-Chain-SBOM-Scout-Provenance-Signing-MAANG-Sheet.md](03-Senior-Production/35-Docker-Supply-Chain-SBOM-Scout-Provenance-Signing-MAANG-Sheet.md) | SBOM, scanning policy, provenance, signing, digest promotion, image trust |
| 36 | [03-Senior-Production/36-Docker-Buildx-BuildKit-Bake-Advanced-MAANG-Sheet.md](03-Senior-Production/36-Docker-Buildx-BuildKit-Bake-Advanced-MAANG-Sheet.md) | Buildx builders, BuildKit cache/secrets/SSH, multi-platform builds, Bake |
| 37 | [03-Senior-Production/37-Docker-Advanced-Networking-Drivers-DNS-Firewall-MAANG-Sheet.md](03-Senior-Production/37-Docker-Advanced-Networking-Drivers-DNS-Firewall-MAANG-Sheet.md) | network drivers, DNS rules, port paths, host firewall/NAT, MTU/proxy debugging |
| 38 | [03-Senior-Production/38-Docker-Advanced-Storage-Overlay2-Tmpfs-Volumes-MAANG-Sheet.md](03-Senior-Production/38-Docker-Advanced-Storage-Overlay2-Tmpfs-Volumes-MAANG-Sheet.md) | overlay2 mental model, tmpfs, bind mounts, volumes, backups, Desktop storage boundary |
| 39 | [03-Senior-Production/39-Docker-Desktop-Platform-Boundaries-Registry-Alternatives-MAANG-Sheet.md](03-Senior-Production/39-Docker-Desktop-Platform-Boundaries-Registry-Alternatives-MAANG-Sheet.md) | Docker Desktop, Windows/WSL2, platform architecture, registry operations, runtime alternatives |

Senior target:

- You can explain Docker in production: security, reliability, resource limits, supply chain, CI/CD, observability, and orchestration tradeoffs.
- You can use appendix #31 and files #32-#39 for the deeper architect-level layer: internals, daemon operations, advanced Compose, Buildx, hardening, supply chain, networking, storage, registry, and platform boundaries.

---

## 6. Scenario Practice Path

| Order | File | What It Builds |
|---:|---|---|
| 17 | [04-Scenario-Practice/17-Docker-Containerize-Web-App-Scenario-Gold-Sheet.md](04-Scenario-Practice/17-Docker-Containerize-Web-App-Scenario-Gold-Sheet.md) | Dockerize a backend/web app correctly |
| 18 | [04-Scenario-Practice/18-Docker-Container-Wont-Start-Debugging-Scenario-Gold-Sheet.md](04-Scenario-Practice/18-Docker-Container-Wont-Start-Debugging-Scenario-Gold-Sheet.md) | command/entrypoint/env/permission/startup debugging |
| 19 | [04-Scenario-Practice/19-Docker-Networking-Port-DNS-Debugging-Scenario-MAANG-Sheet.md](04-Scenario-Practice/19-Docker-Networking-Port-DNS-Debugging-Scenario-MAANG-Sheet.md) | host/container networking, DNS, ports, bridge networks |
| 20 | [04-Scenario-Practice/20-Docker-Volume-Permission-Data-Loss-Scenario-Gold-Sheet.md](04-Scenario-Practice/20-Docker-Volume-Permission-Data-Loss-Scenario-Gold-Sheet.md) | volumes, bind mounts, ownership, data safety |
| 21 | [04-Scenario-Practice/21-Docker-Build-Cache-Image-Size-Scenario-Gold-Sheet.md](04-Scenario-Practice/21-Docker-Build-Cache-Image-Size-Scenario-Gold-Sheet.md) | slow builds, bloated images, cache invalidation |
| 22 | [04-Scenario-Practice/22-Docker-Registry-CI-CD-Deployment-Scenario-MAANG-Sheet.md](04-Scenario-Practice/22-Docker-Registry-CI-CD-Deployment-Scenario-MAANG-Sheet.md) | image promotion, scanning, rollbacks, tag strategy |
| 23 | [04-Scenario-Practice/23-Docker-Production-Incident-Container-Debugging-Scenario-MAANG-Sheet.md](04-Scenario-Practice/23-Docker-Production-Incident-Container-Debugging-Scenario-MAANG-Sheet.md) | on-call container debugging and mitigation |

Scenario target:

- You can diagnose realistic Docker issues with a repeatable path.

---

## 7. Special Interview Rounds

| Order | File | What It Builds |
|---:|---|---|
| 24 | [05-Special-Interview-Rounds/24-Docker-Interview-QA-Beginner-To-Pro-MAANG-Sheet.md](05-Special-Interview-Rounds/24-Docker-Interview-QA-Beginner-To-Pro-MAANG-Sheet.md) | Docker Q&A from beginner to MAANG |
| 25 | [05-Special-Interview-Rounds/25-Docker-Commands-Cheat-Sheet-And-Decision-Map.md](05-Special-Interview-Rounds/25-Docker-Commands-Cheat-Sheet-And-Decision-Map.md) | command map by debugging goal |
| 26 | [05-Special-Interview-Rounds/26-Docker-Anti-Patterns-Debugging-Traps-MAANG-Sheet.md](05-Special-Interview-Rounds/26-Docker-Anti-Patterns-Debugging-Traps-MAANG-Sheet.md) | unsafe Docker practices and debugging traps |

Special-round target:

- You can answer Docker interviews and avoid common production mistakes.

---

## 8. Practice Upgrade Path

| Order | File | What It Builds |
|---:|---|---|
| 27 | [06-Practice-Upgrade/27-Docker-Active-Recall-Question-Bank.md](06-Practice-Upgrade/27-Docker-Active-Recall-Question-Bank.md) | recall prompts across beginner to pro topics |
| 28 | [06-Practice-Upgrade/28-Docker-Hands-On-Exercises-And-Command-Drills.md](06-Practice-Upgrade/28-Docker-Hands-On-Exercises-And-Command-Drills.md) | practical Docker drills |
| 29 | [06-Practice-Upgrade/29-Docker-Mini-Projects-Portfolio.md](06-Practice-Upgrade/29-Docker-Mini-Projects-Portfolio.md) | portfolio-ready Docker projects |
| 30 | [06-Practice-Upgrade/30-Docker-Pro-Gap-Fill-Production-Readiness-Checklist.md](06-Practice-Upgrade/30-Docker-Pro-Gap-Fill-Production-Readiness-Checklist.md) | senior readiness checklist and scoring rubric |
| 40 | [06-Practice-Upgrade/40-Docker-Production-Capstone-Secure-Build-Compose-Registry-Runbook.md](06-Practice-Upgrade/40-Docker-Production-Capstone-Secure-Build-Compose-Registry-Runbook.md) | final production capstone tying secure build, Compose, registry, scan, hardening, and runbooks together |

Practice target:

- You can use Docker daily, debug incidents, explain internals, and build production-grade container workflows.

---

## 9. Docker Mastery Lab

Use the lab when you want practice instead of reading-only notes:

- [docker-mastery-lab/README.md](docker-mastery-lab/README.md)
- [docker-mastery-lab/LEARNING_PATH.md](docker-mastery-lab/LEARNING_PATH.md)

Lab target:

- You can build and run sample images.
- You can use Compose for multi-container practice.
- You can debug startup, network, volume, registry, and production-style failures.

---

## 10. Official Source Anchors

Use these Docker documentation areas when you want the latest product details:

- [Docker Compose](https://docs.docker.com/compose/): profiles, env precedence, watch, multiple files, and secrets.
- [Compose profiles](https://docs.docker.com/compose/how-tos/profiles/), [env precedence](https://docs.docker.com/compose/how-tos/environment-variables/envvars-precedence/), and [Compose Watch](https://docs.docker.com/compose/how-tos/file-watch/).
- [Docker Engine daemon](https://docs.docker.com/engine/daemon/): contexts, remote access, live restore, proxy config, logging drivers, and daemon logs.
- [Remote daemon access](https://docs.docker.com/engine/daemon/remote-access/), [live restore](https://docs.docker.com/engine/daemon/live-restore/), and [logging drivers](https://docs.docker.com/engine/logging/configure/).
- [Docker security](https://docs.docker.com/engine/security/): rootless mode, user namespace remapping, seccomp, AppArmor, daemon socket protection, and content trust.
- [Docker Build](https://docs.docker.com/build/): BuildKit, Buildx builders, Bake, cache backends, multi-platform builds, SBOM, and provenance attestations.
- [Buildx builders](https://docs.docker.com/build/builders/), [Bake](https://docs.docker.com/build/bake/), [cache backends](https://docs.docker.com/build/cache/backends/), and [build attestations](https://docs.docker.com/build/metadata/attestations/).
- [Docker Engine networking](https://docs.docker.com/engine/network/): network drivers, port publishing, packet filtering, iptables/nftables, IPv6, and firewalls.
- [Docker Engine storage](https://docs.docker.com/engine/storage/): volumes, bind mounts, tmpfs, storage drivers, overlay2, and containerd image store.
- [Docker Scout](https://docs.docker.com/scout/): vulnerability analysis, recommendations, policy evaluation, and supply-chain visibility.

---

## 11. Interview Answer Pattern

For Docker debugging and interview answers, use this shape:

```text
1. Symptom:
   What exactly is failing: build, run, network, storage, registry, security, or performance?

2. Object:
   Which Docker object is involved: image, container, network, volume, registry tag, Compose service?

3. Evidence:
   Which command proves the state?

4. Runtime layer:
   Dockerfile, entrypoint, environment, filesystem, network, cgroup, Linux permission, or app logic?

5. Cause:
   What changed or mismatched?

6. Mitigation:
   What safe action restores service?

7. Prevention:
   What Dockerfile, CI/CD, health check, policy, or runbook prevents recurrence?
```

---

## 12. Recommended Study Orders

### 2-Week Practical Path

1. Foundation files 1-4.
2. Practical files 5-10.
3. Scenario files 17-23.
4. Cheat sheet, exercises, and interview Q&A.

### 4-Week Pro Path

1. Week 1: Docker mental model, images, containers, Dockerfiles, daily commands.
2. Week 2: volumes, networking, Compose, registries, tagging, debugging.
3. Week 3: security, resource limits, build optimization, observability, CI/CD, orchestration, and appendix #31.
4. Week 4: senior gap-fill #32-#39, production scenarios, runbooks, mini projects, capstone, and interview practice.

### Production Operator Path

1. Learn command map and inspect/log/debug workflow.
2. Practice container startup, network, volume, and resource incidents.
3. Add security and supply-chain checks.
4. Add daemon, network, storage, registry, and platform-boundary checks.
5. Write RCA notes from each scenario.

---

## 13. Readiness Gate

You are Docker interview-ready when you can do all of this without notes:

- Explain Docker client, daemon, image, layer, container, registry, network, and volume.
- Write production-friendly Dockerfiles with `.dockerignore`, cache-aware layers, non-root users, and multi-stage builds.
- Run, inspect, exec into, log, stop, clean up, and debug containers safely.
- Explain Docker networking, ports, container DNS, bridge networks, and localhost traps.
- Explain volumes, bind mounts, persistence, ownership, and data-loss risks.
- Use Compose for multi-service local systems and explain dependencies/health checks.
- Explain registry tags vs digests, immutable promotion, scanning, and rollback.
- Explain Docker security: non-root, capabilities, secrets, image scanning, rootless mode, least privilege.
- Explain advanced hardening: user namespaces, seccomp, AppArmor/SELinux, read-only root filesystems, tmpfs, and Docker socket risk.
- Explain resource limits, health checks, restart policies, cgroups, and container observability.
- Explain PID 1 signal handling, BuildKit secrets/cache, multi-platform images, OCI runtime boundaries, and Docker disk-pressure cleanup safety.
- Explain advanced Compose: profiles, override files, env precedence, config rendering, secrets, watch, and readiness.
- Explain daemon operations: Docker contexts, remote access, TLS/SSH, `daemon.json`, logging drivers, live restore, proxies, and host-level evidence.
- Explain Buildx and Bake: builders, drivers, cache exporters/importers, secret mounts, SSH mounts, multi-platform strategies, SBOM/provenance.
- Explain advanced networking: bridge/host/overlay/macvlan/ipvlan/none drivers, Docker DNS, published port packet path, firewall/NAT, IPv6, MTU, and host gateway.
- Explain advanced storage: overlay2, writable layers, named volumes, bind mounts, tmpfs, ownership, SELinux labels, backups, and safe cleanup.
- Explain platform boundaries: Docker Desktop VM behavior, WSL2, architecture mismatch, registry operations, OCI artifacts, and Docker vs containerd/CRI-O.
- Handle production Docker incidents with evidence, mitigation, and prevention.
