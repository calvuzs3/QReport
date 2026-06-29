package net.calvuz.qreport.checkup.spareparts.domain.model

data class CheckUpSparePart(
    val id: String,
    val checkupId: String,
    val articleUuid: String,
    val name: String,
    val codeOem: String = "",
    val codeErp: String = "",
    val codeBm: String = "",
    val unit: String = "pz",
    val quantity: Double? = null,
    val notes: String = "",
    val addedAt: Long
) {
    val displayCode: String
        get() = codeOem.ifEmpty { codeErp.ifEmpty { codeBm } }
}
