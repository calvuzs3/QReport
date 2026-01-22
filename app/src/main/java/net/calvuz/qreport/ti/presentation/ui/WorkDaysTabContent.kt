package net.calvuz.qreport.ti.presentation.ui

import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

/**
 * Main content for WorkDays tab.
 * Switches between list view and detail form based on state.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkDaysTabContent(
    interventionId: String,
    modifier: Modifier = Modifier,
    tabViewModel: WorkDaysTabViewModel = hiltViewModel(),
    formViewModel: WorkDayFormViewModel = hiltViewModel()
) {
    val tabState by tabViewModel.state.collectAsStateWithLifecycle()

    // Load work days on first composition
    LaunchedEffect(interventionId) {
        tabViewModel.loadWorkDays(interventionId)
    }

    // Error handling
    LaunchedEffect(tabState.errorMessage) {
        tabState.errorMessage?.let {
            // Error is displayed in UI, will be cleared on user action
        }
    }

    Column(modifier = modifier.fillMaxSize()) {
        // Error banner (if any)
        tabState.errorMessage?.let { error ->
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.errorContainer
            ) {
                Text(
                    text = error,
                    modifier = Modifier.padding(16.dp),
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }

        // Content based on view mode
        AnimatedContent(
            targetState = tabState.viewMode,
            transitionSpec = {
                if (targetState is WorkDaysViewMode.Detail) {
                    // Entering detail: slide in from right
                    slideInHorizontally { it } + fadeIn() togetherWith
                            slideOutHorizontally { -it } + fadeOut()
                } else {
                    // Returning to list: slide in from left
                    slideInHorizontally { -it } + fadeIn() togetherWith
                            slideOutHorizontally { it } + fadeOut()
                }
            },
            label = "work_days_view_mode"
        ) { viewMode ->
            when (viewMode) {
                is WorkDaysViewMode.List -> {
                    WorkDaysListContent(
                        workDays = tabState.workDays,
                        isLoading = tabState.isLoading,
                        onWorkDayClick = { index -> tabViewModel.editWorkDay(index) },
                        onWorkDayDelete = { index -> tabViewModel.deleteWorkDay(index) },
                        onAddWorkDay = { tabViewModel.addNewWorkDay() },
                        modifier = Modifier.fillMaxSize()
                    )
                }

                is WorkDaysViewMode.Detail -> {
                    WorkDayDetailWrapper(
                        interventionId = interventionId,
                        workDayIndex = viewMode.workDayIndex,
                        totalWorkDays = tabState.workDays.size,
                        formViewModel = formViewModel,
                        onNavigateBack = { tabViewModel.navigateBackToList() },
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }
    }
}

/**
 * Wrapper for WorkDayFormScreen with back navigation header
 */
@Composable
private fun WorkDayDetailWrapper(
    interventionId: String,
    workDayIndex: Int?,
    totalWorkDays: Int,
    formViewModel: WorkDayFormViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val formState by formViewModel.state.collectAsStateWithLifecycle()

    // Load work day data when entering detail view
    LaunchedEffect(interventionId, workDayIndex) {
        formViewModel.loadWorkDayData(interventionId, workDayIndex)
    }

    Column(modifier = modifier) {
        // Detail header with back button
        WorkDayDetailHeader(
            workDayIndex = workDayIndex,
            totalWorkDays = totalWorkDays,
            isDirty = formState.isDirty,
            isSaving = formState.isSaving,
            onBackClick = {
                // Auto-save before navigating back if dirty
                if (formState.isDirty) {
                    formViewModel.saveAndNavigateBack(onNavigateBack)
                } else {
                    onNavigateBack()
                }
            }
        )

        // Form content
        WorkDayFormScreen(
            interventionId = interventionId,
            workDayIndex = workDayIndex,
            viewModel = formViewModel,
            modifier = Modifier
                .fillMaxSize()
                .weight(1f)
        )
    }
}

/**
 * Header for detail view with back navigation
 */
@Composable
private fun WorkDayDetailHeader(
    workDayIndex: Int?,
    totalWorkDays: Int,
    isDirty: Boolean,
    isSaving: Boolean,
    onBackClick: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceVariant,
        tonalElevation = 2.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
        ) {
            IconButton(onClick = onBackClick) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "Torna alla lista"
                )
            }

            Text(
                text = if (workDayIndex != null) {
                    "Giornata ${workDayIndex + 1}/${totalWorkDays}"
                } else {
                    "Nuova giornata"
                },
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.weight(1f)
            )

            // Status indicators
            if (isSaving) {
                CircularProgressIndicator(
                    modifier = Modifier
                        .size(20.dp)
                        .padding(end = 8.dp),
                    strokeWidth = 2.dp
                )
            } else if (isDirty) {
                Surface(
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    shape = MaterialTheme.shapes.small
                ) {
                    Text(
                        text = "Non salvato",
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
            }
        }
    }
}