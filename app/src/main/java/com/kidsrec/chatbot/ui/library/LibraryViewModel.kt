package com.kidsrec.chatbot.ui.library

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kidsrec.chatbot.data.model.Book
import com.kidsrec.chatbot.data.model.Recommendation
import com.kidsrec.chatbot.data.remote.OpenLibraryService
import com.kidsrec.chatbot.data.repository.AccountManager
import com.kidsrec.chatbot.data.repository.AnalyticsRepository
import com.kidsrec.chatbot.data.repository.BookDataManager
import com.kidsrec.chatbot.data.repository.FavoritesManager
import com.kidsrec.chatbot.data.repository.RecommendationEngine
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LibraryViewModel @Inject constructor(
    private val bookDataManager: BookDataManager,
    private val recommendationEngine: RecommendationEngine,
    private val accountManager: AccountManager,
    private val favoritesManager: FavoritesManager,
    private val openLibraryService: OpenLibraryService,
    private val analyticsRepository: AnalyticsRepository
) : ViewModel() {

    private val _curatedBooks = MutableStateFlow<List<Book>>(emptyList())
    val curatedBooks: StateFlow<List<Book>> = _curatedBooks.asStateFlow()

    private val _topPicks = MutableStateFlow<List<Recommendation>>(emptyList())
    val topPicks: StateFlow<List<Recommendation>> = _topPicks.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    init {
        observeBooks()
    }

    private fun observeBooks() {
        viewModelScope.launch {
            _isLoading.value = true
            Log.d("LibraryVM", "Starting to observe Curated Books...")
            
            bookDataManager.getCuratedBooksFlow()
                .catch { e ->
                    Log.e("LibraryVM", "FATAL Error observing books: ${e.message}")
                    try {
                        loadFromOpenLibrary()
                    } catch (e2: Exception) {
                        Log.e("LibraryVM", "OpenLibrary fallback also failed: ${e2.message}")
                    }
                    _isLoading.value = false
                }
                .collectLatest { books ->
                    Log.d("LibraryVM", "Received ${books.size} books from Firestore")
                    
                    if (books.isNotEmpty()) {
                        // We found admin books! Show them.
                        _curatedBooks.value = books
                        loadTopPicks(books)
                        _isLoading.value = false
                    } else {
                        // Library is empty or blocked by rules.
                        Log.w("LibraryVM", "Curated library is empty. Checking OpenLibrary fallback...")
                        loadFromOpenLibrary()
                        _isLoading.value = false
                    }
                }
        }
    }

    private suspend fun loadFromOpenLibrary() {
        try {
            Log.d("LibraryVM", "Fetching fallback books from OpenLibrary...")
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
            
            // Only update if we don't have curated books already
            if (_curatedBooks.value.isEmpty()) {
                _curatedBooks.value = books
                loadTopPicks(books)
            }
        } catch (e: Exception) {
            Log.e("LibraryVM", "Failed to load from OpenLibrary: ${e.message}")
        }
    }

    private suspend fun loadTopPicks(books: List<Book>) {
        try {
            val userId = accountManager.getCurrentUserId() ?: return
            val user = accountManager.getUser(userId) ?: return
            val favorites = favoritesManager.getFavorites(userId)
            val picks = recommendationEngine.getTopRecommendations(books, user, favorites, limit = 4)
            _topPicks.value = picks
        } catch (e: Exception) {
            Log.e("LibraryVM", "Failed to load top picks: ${e.message}")
        }
    }

    fun trackBookView(bookTitle: String, bookId: String = "") {
        viewModelScope.launch {
            val currentUserId = accountManager.getCurrentUserId() ?: "unknown"
            analyticsRepository.trackBookView(bookId, bookTitle, currentUserId)
        }
    }
}
