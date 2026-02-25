package com.kidsrec.chatbot.ui.favorites

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

@HiltViewModel
class FavoritesViewModel @Inject constructor(
    private val favoritesManager: FavoritesManager,
    private val accountManager: AccountManager
) : ViewModel() {

    private val _favorites = MutableStateFlow<List<Favorite>>(emptyList())
    val favorites: StateFlow<List<Favorite>> = _favorites.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    init {
        loadFavorites()
    }

    private fun loadFavorites() {
        viewModelScope.launch {
            val userId = accountManager.getCurrentUserId() ?: return@launch
            _isLoading.value = true
            favoritesManager.getFavoritesFlow(userId)
                .onEach { _isLoading.value = false }
                .collect { items -> _favorites.value = items }
        }
    }

    fun addFavorite(
        itemId: String,
        type: RecommendationType,
        title: String,
        description: String,
        imageUrl: String
    ) {
        viewModelScope.launch {
            val userId = accountManager.getCurrentUserId() ?: return@launch
            favoritesManager.addFavorite(userId, itemId, type, title, description, imageUrl)
        }
    }

    fun removeFavorite(itemId: String) {
        viewModelScope.launch {
            val userId = accountManager.getCurrentUserId() ?: return@launch
            favoritesManager.removeFavorite(userId, itemId)
        }
    }
}
