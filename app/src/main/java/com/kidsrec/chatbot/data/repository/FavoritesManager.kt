package com.kidsrec.chatbot.data.repository

import android.util.Log
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.kidsrec.chatbot.data.model.Favorite
import com.kidsrec.chatbot.data.model.Recommendation
import com.kidsrec.chatbot.data.model.RecommendationType
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

// Handles adding, removing, and loading user favorites
@Singleton
class FavoritesManager @Inject constructor(
    private val firestore: FirebaseFirestore
) {

    companion object {
        private const val TAG = "FavoritesManager"
        private const val TEST_TAG = "FAV_TEST"

        // Free plan limits
        private const val FREE_BOOK_LIMIT = 2
        private const val FREE_VIDEO_LIMIT = 2
    }

    // Loads all favorites for a user
    suspend fun getFavorites(userId: String): List<Favorite> {
        return try {
            val snapshot = firestore.collection("favorites")
                .document(userId)
                .collection("items")
                .get()
                .await()

            snapshot.documents.mapNotNull { doc ->
                mapDocumentToFavorite(userId, doc.id, doc.data ?: emptyMap())
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load favorites: ${e.message}", e)
            emptyList()
        }
    }

    // Real-time favorites listener
    fun getFavoritesFlow(userId: String): Flow<List<Favorite>> = callbackFlow {
        val listener = firestore.collection("favorites")
            .document(userId)
            .collection("items")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(TAG, "Favorites listener failed: ${error.message}", error)
                    trySend(emptyList())
                    return@addSnapshotListener
                }

                val items = snapshot?.documents?.mapNotNull { doc ->
                    mapDocumentToFavorite(userId, doc.id, doc.data ?: emptyMap())
                } ?: emptyList()

                Log.d(TEST_TAG, "Favorites flow update for userId=$userId count=${items.size}")
                trySend(items)
            }

        awaitClose { listener.remove() }
    }

    // Adds favorite using Recommendation object
    suspend fun addFavorite(
        userId: String,
        recommendation: Recommendation
    ): Result<Unit> {
        return addFavorite(
            userId = userId,
            itemId = recommendation.id.ifBlank { recommendation.title.trim() },
            type = recommendation.type,
            title = recommendation.title,
            description = recommendation.description,
            imageUrl = recommendation.imageUrl,
            url = recommendation.url
        )
    }

    // Main add favorite function
    suspend fun addFavorite(
        userId: String,
        itemId: String,
        type: RecommendationType,
        title: String,
        description: String,
        imageUrl: String,
        url: String = ""
    ): Result<Unit> {
        return try {
            val safeItemId = itemId.ifBlank { generateFavoriteId(title, type) }

            // Check if user has premium/unlimited favorites
            val hasUnlimited = hasUnlimitedFavorites(userId)

            Log.d(
                TEST_TAG,
                "Limit check: userId=$userId, type=$type, hasUnlimitedFavorites=$hasUnlimited"
            )

            // Apply free-plan limits
            if (!hasUnlimited) {
                val currentFavorites = getFavorites(userId)

                val currentBooks = currentFavorites.count { it.type == RecommendationType.BOOK }
                val currentVideos = currentFavorites.count { it.type == RecommendationType.VIDEO }

                // Free book limit
                if (type == RecommendationType.BOOK && currentBooks >= FREE_BOOK_LIMIT) {
                    return Result.failure(
                        Exception("Free plan allows up to $FREE_BOOK_LIMIT favorite books. Upgrade to Premium for unlimited favorites.")
                    )
                }

                // Free video limit
                if (type == RecommendationType.VIDEO && currentVideos >= FREE_VIDEO_LIMIT) {
                    return Result.failure(
                        Exception("Free plan allows up to $FREE_VIDEO_LIMIT favorite videos. Upgrade to Premium for unlimited favorites.")
                    )
                }
            }

            // Build favorite object
            val favorite = Favorite(
                id = safeItemId,
                userId = userId,
                itemId = safeItemId,
                type = type,
                title = title,
                description = description,
                imageUrl = imageUrl,
                url = url,
                addedAt = Timestamp.now()
            )

            Log.d(
                TEST_TAG,
                "Writing favorite: userId=$userId, itemId=$safeItemId, title=$title, type=$type"
            )

            // Ensure parent favorites document exists
            try {
                firestore.collection("favorites")
                    .document(userId)
                    .set(
                        mapOf(
                            "userId" to userId,
                            "updatedAt" to Timestamp.now()
                        ),
                        SetOptions.merge()
                    )
                    .await()

                Log.d(TEST_TAG, "Parent favorites doc ensured for userId=$userId")
            } catch (e: Exception) {
                Log.w(
                    TEST_TAG,
                    "Parent favorites doc write failed, continuing with child item write: ${e.message}"
                )
            }

            // Save favorite item
            firestore.collection("favorites")
                .document(userId)
                .collection("items")
                .document(safeItemId)
                .set(favorite)
                .await()

            Log.d(TEST_TAG, "Favorite item write success for itemId=$safeItemId")
            Result.success(Unit)

        } catch (e: Exception) {
            Log.e(TAG, "Failed to add favorite: ${e.message}", e)
            Log.e(TEST_TAG, "Failed to add favorite", e)
            Result.failure(e)
        }
    }


    // Checks whether user has premium/admin unlimited favorites
    private suspend fun hasUnlimitedFavorites(userId: String): Boolean {
        return try {
            val userDoc = firestore.collection("users")
                .document(userId)
                .get()
                .await()

            if (!userDoc.exists()) {
                Log.d(TEST_TAG, "Plan check: user doc missing, defaulting to FREE")
                return false
            }

            val planType = userDoc.getString("planType")
                ?.trim()
                ?.uppercase()
                ?: "FREE"

            Log.d(TEST_TAG, "Plan check user=$userId planType=$planType")

            planType == "PREMIUM" || planType == "ADMIN"

        } catch (e: Exception) {
            Log.w(TEST_TAG, "Plan check failed, defaulting to FREE", e)
            false
        }
    }

    // Removes favorite item
    suspend fun removeFavorite(
        userId: String,
        itemId: String
    ): Result<Unit> {
        return try {
            firestore.collection("favorites")
                .document(userId)
                .collection("items")
                .document(itemId)
                .delete()
                .await()

            Log.d(TEST_TAG, "Removed favorite: userId=$userId, itemId=$itemId")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to remove favorite: ${e.message}", e)
            Log.e(TEST_TAG, "Failed to remove favorite", e)
            Result.failure(e)
        }
    }

    // Checks whether an item already exists in favorites
    suspend fun isFavorite(
        userId: String,
        itemId: String
    ): Boolean {
        return try {
            val doc = firestore.collection("favorites")
                .document(userId)
                .collection("items")
                .document(itemId)
                .get()
                .await()

            doc.exists()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to check favorite: ${e.message}", e)
            false
        }
    }

    // Converts Firestore document data into Favorite object
    private fun mapDocumentToFavorite(
        userId: String,
        docId: String,
        data: Map<String, Any>
    ): Favorite {
        val typeValue = data["type"]?.toString().orEmpty()

        val recommendationType = when {
            typeValue.equals("VIDEO", ignoreCase = true) -> RecommendationType.VIDEO
            typeValue.equals("BOOK", ignoreCase = true) -> RecommendationType.BOOK
            else -> RecommendationType.BOOK
        }

        return Favorite(
            id = (data["id"] as? String).orEmpty().ifBlank { docId },
            userId = (data["userId"] as? String).orEmpty().ifBlank { userId },
            itemId = (data["itemId"] as? String).orEmpty().ifBlank { docId },
            type = recommendationType,
            title = (data["title"] as? String).orEmpty(),
            description = (data["description"] as? String).orEmpty(),
            imageUrl = (data["imageUrl"] as? String).orEmpty(),
            url = (data["url"] as? String).orEmpty(),
            addedAt = data["addedAt"] as? Timestamp ?: Timestamp.now()
        )
    }

    // Generates fallback favorite ID if item ID is missing
    private fun generateFavoriteId(
        title: String,
        type: RecommendationType
    ): String {
        return "fav_${type.name.lowercase()}_${title.trim().lowercase().hashCode()}"
    }
}