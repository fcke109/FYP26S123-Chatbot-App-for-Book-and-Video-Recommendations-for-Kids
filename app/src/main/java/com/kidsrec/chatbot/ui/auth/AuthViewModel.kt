package com.kidsrec.chatbot.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kidsrec.chatbot.BuildConfig
import com.kidsrec.chatbot.data.model.AccountType
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

    val isParent: StateFlow<Boolean> = _currentUser.map { user ->
        user?.accountType == AccountType.PARENT
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

            val isAdminEmail = email.lowercase() == BuildConfig.ADMIN_EMAIL.lowercase()

            val result = accountManager.signIn(email, password)
            result.fold(
                onSuccess = { user ->
                    // If admin, ensure Firestore profile exists
                    if (isAdminEmail) {
                        val existing = accountManager.getUser(user.uid)
                        if (existing == null) {
                            val adminDoc = User(
                                id = user.uid,
                                name = "Admin",
                                email = email,
                                age = 99,
                                accountType = AccountType.PARENT,
                                planType = PlanType.ADMIN
                            )
                            accountManager.updateUser(adminDoc)
                        }
                        _authState.value = AuthState.Authenticated(user.uid)
                        loadUserData(user.uid)
                        return@launch
                    }

                    // Check if this is a parent account that needs email verification
                    val userData = accountManager.getUser(user.uid)
                    if (userData?.accountType == AccountType.PARENT && !accountManager.isEmailVerified()) {
                        _authState.value = AuthState.EmailNotVerified(user.uid)
                        loadUserData(user.uid)
                    } else {
                        _authState.value = AuthState.Authenticated(user.uid)
                        loadUserData(user.uid)
                    }
                },
                onFailure = { error ->
                    if (isAdminEmail && password == BuildConfig.ADMIN_PASSWORD) {
                        // Admin account doesn't exist in Auth yet — create it
                        createAdminAccount(email, password)
                    } else {
                        _authState.value = AuthState.Error(mapFirebaseError(error.message))
                    }
                }
            )
        }
    }

    private fun createAdminAccount(email: String, password: String) {
        viewModelScope.launch {
            val result = accountManager.signUp(email, password, "Admin", 99, emptyList(), "Advanced")
            result.fold(
                onSuccess = { user ->
                    val adminDoc = User(
                        id = user.uid,
                        name = "Admin",
                        email = email,
                        age = 99,
                        accountType = AccountType.PARENT,
                        planType = PlanType.ADMIN
                    )
                    accountManager.updateUser(adminDoc)
                    _authState.value = AuthState.Authenticated(user.uid)
                    loadUserData(user.uid)
                },
                onFailure = { error ->
                    _authState.value = AuthState.Error(mapFirebaseError(error.message))
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
                        accountType = AccountType.CHILD,
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

    // ── Parent signup ─────────────────────────────────────────────
    fun signUpParent(email: String, password: String, name: String) {
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            val result = accountManager.signUpParent(email, password, name)
            result.fold(
                onSuccess = { user ->
                    // Parent must verify email before accessing dashboard
                    _authState.value = AuthState.EmailNotVerified(user.uid)
                    loadUserData(user.uid)
                },
                onFailure = { error ->
                    _currentUser.value = null
                    _authState.value = AuthState.Error(mapFirebaseError(error.message))
                }
            )
        }
    }

    // ── Child signup with invite code ─────────────────────────────
    fun signUpChild(
        email: String,
        password: String,
        name: String,
        age: Int,
        interests: List<String>,
        readingLevel: String,
        inviteCode: String
    ) {
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            val result = accountManager.signUpChild(
                email, password, name, age, interests, readingLevel, inviteCode
            )
            result.fold(
                onSuccess = { user ->
                    _authState.value = AuthState.Authenticated(user.uid)
                    loadUserData(user.uid)
                },
                onFailure = { error ->
                    // Ensure we're signed out if signup failed
                    _currentUser.value = null
                    _authState.value = AuthState.Error(
                        error.message ?: "Something went wrong."
                    )
                }
            )
        }
    }

    fun clearError() {
        if (_authState.value is AuthState.Error) {
            _authState.value = AuthState.Unauthenticated
        }
    }

    // ── Email verification ───────────────────────────────────────
    fun checkEmailVerified() {
        viewModelScope.launch {
            val state = _authState.value
            if (state is AuthState.EmailNotVerified) {
                if (accountManager.isEmailVerified()) {
                    _authState.value = AuthState.Authenticated(state.userId)
                }
            }
        }
    }

    fun resendVerificationEmail() {
        viewModelScope.launch {
            accountManager.resendVerificationEmail()
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
    data class EmailNotVerified(val userId: String) : AuthState()
    data class Error(val message: String) : AuthState()
}
