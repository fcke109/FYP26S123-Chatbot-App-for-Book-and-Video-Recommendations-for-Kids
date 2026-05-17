package com.kidsrec.chatbot.ui.library

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kidsrec.chatbot.data.model.Book
import com.kidsrec.chatbot.data.remote.OpenLibraryService
import com.kidsrec.chatbot.data.repository.AccountManager
import com.kidsrec.chatbot.data.repository.AnalyticsRepository
import com.kidsrec.chatbot.data.repository.BookDataManager
import com.kidsrec.chatbot.data.repository.FavoritesManager
import com.kidsrec.chatbot.data.repository.InteractionManager
import com.kidsrec.chatbot.data.repository.RecommendationEngine
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject
import com.kidsrec.chatbot.data.model.Recommendation
import com.kidsrec.chatbot.data.model.RecommendationType

// ViewModel responsible for loading the library, fallback books, and personalised recommendations
@HiltViewModel
class LibraryViewModel @Inject constructor(
    private val bookDataManager: BookDataManager,
    private val recommendationEngine: RecommendationEngine,
    private val accountManager: AccountManager,
    private val favoritesManager: FavoritesManager,
    private val openLibraryService: OpenLibraryService,
    private val analyticsRepository: AnalyticsRepository,
    private val interactionManager: InteractionManager
) : ViewModel() {

    companion object {
        // Tag used for Logcat debugging messages from this ViewModel
        private const val TAG = "LibraryVM"
    }

    // Stores the list of curated or fallback books shown in the library
    private val _curatedBooks = MutableStateFlow<List<Book>>(emptyList())
    val curatedBooks: StateFlow<List<Book>> = _curatedBooks.asStateFlow()

    // Stores personalised recommendations shown as "Top Picks for You"
    private val _topPicks = MutableStateFlow<List<Recommendation>>(emptyList())

    // Tracks previously recommended item IDs to reduce repeated recommendations
    private val previousRecommendationIds = mutableSetOf<String>()
    val topPicks: StateFlow<List<Recommendation>> = _topPicks.asStateFlow()

    // Tracks whether the library is currently loading data
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    // Stores the current user's age, defaulting to 8 if user data is not loaded yet
    private val _userAge = MutableStateFlow(8)
    val userAge: StateFlow<Int> = _userAge.asStateFlow()

    // Loads user age and starts observing library books when the ViewModel is created
    init {
        loadUserAge()
        observeBooks()
    }

    // Loads the current user's age for age-based filtering or display purposes
    private fun loadUserAge() {
        viewModelScope.launch {
            try {
                val userId = accountManager.getCurrentUserId() ?: return@launch
                val user = accountManager.getUser(userId) ?: return@launch
                _userAge.value = user.age
            } catch (e: Exception) {
                Log.e(TAG, "Failed to load user age: ${e.message}", e)
            }
        }
    }

    // Observes curated books from Firestore and falls back to OpenLibrary if needed
    private fun observeBooks() {
        viewModelScope.launch {
            _isLoading.value = true
            Log.d(TAG, "Starting to observe Curated Books...")

            bookDataManager.getCuratedBooksFlow()
                .catch { e ->
                    // If Firestore loading fails, try loading readable children's books from OpenLibrary
                    Log.e(TAG, "FATAL Error observing books: ${e.message}", e)
                    try {
                        loadFromOpenLibrary()
                    } catch (e2: Exception) {
                        Log.e(TAG, "OpenLibrary fallback also failed: ${e2.message}", e2)
                    }
                    _isLoading.value = false
                }
                .collectLatest { books ->
                    Log.d(TAG, "Received ${books.size} books from Firestore")

                    if (books.isNotEmpty()) {
                        // Use curated Firestore books when available
                        _curatedBooks.value = books
                        loadTopPicks(books)
                        _isLoading.value = false
                    } else {
                        // If curated library is empty, load fallback books from OpenLibrary
                        Log.w(TAG, "Curated library is empty. Checking OpenLibrary fallback...")
                        loadFromOpenLibrary()
                        _isLoading.value = false
                    }
                }
        }
    }

    // Loads fallback readable children's books from OpenLibrary
    private suspend fun loadFromOpenLibrary() {
        try {
            Log.d(TAG, "Fetching fallback books from OpenLibrary...")

            val response = openLibraryService.searchBooks("children picture books", limit = 20)
            val books = response.docs
                // Only keep books that have online reading access
                .filter { it.canReadOnline() }
                .take(12)
                .map { olBook ->
                    // Converts OpenLibrary keys into safe local IDs
                    val sanitizedId = olBook.key.removePrefix("/").replace("/", "_")
                    Book(
                        id = sanitizedId,
                        title = olBook.title,
                        author = olBook.getAuthorString(),
                        description = olBook.subject?.take(3)?.joinToString(", ") ?: "",
                        coverUrl = olBook.getCoverUrl("M") ?: "",
                        source = "OpenLibrary",
                        readerUrl = olBook.getReadUrl() ?: olBook.getOpenLibraryUrl(),
                        bookUrl = olBook.getOpenLibraryUrl(),
                        ageMin = 3,
                        ageMax = 12,
                        difficulty = "easy"
                    )
                }

            // Only replace the library list if no curated books are currently loaded
            if (_curatedBooks.value.isEmpty()) {
                _curatedBooks.value = books
                loadTopPicks(books)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load from OpenLibrary: ${e.message}", e)
        }
    }

    // Generates personalised top picks using user profile, favourites, clicks, and curated books
    private suspend fun loadTopPicks(books: List<Book>) {
        try {
            val userId = accountManager.getCurrentUserId() ?: return
            val user = accountManager.getUser(userId) ?: return
            val favorites = favoritesManager.getFavorites(userId)

            val picks = recommendationEngine.getTopRecommendations(
                curatedBooks = books,
                user = user,
                favorites = favorites,
                searchHistory = emptyList(),
                previouslyRecommendedIds = previousRecommendationIds.toList(),
                clickedItems = interactionManager.getClickedItems(),
                limit = 4
            )

            // Updates the UI with the newly ranked recommendation list
            _topPicks.value = picks

            // Stores recommended IDs to help reduce repeated recommendations later
            previousRecommendationIds.addAll(
                picks.map { it.id }
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load top picks: ${e.message}", e)
        }
    }

    // Records a clicked item and refreshes recommendations based on the new interaction
    fun addClickedItem(title: String) {
        interactionManager.addClickedItem(title)
        refreshTopPicks()
    }

    // Clears clicked item history and refreshes recommendations without recent click influence
    fun getClickedItems(): List<String> {
        return interactionManager.getClickedItems()
    }

    // Clears clicked item history and refreshes recommendations without recent click influence
    fun clearClickedItems() {
        interactionManager.clearClickedItems()
        refreshTopPicks()
    }

    // Regenerates top picks using the currently loaded books
    fun refreshTopPicks() {
        viewModelScope.launch {
            val books = _curatedBooks.value
            if (books.isNotEmpty()) {
                loadTopPicks(books)
            }
        }
    }

    // Tracks a book or content view for analytics reporting in the admin dashboard
    fun trackBookView(bookTitle: String, bookId: String = "") {
        viewModelScope.launch {
            try {
                val currentUserId = accountManager.getCurrentUserId() ?: "unknown"
                Log.d(
                    "ANALYTICS_TEST",
                    "trackBookView called -> title=$bookTitle bookId=$bookId userId=$currentUserId"
                )
                analyticsRepository.trackBookView(bookId, bookTitle, currentUserId)
            } catch (e: Exception) {
                Log.e("ANALYTICS_TEST", "trackBookView failed", e)
            }
        }
    }
}