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

@Composable
fun ScreenTimeWrapper(
    content: @Composable () -> Unit
) {
    val viewModel: ScreenTimeViewModel = hiltViewModel()

    // screen time states
    val isTimeLimitReached by viewModel.isTimeLimitReached.collectAsState()
    val todaySession by viewModel.todaySession.collectAsState()
    val screenTimeConfig by viewModel.screenTimeConfig.collectAsState()

    // start real-time tracking
    DisposableEffect(Unit) {
        viewModel.startTracking()

        onDispose {
            viewModel.stopTracking()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {

        // show app only if time is not finished
        if (!isTimeLimitReached) {
            content()
        }

        // block whole child app when time is finished
        if (isTimeLimitReached) {
            TimeLimitReachedScreen(
                usedMinutes = todaySession?.totalMinutes ?: 0,
                limitMinutes = screenTimeConfig.dailyLimitMinutes + (todaySession?.bonusMinutes ?: 0)
            )
        }
    }
}

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

        // big sad dino
        Image(
            painter = painterResource(id = R.drawable.sad_dino),
            contentDescription = "Sad dino",
            modifier = Modifier.size(280.dp)
        )

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "Time's Up!",
            fontSize = 34.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = "Your screen time is finished for today 😢",
            fontSize = 18.sp,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Come back tomorrow or ask your parent for more time.",
            fontSize = 15.sp,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(20.dp))

        Text(
            text = "Used: $usedMinutes / $limitMinutes mins",
            fontSize = 13.sp
        )

        Spacer(modifier = Modifier.height(28.dp))

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