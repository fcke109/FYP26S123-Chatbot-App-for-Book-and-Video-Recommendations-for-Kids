package com.kidsrec.chatbot.ui.parent

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import coil.compose.AsyncImage
import com.kidsrec.chatbot.data.model.Book
import com.kidsrec.chatbot.data.model.StarterBookSeed
import com.kidsrec.chatbot.data.remote.OpenLibraryService
import com.kidsrec.chatbot.data.repository.AccountManager
import com.kidsrec.chatbot.data.repository.BookDataManager
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

// Stores selected interests, generated invite code, recommendation loading state, and starter book selections
data class ParentInviteSetupUiState(
    // Interests selected by the parent for the child
    val selectedInterests: Set<String> = emptySet(),
    // Tracks whether the invite code is currently being generated
    val isGenerating: Boolean = false,
    // Stores the generated invite code after successful creation
    val generatedCode: String? = null,
    // Stores validation or loading errors shown to the parent
    val errorMessage: String? = null,
    // Recommended starter books based on the selected interests
    val recommendedBooks: List<Book> = emptyList(),
    // Tracks whether recommended books are being fetched
    val isLoadingRecommendations: Boolean = false,
    // Books selected to be attached to the child invite setup
    val selectedRecommendedBookIds: Set<String> = emptySet()
)

// ViewModel responsible for managing invite-code creation and starter-book recommendations
@HiltViewModel
class ParentInviteSetupViewModel @Inject constructor(
    // Handles account operations such as invite
    private val accountManager: AccountManager,
    // Provides curated books from the app library
    private val bookDataManager: BookDataManager,
    // Provides fallback book search from Open Library
    private val openLibraryService: OpenLibraryService
) : ViewModel() {

    // Internal mutable state for the invite setup screen
    private val _uiState = MutableStateFlow(ParentInviteSetupUiState())
    // Public read-only state observed by the Compose UI
    val uiState: StateFlow<ParentInviteSetupUiState> = _uiState.asStateFlow()

    // Extra keywords used to improve book matching for each selected interest
    private val interestAliases = mapOf(
        "Reading" to listOf("read", "reader", "book", "story"),
        "Science" to listOf("science", "experiment", "invent", "nature"),
        "Animals" to listOf("animal", "animals", "zoo", "pet", "wildlife"),
        "Adventure" to listOf("adventure", "journey", "quest", "explore"),
        "Fantasy" to listOf("fantasy", "magic", "dragon", "fairy"),
        "Art" to listOf("art", "draw", "paint", "creative"),
        "Music" to listOf("music", "song", "instrument", "rhythm"),
        "Sports" to listOf("sport", "sports", "team", "game"),
        "History" to listOf("history", "past", "biography", "culture"),
        "Nature" to listOf("nature", "forest", "plants", "earth"),
        "Space" to listOf("space", "planet", "star", "moon", "rocket"),
        "Dinosaurs" to listOf("dinosaur", "dinosaurs", "trex", "prehistoric"),
        "Cooking" to listOf("cook", "cooking", "food", "kitchen"),
        "Cars" to listOf("car", "cars", "truck", "vehicle"),
        "Robots" to listOf("robot", "robots", "machine", "tech"),
        "Fairy Tales" to listOf("fairy", "princess", "castle", "tale"),
        "Superheroes" to listOf("superhero", "hero", "heroes", "save"),
        "Ocean" to listOf("ocean", "sea", "fish", "marine"),
        "Puzzles" to listOf("puzzle", "riddle", "mystery", "brain"),
        "Travel" to listOf("travel", "world", "trip", "journey")
    )

    // Adds or removes a recommended book from the starter-book selection
    fun toggleRecommendedBook(book: Book) {
        val current = _uiState.value.selectedRecommendedBookIds
        val updated = if (current.contains(book.id)) {
            current - book.id
        } else {
            current + book.id
        }

        _uiState.value = _uiState.value.copy(
            selectedRecommendedBookIds = updated
        )
    }

    // Converts a full Book object into a lighter StarterBookSeed for invite setup storage
    private fun Book.toStarterBookSeed(): StarterBookSeed {
        return StarterBookSeed(
            id = id,
            title = title,
            author = author,
            coverUrl = coverUrl,
            bookUrl = bookUrl,
            readerUrl = readerUrl,
            source = source,
            ageMin = ageMin,
            ageMax = ageMax
        )
    }

    // Toggles a child interest and enforces the maximum selection limit of 5 interests
    fun toggleInterest(interest: String) {
        val current = _uiState.value.selectedInterests

        val updated = when {
            current.contains(interest) -> current - interest
            current.size < 5 -> current + interest
            else -> current
        }

        _uiState.value = _uiState.value.copy(
            selectedInterests = updated,
            errorMessage = if (!current.contains(interest) && current.size >= 5) {
                "You can select up to 5 interests only."
            } else {
                null
            }
        )
        // Refresh starter book recommendations whenever the selected interests change
        fetchRecommendedBooks(updated)
    }

    // Clears the generated invite code from the UI state
    fun clearGeneratedCode() {
        _uiState.value = _uiState.value.copy(generatedCode = null)
    }

    // Generates an invite code using selected child interests and selected starter books
    fun generateInviteCode(
        parentId: String,
        parentName: String
    ) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isGenerating = true,
                errorMessage = null
            )

            // Converts selected recommended books into starter book seed records
            val selectedStarterBooks = _uiState.value.recommendedBooks
                .filter { it.id in _uiState.value.selectedRecommendedBookIds }
                .map { it.toStarterBookSeed() }

            // Creates the invite code through AccountManager
            val result = accountManager.generateInviteCode(
                parentId = parentId,
                parentName = parentName,
                childInterests = _uiState.value.selectedInterests.toList(),
                starterBooks = selectedStarterBooks
            )

            // Updates UI state based on whether invite code generation succeeds or fails
            result.fold(
                onSuccess = { code ->
                    _uiState.value = _uiState.value.copy(
                        isGenerating = false,
                        generatedCode = code
                    )
                },
                onFailure = { e ->
                    _uiState.value = _uiState.value.copy(
                        isGenerating = false,
                        errorMessage = e.message ?: "Failed to generate invite code."
                    )
                }
            )
        }
    }

    // Fetches up to 6 recommended starter books based on the selected interests
    private fun fetchRecommendedBooks(selectedInterests: Set<String>) {
        // Clear recommendations when no interests are selected
        if (selectedInterests.isEmpty()) {
            _uiState.value = _uiState.value.copy(
                recommendedBooks = emptyList(),
                isLoadingRecommendations = false
            )
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isLoadingRecommendations = true,
                recommendedBooks = emptyList()
            )

            try {
                // Expand selected interests into related terms for stronger matching
                val selectedTerms = selectedInterests.flatMap { interest ->
                    listOf(interest) + interestAliases[interest].orEmpty()
                }.map { it.lowercase() }.distinct()

                // First retrieve curated books from the app library
                val curatedBooks = bookDataManager.getCuratedBooksFlow().first()

                // Match curated books by checking whether interest terms appear in searchable book metadata
                val curatedMatches = curatedBooks
                    .mapNotNull { book ->
                        if (!book.isKidSafe) return@mapNotNull null

                        val searchable = listOf(
                            book.title,
                            book.author,
                            book.source
                        ).joinToString(" ").lowercase()

                        val score = selectedTerms.count { term -> searchable.contains(term) }
                        if (score > 0) book to score else null
                    }
                    .sortedWith(
                        compareByDescending<Pair<Book, Int>> { it.second }
                            .thenBy { it.first.ageMin }
                            .thenBy { it.first.title }
                    )
                    .map { it.first }
                    .take(6)

                // Number of additional books needed if curated matches are fewer than 6
                val needed = (6 - curatedMatches.size).coerceAtLeast(0)

                // Use Open Library as a fallback source when curated matches are not enough
                val remoteMatches = if (needed > 0) {
                    selectedInterests
                        .flatMap { interest ->
                            try {
                                val response = openLibraryService.searchBooks(
                                    "$interest subject:\"Children's fiction\" language:eng"
                                )

                                // Convert Open Library documents into the app's Book model
                                response.docs.mapNotNull { doc ->
                                    val title = doc.title.takeIf { it.isNotBlank() } ?: return@mapNotNull null
                                    val author = doc.author_name?.firstOrNull().orEmpty().ifBlank { "Unknown" }
                                    val iaId = doc.ia?.firstOrNull()
                                    val generatedId = "remote_${title}_${author}"
                                        .lowercase()
                                        .replace(Regex("[^a-z0-9]+"), "_")

                                    Book(
                                        id = iaId ?: generatedId,
                                        title = title,
                                        author = author,
                                        coverUrl = doc.cover_i?.let {
                                            "https://covers.openlibrary.org/b/id/${it}-L.jpg"
                                        } ?: "",
                                        bookUrl = iaId?.let { "https://archive.org/embed/$it" } ?: "",
                                        readerUrl = iaId?.let { "https://archive.org/embed/$it" } ?: "",
                                        source = "ICDL/Archive",
                                        ageMin = 3,
                                        ageMax = 12,
                                        isKidSafe = true
                                    )
                                }
                            } catch (e: Exception) {
                                Log.e("ParentInviteSetupVM", "Search failed for interest: $interest", e)
                                emptyList()
                            }
                        }
                        .distinctBy { "${it.title}|${it.author}" }
                        .filter { remote ->
                            curatedMatches.none {
                                it.title.equals(remote.title, ignoreCase = true) &&
                                        it.author.equals(remote.author, ignoreCase = true)
                            }
                        }
                        .take(needed)
                } else {
                    emptyList()
                }

                // Combine curated and remote recommendations while removing duplicates
                _uiState.value = _uiState.value.copy(
                    recommendedBooks = (curatedMatches + remoteMatches).distinctBy { "${it.title}|${it.author}" },
                    isLoadingRecommendations = false
                )
            } catch (e: Exception) {
                Log.e("ParentInviteSetupVM", "Failed to fetch recommended books", e)
                _uiState.value = _uiState.value.copy(
                    recommendedBooks = emptyList(),
                    isLoadingRecommendations = false,
                    errorMessage = "Unable to load recommendations right now."
                )
            }
        }
    }
}

// Route-level composable that connects the ViewModel, snackbar, clipboard, and screen UI
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ParentInviteSetupRoute(
    parentId: String,
    parentName: String,
    onBack: () -> Unit,
    viewModel: ParentInviteSetupViewModel = hiltViewModel()
) {
    // Observes the current invite setup state from the ViewModel
    val uiState by viewModel.uiState.collectAsState()
    // Snackbar host used to notify the parent when actions complete
    val snackbarHostState = remember { SnackbarHostState() }
    // Clipboard manager used to copy the generated invite code
    val clipboardManager = LocalClipboardManager.current

    // Show feedback after an invite code is generated
    LaunchedEffect(uiState.generatedCode) {
        if (uiState.generatedCode != null) {
            snackbarHostState.showSnackbar("Invite code generated successfully.")
        }
    }

    // Passes state and ViewModel callbacks into the screen composable
    ParentInviteSetupScreen(
        uiState = uiState,
        snackbarHostState = snackbarHostState,
        onBack = onBack,
        onToggleInterest = viewModel::toggleInterest,
        onGenerateCode = {
            viewModel.generateInviteCode(parentId, parentName)
        },
        onCopyCode = { code ->
            clipboardManager.setText(AnnotatedString(code))
        },
        onToggleRecommendedBook = viewModel::toggleRecommendedBook
    )
}

// Main screen where parents select interests, choose starter books, and generate an invite code
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ParentInviteSetupScreen(
    uiState: ParentInviteSetupUiState,
    snackbarHostState: SnackbarHostState,
    onBack: () -> Unit,
    onToggleInterest: (String) -> Unit,
    onGenerateCode: () -> Unit,
    onCopyCode: (String) -> Unit,
    onToggleRecommendedBook: (Book) -> Unit
) {
    // Interest options available for the parent to choose from
    val interests = listOf(
        "Reading", "Science", "Animals", "Adventure",
        "Fantasy", "Art", "Music", "Sports", "History", "Nature",
        "Space", "Dinosaurs", "Cooking", "Cars", "Robots",
        "Fairy Tales", "Superheroes", "Ocean", "Puzzles", "Travel"
    )

    // Screen layout with top bar and snackbar support
    Scaffold(
        topBar = {
            // Top app bar with back navigation
            TopAppBar(
                title = { Text("Set Child Interests") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                text = "Choose up to 5 interests before generating the invite code.",
                style = MaterialTheme.typography.bodyLarge
            )

            // Displays how many interests have been selected
            Text(
                text = "Selected: ${uiState.selectedInterests.size}/5",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )

            // Controls whether the interest dropdown is expanded
            var interestsExpanded by remember { mutableStateOf(false) }

            // Dropdown used to select up to 5 interests for the child
            ExposedDropdownMenuBox(
                expanded = interestsExpanded,
                onExpandedChange = { interestsExpanded = !interestsExpanded }
            ) {
                OutlinedTextField(
                    value = if (uiState.selectedInterests.isEmpty()) {
                        "Select interests"
                    } else {
                        uiState.selectedInterests.take(2).joinToString(", ") +
                                if (uiState.selectedInterests.size > 2) " +${uiState.selectedInterests.size - 2} more" else ""
                    },
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Child interests") },
                    placeholder = { Text("Choose up to 5 interests") },
                    supportingText = {
                        Text("Up to 5 interests.")
                    },
                    trailingIcon = {
                        ExposedDropdownMenuDefaults.TrailingIcon(expanded = interestsExpanded)
                    },
                    modifier = Modifier
                        .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                        .fillMaxWidth(),
                    singleLine = true,
                    maxLines = 1
                )

                // Interest selection menu
                DropdownMenu(
                    expanded = interestsExpanded,
                    onDismissRequest = { interestsExpanded = false },
                    modifier = Modifier
                        .fillMaxWidth(0.72f)
                        .heightIn(max = 280.dp)
                ) {
                    interests.forEach { interest ->
                        val selected = uiState.selectedInterests.contains(interest)
                        val canSelect = selected || uiState.selectedInterests.size < 5

                        DropdownMenuItem(
                            text = {
                                Text(
                                    text = interest,
                                    fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal
                                )
                            },
                            onClick = {
                                if (canSelect) {
                                    onToggleInterest(interest)
                                }
                            },
                            enabled = canSelect,
                            leadingIcon = {
                                Icon(
                                    imageVector = if (selected) Icons.Default.Check else Icons.Default.Add,
                                    contentDescription = null,
                                    tint = if (selected) {
                                        MaterialTheme.colorScheme.primary
                                    } else {
                                        MaterialTheme.colorScheme.onSurfaceVariant
                                    }
                                )
                            }
                        )
                    }
                }
            }

            // Shows the selected interests as chips that can be tapped to remove
            if (uiState.selectedInterests.isNotEmpty()) {
                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "Chosen interests",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Medium
                )

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    uiState.selectedInterests.forEach { interest ->
                        AssistChip(
                            onClick = { onToggleInterest(interest) },
                            label = { Text(interest) }
                        )
                    }
                }
            }

            // Shows loading state while recommendations are being generated
            if (uiState.isLoadingRecommendations) {
                Text(
                    text = "Finding books for the selected interests...",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                CircularProgressIndicator()
            }

            // Shows empty state if no books are found for the current interests
            if (!uiState.isLoadingRecommendations &&
                uiState.selectedInterests.isNotEmpty() &&
                uiState.recommendedBooks.isEmpty()
            ) {
                Text(
                    text = "No matching books found yet. Try selecting different interests.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Displays recommended starter books when available
            if (uiState.recommendedBooks.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Recommended from the library",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    text = "These are pulled from the app's existing library sources based on the selected interests.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Renders each recommended book with metadata and add/remove action
                uiState.recommendedBooks.forEach { book ->
                    val isSelectedForChildLibrary = uiState.selectedRecommendedBookIds.contains(book.id)

                    Card(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box {
                                AsyncImage(
                                    model = book.coverUrl,
                                    contentDescription = book.title,
                                    modifier = Modifier
                                        .size(60.dp)
                                        .background(Color.Gray.copy(alpha = 0.1f), RoundedCornerShape(8.dp)),
                                    contentScale = ContentScale.Crop
                                )
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = book.title,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )

                                Text(
                                    text = "By ${book.author}",
                                    style = MaterialTheme.typography.bodySmall,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )

                                Spacer(modifier = Modifier.height(4.dp))

                                // Displays source and age range labels for the recommended book
                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Surface(
                                        color = MaterialTheme.colorScheme.secondaryContainer,
                                        shape = RoundedCornerShape(4.dp)
                                    ) {
                                        Text(
                                            text = book.source,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                            style = MaterialTheme.typography.labelSmall
                                        )
                                    }

                                    Surface(
                                        color = MaterialTheme.colorScheme.surfaceVariant,
                                        shape = RoundedCornerShape(4.dp)
                                    ) {
                                        Text(
                                            text = "${book.ageMin}-${book.ageMax} yrs",
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                            style = MaterialTheme.typography.labelSmall
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.width(8.dp))

                            // Adds or removes this book from the starter library selection
                            IconButton(
                                onClick = { onToggleRecommendedBook(book) }
                            ) {
                                Icon(
                                    imageVector = if (isSelectedForChildLibrary) Icons.Default.Check else Icons.Default.Add,
                                    contentDescription = if (isSelectedForChildLibrary) {
                                        "Added to child library on registration"
                                    } else {
                                        "Add to child library on registration"
                                    },
                                    tint = if (isSelectedForChildLibrary) {
                                        MaterialTheme.colorScheme.primary
                                    } else {
                                        MaterialTheme.colorScheme.onSurfaceVariant
                                    }
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                }
            }

            // Displays any error message from the setup process
            uiState.errorMessage?.let {
                Text(
                    text = it,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Generates the child invite code using the selected interests and starter books
            Button(
                onClick = onGenerateCode,
                modifier = Modifier.fillMaxWidth(),
                enabled = !uiState.isGenerating
            ) {
                if (uiState.isGenerating) {
                    CircularProgressIndicator(
                        modifier = Modifier.height(18.dp),
                        strokeWidth = 2.dp
                    )
                } else {
                    Text("Generate Invite Code")
                }
            }

            // Shows generated invite code and copy action
            uiState.generatedCode?.let { code ->
                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Invite Code",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    text = code,
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.primary
                )

                OutlinedButton(
                    onClick = { onCopyCode(code) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.ContentCopy, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Copy Code")
                }
            }
        }
    }
}
