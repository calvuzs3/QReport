package net.calvuz.qreport.settings.domain.model

/**
 * Generic view mode for list screens.
 *
 * Each list screen stores its own preference independently.
 * Maps to screen-specific card variants (e.g. ClientCardVariant).
 */
enum class ListViewMode {
    FULL,
    COMPACT,
    MINIMAL;

    companion object {
        /** Default view mode for all lists. */
        val DEFAULT = FULL

        /** Safe parse from stored string, falls back to [DEFAULT]. */
        fun fromString(value: String?): ListViewMode {
            return entries.firstOrNull { it.name == value } ?: DEFAULT
        }
    }
}