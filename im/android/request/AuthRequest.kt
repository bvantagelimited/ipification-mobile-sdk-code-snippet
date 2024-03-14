package com.ipification.im.request

import android.net.Uri
import android.util.Log
import androidx.annotation.NonNull
import com.ipification.im.IPConfiguration

enum class API_TYPE {
    AUTH
}

class AuthRequest() {

    private var appEndpoint: Uri? = null
    internal var queryParameters: HashMap<String, String>? = null

    private var mClientId: String = ""

    private var mResponseType: String? = "code"

    internal var mRedirectUri: Uri? = null

//    internal var apiType: API_TYPE? =
//        API_TYPE.AUTH

    internal var mScope: String? = IPConfiguration.getInstance().DEFAULT_SCOPE

    internal var mState: String? = null

    internal var mChannel: String? = null

    constructor(
        endpoint: Uri?,
        mClientId: String,
        mRedirectUri: Uri?,
        mState: String?,
        mChannel: String?,
        queryParameters: HashMap<String, String>?
        ) : this() {
        this.appEndpoint = endpoint
        this.mClientId = mClientId
        this.mRedirectUri = mRedirectUri
        this.mState = mState
        this.mChannel = mChannel
        this.queryParameters = queryParameters

    }

    class Builder(private val endpoint: Uri? = null) {

        internal var queryParams: HashMap<String, String>? = null

        private var mClientId: String = ""
        private var mRedirectUri: Uri? = null
        internal var mState: String? = null
        internal var mChannel: String? = null

        fun build(): AuthRequest {
            return AuthRequest(
                endpoint,
                mClientId,
                mRedirectUri,
                mState,
                mChannel,
                queryParams,
            )
        }

        fun setRedirectUri(redirectUri: Uri): Builder {
            mRedirectUri = redirectUri
            return this
        }

        fun setState(state: String?): Builder {
            mState = state
            return this
        }

        fun addQueryParam(@NonNull key: String, @NonNull value: String) {
            if (queryParams == null) {
                queryParams = HashMap()
            }
            queryParams!![key] = value
        }

        fun setClientId(clientId: String): Builder {
            mClientId = clientId
            return this
        }

    }

    fun toUri(): Uri {
        val uriBuilder: Uri.Builder = appEndpoint?.buildUpon() ?: return Uri.EMPTY

        try {
            uriBuilder.appendQueryParameter("client_id", mClientId)
            uriBuilder.appendQueryParameter("redirect_uri", mRedirectUri?.toString())
            uriBuilder.appendQueryParameter("response_type", mResponseType)

            mScope?.let { scope ->
                uriBuilder.appendQueryParameter("scope", scope)
            }

            val state = if (mState.isNullOrEmpty()) {
                IPConfiguration.getInstance().generateState()
            } else {
                mState
            }
            uriBuilder.appendQueryParameter("state", state)
            IPConfiguration.getInstance().currentState = state!!

            queryParameters?.forEach { (key, value) ->
                uriBuilder.appendQueryParameter(key, value)
            }
        } catch (e: Exception) {
            // Log the exception or handle it accordingly
            Log.e("toUri", "Error while building URI: ${e.message}")
        }
        return uriBuilder.build()
    }
}

