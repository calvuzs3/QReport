package net.calvuz.qreport.checkup.spareparts.domain.model

data class SelectedArticle(
    val uuid: String,
    val name: String,
    val description: String,
    val codeOem: String,
    val codeErp: String,
    val codeBm: String,
    val unit: String
)
