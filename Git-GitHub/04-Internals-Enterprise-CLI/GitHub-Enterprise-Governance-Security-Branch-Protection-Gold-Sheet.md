# GitHub Enterprise Governance Security Branch Protection Gold Sheet

> Track: Git and GitHub Command Mastery - Senior / MAANG Governance Layer

Goal: understand how mature teams govern GitHub: branch protection, rulesets, CODEOWNERS, required checks, review policy, permissions, tokens, audit, secret scanning, and enterprise repository hygiene.

---

## 0. How To Read This

Use this after the remote collaboration and GitHub CLI sheets.

Enterprise GitHub mental model:

```text
identity -> permission -> branch rule -> pull request -> required checks -> review owners -> merge policy -> audit/security controls
```

Strong interview line:

```text
GitHub at company scale is not just pull requests. It is a governed change-control system around source code.
```

---

# Topic 1: GitHub Enterprise Governance

## 1. Intuition

A personal repo optimizes for speed.

An enterprise repo optimizes for safe change.

Controls exist because source code changes can:

- deploy production bugs
- expose secrets
- bypass review
- break compliance
- modify infrastructure
- change ownership boundaries
- affect many teams in a monorepo

GitHub governance keeps collaboration fast without letting unsafe changes land silently.

---

## 2. Definition

- Definition: GitHub enterprise governance is the set of repository, organization, identity, security, review, and branch controls that make source code changes auditable and safe.
- Category: source control governance and engineering platform operations.
- Core idea: every production change should be reviewed, tested, authorized, traceable, and recoverable.

---

## 3. Why It Exists

Without governance:

- someone can push directly to main
- required CI can be skipped
- sensitive files can change without owner approval
- admins can bypass policies unnoticed
- secrets can be committed and reused
- broad tokens can leak and mutate repos
- release tags can be moved
- monorepo changes can affect teams without review
- audit trail becomes weak during incidents

---

## 4. Branch Protection And Rulesets

Branch protection and rulesets enforce merge policy.

Common controls:

| Control | Purpose |
|---|---|
| require pull request before merge | no direct push to protected branch |
| required status checks | CI/test/security checks must pass |
| require approvals | human review before merge |
| require CODEOWNERS review | owner approval for sensitive paths |
| require conversation resolution | no unresolved review threads |
| require linear history | avoid merge commits if org prefers rebase/squash |
| restrict who can push | limit emergency or release branch access |
| require signed commits/tags | verify author identity and integrity |
| block force pushes | preserve audit history |
| block deletions | protect important branches/tags |

Interview line:

```text
Protected branches turn process into enforcement: reviews, CI, ownership, and history rules become mandatory rather than optional.
```

---

## 5. Rulesets vs Classic Branch Protection

Classic branch protection is usually configured per branch pattern.

Rulesets can provide broader, layered governance across branches and tags.

Use rulesets when:

- many repos need consistent policy
- tags also need protection
- multiple branch patterns share controls
- enterprise wants central policy visibility
- bypasses must be explicit and audited

Strong answer:

```text
For a single repo, branch protection may be enough. For enterprise consistency across repos and tags, rulesets are stronger because policy can be standardized and audited.
```

---

## 6. CODEOWNERS

CODEOWNERS maps paths to required reviewers.

Example:

```text
# Payments service
/services/payments/ @payments-team

# Infrastructure
/terraform/ @platform-infra

# Security-sensitive workflows
/.github/workflows/ @platform-security @devex
```

Good uses:

- domain ownership
- security-sensitive paths
- infrastructure changes
- shared libraries
- generated API contracts
- GitHub Actions workflows

Traps:

- stale owner teams
- owners without write access
- overly broad ownership rules
- no protection rule requiring CODEOWNERS review
- treating CODEOWNERS as documentation only

---

## 7. Required Checks

Required checks should reflect release confidence.

Typical required checks:

- unit tests
- integration tests
- lint/static analysis
- build/package
- security scan
- dependency scan
- code owners review
- migration safety check if applicable
- policy checks for infra/workflow files

Rules:

```text
A check is useful only if it is deterministic, trusted, and mapped to a real release risk.
```

Avoid:

- making flaky checks required without fixing flakiness
- allowing untrusted forks to run privileged workflows
- letting PR authors bypass required checks
- using broad write tokens in PR workflows

---

## 8. Review Policy

Review policy should answer:

- How many approvals are required?
- Who owns this code path?
- Can authors approve their own changes?
- Are stale approvals dismissed after new commits?
- Are conversations required to be resolved?
- Are high-risk files reviewed by specialist teams?
- What is emergency bypass policy?

Good enterprise default:

```text
Require PR, require status checks, require CODEOWNERS review, dismiss stale approvals, block force pushes, and audit bypasses.
```

---

## 9. Merge Strategies

Common options:

| Strategy | Use When | Trade-off |
|---|---|---|
| merge commit | preserve branch context | noisier history |
| squash merge | one PR equals one commit | loses granular commit history |
| rebase merge | linear history with individual commits | rewrites PR commit ids during merge |

Strong answer:

```text
The merge strategy should match debugging and release needs. Squash is simple for product repos, merge commits preserve branch context, and rebase merge keeps history linear but needs disciplined commits.
```

---

## 10. Merge Queue

Merge queue serializes or batches merges after checks pass.

Why it exists:

- main can break when two individually passing PRs conflict together
- busy repos need tested merge ordering
- required checks should run on the candidate merge result

Use when:

- high PR volume
- strict main stability
- expensive release train
- monorepo with many dependent tests

Trade-off:

- slower individual merges
- more queue/process complexity
- requires reliable CI capacity

---

## 11. Permissions Model

Principle:

```text
Use least privilege for humans, teams, bots, and tokens.
```

Common roles:

| Role | Typical Scope |
|---|---|
| read | clone/view |
| triage | issues/PR triage |
| write | push branches, manage PRs |
| maintain | manage repo settings short of admin |
| admin | full repository control |

Recommendations:

- assign permissions through teams
- avoid direct individual admin grants
- separate production/release permissions
- review access periodically
- remove stale external collaborators
- protect `.github/workflows` and infra paths

---

## 12. Authentication And Tokens

Common auth methods:

- SSH keys
- personal access tokens
- fine-grained PATs
- GitHub Apps
- deploy keys
- OIDC from Actions to cloud providers
- SAML/SSO for enterprise identity

Token guidance:

| Token Type | Use Case | Caution |
|---|---|---|
| classic PAT | legacy automation | often too broad |
| fine-grained PAT | scoped user automation | still tied to user identity |
| GitHub App token | repo automation | preferred for app-style automation |
| deploy key | read/deploy one repo | avoid broad write unless needed |
| Actions `GITHUB_TOKEN` | workflow automation | set minimal permissions |

Strong answer:

```text
For durable automation I prefer GitHub Apps or narrowly scoped tokens over broad personal tokens, because ownership, rotation, permissions, and audit are cleaner.
```

---

## 13. Commit And Tag Signing

Signing helps verify identity and integrity.

Options:

- GPG signing
- SSH signing
- S/MIME signing
- signed tags for releases

Use signing for:

- release tags
- high-security repos
- infrastructure repos
- regulated environments
- supply-chain sensitive projects

Caution:

```text
Signed commits prove key ownership, not code correctness. They complement review and CI; they do not replace them.
```

---

## 14. Secret Scanning And Push Protection

Controls:

- secret scanning alerts
- push protection for known secret patterns
- custom secret patterns
- token revocation/rotation process
- CI checks for accidental credential files

Incident response if secret is committed:

1. Assume secret is compromised.
2. Revoke/rotate immediately.
3. Identify exposure window.
4. Remove secret from current code.
5. Consider history rewrite only after coordination.
6. Audit use of the secret.
7. Add prevention: ignore rules, scanners, push protection.

Interview line:

```text
Removing a secret from Git history is not enough. The secret must be rotated because clones, forks, caches, and logs may already contain it.
```

---

## 15. Dependency And Supply Chain Controls

GitHub-side controls may include:

- Dependabot alerts
- Dependabot version updates
- dependency review
- code scanning
- secret scanning
- branch protection for dependency manifests
- required review for lockfile changes
- release provenance or attestations where applicable

Good governance question:

```text
Who is allowed to merge dependency changes, and what checks prove the update is safe?
```

---

## 16. Audit Trail

Audit important events:

- permission changes
- branch protection/ruleset changes
- admin bypasses
- direct pushes to protected branches if allowed
- force push attempts
- secret scanning alerts
- workflow file changes
- release/tag creation or deletion
- token/App installation changes
- repository visibility changes

Strong incident question:

```text
Can we prove who changed what, who approved it, which checks ran, and why it was allowed to merge?
```

---

## 17. Repository Hygiene

Enterprise repositories should have:

- README
- ownership metadata
- CODEOWNERS
- branch protection/rulesets
- PR template
- issue templates if issues are used
- security policy
- contribution policy
- release process
- dependency update policy
- secret scanning enabled
- clear archived/deprecated status when inactive

---

## 18. Governance For GitHub Actions Files

Workflow files can be security-sensitive because they may access tokens and deployment paths.

Controls:

- CODEOWNERS for `.github/workflows`
- required security/platform review
- minimal workflow token permissions
- no untrusted script execution with privileged tokens
- pin third-party actions when appropriate
- required checks for workflow changes

Boundary:

```text
GitHub Actions details belong in the Actions track, but GitHub governance must still protect workflow files because they are source-controlled deployment logic.
```

---

## 19. Enterprise Scenarios

### Scenario 1: Main Branch Was Directly Pushed

Response:

1. Identify commits and actor.
2. Confirm branch protection/ruleset bypass path.
3. Review diff and CI status.
4. Revert or fix forward if needed.
5. Tighten push restrictions and bypass permissions.
6. Record incident and audit gap.

### Scenario 2: CODEOWNERS Did Not Trigger

Check:

- CODEOWNERS file location and syntax
- team has write access
- branch protection requires CODEOWNER review
- path rule order
- PR target branch protection

### Scenario 3: Bot Token Over-Permitted

Fix:

- rotate token
- narrow permissions
- move to GitHub App if durable automation
- audit token usage
- update automation documentation

---

## 20. Common Mistakes

| Mistake | Better Approach |
|---|---|
| direct pushes to main | protected branches/rulesets and PR-only flow |
| broad admin access | team-based least privilege |
| CODEOWNERS without required review | enforce owner review in branch rules |
| required checks that are flaky | fix tests or separate blocking/non-blocking checks |
| broad PAT in automation | GitHub App or fine-grained scoped token |
| secret removed but not rotated | rotate first, then clean history if needed |
| unsigned release tags in regulated systems | annotated and signed tags |
| allowing workflow changes without review | CODEOWNERS and required security/platform approval |
| no audit of bypasses | explicit bypass list and audit review |

---

## 21. Strong Interview Answer

Prompt:

```text
How would you make GitHub safe for a 200-engineer organization?
```

Answer:

```text
I would start with organization identity and team-based permissions, then enforce branch rules or rulesets on protected branches and release tags. Main should require PRs, passing checks, CODEOWNERS review, resolved conversations, and no force pushes. Sensitive paths like workflows, infrastructure, and security code need specialist owners. Automation should use least-privilege GitHub Apps or scoped tokens, not broad PATs. I would enable secret scanning, dependency/code scanning where appropriate, audit permission and ruleset changes, and define an emergency bypass process that is logged and reviewed.
```

---

## 22. Revision Notes

- Enterprise GitHub is a governed change-control system.
- Branch protection/rulesets enforce review, checks, history, and bypass policy.
- CODEOWNERS is useful only when owner review is required.
- Required checks should map to real release risks.
- Secrets must be rotated, not only deleted from history.
- Least privilege applies to humans, bots, tokens, and Apps.
- Workflow files are security-sensitive code.
- Auditability is part of production readiness.

---

## 23. Official Source Notes

- GitHub protected branches: https://docs.github.com/en/repositories/configuring-branches-and-merges-in-your-repository/managing-protected-branches
- GitHub rulesets: https://docs.github.com/en/repositories/configuring-branches-and-merges-in-your-repository/managing-rulesets
- GitHub CODEOWNERS: https://docs.github.com/en/repositories/managing-your-repositorys-settings-and-features/customizing-your-repository/about-code-owners
- GitHub repository roles: https://docs.github.com/en/organizations/managing-access-to-your-organizations-repositories/repository-roles-for-an-organization
- GitHub secret scanning: https://docs.github.com/en/code-security/secret-scanning
- GitHub fine-grained PATs: https://docs.github.com/en/authentication/keeping-your-account-and-data-secure/managing-your-personal-access-tokens
