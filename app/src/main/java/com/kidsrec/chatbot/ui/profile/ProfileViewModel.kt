package com.kidsrec.chatbot.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kidsrec.chatbot.data.model.User
import com.kidsrec.chatbot.data.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _user = MutableStateFlow<User?>(null)
    val user: StateFlow<User?> = _user.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _updateSuccess = MutableStateFlow(false)
    val updateSuccess: StateFlow<Boolean> = _updateSuccess.asStateFlow()

    init {
        loadUser()
    }

    private fun loadUser() {
        viewModelScope.launch {
            val userId = authRepository.getCurrentUserId() ?: return@launch
            _isLoading.value = true

            authRepository.getUserFlow(userId).collect { user ->
                _user.value = user
                _isLoading.value = false
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

            val result = authRepository.updateUser(updatedUser)
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
}
