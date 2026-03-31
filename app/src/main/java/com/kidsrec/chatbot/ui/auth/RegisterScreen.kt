package com.kidsrec.chatbot.ui.auth

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kidsrec.chatbot.data.model.AccountType

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun RegisterScreen(
    onRegisterSuccess: () -> Unit,
    onNavigateToLogin: () -> Unit,
    viewModel: AuthViewModel
) {
    var selectedAccountType by remember { mutableStateOf<AccountType?>(null) }

    // Shared fields
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }

    // Child-only fields
    var age by remember { mutableStateOf("") }
    var inviteCode by remember { mutableStateOf("") }
    var selectedInterests by remember { mutableStateOf(setOf<String>()) }
    var readingLevel by remember { mutableStateOf("Beginner") }

    val authState by viewModel.authState.collectAsState()

    val interests = listOf(
        "Reading", "Science", "Animals", "Adventure",
        "Fantasy", "Art", "Music", "Sports", "History", "Nature",
        "Space", "Dinosaurs", "Cooking", "Cars", "Robots",
        "Fairy Tales", "Superheroes", "Ocean", "Puzzles", "Travel"
    )
    var interestsExpanded by remember { mutableStateOf(false) }

    val readingLevels = listOf("Beginner", "Early Reader", "Intermediate", "Advanced")

    LaunchedEffect(authState) {
        if (authState is AuthState.Authenticated) {
            onRegisterSuccess()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Create Account",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.height(24.dp))

        // ── Step 1: Account Type Selection ──────────────────────
        if (selectedAccountType == null) {
            Text(
                text = "I am a...",
                fontSize = 20.sp,
                fontWeight = FontWeight.Medium
            )

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                AccountTypeCard(
                    title = "Parent",
                    description = "Manage and monitor your children's activity",
                    icon = Icons.Default.Person,
                    modifier = Modifier.weight(1f),
                    onClick = { selectedAccountType = AccountType.PARENT }
                )

                AccountTypeCard(
                    title = "Kid",
                    description = "Discover books and videos with Little Dino",
                    icon = Icons.Default.Face,
                    modifier = Modifier.weight(1f),
                    onClick = { selectedAccountType = AccountType.CHILD }
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            TextButton(onClick = onNavigateToLogin) {
                Text("Already have an account? Login")
            }

            return@Column
        }

        // ── Step 2: Registration Form ───────────────────────────

        // Back button to return to account type selection
        TextButton(
            onClick = {
                selectedAccountType = null
                viewModel.clearError()
            },
            modifier = Modifier.align(Alignment.Start)
        ) {
            Icon(Icons.Default.ArrowBack, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                if (selectedAccountType == AccountType.PARENT) "Parent Account"
                else "Kid Account"
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // ── Invite Code (child only, shown first) ───────────────
        AnimatedVisibility(visible = selectedAccountType == AccountType.CHILD) {
            Column {
                OutlinedTextField(
                    value = inviteCode,
                    onValueChange = {
                        if (it.length <= 6) inviteCode = it.uppercase()
                    },
                    label = { Text("Parent Invite Code") },
                    leadingIcon = { Icon(Icons.Default.Link, contentDescription = null) },
                    supportingText = { Text("Ask your parent for a 6-character code") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text)
                )
                Spacer(modifier = Modifier.height(16.dp))
            }
        }

        // ── Common fields: Name, Email, Password ────────────────
        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            label = { Text("Name") },
            leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("Email") },
            leadingIcon = { Icon(Icons.Default.Email, contentDescription = null) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email)
        )

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Password") },
            leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
            trailingIcon = {
                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                    Icon(
                        imageVector = if (passwordVisible) Icons.Default.Visibility
                        else Icons.Default.VisibilityOff,
                        contentDescription = null
                    )
                }
            },
            visualTransformation = if (passwordVisible) VisualTransformation.None
            else PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password)
        )

        Spacer(modifier = Modifier.height(16.dp))

        // ── Child-only fields: Age, Interests, Reading Level ────
        AnimatedVisibility(
            visible = selectedAccountType == AccountType.CHILD,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            Column {
                OutlinedTextField(
                    value = age,
                    onValueChange = { if (it.isEmpty() || it.all { char -> char.isDigit() }) age = it },
                    label = { Text("Age") },
                    leadingIcon = { Icon(Icons.Default.Cake, contentDescription = null) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )

                Spacer(modifier = Modifier.height(24.dp))

                Text(
                    text = "Select Your Interests",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Selected interests chips
                if (selectedInterests.isNotEmpty()) {
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        selectedInterests.forEach { interest ->
                            InputChip(
                                selected = true,
                                onClick = { selectedInterests = selectedInterests - interest },
                                label = { Text(interest) },
                                trailingIcon = {
                                    Icon(
                                        Icons.Default.Close,
                                        contentDescription = "Remove $interest",
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }

                // Dropdown selector
                ExposedDropdownMenuBox(
                    expanded = interestsExpanded,
                    onExpandedChange = { interestsExpanded = it }
                ) {
                    OutlinedTextField(
                        value = if (selectedInterests.isEmpty()) "" else "${selectedInterests.size} selected",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Tap to pick interests") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = interestsExpanded) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor()
                    )
                    ExposedDropdownMenu(
                        expanded = interestsExpanded,
                        onDismissRequest = { interestsExpanded = false }
                    ) {
                        interests.forEach { interest ->
                            val isSelected = selectedInterests.contains(interest)
                            DropdownMenuItem(
                                text = {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Checkbox(
                                            checked = isSelected,
                                            onCheckedChange = null
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(interest)
                                    }
                                },
                                onClick = {
                                    selectedInterests = if (isSelected) {
                                        selectedInterests - interest
                                    } else {
                                        selectedInterests + interest
                                    }
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Reading Level",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    readingLevels.forEach { level ->
                        FilterChip(
                            selected = readingLevel == level,
                            onClick = { readingLevel = level },
                            label = { Text(level, fontSize = 12.sp) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }

        // ── Error display ───────────────────────────────────────
        if (authState is AuthState.Error) {
            Spacer(modifier = Modifier.height(12.dp))
            Surface(
                color = MaterialTheme.colorScheme.errorContainer,
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.ErrorOutline,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = (authState as AuthState.Error).message,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        fontSize = 14.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // ── Submit button ───────────────────────────────────────
        val isFormValid = when (selectedAccountType) {
            AccountType.PARENT -> {
                name.isNotBlank() && email.isNotBlank() && password.length >= 6
            }
            AccountType.CHILD -> {
                name.isNotBlank() && email.isNotBlank() && password.length >= 6 &&
                        age.isNotBlank() && selectedInterests.isNotEmpty() &&
                        inviteCode.length == 6
            }
            else -> false
        }

        Button(
            onClick = {
                when (selectedAccountType) {
                    AccountType.PARENT -> {
                        viewModel.signUpParent(
                            email = email,
                            password = password,
                            name = name
                        )
                    }
                    AccountType.CHILD -> {
                        val ageInt = age.toIntOrNull() ?: 0
                        viewModel.signUpChild(
                            email = email,
                            password = password,
                            name = name,
                            age = ageInt,
                            interests = selectedInterests.toList(),
                            readingLevel = readingLevel,
                            inviteCode = inviteCode
                        )
                    }
                    else -> {}
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp),
            enabled = authState !is AuthState.Loading && isFormValid
        ) {
            if (authState is AuthState.Loading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    color = MaterialTheme.colorScheme.onPrimary
                )
            } else {
                Text("Create Account", fontSize = 16.sp)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        TextButton(onClick = onNavigateToLogin) {
            Text("Already have an account? Login")
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AccountTypeCard(
    title: String,
    description: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    OutlinedCard(
        onClick = onClick,
        modifier = modifier.height(160.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(40.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = title,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = description,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}
