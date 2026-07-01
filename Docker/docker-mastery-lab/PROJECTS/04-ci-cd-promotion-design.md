# Project 04: CI/CD Image Promotion Design

## Outcome

Design a Docker image delivery pipeline from commit to production.

## Deliverables

- pipeline diagram or ordered stage list
- build stage
- test stage
- scan stage
- push stage
- digest recording step
- environment promotion plan
- rollback plan

## Reference Flow

```text
commit -> build -> test -> scan -> push -> record digest -> staging -> prod -> monitor -> rollback
```

## Acceptance Criteria

- production does not deploy mutable `latest`
- same image artifact is promoted across environments
- registry retention protects rollback images
- scan gate and exception process are defined
- release metadata records exact image identity

## Interview Proof

```text
I can explain why artifact promotion is safer than rebuilding per environment and how digest-based rollback works.
```