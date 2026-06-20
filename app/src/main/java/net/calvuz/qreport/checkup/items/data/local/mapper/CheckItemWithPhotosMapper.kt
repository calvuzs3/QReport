package net.calvuz.qreport.checkup.items.data.local.mapper

import net.calvuz.qreport.checkup.items.data.local.entity.CheckItemWithPhotos
import net.calvuz.qreport.checkup.items.domain.model.CheckItem
import net.calvuz.qreport.checkup.data.local.mapper.toDomain

fun CheckItemWithPhotos.toDomain(): CheckItem {
    return checkItem.toDomain().copy(
        photos = photos.map { it.toDomain() }
    )
}
