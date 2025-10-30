package net.calvuz.qreport.presentation.navigation

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.navigation.NavType
import net.calvuz.qreport.presentation.screen.home.HomeScreen
import net.calvuz.qreport.presentation.screen.checkup.CheckUpListScreen
import net.calvuz.qreport.presentation.screen.checkup.NewCheckUpScreen
import net.calvuz.qreport.presentation.screen.checkup.CheckUpDetailScreen
import net.calvuz.qreport.presentation.screen.archive.ArchiveScreen
import net.calvuz.qreport.presentation.screen.settings.SettingsScreen

/**
 * Sistema di navigazione QReport - COMPLETO
 *
 * Gestisce la navigazione principale dell'app con:
 * - Bottom Navigation Bar con 4 destinazioni principali
 * - Navigation Component per Compose
 * - State management per destinazioni attive
 * - Flusso completo creazione e dettagli check-up
 *
 * Struttura:
 * - Home: Dashboard con quick actions
 * - Check-up: Lista e gestione check-up
 * - Archivio: Storico check-up completati
 * - Impostazioni: Configurazioni app
 */

/**
 * Sealed class per le destinazioni principali
 */
sealed class QReportDestination(
    val route: String,
    val title: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
) {
    object Home : QReportDestination(
        route = "home",
        title = "Home",
        selectedIcon = Icons.Filled.Home,
        unselectedIcon = Icons.Outlined.Home
    )

    object CheckUps : QReportDestination(
        route = "checkups",
        title = "Check-up",
        selectedIcon = Icons.Filled.Assignment,
        unselectedIcon = Icons.Outlined.Assignment
    )

    object Archive : QReportDestination(
        route = "archive",
        title = "Archivio",
        selectedIcon = Icons.Filled.Archive,
        unselectedIcon = Icons.Outlined.Archive
    )

    object Settings : QReportDestination(
        route = "settings",
        title = "Impostazioni",
        selectedIcon = Icons.Filled.Settings,
        unselectedIcon = Icons.Outlined.Settings
    )
}

/**
 * Routes per navigazione secondaria
 */
object QReportRoutes {
    const val HOME = "home"
    const val CHECKUPS = "checkups"
    const val ARCHIVE = "archive"
    const val SETTINGS = "settings"

    // Secondary routes
    const val CHECKUP_CREATE = "checkup_create"
    const val CHECKUP_DETAIL = "checkup_detail/{checkUpId}"
    const val CAMERA = "camera/{checkItemId}"

    // Helper functions for parameters
    fun checkupDetail(checkUpId: String) = "checkup_detail/$checkUpId"
    fun camera(checkItemId: String) = "camera/$checkItemId"
}

/**
 * Lista delle destinazioni bottom navigation
 */
val bottomNavDestinations = listOf(
    QReportDestination.Home,
    QReportDestination.CheckUps,
    QReportDestination.Archive,
    QReportDestination.Settings
)

/**
 * Composable principale per la navigazione
 */
@Composable
fun QReportNavigation(
    navController: NavHostController = rememberNavController(),
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        // Main content area
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            NavHost(
                navController = navController,
                startDestination = QReportRoutes.HOME,
                modifier = Modifier.fillMaxSize()
            ) {
                // ============================================================
                // MAIN DESTINATIONS (Bottom Navigation)
                // ============================================================

                composable(QReportRoutes.HOME) {
                    HomeScreen(
                        onNavigateToCheckUps = {
                            navController.navigate(QReportRoutes.CHECKUPS)
                        },
                        onNavigateToArchive = {
                            navController.navigate(QReportRoutes.ARCHIVE)
                        },
                        onNavigateToNewCheckUp = {
                            navController.navigate(QReportRoutes.CHECKUP_CREATE)
                        },
                        onNavigateToCheckUpDetail = { checkUpId ->
                            navController.navigate(QReportRoutes.checkupDetail(checkUpId))
                        }
                    )
                }

                composable(QReportRoutes.CHECKUPS) {
                    CheckUpListScreen(
                        onNavigateToCheckUpDetail = { checkUpId ->
                            navController.navigate(QReportRoutes.checkupDetail(checkUpId))
                        },
                        onCreateNewCheckUp = {
                            navController.navigate(QReportRoutes.CHECKUP_CREATE)
                        }
                    )
                }

                composable(QReportRoutes.ARCHIVE) {
                    ArchiveScreen(
                        onNavigateToCheckUpDetail = { checkUpId ->
                            navController.navigate(QReportRoutes.checkupDetail(checkUpId))
                        }
                    )
                }

                composable(QReportRoutes.SETTINGS) {
                    SettingsScreen()
                }

                // ============================================================
                // SECONDARY DESTINATIONS
                // ============================================================

                // New Check-up Creation
                composable(QReportRoutes.CHECKUP_CREATE) {
                    NewCheckUpScreen(
                        onNavigateBack = {
                            navController.popBackStack()
                        },
                        onNavigateToCheckUpDetail = { checkUpId ->
                            // Navigate to detail and clear creation from stack
                            navController.navigate(QReportRoutes.checkupDetail(checkUpId)) {
                                popUpTo(QReportRoutes.CHECKUPS) {
                                    inclusive = false
                                }
                            }
                        }
                    )
                }

                // Check-up Detail
                composable(
                    route = QReportRoutes.CHECKUP_DETAIL,
                    arguments = listOf(
                        navArgument("checkUpId") {
                            type = NavType.StringType
                        }
                    )
                ) { backStackEntry ->
                    val checkUpId = backStackEntry.arguments?.getString("checkUpId") ?: ""

                    CheckUpDetailScreen(
                        checkUpId = checkUpId,
                        onNavigateBack = {
                            navController.popBackStack()
                        },
                        onNavigateToCamera = { checkItemId ->
                            navController.navigate(QReportRoutes.camera(checkItemId))
                        }
                    )
                }

                // Camera (Future implementation)
                composable(
                    route = QReportRoutes.CAMERA,
                    arguments = listOf(
                        navArgument("checkItemId") {
                            type = NavType.StringType
                        }
                    )
                ) { backStackEntry ->
                    val checkItemId = backStackEntry.arguments?.getString("checkItemId") ?: ""

                    // TODO: Implement CameraScreen in Phase 4
                    // For now, just show a placeholder
                    CameraPlaceholderScreen(
                        checkItemId = checkItemId,
                        onNavigateBack = {
                            navController.popBackStack()
                        }
                    )
                }
            }
        }

        // Bottom Navigation Bar (only for main destinations)
        val navBackStackEntry by navController.currentBackStackEntryAsState()
        val currentDestination = navBackStackEntry?.destination
        val currentRoute = currentDestination?.route

        // Hide bottom nav for secondary destinations
        val shouldShowBottomNav = when (currentRoute) {
            QReportRoutes.HOME,
            QReportRoutes.CHECKUPS,
            QReportRoutes.ARCHIVE,
            QReportRoutes.SETTINGS -> true
            else -> false
        }

        if (shouldShowBottomNav) {
            QReportBottomNavigation(
                navController = navController,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

/**
 * Bottom Navigation Bar personalizzata
 */
@Composable
fun QReportBottomNavigation(
    navController: NavHostController,
    modifier: Modifier = Modifier
) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    NavigationBar(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface
    ) {
        bottomNavDestinations.forEach { destination ->
            val isSelected = currentDestination?.hierarchy?.any {
                it.route == destination.route
            } == true

            NavigationBarItem(
                icon = {
                    Icon(
                        imageVector = if (isSelected) {
                            destination.selectedIcon
                        } else {
                            destination.unselectedIcon
                        },
                        contentDescription = destination.title
                    )
                },
                label = {
                    Text(
                        text = destination.title,
                        style = MaterialTheme.typography.labelSmall
                    )
                },
                selected = isSelected,
                onClick = {
                    if (destination.route == QReportRoutes.HOME) {
                        // Per Home, naviga senza salvare/ripristinare stato
                        navController.navigate(destination.route) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                inclusive = false
                            }
                            launchSingleTop = true
                            // NO saveState né restoreState per Home
                        }
                    } else {
                        navController.navigate(destination.route) {
                            // Pop up to the start destination of the graph to
                            // avoid building up a large stack of destinations
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            // Avoid multiple copies of the same destination when
                            // reselecting the same item
                            launchSingleTop = true
                            // Restore state when reselecting a previously selected item
                            restoreState = true
                        }
                    }
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = MaterialTheme.colorScheme.onSecondaryContainer,
                    selectedTextColor = MaterialTheme.colorScheme.onSurface,
                    indicatorColor = MaterialTheme.colorScheme.secondaryContainer,
                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            )
        }
    }
}

/**
 * Placeholder per CameraScreen (da implementare in Fase 4)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CameraPlaceholderScreen(
    checkItemId: String,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxSize()
    ) {
        TopAppBar(
            title = { Text("Fotocamera") },
            navigationIcon = {
                IconButton(onClick = onNavigateBack) {
                    Icon(
                        imageVector = Icons.Default.ArrowBackIosNew,
                        contentDescription = "Indietro"
                    )
                }
            }
        )

        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = androidx.compose.ui.Alignment.Center
        ) {
            Column(
                horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.CameraAlt,
                    contentDescription = null,
                    modifier = Modifier.size(64.dp),
                    tint = MaterialTheme.colorScheme.primary
                )

                Text(
                    text = "Fotocamera",
                    style = MaterialTheme.typography.titleLarge
                )

                Text(
                    text = "Funzionalità disponibile in Fase 4",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Text(
                    text = "Check Item ID: $checkItemId",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Button(onClick = onNavigateBack) {
                    Text("Torna Indietro")
                }
            }
        }
    }
}