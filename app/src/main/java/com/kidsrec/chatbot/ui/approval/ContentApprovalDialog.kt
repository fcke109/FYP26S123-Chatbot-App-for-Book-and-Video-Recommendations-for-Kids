package com.kidsrec.chatbot.ui.approval

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// Dialog shown when a child tries to access content that requires parent approval
@Composable
fun ContentApprovalDialog(
    contentTitle: String,
    contentType: String,
    onRequestApproval: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        // Allows the dialog to be dismissed when the user taps outside or presses back
        onDismissRequest = onDismiss,
        // Lock icon visually indicates restricted content
        icon = {
            Icon(
                Icons.Default.Lock,
                contentDescription = null,
                modifier = Modifier.size(40.dp),
                tint = MaterialTheme.colorScheme.primary
            )
        },
        // Main dialog title
        title = {
            Text(
                text = "Parent Approval Required",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
        },
        // Dialog message explaining why approval is needed
        text = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                // Explanation shown to the child
                Text(
                    text = "Your parent needs to approve this $contentType before you can access it:",
                    textAlign = TextAlign.Center,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))
                // Displays the specific content title requiring approval
                Text(
                    text = contentTitle,
                    fontWeight = FontWeight.Medium,
                    fontSize = 16.sp,
                    textAlign = TextAlign.Center
                )
            }
        },
        // Sends an approval request to the parent
        confirmButton = {
            Button(onClick = onRequestApproval) {
                Text("Request Approval")
            }
        },
        // Closes the dialog without sending a request
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("Maybe Later")
            }
        }
    )
}
