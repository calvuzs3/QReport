package net.calvuz.qreport.ti.data.conterter

import androidx.room.TypeConverter
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import net.calvuz.qreport.ti.domain.model.CustomerData
import net.calvuz.qreport.ti.domain.model.CustomerSignature
import net.calvuz.qreport.ti.domain.model.ExternalReport
import net.calvuz.qreport.ti.domain.model.InterventionStatus
import net.calvuz.qreport.ti.domain.model.MaterialsUsed
import net.calvuz.qreport.ti.domain.model.RobotData
import net.calvuz.qreport.ti.domain.model.TechnicianSignature
import net.calvuz.qreport.ti.domain.model.WorkDay
import net.calvuz.qreport.ti.domain.model.WorkLocation

/**
 * TypeConverters for complex objects in TechnicalIntervention
 */
object TechnicalInterventionTypeConverters {

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    // CustomerData converters
    @TypeConverter
    fun fromCustomerData(value: CustomerData): String = json.encodeToString(value)

    @TypeConverter
    fun toCustomerData(value: String): CustomerData = json.decodeFromString(value)

    // RobotData converters
    @TypeConverter
    fun fromRobotData(value: RobotData): String = json.encodeToString(value)

    @TypeConverter
    fun toRobotData(value: String): RobotData = json.decodeFromString(value)

    // WorkLocation converters
    @TypeConverter
    fun fromWorkLocation(value: WorkLocation): String = json.encodeToString(value)

    @TypeConverter
    fun toWorkLocation(value: String): WorkLocation = json.decodeFromString(value)

    // List<String> converters (technicians)
    @TypeConverter
    fun fromStringList(value: List<String>): String = json.encodeToString(value)

    @TypeConverter
    fun toStringList(value: String): List<String> = json.decodeFromString(value)

    // List<WorkDay> converters
    @TypeConverter
    fun fromWorkDayList(value: List<WorkDay>): String = json.encodeToString(value)

    @TypeConverter
    fun toWorkDayList(value: String): List<WorkDay> = json.decodeFromString(value)

    // MaterialsUsed converters (nullable)
    @TypeConverter
    fun fromMaterialsUsed(value: MaterialsUsed?): String? =
        value?.let { json.encodeToString(it) }

    @TypeConverter
    fun toMaterialsUsed(value: String?): MaterialsUsed? =
        value?.let { json.decodeFromString(it) }

    // ExternalReport converters (nullable)
    @TypeConverter
    fun fromExternalReport(value: ExternalReport?): String? =
        value?.let { json.encodeToString(it) }

    @TypeConverter
    fun toExternalReport(value: String?): ExternalReport? =
        value?.let { json.decodeFromString(it) }

    // TechnicianSignature converters (nullable)
    @TypeConverter
    fun fromTechnicianSignature(value: TechnicianSignature?): String? =
        value?.let { json.encodeToString(it) }

    @TypeConverter
    fun toTechnicianSignature(value: String?): TechnicianSignature? =
        value?.let { json.decodeFromString(it) }

    // CustomerSignature converters (nullable)
    @TypeConverter
    fun fromCustomerSignature(value: CustomerSignature?): String? =
        value?.let { json.encodeToString(it) }

    @TypeConverter
    fun toCustomerSignature(value: String?): CustomerSignature? =
        value?.let { json.decodeFromString(it) }

    // InterventionStatus enum converters
    @TypeConverter
    fun fromInterventionStatus(value: InterventionStatus): String = value.name

    @TypeConverter
    fun toInterventionStatus(value: String): InterventionStatus =
        InterventionStatus.valueOf(value)
}