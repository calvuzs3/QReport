package net.calvuz.qreport.checkup.status.data.local.mapper

import kotlinx.datetime.Instant
import net.calvuz.qreport.checkup.status.data.local.entity.CheckUpStatusEntity
import net.calvuz.qreport.checkup.status.domain.model.CheckUpStatusMaster
import javax.inject.Inject

class CheckUpStatusMapper @Inject constructor() {

    fun toDomain(entity: CheckUpStatusEntity): CheckUpStatusMaster = CheckUpStatusMaster(
        id = entity.id,
        code = entity.code,
        label = entity.label,
        colorHex = entity.colorHex,
        iconEmoji = entity.iconEmoji,
        sortOrder = entity.sortOrder,
        isActive = entity.isActive,
        blocksDeletion = entity.blocksDeletion,
        marksCompletion = entity.marksCompletion,
        createdAt = Instant.fromEpochMilliseconds(entity.createdAt),
        updatedAt = Instant.fromEpochMilliseconds(entity.updatedAt)
    )

    fun toEntity(domain: CheckUpStatusMaster): CheckUpStatusEntity = CheckUpStatusEntity(
        id = domain.id,
        code = domain.code,
        label = domain.label,
        colorHex = domain.colorHex,
        iconEmoji = domain.iconEmoji,
        sortOrder = domain.sortOrder,
        isActive = domain.isActive,
        blocksDeletion = domain.blocksDeletion,
        marksCompletion = domain.marksCompletion,
        createdAt = domain.createdAt.toEpochMilliseconds(),
        updatedAt = domain.updatedAt.toEpochMilliseconds()
    )

    fun toDomainList(entities: List<CheckUpStatusEntity>): List<CheckUpStatusMaster> = entities.map { toDomain(it) }
}
