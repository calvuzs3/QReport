package net.calvuz.qreport.export.presentation.model

import net.calvuz.qreport.R
import net.calvuz.qreport.app.error.presentation.UiText
import net.calvuz.qreport.export.domain.reposirory.PhotoQuality

object PhotoQualityExt {

    fun PhotoQuality.getDisplayName(): UiText {
        return when (this) {
            PhotoQuality.ORIGINAL -> UiText.StringResources(R.string.photo_photo_quality_original_name)
            PhotoQuality.OPTIMIZED -> UiText.StringResources(R.string.photo_photo_quality_optimized_name)
            PhotoQuality.COMPRESSED -> UiText.StringResources(R.string.photo_photo_quality_compressed_name)
        }
    }

}