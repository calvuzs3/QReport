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
import net.calvuz.qreport.presentation.screen.camera.CameraScreen
import net.calvuz.qreport.presentation.screen.client.client.ClientDetailScreen
import net.calvuz.qreport.presentation.screen.photo.PhotoGalleryScreen
import net.calvuz.qreport.presentation.screen.settings.SettingsScreen
import net.calvuz.qreport.presentation.screen.export.ExportOptionsScreen
import net.calvuz.qreport.presentation.screen.client.client.ClientListScreen
import net.calvuz.qreport.presentation.screen.client.client.ClientFormScreen
import net.calvuz.qreport.presentation.screen.client.contact.ContactDetailScreen
import net.calvuz.qreport.presentation.screen.client.contact.ContactFormScreen
import net.calvuz.qreport.presentation.screen.client.contact.ContactListScreen
import net.calvuz.qreport.presentation.screen.client.facility.FacilityDetailScreen
import net.calvuz.qreport.presentation.screen.client.facility.FacilityFormScreen
import net.calvuz.qreport.presentation.screen.client.facility.FacilityListScreen
import net.calvuz.qreport.presentation.screen.client.facilityisland.FacilityIslandFormScreen
import net.calvuz.qreport.presentation.screen.client.facilityisland.FacilityIslandDetailScreen
import net.calvuz.qreport.presentation.screen.client.facilityisland.FacilityIslandListScreen
import java.net.URLDecoder
import java.net.URLEncoder

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
 * - Clienti: Gestione clienti
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

    object Clients : QReportDestination(
        route = "clients",
        title = "Clienti",
        selectedIcon = Icons.Filled.Archive,
        unselectedIcon = Icons.Outlined.Archive
    )

    object CheckUps : QReportDestination(
        route = "checkups",
        title = "Check-up",
        selectedIcon = Icons.Filled.Assignment,
        unselectedIcon = Icons.Outlined.Assignment
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
    const val CLIENTS = "clients"
    const val CHECKUPS = "checkups"
    const val SETTINGS = "settings"

    // Secondary routes
    const val CHECKUP_CREATE = "checkup_create"
    const val CHECKUP_DETAIL = "checkup_detail/{checkUpId}"
    const val CAMERA = "camera/{checkItemId}"
    const val PHOTO_GALLERY = "photo_gallery/{checkItemId}"
    const val EXPORT_OPTIONS = "export_options/{checkUpId}"

    // Client management routes
    const val CLIENT_DETAIL = "client_detail/{clientId}"
    const val CLIENT_CREATE = "client_create"
    const val CLIENT_EDIT = "client_edit/{clientId}"

    // Contact routes
    const val CONTACT_DETAIL = "contact_detail/{contactId}"
    const val CONTACT_CREATE = "contact_form/{clientId}/{clientName}" // was create
    const val CONTACT_EDIT = "contact_form/{contactId}" // was edit
    const val CONTACT_LIST = "contacts/{clientId}/{clientName}"

    // Facility routes
    const val FACILITY_LIST = "facilities/{clientId}"
    const val FACILITY_DETAIL = "facility_detail/{clientId}/{facilityId}"
    const val FACILITY_CREATE = "facility_form/{clientId}"
    const val FACILITY_EDIT = "facility_form/{clientId}/{facilityId}"


    // FacilityIsland routes
    const val ISLAND_LIST = "islands/{facilityId}"
    const val ISLAND_DETAIL = "island_detail/{facilityId}/{islandId}"
    const val ISLAND_CREATE = "island_form/{facilityId}"
    const val ISLAND_EDIT = "island_form/{facilityId}/{islandId}"

    // Helper extension for URL encoding client names with spaces/special chars
    private fun String.encodeUrl(): String = URLEncoder.encode(this, "UTF-8")

    // Helper functions for CLIENTS
    fun clientDetail(clientId: String) = "client_detail/$clientId"
    fun clientEdit(clientId: String) = "client_edit/$clientId"

    // Helper functions for CONTACT
    fun contactDetailRoute(contactId: String) =
        "contact_detail/$contactId"
    fun contactCreateRoute(clientId: String, clientName: String) =
        "contact_form/$clientId/${clientName.encodeUrl()}"
    fun contactEditRoute(contactId: String) =
        "contact_form/$contactId" // Keep as is - needs lookup
    fun contactListRoute(clientId: String, clientName: String) =
        "contacts/$clientId/${clientName.encodeUrl()}"

    // Helper functions for FACILITY
    fun facilityListRoute(clientId: String) =
        "facilities/$clientId"
    fun facilityEditRoute(clientId: String, facilityId: String) =
        "facility_form/$clientId/$facilityId"
    fun facilityDetailRoute(clientId: String, savedFacilityId: String) =
        "facility_detail/$clientId/$savedFacilityId"
    fun facilityCreateRoute(clientId: String) =
        "facility_form/$clientId"
    // Helper functions for FACILITY ISLAND
    fun islandListRoute(facilityId: String) =
        "islands/$facilityId"
    fun islandDetailRoute(facilityId: String, islandId: String) =
        "island_detail/$facilityId/$islandId"

    fun islandCreateRoute(facilityId: String) = "island_form/$facilityId"
    fun islandEditRoute(facilityId: String, islandId: String) = "island_form/$facilityId/$islandId"

    // Helper functions for CHECK-UPs
    fun checkupDetail(checkUpId: String) = "checkup_detail/$checkUpId"
    fun camera(checkItemId: String) = "camera/$checkItemId"
    fun photoGallery(checkItemId: String) = "photo_gallery/$checkItemId"
    fun exportOptions(checkUpId: String) = "export_options/$checkUpId"

}

/**
 * Lista delle destinazioni bottom navigation
 */
val bottomNavDestinations = listOf(
    QReportDestination.Home,
    QReportDestination.Clients,
    QReportDestination.CheckUps,
    QReportDestination.Settings
)

/**
 * Composable principale per la navigazione
 */
@Composable
fun QReportNavigation(
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController(),
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
                        onNavigateToClients = {
                            navController.navigate(QReportRoutes.CLIENTS)
                        },
                        onNavigateToNewCheckUp = {
                            navController.navigate(QReportRoutes.CHECKUP_CREATE)
                        },
                        onNavigateToCheckUpDetail = { checkUpId ->
                            navController.navigate(QReportRoutes.checkupDetail(checkUpId))
                        }
                    )
                }

                composable(QReportRoutes.CLIENTS) {
                    ClientListScreen(
                        onNavigateToClientDetail = { clientId ->
                            navController.navigate(QReportRoutes.clientDetail(clientId))
                        },
                        onNavigateToEditClient = { clientId ->
                            navController.navigate(QReportRoutes.clientEdit(clientId))
                        },
                        onCreateNewClient = {
                            navController.navigate(QReportRoutes.CLIENT_CREATE)
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

                composable(QReportRoutes.SETTINGS) {
                    SettingsScreen()
                }

                // ============================================================
                // ✅ CHECK-UP MANAGEMENT DESTINATIONS
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
                        onNavigateToPhotoGallery = { checkItemId ->
                            navController.navigate(QReportRoutes.photoGallery(checkItemId))
                        },
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
                composable(QReportRoutes.CLIENTS) {
                    ClientListScreen(
                        onNavigateToClientDetail = { clientId ->
                            navController.navigate(QReportRoutes.clientDetail(clientId))
                        },
                        onNavigateToEditClient = { clientId ->
                            navController.navigate(QReportRoutes.clientEdit(clientId))
                        },
                        onCreateNewClient = {
                            navController.navigate(QReportRoutes.CLIENT_CREATE)
                        }
                    )
                }

                composable(
                    route = QReportRoutes.CLIENT_DETAIL,
                    arguments = listOf(
                        navArgument("clientId") {
                            type = NavType.StringType
                        }
                    )
                ) { backStackEntry ->
                    val clientId = backStackEntry.arguments?.getString("clientId") ?: ""
                    val clientName = backStackEntry.arguments?.getString("clientName") ?: ""

                    // Capture facilityId dall'outer scope BEFORE callbacks
                    val facilityId = backStackEntry.arguments?.getString("facilityId") ?: ""

                    ClientDetailScreen(
                        clientId = clientId,
                        clientName = clientName,
                        onNavigateBack = {
                            navController.popBackStack()
                        },
                        onDeleteClient = {
                            // ✅ %Navigate back to client list after delete
                            navController.navigate(QReportRoutes.CLIENTS) {
                                popUpTo(QReportRoutes.CLIENT_DETAIL) {
                                    inclusive = true  // Remove client detail from stack
                                }
                            }
                        },
                        onNavigateToEdit = { clientId ->
                            navController.navigate(QReportRoutes.clientEdit(clientId))
                        },
                        onNavigateToContactList = { clientId, clientName ->
                            navController.navigate(
                                QReportRoutes.contactListRoute(
                                    clientId,
                                    clientName
                                )
                            )
                        },
                        onNavigateToCreateContact = { clientId, clientName ->
                            navController.navigate(
                                QReportRoutes.contactCreateRoute(
                                    clientId,
                                    clientName
                                )
                            )
                        },
                        onNavigateToEditContact = { contactId ->
                            // ❌ TODO: Need clientId + clientName lookup from contactId
                            navController.navigate(QReportRoutes.contactEditRoute(contactId))
                        },
                        onNavigateToContactDetail = { contactId ->
                            navController.navigate(QReportRoutes.contactDetailRoute(contactId))
                        },
                        onNavigateToFacilityList = { clientId ->
                            navController.navigate(QReportRoutes.facilityListRoute(clientId))
                        },
                        onNavigateToCreateFacility = { clientId ->
                            navController.navigate(QReportRoutes.facilityCreateRoute(clientId))
                        },
                        onNavigateToEditFacility = { clientId, facilityId ->
                            navController.navigate(
                                QReportRoutes.facilityEditRoute(
                                    clientId,
                                    facilityId
                                )
                            )
                        },
                        onNavigateToFacilityDetail = { clientId, facilityId ->
                            navController.navigate(
                                QReportRoutes.facilityDetailRoute(
                                    clientId,
                                    facilityId
                                )
                            )
                        },
                        onNavigateToIslandDetail = { facilityId, islandId ->
                            navController.navigate(
                                QReportRoutes.islandDetailRoute(
                                    facilityId,
                                    islandId
                                )
                            )
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
                                popUpTo(QReportRoutes.CLIENTS) {
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
                                popUpTo(QReportRoutes.CLIENTS) {
                                    inclusive = false
                                }
                            }
                        }
                    )
                }

                // ============================================================
                // CONTACT
                // ============================================================

                // CREATE
                composable(
                    QReportRoutes.CONTACT_CREATE,
                    arguments = listOf(
                        navArgument("clientId") { type = NavType.StringType },
                        navArgument("clientName") { type = NavType.StringType }
                    )
                ) { backStackEntry ->
                    val clientId = backStackEntry.arguments?.getString("clientId") ?: ""
                    val clientName = backStackEntry.arguments?.getString("clientName")?.let {
                        URLDecoder.decode(it, "UTF-8")
                    } ?: "Cliente"

                    ContactFormScreen(
                        clientId = clientId,
                        clientName = clientName,
                        contactId = null,
                        onNavigateBack = { navController.popBackStack() },
                        onContactSaved = { contactId ->
                            navController.navigate(
                                QReportRoutes.contactDetailRoute(contactId)
                            )
                        }
                    )
//
//                    // Immediate redirect to unified form
//                    LaunchedEffect(clientId) {
//                        navController.navigate(
//                            QReportRoutes.contactFormRoute(
//                                clientId,
//                                clientName,
//                                null
//                            )
//                        ) {
//                            popUpTo(QReportRoutes.contactCreateRoute(clientId, clientName)) {
//                                inclusive = true
//                            }
//                        }
//                    }
                }

//                // FORM SCREEN (Unified Create/Edit)
//                composable(
//                    route = QReportRoutes.CONTACT_FORM,
//                    arguments = listOf(
//                        navArgument("clientId") { type = NavType.StringType },
//                        navArgument("clientName") { type = NavType.StringType },
//                        navArgument("contactId") {
//                            type = NavType.StringType
//                            nullable = true
//                            defaultValue = null
//                        }
//                    )
//                ) { backStackEntry ->
//                    val clientId = backStackEntry.arguments?.getString("clientId") ?: ""
//                    val contactId = backStackEntry.arguments?.getString("contactId") ?: ""
//                    val clientName = backStackEntry.arguments?.getString("clientName")?.let {
//                        URLDecoder.decode(it, "UTF-8")
//                    } ?: "Cliente" // Get clientName from route parameter
//
//                    ContactFormScreen(
//                        clientId = clientId,
//                        clientName = clientName,
//                        contactId = contactId,
//                        onNavigateBack = { navController.popBackStack() },
//                        onContactSaved = { contactId ->
//                            // Torna al ClientDetailScreen dopo il salvataggio
//                            navController.navigate(QReportRoutes.clientDetail(clientId)) {
//                                popUpTo(QReportRoutes.clientDetail(clientId)) { inclusive = true }
//                            }
//                        }
//                    )
//                }
//
//


                // EDIT
                composable(
                    route = QReportRoutes.CONTACT_EDIT,
                    arguments = listOf(
                        navArgument("contactId") { type = NavType.StringType }
                    )
                ) { backStackEntry ->
                    val contactId = backStackEntry.arguments?.getString("contactId") ?: ""

                    // TODO: Qui dovresti recuperare il clientId dal contactId usando un UseCase
                    // Per ora uso un placeholder

                    LaunchedEffect(contactId) {
                        // TODO: Get clientId from contactId using GetContactUseCase
                        val clientId = "placeholder"

                        navController.navigate(
                            QReportRoutes.contactEditRoute(contactId)
                        ) {
                            // "contact_form/$clientId?contactId=$contactId") {
                            popUpTo(QReportRoutes.contactEditRoute(contactId)) {
                                inclusive = true
                            }
                        }
                    }
                }

                // DETAIL
                composable(
                    QReportRoutes.CONTACT_DETAIL,
                    arguments = listOf(
                        navArgument("contactId") { type = NavType.StringType }
                    )
                ) { backStackEntry ->
                    val contactId = backStackEntry.arguments?.getString("contactId") ?: ""

                    ContactDetailScreen(
                        contactId = contactId,
                        onNavigateBack = { navController.popBackStack() },
                        onNavigateToEdit = { contactId ->
                            navController.navigate(QReportRoutes.contactEditRoute(contactId))
                        }
                    )
                }

                // LIST
                composable(
                    route = QReportRoutes.CONTACT_LIST,
                    arguments = listOf(
                        navArgument("clientId") { type = NavType.StringType },
                        navArgument("clientName") { type = NavType.StringType }
                    )
                ) { backStackEntry ->
                    val clientId = backStackEntry.arguments?.getString("clientId") ?: ""
                    val clientName = backStackEntry.arguments?.getString("clientName")?.let {
                        URLDecoder.decode(it, "UTF-8")
                    } ?: "Cliente"

                    ContactListScreen(
                        clientId = clientId,
                        clientName = clientName,
                        onNavigateBack = { navController.popBackStack() },
                        onNavigateToCreateContact = { clientId ->
                            navController.navigate(QReportRoutes.contactCreateRoute(clientId, clientName))
                        },
                        onNavigateToEditContact = { contactId ->
                            navController.navigate(
                                QReportRoutes.contactEditRoute(
                                    contactId
                                )
                            )
                        },
                        onNavigateToContactDetail = { contactId ->
                            navController.navigate(
                                QReportRoutes.contactDetailRoute(
                                    contactId
                                )
                            )
                        }
                    )
                }

                // ============================================================
                // FACILITY' SCREENS
                // ============================================================

                // 1. Route principale FacilityDetail - PASSA ENTRAMBI I PARAMETRI
                composable(QReportRoutes.FACILITY_DETAIL) { backStackEntry ->
                    val clientId = backStackEntry.arguments?.getString("clientId") ?: ""
                    val facilityId = backStackEntry.arguments?.getString("facilityId") ?: ""

                    FacilityDetailScreen(
                        facilityId = facilityId,
                        onNavigateBack = { navController.popBackStack() },
                        onNavigateToEdit = { facilityId ->
                            navController.navigate(
                                QReportRoutes.facilityEditRoute(
                                    clientId,
                                    facilityId
                                )
                            )
                        },
                        onNavigateToCreateIsland = { facilityId ->
                            navController.navigate(QReportRoutes.islandCreateRoute(facilityId))
                        },
                        onNavigateToEditIsland = { islandId ->
                            navController.navigate(
                                QReportRoutes.islandEditRoute(
                                    facilityId,
                                    islandId
                                )
                            )
                        },
                        onNavigateToIslandsList = { facilityId ->
                            navController.navigate(QReportRoutes.islandListRoute(facilityId))
                        },
                        onNavigateToIslandDetail = { islandId ->
                            navController.navigate(
                                QReportRoutes.islandDetailRoute(
                                    facilityId,
                                    islandId
                                )
                            )
                        }
                    )
                }

                // 2. Route per CREATE facility
                composable(QReportRoutes.FACILITY_CREATE) { backStackEntry ->
                    val clientId = backStackEntry.arguments?.getString("clientId") ?: ""

                    FacilityFormScreen(
                        clientId = clientId,
                        facilityId = null, // Create mode
                        onNavigateBack = { navController.popBackStack() },
                        onFacilitySaved = { savedFacilityId ->
                            navController.navigate(QReportRoutes.facilityDetailRoute(clientId, savedFacilityId)) {
                                popUpTo(QReportRoutes.FACILITY_LIST)
                            }
                        }
                    )
                }

                // 3. Route per EDIT facility
                composable(QReportRoutes.FACILITY_EDIT) { backStackEntry ->
                    val clientId = backStackEntry.arguments?.getString("clientId") ?: ""
                    val facilityId = backStackEntry.arguments?.getString("facilityId") ?: ""

                    FacilityFormScreen(
                        clientId = clientId,
                        facilityId = facilityId,
                        onNavigateBack = { navController.popBackStack() },
                        onFacilitySaved = { savedFacilityId ->
                            navController.navigate(
                                QReportRoutes.facilityDetailRoute(
                                    clientId,
                                    savedFacilityId
                                )
                            ) {
                                popUpTo(QReportRoutes.FACILITY_DETAIL) { inclusive = true }
                            }
                        }
                    )
                }

                // 4. FacilityList
                composable(QReportRoutes.FACILITY_LIST) { backStackEntry ->
                    val clientId = backStackEntry.arguments?.getString("clientId") ?: ""

                    FacilityListScreen(
                        clientId = clientId,
                        onNavigateToFacilityDetail = { facilityId ->
                            navController.navigate(
                                QReportRoutes.facilityDetailRoute(
                                    clientId,
                                    facilityId
                                )
                            )
                        },
                        onCreateNewFacility = {
                            navController.navigate(QReportRoutes.facilityCreateRoute(clientId))
                        },
                        onNavigateBack = { navController.popBackStack() }
                    )
                }


                // ============================================================
                // FACILITY ISLAND SCREENS - ✅ FIXED con GetFacilityByIdUseCase
                // ============================================================

                // 1. FacilityIslandForm - CREATE mode
                composable(
                    QReportRoutes.ISLAND_CREATE,
                    arguments = listOf(
                        navArgument("facilityId") { type = NavType.StringType }
                    )
                ) { backStackEntry ->
                    val facilityId = backStackEntry.arguments?.getString("facilityId") ?: ""

                    FacilityIslandFormScreen(
                        facilityId = facilityId,
                        facilityName = "", // ✅ SIMPLE: Let ViewModel handle facilityName lookup
                        islandId = null, // Create mode
                        onNavigateBack = { navController.popBackStack() },
                        onIslandSaved = { savedIslandId ->
                            navController.popBackStack() // Return to list
                        }
                    )
                }

                // 2. FacilityIslandForm - EDIT mode
                composable(
                    QReportRoutes.ISLAND_EDIT,
                    arguments = listOf(
                        navArgument("facilityId") { type = NavType.StringType },
                        navArgument("islandId") { type = NavType.StringType }
                    )
                ) { backStackEntry ->
                    val facilityId = backStackEntry.arguments?.getString("facilityId") ?: ""
                    val islandId = backStackEntry.arguments?.getString("islandId") ?: ""

                    FacilityIslandFormScreen(
                        facilityId = facilityId,
                        facilityName = "", // ✅ SIMPLE: Let ViewModel handle facilityName lookup
                        islandId = islandId, // Edit mode
                        onNavigateBack = { navController.popBackStack() },
                        onIslandSaved = { savedIslandId ->
                            navController.navigate(
                                QReportRoutes.islandDetailRoute(
                                    facilityId,
                                    savedIslandId
                                )
                            ) {
                                popUpTo(
                                    QReportRoutes.islandDetailRoute(
                                        facilityId,
                                        islandId
                                    )
                                ) { inclusive = true }
                            }
                        }
                    )
                }

                // 3. FacilityIslandDetail
                composable(
                    QReportRoutes.ISLAND_DETAIL,
                    arguments = listOf(
                        navArgument("facilityId") { type = NavType.StringType },
                        navArgument("islandId") { type = NavType.StringType }
                    )
                ) { backStackEntry ->
                    val facilityId = backStackEntry.arguments?.getString("facilityId") ?: ""
                    val islandId = backStackEntry.arguments?.getString("islandId") ?: ""

                    FacilityIslandDetailScreen(
                        facilityId = facilityId,
                        islandId = islandId,
                        onNavigateBack = { navController.popBackStack() },
                        onNavigateToEdit = { facilityId, islandId ->
                            navController.navigate(
                                QReportRoutes.islandEditRoute(
                                    facilityId,
                                    islandId
                                )
                            )
                        },
                        onNavigateToMaintenance = { islandId ->
                            // TODO: Navigate to maintenance screen when implemented
                        },
                        onIslandDeleted = {
                            navController.popBackStack()
                        }
                    )
                }

                // 4. FacilityIslandList
                composable(
                    QReportRoutes.ISLAND_LIST,
                    arguments = listOf(
                        navArgument("facilityId") { type = NavType.StringType }
                    )
                ) { backStackEntry ->
                    val facilityId = backStackEntry.arguments?.getString("facilityId") ?: ""

                    FacilityIslandListScreen(
                        facilityId = facilityId,
                        facilityName = "", // ✅ SIMPLE: Let ViewModel handle facilityName lookup
                        onNavigateToIslandDetail = { islandId ->
                            navController.navigate(
                                QReportRoutes.islandDetailRoute(
                                    facilityId,
                                    islandId
                                )
                            )
                        },
                        onCreateNewIsland = {
                            navController.navigate(QReportRoutes.islandCreateRoute(facilityId))
                        },
                        onNavigateBack = { navController.popBackStack() }
                    )
                }
            }
        } // END OF BOX

        // Bottom Navigation Bar (only for main destinations)
        val navBackStackEntry by navController.currentBackStackEntryAsState()
        val currentDestination = navBackStackEntry?.destination
        val currentRoute = currentDestination?.route

        // Hide bottom nav for secondary destinations
        val shouldShowBottomNav = when (currentRoute) {
            QReportRoutes.HOME,
            QReportRoutes.CLIENTS,
            QReportRoutes.CHECKUPS,
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
 * Bottom Navigation Bar
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
                            // Avoid multiple copies of the same destination when
                            // reselecting the same item
                            launchSingleTop = true
                            // NO saveState nor restoreState
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