# Docker Supply Chain: SBOM, Scout, Provenance, Signing - MAANG Sheet

> Track File #35 of 40 - Group 03: Senior Production
> For: senior CI/CD, platform, and security interviews | Level: senior | Mode: image trust

## 1. Intuition

A container image is not just a zip file. It is a deployable artifact with code, OS packages, dependencies, metadata, build history, and risk.

Supply-chain maturity answers:

```text
What is inside this image, who built it, from what source, with what dependencies, and why do we trust it in production?
```

---

## 2. Definition

- Definition: Docker supply-chain security is the process of proving image contents, origin, integrity, vulnerability state, and deployment identity.
- Category: CI/CD security and artifact governance.
- Core idea: production should run the exact scanned and approved image digest that CI produced.

---

## 3. Why It Exists

Tags are mutable, dependencies age, base images gain CVEs, and pipelines can accidentally rebuild different artifacts per environment.

Without supply-chain controls:

- `latest` may point to an unknown image
- staging and prod may run different builds
- vulnerability fixes are not prioritized
- incident response cannot identify affected services
- attackers can tamper with images or build steps

---

## 4. Artifact Identity

| Identifier | Meaning | Trust Level |
|---|---|---|
| tag | human-friendly mutable pointer | weak unless immutable policy exists |
| digest | content-addressed image identity | strong |
| SBOM | dependency inventory | visibility |
| provenance | where/how image was built | traceability |
| signature | cryptographic approval by key/identity | integrity and policy |
| registry metadata | labels, annotations, attestations | governance |

Senior rule:

```text
Humans discuss tags. Production deploys digests.
```

---

## 5. Secure Build-To-Deploy Flow

1. Developer opens PR.
2. CI builds image once from commit SHA.
3. Build uses locked dependencies and pinned base image policy.
4. Build generates SBOM.
5. Build emits provenance/attestation.
6. Image is scanned for vulnerabilities and policy violations.
7. Image is pushed to registry with commit tag and digest.
8. Promotion moves the same digest across environments.
9. Deployment manifest records digest, SBOM, scan result, and source commit.
10. Runtime inventory maps service -> digest -> SBOM -> vulnerabilities.

---

## 6. Commands

```bash
docker buildx build \
  --platform linux/amd64,linux/arm64 \
  --tag registry.example.com/payments/api:git-${GIT_SHA} \
  --provenance=true \
  --sbom=true \
  --push .

docker buildx imagetools inspect registry.example.com/payments/api:git-${GIT_SHA}
docker scout cves registry.example.com/payments/api:git-${GIT_SHA}
docker scout recommendations registry.example.com/payments/api:git-${GIT_SHA}
```

Digest deployment example:

```text
registry.example.com/payments/api@sha256:6f1...
```

---

## 7. SBOM Mental Model

An SBOM is an inventory, not a security guarantee.

It helps answer:

- which packages are inside?
- which versions are affected by a CVE?
- which services must be patched first?
- did this image include unexpected dependency families?

SBOM quality depends on:

- package manager metadata
- language ecosystem metadata
- build process transparency
- whether intermediate artifacts are copied into runtime

---

## 8. Vulnerability Policy

Do not gate only on raw CVE count.

Better policy:

- severity and exploitability
- package reachability where tooling supports it
- fixed version availability
- base image update availability
- internet-facing vs internal workload
- compensating controls
- exception owner and expiry date

Example policy statement:

```text
Block critical exploitable CVEs with a fix available. Allow documented exceptions for non-runtime packages with owner, expiry, and follow-up issue.
```

---

## 9. Signing and Attestation

| Mechanism | What It Proves |
|---|---|
| digest | artifact content identity |
| signature | an identity approved this digest |
| provenance attestation | build source, builder, inputs, and process metadata |
| SBOM attestation | package inventory attached to artifact |
| registry immutability | tag cannot be silently overwritten |

Senior boundary:

```text
Signing does not mean the image has no vulnerabilities. It means you can verify who approved exactly that artifact.
```

---

## 10. Registry Promotion Pattern

Avoid rebuilding per environment.

Bad:

```text
build dev image -> rebuild staging image -> rebuild prod image
```

Good:

```text
build once -> scan -> push digest -> promote same digest -> deploy same digest
```

Tags can help humans:

```text
api:git-a1b2c3
api:staging-2026-07-03
api:prod-2026-07-03
```

But the deployment record should include digest.

---

## 11. Failure Modes

| Symptom | Supply-Chain Cause | Fix |
|---|---|---|
| prod differs from staging | rebuilt image | promote digest |
| scan passes then prod fails policy | tag changed after scan | enforce immutable tags or deploy digest |
| CVE flood | huge base or build tools in runtime image | multi-stage and minimal base |
| cannot answer incident scope | no runtime digest inventory | record service -> digest mapping |
| secret leaked | copied `.env` or build arg in image layer | `.dockerignore`, BuildKit secrets, history review |
| signature verified but app unsafe | trust confused with vulnerability status | combine signing, scanning, and policy |

---

## 12. Scenario

- Product / system: regulated healthcare API platform.
- Why supply-chain controls fit: teams must prove what code and dependencies are running.
- What would go wrong without it: emergency CVE response cannot find affected services quickly and mutable tags can hide drift.

---

## 13. Practical Question

> How would you design a Docker image promotion pipeline for production?

---

## 14. Strong Answer

I would build the image once per commit, generate SBOM and provenance, scan it, sign or attest the digest according to platform policy, and push it to a registry with immutable tags. Staging and production would promote the same digest, not rebuild. Deployment metadata would record commit SHA, image digest, scan result, SBOM location, and rollback digest. Policy would block high-risk vulnerabilities with available fixes and require time-boxed exceptions for accepted risk.

---

## 15. Revision Notes

- One-line summary: Docker supply-chain maturity proves what is inside an image and why that exact digest is allowed to run.
- Three keywords: digest, SBOM, provenance.
- One interview trap: trusting mutable tags as production identity.
- One memory trick: "tag for humans, digest for machines, SBOM for responders."
