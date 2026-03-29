package com.kidsrec.chatbot.ui.admin

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.functions.FirebaseFunctions
import com.kidsrec.chatbot.data.model.Book
import com.kidsrec.chatbot.data.model.User
import com.kidsrec.chatbot.data.repository.AccountManager
import com.kidsrec.chatbot.data.repository.BookDataManager
import com.kidsrec.chatbot.data.remote.OpenLibraryService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

@HiltViewModel
class AdminViewModel @Inject constructor(
    private val accountManager: AccountManager,
    private val bookDataManager: BookDataManager,
    private val openLibraryService: OpenLibraryService,
    private val firestore: FirebaseFirestore,
    private val functions: FirebaseFunctions
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
            bookDataManager.getCuratedBooksFlow()
                .catch { e -> Log.e("AdminVM", "Load failed", e) }
                .collect { books -> _curatedBooks.value = books }
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
            // The DataManager will now automatically pick the next sequential ID
            bookDataManager.addBook(book.copy(id = "")) 
        }
    }

    /**
     * Re-assigns numeric IDs to all books in the library to clean them up.
     */
    fun cleanLibraryIds() {
        viewModelScope.launch {
            val currentBooks = _curatedBooks.value.toList()
            currentBooks.forEachIndexed { index, book ->
                val newId = String.format("%03d", index + 1)
                if (book.id != newId) {
                    // Delete the old record
                    bookDataManager.deleteBook(book.id)
                    // Save it with the new numeric ID
                    bookDataManager.addBook(book.copy(id = newId))
                }
            }
        }
    }

    fun deleteBookFromLibrary(bookId: String) {
        viewModelScope.launch { bookDataManager.deleteBook(bookId) }
    }

    fun deleteUser(userId: String) {
        viewModelScope.launch {
            try {
                // Delete user's Firestore document
                firestore.collection("users").document(userId).delete().await()

                // Delete user's favorites
                val favItems = firestore.collection("favorites")
                    .document(userId).collection("items").get().await()
                for (doc in favItems.documents) {
                    doc.reference.delete().await()
                }

                // Delete user's reading history
                val sessions = firestore.collection("readingHistory")
                    .document(userId).collection("sessions").get().await()
                for (doc in sessions.documents) {
                    doc.reference.delete().await()
                }

                // Delete user's chat history conversations and messages
                val convos = firestore.collection("chatHistory")
                    .document(userId).collection("conversations").get().await()
                for (convo in convos.documents) {
                    val messages = convo.reference.collection("messages").get().await()
                    for (msg in messages.documents) {
                        msg.reference.delete().await()
                    }
                    convo.reference.delete().await()
                }

                // Also delete the Firebase Auth account via Cloud Function
                try {
                    functions.getHttpsCallable("deleteAuthUser")
                        .call(hashMapOf("uid" to userId))
                        .await()
                    Log.d("AdminVM", "Auth account $userId also deleted")
                } catch (authErr: Exception) {
                    Log.w("AdminVM", "Firestore deleted but Auth cleanup failed (deploy functions first): ${authErr.message}")
                }

                Log.d("AdminVM", "User $userId deleted from Firestore")
            } catch (e: Exception) {
                Log.e("AdminVM", "Failed to delete user $userId", e)
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
