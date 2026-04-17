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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
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
    var pinError by remember { mutableStateOf(false) }
    var maxAgeRating by remember { mutableStateOf(13) }
    var allowVideos by remember { mutableStateOf(true) }

    LaunchedEffect(user) {
        user?.let {
            maxAgeRating = it.contentFilters.maxAgeRating
            allowVideos = it.contentFilters.allowVideos
            isPinVerified = false
            enteredPin = ""
            pinError = false
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
        val currentUser = user

        if (currentUser == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }

        val storedPin = currentUser.parentalPin

        if (storedPin.isNullOrBlank()) {
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
                    text = "Parent PIN Not Set",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Your parent needs to open the Parent Dashboard and create a 4-digit PIN for this child account first.",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(24.dp))

                Button(
                    onClick = onNavigateBack,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Go Back")
                }
            }
        } else if (!isPinVerified) {
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
                    text = "Enter your parent's 4-digit PIN to access parental controls",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(32.dp))

                OutlinedTextField(
                    value = enteredPin,
                    onValueChange = {
                        if (it.length <= 4 && it.all { c -> c.isDigit() }) {
                            enteredPin = it
                            pinError = false
                        }
                    },
                    label = { Text("PIN") },
                    placeholder = { Text("Enter 4-digit PIN") },
                    isError = pinError,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    supportingText = {
                        if (pinError) Text("Incorrect PIN. Please try again.")
                    },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(24.dp))

                Button(
                    onClick = {
                        if (enteredPin == storedPin) {
                            isPinVerified = true
                        } else {
                            pinError = true
                            enteredPin = ""
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

                Button(
                    onClick = {
                        val updatedUser = currentUser.copy(
                            contentFilters = currentUser.contentFilters.copy(
                                maxAgeRating = maxAgeRating,
                                allowVideos = allowVideos
                            )
                        )
                        authViewModel.updateUser(updatedUser)
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