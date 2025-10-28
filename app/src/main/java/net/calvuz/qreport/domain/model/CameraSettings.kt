package net.calvuz.qreport.domain.model

import kotlinx.serialization.Serializable

/**
 * Impostazioni camera
 */
@Serializable
data class CameraSettings(
    val iso: Int? = null,
    val exposureTime: String? = null,
    val fNumber: Float? = null,
    val focalLength: Float? = null,
    val flash: Boolean = false
)

///**
// * Impostazioni della camera quando è stata scattata la foto
// */
//@Serializable
//data class CameraSettings(
//    val iso: Int? = null,                    // Sensibilità ISO
//    val aperture: String? = null,            // Apertura (es. "f/2.8")
//    val shutterSpeed: String? = null,        // Velocità otturatore (es. "1/125")
//    val focalLength: String? = null,         // Lunghezza focale (es. "24mm")
//    val flashUsed: Boolean = false,          // Flash utilizzato
//    val whiteBalance: String? = null,        // Bilanciamento bianco (es. "Auto", "Daylight")
//    val exposureCompensation: String? = null // Compensazione esposizione (es. "+0.3", "-1.0")
//)