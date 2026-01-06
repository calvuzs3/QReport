package net.calvuz.qreport.photo.domain.model

import kotlinx.serialization.Serializable

/**
 * Informazioni di geolocalizzazione
 */
@Serializable
data class PhotoLocation(
    val latitude: Double,
    val longitude: Double,
    val altitude: Double? = null,
    val accuracy: Float? = null
)