package com.kidsrec.chatbot.ui.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.*
import androidx.compose.runtime.*
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
import com.kidsrec.chatbot.ui.auth.*
import com.kidsrec.chatbot.ui.chat.DinoChatPage
import com.kidsrec.chatbot.ui.chat.ChatViewModel
import com.kidsrec.chatbot.ui.favorites.FavoritesScreen
import com.kidsrec.chatbot.ui.favorites.FavoritesViewModel
import com.kidsrec.chatbot.ui.library.UserLibraryScreen
import com.kidsrec.chatbot.ui.profile.ProfileScreen
import com.kidsrec.chatbot.ui.profile.ProfileViewModel
import com.kidsrec.chatbot.ui.webview.SafeWebViewScreen
import java.net.URLDecoder
import java.net.URLEncoder

sealed class Screen(val route: String, val title: String, val icon: ImageVector? = null) {
    object Login : Screen("login", "Login")
    object Register : Screen("register", "Register")
    object Chat : Screen("chat", "Chat", Icons.Default.Chat)
    object Library : Screen("library", "Library", Icons.AutoMirrored.Filled.MenuBook) 
    object Favorites : Screen("favorites", "Favorites", Icons.Default.Favorite)
    object Profile : Screen("profile", "Profile", Icons.Default.Person)
    object Admin : Screen("admin", "Admin", Icons.Default.Shield)
    object SafeWebView : Screen("webview/{url}/{title}/{isVideo}", "WebView")
}

@Composable
fun AppNavigation() {
    val authViewModel: AuthViewModel = hiltViewModel()
    val authState by authViewModel.authState.collectAsState()
    val currentUser by authViewModel.currentUser.collectAsState()

    val isAdmin by remember(currentUser, authState) {
        derivedStateOf {
            val firebaseEmail = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.email?.lowercase()
            firebaseEmail == "admin@littledino.com" || 
            currentUser?.email?.lowercase() == "admin@littledino.com" ||
            currentUser?.planType == PlanType.ADMIN
        }
    }

    when (authState) {
        is AuthState.Authenticated -> {
            MainScreen(authViewModel, isAdmin)
        }
        is AuthState.Initial -> {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        }
        else -> {
            // Unauthenticated, Loading, and Error all show the login screen
            // so the user can see errors and retry
            AuthNavigation(authViewModel)
        }
    }
}

@Composable
fun AuthNavigation(authViewModel: AuthViewModel) {
    val navController = rememberNavController()
    NavHost(navController = navController, startDestination = Screen.Login.route) {
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

    val bottomNavItems = if (isAdmin) emptyList() else listOf(Screen.Chat, Screen.Library, Screen.Favorites, Screen.Profile)
    val startRoute = if (isAdmin) Screen.Admin.route else Screen.Chat.route

    Scaffold(
        bottomBar = {
            if (bottomNavItems.isNotEmpty() && currentDestination?.route in bottomNavItems.map { it.route }) {
                NavigationBar {
                    bottomNavItems.forEach { screen ->
                        NavigationBarItem(
                            icon = { screen.icon?.let { Icon(it, contentDescription = screen.title) } },
                            label = { Text(screen.title) },
                            selected = currentDestination?.hierarchy?.any { it.route == screen.route } == true,
                            onClick = {
                                navController.navigate(screen.route) {
                                    popUpTo(navController.graph.findStartDestination().id) { saveState = true }
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
                    onAddToFavorites = { rec ->
                        favoritesViewModel.addFavorite(rec.id, rec.type, rec.title, rec.description, rec.imageUrl)
                    },
                    onOpenRecommendation = { url, title, isVideo ->
                        val encodedUrl = URLEncoder.encode(url, "UTF-8")
                        val encodedTitle = URLEncoder.encode(title, "UTF-8")
                        navController.navigate("webview/$encodedUrl/$encodedTitle/$isVideo")
                    }
                )
            }
            composable(Screen.Library.route) {
                val adminViewModel: AdminViewModel = hiltViewModel()
                val favoritesViewModel: FavoritesViewModel = hiltViewModel()
                UserLibraryScreen(
                    viewModel = adminViewModel,
                    favoritesViewModel = favoritesViewModel,
                    onViewBook = { title, url ->
                        val encodedUrl = URLEncoder.encode(url, "UTF-8")
                        val encodedTitle = URLEncoder.encode(title, "UTF-8")
                        navController.navigate("webview/$encodedUrl/$encodedTitle/false")
                    }
                )
            }
            composable(Screen.Favorites.route) {
                val favoritesViewModel: FavoritesViewModel = hiltViewModel()
                FavoritesScreen(
                    viewModel = favoritesViewModel,
                    onOpenFavorite = { url, title, isVideo ->
                        val encodedUrl = URLEncoder.encode(url, "UTF-8")
                        val encodedTitle = URLEncoder.encode(title, "UTF-8")
                        navController.navigate("webview/$encodedUrl/$encodedTitle/$isVideo")
                    }
                )
            }
            composable(Screen.Profile.route) {
                val profileViewModel: ProfileViewModel = hiltViewModel()
                ProfileScreen(authViewModel = authViewModel, profileViewModel = profileViewModel)
            }
            composable(Screen.Admin.route) {
                val adminViewModel: AdminViewModel = hiltViewModel()
                AdminScreen(
                    viewModel = adminViewModel,
                    onLogout = { authViewModel.signOut() },
                    onViewBook = { title, url, isVideo ->
                        val encodedUrl = URLEncoder.encode(url, "UTF-8")
                        val encodedTitle = URLEncoder.encode(title, "UTF-8")
                        navController.navigate("webview/$encodedUrl/$encodedTitle/$isVideo")
                    }
                )
            }
            composable(
                route = Screen.SafeWebView.route,
                arguments = listOf(
                    navArgument("url") { type = NavType.StringType },
                    navArgument("title") { type = NavType.StringType },
                    navArgument("isVideo") { type = NavType.BoolType }
                )
            ) { backStackEntry ->
                val url = URLDecoder.decode(backStackEntry.arguments?.getString("url") ?: "", "UTF-8")
                val title = URLDecoder.decode(backStackEntry.arguments?.getString("title") ?: "", "UTF-8")
                val isVideo = backStackEntry.arguments?.getBoolean("isVideo") ?: false
                SafeWebViewScreen(url = url, title = title, isVideo = isVideo, onClose = { navController.popBackStack() })
            }
        }
    }
}
