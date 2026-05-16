package com.kidsrec.chatbot.data.repository

import android.util.Log
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.kidsrec.chatbot.data.model.LearningProgressEvent
import com.kidsrec.chatbot.data.model.WeeklyLearningReport
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.util.Calendar
import javax.inject.Inject
import javax.inject.Singleton

// Handles tracking books, videos and topics explored by the child user
@Singleton
class LearningProgressManager @Inject constructor(
    private val firestore: FirebaseFirestore
) {

    companion object {
        // Event type constants
        private const val TYPE_BOOK_READ = "BOOK_READ"
        private const val TYPE_VIDEO_WATCHED = "VIDEO_WATCHED"
        private const val TYPE_TOPIC_EXPLORED = "TOPIC_EXPLORED"
    }

    // Saves a book reading activity into Firestore
    suspend fun trackBookRead(
        childUserId: String,
        contentId: String,
        title: String,
        topic: String,
        readingLevel: String = "Beginner"
    ): Result<Unit> {
        return trackEvent(
            childUserId = childUserId,
            type = TYPE_BOOK_READ,
            title = title,
            topic = topic,
            contentId = contentId,
            contentType = "BOOK",
            readingLevel = readingLevel
        )
    }

    // Saves a watched video activity
    suspend fun trackVideoWatched(
        childUserId: String,
        contentId: String,
        title: String,
        topic: String
    ): Result<Unit> {
        return trackEvent(
            childUserId = childUserId,
            type = TYPE_VIDEO_WATCHED,
            title = title,
            topic = topic,
            contentId = contentId,
            contentType = "VIDEO",
            readingLevel = ""
        )
    }

    // Saves explored topic activity
    suspend fun trackTopicExplored(
        childUserId: String,
        topic: String
    ): Result<Unit> {
        return trackEvent(
            childUserId = childUserId,
            type = TYPE_TOPIC_EXPLORED,
            title = topic,
            topic = topic,
            contentId = "",
            contentType = "TOPIC",
            readingLevel = ""
        )
    }

    // Generic function used to save all learning events
    private suspend fun trackEvent(
        childUserId: String,
        type: String,
        title: String,
        topic: String,
        contentId: String,
        contentType: String,
        readingLevel: String,
        durationSeconds: Long = 0L
    ): Result<Unit> {
        return try {
            // Create Firestore document reference
            val docRef = firestore.collection("learningProgress")
                .document(childUserId)
                .collection("events")
                .document()

            // Create learning progress event object
            val event = LearningProgressEvent(
                id = docRef.id,
                childUserId = childUserId,
                type = type,
                title = title,
                topic = topic,
                contentId = contentId,
                contentType = contentType,
                readingLevel = readingLevel,
                timestamp = Timestamp.now(),
                durationSeconds = durationSeconds
            )

            // Save event into Firestore
            docRef.set(event).await()
            Log.d("LearningProgressManager", "Tracked event: $type / $title / $topic")
            Result.success(Unit)
        } catch (e: Exception) {
            // Log Firestore or tracking errors
            Log.e("LearningProgressManager", "Failed to track event: ${e.message}", e)
            Result.failure(e)
        }
    }

    // Realtime listener for weekly learning report
    fun getWeeklyReportFlow(childUserId: String): Flow<WeeklyLearningReport> = callbackFlow {
        val listener = firestore.collection("learningProgress")
            .document(childUserId)
            .collection("events")
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .limit(100)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("LearningProgressManager", "Weekly report listener error: ${error.message}", error)
                    trySend(WeeklyLearningReport())
                    return@addSnapshotListener
                }

                // Convert Firestore documents into objects
                val allEvents = snapshot?.toObjects(LearningProgressEvent::class.java) ?: emptyList()
                // Filter only this week's events
                val weeklyEvents = allEvents.filter { it.timestamp.seconds >= getStartOfWeekTimestamp().seconds }
                Log.d("LearningProgressManager", "Weekly events loaded: ${weeklyEvents.size}")
                // Generate and send report
                trySend(buildWeeklyReport(weeklyEvents))
            }

        // Remove Firestore listener when flow closes
        awaitClose { listener.remove() }
    }

    // Calculates report statistics from events
    private fun buildWeeklyReport(events: List<LearningProgressEvent>): WeeklyLearningReport {
        val booksRead = events.count { it.type == TYPE_BOOK_READ }
        val videosWatched = events.count { it.type == TYPE_VIDEO_WATCHED }

        // Count explored topics
        val topicCounts = events
            .filter { it.topic.isNotBlank() }
            .groupingBy { it.topic.trim() }
            .eachCount()

        val topicsExplored = topicCounts.keys.sorted()
        val topTopic = topicCounts.maxByOrNull { it.value }?.key.orEmpty()

        val readingScores = events
            .filter { it.type == TYPE_BOOK_READ }
            .mapNotNull { levelToScoreOrNull(it.readingLevel) }

        val averageReadingLevelScore =
            if (readingScores.isNotEmpty()) readingScores.average() else 0.0

        val readingLevelGrowth = when {
            averageReadingLevelScore >= 2.5 -> "Improving"
            averageReadingLevelScore >= 1.5 -> "Stable"
            averageReadingLevelScore > 0.0 -> "Developing"
            else -> "Stable"
        }

        return WeeklyLearningReport(
            booksRead = booksRead,
            videosWatched = videosWatched,
            topicsExplored = topicsExplored,
            topTopic = topTopic,
            averageReadingLevelScore = averageReadingLevelScore,
            readingLevelGrowth = readingLevelGrowth,
            recentEvents = events.take(10)
        )
    }

    // Converts reading levels into numeric scores
    private fun levelToScoreOrNull(level: String): Int? {
        return when (level.trim().lowercase()) {
            "beginner" -> 1
            "intermediate" -> 2
            "advanced" -> 3
            else -> null
        }
    }

    // Returns Monday 12AM timestamp for weekly filtering
    private fun getStartOfWeekTimestamp(): Timestamp {
        val calendar = Calendar.getInstance()
        calendar.firstDayOfWeek = Calendar.MONDAY
        calendar.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        return Timestamp(calendar.time)
    }
}