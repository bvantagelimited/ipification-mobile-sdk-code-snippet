package com.ipification.im

import android.app.*
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.os.Build
import android.util.Log
import com.ipification.im.model.IMSession
import com.ipification.im.utils.ErrorMessages
import com.ipification.im.callback.RedirectDataCallback
import com.ipification.im.callback.VerifyCompleteListener
import com.ipification.im.utils.VerificationExtensionKt
import okhttp3.Call
import okhttp3.Callback
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import java.io.IOException

class IMHelper {
    companion object Factory {
        private val TAG = "IMHelper"
        fun startVerification(
            activity: Activity,
            imSessionInfo: IMSession,
            verifyListener: VerifyCompleteListener
        ) {
            Log.d("IMHelper", "startVerification")
            // Check if validating IM apps is enabled
            val isExist = VerificationExtensionKt.validateInstallApp(
                imSessionInfo.convertToIMList(),
                activity.packageManager
            )
            if (!isExist) {
                verifyListener.onFail("Error: No supported messaging apps are available on your phone.")
                return
            }

            // Automatic mode
            if (IPConfiguration.getInstance().IM_AUTO_MODE) {
                val app = VerificationExtensionKt.findFirstInstalledApp(
                    imSessionInfo.convertToIMList(),
                    activity.packageManager
                )
                app?.let {
                    callRedirectUrl(app.message, object : RedirectDataCallback {
                        override fun onResponse(link: String) {
                            openLink(activity, link, verifyListener)
                        }
                    })

                } ?: run {
                    // This should not happen, but handle it just in case
                    verifyListener.onFail("Error: No supported messaging apps are available on your phone.")
                }
                return
            }
        }



        private fun callRedirectUrl(url: String, callback: RedirectDataCallback) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                val request = Request.Builder().url(url).build()
                val clientBuilder = OkHttpClient.Builder()
                clientBuilder.followRedirects(false)
                clientBuilder.followSslRedirects(false)
                val client = clientBuilder.build()
                client.newCall(request).enqueue(object : Callback {
                    override fun onResponse(call: Call, response: Response) {
                        if (response.isSuccessful || response.isRedirect) {
                            val body = response.body?.string()
                            var location = response.header("Location")
                            if (location == null) {
                                location = response.header("location")
                            }
                            if (location != null || body != null) {
                                callback.onResponse((location ?: body).toString())
                            } else {
                                callback.onResponse("")
                            }
                        } else {
                            val body = response.body?.string()
                            Log.d("response", "error body $body")
                            callback.onResponse(body ?: "")
                        }

                    }

                    override fun onFailure(call: Call, e: IOException) {
                        callback.onResponse("")

                    }
                })
            } else {
                callback.onResponse("unsupported_os")
            }
        }

        fun openLink(
            activity: Activity, url: String, verifyListener: VerifyCompleteListener
        ) {
            try {
                val i = Intent(Intent.ACTION_VIEW)
                i.data = Uri.parse(url)
                Log.d(TAG, "openLink $url")
                activity.startActivity(i)
                verifyListener.onOpenedLink()
            } catch (e: java.lang.Exception) {
                verifyListener.onFail(e.message ?: ("openLink: " + ErrorMessages.GENERAL_ERROR))
            }
        }
    }
}