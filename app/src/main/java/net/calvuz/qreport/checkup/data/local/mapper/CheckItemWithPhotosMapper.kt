package net.calvuz.qreport.checkup.data.local.mapper

import net.calvuz.qreport.checkup.data.local.entity.CheckItemWithPhotos
import net.calvuz.qreport.checkup.domain.model.CheckItem

fun CheckItemWithPhotos.toDomain(): CheckItem {
    return checkItem.toDomain().copy(
        photos = photos.map { it.toDomain() }
    )
}
