# Project 03: Secure Image Baseline

## Outcome

Design a hardened Docker image baseline for a team.

## Deliverables

- minimal base image decision
- non-root runtime user
- multi-stage Dockerfile when useful
- no secrets in image
- vulnerability scan result or documented scanner plan
- digest or immutable tag strategy
- runtime security flags recommendation

## Acceptance Criteria

- image runs without root unless justified
- package manager caches are not shipped unnecessarily
- Dockerfile does not use `ADD` unless justified
- sensitive env vars are documented as runtime-only
- rollback identity is based on digest or immutable tag

## Interview Proof

```text
I can connect Dockerfile design, runtime privilege, image scanning, SBOM/provenance, and deployment identity into one security story.
```