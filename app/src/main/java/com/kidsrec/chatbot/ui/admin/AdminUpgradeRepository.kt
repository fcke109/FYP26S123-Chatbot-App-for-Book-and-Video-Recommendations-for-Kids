package com.kidsrec.chatbot.ui.admin

import android.util.Log
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.kidsrec.chatbot.data.repository.BookDataManager
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

// Repository used by the admin module to handle content safety actions
@Singleton
class AdminUpgradeRepository @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val bookDataManager: BookDataManager
) {

    // Removes unsafe content from the curated library and records the reason for audit purposes
    suspend fun removeUnsafeContent(bookId: String, reason: String) {
        // Log the removal for audit
        firestore.collection("contentRemovals").add(
            mapOf(
                "bookId" to bookId,
                "reason" to reason,
                "removedAt" to Timestamp.now()
            )
        ).await()

        // Delete the book from the library
        bookDataManager.deleteBook(bookId)

        // Log the removal action for debugging and monitoring
        Log.d("AdminUpgradeRepo", "Removed unsafe content: $bookId, reason: $reason")
    }
}
