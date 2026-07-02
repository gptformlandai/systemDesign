# GitHub Repository Lifecycle and Administration Gold Sheet

> Goal: manage repositories as products: creation, templates, settings, visibility, ownership, issue/PR templates, labels, branch defaults, transfers, archive/deprecation, and operational hygiene.

---

## 1. Intuition

A repository is not just a folder with code.

In a company, a repo is a managed asset:

- owner
- purpose
- visibility
- permissions
- branch policy
- release policy
- security settings
- templates
- automation
- lifecycle status

Senior signal:

> A healthy repo tells contributors how to work, who owns it, how it ships, and how it is governed.

---

## 2. Definition

- Definition: Repository lifecycle administration is the management of a GitHub repository from creation through active development, migration, maintenance, archival, or deletion.
- Category: GitHub administration / platform governance.
- Core idea: standardize repo setup so collaboration, security, and operations are predictable.

---

## 3. Why It Exists

Without repo administration:

- nobody knows who owns code
- direct pushes bypass review
- PRs miss templates and tests
- stale repos keep secrets and broad access
- old release branches are unprotected
- issues/labels are inconsistent
- templates drift across teams
- archived projects keep confusing users

Repo hygiene is developer experience and risk reduction.

---

## 4. Repository Creation Checklist

When creating a repo:

1. Set clear name and description.
2. Choose owner org/team.
3. Choose visibility: private, internal, public.
4. Add README.
5. Add license if public/open source.
6. Add `.gitignore`.
7. Add CODEOWNERS if team-owned.
8. Add PR and issue templates.
9. Configure default branch.
10. Configure branch protection/rulesets.
11. Configure security settings.
12. Configure topics and service catalog metadata.
13. Configure Actions permissions if workflows exist.
14. Add release/deployment policy if relevant.

GitHub CLI examples:

```bash
gh repo create ORG/REPO --private --description "Payment service"
gh repo view ORG/REPO
gh repo edit ORG/REPO --description "Payment authorization service"
```

---

## 5. Template Repositories

Template repos are useful when:

- teams create many similar services
- standard CI, README, ownership, and layout matter
- platform wants golden paths
- security wants default scanning/workflow settings

Template should include:

- README skeleton
- service ownership file
- CODEOWNERS
- PR template
- issue templates
- default `.gitignore`
- `.gitattributes`
- baseline CI workflow
- dependency update config
- security policy if needed
- starter docs

Avoid:

- putting secrets in templates
- stale workflow versions
- one template for every language/framework
- templates without ownership or maintenance

---

## 6. Issue And PR Templates

PR template should ask:

- what changed
- why it changed
- testing evidence
- risk
- rollback
- deployment notes
- screenshots if UI
- linked issue/ticket

Example:

```md
## Summary

## Testing

## Risk

## Rollback
```

Issue templates can separate:

- bug report
- feature request
- incident follow-up
- security report
- documentation request

Do not make templates so heavy that contributors ignore them.

---

## 7. Labels, Milestones, Projects, Discussions

Labels:

- `bug`
- `enhancement`
- `documentation`
- `security`
- `good first issue`
- `breaking-change`
- `release-blocker`
- `needs-owner`

Milestones:

- release trains
- major initiatives
- patch releases

Projects:

- team planning
- cross-repo initiatives
- migration tracking

Discussions:

- community Q&A
- RFC-style conversations
- design ideas before issues/PRs

Governance rule:

> Labels and Projects help only when the team agrees on what each state means.

---

## 8. Visibility And Access

Visibility choices:

| Visibility | Use |
|---|---|
| private | sensitive product/business code |
| internal | enterprise-wide sharing inside org/enterprise |
| public | open-source or public docs/code |

Access principles:

- grant through teams, not individuals
- keep admin access rare
- remove stale outside collaborators
- separate release authority
- audit deploy keys and Apps
- restrict branch/tag writes

Lifecycle risk:

> Old repos often keep old secrets, stale admins, and forgotten deploy keys.

---

## 9. Transfer, Rename, Archive, Delete

### Rename

Use when:

- service was renamed
- naming standard changed

Check:

- remote URLs
- CI references
- docs
- service catalog
- package/release links

### Transfer

Use when:

- repo moves to another org/team
- ownership changes

Check:

- permissions
- webhooks
- Actions secrets/environments
- GitHub Apps
- branch protections/rulesets

### Archive

Use when:

- repo is read-only historical reference
- service is decommissioned

Before archive:

- remove secrets
- update README with status
- remove broad write access
- close/transfer open issues if needed
- preserve release notes if relevant

### Delete

Use only after:

- retention policy check
- legal/compliance check
- artifact migration
- owner approval
- backup if required

---

## 10. Repository Health Review

Quarterly or automated checks:

- owner exists
- README is current
- default branch protected
- CODEOWNERS valid
- required checks valid
- no stale admins
- no broad deploy keys
- secret scanning enabled
- dependency alerts configured
- Actions permissions minimal
- branch/tag rulesets active
- archived repos marked clearly
- old release branches protected or deleted
- topics/service catalog metadata current

Automation can open issues/PRs for missing items.

---

## 11. Failure Modes

| Failure | Impact | Fix |
|---|---|---|
| repo has no owner | slow incident response | assign team owner |
| default branch unprotected | direct risky pushes | ruleset/branch protection |
| stale admin user | account compromise blast radius | access review |
| no PR template | low-quality reviews | add focused template |
| archived repo still has secrets | exposure risk | remove/rotate/archive carefully |
| template repo stale | bad defaults spread | assign platform owner |
| repo transfer breaks automation | CI/deploy outage | preflight webhook/App/secrets check |

---

## 12. Practical Question

> Your platform team needs every new service repo to follow the same GitHub standards. What would you set up?

---

## 13. Strong Answer

I would create maintained template repositories or a repo bootstrap process with golden defaults: README, service ownership metadata, CODEOWNERS, PR templates, issue templates, `.gitignore`, `.gitattributes`, baseline CI, dependency/security configuration, and branch rulesets. Access would be team-based with least privilege. Sensitive paths like workflows and infrastructure would require platform/security review. I would add automated repo health checks to detect missing owners, stale admins, disabled scanning, invalid CODEOWNERS, or missing required checks.

---

## 14. Revision Notes

- One-line summary: A repository is an owned, governed software asset.
- Three keywords: owner, template, lifecycle.
- One interview trap: creating repos without branch protection and ownership.
- One memory trick: "Create, govern, operate, retire."

---

## 15. Official Source Notes

- GitHub repository docs: <https://docs.github.com/en/repositories>
- GitHub template repository docs: <https://docs.github.com/en/repositories/creating-and-managing-repositories/creating-a-template-repository>
- GitHub issue and PR template docs: <https://docs.github.com/en/repositories/managing-your-repositorys-settings-and-features/customizing-your-repository/about-issue-and-pull-request-templates>
- GitHub CODEOWNERS docs: <https://docs.github.com/en/repositories/managing-your-repositorys-settings-and-features/customizing-your-repository/about-code-owners>
- GitHub rulesets docs: <https://docs.github.com/en/repositories/configuring-branches-and-merges-in-your-repository/managing-rulesets/about-rulesets>
