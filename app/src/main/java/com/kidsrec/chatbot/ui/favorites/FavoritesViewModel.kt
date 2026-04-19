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

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _isGuest = MutableStateFlow(false)
    val isGuest: StateFlow<Boolean> = _isGuest.asStateFlow()

    private val _isFreePlan = MutableStateFlow(false)
    val isFreePlan: StateFlow<Boolean> = _isFreePlan.asStateFlow()

    val bookFavoritesCount: StateFlow<Int> =
        _favorites.map { favs -> favs.count { it.type == RecommendationType.BOOK } }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val videoFavoritesCount: StateFlow<Int> =
        _favorites.map { favs -> favs.count { it.type == RecommendationType.VIDEO } }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private var currentListeningUserId: String? = null
    private var favoritesJob: Job? = null

    init {
        loadFavorites()
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

                if (userId == null) {
                    Log.w(TEST_TAG, "No logged in user. Clearing favorites list.")
                    _favorites.value = emptyList()
                    _isLoading.value = false
                    currentListeningUserId = null
                    favoritesJob?.cancel()
                    return@launch
                }

                if (userId == currentListeningUserId && favoritesJob?.isActive == true) {
                    Log.d(TEST_TAG, "Already listening for this user: $userId")
                    _isLoading.value = false
                    return@launch
                }

                val user = accountManager.getUser(userId)
                Log.d(TEST_TAG, "Loaded user for favorites. isGuest=${user?.isGuest} plan=${user?.planType}")

                // Free users can still favorite (within limits), so we continue loading.
                _isGuest.value = user?.isGuest == true
                _isFreePlan.value = user?.planType == PlanType.FREE

                favoritesJob?.cancel()
                currentListeningUserId = userId

                Log.d(TEST_TAG, "Starting favorites listener for userId=$userId")

                favoritesJob = viewModelScope.launch {
                    favoritesManager.getFavoritesFlow(userId)
                        .onEach { items ->
                            Log.d(TEST_TAG, "Favorites flow emitted ${items.size} items")
                            _isLoading.value = false
                            _errorMessage.value = null
                        }
                        .catch { e ->
                            Log.e(TAG, "Permission denied or load failed", e)
                            Log.e(TEST_TAG, "Favorites flow failed: ${e.message}", e)
                            _favorites.value = emptyList()
                            _isLoading.value = false
                            _errorMessage.value = e.message ?: "Failed to load favorites."
                        }
                        .collect { items ->
                            Log.d(TEST_TAG, "Collected favorites count=${items.size}")
                            _favorites.value = items
                        }
                }
            } catch (e: Exception) {
                Log.e(TAG, "loadFavorites failed", e)
                Log.e(TEST_TAG, "loadFavorites exception: ${e.message}", e)
                _favorites.value = emptyList()
                _isLoading.value = false
                _errorMessage.value = e.message ?: "Failed to load favorites."
            }
        }
    }

    fun refreshFavorites() {
        Log.d(TEST_TAG, "refreshFavorites called")
        currentListeningUserId = null
        favoritesJob?.cancel()
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
        if (_isFreePlan.value) {
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
                Log.d(
                    TEST_TAG,
                    "addFavorite called with userId=$userId, itemId=$itemId, type=$type, title=$title"
                )

                if (userId.isNullOrBlank()) {
                    Log.e(TEST_TAG, "addFavorite failed: userId is null or blank")
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

                Log.d(TEST_TAG, "addFavorite result success=${result.isSuccess}")

                if (result.isFailure) {
                    Log.e(TAG, "Failed to add favorite", result.exceptionOrNull())
                    Log.e(TEST_TAG, "Failed to add favorite", result.exceptionOrNull())
                    _errorMessage.value =
                        result.exceptionOrNull()?.message ?: "Failed to add favorite."
                } else {
                    Log.d(TEST_TAG, "Favorite added successfully for itemId=$itemId")
                    _errorMessage.value = null
                }
            } catch (e: Exception) {
                Log.e(TAG, "addFavorite exception", e)
                Log.e(TEST_TAG, "addFavorite exception: ${e.message}", e)
                _errorMessage.value = e.message ?: "Failed to add favorite."
            }
        }
    }

    fun removeFavorite(itemId: String) {
        viewModelScope.launch {
            try {
                val userId = accountManager.getCurrentUserId()
                Log.d(TEST_TAG, "removeFavorite called with userId=$userId, itemId=$itemId")

                if (userId.isNullOrBlank()) {
                    Log.e(TEST_TAG, "removeFavorite failed: userId is null or blank")
                    _errorMessage.value = "No logged in user."
                    return@launch
                }

                val result = favoritesManager.removeFavorite(userId, itemId)
                Log.d(TEST_TAG, "removeFavorite result success=${result.isSuccess}")

                if (result.isFailure) {
                    Log.e(TAG, "Failed to remove favorite", result.exceptionOrNull())
                    Log.e(TEST_TAG, "Failed to remove favorite", result.exceptionOrNull())
                    _errorMessage.value =
                        result.exceptionOrNull()?.message ?: "Failed to remove favorite."
                } else {
                    Log.d(TEST_TAG, "Favorite removed successfully for itemId=$itemId")
                    _errorMessage.value = null
                }
            } catch (e: Exception) {
                Log.e(TAG, "removeFavorite exception", e)
                Log.e(TEST_TAG, "removeFavorite exception: ${e.message}", e)
                _errorMessage.value = e.message ?: "Failed to remove favorite."
            }
        }
    }
}