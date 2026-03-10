package com.kidsrec.chatbot.ui.curator

import androidx.lifecycle.ViewModel
import com.kidsrec.chatbot.data.repository.BookDataManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

sealed class CuratorState {
    object Idle : CuratorState()
    data class Error(val message: String) : CuratorState()
}

@HiltViewModel
class CuratorViewModel @Inject constructor(
    private val bookDataManager: BookDataManager
) : ViewModel() {

    private val _state = MutableStateFlow<CuratorState>(CuratorState.Idle)
    val state: StateFlow<CuratorState> = _state.asStateFlow()

    fun showManualCurationMessage() {
        _state.value = CuratorState.Error("Please use the Admin Search to add books manually.")
    }

    fun resetState() {
        _state.value = CuratorState.Idle
    }
}
