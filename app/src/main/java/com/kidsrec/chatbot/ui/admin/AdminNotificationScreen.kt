package com.kidsrec.chatbot.ui.admin

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminNotificationScreen(
    state: AdminNotificationUiState,
    onTitleChange: (String) -> Unit,
    onBodyChange: (String) -> Unit,
    onTypeChange: (NotificationType) -> Unit,
    onTargetChange: (String) -> Unit,
    onSendClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Send Notification",
            style = MaterialTheme.typography.headlineSmall
        )

        Text(
            text = "Sent notifications will appear as a pop-up to users the next time they log in.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            NotificationType.entries.forEach { type ->
                FilterChip(
                    selected = state.type == type,
                    onClick = { onTypeChange(type) },
                    label = { Text(type.name.lowercase().replaceFirstChar { it.uppercase() }) }
                )
            }
        }

        OutlinedTextField(
            value = state.title,
            onValueChange = onTitleChange,
            label = { Text("Title") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        OutlinedTextField(
            value = state.body,
            onValueChange = onBodyChange,
            label = { Text("Message Body") },
            modifier = Modifier.fillMaxWidth(),
            minLines = 3,
            maxLines = 5
        )

        if (state.type == NotificationType.PERSONALIZED) {
            OutlinedTextField(
                value = state.targetValue,
                onValueChange = onTargetChange,
                label = { Text("Interest Category") },
                placeholder = { Text("e.g., dinosaurs, space, fairy tales") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
        }

        Button(
            onClick = onSendClick,
            modifier = Modifier.fillMaxWidth(),
            enabled = state.title.isNotBlank() && state.body.isNotBlank()
        ) {
            Text("Send Notification")
        }
    }
}