package net.calvuz.qreport.settings.presentation.model

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ViewList
import androidx.compose.material.icons.filled.ViewAgenda
import androidx.compose.material.icons.filled.ViewHeadline
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import net.calvuz.qreport.R
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
@Composable
fun ListViewMode.getCardVariantDescription() = when (this) {
    ListViewMode.FULL -> stringResource(R.string.settings_card_variant_full)
    ListViewMode.COMPACT -> stringResource(R.string.settings_card_variant_compact)
    ListViewMode.MINIMAL -> stringResource(R.string.settings_card_variant_minimal)
}