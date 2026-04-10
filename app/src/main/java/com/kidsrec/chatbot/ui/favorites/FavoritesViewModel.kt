package com.kidsrec.chatbot.ui.favorites

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kidsrec.chatbot.data.model.Favorite
import com.kidsrec.chatbot.data.model.RecommendationType
import com.kidsrec.chatbot.data.repository.AccountManager
import com.kidsrec.chatbot.data.repository.FavoritesManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class FavoriteFilter { ALL, BOOKS, VIDEOS }

@HiltViewModel
class FavoritesViewModel @Inject constructor(
    private val favoritesManager: FavoritesManager,
    private val accountManager: AccountManager
) : ViewModel() {

    private val _favorites = MutableStateFlow<List<Favorite>>(emptyList())
    val favorites: StateFlow<List<Favorite>> = _favorites.asStateFlow()

    private val _selectedFilter = MutableStateFlow(FavoriteFilter.ALL)
    val selectedFilter: StateFlow<FavoriteFilter> = _selectedFilter.asStateFlow()

    val filteredFavorites: StateFlow<List<Favorite>> = combine(_favorites, _selectedFilter) { favs, filter ->
        when (filter) {
            FavoriteFilter.ALL -> favs
            FavoriteFilter.BOOKS -> favs.filter { it.type == RecommendationType.BOOK }
            FavoriteFilter.VIDEOS -> favs.filter { it.type == RecommendationType.VIDEO }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val totalFavoritesCount: StateFlow<Int> = _favorites.map { it.size }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _isGuest = MutableStateFlow(false)
    val isGuest: StateFlow<Boolean> = _isGuest.asStateFlow()

    fun setFilter(filter: FavoriteFilter) {
        _selectedFilter.value = filter
    }

    private var currentListeningUserId: String? = null
    private var favoritesJob: kotlinx.coroutines.Job? = null

    init {
        loadFavorites()
    }

    fun loadFavorites() {
        viewModelScope.launch {
            val userId = accountManager.getCurrentUserId()
            if (userId == null) {
                _favorites.value = emptyList()
                _isLoading.value = false
                return@launch
            }

            // Already listening for this user
            if (userId == currentListeningUserId) return@launch

            // Check if guest
            val user = accountManager.getUser(userId)
            if (user?.isGuest == true) {
                _isGuest.value = true
                _isLoading.value = false
                return@launch
            }

            _isGuest.value = false

            // Cancel previous listener and start a new one
            favoritesJob?.cancel()
            currentListeningUserId = userId

            _isLoading.value = true
            favoritesJob = viewModelScope.launch {
                favoritesManager.getFavoritesFlow(userId)
                    .onEach { _isLoading.value = false }
                    .catch { e ->
                        Log.e("FavoritesVM", "Permission denied or load failed", e)
                        _isLoading.value = false
                    }
                    .collect { items -> _favorites.value = items }
            }
        }
    }

    fun addFavorite(
        itemId: String,
        type: RecommendationType,
        title: String,
        description: String,
        imageUrl: String,
        url: String = ""
    ) {
        if (_isGuest.value) return
        viewModelScope.launch {
            val userId = accountManager.getCurrentUserId() ?: return@launch
            val result = favoritesManager.addFavorite(userId, itemId, type, title, description, imageUrl, url)
            if (result.isFailure) {
                Log.e("FavoritesVM", "Failed to add favorite", result.exceptionOrNull())
            }
        }
    }

    fun removeFavorite(itemId: String) {
        if (_isGuest.value) return
        viewModelScope.launch {
            val userId = accountManager.getCurrentUserId() ?: return@launch
            val result = favoritesManager.removeFavorite(userId, itemId)
            if (result.isFailure) {
                Log.e("FavoritesVM", "Failed to remove favorite", result.exceptionOrNull())
            }
        }
    }
}
