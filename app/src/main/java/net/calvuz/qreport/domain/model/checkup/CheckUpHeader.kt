package net.calvuz.qreport.domain.model.checkup

import android.annotation.SuppressLint
import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable
import net.calvuz.qreport.domain.model.ClientInfo
import net.calvuz.qreport.domain.model.island.IslandInfo
import net.calvuz.qreport.domain.model.TechnicianInfo

/**
 * Intestazione del check-up con informazioni cliente e isola
 */
@SuppressLint("UnsafeOptInUsageError")
@Serializable
data class CheckUpHeader(
    val clientInfo: ClientInfo,
    val islandInfo: IslandInfo,
    val technicianInfo: TechnicianInfo,
    val checkUpDate: Instant,
    val notes: String = ""
)