package com.kidsrec.chatbot.ui.profile

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FeedbackDialog(
    onDismiss: () -> Unit,
    onSubmit: (category: String, rating: Int, message: String) -> Unit
) {
    // Stores the selected feedback category
    var category by remember { mutableStateOf("General") }
    // Stores the selected rating, defaulting to 5
    var rating by remember { mutableIntStateOf(5) }
    // Stores the written feedback message
    var message by remember { mutableStateOf("") }
    // Controls whether the category dropdown is expanded
    var expanded by remember { mutableStateOf(false) }

    // Available feedback categories shown to the user
    val categories = listOf("General", "Books", "Videos", "Chatbot", "Safety", "Bug Report")

    AlertDialog(
        // Allows the dialog to be dismissed when the user taps outside or presses back
        onDismissRequest = onDismiss,
        // Dialog title
        title = { Text("Send Feedback") },
        // Main feedback form content
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Tell us what you think about Little Dino.")

                // Dropdown menu for selecting feedback category
                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = !expanded }
                ) {
                    OutlinedTextField(
                        value = category,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Category") },
                        trailingIcon = {
                            ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor()
                    )

                    // List of selectable feedback categories
                    ExposedDropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        categories.forEach { item ->
                            DropdownMenuItem(
                                text = { Text(item) },
                                onClick = {
                                    category = item
                                    expanded = false
                                }
                            )
                        }
                    }
                }

                // Rating section label
                Text("Rating")

                // Rating selector from 1 to 5 using filter chips
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    (1..5).forEach { value ->
                        FilterChip(
                            selected = rating == value,
                            onClick = { rating = value },
                            label = { Text("$value") }
                        )
                    }
                }

                // Text field for the user's feedback message
                OutlinedTextField(
                    value = message,
                    onValueChange = { message = it },
                    label = { Text("Your feedback") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 4
                )
            }
        },
        // Submit button sends feedback only when the message is not blank
        confirmButton = {
            Button(
                onClick = { onSubmit(category, rating, message.trim()) },
                enabled = message.isNotBlank()
            ) {
                Text("Submit")
            }
        },
        // Cancel button closes the dialog without submitting feedback
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}