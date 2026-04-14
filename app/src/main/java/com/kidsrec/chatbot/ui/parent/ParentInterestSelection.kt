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

data class ParentInviteSetupUiState(
    val selectedInterests: Set<String> = emptySet(),
    val isGenerating: Boolean = false,
    val generatedCode: String? = null,
    val errorMessage: String? = null,
    val recommendedBooks: List<Book> = emptyList(),
    val isLoadingRecommendations: Boolean = false,
    val selectedRecommendedBookIds: Set<String> = emptySet()
)

@HiltViewModel
class ParentInviteSetupViewModel @Inject constructor(
    private val accountManager: AccountManager,
    private val bookDataManager: BookDataManager,
    private val openLibraryService: OpenLibraryService
) : ViewModel() {

    private val _uiState = MutableStateFlow(ParentInviteSetupUiState())
    val uiState: StateFlow<ParentInviteSetupUiState> = _uiState.asStateFlow()

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

        fetchRecommendedBooks(updated)
    }

    fun clearGeneratedCode() {
        _uiState.value = _uiState.value.copy(generatedCode = null)
    }

    fun generateInviteCode(
        parentId: String,
        parentName: String
    ) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isGenerating = true,
                errorMessage = null
            )

            val selectedStarterBooks = _uiState.value.recommendedBooks
                .filter { it.id in _uiState.value.selectedRecommendedBookIds }
                .map { it.toStarterBookSeed() }

            val result = accountManager.generateInviteCode(
                parentId = parentId,
                parentName = parentName,
                childInterests = _uiState.value.selectedInterests.toList(),
                starterBooks = selectedStarterBooks
            )

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

    private fun fetchRecommendedBooks(selectedInterests: Set<String>) {
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
                val selectedTerms = selectedInterests.flatMap { interest ->
                    listOf(interest) + interestAliases[interest].orEmpty()
                }.map { it.lowercase() }.distinct()

                val curatedBooks = bookDataManager.getCuratedBooksFlow().first()

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

                val needed = (6 - curatedMatches.size).coerceAtLeast(0)

                val remoteMatches = if (needed > 0) {
                    selectedInterests
                        .flatMap { interest ->
                            try {
                                val response = openLibraryService.searchBooks(
                                    "$interest subject:\"Children's fiction\" language:eng"
                                )

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

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ParentInviteSetupRoute(
    parentId: String,
    parentName: String,
    onBack: () -> Unit,
    viewModel: ParentInviteSetupViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val clipboardManager = LocalClipboardManager.current

    LaunchedEffect(uiState.generatedCode) {
        if (uiState.generatedCode != null) {
            snackbarHostState.showSnackbar("Invite code generated successfully.")
        }
    }

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
    val interests = listOf(
        "Reading", "Science", "Animals", "Adventure",
        "Fantasy", "Art", "Music", "Sports", "History", "Nature",
        "Space", "Dinosaurs", "Cooking", "Cars", "Robots",
        "Fairy Tales", "Superheroes", "Ocean", "Puzzles", "Travel"
    )

    Scaffold(
        topBar = {
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

            Text(
                text = "Selected: ${uiState.selectedInterests.size}/5",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )

            var interestsExpanded by remember { mutableStateOf(false) }

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

            if (uiState.isLoadingRecommendations) {
                Text(
                    text = "Finding books for the selected interests...",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                CircularProgressIndicator()
            }

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

            uiState.errorMessage?.let {
                Text(
                    text = it,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

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
