# Terraform CLI Cheatsheet

## Essential Workflow

```bash
terraform init                    # setup
terraform plan -out=tfplan        # review
terraform apply tfplan            # execute
terraform destroy                 # cleanup
```

## Plan Flags

```bash
-out=tfplan              # save plan
-destroy                 # plan destroy
-target=resource.name    # partial plan
-var="key=value"         # inline variable
-var-file=file.tfvars    # variable file
-refresh=false           # skip API refresh (fast)
-refresh-only            # update state only
-replace=resource.name   # force replace
-no-color                # CI/CD output
-detailed-exitcode       # exit 2 if changes present
-compact-warnings        # terse warnings
```

## Apply Flags

```bash
tfplan                   # apply saved plan (no confirm needed)
-auto-approve            # skip confirmation
-target=resource.name    # partial apply
-replace=resource.name   # force replace
-refresh-only            # accept drift into state
```

## State Commands

```bash
terraform state list                      # list all resources
terraform state show aws_vpc.main         # inspect resource
terraform state mv <from> <to>            # rename address
terraform state rm <address>              # remove (no destroy)
terraform state pull > backup.json        # backup state
terraform state push backup.json          # restore state
terraform force-unlock <LOCK_ID>          # release stale lock
```

## Import

```bash
terraform import aws_vpc.main vpc-0abc1234
terraform import 'aws_subnet.this["public"]' subnet-0abc
terraform import module.vpc.aws_vpc.this vpc-0abc1234
```

## Workspace

```bash
terraform workspace list                  # list
terraform workspace new <name>            # create
terraform workspace select <name>         # switch
terraform workspace show                  # current
terraform workspace delete <name>         # delete
```

## Utilities

```bash
terraform validate                # syntax check
terraform fmt -recursive          # format all files
terraform fmt -check              # check only (CI)
terraform output -json            # all outputs as JSON
terraform output -raw <name>      # raw value for shell
terraform console                 # REPL for expressions
terraform version                 # show version
terraform providers               # show providers
terraform graph | dot -Tsvg > g.svg  # dependency graph
```

## Environment Variables

```bash
TF_LOG=DEBUG              # verbose logging
TF_LOG_PATH=/tmp/tf.log   # log to file
TF_INPUT=false            # disable prompts (CI)
TF_VAR_<name>=value       # set variable
CHECKPOINT_DISABLE=1      # disable telemetry
```

## Exit Codes

| Code | Meaning |
|---|---|
| 0 | Success / No changes |
| 1 | Error |
| 2 | Changes present (with `-detailed-exitcode`) |
