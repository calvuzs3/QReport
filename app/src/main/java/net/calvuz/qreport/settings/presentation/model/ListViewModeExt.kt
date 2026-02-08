package net.calvuz.qreport.settings.presentation.model

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ViewList
import androidx.compose.material.icons.filled.ViewAgenda
import androidx.compose.material.icons.filled.ViewHeadline
import net.calvuz.qreport.settings.domain.model.ListViewMode


/**
 * Returns the appropriate icon for the current card variant.
 * The icon hints at the NEXT mode that will be applied on click.
 */
fun ListViewMode.getCardVariantIcon() = when (this) {
    ListViewMode.FULL -> Icons.Default.ViewAgenda
    ListViewMode.COMPACT -> Icons.AutoMirrored.Default.ViewList
    ListViewMode.MINIMAL -> Icons.Default.ViewHeadline
}

/**
 * Returns the accessibility description for the card variant toggle.
 */
fun  ListViewMode.getCardVariantDescription() = when (this) {
    ListViewMode.FULL -> "Vista completa"
    ListViewMode.COMPACT -> "Vista compatta"
    ListViewMode.MINIMAL -> "Vista minimale"
}