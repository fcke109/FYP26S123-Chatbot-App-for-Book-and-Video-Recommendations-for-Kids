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
// viewmodel for parent dashboard
class ParentDashboardViewModel @Inject constructor(
    private val accountManager: AccountManager,
    private val screenTimeManager: ScreenTimeManager,
    private val contentApprovalManager: ContentApprovalManager,
    private val chatDataManager: ChatDataManager,
    private val firestore: com.google.firebase.firestore.FirebaseFirestore
) : ViewModel() {

    // check parent owns this child (via parent.childIds OR child.parentId,
    // since the new-child redeem flow can't write parent.childIds under the
    // current Firestore rules)
    private suspend fun isMyChild(childId: String): Boolean {
        val parentId = accountManager.getCurrentUserId() ?: return false
        val parent = accountManager.getUser(parentId) ?: return false
        if (childId in parent.childIds) return true
        val child = accountManager.getUser(childId) ?: return false
        return child.parentId == parentId
    }

    // linked children
    private val _children = MutableStateFlow<List<User>>(emptyList())
    val children: StateFlow<List<User>> = _children.asStateFlow()

    // selected child
    private val _selectedChild = MutableStateFlow<User?>(null)
    val selectedChild: StateFlow<User?> = _selectedChild.asStateFlow()

    // Stores the selected child's favourite books/videos
    private val _childFavorites = MutableStateFlow<List<Favorite>>(emptyList())
    val childFavorites: StateFlow<List<Favorite>> = _childFavorites.asStateFlow()

    // Stores the selected child's reading and viewing history
    private val _childHistory = MutableStateFlow<List<ReadingHistory>>(emptyList())
    val childHistory: StateFlow<List<ReadingHistory>> = _childHistory.asStateFlow()

    // Stores the selected child's screen time usage for today
    private val _childScreenTime = MutableStateFlow<ScreenTimeSession?>(null)
    val childScreenTime: StateFlow<ScreenTimeSession?> = _childScreenTime.asStateFlow()

    // Stores the selected child's weekly screen time usage records
    private val _weeklyScreenTime = MutableStateFlow<List<ScreenTimeSession>>(emptyList())
    val weeklyScreenTime: StateFlow<List<ScreenTimeSession>> = _weeklyScreenTime.asStateFlow()

    // Stores the selected child's chatbot conversation list
    private val _childConversations = MutableStateFlow<List<Conversation>>(emptyList())
    val childConversations: StateFlow<List<Conversation>> = _childConversations.asStateFlow()

    // Stores messages for the currently selected chatbot conversation
    private val _childMessages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val childMessages: StateFlow<List<ChatMessage>> = _childMessages.asStateFlow()

    // Stores the ID of the conversation currently opened by the parent
    private val _selectedConversationId = MutableStateFlow<String?>(null)
    val selectedConversationId: StateFlow<String?> = _selectedConversationId.asStateFlow()

    // Stores content approval requests waiting for the parent to approve or reject
    private val _pendingApprovals = MutableStateFlow<List<ContentApproval>>(emptyList())
    val pendingApprovals: StateFlow<List<ContentApproval>> = _pendingApprovals.asStateFlow()

    // Stores the generated invite code used to link/register a child account
    private val _inviteCode = MutableStateFlow<String?>(null)
    val inviteCode: StateFlow<String?> = _inviteCode.asStateFlow()

    // Tracks whether a parent dashboard action is currently loading
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    // Stores user-facing error messages for dashboard actions
    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    // Stores the current state of the remove-child process
    private val _removeChildState = MutableStateFlow<RemoveChildState>(RemoveChildState.Idle)
    val removeChildState: StateFlow<RemoveChildState> = _removeChildState.asStateFlow()

    // Loads parent dashboard data as soon as the ViewModel is created
    init {
        loadChildren()
        loadPendingApprovals()
    }

    // load children linked to parent (by users.parentId == parentId, since Firestore
    // rules block writes to parent.childIds from the child during invite-code redemption)
    private fun loadChildren() {
        val parentId = accountManager.getCurrentUserId() ?: return
        viewModelScope.launch {
            accountManager.getChildrenFlow(parentId)
                .catch { e -> Log.e("ParentDashVM", "Failed to load children", e) }
                .collect { childList ->
                    _children.value = childList
                }
        }
    }

    // Loads all pending approval requests assigned to the current parent
    private fun loadPendingApprovals() {
        val parentId = accountManager.getCurrentUserId() ?: return
        viewModelScope.launch {
            contentApprovalManager.getPendingApprovalsFlow(parentId).collect { approvals ->
                _pendingApprovals.value = approvals
            }
        }
    }

    // open child details
    fun selectChild(child: User) {
        _selectedChild.value = child
        loadChildData(child.id)
    }

    // go back to parent home
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

    // load child activity data
    private fun loadChildData(childId: String) {
        viewModelScope.launch {
            // Observes the child's favourites in real time
            launch {
                accountManager.getChildFavoritesFlow(childId).collect { favorites ->
                    _childFavorites.value = favorites
                }
            }
            // Observes the child's reading history in real time
            launch {
                accountManager.getChildReadingHistoryFlow(childId).collect { history ->
                    _childHistory.value = history
                }
            }
            // Observes the child's screen time usage for the current day
            launch {
                screenTimeManager.getTodayUsageFlow(childId).collect { session ->
                    _childScreenTime.value = session
                }
            }
            // Observes the child's screen time usage for the week
            launch {
                screenTimeManager.getWeeklyUsageFlow(childId).collect { sessions ->
                    _weeklyScreenTime.value = sessions
                }
            }
            // Observes the child's chatbot conversation list and hides empty previews
            launch {
                chatDataManager.getConversationsFlow(childId)
                    .catch { e -> Log.e("ParentDashVM", "Failed to load child conversations", e) }
                    .collect { conversations ->
                        _childConversations.value = conversations.filter { it.preview.isNotBlank() }
                    }
            }
        }
    }

    // Opens a specific child chatbot conversation and loads its messages
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

    // Clears the opened conversation and removes messages from the UI state
    fun clearConversation() {
        _selectedConversationId.value = null
        _childMessages.value = emptyList()
    }

    // Generates an invite code that can be used to register or link a child account
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

    // Updates the selected child's maximum age rating and video recommendation setting
    fun updateChildFilters(childId: String, maxAgeRating: Int, allowVideos: Boolean) {
        viewModelScope.launch {
            if (!isMyChild(childId)) {
                _errorMessage.value = "Unauthorized action."
                return@launch
            }

            val result = accountManager.updateChildFilters(childId, maxAgeRating, allowVideos)
            result.fold(
                onSuccess = {
                    _errorMessage.value = null
                    val updated = accountManager.getUser(childId)
                    if (updated != null) {
                        _selectedChild.value = updated
                    }
                },
                onFailure = { error ->
                    _errorMessage.value = error.message ?: "Failed to update filters."
                }
            )
        }
    }

    // Updates the child's parental PIN after validating that it is exactly 4 digits
    fun updateChildParentalPin(childId: String, pin: String) {
        viewModelScope.launch {
            // Prevents parents from updating PINs for children they do not own
            if (!isMyChild(childId)) {
                _errorMessage.value = "Unauthorized action."
                return@launch
            }

            // Ensures the PIN follows the required 4-digit format
            if (!pin.matches(Regex("^\\d{4}$"))) {
                _errorMessage.value = "PIN must be exactly 4 digits."
                return@launch
            }

            val result = accountManager.updateChildParentalPin(childId, pin)
            result.fold(
                onSuccess = {
                    _errorMessage.value = null
                    val updated = accountManager.getUser(childId)
                    if (updated != null) {
                        _selectedChild.value = updated
                    }
                },
                onFailure = { error ->
                    _errorMessage.value = error.message ?: "Failed to update parental PIN."
                }
            )
        }
    }

    // Updates the list of content topics that should be blocked for a child
    fun updateBlockedTopics(childId: String, topics: List<String>) {
        viewModelScope.launch {
            // Blocks updates if the selected child is not linked to this parent
            if (!isMyChild(childId)) {
                _errorMessage.value = "Unauthorized action."
                return@launch
            }
            try {
                // Saves blocked topics inside the child's content filter settings
                firestore.collection("users")
                    .document(childId)
                    .update("contentFilters.blockedTopics", topics)
                    .await()
                // Refreshes selected child state so the UI shows the latest settings
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

    // Enables or disables approval-before-access for the selected child's content
    fun toggleContentApproval(childId: String, required: Boolean) {
        viewModelScope.launch {
            // Verifies parent-child ownership before changing approval settings
            if (!isMyChild(childId)) {
                _errorMessage.value = "Unauthorized action."
                return@launch
            }
            try {
                // Updates whether content approval is required for this child
                firestore.collection("users")
                    .document(childId)
                    .update("contentApprovalRequired", required)
                    .await()

                // Refreshes selected child state after the update
                val updated = accountManager.getUser(childId)
                if (updated != null) {
                    _selectedChild.value = updated
                }
            } catch (e: Exception) {
                Log.e("ParentDashVM", "Failed to toggle content approval", e)
            }
        }
    }

    // update daily screen time limit
    fun updateScreenTimeLimit(childId: String, minutes: Int) {
        viewModelScope.launch {
            if (!isMyChild(childId)) {
                _errorMessage.value = "Unauthorized action."
                return@launch
            }

            // Keeps the daily limit within a safe and reasonable range
            val safeMinutes = minutes.coerceIn(1, 600)

            // Sets warning threshold 5 minutes before the limit, with a minimum of 1 minute
            val warningMinutes = (safeMinutes - 5).coerceAtLeast(1)

            try {
                // Updates the child's screen time configuration in Firestore
                firestore.collection("users")
                    .document(childId)
                    .update(
                        mapOf(
                            "screenTimeConfig.dailyLimitMinutes" to safeMinutes,
                            "screenTimeConfig.enabled" to true,
                            "screenTimeConfig.warningThresholdMinutes" to warningMinutes
                        )
                    )
                    .await()

                // Refreshes selected child state after the screen time update
                val updated = accountManager.getUser(childId)
                if (updated != null) {
                    _selectedChild.value = updated
                }

                _errorMessage.value = null
                Log.d("ParentDashVM", "Screen time updated for $childId")
            } catch (e: Exception) {
                Log.e("ParentDashVM", "Failed to update screen time limit", e)
                _errorMessage.value = "Failed to update screen time limit."
            }
        }
    }

    // Grants extra screen time to the selected child
    fun grantScreenTimeExtension(childId: String, additionalMinutes: Int = 15) {
        viewModelScope.launch {
            if (!isMyChild(childId)) return@launch
            screenTimeManager.grantExtension(childId, additionalMinutes)
        }
    }

    // Approves a pending child content request
    fun approveContent(approvalId: String) {
        viewModelScope.launch {
            contentApprovalManager.approveContent(approvalId)
        }
    }

    // Rejects a pending child content request
    fun rejectContent(approvalId: String) {
        viewModelScope.launch {
            contentApprovalManager.rejectContent(approvalId)
        }
    }

    // Clears the current parent dashboard error message
    fun dismissError() {
        _errorMessage.value = null
    }

    // Clears the generated invite code from the UI state
    fun dismissInviteCode() {
        _inviteCode.value = null
    }

    // Soft deletes/removes a linked child account after verifying ownership and parent PIN
    fun softDeleteChild(childId: String, pin: String) {
        viewModelScope.launch {
            // Prevents removal of a child not linked to the current parent
            if (!isMyChild(childId)) {
                _removeChildState.value = RemoveChildState.Error("Unauthorized action.")
                return@launch
            }

            // Updates UI state to show the remove-child process is running
            _removeChildState.value = RemoveChildState.Loading

            val result = accountManager.softDeleteChild(childId, pin)
            result.fold(
                onSuccess = {
                    // If the removed child is currently opened, return to the parent home view
                    if (_selectedChild.value?.id == childId) {
                        clearSelectedChild()
                    }
                    _removeChildState.value = RemoveChildState.Success
                },
                onFailure = { e ->
                    _removeChildState.value =
                        RemoveChildState.Error(e.message ?: "Failed to remove child account.")
                }
            )
        }
    }

    // Resets the remove-child state after the UI has handled success or error feedback
    fun resetRemoveChildState() {
        _removeChildState.value = RemoveChildState.Idle
    }
}

// Represents the UI state of the remove-child account flow
sealed class RemoveChildState {
    object Idle : RemoveChildState()
    object Loading : RemoveChildState()
    object Success : RemoveChildState()
    data class Error(val message: String) : RemoveChildState()
}