package com.kidsrec.chatbot.ui.library

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SmartSearchBar(
    uiState: SearchUiState,
    onQueryChange: (String) -> Unit,
    onSearch: () -> Unit,
    onSuggestionClick: (String) -> Unit
) {
    Column {
        OutlinedTextField(
            value = uiState.query,
            onValueChange = onQueryChange,
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("Search books...") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search") },
            singleLine = true
        )

        if (uiState.expanded && uiState.suggestions.isNotEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                LazyColumn(
                    modifier = Modifier.heightIn(max = 200.dp)
                ) {
                    items(uiState.suggestions) { suggestion ->
                        Text(
                            text = suggestion.text,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onSuggestionClick(suggestion.text) }
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
        }
    }
}
