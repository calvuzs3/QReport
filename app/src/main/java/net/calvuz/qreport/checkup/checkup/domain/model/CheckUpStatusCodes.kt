package net.calvuz.qreport.checkup.checkup.domain.model

/**
 * `CheckUp.status` is a plain status code, validated against
 * [net.calvuz.qreport.checkup.status.domain.model.CheckUpStatusMaster] (table
 * `checkup_statuses`, fully user-editable from Settings) rather than a fixed enum —
 * see Migration7to8 for the normalization rationale.
 *
 * These constants are *not* a closed set used for branching: they only name the
 * handful of seeded default statuses that a few call sites need to target
 * explicitly (initial status on creation, target status on completion/export).
 * They're safe against user edits because they reference the master row's `id`
 * (the Room primary key, never exposed as editable in the management screen) —
 * not its `code`/`label`, which the user can freely rename.
 */
object CheckUpStatusCodes {
    const val DRAFT = "DRAFT"
    const val IN_PROGRESS = "IN_PROGRESS"
    const val COMPLETED = "COMPLETED"
    const val EXPORTED = "EXPORTED"
    const val ARCHIVED = "ARCHIVED"
}
