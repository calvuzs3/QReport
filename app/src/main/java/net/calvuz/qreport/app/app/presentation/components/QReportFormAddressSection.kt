package net.calvuz.qreport.app.app.presentation.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import net.calvuz.qreport.R

@Composable
fun QReportFormAddressSection(
    street: String,
    streetNumber: String,
    postalCode: String,
    city: String,
    province: String,
    country: String,
    onStreetChange: (String) -> Unit,
    onStreetNumberChange: (String) -> Unit,
    onPostalCodeChange: (String) -> Unit,
    onCityChange: (String) -> Unit,
    onProvinceChange: (String) -> Unit,
    onCountryChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = stringResource(R.string.address_section_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )

            // Street and number
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = street,
                    onValueChange = onStreetChange,
                    label = { Text(stringResource(R.string.address_field_street)) },
                    placeholder = { Text(stringResource(R.string.address_field_street_placeholder)) },
                    modifier = Modifier.weight(2f),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Text,
                        capitalization = KeyboardCapitalization.Words
                    )
                )
                OutlinedTextField(
                    value = streetNumber,
                    onValueChange = onStreetNumberChange,
                    label = { Text(stringResource(R.string.address_field_street_number)) },
                    placeholder = { Text(stringResource(R.string.address_field_street_number_placeholder)) },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
            }

            // City and postal code
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = city,
                    onValueChange = onCityChange,
                    label = { Text(stringResource(R.string.address_field_city)) },
                    placeholder = { Text(stringResource(R.string.address_field_city_placeholder)) },
                    modifier = Modifier.weight(2f),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Text,
                        capitalization = KeyboardCapitalization.Words
                    )
                )
                OutlinedTextField(
                    value = postalCode,
                    onValueChange = onPostalCodeChange,
                    label = { Text(stringResource(R.string.address_field_postal_code)) },
                    placeholder = { Text(stringResource(R.string.address_field_postal_code_placeholder)) },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
            }

            // Province
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = province,
                    onValueChange = onProvinceChange,
                    label = { Text(stringResource(R.string.address_field_province)) },
                    placeholder = { Text(stringResource(R.string.address_field_province_placeholder)) },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Text,
                        capitalization = KeyboardCapitalization.Characters
                    )
                )
            }

            // Country
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = country,
                    onValueChange = onCountryChange,
                    label = { Text(stringResource(R.string.address_field_country)) },
                    placeholder = { Text(stringResource(R.string.address_field_country_placeholder)) },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Text,
                        capitalization = KeyboardCapitalization.Words
                    )
                )
            }
        }
    }
}