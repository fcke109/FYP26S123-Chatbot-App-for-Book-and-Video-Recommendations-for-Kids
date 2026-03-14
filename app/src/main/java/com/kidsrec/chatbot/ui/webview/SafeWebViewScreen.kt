package com.kidsrec.chatbot.ui.webview

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.net.http.SslError
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.kidsrec.chatbot.data.model.RecommendationType
import com.kidsrec.chatbot.ui.favorites.FavoritesViewModel
import java.util.regex.Pattern

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
    val webViewRef = remember { mutableStateOf<WebView?>(null) }
    val favoriteItems by favoritesViewModel.favorites.collectAsState()
    val isFavorited = remember(favoriteItems, itemId) { favoriteItems.any { it.itemId == itemId } }

    val safeKeywords = listOf(
        "youtube", "youtu.be", "google", "gstatic", "archive.org", 
        "openlibrary", "storyweaver", "icdlbooks", "childrenslibrary", 
        "cloudflare", "googleapis", "firebaseapp", "doubleclick"
    )

    fun isUrlAllowed(urlToCheck: String): Boolean {
        val lowerUrl = urlToCheck.lowercase()
        if (lowerUrl.startsWith("data:") || lowerUrl.startsWith("blob:") || lowerUrl == "about:blank") return true
        return safeKeywords.any { lowerUrl.contains(it) }
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
                    Text(
                        text = title, 
                        fontSize = 16.sp, 
                        fontWeight = FontWeight.Bold, 
                        color = Color.White, 
                        maxLines = 1, 
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = if (isVideo) "Safe Video" else "Safe Story",
                        fontSize = 10.sp,
                        color = Color.White.copy(alpha = 0.8f)
                    )
                }

                // Add Favorite Toggle in WebView
                if (itemId.isNotBlank() && itemId != "admin") {
                    IconButton(onClick = {
                        if (isFavorited) {
                            favoritesViewModel.removeFavorite(itemId)
                        } else {
                            favoritesViewModel.addFavorite(
                                itemId = itemId,
                                type = if (isVideo) RecommendationType.VIDEO else RecommendationType.BOOK,
                                title = title,
                                description = description,
                                imageUrl = imageUrl,
                                url = url
                            )
                        }
                    }) {
                        Icon(
                            imageVector = if (isFavorited) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            contentDescription = "Favorite",
                            tint = if (isFavorited) Color.White else Color.White.copy(alpha = 0.7f)
                        )
                    }
                }

                IconButton(onClick = { webViewRef.value?.reload() }) {
                    Icon(Icons.Default.Refresh, "Refresh", tint = Color.White)
                }
            }
        }

        if (isLoading) {
            LinearProgressIndicator(
                progress = { loadingProgress / 100f },
                modifier = Modifier.fillMaxWidth().height(2.dp),
                color = Color.White,
                trackColor = Color.White.copy(alpha = 0.2f)
            )
        }

        Box(modifier = Modifier.weight(1f)) {
            AndroidView(
                factory = { context ->
                    WebView(context).apply {
                        webViewRef.value = this
                        
                        // Using software rendering for emulator stability
                        setLayerType(android.view.View.LAYER_TYPE_SOFTWARE, null)
                        
                        @SuppressLint("SetJavaScriptEnabled")
                        settings.apply {
                            javaScriptEnabled = true
                            domStorageEnabled = true
                            mediaPlaybackRequiresUserGesture = false
                            mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                            userAgentString = "Mozilla/5.0 (Linux; Android 13) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/116.0.0.0 Mobile Safari/537.36"
                        }

                        webViewClient = object : WebViewClient() {
                            override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                                isLoading = true
                            }
                            override fun onPageFinished(view: WebView?, url: String?) {
                                isLoading = false
                            }
                            override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                                val reqUrl = request?.url?.toString() ?: return true
                                if (request.isForMainFrame && !isUrlAllowed(reqUrl)) return true
                                return false
                            }
                            override fun onReceivedSslError(view: WebView?, handler: SslErrorHandler?, error: SslError?) {
                                handler?.proceed()
                            }
                        }
                        
                        webChromeClient = object : WebChromeClient() {
                            override fun onProgressChanged(view: WebView?, newProgress: Int) {
                                loadingProgress = newProgress
                            }
                            override fun onPermissionRequest(request: PermissionRequest) {
                                request.deny()
                            }
                        }

                        val videoId = extractYoutubeId(url)
                        val finalUrl = if (videoId != null) {
                            "https://www.youtube.com/embed/$videoId?autoplay=1&rel=0&modestbranding=1"
                        } else {
                            url.replace("/details/", "/embed/").replace("http://", "https://")
                        }
                        loadUrl(finalUrl)
                    }
                },
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

private fun extractYoutubeId(url: String): String? {
    if (url.isBlank()) return null
    val patterns = listOf(
        "v=([a-zA-Z0-9_-]{11})",
        "youtu\\.be/([a-zA-Z0-9_-]{11})",
        "embed/([a-zA-Z0-9_-]{11})",
        "shorts/([a-zA-Z0-9_-]{11})"
    )
    for (p in patterns) {
        val matcher = Pattern.compile(p).matcher(url)
        if (matcher.find()) return matcher.group(1)
    }
    return null
}
