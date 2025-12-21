package net.calvuz.qreport.domain.model.backup

/**
 * BackupType - Tipo di backup
 */
enum class BackupType {
    FULL,         // Backup completo
    INCREMENTAL,  // Solo modifiche (future)
    SELECTIVE     // Solo tabelle selezionate (future)
}