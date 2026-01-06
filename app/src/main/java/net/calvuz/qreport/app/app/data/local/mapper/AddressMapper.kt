package net.calvuz.qreport.app.app.data.local.mapper

import net.calvuz.qreport.app.app.data.converter.AddressConverter
import net.calvuz.qreport.app.app.domain.model.Address
import javax.inject.Inject

/**
 * Mapper semplificato per Address con approccio JSON
 * Utilizza AddressConverter per serializzazione JSON invece di entity embedded
 *
 * Consistente con ClientEntity (headquarters_json) e FacilityEntity (address_json)
 */
class AddressMapper @Inject constructor() {

    private val converter = AddressConverter()

    /**
     * Converte JSON string a Address domain model
     */
    fun fromJson(addressJson: String?): Address? {
        return converter.toAddress(addressJson)
    }

    /**
     * Converte Address domain model a JSON string
     */
    fun toJson(address: Address?): String? {
        return converter.fromAddress(address)
    }

    /**
     * Conversione sicura da Address a JSON con fallback
     */
    fun toJsonSafe(address: Address?): String {
        return converter.safeFromAddress(address)
    }

    /**
     * Conversione sicura da JSON a Address con fallback
     */
    fun fromJsonSafe(addressJson: String?): Address? {
        return converter.safeToAddress(addressJson)
    }

    /**
     * Verifica se il JSON contiene un indirizzo valido
     */
    fun hasValidAddress(addressJson: String?): Boolean {
        return fromJsonSafe(addressJson) != null
    }

    /**
     * Estrae città dal JSON senza deserializzare tutto l'oggetto
     * Utile per query e ricerche veloci
     */
    fun extractCityFromJson(addressJson: String?): String? {
        val address = fromJsonSafe(addressJson)
        return address?.city
    }

    /**
     * Estrae provincia dal JSON
     */
    fun extractProvinceFromJson(addressJson: String?): String? {
        val address = fromJsonSafe(addressJson)
        return address?.province
    }

    /**
     * Estrae coordinate dal JSON
     */
    fun extractCoordinatesFromJson(addressJson: String?): Pair<Double, Double>? {
        val address = fromJsonSafe(addressJson)
        return address?.coordinates?.let { coords ->
            Pair(coords.latitude, coords.longitude)
        }
    }
}

/**
 * Extension functions per Address JSON
 */

/**
 * Converte Address a JSON string
 */
fun Address?.toJsonString(): String? {
    return AddressConverter().fromAddress(this)
}

/**
 * Converte Address a JSON string con fallback sicuro
 */
fun Address?.toJsonSafe(): String {
    return AddressConverter().safeFromAddress(this)
}

/**
 * Converte JSON string a Address
 */
fun String?.toAddress(): Address? {
    return AddressConverter().toAddress(this)
}

/**
 * Converte JSON string a Address con fallback sicuro
 */
fun String?.toAddressSafe(): Address? {
    return AddressConverter().safeToAddress(this)
}