# Docker Registry, Tagging, Push/Pull, and Versioning - Gold Sheet

> Track File #10 of 40 - Group 02: Intermediate Practical
> For: image distribution | Level: intermediate | Mode: registry and versioning

## 1. Core Idea

Registries store and distribute images. Tags are human-friendly pointers; digests identify exact image content.

```text
image build -> tag -> push to registry -> deploy by tag or digest
```

---

## 2. Tags vs Digests

| Identifier | Meaning | Risk |
|---|---|---|
| tag | mutable name like `app:1.2.3` or `app:latest` | can move to new content |
| digest | immutable content hash like `sha256:...` | harder for humans, best for exact deploys |

---

## 3. Commands

```bash
docker tag app:local registry.example.com/team/app:1.2.3
docker push registry.example.com/team/app:1.2.3
docker pull registry.example.com/team/app:1.2.3
docker image inspect registry.example.com/team/app:1.2.3
docker login registry.example.com
```

---

## 4. Versioning Rules

- use semantic or build-number tags for releases
- avoid deploying mutable `latest` in production
- record image digest in deployment metadata
- promote the same image between environments instead of rebuilding per environment
- keep rollback tags/digests available

---

## 5. Failure Modes

- registry auth expired
- tag overwritten unexpectedly
- image architecture mismatch
- deployment pulled stale cached tag
- image deleted by retention policy
- production rebuild differs from staging rebuild

---

## 6. Interview Summary

```text
Docker registries distribute images. I use tags for human release naming, digests for exact reproducibility, immutable promotion through environments, registry authentication, scanning, retention policies, and rollback-ready image metadata.
```

---

## 7. Revision Notes

- One-line summary: Tags are names; digests are content identity.
- Three keywords: registry, tag, digest.
- One trap: relying on `latest` for production deployments.