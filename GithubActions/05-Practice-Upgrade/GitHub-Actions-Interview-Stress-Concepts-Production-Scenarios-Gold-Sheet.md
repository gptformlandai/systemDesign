# GitHub Actions Interview Stress Concepts and Production Scenarios Gold Sheet

> Goal: revise the GitHub Actions concepts interviewers press hardest on, especially security boundaries, artifact identity, runner trust, deployment safety, rollback, and production incidents.

---

## 0. Why This Sheet Exists

The main GitHub Actions docs teach the whole platform.

This sheet is the interview pressure sheet.

Use it when an interviewer asks:

```text
Your workflow deploys to production. How do you secure it?
Your runner is compromised. What now?
Your frontend preview leaked a secret. What do you do?
Your prod deploy failed halfway. What do you do?
Your monorepo CI takes 45 minutes. What do you do?
```

The winning answer is rarely just YAML.

The winning answer is:

```text
security boundary
artifact identity
runner trust
deployment safety
rollback
observability
```

---

# Topic 1: GitHub Actions Interview Stress Concepts and Production Scenarios

---

## 1. Intuition

GitHub Actions is not only CI.

It is a production control plane.

It can:

- read code
- build artifacts
- access secrets
- assume cloud roles
- publish packages
- deploy production
- change infrastructure

So interviews focus on one question:

> Can you make automation powerful without making it dangerous?

---

## 2. Definition

- Definition: GitHub Actions interview stress concepts are the CI/CD topics that test production maturity beyond writing a working workflow.
- Category: DevOps / backend / platform interview preparation
- Core idea: prove that you understand trust boundaries, least privilege, artifact flow, runner isolation, and operational recovery.

---

## 3. The One Master Answer

If you get stuck, use this answer skeleton:

```text
I separate validation, build, and deployment.
PR workflows run with minimal permissions and no production secrets.
Artifacts/images are built once, scanned, versioned by SHA or digest, and promoted.
Cloud auth uses OIDC with repo/branch/environment conditions instead of static keys.
Production deploys use environments, approvals, and concurrency.
Runners are selected based on trust and network access.
Rollback redeploys a previous known-good artifact.
Failures are debugged by trigger, job, runner, permission, artifact, and deployment target.
```

---

## 4. Stress Concept 1: `GITHUB_TOKEN` Permissions

### What Interviewer Is Testing

- Do you know workflows get a token automatically?
- Do you set least privilege?
- Do you avoid broad write permissions?

### Strong Answer

I would explicitly set `permissions` at workflow or job level. PR validation usually needs only `contents: read`. Package publishing may need `packages: write`. OIDC cloud auth needs `id-token: write`. I would not use broad `write-all` unless there is a very specific reason.

### Trap

```yaml
permissions: write-all
```

Better:

```yaml
permissions:
  contents: read
```

### Production Check

Ask:

- What exact permission does this job need?
- Can a compromised job push code, create releases, or publish packages?
- Is permission scoped at job level if only one job needs it?

---

## 5. Stress Concept 2: OIDC Over Static Cloud Secrets

### What Interviewer Is Testing

- Do you avoid long-lived cloud credentials?
- Do you understand short-lived trust?
- Do you scope trust policy correctly?

### Strong Answer

I would use OIDC so GitHub can request short-lived credentials from the cloud provider. The cloud trust policy should restrict organization, repository, branch or tag, and production environment. This avoids storing long-lived AWS/Azure/GCP keys in GitHub secrets.

### Required Workflow Permission

```yaml
permissions:
  contents: read
  id-token: write
```

### Production Check

OIDC trust should answer:

- Which repo can assume this role?
- Which branch/tag?
- Which environment?
- What audience?
- What cloud permissions does the role have?

### Trap

Trusting all repos in an organization to assume a production role.

---

## 6. Stress Concept 3: Fork PR Security

### What Interviewer Is Testing

- Do you know fork PR code is untrusted?
- Do you understand secret exposure risk?
- Do you know `pull_request_target` is dangerous if misused?

### Strong Answer

For fork PRs, I would run validation with minimal permissions and no secrets. I would not checkout and run untrusted PR code inside a privileged `pull_request_target` workflow. If privileged action is needed, I would use a maintainer-approved workflow that does not execute untrusted code with secrets.

### Production Check

Ask:

- Can this workflow run user-controlled code?
- Are secrets available?
- Does it use `pull_request_target`?
- Does it comment, label, or deploy based on untrusted input?

### Trap

Using `pull_request_target` to run tests on PR code while secrets are available.

---

## 7. Stress Concept 4: Reusable Workflows vs Composite Actions

### What Interviewer Is Testing

- Do you understand platform standardization?
- Can you choose the right reuse mechanism?

### Strong Answer

Reusable workflows standardize entire jobs or pipelines across repositories using `workflow_call`. Composite actions package repeated steps. If I need environments, permissions, jobs, or deployment orchestration, I use reusable workflows. If I only need a few repeated shell/setup steps, I use a composite action.

### Decision Table

| Need | Use |
|---|---|
| Whole CI pipeline | reusable workflow |
| Deployment with environment approval | reusable workflow |
| Repeated setup steps | composite action |
| GitHub API logic | JavaScript action |
| Packaged runtime/tooling | Docker action |

### Trap

Calling shared workflows from `main` instead of a version tag or SHA.

---

## 8. Stress Concept 5: Self-Hosted Runner Security

### What Interviewer Is Testing

- Do you understand runner trust?
- Do you isolate private network access?
- Do you avoid untrusted code on sensitive runners?

### Strong Answer

Self-hosted runners are part of the production attack surface. I would isolate them with runner groups, restrict which repos can use them, prefer ephemeral runners, clean workspaces, patch runner images, and never run untrusted fork PR code on runners with private network or production access.

### Production Check

Ask:

- What network can this runner reach?
- Which repositories can use it?
- Is it ephemeral?
- Can fork PRs run on it?
- What secrets/cloud roles are reachable?

### Trap

Treating self-hosted runners as merely cheaper compute.

---

## 9. Stress Concept 6: Build Once, Promote Many

### What Interviewer Is Testing

- Do you understand artifact immutability?
- Can you prevent environment drift?

### Strong Answer

I would build the artifact or Docker image once, tag it immutably using SHA or digest, scan it, and promote the same artifact through dev, stage, and production. I would not rebuild separately for each environment because that creates the risk of deploying something different from what was tested.

### Trap

```text
dev build != stage build != prod build
```

Better:

```text
same image digest promoted through environments
```

---

## 10. Stress Concept 7: Artifacts vs Cache

### What Interviewer Is Testing

- Do you know the purpose of each?

### Strong Answer

Cache speeds future workflow runs by reusing dependencies. Artifacts preserve outputs from the current run, such as test reports, coverage, build outputs, Playwright traces, or deployment manifests.

### Trap

Using cache as release artifact storage.

---

## 11. Stress Concept 8: Deployment Environments and Approvals

### What Interviewer Is Testing

- Do you know GitHub environments?
- Do you protect production?

### Strong Answer

I would use GitHub environments for stage and production. Production would have required reviewers, environment-scoped secrets or variables, deployment history, and a concurrency group so only one production deployment runs at a time.

### Production Check

- Is prod deployment manually approved?
- Are prod secrets only available to prod jobs?
- Is deployment serialized?
- Can we view deployment history?

---

## 12. Stress Concept 9: Concurrency

### What Interviewer Is Testing

- Do you cancel stale PR work?
- Do you serialize production?

### Strong Answer

For PRs, I use concurrency with `cancel-in-progress: true` so outdated runs do not waste time. For production deployments, I use an environment-specific concurrency group with `cancel-in-progress: false` so deploys are serialized and not interrupted unsafely.

### Trap

Canceling a production deployment halfway without platform-level safety.

---

## 13. Stress Concept 10: Rollback Strategy

### What Interviewer Is Testing

- Do you design recovery before release?

### Strong Answer

Rollback should redeploy a previous known-good artifact or image digest, not rebuild old code. For Kubernetes, rollback can be Helm rollback, rollout undo, or redeploying an old digest. For frontend, rollback can restore previous static artifact/CDN version. For DB changes, rollback depends on migration compatibility, so I use expand/contract migrations.

### Trap

Thinking app rollback automatically rolls back the database.

---

## 14. Stress Concept 11: Monorepo Affected Builds

### What Interviewer Is Testing

- Do you understand dependency graph impact?
- Do you know path filters are not enough?

### Strong Answer

In a monorepo, I would use affected-build logic that maps changed files to projects and dependents. Simple path filters are useful, but they can miss shared library changes. Tools like Nx, Turborepo, Bazel, Gradle, or custom graph logic can run only impacted services/apps while still protecting shared dependencies.

### Trap

Only building the folder that changed and ignoring shared packages.

---

## 15. Stress Concept 12: Frontend CI/CD Traps

### What Interviewer Is Testing

- Do you understand browser-exposed secrets?
- Do you understand CDN caching?
- Do you understand preview cleanup?

### Strong Answer

Frontend workflows should never embed secrets into browser bundles. Preview deployments should be scoped per PR and cleaned up when the PR closes. Production static assets should use content-hashed filenames with long cache headers, while HTML should be short-cached or invalidated. Source maps should be uploaded to error tracking and not casually exposed.

### Trap

Putting API secrets into `NEXT_PUBLIC_*`, `VITE_*`, or equivalent browser-exposed variables.

---

## 16. Stress Concept 13: IaC and Database Migration Safety

### What Interviewer Is Testing

- Do you treat infra and DB changes as high-risk?

### Strong Answer

For Terraform, I would run plan on PR and apply only after merge/approval with remote state locking and environment concurrency. For databases, I would use expand/contract migrations to keep old and new app versions compatible. Destructive DB changes should be separate, reviewed, and delayed until safe.

### Trap

Dropping a column while old app versions still read it.

---

## 17. Stress Concept 14: Supply-Chain Security

### What Interviewer Is Testing

- Do you know CI/CD can publish compromised artifacts?

### Strong Answer

I would pin critical third-party actions, use trusted reusable workflows, keep token permissions minimal, run dependency/code/container scans, produce SBOM/provenance where required, and use artifact attestations for release artifacts.

### Trap

Using random unpinned third-party actions in production release workflows.

---

## 18. Stress Concept 15: Production Troubleshooting

### What Interviewer Is Testing

- Can you debug systematically?

### Strong Answer

I would isolate the failing layer: trigger, workflow syntax, job graph, runner, dependency/cache, permission/secret, artifact/image, deployment target, or external service. Then I would protect production state, restore the pipeline, and add a prevention mechanism.

### Debug Order

```text
trigger
-> job/step
-> runner
-> dependency/cache
-> permissions/secrets
-> artifact/image
-> deployment target
-> rollback
```

---

# Production Scenario Bank

---

## Scenario 1: Production Deploy Failed Halfway

### Strong Answer

I would stop further promotion, inspect the deployment target state, and decide rollback versus forward fix. For Kubernetes, I would check rollout status, pod events, logs, and health checks. I would not rerun blindly because the system may be partially changed. Rollback should use a previous known-good image digest or Helm/Kubernetes rollback.

### Key Metrics

- deployment status
- health check
- error rate
- latency
- pod readiness
- business metric if available

---

## Scenario 2: OIDC Auth Suddenly Fails

### Strong Answer

I would check whether `id-token: write` is still present, whether the job targets the expected environment, and whether the cloud trust policy still matches repository, branch, and environment claims. I would also check whether a workflow ref or branch changed.

### Common Root Causes

- environment renamed
- branch changed
- missing permission
- cloud trust policy updated
- workflow moved to another repo

---

## Scenario 3: Self-Hosted Runner Compromised

### Strong Answer

I would isolate the runner immediately, revoke runner registration/token, rotate any secrets or cloud credentials it could access, inspect recent workflow runs, rebuild the runner image, and review runner group access. If the runner had private network access, I would treat this as a production security incident.

### Prevention

- ephemeral runners
- runner groups
- no fork PRs
- least-privilege network/cloud access
- patching and monitoring

---

## Scenario 4: PR Workflow From Fork Tries To Exfiltrate Secrets

### Strong Answer

Fork PR workflows should not have secrets. I would verify the workflow uses minimal permissions and does not run untrusted code in a privileged `pull_request_target` context. If any secret was exposed, I would rotate it and audit logs/artifacts.

---

## Scenario 5: Frontend Preview Deployment Leaked Secret

### Strong Answer

I would rotate the leaked secret, remove it from frontend build variables, audit artifacts and preview URLs, and move the secret to a backend/server-side path. Frontend environment variables that enter the browser bundle are public.

### Prevention

- allow only public frontend config in build
- scan built assets for secrets
- restrict preview environments
- clean up previews on PR close

---

## Scenario 6: CDN Serving Old JavaScript After Deployment

### Strong Answer

I would check cache headers, content-hashed filenames, HTML cache policy, CDN invalidation, and service worker cache. The usual strategy is long-cache immutable hashed JS/CSS and short-cache or invalidated HTML.

---

## Scenario 7: Required Check Stuck Forever

### Strong Answer

I would compare branch protection required check names with actual workflow/job names. If a path filter skipped the workflow, the required check may never report. For monorepos, I might use an aggregator check or stable required workflow that always reports.

---

## Scenario 8: CI Takes 45 Minutes

### Strong Answer

I would measure first: queue time, install time, build time, tests, Docker, E2E, and scan time. Then I would add dependency cache, split jobs, cancel stale PR runs, use affected builds, reduce matrix explosion, and move heavy tests to nightly/release gates.

### Trap

Deleting tests without understanding risk.

---

## Scenario 9: Docker Image Built But Kubernetes Deploys Old Image

### Strong Answer

I would check whether the workflow deployed `latest`, whether the manifest tag changed, whether image pull policy caused reuse, and whether the registry push succeeded. I would deploy immutable SHA/digest tags and record the exact digest in deployment output.

---

## Scenario 10: Terraform Apply Failed And State Is Locked

### Strong Answer

I would verify no apply is still running before unlocking. Then I would inspect the partial state, rerun plan, and decide whether to complete, revert, or manually repair. Unlocking blindly can cause state corruption.

---

## Scenario 11: Database Migration Broke Production

### Strong Answer

I would check whether the migration was backward compatible. If old app versions still expect the old schema, rollback may not be enough. I would use forward fix or restore depending on impact. For prevention, I would use expand/contract: add new schema, deploy compatible app, backfill, switch reads/writes, then remove old schema later.

---

## Scenario 12: Action Supply-Chain Incident

### Strong Answer

I would identify workflows using the action, stop affected release paths, inspect runs, rotate credentials if needed, and pin to a known-safe SHA or replace with internal action. Long-term, production workflows should use trusted actions, pinning, minimal permissions, and artifact provenance.

---

## Scenario 13: Build Artifact Missing During Rollback

### Strong Answer

I would avoid rebuilding old code if possible. If the artifact is missing, I would evaluate whether a rebuild from the exact commit is acceptable, but the better design is artifact/image retention by release policy. Rollback should use stored previous artifact or image digest.

---

## Scenario 14: Preview Environments Are Costing Too Much

### Strong Answer

I would add cleanup on PR close, TTL for previews, concurrency per PR, smaller preview infrastructure, and labels/manual triggers for expensive previews. I would also monitor preview count and cost per team.

---

## Scenario 15: Monorepo Shared Package Breaks Two Apps

### Strong Answer

Path filters alone are not enough. I would model dependency graph so changes in shared packages trigger all dependent apps. I would add periodic full CI to catch graph mistakes.

---

## Scenario 16: Release Workflow Needs Third-Party Action

### Strong Answer

I would prefer official/trusted actions or internal wrappers. For production release, I would pin third-party actions by SHA, review source, restrict permissions, and monitor updates. If the action needs broad token access, I would reconsider.

---

## Scenario 17: Prod Deployment Job Has `write-all`

### Strong Answer

I would reduce permissions to only what the deployment needs. If cloud auth uses OIDC, the job may need `contents: read` and `id-token: write`, not broad repository write. Least privilege reduces blast radius if the workflow is compromised.

---

## Scenario 18: Runner Queue Time Is Too High

### Strong Answer

I would check runner utilization, label matching, runner group access, repo concurrency, and workflow spikes. Fixes include autoscaling runner pools, ARC, larger runners, right-sized jobs, and moving heavy scheduled work away from peak PR hours.

---

## Scenario 19: Production Deployment Accidentally Runs From Feature Branch

### Strong Answer

I would use environment protection, branch restrictions, job-level `if` conditions, and OIDC trust policy scoped to `main` or release tags. Production cloud role should not be assumable from arbitrary branches.

---

## Scenario 20: Emergency Hotfix While CI Is Broken

### Strong Answer

I would not bypass all controls casually. I would identify the broken CI layer, run the minimum critical validation through an emergency workflow or manual approved process, deploy the smallest hotfix, and document the exception. Then I would repair CI immediately and add a regression check.

---

## Final Interview Checklist

Before ending any GitHub Actions design answer, mention:

- trigger and trust boundary
- token permissions
- secrets/OIDC
- runner type and trust level
- artifact/image identity
- cache/artifact strategy
- deployment environment and approval
- production concurrency
- rollback path
- observability/troubleshooting
- supply-chain controls

---

## Thirty-Second Closing Answer

> I treat GitHub Actions as production infrastructure. PR workflows run with minimal permissions and no production secrets. Build workflows produce immutable artifacts or images. Deployment workflows use OIDC, environments, approvals, and concurrency. Runners are isolated by trust level. Production rollback uses previous known-good artifacts, not rebuilds. For scale, I standardize with reusable workflows and monitor slow or flaky pipelines.

---

## Revision Notes

- One-line summary: GitHub Actions interview depth is security boundary plus artifact identity plus runner trust plus rollback.
- Three keywords: OIDC, runner trust, rollback
- One interview trap: a workflow that works is not necessarily safe.
- One memory trick: trust the trigger, limit the token, protect the runner, freeze the artifact, verify the deploy.

---

## Official Source Notes

- Secure use reference: <https://docs.github.com/en/actions/reference/security/secure-use>
- OpenID Connect: <https://docs.github.com/en/actions/concepts/security/openid-connect>
- Workflow syntax: <https://docs.github.com/en/actions/reference/workflows-and-actions/workflow-syntax>
- Self-hosted runner access: <https://docs.github.com/en/actions/how-tos/manage-runners/self-hosted-runners/manage-access>

