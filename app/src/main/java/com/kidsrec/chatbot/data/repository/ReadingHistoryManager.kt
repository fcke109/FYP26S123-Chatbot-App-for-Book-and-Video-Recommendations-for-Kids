package com.kidsrec.chatbot.data.repository

import android.util.Log
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.kidsrec.chatbot.data.model.ReadingHistory
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

// Handles saving and loading user's recently opened books/videos
@Singleton
class ReadingHistoryManager @Inject constructor(
    private val firestore: FirebaseFirestore
) {
    // Saves a new reading or video session into Firestore
    suspend fun addEntry(
        userId: String,
        title: String,
        url: String,
        coverUrl: String = "",
        isVideo: Boolean = false
    ) {
        try {
            // Create a new reading history document
            val ref = firestore.collection("readingHistory")
                .document(userId)
                .collection("sessions")
                .document()
            // Prepare reading history data
            val entry = ReadingHistory(
                id = ref.id,
                userId = userId,
                title = title,
                url = url,
                coverUrl = coverUrl,
                isVideo = isVideo,
                openedAt = Timestamp.now()
            )
            // Save history entry into Firestore
            ref.set(entry).await()
        } catch (e: Exception) {
            Log.e("ReadingHistory", "Failed to save reading history entry", e)
        }
    }

    // Realtime listener for user's latest reading/video history
    fun getHistoryFlow(userId: String, limit: Int = 10): Flow<List<ReadingHistory>> = callbackFlow {
        val listener = firestore.collection("readingHistory")
            .document(userId)
            .collection("sessions")
            .orderBy("openedAt", Query.Direction.DESCENDING)
            .limit(limit.toLong())
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                // Convert Firestore documents into ReadingHistory objects
                val items = snapshot?.toObjects(ReadingHistory::class.java) ?: emptyList()
                // Send updated history list into Flow
                trySend(items)
            }
        awaitClose { listener.remove() }
    }
}
