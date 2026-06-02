package net.calvuz.qreport.app.app.presentation.components

import androidx.compose.foundation.layout.BoxScope
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults.Indicator
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier

/**
 * Wrapper around [PullToRefreshBox] with a branded indicator.
 *
 * Replaces the deprecated [PullToRefreshContainer] + [nestedScrollConnection]
 * pattern. All screens should use this component instead.
 *
 * Usage:
 * ```
 * QReportPullToRefresh(
 *     isRefreshing = uiState.isRefreshing,
 *     onRefresh = viewModel::refresh
 * ) {
 *     // screen content
 * }
 * ```
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QReportPullToRefresh(
    isRefreshing: Boolean,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    val state = rememberPullToRefreshState()

    PullToRefreshBox(
        isRefreshing = isRefreshing,
        onRefresh = onRefresh,
        modifier = modifier,
        state = state,
        indicator = {
            Indicator(
                modifier = Modifier.align(Alignment.TopCenter),
                isRefreshing = isRefreshing,
                state = state,
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
    ) {
        content()
    }
}