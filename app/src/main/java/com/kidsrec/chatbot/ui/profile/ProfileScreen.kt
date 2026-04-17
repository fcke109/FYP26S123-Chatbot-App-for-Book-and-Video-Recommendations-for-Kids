package com.kidsrec.chatbot.ui.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.kidsrec.chatbot.data.model.ReadingHistory
import com.kidsrec.chatbot.ui.auth.AuthViewModel
import com.kidsrec.chatbot.ui.parental.ChildSafetyLockGate
import com.kidsrec.chatbot.ui.parental.ChildSettingsEntry

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

    fun requestEditAccess() {
        showEditLockGate = true
    }

    fun grantEditAccess() {
        showEditLockGate = false
        isEditing = true
    }

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
                grantEditAccess()
            },
            content = {
                LaunchedEffect(Unit) {
                    grantEditAccess()
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
                        IconButton(onClick = { requestEditAccess() }) {
                            Icon(Icons.Default.Edit, contentDescription = "Edit Profile")
                        }
                    }
                    IconButton(onClick = { authViewModel.signOut() }) {
                        Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = "Logout")
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

                ExposedDropdownMenuBox(
                    expanded = interestsExpanded,
                    onExpandedChange = { interestsExpanded = it }
                ) {
                    OutlinedTextField(
                        value = if (selectedInterests.isEmpty()) "" else "${selectedInterests.size} selected",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Tap to pick interests") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = interestsExpanded) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor()
                    )
                    ExposedDropdownMenu(
                        expanded = interestsExpanded,
                        onDismissRequest = { interestsExpanded = false }
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
                                    Icon(Icons.AutoMirrored.Filled.MenuBook, contentDescription = null)
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
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(24.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = Brush.linearGradient(
                        colors = listOf(
                            Color(0xFFE8F5FF),
                            Color(0xFFFCE4FF),
                            Color(0xFFFFF5D9)
                        )
                    )
                )
                .padding(20.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        color = Color.White.copy(alpha = 0.9f),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.EmojiEvents,
                            contentDescription = null,
                            tint = Color(0xFFFF9800),
                            modifier = Modifier.padding(12.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Badges & Rewards",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.ExtraBold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "See your badges, points, levels, and reward progress on a full page.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Icon(
                        imageVector = Icons.Default.ChevronRight,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = onClick,
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Icon(Icons.Default.Star, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Open Rewards Page")
                }
            }
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
            Icon(Icons.Default.History, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
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
                                    imageVector = if (entry.isVideo) Icons.Default.PlayCircle else Icons.AutoMirrored.Filled.MenuBook,
                                    contentDescription = null,
                                    modifier = Modifier.size(36.dp),
                                    tint = if (entry.isVideo) Color.Red else MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                        Text(
                            text = entry.title,
                            modifier = Modifier.padding(8.dp),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}