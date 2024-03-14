package com.ipification.im.model

class IMInfo(
    val brand: String,
    var packageName: String,
    val packageName2: String?,
    val message: String,
    var isInstalled: Boolean,

    ){
    fun getBrandName(): String {
        if(brand == "wa"){
            return "whatsapp"
        }
        return brand
    }
}