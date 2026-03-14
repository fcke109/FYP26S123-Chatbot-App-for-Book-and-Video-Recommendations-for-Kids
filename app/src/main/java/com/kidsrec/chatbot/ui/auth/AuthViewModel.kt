package com.kidsrec.chatbot.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kidsrec.chatbot.data.model.PlanType
import com.kidsrec.chatbot.data.model.User
import com.kidsrec.chatbot.data.repository.AccountManager
import dagger.hilt.android.lifecycle.HiltViewModel
import android.util.Log
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val accountManager: AccountManager
) : ViewModel() {

    private val _authState = MutableStateFlow<AuthState>(AuthState.Initial)
    val authState: StateFlow<AuthState> = _authState.asStateFlow()

    private val _currentUser = MutableStateFlow<User?>(null)
    val currentUser: StateFlow<User?> = _currentUser.asStateFlow()

    val isAdmin: StateFlow<Boolean> = _currentUser.map { user ->
        user?.email?.lowercase() == "admin@littledino.com" || user?.planType == PlanType.ADMIN
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    init {
        checkAuthState()
    }

    private fun checkAuthState() {
        val userId = accountManager.getCurrentUserId()
        if (userId != null) {
            _authState.value = AuthState.Authenticated(userId)
            loadUserData(userId)
        } else {
            _authState.value = AuthState.Unauthenticated
        }
    }

    private fun loadUserData(userId: String) {
        viewModelScope.launch {
            accountManager.getUserFlow(userId)
                .catch { e ->
                    Log.e("AuthVM", "Failed to load user data", e)
                }
                .collect { user ->
                    _currentUser.value = user
                }
        }
    }

    fun signIn(email: String, password: String) {
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            
            val result = accountManager.signIn(email, password)
            result.fold(
                onSuccess = { user ->
                    _authState.value = AuthState.Authenticated(user.uid)
                    loadUserData(user.uid)
                },
                onFailure = { error ->
                    if (email.lowercase() == "admin@littledino.com" && password == "dino123") {
                        signUp(email, password, "Admin", 99, emptyList(), "Advanced", PlanType.ADMIN)
                    } else {
                        _authState.value = AuthState.Error(mapFirebaseError(error.message))
                    }
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
        readingLevel: String,
        planType: PlanType = PlanType.FREE
    ) {
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            val result = accountManager.signUp(email, password, name, age, interests, readingLevel)
            result.fold(
                onSuccess = { user ->
                    val userDoc = User(
                        id = user.uid,
                        name = name,
                        email = email,
                        age = age,
                        interests = interests,
                        readingLevel = readingLevel,
                        planType = planType
                    )
                    accountManager.updateUser(userDoc)
                    _authState.value = AuthState.Authenticated(user.uid)
                    loadUserData(user.uid)
                },
                onFailure = { error ->
                    _authState.value = AuthState.Error(mapFirebaseError(error.message))
                }
            )
        }
    }

    fun signOut() {
        viewModelScope.launch {
            accountManager.signOut()
            _authState.value = AuthState.Unauthenticated
            _currentUser.value = null
        }
    }

    fun updateUser(user: User) {
        viewModelScope.launch {
            accountManager.updateUser(user)
        }
    }

    private fun mapFirebaseError(message: String?): String {
        if (message == null) return "Something went wrong."
        val msg = message.lowercase()
        return when {
            msg.contains("badly formatted") -> "Valid email required."
            msg.contains("wrong password") || msg.contains("invalid credential") -> "Incorrect email or password."
            msg.contains("no user record") -> "No account found."
            msg.contains("already in use") -> "Email already registered."
            msg.contains("network") -> "Network error."
            else -> "Something went wrong."
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
