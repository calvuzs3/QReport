package net.calvuz.qreport.util

object NumberUtils {

    fun Float.toItalianPercentage(): String {
        val percentage = (this * 100).toInt()
        return "$percentage%"
    }
}