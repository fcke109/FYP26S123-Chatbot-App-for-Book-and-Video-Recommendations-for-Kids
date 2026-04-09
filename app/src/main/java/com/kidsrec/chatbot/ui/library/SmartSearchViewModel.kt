package com.kidsrec.chatbot.ui.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.firestore.FirebaseFirestore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

data class SearchSuggestion(
    val text: String
)

data class SearchUiState(
    val query: String = "",
    val suggestions: List<SearchSuggestion> = emptyList(),
    val expanded: Boolean = false
)

@HiltViewModel
class SmartSearchViewModel @Inject constructor(
    private val firestore: FirebaseFirestore
) : ViewModel() {

    private val _uiState = MutableStateFlow(SearchUiState())
    val uiState: StateFlow<SearchUiState> = _uiState.asStateFlow()

    private var allSuggestions: List<String> = emptyList()

    init {
        loadSuggestions()
    }

    private fun loadSuggestions() {
        viewModelScope.launch {
            try {
                val docs = firestore.collection("searchSuggestions")
                    .limit(100)
                    .get()
                    .await()
                allSuggestions = docs.documents.mapNotNull { it.getString("text") }
            } catch (_: Exception) {
                // Use default suggestions if Firestore collection doesn't exist
                allSuggestions = listOf(
                    "dinosaurs", "fairy tales", "animals", "space", "adventure",
                    "princesses", "robots", "ocean", "jungle", "magic",
                    "science", "history", "nature", "friendship", "mystery"
                )
            }
        }
    }

    fun onQueryChange(query: String) {
        val filtered = if (query.isBlank()) {
            emptyList()
        } else {
            allSuggestions
                .filter { it.startsWith(query, ignoreCase = true) }
                .take(5)
                .map { SearchSuggestion(text = it) }
        }
        _uiState.value = _uiState.value.copy(
            query = query,
            suggestions = filtered,
            expanded = filtered.isNotEmpty()
        )
    }

    fun onSuggestionClick(suggestion: String) {
        _uiState.value = _uiState.value.copy(
            query = suggestion,
            suggestions = emptyList(),
            expanded = false
        )
    }

    fun onSearch() {
        _uiState.value = _uiState.value.copy(
            expanded = false
        )
    }
}
