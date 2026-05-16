package com.kidsrec.chatbot.data.repository

import android.util.Log
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.kidsrec.chatbot.data.model.Badge
import com.kidsrec.chatbot.data.model.BadgeUnlock
import com.kidsrec.chatbot.data.model.GamificationProfile
import com.kidsrec.chatbot.data.model.LearningProgressEvent
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

// Singleton so only one instance of this manager exists in the app
@Singleton
class GamificationManager @Inject constructor(
    // Firestore database instance
    private val firestore: FirebaseFirestore
) {

    // Points system configuration
    companion object {
        private const val POINTS_BOOK = 10
        private const val POINTS_VIDEO = 5
        private const val POINTS_TOPIC = 8
    }

    // List of all badges available in the app
    val defaultBadges = listOf(
        Badge("books_1", "Story Starter", "Completed your first book.", "book", "reading", 1),
        Badge("books_5", "5 Books Completed", "Completed 5 books.", "menu_book", "reading", 5),
        Badge("books_10", "Super Reader", "Completed 10 books.", "auto_stories", "reading", 10),

        Badge("animals_5", "Animal Expert", "Explored animal topics 5 times.", "pets", "topic", 5),
        Badge("space_5", "Space Explorer", "Explored space topics 5 times.", "rocket", "topic", 5),
        Badge("dino_5", "Dinosaur Detective", "Explored dinosaur topics 5 times.", "search", "topic", 5),

        Badge("videos_1", "Video Starter", "Watched your first video.", "play_circle", "video", 1),
        Badge("videos_5", "Video Learner", "Watched 5 videos.", "smart_display", "video", 5),

        Badge("topics_10", "Curious Reader", "Explored 10 topics.", "psychology", "exploration", 10)
    )

    // Recalculates points, levels and unlocked badges
    suspend fun refreshGamification(childUserId: String): Result<Unit> {
        return try {
            // Get all learning events from Firestore
            val eventsSnapshot = firestore.collection("learningProgress")
                .document(childUserId)
                .collection("events")
                .get()
                .await()

            val events = eventsSnapshot.toObjects(LearningProgressEvent::class.java)

            // Count books, videos and explored topics
            val booksRead = events.count { it.type == "BOOK_READ" }
            val videosWatched = events.count { it.type == "VIDEO_WATCHED" }
            val topicsExplored = events.count { it.type == "TOPIC_EXPLORED" }

            // Count specific topics for special badges
            val animalTopicCount = events.count {
                it.topic.contains("animal", ignoreCase = true) ||
                        it.topic.contains("shark", ignoreCase = true) ||
                        it.topic.contains("ocean", ignoreCase = true) ||
                        it.topic.contains("farm", ignoreCase = true) ||
                        (
                                !it.topic.contains("dinosaur", ignoreCase = true) &&
                                        (
                                                it.topic.contains("fish", ignoreCase = true) ||
                                                        it.topic.contains("whale", ignoreCase = true)
                                                )
                                )
            }

            val spaceTopicCount = events.count {
                it.topic.contains("space", ignoreCase = true) ||
                        it.topic.contains("planet", ignoreCase = true) ||
                        it.topic.contains("moon", ignoreCase = true) ||
                        it.topic.contains("star", ignoreCase = true) ||
                        it.topic.contains("rocket", ignoreCase = true)
            }

            val dinoTopicCount = events.count {
                it.topic.contains("dinosaur", ignoreCase = true) ||
                        it.topic.contains("dino", ignoreCase = true)
            }
           // Calculate total points and user level
            val totalPoints =
                booksRead * POINTS_BOOK +
                        videosWatched * POINTS_VIDEO +
                        topicsExplored * POINTS_TOPIC

            val currentLevel = calculateLevel(totalPoints)
            val profileRef = firestore.collection("gamification").document(childUserId)

            // Get existing profile data
            val existingProfile = profileRef.get().await().toObject(GamificationProfile::class.java)
            val existingUnlocked = existingProfile?.unlockedBadges ?: emptyList()
            val previousLevel = existingProfile?.currentLevel ?: 1

            val newUnlocked = mutableSetOf<String>()
            newUnlocked.addAll(existingUnlocked)

            // Check and unlock badges if requirements are met
            checkAndUnlockBadge(childUserId, "books_1", booksRead, newUnlocked)
            checkAndUnlockBadge(childUserId, "books_5", booksRead, newUnlocked)
            checkAndUnlockBadge(childUserId, "books_10", booksRead, newUnlocked)
            checkAndUnlockBadge(childUserId, "videos_1", videosWatched, newUnlocked)
            checkAndUnlockBadge(childUserId, "videos_5", videosWatched, newUnlocked)
            checkAndUnlockBadge(childUserId, "topics_10", topicsExplored, newUnlocked)
            checkAndUnlockBadge(childUserId, "animals_5", animalTopicCount, newUnlocked)
            checkAndUnlockBadge(childUserId, "space_5", spaceTopicCount, newUnlocked)
            checkAndUnlockBadge(childUserId, "dino_5", dinoTopicCount, newUnlocked)

            // Store latest gamification profile into Firestore
            val profile = GamificationProfile(
                childUserId = childUserId,
                totalPoints = totalPoints,
                currentLevel = currentLevel,
                unlockedBadges = newUnlocked.toList().sorted(),
                currentStreak = 0,
                lastActivityDate = todayString()
            )

            profileRef.set(profile).await()

            // Send notification when user levels up
            if (currentLevel > previousLevel) {
                storeRewardNotification(
                    userId = childUserId,
                    title = "Level Up!",
                    body = "You reached Level $currentLevel. Keep going, superstar!",
                    type = "reward_level_up",
                    category = "reward"
                )
            }

            Result.success(Unit)
        } catch (e: Exception) {
            // Log Firestore or calculation errors
            Log.e("GamificationManager", "Failed to refresh gamification: ${e.message}", e)
            Result.failure(e)
        }
    }

    // Unlocks and stores badge rewards
    private suspend fun checkAndUnlockBadge(
        childUserId: String,
        badgeId: String,
        currentCount: Int,
        unlockedBadges: MutableSet<String>
    ) {
        val badge = defaultBadges.firstOrNull { it.id == badgeId } ?: return
        if (currentCount < badge.requiredCount) return
        if (unlockedBadges.contains(badgeId)) return

        unlockedBadges.add(badgeId)

        val unlock = BadgeUnlock(
            badgeId = badge.id,
            badgeTitle = badge.title,
            description = badge.description,
            iconName = badge.iconName,
            unlockedAt = Timestamp.now()
        )

        // Save unlocked badge into Firestore
        firestore.collection("gamification")
            .document(childUserId)
            .collection("badges")
            .document(badge.id)
            .set(unlock)
            .await()

        // Send badge unlock notification
        storeRewardNotification(
            userId = childUserId,
            title = "Badge Unlocked!",
            body = "You earned '${badge.title}'! ${badge.description}",
            type = "reward_badge",
            category = "reward"
        )
    }

    // Saves reward notifications into Firestore
    private suspend fun storeRewardNotification(
        userId: String,
        title: String,
        body: String,
        type: String,
        category: String
    ) {
        val docRef = firestore.collection("userNotifications")
            .document(userId)
            .collection("items")
            .document()

        val payload = mapOf(
            "title" to title,
            "body" to body,
            "type" to type,
            "read" to false,
            "category" to category,
            "createdAt" to System.currentTimeMillis()
        )

        docRef.set(payload).await()
    }

    // Realtime listener for profile updates
    fun getGamificationProfileFlow(childUserId: String): Flow<GamificationProfile> = callbackFlow {
        val listener = firestore.collection("gamification")
            .document(childUserId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("GamificationManager", "Profile listener failed: ${error.message}", error)
                    trySend(GamificationProfile(childUserId = childUserId))
                    return@addSnapshotListener
                }

                val profile = snapshot?.toObject(GamificationProfile::class.java)
                    ?: GamificationProfile(childUserId = childUserId)

                trySend(profile)
            }

        awaitClose { listener.remove() }
    }

    // Realtime listener for unlocked badges
    fun getUnlockedBadgesFlow(childUserId: String): Flow<List<BadgeUnlock>> = callbackFlow {
        val listener = firestore.collection("gamification")
            .document(childUserId)
            .collection("badges")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("GamificationManager", "Badges listener failed: ${error.message}", error)
                    trySend(emptyList())
                    return@addSnapshotListener
                }

                val badges = snapshot?.toObjects(BadgeUnlock::class.java).orEmpty()
                    .sortedByDescending { it.unlockedAt?.seconds ?: 0L }

                trySend(badges)
            }

        awaitClose { listener.remove() }
    }

    // Converts points into user level
    private fun calculateLevel(points: Int): Int {
        return when {
            points >= 300 -> 4
            points >= 180 -> 3
            points >= 80 -> 2
            else -> 1
        }
    }

    // Returns today's date as yyyy-MM-dd
    private fun todayString(): String {
        return SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
    }
}