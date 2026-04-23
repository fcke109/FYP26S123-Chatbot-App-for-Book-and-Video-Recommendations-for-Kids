package com.kidsrec.chatbot.data.repository

import android.util.Log
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.kidsrec.chatbot.data.model.TopDropOff
import com.kidsrec.chatbot.data.model.TopSearchedTopic
import com.kidsrec.chatbot.data.model.TopViewedBook
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AnalyticsRepository @Inject constructor(
    private val firestore: FirebaseFirestore
) {

    fun getTopSearchedTopicsFlow(): Flow<List<TopSearchedTopic>> = callbackFlow {
        val listener = firestore.collection("analyticsSearches")
            .orderBy("count", Query.Direction.DESCENDING)
            .limit(10)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("AnalyticsRepo", "Error loading top searches", error)
                    trySend(emptyList())
                    return@addSnapshotListener
                }

                val topics = snapshot?.documents?.map { doc ->
                    TopSearchedTopic(
                        query = doc.getString("query") ?: doc.id,
                        count = doc.getLong("count") ?: 0L
                    )
                } ?: emptyList()

                trySend(topics)
            }

        awaitClose { listener.remove() }
    }

    fun getTopViewedBooksFlow(): Flow<List<TopViewedBook>> = callbackFlow {
        val listener = firestore.collection("analyticsBookViews")
            .orderBy("viewCount", Query.Direction.DESCENDING)
            .limit(10)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("AnalyticsRepo", "Error loading top viewed books", error)
                    trySend(emptyList())
                    return@addSnapshotListener
                }

                val books = snapshot?.documents?.map { doc ->
                    val viewCount = doc.getLong("viewCount") ?: doc.getLong("count") ?: 0L

                    TopViewedBook(
                        bookId = doc.getString("bookId") ?: doc.id,
                        bookTitle = doc.getString("bookTitle") ?: "",
                        viewCount = viewCount,
                        count = viewCount
                    )
                } ?: emptyList()

                trySend(books)
            }

        awaitClose { listener.remove() }
    }

    fun getTopDropOffsFlow(): Flow<List<TopDropOff>> = callbackFlow {
        val listener = firestore.collection("analyticsDropOffs")
            .orderBy("dropOffCount", Query.Direction.DESCENDING)
            .limit(10)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("AnalyticsRepo", "Error loading drop-offs", error)
                    trySend(emptyList())
                    return@addSnapshotListener
                }

                val dropOffs = snapshot?.documents?.map { doc ->
                    val avg = (
                            doc.getLong("avgDurationSeconds")
                                ?: doc.getLong("averageDurationSeconds")
                                ?: 0L
                            ).toInt()

                    val count = doc.getLong("dropOffCount")
                        ?: doc.getLong("count")
                        ?: 0L

                    TopDropOff(
                        itemId = doc.getString("itemId") ?: doc.id,
                        itemTitle = doc.getString("itemTitle") ?: "",
                        avgDurationSeconds = avg,
                        averageDurationSeconds = avg,
                        dropOffCount = count,
                        count = count
                    )
                } ?: emptyList()

                trySend(dropOffs)
            }

        awaitClose { listener.remove() }
    }

    suspend fun trackSearch(query: String, userId: String) {
        try {
            val normalizedQuery = query.trim().lowercase()
            if (normalizedQuery.isBlank()) return

            val docId = sanitizeAnalyticsDocId(normalizedQuery)
            if (docId.isBlank()) return

            val docRef = firestore.collection("analyticsSearches").document(docId)

            firestore.runTransaction { transaction ->
                val snapshot = transaction.get(docRef)
                val currentCount = snapshot.getLong("count") ?: 0L

                transaction.set(
                    docRef,
                    mapOf(
                        "query" to normalizedQuery,
                        "count" to currentCount + 1L,
                        "lastSearchedBy" to userId,
                        "updatedAt" to Timestamp.now()
                    )
                )
            }.await()

            Log.d("AnalyticsRepo", "Tracked search: $normalizedQuery")
        } catch (e: Exception) {
            Log.e("AnalyticsRepo", "Failed to track search: $query", e)
        }
    }

    suspend fun trackBookView(bookId: String, bookTitle: String, userId: String) {
        try {
            val safeId = sanitizeAnalyticsDocId(
                raw = if (bookId.isNotBlank()) bookId else bookTitle
            )
            if (safeId.isBlank()) return

            val docRef = firestore.collection("analyticsBookViews").document(safeId)
            val snapshot = docRef.get().await()
            val currentCount = snapshot.getLong("viewCount") ?: 0L
            val newCount = currentCount + 1L

            docRef.set(
                mapOf(
                    "bookId" to bookId,
                    "bookTitle" to bookTitle,
                    "viewCount" to newCount,
                    "count" to newCount,
                    "lastViewedBy" to userId,
                    "updatedAt" to Timestamp.now()
                )
            ).await()

            Log.d("ANALYTICS_TEST", "analyticsBookViews updated for $bookTitle count=$newCount")
        } catch (e: Exception) {
            Log.e("ANALYTICS_TEST", "trackBookView FAILED", e)
        }
    }

    suspend fun trackDropOffPoint(
        itemId: String,
        itemTitle: String,
        userId: String,
        openedAt: Timestamp,
        closedAt: Timestamp,
        durationSeconds: Long
    ) {
        try {
            val safeItemId = sanitizeAnalyticsDocId(
                raw = if (itemId.isNotBlank()) itemId else itemTitle
            )
            if (safeItemId.isBlank()) return

            // Raw event log
            firestore.collection("analyticsDropOffEvents")
                .add(
                    mapOf(
                        "itemId" to itemId,
                        "itemTitle" to itemTitle,
                        "userId" to userId,
                        "openedAt" to openedAt,
                        "closedAt" to closedAt,
                        "durationSeconds" to durationSeconds,
                        "timestamp" to Timestamp.now()
                    )
                )
                .await()

            // Aggregate summary doc
            val docRef = firestore.collection("analyticsDropOffs").document(safeItemId)

            firestore.runTransaction { transaction ->
                val snapshot = transaction.get(docRef)
                val currentCount = snapshot.getLong("dropOffCount") ?: 0L
                val currentAvg = snapshot.getLong("avgDurationSeconds")
                    ?: snapshot.getLong("averageDurationSeconds")
                    ?: 0L

                val newCount = currentCount + 1L
                val newAvg = if (currentCount == 0L) {
                    durationSeconds
                } else {
                    ((currentAvg * currentCount) + durationSeconds) / newCount
                }

                transaction.set(
                    docRef,
                    mapOf(
                        "itemId" to itemId,
                        "itemTitle" to itemTitle,
                        "dropOffCount" to newCount,
                        "count" to newCount,
                        "avgDurationSeconds" to newAvg,
                        "averageDurationSeconds" to newAvg,
                        "lastUserId" to userId,
                        "updatedAt" to Timestamp.now()
                    )
                )
            }.await()

            Log.d("AnalyticsRepo", "Tracked drop-off: $itemTitle")
        } catch (e: Exception) {
            Log.e("AnalyticsRepo", "Failed to track drop-off: $itemTitle", e)
        }
    }

    private fun sanitizeAnalyticsDocId(raw: String): String {
        return raw
            .trim()
            .lowercase()
            .replace(Regex("https?://"), "")
            .replace(Regex("[^a-z0-9]"), "_")
            .replace(Regex("_+"), "_")
            .trim('_')
            .take(120)
    }
}