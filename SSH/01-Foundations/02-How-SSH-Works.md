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

## Real-World Story: Arun SSHes into His AWS EC2 Server

### Setup — What Arun Has

```
ARUN'S LAPTOP (macOS)
  IP address     : 103.45.67.89          ← his home broadband public IP
  Private key    : ~/.ssh/my-ec2-key.pem ← downloaded from AWS when he created the instance
  Public key     : already installed on the EC2 by AWS at launch

AWS EC2 INSTANCE (Ubuntu 22.04)
  Public IP      : 54.12.34.56           ← assigned by AWS (changes on restart unless Elastic IP)
  Public DNS     : ec2-54-12-34-56.compute-1.amazonaws.com  ← AWS hostname for the same IP
  Listening port : 22                    ← sshd daemon is running and waiting here
  Default user   : ubuntu                ← the built-in Linux user on Ubuntu AMIs
  authorized_keys: /home/ubuntu/.ssh/authorized_keys
                   (contains Arun's public key — AWS put it there at launch)
```

> Arun now opens Terminal on his MacBook and types one command.
> Here is **exactly** what happens, second by second.

---

### STEP 1 — "Dial the number" (TCP Connection)

**What Arun types:**
```
$ ssh -i ~/.ssh/my-ec2-key.pem ubuntu@54.12.34.56
```

**Breaking down that command:**
```
ssh                        → the SSH client program
-i ~/.ssh/my-ec2-key.pem   → -i means "identity file" = use THIS private key
ubuntu                     → the Linux username to log in as on the remote machine
@                          → separator between username and host
54.12.34.56                → the public IP address of the EC2 server
                             (you could also write: ubuntu@ec2-54-12-34-56.compute-1.amazonaws.com
                              — same thing, just the DNS hostname for that IP)
```

**What happens under the hood:**
```
Arun's laptop                               EC2 server: 54.12.34.56
      │                                              │
      │── TCP SYN ──────────────── port 22 ────────►│
      │                                              │  sshd sees a new connection
      │◄── TCP SYN-ACK ───────────────────────────── │  on port 22 and responds
      │── TCP ACK ───────────────────────────────────►│
      │                                              │
      TCP connection is now open. No encryption yet.
      Think of it as: the phone is ringing and someone picked up.
      Nobody has said anything private yet.
```

---

### STEP 2 — "Hello, what version are you?" (Protocol Handshake)

**What happens:**
```
EC2 server sends a plain-text greeting:   "SSH-2.0-OpenSSH_8.9p1 Ubuntu-3ubuntu0.6"
Arun's laptop replies:                    "SSH-2.0-OpenSSH_9.7"

Both now know: we are both speaking SSH version 2.
```

**Why this matters:** SSH-1 had serious flaws. SSH-2 is the only version in use today.
This step is like two people picking up the phone and confirming:
*"Do you speak English?"* — *"Yes, I speak English."* — Now we proceed.

---

### STEP 3 — "Show me your ID card" (Server Identity Verification)

This step answers: **"Am I really talking to MY EC2 server, or has someone hijacked the connection?"**

**The EC2 server sends its Host Key — its permanent identity:**
```
Host key type       : ED25519
Host public key     : ssh-ed25519 AAAAC3NzaC1lZDI1NTE5AAAAI... (long string)
Fingerprint (SHA256): SHA256:xK9mB3rPqZ2Vu7Yw1Ld8NfHcT4oE5sR6gJ+Ap0QwXk=
```

**Arun's laptop checks `~/.ssh/known_hosts`:**

```
SCENARIO A — First time ever connecting to 54.12.34.56:
  known_hosts has NO entry for this IP.
  SSH cannot verify if this server is legitimate.
  It shows Arun this prompt:

  The authenticity of host '54.12.34.56 (54.12.34.56)' can't be established.
  ED25519 key fingerprint is SHA256:xK9mB3rPqZ2Vu7Yw1Ld8NfHcT4oE5sR6gJ+Ap0QwXk=.
  This key is not known by any other names.
  Are you sure you want to continue connecting (yes/no/[fingerprint])?

  → Arun types: yes
  → SSH saves this fingerprint to ~/.ssh/known_hosts:

  Contents of ~/.ssh/known_hosts after typing yes:
  54.12.34.56 ssh-ed25519 AAAAC3NzaC1lZDI1NTE5AAAAI...long-key...
  (one line — the IP + key type + public key)

SCENARIO B — Arun connects again next week:
  SSH reads ~/.ssh/known_hosts, finds 54.12.34.56 already stored.
  Compares stored fingerprint vs what server just sent.
  They match → proceeds silently. No prompt.

SCENARIO C — Server was rebuilt (or MITM attack!):
  SSH reads known_hosts, finds old fingerprint for 54.12.34.56.
  New server sends a DIFFERENT fingerprint.
  SSH shows a loud red warning:

  @@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@
  @ WARNING: REMOTE HOST IDENTIFICATION HAS CHANGED! @
  @@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@
  Someone could be eavesdropping on you right now (man-in-the-middle attack)!

  Fix if server was legitimately rebuilt:
  $ ssh-keygen -R 54.12.34.56   → deletes old entry from known_hosts
  $ ssh -i key.pem ubuntu@54.12.34.56  → accepts new fingerprint fresh
```

---

### STEP 4 — "Let's agree on a secret without saying it aloud" (Key Exchange)

This is the cleverest part. Arun and the EC2 need to agree on an encryption key
to protect their conversation — but they cannot safely shout the key across the internet.

**The Diffie-Hellman colour analogy:**
```
Imagine Arun and EC2 agree on a PUBLIC base colour: Yellow (everyone can see this)

Arun picks a SECRET colour: Red     (never revealed)
EC2  picks a SECRET colour: Blue    (never revealed)

Arun mixes Yellow + Red  → sends Orange to EC2    (Orange is visible to hackers)
EC2  mixes Yellow + Blue → sends Green to Arun    (Green is visible to hackers)

Arun takes Green  + his secret Red  → gets BROWN
EC2  takes Orange + its secret Blue → gets BROWN   ← same result!

BROWN = the shared session key. A hacker saw Yellow, Orange, Green — but
cannot compute BROWN without one of the secret colours. Math makes this impossible.
```

**What actually happens (curve25519-sha256):**
```
Arun's laptop:  generates temporary private value  a = [random 256-bit number]
                computes public value              A = curve25519(a)
                sends A ─────────────────────────────────────────────► EC2

EC2 server:     generates temporary private value  b = [random 256-bit number]
                computes public value              B = curve25519(b)
                sends B ◄──────────────────────────────────────────── EC2

Arun's laptop:  shared_secret = curve25519(a, B)
EC2 server:     shared_secret = curve25519(b, A)
                Both arrive at the SAME shared_secret
                This shared_secret is used to derive the AES-256 session key.

A hacker who captured A and B cannot compute shared_secret. Math wins.
```

---

### STEP 5 — "Sealed envelope mode ON" (Symmetric Encryption Begins)

```
Session key is now derived: AES-256-GCM key agreed by both sides.
From this exact moment:

  Everything Arun types ──► AES-256 encrypted ──► travels over internet ──► decrypted on EC2
  Everything EC2 outputs ──► AES-256 encrypted ──► travels over internet ──► decrypted on Arun's screen

A hacker capturing packets now sees:
  Before step 5:  SSH-2.0-OpenSSH_9.7   (readable version strings)
  After  step 5:  ■▓░▒█▓■░▒█▓░▒░▒█▓    (pure encrypted noise)

This is the "sealed envelope". Even if someone records every byte of
Arun's session, they cannot read a single command or output.
```

---

### STEP 6 — "Prove it's really you, Arun" (Authentication)

The tunnel is encrypted. Now the server needs to confirm Arun is who he claims.
Since Arun used `-i ~/.ssh/my-ec2-key.pem`, key-based auth runs:

```
ON THE EC2 SERVER — what it checks:
  /home/ubuntu/.ssh/authorized_keys contains:
  ssh-ed25519 AAAAC3NzaC1lZDI1NTE5AAAAIBq7...Arun's-public-key...  arun@laptop

AUTH CHALLENGE-RESPONSE (all happening inside the encrypted tunnel):
  EC2:  "I'm going to generate a random 32-byte challenge: 8f3a2c..."
        I'll encrypt it with Arun's PUBLIC key from authorized_keys.
        Only the holder of the matching PRIVATE key can decrypt it.
        Encrypted challenge ──────────────────────────────► Arun's laptop

  Arun's laptop:
        Uses my-ec2-key.pem (the private key) to sign the challenge.
        Signs it → sends signature back ────────────────────► EC2

  EC2:  Verifies the signature using the PUBLIC key it already has.
        Signature valid → this is definitely Arun's laptop. ✅
        Access granted.

WHY THIS IS SECURE:
  The private key (my-ec2-key.pem) NEVER left Arun's laptop.
  No password was sent. No secret crossed the internet.
  Even if a hacker captured the entire session, they get nothing usable.
```

---

### STEP 7 — "You're in. The shell is yours." (Interactive Session)

**What Arun sees on his terminal:**
```
Warning: Permanently added '54.12.34.56' (ED25519) to the list of known hosts.
Welcome to Ubuntu 22.04.3 LTS (GNU/Linux 6.2.0-1012-aws x86_64)

  System information as of Mon Jun 30 10:23:41 UTC 2026
  System load:  0.08              Processes:             98
  Usage of /:   14.2% of 7.57GB  Users logged in:       0
  Memory usage: 18%               IPv4 address for eth0: 10.0.1.45

ubuntu@ip-54-12-34-56:~$
```

**What that prompt means:**
```
ubuntu            → the Linux username Arun logged in as
@                 → separator
ip-54-12-34-56    → the EC2 server's internal hostname (AWS auto-names it from the IP)
:                 → separator
~                 → current directory (~ means /home/ubuntu)
$                 → you are a regular user (# would mean root)
```

**From this point:**
```
Arun types:  ls -la /var/log
             ↓ encrypted by AES-256 → sent to EC2
EC2 runs:    ls -la /var/log
             ↓ output encrypted by AES-256 → sent back
Arun sees:   total 128
             drwxrwxr-x  9 root   syslog  4096 Jun 30 10:00 .
             ...

Every single keystroke and every single character of output
travels through the encrypted AES-256 tunnel. 🔒
```

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
