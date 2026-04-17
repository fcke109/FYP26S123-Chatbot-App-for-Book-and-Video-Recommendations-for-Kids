package com.kidsrec.chatbot.ui.admin

import androidx.compose.foundation.background
import com.kidsrec.chatbot.data.model.BookCategory
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

private val AdminBg = Color(0xFFF5F7FA)
private val AdminCard = Color.White
private val AdminTextSecondary = Color(0xFF6B7280)
private val AdminBorder = Color(0xFFE5E7EB)
private val AdminPrimary = Color(0xFF1F3A5F)
private val AdminSuccess = Color(0xFF2E7D32)
private val AdminWarning = Color(0xFFB26A00)
private val AdminDanger = Color(0xFFC62828)
private val AdminInfo = Color(0xFF1565C0)

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
    val tabs = listOf(
        "Dashboard",
        "Users",
        "Library",
        "Add Books",
        "Categories",
        "Analytics",
        "Security",
        "Post Notifications"
    )
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
            ModalDrawerSheet(
                drawerContainerColor = AdminCard,
                drawerContentColor = MaterialTheme.colorScheme.onSurface
            ) {
                Spacer(modifier = Modifier.height(20.dp))
                Text(
                    "Admin Dashboard",
                    modifier = Modifier.padding(horizontal = 20.dp),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    "Little Dino control panel",
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp),
                    style = MaterialTheme.typography.bodySmall,
                    color = AdminTextSecondary
                )
                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider(color = AdminBorder)
                Spacer(modifier = Modifier.height(8.dp))

                tabs.forEachIndexed { index, title ->
                    NavigationDrawerItem(
                        label = {
                            Text(
                                title,
                                fontWeight = if (selectedTabIndex == index) FontWeight.SemiBold else FontWeight.Normal
                            )
                        },
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
                                4 -> Icon(Icons.Default.Category, null)
                                5 -> Icon(Icons.Default.BarChart, null)
                                6 -> Icon(Icons.Default.Security, null)
                                7 -> Icon(Icons.Default.Notifications, null)
                            }
                        },
                        colors = NavigationDrawerItemDefaults.colors(
                            selectedContainerColor = AdminPrimary.copy(alpha = 0.10f),
                            selectedIconColor = AdminPrimary,
                            selectedTextColor = AdminPrimary,
                            unselectedContainerColor = Color.Transparent
                        ),
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 2.dp)
                    )
                }
            }
        }
    ) {
        Scaffold(
            containerColor = AdminBg,
            snackbarHost = { SnackbarHost(snackbarHostState) },
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            "Admin Dashboard",
                            fontWeight = FontWeight.SemiBold
                        )
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = AdminCard,
                        titleContentColor = MaterialTheme.colorScheme.onSurface
                    ),
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
                            Icon(Icons.AutoMirrored.Filled.Logout, "Logout", tint = AdminDanger)
                        }
                    }
                )
            }
        ) { padding ->
            Box(
                modifier = Modifier
                    .padding(padding)
                    .fillMaxSize()
                    .background(AdminBg)
            ) {
                when (selectedTabIndex) {
                    0 -> DashboardTab(users, books, adminStats, isLoadingAdminStats)
                    1 -> UserManagementTab(users, viewModel, snackbarHostState)
                    2 -> BookLibraryTab(books, viewModel, snackbarHostState, onViewBook)
                    3 -> CuratorSearchTab(viewModel, snackbarHostState, onViewBook)
                    4 -> CategoryManagementTab(viewModel)
                    5 -> AnalyticsTab(topSearchedTopics, topViewedBooks, topDropOffs)
                    6 -> SecurityControlPanel(viewModel, snackbarHostState)
                    7 -> NotificationTab(viewModel, snackbarHostState)
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

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Column {
                Text(
                    "Overview",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    "Monitor users, content, and activity",
                    style = MaterialTheme.typography.bodyMedium,
                    color = AdminTextSecondary
                )
            }
        }

        item {
            ElevatedCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.elevatedCardColors(containerColor = AdminCard),
                elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "User Status Overview",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        StatCard("Total Users", totalUsers.toString(), AdminPrimary, Modifier.weight(1f))
                        StatCard("Active", activeUsers.toString(), AdminSuccess, Modifier.weight(1f))
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        StatCard("Suspended", suspendedUsers.toString(), AdminWarning, Modifier.weight(1f))
                        StatCard("Banned", bannedUsers.toString(), AdminDanger, Modifier.weight(1f))
                    }
                }
            }
        }

        item {
            ElevatedCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.elevatedCardColors(containerColor = AdminCard),
                elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "Plan Distribution",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        StatCard("Free", freeUsers.toString(), Color(0xFF607D8B), Modifier.weight(1f))
                        StatCard("Premium", premiumUsers.toString(), AdminInfo, Modifier.weight(1f))
                        StatCard("Admin", adminUsers.toString(), AdminPrimary, Modifier.weight(1f))
                    }
                }
            }
        }

        item {
            ElevatedCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.elevatedCardColors(containerColor = AdminCard),
                elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "Usage Activity",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    if (isLoadingStats) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(120.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator()
                        }
                    } else {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            StatCard("Daily Active Users", stats.dailyActiveUsers.toString(), AdminSuccess, Modifier.weight(1f))
                            StatCard("Monthly Active Users", stats.monthlyActiveUsers.toString(), AdminInfo, Modifier.weight(1f))
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            StatCard("Chatbot Sessions", stats.chatbotUsageCount.toString(), AdminPrimary, Modifier.weight(1f))
                            StatCard("Tracked Users", stats.totalUsers.toString(), Color(0xFF455A64), Modifier.weight(1f))
                        }
                    }
                }
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                ElevatedCard(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.elevatedCardColors(containerColor = AdminCard),
                    elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Library Size", style = MaterialTheme.typography.bodyMedium, color = AdminTextSecondary)
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            totalBooks.toString(),
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                ElevatedCard(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.elevatedCardColors(containerColor = AdminCard),
                    elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Average Age", style = MaterialTheme.typography.bodyMedium, color = AdminTextSecondary)
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            "$avgAge yrs",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }

        item {
            ElevatedCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.elevatedCardColors(containerColor = AdminCard),
                elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "Age Distribution",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    val ageGroups = users.groupBy {
                        when {
                            it.age < 6 -> "Under 6"
                            it.age < 10 -> "6-9"
                            it.age < 13 -> "10-12"
                            else -> "13+"
                        }
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        ageGroups.forEach { (ageGroup, groupUsers) ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(ageGroup, style = MaterialTheme.typography.bodyMedium)
                                Text(
                                    "${groupUsers.size} users",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                            HorizontalDivider(color = AdminBorder)
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
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Column {
                Text(
                    "Analytics",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    "Search trends, engagement, and content performance",
                    style = MaterialTheme.typography.bodyMedium,
                    color = AdminTextSecondary
                )
            }
        }

        item {
            ElevatedCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.elevatedCardColors(containerColor = AdminCard)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Top Searched Topics", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Spacer(modifier = Modifier.height(12.dp))

                    if (topSearchedTopics.isEmpty()) {
                        Text("No search data yet.", color = AdminTextSecondary)
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            topSearchedTopics.take(10).forEachIndexed { index, topic ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            "${index + 1}. ${topic.query}",
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.SemiBold,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            "Searched ${topic.count} times",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = AdminTextSecondary
                                        )
                                    }
                                }
                                if (index < topSearchedTopics.take(10).lastIndex) {
                                    HorizontalDivider(color = AdminBorder)
                                }
                            }
                        }
                    }
                }
            }
        }

        item {
            ElevatedCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.elevatedCardColors(containerColor = AdminCard)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Top Viewed Books", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Spacer(modifier = Modifier.height(12.dp))

                    if (topViewedBooks.isEmpty()) {
                        Text("No book view data yet.", color = AdminTextSecondary)
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            topViewedBooks.take(10).forEachIndexed { index, book ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            "${index + 1}. ${book.bookTitle}",
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.SemiBold,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            "Viewed ${book.count} times",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = AdminTextSecondary
                                        )
                                    }
                                }
                                if (index < topViewedBooks.take(10).lastIndex) {
                                    HorizontalDivider(color = AdminBorder)
                                }
                            }
                        }
                    }
                }
            }
        }

        item {
            ElevatedCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.elevatedCardColors(containerColor = AdminCard)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Top Drop-Off Points", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Spacer(modifier = Modifier.height(12.dp))

                    if (topDropOffs.isEmpty()) {
                        Text("No drop-off data yet.", color = AdminTextSecondary)
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            topDropOffs.take(10).forEachIndexed { index, item ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            "${index + 1}. ${item.itemTitle}",
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.SemiBold,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            "Dropped off ${item.count} times • Avg ${item.averageDurationSeconds}s",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = AdminTextSecondary
                                        )
                                    }
                                }
                                if (index < topDropOffs.take(10).lastIndex) {
                                    HorizontalDivider(color = AdminBorder)
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
fun StatCard(label: String, value: String, color: Color, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.height(110.dp),
        colors = CardDefaults.cardColors(containerColor = AdminCard),
        shape = RoundedCornerShape(14.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(14.dp),
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                color = AdminTextSecondary
            )

            Spacer(modifier = Modifier.height(12.dp))

            Box(
                modifier = Modifier
                    .height(4.dp)
                    .fillMaxWidth()
                    .background(color = color, shape = RoundedCornerShape(100.dp))
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
            placeholder = { Text("Search books (e.g. dinosaurs, space...)") },
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
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(searchResults) { book ->
                    BookAdminCard(
                        book = book,
                        onAction = {
                            viewModel.addBookToLibrary(book)
                            scope.launch {
                                snackbarHostState.showSnackbar("Book added")
                            }
                        },
                        onCardClick = {
                            val url = book.readerUrl.ifBlank { book.bookUrl }
                            if (url.isNotBlank()) onViewBook(book.title, url, false)
                        },
                        actionIcon = Icons.Default.Add
                    )
                }
            }
        }
    }
}

@Composable
fun CategoryManagementTab(viewModel: AdminViewModel) {
    val categories by viewModel.categories.collectAsState()

    var showAddDialog by remember { mutableStateOf(false) }
    var categoryToEdit by remember { mutableStateOf<BookCategory?>(null) }
    var categoryToDelete by remember { mutableStateOf<BookCategory?>(null) }

    LaunchedEffect(Unit) {
        viewModel.loadCategories()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            "Book Categories",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            "Add, edit, and remove book categories",
            style = MaterialTheme.typography.bodyMedium,
            color = AdminTextSecondary
        )

        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End
        ) {
            Button(
                onClick = { showAddDialog = true },
                colors = ButtonDefaults.buttonColors(containerColor = AdminPrimary)
            ) {
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Add Category")
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (categories.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text("No categories found.", color = AdminTextSecondary)
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(categories) { category ->
                    ElevatedCard(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.elevatedCardColors(containerColor = AdminCard),
                        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 1.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    category.name,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    "ID: ${category.id}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = AdminTextSecondary
                                )
                                if (category.description.isNotBlank()) {
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        category.description,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = AdminTextSecondary
                                    )
                                }
                            }

                            IconButton(onClick = { categoryToEdit = category }) {
                                Icon(Icons.Default.Edit, contentDescription = "Edit")
                            }

                            IconButton(onClick = { categoryToDelete = category }) {
                                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = AdminDanger)
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        CategoryDialog(
            title = "Add Category",
            onDismiss = { showAddDialog = false },
            onConfirm = { name, description ->
                viewModel.addCategory(name, description)
                showAddDialog = false
            }
        )
    }

    if (categoryToEdit != null) {
        CategoryDialog(
            title = "Edit Category",
            initialName = categoryToEdit!!.name,
            initialDescription = categoryToEdit!!.description,
            onDismiss = { categoryToEdit = null },
            onConfirm = { name, description ->
                viewModel.updateCategory(
                    id = categoryToEdit!!.id,
                    name = name,
                    description = description
                )
                categoryToEdit = null
            }
        )
    }

    if (categoryToDelete != null) {
        AlertDialog(
            onDismissRequest = { categoryToDelete = null },
            title = { Text("Delete Category") },
            text = {
                Text("Are you sure you want to delete \"${categoryToDelete!!.name}\"?")
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteCategory(categoryToDelete!!.id)
                        categoryToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AdminDanger)
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { categoryToDelete = null }) {
                    Text("Cancel")
                }
            },
            containerColor = AdminCard
        )
    }
}

@Composable
fun CategoryDialog(
    title: String,
    initialName: String = "",
    initialDescription: String = "",
    onDismiss: () -> Unit,
    onConfirm: (String, String) -> Unit
) {
    var name by remember { mutableStateOf(initialName) }
    var description by remember { mutableStateOf(initialDescription) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Category Name") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onConfirm(name.trim(), description.trim())
                },
                enabled = name.trim().isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = AdminPrimary)
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
        containerColor = AdminCard
    )
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

    // 🔴 Unsafe delete dialog
    if (bookToRemoveUnsafe != null) {
        var reason by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { bookToRemoveUnsafe = null },
            icon = {
                Icon(Icons.Default.Warning, null, tint = AdminDanger)
            },
            title = { Text("Remove Unsafe Content") },
            text = {
                Column {
                    Text("This will permanently remove the book.")
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = reason,
                        onValueChange = { reason = it },
                        label = { Text("Reason") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.removeUnsafeContent(
                            bookToRemoveUnsafe!!.id,
                            reason
                        )
                        scope.launch {
                            snackbarHostState.showSnackbar("Removed as unsafe")
                        }
                        bookToRemoveUnsafe = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AdminDanger)
                ) {
                    Text("Remove")
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

        Text(
            "Curated Library (${books.size})",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold
        )

        Spacer(modifier = Modifier.height(12.dp))

        LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            items(books) { book ->

                Card(
                    modifier = Modifier.fillMaxWidth().clickable {
                        val url = book.readerUrl.ifBlank { book.bookUrl }
                        if (url.isNotBlank()) onViewBook(book.title, url, false)
                    },
                    colors = CardDefaults.cardColors(containerColor = AdminCard),
                    shape = RoundedCornerShape(14.dp),
                    elevation = CardDefaults.cardElevation(1.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {

                        AsyncImage(
                            model = book.coverUrl,
                            contentDescription = null,
                            modifier = Modifier
                                .size(60.dp)
                                .clip(RoundedCornerShape(10.dp)),
                            contentScale = ContentScale.Crop
                        )

                        Spacer(modifier = Modifier.width(12.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(book.title, fontWeight = FontWeight.SemiBold)
                            Text(
                                "By ${book.author}",
                                style = MaterialTheme.typography.bodySmall,
                                color = AdminTextSecondary
                            )
                        }

                        Row {

                            // ⚠️ Unsafe delete
                            IconButton(onClick = {
                                bookToRemoveUnsafe = book
                            }) {
                                Icon(
                                    Icons.Default.Warning,
                                    contentDescription = "Unsafe",
                                    tint = AdminDanger
                                )
                            }

                            // 🗑️ Normal delete
                            IconButton(onClick = {
                                viewModel.deleteBookFromLibrary(book.id)
                                scope.launch {
                                    snackbarHostState.showSnackbar("Deleted")
                                }
                            }) {
                                Icon(
                                    Icons.Default.Delete,
                                    contentDescription = "Delete",
                                    tint = Color.Gray
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
fun BookAdminCard(
    book: Book,
    onAction: () -> Unit,
    onCardClick: () -> Unit,
    actionIcon: androidx.compose.ui.graphics.vector.ImageVector
) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable { onCardClick() },
        colors = CardDefaults.cardColors(containerColor = AdminCard),
        shape = RoundedCornerShape(14.dp),
        elevation = CardDefaults.cardElevation(1.dp)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {

            AsyncImage(
                model = book.coverUrl,
                contentDescription = null,
                modifier = Modifier
                    .size(60.dp)
                    .clip(RoundedCornerShape(10.dp)),
                contentScale = ContentScale.Crop
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(book.title, fontWeight = FontWeight.SemiBold)
                Text(
                    "By ${book.author}",
                    style = MaterialTheme.typography.bodySmall,
                    color = AdminTextSecondary
                )
            }

            IconButton(onClick = onAction) {
                Icon(actionIcon, null, tint = AdminPrimary)
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

    val filteredUsers = remember(
        users,
        appliedSearch,
        appliedPlan,
        appliedStatus,
        appliedAgeGroup,
        appliedAccountType
    ) {
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
        }.sortedWith(
            compareBy<User> { user -> user.planType != PlanType.ADMIN }
                .thenBy { it.name }
        )
    }

    if (userToDelete != null) {
        val isCurrentUser = userToDelete?.id == currentUserId

        AlertDialog(
            onDismissRequest = { userToDelete = null },
            icon = { Icon(Icons.Default.Delete, null, tint = AdminDanger) },
            title = { Text("Delete User") },
            text = {
                Text(
                    if (isCurrentUser) {
                        "You cannot delete your own account."
                    } else {
                        "Are you sure you want to delete ${userToDelete!!.name} (${userToDelete!!.email})?\n\nThis will remove their profile, chat history, favorites, and reading history. This cannot be undone."
                    }
                )
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
                    colors = ButtonDefaults.buttonColors(containerColor = AdminDanger)
                ) {
                    if (isDeletingUser) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            color = Color.White,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text(if (isCurrentUser) "Cannot Delete" else "Delete")
                    }
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { userToDelete = null }) {
                    Text("Cancel")
                }
            },
            containerColor = AdminCard
        )
    }

    if (userToSuspend != null) {
        val isCurrentUser = userToSuspend?.id == currentUserId

        AlertDialog(
            onDismissRequest = { userToSuspend = null },
            icon = { Icon(Icons.Default.PauseCircle, null, tint = AdminWarning) },
            title = { Text("Suspend User") },
            text = {
                Text(
                    if (isCurrentUser) {
                        "You are suspending yourself. You will be unable to access the app until reactivated by another admin."
                    } else {
                        "Are you sure you want to suspend ${userToSuspend!!.name} (${userToSuspend!!.email})?\n\nThe user will be temporarily unable to access the app until reactivated."
                    }
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.suspendUser(userToSuspend!!.id)
                        userToSuspend = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AdminWarning)
                ) {
                    Text("Suspend")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { userToSuspend = null }) {
                    Text("Cancel")
                }
            },
            containerColor = AdminCard
        )
    }

    if (userToBan != null) {
        val isCurrentUser = userToBan?.id == currentUserId

        AlertDialog(
            onDismissRequest = { userToBan = null },
            icon = { Icon(Icons.Default.Block, null, tint = AdminDanger) },
            title = { Text("Ban User") },
            text = {
                Text(
                    if (isCurrentUser) {
                        "You are banning yourself. You will be permanently restricted from accessing the app until manually reactivated by another admin."
                    } else {
                        "Are you sure you want to ban ${userToBan!!.name} (${userToBan!!.email})?\n\nThe user will be permanently restricted from accessing the app until manually reactivated."
                    }
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.banUser(userToBan!!.id)
                        userToBan = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AdminDanger)
                ) {
                    Text("Ban")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { userToBan = null }) {
                    Text("Cancel")
                }
            },
            containerColor = AdminCard
        )
    }

    if (userToActivate != null) {
        val isCurrentUser = userToActivate?.id == currentUserId

        AlertDialog(
            onDismissRequest = { userToActivate = null },
            icon = { Icon(Icons.Default.CheckCircle, null, tint = AdminSuccess) },
            title = { Text("Activate User") },
            text = {
                Text(
                    if (isCurrentUser) {
                        "You are reactivating yourself. This will restore your account access."
                    } else {
                        "Are you sure you want to reactivate ${userToActivate!!.name} (${userToActivate!!.email})?\n\nThe user will regain full access to the app."
                    }
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.activateUser(userToActivate!!.id)
                        userToActivate = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AdminSuccess)
                ) {
                    Text("Activate")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { userToActivate = null }) {
                    Text("Cancel")
                }
            },
            containerColor = AdminCard
        )
    }

    if (userToViewActivity != null) {
        UserActivityDialog(
            user = userToViewActivity!!,
            viewModel = viewModel,
            onDismiss = { userToViewActivity = null }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            "User Management",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            "Search, filter, and manage user accounts",
            style = MaterialTheme.typography.bodyMedium,
            color = AdminTextSecondary
        )

        Spacer(modifier = Modifier.height(16.dp))

        ElevatedCard(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.elevatedCardColors(containerColor = AdminCard),
            elevation = CardDefaults.elevatedCardElevation(defaultElevation = 1.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                OutlinedTextField(
                    value = pendingSearch,
                    onValueChange = { pendingSearch = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Search users by name, email, or ID") },
                    trailingIcon = {
                        Icon(Icons.Default.Search, null, tint = AdminTextSecondary)
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(12.dp))

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
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

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
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

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
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

                    Button(
                        onClick = {
                            appliedSearch = pendingSearch.trim()
                            appliedPlan = pendingPlan
                            appliedStatus = pendingStatus
                            appliedAgeGroup = pendingAgeGroup
                            appliedAccountType = pendingAccountType
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = AdminPrimary)
                    ) {
                        Text("Apply Filters")
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        Text(
            "Matching users: ${filteredUsers.size}",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            color = AdminTextSecondary
        )

        Spacer(modifier = Modifier.height(12.dp))

        if (filteredUsers.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No users match the current filters.", color = AdminTextSecondary)
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(filteredUsers) { user ->
                    val isAdminUser = user.email.lowercase() == "admin@littledino.com" || user.planType == PlanType.ADMIN

                    ElevatedCard(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.elevatedCardColors(containerColor = AdminCard),
                        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 1.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                modifier = Modifier.size(42.dp),
                                shape = CircleShape,
                                color = AdminPrimary.copy(alpha = 0.10f)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        user.name.take(1).uppercase(),
                                        fontWeight = FontWeight.SemiBold,
                                        color = AdminPrimary
                                    )
                                }
                            }

                            Spacer(Modifier.width(12.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(user.name, fontWeight = FontWeight.SemiBold)
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    user.email,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = AdminTextSecondary
                                )
                            }

                            Column(horizontalAlignment = Alignment.Start) {
                                PlanBadge(user.planType)
                                Spacer(modifier = Modifier.height(6.dp))
                                StatusBadge(user.status)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    "Age: ${user.age}",
                                    fontSize = 11.sp,
                                    color = AdminTextSecondary
                                )
                            }

                            if (!isAdminUser) {
                                var expanded by remember { mutableStateOf(false) }

                                Box {
                                    IconButton(onClick = { expanded = true }) {
                                        Icon(Icons.Default.MoreVert, "More actions")
                                    }

                                    DropdownMenu(
                                        expanded = expanded,
                                        onDismissRequest = { expanded = false },
                                        containerColor = AdminCard
                                    ) {
                                        when (user.status) {
                                            UserStatus.ACTIVE -> {
                                                DropdownMenuItem(
                                                    text = { Text("Suspend User") },
                                                    onClick = {
                                                        userToSuspend = user
                                                        expanded = false
                                                    },
                                                    leadingIcon = {
                                                        Icon(Icons.Default.PauseCircle, null, tint = AdminWarning)
                                                    }
                                                )
                                                DropdownMenuItem(
                                                    text = { Text("Ban User") },
                                                    onClick = {
                                                        userToBan = user
                                                        expanded = false
                                                    },
                                                    leadingIcon = {
                                                        Icon(Icons.Default.Block, null, tint = AdminDanger)
                                                    }
                                                )
                                            }

                                            UserStatus.SUSPENDED -> {
                                                DropdownMenuItem(
                                                    text = { Text("Activate User") },
                                                    onClick = {
                                                        userToActivate = user
                                                        expanded = false
                                                    },
                                                    leadingIcon = {
                                                        Icon(Icons.Default.CheckCircle, null, tint = AdminSuccess)
                                                    }
                                                )
                                                DropdownMenuItem(
                                                    text = { Text("Ban User") },
                                                    onClick = {
                                                        userToBan = user
                                                        expanded = false
                                                    },
                                                    leadingIcon = {
                                                        Icon(Icons.Default.Block, null, tint = AdminDanger)
                                                    }
                                                )
                                            }

                                            UserStatus.BANNED -> {
                                                DropdownMenuItem(
                                                    text = { Text("Activate User") },
                                                    onClick = {
                                                        userToActivate = user
                                                        expanded = false
                                                    },
                                                    leadingIcon = {
                                                        Icon(Icons.Default.CheckCircle, null, tint = AdminSuccess)
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
                                                Icon(Icons.Default.Info, null, tint = AdminPrimary)
                                            }
                                        )

                                        DropdownMenuItem(
                                            text = { Text("Delete User") },
                                            onClick = {
                                                userToDelete = user
                                                expanded = false
                                            },
                                            leadingIcon = {
                                                Icon(Icons.Default.Delete, null, tint = AdminDanger)
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
    val statusColor = if (attempt.success) AdminSuccess else AdminDanger
    val statusText = if (attempt.success) "Success" else "Failed"

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = AdminCard),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                modifier = Modifier.size(10.dp),
                shape = CircleShape,
                color = statusColor
            ) {}

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    attempt.email,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    "$statusText • ${attempt.timestamp.toDate().let {
                        java.text.SimpleDateFormat("MMM dd, HH:mm", java.util.Locale.getDefault()).format(it)
                    }}",
                    style = MaterialTheme.typography.bodySmall,
                    color = AdminTextSecondary
                )

                if (!attempt.success && attempt.failureReason.isNotBlank()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        "Reason: ${attempt.failureReason}",
                        style = MaterialTheme.typography.bodySmall,
                        color = AdminDanger
                    )
                }

                if (attempt.ipAddress.isNotBlank()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        "IP: ${attempt.ipAddress}",
                        style = MaterialTheme.typography.bodySmall,
                        color = AdminTextSecondary
                    )
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
        SecuritySeverity.LOW -> AdminWarning
        SecuritySeverity.MEDIUM -> Color(0xFFE65100)
        SecuritySeverity.HIGH -> AdminDanger
        SecuritySeverity.CRITICAL -> Color(0xFF8B0000)
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
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = AdminCard),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.Top
        ) {
            Surface(
                modifier = Modifier.size(10.dp),
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
                        Text(
                            activity.email,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            "$activityTypeText • ${activity.timestamp.toDate().let {
                                java.text.SimpleDateFormat("MMM dd, HH:mm", java.util.Locale.getDefault()).format(it)
                            }}",
                            style = MaterialTheme.typography.bodySmall,
                            color = AdminTextSecondary
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
                            modifier = Modifier.height(34.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                        ) {
                            Text("Resolve", fontSize = 12.sp)
                        }
                    } else {
                        Surface(
                            color = AdminSuccess.copy(alpha = 0.10f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                "Resolved",
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                fontSize = 11.sp,
                                color = AdminSuccess,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }

                if (activity.details.isNotBlank()) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(activity.details, style = MaterialTheme.typography.bodySmall)
                }

                if (activity.ipAddress.isNotBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        "IP: ${activity.ipAddress}",
                        style = MaterialTheme.typography.bodySmall,
                        color = AdminTextSecondary
                    )
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
        modifier = Modifier
            .fillMaxWidth(0.95f)
            .fillMaxHeight(0.90f),
        containerColor = AdminCard,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    modifier = Modifier.size(36.dp),
                    shape = CircleShape,
                    color = AdminPrimary.copy(alpha = 0.10f)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            user.name.take(1).uppercase(),
                            fontWeight = FontWeight.SemiBold,
                            color = AdminPrimary,
                            fontSize = 14.sp
                        )
                    }
                }

                Spacer(Modifier.width(12.dp))

                Column {
                    Text("${user.name}'s Activity", style = MaterialTheme.typography.titleLarge)
                    Text(
                        user.email,
                        style = MaterialTheme.typography.bodySmall,
                        color = AdminTextSecondary
                    )
                }
            }
        },
        text = {
            if (isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(220.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else {
                Column(modifier = Modifier.fillMaxWidth()) {
                    ElevatedCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.elevatedCardColors(containerColor = AdminCard),
                        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 1.dp)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Text(
                                "Account Information",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Spacer(Modifier.height(10.dp))

                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Plan", style = MaterialTheme.typography.bodyMedium)
                                PlanBadge(user.planType)
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Status", style = MaterialTheme.typography.bodyMedium)
                                StatusBadge(user.status)
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Age", style = MaterialTheme.typography.bodyMedium)
                                Text(
                                    "${user.age} years old",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Medium
                                )
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Joined", style = MaterialTheme.typography.bodyMedium)
                                Text(
                                    user.createdAt.toDate().let {
                                        java.text.SimpleDateFormat("MMM dd, yyyy", java.util.Locale.getDefault()).format(it)
                                    },
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }

                    Text(
                        "Recent Reading Activity (${userReadingHistory.size})",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    if (userReadingHistory.isEmpty()) {
                        Text(
                            "No reading history found",
                            style = MaterialTheme.typography.bodyMedium,
                            color = AdminTextSecondary,
                            modifier = Modifier.padding(bottom = 16.dp)
                        )
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .height(130.dp)
                                .fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            items(userReadingHistory.take(5)) { history ->
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(10.dp),
                                    colors = CardDefaults.cardColors(containerColor = AdminBg)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(10.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            Icons.AutoMirrored.Filled.LibraryBooks,
                                            null,
                                            modifier = Modifier.size(16.dp),
                                            tint = AdminPrimary
                                        )
                                        Spacer(Modifier.width(8.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                history.title,
                                                style = MaterialTheme.typography.bodySmall,
                                                fontWeight = FontWeight.Medium,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Text(
                                                "Opened: ${history.openedAt.toDate().let {
                                                    java.text.SimpleDateFormat("MMM dd, HH:mm", java.util.Locale.getDefault()).format(it)
                                                }}",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = AdminTextSecondary
                                            )
                                        }
                                    }
                                }
                            }
                        }
                        Spacer(Modifier.height(16.dp))
                    }

                    Text(
                        "Recent Chat Messages (${userChatHistory.size})",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    if (userChatHistory.isEmpty()) {
                        Text(
                            "No chat history found",
                            style = MaterialTheme.typography.bodyMedium,
                            color = AdminTextSecondary
                        )
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .height(130.dp)
                                .fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            items(userChatHistory.take(5)) { message ->
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(10.dp),
                                    colors = CardDefaults.cardColors(containerColor = AdminBg)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(10.dp),
                                        verticalAlignment = Alignment.Top
                                    ) {
                                        Icon(
                                            if (message.role == MessageRole.USER) Icons.Default.Person else Icons.Default.SmartToy,
                                            null,
                                            modifier = Modifier.size(16.dp),
                                            tint = if (message.role == MessageRole.USER) AdminPrimary else AdminSuccess
                                        )
                                        Spacer(Modifier.width(8.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                if (message.role == MessageRole.USER) "User" else "Bot",
                                                style = MaterialTheme.typography.bodySmall,
                                                fontWeight = FontWeight.Medium
                                            )
                                            Text(
                                                message.content,
                                                style = MaterialTheme.typography.bodySmall,
                                                maxLines = 2,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Text(
                                                message.timestamp.toDate().let {
                                                    java.text.SimpleDateFormat("MMM dd, HH:mm", java.util.Locale.getDefault()).format(it)
                                                },
                                                style = MaterialTheme.typography.bodySmall,
                                                color = AdminTextSecondary
                                            )
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
fun SecurityControlPanel(
    viewModel: AdminViewModel,
    snackbarHostState: SnackbarHostState
) {
    val loginAttempts by viewModel.loginAttempts.collectAsState()
    val suspiciousActivities by viewModel.suspiciousActivities.collectAsState()
    val isLoadingSecurityData by viewModel.isLoadingSecurityData.collectAsState()

    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        viewModel.loadSecurityData()
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Column {
                Text(
                    "Security Control Panel",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    "Monitor login attempts and suspicious activities",
                    style = MaterialTheme.typography.bodyMedium,
                    color = AdminTextSecondary
                )
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                OutlinedButton(
                    onClick = { viewModel.loadSecurityData() },
                    enabled = !isLoadingSecurityData
                ) {
                    if (isLoadingSecurityData) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    Text("Refresh Data")
                }
            }
        }

        item {
            ElevatedCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.elevatedCardColors(containerColor = AdminCard),
                elevation = CardDefaults.elevatedCardElevation(defaultElevation = 1.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "Recent Login Attempts (${loginAttempts.size})",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        val failedCount = loginAttempts.count { !it.success }
                        Text(
                            "$failedCount failed",
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (failedCount > 0) AdminDanger else AdminPrimary
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    if (loginAttempts.isEmpty()) {
                        Text("No login attempts recorded", color = AdminTextSecondary)
                    } else {
                        loginAttempts.take(10).forEachIndexed { index, attempt ->
                            LoginAttemptCard(attempt)
                            if (index < loginAttempts.take(10).lastIndex) {
                                Spacer(modifier = Modifier.height(8.dp))
                            }
                        }
                    }
                }
            }
        }

        item {
            ElevatedCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.elevatedCardColors(containerColor = AdminCard),
                elevation = CardDefaults.elevatedCardElevation(defaultElevation = 1.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "Suspicious Activities (${suspiciousActivities.size})",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        val unresolvedCount = suspiciousActivities.count { !it.resolved }
                        Text(
                            "$unresolvedCount unresolved",
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (unresolvedCount > 0) AdminDanger else AdminPrimary
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    if (suspiciousActivities.isEmpty()) {
                        Text("No suspicious activities detected", color = AdminTextSecondary)
                    } else {
                        suspiciousActivities.take(10).forEachIndexed { index, activity ->
                            SuspiciousActivityCard(activity, viewModel, snackbarHostState, scope)
                            if (index < suspiciousActivities.take(10).lastIndex) {
                                Spacer(modifier = Modifier.height(8.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun NotificationTab(
    viewModel: AdminViewModel,
    snackbarHostState: SnackbarHostState
) {
    val scope = rememberCoroutineScope()
    var notificationState by remember { mutableStateOf(AdminNotificationUiState()) }

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
            modifier = Modifier
                .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                .fillMaxWidth(),
            textStyle = TextStyle(fontSize = 12.sp),
            shape = RoundedCornerShape(12.dp),
            maxLines = 1
        )

        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { onExpandedChange(false) },
            containerColor = AdminCard
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
        PlanType.FREE -> Color(0xFF607D8B)
        PlanType.PREMIUM -> AdminInfo
        PlanType.ADMIN -> AdminPrimary
    }

    Surface(
        color = color.copy(alpha = 0.10f),
        contentColor = color,
        shape = RoundedCornerShape(8.dp)
    ) {
        Text(
            text = planType.name,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            fontSize = 10.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
fun StatusBadge(status: UserStatus) {
    val (color, text) = when (status) {
        UserStatus.ACTIVE -> AdminSuccess to "Active"
        UserStatus.SUSPENDED -> AdminWarning to "Suspended"
        UserStatus.BANNED -> AdminDanger to "Banned"
    }

    Surface(
        color = color.copy(alpha = 0.10f),
        contentColor = color,
        shape = RoundedCornerShape(8.dp)
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            fontSize = 10.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}
