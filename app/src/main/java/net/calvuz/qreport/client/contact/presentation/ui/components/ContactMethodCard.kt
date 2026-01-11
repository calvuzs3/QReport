package net.calvuz.qreport.client.contact.presentation.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Launch
import androidx.compose.material.icons.filled.AlternateEmail
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import net.calvuz.qreport.R
import net.calvuz.qreport.client.contact.domain.model.Contact
import net.calvuz.qreport.client.contact.domain.model.ContactMethod


@Composable
fun ContactMethodsCard(
    contact: Contact,
    onPhoneClick: (String) -> Unit,
    onEmailClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = stringResource(R.string.contact_contact_methods),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )

            // Phone
            contact.phone?.let { phone ->
                ContactMethodItem(
                    icon = Icons.Default.Phone,
                    label = stringResource(R.string.contact_field_phone),
                    value = phone,
                    onClick = { onPhoneClick(phone) },
                    isPrimary = contact.preferredContactMethod == ContactMethod.PHONE
                )
            }

            // Mobile
            contact.mobilePhone?.let { mobile ->
                ContactMethodItem(
                    icon = Icons.Default.PhoneAndroid,
                    label = stringResource(R.string.contact_field_mobile),
                    value = mobile,
                    onClick = { onPhoneClick(mobile) },
                    isPrimary = contact.preferredContactMethod == ContactMethod.MOBILE
                )
            }

            // Email
            contact.email?.let { email ->
                ContactMethodItem(
                    icon = Icons.Default.Email,
                    label = stringResource(R.string.contact_field_email),
                    value = email,
                    onClick = { onEmailClick(email) },
                    isPrimary = contact.preferredContactMethod == ContactMethod.EMAIL
                )
            }

            // Alternative Email
            contact.alternativeEmail?.let { altEmail ->
                ContactMethodItem(
                    icon = Icons.Default.AlternateEmail,
                    label = stringResource(R.string.contact_field_alternative_email),
                    value = altEmail,
                    onClick = { onEmailClick(altEmail) }
                )
            }
        }
    }
}


@Composable
fun ContactMethodItem(
    modifier: Modifier = Modifier,
    icon: ImageVector,
    label: String,
    value: String,
    onClick: () -> Unit,
    isPrimary: Boolean = false
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (isPrimary) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(18.dp)
        )

        Column(modifier = Modifier.weight(1f)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                if (isPrimary) {
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = stringResource(R.string.label_preferred),
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(12.dp)
                    )
                }
            }

            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary
            )
        }

        IconButton(onClick = onClick) {
            Icon(
                imageVector = Icons.AutoMirrored.Default.Launch,
                contentDescription = stringResource(R.string.action_contact),
                modifier = Modifier.size(18.dp)
            )
        }
    }
}
