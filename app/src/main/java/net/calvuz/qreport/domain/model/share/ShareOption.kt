package net.calvuz.qreport.domain.model.share

import android.graphics.drawable.Drawable
import android.media.Image
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * Opzione di condivisione disponibile
 */
data class ShareOption(
    val type: ShareOptionType,
    val title: String,
    val subtitle: String,
    val icon: Drawable? = null,
    val ivIcon: ImageVector? = null,
    val targetPackage: String? = null,
    val shareMode: ShareMode
)