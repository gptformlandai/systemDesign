# GitHub Actions Runners, Networking, Governance, and Scale Gold Sheet

> Goal: understand runner types, self-hosted runner security, Actions Runner Controller, private networking, org governance, workflow templates, quotas, and platform-scale CI/CD operations.

---

## 0. How To Read This

Beginner focus:

- runner
- GitHub-hosted runner
- self-hosted runner
- labels

Intermediate focus:

- runner groups
- larger runners
- private networking
- ephemeral runners
- cost and quotas

Senior focus:

- Actions Runner Controller
- runner isolation
- multi-tenant governance
- org templates
- compromised runner response
- platform engineering patterns

---

# Topic 1: Runners, Networking, Governance, and Scale

---

## 1. Intuition

A workflow is the plan. A runner is the machine that does the work.

Choosing runners is choosing:

- where code executes
- what network it can reach
- how much compute it has
- what secrets it might see
- how clean it is between jobs

Beginner explanation:

GitHub Actions jobs run on runners. GitHub-hosted runners are managed by GitHub. Self-hosted runners are machines you operate.

---

## 2. Definition

- Definition: A runner is the execution environment for GitHub Actions jobs, while governance defines how workflows, runners, permissions, and templates are controlled at organization scale.
- Category: CI/CD platform operations
- Core idea: execution environment and org controls are as important as YAML logic.

---

## 3. Why It Exists

Runner and governance decisions matter because:

- jobs need compute
- deployments may need private network access
- untrusted code must be isolated
- large repos need cost control
- many teams need standard templates
- self-hosted runners can become attack paths

---

## 4. Reality

Runner choices:

- GitHub-hosted runners
- larger GitHub-hosted runners
- self-hosted runners
- ephemeral runners
- Actions Runner Controller on Kubernetes

Governance controls:

- runner groups
- repository access
- environment protection
- required workflows
- branch protection
- reusable workflow templates
- action allowlists
- audit logs

---

## 5. How It Works

### Part A: GitHub-Hosted Runners

Pros:

- managed by GitHub
- clean environment
- easy setup
- good default for many CI jobs

Cons:

- limited private network access
- fixed images/sizes unless larger runners
- usage cost/minutes
- less control

Use for:

- PR CI
- open source validation
- standard tests/builds

### Part B: Self-Hosted Runners

Pros:

- private network access
- custom tools
- larger machines
- specialized hardware
- cost/control flexibility

Cons:

- security responsibility
- cleanup responsibility
- patching
- scaling
- compromised runner risk

Use for:

- private deployment networks
- internal tools
- specialized builds
- large monorepo compute

### Part C: Runner Labels

Jobs select runners by labels:

```yaml
runs-on: [self-hosted, linux, x64, prod-network]
```

Labels should describe capabilities:

- OS
- architecture
- network access
- toolchain
- environment class

Do not overload one runner label for everything.

### Part D: Runner Groups

Runner groups restrict which repos can use which runners.

Use groups for:

- production deployment runners
- high-trust internal network runners
- GPU/special hardware
- team-specific runners
- regulated workloads

Principle:

> Not every repository should be able to run jobs on every self-hosted runner.

### Part E: Ephemeral Runners

Ephemeral runners handle one job and then disappear.

Benefits:

- cleaner isolation
- less persistent compromise risk
- predictable cleanup

Trade-off:

- startup cost
- orchestration complexity

### Part F: Actions Runner Controller

Actions Runner Controller runs self-hosted runners on Kubernetes.

Useful for:

- autoscaling runners
- ephemeral runner pods
- large org CI capacity
- private cluster access

Senior concerns:

- pod isolation
- secret handling
- namespace isolation
- cluster permissions
- image patching
- runner scale limits

### Part G: Private Networking

Use self-hosted/private runners when workflows need:

- private Kubernetes API
- internal artifact repositories
- private databases for migration
- internal deployment targets
- private package mirrors

Security rule:

Do not run untrusted PR code on runners that can reach sensitive internal networks.

### Part H: Governance At Scale

For many repos, standardize:

- required checks
- reusable workflows
- deployment environments
- permission defaults
- action allowlists
- runner groups
- branch protection
- secret scopes

### Part I: Workflow Templates

Use org templates for:

- Java CI
- frontend CI
- Docker build
- Terraform plan
- Kubernetes deploy
- security scan

Templates reduce copy/paste drift.

### Part J: Cost and Quotas

Control cost with:

- path filters
- affected builds
- concurrency cancel
- caching
- right-sized runners
- scheduled heavy jobs
- max parallel limits

Track:

- queue time
- runner utilization
- workflow duration
- failure rate
- cost per repo/team

---

## 6. What Problem It Solves

- Primary problem solved: safe and scalable execution of CI/CD jobs
- Secondary benefits: cost control, private access, standardization, platform governance
- Systems impact: lets CI/CD scale across teams without becoming unsafe or chaotic

---

## 7. When To Rely On It

Use GitHub-hosted runners for:

- standard CI
- untrusted PRs
- simple builds

Use self-hosted/private runners for:

- private networking
- custom hardware/tools
- large compute
- deployment into private environments

Use ARC when:

- many self-hosted runners are needed
- Kubernetes-based autoscaling is desired
- ephemeral isolation matters

---

## 8. When Not To Use Self-Hosted Runners

Avoid self-hosted runners when:

- GitHub-hosted runners are sufficient
- team cannot patch/harden machines
- untrusted fork PRs need to run
- cleanup cannot be guaranteed
- private network access creates too much risk

---

## 9. Pros and Cons

| Runner Type | Pros | Cons |
|---|---|---|
| GitHub-hosted | managed, clean, simple | limited control/network |
| Larger hosted | more compute, managed | cost |
| Self-hosted | control/private access | security/maintenance |
| Ephemeral | better isolation | orchestration complexity |
| ARC | autoscaling on Kubernetes | cluster ops complexity |

---

## 10. Trade-offs and Common Mistakes

### Trade-offs

- More control:
  More responsibility.
- Private network:
  More deployment power, larger blast radius.
- Ephemeral runners:
  Better isolation, slower startup.
- Standard templates:
  Consistency, but less per-team freedom.

### Common Mistakes

- Mistake: "Run fork PRs on internal self-hosted runners."
  Why it is wrong: untrusted code can reach private network.
  Better approach: use GitHub-hosted or isolated untrusted runners.

- Mistake: "One runner for all repos."
  Why it is wrong: no isolation or governance.
  Better approach: runner groups and labels.

- Mistake: "Persistent runner without cleanup."
  Why it is wrong: state leaks across jobs.
  Better approach: ephemeral runners or strict cleanup.

- Mistake: "Let every team invent workflows."
  Why it is wrong: drift and security gaps.
  Better approach: reusable workflows and templates.

---

## 11. Key Numbers

Track:

- queue time
- workflow duration
- runner utilization
- cost by repo/team
- failure rate
- cache hit rate
- self-hosted runner patch age

No universal runner size is best. Right-size based on workload.

---

## 12. Failure Modes

### Runner Stuck Offline

Causes:

- machine down
- runner service stopped
- token/registration issue
- network issue

Fix:

- inspect runner service
- restart or replace runner
- autoscale new runner

### Job Queues Forever

Causes:

- no runner matching labels
- runner group not available to repo
- capacity exhausted

Fix:

- check labels
- check runner group access
- scale runner pool

### Compromised Self-Hosted Runner

Response:

- isolate machine
- revoke tokens/secrets
- rotate credentials
- inspect workflow logs
- rebuild runner image
- review access policies

### Internal Network Exposed

Cause:

- untrusted workflow ran on private runner

Fix:

- isolate runner groups
- block fork PRs on private runners
- review allowed repositories

---

## 13. Scenario

- Product / system: organization with 300 repositories and private Kubernetes clusters
- Why this concept fits: teams need CI plus controlled private deployment access
- What would go wrong without it: any repo could run arbitrary code against internal networks

---

## 14. Code Sample

Self-hosted runner selection:

```yaml
jobs:
  deploy:
    runs-on: [self-hosted, linux, x64, prod-network]
    environment: production
    steps:
      - uses: actions/checkout@v4
      - run: ./deploy.sh
```

Governed pattern:

```text
Only approved deployment repositories can use prod-network runner group.
Fork PR workflows never run on prod-network runners.
```

---

## 15. Mini Program / Simulation

Runner label matching:

```python
runner = {"self-hosted", "linux", "x64", "prod-network"}
job = {"self-hosted", "linux", "prod-network"}

print("can run:", job.issubset(runner))
```

---

## 16. Practical Question

> How would you design GitHub Actions runners for a company with private cloud deployments?

---

## 17. Strong Answer

I would use GitHub-hosted runners for normal PR validation and self-hosted or ARC-managed ephemeral runners for private-network deployments. Production deployment runners would be in restricted runner groups available only to approved repositories.

I would avoid running untrusted fork PR code on self-hosted runners with internal network access. Runners would be patched, monitored, and preferably ephemeral. Labels would describe capability, and secrets/cloud roles would be least privilege. At org level, I would provide reusable workflows and templates so teams do not copy insecure patterns.

---

## 18. Revision Notes

- One-line summary: Runners define where workflows execute and what they can reach.
- Three keywords: labels, runner groups, isolation
- One interview trap: self-hosted runners are a security boundary, not just cheaper compute.
- One memory trick: runner access is network access plus code execution.

---

## 19. Official Source Notes

- GitHub-hosted runners: <https://docs.github.com/en/actions/using-github-hosted-runners/about-github-hosted-runners>
- Self-hosted runners: <https://docs.github.com/en/actions/hosting-your-own-runners>
- Runner groups: <https://docs.github.com/en/actions/hosting-your-own-runners/managing-access-to-self-hosted-runners-using-groups>
- Actions Runner Controller: <https://docs.github.com/en/actions/hosting-your-own-runners/managing-self-hosted-runners-with-actions-runner-controller>

