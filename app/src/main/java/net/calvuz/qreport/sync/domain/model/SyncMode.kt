package net.calvuz.qreport.sync.domain.model

/**
 * Defines whether remote sync is active for this device.
 *
 * [LOCAL_ONLY] - default, app works fully offline, no network calls
 * [REMOTE_ENABLED] - bidirectional sync with the remote server is active
 */
enum class SyncMode {
    LOCAL_ONLY,
    REMOTE_ENABLED;

    companion object {
        fun fromString(value: String?): SyncMode =
            entries.firstOrNull { it.name == value } ?: LOCAL_ONLY
    }
}

