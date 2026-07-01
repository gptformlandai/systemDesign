# Data Encoding, Hashing & Cryptography — Beginner to Mastery

> **Scope:** Every transformation data undergoes before storage or transmission — character encoding, binary encoding (Base64, Hex), URL/HTML encoding, serialization formats, hashing, password hashing, HMAC, symmetric/asymmetric encryption, key derivation, digital signatures, JWT internals, key management, and System Design interview decisions. Each section builds on the last; start from Section 1 and progress through to Section 22.

---

## Table of Contents

1. [The Master Mental Model — Four Operations, One Decision Tree](#1-the-master-mental-model)
2. [Character Encoding — ASCII, Unicode, UTF-8, UTF-16](#2-character-encoding)
3. [Binary-to-Text Encoding — Base64, Base64URL, Base32, Hex](#3-binary-to-text-encoding)
4. [URL Encoding (Percent-Encoding)](#4-url-encoding)
5. [HTML Encoding and Context-Aware Escaping](#5-html-encoding)
6. [Serialization Formats — JSON, XML, Protobuf, Avro, MessagePack](#6-serialization-formats)
7. [Hashing Fundamentals — Properties, SHA Family, MD5 Legacy](#7-hashing-fundamentals)
8. [Password Hashing — bcrypt, Argon2, scrypt, PBKDF2](#8-password-hashing)
9. [HMAC — Message Authentication and Request Signing](#9-hmac)
10. [Symmetric Encryption — AES-GCM, ChaCha20-Poly1305](#10-symmetric-encryption)
11. [Asymmetric Encryption — RSA, Elliptic Curves, Hybrid Pattern](#11-asymmetric-encryption)
12. [Key Derivation Functions — HKDF, PBKDF2, scrypt](#12-key-derivation-functions)
13. [Digital Signatures — RSA-PSS, ECDSA, EdDSA](#13-digital-signatures)
14. [JWT Deep Dive — Structure, Algorithms, JWE, Attacks](#14-jwt-deep-dive)
15. [TLS and the Full Cryptographic Handshake](#15-tls-cryptographic-handshake)
16. [Key Management — KMS, Vault, Envelope Encryption](#16-key-management)
17. [Data-at-Rest Encryption — Disk, File, Column, TDE](#17-data-at-rest-encryption)
18. [Data-in-Transit — TLS, mTLS, SSH, SFTP/FTPS](#18-data-in-transit)
19. [System Design Interview Angle — Decisions and Architecture](#19-system-design-interview-angle)
20. [Common Bugs and Anti-Patterns](#20-common-bugs-and-anti-patterns)
21. [Interview Q&A — Beginner to MAANG Level](#21-interview-qa)
22. [Master Cheatsheet — Decision Trees, Tables, Code Reference](#22-master-cheatsheet)

---

## 1. The Master Mental Model

### The Four Core Operations

Before writing a single line of crypto code, you must know which operation you actually need. Most security bugs come from choosing the wrong operation.

```
DATA TRANSFORMATION DECISION TREE
──────────────────────────────────

Q1: Do you need to RECOVER the original data?
  ├── YES → Q2
  └── NO  → Use HASHING (one-way, irreversible)
              └── Is it a password? → bcrypt/Argon2 (not plain SHA-256)

Q2: Does the SAME party that encrypted also decrypt?
  ├── YES (single party, one secret key) → SYMMETRIC ENCRYPTION (AES-GCM)
  └── NO  (two different parties, key exchange needed) → ASYMMETRIC ENCRYPTION (RSA/EC)
        └── Large data? → Hybrid: RSA wraps AES key, AES encrypts data

Q3: Do you just need to PROVE the data wasn't tampered?
  ├── Shared secret → HMAC (fast, symmetric proof)
  └── Public verifiable → DIGITAL SIGNATURE (RSA/ECDSA/EdDSA)

Q4: Do you need to REPRESENT binary data as text?
  └── Use ENCODING (Base64, Hex, URL-encoding) — NOT security, just format
```

### The Vocabulary Clarity Test

| Term | One-line definition | Reversible? | Needs key? |
|---|---|---|---|
| **Encoding** | Format change (binary → text) | Yes (by design) | No |
| **Hashing** | One-way fingerprint | No | No (salt, not key) |
| **Encryption** | Confidential hiding | Yes | Yes |
| **HMAC** | Tamper-proof tag | No (tag) | Yes (shared secret) |
| **Signature** | Non-repudiable proof | No (sig) | Yes (private key) |
| **Serialization** | Object → bytes/string | Yes | No |

> **The cardinal mistake:** Using Base64 for "encryption". Base64 is reversible by anyone with `atob()`. Using MD5/SHA-256 without salt for passwords. Using SHA-256 when you need AES-GCM. Know which quadrant you're in before you start.

---

## 2. Character Encoding

### Why This Exists

Computers store bytes (0–255). Humans write text. Character encoding is the mapping between the two. Get it wrong and you get `Ã©` instead of `é` — called **mojibake**.

### ASCII (1963)

- 7-bit encoding → 128 characters (0–127)
- US English only: A-Z, a-z, 0-9, punctuation, control chars (tab, newline, null)
- 95 printable characters
- Still the subset that works everywhere

```
'A' = 65 = 0x41 = 0b01000001
'a' = 97 = 0x61 = 0b01100001
'0' = 48 = 0x30
' ' = 32 = 0x20
```

### Latin-1 / ISO 8859-1 (1987)

- 8-bit → 256 characters
- Adds é, ñ, ü, £, © etc. (Western European)
- Still common in legacy HTTP headers, email

### Unicode (1991+)

Unicode is a **code point** system — every character in every language gets a number called a **code point**, written as `U+XXXX`.

```
U+0041 = 'A'       (Latin capital letter A)
U+00E9 = 'é'       (Latin small letter e with acute)
U+4E2D = '中'      (CJK ideograph "middle")
U+1F600 = '😀'    (grinning face emoji, U+1F600)
```

Unicode defines ~140,000+ code points. The question is *how to encode those code points as bytes* — that's where UTF-8/16/32 come in.

### UTF-8 (Variable-Width, Default on the Web)

- **1 byte** for ASCII (U+0000–U+007F) → backwards compatible
- **2 bytes** for Latin, Greek, Cyrillic, etc. (U+0080–U+07FF)
- **3 bytes** for CJK, Emojis level 1 (U+0800–U+FFFF)
- **4 bytes** for supplementary planes including most emojis (U+10000–U+10FFFF)

```python
s = "A é 中 😀"

# UTF-8 byte lengths
b = s.encode('utf-8')
print(list(b))
# [65,  32,  195,169,  32,  228,184,173,  32,  240,159,152,128]
#  'A'  ' '  'é'(2B)  ' '  '中'(3B)       ' '  '😀'(4B)

print(len(s))       # 5 characters
print(len(b))       # 13 bytes
```

### UTF-16

- 2 bytes for Basic Multilingual Plane (U+0000–U+FFFF)
- 4 bytes (surrogate pairs) for supplementary planes
- Used internally by JavaScript, Java, C# (their `string` type is UTF-16)
- Has Byte Order Mark (BOM): `U+FEFF` at start, declares endianness

### UTF-32 (UCS-4)

- Fixed 4 bytes per code point — simple but wasteful
- Used internally in Python 3 (CPython uses variable-width internally but exposes full code points)
- Rare in transit/storage

### BOM (Byte Order Mark)

- UTF-8 BOM: `EF BB BF` (not required, often causes problems — avoid in UTF-8)
- UTF-16 BOM: `FF FE` (little-endian) or `FE FF` (big-endian)

### Practical: Reading/Writing Files Correctly

```python
# Always specify encoding explicitly
with open("file.txt", "r", encoding="utf-8") as f:
    content = f.read()

# Node.js
const content = fs.readFileSync("file.txt", { encoding: "utf-8" });

# Java
Files.readString(Path.of("file.txt"), StandardCharsets.UTF_8);

# SQL — always set charset
CREATE DATABASE mydb CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
# utf8mb4 is true UTF-8 in MySQL (the old 'utf8' was only 3-byte, broken for emojis)
```

### Common Mojibake Causes

| Bug | Cause | Fix |
|---|---|---|
| `Ã©` instead of `é` | UTF-8 file read as Latin-1 | Force UTF-8 on read |
| `?` boxes | UTF-8 stored in Latin-1 DB column | Use utf8mb4 column |
| `\u00e9` in JSON | JSON string escaping code point | Normal — JSON spec allows this |
| Emoji breaks MySQL | Using `utf8` (3-byte) not `utf8mb4` | Migrate to `utf8mb4` |

---

## 3. Binary-to-Text Encoding

### Why Binary-to-Text Encoding Exists

Many transport protocols (SMTP email, JSON, HTTP headers, HTML) were designed for **ASCII text**. Binary data (images, encrypted blobs, hashes) cannot be embedded directly. Binary-to-text encoding converts arbitrary bytes into a safe printable subset.

> **Key rule:** Encoding is NOT security. It is format conversion. Anyone can decode Base64 in one line.

### Base64

**Concept:** Take 3 bytes (24 bits), split into four 6-bit groups (0–63), map each to a character from a 64-character alphabet.

```
Alphabet: A-Z (0-25), a-z (26-51), 0-9 (52-61), + (62), / (63)
Padding:  = used to pad output to multiple of 4 chars
```

**Expansion:** 3 bytes → 4 chars → **33% size increase**

```javascript
// JavaScript (browser + Node)
const b64 = btoa("Hello World");         // "SGVsbG8gV29ybGQ="
const str = atob("SGVsbG8gV29ybGQ=");    // "Hello World"

// For binary data in Node.js
const buf = Buffer.from([0xDE, 0xAD, 0xBE, 0xEF]);
const b64 = buf.toString("base64");     // "3q2+7w=="
const back = Buffer.from(b64, "base64"); // <Buffer de ad be ef>
```

```python
import base64

b64 = base64.b64encode(b"Hello World")  # b'SGVsbG8gV29ybGQ='
raw = base64.b64decode(b"SGVsbG8gV29ybGQ=")  # b'Hello World'

# Encode arbitrary binary
import os
key = os.urandom(32)               # 32 random bytes
b64_key = base64.b64encode(key)    # safe to store in env var
```

```java
// Java 8+
import java.util.Base64;

byte[] bytes = "Hello World".getBytes(StandardCharsets.UTF_8);
String b64 = Base64.getEncoder().encodeToString(bytes);    // SGVsbG8gV29ybGQ=
byte[] decoded = Base64.getDecoder().decode(b64);
```

### Base64URL (URL-Safe Base64)

Standard Base64 uses `+` and `/` which are special in URLs. **Base64URL** replaces them:

```
+ → -
/ → _
= (padding) → omitted or replaced with %3D
```

Used in: **JWT tokens**, OAuth tokens, URL-safe identifiers, browser cookies.

```python
import base64

token_bytes = os.urandom(32)
b64url = base64.urlsafe_b64encode(token_bytes).rstrip(b"=")  # strip padding for JWT
# Decode: add padding back
padded = b64url + b"==" * (len(b64url) % 4 != 0)
decoded = base64.urlsafe_b64decode(padded + b"==")
```

```javascript
// Node.js Base64URL
function toBase64URL(buffer) {
  return buffer.toString("base64")
    .replace(/\+/g, "-")
    .replace(/\//g, "_")
    .replace(/=/g, "");
}

function fromBase64URL(str) {
  const padded = str + "=".repeat((4 - str.length % 4) % 4);
  return Buffer.from(padded.replace(/-/g, "+").replace(/_/g, "/"), "base64");
}
```

### Decoding a JWT by Hand

```bash
# JWT = header.payload.signature (all Base64URL encoded)
TOKEN="eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkpvaG4gRG9lIn0.SflKxwRJSMeKKF2QT4fwpMeJf36POk6yJV_adQssw5c"

HEADER=$(echo $TOKEN | cut -d'.' -f1)
PAYLOAD=$(echo $TOKEN | cut -d'.' -f2)

# Add padding and decode
echo "$HEADER==" | base64 -d 2>/dev/null
# {"alg":"HS256","typ":"JWT"}

echo "$PAYLOAD==" | base64 -d 2>/dev/null
# {"sub":"1234567890","name":"John Doe"}
```

### Base32

- 8 bytes → 13 chars (60% overhead vs Base64's 33%)
- Alphabet: A-Z + 2-7 (case-insensitive, no confusable chars like 0/O, 1/I)
- Used in: **TOTP (Time-based OTP / Google Authenticator secrets)**, DNS names, some filesystems

```python
import base64
import pyotp

# TOTP secret is Base32
secret = pyotp.random_base32()        # e.g. "JBSWY3DPEHPK3PXP"
totp = pyotp.TOTP(secret)
print(totp.now())                     # 6-digit OTP valid for 30 seconds

# Manual Base32
b32 = base64.b32encode(b"Hello")      # b'JBSWY3DP'
raw = base64.b32decode(b"JBSWY3DP")  # b'Hello'
```

### Hexadecimal (Base16)

- Each byte → 2 hex digits (0-9, a-f)
- 100% overhead (2× size of binary)
- Used for: checksums, cryptographic hash output, MAC addresses, colors (#FF5733), binary protocols

```python
import hashlib

digest = hashlib.sha256(b"hello").digest()         # 32 bytes (binary)
hex_str = digest.hex()                              # 64-char hex string
# "2cf24dba5fb0a30e26e83b2ac5b9e29e1b161e5c1fa7425e73043362938b9824"

# Convert back
back = bytes.fromhex(hex_str)
```

```javascript
// Node.js
const hash = crypto.createHash("sha256").update("hello").digest("hex");
// "2cf24dba5fb0a30e26e83b2ac5b9e29e1b161e5c1fa7425e73043362938b9824"

// hex ↔ buffer
const buf = Buffer.from(hash, "hex");
const hexBack = buf.toString("hex");
```

### Comparison Table

| Encoding | Alphabet size | Overhead | Padding | Use case |
|---|---|---|---|---|
| Hex (Base16) | 16 | +100% | None | Hash output, checksums, colors |
| Base32 | 32 | +60% | `=` | TOTP secrets, DNS-safe |
| Base64 | 64 | +33% | `=` | Emails, JSON blobs, images in CSS |
| Base64URL | 64 | +33% | None | JWT, OAuth tokens, URL params |

---

## 4. URL Encoding

### Why Percent-Encoding Exists

URLs have a restricted character set. Characters outside `A-Z a-z 0-9 - _ . ~` (unreserved) and characters with special meaning (`:`, `/`, `?`, `#`, `&`, `=`) must be encoded when used in a different context.

Format: `%` + two uppercase hex digits of the byte value.

```
'space' = %20
'é'     = %C3%A9  (UTF-8 bytes of é: 0xC3 0xA9)
'/'     = %2F     (when in path segment, not separator)
'&'     = %26     (when in query value, not param separator)
'='     = %3D     (when in query value, not key=value delimiter)
'+'     = %2B     (note: '+' in query strings means space in old form-style)
```

### The `+` vs `%20` Confusion

Two encoding schemes exist for query strings:

- **`application/x-www-form-urlencoded`** (HTML forms): spaces become `+`, others percent-encoded
- **RFC 3986 percent-encoding** (correct URI standard): spaces become `%20`, `+` is literal

```javascript
// JavaScript — know which one you're using
encodeURIComponent("hello world & more")
// "hello%20world%20%26%20more"  ← RFC 3986 (correct for query values)

encodeURI("https://example.com/path with spaces")
// "https://example.com/path%20with%20spaces"  ← encodes path, NOT query params

new URLSearchParams({ q: "hello world & more" }).toString()
// "q=hello+world+%26+more"  ← application/x-www-form-urlencoded (forms)
```

```python
from urllib.parse import quote, quote_plus, urlencode

quote("hello world & more")           # 'hello%20world%20%26%20more'
quote_plus("hello world & more")      # 'hello+world+%26+more'
urlencode({"q": "hello world"})        # 'q=hello+world' (form-encoded)
```

### Double Encoding Attack

When a server decodes a URL twice, a path like `..%252F..%252Fetc%252Fpasswd` decodes to `../../etc/passwd`:

```
First decode:  ..%252F  →  ..%2F
Second decode: ..%2F    →  ../
```

Prevention: decode URL once, then validate. Never pass user-supplied URL-encoded paths to file system or system calls.

### URL Encoding Context Rules

| Context | Encode with |
|---|---|
| Query parameter key | `encodeURIComponent` |
| Query parameter value | `encodeURIComponent` |
| Path segment | `encodeURIComponent` (not `encodeURI`) |
| Full URL | `encodeURI` (leaves `:`, `/`, `?`, `#`, `&` intact) |
| HTML attribute value containing a URL | First URL-encode, then HTML-encode |

---

## 5. HTML Encoding

### Why HTML Encoding Exists

HTML parsers interpret `<`, `>`, `&`, `"`, `'` as markup. When displaying user-supplied text in HTML, these must be escaped to prevent the browser from interpreting them as HTML/JavaScript — the core defense against **XSS (Cross-Site Scripting)**.

### HTML Entities

```
&  → &amp;
<  → &lt;
>  → &gt;
"  → &quot;      (in attribute values)
'  → &#x27; or &apos; (in attribute values)
/  → &#x2F;      (helps close open tags)
```

```javascript
// Node.js — no built-in, use a library
import { escape } from "html-escaper";
const safe = escape("<script>alert('xss')</script>");
// "&lt;script&gt;alert(&#x27;xss&#x27;)&lt;/script&gt;"

// React escapes by default in JSX
const value = "<script>alert('xss')</script>";
return <div>{value}</div>;  // Rendered as text, not HTML — safe
// Only dangerouslySetInnerHTML bypasses this
```

```python
import html

html.escape("<script>alert('xss')</script>")
# '&lt;script&gt;alert(&#x27;xss&#x27;)&lt;/script&gt;'

html.escape("<b>Hello</b>", quote=True)  # quote=True also escapes "
```

### Context-Aware Encoding (Critical)

The same string needs different encoding depending on where it's inserted:

```html
<!-- Context 1: HTML body text → HTML entity encode -->
<div>Hello, &lt;user&gt;</div>

<!-- Context 2: HTML attribute value → HTML entity encode -->
<input value="&lt;user&gt;">

<!-- Context 3: JavaScript string → JS escape (not just HTML entities!) -->
<script>
  var name = "Hello\u003Cuser\u003E";  // JSON.stringify output
</script>

<!-- Context 4: CSS value → CSS escape -->
<style> .name::before { content: "\003C user\003E"; } </style>

<!-- Context 5: URL attribute → URL-encode THEN HTML-encode -->
<a href="/search?q=hello%20world">link</a>
```

> **Wrong approach:** HTML-encoding all output everywhere. The RIGHT approach: use the correct encoding for each context. Template engines like Jinja2, Handlebars, and React's JSX handle this automatically for the most common contexts.

---

## 6. Serialization Formats

### What is Serialization?

Converting an in-memory data structure (object, array, map) to a byte sequence (or string) that can be stored or transmitted. Deserialization is the reverse.

```
[Memory Object] → serialize → [Bytes/String] → transport/store → deserialize → [Memory Object]
```

### JSON

The web's lingua franca. Text-based, human-readable, widely supported.

```json
{
  "id": 12345,
  "name": "Alice",
  "active": true,
  "scores": [98, 87, 95],
  "meta": null
}
```

**Strengths:** Universal support, human-readable, native to JavaScript, simple spec  
**Weaknesses:** No binary type (must Base64-encode), verbose, no schema, no comments, no `Date` type (just strings), numbers lose precision for 64-bit integers

```javascript
// JavaScript
const obj = { id: 1, name: "Alice" };
const json = JSON.stringify(obj);           // '{"id":1,"name":"Alice"}'
const back = JSON.parse(json);             // { id: 1, name: "Alice" }

// BigInt precision problem
JSON.stringify({ id: 9007199254740993n });  // throws — BigInt not supported
JSON.stringify({ id: 9007199254740993 });   // {"id":9007199254740992} ← WRONG
```

```python
import json

obj = {"id": 1, "name": "Alice"}
s = json.dumps(obj)           # '{"id": 1, "name": "Alice"}'
back = json.loads(s)          # {'id': 1, 'name': 'Alice'}

# Custom encoder for datetime
from datetime import datetime
import json

class DateTimeEncoder(json.JSONEncoder):
    def default(self, obj):
        if isinstance(obj, datetime):
            return obj.isoformat()
        return super().default(obj)

json.dumps({"ts": datetime.utcnow()}, cls=DateTimeEncoder)
```

### XML

Verbose but powerful — namespaces, schemas (XSD), XSLT transforms, standard in enterprise/SOAP.

```xml
<user id="12345" active="true">
  <name>Alice</name>
  <scores>
    <score>98</score>
    <score>87</score>
  </scores>
</user>
```

**Strengths:** Rich schema validation (XSD), XSLT transforms, namespaces, widely used in enterprise SOA/SOAP  
**Weaknesses:** Verbose, complex parsing, XXE (XML External Entity) injection risk

```python
import xml.etree.ElementTree as ET
# SAFE: disable external entities
import defusedxml.ElementTree as ET_safe  # pip install defusedxml

tree = ET_safe.fromstring("<user><name>Alice</name></user>")
print(tree.find("name").text)  # Alice
```

### Protocol Buffers (Protobuf)

Google's binary serialization. Define schema in `.proto` files, compile to language bindings.

```protobuf
// user.proto
syntax = "proto3";

message User {
  int64 id = 1;
  string name = 2;
  bool active = 3;
  repeated int32 scores = 4;
}
```

```python
# Compile: protoc --python_out=. user.proto
from user_pb2 import User

user = User(id=12345, name="Alice", active=True, scores=[98, 87, 95])
data = user.SerializeToString()  # compact binary bytes
user2 = User()
user2.ParseFromString(data)
print(user2.name)  # Alice
```

**Strengths:** Compact (3-10× smaller than JSON), fast serialize/deserialize, strongly typed, backward compatible via field numbers, gRPC uses it  
**Weaknesses:** Not human-readable, requires code generation, schema required

### Apache Avro

Row-based binary format with schema embedded in files. Heavily used in Kafka (with Schema Registry).

```json
// Avro schema (JSON-defined)
{
  "type": "record",
  "name": "User",
  "fields": [
    {"name": "id",   "type": "long"},
    {"name": "name", "type": "string"},
    {"name": "active", "type": "boolean", "default": true}
  ]
}
```

**Strengths:** Schema evolution (add/remove fields), schema stored in file, works with Kafka Schema Registry, good for data lakes  
**Weaknesses:** Requires schema to read, less common outside Kafka/Hadoop ecosystem

### MessagePack

Binary JSON — same data model as JSON but binary encoded. Drop-in JSON replacement.

```python
import msgpack

data = {"id": 12345, "name": "Alice", "scores": [98, 87, 95]}
packed = msgpack.packb(data)       # binary, ~30% smaller than JSON
back = msgpack.unpackb(packed, raw=False)
```

**Strengths:** Binary JSON (no schema needed), 20-30% smaller than JSON, fast  
**Weaknesses:** Not human-readable, smaller ecosystem than JSON or Protobuf

### YAML

Human-friendly superset of JSON. Used for config files (Kubernetes, GitHub Actions, Docker Compose).

```yaml
user:
  id: 12345
  name: Alice
  active: true
  scores:
    - 98
    - 87
```

> **Security Warning:** YAML has `!!python/object` and similar tags that can execute code during deserialization. Always use `yaml.safe_load()`, never `yaml.load()`.

```python
import yaml

# DANGEROUS — do not use yaml.load() with untrusted input
# data = yaml.load(untrusted_string)

# SAFE
data = yaml.safe_load(safe_yaml_string)
```

### Format Comparison: System Design Angle

| Format | Size | Speed | Schema | Human-readable | SD Use Case |
|---|---|---|---|---|---|
| JSON | Medium | Medium | No (or JSON Schema) | Yes | REST APIs, config, logs |
| XML | Large | Slow | XSD | Yes | SOAP, enterprise, legacy |
| Protobuf | Small | Very fast | Yes (.proto) | No | gRPC, microservices, mobile |
| Avro | Small | Fast | Yes (JSON schema) | No | Kafka + Schema Registry |
| MessagePack | Medium-small | Fast | No | No | WebSocket binary, cache |
| YAML | Large | Slow | No | Yes | Config files, CI/CD, K8s |
| CBOR | Small | Fast | No | No | IoT, embedded, COSE |

**SD interview answer:** For high-throughput microservices (>10K RPS), choose Protobuf — 3–10× less bandwidth, faster parse. For public APIs, JSON. For Kafka event streaming, Avro + Schema Registry for schema evolution guarantees.

---

## 7. Hashing Fundamentals

### What is a Hash Function?

A deterministic mathematical function that maps arbitrary-length input to a fixed-length output (digest).

```
Hash(input)  =  digest
Hash("hello") = "2cf24dba5fb0a30e..."  (SHA-256, 64 hex chars = 32 bytes)
Hash("hello") = "2cf24dba5fb0a30e..."  (same input → always same output)
Hash("Hello") = "185f8db32921bd46..."  (one char changed → completely different)
```

### Four Properties a Cryptographic Hash Must Have

| Property | Meaning | Broken if... |
|---|---|---|
| **Deterministic** | Same input → same output always | Output varies |
| **Pre-image resistant** | Given `H`, can't find `x` s.t. `Hash(x) = H` | Hash can be reversed |
| **Second pre-image resistant** | Given `x`, can't find `y ≠ x` s.t. `Hash(x) = Hash(y)` | Can forge inputs |
| **Collision resistant** | Can't find any two `x ≠ y` with same hash | Birthday attack succeeds |
| **Avalanche effect** | Small input change → 50% of output bits change | Statistical predictability |

### SHA-2 Family (Current Standard)

```python
import hashlib

# SHA-256: 256-bit (32 bytes) output — most common
h = hashlib.sha256(b"hello world").hexdigest()
# "b94d27b9934d3e08a52e52d7da7dabfac484efe04294e576f4c039e1e5997ea4"

# SHA-512: 512-bit (64 bytes) — faster on 64-bit CPUs for large data
h512 = hashlib.sha512(b"hello world").hexdigest()

# SHA-384: truncated SHA-512 (common in TLS)
h384 = hashlib.sha384(b"hello world").hexdigest()

# Streaming large files (don't load entire file into memory)
sha = hashlib.sha256()
with open("large_file.bin", "rb") as f:
    for chunk in iter(lambda: f.read(65536), b""):
        sha.update(chunk)
digest = sha.hexdigest()
```

```javascript
// Node.js
const crypto = require("crypto");

const hash = crypto.createHash("sha256").update("hello world").digest("hex");

// Streaming
const stream = fs.createReadStream("large_file.bin");
const hash = crypto.createHash("sha256");
stream.pipe(hash);
hash.on("finish", () => {
  console.log(hash.digest("hex"));
});
```

### SHA-3 / Keccak

- Different construction (sponge function, not Merkle-Damgård like SHA-2)
- **SHA3-256, SHA3-512**: standardized NIST alternative to SHA-2
- Ethereum uses **Keccak-256** (slightly different from NIST SHA3-256 — pre-standardization version)
- More resistant to length-extension attacks than SHA-2

```python
import hashlib

sha3 = hashlib.sha3_256(b"hello").hexdigest()
```

### MD5 and SHA-1 — Broken, Do Not Use for Security

| Algorithm | Output | Status | Why broken |
|---|---|---|---|
| MD5 | 128-bit (16 bytes) | **Broken** | Collision in seconds (2^18 ops), identical-prefix attacks |
| SHA-1 | 160-bit (20 bytes) | **Broken** | SHAttered collision (2017, Google), identical-prefix attacks |
| SHA-256 | 256-bit (32 bytes) | Secure | No practical collision known |
| SHA-512 | 512-bit (64 bytes) | Secure | No practical collision known |
| SHA3-256 | 256-bit (32 bytes) | Secure | Different construction, future-proof |

**When is MD5/SHA-1 still OK?**  
- Non-security checksums (detecting accidental corruption, not tampering): MD5 is fine for git's internal content addressing was SHA-1 (now migrating)
- Legacy interoperability (TLS 1.0 fingerprints, some enterprise auth systems) where you can't change the algorithm
- **Never** for password storage, HMAC, or any tamper-detection where adversaries are involved

### Hash Use Cases

| Use case | Algorithm | Notes |
|---|---|---|
| File integrity check | SHA-256 | Ship hash alongside file |
| Content-addressed storage | SHA-256 (or SHA-1 for git) | Filename = hash of content |
| De-duplication | SHA-256 | Same hash = same content |
| Password storage | bcrypt / Argon2 | See Section 8 — never plain SHA |
| API request signing | HMAC-SHA256 | See Section 9 |
| Digital certificate fingerprint | SHA-256 | Browser shows cert SHA-256 |
| Cache busting | MD5 or SHA-1 OK | Not security-critical |
| Bloom filter | MurmurHash / FNV (non-crypto) | Speed matters, not security |

---

## 8. Password Hashing

### Why Plain SHA-256 is Wrong for Passwords

```
Attack: Precomputed Rainbow Tables
SHA-256("password123") = ef92b778bafe771...  (same every time, no salt)

An attacker with a leaked hash database can:
1. Pre-compute SHA-256 of millions of common passwords (rainbow table)
2. Look up the hash → instant password recovery in O(1)

Even with SHA-256: GPU can compute 10 BILLION SHA-256/sec
→ All 8-char passwords cracked in ~3 hours
```

Password hashing requirements:
1. **Slow by design** (configurable work factor) — makes brute force expensive
2. **Salted** — unique per-user random bytes concatenated before hashing — defeats rainbow tables
3. **One-way** — verifier only needs to re-hash and compare

### bcrypt

- Designed 1999, still widely used
- Work factor = cost parameter (2^cost iterations)
- Salt is embedded in the hash output (60-char string)
- Max password length: **72 bytes** (silently truncates beyond that!)

```
$2b$12$LQv3c1yqBWVHxkd0LHAkCOYz6TtxMQJqhN8/lewdBbGF2dAlY8lPe
  |  |  |___________________________|__________________________|
 ver cost           salt (22 chars)         hash (31 chars)
```

```javascript
// Node.js — bcrypt
const bcrypt = require("bcrypt");

// Hash password (async — bcrypt is intentionally slow)
const hash = await bcrypt.hash("userPassword123!", 12);
// 12 = cost factor (2^12 = 4096 iterations) — aim for ~100-300ms

// Verify
const isMatch = await bcrypt.compare("userPassword123!", hash);
// timing-safe comparison built in
```

```python
import bcrypt

hashed = bcrypt.hashpw(b"userPassword123!", bcrypt.gensalt(rounds=12))
is_match = bcrypt.checkpw(b"userPassword123!", hashed)
```

```java
// Spring Security (BCryptPasswordEncoder)
PasswordEncoder encoder = new BCryptPasswordEncoder(12);
String hash = encoder.encode("userPassword123!");
boolean match = encoder.matches("userPassword123!", hash);
```

### Argon2 (NIST 800-63B, Current Recommendation)

Winner of the Password Hashing Competition (2015). Three variants:

- **Argon2i**: Side-channel resistant (constant-time memory access) — good for password hashing
- **Argon2d**: Faster, not side-channel resistant — good for cryptocurrency
- **Argon2id**: Hybrid (first half i, second half d) — **recommended for most uses**

Three tunable parameters:
- **`t` (time cost)**: number of passes
- **`m` (memory cost)**: kilobytes of RAM to use
- **`p` (parallelism)**: threads

```python
# pip install argon2-cffi
from argon2 import PasswordHasher

ph = PasswordHasher(
    time_cost=3,      # 3 iterations
    memory_cost=65536, # 64 MB RAM
    parallelism=4,    # 4 threads
    hash_len=32,
    salt_len=16
)

hash_value = ph.hash("userPassword123!")
# $argon2id$v=19$m=65536,t=3,p=4$...

try:
    ph.verify(hash_value, "userPassword123!")  # True
    # Check if hash needs rehashing (parameters changed)
    if ph.check_needs_rehash(hash_value):
        hash_value = ph.hash("userPassword123!")
except Exception:
    pass  # wrong password
```

### scrypt

Memory-hard by design. Used by Litecoin, some password managers.

```python
import hashlib, os

password = b"userPassword123!"
salt = os.urandom(16)

# n=2^14=16384 (CPU/memory cost), r=8 (block size), p=1 (parallelism)
hashed = hashlib.scrypt(password, salt=salt, n=16384, r=8, p=1, dklen=32)
```

### PBKDF2

- NIST-approved, FIPS-compliant — required in some regulated environments
- Slower iteration: just repeated HMAC, not memory-hard
- Use Argon2id if not compliance-constrained

```python
import hashlib, os

password = b"userPassword123!"
salt = os.urandom(16)
iterations = 600_000  # NIST 800-63B 2023 recommendation for SHA-256

hashed = hashlib.pbkdf2_hmac("sha256", password, salt, iterations, dklen=32)
```

```javascript
const crypto = require("crypto");
const hash = crypto.pbkdf2Sync("userPassword123!", salt, 600_000, 32, "sha256");
```

### Comparison Table

| Algorithm | Memory-hard | Max password len | FIPS | Recommended |
|---|---|---|---|---|
| bcrypt | No (CPU-hard) | 72 bytes | No | Yes (widely supported) |
| Argon2id | Yes | Unlimited | No | **Best choice** |
| scrypt | Yes | Unlimited | No | Good |
| PBKDF2-SHA256 | No | Unlimited | Yes | Yes (if FIPS required) |
| plain SHA-256 | No | Unlimited | No | **NEVER** |
| MD5 | No | Unlimited | No | **NEVER** |

### NIST 800-63B Password Rules (2024)

- Min length: **8 characters** (allow up to 64+)
- **No mandatory complexity rules** (no "must have uppercase + number + symbol")
- **No forced periodic rotation** (rotate only on breach evidence)
- **Check against breached password lists** (Have I Been Pwned API)
- Allow all printable Unicode characters including spaces
- Support multi-factor authentication

---

## 9. HMAC

### What is HMAC?

**Hash-based Message Authentication Code** — proves a message was created by someone with the shared secret key and wasn't tampered with.

```
HMAC(key, message) = Hash( (key XOR opad) || Hash( (key XOR ipad) || message ) )
```

This double-hashing construction defeats **length-extension attacks** that plain `SHA256(key || message)` is vulnerable to.

Properties:
- **Authenticity**: only someone with the key can produce the HMAC
- **Integrity**: any change to the message produces a different HMAC
- **Deterministic**: same key + message = same HMAC
- NOT confidential: message is not hidden

### HMAC in Code

```javascript
const crypto = require("crypto");

const key = crypto.randomBytes(32);
const message = Buffer.from(JSON.stringify({ userId: 42, action: "transfer" }));

const hmac = crypto.createHmac("sha256", key)
  .update(message)
  .digest("hex");
// "a1b2c3d4..."

// Verify — ALWAYS use timingSafeEqual, never ===
function verifyHmac(message, receivedHmac, key) {
  const expected = crypto.createHmac("sha256", key)
    .update(message)
    .digest();
  const received = Buffer.from(receivedHmac, "hex");
  if (received.length !== expected.length) return false;
  return crypto.timingSafeEqual(expected, received);
}
```

```python
import hmac, hashlib, os, secrets

key = os.urandom(32)
message = b'{"userId": 42, "action": "transfer"}'

mac = hmac.new(key, message, hashlib.sha256).hexdigest()

# Verify
def verify_hmac(key, message, received_hex):
    expected = hmac.new(key, message, hashlib.sha256).digest()
    received = bytes.fromhex(received_hex)
    return hmac.compare_digest(expected, received)  # timing-safe
```

### Real-World HMAC Uses

**1. JWT Signatures (HMAC-SHA256)**

```
HMAC-SHA256(
  base64url(header) + "." + base64url(payload),
  secret
) → signature
```

**2. AWS Signature Version 4 (SigV4)**

```
Signing Key = HMAC(HMAC(HMAC(HMAC("AWS4" + secret, date), region), service), "aws4_request")
Signature = HMAC-SHA256(SigningKey, StringToSign)
```

**3. CSRF Token Double-Submit Pattern**

```javascript
// Server: embed HMAC of sessionId in CSRF token
function generateCsrfToken(sessionId, secret) {
  return crypto.createHmac("sha256", secret).update(sessionId).digest("hex");
}

// Server: verify on form submit
function verifyCsrfToken(sessionId, token, secret) {
  const expected = generateCsrfToken(sessionId, secret);
  return crypto.timingSafeEqual(Buffer.from(expected), Buffer.from(token));
}
```

**4. Webhook Signature Verification (GitHub, Stripe)**

```javascript
// Stripe webhook signature verification
function verifyStripeWebhook(payload, signature, secret) {
  const timestamp = signature.split(",")[0].split("=")[1];
  const sigHash = signature.split(",")[1].split("=")[1];
  const signed = `${timestamp}.${payload}`;
  const expected = crypto.createHmac("sha256", secret)
    .update(signed)
    .digest("hex");
  return crypto.timingSafeEqual(
    Buffer.from(expected),
    Buffer.from(sigHash)
  );
}
```

### Timing Attack — Why `!==` is Wrong

```javascript
// VULNERABLE — timing attack: returns faster for wrong chars early
if (computedHmac !== receivedHmac) return false;

// SAFE — always takes same time regardless of where comparison diverges
if (!crypto.timingSafeEqual(Buffer.from(computedHmac), Buffer.from(receivedHmac))) return false;
```

Timing attacks measure how long comparison takes to infer secret values byte-by-byte. Always use `crypto.timingSafeEqual` / `hmac.compare_digest` / `MessageDigest.isEqual` for HMAC/signature comparisons.

---

## 10. Symmetric Encryption

### What is Symmetric Encryption?

Same key encrypts and decrypts. Fast. Suitable for large data. The key must be shared securely out-of-band.

```
Encrypt: plaintext + key → ciphertext (+ authentication tag in AES-GCM)
Decrypt: ciphertext + key + tag → plaintext (or authentication error)
```

### AES (Advanced Encryption Standard)

Block cipher operating on 128-bit (16-byte) blocks. Key sizes: 128, 192, or 256 bits.

**AES-128 vs AES-256:** AES-256 has a longer key but is actually ~40% *slower* due to more rounds. AES-128 is perfectly secure by current standards. Use AES-256 for long-term sensitive data or if you want a security margin.

### Block Cipher Modes — Why Mode Matters

| Mode | Auth? | IV required? | Parallelizable | Use |
|---|---|---|---|---|
| ECB | No | No | Yes | **Never** — identical blocks → identical ciphertext |
| CBC | No (needs HMAC) | Yes (random) | Decrypt only | Legacy, avoid for new code |
| CTR | No (needs HMAC) | Yes (nonce) | Yes | Stream-cipher mode |
| **GCM** | **Yes (AEAD)** | Yes (nonce, 12 bytes) | Yes | **Standard recommendation** |
| CCM | Yes (AEAD) | Yes | No | IoT, constrained devices |

**ECB mode flaw — never use:**

```
ECB encrypts each block independently → same plaintext block = same ciphertext block
The famous "ECB Linux penguin" — image still visible after ECB encryption
```

### AES-GCM — The Gold Standard

AES-GCM = AES in Counter mode (CTR) + Galois Message Authentication Code.

**AEAD (Authenticated Encryption with Associated Data):**
- Confidentiality (CTR mode)
- Integrity + Authenticity (GHASH tag, 128-bit by default)
- Optional **associated data** (authenticated but not encrypted — e.g., recipient ID in header)

**Critical: IV/Nonce must be unique for every encryption with the same key.** Nonce reuse completely breaks GCM (both message and key can be recovered).

```javascript
const crypto = require("crypto");

const ALGORITHM = "aes-256-gcm";
const KEY_LEN = 32;   // 256 bits
const IV_LEN = 12;    // 96-bit nonce for GCM (recommended)
const TAG_LEN = 16;   // 128-bit auth tag

function encrypt(plaintext, key) {
  const iv = crypto.randomBytes(IV_LEN);  // MUST be unique per encryption
  const cipher = crypto.createCipheriv(ALGORITHM, key, iv, { authTagLength: TAG_LEN });

  const encrypted = Buffer.concat([
    cipher.update(Buffer.from(plaintext, "utf8")),
    cipher.final()
  ]);
  const tag = cipher.getAuthTag();

  // Store: iv (12 bytes) || tag (16 bytes) || ciphertext
  return Buffer.concat([iv, tag, encrypted]).toString("base64");
}

function decrypt(ciphertextB64, key) {
  const data = Buffer.from(ciphertextB64, "base64");
  const iv = data.subarray(0, IV_LEN);
  const tag = data.subarray(IV_LEN, IV_LEN + TAG_LEN);
  const ciphertext = data.subarray(IV_LEN + TAG_LEN);

  const decipher = crypto.createDecipheriv(ALGORITHM, key, iv, { authTagLength: TAG_LEN });
  decipher.setAuthTag(tag);

  // If tag verification fails, this throws — message was tampered
  return Buffer.concat([decipher.update(ciphertext), decipher.final()]).toString("utf8");
}

const key = crypto.randomBytes(KEY_LEN);
const ct = encrypt("Hello, World!", key);
console.log(decrypt(ct, key));  // "Hello, World!"
```

```python
from cryptography.hazmat.primitives.ciphers.aead import AESGCM
import os

key = os.urandom(32)  # 256-bit key
nonce = os.urandom(12)  # 96-bit nonce

aesgcm = AESGCM(key)

# Encrypt
ciphertext = aesgcm.encrypt(nonce, b"Hello, World!", None)  # None = no AAD

# Decrypt (raises InvalidTag if tampered)
plaintext = aesgcm.decrypt(nonce, ciphertext, None)
```

### ChaCha20-Poly1305

Alternative to AES-GCM:
- Faster than AES on systems without hardware AES acceleration (older mobile devices)
- **256-bit key only**, 96-bit nonce
- Poly1305 authentication (AEAD)
- Used in TLS 1.3 as the cipher suite `TLS_CHACHA20_POLY1305_SHA256`
- WireGuard VPN uses it exclusively

```python
from cryptography.hazmat.primitives.ciphers.aead import ChaCha20Poly1305
import os

key = ChaCha20Poly1305.generate_key()  # 32 bytes
nonce = os.urandom(12)
chacha = ChaCha20Poly1305(key)

ct = chacha.encrypt(nonce, b"Hello", None)
pt = chacha.decrypt(nonce, ct, None)
```

### Key Generation

```python
import os
import secrets

# For AES-256
key = os.urandom(32)          # cryptographically secure random bytes
key_b64 = base64.b64encode(key).decode()  # store as Base64 in env var / secrets manager

# For passwords/tokens (not encryption keys)
token = secrets.token_hex(32)     # 64-char hex string
token = secrets.token_urlsafe(32) # 43-char Base64URL
```

---

## 11. Asymmetric Encryption

### The Key Insight

Two mathematically linked keys:
- **Public key**: shareable with anyone — used to **encrypt** (or verify signatures)
- **Private key**: kept secret — used to **decrypt** (or create signatures)

```
Alice publishes public key
Bob encrypts message with Alice's public key
Only Alice (with private key) can decrypt
```

Asymmetric encryption is **much slower** than symmetric (~1000× slower for RSA). **Never encrypt large data directly with RSA.** Use the hybrid pattern.

### RSA

Based on difficulty of factoring large integers.

```
Key generation: choose two large primes p, q
n = p × q (modulus)
Public key = (n, e) where e is typically 65537
Private key = (n, d) where d is the modular inverse of e mod (p-1)(q-1)
Encrypt: C = M^e mod n
Decrypt: M = C^d mod n
```

**Key sizes:** 2048-bit minimum today, 3072-bit recommended for post-2030, 4096-bit for high security. (Compare: AES-128 is equivalent strength to RSA-3072.)

**Padding matters:** Never use raw (textbook) RSA.

| Padding | Use case | Notes |
|---|---|---|
| PKCS#1 v1.5 | Encryption/sig | Legacy, vulnerable to Bleichenbacher oracle in encryption |
| **OAEP** | **Encryption** | Secure — use for any RSA encryption |
| **PSS** | **Signatures** | Secure — use for RSA signatures |

```python
from cryptography.hazmat.primitives.asymmetric import rsa, padding
from cryptography.hazmat.primitives import hashes, serialization

# Generate key pair
private_key = rsa.generate_private_key(
    public_exponent=65537,
    key_size=2048
)
public_key = private_key.public_key()

# Encrypt with OAEP padding
ciphertext = public_key.encrypt(
    b"secret message",
    padding.OAEP(
        mgf=padding.MGF1(algorithm=hashes.SHA256()),
        algorithm=hashes.SHA256(),
        label=None
    )
)

# Decrypt
plaintext = private_key.decrypt(
    ciphertext,
    padding.OAEP(
        mgf=padding.MGF1(algorithm=hashes.SHA256()),
        algorithm=hashes.SHA256(),
        label=None
    )
)

# Serialize keys (PEM format)
pem = private_key.private_bytes(
    encoding=serialization.Encoding.PEM,
    format=serialization.PrivateFormat.PKCS8,
    encryption_algorithm=serialization.BestAvailableEncryption(b"passphrase")
)
```

### Elliptic Curve Cryptography (ECC)

Based on difficulty of elliptic curve discrete logarithm problem. Same security as RSA but with much smaller keys.

```
256-bit EC key ≈ 3072-bit RSA key in security level
EC key operations: ~10× faster than equivalent RSA
```

Common curves:
- **P-256 (prime256v1, secp256r1)**: NIST standard, used in TLS, ECDSA
- **P-384**: Higher security margin, slower
- **secp256k1**: Bitcoin, Ethereum
- **X25519**: Diffie-Hellman key exchange (TLS 1.3)
- **Ed25519**: EdDSA signatures — fastest, most modern (SSH keys, JWT)

```python
from cryptography.hazmat.primitives.asymmetric.ec import (
    ECDH, SECP256R1, generate_private_key
)
from cryptography.hazmat.primitives.asymmetric import ec

# Generate P-256 key pair
private_key = generate_private_key(SECP256R1())
public_key = private_key.public_key()

# ECDH key exchange (used in TLS — both sides derive same shared secret)
server_private = generate_private_key(SECP256R1())
client_private = generate_private_key(SECP256R1())

shared_key_server = server_private.exchange(ECDH(), client_private.public_key())
shared_key_client = client_private.exchange(ECDH(), server_private.public_key())
assert shared_key_server == shared_key_client  # True — this becomes TLS session key
```

### Hybrid Encryption Pattern (The Real-World Pattern)

Never encrypt large data with RSA/EC directly — it's slow and limited in size. Use:

```
1. Generate random AES-256 key (Data Encryption Key = DEK)
2. Encrypt the actual data with AES-GCM using DEK
3. Encrypt the DEK with RSA-OAEP using recipient's public key
4. Transmit: encrypted DEK + encrypted data + GCM nonce + GCM tag

Recipient:
5. Decrypt DEK using their RSA private key
6. Decrypt data using AES-GCM with DEK
```

This is exactly what TLS does: RSA/EC handshake → shared AES session key → AES-GCM for data.

```python
from cryptography.hazmat.primitives.ciphers.aead import AESGCM
from cryptography.hazmat.primitives.asymmetric import padding
from cryptography.hazmat.primitives import hashes
import os

def hybrid_encrypt(plaintext: bytes, recipient_public_key):
    # 1. Generate ephemeral DEK
    dek = os.urandom(32)
    nonce = os.urandom(12)

    # 2. Encrypt data with AES-GCM
    aesgcm = AESGCM(dek)
    ciphertext = aesgcm.encrypt(nonce, plaintext, None)

    # 3. Encrypt DEK with RSA-OAEP
    encrypted_dek = recipient_public_key.encrypt(
        dek,
        padding.OAEP(mgf=padding.MGF1(hashes.SHA256()), algorithm=hashes.SHA256(), label=None)
    )

    return {"encrypted_dek": encrypted_dek, "nonce": nonce, "ciphertext": ciphertext}

def hybrid_decrypt(package: dict, recipient_private_key):
    # 4. Decrypt DEK
    dek = recipient_private_key.decrypt(
        package["encrypted_dek"],
        padding.OAEP(mgf=padding.MGF1(hashes.SHA256()), algorithm=hashes.SHA256(), label=None)
    )

    # 5. Decrypt data
    aesgcm = AESGCM(dek)
    return aesgcm.decrypt(package["nonce"], package["ciphertext"], None)
```

---

## 12. Key Derivation Functions

### Why KDFs Exist

Problem 1 — Low-entropy passwords: A user password has maybe 40 bits of entropy. AES-256 needs 256 random bits. You cannot just `SHA256(password)` and use it as an AES key — too easy to brute-force.

Problem 2 — Key material stretching: A single shared secret should yield multiple keys (encryption key, MAC key, IV, etc.).

Key Derivation Functions (KDFs) solve both problems.

### HKDF (HMAC-based Key Derivation Function)

Two-phase process:
- **Extract**: `HKDF-Extract(salt, IKM)` → pseudorandom key (PRK)
- **Expand**: `HKDF-Expand(PRK, info, L)` → output key material (OKM) of length L

Used in: TLS 1.3 (derives session keys from shared secret), Signal Protocol, WireGuard.

```python
from cryptography.hazmat.primitives.kdf.hkdf import HKDF
from cryptography.hazmat.primitives import hashes
import os

# Example: derive AES key + HMAC key from a single shared secret
shared_secret = os.urandom(32)  # e.g., from ECDH key exchange
salt = os.urandom(32)           # random, not secret

hkdf = HKDF(
    algorithm=hashes.SHA256(),
    length=64,             # 32 bytes for AES key + 32 bytes for HMAC key
    salt=salt,
    info=b"myapp-aes-hmac-keys-v1"  # context-specific label
)

okm = hkdf.derive(shared_secret)
aes_key  = okm[:32]
hmac_key = okm[32:]
```

### PBKDF2 as a KDF

When you need a key from a password (not just for password storage):

```python
import hashlib, os

password = b"user-passphrase"
salt = os.urandom(16)

# Derive 64 bytes → split into two keys
key_material = hashlib.pbkdf2_hmac("sha256", password, salt, 600_000, dklen=64)
aes_key  = key_material[:32]
hmac_key = key_material[32:]
```

### Argon2 as a KDF

When deriving keys from passwords and you want memory-hardness:

```python
from argon2.low_level import hash_secret_raw, Type
import os

password = b"user-passphrase"
salt = os.urandom(16)

key = hash_secret_raw(
    secret=password,
    salt=salt,
    time_cost=3,
    memory_cost=65536,
    parallelism=4,
    hash_len=32,
    type=Type.ID
)
# Use `key` directly as AES-256 key
```

---

## 13. Digital Signatures

### Signing vs Encrypting

| Operation | Key used | Purpose |
|---|---|---|
| Encrypt | Recipient's **public** key | Confidentiality — only recipient reads |
| Decrypt | Recipient's **private** key | Recover plaintext |
| Sign | Sender's **private** key | Authentication + non-repudiation |
| Verify | Sender's **public** key | Anyone can verify sender is genuine |

```
To prove "I wrote this document":
1. Hash the document: h = SHA256(doc)
2. Encrypt the hash with MY private key: sig = RSA-decrypt(private_key, h)
3. Recipient: verifies sig = RSA-encrypt(public_key, sig) == SHA256(doc)
```

Note: "signing" in RSA is conceptually "encrypting with private key" but proper schemes use RSA-PSS padding (not direct RSA-OAEP).

### RSA-PSS (Probabilistic Signature Scheme)

```python
from cryptography.hazmat.primitives.asymmetric import padding, rsa
from cryptography.hazmat.primitives import hashes
from cryptography.hazmat.primitives.asymmetric.rsa import generate_private_key

private_key = generate_private_key(65537, 2048)
public_key = private_key.public_key()

message = b"This document is approved"

# Sign
signature = private_key.sign(
    message,
    padding.PSS(
        mgf=padding.MGF1(hashes.SHA256()),
        salt_length=padding.PSS.MAX_LENGTH
    ),
    hashes.SHA256()
)

# Verify (raises InvalidSignature if tampered)
public_key.verify(
    signature,
    message,
    padding.PSS(
        mgf=padding.MGF1(hashes.SHA256()),
        salt_length=padding.PSS.MAX_LENGTH
    ),
    hashes.SHA256()
)
print("Valid signature")
```

### ECDSA (Elliptic Curve DSA)

```python
from cryptography.hazmat.primitives.asymmetric.ec import (
    generate_private_key, SECP256R1, ECDSA
)
from cryptography.hazmat.primitives import hashes

private_key = generate_private_key(SECP256R1())
public_key = private_key.public_key()

message = b"This document is approved"

# Sign
sig = private_key.sign(message, ECDSA(hashes.SHA256()))

# Verify
public_key.verify(sig, message, ECDSA(hashes.SHA256()))
```

**ECDSA nonce reuse vulnerability:** ECDSA requires a unique random `k` (nonce) per signature. If the same `k` is reused for two different messages, the private key can be extracted. This is how the PS3 private key was extracted in 2010. EdDSA (Ed25519) is deterministic and does not have this problem.

### EdDSA / Ed25519

- Deterministic (no random nonce) — eliminates ECDSA nonce reuse attacks
- Fastest signature algorithm
- Used in: SSH keys (`ssh-keygen -t ed25519`), modern JWTs (`alg: EdDSA`), Signal, WireGuard, age encryption

```python
from cryptography.hazmat.primitives.asymmetric.ed25519 import Ed25519PrivateKey

private_key = Ed25519PrivateKey.generate()
public_key = private_key.public_key()

message = b"This document is approved"
signature = private_key.sign(message)
public_key.verify(signature, message)  # raises if invalid

# Serialize for storage
from cryptography.hazmat.primitives.serialization import Encoding, PrivateFormat, PublicFormat, NoEncryption
pem = private_key.private_bytes(Encoding.PEM, PrivateFormat.PKCS8, NoEncryption())
```

### Algorithm Selection Guide

| Need | Recommended Algorithm | Avoid |
|---|---|---|
| RSA signature (legacy compat) | RSA-PSS with SHA-256, 2048+ bits | PKCS#1 v1.5 padding |
| EC signature (general) | ECDSA with P-256 or P-384 | secp256k1 outside blockchain |
| EC signature (modern/fast) | **Ed25519 (EdDSA)** | — |
| Code signing | RSA-4096 or ECDSA P-384 | Short keys |
| JWT signature | ES256 (P-256) or EdDSA | HS256 for distributed systems |

---

## 14. JWT Deep Dive

### Structure

```
[Base64URL(header)].[Base64URL(payload)].[Base64URL(signature)]

Example:
eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9
.eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkpvaG4gRG9lIiwiaWF0IjoxNTE2MjM5MDIyfQ
.SflKxwRJSMeKKF2QT4fwpMeJf36POk6yJV_adQssw5c
```

Decode header:
```json
{"alg": "HS256", "typ": "JWT"}
```

Decode payload:
```json
{
  "sub": "1234567890",
  "name": "John Doe",
  "iat": 1516239022,
  "exp": 1516325422
}
```

**Standard claims:** `sub` (subject), `iss` (issuer), `aud` (audience), `exp` (expiry), `iat` (issued at), `nbf` (not before), `jti` (JWT ID — for revocation)

### Algorithms

| `alg` value | Algorithm | Type | Key |
|---|---|---|---|
| `HS256` | HMAC-SHA256 | Symmetric | Shared secret |
| `HS384` | HMAC-SHA384 | Symmetric | Shared secret |
| `HS512` | HMAC-SHA512 | Symmetric | Shared secret |
| `RS256` | RSA-PKCS1v1.5-SHA256 | Asymmetric | RSA private/public pair |
| `RS384` | RSA-PKCS1v1.5-SHA384 | Asymmetric | RSA private/public pair |
| `PS256` | RSA-PSS-SHA256 | Asymmetric | RSA private/public pair |
| `ES256` | ECDSA-P256-SHA256 | Asymmetric | EC private/public pair |
| `EdDSA` | Ed25519 | Asymmetric | Ed25519 private/public pair |
| `none` | No signature | ⚠️ None | None — **NEVER ACCEPT** |

**HS256 vs RS256:** HS256 uses a shared secret (all services that verify also need the secret). RS256 uses a keypair — only the auth server holds the private key, any service can verify using the public key. **Use RS256/ES256 in distributed systems**.

### JWT in Code

```javascript
const jwt = require("jsonwebtoken");
const crypto = require("crypto");

// HS256 — shared secret
const secret = crypto.randomBytes(64);

const token = jwt.sign(
  { sub: "user_42", name: "Alice", role: "admin" },
  secret,
  { algorithm: "HS256", expiresIn: "15m", issuer: "auth.myapp.com" }
);

// Verify — throws if expired, tampered, wrong alg
const payload = jwt.verify(token, secret, {
  algorithms: ["HS256"],      // CRITICAL: whitelist algorithms
  issuer: "auth.myapp.com",
  audience: "api.myapp.com"
});

console.log(payload.sub);  // "user_42"
```

```python
import jwt  # pip install PyJWT

import datetime, secrets

secret = secrets.token_bytes(64)

# Sign
token = jwt.encode(
    {"sub": "user_42", "exp": datetime.datetime.utcnow() + datetime.timedelta(minutes=15)},
    secret,
    algorithm="HS256"
)

# Verify
payload = jwt.decode(
    token,
    secret,
    algorithms=["HS256"],     # CRITICAL: always specify allowed algorithms
    options={"require": ["exp", "sub"]}
)
```

### JWT Attack Vectors

**Attack 1: `alg: none`**

```python
# Attacker crafts: {"alg":"none","typ":"JWT"}
# Some libraries accepted this and skipped signature validation

# Fix: ALWAYS specify allowed algorithms whitelist
jwt.decode(token, key, algorithms=["HS256"])  # never algorithms=None or algorithms=[]
```

**Attack 2: HS256 / RS256 Confusion**

```
Server configured for RS256 (RSA public key)
Attacker changes header to alg:HS256
Attacker signs token with the PUBLIC KEY as the HMAC secret
Buggy library uses the public key (which it treats as HMAC secret) to verify
→ Attacker-controlled payload accepted

Fix: always whitelist algorithms on both sign AND verify
```

**Attack 3: Weak HMAC Secret**

```bash
# If HS256 secret is short/guessable, crack with hashcat
# hashcat -a 0 -m 16500 token.jwt wordlist.txt

# Fix: Use 256+ bit random secret
secret = secrets.token_bytes(64)  # 512-bit secret
```

**Attack 4: JWT in localStorage (XSS target)**

```
localStorage is accessible to any JavaScript on the page
→ XSS attack steals all tokens in localStorage

Fix: Store JWT in HttpOnly, SameSite=Strict cookie
The cookie is inaccessible to JS but sent automatically with requests
```

**Attack 5: No Revocation**

```
User logs out, but their JWT is valid until exp
→ Stolen token works until expiry

Fix:
- Short expiry (15 min access token)
- Refresh tokens with rotation
- Revocation list in Redis (check jti on each request)
- Or: stateful tokens (opaque tokens) for high-security scenarios
```

### JWE — JSON Web Encryption

JWT is signed (tamper-evident) but **not encrypted** — payload is readable by anyone (just Base64URL).

JWE encrypts the payload:

```
Protected Header . Encrypted Key . IV . Ciphertext . Auth Tag
```

Algorithm: RSA-OAEP (encrypts DEK) + AES-256-GCM (encrypts payload) = hybrid encryption.

```python
from jose import jwe, jwk  # pip install python-jose

# Generate RSA key
from cryptography.hazmat.primitives.asymmetric import rsa
private_key = rsa.generate_private_key(65537, 2048)

# Encrypt
token = jwe.encrypt(
    b'{"sub":"user42","role":"admin"}',
    jwk.construct(private_key.public_key(), algorithm="RSA-OAEP"),
    algorithm="RSA-OAEP",
    encryption="A256GCM"
)

# Decrypt
payload = jwe.decrypt(token, jwk.construct(private_key, algorithm="RSA-OAEP"))
```

**SD decision:** Use JWE when payload contains sensitive data (PII, roles that must be hidden from intermediaries). Most systems don't need it — keep sensitive data out of JWTs instead.

---

## 15. TLS Cryptographic Handshake

### TLS 1.3 Handshake (Simplified)

```
Client                                Server
  |                                     |
  |--- ClientHello ─────────────────>   |
  |    (supported ciphers, TLS ver,     |
  |     client random, key_share)       |
  |                                     |
  |<── ServerHello ──────────────────   |
  |    (chosen cipher, server random,   |
  |     key_share, certificate)         |
  |                                     |
  | [Both sides compute shared secret via ECDHE]
  | HKDF → Handshake Keys, Session Keys |
  |                                     |
  |<── {EncryptedExtensions}  ────────  |
  |<── {Certificate}          ────────  |
  |<── {CertificateVerify}    ────────  | (server signs transcript)
  |<── {Finished}             ────────  | (HMAC over transcript)
  |                                     |
  |─── {Finished}  ─────────────────>  |
  |                                     |
  |<══ Application Data (AES-GCM) ════> |
```

### Cryptographic Primitives in TLS 1.3

```
Key Exchange:  ECDHE (X25519 or P-256) — ephemeral, forward secret
Auth:          RSA-PSS or ECDSA or Ed25519 (certificate signature)
KDF:           HKDF-SHA256 (derives handshake keys, session keys, finished keys)
AEAD:          AES-128-GCM or AES-256-GCM or ChaCha20-Poly1305
PRF:           HMAC-SHA256 (inside HKDF)
```

### Forward Secrecy

In TLS 1.2 with RSA key exchange: the server's RSA private key was used to protect the session key. If the private key is compromised later, all recorded sessions can be decrypted.

In TLS 1.3 (and TLS 1.2 with ECDHE): session keys are derived from ephemeral ECDH keys that are discarded after the handshake. Compromise of the server's certificate private key cannot decrypt past sessions → **perfect forward secrecy (PFS)**.

### Certificate Pinning

Hardcoding the expected certificate/public key in a client application. Prevents MITM even if a CA is compromised (or a rogue CA cert). Used by mobile banking apps, Google Chrome for google.com.

```javascript
// Node.js — check certificate fingerprint
const agent = new https.Agent({
  checkServerIdentity: (host, cert) => {
    const fingerprint = cert.fingerprint256;
    const expected = "AA:BB:CC:..."; // known good fingerprint
    if (fingerprint !== expected) {
      throw new Error("Certificate pinning failure");
    }
    return undefined;
  }
});
```

---

## 16. Key Management

### The Key Management Problem

Good algorithms are useless with bad key management. Keys are the actual secret — protect them like passwords, but they're often much more sensitive (a compromised AES key decrypts all data encrypted with it).

### Don't Store Keys in Code or Version Control

```bash
# WRONG — key in source code
const AES_KEY = "my-hardcoded-secret-key-12345678";

# WRONG — key in .env committed to git
echo "AES_KEY=my-secret" >> .env && git add .env

# RIGHT — key in environment variable, sourced from secrets manager
process.env.AES_KEY  // injected at deploy time from AWS Parameter Store / Vault
```

### Key Storage Options by Risk Level

| Option | Risk | Use for |
|---|---|---|
| Hardcoded in source | Critical | Never |
| .env file (gitignored) | High | Local dev only |
| Environment variable | Medium | Simple apps with proper CI/CD |
| AWS Parameter Store (SecureString) | Low | AWS workloads |
| AWS Secrets Manager | Low | AWS workloads with auto-rotation |
| HashiCorp Vault | Low | Multi-cloud, fine-grained access |
| **HSM (Hardware Security Module)** | Very low | Keys never leave hardware |
| **Cloud KMS** (AWS KMS, GCP KMS) | Very low | Keys managed by cloud HSM |

### Envelope Encryption Pattern

**The core pattern used by AWS KMS, GCP KMS, HashiCorp Vault.**

```
Two-level key hierarchy:
- KEK (Key Encryption Key): Master key, stored only in KMS/HSM. Never leaves hardware.
- DEK (Data Encryption Key): Generated locally per-resource or per-tenant.

Flow:
1. Generate DEK locally: dek = os.urandom(32)
2. Encrypt data: ciphertext = AES-GCM(dek, plaintext)
3. Encrypt DEK with KEK via KMS API: encrypted_dek = KMS.Encrypt(kek_id, dek)
4. Store: { encrypted_dek, ciphertext, nonce, tag }

Decrypt:
5. dek = KMS.Decrypt(kek_id, encrypted_dek)  ← audit log entry here
6. plaintext = AES-GCM-Decrypt(dek, ciphertext)
```

```python
import boto3, os
from cryptography.hazmat.primitives.ciphers.aead import AESGCM

kms = boto3.client("kms", region_name="us-east-1")

def encrypt_with_envelope(plaintext: bytes, kms_key_id: str) -> dict:
    # KMS generates DEK and returns both plaintext + encrypted versions
    resp = kms.generate_data_key(KeyId=kms_key_id, KeySpec="AES_256")

    dek_plaintext  = resp["Plaintext"]           # use for encryption, then discard
    dek_encrypted  = resp["CiphertextBlob"]      # store alongside ciphertext

    nonce = os.urandom(12)
    aesgcm = AESGCM(dek_plaintext)
    ciphertext = aesgcm.encrypt(nonce, plaintext, None)

    # Zero out DEK from memory (best-effort in Python)
    dek_plaintext = b"\x00" * len(dek_plaintext)

    return {"dek_encrypted": dek_encrypted, "nonce": nonce, "ciphertext": ciphertext}

def decrypt_with_envelope(package: dict, kms_key_id: str) -> bytes:
    resp = kms.decrypt(CiphertextBlob=package["dek_encrypted"], KeyId=kms_key_id)
    dek_plaintext = resp["Plaintext"]

    aesgcm = AESGCM(dek_plaintext)
    return aesgcm.decrypt(package["nonce"], package["ciphertext"], None)
```

**Benefits of envelope encryption:**
- KEK never leaves KMS hardware (HSM-backed)
- Every decrypt call is audited (CloudTrail)
- Key rotation: re-encrypt DEK with new KEK — no need to re-encrypt all data
- Per-tenant DEKs: compromise of one DEK affects only that tenant's data

### Key Rotation

```
Rotation strategy:
- Symmetric keys: rotate by generating new key, re-encrypting DEKs (not data)
- Asymmetric keys: generate new pair, update certificate, keep old private key for decryption of old messages
- Password-derived keys: extend iterations periodically (rehash on next login)
- JWT secrets: keep old secret for verification for one expiry window, then drop it

AWS KMS: automatic rotation every year (optional) — creates new backing key,
all future Encrypt calls use new key, old ciphertext still decryptable via old backing key
```

---

## 17. Data-at-Rest Encryption

### Full Disk Encryption

Encrypts the entire storage medium. If a laptop/disk is stolen, data is unreadable without passphrase/TPM.

| Tool | Platform | Notes |
|---|---|---|
| **LUKS** (dm-crypt) | Linux | Standard on almost all Linux distros |
| **BitLocker** | Windows | TPM + PIN |
| **FileVault** | macOS | AES-XTS |
| **dm-crypt** | Linux | Underlying layer for LUKS |

```bash
# Create LUKS-encrypted partition
cryptsetup luksFormat /dev/sdb1
cryptsetup luksOpen /dev/sdb1 encrypted_data
mkfs.ext4 /dev/mapper/encrypted_data
mount /dev/mapper/encrypted_data /mnt/secure
```

### File-Level Encryption

Encrypt specific files, not entire disks. Suitable when you need selective sharing.

```bash
# GPG symmetric file encryption
gpg --symmetric --cipher-algo AES256 --armor sensitive.txt
# Creates sensitive.txt.asc

# GPG asymmetric
gpg --encrypt --recipient alice@example.com --armor file.txt

# age (modern, simpler than GPG)
age-keygen -o key.txt
age -e -r $(age-keygen -y key.txt) secret.txt > secret.txt.age
age -d -i key.txt secret.txt.age
```

### Database Column-Level Encryption (Application-Level)

Encrypt specific columns in the database. The DB server never sees plaintext.

```python
from cryptography.fernet import Fernet  # AES-128-CBC + HMAC — simple API

key = Fernet.generate_key()  # store in KMS/Vault
fernet = Fernet(key)

# Encrypt before insert
ssn_plaintext = b"123-45-6789"
ssn_encrypted = fernet.encrypt(ssn_plaintext)

# Store ssn_encrypted in DB column (bytes/text)
# Decrypt after read
ssn_decrypted = fernet.decrypt(ssn_encrypted)  # raises if tampered
```

**PostgreSQL pgcrypto:**

```sql
-- Encrypt
INSERT INTO users (email, ssn_encrypted)
VALUES ('alice@example.com',
        pgp_sym_encrypt('123-45-6789', current_setting('myapp.aes_key')));

-- Decrypt
SELECT pgp_sym_decrypt(ssn_encrypted::bytea, current_setting('myapp.aes_key'))
FROM users WHERE id = 1;
```

### Transparent Data Encryption (TDE)

Database engine encrypts data files at rest automatically. Application sees plaintext; encryption is transparent.

| DB | TDE Support |
|---|---|
| Oracle | Oracle TDE (since 10g) |
| SQL Server | SQL Server TDE |
| MySQL / RDS | InnoDB tablespace encryption |
| PostgreSQL | pg_transparent_data_encryption (contrib) or FS-level |
| AWS RDS | Storage encryption with KMS (one checkbox) |

```
TDE protects against: stolen disk image, DB backup leak
TDE does NOT protect against: compromised DB server, SQL injection, insider with DB access
```

**Security layers:** TDE + column-level encryption (for highly sensitive fields) + access controls.

---

## 18. Data-in-Transit

### TLS — The Universal Standard

All sensitive data over the network must use TLS 1.2+ (TLS 1.3 preferred). See Section 15 for handshake details.

```nginx
# Nginx TLS 1.3 only config
server {
    listen 443 ssl http2;
    ssl_certificate     /etc/ssl/certs/cert.pem;
    ssl_certificate_key /etc/ssl/private/key.pem;

    ssl_protocols TLSv1.2 TLSv1.3;
    ssl_prefer_server_ciphers off;  # Let client choose in TLS 1.3

    # TLS 1.2 ciphers (TLS 1.3 ciphers are automatic)
    ssl_ciphers ECDHE-ECDSA-AES128-GCM-SHA256:ECDHE-RSA-AES128-GCM-SHA256:ECDHE-ECDSA-AES256-GCM-SHA384:ECDHE-RSA-AES256-GCM-SHA384:ECDHE-ECDSA-CHACHA20-POLY1305:ECDHE-RSA-CHACHA20-POLY1305;

    # HSTS
    add_header Strict-Transport-Security "max-age=63072000; includeSubDomains; preload" always;
}
```

### mTLS (Mutual TLS)

Both client AND server present certificates. Used in microservice-to-microservice communication.

```
Normal TLS:  Server authenticates to client
mTLS:        Server + Client both authenticate to each other
```

```python
# Python client with mTLS
import ssl, httpx

ctx = ssl.SSLContext(ssl.PROTOCOL_TLS_CLIENT)
ctx.load_cert_chain("client.crt", "client.key")  # client cert + key
ctx.load_verify_locations("server-ca.pem")        # trust only this CA

client = httpx.Client(verify=ctx)
response = client.get("https://service.internal/api/data")
```

### SSH — Secure Shell

Asymmetric auth: server stores `~/.ssh/authorized_keys` (public keys), client holds private key.

```bash
# Generate Ed25519 key pair (recommended)
ssh-keygen -t ed25519 -C "your@email.com" -f ~/.ssh/id_ed25519

# SSH handshake uses Diffie-Hellman or ECDH for session key
# Then AES-256-CTR or ChaCha20-Poly1305 for data

# Key types supported: rsa, ecdsa (p256/p384/p521), ed25519
# Deprecated: dsa, rsa-1024

# Harden SSH server
cat /etc/ssh/sshd_config
# PasswordAuthentication no
# PubkeyAuthentication yes
# AuthorizedKeysFile .ssh/authorized_keys
# KexAlgorithms curve25519-sha256,diffie-hellman-group16-sha512
# Ciphers aes256-gcm@openssh.com,chacha20-poly1305@openssh.com
# MACs hmac-sha2-512-etm@openssh.com,hmac-sha2-256-etm@openssh.com
```

### SFTP vs FTPS

| Protocol | Stack | Auth | Notes |
|---|---|---|---|
| SFTP | SSH subsystem | SSH keys or password | Not FTP at all — SSH-based file transfer |
| FTPS | FTP + TLS | TLS certificates | FTP wrapped in TLS, complex NAT/firewall issues |
| FTP | Plaintext | Password (plaintext) | **Never use** — credentials visible on wire |

**Recommendation:** Use SFTP. It's simpler, single port (22), SSH key auth, and avoids FTP's passive/active mode firewall complexity.

---

## 19. System Design Interview Angle

### Decision Framework: Hash vs Encrypt vs Sign

**HASH when:**
- You only need to verify identity (passwords, file checksums)
- Data need not be recovered — only equality checked
- Storing user passwords (Argon2id with salt)
- Content-addressed storage (deduplication)

**ENCRYPT when:**
- You need to recover the original data
- Data is personally identifiable (PII, SSN, payment data)
- Shared between parties (use asymmetric or hybrid)
- At rest in databases (column-level, TDE)

**SIGN when:**
- You need to prove authenticity + integrity
- Data is public but must be tamper-proof (JWT, API responses, software packages)
- Non-repudiation required (legal documents, audit logs)

**HMAC when:**
- Signing between two parties that share a secret
- Webhooks (sender proves message to receiver)
- API request signing (AWS SigV4)
- CSRF tokens

### SD Question: Token Architecture

**"Design a stateless auth system. How do you handle logout and token revocation?"**

```
Approach 1: Pure JWT (stateless)
- Issue: short-lived JWT (15 min) + refresh token (7 days)
- On logout: blacklist JTI in Redis with TTL = remaining access token lifetime
- Tradeoff: Redis lookup on every request (but it's only the blacklist — small set)

Approach 2: Opaque tokens (stateful)
- Issue: random token, store in Redis: token → {user_id, scopes, expiry}
- On logout: delete from Redis immediately
- Tradeoff: Redis on every request, but simpler revocation

Approach 3: Short JWT, no revocation
- Issue: 5-minute JWT, no revocation list
- On logout: just let it expire (acceptable for 5 min window)
- Tradeoff: user can use token 5 more minutes after logout

Recommendation: JWT RS256 (15 min) + opaque refresh token (7 days, stored in DB)
On access token use: no DB lookup. On refresh: verify + rotate refresh token.
On logout: invalidate refresh token (DB) + add JTI to Redis blacklist with 15 min TTL.
```

### SD Question: PII Encryption Architecture

**"Store user PII (SSN, credit card) in your database. Design the encryption."**

```
Layer 1: TLS in transit (TLS 1.3)
Layer 2: Database storage encryption (AWS RDS with KMS)
Layer 3: Column-level encryption (application layer)

Implementation:
- KMS generates a Customer Master Key (CMK)
- At onboarding: generate per-user DEK, encrypt DEK with CMK, store encrypted DEK
- On write: decrypt DEK via KMS, encrypt PII field with AES-256-GCM, store ciphertext
- On read: decrypt DEK via KMS, decrypt PII, return plaintext to authorized service

Access control:
- Only the specific Lambda/service role has KMS:Decrypt permission for this CMK
- CloudTrail logs every KMS call (who accessed which key when)
- Key rotation: KMS rotates CMK annually; old DEK ciphertexts re-encrypted lazily

Search problem:
- Encrypted fields can't be searched with LIKE
- Options:
  a. Tokenize: store last4 of SSN in separate plaintext column for search, full SSN encrypted
  b. Blind index: HMAC(normalized_value, search_key) — consistent hash, no recovery
  c. Deterministic encryption: same plaintext → same ciphertext (searchable but weaker)
```

### SD Question: Secure API Design

**"How do you authenticate API requests between microservices?"**

```
Option 1: JWT RS256
- Auth service issues short-lived (15 min) JWT signed with RS256
- Services verify with auth service's public key (available via JWKS endpoint)
- No call to auth service per request — fully decentralized verification
- Revocation: add to Redis blocklist on compromise

Option 2: mTLS
- Each service has a certificate issued by internal CA
- Service mesh (Istio/Linkerd) handles mTLS automatically
- No application code changes — transport-level auth
- Best for Kubernetes microservices

Option 3: HMAC Request Signing (AWS SigV4 style)
- Each service has an API key + secret
- Sign canonical request with HMAC-SHA256
- Include timestamp to prevent replay attacks (reject if > 5 min old)
- Good for external API clients

For internal K8s: mTLS via service mesh
For external clients: JWT RS256 or HMAC signing
For server-to-server AWS: IAM roles + SigV4
```

### Data Transformation Pipeline Security

```
Event stream (Kafka):
1. Producer: serialize event as Avro (Schema Registry)
2. Producer: sign event with EdDSA private key (append signature to message header)
3. Transport: Kafka TLS 1.2+ + SASL_SSL auth
4. Consumer: verify EdDSA signature (detect tampered events)
5. Consumer: deserialize Avro
6. Storage: write to S3 with SSE-KMS (server-side encryption)

Key hierarchy:
- Producer signing key: stored in Vault, rotated quarterly
- Schema Registry: TLS client cert auth
- S3 KMS key: AWS managed, 365-day rotation
```

---

## 20. Common Bugs and Anti-Patterns

### 1. Base64 ≠ Encryption

```javascript
// WRONG — thinking Base64 provides confidentiality
const "encrypted" = btoa(JSON.stringify({ userId: 42, role: "admin" }));
// This is just encoding — anyone runs atob() and reads it

// RIGHT — use AES-GCM if you need confidentiality
const ct = await crypto.subtle.encrypt({ name: "AES-GCM", iv }, key, data);
```

### 2. MD5/SHA-1 for Password Storage

```python
# WRONG
import hashlib
hashed = hashlib.md5(password.encode()).hexdigest()  # crackable in minutes

# RIGHT
import bcrypt
hashed = bcrypt.hashpw(password.encode(), bcrypt.gensalt(12))
```

### 3. ECB Mode — Never Use

```
ECB encrypts each 16-byte block independently.
Same plaintext block → same ciphertext block.
An image encrypted with ECB still shows the original image shape.
Always use GCM, CTR, or CBC (with HMAC).
```

### 4. IV/Nonce Reuse in AES-GCM

```python
# WRONG — reusing a fixed nonce with AES-GCM
NONCE = b"\x00" * 12  # fixed nonce
ciphertext1 = aesgcm.encrypt(NONCE, plaintext1, None)
ciphertext2 = aesgcm.encrypt(NONCE, plaintext2, None)
# An attacker can XOR the two ciphertexts to cancel the keystream
# → can recover plaintext1 XOR plaintext2, potentially both plaintexts

# RIGHT — generate fresh random nonce for every encryption
nonce = os.urandom(12)  # new nonce every time
```

### 5. Timing Attack in Comparison

```python
# WRONG — early return reveals how many chars matched
if received_token == expected_token:  # timing leak
    ...

# RIGHT
import hmac
if not hmac.compare_digest(received_token, expected_token):
    raise ValueError("Invalid token")
```

### 6. Using Hash(key || message) Instead of HMAC

```python
# WRONG — length-extension attack: attacker can append data and forge valid tag
import hashlib
tag = hashlib.sha256(key + message).digest()

# RIGHT — HMAC uses inner/outer padding that defeats length extension
import hmac, hashlib
tag = hmac.new(key, message, hashlib.sha256).digest()
```

### 7. Storing Secrets in Logs or Responses

```javascript
// WRONG — logging the private key
console.log(`Initialized with key: ${privateKey.export()}`);

// WRONG — returning internal crypto error details
res.json({ error: err.message });  // might expose "Invalid MAC" → padding oracle info

// RIGHT — generic error, detailed internal log
console.error("Crypto error:", err);  // internal log only
res.status(400).json({ error: "Invalid request" });
```

### 8. Weak RSA Key Size

```
RSA-512: cracked in hours on a laptop (1999)
RSA-768: cracked in 2009
RSA-1024: factored for specific keys, considered weak
RSA-2048: current minimum, safe until ~2030
RSA-3072: recommended for post-2030
RSA-4096: high security, notably slower
```

### 9. Not Verifying Certificate Hostname

```python
# WRONG — disabling SSL verification
import requests
requests.get("https://api.example.com", verify=False)  # MITM possible

# RIGHT
requests.get("https://api.example.com")  # verify=True by default
# Or pass CA bundle for internal certs
requests.get("https://internal.svc", verify="/path/to/internal-ca.pem")
```

### 10. Signed but Not Encrypted JWT with PII

```javascript
// WRONG — putting sensitive data in JWT (signed, not encrypted)
jwt.sign({ sub: "user42", ssn: "123-45-6789", salary: 95000 }, secret);
// Anyone can decode the payload with base64 — SSN exposed

// RIGHT — keep only non-sensitive claims in JWT
jwt.sign({ sub: "user42", role: "employee" }, secret);
// Fetch sensitive data from DB using sub on each request
```

---

## 21. Interview Q&A — Beginner to MAANG Level

### Beginner

**Q: What is the difference between encoding and encryption?**  
A: Encoding converts data format without hiding it — anyone can reverse it (Base64, URL encoding). Encryption hides data; only holders of the key can recover it (AES-GCM). Encoding provides no security, encryption does.

**Q: Why can't I just Base64-encode my password in the database?**  
A: Base64 is trivially reversible — `atob()` in one line. Password storage must use a one-way adaptive hash (bcrypt/Argon2id) so that even if the database is stolen, passwords remain protected.

**Q: What is hashing? Give an example.**  
A: A hash function maps input to a fixed-size digest, deterministically and irreversibly. SHA-256("hello") always produces the same 64-char hex string, but you can't reverse it. Used for file integrity checks, password storage, and digital signatures.

**Q: What is the difference between SHA-256 and bcrypt?**  
A: SHA-256 is a general-purpose hash — extremely fast (billions/second on GPU). bcrypt is specifically designed for passwords with a work factor that makes it slow by design (~100ms), defeating brute-force attacks. Use SHA-256 for file checksums, bcrypt/Argon2id for passwords.

**Q: What is a salt in password hashing?**  
A: A random value generated per user, mixed into the password before hashing. Ensures two users with the same password get different hashes, defeating rainbow table attacks. The salt is stored alongside the hash (it's not secret — only the password is secret).

### Intermediate

**Q: What is HMAC and when do you use it vs a plain hash?**  
A: HMAC is a keyed hash — it proves a message was created by someone with the shared secret. Use plain SHA-256 for checksums (no authentication needed). Use HMAC-SHA256 when you need to prove a message wasn't tampered with by an adversary who might know the message but not the key (JWT HS256, webhook signatures, API request signing).

**Q: Explain symmetric vs asymmetric encryption. When would you use each?**  
A: Symmetric (AES-GCM): one key for encrypt and decrypt, fast, suited for large data. Asymmetric (RSA/EC): public key encrypts, private key decrypts, used when two parties haven't exchanged a secret key. In practice: asymmetric negotiates a session key, then symmetric encrypts the bulk data (hybrid encryption, what TLS does).

**Q: What is the IV/nonce in AES-GCM? What happens if you reuse it?**  
A: The IV (Initialization Vector / nonce) is a random value that makes the same plaintext produce different ciphertext each time. In AES-GCM, nonce reuse is catastrophic — an attacker who sees two ciphertexts with the same nonce can XOR them to cancel the keystream, revealing plaintext XOR plaintext. Always generate 12 random bytes per encryption.

**Q: What is the difference between a digital signature and HMAC?**  
A: Both provide integrity and authenticity. HMAC uses a shared symmetric key — both parties need the secret. Digital signatures use asymmetric keys — anyone can verify using the signer's public key (non-repudiation). Use HMAC between two parties that share a secret; use signatures when public verifiability or non-repudiation is needed.

**Q: Explain JWT structure. Why shouldn't you put sensitive data in a JWT?**  
A: JWT = Base64URL(header).Base64URL(payload).Base64URL(signature). Header and payload are only Base64URL-encoded — anyone can decode them without the signature key. The signature proves authenticity but not confidentiality. Never put SSN, passwords, credit cards, or sensitive roles in a JWT payload.

### Advanced / MAANG

**Q: A user's session JWT is stolen. What mitigation layers would you implement?**  
A: Defense in depth: (1) Short expiry (15 min access token) limits damage window. (2) Refresh token rotation — each use invalidates old refresh token, issues new one; anomaly detected if old token used again. (3) JWT ID (`jti`) blocklist in Redis with TTL equal to remaining token lifetime. (4) HttpOnly+SameSite=Strict cookie storage to prevent XSS token theft. (5) Binding token to client fingerprint (user-agent, IP — softer bind). (6) Device-level refresh token limits per user. (7) Anomaly detection: concurrent sessions from different geos triggers step-up auth.

**Q: You need to store 10M users' SSNs in your database. Design the encryption architecture.**  
A: Envelope encryption with per-user DEKs: (1) AWS KMS holds the CMK (never leaves HSM). (2) On user creation: `kms.generate_data_key(KeySpec=AES_256)` → returns plaintext DEK + encrypted DEK. (3) Encrypt SSN with AES-256-GCM using plaintext DEK. (4) Store: encrypted DEK, nonce, GCM tag, ciphertext in `user_encrypted_fields` table. Discard plaintext DEK. (5) On read: `kms.decrypt(encrypted_dek)` → plaintext DEK → decrypt SSN. (6) Searchability: store `HMAC-SHA256(normalized_ssn, search_hmac_key)` in a separate indexed column (blind index — consistent but non-reversible). (7) Key rotation: annual KMS rotation re-encrypts DEK references without touching ciphertexts.

**Q: Explain the alg:none and HS256/RS256 confusion attacks on JWT. How do you prevent them?**  
A: **alg:none:** Attacker changes header to `{"alg":"none"}`, removes signature — some buggy libs skip verification. **Fix:** Always pass explicit `algorithms=["RS256"]` whitelist to the verify call. **HS256/RS256 confusion:** Server configured for RS256 (trusts public key). Attacker signs with `alg:HS256` using the public key as the HMAC secret. Buggy library takes the configured public key and uses it as HS256 secret → accepts attacker token. **Fix:** Strictly validate `alg` in header matches server's expected algorithm; reject if it doesn't match your whitelist.

**Q: What is forward secrecy and why does TLS 1.3 guarantee it?**  
A: Forward secrecy means compromise of the server's long-term private key cannot decrypt past recorded sessions. In TLS 1.2 with RSA key exchange, the server's RSA private key directly protected the session key — compromise reveals all past sessions. TLS 1.3 mandates ECDHE (ephemeral Elliptic Curve Diffie-Hellman) for key exchange — the ephemeral key is discarded after the handshake. Even if the RSA/EC certificate private key is later compromised, past sessions cannot be decrypted.

**Q: How does envelope encryption provide both security and performance for multi-tenant systems?**  
A: Performance: KMS calls are expensive (network RTT, rate-limited). Envelope encryption makes one KMS call to get the DEK, then all data encryption/decryption uses fast local AES-GCM. Security: each tenant has a separate DEK. Compromise of one DEK (e.g., if one tenant's key material is exposed) affects only that tenant's data. The CMK never leaves KMS hardware. Rotation is efficient: change the CMK and re-encrypt DEKs (small), not the entire dataset. Audit: every KMS call logged — you know who accessed which tenant's key when.

---

## 22. Master Cheatsheet

### Decision Tree: Which Operation Do I Need?

```
Start here:
┌─────────────────────────────────────────────────────────┐
│ Do I need to RECOVER the original value?               │
│   NO  → HASH IT                                        │
│         Password? → Argon2id / bcrypt (NOT plain SHA)  │
│         File check? → SHA-256                          │
│         Blind search? → HMAC with search key           │
│   YES → Continue ↓                                     │
├─────────────────────────────────────────────────────────┤
│ Do encrypt/decrypt parties SHARE A SECRET KEY?         │
│   YES → SYMMETRIC (AES-256-GCM)                        │
│   NO  → ASYMMETRIC                                     │
│         Small data → RSA-OAEP or EC                    │
│         Large data → HYBRID (RSA wraps AES DEK)        │
├─────────────────────────────────────────────────────────┤
│ Do I need to PROVE authenticity / integrity?           │
│   Shared secret → HMAC-SHA256                          │
│   Public verifiable → Digital Signature (EdDSA/ES256)  │
├─────────────────────────────────────────────────────────┤
│ Do I just need to REPRESENT binary as text?            │
│   In URL / JWT → Base64URL (no padding)                │
│   In JSON / email → Base64                             │
│   Checksums / logs → Hex                               │
│   TOTP secrets → Base32                                │
└─────────────────────────────────────────────────────────┘
```

### Algorithm Quick-Reference Table

| Algorithm | Category | Key/Output Size | Speed | Use |
|---|---|---|---|---|
| AES-256-GCM | Symmetric AEAD | 256-bit key, 128-bit tag | Fast (HW accel) | All-purpose data encryption |
| ChaCha20-Poly1305 | Symmetric AEAD | 256-bit key | Fast (no HW needed) | Mobile, embedded, TLS 1.3 |
| RSA-OAEP | Asymmetric | 2048–4096 bit | Slow | Encrypt AES key (hybrid) |
| RSA-PSS | Asymmetric sig | 2048–4096 bit | Slow | RSA signing |
| ECDSA P-256 | Asymmetric sig | 256-bit | Fast | TLS certs, JWT ES256 |
| Ed25519 | Asymmetric sig | 256-bit | Fastest | SSH, JWT EdDSA, modern |
| HMAC-SHA256 | MAC | 256-bit key | Fast | Webhooks, JWT HS256, SigV4 |
| SHA-256 | Hash | 256-bit output | Very fast | Checksums, hashing non-passwords |
| SHA-512 | Hash | 512-bit output | Fast (64-bit CPU) | Checksums, HMAC |
| SHA3-256 | Hash | 256-bit output | Fast | Future-proof alternative |
| Argon2id | Password hash | Configurable | Slow (by design) | Password storage |
| bcrypt | Password hash | 60-char output | Slow (by design) | Password storage (widely supported) |
| PBKDF2-SHA256 | Password hash/KDF | Configurable | Slow (by design) | FIPS-required environments |
| HKDF-SHA256 | KDF | Up to 8160 bytes | Fast | TLS 1.3 key derivation |
| MD5 | Hash | 128-bit | Very fast | ⚠️ Non-security checksums only |
| SHA-1 | Hash | 160-bit | Fast | ⚠️ Avoid — collision found |

### Encoding Quick Reference

| Need | Use | Example |
|---|---|---|
| Binary in JSON/HTML | Base64 | `SGVsbG8=` |
| Binary in URL / JWT | Base64URL | `SGVsbG8` |
| TOTP secret | Base32 | `JBSWY3DP` |
| Hash output display | Hex | `2cf24dba...` |
| URL query param | `encodeURIComponent` | `hello%20world` |
| HTML output | `html.escape()` | `&lt;script&gt;` |

### Secure Defaults Checklist

```
Passwords:
✓ Argon2id (time=3, mem=64MB, para=4) or bcrypt (rounds=12)
✓ Random 16-byte salt (auto-generated by library)
✓ Check against HaveIBeenPwned API on registration/login
✓ Minimum 8 chars, max 128 chars

Encryption:
✓ AES-256-GCM with 12-byte random nonce per operation
✓ Store: nonce || tag || ciphertext as single blob
✓ Keys in AWS KMS / HashiCorp Vault — never hardcoded
✓ Envelope encryption for database PII fields

JWT:
✓ Algorithm: RS256 or ES256 (asymmetric) for distributed systems
✓ Always specify algorithms=["RS256"] whitelist on verify
✓ expiry: 15 min for access tokens, 7 days for refresh
✓ Store in HttpOnly, SameSite=Strict, Secure cookie
✓ Never put SSN/PII in JWT payload

HMAC / Signatures:
✓ HMAC-SHA256 for webhook and API request signing
✓ Always use timing-safe comparison (crypto.timingSafeEqual)
✓ Include timestamp + validate window (prevent replay attacks)

TLS:
✓ TLS 1.2 minimum, TLS 1.3 preferred
✓ HSTS header with min 1 year
✓ Certificate from trusted CA (Let's Encrypt or ACM)
✓ Disable TLS 1.0 / 1.1 / SSLv3

Random:
✓ os.urandom() / crypto.randomBytes() for secrets
✓ Never use Math.random() for security
✓ secrets.token_urlsafe(32) for session tokens (Python)
✓ crypto.randomUUID() or crypto.randomBytes(16).toString('hex') (Node.js)
```

### Data Format / Serialization Decision

```
External REST API → JSON
gRPC / internal microservices → Protobuf
Kafka event streaming → Avro + Schema Registry
Config files (K8s, CI/CD) → YAML (yaml.safe_load only)
Browser WebSocket binary → MessagePack
IoT constrained devices → CBOR
Interop with Java enterprise → XML (with defusedxml)
```

### Cryptographic Pitfalls — Never Do List

```
✗ Never: Base64 for "encryption"
✗ Never: MD5 or SHA-1 for passwords
✗ Never: SHA-256(password) without salt + stretching
✗ Never: AES-ECB mode
✗ Never: Reuse IV/nonce with AES-GCM
✗ Never: == comparison for HMAC tags
✗ Never: jwt.decode(token, key, algorithms=None)
✗ Never: RSA-PKCS1v1.5 for encryption (use OAEP)
✗ Never: Secrets in environment variables committed to git
✗ Never: verify=False in HTTPS calls
✗ Never: yaml.load() with untrusted input (use yaml.safe_load)
✗ Never: Log secrets, keys, or tokens
✗ Never: Fixed salt or shared salt for all users' passwords
```

### Key Size Equivalence Table

| Security Level | Symmetric | RSA | ECC |
|---|---|---|---|
| 80-bit (obsolete) | AES-80 | RSA-1024 | ECC-160 |
| 112-bit (legacy) | AES-112 | RSA-2048 | ECC-224 |
| **128-bit (current)** | **AES-128** | **RSA-3072** | **ECC-256 (P-256)** |
| **192-bit** | **AES-192** | **RSA-7680** | **ECC-384 (P-384)** |
| **256-bit (high)** | **AES-256** | **RSA-15360** | **ECC-512 (P-521)** |

Note: AES-128 and RSA-3072 offer equivalent security. AES-256 + RSA-15360 is overkill for most applications — use AES-256 + RSA-4096 as a practical high-security choice.

---

*Document covers: character encoding (ASCII/Unicode/UTF-8/UTF-16), binary encoding (Base64/Base64URL/Base32/Hex), URL encoding, HTML encoding, serialization (JSON/XML/Protobuf/Avro/MessagePack/YAML), hashing (SHA-2/SHA-3, MD5/SHA-1 broken), password hashing (Argon2id/bcrypt/scrypt/PBKDF2), HMAC, symmetric encryption (AES-256-GCM, ChaCha20-Poly1305), asymmetric encryption (RSA-OAEP, ECC, hybrid), KDFs (HKDF/PBKDF2/Argon2), digital signatures (RSA-PSS/ECDSA/Ed25519), JWT (structure/algorithms/attacks/JWE), TLS 1.3 handshake and forward secrecy, key management (KMS/Vault/envelope encryption), data-at-rest (LUKS/TDE/column encryption), data-in-transit (TLS/mTLS/SSH/SFTP), system design interview decisions, 10 anti-patterns, beginner→MAANG Q&A, and a master cheatsheet.*
