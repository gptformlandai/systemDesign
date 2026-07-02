# Docker Buildx, BuildKit, and Bake Advanced - MAANG Sheet

> Track File #36 of 40 - Group 03: Senior Production
> For: senior build-platform and CI interviews | Level: senior | Mode: advanced image builds

## 1. Intuition

Classic Docker build is like compiling on your laptop. BuildKit is a modern build engine. Buildx is the CLI cockpit. Bake is the build plan.

```text
Dockerfile + contexts + cache + secrets + platforms -> BuildKit -> image/registry/attestations
```

---

## 2. Definition

- Definition: BuildKit is Docker's advanced build backend; Buildx is the CLI plugin for managing builders and advanced build features; Bake defines repeatable multi-target builds.
- Category: image build infrastructure.
- Core idea: production builds should be fast, reproducible, cache-efficient, secret-safe, and multi-platform aware.

---

## 3. Why It Exists

Large teams hit build problems quickly:

- slow CI because every job rebuilds from scratch
- secret leakage through build args or copied files
- separate Dockerfiles for similar targets
- inconsistent multi-platform images
- no provenance/SBOM metadata
- poor cache sharing across runners

BuildKit and Buildx solve these by making the build graph smarter and more portable.

---

## 4. Component Map

| Component | Purpose |
|---|---|
| BuildKit | build engine, graph solver, cache manager, secret/SSH mounts |
| buildx | CLI for advanced builders and `docker buildx build` |
| builder instance | execution environment for builds |
| driver | where the builder runs: Docker, container, Kubernetes, remote |
| cache exporter/importer | moves cache between local, registry, GitHub Actions, S3/Azure, etc. |
| Bake | HCL/Compose-based build definition for multiple targets |
| attestations | SBOM/provenance metadata attached to image outputs |

---

## 5. Builder Operations

```bash
docker buildx ls
docker buildx create --name team-builder --driver docker-container --use
docker buildx inspect --bootstrap
docker buildx du
docker buildx prune
docker buildx rm team-builder
```

Driver decision:

| Driver | Fit |
|---|---|
| `docker` | simple local builds, limited advanced isolation |
| `docker-container` | strong default for local/CI BuildKit features |
| `kubernetes` | shared cluster build capacity |
| `remote` | external BuildKit daemon or managed builder |

---

## 6. BuildKit Features

### Secret Mount

```dockerfile
# syntax=docker/dockerfile:1
FROM alpine
RUN --mount=type=secret,id=npm_token \
    cat /run/secrets/npm_token >/dev/null
```

```bash
docker buildx build --secret id=npm_token,src=.npm-token .
```

Secret mounts do not persist into final image layers when used correctly.

### Cache Mount

```dockerfile
# syntax=docker/dockerfile:1
FROM node:22-alpine
WORKDIR /app
COPY package*.json ./
RUN --mount=type=cache,target=/root/.npm npm ci
COPY . .
RUN npm run build
```

Cache mounts speed repeated dependency operations without copying cache into the runtime image.

### SSH Mount

```bash
docker buildx build --ssh default .
```

Use for private Git dependencies when policy allows. Prefer package registries and scoped tokens where possible.

---

## 7. Multi-Platform Build

```bash
docker buildx build \
  --platform linux/amd64,linux/arm64 \
  --tag registry.example.com/app/api:git-${GIT_SHA} \
  --push .
```

Multi-platform build strategies:

| Strategy | Meaning | Trade-off |
|---|---|---|
| QEMU emulation | build foreign architectures on one host | easiest, slower |
| native nodes | builders for each architecture | faster, operationally heavier |
| cross-compilation | language/toolchain builds target arch | fastest when app supports it |

Always test runtime behavior on each target architecture for native dependencies, CPU assumptions, and compiled packages.

---

## 8. Cache Exporters

```bash
docker buildx build \
  --cache-from type=registry,ref=registry.example.com/app/cache:api \
  --cache-to type=registry,ref=registry.example.com/app/cache:api,mode=max \
  --tag registry.example.com/app/api:git-${GIT_SHA} \
  --push .
```

Cache choices:

| Cache | Fit |
|---|---|
| inline | simple image-linked cache |
| registry | portable team/CI cache |
| local | workstation or mounted CI cache |
| GitHub Actions | GitHub CI workflows |
| S3/Azure | cloud object storage cache where supported |

Do not cache secrets or generated artifacts that should be rebuilt from source.

---

## 9. Bake Example

`docker-bake.hcl`:

```hcl
variable "REGISTRY" {
  default = "registry.example.com/payments"
}

variable "GIT_SHA" {
  default = "dev"
}

group "default" {
  targets = ["api", "worker"]
}

target "common" {
  platforms = ["linux/amd64", "linux/arm64"]
  cache-from = ["type=registry,ref=${REGISTRY}/cache:build"]
  cache-to = ["type=registry,ref=${REGISTRY}/cache:build,mode=max"]
  attest = ["type=provenance", "type=sbom"]
}

target "api" {
  inherits = ["common"]
  context = "."
  dockerfile = "Dockerfile"
  target = "api-runtime"
  tags = ["${REGISTRY}/api:git-${GIT_SHA}"]
}

target "worker" {
  inherits = ["common"]
  context = "."
  dockerfile = "Dockerfile"
  target = "worker-runtime"
  tags = ["${REGISTRY}/worker:git-${GIT_SHA}"]
}
```

Commands:

```bash
docker buildx bake --print
docker buildx bake --push
```

---

## 10. Failure Modes

| Symptom | Build Cause | Fix |
|---|---|---|
| secret appears in image history | used `ARG` or copied secret | use BuildKit secret mount |
| arm64 image fails | dependency not available or native module mismatch | test on arm64, use native builder or cross-build |
| cache never hits | copied source before lock files or cache scope mismatch | reorder Dockerfile and inspect cache keys |
| CI disk fills | unbounded BuildKit cache | configure prune/GC and external cache |
| `--load` missing platforms | local Docker image store cannot load multi-platform output in older setups | use `--push` and inspect manifest |
| build differs by machine | unpinned dependencies/base image | lock dependencies and pin/update base policy |

---

## 11. Scenario

- Product / system: monorepo with API, worker, and frontend images built for x86 and ARM.
- Why advanced build tooling fits: shared cache, multi-target config, SBOM/provenance, and multi-platform images reduce CI time and deploy risk.
- What would go wrong without it: slow builds, duplicated config, secret leakage, and architecture-specific production failures.

---

## 12. Practical Question

> Your Docker builds take 25 minutes in CI and need to produce amd64 and arm64 images with SBOM and provenance. How would you improve them?

---

## 13. Strong Answer

I would use BuildKit through Buildx with a `docker-container`, remote, or Kubernetes builder depending on CI scale. I would reorder Dockerfiles around lock files, use cache mounts for package managers, export/import cache through a registry or CI cache backend, and use Bake for repeatable multi-target builds. For multi-platform output I would prefer native builders when speed matters, generate SBOM/provenance, push the manifest list to the registry, scan the digest, and prune build cache with retention rules.

---

## 14. Revision Notes

- One-line summary: Buildx and BuildKit turn Docker builds into reproducible, cached, secret-safe, multi-platform pipelines.
- Three keywords: builder, cache, bake.
- One interview trap: passing secrets with `ARG` and assuming they disappear.
- One memory trick: "builder first, cache second, output third, metadata fourth."
