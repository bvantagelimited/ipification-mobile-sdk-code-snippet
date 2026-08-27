package com.ipification.example.network

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.os.Build
import android.os.Handler
import android.os.Looper
import androidx.annotation.RequiresApi

/**
 * Requests a cellular network and makes it the default network for this app process.
 *
 * This is intentionally process-scoped. Prefer binding only the IPification HTTP client
 * to Network.socketFactory unless every future socket in the process must use cellular.
 * The owner must call [unbind] when cellular-only routing is no longer required.
 */
@RequiresApi(Build.VERSION_CODES.M)
class ProcessCellularBinding(context: Context) : AutoCloseable {
    private val connectivityManager =
        context.applicationContext.getSystemService(Context.CONNECTIVITY_SERVICE)
            as ConnectivityManager
    private val handler = Handler(Looper.getMainLooper())
    private val lock = Any()

    private var callback: ConnectivityManager.NetworkCallback? = null
    private var boundNetwork: Network? = null
    private var timeoutTask: Runnable? = null

    /**
     * Starts cellular routing for the process.
     *
     * [onBound] can run again if Android replaces a lost cellular network with another one.
     */
    fun bind(
        timeoutMillis: Long = 10_000L,
        onBound: (Network) -> Unit = {},
        onUnavailable: () -> Unit = {},
        onLost: () -> Unit = {},
    ) {
        require(timeoutMillis > 0) { "timeoutMillis must be greater than zero" }

        val networkCallback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                val didBind = synchronized(lock) {
                    if (callback !== this) return

                    cancelTimeoutLocked()
                    connectivityManager.bindProcessToNetwork(network).also { success ->
                        if (success) boundNetwork = network
                    }
                }

                if (didBind) {
                    onBound(network)
                } else {
                    release(this)
                    onUnavailable()
                }
            }

            override fun onLost(network: Network) {
                val lostCurrentBinding = synchronized(lock) {
                    if (callback !== this || boundNetwork != network) {
                        false
                    } else {
                        connectivityManager.bindProcessToNetwork(null)
                        boundNetwork = null
                        true
                    }
                }

                // Keep the request registered. Android may provide another cellular network.
                if (lostCurrentBinding) onLost()
            }

            override fun onUnavailable() {
                if (release(this)) onUnavailable()
            }
        }

        synchronized(lock) {
            check(callback == null) { "ProcessCellularBinding is already active" }
            callback = networkCallback

            timeoutTask = Runnable {
                if (release(networkCallback)) onUnavailable()
            }.also { handler.postDelayed(it, timeoutMillis) }
        }

        val request = NetworkRequest.Builder()
            .addTransportType(NetworkCapabilities.TRANSPORT_CELLULAR)
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()

        try {
            connectivityManager.requestNetwork(request, networkCallback)
        } catch (error: RuntimeException) {
            release(networkCallback)
            throw error
        }
    }

    /** Restores Android's normal default-network selection and releases the request. */
    fun unbind() {
        val activeCallback = synchronized(lock) { callback }
        if (activeCallback != null) release(activeCallback)
    }

    override fun close() = unbind()

    private fun release(expectedCallback: ConnectivityManager.NetworkCallback): Boolean {
        synchronized(lock) {
            if (callback !== expectedCallback) return false

            cancelTimeoutLocked()
            connectivityManager.bindProcessToNetwork(null)
            boundNetwork = null
            callback = null

            try {
                connectivityManager.unregisterNetworkCallback(expectedCallback)
            } catch (_: IllegalArgumentException) {
                // The framework may already have released a timed-out request.
            }
            return true
        }
    }

    private fun cancelTimeoutLocked() {
        timeoutTask?.let(handler::removeCallbacks)
        timeoutTask = null
    }
}
