package com.kidsrec.chatbot.ui.webview

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.util.Log
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import java.net.URLDecoder

@Composable
fun SafeWebViewScreen(
    url: String,
    title: String,
    isVideo: Boolean,
    onClose: (durationSeconds: Long) -> Unit
) {
    val context = LocalContext.current

    val decodedTitle = remember(title) {
        try {
            URLDecoder.decode(title, "UTF-8")
        } catch (_: Exception) {
            title
        }
    }

    val safeUrl = remember(url) {
        url.trim().replace("http://", "https://")
    }

    val openedAtMs = remember { System.currentTimeMillis() }

    fun watchedSeconds(): Long {
        return (System.currentTimeMillis() - openedAtMs) / 1000
    }

    BackHandler {
        onClose(watchedSeconds())
    }

    // -------- VIDEO FALLBACK MODE --------
    // If a video URL reaches this screen, give the user a safe and workable fallback
    if (isVideo) {
        val youtubeLink = remember(safeUrl) { YouTubeLinkHelper.build(safeUrl) }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = decodedTitle,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = if (youtubeLink != null) {
                        "This video cannot be shown in the safe in-app reader. Open the workable video link below."
                    } else {
                        "This video link is not supported."
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(20.dp))

                if (youtubeLink != null) {
                    Button(
                        onClick = {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(youtubeLink.watchUrl))
                            context.startActivity(intent)
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = "Play"
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Open Video")
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Button(
                        onClick = { onClose(watchedSeconds()) }
                    ) {
                        Text("Go Back")
                    }
                } else {
                    Button(
                        onClick = { onClose(watchedSeconds()) }
                    ) {
                        Text("Go Back")
                    }
                }
            }
        }
        return
    }

    // -------- NORMAL SAFE WEBVIEW MODE --------
    var isLoading by remember { mutableStateOf(true) }
    var loadingProgress by remember { mutableStateOf(0) }
    var webView by remember { mutableStateOf<WebView?>(null) }
    var canGoBack by remember { mutableStateOf(false) }
    var blockedUrl by remember { mutableStateOf<String?>(null) }

    val allowedDomains = listOf(
        "storyweaver.org",
        "storyweaver.org.in",
        "archive.org",
        "openlibrary.org",
        "covers.openlibrary.org",
        "childrenslibrary.org",
        "books.google."
    )

    fun isUrlAllowed(urlToCheck: String): Boolean {
        return try {
            val host = Uri.parse(urlToCheck).host?.lowercase() ?: return false
            allowedDomains.any { domain ->
                host == domain || host.endsWith(".$domain") || host.contains(domain)
            }
        } catch (e: Exception) {
            false
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            webView?.stopLoading()
            webView?.destroy()
            webView = null
        }
    }

    BackHandler {
        if (canGoBack) {
            webView?.goBack()
        } else {
            onClose(watchedSeconds())
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Surface(
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = {
                            if (canGoBack) {
                                webView?.goBack()
                            } else {
                                onClose(watchedSeconds())
                            }
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White
                        )
                    }

                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 8.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = "Safe",
                                tint = Color.White,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Safe Visual Reader",
                                fontSize = 10.sp,
                                color = Color.White.copy(alpha = 0.8f)
                            )
                        }

                        Text(
                            text = decodedTitle,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    IconButton(
                        onClick = { onClose(watchedSeconds()) }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = Color.White
                        )
                    }
                }

                if (isLoading) {
                    LinearProgressIndicator(
                        progress = { loadingProgress / 100f },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(3.dp),
                        color = Color.White,
                        trackColor = Color.White.copy(alpha = 0.3f)
                    )
                }
            }
        }

        if (blockedUrl != null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "This page is not allowed in the safe reader.",
                        style = MaterialTheme.typography.titleMedium
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = blockedUrl ?: "",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(onClick = { onClose(watchedSeconds()) }) {
                        Text("Go Back")
                    }
                }
            }
        } else {
            Box(modifier = Modifier.weight(1f)) {
                AndroidView(
                    factory = { webContext ->
                        WebView(webContext).apply {
                            webView = this

                            @SuppressLint("SetJavaScriptEnabled")
                            settings.apply {
                                javaScriptEnabled = true
                                domStorageEnabled = true
                                databaseEnabled = true
                                loadWithOverviewMode = true
                                useWideViewPort = true
                                setSupportZoom(true)
                                builtInZoomControls = false
                                displayZoomControls = false
                                allowFileAccess = false
                                allowContentAccess = false
                                mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW
                                mediaPlaybackRequiresUserGesture = true
                            }

                            webViewClient = object : WebViewClient() {
                                override fun shouldOverrideUrlLoading(
                                    view: WebView?,
                                    request: WebResourceRequest?
                                ): Boolean {
                                    val reqUrl = request?.url?.toString() ?: return true
                                    Log.d("KidsRecWebView", "Requested URL: $reqUrl")

                                    val allowed = isUrlAllowed(reqUrl)
                                    if (!allowed) {
                                        blockedUrl = reqUrl
                                    }
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
                                    canGoBack = view?.canGoBack() ?: false
                                }
                            }

                            webChromeClient = object : WebChromeClient() {
                                override fun onProgressChanged(view: WebView?, newProgress: Int) {
                                    loadingProgress = newProgress
                                    if (newProgress == 100) {
                                        isLoading = false
                                    }
                                }
                            }

                            if (isUrlAllowed(safeUrl)) {
                                loadUrl(safeUrl)
                            } else {
                                blockedUrl = safeUrl
                            }
                        }
                    },
                    update = { view ->
                        if (blockedUrl == null && view.url != safeUrl && isUrlAllowed(safeUrl)) {
                            view.loadUrl(safeUrl)
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                )

                if (isLoading && loadingProgress < 30) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(MaterialTheme.colorScheme.background),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "Opening Story...",
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

data class VideoLinkInfo(
    val videoId: String,
    val embedUrl: String,
    val watchUrl: String,
    val thumbnailUrl: String
)

object YouTubeLinkHelper {

    fun extractVideoId(rawUrl: String): String? {
        val url = rawUrl.trim()

        val patterns = listOf(
            Regex("""youtu\.be/([A-Za-z0-9_-]{11})"""),
            Regex("""youtube\.com/watch\?v=([A-Za-z0-9_-]{11})"""),
            Regex("""youtube\.com/embed/([A-Za-z0-9_-]{11})"""),
            Regex("""youtube\.com/shorts/([A-Za-z0-9_-]{11})"""),
            Regex("""youtube\.com/live/([A-Za-z0-9_-]{11})"""),
            Regex("""[?&]v=([A-Za-z0-9_-]{11})""")
        )

        for (pattern in patterns) {
            val match = pattern.find(url)
            if (match != null) return match.groupValues[1]
        }

        return null
    }

    fun build(rawUrl: String): VideoLinkInfo? {
        val videoId = extractVideoId(rawUrl) ?: return null

        return VideoLinkInfo(
            videoId = videoId,
            embedUrl = "https://www.youtube.com/embed/$videoId?playsinline=1&rel=0&modestbranding=1",
            watchUrl = "https://www.youtube.com/watch?v=$videoId",
            thumbnailUrl = "https://img.youtube.com/vi/$videoId/hqdefault.jpg"
        )
    }
}