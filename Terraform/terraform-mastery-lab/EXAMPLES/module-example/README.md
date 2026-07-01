# Example: Reusable Module Pattern

Demonstrates how to build a reusable module with clean inputs, outputs, and validation.

---

## Structure

```text
module-example/
├── modules/
│   └── security-group/
│       ├── main.tf        ← security group + dynamic ingress rules
│       ├── variables.tf   ← name, vpc_id, ingress_rules list
│       └── outputs.tf     ← security_group_id, security_group_arn
└── main.tf                ← calls the module with different configs
```

---

## Learning Objectives

1. Build a module with a `dynamic` block (ingress rules from a variable)
2. Add variable validation (port range 1-65535)
3. Write outputs (ID, ARN)
4. Call the module twice with different configurations
5. Reference module outputs in other resources

---

## Key Concepts Practiced

- Module `variables.tf` with `list(object({...}))` type
- Dynamic block generating repeated nested blocks
- Variable validation blocks
- Module output references (`module.web_sg.security_group_id`)
