package net.calvuz.qreport.app.navigation

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Assignment
import androidx.compose.material.icons.automirrored.outlined.Assignment
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
import net.calvuz.qreport.backup.presentation.ui.BackupScreen
import net.calvuz.qreport.app.app.presentation.ui.home.HomeScreen
import net.calvuz.qreport.checkup.presentation.CheckUpListScreen
import net.calvuz.qreport.checkup.presentation.NewCheckUpScreen
import net.calvuz.qreport.checkup.presentation.CheckUpDetailScreen
import net.calvuz.qreport.photo.presentation.ui.CameraScreen
import net.calvuz.qreport.client.client.presentation.ui.ClientDetailScreen
import net.calvuz.qreport.photo.presentation.ui.PhotoGalleryScreen
import net.calvuz.qreport.settings.presentation.ui.SettingsScreen
import net.calvuz.qreport.export.presentation.ui.ExportOptionsScreen
import net.calvuz.qreport.client.client.presentation.ui.ClientListScreen
import net.calvuz.qreport.client.client.presentation.ui.ClientFormScreen
import net.calvuz.qreport.client.contact.presentation.ui.ContactFormScreen
import net.calvuz.qreport.client.contact.presentation.ui.ContactListScreen
import net.calvuz.qreport.client.facility.presentation.ui.FacilityDetailScreen
import net.calvuz.qreport.client.facility.presentation.ui.FacilityFormScreen
import net.calvuz.qreport.client.facility.presentation.ui.FacilityListScreen
import net.calvuz.qreport.client.island.presentation.ui.IslandFormScreen
import net.calvuz.qreport.client.island.presentation.ui.IslandDetailScreen
import net.calvuz.qreport.client.island.presentation.ui.IslandListScreen
import net.calvuz.qreport.photo.presentation.ui.PhotoImportPreviewScreen
import net.calvuz.qreport.settings.presentation.ui.TechnicianSettingsScreen
import timber.log.Timber
import java.net.URLDecoder
import java.net.URLEncoder
import androidx.core.net.toUri
import net.calvuz.qreport.R
import net.calvuz.qreport.app.error.presentation.UiText
import net.calvuz.qreport.client.contract.presentation.ui.ContractFormScreen
import net.calvuz.qreport.client.contract.presentation.ui.ContractListScreen
import net.calvuz.qreport.ti.presentation.ui.TechnicalInterventionFormScreen
import net.calvuz.qreport.ti.presentation.ui.TechnicalInterventionListScreen

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
    val title: UiText,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
) {
    object Home : QReportDestination(
        route = QReportRoutes.HOME,
        title = UiText.StringResource(R.string.route_home_title),
        selectedIcon = Icons.Filled.Home,
        unselectedIcon = Icons.Outlined.Home
    )

    object Clients : QReportDestination(
        route = QReportRoutes.CLIENTS,
        title = UiText.StringResource(R.string.route_clients_title),  //"Clienti",
        selectedIcon = Icons.Filled.Archive,
        unselectedIcon = Icons.Outlined.Archive
    )

    object CheckUps : QReportDestination(
        route = QReportRoutes.CHECKUPS,
        title = UiText.StringResource(R.string.route_checkups_title), //"Check-up",
        selectedIcon = Icons.AutoMirrored.Filled.Assignment,
        unselectedIcon = Icons.AutoMirrored.Outlined.Assignment
    )

    object Settings : QReportDestination(
        route = QReportRoutes.SETTINGS,
        title = UiText.StringResource(R.string.route_settings_title), //"Impostazioni",
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
    const val TI = "tis"

    // Intervention
    const val TI_CREATE = "ti_form"
    const val TI_EDIT = "ti_form/{interventionId}"

    // Settings
    const val TECHNICIAN_SETTINGS = "technician_settings"

    // Check up management routes
    const val CHECKUP_CREATE = "checkup_create"
    const val CHECKUP_DETAIL = "checkup_detail/{checkUpId}"
    const val CAMERA = "camera/{checkItemId}"
    const val PHOTO_GALLERY = "photo_gallery/{checkItemId}"
    const val PHOTO_IMPORT_PREVIEW = "photo_import_preview/{checkItemId}?photoUri={photoUri}"
    const val EXPORT_OPTIONS = "export_options/{checkUpId}"

    // Client management routes
    const val CLIENT_DETAIL = "client_detail/{clientId}/{clientName}"
    const val CLIENT_CREATE = "client_create"
    const val CLIENT_EDIT = "client_edit/{clientId}"

    // Contact routes
    const val CONTACT_LIST = "contacts/{clientId}/{clientName}"
    const val CONTACT_CREATE = "contact_form/{clientId}/{clientName}"
    const val CONTACT_EDIT = "contact_form/{clientId}/{clientName}/{contactId}"

    // Contract routes
    const val CONTRACT_LIST = "contracts/{clientId}/{clientName}"
    const val CONTRACT_CREATE = "contract_form/{clientId}/{clientName}"
    const val CONTRACT_EDIT = "contract_form/{clientId}/{clientName}/{contractId}"

    // Facility routes
    const val FACILITY_LIST = "facilities/{clientId}"
    const val FACILITY_DETAIL = "facility_detail/{clientId}/{facilityId}"
    const val FACILITY_CREATE = "facility_form/{clientId}"

    const val FACILITY_EDIT = "facility_form/{clientId}/{facilityId}"

    // Island routes
    const val ISLAND_LIST = "islands/{facilityId}"
    const val ISLAND_DETAIL = "island_detail/{facilityId}/{islandId}"
    const val ISLAND_CREATE = "island_form/{facilityId}"

    const val ISLAND_EDIT = "island_form/{facilityId}/{islandId}"

    // Backup
    const val BACKUP = "backup"


    // Helper extension for URL encoding client names with spaces/special chars
    private fun String.encodeUrl(): String = URLEncoder.encode(this, "UTF-8")


    // Helpers for TI
    fun ti() = "tis"
    fun tiCreateRoute() = "ti_form"
    fun tiEditRoute(interventionId: String) = "ti_form/$interventionId"

    // Helper functions for CLIENTS

    fun clientDetail(clientId: String, clientName: String) =
        "client_detail/$clientId/$clientName"

    fun clientEdit(clientId: String) =
        "client_edit/$clientId"

    // Helper functions for CONTACT

    fun contactCreateRoute(clientId: String, clientName: String) =
        "contact_form/$clientId/${clientName.encodeUrl()}"

    fun contactEditRoute(clientId: String, clientName: String, contactId: String?) =
        "contact_form/$clientId/${clientName.encodeUrl()}/$contactId"

    fun contactListRoute(clientId: String, clientName: String) =
        "contacts/$clientId/${clientName.encodeUrl()}"

    // Helper for CONTRACTS

    fun contractListRoute(clientId: String, clientName: String) =
        "contracts/$clientId/${clientName.encodeUrl()}"

    fun contractCreateRoute(clientId: String, clientName: String) =
        "contract_form/$clientId/${clientName.encodeUrl()}"

    fun contractEditRoute(clientId: String, clientName: String, contractId: String?) =
        "contract_form/$clientId/${clientName.encodeUrl()}/$contractId"

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

    fun islandListRoute(facilityId: String) = "islands/$facilityId"
    fun islandDetailRoute(facilityId: String, islandId: String) =
        "island_detail/$facilityId/$islandId"

    fun islandCreateRoute(facilityId: String) = "island_form/$facilityId"
    fun islandEditRoute(facilityId: String, islandId: String) = "island_form/$facilityId/$islandId"

    // Helper functions for CHECK-UPs

    fun checkUpCreateRoute(clientId: String? = null) = "checkup_create"
    fun checkupDetail(checkUpId: String) = "checkup_detail/$checkUpId"
    fun camera(checkItemId: String) = "camera/$checkItemId"
    fun photoGallery(checkItemId: String) = "photo_gallery/$checkItemId"
    fun photoImportCreateRoute(checkItemId: String, photoUri: String) =
        "photo_import_preview/$checkItemId?photoUri=$photoUri"

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
                // ✅ MAIN DESTINATIONS (Bottom Navigation)
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
                        onNavigateToTechnicalInterventions = {
                            navController.navigate(QReportRoutes.TI)
                        },
                        onNavigateToCheckUpDetail = { checkUpId ->
                            navController.navigate(QReportRoutes.checkupDetail(checkUpId))
                        },
                    )
                }

                composable(QReportRoutes.CLIENTS) {
                    ClientListScreen(
                        onNavigateToClientDetail = { clientId, clientName ->
                            navController.navigate(
                                QReportRoutes.clientDetail(
                                    clientId,
                                    clientName
                                )
                            )
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
                        onNavigateToEditCheckUp = { checkUpId ->
                            navController.navigate(QReportRoutes.checkupDetail(checkUpId))
                        },
                        onCreateNewCheckUp = {
                            navController.navigate(QReportRoutes.CHECKUP_CREATE)
                        }
                    )
                }

                composable(QReportRoutes.SETTINGS) {
                    SettingsScreen(
                        onNavigateToBackup = {
                            navController.navigate(QReportRoutes.BACKUP)
                        },
                        onNavigateToTechnicianSettings = {
                            navController.navigate(QReportRoutes.TECHNICIAN_SETTINGS)
                        }
                    )
                }

                composable(QReportRoutes.TI) {
                    TechnicalInterventionListScreen(
                        onNavigateToCreateIntervention = {
                            navController.navigate(QReportRoutes.tiCreateRoute())
                        },
                        onNavigateToEditIntervention = {interventionId ->
                            navController.navigate(QReportRoutes.tiEditRoute(interventionId))
                        },
                        onNavigateBack = {
                            navController.navigate(QReportRoutes.HOME) {
                                popUpTo(QReportRoutes.HOME) { inclusive = true }
                            }
                        }
                    )
                }

                // ============================================================
                // ✅ INTERVENTION DESTINATIONS
                // ============================================================

                composable(QReportRoutes.TI_CREATE) {
                    TechnicalInterventionFormScreen(
                        onNavigateBack = {
                            navController.popBackStack()
                        },
                        onInterventionSaved = {
                            navController.popBackStack()
                        }
                    )
                }

                composable(
                    route = QReportRoutes.TI_EDIT,
                    arguments = listOf(
                        navArgument("interventionId") {
                            type = NavType.StringType
                        }
                    )
                ) {
                    val tiID = it.arguments?.getString("interventionId") ?: ""

                    TechnicalInterventionFormScreen(
                        interventionId = tiID,
                        onNavigateBack = {
                            navController.popBackStack()
                        },
                        onInterventionSaved = {
                            navController.popBackStack()
                        },
                    )
                }

                // ============================================================
                // ✅ SETTINGS DESTINATIONS
                // ============================================================

                composable(QReportRoutes.TECHNICIAN_SETTINGS) {
                    TechnicianSettingsScreen(
                        onNavigateBack = {
                            navController.popBackStack()
                        }
                    )
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
                        },
                        onNavigateToDeleteCheckUp = {
                            // Navigate back to checkup list after delete
                            navController.navigate(QReportRoutes.CHECKUPS) {
                                popUpTo(QReportRoutes.checkupDetail(checkUpId)) {
                                    inclusive = true  // Remove checkup detail from stack
                                }
                            }
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
                        },
                        onNavigateToPhotoImport = { photoUri ->
                            navController.navigate(
                                QReportRoutes.photoImportCreateRoute(
                                    checkItemId,
                                    photoUri.toString()
                                )
                            )
                        }
                    )
                }

                // Photo Import Preview Screen
                composable(
                    route = QReportRoutes.PHOTO_IMPORT_PREVIEW,
                    arguments = listOf(
                        navArgument("checkItemId") { type = NavType.StringType },
                        navArgument("photoUri") { type = NavType.StringType }
                    )
                ) { backStackEntry ->
                    val checkItemId =
                        backStackEntry.arguments?.getString("checkItemId") ?: return@composable
                    val photoUriString =
                        backStackEntry.arguments?.getString("photoUri") ?: return@composable

                    val photoUri = photoUriString.toUri()

                    PhotoImportPreviewScreen(
                        checkItemId = checkItemId,
                        photoUri = photoUri,
                        onNavigateBack = {
                            navController.popBackStack()
                        },
                        onImportSuccess = {
                            // Torna alla gallery dopo import successo
                            navController.popBackStack()
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
                        }
                    )
                }

                // ============================================================
                // ✅ CLIENT MANAGEMENT DESTINATIONS
                // ============================================================

                composable(
                    route = QReportRoutes.CLIENT_DETAIL,
                    arguments = listOf(
                        navArgument("clientId") { type = NavType.StringType },
                        navArgument("clientName") { type = NavType.StringType }
                    )
                ) { backStackEntry ->
                    val clientId = backStackEntry.arguments?.getString("clientId") ?: ""
                    val clientName = backStackEntry.arguments?.getString("clientName") ?: ""

                    ClientDetailScreen(
                        clientId = clientId,
                        onNavigateBack = {
                            navController.popBackStack()
                        },
                        onDeleteClient = {
                            // Navigate back to client list after delete
                            navController.navigate(QReportRoutes.CLIENTS) {
                                popUpTo(QReportRoutes.clientDetail(clientId, clientName)) {
                                    inclusive = true  // Remove client detail from stack
                                }
                            }
                        },
                        onNavigateToEdit = { clientId ->
                            navController.navigate(QReportRoutes.clientEdit(clientId))
                        },
                        onNavigateToContactList = { clientId, clientName ->
                            navController.navigate(
                                QReportRoutes.contactListRoute(clientId, clientName)
                            )
                        },
                        onNavigateToCreateContact = { clientId, clientName ->
                            navController.navigate(
                                QReportRoutes.contactCreateRoute(clientId, clientName)
                            )
                        },
                        onNavigateToEditContact = { contactId ->
                            navController.navigate(
                                QReportRoutes.contactEditRoute(clientId, clientName, contactId)
                            )
                        },
                        onNavigateToContactDetail = { },
                        onNavigateToContractList = { clientId, clientName ->
                            navController.navigate(
                                QReportRoutes.contractListRoute(clientId, clientName)
                            )
                        },
                        onNavigateToCreateContract = { clientId, clientName ->
                            navController.navigate(
                                QReportRoutes.contractCreateRoute(clientId, clientName)
                            )
                        },
                        onNavigateToEditContract = { contractId ->
                            navController.navigate(
                                QReportRoutes.contractEditRoute(clientId, clientName, contractId)
                            )
                        },
                        onNavigateToFacilityList = { clientId ->
                            navController.navigate(QReportRoutes.facilityListRoute(clientId))
                        },
                        onNavigateToCreateFacility = { clientId ->
                            navController.navigate(QReportRoutes.facilityCreateRoute(clientId))
                        },
                        onNavigateToEditFacility = { clientId, facilityId ->
                            navController.navigate(
                                QReportRoutes.facilityEditRoute(clientId, facilityId)
                            )
                        },
                        onNavigateToFacilityDetail = { clientId, facilityId ->
                            navController.navigate(
                                QReportRoutes.facilityDetailRoute(clientId, facilityId)
                            )
                        },
                        onNavigateToIslandDetail = { facilityId, islandId ->
                            navController.navigate(
                                QReportRoutes.islandDetailRoute(facilityId, islandId)
                            )
                        },
                        onNavigateToCreateCheckUp = { clientId ->
                            navController.navigate(
                                QReportRoutes.checkUpCreateRoute(clientId)
                            )
                        },
                    )
                }

                // ✅ Client Create/Edit Screen
                composable(QReportRoutes.CLIENT_CREATE) {
                    ClientFormScreen(
                        onNavigateBack = {
                            navController.popBackStack()
                        },
                        onNavigateToClientDetail = { clientId, clientName ->
                            navController.navigate(
                                QReportRoutes.clientDetail(
                                    clientId, clientName = ""
                                )
                            ) {
                                popUpTo(QReportRoutes.CLIENTS) {
                                    inclusive = false
                                }
                            }
                        },
                        onClientSaved = { clientId, clientName ->
                            navController.navigate(
                                QReportRoutes.clientDetail(clientId, clientName)
                            ) {
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
                        onNavigateToClientDetail = { savedClientId, savedClientName ->
                            navController.navigate(
                                QReportRoutes.clientDetail(savedClientId, savedClientName)
                            ) {
                                popUpTo(QReportRoutes.CLIENTS) {
                                    inclusive = false
                                }
                            }
                        },
                        onClientSaved = { clientId, clientName ->
                            navController.navigate(
                                QReportRoutes.clientDetail(clientId, clientName)
                            ) {
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
                    } ?: "Customer"

                    ContactFormScreen(
                        clientId = clientId,
                        clientName = clientName,
                        contactId = null,
                        onNavigateBack = { navController.popBackStack() },
                        onContactSaved = { newContactId ->
                            if (newContactId.isNotBlank()) {
                                navController.navigate(
                                    QReportRoutes.contactListRoute(
                                        clientId,
                                        clientName,
                                        //newContactId
                                    )
                                ) {
                                    popUpTo(QReportRoutes.CLIENTS) {
                                        inclusive = false
                                    }
                                }
                            } else {
                                Timber.e("🔥 COMPOSABLE: ContactId is empty, cannot navigate!")
                            }
                        }
                    )
                }

                // FORM SCREEN (Unified Create/Edit)
                composable(
                    route = QReportRoutes.CONTACT_EDIT,
                    arguments = listOf(
                        navArgument("clientId") { type = NavType.StringType },
                        navArgument("clientName") { type = NavType.StringType },
                        navArgument("contactId") {
                            type = NavType.StringType
                            nullable = true
                            defaultValue = null
                        }
                    )
                ) { backStackEntry ->
                    val clientId = backStackEntry.arguments?.getString("clientId") ?: ""
                    val contactId = backStackEntry.arguments?.getString("contactId") ?: ""
                    val clientName = backStackEntry.arguments?.getString("clientName")?.let {
                        URLDecoder.decode(it, "UTF-8")
                    } ?: "Customer" // Get clientName from route parameter

                    ContactFormScreen(
                        clientId = clientId,
                        clientName = clientName,
                        contactId = contactId,
                        onNavigateBack = { navController.popBackStack() },
                        onContactSaved = { contactId ->
                            // Torna indietro
                            navController.popBackStack()
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
                            navController.navigate(
                                QReportRoutes.contactCreateRoute(clientId, clientName)
                            )
                        },
                        onNavigateToEditContact = { contactId ->
                            navController.navigate(
                                QReportRoutes.contactEditRoute(
                                    clientId, clientName, contactId
                                )
                            )
                        },
                        onNavigateToContactDetail = {
                        }
                    )
                }

                // ============================================================
                // ✅ CONTRACT DESTINATIONS
                // ============================================================

                composable(
                    route = QReportRoutes.CONTRACT_LIST,
                    arguments = listOf(
                        navArgument("clientId") { type = NavType.StringType },
                        navArgument("clientName") { type = NavType.StringType }
                    )
                ) { backStackEntry ->
                    val clientId = backStackEntry.arguments?.getString("clientId") ?: ""
                    val clientName = backStackEntry.arguments?.getString("clientName") ?: ""
                    ContractListScreen(
                        onNavigateToCreateContract = { contractId ->
                            navController.navigate(
                                QReportRoutes.contractCreateRoute(clientId, clientName)
                            )
                        },
                        clientId = clientId,
                        clientName = clientName,
                        onNavigateBack = { navController.popBackStack() },
                        onNavigateToEditContract = { contractId ->
                            navController.navigate(
                                QReportRoutes.contractEditRoute(clientId, clientName, contractId)
                            )
                        },
                        onNavigateToContractDetail = {}
                    )
                }

                // CREATE
                composable(
                    QReportRoutes.CONTRACT_CREATE,
                    arguments = listOf(
                        navArgument("clientId") { type = NavType.StringType },
                        navArgument("clientName") { type = NavType.StringType }
                    )
                ) { backStackEntry ->
                    val clientId = backStackEntry.arguments?.getString("clientId") ?: ""
                    val clientName = backStackEntry.arguments?.getString("clientName")?.let {
                        URLDecoder.decode(it, "UTF-8")
                    } ?: "Customer"

                    ContractFormScreen(
                        clientId = clientId,
                        clientName = clientName,
                        contractId = null,
                        onNavigateBack = { navController.popBackStack() },
                        onContractSaved = { newId ->
                            if (newId.isNotBlank()) {
                                navController.navigate(
                                    QReportRoutes.contractListRoute(
                                        // TODO: eval a detail screen
                                        clientId,
                                        clientName,
                                        //newId
                                    )
                                ) {
                                    popUpTo(QReportRoutes.CLIENTS) {
                                        inclusive = false
                                    }
                                }
                            } else {
                                Timber.e("🔥 COMPOSABLE: ContractId is empty, cannot navigate!")
                            }
                        }
                    )
                }

                // FORM SCREEN (Unified Create/Edit)
                composable(
                    route = QReportRoutes.CONTRACT_EDIT,
                    arguments = listOf(
                        navArgument("clientId") { type = NavType.StringType },
                        navArgument("clientName") { type = NavType.StringType },
                        navArgument("contractId") {
                            type = NavType.StringType
                            nullable = true
                            defaultValue = null
                        }
                    )
                ) { backStackEntry ->
                    val contractId = backStackEntry.arguments?.getString("contractId") ?: ""
                    val clientId = backStackEntry.arguments?.getString("clientId") ?: ""
                    val clientName = backStackEntry.arguments?.getString("clientName")?.let {
                        URLDecoder.decode(it, "UTF-8")
                    } ?: "Customer" // Get clientName from route parameter

                    ContractFormScreen(
                        clientId = clientId,
                        clientName = clientName,
                        contractId = contractId,
                        onNavigateBack = { navController.popBackStack() },
                        onContractSaved = { newId ->
                            if (newId.isNotBlank()) {
                                navController.navigate(
                                    QReportRoutes.contractListRoute(
                                        clientId,
                                        clientName,
                                        //newId
                                    )
                                ) {
                                    popUpTo(QReportRoutes.CLIENTS) {
                                        inclusive = false
                                    }
                                }
                            } else {
                                Timber.e("🔥 COMPOSABLE: ContractId is empty, cannot navigate!")
                            }
                        }
                    )
                }

                // ============================================================
                // FACILITY' SCREENS
                // ============================================================

                // 1. DETAIL
                composable(QReportRoutes.FACILITY_DETAIL) { backStackEntry ->
                    val clientId = backStackEntry.arguments?.getString("clientId") ?: ""
                    val facilityId = backStackEntry.arguments?.getString("facilityId") ?: ""

                    FacilityDetailScreen(
                        facilityId = facilityId,
                        onNavigateBack = { navController.popBackStack() },
                        onNavigateToEdit = { facilityId ->
                            navController.navigate(
                                QReportRoutes.facilityEditRoute(clientId, facilityId)
                            )
                        },
                        onNavigateToCreateIsland = { facilityId ->
                            navController.navigate(QReportRoutes.islandCreateRoute(facilityId))
                        },
                        onNavigateToEditIsland = { islandId ->
                            navController.navigate(
                                QReportRoutes.islandEditRoute(facilityId, islandId)
                            )
                        },
                        onNavigateToIslandsList = { facilityId ->
                            navController.navigate(QReportRoutes.islandListRoute(facilityId))
                        },
                        onNavigateToIslandDetail = { islandId ->
                            navController.navigate(
                                QReportRoutes.islandDetailRoute(facilityId, islandId)
                            )
                        },
                        onDeleted = { navController.popBackStack() }
                    )
                }

                // 2. CREATE
                composable(QReportRoutes.FACILITY_CREATE) { backStackEntry ->
                    val clientId = backStackEntry.arguments?.getString("clientId") ?: ""

                    FacilityFormScreen(
                        clientId = clientId,
                        facilityId = null, // Create mode
                        onNavigateBack = { navController.popBackStack() },
                        onFacilitySaved = { savedFacilityId ->
                            navController.popBackStack()
//                            navController.navigate(
//                                QReportRoutes.facilityDetailRoute(
//                                    clientId,
//                                    savedFacilityId
//                                )
//                            ) {
//                                // TODO use the correct route, ClientDetail, once the clientName has been passed
//                                popUpTo(QReportRoutes.CLIENTS) { inclusive = false }
//                            }
                        }
                    )
                }

                // 3. EDIT
                composable(QReportRoutes.FACILITY_EDIT) { backStackEntry ->
                    val clientId = backStackEntry.arguments?.getString("clientId") ?: ""
                    val facilityId = backStackEntry.arguments?.getString("facilityId") ?: ""

                    FacilityFormScreen(
                        clientId = clientId,
                        facilityId = facilityId,
                        onNavigateBack = { navController.popBackStack() },
                        onFacilitySaved = { savedFacilityId ->
                            navController.popBackStack()
//                            navController.navigate(
//                                QReportRoutes.facilityDetailRoute(
//                                    clientId,
//                                    savedFacilityId
//                                )
//                            ) {
//                                // TODO use the correct route, ClientDetail, once the clientName has been passed
//                                popUpTo(QReportRoutes.CLIENTS) { inclusive = false }
//                            }
                        }
                    )
                }

                // 4. LIST
                composable(QReportRoutes.FACILITY_LIST) { backStackEntry ->
                    val clientId = backStackEntry.arguments?.getString("clientId") ?: ""

                    FacilityListScreen(
                        clientId = clientId,
                        onNavigateToFacilityDetail = { facilityId ->
                            navController.navigate(
                                QReportRoutes.facilityDetailRoute(clientId, facilityId)
                            )
                        },
                        onCreateNewFacility = {
                            navController.navigate(QReportRoutes.facilityCreateRoute(clientId))
                        },
                        onEditFacility = { facilityId ->
                            navController.navigate(
                                QReportRoutes.facilityEditRoute(clientId, facilityId)
                            )
                        },
                        onNavigateBack = { navController.popBackStack() }
                    )
                }


                // ============================================================
                // FACILITY ISLAND SCREENS - ✅ FIXED con GetFacilityByIdUseCase
                // ============================================================

                // 1. CREATE
                composable(
                    QReportRoutes.ISLAND_CREATE,
                    arguments = listOf(
                        navArgument("facilityId") { type = NavType.StringType }
                    )
                ) { backStackEntry ->
                    val facilityId = backStackEntry.arguments?.getString("facilityId") ?: ""

                    IslandFormScreen(
                        facilityId = facilityId,
                        facilityName = "", // ✅ SIMPLE: Let ViewModel handle facilityName lookup
                        islandId = null, // Create mode
                        onNavigateBack = { navController.popBackStack() },
                        onIslandSaved = { savedIslandId ->
                            navController.popBackStack()
//                            navController.navigate(
//                                QReportRoutes.islandDetailRoute(
//                                    facilityId,
//                                    savedIslandId
//                                )
//                            ) {
//                                // TODO use the correct route, FacilityDetail, once the clientName has been passed
//                                popUpTo(QReportRoutes.CLIENTS) { inclusive = false }
//                            }
                        }
                    )
                }

                // 2. EDIT
                composable(
                    QReportRoutes.ISLAND_EDIT,
                    arguments = listOf(
                        navArgument("facilityId") { type = NavType.StringType },
                        navArgument("islandId") { type = NavType.StringType }
                    )
                ) { backStackEntry ->
                    val facilityId = backStackEntry.arguments?.getString("facilityId") ?: ""
                    val islandId = backStackEntry.arguments?.getString("islandId") ?: ""

                    IslandFormScreen(
                        facilityId = facilityId,
                        facilityName = "", // ✅ SIMPLE: Let ViewModel handle facilityName lookup
                        islandId = islandId, // Edit mode
                        onNavigateBack = { navController.popBackStack() },
                        onIslandSaved = { savedIslandId ->
                            navController.popBackStack()
//                            navController.navigate(
//                                QReportRoutes.islandDetailRoute(
//                                    facilityId,
//                                    savedIslandId
//                                )
//                            ) {
//                                // TODO use the correct route, FacilityDetail, once the clientName has been passed
//                                popUpTo(QReportRoutes.CLIENTS) { inclusive = false }
//                            }
                        }
                    )
                }

                // 3. DETAIL
                composable(
                    QReportRoutes.ISLAND_DETAIL,
                    arguments = listOf(
                        navArgument("facilityId") { type = NavType.StringType },
                        navArgument("islandId") { type = NavType.StringType }
                    )
                ) { backStackEntry ->
                    val facilityId = backStackEntry.arguments?.getString("facilityId") ?: ""
                    val islandId = backStackEntry.arguments?.getString("islandId") ?: ""

                    IslandDetailScreen(
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

                // 4. LIST
                composable(
                    QReportRoutes.ISLAND_LIST,
                    arguments = listOf(
                        navArgument("facilityId") { type = NavType.StringType }
                    )
                ) { backStackEntry ->
                    val facilityId = backStackEntry.arguments?.getString("facilityId") ?: ""

                    IslandListScreen(
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

                // ============================================================
                // ✅ BACKUP
                // ============================================================

                // ===== BACKUP SCREEN =====
                composable(QReportRoutes.BACKUP) {
                    BackupScreen(
                        onNavigateBack = {
                            navController.popBackStack()
                        }
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
                        contentDescription = destination.title.asString()
                    )
                },
                label = {
                    Text(
                        text = destination.title.asString(),
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