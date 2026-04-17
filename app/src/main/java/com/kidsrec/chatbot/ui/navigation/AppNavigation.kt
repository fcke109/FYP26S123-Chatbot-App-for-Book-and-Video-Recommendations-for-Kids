package com.kidsrec.chatbot.ui.navigation

import android.util.Log
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
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
import com.kidsrec.chatbot.ui.auth.RegisterScreen
import com.kidsrec.chatbot.ui.billing.PremiumUpgradeScreen
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
import com.kidsrec.chatbot.ui.profile.ProfileScreen
import com.kidsrec.chatbot.ui.profile.ProfileViewModel
import com.kidsrec.chatbot.ui.reader.BookReaderScreen
import com.kidsrec.chatbot.ui.screentime.ScreenTimeWrapper
import com.kidsrec.chatbot.ui.webview.SafeWebViewScreen
import com.kidsrec.chatbot.ui.webview.YouTubePlayerScreen
import java.net.URLEncoder
import java.util.Date

private const val ADMIN_EMAIL = "admin@littledino.com"

private fun isAdminEmail(email: String?): Boolean {
    return email.equals(ADMIN_EMAIL, ignoreCase = true)
}

sealed class Screen(val route: String, val title: String, val icon: ImageVector? = null) {
    object Login : Screen("login", "Login")
    object Register : Screen("register", "Register")
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
    object Reader : Screen("reader/{url}", "Reader")
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

    if (isYoutubeLikeUrl(cleanUrl)) {
        val youtubeId = extractYoutubeId(cleanUrl)
        if (youtubeId != null) {
            Log.d("KidsRecNav", "Routing to in-app YouTube player for ID: $youtubeId")
            return buildYouTubePlayerRoute(youtubeId, title)
        }
    }

    val finalIsVideo = when {
        cleanUrl.isBlank() -> isVideo
        isKnownBookUrl(cleanUrl) -> false
        isYoutubeLikeUrl(cleanUrl) -> true
        else -> isVideo
    }

    Log.d("KidsRecNav", "Final isVideo: $finalIsVideo")

    if (!finalIsVideo && isKnownBookUrl(cleanUrl)) {
        val encodedUrl = URLEncoder.encode(cleanUrl, "UTF-8")
        Log.d("KidsRecNav", "Routing to BookReaderScreen")
        return "reader/$encodedUrl"
    }

    val encodedUrl = URLEncoder.encode(cleanUrl, "UTF-8")
    val encodedTitle = URLEncoder.encode(title, "UTF-8")
    val encodedImg = URLEncoder.encode(if (imageUrl.isBlank()) "none" else imageUrl, "UTF-8")
    val encodedDesc = URLEncoder.encode(if (description.isBlank()) "none" else description, "UTF-8")
    val encodedItemId = URLEncoder.encode(itemId, "UTF-8")

    return "webview?url=$encodedUrl&title=$encodedTitle&isVideo=$finalIsVideo&itemId=$encodedItemId&imageUrl=$encodedImg&description=$encodedDesc"
}

@EntryPoint
@InstallIn(SingletonComponent::class)
interface AnalyticsEntryPoint {
    fun analyticsRepository(): com.kidsrec.chatbot.data.repository.AnalyticsRepository
}

@Composable
fun AppNavigation() {
    val authViewModel: AuthViewModel = hiltViewModel()
    val authState by authViewModel.authState.collectAsState()
    val currentUser by authViewModel.currentUser.collectAsState()

    val firebaseEmail = FirebaseAuth.getInstance().currentUser?.email

    val isAdmin by remember(currentUser, firebaseEmail) {
        derivedStateOf {
            currentUser?.planType == PlanType.ADMIN ||
                    isAdminEmail(firebaseEmail) ||
                    isAdminEmail(currentUser?.email)
        }
    }

    val isParent by remember(currentUser, firebaseEmail) {
        derivedStateOf {
            currentUser?.accountType == AccountType.PARENT &&
                    currentUser?.planType != PlanType.ADMIN &&
                    !isAdminEmail(firebaseEmail) &&
                    !isAdminEmail(currentUser?.email)
        }
    }

    when (authState) {
        is AuthState.Authenticated -> MainScreen(
            authViewModel = authViewModel,
            isAdmin = isAdmin,
            isParent = isParent,
            isGuest = currentUser?.isGuest == true
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
fun MainScreen(
    authViewModel: AuthViewModel,
    isAdmin: Boolean,
    isParent: Boolean = false,
    isGuest: Boolean = false
) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination
    val profileViewModel: ProfileViewModel = hiltViewModel()
    val favoritesViewModel: FavoritesViewModel = hiltViewModel()
    val notificationsViewModel: NotificationsViewModel = hiltViewModel()

    val currentUser by authViewModel.currentUser.collectAsState()
    val notifications by notificationsViewModel.uiState.collectAsState()

    val unreadAnnouncements = notifications.filter {
        !it.read && (
                it.type.equals("announcement", ignoreCase = true) ||
                        it.type.equals("personalized", ignoreCase = true)
                )
    }

    LaunchedEffect(currentUser?.id) {
        currentUser?.id?.let { userId ->
            notificationsViewModel.startListening(userId)
        }
    }

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

    LaunchedEffect(Unit) {
        favoritesViewModel.loadFavorites()
    }

    val bottomNavItems = if (isAdmin || isParent) {
        emptyList()
    } else if (isGuest) {
        listOf(Screen.Chat, Screen.Library, Screen.Profile)
    } else {
        listOf(Screen.Chat, Screen.Library, Screen.Favorites, Screen.Profile)
    }

    val startRoute = when {
        isAdmin -> Screen.Admin.route
        isParent -> Screen.ParentDashboard.route
        else -> Screen.Chat.route
    }

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

        val isChildAccount = !isAdmin && !isParent

        val navContent: @Composable () -> Unit = {
            NavHost(
                navController = navController,
                startDestination = startRoute,
                modifier = Modifier.padding(innerPadding)
            ) {

                composable(Screen.Chat.route) {
                    val chatViewModel: ChatViewModel = hiltViewModel()
                    val searchViewModel: SmartSearchViewModel = hiltViewModel()

                    DinoChatPage(
                        viewModel = chatViewModel,
                        favoritesViewModel = favoritesViewModel,
                        searchViewModel = searchViewModel,
                        onOpenRecommendation = { url, title, isVideo, itemId, imageUrl, description ->
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
                    val searchViewModel: SmartSearchViewModel = hiltViewModel()

                    UserLibraryScreen(
                        viewModel = libraryViewModel,
                        favoritesViewModel = favoritesViewModel,
                        searchViewModel = searchViewModel,
                        onOpenRecommendation = { url, title, isVideo, itemId, imageUrl, description ->
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
                                    itemId = itemId,
                                    imageUrl = imageUrl,
                                    description = description
                                )
                            )
                        }
                    )
                }

                composable(Screen.Favorites.route) {
                    FavoritesScreen(
                        viewModel = favoritesViewModel,
                        onOpenFavorite = { url, title, isVideo, itemId, imageUrl, description ->
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
                        }
                    )
                }

                composable(Screen.BadgesRewards.route) {
                    val childUser = currentUser

                    if (childUser != null && !childUser.isGuest) {
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

                composable(Screen.AdminUpgrade.route) {
                    AdminUpgradeScreen(
                        onBack = { navController.popBackStack() }
                    )
                }

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

                composable(Screen.ParentalControls.route) {
                    ParentalControlsScreen(
                        onNavigateBack = { navController.popBackStack() },
                        authViewModel = authViewModel
                    )
                }

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
                                        itemId = itemId,
                                        itemTitle = title.ifBlank { url },
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

                composable(
                    route = Screen.Reader.route,
                    arguments = listOf(
                        navArgument("url") {
                            type = NavType.StringType
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
                                        itemId = url,
                                        itemTitle = url,
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

                composable(Screen.PremiumUpgrade.route) {
                    PremiumUpgradeScreen(
                        onBack = { navController.popBackStack() }
                    )
                }
            }
        }

        if (isChildAccount) {
            ScreenTimeWrapper { navContent() }
        } else {
            navContent()
        }
    }
}

@Composable
fun AnnouncementDialog(
    announcements: List<UserNotification>,
    onDismiss: () -> Unit
) {
    AlertDialog(
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
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Got it!")
            }
        }
    )
}