package com.kidsrec.chatbot.ui.auth

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.FirebaseTooManyRequestsException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import com.kidsrec.chatbot.data.model.AccountType
import com.kidsrec.chatbot.data.model.PlanType
import com.kidsrec.chatbot.data.model.User
import com.kidsrec.chatbot.data.repository.AccountManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val ADMIN_EMAIL = "admin@littledino.com"

private fun isAdminEmail(email: String?): Boolean {
    return email.equals(ADMIN_EMAIL, ignoreCase = true)
}

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val accountManager: AccountManager
) : ViewModel() {

    private val _authState = MutableStateFlow<AuthState>(AuthState.Initial)
    val authState: StateFlow<AuthState> = _authState.asStateFlow()

    private val _currentUser = MutableStateFlow<User?>(null)
    val currentUser: StateFlow<User?> = _currentUser.asStateFlow()

    val isAdmin: StateFlow<Boolean> = _currentUser.map { user ->
        val firebaseEmail = FirebaseAuth.getInstance().currentUser?.email
        isAdminEmail(firebaseEmail) || isAdminEmail(user?.email)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val isParent: StateFlow<Boolean> = _currentUser.map { user ->
        val firebaseEmail = FirebaseAuth.getInstance().currentUser?.email
        val admin = isAdminEmail(firebaseEmail) || isAdminEmail(user?.email)
        !admin && user?.accountType == AccountType.PARENT
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    init {
        checkAuthState()
    }

    private fun checkAuthState() {
        val userId = accountManager.getCurrentUserId()
        val firebaseEmail = FirebaseAuth.getInstance().currentUser?.email

        if (userId != null) {
            // Admin bypasses email verification flow
            if (isAdminEmail(firebaseEmail)) {
                _authState.value = AuthState.Authenticated(userId)
                loadUserData(userId)
            } else {
                _authState.value = AuthState.Authenticated(userId)
                loadUserData(userId)
            }
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

    fun upgradeCurrentUserToPremium() {
        val userId = accountManager.getCurrentUserId() ?: return
        viewModelScope.launch {
            val result = accountManager.upgradeToPremium(userId)
            if (result.isSuccess) {
                _currentUser.value = _currentUser.value?.copy(
                    planType = PlanType.PREMIUM,
                    isGuest = false
                )
            } else {
                Log.e("AuthVM", "Failed to upgrade user to premium", result.exceptionOrNull())
            }
        }
    }

    fun signIn(email: String, password: String) {
        viewModelScope.launch {
            _authState.value = AuthState.Loading

            val result = accountManager.signIn(email, password)
            result.fold(
                onSuccess = { user ->
                    val signedInEmail = user.email ?: FirebaseAuth.getInstance().currentUser?.email
                    val isAdminUser = isAdminEmail(signedInEmail)

                    val userData = accountManager.getUser(user.uid)

                    when {
                        // Admin bypasses email verification completely
                        isAdminUser -> {
                            _authState.value = AuthState.Authenticated(user.uid)
                            loadUserData(user.uid)
                        }

                        // Normal parent account still requires email verification
                        userData?.accountType == AccountType.PARENT && !accountManager.isEmailVerified() -> {
                            _authState.value = AuthState.EmailNotVerified(user.uid)
                            loadUserData(user.uid)
                        }

                        else -> {
                            _authState.value = AuthState.Authenticated(user.uid)
                            loadUserData(user.uid)
                        }
                    }
                },
                onFailure = { error ->
                    Log.e("AuthVM", "Sign-in error: ${error::class.simpleName} -> ${error.message}", error)
                    _authState.value = AuthState.Error(mapFirebaseError(error))
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
                    val updateResult = accountManager.updateUser(userDoc)
                    if (updateResult.isFailure) {
                        Log.e("AuthVM", "Failed to save user profile", updateResult.exceptionOrNull())
                    }
                    _authState.value = AuthState.Authenticated(user.uid)
                    loadUserData(user.uid)
                },
                onFailure = { error ->
                    _authState.value = AuthState.Error(mapFirebaseError(error))
                }
            )
        }
    }

    fun signUpParent(email: String, password: String, name: String) {
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            val result = accountManager.signUpParent(email, password, name)
            result.fold(
                onSuccess = { user ->
                    val signedInEmail = user.email ?: FirebaseAuth.getInstance().currentUser?.email

                    if (isAdminEmail(signedInEmail)) {
                        _authState.value = AuthState.Authenticated(user.uid)
                    } else {
                        _authState.value = AuthState.EmailNotVerified(user.uid)
                    }
                    loadUserData(user.uid)
                },
                onFailure = { error ->
                    _currentUser.value = null
                    _authState.value = AuthState.Error(mapFirebaseError(error))
                }
            )
        }
    }

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
                    _currentUser.value = null
                    _authState.value = AuthState.Error(mapFirebaseError(error))
                }
            )
        }
    }

    fun signUpFreeKid(
        email: String,
        password: String,
        name: String,
        age: Int,
        interests: List<String>,
        readingLevel: String
    ) {
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            val result = accountManager.signUpFreeKid(
                email, password, name, age, interests, readingLevel
            )
            result.fold(
                onSuccess = { user ->
                    _authState.value = AuthState.Authenticated(user.uid)
                    loadUserData(user.uid)
                },
                onFailure = { error ->
                    _currentUser.value = null
                    _authState.value = AuthState.Error(mapFirebaseError(error))
                }
            )
        }
    }

    fun clearError() {
        if (_authState.value is AuthState.Error) {
            _authState.value = AuthState.Unauthenticated
        }
    }

    private val _verificationMessage = MutableStateFlow<String?>(null)
    val verificationMessage: StateFlow<String?> = _verificationMessage.asStateFlow()

    fun checkEmailVerified() {
        viewModelScope.launch {
            val state = _authState.value
            val firebaseEmail = FirebaseAuth.getInstance().currentUser?.email

            if (state is AuthState.EmailNotVerified) {
                try {
                    if (isAdminEmail(firebaseEmail) || accountManager.isEmailVerified()) {
                        _verificationMessage.value = null
                        _authState.value = AuthState.Authenticated(state.userId)
                    } else {
                        _verificationMessage.value = "Email not yet verified. Please check your inbox."
                    }
                } catch (e: Exception) {
                    _verificationMessage.value = "Could not check verification status. Try again."
                }
            }
        }
    }

    fun clearVerificationMessage() {
        _verificationMessage.value = null
    }

    fun resendVerificationEmail() {
        viewModelScope.launch {
            val result = accountManager.resendVerificationEmail()
            if (result.isFailure) {
                _verificationMessage.value = "Failed to resend email. Try again."
            }
        }
    }

    fun signOut() {
        _currentUser.value = null
        _authState.value = AuthState.Unauthenticated
        viewModelScope.launch {
            accountManager.signOut()
        }
    }

    fun updateUser(user: User) {
        viewModelScope.launch {
            accountManager.updateUser(user)
        }
    }

    private fun mapFirebaseError(error: Throwable): String {
        return when (error) {
            is FirebaseAuthInvalidCredentialsException -> "Incorrect email or password."
            is FirebaseAuthInvalidUserException -> "No account found with this email."
            is FirebaseAuthUserCollisionException -> "Email already registered."
            is FirebaseAuthWeakPasswordException -> "Password is too weak. Use at least 6 characters."
            is FirebaseTooManyRequestsException -> "Too many attempts. Try again later."
            is FirebaseNetworkException -> "Network error. Check your connection."
            else -> {
                Log.w("AuthVM", "Unmapped auth error: ${error::class.simpleName} -> ${error.message}")
                "Something went wrong. Please try again."
            }
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