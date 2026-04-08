package com.kidsrec.chatbot.ui.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kidsrec.chatbot.data.model.ChatMessage
import com.kidsrec.chatbot.data.model.Conversation
import com.kidsrec.chatbot.data.remote.OpenLibraryService
import com.kidsrec.chatbot.data.repository.AccountManager
import com.kidsrec.chatbot.data.repository.AnalyticsRepository
import com.kidsrec.chatbot.data.repository.BookDataManager
import com.kidsrec.chatbot.data.repository.ChatDataManager
import dagger.hilt.android.lifecycle.HiltViewModel
import android.util.Log
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ChatViewModel: Manages the logic for the DinoChatPage.
 */
@HiltViewModel
class ChatViewModel @Inject constructor(
    private val chatDataManager: ChatDataManager,
    private val accountManager: AccountManager,
    private val bookDataManager: BookDataManager,
    private val openLibraryService: OpenLibraryService,
    private val analyticsRepository: AnalyticsRepository
) : ViewModel() {

    private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val messages: StateFlow<List<ChatMessage>> = _messages.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val _conversations = MutableStateFlow<List<Conversation>>(emptyList())
    val conversations: StateFlow<List<Conversation>> = _conversations.asStateFlow()

    private var currentConversationId: String? = null
    private var messagesJob: Job? = null

    init {
        initializeConversation()
        loadConversationsList()
    }

    private fun initializeConversation() {
        viewModelScope.launch {
            val userId = accountManager.getCurrentUserId() ?: return@launch
            // Resume the most recent conversation if one exists
            val latestResult = chatDataManager.getLatestConversation(userId)
            val latestConversation = latestResult.getOrNull()
            if (latestConversation != null && latestConversation.id.isNotBlank()) {
                currentConversationId = latestConversation.id
                loadMessages(userId, latestConversation.id)
            } else {
                val result = chatDataManager.createConversation(userId)
                result.fold(
                    onSuccess = { conversationId ->
                        currentConversationId = conversationId
                        loadMessages(userId, conversationId)
                    },
                    onFailure = { error ->
                        _error.value = error.message
                    }
                )
            }
        }
    }

    private fun loadConversationsList() {
        viewModelScope.launch {
            val userId = accountManager.getCurrentUserId() ?: return@launch
            chatDataManager.getConversationsFlow(userId)
                .catch { e -> Log.e("ChatVM", "Failed to load conversations", e) }
                .collect { convos ->
                    // Only show conversations that have at least one message (non-empty preview)
                    _conversations.value = convos.filter { it.preview.isNotBlank() }
                }
        }
    }

    private fun loadMessages(userId: String, conversationId: String) {
        // Cancel previous message listener
        messagesJob?.cancel()
        messagesJob = viewModelScope.launch {
            chatDataManager.getMessagesFlow(userId, conversationId)
                .catch { e -> Log.e("ChatVM", "Failed to load messages", e) }
                .collect { messages ->
                    _messages.value = messages
                }
        }
    }

    fun loadConversation(conversationId: String) {
        viewModelScope.launch {
            val userId = accountManager.getCurrentUserId() ?: return@launch
            currentConversationId = conversationId
            loadMessages(userId, conversationId)
        }
    }

    fun startNewConversation() {
        viewModelScope.launch {
            val userId = accountManager.getCurrentUserId() ?: return@launch
            _messages.value = emptyList()
            val result = chatDataManager.createConversation(userId)
            result.fold(
                onSuccess = { conversationId ->
                    currentConversationId = conversationId
                    loadMessages(userId, conversationId)
                },
                onFailure = { error ->
                    _error.value = error.message
                }
            )
        }
    }

    fun sendMessage(message: String) {
        if (message.isBlank()) return
        // Defense-in-depth: block inappropriate messages at ViewModel level too
        val validation = com.kidsrec.chatbot.util.InputSanitizer.validateMessage(message)
        if (validation != null) {
            _error.value = validation
            return
        }
        viewModelScope.launch {
            val userId = accountManager.getCurrentUserId() ?: return@launch
            val conversationId = currentConversationId ?: return@launch
            _isLoading.value = true
            _error.value = null
            val result = chatDataManager.sendMessage(userId, conversationId, message)
            result.fold(
                onSuccess = { _isLoading.value = false },
                onFailure = { error ->
                    _isLoading.value = false
                    Log.e("ChatVM", "sendMessage failed", error)
                    _error.value = error.message ?: "Something went wrong. Please try again!"
                }
            )
        }
    }

    /**
     * Get the best interactive reader link for a book title.
     * Searches curated books first, then Open Library for a direct reader.
     */
    suspend fun getBookPreviewUrl(title: String): String {
        return try {
            val curatedBooks = bookDataManager.getCuratedBooks().getOrNull()
            val matchingBook = curatedBooks?.firstOrNull { book ->
                book.title.contains(title, ignoreCase = true) ||
                    title.contains(book.title, ignoreCase = true)
            }

            if (matchingBook != null) {
                val url = matchingBook.readerUrl.ifBlank { matchingBook.bookUrl }
                if (url.isNotBlank()) return url
            }

            // Search Open Library for a direct reader link
            val response = openLibraryService.searchBooks("$title children", limit = 5)
            val readable = response.docs.firstOrNull { it.canReadOnline() }
            if (readable != null) {
                val readUrl = readable.getReadUrl()
                if (readUrl != null) return readUrl
            }

            // Fallback: Open Library search filtered to readable books
            val encodedTitle = java.net.URLEncoder.encode(title, "UTF-8")
            "https://openlibrary.org/search?q=$encodedTitle&mode=ebooks&has_fulltext=true"
        } catch (e: Exception) {
            val encodedTitle = java.net.URLEncoder.encode(title, "UTF-8")
            "https://openlibrary.org/search?q=$encodedTitle&mode=ebooks"
        }
    }

    fun trackBookView(bookTitle: String, bookId: String = "") {
        viewModelScope.launch {
            val currentUserId = accountManager.getCurrentUserId() ?: "unknown"
            analyticsRepository.trackBookView(bookId, bookTitle, currentUserId)
        }
    }
}
