# Route an Android app process through cellular

`ProcessCellularBinding` requests a cellular network and calls
`ConnectivityManager.bindProcessToNetwork()`. This changes the default network for future
sockets and DNS lookups created by the current application process.

Use request-scoped routing for IPification whenever possible. Process-scoped routing also
affects unrelated API clients, analytics, image loading, downloads, and other SDKs in the
same process.

## Requirements

- Android 6.0 (API 23) or newer
- Mobile data enabled and a usable cellular network
- These manifest permissions:

```xml
<uses-permission android:name="android.permission.INTERNET" />
<uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />
<uses-permission android:name="android.permission.CHANGE_NETWORK_STATE" />
```

## Usage

Keep one instance in the component that owns the cellular-only period. Do not create a new
instance for each request.

```kotlin
private val cellularBinding by lazy { ProcessCellularBinding(applicationContext) }

fun startCellularOnlyMode() {
    cellularBinding.bind(
        timeoutMillis = 10_000,
        onBound = {
            // Create new HTTP clients/connections here, then start network work.
        },
        onUnavailable = {
            // Fall back or show that mobile data is unavailable.
        },
        onLost = {
            // Pause cellular-only work while Android looks for a replacement network.
        },
    )
}

fun stopCellularOnlyMode() {
    cellularBinding.unbind()
}
```

Call `unbind()` in every completion, cancellation, and error path. `close()` is an alias.

## Important limitations

- Only future sockets use the bound network. Existing sockets do not move to cellular.
- Recreate HTTP clients or evict their connection pools after binding and after unbinding.
- This affects the current process, not necessarily services running in another process.
- Explicitly network-bound sockets are unaffected.
- A lost cellular network breaks sockets created through that network. The helper clears the
  binding and keeps its request active so Android can provide a replacement.
- The helper assumes it exclusively owns process network binding. Do not combine it with
  another component that calls `bindProcessToNetwork()`.
- VPNs, WebView, and third-party SDK networking should be tested on target devices.

Android recommends individually bound sockets for narrow use cases. See the
[`ConnectivityManager` API](https://developer.android.com/reference/android/net/ConnectivityManager#bindProcessToNetwork(android.net.Network)).

## iOS

iOS has no public equivalent for changing the default network of an entire app process.
Use `NWParameters.requiredInterfaceType = .cellular` for each controlled `NWConnection`, as
shown in the repository's iOS request-scoped example.
