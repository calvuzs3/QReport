package net.calvuz.qreport.presentation.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

import androidx.compose.material3.*
import androidx.compose.ui.graphics.Color

/**
 * QReport Theme
 *
 * Tema personalizzato per QReport basato su Material Design 3
 * con palette di colori ottimizzata per ambienti industriali.
 *
 * Features:
 * - Color scheme personalizzato (blu industriale)
 * - Typography ottimizzata per leggibilità
 * - Dark/Light theme automatico
 * - Dynamic colors su Android 12+
 */

// Primary colors - Blu industriale
private val md_theme_light_primary = Color(0xFF1565C0)         // Blue 800
private val md_theme_light_onPrimary = Color(0xFFFFFFFF)
private val md_theme_light_primaryContainer = Color(0xFFE3F2FD) // Blue 50
private val md_theme_light_onPrimaryContainer = Color(0xFF0D47A1) // Blue 900

// Secondary colors - Arancione di accento
private val md_theme_light_secondary = Color(0xFFFF8A65)        // Deep Orange 300
private val md_theme_light_onSecondary = Color(0xFFFFFFFF)
private val md_theme_light_secondaryContainer = Color(0xFFFFF3E0) // Orange 50
private val md_theme_light_onSecondaryContainer = Color(0xFFE65100) // Orange 900

// Error colors - Rosso per NOK status
private val md_theme_light_error = Color(0xFFD32F2F)           // Red 700
private val md_theme_light_errorContainer = Color(0xFFFFEBEE)   // Red 50
private val md_theme_light_onError = Color(0xFFFFFFFF)
private val md_theme_light_onErrorContainer = Color(0xFFB71C1C) // Red 900

// Success colors - Verde per OK status
//val QReportGreen = Color(0xFF388E3C)                           // Green 700
val QReportGreenContainer = Color(0xFFE8F5E8)                  // Green 50
val QReportOnGreen = Color(0xFFFFFFFF)
val QReportOnGreenContainer = Color(0xFF1B5E20)                // Green 900

// Warning colors - Ambra per warning status
val QReportAmber = Color(0xFFFFA726)                           // Amber 400
val QReportAmberContainer = Color(0xFFFFF8E1)                  // Amber 50
val QReportOnAmber = Color(0xFF000000)
val QReportOnAmberContainer = Color(0xFFFF6F00)                // Amber 900

// Surface colors
private val md_theme_light_background = Color(0xFFFCFCFC)      // Quasi bianco
private val md_theme_light_onBackground = Color(0xFF1A1A1A)
private val md_theme_light_surface = Color(0xFFFFFFFF)
private val md_theme_light_onSurface = Color(0xFF1A1A1A)
private val md_theme_light_surfaceVariant = Color(0xFFF5F5F5)  // Grey 100
private val md_theme_light_onSurfaceVariant = Color(0xFF424242) // Grey 800

// Dark theme colors
private val md_theme_dark_primary = Color(0xFF90CAF9)          // Blue 200
private val md_theme_dark_onPrimary = Color(0xFF0D47A1)        // Blue 900
private val md_theme_dark_primaryContainer = Color(0xFF1976D2) // Blue 600
private val md_theme_dark_onPrimaryContainer = Color(0xFFE3F2FD) // Blue 50

private val md_theme_dark_secondary = Color(0xFFFFAB91)        // Deep Orange 200
private val md_theme_dark_onSecondary = Color(0xFFE65100)      // Orange 900
private val md_theme_dark_secondaryContainer = Color(0xFFFF7043) // Deep Orange 400
private val md_theme_dark_onSecondaryContainer = Color(0xFFFFF3E0) // Orange 50

private val md_theme_dark_error = Color(0xFFEF5350)            // Red 400
private val md_theme_dark_errorContainer = Color(0xFFD32F2F)   // Red 700
private val md_theme_dark_onError = Color(0xFFFFFFFF)
private val md_theme_dark_onErrorContainer = Color(0xFFFFEBEE) // Red 50

private val md_theme_dark_background = Color(0xFF121212)       // Material dark
private val md_theme_dark_onBackground = Color(0xFFE0E0E0)
private val md_theme_dark_surface = Color(0xFF1E1E1E)
private val md_theme_dark_onSurface = Color(0xFFE0E0E0)
private val md_theme_dark_surfaceVariant = Color(0xFF2E2E2E)
private val md_theme_dark_onSurfaceVariant = Color(0xFFBDBDBD) // Grey 400

private val LightColors = lightColorScheme(
    primary = md_theme_light_primary,
    onPrimary = md_theme_light_onPrimary,
    primaryContainer = md_theme_light_primaryContainer,
    onPrimaryContainer = md_theme_light_onPrimaryContainer,
    secondary = md_theme_light_secondary,
    onSecondary = md_theme_light_onSecondary,
    secondaryContainer = md_theme_light_secondaryContainer,
    onSecondaryContainer = md_theme_light_onSecondaryContainer,
    error = md_theme_light_error,
    errorContainer = md_theme_light_errorContainer,
    onError = md_theme_light_onError,
    onErrorContainer = md_theme_light_onErrorContainer,
    background = md_theme_light_background,
    onBackground = md_theme_light_onBackground,
    surface = md_theme_light_surface,
    onSurface = md_theme_light_onSurface,
    surfaceVariant = md_theme_light_surfaceVariant,
    onSurfaceVariant = md_theme_light_onSurfaceVariant,
)

private val DarkColors = darkColorScheme(
    primary = md_theme_dark_primary,
    onPrimary = md_theme_dark_onPrimary,
    primaryContainer = md_theme_dark_primaryContainer,
    onPrimaryContainer = md_theme_dark_onPrimaryContainer,
    secondary = md_theme_dark_secondary,
    onSecondary = md_theme_dark_onSecondary,
    secondaryContainer = md_theme_dark_secondaryContainer,
    onSecondaryContainer = md_theme_dark_onSecondaryContainer,
    error = md_theme_dark_error,
    errorContainer = md_theme_dark_errorContainer,
    onError = md_theme_dark_onError,
    onErrorContainer = md_theme_dark_onErrorContainer,
    background = md_theme_dark_background,
    onBackground = md_theme_dark_onBackground,
    surface = md_theme_dark_surface,
    onSurface = md_theme_dark_onSurface,
    surfaceVariant = md_theme_dark_surfaceVariant,
    onSurfaceVariant = md_theme_dark_onSurfaceVariant,
)

/**
 * Tema principale QReport
 */
@Composable
fun QReportTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true, // Available on Android 12+
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }

        darkTheme -> DarkColors
        else -> LightColors
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.primary.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = QReportTypography,
        content = content
    )
}

/**
 * Estensioni ColorScheme per colori custom QReport
 */
val ColorScheme.success: Color
    @Composable get() = QReportGreen

val ColorScheme.onSuccess: Color
    @Composable get() = QReportOnGreen

val ColorScheme.successContainer: Color
    @Composable get() = QReportGreenContainer

val ColorScheme.onSuccessContainer: Color
    @Composable get() = QReportOnGreenContainer

val ColorScheme.warning: Color
    @Composable get() = QReportAmber

val ColorScheme.onWarning: Color
    @Composable get() = QReportOnAmber

val ColorScheme.warningContainer: Color
    @Composable get() = QReportAmberContainer

val ColorScheme.onWarningContainer: Color
    @Composable get() = QReportOnAmberContainer



// Colori QReport - Brand industriale professionale
private val QReportLightColors = lightColorScheme(
    primary = QReportBlue,
    onPrimary = QReportWhite,
    primaryContainer = QReportBlueLight,
    onPrimaryContainer = QReportBlueDark,
    secondary = QReportOrange,
    onSecondary = QReportWhite,
    secondaryContainer = QReportOrangeLight,
    onSecondaryContainer = QReportOrangeDark,
    tertiary = QReportGreen,
    onTertiary = QReportWhite,
    tertiaryContainer = QReportGreenLight,
    onTertiaryContainer = QReportGreenDark,
    error = QReportRed,
    onError = QReportWhite,
    errorContainer = QReportRedLight,
    onErrorContainer = QReportRedDark,
    background = QReportGrey50,
    onBackground = QReportGrey900,
    surface = QReportWhite,
    onSurface = QReportGrey900,
    surfaceVariant = QReportGrey100,
    onSurfaceVariant = QReportGrey700,
    outline = QReportGrey400,
    outlineVariant = QReportGrey200,
    scrim = QReportBlack,
    inverseSurface = QReportGrey900,
    inverseOnSurface = QReportGrey50,
    inversePrimary = QReportBlueLight,
    surfaceDim = QReportGrey100,
    surfaceBright = QReportWhite,
    surfaceContainerLowest = QReportWhite,
    surfaceContainerLow = QReportGrey50,
    surfaceContainer = QReportGrey100,
    surfaceContainerHigh = QReportGrey200,
    surfaceContainerHighest = QReportGrey300
)

private val QReportDarkColors = darkColorScheme(
    primary = QReportBlueLight,
    onPrimary = QReportBlueDark,
    primaryContainer = QReportBlueDark,
    onPrimaryContainer = QReportBlueLight,
    secondary = QReportOrangeLight,
    onSecondary = QReportOrangeDark,
    secondaryContainer = QReportOrangeDark,
    onSecondaryContainer = QReportOrangeLight,
    tertiary = QReportGreenLight,
    onTertiary = QReportGreenDark,
    tertiaryContainer = QReportGreenDark,
    onTertiaryContainer = QReportGreenLight,
    error = QReportRedLight,
    onError = QReportRedDark,
    errorContainer = QReportRedDark,
    onErrorContainer = QReportRedLight,
    background = QReportBlack,
    onBackground = QReportGrey100,
    surface = QReportGrey900,
    onSurface = QReportGrey100,
    surfaceVariant = QReportGrey800,
    onSurfaceVariant = QReportGrey300,
    outline = QReportGrey600,
    outlineVariant = QReportGrey800,
    scrim = QReportBlack,
    inverseSurface = QReportGrey100,
    inverseOnSurface = QReportGrey900,
    inversePrimary = QReportBlue,
    surfaceDim = QReportGrey900,
    surfaceBright = QReportGrey700,
    surfaceContainerLowest = QReportBlack,
    surfaceContainerLow = QReportGrey900,
    surfaceContainer = QReportGrey800,
    surfaceContainerHigh = QReportGrey700,
    surfaceContainerHighest = QReportGrey600
)

//@Composable
//fun QReportTheme(
//    darkTheme: Boolean = isSystemInDarkTheme(),
//    // Dynamic color su Android 12+
//    dynamicColor: Boolean = false, // Disabilitiamo per mantenere brand consistency
//    content: @Composable () -> Unit
//) {
//    val colorScheme = when {
//        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
//            val context = LocalContext.current
//            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
//        }
//
//        darkTheme -> QReportDarkColors
//        else -> QReportLightColors
//    }
//
//    val view = LocalView.current
//    if (!view.isInEditMode) {
//        SideEffect {
//            val window = (view.context as Activity).window
//            window.statusBarColor = colorScheme.primary.toArgb()
//            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = darkTheme
//        }
//    }
//
//    MaterialTheme(
//        colorScheme = colorScheme,
//        typography = QReportTypography,
//        content = content
//    )
//}