
import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.util.Log
import java.lang.reflect.Method

/**
 * IPification - Utility class to check network-related states on an Android device.
 */
object NetworkUtils {

    /**
     * Checks if mobile data is enabled.
     * It defaults to `true` if the state cannot be determined.
     * @param context The context used to access system services.
     * @return `true` if mobile data is enabled or if the state cannot be determined, `false` otherwise.
     */
    internal fun isMobileDataEnabled(context: Context): Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        try {
            val cmClass = Class.forName(cm.javaClass.name)
            val method: Method = cmClass.getDeclaredMethod("getMobileDataEnabled")
            method.isAccessible = true // Make the method callable
            val res = method.invoke(cm)
            if (res is Boolean) {
                return res
            }
        } catch (e: Exception) {
            e.message?.let { Log.e("IPError", "Cannot check mobile data: $it") }
            return true
        }
        return true
    }

    /**
     * Checks if the device is actively connected to a Wi-Fi network.
     * @param context The context used to access system services.
     * @return `true` if the device is connected to a Wi-Fi network, `false` otherwise.
     */
    fun isWifiEnabled(context: Context): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val network = connectivityManager.activeNetwork
            val capabilities = connectivityManager.getNetworkCapabilities(network)
            capabilities?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true
        } else {
            @Suppress("DEPRECATION")
            val networkInfo = connectivityManager.activeNetworkInfo
            @Suppress("DEPRECATION")
            networkInfo?.type == ConnectivityManager.TYPE_WIFI && networkInfo.isConnected
        }
    }
}
