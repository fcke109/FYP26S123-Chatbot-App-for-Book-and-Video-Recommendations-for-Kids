package com.kidsrec.chatbot

import android.app.Application
import com.google.firebase.Firebase
import com.google.firebase.appcheck.appCheck
import com.google.firebase.appcheck.debug.DebugAppCheckProviderFactory
import com.google.firebase.appcheck.playintegrity.PlayIntegrityAppCheckProviderFactory
import com.google.firebase.initialize
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class KidsRecApp : Application() {
    // Called when the app process is created
    override fun onCreate() {
        super.onCreate()

        // Initializes Firebase services for the application
        Firebase.initialize(context = this)
        // Installs Firebase App Check to help protect backend resources from unauthorised access
        Firebase.appCheck.installAppCheckProviderFactory(
            if (BuildConfig.DEBUG) {
                // Uses the debug App Check provider during development/testing
                DebugAppCheckProviderFactory.getInstance()
            } else {
                // Uses Play Integrity App Check provider for release builds
                PlayIntegrityAppCheckProviderFactory.getInstance()
            }
        )
    }
}
