# Docker Build Cache and Image Size Scenario - Gold Sheet

> Track File #21 of 30 - Group 04: Scenario Practice
> For: build optimization interviews | Level: intermediate to senior | Mode: slow builds and bloated images

## 1. Scenario

```text
Docker builds are slow and images are too large.
```

Goal: improve cache behavior, reduce runtime image size, and avoid shipping build tools or secrets.

---

## 2. Debug Flow

```text
build context -> Dockerfile layer order -> dependency install -> multi-stage split -> image history -> scan size/CVEs
```

Commands:

```bash
docker build -t app:local .
docker history app:local
docker image inspect app:local
docker system df
```

---

## 3. Optimization Moves

- add `.dockerignore`
- copy lock files before source files
- use multi-stage builds
- remove package caches in same layer
- use slim/alpine/distroless where appropriate
- avoid copying tests/docs/build cache into runtime
- scan image after build

---

## 4. Common Causes

- context includes `.git`, dependencies, build outputs
- `COPY . .` before dependency install
- runtime image includes compiler/build chain
- package manager cache left behind
- source changes invalidate all layers

---

## 5. Interview Summary

```text
For slow Docker builds or large images, I inspect build context, .dockerignore, layer ordering, cache invalidation, image history, and multi-stage separation. I keep runtime images minimal and reproducible while avoiding secrets and unnecessary build artifacts.
```

---

## 6. Revision Notes

- One-line summary: Build speed and image size are mostly context, cache, and stage design.
- Three keywords: context, cache, multi-stage.
- One trap: copying the whole repo before dependency installation.