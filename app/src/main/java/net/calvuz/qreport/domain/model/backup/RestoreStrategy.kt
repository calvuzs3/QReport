package net.calvuz.qreport.domain.model.backup

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Merge
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * RestoreStrategy - Strategia di ripristino
 */
enum class RestoreStrategy(
    val displayName: String,
    val description: String,
    val icon: ImageVector
) {
    REPLACE_ALL(
        "Sostituisci Tutto",
        "Elimina tutti i dati esistenti e ripristina dal backup",
        Icons.Default.Delete
    ),
    MERGE(
        "Unisci Dati",
        "Mantiene dati esistenti e aggiunge quelli dal backup",
        Icons.Default.Merge
    ),
    SELECTIVE(
        "Ripristino Selettivo",
        "Permette di scegliere specifiche tabelle da ripristinare",
        Icons.Default.SelectAll
    );
}