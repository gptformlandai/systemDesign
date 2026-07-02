# React Native Auth Security: OAuth, PKCE, Passkeys, And Deep Links - Gold Sheet

> Track Module - Group 4: Senior / MAANG Path
> Level: production authentication and authorization safety

---

## 1. Interview Priority Meter

| Topic | Frequency | Why It Matters |
|---|---|---|
| OAuth/OIDC + PKCE | Very high | Standard mobile auth pattern |
| Token storage | Very high | Common security interview topic |
| Deep link callback security | High | Auth callbacks can be abused |
| Refresh rotation | High | Limits stolen-token damage |
| Passkeys/biometrics | Medium-high | Modern mobile login UX |
| Jailbreak/root detection trade-offs | Medium | Useful but not a trust boundary |

MAANG signal:
You can design mobile auth as a security protocol, not just a login screen.

---

## 2. Mental Model

Mobile apps are public clients.

```text
User
  -> system browser/auth session
  -> identity provider
  -> redirect through universal/app link or scheme
  -> app validates state/nonce
  -> backend/token endpoint
  -> secure token storage
  -> API calls with backend authorization
```

Rule:
The backend enforces authorization. The app stores tokens carefully and improves UX, but the app binary is not a trusted boundary.

---

## 3. OAuth/OIDC With PKCE

Use Authorization Code + PKCE for mobile.

Flow:

```text
1. App creates code_verifier and code_challenge.
2. App opens system browser/auth session.
3. User authenticates with IdP.
4. IdP redirects back with authorization code and state.
5. App validates state.
6. App exchanges code + code_verifier for tokens.
7. App stores tokens in secure storage.
8. App calls backend APIs.
```

Why PKCE:
It protects the code exchange for public clients that cannot safely keep a client secret.

Do not:
- embed client secret in the app
- use implicit flow for modern mobile auth
- pass access tokens in deep link URLs
- log auth callback URLs

---

## 4. State And Nonce

Use `state` to prevent CSRF/callback injection.

Use `nonce` when validating identity tokens.

Validation:

```ts
type PendingAuth = {
  state: string;
  nonce: string;
  codeVerifier: string;
  createdAt: number;
};

function validateCallback(expected: PendingAuth, actualState: string) {
  if (expected.state !== actualState) {
    throw new Error('Invalid auth state');
  }
}
```

Rules:
- expire pending auth attempts
- store pending auth state securely enough for the threat model
- clear it after success/failure
- handle duplicate callbacks

---

## 5. Deep Link Callback Security

Prefer universal links/app links for auth callbacks when supported.

Custom schemes are easier to collide with if another app registers the same scheme.

Callback checklist:

```text
[ ] scheme/domain belongs to app/team
[ ] state validated
[ ] no token in URL
[ ] callback parsed by allowlisted host/path
[ ] duplicate callback safe
[ ] errors handled without leaking details
[ ] redirect path covered by tests
```

Do not route every incoming URL directly into the app without validation.

---

## 6. Token Storage And Refresh

Store:
- access token: memory or secure storage depending on app lifecycle needs
- refresh token: platform secure storage
- true service secrets: backend only

Token policy:
- short-lived access tokens
- refresh token rotation
- revoke on logout
- clear on device compromise signal if policy requires
- redact from logs

Refresh storm prevention:

```ts
let refreshPromise: Promise<string> | null = null;

export async function getFreshAccessToken() {
  if (!refreshPromise) {
    refreshPromise = refreshAccessToken().finally(() => {
      refreshPromise = null;
    });
  }

  return refreshPromise;
}
```

---

## 7. Passkeys And Biometrics

Passkeys:
- phishing-resistant authentication
- backed by platform credential providers
- should be integrated with backend WebAuthn/passkey flow

Biometrics:
- useful for local re-auth or unlocking a stored session
- not a replacement for backend auth
- should fall back to device PIN/passcode depending on policy

Interview trap:
Do not say "Face ID secures the backend." It unlocks local access; backend authorization still depends on tokens/session and server checks.

---

## 8. Authorization

Mobile authorization mistakes:
- hiding buttons as the only access control
- trusting role claims forever
- caching entitlements without expiry
- letting old app versions bypass new rules

Correct model:

```text
Client:
  improves UX, hides unavailable actions, sends token

Backend:
  validates token, role, ownership, policy, resource state
```

Feature flags are not authorization. They can control rollout, not security.

---

## 9. Jailbreak/Root Detection

Useful for:
- risk scoring
- blocking high-risk features
- compliance requirements
- additional step-up authentication

Limitations:
- bypassable by advanced attackers
- false positives possible
- can harm legitimate users
- not a backend trust guarantee

Senior answer:
Use device integrity signals as one input to risk policy, not as the only defense.

---

## 10. Secure Logging

Never log:
- access token
- refresh token
- authorization code
- full callback URL
- ID token claims with PII
- password/passkey payload
- session cookies

Better:

```ts
logger.info('auth_callback_received', {
  provider: 'oidc',
  hasCode: true,
  stateMatched: true,
});
```

---

## 11. Failure Modes

| Failure | User Impact | Mitigation |
|---|---|---|
| Auth callback intercepted | login fails or attack path | universal/app links, state validation |
| Refresh storm | API failures and rate limits | single-flight refresh |
| Token leaked in logs | account compromise risk | logger redaction |
| Old app role cache | unauthorized UI | backend enforcement, entitlement expiry |
| Biometric lockout | user blocked | fallback path |
| IdP outage | login unavailable | graceful error and status messaging |

---

## 12. Strong Interview Answer

```text
For mobile auth I treat the app as a public client and use Authorization Code with
PKCE through a system browser or auth session. I validate state and nonce on the
callback, avoid tokens in URLs, prefer universal/app links, store refresh tokens
in secure storage, and prevent refresh storms with a single-flight refresh. I use
biometrics or passkeys for better UX where appropriate, but the backend remains
the authorization boundary. Logs redact all tokens and auth URLs.
```

---

## 13. Revision Notes

- One-line summary: Mobile auth is PKCE plus secure callback validation plus backend authorization.
- Three keywords: PKCE, state, secure storage.
- One interview trap: Treating biometrics or feature flags as backend authorization.
- Memory trick: Public client, private backend, validated callback.
