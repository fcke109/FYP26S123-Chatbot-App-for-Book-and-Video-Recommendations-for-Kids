package com.kidsrec.chatbot.ui.screentime

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.*
import androidx.hilt.navigation.compose.hiltViewModel

@Composable
fun ScreenTimeWrapper(
    content: @Composable () -> Unit
) {
    val viewModel: ScreenTimeViewModel = hiltViewModel()
    val isTimeLimitReached by viewModel.isTimeLimitReached.collectAsState()
    val todaySession by viewModel.todaySession.collectAsState()
    val screenTimeConfig by viewModel.screenTimeConfig.collectAsState()
    val extensionRequested by viewModel.extensionRequested.collectAsState()
    var showDialog by remember { mutableStateOf(false) }

    DisposableEffect(Unit) {
        viewModel.startTracking()
        onDispose {
            viewModel.stopTracking()
        }
    }

    LaunchedEffect(isTimeLimitReached) {
        if (isTimeLimitReached) {
            showDialog = true
        }
    }

    Box {
        content()

        if (showDialog && isTimeLimitReached) {
            ScreenTimeLockDialog(
                usedMinutes = todaySession?.totalMinutes ?: 0,
                limitMinutes = screenTimeConfig.dailyLimitMinutes + (todaySession?.bonusMinutes ?: 0),
                extensionRequested = extensionRequested,
                onRequestMoreTime = { viewModel.requestMoreTime() },
                onDismiss = { showDialog = false }
            )
        }
    }
}
