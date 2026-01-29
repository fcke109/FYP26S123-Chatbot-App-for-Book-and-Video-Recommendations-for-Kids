package com.kidsrec.chatbot.ui.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kidsrec.chatbot.data.model.ChatMessage
import com.kidsrec.chatbot.data.repository.AuthRepository
import com.kidsrec.chatbot.data.repository.ChatRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ChatViewModel @Inject constructor(
    private val chatRepository: ChatRepository,
    private val authRepository: AuthRepository
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
            val userId = authRepository.getCurrentUserId() ?: return@launch

            // Create a new conversation
            val result = chatRepository.createConversation(userId)
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
            chatRepository.getMessagesFlow(userId, conversationId).collect { messages ->
                _messages.value = messages
            }
        }
    }

    fun sendMessage(message: String) {
        if (message.isBlank()) return

        viewModelScope.launch {
            val userId = authRepository.getCurrentUserId() ?: return@launch
            val conversationId = currentConversationId ?: return@launch

            _isLoading.value = true
            _error.value = null

            val result = chatRepository.sendMessage(userId, conversationId, message)
            result.fold(
                onSuccess = {
                    _isLoading.value = false
                },
                onFailure = { error ->
                    _isLoading.value = false
                    _error.value = error.message
                }
            )
        }
    }

    fun clearError() {
        _error.value = null
    }
}
