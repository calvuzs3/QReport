package net.calvuz.qreport.presentation.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * SearchBar riutilizzabile per QReport
 *
 * Features:
 * - Design Material 3 consistente
 * - Icona search + clear
 * - Placeholder personalizzabile
 * - Callback per query changes
 * - Supporto per diversi tipi di ricerca
 */

@Composable
fun QReportSearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    placeholder: String = "Cerca...",
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    leadingIcon: @Composable (() -> Unit)? = {
        Icon(Icons.Default.Search, contentDescription = "Cerca")
    },
    maxLines: Int = 1
) {
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        label = { Text(placeholder) },
        leadingIcon = leadingIcon,
        trailingIcon = if (query.isNotEmpty()) {
            {
                IconButton(onClick = { onQueryChange("") }) {
                    Icon(Icons.Default.Clear, contentDescription = "Cancella ricerca")
                }
            }
        } else null,
        modifier = modifier.fillMaxWidth(),
        singleLine = maxLines == 1,
        maxLines = maxLines,
        enabled = enabled,
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = MaterialTheme.colorScheme.primary,
            unfocusedBorderColor = MaterialTheme.colorScheme.outline
        )
    )
}

/**
 * Versione compatta della SearchBar per spazi ristretti
 */
@Composable
fun CompactSearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    placeholder: String = "Cerca...",
    modifier: Modifier = Modifier
) {
    TextField(
        value = query,
        onValueChange = onQueryChange,
        placeholder = { Text(placeholder) },
        leadingIcon = {
            Icon(
                Icons.Default.Search,
                contentDescription = null,
                modifier = Modifier.size(20.dp)
            )
        },
        trailingIcon = if (query.isNotEmpty()) {
            {
                IconButton(
                    onClick = { onQueryChange("") },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        Icons.Default.Clear,
                        contentDescription = "Cancella",
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        } else null,
        modifier = modifier,
        singleLine = true,
        colors = TextFieldDefaults.colors(
            focusedIndicatorColor = androidx.compose.ui.graphics.Color.Transparent,
            unfocusedIndicatorColor = androidx.compose.ui.graphics.Color.Transparent,
            disabledIndicatorColor = androidx.compose.ui.graphics.Color.Transparent
        ),
        shape = MaterialTheme.shapes.medium
    )
}

/**
 * SearchBar con suggerimenti/autocomplete
 */
@Composable
fun SearchBarWithSuggestions(
    query: String,
    onQueryChange: (String) -> Unit,
    suggestions: List<String>,
    onSuggestionSelected: (String) -> Unit,
    placeholder: String = "Cerca...",
    modifier: Modifier = Modifier,
    maxSuggestions: Int = 5
) {
    var showSuggestions by remember { mutableStateOf(false) }

    Column(modifier = modifier) {
        QReportSearchBar(
            query = query,
            onQueryChange = { newQuery ->
                onQueryChange(newQuery)
                showSuggestions = newQuery.isNotEmpty() && suggestions.isNotEmpty()
            },
            placeholder = placeholder
        )

        if (showSuggestions && query.isNotEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Column(
                    modifier = Modifier.padding(vertical = 8.dp)
                ) {
                    suggestions.take(maxSuggestions).forEach { suggestion ->
                        TextButton(
                            onClick = {
                                onSuggestionSelected(suggestion)
                                showSuggestions = false
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.Start
                            ) {
                                Icon(
                                    Icons.Default.History,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = suggestion,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * SearchBar per ricerca avanzata con filtri
 */
@Composable
fun AdvancedSearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    filterCount: Int = 0,
    onFiltersClick: () -> Unit,
    placeholder: String = "Cerca...",
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        QReportSearchBar(
            query = query,
            onQueryChange = onQueryChange,
            placeholder = placeholder,
            modifier = Modifier.weight(1f)
        )

        // Bottone filtri con badge
        BadgedBox(
            badge = {
                if (filterCount > 0) {
                    Badge {
                        Text(filterCount.toString())
                    }
                }
            }
        ) {
            OutlinedButton(
                onClick = onFiltersClick,
                modifier = Modifier.height(56.dp)
            ) {
                Icon(Icons.Default.FilterList, contentDescription = "Filtri")
            }
        }
    }
}

/**
 * Preview helpers per development
 */
@Composable
private fun SearchBarPreview() {
    MaterialTheme {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            QReportSearchBar(
                query = "",
                onQueryChange = {},
                placeholder = "Cerca clienti..."
            )

            QReportSearchBar(
                query = "Sample query",
                onQueryChange = {},
                placeholder = "Cerca clienti..."
            )

            CompactSearchBar(
                query = "",
                onQueryChange = {},
                placeholder = "Ricerca rapida"
            )

            AdvancedSearchBar(
                query = "",
                onQueryChange = {},
                filterCount = 3,
                onFiltersClick = {},
                placeholder = "Ricerca avanzata"
            )
        }
    }
}