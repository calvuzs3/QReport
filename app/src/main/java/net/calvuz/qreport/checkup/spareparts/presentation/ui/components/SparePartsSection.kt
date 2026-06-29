package net.calvuz.qreport.checkup.spareparts.presentation.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import net.calvuz.qreport.checkup.spareparts.domain.model.CheckUpSparePart

@Composable
fun SparePartsSection(
    spareParts: List<CheckUpSparePart>,
    onAddClick: () -> Unit,
    onRemove: (String) -> Unit,
    onQuantityChange: (String, Double?) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Ricambi necessari",
                    style = MaterialTheme.typography.titleSmall
                )
                TextButton(onClick = onAddClick) {
                    Icon(Icons.Filled.Add, contentDescription = null)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Aggiungi")
                }
            }

            if (spareParts.isEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Nessun ricambio aggiunto",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                spareParts.forEach { part ->
                    SparePartRow(
                        part = part,
                        onRemove = { onRemove(part.id) },
                        onQuantityChange = { qty -> onQuantityChange(part.id, qty) }
                    )
                }
            }
        }
    }
}

@Composable
private fun SparePartRow(
    part: CheckUpSparePart,
    onRemove: () -> Unit,
    onQuantityChange: (Double?) -> Unit,
    modifier: Modifier = Modifier
) {
    var qtyText by remember(part.id, part.quantity) {
        mutableStateOf(part.quantity?.toString() ?: "")
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = part.name,
                style = MaterialTheme.typography.bodyMedium
            )
            if (part.displayCode.isNotEmpty()) {
                Text(
                    text = "${part.displayCode}  •  ${part.unit}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                Text(
                    text = part.unit,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        OutlinedTextField(
            value = qtyText,
            onValueChange = { raw ->
                qtyText = raw
                onQuantityChange(raw.toDoubleOrNull())
            },
            label = { Text("Qtà") },
            modifier = Modifier.width(90.dp),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            singleLine = true
        )

        IconButton(onClick = onRemove) {
            Icon(
                Icons.Filled.Close,
                contentDescription = "Rimuovi",
                tint = MaterialTheme.colorScheme.error
            )
        }
    }
}
