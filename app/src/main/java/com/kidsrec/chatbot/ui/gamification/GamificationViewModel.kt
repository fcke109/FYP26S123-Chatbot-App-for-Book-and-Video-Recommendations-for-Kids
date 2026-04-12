package com.kidsrec.chatbot.ui.gamification

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kidsrec.chatbot.data.model.BadgeUnlock
import com.kidsrec.chatbot.data.model.GamificationProfile
import com.kidsrec.chatbot.data.repository.GamificationManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class GamificationViewModel @Inject constructor(
    private val gamificationManager: GamificationManager
) : ViewModel() {

    private val _profile = MutableStateFlow(GamificationProfile())
    val profile: StateFlow<GamificationProfile> = _profile.asStateFlow()

    private val _badges = MutableStateFlow<List<BadgeUnlock>>(emptyList())
    val badges: StateFlow<List<BadgeUnlock>> = _badges.asStateFlow()

    private val _celebration = MutableStateFlow(RewardCelebration())
    val celebration: StateFlow<RewardCelebration> = _celebration.asStateFlow()

    private var hasStarted = false
    private var previousLevel = 1
    private var previousBadgeIds: Set<String> = emptySet()

    fun observeChildGamification(childUserId: String) {
        if (hasStarted) return
        hasStarted = true

        viewModelScope.launch {
            gamificationManager.refreshGamification(childUserId)
            gamificationManager.getGamificationProfileFlow(childUserId).collect { profile ->
                val oldLevel = _profile.value.currentLevel
                _profile.value = profile

                if (oldLevel > 0 && profile.currentLevel > oldLevel) {
                    _celebration.value = RewardCelebration(
                        type = RewardCelebrationType.LEVEL_UP,
                        title = "🏆 Level Up!",
                        message = "You reached Level ${profile.currentLevel}",
                        subtitle = "Amazing job learning and exploring!"
                    )
                }

                previousLevel = profile.currentLevel
            }
        }

        viewModelScope.launch {
            gamificationManager.getUnlockedBadgesFlow(childUserId).collect { badges ->
                val oldBadgeIds = _badges.value.map { it.badgeId }.toSet()
                _badges.value = badges

                val newBadge = badges.firstOrNull { it.badgeId !in oldBadgeIds }
                if (oldBadgeIds.isNotEmpty() && newBadge != null) {
                    _celebration.value = RewardCelebration(
                        type = RewardCelebrationType.BADGE,
                        title = "🎉 Badge Unlocked!",
                        message = newBadge.badgeTitle,
                        subtitle = newBadge.description
                    )
                }

                previousBadgeIds = badges.map { it.badgeId }.toSet()
            }
        }
    }

    fun refresh(childUserId: String) {
        viewModelScope.launch {
            gamificationManager.refreshGamification(childUserId)
        }
    }

    fun clearCelebration() {
        _celebration.value = RewardCelebration()
    }
}