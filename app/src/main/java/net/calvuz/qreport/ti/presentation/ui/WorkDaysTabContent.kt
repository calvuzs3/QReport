package net.calvuz.qreport.ti.presentation.ui

import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBackIosNew
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import net.calvuz.qreport.R

/**
 * Main content for WorkDays tab.
 * Switches between list view and detail form based on state.
 */
@Suppress("ParamsComparedByRef")
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
                    text = error.asString(),
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
                        onNavigateBack = { skipDataRefresh -> tabViewModel.navigateBackToList(skipDataRefresh) },
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
@Suppress("ParamsComparedByRef")
@Composable
private fun WorkDayDetailWrapper(
    interventionId: String,
    workDayIndex: Int?,
    totalWorkDays: Int,
    formViewModel: WorkDayFormViewModel,
    onNavigateBack: (skipDataRefresh: Boolean) -> Unit,
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
                    onNavigateBack(false)
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
                    imageVector = Icons.Default.ArrowBackIosNew,
                    contentDescription = stringResource(R.string.intervention_workdays_tab_back_to_list_cd)
                )
            }

            Text(
                text = if (workDayIndex != null) {
                    stringResource(R.string.intervention_workdays_tab_day_header, workDayIndex + 1, totalWorkDays)
                } else {
                    stringResource(R.string.intervention_workdays_tab_new_day_header)
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
                        text = stringResource(R.string.intervention_workdays_tab_unsaved_indicator),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
            }
        }
    }
}