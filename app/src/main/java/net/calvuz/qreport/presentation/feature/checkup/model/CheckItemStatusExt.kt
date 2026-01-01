package net.calvuz.qreport.presentation.feature.checkup.model

import android.content.Context
import androidx.compose.ui.graphics.Color
import net.calvuz.qreport.R
import net.calvuz.qreport.domain.model.checkup.CheckItemStatus

object CheckItemStatusExt {

    fun CheckItemStatus.getDisplayName(context: Context): String {
        return when (this) {
            CheckItemStatus.PENDING -> context.getString(R.string.enum_checkup_item_status_pending)
            CheckItemStatus.OK -> context.getString(R.string.enum_checkup_item_status_ok)
            CheckItemStatus.NOK -> context.getString(R.string.enum_checkup_item_status_nok)
            CheckItemStatus.NA -> context.getString(R.string.enum_checkup_item_status_na)
        }
    }

    fun CheckItemStatus.getColor(): Color {
        return when (this) {
            CheckItemStatus.PENDING -> Color(0xFFFF9800)    // Orange 0xFFFF9800 "#FFA726"
            CheckItemStatus.OK -> Color(0xFF4CAF50)         // Green 0xFF4CAF50 "#66BB6A"
            CheckItemStatus.NOK -> Color(0xFFF44336)        // Red 0xFFF44336 "#EF5350"
            CheckItemStatus.NA -> Color(0xFF9E9E9E)         // Blue Grey 0xFF9E9E9E "#78909C"
        }
    }

    fun CheckItemStatus.getIcon(): String {
        return when (this) {
            CheckItemStatus.PENDING -> "⏳"
            CheckItemStatus.OK -> "✓"
            CheckItemStatus.NOK -> "✗"
            CheckItemStatus.NA -> "➖"
        }
    }

    fun CheckItemStatus.getReportColor(): String {
        return when (this) {
            CheckItemStatus.PENDING -> "FFC000"    // "FFC000" 0xFFFFC000
            CheckItemStatus.OK -> "00B050"         // "00B050" 0xFF00B050
            CheckItemStatus.NOK -> "FF0000"        // "FF0000" 0xFFFF0000
            CheckItemStatus.NA -> "000000"         // "000000" 0xFF000000
        }
    }

    fun CheckItemStatus.getNextStatus(): CheckItemStatus {
        return when (this) {
            CheckItemStatus.PENDING -> CheckItemStatus.OK
            CheckItemStatus.OK -> CheckItemStatus.NOK
            CheckItemStatus.NOK -> CheckItemStatus.NA
            CheckItemStatus.NA -> CheckItemStatus.PENDING
        }
    }
}