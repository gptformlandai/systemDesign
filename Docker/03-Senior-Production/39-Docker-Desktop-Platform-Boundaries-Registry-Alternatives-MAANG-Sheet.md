# Docker Desktop, Platform Boundaries, Registry Ops, Alternatives - MAANG Sheet

> Track File #39 of 40 - Group 03: Senior Production
> For: senior platform, cloud, and interview boundary questions | Level: senior | Mode: ecosystem boundaries

## 1. Intuition

Docker is not one identical thing everywhere.

On Linux, Docker Engine talks to the host kernel directly. On macOS and Windows, Docker Desktop usually runs Linux containers through a Linux VM boundary. In Kubernetes, Docker is usually a build/developer tool, not the cluster runtime.

```text
Docker CLI experience can be similar while runtime boundaries are very different.
```

---

## 2. Definition

- Definition: Docker platform boundaries are the differences between Docker Desktop, Docker Engine, containerd image stores, registries, host OS behavior, and non-Docker container tools.
- Category: ecosystem and production architecture.
- Core idea: senior engineers know where Docker ends and the host, VM, registry, orchestrator, or runtime begins.

---

## 3. Why It Exists

Real teams run Docker across:

- developer laptops
- CI runners
- Linux build hosts
- Docker Desktop on macOS/Windows
- private registries
- Kubernetes/ECS/Nomad production platforms
- regulated or air-gapped environments

The same Dockerfile can behave differently because filesystem, architecture, networking, credentials, registry, and runtime layers differ.

---

## 4. Platform Boundary Map

| Platform | Important Boundary |
|---|---|
| Linux Docker Engine | host kernel, systemd, cgroups, iptables/nftables, storage driver |
| Docker Desktop macOS | Linux VM, file sharing, port forwarding, resource settings |
| Docker Desktop Windows | WSL2/Hyper-V boundary, path conversion, Windows vs Linux containers |
| CI runner | ephemeral daemon, cache strategy, socket risk, registry auth |
| Kubernetes | image built by Docker may run under containerd/CRI-O, not Docker Engine |
| ECS/Nomad | orchestrator owns scheduling, secrets, health, and networking |
| air-gapped env | mirrors, offline scanning, private registry promotion |

---

## 5. Docker Desktop Reality

Docker Desktop is a developer product that gives a local Docker experience on non-Linux hosts.

Senior implications:

- Linux containers run in a VM layer.
- Host paths cross a file-sharing boundary.
- `localhost` and host networking can differ from Linux Engine behavior.
- CPU, memory, and disk limits are Desktop settings, not just app settings.
- ARM laptops may build/run different architecture images unless platform is explicit.
- Some enterprise features control registry access, sign-in, image access, settings, or enhanced isolation.

Debug questions:

```text
Am I debugging Docker Engine behavior, Desktop VM behavior, host OS behavior, or app behavior?
```

---

## 6. Windows and WSL2

Key distinctions:

- Linux containers on Windows usually run through WSL2 or Hyper-V.
- Windows containers are different from Linux containers.
- Path mounts can behave differently across shells.
- File watching and case sensitivity can surprise cross-platform teams.
- Corporate endpoint security can interfere with file sharing or network performance.

Practical rule:

```text
Keep Dockerfiles Linux-first for Linux production, and document Windows-specific developer setup separately.
```

---

## 7. Architecture and Device Boundaries

Apple Silicon and ARM servers make architecture explicit.

Commands:

```bash
docker version
docker info
docker image inspect IMAGE --format '{{json .Architecture}}'
docker buildx imagetools inspect IMAGE
docker run --platform linux/amd64 IMAGE
```

Watch-outs:

- emulation can hide performance problems
- native modules may compile for the wrong architecture
- multi-platform images must include all expected manifests
- GPU/device access is platform-specific and runtime-specific

---

## 8. Registry Operations

Registries are production infrastructure, not just upload folders.

| Concern | Senior Control |
|---|---|
| auth | scoped tokens, robot accounts, least privilege |
| immutability | prevent tag overwrite for release tags |
| retention | keep rollback digests, expire old CI tags |
| garbage collection | reclaim unreferenced blobs safely |
| mirrors/cache | reduce latency and rate limits |
| replication | regional availability and disaster recovery |
| air-gapped | import/export, offline scanning, trusted mirrors |
| OCI artifacts | images, attestations, SBOMs, signatures, Helm charts, other artifacts |

Production question:

```text
If registry region A fails, can the platform still pull the last known-good production digest?
```

---

## 9. Docker Alternatives and Boundaries

| Tool | Relationship To Docker |
|---|---|
| containerd | lower-level container runtime used by many platforms |
| runc | OCI runtime that starts containers |
| nerdctl | Docker-like CLI for containerd |
| Podman | daemonless container tooling with Docker-like UX |
| Buildah | image build tooling, often used with Podman ecosystems |
| CRI-O | Kubernetes CRI runtime |
| Kaniko | builds images without Docker daemon, common in Kubernetes CI |
| BuildKit | build engine usable inside and outside Docker workflows |

Interview maturity:

```text
Docker is often the developer/build interface. Production Kubernetes commonly runs images through containerd or CRI-O.
```

---

## 10. When Docker Is The Right Tool

Use Docker directly for:

- local development
- image builds
- CI image packaging
- small single-host tools
- reproducible lab environments
- Compose-based integration tests

Use an orchestrator for:

- production scheduling
- high availability
- service discovery
- autoscaling
- rolling deploys
- workload identity
- cluster-level secrets/configs

---

## 11. Failure Modes

| Symptom | Boundary Cause | Fix |
|---|---|---|
| app slow only on Mac | Desktop bind mount or VM resource limit | use volumes/watch, tune Desktop resources |
| image runs locally but not prod | architecture or runtime mismatch | build multi-platform and test target runtime |
| CI pulls rate-limited | no mirror/cache/auth | configure registry mirror or authenticated pulls |
| tag rollback fails | tag overwritten or garbage-collected | deploy by digest and retention policy |
| Kubernetes issue blamed on Docker | cluster runtime is containerd/CRI-O | inspect pod/runtime events and image metadata |
| host networking differs | Desktop VM boundary | use published ports and platform-specific docs |

---

## 12. Scenario

- Product / system: company-wide developer platform for Java, Node, and Python services.
- Why platform boundaries matter: developers use macOS/Windows while production runs Linux/amd64 and Linux/arm64 images.
- What would go wrong without it: architecture bugs, slow local file sync, broken host networking assumptions, and registry drift.

---

## 13. Practical Question

> Why does a Dockerized app work on a developer Mac but fail in Linux production, and how would you prevent that class of issue?

---

## 14. Strong Answer

I would compare architecture, base image, runtime user, filesystem mounts, env vars, network assumptions, and the actual image digest. On macOS, Docker Desktop adds a Linux VM and file-sharing boundary, so local behavior can hide Linux host differences. Prevention includes building multi-platform images through Buildx, testing in Linux CI, avoiding host-specific bind mount assumptions, publishing ports explicitly, deploying by digest, and documenting Desktop-specific setup separately from production runtime expectations.

---

## 15. Revision Notes

- One-line summary: Senior Docker mastery includes knowing when behavior belongs to Docker, Desktop, the host OS, registry, or orchestrator.
- Three keywords: Desktop, registry, runtime.
- One interview trap: saying Kubernetes "runs Docker" without checking the actual runtime.
- One memory trick: ask "CLI, daemon, VM, registry, orchestrator, or runtime?"
