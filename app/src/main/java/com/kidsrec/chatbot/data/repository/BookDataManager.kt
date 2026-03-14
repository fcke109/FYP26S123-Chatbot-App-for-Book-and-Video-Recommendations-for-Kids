package com.kidsrec.chatbot.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.kidsrec.chatbot.data.model.Book
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BookDataManager @Inject constructor(
    private val firestore: FirebaseFirestore
) {
    private val collection = firestore.collection("content_books")

    suspend fun getCuratedBooks(): Result<List<Book>> {
        return try {
            val snapshot = collection.orderBy("id", Query.Direction.ASCENDING).get().await()
            val books = snapshot.documents.mapNotNull { it.toObject(Book::class.java)?.copy(id = it.id) }
            Result.success(books)
        } catch (e: Exception) { Result.failure(e) }
    }

    suspend fun searchBooks(query: String, age: Int, limit: Int = 8): List<Book> {
        return try {
            val keywords = query.lowercase().split(" ").filter { it.length >= 3 }
            val snap = firestore.collection("content_books")
                .whereEqualTo("isKidSafe", true)
                .limit(80).get().await()

            val all = snap.documents.mapNotNull { it.toObject(Book::class.java)?.copy(id = it.id) }
            all.filter { age in it.ageMin..it.ageMax }
                .filter { book ->
                    if (keywords.isEmpty()) true
                    else keywords.any { kw -> book.tags.any { it.contains(kw) } || book.title.lowercase().contains(kw) }
                }.take(limit)
        } catch (e: Exception) { emptyList() }
    }

    suspend fun addBook(book: Book): Result<Unit> {
        return try {
            val id = book.id.ifBlank { 
                val count = collection.get().await().size()
                String.format("%03d", count + 1)
            }
            collection.document(id).set(book.copy(id = id)).await()
            Result.success(Unit)
        } catch (e: Exception) { Result.failure(e) }
    }

    suspend fun deleteBook(bookId: String): Result<Unit> {
        return try {
            firestore.collection("content_books").document(bookId).delete().await()
            Result.success(Unit)
        } catch (e: Exception) { Result.failure(e) }
    }
}
