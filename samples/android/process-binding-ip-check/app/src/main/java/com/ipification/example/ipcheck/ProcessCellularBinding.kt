package com.ipification.example.ipcheck

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.os.Build
import android.os.Handler
import android.os.Looper

/**
 * Requests a cellular network and makes it the default network for future sockets and DNS
 * lookups created by this application process.
 *
 * The owner must call [unbind] after the final required cellular request finishes.
 */
class ProcessCellularBinding(context: Context) : AutoCloseable {
    private val connectivityManager =
        context.applicationContext.getSystemService(Context.CONNECTIVITY_SERVICE)
            as ConnectivityManager
    private val mainHandler = Handler(Looper.getMainLooper())

    private var callback: ConnectivityManager.NetworkCallback? = null
    private var timeoutTask: Runnable? = null

    fun bind(
        timeoutMillis: Long = 15_000L,
        onBound: (Network) -> Unit,
        onFailure: (String) -> Unit,
        onLost: () -> Unit,
    ) {
        require(timeoutMillis > 0) { "timeoutMillis must be greater than zero" }
        check(callback == null) { "ProcessCellularBinding is already active" }

        val networkCallback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                mainHandler.post {
                    if (callback !== this) return@post
                    cancelTimeout()

                    // Core forcing call: future connections in this app process use this network.
                    if (connectivityManager.bindProcessToNetwork(network)) {
                        onBound(network)
                    } else {
                        release(this)
                        onFailure("Android could not bind this process to cellular")
                    }
                }
            }

            override fun onUnavailable() {
                mainHandler.post {
                    if (release(this)) onFailure("A cellular network is unavailable")
                }
            }

            override fun onLost(network: Network) {
                mainHandler.post {
                    if (callback !== this) return@post
                    connectivityManager.bindProcessToNetwork(null)
                    onLost()
                }
            }
        }

        callback = networkCallback
        timeoutTask = Runnable {
            if (release(networkCallback)) {
                onFailure("Timed out waiting for a cellular network")
            }
        }.also { mainHandler.postDelayed(it, timeoutMillis) }

        val request = NetworkRequest.Builder()
            .addTransportType(NetworkCapabilities.TRANSPORT_CELLULAR)
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                // API 26+: Android can deliver callbacks directly through our main handler.
                connectivityManager.requestNetwork(request, networkCallback, mainHandler)
            } else {
                // API 23-25: use the older overload. Each callback above forwards its work to
                // mainHandler so callback handling, timeout, and cleanup remain serialized.
                @Suppress("DEPRECATION")
                connectivityManager.requestNetwork(request, networkCallback)
            }
        } catch (error: RuntimeException) {
            release(networkCallback)
            throw error
        }
    }

    /** Restores normal routing and unregisters the active cellular request. */
    fun unbind() {
        callback?.let(::release)
    }

    override fun close() = unbind()

    private fun release(expectedCallback: ConnectivityManager.NetworkCallback): Boolean {
        if (callback !== expectedCallback) return false

        cancelTimeout()
        connectivityManager.bindProcessToNetwork(null)
        callback = null

        try {
            connectivityManager.unregisterNetworkCallback(expectedCallback)
        } catch (_: IllegalArgumentException) {
            // Android may already have released an unavailable or timed-out callback.
        }
        return true
    }

    private fun cancelTimeout() {
        timeoutTask?.let(mainHandler::removeCallbacks)
        timeoutTask = null
    }
}
