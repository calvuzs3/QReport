package net.calvuz.qreport.client.document.domain.model

/**
 * MIME type registry for QReport documents.
 *
 * Maps known MIME types to a display name and a suggested [DocumentCategory].
 * Unknown MIME types are always accepted — validation is advisory, never blocking.
 * Field technicians may carry proprietary or uncommon formats.
 */
object DocumentMimeTypes {

    data class MimeTypeInfo(
        val mimeType: String,
        val displayName: String,
        val defaultCategory: DocumentCategory
    )

    val ALL: List<MimeTypeInfo> = listOf(

        // ── PDF ──────────────────────────────────────────────────────────────
        MimeTypeInfo(
            "application/pdf",
            "PDF",
            DocumentCategory.OTHER
        ),

        // ── Word ─────────────────────────────────────────────────────────────
        MimeTypeInfo(
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
            "Word (.docx)",
            DocumentCategory.MANUAL
        ),
        MimeTypeInfo(
            "application/msword",
            "Word (.doc)",
            DocumentCategory.MANUAL
        ),

        // ── Excel ─────────────────────────────────────────────────────────────
        MimeTypeInfo(
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
            "Excel (.xlsx)",
            DocumentCategory.OTHER
        ),
        MimeTypeInfo(
            "application/vnd.ms-excel",
            "Excel (.xls)",
            DocumentCategory.OTHER
        ),

        // ── PowerPoint ────────────────────────────────────────────────────────
        MimeTypeInfo(
            "application/vnd.openxmlformats-officedocument.presentationml.presentation",
            "PowerPoint (.pptx)",
            DocumentCategory.OTHER
        ),

        // ── Text ─────────────────────────────────────────────────────────────
        MimeTypeInfo("text/plain", "Testo (.txt)", DocumentCategory.OTHER),
        MimeTypeInfo("text/csv",   "CSV",          DocumentCategory.OTHER),

        // ── Images ───────────────────────────────────────────────────────────
        MimeTypeInfo("image/jpeg", "Immagine JPEG", DocumentCategory.OTHER),
        MimeTypeInfo("image/png",  "Immagine PNG",  DocumentCategory.OTHER),

        // ── Archives ─────────────────────────────────────────────────────────
        MimeTypeInfo("application/zip",            "Archivio ZIP", DocumentCategory.OTHER),
        MimeTypeInfo("application/x-rar-compressed", "Archivio RAR", DocumentCategory.OTHER),

        // ── Binary / unknown ─────────────────────────────────────────────────
        MimeTypeInfo("application/octet-stream", "File binario", DocumentCategory.OTHER)
    )

    private val byMimeType: Map<String, MimeTypeInfo> = ALL.associateBy { it.mimeType }

    /** Returns info for a known MIME type, or null for unknown types. */
    fun forMimeType(mime: String): MimeTypeInfo? = byMimeType[mime]

    /** True if the MIME type is in the known registry. */
    fun isKnown(mime: String): Boolean = byMimeType.containsKey(mime)

    // ── Picker filter constants ───────────────────────────────────────────────

    /** Accepts all file types — recommended for industrial use. */
    const val PICKER_ALL      = "*/*"

    /** Accepts PDF only — use when the use case is narrowly defined. */
    const val PICKER_PDF_ONLY = "application/pdf"
}