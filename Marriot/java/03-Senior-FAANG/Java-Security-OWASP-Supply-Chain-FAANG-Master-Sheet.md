# Java Security — OWASP Top 10, Supply Chain, Spring Security, JWT — FAANG Master Sheet

## What This Covers

- OWASP Top 10 Java-specific attack patterns and mitigations
- Unsafe deserialization (CWE-502, gadget chains)
- SQL and expression language injection (CWE-89, CWE-95)
- XXE (CWE-611) in Java XML parsers
- Spring Security (SecurityFilterChain, OAuth2, JWT hardening)
- Secrets management (Vault, sealed properties)
- Supply chain hygiene (SBOM, dependency scanning, CVE)
- CWE patterns specific to Java production code

---

## 1. Mental Model

```text
Security = Defense in Depth

Layer 1: Input validation at boundaries (reject bad data early)
Layer 2: Safe API usage (prepared statements, JAXP config, deserialization filters)
Layer 3: Least privilege (auth, authz, RBAC, scopes)
Layer 4: Secure transport (TLS, mTLS, no plaintext secrets)
Layer 5: Supply chain (known-good libraries, no CVEs, SBOM)
Layer 6: Observability (security events logged, alerts on anomalies)
```

```text
FAANG interview line: "Security is not one layer but a set of controls at every boundary.
When one control fails, others limit blast radius."
```

---

## 2. Interview Priority Meter

| Topic | Priority | Why |
|---|---|---|
| SQL Injection prevention (prepared statements) | Very high | #1 interview check |
| Unsafe deserialization (CWE-502) | High | Java gadget chain exploits |
| XXE in XML parsers | High | Common Java-specific OWASP vulnerability |
| JWT vulnerabilities | High | Alg confusion, kid injection |
| Spring Security filter chain | High | Auth architecture |
| Secrets management | High | Vault, env, sealed properties |
| Supply chain (SBOM, NVD scanning) | Medium-high | Modern MAANG expectation |
| OWASP A01–A10 Java mapping | Medium-high | Checklist knowledge |
| Expression Language injection (SpEL/EL) | Medium | Spring-specific, dangerous |

---

## 3. OWASP Top 10 — Java Mapping

| OWASP Category | Java-Specific Risk | Mitigation |
|---|---|---|
| A01 — Broken Access Control | Missing `@PreAuthorize`, horizontal privilege escalation | RBAC, Spring Method Security, deny by default |
| A02 — Cryptographic Failures | Weak algorithms (MD5, SHA-1, 3DES), hardcoded IV | `BCrypt`, `Argon2`, `AES-256-GCM`, PBKDF2 |
| A03 — Injection | SQL via string concatenation, JPQL injection, SpEL injection | Prepared statements, parameterized JPQL, SpEL whitelisting |
| A04 — Insecure Design | Missing threat model, overly permissive architecture | Defense-in-depth, threat modeling |
| A05 — Security Misconfiguration | Stack traces exposed, actuator endpoints public, debug mode on | Actuator security, error handlers, Spring Security config |
| A06 — Vulnerable Components | Outdated Log4j, Jackson, Spring, Bouncy Castle | NVD scanning, Dependabot, SBOM, dependency management |
| A07 — Identification & Auth Failures | JWT `alg: none`, weak token, session fixation | `RS256/ES256`, PKCE, secure session lifecycle |
| A08 — Software & Data Integrity | Unsigned JARs, no artifact verification | Maven checksum verification, Sigstore, supply chain controls |
| A09 — Security Logging & Monitoring | No audit trail, PII in logs, no alerts on auth failure | MDC, redaction, structured security events, SIEM integration |
| A10 — SSRF | HTTP clients calling user-supplied URLs | Allowlist, deny private IP ranges, blocked-host validation |

---

## 4. SQL Injection — CWE-89

### Vulnerable Pattern

```java
// NEVER DO THIS
String query = "SELECT * FROM users WHERE name = '" + name + "'";
Connection connection = dataSource.getConnection();
Statement statement = connection.createStatement();
ResultSet resultSet = statement.executeQuery(query);
```

**Attack**: `name = "' OR '1'='1"` → dumps entire table.

### Safe: PreparedStatement

```java
String sql = "SELECT * FROM users WHERE name = ?";

try (Connection connection = dataSource.getConnection();
     PreparedStatement ps = connection.prepareStatement(sql)) {

    ps.setString(1, name);
    ResultSet resultSet = ps.executeQuery();
    // process
}
```

### Safe: JPA/Hibernate Named Parameters

```java
// JPQL with named param — safe
TypedQuery<User> query = entityManager.createQuery(
    "SELECT u FROM User u WHERE u.name = :name", User.class);
query.setParameter("name", name);
```

**Trap**: JPQL ORDER BY cannot use parameters for column names — validate against allowlist:

```java
private static final Set<String> ALLOWED_COLUMNS =
    Set.of("name", "createdAt", "status");

if (!ALLOWED_COLUMNS.contains(sortColumn)) {
    throw new IllegalArgumentException("Invalid sort column");
}

String jpql = "SELECT u FROM User u ORDER BY u." + sortColumn;
```

### Spring Data JPA

```java
// @Query with positional param — safe
@Query("SELECT u FROM User u WHERE u.email = ?1")
Optional<User> findByEmail(String email);
```

---

## 5. XXE — XML External Entity Injection (CWE-611)

**Risk**: Java XML parsers that process `DOCTYPE` declarations can be exploited to:
- Read local files (e.g., `/etc/passwd`)
- Trigger SSRF (out-of-band requests)
- Cause denial of service via "billion laughs" recursive entity expansion

### Vulnerable Code

```java
DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
DocumentBuilder builder = factory.newDocumentBuilder();
Document document = builder.parse(new InputSource(xmlReader)); // VULNERABLE
```

### Safe: Disable External Entities

```java
DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();

// Disable external entity processing
factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
factory.setExpandEntityReferences(false);

DocumentBuilder builder = factory.newDocumentBuilder();
Document document = builder.parse(new InputSource(xmlReader));
```

### SAX Parser Hardening

```java
SAXParserFactory factory = SAXParserFactory.newInstance();
factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
SAXParser parser = factory.newSAXParser();
```

### Jackson XML Hardening

```java
XmlMapper xmlMapper = XmlMapper.builder()
    .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
    .build();

// Disable XXE in the underlying parser
xmlMapper.getFactory().configure(
    com.fasterxml.jackson.core.JsonParser.Feature.ALLOW_COMMENTS, false);
```

**FAANG line**: "Any Java code processing XML from external sources must disable DOCTYPE and external entity features."

---

## 6. Unsafe Deserialization — CWE-502

### Why Java Deserialization Is Dangerous

Java's `ObjectInputStream.readObject()` executes code during deserialization via object graph construction. Attackers who can supply serialized payloads can trigger **gadget chains** — chains of existing classes with side effects.

Tools like **ysoserial** generate pre-built gadget chain payloads targeting:
- Apache Commons Collections
- Spring Framework
- Jackson (older versions)
- Groovy

### Vulnerable Pattern

```java
// NEVER deserialize untrusted bytes with default ObjectInputStream
ObjectInputStream ois = new ObjectInputStream(inputStream);
Object obj = ois.readObject(); // Can execute arbitrary code
```

### Mitigations

**Option 1: Use deserialization filters (Java 9+)**

```java
ObjectInputStream ois = new ObjectInputStream(inputStream);

ois.setObjectInputFilter(info -> {
    Class<?> clazz = info.serialClass();

    if (clazz == null) {
        return ObjectInputFilter.Status.ALLOWED;
    }

    // Only allow specific classes
    if (clazz == BookingRecord.class || clazz == Long.class || clazz == String.class) {
        return ObjectInputFilter.Status.ALLOWED;
    }

    return ObjectInputFilter.Status.REJECTED;
});

Object obj = ois.readObject();
```

**Option 2: Avoid Java serialization entirely**

Use JSON (Jackson), Protocol Buffers, or Avro for data exchange. Never deserialize untrusted bytes with default `ObjectInputStream`.

**Option 3: Global JVM filter (Java 9+)**

```properties
# jvm.options or security.properties
jdk.serialFilter=java.base/*;!*
```

**Option 4: Remove vulnerable transitive dependencies**

```xml
<!-- Audit and exclude commons-collections if not needed -->
<dependency>
    <groupId>commons-collections</groupId>
    <artifactId>commons-collections</artifactId>
    <version>3.2.2</version>
    <exclusions>
        <!-- use 4.x or remove if unused -->
    </exclusions>
</dependency>
```

**FAANG line**: "Java deserialization of untrusted data is a critical RCE vector. I would never deserialize arbitrary bytes without strict allowlisting or, better, replace Java serialization with a safe format."

---

## 7. Expression Language Injection — CWE-95

### Spring SpEL Injection

SpEL (Spring Expression Language) executed on untrusted input is an RCE.

**Vulnerable**:

```java
// Never evaluate untrusted SpEL expressions
ExpressionParser parser = new SpelExpressionParser();
Expression expression = parser.parseExpression(userInput); // RCE if userInput is user data
Object result = expression.getValue();
```

**Attack**: `userInput = "T(java.lang.Runtime).getRuntime().exec('id')"` → remote code execution.

**Mitigations**:
- Never pass user-controlled data to `SpelExpressionParser.parseExpression()`
- Use `SimpleEvaluationContext` if evaluation is needed (strips dangerous capabilities):

```java
ExpressionParser parser = new SpelExpressionParser();
EvaluationContext context = SimpleEvaluationContext
    .forReadOnlyDataBinding()
    .build();

Expression expression = parser.parseExpression(trustedTemplate);
String result = expression.getValue(context, data, String.class);
```

**EL Injection in JSP/Thymeleaf**: Use contextual output encoding. Never render user input directly in templates without escaping.

---

## 8. JWT Security

### Algorithm Confusion Attack

```text
Attack: Send JWT with alg: none or switch RS256 → HS256
```

**Vulnerable**:

```java
// Never trust algorithm from the JWT header
Jwts.parser()
    .setSigningKey(publicKey)
    .parseClaimsJws(token); // If alg is changed to HS256, uses public key as HMAC secret
```

**Safe**:

```java
// Always enforce expected algorithm
Jwts.parserBuilder()
    .requireAlgorithm("RS256")
    .setSigningKey(publicKey)
    .build()
    .parseClaimsJws(token);
```

### kid Injection Attack

```text
Attack: kid claim points to attacker-controlled URL (SSRF) or path (LFI)
```

**Mitigation**: Never use `kid` to dynamically fetch signing keys from untrusted sources. Use a pre-fetched JWK set:

```java
// Use JWK Set URI from trusted config, not from token kid
JwkProvider jwkProvider = new JwkProviderBuilder("https://trusted-idp.example.com/.well-known/jwks.json")
    .cached(10, 24, TimeUnit.HOURS)
    .build();
```

### JWT Common Traps Table

| Trap | Risk | Fix |
|---|---|---|
| `alg: none` | Signature bypass | Enforce allowed algorithms |
| Symmetric HS256 shared secret | Weak secret → brute force | Use asymmetric RS256/ES256 |
| No expiry check | Tokens valid forever | Validate `exp` claim |
| Not validating `iss` and `aud` | Token from other service accepted | Validate issuer and audience |
| Logging full token | Token theft via logs | Log token prefix only, never full JWT |
| Storing in localStorage | XSS steals token | Use httpOnly cookie or secure storage |

**FAANG line**: "JWT signing must use asymmetric keys (RS256 or ES256), algorithm must be enforced by the server, and issuer, audience, and expiry must all be validated."

---

## 9. Spring Security — SecurityFilterChain

### Filter Chain Mental Model

```text
HTTP Request
  → DelegatingFilterProxy
  → SecurityFilterChain
      → SecurityContextPersistenceFilter (load context)
      → UsernamePasswordAuthenticationFilter (form login)
      → BearerTokenAuthenticationFilter (OAuth2/JWT)
      → ExceptionTranslationFilter (401/403 handling)
      → FilterSecurityInterceptor (authorization checks)
  → Controller
```

### Modern SecurityFilterChain Configuration (Spring Boot 3+)

```java
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable()) // Disable for stateless JWT APIs
            .sessionManagement(session ->
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/public/**").permitAll()
                .requestMatchers("/actuator/health").permitAll()
                .requestMatchers("/api/admin/**").hasRole("ADMIN")
                .anyRequest().authenticated())
            .oauth2ResourceServer(oauth2 ->
                oauth2.jwt(jwt -> jwt.decoder(jwtDecoder())));

        return http.build();
    }

    @Bean
    public JwtDecoder jwtDecoder() {
        // Fetch public key from JWK Set URI
        return NimbusJwtDecoder
            .withJwkSetUri("https://idp.example.com/.well-known/jwks.json")
            .build();
    }
}
```

### Method Security

```java
@Service
public class BookingService {

    // Only authenticated users with ROLE_USER can call this
    @PreAuthorize("hasRole('USER')")
    public Booking getBooking(String id) { ... }

    // Only admin or the booking owner
    @PreAuthorize("hasRole('ADMIN') or #booking.guestId == authentication.name")
    public void cancelBooking(Booking booking) { ... }

    // Post-filter on returned collection
    @PostAuthorize("returnObject.guestId == authentication.name")
    public Booking findById(String id) { ... }

    // Filter on returned list
    @PostFilter("filterObject.guestId == authentication.name")
    public List<Booking> findAll() { ... }
}
```

### Custom PermissionEvaluator

```java
@Component
public class BookingPermissionEvaluator implements PermissionEvaluator {

    @Override
    public boolean hasPermission(Authentication auth, Object targetDomainObject, Object permission) {
        if (targetDomainObject instanceof Booking booking) {
            if ("READ".equals(permission)) {
                return booking.getGuestId().equals(auth.getName());
            }
        }
        return false;
    }

    @Override
    public boolean hasPermission(Authentication auth, Serializable targetId,
                                 String targetType, Object permission) {
        // Load entity and check
        return false;
    }
}
```

```java
// Usage
@PreAuthorize("hasPermission(#booking, 'READ')")
public Booking getBookingSecure(Booking booking) { ... }
```

---

## 10. CSRF and Session Security

### CSRF Defense

For **browser-facing** stateful apps, CSRF protection is required:

```java
http.csrf(csrf -> csrf
    .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse()));
```

For **stateless JWT APIs** called by mobile/SPA with non-browser clients, CSRF can be disabled — but only when:
- Session is stateless (`SessionCreationPolicy.STATELESS`)
- Tokens are in headers, not cookies

### Session Fixation

Spring Security handles session fixation by default:

```java
http.sessionManagement(session ->
    session.sessionFixation(fixation -> fixation.newSession()));
```

---

## 11. Secrets Management

### Anti-Patterns

```properties
# NEVER in application.properties committed to git
db.password=super-secret-password
api.key=sk-1234567890
```

### Pattern 1: Environment Variables (minimum baseline)

```properties
# application.properties
spring.datasource.password=${DB_PASSWORD}
```

```bash
# Injected at deploy time, not stored in code
export DB_PASSWORD=$(vault kv get -field=password secret/myapp/db)
```

### Pattern 2: Spring Cloud Vault

```xml
<dependency>
    <groupId>org.springframework.cloud</groupId>
    <artifactId>spring-cloud-starter-vault-config</artifactId>
</dependency>
```

```yaml
# bootstrap.yml
spring:
  cloud:
    vault:
      uri: https://vault.example.com
      authentication: KUBERNETES
      kubernetes:
        role: myapp-role
      kv:
        enabled: true
        default-context: myapp
```

Spring Cloud Vault fetches secrets at startup and makes them available as Spring properties.

### Pattern 3: AWS Secrets Manager (for EKS/cloud deployments)

```java
@Configuration
public class SecretsConfig {

    @Bean
    public DataSource dataSource() {
        SecretsManagerClient client = SecretsManagerClient.create();

        GetSecretValueRequest request = GetSecretValueRequest.builder()
            .secretId("myapp/db/credentials")
            .build();

        String secretJson = client.getSecretValue(request).secretString();
        ObjectMapper mapper = new ObjectMapper();
        JsonNode secret = mapper.readTree(secretJson);

        return DataSourceBuilder.create()
            .url(secret.get("url").asText())
            .username(secret.get("username").asText())
            .password(secret.get("password").asText())
            .build();
    }
}
```

### Secret Rotation Pattern

```text
1. New secret created in Vault/AWS Secrets Manager
2. App fetches new secret via @RefreshScope or restart
3. Old secret deprecated after grace period
4. Audit trail recorded
```

**Key rule**: Secrets are never in code, never logged, never in environment variable files committed to git.

---

## 12. Supply Chain Security

### Dependency Scanning with OWASP Dependency-Check

```xml
<!-- pom.xml -->
<plugin>
    <groupId>org.owasp</groupId>
    <artifactId>dependency-check-maven</artifactId>
    <version>9.0.9</version>
    <executions>
        <execution>
            <goals>
                <goal>check</goal>
            </goals>
        </execution>
    </executions>
    <configuration>
        <failBuildOnCVSS>7</failBuildOnCVSS> <!-- Fail on HIGH and CRITICAL CVEs -->
    </configuration>
</plugin>
```

### Gradle Dependency Check

```kotlin
// build.gradle.kts
plugins {
    id("org.owasp.dependencycheck") version "9.0.9"
}

dependencyCheck {
    failBuildOnCVSS = 7.0
    formats = listOf("HTML", "JSON", "SARIF")
}
```

### SBOM Generation (CycloneDX)

```xml
<plugin>
    <groupId>org.cyclonedx</groupId>
    <artifactId>cyclonedx-maven-plugin</artifactId>
    <version>2.7.9</version>
    <executions>
        <execution>
            <phase>package</phase>
            <goals>
                <goal>makeAggregateBom</goal>
            </goals>
        </execution>
    </executions>
</plugin>
```

Generates `bom.json` / `bom.xml` — Software Bill of Materials listing all components and licenses.

### GitHub Actions Security Pipeline

```yaml
name: Security Scan

on: [push, pull_request]

jobs:
  security:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4

      - name: Set up Java
        uses: actions/setup-java@v4
        with:
          java-version: '21'
          distribution: 'temurin'

      - name: OWASP Dependency Check
        run: mvn verify org.owasp:dependency-check-maven:check

      - name: Generate SBOM
        run: mvn cyclonedx:makeAggregateBom

      - name: Upload SARIF results
        uses: github/codeql-action/upload-sarif@v3
        with:
          sarif_file: target/dependency-check-report.sarif

      - name: Container scan with Trivy
        uses: aquasecurity/trivy-action@master
        with:
          image-ref: 'myapp:${{ github.sha }}'
          format: 'sarif'
          output: 'trivy-results.sarif'
```

### Supply Chain Checklist

```text
□ No CRITICAL/HIGH CVEs in transitive dependencies
□ All dependencies pinned to specific versions (no ranges)
□ Lock files committed (pom.xml with exact versions or Gradle lockfiles)
□ SBOM generated as part of build pipeline
□ Dependency update automation (Dependabot or Renovate)
□ Container base image scanned (Trivy, Grype, Snyk)
□ Docker image from minimal base (distroless or Alpine-based JRE)
□ No secrets in Dockerfile or build scripts
□ Build outputs reproducible (optional but MAANG-valued)
□ Artifact signing (Sigstore, GPG) for published artifacts
```

---

## 13. Structural Logging Security

**Never log sensitive data**:

```java
// BAD - PII in logs
log.info("User login: email={}, password={}", email, password);
log.info("Processing payment: cardNumber={}", cardNumber);

// GOOD - log identifiers, not secrets
log.info("User login: userId={}, requestId={}", userId, requestId);
log.info("Processing payment: paymentId={}, last4={}", paymentId, last4);
```

**Sanitize MDC values**:

```java
// Never put full JWT or bearer token in MDC
MDC.put("requestId", requestId);
MDC.put("userId", userId); // OK - identifier
// MDC.put("token", bearerToken); // NEVER - token value in logs
```

**Redact in Jackson**:

```java
class PaymentRequest {
    private String cardNumber;

    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    public String getCardNumber() { return cardNumber; }
}
```

---

## 14. Common OWASP Traps in Java Interviews

| Trap | What Candidates Miss | Correct Answer |
|---|---|---|
| SQL injection via JPQL | Think ORM is safe by default | JPQL string concat is still injectable; use named params |
| CSRF only matters for sessions | Think stateless means no CSRF | Correct for JWT in headers; wrong if JWT in cookies |
| JWT `alg` from header is trusted | Server uses header-declared algorithm | Server must enforce algorithm, ignore header algorithm |
| MD5/SHA-1 for passwords | Think any hash is safe | MD5/SHA-1 are fast; use BCrypt/Argon2 for passwords |
| Log4j CVE (Log4Shell) | Not Java-specific | Log4j JNDI injection exploited Java class loading; disable JNDI lookup |
| Actuator endpoints are safe | Management port not secured | Actuator health/info OK public; env/heapdump must be locked down |
| `readObject()` on network data | Trust internal deserialization | Any untrusted byte stream deserializing is CWE-502 |

---

## 15. Strong Interview Answers

### OWASP Injection

```text
For Java, injection risk comes from SQL string concatenation, JPQL string concatenation,
Expression Language injection (SpEL), and OS commands. I prevent it by using prepared
statements, named parameters in JPQL, strict avoidance of user input in SpEL parsing,
and command-line argument allowlisting. I treat all external input as untrusted.
```

### Deserialization

```text
Java's native deserialization via ObjectInputStream is dangerous because it can execute
code through gadget chains during object graph construction. I address this by either
using a safe serialization format (JSON with Jackson, Protobuf), applying a deserialization
filter to allowlist expected classes, or removing the vulnerable gadget libraries from
the classpath entirely.
```

### JWT Security

```text
JWT security requires enforcing the signing algorithm on the server (never trusting the
alg header), using asymmetric signatures (RS256/ES256) for multi-service environments,
validating issuer and audience claims, and setting short expiry with refresh token rotation.
Common attacks include algorithm confusion (switching HS256/RS256) and kid header injection
for SSRF or local file inclusion.
```

### Supply Chain

```text
I treat dependencies as a trust boundary. I use OWASP Dependency-Check or Snyk in CI to fail
builds on high-severity CVEs, generate a CycloneDX SBOM for compliance, pin dependency
versions, and automate updates with Dependabot. I also scan container base images with Trivy
and prefer minimal base images like distroless to reduce the attack surface.
```

---

## 16. Final Revision Checklist

```text
□ SQL injection: prepared statements in JDBC, named params in JPQL, column allowlist for ORDER BY
□ XXE: disable DOCTYPE, external-general-entities, external-parameter-entities in XML parsers
□ Deserialization: never readObject() untrusted bytes; use deserialization filters or safe formats
□ SpEL injection: never parse user input as SpEL; use SimpleEvaluationContext if SpEL is needed
□ JWT: enforce RS256/ES256, validate iss/aud/exp, never log full token
□ Spring Security: SecurityFilterChain with stateless + JWT, @PreAuthorize with SpEL, custom PermissionEvaluator
□ CSRF: disable for stateless JWT APIs, enable for browser session apps
□ Secrets: environment variables or Vault, never in code/git
□ Supply chain: OWASP Dependency-Check in CI, SBOM generation, Trivy for container scan
□ Logging: no PII/secrets in logs, structured security events, MDC with identifiers only
```
