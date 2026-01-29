package com.kidsrec.chatbot.ui.auth

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
class AuthViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _authState = MutableStateFlow<AuthState>(AuthState.Initial)
    val authState: StateFlow<AuthState> = _authState.asStateFlow()

    private val _currentUser = MutableStateFlow<User?>(null)
    val currentUser: StateFlow<User?> = _currentUser.asStateFlow()

    init {
        checkAuthState()
    }

    private fun checkAuthState() {
        val userId = authRepository.getCurrentUserId()
        if (userId != null) {
            _authState.value = AuthState.Authenticated(userId)
            loadUserData(userId)
        } else {
            _authState.value = AuthState.Unauthenticated
        }
    }

    private fun loadUserData(userId: String) {
        viewModelScope.launch {
            authRepository.getUserFlow(userId).collect { user ->
                _currentUser.value = user
            }
        }
    }

    fun signIn(email: String, password: String) {
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            val result = authRepository.signIn(email, password)
            result.fold(
                onSuccess = { user ->
                    _authState.value = AuthState.Authenticated(user.uid)
                    loadUserData(user.uid)
                },
                onFailure = { error ->
                    _authState.value = AuthState.Error(error.message ?: "Login failed")
                }
            )
        }
    }

    fun signUp(
        email: String,
        password: String,
        name: String,
        age: Int,
        interests: List<String>,
        readingLevel: String
    ) {
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            val result = authRepository.signUp(email, password, name, age, interests, readingLevel)
            result.fold(
                onSuccess = { user ->
                    _authState.value = AuthState.Authenticated(user.uid)
                    loadUserData(user.uid)
                },
                onFailure = { error ->
                    _authState.value = AuthState.Error(error.message ?: "Registration failed")
                }
            )
        }
    }

    fun signOut() {
        viewModelScope.launch {
            authRepository.signOut()
            _authState.value = AuthState.Unauthenticated
            _currentUser.value = null
        }
    }

    fun updateUser(user: User) {
        viewModelScope.launch {
            authRepository.updateUser(user)
        }
    }
}

sealed class AuthState {
    object Initial : AuthState()
    object Loading : AuthState()
    object Unauthenticated : AuthState()
    data class Authenticated(val userId: String) : AuthState()
    data class Error(val message: String) : AuthState()
}
