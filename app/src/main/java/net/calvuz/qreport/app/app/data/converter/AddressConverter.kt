@file:Suppress("HardCodedStringLiteral", "unused")
package net.calvuz.qreport.app.app.data.converter

import net.calvuz.qreport.app.app.domain.model.Address
import net.calvuz.qreport.app.app.domain.model.GeoCoordinates
import org.json.JSONObject
import androidx.room.TypeConverter
import javax.inject.Inject

/**
 * TypeConverter per serializzazione JSON di Address
 * Utilizzato nel database Room per convertire Address ↔ String JSON
 *
 * FIXED: Usa JSON manuale invece di kotlinx.serialization per evitare
 * problemi di configurazione plugin nel modulo data
 */
class AddressConverter @Inject constructor() {

    @TypeConverter
    fun fromAddress(address: Address?): String? {
        return address?.let { address ->
            try {
                val json = JSONObject().apply {
                    address.street?.let { put("street", it) }
                    address.streetNumber?.let { put("streetNumber", it) }
                    address.postalCode?.let { put("postalCode", it) }
                    address.city?.let { put("city", it) }
                    address.province?.let { put("province", it) }
                    put("country", address.country)
                    address.notes?.let { put("notes", it) }

                    // Gestione coordinate GPS
                    address.coordinates?.let { coordinates ->
                        val coordinatesJson = JSONObject().apply {
                            put("latitude", coordinates.latitude)
                            put("longitude", coordinates.longitude)
                            coordinates.altitude?.let { put("altitude", it) }
                            coordinates.accuracy?.let { put("accuracy", it) }
                        }
                        put("coordinates", coordinatesJson)
                    }
                }
                json.toString()
            } catch (_: Exception) {
                null // Fallback sicuro
            }
        }
    }

    @TypeConverter
    fun toAddress(addressJson: String?): Address? {
        return if (addressJson.isNullOrBlank()) {
            null
        } else {
            try {
                val json = JSONObject(addressJson)

                // Parse coordinate se presenti
                val coordinates = if (json.has("coordinates")) {
                    val coordinatesJson = json.getJSONObject("coordinates")
                    GeoCoordinates(
                        latitude = coordinatesJson.getDouble("latitude"),
                        longitude = coordinatesJson.getDouble("longitude"),
                        altitude = if (coordinatesJson.has("altitude")) coordinatesJson.getDouble("altitude") else null,
                        accuracy = if (coordinatesJson.has("accuracy")) coordinatesJson.getDouble("accuracy").toFloat() else null
                    )
                } else null

                Address(
                    street = json.optString("street").takeIf { it.isNotBlank() },
                    streetNumber = json.optString("streetNumber").takeIf { it.isNotBlank() },
                    postalCode = json.optString("postalCode").takeIf { it.isNotBlank() },
                    city = json.optString("city").takeIf { it.isNotBlank() },
                    province = json.optString("province").takeIf { it.isNotBlank() },
                    country = json.optString("country", "Italia"),
                    coordinates = coordinates,
                    notes = json.optString("notes").takeIf { it.isNotBlank() }
                )
            } catch (_: Exception) {
                null // Return null se JSON non valido invece di crash
            }
        }
    }

    /**
     * Utility per conversione sicura con fallback
     */
    fun safeFromAddress(address: Address?): String {
        return try {
            fromAddress(address) ?: "{}"
        } catch (_: Exception) {
            "{}" // JSON vuoto come fallback
        }
    }

    /**
     * Utility per conversione sicura con fallback
     */
    fun safeToAddress(addressJson: String?): Address? {
        return if (addressJson.isNullOrBlank() || addressJson == "{}") {
            null
        } else {
            toAddress(addressJson)
        }
    }
}