# Git Monorepo Performance and Maintenance Gold Sheet

> Goal: understand how large repositories stay usable: clone strategies, sparse checkout, partial clone, commit-graph, maintenance, Git LFS, generated files, and large-repo diagnostics.

---

## 1. Intuition

A small repo is a backpack.

A monorepo can become a warehouse.

You need different movement patterns:

- do not load everything if you only need one service
- do not store huge binaries as normal Git objects
- do not let generated files churn diffs
- do not make every command scan the whole world
- do not let CI rebuild everything for every path

Senior mental model:

```text
working set size + history size + file count + binary policy + tooling assumptions = developer experience
```

---

## 2. Definition

- Definition: Git monorepo performance is the practice of keeping large repositories fast and reliable through checkout shaping, object filtering, file policies, maintenance, and workflow design.
- Category: developer productivity / repository operations.
- Core idea: reduce unnecessary local data, reduce expensive scans, and keep repository history healthy.

---

## 3. Why It Exists

Large repos create friction:

- clone takes too long
- status is slow
- IDE indexing is heavy
- CI runs too much
- file watchers struggle
- history contains large binaries
- subtrees are owned by many teams
- generated files create noisy diffs
- checkout includes code a developer never touches

The solution is not one magic command. It is a set of repo policies and tools.

---

## 4. Performance Levers

| Lever | What It Improves | Trade-off |
|---|---|---|
| shallow clone | less history downloaded | some history commands limited |
| partial clone | fewer objects downloaded initially | needs server/client support |
| sparse checkout | fewer working tree files | tools may expect full repo |
| Git LFS | large binaries outside normal Git objects | LFS quotas and infra |
| commit-graph | faster graph walks | maintenance required |
| fsmonitor | faster working tree status | platform/tooling support |
| scalar | opinionated large-repo optimization | not needed for small repos |
| git maintenance | background cleanup and optimization | must avoid disrupting work |
| path-based CI | fewer builds/tests | dependency graph must be correct |

---

## 5. Clone Strategies

Normal clone:

```bash
git clone git@github.com:ORG/REPO.git
```

Shallow clone:

```bash
git clone --depth=1 git@github.com:ORG/REPO.git
```

Partial clone with blob filtering:

```bash
git clone --filter=blob:none git@github.com:ORG/REPO.git
```

Sparse checkout after clone:

```bash
git sparse-checkout init --cone
git sparse-checkout set services/orders libs/common
```

Combined large-repo shape:

```bash
git clone --filter=blob:none --sparse git@github.com:ORG/REPO.git
cd REPO
git sparse-checkout set services/orders libs/common
```

When not to use:

- small repo
- tooling cannot handle sparse trees
- developer frequently touches many areas
- build requires full tree and cannot be changed

---

## 6. Sparse Checkout

Sparse checkout controls what appears in your working tree.

Good for:

- monorepos
- service-specific work
- generated/documentation-heavy repos
- onboarding with limited scope

Commands:

```bash
git sparse-checkout list
git sparse-checkout add services/payments
git sparse-checkout disable
```

Failure mode:

```text
Build fails because a script assumes files outside sparse checkout exist.
```

Fix:

- add required paths
- teach build tooling about sparse checkout
- disable sparse checkout if needed

---

## 7. Partial Clone

Partial clone can defer downloading some objects until needed.

Common filter:

```bash
git clone --filter=blob:none <repo>
```

Meaning:

- commit and tree data are available
- file blobs may be fetched on demand
- local repo references a promisor remote for missing objects

Use when:

- history is large
- many files are never touched locally
- server supports partial clone

Avoid overpromising:

> Partial clone reduces initial object transfer, but it is not a substitute for good repo hygiene.

---

## 8. Git LFS Policy

Use Git LFS for:

- design assets
- videos
- model files
- large binary test fixtures
- archives that must be versioned

Avoid Git LFS for:

- normal source code
- small text configs
- generated build outputs
- dependency archives that belong in package/artifact storage

Commands:

```bash
git lfs install
git lfs track "*.psd"
git lfs track "*.zip"
git add .gitattributes
git commit -m "Track large assets with Git LFS"
git lfs ls-files
```

Policy questions:

- what size threshold requires LFS?
- who pays storage/bandwidth?
- what file types are allowed?
- how are old large files cleaned?
- are CI runners configured for LFS?

---

## 9. Maintenance Commands

Inspect object storage:

```bash
git count-objects -vH
```

Run maintenance:

```bash
git maintenance run
git maintenance start
```

Garbage collection:

```bash
git gc
```

Integrity check:

```bash
git fsck
```

Commit graph:

```bash
git commit-graph write --reachable
```

Prune stale remote branches:

```bash
git fetch --prune
git remote prune origin
```

Caution:

- do not run aggressive cleanup blindly during active incident work
- coordinate cleanup after major history rewrites
- CI caches may also need cleanup

---

## 10. Fsmonitor And Scalar

Fsmonitor helps Git avoid scanning every file to detect changes.

Useful when:

- many files
- slow `git status`
- supported OS/editor/tooling

Scalar provides a large-repo-focused workflow that can configure maintenance and performance features.

Use carefully:

- learn what it configures
- test with your repo
- document the expected developer setup
- keep fallback commands understandable

---

## 11. Monorepo Governance

Monorepos need social and technical ownership:

- CODEOWNERS by path
- required checks by affected area
- path-based CI
- dependency graph for affected builds
- generated file policy
- large file policy
- release ownership
- branch/ruleset governance
- merge queue if main stability is difficult

PR policy:

| Change Type | Requirement |
|---|---|
| service-local | service owner review |
| shared library | affected teams or platform review |
| build tooling | DevEx/platform review |
| workflow files | security/platform review |
| generated contract | source and generated output policy |
| large asset | LFS/artifact policy check |

---

## 12. Diagnostics

When a repo feels slow:

1. Is clone slow or every command slow?
2. Is object database huge?
3. Are there large blobs in history?
4. Is working tree file count huge?
5. Is status slow?
6. Are generated files tracked?
7. Are LFS files misconfigured?
8. Are hooks slow?
9. Are CI path filters wrong?
10. Are developers using full checkout unnecessarily?

Commands:

```bash
git status --short
git count-objects -vH
git remote -v
git sparse-checkout list
git lfs ls-files
git maintenance run
```

---

## 13. Practical Question

> Your company has a monorepo with 400 services. Clone takes 45 minutes, `git status` is slow, CI runs too much, and developers keep committing large binaries. What do you propose?

---

## 14. Strong Answer

I would attack working set, history, file policy, and CI scope separately.

For local work, I would evaluate partial clone and sparse checkout for service teams, plus fsmonitor/scalar/maintenance where supported. For large binaries, I would define a size/type policy, use Git LFS or artifact storage, and clean existing history only through a coordinated rewrite if the bloat is severe. For CI, I would use path-aware affected builds with a correct dependency graph and CODEOWNERS by path. I would also add repo health checks: object size reporting, generated file policy, required checks for large files, and onboarding docs for the recommended clone mode.

Trade-off: sparse/partial clone improves developer experience, but some tools must be updated because they assume a full checkout.

---

## 15. Revision Notes

- One-line summary: Large Git repos need working-set control, object hygiene, and ownership policy.
- Three keywords: sparse, partial, maintenance.
- One interview trap: treating LFS as a cure for already-bloated history.
- One memory trick: "Download less, track less, scan less, rebuild less."

---

## 16. Official Source Notes

- Git partial clone docs: <https://git-scm.com/docs/partial-clone>
- Git maintenance docs: <https://git-scm.com/docs/git-maintenance>
- Git commit-graph docs: <https://git-scm.com/docs/git-commit-graph>
- Git fsmonitor docs: <https://git-scm.com/docs/git-fsmonitor--daemon>
- Scalar docs: <https://git-scm.com/docs/scalar>
- GitHub LFS docs: <https://docs.github.com/en/repositories/working-with-files/managing-large-files/about-git-large-file-storage>
