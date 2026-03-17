package com.kidsrec.chatbot.ui.webview

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.PlayerConstants
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.YouTubePlayer
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.listeners.AbstractYouTubePlayerListener
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.options.IFramePlayerOptions
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.views.YouTubePlayerView

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun YouTubePlayerScreen(
    videoId: String,
    title: String,
    onBack: () -> Unit
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    var youtubePlayerInstance by remember { mutableStateOf<YouTubePlayer?>(null) }
    val cleanVideoId = videoId.trim()

    // Handle video change if the screen is reused
    LaunchedEffect(cleanVideoId, youtubePlayerInstance) {
        youtubePlayerInstance?.loadVideo(cleanVideoId, 0f)
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(title, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        containerColor = Color.Black
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
                .padding(innerPadding),
            verticalArrangement = Arrangement.Center
        ) {
            AndroidView(
                factory = { context ->
                    YouTubePlayerView(context).apply {
                        lifecycleOwner.lifecycle.addObserver(this)

                        val options = IFramePlayerOptions.Builder()
                            .controls(1)
                            .fullscreen(0)
                            .rel(0)
                            .build()

                        enableAutomaticInitialization = false
                        initialize(object : AbstractYouTubePlayerListener() {
                            override fun onReady(youTubePlayer: YouTubePlayer) {
                                youtubePlayerInstance = youTubePlayer
                                Log.d("YouTubePlayer", "Player ready for: $cleanVideoId")
                            }

                            override fun onError(youTubePlayer: YouTubePlayer, error: PlayerConstants.PlayerError) {
                                Log.e("YouTubePlayer", "Player error ($error) for: $cleanVideoId")
                            }
                        }, options)
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 9f)
            )

            Spacer(modifier = Modifier.height(24.dp))
            
            Text(
                text = "Playing kid-safe video",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.6f),
                modifier = Modifier.padding(horizontal = 16.dp)
            )
        }
    }
}
