package com.kidsrec.chatbot.ui.gamification

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.kidsrec.chatbot.R
import com.kidsrec.chatbot.data.model.BadgeUnlock
import kotlinx.coroutines.delay

private data class BRLockedBadge(
    val title: String,
    val description: String,
    val required: Int,
    val category: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BadgesRewardsScreen(
    childUserId: String,
    childName: String,
    onBack: () -> Unit,
    viewModel: GamificationViewModel = hiltViewModel()
) {
    LaunchedEffect(childUserId) {
        viewModel.observeChildGamification(childUserId)
        viewModel.refresh(childUserId)
    }

    val profile by viewModel.profile.collectAsState()
    val badges by viewModel.badges.collectAsState()
    val celebration by viewModel.celebration.collectAsState()

    val safeLevel = if (profile.currentLevel <= 0) 1 else profile.currentLevel
    val lockedBadges = brSampleLockedBadges(badges)

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Badges & Rewards") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFFF8FBFF),
                            Color(0xFFFFFBFF),
                            Color(0xFFFFF8EC)
                        )
                    )
                )
        ) {
            BRFloatingStars()

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Box(modifier = Modifier.fillMaxWidth()) {
                    BRRewardsHeroCard(
                        childName = childName,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(top = 8.dp, end = 8.dp)
                    ) {
                        BRFloatingDinoMascot()
                    }
                }

                BRRewardsStatsRow(
                    points = profile.totalPoints,
                    level = safeLevel,
                    badgeCount = badges.size
                )

                BRRewardJourneyCard(
                    currentPoints = profile.totalPoints,
                    currentLevel = safeLevel
                )

                BRSectionTitle("Earned Badges")

                if (badges.isEmpty()) {
                    BREmptyRewardsState()
                } else {
                    badges.forEach { badge ->
                        BRFullBadgeCard(badge = badge)
                    }
                }

                BRSectionTitle("Locked Badges")

                if (lockedBadges.isEmpty()) {
                    Card(
                        shape = RoundedCornerShape(22.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFE8F5E9))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(18.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text("🌟", style = MaterialTheme.typography.headlineMedium)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "You unlocked all preview badges!",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                } else {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        lockedBadges.forEach { badge ->
                            BRLockedBadgeCard(badge)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
            }

            if (celebration.type != RewardCelebrationType.NONE) {
                BRRewardConfettiOverlay()
                BRRewardPopup(
                    celebration = celebration,
                    onDismiss = { viewModel.clearCelebration() }
                )
            }
        }
    }
}

@Composable
private fun BRRewardsHeroCard(
    childName: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = Brush.linearGradient(
                        colors = listOf(
                            Color(0xFFE3F2FD),
                            Color(0xFFF3E5F5),
                            Color(0xFFFFF8E1)
                        )
                    )
                )
                .padding(22.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Surface(
                    shape = CircleShape,
                    color = Color.White.copy(alpha = 0.95f)
                ) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = null,
                        tint = Color(0xFF1976D2),
                        modifier = Modifier.padding(14.dp)
                    )
                }

                Text(
                    text = "$childName's Reward World",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.ExtraBold
                )

                Text(
                    text = "Collect points, unlock badges, and level up through reading and exploring.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun BRRewardsStatsRow(
    points: Int,
    level: Int,
    badgeCount: Int
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        BRStatCard(
            modifier = Modifier.weight(1f),
            title = "Points",
            value = points.toString(),
            icon = Icons.Default.Star,
            containerColor = Color(0xFFFFF3E0),
            iconTint = Color(0xFFEF6C00)
        )

        BRStatCard(
            modifier = Modifier.weight(1f),
            title = "Level",
            value = level.toString(),
            icon = Icons.AutoMirrored.Filled.TrendingUp,
            containerColor = Color(0xFFE8F5E9),
            iconTint = Color(0xFF2E7D32)
        )

        BRStatCard(
            modifier = Modifier.weight(1f),
            title = "Badges",
            value = badgeCount.toString(),
            icon = Icons.Default.EmojiEvents,
            containerColor = Color(0xFFF3E5F5),
            iconTint = Color(0xFF8E24AA)
        )
    }
}

@Composable
private fun BRStatCard(
    modifier: Modifier = Modifier,
    title: String,
    value: String,
    icon: ImageVector,
    containerColor: Color,
    iconTint: Color
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Surface(
                color = Color.White.copy(alpha = 0.95f),
                shape = RoundedCornerShape(16.dp)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.padding(10.dp)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = value,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.ExtraBold,
                color = iconTint
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun BRRewardJourneyCard(
    currentPoints: Int,
    currentLevel: Int
) {
    val nextLevelPoints = when (currentLevel) {
        1 -> 80
        2 -> 180
        3 -> 300
        4 -> 450
        else -> 450
    }

    val remaining = (nextLevelPoints - currentPoints).coerceAtLeast(0)

    val progress = if (currentLevel >= 5) {
        1f
    } else {
        (currentPoints.toFloat() / nextLevelPoints.toFloat()).coerceIn(0f, 1f)
    }

    Card(
        shape = RoundedCornerShape(26.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFFBFF))
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Text(
                text = "Journey to the Next Reward",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = if (currentLevel >= 5) {
                    "Amazing! You already reached the top reward level."
                } else {
                    "$remaining more points to reach Level ${currentLevel + 1}"
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(14.dp))

            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(12.dp),
                color = Color(0xFFFFC107),
                trackColor = Color(0xFFFFF8E1)
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "Keep reading books and watching learning videos to earn more points.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun BRSectionTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleLarge,
        fontWeight = FontWeight.ExtraBold,
        modifier = Modifier.fillMaxWidth()
    )
}

@Composable
private fun BRFullBadgeCard(badge: BadgeUnlock) {
    val emoji = brBadgeEmoji(badge)

    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = brBadgeColor(badge)),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(74.dp)
                    .background(
                        color = Color.White.copy(alpha = 0.65f),
                        shape = RoundedCornerShape(22.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = emoji,
                    style = MaterialTheme.typography.headlineMedium
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = badge.badgeTitle,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.ExtraBold
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = badge.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = brBadgeLabel(badge),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = brBadgeAccentColor(badge)
                )
            }
        }
    }
}

@Composable
private fun BRLockedBadgeCard(badge: BRLockedBadge) {
    Card(
        modifier = Modifier.widthIn(min = 180.dp, max = 220.dp),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = brLockedBadgeColor(badge.category))
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Surface(
                color = Color.White.copy(alpha = 0.9f),
                shape = RoundedCornerShape(18.dp)
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = null,
                        tint = Color(0xFF616161)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = brLockedBadgeEmoji(badge.category),
                        fontSize = 28.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = badge.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = badge.description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Unlock at ${badge.required} points",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF616161)
            )
        }
    }
}

@Composable
private fun BREmptyRewardsState() {
    Card(
        shape = RoundedCornerShape(26.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFFBFF))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Default.EmojiEvents,
                contentDescription = null,
                tint = Color(0xFFFFB300),
                modifier = Modifier.size(56.dp)
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "No badges yet",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.ExtraBold
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Start reading, learning, and exploring to unlock your first reward badge.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun BRFloatingDinoMascot() {
    val infinite = rememberInfiniteTransition(label = "dino")

    val offsetY by infinite.animateFloat(
        initialValue = 0f,
        targetValue = -18f,
        animationSpec = infiniteRepeatable(
            animation = tween(1300, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "dinoFloat"
    )

    val rotate by infinite.animateFloat(
        initialValue = -4f,
        targetValue = 4f,
        animationSpec = infiniteRepeatable(
            animation = tween(1100, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "dinoRotate"
    )

    Image(
        painter = painterResource(id = R.drawable.dino_badge),
        contentDescription = "Dino mascot",
        modifier = Modifier
            .size(94.dp)
            .offset(y = offsetY.dp)
            .rotate(rotate)
    )
}

@Composable
private fun BRFloatingStars() {
    val infinite = rememberInfiniteTransition(label = "stars")

    val y1 by infinite.animateFloat(
        initialValue = -120f,
        targetValue = 950f,
        animationSpec = infiniteRepeatable(
            animation = tween(4200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "starY1"
    )

    val y2 by infinite.animateFloat(
        initialValue = -260f,
        targetValue = 980f,
        animationSpec = infiniteRepeatable(
            animation = tween(5200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "starY2"
    )

    val y3 by infinite.animateFloat(
        initialValue = -180f,
        targetValue = 900f,
        animationSpec = infiniteRepeatable(
            animation = tween(4700, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "starY3"
    )

    Box(modifier = Modifier.fillMaxSize()) {
        Text("⭐", modifier = Modifier.offset(x = 30.dp, y = y1.dp), fontSize = 18.sp)
        Text("✨", modifier = Modifier.offset(x = 110.dp, y = y2.dp), fontSize = 16.sp)
        Text("⭐", modifier = Modifier.offset(x = 220.dp, y = (y1 * 0.75f).dp), fontSize = 15.sp)
        Text("✨", modifier = Modifier.offset(x = 300.dp, y = (y3 * 0.85f).dp), fontSize = 17.sp)
        Text("⭐", modifier = Modifier.offset(x = 160.dp, y = y3.dp), fontSize = 14.sp)
        Text("✨", modifier = Modifier.offset(x = 340.dp, y = (y2 * 0.65f).dp), fontSize = 16.sp)
    }
}

@Composable
private fun BRRewardPopup(
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
private fun BRRewardConfettiOverlay() {
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
        BRConfettiDot(20.dp, fall1.dp, Color(0xFFFF1744), 14.dp)
        BRConfettiDot(60.dp, (fall2 * 0.8f).dp, Color(0xFFFFC400), 10.dp)
        BRConfettiDot(110.dp, fall3.dp, Color(0xFF00E5FF), 12.dp)
        BRConfettiDot(150.dp, (fall1 * 0.9f).dp, Color(0xFF76FF03), 11.dp)
        BRConfettiDot(200.dp, fall2.dp, Color(0xFFE040FB), 13.dp)
        BRConfettiDot(240.dp, (fall3 * 0.7f).dp, Color(0xFFFF9100), 10.dp)
        BRConfettiDot(280.dp, (fall1 * 1.05f).dp, Color(0xFF18FFFF), 12.dp)
        BRConfettiDot(320.dp, fall2.dp, Color(0xFFFF4081), 14.dp)
        BRConfettiDot(360.dp, (fall3 * 0.95f).dp, Color(0xFF69F0AE), 11.dp)
    }
}

@Composable
private fun BRConfettiDot(
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

private fun brBadgeEmoji(badge: BadgeUnlock): String {
    val id = badge.badgeId.lowercase()
    return when {
        "animals" in id || "animal" in id -> "🦁"
        "space" in id -> "🚀"
        "dino" in id -> "🦖"
        "videos" in id || "video" in id -> "🎬"
        "books" in id || "book" in id || "reading" in id -> "📚"
        "topics" in id -> "💡"
        else -> "🏅"
    }
}

private fun brBadgeColor(badge: BadgeUnlock): Color {
    val id = badge.badgeId.lowercase()
    return when {
        "space" in id -> Color(0xFFE3F2FD)
        "animals" in id || "animal" in id -> Color(0xFFE8F5E9)
        "dino" in id -> Color(0xFFFFF3E0)
        "videos" in id || "video" in id -> Color(0xFFFFEBEE)
        "books" in id || "book" in id || "reading" in id -> Color(0xFFF3E5F5)
        else -> Color(0xFFFFFBFF)
    }
}

private fun brBadgeAccentColor(badge: BadgeUnlock): Color {
    val id = badge.badgeId.lowercase()
    return when {
        "space" in id -> Color(0xFF1976D2)
        "animals" in id || "animal" in id -> Color(0xFF2E7D32)
        "dino" in id -> Color(0xFFEF6C00)
        "videos" in id || "video" in id -> Color(0xFFC62828)
        "books" in id || "book" in id || "reading" in id -> Color(0xFF8E24AA)
        else -> Color(0xFF616161)
    }
}

private fun brBadgeLabel(badge: BadgeUnlock): String {
    val id = badge.badgeId.lowercase()
    return when {
        "space" in id -> "Space Badge"
        "animals" in id || "animal" in id -> "Animal Badge"
        "dino" in id -> "Dino Badge"
        "videos" in id || "video" in id -> "Video Badge"
        "books" in id || "book" in id || "reading" in id -> "Reading Badge"
        else -> "Special Badge"
    }
}

private fun brLockedBadgeColor(category: String): Color {
    return when (category.lowercase()) {
        "space" -> Color(0xFFE3F2FD)
        "animals" -> Color(0xFFE8F5E9)
        "dino" -> Color(0xFFFFF3E0)
        "videos" -> Color(0xFFFFEBEE)
        "books" -> Color(0xFFF3E5F5)
        else -> Color(0xFFE0E0E0)
    }
}

private fun brLockedBadgeEmoji(category: String): String {
    return when (category.lowercase()) {
        "space" -> "🚀"
        "animals" -> "🦁"
        "dino" -> "🦖"
        "videos" -> "🎬"
        "books" -> "📚"
        else -> "🏅"
    }
}

private fun brSampleLockedBadges(unlockedBadges: List<BadgeUnlock>): List<BRLockedBadge> {
    val unlockedIds = unlockedBadges.map { it.badgeId.lowercase() }.toSet()

    val allPreviewBadges = listOf(
        BRLockedBadge(
            title = "Space Explorer",
            description = "Explore amazing space topics and stories.",
            required = 100,
            category = "space"
        ),
        BRLockedBadge(
            title = "Animal Hero",
            description = "Read fun animal books and adventures.",
            required = 150,
            category = "animals"
        ),
        BRLockedBadge(
            title = "Dino Master",
            description = "Discover dinosaurs and prehistoric fun.",
            required = 200,
            category = "dino"
        ),
        BRLockedBadge(
            title = "Video Star",
            description = "Watch and learn from great educational videos.",
            required = 250,
            category = "videos"
        ),
        BRLockedBadge(
            title = "Book Wizard",
            description = "Finish lots of reading journeys.",
            required = 300,
            category = "books"
        )
    )

    return allPreviewBadges.filterNot { locked ->
        unlockedIds.any { id ->
            when (locked.category) {
                "space" -> "space" in id
                "animals" -> "animal" in id || "animals" in id
                "dino" -> "dino" in id
                "videos" -> "video" in id || "videos" in id
                "books" -> "book" in id || "books" in id || "reading" in id
                else -> false
            }
        }
    }
}