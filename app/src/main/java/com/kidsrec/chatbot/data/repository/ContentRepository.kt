package com.kidsrec.chatbot.data

import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

// Loads books and videos from Firestore for collaborative filtering recommendations
class ContentRepository(
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
) {

    // Retrieves all books and videos from Firestore
    suspend fun getAllContentItems(): List<CFItem> {
        val items = mutableListOf<CFItem>()

        // Load curated books
        val booksSnap = db.collection("content_books").get().await()
        for (doc in booksSnap.documents) {
            items.add(doc.toCFItem(defaultType = "BOOK"))
        }

        // Load curated videos
        val videosSnap = db.collection("content_videos").get().await()
        for (doc in videosSnap.documents) {
            items.add(doc.toCFItem(defaultType = "VIDEO"))
        }

        return items
    }

    // Converts Firestore documents into CFItem objects used by the recommendation system.
    private fun com.google.firebase.firestore.DocumentSnapshot.toCFItem(
        defaultType: String
    ): CFItem {
        val cover = getString("coverUrl")?.takeIf { it.isNotBlank() }
        val image = getString("imageUrl")?.takeIf { it.isNotBlank() }
        return CFItem(
            id = id,
            title = getString("title") ?: "",
            description = getString("description") ?: "",
            // Prefer coverUrl first
            imageUrl = cover ?: image ?: "",
            url = getString("url") ?: "",
            type = defaultType,
            category = getString("category") ?: "",
            ageMin = (getLong("ageMin") ?: 0L).toInt(),
            ageMax = (getLong("ageMax") ?: 18L).toInt(),
            // Safety filter for recommendations
            isKidSafe = getBoolean("isKidSafe") ?: true,
            // Tags used for filtering/recommendations
            tags = (get("tags") as? List<*>)?.filterIsInstance<String>() ?: emptyList()
        )
    }
}