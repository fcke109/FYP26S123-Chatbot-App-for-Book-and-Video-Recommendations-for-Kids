package com.kidsrec.chatbot.ui.favorites

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kidsrec.chatbot.data.model.Favorite
import com.kidsrec.chatbot.data.model.PlanType
import com.kidsrec.chatbot.data.model.RecommendationType
import com.kidsrec.chatbot.data.repository.AccountManager
import com.kidsrec.chatbot.data.repository.FavoritesManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
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

    private val _isFreeChild = MutableStateFlow(false)
    val isFreeChild: StateFlow<Boolean> = _isFreeChild.asStateFlow()

    // Prevents free-plan quota check before Firestore user plan has loaded
    private val _userPlanLoaded = MutableStateFlow(false)

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
                _isFreeChild.value = false
                _isGuest.value = false
                _userPlanLoaded.value = true
                return@launch
            }

            accountManager.getUserFlow(userId)
                .catch { e ->
                    Log.e(TAG, "User plan observer failed", e)
                    _isFreeChild.value = false
                    _userPlanLoaded.value = true
                }
                .collect { user ->
                    if (user != null) {
                        _isFreeChild.value =
                            user.planType == PlanType.FREE &&
                                    user.accountType.name.equals("CHILD", ignoreCase = true)

                        _isGuest.value = user.isGuest
                    } else {
                        _isFreeChild.value = false
                        _isGuest.value = false
                    }

                    _userPlanLoaded.value = true

                    Log.d(
                        TEST_TAG,
                        "Plan loaded. isFreeChild=${_isFreeChild.value}, isGuest=${_isGuest.value}"
                    )
                }
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
        // Apply limit only after the plan has loaded and only for FREE CHILD users
        if (_userPlanLoaded.value && _isFreeChild.value) {
            val alreadyFavorited = _favorites.value.any { it.itemId == itemId }

            if (!alreadyFavorited) {
                val currentBooks = _favorites.value.count { it.type == RecommendationType.BOOK }
                val currentVideos = _favorites.value.count { it.type == RecommendationType.VIDEO }

                if (type == RecommendationType.BOOK && currentBooks >= FREE_BOOK_LIMIT) {
                    _errorMessage.value =
                        "Free plan allows up to $FREE_BOOK_LIMIT favorite books. Upgrade to Premium for unlimited favorites."
                    return
                }

                if (type == RecommendationType.VIDEO && currentVideos >= FREE_VIDEO_LIMIT) {
                    _errorMessage.value =
                        "Free plan allows up to $FREE_VIDEO_LIMIT favorite videos. Upgrade to Premium for unlimited favorites."
                    return
                }
            }
        }

        viewModelScope.launch {
            try {
                val userId = accountManager.getCurrentUserId()

                if (userId.isNullOrBlank()) {
                    _errorMessage.value = "No logged in user."
                    return@launch
                }

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
                }

            } catch (e: Exception) {
                Log.e(TAG, "addFavorite exception", e)
                _errorMessage.value = e.message ?: "Failed to add favorite."
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