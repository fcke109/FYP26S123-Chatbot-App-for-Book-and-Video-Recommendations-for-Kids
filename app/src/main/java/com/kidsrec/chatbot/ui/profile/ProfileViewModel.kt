package com.kidsrec.chatbot.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kidsrec.chatbot.data.model.ReadingHistory
import com.kidsrec.chatbot.data.model.User
import com.kidsrec.chatbot.data.repository.AccountManager
import com.kidsrec.chatbot.data.repository.ReadingHistoryManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val accountManager: AccountManager,
    private val readingHistoryManager: ReadingHistoryManager
) : ViewModel() {

    private val _user = MutableStateFlow<User?>(null)
    val user: StateFlow<User?> = _user.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _updateSuccess = MutableStateFlow(false)
    val updateSuccess: StateFlow<Boolean> = _updateSuccess.asStateFlow()

    private val _readingHistory = MutableStateFlow<List<ReadingHistory>>(emptyList())
    val readingHistory: StateFlow<List<ReadingHistory>> = _readingHistory.asStateFlow()

    init {
        loadUser()
        loadReadingHistory()
    }

    private fun loadUser() {
        viewModelScope.launch {
            val userId = accountManager.getCurrentUserId() ?: return@launch
            _isLoading.value = true

            accountManager.getUserFlow(userId).collect { user ->
                _user.value = user
                _isLoading.value = false
            }
        }
    }

    private fun loadReadingHistory() {
        viewModelScope.launch {
            val userId = accountManager.getCurrentUserId() ?: return@launch
            readingHistoryManager.getHistoryFlow(userId, limit = 5).collect { history ->
                _readingHistory.value = history
            }
        }
    }

    fun updateProfile(
        name: String,
        age: Int,
        interests: List<String>,
        readingLevel: String
    ) {
        viewModelScope.launch {
            val currentUser = _user.value ?: return@launch
            _isLoading.value = true

            val updatedUser = currentUser.copy(
                name = name,
                age = age,
                interests = interests,
                readingLevel = readingLevel
            )

            val result = accountManager.updateUser(updatedUser)
            result.fold(
                onSuccess = {
                    _updateSuccess.value = true
                    _isLoading.value = false
                },
                onFailure = {
                    _isLoading.value = false
                }
            )
        }
    }

    fun resetUpdateSuccess() {
        _updateSuccess.value = false
    }

    fun trackReading(title: String, url: String, coverUrl: String = "", isVideo: Boolean = false) {
        viewModelScope.launch {
            val userId = accountManager.getCurrentUserId() ?: return@launch
            readingHistoryManager.addEntry(userId, title, url, coverUrl, isVideo)
        }
    }
}
