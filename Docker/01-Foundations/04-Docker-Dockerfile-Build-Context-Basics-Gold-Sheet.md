# Dockerfile and Build Context Basics - Gold Sheet

> Track File #4 of 40 - Group 01: Foundations
> For: first image builds | Level: beginner | Mode: Dockerfile basics

## 1. Core Idea

A Dockerfile is a recipe. The build context is the set of files Docker can send to the builder.

```text
Dockerfile + build context + .dockerignore -> docker build -> image
```

---

## 2. Common Instructions

| Instruction | Purpose |
|---|---|
| `FROM` | base image |
| `WORKDIR` | working directory |
| `COPY` | copy files from build context |
| `RUN` | execute build-time command |
| `ENV` | set environment variable |
| `EXPOSE` | document intended port |
| `USER` | set runtime user |
| `CMD` | default runtime command |
| `ENTRYPOINT` | fixed executable entrypoint |

---

## 3. Minimal Example

```Dockerfile
FROM nginx:alpine
COPY ./site /usr/share/nginx/html
EXPOSE 80
```

Build and run:

```bash
docker build -t demo-site:local .
docker run --rm -p 8080:80 demo-site:local
```

---

## 4. `.dockerignore`

Use `.dockerignore` to avoid sending secrets, dependencies, build outputs, and large files into the build context.

Common entries:

```text
.git
node_modules
target
build
.env
*.pem
```

---

## 5. Failure Modes

- file not found because it is outside build context
- huge build context slows builds
- secret accidentally copied into image
- `CMD` exits immediately, so container stops
- missing `WORKDIR` causes relative-path issues

---

## 6. Interview Summary

```text
A Dockerfile describes how to build an image, and the build context controls which files are available to COPY. I use .dockerignore to keep builds fast and avoid leaking secrets, structure layers for cache efficiency, and define runtime user, command, and ports intentionally.
```

---

## 7. Revision Notes

- One-line summary: Docker build sees only the build context, not your whole machine.
- Three keywords: Dockerfile, context, ignore.
- One trap: copying `.env`, keys, or huge dependency folders into the image.