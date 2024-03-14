package com.ipification.im

import android.accounts.NetworkErrorException
import android.app.Activity
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.ipification.im.callback.IPificationCallback
import com.ipification.im.callback.VerifyCompleteListener
import com.ipification.im.connection.CellularConnection
import com.ipification.im.response.IPificationError
import com.ipification.im.request.AuthRequest
import com.ipification.im.response.AuthResponse
import com.ipification.im.utils.ErrorCode
import com.ipification.im.utils.ErrorMessages
import com.ipification.im.utils.LogUtils
import com.ipification.im.utils.NetworkUtils
import com.ipification.im.utils.error

class IMServices {
    companion object Factory {
        private var linkwasOpen: Boolean = false
        var TAG = "IMServices"
        var mCallback: VerifyCompleteListener? = null
        private var authRequesting = false

        fun checkAndFinishSession(){
            if(mCallback != null){
                val state = IPConfiguration.getInstance().currentState
                val handler = Handler(Looper.getMainLooper())
                handler.postDelayed({
                    mCallback!!.onSuccess(state)
                    mCallback = null
                }, 1000) //delay 1s

            }else{
                Log.d(TAG, "callback is null")
            }
        }
        /**
        * Perform IPification Authorization
        * @param activity: Activity
        * @param callback: IPificationCallback
        */

        fun startAuthentication(
            activity: Activity,
            callback: VerifyCompleteListener
        ) {
            linkwasOpen = false
            // Set authRequesting flag to true to indicate an authorization request is in progress
            authRequesting = true
            // Set the callback for handling the authorization response
            mCallback = callback

            // Define a callback to handle the authorization response
            val cb = object : IPificationCallback {
                override fun onSuccess(response: AuthResponse) {
                    //TODO: we use AUTOMODE, so flow should not fall to this callback.
                    authRequesting = false
                    mCallback?.onSuccess(response.getState() ?: IPConfiguration.getInstance().currentState)
                    mCallback = null
                }

                override fun onError(error: IPificationError) {
                    authRequesting = false
                    mCallback?.onFail(error.getErrorMessage())
                    mCallback = null
                }
            }
            performIMAuth(activity, cb)
        }

        private fun performIMAuth(activity: Activity, callback: IPificationCallback) {
            val config = IPConfiguration.getInstance()

            // Check configuration settings
            if (!config.customUrls) {
                config.AUTHORIZATION_URL = Uri.parse(config.getAuthorizationUrl())
            }
            if (config.AUTHORIZATION_URL == null || config.CLIENT_ID.isEmpty() ||
                config.REDIRECT_URI == null || config.REDIRECT_URI.toString().isEmpty()
            ) {
                handleException(NullPointerException(ErrorMessages.INVALID_CONFIGURATION), ErrorCode.INVALID_CONFIGURATION, callback)
                return
            }

            // Build AuthRequest
            val requestBuilder = AuthRequest.Builder(config.AUTHORIZATION_URL)
            requestBuilder.setClientId(config.CLIENT_ID)
            requestBuilder.setRedirectUri(config.REDIRECT_URI!!)
            requestBuilder.setState(config.currentState)
            val request = requestBuilder.build()

            // Update request parameters based on IM configuration
            if (config.IM_AUTO_MODE) {
                if (config.IM_PRIORITY_APP_LIST.isEmpty()) {
                    handleException(NullPointerException(ErrorMessages.EMPTY_IM_PRIORITY_APP_LIST), ErrorCode.EMPTY_IM_PRIORITY_APP_LIST, callback)
                    return
                }
                request.queryParameters = (request.queryParameters ?: HashMap()).apply {
                    set("channel", config.IM_PRIORITY_APP_LIST.joinToString(separator = " "))
                }
            }
            // Check for IM flow support
            if (!NetworkUtils.isInternetAvailable(activity)) {
                handleUnavailableCase(callback)
                return
            }
            connect(activity, request, callback)
        }
        private fun connect(activity : Activity, cellularRequest: AuthRequest, callback: IPificationCallback) {
            val cb = object : IPificationCallback{
                override fun onSuccess(response: AuthResponse) {
                    if (response.isIM() && response.getIMInfo() != null) {
                        handleIMResponse(activity, response, callback)
                    } else {
                        val cellularException = IPificationError().apply {
                            error_description = ErrorMessages.EMPTY_IM_HEADER
                            responseCode = ErrorCode.EMPTY_IM_HEADER
                            exception = NullPointerException()
                        }
                        callback.onError(cellularException)
                    }
                }
                override fun onError(error: IPificationError) {
                    callback.onError(error)
                }
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                val connection = CellularConnection(cellularRequest, cb)
                connection.makeConnection(cellularRequest.toUri().toString())
            }else{
                val unsupportedException = IPificationError().apply {
                    error_description = ErrorMessages.UNSUPPORT_VERSION
                    responseCode = ErrorCode.UNSUPPORT_VERSION
                    exception = UnsupportedOperationException()
                }
                callback.onError(unsupportedException)
            }
        }


        private fun handleIMResponse(activity : Activity, response: AuthResponse, callback: IPificationCallback) {
            // open link
            IMHelper.startVerification(activity, response.getIMInfo()!!, object :
                VerifyCompleteListener {
                override fun onSuccess(state: String) {
                    // nothing to do
                    val cellularException = IPificationError().apply {
                        error_description = "wrong flow"
                        responseCode = ErrorCode.IM_FAILED
                        exception = NullPointerException()
                    }
                    callback.onError(cellularException)
                }

                override fun onFail(errorMessage: String) {
                    linkwasOpen = false
                    val cellularException = IPificationError().apply {
                        error_description = errorMessage
                        responseCode = ErrorCode.IM_FAILED
                        exception = NullPointerException()
                    }
                    callback.onError(cellularException)
                }
                override fun onOpenedLink() {
                    linkwasOpen = true
                    val handler = Handler(Looper.getMainLooper())
                    handler.postDelayed({
                        mCallback?.onOpenedLink()
                    }, 1000) //delay 1s

                }
            })
        }


        private fun handleException(e: Exception, code: Int, callback: IPificationCallback) {
            val exception = IPificationError().apply {
                responseCode = code
                exception = e
            }
            callback.onError(exception)
        }

        private fun handleUnavailableCase(callback: IPificationCallback) {
            LogUtils.error("onUnavailable: Your cellular network is not active or not available")
            val cellularException = IPificationError().apply {
                responseCode = ErrorCode.NETWORK_ERROR
                exception = NetworkErrorException(ErrorMessages.NETWORK_ERROR)
            }
            callback.onError(cellularException)
        }
    }
}
