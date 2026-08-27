# Whole-app cellular IP demo app
<img width="300" height="700" alt="Screenshot_20260827_214803" src="https://github.com/user-attachments/assets/c3388c9f-99b0-4eca-8853-904057a86ca3" />

This minimal Android app calls
[`https://api.ipify.org?format=json`](https://api.ipify.org?format=json) three times:

1. Before process binding, through Android's normal default network.
2. While the app process is bound to a cellular `Network`.
3. After removing the binding and restoring Android's normal network selection.

With Wi-Fi and mobile data enabled, the first and third values should normally match, while the
second may show the carrier's public IP. Carrier-grade NAT, a VPN, or network policy can make the
addresses appear equal even when the socket used cellular, so equal results alone do not prove
that binding failed.

## Run the app

1. Open this directory as a project in Android Studio.
2. Allow Gradle sync to finish.
3. Connect a physical Android device running Android 6.0 (API 23) or newer.
4. Enable both Wi-Fi and mobile data.
5. Run the `app` configuration and tap **Run 3-step IP check**.

The app also provides two browser-routing tests:

- **Test WebView while forced** binds this app process to cellular, then loads the IP endpoint
  in a new WebView. Closing the screen restores normal routing.
- **Test Custom Tab limitation** binds this app process and opens the endpoint in a Custom Tab.
  A Custom Tab runs in the selected browser application's process, so
  `bindProcessToNetwork()` in this app cannot force the tab to cellular. This option documents
  and demonstrates that limitation.

An emulator usually cannot demonstrate a separate carrier route. Test on the same real devices
and mobile operators used for the client integration.

## What proves the snippet is active

The network forcing logic is isolated in
[`ProcessCellularBinding.kt`](app/src/main/java/com/ipification/example/ipcheck/ProcessCellularBinding.kt).
The activity keeps one instance and calls it like the main process-scoped code snippet:

```kotlin
private val cellularBinding by lazy {
    ProcessCellularBinding(applicationContext)
}

cellularBinding.bind(
    onBound = {
        // Create a new HTTP connection here. It will use cellular.
        checkPublicIp()
    },
    onFailure = { message -> showError(message) },
    onLost = { showError("The cellular network was lost") },
)

// Call this after the final cellular request succeeds, fails, or is cancelled.
cellularBinding.unbind()
```

Inside the helper, the core forcing call is:

```kotlin
connectivityManager.bindProcessToNetwork(cellularNetwork)
```

It affects only sockets created after binding. The sample therefore sends `Connection: close`,
disconnects each `HttpURLConnection`, and creates a new connection for every step. It restores
normal routing with:

```kotlin
connectivityManager.bindProcessToNetwork(null)
```

The callback is then unregistered. The app also performs this cleanup after failures, timeouts,
network loss, and activity destruction.

This project demonstrates Android process-wide binding. For production IPification integration,
request-scoped routing remains preferred because whole-process binding also affects unrelated
API clients and SDKs in the same app process.
