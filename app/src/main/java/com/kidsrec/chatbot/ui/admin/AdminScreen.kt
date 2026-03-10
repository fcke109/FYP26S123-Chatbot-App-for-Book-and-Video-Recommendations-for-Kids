package com.kidsrec.chatbot.ui.admin

import androidx.compose.foundation.Image
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
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

    LaunchedEffect(Unit) {
        viewModel.startManagingUsers()
    }

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
                        icon = { Icon(Icons.AutoMirrored.Filled.LibraryBooks, null) }
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
        Text("Discover ICDL Stories", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Text("Search for English picture books with drawings.", style = MaterialTheme.typography.bodySmall)
        
        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("Search by title (e.g. Cat)...") },
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
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator()
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Searching visual library...", style = MaterialTheme.typography.bodySmall)
                }
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                items(searchResults) { book ->
                    BookAdminCard(
                        book = book,
                        onAction = { 
                            viewModel.addBookToLibrary(book)
                            scope.launch { snackbarHostState.showSnackbar("Book added successfully!") }
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
    
    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Library Stories (${books.size})", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            
            // FRESH START SEEDING BUTTON
            Button(
                onClick = { 
                    viewModel.seedOfficialLibrary()
                    scope.launch { snackbarHostState.showSnackbar("Seeding library 001-010...") }
                },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
            ) {
                Icon(Icons.Default.Upload, null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text("Seed 001-010", fontSize = 12.sp)
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))

        if (books.isEmpty()) {
            Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.AutoMirrored.Filled.LibraryBooks, null, modifier = Modifier.size(64.dp), tint = Color.Gray.copy(alpha = 0.5f))
                    Text("No books in library yet.", color = Color.Gray)
                    Text("Tap 'Seed' or use 'Add Books' to start!", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                }
            }
        } else {
            LazyColumn(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(books) { book ->
                    BookAdminCard(
                        book = book,
                        onAction = { 
                            viewModel.deleteBookFromLibrary(book.id)
                            scope.launch { snackbarHostState.showSnackbar("Book removed from collection") }
                        },
                        onCardClick = {
                            val url = book.readerUrl.ifBlank { book.bookUrl }
                            if (url.isNotBlank()) onViewBook(book.title, url, false)
                        },
                        actionIcon = Icons.Default.Delete,
                        actionColor = MaterialTheme.colorScheme.error
                    )
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
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCardClick() },
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Box {
                AsyncImage(
                    model = book.coverUrl,
                    contentDescription = null,
                    modifier = Modifier.size(70.dp).clip(RoundedCornerShape(8.dp)).background(Color.Gray.copy(alpha = 0.1f)),
                    contentScale = ContentScale.Crop
                )
                if (showScore && book.searchScore > 0) {
                    Surface(
                        modifier = Modifier.align(Alignment.TopEnd).padding(2.dp),
                        color = MaterialTheme.colorScheme.tertiary,
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            "${book.searchScore}%", 
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp), 
                            fontSize = 8.sp, 
                            color = Color.White, 
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(color = MaterialTheme.colorScheme.primary, shape = CircleShape) {
                        Text(book.id, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp), fontSize = 10.sp, color = Color.White, fontWeight = FontWeight.Bold)
                    }
                    Spacer(Modifier.width(8.dp))
                    Text(book.title, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                Text("By ${book.author}", style = MaterialTheme.typography.bodySmall, maxLines = 1)
                Row(modifier = Modifier.padding(top = 4.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Surface(color = MaterialTheme.colorScheme.secondaryContainer, shape = RoundedCornerShape(4.dp)) {
                        Text("${book.ageMin}-${book.ageMax} yrs", modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp), fontSize = 10.sp)
                    }
                    DifficultyBadge(book.difficulty)
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
    val color = when (level.lowercase()) {
        "easy" -> Color(0xFF4CAF50)
        "medium" -> Color(0xFF2196F3)
        "hard" -> Color(0xFFFF9800)
        else -> Color.Gray
    }
    Surface(
        color = color.copy(alpha = 0.1f),
        contentColor = color,
        shape = RoundedCornerShape(4.dp)
    ) {
        Text(level.uppercase(), modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp), fontSize = 10.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun UserManagementTab(users: List<User>) {
    if (users.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
    } else {
        LazyColumn(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            item {
                Text("Active Users", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))
            }
            items(users) { user ->
                Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) {
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
}

@Composable
fun PlanBadge(planType: PlanType) {
    val color = when (planType) {
        PlanType.FREE -> Color.Gray
        PlanType.PREMIUM -> Color(0xFFFFD700)
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
