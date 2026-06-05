package net.calvuz.qreport.app.app.presentation.model

import net.calvuz.qreport.app.error.presentation.UiText

interface QReportFilter {
    fun getDisplayName(): UiText
}