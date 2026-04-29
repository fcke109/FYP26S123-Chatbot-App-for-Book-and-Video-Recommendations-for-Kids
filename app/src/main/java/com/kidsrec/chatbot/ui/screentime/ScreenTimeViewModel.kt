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

@HiltViewModel
class ScreenTimeViewModel @Inject constructor(
    private val screenTimeManager: ScreenTimeManager,
    private val accountManager: AccountManager
) : ViewModel() {

    // today's session
    private val _todaySession = MutableStateFlow<ScreenTimeSession?>(null)
    val todaySession: StateFlow<ScreenTimeSession?> = _todaySession.asStateFlow()

    // true when time is finished
    private val _isTimeLimitReached = MutableStateFlow(false)
    val isTimeLimitReached: StateFlow<Boolean> = _isTimeLimitReached.asStateFlow()

    // parent screen time settings
    private val _screenTimeConfig = MutableStateFlow(ScreenTimeConfig())
    val screenTimeConfig: StateFlow<ScreenTimeConfig> = _screenTimeConfig.asStateFlow()

    // request more time
    private val _extensionRequested = MutableStateFlow(false)
    val extensionRequested: StateFlow<Boolean> = _extensionRequested.asStateFlow()

    // timer job
    private var timerJob: Job? = null

    // listen job
    private var userJob: Job? = null

    // start app-wide tracking
    fun startTracking() {
        val userId = accountManager.getCurrentUserId() ?: return

        if (timerJob != null) return

        screenTimeManager.startSession(userId)

        // listen to user changes
        userJob = viewModelScope.launch {
            accountManager.getUserFlow(userId).collect { user ->
                if (user != null) {
                    _screenTimeConfig.value = user.screenTimeConfig

                    val limit = user.screenTimeConfig.dailyLimitMinutes
                    val used = user.todayUsageMinutes
                    val enabled = user.screenTimeConfig.enabled

                    _todaySession.value = ScreenTimeSession(
                        userId = user.id,
                        date = LocalDate.now().toString(),
                        totalMinutes = used
                    )

                    _isTimeLimitReached.value =
                        enabled && limit > 0 && used >= limit
                }
            }
        }

        // real timer
        timerJob = viewModelScope.launch {
            while (true) {
                delay(60_000)

                val user = accountManager.getUser(userId) ?: continue
                val config = user.screenTimeConfig

                if (!config.enabled) continue
                if (config.dailyLimitMinutes <= 0) continue

                val newUsage = user.todayUsageMinutes + 1

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

                _todaySession.value = ScreenTimeSession(
                    userId = user.id,
                    date = LocalDate.now().toString(),
                    totalMinutes = newUsage
                )

                _isTimeLimitReached.value =
                    newUsage >= config.dailyLimitMinutes
            }
        }
    }

    // stop timer
    fun stopTracking() {
        val userId = accountManager.getCurrentUserId()

        timerJob?.cancel()
        timerJob = null

        userJob?.cancel()
        userJob = null

        if (userId != null) {
            viewModelScope.launch {
                screenTimeManager.endSession(userId)
            }
        }
    }

    // ask parent for more time
    fun requestMoreTime() {
        val userId = accountManager.getCurrentUserId() ?: return

        viewModelScope.launch {
            screenTimeManager.requestExtension(userId)
            _extensionRequested.value = true
        }
    }
}