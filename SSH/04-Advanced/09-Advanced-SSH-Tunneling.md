# S9 — Advanced SSH: Port Forwarding & Tunneling

---

## Core Concept: SSH as a Secure Pipe

> SSH can forward **any TCP traffic** through its encrypted tunnel. This means you can secure any protocol (HTTP, database, Redis, etc.) that has no encryption of its own.

```
Without tunnel:    Your App ──── plain HTTP ────► Remote Service   (exposed!)
With SSH tunnel:   Your App ──── localhost ──► SSH encrypted ──► Remote Service
```

---

## Three Types of Port Forwarding

### 1. Local Port Forwarding (-L)

> "Forward a **local** port to a remote service."

```bash
ssh -L [local_port]:[remote_host]:[remote_port] user@ssh_server
```

**Analogy:** You ask the SSH server to be your proxy — traffic you send to your local port comes out at the remote destination.

```
YOUR MACHINE          SSH SERVER              TARGET SERVICE
localhost:8080  ────► (ssh encrypted) ────► localhost:3000 on server
                                          OR internal-db:5432
```

**Examples:**

```bash
# Access a remote web app running on port 3000 (firewall blocks it)
ssh -L 8080:localhost:3000 user@server
# → open http://localhost:8080 → hits server's localhost:3000

# Access a remote database (MySQL not exposed to internet)
ssh -L 3307:localhost:3306 user@db-server
# → mysql -h 127.0.0.1 -P 3307 -u root -p  → connects to remote MySQL

# Access a service on an INTERNAL host (via SSH server as jump)
ssh -L 8080:internal-service.local:80 user@bastion
# → http://localhost:8080 → hits internal-service.local:80 (through bastion)

# Background tunnel (stays alive without shell)
ssh -fN -L 5432:localhost:5432 user@prod-server
#   -f = background
#   -N = no remote command
```

---

### 2. Remote Port Forwarding (-R)

> "Expose a **local** service to the remote server."

```bash
ssh -R [remote_port]:[local_host]:[local_port] user@ssh_server
```

**Analogy:** You punch a hole in the remote server — anyone connecting to the remote port gets forwarded to your local machine.

```
YOUR MACHINE          SSH SERVER              OUTSIDE WORLD
localhost:8080  ◄──── (ssh encrypted) ◄──── server:9090
                                             ↑
                                        (clients connect here)
```

**Examples:**

```bash
# Expose your local dev server to the internet (via remote server)
ssh -R 9090:localhost:3000 user@public-server
# → Anyone hitting public-server:9090 → gets your localhost:3000

# Share localhost API for testing/webhooks
ssh -R 8080:localhost:8080 user@public-server

# Enable remote access to your laptop
ssh -R 2222:localhost:22 user@jumphost
# → ssh -p 2222 user@jumphost → reaches YOUR machine
```

> 💡 Use case: Webhook testing without ngrok — expose local webhook receiver to GitHub/Stripe via your VPS.

---

### 3. Dynamic Port Forwarding (-D) — SOCKS Proxy

> "Create a local **SOCKS proxy** that routes all traffic through the SSH server."

```bash
ssh -D [local_port] user@ssh_server
```

**Analogy:** The SSH server becomes your VPN exit node. Configure your browser/app to use the SOCKS proxy.

```
YOUR BROWSER/APP
     │
     │ SOCKS5 → localhost:1080
     │
     ▼
  ssh client
     │
     │ SSH encrypted tunnel
     │
     ▼
  SSH SERVER
     │
     │ exits here to internet
     ▼
  ANY WEBSITE / SERVICE
```

**Examples:**

```bash
# Create SOCKS5 proxy on local port 1080
ssh -D 1080 user@remote-server

# Background + no shell
ssh -fN -D 1080 user@remote-server

# Configure browser to use: SOCKS5 Host=127.0.0.1 Port=1080
# All browser traffic now routes through remote-server
```

**curl via SOCKS proxy:**
```bash
curl --socks5-hostname 127.0.0.1:1080 http://internal-service/
```

---

## Comparison Table

| Type | Flag | Direction | Use Case |
|------|------|-----------|----------|
| **Local** | `-L` | Local → Remote | Access remote DB/service from local |
| **Remote** | `-R` | Remote → Local | Expose local app to remote/internet |
| **Dynamic** | `-D` | Local → Any (SOCKS) | Proxy/VPN-like routing |

---

## Securing a Local HTTP API via SSH Tunnel

**Scenario:** Your API runs on prod-server:8080 (no public access). You need to call it from your laptop.

```bash
# Step 1: Create tunnel
ssh -fN -L 8080:localhost:8080 user@prod-server

# Step 2: Call API locally — goes through encrypted SSH tunnel
curl http://localhost:8080/api/health
curl -X POST http://localhost:8080/api/deploy -d '{"version":"1.2"}'

# Step 3: Kill tunnel when done
pkill -f "ssh -fN -L 8080"
```

**ASCII Diagram:**

```
YOUR LAPTOP                  PROD SERVER
curl localhost:8080           
      │                           
      │──► SSH Client ────────────► SSH Server (port 22)
                      [encrypted]         │
                                          │──► localhost:8080 (API)
                                          │◄── API Response
      │◄── SSH Client ◄────────────────── │
      │
 Response shown
```

---

## Keep Tunnel Alive (Production Pattern)

```bash
# ~/.ssh/config — auto-reconnect tunnel
Host db-tunnel
  HostName prod-db.internal
  User tunnel-user
  LocalForward 5432 localhost:5432
  ServerAliveInterval 30
  ServerAliveCountMax 3
  ExitOnForwardFailure yes
```

Or use `autossh` for robust auto-reconnect tunnels:
```bash
autossh -M 20000 -fN -L 5432:localhost:5432 user@db-server
```

---

## Multi-Hop: SSH Through Bastion

```bash
# Direct jump (SSH 7.3+)
ssh -J bastion-user@bastion.host ubuntu@10.0.0.5

# Nested tunnel: forward DB port through bastion to internal DB
ssh -J ec2-user@bastion -L 5432:internal-db.private:5432 ubuntu@app-server
# → psql -h localhost -p 5432 → hits internal-db through 2 hops
```
