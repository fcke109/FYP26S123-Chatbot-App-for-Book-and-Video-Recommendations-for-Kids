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

@Composable
fun ScreenTimeLockDialog(
    usedMinutes: Int,
    limitMinutes: Int,
    extensionRequested: Boolean,
    onRequestMoreTime: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = { /* Non-dismissable */ },
        icon = {
            Icon(
                Icons.Default.Timer,
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = MaterialTheme.colorScheme.primary
            )
        },
        title = {
            Text(
                text = "Time's Up!",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
        },
        text = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "You've used $usedMinutes minutes of your $limitMinutes minute daily limit.",
                    textAlign = TextAlign.Center,
                    fontSize = 15.sp
                )
                Spacer(modifier = Modifier.height(12.dp))
                if (extensionRequested) {
                    Text(
                        text = "Your request for more time has been sent to your parent!",
                        textAlign = TextAlign.Center,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                } else {
                    Text(
                        text = "You can ask your parent for more time.",
                        textAlign = TextAlign.Center,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        confirmButton = {
            if (!extensionRequested) {
                Button(onClick = onRequestMoreTime) {
                    Text("Ask for More Time")
                }
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("OK")
            }
        }
    )
}
