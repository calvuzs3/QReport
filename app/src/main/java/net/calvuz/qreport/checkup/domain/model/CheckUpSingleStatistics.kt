package net.calvuz.qreport.checkup.domain.model

/**
 * Statistiche di un check-up - VERSIONE CON DEFAULT VALUES
 */
data class CheckUpSingleStatistics(
    val totalModules:Int = 0,
    val totalItems: Int = 0,
    val completedItems: Int = 0,
    val okItems: Int = 0,
    val nokItems: Int = 0,
    val naItems: Int = 0,
    val pendingItems: Int = 0,
    val criticalIssues: Int = 0,
    val importantIssues: Int = 0,
    val modulesCount: Int = 0,
    val photosCount: Int = 0,
    val sparePartsCount: Int = 0,

    /* between 0 and 1 */
    val completionPercentage: Float = 0f
) {
    val okPercentage: Int get() = if (totalItems > 0) (okItems) / totalItems else 0
    val nokPercentage: Int get() = if (totalItems > 0) (nokItems) / totalItems else 0

    fun getCompliancePercentage(): Float {
        val applicableItems = totalItems - naItems
        return if (applicableItems > 0) {
            (okItems.toFloat() / applicableItems) * 100f
        } else 0f
    }
}