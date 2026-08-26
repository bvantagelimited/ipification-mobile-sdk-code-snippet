package com.ipification.im.callback
import com.ipification.im.response.IPificationError
import com.ipification.im.response.AuthResponse

interface IPificationCallback {
    fun onSuccess(response: AuthResponse)
    fun onError(error: IPificationError)

}