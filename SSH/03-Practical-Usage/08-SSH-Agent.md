# S8 — SSH Agent

---

## The Problem SSH Agent Solves

```
Without agent:
  ssh user@server1   → "Enter passphrase for key:"  [type passphrase]
  ssh user@server2   → "Enter passphrase for key:"  [type again]
  scp file user@s3:/  → "Enter passphrase for key:"  [type again]
  [painful every time]

With agent:
  eval "$(ssh-agent -s)"
  ssh-add ~/.ssh/id_ed25519   → "Enter passphrase:"  [type ONCE]
  
  ssh user@server1   → [instant — agent signs challenge]
  ssh user@server2   → [instant]
  scp file user@s3:/ → [instant]
```

---

## How SSH Agent Works

```
┌─────────────────────────────────────────────────────────┐
│                    YOUR MACHINE                          │
│                                                          │
│  ssh-agent (daemon)                                      │
│  ┌─────────────────────┐                                │
│  │ In-memory key store  │                               │
│  │  id_ed25519 (loaded) │   ◄── ssh-add loads keys     │
│  │  github_key  (loaded)│                               │
│  └─────────────────────┘                                │
│          │                                               │
│          │ Unix socket: SSH_AUTH_SOCK                    │
│          │                                               │
│   ssh client ────────────► Server challenge             │
│   "agent, sign this"        ◄── sign with private key   │
│   ◄── signature ────────────── send proof               │
│                                                          │
│   Private key NEVER leaves the agent                     │
└─────────────────────────────────────────────────────────┘
```

---

## Basic Agent Commands

```bash
# Start agent (outputs env vars — MUST eval to capture them)
eval "$(ssh-agent -s)"
# Output: Agent pid 1234

# Add a key (type passphrase once)
ssh-add ~/.ssh/id_ed25519
ssh-add ~/.ssh/github_key
ssh-add ~/.ssh/my-aws-key.pem

# Add with time limit (expires after 4 hours)
ssh-add -t 14400 ~/.ssh/id_ed25519

# List loaded keys
ssh-add -l          # short: fingerprint + filename
ssh-add -L          # full: public key content

# Remove a specific key
ssh-add -d ~/.ssh/id_ed25519

# Remove ALL keys from agent
ssh-add -D

# Check agent is running
echo $SSH_AUTH_SOCK   # non-empty = agent running
```

---

## macOS: Keychain Integration

macOS auto-starts an agent and stores passphrases in the system Keychain:

```bash
# Add to agent + save passphrase to macOS Keychain
ssh-add --apple-use-keychain ~/.ssh/id_ed25519

# In ~/.ssh/config — auto-load from Keychain on use:
Host *
  AddKeysToAgent yes
  UseKeychain yes
  IdentityFile ~/.ssh/id_ed25519
```

With this config, you **never** need to manually `ssh-add` — keys load automatically on first use.

---

## Linux: Auto-Start Agent in Shell Profile

```bash
# Add to ~/.bashrc or ~/.zshrc
if [ -z "$SSH_AUTH_SOCK" ]; then
  eval "$(ssh-agent -s)"
  ssh-add ~/.ssh/id_ed25519 2>/dev/null
fi
```

Or use `ssh-agent` socket persistence tools like `keychain`:
```bash
# keychain (Gentoo/general Linux)
eval $(keychain --eval --quiet id_ed25519)
```

---

## Agent Forwarding (Multi-Hop SSH)

Use your local private key when SSH-ing from one server to another:

```bash
# Method 1: Flag
ssh -A user@bastion

# Method 2: Config (preferred)
Host bastion
  ForwardAgent yes
```

```
YOUR MACHINE                BASTION              INTERNAL SERVER
    │                           │                      │
    │─── ssh -A bastion ───────►│                      │
    │   [agent forwarded]        │─── ssh internal ────►│
    │◄─── sign challenge ────────│◄── challenge ─────────│
    │──── signature ────────────►│──── signature ───────►│
    │                            │                      │
    │   Your KEY stays on YOUR machine throughout
```

> ⚠️ Security note: Only forward agent to servers **you trust**. A compromised server with `ForwardAgent yes` can use your keys to authenticate elsewhere.

---

## SSH Agent vs Keychain vs 1Password SSH Agent

| Option | Platform | Best For |
|--------|----------|----------|
| `ssh-agent` | Any Unix | Standard, widely compatible |
| macOS Keychain | macOS | Desktop dev, survives reboots |
| `gpg-agent` | Linux/macOS | If using GPG-backed SSH keys |
| 1Password SSH Agent | macOS/Win/Linux | Teams, biometric unlock, key mgmt |
| HashiCorp Vault | Enterprise | Short-lived certs, central mgmt |

---

## Troubleshooting Agent

```bash
# Agent not running?
echo $SSH_AUTH_SOCK    # Empty = no agent
eval "$(ssh-agent -s)" # Start it

# Key not loaded?
ssh-add -l             # Lists loaded keys; "The agent has no identities" = empty
ssh-add ~/.ssh/id_ed25519

# Wrong agent socket?
export SSH_AUTH_SOCK=$(ls /tmp/ssh-*/agent.* 2>/dev/null | head -1)

# Check if agent forwarding works
ssh -A user@host "ssh-add -l"  # Should list YOUR local keys
```
