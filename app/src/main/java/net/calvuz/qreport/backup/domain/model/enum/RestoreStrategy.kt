package net.calvuz.qreport.backup.domain.model.enum

/**
 * RestoreStrategy - Strategia di ripristino
 */
enum class RestoreStrategy() {
    REPLACE_ALL,
    MERGE,
    SELECTIVE
}