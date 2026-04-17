package com.kidsrec.chatbot.ui.favorites

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
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
import com.kidsrec.chatbot.data.model.Favorite
import java.net.URLEncoder
import com.kidsrec.chatbot.data.model.Recommendation
import com.kidsrec.chatbot.data.model.RecommendationType

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FavoritesScreen(
    viewModel: FavoritesViewModel,
    onOpenFavorite: (String, String, Boolean, String, String, String) -> Unit
) {
    val favorites by viewModel.filteredFavorites.collectAsState()
    val totalCount by viewModel.totalFavoritesCount.collectAsState()
    val selectedFilter by viewModel.selectedFilter.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val isGuest by viewModel.isGuest.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("My Favorites") }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Filter chips row
            if (!isGuest && !isLoading && totalCount > 0) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = selectedFilter == FavoriteFilter.ALL,
                        onClick = { viewModel.setFilter(FavoriteFilter.ALL) },
                        label = { Text("All") },
                        leadingIcon = {
                            Icon(Icons.Default.Star, contentDescription = null, modifier = Modifier.size(18.dp))
                        }
                    )
                    FilterChip(
                        selected = selectedFilter == FavoriteFilter.BOOKS,
                        onClick = { viewModel.setFilter(FavoriteFilter.BOOKS) },
                        label = { Text("Books") },
                        leadingIcon = {
                            Icon(Icons.Default.Book, contentDescription = null, modifier = Modifier.size(18.dp))
                        }
                    )
                    FilterChip(
                        selected = selectedFilter == FavoriteFilter.VIDEOS,
                        onClick = { viewModel.setFilter(FavoriteFilter.VIDEOS) },
                        label = { Text("Videos") },
                        leadingIcon = {
                            Icon(Icons.Default.PlayCircle, contentDescription = null, modifier = Modifier.size(18.dp))
                        }
                    )
                }
            }

            Box(modifier = Modifier.fillMaxSize()) {
                when {
                    isLoading -> {
                        CircularProgressIndicator(
                            modifier = Modifier.align(Alignment.Center)
                        )
                    }
                    isGuest -> {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = "Favorites require an account",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Create an account to save your favorite books and videos!",
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    favorites.isEmpty() -> {
                        val emptyMessage = when {
                            totalCount == 0 -> "No favorites yet!"
                            selectedFilter == FavoriteFilter.BOOKS -> "No books in your favorites"
                            selectedFilter == FavoriteFilter.VIDEOS -> "No videos in your favorites"
                            else -> "No favorites yet!"
                        }
                        val emptySubtext = when {
                            totalCount == 0 -> "Favorite your best stories from the Library or Chat!"
                            else -> "Try a different filter or add some from Chat!"
                        }
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = emptyMessage,
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = emptySubtext,
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    else -> {
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(2),
                            contentPadding = PaddingValues(16.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(favorites) { favorite ->
                                FavoriteCard(
                                    favorite = favorite,
                                    onRemove = { viewModel.removeFavorite(favorite.itemId) },
                                    onOpen = {
                                        val isVideo = favorite.type == RecommendationType.VIDEO
                                        val url = if (favorite.url.isNotBlank()) {
                                            favorite.url
                                        } else if (isVideo) {
                                            val encodedTitle = URLEncoder.encode(favorite.title, "UTF-8")
                                            "https://www.youtube.com/results?search_query=$encodedTitle+for+kids"
                                        } else {
                                            "https://archive.org/details/texts?query=${URLEncoder.encode(favorite.title, "UTF-8")}"
                                        }
                                        onOpenFavorite(
                                            url,
                                            favorite.title,
                                            isVideo,
                                            favorite.itemId,
                                            favorite.imageUrl,
                                            favorite.description
                                        )
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun FavoriteCard(
    favorite: Favorite,
    onRemove: () -> Unit,
    onOpen: () -> Unit
) {
    val isVideo = favorite.type == RecommendationType.VIDEO

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(280.dp)
            .clickable { onOpen() },
        shape = RoundedCornerShape(16.dp)
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(150.dp)
            ) {
                if (favorite.imageUrl.isNotEmpty()) {
                    AsyncImage(
                        model = favorite.imageUrl,
                        contentDescription = favorite.title,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = if (isVideo)
                            Color(0xFFFF0000).copy(alpha = 0.1f)
                        else
                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                    ) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (isVideo) Icons.Default.PlayCircle else Icons.Default.Book,
                                contentDescription = null,
                                modifier = Modifier.size(48.dp),
                                tint = if (isVideo) Color(0xFFFF0000) else MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }

                IconButton(
                    onClick = onRemove,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(4.dp)
                        .background(Color.White.copy(alpha = 0.7f), CircleShape)
                        .size(32.dp)
                ) {
                    Icon(
                        Icons.Default.Favorite,
                        contentDescription = "Remove",
                        tint = Color.Red,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp)
            ) {
                Text(
                    text = favorite.title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.weight(1f))

                Button(
                    onClick = onOpen,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isVideo) Color(0xFFFF0000) else MaterialTheme.colorScheme.primary
                    ),
                    contentPadding = PaddingValues(vertical = 4.dp)
                ) {
                    Text(
                        text = if (isVideo) "Watch" else "Read",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
