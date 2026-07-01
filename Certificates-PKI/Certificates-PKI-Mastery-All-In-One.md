# Certificates, CA, PKI, and TLS: Beginner to Pro Mastery

> **Scope:** X.509 certificates from first principles → PKI trust chains → TLS handshake → how browsers verify → creating and rotating certs → cert-manager → mTLS → Vault PKI → production patterns → interview Q&A → cheatsheet.

---

## Table of Contents

1. [Intuition: The Passport Analogy](#1-intuition)
2. [The Core Problem: Identity on the Internet](#2-the-core-problem)
3. [Asymmetric Cryptography Fundamentals](#3-asymmetric-cryptography)
4. [What Is an X.509 Certificate?](#4-x509-certificate)
5. [PKI Architecture: Root CA, Intermediate CA, Leaf](#5-pki-architecture)
6. [TLS Handshake: Step by Step](#6-tls-handshake)
7. [How the Internet Verifies Certificates](#7-how-internet-verifies)
8. [OCSP, CRL, and Certificate Transparency](#8-ocsp-crl-ct)
9. [Inside a Certificate: X.509 Fields](#9-certificate-fields)
10. [Certificate Types: DV, OV, EV, Wildcard, SAN](#10-certificate-types)
11. [Certificate File Formats: PEM, DER, PFX, JKS](#11-file-formats)
12. [Creating Certificates with OpenSSL](#12-creating-certs)
13. [Let's Encrypt and the ACME Protocol](#13-lets-encrypt)
14. [AWS Certificate Manager (ACM)](#14-aws-acm)
15. [Certificate Rotation](#15-rotation)
16. [cert-manager in Kubernetes](#16-cert-manager)
17. [mTLS: Mutual TLS](#17-mtls)
18. [HashiCorp Vault PKI Engine](#18-vault-pki)
19. [Production Issues and Anti-Patterns](#19-production-issues)
20. [Interview Q&A: Beginner to Pro](#20-interview-qa)
21. [Cheatsheet: All Commands](#21-cheatsheet)

---

## 1. Intuition

### The Passport Analogy

When you travel to another country, a border agent doesn't know you. But they trust your **passport** — because it was issued by a **government** (an authority they already trust). The passport proves who you are, and the stamps and signatures prevent forgery.

On the internet, the same problem exists:
- You connect to `https://yourbank.com`
- How do you know it's actually your bank and not a hacker's server?
- You need something that says "I, DigiCert (a trusted authority), certify that this server IS yourbank.com."

That "something" is a **digital certificate**.

```text
Certificate = digital passport for a server (or a person, or a device)

                 ┌─────────────────────────────────────┐
                 │  CERTIFICATE for yourbank.com        │
                 │  Subject:  yourbank.com              │
                 │  Issuer:   DigiCert Global CA G2     │
                 │  Valid:    2025-01-01 → 2026-01-01   │
                 │  Public Key: [big number]            │
                 │  Signature: [DigiCert's signature]   │
                 └─────────────────────────────────────┘
```

Your browser already has DigiCert's public key built in. It checks the signature. If it matches, it knows the certificate is genuine.

---

## 2. The Core Problem

### Two Problems TLS + Certificates Solve

**Problem 1: Authentication — Is this really the server I think it is?**

Without certificates, a hacker could intercept your connection to `bank.com` and pretend to be the bank. This is a **Man-in-the-Middle (MITM)** attack.

Certificates solve this by binding a **domain name** to a **public key**, signed by a trusted authority.

**Problem 2: Encryption — Can anyone read the data in transit?**

Even if you know who you're talking to, data traveling over the internet passes through many routers. Without encryption, anyone on the path can read your passwords and credit card numbers.

TLS (Transport Layer Security) solves this by using the certificate to establish an encrypted channel.

```text
Without TLS:
  Browser → PLAINTEXT → ISP Router → NSA → Hacker → Server
  
With TLS:
  Browser → ENCRYPTED GIBBERISH → (intermediate nodes cannot read) → Server
             ↑
         Only browser + server hold the decryption keys
```

---

## 3. Asymmetric Cryptography

Certificates are built on **asymmetric (public-key) cryptography**. This is the math that makes everything work.

### Key Pair Concept

```text
Every participant generates a KEY PAIR:
  Private Key: a large random number — NEVER shared with anyone
  Public Key:  derived from private key — safe to share with everyone

Mathematical property:
  Message encrypted with PUBLIC KEY  → can ONLY be decrypted with PRIVATE KEY
  Message signed with PRIVATE KEY    → can be VERIFIED with PUBLIC KEY
```

### Why Two Keys?

With **symmetric encryption** (one shared key):
- Alice and Bob need to agree on a secret key first
- Problem: how do you securely share the key over an insecure network?

With **asymmetric encryption**:
- Bob publishes his **public key** to the world
- Alice encrypts with Bob's public key → only Bob can decrypt (only Bob has the private key)
- No secret key exchange needed

```text
Bob:  Private Key = secret (stays on Bob's server)
      Public Key  = shared with everyone (in the certificate)

Alice sends message to Bob:
  1. Alice gets Bob's public key (from his certificate)
  2. Alice encrypts message with Bob's public key
  3. Only Bob's private key can decrypt it
  4. Even the person who delivered the message can't read it
```

### Digital Signatures

```text
Bob signs a document:
  1. Bob hashes the document: hash(document) → fingerprint
  2. Bob encrypts the fingerprint with his PRIVATE KEY → signature
  3. Sends: document + signature

Anyone verifies:
  1. Decrypt signature with Bob's PUBLIC KEY → gets fingerprint
  2. Hash the document themselves → compute fingerprint
  3. If fingerprints match → document is genuine, signed by Bob
  4. If anyone tampered with the document → fingerprints won't match
```

A certificate is exactly this: a document (your server's public key + domain name) signed by a Certificate Authority's private key.

---

## 4. What Is an X.509 Certificate?

X.509 is the **international standard** (RFC 5280) that defines the format of a digital certificate.

```text
An X.509 certificate contains:
  1. Version (always 3 for modern certs)
  2. Serial Number (unique ID from the CA)
  3. Subject (who this cert is for: CN=yourbank.com)
  4. Issuer  (who signed this cert: CN=DigiCert Global CA G2)
  5. Validity Period (notBefore, notAfter)
  6. Subject Public Key Info (the server's public key algorithm + key)
  7. Extensions:
       - Subject Alternative Names (SANs): additional domains
       - Key Usage: what the key is allowed to do
       - Extended Key Usage: TLS Server Auth, TLS Client Auth, etc.
       - Subject Key Identifier
       - Authority Key Identifier
       - CRL Distribution Points: where to check revocation
       - OCSP URLs
       - Certificate Policies
  8. CA Signature: the entire above content hashed and signed by issuer
```

To view a certificate in the browser: click the padlock → "Certificate" → Details.

---

## 5. PKI Architecture

### PKI = Public Key Infrastructure

PKI is the entire system of CAs, policies, software, hardware, and procedures that manage digital certificates.

### The Certificate Hierarchy

```text
ROOT CA
  │   ← Root CA certificate is self-signed (trusts itself)
  │   ← Installed in every OS/browser trust store
  │
  ├── INTERMEDIATE CA 1 (signed by Root CA)
  │     │   ← Why intermediate? So Root CA private key stays OFFLINE
  │     │   ← If intermediate is compromised, you revoke it without touching root
  │     │
  │     ├── Leaf Cert: yourbank.com (signed by Intermediate CA 1)
  │     ├── Leaf Cert: shop.yourbank.com
  │     └── Leaf Cert: api.yourbank.com
  │
  └── INTERMEDIATE CA 2 (signed by Root CA)
        │
        └── Leaf Cert: mail.yourbank.com
```

### Root CAs

Root CAs are the absolute trust anchors. Examples:
- DigiCert Global Root G2
- ISRG Root X1 (Let's Encrypt's root)
- Amazon Root CA 1 (AWS ACM)
- GlobalSign Root R1
- Comodo AAA Services Root
- Microsoft RSA Root Certificate Authority 2017

Root CA certificates are **baked into** your operating system and browser:
- macOS: Keychain Access → System Roots
- Windows: certmgr.msc → Trusted Root Certification Authorities
- Linux: `/etc/ssl/certs/`, `/usr/share/ca-certificates/`
- Chrome/Firefox: each maintains their own trust store

### Why Intermediate CAs?

```text
Root CA private key is the crown jewel. If it's compromised:
  → Every certificate ever signed is untrusted
  → You'd have to remove the root from every device on earth

Solution: keep Root CA private key OFFLINE (HSM, air-gapped)
  → Root CA signs ONLY intermediate CA certs
  → Intermediate CA (online, automated) signs end-entity leaf certs
  → If intermediate is compromised → revoke that intermediate cert only
  → Root stays valid
```

### Self-Signed Certificates

A self-signed cert is signed by its own private key (no CA involved):

```text
Subject: my-company-internal-service
Issuer:  my-company-internal-service  ← same as subject = self-signed

Use cases:
  ✓ Development / local testing
  ✓ Internal corporate services (where you can install your own root)
  ✓ Internal Kubernetes clusters (you control all clients)
  
  ✗ Public-facing services (browser will show "Not Secure" warning)
  ✗ Any service where clients can't be configured to trust your root
```

---

## 6. TLS Handshake

The TLS handshake is the negotiation that happens before any HTTP data flows. It establishes: identity, encryption algorithms, and a shared session key.

### TLS 1.3 Handshake (Simplified)

```text
Client (browser)                            Server (yourbank.com)
─────────────────────────────────────────────────────────────────

1. ClientHello →
   - TLS version supported
   - Cipher suites supported
   - Key exchange parameters (ECDH key share)
   - SNI: "I want yourbank.com"

                                  ← 2. ServerHello
                                     - Selected cipher suite
                                     - Server's ECDH key share

                                  ← 3. Certificate (chain)
                                     - Leaf cert for yourbank.com
                                     - Intermediate CA cert

                                  ← 4. CertificateVerify
                                     - Signature proving server holds private key

                                  ← 5. Finished (encrypted)

6. Client validates certificate:
   - Check signature chain up to trusted root
   - Check hostname matches CN/SANs
   - Check not expired
   - Check not revoked (OCSP)

7. Finished (encrypted) →
   
8. Application data flows (all encrypted with derived session key)
```

### SNI (Server Name Indication)

```text
Problem: One IP address, 100 different domain names on the same server.
         Before TLS negotiation, the server doesn't know which certificate to present.

Solution: SNI — the client sends the domain name ("yourbank.com") in the ClientHello,
          BEFORE the certificate is presented.
          Server picks the right certificate for that domain.

Note: SNI is sent unencrypted in TLS 1.2 and 1.3 (without ECH).
      ISPs and network observers can see WHICH domain you're connecting to,
      even if they can't read the content.
      → ECH (Encrypted Client Hello) encrypts SNI in TLS 1.3 (still being standardized)
```

### What Happens After the Handshake?

After the handshake, both sides derive a **shared symmetric session key** (using ECDH). All subsequent data is encrypted with this symmetric key — much faster than asymmetric encryption.

```text
Asymmetric encryption: slow (RSA/ECDSA math)  → used ONLY during handshake
Symmetric encryption:  fast (AES-GCM)         → used for all data
```

---

## 7. How the Internet Verifies Certificates

When your browser receives a certificate, it performs 6 checks:

### Step 1: Chain of Trust

```text
Browser receives: [leaf cert] + [intermediate cert]

Checks:
1. Is the leaf cert signed by the intermediate CA?
   → Decrypt leaf signature with intermediate's public key
   → Does it match the leaf cert's content hash?
   → YES → continue

2. Is the intermediate cert signed by the Root CA?
   → Decrypt intermediate signature with root CA's public key
   → Does it match?
   → YES → continue

3. Is the Root CA in my trust store?
   → Check OS/browser trusted root list
   → YES → TRUSTED

If any step fails → browser shows "Certificate is not trusted" error
```

### Step 2: Hostname Validation

```text
Browser connects to: yourbank.com

Checks certificate fields:
  Subject Alternative Names (SANs): DNS:yourbank.com, DNS:www.yourbank.com
  Common Name (CN): yourbank.com (legacy fallback, deprecated)

Does the domain match any SAN?
  → yourbank.com matches DNS:yourbank.com → PASS

Wildcard: *.bank.com matches api.bank.com but NOT api.sub.bank.com
Multi-domain: SAN cert can list 100+ domains explicitly
```

### Step 3: Validity Period

```text
Certificate has:
  notBefore: 2025-01-01 00:00:00 UTC
  notAfter:  2026-01-01 23:59:59 UTC

Browser checks: is today between notBefore and notAfter?
  → YES → PASS
  → NO  → "Certificate has expired" or "Certificate not yet valid"
```

### Step 4: Revocation Check

Certificates can be revoked before expiry (e.g., private key was compromised).

```text
Two mechanisms:
  CRL  (Certificate Revocation List): a list of revoked serial numbers, published by CA
  OCSP (Online Certificate Status Protocol): real-time query to CA's OCSP responder

Browser queries: "Is serial number 0x1A3B5C7D... still valid?"
CA responds:     "good" | "revoked" | "unknown"
```

### Step 5: Key Usage

```text
Certificate extension KeyUsage must allow TLS server authentication:
  extendedKeyUsage: TLS Web Server Authentication (OID 1.3.6.1.5.5.7.3.1)

If cert was issued for email signing only and is being used for HTTPS → FAIL
```

### Step 6: Signature Algorithm

```text
Modern requirements (2025):
  ✓ SHA-256 (or stronger) for signing
  ✓ RSA 2048+ or ECDSA P-256+
  ✗ MD5: rejected (broken)
  ✗ SHA-1: rejected since 2017 (Chrome, Firefox)
  ✗ RSA < 1024: rejected
```

---

## 8. OCSP, CRL, and Certificate Transparency

### CRL (Certificate Revocation List)

```text
CA publishes a signed list of all revoked certificate serial numbers.
Browser downloads the CRL and checks if the cert is in the list.

Problems:
  - CRLs can be huge (MBs) for large CAs
  - Browser caches CRL (may be stale)
  - Downloading every CRL for every site is slow

Use today: CRLs still exist but OCSP is preferred for leaf certs.
           CRLSets (Chrome's pre-downloaded curated list) replaces per-cert CRL checks.
```

### OCSP (Online Certificate Status Protocol)

```text
Client asks CA's OCSP responder in real-time:
  Request:  "Is certificate serial 0xABCD... issued by CA XYZ... still valid?"
  Response: "good" | "revoked" | "unknown"
  Response is signed by CA → can be cached

Problem: privacy leak — every TLS connection tells CA which sites you visit.
Problem: latency — extra round-trip to CA's OCSP server during TLS handshake.
Problem: if OCSP server is down → soft-fail: browser accepts cert anyway.
```

### OCSP Stapling

```text
Solution to OCSP problems:
  Instead of the browser querying the CA's OCSP server,
  the WEB SERVER pre-fetches its own OCSP response and "staples" it to the TLS handshake.

Flow:
  1. Server periodically fetches OCSP response from CA (every few hours)
  2. Server caches the signed OCSP response
  3. During TLS handshake, server sends the OCSP response along with certificate
  4. Browser verifies OCSP response signature (can do it offline)
  
Benefits:
  ✓ No privacy leak (browser never contacts CA directly)
  ✓ No extra latency during handshake
  ✓ Reduces load on CA's OCSP servers

nginx config:
  ssl_stapling on;
  ssl_stapling_verify on;
  resolver 8.8.8.8 8.8.4.4 valid=300s;
```

### Certificate Transparency (CT Logs)

Certificate Transparency is a public audit log of every certificate ever issued.

```text
Problem: A rogue CA could secretly issue a certificate for google.com.
         Nobody would know until the attack was already in progress.

Solution: Every CA is REQUIRED to log every certificate they issue to public CT logs
          (Merkle tree-based append-only logs operated by Google, Cloudflare, etc.)

Benefits:
  - Domain owners can monitor for unauthorized certificates for their domains
  - Google Chrome requires CT since 2018 (certs not in CT logs are rejected)
  
How it works:
  1. CA issues certificate
  2. CA submits cert to multiple CT logs
  3. CT logs return Signed Certificate Timestamp (SCT)
  4. CA embeds SCTs into the final certificate (or delivers via TLS extension)
  5. Browser checks that cert has valid SCTs → proof it was publicly logged

Monitoring tool: https://crt.sh  (search any domain to see all issued certs)
Alert tool:      Google Certificate Transparency Monitoring
                 Cloudflare Certificate Transparency Monitoring
```

### CAA Records (Certification Authority Authorization)

```text
DNS record that specifies WHICH Certificate Authorities are allowed to issue
certificates for your domain.

Example DNS:
  yourbank.com.  CAA  0 issue    "digicert.com"
  yourbank.com.  CAA  0 issuewild "digicert.com"
  yourbank.com.  CAA  0 iodef    "mailto:security@yourbank.com"

Meaning:
  - Only DigiCert can issue certificates for yourbank.com
  - Only DigiCert can issue wildcard certificates
  - Email security@ if a CA receives an unauthorized request

CAs are required to check CAA records before issuing (since 2017, CA/Browser Forum).
An attacker can't get Let's Encrypt to issue a cert for yourbank.com
if yourbank.com's CAA only allows DigiCert.
```

---

## 9. Inside a Certificate: X.509 Fields

```bash
# View a certificate's content
openssl x509 -in cert.pem -text -noout
```

Sample output explained:

```text
Certificate:
    Data:
        Version: 3 (0x2)                    ← always v3 for modern certs
        Serial Number:                       ← unique ID assigned by CA
            08:3b:e0:56:90:42:46:b1:a1:75:6a:c9:59:91:c7:4a
        
        Signature Algorithm: sha256WithRSAEncryption  ← signing algorithm

        Issuer: C=US, O=DigiCert Inc, CN=DigiCert Global CA G2
                ← who signed this cert (the CA)

        Validity
            Not Before: Jan  1 00:00:00 2025 GMT
            Not After : Jan  1 23:59:59 2026 GMT
                ← 1-year validity (Let's Encrypt: 90 days)

        Subject: CN=yourbank.com
                ← who this cert belongs to
                ← CN alone is deprecated; SANs are authoritative

        Subject Public Key Info:
            Public Key Algorithm: id-ecPublicKey     ← ECDSA key
            EC Key Size: 256 bit                     ← P-256 curve

        X509v3 extensions:
        
            X509v3 Subject Alternative Name:
                DNS:yourbank.com, DNS:www.yourbank.com, DNS:api.yourbank.com
                ← authoritative list of valid hostnames (since RFC 2818, 2000)

            X509v3 Key Usage: critical
                Digital Signature, Key Agreement
                ← what the key is allowed to do

            X509v3 Extended Key Usage:
                TLS Web Server Authentication, TLS Web Client Authentication
                ← TLS server auth required for HTTPS leaf certs

            X509v3 Basic Constraints: critical
                CA:FALSE
                ← this is NOT a CA certificate (cannot sign other certs)
                ← CA:TRUE only in CA certificates; cA:TRUE + pathLen:0 = intermediate CA

            X509v3 CRL Distribution Points:
                URI:http://crl3.digicert.com/GlobalCAG2.crl
                ← where to download the CRL

            Authority Information Access:
                OCSP - URI:http://ocsp.digicert.com
                CA Issuers - URI:http://cacerts.digicert.com/DigiCertGlobalCAG2.crt
                ← OCSP responder URL, and where to get the intermediate CA cert

            X509v3 Certificate Policies:
                Policy: 2.23.140.1.2.2        ← OV (Organization Validated) policy OID

            X509v3 Subject Key Identifier:
                A1:B2:C3:D4:...               ← hash of this cert's public key

            X509v3 Authority Key Identifier:
                keyid:E2:3D:4F:...            ← hash of issuer's public key
                ← used to build the chain: find parent by matching AKI to parent SKI

    Signature Algorithm: sha256WithRSAEncryption
        [CA's digital signature over all the above content]
```

---

## 10. Certificate Types

### By Validation Level

| Type | Validation | Browser Display | Use Case |
|---|---|---|---|
| **DV** (Domain Validated) | Prove domain control only (DNS/HTTP challenge) | Padlock | General websites, APIs, blogs |
| **OV** (Organization Validated) | Prove domain + organization exists (manual review) | Padlock + org details | Business sites, e-commerce |
| **EV** (Extended Validated) | Extensive legal + organization vetting | Padlock (some browsers show org name) | Banks, government, high-trust |

```text
DV: Let's Encrypt, ZeroSSL → free, automated, 90 days
OV: DigiCert, Sectigo, GlobalSign → $50-200/year, 1-2 year
EV: DigiCert, Entrust → $200-1000/year, requires legal review

Note: As of ~2019, major browsers no longer display the green EV bar prominently.
      The visual difference between DV, OV, and EV for end users is minimal.
      Let's Encrypt DV is appropriate for most production services.
```

### By Domain Coverage

```text
Single-domain:  covers exactly one hostname
                  Subject: CN=yourbank.com
                  SANs:    DNS:yourbank.com, DNS:www.yourbank.com

Wildcard:       covers all immediate subdomains with one certificate
                  Subject: CN=*.yourbank.com
                  Valid for: api.yourbank.com, cdn.yourbank.com, mail.yourbank.com
                  NOT valid for: sub.api.yourbank.com (two levels deep)
                  NOT valid for: yourbank.com itself (wildcard doesn't cover base)
                  
Multi-SAN:      covers many explicitly named domains in one certificate
                  SANs: DNS:a.com, DNS:b.com, DNS:api.c.org, IP:10.0.0.1
                  Let's Encrypt: up to 100 SANs per cert
                  Useful for CDNs, shared hosting
```

### Certificates for Non-Web Purposes

```text
Code Signing:    signs software binaries → OS verifies software hasn't been tampered with
Email (S/MIME):  encrypts + signs email messages
Client Cert:     identifies a USER or DEVICE to a server (for mTLS)
IoT Device Cert: unique identity for each device on a fleet
```

---

## 11. Certificate File Formats

```text
PEM (Privacy Enhanced Mail):
  Base64-encoded DER data wrapped in -----BEGIN CERTIFICATE----- headers
  Extension: .pem, .crt, .cer, .key
  Most common format on Linux/nginx/Apache
  Can contain a cert, a private key, or a full chain (concatenate multiple)
  
DER (Distinguished Encoding Rules):
  Binary format (the raw ASN.1 encoding)
  Extension: .der, .cer
  Common on Java systems, Windows CryptoAPI
  
PFX / PKCS#12:
  Binary archive containing cert + private key + optional chain
  Extension: .pfx, .p12
  Password-protected
  Common in Windows, IIS, Java keystores
  Used when you need to export cert + key as a bundle
  
JKS (Java KeyStore):
  Java-specific binary keystore
  Contains keys and trusted certificates
  Extension: .jks
  Used by Tomcat, Java applications
  Being replaced by PKCS#12 even in Java (JDK 9+)
  
PKCS#7 / P7B:
  Base64 or DER format containing cert chain (no private key)
  Extension: .p7b, .p7c
  Used by Windows Certificate Import Wizard
```

### Converting Between Formats

```bash
# PEM → DER
openssl x509 -in cert.pem -outform DER -out cert.der

# DER → PEM
openssl x509 -in cert.der -inform DER -outform PEM -out cert.pem

# PEM cert + key → PFX
openssl pkcs12 -export \
  -out bundle.pfx \
  -inkey private.key \
  -in cert.pem \
  -certfile chain.pem

# PFX → PEM (extracts cert + key from PFX)
openssl pkcs12 -in bundle.pfx -nokeys -out cert.pem
openssl pkcs12 -in bundle.pfx -nocerts -nodes -out private.key

# PEM chain → PKCS#7
openssl crl2pkcs7 -nocrl -certfile fullchain.pem -out bundle.p7b

# View PFX contents
openssl pkcs12 -in bundle.pfx -info -noout
```

---

## 12. Creating Certificates with OpenSSL

### Step 0: Understand the Workflow

```text
Option A: Self-signed (dev/internal)
  1. Generate private key
  2. Generate self-signed certificate (skips CSR + CA)

Option B: CA-signed (internal CA or for testing)
  1. Generate private key (on the server)
  2. Generate CSR (Certificate Signing Request)
  3. Send CSR to CA
  4. CA verifies + signs → returns certificate
  5. Install cert on server

Option C: Public CA (production)
  → Same as B, but you use Let's Encrypt, AWS ACM, DigiCert, etc.
  → For Let's Encrypt: steps are automated via ACME protocol
```

### Generate a Private Key

```bash
# RSA 2048-bit key (common, widely supported)
openssl genrsa -out private.key 2048

# RSA 4096-bit key (stronger, slower)
openssl genrsa -out private.key 4096

# ECDSA P-256 key (modern, faster than RSA, same security as RSA 3072)
openssl ecparam -name prime256v1 -genkey -noout -out private.key

# ECDSA P-384 key (higher security)
openssl ecparam -name secp384r1 -genkey -noout -out private.key

# With password protection (prompts for password)
openssl genrsa -aes256 -out private.key 2048

# View key info
openssl rsa -in private.key -text -noout
openssl ec -in private.key -text -noout   # for ECDSA keys
```

### Generate a CSR (Certificate Signing Request)

```bash
# Interactive (prompts for Subject fields)
openssl req -new -key private.key -out request.csr

# Non-interactive (pass subject on command line)
openssl req -new \
  -key private.key \
  -out request.csr \
  -subj "/C=US/ST=California/L=San Francisco/O=MyCompany Inc/CN=yourbank.com"

# With SANs (required for modern certificates — CN alone is not enough)
# Create a config file first:
cat > csr.conf <<EOF
[req]
default_bits       = 2048
prompt             = no
distinguished_name = dn
req_extensions     = req_ext

[dn]
C  = US
ST = California
L  = San Francisco
O  = MyCompany Inc
CN = yourbank.com

[req_ext]
subjectAltName = @alt_names

[alt_names]
DNS.1 = yourbank.com
DNS.2 = www.yourbank.com
DNS.3 = api.yourbank.com
IP.1  = 10.0.0.1
EOF

openssl req -new -key private.key -out request.csr -config csr.conf

# Verify CSR content
openssl req -in request.csr -text -noout
```

### Create a Self-Signed Certificate

```bash
# Simple self-signed (no SANs — deprecated, but works for internal tools)
openssl req -new -x509 \
  -key private.key \
  -out cert.pem \
  -days 365 \
  -subj "/CN=my-internal-service"

# Self-signed with SANs (required for modern browsers)
cat > ext.conf <<EOF
[SAN]
subjectAltName = DNS:my-service.internal, DNS:localhost, IP:127.0.0.1
EOF

openssl req -new -x509 \
  -key private.key \
  -out cert.pem \
  -days 365 \
  -subj "/CN=my-service.internal" \
  -extensions SAN \
  -config <(cat /etc/ssl/openssl.cnf ext.conf)

# View the certificate
openssl x509 -in cert.pem -text -noout
```

### Build Your Own Root CA and Sign Certificates

```bash
### 1. Create Root CA private key
openssl genrsa -out rootCA.key 4096

### 2. Create Root CA self-signed certificate (valid 10 years)
openssl req -new -x509 \
  -key rootCA.key \
  -out rootCA.crt \
  -days 3650 \
  -subj "/C=US/O=MyCompany/CN=MyCompany Root CA" \
  -extensions v3_ca \
  -config <(cat <<EOF
[req]
distinguished_name = req
[req]
[v3_ca]
subjectKeyIdentifier = hash
authorityKeyIdentifier = keyid:always,issuer
basicConstraints = critical, CA:true, pathlen:1
keyUsage = critical, digitalSignature, cRLSign, keyCertSign
EOF
)

### 3. Generate service private key
openssl genrsa -out server.key 2048

### 4. Generate CSR for the service
openssl req -new \
  -key server.key \
  -out server.csr \
  -subj "/CN=my-service.internal"

### 5. Sign the CSR with your Root CA
cat > v3_ext.conf <<EOF
authorityKeyIdentifier=keyid,issuer
basicConstraints=CA:FALSE
keyUsage = critical, digitalSignature, keyEncipherment
extendedKeyUsage = serverAuth
subjectAltName = DNS:my-service.internal, DNS:localhost
EOF

openssl x509 -req \
  -in server.csr \
  -CA rootCA.crt \
  -CAkey rootCA.key \
  -CAcreateserial \
  -out server.crt \
  -days 365 \
  -sha256 \
  -extfile v3_ext.conf

### 6. Verify the chain
openssl verify -CAfile rootCA.crt server.crt
# Output: server.crt: OK

### 7. Distribute: install rootCA.crt into clients' trust stores
# macOS: sudo security add-trusted-cert -d -r trustRoot -k /Library/Keychains/System.keychain rootCA.crt
# Ubuntu: sudo cp rootCA.crt /usr/local/share/ca-certificates/ && sudo update-ca-certificates
# curl:   curl --cacert rootCA.crt https://my-service.internal
```

### Verify / Inspect Certificates

```bash
# Verify a certificate against a CA
openssl verify -CAfile rootCA.crt server.crt

# Verify a full chain
openssl verify -CAfile rootCA.crt -untrusted intermediate.crt leaf.crt

# Check certificate expiry date
openssl x509 -in cert.pem -noout -dates

# Check if certificate will expire within 30 days (returns exit code 1 if yes)
openssl x509 -in cert.pem -checkend $((30 * 86400)) -noout

# Check a live site's certificate
openssl s_client -connect yourbank.com:443 -servername yourbank.com < /dev/null | \
  openssl x509 -text -noout

# See full chain from a live server
echo | openssl s_client -connect yourbank.com:443 -showcerts 2>/dev/null

# Check OCSP status of a live cert
openssl s_client -connect yourbank.com:443 -status < /dev/null 2>&1 | grep -A 10 "OCSP response"

# Get certificate fingerprint (for pinning or comparison)
openssl x509 -in cert.pem -fingerprint -sha256 -noout
```

---

## 13. Let's Encrypt and the ACME Protocol

### What Is Let's Encrypt?

Let's Encrypt is a **free, automated, open Certificate Authority** run by ISRG (Internet Security Research Group), backed by Mozilla, Google, Cisco, and others.

```text
Key facts:
  - Free: no cost per certificate
  - Automated: no manual verification process
  - Short-lived: 90-day certificates (forces automation of renewal)
  - DV only: domain validation, not OV or EV
  - Wildcard support: yes (via DNS-01 challenge)
  - Rate limits: 50 certs per registered domain per week
  
Root CA: ISRG Root X1
  Intermediate: Let's Encrypt R3, E1, R4, etc.
```

### The ACME Protocol

ACME (Automated Certificate Management Environment, RFC 8555) is the protocol Let's Encrypt uses for automated DV certificate issuance.

```text
ACME Challenge Types:

HTTP-01 Challenge:
  1. Your ACME client asks Let's Encrypt for a certificate
  2. Let's Encrypt generates a random token
  3. Your server must serve the token at:
     http://yourdomain.com/.well-known/acme-challenge/{token}
  4. Let's Encrypt fetches the URL and verifies the token
  5. Proves you control the server responding on port 80 for that domain
  
  ✓ Simple to implement
  ✗ Requires port 80 open
  ✗ Cannot issue wildcard certs (*.yourdomain.com)

DNS-01 Challenge:
  1. Let's Encrypt generates a random token
  2. You must create a TXT DNS record:
     _acme-challenge.yourdomain.com → [token-hash]
  3. Let's Encrypt does a DNS lookup to verify
  4. Proves you control DNS for the domain
  
  ✓ Works without port 80 (internal servers, private networks)
  ✓ Required for wildcard certificates
  ✓ Works when server is behind load balancer
  ✗ Requires DNS API automation (Route53, Cloudflare, etc.)

TLS-ALPN-01 Challenge:
  1. Proves domain control by completing a TLS handshake on port 443
  2. Used less commonly; useful when only 443 is exposed
```

### certbot: The Reference ACME Client

```bash
# Install certbot
sudo apt-get install certbot python3-certbot-nginx  # Ubuntu with nginx
brew install certbot                                 # macOS

# Obtain certificate (nginx)
sudo certbot --nginx -d yoursite.com -d www.yoursite.com

# Obtain certificate (standalone, for services not nginx/apache)
sudo certbot certonly --standalone \
  -d yoursite.com -d api.yoursite.com \
  --email admin@yoursite.com \
  --agree-tos

# Obtain wildcard certificate (requires DNS-01 challenge)
sudo certbot certonly --manual \
  --preferred-challenges dns \
  -d yoursite.com -d '*.yoursite.com'
  
# Automate DNS-01 with Route53 (AWS)
pip install certbot-dns-route53
sudo certbot certonly --dns-route53 \
  -d '*.yoursite.com' -d yoursite.com

# Test renewal dry-run
sudo certbot renew --dry-run

# Force renewal
sudo certbot renew --force-renewal -d yoursite.com

# List certificates
sudo certbot certificates

# Revoke a certificate
sudo certbot revoke --cert-path /etc/letsencrypt/live/yoursite.com/cert.pem
```

### Let's Encrypt Certificate Files

```text
After certbot succeeds, files are in /etc/letsencrypt/live/yoursite.com/:

cert.pem       ← The leaf certificate for your domain
chain.pem      ← The intermediate CA certificate(s)
fullchain.pem  ← cert.pem + chain.pem concatenated (use this for most servers)
privkey.pem    ← Your private key (KEEP SECURE, mode 600)

nginx config:
  ssl_certificate     /etc/letsencrypt/live/yoursite.com/fullchain.pem;
  ssl_certificate_key /etc/letsencrypt/live/yoursite.com/privkey.pem;

Apache config:
  SSLCertificateFile    /etc/letsencrypt/live/yoursite.com/cert.pem
  SSLCertificateKeyFile /etc/letsencrypt/live/yoursite.com/privkey.pem
  SSLCertificateChainFile /etc/letsencrypt/live/yoursite.com/chain.pem
```

### Auto-Renewal Setup

```bash
# systemd timer (set up by certbot automatically on Debian/Ubuntu)
systemctl status certbot.timer
systemctl list-timers | grep certbot

# Cron job alternative (runs twice daily)
echo "0 0,12 * * * root certbot renew --quiet" | sudo tee -a /etc/crontab

# Certbot renews certificates with < 30 days remaining
# Since certs are 90 days, this gives a 60-day window with two monthly chances

# Reload nginx after renewal
cat > /etc/letsencrypt/renewal-hooks/deploy/reload-nginx.sh <<EOF
#!/bin/bash
systemctl reload nginx
EOF
chmod +x /etc/letsencrypt/renewal-hooks/deploy/reload-nginx.sh
```

---

## 14. AWS Certificate Manager (ACM)

ACM provides free SSL/TLS certificates for use with AWS services.

```text
Key points:
  - Free for certificates used with ALB, CloudFront, API Gateway, etc.
  - NOT for installing on EC2 directly (use Let's Encrypt for that)
  - Auto-renewal: ACM renews automatically ~60 days before expiry
  - Supports DNS and email validation
  - Wildcard and multi-domain SAN certificates
  - Cannot export private key (for security — ACM manages it)
  - Supports RSA and ECDSA keys
```

### Request a Certificate

```bash
# Via AWS CLI
aws acm request-certificate \
  --domain-name yoursite.com \
  --subject-alternative-names www.yoursite.com api.yoursite.com \
  --validation-method DNS \
  --region us-east-1

# Get the CNAME records to add to DNS for validation
aws acm describe-certificate \
  --certificate-arn arn:aws:acm:us-east-1:123456789:certificate/abc123 \
  --query "Certificate.DomainValidationOptions[*].ResourceRecord"
```

### Terraform — Request and Validate ACM Certificate

```hcl
resource "aws_acm_certificate" "main" {
  domain_name               = "yoursite.com"
  subject_alternative_names = ["www.yoursite.com", "api.yoursite.com"]
  validation_method         = "DNS"

  lifecycle {
    create_before_destroy = true   # required for zero-downtime rotation
  }
}

# Add validation CNAME records to Route53
resource "aws_route53_record" "cert_validation" {
  for_each = {
    for dvo in aws_acm_certificate.main.domain_validation_options : dvo.domain_name => {
      name   = dvo.resource_record_name
      type   = dvo.resource_record_type
      record = dvo.resource_record_value
    }
  }
  zone_id = data.aws_route53_zone.main.zone_id
  name    = each.value.name
  type    = each.value.type
  records = [each.value.record]
  ttl     = 60
}

# Wait for validation to complete
resource "aws_acm_certificate_validation" "main" {
  certificate_arn         = aws_acm_certificate.main.arn
  validation_record_fqdns = [for record in aws_route53_record.cert_validation : record.fqdn]
}

# Attach to ALB
resource "aws_lb_listener" "https" {
  load_balancer_arn = aws_lb.main.arn
  port              = 443
  protocol          = "HTTPS"
  ssl_policy        = "ELBSecurityPolicy-TLS13-1-2-2021-06"
  certificate_arn   = aws_acm_certificate_validation.main.certificate_arn

  default_action {
    type             = "forward"
    target_group_arn = aws_lb_target_group.main.arn
  }
}
```

---

## 15. Certificate Rotation

Certificate rotation is the process of replacing an expiring (or expired, or compromised) certificate with a new one — without downtime.

### Why Rotate?

```text
1. Expiry: every cert has a validity window (90 days for Let's Encrypt, 1-2 years for commercial)
2. Compromise: private key was leaked → must revoke + replace immediately
3. CA compromise: a CA in your chain was compromised → all its certs must be replaced
4. Algorithm deprecation: SHA-1 was deprecated in 2017; RSA 1024 deprecated
5. Domain change: CN/SANs need to be updated
6. Compliance: policy requiring key rotation every 90 days, 1 year, etc.
```

### Certificate Lifecycle

```text
Issued               Renew window             Expiry
  │                      │←─── 30 days ──→│         │
  │                      │               Expired   │
  ├─── Active period ────┤               ↓
  │                      │          Browser shows
  │                      │          "Your connection is not private"
  │                 Start renewal
  │                 process here
  
Let's Encrypt 90-day lifecycle:
  Day 0:   Certificate issued
  Day 60:  certbot renew starts attempting renewal (30-day window)
  Day 75:  Expiry warnings start appearing in certbot logs
  Day 90:  Certificate expires → service broken if not renewed
```

### Rotation Strategies

**Strategy 1: Pre-expiry renewal (standard)**

```text
Monitor certificate expiry:
  - Set alerts at 30 days, 14 days, 7 days before expiry
  - Automated renewal process runs at 30 days
  - New cert generated, loaded, old cert discarded
  
For most web servers, a reload (not restart) picks up the new certificate:
  nginx:   nginx -s reload
  Apache:  apachectl graceful
  HAProxy: systemctl reload haproxy
```

**Strategy 2: Automated rotation (Let's Encrypt / cert-manager)**

```text
Let's Encrypt + certbot:
  Certbot runs twice daily from cron/systemd
  Renews any cert with < 30 days remaining
  Runs deploy hooks to reload the web server
  Zero-downtime: new cert loaded on reload

cert-manager in Kubernetes:
  Watches Certificate resources
  Renews when remaining lifetime < renewBefore threshold
  Stores cert in a Secret; Pods pick it up via volume mounts
```

**Strategy 3: Blue/Green certificate rotation (zero-downtime for TLS termination)**

```text
For load balancers (ALB/nginx) that support multiple certificates:
  1. Issue NEW certificate
  2. Add NEW cert to load balancer (both old + new active)
  3. Verify NEW cert is working (test with curl --connect-to)
  4. Remove OLD cert from load balancer
  5. Revoke OLD cert (optional, if compromised)
  
ACM + ALB supports multiple certificates on one listener:
  aws elbv2 add-listener-certificates --listener-arn ... --certificates CertificateArn=...
```

**Strategy 4: Emergency rotation (private key compromise)**

```text
Time-sensitive — act immediately:
  1. Generate new private key (NOT the old one — it's compromised)
  2. Request new certificate from CA
  3. Install new cert + key
  4. Revoke old certificate (CRL/OCSP) → informs clients old cert is bad
  5. Audit access logs for unauthorized use of the old key
  6. Rotate any dependent secrets (sessions signed with the old key, API tokens, etc.)
```

### Zero-Downtime Rotation on nginx

```bash
# Step 1: Place new cert files
cp new_cert.pem /etc/ssl/certs/yoursite.crt
cp new_key.pem  /etc/ssl/private/yoursite.key

# Step 2: Verify nginx config is valid before reload
nginx -t

# Step 3: Graceful reload (no dropped connections)
nginx -s reload
# OR: systemctl reload nginx

# Nginx reload behavior:
#   - New worker processes start using the new certificate
#   - Old worker processes finish serving in-flight requests
#   - Old worker processes exit when all connections are done
#   - Zero dropped connections
```

### What Breaks When a Certificate Expires?

```text
Web browser:
  → "Your connection is not private" (NET::ERR_CERT_DATE_INVALID)
  → Users cannot access the site (strict validation)
  → No automatic fallback

curl / HTTP clients:
  → SSL certificate problem: certificate has expired
  → Exit code 60 unless --insecure is used

Mobile apps with certificate pinning:
  → App may crash or refuse to connect
  → App update may be required to pin new cert

Internal services / microservices:
  → TLS handshake failures → 503 errors → cascading failures if not handled
  → gRPC: "ssl handshake failure" 
  
IoT devices:
  → Device may be permanently bricked (can't receive firmware update)
  → Some devices use the system clock; if clock is wrong, cert checks can fail

Email (SMTP TLS):
  → Emails may be rejected or fall back to unencrypted (depending on policy)
```

---

## 16. cert-manager in Kubernetes

cert-manager is the de facto standard for certificate management in Kubernetes. It automatically issues and renews certificates from various sources (Let's Encrypt, internal CAs, Vault, etc.).

### Architecture

```text
cert-manager components:
  cert-manager controller → watches Certificate, CertificateRequest, Order, Challenge resources
  cert-manager cainjector → injects CA bundle into webhooks
  cert-manager webhook    → validates/mutates cert-manager resources
  
Certificate lifecycle:
  Certificate resource created →
    cert-manager creates CertificateRequest →
      CertificateRequest creates Order (for ACME) →
        Order creates Challenge(s) →
          Challenge solved (HTTP-01 or DNS-01) →
        Certificate issued →
      Stored in Kubernetes Secret →
    Certificate resource marked Ready
```

### Installation

```bash
# Install cert-manager via Helm
helm repo add jetstack https://charts.jetstack.io
helm repo update

helm install cert-manager jetstack/cert-manager \
  --namespace cert-manager \
  --create-namespace \
  --version v1.14.5 \
  --set installCRDs=true

# Verify
kubectl get pods -n cert-manager
```

### ClusterIssuer: Let's Encrypt

```yaml
# cluster-issuer-letsencrypt.yaml
apiVersion: cert-manager.io/v1
kind: ClusterIssuer
metadata:
  name: letsencrypt-prod
spec:
  acme:
    server: https://acme-v02.api.letsencrypt.org/directory
    email: admin@yoursite.com
    # Stores the ACME account key in this secret
    privateKeySecretRef:
      name: letsencrypt-prod-account-key
    solvers:
    - http01:
        ingress:
          class: nginx   # or: ingressClassName: nginx

---
# Staging issuer for testing (higher rate limits, untrusted cert)
apiVersion: cert-manager.io/v1
kind: ClusterIssuer
metadata:
  name: letsencrypt-staging
spec:
  acme:
    server: https://acme-staging-v02.api.letsencrypt.org/directory
    email: admin@yoursite.com
    privateKeySecretRef:
      name: letsencrypt-staging-account-key
    solvers:
    - http01:
        ingress:
          class: nginx
```

### Certificate Resource

```yaml
# certificate.yaml
apiVersion: cert-manager.io/v1
kind: Certificate
metadata:
  name: yoursite-tls
  namespace: production
spec:
  secretName: yoursite-tls-secret    # cert stored here as tls.crt + tls.key
  issuerRef:
    name: letsencrypt-prod
    kind: ClusterIssuer
  commonName: yoursite.com
  dnsNames:
  - yoursite.com
  - www.yoursite.com
  - api.yoursite.com
  duration: 2160h       # 90 days (Let's Encrypt default)
  renewBefore: 720h     # renew 30 days before expiry
  privateKey:
    algorithm: ECDSA
    size: 256
```

### Ingress Annotation (Auto-Certificate)

```yaml
# Instead of creating Certificate manually, annotate your Ingress:
apiVersion: networking.k8s.io/v1
kind: Ingress
metadata:
  name: myapp-ingress
  namespace: production
  annotations:
    cert-manager.io/cluster-issuer: "letsencrypt-prod"
    nginx.ingress.kubernetes.io/ssl-redirect: "true"
spec:
  tls:
  - hosts:
    - yoursite.com
    - www.yoursite.com
    secretName: yoursite-tls-secret   # cert-manager creates this Secret
  rules:
  - host: yoursite.com
    http:
      paths:
      - path: /
        pathType: Prefix
        backend:
          service:
            name: myapp
            port:
              number: 80
```

### ClusterIssuer: DNS-01 with Route53 (Wildcard Cert)

```yaml
apiVersion: cert-manager.io/v1
kind: ClusterIssuer
metadata:
  name: letsencrypt-dns-prod
spec:
  acme:
    server: https://acme-v02.api.letsencrypt.org/directory
    email: admin@yoursite.com
    privateKeySecretRef:
      name: letsencrypt-dns-account-key
    solvers:
    - dns01:
        route53:
          region: us-east-1
          hostedZoneID: Z1234567890ABC  # optional, speeds up validation
          # Uses IRSA (EKS pod identity) — no static credentials needed
          
---
# Wildcard certificate using DNS-01
apiVersion: cert-manager.io/v1
kind: Certificate
metadata:
  name: wildcard-tls
  namespace: istio-system
spec:
  secretName: wildcard-tls-secret
  issuerRef:
    name: letsencrypt-dns-prod
    kind: ClusterIssuer
  dnsNames:
  - "*.yoursite.com"
  - "yoursite.com"
```

### ClusterIssuer: Internal CA

```yaml
# Use your own CA stored as a Kubernetes Secret
# Step 1: Create secret with your root CA cert+key
kubectl create secret tls internal-ca-secret \
  --namespace cert-manager \
  --cert rootCA.crt \
  --key rootCA.key

# Step 2: Create CA Issuer
apiVersion: cert-manager.io/v1
kind: ClusterIssuer
metadata:
  name: internal-ca
spec:
  ca:
    secretName: internal-ca-secret   # the secret above
```

### Monitoring cert-manager

```bash
# Check Certificate status
kubectl get certificate -A
kubectl describe certificate yoursite-tls -n production

# Check if cert is ready
kubectl get certificate yoursite-tls -n production -o jsonpath='{.status.conditions[?(@.type=="Ready")].status}'

# Check the secret
kubectl get secret yoursite-tls-secret -n production -o jsonpath='{.data.tls\.crt}' | base64 -d | openssl x509 -text -noout

# Check expiry
kubectl get secret yoursite-tls-secret -n production -o jsonpath='{.data.tls\.crt}' | base64 -d | openssl x509 -noout -dates

# Check CertificateRequest and Order
kubectl get certificaterequest -n production
kubectl get order -n production
kubectl get challenge -n production

# cert-manager controller logs
kubectl logs -n cert-manager -l app=cert-manager --tail=50
```

---

## 17. mTLS: Mutual TLS

### One-Way TLS vs Mutual TLS

```text
One-Way TLS (standard HTTPS):
  Server proves its identity to the client
  Client says: "I trust your certificate → let's communicate securely"
  Server does NOT verify who the client is
  
  Client → presents nothing
  Server → presents certificate
  
Mutual TLS (mTLS):
  BOTH sides present and verify certificates
  Server proves identity to client
  Client also proves identity to server
  Only clients with valid certificates can communicate
  
  Client → presents client certificate
  Server → presents server certificate
  Both verify each other
```

### Where mTLS Is Used

```text
1. Service-to-service communication (microservices)
   → Each service has a certificate identifying it
   → mTLS replaces API keys for inter-service auth
   → Istio/Linkerd handle this transparently

2. Zero Trust networking
   → Network location is untrusted; identity must be cryptographically proven
   → Every connection authenticated with mTLS

3. API authentication (high-security)
   → Banking APIs, payment processors, government APIs
   → Client certificates instead of (or in addition to) API keys

4. VPN alternatives (WireGuard, internal mesh networks)
   → Devices authenticate with certificates

5. IoT fleet management
   → Each IoT device has a unique certificate
   → Server only accepts connections from provisioned devices
```

### Creating Client Certificates

```bash
# Using your internal Root CA (from Section 12):

# 1. Generate client private key
openssl genrsa -out client.key 2048

# 2. Create CSR
openssl req -new \
  -key client.key \
  -out client.csr \
  -subj "/C=US/O=MyCompany/CN=service-account-payment-service"

# 3. Sign with CA
cat > client_ext.conf <<EOF
basicConstraints = CA:FALSE
keyUsage = critical, digitalSignature
extendedKeyUsage = clientAuth   ← KEY DIFFERENCE: clientAuth not serverAuth
EOF

openssl x509 -req \
  -in client.csr \
  -CA rootCA.crt \
  -CAkey rootCA.key \
  -CAcreateserial \
  -out client.crt \
  -days 365 \
  -sha256 \
  -extfile client_ext.conf

# 4. Bundle into PFX for apps that need it
openssl pkcs12 -export \
  -out client.pfx \
  -inkey client.key \
  -in client.crt \
  -certfile rootCA.crt
```

### nginx: Enforce Client Certificate Authentication

```nginx
server {
    listen 443 ssl;
    server_name api.internal.com;

    ssl_certificate     /etc/ssl/certs/server.crt;
    ssl_certificate_key /etc/ssl/private/server.key;

    # Required: CA that signed client certs
    ssl_client_certificate /etc/ssl/certs/rootCA.crt;
    
    # require:  client MUST present a valid certificate
    # optional: accept but don't require
    # off:      no client cert validation (standard TLS)
    ssl_verify_client require;
    ssl_verify_depth  2;   # max chain depth

    location / {
        # Pass client cert info to backend app
        proxy_set_header X-SSL-Client-DN    $ssl_client_s_dn;
        proxy_set_header X-SSL-Client-Verify $ssl_client_verify;
        proxy_pass http://backend;
    }
}
```

### curl with Client Certificate

```bash
# Make request with client cert
curl --cert client.crt \
     --key client.key \
     --cacert rootCA.crt \
     https://api.internal.com/endpoint

# Using PFX
curl --cert-type P12 \
     --cert client.pfx:password \
     --cacert rootCA.crt \
     https://api.internal.com/endpoint
```

### mTLS in Istio (Service Mesh)

```yaml
# Enable STRICT mTLS across the entire mesh
apiVersion: security.istio.io/v1beta1
kind: PeerAuthentication
metadata:
  name: default
  namespace: istio-system   # mesh-wide policy
spec:
  mtls:
    mode: STRICT   # STRICT = require mTLS, PERMISSIVE = allow both

---
# Namespace-scoped mTLS
apiVersion: security.istio.io/v1beta1
kind: PeerAuthentication
metadata:
  name: default
  namespace: production
spec:
  mtls:
    mode: STRICT
    
---
# Authorization policy using mTLS identity
apiVersion: security.istio.io/v1beta1
kind: AuthorizationPolicy
metadata:
  name: payment-policy
  namespace: production
spec:
  selector:
    matchLabels:
      app: payment-service
  rules:
  - from:
    - source:
        # Only allow from order-service (identified by its mTLS cert)
        principals: ["cluster.local/ns/production/sa/order-service"]
```

```text
How Istio mTLS works:
  1. Istio injects Envoy sidecar into each Pod
  2. Envoy intercepts all incoming/outgoing traffic
  3. Istio CA (Istiod) issues X.509 certificates to each Envoy sidecar
     (SPIFFE format: spiffe://cluster.local/ns/production/sa/payment-service)
  4. Envoy-to-Envoy connections are automatically mTLS
  5. Application code doesn't change — mTLS is transparent
```

---

## 18. HashiCorp Vault PKI Engine

Vault's PKI secrets engine acts as a **dynamic** Certificate Authority — generating short-lived certificates on demand.

### Why Use Vault for Certificates?

```text
Problem with long-lived certificates:
  - If private key is compromised, attacker has 1 year of access
  - Revocation is slow (CRL distribution, OCSP soft-fail)
  - Rotation requires human intervention

Vault solution:
  - Issue certificates valid for minutes or hours
  - If compromised, expires quickly
  - No need to revoke — just let it expire
  - Automated issuance: apps request certs from Vault API when they start
  - Audit log: every certificate request is logged
```

### Setting Up Vault PKI

```bash
# Enable the PKI secrets engine
vault secrets enable pki
vault secrets tune -max-lease-ttl=87600h pki   # 10 years for root

# Generate root CA inside Vault
vault write -field=certificate pki/root/generate/internal \
  common_name="MyCompany Root CA" \
  ttl=87600h > rootCA.crt

# Configure URLs (so issued certs know where to find CRL/OCSP)
vault write pki/config/urls \
  issuing_certificates="https://vault.internal:8200/v1/pki/ca" \
  crl_distribution_points="https://vault.internal:8200/v1/pki/crl"

# Enable intermediate CA for day-to-day issuance
vault secrets enable -path=pki_int pki
vault secrets tune -max-lease-ttl=43800h pki_int  # 5 years

# Generate intermediate CA and sign with root
vault write -format=json pki_int/intermediate/generate/internal \
  common_name="MyCompany Intermediate CA" \
  | jq -r '.data.csr' > pki_int.csr

vault write -format=json pki/root/sign-intermediate \
  csr=@pki_int.csr \
  format=pem_bundle \
  ttl=43800h \
  | jq -r '.data.certificate' > intermediate.crt

vault write pki_int/intermediate/set-signed certificate=@intermediate.crt

# Create a role (template for cert issuance)
vault write pki_int/roles/my-services \
  allowed_domains="service.internal,cluster.local" \
  allow_subdomains=true \
  max_ttl=24h \          # max cert lifetime: 1 day
  generate_lease=true    # Vault lease tracks expiry
```

### Issuing Certificates from Vault

```bash
# Request a certificate (app does this on startup)
vault write pki_int/issue/my-services \
  common_name="payment-service.service.internal" \
  alt_names="localhost" \
  ip_sans="10.0.1.5" \
  ttl=1h

# Returns:
#   certificate     ← PEM cert
#   private_key     ← PEM private key
#   ca_chain        ← intermediate CA chain
#   serial_number   ← for tracking/revocation
#   lease_id        ← Vault lease (renew before expiry)
#   lease_duration  ← 3600 (1 hour in seconds)
```

### Vault Agent / cert-manager + Vault

```yaml
# cert-manager Vault Issuer
apiVersion: cert-manager.io/v1
kind: ClusterIssuer
metadata:
  name: vault-issuer
spec:
  vault:
    server: https://vault.internal:8200
    path: pki_int/sign/my-services
    auth:
      kubernetes:
        mountPath: /v1/auth/kubernetes
        role: cert-manager-role
        serviceAccountRef:
          name: cert-manager
```

---

## 19. Production Issues and Anti-Patterns

### Issue 1: Certificate Expiry Surprise

```text
Root cause: No monitoring, manual renewal process, someone forgot.

Prevention:
  1. Monitor expiry with Datadog, Prometheus, or Nagios:
     # Prometheus blackbox exporter
     - module: http_2xx → scrape_configs with ssl_verify
     
     # Custom check script in CI/cron:
     openssl s_client -connect yoursite.com:443 < /dev/null 2>/dev/null \
       | openssl x509 -noout -checkend $((14 * 86400)) \
       || alert "cert expires in < 14 days"
  
  2. Alert at 30 days, 14 days, 7 days
  3. Use automated issuance (Let's Encrypt/cert-manager) — never rely on humans for renewal
  4. Test your renewal process quarterly
```

### Issue 2: Missing Intermediate Certificate

```text
Error: "Certificate chain is incomplete"

Root cause: Server sends only the leaf cert, not the intermediate CA cert.
Browsers usually fix this by downloading the intermediate from the AIA extension.
But strict clients (curl, Java HttpsURLConnection, many APIs) don't.

Fix: Always serve the full chain:
  nginx:  ssl_certificate /path/to/fullchain.pem;  ← cert + intermediates
  Apache: SSLCertificateChainFile /path/to/chain.pem

Test:    openssl s_client -connect site.com:443 -showcerts
         → should show 2-3 certificates (leaf + 1-2 intermediates)
```

### Issue 3: Private Key Exposure

```text
Anti-patterns:
  ✗ Private key in git repository (even accidentally, even for 1 second)
  ✗ Private key in S3 bucket without strict access control
  ✗ Private key in Kubernetes Secret without RBAC restriction
  ✗ Private key in environment variable (shows up in ps, /proc, logs)
  ✗ Private key exported from ACM → defeats the purpose of ACM

Best practices:
  ✓ Private key never leaves the server that generated it
  ✓ For K8s: use cert-manager (generates key inside cluster, stores in encrypted etcd)
  ✓ Use AWS ACM / Vault / HSM — let the platform manage the private key
  ✓ If key must be stored: encrypt at rest (PKCS#8 with AES-256)
  ✓ Rotate immediately if any doubt about exposure
```

### Issue 4: Using Self-Signed Certs in Production

```text
Anti-pattern: disabling TLS verification in production code:
  ✗ curl -k (--insecure)
  ✗ requests.get(url, verify=False)
  ✗ SSLContext.setEndpointIdentificationAlgorithm(null)  // Java
  ✗ NODE_TLS_REJECT_UNAUTHORIZED=0
  
These disable ALL certificate validation — a MITM attacker's dream.

Fix:
  Option A: Distribute your root CA cert to clients (import it as trusted)
  Option B: Use Let's Encrypt (publicly trusted, free, 90 days)
  Option C: Use AWS ACM for internal services (if all clients are in AWS)
  Option D: Use cert-manager with a ClusterIssuer backed by your internal CA
            AND distribute the root CA cert to all clients
```

### Issue 5: Certificate Pinning

```text
Certificate pinning: hard-coding a specific cert's fingerprint in your app.
If the cert changes (rotation!), the app breaks until updated.

Where it appears:
  - Mobile apps (Android/iOS) pinning the production cert
  - Desktop apps with embedded certs
  - Microservices with hard-coded cert hashes

Best practice: pin the CA/issuer, not the leaf certificate:
  - Pin Let's Encrypt's root CA (ISRG Root X1) — stable for years
  - Or pin your intermediate CA (changes rarely)
  - Never pin the leaf cert (changes every 90-365 days)

HPKP (HTTP Public Key Pinning) header: DEPRECATED
  → Was a mechanism to pin certs via HTTP header
  → Removed by Chrome in 2018 (too easy to brick your site)
  → Don't implement HPKP in new systems
```

### Issue 6: HSTS and Preloading

```text
HSTS (HTTP Strict Transport Security):
  HTTP header: Strict-Transport-Security: max-age=31536000; includeSubDomains; preload
  
  Effect: browser remembers "this domain is HTTPS only" for max-age seconds.
          Even if user types http://... browser redirects to HTTPS automatically.
          
Risk: if you include preload AND submit to the HSTS preload list:
  → Your domain is hardcoded in Chrome/Firefox binaries
  → Even a new browser that has never visited your site will use HTTPS
  → If your certificate expires and you can't renew → SITE IS UNREACHABLE
  → Getting removed from preload list takes months
  
Safe approach:
  - Start with max-age=300 (5 minutes) to test
  - Increase to max-age=3600 (1 hour), then max-age=86400, then max-age=31536000
  - Only add preload if you're CERTAIN you will always serve HTTPS
```

### Issue 7: Certificate in the Wrong Trust Store

```text
"Works in browser but curl fails" or "Works in Java but Python fails":
  Each application may have its own trust store:
  
  System trust store:    /etc/ssl/certs/ (Linux), Keychain (macOS)
  Python (requests):     uses certifi package (bundled Mozilla CA list)
  Java:                  $JAVA_HOME/lib/security/cacerts
  curl:                  uses system trust store OR its own (depends on compile flags)
  Node.js:               uses bundled Mozilla list (NODE_EXTRA_CA_CERTS env var for custom)
  
  Fix internal CA:
    Linux: sudo cp rootCA.crt /usr/local/share/ca-certificates/ && sudo update-ca-certificates
    Python: export REQUESTS_CA_BUNDLE=/path/to/rootCA.crt
    Java:   keytool -import -alias myca -keystore $JAVA_HOME/lib/security/cacerts -file rootCA.crt
    Node:   export NODE_EXTRA_CA_CERTS=/path/to/rootCA.crt
    curl:   curl --cacert rootCA.crt https://...
```

---

## 20. Interview Q&A: Beginner to Pro

### Beginner

**Q: What is a TLS/SSL certificate?**

An X.509 digital certificate that binds a domain name to a public key, signed by a trusted Certificate Authority. It enables TLS — encrypting traffic between browser and server, and proving the server's identity. The signature from the CA is what makes it trusted: clients already have the CA's public key in their trust store and use it to verify the signature.

---

**Q: What is the difference between TLS and SSL?**

SSL (Secure Sockets Layer) was the original protocol — SSL 2.0 (1995), SSL 3.0 (1996). TLS (Transport Layer Security) is its replacement: TLS 1.0 (1999), 1.1, 1.2 (2008), 1.3 (2018). SSL 2.0, 3.0 and TLS 1.0/1.1 are deprecated due to vulnerabilities. People still say "SSL certificate" colloquially but technically all modern certificates are used with TLS. The protocol negotiated is always TLS 1.2 or TLS 1.3 in modern systems.

---

**Q: What is a CA?**

A Certificate Authority is an entity trusted to sign digital certificates. It maintains a key pair; its public key is distributed in browser/OS trust stores, and its private key is used to sign certificates for domain owners who have proven their identity through domain validation (DV), organizational validation (OV), or extended validation (EV).

---

**Q: What is the difference between a root CA and an intermediate CA?**

Root CA: the top-level trust anchor. Its certificate is self-signed and pre-installed in OS/browser trust stores. Its private key is kept offline (air-gapped, HSM-protected) to minimize exposure.

Intermediate CA: signed by the root CA. Used for day-to-day certificate issuance. If an intermediate is compromised, you revoke just that intermediate without affecting the root. Leaf certificates are signed by the intermediate.

---

**Q: What is a CSR?**

A Certificate Signing Request. A file generated by the server containing: the server's public key, the requested CN/SAN domains, and organizational information — all signed by the server's private key (proves the requester holds the private key matching the public key in the CSR). The server sends the CSR to a CA; the CA validates the domain/org and returns a signed certificate. The private key never leaves the server.

---

**Q: What is the difference between PEM and DER?**

Both represent X.509 data structures (ASN.1 encoding). DER is the raw binary form. PEM is DER encoded in base64, wrapped in `-----BEGIN CERTIFICATE-----` and `-----END CERTIFICATE-----` headers. PEM is human-readable (as text files), and can contain multiple certs (just concatenate). Most Linux tools (nginx, openssl) default to PEM.

---

### Intermediate

**Q: Walk me through TLS handshake.**

1. Client sends ClientHello (supported TLS versions, cipher suites, ECDH key share, SNI).
2. Server responds with ServerHello (selected cipher suite, its ECDH key share), then sends its certificate chain, a CertificateVerify (proving it holds the private key), and Finished.
3. Client validates the certificate chain (trust chain → hostname → validity → revocation), verifies the server's signature.
4. Both sides use their ECDH parameters to derive the same session key independently.
5. Symmetric encryption (AES-GCM) is used for all subsequent data — faster than asymmetric.

---

**Q: How does OCSP stapling work and why is it better than plain OCSP?**

Plain OCSP: the browser queries the CA's OCSP server during every TLS handshake to check revocation — adds latency, leaks which sites you visit to the CA, and fails soft if the OCSP server is down.

OCSP Stapling: the web server pre-fetches its own OCSP response from the CA, caches it, and "staples" it to the TLS handshake. The browser receives the (CA-signed) OCSP response alongside the certificate, can verify it locally, and never needs to contact the CA directly. No privacy leak, no latency, no soft-fail.

---

**Q: What is Certificate Transparency and why was it introduced?**

CT was introduced to prevent rogue or misbehaving CAs from issuing certificates for domains they shouldn't (e.g., a CA secretly issuing a cert for google.com to enable a surveillance MITM). Every certificate issued must be logged in at least two public CT logs (operated by Google, Cloudflare, etc.). The CA embeds Signed Certificate Timestamps (SCTs) from the logs into the certificate. Chrome requires valid SCTs since 2018 — certs without SCTs are rejected. Domain owners can monitor CT logs (crt.sh, Google's monitor) for unauthorized issuance.

---

**Q: What's the difference between an HTTP-01 and DNS-01 ACME challenge?**

HTTP-01: proves domain control by serving a token at `http://<domain>/.well-known/acme-challenge/<token>`. Requires port 80 open, accessible from the internet. Cannot issue wildcards.

DNS-01: proves domain control by creating a `_acme-challenge.<domain>` TXT record with the token. Works for internal servers, private networks, wildcard certs. Requires DNS API automation (Route53, Cloudflare API) for fully automated renewal.

---

**Q: What does cert-manager do in Kubernetes?**

cert-manager is a Kubernetes controller that automates certificate lifecycle management. You declare a `Certificate` resource (domain, issuer reference, secret name, renewal threshold), and cert-manager handles: requesting the cert from the issuer (ACME, internal CA, Vault), solving ACME challenges (HTTP-01 via Ingress annotation, DNS-01 via Route53/Cloudflare), storing the cert+key in a Kubernetes Secret, and automatically renewing before the `renewBefore` threshold. The application just mounts the Secret as a volume or environment variable.

---

### Senior

**Q: How would you implement zero-downtime certificate rotation for a production nginx service?**

1. Generate a new private key and request a new certificate (from Let's Encrypt or internal CA) while the old one is still valid.
2. Deploy the new cert and key to `/etc/ssl/certs/new.crt` and `/etc/ssl/private/new.key`.
3. Update nginx config to reference the new files.
4. Run `nginx -t` to validate config.
5. Run `nginx -s reload` — nginx does a graceful reload: new worker processes start with the new certificate, old workers finish in-flight requests. Zero dropped connections.
6. Optionally, for load-balanced setups: rotate one instance at a time (rolling reload), verify each instance before proceeding.

For ACM+ALB: `create_before_destroy = true` in Terraform, add new cert to listener before removing old one.

---

**Q: What is mTLS and when would you use it instead of API keys?**

mTLS (Mutual TLS): both client and server authenticate each other with X.509 certificates during the TLS handshake. The server verifies the client's certificate was signed by a trusted CA, and the client verifies the server's certificate.

Use mTLS when:
- API keys can be stolen from logs/environment variables (certs are harder to extract)
- You need cryptographic proof of client identity (not just a shared secret)
- Service-to-service communication in a zero-trust network (Istio/Linkerd handle it transparently)
- IoT device identity (each device has a unique cert, revocable individually)
- High-compliance environments (FIPS, PCI-DSS) requiring non-secret-based auth

mTLS is stronger than API keys because the private key never leaves the client — authentication is based on a cryptographic challenge, not a transferable secret.

---

**Q: How does HashiCorp Vault's PKI engine improve certificate security over traditional CAs?**

Traditional CAs issue long-lived certs (1-2 years). If the private key is compromised, there's a 1-year window of risk, and revocation via CRL/OCSP is slow and often ignored (OCSP soft-fail).

Vault PKI issues short-lived certs (hours, days) on demand. The application requests a cert from Vault's API on startup. If the cert is compromised, it expires quickly. No revocation needed — just let it expire. Vault logs every issuance request (audit trail), integrates with Kubernetes auth (IRSA/pod identity), and the private key is only ever in the application's memory — never stored permanently.

---

### Pro / MAANG

**Q: Explain the full trust chain for `https://yourbank.com`. What happens from DNS lookup to first HTTP byte?**

1. DNS resolution: client queries DNS for `yourbank.com` → A/AAAA record.
2. TCP connection: 3-way handshake to server IP:443.
3. TLS ClientHello: client sends supported versions, cipher suites, ECDH key share, SNI=`yourbank.com`.
4. Server selects cipher suite, responds with ECDH share, then sends: leaf cert (CN=yourbank.com) + intermediate CA cert.
5. Client builds chain: leaf → intermediate → root. Finds root in its trust store.
6. Chain signature validation: each cert's signature is verified with the issuer's public key.
7. Hostname check: `yourbank.com` matches a SAN in the leaf cert.
8. Validity check: current time is within notBefore/notAfter.
9. Revocation: client checks OCSP (or reads stapled response). Verifies cert isn't revoked.
10. CT check: verifies cert has valid SCTs from two or more logs.
11. CertificateVerify: client verifies server's digital signature with the server's public key (proves server holds the private key matching the cert's public key).
12. Session key derivation: both sides derive symmetric session key from ECDH parameters.
13. Encrypted: application data (HTTP request) flows over AES-GCM.

---

**Q: A high-traffic service has a certificate that's about to expire. You can't take downtime. Walk me through the rotation.**

1. Generate new private key on the server (never copy the old one).
2. Generate CSR with the same SANs.
3. Request new cert from CA (ACM, Let's Encrypt, or internal CA). If Let's Encrypt: `certbot renew` handles this automatically with HTTP-01 or DNS-01.
4. If multiple servers behind a load balancer: rotate one server at a time.
   - Server 1: place new cert/key, `nginx -t`, `nginx -s reload`. nginx reloads gracefully — no dropped connections.
   - Verify: `openssl s_client -connect server1:443` → check new expiry.
   - Repeat for remaining servers.
5. If ALB+ACM: use `create_before_destroy` Terraform lifecycle: issue new cert, validate DNS, attach to listener, verify, remove old cert ARN from listener.
6. Monitor error rate and TLS handshake success during rotation.
7. Revoke old cert if warranted (key compromise). Otherwise let it expire.

---

**Q: What are the security implications of CT logs? Can an attacker use them?**

CT logs are public and searchable (crt.sh). Implications:
- Attackers enumerate all subdomains of a target: search `%.yourcompany.com` on crt.sh → all SANs ever certified are visible. Automated subdomain discovery in recon phase.
- Mitigation: this is a known trade-off. Don't treat internal subdomain names as secrets. Use Split-horizon DNS and firewalls — not obscurity — for protecting internal services.
- For monitoring: subscribe to CT alerts for your domain. Any unauthorized certificate issuance is visible within seconds.
- CAA records prevent unauthorized issuance for domains you control.

---

## 21. Cheatsheet: All Commands

### OpenSSL Reference

```bash
# ── KEY GENERATION ──────────────────────────────────────────────────────────
openssl genrsa -out private.key 2048                  # RSA 2048
openssl genrsa -out private.key 4096                  # RSA 4096
openssl ecparam -name prime256v1 -genkey -noout -out private.key  # ECDSA P-256
openssl genrsa -aes256 -out private.key 2048          # RSA encrypted with passphrase

# Remove passphrase from key
openssl rsa -in encrypted.key -out unencrypted.key

# ── CSR ─────────────────────────────────────────────────────────────────────
openssl req -new -key private.key -out request.csr
openssl req -new -key private.key -out request.csr -subj "/CN=mysite.com"
openssl req -in request.csr -text -noout              # view CSR
openssl req -in request.csr -verify                   # verify CSR signature

# ── CERTIFICATES ─────────────────────────────────────────────────────────────
# Self-signed
openssl req -new -x509 -key private.key -out cert.pem -days 365

# Sign CSR with CA
openssl x509 -req -in request.csr -CA ca.crt -CAkey ca.key -CAcreateserial -out cert.pem -days 365 -sha256

# View certificate
openssl x509 -in cert.pem -text -noout
openssl x509 -in cert.pem -noout -subject -issuer -dates  # quick summary
openssl x509 -in cert.pem -noout -dates              # validity dates only
openssl x509 -in cert.pem -noout -fingerprint -sha256 # SHA256 fingerprint
openssl x509 -in cert.pem -checkend 2592000 -noout   # expires in 30 days?

# ── LIVE SITE INSPECTION ─────────────────────────────────────────────────────
openssl s_client -connect mysite.com:443 -servername mysite.com < /dev/null
openssl s_client -connect mysite.com:443 -showcerts < /dev/null  # full chain
echo | openssl s_client -connect mysite.com:443 2>/dev/null | openssl x509 -noout -dates
openssl s_client -connect mysite.com:443 -status < /dev/null 2>&1 | grep -A 10 OCSP

# ── CHAIN VERIFICATION ───────────────────────────────────────────────────────
openssl verify -CAfile ca.crt cert.pem
openssl verify -CAfile root.crt -untrusted intermediate.crt leaf.crt
openssl s_client -connect mysite.com:443 -CAfile /etc/ssl/certs/ca-certificates.crt < /dev/null

# ── FORMAT CONVERSION ─────────────────────────────────────────────────────────
openssl x509 -in cert.pem -outform DER -out cert.der     # PEM → DER
openssl x509 -in cert.der -inform DER -outform PEM -out cert.pem  # DER → PEM
openssl pkcs12 -export -out bundle.pfx -inkey key.pem -in cert.pem -certfile chain.pem  # PEM → PFX
openssl pkcs12 -in bundle.pfx -nokeys -out cert.pem      # PFX → cert PEM
openssl pkcs12 -in bundle.pfx -nocerts -nodes -out key.pem  # PFX → key PEM
openssl pkcs12 -in bundle.pfx -info -noout               # view PFX contents

# ── KEY/CERT CONSISTENCY CHECK ───────────────────────────────────────────────
# These should return the same modulus hash if key and cert match:
openssl rsa  -noout -modulus -in private.key | openssl md5
openssl x509 -noout -modulus -in cert.pem    | openssl md5
openssl req  -noout -modulus -in request.csr | openssl md5
```

### certbot / Let's Encrypt

```bash
certbot --nginx -d mysite.com -d www.mysite.com         # nginx auto-config
certbot certonly --standalone -d mysite.com             # manual (no web server)
certbot certonly --webroot -w /var/www/html -d mysite.com  # webroot method
certbot certonly --dns-route53 -d '*.mysite.com' -d mysite.com  # wildcard via Route53
certbot renew --dry-run                                 # test renewal
certbot renew --force-renewal                           # force renew now
certbot certificates                                    # list certs
certbot delete --cert-name mysite.com                   # remove cert
```

### cert-manager kubectl

```bash
kubectl get certificate -A                              # list all certs
kubectl get certificate -n mynamespace                  # namespace certs
kubectl describe certificate mycert -n mynamespace      # full details
kubectl get secret mycert-tls -n mynamespace -o jsonpath='{.data.tls\.crt}' | base64 -d | openssl x509 -noout -dates
kubectl get certificaterequest -A                       # pending requests
kubectl get order -A                                    # ACME orders
kubectl get challenge -A                                # ACME challenges
kubectl describe challenge -A                           # debug ACME failures
kubectl logs -n cert-manager deploy/cert-manager --tail=50  # controller logs
```

### AWS ACM CLI

```bash
aws acm list-certificates --region us-east-1
aws acm describe-certificate --certificate-arn arn:... --region us-east-1
aws acm request-certificate --domain-name mysite.com --validation-method DNS --region us-east-1
aws acm delete-certificate --certificate-arn arn:... --region us-east-1
aws acm import-certificate --certificate fileb://cert.pem --private-key fileb://key.pem --certificate-chain fileb://chain.pem  # import existing cert
```

### Vault PKI CLI

```bash
vault secrets enable pki
vault secrets tune -max-lease-ttl=87600h pki
vault write pki/root/generate/internal common_name="My Root CA" ttl=87600h
vault secrets enable -path=pki_int pki
vault write pki_int/roles/my-role allowed_domains="svc.cluster.local" allow_subdomains=true max_ttl=24h
vault write pki_int/issue/my-role common_name="myapp.svc.cluster.local" ttl=1h
vault write pki/revoke serial_number=<serial>
vault write pki/tidy safety_buffer=24h tidy_cert_store=true tidy_revoked_certs=true
```

### Monitoring: Check Certificate Expiry (Shell Script)

```bash
#!/bin/bash
# cert-expiry-check.sh — alert if cert expires within WARN_DAYS

WARN_DAYS=30
DOMAINS=("yourbank.com" "api.yourbank.com" "mail.yourbank.com")

for domain in "${DOMAINS[@]}"; do
  expiry=$(echo | openssl s_client -servername "$domain" -connect "$domain":443 2>/dev/null \
    | openssl x509 -noout -dates 2>/dev/null \
    | grep notAfter \
    | cut -d= -f2)
  
  if [ -z "$expiry" ]; then
    echo "ERROR: Could not retrieve cert for $domain"
    continue
  fi
  
  expiry_epoch=$(date -d "$expiry" +%s 2>/dev/null || date -j -f "%b %d %T %Y %Z" "$expiry" +%s)
  now_epoch=$(date +%s)
  days_left=$(( (expiry_epoch - now_epoch) / 86400 ))
  
  if [ "$days_left" -lt "$WARN_DAYS" ]; then
    echo "WARNING: $domain expires in $days_left days ($expiry)"
  else
    echo "OK: $domain expires in $days_left days ($expiry)"
  fi
done
```

---

*Track covers: X.509 certificates, PKI hierarchy, TLS 1.3 handshake, browser verification, OCSP/CRL/OCSP-Stapling, Certificate Transparency, CAA records, certificate types (DV/OV/EV/Wildcard/SAN), file formats (PEM/DER/PFX/JKS), OpenSSL (keys/CSR/signing/conversion/inspection), Let's Encrypt/ACME (HTTP-01/DNS-01), AWS ACM, certificate rotation (pre-expiry/automated/zero-downtime/emergency), cert-manager (ClusterIssuer/Certificate/Ingress annotations/DNS-01/internal CA), mTLS (client certs/nginx/Istio), HashiCorp Vault PKI (dynamic short-lived certs), production anti-patterns, interview Q&A (beginner → MAANG), full command cheatsheet.*
