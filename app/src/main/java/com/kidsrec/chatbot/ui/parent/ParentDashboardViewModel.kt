package com.kidsrec.chatbot.ui.parent

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import android.util.Log
import com.kidsrec.chatbot.data.model.ChatMessage
import com.kidsrec.chatbot.data.model.ContentApproval
import com.kidsrec.chatbot.data.model.Conversation
import com.kidsrec.chatbot.data.model.Favorite
import com.kidsrec.chatbot.data.model.ReadingHistory
import com.kidsrec.chatbot.data.model.ScreenTimeSession
import com.kidsrec.chatbot.data.model.User
import com.kidsrec.chatbot.data.repository.AccountManager
import com.kidsrec.chatbot.data.repository.ChatDataManager
import com.kidsrec.chatbot.data.repository.ContentApprovalManager
import com.kidsrec.chatbot.data.repository.ScreenTimeManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

@HiltViewModel
class ParentDashboardViewModel @Inject constructor(
    private val accountManager: AccountManager,
    private val screenTimeManager: ScreenTimeManager,
    private val contentApprovalManager: ContentApprovalManager,
    private val chatDataManager: ChatDataManager,
    private val firestore: com.google.firebase.firestore.FirebaseFirestore
) : ViewModel() {

    private suspend fun isMyChild(childId: String): Boolean {
        val parentId = accountManager.getCurrentUserId() ?: return false
        val parent = accountManager.getUser(parentId) ?: return false
        return childId in parent.childIds
    }

    private val _children = MutableStateFlow<List<User>>(emptyList())
    val children: StateFlow<List<User>> = _children.asStateFlow()

    private val _selectedChild = MutableStateFlow<User?>(null)
    val selectedChild: StateFlow<User?> = _selectedChild.asStateFlow()

    private val _childFavorites = MutableStateFlow<List<Favorite>>(emptyList())
    val childFavorites: StateFlow<List<Favorite>> = _childFavorites.asStateFlow()

    private val _childHistory = MutableStateFlow<List<ReadingHistory>>(emptyList())
    val childHistory: StateFlow<List<ReadingHistory>> = _childHistory.asStateFlow()

    private val _childScreenTime = MutableStateFlow<ScreenTimeSession?>(null)
    val childScreenTime: StateFlow<ScreenTimeSession?> = _childScreenTime.asStateFlow()

    private val _weeklyScreenTime = MutableStateFlow<List<ScreenTimeSession>>(emptyList())
    val weeklyScreenTime: StateFlow<List<ScreenTimeSession>> = _weeklyScreenTime.asStateFlow()

    private val _childConversations = MutableStateFlow<List<Conversation>>(emptyList())
    val childConversations: StateFlow<List<Conversation>> = _childConversations.asStateFlow()

    private val _childMessages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val childMessages: StateFlow<List<ChatMessage>> = _childMessages.asStateFlow()

    private val _selectedConversationId = MutableStateFlow<String?>(null)
    val selectedConversationId: StateFlow<String?> = _selectedConversationId.asStateFlow()

    private val _pendingApprovals = MutableStateFlow<List<ContentApproval>>(emptyList())
    val pendingApprovals: StateFlow<List<ContentApproval>> = _pendingApprovals.asStateFlow()

    private val _inviteCode = MutableStateFlow<String?>(null)
    val inviteCode: StateFlow<String?> = _inviteCode.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    init {
        loadChildren()
        loadPendingApprovals()
    }

    private fun loadChildren() {
        val parentId = accountManager.getCurrentUserId() ?: return
        viewModelScope.launch {
            accountManager.getUserFlow(parentId).collect { parent ->
                if (parent != null && parent.childIds.isNotEmpty()) {
                    val childList = parent.childIds.mapNotNull { childId ->
                        try {
                            accountManager.getUser(childId)
                        } catch (e: Exception) {
                            Log.e("ParentDashVM", "Failed to fetch child $childId", e)
                            null
                        }
                    }
                    _children.value = childList
                } else {
                    _children.value = emptyList()
                }
            }
        }
    }

    private fun loadPendingApprovals() {
        val parentId = accountManager.getCurrentUserId() ?: return
        viewModelScope.launch {
            contentApprovalManager.getPendingApprovalsFlow(parentId).collect { approvals ->
                _pendingApprovals.value = approvals
            }
        }
    }

    fun selectChild(child: User) {
        _selectedChild.value = child
        loadChildData(child.id)
    }

    fun clearSelectedChild() {
        _selectedChild.value = null
        _childFavorites.value = emptyList()
        _childHistory.value = emptyList()
        _childConversations.value = emptyList()
        _childMessages.value = emptyList()
        _selectedConversationId.value = null
        _childScreenTime.value = null
        _weeklyScreenTime.value = emptyList()
    }

    private fun loadChildData(childId: String) {
        viewModelScope.launch {
            launch {
                accountManager.getChildFavoritesFlow(childId).collect { favorites ->
                    _childFavorites.value = favorites
                }
            }
            launch {
                accountManager.getChildReadingHistoryFlow(childId).collect { history ->
                    _childHistory.value = history
                }
            }
            launch {
                screenTimeManager.getTodayUsageFlow(childId).collect { session ->
                    _childScreenTime.value = session
                }
            }
            launch {
                screenTimeManager.getWeeklyUsageFlow(childId).collect { sessions ->
                    _weeklyScreenTime.value = sessions
                }
            }
            launch {
                chatDataManager.getConversationsFlow(childId)
                    .catch { e -> Log.e("ParentDashVM", "Failed to load child conversations", e) }
                    .collect { conversations ->
                        _childConversations.value = conversations.filter { it.preview.isNotBlank() }
                    }
            }
        }
    }

    fun selectConversation(childId: String, conversationId: String) {
        _selectedConversationId.value = conversationId
        viewModelScope.launch {
            chatDataManager.getMessagesFlow(childId, conversationId)
                .catch { e -> Log.e("ParentDashVM", "Failed to load messages", e) }
                .collect { messages ->
                    _childMessages.value = messages
                }
        }
    }

    fun clearConversation() {
        _selectedConversationId.value = null
        _childMessages.value = emptyList()
    }

    fun generateInviteCode() {
        val parentId = accountManager.getCurrentUserId() ?: return
        viewModelScope.launch {
            _isLoading.value = true
            val parent = accountManager.getUser(parentId)
            val parentName = parent?.name ?: "Parent"

            val result = accountManager.generateInviteCode(parentId, parentName)
            result.fold(
                onSuccess = { code ->
                    _inviteCode.value = code
                    _errorMessage.value = null
                },
                onFailure = { error ->
                    _errorMessage.value = error.message
                }
            )
            _isLoading.value = false
        }
    }

    fun updateChildFilters(childId: String, maxAgeRating: Int, allowVideos: Boolean) {
        viewModelScope.launch {
            if (!isMyChild(childId)) {
                _errorMessage.value = "Unauthorized action."
                return@launch
            }
            accountManager.updateChildFilters(childId, maxAgeRating, allowVideos)
            val updated = accountManager.getUser(childId)
            if (updated != null) {
                _selectedChild.value = updated
            }
        }
    }

    fun updateBlockedTopics(childId: String, topics: List<String>) {
        viewModelScope.launch {
            if (!isMyChild(childId)) {
                _errorMessage.value = "Unauthorized action."
                return@launch
            }
            try {
                firestore.collection("users")
                    .document(childId)
                    .update("contentFilters.blockedTopics", topics)
                    .await()
                val updated = accountManager.getUser(childId)
                if (updated != null) {
                    _selectedChild.value = updated
                }
            } catch (e: Exception) {
                Log.e("ParentDashVM", "Failed to update blocked topics", e)
                _errorMessage.value = "Failed to update blocked topics."
            }
        }
    }

    fun toggleContentApproval(childId: String, required: Boolean) {
        viewModelScope.launch {
            if (!isMyChild(childId)) {
                _errorMessage.value = "Unauthorized action."
                return@launch
            }
            try {
                firestore.collection("users")
                    .document(childId)
                    .update("contentApprovalRequired", required)
                    .await()
                val updated = accountManager.getUser(childId)
                if (updated != null) {
                    _selectedChild.value = updated
                }
            } catch (e: Exception) {
                Log.e("ParentDashVM", "Failed to toggle content approval", e)
            }
        }
    }

    fun updateScreenTimeLimit(childId: String, minutes: Int) {
        viewModelScope.launch {
            if (!isMyChild(childId)) {
                _errorMessage.value = "Unauthorized action."
                return@launch
            }
            try {
                firestore.collection("users")
                    .document(childId)
                    .update("screenTimeConfig.dailyLimitMinutes", minutes)
                    .await()
                val updated = accountManager.getUser(childId)
                if (updated != null) {
                    _selectedChild.value = updated
                }
            } catch (e: Exception) {
                Log.e("ParentDashVM", "Failed to update screen time limit", e)
            }
        }
    }

    fun grantScreenTimeExtension(childId: String, additionalMinutes: Int = 15) {
        viewModelScope.launch {
            if (!isMyChild(childId)) return@launch
            screenTimeManager.grantExtension(childId, additionalMinutes)
        }
    }

    fun approveContent(approvalId: String) {
        viewModelScope.launch {
            contentApprovalManager.approveContent(approvalId)
        }
    }

    fun rejectContent(approvalId: String) {
        viewModelScope.launch {
            contentApprovalManager.rejectContent(approvalId)
        }
    }

    fun dismissError() {
        _errorMessage.value = null
    }

    fun dismissInviteCode() {
        _inviteCode.value = null
    }
}
