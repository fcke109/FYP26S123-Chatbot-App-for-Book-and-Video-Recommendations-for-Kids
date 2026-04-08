package com.kidsrec.chatbot.ui.reader

import android.annotation.SuppressLint
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.activity.compose.BackHandler
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView

/**
 * BookReaderScreen: A dedicated visual reader for ICDL books.
 * Features strict domain security to allow visual storytelling content.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookReaderScreen(
    url: String,
    onBack: (durationSeconds: Long) -> Unit
) {
    val openedAtMs = remember { System.currentTimeMillis() }
    var webView by remember { mutableStateOf<WebView?>(null) }
    var canGoBack by remember { mutableStateOf(false) }

    BackHandler {
        if (canGoBack) {
            webView?.goBack()
        } else {
            onBack((System.currentTimeMillis() - openedAtMs) / 1000)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Visual Storybook") },
                navigationIcon = {
                    IconButton(onClick = { onBack((System.currentTimeMillis() - openedAtMs) / 1000) }) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {
            AndroidView(
                factory = { context ->
                    WebView(context).apply {
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
                                
                                // UNBLOCKED DOMAINS for Visual Book Readers
                                val allowed = listOf(
                                    "childrenslibrary.org",
                                    "archive.org",
                                    "ia80", "ia60", "ia90", "ia70",
                                    "openlibrary.org",
                                    "storyweaver.org.in",
                                    "covers.openlibrary.org"
                                )
                                
                                return if (allowed.any { target.contains(it) }) {
                                    false // allow navigation
                                } else {
                                    true  // block unsafe links
                                }
                            }

                            override fun onPageFinished(view: WebView?, url: String?) {
                                super.onPageFinished(view, url)
                                canGoBack = view?.canGoBack() ?: false
                            }
                        }
                        loadUrl(url)
                    }
                },
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}
