package com.kidsrec.chatbot.ui.profile

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Cake
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.InputChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.kidsrec.chatbot.R
import com.kidsrec.chatbot.data.model.ReadingHistory
import com.kidsrec.chatbot.ui.auth.AuthViewModel
import com.kidsrec.chatbot.ui.gamification.ChildGamificationSection
import com.kidsrec.chatbot.ui.gamification.GamificationViewModel
import com.kidsrec.chatbot.ui.parental.ChildSafetyLockGate
import com.kidsrec.chatbot.ui.parental.ChildSettingsEntry
import kotlin.math.sin

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ProfileScreen(
    authViewModel: AuthViewModel,
    profileViewModel: ProfileViewModel,
    onItemClick: (url: String, title: String, isVideo: Boolean) -> Unit = { _, _, _ -> },
    onNavigateToParentalControls: () -> Unit = {},
    onNavigateToBadgesRewards: () -> Unit = {}
) {
    val user by authViewModel.currentUser.collectAsState()
    val updateSuccess by profileViewModel.updateSuccess.collectAsState()
    val readingHistory by profileViewModel.readingHistory.collectAsState()

    var isEditing by remember { mutableStateOf(false) }
    var name by remember { mutableStateOf("") }
    var age by remember { mutableStateOf("") }
    var selectedInterests by remember { mutableStateOf(setOf<String>()) }
    var readingLevel by remember { mutableStateOf("Beginner") }

    var showEditLockGate by remember { mutableStateOf(false) }
    var isParentalUnlocked by remember { mutableStateOf(false) }

    val interests = listOf(
        "Reading", "Science", "Animals", "Adventure",
        "Fantasy", "Art", "Music", "Sports", "History", "Nature",
        "Space", "Dinosaurs", "Cooking", "Cars", "Robots",
        "Fairy Tales", "Superheroes", "Ocean", "Puzzles", "Travel"
    )
    var interestsExpanded by remember { mutableStateOf(false) }

    val readingLevels = listOf("Beginner", "Early Reader", "Intermediate", "Advanced")

    LaunchedEffect(user) {
        user?.let {
            name = it.name
            age = it.age.toString()
            selectedInterests = it.interests.toSet()
            readingLevel = it.readingLevel
        }
    }

    LaunchedEffect(updateSuccess) {
        if (updateSuccess) {
            isEditing = false
            profileViewModel.resetUpdateSuccess()
        }
    }

    if (showEditLockGate) {
        ChildSafetyLockGate(
            isLocked = user?.parentalPin?.isNotEmpty() == true,
            onAccessGranted = {
                showEditLockGate = false
                isEditing = true
            },
            content = {
                LaunchedEffect(Unit) {
                    showEditLockGate = false
                    isEditing = true
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Profile") },
                actions = {
                    if (!isEditing && user?.isGuest != true) {
                        IconButton(onClick = { showEditLockGate = true }) {
                            Icon(Icons.Default.Edit, contentDescription = "Edit Profile")
                        }
                    }
                    IconButton(onClick = { authViewModel.signOut() }) {
                        Icon(Icons.Default.Logout, contentDescription = "Logout")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Surface(
                color = MaterialTheme.colorScheme.primaryContainer,
                shape = MaterialTheme.shapes.large,
                modifier = Modifier.size(100.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = name.firstOrNull()?.uppercase() ?: "?",
                        fontSize = 48.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            if (isEditing) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Name") },
                    leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = age,
                    onValueChange = { if (it.isEmpty() || it.all { char -> char.isDigit() }) age = it },
                    label = { Text("Age") },
                    leadingIcon = { Icon(Icons.Default.Cake, contentDescription = null) },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(24.dp))

                Text(
                    text = "Interests",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.align(Alignment.Start)
                )

                Spacer(modifier = Modifier.height(12.dp))

                if (selectedInterests.isNotEmpty()) {
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        selectedInterests.forEach { interest ->
                            InputChip(
                                selected = true,
                                onClick = { selectedInterests = selectedInterests - interest },
                                label = { Text(interest) },
                                trailingIcon = {
                                    Icon(
                                        Icons.Default.Close,
                                        contentDescription = "Remove $interest",
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }

                Box(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = if (selectedInterests.isEmpty()) "" else "${selectedInterests.size} selected",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Tap to pick interests") },
                        trailingIcon = {
                            Icon(Icons.Default.ExpandMore, contentDescription = "Open interests")
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { interestsExpanded = true }
                    )

                    DropdownMenu(
                        expanded = interestsExpanded,
                        onDismissRequest = { interestsExpanded = false },
                        modifier = Modifier.fillMaxWidth(0.92f)
                    ) {
                        interests.forEach { interest ->
                            val isSelected = selectedInterests.contains(interest)
                            DropdownMenuItem(
                                text = {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Checkbox(
                                            checked = isSelected,
                                            onCheckedChange = null
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(interest)
                                    }
                                },
                                onClick = {
                                    selectedInterests = if (isSelected) {
                                        selectedInterests - interest
                                    } else {
                                        selectedInterests + interest
                                    }
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Reading Level",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.align(Alignment.Start)
                )

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    readingLevels.forEach { level ->
                        FilterChip(
                            selected = readingLevel == level,
                            onClick = { readingLevel = level },
                            label = { Text(level, fontSize = 12.sp) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = { isEditing = false },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Cancel")
                    }

                    Button(
                        onClick = {
                            profileViewModel.updateProfile(
                                name = name,
                                age = age.toIntOrNull() ?: 0,
                                interests = selectedInterests.toList(),
                                readingLevel = readingLevel
                            )
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Save")
                    }
                }
            } else {
                user?.let { currentUser ->
                    if (currentUser.isGuest) {
                        Text(
                            text = "Guest",
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Bold
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = "You're browsing as a guest",
                            fontSize = 16.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Spacer(modifier = Modifier.height(24.dp))

                        Card(modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    text = "Create an account to unlock:",
                                    fontWeight = FontWeight.Medium,
                                    fontSize = 16.sp
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(text = "- Save your favorite books and videos")
                                Text(text = "- Get personalized recommendations")
                                Text(text = "- Track your reading history")
                            }
                        }
                    } else {
                        Text(
                            text = currentUser.name,
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Bold
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = "${currentUser.age} years old",
                            fontSize = 16.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Spacer(modifier = Modifier.height(24.dp))

                        Card(
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Email, contentDescription = null)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(text = currentUser.email)
                                }

                                Spacer(modifier = Modifier.height(16.dp))

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.MenuBook, contentDescription = null)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(text = "Reading Level: ${currentUser.readingLevel}")
                                }

                                Spacer(modifier = Modifier.height(16.dp))

                                Text(
                                    text = "Interests",
                                    fontWeight = FontWeight.Medium,
                                    fontSize = 16.sp
                                )

                                Spacer(modifier = Modifier.height(8.dp))

                                FlowRow(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    currentUser.interests.forEach { interest ->
                                        AssistChip(
                                            onClick = { },
                                            label = { Text(interest) }
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        BadgesRewardsEntryCard(
                            onClick = onNavigateToBadgesRewards
                        )

                        Spacer(modifier = Modifier.height(24.dp))

                        ChildSettingsEntry(
                            lockEnabled = currentUser.parentalPin?.isNotEmpty() == true,
                            isUnlocked = isParentalUnlocked,
                            onRequestUnlock = {
                                onNavigateToParentalControls()
                            }
                        ) {
                            OutlinedButton(
                                onClick = onNavigateToParentalControls,
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Default.Settings, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Parental Controls")
                            }
                        }

                        if (readingHistory.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(24.dp))
                            RecentlyReadSection(readingHistory, onItemClick)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun BadgesRewardsEntryCard(
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .shadow(10.dp, RoundedCornerShape(28.dp)),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFFFFE78F)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 10.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.linearGradient(
                        colors = listOf(
                            Color(0xFFFFF3A6),
                            Color(0xFFFFDEB0),
                            Color(0xFFFFD3E6)
                        )
                    )
                )
                .padding(20.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                CuteFlappyDino(
                    modifier = Modifier.size(82.dp)
                )

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "My Dino Rewards",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFF6D4C41)
                    )
                    Text(
                        text = "Open your magical prize world!",
                        fontSize = 14.sp,
                        color = Color(0xFF8D6E63)
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            Button(
                onClick = onClick,
                shape = RoundedCornerShape(18.dp)
            ) {
                Text(text = "✨")
                Spacer(modifier = Modifier.width(8.dp))
                Text("Open Rewards Page")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BadgesRewardsScreen(
    childUserId: String,
    childName: String,
    onBack: () -> Unit
) {
    val gamificationViewModel: GamificationViewModel = hiltViewModel()

    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        FunRewardsBackground()

        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            text = "Dino Rewards",
                            fontWeight = FontWeight.ExtraBold
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent
                    )
                )
            }
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 12.dp)
            ) {
                RewardsHeroCard(childName = childName)

                Spacer(modifier = Modifier.height(18.dp))

                RewardsSummaryRow()

                Spacer(modifier = Modifier.height(18.dp))

                FunSectionCard(
                    title = "Super Dino Achievements",
                    subtitle = "Read, watch, and win shiny surprises!"
                ) {
                    ChildGamificationSection(
                        viewModel = gamificationViewModel,
                        childUserId = childUserId
                    )
                }

                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}

@Composable
private fun FunRewardsBackground() {
    val infiniteTransition = rememberInfiniteTransition(label = "bg")

    val animatedShift by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(7000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shift"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFFFFF8C6),
                        Color(0xFFFFD9E8),
                        Color(0xFFD8F2FF),
                        Color(0xFFE8DDFF)
                    )
                )
            )
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height

            val circles = listOf(
                Triple(Color(0x40FFFFFF), 90f, 0.12f),
                Triple(Color(0x35FFD54F), 65f, 0.28f),
                Triple(Color(0x35FF80AB), 70f, 0.48f),
                Triple(Color(0x3539C4FF), 62f, 0.68f),
                Triple(Color(0x359C6BFF), 78f, 0.84f)
            )

            circles.forEachIndexed { index, (color, radius, xFactor) ->
                val yBase = h * (0.15f + index * 0.16f)
                val wave = (sin((animatedShift * 6.28f) + index) * 42f).toFloat()

                drawCircle(
                    color = color,
                    radius = radius,
                    center = Offset(
                        x = w * xFactor + wave,
                        y = yBase
                    )
                )
            }
        }

        FloatingEmoji("⭐", 22.dp, 50.dp, 120.dp, animatedShift)
        FloatingEmoji("💖", 260.dp, 110.dp, 95.dp, animatedShift)
        FloatingEmoji("🌈", 120.dp, 220.dp, 140.dp, animatedShift)
        FloatingEmoji("✨", 300.dp, 310.dp, 100.dp, animatedShift)
        FloatingEmoji("🎈", 25.dp, 430.dp, 125.dp, animatedShift)
        FloatingEmoji("🎉", 290.dp, 560.dp, 135.dp, animatedShift)
    }
}

@Composable
private fun FloatingEmoji(
    emoji: String,
    startX: Dp,
    startY: Dp,
    floatRange: Dp,
    progress: Float
) {
    val wave = (sin(progress * 6.28f) * floatRange.value * 0.12f).toFloat()
    val rotation = (sin(progress * 6.28f) * 8f).toFloat()

    Text(
        text = emoji,
        fontSize = 24.sp,
        modifier = Modifier
            .offset(x = startX, y = startY + wave.dp)
            .graphicsLayer {
                rotationZ = rotation
                alpha = 0.85f
            }
    )
}

@Composable
private fun RewardsHeroCard(childName: String) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(12.dp, RoundedCornerShape(30.dp)),
        shape = RoundedCornerShape(30.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF6A8))
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.linearGradient(
                        colors = listOf(
                            Color(0xFFFFF59D),
                            Color(0xFFFFCCBC),
                            Color(0xFFFFD1DC)
                        )
                    )
                )
                .padding(20.dp)
        ) {
            CuteHeroDino(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .offset(x = (-4).dp, y = (-2).dp)
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 98.dp)
            ) {
                Text(
                    text = "Roar! Great job, $childName!",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color(0xFF6D4C41),
                    lineHeight = 30.sp
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Welcome to your dino reward land! Earn stars, unlock shiny badges, and keep your streak growing!",
                    fontSize = 15.sp,
                    color = Color(0xFF795548),
                    lineHeight = 22.sp
                )
            }
        }
    }
}

@Composable
private fun CuteHeroDino(
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(width = 132.dp, height = 112.dp)
    ) {
        MovingDinoMascot(
            modifier = Modifier
                .align(Alignment.TopStart)
        )
    }
}

@Composable
private fun MovingDinoMascot(
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "dino_main")

    val bobY by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "bobY"
    )

    val sway by infiniteTransition.animateFloat(
        initialValue = -4f,
        targetValue = 4f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "sway"
    )

    val squash by infiniteTransition.animateFloat(
        initialValue = 0.98f,
        targetValue = 1.03f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "squash"
    )

    val limbSwingLeft by infiniteTransition.animateFloat(
        initialValue = -10f,
        targetValue = 10f,
        animationSpec = infiniteRepeatable(
            animation = tween(430, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "limbLeft"
    )

    val limbSwingRight by infiniteTransition.animateFloat(
        initialValue = 10f,
        targetValue = -10f,
        animationSpec = infiniteRepeatable(
            animation = tween(430, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "limbRight"
    )

    val footKickLeft by infiniteTransition.animateFloat(
        initialValue = -8f,
        targetValue = 8f,
        animationSpec = infiniteRepeatable(
            animation = tween(520, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "footLeft"
    )

    val footKickRight by infiniteTransition.animateFloat(
        initialValue = 8f,
        targetValue = -8f,
        animationSpec = infiniteRepeatable(
            animation = tween(520, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "footRight"
    )

    Box(
        modifier = modifier
            .size(118.dp)
    ) {
        // Main dino
        AsyncImage(
            model = R.drawable.dino_badge,
            contentDescription = "Dino Mascot",
            modifier = Modifier
                .matchParentSize()
                .offset(y = (-8f * bobY).dp)
                .graphicsLayer {
                    rotationZ = sway
                    scaleX = squash
                    scaleY = 1.02f
                },
            contentScale = ContentScale.Fit
        )

        // Left arm flap illusion
        Box(
            modifier = Modifier
                .offset(x = 6.dp, y = 26.dp)
                .size(width = 28.dp, height = 30.dp)
                .clip(RoundedCornerShape(16.dp))
        ) {
            AsyncImage(
                model = R.drawable.dino_badge,
                contentDescription = null,
                modifier = Modifier
                    .size(118.dp)
                    .offset(x = (-6).dp, y = (-26).dp)
                    .graphicsLayer {
                        rotationZ = limbSwingLeft
                        transformOrigin = androidx.compose.ui.graphics.TransformOrigin(0.9f, 0.2f)
                    },
                contentScale = ContentScale.Fit
            )
        }

        // Right arm flap illusion
        Box(
            modifier = Modifier
                .offset(x = 78.dp, y = 24.dp)
                .size(width = 26.dp, height = 30.dp)
                .clip(RoundedCornerShape(16.dp))
        ) {
            AsyncImage(
                model = R.drawable.dino_badge,
                contentDescription = null,
                modifier = Modifier
                    .size(118.dp)
                    .offset(x = (-78).dp, y = (-24).dp)
                    .graphicsLayer {
                        rotationZ = limbSwingRight
                        transformOrigin = androidx.compose.ui.graphics.TransformOrigin(0.1f, 0.2f)
                    },
                contentScale = ContentScale.Fit
            )
        }

        // Left leg flap illusion
        Box(
            modifier = Modifier
                .offset(x = 34.dp, y = 78.dp)
                .size(width = 20.dp, height = 24.dp)
                .clip(RoundedCornerShape(14.dp))
        ) {
            AsyncImage(
                model = R.drawable.dino_badge,
                contentDescription = null,
                modifier = Modifier
                    .size(118.dp)
                    .offset(x = (-34).dp, y = (-78).dp)
                    .graphicsLayer {
                        rotationZ = footKickLeft
                        transformOrigin = androidx.compose.ui.graphics.TransformOrigin(0.5f, 0f)
                    },
                contentScale = ContentScale.Fit
            )
        }

        // Right leg flap illusion
        Box(
            modifier = Modifier
                .offset(x = 56.dp, y = 79.dp)
                .size(width = 20.dp, height = 24.dp)
                .clip(RoundedCornerShape(14.dp))
        ) {
            AsyncImage(
                model = R.drawable.dino_badge,
                contentDescription = null,
                modifier = Modifier
                    .size(118.dp)
                    .offset(x = (-56).dp, y = (-79).dp)
                    .graphicsLayer {
                        rotationZ = footKickRight
                        transformOrigin = androidx.compose.ui.graphics.TransformOrigin(0.5f, 0f)
                    },
                contentScale = ContentScale.Fit
            )
        }
    }
}

@Composable
private fun CuteFlappyDino(
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "small_dino")

    val hover by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(850, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "hover"
    )

    val tilt by infiniteTransition.animateFloat(
        initialValue = -5f,
        targetValue = 5f,
        animationSpec = infiniteRepeatable(
            animation = tween(950, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "tilt"
    )

    Box(
        modifier = modifier
    ) {
        MovingDinoMascot(
            modifier = Modifier
                .matchParentSize()
                .offset(y = (-6f * hover).dp)
                .graphicsLayer {
                    rotationZ = tilt
                }
        )
    }
}

@Composable
private fun RewardsSummaryRow() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        RewardMiniCard(
            modifier = Modifier.weight(1f),
            emoji = "⭐",
            title = "Stars",
            subtitle = "Shiny wins!"
        )

        RewardMiniCard(
            modifier = Modifier.weight(1f),
            emoji = "🦕",
            title = "Badges",
            subtitle = "Dino rewards!"
        )

        RewardMiniCard(
            modifier = Modifier.weight(1f),
            emoji = "🚀",
            title = "Streak",
            subtitle = "Zoom ahead!"
        )
    }
}

@Composable
private fun RewardMiniCard(
    modifier: Modifier = Modifier,
    emoji: String,
    title: String,
    subtitle: String
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White.copy(alpha = 0.9f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Column(
            modifier = Modifier.padding(vertical = 16.dp, horizontal = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = emoji,
                fontSize = 28.sp
            )

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = title,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 16.sp,
                color = Color(0xFF5D4037)
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = subtitle,
                fontSize = 11.sp,
                color = Color(0xFF8D6E63),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun FunSectionCard(
    title: String,
    subtitle: String,
    content: @Composable () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(30.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White.copy(alpha = 0.78f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 10.dp)
    ) {
        Column(
            modifier = Modifier.padding(18.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "✨",
                    fontSize = 28.sp
                )

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Text(
                        text = title,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFF6D4C41)
                    )
                    Text(
                        text = subtitle,
                        fontSize = 13.sp,
                        color = Color(0xFF8D6E63)
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            content()
        }
    }
}

@Composable
fun RecentlyReadSection(
    history: List<ReadingHistory>,
    onItemClick: (url: String, title: String, isVideo: Boolean) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                Icons.Default.History,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Recently Read & Watched",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(history) { entry ->
                Card(
                    modifier = Modifier
                        .width(120.dp)
                        .clickable { onItemClick(entry.url, entry.title, entry.isVideo) },
                    shape = RoundedCornerShape(12.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                ) {
                    Column {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(100.dp)
                                .background(
                                    if (entry.isVideo) Color(0xFFFFE5E5) else Color(0xFFE5F0FF)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            if (entry.coverUrl.isNotBlank() && entry.coverUrl != "none") {
                                AsyncImage(
                                    model = entry.coverUrl,
                                    contentDescription = entry.title,
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                            } else {
                                Icon(
                                    imageVector = if (entry.isVideo) Icons.Default.PlayArrow else Icons.Default.MenuBook,
                                    contentDescription = null,
                                    tint = if (entry.isVideo) Color(0xFFD32F2F) else Color(0xFF1976D2),
                                    modifier = Modifier.size(36.dp)
                                )
                            }
                        }

                        Column(
                            modifier = Modifier.padding(10.dp)
                        ) {
                            Text(
                                text = entry.title,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 13.sp,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )

                            Spacer(modifier = Modifier.height(4.dp))

                            Text(
                                text = if (entry.isVideo) "Video" else "Book",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}