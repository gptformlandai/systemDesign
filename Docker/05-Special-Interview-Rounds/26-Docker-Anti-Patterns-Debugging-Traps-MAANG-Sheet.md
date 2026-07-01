# Docker Anti-Patterns and Debugging Traps - MAANG Sheet

> Track File #26 of 30 - Group 05: Special Interview Rounds
> For: production maturity | Level: senior | Mode: traps and safer alternatives

## 1. Dangerous Anti-Patterns

| Anti-Pattern | Why It Is Bad | Safer Approach |
|---|---|---|
| `latest` in production | mutable and hard to rollback | immutable tag or digest |
| secrets in image | leaks through layers/history | secret manager/runtime secrets |
| running as root by default | larger blast radius | non-root user |
| `--privileged` casually | host-level risk | specific capabilities or device access |
| mutating running containers | changes disappear and are not reproducible | rebuild image or config |
| no `.dockerignore` | slow builds and secret leaks | explicit ignore file |
| storing DB data in writable layer | data loss on recreation | named volume or external datastore |
| `compose down -v` casually | deletes named volumes | know volume lifecycle and backups |

---

## 2. Debugging Traps

- `EXPOSE` documents a port but does not publish it
- container `localhost` is not host `localhost`
- `depends_on` is not readiness
- shell exists in dev image but not slim/distroless image
- bind mount can hide image files
- tag does not prove exact image content
- restarting hides crash-loop evidence

---

## 3. Interview Recovery Phrase

```text
I would first identify the Docker object involved: image, container, network, volume, registry, or daemon. Then I would gather logs and inspect output before changing runtime state.
```

---

## 4. Interview Summary

```text
Senior Docker maturity means avoiding mutable tags, secrets in images, root/privileged containers, manual container mutation, unmanaged volumes, and assumption-driven networking. I prefer immutable images, least privilege, explicit config, observability, and reproducible builds.
```

---

## 5. Revision Notes

- One-line summary: Docker mistakes often come from treating containers as pets or images as mutable.
- Three keywords: immutable, non-root, inspect.
- One trap: thinking `EXPOSE 8080` makes the app reachable from the host.