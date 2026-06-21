package net.calvuz.qreport.checkup.status.domain.model

import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable

/**
 * Master data row for a checkup workflow status (e.g. "Bozza", "Completato").
 * Unlike sibling master tables ([net.calvuz.qreport.checkup.modules.domain.model.ModuleTypeMaster],
 * [net.calvuz.qreport.checkup.criticality.domain.model.CriticalityMaster]), this one
 * also carries workflow rules as data, replacing what used to be hardcoded `when`
 * branches in [net.calvuz.qreport.checkup.checkup.domain.usecase.UpdateCheckUpStatusUseCase]
 * and [net.calvuz.qreport.checkup.checkup.domain.usecase.DeleteCheckUpUseCase]:
 * - [blocksDeletion]: a checkup currently in this status cannot be deleted (without `force`).
 * - [marksCompletion]: transitioning a checkup into this status stamps `completedAt`.
 *
 * Allowed transitions out of this status live separately, in the
 * `checkup_status_transitions` crossref table.
 */
@Serializable
data class CheckUpStatusMaster(
    val id: String,
    val code: String,
    val label: String,
    val colorHex: String,
    val iconEmoji: String? = null,
    val sortOrder: Int = 0,
    val isActive: Boolean = true,
    val blocksDeletion: Boolean = false,
    val marksCompletion: Boolean = false,
    val createdAt: Instant,
    val updatedAt: Instant
)
