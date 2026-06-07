package com.tomasps.slurmmanag.presentation

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import com.tomasps.slurmmanag.presentation.dashboard.DashboardScreen
import com.tomasps.slurmmanag.presentation.servers.ServersScreen
import com.tomasps.slurmmanag.presentation.settings.SettingsScreen

@Composable
fun HomeScreen(
    tabIndex: Int,
    onTabChange: (Int) -> Unit,
    showSubmitJob: Boolean,
    onDismissSubmitJob: () -> Unit,
    onJobClick: (String, String) -> Unit,
    onServerClick: (String) -> Unit,
    onAddServer: () -> Unit,
) {
    val pagerState = rememberPagerState(initialPage = tabIndex, pageCount = { 3 })

    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.currentPage }.collect { onTabChange(it) }
    }

    LaunchedEffect(tabIndex) {
        if (pagerState.currentPage != tabIndex) {
            pagerState.animateScrollToPage(tabIndex)
        }
    }

    HorizontalPager(
        state = pagerState,
        modifier = Modifier.fillMaxSize(),
        beyondViewportPageCount = 1,
    ) { page ->
        when (page) {
            0 -> DashboardScreen(
                onJobClick = onJobClick,
                showSubmitJob = showSubmitJob,
                onDismissSubmitJob = onDismissSubmitJob,
            )
            1 -> ServersScreen(
                onServerClick = onServerClick,
                onAddServer = onAddServer,
            )
            else -> SettingsScreen()
        }
    }
}
