package com.kidsrec.chatbot.ui.favorites

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kidsrec.chatbot.data.model.Favorite
import com.kidsrec.chatbot.data.model.RecommendationType
import com.kidsrec.chatbot.data.repository.AuthRepository
import com.kidsrec.chatbot.data.repository.FavoritesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class FavoritesViewModel @Inject constructor(
    private val favoritesRepository: FavoritesRepository,
    private val authRepository: AuthRepository
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
            val userId = authRepository.getCurrentUserId() ?: return@launch
            _isLoading.value = true

            favoritesRepository.getFavoritesFlow(userId).collect { favorites ->
                _favorites.value = favorites
                _isLoading.value = false
            }
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
            val userId = authRepository.getCurrentUserId() ?: return@launch
            favoritesRepository.addFavorite(userId, itemId, type, title, description, imageUrl)
        }
    }

    fun removeFavorite(itemId: String) {
        viewModelScope.launch {
            val userId = authRepository.getCurrentUserId() ?: return@launch
            favoritesRepository.removeFavorite(userId, itemId)
        }
    }

    suspend fun isFavorite(itemId: String): Boolean {
        val userId = authRepository.getCurrentUserId() ?: return false
        return favoritesRepository.isFavorite(userId, itemId)
    }
}
