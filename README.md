# IPification mobile SDK code snippets

Using WiFi is more complicated when trying to use IPification Authentication. By default, all operating systems favor WiFi over cellular connections for all traffic. However, for IPification, the API request must be made using the cellular connection. We realize that users are unlikely to turn off WiFi, so the following code is provided for both iOS and Android to include in your applications. This will allow a small payload to be delivered over the cellular interface, even when WiFi is connected. Telcos usually don’t charge (zero rate) our Authentication URLs, so end users won’t incur any cost.

## Repository contents

| Area | Description |
| --- | --- |
| [`examples/android/request-scoped`](examples/android/request-scoped/README.md) | Recommended Android cellular request example |
| [`examples/android/process-scoped`](examples/android/process-scoped/README.md) | Advanced Android whole-process cellular binding |
| [`examples/ios/request-scoped`](examples/ios/request-scoped/README.md) | iOS cellular `NWConnection` example |
| [`docs`](docs) | Platform integration and configuration guides |
| [`im-authentication/android`](im-authentication/android/README.md) | Android IM authentication example |
| [`device-info`](device-info/README.md) | SIM and mobile-network device information snippets |
| [`deprecated`](deprecated/README.md) | Historical examples retained for reference only |

Request-scoped routing is recommended for IPification. Process-scoped routing affects every
future network connection created in the Android application process.

Files moved from the previous layout are listed in [`MOVED_FILES.md`](MOVED_FILES.md), along
with their compatibility policy.


## Android

The following will allow Android applications using Android API 21+ to use Cellular for the API request.

### Minimum Android 
Android 5.0 (API 21) and up

### Requirement: 
User’s device has the mobile network enabled.

### Permission
```xml
<uses-permission android:name="android.permission.INTERNET" />
<uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />
<uses-permission android:name="android.permission.CHANGE_NETWORK_STATE" />
<uses-permission android:name="android.permission.ACCESS_WIFI_STATE" />
```
### Network 3rd Library
OKHttp3 version 5 (supports IPv6/v4). <br/>
> We use OKHttp3 because it supports binding to the cellular network via socket and targeting DNS.

```groovy
    implementation 'com.squareup.okhttp3:okhttp:5.3.2'

```


### Core Function

Here's a function that can simplify the action of preferring certain types of networks for your application
```kotlin
val builder: NetworkRequest.Builder = NetworkRequest.Builder()
builder.addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET);
builder.addTransportType(NetworkCapabilities.TRANSPORT_CELLULAR);
```

After this you receive an `onAvailable()` callback and can route the required request through
the returned `Network` instance.
```kotlin
val mNetworkCallBack = object: ConnectivityManager.NetworkCallback() {
    override fun onAvailable(network: Network) {
        super.onAvailable(network)
        // TODO: process the connection via this network instance ${network}
    }
    override fun onUnavailable() {
        super.onUnavailable()
    }
    override fun onLost(network: Network) {
        super.onLost(network)
    }
}
```
```kotlin
val manager = mContext.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
manager.requestNetwork( builder.build(), mNetworkCallBack)
```

More Detail here:
[Android integration guide](docs/android.md)

### Whole-app process binding (advanced)

Android 6.0 (API 23) and newer can make cellular the default network for future sockets and
DNS lookups in the current app process. This has broad side effects, so request-scoped routing
remains the recommended approach for IPification.

- [Process-scoped cellular example](examples/android/process-scoped/README.md)
- [ProcessCellularBinding.kt](examples/android/process-scoped/ProcessCellularBinding.kt)

## iOS


For iOS 12 and newer, we use `NetWork framework` to make network connection via cellular interface.

iOS does not provide a public API that forces the entire app process onto cellular. Interface
selection must be applied to connections controlled by the app.


### Core Function
Using the following would be the correct approach to force the connection over cellular:

```swift
let tcpOptions = NWProtocolTCP.Options()
let params = NWParameters(tls: enableTLS ? options : nil, tcp: tcpOptions)
params.requiredInterfaceType = .cellular
self.connection =  NWConnection.init(host:  host  , port: port, using: params)
```
After the connection moves into the `.ready` state and the connection is setup on pdp_ip0 (cell)
```swift
connection.stateUpdateHandler = { (newState) in
    print("TCP state change to: \(newState)")
    switch newState {
    case .ready:
        print("ready")
        // self.delegate!.didConnect(socket: self)
        break
    case .waiting(let error):
        print("waiting error \(error.debugDescription ?? "")")
        break

    case .failed(let error):
        print("failed \(error.debugDescription ?? "")")
        // self.delegate?.didDisconnect(socket: self, error: error)
        break
    case .cancelled:
        print("cancelled" )
        break
    default:
        print("default")
        break
    }
}
connection.start(queue: .main)
```

More detail: [iOS integration guide](docs/ios.md)

---
