package com.kidsrec.chatbot.data

import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class ContentRepository(
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
) {

    suspend fun getAllContentItems(): List<CFItem> {
        val items = mutableListOf<CFItem>()

        val booksSnap = db.collection("content_books").get().await()
        for (doc in booksSnap.documents) {
            items.add(
                CFItem(
                    id = doc.id,
                    title = doc.getString("title") ?: "",
                    description = doc.getString("description") ?: "",
                    imageUrl = doc.getString("imageUrl") ?: "",
                    url = doc.getString("url") ?: "",
                    type = "BOOK",
                    category = doc.getString("category") ?: "",
                    ageMin = (doc.getLong("ageMin") ?: 0L).toInt(),
                    ageMax = (doc.getLong("ageMax") ?: 18L).toInt(),
                    isKidSafe = doc.getBoolean("isKidSafe") ?: true,
                    tags = (doc.get("tags") as? List<*>)?.filterIsInstance<String>() ?: emptyList()
                )
            )
        }

        val videosSnap = db.collection("content_videos").get().await()
        for (doc in videosSnap.documents) {
            items.add(
                CFItem(
                    id = doc.id,
                    title = doc.getString("title") ?: "",
                    description = doc.getString("description") ?: "",
                    imageUrl = doc.getString("imageUrl") ?: "",
                    url = doc.getString("url") ?: "",
                    type = "VIDEO",
                    category = doc.getString("category") ?: "",
                    ageMin = (doc.getLong("ageMin") ?: 0L).toInt(),
                    ageMax = (doc.getLong("ageMax") ?: 18L).toInt(),
                    isKidSafe = doc.getBoolean("isKidSafe") ?: true,
                    tags = (doc.get("tags") as? List<*>)?.filterIsInstance<String>() ?: emptyList()
                )
            )
        }

        return items
    }
}