# GitHub Actions Security, OIDC, Secrets, and Supply Chain Gold Sheet

> Goal: secure GitHub Actions workflows with least privilege, safe secrets, OIDC cloud auth, fork PR safety, action pinning, scanning, SBOM, and artifact attestations.

---

## 0. How To Read This

Beginner focus:

- secrets
- variables
- `GITHUB_TOKEN`
- permissions

Intermediate focus:

- OIDC
- fork PR safety
- dependency review
- CodeQL
- secret scanning
- action pinning

Senior focus:

- supply-chain threat model
- self-hosted runner risk
- artifact attestations
- SBOM
- environment trust boundaries
- least-privilege cloud roles

---

# Topic 1: Security, OIDC, Secrets, and Supply Chain

---

## 1. Intuition

CI/CD is powerful because it can build and deploy production.

That also makes it dangerous.

If an attacker controls your workflow, they may control:

- source code
- secrets
- cloud roles
- production deploys
- packages
- container images

Beginner explanation:

GitHub Actions security means giving workflows only the permissions and secrets they need, protecting untrusted pull requests, and making sure build/deployment artifacts can be trusted.

---

## 2. Definition

- Definition: GitHub Actions security is the practice of protecting workflow execution, credentials, dependencies, artifacts, runners, and deployment paths from misuse or compromise.
- Category: CI/CD security and supply-chain security
- Core idea: minimize trust, avoid static secrets, verify inputs/outputs, and isolate untrusted code.

---

## 3. Why It Exists

CI/CD attacks can:

- steal cloud credentials
- publish malicious packages/images
- deploy backdoors
- exfiltrate secrets through logs/artifacts
- abuse self-hosted runners
- modify release artifacts
- run malicious code from fork PRs

Security hardening reduces blast radius.

---

## 4. Reality

Production GitHub Actions security includes:

- explicit `permissions`
- secrets and environment secrets
- OIDC to cloud providers
- action pinning/versioning
- dependency review
- CodeQL
- secret scanning
- artifact attestations
- self-hosted runner isolation
- branch/environment protection
- audit logs

Senior expectation:

You should treat CI/CD as part of the production attack surface.

---

## 5. How It Works

### Part A: `GITHUB_TOKEN`

GitHub automatically creates a token for workflow runs.

Do not rely on broad defaults.

Set permissions explicitly:

```yaml
permissions:
  contents: read
```

For package publish:

```yaml
permissions:
  contents: read
  packages: write
```

For OIDC:

```yaml
permissions:
  contents: read
  id-token: write
```

### Part B: Secrets vs Variables

| Type | Use |
|---|---|
| Secrets | sensitive values like tokens/passwords |
| Variables | non-sensitive config |
| Environment secrets | secrets scoped to deployment environment |

Rules:

- never echo secrets
- never upload `.env` artifacts
- avoid secrets in frontend builds
- rotate exposed secrets
- prefer OIDC over long-lived cloud keys

### Part C: OIDC

OIDC lets GitHub request short-lived cloud credentials.

Flow:

```text
workflow requests OIDC token
cloud provider validates repo/ref/environment claims
cloud provider returns short-lived role credentials
workflow deploys
credentials expire
```

Benefits:

- no long-lived cloud access keys in GitHub secrets
- cloud role can be limited by repo/branch/environment
- better auditability

### Part D: OIDC Trust Conditions

Cloud trust policy should restrict:

- organization
- repository
- branch/tag
- environment
- workflow if needed
- audience

Bad:

```text
any repo in org can assume prod role
```

Better:

```text
only org/repo on main with environment production can assume prod role
```

### Part E: Fork PR Security

Fork PRs are untrusted code.

Risks:

- malicious workflow changes
- secret exfiltration
- command injection
- artifact poisoning

Rules:

- do not expose secrets to fork PR code
- be careful with `pull_request_target`
- never checkout untrusted PR code and run it with trusted secrets
- separate validation from privileged actions

### Part F: Script Injection

Dangerous:

```yaml
- run: echo "${{ github.event.pull_request.title }}"
```

If user-controlled content enters shell scripts, sanitize or pass via environment variables carefully.

### Part G: Pinning Actions

Options:

- pin by major version: `actions/checkout@v4`
- pin by full commit SHA for stronger supply-chain control
- use trusted internal actions

For high-security pipelines, pin third-party actions by SHA and review updates.

### Part H: Dependency and Code Scanning

Common controls:

- Dependabot
- dependency review
- CodeQL
- secret scanning
- container scanning
- license checks if needed

Use security gates based on severity and exploitability, not raw noise alone.

### Part I: SBOM and Artifact Attestations

SBOM answers:

```text
what dependencies are in this artifact?
```

Attestation answers:

```text
what workflow built this artifact from what source?
```

Use for:

- provenance
- audit
- incident response
- supply-chain policy

### Part J: Environment Protection

Use GitHub environments for:

- prod secrets
- approval gates
- deployment history
- environment-specific variables
- branch restrictions

Production secrets should not be available in ordinary PR workflows.

### Part K: Self-Hosted Runner Security

Self-hosted runners are powerful and risky.

Protect with:

- ephemeral runners
- clean workspace per job
- network segmentation
- runner groups
- no untrusted fork PRs on sensitive runners
- least-privilege cloud/network access

---

## 6. What Problem It Solves

- Primary problem solved: reduces CI/CD credential, deployment, and supply-chain risk
- Secondary benefits: better audit, compliance, provenance, and blast-radius control
- Systems impact: prevents automation from becoming a production backdoor

---

## 7. When To Rely On It

Use strong security when:

- workflow deploys to cloud/prod
- packages/images are published
- secrets are used
- self-hosted runners are connected to private networks
- external contributors open PRs
- compliance matters

---

## 8. When Not To Take Shortcuts

Do not:

- store long-lived cloud admin keys casually
- grant broad `write-all` permissions
- run fork PR code with secrets
- deploy from unreviewed workflows
- use unpinned random third-party actions for release
- upload artifacts containing secrets

---

## 9. Pros and Cons

| Practice | Pros | Cons |
|---|---|
| Explicit permissions | smaller blast radius | more setup |
| OIDC | avoids static cloud keys | trust policy learning curve |
| Environment approvals | safer prod deploys | slower releases |
| Action SHA pinning | stronger supply-chain control | update maintenance |
| Self-hosted runners | private network/control | security responsibility |

---

## 10. Trade-offs and Common Mistakes

### Trade-offs

- Strict security:
  Safer, but more governance overhead.
- OIDC:
  Better credential model, but cloud policy must be correct.
- Fork PR isolation:
  Safer open source collaboration, but more workflow complexity.
- SHA pinning:
  Stronger integrity, but upgrades need process.

### Common Mistakes

- Mistake: "Secrets are safe because GitHub masks them."
  Why it is wrong: secrets can leak through artifacts, network calls, or transformed output.
  Better approach: minimize secrets and prefer OIDC.

- Mistake: "Use `pull_request_target` for everything."
  Why it is wrong: it can run with elevated context and secrets.
  Better approach: use it only with strict patterns and no untrusted code execution.

- Mistake: "Give workflow write-all permission."
  Why it is wrong: compromised workflow has broad repo power.
  Better approach: set least privilege.

- Mistake: "Self-hosted runner is just cheaper."
  Why it is wrong: it can access internal network and secrets.
  Better approach: isolate and harden it.

---

## 11. Key Numbers

Security rules of thumb:

- production cloud credentials should be short-lived
- production deploy should require environment protection
- token permissions should be explicit
- third-party actions in release workflows should be pinned/reviewed
- self-hosted runners should be ephemeral where practical

---

## 12. Failure Modes

### OIDC Access Denied

Causes:

- missing `id-token: write`
- cloud trust policy mismatch
- wrong branch/environment claim
- audience mismatch

Fix:

- inspect token claims
- validate cloud role trust policy
- confirm environment name

### Secret Leaked In Artifact

Actions:

- delete artifact if possible
- rotate secret
- audit logs
- narrow artifact paths

### Fork PR Attempts Secret Exfiltration

Fix:

- ensure secrets unavailable
- avoid privileged workflow on untrusted code
- use safe comment/label workflow patterns

### Compromised Action

Fix:

- pin by SHA
- use trusted internal action
- audit workflow runs
- rotate impacted credentials

---

## 13. Scenario

- Product / system: production deployment to AWS from GitHub Actions
- Why this concept fits: deployment needs cloud access without storing long-lived AWS keys
- What would go wrong without it: leaked key could allow production access outside GitHub

---

## 14. Code Sample

OIDC deployment permission skeleton:

```yaml
name: Deploy With OIDC

on:
  workflow_dispatch:

permissions:
  contents: read
  id-token: write

jobs:
  deploy:
    runs-on: ubuntu-latest
    environment: production
    steps:
      - uses: actions/checkout@v4
      - name: Configure cloud credentials
        run: echo "Use cloud provider OIDC login action here"
      - name: Deploy
        run: ./deploy.sh
```

---

## 15. Mini Program / Simulation

Permission check:

```python
required = {"contents": "read", "id-token": "write"}
workflow = {"contents": "read"}

missing = {k: v for k, v in required.items() if workflow.get(k) != v}
print("missing permissions:", missing)
```

---

## 16. Practical Question

> How would you secure a GitHub Actions workflow that deploys to production cloud infrastructure?

---

## 17. Strong Answer

I would use OIDC instead of long-lived cloud keys. The workflow would request `id-token: write`, and the cloud role trust policy would restrict access to the specific organization, repository, branch, and production environment.

The workflow would use a GitHub production environment with required reviewers and environment secrets if needed. `GITHUB_TOKEN` permissions would be least privilege. I would avoid running production deployment from fork PRs or untrusted workflow changes. Third-party actions in release paths would be pinned or tightly governed, and artifacts/images would be scanned and attested where required.

For self-hosted runners, I would isolate them, prefer ephemeral runners, and avoid running untrusted code on runners with private network access.

---

## 18. Revision Notes

- One-line summary: GitHub Actions security is least privilege, safe trust boundaries, short-lived credentials, and artifact provenance.
- Three keywords: OIDC, permissions, supply chain
- One interview trap: `pull_request_target` can be dangerous with untrusted code.
- One memory trick: secrets are liabilities; permissions are blast radius; OIDC is temporary trust.

---

## 19. Official Source Notes

- Security hardening: <https://docs.github.com/en/actions/security-for-github-actions/security-guides/security-hardening-for-github-actions>
- OpenID Connect: <https://docs.github.com/en/actions/concepts/security/openid-connect>
- Automatic token authentication: <https://docs.github.com/en/actions/security-for-github-actions/security-guides/automatic-token-authentication>
- Artifact attestations: <https://docs.github.com/en/actions/security-for-github-actions/using-artifact-attestations>

