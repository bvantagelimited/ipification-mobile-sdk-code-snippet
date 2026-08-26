package com.ipification.im.connection
import android.os.Build
import android.util.Log
import com.ipification.im.IPConfiguration
import com.ipification.im.utils.IPHeaders
import okhttp3.Interceptor
import okhttp3.MediaType
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody

class HandleRedirectInterceptor(
    private val requestUrl: String,
    private val redirectUri: String
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val request: Request = chain.request()
        onLog("-------")
        onLog("url: ${request.url}")
        val t1 = System.nanoTime()
        onLog(
            String.format(
                "--> Sending request %s on %s%n%s",
                request.url,
                chain.connection(),
                request.headers
            )
        )

        val response: Response = if (request.url.toString().startsWith(requestUrl)) {
            val newRequestBuilder = request.newBuilder()
            newRequestBuilder.addHeader(IPHeaders.IP_SDK_VERSION, IPConfiguration.getInstance().VERSION)
            newRequestBuilder.addHeader(IPHeaders.DEVICE_TYPE, "android")
            newRequestBuilder.addHeader(IPHeaders.DEVICE_NAME, "${Build.MANUFACTURER} - ${Build.MODEL}")
            newRequestBuilder.addHeader(IPHeaders.OS_VERSION, Build.VERSION.RELEASE)
            newRequestBuilder.addHeader(IPHeaders.OS_SDK, "${Build.VERSION.SDK_INT}")
            val newRequest = newRequestBuilder.build()
            chain.proceed(newRequest)
        } else {
            chain.proceed(request)
        }

        val t2 = System.nanoTime()
        onLog(
            String.format(
                "<-- Received response for %s in %.1fms%n%s",
                response.request.url,
                (t2 - t1) / 1e6,
                response.headers
            )
        )
        onLog("response - status code: ${response.code}")
        onLog("-------")
        Log.d("Interceptor", response.request.url.toString())
        Log.d("Interceptor", "location ${response.headers["location"]}")
        IPConfiguration.getInstance().currentUrl = response.headers["location"] ?: ""

        val locationHeader = response.headers["location"] ?: response.headers["Location"]
        val imboxSessionIdHeader = response.headers["imbox_session_id"]
        if (response.code in 300..399 && isRedirectToValidUri(locationHeader) || imboxSessionIdHeader != null) {
            onLog("matched - process")
            val builder: Response.Builder = Response.Builder().request(request).protocol(Protocol.HTTP_1_1)
            val contentType: MediaType? = response.body?.contentType()
                ?: "text/plain".toMediaTypeOrNull()
            val customBody = (locationHeader ?: "").toResponseBody(contentType)
            response.headers.forEach { (name, value) ->
                if (name.isNotEmpty() && value.isNotEmpty()) {
                    builder.addHeader(name, value)
                }
            }
            builder.code(200).message("success").body(customBody)
            response.body?.close()
            return builder.build()
        }
        onLog("return response")
        return response
    }

    private fun isRedirectToValidUri(locationHeader: String?): Boolean {
        return locationHeader?.startsWith(redirectUri, ignoreCase = true) ?: false
    }

    private fun onLog(log: String) {
        Log.d("RedirectIC", log)
    }
}
