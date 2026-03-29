package com.kidsrec.chatbot.ui.parent

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import android.util.Log
import com.kidsrec.chatbot.data.model.Favorite
import com.kidsrec.chatbot.data.model.ReadingHistory
import com.kidsrec.chatbot.data.model.User
import com.kidsrec.chatbot.data.repository.AccountManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ParentDashboardViewModel @Inject constructor(
    private val accountManager: AccountManager
) : ViewModel() {

    private val _children = MutableStateFlow<List<User>>(emptyList())
    val children: StateFlow<List<User>> = _children.asStateFlow()

    private val _selectedChild = MutableStateFlow<User?>(null)
    val selectedChild: StateFlow<User?> = _selectedChild.asStateFlow()

    private val _childFavorites = MutableStateFlow<List<Favorite>>(emptyList())
    val childFavorites: StateFlow<List<Favorite>> = _childFavorites.asStateFlow()

    private val _childHistory = MutableStateFlow<List<ReadingHistory>>(emptyList())
    val childHistory: StateFlow<List<ReadingHistory>> = _childHistory.asStateFlow()

    private val _inviteCode = MutableStateFlow<String?>(null)
    val inviteCode: StateFlow<String?> = _inviteCode.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    init {
        loadChildren()
    }

    private fun loadChildren() {
        val parentId = accountManager.getCurrentUserId() ?: return
        viewModelScope.launch {
            // Listen to parent's own doc to get childIds, then fetch each child by ID
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

    fun selectChild(child: User) {
        _selectedChild.value = child
        loadChildData(child.id)
    }

    fun clearSelectedChild() {
        _selectedChild.value = null
        _childFavorites.value = emptyList()
        _childHistory.value = emptyList()
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
        }
    }

    fun generateInviteCode() {
        val parentId = accountManager.getCurrentUserId() ?: return
        viewModelScope.launch {
            _isLoading.value = true
            // Get parent name
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
            accountManager.updateChildFilters(childId, maxAgeRating, allowVideos)
            // Refresh the selected child data
            val updated = accountManager.getUser(childId)
            if (updated != null) {
                _selectedChild.value = updated
            }
        }
    }

    fun dismissError() {
        _errorMessage.value = null
    }

    fun dismissInviteCode() {
        _inviteCode.value = null
    }
}
