package com.ipification.im.utils

import androidx.annotation.Keep

object ErrorCode {
    const val INVALID_CONFIGURATION = 1003
    const val NETWORK_ERROR = 1000
    const val NETWORK_TIMEOUT_ERROR = 1001

    const val EMPTY_PHONE_NUMBER = 1002
    const val EMPTY_CLIENT_ID = 1003
    const val EMPTY_REDIRECT_URI = 1004
    const val EMPTY_SCOPE = 1005
    const val EMPTY_LOGIN_HINT = 1008
    const val UNSUPPORT_VERSION = 1006
    const val EMPTY_COVERAGE_ENDPOINT = 1007
    const val EMPTY_AUTH_ENDPOINT = 1008


    const val EMPTY_IM_HEADER = 2001
    const val IM_FAILED = 2002
    const val IM_NO_NETWORK_ERROR = 2003
    const val IM_ERROR_UNSUPPORTED = 2004
    const val EMPTY_IM_PRIORITY_APP_LIST = 2005
    const val GENERAL_ERROR = 800
    const val CLASS_CAST_ERROR = 801
    const val NETWORK_RESPONSE_FAILED = 803
    const val SERVER_RESPONSE_FAILED = 804

}
object ErrorMessages {
    const val INVALID_CONFIGURATION = "Invalid configuration. Please check your Client ID and REDIRECT_URI"
    const val NETWORK_ERROR = "Your cellular network is not active or not available"
    const val EMPTY_CLIENT_ID = "The client_id parameter is empty. Please review your initialization function to ensure that the client_id is properly set."
    const val EMPTY_REDIRECT_URI = "The redirect_uri parameter is empty. Please review your initialization function to ensure that the client_id is properly set."
    const val EMPTY_COVERAGE_ENDPOINT = "The Coverage endpoint is null. Please review your initialization function."
    const val EMPTY_AUTH_ENDPOINT = "The Auth endpoint is null. Please review your initialization function."
    const val EMPTY_PHONE_NUMBER = "The phoneNumber parameter cannot be empty"
    const val EMPTY_SCOPE = "The scope parameter cannot be empty"
    const val EMPTY_LOGIN_HINT = "The login_hint parameter cannot be empty when the scope is set to 'ip:phone_verify'"
    const val UNSUPPORT_VERSION = "Our SDK does not support Android SDK versions under 21 (Android 5.0)."
    const val EMPTY_IM_HEADER = "The IM header is null. Please ensure that the necessary information is properly set before proceeding."
    const val IM_FAILED = "IM response is failed"
    const val IM_NO_NETWORK_ERROR = "Your internet network is not active or not available"
    const val IM_ERROR_UNSUPPORTED = "UNSUPPORTED"
    const val EMPTY_IM_PRIORITY_APP_LIST = "IM_PRIORITY_APP_LIST cannot be empty. Please review your initialization function to ensure that the IM_PRIORITY_APP_LIST is properly set."
    const val NETWORK_TIMEOUT_ERROR = "Failed to request network. Timeout error"
    const val GENERAL_ERROR = "Something went wrong"
    const val EMPTY_RESPONSE_ERROR = "Something went wrong. Empty Response"
}
class IPConstant() {

    var whatsappPackageName: String = "com.whatsapp"
    var telegramPackageName: String = "org.telegram.messenger"
    var telegramWebPackageName: String = "org.telegram.messenger.web"
    var viberPackageName: String = "com.viber.voip"

    val IM_SESSION_ID = "imbox_session_id"
    val IM_WA_LINK = "wa_link"
    val IM_TELEGRAM_LINK = "telegram_link"
    val IM_VIBER_LINK = "viber_link"
    val IMBOX_ENDPOINT = "imbox_endpoint"

    private object Holder {
        val INSTANCE = IPConstant()
    }

    @Keep
    companion object {
        @JvmStatic
        fun getInstance(): IPConstant {
            return Holder.INSTANCE
        }
    }
}