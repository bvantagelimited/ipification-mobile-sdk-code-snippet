package com.ipification.im.utils

import android.content.pm.PackageManager
import com.ipification.im.IPConfiguration
import com.ipification.im.model.IMInfo

class VerificationExtensionKt {
    companion object Factory {

        private fun filterValidProviders(
            supportedProviders: List<IMInfo>,
            packageManager: PackageManager
        ): List<IMInfo> {
            val hashMap = ArrayList<IMInfo>()
            for (imService in supportedProviders) {
                if (packageManager.isPackageInstalled(imService.packageName) || (imService.packageName2 != null && packageManager.isPackageInstalled(
                        imService.packageName2
                    ))
                ) {
                    hashMap.add(imService)
                }
            }
            return hashMap
        }

        fun checkInstalledApp(
            supportedProviders: List<IMInfo>,
            packageManager: PackageManager
        ): List<IMInfo> {
            val hashMap = ArrayList<IMInfo>()
            for (imService in supportedProviders) {
                if (packageManager.isPackageInstalled(imService.packageName)) {
                    imService.isInstalled = true
                }
                if ((imService.packageName2 != null && packageManager.isPackageInstalled(imService.packageName2))) {
                    imService.packageName = imService.packageName2
                    imService.isInstalled = true
                }
                hashMap.add(imService)
            }
            return hashMap
        }

        fun validateInstallApp(
            supportedProviders: List<IMInfo>,
            packageManager: PackageManager
        ): Boolean {
            return filterValidProviders(supportedProviders, packageManager).isNotEmpty()
        }

        fun findFirstInstalledApp(
            supportedProviders: List<IMInfo>,
            packageManager: PackageManager
        ): IMInfo? {
            val validProviders = filterValidProviders(supportedProviders, packageManager)
            if (validProviders.isNotEmpty()) {
                val priorityList = IPConfiguration.getInstance().IM_PRIORITY_APP_LIST
                for (priorityItem in priorityList) {
                    for (availableItem in validProviders) {
                        if (priorityItem == availableItem.brand) {
                            return availableItem
                        }
                    }
                }
            }
            return null
        }
    }

}

fun PackageManager.isPackageInstalled(packageName: String): Boolean {
    return try {
        getPackageInfo(packageName, PackageManager.GET_ACTIVITIES)
        true
    } catch (e: PackageManager.NameNotFoundException) {
        false
    }
}
