# Integrating Google Play Integrity with IPification

This guide shows how a client can check Android app and device integrity before starting IPification authentication. The Client Backend verifies the integrity result, links it to a short-lived transaction, and makes the final access decision after IPification completes.

Google Play Integrity supplies app and device signals. IPification supplies the network authentication result. The client combines these signals according to its security policy. This guide applies to Android; iOS requires a separate attestation integration.

## Integration flow

```mermaid
sequenceDiagram
    participant App as Client App
    participant Google as Google Play Integrity
    participant Backend as Client Backend
    participant IP as IPification

    Note over App,Backend: Phase 1 - Verify app and device
    App->>Backend: Create attempt for IPIFICATION_START
    Backend-->>App: Attempt ID, challenge and canonical request
    App->>Google: 1. Request token with requestHash
    Google-->>App: integrityToken
    Note over App,Backend: Request signed state when ready to start IPification
    App->>Backend: 2. POST /security/integrity/verify
    Note right of App: action: IPIFICATION_START<br/>attemptId + integrityToken
    Backend->>Google: Decode integrity token
    Google-->>Backend: Integrity verdict
    Backend->>Backend: Validate action, session, hash, freshness and policy

    alt Integrity accepted
        Backend->>Backend: Consume attempt and persist transaction
        Backend->>Backend: Generate short-lived signed state
        Backend-->>App: Transaction ID, state and authorization URL

        Note over App,IP: Phase 2 - Immediately run IPification
        App->>IP: 3. Start authorization over cellular with signed state
        IP-->>App: Callback with code + state

        Note over App,Backend: Phase 3 - Complete verification
        App->>Backend: 4. Send code + state in original session
        Backend->>Backend: Validate state, action, session, expiry and integrity record
        alt Completion checks pass and unused transaction is claimed
            Backend->>IP: 5. Exchange code using backend credentials
            IP-->>Backend: Token response or error
            Backend->>Backend: Validate result, apply policy and persist outcome
            Backend-->>App: ALLOW / REVIEW / DENY
        else Invalid, expired or already claimed
            Backend-->>App: Reject completion without exchanging code
        end
    else Integrity rejected or unavailable
        Backend-->>App: Error or remediation; no signed state
    end
```

“Create transaction” means creating a record in the Client Backend. It does not imply an additional IPification transaction-creation API.

## Prerequisites

- Configure Play Integrity for the Android application, link the Google Cloud project, and configure backend access following [Google's setup guide](https://developer.android.com/google/play/integrity/setup).
- Add the supported `com.google.android.play:integrity` library version from that guide. Check its Android requirements independently of the IPification example's minimum OS.
- Obtain IPification environment URLs, client ID, confidential credentials, registered redirect URI, and scopes during onboarding.
- Keep Google service-account credentials, IPification client secrets, and state-signing keys on the backend.
- Use HTTPS between the app and Client Backend. Bind all steps to the same authenticated session, or to a server-issued pre-authentication session for login flows.

## Phase 1 — Verify the app and device

### Shared integrity API for multiple actions

Use one Client Backend endpoint, `POST /security/integrity/verify`, for protected actions across the app. The `action` parameter identifies the requested operation. This is an example client-owned API contract, not an IPification endpoint.

| Action | Result after integrity acceptance |
| --- | --- |
| `IPIFICATION_START` | Signed state and an IPification authorization URL |
| `ACCOUNT_RECOVERY` | A short-lived grant restricted to the specified recovery step |
| `CHANGE_PHONE_NUMBER` | A short-lived grant restricted to the specified phone-number change step |

These actions are illustrative. The backend defines an allowlist and the required policy for each action. It must validate that the caller is permitted to initiate the action, store its action and relevant request data in the attempt, and reject an `action` that differs from the stored value. The app cannot select a weaker policy by changing this parameter.

```mermaid
sequenceDiagram
    participant App as Client App
    participant Google as Google Play Integrity
    participant Security as Shared Integrity API
    participant API as Protected Backend API

    App->>Security: Create attempt with action and operation data
    Security->>Security: Validate caller; store action, session, data and expiry
    Security-->>App: Attempt ID, challenge and canonical request
    App->>Google: Request token with hash of canonical request
    Google-->>App: integrityToken
    App->>Security: POST /security/integrity/verify
    Note right of App: action + attemptId + integrityToken
    Security->>Google: Decode integrity token
    Google-->>Security: Integrity verdict
    Security->>Security: Validate stored action, session, hash, freshness and policy

    alt Integrity rejected or unavailable
        Security-->>App: Error or remediation; no state or grant
    else IPIFICATION_START accepted
        Security->>Security: Consume attempt and persist transaction
        Security-->>App: Signed state and authorization URL
        Note over App,Security: Immediately run IPification using the flow above
    else Another supported action accepted
        Security->>Security: Consume attempt and persist restricted single-use grant
        Security-->>App: actionGrant + expiresAt
        App->>API: Operation + actionGrant in original session
        API->>API: Validate grant, action, session, data, expiry and authorization
        alt Valid grant and execution claimed
            API->>API: Consume grant with operation or use durable execution
            API-->>App: Operation result
        else Invalid, expired or already used
            API-->>App: Reject operation
        end
    end
```

Reuse the endpoint and integrity provider, not a token or approval across unrelated actions. Each new protected attempt requests a fresh token bound to its action and data. The calls and redirects within one IPification attempt share its transaction; they do not each need a new integrity token.

For other actions, an `actionGrant` is a backend-issued approval restricted to the stored action, session and exact operation data. The target API must enforce these restrictions and single use, and must still check normal authorization and business requirements. Integrity acceptance alone does not authorize account recovery or a phone-number change. An opaque grant backed by a server-side record is sufficient; never trust an app-supplied `integrityPassed` flag. For non-atomic downstream work, use a durable claim and idempotent execution so retries cannot repeat the operation.

### 1. Create an attempt and request an integrity token

Define a Client Backend endpoint such as `POST /verification/attempts` that accepts `action` and the required operation data in the client session. For this flow, use `action: "IPIFICATION_START"`. Store the validated action, a random attempt ID, a cryptographically random challenge, session ownership, expiry, and the intended operation, including the phone number where applicable.

Return the attempt ID and a canonical request string to the app. For example, serialize this array as compact UTF-8 JSON with a fixed field order:

```json
["integrity-action-v1","IPIFICATION_START","<attempt-id>","<random-challenge>","<phone-number>"]
```

Store the exact canonical string server-side. The app hashes those bytes; the backend independently hashes its stored bytes. Do not accept a client-supplied expected hash. Changing the phone number or operation requires a new attempt.

The following Kotlin helper obtains a Standard API token. Prepare the provider before the user starts verification and reuse it while valid. See [Google's Standard API guide](https://developer.android.com/google/play/integrity/standard).

```kotlin
import android.content.Context
import android.util.Base64
import com.google.android.play.core.integrity.IntegrityManagerFactory
import com.google.android.play.core.integrity.StandardIntegrityManager
import java.security.MessageDigest

class IntegrityGate(context: Context) {
    private val manager =
        IntegrityManagerFactory.createStandard(context.applicationContext)
    private var provider:
        StandardIntegrityManager.StandardIntegrityTokenProvider? = null

    fun prepare(
        cloudProjectNumber: Long,
        onReady: () -> Unit,
        onError: (Exception) -> Unit
    ) {
        manager.prepareIntegrityToken(
            StandardIntegrityManager.PrepareIntegrityTokenRequest.builder()
                .setCloudProjectNumber(cloudProjectNumber)
                .build()
        ).addOnSuccessListener {
            provider = it
            onReady()
        }.addOnFailureListener { onError(it) }
    }

    fun requestToken(
        canonicalRequest: String,
        onToken: (String) -> Unit,
        onError: (Exception) -> Unit
    ) {
        val readyProvider = provider
        if (readyProvider == null) {
            onError(IllegalStateException("Integrity provider is not ready"))
            return
        }
        val digest = MessageDigest.getInstance("SHA-256")
            .digest(canonicalRequest.toByteArray(Charsets.UTF_8))
        val requestHash = Base64.encodeToString(
            digest, Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING
        )
        readyProvider.request(
            StandardIntegrityManager.StandardIntegrityTokenRequest.builder()
                .setRequestHash(requestHash)
                .build()
        ).addOnSuccessListener { onToken(it.token()) }
            .addOnFailureListener { onError(it) }
    }
}
```

On `INTEGRITY_TOKEN_PROVIDER_INVALID`, prepare a new provider before requesting another token. Handle transient failures with bounded retries and backoff; do not continue authorization as though integrity passed.

### 2. Call the Client Backend API to generate signed state

The Client App must call its own shared backend API with `action: "IPIFICATION_START"` to request signed state. In this example, `POST /security/integrity/verify` verifies the integrity token, creates the verification transaction, and generates signed state in one API call. The app must not generate or sign state locally.

Call this API immediately before starting the IPification flow, once user input, consent, and other required preparation are complete. Signed state is short-lived: do not request it at app launch, prefetch it, or cache it for a later authentication attempt. You may prepare the Google integrity token provider in advance, but obtain a fresh integrity token and request signed state when the user is ready to authenticate.

Send the action, attempt ID and integrity token in the original client session:

```http
POST /security/integrity/verify
Content-Type: application/json
Authorization: Bearer <client-session-token>

{
  "action": "IPIFICATION_START",
  "attemptId": "<attempt-id>",
  "integrityToken": "<google-integrity-token>"
}
```

First, the backend checks that the attempt belongs to the current session, is unused and unexpired, and has the same allowed action. It selects the integrity policy from the stored action. The backend then uses Google credentials with the `https://www.googleapis.com/auth/playintegrity` scope to call:

```http
POST https://playintegrity.googleapis.com/v1/<configured-package-name>:decodeIntegrityToken
Authorization: Bearer <google-access-token>
Content-Type: application/json

{"integrity_token":"<google-integrity-token>"}
```

Evaluate `tokenPayloadExternal` on the backend. Require matching `requestPackageName` and `requestHash`, and a fresh `timestampMillis`, with a small allowance for clock skew. For an example Google Play distribution policy, require `PLAY_RECOGNIZED`, the configured app package and allowed signing certificate/version, `MEETS_DEVICE_INTEGRITY`, and `LICENSED`. Select verdict requirements for your distribution and risk policy; missing or `UNEVALUATED` fields must not count as a pass. See [Google's verdict reference](https://developer.android.com/google/play/integrity/verdicts).

After acceptance, atomically consume the attempt and create a local transaction. Persist:

| Field | Purpose |
| --- | --- |
| Transaction ID | Random, unique identifier |
| Session owner | Binds completion to the initiating client session |
| Action | Restricts the transaction to `IPIFICATION_START` |
| Attempt ID and integrity acceptance time | Links the accepted integrity check |
| Policy version | Records which integrity requirements passed |
| Expiry and status | Enforces a short validity window and single use |
| Authorization configuration | Pins environment, client ID, redirect URI, scopes and intended phone number |
| State digest | Allows exact comparison with the issued state |

Generate state using a maintained signing library. An illustrative signed JWT payload is:

```json
{
  "iss": "client-backend",
  "aud": "ipification-completion",
  "jti": "<random-transaction-id>",
  "iat": 1800000000,
  "exp": 1800000300
}
```

These timestamps illustrate a five-minute window; generate current values at runtime and choose a window appropriate for your flow. Pin the signature algorithm and trusted keys, and validate issuer, audience and timestamps. Store session ownership in the transaction, and do not put phone numbers, credentials, or integrity tokens in state. Signed state is readable, not encrypted.

Only after integrity acceptance and successful transaction persistence does the backend return a successful API response, for example:

```json
{
  "action": "IPIFICATION_START",
  "transactionId": "<random-transaction-id>",
  "state": "<backend-signed-state>",
  "expiresAt": "<transaction-expiry-in-UTC>",
  "authorizationUrl": "https://<api-server>/auth/realms/ipification/protocol/openid-connect/auth?<configured-parameters>&state=<url-encoded-backend-signed-state>"
}
```

The `state` field and the decoded `state` parameter in `authorizationUrl` must contain the same value. The app saves that value for callback comparison and immediately starts IPification using the returned URL, without inserting additional user prompts or unrelated work. If integrity verification fails, the API must not issue signed state or an authorization URL; return the applicable error or remediation outcome instead.

The validity window must cover authorization, carrier redirects, and submission of the callback to the backend. The backend enforces expiry using its own clock. If the app is interrupted or state expires before completion, start a fresh verification attempt with a new integrity token and newly issued state; do not reuse or extend the old state.

A signature does not replace the transaction record or single-use enforcement. A random opaque state backed by the same server-side record is also a valid alternative design; this guide uses backend-signed state.

## Phase 2 — Run IPification

### 3. Start authorization with the backend-issued state

Start this step as soon as the signed-state API returns successfully. Keep the API call and IPification launch together in the same authentication action.

Use the authorization endpoint supplied during onboarding. The request below follows the [Android integration guide](android.md); encode every query parameter using a URL builder.

```text
GET https://<api-server>/auth/realms/ipification/protocol/openid-connect/auth
    ?response_type=code
    &client_id=<client-id>
    &redirect_uri=<registered-redirect-uri>
    &scope=openid%20ip%3Aphone_verify
    &login_hint=<phone-number>
    &state=<url-encoded-backend-signed-state>
```

In the [Android cellular example](../examples/android/request-scoped/IPificationService.kt), use the backend-issued authorization URL instead of generating state locally with `generateState()`. Validate that the URL targets the configured IPification endpoint before opening it. Keep authorization and required carrier redirects on the cellular interface; Google calls and Client Backend calls can use normal connectivity.

When the registered callback is received:

```text
<registered-redirect-uri>?code=<authorization-code>&state=<returned-state>
```

Validate the callback destination and require one nonempty `code` and one `state`. Compare returned state exactly with the state saved for this attempt. Handle OAuth error callbacks as failures. Remove example logging of callback URLs, codes and state before using this integration.

IPification returns state for correlation. This design does not require IPification to validate the client's state signature or interpret Play Integrity verdicts.

## Phase 3 — Complete verification

### 4. Validate and claim the transaction

Forward the result to a Client Backend endpoint in the original session:

```http
POST /verification/complete
Content-Type: application/json
Authorization: Bearer <client-session-token>

{"code":"<authorization-code>","state":"<returned-state>"}
```

The following is backend pseudocode. Database, signing, Google and IPification helpers are application-specific, not SDK methods:

```text
complete(session, code, state):
    require code and state are nonempty and within input-size limits
    claims = verifySignatureAndClaims(state, pinnedKeys, algorithm, issuer, audience)
    tx = loadTransaction(claims.jti)
    require tx exists and tx.sessionOwner == session.id
    require tx.action == IPIFICATION_START
    require constantTimeEqual(sha256(state), tx.stateDigest)
    require now < tx.expiresAt
    require tx.integrityAccepted and integrityIsRecentEnough(tx, now)

    // One database operation: only one concurrent completion can succeed.
    require atomicClaim(tx.id, session.id, expectedStatus=READY,
                        requireUnexpired=true, newStatus=EXCHANGING)

    try:
        response = exchangeCode(code, tx.savedAuthorizationConfiguration)
        result = validateIPificationResponse(response, tx)
        decision = applyClientSecurityPolicy(result, tx)
        persistOutcome(tx.id, status=COMPLETED, decision=decision)
        return decision
    catch definitiveFailure:
        persistOutcome(tx.id, status=FAILED, decision=DENY)
        return DENY
    catch timeoutOrUnknownOutcome:
        persistOutcome(tx.id, status=INDETERMINATE, decision=REVIEW)
        return REVIEW
```

Keep the claim consumed after failure. Do not automatically return it to `READY`: the code may already have been redeemed. Recover interrupted exchanges through a controlled reconciliation process or start a fresh verification. Duplicate completion requests must not trigger another exchange; an optional result endpoint can return a stored outcome to the owning session.

### 5. Exchange the code and apply client policy

The Client Backend calls the configured IPification token endpoint using the confidential client authentication method supplied during onboarding:

```http
POST <configured-ipification-token-endpoint>
Content-Type: application/x-www-form-urlencoded
<configured-client-authentication>

grant_type=authorization_code&code=<encoded-code>&redirect_uri=<encoded-original-redirect-uri>
```

Use the redirect URI and credentials saved for this transaction; do not accept replacement configuration from the app. If PKCE is supported for your integration, use S256 and bind its challenge/verifier to the transaction. Confirm support during onboarding.

Validate the documented token response and phone verification result against the intended operation. A returned code or HTTP 200 alone does not establish successful phone verification. Where ID tokens are used, validate signature, issuer, audience, expiry, and nonce when requested. Apply the response requirements supplied for your IPification integration.

| Decision | Example meaning |
| --- | --- |
| ALLOW | Integrity requirements, IPification verification and client policy all pass |
| REVIEW | Additional verification or manual review is required; do not grant the protected action |
| DENY | Verification fails or client policy blocks the action |

Persist the outcome before returning the decision. Return only the application result needed by the mobile app, and retain only minimal audit metadata.

## Security boundaries and verification checklist

Signed state links an accepted integrity check to a backend transaction. It does not cryptographically prove that the later cellular request came from the same device, or bind an authorization code to that device. Session binding, callback validation, single-use records and supported OAuth protections remain necessary. Treat stronger device binding as a separate requirement.

Before release, verify these cases in your test environment:

- Accepted integrity and successful IPification verification reach the intended policy decision.
- Wrong request hash, package, signing certificate, stale token, or unacceptable verdict prevents transaction issuance.
- Changed, missing, expired, or wrong-session state is rejected before code exchange.
- Unsupported or substituted actions, altered operation data, and cross-action grant reuse are rejected. Protected APIs reject missing, expired, wrong-session or already-used grants.
- Concurrent or repeated completion calls produce at most one token exchange.
- Integrity service errors, cellular failures, OAuth errors and token-exchange timeouts do not grant access.
- No credentials, integrity tokens, authorization codes, or full callback URLs appear in logs.

The Kotlin helper is an integration snippet; backend endpoints and pseudocode must be implemented in your application. This document does not represent a runnable end-to-end sample.
