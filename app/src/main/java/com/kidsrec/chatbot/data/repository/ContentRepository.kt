package com.kidsrec.chatbot.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.kidsrec.chatbot.data.model.Book
import com.kidsrec.chatbot.data.model.Video
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ContentRepository @Inject constructor(
    private val firestore: FirebaseFirestore
) {
    suspend fun getBooks(limit: Int = 20): Result<List<Book>> {
        return try {
            val snapshot = firestore.collection("content")
                .document("library")
                .collection("books")
                .limit(limit.toLong())
                .get()
                .await()

            val books = snapshot.toObjects(Book::class.java)
            Result.success(books)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getVideos(limit: Int = 20): Result<List<Video>> {
        return try {
            val snapshot = firestore.collection("content")
                .document("library")
                .collection("videos")
                .limit(limit.toLong())
                .get()
                .await()

            val videos = snapshot.toObjects(Video::class.java)
            Result.success(videos)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getBook(bookId: String): Result<Book?> {
        return try {
            val snapshot = firestore.collection("content")
                .document("library")
                .collection("books")
                .document(bookId)
                .get()
                .await()

            val book = snapshot.toObject(Book::class.java)
            Result.success(book)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getVideo(videoId: String): Result<Video?> {
        return try {
            val snapshot = firestore.collection("content")
                .document("library")
                .collection("videos")
                .document(videoId)
                .get()
                .await()

            val video = snapshot.toObject(Video::class.java)
            Result.success(video)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
