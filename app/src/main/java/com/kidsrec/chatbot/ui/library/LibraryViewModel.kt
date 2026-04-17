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
        private const val TAG = "LibraryVM"
    }

    private val _curatedBooks = MutableStateFlow<List<Book>>(emptyList())
    val curatedBooks: StateFlow<List<Book>> = _curatedBooks.asStateFlow()

    private val _topPicks = MutableStateFlow<List<Recommendation>>(emptyList())
    val topPicks: StateFlow<List<Recommendation>> = _topPicks.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _userAge = MutableStateFlow(8)
    val userAge: StateFlow<Int> = _userAge.asStateFlow()

    init {
        loadUserAge()
        observeBooks()
    }

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

    private fun observeBooks() {
        viewModelScope.launch {
            _isLoading.value = true
            Log.d(TAG, "Starting to observe Curated Books...")

            bookDataManager.getCuratedBooksFlow()
                .catch { e ->
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
                        _curatedBooks.value = books
                        loadTopPicks(books)
                        _isLoading.value = false
                    } else {
                        Log.w(TAG, "Curated library is empty. Checking OpenLibrary fallback...")
                        loadFromOpenLibrary()
                        _isLoading.value = false
                    }
                }
        }
    }

    private suspend fun loadFromOpenLibrary() {
        try {
            Log.d(TAG, "Fetching fallback books from OpenLibrary...")

            val response = openLibraryService.searchBooks("children picture books", limit = 20)
            val books = response.docs
                .filter { it.canReadOnline() }
                .take(12)
                .map { olBook ->
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

            if (_curatedBooks.value.isEmpty()) {
                _curatedBooks.value = books
                loadTopPicks(books)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load from OpenLibrary: ${e.message}", e)
        }
    }

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
                clickedItems = interactionManager.getClickedItems(),
                limit = 4
            )

            _topPicks.value = picks
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load top picks: ${e.message}", e)
        }
    }

    fun addClickedItem(title: String) {
        interactionManager.addClickedItem(title)
        refreshTopPicks()
    }

    fun getClickedItems(): List<String> {
        return interactionManager.getClickedItems()
    }

    fun clearClickedItems() {
        interactionManager.clearClickedItems()
        refreshTopPicks()
    }

    fun refreshTopPicks() {
        viewModelScope.launch {
            val books = _curatedBooks.value
            if (books.isNotEmpty()) {
                loadTopPicks(books)
            }
        }
    }

    fun trackBookView(bookTitle: String, bookId: String = "") {
        viewModelScope.launch {
            val currentUserId = accountManager.getCurrentUserId() ?: "unknown"
            analyticsRepository.trackBookView(bookId, bookTitle, currentUserId)
        }
    }
}