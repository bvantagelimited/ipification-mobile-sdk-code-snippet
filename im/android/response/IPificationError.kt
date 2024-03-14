package com.ipification.im.response

class IPificationError {
    constructor()
    constructor( exception: Exception) {
        this.exception = exception
    }
    fun getErrorMessage(): String {
        if(exception != null && exception!!.localizedMessage != null && error_description != null && error_description != ""){
            return "${exception!!.localizedMessage} - $error_description"
        }
        if(exception != null && exception!!.localizedMessage != null){
            return exception!!.localizedMessage!!
        }
        return error_description ?: error_code ?: "unknown_error $responseCode"
    }

    var responseCode : Int? = 0
    var error_code : String?  = null
    var error_description : String?  = null
    var exception: Exception? = null

    fun getErrorCode(): Int {
        return responseCode ?: -1
    }
}