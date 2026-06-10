package net.calvuz.qreport.client.facility.data.local.mapper

import kotlinx.datetime.Instant
import net.calvuz.qreport.app.app.data.converter.AddressConverter
import net.calvuz.qreport.client.facility.data.local.entity.FacilityEntity
import net.calvuz.qreport.client.facility.domain.model.Facility
import net.calvuz.qreport.client.facility.domain.model.FacilityType
import javax.inject.Inject

/**
 * Maps between [FacilityEntity] (data layer) and [Facility] (domain layer).
 *
 * Address JSON serialization is delegated to [AddressConverter].
 * FacilityType is stored as its enum name string in the DB.
 *
 * [FacilityEntity.isDeleted] is a data-layer concern: the repository filters
 * deleted rows before calling this mapper, so [Facility] never sees them.
 *
 * Island relationships are NOT part of [Facility]; they are assembled by the
 * repository into [FacilityWithIslands] when needed.
 */
class FacilityMapper @Inject constructor(
    private val addressConverter: AddressConverter
) {
    fun toDomain(entity: FacilityEntity): Facility = Facility(
        id = entity.id,
        clientId = entity.clientId,
        name = entity.name,
        code = entity.code,
        notes = entity.notes,
        facilityType = parseFacilityType(entity.facilityType),
        address = addressConverter.toAddress(entity.addressJson),
        isPrimary = entity.isPrimary,
        isActive = entity.isActive,
        createdAt = Instant.fromEpochMilliseconds(entity.createdAt),
        updatedAt = Instant.fromEpochMilliseconds(entity.updatedAt)
    )

    /**
     * [isDeleted] is always false here; soft-delete is managed by the repository,
     * never written directly from a domain model.
     */
    fun toEntity(domain: Facility): FacilityEntity = FacilityEntity(
        id = domain.id,
        clientId = domain.clientId,
        name = domain.name,
        code = domain.code,
        notes = domain.notes,
        facilityType = domain.facilityType.name,
        addressJson = addressConverter.fromAddress(domain.address),
        isPrimary = domain.isPrimary,
        isActive = domain.isActive,
        createdAt = domain.createdAt.toEpochMilliseconds(),
        updatedAt = domain.updatedAt.toEpochMilliseconds(),
        syncedAt = null,    // Managed by the sync layer, not by the mapper
        isDeleted = false
    )

    fun toDomainList(entities: List<FacilityEntity>): List<Facility> = entities.map { toDomain(it) }

    fun toEntityList(domains: List<Facility>): List<FacilityEntity> = domains.map { toEntity(it) }

    // -------------------------------------------------------------------------

    private fun parseFacilityType(typeString: String): FacilityType = try {
        FacilityType.valueOf(typeString)
    } catch (_: IllegalArgumentException) {
        FacilityType.OTHER // Fallback for legacy data
    }
}

// ─── Extension functions ──────────────────────────────────────────────────────

fun FacilityEntity.toDomain(addressConverter: AddressConverter): Facility =
    FacilityMapper(addressConverter).toDomain(this)

fun Facility.toEntity(addressConverter: AddressConverter): FacilityEntity =
    FacilityMapper(addressConverter).toEntity(this)