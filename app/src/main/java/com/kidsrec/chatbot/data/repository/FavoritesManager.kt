package com.kidsrec.chatbot.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.kidsrec.chatbot.data.model.Favorite
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
    fun getFavoritesFlow(userId: String): Flow<List<Favorite>> = callbackFlow {
        val listener = firestore.collection("favorites")
            .document(userId)
            .collection("items")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                val items = snapshot?.toObjects(Favorite::class.java) ?: emptyList()
                trySend(items)
            }
        awaitClose { listener.remove() }
    }

    suspend fun addFavorite(
        userId: String,
        itemId: String,
        type: RecommendationType,
        title: String,
        description: String,
        imageUrl: String
    ): Result<Unit> {
        return try {
            val favorite = Favorite(
                id = itemId,
                userId = userId,
                itemId = itemId,
                type = type,
                title = title,
                description = description,
                imageUrl = imageUrl
            )
            firestore.collection("favorites")
                .document(userId)
                .collection("items")
                .document(itemId)
                .set(favorite)
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun removeFavorite(userId: String, itemId: String): Result<Unit> {
        return try {
            firestore.collection("favorites")
                .document(userId)
                .collection("items")
                .document(itemId)
                .delete()
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
