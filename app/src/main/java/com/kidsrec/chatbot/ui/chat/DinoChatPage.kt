package com.kidsrec.chatbot.ui.chat

import android.Manifest
import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.pm.PackageManager
import android.speech.RecognizerIntent
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Toys
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.SuggestionChipDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import coil.compose.AsyncImage
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.kidsrec.chatbot.R
import com.kidsrec.chatbot.data.model.ChatMessage
import com.kidsrec.chatbot.data.model.Conversation
import com.kidsrec.chatbot.data.model.MessageRole
import com.kidsrec.chatbot.data.model.PlanType
import com.kidsrec.chatbot.data.model.Recommendation
import com.kidsrec.chatbot.data.model.RecommendationType
import com.kidsrec.chatbot.data.repository.ChatQuotaStatus
import com.kidsrec.chatbot.data.repository.GamificationManager
import com.kidsrec.chatbot.data.repository.LearningProgressManager
import com.kidsrec.chatbot.ui.favorites.FavoritesViewModel
import com.kidsrec.chatbot.ui.library.SmartSearchViewModel
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Locale
import kotlin.math.roundToInt

private val SkyTop = Color(0xFFEAF7FF)
private val SkyMid = Color(0xFFF7FBFF)
private val SkyBottom = Color(0xFFFFF7F0)
private val PlayBlue = Color(0xFF5BB6FF)
private val PlayBlueDark = Color(0xFF2F7DDB)
private val DinoGreen = Color(0xFF7ED957)
private val DinoGreenDark = Color(0xFF43A047)
private val WarmYellow = Color(0xFFFFD95A)
private val SoftPink = Color(0xFFFFAED7)
private val SoftOrange = Color(0xFFFFB870)
private val SoftPurple = Color(0xFFCBB7FF)
private val CreamBubble = Color(0xFFFFFCF3)

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
    val quota by viewModel.quota.collectAsState()
    val searchUiState by searchViewModel.uiState.collectAsState()
    val isFreeUser = quota?.planType == PlanType.FREE

    var messageText by remember { mutableStateOf("") }
    var inputWarning by remember { mutableStateOf<String?>(null) }
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()
    var showHistorySheet by remember { mutableStateOf(false) }

    fun openHistorySheet() {
        showHistorySheet = true
    }

    fun closeHistorySheet() {
        showHistorySheet = false
    }

    val context = LocalContext.current
    var isListening by remember { mutableStateOf(false) }

    val speechLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        isListening = false

        if (result.resultCode == Activity.RESULT_OK) {
            val spokenText = result.data
                ?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
                ?.firstOrNull()

            if (!spokenText.isNullOrBlank()) {
                messageText = spokenText

                val lastWord = spokenText.substringAfterLast(' ')
                searchViewModel.onQueryChange(lastWord.ifBlank { spokenText })

                if (inputWarning != null) inputWarning = null
            }
        }
    }

    val audioPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            try {
                isListening = true
                val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                    putExtra(
                        RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                        RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
                    )
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
                    putExtra(RecognizerIntent.EXTRA_PROMPT, "Tell Little Dino what you want")
                }
                speechLauncher.launch(intent)
            } catch (_: ActivityNotFoundException) {
                isListening = false
                Toast.makeText(
                    context,
                    "Speech recognition is not available on this device.",
                    Toast.LENGTH_LONG
                ).show()
            }
        } else {
            Toast.makeText(
                context,
                "Microphone permission is needed for voice chat.",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    fun startVoiceInput() {
        when {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.RECORD_AUDIO
            ) == PackageManager.PERMISSION_GRANTED -> {
                try {
                    isListening = true
                    val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                        putExtra(
                            RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                            RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
                        )
                        putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
                        putExtra(RecognizerIntent.EXTRA_PROMPT, "Tell Little Dino what you want")
                    }
                    speechLauncher.launch(intent)
                } catch (_: ActivityNotFoundException) {
                    isListening = false
                    Toast.makeText(
                        context,
                        "Speech recognition is not available on this device.",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }

            else -> {
                audioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
            }
        }
    }

    val trackedOpenRecommendation: (String, String, Boolean, String, String, String) -> Unit =
        { url, title, isVideo, itemId, imageUrl, description ->

            val currentUid = FirebaseAuth.getInstance().currentUser?.uid

            if (currentUid != null) {
                coroutineScope.launch {
                    try {
                        val firestore = FirebaseFirestore.getInstance()
                        val learningProgressManager = LearningProgressManager(firestore)
                        val gamificationManager = GamificationManager(firestore)

                        if (isVideo) {
                            val result = learningProgressManager.trackVideoWatched(
                                childUserId = currentUid,
                                contentId = itemId,
                                title = title,
                                topic = title
                            )

                            if (result.isFailure) {
                                Log.e(
                                    "DinoChatPage",
                                    "Failed to track video watched: ${result.exceptionOrNull()?.message}",
                                    result.exceptionOrNull()
                                )
                            } else {
                                Log.d("DinoChatPage", "Tracked video watched: $title")

                                val gamificationResult = gamificationManager.refreshGamification(currentUid)
                                if (gamificationResult.isFailure) {
                                    Log.e(
                                        "DinoChatPage",
                                        "Failed to refresh gamification after video: ${gamificationResult.exceptionOrNull()?.message}",
                                        gamificationResult.exceptionOrNull()
                                    )
                                } else {
                                    Log.d("DinoChatPage", "Gamification refreshed after video")
                                }
                            }
                        } else {
                            val result = learningProgressManager.trackBookRead(
                                childUserId = currentUid,
                                contentId = itemId,
                                title = title,
                                topic = title,
                                readingLevel = "Beginner"
                            )

                            if (result.isFailure) {
                                Log.e(
                                    "DinoChatPage",
                                    "Failed to track book read: ${result.exceptionOrNull()?.message}",
                                    result.exceptionOrNull()
                                )
                            } else {
                                Log.d("DinoChatPage", "Tracked book read: $title")

                                val gamificationResult = gamificationManager.refreshGamification(currentUid)
                                if (gamificationResult.isFailure) {
                                    Log.e(
                                        "DinoChatPage",
                                        "Failed to refresh gamification after book: ${gamificationResult.exceptionOrNull()?.message}",
                                        gamificationResult.exceptionOrNull()
                                    )
                                } else {
                                    Log.d("DinoChatPage", "Gamification refreshed after book")
                                }
                            }
                        }
                    } catch (e: Exception) {
                        Log.e("DinoChatPage", "Tracking recommendation failed: ${e.message}", e)
                    }

                    onOpenRecommendation(url, title, isVideo, itemId, imageUrl, description)
                }
            } else {
                onOpenRecommendation(url, title, isVideo, itemId, imageUrl, description)
            }
        }

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
                    withStyle(style = SpanStyle(color = Color.Gray.copy(alpha = 0.45f))) {
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
        ModalBottomSheet(onDismissRequest = { closeHistorySheet() }) {
            ChatHistorySheet(
                conversations = conversations,
                onSelectConversation = { conversationId ->
                    viewModel.loadConversation(conversationId)
                    closeHistorySheet()
                },
                onNewChat = {
                    viewModel.startNewConversation()
                    closeHistorySheet()
                }
            )
        }
    }

    val topInset = WindowInsets.safeDrawing.asPaddingValues().calculateTopPadding()
    val bottomIme = WindowInsets.ime.asPaddingValues().calculateBottomPadding()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(SkyTop, SkyMid, SkyBottom)
                )
            )
    ) {
        FloatingPlayfulBackground()

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = topInset)
        ) {
            DinoChatTopBar(
                onHistoryClick = { openHistorySheet() }
            )

            if (isFreeUser) {
                val currentQuota = quota
                if (currentQuota != null) {
                    FreePlanQuotaBanner(status = currentQuota)
                }
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                if (messages.isEmpty()) {
                    WelcomeView(
                        quickPrompt = { prompt ->
                            messageText = prompt
                            searchViewModel.onQueryChange(prompt)
                        }
                    )
                } else {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(
                            start = 16.dp,
                            end = 16.dp,
                            top = 10.dp,
                            bottom = 18.dp
                        ),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        item {
                            DancingDinoHero()
                            Spacer(modifier = Modifier.height(6.dp))
                        }

                        items(messages) { message ->
                            MessageBubble(
                                message = message,
                                favoriteItems = favoriteItems,
                                isGuest = false,
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
                                onOpenRecommendation = trackedOpenRecommendation,
                                onGetBookPreviewUrl = { title -> viewModel.getBookPreviewUrl(title) }
                            )
                        }

                        if (isLoading) {
                            item { TypingIndicator() }
                        }

                        if (error != null) {
                            item {
                                Surface(
                                    color = Color(0xFFFFEBEE),
                                    shape = RoundedCornerShape(20.dp),
                                    modifier = Modifier.fillMaxWidth(),
                                    shadowElevation = 2.dp
                                ) {
                                    Row(
                                        modifier = Modifier.padding(14.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "Oops! Little Dino couldn't respond. Please try again!",
                                            color = Color(0xFFB3261E),
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

            ChildInputPanel(
                messageText = messageText,
                onMessageChange = {
                    messageText = it
                    val lastWord = it.substringAfterLast(' ')
                    searchViewModel.onQueryChange(lastWord.ifBlank { it })
                    if (inputWarning != null) inputWarning = null
                },
                suggestions = searchUiState.suggestions.map { it.text },
                onSuggestionClick = { suggestion ->
                    val prefix = messageText.substringBeforeLast(' ', "")
                    messageText =
                        if (prefix.isEmpty()) suggestion else "$prefix $suggestion "
                    searchViewModel.onSuggestionClick(suggestion)
                },
                inputWarning = inputWarning,
                onDismissWarning = { inputWarning = null },
                onVoiceClick = { startVoiceInput() },
                isListening = isListening,
                quotaBlocked = isFreeUser && quota?.exhausted == true,
                ghostTextTransformation = ghostTextTransformation,
                onSendClick = {
                    val quotaBlocked = isFreeUser && quota?.exhausted == true
                    if (quotaBlocked) {
                        inputWarning = "Daily Free plan limit reached. Upgrade to Premium to keep chatting."
                        return@ChildInputPanel
                    }
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
                modifier = Modifier.padding(bottom = bottomIme)
            )
        }
    }
}

@Composable
private fun DinoChatTopBar(
    onHistoryClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 10.dp),
        color = Color.Transparent
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(28.dp))
                .background(
                    brush = Brush.horizontalGradient(
                        colors = listOf(PlayBlue, PlayBlueDark)
                    )
                )
                .border(
                    width = 2.dp,
                    color = Color.White.copy(alpha = 0.85f),
                    shape = RoundedCornerShape(28.dp)
                )
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(54.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.92f)),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(id = R.drawable.little_dino),
                    contentDescription = null,
                    modifier = Modifier.size(44.dp),
                    contentScale = ContentScale.Fit
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Little Dino Chat",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White,
                    style = TextStyle(
                        shadow = Shadow(
                            color = Color(0x88000000),
                            blurRadius = 6f
                        )
                    )
                )
                Text(
                    text = "Books, videos, fun ideas and story magic!",
                    fontSize = 12.sp,
                    color = Color.White.copy(alpha = 0.92f)
                )
            }

            IconButton(
                onClick = onHistoryClick,
                modifier = Modifier
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.20f))
            ) {
                Icon(
                    Icons.Default.History,
                    contentDescription = "Chat History",
                    tint = Color.White
                )
            }
        }
    }
}

@Composable
private fun FloatingPlayfulBackground() {
    BoxWithConstraints(
        modifier = Modifier.fillMaxSize()
    ) {
        val width = this.maxWidth
        val height = this.maxHeight

        FloatingOrnament("⭐", 0.08f, 0.10f, width, height, 18.sp, 0)
        FloatingOrnament("🫧", 0.82f, 0.14f, width, height, 22.sp, 250)
        FloatingOrnament("☁️", 0.12f, 0.22f, width, height, 22.sp, 500)
        FloatingOrnament("🌙", 0.88f, 0.24f, width, height, 20.sp, 800)
        FloatingOrnament("✨", 0.18f, 0.42f, width, height, 16.sp, 1000)
        FloatingOrnament("🫧", 0.90f, 0.48f, width, height, 28.sp, 600)
        FloatingOrnament("⭐", 0.76f, 0.62f, width, height, 16.sp, 300)
        FloatingOrnament("☁️", 0.08f, 0.72f, width, height, 24.sp, 1100)
        FloatingOrnament("🪐", 0.84f, 0.80f, width, height, 18.sp, 700)
        FloatingOrnament("✨", 0.22f, 0.88f, width, height, 16.sp, 400)
    }
}

@Composable
private fun FloatingOrnament(
    emoji: String,
    xFraction: Float,
    yFraction: Float,
    width: androidx.compose.ui.unit.Dp,
    height: androidx.compose.ui.unit.Dp,
    size: androidx.compose.ui.unit.TextUnit,
    delayMillis: Int
) {
    val transition = rememberInfiniteTransition(label = "ornament_$emoji")
    val yFloat by transition.animateFloat(
        initialValue = -8f,
        targetValue = 8f,
        animationSpec = infiniteRepeatable(
            animation = tween(2600, delayMillis = delayMillis, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "ornament_y"
    )
    val rotate by transition.animateFloat(
        initialValue = -6f,
        targetValue = 6f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, delayMillis = delayMillis, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "ornament_rotate"
    )

    Text(
        text = emoji,
        fontSize = size,
        modifier = Modifier
            .offset(
                x = width * xFraction,
                y = height * yFraction
            )
            .offset { IntOffset(0, yFloat.roundToInt()) }
            .rotate(rotate),
        color = Color.White.copy(alpha = 0.8f)
    )
}

@Composable
fun FreePlanQuotaBanner(status: ChatQuotaStatus) {
    val resetAtMillis = status.resetAt?.toDate()?.time
    val resetText = remember(resetAtMillis) {
        if (resetAtMillis == null) {
            "Resets 24 hours after your first question today."
        } else {
            val remaining = resetAtMillis - System.currentTimeMillis()
            if (remaining <= 0) {
                "Quota has been refreshed!"
            } else {
                val totalMinutes = remaining / 60_000L
                val hours = totalMinutes / 60L
                val minutes = totalMinutes % 60L
                when {
                    hours > 0 -> "Resets in ${hours}h ${minutes}m"
                    minutes > 0 -> "Resets in ${minutes}m"
                    else -> "Resets in under a minute"
                }
            }
        }
    }

    val containerColor = if (status.exhausted) {
        Color(0xFFFFE4E7)
    } else {
        Color(0xFFFFF5D8)
    }
    val textColor = if (status.exhausted) Color(0xFFC62828) else Color(0xFF8A6500)

    Surface(
        color = containerColor,
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 2.dp),
        shadowElevation = 2.dp
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = if (status.exhausted) Icons.Default.Lock else Icons.Default.Info,
                contentDescription = null,
                tint = textColor,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (status.exhausted) {
                        "Free plan limit reached (0/${status.limit} left)"
                    } else {
                        "Free plan: ${status.remaining}/${status.limit} questions left today"
                    },
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = textColor
                )
                Text(
                    text = resetText,
                    fontSize = 11.sp,
                    color = textColor.copy(alpha = 0.88f)
                )
            }
        }
    }
}

@Composable
fun WelcomeView(
    quickPrompt: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 22.dp, vertical = 18.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        DancingDinoHero()

        Spacer(modifier = Modifier.height(10.dp))

        Text(
            text = "Hi, I’m Little Dino!",
            fontSize = 28.sp,
            fontWeight = FontWeight.ExtraBold,
            color = PlayBlueDark,
            style = TextStyle(
                shadow = Shadow(
                    color = Color.White,
                    blurRadius = 10f
                )
            )
        )

        Spacer(modifier = Modifier.height(8.dp))

        Surface(
            color = CreamBubble,
            shape = RoundedCornerShape(28.dp),
            shadowElevation = 4.dp,
            modifier = Modifier
                .fillMaxWidth()
                .border(
                    2.dp,
                    Color.White.copy(alpha = 0.95f),
                    RoundedCornerShape(28.dp)
                )
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 18.dp, vertical = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Ask me for books, videos, bedtime stories, animals, space, dinosaurs and more!",
                    fontSize = 15.sp,
                    lineHeight = 22.sp,
                    color = Color(0xFF4E5A65)
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(
                listOf(
                    "Recommend dinosaur books",
                    "Show me fun animal videos",
                    "I want a bedtime story",
                    "Give me space books"
                )
            ) { prompt ->
                SuggestionChip(
                    onClick = { quickPrompt(prompt) },
                    label = {
                        Text(
                            text = prompt,
                            fontSize = 12.sp,
                            maxLines = 1
                        )
                    },
                    icon = {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                    },
                    shape = RoundedCornerShape(18.dp),
                    colors = SuggestionChipDefaults.suggestionChipColors(
                        containerColor = Color.White.copy(alpha = 0.92f),
                        labelColor = PlayBlueDark,
                        iconContentColor = SoftOrange
                    )
                )
            }
        }
    }
}

@Composable
private fun DancingDinoHero() {
    val transition = rememberInfiniteTransition(label = "dino_dance")
    val rotation by transition.animateFloat(
        initialValue = -6f,
        targetValue = 6f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "dino_rotation"
    )
    val bounce by transition.animateFloat(
        initialValue = -6f,
        targetValue = 6f,
        animationSpec = infiniteRepeatable(
            animation = tween(700, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "dino_bounce"
    )
    val starScale by transition.animateFloat(
        initialValue = 0.92f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "star_scale"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(30.dp))
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.88f),
                            Color(0xFFF4FBFF)
                        )
                    )
                )
                .border(2.dp, Color.White, RoundedCornerShape(30.dp))
                .padding(horizontal = 20.dp, vertical = 14.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "⭐",
                    fontSize = 20.sp,
                    modifier = Modifier.scale(starScale)
                )

                Spacer(modifier = Modifier.width(8.dp))

                Image(
                    painter = painterResource(id = R.drawable.little_dino),
                    contentDescription = null,
                    modifier = Modifier
                        .size(90.dp)
                        .offset { IntOffset(0, bounce.roundToInt()) }
                        .rotate(rotation),
                    contentScale = ContentScale.Fit
                )

                Spacer(modifier = Modifier.width(8.dp))

                Column {
                    Text(
                        text = "Let’s explore!",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = DinoGreenDark
                    )
                    Text(
                        text = "Tell me what you feel like reading or watching.",
                        fontSize = 12.sp,
                        color = Color(0xFF607080)
                    )
                }
            }
        }
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

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = if (isUser) Alignment.End else Alignment.Start
    ) {
        if (!isUser) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(start = 4.dp, bottom = 4.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(Color.White),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.little_dino),
                        contentDescription = null,
                        modifier = Modifier.size(22.dp),
                        contentScale = ContentScale.Fit
                    )
                }
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Little Dino",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = DinoGreenDark
                )
            }
        }

        Surface(
            color = if (isUser) Color(0xFFDDF1FF) else CreamBubble,
            shape = RoundedCornerShape(
                topStart = 24.dp,
                topEnd = 24.dp,
                bottomStart = if (isUser) 24.dp else 8.dp,
                bottomEnd = if (isUser) 8.dp else 24.dp
            ),
            shadowElevation = 3.dp,
            modifier = Modifier
                .widthIn(max = 320.dp)
                .border(
                    1.5.dp,
                    Color.White.copy(alpha = 0.95f),
                    RoundedCornerShape(
                        topStart = 24.dp,
                        topEnd = 24.dp,
                        bottomStart = if (isUser) 24.dp else 8.dp,
                        bottomEnd = if (isUser) 8.dp else 24.dp
                    )
                )
        ) {
            Text(
                text = message.content,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 13.dp),
                fontSize = 16.sp,
                lineHeight = 22.sp,
                color = Color(0xFF36434E)
            )
        }

        if (!isUser && message.recommendations.isNotEmpty()) {
            Spacer(modifier = Modifier.height(10.dp))
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
    val cardAccent = if (isVideo) SoftOrange else SoftPurple

    Card(
        modifier = Modifier
            .width(230.dp)
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
        shape = RoundedCornerShape(22.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 5.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White.copy(alpha = 0.97f)
        )
    ) {
        Column {
            Box(
                modifier = Modifier
                    .height(130.dp)
                    .fillMaxWidth()
            ) {
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
                            .background(
                                brush = Brush.verticalGradient(
                                    colors = listOf(
                                        cardAccent.copy(alpha = 0.55f),
                                        Color.White
                                    )
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isVideo) Icons.Default.PlayCircle else Icons.Default.Book,
                            contentDescription = null,
                            modifier = Modifier.size(54.dp),
                            tint = if (isVideo) Color(0xFFE65100) else PlayBlueDark
                        )
                    }
                }

                Row(
                    modifier = Modifier
                        .padding(8.dp)
                        .align(Alignment.TopStart)
                ) {
                    Surface(
                        color = if (isVideo) Color(0xFFFF8A65) else Color(0xFF7986FF),
                        shape = RoundedCornerShape(50)
                    ) {
                        Text(
                            text = if (isVideo) "VIDEO" else "BOOK",
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                            fontSize = 10.sp,
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.width(4.dp))

                    Surface(
                        color = if (recommendation.isCurated) Color(0xFF66BB6A) else Color(0xFF4FC3F7),
                        shape = RoundedCornerShape(50)
                    ) {
                        Text(
                            text = if (recommendation.isCurated) "Reviewed" else "Kid-safe",
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                            fontSize = 10.sp,
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                if (recommendation.relevanceScore > 0) {
                    Surface(
                        modifier = Modifier
                            .padding(8.dp)
                            .align(Alignment.TopEnd),
                        color = Color(0xFF43A047),
                        shape = RoundedCornerShape(50)
                    ) {
                        Text(
                            text = "${(recommendation.relevanceScore * 100).toInt()}% Match",
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                            fontSize = 10.sp,
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Column(modifier = Modifier.padding(13.dp)) {
                Text(
                    text = recommendation.title,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 15.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = Color(0xFF2E3A46)
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = recommendation.description,
                    fontSize = 11.sp,
                    maxLines = 2,
                    lineHeight = 15.sp,
                    minLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    color = Color(0xFF6A7985)
                )

                if (recommendation.reason.isNotBlank()) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.Star,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp),
                            tint = WarmYellow
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = recommendation.reason,
                            fontSize = 10.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            color = PlayBlueDark,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(9.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    FilledTonalButton(
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
                        shape = RoundedCornerShape(50),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                    ) {
                        Icon(
                            if (isVideo) Icons.Default.PlayArrow else Icons.AutoMirrored.Filled.MenuBook,
                            null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(if (isVideo) "Watch" else "Read", fontSize = 12.sp)
                    }

                    if (showFavoriteButton) {
                        IconButton(
                            onClick = onToggleFavorite,
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFFFF2F5))
                        ) {
                            Icon(
                                imageVector = if (isFavorited) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                contentDescription = "Toggle Favorite",
                                modifier = Modifier.size(20.dp),
                                tint = if (isFavorited) Color(0xFFE53935) else PlayBlueDark
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
    val transition = rememberInfiniteTransition(label = "typing")
    val dot1 by transition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(500),
            repeatMode = RepeatMode.Reverse
        ),
        label = "dot1"
    )
    val dot2 by transition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(500, delayMillis = 180),
            repeatMode = RepeatMode.Reverse
        ),
        label = "dot2"
    )
    val dot3 by transition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(500, delayMillis = 360),
            repeatMode = RepeatMode.Reverse
        ),
        label = "dot3"
    )

    Row(
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(30.dp)
                .clip(CircleShape)
                .background(Color.White),
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = painterResource(id = R.drawable.little_dino),
                contentDescription = null,
                modifier = Modifier.size(23.dp),
                contentScale = ContentScale.Fit
            )
        }

        Spacer(modifier = Modifier.width(8.dp))

        Surface(
            color = CreamBubble,
            shape = RoundedCornerShape(22.dp),
            shadowElevation = 2.dp
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Dino is thinking",
                    fontSize = 13.sp,
                    color = Color(0xFF50616E)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("•", color = PlayBlueDark.copy(alpha = dot1), fontSize = 18.sp)
                Text("•", color = PlayBlueDark.copy(alpha = dot2), fontSize = 18.sp)
                Text("•", color = PlayBlueDark.copy(alpha = dot3), fontSize = 18.sp)
            }
        }
    }
}

@Composable
private fun ChildInputPanel(
    messageText: String,
    onMessageChange: (String) -> Unit,
    suggestions: List<String>,
    onSuggestionClick: (String) -> Unit,
    inputWarning: String?,
    onDismissWarning: () -> Unit,
    onVoiceClick: () -> Unit,
    isListening: Boolean,
    quotaBlocked: Boolean,
    ghostTextTransformation: VisualTransformation,
    onSendClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth(),
        color = Color.Transparent
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp)
                .clip(RoundedCornerShape(30.dp))
                .background(Color.White.copy(alpha = 0.94f))
                .border(2.dp, Color.White, RoundedCornerShape(30.dp))
                .padding(horizontal = 12.dp, vertical = 12.dp)
        ) {
            if (messageText.isNotBlank() && suggestions.isNotEmpty()) {
                LazyRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    items(suggestions) { suggestion ->
                        SuggestionChip(
                            onClick = { onSuggestionClick(suggestion) },
                            label = {
                                Text(
                                    text = suggestion,
                                    fontSize = 12.sp
                                )
                            },
                            icon = {
                                Icon(
                                    imageVector = Icons.Default.Toys,
                                    contentDescription = null,
                                    modifier = Modifier.size(15.dp)
                                )
                            },
                            colors = SuggestionChipDefaults.suggestionChipColors(
                                containerColor = Color(0xFFEAF6FF),
                                labelColor = PlayBlueDark,
                                iconContentColor = SoftOrange
                            ),
                            border = null,
                            shape = RoundedCornerShape(18.dp)
                        )
                    }
                }
            }

            inputWarning?.let { warning ->
                Surface(
                    color = Color(0xFFFFEBEE),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = warning,
                            color = Color(0xFFB3261E),
                            fontSize = 13.sp,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(
                            onClick = onDismissWarning,
                            modifier = Modifier.size(22.dp)
                        ) {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = "Dismiss",
                                modifier = Modifier.size(16.dp),
                                tint = Color(0xFFB3261E)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
            }

            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = messageText,
                    onValueChange = onMessageChange,
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("Ask Little Dino for a story, video or fun topic...") },
                    shape = RoundedCornerShape(24.dp),
                    visualTransformation = ghostTextTransformation,
                    singleLine = false,
                    maxLines = 4
                )

                Spacer(modifier = Modifier.width(8.dp))

                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FloatingActionButton(
                        onClick = onVoiceClick,
                        containerColor = if (isListening) SoftPink else PlayBlue,
                        modifier = Modifier.size(52.dp)
                    ) {
                        Icon(
                            imageVector = if (isListening) Icons.Default.MicOff else Icons.Default.Mic,
                            contentDescription = if (isListening) "Listening" else "Voice input",
                            tint = Color.White
                        )
                    }

                    FloatingActionButton(
                        onClick = onSendClick,
                        containerColor = if (quotaBlocked) Color.Gray else DinoGreen,
                        modifier = Modifier.size(52.dp)
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.Send,
                            contentDescription = "Send",
                            tint = Color.White
                        )
                    }
                }
            }
        }
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
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .padding(bottom = 32.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Chat History",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = PlayBlueDark
            )
            FilledTonalButton(
                onClick = onNewChat,
                shape = RoundedCornerShape(50)
            ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(4.dp))
                Text("New Chat")
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (conversations.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "No past conversations yet!",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.heightIn(max = 400.dp)
            ) {
                items(conversations) { conversation ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelectConversation(conversation.id) },
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = Color.White
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.ChatBubbleOutline,
                                contentDescription = null,
                                tint = PlayBlueDark,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = conversation.preview.ifBlank { "Empty conversation" },
                                    fontWeight = FontWeight.Medium,
                                    fontSize = 14.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = dateFormat.format(conversation.lastUpdated.toDate()),
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Icon(
                                Icons.Default.ChevronRight,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}