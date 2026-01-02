package net.calvuz.qreport.presentation.feature.export.model

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import net.calvuz.qreport.R
import net.calvuz.qreport.domain.model.export.CompressionLevel

@Composable
fun CompressionLevel.getDisplayName(): String {
    return when (this) {
        CompressionLevel.LOW -> stringResource(R.string.export_screen_compression_low)
        CompressionLevel.MEDIUM -> stringResource(R.string.export_screen_compression_medium)
        CompressionLevel.HIGH -> stringResource(R.string.export_screen_compression_high)
    }
}