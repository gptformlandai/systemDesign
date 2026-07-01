# HCL Reference Cheatsheet

## Block Types

```hcl
resource  "<TYPE>" "<NAME>" { }       # creates infrastructure
data      "<TYPE>" "<NAME>" { }       # reads existing infrastructure
variable  "<NAME>"           { }       # input parameter
output    "<NAME>"           { }       # exposed value
locals    { }                          # computed intermediate values
module    "<NAME>"           { }       # calls a child module
terraform { }                          # version + backend config
provider  "<NAME>"           { }       # provider configuration
moved     { }                          # rename resource in state
import    { }                          # import existing resource (TF 1.5+)
check     "<NAME>"           { }       # ongoing assertion (TF 1.5+)
```

## Variable Types

```hcl
string | number | bool
list(string) | set(string) | map(string)
object({ name = string, count = optional(number, 1) })
tuple([string, number, bool])
any
```

## Version Constraint Operators

```text
= 1.5.0      exact
>= 1.5.0     minimum
<= 1.5.0     maximum
!= 1.5.0     exclude
~> 5.0       >= 5.0, < 6.0  (minor pin)
~> 5.1.0     >= 5.1.0, < 5.2.0  (patch pin)
```

## Meta-Arguments (All Resources)

```hcl
count       = number
for_each    = map or set
depends_on  = [refs]
provider    = provider.alias
lifecycle {
  create_before_destroy = bool
  prevent_destroy       = bool
  ignore_changes        = [attr, ...]
  replace_triggered_by  = [refs]
}
```

## Key Expressions

```hcl
# Conditional
var.env == "prod" ? "t3.large" : "t3.micro"

# For expression (list)
[for item in list : item.id]
[for item in list : item.id if item.active]

# For expression (map)
{for k, v in map : k => upper(v)}

# Splat
aws_instance.web[*].id

# String interpolation
"web-${var.environment}-${count.index}"

# Heredoc
<<-EOT
  line 1
  line 2
EOT
```

## Top Functions

```hcl
merge({a=1}, {b=2})              → {a=1, b=2}
concat(["a"], ["b"])             → ["a","b"]
flatten([["a"], ["b","c"]])      → ["a","b","c"]
toset(["b","a","b"])             → {"a","b"}
tolist(toset(["b","a"]))         → ["a","b"]
zipmap(["a","b"], [1,2])         → {a=1, b=2}
keys({a=1, b=2})                 → ["a","b"]
values({a=1, b=2})               → [1, 2]
length(x)                        → count
contains(list, val)              → bool
element(list, idx)               → value (wraps)
lookup(map, key, default)        → value or default
try(expr1, expr2)                → first non-error
can(expr)                        → bool
cidrsubnet("10.0.0.0/16", 8, 1) → "10.0.1.0/24"
cidrhost("10.0.1.0/24", 5)      → "10.0.1.5"
jsonencode({a = 1})              → "{\"a\":1}"
jsondecode("{\"a\":1}")         → {a=1}
file("path/to/file.sh")          → file contents
templatefile("init.tftpl", {})   → rendered template
base64encode("hello")            → "aGVsbG8="
format("%-10s %d", "item", 42)   → "item       42"
upper("hello") / lower("HELLO")  → "HELLO" / "hello"
split(",", "a,b,c")             → ["a","b","c"]
join(",", ["a","b"])            → "a,b"
trimprefix("web-prod", "web-")  → "prod"
```
