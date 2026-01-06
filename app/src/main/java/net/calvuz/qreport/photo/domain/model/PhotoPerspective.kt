package net.calvuz.qreport.photo.domain.model

import kotlinx.serialization.Serializable

/**
 * Enum per località foto (se serve)
 */
@Serializable
enum class PhotoPerspective(val displayName: String) {
    FRONT("Frontale"),
    BACK("Retro"),
    LEFT("Sinistra"),
    RIGHT("Destra"),
    TOP("Superiore"),
    BOTTOM("Inferiore"),
    DETAIL("Dettaglio"),
    OVERVIEW("Panoramica"),
    ISSUE("Problema"),
    REPAIR("Riparazione");
}