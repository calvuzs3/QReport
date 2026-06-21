package net.calvuz.qreport.app.util

import androidx.compose.ui.graphics.Color

@Suppress("HardCodedStringLiteral")
object ColorUtils {

    /** Parses a "#RRGGBB"/"#AARRGGBB" hex string (e.g. master-data `colorHex` fields) into a Compose [Color]. */
    fun String.toComposeColor(fallback: Color = Color.Gray): Color = try {
        Color(android.graphics.Color.parseColor(this))
    } catch (e: IllegalArgumentException) {
        fallback
    }
}
