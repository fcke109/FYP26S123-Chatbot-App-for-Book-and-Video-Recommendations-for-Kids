package com.kidsrec.chatbot.ui.screentime

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.firestore.FirebaseFirestore
import com.kidsrec.chatbot.data.model.ScreenTimeConfig
import com.kidsrec.chatbot.data.model.ScreenTimeSession
import com.kidsrec.chatbot.data.repository.AccountManager
import com.kidsrec.chatbot.data.repository.ScreenTimeManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

// ViewModel responsible for tracking and enforcing child screen time limits
@HiltViewModel
class ScreenTimeViewModel @Inject constructor(
    private val screenTimeManager: ScreenTimeManager,
    private val accountManager: AccountManager
) : ViewModel() {

    // Stores the child's screen time usage for the current day
    private val _todaySession = MutableStateFlow<ScreenTimeSession?>(null)
    val todaySession: StateFlow<ScreenTimeSession?> = _todaySession.asStateFlow()

    // Tracks whether the child has reached the allowed daily screen time limit
    private val _isTimeLimitReached = MutableStateFlow(false)
    val isTimeLimitReached: StateFlow<Boolean> = _isTimeLimitReached.asStateFlow()

    // Stores the parent's configured screen time settings for the child
    private val _screenTimeConfig = MutableStateFlow(ScreenTimeConfig())
    val screenTimeConfig: StateFlow<ScreenTimeConfig> = _screenTimeConfig.asStateFlow()

    // Tracks whether the child has already requested more screen time
    private val _extensionRequested = MutableStateFlow(false)
    val extensionRequested: StateFlow<Boolean> = _extensionRequested.asStateFlow()

    // Job used for the repeating timer that increases usage every minute
    private var timerJob: Job? = null

    // Job used to listen for user/profile changes, such as updated parent screen time settings
    private var userJob: Job? = null

    // start app-wide screen time tracking for the currently logged-in child
    fun startTracking() {
        val userId = accountManager.getCurrentUserId() ?: return

        // Prevents multiple timer jobs from running at the same time
        if (timerJob != null) return

        // Creates or resumes the child's screen time session
        screenTimeManager.startSession(userId)

        // Listens to user document changes so limits update immediately when parent settings change
        userJob = viewModelScope.launch {
            accountManager.getUserFlow(userId).collect { user ->
                if (user != null) {
                    // Updates local screen time configuration from the user profile
                    _screenTimeConfig.value = user.screenTimeConfig

                    val limit = user.screenTimeConfig.dailyLimitMinutes
                    val used = user.todayUsageMinutes
                    val enabled = user.screenTimeConfig.enabled

                    // Updates today's session state using the latest usage value from Firestore
                    _todaySession.value = ScreenTimeSession(
                        userId = user.id,
                        date = LocalDate.now().toString(),
                        totalMinutes = used
                    )

                    // Marks the limit as reached only when screen time control is enabled
                    _isTimeLimitReached.value =
                        enabled && limit > 0 && used >= limit
                }
            }
        }

        // Starts the real-time timer that increments usage every 60 seconds
        timerJob = viewModelScope.launch {
            while (true) {
                delay(60_000)

                // Gets the latest user data before updating usage
                val user = accountManager.getUser(userId) ?: continue
                val config = user.screenTimeConfig

                // Skips tracking if screen time control is disabled
                if (!config.enabled) continue
                // Skips tracking if no valid daily limit has been set
                if (config.dailyLimitMinutes <= 0) continue

                // Adds one minute of usage
                val newUsage = user.todayUsageMinutes + 1

                // Updates the child's daily usage in Firestore
                FirebaseFirestore.getInstance()
                    .collection("users")
                    .document(user.id)
                    .update("todayUsageMinutes", newUsage)
                    .addOnSuccessListener {
                        Log.d("SCREEN_TIME", "Usage updated to $newUsage mins")
                    }
                    .addOnFailureListener { e ->
                        Log.e("SCREEN_TIME", "Failed to update usage", e)
                    }

                // Updates local UI state immediately after usage increases
                _todaySession.value = ScreenTimeSession(
                    userId = user.id,
                    date = LocalDate.now().toString(),
                    totalMinutes = newUsage
                )

                // Blocks the app once the new usage reaches the daily limit
                _isTimeLimitReached.value =
                    newUsage >= config.dailyLimitMinutes
            }
        }
    }

    // stop timer and ends the current screen time session
    fun stopTracking() {
        val userId = accountManager.getCurrentUserId()

        // Cancels the minute-by-minute tracking job
        timerJob?.cancel()
        timerJob = null

        // Cancels the user document listener job
        userJob?.cancel()
        userJob = null

        // Records the end of the screen time session if a user is still signed in
        if (userId != null) {
            viewModelScope.launch {
                screenTimeManager.endSession(userId)
            }
        }
    }

    // Sends a request for additional screen time to the parent
    fun requestMoreTime() {
        val userId = accountManager.getCurrentUserId() ?: return

        viewModelScope.launch {
            // Creates the extension request through the screen time manager
            screenTimeManager.requestExtension(userId)
            // Updates UI state so the app knows the request has been sent
            _extensionRequested.value = true
        }
    }
}