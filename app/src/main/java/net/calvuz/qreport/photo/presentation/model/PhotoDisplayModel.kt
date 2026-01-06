package net.calvuz.qreport.photo.presentation.model

import kotlinx.datetime.Instant
import net.calvuz.qreport.photo.domain.model.Photo

/**
 * Modello per il display delle foto in grid/list.
 */
data class PhotoDisplayModel(
    val photo: Photo,
    val displayCaption: String,
    val formattedDate: String,
    val formattedSize: String,
    val perspectiveLabel: String,
    val resolutionLabel: String,
    val orderIndex: Int
) {
    companion object {
        fun fromPhoto(photo: Photo): PhotoDisplayModel {
            return PhotoDisplayModel(
                photo = photo,
                displayCaption = photo.caption.ifBlank { "Nessuna descrizione" },
                formattedDate = formatPhotoDate(photo.takenAt),
                formattedSize = formatFileSize(photo.fileSize),
                perspectiveLabel = photo.metadata.perspective?.displayName ?: "Generica",
                resolutionLabel = photo.metadata.resolution?.displayName ?: "Standard",
                orderIndex = photo.orderIndex
            )
        }

        private fun formatPhotoDate(instant: Instant): String {
            // Semplificato - in un'implementazione reale si userebbe DateTimeFormatter
            return instant.toString().substringBefore('T').replace('-', '/')
        }

        private fun formatFileSize(bytes: Long): String {
            return when {
                bytes < 1024 -> "${bytes}B"
                bytes < 1024 * 1024 -> "${bytes / 1024}KB"
                else -> "${bytes / (1024 * 1024)}MB"
            }
        }
    }
}