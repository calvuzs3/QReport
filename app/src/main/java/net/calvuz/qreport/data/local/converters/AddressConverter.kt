package net.calvuz.qreport.data.local.converters

import androidx.room.TypeConverter
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import net.calvuz.qreport.domain.model.client.Address

/**
 * TypeConverter per serializzazione JSON di Address
 * Utilizzato nel database Room per convertire Address → String JSON e viceversa
 *
 * Consistente con l'approccio usato in ClientEntity (headquarters_json)
 * e FacilityEntity (address_json)
 */
class AddressConverter {

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    @TypeConverter
    fun fromAddress(address: Address?): String? {
        return address?.let { json.encodeToString(it) }
    }

    @TypeConverter
    fun toAddress(addressJson: String?): Address? {
        return addressJson?.let {
            try {
                json.decodeFromString<Address>(it)
            } catch (e: Exception) {
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
        } catch (e: Exception) {
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