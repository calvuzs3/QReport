package net.calvuz.qreport.domain.model.checkup

import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable
import net.calvuz.qreport.domain.model.client.CriticalityLevel
import net.calvuz.qreport.domain.model.module.ModuleType
import net.calvuz.qreport.domain.model.photo.Photo

/**
 * Singolo item di controllo
 */
@Serializable
data class CheckItem(
    val id: String,
    val checkUpId: String,
    val moduleType: ModuleType,
    val itemCode: String,
    val description: String,
    val status: CheckItemStatus,
    val criticality: CriticalityLevel,
    val notes: String = "",
    val photos: List<Photo> = emptyList(),
    val checkedAt: Instant? = null,
    val orderIndex: Int
)