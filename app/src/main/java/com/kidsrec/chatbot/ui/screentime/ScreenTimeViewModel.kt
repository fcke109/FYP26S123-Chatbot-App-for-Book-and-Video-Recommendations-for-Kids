package com.kidsrec.chatbot.ui.screentime

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kidsrec.chatbot.data.model.ScreenTimeConfig
import com.kidsrec.chatbot.data.model.ScreenTimeSession
import com.kidsrec.chatbot.data.repository.AccountManager
import com.kidsrec.chatbot.data.repository.ScreenTimeManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ScreenTimeViewModel @Inject constructor(
    private val screenTimeManager: ScreenTimeManager,
    private val accountManager: AccountManager
) : ViewModel() {

    private val _todaySession = MutableStateFlow<ScreenTimeSession?>(null)
    val todaySession: StateFlow<ScreenTimeSession?> = _todaySession.asStateFlow()

    private val _isTimeLimitReached = MutableStateFlow(false)
    val isTimeLimitReached: StateFlow<Boolean> = _isTimeLimitReached.asStateFlow()

    private val _screenTimeConfig = MutableStateFlow(ScreenTimeConfig())
    val screenTimeConfig: StateFlow<ScreenTimeConfig> = _screenTimeConfig.asStateFlow()

    private val _extensionRequested = MutableStateFlow(false)
    val extensionRequested: StateFlow<Boolean> = _extensionRequested.asStateFlow()

    fun startTracking() {
        val userId = accountManager.getCurrentUserId() ?: return
        screenTimeManager.startSession(userId)

        // Load user's screen time config
        viewModelScope.launch {
            accountManager.getUserFlow(userId)
                .collect { user ->
                    user?.let {
                        _screenTimeConfig.value = it.screenTimeConfig
                    }
                }
        }

        // Observe today's usage
        viewModelScope.launch {
            screenTimeManager.getTodayUsageFlow(userId)
                .collect { session ->
                    _todaySession.value = session
                    val config = _screenTimeConfig.value
                    if (config.isEnabled && session != null) {
                        val effectiveLimit = config.dailyLimitMinutes + (session.bonusMinutes)
                        _isTimeLimitReached.value = session.totalMinutes >= effectiveLimit
                        _extensionRequested.value = session.extensionRequested
                    }
                }
        }
    }

    fun stopTracking() {
        val userId = accountManager.getCurrentUserId() ?: return
        viewModelScope.launch {
            screenTimeManager.endSession(userId)
        }
    }

    fun requestMoreTime() {
        val userId = accountManager.getCurrentUserId() ?: return
        viewModelScope.launch {
            screenTimeManager.requestExtension(userId)
            _extensionRequested.value = true
        }
    }
}
