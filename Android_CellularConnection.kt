import android.annotation.TargetApi
import android.content.Context
import android.content.Context.CONNECTIVITY_SERVICE
import android.net.ConnectivityManager
import android.net.ConnectivityManager.NetworkCallback
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.os.Build
import android.util.Log
import okhttp3.*


class CellularTest {

   // required permission:  INTERNET, CHANGE_NETWORK_STATE
   // external library: OkHttp : com.squareup.okhttp3:okhttp

   @TargetApi(Build.VERSION_CODES.LOLLIPOP)
   fun callAPIonCellularNetwork(context: Context, url: String) {
       // 1. check if cellular network is enabled
       // 2. force network connection via cellular interface
       val connectivityManager =
           context.getSystemService(CONNECTIVITY_SERVICE) as ConnectivityManager
       val request = NetworkRequest.Builder()
           .addTransportType(NetworkCapabilities.TRANSPORT_CELLULAR)
           .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET).build()

       // If your app supports Android 21+, you need to implement handling timeout.     
       // Android 21++ support requestNetwork (NetworkRequest request,
       //                ConnectivityManager.NetworkCallback networkCallback)
       // Android 26++ support requestNetwork(NetworkRequest request,
       //                ConnectivityManager.NetworkCallback networkCallback,
       //                int timeoutMs)
       // https://developer.android.com/reference/android/net/ConnectivityManager#requestNetwork(android.net.NetworkRequest,%20android.net.ConnectivityManager.NetworkCallback)
       connectivityManager.requestNetwork(request, object : NetworkCallback() {
           override fun onAvailable(network: Network) {
               //using OkHTTP library to make the connection
               val httpBuilder =
                   OkHttpClient.Builder().socketFactory(network.socketFactory)
               // enable dns based on cellular network
               val dns = NetworkDns.instance
               dns.setNetwork(network)
               httpBuilder.dns(dns)

               val okHttpClient = httpBuilder.build()
               val mRequestBuilder = Request.Builder()
                   .url(url)
               
               try {
                   val response: Response = okHttpClient.newCall(mRequestBuilder.build()).execute()
                   Log.i(
                       "TestAPI", " callAPIonCellularNetwork RESULT:${response.body?.string()}"
                   )
               } catch (ex: Exception) {
                   ex.printStackTrace()
                    Log.e("TestAPI", ex.message!!)
               }
           }
           override fun onUnavailable() {
               // cellular network is not available, callback
               Log.e("TestAPI","cellular network is not available")
           }
       }
       , 5000 // CONNECT_NETWORK_TIMEOUT
      )
   }
}
