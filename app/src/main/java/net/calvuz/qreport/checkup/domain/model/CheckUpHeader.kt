package net.calvuz.qreport.checkup.domain.model

import android.annotation.SuppressLint
import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable
import net.calvuz.qreport.client.client.domain.model.ClientInfo
import net.calvuz.qreport.client.island.domain.model.IslandInfo
import net.calvuz.qreport.settings.domain.model.TechnicianInfo

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