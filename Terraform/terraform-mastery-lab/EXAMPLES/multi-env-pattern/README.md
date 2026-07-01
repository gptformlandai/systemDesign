# Example: Multi-Environment Pattern

Directory-based environment isolation: dev and prod share modules but have separate state files and configurations.

---

## Structure

```text
multi-env-pattern/
├── modules/
│   └── vpc/
│       ├── main.tf
│       ├── variables.tf
│       └── outputs.tf
├── environments/
│   ├── dev/
│   │   ├── main.tf         ← calls module with dev sizing
│   │   ├── variables.tf
│   │   └── versions.tf     ← backend key: environments/dev/...
│   └── prod/
│       ├── main.tf         ← calls module with prod sizing
│       ├── variables.tf
│       └── versions.tf     ← backend key: environments/prod/...
```

---

## Key Points

1. Each environment directory is a separate root module
2. Each has its own backend key → separate state files
3. Shared modules in `modules/` — no duplication
4. Dev uses smaller CIDR, fewer AZs; prod uses larger, multi-AZ
5. Apply dev first, validate, then apply prod

---

## Apply

```bash
# Dev
cd environments/dev
terraform init
terraform apply

# Prod (separate state — cannot accidentally touch dev)
cd environments/prod
terraform init
terraform apply
```
