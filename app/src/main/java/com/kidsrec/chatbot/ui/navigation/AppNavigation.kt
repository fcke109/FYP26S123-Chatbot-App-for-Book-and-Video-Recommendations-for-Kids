package com.kidsrec.chatbot.ui.navigation

import android.util.Log
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import kotlinx.coroutines.launch
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.google.firebase.auth.FirebaseAuth
import com.kidsrec.chatbot.data.model.AccountType
import com.kidsrec.chatbot.data.model.PlanType
import com.kidsrec.chatbot.data.model.UserNotification
import com.kidsrec.chatbot.ui.admin.AdminScreen
import com.kidsrec.chatbot.ui.admin.AdminUpgradeScreen
import com.kidsrec.chatbot.ui.admin.AdminViewModel
import com.kidsrec.chatbot.ui.auth.AuthState
import com.kidsrec.chatbot.ui.auth.AuthViewModel
import com.kidsrec.chatbot.ui.auth.EmailVerificationScreen
import com.kidsrec.chatbot.ui.auth.LoginScreen
import com.kidsrec.chatbot.ui.chat.ChatViewModel
import com.kidsrec.chatbot.ui.chat.DinoChatPage
import com.kidsrec.chatbot.ui.favorites.FavoritesScreen
import com.kidsrec.chatbot.ui.favorites.FavoritesViewModel
import com.kidsrec.chatbot.ui.gamification.BadgesRewardsScreen
import com.kidsrec.chatbot.ui.library.LibraryViewModel
import com.kidsrec.chatbot.ui.library.SmartSearchViewModel
import com.kidsrec.chatbot.ui.library.UserLibraryScreen
import com.kidsrec.chatbot.ui.notification.NotificationsViewModel
import com.kidsrec.chatbot.ui.parent.ParentDashboardScreen
import com.kidsrec.chatbot.ui.parent.ParentDashboardViewModel
import com.kidsrec.chatbot.ui.parent.ParentInviteSetupRoute
import com.kidsrec.chatbot.ui.parent.ParentProgressViewModel
import com.kidsrec.chatbot.ui.parental.ParentalControlsScreen
import com.kidsrec.chatbot.ui.billing.PaymentScreen
import com.kidsrec.chatbot.ui.profile.ProfileScreen
import com.kidsrec.chatbot.ui.profile.ProfileViewModel
import com.kidsrec.chatbot.ui.reader.BookReaderScreen
import com.kidsrec.chatbot.ui.screentime.ScreenTimeWrapper
import com.kidsrec.chatbot.ui.webview.SafeWebViewScreen
import com.kidsrec.chatbot.ui.webview.YouTubePlayerScreen
import java.net.URLEncoder
import java.util.Date

// Fixed admin email used to identify the system administrator account
private const val ADMIN_EMAIL = "admin@littledino.com"

// Checks whether an email belongs to the administrator account
private fun isAdminEmail(email: String?): Boolean {
    return email.equals(ADMIN_EMAIL, ignoreCase = true)
}

// Defines all navigation destinations used in the app
sealed class Screen(val route: String, val title: String, val icon: ImageVector? = null) {
    object Login : Screen("login", "Login")
    object Chat : Screen("chat", "Chat", Icons.Default.Chat)
    object Library : Screen("library", "Library", Icons.AutoMirrored.Filled.MenuBook)
    object Favorites : Screen("favorites", "Favorites", Icons.Default.Favorite)
    object Profile : Screen("profile", "Profile", Icons.Default.Person)
    object BadgesRewards : Screen("badges_rewards", "Badges & Rewards")
    object Admin : Screen("admin", "Admin", Icons.Default.Shield)
    object AdminUpgrade : Screen("admin_upgrade", "Admin CMS Upgrade")
    object ParentDashboard : Screen("parent_dashboard", "Dashboard", Icons.Default.Person)
    object ParentInterestSelection : Screen("parent_interest_selection", "Set Child Interests")
    object ParentalControls : Screen("parental_controls", "Parental Controls")
    object Reader : Screen("reader?url={url}&title={title}", "Reader")
    object SafeWebView : Screen(
        "webview?url={url}&title={title}&isVideo={isVideo}&itemId={itemId}&imageUrl={imageUrl}&description={description}",
        "WebView"
    )
    object YouTubePlayer : Screen(
        "youtube_player?videoId={videoId}&title={title}",
        "YouTube Player"
    )
    object PremiumUpgrade : Screen("premium", "Upgrade")
}

// Normalizes content URLs before routing them to the correct viewer
private fun normalizeContentUrl(url: String): String {
    return url.trim().replace("http://", "https://")
}

// Checks whether a URL belongs to a supported online book/reading source
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

// Checks whether a URL looks like a supported YouTube or YouTube Kids link
private fun isYoutubeLikeUrl(url: String): Boolean {
    val lower = url.trim().lowercase()
    return lower.contains("youtube.com") ||
            lower.contains("youtu.be") ||
            lower.contains("m.youtube.com") ||
            lower.contains("youtube-nocookie.com") ||
            lower.contains("youtubekids.com")
}

// Extracts a YouTube video ID from common YouTube URL formats
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

// Builds the navigation route for the in-app YouTube player
private fun buildYouTubePlayerRoute(videoId: String, title: String): String {
    val encodedId = URLEncoder.encode(videoId, "UTF-8")
    val encodedTitle = URLEncoder.encode(title, "UTF-8")
    return "youtube_player?videoId=$encodedId&title=$encodedTitle"
}

// Builds the correct navigation route depending on whether the content is a book, YouTube video, or safe web content
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

    // YouTube links are routed to the dedicated in-app YouTube player when possible
    if (isYoutubeLikeUrl(cleanUrl)) {
        val youtubeId = extractYoutubeId(cleanUrl)
        if (youtubeId != null) {
            Log.d("KidsRecNav", "Routing to in-app YouTube player for ID: $youtubeId")
            return buildYouTubePlayerRoute(youtubeId, title)
        }
    }

    // Re-evaluates whether the content should be treated as video or reading material
    val finalIsVideo = when {
        cleanUrl.isBlank() -> isVideo
        isKnownBookUrl(cleanUrl) -> false
        isYoutubeLikeUrl(cleanUrl) -> true
        else -> isVideo
    }

    Log.d("KidsRecNav", "Final isVideo: $finalIsVideo")

    if (!finalIsVideo && isKnownBookUrl(cleanUrl)) {
        val encodedUrl = URLEncoder.encode(cleanUrl, "UTF-8")
        val encodedTitle = URLEncoder.encode(title, "UTF-8")
        Log.d("KidsRecNav", "Routing to BookReaderScreen")
        return "reader?url=$encodedUrl&title=$encodedTitle"
    }

    val encodedUrl = URLEncoder.encode(cleanUrl, "UTF-8")
    val encodedTitle = URLEncoder.encode(title, "UTF-8")
    val encodedImg = URLEncoder.encode(if (imageUrl.isBlank()) "none" else imageUrl, "UTF-8")
    val encodedDesc = URLEncoder.encode(if (description.isBlank()) "none" else description, "UTF-8")
    val encodedItemId = URLEncoder.encode(itemId, "UTF-8")

    return "webview?url=$encodedUrl&title=$encodedTitle&isVideo=$finalIsVideo&itemId=$encodedItemId&imageUrl=$encodedImg&description=$encodedDesc"
}

// Hilt entry point used to access AnalyticsRepository from navigation callbacks
@EntryPoint
@InstallIn(SingletonComponent::class)
interface AnalyticsEntryPoint {
    fun analyticsRepository(): com.kidsrec.chatbot.data.repository.AnalyticsRepository
}

// Root navigation controller that decides whether to show auth, child, parent, or admin screens
@Composable
fun AppNavigation() {
    // Shared authentication ViewModel for the app
    val authViewModel: AuthViewModel = hiltViewModel()
    val authState by authViewModel.authState.collectAsState()
    // Current user and notification state observed by the app shell
    val currentUser by authViewModel.currentUser.collectAsState()

    // Firebase Auth email is used as an additional admin check
    val firebaseEmail = FirebaseAuth.getInstance().currentUser?.email

    // Determines whether the current account should enter the admin dashboard
    val isAdmin by remember(currentUser, firebaseEmail) {
        derivedStateOf {
            currentUser?.planType == PlanType.ADMIN ||
                    isAdminEmail(firebaseEmail) ||
                    isAdminEmail(currentUser?.email)
        }
    }

    // Determines whether the current account should enter the parent dashboard
    val isParent by remember(currentUser, firebaseEmail) {
        derivedStateOf {
            currentUser?.accountType == AccountType.PARENT &&
                    currentUser?.planType != PlanType.ADMIN &&
                    !isAdminEmail(firebaseEmail) &&
                    !isAdminEmail(currentUser?.email)
        }
    }

    // Chooses the first screen based on authentication and verification state
    when (authState) {
        is AuthState.Authenticated -> MainScreen(
            authViewModel = authViewModel,
            isAdmin = isAdmin,
            isParent = isParent,
            isFree = currentUser?.planType == PlanType.FREE
        )

        is AuthState.EmailNotVerified -> EmailVerificationScreen(authViewModel)

        is AuthState.Initial -> Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator()
        }

        else -> AuthNavigation(authViewModel)
    }
}

// Navigation graph used before the user is authenticated
@Composable
fun AuthNavigation(authViewModel: AuthViewModel) {
    // Main NavController for authenticated navigation
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = Screen.Login.route
    ) {
        composable(Screen.Login.route) {
            LoginScreen(
                onLoginSuccess = {},
                viewModel = authViewModel
            )
        }
    }
}

// Main authenticated app shell containing the bottom navigation and all protected routes
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    authViewModel: AuthViewModel,
    isAdmin: Boolean,
    isParent: Boolean = false,
    isFree: Boolean = false
) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination
    // Shared ViewModels used across multiple child screens
    val profileViewModel: ProfileViewModel = hiltViewModel()
    val favoritesViewModel: FavoritesViewModel = hiltViewModel()
    val notificationsViewModel: NotificationsViewModel = hiltViewModel()

    val currentUser by authViewModel.currentUser.collectAsState()
    val notifications by notificationsViewModel.uiState.collectAsState()

    // Filters unread notifications that should be shown as announcements
    val unreadAnnouncements = notifications.filter {
        !it.read && (
                it.type.equals("announcement", ignoreCase = true) ||
                        it.type.equals("personalized", ignoreCase = true)
                )
    }

    // Starts realtime notification listening when the current user changes
    LaunchedEffect(currentUser?.id) {
        currentUser?.id?.let { userId ->
            notificationsViewModel.startListening(userId)
        }
    }

    // Displays unread announcement or personalized notifications in a dialog
    if (unreadAnnouncements.isNotEmpty()) {
        AnnouncementDialog(
            announcements = unreadAnnouncements,
            onDismiss = {
                currentUser?.id?.let { userId ->
                    unreadAnnouncements.forEach { notification ->
                        notificationsViewModel.markRead(userId, notification.id)
                    }
                }
            }
        )
    }

    // Loads user favorites once when the main screen is created
    LaunchedEffect(Unit) {
        favoritesViewModel.loadFavorites()
    }

    // Child users receive bottom navigation; admin and parent accounts use dashboard screens
    val bottomNavItems = if (isAdmin || isParent) {
        emptyList()
    } else {
        listOf(Screen.Chat, Screen.Library, Screen.Favorites, Screen.Profile)
    }

    // Selects the start destination based on the authenticated account role
    val startRoute = when {
        isAdmin -> Screen.Admin.route
        isParent -> Screen.ParentDashboard.route
        else -> Screen.Chat.route
    }

    // Redirects role-based users to the correct dashboard if their role changes
    LaunchedEffect(isAdmin, isParent) {
        val targetRoute = when {
            isAdmin -> Screen.Admin.route
            isParent -> Screen.ParentDashboard.route
            else -> null
        }

        if (targetRoute != null && currentDestination?.route != targetRoute) {
            navController.navigate(targetRoute) {
                popUpTo(navController.graph.findStartDestination().id) {
                    inclusive = false
                }
                launchSingleTop = true
            }
        }
    }

    Scaffold(
        // Shows bottom navigation only on child main tabs, not on full-screen readers or dashboards
        bottomBar = {
            if (
                bottomNavItems.isNotEmpty() &&
                currentDestination?.route?.startsWith("webview") == false &&
                currentDestination?.route?.startsWith("reader") == false &&
                currentDestination?.route?.startsWith("youtube_player") == false &&
                currentDestination?.route != Screen.BadgesRewards.route &&
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

        // Screen time enforcement is only applied to child accounts
        val isChildAccount = !isAdmin && !isParent

        // Main authenticated navigation graph
        val navContent: @Composable () -> Unit = {
            NavHost(
                navController = navController,
                startDestination = startRoute,
                modifier = Modifier.padding(innerPadding)
            ) {

                // Chatbot route for Little Dino recommendations and conversations
                composable(Screen.Chat.route) {
                    val chatViewModel: ChatViewModel = hiltViewModel()
                    val searchViewModel: SmartSearchViewModel = hiltViewModel()
                    val libraryViewModel: LibraryViewModel = hiltViewModel()

                    DinoChatPage(
                        viewModel = chatViewModel,
                        favoritesViewModel = favoritesViewModel,
                        searchViewModel = searchViewModel,
                        onOpenRecommendation = { url, title, isVideo, itemId, imageUrl, description ->

                            // Chooses the best available identifier for analytics tracking
                            val analyticsId = when {
                                itemId.isNotBlank() -> itemId
                                url.isNotBlank() -> url
                                else -> title
                            }

                            Log.d("ANALYTICS_TEST", "Chat open -> title=$title analyticsId=$analyticsId")

                            // Tracks the opened recommendation for admin analytics
                            libraryViewModel.trackBookView(
                                bookTitle = title,
                                bookId = analyticsId
                            )

                            // Adds the opened content to the user's reading/watch history
                            profileViewModel.trackReading(
                                title = title,
                                url = url,
                                coverUrl = imageUrl,
                                isVideo = isVideo
                            )

                            navController.navigate(
                                buildContentRoute(
                                    url = url,
                                    title = title,
                                    isVideo = isVideo,
                                    itemId = analyticsId,
                                    imageUrl = imageUrl,
                                    description = description
                                )
                            )
                        }
                    )
                }

                // Library route for browsing curated and recommended books/videos
                composable(Screen.Library.route) {
                    val libraryViewModel: LibraryViewModel = hiltViewModel()
                    val searchViewModel: SmartSearchViewModel = hiltViewModel()

                    UserLibraryScreen(
                        viewModel = libraryViewModel,
                        favoritesViewModel = favoritesViewModel,
                        searchViewModel = searchViewModel,
                        currentUser = currentUser,
                        onOpenRecommendation = { url, title, isVideo, itemId, imageUrl, description ->

                            val analyticsId = when {
                                itemId.isNotBlank() -> itemId
                                url.isNotBlank() -> url
                                else -> title
                            }

                            Log.d("ANALYTICS_TEST", "Library open -> title=$title analyticsId=$analyticsId")

                            libraryViewModel.trackBookView(
                                bookTitle = title,
                                bookId = analyticsId
                            )

                            profileViewModel.trackReading(
                                title = title,
                                url = url,
                                coverUrl = imageUrl,
                                isVideo = isVideo
                            )

                            navController.navigate(
                                buildContentRoute(
                                    url = url,
                                    title = title,
                                    isVideo = isVideo,
                                    itemId = analyticsId,
                                    imageUrl = imageUrl,
                                    description = description
                                )
                            )
                        }
                    )
                }

                // Favorites route for reopening saved books and videos
                composable(Screen.Favorites.route) {
                    val libraryViewModel: LibraryViewModel = hiltViewModel()

                    FavoritesScreen(
                        viewModel = favoritesViewModel,
                        onOpenFavorite = { url, title, isVideo, itemId, imageUrl, description ->

                            val analyticsId = when {
                                itemId.isNotBlank() -> itemId
                                url.isNotBlank() -> url
                                else -> title
                            }

                            Log.d("ANALYTICS_TEST", "Favorites open -> title=$title analyticsId=$analyticsId")

                            libraryViewModel.trackBookView(
                                bookTitle = title,
                                bookId = analyticsId
                            )

                            profileViewModel.trackReading(
                                title = title,
                                url = url,
                                coverUrl = imageUrl,
                                isVideo = isVideo
                            )

                            navController.navigate(
                                buildContentRoute(
                                    url = url,
                                    title = title,
                                    isVideo = isVideo,
                                    itemId = analyticsId,
                                    imageUrl = imageUrl,
                                    description = description
                                )
                            )
                        }
                    )
                }

                // Profile route for viewing and editing child profile information
                composable(Screen.Profile.route) {
                    ProfileScreen(
                        authViewModel = authViewModel,
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
                        },
                        onNavigateToParentalControls = {
                            navController.navigate(Screen.ParentalControls.route)
                        },
                        onNavigateToBadgesRewards = {
                            navController.navigate(Screen.BadgesRewards.route)
                        },
                        onNavigateToUpgrade = {
                            navController.navigate(Screen.PremiumUpgrade.route)
                        }
                    )
                }

                // Premium-only badges and rewards route
                composable(Screen.BadgesRewards.route) {
                    val childUser = currentUser

                    if (childUser != null && childUser.planType == PlanType.PREMIUM) {
                        BadgesRewardsScreen(
                            childUserId = childUser.id,
                            childName = childUser.name,
                            onBack = { navController.popBackStack() }
                        )
                    } else {
                        LaunchedEffect(Unit) {
                            navController.popBackStack()
                        }
                    }
                }

                // Admin dashboard route
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

                // Admin CMS upgrade placeholder route
                composable(Screen.AdminUpgrade.route) {
                    AdminUpgradeScreen(
                        onBack = { navController.popBackStack() }
                    )
                }

                // Parent dashboard route for managing linked children
                composable(Screen.ParentDashboard.route) {
                    val parentDashboardViewModel: ParentDashboardViewModel = hiltViewModel()
                    val parentProgressViewModel: ParentProgressViewModel = hiltViewModel()

                    ParentDashboardScreen(
                        viewModel = parentDashboardViewModel,
                        parentProgressViewModel = parentProgressViewModel,
                        onLogout = { authViewModel.signOut() },
                        onUpgradePremium = { navController.navigate(Screen.PremiumUpgrade.route) },
                        onGenerateCode = { navController.navigate(Screen.ParentInterestSelection.route) }
                    )
                }

                // Parent invite setup route for selecting child interests before generating an invite code
                composable(Screen.ParentInterestSelection.route) {
                    val parentUser = currentUser

                    if (parentUser == null || parentUser.accountType != AccountType.PARENT) {
                        LaunchedEffect(Unit) {
                            navController.popBackStack()
                        }
                    } else {
                        ParentInviteSetupRoute(
                            parentId = parentUser.id,
                            parentName = parentUser.name,
                            onBack = { navController.popBackStack() }
                        )
                    }
                }

                // Child-side parental controls route guarded by the parent PIN
                composable(Screen.ParentalControls.route) {
                    ParentalControlsScreen(
                        onNavigateBack = { navController.popBackStack() },
                        authViewModel = authViewModel
                    )
                }

                // Safe WebView route for supported non-book web content and fallback viewing
                composable(
                    route = Screen.SafeWebView.route,
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
                    val url = bse.arguments?.getString("url") ?: ""

                    if (url.isBlank()) {
                        LaunchedEffect(Unit) {
                            navController.popBackStack()
                        }
                    } else {
                        val analyticsRepository = EntryPointAccessors.fromApplication(
                            LocalContext.current.applicationContext,
                            AnalyticsEntryPoint::class.java
                        ).analyticsRepository()

                        val scope = rememberCoroutineScope()
                        val itemId = bse.arguments?.getString("itemId") ?: ""
                        val title = bse.arguments?.getString("title") ?: ""

                        // Opens the URL in the safe web viewer and records drop-off analytics when closed
                        SafeWebViewScreen(
                            url = url,
                            title = title,
                            isVideo = bse.arguments?.getBoolean("isVideo") ?: false,
                            onClose = { durationSeconds ->
                                val closeTimestamp = com.google.firebase.Timestamp.now()
                                val openedAt = com.google.firebase.Timestamp(
                                    Date(closeTimestamp.toDate().time - durationSeconds * 1000)
                                )
                                val userId = currentUser?.id ?: "unknown"

                                scope.launch {
                                    analyticsRepository.trackDropOffPoint(
                                        itemId = if (url.isNotBlank()) url else title,
                                        itemTitle = if (title.isNotBlank()) title else url,
                                        userId = userId,
                                        openedAt = openedAt,
                                        closedAt = closeTimestamp,
                                        durationSeconds = durationSeconds
                                    )
                                }

                                navController.popBackStack()
                            }
                        )
                    }
                }

                // In-app YouTube player route for validated YouTube video IDs
                composable(
                    route = Screen.YouTubePlayer.route,
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
                    val videoId = bse.arguments?.getString("videoId") ?: ""

                    if (videoId.isBlank()) {
                        LaunchedEffect(Unit) {
                            navController.popBackStack()
                        }
                    } else {
                        val analyticsRepository = EntryPointAccessors.fromApplication(
                            LocalContext.current.applicationContext,
                            AnalyticsEntryPoint::class.java
                        ).analyticsRepository()

                        val scope = rememberCoroutineScope()
                        val title = bse.arguments?.getString("title") ?: ""

                        // Plays the YouTube video inside the app and records drop-off analytics when closed
                        YouTubePlayerScreen(
                            videoId = videoId,
                            title = title,
                            onBack = { durationSeconds ->
                                val closeTimestamp = com.google.firebase.Timestamp.now()
                                val openedAt = com.google.firebase.Timestamp(
                                    Date(closeTimestamp.toDate().time - durationSeconds * 1000)
                                )
                                val userId = currentUser?.id ?: "unknown"

                                scope.launch {
                                    analyticsRepository.trackDropOffPoint(
                                        itemId = videoId,
                                        itemTitle = title.ifBlank { videoId },
                                        userId = userId,
                                        openedAt = openedAt,
                                        closedAt = closeTimestamp,
                                        durationSeconds = durationSeconds
                                    )
                                }

                                navController.popBackStack()
                            }
                        )
                    }
                }

                // Book reader route for supported online reading sources
                composable(
                    route = Screen.Reader.route,
                    arguments = listOf(
                        navArgument("url") {
                            type = NavType.StringType
                            defaultValue = ""
                        },
                        navArgument("title") {
                            type = NavType.StringType
                            defaultValue = ""
                        }
                    )
                ) { bse ->
                    val url = bse.arguments?.getString("url") ?: ""
                    val title = bse.arguments?.getString("title") ?: ""

                    if (url.isBlank()) {
                        LaunchedEffect(Unit) {
                            navController.popBackStack()
                        }
                    } else {
                        val analyticsRepository = EntryPointAccessors.fromApplication(
                            LocalContext.current.applicationContext,
                            AnalyticsEntryPoint::class.java
                        ).analyticsRepository()

                        val scope = rememberCoroutineScope()

                        // Opens the book reader and records reading duration when closed
                        BookReaderScreen(
                            url = url,
                            onBack = { durationSeconds ->
                                val closeTimestamp = com.google.firebase.Timestamp.now()
                                val openedAt = com.google.firebase.Timestamp(
                                    Date(closeTimestamp.toDate().time - durationSeconds * 1000)
                                )
                                val userId = currentUser?.id ?: "unknown"

                                scope.launch {
                                    analyticsRepository.trackDropOffPoint(
                                        itemId = if (title.isNotBlank()) title else url,
                                        itemTitle = if (title.isNotBlank()) title else url,
                                        userId = userId,
                                        openedAt = openedAt,
                                        closedAt = closeTimestamp,
                                        durationSeconds = durationSeconds
                                    )
                                }

                                navController.popBackStack()
                            }
                        )
                    }
                }

                // Premium upgrade and payment route
                composable(Screen.PremiumUpgrade.route) {
                    PaymentScreen(
                        onBack = { navController.popBackStack() },
                        onPaymentSuccess = {
                            authViewModel.upgradeCurrentUserToPremium()
                            navController.popBackStack()
                        }
                    )
                }
            }
        }

        // Wraps child accounts with screen time tracking and blocking logic
        if (isChildAccount) {
            ScreenTimeWrapper { navContent() }
        } else {
            navContent()
        }
    }
}

// Dialog used to show unread announcements and personalized notifications
@Composable
fun AnnouncementDialog(
    announcements: List<UserNotification>,
    onDismiss: () -> Unit
) {
    AlertDialog(
        // Marks notifications as handled when the dialog is dismissed
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                Icons.Default.Notifications,
                contentDescription = "Announcements",
                tint = MaterialTheme.colorScheme.primary
            )
        },
        title = {
            Text(
                "New Notifications",
                style = MaterialTheme.typography.headlineSmall
            )
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Renders each unread announcement inside its own card
                announcements.forEach { announcement ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = announcement.title,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = announcement.body,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }
            }
        },
        // Confirmation button closes the dialog and marks messages as read
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Got it!")
            }
        }
    )
}