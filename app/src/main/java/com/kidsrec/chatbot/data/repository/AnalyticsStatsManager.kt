package com.kidsrec.chatbot.data.repository

import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

// Handles homepage analytics numbers shown on the Little Dino website
object AnalyticsStatsManager {

    private val db = FirebaseFirestore.getInstance()

    // Main Firestore document that stores website analytics stats
    private val analyticsRef =
        db.collection("site_content")
            .document("analytics")

    // Increases registered user count by 1
    fun incrementRegisteredUsers() {
        analyticsRef.update("stat1Value", FieldValue.increment(1))
    }

    // Increases app feedback count by 1
    fun incrementAppFeedback() {
        analyticsRef.update("stat2Value", FieldValue.increment(1))
    }

    // Increases available books count by 1
    fun incrementBooksAvailable() {
        analyticsRef.update("stat3Value", FieldValue.increment(1))
    }

    // Recalculates homepage analytics using real Firestore collection data
    suspend fun syncHomepageAnalytics() {
        val usersSnap = db.collection("users").get().await()
        val feedbackSnap = db.collection("feedback")
            .whereEqualTo("contentType", "app")
            .whereEqualTo("status", "published")
            .get()
            .await()
        val booksSnap = db.collection("content_books").get().await()

        // Updates the website analytics document with latest values and labels
        analyticsRef.update(
            mapOf(
                "stat1Value" to usersSnap.size(),
                "stat2Value" to feedbackSnap.size(),
                "stat3Value" to booksSnap.size(),

                "stat1Title" to "Registered Users",
                "stat1Text" to "Total registered users in Little Dino.",

                "stat2Title" to "App Feedback",
                "stat2Text" to "Published feedback submitted by users.",

                "stat3Title" to "Books Available",
                "stat3Text" to "Books currently available for safe and guided discovery."
            )
        ).await()
    }
}