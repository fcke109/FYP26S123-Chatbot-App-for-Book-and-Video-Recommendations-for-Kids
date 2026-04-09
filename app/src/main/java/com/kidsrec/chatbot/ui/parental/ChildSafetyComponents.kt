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
    if (!isLocked) {
        content()
        return
    }

    var pin by remember { mutableStateOf("") }
    var showDialog by remember { mutableStateOf(true) }

    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text("Parental PIN Required") },
            text = {
                Column {
                    Text("Enter your parental PIN to continue.")
                    Spacer(modifier = Modifier.height(8.dp))
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
            confirmButton = {
                TextButton(onClick = {
                    showDialog = false
                    onAccessGranted()
                }) {
                    Text("Confirm")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun ChildSettingsEntry(
    lockEnabled: Boolean,
    isUnlocked: Boolean,
    onRequestUnlock: () -> Unit,
    content: @Composable () -> Unit
) {
    if (!lockEnabled || isUnlocked) {
        content()
    } else {
        OutlinedButton(
            onClick = onRequestUnlock,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Unlock Parental Controls")
        }
    }
}
