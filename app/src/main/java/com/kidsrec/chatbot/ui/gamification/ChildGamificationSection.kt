package com.kidsrec.chatbot.ui.gamification

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.kidsrec.chatbot.data.model.BadgeUnlock
import kotlinx.coroutines.delay

@Composable
fun ChildGamificationSection(
    viewModel: GamificationViewModel,
    childUserId: String,
    onOpenFullPage: (() -> Unit)? = null
) {
    LaunchedEffect(childUserId) {
        viewModel.observeChildGamification(childUserId)
    }

    val profile by viewModel.profile.collectAsState()
    val badges by viewModel.badges.collectAsState()
    val celebration by viewModel.celebration.collectAsState()

    Box(modifier = Modifier.fillMaxWidth()) {
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            HeroRewardsHeader()

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                RewardSummaryCard(
                    modifier = Modifier.weight(1f),
                    title = "Points",
                    value = profile.totalPoints.toString(),
                    icon = "⭐",
                    bgColor = Color(0xFFFFF3E0),
                    iconColor = Color(0xFFEF6C00)
                )

                RewardSummaryCard(
                    modifier = Modifier.weight(1f),
                    title = "Level",
                    value = profile.currentLevel.toString(),
                    icon = "🚀",
                    bgColor = Color(0xFFE8F5E9),
                    iconColor = Color(0xFF2E7D32)
                )

                RewardSummaryCard(
                    modifier = Modifier.weight(1f),
                    title = "Badges",
                    value = badges.size.toString(),
                    icon = "🏅",
                    bgColor = Color(0xFFF3E5F5),
                    iconColor = Color(0xFF8E24AA)
                )
            }

            NextRewardCard(
                currentPoints = profile.totalPoints,
                currentLevel = profile.currentLevel
            )

            if (badges.isEmpty()) {
                EmptyBadgeState()
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Latest Badges",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        items(badges.take(5)) { badge ->
                            BadgeCard(badge = badge)
                        }
                    }
                }
            }

            if (onOpenFullPage != null) {
                TextButton(onClick = onOpenFullPage) {
                    Text("Open Full Rewards Page")
                }
            }
        }

        if (celebration.type != RewardCelebrationType.NONE) {
            RewardConfettiOverlay()
            RewardPopup(
                celebration = celebration,
                onDismiss = { viewModel.clearCelebration() }
            )
        }
    }
}

@Composable
private fun HeroRewardsHeader() {
    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFE8F5FF)),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = CircleShape,
                color = Color.White.copy(alpha = 0.95f)
            ) {
                Icon(
                    imageVector = Icons.Default.AutoAwesome,
                    contentDescription = null,
                    tint = Color(0xFF1976D2),
                    modifier = Modifier.padding(12.dp)
                )
            }

            Column {
                Text(
                    text = "Rewards & Badges",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Read, watch, explore — and unlock fun surprises!",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun RewardSummaryCard(
    modifier: Modifier = Modifier,
    title: String,
    value: String,
    icon: String,
    bgColor: Color,
    iconColor: Color
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = bgColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Surface(
                color = Color.White.copy(alpha = 0.9f),
                shape = RoundedCornerShape(14.dp)
            ) {
                Text(
                    text = icon,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                    color = iconColor
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = value,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.ExtraBold,
                color = iconColor
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                color = Color.Black.copy(alpha = 0.75f)
            )
        }
    }
}

@Composable
private fun NextRewardCard(
    currentPoints: Int,
    currentLevel: Int
) {
    val nextLevelPoints = when (currentLevel) {
        1 -> 80
        2 -> 180
        3 -> 300
        else -> 300
    }

    val remaining = (nextLevelPoints - currentPoints).coerceAtLeast(0)
    val progress = when {
        currentLevel >= 4 -> 1f
        nextLevelPoints == 0 -> 1f
        else -> currentPoints.toFloat() / nextLevelPoints.toFloat()
    }.coerceIn(0f, 1f)

    Card(
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFFBFF))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Next Reward Goal",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = if (currentLevel >= 4) {
                    "Amazing! You reached the top reward level."
                } else {
                    "$remaining more points to reach Level ${currentLevel + 1}"
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(12.dp))

            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(10.dp),
                color = Color(0xFFFFC107),
                trackColor = Color(0xFFFFF8E1)
            )
        }
    }
}

@Composable
private fun BadgeCard(badge: BadgeUnlock) {
    val badgeEmoji = badgeEmojiFor(badge)

    Card(
        modifier = Modifier.widthIn(min = 180.dp, max = 220.dp),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFFBFF)),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .background(
                        color = Color(0xFFFFF8E1),
                        shape = RoundedCornerShape(22.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = badgeEmoji,
                    style = MaterialTheme.typography.headlineMedium
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = badge.badgeTitle,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = badge.description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

private fun badgeEmojiFor(badge: BadgeUnlock): String {
    val id = badge.badgeId.lowercase()
    return when {
        "animals" in id -> "🦁"
        "space" in id -> "🚀"
        "dino" in id -> "🦖"
        "videos" in id -> "🎬"
        "books" in id -> "📚"
        "topics" in id -> "💡"
        else -> "🏅"
    }
}

@Composable
private fun EmptyBadgeState() {
    Card(
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFFBFF))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "🏅",
                style = MaterialTheme.typography.headlineMedium
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "No badges yet",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "Read books, watch videos, and explore topics to unlock your first badge.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun RewardPopup(
    celebration: RewardCelebration,
    onDismiss: () -> Unit
) {
    val scale = remember { Animatable(0.8f) }

    LaunchedEffect(celebration) {
        scale.animateTo(
            targetValue = 1f,
            animationSpec = spring(
                dampingRatio = 0.55f,
                stiffness = 220f
            )
        )
        delay(2600)
        onDismiss()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.22f)),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .padding(24.dp)
                .scale(scale.value),
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFFFFFBFF)),
            elevation = CardDefaults.cardElevation(defaultElevation = 10.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = if (celebration.type == RewardCelebrationType.BADGE) "🏅" else "🎉",
                    style = MaterialTheme.typography.headlineLarge
                )

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = celebration.title,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.ExtraBold
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = celebration.message,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = celebration.subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(12.dp))

                TextButton(onClick = onDismiss) {
                    Text("Yay!")
                }
            }
        }
    }
}

@Composable
fun RewardConfettiOverlay() {
    val infinite = rememberInfiniteTransition(label = "confetti")

    val fall1 by infinite.animateFloat(
        initialValue = -60f,
        targetValue = 700f,
        animationSpec = infiniteRepeatable(
            animation = keyframes {
                durationMillis = 1700
                700f at 1700 using FastOutSlowInEasing
            },
            repeatMode = RepeatMode.Restart
        ),
        label = "fall1"
    )

    val fall2 by infinite.animateFloat(
        initialValue = -140f,
        targetValue = 760f,
        animationSpec = infiniteRepeatable(
            animation = keyframes {
                durationMillis = 2100
                760f at 2100 using FastOutSlowInEasing
            },
            repeatMode = RepeatMode.Restart
        ),
        label = "fall2"
    )

    val fall3 by infinite.animateFloat(
        initialValue = -100f,
        targetValue = 740f,
        animationSpec = infiniteRepeatable(
            animation = keyframes {
                durationMillis = 1950
                740f at 1950 using FastOutSlowInEasing
            },
            repeatMode = RepeatMode.Restart
        ),
        label = "fall3"
    )

    Box(modifier = Modifier.fillMaxSize()) {
        ConfettiDot(20.dp, fall1.dp, Color(0xFFFF1744), 14.dp)
        ConfettiDot(60.dp, (fall2 * 0.8f).dp, Color(0xFFFFC400), 10.dp)
        ConfettiDot(110.dp, fall3.dp, Color(0xFF00E5FF), 12.dp)
        ConfettiDot(150.dp, (fall1 * 0.9f).dp, Color(0xFF76FF03), 11.dp)
        ConfettiDot(200.dp, fall2.dp, Color(0xFFE040FB), 13.dp)
        ConfettiDot(240.dp, (fall3 * 0.7f).dp, Color(0xFFFF9100), 10.dp)
        ConfettiDot(280.dp, (fall1 * 1.05f).dp, Color(0xFF18FFFF), 12.dp)
        ConfettiDot(320.dp, fall2.dp, Color(0xFFFF4081), 14.dp)
        ConfettiDot(360.dp, (fall3 * 0.95f).dp, Color(0xFF69F0AE), 11.dp)
    }
}

@Composable
private fun ConfettiDot(
    xOffset: Dp,
    yOffset: Dp,
    color: Color,
    size: Dp
) {
    Box(
        modifier = Modifier
            .offset(x = xOffset, y = yOffset)
            .size(size)
            .background(color, CircleShape)
            .alpha(0.95f)
    )
}