package com.kidsrec.chatbot.ui.library

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
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
import com.kidsrec.chatbot.ui.favorites.FavoritesViewModel

@Composable
fun UserLibraryScreen(
    viewModel: LibraryViewModel,
    favoritesViewModel: FavoritesViewModel,
    onOpenRecommendation: (url: String, title: String, isVideo: Boolean, itemId: String, imageUrl: String, description: String) -> Unit
) {
    val books by viewModel.curatedBooks.collectAsState()
    val topPicks by viewModel.topPicks.collectAsState()
    val favoriteItems by favoritesViewModel.favorites.collectAsState()
    val isGuest by favoritesViewModel.isGuest.collectAsState()

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text(
            text = "My Story Library",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = "Favorite your best stories to read again!",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(16.dp))

        if (books.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No books here yet. Check back soon!")
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Top Picks Section
                if (topPicks.isNotEmpty()) {
                    item {
                        TopPicksSection(
                            picks = topPicks,
                            favoriteItems = favoriteItems,
                            isGuest = isGuest,
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
                        text = "All Books",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }

                items(books) { book ->
                    val isFavorited = favoriteItems.any { it.itemId == book.id }

                    UserBookCard(
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

@Composable
fun TopPicksSection(
    picks: List<Recommendation>,
    favoriteItems: List<Favorite>,
    isGuest: Boolean = false,
    onToggleFavorite: (Recommendation) -> Unit,
    onPickClick: (Recommendation) -> Unit
) {
    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                Icons.Default.AutoAwesome,
                contentDescription = null,
                tint = Color(0xFFFFA000),
                modifier = Modifier.size(22.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "Top Picks for You",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        }
        Spacer(modifier = Modifier.height(8.dp))

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(picks) { pick ->
                val isFavorited = favoriteItems.any { it.itemId == pick.id }
                TopPickCard(
                    pick = pick,
                    isFavorited = isFavorited,
                    showFavoriteButton = !isGuest,
                    onFavoriteClick = { onToggleFavorite(pick) },
                    onClick = { onPickClick(pick) }
                )
            }
        }
    }
}

@Composable
fun TopPickCard(
    pick: Recommendation,
    isFavorited: Boolean,
    showFavoriteButton: Boolean = true,
    onFavoriteClick: () -> Unit,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .width(160.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column {
            Box(modifier = Modifier.height(120.dp).fillMaxWidth()) {
                if (pick.imageUrl.isNotEmpty()) {
                    AsyncImage(
                        model = pick.imageUrl,
                        contentDescription = pick.title,
                        modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp)),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    val isVideo = pick.type == RecommendationType.VIDEO
                    Box(
                        modifier = Modifier.fillMaxSize().background(if (isVideo) Color(0xFFFFE5E5) else Color(0xFFE5F0FF)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isVideo) Icons.Default.PlayCircle else Icons.Default.MenuBook,
                            contentDescription = null,
                            modifier = Modifier.size(40.dp),
                            tint = if (isVideo) Color.Red else MaterialTheme.colorScheme.primary
                        )
                    }
                }

                // Match Score Badge
                if (pick.relevanceScore > 0) {
                    Surface(
                        modifier = Modifier.padding(6.dp).align(Alignment.TopEnd),
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
                
                // Favorite Toggle Button
                if (showFavoriteButton) {
                    IconButton(
                        onClick = onFavoriteClick,
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(4.dp)
                            .size(32.dp)
                            .background(Color.White.copy(alpha = 0.7f), CircleShape)
                    ) {
                        Icon(
                            imageVector = if (isFavorited) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            contentDescription = "Favorite",
                            tint = if (isFavorited) Color.Red else Color.Gray,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            Column(modifier = Modifier.padding(8.dp)) {
                Text(
                    text = pick.title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (pick.reason.isNotBlank()) {
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
fun UserBookCard(
    book: Book,
    isFavorited: Boolean,
    showFavoriteButton: Boolean = true,
    onFavoriteClick: () -> Unit,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(modifier = Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
            Box {
                AsyncImage(
                    model = book.coverUrl,
                    contentDescription = book.title,
                    modifier = Modifier.size(70.dp).clip(RoundedCornerShape(8.dp)).background(Color.Gray.copy(alpha = 0.1f)),
                    contentScale = ContentScale.Crop
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(book.title, fontWeight = FontWeight.Bold, fontSize = 16.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text("By ${book.author}", style = MaterialTheme.typography.bodySmall, maxLines = 1)
                Row(modifier = Modifier.padding(top = 4.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
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
