package com.kidsrec.chatbot.ui.curator

import androidx.lifecycle.ViewModel
import com.kidsrec.chatbot.data.repository.BookDataManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

// Different states used by the Curator screen
sealed class CuratorState {
    object Idle : CuratorState()
    // Error or info message state
    data class Error(val message: String) : CuratorState()
}

// ViewModel for curator/admin screen logic
@HiltViewModel
class CuratorViewModel @Inject constructor(
    private val bookDataManager: BookDataManager
) : ViewModel() {

    private val _state = MutableStateFlow<CuratorState>(CuratorState.Idle)
    val state: StateFlow<CuratorState> = _state.asStateFlow()

    // Shows message directing admin to use search feature
    fun showManualCurationMessage() {
        _state.value = CuratorState.Error("Please use the Admin Search to add books manually.")
    }

    // Resets screen state back to idle
    fun resetState() {
        _state.value = CuratorState.Idle
    }
}
