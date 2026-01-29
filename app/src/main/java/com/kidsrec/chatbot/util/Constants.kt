package com.kidsrec.chatbot.util

import com.kidsrec.chatbot.BuildConfig

object Constants {
    // API key is loaded from local.properties via BuildConfig
    // Add OPENAI_API_KEY=your_key_here to local.properties
    val OPENAI_API_KEY: String = BuildConfig.OPENAI_API_KEY
}
