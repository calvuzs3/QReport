package net.calvuz.qreport.checkup.spareparts.data.local.mapper

import net.calvuz.qreport.checkup.spareparts.data.local.entity.CheckUpSparePartEntity
import net.calvuz.qreport.checkup.spareparts.domain.model.CheckUpSparePart

object CheckUpSparePartMapper {

    fun toDomain(entity: CheckUpSparePartEntity) = CheckUpSparePart(
        id          = entity.id,
        checkupId   = entity.checkupId,
        articleUuid = entity.articleUuid,
        name        = entity.name,
        codeOem     = entity.codeOem,
        codeErp     = entity.codeErp,
        codeBm      = entity.codeBm,
        unit        = entity.unit,
        quantity    = entity.quantity,
        notes       = entity.notes,
        addedAt     = entity.addedAt
    )

    fun toEntity(domain: CheckUpSparePart) = CheckUpSparePartEntity(
        id          = domain.id,
        checkupId   = domain.checkupId,
        articleUuid = domain.articleUuid,
        name        = domain.name,
        codeOem     = domain.codeOem,
        codeErp     = domain.codeErp,
        codeBm      = domain.codeBm,
        unit        = domain.unit,
        quantity    = domain.quantity,
        notes       = domain.notes,
        addedAt     = domain.addedAt
    )
}
