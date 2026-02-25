package com.kidsrec.chatbot.ui.admin

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.firestore.FirebaseFirestore
import com.kidsrec.chatbot.data.model.Book
import com.kidsrec.chatbot.data.model.User
import com.kidsrec.chatbot.data.repository.AccountManager
import com.kidsrec.chatbot.data.repository.BookDataManager
import com.kidsrec.chatbot.data.remote.GutendexService
import com.kidsrec.chatbot.data.remote.OpenLibraryService
import com.kidsrec.chatbot.data.remote.StoryweaverService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

/**
 * AdminViewModel: Re-linked to work with renamed Managers and APIs.
 */
@HiltViewModel
class AdminViewModel @Inject constructor(
    private val accountManager: AccountManager,
    private val bookDataManager: BookDataManager,
    private val gutendexService: GutendexService,
    private val openLibraryService: OpenLibraryService,
    private val storyweaverService: StoryweaverService,
    private val firestore: FirebaseFirestore
) : ViewModel() {

    private val _users = MutableStateFlow<List<User>>(emptyList())
    val users: StateFlow<List<User>> = _users.asStateFlow()

    private val _curatedBooks = MutableStateFlow<List<Book>>(emptyList())
    val curatedBooks: StateFlow<List<Book>> = _curatedBooks.asStateFlow()

    private val _searchResults = MutableStateFlow<List<Book>>(emptyList())
    val searchResults: StateFlow<List<Book>> = _searchResults.asStateFlow()

    private val _isSearching = MutableStateFlow(false)
    val isSearching: StateFlow<Boolean> = _isSearching.asStateFlow()

    init {
        loadUsers()
        loadCuratedBooks()
    }

    private fun loadUsers() {
        viewModelScope.launch {
            accountManager.getAllUsersFlow()
                .catch { e -> Log.e("AdminVM", "User load failed", e) }
                .collect { userList -> _users.value = userList }
        }
    }

    private fun loadCuratedBooks() {
        viewModelScope.launch {
            try {
                firestore.collection("gutenberg_books")
                    .addSnapshotListener { snapshot, _ ->
                        if (snapshot != null) {
                            _curatedBooks.value = snapshot.toObjects(Book::class.java)
                        }
                    }
            } catch (e: Exception) {
                Log.e("AdminVM", "Failed to load books", e)
            }
        }
    }

    fun searchBooks(query: String) {
        if (query.isBlank()) return
        viewModelScope.launch {
            _isSearching.value = true
            _searchResults.value = emptyList()
            try {
                // RUN SEARCHES IN PARALLEL
                val swDeferred = async {
                    try {
                        // Storyweaver Search
                        val response = storyweaverService.searchStories(query)
                        response.stories.map { story ->
                            Book(
                                id = "sw_${story.id}",
                                title = story.title,
                                author = story.authors.firstOrNull()?.name ?: "Various Authors",
                                coverUrl = story.image_url,
                                readerUrl = "https://storyweaver.org.in/stories/${story.slug}/read?mode=read",
                                isPictureBook = true,
                                readingAvailability = "Easy",
                                ageRating = "0-10 years",
                                description = story.synopsis ?: "Illustrated story book."
                            )
                        }
                    } catch (e: Exception) {
                        Log.e("AdminVM", "SW Error: ${e.message}")
                        emptyList<Book>()
                    }
                }

                val olDeferred = async {
                    try {
                        // Open Library Search
                        val response = openLibraryService.searchBooks("$query children picture books")
                        response.docs.mapNotNull { doc ->
                            val archiveId = doc.ia?.firstOrNull() ?: return@mapNotNull null
                            if (doc.cover_i == null) return@mapNotNull null
                            Book(
                                id = archiveId,
                                title = doc.title ?: "Story Book",
                                author = doc.author_name?.firstOrNull() ?: "Unknown Author",
                                coverUrl = "https://covers.openlibrary.org/b/id/${doc.cover_i}-L.jpg",
                                readerUrl = "https://archive.org/embed/$archiveId",
                                isPictureBook = true,
                                readingAvailability = "Intermediate",
                                ageRating = "6-12 years",
                                description = "Illustrated classic story."
                            )
                        }
                    } catch (e: Exception) {
                        Log.e("AdminVM", "OL Error: ${e.message}")
                        emptyList<Book>()
                    }
                }

                _searchResults.value = swDeferred.await() + olDeferred.await()
            } catch (e: Exception) {
                Log.e("AdminVM", "Global Search Error: ${e.message}")
            } finally {
                _isSearching.value = false
            }
        }
    }

    fun addBookToLibrary(book: Book) {
        viewModelScope.launch {
            try {
                firestore.collection("gutenberg_books").document(book.id).set(book).await()
            } catch (e: Exception) {
                Log.e("AdminVM", "Add failed", e)
            }
        }
    }

    fun deleteBookFromLibrary(bookId: String) {
        viewModelScope.launch {
            try {
                firestore.collection("gutenberg_books").document(bookId).delete().await()
            } catch (e: Exception) {
                Log.e("AdminVM", "Delete failed", e)
            }
        }
    }

    fun logout(onSuccess: () -> Unit) {
        viewModelScope.launch {
            accountManager.signOut()
            onSuccess()
        }
    }
}
