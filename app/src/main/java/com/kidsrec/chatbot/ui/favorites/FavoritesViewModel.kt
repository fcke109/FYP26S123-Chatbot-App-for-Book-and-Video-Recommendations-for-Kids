package com.kidsrec.chatbot.ui.favorites

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kidsrec.chatbot.data.model.AccountType
import com.kidsrec.chatbot.data.model.Favorite
import com.kidsrec.chatbot.data.model.PlanType
import com.kidsrec.chatbot.data.model.RecommendationType
import com.kidsrec.chatbot.data.repository.AccountManager
import com.kidsrec.chatbot.data.repository.FavoritesManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeoutOrNull
import javax.inject.Inject

enum class FavoriteFilter { ALL, BOOKS, VIDEOS }

@HiltViewModel
class FavoritesViewModel @Inject constructor(
    private val favoritesManager: FavoritesManager,
    private val accountManager: AccountManager
) : ViewModel() {

    companion object {
        private const val TAG = "FavoritesVM"
        private const val TEST_TAG = "FAV_TEST"

        const val FREE_BOOK_LIMIT = 2
        const val FREE_VIDEO_LIMIT = 2

        private const val PLAN_LOAD_TIMEOUT_MS = 8_000L
    }

    private val _favorites = MutableStateFlow<List<Favorite>>(emptyList())
    val favorites: StateFlow<List<Favorite>> = _favorites.asStateFlow()

    private val _selectedFilter = MutableStateFlow(FavoriteFilter.ALL)
    val selectedFilter: StateFlow<FavoriteFilter> = _selectedFilter.asStateFlow()

    val filteredFavorites: StateFlow<List<Favorite>> =
        combine(_favorites, _selectedFilter) { favs, filter ->
            when (filter) {
                FavoriteFilter.ALL -> favs
                FavoriteFilter.BOOKS -> favs.filter { it.type == RecommendationType.BOOK }
                FavoriteFilter.VIDEOS -> favs.filter { it.type == RecommendationType.VIDEO }
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val totalFavoritesCount: StateFlow<Int> =
        _favorites.map { it.size }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val bookFavoritesCount: StateFlow<Int> =
        _favorites.map { favs -> favs.count { it.type == RecommendationType.BOOK } }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val videoFavoritesCount: StateFlow<Int> =
        _favorites.map { favs -> favs.count { it.type == RecommendationType.VIDEO } }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _isGuest = MutableStateFlow(false)
    val isGuest: StateFlow<Boolean> = _isGuest.asStateFlow()

    // Premium/Admin = unlimited favorites.
    // Free users = limited to 2 books + 2 videos.
    private val _hasUnlimitedFavorites = MutableStateFlow(false)

    private val _userPlanLoaded = MutableStateFlow(false)

    // Keeps the existing UI variable name.
    // true means show free-plan favorite banner.
    val isFreeChild: StateFlow<Boolean> =
        combine(_userPlanLoaded, _hasUnlimitedFavorites) { loaded, unlimited ->
            loaded && !unlimited
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    // Tracks favorite writes that are currently in progress.
    // This prevents fast double tapping from bypassing the free limit.
    private val pendingAdds = MutableStateFlow<Map<String, RecommendationType>>(emptyMap())
    private val addMutex = Mutex()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private var currentListeningUserId: String? = null
    private var favoritesJob: Job? = null
    private var userObserverJob: Job? = null

    init {
        observeUserPlan()
        loadFavorites()
    }

    private fun observeUserPlan() {
        userObserverJob?.cancel()

        userObserverJob = viewModelScope.launch {
            val userId = accountManager.getCurrentUserId()

            if (userId.isNullOrBlank()) {
                _isGuest.value = false
                _hasUnlimitedFavorites.value = false
                _userPlanLoaded.value = true
                return@launch
            }

            val timeoutJob = launch {
                delay(PLAN_LOAD_TIMEOUT_MS)
                if (!_userPlanLoaded.value) {
                    Log.w(TAG, "User plan never loaded within timeout — defaulting to limited")
                    _hasUnlimitedFavorites.value = false
                    _userPlanLoaded.value = true
                }
            }

            accountManager.getUserFlow(userId)
                .catch { e ->
                    Log.e(TAG, "User plan observer failed", e)
                    _hasUnlimitedFavorites.value = false
                    _userPlanLoaded.value = true
                }
                .collect { user ->
                    if (user != null) {
                        val effectivePlan = getEffectivePlanForFavorites(user)

                        _hasUnlimitedFavorites.value = hasPremiumFavorites(effectivePlan)
                        _isGuest.value = user.isGuest
                        _userPlanLoaded.value = true
                        timeoutJob.cancel()

                        Log.d(
                            TEST_TAG,
                            "Plan loaded. accountType=${user.accountType}, userPlan=${user.planType}, parentId=${user.parentId}, effectivePlan=$effectivePlan, hasUnlimitedFavorites=${_hasUnlimitedFavorites.value}"
                        )
                    }
                }
        }
    }

    // Decides the plan used for favorite limits.
    // Child accounts inherit premium/admin only if the parent is premium/admin.
    private suspend fun getEffectivePlanForFavorites(
        user: com.kidsrec.chatbot.data.model.User
    ): PlanType {
        var effectivePlan = user.planType

        if (
            user.accountType == AccountType.CHILD &&
            !user.parentId.isNullOrBlank()
        ) {
            try {
                val parentUser = accountManager.getUser(user.parentId)

                if (
                    parentUser?.planType == PlanType.PREMIUM ||
                    parentUser?.planType == PlanType.ADMIN
                ) {
                    effectivePlan = parentUser.planType
                }

            } catch (e: Exception) {
                Log.e(TAG, "Failed to load parent plan", e)
            }
        }

        return effectivePlan
    }

    // Premium/Admin users skip the free favorite limit.
    private fun hasPremiumFavorites(plan: PlanType): Boolean {
        return plan == PlanType.PREMIUM || plan == PlanType.ADMIN
    }

    // Free favorite rule:
    // max 2 books and max 2 videos.
    private fun canAddFreeFavorite(type: RecommendationType): Boolean {
        val pending = pendingAdds.value.values

        val currentBooks =
            _favorites.value.count { it.type == RecommendationType.BOOK } +
                    pending.count { it == RecommendationType.BOOK }

        val currentVideos =
            _favorites.value.count { it.type == RecommendationType.VIDEO } +
                    pending.count { it == RecommendationType.VIDEO }

        return when (type) {
            RecommendationType.BOOK -> currentBooks < FREE_BOOK_LIMIT
            RecommendationType.VIDEO -> currentVideos < FREE_VIDEO_LIMIT
        }
    }

    // Shows a clear message when free favorite limit is reached.
    private fun setFreeFavoriteError(type: RecommendationType) {
        _errorMessage.value = when (type) {
            RecommendationType.BOOK ->
                "Free plan allows up to $FREE_BOOK_LIMIT favorite books. Upgrade to Premium for unlimited favorites."

            RecommendationType.VIDEO ->
                "Free plan allows up to $FREE_VIDEO_LIMIT favorite videos. Upgrade to Premium for unlimited favorites."
        }
    }

    fun setFilter(filter: FavoriteFilter) {
        _selectedFilter.value = filter
    }

    fun clearError() {
        _errorMessage.value = null
    }

    fun loadFavorites() {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null

            try {
                val userId = accountManager.getCurrentUserId()
                Log.d(TEST_TAG, "loadFavorites called. currentUserId=$userId")

                if (userId.isNullOrBlank()) {
                    _favorites.value = emptyList()
                    _isLoading.value = false
                    currentListeningUserId = null
                    favoritesJob?.cancel()
                    return@launch
                }

                if (userId == currentListeningUserId && favoritesJob?.isActive == true) {
                    _isLoading.value = false
                    return@launch
                }

                favoritesJob?.cancel()
                currentListeningUserId = userId

                favoritesJob = viewModelScope.launch {
                    favoritesManager.getFavoritesFlow(userId)
                        .onEach { items ->
                            Log.d(TEST_TAG, "Favorites flow emitted ${items.size} items")
                            _isLoading.value = false
                            _errorMessage.value = null
                        }
                        .catch { e ->
                            Log.e(TAG, "Favorites flow failed", e)
                            _favorites.value = emptyList()
                            _isLoading.value = false
                            _errorMessage.value = e.message ?: "Failed to load favorites."
                        }
                        .collect { items ->
                            _favorites.value = items
                        }
                }

            } catch (e: Exception) {
                Log.e(TAG, "loadFavorites failed", e)
                _favorites.value = emptyList()
                _isLoading.value = false
                _errorMessage.value = e.message ?: "Failed to load favorites."
            }
        }
    }

    fun refreshFavorites() {
        currentListeningUserId = null
        favoritesJob?.cancel()
        observeUserPlan()
        loadFavorites()
    }

    fun addFavorite(
        itemId: String,
        type: RecommendationType,
        title: String,
        description: String,
        imageUrl: String,
        url: String = ""
    ) {
        viewModelScope.launch {
            addMutex.withLock {
                _userPlanLoaded.first { it }

                val alreadyCommitted = _favorites.value.any { it.itemId == itemId }
                val alreadyPending = pendingAdds.value.containsKey(itemId)

                if (alreadyCommitted || alreadyPending) return@withLock

                // FREE logic:
                // if user does not have unlimited favorites,
                // enforce 2 books + 2 videos only.

                // Reserve item before Firestore write to prevent rapid tap bypass.
                pendingAdds.update { it + (itemId to type) }
            }

            try {
                val userId = accountManager.getCurrentUserId()

                if (userId.isNullOrBlank()) {
                    _errorMessage.value = "No logged in user."
                    pendingAdds.update { it - itemId }
                    return@launch
                }

                if (!pendingAdds.value.containsKey(itemId)) return@launch

                val result = favoritesManager.addFavorite(
                    userId = userId,
                    itemId = itemId,
                    type = type,
                    title = title,
                    description = description,
                    imageUrl = imageUrl,
                    url = url
                )

                if (result.isFailure) {
                    Log.e(TAG, "Failed to add favorite", result.exceptionOrNull())
                    _errorMessage.value =
                        result.exceptionOrNull()?.message ?: "Failed to add favorite."
                } else {
                    Log.d(TEST_TAG, "Favorite added successfully: $itemId")
                    _errorMessage.value = null

                    withTimeoutOrNull(5_000) {
                        _favorites.first { favs -> favs.any { it.itemId == itemId } }
                    }
                }

            } catch (e: Exception) {
                Log.e(TAG, "addFavorite exception", e)
                _errorMessage.value = e.message ?: "Failed to add favorite."
            } finally {
                pendingAdds.update { it - itemId }
            }
        }
    }

    fun removeFavorite(itemId: String) {
        viewModelScope.launch {
            try {
                val userId = accountManager.getCurrentUserId()

                if (userId.isNullOrBlank()) {
                    _errorMessage.value = "No logged in user."
                    return@launch
                }

                val result = favoritesManager.removeFavorite(userId, itemId)

                if (result.isFailure) {
                    Log.e(TAG, "Failed to remove favorite", result.exceptionOrNull())
                    _errorMessage.value =
                        result.exceptionOrNull()?.message ?: "Failed to remove favorite."
                } else {
                    Log.d(TEST_TAG, "Favorite removed successfully: $itemId")
                    _errorMessage.value = null
                }

            } catch (e: Exception) {
                Log.e(TAG, "removeFavorite exception", e)
                _errorMessage.value = e.message ?: "Failed to remove favorite."
            }
        }
    }
}