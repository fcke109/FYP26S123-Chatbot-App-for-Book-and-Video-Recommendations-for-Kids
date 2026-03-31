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

@Singleton
class ReadingHistoryManager @Inject constructor(
    private val firestore: FirebaseFirestore
) {
    suspend fun addEntry(
        userId: String,
        title: String,
        url: String,
        coverUrl: String = "",
        isVideo: Boolean = false
    ) {
        try {
            val ref = firestore.collection("readingHistory")
                .document(userId)
                .collection("sessions")
                .document()
            val entry = ReadingHistory(
                id = ref.id,
                userId = userId,
                title = title,
                url = url,
                coverUrl = coverUrl,
                isVideo = isVideo,
                openedAt = Timestamp.now()
            )
            ref.set(entry).await()
        } catch (e: Exception) {
            Log.e("ReadingHistory", "Failed to save reading history entry", e)
        }
    }

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
                val items = snapshot?.toObjects(ReadingHistory::class.java) ?: emptyList()
                trySend(items)
            }
        awaitClose { listener.remove() }
    }
}
