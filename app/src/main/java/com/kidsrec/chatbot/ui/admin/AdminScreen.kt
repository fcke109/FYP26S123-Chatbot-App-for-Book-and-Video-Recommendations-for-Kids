package com.kidsrec.chatbot.ui.admin

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.LibraryBooks
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.kidsrec.chatbot.data.model.Book
import com.kidsrec.chatbot.data.model.LoginAttempt
import com.kidsrec.chatbot.data.model.MessageRole
import com.kidsrec.chatbot.data.model.PlanType
import com.kidsrec.chatbot.data.model.SecuritySeverity
import com.kidsrec.chatbot.data.model.SuspiciousActivity
import com.kidsrec.chatbot.data.model.SuspiciousActivityType
import com.kidsrec.chatbot.data.model.User
import com.kidsrec.chatbot.data.model.TopDropOff
import com.kidsrec.chatbot.data.model.TopSearchedTopic
import com.kidsrec.chatbot.data.model.TopViewedBook
import com.kidsrec.chatbot.data.model.UserStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminScreen(
    viewModel: AdminViewModel,
    onLogout: () -> Unit,
    onViewBook: (String, String, Boolean) -> Unit
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var selectedTabIndex by remember { mutableIntStateOf(0) }
    val tabs = listOf("Dashboard", "Users", "Library", "Add Books", "Analytics", "Security", "Post Notifications")
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)

    val users by viewModel.users.collectAsState()
    val books by viewModel.curatedBooks.collectAsState()
    val adminStats by viewModel.adminStats.collectAsState()
    val isLoadingAdminStats by viewModel.isLoadingAdminStats.collectAsState()
    val topSearchedTopics by viewModel.topSearchedTopics.collectAsState()
    val topViewedBooks by viewModel.topViewedBooks.collectAsState()
    val topDropOffs by viewModel.topDropOffs.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.startManagingUsers()
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    "Admin Dashboard",
                    modifier = Modifier.padding(16.dp),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                HorizontalDivider()
                Spacer(modifier = Modifier.height(8.dp))

                tabs.forEachIndexed { index, title ->
                    NavigationDrawerItem(
                        label = { Text(title) },
                        selected = selectedTabIndex == index,
                        onClick = {
                            selectedTabIndex = index
                            scope.launch { drawerState.close() }
                        },
                        icon = {
                            when (index) {
                                0 -> Icon(Icons.Default.Dashboard, null)
                                1 -> Icon(Icons.Default.Group, null)
                                2 -> Icon(Icons.AutoMirrored.Filled.LibraryBooks, null)
                                3 -> Icon(Icons.Default.Search, null)
                                4 -> Icon(Icons.Default.BarChart, null)
                                5 -> Icon(Icons.Default.Security, null)
                                6 -> Icon(Icons.Default.Notifications, null)
                            }
                        },
                        modifier = Modifier.padding(horizontal = 12.dp)
                    )
                }
            }
        }
    ) {
        Scaffold(
            snackbarHost = { SnackbarHost(snackbarHostState) },
            topBar = {
                TopAppBar(
                    title = { Text("Admin Dashboard", fontWeight = FontWeight.Bold) },
                    navigationIcon = {
                        IconButton(onClick = { scope.launch { drawerState.open() } }) {
                            Icon(Icons.Default.Menu, "Menu")
                        }
                    },
                    actions = {
                        IconButton(onClick = { viewModel.startManagingUsers() }) {
                            Icon(Icons.Default.Refresh, "Refresh Users")
                        }
                        IconButton(onClick = onLogout) {
                            Icon(Icons.AutoMirrored.Filled.Logout, "Logout", tint = MaterialTheme.colorScheme.error)
                        }
                    }
                )
            }
        ) { padding ->
            Box(modifier = Modifier.padding(padding).fillMaxSize()) {
                when (selectedTabIndex) {
                    0 -> DashboardTab(users, books, adminStats, isLoadingAdminStats)
                    1 -> UserManagementTab(users, viewModel, snackbarHostState)
                    2 -> BookLibraryTab(books, viewModel, snackbarHostState, onViewBook)
                    3 -> CuratorSearchTab(viewModel, snackbarHostState, onViewBook)
                    4 -> AnalyticsTab(topSearchedTopics, topViewedBooks, topDropOffs)
                    5 -> SecurityControlPanel(viewModel, snackbarHostState)
                    6 -> NotificationTab(viewModel, snackbarHostState)
                }
            }
        }
    }
}

@Composable
fun DashboardTab(users: List<User>, books: List<Book>, stats: AdminStats, isLoadingStats: Boolean) {
    val totalUsers = users.size
    val activeUsers = users.count { it.status == UserStatus.ACTIVE }
    val suspendedUsers = users.count { it.status == UserStatus.SUSPENDED }
    val bannedUsers = users.count { it.status == UserStatus.BANNED }
    
    val freeUsers = users.count { it.planType == PlanType.FREE }
    val premiumUsers = users.count { it.planType == PlanType.PREMIUM }
    val adminUsers = users.count { it.planType == PlanType.ADMIN }
    
    val totalBooks = books.size
    val avgAge = if (users.isNotEmpty()) users.map { it.age }.average().toInt() else 0
    
    LazyColumn(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item {
            Text("User Statistics", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        }
        
        // User Status Overview
        item {
            Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("User Status Overview", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 12.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                        StatCard("Total Users", totalUsers.toString(), MaterialTheme.colorScheme.primary)
                        StatCard("Active", activeUsers.toString(), Color(0xFF4CAF50))
                        StatCard("Suspended", suspendedUsers.toString(), Color(0xFFFF9800))
                        StatCard("Banned", bannedUsers.toString(), Color(0xFFF44336))
                    }
                }
            }
        }
        
        // Plan Distribution
        item {
            Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Plan Distribution", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 12.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                        StatCard("Free", freeUsers.toString(), Color.Gray)
                        StatCard("Premium", premiumUsers.toString(), Color(0xFFFFA000))
                        StatCard("Admin", adminUsers.toString(), Color(0xFF9C27B0))
                    }
                }
            }
        }

        item {
            Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Usage Activity", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 12.dp))
                    if (isLoadingStats) {
                        Box(modifier = Modifier.fillMaxWidth().height(80.dp), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator()
                        }
                    } else {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                            StatCard("Total Users", stats.totalUsers.toString(), MaterialTheme.colorScheme.primary)
                            StatCard("DAU", stats.dailyActiveUsers.toString(), Color(0xFF4CAF50))
                            StatCard("MAU", stats.monthlyActiveUsers.toString(), Color(0xFF1E88E5))
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                            StatCard("Chatbot Usage", stats.chatbotUsageCount.toString(), Color(0xFFFF9800))
                        }
                    }
                }
            }
        }
        
        // Additional Stats
        item {
            Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Additional Statistics", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 12.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                        StatCard("Library Size", totalBooks.toString(), MaterialTheme.colorScheme.secondary)
                        StatCard("Average age", "$avgAge yrs", MaterialTheme.colorScheme.tertiary)
                    }
                }
            }
        }
        
        // Age Distribution
        item {
            Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Age Distribution", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 12.dp))
                    val ageGroups = users.groupBy { 
                        when {
                            it.age < 6 -> "Under 6"
                            it.age < 10 -> "6-9"
                            it.age < 13 -> "10-12"
                            else -> "13+"
                        }
                    }
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        ageGroups.forEach { (ageGroup, groupUsers) ->
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Text(ageGroup, style = MaterialTheme.typography.bodyMedium)
                                Text("${groupUsers.size} users", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AnalyticsTab(
    topSearchedTopics: List<TopSearchedTopic>,
    topViewedBooks: List<TopViewedBook>,
    topDropOffs: List<TopDropOff>
) {
    LazyColumn(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item {
            Text("Analytics Overview", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        }

        // Top Searched Topics
        item {
            Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Top Searched Topics", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 12.dp))
                    if (topSearchedTopics.isEmpty()) {
                        Text("No search data yet.", color = Color.Gray)
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            topSearchedTopics.take(10).forEachIndexed { index, topic ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text("${index + 1}. ${topic.query}", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                        Text("Searched ${topic.count} times", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Top Viewed Books
        item {
            Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Top Viewed Books", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 12.dp))
                    if (topViewedBooks.isEmpty()) {
                        Text("No book view data yet.", color = Color.Gray)
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            topViewedBooks.take(10).forEachIndexed { index, book ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text("${index + 1}. ${book.bookTitle}", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                        Text("Viewed ${book.count} times", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Top Drop-Off Points
        item {
            Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Top Drop-Off Points", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 12.dp))
                    if (topDropOffs.isEmpty()) {
                        Text("No drop-off data yet.", color = Color.Gray)
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            topDropOffs.take(10).forEachIndexed { index, item ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text("${index + 1}. ${item.itemTitle}", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                        Text("Dropped off ${item.count} times • Avg ${item.averageDurationSeconds}s", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun StatCard(label: String, value: String, color: Color) {
    Card(
        modifier = Modifier.size(80.dp),
        colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.1f)),
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = color
            )
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                textAlign = TextAlign.Center,
                color = color
            )
        }
    }
}

@Composable
fun CuratorSearchTab(
    viewModel: AdminViewModel, 
    snackbarHostState: SnackbarHostState,
    onViewBook: (String, String, Boolean) -> Unit
) {
    var query by remember { mutableStateOf("") }
    val searchResults by viewModel.searchResults.collectAsState()
    val isSearching by viewModel.isSearching.collectAsState()
    val scope = rememberCoroutineScope()
    
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current

    val onSearch = {
        if (query.isNotBlank()) {
            viewModel.searchBooks(query)
            keyboardController?.hide()
            focusManager.clearFocus()
        }
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("Search ICDL & OpenLibrary (e.g. Cat)...") },
            trailingIcon = {
                IconButton(onClick = onSearch) {
                    Icon(Icons.Default.Search, null)
                }
            },
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = { onSearch() }),
            singleLine = true,
            shape = RoundedCornerShape(12.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))

        if (isSearching) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                items(searchResults) { book ->
                    BookAdminCard(
                        book = book,
                        onAction = { 
                            viewModel.addBookToLibrary(book)
                            scope.launch { snackbarHostState.showSnackbar("Book added to collection!") }
                        },
                        onCardClick = {
                            val url = book.readerUrl.ifBlank { book.bookUrl }
                            if (url.isNotBlank()) onViewBook(book.title, url, false)
                        },
                        actionIcon = Icons.Default.Add,
                        showScore = true
                    )
                }
            }
        }
    }
}

@Composable
fun BookLibraryTab(
    books: List<Book>, 
    viewModel: AdminViewModel, 
    snackbarHostState: SnackbarHostState,
    onViewBook: (String, String, Boolean) -> Unit
) {
    val scope = rememberCoroutineScope()
    var bookToRemoveUnsafe by remember { mutableStateOf<Book?>(null) }

    // Remove unsafe content confirmation dialog
    if (bookToRemoveUnsafe != null) {
        var reason by remember { mutableStateOf("Violation of safety guidelines.") }
        AlertDialog(
            onDismissRequest = { bookToRemoveUnsafe = null },
            icon = { Icon(Icons.Default.ReportProblem, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
            title = { Text("Remove Unsafe Content") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Are you sure you want to remove \"${bookToRemoveUnsafe!!.title}\"? This will permanently delete it from the library.")
                    OutlinedTextField(
                        value = reason,
                        onValueChange = { reason = it },
                        label = { Text("Reason for removal") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 3
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.removeUnsafeContent(bookToRemoveUnsafe!!.id, reason)
                        scope.launch { snackbarHostState.showSnackbar("${bookToRemoveUnsafe!!.title} removed as unsafe") }
                        bookToRemoveUnsafe = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Remove Permanently")
                }
            },
            dismissButton = {
                TextButton(onClick = { bookToRemoveUnsafe = null }) {
                    Text("Cancel")
                }
            }
        )
    }
    
    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Curated Library (${books.size})", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        }
        
        Spacer(modifier = Modifier.height(12.dp))

        if (books.isEmpty()) {
            Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                Text("No books added. Use 'Add Books' to start.", color = Color.Gray)
            }
        } else {
            LazyColumn(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(books) { book ->
                    Card(
                        modifier = Modifier.fillMaxWidth().clickable {
                            val url = book.readerUrl.ifBlank { book.bookUrl }
                            if (url.isNotBlank()) onViewBook(book.title, url, false)
                        },
                        shape = RoundedCornerShape(12.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Row(modifier = Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                            Box {
                                AsyncImage(
                                    model = book.coverUrl,
                                    contentDescription = null,
                                    modifier = Modifier.size(60.dp).clip(RoundedCornerShape(8.dp)).background(Color.Gray.copy(alpha = 0.1f)),
                                    contentScale = ContentScale.Crop
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(book.title, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text("By ${book.author}", style = MaterialTheme.typography.bodySmall, maxLines = 1)
                                Row(modifier = Modifier.padding(top = 4.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Surface(color = if (book.isKidSafe) Color(0xFF4CAF50).copy(alpha = 0.1f) else Color(0xFFF44336).copy(alpha = 0.1f), shape = RoundedCornerShape(4.dp)) {
                                        Text(if (book.isKidSafe) "SAFE" else "UNSAFE", modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp), fontSize = 9.sp, fontWeight = FontWeight.Bold, color = if (book.isKidSafe) Color(0xFF4CAF50) else Color(0xFFF44336))
                                    }
                                }
                            }
                            Row {
                                IconButton(onClick = { bookToRemoveUnsafe = book }) {
                                    Icon(Icons.Default.GppBad, "Remove unsafe", tint = MaterialTheme.colorScheme.error)
                                }
                                IconButton(onClick = { 
                                    viewModel.deleteBookFromLibrary(book.id)
                                    scope.launch { snackbarHostState.showSnackbar("Book removed") }
                                }) {
                                    Icon(Icons.Default.Delete, "Delete", tint = Color.Gray)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun BookAdminCard(
    book: Book,
    onAction: () -> Unit,
    onCardClick: () -> Unit,
    actionIcon: androidx.compose.ui.graphics.vector.ImageVector,
    actionColor: Color = MaterialTheme.colorScheme.primary,
    showScore: Boolean = false
) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable { onCardClick() },
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(modifier = Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
            Box {
                AsyncImage(
                    model = book.coverUrl,
                    contentDescription = null,
                    modifier = Modifier.size(60.dp).clip(RoundedCornerShape(8.dp)).background(Color.Gray.copy(alpha = 0.1f)),
                    contentScale = ContentScale.Crop
                )
                if (showScore && book.searchScore > 0) {
                    Surface(
                        modifier = Modifier.align(Alignment.TopEnd).padding(2.dp),
                        color = MaterialTheme.colorScheme.tertiary,
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text("${book.searchScore}%", modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp), fontSize = 8.sp, color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(book.title, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text("By ${book.author}", style = MaterialTheme.typography.bodySmall, maxLines = 1)
                Row(modifier = Modifier.padding(top = 4.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Surface(color = MaterialTheme.colorScheme.secondaryContainer, shape = RoundedCornerShape(4.dp)) {
                        Text(book.id, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp), fontSize = 9.sp, fontWeight = FontWeight.Bold)
                    }
                    Surface(color = MaterialTheme.colorScheme.surfaceVariant, shape = RoundedCornerShape(4.dp)) {
                        Text("${book.ageMin}-${book.ageMax} yrs", modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp), fontSize = 9.sp)
                    }
                }
            }
            IconButton(onClick = onAction) {
                Icon(actionIcon, null, tint = actionColor)
            }
        }
    }
}

@Composable
fun UserManagementTab(
    users: List<User>,
    viewModel: AdminViewModel,
    snackbarHostState: SnackbarHostState
) {
    val scope = rememberCoroutineScope()
    var userToDelete by remember { mutableStateOf<User?>(null) }
    var isDeletingUser by remember { mutableStateOf(false) }
    val deleteResult by viewModel.deleteResult.collectAsState()
    var userToSuspend by remember { mutableStateOf<User?>(null) }
    val currentUserId = viewModel.getCurrentUserId()

    // Handle delete result
    LaunchedEffect(deleteResult) {
        deleteResult?.let { result ->
            if (result == "success") {
                snackbarHostState.showSnackbar("${userToDelete?.name ?: "User"} deleted successfully")
            } else if (result.startsWith("error:")) {
                snackbarHostState.showSnackbar("Failed to delete user: ${result.substringAfter("error: ")}")
            }
            viewModel.clearDeleteResult()
            userToDelete = null
            isDeletingUser = false
        }
    }
    var userToBan by remember { mutableStateOf<User?>(null) }
    var userToActivate by remember { mutableStateOf<User?>(null) }
    var userToViewActivity by remember { mutableStateOf<User?>(null) }
    var pendingSearch by remember { mutableStateOf("") }
    var pendingPlan by remember { mutableStateOf("ALL") }
    var pendingStatus by remember { mutableStateOf("ALL") }
    var pendingAgeGroup by remember { mutableStateOf("ALL") }
    var pendingAccountType by remember { mutableStateOf("ALL") }
    var appliedSearch by remember { mutableStateOf("") }
    var appliedPlan by remember { mutableStateOf("ALL") }
    var appliedStatus by remember { mutableStateOf("ALL") }
    var appliedAgeGroup by remember { mutableStateOf("ALL") }
    var appliedAccountType by remember { mutableStateOf("ALL") }
    var planExpanded by remember { mutableStateOf(false) }
    var statusExpanded by remember { mutableStateOf(false) }
    var ageExpanded by remember { mutableStateOf(false) }
    var accountExpanded by remember { mutableStateOf(false) }

    val filteredUsers = remember(users, appliedSearch, appliedPlan, appliedStatus, appliedAgeGroup, appliedAccountType) {
        users.filter { user ->
            val matchesSearch = appliedSearch.isBlank() || listOf(user.name, user.email, user.id).any {
                it.contains(appliedSearch, ignoreCase = true)
            }
            val matchesPlan = appliedPlan == "ALL" || user.planType.name == appliedPlan
            val matchesStatus = appliedStatus == "ALL" || user.status.name == appliedStatus
            val matchesAge = when (appliedAgeGroup) {
                "Under 6" -> user.age < 6
                "6-9" -> user.age in 6..9
                "10-12" -> user.age in 10..12
                "13+" -> user.age >= 13
                else -> true
            }
            val matchesAccountType = appliedAccountType == "ALL" || user.accountType.name == appliedAccountType
            matchesSearch && matchesPlan && matchesStatus && matchesAge && matchesAccountType
        }.sortedWith(compareBy<User> { user -> 
            user.planType != PlanType.ADMIN
        }.thenBy { it.name })
    }

    // Delete confirmation dialog
    if (userToDelete != null) {
        val isCurrentUser = userToDelete?.id == currentUserId
        AlertDialog(
            onDismissRequest = { userToDelete = null },
            icon = { Icon(Icons.Default.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
            title = { Text("Delete User") },
            text = {
                Text(if (isCurrentUser) {
                    "You cannot delete your own account."
                } else {
                    "Are you sure you want to delete ${userToDelete!!.name} (${userToDelete!!.email})?\n\nThis will remove their profile, chat history, favorites, and reading history. This cannot be undone."
                })
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (!isDeletingUser && !isCurrentUser) {
                            isDeletingUser = true
                            viewModel.deleteUser(userToDelete!!.id)
                        }
                    },
                    enabled = !isDeletingUser && !isCurrentUser,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    if (isDeletingUser) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.White)
                    } else {
                        Text(if (isCurrentUser) "Cannot Delete" else "Delete")
                    }
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { userToDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Suspend confirmation dialog
    if (userToSuspend != null) {
        val isCurrentUser = userToSuspend?.id == currentUserId
        AlertDialog(
            onDismissRequest = { userToSuspend = null },
            icon = { Icon(Icons.Default.Warning, contentDescription = null, tint = Color(0xFFFF9800)) },
            title = { Text("Suspend User") },
            text = {
                Text(if (isCurrentUser) {
                    "You are suspending yourself. You will be unable to access the app until reactivated by another admin."
                } else {
                    "Are you sure you want to suspend ${userToSuspend!!.name} (${userToSuspend!!.email})?\n\nThe user will be temporarily unable to access the app until reactivated."
                })
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.suspendUser(userToSuspend!!.id)
                        userToSuspend = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF9800))
                ) {
                    Text("Suspend")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { userToSuspend = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Ban confirmation dialog
    if (userToBan != null) {
        val isCurrentUser = userToBan?.id == currentUserId
        AlertDialog(
            onDismissRequest = { userToBan = null },
            icon = { Icon(Icons.Default.Block, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
            title = { Text("Ban User") },
            text = {
                Text(if (isCurrentUser) {
                    "You are banning yourself. You will be permanently restricted from accessing the app until manually reactivated by another admin."
                } else {
                    "Are you sure you want to ban ${userToBan!!.name} (${userToBan!!.email})?\n\nThe user will be permanently restricted from accessing the app until manually reactivated."
                })
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.banUser(userToBan!!.id)
                        userToBan = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Ban")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { userToBan = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Activate confirmation dialog
    if (userToActivate != null) {
        val isCurrentUser = userToActivate?.id == currentUserId
        AlertDialog(
            onDismissRequest = { userToActivate = null },
            icon = { Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF4CAF50)) },
            title = { Text("Activate User") },
            text = {
                Text(if (isCurrentUser) {
                    "You are reactivating yourself. This will restore your account access."
                } else {
                    "Are you sure you want to reactivate ${userToActivate!!.name} (${userToActivate!!.email})?\n\nThe user will regain full access to the app."
                })
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.activateUser(userToActivate!!.id)
                        userToActivate = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50))
                ) {
                    Text("Activate")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { userToActivate = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    // User activity detail dialog
    if (userToViewActivity != null) {
        UserActivityDialog(
            user = userToViewActivity!!,
            viewModel = viewModel,
            onDismiss = { userToViewActivity = null }
        )
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        OutlinedTextField(
            value = pendingSearch,
            onValueChange = { pendingSearch = it },
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("Search users by name, email, or ID") },
            trailingIcon = {
                Icon(Icons.Default.Search, null)
            },
            singleLine = true,
            shape = RoundedCornerShape(12.dp)
        )

        Spacer(modifier = Modifier.height(12.dp))

        Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterDropdown(
                    modifier = Modifier.weight(1f),
                    label = "Plan",
                    options = listOf("ALL", "FREE", "PREMIUM", "ADMIN"),
                    selectedOption = pendingPlan,
                    onOptionSelected = { pendingPlan = it },
                    expanded = planExpanded,
                    onExpandedChange = { planExpanded = it }
                )
                FilterDropdown(
                    modifier = Modifier.weight(1f),
                    label = "Status",
                    options = listOf("ALL", "ACTIVE", "SUSPENDED", "BANNED"),
                    selectedOption = pendingStatus,
                    onOptionSelected = { pendingStatus = it },
                    expanded = statusExpanded,
                    onExpandedChange = { statusExpanded = it }
                )
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterDropdown(
                    modifier = Modifier.weight(1f),
                    label = "Age",
                    options = listOf("ALL", "Under 6", "6-9", "10-12", "13+"),
                    selectedOption = pendingAgeGroup,
                    onOptionSelected = { pendingAgeGroup = it },
                    expanded = ageExpanded,
                    onExpandedChange = { ageExpanded = it }
                )
                FilterDropdown(
                    modifier = Modifier.weight(1f),
                    label = "Account",
                    options = listOf("ALL", "CHILD", "PARENT"),
                    selectedOption = pendingAccountType,
                    onOptionSelected = { pendingAccountType = it },
                    expanded = accountExpanded,
                    onExpandedChange = { accountExpanded = it }
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            OutlinedButton(
                onClick = {
                    pendingSearch = ""
                    pendingPlan = "ALL"
                    pendingStatus = "ALL"
                    pendingAgeGroup = "ALL"
                    pendingAccountType = "ALL"
                }
            ) {
                Text("Reset")
            }
            Spacer(modifier = Modifier.width(8.dp))
            Button(onClick = {
                appliedSearch = pendingSearch.trim()
                appliedPlan = pendingPlan
                appliedStatus = pendingStatus
                appliedAgeGroup = pendingAgeGroup
                appliedAccountType = pendingAccountType
            }) {
                Text("Apply filters")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text("Matching users: ${filteredUsers.size}", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)

        Spacer(modifier = Modifier.height(12.dp))

        if (filteredUsers.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No users match the current filters.", color = Color.Gray)
            }
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(filteredUsers) { user ->
                    val isAdminUser = user.email.lowercase() == "admin@littledino.com" || user.planType == PlanType.ADMIN

                    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))) {
                        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                            Surface(modifier = Modifier.size(40.dp), shape = CircleShape, color = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(user.name.take(1).uppercase(), fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                }
                            }
                            Spacer(Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(user.name, fontWeight = FontWeight.Bold)
                                Text(user.email, style = MaterialTheme.typography.bodySmall)
                            }
                            Column(horizontalAlignment = Alignment.Start) {
                                PlanBadge(user.planType)
                                StatusBadge(user.status)
                                Text("Age: ${user.age}", fontSize = 10.sp, color = Color.Gray)
                            }
                            // Action menu (not for admin account)
                            if (!isAdminUser) {
                                var expanded by remember { mutableStateOf(false) }
                                Box {
                                    IconButton(onClick = { expanded = true }) {
                                        Icon(Icons.Default.MoreVert, "More actions")
                                    }
                                    DropdownMenu(
                                        expanded = expanded,
                                        onDismissRequest = { expanded = false }
                                    ) {
                                        when (user.status) {
                                            UserStatus.ACTIVE -> {
                                                DropdownMenuItem(
                                                    text = { Text("Suspend User", color = Color(0xFFFF9800)) },
                                                    onClick = {
                                                        userToSuspend = user
                                                        expanded = false
                                                    },
                                                    leadingIcon = {
                                                        Icon(Icons.Default.Warning, null, tint = Color(0xFFFF9800))
                                                    }
                                                )
                                                DropdownMenuItem(
                                                    text = { Text("Ban User", color = MaterialTheme.colorScheme.error) },
                                                    onClick = {
                                                        userToBan = user
                                                        expanded = false
                                                    },
                                                    leadingIcon = {
                                                        Icon(Icons.Default.Block, null, tint = MaterialTheme.colorScheme.error)
                                                    }
                                                )
                                            }
                                            UserStatus.SUSPENDED -> {
                                                DropdownMenuItem(
                                                    text = { Text("Activate User", color = Color(0xFF4CAF50)) },
                                                    onClick = {
                                                        userToActivate = user
                                                        expanded = false
                                                    },
                                                    leadingIcon = {
                                                        Icon(Icons.Default.CheckCircle, null, tint = Color(0xFF4CAF50))
                                                    }
                                                )
                                                DropdownMenuItem(
                                                    text = { Text("Ban User", color = MaterialTheme.colorScheme.error) },
                                                    onClick = {
                                                        userToBan = user
                                                        expanded = false
                                                    },
                                                    leadingIcon = {
                                                        Icon(Icons.Default.Block, null, tint = MaterialTheme.colorScheme.error)
                                                    }
                                                )
                                            }
                                            UserStatus.BANNED -> {
                                                DropdownMenuItem(
                                                    text = { Text("Activate User", color = Color(0xFF4CAF50)) },
                                                    onClick = {
                                                        userToActivate = user
                                                        expanded = false
                                                    },
                                                    leadingIcon = {
                                                        Icon(Icons.Default.CheckCircle, null, tint = Color(0xFF4CAF50))
                                                    }
                                                )
                                            }
                                        }
                                        DropdownMenuItem(
                                            text = { Text("View Activity") },
                                            onClick = {
                                                userToViewActivity = user
                                                viewModel.loadUserActivity(user.id)
                                                expanded = false
                                            },
                                            leadingIcon = {
                                                Icon(Icons.Default.Info, null)
                                            }
                                        )
                                        DropdownMenuItem(
                                            text = { Text("Delete User", color = MaterialTheme.colorScheme.error) },
                                            onClick = {
                                                userToDelete = user
                                                expanded = false
                                            },
                                            leadingIcon = {
                                                Icon(Icons.Default.Delete, null, tint = MaterialTheme.colorScheme.error)
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun LoginAttemptCard(attempt: LoginAttempt) {
    val statusColor = if (attempt.success) Color(0xFF4CAF50) else MaterialTheme.colorScheme.error
    val statusText = if (attempt.success) "Success" else "Failed"

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
    ) {
        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Surface(
                modifier = Modifier.size(12.dp),
                shape = CircleShape,
                color = statusColor
            ) {}

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(attempt.email, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                Text(
                    "$statusText • ${attempt.timestamp.toDate().let {
                        java.text.SimpleDateFormat("MMM dd, HH:mm", java.util.Locale.getDefault()).format(it)
                    }}",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )
                if (!attempt.success && attempt.failureReason.isNotBlank()) {
                    Text("Reason: ${attempt.failureReason}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                }
                if (attempt.ipAddress.isNotBlank()) {
                    Text("IP: ${attempt.ipAddress}", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                }
            }
        }
    }
}

@Composable
fun SuspiciousActivityCard(
    activity: SuspiciousActivity,
    viewModel: AdminViewModel,
    snackbarHostState: SnackbarHostState,
    scope: CoroutineScope
) {
    val severityColor = when (activity.severity) {
        SecuritySeverity.LOW -> Color(0xFFFF9800)
        SecuritySeverity.MEDIUM -> Color(0xFFFF5722)
        SecuritySeverity.HIGH -> Color(0xFFF44336)
        SecuritySeverity.CRITICAL -> Color(0xFFB71C1C)
    }

    val activityTypeText = when (activity.activityType) {
        SuspiciousActivityType.MULTIPLE_FAILED_LOGINS -> "Multiple Failed Logins"
        SuspiciousActivityType.UNUSUAL_ACCESS_PATTERN -> "Unusual Access Pattern"
        SuspiciousActivityType.SUSPICIOUS_IP_ACTIVITY -> "Suspicious IP Activity"
        SuspiciousActivityType.ACCOUNT_BRUTE_FORCE -> "Account Brute Force"
        SuspiciousActivityType.UNUSUAL_DEVICE_ACTIVITY -> "Unusual Device Activity"
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (activity.resolved)
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)
            else
                severityColor.copy(alpha = 0.1f)
        )
    ) {
        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.Top) {
            Surface(
                modifier = Modifier.size(12.dp),
                shape = CircleShape,
                color = severityColor
            ) {}

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(activity.email, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                        Text(
                            "$activityTypeText • ${activity.timestamp.toDate().let {
                                java.text.SimpleDateFormat("MMM dd, HH:mm", java.util.Locale.getDefault()).format(it)
                            }}",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.Gray
                        )
                    }

                    if (!activity.resolved) {
                        OutlinedButton(
                            onClick = {
                                viewModel.markSuspiciousActivityResolved(activity.id)
                                scope.launch {
                                    snackbarHostState.showSnackbar("Activity marked as resolved")
                                }
                            },
                            modifier = Modifier.height(32.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                        ) {
                            Text("Resolve", fontSize = 12.sp)
                        }
                    } else {
                        Surface(
                            color = Color(0xFF4CAF50).copy(alpha = 0.1f),
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                "Resolved",
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                fontSize = 10.sp,
                                color = Color(0xFF4CAF50),
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                if (activity.details.isNotBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(activity.details, style = MaterialTheme.typography.bodySmall)
                }

                if (activity.ipAddress.isNotBlank()) {
                    Text("IP: ${activity.ipAddress}", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserActivityDialog(
    user: User,
    viewModel: AdminViewModel,
    onDismiss: () -> Unit
) {
    val userReadingHistory by viewModel.userReadingHistory.collectAsState()
    val userChatHistory by viewModel.userChatHistory.collectAsState()
    val isLoading by viewModel.isLoadingUserActivity.collectAsState()

    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.fillMaxWidth(0.95f).fillMaxHeight(0.9f),
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(modifier = Modifier.size(32.dp), shape = CircleShape, color = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(user.name.take(1).uppercase(), fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, fontSize = 14.sp)
                    }
                }
                Spacer(Modifier.width(12.dp))
                Column {
                    Text("${user.name}'s Activity", style = MaterialTheme.typography.headlineSmall)
                    Text(user.email, style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                }
            }
        },
        text = {
            if (isLoading) {
                Box(modifier = Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else {
                Column(modifier = Modifier.fillMaxWidth()) {
                    // User basic info
                    Card(modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp), shape = RoundedCornerShape(8.dp)) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text("Account Information", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Spacer(Modifier.height(8.dp))
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Plan:", style = MaterialTheme.typography.bodyMedium)
                                PlanBadge(user.planType)
                            }
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Status:", style = MaterialTheme.typography.bodyMedium)
                                StatusBadge(user.status)
                            }
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Age:", style = MaterialTheme.typography.bodyMedium)
                                Text("${user.age} years old", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                            }
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Joined:", style = MaterialTheme.typography.bodyMedium)
                                Text(user.createdAt.toDate().let { 
                                    java.text.SimpleDateFormat("MMM dd, yyyy", java.util.Locale.getDefault()).format(it)
                                }, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                            }
                        }
                    }

                    // Reading History
                    Text("Recent Reading Activity (${userReadingHistory.size})", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 8.dp))
                    if (userReadingHistory.isEmpty()) {
                        Text("No reading history found", style = MaterialTheme.typography.bodyMedium, color = Color.Gray, modifier = Modifier.padding(bottom = 16.dp))
                    } else {
                        LazyColumn(modifier = Modifier.height(120.dp).fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            items(userReadingHistory.take(5)) { history ->
                                Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(6.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))) {
                                    Row(modifier = Modifier.padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.AutoMirrored.Filled.LibraryBooks, null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
                                        Spacer(Modifier.width(8.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(history.title, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                            Text("Opened: ${history.openedAt.toDate().let { 
                                                java.text.SimpleDateFormat("MMM dd, HH:mm", java.util.Locale.getDefault()).format(it)
                                            }}", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                                        }
                                    }
                                }
                            }
                        }
                        Spacer(Modifier.height(16.dp))
                    }

                    // Chat History
                    Text("Recent Chat Messages (${userChatHistory.size})", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 8.dp))
                    if (userChatHistory.isEmpty()) {
                        Text("No chat history found", style = MaterialTheme.typography.bodyMedium, color = Color.Gray)
                    } else {
                        LazyColumn(modifier = Modifier.height(120.dp).fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            items(userChatHistory.take(5)) { message ->
                                Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(6.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))) {
                                    Row(modifier = Modifier.padding(8.dp), verticalAlignment = Alignment.Top) {
                                        Icon(if (message.role == MessageRole.USER) Icons.Default.Person else Icons.Default.SmartToy, null, modifier = Modifier.size(16.dp), tint = if (message.role == MessageRole.USER) MaterialTheme.colorScheme.primary else Color(0xFF4CAF50))
                                        Spacer(Modifier.width(8.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(if (message.role == MessageRole.USER) "User" else "Bot", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium)
                                            Text(message.content, style = MaterialTheme.typography.bodySmall, maxLines = 2, overflow = TextOverflow.Ellipsis)
                                            Text(message.timestamp.toDate().let { 
                                                java.text.SimpleDateFormat("MMM dd, HH:mm", java.util.Locale.getDefault()).format(it)
                                            }, style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}

@Composable
fun SecurityControlPanel(viewModel: AdminViewModel, snackbarHostState: SnackbarHostState) {
    val loginAttempts by viewModel.loginAttempts.collectAsState()
    val suspiciousActivities by viewModel.suspiciousActivities.collectAsState()
    val isLoadingSecurityData by viewModel.isLoadingSecurityData.collectAsState()

    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        viewModel.loadSecurityData()
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header
        item {
            Text(
                "Security Control Panel",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            Text(
                "Monitor login attempts and suspicious activities",
                style = MaterialTheme.typography.bodyLarge,
                color = Color.Gray
            )
        }

        // Refresh Button
        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                OutlinedButton(
                    onClick = { viewModel.loadSecurityData() },
                    enabled = !isLoadingSecurityData
                ) {
                    if (isLoadingSecurityData) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    Text("Refresh Data")
                }
            }
        }

        // Login Attempts Section
        item {
            Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Recent Login Attempts (${loginAttempts.size})", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        val failedCount = loginAttempts.count { !it.success }
                        Text("$failedCount failed", style = MaterialTheme.typography.bodyMedium, color = if (failedCount > 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary)
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    if (loginAttempts.isEmpty()) {
                        Text("No login attempts recorded", style = MaterialTheme.typography.bodyMedium, color = Color.Gray)
                    } else {
                        loginAttempts.take(10).forEach { attempt ->
                            LoginAttemptCard(attempt)
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                    }
                }
            }
        }

        // Suspicious Activities Section
        item {
            Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Suspicious Activities (${suspiciousActivities.size})", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        val unresolvedCount = suspiciousActivities.count { !it.resolved }
                        Text("$unresolvedCount unresolved", style = MaterialTheme.typography.bodyMedium, color = if (unresolvedCount > 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary)
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    if (suspiciousActivities.isEmpty()) {
                        Text("No suspicious activities detected", style = MaterialTheme.typography.bodyMedium, color = Color.Gray)
                    } else {
                        suspiciousActivities.take(10).forEach { activity ->
                            SuspiciousActivityCard(activity, viewModel, snackbarHostState, scope)
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun NotificationTab(viewModel: AdminViewModel, snackbarHostState: SnackbarHostState) {
    val scope = rememberCoroutineScope()
    var notificationState by remember {
        mutableStateOf(AdminNotificationUiState())
    }

    val onSendClick: () -> Unit = {
        scope.launch {
            try {
                viewModel.sendNotification(
                    title = notificationState.title,
                    body = notificationState.body,
                    type = notificationState.type,
                    targetValue = notificationState.targetValue
                )
                snackbarHostState.showSnackbar("Notification sent successfully!")
                // Reset form
                notificationState = AdminNotificationUiState()
            } catch (e: Exception) {
                snackbarHostState.showSnackbar("Failed to send notification: ${e.message}")
            }
        }
    }

    AdminNotificationScreen(
        state = notificationState,
        onTitleChange = { notificationState = notificationState.copy(title = it) },
        onBodyChange = { notificationState = notificationState.copy(body = it) },
        onTypeChange = { notificationState = notificationState.copy(type = it) },
        onTargetChange = { notificationState = notificationState.copy(targetValue = it) },
        onSendClick = onSendClick
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FilterDropdown(
    modifier: Modifier = Modifier,
    label: String,
    options: List<String>,
    selectedOption: String,
    onOptionSelected: (String) -> Unit,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit
) {
    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = onExpandedChange,
        modifier = modifier
    ) {
        OutlinedTextField(
            value = selectedOption,
            onValueChange = {},
            readOnly = true,
            label = { Text(label, fontSize = 12.sp) },
            trailingIcon = {
                ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
            },
            modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable).fillMaxWidth(),
            textStyle = TextStyle(fontSize = 12.sp),
            shape = RoundedCornerShape(12.dp),
            maxLines = 1
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { onExpandedChange(false) }
        ) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option, fontSize = 12.sp) },
                    onClick = {
                        onOptionSelected(option)
                        onExpandedChange(false)
                    }
                )
            }
        }
    }
}

@Composable
fun PlanBadge(planType: PlanType) {
    val color = when (planType) {
        PlanType.FREE -> Color.Gray
        PlanType.PREMIUM -> Color(0xFFFFA000)
        PlanType.ADMIN -> Color(0xFF9C27B0)
    }
    Surface(color = color.copy(alpha = 0.15f), contentColor = color, shape = RoundedCornerShape(8.dp)) {
        Text(text = planType.name, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp), fontSize = 9.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun StatusBadge(status: UserStatus) {
    val (color, text) = when (status) {
        UserStatus.ACTIVE -> Color(0xFF4CAF50) to "Active"
        UserStatus.SUSPENDED -> Color(0xFFFF9800) to "Suspended"
        UserStatus.BANNED -> Color(0xFFF44336) to "Banned"
    }
    Surface(color = color.copy(alpha = 0.15f), contentColor = color, shape = RoundedCornerShape(8.dp)) {
        Text(text = text, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp), fontSize = 9.sp, fontWeight = FontWeight.Bold)
    }
}
