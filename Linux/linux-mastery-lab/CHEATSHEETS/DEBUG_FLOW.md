# Linux Debug Flow Cheatsheet

## Universal Flow

```text
symptom -> scope -> command evidence -> OS layer -> likely cause -> mitigation -> validation -> prevention
```

## Service Down

```text
systemctl -> journalctl -> unit file -> process -> port -> config -> dependency
```

## High Load

```text
uptime -> top -> CPU/memory/disk/network -> top process -> logs -> recent change
```

## Network Failure

```text
DNS -> route -> listener -> firewall -> TLS -> HTTP/app response
```

## Permission Denied

```text
process user -> path directories -> file mode -> ACL -> mount option -> SELinux/AppArmor
```