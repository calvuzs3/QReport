package net.calvuz.qreport.app.app.presentation.components.list

sealed interface QrListItemCard {
    enum class QrListItemCardVariant : QrListItemCard {
        FULL,
        COMPACT,
        MINIMAL
    }
}