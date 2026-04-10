package com.kidsrec.chatbot.data.repository

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class InteractionManager @Inject constructor() {

    companion object {
        private const val MAX_CLICKED_ITEMS = 10
    }

    private val _clickedItems = MutableStateFlow<List<String>>(emptyList())
    val clickedItems: StateFlow<List<String>> = _clickedItems.asStateFlow()

    fun addClickedItem(title: String) {
        val trimmedTitle = title.trim()
        if (trimmedTitle.isBlank()) return

        val updated = _clickedItems.value.toMutableList()

        // Avoid duplicate consecutive clicks
        if (updated.lastOrNull()?.equals(trimmedTitle, ignoreCase = true) == true) {
            return
        }

        updated.add(trimmedTitle)

        while (updated.size > MAX_CLICKED_ITEMS) {
            updated.removeAt(0)
        }

        _clickedItems.value = updated
    }

    fun getClickedItems(): List<String> {
        return _clickedItems.value
    }

    fun clearClickedItems() {
        _clickedItems.value = emptyList()
    }
}