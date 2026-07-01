# Docker Security: Rootless, Users, Secrets, Capabilities - MAANG Sheet

> Track File #11 of 30 - Group 03: Senior Production
> For: senior Docker/security interviews | Level: senior | Mode: container security

## 1. Core Idea

Docker security is layered across image, runtime, host, registry, and pipeline.

```text
trusted image + least privilege runtime + protected secrets + scanned supply chain + hardened host
```

Containers share the host kernel, so container isolation is not the same as VM isolation.

---

## 2. Security Layers

| Layer | Controls |
|---|---|
| image | minimal base, pinned versions, scanning, SBOM |
| Dockerfile | non-root `USER`, no secrets, least packages |
| runtime | read-only filesystem, capabilities, seccomp, no privileged mode |
| host | patched kernel, daemon access control, rootless mode where feasible |
| registry | auth, immutability, signing/provenance, retention |
| pipeline | scanning gates, digest promotion, secret hygiene |

---

## 3. Commands And Checks

```bash
docker inspect CONTAINER --format '{{.Config.User}}'
docker inspect CONTAINER --format '{{json .HostConfig.CapAdd}}'
docker inspect CONTAINER --format '{{.HostConfig.Privileged}}'
docker image inspect IMAGE
docker history IMAGE
```

---

## 4. Good Practices

- run as non-root when possible
- avoid `--privileged`
- drop unnecessary capabilities
- do not bake secrets into images
- use Docker secrets or platform secret stores
- scan images for vulnerabilities
- pin base images or record digests
- avoid mounting Docker socket into containers unless absolutely required

---

## 5. Failure Modes

- container breakout blast radius from privileged mode
- secret leaked in image layer or build history
- app runs as root and writes files with bad ownership
- vulnerable base image remains unpatched
- Docker socket mounted into CI container gives host control

---

## 6. Interview Summary

```text
I secure Docker by minimizing images, running as non-root, avoiding privileged mode, dropping capabilities, protecting secrets, scanning images, pinning or recording digests, hardening the host, and controlling access to the Docker daemon and registry.
```

---

## 7. Revision Notes

- One-line summary: Docker security is image, runtime, host, registry, and pipeline security together.
- Three keywords: non-root, capabilities, secrets.
- One trap: mounting `/var/run/docker.sock` into a container without realizing it can control the host daemon.