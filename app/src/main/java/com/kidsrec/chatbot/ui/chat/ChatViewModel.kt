package com.kidsrec.chatbot.ui.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kidsrec.chatbot.data.model.ChatMessage
import com.kidsrec.chatbot.data.repository.AccountManager
import com.kidsrec.chatbot.data.repository.BookDataManager
import com.kidsrec.chatbot.data.repository.ChatDataManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ChatViewModel: Manages the logic for the DinoChatPage.
 */
@HiltViewModel
class ChatViewModel @Inject constructor(
    private val chatDataManager: ChatDataManager,
    private val accountManager: AccountManager,
    private val bookDataManager: BookDataManager
) : ViewModel() {

    private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val messages: StateFlow<List<ChatMessage>> = _messages.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private var currentConversationId: String? = null

    init {
        initializeConversation()
    }

    private fun initializeConversation() {
        viewModelScope.launch {
            val userId = accountManager.getCurrentUserId() ?: return@launch
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

    private fun loadMessages(userId: String, conversationId: String) {
        viewModelScope.launch {
            chatDataManager.getMessagesFlow(userId, conversationId).collect { messages ->
                _messages.value = messages
            }
        }
    }

    fun sendMessage(message: String) {
        if (message.isBlank()) return
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
                    _error.value = error.message
                }
            )
        }
    }

    /**
     * Get the best interactive reader link for a book title.
     */
    suspend fun getBookPreviewUrl(title: String): String {
        return try {
            val curatedBooks = bookDataManager.getCuratedBooks().getOrNull()
            val matchingBook = curatedBooks?.firstOrNull { it.title.contains(title, ignoreCase = true) }
            
            if (matchingBook?.readerUrl != null) {
                return matchingBook.readerUrl!!
            }

            val encodedTitle = java.net.URLEncoder.encode(title, "UTF-8")
            "https://archive.org/details/texts?query=$encodedTitle+children+picture+books"
        } catch (e: Exception) {
            "https://archive.org/details/texts?query=$title"
        }
    }
}
