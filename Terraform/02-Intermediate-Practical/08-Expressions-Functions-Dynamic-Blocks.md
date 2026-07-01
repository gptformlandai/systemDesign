# 08. Expressions, Functions, Dynamic Blocks

## Built-In Functions Reference

Terraform has a rich standard library. No imports needed — functions are always available.

### String Functions

```hcl
upper("hello")           # "HELLO"
lower("HELLO")           # "hello"
title("hello world")     # "Hello World"
trim("  hello  ", " ")   # "hello"
trimprefix("web-prod", "web-")  # "prod"
trimsuffix("web-prod", "-prod") # "web"
split(",", "a,b,c")      # ["a", "b", "c"]
join(",", ["a", "b"])    # "a,b"
replace("hello", "l", "r")  # "herro"
format("Hello, %s! You are %d years old.", "Alice", 30)
format("%-10s %5d", "item", 42)  # "item           42"
formatdate("YYYY-MM-DD", "2024-01-15T10:00:00Z")  # "2024-01-15"
```

### Numeric Functions

```hcl
max(1, 5, 3)     # 5
min(1, 5, 3)     # 1
abs(-42)         # 42
ceil(1.2)        # 2
floor(1.9)       # 1
parseint("10", 10)  # 10   (parse string as integer in given base)
```

### Collection Functions

```hcl
length(["a", "b", "c"])   # 3
length("hello")            # 5
length({a=1, b=2})         # 2

keys({a = 1, b = 2})      # ["a", "b"]
values({a = 1, b = 2})    # [1, 2]

contains(["a","b","c"], "b")  # true
element(["a","b","c"], 1)     # "b"  (wraps around: index 3 = "a")
index(["a","b","c"], "b")     # 1    (returns index of value)

concat(["a"], ["b","c"])  # ["a", "b", "c"]
flatten([["a"], ["b","c"]])  # ["a", "b", "c"]

merge({a=1}, {b=2}, {c=3})   # {a=1, b=2, c=3}
                               # later maps override earlier: merge({a=1},{a=2}) → {a=2}

toset(["b","a","c","a"])   # {"a", "b", "c"}  (unique, unordered)
tolist(toset(["b","a"]))   # ["a", "b"]  (set → sorted list)
tomap({a = "1", b = "2"})  # {a="1", b="2"}

zipmap(["a","b"], [1,2])   # {a=1, b=2}

slice(["a","b","c","d"], 1, 3)  # ["b", "c"]  (from index 1, up to (not including) 3)

# reverse
reverse(["a","b","c"])     # ["c", "b", "a"]

# distinct (remove duplicates from list, preserve order)
distinct(["a","b","a","c"])  # ["a","b","c"]

# compact (remove null and empty strings)
compact(["a", "", null, "b"])  # ["a", "b"]
```

### Type Conversion Functions

```hcl
tostring(42)       # "42"
tonumber("42")     # 42
tobool("true")     # true
```

### Filesystem Functions

```hcl
file("/path/to/script.sh")         # returns file contents as string
filebase64("/path/to/cert.pem")    # returns base64-encoded file contents
templatefile("/path/to/init.tftpl", { env = var.environment, port = 8080 })
```

### Networking Functions

```hcl
cidrsubnet("10.0.0.0/16", 8, 0)   # "10.0.0.0/24"
cidrsubnet("10.0.0.0/16", 8, 1)   # "10.0.1.0/24"
cidrsubnet("10.0.0.0/16", 8, 10)  # "10.0.10.0/24"
cidrhost("10.0.1.0/24", 5)        # "10.0.1.5"
cidrnetmask("10.0.1.0/24")        # "255.255.255.0"
```

### Encoding Functions

```hcl
base64encode("hello")               # "aGVsbG8="
base64decode("aGVsbG8=")           # "hello"
jsonencode({ key = "value" })      # "{\"key\":\"value\"}"
jsondecode("{\"key\":\"value\"}")  # {key = "value"}
yamlencode({ key = "value" })
yamldecode(file("config.yaml"))
```

---

## For Expressions

For expressions transform collections.

```hcl
# List comprehension
[for item in list : expression]
[for item in list : expression if condition]

# Map comprehension
{for key, value in map : new_key => new_value}
{for key, value in map : new_key => new_value if condition}

# Examples:
[for s in var.names : upper(s)]
# ["ALICE", "BOB"]

[for s in var.names : upper(s) if length(s) > 3]
# filter to names longer than 3 chars, then uppercase

{for k, v in aws_subnet.this : k => v.id}
# {public-1 = "subnet-abc", private-1 = "subnet-def"}

# Invert a map (flip keys and values)
{for k, v in var.name_to_id : v => k}

# Convert list to set of objects
toset([for i in range(3) : { index = i, name = "item-${i}" }])
```

---

## Conditional Expressions

```hcl
# Ternary
condition ? value_if_true : value_if_false

instance_type = var.environment == "prod" ? "t3.large" : "t3.micro"
subnet_ids    = var.private ? aws_subnet.private[*].id : aws_subnet.public[*].id
```

---

## try() And can()

```hcl
# try(): return first argument that doesn't error, else last argument
region = try(var.region, data.aws_region.current.name, "us-east-1")

# Handle optional nested attribute that may not exist
name = try(var.config.name, "default-name")

# can(): test if an expression evaluates without error → returns bool
can(var.optional_config.name)  # true if var.optional_config is set and has .name
```

---

## Splat Operator

```hcl
# Traditional for expression
[for instance in aws_instance.web : instance.id]

# Splat shorthand (equivalent)
aws_instance.web[*].id

# Nested splat
aws_instance.web[*].network_interface[*].private_ip
```

---

## Dynamic Blocks

Dynamic blocks generate repeated nested blocks inside a resource.

```hcl
# Without dynamic (hardcoded ingress rules — repeated boilerplate):
resource "aws_security_group" "web" {
  name = "web-sg"

  ingress {
    from_port   = 80
    to_port     = 80
    protocol    = "tcp"
    cidr_blocks = ["0.0.0.0/0"]
  }
  ingress {
    from_port   = 443
    to_port     = 443
    protocol    = "tcp"
    cidr_blocks = ["0.0.0.0/0"]
  }
}

# With dynamic block (driven by variable):
variable "ingress_rules" {
  type = list(object({
    port        = number
    protocol    = string
    cidr_blocks = list(string)
  }))
  default = [
    { port = 80,  protocol = "tcp", cidr_blocks = ["0.0.0.0/0"] },
    { port = 443, protocol = "tcp", cidr_blocks = ["0.0.0.0/0"] },
    { port = 8080, protocol = "tcp", cidr_blocks = ["10.0.0.0/8"] },
  ]
}

resource "aws_security_group" "web" {
  name = "web-sg"

  dynamic "ingress" {
    for_each = var.ingress_rules
    content {
      from_port   = ingress.value.port
      to_port     = ingress.value.port
      protocol    = ingress.value.protocol
      cidr_blocks = ingress.value.cidr_blocks
    }
  }

  egress {
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }
}
```

### Dynamic Block Anatomy

```hcl
dynamic "<BLOCK_TYPE>" {
  for_each = <collection>
  iterator = <optional_alias>    # default: same as block type
  content {
    # Use <iterator>.key, <iterator>.value
    # or <iterator>.value.<field> for objects
  }
}
```

---

## Practical Patterns

### Generating Subnet CIDRs From A VPC CIDR

```hcl
locals {
  vpc_cidr = "10.0.0.0/16"
  azs      = ["us-east-1a", "us-east-1b", "us-east-1c"]
  
  public_cidrs  = [for i, az in local.azs : cidrsubnet(local.vpc_cidr, 8, i)]
  private_cidrs = [for i, az in local.azs : cidrsubnet(local.vpc_cidr, 8, i + 10)]
}
# public_cidrs  = ["10.0.0.0/24", "10.0.1.0/24", "10.0.2.0/24"]
# private_cidrs = ["10.0.10.0/24", "10.0.11.0/24", "10.0.12.0/24"]
```

### Flatten Nested Structure

```hcl
variable "environment_subnets" {
  default = {
    dev  = ["10.1.0.0/24", "10.1.1.0/24"]
    prod = ["10.0.0.0/24", "10.0.1.0/24"]
  }
}

locals {
  all_cidrs = flatten([for env, cidrs in var.environment_subnets : cidrs])
  # ["10.1.0.0/24", "10.1.1.0/24", "10.0.0.0/24", "10.0.1.0/24"]
}
```

---

## Interview Sound Bite

Terraform's expression language centers on three patterns: `for` expressions transform collections (`[for x in list : x.id]`), the ternary conditional `condition ? a : b` for conditional values, and `dynamic` blocks for generating repeated nested blocks from a variable list. The most-used functions: `merge()` for maps, `flatten()` to denest lists, `cidrsubnet()` for IP math, `try()` for safe attribute access, `templatefile()` for rendering scripts with variables. The splat operator `[*]` is syntactic sugar for `for` over a collection. Dynamic blocks keep security group rules, EBS volumes, and other repeated nested blocks DRY.
