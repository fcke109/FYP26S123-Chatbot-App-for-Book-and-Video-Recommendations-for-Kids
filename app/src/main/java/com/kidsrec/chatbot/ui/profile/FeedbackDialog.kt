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
    var category by remember { mutableStateOf("General") }
    var rating by remember { mutableIntStateOf(5) }
    var message by remember { mutableStateOf("") }
    var expanded by remember { mutableStateOf(false) }

    val categories = listOf("General", "Books", "Videos", "Chatbot", "Safety", "Bug Report")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Send Feedback") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Tell us what you think about Little Dino.")

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

                Text("Rating")

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    (1..5).forEach { value ->
                        FilterChip(
                            selected = rating == value,
                            onClick = { rating = value },
                            label = { Text("$value") }
                        )
                    }
                }

                OutlinedTextField(
                    value = message,
                    onValueChange = { message = it },
                    label = { Text("Your feedback") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 4
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onSubmit(category, rating, message.trim()) },
                enabled = message.isNotBlank()
            ) {
                Text("Submit")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}