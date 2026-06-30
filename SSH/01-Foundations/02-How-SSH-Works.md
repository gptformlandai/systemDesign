# S2 — How SSH Works (Under the Hood)

---

## Client vs Server Model

```
┌─────────────┐                        ┌─────────────┐
│  SSH CLIENT │                        │  SSH SERVER │
│  (your Mac) │ ──── TCP port 22 ────► │ (EC2/VPS)   │
│             │ ◄─────────────────────  │  (sshd)     │
└─────────────┘                        └─────────────┘

Client = ssh (command you run)
Server = sshd (daemon running on remote machine)
```

- `sshd` = SSH Daemon — listens on port 22, manages incoming sessions
- `ssh`  = SSH client — initiates the connection

---

## Step-by-Step Connection Flow

```
STEP 1: TCP Handshake
  Client → Server: SYN
  Server → Client: SYN-ACK
  Client → Server: ACK
  [TCP connection established on port 22]

STEP 2: Protocol Version Exchange
  Server: "SSH-2.0-OpenSSH_9.0"
  Client: "SSH-2.0-OpenSSH_9.3"
  [Both agree on SSH-2 protocol]

STEP 3: Server Identity Check (Host Key)
  Server sends its public host key + fingerprint
  Client checks ~/.ssh/known_hosts
    → First time: "Are you sure you want to connect? (yes/no)"
    → Known host: silently verified
  [Prevents Man-in-the-Middle attacks]

STEP 4: Key Exchange (Diffie-Hellman / ECDH)
  Client and server negotiate algorithm (e.g., curve25519-sha256)
  They exchange values → derive a SHARED SESSION KEY
  Neither side ever sends the full key over the wire
  [This is asymmetric crypto used to bootstrap symmetric]

STEP 5: Symmetric Encryption Begins
  All further traffic encrypted with session key (e.g., AES-256)
  Fast, efficient — symmetric encryption for bulk data
  [Now the tunnel is secure]

STEP 6: Authentication
  Client proves identity:
    → Password: sends encrypted password
    → Key-based: server sends a challenge, client signs with private key
  Server grants or denies access

STEP 7: Shell / Command Execution
  Encrypted shell session opens
  You type → encrypted → sent → decrypted → executed → output encrypted → sent back
```

---

## Three Cryptographic Pillars

### 1. Asymmetric Encryption (Public-Key Crypto)
- Used during: **key exchange + authentication**
- Two keys: public key (shareable) + private key (secret)
- What one key encrypts, only the other can decrypt
- Algorithms: RSA, ECDSA, Ed25519

```
Private key signs → Server verifies with Public key
Server encrypts  → Client decrypts with Private key
```

### 2. Symmetric Encryption
- Used during: **bulk data transfer** (the active session)
- One shared key encrypts + decrypts
- Faster than asymmetric — suited for streaming data
- Algorithms: AES-128-CTR, AES-256-GCM, ChaCha20-Poly1305

### 3. Hashing (MAC — Message Authentication Code)
- Used for: **data integrity**
- Every message includes a hash (HMAC)
- Server and client verify each packet wasn't tampered with
- Algorithms: HMAC-SHA2-256, HMAC-SHA2-512

---

## Why Both Asymmetric AND Symmetric?

```
Asymmetric is SLOW but SAFE for key exchange.
Symmetric is FAST but needs a shared secret first.

SSH Solution:
  1. Use asymmetric (ECDH) to securely exchange a session key
  2. Derive shared symmetric key from that exchange
  3. Use symmetric key for all session traffic
  ✅ Best of both worlds
```

---

## How Trust Is Established

```
FIRST CONNECT:
  Server presents fingerprint
  You see: "The authenticity of host 'x.x.x.x' can't be established.
            ECDSA key fingerprint is SHA256:abc123..."
  → Type 'yes' → fingerprint stored in ~/.ssh/known_hosts

SUBSEQUENT CONNECTS:
  SSH checks fingerprint against ~/.ssh/known_hosts
  If it matches → silent proceed
  If it CHANGED → 🚨 WARNING: MITM possible!

VERIFY FINGERPRINT (safe channel):
  ssh-keyscan host | ssh-keygen -l -f -
  Compare with what server admin tells you out-of-band
```

---

## Encryption Algorithm Negotiation

Both sides advertise their supported algorithms. They pick the best match:

```
Key Exchange:     curve25519-sha256 > ecdh-sha2-nistp256 > diffie-hellman
Host Key Type:    ed25519 > ecdsa > rsa
Cipher:           aes256-gcm > aes128-gcm > chacha20-poly1305
MAC:              hmac-sha2-256 > hmac-sha2-512
```

Modern default (OpenSSH 8+): curve25519 + ed25519 + AES-256-GCM

---

## ASCII: Full SSH Handshake

```
CLIENT                              SERVER
  │                                   │
  │──── TCP SYN ─────────────────────►│
  │◄─── TCP SYN-ACK ──────────────────│
  │──── TCP ACK ─────────────────────►│
  │                                   │
  │──── SSH version string ──────────►│
  │◄─── SSH version string ───────────│
  │                                   │
  │◄─── Server host key + algos ──────│
  │──── Client selects algos ────────►│
  │                                   │
  │   [ECDH key exchange rounds]      │
  │──── client DH public value ──────►│
  │◄─── server DH public value ───────│
  │                                   │
  │   [Both derive same session key]  │
  │══════ ENCRYPTED FROM HERE ════════│
  │                                   │
  │──── auth request ────────────────►│
  │◄─── auth success/failure ─────────│
  │                                   │
  │══════ INTERACTIVE SESSION ════════│
```
