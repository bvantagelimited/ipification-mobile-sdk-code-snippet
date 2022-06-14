import android.telephony.SubscriptionManager
import android.telephony.TelephonyManager
import android.content.Context
import android.os.Build

class DeviceInfo {
    companion object Factory {
      
      private fun getActiveDataSimOperator(context: Context): SIMOperator {
        try {
            val tm = context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
            return if(!isDualSim()){
                SIMOperator(tm.simOperator,  tm.simCountryIso ?: "", tm.simOperatorName ?: "")
            } else {
                when {
                    // 24092021 support new function from API 30 (getActiveDataSubscriptionId)
                    Build.VERSION.SDK_INT >= Build.VERSION_CODES.R -> {
                        var dataSubId = SubscriptionManager.getActiveDataSubscriptionId()
                        var dataSimManager = tm.createForSubscriptionId(dataSubId)
                        if(dataSimManager == null || dataSimManager.simOperator.isNullOrEmpty()){
                            dataSubId = SubscriptionManager.getDefaultDataSubscriptionId()
                            dataSimManager = tm.createForSubscriptionId(dataSubId)
                            SIMOperator(dataSimManager?.simOperator ?: "",  dataSimManager?.simCountryIso ?: "", dataSimManager?.simOperatorName  ?: "", errorMessage = "CASE 2")
                        }else{
                            SIMOperator(dataSimManager.simOperator ?: "",  dataSimManager.simCountryIso ?: "", dataSimManager.simOperatorName ?: "", errorMessage = "CASE 1")
                        }

                    }
                    Build.VERSION.SDK_INT >= Build.VERSION_CODES.N -> {
                        val dataSubId = SubscriptionManager.getDefaultDataSubscriptionId()
                        val dataSimManager = tm.createForSubscriptionId(dataSubId)
                        SIMOperator(dataSimManager?.simOperator ?: "",  dataSimManager?.simCountryIso ?: "", dataSimManager?.simOperatorName ?: "")
                    }
                    else -> {
                        SIMOperator("", "", "", errorMessage = "unsupported_os_version")
                    }
                }
            }
        } catch (e: Exception){
            return SIMOperator("", "", "", errorMessage = "error_exception: ${e.message}")
        }
      }
      fun getSIM1(context: Context): SIMOperator {
        val tm = context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
        try {
            if(!isDualSim()){
                return SIMOperator(tm.simOperator,  tm.simCountryIso ?: "", tm.simOperatorName ?: "")
            }
            else return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val sm =
                    context.getSystemService(Context.TELEPHONY_SUBSCRIPTION_SERVICE) as SubscriptionManager
                val subIds1 = sm.getSubscriptionIds(0)
                if (subIds1 != null) {
                    val dataSimManager = subIds1.let { tm.createForSubscriptionId(it[0]) }
                    SIMOperator(dataSimManager?.simOperator ?: "",  dataSimManager?.simCountryIso ?: "", dataSimManager?.simOperatorName ?: "")
                } else {
                    SIMOperator("", "", "", errorMessage = "subIds1 is null")
                }

            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                val dataSubId = SubscriptionManager.getDefaultDataSubscriptionId()
                val dataSimManager = tm.createForSubscriptionId(dataSubId)
                SIMOperator(dataSimManager?.simOperator ?: "",  dataSimManager?.simCountryIso ?: "", dataSimManager?.simOperatorName ?: "")
            } else {
                SIMOperator("", "", "", errorMessage = "unsupported_os_version")
            }
        } catch (e: Exception){
            return SIMOperator("" , "", "", errorMessage = "error_exception: ${e.message)}")
        }
    }
     
    
    fun getSIM2(context: Context): SIMOperator {
        try {
            val tm = context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
            return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1) {
                val sm =
                    context.getSystemService(Context.TELEPHONY_SUBSCRIPTION_SERVICE) as SubscriptionManager
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        val subIds2 = sm.getSubscriptionIds(1)
                        if (subIds2 != null) {
                            val dataSimManager = subIds2.let { tm.createForSubscriptionId(it[0]) }
                            SIMOperator(dataSimManager?.simOperator ?: "",  dataSimManager?.simCountryIso ?: "", dataSimManager?.simOperatorName ?: "")
                        } else {
                            SIMOperator( "",  "", "", errorMessage = "subIds2 is null")
                        }

                    } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                        var dataSubId = SubscriptionManager.getDefaultDataSubscriptionId()
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R){
                            dataSubId = SubscriptionManager.getActiveDataSubscriptionId()
                        }
                        val smsSubId = SubscriptionManager.getDefaultSmsSubscriptionId()
                        val defaultId = SubscriptionManager.getDefaultSubscriptionId()

                        var secondId = -1
                        when {
                            dataSubId != smsSubId -> {
                                secondId = smsSubId
                            }
                            dataSubId != defaultId -> {
                                secondId = defaultId
                            }
                        }
                        if (secondId != -1) {
                            val dataSimManager = tm.createForSubscriptionId(secondId)
                            SIMOperator(dataSimManager?.simOperator ?: "",  dataSimManager?.simCountryIso ?: "", dataSimManager?.simOperatorName ?: "")
                        } else {
                            SIMOperator("", "", "", errorMessage = "secondId is null")
                        }

                    } else {
                        SIMOperator("", "", "", errorMessage = "unsupported_os_version")
                    }

            } else {
                SIMOperator("", "", "", errorMessage = "unsupported_os_version")
            }
        } catch (e: Exception){
            return SIMOperator("" , "", "",errorMessage = "error_exception: ${e.message}")
        }
        
    }
     // check if a device is Dual SIM 
    fun isDualSim(context: Context): Boolean {
      val tm = context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
      return when {
          Build.VERSION.SDK_INT >= Build.VERSION_CODES.R -> {
              tm.activeModemCount > 1
          }
          Build.VERSION.SDK_INT >= Build.VERSION_CODES.M -> {
              tm.phoneCount > 1
          }
          else -> {
              false
          }
      }
    }
    
    }
}



class SIMOperator(private val simOperator: String, private val simCountryIso: String, private val simOperatorName : String, private val errorMessage: String = ""){
    fun getMNC(): String {
        return if (simOperator.length >= 3) {
            simOperator.substring(3)
        } else {
            ""
        }
    }
    fun getMCC(): String {
        return if (simOperator.length >= 3) {
            simOperator.substring(0, 3)
        } else {
            ""
        }
    }
    fun getSimOperatorStr(): String {
        return simOperator
    }
    fun getOperatorName(): String {
        return simOperatorName
    }
    fun getCountryName(): String {
        val l = Locale("en-US", simCountryIso)
        return l.displayCountry
    }
    fun getErrorMessage(): String {
        return errorMessage
    }
}
