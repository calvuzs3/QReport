package net.calvuz.qreport.presentation.navigation

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
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
import net.calvuz.qreport.presentation.screen.camera.CameraScreen
import net.calvuz.qreport.presentation.screen.client.client.ClientDetailScreen
import net.calvuz.qreport.presentation.screen.photo.PhotoGalleryScreen
import net.calvuz.qreport.presentation.screen.settings.SettingsScreen
import net.calvuz.qreport.presentation.screen.export.ExportOptionsScreen
import net.calvuz.qreport.presentation.screen.client.client.ClientListScreen
import net.calvuz.qreport.presentation.screen.client.client.ClientFormScreen
import net.calvuz.qreport.presentation.screen.client.contact.ContactFormScreen
import net.calvuz.qreport.presentation.screen.client.contact.ContactListScreen

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
    const val PHOTO_GALLERY = "photo_gallery/{checkItemId}"
    const val EXPORT_OPTIONS = "export_options/{checkUpId}"

    // ✅ NEW: Client management routes
    const val CLIENT_LIST = "clients"
    const val CLIENT_DETAIL = "client_detail/{clientId}"
    const val CLIENT_CREATE = "client_create"
    const val CLIENT_EDIT = "client_edit/{clientId}"

    // Contact routes - UNIFIED FORM
    const val CONTACT_FORM = "contact_form/{clientId}?contactId={contactId}"
    const val CONTACT_CREATE = "contact_create/{clientId}"  // Redirect to form
    const val CONTACT_EDIT = "contact_edit/{contactId}"     // Redirect to form
    const val CONTACT_LIST = "contacts/{clientId}"

    // Helper functions
    fun contactFormRoute(clientId: String, contactId: String? = null) =
        if (contactId != null) "contact_form/$clientId?contactId=$contactId"
        else "contact_form/$clientId"

    fun contactCreateRoute(clientId: String) = "contact_create/$clientId"
    fun contactEditRoute(contactId: String) = "contact_edit/$contactId"
    fun contactListRoute(clientId: String) = "contacts/$clientId"

    // Helper functions for parameters
    fun checkupDetail(checkUpId: String) = "checkup_detail/$checkUpId"
    fun camera(checkItemId: String) = "camera/$checkItemId"
    fun photoGallery(checkItemId: String) = "photo_gallery/$checkItemId"
    fun exportOptions(checkUpId: String) = "export_options/$checkUpId"

    // ✅ NEW: Client helper functions
    fun clientDetail(clientId: String) = "client_detail/$clientId"
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
                        },
                        // ✅ NEW: Navigation to client management
                        onNavigateToClients = {
                            navController.navigate(QReportRoutes.CLIENT_LIST)
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
                        },
                        // ✅ NUOVO: Aggiungi navigation per PhotoGallery
                        onNavigateToPhotoGallery = { checkItemId ->
                            navController.navigate(QReportRoutes.photoGallery(checkItemId))
                        },
                        // ✅ NUOVO: Aggiungi navigation per ExportOptions
                        onNavigateToExportOptions = { checkUpId ->
                            navController.navigate(QReportRoutes.exportOptions(checkUpId))
                        }
                    )
                }

                // Camera
                composable(
                    route = QReportRoutes.CAMERA,
                    arguments = listOf(
                        navArgument("checkItemId") {
                            type = NavType.StringType
                        }
                    )
                ) { backStackEntry ->
                    val checkItemId = backStackEntry.arguments?.getString("checkItemId") ?: ""

                    CameraScreen(
                        checkItemId = checkItemId,
                        onNavigateBack = { navController.popBackStack() },
                        onPhotoSaved = {
                            // Torna indietro dopo aver salvato la foto
                            navController.popBackStack()
                        }
                    )
                }

                // ✅ NUOVO: Aggiungi route per PhotoGallery
                composable(
                    route = QReportRoutes.PHOTO_GALLERY,
                    arguments = listOf(
                        navArgument("checkItemId") {
                            type = NavType.StringType
                        }
                    )
                ) { backStackEntry ->
                    val checkItemId = backStackEntry.arguments?.getString("checkItemId") ?: ""

                    PhotoGalleryScreen(
                        checkItemId = checkItemId,
                        onNavigateBack = {
                            navController.popBackStack()
                        },
                        onNavigateToCamera = {
                            navController.navigate(QReportRoutes.camera(checkItemId))
                        }
                    )
                }

                // ✅ NUOVO: Export Options Screen
                composable(
                    route = QReportRoutes.EXPORT_OPTIONS,
                    arguments = listOf(
                        navArgument("checkUpId") {
                            type = NavType.StringType
                        }
                    )
                ) { backStackEntry ->
                    val checkUpId = backStackEntry.arguments?.getString("checkUpId") ?: ""

                    ExportOptionsScreen(
                        checkUpId = checkUpId,
                        onNavigateBack = {
                            navController.popBackStack()
                        },
                        onExportStarted = {
                            // Torna al detail screen dopo l'export
                            navController.popBackStack()
                        }
                    )
                }

                // ============================================================
                // ✅ CLIENT MANAGEMENT DESTINATIONS
                // ============================================================

                // Client List Screen
                composable(QReportRoutes.CLIENT_LIST) {
                    ClientListScreen(
                        onNavigateToClientDetail = { clientId ->
                            navController.navigate(QReportRoutes.clientDetail(clientId))
                        },
                        onCreateNewClient = {
                            navController.navigate(QReportRoutes.CLIENT_CREATE)
                        }
                    )
                }

                // Client Detail Screen (placeholder - to be implemented)
                composable(
                    route = QReportRoutes.CLIENT_DETAIL,
                    arguments = listOf(
                        navArgument("clientId") {
                            type = NavType.StringType
                        }
                    )
                ) { backStackEntry ->
                    val clientId = backStackEntry.arguments?.getString("clientId") ?: ""
                    ClientDetailScreen(
                        clientId = clientId,
                        onNavigateBack = {
                            navController.popBackStack()
                        },
                        onNavigateToEdit = { editClientId ->
                            navController.navigate("client_edit/$editClientId")
                        },
                        onNavigateToContactList = { clientId, clientName ->
                            navController.navigate("contacts/$clientId")
                        },
                        onNavigateToCreateContact = { clientId ->
                            navController.navigate("contact_create/$clientId")
                        },
                        onNavigateToEditContact = { contactId ->
                            navController.navigate("contact_edit/$contactId")
                        },
                        onNavigateToFacilityDetail = { facilityId ->
                            // TODO: Implementare quando FacilityDetailScreen sarà disponibile
                            // navController.navigate("facility_detail/$facilityId")
                        },
                        onNavigateToIslandDetail = { islandId ->
                            // TODO: Implementare quando IslandDetailScreen sarà disponibile
                            // navController.navigate("island_detail/$islandId")
                        }
                    )
                }

                // ✅ Client Create/Edit Screen
                composable(QReportRoutes.CLIENT_CREATE) {
                    ClientFormScreen(
                        onNavigateBack = {
                            navController.popBackStack()
                        },
                        onNavigateToClientDetail = { clientId ->
                            navController.navigate(QReportRoutes.clientDetail(clientId)) {
                                popUpTo(QReportRoutes.CLIENT_LIST) {
                                    inclusive = false
                                }
                            }
                        }
                    )
                }

                // ✅ Client Edit Screen
                composable(
                    route = QReportRoutes.CLIENT_EDIT,
                    arguments = listOf(
                        navArgument("clientId") {
                            type = NavType.StringType
                        }
                    )
                ) { backStackEntry ->
                    val clientId = backStackEntry.arguments?.getString("clientId") ?: ""

                    ClientFormScreen(
                        clientId = clientId,
                        onNavigateBack = {
                            navController.popBackStack()
                        },
                        onNavigateToClientDetail = { savedClientId ->
                            navController.navigate(QReportRoutes.clientDetail(savedClientId)) {
                                popUpTo(QReportRoutes.CLIENT_LIST) {
                                    inclusive = false
                                }
                            }
                        }
                    )
                }

                // ============================================================
                // CONTACT FORM SCREEN (Unified Create/Edit)
                // ============================================================
                composable(
                    route = "contact_form/{clientId}?contactId={contactId}",
                    arguments = listOf(
                        navArgument("clientId") { type = NavType.StringType },
                        navArgument("contactId") {
                            type = NavType.StringType
                            nullable = true
                            defaultValue = null
                        }
                    )
                ) { backStackEntry ->
                    val clientId = backStackEntry.arguments?.getString("clientId") ?: ""
                    val contactId = backStackEntry.arguments?.getString("contactId")

                    // TODO: Recupera clientName dal ClientDetailViewModel o da un repository
                    // Per ora uso un placeholder - dovresti passarlo dal ClientDetailScreen
                    val clientName = "Cliente" // TODO: Get from ViewModel or pass through route

                    ContactFormScreen(
                        clientId = clientId,
                        clientName = clientName,
                        contactId = contactId,
                        onNavigateBack = { navController.popBackStack() },
                        onContactSaved = { contactId ->
                            // Torna al ClientDetailScreen dopo il salvataggio
                            navController.navigate("client_detail/$clientId") {
                                popUpTo("client_detail/$clientId") { inclusive = true }
                            }
                        }
                    )
                }

                // ============================================================
                // CONTACT CREATE - Redirect to unified form
                // ============================================================
                composable(
                    route = "contact_create/{clientId}",
                    arguments = listOf(navArgument("clientId") { type = NavType.StringType })
                ) { backStackEntry ->
                    val clientId = backStackEntry.arguments?.getString("clientId") ?: ""

                    // Immediate redirect to unified form
                    LaunchedEffect(clientId) {
                        navController.navigate("contact_form/$clientId") {
                            popUpTo("contact_create/$clientId") { inclusive = true }
                        }
                    }
                }

                // ============================================================
                // CONTACT EDIT - Redirect to unified form
                // ============================================================
                composable(
                    route = "contact_edit/{contactId}",
                    arguments = listOf(navArgument("contactId") { type = NavType.StringType })
                ) { backStackEntry ->
                    val contactId = backStackEntry.arguments?.getString("contactId") ?: ""

                    // TODO: Qui dovresti recuperare il clientId dal contactId usando un UseCase
                    // Per ora uso un placeholder

                    LaunchedEffect(contactId) {
                        // TODO: Get clientId from contactId using GetContactUseCase
                        val clientId = "placeholder" // TODO: Implement proper lookup

                        navController.navigate("contact_form/$clientId?contactId=$contactId") {
                            popUpTo("contact_edit/$contactId") { inclusive = true }
                        }
                    }
                }

                // ============================================================
                // CONTACT LIST SCREEN
                // ============================================================
                composable(
                    route = "contacts/{clientId}",
                    arguments = listOf(navArgument("clientId") { type = NavType.StringType })
                ) { backStackEntry ->
                    val clientId = backStackEntry.arguments?.getString("clientId") ?: ""

                    // TODO: Recupera clientName dal ClientDetailViewModel o repository
                    val clientName = "Cliente" // TODO: Get from ViewModel

                    ContactListScreen(
                        clientId = clientId,
                        clientName = clientName,
                        onNavigateBack = { navController.popBackStack() },
                        onNavigateToCreateContact = { clientId ->
                            navController.navigate("contact_form/$clientId")
                        },
                        onNavigateToEditContact = { contactId ->
                            // TODO: Get clientId from contactId
                            val clientId = clientId // Use current clientId for now
                            navController.navigate("contact_form/$clientId?contactId=$contactId")
                        }
                    )
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
                // ✅ UPDATED: Show bottom nav for main client list, hide for detail/create
                QReportRoutes.CLIENT_LIST -> true
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