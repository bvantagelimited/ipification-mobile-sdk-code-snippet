# IPification for iOS

This guide describes request-scoped cellular routing for IPification coverage and mobile
authentication on iOS.

## 1. Requirements

| Item | Details |
| --- | --- |
| Minimum OS | iOS 12 or newer |
| Device prerequisite | Cellular data must be enabled and a usable cellular network must be available. |
| Frameworks | `Foundation` and Apple's `Network` framework |

## 2. Cellular-routing scope

Set the required interface type on each connection used by the IPification flow:

```swift
let parameters = NWParameters(tls: tlsOptions, tcp: tcpOptions)
parameters.requiredInterfaceType = .cellular

let connection = NWConnection(
    host: NWEndpoint.Host(host),
    port: NWEndpoint.Port(rawValue: port)!,
    using: parameters
)
```

This setting applies only to the `NWConnection` created with those parameters. iOS does not
provide a public equivalent to Android's `bindProcessToNetwork()` and cannot force every
connection in the app process to use cellular.

Create cellular-scoped connections for the Coverage request, Authorization request, and every
required intermediate redirect. Keep the connection active until its request reaches a terminal
success or failure, then cancel it. Unrelated application traffic continues using the
system-selected default network, such as Wi-Fi.

## 3. Authentication flow

### 3.1 Check coverage

1. Build the Coverage request using the client ID and the user's phone number.
2. Send it through a cellular-scoped connection.
3. Read the `available` value from the response:
   - `true`: continue to Authorization.
   - `false`: use the configured fallback authentication method.

### 3.2 Start authorization

1. Generate a cryptographically random `state` value and retain it for validation.
2. Build the Authorization request using the parameters below.
3. Send the request through a cellular-scoped connection.
4. Continue intermediate HTTP redirects through cellular until one of these terminal results:
   - The response contains the registered `redirect_uri` with `code` and `state`.
   - The request fails, times out, or cellular becomes unavailable.
5. Verify that the returned `state` exactly matches the value created for this flow before
   accepting the authorization `code`.

Network callbacks may run away from the main thread. Dispatch UI updates to the main queue.
Cancel the `NWConnection` in every terminal success, failure, timeout, and cancellation path.

### 3.3 Complete verification through your backend

The mobile app must not perform the confidential token exchange directly:

1. Send the short-lived authorization `code` to **your own backend** over HTTPS. Include the
   same `redirect_uri` and any internal transaction/session identifier needed for correlation.
2. Your backend sends the code to the configured IPification token endpoint using the
   confidential client credentials supplied during onboarding and the same `redirect_uri`.
3. Your backend validates the token response and verification result, creates or updates the
   application session, and returns only the required result to the app.

The server-to-server token exchange runs on your backend and does not use the device's cellular
connection. Never embed the client secret in the iOS app, and do not log or persist authorization
codes, tokens, or confidential credentials.

## 4. Authorization request

```http
GET https://{api-server}/auth/realms/ipification/protocol/openid-connect/auth?
response_type=code&
client_id={client-id}&
redirect_uri={client-callback-uri}&
scope=openid%20ip%3Aphone_verify&
state={state}&
login_hint={login-hint}
```

Possible responses:

```text
200: {redirect_uri}?code={authorization-code}&state={state}
30x: Location: {intermediate-telco-url}
```

| Name | Description |
| --- | --- |
| `api-server` | API host for the configured environment or client deployment. |
| `client_id` | Public client identifier supplied during onboarding. |
| `redirect_uri` | Registered client callback URI. Use the same value throughout the flow and during the backend token exchange. |
| `scope` | Use `openid ip:phone_verify` for phone-number verification. |
| `state` | Cryptographically random, per-flow value used to prevent response substitution and CSRF. |
| `login_hint` | End-user phone number in E.164 format without the leading `+`. |
| `consent_id` (optional) | Traceable consent identifier, when required by the integration. |
| `consent_timestamp` (optional) | Time consent was accepted, as a Unix timestamp in seconds. |
| `mcc` (optional) | Mobile Country Code. |
| `mnc` (optional) | Mobile Network Code. |

## 5. iOS code sample

- [`IPificationService.swift`](../examples/ios/request-scoped/IPificationService.swift)
- [Request-scoped routing notes](../examples/ios/request-scoped/README.md)
- [Objective-C compatibility guide](ios-objective-c.md)
- [IPification iOS developer documentation](https://developer.ipification.com/#/ios/latest/)

## License

```text
Copyright 2022 IPification, Inc.

Licensed to the Apache Software Foundation (ASF) under one or more contributor
license agreements. See the NOTICE file distributed with this work for
additional information regarding copyright ownership. The ASF licenses this
file to you under the Apache License, Version 2.0 (the "License"); you may not
use this file except in compliance with the License. You may obtain a copy of
the License at

http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
License for the specific language governing permissions and limitations under
the License.
```
