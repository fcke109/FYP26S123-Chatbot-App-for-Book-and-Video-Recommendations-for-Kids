package com.kidsrec.chatbot.data.repository

import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.kidsrec.chatbot.data.model.Favorite
import com.kidsrec.chatbot.data.model.RecommendationType
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FavoritesRepository @Inject constructor(
    private val firestore: FirebaseFirestore
) {
    suspend fun addFavorite(
        userId: String,
        itemId: String,
        type: RecommendationType,
        title: String,
        description: String,
        imageUrl: String
    ): Result<Unit> {
        return try {
            val favoriteId = "${userId}_${itemId}"
            val favorite = Favorite(
                id = favoriteId,
                userId = userId,
                itemId = itemId,
                type = type,
                title = title,
                description = description,
                imageUrl = imageUrl,
                addedAt = Timestamp.now()
            )

            firestore.collection("favorites")
                .document(userId)
                .collection("items")
                .document(favoriteId)
                .set(favorite)
                .await()

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun removeFavorite(userId: String, itemId: String): Result<Unit> {
        return try {
            val favoriteId = "${userId}_${itemId}"
            firestore.collection("favorites")
                .document(userId)
                .collection("items")
                .document(favoriteId)
                .delete()
                .await()

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun getFavoritesFlow(userId: String): Flow<List<Favorite>> = callbackFlow {
        val listener = firestore.collection("favorites")
            .document(userId)
            .collection("items")
            .orderBy("addedAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                val favorites = snapshot?.toObjects(Favorite::class.java) ?: emptyList()
                trySend(favorites)
            }

        awaitClose { listener.remove() }
    }

    suspend fun isFavorite(userId: String, itemId: String): Boolean {
        return try {
            val favoriteId = "${userId}_${itemId}"
            val doc = firestore.collection("favorites")
                .document(userId)
                .collection("items")
                .document(favoriteId)
                .get()
                .await()

            doc.exists()
        } catch (e: Exception) {
            false
        }
    }
}
