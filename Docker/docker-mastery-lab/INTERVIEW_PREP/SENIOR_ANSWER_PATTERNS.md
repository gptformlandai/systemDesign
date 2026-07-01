# Docker Senior Answer Patterns

## Pattern 1: Explain A Docker Concept

```text
Definition -> why it exists -> Docker object involved -> Linux/runtime behavior -> failure mode -> command evidence -> production best practice
```

## Pattern 2: Debug A Docker Issue

```text
scope -> object -> state -> logs -> inspect -> resource/network/storage evidence -> mitigation -> prevention
```

## Pattern 3: Design A Docker Pipeline

```text
build once -> test -> scan -> push -> record digest -> promote -> deploy -> observe -> rollback
```

## Pattern 4: Security Answer

```text
minimal image -> non-root -> no secrets -> least capabilities -> scan/SBOM -> digest/signature -> daemon/registry access control
```

## Pattern 5: Orchestration Boundary

```text
Docker packages and runs containers. Compose defines local multi-service apps. Kubernetes or managed orchestration adds scheduling, desired state, rollouts, service discovery, autoscaling, storage, and cluster operations.
```