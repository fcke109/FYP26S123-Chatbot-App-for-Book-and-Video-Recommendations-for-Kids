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

// Represents one search suggestion displayed under the smart search bar
data class SearchSuggestion(
    val text: String // Suggested search keyword or phrase
)

// Stores the current UI state for the smart search feature
data class SearchUiState(
    val query: String = "", // Current text typed into the search field
    val suggestions: List<SearchSuggestion> = emptyList(), // Filtered suggestions shown to the user
    val expanded: Boolean = false // Controls whether the suggestion dropdown is visible
)

// ViewModel responsible for loading and filtering smart search suggestions
@HiltViewModel
class SmartSearchViewModel @Inject constructor(
    private val firestore: FirebaseFirestore
) : ViewModel() {

    // Backing state for the search UI
    private val _uiState = MutableStateFlow(SearchUiState())
    val uiState: StateFlow<SearchUiState> = _uiState.asStateFlow()

    // Stores all available suggestions before filtering based on user input
    private var allSuggestions: List<String> = emptyList()

    // Loads suggestions when the ViewModel is created
    init {
        loadSuggestions()
    }

    // Loads search suggestions from Firestore, with default suggestions as fallback
    private fun loadSuggestions() {
        viewModelScope.launch {
            try {
                val docs = firestore.collection("searchSuggestions")
                    .limit(100)
                    .get()
                    .await()
                // Extracts the suggestion text from each Firestore document
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

    // Updates the search query and filters matching suggestions
    fun onQueryChange(query: String) {
        val filtered = if (query.isBlank()) {
            emptyList()
        } else {
            allSuggestions
                .filter { it.startsWith(query, ignoreCase = true) }
                .take(5)
                .map { SearchSuggestion(text = it) }
        }
        // Updates the UI state with the new query and suggestion dropdown visibility
        _uiState.value = _uiState.value.copy(
            query = query,
            suggestions = filtered,
            expanded = filtered.isNotEmpty()
        )
    }

    // Applies the selected suggestion as the search query
    fun onSuggestionClick(suggestion: String) {
        _uiState.value = _uiState.value.copy(
            query = suggestion,
            suggestions = emptyList(),
            expanded = false
        )
    }

    // Closes the suggestion dropdown when the user performs a search
    fun onSearch() {
        _uiState.value = _uiState.value.copy(
            expanded = false
        )
    }
}
