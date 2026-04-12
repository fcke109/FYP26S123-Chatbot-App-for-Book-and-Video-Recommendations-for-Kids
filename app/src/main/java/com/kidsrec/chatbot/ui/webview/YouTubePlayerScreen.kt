package com.kidsrec.chatbot.ui.webview

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.kidsrec.chatbot.data.model.BadgeUnlock
import com.kidsrec.chatbot.data.model.GamificationProfile
import com.kidsrec.chatbot.data.repository.GamificationManager
import com.kidsrec.chatbot.data.repository.LearningProgressManager
import kotlinx.coroutines.delay
import kotlinx.coroutines.tasks.await
import java.net.URLDecoder

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun YouTubePlayerScreen(
    videoId: String,
    title: String,
    onBack: (durationSeconds: Long) -> Unit
) {
    val cleanVideoId = videoId.trim()
    val openedAtMs = remember { System.currentTimeMillis() }
    var isLoading by remember { mutableStateOf(true) }
    var loadError by remember { mutableStateOf(false) }
    var isFinishing by remember { mutableStateOf(false) }
    var showCelebrate by remember { mutableStateOf(false) }
    var rewardTitle by remember { mutableStateOf("🎉 Great Job!") }
    var rewardMessage by remember { mutableStateOf("You finished watching!") }
    var rewardSubtitle by remember { mutableStateOf("Keep learning and exploring!") }

    val decodedTitle = remember(title) {
        try {
            URLDecoder.decode(title, "UTF-8")
        } catch (_: Exception) {
            title
        }
    }

    fun exitPlayer() {
        onBack((System.currentTimeMillis() - openedAtMs) / 1000)
    }

    suspend fun completeVideo() {
        val currentUid = FirebaseAuth.getInstance().currentUser?.uid
        if (currentUid == null) {
            rewardTitle = "🎬 Video Completed!"
            rewardMessage = "Nice watching!"
            rewardSubtitle = "Keep learning!"
            showCelebrate = true
            return
        }

        val firestore = FirebaseFirestore.getInstance()
        val learningProgressManager = LearningProgressManager(firestore)
        val gamificationManager = GamificationManager(firestore)

        val beforeProfile = firestore.collection("gamification")
            .document(currentUid)
            .get()
            .await()
            .toObject(GamificationProfile::class.java)

        val beforeBadges = firestore.collection("gamification")
            .document(currentUid)
            .collection("badges")
            .get()
            .await()
            .toObjects(BadgeUnlock::class.java)
            .map { it.badgeId }
            .toSet()

        val trackingResult = learningProgressManager.trackVideoWatched(
            childUserId = currentUid,
            contentId = cleanVideoId,
            title = decodedTitle,
            topic = decodedTitle
        )

        if (trackingResult.isFailure) {
            rewardTitle = "🎬 Video Completed!"
            rewardMessage = "Nice watching!"
            rewardSubtitle = "Keep learning!"
            showCelebrate = true
            return
        }

        val refreshResult = gamificationManager.refreshGamification(currentUid)
        if (refreshResult.isFailure) {
            rewardTitle = "🎉 Great Job!"
            rewardMessage = "Progress saved!"
            rewardSubtitle = "You're doing amazing!"
            showCelebrate = true
            return
        }

        val afterProfile = firestore.collection("gamification")
            .document(currentUid)
            .get()
            .await()
            .toObject(GamificationProfile::class.java)

        val afterBadges = firestore.collection("gamification")
            .document(currentUid)
            .collection("badges")
            .get()
            .await()
            .toObjects(BadgeUnlock::class.java)

        val unlockedNow = afterBadges.firstOrNull { badge ->
            badge.badgeId !in beforeBadges
        }

        val leveledUpNow =
            (afterProfile?.currentLevel ?: 1) > (beforeProfile?.currentLevel ?: 1)

        when {
            unlockedNow != null -> {
                rewardTitle = "🏅 Badge Unlocked!"
                rewardMessage = unlockedNow.badgeTitle
                rewardSubtitle = unlockedNow.description
                showCelebrate = true
            }

            leveledUpNow -> {
                rewardTitle = "🚀 Level Up!"
                rewardMessage = "You reached Level ${afterProfile?.currentLevel ?: 1}"
                rewardSubtitle = "Your learning power is growing!"
                showCelebrate = true
            }

            else -> {
                rewardTitle = "🎬 Video Completed!"
                rewardMessage = "Awesome watching!"
                rewardSubtitle = "You earned more rewards."
                showCelebrate = true
            }
        }
    }

    val htmlContent = """
        <!DOCTYPE html>
        <html>
        <head>
            <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no">
            <style>
                * { margin: 0; padding: 0; box-sizing: border-box; }
                body { background: #000; overflow: hidden; }
                .container {
                    position: fixed;
                    top: 0; left: 0; right: 0; bottom: 0;
                    display: flex;
                    align-items: center;
                    justify-content: center;
                }
                iframe {
                    width: 100%;
                    aspect-ratio: 16/9;
                    max-height: 100%;
                    border: none;
                }
            </style>
        </head>
        <body>
            <div class="container">
                <iframe
                    src="https://www.youtube-nocookie.com/embed/$cleanVideoId?autoplay=1&rel=0&modestbranding=1&playsinline=1&fs=0"
                    allow="autoplay; encrypted-media"
                    allowfullscreen="false">
                </iframe>
            </div>
        </body>
        </html>
    """.trimIndent()

    BackHandler(enabled = !showCelebrate) {
        exitPlayer()
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            topBar = {
                CenterAlignedTopAppBar(
                    title = { Text(decodedTitle, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                    navigationIcon = {
                        IconButton(onClick = { exitPlayer() }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    }
                )
            },
            bottomBar = {
                Surface(shadowElevation = 8.dp, color = Color.Black) {
                    Button(
                        onClick = { isFinishing = true },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                            .navigationBarsPadding()
                    ) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null)
                        Text(if (isFinishing) " Finishing..." else " I Finished Watching")
                    }
                }
            },
            containerColor = Color.Black
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black)
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                if (loadError) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Surface(
                            color = Color(0xFFB71C1C),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = "Video Cannot Play",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.size(8.dp))
                                Text(
                                    text = "Video ID: $cleanVideoId",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color.White.copy(alpha = 0.6f),
                                    fontSize = 11.sp
                                )
                                Spacer(modifier = Modifier.size(12.dp))
                                Text(
                                    text = "This video may be unavailable or restricted.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color.White.copy(alpha = 0.7f),
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                        Spacer(modifier = Modifier.size(16.dp))
                        Button(onClick = { exitPlayer() }) {
                            Text("Go Back")
                        }
                    }
                } else {
                    AndroidView(
                        factory = { context ->
                            WebView(context).apply {
                                @SuppressLint("SetJavaScriptEnabled")
                                settings.apply {
                                    javaScriptEnabled = true
                                    domStorageEnabled = true
                                    mediaPlaybackRequiresUserGesture = false
                                    loadWithOverviewMode = true
                                    useWideViewPort = true
                                    mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                                }

                                setBackgroundColor(android.graphics.Color.BLACK)

                                webViewClient = object : WebViewClient() {
                                    override fun shouldOverrideUrlLoading(
                                        view: WebView?,
                                        request: WebResourceRequest?
                                    ): Boolean {
                                        val reqUrl = request?.url?.toString() ?: return true
                                        val allowed = reqUrl.contains("youtube.com") ||
                                                reqUrl.contains("youtube-nocookie.com") ||
                                                reqUrl.contains("youtu.be") ||
                                                reqUrl.contains("googlevideo.com") ||
                                                reqUrl.contains("google.com") ||
                                                reqUrl.contains("gstatic.com")
                                        return !allowed
                                    }

                                    override fun onPageStarted(
                                        view: WebView?,
                                        url: String?,
                                        favicon: Bitmap?
                                    ) {
                                        super.onPageStarted(view, url, favicon)
                                        isLoading = true
                                    }

                                    override fun onPageFinished(view: WebView?, url: String?) {
                                        super.onPageFinished(view, url)
                                        isLoading = false
                                    }

                                    override fun onReceivedError(
                                        view: WebView?,
                                        request: WebResourceRequest?,
                                        error: WebResourceError?
                                    ) {
                                        super.onReceivedError(view, request, error)
                                        if (request?.isForMainFrame == true) {
                                            loadError = true
                                        }
                                    }
                                }

                                webChromeClient = WebChromeClient()

                                loadDataWithBaseURL(
                                    "https://www.youtube-nocookie.com",
                                    htmlContent,
                                    "text/html",
                                    "UTF-8",
                                    null
                                )
                            }
                        },
                        modifier = Modifier.fillMaxSize()
                    )

                    if (isLoading) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                CircularProgressIndicator(color = Color.White)
                                Spacer(modifier = Modifier.size(16.dp))
                                Text(
                                    text = "Loading video...",
                                    color = Color.White.copy(alpha = 0.6f)
                                )
                            }
                        }
                    }
                }

                if (isFinishing) {
                    LaunchedEffect(Unit) {
                        completeVideo()
                        isFinishing = false
                    }

                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Card {
                            Column(
                                modifier = Modifier.padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                CircularProgressIndicator()
                                Text("Saving your reward...")
                            }
                        }
                    }
                }
            }
        }

        if (showCelebrate) {
            CelebrationOverlay(
                title = rewardTitle,
                message = rewardMessage,
                subtitle = rewardSubtitle,
                onDismiss = {
                    showCelebrate = false
                    exitPlayer()
                }
            )
        }
    }
}

@Composable
private fun CelebrationOverlay(
    title: String,
    message: String,
    subtitle: String,
    onDismiss: () -> Unit
) {
    val dismissState by rememberUpdatedState(onDismiss)

    LaunchedEffect(title, message, subtitle) {
        delay(3200)
        dismissState()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.12f)),
        contentAlignment = Alignment.Center
    ) {
        SuperConfetti()

        Card(
            modifier = Modifier.padding(24.dp),
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFFFFFBFF)),
            elevation = CardDefaults.cardElevation(defaultElevation = 14.dp)
        ) {
            Column(
                modifier = Modifier.padding(28.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = when {
                        "Badge" in title -> "🏅"
                        "Level" in title -> "🚀"
                        else -> "🎉"
                    },
                    style = MaterialTheme.typography.headlineLarge
                )

                Text(
                    text = title,
                    style = MaterialTheme.typography.headlineSmall,
                    color = Color(0xFF6A1B9A)
                )

                Text(
                    text = message,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                TextButton(onClick = onDismiss) {
                    Text("Awesome!")
                }
            }
        }
    }
}

@Composable
private fun SuperConfetti() {
    val infinite = rememberInfiniteTransition(label = "super_confetti")

    val a by infinite.animateFloat(
        initialValue = -80f,
        targetValue = 1100f,
        animationSpec = infiniteRepeatable(
            animation = keyframes {
                durationMillis = 1700
                1100f at 1700 using FastOutSlowInEasing
            },
            repeatMode = RepeatMode.Restart
        ),
        label = "a"
    )

    val b by infinite.animateFloat(
        initialValue = -160f,
        targetValue = 1150f,
        animationSpec = infiniteRepeatable(
            animation = keyframes {
                durationMillis = 2200
                1150f at 2200 using FastOutSlowInEasing
            },
            repeatMode = RepeatMode.Restart
        ),
        label = "b"
    )

    val c by infinite.animateFloat(
        initialValue = -120f,
        targetValue = 1180f,
        animationSpec = infiniteRepeatable(
            animation = keyframes {
                durationMillis = 1900
                1180f at 1900 using FastOutSlowInEasing
            },
            repeatMode = RepeatMode.Restart
        ),
        label = "c"
    )

    Box(modifier = Modifier.fillMaxSize()) {
        CelebrationPiece(10.dp, a.dp, Color(0xFFFF1744), 16.dp)
        CelebrationPiece(40.dp, (b * 0.7f).dp, Color(0xFFFFC400), 12.dp)
        CelebrationPiece(80.dp, c.dp, Color(0xFF00E5FF), 14.dp)
        CelebrationPiece(120.dp, (a * 0.8f).dp, Color(0xFF76FF03), 12.dp)
        CelebrationPiece(160.dp, b.dp, Color(0xFFE040FB), 16.dp)
        CelebrationPiece(200.dp, (c * 0.85f).dp, Color(0xFFFF9100), 12.dp)
        CelebrationPiece(240.dp, (a * 1.05f).dp, Color(0xFF18FFFF), 14.dp)
        CelebrationPiece(280.dp, b.dp, Color(0xFFFF4081), 16.dp)
        CelebrationPiece(320.dp, (c * 0.9f).dp, Color(0xFF69F0AE), 12.dp)
        CelebrationPiece(360.dp, (a * 0.95f).dp, Color(0xFF7C4DFF), 14.dp)
    }
}

@Composable
private fun CelebrationPiece(
    x: androidx.compose.ui.unit.Dp,
    y: androidx.compose.ui.unit.Dp,
    color: Color,
    size: androidx.compose.ui.unit.Dp
) {
    Box(
        modifier = Modifier
            .offset(x = x, y = y)
            .size(size)
            .background(color, CircleShape)
    )
}