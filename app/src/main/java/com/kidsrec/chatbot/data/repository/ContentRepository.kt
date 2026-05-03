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
            items.add(doc.toCFItem(defaultType = "BOOK"))
        }

        val videosSnap = db.collection("content_videos").get().await()
        for (doc in videosSnap.documents) {
            items.add(doc.toCFItem(defaultType = "VIDEO"))
        }

        return items
    }

    // Most book docs in content_books store the cover under "coverUrl", not
    // "imageUrl" — match Book.displayImageUrl's coverUrl-first preference so
    // the CF "Users like you" cards render covers instead of the fallback icon.
    private fun com.google.firebase.firestore.DocumentSnapshot.toCFItem(
        defaultType: String
    ): CFItem {
        val cover = getString("coverUrl")?.takeIf { it.isNotBlank() }
        val image = getString("imageUrl")?.takeIf { it.isNotBlank() }
        return CFItem(
            id = id,
            title = getString("title") ?: "",
            description = getString("description") ?: "",
            imageUrl = cover ?: image ?: "",
            url = getString("url") ?: "",
            type = defaultType,
            category = getString("category") ?: "",
            ageMin = (getLong("ageMin") ?: 0L).toInt(),
            ageMax = (getLong("ageMax") ?: 18L).toInt(),
            isKidSafe = getBoolean("isKidSafe") ?: true,
            tags = (get("tags") as? List<*>)?.filterIsInstance<String>() ?: emptyList()
        )
    }
}