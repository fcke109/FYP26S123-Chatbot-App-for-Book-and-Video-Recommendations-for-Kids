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
import androidx.compose.material.icons.automirrored.filled.MenuBook
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
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.kidsrec.chatbot.R
import com.kidsrec.chatbot.data.model.ChatMessage
import com.kidsrec.chatbot.data.model.Conversation
import com.kidsrec.chatbot.data.model.MessageRole
import com.kidsrec.chatbot.data.model.Recommendation
import com.kidsrec.chatbot.data.model.RecommendationType
import com.kidsrec.chatbot.ui.favorites.FavoritesViewModel
import com.kidsrec.chatbot.ui.library.SmartSearchViewModel
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DinoChatPage(
    viewModel: ChatViewModel,
    favoritesViewModel: FavoritesViewModel,
    searchViewModel: SmartSearchViewModel,
    onOpenRecommendation: (String, String, Boolean, String, String, String) -> Unit
) {
    val messages by viewModel.messages.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()
    val conversations by viewModel.conversations.collectAsState()
    val favoriteItems by favoritesViewModel.favorites.collectAsState()
    val isGuestUser by favoritesViewModel.isGuest.collectAsState()
    val searchUiState by searchViewModel.uiState.collectAsState()

    var messageText by remember { mutableStateOf("") }
    var inputWarning by remember { mutableStateOf<String?>(null) }
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()
    var showHistorySheet by remember { mutableStateOf(false) }

    // Ghost text autocomplete logic
    val autocompleteSuggestion = remember(messageText, searchUiState.suggestions) {
        if (messageText.isBlank() || messageText.endsWith(" ")) return@remember null
        
        val lastWord = messageText.substringAfterLast(' ')
        if (lastWord.isEmpty()) return@remember null
        
        searchUiState.suggestions.firstNotNullOfOrNull { suggestion ->
            if (suggestion.text.startsWith(lastWord, ignoreCase = true)) {
                val remaining = suggestion.text.substring(lastWord.length)
                if (remaining.isNotEmpty()) remaining else null
            } else null
        }
    }

    val ghostTextTransformation = remember(autocompleteSuggestion) {
        VisualTransformation { text ->
            if (autocompleteSuggestion != null) {
                val annotatedString = buildAnnotatedString {
                    append(text.text)
                    withStyle(style = SpanStyle(color = Color.Gray.copy(alpha = 0.5f))) {
                        append(autocompleteSuggestion)
                    }
                }
                TransformedText(
                    annotatedString,
                    object : OffsetMapping {
                        override fun originalToTransformed(offset: Int): Int = offset
                        override fun transformedToOriginal(offset: Int): Int =
                            if (offset > text.length) text.length else offset
                    }
                )
            } else {
                TransformedText(text, OffsetMapping.Identity)
            }
        }
    }

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            coroutineScope.launch {
                listState.animateScrollToItem(messages.size - 1)
            }
        }
    }

    if (showHistorySheet) {
        ModalBottomSheet(onDismissRequest = { showHistorySheet = false }) {
            ChatHistorySheet(
                conversations = conversations,
                onSelectConversation = { conversationId ->
                    viewModel.loadConversation(conversationId)
                    showHistorySheet = false
                },
                onNewChat = {
                    viewModel.startNewConversation()
                    showHistorySheet = false
                }
            )
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Surface(color = MaterialTheme.colorScheme.primary, modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Image(
                    painter = painterResource(id = R.drawable.little_dino),
                    contentDescription = null,
                    modifier = Modifier.size(48.dp).clip(CircleShape).background(Color.White),
                    contentScale = ContentScale.Fit
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text("Little Dino", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    Text("Your visual story-time buddy", fontSize = 12.sp, color = Color.White.copy(alpha = 0.8f))
                }
                IconButton(onClick = { showHistorySheet = true }) {
                    Icon(Icons.Default.History, contentDescription = "Chat History", tint = Color.White)
                }
            }
        }

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
                            favoriteItems = favoriteItems,
                            isGuest = isGuestUser,
                            onToggleFavorite = { rec ->
                                val isFav = favoriteItems.any { it.itemId == rec.id }
                                if (isFav) {
                                    favoritesViewModel.removeFavorite(rec.id)
                                } else {
                                    coroutineScope.launch {
                                        val resolvedUrl = if (rec.type == RecommendationType.VIDEO) {
                                            rec.url
                                        } else {
                                            viewModel.getBookPreviewUrl(rec.title).ifBlank { rec.url }
                                        }
                                        favoritesViewModel.addFavorite(
                                            itemId = rec.id,
                                            type = rec.type,
                                            title = rec.title,
                                            description = rec.description,
                                            imageUrl = rec.imageUrl,
                                            url = resolvedUrl
                                        )
                                    }
                                }
                            },
                            onOpenRecommendation = onOpenRecommendation,
                            onGetBookPreviewUrl = { title -> viewModel.getBookPreviewUrl(title) }
                        )
                    }
                    if (isLoading) { item { TypingIndicator() } }
                    if (error != null) {
                        item {
                            Surface(
                                color = MaterialTheme.colorScheme.errorContainer,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Oops! Little Dino couldn't respond. Please try again!",
                                        color = MaterialTheme.colorScheme.onErrorContainer,
                                        fontSize = 13.sp,
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        Surface(shadowElevation = 8.dp, modifier = Modifier.fillMaxWidth()) {
            Column {
                // Suggestions Row
                if (messageText.isNotBlank() && searchUiState.suggestions.isNotEmpty()) {
                    LazyRow(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        items(searchUiState.suggestions) { suggestion ->
                            SuggestionChip(
                                onClick = {
                                    val prefix = messageText.substringBeforeLast(' ', "")
                                    messageText = if (prefix.isEmpty()) suggestion.text else "$prefix ${suggestion.text} "
                                    searchViewModel.onSuggestionClick(suggestion.text)
                                },
                                label = { 
                                    Text(
                                        text = suggestion.text,
                                        fontSize = 12.sp
                                    ) 
                                },
                                colors = SuggestionChipDefaults.suggestionChipColors(
                                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                    labelColor = MaterialTheme.colorScheme.onSecondaryContainer
                                ),
                                border = null,
                                shape = RoundedCornerShape(16.dp)
                            )
                        }
                    }
                }

                // Warning banner for inappropriate input
                inputWarning?.let { warning ->
                    Surface(
                        color = MaterialTheme.colorScheme.errorContainer,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = warning,
                                color = MaterialTheme.colorScheme.errorContainer,
                                fontSize = 13.sp,
                                modifier = Modifier.weight(1f)
                            )
                            IconButton(
                                onClick = { inputWarning = null },
                                modifier = Modifier.size(20.dp)
                            ) {
                                Icon(
                                    Icons.Default.Close,
                                    contentDescription = "Dismiss",
                                    modifier = Modifier.size(16.dp),
                                    tint = MaterialTheme.colorScheme.onErrorContainer
                                )
                            }
                        }
                    }
                }

                Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = messageText,
                        onValueChange = {
                            messageText = it
                            // Pass the last word to search view model for ghost-text relevant suggestions
                            val lastWord = it.substringAfterLast(' ')
                            searchViewModel.onQueryChange(lastWord.ifBlank { it })
                            // Clear warning as user types
                            if (inputWarning != null) inputWarning = null
                        },
                        modifier = Modifier.weight(1f),
                        placeholder = { Text("Ask Little Dino for a story...") },
                        shape = RoundedCornerShape(24.dp),
                        visualTransformation = ghostTextTransformation
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    FloatingActionButton(
                        onClick = {
                            if (messageText.isNotBlank()) {
                                val validation = com.kidsrec.chatbot.util.InputSanitizer.validateMessage(messageText)
                                if (validation != null) {
                                    inputWarning = validation
                                } else {
                                    inputWarning = null
                                    viewModel.sendMessage(messageText)
                                    messageText = ""
                                    searchViewModel.onSearch()
                                }
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
    favoriteItems: List<com.kidsrec.chatbot.data.model.Favorite>,
    isGuest: Boolean = false,
    onToggleFavorite: (Recommendation) -> Unit,
    onOpenRecommendation: (String, String, Boolean, String, String, String) -> Unit,
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
                    val isFavorited = favoriteItems.any { it.itemId == recommendation.id }
                    RecommendationCard(
                        recommendation = recommendation,
                        isFavorited = isFavorited,
                        showFavoriteButton = !isGuest,
                        onToggleFavorite = { onToggleFavorite(recommendation) },
                        onOpenRecommendation = onOpenRecommendation,
                        onGetBookPreviewUrl = onGetBookPreviewUrl
                    )
                }
            }
        }
    }
}

@Composable
fun RecommendationCard(
    recommendation: Recommendation,
    isFavorited: Boolean,
    showFavoriteButton: Boolean = true,
    onToggleFavorite: () -> Unit,
    onOpenRecommendation: (String, String, Boolean, String, String, String) -> Unit,
    onGetBookPreviewUrl: (suspend (String) -> String)? = null
) {
    val coroutineScope = rememberCoroutineScope()
    val isVideo = recommendation.type == RecommendationType.VIDEO
    
    Card(
        modifier = Modifier
            .width(220.dp)
            .clickable {
                coroutineScope.launch {
                    val finalUrl = if (isVideo) {
                        recommendation.url
                    } else {
                        onGetBookPreviewUrl?.invoke(recommendation.title) ?: recommendation.url
                    }
                    if (finalUrl.isNotBlank()) {
                        onOpenRecommendation(
                            finalUrl, 
                            recommendation.title, 
                            isVideo,
                            recommendation.id,
                            recommendation.imageUrl,
                            recommendation.description
                        )
                    }
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
                        modifier = Modifier.fillMaxSize().background(if (isVideo) Color(0xFFFFE5E5) else Color(0xFFE5F0FF)),
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
                
                Row(modifier = Modifier.padding(8.dp).align(Alignment.TopStart)) {
                    Surface(
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

                    if (!recommendation.isCurated) {
                        Spacer(modifier = Modifier.width(4.dp))
                        Surface(
                            color = Color(0xFFFF9800),
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                text = "Not Reviewed",
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                fontSize = 10.sp,
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                if (recommendation.relevanceScore > 0) {
                    Surface(
                        modifier = Modifier.padding(8.dp).align(Alignment.TopEnd),
                        color = Color(0xFF4CAF50),
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            text = "${(recommendation.relevanceScore * 100).toInt()}% Match",
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            fontSize = 10.sp,
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    }
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

                if (recommendation.reason.isNotBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(text = recommendation.reason, fontSize = 10.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Medium)
                }

                Spacer(modifier = Modifier.height(8.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(
                        onClick = {
                            coroutineScope.launch {
                                val finalUrl = if (isVideo) {
                                    recommendation.url
                                } else {
                                    onGetBookPreviewUrl?.invoke(recommendation.title) ?: recommendation.url
                                }
                                if (finalUrl.isNotBlank()) {
                                    onOpenRecommendation(
                                        finalUrl, 
                                        recommendation.title, 
                                        isVideo,
                                        recommendation.id,
                                        recommendation.imageUrl,
                                        recommendation.description
                                    )
                                }
                            }
                        },
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Icon(if (isVideo) Icons.Default.PlayArrow else Icons.AutoMirrored.Filled.MenuBook, null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text(if (isVideo) "Watch" else "Read", fontSize = 12.sp)
                    }
                    
                    if (showFavoriteButton) {
                        IconButton(
                            onClick = onToggleFavorite,
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = if (isFavorited) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                contentDescription = "Toggle Favorite",
                                modifier = Modifier.size(20.dp),
                                tint = if (isFavorited) Color.Red else MaterialTheme.colorScheme.primary
                            )
                        }
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

@Composable
fun ChatHistorySheet(
    conversations: List<Conversation>,
    onSelectConversation: (String) -> Unit,
    onNewChat: () -> Unit
) {
    val dateFormat = remember { SimpleDateFormat("MMM d, h:mm a", Locale.getDefault()) }

    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).padding(bottom = 32.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = "Chat History", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            FilledTonalButton(onClick = onNewChat) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(4.dp))
                Text("New Chat")
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (conversations.isEmpty()) {
            Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                Text("No past conversations yet!", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.heightIn(max = 400.dp)) {
                items(conversations) { conversation ->
                    Card(
                        modifier = Modifier.fillMaxWidth().clickable { onSelectConversation(conversation.id) },
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.ChatBubbleOutline, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(text = conversation.preview.ifBlank { "Empty conversation" }, fontWeight = FontWeight.Medium, fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text(text = dateFormat.format(conversation.lastUpdated.toDate()), fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Icon(Icons.Default.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }
    }
}
