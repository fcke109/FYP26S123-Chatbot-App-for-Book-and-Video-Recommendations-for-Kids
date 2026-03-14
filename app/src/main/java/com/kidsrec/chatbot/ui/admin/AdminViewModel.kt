package com.kidsrec.chatbot.ui.admin

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.firestore.FirebaseFirestore
import com.kidsrec.chatbot.data.model.Book
import com.kidsrec.chatbot.data.model.User
import com.kidsrec.chatbot.data.repository.AccountManager
import com.kidsrec.chatbot.data.repository.BookDataManager
import com.kidsrec.chatbot.data.remote.OpenLibraryService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AdminViewModel @Inject constructor(
    private val accountManager: AccountManager,
    private val bookDataManager: BookDataManager,
    private val openLibraryService: OpenLibraryService,
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
        loadCuratedBooks()
    }

    fun startManagingUsers() {
        viewModelScope.launch {
            accountManager.getAllUsersFlow()
                .catch { e -> Log.e("AdminVM", "Permission denied for users list", e) }
                .collect { userList -> _users.value = userList }
        }
    }

    private fun loadCuratedBooks() {
        viewModelScope.launch {
            try {
                firestore.collection("content_books")
                    .addSnapshotListener { snapshot, error ->
                        if (error != null) return@addSnapshotListener
                        if (snapshot != null) {
                            _curatedBooks.value = snapshot.toObjects(Book::class.java).sortedBy { it.id }
                        }
                    }
            } catch (e: Exception) {
                Log.e("AdminVM", "Load failed", e)
            }
        }
    }

    fun seedOfficialLibrary() {
        viewModelScope.launch {
            val stories = listOf(
                Book(
                    id = "001", 
                    title = "The Tale of Peter Rabbit", 
                    author = "Beatrix Potter", 
                    ageMin = 3, 
                    ageMax = 8, 
                    category = "Animals", 
                    source = "ICDL", 
                    language = "English", 
                    description = "Fun rabbit stories.", 
                    tags = listOf("rabbit", "nature"), 
                    isKidSafe = true, 
                    difficulty = "easy", 
                    bookUrl = "https://archive.org/embed/taleofpeterrabbi00pott", 
                    readerUrl = "https://archive.org/embed/taleofpeterrabbi00pott",
                    coverUrl = "https://archive.org/services/img/taleofpeterrabbi00pott"
                ),
                Book(
                    id = "002", 
                    title = "Alice in Wonderland", 
                    author = "Lewis Carroll", 
                    ageMin = 6, 
                    ageMax = 12, 
                    category = "Fantasy", 
                    source = "ICDL", 
                    language = "English", 
                    description = "Magical adventure.", 
                    tags = listOf("magic", "adventure"), 
                    isKidSafe = true, 
                    difficulty = "medium", 
                    bookUrl = "https://archive.org/embed/alicesadventures00carr_0", 
                    readerUrl = "https://archive.org/embed/alicesadventures00carr_0",
                    coverUrl = "https://archive.org/services/img/alicesadventures00carr_0"
                ),
                Book(
                    id = "003", 
                    title = "Cinderella", 
                    author = "Charles Perrault", 
                    ageMin = 3, 
                    ageMax = 10, 
                    category = "Fairy Tales", 
                    source = "ICDL", 
                    language = "English", 
                    description = "Classic magic story.", 
                    tags = listOf("magic", "princess"), 
                    isKidSafe = true, 
                    difficulty = "easy", 
                    bookUrl = "https://archive.org/embed/cinderellaorfair00perr", 
                    readerUrl = "https://archive.org/embed/cinderellaorfair00perr",
                    coverUrl = "https://archive.org/services/img/cinderellaorfair00perr"
                ),
                Book(
                    id = "004", 
                    title = "The Jungle Book", 
                    author = "Rudyard Kipling", 
                    ageMin = 7, 
                    ageMax = 13, 
                    category = "Adventure", 
                    source = "ICDL", 
                    language = "English", 
                    description = "Mowgli in the wild.", 
                    tags = listOf("animals", "jungle"), 
                    isKidSafe = true, 
                    difficulty = "medium", 
                    bookUrl = "https://archive.org/embed/junglebook00kipl", 
                    readerUrl = "https://archive.org/embed/junglebook00kipl",
                    coverUrl = "https://archive.org/services/img/junglebook00kipl"
                ),
                Book(
                    id = "005", 
                    title = "Pinocchio", 
                    author = "Carlo Collodi", 
                    ageMin = 5, 
                    ageMax = 11, 
                    category = "Adventure", 
                    source = "ICDL", 
                    language = "English", 
                    description = "The wooden puppet.", 
                    tags = listOf("magic", "puppet"), 
                    isKidSafe = true, 
                    difficulty = "medium", 
                    bookUrl = "https://archive.org/embed/theadventuresofp00coll", 
                    readerUrl = "https://archive.org/embed/theadventuresofp00coll",
                    coverUrl = "https://archive.org/services/img/theadventuresofp00coll"
                )
            )
            stories.forEach { bookDataManager.addBook(it) }
        }
    }

    fun searchBooks(query: String) {
        if (query.isBlank()) return
        val lowerQuery = query.lowercase().trim()
        viewModelScope.launch {
            _isSearching.value = true
            try {
                val response = openLibraryService.searchBooks("$query subject:\"Children's fiction\" language:eng")
                val results: List<Book> = response.docs.mapNotNull { doc ->
                    val iaId = doc.ia?.firstOrNull() ?: return@mapNotNull null
                    if (doc.cover_i == null) return@mapNotNull null
                    
                    val title = doc.title
                    val author = doc.author_name?.firstOrNull() ?: "Unknown"
                    
                    // Basic Scoring
                    var score = 0
                    if (title.lowercase().contains(lowerQuery)) score += 60
                    if (author.lowercase().contains(lowerQuery)) score += 20
                    if (title.lowercase() == lowerQuery) score += 40
                    
                    // Cap score at 100
                    val finalScore = score.coerceAtMost(100)
                    
                    if (finalScore > 0) {
                        Book(
                            id = iaId, 
                            title = title, 
                            author = author,
                            coverUrl = "https://covers.openlibrary.org/b/id/${doc.cover_i}-L.jpg",
                            bookUrl = "https://archive.org/embed/$iaId",
                            readerUrl = "https://archive.org/embed/$iaId",
                            source = "ICDL/Archive", 
                            ageMin = 3, 
                            ageMax = 12, 
                            isKidSafe = true
                        ).apply { searchScore = finalScore }
                    } else null
                }.sortedByDescending { it.searchScore }

                _searchResults.value = results
            } catch (e: Exception) {
                Log.e("AdminVM", "Search failed", e)
            } finally {
                _isSearching.value = false
            }
        }
    }

    fun addBookToLibrary(book: Book) {
        viewModelScope.launch { 
            val nextNum = _curatedBooks.value.size + 1
            val formattedId = String.format("%03d", nextNum)
            bookDataManager.addBook(book.copy(id = formattedId))
        }
    }

    fun deleteBookFromLibrary(bookId: String) {
        viewModelScope.launch { bookDataManager.deleteBook(bookId) }
    }

    fun logout(onSuccess: () -> Unit) {
        viewModelScope.launch {
            accountManager.signOut()
            onSuccess()
        }
    }
}
