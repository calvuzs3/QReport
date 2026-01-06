package net.calvuz.qreport.checkup.presentation.model

import android.content.Context
import androidx.compose.ui.graphics.Color
import net.calvuz.qreport.R
import net.calvuz.qreport.checkup.domain.model.CheckUpStatus
import net.calvuz.qreport.app.app.presentation.ui.theme.QReportBlue
import net.calvuz.qreport.app.app.presentation.ui.theme.QReportGreen
import net.calvuz.qreport.app.app.presentation.ui.theme.QReportGrey200
import net.calvuz.qreport.app.app.presentation.ui.theme.QReportGrey500
import net.calvuz.qreport.app.app.presentation.ui.theme.QReportYellowDark

object CheckUpStatusExt {

    fun CheckUpStatus.getDisplayName(context: Context): String {
        return when (this) {
            CheckUpStatus.DRAFT -> context.getString(R.string.enum_checkup_status_draft)
            CheckUpStatus.IN_PROGRESS -> context.getString(R.string.enum_checkup_status_in_progress)
            CheckUpStatus.COMPLETED -> context.getString(R.string.enum_checkup_status_completed)
            CheckUpStatus.EXPORTED -> context.getString(R.string.enum_checkup_status_exported)
            CheckUpStatus.ARCHIVED -> context.getString(R.string.enum_checkup_status_archived)
        }
    }

    fun CheckUpStatus.getColor(): Color {
        return when (this) {
            CheckUpStatus.DRAFT -> QReportGrey200
            CheckUpStatus.IN_PROGRESS -> QReportGrey500
            CheckUpStatus.COMPLETED -> QReportBlue
            CheckUpStatus.EXPORTED -> QReportGreen
            CheckUpStatus.ARCHIVED -> QReportYellowDark
        }
    }
}