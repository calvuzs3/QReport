package net.calvuz.qreport.export.data.model

import net.calvuz.qreport.checkup.items.domain.model.CheckItem

fun List<CheckItem>.groupByModuleTypeId(): Map<String, List<CheckItem>> {
    return this.groupBy { it.moduleTypeId }
        .toSortedMap()
}

fun List<CheckItem>.getAllPhotos() = this.flatMap { it.photos }

fun Map<String, List<CheckItem>>.getPhotoCountByModule(): Map<String, Int> {
    return this.mapValues { (_, items) -> items.getAllPhotos().size }
}
