package com.kidsrec.chatbot.ui.auth

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.FirebaseTooManyRequestsException
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
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
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

    val isAdmin: StateFlow<Boolean> = _currentUser
        .map { user ->
            user?.planType == PlanType.ADMIN
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val isParent: StateFlow<Boolean> = _currentUser
        .map { user ->
            user?.accountType == AccountType.PARENT && user.planType != PlanType.ADMIN
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    private val _verificationMessage = MutableStateFlow<String?>(null)
    val verificationMessage: StateFlow<String?> = _verificationMessage.asStateFlow()

    init {
        checkAuthState()
    }

    private fun checkAuthState() {
        val userId = accountManager.getCurrentUserId()

        if (userId != null) {
            _authState.value = AuthState.Loading
            refreshSignedInUser(userId)
        } else {
            _currentUser.value = null
            _authState.value = AuthState.Unauthenticated
        }
    }

    private fun refreshSignedInUser(userId: String) {
        viewModelScope.launch {
            try {
                val userData = accountManager.getUser(userId)

                if (userData == null) {
                    Log.w("AuthVM", "No Firestore user document found for uid=$userId")
                    _currentUser.value = null
                    _authState.value = AuthState.Error("User profile not found.")
                    return@launch
                }

                _currentUser.value = userData
                observeUserData(userId)

                val isAdminUser = userData.planType == PlanType.ADMIN
                val isParentUser = userData.accountType == AccountType.PARENT

                when {
                    isAdminUser -> {
                        _authState.value = AuthState.Authenticated(userId)
                    }

                    isParentUser && !accountManager.isEmailVerified() -> {
                        _authState.value = AuthState.EmailNotVerified(userId)
                    }

                    else -> {
                        _authState.value = AuthState.Authenticated(userId)
                    }
                }
            } catch (e: Exception) {
                Log.e("AuthVM", "Failed to refresh signed-in user", e)
                _currentUser.value = null
                _authState.value = AuthState.Error("Failed to load account data.")
            }
        }
    }

    private fun observeUserData(userId: String) {
        viewModelScope.launch {
            accountManager.getUserFlow(userId)
                .catch { e ->
                    Log.e("AuthVM", "Failed to observe user data", e)
                }
                .collect { user ->
                    if (user != null) {
                        _currentUser.value = user
                    }
                }
        }
    }

    fun upgradeCurrentUserToPremium() {
        val userId = accountManager.getCurrentUserId() ?: return

        viewModelScope.launch {
            val current = _currentUser.value
            if (current?.planType == PlanType.ADMIN) {
                Log.w("AuthVM", "Skipping premium upgrade for admin account")
                return@launch
            }

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
        val cleanEmail = email.trim()

        viewModelScope.launch {
            _authState.value = AuthState.Loading
            _verificationMessage.value = null

            val result = accountManager.signIn(cleanEmail, password)
            result.fold(
                onSuccess = { firebaseUser ->
                    try {
                        val userData = accountManager.getUser(firebaseUser.uid)

                        if (userData == null) {
                            Log.e("AuthVM", "Signed in but Firestore user document missing")
                            _currentUser.value = null
                            _authState.value = AuthState.Error("User profile not found.")
                            return@fold
                        }

                        _currentUser.value = userData
                        observeUserData(firebaseUser.uid)

                        val isAdminUser = userData.planType == PlanType.ADMIN
                        val isParentUser = userData.accountType == AccountType.PARENT

                        when {
                            isAdminUser -> {
                                _authState.value = AuthState.Authenticated(firebaseUser.uid)
                            }

                            isParentUser && !accountManager.isEmailVerified() -> {
                                _authState.value = AuthState.EmailNotVerified(firebaseUser.uid)
                            }

                            else -> {
                                _authState.value = AuthState.Authenticated(firebaseUser.uid)
                            }
                        }
                    } catch (e: Exception) {
                        Log.e("AuthVM", "Failed after sign-in", e)
                        _currentUser.value = null
                        _authState.value = AuthState.Error("Failed to load account data.")
                    }
                },
                onFailure = { error ->
                    Log.e("AuthVM", "Sign-in error: ${error::class.simpleName} -> ${error.message}", error)
                    _currentUser.value = null
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
        val safePlanType = if (planType == PlanType.ADMIN) PlanType.FREE else planType
        val cleanEmail = email.trim()

        viewModelScope.launch {
            _authState.value = AuthState.Loading
            _verificationMessage.value = null

            val result = accountManager.signUp(cleanEmail, password, name, age, interests, readingLevel)
            result.fold(
                onSuccess = { user ->
                    val userDoc = User(
                        id = user.uid,
                        name = name,
                        email = cleanEmail,
                        age = age,
                        accountType = AccountType.CHILD,
                        interests = interests,
                        readingLevel = readingLevel,
                        planType = safePlanType
                    )

                    val updateResult = accountManager.updateUser(userDoc)
                    if (updateResult.isFailure) {
                        Log.e("AuthVM", "Failed to save user profile", updateResult.exceptionOrNull())
                        _authState.value = AuthState.Error("Failed to create user profile.")
                        return@fold
                    }

                    _currentUser.value = userDoc
                    observeUserData(user.uid)
                    _authState.value = AuthState.Authenticated(user.uid)
                },
                onFailure = { error ->
                    _currentUser.value = null
                    _authState.value = AuthState.Error(mapFirebaseError(error))
                }
            )
        }
    }

    fun signUpParent(email: String, password: String, name: String) {
        val cleanEmail = email.trim()

        viewModelScope.launch {
            _authState.value = AuthState.Loading
            _verificationMessage.value = null

            val result = accountManager.signUpParent(cleanEmail, password, name)
            result.fold(
                onSuccess = { user ->
                    try {
                        val userData = accountManager.getUser(user.uid)
                            ?: accountManager.getUserFlow(user.uid).firstOrNull()

                        _currentUser.value = userData
                        observeUserData(user.uid)

                        if (userData?.planType == PlanType.ADMIN) {
                            _authState.value = AuthState.Authenticated(user.uid)
                        } else {
                            _authState.value = AuthState.EmailNotVerified(user.uid)
                        }
                    } catch (e: Exception) {
                        Log.e("AuthVM", "Failed to load parent user after signup", e)
                        _authState.value = AuthState.Error("Failed to load account data.")
                    }
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
        val cleanEmail = email.trim()

        viewModelScope.launch {
            _authState.value = AuthState.Loading
            _verificationMessage.value = null

            val result = accountManager.signUpChild(
                cleanEmail,
                password,
                name,
                age,
                interests,
                readingLevel,
                inviteCode
            )

            result.fold(
                onSuccess = { user ->
                    refreshSignedInUser(user.uid)
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
        val cleanEmail = email.trim()

        viewModelScope.launch {
            _authState.value = AuthState.Loading
            _verificationMessage.value = null

            val result = accountManager.signUpFreeKid(
                cleanEmail,
                password,
                name,
                age,
                interests,
                readingLevel
            )

            result.fold(
                onSuccess = { user ->
                    refreshSignedInUser(user.uid)
                },
                onFailure = { error ->
                    _currentUser.value = null
                    _authState.value = AuthState.Error(mapFirebaseError(error))
                }
            )
        }
    }

    fun checkEmailVerified() {
        viewModelScope.launch {
            val state = _authState.value
            val current = _currentUser.value

            if (state is AuthState.EmailNotVerified) {
                try {
                    if (current?.planType == PlanType.ADMIN || accountManager.isEmailVerified()) {
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

    fun clearError() {
        if (_authState.value is AuthState.Error) {
            _authState.value = AuthState.Unauthenticated
        }
    }

    fun signOut() {
        _currentUser.value = null
        _verificationMessage.value = null
        _authState.value = AuthState.Unauthenticated

        viewModelScope.launch {
            accountManager.signOut()
        }
    }

    fun updateUser(user: User) {
        viewModelScope.launch {
            val current = _currentUser.value

            if (current?.planType == PlanType.ADMIN && user.planType != PlanType.ADMIN) {
                Log.w("AuthVM", "Blocked client-side attempt to downgrade admin planType")
                return@launch
            }

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