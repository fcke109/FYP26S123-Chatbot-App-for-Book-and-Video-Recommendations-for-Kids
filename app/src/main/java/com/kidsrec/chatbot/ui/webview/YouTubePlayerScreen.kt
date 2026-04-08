package com.kidsrec.chatbot.ui.webview

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.util.Log
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
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

    val decodedTitle = remember(title) {
        try {
            URLDecoder.decode(title, "UTF-8")
        } catch (_: Exception) {
            title
        }
    }

    // Full HTML page with embedded YouTube player that fills the view
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

    Log.d("YouTubePlayer", "Loading video ID: $cleanVideoId")

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(decodedTitle, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                navigationIcon = {
                    IconButton(onClick = { onBack((System.currentTimeMillis() - openedAtMs) / 1000) }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
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
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Video ID: $cleanVideoId",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.White.copy(alpha = 0.6f),
                                fontSize = 11.sp
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "This video may be unavailable or restricted.",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.White.copy(alpha = 0.7f),
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(onClick = { onBack((System.currentTimeMillis() - openedAtMs) / 1000) }) {
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
                                    // Block ALL navigation — keep kids in app
                                    val reqUrl = request?.url?.toString() ?: return true
                                    val allowed = reqUrl.contains("youtube.com") ||
                                            reqUrl.contains("youtube-nocookie.com") ||
                                            reqUrl.contains("youtu.be") ||
                                            reqUrl.contains("googlevideo.com") ||
                                            reqUrl.contains("google.com") ||
                                            reqUrl.contains("gstatic.com")
                                    if (!allowed) {
                                        Log.d("YouTubePlayer", "Blocked navigation to: $reqUrl")
                                    }
                                    return !allowed
                                }

                                override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
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
                                        Log.e("YouTubePlayer", "WebView error: ${error?.description}")
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
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "Loading video...",
                                color = Color.White.copy(alpha = 0.6f)
                            )
                        }
                    }
                }
            }
        }
    }
}
