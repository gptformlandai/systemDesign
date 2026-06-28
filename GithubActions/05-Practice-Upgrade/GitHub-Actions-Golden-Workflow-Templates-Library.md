# GitHub Actions Golden Workflow Templates Library

> Goal: provide high-signal workflow templates that are easy to revise, adapt, and explain in interviews.

---

## 0. How To Use This Library

These templates are learning templates.

Before production use, adjust:

- paths
- runner labels
- language versions
- secrets
- cloud login steps
- environment names
- registry names
- deployment commands
- permissions

Rule:

> Copy the structure, not blindly the values.

---

# Template 1: Java Spring Boot PR CI

```yaml
name: Java Spring Boot CI

on:
  pull_request:
    paths:
      - "services/order/**"
      - ".github/workflows/order-ci.yml"

permissions:
  contents: read

concurrency:
  group: order-ci-${{ github.event.pull_request.number }}
  cancel-in-progress: true

jobs:
  verify:
    runs-on: ubuntu-latest
    services:
      postgres:
        image: postgres:16
        env:
          POSTGRES_USER: order
          POSTGRES_PASSWORD: order
          POSTGRES_DB: order_test
        ports:
          - 5432:5432
        options: >-
          --health-cmd pg_isready
          --health-interval 10s
          --health-timeout 5s
          --health-retries 5
    defaults:
      run:
        working-directory: services/order
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-java@v4
        with:
          distribution: temurin
          java-version: "21"
          cache: maven
      - run: ./mvnw -B verify
      - uses: actions/upload-artifact@v4
        if: always()
        with:
          name: order-test-reports
          path: services/order/target/surefire-reports/
```

Use when:

- backend service needs real database integration tests
- PR checks should be fast but meaningful

---

# Template 2: Frontend PR CI With Playwright

```yaml
name: Frontend CI

on:
  pull_request:
    paths:
      - "frontend/**"

permissions:
  contents: read

concurrency:
  group: frontend-ci-${{ github.event.pull_request.number }}
  cancel-in-progress: true

jobs:
  verify:
    runs-on: ubuntu-latest
    defaults:
      run:
        working-directory: frontend
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-node@v4
        with:
          node-version: "22"
          cache: npm
          cache-dependency-path: frontend/package-lock.json
      - run: npm ci
      - run: npm run lint
      - run: npm run typecheck
      - run: npm test -- --run
      - run: npm run build

  e2e:
    runs-on: ubuntu-latest
    needs: verify
    defaults:
      run:
        working-directory: frontend
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-node@v4
        with:
          node-version: "22"
          cache: npm
          cache-dependency-path: frontend/package-lock.json
      - run: npm ci
      - run: npx playwright install --with-deps
      - run: npm run e2e
      - uses: actions/upload-artifact@v4
        if: always()
        with:
          name: playwright-report
          path: frontend/playwright-report/
```

Use when:

- frontend needs lint, types, tests, build, and browser evidence

---

# Template 3: Docker Build And Push To GHCR

```yaml
name: Build Image

on:
  push:
    branches: [main]

permissions:
  contents: read
  packages: write

jobs:
  docker:
    runs-on: ubuntu-latest
    outputs:
      image: ${{ steps.meta.outputs.image }}
    steps:
      - uses: actions/checkout@v4
      - uses: docker/setup-buildx-action@v3
      - uses: docker/login-action@v3
        with:
          registry: ghcr.io
          username: ${{ github.actor }}
          password: ${{ secrets.GITHUB_TOKEN }}
      - id: meta
        run: echo "image=ghcr.io/${{ github.repository }}:sha-${{ github.sha }}" >> "$GITHUB_OUTPUT"
      - uses: docker/build-push-action@v6
        with:
          context: .
          push: true
          tags: ${{ steps.meta.outputs.image }}
          cache-from: type=gha
          cache-to: type=gha,mode=max
```

Use when:

- you need immutable image artifacts
- deployment will consume the image tag/digest

---

# Template 4: Production Deployment With Environment Approval

```yaml
name: Deploy Production

on:
  workflow_dispatch:
    inputs:
      image:
        required: true
        type: string

permissions:
  contents: read
  id-token: write

concurrency:
  group: deploy-production
  cancel-in-progress: false

jobs:
  deploy:
    runs-on: ubuntu-latest
    environment: production
    steps:
      - uses: actions/checkout@v4
      - run: echo "Authenticate to cloud using OIDC"
      - run: echo "Deploy image ${{ inputs.image }}"
      - run: echo "Run smoke test"
```

Use when:

- production requires approval
- deployment should be serialized
- cloud authentication should avoid static credentials

---

# Template 5: Terraform Plan On Pull Request

```yaml
name: Terraform Plan

on:
  pull_request:
    paths:
      - "infra/**"

permissions:
  contents: read
  pull-requests: write
  id-token: write

jobs:
  plan:
    runs-on: ubuntu-latest
    defaults:
      run:
        working-directory: infra
    steps:
      - uses: actions/checkout@v4
      - run: echo "Authenticate to cloud using OIDC"
      - run: terraform fmt -check
      - run: terraform init
      - run: terraform validate
      - run: terraform plan -no-color
```

Use when:

- reviewers need to see infrastructure blast radius before merge

---

# Template 6: Terraform Apply With Approval

```yaml
name: Terraform Apply

on:
  workflow_dispatch:
    inputs:
      environment:
        required: true
        type: choice
        options: [stage, production]

permissions:
  contents: read
  id-token: write

concurrency:
  group: terraform-${{ inputs.environment }}
  cancel-in-progress: false

jobs:
  apply:
    runs-on: ubuntu-latest
    environment: ${{ inputs.environment }}
    defaults:
      run:
        working-directory: infra
    steps:
      - uses: actions/checkout@v4
      - run: echo "Authenticate to cloud using OIDC"
      - run: terraform init
      - run: terraform plan -out=tfplan
      - run: terraform apply -auto-approve tfplan
```

Use when:

- infrastructure changes must be controlled per environment

---

# Template 7: Reusable Java CI Workflow

Reusable workflow:

```yaml
name: Reusable Java CI

on:
  workflow_call:
    inputs:
      working-directory:
        required: true
        type: string
      java-version:
        required: false
        type: string
        default: "21"

jobs:
  test:
    runs-on: ubuntu-latest
    defaults:
      run:
        working-directory: ${{ inputs.working-directory }}
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-java@v4
        with:
          distribution: temurin
          java-version: ${{ inputs.java-version }}
          cache: maven
      - run: ./mvnw -B verify
```

Caller:

```yaml
jobs:
  ci:
    uses: org/platform/.github/workflows/reusable-java-ci.yml@v1
    with:
      working-directory: services/order
      java-version: "21"
```

Use when:

- many services need the same CI standard

---

# Template 8: Rollback Workflow

```yaml
name: Rollback Production

on:
  workflow_dispatch:
    inputs:
      image:
        description: Previous known-good image tag or digest
        required: true
        type: string

permissions:
  contents: read
  id-token: write

concurrency:
  group: deploy-production
  cancel-in-progress: false

jobs:
  rollback:
    runs-on: ubuntu-latest
    environment: production
    steps:
      - uses: actions/checkout@v4
      - run: echo "Authenticate to cloud using OIDC"
      - run: echo "Rollback production to ${{ inputs.image }}"
      - run: echo "Verify health"
```

Use when:

- production recovery should be fast and approved

---

# Template 9: Scheduled Security Scan

```yaml
name: Scheduled Security Scan

on:
  schedule:
    - cron: "0 3 * * *"
  workflow_dispatch:

permissions:
  contents: read
  security-events: write

jobs:
  scan:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - run: echo "Run dependency, code, container, or IaC scans here"
```

Use when:

- security checks should run even without code changes

---

# Template 10: PR Preview Deployment Shape

```yaml
name: Preview Deployment

on:
  pull_request:
    types: [opened, synchronize, reopened, closed]

permissions:
  contents: read
  pull-requests: write

concurrency:
  group: preview-${{ github.event.pull_request.number }}
  cancel-in-progress: true

jobs:
  preview:
    if: github.event.action != 'closed'
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - run: echo "Build frontend"
      - run: echo "Deploy preview"
      - run: echo "Comment preview URL on PR"

  cleanup:
    if: github.event.action == 'closed'
    runs-on: ubuntu-latest
    steps:
      - run: echo "Delete preview environment"
```

Use when:

- reviewers need a live PR URL
- cleanup is required after PR close

---

## Final Template Checklist

Every production workflow should answer:

- What triggers it?
- What permissions does it need?
- Does it use secrets?
- Can fork PRs reach secrets?
- What runner does it use?
- What artifact/image does it produce?
- Does it upload useful failure evidence?
- Is production deployment approved?
- Is deployment serialized?
- How do we roll back?

---

## Revision Notes

- One-line summary: Templates are starting points for repeatable CI/CD patterns.
- Three keywords: permissions, artifact, rollback
- One interview trap: a copied template is not production-ready until scoped and secured.
- One memory trick: trigger, build, secure, publish, deploy, verify, recover.

