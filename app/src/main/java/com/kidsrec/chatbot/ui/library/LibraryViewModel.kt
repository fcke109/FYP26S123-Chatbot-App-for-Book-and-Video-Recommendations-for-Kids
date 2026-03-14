package com.kidsrec.chatbot.ui.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kidsrec.chatbot.data.model.Book
import com.kidsrec.chatbot.data.model.Recommendation
import com.kidsrec.chatbot.data.remote.OpenLibraryService
import com.kidsrec.chatbot.data.repository.AccountManager
import com.kidsrec.chatbot.data.repository.BookDataManager
import com.kidsrec.chatbot.data.repository.FavoritesManager
import com.kidsrec.chatbot.data.repository.RecommendationEngine
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LibraryViewModel @Inject constructor(
    private val bookDataManager: BookDataManager,
    private val recommendationEngine: RecommendationEngine,
    private val accountManager: AccountManager,
    private val favoritesManager: FavoritesManager,
    private val openLibraryService: OpenLibraryService
) : ViewModel() {

    private val _curatedBooks = MutableStateFlow<List<Book>>(emptyList())
    val curatedBooks: StateFlow<List<Book>> = _curatedBooks.asStateFlow()

    private val _topPicks = MutableStateFlow<List<Recommendation>>(emptyList())
    val topPicks: StateFlow<List<Recommendation>> = _topPicks.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    init {
        loadBooks()
    }

    private fun loadBooks() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val books = bookDataManager.getCuratedBooks().getOrDefault(emptyList())
                if (books.isNotEmpty()) {
                    _curatedBooks.value = books
                    loadTopPicks(books)
                } else {
                    loadFromOpenLibrary()
                }
            } catch (_: Exception) { }
            _isLoading.value = false
        }
    }

    private suspend fun loadFromOpenLibrary() {
        try {
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
            _curatedBooks.value = books
            loadTopPicks(books)
        } catch (_: Exception) { }
    }

    private suspend fun loadTopPicks(books: List<Book>) {
        try {
            val userId = accountManager.getCurrentUserId() ?: return
            val user = accountManager.getUser(userId) ?: return
            val favorites = favoritesManager.getFavorites(userId)
            val picks = recommendationEngine.getTopRecommendations(books, user, favorites, limit = 4)
            _topPicks.value = picks
        } catch (_: Exception) { }
    }
}
