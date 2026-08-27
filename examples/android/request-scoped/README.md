# Android request-scoped cellular routing

This is the recommended Android example. It requests a cellular `Network`, then configures
the IPification OkHttp client with that network's socket factory and DNS resolver. Other app
traffic continues to use Android's normal default network.

## Files

- [`IPificationService.kt`](IPificationService.kt): coverage and authentication flow
- [`util/NetworkDns.kt`](util/NetworkDns.kt): DNS resolution through the selected network
- [`util/NetworkUtils.kt`](util/NetworkUtils.kt): connectivity checks
- [`resources`](resources): Android network security configuration examples

See the [Android integration guide](../../../docs/android.md) for permissions, dependencies,
flow details, and cleartext HTTP configuration.
