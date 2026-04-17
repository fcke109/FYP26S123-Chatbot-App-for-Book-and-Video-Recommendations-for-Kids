package com.kidsrec.chatbot.data.repository

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.kidsrec.chatbot.data.model.Book
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton


@Singleton
class BookDataManager @Inject constructor(
    private val firestore: FirebaseFirestore
) {
    private val collection = firestore.collection("content_books")

    fun getCuratedBooksFlow(): Flow<List<Book>> = callbackFlow {
        val subscription = collection
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("BookDataManager", "Firestore Error: ${error.message}")
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val books = snapshot.documents
                        .mapNotNull { it.toObject(Book::class.java)?.copy(id = it.id) }
                        .sortedBy { it.id }
                    trySend(books)
                }
            }
        awaitClose { subscription.remove() }
    }

    suspend fun getCuratedBooks(): Result<List<Book>> {
        return try {
            val snapshot = collection.get().await()
            val books = snapshot.documents
                .mapNotNull { it.toObject(Book::class.java)?.copy(id = it.id) }
                .sortedBy { it.id }
            Result.success(books)
        } catch (e: Exception) { Result.failure(e) }
    }

    suspend fun getNextSequentialId(): String {
        return try {
            val snapshot = collection.get().await()
            val existingIds = snapshot.documents.map { it.id }
            val maxId = existingIds
                .filter { it.all { char -> char.isDigit() } }
                .mapNotNull { it.toIntOrNull() }
                .maxOrNull() ?: 0
            String.format("%03d", maxId + 1)
        } catch (e: Exception) {
            "001"
        }
    }

    suspend fun addBook(book: Book): Result<Unit> {
        return try {
            val finalId = if (book.id.isBlank() || !book.id.all { it.isDigit() }) {
                getNextSequentialId()
            } else {
                book.id
            }
            collection.document(finalId).set(book.copy(id = finalId)).await()
            Result.success(Unit)
        } catch (e: Exception) { Result.failure(e) }
    }

    suspend fun deleteBook(bookId: String): Result<Unit> {
        return try {
            collection.document(bookId).delete().await()
            Result.success(Unit)
        } catch (e: Exception) { Result.failure(e) }
    }
}
