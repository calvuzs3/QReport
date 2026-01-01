package net.calvuz.qreport.presentation.feature.checkup.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import net.calvuz.qreport.R
import net.calvuz.qreport.domain.model.spare.SparePartCategory
import net.calvuz.qreport.domain.model.spare.SparePartUrgency


/**
 * Dialog per aggiungere spare parts
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddSparePartDialog(
    onDismiss: () -> Unit,
    onConfirm: (String, String, Int, SparePartUrgency, SparePartCategory, Double?, String, String) -> Unit,
    isLoading: Boolean,
    modifier: Modifier = Modifier
) {
    var partNumber by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var quantity by remember { mutableStateOf("1") }
    var selectedUrgency by remember { mutableStateOf(SparePartUrgency.MEDIUM) }
    var selectedCategory by remember { mutableStateOf(SparePartCategory.OTHER) }
    var estimatedCost by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    var supplierInfo by remember { mutableStateOf("") }

    val context = LocalContext.current

    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = modifier,
        title = { Text(stringResource(R.string.checkup_screen_detail_dialog_spare_part_title)) },
        text = {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.heightIn(max = 400.dp)
            ) {
                item {
                    OutlinedTextField(
                        value = partNumber,
                        onValueChange = { partNumber = it },
                        label = { Text(stringResource(R.string.checkup_screen_detail_dialog_spare_part_number_label)) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }

                item {
                    OutlinedTextField(
                        value = description,
                        onValueChange = { description = it },
                        label = { Text(stringResource(R.string.checkup_screen_detail_dialog_spare_part_description_label)) },
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 2
                    )
                }

                item {
                    OutlinedTextField(
                        value = quantity,
                        onValueChange = { if (it.all { char -> char.isDigit() }) quantity = it },
                        label = { Text(stringResource(R.string.checkup_screen_detail_dialog_spare_part_quantity_label)) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }

                item {
                    var urgencyExpanded by remember { mutableStateOf(false) }

                    ExposedDropdownMenuBox(
                        expanded = urgencyExpanded,
                        onExpandedChange = { urgencyExpanded = !urgencyExpanded }
                    ) {
                        OutlinedTextField(
                            value = selectedUrgency.displayName,
                            onValueChange = { },
                            readOnly = true,
                            label = { Text(stringResource(R.string.checkup_screen_detail_dialog_spare_part_urgency_label)) },
                            trailingIcon = {
                                ExposedDropdownMenuDefaults.TrailingIcon(expanded = urgencyExpanded)
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor()
                        )

                        ExposedDropdownMenu(
                            expanded = urgencyExpanded,
                            onDismissRequest = { urgencyExpanded = false }
                        ) {
                            SparePartUrgency.entries.forEach { urgency ->
                                DropdownMenuItem(
                                    text = { Text(urgency.displayName) },
                                    onClick = {
                                        selectedUrgency = urgency
                                        urgencyExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }

                item {
                    var categoryExpanded by remember { mutableStateOf(false) }

                    ExposedDropdownMenuBox(
                        expanded = categoryExpanded,
                        onExpandedChange = { categoryExpanded = !categoryExpanded }
                    ) {
                        OutlinedTextField(
                            value = selectedCategory.displayName,
                            onValueChange = { },
                            readOnly = true,
                            label = { Text(stringResource(R.string.checkup_screen_detail_dialog_spare_part_category_label)) },
                            trailingIcon = {
                                ExposedDropdownMenuDefaults.TrailingIcon(expanded = categoryExpanded)
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor()
                        )

                        ExposedDropdownMenu(
                            expanded = categoryExpanded,
                            onDismissRequest = { categoryExpanded = false }
                        ) {
                            SparePartCategory.entries.forEach { category ->
                                DropdownMenuItem(
                                    text = { Text(category.displayName) },
                                    onClick = {
                                        selectedCategory = category
                                        categoryExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }

                item {
                    OutlinedTextField(
                        value = estimatedCost,
                        onValueChange = {
                            // Allow only numbers and decimal point
                            if (it.isEmpty() || it.matches(Regex("^\\d*\\.?\\d*$"))) {
                                estimatedCost = it
                            }
                        },
                        label = { Text(stringResource(R.string.checkup_screen_detail_dialog_spare_part_cost_label)) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        prefix = { Text(stringResource(R.string.checkup_screen_detail_dialog_spare_part_cost_prefix)) }
                    )
                }

                item {
                    OutlinedTextField(
                        value = notes,
                        onValueChange = { notes = it },
                        label = { Text(stringResource(R.string.checkup_screen_detail_dialog_spare_part_notes_label)) },
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 3
                    )
                }

                item {
                    OutlinedTextField(
                        value = supplierInfo,
                        onValueChange = { supplierInfo = it },
                        label = { Text(stringResource(R.string.checkup_screen_detail_dialog_spare_part_supplier_label)) },
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 2
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val quantityInt = quantity.toIntOrNull() ?: 1
                    val costDouble = estimatedCost.toDoubleOrNull()

                    onConfirm(
                        partNumber.trim(),
                        description.trim(),
                        quantityInt,
                        selectedUrgency,
                        selectedCategory,
                        costDouble,
                        notes.trim(),
                        supplierInfo.trim()
                    )
                },
                enabled = !isLoading && partNumber.isNotBlank() && description.isNotBlank()
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp
                    )
                } else {
                    Text(stringResource(R.string.checkup_screen_detail_dialog_spare_part_action_add))
                }
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                enabled = !isLoading
            ) {
                Text(stringResource(R.string.checkup_screen_detail_dialog_spare_part_action_cancel))
            }
        }
    )
}