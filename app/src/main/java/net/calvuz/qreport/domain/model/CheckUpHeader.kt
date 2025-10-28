package net.calvuz.qreport.domain.model

import android.annotation.SuppressLint
import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable

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