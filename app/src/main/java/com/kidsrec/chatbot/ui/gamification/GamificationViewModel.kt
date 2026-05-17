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

// ViewModel responsible for managing gamification progress, badges, and reward celebrations
@HiltViewModel
class GamificationViewModel @Inject constructor(
    private val gamificationManager: GamificationManager
) : ViewModel() {

    // Stores the child's current gamification profile, including points and level
    private val _profile = MutableStateFlow(GamificationProfile())
    val profile: StateFlow<GamificationProfile> = _profile.asStateFlow()

    // Stores the list of badges the child has unlocked
    private val _badges = MutableStateFlow<List<BadgeUnlock>>(emptyList())
    val badges: StateFlow<List<BadgeUnlock>> = _badges.asStateFlow()

    // Stores the current reward celebration shown when a badge or level-up is achieved
    private val _celebration = MutableStateFlow(RewardCelebration())
    val celebration: StateFlow<RewardCelebration> = _celebration.asStateFlow()

    // Prevents the gamification listeners from being started multiple times
    private var hasStarted = false

    // Stores the previous level for comparison when detecting level-ups
    private var previousLevel = 1

    // Stores previously unlocked badge IDs for comparison when detecting new badges
    private var previousBadgeIds: Set<String> = emptySet()

    // Starts observing the child's gamification profile and unlocked badges
    fun observeChildGamification(childUserId: String) {
        // Avoids creating duplicate Firestore listeners for the same ViewModel
        if (hasStarted) return
        hasStarted = true

        // Observes the child's gamification profile and checks for level-up events
        viewModelScope.launch {
            // Refreshes gamification before collecting profile updates
            gamificationManager.refreshGamification(childUserId)
            gamificationManager.getGamificationProfileFlow(childUserId).collect { profile ->
                val oldLevel = _profile.value.currentLevel

                // Updates profile state for the UI
                _profile.value = profile

                // Shows a level-up celebration if the new level is higher than the previous level
                if (oldLevel > 0 && profile.currentLevel > oldLevel) {
                    _celebration.value = RewardCelebration(
                        type = RewardCelebrationType.LEVEL_UP,
                        title = "🏆 Level Up!",
                        message = "You reached Level ${profile.currentLevel}",
                        subtitle = "Amazing job learning and exploring!"
                    )
                }

                // Saves the latest level for future comparison
                previousLevel = profile.currentLevel
            }
        }

        // Observes unlocked badges and checks whether a new badge was earned
        viewModelScope.launch {
            gamificationManager.getUnlockedBadgesFlow(childUserId).collect { badges ->
                val oldBadgeIds = _badges.value.map { it.badgeId }.toSet()

                // Updates badge list state for the UI
                _badges.value = badges

                // Finds the first badge that was not previously unlocked
                val newBadge = badges.firstOrNull { it.badgeId !in oldBadgeIds }

                // Shows a badge celebration only after there were already existing badge records
                if (oldBadgeIds.isNotEmpty() && newBadge != null) {
                    _celebration.value = RewardCelebration(
                        type = RewardCelebrationType.BADGE,
                        title = "🎉 Badge Unlocked!",
                        message = newBadge.badgeTitle,
                        subtitle = newBadge.description
                    )
                }

                // Saves the latest badge IDs for future comparison
                previousBadgeIds = badges.map { it.badgeId }.toSet()
            }
        }
    }

    // Manually refreshes the child's gamification progress, points, level, and badges
    fun refresh(childUserId: String) {
        viewModelScope.launch {
            gamificationManager.refreshGamification(childUserId)
        }
    }

    // Clears the current celebration after it has been displayed
    fun clearCelebration() {
        _celebration.value = RewardCelebration()
    }
}