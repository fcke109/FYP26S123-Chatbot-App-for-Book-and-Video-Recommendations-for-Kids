package com.kidsrec.chatbot.ui.admin

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminUpgradeScreen(
    onBack: () -> Unit
) {
    // Main screen layout with a top app bar and body content area
    Scaffold(
        topBar = {
            // Top navigation bar for the Admin CMS Upgrade screen
            TopAppBar(
                title = { Text("Admin CMS Upgrade") },
                navigationIcon = {
                    // Back button that triggers the onBack callback
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        // Main content container, centered within the available screen space
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentAlignment = Alignment.Center
        ) {
            Text(
                // Placeholder text shown until Admin CMS upgrade features are implemented
                text = "Admin CMS Upgrade features coming soon.",
                style = MaterialTheme.typography.bodyLarge
            )
        }
    }
}
