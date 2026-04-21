package com.kidsrec.chatbot.ui.parent

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.ChildCare
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kidsrec.chatbot.data.model.ChatMessage
import com.kidsrec.chatbot.data.model.Conversation
import com.kidsrec.chatbot.data.model.Favorite
import com.kidsrec.chatbot.data.model.MessageRole
import com.kidsrec.chatbot.data.model.ReadingHistory
import com.kidsrec.chatbot.data.model.User
import java.text.SimpleDateFormat
import java.util.Locale

private val ParentBlue = Color(0xFF4F8EE8)
private val ParentBlueDark = Color(0xFF316FC8)
private val ParentSoftBlue = Color(0xFFEFF6FF)
private val ParentMint = Color(0xFFE9F8F0)
private val ParentGold = Color(0xFFFFB300)
private val ParentDanger = Color(0xFFE65353)
private val ParentSurface = Color(0xFFF8FBFF)
private val ParentBorder = Color(0xFFDCE7F7)
private val ParentTextSoft = Color(0xFF6D7A8C)
private val ParentPinkSoft = Color(0xFFFFEEF5)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ParentDashboardScreen(
    viewModel: ParentDashboardViewModel,
    parentProgressViewModel: ParentProgressViewModel,
    onLogout: () -> Unit,
    onUpgradePremium: () -> Unit = {},
    onGenerateCode: () -> Unit = {}
) {
    val children by viewModel.children.collectAsState()
    val selectedChild by viewModel.selectedChild.collectAsState()
    val inviteCode by viewModel.inviteCode.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    val childFavorites by viewModel.childFavorites.collectAsState()
    val childHistory by viewModel.childHistory.collectAsState()
    val childConversations by viewModel.childConversations.collectAsState()
    val childMessages by viewModel.childMessages.collectAsState()
    val selectedConversationId by viewModel.selectedConversationId.collectAsState()

    Scaffold(
        containerColor = ParentSurface,
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.White,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                ),
                title = {
                    Column {
                        Text(
                            text = selectedChild?.name ?: "Parent Dashboard",
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = if (selectedChild != null) {
                                "Monitor activity and update child settings"
                            } else {
                                "Manage linked children and account controls"
                            },
                            fontSize = 12.sp,
                            color = ParentTextSoft
                        )
                    }
                },
                navigationIcon = {
                    if (selectedChild != null) {
                        IconButton(onClick = { viewModel.clearSelectedChild() }) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back"
                            )
                        }
                    }
                },
                actions = {
                    IconButton(onClick = onUpgradePremium) {
                        Icon(
                            Icons.Default.Star,
                            contentDescription = "Upgrade to Premium",
                            tint = ParentGold
                        )
                    }
                    IconButton(onClick = onLogout) {
                        Icon(
                            Icons.AutoMirrored.Filled.Logout,
                            contentDescription = "Logout",
                            tint = ParentDanger
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        if (selectedChild != null) {
            ChildDetailView(
                child = selectedChild!!,
                favorites = childFavorites,
                history = childHistory,
                conversations = childConversations,
                messages = childMessages,
                selectedConversationId = selectedConversationId,
                parentProgressViewModel = parentProgressViewModel,
                onSelectConversation = { conversationId ->
                    viewModel.selectConversation(selectedChild!!.id, conversationId)
                },
                onClearConversation = { viewModel.clearConversation() },
                onUpdateFilters = { maxAge, allowVideos ->
                    viewModel.updateChildFilters(selectedChild!!.id, maxAge, allowVideos)
                },
                onUpdatePin = { pin ->
                    viewModel.updateChildParentalPin(selectedChild!!.id, pin)
                },
                modifier = Modifier.padding(paddingValues)
            )
        } else {
            ParentHomeView(
                children = children,
                inviteCode = inviteCode,
                isLoading = isLoading,
                errorMessage = errorMessage,
                onGenerateCode = onGenerateCode,
                onDismissCode = { viewModel.dismissInviteCode() },
                onDismissError = { viewModel.dismissError() },
                onSelectChild = { child -> viewModel.selectChild(child) },
                modifier = Modifier.padding(paddingValues)
            )
        }
    }
}

@Composable
private fun ParentHomeView(
    children: List<User>,
    inviteCode: String?,
    isLoading: Boolean,
    errorMessage: String?,
    onGenerateCode: () -> Unit,
    onDismissCode: () -> Unit,
    onDismissError: () -> Unit,
    onSelectChild: (User) -> Unit,
    modifier: Modifier = Modifier
) {
    val clipboardManager = LocalClipboardManager.current

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        HeroSummaryCard(
            childCount = children.size,
            isLoading = isLoading,
            onGenerateCode = onGenerateCode
        )

        if (inviteCode != null) {
            Spacer(modifier = Modifier.height(16.dp))
            InviteCodeCard(
                inviteCode = inviteCode,
                onCopy = { clipboardManager.setText(AnnotatedString(inviteCode)) },
                onDismiss = onDismissCode
            )
        }

        if (errorMessage != null) {
            Spacer(modifier = Modifier.height(12.dp))
            ErrorBanner(
                message = errorMessage,
                onDismiss = onDismissError
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        SectionHeader(
            title = "Your Children",
            subtitle = if (children.isEmpty()) {
                "No linked child accounts yet"
            } else {
                "${children.size} linked account${if (children.size == 1) "" else "s"}"
            }
        )

        Spacer(modifier = Modifier.height(12.dp))

        if (children.isEmpty()) {
            EmptyChildrenCard()
        } else {
            children.forEach { child ->
                ChildCard(
                    child = child,
                    onClick = { onSelectChild(child) }
                )
                Spacer(modifier = Modifier.height(10.dp))
            }
        }
    }
}

@Composable
private fun HeroSummaryCard(
    childCount: Int,
    isLoading: Boolean,
    onGenerateCode: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = Brush.verticalGradient(
                        listOf(ParentSoftBlue, Color.White)
                    )
                )
                .padding(20.dp)
        ) {
            Column {
                Surface(
                    shape = RoundedCornerShape(50),
                    color = ParentBlue.copy(alpha = 0.10f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Shield,
                            contentDescription = null,
                            tint = ParentBlue,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Parent controls",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = ParentBlueDark
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = "Welcome, Parent",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.ExtraBold
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Create invite codes, review activity, and manage safer content settings for your child accounts.",
                    fontSize = 14.sp,
                    lineHeight = 20.sp,
                    color = ParentTextSoft
                )

                Spacer(modifier = Modifier.height(16.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    StatChip(title = "Linked", value = childCount.toString())
                    StatChip(
                        title = "Status",
                        value = if (childCount == 0) "Setup" else "Active"
                    )
                }

                Spacer(modifier = Modifier.height(18.dp))

                Button(
                    onClick = onGenerateCode,
                    enabled = !isLoading,
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = ParentBlue)
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            color = Color.White,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(Icons.Default.Add, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Generate Invite Code")
                    }
                }
            }
        }
    }
}

@Composable
private fun StatChip(
    title: String,
    value: String
) {
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = ParentMint
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = value,
                fontWeight = FontWeight.Bold,
                color = ParentBlueDark
            )
            Text(
                text = title,
                fontSize = 11.sp,
                color = ParentTextSoft
            )
        }
    }
}

@Composable
private fun InviteCodeCard(
    inviteCode: String,
    onCopy: () -> Unit,
    onDismiss: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Invite Code Ready",
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = ParentBlueDark
            )

            Spacer(modifier = Modifier.height(10.dp))

            Surface(
                shape = RoundedCornerShape(18.dp),
                color = ParentSoftBlue
            ) {
                Text(
                    text = inviteCode,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 14.dp),
                    fontSize = 32.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 5.sp,
                    color = ParentBlue
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Expires in 24 hours",
                fontSize = 12.sp,
                color = ParentTextSoft
            )

            Spacer(modifier = Modifier.height(14.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = onCopy,
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Icon(
                        Icons.Default.ContentCopy,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Copy")
                }

                TextButton(onClick = onDismiss) {
                    Text("Dismiss")
                }
            }
        }
    }
}

@Composable
private fun ErrorBanner(
    message: String,
    onDismiss: () -> Unit
) {
    Surface(
        color = MaterialTheme.colorScheme.errorContainer,
        shape = RoundedCornerShape(18.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.ErrorOutline,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = message,
                color = MaterialTheme.colorScheme.onErrorContainer,
                modifier = Modifier.weight(1f)
            )
            IconButton(onClick = onDismiss) {
                Icon(Icons.Default.Close, contentDescription = "Dismiss")
            }
        }
    }
}

@Composable
private fun EmptyChildrenCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Surface(
                shape = CircleShape,
                color = ParentSoftBlue,
                modifier = Modifier.size(72.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        Icons.Default.ChildCare,
                        contentDescription = null,
                        modifier = Modifier.size(36.dp),
                        tint = ParentBlue
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = "No children linked yet",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "Generate an invite code and share it with your child to connect their account here.",
                fontSize = 13.sp,
                lineHeight = 20.sp,
                color = ParentTextSoft,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun SectionHeader(
    title: String,
    subtitle: String? = null
) {
    Column {
        Text(
            text = title,
            fontSize = 21.sp,
            fontWeight = FontWeight.Bold
        )
        if (subtitle != null) {
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                fontSize = 13.sp,
                color = ParentTextSoft
            )
        }
    }
}

@Composable
private fun ChildCard(
    child: User,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                color = ParentSoftBlue,
                shape = CircleShape,
                modifier = Modifier.size(56.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = child.name.firstOrNull()?.uppercase() ?: "?",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = ParentBlue
                    )
                }
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = child.name,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "${child.age} years old · ${child.readingLevel}",
                    fontSize = 13.sp,
                    color = ParentTextSoft
                )
                if (child.interests.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = child.interests.take(3).joinToString(" • "),
                        fontSize = 12.sp,
                        color = ParentBlueDark,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Icon(
                Icons.Default.ChevronRight,
                contentDescription = "View details",
                tint = ParentTextSoft
            )
        }
    }
}

@Composable
private fun ChildDetailView(
    child: User,
    favorites: List<Favorite>,
    history: List<ReadingHistory>,
    conversations: List<Conversation>,
    messages: List<ChatMessage>,
    selectedConversationId: String?,
    parentProgressViewModel: ParentProgressViewModel,
    onSelectConversation: (String) -> Unit,
    onClearConversation: () -> Unit,
    onUpdateFilters: (maxAgeRating: Int, allowVideos: Boolean) -> Unit,
    onUpdatePin: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Activity", "Favorites", "Chat", "Controls", "Progress")

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(ParentSurface)
    ) {
        ChildDashboardSummaryCard(
            child = child,
            historyCount = history.size,
            favoritesCount = favorites.size,
            conversationCount = conversations.size
        )

        ScrollableTabRow(
            selectedTabIndex = selectedTab,
            edgePadding = 12.dp,
            containerColor = ParentSurface,
            contentColor = ParentBlue
        ) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTab == index,
                    onClick = { selectedTab = index },
                    text = {
                        Text(
                            text = title,
                            fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Medium
                        )
                    }
                )
            }
        }

        when (selectedTab) {
            0 -> ActivityTab(history)
            1 -> FavoritesTab(favorites)
            2 -> ChatHistoryTab(
                conversations = conversations,
                messages = messages,
                selectedConversationId = selectedConversationId,
                onSelectConversation = onSelectConversation,
                onBack = onClearConversation
            )
            3 -> ControlsTab(
                child = child,
                onUpdateFilters = onUpdateFilters,
                onUpdatePin = onUpdatePin
            )
            4 -> ProgressTab(
                child = child,
                parentProgressViewModel = parentProgressViewModel
            )
        }
    }
}

@Composable
private fun ChildDashboardSummaryCard(
    child: User,
    historyCount: Int,
    favoritesCount: Int,
    conversationCount: Int
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    color = ParentSoftBlue,
                    shape = CircleShape,
                    modifier = Modifier.size(60.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = child.name.firstOrNull()?.uppercase() ?: "?",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = ParentBlue
                        )
                    }
                }

                Spacer(modifier = Modifier.width(14.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = child.name,
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "${child.age} years old · ${child.readingLevel}",
                        fontSize = 13.sp,
                        color = ParentTextSoft
                    )
                    if (child.interests.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = child.interests.take(4).joinToString(" • "),
                            fontSize = 12.sp,
                            color = ParentBlueDark,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                SummaryStatCard(
                    modifier = Modifier.weight(1f),
                    title = "Activity",
                    value = historyCount.toString(),
                    tint = ParentBlue,
                    bg = ParentSoftBlue
                )
                SummaryStatCard(
                    modifier = Modifier.weight(1f),
                    title = "Favorites",
                    value = favoritesCount.toString(),
                    tint = Color(0xFFE91E63),
                    bg = ParentPinkSoft
                )
                SummaryStatCard(
                    modifier = Modifier.weight(1f),
                    title = "Chats",
                    value = conversationCount.toString(),
                    tint = Color(0xFF26A69A),
                    bg = ParentMint
                )
            }
        }
    }
}

@Composable
private fun SummaryStatCard(
    modifier: Modifier = Modifier,
    title: String,
    value: String,
    tint: Color,
    bg: Color
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(18.dp),
        color = bg
    ) {
        Column(
            modifier = Modifier.padding(vertical = 14.dp, horizontal = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = value,
                fontWeight = FontWeight.ExtraBold,
                color = tint,
                fontSize = 18.sp
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = title,
                fontSize = 12.sp,
                color = ParentTextSoft
            )
        }
    }
}

@Composable
private fun ProgressTab(
    child: User,
    parentProgressViewModel: ParentProgressViewModel
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        ParentProgressSection(
            viewModel = parentProgressViewModel,
            childUserId = child.id
        )
    }
}

@Composable
private fun ActivityTab(history: List<ReadingHistory>) {
    if (history.isEmpty()) {
        EmptyState(
            icon = Icons.Default.History,
            title = "No activity yet",
            subtitle = "Activity will appear here once your child opens books or videos."
        )
    } else {
        val dateFormat = remember { SimpleDateFormat("MMM dd, yyyy  hh:mm a", Locale.getDefault()) }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(history) { entry ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White)
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = if (entry.isVideo) Color(0xFFFFEBEE) else ParentSoftBlue,
                            modifier = Modifier.size(42.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = if (entry.isVideo) {
                                        Icons.Default.PlayCircle
                                    } else {
                                        Icons.AutoMirrored.Filled.MenuBook
                                    },
                                    contentDescription = null,
                                    tint = if (entry.isVideo) Color(0xFFE53935) else ParentBlue,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = entry.title,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = dateFormat.format(entry.openedAt.toDate()),
                                fontSize = 12.sp,
                                color = ParentTextSoft
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FavoritesTab(favorites: List<Favorite>) {
    if (favorites.isEmpty()) {
        EmptyState(
            icon = Icons.Default.FavoriteBorder,
            title = "No favorites yet",
            subtitle = "Saved books and videos will appear here when your child favorites them."
        )
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(favorites) { fav ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White)
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = ParentPinkSoft,
                            modifier = Modifier.size(42.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Favorite,
                                    contentDescription = null,
                                    tint = Color(0xFFE91E63),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = fav.title,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                            if (fav.description.isNotBlank()) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = fav.description,
                                    fontSize = 12.sp,
                                    color = ParentTextSoft,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ControlsTab(
    child: User,
    onUpdateFilters: (maxAgeRating: Int, allowVideos: Boolean) -> Unit,
    onUpdatePin: (String) -> Unit
) {
    var maxAgeRating by remember(child.id) {
        mutableFloatStateOf(child.contentFilters.maxAgeRating.toFloat())
    }
    var allowVideos by remember(child.id) {
        mutableStateOf(child.contentFilters.allowVideos)
    }
    var pinInput by remember(child.id) {
        mutableStateOf("")
    }
    var pinError by remember(child.id) {
        mutableStateOf<String?>(null)
    }
    var hasChanges by remember(child.id) { mutableStateOf(false) }

    val hasExistingPin = child.parentalPin?.matches(Regex("^\\d{4}$")) == true
    val canSave = hasChanges || pinInput.isNotEmpty()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        SectionHeader(
            title = "Parental Controls",
            subtitle = "Manage content visibility and parent approval settings"
        )

        Spacer(modifier = Modifier.height(16.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(22.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Tune,
                        contentDescription = null,
                        tint = ParentBlue
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Content Filters",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(18.dp))

                Text(
                    text = "Maximum Age Rating",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = "${maxAgeRating.toInt()} years old",
                    color = ParentTextSoft,
                    fontSize = 13.sp
                )

                Spacer(modifier = Modifier.height(12.dp))

                Slider(
                    value = maxAgeRating,
                    onValueChange = {
                        maxAgeRating = it
                        hasChanges = true
                    },
                    valueRange = 3f..18f,
                    steps = 14
                )

                LinearProgressIndicator(
                    progress = { (maxAgeRating - 3f) / 15f },
                    modifier = Modifier.fillMaxWidth(),
                    color = ParentBlue,
                    trackColor = ParentSoftBlue
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("3", fontSize = 12.sp, color = ParentTextSoft)
                    Text("18", fontSize = 12.sp, color = ParentTextSoft)
                }

                Spacer(modifier = Modifier.height(20.dp))

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = ParentSoftBlue)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Allow Video Recommendations",
                                fontWeight = FontWeight.SemiBold
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Enable or disable video content for this child",
                                fontSize = 12.sp,
                                color = ParentTextSoft
                            )
                        }

                        Switch(
                            checked = allowVideos,
                            onCheckedChange = {
                                allowVideos = it
                                hasChanges = true
                            }
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(22.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Lock,
                        contentDescription = null,
                        tint = ParentBlue
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Parent PIN",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = if (hasExistingPin) {
                        "A PIN is already set. Enter a new 4-digit PIN below to change it."
                    } else {
                        "Create a 4-digit parent PIN for this child."
                    },
                    fontSize = 13.sp,
                    lineHeight = 20.sp,
                    color = ParentTextSoft
                )

                Spacer(modifier = Modifier.height(14.dp))

                OutlinedTextField(
                    value = pinInput,
                    onValueChange = { value ->
                        if (value.length <= 4 && value.all { it.isDigit() }) {
                            pinInput = value
                            pinError = null
                        }
                    },
                    label = { Text("4-digit PIN") },
                    placeholder = { Text("Enter PIN") },
                    singleLine = true,
                    isError = pinError != null,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    supportingText = {
                        Text(
                            pinError ?: if (hasExistingPin) {
                                "Leave blank if you do not want to change the current PIN."
                            } else {
                                "Only the parent should know this PIN."
                            }
                        )
                    },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        Button(
            onClick = {
                if (pinInput.isNotEmpty() && pinInput.length != 4) {
                    pinError = "PIN must be exactly 4 digits."
                    return@Button
                }

                if (hasChanges) {
                    onUpdateFilters(maxAgeRating.toInt(), allowVideos)
                }

                if (pinInput.length == 4) {
                    onUpdatePin(pinInput)
                    pinInput = ""
                    pinError = null
                }

                hasChanges = false
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = canSave,
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = ParentBlue)
        ) {
            Icon(Icons.Default.Save, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Save Changes")
        }
    }
}

@Composable
private fun ChatHistoryTab(
    conversations: List<Conversation>,
    messages: List<ChatMessage>,
    selectedConversationId: String?,
    onSelectConversation: (String) -> Unit,
    onBack: () -> Unit
) {
    val dateFormat = remember { SimpleDateFormat("MMM dd, yyyy  hh:mm a", Locale.getDefault()) }

    if (selectedConversationId != null) {
        Column(modifier = Modifier.fillMaxSize()) {
            Surface(
                color = Color.White,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back to conversations"
                        )
                    }
                    Text(
                        text = "Conversation",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp
                    )
                }
            }

            HorizontalDivider(color = ParentBorder)

            if (messages.isEmpty()) {
                EmptyState(
                    icon = Icons.AutoMirrored.Filled.Chat,
                    title = "No messages in this conversation",
                    subtitle = "Messages will appear here once the conversation contains chat history."
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(messages) { message ->
                        val isUser = message.role == MessageRole.USER

                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = if (isUser) Alignment.End else Alignment.Start
                        ) {
                            Card(
                                modifier = Modifier.widthIn(max = 310.dp),
                                shape = RoundedCornerShape(
                                    topStart = 18.dp,
                                    topEnd = 18.dp,
                                    bottomStart = if (isUser) 18.dp else 6.dp,
                                    bottomEnd = if (isUser) 6.dp else 18.dp
                                ),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isUser) ParentSoftBlue else Color.White
                                )
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text(
                                        text = if (isUser) "Child" else "Little Dino",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = ParentTextSoft
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = message.content,
                                        fontSize = 14.sp
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = dateFormat.format(message.timestamp.toDate()),
                                        fontSize = 10.sp,
                                        color = ParentTextSoft
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    } else {
        if (conversations.isEmpty()) {
            EmptyState(
                icon = Icons.AutoMirrored.Filled.Chat,
                title = "No chat history yet",
                subtitle = "Chat conversations will appear here once your child starts chatting with Little Dino."
            )
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(conversations) { conversation ->
                    Card(
                        onClick = { onSelectConversation(conversation.id) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White)
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = ParentSoftBlue,
                                modifier = Modifier.size(42.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.Chat,
                                        contentDescription = null,
                                        tint = ParentBlue,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = conversation.preview,
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = dateFormat.format(conversation.lastUpdated.toDate()),
                                    fontSize = 12.sp,
                                    color = ParentTextSoft
                                )
                            }

                            Icon(
                                Icons.Default.ChevronRight,
                                contentDescription = null,
                                tint = ParentTextSoft
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyState(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Surface(
                shape = CircleShape,
                color = ParentSoftBlue,
                modifier = Modifier.size(72.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        icon,
                        contentDescription = null,
                        modifier = Modifier.size(34.dp),
                        tint = ParentBlue
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = title,
                fontWeight = FontWeight.Bold,
                fontSize = 17.sp
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = subtitle,
                fontSize = 13.sp,
                lineHeight = 20.sp,
                color = ParentTextSoft,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 20.dp)
            )
        }
    }
}