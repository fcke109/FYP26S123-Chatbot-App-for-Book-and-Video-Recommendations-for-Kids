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
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Send
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
import com.kidsrec.chatbot.R
import com.kidsrec.chatbot.data.model.ChatMessage
import com.kidsrec.chatbot.data.model.MessageRole
import com.kidsrec.chatbot.data.model.Recommendation
import com.kidsrec.chatbot.data.model.RecommendationType
import kotlinx.coroutines.launch
import java.net.URLEncoder

/**
 * DinoChatPage: The main interaction screen where kids chat with Little Dino.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DinoChatPage(
    viewModel: ChatViewModel,
    onAddToFavorites: ((Recommendation) -> Unit)? = null,
    onOpenRecommendation: ((url: String, title: String, isVideo: Boolean) -> Unit)? = null
) {
    val messages by viewModel.messages.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()
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

        // Error display
        if (error != null) {
            Surface(color = MaterialTheme.colorScheme.errorContainer, modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Error: $error",
                    modifier = Modifier.padding(8.dp),
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    fontSize = 12.sp
                )
            }
        }

        // Input
        InputArea(
            text = messageText,
            onValueChange = { messageText = it },
            onSend = {
                if (messageText.isNotBlank()) {
                    viewModel.sendMessage(messageText)
                    messageText = ""
                }
            }
        )
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
fun InputArea(text: String, onValueChange: (String) -> Unit, onSend: () -> Unit) {
    Surface(shadowElevation = 8.dp, modifier = Modifier.fillMaxWidth()) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = text,
                onValueChange = onValueChange,
                modifier = Modifier.weight(1f),
                placeholder = { Text("Ask Little Dino for a story...") },
                shape = RoundedCornerShape(24.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            FloatingActionButton(onClick = onSend, containerColor = MaterialTheme.colorScheme.primary) {
                Icon(Icons.Default.Send, contentDescription = "Send")
            }
        }
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
            LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
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
    Card(
        modifier = Modifier.width(200.dp).clickable { 
            coroutineScope.launch {
                val url = onGetBookPreviewUrl?.invoke(recommendation.title) ?: ""
                onOpenRecommendation?.invoke(url, recommendation.title, recommendation.type == RecommendationType.VIDEO)
            }
        },
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(recommendation.title, fontWeight = FontWeight.Bold, fontSize = 14.sp, maxLines = 1)
            Text(recommendation.description, fontSize = 12.sp, maxLines = 2)
            Button(
                onClick = {
                    coroutineScope.launch {
                        val url = onGetBookPreviewUrl?.invoke(recommendation.title) ?: ""
                        onOpenRecommendation?.invoke(url, recommendation.title, recommendation.type == RecommendationType.VIDEO)
                    }
                },
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
            ) {
                Text("View Story")
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
