package net.calvuz.qreport.sync.qstore

import android.content.ContentResolver
import android.database.Cursor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.withContext
import net.calvuz.qreport.checkup.spareparts.domain.model.SelectedArticle

class QStoreArticleReader(
    private val contentResolver: ContentResolver
) {

    private val projection = arrayOf(
        ArticleContract.Articles.UUID,
        ArticleContract.Articles.NAME,
        ArticleContract.Articles.DESCRIPTION,
        ArticleContract.Articles.CODE_OEM,
        ArticleContract.Articles.CODE_ERP,
        ArticleContract.Articles.CODE_BM,
        ArticleContract.Articles.UNIT_OF_MEASURE,
        ArticleContract.Articles.NOTES,
        ArticleContract.Articles.UPDATED_AT,
    )

    suspend fun fetchByUuids(uuids: List<String>): List<SelectedArticle> =
        withContext(Dispatchers.IO) {
            if (uuids.isEmpty()) return@withContext emptyList()
            val placeholders = uuids.joinToString(",") { "?" }
            runCatching {
                contentResolver.query(
                    ArticleContract.ARTICLES_URI,
                    projection,
                    "${ArticleContract.Articles.UUID} IN ($placeholders)",
                    uuids.toTypedArray(),
                    null
                )?.use { it.toSelectedArticleList() }
            }.getOrNull() ?: emptyList()
        }

    suspend fun search(query: String): List<SelectedArticle> =
        withContext(Dispatchers.IO) {
            runCatching {
                contentResolver.query(
                    ArticleContract.ARTICLES_URI,
                    projection,
                    "${ArticleContract.Articles.NAME} LIKE ? OR " +
                    "${ArticleContract.Articles.CODE_OEM} LIKE ? OR " +
                    "${ArticleContract.Articles.CODE_ERP} LIKE ?",
                    arrayOf("%$query%", "%$query%", "%$query%"),
                    "${ArticleContract.Articles.NAME} ASC"
                )?.use { it.toSelectedArticleList() }
            }.getOrNull() ?: emptyList()
        }

    fun observeCatalogChanges(): Flow<Unit> = callbackFlow {
        val observer = object : android.database.ContentObserver(null) {
            override fun onChange(selfChange: Boolean) { trySend(Unit) }
        }
        contentResolver.registerContentObserver(
            ArticleContract.ARTICLES_URI,
            true,
            observer
        )
        awaitClose { contentResolver.unregisterContentObserver(observer) }
    }

    private fun Cursor.toSelectedArticleList(): List<SelectedArticle> =
        buildList {
            while (moveToNext()) { add(toSelectedArticle()) }
        }

    private fun Cursor.toSelectedArticle() = SelectedArticle(
        uuid        = str(ArticleContract.Articles.UUID),
        name        = str(ArticleContract.Articles.NAME),
        description = strOrEmpty(ArticleContract.Articles.DESCRIPTION),
        codeOem     = strOrEmpty(ArticleContract.Articles.CODE_OEM),
        codeErp     = strOrEmpty(ArticleContract.Articles.CODE_ERP),
        codeBm      = strOrEmpty(ArticleContract.Articles.CODE_BM),
        unit        = strOrDefault(ArticleContract.Articles.UNIT_OF_MEASURE, "pz"),
    )

    private fun Cursor.str(col: String) =
        getString(getColumnIndexOrThrow(col))

    private fun Cursor.strOrEmpty(col: String) =
        getColumnIndex(col).takeIf { it >= 0 }?.let { getString(it) }.orEmpty()

    private fun Cursor.strOrDefault(col: String, default: String) =
        getColumnIndex(col).takeIf { it >= 0 }
            ?.let { getString(it) }
            ?.ifEmpty { default }
            ?: default
}
