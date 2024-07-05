package com.ipification.demoapp.manager

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.net.Uri
import android.os.Build
import android.os.Build.VERSION_CODES
import android.util.Base64
import android.util.Log
import android.util.Patterns
import androidx.annotation.RequiresApi
import okhttp3.Call
import okhttp3.Callback
import okhttp3.Cookie
import okhttp3.CookieJar
import okhttp3.Dns
import okhttp3.HttpUrl
import okhttp3.Interceptor
import okhttp3.MediaType
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.json.JSONObject
import java.io.IOException
import java.lang.Exception
import java.lang.reflect.Method
import java.net.Inet4Address
import java.net.InetAddress
import java.net.UnknownHostException
import java.security.SecureRandom
import java.util.Timer
import java.util.TimerTask
import java.util.concurrent.TimeUnit


/**
 * IPificationService class handles the network requests for coverage and authentication.
 * It constructs the necessary URLs with the required parameters and makes requests to the IPification API.
 * The class includes functions to perform coverage requests and authentication requests.
 * It also includes helper functions to construct URLs, generate random states, and extract parameters from responses.
 */
class IPificationService {
    private var coverageRequesting: Boolean = false
    private var authRequesting: Boolean = false
    private val TAG: String = "IPificationService"
    val environment = "stage" // or "live"

    // performRequest determines the network request handling logic based on network status
    fun performCoverageRequest(context: Context) {
        Log.d(TAG, "performCoverageRequest...")

        val coverageURLString = getCoverageURL(environment)
        val clientID = "" // TODO
        val phoneNumber = "" // TODO

        if (clientID.isEmpty()) {
            Log.e(TAG, "clientID is null or empty")
            return
        }


        if (phoneNumber.isEmpty()) {
            Log.e(TAG, "phoneNumber is null or empty")
            return
        }

        if (coverageRequesting) {
            Log.e(TAG, "requesting .... ignored")
            return
        }
        coverageRequesting = true

        // Create an instance of IPificationCoreService and connect to the coverage URL
        val ipService = IPificationCoreService(null)

        // Construct the coverage URL with query parameters
        // - coverageURLString: The base URL for the coverage service
        // - clientID: The unique identifier for the client making the request
        // - phoneNumber: The phone number for which coverage information is being requested
        // These parameters are appended to the base URL as query parameters to form the final request URL
        val coverageUrlWithParams = "$coverageURLString?client_id=$clientID&phone=$phoneNumber"

        ipService.connectTo(context, coverageUrlWithParams, APIType.COVERAGE, object : IPCallback {
            override fun onSuccess(response: String) {
                Log.d(TAG, response)
                coverageRequesting = false
                // Handle the success response
                // Supported telco
                // TODO: Start authentication with the phone number
                performAuthenticationRequest(context)
            }

            override fun onFailure(error: String) {
                Log.e(TAG, "checkCoverage error: $error")
                coverageRequesting = false
                // Handle the failure response
                // Unsupported telco or error
                // TODO: Fallback to SMS flow
            }
        })
    }

    fun performAuthenticationRequest(context: Context) {
        Log.d(TAG, "performAuthenticationRequest...")

        val authURLString = getAuthURL(environment)
        val clientID = "" // TODO
        val redirectUri = "" // TODO
        val phoneNumber = "" // TODO

        if (clientID.isEmpty()) {
            Log.e(TAG, "clientID is null or empty")
            return
        }

        if (redirectUri.isEmpty()) {
            Log.e(TAG, "redirectUri is null or empty")
            return
        }

        if (phoneNumber.isEmpty()) {
            Log.e(TAG, "phoneNumber is null or empty")
            return
        }

        // Prevent multiple clicks
        if (authRequesting) {
            Log.e(TAG, "requesting .... ignored")
            return
        }
        authRequesting = true

        // Create an instance of IPificationCoreService and connect to the auth URL
        val ipService = IPificationCoreService(redirectUri)
        val randomState = generateState()

        // Construct the URL with parameters for authentication
        // - authURLString: The base URL for the authentication service
        // - clientID: The unique identifier for the client making the request
        // - redirectUri: The URI to redirect to after authentication
        // - phoneNumber: A hint for the login process, typically the user's phone number
        // - response_type=code: Indicates that the response will include an authorization code
        // - scope=openid%20ip:phone_verify: Specifies the scopes of the request, including OpenID and phone verification
        // - randomState: A randomly generated string to maintain state between the request and callback
        // These parameters are appended to the base URL using a StringBuilder to form the final authentication URL
        val authUrlWithParams = StringBuilder(authURLString).apply {
            append("?client_id=").append(clientID)
            append("&redirect_uri=").append(redirectUri)
            append("&login_hint=").append(phoneNumber)
            append("&response_type=code")
            append("&scope=openid%20ip:phone_verify")
            append("&state=").append(randomState)
        }.toString()

        ipService.connectTo(context, authUrlWithParams, APIType.AUTH, object : IPCallback {
            override fun onSuccess(response: String) {
                Log.d(TAG, response)
                authRequesting = false
                // Handle the success response
                // Extract code and state from the response
                val code = getParam("code", response)
                val state = getParam("state", response)
                Log.d(TAG, "$state -  $code")
                if (code != null) {
                    // TODO: Call TokenExchange to check {code}
                    Log.d(TAG, "code value: $code")
                } else {
                    // TODO: Fallback to SMS flow
                }
            }

            override fun onFailure(error: String) {
                authRequesting = false
                Log.e(TAG, "auth error: $error")
                // Handle the failure response
                // TODO: Fallback to SMS flow
            }
        })
    }

    /**
     * Constructs the coverage URL string based on the provided environment.
     *
     * @param environment The environment for the URL, either "live" or "stage".
     * @return The constructed coverage URL string.
     */
    private fun getCoverageURL(environment: String): String {
        val baseUrl = when (environment) {
            "live" -> "https://api.ipification.com"
            else -> "https://stage.ipification.com"
        }
        return "$baseUrl/auth/realms/ipification/coverage"
    }

    /**
     * Constructs the authentication URL string based on the provided environment.
     *
     * @param environment The environment for the URL, either "live" or "stage".
     * @return The constructed authentication URL string.
     */
    private fun getAuthURL(environment: String): String {
        val baseUrl = when (environment) {
            "live" -> "https://api.ipification.com"
            else -> "https://stage.ipification.com"
        }
        return "$baseUrl/auth/realms/ipification/protocol/openid-connect/auth"
    }

    // Generate a random state string with a length of 16 characters
    private fun generateState(): String {
        val STATE_LENGTH = 16
        val sr = SecureRandom()
        val random = ByteArray(STATE_LENGTH)
        sr.nextBytes(random)
        var result = Base64.encodeToString(random, Base64.NO_WRAP or Base64.NO_PADDING or Base64.URL_SAFE)
        if (result.length > STATE_LENGTH) {
            result = result.substring(0, STATE_LENGTH)
        }
        return "ip-sdk-$result"
    }

    // Extract a query parameter value from the response body
    fun getParam(dataKey: String, responseBody: String): String? {
        return try {
            val data = Uri.parse(responseBody)
            if (data.isHierarchical) {
                data.getQueryParameter(dataKey)
            } else {
                null
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}




enum class APIType {
    COVERAGE,
    AUTH
}

interface IPCallback {
    fun onSuccess(response: String)
    fun onFailure(error: String)
}
class IPificationCoreService(redirectUri: String?) {
    private val CONNECT_NETWORK_TIMEOUT : Long = 5000L
    private val TAG: String = "IPificationCoreService"
    private var mRedirectUri: String? = redirectUri

    private lateinit var mRequestUrl : String
    private var mCallback : IPCallback? = null
    private var mApiType : APIType = APIType.COVERAGE
    fun connectTo(context: Context, urlString: String, apiType: APIType, callback: IPCallback) {

        // validation
        if (mApiType == APIType.AUTH && mRedirectUri.isNullOrEmpty()) {
            Log.e(TAG, "Error: The REDIRECT_URI is invalid or empty. Please provide a valid redirect URI.")
            callback.onFailure("The REDIRECT_URI is invalid or empty. Please provide a valid redirect URI.")
            return
        }
        // Parse the URL
        if (!isValidUrl(urlString)) {
            callback.onFailure("Request URL is not valid")
            return
        }

        if(!isMobileDataEnabled(context)){
            callback.onFailure("cellular network is not available")
            return
        }


        this.mRequestUrl = urlString
        this.mCallback = callback
        this.mApiType = apiType

        // start force cellular network
        requestCellularNetwork(context)
    }
    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    private fun requestCellularNetwork(context: Context) {
        Log.d(TAG, "requestCellularNetwork...")
        // 1. force network connection via cellular interface
        // If your app supports Android 21+, you need to implement handling timeout manually.
        // Android 21++ support requestNetwork (NetworkRequest request,
        //                ConnectivityManager.NetworkCallback networkCallback)
        // Android 26++ support requestNetwork(NetworkRequest request,
        //                ConnectivityManager.NetworkCallback networkCallback,
        //                int timeoutMs)
        // https://developer.android.com/reference/android/net/ConnectivityManager#requestNetwork(android.net.NetworkRequest,%20android.net.ConnectivityManager.NetworkCallback)

        val connectivityManager =
            context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val request = NetworkRequest.Builder()
            .addTransportType(NetworkCapabilities.TRANSPORT_CELLULAR)
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET).build()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            connectivityManager.requestNetwork(
                request,
                object : ConnectivityManager.NetworkCallback() {
                    override fun onAvailable(network: Network) {
                        Log.d(TAG, "onAvailable...")
                        processRequestWithNetwork(network)
                    }

                    override fun onUnavailable() {
                        // cellular network is not available, call the callback error
                        Log.e(TAG, "cellular network is not available")
                        handleUnAvailableCase()
                    }
                },
                CONNECT_NETWORK_TIMEOUT.toInt() // CONNECT_NETWORK_TIMEOUT
            )
        } else {
            // manual adding timeout
            var isReceiveResponse = false
            connectivityManager.requestNetwork(
                request,
                object : ConnectivityManager.NetworkCallback() {
                    override fun onAvailable(network: Network) {
                        isReceiveResponse = true
                        processRequestWithNetwork(network)
                    }

                    override fun onUnavailable() {
                        isReceiveResponse = true
                        // cellular network is not available, callback
                        Log.e(TAG, "cellular network is not available")
                        handleUnAvailableCase()
                    }
                }
            )
            Timer().schedule(object : TimerTask() {
                override fun run() {
                    Log.d(TAG, "timeout isReceiveResponse=${isReceiveResponse} ")
                    if (!isReceiveResponse) {
                        handleUnAvailableCase()
                    }
                }
            }, CONNECT_NETWORK_TIMEOUT)
        }
    }

    /**
     * This function processes the request using the provided Network object.
     *
     * @param network The network object used to process the request.
     */
    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    private fun processRequestWithNetwork(network: Network) {
        Log.d(TAG, "processRequestWithNetwork...")
        // Add the implementation for processing the request with the network
        // using OkHTTP library to make the connection
        val httpBuilder = OkHttpClient.Builder()
        // enable socket for network
        httpBuilder.socketFactory(network.socketFactory)

        // add dns if needed
        if (!isIPEndpoints(mRequestUrl)) {
            // enable DNS resolver with cellular network
            // enable dns based on cellular network
            val dns = NetworkDns.instance
            dns.setNetwork(network)
            httpBuilder.dns(dns)

            // handle cookie (for special market telcos: RU,UK)
            httpBuilder.cookieJar(cookieJar)
        }

        //check and handle the response with redirect_uri
        if(mApiType == APIType.AUTH){
            httpBuilder.addNetworkInterceptor(
                HandleRedirectInterceptor(
                    mRedirectUri!!
                )
            )
        }


        httpBuilder.connectTimeout(
            10000,
            TimeUnit.MILLISECONDS
        ) // connect timeout
        httpBuilder.readTimeout(
            10000,
            TimeUnit.MILLISECONDS
        )    // read timeout
        // disable retry connection
        httpBuilder.retryOnConnectionFailure(false)



        val httpClient = httpBuilder.build()

        val okHttpRequestBuilder = Request.Builder()
        //url
        okHttpRequestBuilder.url(mRequestUrl)

        val okHttpRequest: Request = okHttpRequestBuilder
            .build()
        httpClient.newCall(okHttpRequest).enqueue(object : Callback {
            override fun onResponse(call: Call, response: Response) {
                // handle the response
                parseResponse(network, response)
            }

            override fun onFailure(call: Call, e: IOException) {
                Log.e(TAG, ("httpClient - onFailure: " + e.message ))
                e.printStackTrace()
                mCallback?.onFailure(e.message ?: "something went wrong")
            }
        })
    }

    @RequiresApi(VERSION_CODES.LOLLIPOP)
    private fun parseResponse(network: Network, response: Response) {
        Log.d(TAG, "parseResponse...")
        if(response.isRedirect){
            val isRedirect = handleRedirectResponse(network, response)
            if (isRedirect){
                return
            }
        }

        if(response.isSuccessful){
            when (mApiType) {
                APIType.COVERAGE -> handleCoverageResponse(response)
                APIType.AUTH -> handleAuthResponse(response)
                else -> handleAuthResponse(response)
            }

        }else if(response.code in(300..310)){
            handleAuthResponse(response)
        }
        else { // error
            handleErrorResponse(response)
        }
    }

    private fun handleAuthResponse(response: Response) {
        try{
            val responseBody = response.body!!.string()
            val data = Uri.parse(responseBody)
            if(data.isHierarchical){
                val code =  if (data.getQueryParameter("code") != null) data.getQueryParameter("code") else null
                if(!code.isNullOrEmpty()){
                    mCallback?.onSuccess(responseBody)
                    mCallback = null
                } else {
                    mCallback?.onFailure(responseBody)
                    mCallback = null
                }
            } else {
                mCallback?.onFailure(responseBody)
                mCallback = null
            }

        }catch (e: Exception){
            e.printStackTrace()
            mCallback?.onFailure(e.message ?: "something went wrong")
            mCallback = null
        }
    }

    private fun handleCoverageResponse(response: Response) {
        try{
            val responseBody = response.body!!.string()

            val json = JSONObject(responseBody.toString())
            if(json.has("available")) {
                val available = json.getBoolean("available")
                if(available){
                    mCallback?.onSuccess(responseBody)
                } else {
                    mCallback?.onFailure(responseBody)
                }
            }
        }catch (e: Exception){
            e.printStackTrace()
            mCallback?.onFailure("${e.message}")
        }
    }
    
    /**
    * Handles error response
    * @param response The HTTP response received.
    */
    private fun handleErrorResponse(response: Response) {
        try{
            val responseBody = response.body!!.string()
            mCallback?.onFailure(responseBody)
            mCallback = null

        }catch (e: Exception){
            e.printStackTrace()
            mCallback?.onFailure(e.message ?: "something went wrong")
            mCallback = null
        }
    }
    
    /**
     * Handles the redirect response by extracting the 'Location' header.
     *
     * @param response The HTTP response received.
     * @return An AuthResponse object containing the response code, location, and headers.
     */
    @RequiresApi(VERSION_CODES.LOLLIPOP)
    private fun handleRedirectResponse(network: Network, response: Response) : Boolean {
        Log.d(TAG, "handleRedirectResponse...")

        // Try to get the 'Location' header (case insensitive)
        val locationUrl = response.header("Location") ?: response.header("location")

        // Check if the location URL is valid and starts with "http" but not with the defined redirect URI
        if (mRedirectUri != null && locationUrl != null && locationUrl.startsWith("http") && !locationUrl.startsWith(mRedirectUri!!)) {
            mRequestUrl = locationUrl
            processRequestWithNetwork(network)
            return true
        }
        return false
    }

    /**
     * This function handles the case when the network is unavailable.
     */
    private fun handleUnAvailableCase() {
        // Add the implementation for handling the unavailable network case
        mCallback?.onFailure("onUnavailable: Failed to request network. Timeout error")
        mCallback = null
    }
    /**
     * Checks if the provided URL string is a valid URL.
     *
     * @param urlString The URL string to validate.
     * @return True if the URL is valid, false otherwise.
     */
    private fun isValidUrl(urlString: String): Boolean {
        return Patterns.WEB_URL.matcher(urlString).matches()
    }

    /**
     * Checks if the provided request URI is one of the specified IP endpoints.
     *
     * @param requestUri The request URI to check.
     * @return True if the URI is a known IP endpoint, false otherwise.
     */
    private fun isIPEndpoints(requestUri: String): Boolean {
        return requestUri.startsWith("https://stage.ipification.com") ||
                requestUri.startsWith("https://api.ipification.com")
    }

    internal fun isMobileDataEnabled(context: Context): Boolean {
        val cm =
            context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        try {
            val cmClass = Class.forName(cm.javaClass.name)
            val method: Method = cmClass.getDeclaredMethod("getMobileDataEnabled")
            method.isAccessible = true // Make the method callable
            // get the setting for "mobile data"
            val res = method.invoke(cm)
            if (res is Boolean) {
                return res
            }
        } catch (e: java.lang.Exception) {
            Log.e(TAG, e.message ?: "")
            return true
        }
        return true
    }
}

class NetworkDns private constructor() : Dns {
    private var mNetwork: Network? = null
    fun setNetwork(network: Network?) {
        mNetwork = network
    }

    @Throws(UnknownHostException::class)
    override fun lookup(hostname: String): List<InetAddress> {
        return if (mNetwork != null && Build.VERSION.SDK_INT >= VERSION_CODES.LOLLIPOP) {
            try {
                val inetAddressList: MutableList<InetAddress> = ArrayList()
                val inetAddresses = mNetwork!!.getAllByName(hostname)
                for (inetAddress in inetAddresses) {
                    if (inetAddress is Inet4Address) {
                        inetAddressList.add(0, inetAddress)
                    } else {
                        inetAddressList.add(inetAddress)
                    }
                }
                inetAddressList
            } catch (ex: NullPointerException) {
                try {
                    Dns.SYSTEM.lookup(hostname)
                } catch (e: UnknownHostException) {
                    listOf(*InetAddress.getAllByName(hostname))
                }
            } catch (ex: UnknownHostException) {
                try {
                    Dns.SYSTEM.lookup(hostname)
                } catch (e: UnknownHostException) {
                    listOf(*InetAddress.getAllByName(hostname))
                }
            }
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

class HandleRedirectInterceptor(redirectUri: String) : Interceptor {
    private val TAG: String = "RedirectInterceptor"
    private var mRedirectUri: String = redirectUri

    override fun intercept(chain: Interceptor.Chain): Response {

        val request: Request = chain.request()
        Log.d(TAG, "url: ${request.url}" )
        val t1 = System.nanoTime()
        Log.d(TAG,
            java.lang.String.format(
                "--> Sending request %s on %s%n%s",
                request.url,
                chain.connection(),
                request.headers
            )
        )
        val response: Response = chain.proceed(request)
        // Check if the response is a redirect (HTTP 3xx status code)
        if (response.code in 300.. 399){
            val locationHeader = response.header("location") ?: response.header("Location")

            // Check if the location header starts with the defined redirectUri
            if (locationHeader != null && locationHeader.startsWith(mRedirectUri)) {
                // Build a new successful response with HTTP 200 status code
                val builder: Response.Builder = Response.Builder().request(request).protocol(Protocol.HTTP_1_1)

                val contentType: MediaType? = response.body?.contentType()
                val body = locationHeader.toResponseBody(contentType)
                builder.code(200).message("success").body(body)

                // Close the original response body to avoid exceptions
                response.body?.close()

                return builder.build()
            }
        }
        return response
    }
}

private val cookieJar: CookieJar = object : CookieJar {
    private val cookieStore: MutableMap<String, MutableList<Cookie>> = mutableMapOf()

    override fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>) {
        val domain = url.host
        val sameDomainCookies = cookieStore.getOrPut(domain) { mutableListOf() }

        cookies.forEach { cookie ->
            val domainCookies = cookieStore.getOrPut(cookie.domain) { mutableListOf() }

            if (cookie.domain == domain) {  // same cookie domain
                val pos = sameDomainCookies.indexOfFirst { it.name == cookie.name }
                if (pos >= 0) {
                    sameDomainCookies[pos] = cookie
                } else {
                    sameDomainCookies.add(cookie)
                }
            } else { // save then check root domain
                val pos = domainCookies.indexOfFirst { it.name == cookie.name && it.domain == cookie.domain }
                if (pos >= 0) {
                    domainCookies[pos] = cookie
                } else {
                    domainCookies.add(cookie)
                }
            }
            cookieStore[cookie.domain] = domainCookies
        }
        cookieStore[domain] = sameDomainCookies
    }

    override fun loadForRequest(url: HttpUrl): List<Cookie> {
        val cookies = mutableListOf<Cookie>()
        val domain = url.topPrivateDomain() ?: url.host

        cookieStore[domain]?.let { domainCookies ->
            domainCookies.forEach { cookie ->
                if (url.encodedPath.startsWith(cookie.path)) {
                    cookies.add(cookie)
                }
            }
        }

        return cookies
    }
}
