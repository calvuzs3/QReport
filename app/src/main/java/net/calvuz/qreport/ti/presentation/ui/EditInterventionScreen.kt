@file:OptIn(ExperimentalMaterial3Api::class)
@file: Suppress("HardCodedStringLiteral", "ASSIGNED_VALUE_IS_NEVER_READ")
package net.calvuz.qreport.ti.presentation.ui

import androidx.activity.compose.BackHandler
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBackIosNew
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.outlined.Save
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch
import net.calvuz.qreport.R
import net.calvuz.qreport.app.app.presentation.ui.theme.warning
import net.calvuz.qreport.app.result.domain.QrResult
import timber.log.Timber

/**
 * Edit screen for TechnicalIntervention with tabbed interface
 * Tabs: General, Details, WorkDays, Signatures
 */
@Suppress("ParamsComparedByRef")
@Composable
fun EditInterventionScreen(
    interventionId: String,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: EditInterventionViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current

    // ViewModels for each tab (for auto-save coordination)
    val generalViewModel: GeneralFormViewModel = hiltViewModel()
    val detailsViewModel: DetailsFormViewModel = hiltViewModel()
    val workDaysTabViewModel: WorkDaysTabViewModel = hiltViewModel()
    val workDayFormViewModel: WorkDayFormViewModel = hiltViewModel()
    val signaturesViewModel: SignaturesFormViewModel = hiltViewModel()

    // Collect states from all ViewModels
    val generalState by generalViewModel.state.collectAsStateWithLifecycle()
    val detailsState by detailsViewModel.state.collectAsStateWithLifecycle()
    val workDaysTabState by workDaysTabViewModel.state.collectAsStateWithLifecycle()
    val workDayFormState by workDayFormViewModel.state.collectAsStateWithLifecycle()
    val signaturesState by signaturesViewModel.state.collectAsStateWithLifecycle()

    // Track dirty states from ViewModel states (not manual variables)
    val generalTabDirty = generalState.isDirty
    val detailsTabDirty = detailsState.isDirty
    val workDayTabDirty = workDaysTabState.viewMode is WorkDaysViewMode.Detail && workDayFormState.isDirty
    val signaturesTabDirty = signaturesState.isDirty

    // Calculate overall dirty state
    val isAnyTabDirty = generalTabDirty || detailsTabDirty || workDayTabDirty || signaturesTabDirty

    // Track if any tab is currently saving
    val isAnyTabSaving =
        generalState.isSaving || detailsState.isSaving || workDaysTabState.isSaving || signaturesState.isSaving

    // Show exit confirmation dialog
    var showExitDialog by remember { mutableStateOf(false) }

    // Track tab switching state
    var isTabSwitching by remember { mutableStateOf(false) }

    // State per tracking save durante exit
    var isExitSaving by remember { mutableStateOf(false) }

    // Coroutine scope for coordinated tab switching
    val coroutineScope = rememberCoroutineScope()

    // SnackBar
    val snackbarHostState = remember { SnackbarHostState() }

    // Initialize ViewModels with intervention ID
    LaunchedEffect(interventionId) {
        viewModel.loadIntervention(interventionId)
        // Initialize form ViewModels
        generalViewModel.loadInterventionGeneral(interventionId)
        detailsViewModel.loadInterventionDetails(interventionId)
        workDaysTabViewModel.loadWorkDays(interventionId)
        signaturesViewModel.loadSignaturesData(interventionId)
    }

    // Handle navigation back
    LaunchedEffect(state.shouldNavigateBack) {
        if (state.shouldNavigateBack) {
            Timber.d("EditInterventionScreen: Navigation back triggered")
            onNavigateBack()
        }
    }

    // Show error messages
    LaunchedEffect(state.errorMessage) {
        state.errorMessage?.let { error ->
            Timber.e("EditInterventionScreen: Error message: $error")
            snackbarHostState.showSnackbar(error.asString(context))
            viewModel.clearError()
        }
    }

    // Show success messages
    LaunchedEffect(state.successMessage) {
        state.successMessage?.let { message ->
            Timber.d("EditInterventionScreen: Success message: $message")
            snackbarHostState.showSnackbar(message.asString(context))
            viewModel.clearSuccess()
        }
    }

    /**
     * Save current tab before exit - ASYNC IMPLEMENTATION
     */
    fun saveCurrentTabAndExit() {
        coroutineScope.launch {
            isExitSaving = true

            try {
                Timber.d("Saving current tab before exit")

                // Determine which tab is currently selected and save it
                val saveResult = when (EditInterventionTab.entries[state.selectedTabIndex]) {
                    EditInterventionTab.GENERAL -> {
                        if (generalTabDirty) generalViewModel.autoSaveOnTabChange() else QrResult.Success(
                            Unit
                        )
                    }

                    EditInterventionTab.DETAILS -> {
                        if (detailsTabDirty) detailsViewModel.autoSaveOnTabChange() else QrResult.Success(
                            Unit
                        )
                    }

                    EditInterventionTab.WORK_DAYS -> {
                        if (workDayTabDirty) workDayFormViewModel.autoSaveOnTabChange() else QrResult.Success(
                            Unit
                        )
                    }

                    EditInterventionTab.SIGNATURES -> {
                        if (signaturesTabDirty) signaturesViewModel.autoSaveOnTabChange() else QrResult.Success(
                            Unit
                        )
                    }
                }

                when (saveResult) {
                    is QrResult.Success -> {
                        Timber.d("Current tab saved successfully, navigating back")
                        onNavigateBack()
                    }

                    is QrResult.Error -> {
                        Timber.e("Save failed before exit: ${saveResult.error}")
                        // Show error but allow exit anyway (user can choose)
                        onNavigateBack()
                    }
                }
            } finally {
                isExitSaving = false
                showExitDialog = false
            }
        }
    }

    /**
     * Save the current tab without exiting the screen.
     */
    fun saveCurrentTab() {
        coroutineScope.launch {
            try {
                Timber.d("Saving current tab")

                val saveResult = when (EditInterventionTab.entries[state.selectedTabIndex]) {
                    EditInterventionTab.GENERAL -> {
                        if (generalTabDirty) generalViewModel.autoSaveOnTabChange() else QrResult.Success(
                            Unit
                        )
                    }

                    EditInterventionTab.DETAILS -> {
                        if (detailsTabDirty) detailsViewModel.autoSaveOnTabChange() else QrResult.Success(
                            Unit
                        )
                    }

                    EditInterventionTab.WORK_DAYS -> {
                        if (workDayTabDirty) workDayFormViewModel.autoSaveOnTabChange() else QrResult.Success(
                            Unit
                        )
                    }

                    EditInterventionTab.SIGNATURES -> {
                        if (signaturesTabDirty) signaturesViewModel.autoSaveOnTabChange() else QrResult.Success(
                            Unit
                        )
                    }
                }

                when (saveResult) {
                    is QrResult.Success -> {
                        snackbarHostState.showSnackbar(context.getString(R.string.form_status_saved))
                    }

                    is QrResult.Error -> {
                        Timber.e("Save failed: ${saveResult.error}")
                    }
                }
            } catch (e: Exception) {
                Timber.e(e, "Exception saving current tab")
            }
        }
    }

    val handleBackPress = {
        if (isAnyTabDirty) {
            showExitDialog = true
        } else {
            onNavigateBack()
        }
    }

    BackHandler(onBack = handleBackPress)

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = stringResource(R.string.intervention_edit_title),
                                style = MaterialTheme.typography.titleMedium
                            )

                            // Saving indicator
                            if (isAnyTabSaving) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    strokeWidth = 2.dp
                                )
                            }
                        }
                        if (state.interventionNumber.isNotBlank()) {
                            Text(
                                text = state.interventionNumber,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = handleBackPress) {
                        Icon(
                            imageVector = Icons.Default.ArrowBackIosNew,
                            contentDescription = stringResource(R.string.action_back)
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = { saveCurrentTab() },
                        enabled = isAnyTabDirty && !isAnyTabSaving
                    ) {
                        Icon(
                            imageVector = if (isAnyTabDirty) Icons.Default.Save else Icons.Outlined.Save,
                            contentDescription = stringResource(R.string.action_save)
                        )
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        modifier = modifier
    ) { paddingValues ->

        if (state.isLoading) {
            // Loading state
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    CircularProgressIndicator()
                    Text(
                        text = stringResource(R.string.interventions_loading),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        } else {
            // Tabbed content
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                // Tab Row with dirty indicators
                TabRow(
                    selectedTabIndex = state.selectedTabIndex,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    EditInterventionTab.entries.forEachIndexed { index, tab ->
                        Tab(
                            selected = state.selectedTabIndex == index,
                            enabled = !isTabSwitching, // Disable during tab switching
                            onClick = {
                                handleCoordinatedTabSwitch(
                                    newTabIndex = index,
                                    currentTabIndex = state.selectedTabIndex,
                                    generalViewModel = generalViewModel,
                                    detailsViewModel = detailsViewModel,
                                    workDayFormViewModel = workDayFormViewModel,
                                    isWorkDayDetailOpen = workDaysTabState.viewMode is WorkDaysViewMode.Detail,
                                    signaturesViewModel = signaturesViewModel,
                                    parentViewModel = viewModel,
                                    coroutineScope = coroutineScope,
                                    onTabSwitchingStart = { isTabSwitching = true },
                                    onTabSwitchingEnd = { isTabSwitching = false }
                                )
                            },
                            text = {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = stringResource(tab.getDisplayNameRes()),
                                        style = MaterialTheme.typography.labelLarge
                                    )

                                    // Show dirty indicator per tab
                                    val tabIsDirty = when (tab) {
                                        EditInterventionTab.GENERAL -> generalTabDirty
                                        EditInterventionTab.DETAILS -> detailsTabDirty
                                        EditInterventionTab.WORK_DAYS -> workDayTabDirty
                                        EditInterventionTab.SIGNATURES -> signaturesTabDirty
                                    }

                                    if (tabIsDirty) {
                                        Icon(
                                            imageVector = Icons.Default.Warning,
                                            contentDescription = stringResource(R.string.intervention_general_unsaved_changes),
                                            tint = MaterialTheme.colorScheme.warning,
                                            modifier = Modifier.size(12.dp)
                                        )
                                    }
                                }
                            }
                        )
                    }
                }

                // Tab Content
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f)
                ) {
                    when (EditInterventionTab.entries[state.selectedTabIndex]) {
                        EditInterventionTab.GENERAL -> {
                            GeneralFormScreen(
                                interventionId = interventionId,
                                modifier = Modifier.fillMaxSize()
                            )
                        }

                        EditInterventionTab.DETAILS -> {
                            DetailsFormScreen(
                                interventionId = interventionId,
                                modifier = Modifier.fillMaxSize()
                            )
                        }

                        EditInterventionTab.WORK_DAYS -> {
                            WorkDaysTabContent(
                                interventionId = interventionId,
                                tabViewModel = workDaysTabViewModel,
                                formViewModel = workDayFormViewModel,
                                modifier = Modifier.fillMaxSize()
                            )
                        }

                        EditInterventionTab.SIGNATURES -> {
                            SignaturesFormScreen(
                                interventionId = interventionId,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }
                }
            }
        }
    }

    // ✅ EXIT CONFIRMATION DIALOG
    if (showExitDialog) {
        AlertDialog(
            onDismissRequest = {
                if (!isExitSaving) showExitDialog = false
            },
            title = {
                Text(stringResource(R.string.intervention_general_unsaved_changes))
            },
            text = {
                if (isExitSaving) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp))
                        Text(stringResource(R.string.msg_saving))
                    }
                } else {
                    Text(stringResource(R.string.intervention_edit_unsaved_dialog_text))
                }
            },
            confirmButton = {
                TextButton(
                    onClick = { saveCurrentTabAndExit() },
                    enabled = !isExitSaving
                ) {
                    Text(stringResource(R.string.intervention_edit_save_and_exit))
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showExitDialog = false
                        onNavigateBack() // Exit without saving
                    },
                    enabled = !isExitSaving
                ) {
                    Text(stringResource(R.string.intervention_edit_exit_without_saving))
                }
            }
        )
    }
}


/**
 * Handle coordinated tab switching with auto-save for all tabs
 */
private fun handleCoordinatedTabSwitch(
    newTabIndex: Int,
    currentTabIndex: Int,
    generalViewModel: GeneralFormViewModel,
    detailsViewModel: DetailsFormViewModel,
    workDayFormViewModel: WorkDayFormViewModel,
    isWorkDayDetailOpen: Boolean,
    signaturesViewModel: SignaturesFormViewModel,
    parentViewModel: EditInterventionViewModel,
    coroutineScope: kotlinx.coroutines.CoroutineScope,
    onTabSwitchingStart: () -> Unit,
    onTabSwitchingEnd: () -> Unit
) {
    if (newTabIndex == currentTabIndex) {
        Timber.d("handleCoordinatedTabSwitch: Same tab selected, no action needed")
        return
    }

    Timber.d("handleCoordinatedTabSwitch: Switching from tab $currentTabIndex to $newTabIndex")

    coroutineScope.launch {
        try {
            onTabSwitchingStart()

            // Auto-save current tab before switching based on which tab we're leaving
            val autoSaveResult = when (EditInterventionTab.entries[currentTabIndex]) {
                EditInterventionTab.GENERAL -> {
                    Timber.i("handleCoordinatedTabSwitch: Auto-saving General tab")
                    generalViewModel.autoSaveOnTabChange()
                }

                EditInterventionTab.DETAILS -> {
                    Timber.i("handleCoordinatedTabSwitch: Auto-saving Details tab")
                    detailsViewModel.autoSaveOnTabChange()
                }

                EditInterventionTab.WORK_DAYS -> {
                    if (isWorkDayDetailOpen) {
                        Timber.i("handleCoordinatedTabSwitch: Auto-saving open WorkDay detail")
                        workDayFormViewModel.autoSaveOnTabChange()
                    } else {
                        QrResult.Success(Unit)
                    }
                }

                EditInterventionTab.SIGNATURES -> {
                    Timber.i("handleCoordinatedTabSwitch: Auto-saving Signatures tab")
                    signaturesViewModel.autoSaveOnTabChange()
                }
            }

            when (autoSaveResult) {
                is QrResult.Success -> {
                    Timber.d("handleCoordinatedTabSwitch: Auto-save successful, switching to tab $newTabIndex")
                    parentViewModel.selectTab(newTabIndex)
                }

                is QrResult.Error -> {
                    Timber.w("handleCoordinatedTabSwitch: Auto-save failed, staying on current tab: ${autoSaveResult.error}")
                    // Stay on current tab due to validation/save failure
                    // Error message will be shown by the respective form
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "handleCoordinatedTabSwitch: Exception during auto-save")
            // Stay on current tab due to exception
        } finally {
            onTabSwitchingEnd()
        }
    }
}

/**
 * Tabs for EditInterventionScreen
 */
enum class EditInterventionTab {
    GENERAL,
    DETAILS,
    WORK_DAYS,
    SIGNATURES;

    @StringRes
    fun getDisplayNameRes(): Int {
        return when (this) {
            GENERAL -> R.string.intervention_edit_tab_general
            DETAILS -> R.string.intervention_edit_tab_details
            WORK_DAYS -> R.string.intervention_edit_tab_workdays
            SIGNATURES -> R.string.intervention_edit_tab_signatures
        }
    }
}