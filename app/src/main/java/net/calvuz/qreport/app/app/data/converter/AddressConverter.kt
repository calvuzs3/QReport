package net.calvuz.qreport.app.app.data.converter

import androidx.room.TypeConverter
import net.calvuz.qreport.app.app.domain.model.Address
import net.calvuz.qreport.app.app.domain.model.GeoCoordinates
import org.json.JSONObject
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
        return address?.let { addr ->
            try {
                val json = JSONObject().apply {
                    addr.street?.let { put("street", it) }
                    addr.streetNumber?.let { put("streetNumber", it) }
                    addr.city?.let { put("city", it) }
                    addr.province?.let { put("province", it) }
                    addr.region?.let { put("region", it) }
                    addr.postalCode?.let { put("postalCode", it) }
                    put("country", addr.country)
                    addr.notes?.let { put("notes", it) }

                    // Gestione coordinate GPS
                    addr.coordinates?.let { coords ->
                        val coordsJson = JSONObject().apply {
                            put("latitude", coords.latitude)
                            put("longitude", coords.longitude)
                            coords.altitude?.let { put("altitude", it) }
                            coords.accuracy?.let { put("accuracy", it) }
                        }
                        put("coordinates", coordsJson)
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
                    val coordsJson = json.getJSONObject("coordinates")
                    GeoCoordinates(
                        latitude = coordsJson.getDouble("latitude"),
                        longitude = coordsJson.getDouble("longitude"),
                        altitude = if (coordsJson.has("altitude")) coordsJson.getDouble("altitude") else null,
                        accuracy = if (coordsJson.has("accuracy")) coordsJson.getDouble("accuracy").toFloat() else null
                    )
                } else null

                Address(
                    street = json.optString("street").takeIf { it.isNotBlank() },
                    streetNumber = json.optString("streetNumber").takeIf { it.isNotBlank() },
                    city = json.optString("city").takeIf { it.isNotBlank() },
                    province = json.optString("province").takeIf { it.isNotBlank() },
                    region = json.optString("region").takeIf { it.isNotBlank() },
                    postalCode = json.optString("postalCode").takeIf { it.isNotBlank() },
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