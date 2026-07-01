# Dockerfile Layers, Cache, Multi-Stage Builds, and Best Practices - Gold Sheet

> Track File #6 of 30 - Group 02: Intermediate Practical
> For: image build quality | Level: intermediate | Mode: Dockerfile optimization

## 1. Core Idea

Every Dockerfile instruction creates or influences layers. Cache efficiency depends on ordering stable layers before frequently changing layers.

```text
stable dependency layers -> changing source code layers -> final runtime image
```

---

## 2. Cache Rules

- Docker reuses a layer when the instruction and relevant inputs are unchanged.
- Once a layer cache is invalidated, later layers rebuild.
- Copy dependency manifests before source code for faster builds.
- Keep `.dockerignore` tight.

---

## 3. Multi-Stage Pattern

```Dockerfile
FROM node:22-alpine AS build
WORKDIR /app
COPY package*.json ./
RUN npm ci
COPY . .
RUN npm run build

FROM nginx:alpine
COPY --from=build /app/dist /usr/share/nginx/html
```

Benefits:

- smaller runtime image
- build tools not shipped to production
- fewer vulnerabilities
- cleaner separation of build and runtime

---

## 4. Best Practices

- pin base image versions where practical
- use small trusted base images
- run as non-root
- combine package installs and cleanup in one layer
- avoid copying secrets
- use build args carefully; they may appear in metadata/history
- add health checks when useful

---

## 5. Failure Modes

- slow builds from poor layer ordering
- huge images from build tools in runtime
- cache invalidated by copying entire source too early
- secrets embedded in image layers
- runtime image missing required libraries

---

## 6. Interview Summary

```text
I optimize Dockerfiles by ordering layers for cache reuse, using .dockerignore, separating build and runtime with multi-stage builds, minimizing image size, avoiding secrets, pinning trusted bases, and running as non-root when possible.
```

---

## 7. Revision Notes

- One-line summary: Good Dockerfiles optimize cache, size, security, and reproducibility.
- Three keywords: layer, cache, multi-stage.
- One trap: putting secrets or credentials into image layers.