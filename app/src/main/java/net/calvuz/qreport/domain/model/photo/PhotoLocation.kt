package net.calvuz.qreport.domain.model.photo

/**
 * Informazioni di geolocalizzazione
 */
data class PhotoLocation(
    val latitude: Double,
    val longitude: Double,
    val altitude: Double? = null,
    val accuracy: Float? = null
)