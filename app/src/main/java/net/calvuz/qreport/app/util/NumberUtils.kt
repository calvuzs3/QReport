package net.calvuz.qreport.app.util

import java.util.Locale

object NumberUtils {

    fun Float.toItalianPercentage(): String {
        val percentage = (this * 100)
        return "${String.format(Locale.ITALIAN,"%.1f", percentage)}%"
    }
    fun Double.toItalianPercentage(): String {
        return "${String.format(Locale.ITALIAN,"%.1f", this)}%"
    }
    fun Float.toItalianChange(): String {
        return "${String.format(Locale.ITALIAN, "%.2f", this)} €"
    }
}