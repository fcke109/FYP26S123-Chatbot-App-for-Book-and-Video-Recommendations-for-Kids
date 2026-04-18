package com.kidsrec.chatbot.ui.library

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.kidsrec.chatbot.data.model.Book
import com.kidsrec.chatbot.data.model.Favorite
import com.kidsrec.chatbot.data.model.Recommendation
import com.kidsrec.chatbot.data.model.RecommendationType
import com.kidsrec.chatbot.ui.common.AgeUiMode
import com.kidsrec.chatbot.ui.common.getAgeUiMode
import com.kidsrec.chatbot.ui.favorites.FavoritesViewModel

private data class CategoryUi(
    val emoji: String,
    val container: Color,
    val content: Color
)

private fun normalizedCategory(category: String): String {
    return category.trim().ifBlank { "General" }
}

private fun categoryUi(category: String): CategoryUi {
    val name = normalizedCategory(category).lowercase()

    return when {
        name == "all" -> CategoryUi(
            emoji = "✨",
            container = Color(0xFFEDE7F6),
            content = Color(0xFF5E35B1)
        )

        "dino" in name || "dinosaur" in name || "prehistoric" in name -> CategoryUi(
            emoji = "🦖",
            container = Color(0xFFE8F5E9),
            content = Color(0xFF2E7D32)
        )

        "space" in name || "planet" in name || "rocket" in name || "science" in name -> CategoryUi(
            emoji = "🚀",
            container = Color(0xFFE3F2FD),
            content = Color(0xFF1565C0)
        )

        "animal" in name || "dog" in name || "pet" in name || "wildlife" in name -> CategoryUi(
            emoji = "🐶",
            container = Color(0xFFFFF3E0),
            content = Color(0xFFEF6C00)
        )

        "fairy" in name || "princess" in name || "magic" in name -> CategoryUi(
            emoji = "🪄",
            container = Color(0xFFFCE4EC),
            content = Color(0xFFC2185B)
        )

        "adventure" in name || "journey" in name -> CategoryUi(
            emoji = "🗺️",
            container = Color(0xFFF3E5F5),
            content = Color(0xFF7B1FA2)
        )

        "education" in name || "learning" in name || "school" in name -> CategoryUi(
            emoji = "📚",
            container = Color(0xFFE0F2F1),
            content = Color(0xFF00695C)
        )

        else -> CategoryUi(
            emoji = "⭐",
            container = Color(0xFFFFF8E1),
            content = Color(0xFFF9A825)
        )
    }
}

@Composable
fun UserLibraryScreen(
    viewModel: LibraryViewModel,
    favoritesViewModel: FavoritesViewModel,
    searchViewModel: SmartSearchViewModel,
    onOpenRecommendation: (
        url: String,
        title: String,
        isVideo: Boolean,
        itemId: String,
        imageUrl: String,
        description: String
    ) -> Unit
) {
    val books by viewModel.curatedBooks.collectAsState()
    val topPicks by viewModel.topPicks.collectAsState()
    val favoriteItems by favoritesViewModel.favorites.collectAsState()
    val isGuest by favoritesViewModel.isGuest.collectAsState()
    val searchUiState by searchViewModel.uiState.collectAsState()
    val userAge by viewModel.userAge.collectAsState()

    val ageUiMode = getAgeUiMode(userAge)

    var selectedCategory by remember { mutableStateOf("All") }

    val categories = remember(books) {
        listOf("All") + books
            .map { normalizedCategory(it.category) }
            .distinct()
            .sorted()
    }

    val filteredBooksBySearch = if (searchUiState.query.isNotBlank() && !searchUiState.expanded) {
        books.filter {
            it.title.contains(searchUiState.query, ignoreCase = true) ||
                    it.author.contains(searchUiState.query, ignoreCase = true) ||
                    normalizedCategory(it.category).contains(searchUiState.query, ignoreCase = true)
        }
    } else {
        books
    }

    val finalFilteredBooks = if (selectedCategory != "All") {
        filteredBooksBySearch.filter { normalizedCategory(it.category) == selectedCategory }
    } else {
        filteredBooksBySearch
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = when (ageUiMode) {
                AgeUiMode.EARLY_CHILD -> "My Books"
                AgeUiMode.YOUNG_CHILD -> "My Story Library"
                AgeUiMode.OLDER_CHILD -> "Explore Your Library"
            },
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )

        Text(
            text = when (ageUiMode) {
                AgeUiMode.EARLY_CHILD -> "Tap a book to start reading."
                AgeUiMode.YOUNG_CHILD -> "Find fun books and save your favorites."
                AgeUiMode.OLDER_CHILD -> "Browse categories, explore recommendations, and save favorites."
            },
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(16.dp))

        SmartSearchBar(
            uiState = searchUiState,
            onQueryChange = { searchViewModel.onQueryChange(it) },
            onSearch = { searchViewModel.onSearch() },
            onSuggestionClick = { searchViewModel.onSuggestionClick(it) }
        )

        Spacer(modifier = Modifier.height(16.dp))

        if (categories.size > 1) {
            CategorySection(
                categories = categories,
                selectedCategory = selectedCategory,
                onCategorySelected = { selectedCategory = it }
            )
            Spacer(modifier = Modifier.height(16.dp))
        }

        if (books.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text("No books here yet. Check back soon!")
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (topPicks.isNotEmpty() && searchUiState.query.isBlank()) {
                    item {
                        TopPicksSection(
                            title = when (ageUiMode) {
                                AgeUiMode.EARLY_CHILD -> "Fun Picks"
                                AgeUiMode.YOUNG_CHILD -> "Top Picks for You"
                                AgeUiMode.OLDER_CHILD -> "Recommended for You"
                            },
                            picks = topPicks,
                            favoriteItems = favoriteItems,
                            isGuest = isGuest,
                            ageUiMode = ageUiMode,
                            onToggleFavorite = { rec ->
                                val isFav = favoriteItems.any { it.itemId == rec.id }
                                if (isFav) {
                                    favoritesViewModel.removeFavorite(rec.id)
                                } else {
                                    favoritesViewModel.addFavorite(
                                        itemId = rec.id,
                                        type = rec.type,
                                        title = rec.title,
                                        description = rec.description,
                                        imageUrl = rec.imageUrl,
                                        url = rec.url
                                    )
                                }
                            },
                            onPickClick = { rec ->
                                viewModel.addClickedItem(rec.title)
                                if (rec.url.isNotBlank()) {
                                    onOpenRecommendation(
                                        rec.url,
                                        rec.title,
                                        rec.type == RecommendationType.VIDEO,
                                        rec.id,
                                        rec.imageUrl,
                                        rec.description
                                    )
                                }
                            }
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                }

                item {
                    Text(
                        text = when {
                            searchUiState.query.isNotBlank() && selectedCategory != "All" ->
                                "Search Results • $selectedCategory"
                            searchUiState.query.isNotBlank() ->
                                "Search Results"
                            selectedCategory != "All" ->
                                "$selectedCategory Books"
                            else ->
                                when (ageUiMode) {
                                    AgeUiMode.EARLY_CHILD -> "Books"
                                    AgeUiMode.YOUNG_CHILD -> "All Books"
                                    AgeUiMode.OLDER_CHILD -> "Browse Books"
                                }
                        },
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }

                if (finalFilteredBooks.isEmpty()) {
                    item {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant
                        ) {
                            Text(
                                text = "No books found in this category yet.",
                                modifier = Modifier.padding(16.dp),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                } else if (ageUiMode == AgeUiMode.EARLY_CHILD) {
                    item {
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(2),
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 200.dp, max = 1200.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(finalFilteredBooks) { book ->
                                val isFavorited = favoriteItems.any { it.itemId == book.id }
                                BigBookTile(
                                    book = book,
                                    isFavorited = isFavorited,
                                    showFavoriteButton = !isGuest,
                                    onFavoriteClick = {
                                        if (isFavorited) {
                                            favoritesViewModel.removeFavorite(book.id)
                                        } else {
                                            val url = book.readerUrl.ifBlank { book.bookUrl }
                                            favoritesViewModel.addFavorite(
                                                itemId = book.id,
                                                type = RecommendationType.BOOK,
                                                title = book.title,
                                                description = book.description,
                                                imageUrl = book.coverUrl,
                                                url = url
                                            )
                                        }
                                    },
                                    onClick = {
                                        viewModel.addClickedItem(book.title)
                                        val url = book.readerUrl.ifBlank { book.bookUrl }
                                        if (url.isNotBlank()) {
                                            onOpenRecommendation(
                                                url,
                                                book.title,
                                                false,
                                                book.id,
                                                book.coverUrl,
                                                book.description
                                            )
                                        }
                                    }
                                )
                            }
                        }
                    }
                } else {
                    items(finalFilteredBooks) { book ->
                        val isFavorited = favoriteItems.any { it.itemId == book.id }

                        UserBookCardAdaptive(
                            book = book,
                            isFavorited = isFavorited,
                            showFavoriteButton = !isGuest,
                            ageUiMode = ageUiMode,
                            onFavoriteClick = {
                                if (isFavorited) {
                                    favoritesViewModel.removeFavorite(book.id)
                                } else {
                                    val url = book.readerUrl.ifBlank { book.bookUrl }
                                    favoritesViewModel.addFavorite(
                                        itemId = book.id,
                                        type = RecommendationType.BOOK,
                                        title = book.title,
                                        description = book.description,
                                        imageUrl = book.coverUrl,
                                        url = url
                                    )
                                }
                            },
                            onClick = {
                                viewModel.addClickedItem(book.title)
                                val url = book.readerUrl.ifBlank { book.bookUrl }
                                if (url.isNotBlank()) {
                                    onOpenRecommendation(
                                        url,
                                        book.title,
                                        false,
                                        book.id,
                                        book.coverUrl,
                                        book.description
                                    )
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun getEmoji(category: String): String {
    return when (category.trim().lowercase()) {
        "all" -> "✨"
        "dinosaurs", "dinosaur", "prehistoric" -> "🦖"
        "space", "planet", "rocket", "science" -> "🚀"
        "animals", "animal", "dog", "pet", "wildlife" -> "🐶"
        "adventure", "journey" -> "🗺️"
        "fairy tales", "fairy", "princess", "magic" -> "🪄"
        "education", "learning", "school" -> "📚"
        else -> "✨"
    }
}
@Composable
fun CategorySection(
    categories: List<String>,
    selectedCategory: String,
    onCategorySelected: (String) -> Unit
) {
    Column {
        Text(
            text = "Categories",
            fontWeight = FontWeight.Bold,
            color = Color(0xFF374151),
            fontSize = 16.sp
        )

        Spacer(modifier = Modifier.height(10.dp))

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(categories) { category ->
                val isSelected = selectedCategory == category

                Surface(
                    onClick = { onCategorySelected(category) },
                    shape = RoundedCornerShape(50),
                    color = if (isSelected) Color(0xFF1F2937) else Color.White,
                    border = BorderStroke(
                        1.dp,
                        if (isSelected) Color(0xFF1F2937) else Color(0xFFD1D5DB)
                    ),
                    shadowElevation = if (isSelected) 2.dp else 0.dp
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = getEmoji(category),
                            fontSize = 14.sp
                        )

                        Text(
                            text = category,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (isSelected) Color.White else Color(0xFF374151),
                            letterSpacing = 0.3.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun CategoryBadge(
    category: String,
    modifier: Modifier = Modifier
) {
    val name = normalizedCategory(category)
    val emoji = getEmoji(name)

    Surface(
        modifier = modifier,
        color = Color.White,
        shape = RoundedCornerShape(50),
        border = BorderStroke(1.dp, Color(0xFFD1D5DB)),
        shadowElevation = 0.dp
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = emoji,
                fontSize = 12.sp
            )
            Text(
                text = name,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF374151),
                maxLines = 1
            )
        }
    }
}

@Composable
fun TopPicksSection(
    title: String,
    picks: List<Recommendation>,
    favoriteItems: List<Favorite>,
    isGuest: Boolean = false,
    ageUiMode: AgeUiMode,
    onToggleFavorite: (Recommendation) -> Unit,
    onPickClick: (Recommendation) -> Unit
) {
    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                Icons.Default.AutoAwesome,
                contentDescription = null,
                tint = Color(0xFFFFA000),
                modifier = Modifier.size(if (ageUiMode == AgeUiMode.EARLY_CHILD) 26.dp else 22.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = title,
                fontSize = if (ageUiMode == AgeUiMode.EARLY_CHILD) 20.sp else 18.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            items(picks) { pick ->
                val isFavorited = favoriteItems.any { it.itemId == pick.id }
                TopPickCardAdaptive(
                    pick = pick,
                    isFavorited = isFavorited,
                    showFavoriteButton = !isGuest,
                    ageUiMode = ageUiMode,
                    onFavoriteClick = { onToggleFavorite(pick) },
                    onClick = { onPickClick(pick) }
                )
            }
        }
    }
}

@Composable
fun TopPickCardAdaptive(
    pick: Recommendation,
    isFavorited: Boolean,
    showFavoriteButton: Boolean = true,
    ageUiMode: AgeUiMode,
    onFavoriteClick: () -> Unit,
    onClick: () -> Unit
) {
    val cardWidth = when (ageUiMode) {
        AgeUiMode.EARLY_CHILD -> 190.dp
        AgeUiMode.YOUNG_CHILD -> 170.dp
        AgeUiMode.OLDER_CHILD -> 160.dp
    }

    val imageHeight = when (ageUiMode) {
        AgeUiMode.EARLY_CHILD -> 140.dp
        AgeUiMode.YOUNG_CHILD -> 125.dp
        AgeUiMode.OLDER_CHILD -> 120.dp
    }

    Card(
        modifier = Modifier
            .width(cardWidth)
            .clickable { onClick() },
        shape = RoundedCornerShape(14.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column {
            Box(
                modifier = Modifier
                    .height(imageHeight)
                    .fillMaxWidth()
            ) {
                if (pick.imageUrl.isNotEmpty()) {
                    AsyncImage(
                        model = pick.imageUrl,
                        contentDescription = pick.title,
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(RoundedCornerShape(topStart = 14.dp, topEnd = 14.dp)),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    val isVideo = pick.type == RecommendationType.VIDEO
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(if (isVideo) Color(0xFFFFE5E5) else Color(0xFFE5F0FF)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isVideo) Icons.Default.PlayCircle else Icons.Default.MenuBook,
                            contentDescription = null,
                            modifier = Modifier.size(if (ageUiMode == AgeUiMode.EARLY_CHILD) 48.dp else 40.dp),
                            tint = if (isVideo) Color.Red else MaterialTheme.colorScheme.primary
                        )
                    }
                }

                if (pick.relevanceScore > 0 && ageUiMode != AgeUiMode.EARLY_CHILD) {
                    Surface(
                        modifier = Modifier
                            .padding(6.dp)
                            .align(Alignment.TopEnd),
                        color = Color(0xFF4CAF50),
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            text = "${(pick.relevanceScore * 100).toInt()}% Match",
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            fontSize = 10.sp,
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                if (showFavoriteButton) {
                    IconButton(
                        onClick = onFavoriteClick,
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(4.dp)
                            .size(if (ageUiMode == AgeUiMode.EARLY_CHILD) 38.dp else 32.dp)
                            .background(Color.White.copy(alpha = 0.7f), CircleShape)
                    ) {
                        Icon(
                            imageVector = if (isFavorited) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            contentDescription = "Favorite",
                            tint = if (isFavorited) Color.Red else Color.Gray,
                            modifier = Modifier.size(if (ageUiMode == AgeUiMode.EARLY_CHILD) 22.dp else 18.dp)
                        )
                    }
                }
            }

            Column(modifier = Modifier.padding(10.dp)) {
                Text(
                    text = pick.title,
                    fontWeight = FontWeight.Bold,
                    fontSize = when (ageUiMode) {
                        AgeUiMode.EARLY_CHILD -> 15.sp
                        AgeUiMode.YOUNG_CHILD -> 13.sp
                        AgeUiMode.OLDER_CHILD -> 12.sp
                    },
                    maxLines = if (ageUiMode == AgeUiMode.EARLY_CHILD) 2 else 1,
                    overflow = TextOverflow.Ellipsis
                )

                if (ageUiMode != AgeUiMode.EARLY_CHILD && pick.reason.isNotBlank()) {
                    Text(
                        text = pick.reason,
                        fontSize = 10.sp,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

@Composable
fun BigBookTile(
    book: Book,
    isFavorited: Boolean,
    showFavoriteButton: Boolean = true,
    onFavoriteClick: () -> Unit,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(220.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            AsyncImage(
                model = book.coverUrl,
                contentDescription = book.title,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.20f))
            )

            CategoryBadge(
                category = book.category,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(10.dp)
            )

            Text(
                text = book.title,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(12.dp),
                maxLines = 2
            )

            if (showFavoriteButton) {
                IconButton(
                    onClick = onFavoriteClick,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(6.dp)
                        .background(Color.White.copy(alpha = 0.75f), CircleShape)
                ) {
                    Icon(
                        imageVector = if (isFavorited) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = "Favorite",
                        tint = if (isFavorited) Color.Red else Color.Gray
                    )
                }
            }
        }
    }
}

@Composable
fun UserBookCardAdaptive(
    book: Book,
    isFavorited: Boolean,
    showFavoriteButton: Boolean = true,
    ageUiMode: AgeUiMode,
    onFavoriteClick: () -> Unit,
    onClick: () -> Unit
) {
    val imageSize = when (ageUiMode) {
        AgeUiMode.EARLY_CHILD -> 90.dp
        AgeUiMode.YOUNG_CHILD -> 76.dp
        AgeUiMode.OLDER_CHILD -> 70.dp
    }

    val titleSize = when (ageUiMode) {
        AgeUiMode.EARLY_CHILD -> 18.sp
        AgeUiMode.YOUNG_CHILD -> 16.sp
        AgeUiMode.OLDER_CHILD -> 16.sp
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier.padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AsyncImage(
                model = book.coverUrl,
                contentDescription = book.title,
                modifier = Modifier
                    .size(imageSize)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color.Gray.copy(alpha = 0.1f)),
                contentScale = ContentScale.Crop
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = book.title,
                    fontWeight = FontWeight.Bold,
                    fontSize = titleSize,
                    maxLines = if (ageUiMode == AgeUiMode.EARLY_CHILD) 2 else 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(6.dp))

                CategoryBadge(category = book.category)

                Spacer(modifier = Modifier.height(6.dp))

                when (ageUiMode) {
                    AgeUiMode.EARLY_CHILD -> {
                        AssistChip(
                            onClick = onClick,
                            label = { Text("Open") }
                        )
                    }

                    AgeUiMode.YOUNG_CHILD -> {
                        Text(
                            text = book.description.ifBlank { "A fun story to explore." },
                            style = MaterialTheme.typography.bodySmall,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    AgeUiMode.OLDER_CHILD -> {
                        Text(
                            text = "By ${book.author}",
                            style = MaterialTheme.typography.bodySmall,
                            maxLines = 1
                        )
                        Text(
                            text = book.description.ifBlank { "A fun story to explore." },
                            style = MaterialTheme.typography.bodySmall,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                if (ageUiMode != AgeUiMode.EARLY_CHILD) {
                    Row(
                        modifier = Modifier.padding(top = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Surface(
                            color = MaterialTheme.colorScheme.primaryContainer,
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                text = book.ageRating,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }

                        if (ageUiMode == AgeUiMode.OLDER_CHILD) {
                            Surface(
                                color = MaterialTheme.colorScheme.secondaryContainer,
                                shape = RoundedCornerShape(4.dp)
                            ) {
                                Text(
                                    text = book.difficulty.uppercase(),
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }

            if (showFavoriteButton) {
                IconButton(onClick = onFavoriteClick) {
                    Icon(
                        imageVector = if (isFavorited) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = "Favorite",
                        tint = if (isFavorited) Color.Red else Color.Gray,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }
    }
}