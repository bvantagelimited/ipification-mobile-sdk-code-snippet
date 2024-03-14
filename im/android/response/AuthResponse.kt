package com.ipification.im.response

import android.net.Uri
import com.ipification.im.model.IMSession
import com.ipification.im.utils.IPConstant
import okhttp3.Headers

class AuthResponse(private val statusCode: Int, private val responseData: String, private val headers: Headers?) {

//    fun getCode(): String? {
//        try{
//            val data = Uri.parse(responseData)
//            if(data.isHierarchical){
//                return if (data.getQueryParameter("code") != null) data.getQueryParameter("code") else null
//            }
//            return null
//
//        }catch (e: Exception){
//            e.printStackTrace()
//            return null
//        }
//
//    }
    fun isIM() :Boolean{
        if(headers == null || headers[IPConstant.getInstance().IM_SESSION_ID] == null) {
            return false
        }
        return true
    }
    fun getIMInfo(): IMSession? {
        if(headers == null || headers[IPConstant.getInstance().IM_SESSION_ID] == null) {
            return null
        }
        try{
            val imSession = IMSession()
            imSession.sessionId =  headers[IPConstant.getInstance().IM_SESSION_ID]
            imSession.waLink =  headers[IPConstant.getInstance().IM_WA_LINK]

            imSession.telegramLink =  headers[IPConstant.getInstance().IM_TELEGRAM_LINK]
            imSession.viberLink =  headers[IPConstant.getInstance().IM_VIBER_LINK]
            if(imSession.waLink == null && imSession.telegramLink == null && imSession.viberLink == null){
                if(headers["location"] != null || headers["Location"] != null){
                    var oneLink = headers["location"]
                    if(oneLink == null){
                        oneLink = headers["Location"]
                    }
                    when {
                        oneLink?.contains("wa") == true -> {
                            imSession.waLink = oneLink
                        }
                        oneLink?.contains("viber") == true -> {
                            imSession.viberLink = oneLink
                        }
                        oneLink?.contains("telegram") == true -> {
                            imSession.telegramLink = oneLink
                        }

                    }
                }
            }
            imSession.completeSessionUrl =  headers[IPConstant.getInstance().IMBOX_ENDPOINT]
            return imSession
        }catch (e: Exception){
            e.printStackTrace()
            return null
        }

    }
    fun getState(): String? {
        try{
            val data = Uri.parse(responseData)
            if(data.isHierarchical) {
                return if (data.getQueryParameter("state") != null) data.getQueryParameter("state") else null
            }
            return null

        }catch (e: Exception){
            e.printStackTrace()
            return null
        }

    }
    fun getErrorMessage(): String{
        try{
            val data = Uri.parse(responseData)
            if(data.isHierarchical) {
                val error = data.getQueryParameter(
                    "error"
                )
                val errorDes = data.getQueryParameter(
                    "error_description"
                )
                if(error != null && errorDes != null){
                    return "$error $errorDes"
                }
                return errorDes ?: error ?: responseData
            }
            return ""

        }catch (e: Exception){
            return "exception: " + e.localizedMessage + " - "+ responseData
        }

    }
    internal fun getErrorDesc(): String{
        try{
            val data = Uri.parse(responseData)
            if(data.isHierarchical) {
                val errorDes = data.getQueryParameter(
                    "error_description"
                )
                return errorDes ?: ""
            }

            return ""
        }catch (e: Exception){
            return "exception: " + e.localizedMessage + " - "+ responseData
        }

    }
    internal fun getErrorCode(): String{
        try{
            val data = Uri.parse(responseData)
            if(data.isHierarchical) {
                val error = data.getQueryParameter(
                    "error"
                )
                return error ?: ""
            }
            return ""

        }catch (e: Exception){
            return "exception: " + e.localizedMessage + " - "+ responseData
        }
    }
}