package com.kidsrec.chatbot.ui.webview

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.net.http.SslError
import android.util.Log
import android.view.View
import android.webkit.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
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
import com.kidsrec.chatbot.data.model.RecommendationType
import com.kidsrec.chatbot.ui.favorites.FavoritesViewModel
import java.util.regex.Pattern

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SafeWebViewScreen(
    url: String,
    title: String,
    isVideo: Boolean,
    itemId: String,
    imageUrl: String,
    description: String,
    favoritesViewModel: FavoritesViewModel,
    onClose: () -> Unit
) {
    var isLoading by remember { mutableStateOf(true) }
    var loadingProgress by remember { mutableStateOf(0) }
    var hasError by remember { mutableStateOf(false) }
    var isBlocked by remember { mutableStateOf(false) }
    val webViewRef = remember { mutableStateOf<WebView?>(null) }

    val favoriteItems by favoritesViewModel.favorites.collectAsState()
    val isFavorited = remember(favoriteItems, itemId) { favoriteItems.any { it.itemId == itemId } }

    val allowedVideoDomains = listOf(
        "youtubekids.com", "youtube.com", "youtu.be", "youtube-nocookie.com",
        "googlevideo.com", "ytimg.com", "gstatic.com", "googleapis.com", "google.com"
    )
    val allowedBookDomains = listOf(
        "archive.org", "openlibrary.org", "storyweaver.org", "storyweaver.org.in",
        "childrenslibrary.org", "icdlbooks.org", "books.google."
    )

    fun isUrlAllowed(urlToCheck: String): Boolean {
        val lower = urlToCheck.lowercase()
        if (lower.startsWith("data:") || lower.startsWith("blob:") || lower == "about:blank") return true
        val allAllowed = if (isVideo) allowedVideoDomains else allowedBookDomains
        return allAllowed.any { lower.contains(it) }
    }

    // Build the final internal URL
    val videoId = remember(url) { extractYoutubeIdInternal(url) }
    
    // Create an iframe HTML string for the video. This is the most reliable way to play YouTube in WebView.
    val videoHtml = remember(videoId) {
        if (videoId != null) {
            """
            <!DOCTYPE html>
            <html>
            <head>
                <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no">
                <style>
                    body, html { margin: 0; padding: 0; width: 100%; height: 100%; background-color: black; overflow: hidden; }
                    .video-container { position: relative; width: 100%; height: 100%; }
                    iframe { position: absolute; top: 0; left: 0; width: 100%; height: 100%; border: 0; }
                </style>
            </head>
            <body>
                <div class="video-container">
                    <iframe src="https://www.youtube.com/embed/$videoId?autoplay=1&rel=0&modestbranding=1&playsinline=1&enablejsapi=1" 
                            allow="autoplay; encrypted-media; picture-in-picture" allowfullscreen></iframe>
                </div>
            </body>
            </html>
            """.trimIndent()
        } else null
    }

    val bookUrl = remember(url) {
        url.replace("/details/", "/embed/").replace("http://", "https://")
    }

    LaunchedEffect(url, isVideo, videoId) {
        if (isVideo && videoId == null) {
            hasError = true
        }
    }

    Column(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        Surface(
            color = if (isVideo) Color(0xFFC62828) else MaterialTheme.colorScheme.primary,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onClose) {
                    Icon(Icons.Default.Close, "Close", tint = Color.White)
                }
                Column(modifier = Modifier.weight(1f).padding(horizontal = 8.dp)) {
                    Text(text = title, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(text = if (isVideo) "Safe Video Player" else "Safe Book Reader", fontSize = 10.sp, color = Color.White.copy(alpha = 0.8f))
                }

                if (itemId.isNotBlank() && itemId != "admin") {
                    IconButton(onClick = {
                        if (isFavorited) favoritesViewModel.removeFavorite(itemId)
                        else favoritesViewModel.addFavorite(itemId, if (isVideo) RecommendationType.VIDEO else RecommendationType.BOOK, title, description, imageUrl, url)
                    }) {
                        Icon(imageVector = if (isFavorited) Icons.Default.Favorite else Icons.Default.FavoriteBorder, contentDescription = "Favorite", tint = Color.White)
                    }
                }
                IconButton(onClick = { webViewRef.value?.reload() }) {
                    Icon(Icons.Default.Refresh, "Refresh", tint = Color.White)
                }
            }
        }

        Box(modifier = Modifier.weight(1f)) {
            if (hasError) {
                FallbackMessage(isVideo = isVideo, isBlocked = false)
            } else if (isBlocked) {
                FallbackMessage(isVideo = isVideo, isBlocked = true)
            } else {
                AndroidView(
                    factory = { ctx ->
                        WebView(ctx).apply {
                            webViewRef.value = this
                            
                            // Hardware acceleration is required for video playback
                            setLayerType(View.LAYER_TYPE_HARDWARE, null)
                            
                            @SuppressLint("SetJavaScriptEnabled")
                            settings.apply {
                                javaScriptEnabled = true
                                domStorageEnabled = true
                                databaseEnabled = true
                                mediaPlaybackRequiresUserGesture = false
                                mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                                cacheMode = WebSettings.LOAD_DEFAULT
                                loadWithOverviewMode = true
                                useWideViewPort = true
                                userAgentString = "Mozilla/5.0 (Linux; Android 13; Pixel 7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/116.0.0.0 Mobile Safari/537.36"
                            }
                            
                            webViewClient = object : WebViewClient() {
                                override fun onPageStarted(v: WebView?, u: String?, f: Bitmap?) {
                                    isLoading = true
                                    hasError = false
                                    isBlocked = false
                                }
                                override fun onPageFinished(v: WebView?, u: String?) {
                                    isLoading = false
                                }
                                override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                                    val reqUrl = request?.url?.toString() ?: return true
                                    val lower = reqUrl.lowercase()

                                    if (reqUrl.startsWith("intent://") || reqUrl.startsWith("youtube://") || reqUrl.startsWith("vnd.youtube:")) {
                                        return true
                                    }

                                    if (isVideo) {
                                        val isSafe = lower.contains("/embed/") || 
                                                     lower.contains("youtube.com") || 
                                                     lower.contains("youtube-nocookie.com") || 
                                                     lower.contains("gstatic.com") || 
                                                     lower.contains("googlevideo.com") ||
                                                     lower.contains("google.com/generate_204")
                                        
                                        val isRestricted = (lower.contains("youtube.com") || lower.contains("youtu.be")) && 
                                                           (lower.endsWith("youtube.com/") || lower.contains("/feed/") || lower.contains("/channel/") || (lower.contains("/watch") && !lower.contains("/embed/")))
                                        
                                        if (!isSafe || isRestricted) return true
                                    } else {
                                        if (!isUrlAllowed(reqUrl)) {
                                            isBlocked = true
                                            return true
                                        }
                                    }
                                    return false
                                }
                                override fun onReceivedError(v: WebView?, r: WebResourceRequest?, e: WebResourceError?) {
                                    if (r?.isForMainFrame == true) hasError = true
                                }
                                override fun onReceivedSslError(v: WebView?, h: SslErrorHandler?, e: SslError?) {
                                    h?.proceed()
                                }
                            }
                            
                            webChromeClient = WebChromeClient()

                            if (isVideo && videoHtml != null) {
                                // loadDataWithBaseURL ensures the player sees "youtube.com" as the host, fixing the unavailability error.
                                loadDataWithBaseURL("https://www.youtube.com", videoHtml, "text/html", "UTF-8", null)
                            } else {
                                loadUrl(bookUrl)
                            }
                        }
                    },
                    update = { view ->
                        // No specific update logic needed as factory handles the initial state
                        // and LaunchedEffect handles the validation.
                    },
                    modifier = Modifier.fillMaxSize()
                )
                if (isLoading) {
                    LinearProgressIndicator(progress = { loadingProgress / 100f }, modifier = Modifier.fillMaxWidth().height(2.dp), color = Color.White)
                }
            }
        }
    }
}

@Composable
fun FallbackMessage(isVideo: Boolean, isBlocked: Boolean) {
    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(Icons.Default.ErrorOutline, null, modifier = Modifier.size(64.dp), tint = Color.Gray)
        Spacer(Modifier.height(16.dp))
        Text(
            text = when {
                isBlocked -> "Access to this page is restricted for safety."
                isVideo -> "This video cannot be played in-app. Please choose another approved video."
                else -> "This story cannot be loaded. Please try again or choose another story."
            },
            color = Color.White,
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.bodyLarge
        )
    }
}

private fun extractYoutubeIdInternal(url: String): String? {
    if (url.isBlank()) return null
    val patterns = listOf(
        "v=([a-zA-Z0-9_-]{11})",
        "youtu\\.be/([a-zA-Z0-9_-]{11})",
        "embed/([a-zA-Z0-9_-]{11})",
        "shorts/([a-zA-Z0-9_-]{11})",
        "youtubekids\\.com/watch\\?v=([a-zA-Z0-9_-]{11})",
        "youtubekids\\.com/embed/([a-zA-Z0-9_-]{11})"
    )
    for (p in patterns) {
        val matcher = Pattern.compile(p).matcher(url)
        if (matcher.find()) return matcher.group(1)
    }
    return null
}
