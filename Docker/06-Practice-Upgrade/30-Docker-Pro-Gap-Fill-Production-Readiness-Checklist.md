# Docker Pro Gap-Fill Production Readiness Checklist

> Track File #30 of 40 - Group 06: Practice Upgrade
> For: final review | Level: pro | Mode: readiness checklist

## 1. Image Readiness

- Dockerfile is cache-aware
- `.dockerignore` exists
- multi-stage build used when useful
- runtime image excludes build tools
- dependency versions are pinned or locked
- image is scanned
- digest is recorded
- no secrets are present in layers/history

## 2. Runtime Readiness

- container runs as non-root where possible
- ports are documented and published intentionally
- env vars are documented
- health check exists and is meaningful
- logs go to stdout/stderr
- resource limits are set where appropriate
- restart policy is intentional
- root filesystem is read-only where practical
- tmpfs or explicit volumes are used for required writable paths
- Linux capabilities are minimized
- Docker socket is not mounted unless the workload is fully trusted and isolated

## 3. Storage Readiness

- persistent paths use named volumes or external storage
- bind mounts are used only where appropriate
- UID/GID behavior is understood
- backup/restore is documented for stateful data
- destructive cleanup commands are documented clearly
- tmpfs is used for temporary sensitive or scratch data where useful
- overlay/writable layer growth is monitored
- SELinux/AppArmor/user namespace effects are considered on hardened hosts
- Desktop file-sharing limitations are documented for macOS/Windows developers

## 4. Network Readiness

- service-to-service names are documented
- host-to-container ports are clear
- app binds to correct address
- network membership is understood
- TLS/proxy boundary is documented if applicable
- bridge/host/overlay/macvlan/ipvlan/none driver choice is justified
- internal services are not accidentally published to host
- host gateway and Docker Desktop host access behavior are documented
- firewall/NAT/MTU/proxy troubleshooting steps exist

## 5. Security Readiness

- no `--privileged` unless justified
- capabilities are minimized
- Docker socket is not mounted casually
- secrets come from runtime secret mechanism
- registry access is controlled
- base image update process exists
- seccomp/AppArmor/SELinux defaults are not disabled without documented reason
- user namespace remapping or rootless mode is evaluated where it fits
- read-only root filesystem and `no-new-privileges` are tested
- CIS-style host and daemon checks are reviewed for production hosts

## 6. CI/CD Readiness

- image is built once per commit/release
- tests run against image or equivalent artifact
- scan gate exists
- image is pushed to registry
- deployment uses digest or immutable tag
- rollback digest is retained
- release metadata records image identity
- Buildx/BuildKit builder choice is documented
- cache exporter/importer strategy is safe and bounded
- SBOM and provenance are generated or explicitly documented as not available
- image signing or attestation policy is defined for production artifacts
- tag immutability and registry retention policies protect rollback

## 7. Compose Readiness

- `docker compose config` is clean and reviewed
- project name is explicit where collisions matter
- optional tools use profiles
- env precedence is understood and documented
- health checks gate readiness where dependencies matter
- `down -v` risk is documented
- `develop.watch` or bind mounts are chosen intentionally for local development

## 8. Daemon And Host Readiness

- active Docker context is verified before production actions
- daemon access is protected by Unix permissions, SSH, or TLS
- remote unauthenticated TCP daemon access is not used
- log driver and rotation are configured intentionally
- daemon proxy/registry mirror settings are documented
- live restore is evaluated for daemon maintenance behavior
- disk pressure monitoring separates logs, images, build cache, writable layers, and volumes

## 9. Observability Readiness

- logs are accessible
- health status is visible
- resource usage can be checked
- events can be inspected
- runbooks exist for common incidents
- daemon logs and Docker events are part of incident evidence
- registry pull/push failures have a runbook
- build cache and disk usage have safe cleanup commands

## 10. Platform Boundary Readiness

- Docker Desktop vs Linux Engine differences are documented for developers
- WSL2/Windows path and filesystem assumptions are tested if relevant
- architecture targets are explicit for amd64/arm64 builds
- production runtime boundary is known: Docker Engine, containerd, CRI-O, ECS, Nomad, or Kubernetes
- registry outage and rate-limit behavior are considered

## 11. Final Self-Test

You are ready for pro-level Docker interviews when you can answer:

```text
How is this image built, cached, secured, identified, attested, scanned, shipped, run, observed, limited, debugged, cleaned up, and rolled back?
```
