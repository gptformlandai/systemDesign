# GitHub Actions Release Engineering and Progressive Delivery Gold Sheet

> Goal: master release workflows: SemVer, tags, changelogs, GitHub Releases, hotfixes, artifact promotion, canary, blue-green, feature flags, and rollback.

---

## 0. How To Read This

Beginner focus:

- version
- tag
- release
- changelog
- rollback

Intermediate focus:

- SemVer
- release branches
- hotfix
- artifact promotion
- GitHub Releases

Senior focus:

- progressive delivery
- canary analysis
- feature flags
- release trains
- compatibility windows
- release governance

---

# Topic 1: Release Engineering and Progressive Delivery

---

## 1. Intuition

Release engineering answers:

```text
What are we shipping?
Who approved it?
Which artifact is it?
Where is it deployed?
How do we stop or roll back?
```

Beginner explanation:

A release workflow turns a tested commit into a versioned release artifact and deploys it through environments safely.

---

## 2. Definition

- Definition: Release engineering is the discipline of versioning, packaging, approving, deploying, observing, and rolling back software changes.
- Category: CI/CD and production delivery
- Core idea: make software releases predictable, traceable, and recoverable.

---

## 3. Why It Exists

Without release engineering:

- no one knows what is in production
- rollback is unclear
- changelogs are manual
- hotfixes are chaotic
- tags are inconsistent
- artifacts differ by environment
- releases cannot be audited

---

## 4. Reality

Release workflows include:

- semantic versioning
- release tags
- changelog generation
- GitHub Releases
- Docker image promotion
- release branches
- hotfix workflows
- rollback workflows
- canary/blue-green deployment
- feature flag rollout

---

## 5. How It Works

### Part A: Release Flow

```text
main is green
-> create release version/tag
-> build immutable artifact/image
-> generate changelog
-> scan/sign/attest
-> deploy to stage
-> approval
-> deploy progressively to prod
-> monitor
-> rollback or complete
```

### Part B: Semantic Versioning

```text
MAJOR.MINOR.PATCH
```

- MAJOR: breaking change
- MINOR: backward-compatible feature
- PATCH: backward-compatible fix

For internal services, SemVer may be less visible, but versioning still matters for traceability.

### Part C: Git Tags

Example:

```yaml
on:
  push:
    tags:
      - "v*.*.*"
```

Tag releases are good for:

- libraries
- deployable versions
- audit
- changelog boundaries

### Part D: GitHub Releases

GitHub Releases can store:

- release notes
- artifacts
- checksums
- links to images
- changelog

### Part E: Changelog

Good changelog explains:

- features
- bug fixes
- breaking changes
- migration steps
- known risks

Source options:

- conventional commits
- PR labels
- release notes automation
- manually curated release notes for major releases

### Part F: Hotfix Flow

```text
production incident
-> branch from production tag or main depending strategy
-> minimal fix
-> tests
-> release patch version
-> deploy
-> merge back to main
```

Hotfix rule:

> Keep hotfixes small and traceable.

### Part G: Feature Flags

Feature flags separate deploy from release.

Use for:

- gradual rollout
- kill switch
- canary enablement
- A/B tests
- risky UI/backend behavior

Do not use flags as permanent complexity. Remove old flags.

### Part H: Canary

Canary deploy:

```text
deploy to 1% traffic
monitor errors/latency/business metrics
increase to 10%
then 50%
then 100%
```

Rollback if:

- error rate increases
- latency spikes
- business metric drops
- logs show serious exceptions

### Part I: Blue-Green

Blue-green deploy:

- blue is current prod
- green is new version
- switch traffic after validation
- rollback by switching traffic back

Best when:

- fast rollback matters
- infra can support duplicate environment

### Part J: Release Train

Release train:

- releases happen at fixed intervals
- changes that miss train wait for next train

Useful for:

- large organizations
- coordinated dependencies
- compliance-heavy release approvals

### Part K: Rollback Workflow

Rollback should know:

- previous artifact/image digest
- previous config
- previous DB compatibility state
- who can approve rollback
- how to verify rollback success

Rollback trigger:

```yaml
on:
  workflow_dispatch:
    inputs:
      image:
        description: Image tag/digest to roll back to
        required: true
```

---

## 6. What Problem It Solves

- Primary problem solved: predictable and recoverable software releases
- Secondary benefits: auditability, traceability, changelog clarity, safer rollout
- Systems impact: reduces production release risk

---

## 7. When To Rely On It

Use release workflows when:

- production deploys matter
- rollback matters
- multiple environments exist
- customers need release notes
- compliance/audit is required
- many services deploy frequently

---

## 8. When Not To Overcomplicate

Avoid heavyweight release process for:

- tiny internal tools
- non-production experiments
- disposable prototypes

But still keep:

- version traceability
- rollback ability
- basic approval if production-facing

---

## 9. Pros and Cons

| Practice | Pros | Cons |
|---|---|
| SemVer | clear version meaning | needs discipline |
| Release tags | traceable | tag mistakes need correction process |
| Canary | limits blast radius | requires metrics/routing |
| Blue-green | fast rollback | duplicate capacity |
| Feature flags | decouple deploy/release | flag debt |
| Release trains | predictable | less flexible |

---

## 10. Trade-offs and Common Mistakes

### Trade-offs

- Faster deploy:
  More velocity, but needs stronger monitoring.
- Manual approval:
  More control, less speed.
- Canary:
  Safer rollout, more platform complexity.
- Feature flags:
  Safer releases, more code paths.

### Common Mistakes

- Mistake: "Rollback only app, ignore DB."
  Why it is wrong: DB migrations may not be backward compatible.
  Better approach: expand/contract migrations.

- Mistake: "Release notes are only for users."
  Why it is wrong: operators need risk and migration notes.
  Better approach: include operational impact.

- Mistake: "Canary without metrics."
  Why it is wrong: no signal to decide promotion.
  Better approach: define metrics before rollout.

- Mistake: "Mutable production tag."
  Why it is wrong: audit and rollback break.
  Better approach: immutable version/SHA/digest.

---

## 11. Key Numbers

Useful targets:

- production rollout should have clear monitoring window
- rollback should be practiced, not theoretical
- canary stages should match traffic/risk
- old artifacts should be retained long enough for rollback/compliance
- stale feature flags should be reviewed regularly

---

## 12. Failure Modes

### Bad Release Tag

Fix:

- do not silently rewrite production history
- create corrected release
- document incident

### Canary Fails

Fix:

- stop rollout
- rollback canary
- inspect metrics/logs
- keep artifact for debugging

### Hotfix Misses Main

Fix:

- merge hotfix back to main
- add regression test
- automate hotfix checklist

### Rollback Fails

Causes:

- DB incompatible
- artifact missing
- config changed

Fix:

- retain artifacts
- use expand/contract DB
- store config versions

---

## 13. Scenario

- Product / system: payment service release
- Why this concept fits: release must be traceable and rollback-safe
- What would go wrong without it: duplicate builds, unclear production version, and slow incident recovery

---

## 14. Code Sample

Tag-based release:

```yaml
name: Release

on:
  push:
    tags:
      - "v*.*.*"

permissions:
  contents: write
  packages: write

jobs:
  release:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - run: echo "Build and test release artifact"
      - run: echo "Build and push Docker image tagged ${{ github.ref_name }}"
      - uses: actions/upload-artifact@v4
        with:
          name: release-notes
          path: CHANGELOG.md
```

---

## 15. Mini Program / Simulation

Canary decision:

```python
def can_promote(error_rate, latency_p95_ms):
    return error_rate < 0.01 and latency_p95_ms < 300

print(can_promote(0.005, 220))
print(can_promote(0.02, 220))
```

---

## 16. Practical Question

> How would you design release and rollback workflows for a production microservice?

---

## 17. Strong Answer

I would build and scan an immutable artifact or image, tag it with a version and commit SHA, and create release notes from PRs or conventional commits. The same artifact would be promoted through stage and production.

Production rollout would use an approval gate and either rolling, canary, or blue-green deployment depending on risk and platform support. Rollback would redeploy a previous known-good artifact or switch traffic back. For database changes, I would use expand/contract so app rollback remains possible.

I would monitor technical and business metrics during rollout and stop promotion automatically or manually if signals degrade.

---

## 18. Revision Notes

- One-line summary: Release engineering makes shipping versioned, traceable, and reversible.
- Three keywords: version, promote, rollback
- One interview trap: canary without metrics is theater.
- One memory trick: release is artifact plus notes plus rollout plus recovery.

---

## 19. Official Source Notes

- GitHub Releases: <https://docs.github.com/en/repositories/releasing-projects-on-github/about-releases>
- Workflow syntax: <https://docs.github.com/en/actions/reference/workflows-and-actions/workflow-syntax>
- Deployment environments: <https://docs.github.com/en/actions/deployment>

