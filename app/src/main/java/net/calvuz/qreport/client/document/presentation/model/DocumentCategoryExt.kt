package net.calvuz.qreport.client.document.presentation.model

import net.calvuz.qreport.R
import net.calvuz.qreport.app.error.presentation.UiText
import net.calvuz.qreport.client.document.domain.model.DocumentCategory

fun DocumentCategory.displayLabel(): UiText = when (this) {
    DocumentCategory.ELECTRICAL -> UiText.StringResource(R.string.document_category_electrical)
    DocumentCategory.MECHANICAL -> UiText.StringResource(R.string.document_category_mechanical)
    DocumentCategory.FLUID      -> UiText.StringResource(R.string.document_category_fluid)
    DocumentCategory.MANUAL     -> UiText.StringResource(R.string.document_category_manual)
    DocumentCategory.CONTRACT   -> UiText.StringResource(R.string.document_category_contract)
    DocumentCategory.OTHER      -> UiText.StringResource(R.string.document_category_other)
}