# Moved files and compatibility paths

The repository was reorganized to separate active examples, documentation, supporting
resources, and deprecated code. Existing public entry points are retained as compatibility
files where practical.

## Public entry points

| Previous path | Current path |
| --- | --- |
| `android_IPificationService.kt` | [`examples/android/request-scoped/IPificationService.kt`](examples/android/request-scoped/IPificationService.kt) |
| `ios_IPificationService.swift` | [`examples/ios/request-scoped/IPificationService.swift`](examples/ios/request-scoped/IPificationService.swift) |
| `android_sdk_core_document.md` | [`docs/android.md`](docs/android.md) |
| `ios_sdk_core_document.md` | [`docs/ios.md`](docs/ios.md) |
| `ios_Objective-C_Guideline.md` | [`docs/ios-objective-c.md`](docs/ios-objective-c.md) |
| `android-phone-number-hint.md` | [`docs/android-phone-number-hint.md`](docs/android-phone-number-hint.md) |
| `custom_urls.md` | [`docs/custom-urls.md`](docs/custom-urls.md) |
| `xml/ipification_network_security_config.xml` | [`examples/android/request-scoped/resources/ipification_network_security_config.xml`](examples/android/request-scoped/resources/ipification_network_security_config.xml) |
| `xml/ipification_network_security_config_for_id.xml` | [`examples/android/request-scoped/resources/ipification_network_security_config_for_id.xml`](examples/android/request-scoped/resources/ipification_network_security_config_for_id.xml) |

## Deprecated and supporting files

| Previous path | Current path |
| --- | --- |
| `Android_CellularConnection.kt` | [`deprecated/android/CellularConnection.kt`](deprecated/android/CellularConnection.kt) |
| `[Deprecated]_iOS_CellularConnection.swift` | [`deprecated/ios/CellularConnection.swift`](deprecated/ios/CellularConnection.swift) |
| `mccmnc/Android_DeviceInfo.kt` | [`device-info/android/DeviceInfo.kt`](device-info/android/DeviceInfo.kt) |
| `mccmnc/[Deprecated]_iOS_DeviceInfo.swift` | [`deprecated/ios/DeviceInfo.swift`](deprecated/ios/DeviceInfo.swift) |
| `util/NetworkDns.kt` | [`examples/android/request-scoped/util/NetworkDns.kt`](examples/android/request-scoped/util/NetworkDns.kt) |
| `util/NetworkUtils.kt` | [`examples/android/request-scoped/util/NetworkUtils.kt`](examples/android/request-scoped/util/NetworkUtils.kt) |
| `im/` | [`im-authentication/android/`](im-authentication/android/README.md) |

## Compatibility policy

Compatibility files prevent common GitHub `blob/main` links from returning 404. They are
navigation aids, not duplicate maintained implementations. New integrations should always
use the current paths above. Raw source consumers must update to the current source paths.
