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

@Singleton
class FavoritesManager @Inject constructor(
    private val firestore: FirebaseFirestore
) {

    companion object {
        private const val TAG = "FavoritesManager"
        private const val TEST_TAG = "FAV_TEST"
    }

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

            // Try to create/update parent doc for CF support.
            // If rules block this, do NOT fail the actual favorite save.
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

    private fun mapDocumentToFavorite(
        userId: String,
        docId: String,
        data: Map<String, Any>
    ): Favorite {
        val typeValue = data["type"]?.toString().orEmpty()
        val recommendationType = if (typeValue.equals("VIDEO", ignoreCase = true)) {
            RecommendationType.VIDEO
        } else {
            RecommendationType.BOOK
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

    private fun generateFavoriteId(
        title: String,
        type: RecommendationType
    ): String {
        return "fav_${type.name.lowercase()}_${title.trim().lowercase().hashCode()}"
    }
}