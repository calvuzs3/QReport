package net.calvuz.qreport.domain.model.backup

import android.os.Build
import kotlinx.serialization.Serializable
import java.util.Locale

/**
 * DeviceInfo - Informazioni del dispositivo
 */
@Serializable
data class DeviceInfo(
    val model: String,
    val manufacturer: String,
    val osVersion: String,
    val appBuild: String,
    val locale: String
) {
    companion object {
        fun current(): DeviceInfo {
            return DeviceInfo(
                model = Build.MODEL,
                manufacturer = Build.MANUFACTURER,
                osVersion = Build.VERSION.RELEASE,
                appBuild = Build.VERSION.SDK_INT.toString(),
                locale = Locale.getDefault().toString()
            )
        }
    }
}