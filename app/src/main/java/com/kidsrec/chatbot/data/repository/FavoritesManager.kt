package com.kidsrec.chatbot.data.repository

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import com.kidsrec.chatbot.data.model.Favorite
import com.kidsrec.chatbot.data.model.RecommendationType
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.first
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
                    Log.e("FavoritesManager", "Error loading favorites: ${error.message}")
                    trySend(emptyList())
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
        imageUrl: String,
        url: String = ""
    ): Result<Unit> {
        return try {
            val favorite = Favorite(
                id = itemId,
                userId = userId,
                itemId = itemId,
                type = type,
                title = title,
                description = description,
                imageUrl = imageUrl,
                url = url
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

    suspend fun getFavorites(userId: String): List<Favorite> {
        return try {
            val snapshot = firestore.collection("favorites")
                .document(userId)
                .collection("items")
                .get()
                .await()
            snapshot.toObjects(Favorite::class.java)
        } catch (e: Exception) {
            emptyList()
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
