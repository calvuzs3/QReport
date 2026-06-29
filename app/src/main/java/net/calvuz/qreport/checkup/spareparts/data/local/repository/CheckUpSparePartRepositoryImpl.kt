package net.calvuz.qreport.checkup.spareparts.data.local.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import net.calvuz.qreport.checkup.spareparts.data.local.dao.CheckUpSparePartDao
import net.calvuz.qreport.checkup.spareparts.data.local.mapper.CheckUpSparePartMapper
import net.calvuz.qreport.checkup.spareparts.domain.model.CheckUpSparePart
import net.calvuz.qreport.checkup.spareparts.domain.repository.CheckUpSparePartRepository
import javax.inject.Inject

class CheckUpSparePartRepositoryImpl @Inject constructor(
    private val dao: CheckUpSparePartDao
) : CheckUpSparePartRepository {

    override fun observeByCheckup(checkupId: String): Flow<List<CheckUpSparePart>> =
        dao.observeByCheckup(checkupId).map { list -> list.map(CheckUpSparePartMapper::toDomain) }

    override suspend fun addParts(parts: List<CheckUpSparePart>): Result<Unit> =
        runCatching { dao.insertAll(parts.map(CheckUpSparePartMapper::toEntity)) }

    override suspend fun removePart(id: String): Result<Unit> =
        runCatching { dao.deleteById(id) }

    override suspend fun updateQuantity(id: String, quantity: Double?): Result<Unit> =
        runCatching { dao.updateQuantity(id, quantity) }

    override suspend fun updateNotes(id: String, notes: String): Result<Unit> =
        runCatching { dao.updateNotes(id, notes) }

    override suspend fun clearAllForCheckup(checkupId: String): Result<Unit> =
        runCatching { dao.deleteAllForCheckup(checkupId) }
}
