package net.calvuz.qreport.app.util

object SizeUtils {

    fun Double.getFormattedSize(): String {
        val value: Double = this
        return when {
            value < 1 -> "${(value * 1024).toInt()}KB"
            value < 1024 -> "${value.toInt()}MB"
            else -> "${(value / 1024).toInt()}GB"
        }
    }

    fun Long.getFormattedSize(): String {
        val value: Long = this
        return when {
            value < 1024 -> "${value}B"
            value < (1024 * 1024) -> "${(value / 1024)}KB"
            value < (1024 * 1024 * 1024) -> "${(value / 1024 * 1024)}MB"
            else -> "${(value / 1024 * 1024 * 1024)}GB"
        }
    }

    fun Long.getFormattedCycleCount(): String {
        val value: Long = this
        return when {
            value >= 1_000_000 -> "${(value / 1_000_000).toInt() }M"
            value >= 1_000 -> "${(value / 1_000).toInt()}K"
            else -> value.toString()
        }
    }

    fun Long.getFormattedHours(): String {
        val value: Long = this
        return when {
            value >= 1000 -> "${value / 1000}K ore"
            else -> "$value ore"
        }
    }
    fun Int.getFormattedHours(): String {
        val value: Int = this
        return when {
            value >= 1000 -> "${value / 1000}K ore"
            else -> "$value ore"
        }
    }
}