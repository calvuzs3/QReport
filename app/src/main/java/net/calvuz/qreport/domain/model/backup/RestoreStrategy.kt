package net.calvuz.qreport.domain.model.backup

/**
 * RestoreStrategy - Strategia di ripristino
 */
enum class RestoreStrategy {
    REPLACE_ALL,  // Cancella tutto e ripristina
    MERGE,        // Unisci con esistente (future)
    SELECTIVE     // Ripristino selettivo (future)
}