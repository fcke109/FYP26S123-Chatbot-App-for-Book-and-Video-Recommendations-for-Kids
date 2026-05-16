package com.kidsrec.chatbot.ui.screentime

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.material3.Button
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kidsrec.chatbot.R
import com.kidsrec.chatbot.ui.auth.AuthViewModel

// Wrapper that monitors the child's screen time while displaying the app content
@Composable
fun ScreenTimeWrapper(
    content: @Composable () -> Unit
) {
    val viewModel: ScreenTimeViewModel = hiltViewModel()

    // Observes whether the child has reached the daily screen time limit
    val isTimeLimitReached by viewModel.isTimeLimitReached.collectAsState()
    // Observes today's screen time session record
    val todaySession by viewModel.todaySession.collectAsState()
    // Observes the parent's configured screen time settings
    val screenTimeConfig by viewModel.screenTimeConfig.collectAsState()

    // start real-time tracking
    DisposableEffect(Unit) {
        viewModel.startTracking()

        // Stops tracking when the wrapper is removed from the screen
        onDispose {
            viewModel.stopTracking()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {

        // Shows the normal app content only if the child still has available screen time
        if (!isTimeLimitReached) {
            content()
        }

        // Blocks the app and shows the time limit screen once the daily limit is reached
        if (isTimeLimitReached) {
            TimeLimitReachedScreen(
                usedMinutes = todaySession?.totalMinutes ?: 0,
                limitMinutes = screenTimeConfig.dailyLimitMinutes + (todaySession?.bonusMinutes ?: 0)
            )
        }
    }
}

// Screen displayed when the child has used up their allowed screen time
@Composable
private fun TimeLimitReachedScreen(
    usedMinutes: Int,
    limitMinutes: Int
) {
    val authViewModel: AuthViewModel = hiltViewModel()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {

        // Displays sad little dino image to make the message child-friendly
        Image(
            painter = painterResource(id = R.drawable.sad_dino),
            contentDescription = "Sad dino",
            modifier = Modifier.size(280.dp)
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Main warning title
        Text(
            text = "Time's Up!",
            fontSize = 34.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Explains that today's screen time has been used up
        Text(
            text = "Your screen time is finished for today 😢",
            fontSize = 18.sp,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Suggests returning later or asking a parent for extra time
        Text(
            text = "Come back tomorrow or ask your parent for more time.",
            fontSize = 15.sp,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(20.dp))

        // Shows screen time usage progress for the day
        Text(
            text = "Used: $usedMinutes / $limitMinutes mins",
            fontSize = 13.sp
        )

        Spacer(modifier = Modifier.height(28.dp))

        // Allows the child to log out after the time limit is reached
        Button(
            onClick = {
                authViewModel.signOut()
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Logout")
        }
    }
}