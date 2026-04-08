package com.kidsrec.chatbot.ui.webview

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.util.Log
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.activity.compose.BackHandler
import androidx.compose.runtime.*
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

    // Videos should be routed to YouTubePlayerScreen via navigation.
    // No external app launch — force in-app player for safety.

    // If somehow a video URL reaches here, show a message and close
    if (isVideo) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "This video should be opened in the safe player.",
                    style = MaterialTheme.typography.titleMedium
                )
                Spacer(modifier = Modifier.height(16.dp))
                Button(onClick = { onClose(0) }) {
                    Text("Go Back")
                }
            }
        }
        return
    }

    var isLoading by remember { mutableStateOf(true) }
    var loadingProgress by remember { mutableStateOf(0) }
    var webView by remember { mutableStateOf<WebView?>(null) }
    var canGoBack by remember { mutableStateOf(false) }

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
            val host = android.net.Uri.parse(urlToCheck).host?.lowercase() ?: return false
            allowedDomains.any { domain ->
                host == domain || host.endsWith(".$domain")
            }
        } catch (e: Exception) {
            false
        }
    }

    val safeUrl = remember(url) {
        url.trim().replace("http://", "https://")
    }

    val openedAtMs = remember { System.currentTimeMillis() }

    BackHandler {
        if (canGoBack) {
            webView?.goBack()
        } else {
            onClose((System.currentTimeMillis() - openedAtMs) / 1000)
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
                                onClose((System.currentTimeMillis() - openedAtMs) / 1000)
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

                    IconButton(onClick = { onClose((System.currentTimeMillis() - openedAtMs) / 1000) }) {
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
                        }

                        webViewClient = object : WebViewClient() {
                            override fun shouldOverrideUrlLoading(
                                view: WebView?,
                                request: WebResourceRequest?
                            ): Boolean {
                                val reqUrl = request?.url?.toString() ?: return true
                                Log.d("KidsRecWebView", "Requested URL: $reqUrl")
                                return !isUrlAllowed(reqUrl)
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

                        loadUrl(safeUrl)
                    }
                },
                update = { view ->
                    if (view.url != safeUrl) {
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
