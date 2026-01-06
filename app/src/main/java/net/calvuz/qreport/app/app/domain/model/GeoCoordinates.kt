package net.calvuz.qreport.app.app.domain.model

import kotlinx.serialization.Serializable

/**
 * Coordinate geografiche
 */
@Serializable
data class GeoCoordinates(
    val latitude: Double,
    val longitude: Double,
    val altitude: Double? = null,
    val accuracy: Float? = null        // Accuratezza in metri
) {

    /**
     * Coordinate formattate
     */
    override fun toString(): String = "$latitude, $longitude"

    /**
     * Link Google Maps
     */
    fun toGoogleMapsUrl(): String = "https://maps.google.com/?q=$latitude,$longitude"
}