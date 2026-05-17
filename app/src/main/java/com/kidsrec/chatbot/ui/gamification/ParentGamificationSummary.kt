package com.kidsrec.chatbot.ui.gamification

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

// Summary card shown to parents to view a child's reward progress
@Composable
fun ParentGamificationSummary(
    viewModel: GamificationViewModel,
    childUserId: String
) {
    // Starts observing the selected child's gamification profile and badges
    LaunchedEffect(childUserId) {
        viewModel.observeChildGamification(childUserId)
    }

    // Observes the child's points and level
    val profile by viewModel.profile.collectAsState()

    // Observes the child's unlocked badges
    val badges by viewModel.badges.collectAsState()

    // Card container for the reward summary section
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Section title
            Text(
                text = "Reward Summary",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Displays points, level, and badge count side by side
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                SummaryMetric("Points", profile.totalPoints.toString(), Icons.Default.Star)
                SummaryMetric("Level", profile.currentLevel.toString(), Icons.Default.TrendingUp)
                SummaryMetric("Badges", badges.size.toString(), Icons.Default.EmojiEvents)
            }
        }
    }
}

// Small reusable metric block used inside the parent reward summary card
@Composable
private fun SummaryMetric(
    label: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector
) {
    Column {
        // Metric icon
        Icon(icon, contentDescription = null)
        Spacer(modifier = Modifier.height(4.dp))
        // Main metric value
        Text(text = value, fontWeight = FontWeight.Bold)
        // Metric label
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall
        )
    }
}