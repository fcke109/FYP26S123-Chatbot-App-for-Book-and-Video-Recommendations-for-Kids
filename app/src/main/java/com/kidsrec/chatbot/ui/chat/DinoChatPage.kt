package com.kidsrec.chatbot.ui.chat

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.kidsrec.chatbot.R
import com.kidsrec.chatbot.data.model.ChatMessage
import com.kidsrec.chatbot.data.model.MessageRole
import com.kidsrec.chatbot.data.model.Recommendation
import com.kidsrec.chatbot.data.model.RecommendationType
import kotlinx.coroutines.launch

@Composable
fun DinoChatPage(
    viewModel: ChatViewModel,
    onAddToFavorites: ((Recommendation) -> Unit)? = null,
    onOpenRecommendation: ((url: String, title: String, isVideo: Boolean) -> Unit)? = null
) {
    val messages by viewModel.messages.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    var messageText by remember { mutableStateOf("") }
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            coroutineScope.launch {
                listState.animateScrollToItem(messages.size - 1)
            }
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Header
        Surface(color = MaterialTheme.colorScheme.primary, modifier = Modifier.fillMaxWidth()) {
            Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Image(
                    painter = painterResource(id = R.drawable.little_dino),
                    contentDescription = null,
                    modifier = Modifier.size(48.dp).clip(CircleShape).background(Color.White),
                    contentScale = ContentScale.Fit
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text("Little Dino", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    Text("Your visual story-time buddy", fontSize = 12.sp, color = Color.White.copy(alpha = 0.8f))
                }
            }
        }

        // Chat List
        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            if (messages.isEmpty()) {
                WelcomeView()
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(messages) { message ->
                        MessageBubble(
                            message = message,
                            onAddToFavorites = onAddToFavorites,
                            onOpenRecommendation = onOpenRecommendation,
                            onGetBookPreviewUrl = { title -> viewModel.getBookPreviewUrl(title) }
                        )
                    }
                    if (isLoading) { item { TypingIndicator() } }
                }
            }
        }

        // Input
        Surface(shadowElevation = 8.dp, modifier = Modifier.fillMaxWidth()) {
            Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = messageText,
                    onValueChange = { messageText = it },
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("Ask Little Dino for a story...") },
                    shape = RoundedCornerShape(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                FloatingActionButton(
                    onClick = {
                        if (messageText.isNotBlank()) {
                            viewModel.sendMessage(messageText)
                            messageText = ""
                        }
                    },
                    containerColor = MaterialTheme.colorScheme.primary
                ) {
                    Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send")
                }
            }
        }
    }
}

@Composable
fun WelcomeView() {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Image(
            painter = painterResource(id = R.drawable.little_dino),
            contentDescription = null,
            modifier = Modifier.size(200.dp),
            contentScale = ContentScale.Fit
        )
        Text("Hi! I'm Little Dino!", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
        Text("Ask me for a story or search the library!", fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
fun MessageBubble(
    message: ChatMessage,
    onAddToFavorites: ((Recommendation) -> Unit)? = null,
    onOpenRecommendation: ((url: String, title: String, isVideo: Boolean) -> Unit)? = null,
    onGetBookPreviewUrl: (suspend (String) -> String)? = null
) {
    val isUser = message.role == MessageRole.USER
    Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = if (isUser) Alignment.End else Alignment.Start) {
        Surface(
            color = if (isUser) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
            shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp, bottomStart = if (isUser) 16.dp else 4.dp, bottomEnd = if (isUser) 4.dp else 16.dp),
            modifier = Modifier.widthIn(max = 300.dp)
        ) {
            Text(text = message.content, modifier = Modifier.padding(12.dp), fontSize = 16.sp)
        }

        if (!isUser && message.recommendations.isNotEmpty()) {
            Spacer(modifier = Modifier.height(8.dp))
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(end = 16.dp)
            ) {
                items(message.recommendations) { recommendation ->
                    RecommendationCard(recommendation, onAddToFavorites, onOpenRecommendation, onGetBookPreviewUrl)
                }
            }
        }
    }
}

@Composable
fun RecommendationCard(
    recommendation: Recommendation,
    onAddToFavorites: ((Recommendation) -> Unit)? = null,
    onOpenRecommendation: ((url: String, title: String, isVideo: Boolean) -> Unit)? = null,
    onGetBookPreviewUrl: (suspend (String) -> String)? = null
) {
    val coroutineScope = rememberCoroutineScope()
    val isVideo = recommendation.type == RecommendationType.VIDEO
    
    Card(
        modifier = Modifier
            .width(220.dp)
            .clickable {
                coroutineScope.launch {
                    val url = if (recommendation.url.isNotBlank()) {
                        recommendation.url
                    } else if (isVideo) {
                        "https://www.youtube.com/results?search_query=${java.net.URLEncoder.encode(recommendation.title, "UTF-8")}+kids"
                    } else {
                        onGetBookPreviewUrl?.invoke(recommendation.title) ?: ""
                    }
                    onOpenRecommendation?.invoke(url, recommendation.title, isVideo)
                }
            },
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column {
            Box(modifier = Modifier.height(120.dp).fillMaxWidth()) {
                if (recommendation.imageUrl.isNotEmpty()) {
                    AsyncImage(
                        model = recommendation.imageUrl,
                        contentDescription = recommendation.title,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(if (isVideo) Color(0xFFFFE5E5) else Color(0xFFE5F0FF)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isVideo) Icons.Default.PlayCircle else Icons.Default.Book,
                            contentDescription = null,
                            modifier = Modifier.size(48.dp),
                            tint = if (isVideo) Color.Red else MaterialTheme.colorScheme.primary
                        )
                    }
                }
                
                // Type Badge
                Surface(
                    modifier = Modifier.padding(8.dp).align(Alignment.TopStart),
                    color = if (isVideo) Color.Red else MaterialTheme.colorScheme.primary,
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Text(
                        text = if (isVideo) "VIDEO" else "BOOK",
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        fontSize = 10.sp,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    text = recommendation.title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = recommendation.description,
                    fontSize = 11.sp,
                    maxLines = 2,
                    lineHeight = 14.sp,
                    minLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(
                        onClick = {
                            coroutineScope.launch {
                                val url = if (recommendation.url.isNotBlank()) {
                                    recommendation.url
                                } else if (isVideo) {
                                    "https://www.youtube.com/results?search_query=${java.net.URLEncoder.encode(recommendation.title, "UTF-8")}+kids"
                                } else {
                                    onGetBookPreviewUrl?.invoke(recommendation.title) ?: ""
                                }
                                onOpenRecommendation?.invoke(url, recommendation.title, isVideo)
                            }
                        },
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Icon(if (isVideo) Icons.Default.PlayArrow else Icons.Default.MenuBook, null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text(if (isVideo) "Watch" else "Read", fontSize = 12.sp)
                    }
                    
                    IconButton(
                        onClick = { onAddToFavorites?.invoke(recommendation) },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(Icons.Default.FavoriteBorder, null, modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.primary)
                    }
                }
            }
        }
    }
}

@Composable
fun TypingIndicator() {
    Surface(color = MaterialTheme.colorScheme.surfaceVariant, shape = RoundedCornerShape(16.dp)) {
        Text("Dino is thinking...", modifier = Modifier.padding(12.dp), fontSize = 12.sp)
    }
}
