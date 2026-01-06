package net.calvuz.qreport.backup.domain.model.enum

/**
 * BackupType - Tipo di backup
 */
enum class BackupType {
    FULL,         // Backup completo
    INCREMENTAL,  // Solo modifiche (future)
    SELECTIVE     // Solo tabelle selezionate (future)
}