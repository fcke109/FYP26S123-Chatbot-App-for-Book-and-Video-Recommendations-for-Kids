package com.kidsrec.chatbot.ui.navigation

import android.util.Log
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.kidsrec.chatbot.data.model.PlanType
import com.kidsrec.chatbot.ui.admin.AdminScreen
import com.kidsrec.chatbot.ui.admin.AdminViewModel
import com.kidsrec.chatbot.ui.auth.AuthState
import com.kidsrec.chatbot.ui.auth.AuthViewModel
import com.kidsrec.chatbot.ui.auth.LoginScreen
import com.kidsrec.chatbot.ui.auth.RegisterScreen
import com.kidsrec.chatbot.ui.chat.ChatViewModel
import com.kidsrec.chatbot.ui.chat.DinoChatPage
import com.kidsrec.chatbot.ui.favorites.FavoritesScreen
import com.kidsrec.chatbot.ui.favorites.FavoritesViewModel
import com.kidsrec.chatbot.ui.library.LibraryViewModel
import com.kidsrec.chatbot.ui.library.UserLibraryScreen
import com.kidsrec.chatbot.ui.profile.ProfileScreen
import com.kidsrec.chatbot.ui.profile.ProfileViewModel
import com.kidsrec.chatbot.ui.reader.BookReaderScreen
import com.kidsrec.chatbot.ui.webview.SafeWebViewScreen
import com.kidsrec.chatbot.ui.webview.YouTubePlayerScreen
import java.net.URLEncoder

sealed class Screen(val route: String, val title: String, val icon: ImageVector? = null) {
    object Login : Screen("login", "Login")
    object Register : Screen("register", "Register")
    object Chat : Screen("chat", "Chat", Icons.Default.Chat)
    object Library : Screen("library", "Library", Icons.AutoMirrored.Filled.MenuBook)
    object Favorites : Screen("favorites", "Favorites", Icons.Default.Favorite)
    object Profile : Screen("profile", "Profile", Icons.Default.Person)
    object Admin : Screen("admin", "Admin", Icons.Default.Shield)
    object Reader : Screen("reader/{url}", "Reader")
    object SafeWebView : Screen(
        "webview?url={url}&title={title}&isVideo={isVideo}&itemId={itemId}&imageUrl={imageUrl}&description={description}",
        "WebView"
    )
    object YouTubePlayer : Screen(
        "youtube_player?videoId={videoId}&title={title}",
        "YouTube Player"
    )
}

// -----------------------------
// HELPER FUNCTIONS
// -----------------------------

private fun normalizeContentUrl(url: String): String {
    return url.trim().replace("http://", "https://")
}

private fun isKnownBookUrl(url: String): Boolean {
    val lower = url.trim().lowercase()
    return lower.contains("archive.org") ||
            lower.contains("openlibrary.org") ||
            lower.contains("storyweaver.org") ||
            lower.contains("storyweaver.org.in") ||
            lower.contains("childrenslibrary.org") ||
            lower.contains("icdlbooks.org") ||
            lower.contains("books.google.")
}

private fun isYoutubeLikeUrl(url: String): Boolean {
    val lower = url.trim().lowercase()
    return lower.contains("youtube.com") ||
            lower.contains("youtu.be") ||
            lower.contains("m.youtube.com") ||
            lower.contains("youtube-nocookie.com") ||
            lower.contains("youtubekids.com")
}

private fun extractYoutubeId(url: String): String? {
    if (url.isBlank()) return null

    val patterns = listOf(
        Regex("""[?&]v=([a-zA-Z0-9_-]{11})"""),
        Regex("""youtu\.be/([a-zA-Z0-9_-]{11})"""),
        Regex("""embed/([a-zA-Z0-9_-]{11})"""),
        Regex("""shorts/([a-zA-Z0-9_-]{11})"""),
        Regex("""live/([a-zA-Z0-9_-]{11})"""),
        Regex("""youtubekids\.com/watch\?v=([a-zA-Z0-9_-]{11})"""),
        Regex("""youtubekids\.com/embed/([a-zA-Z0-9_-]{11})""")
    )

    for (pattern in patterns) {
        val match = pattern.find(url)
        if (match != null) {
            return match.groupValues[1]
        }
    }

    return null
}

private fun buildYouTubePlayerRoute(videoId: String, title: String): String {
    val encodedId = URLEncoder.encode(videoId, "UTF-8")
    val encodedTitle = URLEncoder.encode(title, "UTF-8")
    return "youtube_player?videoId=$encodedId&title=$encodedTitle"
}

private fun buildContentRoute(
    url: String,
    title: String,
    isVideo: Boolean,
    itemId: String = "",
    imageUrl: String = "",
    description: String = ""
): String {
    val cleanUrl = normalizeContentUrl(url)

    Log.d("KidsRecNav", "Original URL: $url")
    Log.d("KidsRecNav", "Clean URL: $cleanUrl")
    Log.d("KidsRecNav", "Incoming isVideo: $isVideo")

    // Route YouTube videos to in-app player (only if URL is actually YouTube)
    if (isYoutubeLikeUrl(cleanUrl)) {
        val youtubeId = extractYoutubeId(cleanUrl)
        if (youtubeId != null) {
            Log.d("KidsRecNav", "Routing to in-app YouTube player for ID: $youtubeId")
            return buildYouTubePlayerRoute(youtubeId, title)
        }
    }

    // FORCE correct mode from URL when possible
    val finalIsVideo = when {
        cleanUrl.isBlank() -> isVideo
        isKnownBookUrl(cleanUrl) -> false
        isYoutubeLikeUrl(cleanUrl) -> true
        else -> isVideo
    }

    Log.d("KidsRecNav", "Final isVideo: $finalIsVideo")

    val encodedUrl = URLEncoder.encode(cleanUrl, "UTF-8")
    val encodedTitle = URLEncoder.encode(title, "UTF-8")
    val encodedImg = URLEncoder.encode(if (imageUrl.isBlank()) "none" else imageUrl, "UTF-8")
    val encodedDesc = URLEncoder.encode(if (description.isBlank()) "none" else description, "UTF-8")
    val encodedItemId = URLEncoder.encode(itemId, "UTF-8")

    return "webview?url=$encodedUrl&title=$encodedTitle&isVideo=$finalIsVideo&itemId=$encodedItemId&imageUrl=$encodedImg&description=$encodedDesc"
}

@Composable
fun AppNavigation() {
    val authViewModel: AuthViewModel = hiltViewModel()
    val authState by authViewModel.authState.collectAsState()
    val currentUser by authViewModel.currentUser.collectAsState()

    val isAdmin by remember(currentUser, authState) {
        derivedStateOf {
            val firebaseEmail =
                com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.email?.lowercase()

            firebaseEmail == "admin@littledino.com" ||
                    currentUser?.email?.lowercase() == "admin@littledino.com" ||
                    currentUser?.planType == PlanType.ADMIN
        }
    }

    when (authState) {
        is AuthState.Authenticated -> MainScreen(authViewModel, isAdmin)
        is AuthState.Initial -> Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator()
        }

        else -> AuthNavigation(authViewModel)
    }
}

@Composable
fun AuthNavigation(authViewModel: AuthViewModel) {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = Screen.Login.route
    ) {
        composable(Screen.Login.route) {
            LoginScreen(
                onLoginSuccess = {},
                onAdminLogin = {},
                onNavigateToRegister = { navController.navigate(Screen.Register.route) },
                viewModel = authViewModel
            )
        }

        composable(Screen.Register.route) {
            RegisterScreen(
                onRegisterSuccess = {},
                onNavigateToLogin = { navController.navigate(Screen.Login.route) },
                viewModel = authViewModel
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(authViewModel: AuthViewModel, isAdmin: Boolean) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination
    val profileViewModel: ProfileViewModel = hiltViewModel()

    val bottomNavItems = if (isAdmin) {
        emptyList()
    } else {
        listOf(Screen.Chat, Screen.Library, Screen.Favorites, Screen.Profile)
    }

    val startRoute = if (isAdmin) Screen.Admin.route else Screen.Chat.route

    Scaffold(
        bottomBar = {
            if (
                bottomNavItems.isNotEmpty() &&
                currentDestination?.route?.startsWith("webview") == false &&
                currentDestination?.route in bottomNavItems.map { it.route }
            ) {
                NavigationBar {
                    bottomNavItems.forEach { screen ->
                        NavigationBarItem(
                            icon = {
                                screen.icon?.let {
                                    Icon(it, contentDescription = screen.title)
                                }
                            },
                            label = { Text(screen.title) },
                            selected = currentDestination?.hierarchy?.any { it.route == screen.route } == true,
                            onClick = {
                                navController.navigate(screen.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        )
                    }
                }
            }
        }
    ) { innerPadding ->

        NavHost(
            navController = navController,
            startDestination = startRoute,
            modifier = Modifier.padding(innerPadding)
        ) {

            composable(Screen.Chat.route) {
                val chatViewModel: ChatViewModel = hiltViewModel()
                val favoritesViewModel: FavoritesViewModel = hiltViewModel()

                DinoChatPage(
                    viewModel = chatViewModel,
                    favoritesViewModel = favoritesViewModel,
                    onOpenRecommendation = { url, title, isVideo, itemId, imageUrl, description ->
                        profileViewModel.trackReading(title, url, coverUrl = imageUrl, isVideo = isVideo)
                        navController.navigate(
                            buildContentRoute(
                                url = url,
                                title = title,
                                isVideo = isVideo,
                                itemId = itemId,
                                imageUrl = imageUrl,
                                description = description
                            )
                        )
                    }
                )
            }

            composable(Screen.Library.route) {
                val libraryViewModel: LibraryViewModel = hiltViewModel()
                val favoritesViewModel: FavoritesViewModel = hiltViewModel()

                UserLibraryScreen(
                    viewModel = libraryViewModel,
                    favoritesViewModel = favoritesViewModel,
                    onOpenRecommendation = { url, title, isVideo, itemId, imageUrl, description ->
                        profileViewModel.trackReading(title, url, coverUrl = imageUrl, isVideo = isVideo)
                        navController.navigate(
                            buildContentRoute(
                                url = url,
                                title = title,
                                isVideo = isVideo,
                                itemId = itemId,
                                imageUrl = imageUrl,
                                description = description
                            )
                        )
                    }
                )
            }

            composable(Screen.Favorites.route) {
                val favoritesViewModel: FavoritesViewModel = hiltViewModel()

                FavoritesScreen(
                    viewModel = favoritesViewModel,
                    onOpenFavorite = { url, title, isVideo, itemId, imageUrl, description ->
                        profileViewModel.trackReading(title, url, coverUrl = imageUrl, isVideo = isVideo)
                        navController.navigate(
                            buildContentRoute(
                                url = url,
                                title = title,
                                isVideo = isVideo,
                                itemId = itemId,
                                imageUrl = imageUrl,
                                description = description
                            )
                        )
                    }
                )
            }

            composable(Screen.Profile.route) {
                ProfileScreen(
                    authViewModel = hiltViewModel(),
                    profileViewModel = profileViewModel,
                    onItemClick = { url, title, isVideo ->
                        navController.navigate(
                            buildContentRoute(
                                url = url,
                                title = title,
                                isVideo = isVideo,
                                itemId = "history",
                                imageUrl = "none",
                                description = "none"
                            )
                        )
                    }
                )
            }

            composable(Screen.Admin.route) {
                val adminViewModel: AdminViewModel = hiltViewModel()

                AdminScreen(
                    viewModel = adminViewModel,
                    onLogout = { authViewModel.signOut() },
                    onViewBook = { title, url, isVideo ->
                        navController.navigate(
                            buildContentRoute(
                                url = url,
                                title = title,
                                isVideo = isVideo,
                                itemId = "admin",
                                imageUrl = "none",
                                description = "none"
                            )
                        )
                    }
                )
            }

            composable(
                Screen.SafeWebView.route,
                arguments = listOf(
                    navArgument("url") {
                        type = NavType.StringType
                        defaultValue = ""
                    },
                    navArgument("title") {
                        type = NavType.StringType
                        defaultValue = ""
                    },
                    navArgument("isVideo") {
                        type = NavType.BoolType
                        defaultValue = false
                    },
                    navArgument("itemId") {
                        type = NavType.StringType
                        defaultValue = ""
                    },
                    navArgument("imageUrl") {
                        type = NavType.StringType
                        defaultValue = ""
                    },
                    navArgument("description") {
                        type = NavType.StringType
                        defaultValue = ""
                    }
                )
            ) { bse ->
                SafeWebViewScreen(
                    url = bse.arguments?.getString("url") ?: "",
                    title = bse.arguments?.getString("title") ?: "",
                    isVideo = bse.arguments?.getBoolean("isVideo") ?: false,
                    onClose = { navController.popBackStack() }
                )
            }

            composable(
                Screen.YouTubePlayer.route,
                arguments = listOf(
                    navArgument("videoId") {
                        type = NavType.StringType
                        defaultValue = ""
                    },
                    navArgument("title") {
                        type = NavType.StringType
                        defaultValue = ""
                    }
                )
            ) { bse ->
                YouTubePlayerScreen(
                    videoId = bse.arguments?.getString("videoId") ?: "",
                    title = bse.arguments?.getString("title") ?: "",
                    onBack = { navController.popBackStack() }
                )
            }

            composable(
                Screen.Reader.route,
                arguments = listOf(
                    navArgument("url") { type = NavType.StringType }
                )
            ) { bse ->
                BookReaderScreen(
                    url = bse.arguments?.getString("url") ?: "",
                    onBack = { navController.popBackStack() }
                )
            }
        }
    }
}
