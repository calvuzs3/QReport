package net.calvuz.qreport.client.document.sync

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.File
import java.security.MessageDigest

/**
 * SHA-256 hash utility for document file content.
 *
 * The hash is computed exclusively on raw file bytes — never on metadata
 * (title, category, updatedAt, etc.). This guarantees that a rename or
 * re-categorisation never changes the hash and never triggers a re-download.
 *
 * All methods must be called on [Dispatchers.IO].
 */
object DocumentHash {

    private const val BUFFER_SIZE = 8192
    private const val ALGORITHM   = "SHA-256"

    /**
     * Computes SHA-256 over the raw bytes of [filePath].
     * Reads the file in chunks to support large files without loading
     * the entire content into memory.
     *
     * @throws java.io.IOException if the file cannot be read.
     */
    suspend fun compute(filePath: String): String = withContext(Dispatchers.IO) {
        val digest = MessageDigest.getInstance(ALGORITHM)
        File(filePath).inputStream().buffered(BUFFER_SIZE).use { stream ->
            val buffer = ByteArray(BUFFER_SIZE)
            var bytesRead: Int
            while (stream.read(buffer).also { bytesRead = it } != -1) {
                digest.update(buffer, 0, bytesRead)
            }
        }
        digest.digest().toHexString()
    }

    /**
     * Verifies [filePath] against [expectedHash].
     * Returns true if the file content matches, false otherwise.
     * Logs a warning on mismatch for diagnostic purposes.
     */
    suspend fun verify(filePath: String, expectedHash: String): Boolean {
        val actual = runCatching { compute(filePath) }.getOrElse { e ->
            Timber.e(e, "DocumentHash: failed to compute hash for $filePath")
            return false
        }
        val match = actual == expectedHash
        if (!match) {
            Timber.w(
                "DocumentHash: mismatch for $filePath — " +
                        "expected=$expectedHash actual=$actual"
            )
        }
        return match
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    private fun ByteArray.toHexString(): String =
        joinToString("") { "%02x".format(it) }
}