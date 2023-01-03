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

   // required permission:  INTERNET, ACCESS_WIFI_STATE, ACCESS_NETWORK_STATE, CHANGE_NETWORK_STATE
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

--------------------------
NetworkDns.kt
--------------------------
import android.net.Network
import android.os.Build
import android.os.Build.VERSION_CODES
import okhttp3.Dns
import java.net.InetAddress
import java.net.UnknownHostException


class NetworkDns private constructor() : Dns {
   private var mNetwork: Network? = null
   fun setNetwork(network: Network?) {
       mNetwork = network
   }

   @Throws(UnknownHostException::class)
   override fun lookup(hostname: String): List<InetAddress> {
       return if (mNetwork != null && Build.VERSION.SDK_INT >= VERSION_CODES.LOLLIPOP) {
           listOf(*mNetwork!!.getAllByName(hostname))
       } else Dns.SYSTEM.lookup(hostname)
   }

   companion object {
       private var sInstance: NetworkDns? = null
       val instance: NetworkDns
           get() {
               if (sInstance == null) {
                   sInstance = NetworkDns()
               }
               return sInstance!!
           }
   }
}



--------------------------
HandleRedirectInterceptor.kt (need to implement in case handling redirect urls)
--------------------------
import android.content.Context
import android.os.Build
import android.util.Log
import okhttp3.*
import okhttp3.ResponseBody.Companion.toResponseBody

class HandleRedirectInterceptor(ctx: Context, requestUrl: String, redirect_uri: String) : Interceptor {
    private var redirectUri: String = redirect_uri
    private var url: String = requestUrl
   
    override fun intercept(chain: Interceptor.Chain): Response {

        val request: Request = chain.request()
        val response: Response = response = chain.proceed(request)
        // check and return success response if location match with defined redirect-uri
        if (response.code in 300.. 399){
           if ((response.headers["location"] != null && response.headers["location"]!!.startsWith(redirectUri)) 
                  || (response.headers["Location"] != null && response.headers["Location"]!!.startsWith(redirectUri)))
               {
                  val builder: Response.Builder = Response.Builder().request(request).protocol(Protocol.HTTP_1_1)
                  val contentType: MediaType? = response.body!!.contentType()
                  
                  val locationRes = response.headers["location"] ?: response.headers["Location"] ?: ""
                  val body = locationRes.toResponseBody(contentType)
                  builder.code(200).message("success").body(body)
                  
                  // close the response body
                  response.body?.close()
                  
                  return builder.build()
               }
        }
        return response
    }
}


