package com.kidsrec.chatbot.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import android.util.Log
import com.kidsrec.chatbot.data.model.Feedback
import com.kidsrec.chatbot.data.model.ReadingHistory
import com.kidsrec.chatbot.data.model.User
import com.kidsrec.chatbot.data.repository.AccountManager
import com.kidsrec.chatbot.data.repository.FeedbackManager
import com.kidsrec.chatbot.data.repository.ReadingHistoryManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val accountManager: AccountManager,
    private val readingHistoryManager: ReadingHistoryManager,
    private val feedbackManager: FeedbackManager
) : ViewModel() {

    private val _user = MutableStateFlow<User?>(null)
    val user: StateFlow<User?> = _user.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _updateSuccess = MutableStateFlow(false)
    val updateSuccess: StateFlow<Boolean> = _updateSuccess.asStateFlow()

    private val _readingHistory = MutableStateFlow<List<ReadingHistory>>(emptyList())
    val readingHistory: StateFlow<List<ReadingHistory>> = _readingHistory.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val _feedbackSuccess = MutableStateFlow(false)

    init {
        loadUser()
        loadReadingHistory()
    }

    private fun loadUser() {
        viewModelScope.launch {
            val userId = accountManager.getCurrentUserId() ?: return@launch
            _isLoading.value = true

            accountManager.getUserFlow(userId)
                .catch { e ->
                    Log.e("ProfileVM", "Failed to load user", e)
                    _error.value = "Failed to load profile."
                    _isLoading.value = false
                }
                .collect { user ->
                    _user.value = user
                    _isLoading.value = false
                }
        }
    }

    private fun loadReadingHistory() {
        viewModelScope.launch {
            val userId = accountManager.getCurrentUserId() ?: return@launch
            readingHistoryManager.getHistoryFlow(userId, limit = 5)
                .catch { e ->
                    Log.e("ProfileVM", "Failed to load reading history", e)
                }
                .collect { history ->
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

            if (age !in 1..18) {
                _error.value = "Age must be between 1 and 18."
                return@launch
            }

            if (name.isBlank()) {
                _error.value = "Name cannot be empty."
                return@launch
            }

            _isLoading.value = true

            val updatedUser = currentUser.copy(
                name = name.trim(),
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
                onFailure = { e ->
                    Log.e("ProfileVM", "Failed to update profile", e)
                    _error.value = "Failed to save profile changes."
                    _isLoading.value = false
                }
            )
        }
    }

    fun submitFeedback(
        category: String,
        rating: Int,
        message: String
    ) {
        viewModelScope.launch {
            val currentUser = _user.value
            if (currentUser == null) {
                _error.value = "User profile is not loaded yet. Please try again."
                return@launch
            }

            if (message.isBlank()) {
                _error.value = "Feedback message cannot be empty."
                return@launch
            }

            val feedback = Feedback(
                userId = currentUser.id,
                userName = currentUser.name,
                userEmail = currentUser.email,
                category = category,
                rating = rating,
                message = message.trim()
            )

            val result = feedbackManager.submitFeedback(feedback)

            result.fold(
                onSuccess = {
                    _feedbackSuccess.value = true
                    _error.value = null
                },
                onFailure = { e ->
                    Log.e("ProfileVM", "Failed to submit feedback", e)
                    _error.value = e.message ?: "Failed to submit feedback."
                }
            )
        }
    }

    fun resetUpdateSuccess() {
        _updateSuccess.value = false
    }



    fun trackReading(
        title: String,
        url: String,
        coverUrl: String = "",
        isVideo: Boolean = false
    ) {
        viewModelScope.launch {
            val userId = accountManager.getCurrentUserId() ?: return@launch
            readingHistoryManager.addEntry(userId, title, url, coverUrl, isVideo)
        }
    }
}
