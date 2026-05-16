package com.kidsrec.chatbot.data.repository

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

// Tracks recently clicked books/videos/topics for recommendation and personalization features
@Singleton
class InteractionManager @Inject constructor() {

    companion object {
        // Maximum number of recent clicked items stored
        private const val MAX_CLICKED_ITEMS = 10
    }

    // Stores clicked item titles in memory
    private val _clickedItems = MutableStateFlow<List<String>>(emptyList())
    val clickedItems: StateFlow<List<String>> = _clickedItems.asStateFlow()

    // Adds a new clicked item into recent history
    fun addClickedItem(title: String) {

        // Remove unnecessary spaces
        val trimmedTitle = title.trim()

        // Ignore empty titles
        if (trimmedTitle.isBlank()) return

        val updated = _clickedItems.value.toMutableList()

        // Prevent duplicate consecutive clicks
        if (updated.lastOrNull()?.equals(trimmedTitle, ignoreCase = true) == true) {
            return
        }

        // Add latest clicked item
        updated.add(trimmedTitle)

        // Keep only the latest MAX_CLICKED_ITEMS items
        while (updated.size > MAX_CLICKED_ITEMS) {
            updated.removeAt(0)
        }

        // Update flow value
        _clickedItems.value = updated
    }

    // Returns current clicked items list
    fun getClickedItems(): List<String> {
        return _clickedItems.value
    }

    // Removes all stored clicked items
    fun clearClickedItems() {
        _clickedItems.value = emptyList()
    }
}