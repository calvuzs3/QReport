package net.calvuz.qreport.app.app.presentation.components

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable


@Composable
fun ConfirmDeleteDialog(
    objectName: String,
    objectDesc: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    DeleteDialog(
        title = "Elimina $objectName",
        text = "Sei sicuro di voler eliminare $objectName: $objectDesc?\nL'operazione non può essere annullata.",
        onConfirm = onConfirm,
        onDismiss = onDismiss
    )
}