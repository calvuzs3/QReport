package net.calvuz.qreport.presentation.ui.theme

import androidx.compose.ui.graphics.Color

// =============================================
// QReport Color System - Industrial Theme
// =============================================

// Primary Colors - Professional Blue
val QReportBlue = Color(0xFF1976D2)       // Primary blue - brand color
val QReportBlueLight = Color(0xFF63A4FF)  // Light blue variant
val QReportBlueDark = Color(0xFF004BA0)   // Dark blue variant

// Secondary Colors - Warning Orange
val QReportOrange = Color(0xFFFF8F00)     // Secondary orange - warnings/attention
val QReportOrangeLight = Color(0xFFFFC947) // Light orange
val QReportOrangeDark = Color(0xFFC56000)  // Dark orange

// Status Colors
val QReportGreen = Color(0xFF388E3C)      // Success/OK status
val QReportGreenLight = Color(0xFF6ABF47) // Light green
val QReportGreenDark = Color(0xFF00600F)  // Dark green

val QReportRed = Color(0xFFD32F2F)        // Error/NOK status
val QReportRedLight = Color(0xFFFF6659)   // Light red
val QReportRedDark = Color(0xFF9A0007)    // Dark red

val QReportYellow = Color(0xFFFBC02D)     // Pending/Warning status
val QReportYellowLight = Color(0xFFFFF263) // Light yellow
val QReportYellowDark = Color(0xFFC49000) // Dark yellow

// Neutral Colors - Professional Grey Scale
val QReportWhite = Color(0xFFFFFFFF)
val QReportBlack = Color(0xFF000000)
val QReportGrey50 = Color(0xFFFAFAFA)
val QReportGrey100 = Color(0xFFF5F5F5)
val QReportGrey200 = Color(0xFFEEEEEE)
val QReportGrey300 = Color(0xFFE0E0E0)
val QReportGrey400 = Color(0xFFBDBDBD)
val QReportGrey500 = Color(0xFF9E9E9E)
val QReportGrey600 = Color(0xFF757575)
val QReportGrey700 = Color(0xFF616161)
val QReportGrey800 = Color(0xFF424242)
val QReportGrey900 = Color(0xFF212121)

// =============================================
// Status-Specific Colors
// =============================================

// Check Item Status Colors
val StatusOK = QReportGreen
val StatusNOK = QReportRed
val StatusPending = QReportOrange
val StatusNA = QReportGrey500

// Criticality Colors
val CriticalityHigh = QReportRed
val CriticalityMedium = QReportOrange
val CriticalityLow = QReportGreen
val CriticalityNA = QReportGrey500

// Urgency Colors (for spare parts)
val UrgencyCritical = QReportRed
val UrgencyHigh = QReportOrange
val UrgencyMedium = QReportYellow
val UrgencyLow = QReportGreen

// =============================================
// Surface Variations
// =============================================

// Card Backgrounds
val CardBackground = QReportWhite
val CardBackgroundElevated = Color(0xFFFAFAFA)
val CardBackgroundSelected = Color(0xFFE3F2FD)

// Module Colors (for check item grouping)
val ModuleSafety = Color(0xFFE53935)      // Red for safety
val ModuleMechanical = Color(0xFF3F51B5)   // Indigo for mechanical
val ModuleElectrical = Color(0xFFFF9800)   // Orange for electrical
val ModuleSoftware = Color(0xFF4CAF50)     // Green for software
val ModuleSpecific = Color(0xFF9C27B0)     // Purple for island-specific

// =============================================
// Semantic Colors
// =============================================

// Progress Indicators
val ProgressCompleted = QReportGreen
val ProgressInProgress = QReportBlue
val ProgressPending = QReportOrange
val ProgressBackground = QReportGrey200

// Photo/Media Colors
val PhotoBackground = QReportGrey100
val PhotoBorder = QReportGrey300
val PhotoSelected = QReportBlue

// Export Status
val ExportReady = QReportGreen
val ExportProcessing = QReportOrange
val ExportError = QReportRed

// =============================================
// Accessibility Colors
// =============================================

// High contrast variants for better accessibility
val HighContrastPrimary = Color(0xFF0D47A1)
val HighContrastSecondary = Color(0xFFE65100)
val HighContrastError = Color(0xFFB71C1C)
val HighContrastSuccess = Color(0xFF1B5E20)

// Focus indicators
val FocusIndicator = QReportBlue
val FocusIndicatorHigh = Color(0xFF0D47A1)

// =============================================
// Helper Functions
// =============================================

/**
 * Restituisce il colore appropriato per uno stato di check item
 */
fun getCheckItemStatusColor(status: String): Color = when (status.uppercase()) {
    "OK" -> StatusOK
    "NOK" -> StatusNOK
    "PENDING" -> StatusPending
    "NA" -> StatusNA
    else -> QReportGrey500
}

/**
 * Restituisce il colore appropriato per un livello di criticità
 */
fun getCriticalityColor(criticality: String): Color = when (criticality.uppercase()) {
    "CRITICAL" -> CriticalityHigh
    "IMPORTANT" -> CriticalityMedium
    "ROUTINE" -> CriticalityLow
    "NA" -> CriticalityNA
    else -> QReportGrey500
}

/**
 * Restituisce il colore appropriato per un modulo
 */
fun getModuleColor(moduleType: String): Color = when (moduleType.lowercase()) {
    "safety" -> ModuleSafety
    "mechanical" -> ModuleMechanical
    "electrical" -> ModuleElectrical
    "software" -> ModuleSoftware
    else -> ModuleSpecific
}