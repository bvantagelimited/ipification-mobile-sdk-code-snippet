package com.ipification.im.callback


interface VerifyCompleteListener {
    fun onSuccess(state: String)
    fun onFail(errorMessage: String)
    fun onOpenedLink()
}