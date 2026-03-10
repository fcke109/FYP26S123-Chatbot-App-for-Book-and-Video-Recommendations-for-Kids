package com.kidsrec.chatbot.ui.library

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
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
import com.kidsrec.chatbot.data.model.Recommendation
import com.kidsrec.chatbot.data.model.RecommendationType
import com.kidsrec.chatbot.ui.favorites.FavoritesViewModel

@Composable
fun UserLibraryScreen(
    viewModel: LibraryViewModel,
    favoritesViewModel: FavoritesViewModel,
    onViewBook: (String, String) -> Unit
) {
    val books by viewModel.curatedBooks.collectAsState()
    val topPicks by viewModel.topPicks.collectAsState()
    val favoriteItems by favoritesViewModel.favorites.collectAsState()

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
            // Top Picks Section
            if (topPicks.isNotEmpty()) {
                TopPicksSection(
                    picks = topPicks,
                    onPickClick = { rec ->
                        if (rec.url.isNotBlank()) onViewBook(rec.title, rec.url)
                    }
                )
                Spacer(modifier = Modifier.height(16.dp))
            }

            // All Books Grid
            Text(
                text = "All Books",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(8.dp))

            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(books) { book ->
                    val isFavorited = favoriteItems.any { it.itemId == book.id }

                    UserBookCard(
                        book = book,
                        isFavorited = isFavorited,
                        onFavoriteClick = {
                            if (isFavorited) {
                                favoritesViewModel.removeFavorite(book.id)
                            } else {
                                favoritesViewModel.addFavorite(
                                    itemId = book.id,
                                    type = RecommendationType.BOOK,
                                    title = book.title,
                                    description = book.description,
                                    imageUrl = book.coverUrl,
                                    url = book.readerUrl.ifBlank { book.bookUrl }
                                )
                            }
                        },
                        onClick = {
                            val url = book.readerUrl.ifBlank { book.bookUrl }
                            if (url.isNotBlank()) onViewBook(book.title, url)
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
                TopPickCard(pick = pick, onClick = { onPickClick(pick) })
            }
        }
    }
}

@Composable
fun TopPickCard(pick: Recommendation, onClick: () -> Unit) {
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
                    Box(
                        modifier = Modifier.fillMaxSize().background(Color(0xFFE5F0FF)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.MenuBook, null, modifier = Modifier.size(40.dp), tint = MaterialTheme.colorScheme.primary)
                    }
                }

                // ANN Score Badge
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
    onFavoriteClick: () -> Unit,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column {
            Box {
                AsyncImage(
                    model = book.coverUrl,
                    contentDescription = book.title,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                        .clip(RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp)),
                    contentScale = ContentScale.Crop
                )

                // Favorite Button Overlay
                IconButton(
                    onClick = onFavoriteClick,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(4.dp)
                        .background(Color.White.copy(alpha = 0.7f), CircleShape)
                        .size(32.dp)
                ) {
                    Icon(
                        imageVector = if (isFavorited) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = "Favorite",
                        tint = if (isFavorited) Color.Red else Color.Gray,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    text = book.title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = book.author,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 1
                )

                Spacer(modifier = Modifier.height(4.dp))

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
            }
        }
    }
}
