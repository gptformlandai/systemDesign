# GitHub Actions Advanced Security Threat Model Untrusted Code Gold Sheet

> Track: GitHub Actions Interview Track - Senior / MAANG Security Layer

Goal: reason about GitHub Actions as an attack surface: untrusted code, token permissions, `pull_request_target`, cache/artifact poisoning, runner compromise, command injection, dependency actions, OIDC trust, and secure workflow design.

---

## 0. How To Read This

Use this after the security/OIDC sheet and the interview stress sheet.

Security mental model:

```text
event trust -> code trust -> token permissions -> secret access -> runner trust -> artifact/cache trust -> deploy authority
```

Senior interview rule:

```text
Before calling a workflow production-ready, identify what code runs, who can trigger it, what credentials it receives, and what it can mutate.
```

---

# Topic 1: Actions Threat Modeling

## 1. Intuition

A GitHub Actions workflow is executable infrastructure.

It can:

- run arbitrary code
- read repository contents
- publish artifacts
- push commits/tags
- open or modify PRs
- assume cloud roles through OIDC
- deploy production
- access internal networks on self-hosted runners

That means a workflow is not just CI YAML. It is a privileged program triggered by repository events.

---

## 2. Definition

- Definition: GitHub Actions threat modeling is the practice of analyzing workflow triggers, permissions, credentials, dependencies, runners, artifacts, and deployment paths to prevent untrusted code from gaining trusted privileges.
- Category: CI/CD security and supply-chain defense.
- Core idea: trust boundaries in Actions are event-driven, not only user-driven.

---

## 3. Why It Exists

Actions incidents happen when teams assume all workflow runs are equally trusted.

Common failures:

- fork PR code gets access to secrets
- `pull_request_target` checks out untrusted code
- workflow token has broad write permissions
- cache poisoning influences later trusted builds
- artifacts from untrusted jobs are deployed
- self-hosted runner executes attacker-controlled code
- third-party action is compromised
- shell interpolation creates command injection
- OIDC role trust is too broad
- deployment approval protects UI only, not credentials or runner access

---

## 4. Trust Boundaries

| Boundary | Question |
|---|---|
| Event | Who can trigger this workflow? |
| Code | Is the code trusted repository code or untrusted PR code? |
| Token | What can `GITHUB_TOKEN` do? |
| Secrets | Are secrets available in this event/job/environment? |
| Runner | Is the runner isolated from sensitive network/resources? |
| Artifact | Who produced this artifact and can it be trusted? |
| Cache | Can an attacker influence restored cache contents? |
| OIDC | Which repo/ref/environment can assume cloud role? |
| Deploy | Who approves and what exactly is deployed? |

Strong answer:

```text
The most important Actions question is not only what the YAML does. It is whether the trigger, code, token, secrets, runner, and artifact are all in the same trust level.
```

---

## 5. Event Trust Levels

| Event | Typical Trust |
|---|---|
| `push` to protected branch | high if branch protection is strong |
| `pull_request` from same repo | medium; author may be trusted but code is not merged |
| `pull_request` from fork | low; code is untrusted |
| `pull_request_target` | dangerous if misused; runs in target repo context |
| `workflow_dispatch` | depends on who can run it and inputs |
| `workflow_run` | depends on producer workflow and artifacts |
| `schedule` | trusted code from default branch, but supply-chain dependencies still matter |
| `release` / tag events | trust depends on tag protection and release process |

Rule:

```text
Untrusted code should not receive trusted credentials or run on trusted internal runners.
```

---

## 6. `pull_request` vs `pull_request_target`

`pull_request`:

- runs in context of PR merge/ref
- safer for testing PR code
- secrets usually unavailable for fork PRs
- token permissions are restricted for fork PRs

`pull_request_target`:

- runs in context of base repository
- can access secrets and write token depending on settings
- useful for safe metadata actions such as labeling/commenting
- dangerous if it checks out and runs PR code

Unsafe pattern:

```yaml
on: pull_request_target
jobs:
  test:
    steps:
      - uses: actions/checkout@v4
        with:
          ref: ${{ github.event.pull_request.head.sha }}
      - run: npm test
```

Why unsafe:

```text
It runs attacker-controlled PR code with base-repo privileges.
```

Safer pattern:

```text
Use `pull_request` for running PR code. Use `pull_request_target` only for metadata actions that do not checkout or execute untrusted code.
```

---

## 7. Token Permissions

Default habit:

```yaml
permissions:
  contents: read
```

Grant only what a job needs:

```yaml
permissions:
  contents: read
  id-token: write
```

Common permissions:

| Permission | Use |
|---|---|
| `contents: read` | checkout/read repo |
| `contents: write` | push commits/tags/releases |
| `pull-requests: write` | comment/update PRs |
| `issues: write` | labels/comments/issues |
| `packages: write` | publish packages/images |
| `id-token: write` | request OIDC token |
| `security-events: write` | upload code scanning results |

Strong answer:

```text
Permissions should be job-scoped and minimal. A test job does not need deployment or repository write privileges.
```

---

## 8. Secrets And Environments

Secrets can exist at:

- repository level
- environment level
- organization level
- enterprise level

Environment secrets are safer for deployment because they can be tied to:

- required reviewers
- wait timers
- branch/tag restrictions
- deployment records

Rules:

- PR validation should not need production secrets.
- Production secrets should be environment-scoped.
- Secrets should not be echoed, transformed into logs, or passed to untrusted scripts.
- Prefer OIDC to cloud over long-lived cloud keys.

---

## 9. OIDC Trust Policy Pitfalls

OIDC is strong only if cloud trust policy is narrow.

Bad trust idea:

```text
Any workflow in any branch of repo can assume production role.
```

Better constraints:

- organization/repository
- branch or tag pattern
- environment name
- workflow file path if supported by policy design
- audience
- subject claim

Strong answer:

```text
OIDC removes static cloud secrets, but the cloud role trust policy becomes the security boundary. It must restrict which repo, ref, and environment can assume the role.
```

---

## 10. Self-Hosted Runner Threats

Self-hosted runners are powerful because they can reach private networks.

Threats:

- untrusted PR code runs inside private network
- runner workspace leaks between jobs
- persistent runner keeps compromised state
- broad cloud credentials are available on runner
- Docker socket exposes host privileges
- runner labels route jobs too broadly

Safer controls:

- do not run fork PRs on sensitive self-hosted runners
- use runner groups and repo allowlists
- prefer ephemeral runners for risky workloads
- isolate network access by runner type
- patch and monitor runners
- avoid mounting Docker socket unless required
- keep deployment runners separate from test runners

---

## 11. Cache Poisoning

Cache is performance infrastructure, not a trust boundary.

Risks:

- untrusted branch creates cache restored by trusted branch
- dependency cache contains malicious executable
- cache key is too broad
- restore keys fallback to unsafe cache

Safer design:

- include lockfile hash in key
- separate caches by OS/language/toolchain
- avoid restoring untrusted caches into release jobs
- do not deploy from cache contents
- rebuild critical artifacts from trusted source

Rule:

```text
Cache speeds builds. It should not determine what gets deployed.
```

---

## 12. Artifact Poisoning

Artifacts cross job/workflow boundaries.

Risks:

- untrusted test job uploads artifact
- trusted deploy job downloads and deploys it
- artifact name collision
- artifact retention is too long
- artifact is not tied to commit SHA/build provenance

Safer design:

- separate validation artifacts from release artifacts
- build deployable artifact in trusted workflow/ref
- name artifacts with commit SHA/run id
- verify checksums/signatures/attestations where appropriate
- use environments before deployment
- avoid deploying artifacts from fork PR workflows

---

## 13. Command Injection

Unsafe shell interpolation:

```yaml
- run: echo "Deploying ${{ github.event.pull_request.title }}"
```

If untrusted input reaches shell, it can break assumptions.

Safer pattern:

```yaml
- name: Use env var for untrusted input
  env:
    PR_TITLE: ${{ github.event.pull_request.title }}
  run: |
    printf '%s\n' "$PR_TITLE"
```

Rules:

- treat PR titles, branch names, issue comments, labels, and workflow inputs as untrusted
- quote shell variables
- avoid `eval`
- validate manual inputs
- prefer action inputs over shell interpolation when possible

---

## 14. Third-Party Action Risk

Third-party actions are code execution.

Controls:

- pin to commit SHA for high-security workflows
- use trusted vendors/actions
- review action permissions and source
- avoid actions that require broad tokens
- monitor advisories
- centralize approved action catalog when possible

Trade-off:

```text
Pinning to SHA improves supply-chain stability but requires update automation/process.
```

---

## 15. Secure Workflow Patterns

### Pattern 1: Untrusted PR Validation

```yaml
on: pull_request
permissions:
  contents: read
jobs:
  test:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - run: npm ci
      - run: npm test
```

No secrets. No deployment. No self-hosted private runner.

### Pattern 2: Trusted Deployment

```yaml
on:
  push:
    branches: [main]
permissions:
  contents: read
  id-token: write
jobs:
  deploy:
    environment: production
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - run: echo "Build, verify, deploy trusted main"
```

Deployment tied to trusted branch and environment approval.

---

## 16. Security Review Checklist

Before approving a workflow:

1. What event triggers it?
2. Can fork PRs trigger it?
3. Does it run untrusted code?
4. What are job-level permissions?
5. Are secrets available?
6. Does it use `pull_request_target`?
7. Does it run on self-hosted runner?
8. Does it assume cloud role through OIDC?
9. Is OIDC trust policy narrow?
10. Does it restore cache from untrusted sources?
11. Does it deploy artifacts from untrusted jobs?
12. Are third-party actions pinned/trusted?
13. Are workflow inputs validated?
14. Is deployment protected by environment gates?
15. Is there audit visibility for deployment and bypass?

---

## 17. Common Mistakes

| Mistake | Better Approach |
|---|---|
| `pull_request_target` runs PR code | use `pull_request` for code execution |
| workflow-level write permissions everywhere | job-level least privilege |
| production secrets in PR checks | environment-scoped secrets only for deploy jobs |
| untrusted PR on internal self-hosted runner | hosted/isolated runners for untrusted code |
| deploy from untrusted artifacts | build release artifact from trusted ref |
| broad OIDC trust policy | restrict repo/ref/environment/audience |
| cache used as trusted source | rebuild deployable output from source |
| unquoted untrusted input in shell | env vars, quoting, validation |
| third-party actions pinned only to major tag in sensitive workflow | pin to SHA or approved catalog |

---

## 18. Scenario

Prompt:

```text
A public repo uses GitHub Actions. Contributors open fork PRs. The workflow uses `pull_request_target`, checks out PR code, and has `contents: write` plus a cloud deployment secret. What is wrong?
```

Strong answer:

```text
This crosses trust boundaries. `pull_request_target` runs with base repository context, so checking out and executing fork PR code can expose write token permissions and secrets to untrusted code. I would split the workflow: use `pull_request` with read-only permissions and no secrets for tests, and use a separate trusted deployment workflow on protected branch push with environment approval and OIDC. If metadata automation is needed on PRs, keep `pull_request_target` limited to safe label/comment operations without checking out PR code.
```

---

## 19. Revision Notes

- Treat workflow files as privileged code.
- Identify event trust, code trust, token trust, secret trust, runner trust, artifact trust.
- `pull_request_target` is dangerous when it checks out PR code.
- Job-level permissions should be minimal.
- OIDC trust policy must be narrow.
- Self-hosted runners need isolation and repo allowlists.
- Cache and artifacts are not automatically trusted.
- Untrusted input can become shell injection.
- Secure Actions design is about keeping trust boundaries aligned.

---

## 20. Official Source Notes

- GitHub Actions security hardening: https://docs.github.com/en/actions/security-for-github-actions/security-guides/security-hardening-for-github-actions
- GitHub Actions permissions: https://docs.github.com/en/actions/reference/workflows-and-actions/workflow-syntax#permissions
- OIDC in GitHub Actions: https://docs.github.com/en/actions/concepts/security/openid-connect
- Secure use reference: https://docs.github.com/en/actions/security-for-github-actions/security-guides/security-hardening-for-github-actions
