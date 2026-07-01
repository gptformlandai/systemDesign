# Docker Pro Gap-Fill Production Readiness Checklist

> Track File #30 of 30 - Group 06: Practice Upgrade
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

## 3. Storage Readiness

- persistent paths use named volumes or external storage
- bind mounts are used only where appropriate
- UID/GID behavior is understood
- backup/restore is documented for stateful data
- destructive cleanup commands are documented clearly

## 4. Network Readiness

- service-to-service names are documented
- host-to-container ports are clear
- app binds to correct address
- network membership is understood
- TLS/proxy boundary is documented if applicable

## 5. Security Readiness

- no `--privileged` unless justified
- capabilities are minimized
- Docker socket is not mounted casually
- secrets come from runtime secret mechanism
- registry access is controlled
- base image update process exists

## 6. CI/CD Readiness

- image is built once per commit/release
- tests run against image or equivalent artifact
- scan gate exists
- image is pushed to registry
- deployment uses digest or immutable tag
- rollback digest is retained
- release metadata records image identity

## 7. Observability Readiness

- logs are accessible
- health status is visible
- resource usage can be checked
- events can be inspected
- runbooks exist for common incidents

## 8. Final Self-Test

You are ready for pro-level Docker interviews when you can answer:

```text
How is this image built, secured, identified, shipped, run, observed, limited, debugged, and rolled back?
```