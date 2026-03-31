package com.kidsrec.chatbot.ui.auth

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun EmailVerificationScreen(
    authViewModel: AuthViewModel
) {
    var resent by remember { mutableStateOf(false) }
    val verificationMessage by authViewModel.verificationMessage.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            Icons.Default.Email,
            contentDescription = null,
            modifier = Modifier.size(80.dp),
            tint = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "Verify Your Email",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = "We sent a verification link to your email. Please check your inbox and click the link to verify your account.",
            fontSize = 15.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )

        // Show feedback message
        verificationMessage?.let { message ->
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = message,
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.error,
                textAlign = TextAlign.Center
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        // Check verification button
        Button(
            onClick = { authViewModel.checkEmailVerified() },
            modifier = Modifier.fillMaxWidth().height(50.dp)
        ) {
            Icon(Icons.Default.Refresh, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("I've Verified — Continue", fontSize = 16.sp)
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Resend email button
        OutlinedButton(
            onClick = {
                authViewModel.resendVerificationEmail()
                resent = true
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(if (resent) "Email Resent!" else "Resend Verification Email")
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Sign out / go back
        TextButton(onClick = { authViewModel.signOut() }) {
            Text("Use a Different Account")
        }
    }
}
