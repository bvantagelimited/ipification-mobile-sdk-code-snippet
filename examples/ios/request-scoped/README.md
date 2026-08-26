# iOS request-scoped cellular routing

[`IPificationService.swift`](IPificationService.swift) uses Apple's Network framework and
sets `NWParameters.requiredInterfaceType = .cellular` on connections controlled by the
service.

iOS does not expose a public API for binding every connection in an app process to cellular.
Each connection must be created with the appropriate Network framework parameters.

See the [iOS integration guide](../../../docs/ios.md) and the
[Objective-C compatibility guide](../../../docs/ios-objective-c.md).
