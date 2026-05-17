package com.kidsrec.chatbot.ui.parental

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp

@Composable
fun ChildSafetyLockGate(
    isLocked: Boolean,
    onAccessGranted: () -> Unit,
    content: @Composable () -> Unit
) {
    // If the lock is disabled, show the content immediately
    if (!isLocked) {
        content()
        return
    }

    // Stores the PIN typed by the user
    var pin by remember { mutableStateOf("") }

    // Controls whether the PIN dialog is visible
    var showDialog by remember { mutableStateOf(true) }

    // Shows the parental PIN dialog when locked content is accessed
    if (showDialog) {
        AlertDialog(
            // Closes the dialog when dismissed
            onDismissRequest = { showDialog = false },

            // Dialog title
            title = { Text("Parental PIN Required") },

            // Dialog body containing explanation and PIN input field
            text = {
                Column {
                    Text("Enter your parental PIN to continue.")
                    Spacer(modifier = Modifier.height(8.dp))

                    // PIN input field with password-style masking
                    OutlinedTextField(
                        value = pin,
                        onValueChange = { if (it.length <= 6) pin = it },
                        label = { Text("PIN") },
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        singleLine = true
                    )
                }
            },
            // Confirms access and triggers the access callback
            confirmButton = {
                TextButton(onClick = {
                    showDialog = false
                    onAccessGranted()
                }) {
                    Text("Confirm")
                }
            },
            // Cancels access and closes the dialog
            dismissButton = {
                TextButton(onClick = { showDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

// Entry component that either shows locked content or an unlock button
@Composable
fun ChildSettingsEntry(
    lockEnabled: Boolean,
    isUnlocked: Boolean,
    onRequestUnlock: () -> Unit,
    content: @Composable () -> Unit
) {
    // Show the protected content if locking is disabled or access has already been granted
    if (!lockEnabled || isUnlocked) {
        content()
    } else {
        // Shows an unlock button when parental controls are locked
        OutlinedButton(
            onClick = onRequestUnlock,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Unlock Parental Controls")
        }
    }
}
