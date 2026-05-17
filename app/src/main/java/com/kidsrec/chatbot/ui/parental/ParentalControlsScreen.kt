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
import androidx.compose.runtime.saveable.rememberSaveable

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ParentalControlsScreen(
    onNavigateBack: () -> Unit,
    authViewModel: AuthViewModel
) {
    // Observes the currently logged-in user
    val user by authViewModel.currentUser.collectAsState()

    // Tracks whether the parent PIN has been successfully verified
    var isPinVerified by rememberSaveable { mutableStateOf(false) }

    // Stores the PIN entered by the user
    var enteredPin by rememberSaveable { mutableStateOf("") }

    // Tracks whether the entered PIN is incorrect
    var pinError by rememberSaveable { mutableStateOf(false) }

    // Stores the selected maximum age rating for content filtering
    var maxAgeRating by remember { mutableStateOf(13) }

    // Stores whether video recommendations are allowed
    var allowVideos by remember { mutableStateOf(true) }

    // Loads the current user's saved content filter settings when the user changes
    LaunchedEffect(user?.id) {
        user?.let {
            maxAgeRating = it.contentFilters.maxAgeRating
            allowVideos = it.contentFilters.allowVideos
            pinError = false
        }
    }

    Scaffold(
        // Top navigation bar for the parental controls screen
        topBar = {
            TopAppBar(
                title = { Text("Parental Controls") },
                navigationIcon = {
                    // Back button to leave the parental controls screen
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        val currentUser = user

        // Shows a loading spinner while the current user data is not available yet
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

        // Retrieves the saved parental PIN from the current user profile
        val storedPin = currentUser.parentalPin

        // If no PIN has been created, tell the user that the parent must set one first
        if (storedPin.isNullOrBlank()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                // Lock icon indicates restricted parent-only settings
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

                // Returns the user to the previous screen
                Button(
                    onClick = onNavigateBack,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Go Back")
                }
            }
        } else if (!isPinVerified) {
            // Shows PIN verification screen before allowing access to parental controls
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                // Lock icon shown before PIN verification
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

                // PIN input field that only accepts up to 4 numeric digits
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

                // Verifies the entered PIN against the stored parental PIN
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

                // Cancels PIN verification and returns to the previous screen
                TextButton(onClick = onNavigateBack) {
                    Text("Cancel")
                }
            }
        } else {
            // Shows parental control settings after the PIN is successfully verified
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

                // Card for setting the maximum allowed content age rating
                Card(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Maximum Age Rating: $maxAgeRating",
                            fontWeight = FontWeight.Medium
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        // Slider allows the parent to choose an age range from 3 to 18
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

                // Card for enabling or disabling video recommendations
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

                        // Toggle for allowing video content recommendations
                        Switch(
                            checked = allowVideos,
                            onCheckedChange = { allowVideos = it }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Saves updated content filter settings to the current user profile
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