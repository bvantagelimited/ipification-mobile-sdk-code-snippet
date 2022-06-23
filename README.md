# Using Cellular Network for Phone Number Verification even when Wifi is on

Using WiFi is more complicated when trying to use `IPification Authentication`. By default, all traffic on all operating systems will favor WiFi above cellular connections. However for IPification, the API request must be made using the cellular connection. We realize that users are unlikely to turn off WiFi and so the following code is provided for both iOS and Android to include in your applications that will allow a small payload to be delivered over the cellular interface, even when WiFi is connected. Telcos usually don’t charge (zero rate) our Authentication URLs so end users won’t have any cost.


## Android

The following will allow Android applications using Android API 21 and above to use Cellular for the API request.

### Minimum Android 
Android 5.0 (API 21) and up

### Permission
```
<uses-permission android:name="android.permission.INTERNET" />
<uses-permission android:name="android.permission.CHANGE_NETWORK_STATE" />
<uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />
```
### Network 3rd Library:
OKHttp3 version 3 / 4
We use OKHttp3 because it support `socket` and target `DNS`

```
implementation 'com.squareup.okhttp3:okhttp:4.9.0'
```


### Sample Code

Here's a function that can simplify the action of preferring certain types of networks for your application
```
val builder: NetworkRequest.Builder = NetworkRequest.Builder()
builder.addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET);
builder.addTransportType(NetworkCapabilities.TRANSPORT_CELLULAR);
```
After this you are able to get `onAvailable` callback from system and later you set process default network as mobile data.
```
val mNetworkCallBack = object: ConnectivityManager.NetworkCallback() {
    override fun onAvailable(network: Network) {
        super.onAvailable(network)
    }
    override fun onUnavailable() {
        super.onUnavailable()
    }
    override fun onLost(network: Network) {
        super.onLost(network)
    }
}
```
```
val manager = mContext.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
manager.requestNetwork( builder.build(), mNetworkCallBack)
```



