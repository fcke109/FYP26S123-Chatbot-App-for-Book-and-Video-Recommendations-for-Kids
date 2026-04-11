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

@Singleton
class LearningProgressManager @Inject constructor(
    private val firestore: FirebaseFirestore
) {

    companion object {
        private const val TYPE_BOOK_READ = "BOOK_READ"
        private const val TYPE_VIDEO_WATCHED = "VIDEO_WATCHED"
        private const val TYPE_TOPIC_EXPLORED = "TOPIC_EXPLORED"
    }

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
            val docRef = firestore.collection("learningProgress")
                .document(childUserId)
                .collection("events")
                .document()

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

            docRef.set(event).await()
            Log.d("LearningProgressManager", "Tracked event: $type / $title / $topic")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("LearningProgressManager", "Failed to track event: ${e.message}", e)
            Result.failure(e)
        }
    }

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

                val allEvents = snapshot?.toObjects(LearningProgressEvent::class.java) ?: emptyList()
                val weeklyEvents = allEvents.filter { it.timestamp.seconds >= getStartOfWeekTimestamp().seconds }
                Log.d("LearningProgressManager", "Weekly events loaded: ${weeklyEvents.size}")
                trySend(buildWeeklyReport(weeklyEvents))
            }

        awaitClose { listener.remove() }
    }

    private fun buildWeeklyReport(events: List<LearningProgressEvent>): WeeklyLearningReport {
        val booksRead = events.count { it.type == TYPE_BOOK_READ }
        val videosWatched = events.count { it.type == TYPE_VIDEO_WATCHED }

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

    private fun levelToScoreOrNull(level: String): Int? {
        return when (level.trim().lowercase()) {
            "beginner" -> 1
            "intermediate" -> 2
            "advanced" -> 3
            else -> null
        }
    }

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