package com.kidsrec.chatbot.ui.screentime

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// Dialog shown when the child reaches their daily screen time limit
@Composable
fun ScreenTimeLockDialog(
    usedMinutes: Int,
    limitMinutes: Int,
    extensionRequested: Boolean,
    onRequestMoreTime: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        // Prevents normal dismissal by tapping outside the dialog
        onDismissRequest = { /* Non-dismissable */ },

        // Timer icon represents screen time control
        icon = {
            Icon(
                Icons.Default.Timer,
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = MaterialTheme.colorScheme.primary
            )
        },
        // Main dialog title shown when the limit is reached
        title = {
            Text(
                text = "Time's Up!",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
        },
        // Dialog body showing usage details and request status
        text = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                // Shows how much time has been used compared to the daily limit
                Text(
                    text = "You've used $usedMinutes minutes of your $limitMinutes minute daily limit.",
                    textAlign = TextAlign.Center,
                    fontSize = 15.sp
                )
                Spacer(modifier = Modifier.height(12.dp))

                // Shows confirmation if a time extension request has already been sent
                if (extensionRequested) {
                    Text(
                        text = "Your request for more time has been sent to your parent!",
                        textAlign = TextAlign.Center,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                } else {
                    // Encourages the child to ask their parent for additional time
                    Text(
                        text = "You can ask your parent for more time.",
                        textAlign = TextAlign.Center,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        // Shows the request button only if no extension request has been sent yet
        confirmButton = {
            if (!extensionRequested) {
                Button(onClick = onRequestMoreTime) {
                    Text("Ask for More Time")
                }
            }
        },
        // Allows the child to acknowledge and close the dialog
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("OK")
            }
        }
    )
}
