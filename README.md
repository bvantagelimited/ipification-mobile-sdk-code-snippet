# Using Cellular Network for Phone Number Verification even when Wifi is on

Using WiFi is more complicated when trying to use IPification Authentication. By default, all operating systems favor WiFi over cellular connections for all traffic. However, for IPification, the API request must be made using the cellular connection. We realize that users are unlikely to turn off WiFi, so the following code is provided for both iOS and Android to include in your applications. This will allow a small payload to be delivered over the cellular interface, even when WiFi is connected. Telcos usually don’t charge (zero rate) our Authentication URLs, so end users won’t incur any cost.



## Android

The following will allow Android applications using Android API 21+ to use Cellular for the API request.

### Minimum Android 
Android 5.0 (API 21) and up

### Permission
```xml
<uses-permission android:name="android.permission.INTERNET" />
<uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />
<uses-permission android:name="android.permission.CHANGE_NETWORK_STATE" />
<uses-permission android:name="android.permission.ACCESS_WIFI_STATE" />
```
### Network 3rd Library
OKHttp3 version 3 / 4. <br/>
> We use OKHttp3 because it supports binding to the cellular network via socket and targeting DNS.

```groovy
implementation 'com.squareup.okhttp3:okhttp:4.9.3'
```


### Core Function

Here's a function that can simplify the action of preferring certain types of networks for your application
```kotlin
val builder: NetworkRequest.Builder = NetworkRequest.Builder()
builder.addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET);
builder.addTransportType(NetworkCapabilities.TRANSPORT_CELLULAR);
```

After this you are able to get `onAvailable()` callback from system and later you set process default network as mobile data.
```kotlin
val mNetworkCallBack = object: ConnectivityManager.NetworkCallback() {
    override fun onAvailable(network: Network) {
        super.onAvailable(network)
        // TODO: process the connection via this network
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
[Android Document](https://github.com/bvantagelimited/ipification-mobile-sdk-code-snippet/blob/main/android_sdk_core_document.md)

## iOS


For iOS 12 and newer, we use `NetWork framework` to make network connection via cellular interface.


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

More Detail here: [iOS Document](https://github.com/bvantagelimited/ipification-mobile-sdk-code-snippet/blob/main/ios_sdk_core_document.md)

---
