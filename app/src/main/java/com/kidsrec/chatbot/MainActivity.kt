package com.kidsrec.chatbot

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.kidsrec.chatbot.ui.navigation.AppNavigation
import com.kidsrec.chatbot.ui.theme.KidsRecommendationAppTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    // Called when the activity is first created
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            // Applies the custom LittleDino app theme to all screens
            KidsRecommendationAppTheme {
                Surface(
                    // Provides a full-screen surface using the app's background colour
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    // Loads the main navigation graph for authentication and app screens
                    AppNavigation()
                }
            }
        }
    }
}
