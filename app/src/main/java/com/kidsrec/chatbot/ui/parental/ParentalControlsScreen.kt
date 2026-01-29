package com.kidsrec.chatbot.ui.parental

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kidsrec.chatbot.ui.auth.AuthViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ParentalControlsScreen(
    onNavigateBack: () -> Unit,
    authViewModel: AuthViewModel
) {
    val user by authViewModel.currentUser.collectAsState()
    var isPinVerified by remember { mutableStateOf(false) }
    var enteredPin by remember { mutableStateOf("") }
    var maxAgeRating by remember { mutableStateOf(13) }
    var allowVideos by remember { mutableStateOf(true) }

    LaunchedEffect(user) {
        user?.let {
            maxAgeRating = it.contentFilters.maxAgeRating
            allowVideos = it.contentFilters.allowVideos
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Parental Controls") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        if (!isPinVerified) {
            // PIN Entry Screen
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    Icons.Default.Lock,
                    contentDescription = null,
                    modifier = Modifier.size(64.dp),
                    tint = MaterialTheme.colorScheme.primary
                )

                Spacer(modifier = Modifier.height(24.dp))

                Text(
                    text = "Parental Access Required",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Enter your PIN to access parental controls",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(32.dp))

                OutlinedTextField(
                    value = enteredPin,
                    onValueChange = { if (it.length <= 4) enteredPin = it },
                    label = { Text("PIN") },
                    placeholder = { Text("Enter 4-digit PIN") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(24.dp))

                Button(
                    onClick = {
                        // In production, verify against stored PIN
                        // For demo, accept any 4-digit PIN
                        if (enteredPin.length == 4) {
                            isPinVerified = true
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = enteredPin.length == 4
                ) {
                    Text("Verify PIN")
                }

                Spacer(modifier = Modifier.height(16.dp))

                TextButton(onClick = onNavigateBack) {
                    Text("Cancel")
                }
            }
        } else {
            // Parental Controls Settings
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .verticalScroll(rememberScrollState())
                    .padding(24.dp)
            ) {
                Text(
                    text = "Content Filters",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(16.dp))

                Card(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Maximum Age Rating: $maxAgeRating",
                            fontWeight = FontWeight.Medium
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Slider(
                            value = maxAgeRating.toFloat(),
                            onValueChange = { maxAgeRating = it.toInt() },
                            valueRange = 3f..18f,
                            steps = 14,
                            modifier = Modifier.fillMaxWidth()
                        )

                        Text(
                            text = "Content rated for ages up to $maxAgeRating will be shown",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Card(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Allow Video Recommendations",
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = "Enable or disable video content recommendations",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Switch(
                            checked = allowVideos,
                            onCheckedChange = { allowVideos = it }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                Text(
                    text = "Activity Monitoring",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedButton(
                    onClick = { /* Navigate to chat history */ },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("View Chat History")
                }

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedButton(
                    onClick = { /* Navigate to favorites */ },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("View Favorites")
                }

                Spacer(modifier = Modifier.height(32.dp))

                Button(
                    onClick = {
                        // Save settings
                        user?.let { currentUser ->
                            val updatedUser = currentUser.copy(
                                contentFilters = currentUser.contentFilters.copy(
                                    maxAgeRating = maxAgeRating,
                                    allowVideos = allowVideos
                                )
                            )
                            authViewModel.updateUser(updatedUser)
                        }
                        onNavigateBack()
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Save Changes")
                }
            }
        }
    }
}
