
import android.net.Network
import android.os.Build
import android.os.Build.VERSION_CODES

import java.net.Inet6Address
import java.net.InetAddress
import java.net.UnknownHostException

import java.util.ArrayList

import okhttp3.Dns

// Some telco networks intercept DNS and force resolutions through their own DNS servers.
class NetworkDns private constructor() : Dns {
    // The cellular Network to use for DNS resolution
    private var mNetwork: Network? = null

    /**
     * Assigns the network on which DNS lookups will be performed.
     */
    fun setNetwork(network: Network?) {
        mNetwork = network
    }

    /**
     * Performs a DNS lookup for the given hostname.
     * If a Network is set (and API ≥ 21), uses that Network’s DNS; otherwise falls back to the system DNS.
     * Prepends IPv6 addresses so they have priority over IPv4.
     */
    @Throws(UnknownHostException::class)
    override fun lookup(hostname: String): List<InetAddress> {
        // case: wifi + 4G
        if (mNetwork != null && Build.VERSION.SDK_INT >= VERSION_CODES.LOLLIPOP) {
            try {
                val inetAddressList: MutableList<InetAddress> = ArrayList()
                // Query the specified Network for all addresses for this hostname
                val inetAddresses = mNetwork!!.getAllByName(hostname)

                for (inetAddress in inetAddresses) {
                    // priority ipv6 first
                    if (inetAddress is Inet6Address) {
                        inetAddressList.add(0, inetAddress)
                    } else {
                        // IPv4 and any other address types get appended
                        inetAddressList.add(inetAddress)
                    }
                }
                return inetAddressList

            } catch (ex: NullPointerException) {
                // In rare cases Network.getAllByName might NPE—fall back to the system resolver
                return try {
                    Dns.SYSTEM.lookup(hostname)
                } catch (e: UnknownHostException) {
                    // As a last resort, use InetAddress
                    listOf(*InetAddress.getAllByName(hostname))
                }

            } catch (ex: UnknownHostException) {
                // If the network-based lookup fails, similarly fall back
                return try {
                    Dns.SYSTEM.lookup(hostname)
                } catch (e: UnknownHostException) {
                    listOf(*InetAddress.getAllByName(hostname))
                }
            }
        }

        // case mobile only
        return Dns.SYSTEM.lookup(hostname)
    }

    companion object {
        // Singleton instance
        private var sInstance: NetworkDns? = null

        /**
         * Provides the single shared instance of NetworkDns.
         */
        val instance: NetworkDns
            get() {
                if (sInstance == null) {
                    sInstance = NetworkDns()
                }
                return sInstance!!
            }
    }
}
