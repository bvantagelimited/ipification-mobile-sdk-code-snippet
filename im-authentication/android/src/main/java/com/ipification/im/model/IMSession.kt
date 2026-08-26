package com.ipification.im.model

import com.ipification.im.utils.IPConstant
class IMSession(
    var viberLink: String? = null,
    var telegramLink: String? = null,
    var waLink: String? = null,
    var sessionId: String? = null,
    var completeSessionUrl: String? = null
) {
    /**
     * Converts the IMSession object to a list of IMInfo objects.
     * @return List<IMInfo> containing IMInfo objects representing the supported IM services.
     */
    fun convertToIMList(): List<IMInfo> {
        val result = ArrayList<IMInfo>()

        // Check and add WhatsApp info
        if (!waLink.isNullOrEmpty()) {
            val waInfo = IMInfo("wa", IPConstant.getInstance().whatsappPackageName, null, waLink!!, false)
            result.add(waInfo)
        }

        // Check and add Telegram info
        if (!telegramLink.isNullOrEmpty()) {
            val telegramInfo = IMInfo("telegram", IPConstant.getInstance().telegramPackageName, IPConstant.getInstance().telegramWebPackageName, telegramLink!!, false)
            result.add(telegramInfo)
        }

        // Check and add Viber info
        if (!viberLink.isNullOrEmpty()) {
            val viberInfo = IMInfo("viber", IPConstant.getInstance().viberPackageName, null, viberLink!!, false)
            result.add(viberInfo)
        }
        return result
    }
}
