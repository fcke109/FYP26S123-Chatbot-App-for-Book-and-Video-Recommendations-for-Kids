package com.kidsrec.chatbot.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import android.util.Log
import com.kidsrec.chatbot.data.model.ReadingHistory
import com.kidsrec.chatbot.data.model.User
import com.kidsrec.chatbot.data.repository.AccountManager
import com.kidsrec.chatbot.data.repository.ReadingHistoryManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import javax.inject.Inject

// ViewModel responsible for managing the user's profile screen data and actions
@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val accountManager: AccountManager,
    private val readingHistoryManager: ReadingHistoryManager
) : ViewModel() {

    // Stores the current logged-in user's profile data
    private val _user = MutableStateFlow<User?>(null)
    val user: StateFlow<User?> = _user.asStateFlow()

    // Tracks whether profile data is currently loading or being updated
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    // Indicates whether a profile update was completed successfully
    private val _updateSuccess = MutableStateFlow(false)
    val updateSuccess: StateFlow<Boolean> = _updateSuccess.asStateFlow()

    // Stores the user's recent reading history shown on the profile screen
    private val _readingHistory = MutableStateFlow<List<ReadingHistory>>(emptyList())
    val readingHistory: StateFlow<List<ReadingHistory>> = _readingHistory.asStateFlow()

    // Stores any error message that should be shown to the user
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    // Loads profile information and reading history when the ViewModel is created
    init {
        loadUser()
        loadReadingHistory()
    }

    // Loads the current user's profile and listens for realtime updates
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

    // Loads the latest reading history entries for the current user
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

    // Updates the user's editable profile fields after validating input
    fun updateProfile(
        name: String,
        age: Int,
        interests: List<String>,
        readingLevel: String
    ) {
        viewModelScope.launch {
            _error.value = null

            // Validates that the user entered a name
            if (name.isBlank()) {
                _error.value = "Name cannot be empty."
                return@launch
            }
            // Validates that the age is within the allowed range
            if (age !in 1..18) {
                _error.value = "Age must be between 1 and 18."
                return@launch
            }

            // Resolve current user, with a one-shot fetch fallback if the flow hasn't emitted yet.
            val currentUser = _user.value ?: run {
                val userId = accountManager.getCurrentUserId()
                if (userId == null) {
                    _error.value = "You're signed out. Please sign in again."
                    return@launch
                }
                accountManager.getUser(userId)
            }
            // Stops the update if the profile could not be loaded
            if (currentUser == null) {
                _error.value = "Could not load your profile. Please try again."
                return@launch
            }

            _isLoading.value = true

            // Creates an updated copy of the current user with the edited profile details
            val updatedUser = currentUser.copy(
                name = name.trim(),
                age = age,
                interests = interests,
                readingLevel = readingLevel
            )

            // Saves the updated profile through AccountManager
            val result = accountManager.updateUser(updatedUser)
            result.fold(
                onSuccess = {
                    _user.value = updatedUser
                    _updateSuccess.value = true
                    _isLoading.value = false
                },
                onFailure = { e ->
                    Log.e("ProfileVM", "Failed to update profile", e)
                    _error.value = e.message ?: "Failed to save profile changes."
                    _isLoading.value = false
                }
            )
        }
    }

    // Resets the update success flag after the UI has handled it
    fun resetUpdateSuccess() {
        _updateSuccess.value = false
    }

    // Clears the current error message
    fun clearError() {
        _error.value = null
    }

    // Adds a book or video entry to the user's reading history
    fun trackReading(title: String, url: String, coverUrl: String = "", isVideo: Boolean = false) {
        viewModelScope.launch {
            val userId = accountManager.getCurrentUserId() ?: return@launch
            readingHistoryManager.addEntry(userId, title, url, coverUrl, isVideo)
        }
    }
}
