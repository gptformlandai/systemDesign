# Cheatsheet 06: Security And ACL Commands

## AUTH

```bash
AUTH password                        # legacy single password
AUTH username password               # ACL user login
```

## ACL Commands

```bash
ACL LIST                             # list all users and rules
ACL WHOAMI                           # current username
ACL CAT                              # list categories
ACL CAT category                     # list commands in category
ACL SETUSER username [rules]         # create or update user
ACL GETUSER username                 # get user details
ACL DELUSER username [username...]   # delete user
ACL LOG [RESET]                      # view or reset ACL violation log
```

## ACL Rule Syntax

```bash
# Enable user.
ACL SETUSER alice on

# Set password.
ACL SETUSER alice >password

# Allow only read commands.
ACL SETUSER alice +@read

# Deny write commands.
ACL SETUSER alice -@write

# Allow all commands.
ACL SETUSER alice +@all

# Restrict to key pattern.
ACL SETUSER alice ~orders:*

# Allow all key patterns.
ACL SETUSER alice ~*

# Allow all channels (Pub/Sub).
ACL SETUSER alice &*

# Full read-only user example.
ACL SETUSER readonly-svc on >securepass ~* &* +@read -@admin
```

## Rename Dangerous Commands (Legacy)

```conf
rename-command FLUSHALL ""
rename-command FLUSHDB ""
rename-command DEBUG ""
rename-command KEYS ""
rename-command CONFIG "CONFIG-HIDDEN-XYZ"
```

## Network Security Config

```conf
bind 127.0.0.1 10.0.0.5
protected-mode yes
requirepass your-strong-password
```

## TLS Config

```conf
port 0
tls-port 6380
tls-cert-file /etc/redis/redis.crt
tls-key-file /etc/redis/redis.key
tls-ca-cert-file /etc/redis/ca.crt
tls-auth-clients yes
```

```bash
# Connect with TLS.
redis-cli --tls --cert redis.crt --key redis.key --cacert ca.crt -p 6380 PING
```
