package com.kidsrec.chatbot.ui.admin

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.kidsrec.chatbot.R
import com.kidsrec.chatbot.data.model.Book
import com.kidsrec.chatbot.data.model.PlanType
import com.kidsrec.chatbot.data.model.User
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminScreen(
    viewModel: AdminViewModel,
    onLogout: () -> Unit,
    onViewBook: (String, String, Boolean) -> Unit
) {
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var currentScreen by remember { mutableStateOf("Users") }

    val users by viewModel.users.collectAsState()
    val books by viewModel.curatedBooks.collectAsState()

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet {
                Column(modifier = Modifier.padding(16.dp)) {
                    Image(
                        painter = painterResource(id = R.drawable.little_dino),
                        contentDescription = null,
                        modifier = Modifier.size(100.dp).padding(bottom = 16.dp)
                    )
                    Text("Admin Dashboard", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                    HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))
                    
                    NavigationDrawerItem(
                        label = { Text("User Management") },
                        selected = currentScreen == "Users",
                        onClick = { currentScreen = "Users"; scope.launch { drawerState.close() } },
                        icon = { Icon(Icons.Default.Group, null) }
                    )
                    NavigationDrawerItem(
                        label = { Text("Book Library") },
                        selected = currentScreen == "Library",
                        onClick = { currentScreen = "Library"; scope.launch { drawerState.close() } },
                        icon = { Icon(Icons.Default.Book, null) }
                    )
                    NavigationDrawerItem(
                        label = { Text("Add Books") },
                        selected = currentScreen == "Curator",
                        onClick = { currentScreen = "Curator"; scope.launch { drawerState.close() } },
                        icon = { Icon(Icons.Default.Search, null) }
                    )
                    NavigationDrawerItem(
                        label = { Text("Settings") },
                        selected = currentScreen == "Settings",
                        onClick = { currentScreen = "Settings"; scope.launch { drawerState.close() } },
                        icon = { Icon(Icons.Default.Settings, null) }
                    )
                    
                    Spacer(modifier = Modifier.weight(1f))
                    
                    Button(
                        onClick = { viewModel.logout(onLogout) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Logout, null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Logout")
                    }
                }
            }
        }
    ) {
        Scaffold(
            snackbarHost = { SnackbarHost(snackbarHostState) },
            topBar = {
                TopAppBar(
                    title = { Text(currentScreen) },
                    navigationIcon = {
                        IconButton(onClick = { scope.launch { drawerState.open() } }) {
                            Icon(Icons.Default.Menu, "Open Menu")
                        }
                    }
                )
            }
        ) { padding ->
            Box(modifier = Modifier.padding(padding).fillMaxSize()) {
                when (currentScreen) {
                    "Users" -> UserManagementTab(users)
                    "Library" -> BookLibraryTab(books, viewModel, snackbarHostState, onViewBook)
                    "Curator" -> CuratorSearchTab(viewModel, snackbarHostState, onViewBook)
                    "Settings" -> SettingsTab()
                }
            }
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

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("Search Visual Kids Books", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Text("Find illustrated stories with drawings.", style = MaterialTheme.typography.bodySmall)
        
        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("Search by title (e.g. Disney)...") },
            trailingIcon = {
                IconButton(onClick = { viewModel.searchBooks(query) }) {
                    Icon(Icons.Default.Search, null)
                }
            },
            singleLine = true
        )

        Spacer(modifier = Modifier.height(16.dp))

        if (isSearching) {
            CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                items(searchResults) { book ->
                    BookAdminCard(
                        book = book,
                        onAction = { 
                            viewModel.addBookToLibrary(book)
                            scope.launch { snackbarHostState.showSnackbar("Added: ${book.title}") }
                        },
                        onCardClick = {
                            val url = book.readerUrl ?: book.openLibraryUrl ?: ""
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
fun BookLibraryTab(
    books: List<Book>, 
    viewModel: AdminViewModel, 
    snackbarHostState: SnackbarHostState,
    onViewBook: (String, String, Boolean) -> Unit
) {
    val scope = rememberCoroutineScope()
    LazyColumn(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        item {
            Text("Library Collection (${books.size})", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))
        }
        items(books) { book ->
            BookAdminCard(
                book = book,
                onAction = { 
                    viewModel.deleteBookFromLibrary(book.id)
                    scope.launch { snackbarHostState.showSnackbar("Deleted: ${book.title}") }
                },
                onCardClick = {
                    val url = book.readerUrl ?: book.openLibraryUrl ?: ""
                    if (url.isNotBlank()) onViewBook(book.title, url, false)
                },
                actionIcon = Icons.Default.Delete,
                actionColor = MaterialTheme.colorScheme.error
            )
        }
    }
}

@Composable
fun BookAdminCard(
    book: Book,
    onAction: () -> Unit,
    onCardClick: () -> Unit,
    actionIcon: androidx.compose.ui.graphics.vector.ImageVector,
    actionColor: Color = MaterialTheme.colorScheme.primary
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCardClick() }
    ) {
        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            AsyncImage(
                model = book.coverUrl,
                contentDescription = null,
                modifier = Modifier.size(60.dp).clip(RoundedCornerShape(4.dp)),
                contentScale = ContentScale.Crop
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(book.title, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text("By ${book.author}", style = MaterialTheme.typography.bodySmall)
                Row(modifier = Modifier.padding(top = 4.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Surface(color = MaterialTheme.colorScheme.secondaryContainer, shape = RoundedCornerShape(4.dp)) {
                        Text(book.ageRating, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp), fontSize = 10.sp)
                    }
                    DifficultyBadge(book.readingAvailability)
                }
            }
            IconButton(onClick = onAction) {
                Icon(actionIcon, null, tint = actionColor)
            }
        }
    }
}

@Composable
fun DifficultyBadge(level: String) {
    val color = when (level) {
        "Easy" -> Color(0xFF4CAF50)
        "Intermediate" -> Color(0xFF2196F3)
        "Hard" -> Color(0xFFFF9800)
        else -> Color.Gray
    }
    Surface(
        color = color.copy(alpha = 0.1f),
        contentColor = color,
        shape = RoundedCornerShape(4.dp)
    ) {
        Text(level, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp), fontSize = 10.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun UserManagementTab(users: List<User>) {
    LazyColumn(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        item {
            Text("Active Users", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))
        }
        items(users) { user ->
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(user.name, fontWeight = FontWeight.Bold)
                        PlanBadge(user.planType)
                    }
                    Text(user.email, style = MaterialTheme.typography.bodySmall)
                    Text("Age: ${user.age}", style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}

@Composable
fun PlanBadge(planType: PlanType) {
    val color = when (planType) {
        PlanType.FREE -> Color.Gray
        PlanType.PREMIUM -> Color(0xFFFFD700)
        PlanType.FAMILY -> Color(0xFF4CAF50)
        PlanType.ADMIN -> Color(0xFF9C27B0)
    }
    Surface(
        color = color.copy(alpha = 0.2f),
        contentColor = color,
        shape = RoundedCornerShape(8.dp)
    ) {
        Text(text = planType.name, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp), fontSize = 10.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun SettingsTab() {
    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("Admin Settings", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Text("System configuration center.")
    }
}
