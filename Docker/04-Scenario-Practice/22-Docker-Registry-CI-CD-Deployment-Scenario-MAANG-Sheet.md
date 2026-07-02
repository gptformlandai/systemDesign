# Docker Registry, CI/CD, and Deployment Scenario - MAANG Sheet

> Track File #22 of 40 - Group 04: Scenario Practice
> For: platform and deployment interviews | Level: senior | Mode: image delivery

## 1. Scenario

```text
A team wants reliable Docker image delivery from CI to production with rollback.
```

Goal: build once, scan, publish, promote, deploy, monitor, and rollback by exact image identity.

---

## 2. Delivery Flow

```text
commit -> build image -> test -> scan -> push -> record digest -> promote -> deploy -> monitor -> rollback
```

---

## 3. Required Controls

- immutable release tag or digest
- vulnerability and secret scanning
- SBOM/provenance if required
- registry auth and retention policy
- environment promotion of same image
- rollback digest preserved
- deployment metadata records exact image

---

## 4. Failure Modes

- mutable tag changes after testing
- staging and prod rebuild different images
- registry cleanup deletes rollback image
- image architecture mismatch
- pipeline pushes unscanned image
- production pulls stale image due to tag caching

---

## 5. Interview Summary

```text
For Docker delivery, I build once, test and scan the image, push it to a registry, record the digest, promote the same artifact through environments, deploy by digest or immutable tag, monitor rollout, and rollback to a known-good digest.
```

---

## 6. Revision Notes

- One-line summary: Reliable Docker deployment depends on immutable image identity.
- Three keywords: scan, digest, rollback.
- One trap: using mutable `latest` as the production release mechanism.