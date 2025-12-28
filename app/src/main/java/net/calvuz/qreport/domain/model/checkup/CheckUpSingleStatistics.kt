package net.calvuz.qreport.domain.model.checkup

/**
 * Statistiche di un check-up - VERSIONE CON DEFAULT VALUES
 */
data class CheckUpSingleStatistics(
    val totalItems: Int = 0,
    val completedItems: Int = 0,
    val okItems: Int = 0,
    val nokItems: Int = 0,
    val naItems: Int = 0,
    val pendingItems: Int = 0,
    val criticalIssues: Int = 0,
    val importantIssues: Int = 0,
    val photosCount: Int = 0,
    val sparePartsCount: Int = 0,
    val completionPercentage: Float = 0f
) {
    fun getCompliancePercentage(): Float {
        val applicableItems = totalItems - naItems
        return if (applicableItems > 0) {
            (okItems.toFloat() / applicableItems) * 100f
        } else 0f
    }
}