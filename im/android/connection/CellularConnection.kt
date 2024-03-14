package com.ipification.im.connection

import android.util.Log
import com.ipification.im.IPConfiguration
import com.ipification.im.callback.IPificationCallback
import com.ipification.im.response.IPificationError
import com.ipification.im.request.AuthRequest
import com.ipification.im.response.AuthResponse
import com.ipification.im.utils.ErrorCode
import com.ipification.im.utils.ErrorMessages
import okhttp3.Call
import okhttp3.Callback
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import java.io.IOException
import java.util.concurrent.TimeUnit

class CellularConnection(
    private val authRequest: AuthRequest,
    private val callback: IPificationCallback,
) {
    fun makeConnection(requestUri: String) {
        logInfo("Request URL: $requestUri")
        IPConfiguration.getInstance().currentUrl = requestUri

        val httpClient = OkHttpClient.Builder()
            .addNetworkInterceptor(HandleRedirectInterceptor(requestUri, authRequest.mRedirectUri.toString()))
            .connectTimeout(IPConfiguration.getInstance().AUTH_CONNECT_TIMEOUT, TimeUnit.MILLISECONDS)
            .readTimeout(IPConfiguration.getInstance().AUTH_READ_TIMEOUT, TimeUnit.MILLISECONDS)
            .build()

        val request = Request.Builder().url(requestUri).build()

        httpClient.newCall(request).enqueue(object : Callback {
            override fun onResponse(call: Call, response: Response) {
                try {
                    parseResponse(response)
                } catch (e: Exception) {
                    handleResponseError(e)
                }
            }

            override fun onFailure(call: Call, e: IOException) {
                logInfo("onFailure: ${e.message}")
                val ex = IPificationError()
                ex.error_description = IPConfiguration.getInstance().currentUrl
                ex.exception = e
                ex.responseCode = ErrorCode.SERVER_RESPONSE_FAILED
                callbackFailed(ex)
            }
        })
    }

    private fun parseResponse(response: Response) {
        val responseBody = response.body?.string() ?: throw IOException(ErrorMessages.EMPTY_RESPONSE_ERROR)
        logInfo("Response Body: $responseBody")

        when {
            response.isSuccessful -> {
                val convertedResponse = AuthResponse(response.code, responseBody, response.headers)
                if (convertedResponse.isIM()) {
                    callbackSuccess(convertedResponse)
                } else {
                    val ex = IPificationError()
                    ex.error_description = IPConfiguration.getInstance().currentUrl
                    ex.exception = java.lang.Exception(responseBody)
                    ex.responseCode = ErrorCode.INVALID_CONFIGURATION
                    callbackFailed(ex)
                }
            }
            response.isRedirect -> handleRedirectResponse(response)
            else -> {
                val ex = IPificationError()
                ex.responseCode = ErrorCode.NETWORK_RESPONSE_FAILED
                ex.exception = Exception(responseBody)
                callbackFailed(ex)
            }
        }
    }

    private fun handleRedirectResponse(response: Response) {
        val location = response.headers["Location"] ?: response.headers["location"]
        val ex = IPificationError()
        ex.error_description = IPConfiguration.getInstance().currentUrl
        ex.exception = java.lang.Exception(location ?: "Error: Empty Location Header")
        ex.responseCode = response.code
        callbackFailed(ex)
    }

    private fun handleResponseError(e: Exception) {
        val ex =
            IPificationError()
        ex.responseCode = ErrorCode.CLASS_CAST_ERROR
        ex.error_description = IPConfiguration.getInstance().currentUrl
        ex.exception =
            java.lang.Exception("invalid callback type. (${e.localizedMessage})")
        callbackFailed(ex)
    }

    private fun callbackSuccess(cellularResponse: AuthResponse) {
        callback.onSuccess(cellularResponse)
    }

    private fun callbackFailed(ex: IPificationError) {
        logInfo("Callback Failed: ${ex.responseCode} ${ex.getErrorMessage()}")
        callback.onError(ex)
    }

    private fun logInfo(log: String) {
        Log.d("CellularConnection", "[CellularConnection]: $log")
    }
}

