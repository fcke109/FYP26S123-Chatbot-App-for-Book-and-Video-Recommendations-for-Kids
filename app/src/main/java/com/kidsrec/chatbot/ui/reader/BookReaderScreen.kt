package com.kidsrec.chatbot.ui.reader

import android.annotation.SuppressLint
import android.webkit.WebResourceRequest
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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.kidsrec.chatbot.data.model.BadgeUnlock
import com.kidsrec.chatbot.data.model.GamificationProfile
import com.kidsrec.chatbot.data.repository.GamificationManager
import com.kidsrec.chatbot.data.repository.LearningProgressManager
import kotlinx.coroutines.delay
import kotlinx.coroutines.tasks.await

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookReaderScreen(
    url: String,
    onBack: (durationSeconds: Long) -> Unit
) {
    // Records when the reader was opened so reading duration can be calculated later
    val openedAtMs = remember { System.currentTimeMillis() }
    // Holds the WebView instance so back navigation can be controlled
    var webView by remember { mutableStateOf<WebView?>(null) }
    // Tracks whether the WebView has browsing history to go back to
    var canGoBack by remember { mutableStateOf(false) }
    // Tracks whether the app is currently saving the completed reading reward
    var isFinishing by remember { mutableStateOf(false) }
    // Controls whether the reward/celebration overlay should be shown
    var showCelebrate by remember { mutableStateOf(false) }
    // Reward message states displayed after completing a book
    var rewardTitle by remember { mutableStateOf("🎉 Great Job!") }
    var rewardMessage by remember { mutableStateOf("You finished reading!") }
    var rewardSubtitle by remember { mutableStateOf("Keep going, superstar reader!") }

    // Calculates reading duration and exits the reader screen
    fun exitReader() {
        onBack((System.currentTimeMillis() - openedAtMs) / 1000)
    }

    // Saves reading progress, refreshes gamification, and prepares reward feedback
    suspend fun completeBook() {
        val currentUid = FirebaseAuth.getInstance().currentUser?.uid
        if (currentUid == null) {
            rewardTitle = "📚 Book Completed!"
            rewardMessage = "Nice reading!"
            rewardSubtitle = "Keep going!"
            showCelebrate = true
            return
        }

        val firestore = FirebaseFirestore.getInstance()
        val learningProgressManager = LearningProgressManager(firestore)
        val gamificationManager = GamificationManager(firestore)

        // Captures gamification state before saving progress to detect level-ups
        val beforeProfile = firestore.collection("gamification")
            .document(currentUid)
            .get()
            .await()
            .toObject(GamificationProfile::class.java)

        // Captures existing badges before progress update to detect newly unlocked badges
        val beforeBadges = firestore.collection("gamification")
            .document(currentUid)
            .collection("badges")
            .get()
            .await()
            .toObjects(BadgeUnlock::class.java)
            .map { it.badgeId }
            .toSet()

        // Uses the URL hash as a simple content identifier for tracking
        val contentId = url.hashCode().toString()

        // Records that the child completed reading this book
        val trackingResult = learningProgressManager.trackBookRead(
            childUserId = currentUid,
            contentId = contentId,
            title = "Completed Story Book",
            topic = "book",
            readingLevel = "Beginner"
        )

        // If progress tracking fails, still show a friendly completion message
        if (trackingResult.isFailure) {
            rewardTitle = "📚 Book Completed!"
            rewardMessage = "Nice reading!"
            rewardSubtitle = "Keep going!"
            showCelebrate = true
            return
        }

        // Refreshes points, badges, streaks, or level after reading progress is saved
        val refreshResult = gamificationManager.refreshGamification(currentUid)
        // If gamification refresh fails, show a fallback reward message
        if (refreshResult.isFailure) {
            rewardTitle = "🎉 Great Job!"
            rewardMessage = "Progress saved locally!"
            rewardSubtitle = "You're doing amazing!"
            showCelebrate = true
            return
        }

        // Loads updated gamification profile after refresh
        val afterProfile = firestore.collection("gamification")
            .document(currentUid)
            .get()
            .await()
            .toObject(GamificationProfile::class.java)

        // Loads updated badges after refresh
        val afterBadges = firestore.collection("gamification")
            .document(currentUid)
            .collection("badges")
            .get()
            .await()
            .toObjects(BadgeUnlock::class.java)

        // Finds a badge that was unlocked during this reading completion
        val unlockedNow = afterBadges.firstOrNull { badge ->
            badge.badgeId !in beforeBadges
        }

        // Checks whether the child moved to a higher level
        val leveledUpNow =
            (afterProfile?.currentLevel ?: 1) > (beforeProfile?.currentLevel ?: 1)

        // Chooses the most meaningful reward message to display
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
                rewardSubtitle = "Your reading adventure is growing!"
                showCelebrate = true
            }

            else -> {
                rewardTitle = "📚 Book Completed!"
                rewardMessage = "Awesome reading!"
                rewardSubtitle = "You earned more rewards."
                showCelebrate = true
            }
        }
    }

    // Handles Android back press while the celebration overlay is not visible
    BackHandler(enabled = !showCelebrate) {
        if (canGoBack) {
            webView?.goBack()
        } else {
            exitReader()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            topBar = {
                CenterAlignedTopAppBar(
                    // Top app bar with reader title and close button
                    title = { Text("Visual Storybook") },
                    navigationIcon = {
                        IconButton(onClick = { exitReader() }) {
                            Icon(Icons.Default.Close, contentDescription = "Close")
                        }
                    }
                )
            },
            bottomBar = {
                // Bottom action button for marking the book as completed
                Surface(shadowElevation = 8.dp) {
                    Button(
                        onClick = {
                            isFinishing = true
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                            .navigationBarsPadding()
                    ) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null)
                        Text(
                            text = if (isFinishing) " Finishing..." else " I Finished Reading"
                        )
                    }
                }
            }
        ) { padding ->
            Box(modifier = Modifier.fillMaxSize()) {
                Column(
                    modifier = Modifier
                        .padding(padding)
                        .fillMaxSize()
                ) {
                    // Embeds an Android WebView inside Compose to display online reading content
                    AndroidView(
                        factory = { context ->
                            WebView(context).apply {
                                webView = this

                                // Enables required WebView settings for online book readers
                                @SuppressLint("SetJavaScriptEnabled")
                                settings.javaScriptEnabled = true
                                settings.domStorageEnabled = true
                                settings.loadWithOverviewMode = true
                                settings.useWideViewPort = true

                                webViewClient = object : WebViewClient() {
                                    override fun shouldOverrideUrlLoading(
                                        view: WebView?,
                                        request: WebResourceRequest?
                                    ): Boolean {
                                        val target = request?.url?.toString().orEmpty()

                                        // Allows only known safe reading/content domains
                                        val allowed = listOf(
                                            "childrenslibrary.org",
                                            "archive.org",
                                            "ia80", "ia60", "ia90", "ia70",
                                            "openlibrary.org",
                                            "storyweaver.org.in",
                                            "covers.openlibrary.org"
                                        )

                                        // Blocks navigation to any domain outside the approved list
                                        return !allowed.any { domain -> target.contains(domain) }
                                    }

                                    override fun onPageFinished(view: WebView?, url: String?) {
                                        super.onPageFinished(view, url)
                                        // Updates back-navigation state after each page finishes loading
                                        canGoBack = view?.canGoBack() ?: false
                                    }
                                }

                                // Loads the selected book/story URL
                                loadUrl(url)
                            }
                        },
                        modifier = Modifier.fillMaxSize()
                    )
                }

                // Shows a loading overlay while reading completion is being saved
                if (isFinishing) {
                    LaunchedEffect(Unit) {
                        completeBook()
                        isFinishing = false
                    }

                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surface
                            )
                        ) {
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

        // Displays reward, badge, or level-up celebration after completion
        if (showCelebrate) {
            CelebrationOverlay(
                title = rewardTitle,
                message = rewardMessage,
                subtitle = rewardSubtitle,
                onDismiss = {
                    showCelebrate = false
                    exitReader()
                }
            )
        }
    }
}

// Overlay shown after a child finishes reading and earns progress/rewards
@Composable
private fun CelebrationOverlay(
    title: String,
    message: String,
    subtitle: String,
    onDismiss: () -> Unit
) {
    val dismissState by rememberUpdatedState(onDismiss)

    // Automatically dismisses the celebration after a short delay
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
        // Animated confetti background
        SuperConfetti()

        // Central reward card
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
                // Selects an emoji based on the reward type
                Text(
                    text = when {
                        "Badge" in title -> "🏅"
                        "Level" in title -> "🚀"
                        else -> "🎉"
                    }
                )

                Text(
                    text = title,
                    style = MaterialTheme.typography.headlineSmall,
                    color = Color(0xFF6A1B9A)
                )

                Text(
                    text = message,
                    style = MaterialTheme.typography.titleMedium
                )

                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // Allows the child to manually close the celebration
                TextButton(onClick = onDismiss) {
                    Text("Awesome!")
                }
            }
        }
    }
}

// Creates an animated confetti effect for the celebration overlay
@Composable
private fun SuperConfetti() {
    val infinite = rememberInfiniteTransition(label = "super_confetti")

    // First confetti animation path
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

    // Second confetti animation path
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

    // Third confetti animation path
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

    // Places multiple colorful animated confetti pieces across the screen
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

// Single animated confetti piece used by the celebration overlay
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