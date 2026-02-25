package com.kidsrec.chatbot.ui.curator

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kidsrec.chatbot.data.repository.BookDataManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class CuratorState {
    object Idle : CuratorState()
    object Loading : CuratorState()
    data class Success(val count: Int) : CuratorState()
    data class Error(val message: String) : CuratorState()
}

@HiltViewModel
class CuratorViewModel @Inject constructor(
    private val bookDataManager: BookDataManager
) : ViewModel() {

    private val _state = MutableStateFlow<CuratorState>(CuratorState.Idle)
    val state: StateFlow<CuratorState> = _state.asStateFlow()

    fun curateBooks(text: String) {
        if (text.isBlank()) {
            _state.value = CuratorState.Error("Please paste some text first!")
            return
        }

        viewModelScope.launch {
            _state.value = CuratorState.Loading
            val result = bookDataManager.curateBooksFromText(text)
            result.fold(
                onSuccess = { count ->
                    _state.value = CuratorState.Success(count)
                },
                onFailure = { error ->
                    _state.value = CuratorState.Error(error.message ?: "Unknown error occurred")
                }
            )
        }
    }

    fun resetState() {
        _state.value = CuratorState.Idle
    }
}
